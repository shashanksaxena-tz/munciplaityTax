# Data Model: Mock Payment Gateway Integration

**Phase**: 1 - Design & Contracts  
**Date**: 2025-12-03

## Entity Overview

This feature introduces DTOs for the test payment methods API response. No new database entities are required as the test data is static configuration.

---

## New DTOs

### TestPaymentMethodsResponse

Response wrapper for the test methods endpoint.

| Field | Type | Description |
|-------|------|-------------|
| `creditCards` | `List<TestCreditCard>` | Available test credit cards |
| `achAccounts` | `List<TestACHAccount>` | Available test ACH accounts |
| `testMode` | `boolean` | Always true when populated |

**JSON Example:**
```json
{
  "creditCards": [...],
  "achAccounts": [...],
  "testMode": true
}
```

---

### TestCreditCard

Represents a test credit card with expected outcome.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| `cardNumber` | `String` | Test card number (formatted) | Required, pattern: `\d{4}-\d{4}-\d{4}-\d{4}` or `\d{15}` |
| `cardType` | `String` | Card network (Visa, Mastercard, Amex) | Required, enum: `VISA`, `MASTERCARD`, `AMEX` |
| `expectedResult` | `String` | Expected payment outcome | Required, enum: `APPROVED`, `DECLINED`, `ERROR` |
| `description` | `String` | Human-readable description | Required |

**JSON Example:**
```json
{
  "cardNumber": "4111-1111-1111-1111",
  "cardType": "VISA",
  "expectedResult": "APPROVED",
  "description": "Standard Visa test card - always approved"
}
```

---

### TestACHAccount

Represents a test ACH account with expected outcome.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| `routingNumber` | `String` | 9-digit ABA routing number | Required, pattern: `\d{9}` |
| `accountNumber` | `String` | Test account number | Required |
| `expectedResult` | `String` | Expected payment outcome | Required, enum: `APPROVED`, `DECLINED` |
| `description` | `String` | Human-readable description | Required |

**JSON Example:**
```json
{
  "routingNumber": "110000000",
  "accountNumber": "000123456789",
  "expectedResult": "APPROVED",
  "description": "Standard ACH test account - always approved"
}
```

---

## Existing Entities (No Changes Required)

### PaymentTransaction

Already exists and tracks payment details. Relevant fields for this feature:

| Field | Type | Description |
|-------|------|-------------|
| `transactionId` | `UUID` | Primary key |
| `paymentId` | `UUID` | External payment reference |
| `status` | `PaymentStatus` | APPROVED, DECLINED, ERROR |
| `paymentMethod` | `PaymentMethod` | CREDIT_CARD, ACH, CHECK, WIRE |
| `journalEntryId` | `UUID` | Link to double-entry journal entry |
| `isTestMode` | `Boolean` | True for test payments |

### AuditLog

Already exists for audit trail. No schema changes needed, but frontend will filter by:
- `action`: PAYMENT events
- `entityType`: PAYMENT
- Additional details: payment method, status, failure reason

### ReconciliationResponse

Already exists. Will be enhanced with payment-specific fields in a future iteration if needed. Current structure supports payment reconciliation through existing Cash Receipts section.

---

## Frontend Types

### TypeScript Interfaces

```typescript
// New types for test methods API
interface TestPaymentMethods {
  creditCards: TestCreditCard[];
  achAccounts: TestACHAccount[];
  testMode: boolean;
}

interface TestCreditCard {
  cardNumber: string;
  cardType: 'VISA' | 'MASTERCARD' | 'AMEX';
  expectedResult: 'APPROVED' | 'DECLINED' | 'ERROR';
  description: string;
}

interface TestACHAccount {
  routingNumber: string;
  accountNumber: string;
  expectedResult: 'APPROVED' | 'DECLINED';
  description: string;
}
```

---

## State Transitions

### Test Methods API Response

```
[Start] → check paymentMode
    │
    ├─ paymentMode == "TEST" → Return populated TestPaymentMethodsResponse
    │
    └─ paymentMode != "TEST" → Return empty TestPaymentMethodsResponse (empty arrays)
```

### Payment Processing (Existing - No Changes)

```
PENDING → APPROVED → (creates journal entries)
       → DECLINED → (no journal entries)
       → ERROR → (no journal entries)
```

---

## Validation Rules

### Test Methods Endpoint

1. **Mode Check**: If `ledger.payment.mode != "TEST"`, return empty arrays
2. **Response Cacheable**: Response can be cached by frontend (ETag/Cache-Control headers)
3. **No Authentication**: Endpoint is public in TEST mode

### Frontend Auto-Fill

1. **Credit Card**: Only card number field is auto-filled
2. **ACH**: Both routing and account number fields are auto-filled
3. **Validation Bypass**: Test mode does not validate expiry date format
