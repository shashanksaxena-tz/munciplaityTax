# Tasks: Enhanced Extraction with PDF Preview and Source Visibility

**Input**: Design documents from `/specs/015-enhanced-extraction-preview/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: Tests are included as the specification explicitly requests testing the UI and attaching screenshots.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and dependencies

- [ ] T001 Install PDF viewer library dependency (react-pdf or pdfjs-dist) in package.json
- [ ] T002 [P] Update types.ts with BoundingBox, FieldProvenance, and ExtendedFormFields types

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T003 Add BoundingBox parsing to backend/extraction-service/src/main/java/com/munitax/extraction/model/ExtractionDto.java
- [ ] T004 [P] Extend RealGeminiService.java prompt to request bounding box coordinates for extracted fields
- [ ] T005 [P] Create base PDF viewer service in services/pdfViewerService.ts
- [ ] T006 Update extractionMapper.ts to handle extended field provenances

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - View PDF with Extraction Source Highlighting (Priority: P1) 🎯 MVP

**Goal**: Users can view PDF alongside extracted data and click fields to see source locations highlighted

**Independent Test**: Upload a PDF, view extraction summary, click a field's "show source" button, verify PDF navigates to correct page with highlighting

### Tests for User Story 1

- [ ] T007 [P] [US1] Create Playwright e2e test for PDF viewer in e2e/pdf-viewer.spec.ts
- [ ] T008 [P] [US1] Create unit test for HighlightOverlay component in tests for coordinate calculations

### Implementation for User Story 1

- [ ] T009 [P] [US1] Create PdfViewer.tsx component in components/PdfViewer/PdfViewer.tsx
- [ ] T010 [P] [US1] Create HighlightOverlay.tsx component in components/PdfViewer/HighlightOverlay.tsx
- [ ] T011 [P] [US1] Create FieldSourceTooltip.tsx component in components/PdfViewer/FieldSourceTooltip.tsx
- [ ] T012 [US1] Create SplitViewLayout.tsx component in components/ExtractionReview/SplitViewLayout.tsx
- [ ] T013 [US1] Create FieldWithSource.tsx component in components/ExtractionReview/FieldWithSource.tsx
- [ ] T014 [US1] Modify ExtractionSummary.tsx to integrate split view with PDF preview
- [ ] T015 [US1] Add PDF page navigation and zoom controls
- [ ] T016 [US1] Implement field-to-page linking with bounding box highlight on click

**Checkpoint**: User Story 1 should be fully functional - users can view PDF and see extraction sources

---

## Phase 4: User Story 2 - Extended Field Extraction for All Form Types (Priority: P1)

**Goal**: Extract all relevant fields from W-2, 1040, Schedule E, 1099s, and business forms

**Independent Test**: Upload different form types and verify all expected fields are extracted with confidence scores

### Tests for User Story 2

- [ ] T017 [P] [US2] Create backend test for extended W-2 field extraction in backend/extraction-service/src/test/java/com/munitax/extraction/service/W2ExtractionTest.java
- [ ] T018 [P] [US2] Create backend test for extended 1040 field extraction

### Implementation for User Story 2

- [ ] T019 [P] [US2] Create ExtendedFormFields.java to define comprehensive field sets per form type in backend/extraction-service/
- [ ] T020 [US2] Update buildProductionExtractionPrompt() in RealGeminiService.java to request all W-2 boxes (1-20)
- [ ] T021 [US2] Update extraction prompt for Federal 1040 comprehensive fields (filing status, all income lines, deductions, tax)
- [ ] T022 [US2] Update extraction prompt for Schedule E property-level and K-1 entity extraction
- [ ] T023 [US2] Update extraction prompt for 1099-NEC/MISC payer and income fields
- [ ] T024 [US2] Update extraction prompt for business forms (1120, 1065, Form 27) with Schedule X/Y data
- [ ] T025 [US2] Create ExtractionFieldMapper.java to map raw extraction results to typed form fields
- [ ] T026 [US2] Update extractionMapper.ts to handle all new field types

**Checkpoint**: User Story 2 complete - all form types extract comprehensive fields

---

## Phase 5: User Story 3 - Extraction Failure Transparency (Priority: P2)

**Goal**: Users see clear explanations when forms or pages cannot be extracted

**Independent Test**: Upload a document with poor quality pages or unsupported forms, verify specific failure reasons are displayed

### Tests for User Story 3

- [ ] T027 [P] [US3] Create unit test for ExtractionFailures component rendering different failure types

### Implementation for User Story 3

- [ ] T028 [US3] Create ExtractionFailures.tsx component in components/ExtractionReview/ExtractionFailures.tsx
- [ ] T029 [US3] Update ExtractionDto.java SkippedForm to include detailed reason categories and suggestions
- [ ] T030 [US3] Update RealGeminiService.java to populate skippedForms with specific reasons
- [ ] T031 [US3] Modify ExtractionSummary.tsx to display Skipped Pages section with actionable guidance
- [ ] T032 [US3] Add "could not extract" indicators for individual fields within extracted forms

**Checkpoint**: User Story 3 complete - users understand extraction failures with guidance

---

## Phase 6: User Story 4 - Submission with Supporting Documents (Priority: P2)

**Goal**: Submitted returns are permanently linked to source PDFs, auditors can trace all values

**Independent Test**: Submit a return, verify in auditor view that all PDFs are accessible and field sources visible

### Tests for User Story 4

- [ ] T033 [P] [US4] Create backend integration test for document-submission association
- [ ] T034 [P] [US4] Create e2e test for auditor document viewing in e2e/auditor-document-view.spec.ts

### Implementation for User Story 4

- [ ] T035 [P] [US4] Create database migration V003__document_association.sql for document-submission links
- [ ] T036 [P] [US4] Create SubmissionDocument.java model in backend/submission-service/
- [ ] T037 [P] [US4] Create FieldAuditTrail.java model for tracking original vs corrected values
- [ ] T038 [US4] Modify SubmissionService.java to attach documents on return submission
- [ ] T039 [US4] Modify SubmissionController.java to handle document association requests
- [ ] T040 [US4] Create SubmissionDocuments.tsx component for document attachment UI
- [ ] T041 [US4] Add document viewing capability to auditor review panel
- [ ] T042 [US4] Implement manual correction tracking (preserve original extracted value)

**Checkpoint**: User Story 4 complete - all submissions have documents attached and traceable

---

## Phase 7: User Story 5 - Manual Entry Fallback with Document Association (Priority: P3)

**Goal**: Manual entries can still be associated with supporting documents

**Independent Test**: Enter data manually, attach a document, verify association persists through submission

### Implementation for User Story 5

- [ ] T043 [P] [US5] Create ManualEntry.java model in backend/submission-service/
- [ ] T044 [US5] Add "attach document" prompt when saving manual entries
- [ ] T045 [US5] Update auditor view to show "Manually entered - supporting document attached" status
- [ ] T046 [US5] Add indicator for manual entries without documents

**Checkpoint**: User Story 5 complete - manual entries can have supporting documents

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final UI testing, screenshots, and documentation

- [ ] T047 Run full UI test suite and capture screenshots of all new features
- [ ] T048 [P] Create screenshots showing:
  - Split view with PDF and extracted data
  - Field source highlighting on PDF
  - Extraction failures display
  - Document attachment during submission
  - Auditor document viewing
- [ ] T049 [P] Update component documentation
- [ ] T050 Code cleanup and refactoring
- [ ] T051 Performance testing for PDF loading with large documents
- [ ] T052 Security review for document access controls

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - US1 (P1) and US2 (P1) are both MVP priority - can run in parallel
  - US3 (P2) depends on extraction infrastructure from US2
  - US4 (P2) can start after US1 (needs PDF viewing for auditor)
  - US5 (P3) depends on US4 (document association model)
- **Polish (Phase 8)**: Depends on all user stories being complete

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- T001 and T002 (Setup) can run in parallel
- T003, T004, T005, T006 (Foundational) - T004 and T005 can run in parallel
- US1 and US2 can start in parallel once foundational complete
- T009, T010, T011 (US1 components) can run in parallel
- T017, T018 (US2 tests) can run in parallel
- T019-T026 (US2 prompt updates) - some can run in parallel

---

## Implementation Strategy

### MVP First (User Stories 1 + 2)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (PDF Preview)
4. Complete Phase 4: User Story 2 (Extended Extraction)
5. **STOP and VALIDATE**: Test both stories - upload PDF, verify extraction, check source highlighting
6. Deploy/demo MVP

### Incremental Delivery

1. Setup + Foundational → Ready
2. US1 (PDF Preview) → Demo ability to see extraction sources
3. US2 (Extended Extraction) → Demo all form fields extracted
4. US3 (Failure Visibility) → Demo clear failure explanations
5. US4 (Submission Docs) → Demo auditor document access
6. US5 (Manual Fallback) → Demo complete workflow
7. Polish → Final screenshots and documentation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Take screenshots at each checkpoint for documentation
- Stop at any checkpoint to validate story independently
