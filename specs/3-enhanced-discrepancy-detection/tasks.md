# Tasks: Enhanced Discrepancy Detection (10+ Validation Rules)

**Feature**: Enhanced Discrepancy Detection  
**Branch**: `3-enhanced-discrepancy-detection`  
**Input**: Design documents from `/specs/3-enhanced-discrepancy-detection/`

**Project Structure**:
- Backend: Java Spring Boot in `backend/tax-engine-service/`
- Frontend: React/TypeScript in `components/` and `services/`

**Organization**: Tasks are grouped by user story (US1-US10 from spec.md) to enable independent implementation and testing of each story. Each story delivers value independently and can be tested without waiting for other stories to complete.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and database schema setup

- [ ] T001 Create database migration script for discrepancy tables in backend/tax-engine-service/src/main/resources/db/migration/V1_4__create_discrepancy_tables.sql
- [ ] T002 [P] Create DiscrepancyReport model in backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/DiscrepancyReport.java
- [ ] T003 [P] Create DiscrepancyIssue model in backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/DiscrepancyIssue.java
- [ ] T004 [P] Create ValidationRule enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/ValidationRule.java
- [ ] T005 [P] Create TypeScript interfaces in frontend/types/discrepancy.ts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core validation infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 Create DiscrepancyReportRepository in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/DiscrepancyReportRepository.java
- [ ] T007 Create DiscrepancyIssueRepository in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/DiscrepancyIssueRepository.java
- [ ] T008 Create DiscrepancyValidator service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/DiscrepancyValidator.java
- [ ] T009 Add validation endpoint POST /validate to TaxEngineController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/TaxEngineController.java
- [ ] T010 Add accept issue endpoint POST /validate/{reportId}/issues/{issueId}/accept to TaxEngineController
- [ ] T011 Add get report endpoint GET /validate/{reportId}/report to TaxEngineController
- [ ] T012 Create validateTaxReturn API function in services/taxEngineService.ts
- [ ] T013 [P] Create acceptIssue API function in services/taxEngineService.ts
- [ ] T014 [P] Create DiscrepancyView React component shell in components/DiscrepancyView.tsx
- [ ] T015 [P] Create DiscrepancyIssueCard React component in components/DiscrepancyIssueCard.tsx
- [ ] T016 [P] Create DiscrepancySummary React component in components/DiscrepancySummary.tsx

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - W-2 Box Validation Catches Data Entry Error (Priority: P1) 🎯 MVP

**Goal**: Detect W-2 Box 1 vs Box 18 variance to catch data entry errors like missing zeros or transposition

**Independent Test**: Enter W-2 with Box 1 = $75,000 and Box 18 = $7,500. System should flag HIGH severity warning "Box 18 is 90% lower than Box 1 - verify data entry"

**Acceptance Scenarios**: 
- FR-001: Box 18 variance >20% from Box 1 → HIGH severity
- FR-001: Box 18 variance 10-20% from Box 1 → MEDIUM severity
- FR-001: Multiple W-2s validated independently
- FR-004: Calculate implied withholding rate and flag if excessive

### Implementation for User Story 1

- [ ] T017 [P] [US1] Create W2Validator service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/W2Validator.java
- [ ] T018 [US1] Implement FR-001 (Box 1 vs Box 18 variance validation) in W2Validator.validateBoxVariance() method
- [ ] T019 [US1] Implement FR-002 (withholding rate validation) in W2Validator.validateWithholdingRate() method
- [ ] T020 [US1] Implement FR-003 (duplicate W-2 detection) in W2Validator.detectDuplicates() method
- [ ] T021 [US1] Implement FR-004 (employer jurisdiction check) in W2Validator.validateEmployerJurisdiction() method
- [ ] T022 [US1] Implement FR-005 (corrected vs duplicate flag) in W2Validator.validateCorrectedFlag() method
- [ ] T023 [US1] Wire W2Validator into DiscrepancyValidator.validateTaxReturn() method
- [ ] T024 [US1] Add W-2 validation tests in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/W2ValidatorTest.java
- [ ] T025 [US1] Update DiscrepancyView component to display W-2 category issues in components/DiscrepancyView.tsx

**Checkpoint**: User Story 1 complete - W-2 validation rules working end-to-end

---

## Phase 4: User Story 2 - Schedule C Income vs Estimated Tax Paid Mismatch (Priority: P1)

**Goal**: Detect missing estimated tax payments for self-employed taxpayers to prevent underpayment penalties

**Independent Test**: Enter Schedule C with $120,000 net profit and $0 estimated payments. System should calculate required payments ($2,160) and flag WARNING "Estimated tax payments appear to be missing"

**Acceptance Scenarios**:
- FR-006: Schedule C profit with $0 estimates → WARNING with required payment calculation
- FR-006: Quarterly estimates totaling ≥90% of liability → Pass
- FR-006: Prior year safe harbor (100% of prior year) → Alternative calculation
- FR-006: W-2 withholding combined with estimates → Total coverage check

### Implementation for User Story 2

- [ ] T026 [P] [US2] Create ScheduleValidator service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/ScheduleValidator.java
- [ ] T027 [US2] Implement FR-006 (estimated tax validation) in ScheduleValidator.validateEstimatedTax() method
- [ ] T028 [US2] Implement FR-010 (Schedule C vs 1099-K reconciliation) in ScheduleValidator.reconcileWith1099K() method
- [ ] T029 [US2] Wire ScheduleValidator into DiscrepancyValidator.validateTaxReturn() method
- [ ] T030 [US2] Add Schedule C validation tests in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/ScheduleValidatorTest.java
- [ ] T031 [US2] Update DiscrepancyView to display Schedule C category issues

**Checkpoint**: User Story 2 complete - Schedule C estimated tax validation working

---

## Phase 5: User Story 3 - Schedule E Rental Property Count Validation (Priority: P2)

**Goal**: Detect incomplete Schedule E data when property count doesn't match address count

**Independent Test**: Enter Schedule E with 3 rental properties showing income but only 2 addresses provided. System should flag MEDIUM severity "Missing property address for Rental #3"

**Acceptance Scenarios**:
- FR-007: 3 properties with income but only 2 addresses → MEDIUM warning
- FR-007: All properties have complete data → Pass
- FR-008: Property address outside Dublin → Jurisdiction flag
- FR-009: Rental loss with AGI >$150K → Passive loss limit informational note

### Implementation for User Story 3

- [ ] T032 [US3] Implement FR-007 (rental property count validation) in ScheduleValidator.validateRentalPropertyCount() method
- [ ] T033 [US3] Implement FR-008 (Schedule E jurisdiction check) in ScheduleValidator.validateRentalJurisdiction() method
- [ ] T034 [US3] Implement FR-009 (passive loss limits) in ScheduleValidator.checkPassiveLossLimits() method
- [ ] T035 [US3] Add Schedule E validation tests in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/ScheduleValidatorTest.java

**Checkpoint**: User Story 3 complete - Schedule E property validation working

---

## Phase 6: User Story 4 - K-1 Income Allocation Check (Priority: P2)

**Goal**: Validate K-1 income components are fully reported including ordinary income and guaranteed payments

**Independent Test**: Upload K-1 with Box 1 (ordinary income) = $100,000 and Box 4c (guaranteed payments) = $50,000. If Schedule X only shows $100,000, system should flag "Missing $50,000 guaranteed payments from K-1"

**Acceptance Scenarios**:
- FR-011: K-1 has ordinary income + guaranteed payments but Schedule X missing payments → MEDIUM warning
- FR-011: Both components properly reported → Pass
- FR-012: Partner's profit share validated against partnership total
- FR-013: Section 179 deduction flagged for municipal adjustment

### Implementation for User Story 4

- [ ] T036 [P] [US4] Create K1Validator service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/K1Validator.java
- [ ] T037 [US4] Implement FR-011 (K-1 component completeness) in K1Validator.validateComponentCompleteness() method
- [ ] T038 [US4] Implement FR-012 (K-1 profit share allocation) in K1Validator.validateProfitShareAllocation() method
- [ ] T039 [US4] Implement FR-013 (K-1 municipal adjustment items) in K1Validator.checkMunicipalAdjustments() method
- [ ] T040 [US4] Wire K1Validator into DiscrepancyValidator.validateTaxReturn() method
- [ ] T041 [US4] Add K-1 validation tests in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/K1ValidatorTest.java
- [ ] T042 [US4] Update DiscrepancyView to display K-1 category issues

**Checkpoint**: User Story 4 complete - K-1 validation working

---

## Phase 7: User Story 5 - Municipal Credit Limit Validation (Priority: P1)

**Goal**: Enforce municipal credit limits to prevent credits from exceeding tax liability (no refundable credits)

**Independent Test**: Enter Schedule Y with $3,000 Cleveland credit when Dublin liability is $2,500. System should cap credit at $2,500 and show WARNING "Municipal credit limited to Dublin tax liability - $500 credit cannot be applied"

**Acceptance Scenarios**:
- FR-014: Credit ($3,000) exceeds liability ($2,500) → HIGH severity, cap credit
- FR-014: Multiple credits totaling more than liability → Apply in order, cap total
- FR-015: Credit order of application validated
- FR-016: Credit percentage limits enforced

### Implementation for User Story 5

- [ ] T043 [P] [US5] Create CreditValidator service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/CreditValidator.java
- [ ] T044 [US5] Implement FR-014 (credit exceeds liability validation) in CreditValidator.validateCreditLimits() method
- [ ] T045 [US5] Implement FR-015 (credit order of application) in CreditValidator.validateCreditOrder() method
- [ ] T046 [US5] Implement FR-016 (credit percentage limits) in CreditValidator.validateCreditPercentages() method
- [ ] T047 [US5] Wire CreditValidator into DiscrepancyValidator.validateTaxReturn() method
- [ ] T048 [US5] Add municipal credit validation tests in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/CreditValidatorTest.java
- [ ] T049 [US5] Update DiscrepancyView to display Municipal Credit category issues

**Checkpoint**: User Story 5 complete - Municipal credit validation working

---

## Phase 8: User Story 6 - Withholding Rate Validation (Priority: P2)

**Goal**: Detect employer over-withholding or data entry errors by validating withholding rate

**Independent Test**: Enter W-2 with Box 18 = $50,000 and Box 19 = $1,500 (3.0% rate). System should flag MEDIUM severity "Withholding rate of 3.0% exceeds Dublin rate of 2.5%"

**Acceptance Scenarios**:
- FR-002: Withholding rate 3.0% (exceeds 2.5%) → MEDIUM warning
- FR-002: Withholding rate 2.0% (within tolerance) → Pass
- FR-002: $0 withholding on $50,000 wages → MEDIUM warning
- FR-002: Cleveland withholding instead of Dublin → Flag wrong municipality

**Note**: This is already implemented in User Story 1 (FR-002). No additional implementation needed.

- [ ] T050 [US6] Add additional withholding rate edge case tests to W2ValidatorTest.java
- [ ] T051 [US6] Verify withholding rate validation displays correctly in DiscrepancyView

**Checkpoint**: User Story 6 complete - Withholding rate validation verified

---

## Phase 9: User Story 7 - Cross-Year Carryforward Verification (Priority: P3)

**Goal**: Validate carryforward amounts against prior year returns to prevent fraud and errors

**Independent Test**: Enter 2024 return claiming $5,000 NOL carryforward, but 2023 return shows $0 NOL. System should flag HIGH severity "Carryforward amount does not match prior year return"

**Acceptance Scenarios**:
- FR-020: 2024 claims $5K NOL but 2023 shows $0 → HIGH warning
- FR-020: 2023 generated $5K NOL and 2024 claims $5K → Pass
- FR-021: Safe harbor calculation using prior year liability
- FR-022: Multi-year NOL tracking and utilization

### Implementation for User Story 7

- [ ] T052 [P] [US7] Create PriorYearReturnService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/PriorYearReturnService.java
- [ ] T053 [P] [US7] Create CarryforwardValidator service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/CarryforwardValidator.java
- [ ] T054 [US7] Implement FR-020 (carryforward source verification) in CarryforwardValidator.validateCarryforwardSource() method
- [ ] T055 [US7] Implement FR-021 (safe harbor prior year query) in CarryforwardValidator.validateSafeHarbor() method
- [ ] T056 [US7] Implement FR-022 (multi-year NOL tracking) in CarryforwardValidator.trackNOLUtilization() method
- [ ] T057 [US7] Implement getPriorYearReturn() method in PriorYearReturnService
- [ ] T058 [US7] Wire CarryforwardValidator into DiscrepancyValidator.validateTaxReturn() method
- [ ] T059 [US7] Add carryforward validation tests in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/CarryforwardValidatorTest.java
- [ ] T060 [US7] Update DiscrepancyView to display Carryforward category issues

**Checkpoint**: User Story 7 complete - Carryforward validation working

---

## Phase 10: User Story 8 - Federal Form 1040 AGI vs Local Calculation (Priority: P2)

**Goal**: Reconcile Federal Form 1040 AGI with local income calculation to detect missing income or errors

**Independent Test**: Upload Form 1040 with AGI = $100,000, but W-2 wages = $80,000 and Schedule C = $15,000 (total $95,000). System should flag MEDIUM severity "$5,000 income difference between Federal AGI and local calculation"

**Acceptance Scenarios**:
- FR-017: Form 1040 AGI is $100K, local calc is $95K → MEDIUM warning
- FR-017: Difference explained by non-taxable interest → Informational note
- FR-018: Unemployment compensation identified as non-taxable locally
- FR-019: Federal wages match sum of W-2 Box 1 amounts

### Implementation for User Story 8

- [ ] T061 [P] [US8] Create ReconciliationValidator service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/ReconciliationValidator.java
- [ ] T062 [US8] Implement FR-017 (Federal AGI reconciliation) in ReconciliationValidator.reconcileFederalAGI() method
- [ ] T063 [US8] Implement FR-018 (non-taxable income identification) in ReconciliationValidator.identifyNonTaxableIncome() method
- [ ] T064 [US8] Implement FR-019 (Federal wages vs W-2 sum) in ReconciliationValidator.validateFederalWagesSum() method
- [ ] T065 [US8] Wire ReconciliationValidator into DiscrepancyValidator.validateTaxReturn() method
- [ ] T066 [US8] Add reconciliation validation tests in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/ReconciliationValidatorTest.java
- [ ] T067 [US8] Update DiscrepancyView to display Federal Reconciliation category issues

**Checkpoint**: User Story 8 complete - Federal reconciliation working

---

## Phase 11: User Story 9 - Duplicate W-2 Detection (Priority: P2)

**Goal**: Detect and prevent duplicate W-2 uploads to avoid double-counting income

**Independent Test**: Upload W-2 with Employer EIN 12-3456789, SSN 123-45-6789, wages $50,000, then upload identical W-2 again. System should flag HIGH severity "Duplicate W-2 detected"

**Acceptance Scenarios**:
- FR-003: Two W-2s with identical EIN + SSN + wages → HIGH severity
- FR-003: Same employer but different wages → Accept both (multiple positions)
- FR-003: Corrected W-2 marked as replacement → Archive original
- FR-003: Joint filers with W-2s from same employer → Accept both (different SSNs)

**Note**: This is already implemented in User Story 1 (FR-003). No additional implementation needed.

- [ ] T068 [US9] Add duplicate detection edge case tests to W2ValidatorTest.java
- [ ] T069 [US9] Verify duplicate W-2 detection displays correctly with recommended actions

**Checkpoint**: User Story 9 complete - Duplicate detection verified

---

## Phase 12: User Story 10 - Schedule E Passive Loss Limitation Check (Priority: P3)

**Goal**: Inform taxpayers about passive loss limits when AGI exceeds threshold

**Independent Test**: Enter Schedule E rental loss of $25,000 with AGI of $180,000. System should show informational "AGI exceeds $150,000 passive loss threshold - rental loss may be limited"

**Acceptance Scenarios**:
- FR-009: Rental loss $25K with AGI $180K → LOW informational warning
- FR-009: Rental loss $10K with AGI $120K → Pass (under threshold)
- FR-009: Real estate professional checked → Skip passive loss validation
- FR-009: Suspended losses with current rental income → Prompt for utilization

**Note**: This is already implemented in User Story 3 (FR-009). No additional implementation needed.

- [ ] T070 [US10] Add passive loss edge case tests to ScheduleValidatorTest.java
- [ ] T071 [US10] Verify passive loss warnings display with proper informational severity

**Checkpoint**: User Story 10 complete - Passive loss validation verified

---

## Phase 13: Integration & Filing Submission Gate

**Purpose**: Integrate validation with filing submission and implement blocking logic

- [ ] T072 Update SubmissionController to check for blocking discrepancies in backend/submission-service/src/main/java/com/munitax/submission/controller/SubmissionController.java
- [ ] T073 Add filing submission gate test in backend/submission-service/src/test/java/com/munitax/submission/controller/SubmissionControllerTest.java
- [ ] T074 Add "Review & Submit" button to tax return form that triggers validation in frontend
- [ ] T075 Implement blocking UI when HIGH severity issues exist in frontend
- [ ] T076 Add acceptance workflow for MEDIUM/LOW severity issues in DiscrepancyView
- [ ] T077 Persist discrepancy reports with return submissions for audit trail

**Checkpoint**: Filing submission gate working - HIGH severity blocks, MEDIUM/LOW allows with acceptance

---

## Phase 14: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements, documentation, and validation

- [ ] T078 [P] Add validation rules version tracking in backend configuration
- [ ] T079 [P] Add tenant-specific threshold configuration to TaxRulesConfig
- [ ] T080 [P] Implement validation report PDF generation endpoint in pdf-service
- [ ] T081 [P] Add comprehensive integration tests for all 22 validation rules
- [ ] T082 [P] Update API documentation with validation endpoints
- [ ] T083 [P] Add error handling and logging for validation failures
- [ ] T084 [P] Optimize database queries for prior year return lookups
- [ ] T085 Add indexes on discrepancy tables for performance
- [ ] T086 Run quickstart.md validation scenarios
- [ ] T087 Update user documentation with validation feature guide

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-12)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Integration (Phase 13)**: Depends on User Stories 1, 2, 5 (critical path)
- **Polish (Phase 14)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1) - W-2 Validation**: Independent, can start after Foundational
- **User Story 2 (P1) - Schedule C Estimated Tax**: Independent, can start after Foundational
- **User Story 3 (P2) - Schedule E Property Count**: Extends US2 (ScheduleValidator), minimal dependency
- **User Story 4 (P2) - K-1 Allocation**: Independent, can start after Foundational
- **User Story 5 (P1) - Municipal Credit Limits**: Independent, can start after Foundational
- **User Story 6 (P2) - Withholding Rate**: Already in US1, verification only
- **User Story 7 (P3) - Carryforward Verification**: Independent, can start after Foundational
- **User Story 8 (P2) - Federal Reconciliation**: Independent, can start after Foundational
- **User Story 9 (P2) - Duplicate W-2**: Already in US1, verification only
- **User Story 10 (P3) - Passive Loss**: Already in US3, verification only

### Within Each User Story

- Models before services
- Services before controller wiring
- Implementation before tests
- Backend before frontend display

### Parallel Opportunities

**Phase 1 (Setup)**: All tasks marked [P] can run in parallel (T002, T003, T004, T005)

**Phase 2 (Foundational)**: Tasks T012-T016 marked [P] can run in parallel (frontend services/components)

**User Story Implementation**: After Foundational completes, these can run in parallel:
- US1 (W-2 Validation) - Developer A
- US2 (Schedule C) - Developer B  
- US4 (K-1) - Developer C
- US5 (Municipal Credit) - Developer D
- US7 (Carryforward) - Developer E
- US8 (Reconciliation) - Developer F

**Phase 14 (Polish)**: All tasks marked [P] can run in parallel (T078-T084)

---

## Parallel Example: User Story 1 (W-2 Validation)

```bash
# Launch model creation together:
Task T017: "Create W2Validator service"

# Launch all FR implementations in parallel (different methods):
Task T018: "Implement FR-001 (Box variance)"
Task T019: "Implement FR-002 (Withholding rate)"
Task T020: "Implement FR-003 (Duplicate detection)"
Task T021: "Implement FR-004 (Jurisdiction)"
Task T022: "Implement FR-005 (Corrected flag)"

# Wire up and test:
Task T023: "Wire W2Validator into DiscrepancyValidator"
Task T024: "Add W-2 validation tests"
Task T025: "Update DiscrepancyView component"
```

---

## Implementation Strategy

### MVP First (User Stories 1, 2, 5 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (W-2 Validation)
4. Complete Phase 4: User Story 2 (Schedule C)
5. Complete Phase 7: User Story 5 (Municipal Credits)
6. Complete Phase 13: Integration & Filing Gate
7. **STOP and VALIDATE**: Test P1 user stories independently
8. Deploy/demo MVP with core validations

**MVP Scope**: 3 P1 user stories covering most common errors (W-2 data entry, missing estimated payments, credit limits)

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 5 → Test independently → Deploy/Demo (Core MVP)
5. Add User Story 4 → Test independently → Deploy/Demo
6. Add User Story 8 → Test independently → Deploy/Demo
7. Add User Story 3 → Test independently → Deploy/Demo
8. Add User Story 7 → Test independently → Deploy/Demo
9. Verify User Stories 6, 9, 10 (already implemented)
10. Complete Integration & Polish

### Parallel Team Strategy

With 3-4 developers:

1. Team completes Setup + Foundational together (Days 1-2)
2. Once Foundational is done (Day 3+):
   - Developer A: User Story 1 (W-2)
   - Developer B: User Story 2 (Schedule C) + User Story 3 (Schedule E)
   - Developer C: User Story 4 (K-1) + User Story 8 (Reconciliation)
   - Developer D: User Story 5 (Credits) + User Story 7 (Carryforward)
3. Stories complete and integrate independently
4. Team completes Integration & Polish together

---

## Task Summary

**Total Tasks**: 87 tasks
- Phase 1 (Setup): 5 tasks
- Phase 2 (Foundational): 11 tasks
- Phase 3 (US1 - W-2): 9 tasks
- Phase 4 (US2 - Schedule C): 6 tasks
- Phase 5 (US3 - Schedule E): 4 tasks
- Phase 6 (US4 - K-1): 7 tasks
- Phase 7 (US5 - Credits): 7 tasks
- Phase 8 (US6 - Withholding): 2 tasks
- Phase 9 (US7 - Carryforward): 9 tasks
- Phase 10 (US8 - Reconciliation): 7 tasks
- Phase 11 (US9 - Duplicates): 2 tasks
- Phase 12 (US10 - Passive Loss): 2 tasks
- Phase 13 (Integration): 6 tasks
- Phase 14 (Polish): 10 tasks

**Tasks by User Story**:
- US1: 9 tasks (W-2 validation with 5 FR rules)
- US2: 6 tasks (Schedule C estimated tax)
- US3: 4 tasks (Schedule E property validation)
- US4: 7 tasks (K-1 allocation checks)
- US5: 7 tasks (Municipal credit limits)
- US6: 2 tasks (Withholding rate - already in US1)
- US7: 9 tasks (Carryforward verification)
- US8: 7 tasks (Federal reconciliation)
- US9: 2 tasks (Duplicate detection - already in US1)
- US10: 2 tasks (Passive loss - already in US3)

**Parallel Opportunities**: 35+ tasks marked [P] across all phases

**Independent Test Criteria**: Each user story (US1-US10) has specific test scenarios from spec.md that can be validated independently without other stories

**MVP Scope**: User Stories 1, 2, 5 (Priority P1) - 22 tasks covering most common validation errors

---

## Notes

- [P] tasks = different files, no dependencies, can run in parallel
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- All 22 functional requirements (FR-001 through FR-022) are mapped to tasks
- Backend validators organized by domain (W2Validator, ScheduleValidator, K1Validator, CreditValidator, ReconciliationValidator, CarryforwardValidator)
- Frontend components handle all severity levels (HIGH blocks filing, MEDIUM/LOW allow acceptance)
- Database schema supports audit trail with acceptance tracking
- Cross-year validation queries prior year returns with tenant scoping
- Validation runs on-demand when user clicks "Review & Submit" button
