# Spec 8: Comprehensive Municipal Tax Form Generation System

**Priority:** HIGH  
**Feature Branch:** `8-business-form-library`  
**Spec Document:** `specs/8-business-form-library/spec.md`

## Overview

Implement comprehensive form generation system for all municipal business tax forms including Form 27-EXT (Extension Request), Form 27-ES (Estimated Tax Vouchers), Form 27-NOL (NOL Schedule), Form 27-W1 (Withholding Report), Form 27-Y (Apportionment), Form 27-X (Book-Tax Adjustments), Form 27-PA (Penalty Abatement), and Form 27 (Main Business Return).

## Implementation Status

**Current:** Basic Form 27 generation exists (~10%)  
**Required:** Full form library with 9+ form types and complete filing packages

## Core Requirements (FR-001 to FR-042)

### Form Generation Engine (FR-001 to FR-005)
- [ ] Support template-based PDF generation for all 9 municipal forms:
  - Form 27 (Main Business Return)
  - Form 27-EXT (Extension Request)
  - Form 27-ES (Estimated Payment Vouchers Q1-Q4)
  - Form 27-NOL (NOL Schedule)
  - Form 27-W1 (Quarterly Withholding Report)
  - Form 27-Y (Apportionment Schedule)
  - Form 27-X (Schedule X Book-Tax Adjustments)
  - Form 27-PA (Penalty Abatement Request)
  - Form 27-AMD (Amended Return)
- [ ] Use official form templates matching government-published PDFs
- [ ] Pre-populate all forms from database
- [ ] Support multi-page forms with continuation sheets
- [ ] Generate forms as fillable PDFs (allow manual corrections)

### Form 27-EXT (Extension Request) (FR-006 to FR-008)
- [ ] Generate with all required fields: Business info, tax year, estimated liability, payments made, balance due, amount being paid, reason, signature
- [ ] Calculate recommended extension payment (100% of estimated tax to avoid penalties)
- [ ] Update filing deadline when extension granted (Oct 15 extended deadline)

### Form 27-ES (Estimated Tax Vouchers) (FR-009 to FR-012)
- [ ] Generate 4 quarterly vouchers with due dates (Apr 15, Jun 15, Sep 15, Jan 15)
- [ ] Calculate default payment amounts: 25% of estimated annual tax
- [ ] Include detachable payment stub on voucher
- [ ] Track voucher status: GENERATED | PAID | LATE | OVERPAID

### Form 27-NOL (NOL Schedule) (FR-013 to FR-016)
- [ ] Generate with NOL carryforward table and vintage tracking
- [ ] Populate from NOL database (auto-fetch all NOL records)
- [ ] Display current year NOL deduction calculation (80% limit)
- [ ] Validate NOL schedule totals match Form 27 NOL deduction line

### Form 27-W1 (Withholding Report) (FR-017 to FR-020)
- [ ] Generate quarterly with employee detail table
- [ ] Calculate cumulative YTD totals (Q1 → Q1, Q1+Q2 → Q2 YTD, etc.)
- [ ] Generate annual W-1 reconciliation (Q4/Year-end report)
- [ ] Reconcile to W-2/W-3 totals with discrepancy identification

### Form 27-Y (Apportionment Schedule) (FR-021 to FR-023)
- [ ] Generate with property, payroll, sales factor calculations
- [ ] Populate from Schedule Y database (Spec 5)
- [ ] Display sourcing method elections (Joyce/Finnigan, Throwback, Market-based)

### Form 27-X (Schedule X Book-Tax Adjustments) (FR-024 to FR-026)
- [ ] Generate with book income, add-backs (20 categories), deductions (7 categories)
- [ ] Populate from Schedule X database (Spec 2)
- [ ] Include detailed line items for each adjustment

### PDF Formatting & Quality (FR-027 to FR-030)
- [ ] Generate PDFs with 300 DPI resolution minimum
- [ ] Include form metadata (title, author, creation date, keywords)
- [ ] Support both fillable and flattened PDFs
- [ ] Add "DRAFT - NOT FOR FILING" watermark for drafts

### Filing Package Assembly (FR-031 to FR-033)
- [ ] Generate complete filing package with cover page and table of contents
- [ ] Include all forms in logical order with page numbering
- [ ] Add PDF bookmarks for navigation
- [ ] Optimize PDF file size (<10MB target for email)

### Form Validation (FR-034 to FR-036)
- [ ] Validate forms before PDF generation (required fields, calculations, cross-form consistency)
- [ ] Display validation errors with form reference
- [ ] Prevent generation of invalid forms

### Form History & Versioning (FR-037 to FR-039)
- [ ] Track form generation history (type, date, user, file path, status)
- [ ] Support form regeneration with version incrementing
- [ ] Label regenerated forms with version number

### Electronic Submission Preparation (FR-040 to FR-042)
- [ ] Prepare forms for electronic submission (XML data file, digital signature if required)
- [ ] Validate forms meet e-filing requirements (PDF/A format, no encryption, size limits)
- [ ] Generate submission confirmation with checklist

## User Stories (5 Priority P1-P3)

1. **US-1 (P1):** Generate Extension Request (Form 27-EXT)
2. **US-2 (P1):** Generate Quarterly Estimated Tax Vouchers (Form 27-ES)
3. **US-3 (P2):** Generate NOL Schedule (Form 27-NOL)
4. **US-4 (P2):** Generate Withholding Report (Form 27-W1)
5. **US-5 (P3):** Generate Complete Filing Package (All Forms)

## Key Entities

### FormTemplate
- templateId, formCode ("27-EXT", "27-NOL", etc.)
- formName, templateFilePath, revisionDate
- applicableYears[], fieldMappings (JSON), validationRules (JSON)

### GeneratedForm
- generatedFormId, returnId, formCode, taxYear
- version, status (DRAFT/FINAL/SUBMITTED/AMENDED)
- generatedDate, generatedBy, pdfFilePath, xmlFilePath
- isWatermarked, pageCount, fileSizeBytes

### FilingPackage
- packageId, returnId, taxYear, packageType (ORIGINAL/AMENDED/EXTENSION)
- createdDate, includedForms[], totalPages
- packagePDFPath, tableOfContents (JSON)
- submissionDate, confirmationNumber

## Success Criteria

- 100% of required municipal forms available for generation (vs current 10%)
- 95%+ of form fields auto-populated from system data
- Generated PDFs indistinguishable from hand-prepared forms
- Complete filing package generated in <2 minutes (vs 1-2 hours manual)
- Form validation catches 100% of calculation errors before submission
- PDFs meet 100% of municipality e-filing requirements

## Edge Cases Documented

- Form template changed mid-year
- Missing data for required field
- Form doesn't fit on page (continuation sheet needed)
- Regeneration after submission
- Special characters in business name
- Extremely large filing package (split into multiple PDFs)
- PDF generation failure
- Voucher payment before voucher generated

## Technical Implementation

### PDF Generation Library
- [ ] Choose and integrate PDF library (PDFBox, iText, or similar)
- [ ] Create form template system with field mapping
- [ ] Implement form filling and validation

### Backend Services
- [ ] FormGenerationService.java
- [ ] FormTemplateService.java
- [ ] FormValidationService.java
- [ ] FilingPackageService.java

### Controllers
- [ ] FormController.java
  - POST /api/forms/generate/{formType}/{returnId}
  - GET /api/forms/{formId}/download
  - POST /api/forms/filing-package/{returnId}

### Frontend Components
- [ ] FormGenerationButton.tsx
- [ ] FormPreview.tsx
- [ ] FilingPackageBuilder.tsx
- [ ] FormDownloadLink.tsx

## Dependencies

- All Specifications - Form generation pulls data from all other specs:
  - NOL tracker (Spec 6) for Form 27-NOL
  - Apportionment (Spec 5) for Form 27-Y
  - Schedule X (Spec 2) for Form 27-X
  - Withholding (Spec 1) for Form 27-W1
  - Penalties (Spec 7) for Form 27-PA
- Rule Engine (Spec 4) - Form templates versioned by year
- Double-Entry Ledger (Spec 12) - Payment vouchers integrated with ledger

## Out of Scope

- Federal form generation (IRS forms)
- State forms (Ohio IT-1120, IT-1065)
- Interactive PDFs (full in-PDF calculation)
- Multi-language forms (English only)
- Paper filing optimization (pre-printed forms)

## Related Specs

- Uses data from: ALL specs (generates forms for all features)
- Critical for: Complete tax filing workflow
- Integrates with: Spec 12 (Ledger for payment vouchers)
