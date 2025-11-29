# Tasks: Double-Entry Ledger System with Mock Payment Provider

**Input**: Design documents from `/specs/12-double-entry-ledger/`
**Prerequisites**: spec.md (user stories with priorities)

**Tests**: Test tasks included per user story requirements

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `backend/ledger-service/src/main/java/com/munitax/ledger/`
- **Frontend**: `components/`
- **Tests**: `backend/ledger-service/src/test/java/com/munitax/ledger/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify existing project structure and configuration

**Status**: ✅ COMPLETE - All infrastructure exists

The following components are already implemented and working:
- ✅ Spring Boot ledger-service with package structure
- ✅ Database schema with Flyway migrations (V1__Create_ledger_tables.sql)
- ✅ Chart of accounts (filer and municipality accounts)
- ✅ All entity models (JournalEntry, JournalEntryLine, PaymentTransaction, ChartOfAccounts, AccountBalance, AuditLog)
- ✅ All repositories (JournalEntryRepository, PaymentTransactionRepository, ChartOfAccountsRepository, AuditLogRepository)
- ✅ Core enums (PaymentStatus, PaymentMethod, SourceType, EntryStatus, AccountType, NormalBalance)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core services that are COMPLETE and ready for enhancement

**⚠️ Status**: ✅ MOSTLY COMPLETE - Core ledger functionality exists

**Already Implemented**:
- ✅ JournalEntryService with double-entry validation, entry number generation (JE-YYYY-#####), reversing entries
- ✅ MockPaymentProviderService with test cards and ACH accounts
- ✅ PaymentService with journal entry creation and payment allocation
- ✅ TaxAssessmentService for recording tax assessments
- ✅ RefundService for refund requests and issuance
- ✅ ReconciliationService (basic implementation, needs enhancement)
- ✅ AccountStatementService for filer statements
- ✅ AuditLogService for audit trail

**⚠️ Needs Enhancement**:
- ReconciliationService currently uses simplified logic (TODO comments indicate production implementation needed)

**Checkpoint**: Foundation is ready - user story implementation tasks focus on testing, integration, and missing features

---

## Phase 3: User Story 1 - Simulate Payment with Mock Payment Provider (Priority: P1 - Critical) 🎯 MVP

**Goal**: Enable filers to test payments using mock credit cards and ACH without real charges, with proper ledger entries

**Independent Test**: 
1. Filer uses test card 4111-1111-1111-1111 to pay $5,000 tax
2. System displays "TEST MODE: No real charges" indicator
3. Payment approved instantly with mock transaction ID
4. Ledger entries created on both filer and municipality books
5. Filer sees receipt with transaction ID

**Status**: ✅ BACKEND COMPLETE - Frontend needs enhancement

### Tests for User Story 1

- [X] T001 [P] [US1] Enhance MockPaymentProviderServiceTest to verify all test card scenarios in backend/ledger-service/src/test/java/com/munitax/ledger/service/MockPaymentProviderServiceTest.java
- [X] T002 [P] [US1] Create PaymentServiceTest for end-to-end payment flow with journal entries in backend/ledger-service/src/test/java/com/munitax/ledger/service/PaymentServiceTest.java
- [X] T003 [P] [US1] Add integration test for payment API endpoints in backend/ledger-service/src/test/java/com/munitax/ledger/controller/PaymentControllerTest.java

### Implementation for User Story 1

- [X] T004 [US1] Add test mode indicator to PaymentGateway.tsx frontend component in components/PaymentGateway.tsx
- [X] T005 [US1] Enhance PaymentGateway.tsx to integrate with backend /api/v1/payments/process endpoint in components/PaymentGateway.tsx
- [X] T006 [US1] Add test card helper text to PaymentGateway.tsx showing available test cards (4111..., 4000...0002, etc.) in components/PaymentGateway.tsx
- [X] T007 [US1] Add receipt display to PaymentGateway.tsx showing transaction ID, amount, and journal entry number in components/PaymentGateway.tsx
- [X] T008 [P] [US1] Add environment configuration for payment mode toggle (TEST/PRODUCTION) in backend/ledger-service/src/main/resources/application.properties
- [X] T009 [US1] Verify MockPaymentProviderService handles all FR-002 and FR-003 test scenarios in backend/ledger-service/src/main/java/com/munitax/ledger/service/MockPaymentProviderService.java
- [X] T010 [US1] Add payment receipt generation endpoint GET /api/v1/payments/{paymentId}/receipt in backend/ledger-service/src/main/java/com/munitax/ledger/controller/PaymentController.java

**Checkpoint**: User Story 1 complete - filers can make test payments with full ledger integration

---

## Phase 4: User Story 2 - Record Double-Entry Journal Entries for Tax Assessment (Priority: P1 - Critical)

**Goal**: Automatically create balanced double-entry journal entries when tax is assessed, recording on both filer and municipality books

**Independent Test**:
1. Business files Q1 2024 return with $10,000 tax due
2. System creates filer journal entry: DEBIT Tax Liability $10K, CREDIT Tax Expense $10K
3. System creates municipality journal entry: DEBIT AR $10K, CREDIT Tax Revenue $10K
4. Both entries balance (debits = credits)
5. Entries linked to tax return ID

**Status**: ✅ COMPLETE - TaxAssessmentService fully implemented

### Tests for User Story 2

- [ ] T011 [P] [US2] Create TaxAssessmentServiceTest to verify journal entry creation for tax assessments in backend/ledger-service/src/test/java/com/munitax/ledger/service/TaxAssessmentServiceTest.java
- [ ] T012 [P] [US2] Add test for compound tax assessment (tax + penalty + interest) in backend/ledger-service/src/test/java/com/munitax/ledger/service/TaxAssessmentServiceTest.java
- [ ] T013 [P] [US2] Add integration test for tax assessment API endpoint in backend/ledger-service/src/test/java/com/munitax/ledger/controller/TaxAssessmentControllerTest.java

### Implementation for User Story 2

- [ ] T014 [US2] Verify TaxAssessmentService handles all scenarios from FR-016 (tax, penalty, interest) in backend/ledger-service/src/main/java/com/munitax/ledger/service/TaxAssessmentService.java
- [ ] T015 [US2] Add API endpoint documentation for POST /api/v1/tax-assessments in backend/ledger-service/src/main/java/com/munitax/ledger/controller/TaxAssessmentController.java
- [ ] T016 [P] [US2] Create TaxAssessmentRequest DTO if missing in backend/ledger-service/src/main/java/com/munitax/ledger/dto/TaxAssessmentRequest.java
- [ ] T017 [P] [US2] Create TaxAssessmentResponse DTO if missing in backend/ledger-service/src/main/java/com/munitax/ledger/dto/TaxAssessmentResponse.java

**Checkpoint**: User Story 2 complete - tax assessments automatically create proper double-entry ledger entries

---

## Phase 5: User Story 3 - Two-Way Ledger Reconciliation Report (Priority: P2 - High Value)

**Goal**: Generate reconciliation report comparing municipality AR to all filers' liabilities, showing zero variance when books match

**Independent Test**:
1. 100 filers with total tax liabilities = $2,500,000
2. Municipality AR = $2,500,000
3. Reconciliation report shows: Difference = $0, Status = RECONCILED
4. If discrepancy exists, report lists specific transactions

**Status**: ⚠️ PARTIALLY COMPLETE - Basic implementation exists, needs production-ready enhancement

### Tests for User Story 3

- [ ] T018 [P] [US3] Create ReconciliationServiceTest with multi-filer aggregation test in backend/ledger-service/src/test/java/com/munitax/ledger/service/ReconciliationServiceTest.java
- [ ] T019 [P] [US3] Add test for discrepancy detection (filer payment not recorded by municipality) in backend/ledger-service/src/test/java/com/munitax/ledger/service/ReconciliationServiceTest.java
- [ ] T020 [P] [US3] Add integration test for reconciliation API endpoint in backend/ledger-service/src/test/java/com/munitax/ledger/controller/ReconciliationControllerTest.java

### Implementation for User Story 3

- [ ] T021 [US3] Implement production reconciliation logic in ReconciliationService to aggregate all filers per TODO comments in backend/ledger-service/src/main/java/com/munitax/ledger/service/ReconciliationService.java
- [ ] T022 [US3] Add method to query all filer entities for tenant in ReconciliationService in backend/ledger-service/src/main/java/com/munitax/ledger/service/ReconciliationService.java
- [ ] T023 [US3] Add method to sum filer tax liability accounts (2100, 2110, 2120, 2130) across all filers in backend/ledger-service/src/main/java/com/munitax/ledger/service/ReconciliationService.java
- [ ] T024 [US3] Add method to sum filer payment entries across all filers in ReconciliationService in backend/ledger-service/src/main/java/com/munitax/ledger/service/ReconciliationService.java
- [ ] T025 [US3] Add drill-down reconciliation endpoint GET /api/v1/reconciliation/{filerId} for individual filer in backend/ledger-service/src/main/java/com/munitax/ledger/controller/ReconciliationController.java
- [ ] T026 [US3] Enhance DiscrepancyDetail DTO to include filer name and ID in backend/ledger-service/src/main/java/com/munitax/ledger/dto/DiscrepancyDetail.java
- [ ] T027 [P] [US3] Create frontend ReconciliationReport.tsx component to display reconciliation results in components/ReconciliationReport.tsx
- [ ] T028 [US3] Add discrepancy drill-down UI to ReconciliationReport.tsx in components/ReconciliationReport.tsx

**Checkpoint**: User Story 3 complete - municipality can verify books reconcile with all filers

---

## Phase 6: User Story 4 - Track Filer Payment History in Account Statement (Priority: P2 - High Value)

**Goal**: Provide filers with detailed account statement showing all transactions with running balance

**Independent Test**:
1. Filer with Q1 tax $10,000 (04/20), payment $10,000 (04/25), penalty $50 (05/15), payment $50 (05/20)
2. Account statement shows all 4 transactions chronologically
3. Running balance calculated correctly: $10K → $0 → $50 → $0
4. Current balance shows $0

**Status**: ✅ COMPLETE - AccountStatementService fully implemented

### Tests for User Story 4

- [ ] T029 [P] [US4] Create AccountStatementServiceTest with multi-transaction scenario in backend/ledger-service/src/test/java/com/munitax/ledger/service/AccountStatementServiceTest.java
- [ ] T030 [P] [US4] Add test for date range filtering in account statement in backend/ledger-service/src/test/java/com/munitax/ledger/service/AccountStatementServiceTest.java
- [ ] T031 [P] [US4] Add integration test for account statement API endpoint in backend/ledger-service/src/test/java/com/munitax/ledger/controller/AccountStatementControllerTest.java

### Implementation for User Story 4

- [ ] T032 [US4] Verify AccountStatementService handles all FR-023 requirements (all transaction types) in backend/ledger-service/src/main/java/com/munitax/ledger/service/AccountStatementService.java
- [ ] T033 [US4] Add transaction type filtering to AccountStatementService per FR-024 in backend/ledger-service/src/main/java/com/munitax/ledger/service/AccountStatementService.java
- [ ] T034 [US4] Add tax year filtering to AccountStatementService per FR-024 in backend/ledger-service/src/main/java/com/munitax/ledger/service/AccountStatementService.java
- [ ] T035 [US4] Add PDF export endpoint GET /api/v1/account-statements/{filerId}/pdf per FR-025 in backend/ledger-service/src/main/java/com/munitax/ledger/controller/AccountStatementController.java
- [ ] T036 [US4] Add CSV export endpoint GET /api/v1/account-statements/{filerId}/csv per FR-025 in backend/ledger-service/src/main/java/com/munitax/ledger/controller/AccountStatementController.java
- [ ] T037 [US4] Add aging calculation method to AccountStatementService per FR-026 in backend/ledger-service/src/main/java/com/munitax/ledger/service/AccountStatementService.java
- [ ] T038 [P] [US4] Create frontend FilerAccountStatement.tsx component to display account statement in components/FilerAccountStatement.tsx
- [ ] T039 [US4] Add date range filter UI to FilerAccountStatement.tsx in components/FilerAccountStatement.tsx
- [ ] T040 [US4] Add export buttons (PDF, CSV) to FilerAccountStatement.tsx in components/FilerAccountStatement.tsx

**Checkpoint**: User Story 4 complete - filers can view complete payment history with running balance

---

## Phase 7: User Story 5 - Generate Trial Balance for Municipality (Priority: P3 - Future)

**Goal**: Generate trial balance showing all accounts with debit/credit balances, verify debits = credits

**Independent Test**:
1. Municipality has accounts: Cash $1M (debit), AR $2.5M (debit), Tax Revenue $3M (credit), Refunds Payable $500K (credit)
2. Trial balance lists all accounts with balances
3. Total debits = $3.5M, Total credits = $3.5M
4. Status: BALANCED

**Status**: ❌ NOT IMPLEMENTED

### Tests for User Story 5

- [ ] T041 [P] [US5] Create TrialBalanceServiceTest with balanced ledger test in backend/ledger-service/src/test/java/com/munitax/ledger/service/TrialBalanceServiceTest.java
- [ ] T042 [P] [US5] Add test for unbalanced trial balance detection in backend/ledger-service/src/test/java/com/munitax/ledger/service/TrialBalanceServiceTest.java
- [ ] T043 [P] [US5] Add integration test for trial balance API endpoint in backend/ledger-service/src/test/java/com/munitax/ledger/controller/TrialBalanceControllerTest.java

### Implementation for User Story 5

- [ ] T044 [P] [US5] Create TrialBalanceService with generateTrialBalance method in backend/ledger-service/src/main/java/com/munitax/ledger/service/TrialBalanceService.java
- [ ] T045 [P] [US5] Create TrialBalanceResponse DTO in backend/ledger-service/src/main/java/com/munitax/ledger/dto/TrialBalanceResponse.java
- [ ] T046 [P] [US5] Create AccountBalanceSummary DTO for trial balance line items in backend/ledger-service/src/main/java/com/munitax/ledger/dto/AccountBalanceSummary.java
- [ ] T047 [US5] Implement account balance calculation from journal entries in TrialBalanceService in backend/ledger-service/src/main/java/com/munitax/ledger/service/TrialBalanceService.java
- [ ] T048 [US5] Add account hierarchy grouping (assets, liabilities, revenue, expense) to TrialBalanceService in backend/ledger-service/src/main/java/com/munitax/ledger/service/TrialBalanceService.java
- [ ] T049 [US5] Add date range filtering for trial balance (month-end, quarter-end, year-end) per FR-035 in backend/ledger-service/src/main/java/com/munitax/ledger/service/TrialBalanceService.java
- [ ] T050 [P] [US5] Create TrialBalanceController with GET /api/v1/trial-balance endpoint in backend/ledger-service/src/main/java/com/munitax/ledger/controller/TrialBalanceController.java
- [ ] T051 [P] [US5] Create frontend TrialBalance.tsx component to display trial balance in components/TrialBalance.tsx
- [ ] T052 [US5] Add account hierarchy tree view to TrialBalance.tsx in components/TrialBalance.tsx
- [ ] T053 [US5] Add balance validation indicator (balanced/unbalanced) to TrialBalance.tsx in components/TrialBalance.tsx

**Checkpoint**: User Story 5 complete - municipality can generate trial balance report

---

## Phase 8: User Story 6 - Process Refund with Ledger Entries (Priority: P2 - High Value)

**Goal**: Handle refund requests and issuance with proper double-entry journal entries on both books

**Independent Test**:
1. Filer overpaid by $1,000 (paid $11K, tax due $10K)
2. Filer requests refund of $1,000
3. System creates request journal entries: Filer DEBIT Refund Receivable, Municipality DEBIT Refund Expense
4. When issued, creates issuance entries: Filer DEBIT Cash, Municipality CREDIT Cash
5. Both books reconcile

**Status**: ✅ COMPLETE - RefundService fully implemented

### Tests for User Story 6

- [ ] T054 [P] [US6] Create RefundServiceTest with refund request and issuance test in backend/ledger-service/src/test/java/com/munitax/ledger/service/RefundServiceTest.java
- [ ] T055 [P] [US6] Add test for refund validation (amount ≤ overpayment) in backend/ledger-service/src/test/java/com/munitax/ledger/service/RefundServiceTest.java
- [ ] T056 [P] [US6] Add integration test for refund API endpoints in backend/ledger-service/src/test/java/com/munitax/ledger/controller/RefundControllerTest.java

### Implementation for User Story 6

- [ ] T057 [US6] Add overpayment detection method to RefundService per FR-036 in backend/ledger-service/src/main/java/com/munitax/ledger/service/RefundService.java
- [ ] T058 [US6] Verify RefundService handles all FR-038 through FR-042 requirements in backend/ledger-service/src/main/java/com/munitax/ledger/service/RefundService.java
- [ ] T059 [US6] Add refund approval workflow to RefundService per FR-039 in backend/ledger-service/src/main/java/com/munitax/ledger/service/RefundService.java
- [ ] T060 [US6] Add refund method selection (ACH, Check, Wire) to RefundService per FR-041 in backend/ledger-service/src/main/java/com/munitax/ledger/service/RefundService.java
- [ ] T061 [P] [US6] Create RefundRequest DTO if missing in backend/ledger-service/src/main/java/com/munitax/ledger/dto/RefundRequest.java
- [ ] T062 [P] [US6] Create RefundResponse DTO if missing in backend/ledger-service/src/main/java/com/munitax/ledger/dto/RefundResponse.java
- [ ] T063 [P] [US6] Create frontend RefundRequest.tsx component for filers to request refunds in components/RefundRequest.tsx
- [ ] T064 [US6] Add overpayment amount display to RefundRequest.tsx in components/RefundRequest.tsx
- [ ] T065 [US6] Add refund method selector to RefundRequest.tsx in components/RefundRequest.tsx

**Checkpoint**: User Story 6 complete - filers can request and receive refunds with proper ledger tracking

---

## Phase 9: User Story 7 - Audit Trail of All Ledger Entries (Priority: P1 - Critical)

**Goal**: Provide complete audit trail for all ledger entries showing who, when, what, and any modifications

**Independent Test**:
1. Payment $10,000 made on 04/25/2024 by filer123
2. Journal Entry #JE-2024-00125 created by System on 04/25 at 10:32 AM
3. Entry posted by FinanceOfficer on 04/25 at 2:15 PM
4. Adjustment -$100 on 04/30 via reversing entry
5. Audit trail shows all 5 events with timestamps and user IDs

**Status**: ✅ COMPLETE - AuditLogService fully implemented

### Tests for User Story 7

- [ ] T066 [P] [US7] Create AuditLogServiceTest to verify audit trail logging in backend/ledger-service/src/test/java/com/munitax/ledger/service/AuditLogServiceTest.java
- [ ] T067 [P] [US7] Add test for audit trail query by entity and date range in backend/ledger-service/src/test/java/com/munitax/ledger/service/AuditLogServiceTest.java
- [ ] T068 [P] [US7] Add integration test for audit log API endpoints in backend/ledger-service/src/test/java/com/munitax/ledger/controller/AuditControllerTest.java

### Implementation for User Story 7

- [ ] T069 [US7] Verify JournalEntryService prevents deletion of posted entries per FR-049 in backend/ledger-service/src/main/java/com/munitax/ledger/service/JournalEntryService.java
- [ ] T070 [US7] Add reversing entry method to JournalEntryService per FR-050 in backend/ledger-service/src/main/java/com/munitax/ledger/service/JournalEntryService.java
- [ ] T071 [US7] Add audit log access tracking per FR-051 to AuditLogService in backend/ledger-service/src/main/java/com/munitax/ledger/service/AuditLogService.java
- [ ] T072 [US7] Add query method for audit trail by journal entry ID in AuditLogService in backend/ledger-service/src/main/java/com/munitax/ledger/service/AuditLogService.java
- [ ] T073 [US7] Add API endpoint GET /api/v1/audit/journal-entries/{entryId} in backend/ledger-service/src/main/java/com/munitax/ledger/controller/AuditController.java
- [ ] T074 [P] [US7] Create frontend AuditTrail.tsx component to display audit history in components/AuditTrail.tsx
- [ ] T075 [US7] Add timeline view to AuditTrail.tsx showing chronological events in components/AuditTrail.tsx
- [ ] T076 [US7] Add filtering (by user, by action type, by date) to AuditTrail.tsx in components/AuditTrail.tsx

**Checkpoint**: User Story 7 complete - complete audit trail available for all ledger operations

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, testing, documentation, and deployment readiness

### Integration & End-to-End Testing

- [ ] T077 [P] Create end-to-end test for complete payment flow (assessment → payment → ledger → statement) in backend/ledger-service/src/test/java/com/munitax/ledger/integration/PaymentFlowIntegrationTest.java
- [ ] T078 [P] Create end-to-end test for refund flow (overpayment → refund request → approval → issuance) in backend/ledger-service/src/test/java/com/munitax/ledger/integration/RefundFlowIntegrationTest.java
- [ ] T079 [P] Create end-to-end test for reconciliation accuracy across multiple filers in backend/ledger-service/src/test/java/com/munitax/ledger/integration/ReconciliationIntegrationTest.java
- [ ] T080 Create performance test for trial balance with large dataset (10,000+ entries) in backend/ledger-service/src/test/java/com/munitax/ledger/performance/TrialBalancePerformanceTest.java

### Frontend Integration

- [ ] T081 [P] Create LedgerDashboard.tsx component integrating all ledger features in components/LedgerDashboard.tsx
- [ ] T082 Add navigation between payment, statement, and reconciliation features to LedgerDashboard.tsx in components/LedgerDashboard.tsx
- [ ] T083 Add role-based view (filer vs municipality accountant) to LedgerDashboard.tsx in components/LedgerDashboard.tsx

### API Documentation

- [ ] T084 [P] Add OpenAPI/Swagger documentation for all ledger-service endpoints in backend/ledger-service/src/main/java/com/munitax/ledger/config/SwaggerConfig.java
- [ ] T085 [P] Create API usage examples document in backend/ledger-service/docs/api-examples.md
- [ ] T086 [P] Document test card numbers and expected behaviors in backend/ledger-service/docs/testing-guide.md

### Configuration & Deployment

- [ ] T087 [P] Add configuration for payment mode toggle (TEST/PRODUCTION) with admin UI in backend/ledger-service/src/main/java/com/munitax/ledger/controller/ConfigController.java
- [ ] T088 [P] Add health check endpoints for ledger-service in backend/ledger-service/src/main/java/com/munitax/ledger/controller/HealthController.java
- [ ] T089 Create deployment guide with environment variable documentation in backend/ledger-service/docs/deployment.md

### Data Migration & Seeding

- [ ] T090 [P] Create data seeding script for demo/testing with sample filers and transactions in backend/ledger-service/src/main/resources/db/migration/V2__Seed_test_data.sql
- [ ] T091 Add script to generate realistic test data (100 filers, 1000 transactions) in backend/ledger-service/src/test/resources/data-generator.sql

### Security Hardening

- [ ] T092 [P] Add input validation for all payment endpoints to prevent injection attacks in backend/ledger-service/src/main/java/com/munitax/ledger/controller/PaymentController.java
- [ ] T093 [P] Add rate limiting for payment processing endpoints in backend/ledger-service/src/main/java/com/munitax/ledger/config/RateLimitConfig.java
- [ ] T094 Add audit logging for all sensitive operations (payment mode toggle, refund approval) in backend/ledger-service/src/main/java/com/munitax/ledger/service/AuditLogService.java

### Error Handling & Resilience

- [ ] T095 [P] Add global exception handler for ledger-service in backend/ledger-service/src/main/java/com/munitax/ledger/exception/GlobalExceptionHandler.java
- [ ] T096 Add circuit breaker for external payment provider integration (future real gateway) in backend/ledger-service/src/main/java/com/munitax/ledger/config/CircuitBreakerConfig.java
- [ ] T097 Add retry logic for journal entry creation failures in backend/ledger-service/src/main/java/com/munitax/ledger/service/JournalEntryService.java

### Documentation

- [ ] T098 [P] Create feature README documenting all user stories and capabilities in backend/ledger-service/README.md
- [ ] T099 [P] Create accounting concepts guide for developers unfamiliar with double-entry bookkeeping in backend/ledger-service/docs/accounting-primer.md
- [ ] T100 Create troubleshooting guide for common ledger issues in backend/ledger-service/docs/troubleshooting.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: ✅ COMPLETE - All infrastructure exists
- **Foundational (Phase 2)**: ✅ COMPLETE - Core services implemented
- **User Stories (Phase 3-9)**: Can proceed in parallel once Foundational is verified
  - US1 (Payment Mock Provider): 🎯 MVP - High priority
  - US2 (Tax Assessment): ✅ COMPLETE - Needs testing
  - US3 (Reconciliation): ⚠️ Partial - Needs production implementation
  - US4 (Account Statement): ✅ COMPLETE - Needs frontend
  - US5 (Trial Balance): ❌ NEW - Full implementation needed
  - US6 (Refund Processing): ✅ COMPLETE - Needs testing and frontend
  - US7 (Audit Trail): ✅ COMPLETE - Needs frontend
- **Polish (Phase 10)**: Depends on completed user stories

### User Story Dependencies

- **US1 (P1)**: Independent - Can complete first ✅
- **US2 (P1)**: Independent - Backend complete, needs tests ✅
- **US3 (P2)**: Depends on US1 and US2 (needs transactions to reconcile)
- **US4 (P2)**: Depends on US1 and US2 (needs transactions to display)
- **US5 (P3)**: Depends on US1-US4 (needs data for trial balance)
- **US6 (P2)**: Independent - Backend complete, needs tests ✅
- **US7 (P1)**: Independent - Backend complete, needs frontend ✅

### Critical Path (MVP - US1 Only)

1. ✅ Foundation exists (Phase 2 complete)
2. T001-T003: Write and verify tests for payment flow
3. T004-T007: Enhance frontend PaymentGateway component
4. T008-T010: Add configuration and receipt generation
5. ✅ US1 COMPLETE: Deploy payment testing capability

### Recommended Execution Order

**Week 1 - Critical Testing & US1 Frontend** (MVP)
- T001-T003: US1 Tests (payment provider)
- T004-T010: US1 Frontend integration
- Milestone: Filers can make test payments with ledger entries

**Week 2 - Testing Existing Services**
- T011-T013: US2 Tests (tax assessment)
- T054-T056: US6 Tests (refunds)
- T066-T068: US7 Tests (audit trail)
- Milestone: Core backend services verified

**Week 3 - Reconciliation Production Implementation**
- T018-T020: US3 Tests
- T021-T026: US3 Production reconciliation logic
- T027-T028: US3 Frontend
- Milestone: Two-way reconciliation working

**Week 4 - Account Statements & Frontend**
- T029-T031: US4 Tests
- T032-T037: US4 Backend enhancements
- T038-T040: US4 Frontend
- T063-T065: US6 Frontend (refunds)
- T074-T076: US7 Frontend (audit trail)
- Milestone: Filers can view statements, request refunds, see audit trail

**Week 5 - Trial Balance (Future)**
- T041-T053: US5 Complete implementation
- Milestone: Municipality can generate trial balance

**Week 6 - Polish & Production Ready**
- T077-T100: Integration tests, documentation, security, deployment
- Milestone: Production deployment ready

### Parallel Opportunities

Within each week, tasks marked [P] can be executed in parallel:

**Week 1 Parallel Tasks**:
- T001, T002, T003 (different test files)
- T008 (config) can run parallel to T004-T007 (frontend)

**Week 2 Parallel Tasks**:
- T011, T012, T013 (US2 tests)
- T054, T055, T056 (US6 tests)
- T066, T067, T068 (US7 tests)
- All can run in parallel (different test files)

**Week 3 Parallel Tasks**:
- T018, T019, T020 (US3 tests)
- T027 (frontend) can start while T021-T026 (backend) are in progress

**Week 6 Parallel Tasks**:
- Most polish tasks (T084-T100) can run in parallel as they touch different concerns

---

## Implementation Strategy

### MVP First (User Story 1 Only)

**Goal**: Enable payment testing capability
**Estimated Effort**: 3-5 days
**Tasks**: T001-T010 (10 tasks)

**Value Delivered**:
- Filers can test payment flows without risk
- Mock payment provider fully integrated
- Ledger entries created automatically
- Foundation for all other user stories

### Incremental Delivery Strategy

1. **Sprint 1 (Week 1)**: US1 Complete → Test payments working
2. **Sprint 2 (Week 2)**: US2, US6, US7 tested → Core backend verified
3. **Sprint 3 (Week 3)**: US3 Complete → Reconciliation working
4. **Sprint 4 (Week 4)**: US4 + Frontend → Filer experience complete
5. **Sprint 5 (Week 5)**: US5 Complete → Municipality reporting complete
6. **Sprint 6 (Week 6)**: Polish → Production ready

### Validation Checkpoints

After each user story phase, validate:
- ✅ All tests pass
- ✅ User story acceptance criteria met from spec.md
- ✅ Independent test scenario works
- ✅ API documentation updated
- ✅ Frontend integration working (if applicable)
- ✅ No regression in other stories

---

## Summary

**Total Tasks**: 100 tasks across 10 phases

**Task Breakdown by Phase**:
- Phase 1 (Setup): ✅ 0 tasks (already complete)
- Phase 2 (Foundational): ✅ 0 tasks (already complete)
- Phase 3 (US1 - Payment Mock): 10 tasks (3 tests, 7 implementation)
- Phase 4 (US2 - Tax Assessment): 7 tasks (3 tests, 4 implementation) - Backend complete
- Phase 5 (US3 - Reconciliation): 11 tasks (3 tests, 8 implementation) - Partial
- Phase 6 (US4 - Account Statement): 12 tasks (3 tests, 9 implementation) - Backend complete
- Phase 7 (US5 - Trial Balance): 13 tasks (3 tests, 10 implementation) - New
- Phase 8 (US6 - Refunds): 12 tasks (3 tests, 9 implementation) - Backend complete
- Phase 9 (US7 - Audit Trail): 11 tasks (3 tests, 8 implementation) - Backend complete
- Phase 10 (Polish): 24 tasks (cross-cutting concerns)

**Current Implementation Status**:
- ✅ **Backend Core**: 85% complete (missing: US3 production reconciliation, US5 trial balance)
- ⚠️ **Frontend**: 20% complete (basic PaymentGateway exists, needs enhancement and new components)
- ⚠️ **Testing**: 10% complete (2 test files exist, need 20+ more test classes)
- ❌ **Integration**: 0% complete (no end-to-end tests)
- ❌ **Documentation**: 0% complete (needs API docs, deployment guide, troubleshooting)

**Parallel Execution Opportunities**:
- **39 tasks** marked [P] can run in parallel
- Test tasks (T001-T068) can mostly run in parallel
- Frontend tasks (T027, T038-T040, T051-T053, T063-T065, T074-T076, T081-T083) can run parallel to backend work
- Polish tasks (T084-T100) can run in parallel

**Recommended MVP Scope**:
- Phase 3 (US1) only: 10 tasks = 3-5 days
- Delivers: Working payment testing with full ledger integration
- Validates: Core double-entry ledger infrastructure

**Production-Ready Scope**:
- All phases: 100 tasks = 6 weeks with 2-3 developers
- Delivers: Complete ledger system with all 7 user stories
- Includes: Full test coverage, documentation, production deployment

---

## Notes

- Most backend services are already implemented and working per the user's context
- Primary focus should be on:
  1. **Testing**: 30+ test tasks to verify existing implementation
  2. **Frontend**: 15+ UI components for user interaction
  3. **Enhancement**: US3 reconciliation needs production logic, US5 trial balance is new
  4. **Polish**: Integration tests, documentation, deployment readiness
- The double-entry ledger foundation is solid and ready for use
- Mock payment provider is complete with all test scenarios
- All entity models and repositories exist and are working
- The biggest gaps are in testing and frontend integration
