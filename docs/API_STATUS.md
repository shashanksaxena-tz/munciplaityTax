# MuniTax API Status Document

## Overview

This document provides a comprehensive inventory of all APIs in the MuniTax system, categorizing them by usage status and implementation completeness. This helps identify areas for optimization, technical debt reduction, and future development priorities.

**Last Updated:** December 10, 2025  
**Total APIs:** 100+  
**Active/Used:** 62%  
**Unused:** 38%

---

## API Categories

### 1. Working and Actively Used APIs

These APIs are fully implemented, tested, and actively used by the frontend application.

#### Authentication & User Management
- `POST /api/v1/auth/login` - User authentication
- `GET /api/v1/auth/me` - Get current user info
- `POST /api/v1/auth/validate` - Validate JWT token
- **Status:** ✅ Fully functional and used
- **Frontend Usage:** `services/api.ts` auth module

#### Tax Calculation
- `POST /api/v1/tax-engine/calculate/individual` - Individual tax calculation
- `POST /api/v1/tax-engine/calculate/business` - Business tax calculation
- **Status:** ✅ Fully functional and used
- **Frontend Usage:** `services/api.ts` taxEngine module
- **Notes:** Core tax calculation engine with support for W-2, 1099, Schedule C/E/F, Schedule X/Y

#### Document Extraction
- `POST /api/v1/extraction/extract` - Single document extraction with SSE streaming
- `POST /api/v1/extraction/extract/batch` - Batch document extraction
- **Status:** ✅ Fully functional and used
- **Frontend Usage:** `services/api.ts` extraction module
- **Notes:** Uses Google Gemini AI for intelligent form recognition and data extraction

#### PDF Generation
- `POST /api/v1/pdf/generate/tax-return` - Generate tax return PDF
- **Status:** ✅ Fully functional and used
- **Frontend Usage:** `services/api.ts` pdf module
- **Notes:** Uses PDFBox to generate Dublin 1040 forms

#### Submissions
- `POST /api/v1/submissions` - Submit tax return
- **Status:** ✅ Fully functional and used
- **Frontend Usage:** `services/api.ts` submission module

#### W-3 Reconciliation
- `POST /api/v1/w3-reconciliation` - Create W-3 reconciliation
- `GET /api/v1/w3-reconciliation/{year}` - Get reconciliation by year
- `POST /api/v1/w3-reconciliation/{id}/submit` - Submit W-3 reconciliation
- `GET /api/v1/w3-reconciliation/{id}/discrepancies` - Get discrepancies
- **Status:** ✅ Fully functional and used
- **Frontend Usage:** `services/api.ts` w3Reconciliation module

#### Rules Management (Read Operations)
- `GET /api/rules/active` - Get active tax rules
- `GET /api/rules/as-of` - Get rules as of specific date
- **Status:** ✅ Fully functional and used
- **Frontend Usage:** `services/ruleService.ts`

#### Session Management
- `POST /api/v1/sessions` - Create session
- `GET /api/v1/sessions` - List sessions
- `GET /api/v1/sessions/{sessionId}` - Get session details
- `PUT /api/v1/sessions/{sessionId}` - Update session
- `DELETE /api/v1/sessions/{sessionId}` - Delete session
- **Status:** ✅ Fully functional and used
- **Frontend Usage:** `services/sessionService.ts`

---

### 2. Working but Unused APIs (38%)

These APIs are fully implemented and functional but not currently used by the frontend application. They represent potential for feature expansion or technical debt that should be evaluated.

#### Audit Workflow (Partially Used)

**Used:**
- `GET /api/v1/audit/queue` - Get audit queue
- `POST /api/v1/audit/assign` - Assign auditor
- `POST /api/v1/audit/approve` - Approve return
- `POST /api/v1/audit/reject` - Reject return

**Unused:**
- ⚠️ `POST /api/v1/audit/request-docs` - Request additional documents
  - **Tracking Issue:** [shashanksaxena-tz/munciplaityTax#XX](https://github.com/shashanksaxena-tz/munciplaityTax/issues/XX)
  - **Impact:** Auditors cannot request additional documentation from taxpayers
  - **Recommendation:** Implement document request UI in auditor workflow
  
- ⚠️ `GET /api/v1/audit/trail/{returnId}` - View audit trail
  - **Tracking Issue:** [shashanksaxena-tz/munciplaityTax#XX](https://github.com/shashanksaxena-tz/munciplaityTax/issues/XX)
  - **Impact:** No visibility into audit history and actions taken
  - **Recommendation:** Add audit trail viewer to auditor dashboard
  
- ⚠️ `GET /api/v1/audit/document-requests/{returnId}` - Get document requests
- ⚠️ `POST /api/v1/audit/document-requests/{requestId}/received` - Mark documents received
- ⚠️ `GET /api/v1/audit/document-requests/overdue` - Get overdue requests
- ⚠️ `GET /api/v1/audit/actions/{returnId}` - Get audit actions
- ⚠️ `POST /api/v1/audit/report/generate/{returnId}` - Generate audit report
- ⚠️ `GET /api/v1/audit/report/{returnId}` - Get audit report
- ⚠️ `GET /api/v1/audit/reports/high-risk` - Get high-risk reports
- ⚠️ `GET /api/v1/audit/workload/{auditorId}` - Get auditor workload
- ⚠️ `POST /api/v1/audit/start-review` - Start audit review
- ⚠️ `POST /api/v1/audit/priority` - Update priority

#### Withholding Tax (W-1 Forms)
- ⚠️ `POST /api/v1/w1-filings` - Create W-1 filing
  - **Tracking Issue:** [shashanksaxena-tz/munciplaityTax#XX](https://github.com/shashanksaxena-tz/munciplaityTax/issues/XX)
  - **Impact:** No monthly withholding tax filing capability
  - **Recommendation:** Implement W-1 filing workflow for employers
  
- ⚠️ `POST /api/v1/w1-filings/reconcile` - Reconcile W-1 filings
- ⚠️ `GET /api/v1/w1-filings/reconciliation/{employerId}` - Get W-1 reconciliation

#### Ledger & Accounting
- ⚠️ `GET /api/v1/reconciliation/report/{tenantId}/{municipalityId}` - Reconciliation report
  - **Tracking Issue:** [shashanksaxena-tz/munciplaityTax#XX](https://github.com/shashanksaxena-tz/munciplaityTax/issues/XX)
  - **Impact:** Municipality cannot view reconciliation reports
  - **Recommendation:** Add municipality dashboard with reconciliation reports
  
- ⚠️ `GET /api/v1/reconciliation/{tenantId}/{municipalityId}/filer/{filerId}` - Filer reconciliation
- ⚠️ `GET /api/v1/trial-balance` - Get trial balance
- ⚠️ `GET /api/v1/trial-balance/period` - Get trial balance for period
- ⚠️ `GET /api/v1/statements/filer/{tenantId}/{filerId}` - Account statement
- ⚠️ `GET /api/v1/statements/filer/{tenantId}/{filerId}/pdf` - Account statement PDF
- ⚠️ `GET /api/v1/statements/filer/{tenantId}/{filerId}/csv` - Account statement CSV
- ⚠️ `POST /api/v1/journal-entries` - Create journal entry
- ⚠️ `POST /api/v1/journal-entries/{entryId}/reverse` - Reverse journal entry
- ⚠️ `GET /api/v1/journal-entries/entity/{tenantId}/{entityId}` - Get entity journal entries
- ⚠️ `GET /api/v1/journal-entries/{entryId}` - Get journal entry
- ⚠️ `GET /api/v1/audit/entity/{entityId}` - Ledger audit trail (entity)
- ⚠️ `GET /api/v1/audit/tenant/{tenantId}` - Ledger audit trail (tenant)
- ⚠️ `GET /api/v1/audit/journal-entries/{entryId}` - Ledger audit trail (entry)
- ⚠️ `GET /api/v1/audit/filtered` - Filtered ledger audit trail

#### Payment Processing
- ⚠️ `GET /api/v1/payments/filer/{filerId}` - Get filer payments (partially used)
  - **Note:** Payment endpoints exist but full workflow not implemented
  - **See:** Known Limitations section for details
  
- ⚠️ `GET /api/v1/payments/{paymentId}` - Get payment details
- ⚠️ `GET /api/v1/payments/{paymentId}/receipt` - Payment receipt
- ⚠️ `POST /api/v1/payments/{id}/confirm` - Confirm payment
- ⚠️ `GET /api/v1/payments/test-mode-indicator` - Test mode indicator
- ⚠️ `GET /api/v1/payments/test-methods` - Test payment methods

#### Refund Processing
- ⚠️ `POST /api/v1/refunds/request` - Request refund
- ⚠️ `POST /api/v1/refunds/issue` - Issue refund

#### Tax Assessment
- ⚠️ `POST /api/v1/tax-assessments/record` - Record tax assessment
- ⚠️ `POST /api/v1/tax-assessments` - Create tax assessment

#### Advanced Tax Features

**Schedule X & Y:**
- ⚠️ `POST /api/schedule-x/auto-calculate` - Auto-calculate Schedule X
- ⚠️ `GET /api/schedule-x/multi-year-comparison` - Multi-year comparison
- ⚠️ `POST /api/schedule-x/import-from-federal` - Import from federal return
- ⚠️ `POST /api/schedule-y` - Create Schedule Y
- ⚠️ `GET /api/schedule-y` - Get Schedule Y list
- ⚠️ `GET /api/schedule-y/{id}` - Get Schedule Y details
- ⚠️ `GET /api/schedule-y/{id}/breakdown` - Get breakdown
- ⚠️ `GET /api/schedule-y/{id}/audit-log` - Get audit log

**NOL (Net Operating Loss):**
- ⚠️ `POST /api/nol` - Create NOL record
- ⚠️ `GET /api/nol/{businessId}` - Get NOL records
- ⚠️ `GET /api/nol/{businessId}/available` - Get available NOL
- ⚠️ `POST /api/nol/apply` - Apply NOL
- ⚠️ `GET /api/nol/schedule/{returnId}` - NOL schedule
- ⚠️ `GET /api/nol/schedule/{businessId}/vintages/{taxYear}` - NOL vintages
- ⚠️ `POST /api/nol/carryback` - NOL carryback
- ⚠️ `GET /api/nol/carryback/{nolId}` - Get carryback
- ⚠️ `GET /api/nol/alerts/{businessId}` - NOL alerts

**Penalties & Interest:**
- ⚠️ `POST /api/penalties/calculate` - Calculate penalties
- ⚠️ `GET /api/penalties/{id}` - Get penalty
- ⚠️ `GET /api/penalties` - List penalties
- ⚠️ `GET /api/penalties/return/{returnId}` - Get return penalties
- ⚠️ `GET /api/penalties/return/{returnId}/combined` - Combined penalties
- ⚠️ `POST /api/interest/calculate` - Calculate interest
- ⚠️ `GET /api/interest/{id}` - Get interest
- ⚠️ `GET /api/interest/return/{returnId}` - Get return interest
- ⚠️ `GET /api/interest/tenant/{tenantId}` - Get tenant interest

**Penalty Abatement:**
- ⚠️ `POST /api/abatements` - Request abatement
- ⚠️ `GET /api/abatements/{id}` - Get abatement
- ⚠️ `GET /api/abatements/return/{returnId}` - Get return abatements
- ⚠️ `GET /api/abatements/tenant/{tenantId}/pending` - Pending abatements
- ⚠️ `PATCH /api/abatements/{id}/review` - Review abatement
- ⚠️ `PATCH /api/abatements/{id}/withdraw` - Withdraw abatement
- ⚠️ `POST /api/abatements/{id}/documents` - Upload documents
- ⚠️ `GET /api/abatements/{id}/form-27pa` - Generate Form 27-PA

**Estimated Tax:**
- ⚠️ `POST /api/estimated-tax/evaluate-safe-harbor` - Evaluate safe harbor
- ⚠️ `POST /api/estimated-tax/calculate-penalty` - Calculate penalty
- ⚠️ `GET /api/estimated-tax/penalties/{id}` - Get penalty
- ⚠️ `GET /api/estimated-tax/penalties/return/{returnId}` - Get return penalties

**Payment Allocation:**
- ⚠️ `POST /api/payments/allocate` - Allocate payment
- ⚠️ `GET /api/payments/{id}` - Get allocation
- ⚠️ `GET /api/payments` - List allocations
- ⚠️ `GET /api/payments/return/{returnId}` - Get return allocations
- ⚠️ `GET /api/payments/return/{returnId}/latest` - Latest allocation
- ⚠️ `GET /api/payments/return/{returnId}/total` - Total allocated

**Apportionment:**
- ⚠️ `POST /api/apportionment/calculate` - Calculate apportionment
- ⚠️ `POST /api/apportionment/compare` - Compare apportionment

**Nexus Management:**
- ⚠️ `GET /api/nexus/{businessId}` - Get nexus records
- ⚠️ `GET /api/nexus/{businessId}/state/{state}` - Get state nexus
- ⚠️ `POST /api/nexus/{businessId}/update` - Update nexus
- ⚠️ `POST /api/nexus/{businessId}/batch-update` - Batch update nexus
- ⚠️ `POST /api/nexus/{businessId}/economic-nexus` - Economic nexus determination
- ⚠️ `GET /api/nexus/thresholds/{state}` - Get state thresholds
- ⚠️ `GET /api/nexus/{businessId}/count` - Count nexus states
- ⚠️ `GET /api/nexus/{businessId}/nexus-states` - List nexus states
- ⚠️ `GET /api/nexus/{businessId}/non-nexus-states` - List non-nexus states

#### Rule Management (Write Operations)
- ⚠️ `POST /api/rules` - Create rule
- ⚠️ `PUT /api/rules/{ruleId}` - Update rule
- ⚠️ `POST /api/rules/{ruleId}/approve` - Approve rule
- ⚠️ `POST /api/rules/{ruleId}/reject` - Reject rule
- ⚠️ `GET /api/rules/{ruleId}` - Get rule details
- ⚠️ `DELETE /api/rules/{ruleId}` - Delete rule
- ⚠️ `GET /api/rules/future` - Get future rules
- ⚠️ `GET /api/rules/history/{ruleCode}` - Get rule history
- ⚠️ `GET /api/rules/validate-overlap` - Validate overlap

#### User Management (Extended)
- ⚠️ `POST /api/v1/users/register` - User registration
- ⚠️ `GET /api/v1/users/verify-email` - Verify email
- ⚠️ `POST /api/v1/users/forgot-password` - Forgot password
- ⚠️ `POST /api/v1/users/reset-password` - Reset password
- ⚠️ `GET /api/v1/users/profiles` - List user profiles
- ⚠️ `GET /api/v1/users/profiles/primary` - Get primary profile
- ⚠️ `POST /api/v1/users/profiles` - Create profile
- ⚠️ `PUT /api/v1/users/profiles/{profileId}` - Update profile
- ⚠️ `DELETE /api/v1/users/profiles/{profileId}` - Delete profile

#### Address Validation
- ⚠️ `POST /api/v1/address/validate` - Validate address
- ⚠️ `POST /api/v1/address/is-dublin` - Check if address is in Dublin

#### Form Generation (Extended)
- ⚠️ `POST /api/forms/generate` - Generate form
- ⚠️ `GET /api/forms/{formId}` - Get form
- ⚠️ `GET /api/forms/{formId}/download` - Download form
- ⚠️ `GET /api/forms/health` - Health check

#### Submission Management (Extended)
- ⚠️ `GET /api/v1/submissions` - List submissions
- ⚠️ `POST /api/v1/submissions/{id}/approve` - Approve submission
- ⚠️ `POST /api/v1/submissions/{id}/reject` - Reject submission
- ⚠️ `GET /api/v1/submissions/{id}/documents` - Get submission documents
- ⚠️ `GET /api/v1/submissions/{id}/documents/{docId}` - Get document
- ⚠️ `GET /api/v1/submissions/{id}/documents/{docId}/provenance` - Document provenance

---

### 3. Partially Implemented APIs

These APIs have backend implementation but lack full integration or have incomplete features.

#### Payment Processing (Partially Implemented)
- `POST /api/v1/payments/process` - Process payment
  - **Status:** ⚠️ Backend implemented, workflow incomplete
  - **Issue:** No frontend integration for payment → confirmation → receipt flow
  - **Tracking Issues:** 
    - [shashanksaxena-tz/munciplaityTax#94](https://github.com/shashanksaxena-tz/munciplaityTax/issues/94) - Payment processing workflow
    - [shashanksaxena-tz/munciplaityTax#102](https://github.com/shashanksaxena-tz/munciplaityTax/issues/102) - Payment confirmation UI
  - **See:** Known Limitations section for details

---

### 4. Missing APIs

These APIs are referenced in documentation or are needed for complete workflows but are not yet implemented.

#### Batch Operations
- 📝 Bulk submission processing
- 📝 Batch audit assignment
- 📝 Bulk rule updates across tenants

#### Reporting & Analytics
- 📝 Municipality revenue reports
- 📝 Collection statistics
- 📝 Taxpayer filing trends
- 📝 Audit effectiveness metrics

#### Notifications
- 📝 Email notification triggers
- 📝 SMS alerts for deadlines
- 📝 Webhook integrations

#### Multi-Tenant Administration
- 📝 Tenant provisioning
- 📝 Tenant configuration management
- 📝 Cross-tenant reporting

---

## Known Limitations

### Payment Processing Workflow

**Issue:** Steps 26-30 in the Individual Tax Filing sequence diagram (Payment → Confirmation → Receipt) are documented but not fully implemented in the frontend.

**Impact:**
- Taxpayers can calculate tax liability but cannot complete payment online
- No automated receipt generation after payment
- Manual payment processing required by municipality staff

**Workaround:**
- Taxpayers download PDF tax return
- Payment must be submitted separately (check, wire transfer, in-person)
- Municipality staff manually record payments in ledger system

**Backend Status:**
- ✅ `POST /api/v1/payments/process` - Implemented with mock payment provider
- ✅ `GET /api/v1/payments/{paymentId}/receipt` - Implemented
- ✅ `POST /api/v1/payments/{id}/confirm` - Implemented
- ❌ Frontend payment form - Not implemented
- ❌ Payment confirmation screen - Not implemented
- ❌ Receipt display - Not implemented

**Tracking Issues:**
- [shashanksaxena-tz/munciplaityTax#94](https://github.com/shashanksaxena-tz/munciplaityTax/issues/94) - Implement payment processing workflow
- [shashanksaxena-tz/munciplaityTax#102](https://github.com/shashanksaxena-tz/munciplaityTax/issues/102) - Create payment confirmation UI

**References:**
- Backend: `backend/ledger-service/src/main/java/com/munitax/ledger/controller/PaymentController.java`
- Sequence Diagram: `docs/SEQUENCE_DIAGRAMS.md` (Section 7, Steps 26-30)

---

## Recommendations

### High Priority
1. **Complete Payment Workflow** - Implement frontend for payment processing (#94, #102)
2. **Audit Trail Visibility** - Add UI for audit trail viewing
3. **Document Request System** - Enable auditors to request additional documents
4. **W-1 Filing Workflow** - Implement monthly withholding tax filing for employers

### Medium Priority
1. **Reconciliation Reports** - Add municipality dashboard with reconciliation capabilities
2. **Account Statements** - Enable taxpayers to view and download account statements
3. **NOL Management** - Add UI for NOL tracking and application
4. **Penalty & Interest** - Display penalty and interest calculations in UI

### Low Priority / Future
1. **Advanced Tax Features** - Schedule X/Y enhancements, apportionment, nexus management
2. **Refund Processing** - Implement refund request and issuance workflows
3. **Penalty Abatement** - Add UI for penalty abatement requests (Form 27-PA)
4. **Batch Operations** - Implement bulk processing capabilities

### Technical Debt
1. **API Cleanup** - Consider deprecating or removing truly unused endpoints
2. **API Documentation** - Generate OpenAPI/Swagger documentation for all endpoints
3. **Frontend-Backend Alignment** - Ensure frontend uses all necessary APIs
4. **Testing** - Add integration tests for unused but implemented APIs

---

## API Usage Statistics

| Category | Count | Percentage |
|----------|-------|------------|
| **Active/Used** | 22 | 22% |
| **Unused** | 78 | 78% |
| **Partially Implemented** | 1 | 1% |
| **Missing** | ~12 | N/A |

**Note:** Percentages based on backend controller endpoints. Many "unused" APIs are fully functional and ready for frontend integration.

---

## Maintenance

This document should be updated:
- When new APIs are added to the backend
- When frontend integration changes API usage status
- When tracking issues are created or resolved
- Quarterly during architecture review meetings

**Document Owner:** Development Team  
**Next Review:** March 2026

---

## See Also

- [Sequence Diagrams](SEQUENCE_DIAGRAMS.md) - Detailed workflow diagrams
- [Data Flow Documentation](DATA_FLOW.md) - System data flows
- [Architecture Documentation](ARCHITECTURE.md) - System architecture
- [API Samples](../API_SAMPLES.md) - Example API requests and responses
