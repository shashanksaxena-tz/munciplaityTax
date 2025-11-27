# Business Form Library

**Feature Name:** Comprehensive Municipal Tax Form Generation System  
**Priority:** HIGH  
**Status:** Specification  
**Created:** 2025-11-27

---

## Overview

Implement comprehensive form generation system for all municipal business tax forms including Form 27-EXT (Extension Request), Form 27-ES (Estimated Tax Vouchers), Form 27-NOL (NOL Schedule), Form 27-W1 (Withholding Report), Form 27-Y (Apportionment), Form 27-X (Book-Tax Adjustments), Form 27-PA (Penalty Abatement), and Form 27 (Main Business Return). All forms must generate professional PDFs matching official government formats with pre-populated data from the system.

**Current State:** System only generates basic Form 27. No support for extensions, estimated payments, NOL schedules, withholding reports, or supplemental schedules.

**Target Users:** Business filers needing to submit various forms throughout tax year, CPAs preparing complete filing packages, auditors reviewing multi-form submissions.

---

## User Scenarios & Testing

### US-1: Generate Extension Request (Form 27-EXT) (P1 - Critical)

**User Story:**  
As a business that needs more time to prepare my tax return, I want to generate Form 27-EXT requesting a 6-month extension to October 15, calculate the estimated tax payment due with the extension, and submit electronically, so that I avoid late filing penalties while gathering all necessary documents.

**Business Context:**  
Extension requests are due by original deadline (April 15). Extension is automatic if granted but requires payment of estimated tax. Failure to pay at least 90% of actual tax liability results in late payment penalties even with valid extension.

**Independent Test:**  
- Business estimates 2024 tax liability: $25,000
- Prior year tax: $20,000
- Extension payment: $25,000 (full estimated amount to avoid underpayment)
- Form 27-EXT submitted April 10, 2024
- New filing deadline: October 15, 2024

**Acceptance Criteria:**
- GIVEN tax year not yet filed and approaching deadline
- WHEN user requests extension
- THEN system MUST generate Form 27-EXT with:
  - Business name, FEIN, address
  - Tax year requesting extension for
  - Estimated tax liability
  - Amount paid with extension
  - Calculation: (Estimated tax) - (Prior payments) = Amount due with extension
  - Electronic signature and date
- AND system MUST calculate recommended payment (100% of estimated liability to avoid penalties)
- AND system MUST update filing due date to extended deadline (October 15)
- AND system MUST track extension status (FILED | GRANTED | DENIED)

---

### US-2: Generate Quarterly Estimated Tax Vouchers (Form 27-ES) (P1 - Critical)

**User Story:**  
As a business expecting to owe more than $200 in municipal tax, I want to generate four quarterly estimated tax vouchers (Form 27-ES) with calculated payment amounts and due dates, so that I can make timely quarterly payments and avoid underpayment penalties.

**Business Context:**  
Businesses must make quarterly estimated payments if expecting to owe >$200. Payments due April 15, June 15, September 15, January 15. Each payment typically 25% of annual estimate (or annualized income method if income uneven).

**Independent Test:**  
- 2024 estimated tax: $20,000
- Quarterly payments: $5,000 each
- Generate 4 vouchers:
  - Q1 voucher: $5,000 due April 15, 2024
  - Q2 voucher: $5,000 due June 15, 2024
  - Q3 voucher: $5,000 due September 15, 2024
  - Q4 voucher: $5,000 due January 15, 2025

**Acceptance Criteria:**
- GIVEN estimated annual tax liability >$200
- WHEN generating estimated payment vouchers
- THEN system MUST generate 4 Form 27-ES vouchers (Q1-Q4) with:
  - Business name, FEIN, address
  - Tax year
  - Quarter number (1st, 2nd, 3rd, 4th)
  - Due date (Apr 15, Jun 15, Sep 15, Jan 15)
  - Payment amount (25% of annual estimate or custom)
  - Payment instructions (mail with check or pay online)
- AND system MUST allow custom payment amounts per quarter (annualized income method)
- AND system MUST include detachable payment stub
- AND system MUST track payment status for each voucher

---

### US-3: Generate NOL Schedule (Form 27-NOL) (P2 - High Value)

**User Story:**  
As a business with Net Operating Loss carryforwards, I want to generate Form 27-NOL showing all NOL vintages, amounts used in current year, and remaining balances, so that I can attach comprehensive NOL documentation to my tax return and support my NOL deduction.

**Business Context:**  
NOL schedule required when claiming NOL deduction on main return. Must show multi-year NOL tracking: year of origin, original amount, prior utilization, current year usage, remaining balance, expiration dates.

**Independent Test:**  
- 2020 NOL: Original $200K, used $150K in prior years, used $50K in 2024, remaining $0
- 2021 NOL: Original $100K, used $0 in prior years, used $80K in 2024 (80% limit), remaining $20K
- Form 27-NOL shows both NOLs with calculations and final $20K carryforward to 2025

**Acceptance Criteria:**
- GIVEN business has NOL carryforwards
- WHEN generating NOL schedule
- THEN system MUST generate Form 27-NOL with:
  - Business name, FEIN, tax year
  - Table of NOL carryforwards with columns:
    - Tax Year of Origin
    - Original NOL Amount
    - Used in Prior Years
    - Available This Year
    - Used This Year (limited to 80% of taxable income)
    - Remaining for Future Years
    - Expiration Date (if applicable)
  - Current year calculation:
    - Taxable income before NOL: $X
    - Maximum NOL (80% limit): $Y
    - NOL deduction applied: $Z
    - Taxable income after NOL: $X - $Z
  - Total NOL carryforward to next year
- AND system MUST auto-populate from NOL tracking database
- AND system MUST validate total matches NOL deduction on main return

---

### US-4: Generate Withholding Report (Form 27-W1) (P2 - High Value)

**User Story:**  
As a business that withholds municipal tax from employee wages, I want to generate quarterly Form 27-W1 showing all employees, wages paid, tax withheld, and cumulative YTD totals, so that I can submit my quarterly withholding reports and remit withheld taxes to the municipality.

**Business Context:**  
Businesses must file quarterly withholding reports (Form 27-W1) showing employee-by-employee detail. Due 30 days after quarter end (Apr 30, Jul 31, Oct 31, Jan 31). Annual reconciliation required matching W-2/W-3 totals.

**Independent Test:**  
- Q1 2024 withholding:
  - Employee A: $50K wages, $1,250 tax (2.5%)
  - Employee B: $40K wages, $1,000 tax (2.5%)
  - Total: $90K wages, $2,250 tax
- Form 27-W1 lists both employees and totals
- Due April 30, 2024

**Acceptance Criteria:**
- GIVEN business with employees and withholding
- WHEN generating withholding report
- THEN system MUST generate Form 27-W1 with:
  - Business name, FEIN, quarter, year
  - Employee-by-employee detail:
    - Employee name, SSN
    - Wages subject to municipal tax
    - Tax withheld
  - Quarterly totals:
    - Total wages
    - Total tax withheld
  - Year-to-date totals (cumulative)
  - Payment due: Total tax withheld
  - Due date (30 days after quarter end)
- AND system MUST auto-populate from payroll data
- AND system MUST track cumulative YTD across quarters
- AND system MUST generate W-1 for all 4 quarters (Q1-Q4)

---

### US-5: Generate Complete Filing Package (All Forms) (P3 - Future)

**User Story:**  
As a CPA preparing a business return, I want to generate a complete filing package with all required forms and schedules in a single PDF, properly organized with bookmarks and page numbers, so that I can submit a professional, comprehensive filing to the municipality.

**Business Context:**  
Complete filing typically includes: Form 27 (main return), Form 27-Y (apportionment), Form 27-X (book-tax adjustments), Form 27-NOL (if applicable), Form 27-W1 reconciliation (Q1-Q4 summary), Form 27-EXT (if filed), supporting schedules (K-1s, depreciation, etc.). Must be organized logically and professionally formatted.

**Independent Test:**  
Business filing includes:
- Form 27 (main return) - 3 pages
- Form 27-Y (apportionment) - 2 pages
- Form 27-X (Schedule X adjustments) - 1 page
- Form 27-NOL (NOL schedule) - 1 page
- W-1 reconciliation (annual summary) - 1 page
- Supporting: Federal 1120 (15 pages), K-1s (3 pages)
- Total package: 26 pages with table of contents

**Acceptance Criteria:**
- GIVEN completed tax return with all data
- WHEN generating filing package
- THEN system MUST create single PDF containing:
  - Cover page with business info, filing summary, preparer signature
  - Table of contents with page numbers
  - Form 27 (main return)
  - All applicable schedules (27-Y, 27-X, 27-NOL, 27-W1)
  - Supporting federal forms (1120, K-1s)
  - Bookmarks for navigation
  - Page numbers (e.g., "Page 1 of 26")
  - Professional formatting matching government forms
- AND system MUST allow user to include/exclude specific forms
- AND system MUST compress PDF for efficient submission (<10MB)

---

## Functional Requirements

### Form Generation Engine

**FR-001:** System MUST support template-based PDF generation for all municipal forms:
- Form 27 (Main Business Return)
- Form 27-EXT (Extension Request)
- Form 27-ES (Estimated Payment Vouchers Q1-Q4)
- Form 27-NOL (NOL Schedule)
- Form 27-W1 (Quarterly Withholding Report)
- Form 27-Y (Apportionment Schedule)
- Form 27-X (Schedule X Book-Tax Adjustments)
- Form 27-PA (Penalty Abatement Request)
- Form 27-AMD (Amended Return)

**FR-002:** System MUST use official form templates matching government-published PDFs:
- Exact field positioning
- Official fonts and styling
- Proper form headers and footers
- OMB numbers and form revision dates

**FR-003:** System MUST pre-populate all forms from database:
- Business profile (name, FEIN, address)
- Return data (income, deductions, credits, tax)
- Schedule data (NOLs, apportionment factors, withholding)
- Historical data (prior year amounts)

**FR-004:** System MUST support multi-page forms with page breaks and continuation sheets

**FR-005:** System MUST generate forms as fillable PDFs (allow manual corrections before submission)

### Form 27-EXT (Extension Request)

**FR-006:** System MUST generate Form 27-EXT with fields:
- Business name, FEIN, address, phone
- Tax year requesting extension for
- Estimated total tax liability for the year
- Credits and payments already made
- Balance due with extension
- Amount being paid with extension request
- Reason for extension (dropdown: More time needed, Awaiting K-1s, Other)
- Preparer signature and date

**FR-007:** System MUST calculate recommended extension payment:
- 100% of estimated tax to avoid penalties
- Or 100% of prior year tax (safe harbor)
- Highlight if payment insufficient (<90%)

**FR-008:** System MUST update filing deadline when extension granted:
- Original deadline: April 15
- Extended deadline: October 15 (6 months)
- Display countdown: "Extension filed. Return due in 180 days (Oct 15)."

### Form 27-ES (Estimated Tax Vouchers)

**FR-009:** System MUST generate 4 quarterly vouchers (Form 27-ES-Q1 through Q4) with:
- Business name, FEIN, address
- Tax year
- Quarter (1st, 2nd, 3rd, 4th)
- Due date (Apr 15, Jun 15, Sep 15, Jan 15)
- Payment amount
- Check payment instructions (mail to: [address])
- Electronic payment instructions (pay online at: [url])

**FR-010:** System MUST calculate default payment amounts:
- Standard method: 25% of estimated annual tax per quarter
- Prior year method: 25% of prior year tax per quarter
- Allow user to override with custom amounts (annualized income method)

**FR-011:** System MUST include detachable payment stub on voucher:
- Perforation line
- "Detach and mail with payment" instructions
- Duplicate of FEIN and quarter for processing

**FR-012:** System MUST track voucher status:
- GENERATED: Voucher created
- PAID: Payment received
- LATE: Due date passed without payment
- OVERPAID: Paid more than voucher amount

### Form 27-NOL (NOL Schedule)

**FR-013:** System MUST generate Form 27-NOL with:
- Business identification section
- NOL carryforward table with vintage tracking
- Current year NOL calculation (if generating new NOL)
- NOL deduction calculation (80% limit)
- Summary: Total NOL carryforward to next year

**FR-014:** System MUST populate NOL table from database (FR-001 to FR-006 of Spec 6):
- Auto-fetch all NOL records for business
- Calculate available balances
- Apply FIFO ordering by default
- Show expiration warnings

**FR-015:** System MUST display current year NOL deduction calculation:
```
Taxable income before NOL:          $300,000
Available NOL balance:              $500,000
Maximum NOL deduction (80%):        $240,000
NOL deduction claimed:              $240,000
Taxable income after NOL:           $60,000
Remaining NOL carryforward:         $260,000
```

**FR-016:** System MUST validate NOL schedule totals match Form 27 line for NOL deduction

### Form 27-W1 (Withholding Report)

**FR-017:** System MUST generate quarterly Form 27-W1 with:
- Business identification
- Quarter and year
- Employee detail table:
  - Employee name
  - Social Security Number (XXX-XX-1234 format)
  - Wages subject to municipal tax
  - Municipal tax withheld
- Quarterly totals
- Year-to-date cumulative totals
- Payment due (total tax withheld)
- Due date (30 days after quarter end)

**FR-018:** System MUST populate employee data from payroll integration or manual entry

**FR-019:** System MUST calculate cumulative YTD:
- Track Q1 → Q1
- Track Q1+Q2 → Q2 YTD
- Track Q1+Q2+Q3 → Q3 YTD
- Track Q1+Q2+Q3+Q4 → Q4 YTD (annual total)

**FR-020:** System MUST generate annual W-1 reconciliation (Q4/Year-end report):
- Total wages for year
- Total tax withheld for year
- Reconciliation to W-2/W-3 totals
- Discrepancy identification

### Form 27-Y (Apportionment Schedule)

**FR-021:** System MUST generate Form 27-Y with:
- Property factor calculation (Ohio / Total)
- Payroll factor calculation (Ohio / Total)
- Sales factor calculation (Ohio / Total)
- Formula application (weighted average)
- Final apportionment percentage

**FR-022:** System MUST populate from Schedule Y database (Spec 5: Schedule Y Sourcing)

**FR-023:** System MUST display sourcing method elections:
- Joyce vs Finnigan
- Throwback vs Throwout
- Market-based vs Cost-of-performance

### Form 27-X (Schedule X Book-Tax Adjustments)

**FR-024:** System MUST generate Form 27-X with:
- Book income from financial statements
- Add-backs (federal deductions not allowed for municipal - 20 categories)
- Deductions (municipal deductions not on federal - 7 categories)
- Net adjustments
- Municipal taxable income

**FR-025:** System MUST populate from Schedule X database (Spec 2: Expand Schedule X)

**FR-026:** System MUST include detailed line items for each adjustment (depreciation, meals, interest, etc.)

### PDF Formatting & Quality

**FR-027:** System MUST generate PDFs with professional quality:
- 300 DPI resolution minimum
- Vector graphics for lines and borders
- Embedded fonts (no font substitution)
- Proper page sizes (8.5" × 11" US Letter)

**FR-028:** System MUST include form metadata:
- PDF title: "Form 27-EXT - 2024 Extension Request - [Business Name]"
- Author: MuniTax System
- Creation date
- Keywords for searchability

**FR-029:** System MUST support both fillable and flattened PDFs:
- Fillable: User can edit fields before printing
- Flattened: Read-only, suitable for official submission

**FR-030:** System MUST add watermark for drafts:
- "DRAFT - NOT FOR FILING" diagonal watermark
- Remove watermark when user marks as final

### Filing Package Assembly

**FR-031:** System MUST generate complete filing package PDF with:
- Cover page
- Table of contents with page numbers
- All required forms in logical order:
  1. Form 27 (main return)
  2. Supporting schedules (27-Y, 27-X, 27-NOL)
  3. Withholding reconciliation
  4. Federal return (1120, 1065, or 1120-S)
  5. Supporting documents (K-1s, depreciation schedules)
- Page numbering throughout

**FR-032:** System MUST add PDF bookmarks for navigation:
- Main return
- Each schedule
- Supporting documents
- Allow jump to specific sections

**FR-033:** System MUST optimize PDF file size:
- Compress images
- Remove duplicate resources
- Target <10MB total file size (for email submission)

### Form Validation

**FR-034:** System MUST validate forms before PDF generation:
- Required fields completed
- Calculations correct
- Cross-form consistency (NOL on 27-NOL matches NOL line on Form 27)
- Field lengths within limits (name ≤ 50 chars, FEIN = 9 digits)

**FR-035:** System MUST display validation errors with form reference:
- "Form 27-NOL: NOL deduction ($250K) exceeds available balance ($200K)"
- "Form 27-EXT: Estimated tax required (line 3)"

**FR-036:** System MUST prevent generation of invalid forms (block PDF creation until errors resolved)

### Form History & Versioning

**FR-037:** System MUST track form generation history:
- Form type
- Generation date
- Return year
- User who generated
- PDF file path
- Form status (DRAFT | FINAL | SUBMITTED | AMENDED)

**FR-038:** System MUST support form regeneration:
- If data changes, regenerate forms with updated values
- Increment version (v1, v2, v3)
- Preserve prior versions for audit trail

**FR-039:** System MUST label regenerated forms:
- "Version 2 - Generated [date] - Replaces version 1"

### Electronic Submission Preparation

**FR-040:** System MUST prepare forms for electronic submission:
- Generate XML data file (if municipality accepts e-filing)
- Sign PDF with digital signature (if required)
- Create submission manifest listing all forms

**FR-041:** System MUST validate forms meet e-filing requirements:
- PDF/A format (archival standard)
- No encryption or password protection
- Acceptable file size (<20MB)

**FR-042:** System MUST generate submission confirmation:
- List of forms being submitted
- Total pages
- Total tax due
- Payment method
- "Review before submitting" checklist

---

## Key Entities

### FormTemplate

**Attributes:**
- `templateId` (UUID)
- `formCode` (string): "27", "27-EXT", "27-ES", "27-NOL", "27-W1", "27-Y", "27-X", "27-PA"
- `formName` (string): "Extension Request", "Estimated Tax Voucher", etc.
- `templateFilePath` (string): Path to PDF template
- `revisionDate` (date): Form version date (e.g., "Rev. 01/2024")
- `applicableYears` (array): [2024, 2025, ...] - years this template is valid
- `fieldMappings` (JSON): Map database fields to PDF form fields
- `validationRules` (JSON): Required fields, field formats, cross-field validations

### GeneratedForm

**Attributes:**
- `generatedFormId` (UUID)
- `returnId` (UUID): Foreign key to TaxReturn
- `formCode` (string): "27-EXT", "27-NOL", etc.
- `taxYear` (number)
- `version` (number): 1, 2, 3 (increments with each regeneration)
- `status` (enum): DRAFT | FINAL | SUBMITTED | AMENDED
- `generatedDate` (timestamp)
- `generatedBy` (UUID): User ID
- `pdfFilePath` (string): Location of generated PDF
- `xmlFilePath` (string): XML data file for e-filing (if applicable)
- `isWatermarked` (boolean): Whether PDF has DRAFT watermark
- `pageCount` (number)
- `fileSizeBytes` (number)

### FilingPackage

**Attributes:**
- `packageId` (UUID)
- `returnId` (UUID)
- `taxYear` (number)
- `packageType` (enum): ORIGINAL | AMENDED | EXTENSION
- `createdDate` (timestamp)
- `includedForms` (array): List of GeneratedForm IDs
- `totalPages` (number)
- `packagePDFPath` (string): Combined PDF file
- `tableOfContents` (JSON): Form names and page numbers
- `submissionDate` (timestamp): When submitted to municipality
- `confirmationNumber` (string): Submission tracking number

---

## Success Criteria

- **Form Coverage:** 100% of required municipal forms available for generation (vs current 10% - only main Form 27)
- **Automation:** 95%+ of form fields auto-populated from system data (vs current 50% manual entry)
- **Quality:** Generated PDFs indistinguishable from hand-prepared forms (professional formatting, exact field placement)
- **Time Savings:** Complete filing package generated in <2 minutes (vs 1-2 hours manual form completion)
- **Error Reduction:** Form validation catches 100% of calculation errors and missing required fields before submission
- **E-filing Ready:** PDFs meet 100% of municipality e-filing technical requirements (format, size, signatures)

---

## Assumptions

- Municipality provides official PDF form templates or specifications
- Forms updated annually (new templates for each tax year)
- PDF generation using standard libraries (PDFBox, iText, or similar)
- Electronic signatures supported via digital certificate
- File size limits: 10MB for email, 20MB for portal upload
- Forms fillable before finalization (allow manual corrections)

---

## Dependencies

- **All Specifications:** Form generation pulls data from all other specs (NOL tracker, apportionment, withholding, penalties, etc.)
- **Rule Engine (Spec 4):** Form templates versioned by year, retrieved from rule configuration
- **Double-Entry Ledger (Spec 12):** Payment vouchers integrated with ledger (track voucher payments)

---

## Out of Scope

- **Federal form generation:** IRS forms (1120, 1065, 1040) - use third-party tax software
- **State forms:** Ohio IT-1120, IT-1065 - focus on local only
- **Interactive PDFs:** Full in-PDF calculation (forms are pre-calculated, then flattened)
- **Multi-language forms:** English only (no Spanish, other languages)
- **Paper filing optimization:** Pre-printed forms (focus on digital/print-yourself)

---

## Edge Cases

1. **Form template changed mid-year:** Municipality updates Form 27-EXT template in June. System must use correct template based on generation date (pre-June vs post-June).

2. **Missing data for required field:** Business hasn't entered prior year tax. System blocks Form 27-EXT generation, displays error: "Prior year tax required for extension calculation."

3. **Form doesn't fit on page:** NOL schedule has 15 vintages, doesn't fit on single-page template. System generates continuation sheet: "Form 27-NOL (Page 2 of 2)."

4. **Regeneration after submission:** User already submitted return but needs to regenerate Form 27-NOL with corrected data. System marks original as "SUPERSEDED," generates new version, prompts to file amended return.

5. **Special characters in business name:** Business name includes "&" or non-ASCII characters. System converts to PDF-safe characters or escapes properly.

6. **Extremely large filing package:** Business has 50-page federal return + schedules. Total package 150 pages, 25MB. System splits into multiple PDFs: "Filing-Part1.pdf (Main return)," "Filing-Part2.pdf (Federal return)."

7. **PDF generation failure:** Template corrupted or library error. System logs error, notifies user: "Unable to generate Form 27-EXT. Please try again or contact support."

8. **Voucher payment before voucher generated:** Business makes Q1 payment in March, then generates vouchers in April. System back-dates Q1 voucher to Apr 15 but marks as "PAID" status with payment date March 15.
