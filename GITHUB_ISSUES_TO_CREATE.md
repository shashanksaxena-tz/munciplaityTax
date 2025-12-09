# GitHub Issues to Create - Execution Order

This document contains all GitHub issues that should be created based on the documentation-implementation disconnect analysis. Issues are organized in execution order: **Backend → UI → Documentation**.

---

## 📋 Quick Summary

**Total Issues:** 15
- **Backend (BE) Issues:** 6 (Issues #1-6)
- **UI Issues:** 5 (Issues #7-11)
- **Documentation Issues:** 4 (Issues #12-15)

**Execution Order:**
1. Fix Backend (Issues #1-6)
2. Fix UI (Issues #7-11)
3. Update Documentation (Issues #12-15)

---

## 🔴 BACKEND ISSUES (Fix First)

### Issue #1: Integrate Rule Service with Tax Calculators

**Title:** `[BE] Integrate rule service with tax calculators - remove hardcoded rates`

**Labels:** `backend`, `critical`, `rule-engine`

**Priority:** 🔴 Critical (Severity 1)

**Description:**

Tax calculators currently use hardcoded rates instead of the rule service.

**Current State:**
```java
// IndividualTaxCalculator.java:45
private static final double DUBLIN_TAX_RATE = 0.025; // HARDCODED!

// BusinessTaxCalculator.java:38
private static final double DUBLIN_TAX_RATE = 0.025; // HARDCODED!
```

**Problem:**
- Rule service exists and stores rules in database
- Tax calculators ignore rule service completely
- Cannot change tax rates without code redeployment
- Architectural disconnect between services

**Requirements:**
1. Create `RuleServiceClient` in tax-engine-service
2. Fetch active rules on calculation requests
3. Use rule values instead of hardcoded constants
4. Add caching for rule lookups (Redis)
5. Add fallback to default rates if rule service unavailable
6. Add logging for rule applications

**Files to Modify:**
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/IndividualTaxCalculator.java`
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/BusinessTaxCalculator.java`
- `backend/tax-engine-service/pom.xml` (add rule-service dependency)

**Acceptance Criteria:**
- [ ] Tax calculations use rates from rule service
- [ ] Hardcoded rates removed
- [ ] Rules cached for performance
- [ ] Audit log shows which rules were applied
- [ ] Fallback mechanism works if rule service down
- [ ] Tests pass with dynamic rules

**Estimated Effort:** 8 hours

**Reference:** `DOCUMENTATION_IMPLEMENTATION_DISCONNECT_ANALYSIS.md` Section 9.1

---

### Issue #2: Implement Mock Payment Backend Service

**Title:** `[BE] Implement mock payment processing service`

**Labels:** `backend`, `critical`, `payment`

**Priority:** 🔴 Critical (Severity 1)

**Description:**

Payment processing is documented but not implemented. Users cannot pay tax liabilities.

**Current State:**
- Payment flow shown in sequence diagrams
- No payment processing endpoint
- No payment confirmation logic
- No receipt generation

**Requirements:**
1. Create mock payment provider service (like Stripe test mode)
2. Implement payment processing endpoints:
   - `POST /api/v1/payments` - Process payment
   - `GET /api/v1/payments/{id}` - Get payment status
   - `POST /api/v1/payments/{id}/confirm` - Confirm payment
3. Mock card testing:
   - `4242 4242 4242 4242` - Always succeeds
   - `4000 0000 0000 0002` - Always fails
4. Generate realistic transaction IDs
5. Post to ledger on successful payment
6. Generate payment confirmation
7. Store payment records with audit trail

**Mock Service Features:**
- Instant success/failure (no real processing delays)
- Configurable failure scenarios for testing
- Transaction history storage
- Payment method validation

**Files to Create:**
- `backend/payment-service/` (new microservice)
- `backend/payment-service/src/main/java/com/munitax/payment/service/MockPaymentProvider.java`
- `backend/payment-service/src/main/java/com/munitax/payment/controller/PaymentController.java`
- `backend/payment-service/src/main/java/com/munitax/payment/model/Payment.java`

**Integration:**
- Connect to ledger service for posting
- Emit events for successful/failed payments
- Update submission status on payment

**Acceptance Criteria:**
- [ ] Mock payment endpoint accepts payment requests
- [ ] Test cards work as expected (success/failure)
- [ ] Transaction IDs generated
- [ ] Payments recorded in database
- [ ] Ledger entries created automatically
- [ ] Payment confirmation returned
- [ ] Audit trail complete

**Estimated Effort:** 12 hours

**Reference:** `DOCUMENTATION_IMPLEMENTATION_DISCONNECT_ANALYSIS.md` Section 9.3

---

### Issue #3: Complete Schedule X with Missing 21 Fields

**Title:** `[BE] Complete Schedule X business tax reconciliation - add 21 missing fields`

**Labels:** `backend`, `critical`, `business-tax`, `schedule-x`

**Priority:** 🔴 Critical (Severity 1)

**Description:**

Schedule X (book-tax reconciliation) only has 6 of 27 documented fields (22% complete).

**Current State:**
Only 6 fields implemented:
1. Depreciation adjustment
2. Officer compensation
3. Guaranteed payments
4. Municipal tax deduction
5. Section 179 excess
6. Other adjustments

**Missing 21 Fields:**

**Tax-Exempt Income:**
7. Municipal bond interest
8. Life insurance proceeds
9. Tax-exempt dividends

**Non-Deductible Expenses:**
10. Meals & entertainment (50% rule)
11. Penalties and fines
12. Political contributions
13. Club dues
14. Life insurance premiums

**Depreciation Reconciliation:**
15. MACRS vs GAAP depreciation difference
16. Bonus depreciation adjustments
17. Section 168(k) adjustments

**Amortization:**
18. Goodwill amortization
19. Start-up costs amortization
20. Organization costs

**Related-Party Transactions:**
21. Related-party interest
22. Related-party rent

**Other Adjustments:**
23. Bad debt reserve changes
24. Inventory accounting method changes
25. Pension/profit-sharing limits
26. State tax add-backs
27. Domestic production activities deduction

**Requirements:**
1. Add 21 new fields to `ScheduleX` entity
2. Update `ScheduleXCalculationService` with new logic
3. Add validation rules for each field
4. Update business tax calculator to use new fields
5. Add field-specific help text
6. Create migration script for existing data

**Files to Modify:**
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/ScheduleX.java`
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/ScheduleXCalculationService.java`
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/BusinessTaxCalculator.java`

**Acceptance Criteria:**
- [ ] All 27 fields available in ScheduleX entity
- [ ] Calculation logic complete for each field
- [ ] Validation rules implemented
- [ ] Database migration successful
- [ ] Tests cover all new fields
- [ ] Documentation updated

**Estimated Effort:** 16 hours

**Reference:** `DOCUMENTATION_IMPLEMENTATION_DISCONNECT_ANALYSIS.md` Section 2.2

---

### Issue #4: Implement Withholding Reconciliation Logic

**Title:** `[BE] Implement withholding reconciliation logic for W-1 filings`

**Labels:** `backend`, `critical`, `withholding`

**Priority:** 🔴 Critical (Severity 1)

**Description:**

`reconcilePayroll()` function is a stub that returns empty array. No actual reconciliation logic exists.

**Current State:**
```typescript
// WithholdingWizard.tsx
const reconcilePayroll = (): ReconciliationIssue[] => {
  return []; // TODO: Implement actual reconciliation logic
};
```

**Requirements:**

1. **Backend Reconciliation Service** (`backend/tax-engine-service/`):
   - Create `WithholdingReconciliationService.java`
   - Implement reconciliation logic:
     - Compare W-1 filings to W-2 reported withholding
     - Check cumulative totals match year-to-date
     - Verify withholding rates are correct
     - Detect duplicate filings
     - Flag late filings
   - Return list of reconciliation issues

2. **Reconciliation Checks:**
   - W-1 wages match W-2 Box 1 (federal wages)
   - W-1 local wages match W-2 Box 18 (local wages)
   - Withholding rate within 0-3.0% range
   - Quarterly totals match cumulative
   - All required periods filed
   - No duplicate EIN filings for same period

3. **Database Tracking:**
   - Store cumulative W-1 totals
   - Track filing history per employer
   - Flag reconciliation issues
   - Store resolution status

4. **API Endpoint:**
   - `POST /api/v1/w1-filings/reconcile` - Run reconciliation
   - `GET /api/v1/w1-filings/reconciliation/{employerId}` - Get issues

**Files to Create:**
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/WithholdingReconciliationService.java`
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/ReconciliationIssue.java`
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/CumulativeWithholdingRepository.java`

**Files to Modify:**
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/W1FilingController.java`

**Acceptance Criteria:**
- [ ] Reconciliation service compares W-1 to W-2 data
- [ ] All 6 reconciliation checks implemented
- [ ] Issues detected and categorized (HIGH/MEDIUM/LOW)
- [ ] Cumulative tracking works across periods
- [ ] API endpoint returns reconciliation results
- [ ] Tests cover all reconciliation scenarios

**Estimated Effort:** 10 hours

**Reference:** `DOCUMENTATION_IMPLEMENTATION_DISCONNECT_ANALYSIS.md` Section 9.4

---

### Issue #5: Add Document Attachment to Tax Submissions

**Title:** `[BE] Add document attachment support to tax submissions for auditor review`

**Labels:** `backend`, `enhancement`, `auditor`, `documents`

**Priority:** 🟡 High

**Description:**

Submitted tax returns should include attached documents that flow through to auditor review.

**Current State:**
- Documents uploaded during extraction
- Documents not attached to submission
- Auditor cannot see original uploaded documents
- No document tracking in submission workflow

**Requirements:**

1. **Document Storage:**
   - Store uploaded PDFs with submission
   - Link documents to specific forms (W-2, 1099, etc.)
   - Maintain document metadata (filename, upload date, size)
   - Store extraction results with documents

2. **Submission Enhancement:**
   - Add `documents` field to submission entity
   - Store document IDs with submission
   - Include document count in submission summary

3. **Auditor Access:**
   - Return documents with submission details
   - Allow auditor to view original PDFs
   - Show which document each field was extracted from
   - Enable document download

4. **API Updates:**
   - `POST /api/v1/submissions` - Accept document IDs
   - `GET /api/v1/submissions/{id}/documents` - Get submission documents
   - `GET /api/v1/submissions/{id}/documents/{docId}` - Download document

**Files to Modify:**
- `backend/submission-service/src/main/java/com/munitax/submission/model/Submission.java`
- `backend/submission-service/src/main/java/com/munitax/submission/controller/SubmissionController.java`
- `backend/submission-service/src/main/java/com/munitax/submission/service/SubmissionService.java`

**Files to Create:**
- `backend/submission-service/src/main/java/com/munitax/submission/model/SubmissionDocument.java`
- `backend/submission-service/src/main/java/com/munitax/submission/repository/SubmissionDocumentRepository.java`

**Database Changes:**
- New table: `submission_documents`
- Columns: id, submission_id, document_name, file_path, upload_date, file_size, document_type

**Acceptance Criteria:**
- [ ] Documents linked to submissions
- [ ] Document metadata stored
- [ ] Auditor can view documents via API
- [ ] Document download works
- [ ] Extraction provenance maintained
- [ ] Migration script for existing submissions

**Estimated Effort:** 8 hours

**Reference:** Comment from @shashanksaxena-tz - "documents should be part of the return till it reaches auditor for review"

---

### Issue #6: Complete W-3 Year-End Reconciliation Backend

**Title:** `[BE] Implement W-3 year-end reconciliation logic`

**Labels:** `backend`, `enhancement`, `withholding`, `w-3`

**Priority:** 🟡 High

**Description:**

W-3 reconciliation is marked as "IN PROGRESS" but no backend logic exists.

**Current State:**
- `ReconciliationWizard.tsx` component exists (UI shell)
- No backend endpoint for W-3 reconciliation
- No W-3 entity or service
- No reconciliation logic

**Requirements:**

1. **W-3 Entity and Service:**
   - Create `W3Reconciliation` entity
   - Store year-end totals
   - Link to all W-1 filings for the year
   - Track reconciliation status

2. **Reconciliation Logic:**
   - Sum all W-1 filings for the year
   - Compare to W-2 totals reported
   - Identify discrepancies
   - Calculate penalties for late/missing filings
   - Generate W-3 form

3. **API Endpoints:**
   - `POST /api/v1/w3-reconciliation` - Create W-3
   - `GET /api/v1/w3-reconciliation/{year}` - Get reconciliation
   - `POST /api/v1/w3-reconciliation/{id}/submit` - Submit W-3
   - `GET /api/v1/w3-reconciliation/{id}/discrepancies` - Get issues

**Files to Create:**
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/W3Reconciliation.java`
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/W3ReconciliationService.java`
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/W3ReconciliationController.java`
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/W3ReconciliationRepository.java`

**Acceptance Criteria:**
- [ ] W-3 entity created
- [ ] Year-end reconciliation logic complete
- [ ] W-1 to W-2 comparison works
- [ ] Discrepancies identified
- [ ] W-3 form generation works
- [ ] API endpoints functional
- [ ] Tests cover reconciliation scenarios

**Estimated Effort:** 12 hours

**Reference:** `DOCUMENTATION_IMPLEMENTATION_DISCONNECT_ANALYSIS.md` Section 2.2

---

## 🎨 UI ISSUES (Fix After Backend)

### Issue #7: Implement Payment UI with Mock Provider Integration

**Title:** `[UI] Implement payment UI for tax liability payments`

**Labels:** `ui`, `critical`, `payment`

**Priority:** 🔴 Critical (Severity 1)

**Description:**

Users cannot pay tax liabilities. Payment UI is missing.

**Current State:**
- Tax calculation shows amount due
- No payment button or form
- No payment confirmation screen

**Requirements:**

1. **Payment Method Selector:**
   - Credit card payment form
   - ACH/bank transfer form
   - Payment plan option

2. **Payment Form Components:**
   - Create `PaymentMethodSelector.tsx`
   - Create `CreditCardForm.tsx`
   - Create `BankAccountForm.tsx`
   - Create `PaymentConfirmation.tsx`
   - Create `PaymentReceipt.tsx`

3. **Payment Flow:**
   - Show amount due after calculation
   - "Pay Now" button
   - Select payment method
   - Enter payment details
   - Confirm payment
   - Show confirmation with transaction ID
   - Generate/download receipt

4. **Mock Card Testing UI:**
   - Show test card numbers
   - "Use Test Card" quick buttons
   - Clear indication this is mock/test mode

5. **Integration:**
   - Call payment service API
   - Handle success/failure
   - Show appropriate messages
   - Update submission status

**Files to Create:**
- `components/payment/PaymentMethodSelector.tsx`
- `components/payment/CreditCardForm.tsx`
- `components/payment/BankAccountForm.tsx`
- `components/payment/PaymentConfirmation.tsx`
- `components/payment/PaymentReceipt.tsx`

**Files to Modify:**
- `components/ResultsSection.tsx` (add payment button)
- `TaxFilingApp.tsx` (add payment step)
- `types.ts` (add payment types)

**Acceptance Criteria:**
- [ ] Payment button visible after calculation
- [ ] Payment form accepts card details
- [ ] Mock payment processing works
- [ ] Success/failure handled correctly
- [ ] Confirmation screen shows transaction ID
- [ ] Receipt can be downloaded
- [ ] Error messages are clear

**Estimated Effort:** 10 hours

**Dependencies:** Issue #2 (Backend payment service)

**Reference:** `DOCUMENTATION_IMPLEMENTATION_DISCONNECT_ANALYSIS.md` Section 5.1

---

### Issue #8: Complete Schedule X UI with 21 Missing Fields

**Title:** `[UI] Add 21 missing fields to Schedule X business tax reconciliation UI`

**Labels:** `ui`, `critical`, `business-tax`, `schedule-x`

**Priority:** 🔴 Critical (Severity 1)

**Description:**

Schedule X UI only shows 6 of 27 fields. Need to add 21 missing fields.

**Current State:**
- `NetProfitsWizard.tsx` only shows 6 basic fields
- Missing 78% of required reconciliation fields
- No field-specific help text
- No validation for new fields

**Requirements:**

1. **Organize Fields by Category:**
   - Tax-Exempt Income (3 fields)
   - Non-Deductible Expenses (7 fields)
   - Depreciation Reconciliation (3 fields)
   - Amortization (3 fields)
   - Related-Party Transactions (2 fields)
   - Other Adjustments (6 fields)

2. **UI Enhancements:**
   - Collapsible sections for each category
   - Help tooltips for each field
   - Field validation
   - Running total calculation
   - Clear labeling and descriptions

3. **Form Components:**
   - Structured form layout
   - Input validation
   - Error messages
   - Save draft functionality
   - Progress indicator (e.g., "15 of 27 fields complete")

**Files to Modify:**
- `components/NetProfitsWizard.tsx`
- `types.ts` (add new ScheduleX field types)

**Acceptance Criteria:**
- [ ] All 27 fields visible in UI
- [ ] Fields organized by category
- [ ] Help tooltips for each field
- [ ] Validation works for all fields
- [ ] Running total updates as fields change
- [ ] Form can be saved as draft
- [ ] Clear instructions for each section

**Estimated Effort:** 12 hours

**Dependencies:** Issue #3 (Backend Schedule X completion)

**Reference:** `DOCUMENTATION_IMPLEMENTATION_DISCONNECT_ANALYSIS.md` Section 4.1.3

---

### Issue #9: Implement Withholding Reconciliation UI

**Title:** `[UI] Implement withholding reconciliation UI for W-1 filings`

**Labels:** `ui`, `critical`, `withholding`

**Priority:** 🔴 Critical (Severity 1)

**Description:**

`reconcilePayroll()` is a stub. Need UI to display reconciliation results from backend.

**Current State:**
```typescript
const reconcilePayroll = (): ReconciliationIssue[] => {
  return []; // Stub
};
```

**Requirements:**

1. **Reconciliation Display:**
   - Show all reconciliation issues
   - Categorize by severity (HIGH/MEDIUM/LOW)
   - Color-code issues (red/yellow/green)
   - Show resolution status

2. **Issue Details:**
   - Issue description
   - Affected period
   - Expected vs actual values
   - Recommended action
   - Resolution options

3. **Period History View:**
   - Table showing all W-1 filings for the year
   - Cumulative totals
   - Status indicators
   - Quick navigation between periods

4. **Components to Create:**
   - `ReconciliationIssuesList.tsx`
   - `PeriodHistoryTable.tsx`
   - `CumulativeTotalsPanel.tsx`
   - `ReconciliationStatusBadge.tsx`

**Files to Modify:**
- `components/WithholdingWizard.tsx`
- `types.ts` (add ReconciliationIssue type)

**API Integration:**
- Call `/api/v1/w1-filings/reconcile`
- Display returned issues
- Allow issue acknowledgment/resolution

**Acceptance Criteria:**
- [ ] Reconciliation issues displayed
- [ ] Issues categorized by severity
- [ ] Period history visible
- [ ] Cumulative totals shown
- [ ] Users can view issue details
- [ ] Clear action items for each issue
- [ ] Status updates when issues resolved

**Estimated Effort:** 8 hours

**Dependencies:** Issue #4 (Backend reconciliation logic)

**Reference:** `DOCUMENTATION_IMPLEMENTATION_DISCONNECT_ANALYSIS.md` Section 4.1.2

---

### Issue #10: Add Document Display to Auditor Review Panel

**Title:** `[UI] Add document viewing to auditor review panel`

**Labels:** `ui`, `enhancement`, `auditor`, `documents`

**Priority:** 🟡 High

**Description:**

Auditors cannot view documents attached to submissions.

**Current State:**
- `ReturnReviewPanel.tsx` shows submission data
- No document list or viewer
- Documents uploaded during extraction not visible

**Requirements:**

1. **Document List:**
   - Show all documents attached to submission
   - Document names and types
   - File sizes and upload dates
   - Extraction status

2. **Document Viewer:**
   - Click to view PDF inline
   - Highlight extracted regions (bounding boxes)
   - Show which fields came from which document
   - Download document option

3. **Split-Screen Layout (Future):**
   - PDF on left, data on right
   - Click field to highlight source in PDF
   - Click PDF region to show extracted field

4. **Components to Create:**
   - `SubmissionDocumentsList.tsx`
   - `DocumentViewer.tsx` (using existing PdfViewer)
   - `ExtractionProvenanceDisplay.tsx`

**Files to Modify:**
- `components/ReturnReviewPanel.tsx`
- `types.ts` (add document types)

**API Integration:**
- Call `/api/v1/submissions/{id}/documents`
- Download documents via `/api/v1/submissions/{id}/documents/{docId}`

**Acceptance Criteria:**
- [ ] Document list visible in review panel
- [ ] Documents can be viewed inline
- [ ] Extraction provenance shown
- [ ] Documents can be downloaded
- [ ] Bounding boxes displayed (if available)
- [ ] Clear visual connection between data and source

**Estimated Effort:** 10 hours

**Dependencies:** Issue #5 (Backend document attachment)

**Reference:** Comment from @shashanksaxena-tz - "documents should be part of the return till it reaches auditor for review"

---

### Issue #11: Implement W-3 Reconciliation UI

**Title:** `[UI] Implement W-3 year-end reconciliation UI`

**Labels:** `ui`, `enhancement`, `withholding`, `w-3`

**Priority:** 🟡 High

**Description:**

`ReconciliationWizard.tsx` is a UI shell with no functionality.

**Current State:**
- Component exists but not connected to backend
- No data display
- No submission workflow

**Requirements:**

1. **Reconciliation Summary:**
   - Year-end totals from all W-1 filings
   - W-2 totals for comparison
   - Discrepancies highlighted
   - Reconciliation status

2. **W-3 Form:**
   - Pre-filled from W-1 data
   - Editable fields for corrections
   - Validation
   - E-signature area

3. **Discrepancy Resolution:**
   - List of discrepancies
   - Explanation fields
   - Attach supporting documents
   - Track resolution status

4. **Submission Flow:**
   - Review reconciliation
   - Resolve discrepancies
   - Generate W-3 form
   - Submit to municipality

**Files to Modify:**
- `components/ReconciliationWizard.tsx`
- `types.ts` (add W-3 types)

**API Integration:**
- `GET /api/v1/w3-reconciliation/{year}`
- `POST /api/v1/w3-reconciliation/{id}/submit`

**Acceptance Criteria:**
- [ ] W-3 reconciliation displays correctly
- [ ] Discrepancies are highlighted
- [ ] W-3 form can be generated
- [ ] Users can resolve discrepancies
- [ ] Submission workflow works
- [ ] Confirmation displayed after submission

**Estimated Effort:** 10 hours

**Dependencies:** Issue #6 (Backend W-3 logic)

**Reference:** `DOCUMENTATION_IMPLEMENTATION_DISCONNECT_ANALYSIS.md` Section 2.2

---

## 📚 DOCUMENTATION ISSUES (Fix Last)

### Issue #12: Update Feature Status in Documentation

**Title:** `[Docs] Update feature statuses to reflect actual implementation`

**Labels:** `documentation`, `critical`

**Priority:** 🔴 Critical

**Description:**

Multiple features marked as ✅ IMPLEMENTED when they are incomplete.

**Files to Update:**
- `/docs/FEATURES_LIST.md`
- `/docs/README.md`

**Required Changes:**

| Line | Current | Correct | Reason |
|------|---------|---------|--------|
| ~48 | Schedule X: ✅ 27-field | ⚠️ 6-field (22% complete) | Only 6 fields implemented |
| ~197 | Dynamic Rule Loading: ✅ | ❌ NOT WORKING | Rules not applied |
| ~50 | W-3 Reconciliation: 🚧 | ❌ NOT STARTED | No code exists |
| ~154 | Split-Screen Review: 🚧 | ❌ NOT STARTED | No code exists |
| ~155 | Taxpayer History: 🚧 | ❌ NOT STARTED | No code exists |
| ~156 | Document Requests: ✅ | ⚠️ BACKEND ONLY | No UI component |
| ~238 | Receipt Generation: 🚧 | ❌ NOT STARTED | Not implemented |

**Acceptance Criteria:**
- [ ] All feature statuses accurate
- [ ] Incomplete features marked clearly
- [ ] Percentage complete shown where applicable
- [ ] Known limitations documented

**Estimated Effort:** 30 minutes

**Reference:** `DOCUMENTATION_REVIEW_ACTION_ITEMS.md` Section 1

---

### Issue #13: Add Critical Warnings to Rule Engine Documentation

**Title:** `[Docs] Add critical warning about rule engine disconnect`

**Labels:** `documentation`, `critical`

**Priority:** 🔴 Critical

**Description:**

Rule engine documented as working but tax calculators don't use it.

**Files to Update:**
- `/docs/MODULES_LIST.md` (line ~405)
- `/docs/ARCHITECTURE.md`
- `/docs/RULE_ENGINE.md`

**Required Warning:**

```markdown
> 🔴 **CRITICAL ISSUE:** The Rule Service is NOT integrated with tax calculators.
> While rules can be created, approved, and stored in the database, they are
> **never applied** during tax calculations. Tax rates and rules are hardcoded
> in `IndividualTaxCalculator.java` and `BusinessTaxCalculator.java`.
> 
> **Status:** Architectural disconnect - Rule service exists but is unused.
> 
> **Resolution:** See Issue #1 for integration work.
```

**Placement:**
- Top of each file, immediately after overview
- Prominent callout box with red background
- Link to Issue #1 for tracking

**Acceptance Criteria:**
- [ ] Warning added to all 3 files
- [ ] Warning prominently displayed
- [ ] Link to tracking issue included
- [ ] Code locations referenced

**Estimated Effort:** 15 minutes

**Reference:** `DOCUMENTATION_REVIEW_ACTION_ITEMS.md` Section 2

---

### Issue #14: Fix Service Port Numbers in Architecture Docs

**Title:** `[Docs] Correct service port numbers in architecture documentation`

**Labels:** `documentation`, `bug`

**Priority:** 🟡 High

**Description:**

Port numbers for Extraction and Submission services are incorrect.

**Files to Update:**
- `/docs/ARCHITECTURE.md`
- `/docs/MODULES_LIST.md`

**Corrections:**

| Service | Documented | Actual | Fix |
|---------|-----------|--------|-----|
| Extraction Service | 8083 | 8084 | Update to 8084 |
| Submission Service | 8084 | 8082 | Update to 8082 |

**Verification:**
- Check `backend/extraction-service/src/main/resources/application.yml`
- Check `backend/submission-service/src/main/resources/application.yml`

**Acceptance Criteria:**
- [ ] Port numbers corrected in both files
- [ ] Architecture diagram updated
- [ ] Port table accurate
- [ ] No other port mismatches

**Estimated Effort:** 10 minutes

**Reference:** `DOCUMENTATION_REVIEW_ACTION_ITEMS.md` Section 3

---

### Issue #15: Document Unused APIs and System Limitations

**Title:** `[Docs] Create API status document and add workflow limitations`

**Labels:** `documentation`, `enhancement`

**Priority:** 🟡 High

**Description:**

38% of APIs are unused. Workflow limitations not documented.

**New Files to Create:**

1. **`/docs/API_STATUS.md`** - API inventory
   - Working and used APIs
   - Working but unused APIs (13 endpoints)
   - Partially implemented APIs
   - Missing APIs

2. **Add "Known Limitations" sections to:**
   - `/docs/SEQUENCE_DIAGRAMS.md`
   - `/docs/DATA_FLOW.md`

**Content for Limitations:**

Example for Individual Tax Filing:
```markdown
### Known Limitations

⚠️ **Payment Processing Not Implemented**
- Steps 26-30 (Payment → Confirmation → Receipt) documented but not implemented
- Workaround: Manual payment processing required
- Tracking: Issue #2, #7

⚠️ **Auto-Save Not Implemented**
- Sessions only saved on explicit user action
- Users may lose data if browser closes
```

**Unused APIs to Document:**
```
/api/v1/audit/request-docs    # Document request (Issue #10)
/api/v1/audit/trail/{id}      # Audit trail display
/api/v1/w1-filings            # W-1 filing list (Issue #9)
/api/v1/ledger/reconciliation # Reconciliation report
[... 9 more]
```

**Acceptance Criteria:**
- [ ] API_STATUS.md created with complete inventory
- [ ] Unused APIs listed with tracking issues
- [ ] Known limitations added to workflow docs
- [ ] Each limitation links to tracking issue
- [ ] Clear workarounds documented where available

**Estimated Effort:** 2 hours

**Reference:** `DOCUMENTATION_REVIEW_ACTION_ITEMS.md` Sections 4-6

---

## 📊 Summary Dashboard

### By Priority

| Priority | Backend | UI | Docs | Total |
|----------|---------|-----|------|-------|
| 🔴 Critical | 4 | 3 | 2 | 9 |
| 🟡 High | 2 | 2 | 2 | 6 |
| **Total** | **6** | **5** | **4** | **15** |

### By Estimated Effort

| Category | Issues | Total Hours |
|----------|--------|-------------|
| Backend | 6 | 66 hours |
| UI | 5 | 50 hours |
| Documentation | 4 | 3.75 hours |
| **Total** | **15** | **119.75 hours** |

### Execution Timeline

**Phase 1: Critical Backend (2-3 weeks)**
- Issues #1-4: 46 hours
- Focus: Rule engine, payments, Schedule X, reconciliation

**Phase 2: Backend Enhancements (1-2 weeks)**
- Issues #5-6: 20 hours
- Focus: Documents, W-3

**Phase 3: Critical UI (2 weeks)**
- Issues #7-9: 30 hours
- Focus: Payment UI, Schedule X UI, reconciliation UI

**Phase 4: UI Enhancements (1-2 weeks)**
- Issues #10-11: 20 hours
- Focus: Document viewing, W-3 UI

**Phase 5: Documentation (1-2 days)**
- Issues #12-15: 3.75 hours
- Focus: Update all documentation

---

## 🚀 Getting Started

### For Project Manager:
1. Review all 15 issues
2. Adjust priorities based on business needs
3. Create issues in GitHub (copy from this document)
4. Assign to developers
5. Set up project board with phases

### For Developers:
1. Start with Issue #1 (Rule Engine)
2. Work through backend issues sequentially
3. Move to UI issues after backend complete
4. Update documentation as final step

### Issue Creation Command:

```bash
# Example for creating Issue #1
gh issue create \
  --title "[BE] Integrate rule service with tax calculators - remove hardcoded rates" \
  --body "$(cat issue-1-body.md)" \
  --label "backend,critical,rule-engine" \
  --assignee "developer-username"
```

---

**Created:** December 9, 2025  
**Based on:** DOCUMENTATION_IMPLEMENTATION_DISCONNECT_ANALYSIS.md  
**Total Issues:** 15  
**Estimated Total Effort:** ~120 hours (3 weeks for 2 developers)

**Note:** Issue descriptions include all necessary context. Each can be copied directly into GitHub.
