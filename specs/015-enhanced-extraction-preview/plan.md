# Implementation Plan: Enhanced Extraction with PDF Preview and Source Visibility

**Branch**: `015-enhanced-extraction-preview` | **Date**: 2025-12-03 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/015-enhanced-extraction-preview/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

This feature enhances the tax document extraction system with PDF preview, source visibility, extended field extraction, and auditor submission workflows. Users will be able to view extracted data alongside the original PDF, click on any field to see exactly where it was extracted from (with visual highlighting), and understand why certain pages/forms could not be extracted. All submitted returns will be permanently linked to their source documents, maintaining audit trail integrity even when manual corrections are made.

**Technical Approach**: Extend the existing `extraction-service` and frontend components to support bounding box tracking from the Gemini AI extraction, add a PDF viewer component with highlighting overlay, expand the extraction prompt to capture all standard fields for each form type, enhance the submission workflow to persist document associations, and add transparency components showing extraction failures.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3.2.3), TypeScript (React 18+)  
**Primary Dependencies**: Spring Boot, Spring Cloud, React, Vite, Tailwind CSS, pdf.js or react-pdf (for PDF viewing)  
**Storage**: PostgreSQL 16+ (multi-tenant schemas), Document storage for PDFs  
**Testing**: JUnit 5 (backend unit/integration tests), React Testing Library, Vitest (frontend), Playwright (e2e)  
**Target Platform**: Linux server (Docker/Kubernetes), Web browsers (Chrome, Firefox, Safari, Edge)  
**Project Type**: Web application (React frontend + Spring Boot microservices backend)  
**Performance Goals**: PDF preview loads within 2 seconds, extraction highlight navigation <500ms  
**Constraints**: PDFs up to 50 pages, maintain existing extraction quality, preserve audit trail  
**Scale/Scope**: 5,000-10,000 annual tax returns per municipality, 1,000 concurrent users during tax season

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Microservices Architecture First ✅

**Compliant**: Feature extends existing services:
- `extraction-service`: Add bounding box tracking, extended field extraction, failure reasons
- `submission-service`: Add document association with submissions
- `pdf-service`: Leverage for document storage and retrieval

**No new microservice required** - this is domain-appropriate extension of existing services.

### II. Multi-Tenant Data Isolation (NON-NEGOTIABLE) ✅

**Compliant**: All document storage and extraction results inherit tenant context from existing flows. Document associations with submissions are scoped by tenant. PDF viewing respects tenant boundaries via existing authorization.

### III. Audit Trail Immutability ✅

**Compliant**: 
- Submitted returns permanently link to source documents
- Manual corrections preserve original extracted values
- Document associations are immutable after submission
- Extraction metadata (confidence, provenance) persisted with forms

### IV. AI Transparency & Explainability ✅

**Compliant**: Core feature purpose is AI transparency:
- Field-level provenance shows exact extraction location
- Confidence scores displayed for each field
- Bounding box visualization shows AI's "reasoning"
- Failed extractions explained with specific reasons

### V. Security & Compliance First ✅

**Compliant**: No new PII storage patterns. PDF documents already handled by pdf-service with encryption. Viewing requires existing authorization. SSN masking in extraction results maintained.

### VI. User-Centric Design ✅

**Compliant**: Feature is entirely user-centric:
- Visual verification of AI extraction
- Clear failure explanations with actionable suggestions
- Split-screen view for efficient review
- One-click navigation to source locations

### VII. Test Coverage & Quality Gates ✅

**Compliant**: All functional requirements (FR-001 through FR-023) will have corresponding tests:
- Unit tests for extraction field mapping
- Integration tests for document association
- E2E tests for PDF viewer interactions
- Screenshot tests for UI verification

---

**Gate Status**: ✅ **PASSED** - All constitution principles satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/015-enhanced-extraction-preview/
├── spec.md              # Feature specification (complete)
├── plan.md              # This file
├── checklists/
│   └── requirements.md  # Spec quality validation
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/extraction-service/
├── src/main/java/com/munitax/extraction/
│   ├── model/
│   │   ├── ExtractionDto.java              # MODIFIED: Expand BoundingBox, FieldProvenance
│   │   └── ExtendedFormFields.java         # NEW: Comprehensive field definitions per form type
│   ├── service/
│   │   ├── RealGeminiService.java          # MODIFIED: Enhanced prompt, bounding box parsing
│   │   └── ExtractionFieldMapper.java      # NEW: Map raw extraction to extended fields
│   └── controller/
│       └── ExtractionController.java       # MODIFIED: Return enhanced provenance data
└── src/test/java/com/munitax/extraction/
    ├── service/
    │   ├── RealGeminiServiceTest.java      # MODIFIED: Test extended extraction
    │   └── ExtractionFieldMapperTest.java  # NEW: Field mapping tests
    └── integration/
        └── ExtractionIntegrationTest.java  # NEW: End-to-end extraction tests

backend/submission-service/
├── src/main/java/com/munitax/submission/
│   ├── model/
│   │   ├── SubmissionDocument.java         # NEW: Document-submission association
│   │   ├── ManualEntry.java                # NEW: Manual entry with optional doc link
│   │   └── FieldAuditTrail.java            # NEW: Original vs corrected value tracking
│   ├── service/
│   │   └── SubmissionService.java          # MODIFIED: Attach documents on submit
│   └── controller/
│       └── SubmissionController.java       # MODIFIED: Handle document associations
└── src/main/resources/db/migration/
    └── V003__document_association.sql      # NEW: Document-submission link tables

components/
├── PdfViewer/
│   ├── PdfViewer.tsx                       # NEW: PDF rendering with page navigation
│   ├── HighlightOverlay.tsx                # NEW: Bounding box highlighting
│   └── FieldSourceTooltip.tsx              # NEW: Show extraction source info
├── ExtractionReview/
│   ├── SplitViewLayout.tsx                 # NEW: PDF + extracted data side by side
│   ├── FieldWithSource.tsx                 # NEW: Field value with "show source" button
│   └── ExtractionFailures.tsx              # NEW: Display skipped forms/pages
├── ExtractionSummary.tsx                   # MODIFIED: Add PDF preview, failure section
├── ReviewSection.tsx                       # MODIFIED: Add source navigation
└── SubmissionDocuments.tsx                 # NEW: Document attachment UI

services/
├── extractionMapper.ts                     # MODIFIED: Handle extended fields
├── pdfViewerService.ts                     # NEW: PDF page loading, coordinate mapping
└── submissionService.ts                    # MODIFIED: Document association API calls

types.ts                                    # MODIFIED: Add BoundingBox, FieldProvenance types
```

**Structure Decision**: Web application structure with separate frontend (React components) and backend (Spring Boot microservices). Feature extends three existing services:
1. `extraction-service`: Core extraction enhancements
2. `submission-service`: Document association
3. `pdf-service`: PDF storage and retrieval (existing, minimal changes)

## Complexity Tracking

> **Feature does not violate any constitution principles—no justification needed.**

Complexity is managed through:
1. Modular PDF viewer components that can be tested independently
2. Field provenance stored in existing extraction result structure
3. Document associations use standard foreign key relationships
4. Bounding box visualization uses CSS overlays, not PDF modification
