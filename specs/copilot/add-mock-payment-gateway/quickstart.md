# Quickstart: Mock Payment Gateway Integration

**Phase**: 1 - Design & Contracts  
**Date**: 2025-12-03

## Overview

This guide provides a quick reference for implementing the Mock Payment Gateway Integration feature.

---

## Prerequisites

- Java 21 installed
- Node.js 18+ installed
- PostgreSQL 16+ running
- Backend `ledger-service` buildable
- Frontend Vite dev server runnable

---

## Implementation Steps

### 1. Backend: Create Test Methods DTOs

**Location**: `backend/ledger-service/src/main/java/com/munitax/ledger/dto/`

Create three new DTO files:

```java
// TestCreditCard.java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class TestCreditCard {
    private String cardNumber;
    private String cardType;      // VISA, MASTERCARD, AMEX
    private String expectedResult; // APPROVED, DECLINED, ERROR
    private String description;
}

// TestACHAccount.java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class TestACHAccount {
    private String routingNumber;
    private String accountNumber;
    private String expectedResult; // APPROVED, DECLINED
    private String description;
}

// TestPaymentMethodsResponse.java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class TestPaymentMethodsResponse {
    private List<TestCreditCard> creditCards;
    private List<TestACHAccount> achAccounts;
    private boolean testMode;
}
```

### 2. Backend: Add Service Method

**Location**: `backend/ledger-service/src/main/java/com/munitax/ledger/service/MockPaymentProviderService.java`

Add method to return test payment methods:

```java
public TestPaymentMethodsResponse getTestPaymentMethods() {
    if (!"TEST".equals(paymentMode)) {
        return new TestPaymentMethodsResponse(List.of(), List.of(), false);
    }
    
    List<TestCreditCard> cards = List.of(
        TestCreditCard.builder()
            .cardNumber("4111-1111-1111-1111")
            .cardType("VISA")
            .expectedResult("APPROVED")
            .description("Standard Visa test card - always approved")
            .build(),
        // ... more cards
    );
    
    List<TestACHAccount> accounts = List.of(
        TestACHAccount.builder()
            .routingNumber("110000000")
            .accountNumber("000123456789")
            .expectedResult("APPROVED")
            .description("Standard ACH test account - always approved")
            .build(),
        // ... more accounts
    );
    
    return new TestPaymentMethodsResponse(cards, accounts, true);
}
```

### 3. Backend: Add Controller Endpoint

**Location**: `backend/ledger-service/src/main/java/com/munitax/ledger/controller/PaymentController.java`

Add new endpoint:

```java
@GetMapping("/test-methods")
@Operation(summary = "Get test payment methods", 
           description = "Returns test cards and ACH accounts for TEST mode")
public ResponseEntity<TestPaymentMethodsResponse> getTestPaymentMethods() {
    return ResponseEntity.ok(paymentProvider.getTestPaymentMethods());
}
```

### 4. Frontend: Update PaymentGateway

**Location**: `components/PaymentGateway.tsx`

Replace static TEST_CARDS and TEST_ACH_ACCOUNTS with API fetch:

```typescript
interface TestPaymentMethods {
  creditCards: TestCreditCard[];
  achAccounts: TestACHAccount[];
  testMode: boolean;
}

// In component:
const [testMethods, setTestMethods] = useState<TestPaymentMethods | null>(null);
const [testMethodsError, setTestMethodsError] = useState<string | null>(null);

useEffect(() => {
  fetch('/api/v1/payments/test-methods')
    .then(res => res.json())
    .then(data => setTestMethods(data))
    .catch(() => setTestMethodsError('Failed to load test methods'));
}, []);
```

### 5. Frontend: Add Auto-Fill Handlers

**Location**: `components/PaymentGateway.tsx`

Add click handlers for test card/ACH selection:

```typescript
const handleSelectTestCard = (card: TestCreditCard) => {
  setCardNumber(card.cardNumber);
  // Focus on expiry field after auto-fill
};

const handleSelectTestACH = (account: TestACHAccount) => {
  setAchRouting(account.routingNumber);
  setAchAccount(account.accountNumber);
  // Focus on submit button after auto-fill
};
```

---

## Testing

### Backend Test

```bash
cd backend/ledger-service
./mvnw test -Dtest=PaymentControllerTest
```

### Frontend Test

```bash
npm test -- PaymentGateway.test.tsx
```

### Manual Verification

1. Start backend in TEST mode: `./mvnw spring-boot:run`
2. Start frontend: `npm run dev`
3. Open payment gateway
4. Verify test cards/ACH loaded from API (check Network tab)
5. Click test card → verify auto-fill
6. Submit payment → verify expected result

---

## API Reference

### GET /api/v1/payments/test-methods

Returns test payment methods.

**Response (TEST mode)**:
```json
{
  "creditCards": [
    {
      "cardNumber": "4111-1111-1111-1111",
      "cardType": "VISA",
      "expectedResult": "APPROVED",
      "description": "Standard Visa test card"
    }
  ],
  "achAccounts": [
    {
      "routingNumber": "110000000",
      "accountNumber": "000123456789",
      "expectedResult": "APPROVED",
      "description": "Standard ACH test account"
    }
  ],
  "testMode": true
}
```

**Response (PRODUCTION mode)**:
```json
{
  "creditCards": [],
  "achAccounts": [],
  "testMode": false
}
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Empty test methods response | Check `ledger.payment.mode=TEST` in application.yml |
| API returns 404 | Ensure endpoint is mapped correctly in PaymentController |
| Auto-fill not working | Check click handler is attached to test card elements |
| Test card declined unexpectedly | Verify card number matches exactly (dashes/spaces matter) |

---

## Files Changed Summary

| File | Change Type | Description |
|------|-------------|-------------|
| `dto/TestCreditCard.java` | NEW | Credit card DTO |
| `dto/TestACHAccount.java` | NEW | ACH account DTO |
| `dto/TestPaymentMethodsResponse.java` | NEW | Response wrapper DTO |
| `service/MockPaymentProviderService.java` | MODIFIED | Add getTestPaymentMethods() |
| `controller/PaymentController.java` | MODIFIED | Add GET /test-methods endpoint |
| `components/PaymentGateway.tsx` | MODIFIED | Fetch from API, add auto-fill |
