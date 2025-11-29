# Spec 12: Double-Entry Ledger System with Mock Payment Provider

**Priority:** MEDIUM  
**Feature Branch:** `12-double-entry-ledger`  
**Spec Document:** `specs/12-double-entry-ledger/spec.md`

## Overview

Implement a complete double-entry accounting ledger system to track all tax payments, liabilities, refunds, and adjustments with full transaction history and two-way reconciliation between filer accounts and municipality accounts. Include a mock payment provider (test mode like Stripe) to simulate credit card, ACH, and check payments without processing real transactions.

## Implementation Status

**Current:** 0% - Simple transaction tracking, no double-entry bookkeeping  
**Required:** Full ledger system with chart of accounts, journal entries, trial balance, reconciliation

## Core Requirements (FR-001 to FR-055)

### Mock Payment Provider (FR-001 to FR-008)
- [ ] Provide mock payment provider API supporting test mode (no real charges)
- [ ] Accept test credit cards:
  - 4111-1111-1111-1111 → APPROVED (Visa)
  - 4000-0000-0000-0002 → DECLINED (card declined)
  - 4000-0000-0000-0119 → ERROR (processing error)
  - 5555-5555-5555-4444 → APPROVED (Mastercard)
  - 3782-822463-10005 → APPROVED (Amex)
- [ ] Accept test ACH accounts: Routing 110000000, various account numbers
- [ ] Return mock transaction response with status, transaction ID, authorization code
- [ ] Process approved payments instantly (no delay)
- [ ] Display test mode indicator: "TEST MODE: No real charges"
- [ ] Support payment methods: Credit Card, ACH/eCheck, Check, Wire Transfer
- [ ] Support toggling between test mode and production mode (admin only)

### Chart of Accounts (FR-009 to FR-012)
- [ ] Provide chart of accounts for filers:
  - 1000: Cash, 1200: Refund Receivable
  - 2100: Tax Liability (current year), 2110: Tax Liability (prior years)
  - 2120: Penalty Liability, 2130: Interest Liability
  - 6100: Tax Expense
- [ ] Provide chart of accounts for municipalities:
  - 1000: Cash, 1200: Accounts Receivable (taxes due)
  - 2200: Refunds Payable
  - 4100: Tax Revenue, 4200: Penalty Revenue, 4300: Interest Revenue
  - 5200: Refund Expense
- [ ] Support custom accounts (configurable by municipality)
- [ ] Categorize accounts by type: ASSET, LIABILITY, REVENUE, EXPENSE

### Double-Entry Journal Entries (FR-013 to FR-018)
- [ ] Create double-entry journal entries for all transactions
- [ ] Validate all entries: Total debits = Total credits (reject if unequal)
- [ ] Assign unique journal entry number (JE-YYYY-#####)
- [ ] Create journal entries for: Tax assessment, Payment, Refund, Adjustment, Penalty, Interest, Write-off
- [ ] Link journal entries to source transactions (Return ID, Payment ID, etc.)
- [ ] Support compound journal entries (more than 2 lines)

### Ledger Balances (FR-019 to FR-022)
- [ ] Maintain running balance for each account
- [ ] Calculate balance correctly based on account type:
  - ASSET/EXPENSE: Debit increases, Credit decreases
  - LIABILITY/REVENUE: Credit increases, Debit decreases
- [ ] Support account balance inquiry: Current, Historical, Beginning, Ending
- [ ] Display account activity (all entries affecting the account)

### Filer Account Statement (FR-023 to FR-026)
- [ ] Generate filer account statement showing all transactions
- [ ] Display: Transaction date, description, debit, credit, running balance
- [ ] Support filtering by date range, tax year, transaction type
- [ ] Export statement to PDF and CSV
- [ ] Calculate aging of balance (0-30, 31-60, 61-90, 90+ days overdue)

### Two-Way Reconciliation (FR-027 to FR-030)
- [ ] Reconcile filer ledgers to municipality ledger:
  - Total Filer Tax Liabilities = Municipality Accounts Receivable
  - Total Filer Payments = Municipality Cash Receipts
- [ ] Generate reconciliation report with variances
- [ ] Support drill-down reconciliation (specific filer vs municipality entries)
- [ ] Flag discrepancies: unmatched transactions, amount mismatches

### Trial Balance (FR-031 to FR-035)
- [ ] Generate trial balance for municipality general ledger
- [ ] List all accounts with debit balance, credit balance, net balance
- [ ] Calculate total debits, total credits, difference
- [ ] Flag if unbalanced (debits ≠ credits)
- [ ] Support trial balance as of specific date

### Refund Processing (FR-036 to FR-042)
- [ ] Detect overpayments (payments > tax liability)
- [ ] Allow filer to request refund (amount ≤ overpayment)
- [ ] Create journal entries for refund:
  - Filer books: DEBIT Refund Receivable, CREDIT Tax Liability
  - Municipality books: DEBIT Refund Expense, CREDIT Refunds Payable
- [ ] Support refund approval workflow
- [ ] When refund issued: update both books with payment entries
- [ ] Support refund methods: ACH, Check, Wire Transfer
- [ ] Generate refund confirmation with transaction ID

### Payment Allocation (FR-043 to FR-046)
- [ ] Allocate payments to liabilities in order: Interest → Penalties → Tax (oldest first)
- [ ] Support custom allocation (filer specifies target)
- [ ] Handle partial payments with balance recalculation
- [ ] Display payment allocation breakdown

### Audit Trail (FR-047 to FR-051)
- [ ] Log all ledger entry creation: User ID, timestamp, source transaction
- [ ] Log all modifications: old value, new value, reason
- [ ] Prevent deletion of posted entries (immutability)
- [ ] Support reversing entries only (to correct errors)
- [ ] Log all access to ledger (who viewed, when)

### Financial Reporting (FR-052 to FR-055)
- [ ] Generate municipality financial reports: Balance Sheet, Income Statement, Cash Flow
- [ ] Generate aging report: Total AR by age bucket (0-30, 31-60, 61-90, 90+ days)
- [ ] Generate cash receipts report: Total cash by date, filer, payment method
- [ ] Export all reports to PDF, CSV, Excel

## User Stories (7 Priority P1-P3)

1. **US-1 (P1):** Simulate Payment with Mock Payment Provider
2. **US-2 (P1):** Record Double-Entry Journal Entries for Tax Assessment
3. **US-3 (P2):** Two-Way Ledger Reconciliation Report
4. **US-4 (P2):** Track Filer Payment History in Account Statement
5. **US-5 (P3):** Generate Trial Balance for Municipality
6. **US-6 (P2):** Process Refund with Ledger Entries
7. **US-7 (P1):** Audit Trail of All Ledger Entries

## Key Entities

### MockPaymentProvider
- providerId, providerName ("Mock Payment Gateway")
- mode (TEST/PRODUCTION), supportedMethods[]
- testCards (JSON with test card numbers and outcomes)

### PaymentTransaction
- transactionId, paymentId, providerTransactionId
- status (APPROVED/DECLINED/ERROR/PENDING)
- paymentMethod, amount, currency, authorizationCode
- failureReason, timestamp

### ChartOfAccounts
- accountId, accountNumber, accountName
- accountType (ASSET/LIABILITY/REVENUE/EXPENSE)
- normalBalance (DEBIT/CREDIT), parentAccountId, tenantId

### JournalEntry
- entryId, entryNumber, entryDate, description
- sourceType (TAX_ASSESSMENT/PAYMENT/REFUND/etc.), sourceId
- status (DRAFT/POSTED/REVERSED)
- createdBy, createdAt, postedBy, postedAt
- reversedBy, reversalEntryId, lines[] (array of JournalEntryLine)

### JournalEntryLine
- lineId, entryId, accountId, lineNumber
- debit, credit, description

### AccountBalance
- balanceId, accountId, entityId (Filer or Municipality)
- periodStartDate, periodEndDate
- beginningBalance, totalDebits, totalCredits, endingBalance

### FilerAccountStatement
- statementId, filerId, statementDate
- beginningBalance, transactions[] (array of StatementTransaction)
- endingBalance, totalDebits, totalCredits

### StatementTransaction
- transactionDate, transactionType, description
- debitAmount, creditAmount, runningBalance

### ReconciliationReport
- reportId, reportDate, municipalityId
- totalMunicipalityAR, totalFilerLiabilities, arVariance
- totalMunicipalityCash, totalFilerPayments, cashVariance
- reconciliationStatus (RECONCILED/DISCREPANCY)
- discrepancies[] (array of Discrepancy objects)

### Discrepancy
- discrepancyId, filerId, filerName, transactionType
- transactionDate, filerAmount, municipalityAmount, variance
- description

## Success Criteria

- 100% of test mode payments processed instantly with mock transaction IDs (no real charges)
- 100% of journal entries balance (total debits = total credits, zero tolerance)
- Filer and municipality ledgers reconcile with zero variance (or all explained within 5 days)
- Complete audit trail for 100% of ledger entries (who, when, what, why)
- 90% of filers use account statement feature to verify balances at least once per year

## Edge Cases Documented

- Overpayment exceeds tax due (credit balance)
- Payment allocation with multiple liabilities
- Refund exceeds cash balance
- Journal entry unbalanced due to rounding
- Reversing entry for incorrect payment amount
- Test card declined
- Trial balance out of balance
- Reconciliation discrepancy due to timing

## Technical Implementation

### Backend Services
- [ ] MockPaymentProviderService.java
- [ ] PaymentTransactionService.java
- [ ] ChartOfAccountsService.java
- [ ] JournalEntryService.java
- [ ] LedgerBalanceService.java
- [ ] ReconciliationService.java
- [ ] FinancialReportService.java

### Controllers
- [ ] PaymentController.java
  - POST /api/payment/process (with test mode)
  - GET /api/payment/transaction/{transactionId}
- [ ] LedgerController.java
  - POST /api/ledger/journal-entry
  - GET /api/ledger/account-statement/{filerId}
  - GET /api/ledger/trial-balance
  - GET /api/ledger/reconciliation

### Frontend Components
- [ ] MockPaymentForm.tsx (with test mode indicator)
- [ ] AccountStatement.tsx
- [ ] TrialBalance.tsx
- [ ] ReconciliationReport.tsx
- [ ] JournalEntryView.tsx
- [ ] RefundRequestForm.tsx

## Dependencies

- Payment Gateway integration (mock provider standalone, real gateway separate)
- Business Form Library (Spec 8) - Generate payment vouchers
- Auditor Workflow (Spec 9) - Audit trail integration
- Enhanced Penalty/Interest (Spec 7) - Penalties recorded as journal entries

## Out of Scope

- Real payment gateway integration (Stripe, Square, PayPal)
- Real ACH processing (Plaid, Dwolla)
- Check scanning (mobile app deposit)
- Multi-currency support (USD only)
- Full GAAP compliance (simplified for municipal tax)
- Automated bank reconciliation

## Related Specs

- Used by: ALL payment-related specs (universal payment tracking)
- Integrates with: Spec 7 (Penalty entries), Spec 6 (NOL affects income)
- Critical for: Financial transparency and audit compliance
