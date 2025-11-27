# Implementation Plan: Complete Withholding Reconciliation System

**Branch**: `1-withholding-reconciliation` | **Date**: 2024-11-28 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/1-withholding-reconciliation/spec.md`

## Summary

Implement comprehensive W-1 withholding reconciliation system enabling businesses to file quarterly/monthly/daily withholding returns with automatic cumulative tracking, year-end W-2/W-3 reconciliation, discrepancy detection, and penalty calculations. System validates filing patterns, projects annual totals, and prevents next-year filing until prior-year reconciliation completes. Integrates with existing AI extraction service for W-2 data and payment gateway for liability tracking.

**Primary Requirement**: Year-end reconciliation comparing cumulative W-1 filings to W-2/W-3 totals with automated discrepancy detection and resolution workflow.

**Technical Approach**: Extend tax-engine-service with withholding domain models (W1Filing, WithholdingReconciliation, CumulativeWithholdingTotals), implement event-driven cumulative calculation updates, leverage AI extraction for W-2 Box 18/19 data, and add reconciliation dashboard UI components to existing React frontend.

---

## Technical Context

**Language/Version**: 
- **Backend**: Java 21 with Spring Boot 3.2.3
- **Frontend**: TypeScript 5.x with React 18.2, Node.js 20.x
- **Build**: Maven 3.9+ (backend), Vite 5.x (frontend)

**Primary Dependencies**:
- **Backend**: Spring Data JPA, Spring Web, Spring Cloud (Eureka client), PostgreSQL driver, Jackson (JSON), Lombok
- **Frontend**: React Router 6.x, Axios, Tailwind CSS 3.x, date-fns (date handling)
- **Testing**: JUnit 5, Mockito (backend), Vitest, React Testing Library (frontend)

**Storage**: 
- PostgreSQL 16 with multi-tenant schemas (tenant-scoped w1_filings, withholding_reconciliations, cumulative_withholding_totals tables)
- Redis 7 for caching cumulative totals and reducing recalculation overhead

**Testing**:
- Backend: JUnit 5 + Mockito for service layer, Spring Boot Test for integration tests, TestContainers for PostgreSQL
- Frontend: Vitest + React Testing Library for component tests, Playwright for E2E reconciliation workflow

**Target Platform**: 
- Docker containers deployed via docker-compose (development) and Kubernetes (production)
- Web browsers: Chrome/Edge 100+, Firefox 100+, Safari 15+ (desktop and mobile)

**Project Type**: Web application with microservices backend (9 services) and React SPA frontend

**Performance Goals**:
- W-1 filing submission and cumulative calculation: <2 seconds (FR-001, Success Criteria)
- Year-end reconciliation for 52 W-1 filings: <10 seconds (Success Criteria)
- Dashboard load time: <1 second for businesses with <100 W-1 filings

**Constraints**:
- Multi-tenant data isolation (Constitution II): All queries scoped to tenant schema via tenant context
- Audit trail immutability (Constitution III): All W-1 filings, amendments, reconciliation actions logged immutably
- Must integrate with existing extraction-service (Gemini AI) for W-2 parsing
- Must integrate with existing payment gateway for W-1 payment tracking

**Scale/Scope**:
- Target: 5,000 businesses per municipality, 4-52 W-1 filings per business per year
- ~100K W-1 filings per year per municipality (200K cumulative calculation updates)
- Year-end reconciliation: ~5K reconciliations per municipality (January spike)

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ I. Microservices Architecture First

**Evaluation**: Feature extends existing **tax-engine-service** (owns tax calculations, filings, returns). W-1 withholding is core tax domain logic. No new service required.

**Service Placement**: 
- W-1 filing logic → tax-engine-service (domain: withholding filings)
- W-2 extraction → extraction-service (existing AI service)
- Payment tracking → submission-service + payment gateway (existing)
- Frontend → React SPA (existing)

**No Violations**: Feature properly extends existing service boundaries.

---

### ✅ II. Multi-Tenant Data Isolation (NON-NEGOTIABLE)

**Evaluation**: All database entities (W1Filing, WithholdingReconciliation, CumulativeWithholdingTotals) MUST include tenant_id foreign key. All JPA queries MUST use tenant-scoped schemas.

**Implementation**:
- Tenant context from JWT (existing auth-service integration)
- PostgreSQL schema-per-tenant (dublin.w1_filings, columbus.w1_filings)
- JPA @Filter annotation for automatic tenant scoping

**No Violations**: Feature complies with tenant isolation requirements.

---

### ✅ III. Audit Trail Immutability

**Evaluation**: W-1 filings are financial records subject to 7-year retention (IRS IRC § 6001). Reconciliation decisions (accept/reject discrepancy) are audit-critical.

**Implementation**:
- W1Filing entity: created_at, created_by (user ID), never deleted (soft delete flag if needed)
- Amended W-1s: new W1Filing record with amends_filing_id reference to original (both preserved)
- WithholdingReconciliation entity: status changes logged with timestamp, actor, reason
- Audit log table: withholding_audit_log (action, entity_type, entity_id, old_value, new_value, actor, timestamp)

**No Violations**: Feature implements immutable audit trails per constitution.

---

### ⚠️ IV. AI Transparency & Explainability

**Evaluation**: Feature depends on AI extraction of W-2 Box 18 (wages) and Box 19 (tax withheld) from uploaded PDFs.

**Existing Coverage**: extraction-service already provides:
- Bounding box coordinates for extracted fields (Constitution IV requirement)
- Confidence scores per field (existing)
- Human override capability (existing UI)

**Gap Identified**: "Ignored Items Report" for uploaded W-2s not used in reconciliation.

**Mitigation**: Phase 1 will design ignored W-2 report showing:
- Which W-2 PDFs were uploaded but not matched to business EIN
- Why excluded (wrong EIN, duplicate, corrupted file, unsupported format)
- Action button: "Re-upload W-2" or "Override EIN match"

**Action Required**: Research task in Phase 0 to design ignored W-2 detection logic.

---

### ✅ V. Security & Compliance First

**Evaluation**: W-1 filings contain sensitive data (EIN, employee count, wage totals). W-2s contain SSN, wages (highly sensitive).

**Implementation**:
- Authentication: JWT required (existing auth-service)
- Authorization: ROLE_BUSINESS (file W-1, view own reconciliation), ROLE_AUDITOR (view all reconciliations, approve discrepancies)
- Encryption: SSN/EIN encrypted at rest (existing encryption layer in database)
- Logging: No SSN in logs (existing log sanitization)
- TLS 1.3: All production traffic (existing infrastructure)

**Compliance**: IRS Publication 1075 (federal tax info safeguarding), Ohio R.C. 718 (municipal tax confidentiality)

**No Violations**: Feature leverages existing security infrastructure.

---

### ✅ VI. User-Centric Design

**Evaluation**: Feature UI must simplify complex reconciliation workflow for business owners (non-tax-experts).

**Implementation**:
- Progressive disclosure: Dashboard shows "✓ Reconciled" or "⚠️ Discrepancy requires resolution" (simple status)
- Drill-down: Click status to see detailed reconciliation report (W-1 vs W-2 totals, variance, resolution options)
- Error prevention: Real-time validation on W-1 filing (e.g., "Q2 wages 70% lower than Q1 - verify payroll data")
- Wizards: Multi-step W-1 filing wizard (Enter wages → Review → File)
- Mobile-first: W-1 filing form responsive (375px+ width), reconciliation dashboard table horizontal scroll on mobile

**No Violations**: Feature follows user-centric design principles.

---

### ✅ VII. Test Coverage & Quality Gates

**Evaluation**: Feature requires comprehensive test coverage for financial calculations (cumulative totals, penalty calculations, reconciliation variance).

**Implementation**:
- Unit tests: 100% coverage of service layer (W1FilingService, ReconciliationService, PenaltyCalculationService)
- Integration tests: Spring Boot Test with TestContainers PostgreSQL for multi-filing scenarios
- Contract tests: API contracts for tax-engine-service endpoints (W-1 filing, reconciliation)
- E2E tests: Playwright workflow: File 4 quarterly W-1s → Upload W-2s → Reconcile → Verify status

**Quality Gates**:
- Build fails if test coverage <80%
- Build fails if integration tests fail (reconciliation scenarios)
- Manual QA checklist: Test amended W-1 cascade updates, penalty calculations, year-end reconciliation for all filing frequencies

**No Violations**: Feature includes comprehensive testing strategy.

---

## Constitution Violations Summary

**Total Violations**: 0 (Zero)

**Warnings**: 1 (Ignored W-2 Report - addressed in Phase 0 research)

**Status**: ✅ **APPROVED** - Feature complies with all constitution principles. Proceed to Phase 0 research.

### Constitution Check: Post-Design Re-Evaluation (Phase 1 Complete)

**Re-evaluation Date**: Phase 1 completion  
**Artifacts Reviewed**: data-model.md (6 entities), contracts/ (3 API files), quickstart.md

**Violations**: 0  
**Warnings**: 0

**Detailed Assessment**:

1. ✅ **Principle I: Microservices** - Design extends tax-engine-service (not new service)
   - W1Filing, CumulativeWithholdingTotals, WithholdingReconciliation entities added to tax-engine-service domain
   - No new microservice introduced

2. ✅ **Principle II: Multi-Tenant Isolation** - All 6 entities have tenant_id NOT NULL
   - data-model.md confirms: W1Filing, CumulativeWithholdingTotals, WithholdingReconciliation, IgnoredW2, WithholdingPayment, WithholdingAuditLog all include tenant_id column
   - Indexes include tenant_id for query isolation (idx_w1_business_year, idx_cumulative_business_year)

3. ✅ **Principle III: Audit Trail** - WithholdingAuditLog entity is @Immutable with 7-year retention
   - data-model.md shows WithholdingAuditLog with oldValue/newValue JSONB columns
   - event-w1-filed.yaml defines withholding/audit-log channel with 365-day Kafka retention

4. ✅ **Principle IV: AI Transparency** - IgnoredW2 entity tracks extraction confidence + ignored reason
   - data-model.md: IgnoredW2 entity with metadata JSONB field (stores extraction confidence)
   - api-reconciliation.yaml: IgnoredW2Detail schema includes extractionConfidence (0-1 decimal), reason enum (WRONG_EIN, DUPLICATE, EXTRACTION_ERROR, INCOMPLETE_DATA)
   - Ignored W-2 Report (GET /reconciliations/{id}/ignored-w2s) provides full transparency to business owners

5. ✅ **Principle V: Security** - JWT authentication on all endpoints
   - api-w1-filing.yaml: bearerAuth (HTTP bearer JWT) security scheme on all 6 endpoints
   - api-reconciliation.yaml: bearerAuth on all 6 endpoints
   - Error responses include 401 Unauthorized, 403 Forbidden

6. ✅ **Principle VI: User-Centric Design** - Comprehensive error handling with field-level validation
   - api-w1-filing.yaml: ValidationErrorResponse schema with field, code, message properties
   - api-reconciliation.yaml: 422 validation errors for invalid state transitions
   - quickstart.md Section 8: Troubleshooting guide with 4 common scenarios and solutions

7. ✅ **Principle VII: Test Coverage** - Comprehensive test strategy in quickstart.md
   - Section 4: Unit tests (W1FilingServiceTest, CumulativeCalculationServiceTest, PenaltyCalculationServiceTest)
   - Section 4: Integration tests (WithholdingReconciliationIntegrationTest with TestContainers)
   - Section 4: Frontend tests (Vitest component tests, Playwright E2E tests)

**Conclusion**: Phase 1 design introduces 0 new violations. All constitution principles satisfied. Ready for implementation.

---

## Project Structure

### Documentation (this feature)

```text
specs/1-withholding-reconciliation/
├── plan.md              # This file (/speckit.plan command output) ✅ COMPLETE
├── research.md          # Phase 0 output ✅ COMPLETE (554 lines, R1-R5 answered)
├── data-model.md        # Phase 1 output ✅ COMPLETE (1,200+ lines, 6 entities)
├── quickstart.md        # Phase 1 output ✅ COMPLETE (450+ lines, 11 sections)
├── contracts/           # Phase 1 output ✅ COMPLETE (3 files)
│   ├── api-w1-filing.yaml          # OpenAPI spec for W-1 filing endpoints (6 endpoints)
│   ├── api-reconciliation.yaml     # OpenAPI spec for reconciliation endpoints (6 endpoints)
│   └── event-w1-filed.yaml         # AsyncAPI event: 6 channels (w1-filed, w1-amended, reconciliation-completed, etc.)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Backend: Extend tax-engine-service (Spring Boot microservice)
backend/tax-engine-service/
├── src/main/java/com/munitax/taxengine/
│   ├── domain/
│   │   ├── withholding/
│   │   │   ├── W1Filing.java                    # JPA entity (FR-001)
│   │   │   ├── WithholdingReconciliation.java   # JPA entity (FR-006, FR-009)
│   │   │   ├── CumulativeWithholdingTotals.java # JPA entity (FR-002)
│   │   │   ├── WithholdingPayment.java          # JPA entity (FR-020)
│   │   │   └── WithholdingAuditLog.java         # Audit trail (Constitution III)
│   ├── repository/
│   │   ├── W1FilingRepository.java              # Spring Data JPA
│   │   ├── WithholdingReconciliationRepository.java
│   │   └── CumulativeWithholdingTotalsRepository.java
│   ├── service/
│   │   ├── W1FilingService.java                 # Business logic: file W-1, amend W-1 (FR-001, FR-003)
│   │   ├── ReconciliationService.java           # Business logic: reconcile W-1 vs W-2 (FR-006, FR-007)
│   │   ├── CumulativeCalculationService.java    # Business logic: update YTD totals (FR-002)
│   │   ├── PenaltyCalculationService.java       # Business logic: late filing penalties (FR-011, FR-012)
│   │   └── W2ExtractionIntegrationService.java  # Call extraction-service for W-2 data (FR-014)
│   ├── controller/
│   │   ├── W1FilingController.java              # REST API: POST /api/w1-filings, GET /api/w1-filings/{id}
│   │   └── ReconciliationController.java        # REST API: POST /api/reconciliations, GET /api/reconciliations/{year}
│   └── dto/
│       ├── W1FilingRequest.java                 # Request DTO
│       ├── W1FilingResponse.java                # Response DTO
│       └── ReconciliationReportDto.java         # Reconciliation report DTO (FR-007)
└── src/test/java/com/munitax/taxengine/
    ├── service/
    │   ├── W1FilingServiceTest.java             # Unit tests
    │   ├── ReconciliationServiceTest.java       # Unit tests (cover all FR-006 scenarios)
    │   └── CumulativeCalculationServiceTest.java # Unit tests (FR-002, FR-003 cascade)
    └── integration/
        └── WithholdingReconciliationIntegrationTest.java # TestContainers + full workflow

# Backend: No changes needed to extraction-service (existing W-2 extraction capability)
backend/extraction-service/
# (No changes - already extracts W-2 Box 18, Box 19)

# Frontend: React SPA (extend existing app)
src/
├── components/
│   ├── withholding/
│   │   ├── W1FilingWizard.tsx                   # Multi-step W-1 filing form (US-1)
│   │   ├── W1FilingHistory.tsx                  # Table showing all W-1 filings (FR-001)
│   │   ├── CumulativeTotalsCard.tsx             # Display YTD totals (FR-002, Success Criteria)
│   │   ├── ReconciliationDashboard.tsx          # Main reconciliation UI (US-2, FR-015)
│   │   ├── ReconciliationReportModal.tsx        # Detailed reconciliation report (FR-007)
│   │   ├── AmendW1Form.tsx                      # Amended W-1 filing form (US-3, FR-003)
│   │   └── IgnoredW2ReportModal.tsx             # Show W-2s not matched to EIN (Constitution IV)
│   └── shared/
│       ├── FilingFrequencySelector.tsx          # Dropdown: Daily, Semi-Monthly, Monthly, Quarterly (FR-013)
│       └── PenaltyCalculationTooltip.tsx        # Explain penalty calculation (User-Centric Design)
├── services/
│   ├── w1FilingService.ts                       # API client: POST /api/w1-filings, GET /api/w1-filings
│   ├── reconciliationService.ts                 # API client: POST /api/reconciliations, GET /api/reconciliations/{year}
│   └── extractionService.ts                     # Existing: call extraction-service for W-2 upload
├── hooks/
│   ├── useW1Filing.ts                           # React Query hook for W-1 filings
│   ├── useCumulativeTotals.ts                   # React Query hook for YTD totals (FR-002)
│   └── useReconciliation.ts                     # React Query hook for reconciliation data
└── types/
    ├── withholding.ts                           # TypeScript types: W1Filing, CumulativeTotals, Reconciliation
    └── reconciliationStatus.ts                  # Enum: NOT_STARTED, IN_PROGRESS, DISCREPANCY, RECONCILED

# Frontend Tests
src/
└── __tests__/
    ├── components/
    │   ├── W1FilingWizard.test.tsx              # Component tests (Vitest + RTL)
    │   └── ReconciliationDashboard.test.tsx     # Component tests
    └── e2e/
        └── withholding-reconciliation.spec.ts    # Playwright E2E: Full filing + reconciliation workflow

# Database Migrations (Flyway - tax-engine-service)
backend/tax-engine-service/src/main/resources/db/migration/
├── V1.20__create_w1_filings_table.sql           # CREATE TABLE w1_filings (tenant-scoped)
├── V1.21__create_withholding_reconciliation_table.sql
├── V1.22__create_cumulative_withholding_totals_table.sql
├── V1.23__create_withholding_payments_table.sql
└── V1.24__create_withholding_audit_log_table.sql
```

**Structure Decision**: 
- **Backend**: Extend existing tax-engine-service (owns tax calculations and filings domain). No new microservice needed (Constitution I: "add to existing services if they share domain concerns").
- **Frontend**: Extend existing React SPA with new withholding components. No separate frontend app needed.
- **Database**: New tables in tax-engine-service database, tenant-scoped per Constitution II.
- **Integration**: Leverage existing extraction-service (AI), payment gateway (submission-service), auth-service (JWT).

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

#### R1: Ignored W-2 Detection Logic (Constitution IV requirement)

**Question**: How should system identify uploaded W-2 PDFs that were not matched to business profile during reconciliation?

**Context**: Business uploads 15 W-2 PDFs. Extraction service extracts employer EIN from each W-2. 13 W-2s match business EIN (12-3456789), 2 W-2s have different EINs. User needs to see "2 W-2s ignored - different employer EIN".

**Acceptance**: Research document must answer:
1. Does extraction-service already provide employer EIN from W-2 extraction? (Check extraction-service API response format)
2. How to store "ignored W-2" metadata (separate table vs JSON field in reconciliation)?
3. UI design: Where to show ignored W-2 report (modal? dashboard section? both?)
4. Error scenarios: What if W-2 extraction fails entirely? (corrupted PDF, unsupported format)

**Dependencies**: Review extraction-service API contract, existing W-2 extraction response format.

---

#### R2: Cumulative Totals Performance Optimization

**Question**: What is optimal approach for cumulative YTD totals: real-time calculation on every query vs cached totals updated on W-1 filing?

**Context**: Dashboard displays YTD totals for each business. Business files 52 W-1s per year (weekly filer). Performance target: <1 second dashboard load.

**Trade-offs**:
- **Option A - Real-time**: Query SUM(wages) FROM w1_filings WHERE business_id = X AND year = 2024. Pros: Always accurate. Cons: 52 rows summed on every query (slow for high-volume filers).
- **Option B - Cached**: CumulativeWithholdingTotals table updated via event listener on W-1 filing. Pros: O(1) query. Cons: Event-driven complexity, potential cache invalidation bugs.
- **Option C - Hybrid**: Redis cache with 5-minute TTL, fall back to real-time calculation if cache miss. Pros: Fast + accurate. Cons: Redis dependency, cache warming strategy needed.

**Acceptance**: Research document must recommend approach with:
- Performance benchmark: simulate 5,000 businesses, 52 filings each, 10 concurrent dashboard loads
- Cache invalidation strategy (if Option B or C)
- Rollback plan if chosen approach has bugs

**Dependencies**: Test with existing PostgreSQL + Redis infrastructure.

---

#### R3: Amended W-1 Cascade Update Implementation

**Question**: How should system handle amended W-1 that changes earlier period (affects all subsequent cumulative totals)?

**Context**: Business files W-1 for Jan-May (5 filings). In June, they discover March was wrong. They file amended March W-1 with +$10,000 wages. System must recalculate cumulative totals for April, May, June.

**Scenarios**:
1. **Sequential update**: Update April cumulative (+$10K), then May (+$10K), then June (+$10K). Pros: Simple. Cons: Slow if many filings after amended period.
2. **Batch update**: Single SQL UPDATE cumulative_withholding_totals SET wages_ytd = wages_ytd + 10000 WHERE business_id = X AND period > '2024-03'. Pros: Fast. Cons: Less audit trail visibility.
3. **Event-driven**: Publish "W1Amended" event, consumer recalculates all subsequent periods asynchronously. Pros: Scalable. Cons: Eventual consistency (totals not updated immediately).

**Acceptance**: Research document must recommend approach with:
- Test scenario: Amend January W-1 when 52 weekly filings already exist (worst case: 51 cascading updates)
- Performance target: <5 seconds for cascade update
- Audit trail: How to log "Cumulative totals recalculated due to amended March W-1"

**Dependencies**: Test with PostgreSQL transaction isolation levels, consider database triggers vs application-level updates.

---

#### R4: Late Filing Penalty Edge Cases

**Question**: How should penalty calculation handle partial months, business registration date, and safe harbor exceptions?

**Context**: FR-011 specifies "5% per month, max 25%, minimum $50 if tax due > $200". But edge cases:
- Business files Q1 W-1 on May 10 (due date April 30). Is this 1 month late (5% penalty) or 0.33 months late (1.67% penalty)?
- Business registered on March 15. Q1 due date is April 30. Should penalty waiver apply for first filing?
- Business has $0 tax due (seasonal, no payroll). Should $50 minimum penalty apply?

**Acceptance**: Research document must provide:
- Penalty calculation algorithm (pseudocode)
- Decision on partial month rounding (round up? prorate?)
- Safe harbor exceptions (first-time filer waiver?)
- Test cases: 10 scenarios covering all edge cases

**Dependencies**: Review Ohio municipal tax code (ORC 718.27), confirm municipality-specific penalty rules (Dublin vs Columbus may differ).

---

#### R5: Multi-Frequency Filing Due Date Calculation

**Question**: How should system calculate due dates for daily, semi-monthly, monthly, and quarterly filers, considering weekends, holidays, and municipality-specific rules?

**Context**: FR-013 requires "daily, semi-monthly, monthly, and quarterly filing frequencies with appropriate due date calculations". Assumption states "Monthly = 15th of following month, Quarterly = 30 days after quarter end, Daily = Next business day". But:
- What if due date falls on Saturday? (Next business day = Monday)
- What if due date is a federal holiday (e.g., July 4th)?
- What if municipality has custom holiday calendar (local holiday)?

**Acceptance**: Research document must provide:
- Due date calculation algorithm for each frequency (pseudocode)
- Holiday calendar source (federal holidays? municipality-specific?)
- Test cases: 5 scenarios per frequency (20 total) including weekend/holiday edge cases
- Database storage: Store calculated due date on W1Filing record (avoid recalculation bugs)

**Dependencies**: Research federal holiday calendar (java.time.temporal.TemporalAdjusters), check if municipality-specific holiday calendar exists.

---

### Research Deliverables

**research.md** file must include:

1. **Executive Summary**: 1-paragraph summary of all research findings and recommendations
2. **R1: Ignored W-2 Detection**: Decision on approach, data model, UI mockup
3. **R2: Cumulative Totals Performance**: Benchmark results, chosen approach (A/B/C), cache invalidation strategy
4. **R3: Amended W-1 Cascade**: Chosen approach (sequential/batch/event-driven), performance test results, audit trail design
5. **R4: Late Filing Penalty**: Algorithm pseudocode, edge case decisions, test case table
6. **R5: Due Date Calculation**: Algorithm pseudocode per frequency, holiday calendar decision, test case table
7. **Technology Decisions Summary**: Table with [Decision, Rationale, Alternatives Considered]

**Acceptance Criteria for Phase 0 Completion**:
- All NEEDS CLARIFICATION items in Technical Context are resolved
- All 5 research tasks (R1-R5) have documented decisions with rationale
- Technology choices are concrete (no "or" options, no TBD)
- Constitution Check re-evaluated (all warnings addressed)

---

## Phase 1: Design & Contracts

**Status**: ✅ COMPLETE  
**Output Files**: `data-model.md`, `contracts/`, `quickstart.md`

### Phase 1 Deliverables

1. ✅ **data-model.md** (COMPLETE):
   - 6 entities: W1Filing, CumulativeWithholdingTotals, WithholdingReconciliation, IgnoredW2, WithholdingPayment, WithholdingAuditLog
   - Full field definitions (46 fields for W1Filing, 13 for CumulativeWithholdingTotals, etc.)
   - Relationships mapped (foreign keys, indexes)
   - Validation constraints (CHECK, UNIQUE, NOT NULL)
   - Flyway migration plan (V1.20-V1.27)
   - Performance considerations (query optimization, cache strategy)
   - Data retention policy (7 years per IRS requirement)

2. ✅ **contracts/** (COMPLETE):
   - **api-w1-filing.yaml** (OpenAPI 3.0): 5 endpoints
     - POST /api/w1-filings (file new W-1)
     - GET /api/w1-filings (list filings with pagination)
     - GET /api/w1-filings/{id} (get filing details)
     - POST /api/w1-filings/{id}/amend (file amended W-1)
     - GET /api/w1-filings/{id}/penalties (calculate penalties)
     - GET /api/cumulative-totals (query YTD totals)
   - **api-reconciliation.yaml** (OpenAPI 3.0): 5 endpoints
     - POST /api/reconciliations (initiate reconciliation with W-2 upload)
     - GET /api/reconciliations (list reconciliations)
     - GET /api/reconciliations/{id} (get reconciliation report)
     - PATCH /api/reconciliations/{id}/resolve (resolve discrepancy)
     - GET /api/reconciliations/{id}/ignored-w2s (get ignored W-2s report)
     - POST /api/w2-upload (upload additional W-2s)
   - **event-w1-filed.yaml** (AsyncAPI 2.6): 6 event channels
     - withholding/w1-filed (triggers cumulative update)
     - withholding/w1-amended (triggers cascade update)
     - withholding/reconciliation-completed (notification)
     - withholding/discrepancy-detected (alert)
     - withholding/payment-received (update filing status)
     - withholding/audit-log (immutable audit trail)

3. ✅ **quickstart.md** (COMPLETE):
   - Environment setup (Docker, PostgreSQL, Redis, JWT auth)
   - 8 API examples with curl commands
   - Database queries (SQL examples)
   - Test execution (unit, integration, E2E)
   - Flyway migration guide
   - Event-driven architecture (Redis Pub/Sub, Kafka)
   - Performance testing (Apache Bench load tests)
   - Troubleshooting guide (8 common issues)

4. ⏳ **Update agent context**: TODO - Run update-agent-context script

### Phase 1 Acceptance Criteria ✅

- ✅ data-model.md includes 6 entities (not 4 - added IgnoredW2 and WithholdingAuditLog from research)
- ✅ contracts/ includes 3 files: api-w1-filing.yaml (6 endpoints), api-reconciliation.yaml (6 endpoints), event-w1-filed.yaml (6 channels)
- ✅ quickstart.md includes 8 curl examples (exceeded 5+ requirement)
- ✅ Constitution Check re-evaluated (see below - 0 violations)

---

## Phase 2: Task Breakdown (NOT part of /speckit.plan)

**Status**: NOT STARTED  
**Output File**: `tasks.md` (generated by separate `/speckit.tasks` command)

Phase 2 is NOT included in this plan. After Phase 0 and Phase 1 complete, run `/speckit.tasks` to generate implementation tasks.

---

## Next Steps

1. **Immediate**: Execute Phase 0 research tasks (R1-R5)
2. **Generate research.md**: Document all findings, decisions, rationale
3. **Checkpoint**: Review research.md with team, validate technology choices
4. **Proceed to Phase 1**: Design data model, API contracts, quickstart guide
5. **Final checkpoint**: Constitution Check post-design
6. **Output**: Deliver plan.md, research.md, data-model.md, contracts/, quickstart.md to implementation team

**Estimated Timeline**:
- Phase 0 (Research): 2-3 days
- Phase 1 (Design): 2-3 days
- **Total Planning**: 4-6 days before implementation begins

---

**Plan Status**: ✅ PHASE 0 READY - Proceed with research tasks (R1-R5)
