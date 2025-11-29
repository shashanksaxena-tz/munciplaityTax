# Testing Guide - Ledger Service

This guide explains how to test the Ledger Service using the mock payment provider.

## Table of Contents

1. [Overview](#overview)
2. [Test Mode](#test-mode)
3. [Test Credit Cards](#test-credit-cards)
4. [Test ACH Accounts](#test-ach-accounts)
5. [Test Scenarios](#test-scenarios)
6. [Integration Testing](#integration-testing)
7. [Performance Testing](#performance-testing)

---

## Overview

The Ledger Service includes a **Mock Payment Provider** that simulates real payment processing without charging real money. This is similar to Stripe's test mode and is perfect for:

- Development and testing
- Integration testing
- Demonstrations and training
- CI/CD pipeline testing

## Test Mode

The system is in **TEST MODE** by default. All payments processed in test mode:
- ✅ Create real journal entries
- ✅ Update account balances
- ✅ Generate receipts and audit trails
- ❌ Do NOT charge real money
- ❌ Do NOT connect to real payment providers

### Checking Test Mode Status

```bash
curl http://localhost:8080/api/v1/payments/test-mode-indicator
```

Response:
```
TEST MODE: No real charges will be processed
```

---

## Test Credit Cards

Use these test card numbers to simulate different scenarios:

### Approved Transactions

| Card Number | Brand | Result | Use Case |
|------------|-------|--------|----------|
| `4111-1111-1111-1111` | Visa | Approved | Standard successful payment |
| `5555-5555-5555-4444` | Mastercard | Approved | Mastercard payment |
| `378282246310005` | American Express | Approved | AMEX payment |
| `6011-1111-1111-1117` | Discover | Approved | Discover payment |

### Declined Transactions

| Card Number | Brand | Result | Reason |
|------------|-------|--------|--------|
| `4000-0000-0000-0002` | Visa | Declined | Generic decline |
| `4000-0000-0000-0127` | Visa | Declined | Insufficient funds |
| `4000-0000-0000-0069` | Visa | Declined | Expired card |
| `4000-0000-0000-0119` | Visa | Declined | Processing error |

### Special Cases

| Card Number | Result | Scenario |
|------------|--------|----------|
| `4000-0000-0000-9235` | Error | Network timeout |
| `4242-4242-4242-4242` | Approved | Stripe-compatible test card |

### Card Details for Testing

For all test cards, use:
- **Expiration Date**: Any future date (e.g., `12/25`, `06/28`)
- **CVV**: Any 3 digits (e.g., `123`, `456`)
- **ZIP Code**: Any valid ZIP (e.g., `12345`, `90210`)

---

## Test ACH Accounts

Use these test ACH account details:

### Approved ACH Transactions

| Routing Number | Account Number | Bank | Result |
|---------------|----------------|------|--------|
| `021000021` | `TEST123456789` | Chase | Approved |
| `011401533` | `TEST987654321` | Wells Fargo | Approved |
| `026009593` | `TEST555555555` | Bank of America | Approved |

### Declined ACH Transactions

| Routing Number | Account Number | Bank | Result |
|---------------|----------------|------|--------|
| `021000021` | `DECLINE123456` | Chase | Declined - Insufficient funds |
| `011401533` | `DECLINE789012` | Wells Fargo | Declined - Invalid account |

---

## Test Scenarios

### Scenario 1: Successful Credit Card Payment

```bash
curl -X POST http://localhost:8080/api/v1/payments/process \
  -H "Content-Type: application/json" \
  -d '{
    "filerId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "amount": 5000.00,
    "paymentMethod": "CREDIT_CARD",
    "cardNumber": "4111-1111-1111-1111",
    "expirationDate": "12/25",
    "cvv": "123",
    "description": "Q1 2024 Tax Payment"
  }'
```

**Expected Result**:
- Status: `APPROVED`
- Transaction ID starts with `mock_ch_`
- Journal entries created on both filer and municipality books
- `testMode: true` in response

### Scenario 2: Declined Credit Card Payment

```bash
curl -X POST http://localhost:8080/api/v1/payments/process \
  -H "Content-Type: application/json" \
  -d '{
    "filerId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "amount": 5000.00,
    "paymentMethod": "CREDIT_CARD",
    "cardNumber": "4000-0000-0000-0002",
    "expirationDate": "12/25",
    "cvv": "123",
    "description": "Q1 2024 Tax Payment"
  }'
```

**Expected Result**:
- Status: `DECLINED`
- No journal entries created
- Error message in response

### Scenario 3: Successful ACH Payment

```bash
curl -X POST http://localhost:8080/api/v1/payments/process \
  -H "Content-Type: application/json" \
  -d '{
    "filerId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "amount": 10000.00,
    "paymentMethod": "ACH",
    "accountNumber": "TEST123456789",
    "routingNumber": "021000021",
    "description": "Q2 2024 Tax Payment"
  }'
```

**Expected Result**:
- Status: `APPROVED`
- Transaction ID starts with `mock_ach_`
- Journal entries created
- `testMode: true` in response

### Scenario 4: Complete Tax Payment Flow

```bash
# Step 1: Create tax assessment
curl -X POST http://localhost:8080/api/v1/tax-assessments \
  -H "Content-Type: application/json" \
  -d '{
    "filerId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "taxReturnId": "TR-2024-Q1-12345",
    "taxAmount": 10000.00,
    "penaltyAmount": 500.00,
    "interestAmount": 150.00,
    "assessmentDate": "2024-04-20",
    "taxYear": "2024",
    "taxPeriod": "Q1"
  }'

# Step 2: Make payment
curl -X POST http://localhost:8080/api/v1/payments/process \
  -H "Content-Type: application/json" \
  -d '{
    "filerId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "amount": 10650.00,
    "paymentMethod": "CREDIT_CARD",
    "cardNumber": "4111-1111-1111-1111",
    "taxReturnId": "TR-2024-Q1-12345"
  }'

# Step 3: Get account statement
curl "http://localhost:8080/api/v1/account-statements/550e8400-e29b-41d4-a716-446655440000?startDate=2024-01-01&endDate=2024-12-31"

# Step 4: Run reconciliation
curl -X POST http://localhost:8080/api/v1/reconciliation \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "asOfDate": "2024-04-30"
  }'
```

**Expected Result**:
- Assessment creates 2 journal entries (filer + municipality)
- Payment creates 2 more journal entries
- Account statement shows both transactions with $0 balance
- Reconciliation shows balanced books

### Scenario 5: Overpayment and Refund

```bash
# Step 1: Assess $5,000 tax
curl -X POST http://localhost:8080/api/v1/tax-assessments \
  -H "Content-Type: application/json" \
  -d '{
    "filerId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "taxReturnId": "TR-2024-Q1-12345",
    "taxAmount": 5000.00,
    "assessmentDate": "2024-04-20"
  }'

# Step 2: Pay $6,000 (overpayment)
curl -X POST http://localhost:8080/api/v1/payments/process \
  -H "Content-Type: application/json" \
  -d '{
    "filerId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "amount": 6000.00,
    "paymentMethod": "CREDIT_CARD",
    "cardNumber": "4111-1111-1111-1111"
  }'

# Step 3: Request refund of $1,000
curl -X POST http://localhost:8080/api/v1/refunds/request \
  -H "Content-Type: application/json" \
  -d '{
    "filerId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "amount": 1000.00,
    "refundMethod": "ACH",
    "accountNumber": "TEST123456789",
    "routingNumber": "021000021"
  }'
```

**Expected Result**:
- Account statement shows -$1,000 balance (credit)
- Refund request creates journal entries
- Refund workflow: REQUESTED → APPROVED → ISSUED

---

## Integration Testing

### Running Integration Tests

```bash
# Run all integration tests
mvn test -Dtest=*IntegrationTest

# Run specific integration test
mvn test -Dtest=PaymentFlowIntegrationTest
mvn test -Dtest=RefundFlowIntegrationTest
mvn test -Dtest=ReconciliationIntegrationTest
```

### Test Coverage

The integration tests cover:
- ✅ End-to-end payment flow (assessment → payment → statement → reconciliation)
- ✅ Refund lifecycle (overpayment → request → approval → issuance)
- ✅ Multi-filer reconciliation accuracy
- ✅ Audit trail completeness

---

## Performance Testing

### Running Performance Tests

Performance tests are disabled by default. To run them:

```bash
mvn test -Dtest=TrialBalancePerformanceTest -Dperformance.tests.enabled=true
```

### Performance Benchmarks

| Operation | Dataset Size | Target Time | Acceptable Time |
|-----------|-------------|-------------|-----------------|
| Trial Balance | 10,000 entries | < 5 seconds | < 30 seconds |
| Reconciliation | 100 filers | < 10 seconds | < 60 seconds |
| Account Statement | 1,000 transactions | < 2 seconds | < 10 seconds |
| Payment Processing | Single payment | < 500ms | < 2 seconds |

---

## Debugging Tips

### Enable Debug Logging

Add to `application.properties`:
```properties
logging.level.com.munitax.ledger=DEBUG
logging.level.org.springframework.web=DEBUG
```

### View Journal Entries

```bash
curl "http://localhost:8080/api/v1/journal-entries?tenantId=660e8400-e29b-41d4-a716-446655440000"
```

### Check Audit Trail

```bash
curl "http://localhost:8080/api/v1/audit?tenantId=660e8400-e29b-41d4-a716-446655440000&startDate=2024-04-01"
```

### Verify Balance Integrity

```bash
curl "http://localhost:8080/api/v1/trial-balance?tenantId=660e8400-e29b-41d4-a716-446655440000&asOfDate=2024-04-30"
```

All debits should equal all credits. If not balanced, check audit logs for errors.

---

## Common Issues

### Issue: Payment approved but no journal entries created
**Solution**: Check AuditLogService for errors. Verify JournalEntryService is working.

### Issue: Reconciliation shows variance
**Solution**: Run trial balance first. Verify all journal entries are balanced.

### Issue: Refund validation fails
**Solution**: Check account statement to verify overpayment exists.

### Issue: Test card being declined when it shouldn't
**Solution**: Verify card number matches exactly (including dashes). Check test mode is enabled.

---

## Support

For testing support:
- Email: dev-support@munitax.com
- Slack: #ledger-service-help
- Documentation: See `/swagger-ui.html` for interactive API docs
