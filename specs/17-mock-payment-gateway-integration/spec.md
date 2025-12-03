# Mock Payment Gateway Integration with Ledger, Reconciliation, and Audit Reports

**Feature Name:** Mock Payment Gateway Integration  
**Priority:** MEDIUM  
**Status:** Specification  
**Created:** 2025-12-03

---

## Overview

Enhance the existing mock payment gateway to remove hardcoded frontend test payment data by fetching it from the backend API, ensure proper integration between payments and the double-entry ledger system, enhance reconciliation capabilities for payment tracking, and provide comprehensive audit reports for all payment events.

**Current State**: 
- Backend already implements mock payment processing with test cards and ACH accounts producing appropriate outcomes
- Frontend currently uses static test payment data instead of dynamically fetching from backend API
- System already creates double-entry journal entries for approved payments
- Reconciliation and audit capabilities exist but need payment-specific enhancements

**Target Users**: Filers making tax payments, municipality finance officers verifying payment flows, auditors tracing payment history, developers testing payment integration.

---

## Clarifications

### Session 2025-12-03

- Q: What should be the API endpoint path for fetching test payment methods? → A: `GET /api/v1/payments/test-methods` (follows existing payment API pattern)
- Q: What response time threshold (ms) should define acceptable performance for the test methods API endpoint? → A: 500ms
- Q: When user clicks a test card/ACH in the helper panel, what fields should auto-fill? → A: Card number only (user fills expiry/CVV); for ACH, routing and account number
- Q: Should CHECK and WIRE payment methods be included in test methods API? → A: No, exclude CHECK/WIRE (manual types with no test scenarios)
- Q: Should the test methods API require authentication? → A: No authentication required (TEST mode protected, returns empty in PRODUCTION)

---

## User Scenarios & Testing

### User Story 1 - Fetch Test Payment Methods from Backend API (Priority: P1)

As a filer using the payment gateway in test mode, I want the available test card numbers and ACH accounts to be loaded from the backend API rather than hardcoded in the frontend, so that the test payment methods are centrally managed and consistent across all system components.

**Why this priority**: This addresses the primary technical debt issue - static data in the frontend creates maintenance burden and potential inconsistencies with the backend. Central management ensures all test scenarios work correctly.

**Independent Test**: Can be fully tested by loading the payment gateway in test mode and verifying test payment methods are fetched via API call rather than from static data.

**Acceptance Scenarios**:

1. **Given** system is in TEST mode, **When** filer opens the payment gateway, **Then** the system fetches available test payment methods from the backend API and displays them in the helper panel.

2. **Given** system is in PRODUCTION mode, **When** filer opens the payment gateway, **Then** the system does not fetch or display test payment methods.

3. **Given** API call to fetch test methods fails, **When** filer views the payment helper panel, **Then** a graceful error message is displayed and the user can still enter payment details manually.

4. **Given** test methods are fetched successfully, **When** filer views the test cards helper, **Then** each card shows: card number, card type (Visa/Mastercard/Amex), and expected result (Approved/Declined/Error).

---

### User Story 2 - Payment to Ledger Integration with Double-Entry Journal Entries (Priority: P1)

As a filer making a payment, I want the payment to be properly recorded in the double-entry ledger with matching journal entries on both filer and municipality books, so that my payment is accurately reflected in my account statement and can be reconciled against municipality records.

**Why this priority**: This is the core financial integration that ensures payment integrity and auditability. Without proper ledger integration, payments cannot be reconciled or audited.

**Independent Test**: Can be tested by making a payment with test card 4111-1111-1111-1111 and verifying journal entries are created on both filer and municipality ledgers with correct amounts and account codes.

**Acceptance Scenarios**:

1. **Given** filer makes $5,000 payment using approved test card, **When** payment is processed successfully, **Then** system creates journal entry on filer books: DEBIT Tax Liability account $5,000, CREDIT Cash account $5,000.

2. **Given** filer payment is approved, **When** ledger entries are created, **Then** system creates matching journal entry on municipality books: DEBIT Cash account $5,000, CREDIT Accounts Receivable account $5,000.

3. **Given** filer makes payment using declined test card, **When** payment is rejected, **Then** no journal entries are created and payment transaction is recorded with status DECLINED.

4. **Given** payment succeeds but ledger entry creation fails, **When** error occurs during journal entry creation, **Then** entire transaction is rolled back and filer receives error message indicating payment was not processed.

5. **Given** payment is successfully recorded with journal entries, **When** filer views account statement, **Then** payment appears with transaction type "Payment", credit amount, and running balance updated accordingly.

---

### User Story 3 - Payment Reconciliation with Drill-Down Capability (Priority: P2)

As a municipality finance officer, I want to reconcile payments between filer accounts and municipality accounts with the ability to drill down to individual payment discrepancies, so that I can identify and resolve any mismatches in payment records.

**Why this priority**: High value for financial integrity and audit compliance. Ensures payments recorded by filers match payments received by municipality.

**Independent Test**: Can be tested by running reconciliation report after multiple payments and verifying totals match with no discrepancies, or by introducing a test discrepancy and verifying it is detected and can be investigated.

**Acceptance Scenarios**:

1. **Given** multiple filers have made payments, **When** finance officer runs reconciliation report, **Then** report shows: Total Municipality Cash Receipts, Total Filer Payments, and Variance (should be $0 if reconciled).

2. **Given** reconciliation shows a discrepancy, **When** finance officer clicks on discrepancy row, **Then** system shows drill-down view with: filer name, payment date, filer recorded amount, municipality recorded amount, and variance.

3. **Given** drill-down view is displayed, **When** finance officer clicks "View Transactions", **Then** system navigates to filer's transaction history filtered to the relevant payment period.

4. **Given** reconciliation report is generated, **When** filtering by date range (e.g., last 30 days), **Then** only payments within that date range are included in the reconciliation totals.

---

### User Story 4 - Payment Audit Trail with Comprehensive Filtering (Priority: P2)

As an auditor, I want to view a complete audit trail for all payment events including method used, status, timestamps, and user IDs, with the ability to filter by date range, filer, and status, so that I can verify payment legitimacy and trace issues.

**Why this priority**: Essential for compliance, fraud detection, and audit defense. Provides complete traceability of all payment activities.

**Independent Test**: Can be tested by making several payments with different methods and statuses, then viewing audit trail filtered by various criteria.

**Acceptance Scenarios**:

1. **Given** payments have been processed, **When** auditor views payment audit trail, **Then** each payment event shows: timestamp, filer ID, payment method (CREDIT_CARD/ACH/CHECK/WIRE), amount, status (APPROVED/DECLINED/ERROR), and transaction ID.

2. **Given** payment audit trail is displayed, **When** auditor filters by date range "2025-12-01 to 2025-12-31", **Then** only payments within that range are shown.

3. **Given** payment audit trail is displayed, **When** auditor filters by status "DECLINED", **Then** only declined payments are shown with their failure reasons.

4. **Given** payment audit trail is displayed, **When** auditor filters by specific filer ID, **Then** only payments from that filer are shown.

5. **Given** payment was processed, **When** auditor views payment detail, **Then** audit trail shows all events: payment initiated, payment processed, journal entry created, with timestamps and user IDs for each event.

---

### User Story 5 - Test Payment Method Auto-Fill (Priority: P3)

As a developer or tester, I want to quickly select a test payment method from the helper panel to auto-fill the payment form, so that I can efficiently test different payment scenarios without typing full card numbers.

**Why this priority**: Convenience feature that improves developer experience and testing efficiency. Not critical for core functionality.

**Independent Test**: Can be tested by clicking on a test card in the helper panel and verifying form fields are populated.

**Acceptance Scenarios**:

1. **Given** test card helper panel is expanded, **When** user clicks on test card "4111-1111-1111-1111", **Then** card number field only is auto-filled with the selected card number (user must fill expiry and CVV manually).

2. **Given** test ACH helper panel is expanded, **When** user clicks on test account "110000000 / 000123456789", **Then** routing and account number fields are auto-filled.

3. **Given** form is auto-filled with test data, **When** user submits payment, **Then** payment is processed with the auto-filled values producing the expected result (approved/declined based on test data).

---

### Edge Cases

- What happens when backend API for test methods is unavailable? Frontend should gracefully degrade and allow manual entry.
- How does system handle payment timeout? Payment should be marked as ERROR with appropriate audit log entry.
- What happens if filer makes duplicate payment? System should warn filer of potential duplicate but allow processing if confirmed.
- How are partial refunds handled in reconciliation? Refunds should create reverse journal entries and be tracked separately.
- What happens when payment amount exceeds tax liability (overpayment)? System records full payment, creates credit balance on filer account.

---

## Requirements

### Functional Requirements

#### Test Payment Methods API

- **FR-001**: System MUST provide a `GET /api/v1/payments/test-methods` endpoint that returns available test payment methods when system is in TEST mode.

- **FR-002**: System MUST return test credit cards with the following information: card number, card type (Visa/Mastercard/Amex), expected result (APPROVED/DECLINED/ERROR), and description. CHECK and WIRE payment types are excluded (manual types with no test scenarios).

- **FR-003**: System MUST return test ACH accounts with the following information: routing number, account number, expected result (APPROVED/DECLINED), and description.

- **FR-004**: System MUST return empty result or access denied when test methods endpoint is called in PRODUCTION mode.

- **FR-005**: Frontend MUST fetch test payment methods from `GET /api/v1/payments/test-methods` on component load instead of using static data.

- **FR-006**: Frontend MUST gracefully handle API failure by displaying error message and allowing manual data entry.

- **FR-006a**: The test methods endpoint MUST NOT require authentication (protected by TEST mode; returns empty in PRODUCTION).

#### Payment-Ledger Integration

- **FR-007**: System MUST create double-entry journal entries for every approved payment.

- **FR-008**: System MUST link payment transaction to journal entry ID for traceability.

- **FR-009**: System MUST rollback entire transaction if journal entry creation fails after payment approval.

- **FR-010**: System MUST record payment status regardless of outcome (APPROVED, DECLINED, ERROR).

- **FR-011**: System MUST include payment method, provider transaction ID, and authorization code in payment record.

#### Reconciliation Enhancements

- **FR-012**: Reconciliation report MUST include payment-specific totals: Municipality Cash Receipts vs Filer Payments.

- **FR-013**: System MUST detect payment discrepancies where filer recorded payment differs from municipality record.

- **FR-014**: System MUST support drill-down from reconciliation discrepancy to specific payment transaction details.

- **FR-015**: Reconciliation report MUST support filtering by date range for payment period analysis.

- **FR-016**: System MUST link discrepancies to source journal entries for investigation.

#### Payment Audit Trail

- **FR-017**: System MUST log all payment events: initiated, approved, declined, error, refunded.

- **FR-018**: Payment audit log MUST capture: timestamp, user ID, filer ID, payment method, amount, status, transaction ID.

- **FR-019**: System MUST support filtering audit trail by: date range, filer ID, payment status, payment method.

- **FR-020**: System MUST link payment audit entries to journal entry audit trail for complete traceability.

- **FR-021**: System MUST record failure reasons for declined/error payments in audit log.

### Non-Functional Requirements

- **NFR-001**: The test methods API endpoint (`GET /api/v1/payments/test-methods`) MUST respond within 500ms under normal load.

- **NFR-002**: The test methods API MUST be stateless and cacheable (response can be cached by frontend).

### Key Entities

- **TestPaymentMethod**: Represents a test payment method with number, type, expected outcome, and description. Used for API response.

- **PaymentTransaction**: Existing entity tracking payment details, status, provider response, and journal entry link.

- **PaymentAuditEvent**: Audit entry for payment-specific events with action type, status, and failure reason.

- **ReconciliationReport**: Existing entity enhanced with payment-specific totals (municipality cash, filer payments, cash variance).

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: 100% of approved payments have corresponding journal entries created within the same transaction.

- **SC-002**: Reconciliation report accurately reflects all payment transactions with zero untracked payments.

- **SC-003**: Payment audit trail provides complete history for 100% of payment events with all required fields populated.

- **SC-004**: Test payment methods are fetched from backend API, eliminating dependency on static frontend data.

- **SC-005**: Finance officers can identify payment discrepancies within 3 clicks (report → discrepancy → transaction detail).

- **SC-006**: Audit filters correctly narrow results to match filter criteria with 100% accuracy.

- **SC-007**: Test methods API responds within 500ms (per NFR-001).

---

## Assumptions

- System operates in TEST mode for development and testing; PRODUCTION mode prevents test method exposure.
- Existing payment processing and mock payment provider implementations are correct and stable.
- Frontend follows existing patterns and conventions in the codebase.
- Reconciliation runs on-demand, not scheduled (user triggers report generation).
- Audit retention follows existing system-wide audit retention policy.

---

## Dependencies

- **Spec 12 (Double-Entry Ledger)**: Core ledger functionality - Implementation exists
- **Mock Payment Provider**: Backend mock payment processing capability - Implementation exists  
- **Payment Service**: Payment processing with ledger integration capability - Implementation exists
- **Reconciliation Report**: Reconciliation display capability - Implementation exists
- **Audit Trail**: Audit display with filtering capability - Implementation exists

---

## Out of Scope

- Real payment gateway integration (Stripe, Square, PayPal) - separate implementation
- Payment scheduling or recurring payments
- Multi-currency support (USD only)
- Mobile-specific payment flows (Apple Pay, Google Pay)
- PCI compliance for real card handling (mock only)
- Automated bank reconciliation with external bank feeds
