# Research: Mock Payment Gateway Integration

**Phase**: 0 - Research & Discovery  
**Date**: 2025-12-03

## Research Summary

All NEEDS CLARIFICATION items have been resolved through spec clarifications and codebase analysis.

---

## 1. Test Payment Methods API Design

### Decision
Create `GET /api/v1/payments/test-methods` endpoint in `PaymentController` that returns available test credit cards and ACH accounts.

### Rationale
- **Follows existing pattern**: Payment endpoints are already in `PaymentController` at `/api/v1/payments/*`
- **Centralized management**: Backend is single source of truth for test payment methods
- **No authentication required**: Endpoint is protected by TEST mode (returns empty in PRODUCTION)
- **Cacheable**: Response is static, frontend can cache indefinitely

### Alternatives Considered
1. **Static JSON file served by frontend** - Rejected: Creates maintenance burden, potential inconsistency with backend
2. **Environment variables** - Rejected: Requires deployment changes to update test cards
3. **Database-stored test methods** - Rejected: Overkill for static test data

### Implementation Details
- Extract test card/ACH data from `MockPaymentProviderService` into structured DTOs
- Return empty array when `paymentMode != "TEST"` (PRODUCTION protection)
- Exclude CHECK/WIRE payment types (manual types with no test scenarios)

---

## 2. Frontend API Integration Pattern

### Decision
Fetch test methods on component mount using `useEffect` with loading/error states.

### Rationale
- **Consistent with existing patterns**: `ReconciliationReport.tsx` uses same pattern
- **Graceful degradation**: On API failure, display error message and allow manual entry
- **No blocking**: Payment form remains functional even if test helpers fail to load

### Alternatives Considered
1. **React Query/SWR** - Rejected: Not currently in codebase, adds dependency
2. **Context provider at app level** - Rejected: Overkill for single-component use
3. **Fetch on helper panel expand** - Rejected: Adds latency at user interaction time

### Implementation Details
```typescript
// PaymentGateway.tsx
const [testMethods, setTestMethods] = useState<TestPaymentMethods | null>(null);
const [testMethodsError, setTestMethodsError] = useState<string | null>(null);

useEffect(() => {
  fetch('/api/v1/payments/test-methods')
    .then(res => res.json())
    .then(data => setTestMethods(data))
    .catch(() => setTestMethodsError('Failed to load test methods'));
}, []);
```

---

## 3. Reconciliation Enhancement Pattern

### Decision
Add payment-specific section to existing `ReconciliationReport.tsx` with drill-down capability.

### Rationale
- **Existing component**: `ReconciliationReport.tsx` already handles AR/Cash reconciliation
- **Drill-down pattern**: Component already has `toggleDiscrepancy` and expanded row pattern
- **Navigation**: Add "View Transactions" button linking to filer's payment history

### Alternatives Considered
1. **New PaymentReconciliation component** - Rejected: Duplicates reconciliation logic
2. **Separate payment report** - Rejected: Finance officers want unified view
3. **Modal drill-down** - Rejected: Existing pattern uses inline expansion

### Implementation Details
- Enhance `ReconciliationData` interface with `paymentDiscrepancies` array
- Add payment-specific columns: payment method, transaction ID, authorization code
- Link "View Transactions" to `/filer/{filerId}/payments?from={date}&to={date}`

---

## 4. Audit Trail Enhancement Pattern

### Decision
Add payment-specific filters (payment method, payment status) to existing `AuditTrail.tsx`.

### Rationale
- **Existing filter pattern**: Component already has action/user/date filters
- **Payment-specific needs**: Auditors need to filter by DECLINED status, specific methods
- **Linkage**: Payment events should link to journal entry audit trail

### Alternatives Considered
1. **Separate PaymentAuditTrail component** - Rejected: Duplicates audit logic
2. **Tab-based separation** - Rejected: Auditors want unified view with filters
3. **Server-side filtering only** - Rejected: Existing pattern uses client-side filtering

### Implementation Details
```typescript
// Add to AuditTrail.tsx filter state
const [filterPaymentMethod, setFilterPaymentMethod] = useState<string>('');
const [filterPaymentStatus, setFilterPaymentStatus] = useState<string>('');
```

---

## 5. Auto-Fill Behavior

### Decision
Card number only auto-fill for credit cards; routing + account for ACH.

### Rationale
- **Security consideration**: CVV should always be manually entered (even in test mode)
- **Expiry flexibility**: Test mode doesn't validate expiry, user can enter any date
- **ACH complete**: Both routing and account needed for ACH processing

### Alternatives Considered
1. **Full auto-fill including CVV** - Rejected: Creates bad habits for production
2. **Copy to clipboard** - Rejected: Requires extra user step
3. **Form field focus after auto-fill** - Selected: Focus moves to next empty field

### Implementation Details
- Credit Card click: `setCardNumber(card.number)` only
- ACH click: `setAchRouting(account.routing)` + `setAchAccount(account.account)`
- After auto-fill, focus next empty field (expiry for card, submit for ACH)

---

## 6. Test Mode Protection

### Decision
Backend returns empty array when `ledger.payment.mode != "TEST"`.

### Rationale
- **Defense in depth**: Frontend shouldn't have TEST mode logic
- **Configuration-driven**: Same code runs in all environments
- **No exposure risk**: Production never sees test card data

### Alternatives Considered
1. **Frontend environment variable** - Rejected: Frontend build artifacts shared across envs
2. **Separate test endpoint path** - Rejected: Additional API surface
3. **Header-based toggle** - Rejected: Can be spoofed

### Implementation Details
```java
// MockPaymentProviderService.java
public TestPaymentMethodsResponse getTestPaymentMethods() {
    if (!"TEST".equals(paymentMode)) {
        return new TestPaymentMethodsResponse(List.of(), List.of());
    }
    // Return test cards and ACH accounts
}
```

---

## Dependencies Verified

| Dependency | Status | Notes |
|------------|--------|-------|
| `ledger-service` PaymentController | ✅ Exists | Will add new endpoint |
| `MockPaymentProviderService` | ✅ Exists | Contains test card logic |
| `PaymentGateway.tsx` | ✅ Exists | Has static TEST_CARDS array |
| `ReconciliationReport.tsx` | ✅ Exists | Has drill-down pattern |
| `AuditTrail.tsx` | ✅ Exists | Has filter pattern |

---

## Open Questions Resolved

All questions from spec clarifications session (2025-12-03) have been resolved:
- ✅ API endpoint path: `GET /api/v1/payments/test-methods`
- ✅ Response time threshold: 500ms
- ✅ Auto-fill fields: Card number only; ACH routing + account
- ✅ CHECK/WIRE excluded: Manual types with no test scenarios
- ✅ Authentication: Not required (TEST mode protected)
