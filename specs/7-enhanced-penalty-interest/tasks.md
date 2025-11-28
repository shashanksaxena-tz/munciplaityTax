# Tasks: Enhanced Penalty & Interest Calculation

**Input**: Design documents from `/specs/7-enhanced-penalty-interest/`
**Prerequisites**: plan.md ✅, spec.md ✅, data-model.md ✅

**Tests**: Comprehensive test coverage included per requirements (unit, integration, E2E)

**Organization**: Tasks are grouped by user story (US-1 through US-7) to enable independent implementation and testing of each story.

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
- [ ] T002 Verify Spring Boot dependencies in backend/tax-engine-service/pom.xml (Spring Data JPA, PostgreSQL driver, Lombok, Jackson, java.time API)
- [ ] T003 [P] Verify React dependencies in package.json (React Router, Axios, Tailwind CSS, date-fns, recharts)
- [ ] T004 [P] Verify test frameworks in pom.xml (JUnit 5, Mockito, TestContainers) and package.json (Vitest, React Testing Library, Playwright)
- [ ] T005 Verify Redis configuration for caching penalty/interest rates (updated quarterly)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core database schema and infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Migrations

- [ ] T006 Create Flyway migration V1.30__create_penalties_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T007 Create Flyway migration V1.31__create_estimated_tax_penalties_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T008 Create Flyway migration V1.32__create_quarterly_underpayments_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T009 Create Flyway migration V1.33__create_interests_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T010 Create Flyway migration V1.34__create_quarterly_interests_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T011 Create Flyway migration V1.35__create_penalty_abatements_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T012 Create Flyway migration V1.36__create_payment_allocations_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T013 Create Flyway migration V1.37__create_penalty_audit_log_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T014 Create Flyway migration V1.38__add_penalty_indexes.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T015 Create Flyway migration V1.39__add_penalty_constraints.sql in backend/tax-engine-service/src/main/resources/db/migration/

### Domain Models - Enums

- [ ] T016 [P] Create PenaltyType enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/PenaltyType.java
- [ ] T017 [P] Create CalculationMethod enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/CalculationMethod.java
- [ ] T018 [P] Create Quarter enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/Quarter.java
- [ ] T019 [P] Create CompoundingFrequency enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/CompoundingFrequency.java
- [ ] T020 [P] Create AbatementType enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/AbatementType.java
- [ ] T021 [P] Create AbatementReason enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/AbatementReason.java
- [ ] T022 [P] Create AbatementStatus enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/AbatementStatus.java
- [ ] T023 [P] Create AllocationOrder enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/AllocationOrder.java
- [ ] T024 [P] Create PenaltyAuditEntityType enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/PenaltyAuditEntityType.java
- [ ] T025 [P] Create PenaltyAuditAction enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/PenaltyAuditAction.java
- [ ] T026 [P] Create ActorRole enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/ActorRole.java

### Domain Models - Entities

- [ ] T027 [P] Create Penalty entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/Penalty.java
- [ ] T028 [P] Create EstimatedTaxPenalty entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/EstimatedTaxPenalty.java
- [ ] T029 [P] Create QuarterlyUnderpayment entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/QuarterlyUnderpayment.java
- [ ] T030 [P] Create Interest entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/Interest.java
- [ ] T031 [P] Create QuarterlyInterest entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/QuarterlyInterest.java
- [ ] T032 [P] Create PenaltyAbatement entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/PenaltyAbatement.java
- [ ] T033 [P] Create PaymentAllocation entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/PaymentAllocation.java
- [ ] T034 [P] Create PenaltyAuditLog entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/penalty/PenaltyAuditLog.java

### Repositories

- [ ] T035 [P] Create PenaltyRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/PenaltyRepository.java
- [ ] T036 [P] Create EstimatedTaxPenaltyRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/EstimatedTaxPenaltyRepository.java
- [ ] T037 [P] Create QuarterlyUnderpaymentRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/QuarterlyUnderpaymentRepository.java
- [ ] T038 [P] Create InterestRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/InterestRepository.java
- [ ] T039 [P] Create QuarterlyInterestRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/QuarterlyInterestRepository.java
- [ ] T040 [P] Create PenaltyAbatementRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/PenaltyAbatementRepository.java
- [ ] T041 [P] Create PaymentAllocationRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/PaymentAllocationRepository.java
- [ ] T042 [P] Create PenaltyAuditLogRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/PenaltyAuditLogRepository.java

### DTOs

- [ ] T043 [P] Create PenaltyCalculationRequest DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/PenaltyCalculationRequest.java
- [ ] T044 [P] Create PenaltyCalculationResponse DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/PenaltyCalculationResponse.java
- [ ] T045 [P] Create SafeHarborEvaluationDto in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/SafeHarborEvaluationDto.java
- [ ] T046 [P] Create InterestCalculationRequest DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/InterestCalculationRequest.java
- [ ] T047 [P] Create InterestCalculationResponse DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/InterestCalculationResponse.java
- [ ] T048 [P] Create PenaltyAbatementRequest DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/PenaltyAbatementRequest.java
- [ ] T049 [P] Create PenaltyAbatementResponse DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/PenaltyAbatementResponse.java
- [ ] T050 [P] Create PaymentAllocationRequest DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/PaymentAllocationRequest.java
- [ ] T051 [P] Create PaymentAllocationResponse DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/PaymentAllocationResponse.java

### Core Services (Shared)

- [ ] T052 Create RuleEngineIntegrationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/RuleEngineIntegrationService.java (retrieve penalty rates, interest rates from rule-engine-service)
- [ ] T053 Create PdfServiceIntegrationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/PdfServiceIntegrationService.java (generate Form 27-PA via pdf-service)

### Frontend Types

- [ ] T054 [P] Create penalty types in src/types/penalty.ts (Penalty, EstimatedTaxPenalty, QuarterlyUnderpayment interfaces)
- [ ] T055 [P] Create interest types in src/types/interest.ts (Interest, QuarterlyInterest interfaces)
- [ ] T056 [P] Create abatement types in src/types/abatement.ts (PenaltyAbatement, AbatementStatus, AbatementReason enums)
- [ ] T057 [P] Create payment allocation types in src/types/paymentAllocation.ts (PaymentAllocation interface)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Late Filing Penalty (5% Per Month, Max 25%) (Priority: P1) 🎯 MVP

**Goal**: Business files return 3 months late, system automatically calculates late filing penalty as 15% of unpaid tax

**Independent Test**: Tax due $10,000, filed 3 months late → penalty = $10,000 × 5% × 3 = $1,500

### Backend Unit Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T058 [P] [US1] Create LateFilingPenaltyServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/LateFilingPenaltyServiceTest.java (test 1 month late, 3 months late, 6+ months late with 25% cap)
- [ ] T059 [P] [US1] Add parameterized tests for month calculation in LateFilingPenaltyServiceTest (test partial months rounded up)
- [ ] T060 [P] [US1] Add extension test in LateFilingPenaltyServiceTest (test extension moves due date, no penalty until after extended date)

### Backend Implementation for User Story 1

- [ ] T061 [US1] Implement LateFilingPenaltyService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/LateFilingPenaltyService.java (FR-001 to FR-006: calculate months late, apply 5% per month, cap at 25%)
- [ ] T062 [US1] Create PenaltyCalculationController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/PenaltyCalculationController.java (POST /api/penalties/calculate)
- [ ] T063 [US1] Add GET /api/penalties/{id} endpoint in PenaltyCalculationController (get penalty details)
- [ ] T064 [US1] Add GET /api/penalties endpoint in PenaltyCalculationController (list penalties with pagination)
- [ ] T065 [US1] Implement validation in LateFilingPenaltyService (unpaid tax >= 0, actual date >= due date, penalty <= maximum)
- [ ] T066 [US1] Add audit logging in LateFilingPenaltyService (create PenaltyAuditLog entry on every penalty assessment)

### Backend Integration Tests for User Story 1

- [ ] T067 [US1] Create PenaltyIntegrationTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/integration/PenaltyIntegrationTest.java (file return 3 months late → verify penalty calculated via GET /penalties/{id})

### Frontend Implementation for User Story 1

- [ ] T068 [P] [US1] Create penaltyService API client in src/services/penaltyService.ts (POST /penalties/calculate, GET /penalties, GET /penalties/{id})
- [ ] T069 [P] [US1] Create usePenaltyCalculation hook in src/hooks/usePenaltyCalculation.ts (React Query hook for penalty calculations)
- [ ] T070 [US1] Create PenaltySummaryCard component in src/components/penalties/PenaltySummaryCard.tsx (display penalty breakdown for return)
- [ ] T071 [US1] Create PenaltyBreakdownModal component in src/components/penalties/PenaltyBreakdownModal.tsx (detailed calculation: "Filed 3 months late → 5% × 3 = 15% penalty")
- [ ] T072 [US1] Create MonthCalculator component in src/components/shared/MonthCalculator.tsx (display "3 months late" calculation with tooltip)

### Frontend Tests for User Story 1

- [ ] T073 [P] [US1] Create PenaltySummaryCard.test.tsx in src/__tests__/components/PenaltySummaryCard.test.tsx (Vitest test: render penalty, display breakdown button)
- [ ] T074 [P] [US1] Create PenaltyBreakdownModal.test.tsx in src/__tests__/components/PenaltyBreakdownModal.test.tsx (Vitest test: display calculation steps)

### E2E Tests for User Story 1

- [ ] T075 [US1] Create penalty-calculation-e2e.spec.ts in src/__tests__/e2e/penalty-calculation-e2e.spec.ts (Playwright: login → view return → see late filing penalty → click breakdown → verify formula)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently (late filing penalty calculation)

---

## Phase 4: User Story 2 - Late Payment Penalty (1% Per Month, Max 25%) (Priority: P1)

**Goal**: Filer submits return on time but pays 4 months late, system calculates late payment penalty as 4% of unpaid tax

**Independent Test**: Tax due $5,000, paid 4 months late → penalty = $5,000 × 1% × 4 = $200

### Backend Unit Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T076 [P] [US2] Create LatePaymentPenaltyServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/LatePaymentPenaltyServiceTest.java (test 1 month, 4 months, 25+ months with 25% cap)
- [ ] T077 [P] [US2] Add partial payment test in LatePaymentPenaltyServiceTest (test penalty recalculation on remaining balance after partial payment)

### Backend Implementation for User Story 2

- [ ] T078 [US2] Implement LatePaymentPenaltyService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/LatePaymentPenaltyService.java (FR-007 to FR-011: calculate months late, apply 1% per month, handle partial payments)
- [ ] T079 [US2] Add POST /api/penalties/calculate endpoint logic in PenaltyCalculationController to support both filing and payment penalties
- [ ] T080 [US2] Implement PaymentAllocationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/PaymentAllocationService.java (FR-040 to FR-043: allocate payments to tax → penalties → interest)
- [ ] T081 [US2] Create PaymentAllocationController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/PaymentAllocationController.java (POST /api/payments/allocate, GET /api/payments/{id})
- [ ] T082 [US2] Add audit logging in LatePaymentPenaltyService and PaymentAllocationService

### Backend Integration Tests for User Story 2

- [ ] T083 [US2] Add late payment penalty workflow test in PenaltyIntegrationTest (file return on time → pay 4 months late → verify late payment penalty)
- [ ] T084 [US2] Add partial payment workflow test in PenaltyIntegrationTest (pay $5K of $10K tax → verify penalty on remaining $5K only)

### Frontend Implementation for User Story 2

- [ ] T085 [P] [US2] Create paymentAllocationService API client in src/services/paymentAllocationService.ts (POST /payments/allocate, GET /payments/{id})
- [ ] T086 [P] [US2] Create usePaymentAllocation hook in src/hooks/usePaymentAllocation.ts (React Query hook for payment allocation)
- [ ] T087 [US2] Create PaymentHistoryTimeline component in src/components/penalties/PaymentHistoryTimeline.tsx (visual timeline showing payments and balance reduction)
- [ ] T088 [US2] Add late payment penalty display to PenaltySummaryCard component (show both filing and payment penalties)

### Frontend Tests for User Story 2

- [ ] T089 [P] [US2] Create PaymentHistoryTimeline.test.tsx in src/__tests__/components/PaymentHistoryTimeline.test.tsx (Vitest test: display payment dates, balance changes)

### E2E Tests for User Story 2

- [ ] T090 [US2] Add late payment penalty workflow to penalty-calculation-e2e.spec.ts (file on time → pay late → verify late payment penalty)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently (late filing and late payment penalties)

---

## Phase 5: User Story 3 - Combined Late Filing & Late Payment Penalty Cap (Priority: P1)

**Goal**: Filer filed and paid 2 months late, system applies combined 5% per month cap (not 6%)

**Independent Test**: Tax due $10,000, filed and paid 2 months late → combined penalty = $10,000 × 5% × 2 = $1,000 (not $1,200)

### Backend Unit Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T091 [P] [US3] Create CombinedPenaltyCapServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/CombinedPenaltyCapServiceTest.java (test 2 months both late, 5 months both late, 6 months both late)
- [ ] T092 [P] [US3] Add transition test in CombinedPenaltyCapServiceTest (test filing penalty maxes at 25% after month 5, payment penalty continues)

### Backend Implementation for User Story 3

- [ ] T093 [US3] Implement CombinedPenaltyCapService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/CombinedPenaltyCapService.java (FR-012 to FR-014: apply 5% combined cap, handle transition after month 5)
- [ ] T094 [US3] Update PenaltyCalculationController to call CombinedPenaltyCapService when both penalties apply
- [ ] T095 [US3] Add combined penalty display to PenaltyCalculationResponse DTO (show breakdown by month with cap applied)

### Backend Integration Tests for User Story 3

- [ ] T096 [US3] Add combined penalty cap workflow test in PenaltyIntegrationTest (file and pay 2 months late → verify combined penalty = 10%, not 12%)

### Frontend Implementation for User Story 3

- [ ] T097 [US3] Update PenaltyBreakdownModal to show combined penalty cap calculation ("Month 1: 5% combined, Month 2: 5% combined")
- [ ] T098 [US3] Add combined cap indicator in PenaltySummaryCard (show "Combined cap applied" badge)

### Frontend Tests for User Story 3

- [ ] T099 [US3] Add combined penalty cap test to PenaltyBreakdownModal.test.tsx (verify cap calculation displayed)

### E2E Tests for User Story 3

- [ ] T100 [US3] Add combined penalty cap workflow to penalty-calculation-e2e.spec.ts (file and pay late → verify combined cap applied)

**Checkpoint**: At this point, all basic penalty types (US1, US2, US3) work independently and interact correctly

---

## Phase 6: User Story 4 - Quarterly Estimated Tax Underpayment Penalty with Safe Harbor (Priority: P1)

**Goal**: Self-employed filer paid estimated taxes but owes at year-end, system evaluates safe harbor rules and only assesses penalty if both safe harbors failed

**Independent Test**: Annual tax $20,000, paid $16,000 estimated, prior year tax $15,000 → passed safe harbor 2 (100% prior year) → NO penalty

### Backend Unit Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T101 [P] [US4] Create SafeHarborEvaluationServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/SafeHarborEvaluationServiceTest.java (test safe harbor 1 pass, safe harbor 2 pass, both fail, AGI > $150K with 110% rule)
- [ ] T102 [P] [US4] Add new business edge case test in SafeHarborEvaluationServiceTest (test no prior year tax → safe harbor 2 unavailable)

### Backend Implementation for User Story 4

- [ ] T103 [US4] Implement SafeHarborEvaluationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/SafeHarborEvaluationService.java (FR-015 to FR-019: check 90% current year OR 100%/110% prior year)
- [ ] T104 [US4] Add GET /api/estimated-tax/safe-harbor endpoint in PenaltyCalculationController (evaluate safe harbor status for taxpayer)
- [ ] T105 [US4] Update PenaltyCalculationController to skip underpayment penalty if either safe harbor met
- [ ] T106 [US4] Add safe harbor status to SafeHarborEvaluationDto (show which safe harbor passed, calculations)

### Backend Integration Tests for User Story 4

- [ ] T107 [US4] Add safe harbor pass workflow test in PenaltyIntegrationTest (meet safe harbor 2 → verify NO underpayment penalty)
- [ ] T108 [US4] Add safe harbor fail workflow test in PenaltyIntegrationTest (fail both safe harbors → verify underpayment penalty assessed)

### Frontend Implementation for User Story 4

- [ ] T109 [P] [US4] Create useSafeHarborStatus hook in src/hooks/useSafeHarborStatus.ts (React Query hook for safe harbor evaluation)
- [ ] T110 [US4] Create SafeHarborStatusBanner component in src/components/penalties/SafeHarborStatusBanner.tsx (display "✓ Safe Harbor Met" or "✗ Safe Harbor Failed" with explanation)
- [ ] T111 [US4] Add safe harbor status to return summary page (integrate SafeHarborStatusBanner)

### Frontend Tests for User Story 4

- [ ] T112 [P] [US4] Create SafeHarborStatusBanner.test.tsx in src/__tests__/components/SafeHarborStatusBanner.test.tsx (Vitest test: display pass/fail status)

### E2E Tests for User Story 4

- [ ] T113 [US4] Add safe harbor workflow to penalty-calculation-e2e.spec.ts (view estimated tax return → see safe harbor status → verify no penalty if passed)

**Checkpoint**: At this point, safe harbor evaluation (US4) works and prevents penalties when appropriate

---

## Phase 7: User Story 5 - Quarterly Estimated Tax Underpayment Penalty Calculation (Priority: P2)

**Goal**: Filer underpaid estimated taxes (failed safe harbor), system calculates penalty separately for each quarter with overpayment application

**Independent Test**: Q1 underpaid $3K, Q2 underpaid $2K, Q3 paid fully, Q4 overpaid $1K → Q4 overpayment reduces Q1 shortfall

### Backend Unit Tests for User Story 5

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T114 [P] [US5] Create EstimatedTaxPenaltyServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/EstimatedTaxPenaltyServiceTest.java (test quarterly calculation, overpayment application, annualized income method)
- [ ] T115 [P] [US5] Add 50+ parameterized tests in EstimatedTaxPenaltyServiceTest (test various underpayment scenarios per performance goals)

### Backend Implementation for User Story 5

- [ ] T116 [US5] Implement EstimatedTaxPenaltyService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/EstimatedTaxPenaltyService.java (FR-020 to FR-026: calculate quarterly underpayment, apply overpayments, retrieve rates from rule engine)
- [ ] T117 [US5] Add POST /api/estimated-tax/calculate-penalty endpoint in PenaltyCalculationController (calculate quarterly underpayment penalty)
- [ ] T118 [US5] Create EstimatedTaxPenaltyController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/EstimatedTaxPenaltyController.java (GET /api/estimated-tax-penalties/{id})

### Backend Integration Tests for User Story 5

- [ ] T119 [US5] Add quarterly underpayment penalty workflow test in PenaltyIntegrationTest (fail safe harbor → verify quarterly penalties calculated correctly)

### Frontend Implementation for User Story 5

- [ ] T120 [P] [US5] Create estimatedTaxPenaltyService API client in src/services/estimatedTaxPenaltyService.ts (POST /calculate-penalty, GET /estimated-tax-penalties/{id})
- [ ] T121 [US5] Create EstimatedTaxPenaltyTable component in src/components/penalties/EstimatedTaxPenaltyTable.tsx (display quarterly schedule: required, paid, underpaid, penalty per quarter)
- [ ] T122 [US5] Add estimated tax penalty section to PenaltySummaryCard (show total underpayment penalty with link to quarterly breakdown)

### Frontend Tests for User Story 5

- [ ] T123 [P] [US5] Create EstimatedTaxPenaltyTable.test.tsx in src/__tests__/components/EstimatedTaxPenaltyTable.test.tsx (Vitest test: display quarterly data, overpayment application)

### E2E Tests for User Story 5

- [ ] T124 [US5] Add estimated tax penalty workflow to penalty-calculation-e2e.spec.ts (fail safe harbor → view quarterly penalties → verify overpayment applied)

**Checkpoint**: At this point, User Stories 1-5 (all penalties) work independently and integrate correctly

---

## Phase 8: User Story 6 - Interest on Unpaid Tax with Quarterly Compounding (Priority: P2)

**Goal**: Filer with $10,000 unpaid tax for 12 months, system calculates compound interest quarterly (not simple interest)

**Independent Test**: $10,000 unpaid, 6% annual rate, 4 quarters → compound interest = $614 (not $600 simple interest)

### Backend Unit Tests for User Story 6

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T125 [P] [US6] Create InterestCalculationServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/InterestCalculationServiceTest.java (test quarterly compounding, rate versioning, split-period calculation)
- [ ] T126 [P] [US6] Add rate change test in InterestCalculationServiceTest (test interest rate changes mid-period)

### Backend Implementation for User Story 6

- [ ] T127 [US6] Implement InterestCalculationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/InterestCalculationService.java (FR-027 to FR-032: retrieve rate from rule engine, calculate daily interest, compound quarterly)
- [ ] T128 [US6] Create InterestCalculationController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/InterestCalculationController.java (POST /api/interest/calculate, GET /api/interest/{id})
- [ ] T129 [US6] Add interest rate versioning support in RuleEngineIntegrationService (retrieve rate effective for each quarter)

### Backend Integration Tests for User Story 6

- [ ] T130 [US6] Add interest calculation workflow test in PenaltyIntegrationTest (unpaid tax for 12 months → verify quarterly compounding)

### Frontend Implementation for User Story 6

- [ ] T131 [P] [US6] Create interestService API client in src/services/interestService.ts (POST /interest/calculate, GET /interest/{id})
- [ ] T132 [P] [US6] Create useInterestCalculation hook in src/hooks/useInterestCalculation.ts (React Query hook for interest calculations)
- [ ] T133 [US6] Create InterestCalculationCard component in src/components/penalties/InterestCalculationCard.tsx (display quarterly interest breakdown)
- [ ] T134 [US6] Add interest display to return summary page (show total interest with link to quarterly breakdown)

### Frontend Tests for User Story 6

- [ ] T135 [P] [US6] Create InterestCalculationCard.test.tsx in src/__tests__/components/InterestCalculationCard.test.tsx (Vitest test: display quarterly compounding)

### E2E Tests for User Story 6

- [ ] T136 [US6] Add interest calculation workflow to penalty-calculation-e2e.spec.ts (view return with unpaid tax → see interest calculation → verify compounding)

**Checkpoint**: At this point, all penalty and interest calculations (US1-US6) work independently

---

## Phase 9: User Story 7 - Penalty Abatement for Reasonable Cause (Priority: P3)

**Goal**: Filer filed late due to hospitalization, submits penalty abatement request with documentation, system generates Form 27-PA and tracks status

**Independent Test**: $1,000 late filing penalty → submit abatement request with hospital records → system generates Form 27-PA → auditor approves → penalty removed

### Backend Unit Tests for User Story 7

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T137 [P] [US7] Create PenaltyAbatementServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/PenaltyAbatementServiceTest.java (test first-time eligibility check, abatement approval workflow, return adjustment)
- [ ] T138 [P] [US7] Add first-time penalty abatement test in PenaltyAbatementServiceTest (test clean 3-year history validation)

### Backend Implementation for User Story 7

- [ ] T139 [US7] Implement PenaltyAbatementService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/PenaltyAbatementService.java (FR-033 to FR-039: track abatement requests, validate first-time eligibility, adjust return on approval)
- [ ] T140 [US7] Create PenaltyAbatementController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/PenaltyAbatementController.java (POST /api/abatements, GET /api/abatements/{id}, PATCH /api/abatements/{id}/review, POST /api/abatements/{id}/documents, GET /api/abatements/{id}/form-27pa)
- [ ] T141 [US7] Integrate PdfServiceIntegrationService to generate Form 27-PA (call pdf-service with abatement data)
- [ ] T142 [US7] Add document upload support in PenaltyAbatementController (multipart file upload for supporting documents)
- [ ] T143 [US7] Add audit logging in PenaltyAbatementService (log all status changes, approvals, denials)

### Backend Integration Tests for User Story 7

- [ ] T144 [US7] Add penalty abatement workflow test in PenaltyIntegrationTest (submit abatement → upload documents → auditor approves → verify penalty removed)

### Frontend Implementation for User Story 7

- [ ] T145 [P] [US7] Create abatementService API client in src/services/abatementService.ts (POST /abatements, GET /abatements/{id}, PATCH /review, POST /documents, GET /form-27pa)
- [ ] T146 [P] [US7] Create usePenaltyAbatement hook in src/hooks/usePenaltyAbatement.ts (React Query hook for abatement requests)
- [ ] T147 [US7] Create PenaltyAbatementWizard component in src/components/penalties/PenaltyAbatementWizard.tsx (multi-step form: Select penalty → Choose reason → Explain → Upload documents → Review → Submit)
- [ ] T148 [US7] Create Form27PaViewer component in src/components/penalties/Form27PaViewer.tsx (PDF viewer for generated Form 27-PA)
- [ ] T149 [US7] Add abatement request option to PenaltySummaryCard (show "Request Abatement" button if penalties assessed)
- [ ] T150 [US7] Create abatement status tracking UI in PenaltyAbatementWizard (display PENDING, APPROVED, DENIED status with reviewer notes)

### Frontend Tests for User Story 7

- [ ] T151 [P] [US7] Create PenaltyAbatementWizard.test.tsx in src/__tests__/components/PenaltyAbatementWizard.test.tsx (Vitest test: fill form, upload document, submit)
- [ ] T152 [P] [US7] Create Form27PaViewer.test.tsx in src/__tests__/components/Form27PaViewer.test.tsx (Vitest test: display PDF)

### E2E Tests for User Story 7

- [ ] T153 [US7] Add penalty abatement workflow to penalty-calculation-e2e.spec.ts (view penalty → request abatement → upload documents → verify Form 27-PA generated)

**Checkpoint**: All user stories (US1-US7) should now be independently functional with penalty abatement workflow

---

## Phase 10: Integration & Cross-Story Features

**Purpose**: Features that span multiple user stories and integration with other services

### Integration with Rule Engine (Spec 4)

- [ ] T154 Add RuleEngineIntegrationService methods to retrieve penalty rates (late filing 5%, late payment 1%, safe harbor thresholds)
- [ ] T155 Add RuleEngineIntegrationService methods to retrieve interest rates (updated quarterly, with effective dates)
- [ ] T156 Add Redis caching to RuleEngineIntegrationService (cache rates with 15-minute TTL)

### Integration with PDF Service (Spec 8)

- [ ] T157 Add Form 27-PA template to pdf-service (if not already exists)
- [ ] T158 Test PdfServiceIntegrationService Form 27-PA generation end-to-end

### Integration with Ledger (Spec 12)

- [ ] T159 Create ledger integration service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/LedgerIntegrationService.java (post penalty/interest transactions to double-entry ledger)
- [ ] T160 Add ledger posting on penalty assessment (debit penalty receivable, credit penalty revenue)
- [ ] T161 Add ledger posting on penalty abatement (reverse penalty entries)
- [ ] T162 Add ledger posting on payment allocation (debit cash, credit tax/penalty/interest receivable)

### Aggregate Penalty Report

- [ ] T163 [P] Implement PenaltyAggregationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/PenaltyAggregationService.java (FR-047: generate penalty summary report)
- [ ] T164 Add GET /api/penalties/summary endpoint in PenaltyCalculationController (return penalty summary for return)
- [ ] T165 Add penalty summary export to PenaltyAggregationService (CSV/PDF export)

### Payment Calculator Tool

- [ ] T166 [P] Create PenaltyCalculatorTool component in src/components/penalties/PenaltyCalculatorTool.tsx (what-if calculator: "If I pay in 1 month, 3 months, 6 months, how much penalty?")
- [ ] T167 Add penalty calculator to return summary page (interactive tool for planning)

**Checkpoint**: All integrations complete, feature fully integrated with ecosystem

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T168 [P] Add error handling to all API endpoints in controllers (ValidationException, BusinessException with proper HTTP status codes)
- [ ] T169 [P] Add logging to all services (MDC context with tenant_id, taxpayer_id, user_id)
- [ ] T170 [P] Add API documentation comments to all DTOs (JavaDoc for fields)
- [ ] T171 [P] Add accessibility attributes to all form components (aria-label, aria-describedby)
- [ ] T172 [P] Add loading states to all frontend components (skeleton loaders)
- [ ] T173 [P] Add error boundaries to frontend pages (ErrorBoundary component)
- [ ] T174 Performance optimization: Verify database query indexes per data-model.md V1.38 migration are optimal
- [ ] T175 Security hardening: Add rate limiting to penalty calculation endpoints (max 50 calculations per minute per user)
- [ ] T176 Security hardening: Add file size validation to abatement document upload (max 10MB per file, 50MB total)
- [ ] T177 Security hardening: Add virus scanning to document uploads (ClamAV integration)
- [ ] T178 Generate OpenAPI documentation: Run springdoc-openapi-maven-plugin to generate api-docs.yaml
- [ ] T179 Code cleanup: Run Checkstyle and fix violations (refer to /backend/checkstyle.xml for project configuration)
- [ ] T180 Code cleanup: Run ESLint and fix violations (refer to /.eslintrc.json for project configuration)
- [ ] T181 [P] Update README.md with enhanced penalty & interest calculation feature overview
- [ ] T182 [P] Create PENALTY_CALCULATION_GUIDE.md in docs/ folder (explain penalty types, safe harbor rules, abatement process)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-9)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Integration (Phase 10)**: Depends on relevant user stories being complete
- **Polish (Phase 11)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 3 (P1)**: Depends on User Story 1 AND User Story 2 completion (combined penalty cap requires both penalty types)
- **User Story 4 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories (safe harbor evaluation)
- **User Story 5 (P2)**: Depends on User Story 4 completion (underpayment penalty calculation requires safe harbor evaluation)
- **User Story 6 (P2)**: Can start after Foundational (Phase 2) - No dependencies on other stories (interest calculation)
- **User Story 7 (P3)**: Depends on User Stories 1, 2, 5 completion (abatement applies to all penalty types)

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

- **Setup**: T001-T005 can run in parallel (5 verification tasks)
- **Foundational - Migrations**: T006-T015 can run in parallel (10 migration files)
- **Foundational - Enums**: T016-T026 can run in parallel (11 enum files)
- **Foundational - Entities**: T027-T034 can run in parallel (8 entity files)
- **Foundational - Repositories**: T035-T042 can run in parallel (8 repository files)
- **Foundational - DTOs**: T043-T051 can run in parallel (9 DTO files)
- **Foundational - Frontend Types**: T054-T057 can run in parallel (4 type files)
- **User Story 1 - Unit Tests**: T058-T060 can run in parallel (3 test files)
- **User Story 1 - Frontend**: T068-T069 can run in parallel (2 files: API client + hook)
- **User Story 1 - Frontend Tests**: T073-T074 can run in parallel (2 component tests)
- **User Story 2 - Unit Tests**: T076-T077 can run in parallel (2 test files)
- **User Story 2 - Frontend**: T085-T086 can run in parallel (API client + hook)
- **User Story 4 - Frontend**: T109 (hook) can run with T110 (component) in parallel
- **User Story 5 - Frontend**: T120 (API client) can run in parallel with T121 (component)
- **User Story 6 - Frontend**: T131-T132 can run in parallel (API client + hook)
- **User Story 7 - Frontend**: T145-T146 can run in parallel (API client + hook)
- **User Story 7 - Frontend Tests**: T151-T152 can run in parallel (2 component tests)
- **Integration**: T154-T155, T159-T162 can run in parallel (different services)
- **Integration - Aggregate**: T163-T165 can run in parallel (service + endpoint + export)
- **Polish - Documentation**: T168-T173 can run in parallel (different areas)
- **Polish - Final Tasks**: T181-T182 can run in parallel (documentation updates)
- **Once Foundational phase completes**: User Stories 1, 2, 4, 6 can start in parallel (all independent)

---

## Parallel Example: User Story 1

```bash
# Launch all unit tests for User Story 1 together:
Task T058: "Create LateFilingPenaltyServiceTest"
Task T059: "Add parameterized tests for month calculation"
Task T060: "Add extension test"

# Launch all frontend services for User Story 1 together:
Task T068: "Create penaltyService API client"
Task T069: "Create usePenaltyCalculation hook"

# Launch all frontend component tests for User Story 1 together:
Task T073: "Create PenaltySummaryCard.test.tsx"
Task T074: "Create PenaltyBreakdownModal.test.tsx"
```

---

## Parallel Example: Foundational Phase

```bash
# Launch all enum files together:
Task T016-T026: "Create all 11 enum files in parallel"

# Launch all entity files together:
Task T027-T034: "Create all 8 entity files in parallel"

# Launch all repository files together:
Task T035-T042: "Create all 8 repository files in parallel"
```

---

## Implementation Strategy

### MVP First (User Stories 1, 2, 3 Only)

Since US1, US2, and US3 are all Priority P1 and represent the core penalty calculations:

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (late filing penalty)
4. Complete Phase 4: User Story 2 (late payment penalty)
5. Complete Phase 5: User Story 3 (combined penalty cap)
6. **STOP and VALIDATE**: Test US1 + US2 + US3 together (file late, pay late, verify combined cap)
7. Deploy/demo if ready

**MVP Value**: Taxpayers can see accurate late filing and late payment penalties with proper combined cap, covering the most common penalty scenarios.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (late filing penalties only)
3. Add User Story 2 → Test with US1 → Deploy/Demo (late filing + late payment)
4. Add User Story 3 → Test with US1+US2 → Deploy/Demo (MVP! Combined cap applied)
5. Add User Story 4 → Test independently → Deploy/Demo (safe harbor evaluation)
6. Add User Story 5 → Test with US4 → Deploy/Demo (underpayment penalty calculation)
7. Add User Story 6 → Test independently → Deploy/Demo (interest calculation)
8. Add User Story 7 → Test with US1+US2+US5 → Deploy/Demo (penalty abatement)
9. Add Integration features → Deploy/Demo (full ecosystem integration)
10. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers after Foundational phase completes:

**Scenario 1: 2 Developers**
- Developer A: User Story 1 (P1) - Late filing penalty
- Developer B: User Story 2 (P1) - Late payment penalty
- Once both complete: Developer A does US3 (combines US1+US2)

**Scenario 2: 3 Developers**
- Developer A: User Story 1 (P1)
- Developer B: User Story 2 (P1)
- Developer C: User Story 4 (P1) - Safe harbor (independent)
- Then: Dev A does US3, Dev C does US5 (depends on US4)

**Scenario 3: Full Team (5 developers)**
- Dev A: US1 (P1)
- Dev B: US2 (P1)
- Dev C: US4 (P1) - independent
- Dev D: US6 (P2) - independent
- Dev E: US3 (P1) - waits for US1+US2
- Then: Dev C does US5 (depends on US4), Dev E does US7 (depends on US1+US2+US5)

---

## Summary

**Total Tasks**: 182 tasks
- **Phase 1 (Setup)**: 5 tasks
- **Phase 2 (Foundational)**: 52 tasks (10 migrations + 11 enums + 8 entities + 8 repositories + 9 DTOs + 2 core services + 4 frontend types)
- **Phase 3 (User Story 1)**: 18 tasks (3 unit tests + 6 backend + 1 integration test + 5 frontend + 2 frontend tests + 1 E2E test)
- **Phase 4 (User Story 2)**: 15 tasks (2 unit tests + 5 backend + 2 integration tests + 4 frontend + 1 frontend test + 1 E2E test)
- **Phase 5 (User Story 3)**: 10 tasks (2 unit tests + 3 backend + 1 integration test + 2 frontend + 1 frontend test + 1 E2E test)
- **Phase 6 (User Story 4)**: 13 tasks (2 unit tests + 4 backend + 2 integration tests + 3 frontend + 1 frontend test + 1 E2E test)
- **Phase 7 (User Story 5)**: 11 tasks (2 unit tests + 3 backend + 1 integration test + 3 frontend + 1 frontend test + 1 E2E test)
- **Phase 8 (User Story 6)**: 12 tasks (2 unit tests + 3 backend + 1 integration test + 4 frontend + 1 frontend test + 1 E2E test)
- **Phase 9 (User Story 7)**: 17 tasks (2 unit tests + 5 backend + 1 integration test + 6 frontend + 2 frontend tests + 1 E2E test)
- **Phase 10 (Integration)**: 14 tasks (rule engine, pdf service, ledger integration, aggregate reporting)
- **Phase 11 (Polish)**: 15 tasks

**Task Breakdown by User Story**:
- **US1**: 18 tasks (P1 - Late filing penalty)
- **US2**: 15 tasks (P1 - Late payment penalty)
- **US3**: 10 tasks (P1 - Combined penalty cap)
- **US4**: 13 tasks (P1 - Safe harbor evaluation)
- **US5**: 11 tasks (P2 - Underpayment penalty calculation)
- **US6**: 12 tasks (P2 - Interest calculation)
- **US7**: 17 tasks (P3 - Penalty abatement)

**Parallel Opportunities Identified**: 20+ groups of parallelizable tasks across all phases

**Independent Test Criteria**:
- **US1**: File 3 months late → verify penalty = $10,000 × 5% × 3 = $1,500
- **US2**: Pay 4 months late → verify penalty = $5,000 × 1% × 4 = $200
- **US3**: File and pay 2 months late → verify combined penalty = $1,000 (not $1,200)
- **US4**: Meet safe harbor 2 → verify NO underpayment penalty
- **US5**: Underpay Q1 $3K, Q2 $2K, overpay Q4 $1K → verify Q4 applied to Q1
- **US6**: $10,000 unpaid, 6% rate, 4 quarters → verify compound interest = $614
- **US7**: Submit abatement → upload documents → verify Form 27-PA generated

**Suggested MVP Scope**: 
- Phase 1 (Setup): 5 tasks
- Phase 2 (Foundational): 52 tasks
- Phase 3 (User Story 1): 18 tasks
- Phase 4 (User Story 2): 15 tasks
- Phase 5 (User Story 3): 10 tasks
- **Total MVP**: 100 tasks (55% of total)

This MVP delivers the complete P1 requirements: late filing penalty, late payment penalty, and combined penalty cap - covering the most common penalty scenarios taxpayers encounter.

---

## Notes

- [P] tasks = different files, no dependencies (can run in parallel)
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD approach)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All file paths are absolute from repository root
- All database migrations follow sequential versioning (V1.30 through V1.39)
- All entities include tenant_id for multi-tenant isolation per Constitution II
- All audit-sensitive actions logged to PenaltyAuditLog per Constitution III
- Penalty abatement workflow provides transparency per Constitution IV
- Integration with rule engine (Spec 4) for configurable penalty/interest rates
- Integration with PDF service (Spec 8) for Form 27-PA generation
- Integration with ledger (Spec 12) for financial transaction posting
