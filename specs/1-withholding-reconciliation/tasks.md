# Tasks: Complete Withholding Reconciliation System

**Input**: Design documents from `/specs/1-withholding-reconciliation/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Tests**: Comprehensive test coverage included as requested in context (unit, integration, E2E)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

This is a web application with:
- **Backend**: `backend/tax-engine-service/` (Java 21 Spring Boot)
- **Frontend**: `src/` (React TypeScript)
- **Database**: PostgreSQL migrations in `backend/tax-engine-service/src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Review project structure in plan.md and verify existing tax-engine-service setup
- [ ] T002 Verify Spring Boot dependencies in backend/tax-engine-service/pom.xml (Spring Data JPA, PostgreSQL driver, Lombok, Jackson)
- [ ] T003 [P] Verify React dependencies in package.json (React Router, Axios, Tailwind CSS, date-fns)
- [ ] T004 [P] Verify test frameworks in pom.xml (JUnit 5, Mockito, TestContainers) and package.json (Vitest, Playwright)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core database schema and infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Migrations

- [ ] T005 Create Flyway migration V1.20__create_w1_filings_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T006 Create Flyway migration V1.21__create_cumulative_withholding_totals_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T007 Create Flyway migration V1.22__create_withholding_reconciliations_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T008 Create Flyway migration V1.23__create_ignored_w2s_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T009 Create Flyway migration V1.24__create_withholding_payments_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T010 Create Flyway migration V1.25__create_withholding_audit_log_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T011 Create Flyway migration V1.26__add_withholding_indexes.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T012 Create Flyway migration V1.27__add_withholding_constraints.sql in backend/tax-engine-service/src/main/resources/db/migration/

### Domain Models

- [ ] T013 [P] Create FilingFrequency enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/withholding/FilingFrequency.java
- [ ] T014 [P] Create W1FilingStatus enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/withholding/W1FilingStatus.java
- [ ] T015 [P] Create ReconciliationStatus enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/withholding/ReconciliationStatus.java
- [ ] T016 [P] Create IgnoredW2Reason enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/withholding/IgnoredW2Reason.java
- [ ] T017 [P] Create W1Filing entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/withholding/W1Filing.java
- [ ] T018 [P] Create CumulativeWithholdingTotals entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/withholding/CumulativeWithholdingTotals.java
- [ ] T019 [P] Create WithholdingReconciliation entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/withholding/WithholdingReconciliation.java
- [ ] T020 [P] Create IgnoredW2 entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/withholding/IgnoredW2.java
- [ ] T021 [P] Create WithholdingPayment entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/withholding/WithholdingPayment.java
- [ ] T022 [P] Create WithholdingAuditLog entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/withholding/WithholdingAuditLog.java

### Repositories

- [ ] T023 [P] Create W1FilingRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/W1FilingRepository.java
- [ ] T024 [P] Create CumulativeWithholdingTotalsRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/CumulativeWithholdingTotalsRepository.java
- [ ] T025 [P] Create WithholdingReconciliationRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/WithholdingReconciliationRepository.java
- [ ] T026 [P] Create IgnoredW2Repository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/IgnoredW2Repository.java
- [ ] T027 [P] Create WithholdingPaymentRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/WithholdingPaymentRepository.java
- [ ] T028 [P] Create WithholdingAuditLogRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/WithholdingAuditLogRepository.java

### DTOs

- [ ] T029 [P] Create W1FilingRequest DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/W1FilingRequest.java
- [ ] T030 [P] Create W1FilingResponse DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/W1FilingResponse.java
- [ ] T031 [P] Create CumulativeTotalsResponse DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/CumulativeTotalsResponse.java
- [ ] T032 [P] Create ReconciliationReportDto in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/ReconciliationReportDto.java
- [ ] T033 [P] Create AmendW1Request DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/AmendW1Request.java
- [ ] T034 [P] Create PenaltyCalculationResponse DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/PenaltyCalculationResponse.java

### Core Services

- [ ] T035 Create DueDateCalculationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/DueDateCalculationService.java (research R5: TemporalAdjusters + federal holiday calendar)
- [ ] T036 Create PenaltyCalculationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/PenaltyCalculationService.java (research R4: 5% per month, round up, safe harbor)
- [ ] T037 Create W2ExtractionIntegrationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/W2ExtractionIntegrationService.java (call extraction-service for W-2 Box 18/19)

### Frontend Types

- [ ] T038 [P] Create withholding types in src/types/withholding.ts (W1Filing, CumulativeTotals, FilingFrequency interfaces)
- [ ] T039 [P] Create reconciliationStatus types in src/types/reconciliationStatus.ts (ReconciliationStatus enum)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Quarterly Filer Submits W-1 with Cumulative Validation (Priority: P1) 🎯 MVP

**Goal**: Business can file quarterly W-1 return and immediately see cumulative YTD totals with run-rate projection

**Independent Test**: Submit Q2 W-1 with $50,000 wages when Q1 showed $45,000. System displays cumulative $95,000 wages YTD and projects $190,000 annual wages.

### Backend Unit Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T040 [P] [US1] Create W1FilingServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/W1FilingServiceTest.java (test filing Q1, tax calculation, due date)
- [ ] T041 [P] [US1] Create CumulativeCalculationServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/CumulativeCalculationServiceTest.java (test cumulative update after Q1, Q2)
- [ ] T042 [P] [US1] Create DueDateCalculationServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/DueDateCalculationServiceTest.java (test quarterly due date = 30 days after quarter end)

### Backend Implementation for User Story 1

- [ ] T043 [US1] Implement CumulativeCalculationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/CumulativeCalculationService.java (event-driven update on W1FiledEvent)
- [ ] T044 [US1] Implement W1FilingService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/W1FilingService.java (file W-1, calculate tax, trigger cumulative update)
- [ ] T045 [US1] Create W1FilingController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/W1FilingController.java (POST /api/v1/w1-filings)
- [ ] T046 [US1] Add GET /api/v1/w1-filings endpoint in W1FilingController (list filings with pagination)
- [ ] T047 [US1] Add GET /api/v1/w1-filings/{id} endpoint in W1FilingController (get filing details)
- [ ] T048 [US1] Add GET /api/v1/cumulative-totals endpoint in W1FilingController (query YTD totals)
- [ ] T049 [US1] Implement validation in W1FilingService (check wages >= 0, employee count >= 0, period matches frequency)
- [ ] T050 [US1] Add audit logging in W1FilingService (create WithholdingAuditLog entry on every filing)

### Backend Integration Tests for User Story 1

- [ ] T051 [US1] Create WithholdingReconciliationIntegrationTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/integration/WithholdingReconciliationIntegrationTest.java (file Q1 W-1 → verify cumulative totals via GET /cumulative-totals)

### Frontend Implementation for User Story 1

- [ ] T052 [P] [US1] Create w1FilingService API client in src/services/w1FilingService.ts (POST /w1-filings, GET /w1-filings, GET /cumulative-totals)
- [ ] T053 [P] [US1] Create useW1Filing hook in src/hooks/useW1Filing.ts (React Query hook for W-1 filing mutations)
- [ ] T054 [P] [US1] Create useCumulativeTotals hook in src/hooks/useCumulativeTotals.ts (React Query hook for YTD totals)
- [ ] T055 [US1] Create W1FilingWizard component in src/components/withholding/W1FilingWizard.tsx (multi-step form: Enter wages → Review → File)
- [ ] T056 [US1] Create CumulativeTotalsCard component in src/components/withholding/CumulativeTotalsCard.tsx (display YTD wages, tax, projected annual)
- [ ] T057 [US1] Create W1FilingHistory component in src/components/withholding/W1FilingHistory.tsx (table showing all W-1 filings)
- [ ] T058 [US1] Add filing frequency dropdown in src/components/shared/FilingFrequencySelector.tsx (DAILY, SEMI_MONTHLY, MONTHLY, QUARTERLY)

### Frontend Tests for User Story 1

- [ ] T059 [P] [US1] Create W1FilingWizard.test.tsx in src/__tests__/components/W1FilingWizard.test.tsx (Vitest component test: render form, fill wages, submit)
- [ ] T060 [P] [US1] Create CumulativeTotalsCard.test.tsx in src/__tests__/components/CumulativeTotalsCard.test.tsx (Vitest test: display YTD totals, on-track indicator)

### E2E Tests for User Story 1

- [ ] T061 [US1] Create withholding-filing-e2e.spec.ts in src/__tests__/e2e/withholding-filing-e2e.spec.ts (Playwright: login → file Q1 W-1 → verify cumulative totals displayed)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently (file W-1, see cumulative totals)

---

## Phase 4: User Story 2 - Year-End W-2/W-3 Reconciliation with Discrepancy Detection (Priority: P1)

**Goal**: Business uploads W-2s at year-end, system reconciles against cumulative W-1 filings and flags discrepancies for resolution

**Independent Test**: Upload W-3 showing $200,000 total wages when W-1 filings showed $198,500 cumulative. System flags $1,500 discrepancy and requires explanation or amended W-1 filing.

### Backend Unit Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T062 [P] [US2] Create ReconciliationServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/ReconciliationServiceTest.java (test reconciliation calculation: W1 vs W2 variance)
- [ ] T063 [P] [US2] Create W2ExtractionIntegrationServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/W2ExtractionIntegrationServiceTest.java (mock extraction-service response)

### Backend Implementation for User Story 2

- [ ] T064 [US2] Implement ReconciliationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/ReconciliationService.java (calculate W1 vs W2 variance, detect discrepancies)
- [ ] T065 [US2] Create ReconciliationController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/ReconciliationController.java (POST /api/v1/reconciliations with multipart W-2 upload)
- [ ] T066 [US2] Add GET /api/v1/reconciliations endpoint in ReconciliationController (list reconciliations)
- [ ] T067 [US2] Add GET /api/v1/reconciliations/{id} endpoint in ReconciliationController (get reconciliation report)
- [ ] T068 [US2] Add PATCH /api/v1/reconciliations/{id}/resolve endpoint in ReconciliationController (resolve discrepancy with explanation)
- [ ] T069 [US2] Implement discrepancy threshold logic in ReconciliationService (>$100 OR >1% variance triggers DISCREPANCY status)
- [ ] T070 [US2] Add validation in ReconciliationService (prevent next year W-1 filing if prior year reconciliation incomplete per FR-010)

### Backend Integration Tests for User Story 2

- [ ] T071 [US2] Add reconciliation workflow test in WithholdingReconciliationIntegrationTest (file 4 W-1s → upload W-2s → verify reconciliation status)

### Frontend Implementation for User Story 2

- [ ] T072 [P] [US2] Create reconciliationService API client in src/services/reconciliationService.ts (POST /reconciliations, GET /reconciliations, PATCH /resolve)
- [ ] T073 [P] [US2] Create useReconciliation hook in src/hooks/useReconciliation.ts (React Query hook for reconciliation mutations)
- [ ] T074 [US2] Create ReconciliationDashboard component in src/components/withholding/ReconciliationDashboard.tsx (show reconciliation status, variance, action items)
- [ ] T075 [US2] Create ReconciliationReportModal component in src/components/withholding/ReconciliationReportModal.tsx (detailed W-1 vs W-2 comparison)
- [ ] T076 [US2] Create W2UploadForm component in src/components/withholding/W2UploadForm.tsx (multipart file upload, progress indicator)
- [ ] T077 [US2] Add discrepancy resolution UI in ReconciliationReportModal (Accept/Amend/Explain options)

### Frontend Tests for User Story 2

- [ ] T078 [P] [US2] Create ReconciliationDashboard.test.tsx in src/__tests__/components/ReconciliationDashboard.test.tsx (Vitest test: display status, variance)
- [ ] T079 [P] [US2] Create W2UploadForm.test.tsx in src/__tests__/components/W2UploadForm.test.tsx (Vitest test: file selection, upload progress)

### E2E Tests for User Story 2

- [ ] T080 [US2] Add reconciliation workflow to withholding-filing-e2e.spec.ts (file 4 W-1s → upload W-2s → verify reconciliation report → resolve discrepancy)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently (file W-1s, upload W-2s, reconcile)

---

## Phase 5: User Story 3 - Monthly Filer with Mid-Year Correction (Priority: P2)

**Goal**: Business discovers error in Month 3, files amended W-1, system automatically recalculates all subsequent cumulative totals

**Independent Test**: File amended March W-1 with $10,000 additional wages. System recalculates cumulative totals for April, May, and all subsequent months automatically.

### Backend Unit Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T081 [P] [US3] Add amended W-1 test in W1FilingServiceTest (file Q1 → amend Q1 → verify original status = AMENDED, new filing created)
- [ ] T082 [P] [US3] Add cascade update test in CumulativeCalculationServiceTest (amend March → verify April, May cumulative totals recalculated)

### Backend Implementation for User Story 3

- [ ] T083 [US3] Add POST /api/v1/w1-filings/{id}/amend endpoint in W1FilingController (file amended W-1)
- [ ] T084 [US3] Implement amend logic in W1FilingService (create new W1Filing with is_amended=true, amends_filing_id set, mark original status=AMENDED)
- [ ] T085 [US3] Implement cascade update in CumulativeCalculationService (batch SQL UPDATE for all periods after amended period, research R3 batch approach)
- [ ] T086 [US3] Add audit logging for amended filings in W1FilingService (log "Amended [period] - Reason: [reason]")

### Backend Integration Tests for User Story 3

- [ ] T087 [US3] Add amended W-1 workflow test in WithholdingReconciliationIntegrationTest (file Jan-May → amend March → verify cumulative totals for Apr-May updated)

### Frontend Implementation for User Story 3

- [ ] T088 [US3] Create AmendW1Form component in src/components/withholding/AmendW1Form.tsx (form with amendment reason, new wages)
- [ ] T089 [US3] Add "Amend" button to W1FilingHistory component (opens AmendW1Form modal)
- [ ] T090 [US3] Add cascade update notification in CumulativeTotalsCard (show "Recalculated due to amended [period]" alert)

### Frontend Tests for User Story 3

- [ ] T091 [P] [US3] Create AmendW1Form.test.tsx in src/__tests__/components/AmendW1Form.test.tsx (Vitest test: fill amendment form, submit)

### E2E Tests for User Story 3

- [ ] T092 [US3] Add amended W-1 workflow to withholding-filing-e2e.spec.ts (file Jan-May → amend March → verify cumulative totals updated)

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work independently (file W-1s, amend W-1s, reconcile)

---

## Phase 6: User Story 4 - Daily Filer with High-Volume Reconciliation (Priority: P2)

**Goal**: Construction company with daily filing frequency (100+ W-1 filings per year) reconciles efficiently at year-end without manual review

**Independent Test**: Simulate 252 daily W-1 filings totaling $5,000,000 wages, upload W-3 with matching total. System reconciles automatically in under 5 seconds.

### Backend Unit Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T093 [P] [US4] Add high-volume test in ReconciliationServiceTest (252 W-1 filings → verify reconciliation completes <10 seconds per success criteria)
- [ ] T094 [P] [US4] Add safe harbor test in ReconciliationServiceTest (variance <0.1% → verify "Accept Minor Variance" option available)

### Backend Implementation for User Story 4

- [ ] T095 [US4] Optimize reconciliation query in ReconciliationService (single aggregate query for W-1 totals instead of N+1)
- [ ] T096 [US4] Implement safe harbor logic in ReconciliationService (variance <0.1% AND <$1000 → offer "Accept Minor Variance" without explanation)
- [ ] T097 [US4] Add pagination support to GET /api/v1/w1-filings endpoint (page, size, sort parameters)
- [ ] T098 [US4] Add caching to CumulativeCalculationService (Redis cache with 5-minute TTL per research R2)

### Backend Integration Tests for User Story 4

- [ ] T099 [US4] Add high-volume reconciliation test in WithholdingReconciliationIntegrationTest (insert 252 daily filings via SQL → verify reconciliation performance)

### Frontend Implementation for User Story 4

- [ ] T100 [US4] Add pagination to W1FilingHistory component (page controls, 20 filings per page)
- [ ] T101 [US4] Add "Accept Minor Variance" button in ReconciliationReportModal (only show if variance <0.1%)
- [ ] T102 [US4] Add loading indicator to ReconciliationDashboard (show "Processing 252 filings..." during reconciliation)

### Frontend Tests for User Story 4

- [ ] T103 [US4] Add pagination test to W1FilingHistory.test.tsx (verify page navigation works)

**Checkpoint**: At this point, all P2 user stories (US3, US4) should work independently and support high-volume scenarios

---

## Phase 7: User Story 5 - Semi-Monthly Filer with Employee Count Validation (Priority: P3)

**Goal**: Business files W-1 semi-monthly (24 times/year) with employee count, system validates employee count aligns with W-2s at year-end

**Independent Test**: File W-1 returns reporting average of 15 employees, upload only 12 W-2s. System flags "3 fewer W-2s than reported employee count - verify terminations."

### Backend Unit Tests for User Story 5

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T104 [P] [US5] Add employee count validation test in ReconciliationServiceTest (15 avg employees reported → 12 W-2s uploaded → verify WARNING status)

### Backend Implementation for User Story 5

- [ ] T105 [US5] Add employee count validation in ReconciliationService (calculate average employee count from W-1s, compare to W-2 count, ±20% tolerance per FR-018)
- [ ] T106 [US5] Add employee count mismatch warning to reconciliation report DTO

### Frontend Implementation for User Story 5

- [ ] T107 [US5] Add employee count section to ReconciliationReportModal (show "Employee count: 15 avg reported vs 12 W-2s issued")
- [ ] T108 [US5] Add employee count explanation field in discrepancy resolution form

### Frontend Tests for User Story 5

- [ ] T109 [US5] Add employee count validation test to ReconciliationDashboard.test.tsx (verify warning displayed)

**Checkpoint**: All user stories (US1-US5) should now be independently functional with comprehensive validation

---

## Phase 8: Ignored W-2 Report (Constitution IV - AI Transparency)

**Goal**: Provide transparency for W-2s not included in reconciliation (wrong EIN, duplicates, extraction errors)

**Independent Test**: Upload 15 W-2 PDFs where 2 have different employer EIN. System shows "2 W-2s ignored" with reasons and re-upload option.

### Backend Unit Tests for Ignored W-2 Report

- [ ] T110 [P] Create IgnoredW2ServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/IgnoredW2ServiceTest.java (test EIN mismatch detection, duplicate detection)

### Backend Implementation for Ignored W-2 Report

- [ ] T111 Implement IgnoredW2Service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/IgnoredW2Service.java (track W-2s not matched to business EIN)
- [ ] T112 Add ignored W-2 detection in ReconciliationService (call IgnoredW2Service for each W-2 extraction)
- [ ] T113 Add GET /api/v1/reconciliations/{id}/ignored-w2s endpoint in ReconciliationController (return list of ignored W-2s with reasons)
- [ ] T114 Add POST /api/v1/reconciliations/{id}/ignored-w2s/{w2Id}/reupload endpoint in ReconciliationController (re-upload corrected W-2)

### Frontend Implementation for Ignored W-2 Report

- [ ] T115 [P] Create IgnoredW2ReportModal component in src/components/withholding/IgnoredW2ReportModal.tsx (show ignored W-2s with reasons, re-upload button)
- [ ] T116 Add "View Ignored W-2s" link to ReconciliationDashboard (opens IgnoredW2ReportModal)

### Frontend Tests for Ignored W-2 Report

- [ ] T117 Create IgnoredW2ReportModal.test.tsx in src/__tests__/components/IgnoredW2ReportModal.test.tsx (Vitest test: display ignored W-2s, click re-upload)

---

## Phase 9: Penalty Calculation

**Goal**: Calculate late filing and underpayment penalties per FR-011, FR-012

**Independent Test**: File Q1 W-1 on May 15 (due date April 30, 15 days late). System calculates 5% penalty ($140.63 on $2,812.50 tax due).

### Backend Unit Tests for Penalty Calculation

- [ ] T118 [P] Add late filing penalty tests in PenaltyCalculationServiceTest (1 month late = 5%, 6 months late = 25% max)
- [ ] T119 [P] Add safe harbor tests in PenaltyCalculationServiceTest (90% of current year OR 100% of prior year)

### Backend Implementation for Penalty Calculation

- [ ] T120 Add GET /api/v1/w1-filings/{id}/penalties endpoint in W1FilingController (calculate penalties as of given date)
- [ ] T121 Add penalty calculation to W1FilingService (call PenaltyCalculationService on filing if past due date)
- [ ] T122 Add penalty display to W1FilingResponse DTO (lateFilingPenalty, underpaymentPenalty fields)

### Frontend Implementation for Penalty Calculation

- [ ] T123 [P] Create PenaltyCalculationTooltip component in src/components/shared/PenaltyCalculationTooltip.tsx (explain penalty calculation)
- [ ] T124 Add penalty display to W1FilingHistory component (show penalties in red if > 0)

### Frontend Tests for Penalty Calculation

- [ ] T125 Create PenaltyCalculationTooltip.test.tsx in src/__tests__/components/PenaltyCalculationTooltip.test.tsx (Vitest test: hover tooltip, display calculation)

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T126 [P] Add error handling to all API endpoints in controllers (ValidationException, BusinessException with proper HTTP status codes)
- [ ] T127 [P] Add logging to all services (MDC context with tenant_id, business_id, user_id)
- [ ] T128 [P] Add API documentation comments to all DTOs (JavaDoc for fields)
- [ ] T129 [P] Add accessibility attributes to all form components (aria-label, aria-describedby)
- [ ] T130 [P] Add loading states to all frontend components (skeleton loaders)
- [ ] T131 [P] Add error boundaries to frontend pages (ErrorBoundary component)
- [ ] T132 Performance optimization: Add database query indexes per data-model.md V1.26 migration
- [ ] T133 Security hardening: Add rate limiting to W-1 filing endpoint (max 10 filings per minute per business)
- [ ] T134 Security hardening: Add file size validation to W-2 upload (max 10MB per PDF)
- [ ] T135 Security hardening: Add virus scanning to W-2 uploads (ClamAV integration)
- [ ] T136 Run quickstart.md validation: Execute all 8 curl examples from quickstart.md Section 2
- [ ] T137 Run quickstart.md validation: Execute all SQL queries from quickstart.md Section 3
- [ ] T138 Run quickstart.md validation: Execute all test commands from quickstart.md Section 4
- [ ] T139 Generate OpenAPI documentation: Run springdoc-openapi-maven-plugin to generate api-docs.yaml
- [ ] T140 Code cleanup: Run Checkstyle and fix violations
- [ ] T141 Code cleanup: Run ESLint and fix violations
- [ ] T142 [P] Update README.md with withholding reconciliation feature overview
- [ ] T143 [P] Create API_DOCUMENTATION.md in docs/ folder (link to OpenAPI specs)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Ignored W-2 Report (Phase 8)**: Depends on User Story 2 completion (reconciliation)
- **Penalty Calculation (Phase 9)**: Depends on User Story 1 completion (W-1 filing)
- **Polish (Phase 10)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Integrates with US1 but independently testable
- **User Story 3 (P2)**: Depends on User Story 1 completion (amends existing W-1 filings)
- **User Story 4 (P2)**: Depends on User Stories 1 and 2 completion (high-volume reconciliation)
- **User Story 5 (P3)**: Depends on User Story 2 completion (employee count validation in reconciliation)

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Enums before entities
- Entities before repositories
- Repositories before services
- Services before controllers
- DTOs before controllers
- Backend API before frontend components
- Frontend hooks before frontend components
- Component tests before E2E tests
- Story complete before moving to next priority

### Parallel Opportunities

- **Setup**: All 4 setup tasks can run in parallel (different verification tasks)
- **Foundational - Migrations**: T005-T012 can run in parallel (8 migration files)
- **Foundational - Enums**: T013-T016 can run in parallel (4 enum files)
- **Foundational - Entities**: T017-T022 can run in parallel (6 entity files)
- **Foundational - Repositories**: T023-T028 can run in parallel (6 repository files)
- **Foundational - DTOs**: T029-T034 can run in parallel (6 DTO files)
- **Foundational - Frontend Types**: T038-T039 can run in parallel (2 type files)
- **User Story 1 - Unit Tests**: T040-T042 can run in parallel (3 test files)
- **User Story 1 - Frontend Services**: T052-T054 can run in parallel (3 files: API client + 2 hooks)
- **User Story 1 - Frontend Tests**: T059-T060 can run in parallel (2 component tests)
- **User Story 2 - Unit Tests**: T062-T063 can run in parallel (2 test files)
- **User Story 2 - Frontend Services**: T072-T073 can run in parallel (API client + hook)
- **User Story 2 - Frontend Tests**: T078-T079 can run in parallel (2 component tests)
- **User Story 3 - Unit Tests**: T081-T082 can run in parallel (add tests to existing test files)
- **Polish - Documentation**: T126-T131 can run in parallel (different areas)
- **Polish - Final Tasks**: T142-T143 can run in parallel (documentation updates)
- **Once Foundational phase completes**: User Stories 1 and 2 can start in parallel (both P1, independent)

---

## Parallel Example: User Story 1

```bash
# Launch all unit tests for User Story 1 together:
Task T040: "Create W1FilingServiceTest"
Task T041: "Create CumulativeCalculationServiceTest"
Task T042: "Create DueDateCalculationServiceTest"

# Launch all frontend services for User Story 1 together:
Task T052: "Create w1FilingService API client"
Task T053: "Create useW1Filing hook"
Task T054: "Create useCumulativeTotals hook"

# Launch all frontend component tests for User Story 1 together:
Task T059: "Create W1FilingWizard.test.tsx"
Task T060: "Create CumulativeTotalsCard.test.tsx"
```

---

## Parallel Example: User Story 2

```bash
# Launch all unit tests for User Story 2 together:
Task T062: "Create ReconciliationServiceTest"
Task T063: "Create W2ExtractionIntegrationServiceTest"

# Launch all frontend services for User Story 2 together:
Task T072: "Create reconciliationService API client"
Task T073: "Create useReconciliation hook"
```

---

## Implementation Strategy

### MVP First (User Story 1 + User Story 2 Only)

Since both US1 and US2 are Priority P1 and represent the core withholding reconciliation workflow:

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (file W-1, see cumulative totals)
4. Complete Phase 4: User Story 2 (upload W-2s, reconcile)
5. **STOP and VALIDATE**: Test US1 + US2 together (file W-1s all year → upload W-2s → reconcile)
6. Deploy/demo if ready

**MVP Value**: Business can file quarterly W-1 returns all year, then reconcile at year-end with W-2 uploads. This satisfies the core IRS compliance requirement.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (filing only, no reconciliation yet)
3. Add User Story 2 → Test with US1 → Deploy/Demo (MVP! Full filing + reconciliation)
4. Add User Story 3 → Test independently → Deploy/Demo (amendments)
5. Add User Story 4 → Test with US1+US2 → Deploy/Demo (high-volume support)
6. Add User Story 5 → Test with US2 → Deploy/Demo (employee count validation)
7. Add Ignored W-2 Report → Test with US2 → Deploy/Demo (AI transparency)
8. Add Penalty Calculation → Test with US1 → Deploy/Demo (late filing penalties)
9. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers after Foundational phase completes:

**Scenario 1: 2 Developers**
- Developer A: User Story 1 (P1) - W-1 filing
- Developer B: User Story 2 (P1) - Reconciliation
- Once both complete: Integrate and test together (MVP!)

**Scenario 2: 3 Developers**
- Developer A: User Story 1 (P1)
- Developer B: User Story 2 (P1)
- Developer C: User Story 3 (P2) - Amendments (starts after US1 complete)

**Scenario 3: Full Team (5 developers)**
- Dev A: US1 (P1)
- Dev B: US2 (P1)
- Dev C: US3 (P2) - waits for US1
- Dev D: US4 (P2) - waits for US1+US2
- Dev E: US5 (P3) - waits for US2

---

## Summary

**Total Tasks**: 143 tasks
- **Phase 1 (Setup)**: 4 tasks
- **Phase 2 (Foundational)**: 34 tasks (8 migrations + 6 enums + 6 entities + 6 repositories + 6 DTOs + 3 services + 2 types)
- **Phase 3 (User Story 1)**: 22 tasks (3 unit tests + 8 backend + 1 integration test + 6 frontend + 2 frontend tests + 1 E2E test + 1 checkpoint)
- **Phase 4 (User Story 2)**: 19 tasks (2 unit tests + 6 backend + 1 integration test + 6 frontend + 2 frontend tests + 1 E2E test + 1 checkpoint)
- **Phase 5 (User Story 3)**: 12 tasks (2 unit tests + 4 backend + 1 integration test + 3 frontend + 1 frontend test + 1 E2E test)
- **Phase 6 (User Story 4)**: 9 tasks (2 unit tests + 4 backend + 1 integration test + 3 frontend + 1 frontend test)
- **Phase 7 (User Story 5)**: 6 tasks (1 unit test + 2 backend + 2 frontend + 1 frontend test)
- **Phase 8 (Ignored W-2 Report)**: 7 tasks (1 unit test + 4 backend + 2 frontend + 1 frontend test)
- **Phase 9 (Penalty Calculation)**: 8 tasks (2 unit tests + 3 backend + 2 frontend + 1 frontend test)
- **Phase 10 (Polish)**: 18 tasks

**Task Breakdown by User Story**:
- **US1**: 22 tasks (P1 - MVP core)
- **US2**: 19 tasks (P1 - MVP core)
- **US3**: 12 tasks (P2 - Amendments)
- **US4**: 9 tasks (P2 - High-volume)
- **US5**: 6 tasks (P3 - Employee count)

**Parallel Opportunities Identified**: 15+ groups of parallelizable tasks across all phases

**Independent Test Criteria**:
- **US1**: File Q2 W-1 → verify cumulative totals displayed
- **US2**: Upload W-3 with variance → verify discrepancy flagged
- **US3**: Amend March W-1 → verify April+ totals recalculated
- **US4**: 252 daily filings → verify reconciliation <10 seconds
- **US5**: 15 avg employees, 12 W-2s → verify warning displayed

**Suggested MVP Scope**: 
- Phase 1 (Setup): 4 tasks
- Phase 2 (Foundational): 34 tasks
- Phase 3 (User Story 1): 22 tasks
- Phase 4 (User Story 2): 19 tasks
- **Total MVP**: 79 tasks (55% of total)

This MVP delivers the complete P1 requirement: businesses can file W-1 returns all year and reconcile with W-2s at year-end, meeting IRS compliance requirements.

---

## Notes

- [P] tasks = different files, no dependencies (can run in parallel)
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD approach)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All file paths are absolute from repository root
- All database migrations follow sequential versioning (V1.20 through V1.27)
- All entities include tenant_id for multi-tenant isolation per Constitution II
- All audit-sensitive actions logged to WithholdingAuditLog per Constitution III
- Ignored W-2 report provides AI transparency per Constitution IV
