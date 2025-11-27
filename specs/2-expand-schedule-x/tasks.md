# Tasks: Comprehensive Business Schedule X Reconciliation (25+ Fields)

**Feature Branch**: `2-expand-schedule-x`  
**Input**: Design documents from `/specs/2-expand-schedule-x/`  
**Prerequisites**: spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4, US5)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `backend/tax-engine-service/`, `backend/extraction-service/`, `backend/pdf-service/`
- **Frontend**: `src/` (React SPA at repository root)
- **Tests**: `backend/[service]/src/test/`, `src/__tests__/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and configuration for 27-field Schedule X expansion

- [ ] T001 Update BusinessScheduleXDetails.java to support both old 6-field and new 27-field format (runtime conversion from data-model.md)
- [ ] T002 [P] Create scheduleXConstants.ts with field definitions, help text, and validation rules (27 fields × metadata)
- [ ] T003 [P] Update TypeScript interfaces in src/types/scheduleX.ts (AddBacks, Deductions, CalculatedFields, Metadata)
- [ ] T004 [P] Configure Gemini Vision API bounding box extraction in extraction-service application.properties

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Backend Foundation

- [ ] T005 Implement BusinessScheduleXDetails backward compatibility conversion in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/BusinessScheduleXService.java (Research R2 runtime conversion)
- [ ] T006 [P] Create ScheduleXCalculationService.java in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/ (FR-028, FR-029, FR-030 calculations)
- [ ] T007 [P] Create ScheduleXValidationService.java in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/ (FR-033 federal income match, FR-034 variance flag)
- [ ] T008 Update BusinessTaxCalculator.java to use expanded BusinessScheduleXDetails in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/BusinessTaxCalculator.java

### Backend Auto-Calculation Helpers

- [ ] T009 Create ScheduleXAutoCalculationService.java in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/ (Research R3 hybrid approach for complex calculations)
- [ ] T010 [P] Implement charitable contribution 10% limit calculation with carryforward in ScheduleXAutoCalculationService.java (FR-016, requires DB query)
- [ ] T011 [P] Implement officer compensation reasonableness test in ScheduleXAutoCalculationService.java (FR-055 from US3)

### Frontend Foundation

- [ ] T012 Create scheduleXCalculations.ts utility in src/utils/ (Research R3 frontend simple calculations: meals, 5% Rule, related-party)
- [ ] T013 [P] Create scheduleXFormatting.ts utility in src/utils/ (currency formatting, percentages for display)
- [ ] T014 [P] Create CollapsibleAccordion.tsx reusable component in src/components/shared/
- [ ] T015 Create ScheduleXAccordion.tsx in src/components/business/ (collapsible Add-Backs vs Deductions sections - FR-031)

### Frontend Field Components

- [ ] T016 [P] Create ScheduleXFieldInput.tsx in src/components/business/ (reusable input with help icon, auto-calc button, confidence score display)
- [ ] T017 [P] Create ScheduleXHelpTooltip.tsx in src/components/business/ (help tooltip explaining each adjustment - FR-031)
- [ ] T018 [P] Create ScheduleXConfidenceScore.tsx in src/components/business/ (displays AI confidence score, clickable to view PDF bounding box - Research R1)
- [ ] T019 [P] Create ScheduleXAutoCalcButton.tsx in src/components/business/ (auto-calculation button for 5% Rule, meals, etc. - FR-031)

### API Endpoints Foundation

- [ ] T020 Update NetProfitsController.java to add Schedule X endpoints in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/ (GET, PUT /api/net-profits/{returnId}/schedule-x)
- [ ] T021 [P] Create ScheduleXController.java in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/ (POST /auto-calculate, GET /multi-year-comparison, POST /import-from-federal)
- [ ] T022 [P] Create DTOs: BusinessScheduleXDetailsDto, ScheduleXAutoCalcRequest, MultiYearComparisonDto, ScheduleXAutoCalcResponse in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/

### AI Extraction Foundation

- [ ] T023 Update GeminiExtractionService.java to extract 27 Schedule X fields in backend/extraction-service/src/main/java/com/munitax/extraction/service/ (FR-039, FR-040, FR-041)
- [ ] T024 [P] Update ExtractionPromptBuilder.java to add prompts for 27 Schedule X fields with bounding box requirements in backend/extraction-service/src/main/java/com/munitax/extraction/service/
- [ ] T025 [P] Create ScheduleXExtractionResult.java model with confidence scores and bounding boxes in backend/extraction-service/src/main/java/com/munitax/extraction/model/ (Research R1)
- [ ] T026 [P] Create ExtractionBoundingBox.java model in backend/extraction-service/src/main/java/com/munitax/extraction/model/ (4 vertices, page number)

### PDF Generation Foundation

- [ ] T027 Update Form27Generator.java to expand Schedule X section for 27 fields in backend/pdf-service/src/main/java/com/munitax/pdf/service/ (Research R5 multi-page layout)
- [ ] T028 [P] Update form-27-template.html to add Schedule X detail pages (Page 2-3) in backend/pdf-service/src/main/resources/templates/

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - CPA Performs Complete M-1 Reconciliation for C-Corporation (Priority: P1) 🎯 MVP

**Goal**: Enable CPAs to reconcile federal taxable income with municipal taxable income for C-Corps, including depreciation (MACRS vs Book), meals & entertainment (50% rule), and state tax add-backs.

**Independent Test**: Enter Federal Form 1120 Line 30 of $500,000, add back $50,000 depreciation difference, $15,000 meals (50% of $30,000), $10,000 state taxes. System calculates adjusted municipal taxable income of $575,000.

**Acceptance Criteria**: FR-001 (depreciation), FR-003 (state taxes), FR-005 (meals), FR-028/FR-029/FR-030 (calculations)

### Backend Implementation for User Story 1

- [ ] T029 [P] [US1] Add depreciationAdjustment field handling to BusinessScheduleXDetails.AddBacks in backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/BusinessScheduleXDetails.java (FR-001)
- [ ] T030 [P] [US1] Add mealsAndEntertainment field handling to BusinessScheduleXDetails.AddBacks (FR-005)
- [ ] T031 [P] [US1] Add incomeAndStateTaxes field handling to BusinessScheduleXDetails.AddBacks (FR-003 - already exists, verify migration)
- [ ] T032 [US1] Implement total add-backs calculation (sum of 20 fields) in ScheduleXCalculationService.java (FR-028)
- [ ] T033 [US1] Implement total deductions calculation (sum of 7 fields) in ScheduleXCalculationService.java (FR-029)
- [ ] T034 [US1] Implement adjusted municipal income calculation (federal + addBacks - deductions) in ScheduleXCalculationService.java (FR-030)
- [ ] T035 [US1] Add variance validation (>20% flag) in ScheduleXValidationService.java (FR-034)

### Frontend Implementation for User Story 1

- [ ] T036 [P] [US1] Add Depreciation Adjustment field to ScheduleXAccordion.tsx Add-Backs section in src/components/business/
- [ ] T037 [P] [US1] Add Meals & Entertainment field with auto-calc button (50%→100%) to ScheduleXAccordion.tsx
- [ ] T038 [P] [US1] Add Income & State Taxes field to ScheduleXAccordion.tsx (migrate from old 6-field format)
- [ ] T039 [US1] Implement meals auto-calculation (federalMeals × 2) in src/utils/scheduleXCalculations.ts (Research R3 frontend calculation)
- [ ] T040 [US1] Add calculated totals display (Total Add-Backs, Total Deductions, Adjusted Municipal Income) to ScheduleXAccordion.tsx
- [ ] T041 [US1] Create ScheduleXValidationWarning.tsx component for >20% variance alert in src/components/business/ (FR-034)
- [ ] T042 [US1] Integrate ScheduleXAccordion into NetProfitsWizard.tsx as Step 3 in src/components/business/

### Frontend Services for User Story 1

- [ ] T043 [P] [US1] Create scheduleXService.ts API client in src/services/ (GET/PUT /api/net-profits/{returnId}/schedule-x)
- [ ] T044 [P] [US1] Create autoCalculationService.ts API client in src/services/ (POST /api/schedule-x/auto-calculate)
- [ ] T045 [P] [US1] Create useScheduleX.ts React hook (React Query) in src/hooks/
- [ ] T046 [P] [US1] Create useScheduleXAutoCalc.ts React hook in src/hooks/

### Unit Tests for User Story 1

- [ ] T047 [P] [US1] Unit test for ScheduleXCalculationService.calculateTotalAddBacks() in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/ScheduleXCalculationServiceTest.java
- [ ] T048 [P] [US1] Unit test for ScheduleXCalculationService.calculateAdjustedMunicipalIncome() (test case: $500K + $75K - $0 = $575K)
- [ ] T049 [P] [US1] Unit test for meals auto-calculation (federalMeals $15K → municipal add-back $30K) in ScheduleXAutoCalculationServiceTest.java
- [ ] T050 [P] [US1] Frontend unit test for calculateMealsAddBack() in src/__tests__/utils/scheduleXCalculations.test.ts
- [ ] T051 [P] [US1] Component test for ScheduleXAccordion rendering add-backs section in src/__tests__/components/ScheduleXAccordion.test.tsx

### Integration Test for User Story 1

- [ ] T052 [US1] Integration test: C-Corp with depreciation $50K, meals $15K, state taxes $10K → Adjusted income $575K in backend/tax-engine-service/src/test/java/com/munitax/taxengine/integration/ScheduleXIntegrationTest.java (spec.md User Story 1 scenarios 1-4)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently. CPAs can enter depreciation, meals, and state tax adjustments and see calculated adjusted municipal income.

---

## Phase 4: User Story 2 - Partnership Files Form 27 with Guaranteed Payments and Intangible Income (Priority: P1)

**Goal**: Enable partnerships to add back guaranteed payments (deductible federally, not municipally) and deduct intangible income (interest, dividends) with 5% Rule expense add-back.

**Independent Test**: Enter Form 1065 Line 22 ordinary income of $300,000, add back $50,000 guaranteed payments, deduct $35,000 intangible income ($20K interest + $15K dividends), apply 5% Rule ($1,750 add-back). Final municipal income = $316,750.

**Acceptance Criteria**: FR-004 (guaranteed payments), FR-012 (5% Rule), FR-021/FR-022 (intangible income deductions)

### Backend Implementation for User Story 2

- [ ] T053 [P] [US2] Add guaranteedPayments field handling to BusinessScheduleXDetails.AddBacks in backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/BusinessScheduleXDetails.java (FR-004)
- [ ] T054 [P] [US2] Add expensesOnIntangibleIncome field handling to BusinessScheduleXDetails.AddBacks (FR-012 - 5% Rule)
- [ ] T055 [P] [US2] Add interestIncome field handling to BusinessScheduleXDetails.Deductions (FR-021 - already exists, verify migration)
- [ ] T056 [P] [US2] Add dividends field handling to BusinessScheduleXDetails.Deductions (FR-022 - already exists, verify migration)
- [ ] T057 [US2] Implement 5% Rule calculation with manual override in ScheduleXAutoCalculationService.java (FR-012 - auto-calc or manual)

### Frontend Implementation for User Story 2

- [ ] T058 [P] [US2] Add Guaranteed Payments field to ScheduleXAccordion.tsx Add-Backs section (conditionally rendered for partnerships only)
- [ ] T059 [P] [US2] Add Expenses on Intangible Income field with auto-calc button (5% Rule) to ScheduleXAccordion.tsx Add-Backs section
- [ ] T060 [P] [US2] Add Interest Income field to ScheduleXAccordion.tsx Deductions section
- [ ] T061 [P] [US2] Add Dividend Income field to ScheduleXAccordion.tsx Deductions section
- [ ] T062 [US2] Implement 5% Rule auto-calculation ((interest + dividends + capitalGains) × 0.05) in src/utils/scheduleXCalculations.ts (Research R3 frontend)
- [ ] T063 [US2] Add manual override capability for 5% Rule in ScheduleXFieldInput.tsx (allow user to enter actual expense if > 5%)

### Unit Tests for User Story 2

- [ ] T064 [P] [US2] Unit test for 5% Rule: $35K intangible income → $1,750 add-back (5%) in ScheduleXAutoCalculationServiceTest.java
- [ ] T065 [P] [US2] Unit test for 5% Rule manual override: user enters $2,500 actual expense instead of $1,750 in ScheduleXAutoCalculationServiceTest.java
- [ ] T066 [P] [US2] Frontend unit test for calculate5PercentRule() in src/__tests__/utils/scheduleXCalculations.test.ts
- [ ] T067 [P] [US2] Component test for guaranteed payments field (conditional rendering for partnerships) in src/__tests__/components/ScheduleXAccordion.test.tsx

### Integration Test for User Story 2

- [ ] T068 [US2] Integration test: Partnership with guaranteed payments $50K, intangible income $35K, 5% Rule $1,750 → Adjusted income $316,750 in backend/tax-engine-service/src/test/java/com/munitax/taxengine/integration/ScheduleXIntegrationTest.java (spec.md User Story 2 scenarios 1-4)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently. Partnerships can file Form 27 with guaranteed payments and intangible income adjustments.

---

## Phase 5: User Story 3 - S-Corporation with Related-Party Transactions and Officer Compensation (Priority: P2)

**Goal**: Enable S-Corps to add back excess related-party payments (above FMV) and display informational notes for officer compensation reasonableness.

**Independent Test**: Enter S-Corp Form 1120-S taxable income of $400,000, add back $2,500 excess related-party rent ($10,000 paid - $7,500 FMV). System calculates adjusted income of $402,500 and shows officer compensation reasonableness warning if >50% of net income.

**Acceptance Criteria**: FR-006 (related-party excess), FR-055 (officer compensation info note from spec.md User Story 3)

### Backend Implementation for User Story 3

- [ ] T069 [P] [US3] Add relatedPartyExcess field handling to BusinessScheduleXDetails.AddBacks in backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/BusinessScheduleXDetails.java (FR-006)
- [ ] T070 [US3] Implement related-party excess calculation (paid - FMV) in ScheduleXAutoCalculationService.java (simple subtraction, no DB query needed)
- [ ] T071 [US3] Implement officer compensation reasonableness test (compare to IRS guidelines) in ScheduleXValidationService.java (warning if >50% of net income)

### Frontend Implementation for User Story 3

- [ ] T072 [P] [US3] Add Related-Party Excess Expenses field to ScheduleXAccordion.tsx Add-Backs section with inputs for "Paid Amount" and "Fair Market Value"
- [ ] T073 [US3] Implement related-party excess calculation (paid - FMV) in src/utils/scheduleXCalculations.ts (Research R3 frontend simple calculation)
- [ ] T074 [US3] Add officer compensation informational note to ScheduleXAccordion.tsx (display warning if >50% of net income - spec.md US3 scenario 3)
- [ ] T075 [US3] Create help tooltip for related-party transactions explaining FMV requirement in ScheduleXHelpTooltip.tsx

### Unit Tests for User Story 3

- [ ] T076 [P] [US3] Unit test for related-party excess: paid $10K, FMV $7.5K → add-back $2.5K in ScheduleXAutoCalculationServiceTest.java
- [ ] T077 [P] [US3] Unit test for officer compensation reasonableness: $150K compensation, $400K income → warning (37.5% - no warning) vs $200K/$400K → warning (50%) in ScheduleXValidationServiceTest.java
- [ ] T078 [P] [US3] Frontend unit test for calculateRelatedPartyExcess() in src/__tests__/utils/scheduleXCalculations.test.ts

### Integration Test for User Story 3

- [ ] T079 [US3] Integration test: S-Corp with related-party rent $2,500 excess → Adjusted income $402,500 in backend/tax-engine-service/src/test/java/com/munitax/taxengine/integration/ScheduleXIntegrationTest.java (spec.md User Story 3 scenarios 1-2)

**Checkpoint**: User Stories 1, 2, AND 3 should all work independently. S-Corps can file with related-party adjustments and see officer compensation guidance.

---

## Phase 6: User Story 4 - Corporation with Charitable Contributions Exceeding 10% Limit (Priority: P2)

**Goal**: Enable C-Corps to handle charitable contribution 10% limit with carryforward tracking. Municipal follows federal treatment.

**Independent Test**: Enter federal taxable income of $600,000 and charitable contributions of $80,000. System calculates current year deduction of $60,000 (10% limit) and carryforward of $20,000 to next year. No municipal add-back if federal correctly applied 10% limit.

**Acceptance Criteria**: FR-016 (charitable contribution excess), spec.md User Story 4 scenarios 1-3

### Backend Implementation for User Story 4

- [ ] T080 [P] [US4] Add charitableContributionExcess field handling to BusinessScheduleXDetails.AddBacks in backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/BusinessScheduleXDetails.java (FR-016)
- [ ] T081 [US4] Implement charitable contribution 10% limit calculation with prior year carryforward query in ScheduleXAutoCalculationService.calculateCharitableContribution() (Research R3 backend complex calculation - requires DB query)
- [ ] T082 [US4] Add charitable contribution carryforward field to BusinessScheduleXDetails for tracking multi-year carryforward

### Frontend Implementation for User Story 4

- [ ] T083 [P] [US4] Add Charitable Contribution Excess field to ScheduleXAccordion.tsx Add-Backs section
- [ ] T084 [US4] Add charitable contribution auto-calc button (10% limit calculation) to ScheduleXFieldInput for charitableContributionExcess
- [ ] T085 [US4] Display carryforward amount and explanation in ScheduleXAutoCalcButton response tooltip (e.g., "Deduct $60K this year, carry forward $20K to 2025")

### Unit Tests for User Story 4

- [ ] T086 [P] [US4] Unit test for charitable 10% limit: contributions $80K, income $600K, no prior carryforward → deduction $60K, carryforward $20K in ScheduleXAutoCalculationServiceTest.java
- [ ] T087 [P] [US4] Unit test for charitable with prior year carryforward: contributions $50K, income $700K (10% = $70K), prior carryforward $15K → deduction $65K, carryforward $0 in ScheduleXAutoCalculationServiceTest.java
- [ ] T088 [P] [US4] Unit test for charitable federal error: federal deducted $80K (ignored 10% limit) → municipal add-back $20K in ScheduleXAutoCalculationServiceTest.java

### Integration Test for User Story 4

- [ ] T089 [US4] Integration test: C-Corp with charitable contributions $80K, 10% limit $60K → carryforward $20K, no municipal add-back (follows federal) in backend/tax-engine-service/src/test/java/com/munitax/taxengine/integration/ScheduleXIntegrationTest.java (spec.md User Story 4 scenarios 1-3)

**Checkpoint**: User Stories 1-4 should all work independently. C-Corps can handle charitable contributions with 10% limit and carryforward tracking.

---

## Phase 7: User Story 5 - Service Business with Domestic Production Activities Deduction (Priority: P3)

**Goal**: Enable manufacturing/service businesses to add back DPAD (Section 199 deduction) not allowed municipally. Affects pre-TCJA returns and certain JEDD zones.

**Independent Test**: Enter federal Form 1120 showing DPAD of $25,000 (Line 25 on older returns). System adds back full $25,000 with label "Domestic Production Activities Deduction not allowed for municipal purposes."

**Acceptance Criteria**: FR-017 (DPAD), spec.md User Story 5 scenarios 1-3

### Backend Implementation for User Story 5

- [ ] T090 [P] [US5] Add domesticProductionActivities field handling to BusinessScheduleXDetails.AddBacks in backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/BusinessScheduleXDetails.java (FR-017)
- [ ] T091 [US5] Add DPAD field validation: check if business is in JEDD zone (may have different rules) in ScheduleXValidationService.java

### Frontend Implementation for User Story 5

- [ ] T092 [P] [US5] Add Domestic Production Activities Deduction field to ScheduleXAccordion.tsx Add-Backs section
- [ ] T093 [US5] Add help tooltip for DPAD explaining pre-TCJA context and JEDD zone exceptions in ScheduleXHelpTooltip.tsx

### Unit Tests for User Story 5

- [ ] T094 [P] [US5] Unit test for DPAD add-back: federal claimed $25K DPAD → municipal add-back $25K in ScheduleXCalculationServiceTest.java
- [ ] T095 [P] [US5] Unit test for DPAD JEDD zone exception: business in JEDD zone → no add-back (follow JEDD rules) in ScheduleXValidationServiceTest.java

### Integration Test for User Story 5

- [ ] T096 [US5] Integration test: Manufacturing with DPAD $25K → Adjusted income increases by $25K in backend/tax-engine-service/src/test/java/com/munitax/taxengine/integration/ScheduleXIntegrationTest.java (spec.md User Story 5 scenarios 1-2)

**Checkpoint**: All 5 user stories should now be independently functional. All entity types (C-Corp, Partnership, S-Corp) can file with comprehensive Schedule X adjustments.

---

## Phase 8: AI Extraction - Import Schedule X from Federal Return (FR-032, FR-039-FR-043)

**Goal**: Enable AI-assisted extraction of 27 Schedule X fields from uploaded Form 1120 Schedule M-1 and Form 4562 PDFs.

**Acceptance Criteria**: FR-032 (import button), FR-039 (AI extraction), FR-040 (field identification), FR-041 (depreciation schedule parsing), FR-042 (confidence scores), FR-043 (human override)

### Backend AI Extraction Implementation

- [ ] T097 [P] Implement Form 1120 Schedule M-1 parsing in GeminiExtractionService.extractScheduleM1Fields() in backend/extraction-service/src/main/java/com/munitax/extraction/service/
- [ ] T098 [P] Implement Form 4562 (depreciation schedule) parsing in GeminiExtractionService.extractForm4562Fields() (FR-041)
- [ ] T099 [P] Implement Form 1065 K-1 parsing for partnerships in GeminiExtractionService.extractForm1065Fields() (guaranteed payments extraction)
- [ ] T100 Add bounding box coordinates extraction to ExtractionResult for all 27 fields (Research R1 - Constitution IV compliance)
- [ ] T101 Create extraction endpoint POST /api/extraction/schedule-x in backend/extraction-service/src/main/java/com/munitax/extraction/controller/ExtractionController.java
- [ ] T102 Implement extraction status polling endpoint GET /api/extraction/schedule-x/{extractionId} (async processing support)

### Frontend AI Extraction Implementation

- [ ] T103 [P] Create ScheduleXImportButton.tsx component in src/components/business/ (triggers PDF upload and extraction - FR-032)
- [ ] T104 Create extractionService.ts API client in src/services/ (POST /import-from-federal, GET /extraction/schedule-x/{id})
- [ ] T105 Implement extraction progress modal showing "Processing Form 1120..." with estimated completion time
- [ ] T106 Display extraction results with confidence scores per field (green badge ≥0.9, yellow 0.7-0.89, red <0.7)
- [ ] T107 Implement clickable confidence badges that open PDF viewer with highlighted bounding box regions (Research R1)
- [ ] T108 Add manual override capability: user can click extracted field and modify value, preserving original AI value in history (FR-043)

### AI Extraction Tests

- [ ] T109 [P] Create 10 sample PDFs for testing in backend/extraction-service/src/test/resources/test-pdfs/ (5 C-Corp Form 1120, 3 Partnership Form 1065, 2 S-Corp Form 1120-S)
- [ ] T110 [P] Unit test for Form 1120 Schedule M-1 extraction (depreciation, meals, state taxes fields) in backend/extraction-service/src/test/java/com/munitax/extraction/service/GeminiExtractionServiceTest.java
- [ ] T111 [P] Unit test for Form 4562 extraction (MACRS depreciation, book depreciation calculation) in GeminiExtractionServiceTest.java
- [ ] T112 [P] Integration test: Upload Form 1120 PDF → extract 27 fields → verify average confidence ≥0.85 (Success Criteria) in backend/extraction-service/src/test/java/com/munitax/extraction/integration/ScheduleXExtractionIntegrationTest.java
- [ ] T113 [P] E2E test: Upload PDF, view confidence scores, override 2 fields, save Schedule X in src/__tests__/e2e/schedule-x-extraction.spec.ts (Playwright)

**Checkpoint**: CPAs can upload Form 1120/1065 PDFs and have 27 Schedule X fields auto-populated with >90% accuracy (Success Criteria).

---

## Phase 9: Multi-Year Comparison View (FR-038)

**Goal**: Enable CPAs to view Schedule X data for 3+ years side-by-side to identify recurring adjustments (depreciation trends, meals patterns).

**Acceptance Criteria**: FR-038 (multi-year comparison), Research R4 (<2 second response time)

### Backend Multi-Year Implementation

- [ ] T114 Implement multi-year query with IN clause in NetProfitReturnRepository.findByBusinessIdAndTaxYearIn() in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/
- [ ] T115 Create multi-year comparison endpoint GET /api/schedule-x/multi-year-comparison?businessId={id}&years=2024,2023,2022 in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/ScheduleXController.java
- [ ] T116 Optimize multi-year query with GIN index on JSONB schedule_x_details field (Research R4 - query time <180ms avg)

### Frontend Multi-Year Implementation

- [ ] T117 Create ScheduleXMultiYearComparison.tsx component in src/components/business/ (collapsible accordion with summary + detail views)
- [ ] T118 Implement summary view: display Federal Income, Total Add-Backs, Total Deductions, Adjusted Income for 3 years (always visible)
- [ ] T119 Implement detail view: display all 27 fields × 3 years in collapsible accordion (collapsed by default)
- [ ] T120 Add year-over-year change highlighting (e.g., "Depreciation increased 15% from 2023")
- [ ] T121 Create useMultiYearComparison.ts React hook in src/hooks/

### Multi-Year Tests

- [ ] T122 [P] Performance test: Query 3 years × 27 fields for 5,000 businesses → verify <2 second response time (P95) in backend/tax-engine-service/src/test/java/com/munitax/taxengine/integration/MultiYearComparisonPerformanceTest.java
- [ ] T123 [P] Component test for ScheduleXMultiYearComparison.tsx (render 3-year table, highlight changed fields) in src/__tests__/components/ScheduleXMultiYearComparison.test.tsx

**Checkpoint**: CPAs can view 3-year Schedule X comparison in <2 seconds, helping identify recurring adjustments and year-over-year trends.

---

## Phase 10: Form 27 PDF Generation with Expanded Schedule X (FR-035, Research R5)

**Goal**: Generate Form 27 PDF with all 27 Schedule X fields displayed across 3 pages (Page 1 = summary, Pages 2-3 = detail).

**Acceptance Criteria**: FR-035 (PDF export), Research R5 (multi-page layout, 10pt font readable)

### Backend PDF Generation Implementation

- [ ] T124 Update Form27Generator.renderScheduleXDetail() to display 27 fields in multi-page layout in backend/pdf-service/src/main/java/com/munitax/pdf/service/Form27Generator.java
- [ ] T125 Implement Page 1 summary: Federal Income, Total Add-Backs, Total Deductions, Adjusted Municipal Income (bold 11pt font)
- [ ] T126 Implement Page 2 detail: Add-backs fields 1-13 (Depreciation through Expenses on Intangible Income) with 10pt font
- [ ] T127 Implement Page 3 detail: Add-backs fields 14-20 + Deductions fields 21-27 with totals (bold 11pt)
- [ ] T128 Update form-27-template.html to add Schedule X Page 2-3 HTML structure in backend/pdf-service/src/main/resources/templates/

### PDF Generation Tests

- [ ] T129 [P] Unit test: Generate PDF with all 27 fields populated → verify 3 pages, readable 10pt font in backend/pdf-service/src/test/java/com/munitax/pdf/service/Form27GeneratorTest.java
- [ ] T130 [P] Manual QA: Print PDF and verify readability, field alignment, totals match calculated values (CPA review)

**Checkpoint**: Form 27 PDFs generated with expanded Schedule X are readable (10pt font), professional, and compliant with Dublin Form 27 instructions.

---

## Phase 11: Additional Fields for Comprehensive M-1 Coverage (FR-002, FR-007-FR-011, FR-013-FR-015, FR-018-FR-019, FR-024-FR-026)

**Goal**: Complete implementation of remaining 12 Schedule X fields not covered in User Stories 1-5.

**Acceptance Criteria**: All 27 fields implemented with validation, help text, and auto-calculation where applicable.

### Backend Implementation for Remaining Fields

- [ ] T131 [P] Add amortizationAdjustment field to BusinessScheduleXDetails.AddBacks (FR-002 - intangibles: goodwill, patents)
- [ ] T132 [P] Add penaltiesAndFines field (FR-007)
- [ ] T133 [P] Add politicalContributions field (FR-008)
- [ ] T134 [P] Add officerLifeInsurance field (FR-009)
- [ ] T135 [P] Add capitalLossExcess field (FR-010 - rename from losses1231)
- [ ] T136 [P] Add federalTaxRefunds field (FR-011)
- [ ] T137 [P] Add section179Excess field (FR-013)
- [ ] T138 [P] Add bonusDepreciation field (FR-014)
- [ ] T139 [P] Add badDebtReserveIncrease field (FR-015)
- [ ] T140 [P] Add stockCompensationAdjustment field (FR-018)
- [ ] T141 [P] Add inventoryMethodChange field (FR-019 - Section 481(a))
- [ ] T142 [P] Add section179Recapture field to BusinessScheduleXDetails.Deductions (FR-024)
- [ ] T143 [P] Add municipalBondInterest field to Deductions (FR-025)
- [ ] T144 [P] Add depletionDifference field to Deductions (FR-026)

### Frontend Implementation for Remaining Fields

- [ ] T145 [P] Add all 12 remaining fields to ScheduleXAccordion.tsx with appropriate sections (Add-Backs vs Deductions)
- [ ] T146 [P] Create help tooltips for all 12 fields explaining when they apply and how to calculate (use scheduleXConstants.ts)
- [ ] T147 Add field-level validation: otherAddBacksDescription required if otherAddBacks > 0, same for otherDeductions

### Tests for Remaining Fields

- [ ] T148 [P] Unit tests for remaining 12 fields: verify calculation, validation, display in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/ScheduleXCalculationServiceTest.java
- [ ] T149 [P] Component tests for remaining fields in ScheduleXAccordion.tsx in src/__tests__/components/ScheduleXAccordion.test.tsx

**Checkpoint**: All 27 Schedule X fields implemented and tested. Comprehensive M-1 reconciliation capability complete.

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements, documentation, and validation

- [ ] T150 [P] Update quickstart.md with 8 API examples (upload PDF, auto-calc meals, multi-year comparison) in /specs/2-expand-schedule-x/quickstart.md
- [ ] T151 [P] Code cleanup: Remove old 6-field Schedule X code paths (after verifying all returns migrated to new format)
- [ ] T152 Add backward compatibility audit log: track when old format returns are converted to new format
- [ ] T153 Performance optimization: Add Redis caching for multi-year comparison (optional future enhancement - Research R4)
- [ ] T154 [P] Security review: Verify Schedule X data encrypted at rest, no sensitive data in logs
- [ ] T155 [P] Add telemetry: Track AI extraction accuracy per field (monitor confidence scores over time)
- [ ] T156 [P] Update API documentation: Add OpenAPI 3.0 spec examples to contracts/api-schedule-x.yaml
- [ ] T157 Manual QA: CPA review of UI (27 fields), AI extraction accuracy (10 sample returns), PDF output quality
- [ ] T158 Run quickstart.md validation: Execute all 8 API examples, verify correct responses
- [ ] T159 Load test: 100 concurrent requests to multi-year comparison endpoint → verify <2 second P95 (Success Criteria)
- [ ] T160 Constitution Check: Re-verify bounding box coordinates (Constitution IV), audit trail immutability (Constitution III), multi-tenant isolation (Constitution II)

**Final Checkpoint**: All 5 user stories complete, all 27 fields implemented, AI extraction >90% accurate, multi-year comparison <2 seconds, Form 27 PDF readable. Feature ready for production deployment.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - **BLOCKS all user stories**
- **User Stories (Phases 3-7)**: All depend on Foundational phase completion
  - User stories can then proceed **in parallel** (if staffed)
  - Or sequentially in priority order: US1 (P1) → US2 (P1) → US3 (P2) → US4 (P2) → US5 (P3)
- **AI Extraction (Phase 8)**: Can start after Foundational, runs in parallel with user stories
- **Multi-Year Comparison (Phase 9)**: Can start after Foundational, runs in parallel with user stories
- **PDF Generation (Phase 10)**: Can start after Foundational, runs in parallel with user stories
- **Additional Fields (Phase 11)**: Can start after Foundational, runs in parallel with user stories
- **Polish (Phase 12)**: Depends on all desired user stories + AI extraction + multi-year + PDF being complete

### User Story Dependencies

- **User Story 1 (P1)**: No dependencies on other stories - can start after Foundational (Phase 2)
- **User Story 2 (P1)**: No dependencies on US1 - can run in parallel with US1
- **User Story 3 (P2)**: No dependencies on US1/US2 - can run in parallel
- **User Story 4 (P2)**: No dependencies on US1/US2/US3 - can run in parallel
- **User Story 5 (P3)**: No dependencies on US1-US4 - can run in parallel

**All user stories are independently testable** - no blocking dependencies between stories.

### Within Each User Story

- Models before services
- Services before controllers/endpoints
- Core implementation before integration
- Unit tests can run in parallel with implementation (TDD approach)
- Integration test runs after all story tasks complete

### Parallel Opportunities

**Within Foundational Phase (Phase 2)**:
```bash
# All marked [P] can run in parallel:
T006: ScheduleXCalculationService.java
T007: ScheduleXValidationService.java
T009: ScheduleXAutoCalculationService.java
T010: Charitable contribution calculation
T011: Officer compensation test
T012-T019: All frontend components
T022: DTOs
T024-T026: AI extraction models
T028: PDF template
```

**Across User Stories** (after Foundational complete):
```bash
# Team of 5 developers can work in parallel:
Developer A: User Story 1 (T029-T052)
Developer B: User Story 2 (T053-T068)
Developer C: User Story 3 (T069-T079)
Developer D: AI Extraction (T097-T113)
Developer E: Multi-Year Comparison (T114-T123)
```

**Within User Story 1**:
```bash
# All models can be created in parallel:
T029: depreciationAdjustment field
T030: mealsAndEntertainment field
T031: incomeAndStateTaxes field

# All frontend fields can be added in parallel:
T036: Depreciation field UI
T037: Meals field UI
T038: State taxes field UI

# All services can be created in parallel:
T043: scheduleXService.ts
T044: autoCalculationService.ts
T045: useScheduleX.ts
T046: useScheduleXAutoCalc.ts

# All unit tests can run in parallel:
T047-T051: All unit tests
```

---

## Parallel Example: Foundational Phase (Phase 2)

```bash
# Launch all backend services together:
Task T006: "Create ScheduleXCalculationService.java"
Task T007: "Create ScheduleXValidationService.java"
Task T009: "Create ScheduleXAutoCalculationService.java"

# Launch all frontend components together:
Task T014: "Create CollapsibleAccordion.tsx"
Task T016: "Create ScheduleXFieldInput.tsx"
Task T017: "Create ScheduleXHelpTooltip.tsx"
Task T018: "Create ScheduleXConfidenceScore.tsx"
Task T019: "Create ScheduleXAutoCalcButton.tsx"

# Launch all AI extraction models together:
Task T025: "Create ScheduleXExtractionResult.java"
Task T026: "Create ExtractionBoundingBox.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T028) - **CRITICAL** - blocks all stories
3. Complete Phase 3: User Story 1 (T029-T052)
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo MVP: C-Corps can file with depreciation, meals, state taxes adjustments

**Estimated Timeline**: 2-3 weeks for MVP (Setup + Foundational + US1)

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (Week 1-2)
2. Add User Story 1 → Test independently → Deploy/Demo (Week 3) 🎯 **MVP**
3. Add User Story 2 → Test independently → Deploy/Demo (Week 4)
4. Add User Story 3 → Test independently → Deploy/Demo (Week 5)
5. Add AI Extraction (Phase 8) → Deploy/Demo (Week 6)
6. Add Multi-Year Comparison (Phase 9) → Deploy/Demo (Week 7)
7. Each increment adds value without breaking previous functionality

### Parallel Team Strategy

With 5 developers after Foundational phase complete:

1. Team completes Setup + Foundational together (2 weeks)
2. Once Foundational is done, parallel work:
   - **Developer A**: User Story 1 (C-Corp depreciation, meals, state taxes)
   - **Developer B**: User Story 2 (Partnership guaranteed payments, intangible income)
   - **Developer C**: User Story 3 (S-Corp related-party, officer compensation)
   - **Developer D**: AI Extraction (Phase 8 - Gemini Vision API integration)
   - **Developer E**: Multi-Year Comparison (Phase 9 - performance optimization)
3. Stories complete and integrate independently (1-2 weeks per story)
4. Final integration + Polish (Week 7-8)

**Total Timeline**: 7-8 weeks with 5-developer team

---

## Success Metrics (from spec.md Success Criteria)

- [ ] **AI Extraction Accuracy**: 90% of AI-extracted Schedule X fields require zero manual correction for standard C-Corp returns (measure with 100 test PDFs)
- [ ] **Time Savings**: CPAs complete full Schedule X reconciliation in <10 minutes with AI-assisted extraction (vs 45+ minutes manual) - 78% time savings
- [ ] **Calculation Accuracy**: Adjusted Municipal Income calculation matches CPA's manual workpaper within $100 for 98% of returns (test with 50 returns)
- [ ] **Multi-Year Performance**: Multi-year comparison view loads in <2 seconds for businesses with 3+ prior year returns (P95 < 2 seconds, test with 100 businesses)
- [ ] **PDF Quality**: Form 27 PDF with 27 Schedule X fields is readable (10pt font minimum), professional, and compliant with Dublin Form 27 instructions

---

## Notes

- **[P] tasks** = different files, no dependencies - can run in parallel
- **[Story] label** maps task to specific user story (US1-US5) for traceability
- Each user story should be **independently completable and testable**
- Backward compatibility maintained: old 6-field format auto-converts to new 27-field format (Research R2)
- Constitution compliance: Bounding boxes (R1), Multi-tenant isolation, Audit trail immutability, AI transparency
- Tests are included because spec.md implies comprehensive testing for financial calculations
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- **Avoid**: vague tasks, same file conflicts, cross-story dependencies that break independence
