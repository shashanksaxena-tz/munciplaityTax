# Ledger Service - Double-Entry Accounting System

## Overview

The Ledger Service implements a complete double-entry accounting system for tracking tax payments, liabilities, refunds, and adjustments. It includes a mock payment provider for testing payment workflows without processing real transactions.

## Features

### 1. Double-Entry Bookkeeping
- Every transaction creates balanced journal entries (debits = credits)
- Automatic validation ensures entries are balanced before posting
- Immutable ledger entries (reversals only, no deletions)
- Complete audit trail for all transactions

### 2. Mock Payment Provider
- Simulates credit card, ACH, check, and wire payments in TEST mode
- Test cards for various scenarios:
  - `4111-1111-1111-1111` → APPROVED (Visa)
  - `4000-0000-0000-0002` → DECLINED (insufficient funds)
  - `4000-0000-0000-0119` → ERROR (processing error)
- Test ACH accounts:
  - Routing: `110000000`, Account: `000123456789` → APPROVED
  - Routing: `110000000`, Account: `000111111113` → DECLINED
- No real charges processed in TEST mode

### 3. Chart of Accounts

#### Filer Accounts
- `1000` - Cash
- `1200` - Refund Receivable
- `2100` - Tax Liability (Current Year)
- `2110` - Tax Liability (Prior Years)
- `2120` - Penalty Liability
- `2130` - Interest Liability
- `6100` - Tax Expense

#### Municipality Accounts
- `1001` - Cash
- `1201` - Accounts Receivable
- `2200` - Refunds Payable
- `4100` - Tax Revenue
- `4200` - Penalty Revenue
- `4300` - Interest Revenue
- `5200` - Refund Expense

### 4. Journal Entries
- Auto-generated entry numbers (JE-YYYY-#####)
- Support for compound entries (multiple debits/credits)
- Links to source transactions (returns, payments, adjustments)
- Reversal entries for corrections

### 5. Account Statements
- Filer account statements with running balances
- Transaction history with date, type, description
- Filter by date range and transaction type
- Export to PDF and CSV (planned)

### 6. Reconciliation
- Two-way reconciliation between filer and municipality books
- Variance detection and reporting
- Drill-down to discrepancies
- Balance verification

### 7. Audit Trail
- Complete logging of all ledger actions
- User attribution for all changes
- Timestamp tracking
- Modification history

## API Endpoints

### Payments
- `POST /api/v1/payments/process` - Process payment (credit card, ACH, etc.)
- `GET /api/v1/payments/filer/{filerId}` - Get filer payment history
- `GET /api/v1/payments/{paymentId}` - Get payment details
- `GET /api/v1/payments/test-mode-indicator` - Check test mode status

### Journal Entries
- `POST /api/v1/journal-entries` - Create journal entry
- `POST /api/v1/journal-entries/{entryId}/reverse` - Reverse entry
- `GET /api/v1/journal-entries/entity/{tenantId}/{entityId}` - Get entity entries
- `GET /api/v1/journal-entries/{entryId}` - Get entry details

### Tax Assessments
- `POST /api/v1/tax-assessments/record` - Record tax assessment with ledger entries

### Refunds
- `POST /api/v1/refunds/request` - Request refund
- `POST /api/v1/refunds/issue` - Issue refund

### Account Statements
- `GET /api/v1/statements/filer/{tenantId}/{filerId}` - Generate filer statement

### Reconciliation
- `GET /api/v1/reconciliation/report/{tenantId}/{municipalityId}` - Generate reconciliation report

### Audit
- `GET /api/v1/audit/entity/{entityId}` - Get audit trail for entity
- `GET /api/v1/audit/tenant/{tenantId}` - Get tenant audit logs

## Usage Examples

### 1. Process Test Payment

```bash
curl -X POST http://localhost:8087/api/v1/payments/process \
  -H "Content-Type: application/json" \
  -d '{
    "filerId": "123e4567-e89b-12d3-a456-426614174000",
    "tenantId": "123e4567-e89b-12d3-a456-426614174001",
    "amount": 5000.00,
    "paymentMethod": "CREDIT_CARD",
    "cardNumber": "4111-1111-1111-1111",
    "cardExpiration": "12/25",
    "cardCvv": "123",
    "description": "Q1 2024 Tax Payment"
  }'
```

### 2. Record Tax Assessment

```bash
curl -X POST "http://localhost:8087/api/v1/tax-assessments/record" \
  -d "tenantId=123e4567-e89b-12d3-a456-426614174001" \
  -d "filerId=123e4567-e89b-12d3-a456-426614174000" \
  -d "returnId=123e4567-e89b-12d3-a456-426614174002" \
  -d "taxAmount=10000.00" \
  -d "penaltyAmount=50.00" \
  -d "interestAmount=25.00" \
  -d "taxYear=2024" \
  -d "taxPeriod=Q1"
```

### 3. Get Filer Statement

```bash
curl "http://localhost:8087/api/v1/statements/filer/{tenantId}/{filerId}?startDate=2024-01-01&endDate=2024-12-31"
```

### 4. Request Refund

```bash
curl -X POST "http://localhost:8087/api/v1/refunds/request" \
  -d "tenantId=123e4567-e89b-12d3-a456-426614174001" \
  -d "filerId=123e4567-e89b-12d3-a456-426614174000" \
  -d "amount=1000.00" \
  -d "reason=Overpayment Q1 2024" \
  -d "requestedBy=123e4567-e89b-12d3-a456-426614174000"
```

## Configuration

### application.properties

```properties
spring.application.name=ledger-service
server.port=8087

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/munitax
spring.datasource.username=postgres
spring.datasource.password=postgres

# Payment Provider
ledger.payment.mode=TEST  # TEST or PRODUCTION
ledger.payment.provider=MOCK
```

## Database Schema

The service uses PostgreSQL with Flyway migrations. Tables include:
- `chart_of_accounts` - Account definitions
- `journal_entries` - Journal entry headers
- `journal_entry_lines` - Journal entry line items
- `payment_transactions` - Payment transaction records
- `account_balances` - Account balance snapshots
- `audit_logs` - Audit trail

## Testing

The service includes unit tests for:
- Mock payment provider (various card scenarios)
- Journal entry validation (balanced/unbalanced)
- Double-entry bookkeeping rules

Run tests:
```bash
cd backend/ledger-service
mvn test
```

## Success Criteria

✅ All journal entries balance (debits = credits)  
✅ Test payments process instantly with mock transaction IDs  
✅ Complete audit trail for all ledger activities  
✅ Reconciliation reports identify discrepancies  
✅ Refund workflow with proper reversing entries  

## Future Enhancements

- Real payment gateway integration (Stripe, ACH)
- Trial balance reports
- Financial statements (balance sheet, income statement)
- Multi-currency support
- Automated bank reconciliation
