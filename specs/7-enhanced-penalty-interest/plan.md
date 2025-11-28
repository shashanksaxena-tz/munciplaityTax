# Implementation Plan: Enhanced Penalty & Interest Calculation

**Branch**: `7-enhanced-penalty-interest` | **Date**: 2025-11-28 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/7-enhanced-penalty-interest/spec.md`

## Summary

Implement comprehensive tax penalty and interest calculation system covering late filing penalties (5% per month, max 25%), late payment penalties (1% per month, max 25%), quarterly estimated tax underpayment penalties with safe harbor rules, compound quarterly interest calculation, and automated penalty abatement for reasonable cause. This feature is critical for accurate tax liability calculation and helps filers avoid surprises at payment time.

**Primary Requirement**: Automated calculation of all penalty types (late filing, late payment, estimated underpayment) and quarterly-compounded interest with safe harbor rule evaluation, penalty abatement workflow, and transparent calculation breakdowns for taxpayer understanding.

**Technical Approach**: Extend tax-engine-service with penalty domain models (Penalty, EstimatedTaxPenalty, Interest, PenaltyAbatement, PaymentAllocation), implement event-driven penalty calculation updates triggered by filing/payment events, leverage rule engine service for configurable penalty rates/interest rates, add penalty calculation API endpoints and abatement workflow UI components to existing React frontend.

---

## Technical Context

**Language/Version**: 
- **Backend**: Java 21 with Spring Boot 3.2.3
- **Frontend**: TypeScript 5.x with React 18.2, Node.js 20.x
- **Build**: Maven 3.9+ (backend), Vite 5.x (frontend)

**Primary Dependencies**:
- **Backend**: Spring Data JPA, Spring Web, Spring Cloud (Eureka client), PostgreSQL driver, Jackson (JSON), Lombok, java.time API (date/time calculations)
- **Frontend**: React Router 6.x, Axios, Tailwind CSS 3.x, date-fns (date calculations), recharts (penalty breakdown visualization)
- **Testing**: JUnit 5, Mockito (backend), Vitest, React Testing Library (frontend)

**Storage**: 
- PostgreSQL 16 with multi-tenant schemas (tenant-scoped penalties, interests, penalty_abatements tables)
- Redis 7 for caching interest rates and penalty rates (updated quarterly)

**Testing**:
- Backend: JUnit 5 + Mockito for service layer, Spring Boot Test for integration tests, TestContainers for PostgreSQL
- Frontend: Vitest + React Testing Library for component tests, Playwright for E2E penalty calculation workflow
- Calculation accuracy: Parameterized tests covering 50+ penalty/interest scenarios

**Target Platform**: 
- Docker containers deployed via docker-compose (development) and Kubernetes (production)
- Web browsers: Chrome/Edge 100+, Firefox 100+, Safari 15+ (desktop and mobile)

**Project Type**: Web application with microservices backend (9 services) and React SPA frontend

**Performance Goals**:
- Penalty calculation for single return: <500ms (Success Criteria)
- Interest calculation with quarterly compounding (4 quarters): <1 second (Success Criteria)
- Safe harbor evaluation: <200ms (Success Criteria)
- Penalty abatement form generation: <2 seconds (Success Criteria)

**Constraints**:
- Multi-tenant data isolation (Constitution II): All queries scoped to tenant schema via tenant context
- Audit trail immutability (Constitution III): All penalty assessments, abatements, payment allocations logged immutably
- Must integrate with rule engine service (Spec 4) for configurable penalty/interest rates
- Must integrate with payment gateway for payment tracking and allocation
- Interest rate changes quarterly - system must support rate versioning

**Scale/Scope**:
- Target: 50,000 taxpayers per municipality, ~30% file/pay late (15,000 penalty calculations per year)
- Estimated tax underpayment: ~20% of self-employed filers (4,000 underpayment penalty calculations per year)
- Penalty abatement requests: ~5% of penalized filers (750 requests per year per municipality)
- Interest calculations: Continuous compounding over 1-48 months (1-16 quarters)

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ I. Microservices Architecture First

**Evaluation**: Feature extends existing **tax-engine-service** (owns tax calculations, penalties, returns). Penalty and interest calculations are core tax domain logic. No new service required.

**Service Placement**: 
- Penalty/interest calculation logic → tax-engine-service (domain: tax liability calculations)
- Payment tracking → submission-service + payment gateway (existing)
- Rule engine integration → rule-engine-service (Spec 4 - penalty rates, interest rates)
- PDF generation (Form 27-PA) → pdf-service (existing)
- Frontend → React SPA (existing)

**No Violations**: Feature properly extends existing service boundaries.

---

### ✅ II. Multi-Tenant Data Isolation (NON-NEGOTIABLE)

**Evaluation**: All database entities (Penalty, EstimatedTaxPenalty, Interest, PenaltyAbatement, PaymentAllocation) MUST include tenant_id foreign key. All JPA queries MUST use tenant-scoped schemas.

**Implementation**:
- Tenant context from JWT (existing auth-service integration)
- PostgreSQL schema-per-tenant (dublin.penalties, columbus.penalties)
- JPA @Filter annotation for automatic tenant scoping

**No Violations**: Feature complies with tenant isolation requirements.

---

### ✅ III. Audit Trail Immutability

**Evaluation**: Penalty assessments are financial records subject to 7-year retention (IRS IRC § 6001). Penalty abatement decisions are audit-critical and may be challenged in tax court.

**Implementation**:
- Penalty entity: created_at, created_by (user ID), never deleted (soft delete flag if needed)
- PenaltyAbatement entity: status changes logged with timestamp, actor, reason
- PaymentAllocation entity: immutable record of how each payment was applied
- Audit log table: penalty_audit_log (action, entity_type, entity_id, old_value, new_value, actor, timestamp)
- Interest rate versioning: interest_rate_history table tracks quarterly rate changes

**No Violations**: Feature implements immutable audit trails per constitution.

---

### ✅ IV. AI Transparency & Explainability

**Evaluation**: Feature does not involve AI extraction or machine learning. All penalty/interest calculations are deterministic rule-based calculations.

**Transparency Requirements**:
- Calculation breakdown display: Show step-by-step how penalty/interest was calculated
- Safe harbor explanation: Show which safe harbor rule was evaluated and why it passed/failed
- Penalty abatement reasoning: Store and display why penalty was approved/denied

**No Violations**: Feature provides full transparency of calculation logic without AI components.

---

### ✅ V. Security & Compliance First

**Evaluation**: Penalty/interest data contains sensitive financial information (tax amounts, SSN/EIN, payment history).

**Implementation**:
- Authentication: JWT required (existing auth-service)
- Authorization: ROLE_INDIVIDUAL (view own penalties), ROLE_BUSINESS (view own penalties), ROLE_AUDITOR (approve abatements, view all penalties)
- Encryption: SSN/EIN encrypted at rest (existing encryption layer in database)
- Logging: No SSN in logs (existing log sanitization)
- TLS 1.3: All production traffic (existing infrastructure)

**Compliance**: IRS Publication 1075 (federal tax info safeguarding), Ohio R.C. 718 (municipal tax confidentiality), Ohio R.C. 718.27 (penalty authority)

**No Violations**: Feature leverages existing security infrastructure.

---

### ✅ VI. User-Centric Design

**Evaluation**: Feature UI must make complex penalty/interest calculations understandable to non-tax-experts (individual filers).

**Implementation**:
- Progressive disclosure: Return summary shows "Total Due: $10,500 (Tax: $10,000 + Penalties: $400 + Interest: $100)" with drill-down for details
- Visual breakdown: Interactive chart showing penalty accumulation over time (month-by-month bars)
- Plain language: "Filed 3 months late → 15% late filing penalty" instead of technical jargon
- Penalty calculator: "What-if" tool showing penalty if paid in 1 month, 3 months, 6 months
- Abatement wizard: Step-by-step form for penalty abatement requests with reason selection and document upload
- Error prevention: Warn if estimated payment below safe harbor threshold

**No Violations**: Feature follows user-centric design principles.

---

### ✅ VII. Test Coverage & Quality Gates

**Evaluation**: Feature requires comprehensive test coverage for financial calculations (penalties, interest, compounding, safe harbor).

**Implementation**:
- Unit tests: 100% coverage of service layer (PenaltyCalculationService, InterestCalculationService, SafeHarborEvaluationService)
- Parameterized tests: 50+ scenarios covering all edge cases (partial months, combined cap, safe harbor variations)
- Integration tests: Spring Boot Test with TestContainers PostgreSQL for multi-payment scenarios
- Contract tests: API contracts for penalty calculation endpoints
- E2E tests: Playwright workflow: File late → View penalty → Request abatement → Verify status

**Quality Gates**:
- Build fails if test coverage <80%
- Build fails if any parameterized penalty calculation test fails
- Manual QA checklist: Test all 8 edge cases from spec.md, verify Form 27-PA PDF generation

**No Violations**: Feature includes comprehensive testing strategy.

---

## Constitution Violations Summary

**Total Violations**: 0 (Zero)

**Warnings**: 0 (Zero)

**Status**: ✅ **APPROVED** - Feature complies with all constitution principles. Proceed to Phase 0 research.

### Constitution Check: Post-Design Re-Evaluation (Phase 1 Complete)

**Re-evaluation Date**: After Phase 1 completion  
**Artifacts Reviewed**: data-model.md, contracts/, quickstart.md

**Status**: ⏳ **PENDING** - Will be completed after Phase 1 design phase

---

## Project Structure

### Documentation (this feature)

```text
specs/7-enhanced-penalty-interest/
├── plan.md              # This file ✅ COMPLETE
├── research.md          # Phase 0 output (to be generated)
├── data-model.md        # Phase 1 output (to be generated)
├── quickstart.md        # Phase 1 output (to be generated)
├── contracts/           # Phase 1 output (to be generated)
│   ├── api-penalty-calculation.yaml    # OpenAPI spec for penalty endpoints
│   ├── api-interest-calculation.yaml   # OpenAPI spec for interest endpoints
│   └── event-penalty-assessed.yaml     # AsyncAPI event: penalty-assessed, abatement-approved, etc.
└── tasks.md             # Phase 2 output (generated by /speckit.tasks - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Backend: Extend tax-engine-service (Spring Boot microservice)
backend/tax-engine-service/
├── src/main/java/com/munitax/taxengine/
│   ├── domain/
│   │   ├── penalty/
│   │   │   ├── Penalty.java                         # JPA entity (FR-001-006)
│   │   │   ├── EstimatedTaxPenalty.java             # JPA entity (FR-015-026)
│   │   │   ├── QuarterlyUnderpayment.java           # Value object (embedded in EstimatedTaxPenalty)
│   │   │   ├── Interest.java                        # JPA entity (FR-027-032)
│   │   │   ├── QuarterlyInterest.java               # Value object (embedded in Interest)
│   │   │   ├── PenaltyAbatement.java                # JPA entity (FR-033-039)
│   │   │   ├── PaymentAllocation.java               # JPA entity (FR-040-043)
│   │   │   └── PenaltyAuditLog.java                 # Audit trail (Constitution III)
│   │   └── rules/
│   │       ├── PenaltyRate.java                     # JPA entity (versioned penalty rates)
│   │       └── InterestRate.java                    # JPA entity (versioned quarterly interest rates)
│   ├── repository/
│   │   ├── PenaltyRepository.java                   # Spring Data JPA
│   │   ├── EstimatedTaxPenaltyRepository.java
│   │   ├── InterestRepository.java
│   │   ├── PenaltyAbatementRepository.java
│   │   ├── PaymentAllocationRepository.java
│   │   ├── PenaltyRateRepository.java
│   │   └── InterestRateRepository.java
│   ├── service/
│   │   ├── LateFilingPenaltyService.java            # Business logic: FR-001-006
│   │   ├── LatePaymentPenaltyService.java           # Business logic: FR-007-011
│   │   ├── CombinedPenaltyCapService.java           # Business logic: FR-012-014
│   │   ├── EstimatedTaxPenaltyService.java          # Business logic: FR-015-026
│   │   ├── SafeHarborEvaluationService.java         # Business logic: FR-015-019
│   │   ├── InterestCalculationService.java          # Business logic: FR-027-032
│   │   ├── PenaltyAbatementService.java             # Business logic: FR-033-039
│   │   ├── PaymentAllocationService.java            # Business logic: FR-040-043
│   │   └── PenaltyAggregationService.java           # Business logic: FR-047 (summary report)
│   ├── controller/
│   │   ├── PenaltyCalculationController.java        # REST API: POST /api/penalties/calculate, GET /api/penalties/{id}
│   │   ├── InterestCalculationController.java       # REST API: POST /api/interest/calculate, GET /api/interest/{id}
│   │   ├── PenaltyAbatementController.java          # REST API: POST /api/abatements, GET /api/abatements/{id}
│   │   └── PaymentAllocationController.java         # REST API: POST /api/payments/allocate, GET /api/payments/{id}
│   └── dto/
│       ├── PenaltyCalculationRequest.java           # Request DTO
│       ├── PenaltyCalculationResponse.java          # Response DTO
│       ├── SafeHarborEvaluationDto.java             # Safe harbor status DTO (FR-018)
│       ├── InterestCalculationRequest.java
│       ├── InterestCalculationResponse.java
│       ├── PenaltyAbatementRequest.java
│       └── PenaltyAbatementResponse.java
└── src/test/java/com/munitax/taxengine/
    ├── service/
    │   ├── LateFilingPenaltyServiceTest.java       # Unit tests
    │   ├── LatePaymentPenaltyServiceTest.java      # Unit tests
    │   ├── CombinedPenaltyCapServiceTest.java      # Unit tests (FR-012-014 scenarios)
    │   ├── EstimatedTaxPenaltyServiceTest.java     # Parameterized tests (50+ scenarios)
    │   ├── SafeHarborEvaluationServiceTest.java    # Unit tests (FR-015-019)
    │   ├── InterestCalculationServiceTest.java     # Unit tests (quarterly compounding)
    │   └── PenaltyAbatementServiceTest.java        # Unit tests (abatement workflow)
    └── integration/
        └── PenaltyInterestIntegrationTest.java     # TestContainers + full workflow

# Backend: Integration with rule-engine-service (Spec 4)
backend/rule-engine-service/
# (Stores penalty rates, interest rates, safe harbor thresholds as configurable rules)

# Backend: Integration with pdf-service
backend/pdf-service/
# (Generates Form 27-PA - Penalty Abatement Request)

# Frontend: React SPA (extend existing app)
src/
├── components/
│   ├── penalties/
│   │   ├── PenaltySummaryCard.tsx                  # Display penalty breakdown (US-1, US-2)
│   │   ├── PenaltyBreakdownModal.tsx               # Detailed penalty calculation (FR-047)
│   │   ├── InterestCalculationCard.tsx             # Display interest breakdown (US-6)
│   │   ├── SafeHarborStatusBanner.tsx              # Show safe harbor status (US-4, FR-018)
│   │   ├── EstimatedTaxPenaltyTable.tsx            # Quarterly underpayment schedule (US-5, FR-026)
│   │   ├── PenaltyAbatementWizard.tsx              # Multi-step abatement request form (US-7, FR-034)
│   │   ├── PaymentHistoryTimeline.tsx              # Visual payment history (FR-043)
│   │   ├── PenaltyCalculatorTool.tsx               # What-if calculator (User-Centric Design)
│   │   └── Form27PaViewer.tsx                      # View generated Form 27-PA PDF (FR-037)
│   └── shared/
│       ├── MonthCalculator.tsx                     # Display "3 months late" calculation
│       └── CurrencyFormatter.tsx                   # Consistent currency display
├── services/
│   ├── penaltyService.ts                           # API client: POST /api/penalties/calculate
│   ├── interestService.ts                          # API client: POST /api/interest/calculate
│   ├── abatementService.ts                         # API client: POST /api/abatements
│   └── paymentAllocationService.ts                 # API client: GET /api/payments/{id}
├── hooks/
│   ├── usePenaltyCalculation.ts                    # React Query hook for penalties
│   ├── useInterestCalculation.ts                   # React Query hook for interest
│   ├── useSafeHarborStatus.ts                      # React Query hook for safe harbor evaluation
│   └── usePenaltyAbatement.ts                      # React Query hook for abatement requests
└── types/
    ├── penalty.ts                                  # TypeScript types: Penalty, EstimatedTaxPenalty
    ├── interest.ts                                 # TypeScript types: Interest, QuarterlyInterest
    └── abatement.ts                                # TypeScript types: PenaltyAbatement, AbatementStatus

# Frontend Tests
src/
└── __tests__/
    ├── components/
    │   ├── PenaltySummaryCard.test.tsx             # Component tests (Vitest + RTL)
    │   ├── EstimatedTaxPenaltyTable.test.tsx       # Component tests
    │   └── PenaltyAbatementWizard.test.tsx         # Component tests
    └── e2e/
        └── penalty-calculation.spec.ts              # Playwright E2E: File late → View penalty → Request abatement

# Database Migrations (Flyway - tax-engine-service)
backend/tax-engine-service/src/main/resources/db/migration/
├── V1.30__create_penalties_table.sql               # CREATE TABLE penalties (tenant-scoped)
├── V1.31__create_estimated_tax_penalties_table.sql
├── V1.32__create_interests_table.sql
├── V1.33__create_penalty_abatements_table.sql
├── V1.34__create_payment_allocations_table.sql
├── V1.35__create_penalty_rates_table.sql
├── V1.36__create_interest_rates_table.sql
└── V1.37__create_penalty_audit_log_table.sql
```

**Structure Decision**: 
- **Backend**: Extend existing tax-engine-service (owns tax calculations, penalties, interest domain). No new microservice needed (Constitution I: "add to existing services if they share domain concerns").
- **Frontend**: Extend existing React SPA with new penalty/interest components. No separate frontend app needed.
- **Database**: New tables in tax-engine-service database, tenant-scoped per Constitution II.
- **Integration**: Leverage existing rule-engine-service (penalty/interest rates), pdf-service (Form 27-PA), payment gateway (submission-service), auth-service (JWT).

---

## Complexity Tracking

**No violations require justification.** Feature complies with all constitution principles.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| *(None)* | N/A | N/A |

---

## Phase 0: Research & Unknowns Resolution

**Status**: NOT STARTED  
**Output File**: `research.md`

### Research Tasks

#### R1: Interest Rate Versioning and Quarterly Updates

**Question**: How should system handle interest rate changes that occur mid-calculation period (e.g., rate changes from 5% to 6% in Q3 while calculating interest for Q1-Q4)?

**Context**: IRS updates interest rate quarterly based on federal short-term rate + 3%. Municipality may adopt federal rate or set own rate. If taxpayer owes tax for 12 months and rate changes in month 7, which rate applies to which period?

**Trade-offs**:
- **Option A - Rate at assessment**: Use interest rate effective at time penalty was assessed. Pros: Simple. Cons: Inaccurate (taxpayer benefits if rates increase).
- **Option B - Rate per period**: Apply rate effective during each quarter. Pros: Accurate. Cons: Complex (need to track rate changes, split calculation).
- **Option C - Blended rate**: Average rates over entire period. Pros: Simple approximation. Cons: Not legally accurate.

**Acceptance**: Research document must answer:
1. Review IRS interest rate calculation methodology (IRS Notice 2024-X)
2. Confirm Ohio municipal tax code requirement (ORC 718.27) - does it specify rate versioning?
3. Design interest_rate_history table schema (effective_date, rate, tenant_id)
4. Algorithm for split-period calculation (pseudocode)
5. Test scenario: Calculate interest for 12-month period spanning 2 rate changes

**Dependencies**: Review existing rule-engine-service architecture for configurable rules with effective dates.

---

#### R2: Safe Harbor Edge Case - Mid-Year Business Formation

**Question**: How should safe harbor rules apply to businesses that started mid-year (no prior year tax liability for comparison)?

**Context**: Safe Harbor 2 requires "paid 100% of prior year tax". If business registered in June 2024, there's no 2023 tax liability. Should safe harbor automatically fail? Or should prorated safe harbor apply (paid 100% of prorated current year)?

**Scenarios**:
1. **New business (no prior year)**: Registered March 2024 → No 2023 return → Safe Harbor 2 unavailable
2. **Partial year filer**: Registered March 2024 → Filed partial 2024 return (10 months) → Should 2025 safe harbor be prorated?
3. **Zero tax prior year**: 2023 tax was $0 (seasonal, no payroll) → 2024 tax is $20,000 → Does 100% of $0 = $0 safe harbor?

**Acceptance**: Research document must provide:
- Decision on new business safe harbor (fail? grace period? prorated?)
- Decision on zero prior year tax (treat as automatic safe harbor pass? or fail?)
- Validation rules in SafeHarborEvaluationService
- Test cases: 5 scenarios covering edge cases

**Dependencies**: Review IRS Form 2210 (Underpayment Penalty) instructions for new business guidance.

---

#### R3: Penalty Abatement First-Time Eligibility - Clean 3-Year Definition

**Question**: What constitutes "clean 3-year history" for first-time penalty abatement (FPA)? Does prior penalty that was later abated count against clean history?

**Context**: FR-036 requires "No penalties in prior 3 years" for FPA eligibility. But edge cases:
- Taxpayer had 2022 late filing penalty that was later abated for reasonable cause. Does 2025 FPA request still qualify?
- Taxpayer had 2021 underpayment penalty (before 3-year window). Does 2025 FPA request qualify?
- Taxpayer had 2023 late payment penalty but returned to compliance (filed/paid on time in 2024). Does 2025 FPA request qualify?

**Acceptance**: Research document must provide:
- Definition of "penalty-free" for FPA (assessed penalties? or non-abated penalties?)
- Lookback period calculation (rolling 3 years? or 3 prior tax years?)
- Database query to check FPA eligibility (SQL example)
- Decision tree: When to auto-approve FPA vs require manual review

**Dependencies**: Review IRS FPA policy (IRS Penalty Handbook), confirm municipality adopts same policy.

---

#### R4: Payment Allocation Order with Multiple Penalty Types

**Question**: How should payments be allocated when taxpayer owes multiple penalty types (late filing + late payment + underpayment) plus interest?

**Context**: FR-040 specifies "Tax → Penalties → Interest" ordering (IRS standard). But within "Penalties" step, which penalty gets paid first if payment insufficient to cover all penalties?

**Scenarios**:
- Taxpayer owes: $10,000 tax, $1,500 late filing penalty, $200 late payment penalty, $175 underpayment penalty, $100 interest
- Payment 1: $11,000 → Apply $10,000 to tax, $1,000 to late filing penalty (leaving $500 unpaid late filing)
- Payment 2: $1,000 → Apply $500 to remaining late filing, $200 to late payment, $175 to underpayment, $125 to interest? Or different order?

**Acceptance**: Research document must provide:
- Penalty allocation order within "Penalties" category (assessed date? penalty type priority?)
- Pseudocode for PaymentAllocationService.allocatePayment() method
- Test cases: 5 scenarios with partial payments covering various combinations
- Database schema for PaymentAllocation entity (applied_to_tax, applied_to_penalties_detail JSONB)

**Dependencies**: Review IRS Publication 594 (payment allocation) and Ohio R.C. 718 for municipal guidance.

---

#### R5: Partial Month Rounding for Penalty Calculation

**Question**: Should partial months be rounded up, down, or prorated for late filing/payment penalty calculations?

**Context**: FR-001 specifies "months late: (File date - Due date) in months, rounded up to next full month". But is this correct legally? Edge cases:
- Filed 1 day late (April 16 instead of April 15): 1 full month penalty (5%) or prorated (0.16% for 1 day)?
- Filed 15 days late (April 30 instead of April 15): 1 month or 0.5 months?

**IRS Guidance**: IRS uses "any part of a month counts as full month" for failure-to-file penalty (IRC § 6651). Should municipality adopt same approach?

**Acceptance**: Research document must provide:
- Decision on rounding approach (up/down/prorate) with legal justification
- Algorithm for month calculation (java.time examples)
- Test cases: 10 scenarios covering 1 day late, 15 days late, 30 days late, 31 days late, etc.
- Decision on "business days" vs "calendar days" (if due date is weekend/holiday)

**Dependencies**: Review Ohio R.C. 718.27 for penalty calculation methodology, confirm whether partial month rounding is specified.

---

### Research Deliverables

**research.md** file must include:

1. **Executive Summary**: 1-paragraph summary of all research findings and recommendations
2. **R1: Interest Rate Versioning**: Decision on rate versioning approach, schema design, split-period algorithm
3. **R2: Safe Harbor Edge Cases**: Decisions for new business, zero prior tax, prorated safe harbor
4. **R3: FPA Eligibility**: Definition of "clean 3-year history", eligibility query, auto-approval criteria
5. **R4: Payment Allocation Order**: Allocation order within penalties, pseudocode, test scenarios
6. **R5: Partial Month Rounding**: Decision on rounding approach, algorithm, test cases
7. **Technology Decisions Summary**: Table with [Decision, Rationale, Alternatives Considered]

**Acceptance Criteria for Phase 0 Completion**:
- All NEEDS CLARIFICATION items in Technical Context are resolved
- All 5 research tasks (R1-R5) have documented decisions with rationale
- Technology choices are concrete (no "or" options, no TBD)
- Constitution Check re-evaluated (all warnings addressed)

---

## Phase 1: Design & Contracts

**Status**: NOT STARTED  
**Output Files**: `data-model.md`, `contracts/`, `quickstart.md`

### Phase 1 Deliverables

1. **data-model.md**:
   - 7 entities: Penalty, EstimatedTaxPenalty, QuarterlyUnderpayment (embedded), Interest, QuarterlyInterest (embedded), PenaltyAbatement, PaymentAllocation
   - 2 rule entities: PenaltyRate, InterestRate (versioned rates)
   - Full field definitions (30+ fields for Penalty, 15+ for EstimatedTaxPenalty, etc.)
   - Relationships mapped (foreign keys, indexes)
   - Validation constraints (CHECK, UNIQUE, NOT NULL)
   - Flyway migration plan (V1.30-V1.37)
   - Performance considerations (query optimization, index strategy)
   - Data retention policy (7 years per IRS requirement)

2. **contracts/**:
   - **api-penalty-calculation.yaml** (OpenAPI 3.0): Endpoints
     - POST /api/penalties/calculate (calculate all penalties for return)
     - GET /api/penalties (list penalties with pagination)
     - GET /api/penalties/{id} (get penalty details)
     - GET /api/penalties/{id}/breakdown (get calculation breakdown for transparency)
   - **api-interest-calculation.yaml** (OpenAPI 3.0): Endpoints
     - POST /api/interest/calculate (calculate interest with quarterly compounding)
     - GET /api/interest (list interest calculations)
     - GET /api/interest/{id} (get interest details with quarterly breakdown)
   - **api-abatement.yaml** (OpenAPI 3.0): Endpoints
     - POST /api/abatements (submit penalty abatement request)
     - GET /api/abatements (list abatement requests)
     - GET /api/abatements/{id} (get abatement request details)
     - PATCH /api/abatements/{id}/review (approve/deny abatement - auditor only)
     - POST /api/abatements/{id}/documents (upload supporting documents)
     - GET /api/abatements/{id}/form-27pa (download Form 27-PA PDF)
   - **event-penalty-assessed.yaml** (AsyncAPI 2.6): Event channels
     - penalties/penalty-assessed (triggers notification)
     - penalties/interest-calculated (triggers notification)
     - penalties/abatement-requested (triggers auditor workflow)
     - penalties/abatement-approved (triggers return adjustment)
     - penalties/payment-allocated (triggers balance recalculation)
     - penalties/audit-log (immutable audit trail)

3. **quickstart.md**:
   - Environment setup (Docker, PostgreSQL, Redis, JWT auth)
   - 10+ API examples with curl commands
   - Database queries (SQL examples for penalty queries)
   - Test execution (unit, integration, E2E)
   - Flyway migration guide
   - Event-driven architecture (Redis Pub/Sub, Kafka)
   - Performance testing (penalty calculation load tests)
   - Troubleshooting guide (common penalty calculation issues)

4. **Update agent context**: Run update-agent-context script after Phase 1 completion

### Phase 1 Acceptance Criteria

- data-model.md includes 7 entities + 2 rule entities
- contracts/ includes 3 API files + 1 event file
- quickstart.md includes 10+ curl examples
- Constitution Check re-evaluated (0 violations expected)

---

## Phase 2: Task Breakdown (NOT part of /speckit.plan)

**Status**: NOT STARTED  
**Output File**: `tasks.md` (generated by separate `/speckit.tasks` command)

Phase 2 is NOT included in this plan. After Phase 0 and Phase 1 complete, run `/speckit.tasks` to generate implementation tasks.

---

## Next Steps

1. **Immediate**: Execute Phase 0 research tasks (R1-R5)
2. **Generate research.md**: Document all findings, decisions, rationale
3. **Checkpoint**: Review research.md with team, validate approach for interest rate versioning, safe harbor edge cases, FPA eligibility
4. **Proceed to Phase 1**: Design data model, API contracts, quickstart guide
5. **Final checkpoint**: Constitution Check post-design
6. **Output**: Deliver plan.md, research.md, data-model.md, contracts/, quickstart.md to implementation team

**Estimated Timeline**:
- Phase 0 (Research): 3-4 days (5 research tasks, some require legal/regulatory review)
- Phase 1 (Design): 3-4 days (7 entities, 3 API specs, complex calculation logic)
- **Total Planning**: 6-8 days before implementation begins

---

**Plan Status**: ✅ PHASE 0 READY - Proceed with research tasks (R1-R5)
