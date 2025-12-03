# Tasks: Mock Payment Gateway Integration

**Input**: Design documents from `/specs/copilot/add-mock-payment-gateway/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Tests**: Unit tests requested for backend service/controller and frontend component changes.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `backend/ledger-service/src/main/java/com/munitax/ledger/`
- **Backend Tests**: `backend/ledger-service/src/test/java/com/munitax/ledger/`
- **Frontend**: `components/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create new DTO files for test payment methods API

- [X] T001 [P] Create TestCreditCard DTO in `backend/ledger-service/src/main/java/com/munitax/ledger/dto/TestCreditCard.java`
- [X] T002 [P] Create TestACHAccount DTO in `backend/ledger-service/src/main/java/com/munitax/ledger/dto/TestACHAccount.java`
- [X] T003 Create TestPaymentMethodsResponse DTO in `backend/ledger-service/src/main/java/com/munitax/ledger/dto/TestPaymentMethodsResponse.java` (depends on T001, T002)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Backend service method that provides test payment data - required by ALL user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Add getTestPaymentMethods() method to `backend/ledger-service/src/main/java/com/munitax/ledger/service/MockPaymentProviderService.java`
- [X] T005 Inject MockPaymentProviderService into PaymentController in `backend/ledger-service/src/main/java/com/munitax/ledger/controller/PaymentController.java`

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Fetch Test Payment Methods from Backend API (Priority: P1) 🎯 MVP

**Goal**: Replace hardcoded frontend test payment data with dynamic API fetch so test methods are centrally managed

**Independent Test**: Load payment gateway in test mode, verify test payment methods are fetched via API call (Network tab shows GET /api/v1/payments/test-methods)

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T006 [P] [US1] Add unit test for getTestPaymentMethods() in TEST mode in `backend/ledger-service/src/test/java/com/munitax/ledger/service/MockPaymentProviderServiceTest.java`
- [ ] T007 [P] [US1] Add unit test for getTestPaymentMethods() in PRODUCTION mode (empty response) in `backend/ledger-service/src/test/java/com/munitax/ledger/service/MockPaymentProviderServiceTest.java`
- [ ] T008 [P] [US1] Add controller test for GET /api/v1/payments/test-methods endpoint in `backend/ledger-service/src/test/java/com/munitax/ledger/controller/PaymentControllerTest.java`

### Implementation for User Story 1

- [X] T009 [US1] Add GET /test-methods endpoint to `backend/ledger-service/src/main/java/com/munitax/ledger/controller/PaymentController.java`
- [X] T010 [P] [US1] Create TypeScript interfaces for TestPaymentMethods, TestCreditCard, TestACHAccount in `components/PaymentGateway.tsx`
- [X] T011 [US1] Add useEffect hook to fetch test methods from API on component mount in `components/PaymentGateway.tsx`
- [X] T012 [US1] Add loading and error state handling for test methods fetch in `components/PaymentGateway.tsx`
- [X] T013 [US1] Replace static TEST_CARDS array with API-fetched creditCards in `components/PaymentGateway.tsx`
- [X] T014 [US1] Replace static TEST_ACH_ACCOUNTS array with API-fetched achAccounts in `components/PaymentGateway.tsx`
- [X] T015 [US1] Add graceful error message when API fetch fails (allow manual entry) in `components/PaymentGateway.tsx`
- [X] T016 [US1] Conditionally hide test helper panels when testMode is false (PRODUCTION) in `components/PaymentGateway.tsx`

**Checkpoint**: User Story 1 complete - test payment methods are fetched from backend API

---

## Phase 4: User Story 2 - Payment to Ledger Integration (Priority: P1)

**Goal**: Verify payment-ledger integration with double-entry journal entries (already implemented)

**Independent Test**: Make payment with test card 4111-1111-1111-1111, verify journal entries are created on both filer and municipality ledgers

### Tests for User Story 2 ⚠️

- [ ] T017 [P] [US2] Add integration test verifying approved payment creates journal entry in `backend/ledger-service/src/test/java/com/munitax/ledger/controller/PaymentControllerTest.java`
- [ ] T018 [P] [US2] Add integration test verifying declined payment does NOT create journal entry in `backend/ledger-service/src/test/java/com/munitax/ledger/controller/PaymentControllerTest.java`

### Implementation for User Story 2

> **NOTE**: Payment-ledger integration is already implemented. This phase verifies and documents existing behavior.

- [ ] T019 [US2] Verify payment response includes journalEntryId for approved payments (review existing code)
- [ ] T020 [US2] Add Swagger documentation for journalEntryId field in PaymentResponse in `backend/ledger-service/src/main/java/com/munitax/ledger/dto/PaymentResponse.java`

**Checkpoint**: User Story 2 verified - payment-ledger integration confirmed working

---

## Phase 5: User Story 3 - Payment Reconciliation with Drill-Down (Priority: P2)

**Goal**: Enhance reconciliation report with payment-specific drill-down capability

**Independent Test**: Run reconciliation report after multiple payments, verify totals match and discrepancies can be investigated

### Tests for User Story 3 ⚠️

- [ ] T021 [P] [US3] Add test for payment discrepancy detection in `backend/ledger-service/src/test/java/com/munitax/ledger/controller/ReconciliationControllerTest.java`

### Implementation for User Story 3

> **NOTE**: Reconciliation exists. This phase enhances with payment-specific features.

- [ ] T022 [US3] Add payment discrepancy fields to ReconciliationResponse if not present in `backend/ledger-service/src/main/java/com/munitax/ledger/dto/ReconciliationResponse.java`
- [ ] T023 [US3] Enhance ReconciliationReport.tsx with payment totals section (Municipality Cash vs Filer Payments) in `components/ReconciliationReport.tsx`
- [ ] T024 [US3] Add drill-down capability to payment discrepancy rows in `components/ReconciliationReport.tsx`
- [ ] T025 [US3] Add "View Transactions" navigation link from discrepancy to filer payment history in `components/ReconciliationReport.tsx`

**Checkpoint**: User Story 3 complete - payment reconciliation with drill-down working

---

## Phase 6: User Story 4 - Payment Audit Trail with Filtering (Priority: P2)

**Goal**: Enhance audit trail with payment-specific filters (method, status)

**Independent Test**: Make payments with different methods/statuses, filter audit trail by various criteria

### Tests for User Story 4 ⚠️

- [ ] T026 [P] [US4] Add test for audit trail payment filtering in `backend/ledger-service/src/test/java/com/munitax/ledger/controller/AuditControllerTest.java`

### Implementation for User Story 4

> **NOTE**: Audit trail exists. This phase adds payment-specific filters.

- [ ] T027 [US4] Add payment method filter dropdown to AuditTrail component in `components/AuditTrail.tsx`
- [ ] T028 [US4] Add payment status filter dropdown to AuditTrail component in `components/AuditTrail.tsx`
- [ ] T029 [US4] Implement client-side filtering by payment method in `components/AuditTrail.tsx`
- [ ] T030 [US4] Implement client-side filtering by payment status in `components/AuditTrail.tsx`
- [ ] T031 [US4] Add link from payment audit entry to related journal entry audit trail in `components/AuditTrail.tsx`

**Checkpoint**: User Story 4 complete - payment audit trail with comprehensive filtering

---

## Phase 7: User Story 5 - Test Payment Method Auto-Fill (Priority: P3)

**Goal**: Click test card/ACH to auto-fill payment form fields for faster testing

**Independent Test**: Click test card in helper panel, verify card number field is populated

### Implementation for User Story 5

- [X] T032 [P] [US5] Add handleSelectTestCard callback to auto-fill card number in `components/PaymentGateway.tsx`
- [X] T033 [P] [US5] Add handleSelectTestACH callback to auto-fill routing and account numbers in `components/PaymentGateway.tsx`
- [X] T034 [US5] Make test card rows clickable with cursor pointer styling in `components/PaymentGateway.tsx`
- [X] T035 [US5] Make test ACH rows clickable with cursor pointer styling in `components/PaymentGateway.tsx`
- [X] T036 [US5] Auto-focus expiry field after credit card auto-fill in `components/PaymentGateway.tsx`
- [X] T037 [US5] Auto-focus submit button after ACH auto-fill in `components/PaymentGateway.tsx`

**Checkpoint**: User Story 5 complete - test payment auto-fill working

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, cleanup, and final validation

- [ ] T038 [P] Update API documentation in `specs/copilot/add-mock-payment-gateway/contracts/test-methods-api.yaml` (verify matches implementation)
- [ ] T039 [P] Add inline code comments for test methods endpoint purpose in `backend/ledger-service/src/main/java/com/munitax/ledger/controller/PaymentController.java`
- [ ] T040 Run backend tests: `cd backend/ledger-service && ./mvnw test`
- [ ] T041 Run frontend tests: `npm test -- PaymentGateway`
- [ ] T042 Manual validation: Run quickstart.md test scenarios from `specs/copilot/add-mock-payment-gateway/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup (Phase 1) completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational (Phase 2) completion
  - US1 (P1): Can start immediately after Phase 2
  - US2 (P1): Can start in parallel with US1 (different files)
  - US3 (P2): Can start after Phase 2, no dependency on US1/US2
  - US4 (P2): Can start after Phase 2, no dependency on other stories
  - US5 (P3): Depends on US1 (needs API fetch working for auto-fill data)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2 - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Phase 2 - Independent (verifies existing functionality)
- **User Story 3 (P2)**: Can start after Phase 2 - Independent
- **User Story 4 (P2)**: Can start after Phase 2 - Independent
- **User Story 5 (P3)**: Requires US1 complete (needs API-fetched test data)

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Backend changes before frontend changes
- Service layer before controller layer
- Core implementation before enhancements

### Parallel Opportunities

Within **Phase 1 (Setup)**:
- T001, T002 can run in parallel (independent DTO files)

Within **Phase 3 (US1)**:
- T006, T007, T008 (tests) can run in parallel
- T010 can run in parallel with backend work

Within **Phase 4-6**:
- Test tasks marked [P] can run in parallel

Across **User Stories**:
- US1 and US2 can run in parallel (different concerns)
- US3 and US4 can run in parallel (different components)

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task T006: "Unit test for getTestPaymentMethods() in TEST mode"
Task T007: "Unit test for getTestPaymentMethods() in PRODUCTION mode"
Task T008: "Controller test for GET /test-methods endpoint"

# After tests written, launch backend + frontend in parallel:
# Backend: T009 (controller endpoint)
# Frontend: T010 (TypeScript interfaces)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (DTOs)
2. Complete Phase 2: Foundational (service method)
3. Complete Phase 3: User Story 1 (API endpoint + frontend fetch)
4. **STOP and VALIDATE**: Test API returns data, frontend displays it
5. Deploy/demo if ready - static test data eliminated!

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → **MVP Delivered!**
3. Add User Story 2 → Verify ledger integration → Deploy
4. Add User Story 3 → Payment reconciliation enhanced → Deploy
5. Add User Story 4 → Audit filtering enhanced → Deploy
6. Add User Story 5 → Auto-fill convenience feature → Deploy
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Phase 2 is done:
   - Developer A: User Story 1 (API + frontend)
   - Developer B: User Story 2 (ledger verification)
3. After US1 complete:
   - Developer A: User Story 5 (auto-fill)
   - Developer B: User Story 3 (reconciliation)
   - Developer C: User Story 4 (audit)

---

## Files Changed Summary

| File | Phase | Change Type | Description |
|------|-------|-------------|-------------|
| `dto/TestCreditCard.java` | 1 | NEW | Credit card DTO |
| `dto/TestACHAccount.java` | 1 | NEW | ACH account DTO |
| `dto/TestPaymentMethodsResponse.java` | 1 | NEW | Response wrapper DTO |
| `service/MockPaymentProviderService.java` | 2 | MODIFIED | Add getTestPaymentMethods() |
| `controller/PaymentController.java` | 3 | MODIFIED | Add GET /test-methods endpoint |
| `components/PaymentGateway.tsx` | 3,5,7 | MODIFIED | Fetch from API, auto-fill, remove static data |
| `components/ReconciliationReport.tsx` | 5 | MODIFIED | Payment drill-down |
| `components/AuditTrail.tsx` | 6 | MODIFIED | Payment filters |
| `dto/PaymentResponse.java` | 4 | MODIFIED | Documentation |
| `dto/ReconciliationResponse.java` | 5 | MODIFIED | Payment fields |
| `service/MockPaymentProviderServiceTest.java` | 3 | NEW | Service unit tests |
| `controller/PaymentControllerTest.java` | 3,4 | MODIFIED | Endpoint tests |
| `controller/ReconciliationControllerTest.java` | 5 | MODIFIED | Reconciliation tests |
| `controller/AuditControllerTest.java` | 6 | MODIFIED | Audit tests |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- API endpoint does NOT require authentication (protected by TEST mode)
- Test methods API must respond within 500ms (NFR-001)
