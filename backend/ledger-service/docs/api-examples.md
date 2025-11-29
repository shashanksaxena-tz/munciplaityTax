# API Examples - Ledger Service

This document provides practical examples for using the Ledger Service API.

## Table of Contents

1. [Authentication](#authentication)
2. [Payment Processing](#payment-processing)
3. [Tax Assessments](#tax-assessments)
4. [Refunds](#refunds)
5. [Account Statements](#account-statements)
6. [Reconciliation](#reconciliation)
7. [Trial Balance](#trial-balance)
8. [Audit Trail](#audit-trail)

## Base URL

- **Local Development**: `http://localhost:8080`
- **Staging**: `https://api-staging.munitax.com`
- **Production**: `https://api.munitax.com`

## Interactive Documentation

Swagger UI is available at: `/swagger-ui.html`  
OpenAPI JSON spec is available at: `/v3/api-docs`

---

## Authentication

All API requests require authentication (implementation depends on your auth setup).

```bash
# Example with Bearer token
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/api/v1/payments
```

---

## Payment Processing

### 1. Process a Credit Card Payment

**Endpoint**: `POST /api/v1/payments/process`

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
    "description": "Q1 2024 Tax Payment",
    "taxReturnId": "TR-2024-Q1-12345"
  }'
```

**Response**:
```json
{
  "transactionId": "770e8400-e29b-41d4-a716-446655440000",
  "providerTransactionId": "mock_ch_abc123def456",
  "status": "APPROVED",
  "amount": 5000.00,
  "journalEntryId": "880e8400-e29b-41d4-a716-446655440000",
  "testMode": true,
  "message": "Payment approved",
  "timestamp": "2024-04-25T10:32:15Z"
}
```

### 2. Process an ACH Payment

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

### 3. Test Card Numbers

- **4111-1111-1111-1111**: Visa - Always approved
- **4000-0000-0000-0002**: Visa - Always declined
- **5555-5555-5555-4444**: Mastercard - Approved
- **378282246310005**: American Express - Approved

### 4. Get Payment Receipt

**Endpoint**: `GET /api/v1/payments/{paymentId}/receipt`

```bash
curl http://localhost:8080/api/v1/payments/770e8400-e29b-41d4-a716-446655440000/receipt
```

**Response**:
```json
{
  "paymentId": "770e8400-e29b-41d4-a716-446655440000",
  "transactionId": "mock_ch_abc123def456",
  "filerId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 5000.00,
  "paymentDate": "2024-04-25",
  "paymentMethod": "CREDIT_CARD",
  "last4": "1111",
  "description": "Q1 2024 Tax Payment",
  "journalEntryNumber": "JE-2024-00125"
}
```

---

## Tax Assessments

### 1. Record a Tax Assessment

**Endpoint**: `POST /api/v1/tax-assessments`

```bash
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
    "taxPeriod": "Q1",
    "description": "Q1 2024 Tax Assessment"
  }'
```

**Response**:
```json
{
  "filerJournalEntryId": "990e8400-e29b-41d4-a716-446655440000",
  "municipalityJournalEntryId": "aa0e8400-e29b-41d4-a716-446655440000",
  "entryNumber": "JE-2024-00100",
  "filerId": "550e8400-e29b-41d4-a716-446655440000",
  "returnId": "TR-2024-Q1-12345",
  "assessmentDate": "2024-04-20",
  "taxAmount": 10000.00,
  "penaltyAmount": 500.00,
  "interestAmount": 150.00,
  "totalAmount": 10650.00,
  "message": "Tax assessment recorded successfully"
}
```

---

## Refunds

### 1. Request a Refund

**Endpoint**: `POST /api/v1/refunds/request`

```bash
curl -X POST http://localhost:8080/api/v1/refunds/request \
  -H "Content-Type: application/json" \
  -d '{
    "filerId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "amount": 1000.00,
    "refundMethod": "ACH",
    "accountNumber": "TEST123456789",
    "routingNumber": "021000021",
    "reason": "Overpayment refund for Q1 2024",
    "taxReturnId": "TR-2024-Q1-12345"
  }'
```

**Response**:
```json
{
  "refundId": "bb0e8400-e29b-41d4-a716-446655440000",
  "status": "REQUESTED",
  "amount": 1000.00,
  "refundMethod": "ACH",
  "requestJournalEntryId": "cc0e8400-e29b-41d4-a716-446655440000",
  "requestDate": "2024-04-26",
  "message": "Refund request submitted successfully"
}
```

### 2. Approve a Refund (Admin only)

**Endpoint**: `POST /api/v1/refunds/{refundId}/approve`

```bash
curl -X POST http://localhost:8080/api/v1/refunds/bb0e8400-e29b-41d4-a716-446655440000/approve \
  -H "Content-Type: application/json" \
  -d '{
    "refundId": "bb0e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "approvedBy": "FinanceManager",
    "approvalNotes": "Verified overpayment"
  }'
```

### 3. Issue a Refund (Admin only)

**Endpoint**: `POST /api/v1/refunds/{refundId}/issue`

```bash
curl -X POST http://localhost:8080/api/v1/refunds/bb0e8400-e29b-41d4-a716-446655440000/issue \
  -H "Content-Type: application/json" \
  -d '{
    "refundId": "bb0e8400-e29b-41d4-a716-446655440000",
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "issuanceDate": "2024-05-01",
    "issuedBy": "PaymentProcessor"
  }'
```

---

## Account Statements

### 1. Get Account Statement

**Endpoint**: `GET /api/v1/account-statements/{filerId}`

```bash
curl "http://localhost:8080/api/v1/account-statements/550e8400-e29b-41d4-a716-446655440000?startDate=2024-01-01&endDate=2024-12-31"
```

**Response**:
```json
{
  "filerId": "550e8400-e29b-41d4-a716-446655440000",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "currentBalance": 0.00,
  "transactions": [
    {
      "id": "tx-001",
      "date": "2024-04-20",
      "description": "Q1 2024 Tax Assessment",
      "debit": 10000.00,
      "credit": 0.00,
      "balance": 10000.00,
      "type": "ASSESSMENT"
    },
    {
      "id": "tx-002",
      "date": "2024-04-25",
      "description": "Payment - Credit Card",
      "debit": 0.00,
      "credit": 10000.00,
      "balance": 0.00,
      "type": "PAYMENT"
    }
  ]
}
```

### 2. Export Statement as PDF

**Endpoint**: `GET /api/v1/account-statements/{filerId}/pdf`

```bash
curl -o statement.pdf \
  "http://localhost:8080/api/v1/account-statements/550e8400-e29b-41d4-a716-446655440000/pdf?startDate=2024-01-01&endDate=2024-12-31"
```

### 3. Export Statement as CSV

**Endpoint**: `GET /api/v1/account-statements/{filerId}/csv`

```bash
curl -o statement.csv \
  "http://localhost:8080/api/v1/account-statements/550e8400-e29b-41d4-a716-446655440000/csv?startDate=2024-01-01&endDate=2024-12-31"
```

---

## Reconciliation

### 1. Run Municipality-wide Reconciliation

**Endpoint**: `POST /api/v1/reconciliation`

```bash
curl -X POST http://localhost:8080/api/v1/reconciliation \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "660e8400-e29b-41d4-a716-446655440000",
    "asOfDate": "2024-04-30"
  }'
```

**Response**:
```json
{
  "tenantId": "660e8400-e29b-41d4-a716-446655440000",
  "asOfDate": "2024-04-30",
  "isReconciled": true,
  "municipalityAR": 2500000.00,
  "totalFilerLiabilities": 2500000.00,
  "variance": 0.00,
  "totalFilers": 100,
  "discrepancies": [],
  "message": "Books are reconciled"
}
```

### 2. Reconcile Individual Filer

**Endpoint**: `GET /api/v1/reconciliation/{filerId}`

```bash
curl "http://localhost:8080/api/v1/reconciliation/550e8400-e29b-41d4-a716-446655440000?asOfDate=2024-04-30"
```

---

## Trial Balance

### 1. Generate Trial Balance

**Endpoint**: `GET /api/v1/trial-balance`

```bash
curl "http://localhost:8080/api/v1/trial-balance?tenantId=660e8400-e29b-41d4-a716-446655440000&asOfDate=2024-04-30"
```

**Response**:
```json
{
  "tenantId": "660e8400-e29b-41d4-a716-446655440000",
  "asOfDate": "2024-04-30",
  "isBalanced": true,
  "totalDebits": 3500000.00,
  "totalCredits": 3500000.00,
  "accountBalances": [
    {
      "accountNumber": "1000",
      "accountName": "Cash",
      "accountType": "ASSET",
      "debitBalance": 1000000.00,
      "creditBalance": 0.00,
      "normalBalance": "DEBIT"
    },
    {
      "accountNumber": "1200",
      "accountName": "Accounts Receivable",
      "accountType": "ASSET",
      "debitBalance": 2500000.00,
      "creditBalance": 0.00,
      "normalBalance": "DEBIT"
    },
    {
      "accountNumber": "4100",
      "accountName": "Tax Revenue",
      "accountType": "REVENUE",
      "debitBalance": 0.00,
      "creditBalance": 3000000.00,
      "normalBalance": "CREDIT"
    },
    {
      "accountNumber": "2300",
      "accountName": "Refunds Payable",
      "accountType": "LIABILITY",
      "debitBalance": 0.00,
      "creditBalance": 500000.00,
      "normalBalance": "CREDIT"
    }
  ]
}
```

---

## Audit Trail

### 1. Get Audit Log for Journal Entry

**Endpoint**: `GET /api/v1/audit/journal-entries/{entryId}`

```bash
curl "http://localhost:8080/api/v1/audit/journal-entries/880e8400-e29b-41d4-a716-446655440000"
```

**Response**:
```json
{
  "auditLogs": [
    {
      "id": "dd0e8400-e29b-41d4-a716-446655440000",
      "entityType": "JOURNAL_ENTRY",
      "entityId": "880e8400-e29b-41d4-a716-446655440000",
      "action": "CREATE",
      "userId": "System",
      "timestamp": "2024-04-25T10:32:15Z",
      "details": "Journal entry created for payment"
    },
    {
      "id": "ee0e8400-e29b-41d4-a716-446655440000",
      "entityType": "JOURNAL_ENTRY",
      "entityId": "880e8400-e29b-41d4-a716-446655440000",
      "action": "POST",
      "userId": "FinanceOfficer",
      "timestamp": "2024-04-25T14:15:30Z",
      "details": "Journal entry posted to ledger"
    }
  ]
}
```

### 2. Get Audit Log with Filters

**Endpoint**: `GET /api/v1/audit`

```bash
curl "http://localhost:8080/api/v1/audit?tenantId=660e8400-e29b-41d4-a716-446655440000&startDate=2024-04-01&endDate=2024-04-30&action=PAYMENT"
```

---

## Error Responses

All endpoints return standard error responses:

```json
{
  "timestamp": "2024-04-25T10:32:15Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid payment request: Amount must be positive",
  "path": "/api/v1/payments/process"
}
```

## Rate Limiting

API requests are rate-limited to:
- **100 requests per minute** for read operations
- **20 requests per minute** for write operations

## Support

For API support, contact: support@munitax.com
