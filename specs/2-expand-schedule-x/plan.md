# Implementation Plan: Comprehensive Business Schedule X Reconciliation (25+ Fields)

**Branch**: `2-expand-schedule-x` | **Date**: 2025-11-27 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/2-expand-schedule-x/spec.md`

## Summary

Expand Schedule X from 6 basic fields to 25+ comprehensive book-to-tax reconciliation fields, enabling CPAs to complete accurate M-1 reconciliations for C-Corporations, Partnerships, and S-Corporations. System provides AI-assisted extraction from Form 1120/1065/1120-S Schedule M-1 and Form 4562 (depreciation schedules), auto-calculation helpers (5% Rule, meals 50%→100% add-back), multi-year comparison view, and comprehensive field-level documentation. Integrates with existing PDF generation service for Form 27 creation.

**Primary Requirement**: Enable complete federal-to-municipal book-to-tax reconciliation including depreciation (MACRS vs Book), amortization, officer compensation, related-party transactions, charitable contributions, meals & entertainment (50% rule), penalties/fines, political contributions, bad debt reserves, and all standard M-1 adjustments.

**Technical Approach**: Extend tax-engine-service BusinessScheduleXDetails model from 6 fields to 27 fields with structured add-backs/deductions objects, update AI extraction service (Gemini) to parse Form 1120 Schedule M-1 and Form 4562, redesign NetProfitsWizard UI with collapsible accordion sections for organized field entry, implement auto-calculation helpers (meals, 5% Rule), and add multi-year comparison API endpoint.

---

## Technical Context

**Language/Version**: 
- **Backend**: Java 21 with Spring Boot 3.2.3
- **Frontend**: TypeScript 5.x with React 19.2, Node.js 20.x
- **Build**: Maven 3.9+ (backend), Vite 6.x (frontend)

**Primary Dependencies**:
- **Backend**: Spring Data JPA, Spring Web, Spring Cloud (Eureka client), PostgreSQL driver, Jackson (JSON), Lombok
- **Frontend**: React 19.2, React Router 7.x, Lucide React (icons), jsPDF 2.5.1 (PDF generation)
- **AI Extraction**: Google Gemini AI (@google/genai 1.30.0) via extraction-service
- **Testing**: JUnit 5, Mockito (backend), Vitest (frontend)

**Storage**: 
- PostgreSQL 16 with multi-tenant schemas (tenant-scoped business_schedule_x_details table expansion)
- Existing business_tax_return table stores BusinessScheduleXDetails as JSONB field (no schema migration needed - JSONB allows dynamic field expansion)

**Testing**:
- Backend: JUnit 5 + Mockito for service layer (BusinessTaxCalculator), Spring Boot Test for integration tests
- Frontend: Vitest for component tests (NetProfitsWizard, ScheduleXAccordion), manual QA for AI extraction accuracy
- AI Extraction: Test with sample Form 1120 Schedule M-1 PDFs (10 samples covering all entity types)

**Target Platform**: 
- Docker containers deployed via docker-compose (development) and production deployment
- Web browsers: Chrome/Edge 100+, Firefox 100+, Safari 15+ (desktop primarily - CPA workflow)

**Project Type**: Web application with microservices backend (8 services) and React SPA frontend

**Performance Goals**:
- AI extraction of 27 Schedule X fields from Form 1120 PDF: <10 seconds (FR-039, Success Criteria)
- NetProfitsWizard UI load time: <2 seconds with 27 fields rendered (Success Criteria)
- Multi-year comparison view (3 years): <2 seconds (FR-038, Success Criteria)
- Auto-calculation helpers (meals 50%→100%, 5% Rule): <100ms real-time calculation (Success Criteria)

**Constraints**:
- Multi-tenant data isolation: All queries scoped to tenant schema via tenant context (existing infrastructure)
- JSONB field expansion: BusinessScheduleXDetails stored as JSONB - no SQL migration needed, but must maintain backward compatibility with existing 6-field structure
- AI extraction accuracy target: 90% for standard C-Corp returns (Success Criteria) - must validate extracted values before auto-populating form
- PDF generation service integration: Must export all 27 fields to Form 27 PDF with proper formatting

**Scale/Scope**:
- Target: 5,000 businesses per municipality filing net profits returns
- ~60% C-Corps, 30% Partnerships, 10% S-Corps (affects field relevance - guaranteed payments only for partnerships)
- Average 15 Schedule X adjustments per return (out of 27 available fields)
- Multi-year comparison: 3-5 years typical (recurring adjustments like depreciation)

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ I. Microservices Architecture First

**Evaluation**: Feature extends existing **tax-engine-service** (owns tax calculations, business returns). Schedule X is core tax domain logic. No new service required.

**Service Placement**: 
- Schedule X data model → tax-engine-service (domain: business tax calculations)
- AI extraction updates → extraction-service (existing Gemini service)
- PDF generation → pdf-service (existing Form 27 generation)
- Frontend UI → React SPA (existing NetProfitsWizard component)

**No Violations**: Feature properly extends existing service boundaries.

---

### ✅ II. Multi-Tenant Data Isolation (NON-NEGOTIABLE)

**Evaluation**: BusinessScheduleXDetails is already tenant-scoped via business_tax_return → business → tenant_id foreign key chain. Expanding JSONB fields maintains tenant isolation.

**Implementation**:
- Tenant context from JWT (existing auth-service integration)
- PostgreSQL schema-per-tenant (dublin.business_tax_return, columbus.business_tax_return)
- JPA queries already tenant-scoped via business entity (no changes needed)

**No Violations**: Feature complies with tenant isolation requirements.

---

### ✅ III. Audit Trail Immutability

**Evaluation**: Schedule X data is financial record subject to 7-year retention (IRS IRC § 6001). Changes to Schedule X (amended returns) must preserve history.

**Implementation**:
- business_tax_return entity: created_at, updated_at, version (existing optimistic locking)
- Amended returns: new business_tax_return record with amends_return_id reference to original (both preserved)
- AI extraction metadata: auto_calculated_fields, manual_overrides arrays in BusinessScheduleXDetails (FR-037)
- Supporting documents: attached_documents array with file URLs (FR-036)

**No Violations**: Feature implements immutable audit trails per constitution.

---

### ⚠️ IV. AI Transparency & Explainability

**Evaluation**: Feature heavily depends on AI extraction of 27 Schedule X fields from Form 1120/1065 Schedule M-1 and Form 4562.

**Existing Coverage**: extraction-service already provides:
- Confidence scores per field (existing)
- Human override capability (existing UI)

**Gap Identified**: 
1. **Bounding box coordinates** for extracted Schedule X fields not currently returned (Constitution IV requirement)
2. **Explanation of auto-calculations** (5% Rule, meals 50%→100%) needs UI tooltips

**Mitigation**: 
- Phase 0 research task R1: Design bounding box coordinate extraction for Form 1120 Schedule M-1 line items
- Phase 1: Add confidence score display and override UI for each Schedule X field
- Phase 1: Implement help tooltips explaining each auto-calculation (FR-031)

**Action Required**: Research task in Phase 0 to update extraction-service API contract for bounding boxes.

---

### ✅ V. Security & Compliance First

**Evaluation**: Schedule X data contains sensitive financial information (EIN, income amounts, tax adjustments).

**Implementation**:
- Authentication: JWT required (existing auth-service)
- Authorization: ROLE_BUSINESS (file returns), ROLE_CPA (file on behalf of clients), ROLE_AUDITOR (view all returns)
- Encryption: EIN/tax amounts encrypted at rest (existing encryption layer in database)
- Logging: No sensitive tax data in logs (existing log sanitization)
- TLS 1.3: All production traffic (existing infrastructure)

**Compliance**: IRS Publication 1075 (federal tax info safeguarding), Ohio R.C. 718 (municipal tax confidentiality)

**No Violations**: Feature leverages existing security infrastructure.

---

### ✅ VI. User-Centric Design

**Evaluation**: Feature UI must simplify complex M-1 reconciliation workflow for CPAs (tax experts) without overwhelming them with 27 fields.

**Implementation**:
- Progressive disclosure: Collapsible accordion sections (Add-Backs vs Deductions) - only show relevant fields for entity type (FR-004 guaranteed payments only for partnerships)
- Field organization: Group related fields (Depreciation Adjustments, Meals & Entertainment, Related-Party Transactions)
- Auto-calculation helpers: Meals field has "Auto-calc 50% add-back" button, 5% Rule has "Calculate from intangible income" button (FR-031)
- Help icons: Each field has help tooltip explaining what adjustment represents and when it applies (FR-031)
- Import button: "Import from Federal Return" pre-fills fields from uploaded Form 1120 PDF (FR-032)
- Validation warnings: Flag if Adjusted Municipal Income differs from Federal Income by >20% (FR-034)
- Multi-year comparison: Side-by-side view shows prior year Schedule X for reference (FR-038)

**No Violations**: Feature follows user-centric design principles.

---

### ✅ VII. Test Coverage & Quality Gates

**Evaluation**: Feature requires comprehensive test coverage for financial calculations (Total Add-Backs, Total Deductions, Adjusted Municipal Income).

**Implementation**:
- Unit tests: 100% coverage of BusinessTaxCalculator service (Schedule X calculation logic)
- Integration tests: Test all 5 user stories with representative data (C-Corp with depreciation, Partnership with guaranteed payments, S-Corp with related-party transactions, etc.)
- AI extraction tests: 10 sample Form 1120/1065 PDFs covering edge cases (multi-page schedules, handwritten annotations, scanned documents)
- Frontend component tests: Vitest tests for ScheduleXAccordion, auto-calculation buttons, validation warnings
- Manual QA: CPA review of AI extraction accuracy (10 real returns), Form 27 PDF output validation

**Quality Gates**:
- Build fails if test coverage <80%
- Build fails if integration tests fail (Schedule X calculation scenarios)
- Manual QA checklist: Test auto-calculations (meals, 5% Rule), test multi-year comparison, test AI extraction for all entity types (1120, 1065, 1120-S)

**No Violations**: Feature includes comprehensive testing strategy.

---

## Constitution Violations Summary

**Total Violations**: 0 (Zero)

**Warnings**: 1 (Bounding box coordinates for AI extraction - addressed in Phase 0 research)

**Status**: ✅ **APPROVED** - Feature complies with all constitution principles. Proceed to Phase 0 research.

### Constitution Check: Post-Design Re-Evaluation (Phase 1 Complete)

**Re-evaluation Date**: After Phase 1 completion  
**Artifacts Reviewed**: data-model.md, contracts/, quickstart.md

**Status**: ⏳ **PENDING** - Will be completed after Phase 1 design phase

---

## Project Structure

### Documentation (this feature)

```text
specs/2-expand-schedule-x/
├── spec.md             # Feature specification (COMPLETE - 252 lines)
├── plan.md             # This file (/speckit.plan command output) ✅ IN PROGRESS
├── research.md         # Phase 0 output (TODO)
├── data-model.md       # Phase 1 output (TODO)
├── quickstart.md       # Phase 1 output (TODO)
├── contracts/          # Phase 1 output (TODO)
│   ├── api-schedule-x.yaml           # OpenAPI spec for Schedule X endpoints
│   ├── data-business-schedule-x.yaml # JSON Schema for BusinessScheduleXDetails
│   └── event-schedule-x-updated.yaml # AsyncAPI event for Schedule X changes
└── tasks.md            # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Backend: Extend tax-engine-service (Spring Boot microservice)
backend/tax-engine-service/
├── src/main/java/com/munitax/taxengine/
│   ├── model/
│   │   ├── BusinessScheduleXDetails.java         # EXPAND: 6 fields → 27 fields (FR-001 to FR-027)
│   │   │   # Current fields: fedTaxableIncome, incomeAndStateTaxes, interestIncome, dividends, capitalGains, other
│   │   │   # New fields: add addBacks object (20 properties), deductions object (7 properties), calculatedFields object (3 properties), metadata object (4 properties)
│   │   ├── NetProfitReturnData.java              # No changes (already references BusinessScheduleXDetails)
│   │   └── BusinessTaxRulesConfig.java           # UPDATE: Add municipal rules (Section 179 limit, meals treatment, 5% Rule)
│   ├── service/
│   │   ├── BusinessTaxCalculator.java            # UPDATE: Use expanded BusinessScheduleXDetails for calculation (FR-028, FR-029, FR-030)
│   │   ├── ScheduleXCalculationService.java      # NEW: Dedicated service for Schedule X logic (total add-backs, total deductions, adjusted income)
│   │   ├── ScheduleXAutoCalculationService.java  # NEW: Auto-calculation helpers (5% Rule, meals 50%→100%, charitable 10% limit)
│   │   └── ScheduleXValidationService.java       # NEW: Validation logic (FR-033 federal income match, FR-034 variance flag)
│   ├── controller/
│   │   ├── NetProfitsController.java             # UPDATE: Add endpoints for Schedule X operations
│   │   │   # New endpoints: GET /api/schedule-x/multi-year-comparison, POST /api/schedule-x/auto-calculate
│   └── dto/
│       ├── BusinessScheduleXDetailsDto.java       # NEW: DTO for expanded Schedule X (maps to frontend TypeScript types)
│       ├── ScheduleXAutoCalcRequest.java          # NEW: Request DTO for auto-calculation (field name, input values)
│       └── MultiYearComparisonDto.java            # NEW: Response DTO for multi-year comparison (FR-038)
└── src/test/java/com/munitax/taxengine/
    ├── service/
    │   ├── BusinessTaxCalculatorTest.java         # UPDATE: Add tests for expanded Schedule X calculation
    │   ├── ScheduleXCalculationServiceTest.java   # NEW: Unit tests for Schedule X logic (12 test cases)
    │   ├── ScheduleXAutoCalculationServiceTest.java # NEW: Unit tests for auto-calculation helpers (8 test cases)
    │   └── ScheduleXValidationServiceTest.java    # NEW: Unit tests for validation (5 test cases)
    └── integration/
        └── ScheduleXIntegrationTest.java          # NEW: Integration tests for all 5 user stories (US-1 to US-5)

# Backend: Update extraction-service (Gemini AI service)
backend/extraction-service/
├── src/main/java/com/munitax/extraction/
│   ├── service/
│   │   ├── GeminiExtractionService.java           # UPDATE: Add Schedule X field extraction (FR-039, FR-040, FR-041)
│   │   │   # New extraction logic: Parse Form 1120 Schedule M-1 (lines 1-10), Form 4562 (depreciation), Form 1065 K-1
│   │   └── ExtractionPromptBuilder.java           # UPDATE: Add prompts for 27 Schedule X fields with bounding box requirements
│   └── model/
│       ├── ScheduleXExtractionResult.java         # NEW: Extraction result with confidence scores per field (FR-042)
│       └── ExtractionBoundingBox.java             # NEW: Bounding box coordinates for extracted fields (Constitution IV)
└── src/test/resources/
    └── test-pdfs/
        ├── form-1120-schedule-m1-sample-1.pdf     # NEW: C-Corp with depreciation adjustments
        ├── form-1120-schedule-m1-sample-2.pdf     # NEW: C-Corp with meals & entertainment
        ├── form-1065-schedule-m1-sample-1.pdf     # NEW: Partnership with guaranteed payments
        ├── form-1120s-schedule-m1-sample-1.pdf    # NEW: S-Corp with related-party transactions
        └── form-4562-depreciation-sample.pdf      # NEW: Depreciation schedule (MACRS vs Book)

# Backend: Update pdf-service (Form 27 generation)
backend/pdf-service/
├── src/main/java/com/munitax/pdf/
│   ├── service/
│   │   └── Form27Generator.java                   # UPDATE: Expand Schedule X section to show all 27 fields (FR-035)
│   └── templates/
│       └── form-27-template.html                  # UPDATE: Add HTML template for expanded Schedule X (20 add-back rows, 7 deduction rows)

# Frontend: React SPA (expand existing NetProfitsWizard)
src/
├── components/
│   ├── business/
│   │   ├── NetProfitsWizard.tsx                   # UPDATE: Add Schedule X step with accordion UI
│   │   ├── ScheduleXAccordion.tsx                 # NEW: Collapsible sections for Add-Backs vs Deductions (FR-031)
│   │   ├── ScheduleXFieldInput.tsx                # NEW: Reusable input field with help icon, auto-calc button, confidence score display
│   │   ├── ScheduleXAutoCalcButton.tsx            # NEW: Auto-calculation button (5% Rule, meals 50%→100%) (FR-031)
│   │   ├── ScheduleXHelpTooltip.tsx               # NEW: Help tooltip component explaining each adjustment (FR-031)
│   │   ├── ScheduleXConfidenceScore.tsx           # NEW: Display AI extraction confidence score (FR-042)
│   │   ├── ScheduleXImportButton.tsx              # NEW: "Import from Federal Return" button (FR-032)
│   │   ├── ScheduleXValidationWarning.tsx         # NEW: Warning component for >20% variance (FR-034)
│   │   ├── ScheduleXMultiYearComparison.tsx       # NEW: Multi-year comparison table (FR-038)
│   │   └── ScheduleXAttachmentUpload.tsx          # NEW: Upload supporting documentation (FR-036)
│   └── shared/
│       └── CollapsibleAccordion.tsx               # NEW: Reusable accordion component (used by ScheduleXAccordion)
├── services/
│   ├── scheduleXService.ts                        # NEW: API client for Schedule X endpoints
│   ├── autoCalculationService.ts                  # NEW: Client for auto-calculation helpers
│   └── extractionService.ts                       # UPDATE: Add Schedule X extraction endpoint
├── hooks/
│   ├── useScheduleX.ts                            # NEW: React hook for Schedule X data (React Query)
│   ├── useScheduleXAutoCalc.ts                    # NEW: React hook for auto-calculation
│   └── useMultiYearComparison.ts                  # NEW: React hook for multi-year comparison
├── types/
│   ├── scheduleX.ts                               # NEW: TypeScript types for BusinessScheduleXDetails
│   │   # Types: AddBacks (20 properties), Deductions (7 properties), CalculatedFields (3 properties), Metadata (4 properties)
│   └── scheduleXValidation.ts                     # NEW: Validation error types
└── utils/
    ├── scheduleXCalculations.ts                   # NEW: Frontend calculation helpers (total add-backs, total deductions)
    └── scheduleXFormatting.ts                     # NEW: Format currency, percentages for display

# Frontend Tests
src/
└── __tests__/
    ├── components/
    │   ├── ScheduleXAccordion.test.tsx            # Component tests (Vitest)
    │   ├── ScheduleXFieldInput.test.tsx           # Component tests
    │   ├── ScheduleXAutoCalcButton.test.tsx       # Component tests (test auto-calc logic)
    │   └── ScheduleXMultiYearComparison.test.tsx  # Component tests
    └── utils/
        └── scheduleXCalculations.test.ts           # Unit tests for calculation helpers

# Constants/Configuration
src/
└── constants/
    └── scheduleXConstants.ts                      # NEW: Field definitions, help text, validation rules
        # Constants: SCHEDULE_X_FIELDS (27 field definitions), AUTO_CALC_RULES (meals, 5% Rule), VALIDATION_THRESHOLDS (20% variance)

# No database migrations needed - BusinessScheduleXDetails stored as JSONB in business_tax_return table
# JSONB allows dynamic field expansion without schema changes
# Backward compatibility: Existing 6-field returns remain valid, new fields default to 0 or null
```

**Structure Decision**: 
- **Backend**: Extend existing tax-engine-service (owns business tax calculations), extraction-service (AI extraction), pdf-service (Form 27 generation). No new microservice needed.
- **Frontend**: Extend existing React SPA with new Schedule X components in business/ directory. Reusable components in shared/.
- **Database**: No SQL migration needed - BusinessScheduleXDetails stored as JSONB field in business_tax_return table. JSONB expansion is backward-compatible.
- **Integration**: Leverage existing extraction-service (Gemini AI), pdf-service (Form 27 generation), auth-service (JWT).

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

#### R1: AI Extraction - Bounding Box Coordinates for Schedule X Fields (Constitution IV requirement)

**Question**: How should extraction-service return bounding box coordinates for each extracted Schedule X field from Form 1120 Schedule M-1?

**Context**: Constitution IV requires "Bounding box coordinates (x, y, width, height)" for all AI-extracted data. Existing extraction-service returns confidence scores but not bounding boxes. CPAs need to verify AI-extracted Schedule M-1 line items visually against source PDF.

**Scenarios**:
1. Form 1120 Schedule M-1 has 10 standard lines (Line 1: Net income/loss per books, Line 2: Federal income tax, etc.)
2. Some lines have multiple sub-items (Line 5a: Depreciation, Line 5b: Other)
3. Form 4562 (depreciation schedule) spans multiple pages with MACRS vs Book columns
4. Gemini Vision API can return bounding box coordinates for text regions

**Acceptance**: Research document must answer:
1. Does Gemini Vision API already provide bounding box coordinates? (Check @google/genai 1.30.0 API docs)
2. How to store bounding boxes in ExtractionResult (JSON structure: `{field: "depreciationAdjustment", boundingBox: {page: 1, x: 100, y: 200, width: 50, height: 20}}`)?
3. UI design: How to display bounding boxes? (Clickable field → highlights region in PDF viewer?)
4. Performance impact: Does requesting bounding boxes slow down extraction? (Benchmark: extract 27 fields with/without bounding boxes)

**Dependencies**: Review Gemini Vision API documentation, test extraction with bounding box requests.

---

#### R2: JSONB Field Expansion - Backward Compatibility Strategy

**Question**: How to expand BusinessScheduleXDetails JSONB field from 6 fields to 27 fields while maintaining backward compatibility with existing returns?

**Context**: Existing business_tax_return records store BusinessScheduleXDetails as JSONB with 6 fields:
```json
{
  "fedTaxableIncome": 500000,
  "incomeAndStateTaxes": 10000,
  "interestIncome": 5000,
  "dividends": 3000,
  "capitalGains": 2000,
  "other": 0
}
```

New structure requires nested objects:
```json
{
  "fedTaxableIncome": 500000,
  "addBacks": {
    "depreciationAdjustment": 50000,
    "incomeAndStateTaxes": 10000,
    "mealsAndEntertainment": 15000,
    ...
  },
  "deductions": {
    "interestIncome": 5000,
    "dividends": 3000,
    ...
  },
  "calculatedFields": {...},
  "metadata": {...}
}
```

**Trade-offs**:
- **Option A - Migration script**: Run one-time script to convert all existing 6-field records to new structure (move incomeAndStateTaxes to addBacks.incomeAndStateTaxes, etc.). Pros: Clean data model. Cons: Risk of data loss, requires downtime.
- **Option B - Runtime conversion**: BusinessTaxCalculator detects old format and converts on-the-fly, saves in new format on update. Pros: No downtime. Cons: Two code paths to maintain.
- **Option C - Support both formats**: Accept old 6-field and new 27-field format indefinitely. Pros: Simple. Cons: Technical debt, confusing for developers.

**Acceptance**: Research document must recommend approach with:
- Migration strategy (if Option A)
- Code example: How to detect and convert old format (if Option B)
- Rollback plan if migration fails
- Test cases: Verify old returns still calculate correctly after expansion

**Dependencies**: Test with existing business_tax_return data (production snapshot if available).

---

#### R3: Auto-Calculation Helpers - Implementation Approach

**Question**: Should auto-calculation helpers (5% Rule, meals 50%→100%, charitable 10% limit) execute client-side (frontend) or server-side (backend API)?

**Context**: FR-031 requires auto-calculation helpers. For example:
- **Meals 50%→100%**: User enters "Federal meals deduction: $30,000" → System auto-calculates "Municipal add-back: $30,000" (because municipal allows 0%, federal allows 50%, so add back full 50% that was deducted federally)
- **5% Rule**: User enters "Interest income: $20,000, Dividends: $15,000" → System auto-calculates "5% Rule add-back: $1,750" (5% of $35,000 intangible income)
- **Charitable 10% limit**: User enters "Charitable contributions: $80,000, Taxable income before contributions: $600,000" → System calculates "Current year deduction: $60,000 (10% limit), Carryforward: $20,000"

**Trade-offs**:
- **Option A - Frontend only**: Implement calculations in scheduleXCalculations.ts (TypeScript). Pros: Instant feedback, no API latency. Cons: Logic duplication (must also implement server-side for validation), complex calculations (charitable 10% limit with carryforward) harder to test in TypeScript.
- **Option B - Backend API**: POST /api/schedule-x/auto-calculate with {field: "mealsAndEntertainment", inputs: {federalMeals: 30000}}. Pros: Single source of truth, complex calculations easier in Java. Cons: Network latency (200-500ms), requires API call for each auto-calc.
- **Option C - Hybrid**: Simple calculations (meals 50%→100%) in frontend, complex calculations (charitable 10% limit) in backend. Pros: Best of both worlds. Cons: Two code paths, developers need to know which calculations go where.

**Acceptance**: Research document must recommend approach with:
- Decision matrix: Which calculations are "simple" vs "complex"?
- Code example: How to implement 5% Rule in chosen approach
- Performance test: Measure latency for backend API option (10 concurrent auto-calc requests)
- Test strategy: How to ensure frontend and backend calculations match (if Option C)

**Dependencies**: Benchmark frontend vs backend calculation performance, consider user experience (instant feedback vs accuracy).

---

#### R4: Multi-Year Comparison - Data Retrieval and Performance

**Question**: How should multi-year comparison view retrieve Schedule X data for prior years while maintaining performance targets (<2 seconds for 3 years)?

**Context**: FR-038 requires "multi-year comparison view showing current year vs prior year Schedule X side-by-side for recurring adjustments". CPAs use this to identify patterns (depreciation increases/decreases year-over-year, recurring meals adjustments).

**Scenarios**:
1. Business has 5 prior years of returns (2020-2024), CPA wants to compare 2024 to 2023-2022
2. Each prior year return has full 27-field Schedule X data (stored as JSONB in business_tax_return)
3. Query must be tenant-scoped (multi-tenant architecture)
4. Performance target: <2 seconds for 3-year comparison

**Trade-offs**:
- **Option A - Single query with JSON aggregation**: `SELECT year, schedule_x_details FROM business_tax_return WHERE business_id = X AND year IN (2024, 2023, 2022) ORDER BY year DESC`. Pros: Simple, fast (single query). Cons: Large payload if 27 fields × 3 years, may exceed response size limits.
- **Option B - Separate queries per year**: Query 2024, then 2023, then 2022. Pros: Smaller payloads. Cons: 3× database round-trips (slower).
- **Option C - Cached API endpoint**: GET /api/schedule-x/multi-year-comparison?years=2024,2023,2022 with Redis cache (5-minute TTL). Pros: Fast after first load. Cons: Cache invalidation complexity (must invalidate when Schedule X updated).

**Acceptance**: Research document must recommend approach with:
- Performance benchmark: Test query time with 5,000 business records, 5 years each (simulated load)
- Response size: Calculate payload size for 27 fields × 3 years (is it <100KB? <1MB?)
- Caching strategy (if Option C): Cache key structure, invalidation rules
- UI design: How to display 27 fields × 3 years without overwhelming user (table? chart? collapsible rows?)

**Dependencies**: Test with PostgreSQL JSONB query performance, consider frontend rendering performance (React re-render time for large data).

---

#### R5: Form 27 PDF Generation - Layout Design for 27 Fields

**Question**: How should Form 27 PDF layout display 27 Schedule X fields without spanning multiple pages or reducing readability?

**Context**: FR-035 requires "Schedule X data exports to Form 27 PDF with all line items properly labeled and totaled". Current Form 27 has space for ~10 Schedule X lines. Expanding to 27 lines may not fit on single page.

**Design Options**:
- **Option A - Two-column layout**: Add-Backs (20 fields) on left column, Deductions (7 fields) on right column. Pros: Fits on single page. Cons: Small font (8pt?), harder to read.
- **Option B - Multi-page Schedule X**: Page 1 shows summary (Total Add-Backs, Total Deductions, Adjusted Income), Page 2 shows detailed line items. Pros: Readable font (10pt), clear layout. Cons: Two pages to review.
- **Option C - Show only non-zero fields**: Only render fields with values > 0. Pros: Compact, most returns have <15 adjustments. Cons: Inconsistent page count per return, harder to compare returns.
- **Option D - Separate Schedule X attachment**: Form 27 shows summary only, full Schedule X is separate PDF attachment. Pros: Clean Form 27, detailed Schedule X for audits. Cons: Two files to manage.

**Acceptance**: Research document must recommend approach with:
- Mockup: PDF layout screenshot showing 27 fields (use sample data from User Story 1)
- Font size: Confirm readability (minimum 9pt for CPA review)
- Compliance check: Does municipal tax code require specific Schedule X format? (Check Dublin Form 27 instructions)
- Test with pdf-service: Generate sample Form 27 with 27-field Schedule X, review with stakeholders

**Dependencies**: Review Dublin Form 27 PDF template, consult with CPA stakeholders on preferred layout.

---

### Research Deliverables

**research.md** file must include:

1. **Executive Summary**: 1-paragraph summary of all research findings and recommendations
2. **R1: Bounding Box Coordinates**: Decision on API changes, data structure, UI design, performance benchmark
3. **R2: JSONB Backward Compatibility**: Chosen approach (A/B/C), migration script or conversion code, rollback plan, test results
4. **R3: Auto-Calculation Helpers**: Chosen approach (frontend/backend/hybrid), decision matrix, code examples, performance tests
5. **R4: Multi-Year Comparison**: Chosen approach (single query/separate/cached), performance benchmark, response size calculation, UI design
6. **R5: Form 27 PDF Layout**: Chosen layout option (A/B/C/D), mockup screenshot, font size decision, compliance check
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
   - **BusinessScheduleXDetails** expanded structure:
     - fedTaxableIncome: number (existing)
     - addBacks: object with 20 properties (FR-001 to FR-020)
       - depreciationAdjustment, amortizationAdjustment, incomeAndStateTaxes, guaranteedPayments, mealsAndEntertainment, relatedPartyExcess, penaltiesAndFines, politicalContributions, officerLifeInsurance, capitalLossExcess, federalTaxRefunds, expensesOnIntangibleIncome, section179Excess, bonusDepreciation, badDebtReserveIncrease, charitableContributionExcess, domesticProductionActivities, stockCompensationAdjustment, inventoryMethodChange, otherAddBacks
       - otherAddBacksDescription: string (required if otherAddBacks > 0)
     - deductions: object with 7 properties (FR-021 to FR-027)
       - interestIncome, dividends, capitalGains, section179Recapture, municipalBondInterest, depletionDifference, otherDeductions
       - otherDeductionsDescription: string (required if otherDeductions > 0)
     - calculatedFields: object (read-only)
       - totalAddBacks: number (FR-028)
       - totalDeductions: number (FR-029)
       - adjustedMunicipalIncome: number (FR-030)
     - metadata: object
       - lastModified: timestamp
       - autoCalculatedFields: string[] (FR-037)
       - manualOverrides: string[] (FR-037)
       - attachedDocuments: array of {fileName: string, fileUrl: string, fieldName: string} (FR-036)
   - **ExtractionResult** expanded structure (extraction-service):
     - scheduleXFields: map<string, ScheduleXFieldExtraction>
     - ScheduleXFieldExtraction: {value: number, confidence: number, boundingBox: {page: number, x: number, y: number, width: number, height: number}}
   - Backward compatibility notes for existing 6-field BusinessScheduleXDetails
   - Validation constraints (e.g., fedTaxableIncome must match Form 1120 Line 30)
   - Data retention policy (7 years per IRS requirement)

2. **contracts/**:
   - **api-schedule-x.yaml** (OpenAPI 3.0): 5 endpoints
     - GET /api/net-profits/{returnId}/schedule-x (get Schedule X data)
     - PUT /api/net-profits/{returnId}/schedule-x (update Schedule X data)
     - POST /api/schedule-x/auto-calculate (auto-calculation helpers)
     - GET /api/schedule-x/multi-year-comparison (multi-year comparison)
     - POST /api/schedule-x/import-from-federal (import from uploaded Form 1120)
   - **data-business-schedule-x.yaml** (JSON Schema): Complete schema for BusinessScheduleXDetails with all 27 fields, validation rules, examples
   - **event-schedule-x-updated.yaml** (AsyncAPI 2.6): 2 event channels
     - business-tax/schedule-x-updated (triggers PDF regeneration)
     - business-tax/schedule-x-validation-warning (triggers alert if >20% variance)

3. **quickstart.md**:
   - Environment setup (Java 21, Maven, PostgreSQL, Gemini API key)
   - 8 API examples with curl commands (update Schedule X, auto-calculate meals, multi-year comparison, import from federal)
   - Sample BusinessScheduleXDetails JSON (all 27 fields populated)
   - AI extraction test: Upload sample Form 1120 Schedule M-1, verify extracted fields
   - Frontend component integration: How to use ScheduleXAccordion in NetProfitsWizard
   - PDF generation test: Generate Form 27 with expanded Schedule X
   - Troubleshooting guide (AI extraction confidence < 0.9, validation warnings, multi-year comparison slow)

4. **Update agent context**: Run `.specify/scripts/powershell/update-agent-context.ps1 -AgentType copilot` to update agent-specific context with Schedule X expansion details

### Phase 1 Acceptance Criteria

- ✅ data-model.md includes expanded BusinessScheduleXDetails with all 27 fields + metadata
- ✅ contracts/ includes 3 files: api-schedule-x.yaml (5 endpoints), data-business-schedule-x.yaml (JSON schema), event-schedule-x-updated.yaml (2 channels)
- ✅ quickstart.md includes 8 curl examples, AI extraction test, PDF generation test
- ✅ Constitution Check re-evaluated (see below - target 0 violations)

---

## Phase 2: Task Breakdown (NOT part of /speckit.plan)

**Status**: NOT STARTED  
**Output File**: `tasks.md` (generated by separate `/speckit.tasks` command)

Phase 2 is NOT included in this plan. After Phase 0 and Phase 1 complete, run `/speckit.tasks` to generate implementation tasks.

---

## Next Steps

1. **Immediate**: Execute Phase 0 research tasks (R1-R5)
2. **Generate research.md**: Document all findings, decisions, rationale
3. **Checkpoint**: Review research.md with team, validate technology choices (especially AI bounding boxes, JSONB backward compatibility, Form 27 PDF layout)
4. **Proceed to Phase 1**: Design data model, API contracts, quickstart guide
5. **Final checkpoint**: Constitution Check post-design
6. **Output**: Deliver plan.md, research.md, data-model.md, contracts/, quickstart.md to implementation team

**Estimated Timeline**:
- Phase 0 (Research): 2-3 days (AI extraction testing, JSONB migration testing, PDF layout design)
- Phase 1 (Design): 2-3 days (data model design, API contract creation, quickstart guide)
- **Total Planning**: 4-6 days before implementation begins

---

## Database Schema Changes

**No SQL migrations required** - BusinessScheduleXDetails stored as JSONB field in existing business_tax_return table.

**JSONB expansion strategy**:
```sql
-- Existing structure (6 fields)
{
  "fedTaxableIncome": 500000,
  "incomeAndStateTaxes": 10000,
  "interestIncome": 5000,
  "dividends": 3000,
  "capitalGains": 2000,
  "other": 0
}

-- New structure (27 fields) - backward compatible
{
  "fedTaxableIncome": 500000,
  "addBacks": {
    "depreciationAdjustment": 50000,
    "amortizationAdjustment": 0,
    "incomeAndStateTaxes": 10000,  -- Migrated from top level
    "guaranteedPayments": 0,
    "mealsAndEntertainment": 15000,
    "relatedPartyExcess": 0,
    "penaltiesAndFines": 0,
    "politicalContributions": 0,
    "officerLifeInsurance": 0,
    "capitalLossExcess": 0,
    "federalTaxRefunds": 0,
    "expensesOnIntangibleIncome": 0,
    "section179Excess": 0,
    "bonusDepreciation": 0,
    "badDebtReserveIncrease": 0,
    "charitableContributionExcess": 0,
    "domesticProductionActivities": 0,
    "stockCompensationAdjustment": 0,
    "inventoryMethodChange": 0,
    "otherAddBacks": 0,
    "otherAddBacksDescription": null
  },
  "deductions": {
    "interestIncome": 5000,  -- Migrated from top level
    "dividends": 3000,       -- Migrated from top level
    "capitalGains": 2000,    -- Migrated from top level
    "section179Recapture": 0,
    "municipalBondInterest": 0,
    "depletionDifference": 0,
    "otherDeductions": 0,    -- Migrated from "other"
    "otherDeductionsDescription": null
  },
  "calculatedFields": {
    "totalAddBacks": 75000,
    "totalDeductions": 10000,
    "adjustedMunicipalIncome": 565000
  },
  "metadata": {
    "lastModified": "2024-11-27T19:45:00Z",
    "autoCalculatedFields": ["expensesOnIntangibleIncome", "mealsAndEntertainment"],
    "manualOverrides": [],
    "attachedDocuments": []
  }
}
```

**Backward compatibility handling**:
- BusinessTaxCalculator.java detects old format (top-level incomeAndStateTaxes, interestIncome, dividends, capitalGains, other)
- Runtime conversion: Move fields to new nested structure
- Save in new format on next update
- Old format remains valid for read-only operations (historical returns)

---

## API Endpoints

**New Endpoints** (tax-engine-service):

1. **GET /api/net-profits/{returnId}/schedule-x**
   - Get Schedule X data for a return
   - Response: BusinessScheduleXDetailsDto (27 fields + metadata)
   - Auth: JWT required, ROLE_BUSINESS or ROLE_CPA
   - Performance: <500ms

2. **PUT /api/net-profits/{returnId}/schedule-x**
   - Update Schedule X data
   - Request: BusinessScheduleXDetailsDto
   - Response: Updated BusinessScheduleXDetailsDto with calculatedFields
   - Validation: FR-033 (federal income match), FR-034 (variance flag)
   - Auth: JWT required, ROLE_BUSINESS or ROLE_CPA

3. **POST /api/schedule-x/auto-calculate**
   - Auto-calculate helper values (5% Rule, meals 50%→100%, charitable 10% limit)
   - Request: ScheduleXAutoCalcRequest {field: string, inputs: object}
   - Response: {calculatedValue: number, explanation: string}
   - Examples:
     - {field: "mealsAndEntertainment", inputs: {federalMeals: 30000}} → {calculatedValue: 30000, explanation: "Municipal allows 0% meals deduction..."}
     - {field: "expensesOnIntangibleIncome", inputs: {interestIncome: 20000, dividends: 15000}} → {calculatedValue: 1750, explanation: "5% Rule: 5% × $35,000 intangible income"}
   - Performance: <100ms

4. **GET /api/schedule-x/multi-year-comparison**
   - Get Schedule X data for multiple years (side-by-side comparison)
   - Query params: businessId, years (comma-separated, e.g., "2024,2023,2022")
   - Response: MultiYearComparisonDto {years: string[], data: BusinessScheduleXDetailsDto[]}
   - Performance: <2 seconds (FR-038)
   - Auth: JWT required, ROLE_BUSINESS or ROLE_CPA

5. **POST /api/schedule-x/import-from-federal**
   - Import Schedule X fields from uploaded Form 1120/1065 PDF
   - Request: {returnId: string, federalFormPdfUrl: string}
   - Response: ExtractionResult with confidence scores per field (FR-042)
   - Triggers extraction-service Gemini AI extraction
   - Performance: <10 seconds (FR-039, Success Criteria)
   - Auth: JWT required, ROLE_BUSINESS or ROLE_CPA

**Updated Endpoints** (extraction-service):

6. **POST /api/extraction/schedule-x**
   - Extract Schedule X fields from Form 1120/1065 PDF
   - Request: {pdfUrl: string, entityType: "C-Corp" | "Partnership" | "S-Corp"}
   - Response: ScheduleXExtractionResult with 27 fields, confidence scores, bounding boxes
   - AI model: Gemini 1.5 Pro Vision
   - Performance: <10 seconds

---

## Frontend Components

**New Components**:

1. **ScheduleXAccordion.tsx**
   - Collapsible accordion with 2 sections: "Add-Backs (Increase Federal Income)" and "Deductions (Decrease Federal Income)"
   - Props: scheduleXData, onUpdate, entityType (C-Corp/Partnership/S-Corp)
   - Conditionally renders fields based on entity type (e.g., guaranteed payments only for partnerships)
   - Displays calculated totals (Total Add-Backs, Total Deductions, Adjusted Municipal Income)

2. **ScheduleXFieldInput.tsx**
   - Reusable input field for each Schedule X field
   - Props: fieldName, value, onChange, helpText, showAutoCalcButton, confidenceScore
   - Features: Currency formatting, help icon with tooltip, auto-calc button (if applicable), confidence score badge (if AI-extracted)
   - Validation: Real-time validation (e.g., must be non-negative number)

3. **ScheduleXAutoCalcButton.tsx**
   - Auto-calculation button for 5% Rule, meals 50%→100%, charitable 10% limit
   - Props: fieldName, inputs, onCalculated
   - Calls POST /api/schedule-x/auto-calculate on click
   - Displays loading spinner, then shows calculated value with explanation tooltip

4. **ScheduleXHelpTooltip.tsx**
   - Help tooltip component explaining each adjustment
   - Props: fieldName
   - Content: Field description, when it applies, example (e.g., "Depreciation Adjustment: Add back if book depreciation < MACRS. Example: Book $80K, MACRS $130K → Add back $50K")

5. **ScheduleXConfidenceScore.tsx**
   - Displays AI extraction confidence score (0-1)
   - Props: score, boundingBox (optional)
   - Visual: Green badge (score ≥ 0.9), yellow badge (0.7-0.89), red badge (< 0.7)
   - Click to view bounding box highlight in PDF viewer (if boundingBox provided)

6. **ScheduleXImportButton.tsx**
   - "Import from Federal Return" button
   - Triggers POST /api/schedule-x/import-from-federal
   - Shows progress modal during extraction (10 seconds)
   - Displays extraction results with confidence scores, allows manual override

7. **ScheduleXValidationWarning.tsx**
   - Warning component for >20% variance between federal and municipal income
   - Props: fedTaxableIncome, adjustedMunicipalIncome
   - Calculation: variance = abs(adjustedMunicipalIncome - fedTaxableIncome) / fedTaxableIncome
   - Displays warning if variance > 0.2: "⚠️ Adjusted Municipal Income differs from Federal Income by 25% - please verify all adjustments"

8. **ScheduleXMultiYearComparison.tsx**
   - Multi-year comparison table showing Schedule X for 3 years side-by-side
   - Props: businessId, years
   - Calls GET /api/schedule-x/multi-year-comparison
   - Features: Sortable columns, highlight changed fields year-over-year, export to CSV

9. **ScheduleXAttachmentUpload.tsx**
   - Upload supporting documentation (Excel workpaper, depreciation schedule)
   - Props: returnId, fieldName (attach to specific Schedule X field)
   - Stores file URL in metadata.attachedDocuments array

**Updated Components**:

10. **NetProfitsWizard.tsx**
    - Add "Schedule X Reconciliation" step (Step 3 of 5)
    - Replace simple 6-field input with ScheduleXAccordion component
    - Pass entity type (C-Corp/Partnership/S-Corp) from BusinessTaxRulesConfig

---

## Testing Strategy

### Unit Tests (Backend)

**BusinessTaxCalculatorTest.java** (expanded):
- Test Schedule X calculation with all 27 fields populated
- Test backward compatibility (old 6-field format still calculates correctly)
- Test validation (FR-033 federal income match, FR-034 variance flag)
- Edge case: All fields zero (adjusted income = federal income)
- Edge case: Large variance >20% (should trigger warning)

**ScheduleXCalculationServiceTest.java** (new):
- Test totalAddBacks calculation (sum of 20 fields) (FR-028)
- Test totalDeductions calculation (sum of 7 fields) (FR-029)
- Test adjustedMunicipalIncome = fedTaxableIncome + totalAddBacks - totalDeductions (FR-030)
- Edge case: Negative adjusted income (loss carryforward scenario)

**ScheduleXAutoCalculationServiceTest.java** (new):
- Test 5% Rule: intangibleIncome = $35,000 → expensesOnIntangibleIncome = $1,750
- Test meals 50%→100%: federalMeals = $30,000 → mealsAndEntertainment = $30,000
- Test charitable 10% limit: contributions = $80,000, taxableIncome = $600,000 → deduction = $60,000, carryforward = $20,000
- Edge case: 5% Rule manual override (user provides actual expense > 5%)
- Edge case: Meals federally disallowed (no federal deduction → no municipal add-back)

**ScheduleXValidationServiceTest.java** (new):
- Test FR-033: Federal income must match Form 1120 Line 30 (within $100 tolerance)
- Test FR-034: Variance >20% triggers validation warning
- Test required field validation (otherAddBacksDescription required if otherAddBacks > 0)
- Edge case: Federal income = $0 (startup loss scenario)

### Integration Tests (Backend)

**ScheduleXIntegrationTest.java** (new):
- **User Story 1**: C-Corp with depreciation, meals, state taxes → Adjusted income = $575,000
- **User Story 2**: Partnership with guaranteed payments, intangible income, 5% Rule → Adjusted income = $316,750
- **User Story 3**: S-Corp with related-party rent adjustment → Adjusted income = $402,500
- **User Story 4**: C-Corp with charitable contributions 10% limit → No add-back (follows federal)
- **User Story 5**: Manufacturing with DPAD → Add-back $25,000
- Test multi-year comparison: Query 3 years, verify response time <2 seconds
- Test import from federal: Upload Form 1120 PDF, verify AI extraction accuracy >90%
- Test PDF generation: Generate Form 27 with 27-field Schedule X, verify all fields present

### Frontend Tests (Vitest)

**ScheduleXAccordion.test.tsx**:
- Render accordion with add-backs and deductions sections
- Expand/collapse sections on click
- Display calculated totals (Total Add-Backs, Total Deductions, Adjusted Municipal Income)
- Conditional rendering: Guaranteed payments only for partnerships

**ScheduleXFieldInput.test.tsx**:
- Render input field with help icon, auto-calc button, confidence score
- Currency formatting ($50,000.00)
- Validation: Reject negative numbers, non-numeric input

**ScheduleXAutoCalcButton.test.tsx**:
- Click button → calls POST /api/schedule-x/auto-calculate
- Display loading spinner during API call
- Display calculated value with explanation tooltip

**ScheduleXMultiYearComparison.test.tsx**:
- Render 3-year comparison table
- Highlight changed fields year-over-year
- Export to CSV

### Manual QA Checklist

1. **AI Extraction Accuracy**:
   - Upload 10 sample Form 1120 PDFs (5 C-Corps, 3 Partnerships, 2 S-Corps)
   - Verify AI extracts all 27 fields with >90% accuracy
   - Verify bounding box coordinates highlight correct PDF regions
   - Test edge cases: Multi-page schedules, handwritten annotations, scanned documents

2. **Auto-Calculation Helpers**:
   - Test 5% Rule: Enter interest $20K, dividends $15K → Verify $1,750 calculated
   - Test meals 50%→100%: Enter federal meals $30K → Verify $30K add-back calculated with explanation
   - Test charitable 10% limit: Enter contributions $80K, taxable income $600K → Verify $60K deduction, $20K carryforward

3. **Multi-Year Comparison**:
   - Load 3-year comparison for business with 5 prior years
   - Verify response time <2 seconds
   - Verify table displays all 27 fields × 3 years
   - Verify changed fields highlighted (depreciation increase year-over-year)

4. **Form 27 PDF Generation**:
   - Generate Form 27 with 27-field Schedule X (all fields populated)
   - Verify layout fits on page (or multi-page if designed that way)
   - Verify font size readable (minimum 9pt)
   - Verify totals match calculated values (Total Add-Backs, Total Deductions, Adjusted Income)

5. **Backward Compatibility**:
   - Load existing return with old 6-field Schedule X
   - Verify fields display correctly (top-level fields migrated to nested structure)
   - Update one field → Verify saves in new 27-field format
   - Re-load return → Verify new format persists

---

## Success Metrics

1. **AI Extraction Accuracy**: 90% of AI-extracted Schedule X fields require zero manual correction for standard C-Corp returns (Success Criteria)
   - Measurement: Upload 100 C-Corp Form 1120 PDFs, count fields requiring manual correction
   - Target: ≤10% correction rate

2. **Time Savings**: CPAs complete full Schedule X reconciliation in <10 minutes with AI-assisted extraction (vs 45+ minutes manual) (Success Criteria)
   - Measurement: Time from "Upload Form 1120" to "Schedule X complete" for 20 test CPAs
   - Target: Average <10 minutes (78% time savings)

3. **Calculation Accuracy**: Adjusted Municipal Income calculation matches CPA's manual workpaper within $100 for 98% of returns (Success Criteria)
   - Measurement: Compare system-calculated Adjusted Municipal Income to CPA manual calculation for 50 returns
   - Target: ≥98% match within $100

4. **System Performance**: Multi-year comparison view loads in <2 seconds for businesses with 3+ prior year returns (Success Criteria)
   - Measurement: Load 3-year comparison for 100 businesses, measure response time
   - Target: P95 < 2 seconds

5. **User Adoption**: 80% of net profits returns use expanded Schedule X (27 fields) within 3 months of release
   - Measurement: Query business_tax_return table, count returns with new 27-field format vs old 6-field
   - Target: ≥80% adoption rate after 3 months

6. **AI Confidence**: Average AI extraction confidence score ≥0.85 for all 27 Schedule X fields
   - Measurement: Average confidence scores from 100 extracted returns
   - Target: Mean confidence ≥0.85 (high confidence)

---

**Plan Status**: ✅ PHASE 0 READY - Proceed with research tasks (R1-R5)
