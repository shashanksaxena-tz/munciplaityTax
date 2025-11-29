# Implementation Plan: Schedule Y Multi-State Sourcing Feature

**Branch**: `copilot/add-schedule-y-sourcing-feature` | **Date**: 2025-11-28 | **Spec**: [spec.md](../../5-schedule-y-sourcing/spec.md)
**Input**: Feature specification from `/specs/5-schedule-y-sourcing/spec.md`

**Note**: This plan follows the `/speckit.plan` command workflow and aligns with the constitution principles.

## Summary

Implement comprehensive multi-state income sourcing and apportionment rules for businesses operating in multiple jurisdictions. This includes Joyce vs Finnigan election for sales factor sourcing, throwback/throwout rules for destination states without nexus, market-based sourcing for service revenue, and a three-factor apportionment formula (property, payroll, sales) with double-weighted sales. The system will support Schedule Y (Apportionment Schedule) with complete entity models, validation, audit trail, and UI components for apportionment breakdown, election controls, and PDF generation.

**Primary Requirement**: Enable 40%+ of multi-state business filers to accurately allocate income to each jurisdiction using standardized apportionment formulas with proper sourcing elections and throwback rule application.

**Technical Approach**: Extend tax-engine-service with apportionment domain models (ScheduleY, PropertyFactor, PayrollFactor, SalesFactor, SaleTransaction, NexusTracking, ApportionmentAuditLog), implement rule-based sourcing calculations, leverage existing rule engine for formula configuration, and add Schedule Y UI components to React frontend with apportionment breakdown visualization.

## Technical Context

**Language/Version**: 
- **Backend**: Java 21 with Spring Boot 3.2.3
- **Frontend**: TypeScript 5.x with React 18.2, Node.js 20.x
- **Build**: Maven 3.9+ (backend), Vite 5.x (frontend)

**Primary Dependencies**:
- **Backend**: Spring Data JPA, Spring Web, Spring Cloud (Eureka client), PostgreSQL driver, Jackson (JSON), Lombok
- **Frontend**: React Router 6.x, Axios, Tailwind CSS 3.x, Recharts (visualization), date-fns (date handling)
- **Testing**: JUnit 5, Mockito (backend), Vitest, React Testing Library (frontend)

**Storage**: 
- PostgreSQL 16 with multi-tenant schemas (tenant-scoped schedule_y, property_factor, payroll_factor, sales_factor, sale_transaction, nexus_tracking, apportionment_audit_log tables)
- Redis 7 for caching apportionment calculations and nexus determinations
- Rule engine (from Spec 4) for apportionment formula configuration and sourcing method rules

**Testing**:
- Backend: JUnit 5 + Mockito for service layer, Spring Boot Test for integration tests, TestContainers for PostgreSQL
- Frontend: Vitest + React Testing Library for component tests, Playwright for E2E apportionment workflow
- Contract Tests: Spring Cloud Contract for API validation

**Target Platform**: 
- Docker containers deployed via docker-compose (development) and Kubernetes (production)
- Web browsers: Chrome/Edge 100+, Firefox 100+, Safari 15+ (desktop and mobile)

**Project Type**: Web application with microservices backend (9 existing services) and React SPA frontend

**Performance Goals**:
- Apportionment calculation: <3 seconds for business with 1000+ transactions (FR-005, FR-037)
- Schedule Y PDF generation: <5 seconds (FR-046)
- Dashboard load time: <1 second for businesses with multi-state operations
- Nexus determination: <500ms for throwback rule evaluation (FR-011)

**Constraints**:
- Multi-tenant data isolation (Constitution II): All queries scoped to tenant schema via tenant context
- Audit trail immutability (Constitution III): All apportionment calculations, election changes, and factor adjustments logged immutably
- Must integrate with existing rule engine (Spec 4) for formula configuration
- Must integrate with existing withholding reconciliation (Spec 1) for payroll factor data
- Must support PDF generation via existing pdf-service

**Scale/Scope**:
- Target: 2,000 multi-state businesses per municipality (40% of 5,000 total business filers)
- ~8,000 Schedule Y filings per year per municipality (including amendments)
- Average 500 sales transactions per business per year requiring sourcing determination
- Support 50+ states/municipalities for nexus tracking

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ I. Microservices Architecture First

**Evaluation**: Feature extends existing **tax-engine-service** (owns tax calculations, filings, returns). Schedule Y apportionment is core tax domain logic. No new service required.

**Service Placement**: 
- Schedule Y apportionment logic → tax-engine-service (domain: tax calculations and returns)
- PDF generation → pdf-service (existing service)
- Rule engine integration → rule-engine-service (existing, from Spec 4)
- Payroll data integration → tax-engine-service (from Spec 1 withholding reconciliation)
- Frontend → React SPA (existing)

**No Violations**: Feature properly extends existing service boundaries.

---

### ✅ II. Multi-Tenant Data Isolation (NON-NEGOTIABLE)

**Evaluation**: All database entities (ScheduleY, PropertyFactor, PayrollFactor, SalesFactor, SaleTransaction, NexusTracking, ApportionmentAuditLog) MUST include tenant_id foreign key. All JPA queries MUST use tenant-scoped schemas.

**Implementation**:
- Tenant context from JWT (existing auth-service integration)
- PostgreSQL schema-per-tenant (dublin.schedule_y, columbus.schedule_y)
- JPA @Filter annotation for automatic tenant scoping
- Nexus tracking isolated per tenant (Dublin's nexus ≠ Columbus's nexus)

**No Violations**: Feature complies with tenant isolation requirements.

---

### ✅ III. Audit Trail Immutability

**Evaluation**: Apportionment calculations are financial records subject to 7-year retention (IRS IRC § 6001). Election changes (Joyce/Finnigan, throwback/throwout) and factor adjustments are audit-critical.

**Implementation**:
- ScheduleY entity: created_at, created_by (user ID), never deleted (soft delete flag if needed)
- Amended Schedule Y: new ScheduleY record with amends_schedule_y_id reference to original (both preserved)
- ApportionmentAuditLog entity: status changes, election changes, factor recalculations logged with timestamp, actor, reason
- Audit log table: apportionment_audit_log (change_type, entity_type, entity_id, old_value, new_value, actor, timestamp)
- All sourcing method elections and nexus determinations logged

**No Violations**: Feature implements immutable audit trails per constitution.

---

### ✅ IV. AI Transparency & Explainability

**Evaluation**: Feature does not directly use AI extraction but relies on existing data sources (payroll from Spec 1, sales records from user input).

**Existing Coverage**: 
- Payroll factor data comes from W-1 filings (Spec 1) which already has AI transparency for W-2 extraction
- Sales transaction data manually entered by user or imported from accounting system
- No new AI extraction needed

**Gap Identified**: None. Feature uses existing data with established provenance.

**No Violations**: Feature does not introduce new AI components requiring transparency measures.

---

### ✅ V. Security & Compliance First

**Evaluation**: Schedule Y contains sensitive business data (multi-state operations, revenue breakdown, property/payroll values). Must maintain confidentiality.

**Implementation**:
- Authentication: JWT required (existing auth-service)
- Authorization: ROLE_BUSINESS (file Schedule Y, view own apportionment), ROLE_AUDITOR (view all apportionments, approve calculations)
- Encryption: EIN, sensitive financial data encrypted at rest (existing encryption layer in database)
- Logging: No EIN in logs (existing log sanitization)
- TLS 1.3: All production traffic (existing infrastructure)

**Compliance**: IRS Publication 1075 (federal tax info safeguarding), Ohio R.C. 718 (municipal tax confidentiality)

**No Violations**: Feature leverages existing security infrastructure.

---

### ✅ VI. User-Centric Design

**Evaluation**: Feature UI must simplify complex multi-state apportionment for business owners (non-tax-experts) and CPAs.

**Implementation**:
- Progressive disclosure: Dashboard shows "Apportionment: 40.72%" with simple status
- Drill-down: Click to see detailed breakdown (property factor, payroll factor, sales factor with calculations)
- Error prevention: Real-time validation on factor entry (e.g., "Property factor >100% - verify property values")
- Wizards: Multi-step Schedule Y wizard (Property → Payroll → Sales → Elections → Review)
- Tooltips: Explain Joyce vs Finnigan, throwback rules, market-based sourcing with examples
- Mobile-first: Schedule Y form responsive (375px+ width), apportionment breakdown table horizontal scroll on mobile
- Visual breakdown: Chart/graph showing factor contributions to final apportionment percentage

**No Violations**: Feature follows user-centric design principles.

---

### ✅ VII. Test Coverage & Quality Gates

**Evaluation**: Feature requires comprehensive test coverage for financial calculations (apportionment factors, sourcing rules, throwback adjustments).

**Implementation**:
- Unit tests: 100% coverage of service layer (ApportionmentService, SourcingService, ThrowbackService, NexusService)
- Integration tests: Spring Boot Test with TestContainers PostgreSQL for multi-state scenarios
- Contract tests: API contracts for tax-engine-service endpoints (Schedule Y filing, apportionment calculation)
- E2E tests: Playwright workflow: Enter property/payroll/sales → Apply elections → Calculate apportionment → Verify breakdown

**Quality Gates**:
- Build fails if test coverage <80%
- Build fails if integration tests fail (apportionment scenarios)
- Manual QA checklist: Test Joyce vs Finnigan calculations, throwback adjustments, market-based sourcing, multi-jurisdiction allocation

**No Violations**: Feature includes comprehensive testing strategy.

---

## Constitution Violations Summary

**Total Violations**: 0 (Zero)

**Warnings**: 0 (Zero)

**Status**: ✅ **APPROVED** - Feature complies with all constitution principles. Proceed to Phase 0 research.

### Constitution Check: Post-Design Re-Evaluation (Phase 1 Complete)

**Re-evaluation Date**: [To be completed after Phase 1]  
**Artifacts Reviewed**: data-model.md, contracts/, quickstart.md

**Status**: ⏳ PENDING PHASE 1 COMPLETION

## Project Structure

### Documentation (this feature)

```text
specs/copilot/add-schedule-y-sourcing-feature/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Backend: Extend tax-engine-service (Spring Boot microservice)
backend/tax-engine-service/
├── src/main/java/com/munitax/taxengine/
│   ├── domain/
│   │   ├── apportionment/
│   │   │   ├── ScheduleY.java                      # JPA entity (FR-001 to FR-005)
│   │   │   ├── PropertyFactor.java                 # JPA entity (FR-027 to FR-031)
│   │   │   ├── PayrollFactor.java                  # JPA entity (FR-032 to FR-036)
│   │   │   ├── SalesFactor.java                    # JPA entity (FR-037 to FR-042)
│   │   │   ├── SaleTransaction.java                # JPA entity (transaction-level sourcing)
│   │   │   ├── NexusTracking.java                  # JPA entity (FR-016, throwback determination)
│   │   │   └── ApportionmentAuditLog.java          # Audit trail (Constitution III)
│   ├── repository/
│   │   ├── ScheduleYRepository.java                # Spring Data JPA
│   │   ├── PropertyFactorRepository.java
│   │   ├── PayrollFactorRepository.java
│   │   ├── SalesFactorRepository.java
│   │   ├── SaleTransactionRepository.java
│   │   ├── NexusTrackingRepository.java
│   │   └── ApportionmentAuditLogRepository.java
│   ├── service/
│   │   ├── ApportionmentService.java               # Business logic: calculate apportionment (FR-001 to FR-005)
│   │   ├── PropertyFactorService.java              # Business logic: property factor calculation (FR-027 to FR-031)
│   │   ├── PayrollFactorService.java               # Business logic: payroll factor calculation (FR-032 to FR-036)
│   │   ├── SalesFactorService.java                 # Business logic: sales factor calculation (FR-037 to FR-042)
│   │   ├── SourcingService.java                    # Business logic: sales sourcing (market-based, cost-of-performance)
│   │   ├── ThrowbackService.java                   # Business logic: throwback/throwout rules (FR-011 to FR-016)
│   │   ├── NexusService.java                       # Business logic: nexus determination (FR-016)
│   │   ├── FormulaConfigService.java               # Integration: retrieve formulas from rule engine
│   │   └── WithholdingIntegrationService.java      # Integration: get payroll data from Spec 1
│   ├── controller/
│   │   ├── ScheduleYController.java                # REST API: POST /api/schedule-y, GET /api/schedule-y/{id}
│   │   ├── ApportionmentController.java            # REST API: POST /api/apportionment/calculate
│   │   └── NexusController.java                    # REST API: GET /api/nexus/{businessId}
│   └── dto/
│       ├── ScheduleYRequest.java                   # Request DTO
│       ├── ScheduleYResponse.java                  # Response DTO
│       ├── ApportionmentBreakdownDto.java          # Breakdown display (FR-043 to FR-045)
│       └── NexusStatusDto.java                     # Nexus status by state
└── src/test/java/com/munitax/taxengine/
    ├── service/
    │   ├── ApportionmentServiceTest.java           # Unit tests (all formula types)
    │   ├── SourcingServiceTest.java                # Unit tests (Joyce vs Finnigan, market-based)
    │   ├── ThrowbackServiceTest.java               # Unit tests (FR-011 to FR-016 scenarios)
    │   └── NexusServiceTest.java                   # Unit tests (nexus determination)
    └── integration/
        └── ScheduleYIntegrationTest.java           # TestContainers + full apportionment workflow

# Backend: Integration with pdf-service (existing)
backend/pdf-service/
├── src/main/resources/templates/
│   └── schedule-y-template.html                    # Thymeleaf template for Schedule Y PDF (FR-046)

# Backend: Integration with rule-engine-service (existing, from Spec 4)
backend/rule-engine-service/
# (No code changes - store apportionment formulas as rules)

# Frontend: React SPA (extend existing app)
src/
├── components/
│   ├── apportionment/
│   │   ├── ScheduleYWizard.tsx                     # Multi-step Schedule Y form (US-1, US-2, US-3)
│   │   ├── PropertyFactorForm.tsx                  # Property factor entry (FR-027 to FR-031)
│   │   ├── PayrollFactorForm.tsx                   # Payroll factor entry (FR-032 to FR-036)
│   │   ├── SalesFactorForm.tsx                     # Sales factor entry (FR-037 to FR-042)
│   │   ├── SourcingElectionPanel.tsx               # Joyce vs Finnigan election (FR-006 to FR-010)
│   │   ├── ThrowbackElectionPanel.tsx              # Throwback vs Throwout election (FR-013)
│   │   ├── ServiceSourcingPanel.tsx                # Market-based vs Cost-of-performance (FR-017 to FR-022)
│   │   ├── ApportionmentBreakdownCard.tsx          # Display factor breakdown (US-4, FR-043 to FR-045)
│   │   ├── ApportionmentChart.tsx                  # Visualization with Recharts
│   │   ├── NexusTrackingPanel.tsx                  # Nexus status by state (FR-016)
│   │   ├── ScheduleYHistory.tsx                    # Table showing all Schedule Y filings
│   │   └── ScheduleYPdfViewer.tsx                  # PDF preview/download (FR-046)
│   └── shared/
│       ├── FactorPercentageDisplay.tsx             # Reusable factor display component
│       └── ElectionTooltip.tsx                     # Explain elections (Joyce/Finnigan, throwback)
├── services/
│   ├── scheduleYService.ts                         # API client: POST /api/schedule-y, GET /api/schedule-y
│   ├── apportionmentService.ts                     # API client: POST /api/apportionment/calculate
│   └── nexusService.ts                             # API client: GET /api/nexus/{businessId}
├── hooks/
│   ├── useScheduleY.ts                             # React Query hook for Schedule Y
│   ├── useApportionment.ts                         # React Query hook for apportionment calculation
│   └── useNexus.ts                                 # React Query hook for nexus status
└── types/
    ├── apportionment.ts                            # TypeScript types: ScheduleY, PropertyFactor, PayrollFactor, SalesFactor
    ├── sourcing.ts                                 # Enum: FINNIGAN, JOYCE, MARKET_BASED, COST_OF_PERFORMANCE
    └── nexus.ts                                    # NexusStatus, NexusReason types

# Frontend Tests
src/
└── __tests__/
    ├── components/
    │   ├── ScheduleYWizard.test.tsx                # Component tests (Vitest + RTL)
    │   ├── ApportionmentBreakdownCard.test.tsx     # Component tests
    │   └── SourcingElectionPanel.test.tsx          # Component tests
    └── e2e/
        └── schedule-y-apportionment.spec.ts        # Playwright E2E: Full apportionment workflow

# Database Migrations (Flyway - tax-engine-service)
backend/tax-engine-service/src/main/resources/db/migration/
├── V1.30__create_schedule_y_table.sql              # CREATE TABLE schedule_y (tenant-scoped)
├── V1.31__create_property_factor_table.sql
├── V1.32__create_payroll_factor_table.sql
├── V1.33__create_sales_factor_table.sql
├── V1.34__create_sale_transaction_table.sql
├── V1.35__create_nexus_tracking_table.sql
└── V1.36__create_apportionment_audit_log_table.sql
```

**Structure Decision**: 
- **Backend**: Extend existing tax-engine-service (owns tax calculations and returns domain). No new microservice needed (Constitution I: "add to existing services if they share domain concerns").
- **Frontend**: Extend existing React SPA with new apportionment components. No separate frontend app needed.
- **Database**: New tables in tax-engine-service database, tenant-scoped per Constitution II.
- **Integration**: Leverage existing pdf-service (PDF generation), rule-engine-service (formula configuration, from Spec 4), withholding reconciliation (payroll data, from Spec 1).

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

#### R1: Apportionment Formula Configuration and Rule Engine Integration

**Question**: How should system retrieve and apply apportionment formulas from the rule engine (Spec 4)? What formula variations need to be supported?

**Context**: Different municipalities may use different apportionment formulas:
- Traditional three-factor (property, payroll, sales) equally weighted: (P + PY + S) / 3
- Four-factor with double-weighted sales (Ohio default): (P + PY + S + S) / 4
- Single-sales-factor: S / 1
- Custom weightings: e.g., (0.5 * S + 0.25 * P + 0.25 * PY)

Rule engine must provide formula configuration based on tenant, tax year, and entity type.

**Acceptance**: Research document must answer:
1. What is the rule engine API contract for retrieving apportionment formulas? (Check Spec 4 design)
2. How to represent formula weights in database? (JSONB vs separate columns)
3. How to handle formula changes mid-year? (e.g., municipality changes formula on July 1)
4. How to support custom formulas with arbitrary weights? (e.g., 40% sales, 30% property, 30% payroll)
5. Performance: Cache formula configuration per tenant? (Redis TTL strategy)

**Dependencies**: Review Spec 4 rule engine design (if available), test with PostgreSQL JSONB queries.

---

#### R2: Nexus Determination Logic and Economic Nexus Thresholds

**Question**: What is the algorithm for determining nexus in each state/municipality for throwback rule application? How to handle economic nexus thresholds that vary by state?

**Context**: Nexus is established when:
- Physical presence (office, warehouse, property)
- Employee presence (employees working in state)
- Economic nexus (sales > $500K or 200 transactions, post-Wayfair)
- Factor presence (P.L. 86-272 substantial presence)

Each state has different economic nexus thresholds (e.g., Ohio: $500K, California: $500K, New York: $1M).

**Acceptance**: Research document must provide:
1. Nexus determination algorithm (pseudocode) that evaluates all 4 nexus types
2. Decision on economic nexus threshold storage (rule engine? database table? hardcoded?)
3. How to track nexus changes mid-year (business opens office on July 1)
4. How to handle affiliate nexus (parent has nexus, does subsidiary automatically have nexus?)
5. Test cases: 10 scenarios covering all nexus types

**Dependencies**: Research state nexus rules (Multistate Tax Commission guidelines), confirm municipality-specific thresholds.

---

#### R3: Market-Based Sourcing Cascading Rules for Service Revenue

**Question**: How should system implement cascading sourcing rules when customer location is unknown? What are the fallback mechanisms?

**Context**: Market-based sourcing requires customer location (where benefit is received). But:
- Customer location may be unknown (data not collected)
- Customer may be multi-location (benefit received in multiple states)
- Customer may be mobile (individual consumer traveling)

Cascading rules:
1. First attempt: Market-based (customer location)
2. If unknown: Cost-of-performance (employee location)
3. If both unknown: Pro-rata based on overall apportionment percentage

**Acceptance**: Research document must provide:
1. Cascading algorithm (pseudocode) with all fallback steps
2. Decision on how to prompt user for customer location (UI flow)
3. How to handle multi-location customers (e.g., Fortune 500 with offices in all 50 states)
4. How to prorate service revenue by employee location when using cost-of-performance
5. Data model: Store sourcing method used for each transaction (for audit trail)
6. Test cases: 8 scenarios covering all cascading fallback scenarios

**Dependencies**: Review modern state sourcing rules (UDITPA Section 17, MTC model apportionment regulation).

---

#### R4: Throwback vs Throwout Implementation and Performance

**Question**: What is the optimal implementation approach for throwback/throwout rules? How to efficiently determine if destination state has nexus for each sale transaction?

**Context**: For each sale transaction, system must:
1. Determine origin state (where goods shipped from)
2. Determine destination state (where goods delivered)
3. Check if business has nexus in destination state
4. If no nexus: Apply throwback (add to origin numerator) or throwout (exclude from both numerator and denominator)

Performance concern: Business with 1000 sales transactions requires 1000 nexus checks. Need efficient lookup.

**Acceptance**: Research document must provide:
1. Throwback/throwout algorithm (pseudocode)
2. Nexus lookup optimization strategy (cache nexus status? pre-compute per state? database index?)
3. Decision on throwback vs throwout: municipality election or per-transaction override?
4. How to handle sales to federal government (not a "state" for throwback)
5. How to handle sales originating from multi-state operations (manufactured in OH, warehoused in PA, shipped from PA)
6. Performance benchmark: 1000 transactions with nexus checks < 2 seconds
7. Test cases: 10 edge case scenarios

**Dependencies**: Test with PostgreSQL + Redis for nexus caching, benchmark nexus lookup performance.

---

#### R5: Payroll Factor Integration with Withholding Reconciliation (Spec 1)

**Question**: How should Schedule Y integrate with withholding reconciliation (Spec 1) to automatically populate payroll factor data?

**Context**: Payroll factor requires:
- Total Ohio payroll (W-2 wages + 1099-NEC + officer compensation)
- Total payroll everywhere (all states)
- Employee count per state
- Remote employee allocation

Withholding reconciliation (Spec 1) already collects quarterly W-1 filings with payroll data. Can Schedule Y leverage this data?

**Acceptance**: Research document must answer:
1. What is the data model relationship between W1Filing (Spec 1) and PayrollFactor (Spec 5)?
2. Can payroll factor auto-populate from cumulative W-1 totals? (accuracy concerns: W-1 is Ohio-only, need multi-state data)
3. How to handle discrepancies between W-1 payroll and Schedule Y payroll? (different reporting periods, amendments)
4. UI flow: Show pre-filled payroll factor with option to override/adjust?
5. Audit trail: Log when payroll factor is auto-populated vs manually entered
6. Test cases: Auto-population from quarterly W-1s, handle amended W-1 cascade updates

**Dependencies**: Review Spec 1 data model (W1Filing, CumulativeWithholdingTotals entities), test integration with existing withholding data.

---

#### R6: PDF Generation for Schedule Y with Complex Breakdown

**Question**: How should PDF generation handle complex apportionment breakdown with factor details, elections, and multi-year comparison?

**Context**: Schedule Y PDF (Form 27-Y) must include:
- Property factor breakdown (real property, tangible personal property, rented property)
- Payroll factor breakdown (W-2 wages, contractor payments, officer compensation, by state)
- Sales factor breakdown (tangible goods, services, rental income, interest, royalties, by state)
- Throwback adjustments (line items per state)
- Sourcing method elections (Joyce/Finnigan, throwback/throwout, market-based/cost-of-performance)
- Multi-year comparison (current year, prior year, 3-year average)

Standard PDF template may not accommodate variable-length transaction lists.

**Acceptance**: Research document must provide:
1. Thymeleaf template design for Schedule Y (HTML structure)
2. How to handle variable-length transaction lists (pagination? truncation? separate appendix?)
3. Chart/graph rendering in PDF (Recharts server-side rendering? PDF library support?)
4. PDF file size estimation (1000 transactions = X MB?)
5. Performance target: PDF generation < 5 seconds for 1000 transactions
6. Fallback: If PDF too large, generate CSV export instead?

**Dependencies**: Review existing pdf-service capabilities, test Thymeleaf template rendering with large datasets.

---

### Research Deliverables

**research.md** file must include:

1. **Executive Summary**: 1-paragraph summary of all research findings and recommendations
2. **R1: Apportionment Formula Configuration**: Decision on rule engine integration, formula representation, caching strategy
3. **R2: Nexus Determination Logic**: Algorithm pseudocode, threshold storage decision, edge case handling
4. **R3: Market-Based Sourcing Cascading**: Cascading algorithm, UI flow, multi-location customer handling
5. **R4: Throwback vs Throwout Implementation**: Algorithm pseudocode, performance optimization, benchmark results
6. **R5: Payroll Factor Integration**: Data model relationship, auto-population strategy, discrepancy handling
7. **R6: PDF Generation**: Template design, variable-length handling, performance target validation
8. **Technology Decisions Summary**: Table with [Decision, Rationale, Alternatives Considered]

**Acceptance Criteria for Phase 0 Completion**:
- All NEEDS CLARIFICATION items in Technical Context are resolved (none currently)
- All 6 research tasks (R1-R6) have documented decisions with rationale
- Technology choices are concrete (no "or" options, no TBD)
- Constitution Check re-evaluated (all warnings addressed)

---

## Phase 1: Design & Contracts

**Status**: NOT STARTED  
**Output Files**: `data-model.md`, `contracts/`, `quickstart.md`

### Phase 1 Deliverables

1. **data-model.md**:
   - 7 entities: ScheduleY, PropertyFactor, PayrollFactor, SalesFactor, SaleTransaction, NexusTracking, ApportionmentAuditLog
   - Full field definitions (all attributes from spec entities)
   - Relationships mapped (foreign keys, indexes)
   - Validation constraints (CHECK, UNIQUE, NOT NULL)
   - Flyway migration plan (V1.30-V1.36)
   - Performance considerations (query optimization, cache strategy for nexus lookups)
   - Data retention policy (7 years per IRS requirement)

2. **contracts/**:
   - **api-schedule-y.yaml** (OpenAPI 3.0): 6+ endpoints
     - POST /api/schedule-y (create/file Schedule Y)
     - GET /api/schedule-y (list Schedule Y filings with pagination)
     - GET /api/schedule-y/{id} (get Schedule Y details)
     - POST /api/schedule-y/{id}/amend (file amended Schedule Y)
     - GET /api/schedule-y/{id}/breakdown (get apportionment breakdown)
     - GET /api/schedule-y/{id}/pdf (generate Schedule Y PDF)
   - **api-apportionment.yaml** (OpenAPI 3.0): 5+ endpoints
     - POST /api/apportionment/calculate (calculate apportionment percentage)
     - POST /api/apportionment/sourcing (determine sales sourcing)
     - POST /api/apportionment/throwback (apply throwback/throwout rules)
     - GET /api/nexus/{businessId} (get nexus status by state)
     - POST /api/nexus/{businessId}/update (update nexus status)
   - **event-schedule-y.yaml** (AsyncAPI 2.6): 5+ event channels
     - apportionment/schedule-y-filed (triggers downstream updates)
     - apportionment/schedule-y-amended (triggers recalculation)
     - apportionment/election-changed (Joyce/Finnigan, throwback/throwout)
     - apportionment/nexus-changed (nexus status updated)
     - apportionment/audit-log (immutable audit trail)

3. **quickstart.md**:
   - Environment setup (Docker, PostgreSQL, Redis, JWT auth, rule engine)
   - 8+ API examples with curl commands (Schedule Y filing, apportionment calculation, nexus tracking)
   - Database queries (SQL examples for apportionment breakdown)
   - Test execution (unit, integration, E2E)
   - Flyway migration guide
   - Event-driven architecture (Redis Pub/Sub, Kafka)
   - Performance testing (benchmark apportionment calculation with 1000 transactions)
   - Troubleshooting guide (8+ common issues)

4. **Update agent context**: Run `.specify/scripts/powershell/update-agent-context.ps1 -AgentType copilot`
   - Add apportionment domain knowledge to agent context
   - Include sourcing method patterns (Joyce/Finnigan, market-based)
   - Include nexus determination patterns
   - Preserve manual additions between markers

### Phase 1 Acceptance Criteria

- data-model.md includes 7 entities (ScheduleY, PropertyFactor, PayrollFactor, SalesFactor, SaleTransaction, NexusTracking, ApportionmentAuditLog)
- contracts/ includes 3 files: api-schedule-y.yaml (6+ endpoints), api-apportionment.yaml (5+ endpoints), event-schedule-y.yaml (5+ channels)
- quickstart.md includes 8+ curl examples and comprehensive troubleshooting
- Constitution Check re-evaluated post-design (verify 0 violations)
- Agent context updated with apportionment domain patterns

---

## Phase 2: Task Breakdown (NOT part of /speckit.plan)

**Status**: NOT STARTED  
**Output File**: `tasks.md` (generated by separate `/speckit.tasks` command)

Phase 2 is NOT included in this plan. After Phase 0 and Phase 1 complete, run `/speckit.tasks` to generate implementation tasks organized by user story.

---

## Next Steps

1. **Immediate**: Execute Phase 0 research tasks (R1-R6)
2. **Generate research.md**: Document all findings, decisions, rationale
3. **Checkpoint**: Review research.md with team, validate technology choices
4. **Proceed to Phase 1**: Design data model, API contracts, quickstart guide
5. **Final checkpoint**: Constitution Check post-design
6. **Output**: Deliver plan.md, research.md, data-model.md, contracts/, quickstart.md to implementation team

**Estimated Timeline**:
- Phase 0 (Research): 3-4 days (6 research tasks, some require benchmarking)
- Phase 1 (Design): 3-4 days (7 entities, 11+ API endpoints, complex PDF template)
- **Total Planning**: 6-8 days before implementation begins

---

**Plan Status**: ✅ PHASE 0 READY - Proceed with research tasks (R1-R6)
