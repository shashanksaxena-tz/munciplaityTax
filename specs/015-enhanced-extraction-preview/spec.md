# Feature Specification: Enhanced Extraction with PDF Preview and Source Visibility

**Feature Branch**: `015-enhanced-extraction-preview`  
**Created**: 2025-12-03  
**Status**: Draft  
**Input**: User description: "Improve the extraction service to extract more fields from the forms. Review each form to check which forms need more fields extracted as per requirements and which would be useful to show on the UI. Update the UI, BE and extraction in sync with more forms. User should see the uploaded PDF, with a preview of why a certain value is extracted and from where it was extracted from the page. Forms submitted to auditor should always be tied with supporting documents even if manual entries are used. User should see which forms were not extracted and why. Test the UI and attach screenshots."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View PDF with Extraction Source Highlighting (Priority: P1)

As a taxpayer, after uploading my tax documents, I want to view the original PDF alongside the extracted data so I can verify each extracted field by clicking on it to see exactly where and why the value was extracted from the document.

**Why this priority**: This is the core user-facing feature that enables transparency and trust in the extraction process. Users need to understand and verify AI-extracted data before submission.

**Independent Test**: Can be fully tested by uploading a PDF, viewing extracted fields, and clicking a field to see the PDF page with the source location highlighted. Delivers immediate transparency value.

**Acceptance Scenarios**:

1. **Given** a user has uploaded a PDF tax document, **When** they view the extraction summary, **Then** they can see a split view with extracted data on one side and the PDF preview on the other.

2. **Given** an extracted field is displayed with confidence score, **When** the user clicks a "show source" button next to the field, **Then** the PDF viewer navigates to the relevant page and highlights the bounding box where the value was extracted.

3. **Given** a field has been extracted from a W-2 Box 1 (federalWages), **When** the user clicks to view the source, **Then** they see the PDF page with Box 1 visually highlighted and a tooltip showing "Extracted from W-2, Box 1, Page 1".

4. **Given** a field has low confidence (below 70%), **When** the user clicks the source preview, **Then** the system displays both the extracted value and the raw text detected, allowing the user to correct if needed.

---

### User Story 2 - Extended Field Extraction for All Form Types (Priority: P1)

As a taxpayer or business filer, I want the system to extract all relevant fields from each tax form type so that I don't have to manually enter data that is clearly visible on the documents.

**Why this priority**: Comprehensive extraction reduces manual data entry errors and saves significant user time, making the entire filing process more efficient.

**Independent Test**: Can be tested by uploading different form types and verifying that all expected fields are extracted with appropriate confidence scores.

**Acceptance Scenarios**:

1. **Given** a W-2 form is uploaded, **When** extraction completes, **Then** the system extracts: Employer name, EIN, Federal wages (Box 1), Social Security wages (Box 3), Medicare wages (Box 5), Social Security tax withheld (Box 4), Medicare tax withheld (Box 6), State wages (Box 16), State tax withheld (Box 17), Local wages (Box 18), Local tax withheld (Box 19), Locality name (Box 20), and Employee SSN (masked).

2. **Given** a Federal 1040 form is uploaded, **When** extraction completes, **Then** the system extracts: Filing status, Taxpayer name, SSN (masked), Spouse name and SSN (if joint), Total income (Line 9), Adjusted Gross Income (Line 11), Standard/Itemized deduction amount, Taxable income (Line 15), Total tax (Line 24), Federal withholding (Line 25a), and Refund or Amount Owed.

3. **Given** a Schedule E form is uploaded, **When** extraction completes, **Then** the system extracts for each rental property: Property address, Property type, Days rented, Personal use days, Rental income, and all expense categories (Lines 5-19); and for each K-1 entity: Entity name, EIN, Ordinary income/loss, and Passive/Non-passive classification.

4. **Given** a 1099-NEC or 1099-MISC is uploaded, **When** extraction completes, **Then** the system extracts: Payer name, Payer TIN, Recipient name, Recipient SSN/TIN (masked), Nonemployee compensation amount, and Federal tax withheld.

5. **Given** a business form (1120/1065) is uploaded, **When** extraction completes, **Then** the system extracts: Business name, EIN, Fiscal year dates, Total income, Total deductions, Taxable income, and Schedule X reconciliation items (add-backs and deductions).

---

### User Story 3 - Extraction Failure Transparency (Priority: P2)

As a user, when some forms or pages could not be extracted, I want to see a clear explanation of which forms failed and why so that I can take corrective action (re-scan, manually enter, or upload a different document).

**Why this priority**: Transparency about extraction failures prevents user confusion and provides actionable guidance for resolution.

**Independent Test**: Can be tested by uploading a document with poor quality pages or unsupported forms, and verifying that specific failure reasons are displayed.

**Acceptance Scenarios**:

1. **Given** a multi-page PDF is uploaded where some pages are blank or illegible, **When** extraction completes, **Then** the user sees a "Skipped Pages" section listing each skipped page number with the reason (e.g., "Page 3: Blank page detected", "Page 5: Image quality too low for accurate extraction").

2. **Given** a document contains an unsupported form type, **When** extraction completes, **Then** the user sees a message like "Page 2: Unrecognized form type. Consider manual entry or contact support."

3. **Given** a form was partially extracted due to obscured fields, **When** viewing the extraction summary, **Then** the user sees which specific fields could not be extracted and suggestions for resolution (e.g., "Employer EIN: Could not extract - field obscured. Please verify manually.").

4. **Given** extraction failed entirely for a document, **When** the error is displayed, **Then** the user sees a helpful error message with options: "Try uploading a clearer scan", "Split large documents into smaller files", or "Enter data manually".

---

### User Story 4 - Submission with Supporting Documents (Priority: P2)

As a taxpayer submitting my tax return, I want the submitted form to be permanently linked to all supporting documents (uploaded PDFs) so that auditors can always trace extracted values back to original sources, even if I made manual corrections.

**Why this priority**: Ensures audit trail integrity and compliance requirements. Auditors need to verify source documents regardless of whether data was AI-extracted or manually entered.

**Independent Test**: Can be tested by submitting a return with both extracted and manually-corrected fields, then verifying in the auditor view that all supporting documents are accessible.

**Acceptance Scenarios**:

1. **Given** a user has uploaded tax documents and reviewed extracted data, **When** they submit the return to the auditor, **Then** all uploaded PDFs are permanently attached to the submission record.

2. **Given** a user manually corrected an extracted field before submission, **When** an auditor reviews the return, **Then** the auditor can see both the corrected value and the original extracted value with a link to view the source document.

3. **Given** a user entered data manually without uploading a supporting document, **When** they attempt to submit, **Then** the system prompts them to upload supporting documentation or explicitly acknowledge that no documentation is provided.

4. **Given** an auditor is reviewing a submitted return, **When** they click on any data field, **Then** they can view the source PDF page with the extraction location highlighted (if extracted) or see "Manually entered - no source document" (if manual entry).

---

### User Story 5 - Manual Entry Fallback with Document Association (Priority: P3)

As a user who needs to enter data manually (due to extraction failure or missing documents), I want to still associate any available supporting documents with my manual entries so that auditors have context for verification.

**Why this priority**: Supports edge cases where extraction fails but users still have documents that can provide context for auditors.

**Independent Test**: Can be tested by manually entering form data and associating a scanned document, then verifying the association persists through submission.

**Acceptance Scenarios**:

1. **Given** extraction failed for a document, **When** the user switches to manual entry mode, **Then** the system still allows them to attach the original PDF as a supporting document.

2. **Given** a user is manually entering W-2 data, **When** they save the entry, **Then** the system prompts "Would you like to attach a supporting document for this W-2?"

3. **Given** a manual entry has an attached document, **When** the auditor reviews it, **Then** they see "Data entered manually - supporting document attached" with a link to view the document.

---

### Edge Cases

- What happens when a PDF has password protection? System should display a clear message: "This PDF is password-protected. Please remove the password and re-upload."
- What happens when a very large PDF (50+ pages) is uploaded? System should process in chunks and provide progress feedback, with the option to cancel.
- What happens when the same form appears multiple times in a document? System should extract each instance separately and allow user to specify owner (Primary/Spouse).
- What happens when bounding box coordinates are not available for a field? System displays the page number but shows "Exact location not available - field extracted from this page."
- What happens when user tries to submit without reviewing low-confidence fields? System warns and requires acknowledgment: "Some fields have low confidence. Please review before submission."

## Requirements *(mandatory)*

### Functional Requirements

**PDF Preview & Source Visualization:**
- **FR-001**: System MUST display the uploaded PDF in a viewer alongside the extracted data in a split-screen layout.
- **FR-002**: System MUST allow users to click on any extracted field to navigate the PDF viewer to the relevant page.
- **FR-003**: System MUST highlight the bounding box location on the PDF page where each field value was extracted.
- **FR-004**: System MUST display extraction provenance tooltip showing form type, box/line number, page number, and confidence score.
- **FR-005**: System MUST support zooming and panning in the PDF viewer for detailed inspection.

**Extended Field Extraction:**
- **FR-006**: System MUST extract all documented fields for W-2 forms (Boxes 1-20).
- **FR-007**: System MUST extract filing status, income lines, deduction amounts, and tax lines from Federal 1040.
- **FR-008**: System MUST extract property-level details and K-1 entity details from Schedule E.
- **FR-009**: System MUST extract payer information and income amounts from all 1099 variants (NEC, MISC).
- **FR-010**: System MUST extract business identification, income, and Schedule X/Y data from forms 1120, 1065, and Form 27.
- **FR-011**: System MUST provide field-level confidence scores with weight classifications (CRITICAL, HIGH, MEDIUM, LOW).

**Extraction Failure Visibility:**
- **FR-012**: System MUST display a "Skipped Forms/Pages" section when extraction is incomplete.
- **FR-013**: System MUST provide specific failure reasons for each skipped page (blank, illegible, unsupported form type).
- **FR-014**: System MUST identify and display fields that could not be extracted within a successfully identified form.
- **FR-015**: System MUST provide actionable suggestions for resolving extraction failures.

**Auditor Submission with Documents:**
- **FR-016**: System MUST permanently attach all uploaded PDFs to the submission record when a return is submitted.
- **FR-017**: System MUST maintain a link between each extracted field and its source document page.
- **FR-018**: System MUST record when a field was manually corrected and preserve both original extracted value and corrected value.
- **FR-019**: System MUST allow auditors to view source documents and extraction highlights for any submitted return.
- **FR-020**: System MUST prompt users to attach supporting documents for manual entries.

**UI Synchronization:**
- **FR-021**: System MUST update the extraction summary in real-time as extraction progresses.
- **FR-022**: System MUST display confidence indicators visually (color-coded: green/amber/red).
- **FR-023**: System MUST allow inline correction of extracted values with immediate UI update.

### Key Entities

- **ExtractionResult**: Represents the complete extraction output including all forms, field confidences, and provenance data.
- **FieldProvenance**: Tracks where each field was extracted from - page number, bounding box coordinates, raw value, processed value, and confidence score.
- **FormProvenance**: Tracks form-level extraction metadata - form type, page number, overall confidence, and list of field provenances.
- **SkippedForm**: Records forms or pages that could not be extracted with reason and suggestion for resolution.
- **SubmissionDocument**: Links uploaded PDFs to submission records with metadata for auditor access.
- **ManualEntry**: Records manually entered data with optional linked supporting document and flag indicating no AI extraction.
- **AuditTrail**: Records all changes to extracted values including who made the change and when.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can verify any extracted field by viewing its source location in the PDF within 2 clicks.
- **SC-002**: 90% of all fields from supported form types are successfully extracted with confidence scores above 70%.
- **SC-003**: Users can identify why a form or page was not extracted within 5 seconds of viewing the extraction summary.
- **SC-004**: 100% of submitted returns have all supporting documents permanently attached and accessible to auditors.
- **SC-005**: Auditors can trace any data field back to its source document in under 10 seconds.
- **SC-006**: Manual corrections preserve the original extracted value for audit purposes.
- **SC-007**: Users spend 50% less time manually entering data compared to the previous extraction coverage.
- **SC-008**: 95% of users successfully submit returns with all required supporting documents attached.

## Assumptions

- The existing Gemini-based extraction service will be extended rather than replaced.
- Bounding box coordinates can be obtained from the AI model with reasonable accuracy for most fields.
- PDF rendering will use a standard library capable of displaying pages and overlay highlights.
- The current auditor workflow (spec-9) infrastructure will be leveraged for document storage and retrieval.
- All supported form types are already defined in the TaxFormType enum.
