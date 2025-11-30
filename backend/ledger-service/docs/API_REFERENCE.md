# API Reference - Ledger Service

Complete API reference for all ledger service endpoints.

## Base Information

- **Base URL**: `http://localhost:8087/api/v1` (development)
- **Production URL**: `https://api.munitax.com/api/v1`
- **API Version**: 1.0.0
- **Interactive Docs**: `/swagger-ui.html`
- **OpenAPI Spec**: `/v3/api-docs`

## Authentication

All API requests require authentication (implementation specific to your auth setup).

```http
Authorization: Bearer YOUR_TOKEN_HERE
```

---

## Payment Endpoints

### Process Payment

Create and process a payment transaction.

```http
POST /api/v1/payments/process
Content-Type: application/json
```

**Request Body:**
```json
{
  "filerId": "uuid",
  "tenantId": "uuid",
  "amount": 5000.00,
  "paymentMethod": "CREDIT_CARD",
  "cardNumber": "4111-1111-1111-1111",
  "expirationDate": "12/25",
  "cvv": "123",
  "cardholderName": "John Doe",
  "description": "Q1 2024 Tax Payment",
  "taxReturnId": "TR-2024-Q1-12345"
}
```

**Payment Methods:**
- `CREDIT_CARD`: Requires cardNumber, expirationDate, cvv, cardholderName
- `ACH`: Requires achRouting, achAccount
- `CHECK`: Requires checkNumber
- `WIRE`: Requires wireConfirmation

**Response:**
```json
{
  "transactionId": "uuid",
  "providerTransactionId": "mock_ch_abc123",
  "status": "APPROVED",
  "amount": 5000.00,
  "journalEntryId": "uuid",
  "testMode": true,
  "message": "Payment approved",
  "timestamp": "2024-04-25T10:32:15Z"
}
```

**Status Codes:**
- `200 OK`: Payment processed successfully
- `400 Bad Request`: Invalid payment data
- `402 Payment Required`: Payment declined
- `500 Internal Server Error`: System error

### Get Filer Payments

Retrieve all payments for a specific filer.

```http
GET /api/v1/payments/filer/{filerId}
```

**Path Parameters:**
- `filerId` (UUID): Filer identifier

**Response:**
```json
[
  {
    "paymentId": "uuid",
    "filerId": "uuid",
    "tenantId": "uuid",
    "amount": 5000.00,
    "paymentMethod": "CREDIT_CARD",
    "paymentStatus": "APPROVED",
    "paymentDate": "2024-04-25",
    "providerTransactionId": "mock_ch_abc123",
    "journalEntryId": "uuid",
    "description": "Q1 2024 Tax Payment",
    "testMode": true
  }
]
```

### Get Payment Receipt

Generate receipt for a completed payment.

```http
GET /api/v1/payments/{paymentId}/receipt
```

**Response:**
```json
{
  "paymentId": "uuid",
  "transactionId": "mock_ch_abc123",
  "filerId": "uuid",
  "amount": 5000.00,
  "paymentDate": "2024-04-25",
  "paymentMethod": "CREDIT_CARD",
  "last4": "1111",
  "description": "Q1 2024 Tax Payment",
  "journalEntryNumber": "JE-2024-00125"
}
```

---

## Tax Assessment Endpoints

### Record Tax Assessment

Record a tax assessment with automatic journal entry creation.

```http
POST /api/v1/tax-assessments
Content-Type: application/json
```

**Request Body:**
```json
{
  "filerId": "uuid",
  "tenantId": "uuid",
  "taxReturnId": "TR-2024-Q1-12345",
  "taxAmount": 10000.00,
  "penaltyAmount": 500.00,
  "interestAmount": 150.00,
  "assessmentDate": "2024-04-20",
  "taxYear": "2024",
  "taxPeriod": "Q1",
  "description": "Q1 2024 Tax Assessment"
}
```

**Response:**
```json
{
  "filerJournalEntryId": "uuid",
  "municipalityJournalEntryId": "uuid",
  "entryNumber": "JE-2024-00100",
  "filerId": "uuid",
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

## Refund Endpoints

### Request Refund

Submit a refund request for overpayment.

```http
POST /api/v1/refunds/request
Content-Type: application/json
```

**Request Body:**
```json
{
  "filerId": "uuid",
  "tenantId": "uuid",
  "amount": 1000.00,
  "refundMethod": "ACH",
  "accountNumber": "123456789",
  "routingNumber": "021000021",
  "reason": "Overpayment refund for Q1 2024",
  "taxReturnId": "TR-2024-Q1-12345"
}
```

**Refund Methods:**
- `ACH`: Direct deposit (3-5 business days)
- `CHECK`: Mailed check (7-10 business days)
- `WIRE`: Wire transfer (1-2 business days)

**Response:**
```json
{
  "refundId": "uuid",
  "status": "REQUESTED",
  "amount": 1000.00,
  "refundMethod": "ACH",
  "requestJournalEntryId": "uuid",
  "requestDate": "2024-04-26",
  "message": "Refund request submitted successfully"
}
```

### Approve Refund (Admin)

Approve a pending refund request.

```http
POST /api/v1/refunds/{refundId}/approve
Content-Type: application/json
```

**Request Body:**
```json
{
  "refundId": "uuid",
  "tenantId": "uuid",
  "approvedBy": "FinanceManager",
  "approvalNotes": "Verified overpayment"
}
```

### Issue Refund (Admin)

Issue an approved refund.

```http
POST /api/v1/refunds/{refundId}/issue
Content-Type: application/json
```

**Request Body:**
```json
{
  "refundId": "uuid",
  "tenantId": "uuid",
  "issuanceDate": "2024-05-01",
  "issuedBy": "PaymentProcessor"
}
```

---

## Account Statement Endpoints

### Get Account Statement

Retrieve account statement with transaction history.

```http
GET /api/v1/account-statements/{filerId}?startDate=2024-01-01&endDate=2024-12-31
```

**Query Parameters:**
- `startDate` (date): Start date (YYYY-MM-DD)
- `endDate` (date): End date (YYYY-MM-DD)
- `transactionType` (optional): Filter by type (ASSESSMENT, PAYMENT, REFUND)

**Response:**
```json
{
  "filerId": "uuid",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "currentBalance": 0.00,
  "transactions": [
    {
      "id": "uuid",
      "date": "2024-04-20",
      "description": "Q1 2024 Tax Assessment",
      "debit": 10000.00,
      "credit": 0.00,
      "balance": 10000.00,
      "type": "ASSESSMENT",
      "referenceNumber": "JE-2024-00100"
    },
    {
      "id": "uuid",
      "date": "2024-04-25",
      "description": "Payment - Credit Card",
      "debit": 0.00,
      "credit": 10000.00,
      "balance": 0.00,
      "type": "PAYMENT",
      "referenceNumber": "JE-2024-00125"
    }
  ]
}
```

### Export Statement (PDF)

```http
GET /api/v1/account-statements/{filerId}/pdf?startDate=2024-01-01&endDate=2024-12-31
Content-Type: application/pdf
```

### Export Statement (CSV)

```http
GET /api/v1/account-statements/{filerId}/csv?startDate=2024-01-01&endDate=2024-12-31
Content-Type: text/csv
```

---

## Reconciliation Endpoints

### Run Reconciliation

Reconcile municipality AR with filer liabilities.

```http
POST /api/v1/reconciliation
Content-Type: application/json
```

**Request Body:**
```json
{
  "tenantId": "uuid",
  "asOfDate": "2024-04-30"
}
```

**Response:**
```json
{
  "tenantId": "uuid",
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

### Reconcile Individual Filer

```http
GET /api/v1/reconciliation/{filerId}?asOfDate=2024-04-30
```

**Response:**
```json
{
  "filerId": "uuid",
  "asOfDate": "2024-04-30",
  "isReconciled": true,
  "filerLiability": 5000.00,
  "municipalityAR": 5000.00,
  "variance": 0.00,
  "message": "Filer account is reconciled"
}
```

---

## Trial Balance Endpoints

### Generate Trial Balance

Generate trial balance report for municipality.

```http
GET /api/v1/trial-balance?tenantId={uuid}&asOfDate=2024-04-30
```

**Query Parameters:**
- `tenantId` (UUID): Municipality identifier
- `asOfDate` (date): Report date (YYYY-MM-DD)
- `accountType` (optional): Filter by type (ASSET, LIABILITY, REVENUE, EXPENSE)

**Response:**
```json
{
  "tenantId": "uuid",
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
    }
  ]
}
```

---

## Journal Entry Endpoints

### Create Journal Entry

Manually create a journal entry.

```http
POST /api/v1/journal-entries
Content-Type: application/json
```

**Request Body:**
```json
{
  "tenantId": "uuid",
  "entityId": "uuid",
  "entryDate": "2024-04-25",
  "description": "Manual adjustment",
  "sourceType": "MANUAL",
  "sourceId": "ADJ-001",
  "createdBy": "admin@munitax.com",
  "lines": [
    {
      "accountNumber": "1000",
      "debit": 500.00,
      "credit": 0.00,
      "description": "Debit cash"
    },
    {
      "accountNumber": "4100",
      "debit": 0.00,
      "credit": 500.00,
      "description": "Credit revenue"
    }
  ]
}
```

**Important**: Journal entries must balance (total debits = total credits)

### Get Journal Entry

```http
GET /api/v1/journal-entries/{entryId}
```

---

## Audit Trail Endpoints

### Get Audit Log

Retrieve audit trail for entity or date range.

```http
GET /api/v1/audit?tenantId={uuid}&startDate=2024-04-01&endDate=2024-04-30&action=PAYMENT
```

**Query Parameters:**
- `tenantId` (UUID): Municipality identifier
- `startDate` (date): Start date
- `endDate` (date): End date
- `entityType` (optional): JOURNAL_ENTRY, PAYMENT, REFUND, etc.
- `action` (optional): CREATE, UPDATE, DELETE, APPROVE, etc.
- `userId` (optional): Filter by user

**Response:**
```json
{
  "auditLogs": [
    {
      "id": "uuid",
      "entityType": "PAYMENT",
      "entityId": "uuid",
      "action": "CREATE",
      "userId": "user@munitax.com",
      "timestamp": "2024-04-25T10:32:15Z",
      "details": "Payment created for $5000.00"
    }
  ]
}
```

### Get Audit Log for Journal Entry

```http
GET /api/v1/audit/journal-entries/{entryId}
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
  "path": "/api/v1/payments/process",
  "details": [
    {
      "field": "amount",
      "message": "Amount must be greater than zero"
    }
  ]
}
```

**Common Status Codes:**
- `200 OK`: Request successful
- `201 Created`: Resource created successfully
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource conflict (e.g., duplicate entry)
- `422 Unprocessable Entity`: Validation failed
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: System error
- `503 Service Unavailable`: Service temporarily unavailable

---

## Rate Limits

- **Read operations**: 100 requests per minute per IP
- **Write operations**: 20 requests per minute per IP

When rate limit is exceeded:
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many write requests. Please try again later.",
  "limit": 20,
  "window": "1 minute"
}
```

---

## Pagination

Endpoints that return lists support pagination:

```http
GET /api/v1/payments/filer/{filerId}?page=1&size=20
```

**Query Parameters:**
- `page` (int): Page number (0-indexed, default: 0)
- `size` (int): Items per page (default: 20, max: 100)

**Response Headers:**
- `X-Total-Count`: Total number of items
- `X-Total-Pages`: Total number of pages

---

## Webhooks (Future)

Support for webhooks is planned for future releases:
- Payment approved
- Refund issued
- Reconciliation completed

---

## SDKs and Client Libraries

### JavaScript/TypeScript

```javascript
import { LedgerClient } from '@munitax/ledger-sdk';

const client = new LedgerClient({
  baseUrl: 'https://api.munitax.com/api/v1',
  apiKey: 'your-api-key'
});

const payment = await client.payments.process({
  filerId: 'uuid',
  tenantId: 'uuid',
  amount: 5000.00,
  paymentMethod: 'CREDIT_CARD',
  cardNumber: '4111-1111-1111-1111'
});
```

### Python

```python
from munitax_ledger import LedgerClient

client = LedgerClient(
    base_url='https://api.munitax.com/api/v1',
    api_key='your-api-key'
)

payment = client.payments.process(
    filer_id='uuid',
    tenant_id='uuid',
    amount=5000.00,
    payment_method='CREDIT_CARD',
    card_number='4111-1111-1111-1111'
)
```

---

## Support

- **API Documentation**: `/swagger-ui.html`
- **Technical Support**: support@munitax.com
- **Bug Reports**: https://github.com/munitax/ledger-service/issues
- **Status Page**: https://status.munitax.com

---

*Last Updated: November 2024*  
*API Version: 1.0.0*
