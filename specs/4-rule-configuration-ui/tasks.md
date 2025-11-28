# Tasks: Dynamic Rule Configuration System

**Input**: Design documents from `/specs/4-rule-configuration-ui/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure for new rule-service microservice

- [ ] T001 Create rule-service microservice directory structure at backend/rule-service/
- [ ] T002 Initialize Maven project for rule-service with Spring Boot 3.2.3 dependencies in backend/rule-service/pom.xml
- [ ] T003 [P] Configure application.yml for rule-service in backend/rule-service/src/main/resources/application.yml
- [ ] T004 [P] Setup Flyway database migrations directory in backend/rule-service/src/main/resources/db/migration/
- [ ] T005 [P] Configure Redis connection settings in backend/rule-service/src/main/resources/application.yml
- [ ] T006 [P] Setup Eureka service discovery registration in backend/rule-service/src/main/resources/application.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Schema

- [ ] T007 Create V1__create_tax_rules_table.sql migration in backend/rule-service/src/main/resources/db/migration/
- [ ] T008 Create V2__create_rule_change_log_table.sql migration in backend/rule-service/src/main/resources/db/migration/
- [ ] T009 Create V3__create_tenant_rls_policies.sql migration in backend/rule-service/src/main/resources/db/migration/
- [ ] T010 Run Flyway migrations to create database schema

### Core Entities and Enums

- [ ] T011 [P] Create RuleCategory enum in backend/rule-service/src/main/java/com/munitax/rules/model/RuleCategory.java
- [ ] T012 [P] Create RuleValueType enum in backend/rule-service/src/main/java/com/munitax/rules/model/RuleValueType.java
- [ ] T013 [P] Create ApprovalStatus enum in backend/rule-service/src/main/java/com/munitax/rules/model/ApprovalStatus.java
- [ ] T014 [P] Create ChangeType enum in backend/rule-service/src/main/java/com/munitax/rules/model/ChangeType.java
- [ ] T015 [P] Create TaxRule entity in backend/rule-service/src/main/java/com/munitax/rules/model/TaxRule.java
- [ ] T016 [P] Create RuleChangeLog entity in backend/rule-service/src/main/java/com/munitax/rules/model/RuleChangeLog.java

### Repositories

- [ ] T017 [P] Create TaxRuleRepository interface in backend/rule-service/src/main/java/com/munitax/rules/repository/TaxRuleRepository.java
- [ ] T018 [P] Create RuleChangeLogRepository interface in backend/rule-service/src/main/java/com/munitax/rules/repository/RuleChangeLogRepository.java

### DTOs

- [ ] T019 [P] Create CreateRuleRequest DTO in backend/rule-service/src/main/java/com/munitax/rules/dto/CreateRuleRequest.java
- [ ] T020 [P] Create UpdateRuleRequest DTO in backend/rule-service/src/main/java/com/munitax/rules/dto/UpdateRuleRequest.java
- [ ] T021 [P] Create RuleResponse DTO in backend/rule-service/src/main/java/com/munitax/rules/dto/RuleResponse.java
- [ ] T022 [P] Create RuleHistoryResponse DTO in backend/rule-service/src/main/java/com/munitax/rules/dto/RuleHistoryResponse.java

### Core Services

- [ ] T023 Create RuleCacheService with Redis operations in backend/rule-service/src/main/java/com/munitax/rules/service/RuleCacheService.java
- [ ] T024 [P] Create RuleValidationService with overlap detection in backend/rule-service/src/main/java/com/munitax/rules/service/RuleValidationService.java
- [ ] T025 [P] Create TemporalRuleService with date-range queries in backend/rule-service/src/main/java/com/munitax/rules/service/TemporalRuleService.java
- [ ] T026 Create RuleManagementService with CRUD operations in backend/rule-service/src/main/java/com/munitax/rules/service/RuleManagementService.java

### Security Configuration

- [ ] T027 Configure Spring Security with TAX_ADMINISTRATOR role enforcement in backend/rule-service/src/main/java/com/munitax/rules/config/SecurityConfig.java
- [ ] T028 Create JWT authentication filter in backend/rule-service/src/main/java/com/munitax/rules/security/JwtAuthenticationFilter.java

### Main Application

- [ ] T029 Create RuleServiceApplication main class in backend/rule-service/src/main/java/com/munitax/rules/RuleServiceApplication.java

### Frontend Foundation

- [ ] T030 [P] Create TypeScript interfaces in types.ts for TaxRule, RuleChangeLog, and related types
- [ ] T031 [P] Create ruleService.ts API client in services/ruleService.ts

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Tax Administrator Updates Municipal Rate (Priority: P1) 🎯 MVP

**Goal**: Enable tax administrators to configure new municipal tax rates with effective dates, supporting temporal rule logic (2025 returns use old rate, 2026 returns use new rate)

**Independent Test**: Create rule "Municipal Rate = 2.25%" with effective date 2026-01-01. System should apply 2.0% to returns for tax year 2025 and 2.25% to returns for tax year 2026.

### Implementation for User Story 1

- [ ] T032 [P] [US1] Create RuleConfigController with POST /api/rules endpoint in backend/rule-service/src/main/java/com/munitax/rules/controller/RuleConfigController.java
- [ ] T033 [P] [US1] Create RuleQueryController with GET /api/rules/active endpoint in backend/rule-service/src/main/java/com/munitax/rules/controller/RuleQueryController.java
- [ ] T034 [US1] Implement createRule method in RuleManagementService in backend/rule-service/src/main/java/com/munitax/rules/service/RuleManagementService.java
- [ ] T035 [US1] Implement getActiveRules temporal query in TemporalRuleService in backend/rule-service/src/main/java/com/munitax/rules/service/TemporalRuleService.java
- [ ] T036 [US1] Implement validateNoOverlap method in RuleValidationService in backend/rule-service/src/main/java/com/munitax/rules/service/RuleValidationService.java
- [ ] T037 [US1] Implement approveRule method in RuleManagementService with cache invalidation in backend/rule-service/src/main/java/com/munitax/rules/service/RuleManagementService.java
- [ ] T038 [US1] Implement updateRule method with retroactive change prevention in backend/rule-service/src/main/java/com/munitax/rules/service/RuleManagementService.java
- [ ] T039 [US1] Add validation for effective date logic (prevent modification after effective date) in backend/rule-service/src/main/java/com/munitax/rules/service/RuleValidationService.java

### Frontend for User Story 1

- [ ] T040 [P] [US1] Create RuleConfigurationDashboard component in components/admin/RuleConfigurationDashboard.tsx
- [ ] T041 [P] [US1] Create RuleEditor component with form validation in components/admin/RuleEditor.tsx
- [ ] T042 [P] [US1] Create TemporalRuleEditor component with date range picker in components/admin/TemporalRuleEditor.tsx
- [ ] T043 [US1] Add route for /admin/rules in App.tsx or router configuration
- [ ] T044 [US1] Implement overlap detection UI feedback in TemporalRuleEditor component in components/admin/TemporalRuleEditor.tsx

### Integration

- [ ] T045 [US1] Create V4__migrate_existing_rules_from_constants.sql with initial municipal rates in backend/rule-service/src/main/resources/db/migration/
- [ ] T046 [US1] Test end-to-end workflow: create rule → approve → query active rules → verify temporal logic

**Checkpoint**: User Story 1 complete - administrators can configure municipal rates with effective dates

---

## Phase 4: User Story 2 - Multi-Tenant Rule Management (Priority: P1)

**Goal**: Enable CPA firms to manage rules for multiple municipal clients (Dublin, Columbus, Cleveland) with tenant-specific rule sets

**Independent Test**: Configure 3 tenants with different municipal rates (Dublin: 2.0%, Columbus: 2.5%, Cleveland: 2.0%). When selecting "Dublin" tenant, system applies 2.0% rate; when selecting "Columbus", applies 2.5% rate.

### Implementation for User Story 2

- [ ] T047 [P] [US2] Implement tenant filtering in TaxRuleRepository queries in backend/rule-service/src/main/java/com/munitax/rules/repository/TaxRuleRepository.java
- [ ] T048 [P] [US2] Implement tenant context extraction from JWT in backend/rule-service/src/main/java/com/munitax/rules/security/TenantContextHolder.java
- [ ] T049 [US2] Add tenant_id validation in RuleValidationService in backend/rule-service/src/main/java/com/munitax/rules/service/RuleValidationService.java
- [ ] T050 [US2] Implement tenant-specific cache invalidation in RuleCacheService in backend/rule-service/src/main/java/com/munitax/rules/service/RuleCacheService.java
- [ ] T051 [US2] Implement GET /api/rules with tenant filter in RuleConfigController in backend/rule-service/src/main/java/com/munitax/rules/controller/RuleConfigController.java

### Frontend for User Story 2

- [ ] T052 [P] [US2] Create TenantComparisonView component in components/admin/TenantComparisonView.tsx
- [ ] T053 [P] [US2] Add tenant dropdown filter to RuleList component in components/admin/RuleList.tsx
- [ ] T054 [US2] Implement side-by-side rule comparison table in TenantComparisonView in components/admin/TenantComparisonView.tsx
- [ ] T055 [US2] Add tenant selector to RuleEditor form in components/admin/RuleEditor.tsx

### Integration

- [ ] T056 [US2] Seed sample rules for 3 tenants (Dublin, Columbus, Cleveland) in V4 migration
- [ ] T057 [US2] Test multi-tenant isolation: verify tenant A cannot see tenant B rules
- [ ] T058 [US2] Test tenant comparison view with different rate configurations

**Checkpoint**: User Story 2 complete - multi-tenant rule management functional

---

## Phase 5: User Story 3 - Individual Tax Rules (W-2 Qualifying Wages) (Priority: P2)

**Goal**: Enable administrators to configure W-2 qualifying wages calculation method (Highest of All Boxes vs Box 5 Medicare Only) without code deployment

**Independent Test**: Configure rule "W2 Qualifying Wages Method = BOX_5_MEDICARE" effective 2026-01-01. When calculating 2026 return with W-2 showing Box 1=$50K, Box 5=$52K, system should use Box 5 value ($52K).

### Implementation for User Story 3

- [ ] T059 [P] [US3] Create FormulaEvaluationService with SpEL expression parser in backend/rule-service/src/main/java/com/munitax/rules/service/FormulaEvaluationService.java
- [ ] T060 [US3] Implement evaluateFormula method with security constraints in FormulaEvaluationService in backend/rule-service/src/main/java/com/munitax/rules/service/FormulaEvaluationService.java
- [ ] T061 [US3] Implement validateFormula method with variable whitelist in FormulaEvaluationService in backend/rule-service/src/main/java/com/munitax/rules/service/FormulaEvaluationService.java
- [ ] T062 [US3] Add formula validation to RuleValidationService in backend/rule-service/src/main/java/com/munitax/rules/service/RuleValidationService.java
- [ ] T063 [US3] Create WhatIfAnalysisRequest DTO in backend/rule-service/src/main/java/com/munitax/rules/dto/WhatIfAnalysisRequest.java
- [ ] T064 [US3] Implement POST /api/rules/what-if endpoint in RuleConfigController in backend/rule-service/src/main/java/com/munitax/rules/controller/RuleConfigController.java

### Frontend for User Story 3

- [ ] T065 [P] [US3] Create WhatIfAnalysisTool component in components/admin/WhatIfAnalysisTool.tsx
- [ ] T066 [P] [US3] Create FormulaBuilder component with syntax highlighting in components/admin/FormulaBuilder.tsx
- [ ] T067 [US3] Add formula testing tool with sample input values in FormulaBuilder in components/admin/FormulaBuilder.tsx
- [ ] T068 [US3] Integrate WhatIfAnalysisPreview into RuleEditor modal in components/admin/RuleEditor.tsx
- [ ] T069 [US3] Add route for /admin/rules/what-if in App.tsx

### Tax Calculator Integration

- [ ] T070 [US3] Create RuleServiceClient REST client in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/RuleServiceClient.java
- [ ] T071 [US3] Refactor IndividualTaxCalculator to query rules from rule-service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/IndividualTaxCalculator.java
- [ ] T072 [US3] Update W-2 qualifying wages logic to use dynamic rule in IndividualTaxCalculator in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/IndividualTaxCalculator.java

### Integration

- [ ] T073 [US3] Seed W-2 qualifying wages rules for all tenants in V4 migration
- [ ] T074 [US3] Test what-if analysis with sample returns showing impact of rule change
- [ ] T075 [US3] Test tax calculation uses correct W-2 rule for tax year

**Checkpoint**: User Story 3 complete - W-2 qualifying wages rules are configurable

---

## Phase 6: User Story 4 - Business Entity-Specific Rules (Priority: P2)

**Goal**: Enable entity-specific income inclusion rules (e.g., dividend income deductible for C-Corps but not for Partnerships)

**Independent Test**: Configure rule "Dividend Income Inclusion: C-Corp=false, Partnership=true". When C-Corp reports $10K dividends, system deducts full $10K; when Partnership reports $10K dividends, system deducts $0.

### Implementation for User Story 4

- [ ] T076 [P] [US4] Add entity type filtering to TemporalRuleService queries in backend/rule-service/src/main/java/com/munitax/rules/service/TemporalRuleService.java
- [ ] T077 [US4] Implement entity-specific rule precedence logic in TemporalRuleService in backend/rule-service/src/main/java/com/munitax/rules/service/TemporalRuleService.java
- [ ] T078 [US4] Add entity type multi-select to RuleEditor form in components/admin/RuleEditor.tsx
- [ ] T079 [US4] Implement conditional rule evaluation for entity targeting in FormulaEvaluationService in backend/rule-service/src/main/java/com/munitax/rules/service/FormulaEvaluationService.java

### Tax Calculator Integration

- [ ] T080 [US4] Refactor BusinessTaxCalculator to query rules from rule-service in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/BusinessTaxCalculator.java
- [ ] T081 [US4] Update dividend income deduction logic to use entity-specific rules in BusinessTaxCalculator in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/BusinessTaxCalculator.java
- [ ] T082 [US4] Update NOL carryforward logic to use entity-specific rules in BusinessTaxCalculator in backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/BusinessTaxCalculator.java

### Integration

- [ ] T083 [US4] Seed entity-specific income inclusion rules in V4 migration
- [ ] T084 [US4] Test C-Corp calculation with dividend deduction rule
- [ ] T085 [US4] Test Partnership calculation without dividend deduction rule
- [ ] T086 [US4] Test entity-specific rule precedence (specific overrides general)

**Checkpoint**: User Story 4 complete - entity-specific rules functional

---

## Phase 7: User Story 5 - Version Control and Audit Trail (Priority: P3)

**Goal**: Provide complete audit trail for rule changes to support state auditor reviews

**Independent Test**: Query rule history for "Municipal Rate" from 2020-2024. System should return all versions with timestamps, change reasons, and who made changes.

### Implementation for User Story 5

- [ ] T087 [P] [US5] Create RuleHistoryController with GET /api/rules/{id}/history endpoint in backend/rule-service/src/main/java/com/munitax/rules/controller/RuleHistoryController.java
- [ ] T088 [P] [US5] Implement getRuleHistory method in RuleManagementService in backend/rule-service/src/main/java/com/munitax/rules/service/RuleManagementService.java
- [ ] T089 [US5] Implement point-in-time rule query (as-of date) in TemporalRuleService in backend/rule-service/src/main/java/com/munitax/rules/service/TemporalRuleService.java
- [ ] T090 [US5] Implement rollbackRule method (within 24 hours) in RuleManagementService in backend/rule-service/src/main/java/com/munitax/rules/service/RuleManagementService.java
- [ ] T091 [US5] Add audit logging for all rule changes in RuleChangeLogRepository in backend/rule-service/src/main/java/com/munitax/rules/repository/RuleChangeLogRepository.java
- [ ] T092 [US5] Implement GET /api/rules/audit/report endpoint for PDF export in RuleHistoryController in backend/rule-service/src/main/java/com/munitax/rules/controller/RuleHistoryController.java

### Frontend for User Story 5

- [ ] T093 [P] [US5] Create RuleHistoryViewer component with timeline view in components/admin/RuleHistoryViewer.tsx
- [ ] T094 [P] [US5] Add diff viewer showing before/after values in RuleHistoryViewer in components/admin/RuleHistoryViewer.tsx
- [ ] T095 [US5] Add rollback button to RuleHistoryViewer with 24-hour constraint in components/admin/RuleHistoryViewer.tsx
- [ ] T096 [US5] Create audit report export functionality in RuleHistoryViewer in components/admin/RuleHistoryViewer.tsx
- [ ] T097 [US5] Add route for /admin/rules/{id}/history in App.tsx

### Integration

- [ ] T098 [US5] Test version chain links (previous_version_id references)
- [ ] T099 [US5] Test point-in-time query returns correct historical rule
- [ ] T100 [US5] Test audit report generation with date range filter
- [ ] T101 [US5] Test rollback functionality within 24-hour window

**Checkpoint**: User Story 5 complete - full audit trail and version control available

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

### Testing

- [ ] T102 [P] Create RuleValidationServiceTest unit tests in backend/rule-service/src/test/java/com/munitax/rules/service/RuleValidationServiceTest.java
- [ ] T103 [P] Create TemporalRuleServiceTest unit tests in backend/rule-service/src/test/java/com/munitax/rules/service/TemporalRuleServiceTest.java
- [ ] T104 [P] Create FormulaEvaluationServiceTest unit tests in backend/rule-service/src/test/java/com/munitax/rules/service/FormulaEvaluationServiceTest.java
- [ ] T105 [P] Create RuleConfigControllerTest integration tests in backend/rule-service/src/test/java/com/munitax/rules/controller/RuleConfigControllerTest.java
- [ ] T106 [P] Create RuleServiceIntegrationTest with Testcontainers in backend/rule-service/src/test/java/com/munitax/rules/integration/RuleServiceIntegrationTest.java
- [ ] T107 [P] Create cache invalidation tests with Redis in backend/rule-service/src/test/java/com/munitax/rules/service/RuleCacheServiceTest.java
- [ ] T108 [P] Create frontend component tests for RuleEditor in components/admin/__tests__/RuleEditor.test.tsx
- [ ] T109 [P] Create frontend component tests for WhatIfAnalysisTool in components/admin/__tests__/WhatIfAnalysisTool.test.tsx

### Performance & Optimization

- [ ] T110 [P] Implement cache warming on service startup in backend/rule-service/src/main/java/com/munitax/rules/cache/CacheWarmer.java
- [ ] T111 [P] Add circuit breaker for Redis connections in backend/rule-service/src/main/java/com/munitax/rules/config/ResilienceConfig.java
- [ ] T112 [P] Configure Spring Boot Actuator metrics for monitoring in backend/rule-service/src/main/resources/application.yml
- [ ] T113 Add performance benchmarking tests for rule queries

### Documentation

- [ ] T114 [P] Update quickstart.md with final implementation details in specs/4-rule-configuration-ui/quickstart.md
- [ ] T115 [P] Add API documentation comments to controllers in backend/rule-service/
- [ ] T116 [P] Create operator manual for tax administrators in docs/operator-manual.md

### Security

- [ ] T117 Run security audit for SpEL expression evaluation
- [ ] T118 Verify row-level security policies enforce tenant isolation
- [ ] T119 Test self-approval prevention logic
- [ ] T120 Verify JWT authentication on all endpoints

### Migration & Deployment

- [ ] T121 Create deployment guide in docs/deployment-guide.md
- [ ] T122 Configure Docker compose for local development in docker-compose.yml
- [ ] T123 Create Kubernetes manifests for rule-service in k8s/rule-service/
- [ ] T124 Test backward compatibility with existing tax calculations
- [ ] T125 Run quickstart.md validation with fresh environment

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational - Can start after Phase 2
- **User Story 2 (Phase 4)**: Depends on Foundational - Can start after Phase 2 (or in parallel with US1)
- **User Story 3 (Phase 5)**: Depends on Foundational + US1 (needs basic rule CRUD) - Start after US1
- **User Story 4 (Phase 6)**: Depends on Foundational + US1 - Can run parallel with US3
- **User Story 5 (Phase 7)**: Depends on US1 (needs rule changes to audit) - Start after US1
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Foundation + basic CRUD + temporal logic + approval workflow
  - **Blocking**: None (can start after Foundational)
  - **Required for**: US3, US5 (other stories need basic rule management working)
  
- **User Story 2 (P1)**: Multi-tenant isolation + tenant filtering
  - **Blocking**: None (can run parallel with US1)
  - **Required for**: All subsequent stories (multi-tenancy is foundational)
  
- **User Story 3 (P2)**: Formula evaluation + what-if analysis
  - **Blocking**: Needs US1 (basic rule CRUD must work first)
  - **Required for**: US4 (conditional rules build on formula engine)
  
- **User Story 4 (P2)**: Entity-specific rules + tax calculator integration
  - **Blocking**: Needs US1 (basic rule CRUD must work first)
  - **Required for**: None (other stories don't depend on entity filtering)
  
- **User Story 5 (P3)**: Version control + audit trail + rollback
  - **Blocking**: Needs US1 (must have rule changes to audit)
  - **Required for**: None (audit trail is enhancement, not blocker)

### Within Each User Story

- Backend services before controllers
- Controllers before frontend components
- Frontend components before integration tests
- All story components before checkpoint validation

### Parallel Opportunities

**Setup Phase**:
- T003, T004, T005, T006 can run in parallel (different config files)

**Foundational Phase**:
- T011-T016 (enums and entities) can run in parallel
- T017-T018 (repositories) can run in parallel after entities
- T019-T022 (DTOs) can run in parallel
- T024-T025 (validation services) can run in parallel

**User Story 1**:
- T032, T033 (controllers) can run in parallel
- T040, T041, T042 (frontend components) can run in parallel

**User Story 2**:
- T047, T048 (tenant filtering) can run in parallel
- T052, T053 (frontend components) can run in parallel

**User Story 3**:
- T065, T066 (frontend components) can run in parallel
- T070, T071 (tax calculator changes) can run after rule-service ready

**User Story 4**:
- T080, T081, T082 (tax calculator updates) can run in parallel

**User Story 5**:
- T087, T088 (history endpoints) can run in parallel
- T093, T094 (frontend components) can run in parallel

**Polish Phase**:
- T102-T109 (all tests) can run in parallel
- T110-T112 (performance tasks) can run in parallel
- T114-T116 (documentation) can run in parallel

**Once Foundational completes, multiple user stories can proceed in parallel if team capacity allows**

---

## Parallel Example: Foundational Phase

```bash
# After T010 (database migrations), launch entity/enum creation in parallel:
Task T011: Create RuleCategory enum
Task T012: Create RuleValueType enum  
Task T013: Create ApprovalStatus enum
Task T014: Create ChangeType enum
Task T015: Create TaxRule entity
Task T016: Create RuleChangeLog entity

# After entities complete, launch repositories in parallel:
Task T017: Create TaxRuleRepository
Task T018: Create RuleChangeLogRepository

# Launch DTOs in parallel (independent of repositories):
Task T019: Create CreateRuleRequest DTO
Task T020: Create UpdateRuleRequest DTO
Task T021: Create RuleResponse DTO
Task T022: Create RuleHistoryResponse DTO
```

---

## Parallel Example: User Story 1

```bash
# After foundational services (T023-T026) complete, launch in parallel:
Task T032: Create RuleConfigController (POST /api/rules)
Task T033: Create RuleQueryController (GET /api/rules/active)

# Launch frontend components in parallel:
Task T040: Create RuleConfigurationDashboard
Task T041: Create RuleEditor component
Task T042: Create TemporalRuleEditor component
```

---

## Implementation Strategy

### MVP First (User Stories 1 & 2 Only)

**Recommended MVP Scope**: User Story 1 + User Story 2

1. Complete Phase 1: Setup → ~1 day
2. Complete Phase 2: Foundational → ~3-5 days (CRITICAL - blocks everything)
3. Complete Phase 3: User Story 1 → ~3-4 days
4. Complete Phase 4: User Story 2 → ~2-3 days
5. **STOP and VALIDATE**: Test municipal rate configuration + multi-tenant isolation
6. Deploy/demo MVP

**MVP Value**: Administrators can configure municipal tax rates with effective dates for multiple tenants - addresses #1 pain point (hardcoded rates)

**Total MVP Timeline**: ~2 weeks

### Incremental Delivery

1. **Setup + Foundational** → Foundation ready (~1 week)
2. **Add User Story 1** → Basic rule configuration with temporal logic (~3-4 days)
3. **Add User Story 2** → Multi-tenant support (~2-3 days)
4. **DEPLOY MVP** → Validate in production with real users
5. **Add User Story 3** → Formula evaluation + what-if analysis (~4-5 days)
6. **Add User Story 4** → Entity-specific rules (~3-4 days)
7. **Add User Story 5** → Audit trail + version control (~3-4 days)
8. **Polish** → Testing, performance, documentation (~1 week)

**Total Timeline**: 4-6 weeks for complete feature

### Parallel Team Strategy

With 3 developers available after Foundational phase completes:

1. **Team completes Setup + Foundational together** (~1 week)
2. Once Foundational is done:
   - **Developer A**: User Story 1 (backend + frontend)
   - **Developer B**: User Story 2 (backend + frontend)
   - **Developer C**: User Story 3 preparation (research formula libraries, design FormulaBuilder UI)
3. **After US1 + US2 complete** (~1.5 weeks):
   - **Developer A**: User Story 3 (formula evaluation)
   - **Developer B**: User Story 4 (entity-specific rules)
   - **Developer C**: User Story 5 (audit trail)
4. **Final integration** (~3-4 days):
   - All developers: Polish, testing, documentation

**Parallel Timeline**: 3-4 weeks for complete feature (vs 4-6 weeks sequential)

---

## Notes

- [P] tasks = different files, no dependencies - can run in parallel
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Foundational phase (Phase 2) is CRITICAL - it blocks all user stories
- MVP = User Stories 1 + 2 (addresses primary pain points)
- User Stories 3-5 are enhancements that build on MVP
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

---

## Task Count Summary

- **Setup**: 6 tasks
- **Foundational**: 23 tasks (BLOCKING - highest priority)
- **User Story 1**: 15 tasks (P1 - MVP)
- **User Story 2**: 12 tasks (P1 - MVP)
- **User Story 3**: 17 tasks (P2)
- **User Story 4**: 11 tasks (P2)
- **User Story 5**: 15 tasks (P3)
- **Polish**: 24 tasks

**Total**: 125 tasks

**MVP Task Count** (Setup + Foundational + US1 + US2): 56 tasks (~45% of total)

**Parallel Opportunities**: ~40 tasks marked [P] for parallel execution

---

## Success Criteria per User Story

### User Story 1 Success Criteria
- ✅ Admin can create rule with effective date in future
- ✅ Rule starts with PENDING status
- ✅ Approver can approve rule (different user than creator)
- ✅ Approved rule becomes active on effective date
- ✅ Tax calculations use correct rate based on tax year
- ✅ Cannot modify rule after effective date has passed

### User Story 2 Success Criteria
- ✅ Rules are isolated per tenant (Dublin cannot see Columbus rules)
- ✅ CPA can view side-by-side comparison of rules across tenants
- ✅ Tax calculations apply correct tenant-specific rates
- ✅ Cache invalidation is tenant-scoped (Dublin change doesn't clear Columbus cache)

### User Story 3 Success Criteria
- ✅ Admin can configure W-2 qualifying wages rule (HIGHEST_OF_ALL vs BOX_5_MEDICARE)
- ✅ What-if analysis shows impact on sample returns
- ✅ Tax calculations use configured W-2 rule for qualifying wages
- ✅ Formula evaluation is secure (no code injection possible)

### User Story 4 Success Criteria
- ✅ Admin can configure entity-specific rules (C-Corp vs Partnership)
- ✅ Dividend deduction applies to C-Corps but not Partnerships
- ✅ Entity-specific rules override general rules (precedence works)
- ✅ Tax calculations use correct entity-specific rules

### User Story 5 Success Criteria
- ✅ Rule history shows all versions with timestamps and change reasons
- ✅ Point-in-time query returns correct historical rule
- ✅ Audit report exports to PDF with date range filter
- ✅ Rollback works within 24 hours of change
- ✅ Audit log is immutable (cannot UPDATE or DELETE)

---

**Last Updated**: 2025-11-28
