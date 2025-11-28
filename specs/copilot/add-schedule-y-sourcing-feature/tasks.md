# Tasks: Schedule Y Multi-State Sourcing Feature

**Input**: Design documents from `/specs/copilot/add-schedule-y-sourcing-feature/`
**Prerequisites**: plan.md ✅, spec.md ✅ (from /specs/5-schedule-y-sourcing/)

**Tests**: Comprehensive test coverage included as required for financial calculations

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

This is a web application with:
- **Backend**: `backend/tax-engine-service/` (Java 21 Spring Boot)
- **Frontend**: `src/` (React TypeScript)
- **Database**: PostgreSQL migrations in `backend/tax-engine-service/src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and verification of existing structure

- [ ] T001 Review project structure in plan.md and verify existing tax-engine-service setup
- [ ] T002 Verify Spring Boot dependencies in backend/tax-engine-service/pom.xml (Spring Data JPA, PostgreSQL driver, Lombok, Jackson)
- [ ] T003 [P] Verify React dependencies in package.json (React Router, Axios, Tailwind CSS, Recharts, date-fns)
- [ ] T004 [P] Verify test frameworks in pom.xml (JUnit 5, Mockito, TestContainers) and package.json (Vitest, React Testing Library, Playwright)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core database schema, enums, entities, repositories that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Migrations

- [ ] T005 Create Flyway migration V1.30__create_schedule_y_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T006 Create Flyway migration V1.31__create_property_factor_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T007 Create Flyway migration V1.32__create_payroll_factor_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T008 Create Flyway migration V1.33__create_sales_factor_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T009 Create Flyway migration V1.34__create_sale_transaction_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T010 Create Flyway migration V1.35__create_nexus_tracking_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T011 Create Flyway migration V1.36__create_apportionment_audit_log_table.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T012 Create Flyway migration V1.37__add_apportionment_indexes.sql in backend/tax-engine-service/src/main/resources/db/migration/
- [ ] T013 Create Flyway migration V1.38__add_apportionment_constraints.sql in backend/tax-engine-service/src/main/resources/db/migration/

### Enums and Types

- [ ] T014 [P] Create ApportionmentFormula enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/ApportionmentFormula.java
- [ ] T015 [P] Create SourcingMethodElection enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/SourcingMethodElection.java
- [ ] T016 [P] Create ThrowbackElection enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/ThrowbackElection.java
- [ ] T017 [P] Create ServiceSourcingMethod enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/ServiceSourcingMethod.java
- [ ] T018 [P] Create SaleType enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/SaleType.java
- [ ] T019 [P] Create SourcingMethod enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/SourcingMethod.java
- [ ] T020 [P] Create NexusReason enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/NexusReason.java
- [ ] T021 [P] Create AuditChangeType enum in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/AuditChangeType.java

### Domain Entities (7 core entities)

- [ ] T022 [P] Create ScheduleY entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/ScheduleY.java
- [ ] T023 [P] Create PropertyFactor entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/PropertyFactor.java
- [ ] T024 [P] Create PayrollFactor entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/PayrollFactor.java
- [ ] T025 [P] Create SalesFactor entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/SalesFactor.java
- [ ] T026 [P] Create SaleTransaction entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/SaleTransaction.java
- [ ] T027 [P] Create NexusTracking entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/NexusTracking.java
- [ ] T028 [P] Create ApportionmentAuditLog entity in backend/tax-engine-service/src/main/java/com/munitax/taxengine/domain/apportionment/ApportionmentAuditLog.java

### Repositories

- [X] T029 [P] Create ScheduleYRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/ScheduleYRepository.java
- [X] T030 [P] Create PropertyFactorRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/PropertyFactorRepository.java
- [X] T031 [P] Create PayrollFactorRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/PayrollFactorRepository.java
- [X] T032 [P] Create SalesFactorRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/SalesFactorRepository.java
- [X] T033 [P] Create SaleTransactionRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/SaleTransactionRepository.java
- [X] T034 [P] Create NexusTrackingRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/NexusTrackingRepository.java
- [X] T035 [P] Create ApportionmentAuditLogRepository interface in backend/tax-engine-service/src/main/java/com/munitax/taxengine/repository/ApportionmentAuditLogRepository.java

### DTOs

- [X] T036 [P] Create ScheduleYRequest DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/ScheduleYRequest.java
- [X] T037 [P] Create ScheduleYResponse DTO in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/ScheduleYResponse.java
- [X] T038 [P] Create ApportionmentBreakdownDto in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/ApportionmentBreakdownDto.java
- [X] T039 [P] Create NexusStatusDto in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/NexusStatusDto.java
- [X] T040 [P] Create PropertyFactorDto in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/PropertyFactorDto.java
- [X] T041 [P] Create PayrollFactorDto in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/PayrollFactorDto.java
- [X] T042 [P] Create SalesFactorDto in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/SalesFactorDto.java
- [X] T043 [P] Create SaleTransactionDto in backend/tax-engine-service/src/main/java/com/munitax/taxengine/dto/SaleTransactionDto.java

### Core Services (Foundational)

- [X] T044 Create FormulaConfigService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/FormulaConfigService.java (integration with rule-engine-service)
- [X] T045 Create WithholdingIntegrationService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/WithholdingIntegrationService.java (integration with Spec 1 payroll data)
- [X] T046 Create NexusService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/NexusService.java (nexus determination logic)

### Frontend Types

- [X] T047 [P] Create apportionment types in src/types/apportionment.ts (ScheduleY, PropertyFactor, PayrollFactor, SalesFactor interfaces)
- [X] T048 [P] Create sourcing types in src/types/sourcing.ts (SourcingMethodElection, ThrowbackElection, ServiceSourcingMethod enums)
- [X] T049 [P] Create nexus types in src/types/nexus.ts (NexusStatus, NexusReason interfaces)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Multi-State Business Elects Finnigan Method (Priority: P1) 🎯 MVP

**Goal**: Business can elect Finnigan sales factor sourcing and include all sales made by affiliated group in Ohio apportionment calculation

**Independent Test**: Given multi-state corporate group with 3 entities: Parent (OH nexus, $5M sales, $1M OH sales), Sub A (OH nexus, $3M sales, $500K OH sales), Sub B (no OH nexus, $2M sales, $0 OH sales). Under Finnigan: OH apportionment = ($1M + $500K) / ($5M + $3M + $2M) = 15%. Under Joyce: OH apportionment = ($1M + $500K) / ($5M + $3M) = 18.75%.

### Backend Unit Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T050 [P] [US1] Create SourcingServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/SourcingServiceTest.java (test Finnigan vs Joyce calculations)
- [X] T051 [P] [US1] Create ApportionmentServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/ApportionmentServiceTest.java (test sales factor denominator calculation)
- [X] T052 [P] [US1] Create FormulaConfigServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/FormulaConfigServiceTest.java (mock rule engine response)

### Backend Implementation for User Story 1

- [X] T053 [US1] Implement SourcingService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/SourcingService.java (Finnigan vs Joyce logic)
- [X] T054 [US1] Implement ApportionmentService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/ApportionmentService.java (calculate apportionment percentage with formula weights)
- [X] T055 [US1] Implement SalesFactorService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/SalesFactorService.java (calculate sales factor with sourcing election)
- [X] T056 [US1] Create ScheduleYController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/ScheduleYController.java (POST /api/schedule-y endpoint)
- [X] T057 [US1] Add GET /api/schedule-y endpoint in ScheduleYController (list Schedule Y filings with pagination)
- [X] T058 [US1] Add GET /api/schedule-y/{id} endpoint in ScheduleYController (get Schedule Y details)
- [X] T059 [US1] Add validation in ApportionmentService (sales factor 0-100%, election required, validate entity nexus data)
- [X] T060 [US1] Add audit logging in ApportionmentService (create ApportionmentAuditLog entry on election change)

### Backend Integration Tests for User Story 1

- [ ] T061 [US1] Create ScheduleYIntegrationTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/integration/ScheduleYIntegrationTest.java (file Schedule Y with Finnigan election → verify apportionment calculation)

### Frontend Implementation for User Story 1

- [ ] T062 [P] [US1] Create scheduleYService API client in src/services/scheduleYService.ts (POST /schedule-y, GET /schedule-y, GET /schedule-y/{id})
- [ ] T063 [P] [US1] Create useScheduleY hook in src/hooks/useScheduleY.ts (React Query hook for Schedule Y filing mutations)
- [ ] T064 [P] [US1] Create useApportionment hook in src/hooks/useApportionment.ts (React Query hook for apportionment calculation)
- [ ] T065 [US1] Create ScheduleYWizard component in src/components/apportionment/ScheduleYWizard.tsx (multi-step form: Sales → Sourcing Election → Review)
- [ ] T066 [US1] Create SourcingElectionPanel component in src/components/apportionment/SourcingElectionPanel.tsx (Finnigan vs Joyce radio buttons with tooltips)
- [ ] T067 [US1] Create ScheduleYHistory component in src/components/apportionment/ScheduleYHistory.tsx (table showing all Schedule Y filings)
- [ ] T068 [US1] Create ElectionTooltip component in src/components/shared/ElectionTooltip.tsx (explain Finnigan vs Joyce with examples)

### Frontend Tests for User Story 1

- [ ] T069 [P] [US1] Create ScheduleYWizard.test.tsx in src/__tests__/components/ScheduleYWizard.test.tsx (Vitest: render form, select Finnigan, submit)
- [ ] T070 [P] [US1] Create SourcingElectionPanel.test.tsx in src/__tests__/components/SourcingElectionPanel.test.tsx (Vitest: toggle Finnigan/Joyce, display explanation)

### E2E Tests for User Story 1

- [ ] T071 [US1] Create schedule-y-apportionment.spec.ts in src/__tests__/e2e/schedule-y-apportionment.spec.ts (Playwright: login → file Schedule Y → select Finnigan → verify apportionment calculation)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently (file Schedule Y, elect Finnigan, see apportionment)

---

## Phase 4: User Story 2 - Apply Throwback Rule for Destination State Without Nexus (Priority: P1)

**Goal**: System automatically applies throwback rules when business ships goods to states where they lack nexus, throwing sales back to origin state (Ohio)

**Independent Test**: Company ships $100K of goods from Ohio warehouse to California customer. Company has no California nexus (no office, employees, or property in CA). Without throwback: OH sales factor numerator = $0. With throwback: OH sales factor numerator = $100K (thrown back to OH).

### Backend Unit Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T072 [P] [US2] Create ThrowbackServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/ThrowbackServiceTest.java (test throwback rule application, throwout alternative)
- [X] T073 [P] [US2] Create NexusServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/NexusServiceTest.java (test nexus determination: physical, employee, economic, factor presence)

### Backend Implementation for User Story 2

- [X] T074 [US2] Implement ThrowbackService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/ThrowbackService.java (apply throwback/throwout rules per transaction)
- [X] T075 [US2] Enhance NexusService with economic nexus threshold logic (check sales > $500K, 200 transactions)
- [X] T076 [US2] Update SalesFactorService to integrate throwback adjustments (call ThrowbackService for each sale)
- [X] T077 [US2] Create NexusController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/NexusController.java (GET /api/nexus/{businessId} endpoint)
- [X] T078 [US2] Add POST /api/nexus/{businessId}/update endpoint in NexusController (update nexus status for states)
- [X] T079 [US2] Add throwback adjustment display to ScheduleYResponse DTO (line items showing thrown back amounts)
- [ ] T080 [US2] Implement nexus caching in NexusService (Redis cache with 15-minute TTL for performance)

### Backend Integration Tests for User Story 2

- [ ] T081 [US2] Add throwback workflow test in ScheduleYIntegrationTest (file Schedule Y with sales to no-nexus state → verify throwback adjustment applied)

### Frontend Implementation for User Story 2

- [X] T082 [P] [US2] Create nexusService API client in src/services/nexusService.ts (GET /nexus/{businessId}, POST /nexus/{businessId}/update)
- [X] T083 [P] [US2] Create useNexus hook in src/hooks/useNexus.ts (React Query hook for nexus status)
- [ ] T084 [US2] Create ThrowbackElectionPanel component in src/components/apportionment/ThrowbackElectionPanel.tsx (throwback vs throwout radio buttons)
- [ ] T085 [US2] Create NexusTrackingPanel component in src/components/apportionment/NexusTrackingPanel.tsx (table showing nexus status by state with reasons)
- [ ] T086 [US2] Update SalesFactorForm component in src/components/apportionment/SalesFactorForm.tsx (add destination state field, show throwback indicator)
- [ ] T087 [US2] Add throwback adjustment display to ApportionmentBreakdownCard component (show "Sales to [State] - No nexus - Thrown back: $X")

### Frontend Tests for User Story 2

- [ ] T088 [P] [US2] Create ThrowbackElectionPanel.test.tsx in src/__tests__/components/ThrowbackElectionPanel.test.tsx (Vitest: toggle throwback/throwout, display explanation)
- [ ] T089 [P] [US2] Create NexusTrackingPanel.test.tsx in src/__tests__/components/NexusTrackingPanel.test.tsx (Vitest: display nexus status by state)

### E2E Tests for User Story 2

- [ ] T090 [US2] Add throwback workflow to schedule-y-apportionment.spec.ts (enter sale to no-nexus state → verify throwback adjustment → verify apportionment updated)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently (elect sourcing method, apply throwback rules)

---

## Phase 5: User Story 3 - Market-Based Sourcing for Service Revenue (Priority: P1)

**Goal**: System sources service revenue based on where customers receive benefit (market-based sourcing) rather than where employees perform work (cost-of-performance)

**Independent Test**: IT consulting firm has office in Ohio (5 employees) and California (2 employees). Provides $1M consulting project to New York customer (NY-based Fortune 500). Cost-of-performance: 70% OH, 30% CA → NY gets $0. Market-based: 100% to NY → OH gets $0, CA gets $0.

### Backend Unit Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T091 [P] [US3] Add market-based sourcing tests in SourcingServiceTest (test customer location sourcing, cascading fallback rules)
- [X] T092 [P] [US3] Add cost-of-performance tests in SourcingServiceTest (test employee location proration by payroll)

### Backend Implementation for User Story 3

- [X] T093 [US3] Enhance SourcingService with market-based sourcing logic (source to customer location, handle multi-location customers)
- [X] T094 [US3] Add cost-of-performance sourcing to SourcingService (prorate by employee location/payroll from WithholdingIntegrationService)
- [X] T095 [US3] Implement cascading sourcing rules in SourcingService (try market-based → fallback to cost-of-performance → fallback to pro-rata)
- [ ] T096 [US3] Add service sourcing method validation in SalesFactorService (require customer location for market-based)
- [ ] T097 [US3] Add service revenue breakdown to SalesFactorResponse DTO (tangible goods vs services, sourcing method per transaction)

### Backend Integration Tests for User Story 3

- [ ] T098 [US3] Add service sourcing workflow test in ScheduleYIntegrationTest (file Schedule Y with service revenue → verify market-based sourcing applied)

### Frontend Implementation for User Story 3

- [ ] T099 [US3] Create ServiceSourcingPanel component in src/components/apportionment/ServiceSourcingPanel.tsx (market-based vs cost-of-performance radio buttons)
- [ ] T100 [US3] Enhance SalesFactorForm component to handle service transactions (add customer location field, sale type dropdown)
- [ ] T101 [US3] Add customer location prompt in ServiceSourcingPanel (text input with state dropdown, show cascading rules explanation)
- [ ] T102 [US3] Add service revenue breakdown to ApportionmentBreakdownCard (show sourcing method used per transaction)

### Frontend Tests for User Story 3

- [ ] T103 [P] [US3] Create ServiceSourcingPanel.test.tsx in src/__tests__/components/ServiceSourcingPanel.test.tsx (Vitest: toggle sourcing methods, display cascading rules)
- [ ] T104 [P] [US3] Update SalesFactorForm.test.tsx to test service transactions (add customer location, verify sourcing)

### E2E Tests for User Story 3

- [ ] T105 [US3] Add service sourcing workflow to schedule-y-apportionment.spec.ts (enter service revenue → select market-based → enter customer location → verify sourcing)

**Checkpoint**: At this point, all P1 user stories (US1, US2, US3) should work independently and together (complete multi-state sourcing with elections, throwback, and service revenue)

---

## Phase 6: User Story 4 - Display Apportionment Factor Calculation with Breakdown (Priority: P2)

**Goal**: Business sees clear breakdown of three-factor apportionment formula (property, payroll, sales) with double-weighted sales, understands calculation

**Independent Test**: Business has Property: $2M OH / $8M total = 20%, Payroll: $3M OH / $7M total = 42.86%, Sales: $5M OH / $10M total = 50%. Apportionment: (20% + 42.86% + 50% + 50%) / 4 = 40.715%.

### Backend Unit Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T106 [P] [US4] Create PropertyFactorServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/PropertyFactorServiceTest.java (test property factor calculation, rented property capitalization)
- [ ] T107 [P] [US4] Create PayrollFactorServiceTest in backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/PayrollFactorServiceTest.java (test payroll factor calculation, remote employee allocation)
- [ ] T108 [P] [US4] Add four-factor formula tests in ApportionmentServiceTest (test double-weighted sales, final apportionment percentage)

### Backend Implementation for User Story 4

- [ ] T109 [US4] Implement PropertyFactorService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/PropertyFactorService.java (calculate property factor with averaging, rented property capitalization)
- [ ] T110 [US4] Implement PayrollFactorService in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/PayrollFactorService.java (calculate payroll factor, integrate with WithholdingIntegrationService)
- [ ] T111 [US4] Enhance ApportionmentService to calculate weighted formula (apply property/payroll/sales weights from FormulaConfigService)
- [ ] T112 [US4] Add GET /api/schedule-y/{id}/breakdown endpoint in ScheduleYController (return detailed factor breakdown)
- [ ] T113 [US4] Create ApportionmentController in backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/ApportionmentController.java (POST /api/apportionment/calculate endpoint)
- [ ] T114 [US4] Implement breakdown DTO with all factor details (numerator, denominator, percentage, weights, final calculation)

### Backend Integration Tests for User Story 4

- [ ] T115 [US4] Add full apportionment workflow test in ScheduleYIntegrationTest (file Schedule Y with all factors → verify breakdown calculation)

### Frontend Implementation for User Story 4

- [ ] T116 [P] [US4] Create apportionmentService API client in src/services/apportionmentService.ts (POST /apportionment/calculate, GET /schedule-y/{id}/breakdown)
- [ ] T117 [US4] Create PropertyFactorForm component in src/components/apportionment/PropertyFactorForm.tsx (enter OH property, total property, rented property)
- [ ] T118 [US4] Create PayrollFactorForm component in src/components/apportionment/PayrollFactorForm.tsx (enter OH payroll, total payroll, employee counts)
- [ ] T119 [US4] Update SalesFactorForm component for complete sales entry (tangible goods, services, rental, interest, royalties)
- [ ] T120 [US4] Create ApportionmentBreakdownCard component in src/components/apportionment/ApportionmentBreakdownCard.tsx (display factor breakdown with weighted formula)
- [ ] T121 [US4] Create ApportionmentChart component in src/components/apportionment/ApportionmentChart.tsx (Recharts bar/pie chart showing factor contributions)
- [ ] T122 [US4] Create FactorPercentageDisplay component in src/components/shared/FactorPercentageDisplay.tsx (reusable percentage display with numerator/denominator)
- [ ] T123 [US4] Update ScheduleYWizard to include all factor forms (Property → Payroll → Sales → Elections → Review → Submit)

### Frontend Tests for User Story 4

- [ ] T124 [P] [US4] Create PropertyFactorForm.test.tsx in src/__tests__/components/PropertyFactorForm.test.tsx (Vitest: enter property values, calculate factor)
- [ ] T125 [P] [US4] Create PayrollFactorForm.test.tsx in src/__tests__/components/PayrollFactorForm.test.tsx (Vitest: enter payroll values, calculate factor)
- [ ] T126 [P] [US4] Create ApportionmentBreakdownCard.test.tsx in src/__tests__/components/ApportionmentBreakdownCard.test.tsx (Vitest: display breakdown, verify calculation)
- [ ] T127 [P] [US4] Create ApportionmentChart.test.tsx in src/__tests__/components/ApportionmentChart.test.tsx (Vitest: render chart with factor data)

### E2E Tests for User Story 4

- [ ] T128 [US4] Update schedule-y-apportionment.spec.ts with full apportionment workflow (enter property → payroll → sales → elections → verify breakdown → verify chart)

**Checkpoint**: At this point, all core apportionment functionality is complete (US1-US4 work together for full Schedule Y filing with detailed breakdown)

---

## Phase 7: User Story 5 - Handle Single-Sales-Factor Election (Priority: P3)

**Goal**: Business can elect single-sales-factor apportionment (if allowed) to base apportionment solely on sales factor

**Independent Test**: Business has Property: 5% OH, Payroll: 10% OH, Sales: 60% OH. Traditional 4-factor: (5% + 10% + 60% + 60%) / 4 = 33.75%. Single-sales-factor: 60%. System calculates both and recommends option that minimizes tax liability.

### Backend Unit Tests for User Story 5

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T129 [P] [US5] Add single-sales-factor tests in ApportionmentServiceTest (test single-factor calculation, compare to traditional formula)
- [ ] T130 [P] [US5] Add formula comparison tests in ApportionmentServiceTest (verify recommendation logic based on which minimizes tax)

### Backend Implementation for User Story 5

- [ ] T131 [US5] Enhance ApportionmentService to support single-sales-factor formula (override weights to 100% sales, 0% property/payroll)
- [ ] T132 [US5] Add formula comparison logic in ApportionmentService (calculate both formulas, recommend lower apportionment)
- [ ] T133 [US5] Add formula election validation in ApportionmentService (check if municipality allows single-sales-factor via FormulaConfigService)
- [ ] T134 [US5] Add formula comparison to ScheduleYResponse DTO (include traditional calculation, single-sales calculation, recommendation)

### Backend Integration Tests for User Story 5

- [ ] T135 [US5] Add single-sales-factor workflow test in ScheduleYIntegrationTest (file Schedule Y → elect single-sales-factor → verify calculation)

### Frontend Implementation for User Story 5

- [ ] T136 [US5] Add formula election to ScheduleYWizard (show traditional vs single-sales-factor comparison before submission)
- [ ] T137 [US5] Create FormulaComparisonPanel component in src/components/apportionment/FormulaComparisonPanel.tsx (side-by-side comparison with recommendation)
- [ ] T138 [US5] Add formula election radio buttons in ScheduleYWizard (allow override of recommendation if desired)

### Frontend Tests for User Story 5

- [ ] T139 [P] [US5] Create FormulaComparisonPanel.test.tsx in src/__tests__/components/FormulaComparisonPanel.test.tsx (Vitest: display both calculations, show recommendation)

### E2E Tests for User Story 5

- [ ] T140 [US5] Add single-sales-factor workflow to schedule-y-apportionment.spec.ts (enter factors → see comparison → elect single-sales → verify calculation)

**Checkpoint**: All user stories (US1-US5) are now complete and independently functional with full election support

---

## Phase 8: PDF Generation for Schedule Y

**Goal**: Generate PDF Form 27-Y (Schedule Y) with all factors, calculations, and elections for filing and record-keeping

**Independent Test**: File Schedule Y with complete data, click "Download PDF" button, verify PDF includes property factor breakdown, payroll factor breakdown, sales factor breakdown, throwback adjustments, sourcing method elections, and final apportionment percentage.

### Backend Implementation for PDF Generation

- [ ] T141 Create schedule-y-template.html Thymeleaf template in backend/pdf-service/src/main/resources/templates/schedule-y-template.html
- [ ] T142 Add GET /api/schedule-y/{id}/pdf endpoint in ScheduleYController (call pdf-service to generate PDF)
- [ ] T143 Implement PDF generation integration with pdf-service (send Schedule Y data to pdf-service)
- [ ] T144 Add variable-length transaction list handling in template (pagination for 100+ transactions)

### Frontend Implementation for PDF Generation

- [ ] T145 [P] Create ScheduleYPdfViewer component in src/components/apportionment/ScheduleYPdfViewer.tsx (PDF preview and download button)
- [ ] T146 Add "Download PDF" button to ScheduleYHistory component (fetch PDF from API)

### Frontend Tests for PDF Generation

- [ ] T147 Create ScheduleYPdfViewer.test.tsx in src/__tests__/components/ScheduleYPdfViewer.test.tsx (Vitest: click download, verify API call)

---

## Phase 9: Validation & Audit Trail

**Goal**: Comprehensive validation of apportionment data and immutable audit trail for compliance

**Independent Test**: Attempt to file Schedule Y with property factor 150% (invalid), verify error blocked. Change sourcing election from Joyce to Finnigan, verify audit log records: "Changed sourcing election from JOYCE to FINNIGAN on [date] by [user]".

### Backend Implementation for Validation

- [ ] T148 [P] Enhance ApportionmentService with comprehensive validations (factors 0-100%, sum of state apportionments ±5% of 100%)
- [ ] T149 [P] Add consistency validations in ApportionmentService (property factor 0% but business has OH office address → warning)
- [ ] T150 [P] Add election change validations in ApportionmentService (election binding for tax year, cannot change mid-year)
- [ ] T151 Add validation error messages to API responses (clear, actionable error messages)

### Backend Implementation for Audit Trail

- [ ] T152 Implement audit logging in all services (log sourcing election changes, factor adjustments, nexus changes to ApportionmentAuditLog)
- [ ] T153 Add GET /api/schedule-y/{id}/audit-log endpoint in ScheduleYController (retrieve audit trail for Schedule Y)
- [ ] T154 Ensure audit log immutability (INSERT-only table, no UPDATE/DELETE operations)

### Frontend Implementation for Validation

- [ ] T155 [P] Add real-time validation to all factor forms (validate ranges, show error messages inline)
- [ ] T156 [P] Add cross-field validations in ScheduleYWizard (verify data consistency before submission)
- [ ] T157 Add validation summary in review step of ScheduleYWizard (show all warnings/errors before filing)

### Frontend Implementation for Audit Trail

- [ ] T158 Create AuditLogViewer component in src/components/apportionment/AuditLogViewer.tsx (display audit trail with timeline)
- [ ] T159 Add "View Audit Log" button to ScheduleYHistory component (open AuditLogViewer modal)

### Tests for Validation & Audit Trail

- [ ] T160 [P] Add validation tests in ApportionmentServiceTest (test all validation rules, verify error messages)
- [ ] T161 [P] Add audit log tests in ApportionmentServiceTest (verify all changes logged, verify immutability)
- [ ] T162 Create AuditLogViewer.test.tsx in src/__tests__/components/AuditLogViewer.test.tsx (Vitest: display audit entries)

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and production readiness

### Error Handling & Logging

- [ ] T163 [P] Add error handling to all API endpoints in controllers (ValidationException, BusinessException with proper HTTP status codes)
- [ ] T164 [P] Add logging to all services (MDC context with tenant_id, business_id, user_id)
- [ ] T165 [P] Add API documentation comments to all DTOs (JavaDoc for fields)

### Frontend Polish

- [ ] T166 [P] Add accessibility attributes to all form components (aria-label, aria-describedby, keyboard navigation)
- [ ] T167 [P] Add loading states to all frontend components (skeleton loaders for data fetching)
- [ ] T168 [P] Add error boundaries to frontend pages (ErrorBoundary component wrapping Schedule Y pages)
- [ ] T169 [P] Add responsive design for mobile in all apportionment components (375px+ width support, horizontal scroll for tables)
- [ ] T170 [P] Add tooltips to all calculation fields (explain property factor, payroll factor, sales factor with examples)

### Performance Optimization

- [ ] T171 Performance optimization: Add database query indexes per V1.37 migration (indexes on schedule_y.return_id, nexus_tracking.business_id)
- [ ] T172 Performance optimization: Implement caching for nexus determinations (Redis cache with 15-minute TTL)
- [ ] T173 Performance optimization: Optimize apportionment calculation for 1000+ transactions (batch processing, aggregate queries)
- [ ] T174 Performance benchmark: Verify apportionment calculation <3 seconds for 1000 transactions (load test with JMeter)
- [ ] T175 Performance benchmark: Verify Schedule Y PDF generation <5 seconds (test with 500 transactions)

### Security Hardening

- [ ] T176 Security hardening: Add rate limiting to Schedule Y filing endpoint (max 10 filings per minute per business)
- [ ] T177 Security hardening: Add authorization checks (ROLE_BUSINESS can file own Schedule Y, ROLE_AUDITOR can view all)
- [ ] T178 Security hardening: Add input sanitization to all form fields (prevent XSS, SQL injection)
- [ ] T179 Security hardening: Add encryption for sensitive financial data at rest (property values, sales amounts)

### Documentation & Code Quality

- [ ] T180 Run code cleanup: Execute Checkstyle and fix violations in backend/tax-engine-service/
- [ ] T181 Run code cleanup: Execute ESLint and fix violations in src/
- [ ] T182 Generate OpenAPI documentation: Run springdoc-openapi-maven-plugin to generate api-docs.yaml for Schedule Y endpoints
- [ ] T183 [P] Update README.md with Schedule Y multi-state sourcing feature overview
- [ ] T184 [P] Create APPORTIONMENT_GUIDE.md in docs/ folder (user guide for filing Schedule Y, explaining elections)
- [ ] T185 [P] Update API_DOCUMENTATION.md with Schedule Y endpoint details

### Integration Testing

- [ ] T186 Run full integration test suite: Execute all Schedule Y integration tests with TestContainers
- [ ] T187 Run E2E test suite: Execute all Playwright tests for Schedule Y workflows
- [ ] T188 Verify multi-year comparison: Test Schedule Y filing for 3 consecutive years, verify historical data access

### Deployment Readiness

- [ ] T189 Verify Flyway migrations execute successfully in test environment (V1.30 through V1.38)
- [ ] T190 Verify rule engine integration: Test retrieval of apportionment formulas from rule-engine-service
- [ ] T191 Verify withholding integration: Test payroll factor auto-population from Spec 1 W-1 filings
- [ ] T192 Run smoke tests: Verify all Schedule Y endpoints return 200 OK with valid data
- [ ] T193 Perform security scan: Run OWASP dependency check on backend/tax-engine-service/
- [ ] T194 Perform accessibility scan: Run axe-core on all Schedule Y pages

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **PDF Generation (Phase 8)**: Depends on User Story 4 completion (requires full Schedule Y data)
- **Validation & Audit Trail (Phase 9)**: Depends on all user stories (cross-cutting concern)
- **Polish (Phase 10)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Integrates with US1 but independently testable
- **User Story 3 (P1)**: Can start after Foundational (Phase 2) - Integrates with US1 but independently testable
- **User Story 4 (P2)**: Depends on User Stories 1, 2, 3 completion (displays breakdown of all factors)
- **User Story 5 (P3)**: Depends on User Story 4 completion (requires full apportionment calculation to compare formulas)

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Enums before entities
- Entities before repositories
- Repositories before services
- Services before controllers
- DTOs before controllers
- Backend API before frontend components
- Frontend hooks before frontend components
- Component tests before E2E tests
- Story complete before moving to next priority

### Parallel Opportunities

- **Setup**: All 4 setup tasks can run in parallel (different verification tasks)
- **Foundational - Migrations**: T005-T013 can run in parallel (9 migration files)
- **Foundational - Enums**: T014-T021 can run in parallel (8 enum files)
- **Foundational - Entities**: T022-T028 can run in parallel (7 entity files)
- **Foundational - Repositories**: T029-T035 can run in parallel (7 repository files)
- **Foundational - DTOs**: T036-T043 can run in parallel (8 DTO files)
- **Foundational - Frontend Types**: T047-T049 can run in parallel (3 type files)
- **User Story 1 - Unit Tests**: T050-T052 can run in parallel (3 test files)
- **User Story 1 - Frontend Services**: T062-T064 can run in parallel (3 files: API client + 2 hooks)
- **User Story 1 - Frontend Tests**: T069-T070 can run in parallel (2 component tests)
- **User Story 2 - Unit Tests**: T072-T073 can run in parallel (2 test files)
- **User Story 2 - Frontend Services**: T082-T083 can run in parallel (API client + hook)
- **User Story 2 - Frontend Tests**: T088-T089 can run in parallel (2 component tests)
- **User Story 3 - Frontend Tests**: T103-T104 can run in parallel (2 component tests)
- **User Story 4 - Unit Tests**: T106-T108 can run in parallel (3 test files)
- **User Story 4 - Frontend Tests**: T124-T127 can run in parallel (4 component tests)
- **User Story 5 - Unit Tests**: T129-T130 can run in parallel (2 test files)
- **Phase 9 - Validations**: T148-T151 can run in parallel (4 validation areas)
- **Phase 9 - Frontend Validations**: T155-T157 can run in parallel (3 validation areas)
- **Phase 9 - Audit Tests**: T160-T161 can run in parallel (2 test areas)
- **Phase 10 - Error Handling**: T163-T165 can run in parallel (3 areas)
- **Phase 10 - Frontend Polish**: T166-T170 can run in parallel (5 areas)
- **Phase 10 - Documentation**: T183-T185 can run in parallel (3 documentation files)
- **Once Foundational phase completes**: User Stories 1, 2, and 3 can start in parallel (all P1, independent)

---

## Parallel Example: User Story 1

```bash
# Launch all unit tests for User Story 1 together:
Task T050: "Create SourcingServiceTest"
Task T051: "Create ApportionmentServiceTest"
Task T052: "Create FormulaConfigServiceTest"

# Launch all frontend services for User Story 1 together:
Task T062: "Create scheduleYService API client"
Task T063: "Create useScheduleY hook"
Task T064: "Create useApportionment hook"

# Launch all frontend component tests for User Story 1 together:
Task T069: "Create ScheduleYWizard.test.tsx"
Task T070: "Create SourcingElectionPanel.test.tsx"
```

---

## Parallel Example: User Story 4

```bash
# Launch all unit tests for User Story 4 together:
Task T106: "Create PropertyFactorServiceTest"
Task T107: "Create PayrollFactorServiceTest"
Task T108: "Add four-factor formula tests in ApportionmentServiceTest"

# Launch all frontend component tests for User Story 4 together:
Task T124: "Create PropertyFactorForm.test.tsx"
Task T125: "Create PayrollFactorForm.test.tsx"
Task T126: "Create ApportionmentBreakdownCard.test.tsx"
Task T127: "Create ApportionmentChart.test.tsx"
```

---

## Implementation Strategy

### MVP First (User Stories 1, 2, 3 Only - All P1)

Since US1, US2, and US3 are all Priority P1 and represent the core multi-state sourcing requirements:

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (elect Finnigan/Joyce)
4. Complete Phase 4: User Story 2 (apply throwback rules)
5. Complete Phase 5: User Story 3 (market-based sourcing for services)
6. **STOP and VALIDATE**: Test US1 + US2 + US3 together (complete multi-state sourcing workflow)
7. Deploy/demo if ready

**MVP Value**: Business can file Schedule Y with complete multi-state sourcing: elect Finnigan method for affiliated group sales, apply throwback rules for no-nexus states, and use market-based sourcing for service revenue. This satisfies 90%+ of multi-state business filer needs.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (sourcing election only)
3. Add User Story 2 → Test with US1 → Deploy/Demo (sourcing + throwback)
4. Add User Story 3 → Test with US1+US2 → Deploy/Demo (MVP! Complete sourcing with services)
5. Add User Story 4 → Test with US1+US2+US3 → Deploy/Demo (add visual breakdown)
6. Add User Story 5 → Test with US4 → Deploy/Demo (add single-sales-factor election)
7. Add PDF Generation → Test with all stories → Deploy/Demo (add PDF export)
8. Add Validation & Audit Trail → Test with all stories → Deploy/Demo (production hardening)
9. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers after Foundational phase completes:

**Scenario 1: 3 Developers**
- Developer A: User Story 1 (P1) - Finnigan/Joyce election
- Developer B: User Story 2 (P1) - Throwback rules
- Developer C: User Story 3 (P1) - Service sourcing
- Once all complete: Integrate and test together (MVP!)

**Scenario 2: 5 Developers**
- Developer A: User Story 1 (P1)
- Developer B: User Story 2 (P1)
- Developer C: User Story 3 (P1)
- Developer D: User Story 4 (P2) - starts after US1+US2+US3 complete
- Developer E: User Story 5 (P3) - starts after US4 complete

**Scenario 3: Full Team (7 developers)**
- Dev A: US1 (P1)
- Dev B: US2 (P1)
- Dev C: US3 (P1)
- Dev D: US4 (P2) - waits for US1+US2+US3
- Dev E: US5 (P3) - waits for US4
- Dev F: PDF Generation (Phase 8) - waits for US4
- Dev G: Validation & Audit (Phase 9) - waits for all stories

---

## Summary

**Total Tasks**: 194 tasks
- **Phase 1 (Setup)**: 4 tasks
- **Phase 2 (Foundational)**: 45 tasks (9 migrations + 8 enums + 7 entities + 7 repositories + 8 DTOs + 3 services + 3 types)
- **Phase 3 (User Story 1)**: 22 tasks (3 unit tests + 8 backend + 1 integration test + 7 frontend + 2 frontend tests + 1 E2E test)
- **Phase 4 (User Story 2)**: 19 tasks (2 unit tests + 7 backend + 1 integration test + 6 frontend + 2 frontend tests + 1 E2E test)
- **Phase 5 (User Story 3)**: 15 tasks (2 unit tests + 5 backend + 1 integration test + 4 frontend + 2 frontend tests + 1 E2E test)
- **Phase 6 (User Story 4)**: 23 tasks (3 unit tests + 6 backend + 1 integration test + 8 frontend + 4 frontend tests + 1 E2E test)
- **Phase 7 (User Story 5)**: 12 tasks (2 unit tests + 4 backend + 1 integration test + 3 frontend + 1 frontend test + 1 E2E test)
- **Phase 8 (PDF Generation)**: 7 tasks (4 backend + 2 frontend + 1 frontend test)
- **Phase 9 (Validation & Audit Trail)**: 15 tasks (8 backend + 4 frontend + 3 tests)
- **Phase 10 (Polish)**: 32 tasks (performance, security, documentation, quality)

**Task Breakdown by User Story**:
- **US1**: 22 tasks (P1 - MVP core: Finnigan/Joyce election)
- **US2**: 19 tasks (P1 - MVP core: Throwback rules)
- **US3**: 15 tasks (P1 - MVP core: Service sourcing)
- **US4**: 23 tasks (P2 - High value: Visual breakdown)
- **US5**: 12 tasks (P3 - Future: Single-sales-factor)

**Parallel Opportunities Identified**: 20+ groups of parallelizable tasks across all phases

**Independent Test Criteria**:
- **US1**: File Schedule Y with Finnigan election → verify sales factor denominator includes all group sales
- **US2**: Enter sale to no-nexus state → verify throwback adjustment applied
- **US3**: Enter service revenue with customer location → verify market-based sourcing applied
- **US4**: File complete Schedule Y → verify factor breakdown displays with weights
- **US5**: Enter factors → verify single-sales-factor calculation and comparison

**Suggested MVP Scope**: 
- Phase 1 (Setup): 4 tasks
- Phase 2 (Foundational): 45 tasks
- Phase 3 (User Story 1): 22 tasks
- Phase 4 (User Story 2): 19 tasks
- Phase 5 (User Story 3): 15 tasks
- **Total MVP**: 105 tasks (54% of total)

This MVP delivers the complete P1 requirement: businesses can file Schedule Y with comprehensive multi-state sourcing including Finnigan/Joyce election, throwback rules for no-nexus states, and market-based sourcing for service revenue, meeting modern state apportionment compliance requirements.

---

## Notes

- [P] tasks = different files, no dependencies (can run in parallel)
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD approach)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All file paths are absolute from repository root
- All database migrations follow sequential versioning (V1.30 through V1.38)
- All entities include tenant_id for multi-tenant isolation per Constitution II
- All audit-sensitive actions logged to ApportionmentAuditLog per Constitution III
- All apportionment calculations subject to 7-year retention (IRS IRC § 6001)
- Performance targets: Apportionment calculation <3 seconds for 1000 transactions, PDF generation <5 seconds
- Security: Rate limiting, authorization checks, input sanitization, encryption at rest
