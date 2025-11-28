# Tasks: Business Form Library

**Input**: Design documents from `/specs/8-business-form-library/`
**Prerequisites**: plan.md ✅, spec.md ✅

**Tests**: Tests are OPTIONAL and not included in this task list (not explicitly requested in specification)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

This is a web application with:
- **Backend**: `backend/pdf-service/` (Java 21 Spring Boot - PDF generation), `backend/tax-engine-service/` (form data)
- **Frontend**: `src/` (React TypeScript)
- **Database**: PostgreSQL migrations in `backend/pdf-service/src/main/resources/db/migration/`
- **Object Storage**: S3-compatible (MinIO) for PDF files and templates

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Review project structure in plan.md and verify existing pdf-service and tax-engine-service setup
- [ ] T002 Verify Apache PDFBox 3.0+ dependency in backend/pdf-service/pom.xml
- [ ] T003 [P] Verify Spring Boot dependencies in backend/pdf-service/pom.xml (Spring Data JPA, PostgreSQL driver, Spring Cloud Feign)
- [ ] T004 [P] Verify React dependencies in package.json (react-pdf, pdf-lib, Axios, React Query)
- [ ] T005 [P] Setup MinIO/S3 buckets for form templates and generated PDFs (s3://munitax-forms/templates/, s3://munitax-forms/generated/)
- [ ] T006 Configure Redis for template caching in backend/pdf-service/src/main/resources/application.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core database schema, domain models, and PDF infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Migrations

- [ ] T007 Create Flyway migration V1.30__create_form_templates_table.sql in backend/pdf-service/src/main/resources/db/migration/
- [ ] T008 Create Flyway migration V1.31__create_generated_forms_table.sql in backend/pdf-service/src/main/resources/db/migration/
- [ ] T009 Create Flyway migration V1.32__create_filing_packages_table.sql in backend/pdf-service/src/main/resources/db/migration/
- [ ] T010 Create Flyway migration V1.33__create_form_audit_log_table.sql in backend/pdf-service/src/main/resources/db/migration/
- [ ] T011 Create Flyway migration V1.34__insert_default_form_templates.sql in backend/pdf-service/src/main/resources/db/migration/ (seed Form 27-EXT, 27-ES templates)
- [ ] T012 Create Flyway migration V1.35__add_form_indexes.sql in backend/pdf-service/src/main/resources/db/migration/ (indexes on tenant_id, form_code, tax_year)

### Domain Models and Enums

- [ ] T013 [P] Create FormStatus enum in backend/pdf-service/src/main/java/com/munitax/pdf/domain/FormStatus.java (DRAFT, FINAL, SUBMITTED, AMENDED, SUPERSEDED)
- [ ] T014 [P] Create PackageType enum in backend/pdf-service/src/main/java/com/munitax/pdf/domain/PackageType.java (ORIGINAL, AMENDED, EXTENSION)
- [ ] T015 [P] Create FormTemplate entity in backend/pdf-service/src/main/java/com/munitax/pdf/domain/FormTemplate.java
- [ ] T016 [P] Create GeneratedForm entity in backend/pdf-service/src/main/java/com/munitax/pdf/domain/GeneratedForm.java
- [ ] T017 [P] Create FilingPackage entity in backend/pdf-service/src/main/java/com/munitax/pdf/domain/FilingPackage.java
- [ ] T018 [P] Create FormAuditLog entity in backend/pdf-service/src/main/java/com/munitax/pdf/domain/FormAuditLog.java

### Repositories

- [ ] T019 [P] Create FormTemplateRepository interface in backend/pdf-service/src/main/java/com/munitax/pdf/repository/FormTemplateRepository.java
- [ ] T020 [P] Create GeneratedFormRepository interface in backend/pdf-service/src/main/java/com/munitax/pdf/repository/GeneratedFormRepository.java
- [ ] T021 [P] Create FilingPackageRepository interface in backend/pdf-service/src/main/java/com/munitax/pdf/repository/FilingPackageRepository.java
- [ ] T022 [P] Create FormAuditLogRepository interface in backend/pdf-service/src/main/java/com/munitax/pdf/repository/FormAuditLogRepository.java

### DTOs

- [ ] T023 [P] Create FormGenerationRequest DTO in backend/pdf-service/src/main/java/com/munitax/pdf/dto/FormGenerationRequest.java
- [ ] T024 [P] Create FormGenerationResponse DTO in backend/pdf-service/src/main/java/com/munitax/pdf/dto/FormGenerationResponse.java
- [ ] T025 [P] Create FilingPackageRequest DTO in backend/pdf-service/src/main/java/com/munitax/pdf/dto/FilingPackageRequest.java
- [ ] T026 [P] Create FilingPackageResponse DTO in backend/pdf-service/src/main/java/com/munitax/pdf/dto/FilingPackageResponse.java
- [ ] T027 [P] Create FormTemplateDto DTO in backend/pdf-service/src/main/java/com/munitax/pdf/dto/FormTemplateDto.java

### Core PDF Infrastructure

- [ ] T028 Create PDFBoxHelper utility in backend/pdf-service/src/main/java/com/munitax/pdf/util/PDFBoxHelper.java (field filling, page manipulation)
- [ ] T029 Create FormWatermarkUtil utility in backend/pdf-service/src/main/java/com/munitax/pdf/util/FormWatermarkUtil.java (add/remove DRAFT watermark per FR-030)
- [ ] T030 Create PDFCompressionUtil utility in backend/pdf-service/src/main/java/com/munitax/pdf/util/PDFCompressionUtil.java (optimize file size per FR-033)
- [ ] T031 Create AbstractFormGenerator base class in backend/pdf-service/src/main/java/com/munitax/pdf/generator/AbstractFormGenerator.java (template pattern for all generators)
- [ ] T032 Create FormTemplateService in backend/pdf-service/src/main/java/com/munitax/pdf/service/FormTemplateService.java (template management, caching)
- [ ] T033 Create FieldMappingService in backend/pdf-service/src/main/java/com/munitax/pdf/service/FieldMappingService.java (map database fields → PDF form fields per FR-003)
- [ ] T034 Create FormValidationService in backend/pdf-service/src/main/java/com/munitax/pdf/service/FormValidationService.java (pre-generation validation per FR-034, FR-035)
- [ ] T035 Create FormVersioningService in backend/pdf-service/src/main/java/com/munitax/pdf/service/FormVersioningService.java (track versions, supersede old forms per FR-038, FR-039)

### Frontend Types

- [ ] T036 [P] Create formTypes.ts in src/types/formTypes.ts (FormTemplate, GeneratedForm, FilingPackage interfaces)
- [ ] T037 [P] Create formStatus.ts in src/types/formStatus.ts (FormStatus enum)

### Form Templates (Upload to S3)

- [ ] T038 [P] Upload Form-27-EXT-2024.pdf template to s3://munitax-forms/templates/2024/ (official government template - placeholder for actual template)
- [ ] T039 [P] Upload Form-27-ES-2024.pdf template to s3://munitax-forms/templates/2024/ (quarterly estimated tax voucher template)
- [ ] T040 [P] Upload Form-27-NOL-2024.pdf template to s3://munitax-forms/templates/2024/ (NOL schedule template)
- [ ] T041 [P] Upload Form-27-W1-2024.pdf template to s3://munitax-forms/templates/2024/ (withholding report template)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Generate Extension Request (Form 27-EXT) (Priority: P1) 🎯 MVP

**Goal**: Business can generate Form 27-EXT requesting 6-month extension with calculated estimated tax payment

**Independent Test**: Business estimates 2024 tax liability: $25,000. Form 27-EXT generated April 10, 2024 with estimated tax $25,000, new filing deadline October 15, 2024.

### Backend Implementation for User Story 1

- [ ] T042 [P] [US1] Create Form27ExtGenerator in backend/pdf-service/src/main/java/com/munitax/pdf/generator/Form27ExtGenerator.java (extends AbstractFormGenerator, implement Form 27-EXT generation per FR-006, FR-007)
- [ ] T043 [US1] Create ExtensionRequestService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/ExtensionRequestService.java (calculate recommended extension payment per FR-007, update filing deadline per FR-008)
- [ ] T044 [US1] Create FormGenerationService in backend/pdf-service/src/main/java/com/munitax/pdf/service/FormGenerationService.java (core PDF generation logic per FR-001, FR-003, FR-027)
- [ ] T045 [US1] Create FormGenerationController in backend/pdf-service/src/main/java/com/munitax/pdf/controller/FormGenerationController.java (REST API: POST /api/forms/generate)
- [ ] T046 [US1] Add GET /api/forms endpoint in FormGenerationController (list generated forms with filters)
- [ ] T047 [US1] Add GET /api/forms/{id} endpoint in FormGenerationController (get form metadata + download URL)
- [ ] T048 [US1] Add PUT /api/forms/{id}/regenerate endpoint in FormGenerationController (regenerate form with updated data)
- [ ] T049 [US1] Implement form validation in FormValidationService for Form 27-EXT (required fields: estimated tax, business name, FEIN, tax year)
- [ ] T050 [US1] Add audit logging in FormGenerationService (create FormAuditLog entry on every generation per Constitution III)
- [ ] T051 [US1] Add form generation to tax-engine-service controller: FormDataController with GET /api/form-data/27-EXT/{returnId} endpoint

### Frontend Implementation for User Story 1

- [ ] T052 [P] [US1] Create formGenerationService API client in src/services/formGenerationService.ts (POST /api/forms/generate, GET /api/forms, GET /api/forms/{id})
- [ ] T053 [P] [US1] Create useFormGeneration hook in src/hooks/useFormGeneration.ts (React Query hook for form generation mutations)
- [ ] T054 [P] [US1] Create useFormHistory hook in src/hooks/useFormHistory.ts (React Query hook for form history)
- [ ] T055 [US1] Create ExtensionRequestForm component in src/components/extensions/ExtensionRequestForm.tsx (input form for Form 27-EXT data per FR-006)
- [ ] T056 [US1] Create ExtensionCalculator component in src/components/extensions/ExtensionCalculator.tsx (calculate recommended extension payment per FR-007)
- [ ] T057 [US1] Create FormPreview component in src/components/forms/FormPreview.tsx (PDF preview before finalization using react-pdf)
- [ ] T058 [US1] Create FormHistoryTable component in src/components/forms/FormHistoryTable.tsx (list of generated forms with versions per FR-037)
- [ ] T059 [US1] Create GeneratedFormCard component in src/components/forms/GeneratedFormCard.tsx (display single form with status badge)
- [ ] T060 [US1] Create FormStatusBadge component in src/components/shared/FormStatusBadge.tsx (status indicator: DRAFT, FINAL, SUBMITTED per FR-037)

**Checkpoint**: At this point, User Story 1 should be fully functional - business can generate Form 27-EXT extension request with calculated payment

---

## Phase 4: User Story 2 - Generate Quarterly Estimated Tax Vouchers (Form 27-ES) (Priority: P1)

**Goal**: Business can generate four quarterly estimated tax vouchers (Form 27-ES) with calculated payment amounts and due dates

**Independent Test**: 2024 estimated tax: $20,000. Generate 4 vouchers: Q1 $5,000 due April 15, Q2 $5,000 due June 15, Q3 $5,000 due September 15, Q4 $5,000 due January 15, 2025.

### Backend Implementation for User Story 2

- [ ] T061 [P] [US2] Create Form27EsGenerator in backend/pdf-service/src/main/java/com/munitax/pdf/generator/Form27EsGenerator.java (generate 4 quarterly vouchers per FR-009, FR-010, FR-011)
- [ ] T062 [US2] Create EstimatedTaxService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/EstimatedTaxService.java (calculate quarterly payment amounts per FR-010)
- [ ] T063 [US2] Add GET /api/form-data/27-ES/{returnId} endpoint in FormDataController (backend/tax-engine-service) to provide estimated tax data for vouchers
- [ ] T064 [US2] Implement payment stub generation in Form27EsGenerator (detachable payment stub per FR-011)
- [ ] T065 [US2] Add voucher status tracking to GeneratedForm entity and GeneratedFormRepository (GENERATED, PAID, LATE, OVERPAID per FR-012)

### Frontend Implementation for User Story 2

- [ ] T066 [P] [US2] Create EstimatedTaxVouchersForm component in src/components/estimated/EstimatedTaxVouchersForm.tsx (input form for annual estimate, allow custom quarterly amounts)
- [ ] T067 [P] [US2] Create QuarterlyVoucherTable component in src/components/estimated/QuarterlyVoucherTable.tsx (display 4 quarterly vouchers with due dates per FR-009)
- [ ] T068 [US2] Add multi-form generation support to FormGenerationService (generate all 4 vouchers in single request)
- [ ] T069 [US2] Create VoucherStatusBadge component in src/components/estimated/VoucherStatusBadge.tsx (show payment status per FR-012)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - extension requests and estimated tax vouchers

---

## Phase 5: User Story 3 - Generate NOL Schedule (Form 27-NOL) (Priority: P2)

**Goal**: Business with NOL carryforwards can generate Form 27-NOL showing all NOL vintages, usage, and remaining balances

**Independent Test**: 2020 NOL: $200K original, $150K used prior, $50K used in 2024, $0 remaining. 2021 NOL: $100K original, $0 used prior, $80K used in 2024, $20K remaining. Form 27-NOL shows both NOLs with final $20K carryforward to 2025.

### Backend Implementation for User Story 3

- [ ] T070 [P] [US3] Create Form27NolGenerator in backend/pdf-service/src/main/java/com/munitax/pdf/generator/Form27NolGenerator.java (generate NOL schedule per FR-013, FR-014, FR-015)
- [ ] T071 [US3] Add GET /api/form-data/27-NOL/{returnId} endpoint in FormDataController (backend/tax-engine-service) to fetch NOL data from NOL tracker
- [ ] T072 [US3] Implement multi-page support in AbstractFormGenerator for forms with many vintages (continuation sheets per Edge Case 3)
- [ ] T073 [US3] Implement cross-form validation in FormValidationService (NOL deduction on Form 27-NOL must match Form 27 per FR-016)
- [ ] T074 [US3] Add NOL tracker integration in tax-engine-service: NolDataService with methods to aggregate NOL vintage data for forms

### Frontend Implementation for User Story 3

- [ ] T075 [P] [US3] Create NolSchedulePreview component in src/components/nol/NolSchedulePreview.tsx (display NOL vintage table before generation)
- [ ] T076 [US3] Create NolVintageTable component in src/components/nol/NolVintageTable.tsx (table showing NOL vintages: year, original, used, remaining per FR-013)
- [ ] T077 [US3] Add NOL validation display in FormPreview component (show warning if NOL deduction exceeds 80% limit per FR-015)

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work independently - extension, vouchers, and NOL schedule generation

---

## Phase 6: User Story 4 - Generate Withholding Report (Form 27-W1) (Priority: P2)

**Goal**: Business that withholds municipal tax can generate quarterly Form 27-W1 showing employees, wages, tax withheld, and YTD totals

**Independent Test**: Q1 2024 withholding: Employee A $50K wages, $1,250 tax; Employee B $40K wages, $1,000 tax. Total: $90K wages, $2,250 tax. Form 27-W1 due April 30, 2024.

### Backend Implementation for User Story 4

- [ ] T078 [P] [US4] Create Form27W1Generator in backend/pdf-service/src/main/java/com/munitax/pdf/generator/Form27W1Generator.java (generate quarterly withholding report per FR-017, FR-018, FR-019)
- [ ] T079 [US4] Add GET /api/form-data/27-W1/{returnId}/{quarter} endpoint in FormDataController (backend/tax-engine-service) to fetch W-1 filing data
- [ ] T080 [US4] Implement cumulative YTD calculation in Form27W1Generator (track Q1 → Q1, Q1+Q2 → Q2 YTD, etc. per FR-019)
- [ ] T081 [US4] Implement employee-by-employee detail table in Form27W1Generator (employee name, SSN masking XXX-XX-1234, wages, tax per FR-017)
- [ ] T082 [US4] Add annual W-1 reconciliation generation support (Q4/Year-end report per FR-020)

### Frontend Implementation for User Story 4

- [ ] T083 [P] [US4] Create WithholdingReportForm component in src/components/withholding/WithholdingReportForm.tsx (select quarter, generate W-1)
- [ ] T084 [US4] Create WithholdingEmployeeTable component in src/components/withholding/WithholdingEmployeeTable.tsx (display employee detail per FR-017)
- [ ] T085 [US4] Add YTD cumulative totals display to WithholdingReportForm (show cumulative wages and tax per FR-019)

**Checkpoint**: At this point, User Stories 1-4 should all work independently - all major form types generated

---

## Phase 7: User Story 5 - Generate Complete Filing Package (Priority: P3)

**Goal**: CPA can generate complete filing package with all required forms in single PDF, properly organized with bookmarks and page numbers

**Independent Test**: Filing includes Form 27 (3 pages), Form 27-Y (2 pages), Form 27-X (1 page), Form 27-NOL (1 page), W-1 reconciliation (1 page), Federal 1120 (15 pages). Total: 23 pages with table of contents.

### Backend Implementation for User Story 5

- [ ] T086 [US5] Create PDFAssemblyService in backend/pdf-service/src/main/java/com/munitax/pdf/service/PDFAssemblyService.java (combine forms into filing package per FR-031, FR-032)
- [ ] T087 [US5] Create FilingPackageController in backend/pdf-service/src/main/java/com/munitax/pdf/controller/FilingPackageController.java (REST API: POST /api/filing-packages, GET /api/filing-packages/{id})
- [ ] T088 [US5] Implement PDF merging in PDFAssemblyService (combine multiple PDFs using PDFBox per research R4)
- [ ] T089 [US5] Implement table of contents generation in PDFAssemblyService (create TOC page with form names and page numbers per FR-031)
- [ ] T090 [US5] Implement PDF bookmark creation in PDFAssemblyService (add outline structure using PDFBox PDDocumentOutline per FR-032)
- [ ] T091 [US5] Implement page numbering in PDFAssemblyService (add footer "Page X of Y" to every page per FR-031)
- [ ] T092 [US5] Implement file size optimization in PDFAssemblyService (compress images, target <10MB per FR-033)
- [ ] T093 [US5] Add filing package metadata to FilingPackageResponse DTO (total pages, included forms, file size)

### Frontend Implementation for User Story 5

- [ ] T094 [P] [US5] Create filingPackageService API client in src/services/filingPackageService.ts (POST /api/filing-packages, GET /api/filing-packages/{id})
- [ ] T095 [P] [US5] Create useFilingPackage hook in src/hooks/useFilingPackage.ts (React Query hook for filing package)
- [ ] T096 [US5] Create FormGenerationWizard component in src/components/forms/FormGenerationWizard.tsx (multi-step wizard: Select forms → Review → Generate)
- [ ] T097 [US5] Create FormSelector component in src/components/forms/FormSelector.tsx (checkboxes to select forms for package per FR-031)
- [ ] T098 [US5] Create FilingPackageBuilder component in src/components/forms/FilingPackageBuilder.tsx (filing package assembly UI per FR-031)
- [ ] T099 [US5] Create FormDataReview component in src/components/forms/FormDataReview.tsx (review auto-populated data before generation)
- [ ] T100 [US5] Add cover page generation support in PDFAssemblyService (cover page with business info, filing summary per FR-031)

**Checkpoint**: All user stories (US1-US5) should now be independently functional - complete filing package generation

---

## Phase 8: Cross-Form Validation & Integration

**Goal**: Implement comprehensive validation across multiple forms to ensure data consistency

- [ ] T101 Create FormValidationCoordinator in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/FormValidationCoordinator.java (cross-form validation orchestration)
- [ ] T102 Implement NOL cross-validation (Form 27-NOL deduction matches Form 27 NOL line per FR-016)
- [ ] T103 Implement apportionment cross-validation (Form 27-Y percentage matches Form 27 apportionment factor per FR-022)
- [ ] T104 Implement withholding cross-validation (Form 27-W1 annual total matches Form 27 withholding credit)
- [ ] T105 Add validation error display in frontend FormPreview component (show cross-form inconsistencies per FR-035)

---

## Phase 9: Form Template Management (Admin)

**Goal**: Provide admin interface for managing form templates (upload new templates, update field mappings)

- [ ] T106 [P] Create FormTemplateController in backend/pdf-service/src/main/java/com/munitax/pdf/controller/FormTemplateController.java (REST API: GET /api/form-templates, POST /api/form-templates, PUT /api/form-templates/{id})
- [ ] T107 [P] Create formTemplateService API client in src/services/formTemplateService.ts (GET /api/form-templates, POST /api/form-templates)
- [ ] T108 Create FormTemplateUpload component in src/components/admin/FormTemplateUpload.tsx (multipart file upload for new templates)
- [ ] T109 Create FormTemplateMappingEditor component in src/components/admin/FormTemplateMappingEditor.tsx (edit field mappings JSON per research R6)
- [ ] T110 Add template validation in FormTemplateService (verify PDF template has required AcroForm fields)

---

## Phase 10: Additional Form Types (Form 27-Y, 27-X)

**Goal**: Implement generators for apportionment and book-tax adjustment schedules

### Form 27-Y (Apportionment Schedule)

- [ ] T111 [P] Create Form27YGenerator in backend/pdf-service/src/main/java/com/munitax/pdf/generator/Form27YGenerator.java (apportionment schedule per FR-021, FR-022, FR-023)
- [ ] T112 Add GET /api/form-data/27-Y/{returnId} endpoint in FormDataController (fetch apportionment data from Schedule Y service)
- [ ] T113 [P] Create ApportionmentSchedulePreview component in src/components/apportionment/ApportionmentSchedulePreview.tsx (display property, payroll, sales factors)

### Form 27-X (Book-Tax Adjustments)

- [ ] T114 [P] Create Form27XGenerator in backend/pdf-service/src/main/java/com/munitax/pdf/generator/Form27XGenerator.java (Schedule X adjustments per FR-024, FR-025, FR-026)
- [ ] T115 Add GET /api/form-data/27-X/{returnId} endpoint in FormDataController (fetch Schedule X data from tax-engine-service)
- [ ] T116 [P] Create BookTaxAdjustmentsPreview component in src/components/adjustments/BookTaxAdjustmentsPreview.tsx (display add-backs and deductions)

---

## Phase 11: Electronic Submission Preparation

**Goal**: Prepare forms for electronic submission (PDF/A format, digital signatures)

- [ ] T117 Implement PDF/A conversion in FormGenerationService (archival format per FR-041)
- [ ] T118 Implement digital signature support in FormGenerationService (sign PDF with digital certificate per FR-040)
- [ ] T119 Create SubmissionManifestGenerator in backend/pdf-service/src/main/java/com/munitax/pdf/service/SubmissionManifestGenerator.java (create XML manifest per FR-040)
- [ ] T120 Add submission validation in FormValidationService (validate forms meet e-filing requirements per FR-041)
- [ ] T121 Create SubmissionConfirmation component in src/components/forms/SubmissionConfirmation.tsx (review checklist before submission per FR-042)

---

## Phase 12: Data Provenance & AI Transparency (Constitution IV)

**Goal**: Track and display where form fields were auto-populated from

- [ ] T122 Add provenance metadata to FormGenerationRequest DTO (track data source for each field)
- [ ] T123 Create DataProvenanceTooltip component in src/components/shared/DataProvenanceTooltip.tsx (show data source on hover)
- [ ] T124 Add provenance display to FormDataReview component (show "Auto-populated from [source]" for each field)
- [ ] T125 Add AI confidence scores to provenance display (for AI-extracted fields per Constitution IV)
- [ ] T126 Add manual override tracking in FormAuditLog (log when user changes auto-populated field)

---

## Phase 13: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

### Error Handling & Logging

- [ ] T127 [P] Add error handling to all controllers (ValidationException, BusinessException with proper HTTP status codes)
- [ ] T128 [P] Add logging to all services (MDC context with tenant_id, business_id, user_id)
- [ ] T129 [P] Add retry logic for S3 operations in FormTemplateService (handle transient failures)
- [ ] T130 [P] Add error boundaries to frontend pages (ErrorBoundary component)

### Performance Optimization

- [ ] T131 Add Redis caching to FormTemplateService (cache templates with 95%+ hit rate per plan.md)
- [ ] T132 Add database query indexes (verify V1.35__add_form_indexes.sql covers all query patterns)
- [ ] T133 Add connection pooling configuration for S3 client (optimize object storage access)
- [ ] T134 Add async form generation for filing packages >20 pages (use event-driven generation per plan.md)

### Security & Validation

- [ ] T135 Add rate limiting to form generation endpoints (max 10 generations per minute per business)
- [ ] T136 Add file size validation (max 10MB for single forms, 20MB for filing packages per FR-041)
- [ ] T137 Add PDF encryption for generated forms (S3 server-side encryption per Constitution V)
- [ ] T138 Add digital signature validation (verify certificate chain per FR-040)

### Documentation & Testing

- [ ] T139 [P] Add API documentation comments to all controllers (Swagger/OpenAPI annotations)
- [ ] T140 [P] Add JavaDoc to all public service methods
- [ ] T141 [P] Update README.md with form generation feature overview
- [ ] T142 [P] Create FORM_GENERATION_GUIDE.md in docs/ folder (user guide for generating forms)
- [ ] T143 Add accessibility attributes to all form components (aria-label, aria-describedby)
- [ ] T144 Add loading states to all frontend components (skeleton loaders during PDF generation)

### Quality Assurance

- [ ] T145 Run Checkstyle and fix violations in backend code
- [ ] T146 Run ESLint and fix violations in frontend code
- [ ] T147 Verify all form templates uploaded to S3 (check s3://munitax-forms/templates/2024/)
- [ ] T148 Manual QA: Visual comparison of generated PDFs vs official government samples (pixel-perfect match per plan.md)
- [ ] T149 Performance testing: Verify single form generation <2 seconds (FR-027, FR-028)
- [ ] T150 Performance testing: Verify filing package assembly <10 seconds (Success Criteria)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Cross-Form Validation (Phase 8)**: Depends on User Stories 3-4 completion (NOL, withholding forms exist)
- **Template Management (Phase 9)**: Depends on Foundational completion (can start in parallel with user stories)
- **Additional Forms (Phase 10)**: Depends on User Story 1 completion (Form 27-Y, 27-X follow same pattern)
- **E-Filing Prep (Phase 11)**: Depends on User Story 5 completion (filing packages exist)
- **Data Provenance (Phase 12)**: Depends on User Stories 1-4 completion (forms with auto-populated data exist)
- **Polish (Phase 13)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories (extension request standalone)
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Independent of US1 (estimated tax vouchers standalone)
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Independent of US1/US2 (NOL schedule standalone)
- **User Story 4 (P2)**: Can start after Foundational (Phase 2) - Independent of US1/US2/US3 (withholding report standalone)
- **User Story 5 (P3)**: Depends on User Stories 1-4 completion (filing package requires multiple form types to combine)

### Within Each User Story

- Backend generators before frontend components
- Repositories before services
- Services before controllers
- DTOs before controllers
- API clients before React hooks
- React hooks before components
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- **Setup**: All 6 setup tasks can run in parallel
- **Foundational - Migrations**: T007-T012 can run in parallel (6 migration files)
- **Foundational - Enums**: T013-T014 can run in parallel (2 enum files)
- **Foundational - Entities**: T015-T018 can run in parallel (4 entity files)
- **Foundational - Repositories**: T019-T022 can run in parallel (4 repository files)
- **Foundational - DTOs**: T023-T027 can run in parallel (5 DTO files)
- **Foundational - Frontend Types**: T036-T037 can run in parallel (2 type files)
- **Foundational - Form Templates**: T038-T041 can run in parallel (4 template uploads)
- **User Story 1 - Backend**: T042 (Form27ExtGenerator) and T043 (ExtensionRequestService) can run in parallel
- **User Story 1 - Frontend**: T052-T054 can run in parallel (API client + 2 hooks)
- **User Story 2 - Backend**: T061 (Form27EsGenerator) and T062 (EstimatedTaxService) can run in parallel
- **User Story 2 - Frontend**: T066-T067 can run in parallel (2 components)
- **User Story 3 - Backend**: T070 (Form27NolGenerator) and T071 (GET endpoint) can run in parallel
- **User Story 3 - Frontend**: T075-T076 can run in parallel (2 components)
- **User Story 4 - Backend**: T078 (Form27W1Generator) and T079 (GET endpoint) can run in parallel
- **User Story 4 - Frontend**: T083-T084 can run in parallel (2 components)
- **User Story 5 - Frontend**: T094-T095 can run in parallel (API client + hook)
- **Phase 10**: T111-T113 (Form 27-Y) and T114-T116 (Form 27-X) can run in parallel (independent forms)
- **Polish - Error Handling**: T127-T130 can run in parallel (different areas)
- **Polish - Performance**: T131-T134 can run in parallel (different optimizations)
- **Polish - Security**: T135-T138 can run in parallel (different security aspects)
- **Polish - Documentation**: T139-T144 can run in parallel (different documentation types)
- **Once Foundational phase completes**: User Stories 1, 2, 3, 4 can all start in parallel (all independent, no cross-dependencies)

---

## Parallel Example: User Story 1 (Extension Request)

```bash
# Launch backend components for User Story 1 together:
Task T042: "Create Form27ExtGenerator"
Task T043: "Create ExtensionRequestService"

# Launch all frontend services for User Story 1 together:
Task T052: "Create formGenerationService API client"
Task T053: "Create useFormGeneration hook"
Task T054: "Create useFormHistory hook"
```

---

## Parallel Example: User Stories 1-4 (After Foundational Phase)

```bash
# With 4 developers, all P1 and P2 stories can start in parallel:
Developer A: User Story 1 (Form 27-EXT extension request)
Developer B: User Story 2 (Form 27-ES estimated tax vouchers)
Developer C: User Story 3 (Form 27-NOL NOL schedule)
Developer D: User Story 4 (Form 27-W1 withholding report)

# Each story is independently testable and deliverable
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

For fastest time-to-value, focus on extension requests first:

1. Complete Phase 1: Setup (6 tasks)
2. Complete Phase 2: Foundational (35 tasks - CRITICAL, blocks all stories)
3. Complete Phase 3: User Story 1 (19 tasks - extension requests)
4. **STOP and VALIDATE**: Test Form 27-EXT generation independently
5. Deploy/demo if ready

**MVP Value**: Businesses can generate extension requests with calculated payment amounts before April 15 deadline. This is the most time-sensitive form.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (41 tasks)
2. Add User Story 1 → Test independently → Deploy/Demo (MVP! Extension requests) (+19 tasks = 60 total)
3. Add User Story 2 → Test independently → Deploy/Demo (Estimated tax vouchers) (+9 tasks = 69 total)
4. Add User Story 3 → Test independently → Deploy/Demo (NOL schedules) (+8 tasks = 77 total)
5. Add User Story 4 → Test independently → Deploy/Demo (Withholding reports) (+8 tasks = 85 total)
6. Add User Story 5 → Test with US1-4 → Deploy/Demo (Complete filing packages) (+15 tasks = 100 total)
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers after Foundational phase completes:

**Scenario 1: 2 Developers**
- Developer A: User Story 1 (P1) - Extension requests
- Developer B: User Story 2 (P1) - Estimated tax vouchers
- Both stories independent, can integrate separately

**Scenario 2: 4 Developers (Maximum Parallelization)**
- Developer A: User Story 1 (P1) - Extension requests
- Developer B: User Story 2 (P1) - Estimated tax vouchers
- Developer C: User Story 3 (P2) - NOL schedules
- Developer D: User Story 4 (P2) - Withholding reports
- All stories independent until User Story 5 (filing package)

**Scenario 3: Full Team (5 developers)**
- Dev A: US1 (P1) - Extension requests
- Dev B: US2 (P1) - Estimated tax vouchers
- Dev C: US3 (P2) - NOL schedules
- Dev D: US4 (P2) - Withholding reports
- Dev E: Template Management (Phase 9) - Admin features
- Once US1-4 complete: All converge on US5 (filing packages)

---

## Summary

**Total Tasks**: 150 tasks
- **Phase 1 (Setup)**: 6 tasks
- **Phase 2 (Foundational)**: 35 tasks (6 migrations + 2 enums + 4 entities + 4 repositories + 5 DTOs + 7 core services + 2 types + 4 template uploads + 1 Redis config)
- **Phase 3 (User Story 1 - Extension Request)**: 19 tasks (10 backend + 9 frontend)
- **Phase 4 (User Story 2 - Estimated Tax Vouchers)**: 9 tasks (5 backend + 4 frontend)
- **Phase 5 (User Story 3 - NOL Schedule)**: 8 tasks (5 backend + 3 frontend)
- **Phase 6 (User Story 4 - Withholding Report)**: 8 tasks (5 backend + 3 frontend)
- **Phase 7 (User Story 5 - Filing Package)**: 15 tasks (9 backend + 6 frontend)
- **Phase 8 (Cross-Form Validation)**: 5 tasks
- **Phase 9 (Template Management)**: 5 tasks
- **Phase 10 (Additional Forms 27-Y, 27-X)**: 6 tasks
- **Phase 11 (E-Filing Preparation)**: 5 tasks
- **Phase 12 (Data Provenance)**: 5 tasks
- **Phase 13 (Polish)**: 24 tasks

**Task Breakdown by User Story**:
- **US1 (Extension Request)**: 19 tasks (P1 - MVP core)
- **US2 (Estimated Tax Vouchers)**: 9 tasks (P1 - High value)
- **US3 (NOL Schedule)**: 8 tasks (P2 - Complex businesses)
- **US4 (Withholding Report)**: 8 tasks (P2 - Employer businesses)
- **US5 (Filing Package)**: 15 tasks (P3 - Complete workflow)

**Parallel Opportunities Identified**: 20+ groups of parallelizable tasks across all phases

**Independent Test Criteria**:
- **US1**: Generate Form 27-EXT with $25,000 estimated tax, verify new deadline October 15
- **US2**: Generate 4 quarterly vouchers with $5,000 each, verify due dates Apr/Jun/Sep/Jan
- **US3**: Generate Form 27-NOL with 2 NOL vintages, verify $20K carryforward to 2025
- **US4**: Generate Q1 Form 27-W1 with 2 employees, verify YTD totals
- **US5**: Generate complete filing package with 5+ forms, verify TOC and bookmarks

**Suggested MVP Scope**: 
- Phase 1 (Setup): 6 tasks
- Phase 2 (Foundational): 35 tasks
- Phase 3 (User Story 1): 19 tasks
- **Total MVP**: 60 tasks (40% of total)

This MVP delivers Form 27-EXT extension requests - the most time-sensitive form (due April 15). Businesses can request extensions with calculated payment amounts, meeting the critical deadline.

**Suggested V1.0 Scope (All P1 Stories)**:
- Phases 1-4 (Setup + Foundational + US1 + US2): 69 tasks (46% of total)
- Delivers extension requests AND estimated tax vouchers - covers most common business filing needs

**Suggested V2.0 Scope (Add P2 Stories)**:
- Phases 1-6 (Add US3 + US4): 85 tasks (57% of total)  
- Adds NOL schedules and withholding reports - covers complex businesses and employers

**Suggested V3.0 Scope (Complete Feature)**:
- All phases: 150 tasks (100%)
- Full filing package assembly, admin tools, e-filing preparation, complete polish

---

## Notes

- [P] tasks = different files, no dependencies (can run in parallel)
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All file paths are absolute from repository root
- All database migrations follow sequential versioning (V1.30 through V1.35)
- All entities include tenant_id for multi-tenant isolation per Constitution II
- All form generations logged to FormAuditLog per Constitution III
- Data provenance tracking provides AI transparency per Constitution IV
- PDF templates must match official government formats exactly (field positioning, fonts, styling per FR-002)
- Template caching critical for performance (95%+ hit rate target per plan.md)
- File size optimization critical for e-filing (target <10MB per package per FR-033)
