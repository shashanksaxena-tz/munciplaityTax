# Double-Entry Ledger System with Mock Payment Provider

**Feature Name:** Comprehensive Payment Ledger and Mock Payment Gateway  
**Priority:** MEDIUM  
**Status:** Specification  
**Created:** 2025-11-27

---

## Overview

Implement a complete double-entry accounting ledger system to track all tax payments, liabilities, refunds, and adjustments with full transaction history and two-way reconciliation between filer accounts and municipality accounts. Include a mock payment provider (test mode like Stripe) to simulate credit card, ACH, and check payments without processing real transactions. The ledger ensures perfect reconciliation where every debit has a matching credit, providing auditable financial trails for both filers and municipalities.

**Current State:** No ledger system (0% complete). Payments recorded as simple transactions with no double-entry bookkeeping, no chart of accounts, no trial balance, no reconciliation between filer and municipality books.

**Target Users:** Filers verifying payment history, CFOs reconciling tax accounts, municipality finance officers tracking revenue, auditors verifying payment trails, system administrators testing payment flows.

---

## User Scenarios & Testing

### US-1: Simulate Payment with Mock Payment Provider (P1 - Critical)

**User Story:**  
As a filer testing the system, I want to use test credit card 4111-1111-1111-1111 to make a $5,000 tax payment in sandbox mode, receive immediate approval, and see the payment reflected in my ledger without actually charging my real card, so that I can validate the payment workflow before going live.

**Business Context:**  
Mock payment provider mimics Stripe's test mode: accepts test credit cards (4111... = Visa success, 4000-0000-0000-0002 = card declined), processes instantly, returns transaction ID. No real money moves. Essential for development, testing, demos, and training.

**Independent Test:**  
- Filer owes $5,000 tax for Q1 2024
- Filer selects "Credit Card" payment method
- System displays: "TEST MODE: Use test cards. No real charges."
- Filer enters: 4111-1111-1111-1111, Exp: 12/25, CVV: 123
- System calls mock provider API → Returns: Status=APPROVED, TransactionID=mock_ch_abc123, Amount=$5,000
- Ledger entries:
  - DEBIT: Filer Cash Account $5,000 (asset decreases)
  - CREDIT: Filer Tax Liability Account $5,000 (liability decreases)
  - DEBIT: Municipality Receivable Account $5,000 (asset decreases)
  - CREDIT: Municipality Revenue Account $5,000 (revenue increases)
- Filer sees: "Payment successful. Receipt #mock_ch_abc123"

**Acceptance Criteria:**
- GIVEN system in test/sandbox mode
- WHEN filer makes payment with test card 4111-1111-1111-1111
- THEN system MUST call mock payment provider API with:
  - Amount, Currency (USD)
  - Payment method (card number, expiration, CVV)
  - Description (tax payment, year, quarter)
- AND mock provider MUST return:
  - Status: APPROVED | DECLINED | ERROR
  - Transaction ID (mock_ch_*)
  - Authorization code
  - Timestamp
- AND system MUST process approved payment:
  - Create Payment record with status=COMPLETED
  - Generate ledger entries (4 entries: filer debit/credit, municipality debit/credit)
  - Update tax return balance to $0
  - Generate receipt with transaction ID
- AND system MUST display test mode indicator: "TEST MODE: No real charges"
- AND system MUST NOT process real payments in test mode

---

### US-2: Record Double-Entry Journal Entries for Tax Assessment (P1 - Critical)

**User Story:**  
As the system, when a business files a return showing $10,000 tax due, I want to automatically create double-entry journal entries (DEBIT Filer Tax Liability $10K, CREDIT Municipality Accounts Receivable $10K) on both filer and municipality books, so that the tax liability is properly recorded and the ledger stays balanced.

**Business Context:**  
Double-entry bookkeeping: Every transaction has equal debits and credits. When tax assessed:
- Filer books: DEBIT Tax Liability (liability increases), CREDIT (offset depends on transaction type, typically accrued expenses)
- Municipality books: DEBIT Accounts Receivable (asset increases), CREDIT Tax Revenue (revenue increases)

This creates matching records on both sides, enabling reconciliation.

**Independent Test:**  
- Business files 2024 Q1 return on April 20, 2024
- Tax calculated: $10,000
- System creates journal entries:

**Filer Books (Entry #1):**
- Date: 2024-04-20
- Description: "Q1 2024 Tax Assessment"
- DEBIT: Account 2100 (Tax Liability) $10,000
- CREDIT: Account 6100 (Tax Expense) $10,000
- Balance: Filer now owes $10,000

**Municipality Books (Entry #2):**
- Date: 2024-04-20
- Description: "Business XYZ Q1 2024 Tax"
- DEBIT: Account 1200 (Accounts Receivable) $10,000
- CREDIT: Account 4100 (Tax Revenue) $10,000
- Balance: Municipality expects $10,000 payment

**Acceptance Criteria:**
- GIVEN tax return filed and calculated
- WHEN tax liability finalized
- THEN system MUST create journal entry on filer books:
  - Entry date = filing date
  - Description = "Tax Year YYYY Quarter Q Tax Assessment"
  - DEBIT: Tax Liability account (increases liability)
  - CREDIT: Tax Expense account (recognizes expense)
  - Amount = total tax due
- AND system MUST create matching journal entry on municipality books:
  - Entry date = filing date
  - Description = "Filer Name Tax Year YYYY Quarter Q"
  - DEBIT: Accounts Receivable account (increases asset)
  - CREDIT: Tax Revenue account (increases revenue)
  - Amount = total tax due
- AND system MUST validate entry balances (total debits = total credits)
- AND system MUST assign unique journal entry number to each entry
- AND system MUST link filer entry to municipality entry (reconciliation reference)

---

### US-3: Two-Way Ledger Reconciliation Report (P2 - High Value)

**User Story:**  
As a municipality finance officer, I want to generate a reconciliation report comparing my Accounts Receivable ledger ($2.5M) to all filers' Tax Liability ledgers ($2.5M), showing that the books match perfectly, so that I can verify no discrepancies exist and my financial statements are accurate.

**Business Context:**  
Two-way reconciliation ensures filer ledgers match municipality ledgers:
- Filer total tax liabilities (all businesses) MUST equal Municipality total accounts receivable
- Filer total payments (all businesses) MUST equal Municipality total cash receipts
- Any mismatch indicates error: payment not recorded, duplicate entry, amount typo

This is critical for audit defense and financial reporting.

**Independent Test:**  
- 100 businesses with tax liabilities
- Municipality Accounts Receivable balance: $2,500,000
- Filer ledgers sum: $2,500,000
- Reconciliation report:
  - Total AR (Municipality): $2,500,000
  - Total Liabilities (Filers): $2,500,000
  - Difference: $0 ✓
  - Status: RECONCILED
- If one filer has $5K payment not recorded by municipality:
  - Total AR: $2,505,000
  - Total Liabilities: $2,500,000
  - Difference: $5,000 ✗
  - Status: DISCREPANCY DETECTED
  - Details: "Filer ABC123 shows $5K payment on 2024-04-25 not recorded in municipality books"

**Acceptance Criteria:**
- GIVEN multiple filers with tax liabilities and payments
- WHEN municipality runs reconciliation report
- THEN system MUST calculate:
  - Total Municipality Accounts Receivable (sum of all open AR)
  - Total Filer Tax Liabilities (sum of all filer liability balances)
  - Difference (AR - Liabilities)
- AND system MUST calculate:
  - Total Municipality Cash Receipts (sum of all payments received)
  - Total Filer Payments Made (sum of all payments from filer ledgers)
  - Difference (Receipts - Payments)
- AND system MUST display reconciliation summary:
  - Municipality AR total, Filer Liability total, Variance
  - Municipality Cash total, Filer Payment total, Variance
  - Reconciliation status: RECONCILED (variance = $0) | DISCREPANCY (variance ≠ $0)
- AND if discrepancy detected:
  - System MUST list transactions causing discrepancy
  - Show filer name, transaction date, amount, transaction type
  - Link to journal entries for investigation
- AND system MUST support drill-down to individual filer reconciliation (one filer's books vs municipality entry for that filer)

---

### US-4: Track Filer Payment History in Account Statement (P2 - High Value)

**User Story:**  
As a filer, I want to view my account statement showing all transactions (tax assessments, payments, refunds, adjustments) with running balance, so that I can see my current balance ($0 paid in full vs $2,500 outstanding) and verify payment history.

**Business Context:**  
Account statement is like a bank statement: shows chronological list of transactions with running balance. Example:
- 04/20: Tax assessed $10,000 → Balance: $10,000 owed
- 04/25: Payment $10,000 → Balance: $0
- 05/15: Penalty $50 → Balance: $50 owed
- 05/20: Payment $50 → Balance: $0

Clear, easy to understand, essential for filers to track what they owe.

**Independent Test:**  
- Filer files Q1 return on 04/20, tax $10,000
- Filer pays $10,000 on 04/25
- Late penalty assessed $50 on 05/15
- Filer pays $50 on 05/20

**Account Statement:**
```
Date       | Transaction Type       | Debit     | Credit    | Balance
-----------|------------------------|-----------|-----------|----------
2024-04-20 | Q1 2024 Tax Assessment | $10,000   |           | $10,000
2024-04-25 | Credit Card Payment    |           | $10,000   | $0
2024-05-15 | Late Filing Penalty    | $50       |           | $50
2024-05-20 | ACH Payment            |           | $50       | $0
```

**Acceptance Criteria:**
- GIVEN filer with transaction history
- WHEN filer views account statement
- THEN system MUST display transactions chronologically:
  - Transaction date
  - Transaction type (tax assessment, payment, refund, adjustment, penalty, interest)
  - Debit amount (increases balance: tax, penalties, interest)
  - Credit amount (decreases balance: payments, refunds)
  - Running balance after each transaction
- AND system MUST calculate current balance (sum of all debits - sum of all credits)
- AND system MUST support filtering:
  - By date range
  - By transaction type
  - By tax year
- AND system MUST support export to PDF and CSV
- AND system MUST display payment method for each payment (credit card, ACH, check, wire)

---

### US-5: Generate Trial Balance for Municipality (P3 - Future)

**User Story:**  
As a municipality accountant, I want to generate a trial balance showing all general ledger accounts with debit and credit balances, verify that total debits equal total credits ($5M = $5M), and ensure the books are balanced before closing the month, so that I can prepare accurate financial statements.

**Business Context:**  
Trial balance is accounting 101: list all accounts with debit/credit balances, sum them, verify debits = credits. If unbalanced, there's an error in journal entries. Trial balance is foundation for balance sheet and income statement.

Example:
```
Account                      | Debit      | Credit
-----------------------------|------------|------------
Cash                         | $1,000,000 |
Accounts Receivable          | $2,500,000 |
Tax Revenue                  |            | $3,000,000
Refunds Payable              |            | $500,000
TOTAL                        | $3,500,000 | $3,500,000
```

**Acceptance Criteria:**
- GIVEN municipality general ledger with journal entries
- WHEN generating trial balance
- THEN system MUST list all accounts with:
  - Account number and name
  - Total debit balance (sum of all debit entries for account)
  - Total credit balance (sum of all credit entries for account)
  - Net balance (debit - credit)
- AND system MUST calculate:
  - Total of all debit balances
  - Total of all credit balances
  - Difference (should be $0)
- AND system MUST flag if unbalanced:
  - "Trial balance does not balance. Total debits $3,500,000 ≠ Total credits $3,499,950. Difference: $50."
- AND system MUST support filtering by date range (month-end, quarter-end, year-end)
- AND system MUST display account hierarchy (asset accounts, liability accounts, revenue accounts, expense accounts)

---

### US-6: Process Refund with Ledger Entries (P2 - High Value)

**User Story:**  
As a filer who overpaid $1,000, I want to request a refund, have the system create double-entry ledger entries (DEBIT Filer Receivable from Municipality $1K, CREDIT Filer Cash $1K on my books; DEBIT Municipality Refund Expense $1K, CREDIT Municipality Cash $1K on municipality books), and receive the refund via ACH, so that my books accurately reflect the refund and reconcile with the municipality.

**Business Context:**  
Refunds reverse the payment flow:
- Filer books: DEBIT Refund Receivable (asset increases), CREDIT Cash (asset decreases when paid)
- Municipality books: DEBIT Refund Expense (expense increases), CREDIT Cash (asset decreases)

Refund ledger entries mirror payment ledger entries in reverse.

**Independent Test:**  
- Filer paid $11,000 for Q1 2024 tax, actual tax due was $10,000
- Overpayment: $1,000
- Filer requests refund on 05/01/2024

**Filer Books (Entry #1):**
- Date: 2024-05-01
- Description: "Refund Request - Overpayment Q1 2024"
- DEBIT: Account 1300 (Refund Receivable) $1,000
- CREDIT: Account 2100 (Tax Liability) -$1,000 (reduces overpayment)

**Municipality Books (Entry #2):**
- Date: 2024-05-01
- Description: "Refund to Business XYZ - Overpayment"
- DEBIT: Account 5200 (Refund Expense) $1,000
- CREDIT: Account 2200 (Refunds Payable) $1,000

**When refund issued (05/10/2024):**

**Filer Books:**
- DEBIT: Account 1000 (Cash) $1,000
- CREDIT: Account 1300 (Refund Receivable) $1,000

**Municipality Books:**
- DEBIT: Account 2200 (Refunds Payable) $1,000
- CREDIT: Account 1000 (Cash) $1,000

**Acceptance Criteria:**
- GIVEN filer with overpayment (payments exceed tax liability)
- WHEN filer requests refund
- THEN system MUST validate refund amount ≤ overpayment
- AND system MUST create journal entry on filer books:
  - DEBIT: Refund Receivable (asset increases)
  - CREDIT: Tax Liability (liability decreases, may go negative)
- AND system MUST create journal entry on municipality books:
  - DEBIT: Refund Expense (expense increases)
  - CREDIT: Refunds Payable (liability increases)
- AND when refund issued:
  - Filer books: DEBIT Cash, CREDIT Refund Receivable
  - Municipality books: DEBIT Refunds Payable, CREDIT Cash
- AND system MUST support refund methods (ACH, check, wire)
- AND system MUST generate refund confirmation with transaction ID

---

### US-7: Audit Trail of All Ledger Entries (P1 - Critical)

**User Story:**  
As an auditor, I want to view the complete audit trail for a specific payment (who created the entry, when, original amount, any adjustments, approval status), so that I can verify the payment is legitimate and trace it back to the source transaction.

**Business Context:**  
Ledger entries must be immutable once posted (no deletion, only reversing entries). Audit trail tracks:
- Who created the entry (user ID, timestamp)
- Source transaction (payment ID, return ID, adjustment ID)
- Entry status (draft, posted, reversed)
- Any modifications (adjustments, corrections)
- Approvals (if required for large amounts)

This is essential for SOX compliance, fraud prevention, audit defense.

**Independent Test:**  
- Payment $10,000 made on 04/25/2024 by User ID: filer123
- Journal Entry #JE-2024-00125 created by System on 04/25 at 10:32 AM
- Entry posted by Finance Officer on 04/25 at 2:15 PM
- Adjustment -$100 on 04/30 (refund for overpayment) by User ID: admin456
- Reversing entry on 04/30
- Audit trail shows all 5 events with timestamps, user IDs, amounts

**Acceptance Criteria:**
- GIVEN journal entry in ledger
- WHEN viewing audit trail
- THEN system MUST display:
  - Entry creation: User ID, timestamp, source transaction
  - Entry posting: User ID, timestamp (when moved from draft to posted)
  - Entry modifications: User ID, timestamp, old value, new value, reason
  - Entry reversal: User ID, timestamp, reversing entry ID, reason
  - Entry approvals: Approver user ID, timestamp, approval status
- AND system MUST display source transaction details:
  - Payment ID (link to Payment record)
  - Return ID (link to TaxReturn record)
  - Adjustment ID (link to Adjustment record)
- AND system MUST prevent deletion of posted entries (enforce immutability)
- AND system MUST support reversing entries only (to correct errors)
- AND system MUST log all access to ledger entries (who viewed, when)

---

## Functional Requirements

### Mock Payment Provider

**FR-001:** System MUST provide mock payment provider API supporting test mode (no real charges)

**FR-002:** System MUST accept test credit cards:
- 4111-1111-1111-1111 → APPROVED (Visa)
- 4000-0000-0000-0002 → DECLINED (card declined)
- 4000-0000-0000-0119 → ERROR (processing error)
- 5555-5555-5555-4444 → APPROVED (Mastercard)
- 3782-822463-10005 → APPROVED (Amex)

**FR-003:** System MUST accept test ACH accounts:
- Routing: 110000000, Account: 000123456789 → APPROVED
- Routing: 110000000, Account: 000111111113 → DECLINED (insufficient funds)

**FR-004:** System MUST return mock transaction response:
- Status: APPROVED | DECLINED | ERROR
- Transaction ID: mock_ch_[random] or mock_ach_[random]
- Authorization code: mock_auth_[random]
- Amount, Currency, Timestamp
- Failure reason (if declined): "insufficient_funds" | "invalid_card" | "expired_card"

**FR-005:** System MUST process approved payments instantly (no delay, no async webhook)

**FR-006:** System MUST display test mode indicator: "TEST MODE: No real charges" on payment screen

**FR-007:** System MUST support payment methods:
- Credit Card (test cards only in test mode)
- ACH/eCheck (test accounts only in test mode)
- Check (manual entry, mark as cleared)
- Wire Transfer (manual entry, mark as cleared)

**FR-008:** System MUST support toggling between test mode and production mode (admin only)

### Chart of Accounts

**FR-009:** System MUST provide chart of accounts for filers:
- 1000: Cash
- 1200: Refund Receivable
- 2100: Tax Liability (current year)
- 2110: Tax Liability (prior years)
- 2120: Penalty Liability
- 2130: Interest Liability
- 6100: Tax Expense (income statement account)

**FR-010:** System MUST provide chart of accounts for municipalities:
- 1000: Cash
- 1200: Accounts Receivable (taxes due from filers)
- 2200: Refunds Payable
- 4100: Tax Revenue
- 4200: Penalty Revenue
- 4300: Interest Revenue
- 5200: Refund Expense

**FR-011:** System MUST support custom accounts (configurable by municipality)

**FR-012:** System MUST categorize accounts by type:
- ASSET (debit balance normal)
- LIABILITY (credit balance normal)
- REVENUE (credit balance normal)
- EXPENSE (debit balance normal)

### Double-Entry Journal Entries

**FR-013:** System MUST create double-entry journal entries for all transactions

**FR-014:** System MUST validate all entries: Total debits = Total credits (if unequal, reject entry)

**FR-015:** System MUST assign unique journal entry number (JE-YYYY-#####)

**FR-016:** System MUST create journal entries for:
- Tax assessment (when return filed)
- Payment (when payment processed)
- Refund (when refund requested and issued)
- Adjustment (when correction made)
- Penalty (when penalty assessed)
- Interest (when interest calculated)
- Write-off (when uncollectible)

**FR-017:** System MUST link journal entries to source transactions:
- Entry references Return ID, Payment ID, Adjustment ID, etc.
- Bidirectional link (payment → entry, entry → payment)

**FR-018:** System MUST support compound journal entries (more than 2 lines):
- Example: Payment $10,000 applied to Tax $9,000 + Penalty $500 + Interest $500
- DEBIT: Cash $10,000
- CREDIT: Tax Liability $9,000
- CREDIT: Penalty Liability $500
- CREDIT: Interest Liability $500

### Ledger Balances

**FR-019:** System MUST maintain running balance for each account

**FR-020:** System MUST calculate balance correctly based on account type:
- ASSET / EXPENSE: Debit increases, Credit decreases
- LIABILITY / REVENUE: Credit increases, Debit decreases

**FR-021:** System MUST support account balance inquiry:
- Current balance (as of today)
- Historical balance (as of specific date)
- Beginning balance (start of period)
- Ending balance (end of period)

**FR-022:** System MUST display account activity (all entries affecting the account)

### Filer Account Statement

**FR-023:** System MUST generate filer account statement showing:
- All transactions (tax assessments, payments, refunds, penalties, interest)
- Transaction date, description, debit, credit, running balance
- Current balance (amount owed or overpaid)

**FR-024:** System MUST support filtering by date range, tax year, transaction type

**FR-025:** System MUST export statement to PDF and CSV

**FR-026:** System MUST calculate aging of balance (0-30 days, 31-60, 61-90, 90+ days overdue)

### Two-Way Reconciliation

**FR-027:** System MUST reconcile filer ledgers to municipality ledger:
- Total Filer Tax Liabilities = Municipality Accounts Receivable
- Total Filer Payments = Municipality Cash Receipts

**FR-028:** System MUST generate reconciliation report showing:
- Municipality AR total, Filer Liability total, Variance
- Municipality Cash total, Filer Payment total, Variance
- Reconciliation status: RECONCILED | DISCREPANCY
- List of discrepancies (if any)

**FR-029:** System MUST support drill-down reconciliation:
- Select specific filer
- Compare filer's books to municipality's entries for that filer
- Identify unmatched transactions

**FR-030:** System MUST flag discrepancies:
- Payment recorded by filer but not municipality
- Payment recorded by municipality but not filer
- Amount mismatch (filer shows $1,000, municipality shows $1,050)

### Trial Balance

**FR-031:** System MUST generate trial balance for municipality general ledger

**FR-032:** System MUST list all accounts with debit balance, credit balance, net balance

**FR-033:** System MUST calculate total debits, total credits, difference

**FR-034:** System MUST flag if unbalanced (debits ≠ credits)

**FR-035:** System MUST support trial balance as of specific date (month-end, quarter-end, year-end)

### Refund Processing

**FR-036:** System MUST detect overpayments (payments > tax liability)

**FR-037:** System MUST allow filer to request refund (amount ≤ overpayment)

**FR-038:** System MUST create journal entries for refund:
- Filer books: DEBIT Refund Receivable, CREDIT Tax Liability
- Municipality books: DEBIT Refund Expense, CREDIT Refunds Payable

**FR-039:** System MUST support refund approval workflow (municipality approves refund)

**FR-040:** When refund issued:
- Filer books: DEBIT Cash, CREDIT Refund Receivable
- Municipality books: DEBIT Refunds Payable, CREDIT Cash

**FR-041:** System MUST support refund methods: ACH, Check, Wire Transfer

**FR-042:** System MUST generate refund confirmation with transaction ID

### Payment Allocation

**FR-043:** System MUST allocate payments to liabilities in this order (by default):
1. Interest (oldest first)
2. Penalties (oldest first)
3. Tax (oldest year first)

**FR-044:** System MUST support custom allocation (filer specifies: "Apply to Q1 2024 tax only")

**FR-045:** System MUST handle partial payments:
- If filer owes $10,000 but pays $5,000, apply per allocation rules
- Update each liability balance (e.g., Tax Liability reduced by $5,000)

**FR-046:** System MUST display payment allocation breakdown:
- Total payment: $10,000
- Applied to Tax: $9,000
- Applied to Penalty: $500
- Applied to Interest: $500

### Audit Trail

**FR-047:** System MUST log all ledger entry creation: User ID, timestamp, source transaction

**FR-048:** System MUST log all ledger entry modifications: User ID, timestamp, old value, new value, reason

**FR-049:** System MUST prevent deletion of posted entries (immutability)

**FR-050:** System MUST support reversing entries only (to correct errors):
- Create new entry with opposite debit/credit
- Link to original entry (reversal of JE-2024-00125)

**FR-051:** System MUST log all access to ledger (who viewed, when)

### Financial Reporting

**FR-052:** System MUST generate municipality financial reports:
- Balance Sheet (Assets, Liabilities, Equity)
- Income Statement (Revenue, Expenses, Net Income)
- Cash Flow Statement (Operating, Investing, Financing)

**FR-053:** System MUST generate aging report for municipality:
- Total AR by age bucket (0-30, 31-60, 61-90, 90+ days)
- List of filers in each bucket

**FR-054:** System MUST generate cash receipts report:
- Total cash received by date, by filer, by payment method
- Daily/Monthly/Quarterly summaries

**FR-055:** System MUST export all reports to PDF, CSV, Excel

---

## Key Entities

### MockPaymentProvider

**Attributes:**
- `providerId` (UUID)
- `providerName` (string): "Mock Payment Gateway"
- `mode` (enum): TEST | PRODUCTION
- `supportedMethods` (array): [CREDIT_CARD, ACH, CHECK, WIRE]
- `testCards` (JSON): List of test card numbers and expected outcomes

### PaymentTransaction

**Attributes:**
- `transactionId` (UUID)
- `paymentId` (UUID): Foreign key to Payment
- `providerTransactionId` (string): mock_ch_abc123 or mock_ach_xyz789
- `status` (enum): APPROVED | DECLINED | ERROR | PENDING
- `paymentMethod` (enum): CREDIT_CARD | ACH | CHECK | WIRE
- `amount` (decimal)
- `currency` (string): USD
- `authorizationCode` (string)
- `failureReason` (string): If declined, reason code
- `timestamp` (datetime)

### ChartOfAccounts

**Attributes:**
- `accountId` (UUID)
- `accountNumber` (string): "1000", "2100", "4100"
- `accountName` (string): "Cash", "Tax Liability", "Tax Revenue"
- `accountType` (enum): ASSET | LIABILITY | REVENUE | EXPENSE
- `normalBalance` (enum): DEBIT | CREDIT
- `parentAccountId` (UUID): For sub-accounts (e.g., 2110 under 2100)
- `tenantId` (UUID): Multi-tenant support (each municipality has own chart)

### JournalEntry

**Attributes:**
- `entryId` (UUID)
- `entryNumber` (string): "JE-2024-00125"
- `entryDate` (date)
- `description` (string): "Q1 2024 Tax Assessment"
- `sourceType` (enum): TAX_ASSESSMENT | PAYMENT | REFUND | ADJUSTMENT | PENALTY | INTEREST
- `sourceId` (UUID): Return ID, Payment ID, etc.
- `status` (enum): DRAFT | POSTED | REVERSED
- `createdBy` (UUID): User ID
- `createdAt` (datetime)
- `postedBy` (UUID): User ID who posted entry
- `postedAt` (datetime)
- `reversedBy` (UUID): If reversed, user ID
- `reversalEntryId` (UUID): Link to reversing entry
- `lines` (array): List of JournalEntryLine objects

### JournalEntryLine

**Attributes:**
- `lineId` (UUID)
- `entryId` (UUID): Foreign key to JournalEntry
- `accountId` (UUID): Foreign key to ChartOfAccounts
- `lineNumber` (number): 1, 2, 3 (order within entry)
- `debit` (decimal): Amount if debit, else 0
- `credit` (decimal): Amount if credit, else 0
- `description` (string): Line-specific description

### AccountBalance

**Attributes:**
- `balanceId` (UUID)
- `accountId` (UUID): Foreign key to ChartOfAccounts
- `entityId` (UUID): Filer or Municipality
- `periodStartDate` (date)
- `periodEndDate` (date)
- `beginningBalance` (decimal)
- `totalDebits` (decimal): Sum of debits in period
- `totalCredits` (decimal): Sum of credits in period
- `endingBalance` (decimal): Beginning + Debits - Credits (for asset/expense) or Beginning + Credits - Debits (for liability/revenue)

### FilerAccountStatement

**Attributes:**
- `statementId` (UUID)
- `filerId` (UUID): Foreign key to Business
- `statementDate` (date)
- `beginningBalance` (decimal)
- `transactions` (array): List of StatementTransaction objects
- `endingBalance` (decimal)
- `totalDebits` (decimal): Tax, penalties, interest assessed
- `totalCredits` (decimal): Payments, refunds received

### StatementTransaction

**Attributes:**
- `transactionDate` (date)
- `transactionType` (enum): TAX_ASSESSMENT | PAYMENT | REFUND | PENALTY | INTEREST | ADJUSTMENT
- `description` (string): "Q1 2024 Tax", "Credit Card Payment"
- `debitAmount` (decimal): Increases balance owed
- `creditAmount` (decimal): Decreases balance owed
- `runningBalance` (decimal): Balance after this transaction

### ReconciliationReport

**Attributes:**
- `reportId` (UUID)
- `reportDate` (date)
- `municipalityId` (UUID)
- `totalMunicipalityAR` (decimal): Sum of all accounts receivable
- `totalFilerLiabilities` (decimal): Sum of all filer tax liabilities
- `arVariance` (decimal): AR - Liabilities
- `totalMunicipalityCash` (decimal): Sum of all cash receipts
- `totalFilerPayments` (decimal): Sum of all filer payments
- `cashVariance` (decimal): Cash - Payments
- `reconciliationStatus` (enum): RECONCILED | DISCREPANCY
- `discrepancies` (array): List of Discrepancy objects

### Discrepancy

**Attributes:**
- `discrepancyId` (UUID)
- `filerId` (UUID)
- `filerName` (string)
- `transactionType` (string): "Payment"
- `transactionDate` (date)
- `filerAmount` (decimal): Amount on filer books
- `municipalityAmount` (decimal): Amount on municipality books
- `variance` (decimal): Filer - Municipality
- `description` (string): "Filer shows $5K payment not recorded by municipality"

---

## Success Criteria

- **Payment Processing:** 100% of test mode payments processed instantly with mock transaction IDs (no real charges)
- **Ledger Accuracy:** 100% of journal entries balance (total debits = total credits, zero tolerance)
- **Reconciliation:** Filer and municipality ledgers reconcile with zero variance (or all variances explained and resolved within 5 business days)
- **Audit Compliance:** Complete audit trail for 100% of ledger entries (who, when, what, why)
- **User Adoption:** Filers use account statement feature to verify balances (90% of filers view statement at least once per year)

---

## Assumptions

- Mock payment provider sufficient for development, testing, demos (real payment gateway integrated separately in production)
- Most filers have simple account activity (1-4 transactions per quarter)
- Municipality accountant has basic accounting knowledge (understands debits, credits, trial balance)
- Double-entry bookkeeping standard is universally accepted (not controversial)
- Refunds processed manually (ACH/check issued outside system, system records transaction)

---

## Dependencies

- **Payment Gateway (Spec 12 - this spec):** Mock provider standalone, real gateway integration separate project
- **Business Form Library (Spec 8):** Generate payment vouchers with account details
- **Auditor Workflow (Spec 9):** Audit trail integration (auditors view ledger entries)
- **Enhanced Penalty/Interest (Spec 7):** Penalties and interest recorded as journal entries

---

## Out of Scope

- **Real payment gateway integration:** Stripe, Square, PayPal (separate implementation, outside this spec)
- **Real ACH processing:** Plaid, Dwolla (separate implementation)
- **Check scanning:** Physical check deposit via mobile app (manual entry only)
- **Multi-currency support:** USD only (Ohio municipalities)
- **GAAP compliance:** Full Generally Accepted Accounting Principles (simplified for municipal tax)
- **Automated bank reconciliation:** Matching bank statements to ledger (manual only)

---

## Edge Cases

1. **Overpayment exceeds tax due:** Filer pays $15,000 for Q1 tax of $10,000. System records payment, creates $5,000 credit balance (negative liability). Filer can request $5,000 refund or apply to future tax.

2. **Payment allocation with multiple liabilities:** Filer owes $5K tax, $1K penalty, $200 interest (total $6,200). Filer pays $1,000. System allocates: $200 to interest (oldest), $800 to penalty. Tax liability remains $5K. Filer still owes $5,200.

3. **Refund exceeds cash balance:** Municipality has $10K cash, but 20 filers request $1K refunds each (total $20K refunds). System flags: "Insufficient cash to process all refunds. Prioritize or wait for new tax receipts."

4. **Journal entry unbalanced due to rounding:** Tax calculation $10,000.333 rounded to $10,000.33. Penalty calculation $50.666 rounded to $50.67. Total: $10,051.00. But individual entries sum to $10,051.003. System flags: "Entry out of balance by $0.003. Round to nearest cent."

5. **Reversing entry for incorrect payment amount:** Filer paid $10,000, recorded correctly, but payment was actually $9,000 (typo). Accountant creates reversing entry (reverse $10,000 payment), then creates new entry ($9,000 payment). Net effect: -$1,000 payment adjustment.

6. **Test card declined:** Filer uses test card 4000-0000-0000-0002 (always declines). System displays: "Payment declined: insufficient_funds. Please try another card." No journal entries created (payment failed).

7. **Trial balance out of balance:** Accountant runs trial balance, sees debits $3,500,000, credits $3,499,950 (difference $50). System lists last 10 entries to identify error. Found: Entry JE-2024-00098 has $50 credit missing (only 1 line instead of 2). Accountant posts correcting entry.

8. **Reconciliation discrepancy - timing:** Filer records payment on 04/25 (end of day), municipality receives bank notification on 04/26 (next day). Reconciliation run on 04/25 shows discrepancy. Run on 04/26 shows reconciled. System notes: "Timing difference: Filer records payment date, municipality records receipt date."
