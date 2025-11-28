# Implementation Plan: Business Form Library

**Branch**: `8-business-form-library` | **Date**: 2025-11-28 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/8-business-form-library/spec.md`

## Summary

Implement comprehensive form generation system for all municipal business tax forms including Form 27-EXT (Extension Request), Form 27-ES (Estimated Tax Vouchers), Form 27-NOL (NOL Schedule), Form 27-W1 (Withholding Report), Form 27-Y (Apportionment), Form 27-X (Book-Tax Adjustments), Form 27-PA (Penalty Abatement), and Form 27 (Main Business Return). All forms generate professional PDFs matching official government formats with pre-populated data from the system, validation, electronic submission preparation, and complete filing package assembly.

**Primary Requirement**: Template-based PDF generation engine supporting 9+ municipal form types with automatic data population, cross-form validation, multi-page handling, fillable/flattened PDF output, and filing package assembly with table of contents and bookmarks.

**Technical Approach**: Extend pdf-service microservice with template management system, field mapping engine, and PDF generation pipeline using Apache PDFBox. Implement form validation in tax-engine-service (domain logic), coordinate multi-form data aggregation across services (NOL tracker, apportionment, withholding, penalties), and add form generation UI components to React frontend with preview, draft/final workflow, and filing package builder.

---

## Technical Context

**Language/Version**: 
- **Backend**: Java 21 with Spring Boot 3.2.3
- **Frontend**: TypeScript 5.x with React 18.2, Node.js 20.x
- **Build**: Maven 3.9+ (backend), Vite 5.x (frontend)

**Primary Dependencies**:
- **Backend**: 
  - Apache PDFBox 3.0+ (PDF creation, manipulation, form filling)
  - Spring Data JPA, Spring Web, Spring Cloud (Eureka client, Feign for inter-service calls)
  - PostgreSQL driver, Jackson (JSON), Lombok
  - iText 7.x (optional alternative for PDF generation)
- **Frontend**: 
  - React Router 6.x, Axios, Tailwind CSS 3.x, date-fns
  - react-pdf (PDF preview), pdf-lib (client-side PDF manipulation if needed)
- **Testing**: 
  - JUnit 5, Mockito, AssertJ (backend)
  - PDFBox test utilities for PDF validation
  - Vitest, React Testing Library (frontend)
  - Playwright for E2E form generation workflows

**Storage**: 
- PostgreSQL 16 with multi-tenant schemas:
  - form_templates (template metadata, field mappings, validation rules)
  - generated_forms (form generation history, version tracking, status)
  - filing_packages (multi-form package metadata, submission tracking)
- Object Storage (S3-compatible / MinIO): PDF files, form templates
- Redis 7: Template caching, form field data caching

**Testing**:
- Backend: JUnit 5 + Mockito for service layer, Spring Boot Test for integration tests, TestContainers for PostgreSQL
- PDF Validation: PDFBox utilities to verify form structure, field placement, text content, page count
- Frontend: Vitest + React Testing Library for component tests, Playwright for E2E form generation and filing workflows
- Contract Tests: Verify form generation API contracts between pdf-service and tax-engine-service

**Target Platform**: 
- Docker containers deployed via docker-compose (development) and Kubernetes (production)
- Web browsers: Chrome/Edge 100+, Firefox 100+, Safari 15+ (desktop and mobile for form preview)
- PDF output: Compatible with Adobe Acrobat Reader, browser PDF viewers, e-filing portals

**Project Type**: Web application with microservices backend (9 services) and React SPA frontend

**Performance Goals**:
- Single form generation (1-3 pages): <2 seconds (FR-027, FR-028)
- Complex form with schedules (Form 27-NOL, 15 vintages): <5 seconds
- Complete filing package assembly (20-30 pages): <10 seconds (Success Criteria)
- Template caching hit rate: >95% (reduce template load overhead)
- File size optimization: <10MB per filing package (FR-033)

**Constraints**:
- Multi-tenant data isolation (Constitution II): Form templates and generated forms tenant-scoped
- Audit trail immutability (Constitution III): All form generations, versions, status changes logged
- PDF quality standards (FR-027): 300 DPI minimum, vector graphics, embedded fonts
- E-filing compatibility (FR-041): PDF/A format, no encryption, <20MB size limit
- Must integrate with existing services: tax-engine-service (tax data), submission-service (e-filing workflow)
- Form templates must match official government formats exactly (field positioning, fonts, styling)

**Scale/Scope**:
- Target: 5,000 businesses per municipality
- 9+ form types × 4 quarterly filings + year-end forms = ~50,000 form generations per year per municipality
- Complete filing packages (20+ pages each): ~5,000 packages per year
- Form template library: 9 core forms × 5 tax years = 45 templates minimum
- Peak load: Tax deadline days (April 15) - 10x-100x normal traffic

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ I. Microservices Architecture First

**Evaluation**: Feature extends existing **pdf-service** (owns PDF generation) and **tax-engine-service** (owns tax calculations, form data). Form generation is naturally split across services.

**Service Placement**: 
- PDF generation logic, template management → pdf-service (domain: document generation)
- Form validation, data aggregation, business rules → tax-engine-service (domain: tax calculations and forms)
- Form data extraction → extraction-service (existing AI service)
- Filing submission workflow → submission-service (existing)
- Frontend form UI → React SPA (existing)

**Service Communication**:
- tax-engine-service calls pdf-service via Feign client: "Generate Form 27-EXT with this data"
- pdf-service is stateless: receives form data, returns PDF bytes
- Asynchronous: Large filing packages use event-driven generation (publish event → pdf-service generates → notify completion)

**No Violations**: Feature properly extends existing service boundaries. pdf-service is domain-appropriate owner of PDF generation capability.

---

### ✅ II. Multi-Tenant Data Isolation (NON-NEGOTIABLE)

**Evaluation**: Form templates may be shared across tenants (standard government forms) but generated forms and filing packages must be tenant-isolated.

**Implementation**:
- FormTemplate: Optionally tenant-specific (allows customization per municipality), default templates are cross-tenant
- GeneratedForm, FilingPackage: tenant_id NOT NULL (strict isolation)
- PostgreSQL schema-per-tenant for generated forms: dublin.generated_forms, columbus.generated_forms
- Object storage: Tenant-prefixed paths: s3://forms/dublin/2024/..., s3://forms/columbus/2024/...
- JPA @Filter annotation for automatic tenant scoping on queries

**No Violations**: Feature complies with tenant isolation. Form templates can be shared (efficiency), but generated outputs are strictly isolated.

---

### ✅ III. Audit Trail Immutability

**Evaluation**: Generated forms are legal documents subject to 7-year retention (IRS IRC § 6001). Form regeneration, status changes, submission actions are audit-critical.

**Implementation**:
- GeneratedForm entity: created_at, created_by (user ID), version number, never deleted
- Form regeneration: Create new GeneratedForm with incremented version, preserve all prior versions
- FilingPackage entity: Track all included forms, submission date, confirmation number
- Audit log: form_audit_log table with immutable records:
  - "Form 27-EXT v1 generated by User123 on 2024-04-10"
  - "Form 27-EXT v2 regenerated (replaces v1) on 2024-04-12 - data updated"
  - "Filing package submitted via e-filing portal - confirmation #ABC123"
- Soft delete only: Forms marked as SUPERSEDED but never physically deleted

**No Violations**: Feature implements comprehensive audit trail for all form lifecycle events.

---

### ⚠️ IV. AI Transparency & Explainability

**Evaluation**: Feature depends on pre-populated form data from other services (NOL tracker, apportionment, withholding). Some data comes from AI extraction (W-2s, federal returns).

**Existing Coverage**: extraction-service already provides:
- Bounding box coordinates for extracted fields (Constitution IV requirement)
- Confidence scores per field
- Human override capability

**Gap Identified**: Form generation UI should show data provenance:
- "Estimated tax on Form 27-EXT: $25,000 (source: Prior year return Form 27 line 18)"
- "NOL carryforward on Form 27-NOL: $200K (source: NOL tracker, vintage 2020)"
- "Withholding totals on Form 27-W1: $10K (source: Quarterly W-1 filings Q1-Q4)"

**Mitigation**: Phase 1 will design data provenance display:
- Form preview shows tooltip on hover: "This field was auto-populated from [source]"
- Validation errors include data source: "Form 27-NOL: NOL deduction ($250K) exceeds available balance ($200K from NOL tracker)"
- Form generation summary: "15 fields auto-populated, 2 fields require manual entry"

**Action Required**: Research task in Phase 0 to design data provenance UI and storage model.

---

### ✅ V. Security & Compliance First

**Evaluation**: Form generation involves sensitive data (EIN, financial amounts, SSN on W-2s). Generated PDFs contain complete tax returns (highly sensitive).

**Implementation**:
- Authentication: JWT required on all form generation endpoints (existing auth-service)
- Authorization: ROLE_BUSINESS (generate own forms), ROLE_CPA (generate client forms), ROLE_AUDITOR (view all forms)
- Encryption: Generated PDFs stored encrypted at rest in object storage (S3 server-side encryption)
- Logging: No SSN/EIN in plaintext logs (existing log sanitization)
- TLS 1.3: All production traffic (existing infrastructure)
- Digital signatures: Support for PDF digital signatures for e-filing (FR-040)

**Compliance**: 
- IRS Publication 1075 (safeguarding federal tax information)
- Ohio R.C. 718 (municipal tax confidentiality)
- PDF/A format for archival compliance (FR-041)

**No Violations**: Feature leverages existing security infrastructure and adds PDF encryption/signatures as needed.

---

### ✅ VI. User-Centric Design

**Evaluation**: Form generation must be accessible to non-technical business owners. Complex multi-form filing packages need simplified workflows.

**Implementation**:
- Progressive disclosure: 
  - Dashboard: Simple "Generate Form 27-EXT" button
  - Advanced: "Generate filing package" with form selector checkmarks
- Error prevention: 
  - Pre-generation validation: "Cannot generate Form 27-EXT: Estimated tax amount required"
  - Form preview before finalization: "Review form before marking as FINAL"
- Transparency: 
  - Status indicators: DRAFT (with watermark) → FINAL → SUBMITTED
  - Generation history: "Form 27-EXT v1 generated Apr 10, v2 generated Apr 12 (replaces v1)"
- Wizards: Multi-step filing package builder:
  1. Select forms to include (checkboxes: Form 27, Form 27-Y, Form 27-NOL, etc.)
  2. Review auto-populated data
  3. Preview combined PDF
  4. Generate and download
- Mobile-first: Form list/preview responsive (375px+ width), PDF preview uses responsive viewer

**Accessibility**: 
- Form generation UI meets WCAG 2.1 AA
- PDF preview keyboard-navigable (arrow keys for page navigation)
- Screen reader announcements: "Form 27-EXT generated successfully. Download ready."

**No Violations**: Feature follows user-centric design with progressive disclosure and error prevention.

---

### ✅ VII. Test Coverage & Quality Gates

**Evaluation**: Form generation requires high test coverage due to legal/financial implications. Incorrect form output creates liability.

**Implementation**:
- Unit tests: 100% coverage of form generation logic:
  - FormGenerationService, TemplateFieldMapper, PDFAssemblyService
  - Test each form type (27-EXT, 27-ES, 27-NOL, etc.) with sample data
  - Test edge cases: Multi-page forms, special characters in business names, missing optional fields
- Integration tests: Spring Boot Test with TestContainers:
  - End-to-end form generation: API call → PDF bytes returned → validate PDF structure
  - Multi-service integration: tax-engine-service → pdf-service → verify PDF content matches input data
- PDF Validation Tests: Use PDFBox utilities to verify:
  - Page count matches expected
  - Text content present (business name, EIN, amounts)
  - Form fields positioned correctly (tolerance: ±2 pixels)
  - File size within limits (<10MB)
- Contract Tests: Verify pdf-service API contract:
  - Request schema: { formCode, taxYear, formData }
  - Response: PDF bytes, metadata (page count, file size)
- E2E Tests: Playwright workflow:
  - Navigate to form generation page
  - Select Form 27-EXT
  - Review auto-populated data
  - Generate PDF
  - Verify download
  - Open PDF and validate content

**Quality Gates**:
- Build fails if test coverage <80%
- Build fails if any PDF validation test fails (critical: forms must be correct)
- Manual QA: Visual comparison of generated PDFs vs official government samples (pixel-perfect match)

**No Violations**: Feature includes comprehensive testing strategy with PDF-specific validation.

---

## Constitution Violations Summary

**Total Violations**: 0 (Zero)

**Warnings**: 1 (Data provenance display - addressed in Phase 0 research)

**Status**: ✅ **APPROVED** - Feature complies with all constitution principles. Proceed to Phase 0 research.

### Constitution Check: Post-Design Re-Evaluation (Phase 1 Complete)

**Re-evaluation Date**: *(To be completed after Phase 1)*  
**Artifacts Reviewed**: *(data-model.md, contracts/, quickstart.md - to be created)*

**Status**: ⏳ PENDING PHASE 1 COMPLETION

---

## Project Structure

### Documentation (this feature)

```text
specs/8-business-form-library/
├── plan.md              # This file (/speckit.plan command output) ✅ COMPLETE
├── research.md          # Phase 0 output (NOT YET STARTED)
├── data-model.md        # Phase 1 output (NOT YET STARTED)
├── quickstart.md        # Phase 1 output (NOT YET STARTED)
├── contracts/           # Phase 1 output (NOT YET STARTED)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Backend: Extend pdf-service (Spring Boot microservice)
backend/pdf-service/
├── src/main/java/com/munitax/pdf/
│   ├── domain/
│   │   ├── FormTemplate.java                # JPA entity (FR-001)
│   │   ├── GeneratedForm.java               # JPA entity (FR-037, FR-038)
│   │   ├── FilingPackage.java               # JPA entity (FR-031)
│   │   └── FormAuditLog.java                # Audit trail (Constitution III)
│   ├── repository/
│   │   ├── FormTemplateRepository.java      # Spring Data JPA
│   │   ├── GeneratedFormRepository.java
│   │   └── FilingPackageRepository.java
│   ├── service/
│   │   ├── FormTemplateService.java         # Template management, field mappings (FR-002)
│   │   ├── FormGenerationService.java       # Core PDF generation logic (FR-001, FR-003, FR-027)
│   │   ├── FieldMappingService.java         # Map database fields → PDF form fields (FR-003)
│   │   ├── PDFAssemblyService.java          # Combine forms into filing package (FR-031, FR-032)
│   │   ├── FormValidationService.java       # Pre-generation validation (FR-034, FR-035)
│   │   └── FormVersioningService.java       # Track versions, supersede old forms (FR-038, FR-039)
│   ├── generator/
│   │   ├── AbstractFormGenerator.java       # Base class for all form generators
│   │   ├── Form27ExtGenerator.java          # Form 27-EXT implementation (FR-006, FR-007, FR-008)
│   │   ├── Form27EsGenerator.java           # Form 27-ES implementation (FR-009, FR-010, FR-011)
│   │   ├── Form27NolGenerator.java          # Form 27-NOL implementation (FR-013, FR-014, FR-015)
│   │   ├── Form27W1Generator.java           # Form 27-W1 implementation (FR-017, FR-018, FR-019)
│   │   ├── Form27YGenerator.java            # Form 27-Y implementation (FR-021, FR-022, FR-023)
│   │   ├── Form27XGenerator.java            # Form 27-X implementation (FR-024, FR-025, FR-026)
│   │   └── Form27Generator.java             # Form 27 (Main Return) implementation (existing - enhance)
│   ├── controller/
│   │   ├── FormGenerationController.java    # REST API: POST /api/forms/generate, GET /api/forms/{id}
│   │   ├── FilingPackageController.java     # REST API: POST /api/filing-packages, GET /api/filing-packages/{id}
│   │   └── FormTemplateController.java      # REST API: GET /api/form-templates (admin)
│   ├── dto/
│   │   ├── FormGenerationRequest.java       # Request DTO: { formCode, taxYear, formData }
│   │   ├── FormGenerationResponse.java      # Response DTO: { formId, pdfUrl, pageCount }
│   │   ├── FilingPackageRequest.java        # Request DTO: { returnId, includedForms[] }
│   │   └── FilingPackageResponse.java       # Response DTO: { packageId, pdfUrl, totalPages }
│   └── util/
│       ├── PDFBoxHelper.java                # PDFBox utilities: field filling, page manipulation
│       ├── FormWatermarkUtil.java           # Add/remove DRAFT watermarks (FR-030)
│       └── PDFCompressionUtil.java          # Optimize file size (FR-033)
└── src/test/java/com/munitax/pdf/
    ├── service/
    │   ├── FormGenerationServiceTest.java   # Unit tests
    │   ├── PDFAssemblyServiceTest.java      # Unit tests
    │   └── FormValidationServiceTest.java   # Unit tests
    ├── generator/
    │   ├── Form27ExtGeneratorTest.java      # Test Form 27-EXT generation with sample data
    │   ├── Form27EsGeneratorTest.java       # Test Form 27-ES generation (4 vouchers)
    │   └── Form27NolGeneratorTest.java      # Test Form 27-NOL with 15 vintage NOLs
    ├── integration/
    │   └── FormGenerationIntegrationTest.java # TestContainers + full PDF generation workflow
    └── validation/
        └── PDFValidationTest.java            # Validate PDF structure, content, file size

# Backend: Extend tax-engine-service (coordinate form data)
backend/tax-engine-service/
├── src/main/java/com/munitax/taxengine/
│   ├── service/
│   │   ├── FormDataAggregationService.java  # Aggregate data from multiple sources for forms
│   │   ├── ExtensionRequestService.java     # Business logic: extension calculations (FR-007, FR-008)
│   │   ├── EstimatedTaxService.java         # Business logic: quarterly estimates (FR-010)
│   │   └── FormValidationCoordinator.java   # Cross-form validation (FR-034, FR-036)
│   └── controller/
│       └── FormDataController.java          # REST API: GET /api/form-data/{formCode}/{returnId}

# Backend: Form templates and database migrations
backend/pdf-service/src/main/resources/
├── templates/
│   ├── Form-27-EXT-2024.pdf                 # Official government template (fillable PDF)
│   ├── Form-27-ES-2024.pdf
│   ├── Form-27-NOL-2024.pdf
│   ├── Form-27-W1-2024.pdf
│   ├── Form-27-Y-2024.pdf
│   ├── Form-27-X-2024.pdf
│   └── Form-27-2024.pdf
└── db/migration/
    ├── V1.30__create_form_templates_table.sql
    ├── V1.31__create_generated_forms_table.sql
    ├── V1.32__create_filing_packages_table.sql
    ├── V1.33__create_form_audit_log_table.sql
    └── V1.34__insert_default_form_templates.sql  # Seed default templates

# Frontend: React SPA (extend existing app)
src/
├── components/
│   ├── forms/
│   │   ├── FormGenerationWizard.tsx         # Multi-step form generation wizard (User Story US-5)
│   │   ├── FormSelector.tsx                 # Checkboxes to select forms for package
│   │   ├── FormPreview.tsx                  # PDF preview before finalization (react-pdf)
│   │   ├── FormHistoryTable.tsx             # List of generated forms with versions (FR-037)
│   │   ├── FilingPackageBuilder.tsx         # Filing package assembly UI (FR-031, US-5)
│   │   ├── FormDataReview.tsx               # Review auto-populated data before generation
│   │   └── GeneratedFormCard.tsx            # Display single form with status badge
│   ├── extensions/
│   │   ├── ExtensionRequestForm.tsx         # Form 27-EXT input form (US-1)
│   │   └── ExtensionCalculator.tsx          # Calculate recommended extension payment (FR-007)
│   ├── estimated/
│   │   ├── EstimatedTaxVouchersForm.tsx     # Form 27-ES input form (US-2)
│   │   └── QuarterlyVoucherTable.tsx        # Display 4 quarterly vouchers (FR-009)
│   └── shared/
│       ├── PDFViewer.tsx                    # Embedded PDF viewer (react-pdf)
│       ├── FormStatusBadge.tsx              # Status indicator: DRAFT, FINAL, SUBMITTED (FR-037)
│       └── DataProvenanceTooltip.tsx        # Show data source on hover (Constitution IV)
├── services/
│   ├── formGenerationService.ts             # API client: POST /api/forms/generate
│   ├── filingPackageService.ts              # API client: POST /api/filing-packages
│   ├── formTemplateService.ts               # API client: GET /api/form-templates
│   └── formDataService.ts                   # API client: GET /api/form-data/{formCode}
├── hooks/
│   ├── useFormGeneration.ts                 # React Query hook for form generation
│   ├── useFilingPackage.ts                  # React Query hook for filing package
│   └── useFormHistory.ts                    # React Query hook for form history
└── types/
    ├── formTypes.ts                         # TypeScript types: FormTemplate, GeneratedForm, FilingPackage
    └── formStatus.ts                        # Enum: DRAFT, FINAL, SUBMITTED, AMENDED, SUPERSEDED

# Frontend Tests
src/
└── __tests__/
    ├── components/
    │   ├── FormGenerationWizard.test.tsx    # Component tests (Vitest + RTL)
    │   ├── FormPreview.test.tsx
    │   └── FilingPackageBuilder.test.tsx
    └── e2e/
        ├── form-generation.spec.ts          # Playwright E2E: Generate single form
        └── filing-package.spec.ts           # Playwright E2E: Generate complete filing package

# Object Storage Structure (S3/MinIO)
s3://munitax-forms/
├── templates/
│   ├── 2024/
│   │   ├── Form-27-EXT.pdf
│   │   ├── Form-27-ES.pdf
│   │   └── ...
│   └── 2025/
│       └── ...
└── generated/
    ├── dublin/                              # Tenant-isolated
    │   └── 2024/
    │       ├── businesses/
    │       │   └── {business-id}/
    │       │       ├── form-27-ext-{id}.pdf
    │       │       ├── form-27-es-q1-{id}.pdf
    │       │       └── filing-package-{id}.pdf
    └── columbus/
        └── ...
```

**Structure Decision**: 
- **Backend**: Extend existing pdf-service (owns PDF generation) and tax-engine-service (owns form data validation/aggregation). No new microservice needed (Constitution I).
- **Frontend**: Extend existing React SPA with new form generation components. No separate frontend app needed.
- **Database**: New tables in pdf-service database (form_templates, generated_forms, filing_packages), tenant-scoped per Constitution II.
- **Object Storage**: Use S3-compatible storage for PDF files (templates and generated forms), tenant-prefixed paths for isolation.
- **Integration**: 
  - tax-engine-service → pdf-service (Feign client): Request form generation with data
  - pdf-service → Object Storage: Store generated PDFs
  - Frontend → pdf-service: Download generated PDFs
  - submission-service: Integrate with filing packages for e-filing workflow

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

#### R1: PDF Form Filling Technology Choice

**Question**: Which PDF library should be used for form generation: Apache PDFBox vs iText 7 vs PDF-lib?

**Context**: Need to generate professional PDFs matching government form templates with exact field positioning, embedded fonts, and 300 DPI quality. Forms are complex with tables, calculations, and multi-page layouts.

**Options**:
- **Apache PDFBox 3.0+**: Open-source (Apache 2.0 license), mature, Java-native, supports form filling, page manipulation, text extraction
- **iText 7.x**: Commercial (AGPL + commercial licenses), feature-rich, excellent documentation, used by many enterprise PDF applications
- **PDF-lib (JavaScript)**: Client-side PDF generation in browser, lighter weight, but may have quality/feature limitations

**Acceptance**: Research document must answer:
1. Can PDFBox fill existing PDF form fields (AcroForm) with exact positioning?
2. Does PDFBox support vector graphics (lines, borders) and embedded fonts?
3. What is PDFBox performance for 20-page filing packages? (<10 seconds target)
4. Does PDFBox support PDF/A format (archival standard for e-filing)?
5. Licensing: Confirm Apache 2.0 license is compatible with commercial use
6. Alternative evaluation: Why is iText rejected (if PDFBox chosen)? Cost/licensing vs features trade-off
7. Fallback plan: If PDFBox has gaps, what is migration path to iText?

**Dependencies**: Review Apache PDFBox documentation, test with sample Form 27-EXT template, benchmark generation time.

---

#### R2: Form Template Management Strategy

**Question**: How should form templates be stored and versioned across tax years and municipalities?

**Context**: Form 27-EXT has different versions for 2024, 2025, etc. Some municipalities may have custom form variants (Dublin vs Columbus). Need to support template updates mid-year (government publishes revised forms).

**Options**:
- **Database + Object Storage**: Template metadata in PostgreSQL (form_templates table), actual PDF templates in S3
- **File System**: Templates stored in backend/pdf-service/src/main/resources/templates/ (simple but not dynamic)
- **Template Versioning**: How to handle mid-year updates? (e.g., Form 27-EXT Rev. 01/2024 → Rev. 06/2024)

**Acceptance**: Research document must answer:
1. Storage location: Database vs object storage vs file system? (recommendation with rationale)
2. Template metadata schema: What fields are needed? (formCode, taxYear, revisionDate, fieldMappings, validationRules)
3. Field mapping storage: How to map database fields → PDF form fields? (JSON in database? XML? Code?)
4. Versioning strategy: How to handle template updates? (new row in form_templates with incremented version?)
5. Caching strategy: Cache templates in Redis? (reduce S3 fetch overhead, 95%+ hit rate target)
6. Multi-tenant customization: Can Dublin override default template with custom version? (tenant_id = dublin vs tenant_id = NULL for default)

**Dependencies**: Review PostgreSQL JSONB for field mappings, S3 object versioning, Redis caching patterns.

---

#### R3: Cross-Form Validation Architecture

**Question**: How should system validate data consistency across multiple forms (e.g., NOL on Form 27-NOL matches NOL line on Form 27)?

**Context**: Filing packages include multiple forms with interdependent data. Form 27 line 15 (NOL deduction) must match Form 27-NOL total NOL deduction. Form 27-Y apportionment percentage must match Form 27 apportionment factor. Validation must run before PDF generation to prevent invalid forms.

**Scenarios**:
1. **Synchronous validation**: Before generating filing package, validate all forms together. Block generation if validation fails.
2. **Asynchronous validation**: Generate forms first, run validation job, flag discrepancies, allow user to regenerate.
3. **Multi-service coordination**: NOL validation requires data from NOL tracker service, apportionment validation requires data from apportionment service. How to coordinate?

**Acceptance**: Research document must answer:
1. Validation timing: Pre-generation (synchronous) vs post-generation (asynchronous)? (recommendation with rationale)
2. Validation orchestration: Where does validation logic live? (tax-engine-service? pdf-service? separate validation-service?)
3. Cross-service data fetching: How to get NOL data from NOL tracker, withholding data from W-1 service, etc.? (Feign client calls? Event-driven?)
4. Error reporting: How to show validation errors to user? (Field-level errors: "Form 27 line 15: $250K does not match Form 27-NOL total: $200K")
5. Partial generation: Can user generate individual forms even if filing package validation fails? (e.g., generate Form 27-EXT even if Form 27-NOL has errors)
6. Performance: If validation requires 5 service calls, what is impact on generation time? (<2 seconds target for single form)

**Dependencies**: Review existing validation patterns in tax-engine-service, Feign client configuration for inter-service calls.

---

#### R4: Filing Package Assembly & Bookmarking

**Question**: How to combine multiple PDFs into single filing package with table of contents and PDF bookmarks?

**Context**: Filing package includes Form 27 (3 pages), Form 27-Y (2 pages), Form 27-NOL (1 page), Form 27-X (1 page), Federal 1120 (15 pages) = 22 pages total. Need table of contents, page numbers ("Page 1 of 22"), and PDF bookmarks for navigation.

**Technical Challenges**:
1. **PDF merging**: Combine multiple PDF files without corruption
2. **Page numbering**: Add footer with "Page X of Y" to every page
3. **Table of contents**: Generate TOC page with form names and page numbers
4. **PDF bookmarks**: Add outline structure (Acrobat left sidebar navigation)
5. **File size optimization**: Compress images, remove duplicate fonts, target <10MB

**Acceptance**: Research document must answer:
1. PDF merging: Does PDFBox support merging multiple PDFs? (PDDocument.addPage() from multiple sources?)
2. Page numbering: How to add footer text to existing PDF pages? (PDPageContentStream overlay?)
3. Table of contents: Generate new PDF page with formatted text (form names + page numbers), insert as page 1
4. Bookmarks: PDFBox bookmark API? (PDDocumentOutline, PDOutlineItem)
5. Compression: PDFBox compression options? (JPEG compression for images, font subsetting)
6. Performance: Benchmark 20-page package assembly time (<10 seconds target)

**Dependencies**: Review PDFBox API documentation, test with sample PDFs, benchmark merging/compression performance.

---

#### R5: Data Provenance Tracking & Display

**Question**: How should system track and display where form fields were auto-populated from (Constitution IV: AI Transparency)?

**Context**: Form 27-EXT estimated tax field is auto-populated from prior year Form 27 line 18. User hovers over field in UI, tooltip shows: "Source: 2023 Form 27 line 18 ($22,000)". Need to track data lineage for all auto-populated fields.

**Provenance Sources**:
- Prior year tax returns (Form 27)
- NOL tracker database (Form 27-NOL)
- Apportionment data (Form 27-Y)
- Withholding filings (Form 27-W1)
- AI extraction (W-2s, federal returns)
- User manual entry

**Acceptance**: Research document must answer:
1. Data model: How to store provenance metadata? (JSON field in GeneratedForm? Separate table?)
2. Provenance schema: { fieldName: "estimatedTax", source: "PRIOR_YEAR_RETURN", sourceDetail: "2023 Form 27 line 18", value: 22000, confidence: 1.0 }
3. UI display: Tooltip on hover? Info icon? Dedicated provenance panel?
4. AI confidence: For AI-extracted fields, show confidence score? (e.g., "Estimated tax: $22,000 (90% confidence)")
5. Manual override tracking: If user changes auto-populated field, log override in audit trail? ("User changed estimated tax from $22,000 to $25,000")
6. Performance impact: Storing provenance for 50+ fields per form - database size? Query performance?

**Dependencies**: Review extraction-service provenance format, design UI mockups for provenance display.

---

#### R6: Form Template Field Mapping Design

**Question**: How should field mappings be defined and maintained (database field names → PDF form field names)?

**Context**: Form 27-EXT has fields like "Business Name", "FEIN", "Estimated Tax". PDF template has AcroForm fields like "txt_business_name", "txt_fein", "txt_estimated_tax". Need mapping configuration.

**Mapping Approaches**:
- **Convention-based**: Database field "businessName" → PDF field "txt_business_name" (automatic camelCase → snake_case)
- **Configuration-based**: JSON mapping in form_templates table: { "businessName": "txt_business_name", "fein": "txt_fein", ... }
- **Code-based**: Java mapping class per form: Form27ExtMapper.java with explicit mappings

**Acceptance**: Research document must answer:
1. Mapping approach: Convention vs configuration vs code? (recommendation with rationale)
2. Mapping storage: If configuration-based, store in PostgreSQL JSONB column? Redis cache?
3. Complex mappings: How to handle calculated fields? (e.g., "Balance due" = "Estimated tax" - "Prior payments")
4. Nested data: Form 27-NOL has table with 15 rows. How to map array of NOL vintages → PDF table rows?
5. Conditional fields: Some forms have optional sections. How to handle? (e.g., Form 27-PA penalty abatement reason - only filled if requesting abatement)
6. Maintenance: How do developers update mappings when government changes forms? (SQL migration? Admin UI?)

**Dependencies**: Review PDFBox AcroForm field names extraction, design mapping configuration schema.

---

### Research Deliverables

**research.md** file must include:

1. **Executive Summary**: 1-paragraph summary of all research findings and recommendations
2. **R1: PDF Technology**: Decision on PDFBox vs iText, rationale, licensing, fallback plan
3. **R2: Template Management**: Storage strategy, versioning approach, caching strategy, schema design
4. **R3: Cross-Form Validation**: Validation timing, orchestration, error reporting, performance analysis
5. **R4: Filing Package Assembly**: PDF merging approach, page numbering, TOC generation, bookmark API, compression strategy
6. **R5: Data Provenance**: Provenance data model, UI display design, AI confidence integration, audit trail
7. **R6: Field Mapping**: Mapping approach decision, complex mapping patterns, maintenance workflow
8. **Technology Decisions Summary**: Table with [Decision, Rationale, Alternatives Considered]

**Acceptance Criteria for Phase 0 Completion**:
- All NEEDS CLARIFICATION items in Technical Context are resolved
- All 6 research tasks (R1-R6) have documented decisions with rationale
- Technology choices are concrete (PDFBox vs iText decided, no TBD)
- Constitution Check re-evaluated (data provenance warning addressed)

---

## Phase 1: Design & Contracts

**Status**: NOT STARTED  
**Output Files**: `data-model.md`, `contracts/`, `quickstart.md`

### Phase 1 Deliverables

1. **data-model.md**:
   - Entities: FormTemplate, GeneratedForm, FilingPackage, FormAuditLog
   - Full field definitions (40+ fields for FormTemplate including fieldMappings JSONB, validationRules JSONB)
   - Relationships: GeneratedForm → FormTemplate (FK), FilingPackage → GeneratedForm[] (many-to-many via junction table)
   - Validation constraints (CHECK, UNIQUE, NOT NULL)
   - Indexes: (tenant_id, form_code, tax_year), (return_id, status)
   - Flyway migration plan (V1.30-V1.34)
   - Object storage structure (S3 paths)
   - Data retention policy (7 years per IRS requirement)

2. **contracts/**:
   - **api-form-generation.yaml** (OpenAPI 3.0): Form generation endpoints
     - POST /api/forms/generate (generate single form)
     - GET /api/forms (list generated forms with filters)
     - GET /api/forms/{id} (get form metadata + download URL)
     - PUT /api/forms/{id}/regenerate (regenerate form with updated data)
     - DELETE /api/forms/{id} (soft delete - mark as SUPERSEDED)
   - **api-filing-package.yaml** (OpenAPI 3.0): Filing package endpoints
     - POST /api/filing-packages (create filing package)
     - GET /api/filing-packages/{id} (get package metadata + download URL)
     - GET /api/filing-packages/{id}/forms (list forms in package)
     - PUT /api/filing-packages/{id}/regenerate (regenerate package)
   - **api-form-templates.yaml** (OpenAPI 3.0): Template management (admin)
     - GET /api/form-templates (list templates)
     - GET /api/form-templates/{id} (get template details)
     - POST /api/form-templates (upload new template - admin only)
     - PUT /api/form-templates/{id} (update template - admin only)
   - **event-form-generated.yaml** (AsyncAPI 2.6): Events
     - forms/form-generated (notify form generation complete)
     - forms/filing-package-generated (notify package generation complete)
     - forms/form-validation-failed (alert on validation errors)
     - forms/audit-log (immutable audit trail)

3. **quickstart.md**:
   - Environment setup (Docker, PostgreSQL, MinIO/S3, Redis)
   - API examples: Generate Form 27-EXT, Form 27-ES vouchers, filing package
   - curl commands for all endpoints
   - Database queries (SQL examples)
   - Test execution (unit, integration, E2E, PDF validation)
   - Flyway migration guide
   - Object storage setup (MinIO buckets, IAM policies)
   - Template upload workflow
   - Troubleshooting guide

4. **Update agent context**: Run `.specify/scripts/powershell/update-agent-context.ps1 -AgentType copilot`

### Phase 1 Acceptance Criteria

- data-model.md includes 4 core entities + audit log (5 total)
- contracts/ includes 4 files: 3 OpenAPI specs (form generation, filing package, templates) + 1 AsyncAPI spec (events)
- quickstart.md includes 8+ curl examples covering all major workflows
- Constitution Check re-evaluated (0 violations expected)

---

## Phase 2: Task Breakdown (NOT part of /speckit.plan)

**Status**: NOT STARTED  
**Output File**: `tasks.md` (generated by separate `/speckit.tasks` command)

Phase 2 is NOT included in this plan. After Phase 0 and Phase 1 complete, run `/speckit.tasks` to generate implementation tasks.

---

## Next Steps

1. **Immediate**: Execute Phase 0 research tasks (R1-R6)
2. **Generate research.md**: Document all findings, decisions, rationale
3. **Checkpoint**: Review research.md with team, validate technology choices (PDFBox vs iText decision)
4. **Proceed to Phase 1**: Design data model, API contracts, quickstart guide
5. **Final checkpoint**: Constitution Check post-design
6. **Output**: Deliver plan.md, research.md, data-model.md, contracts/, quickstart.md to implementation team

**Estimated Timeline**:
- Phase 0 (Research): 3-4 days (PDF library evaluation, template management design, validation architecture)
- Phase 1 (Design): 3-4 days (data model, API contracts, quickstart guide)
- **Total Planning**: 6-8 days before implementation begins

---

**Plan Status**: ✅ PHASE 0 READY - Proceed with research tasks (R1-R6)
