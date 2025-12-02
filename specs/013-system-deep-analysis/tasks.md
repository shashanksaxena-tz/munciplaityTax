# Tasks: System Deep Analysis & Gap Identification

**Input**: Design documents from `/specs/013-system-deep-analysis/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Tests**: Not applicable - this is a documentation/analysis feature.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Analysis Output**: `specs/013-system-deep-analysis/analysis/`
- **Backend Source**: `backend/{service}/src/main/java/`
- **Frontend Source**: `src/`, `components/`, `services/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create analysis output directory and verify access to source files

- [ ] T001 Create analysis output directory `specs/013-system-deep-analysis/analysis/`
- [ ] T002 [P] Verify access to all 9 backend service directories in `backend/`
- [ ] T003 [P] Verify access to frontend source in `src/`, `components/`, `services/`
- [ ] T004 [P] Review existing documentation files (CURRENT_FEATURES.md, Gaps.md, ARCHITECTURE.md)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data collection that MUST be complete before user story-specific analysis

**⚠️ CRITICAL**: User story analysis cannot begin until raw data collection is complete

### Backend API Inventory

- [ ] T005 Scan auth-service controllers: `backend/auth-service/src/main/java/com/munitax/auth/controller/*.java`
- [ ] T006 [P] Scan tenant-service controllers: `backend/tenant-service/src/main/java/com/munitax/tenant/controller/*.java`
- [ ] T007 [P] Scan extraction-service controllers: `backend/extraction-service/src/main/java/com/munitax/extraction/controller/*.java`
- [ ] T008 [P] Scan submission-service controllers: `backend/submission-service/src/main/java/com/munitax/submission/controller/*.java`
- [ ] T009 [P] Scan tax-engine-service controllers: `backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/*.java`
- [ ] T010 [P] Scan pdf-service controllers: `backend/pdf-service/src/main/java/com/munitax/pdf/controller/*.java`
- [ ] T011 [P] Scan rule-service controllers: `backend/rule-service/src/main/java/com/munitax/rule/controller/*.java`
- [ ] T012 [P] Scan ledger-service controllers: `backend/ledger-service/src/main/java/com/munitax/ledger/controller/*.java`
- [ ] T013 [P] Scan gateway-service for routing configuration

### Frontend API Client Inventory

- [ ] T014 Parse `services/api.ts` to extract all API method definitions
- [ ] T015 [P] Parse `services/sessionService.ts` for additional API calls
- [ ] T016 [P] Parse `services/ruleService.ts` for additional API calls
- [ ] T017 [P] Scan `src/hooks/` directory for React Query hooks with API calls

### UI Component Inventory

- [ ] T018 Catalog all components in `components/` root directory
- [ ] T019 [P] Catalog all components in `src/components/` directory
- [ ] T020 [P] Catalog all components in `components/auth/` directory
- [ ] T021 [P] Catalog all components in `components/forms/` directory
- [ ] T022 [P] Catalog all components in `components/profile/` directory

**Checkpoint**: Raw data collection complete - user story analysis can now proceed

---

## Phase 3: User Story 1 - Technical Lead Reviews API Coverage Report (Priority: P1) 🎯 MVP

**Goal**: Generate comprehensive API coverage report mapping backend endpoints to frontend consumers

**Independent Test**: Review report and verify each endpoint correctly shows consumer status

### Implementation for User Story 1

- [ ] T023 [US1] Create `analysis/api-coverage-report.md` with header and summary template
- [ ] T024 [US1] Document auth-service endpoints (AuthController, UserController) with consumers
- [ ] T025 [P] [US1] Document tenant-service endpoints (AddressController, SessionController) with consumers
- [ ] T026 [P] [US1] Document extraction-service endpoints with consumers
- [ ] T027 [P] [US1] Document submission-service endpoints (SubmissionController, AuditController) with consumers
- [ ] T028 [P] [US1] Document tax-engine-service endpoints (TaxEngineController, ScheduleXController, ScheduleYController, etc.) with consumers
- [ ] T029 [P] [US1] Document pdf-service endpoints (PdfController, FormGenerationController) with consumers
- [ ] T030 [P] [US1] Document rule-service endpoints with consumers
- [ ] T031 [P] [US1] Document ledger-service endpoints (all 8 controllers) with consumers
- [ ] T032 [US1] Cross-reference to identify UNUSED endpoints (backend without frontend consumer)
- [ ] T033 [US1] Cross-reference to identify API MISSING (frontend calls without backend)
- [ ] T034 [US1] Generate summary statistics (total endpoints, used, unused, coverage percentage)
- [ ] T035 [US1] Create "Unused Endpoints" section in report
- [ ] T036 [US1] Create "Missing Backend APIs" section in report

**Checkpoint**: API coverage report complete - Technical Lead can review endpoint mapping

---

## Phase 4: User Story 2 - Product Owner Reviews User Journey Gaps (Priority: P1)

**Goal**: Document all user journeys with step-by-step implementation status

**Independent Test**: Walk through each journey in the app and verify report accuracy

### Implementation for User Story 2

- [ ] T037 [US2] Create `analysis/user-journey-report.md` with header and summary template
- [ ] T038 [US2] Document Individual Tax Filing journey (6 steps: upload → extract → review → calculate → submit → pay)
- [ ] T039 [US2] Document Business Net Profits journey (5 steps: federal → scheduleX → scheduleY → calculate → submit)
- [ ] T040 [US2] Document Auditor Review journey (6 steps: queue → assign → review → decision → docs → sign)
- [ ] T041 [US2] Document Administrator Configuration journey (4 steps: login → rules → tenants → reports)
- [ ] T042 [US2] Add mermaid flowcharts for each journey with status colors
- [ ] T043 [US2] Validate role-based logical constraints (auditor login should not request SSN)
- [ ] T044 [US2] List critical gaps per journey
- [ ] T045 [US2] Calculate completion percentage per journey
- [ ] T046 [US2] Generate overall summary table

**Checkpoint**: User journey report complete - Product Owner can prioritize development

---

## Phase 5: User Story 3 - Architect Reviews Swagger Documentation Status (Priority: P2)

**Goal**: Document Swagger availability for each microservice with remediation steps

**Independent Test**: Click each Swagger link to verify availability

### Implementation for User Story 3

- [ ] T047 [US3] Create `analysis/swagger-status.md` with header template
- [ ] T048 [US3] Check auth-service pom.xml for springdoc-openapi dependency
- [ ] T049 [P] [US3] Check tenant-service pom.xml for springdoc-openapi dependency
- [ ] T050 [P] [US3] Check extraction-service pom.xml for springdoc-openapi dependency
- [ ] T051 [P] [US3] Check submission-service pom.xml for springdoc-openapi dependency
- [ ] T052 [P] [US3] Check tax-engine-service pom.xml for springdoc-openapi dependency
- [ ] T053 [P] [US3] Check pdf-service pom.xml for springdoc-openapi dependency
- [ ] T054 [P] [US3] Check rule-service pom.xml for springdoc-openapi dependency
- [ ] T055 [P] [US3] Check ledger-service pom.xml for springdoc-openapi dependency
- [ ] T056 [US3] Generate status table with service name, port, status, URL
- [ ] T057 [US3] Add remediation steps for services missing Swagger

**Checkpoint**: Swagger status documented - Architect can plan documentation improvements

---

## Phase 6: User Story 4 - Developer Reviews Rule Engine Integration (Priority: P2)

**Goal**: Document rule engine integration status and database disconnect issue

**Independent Test**: Review database configurations to verify disconnect

### Implementation for User Story 4

- [ ] T058 [US4] Create `analysis/rule-engine-analysis.md` with header template
- [ ] T059 [US4] Review rule-service application.yml database configuration
- [ ] T060 [US4] Review tax-engine-service application.yml database configuration
- [ ] T061 [US4] Document database disconnect between services
- [ ] T062 [US4] Enumerate all rule categories (8 categories from RULE_ENGINE.md)
- [ ] T063 [US4] For each category, identify if rules are DYNAMIC or HARDCODED
- [ ] T064 [US4] Document where hardcoded rules exist in Java source
- [ ] T065 [US4] Create integration diagram showing current vs intended data flow
- [ ] T066 [US4] Provide specific remediation steps for database unification

**Checkpoint**: Rule engine analysis complete - Developer can plan integration fix

---

## Phase 7: User Story 5 - QA Lead Reviews Sequence Flow Completeness (Priority: P3)

**Goal**: Document sequence diagrams with implementation status annotations

**Independent Test**: Compare diagrams against actual system behavior

### Implementation for User Story 5

- [ ] T067 [US5] Create `analysis/sequence-diagrams.md` with header template
- [ ] T068 [US5] Document authentication flow sequence with implementation status
- [ ] T069 [US5] Document document extraction flow sequence with implementation status
- [ ] T070 [US5] Document individual tax calculation flow with implementation status
- [ ] T071 [US5] Document business tax calculation flow with implementation status
- [ ] T072 [US5] Document payment processing flow with implementation status (mostly MISSING)
- [ ] T073 [US5] Document auditor review flow with implementation status (mostly MISSING)
- [ ] T074 [US5] Add mermaid sequence diagrams for each flow
- [ ] T075 [US5] Annotate each step with IMPLEMENTED/PARTIAL/MISSING status

**Checkpoint**: Sequence diagrams complete - QA Lead can create test plans

---

## Phase 8: User Story 6 - Security Officer Reviews Data Flow (Priority: P3)

**Goal**: Document sensitive data flow through the system

**Independent Test**: Trace SSN/EIN fields through documented data flows

### Implementation for User Story 6

- [ ] T076 [US6] Create `analysis/data-flow-diagrams.md` with header template
- [ ] T077 [US6] Document SSN data flow (entry → storage → display)
- [ ] T078 [US6] Document EIN data flow for businesses
- [ ] T079 [US6] Document bank account data flow (for payments - planned)
- [ ] T080 [US6] Identify data protection measures at each step
- [ ] T081 [US6] Identify potential security gaps in data handling
- [ ] T082 [US6] Document which UI components display masked vs unmasked PII

**Checkpoint**: Data flow documentation complete - Security Officer can review

---

## Phase 9: UI Component Inventory (Cross-Cutting)

**Purpose**: Complete catalog of all React components

- [ ] T083 Create `analysis/ui-component-inventory.md` with header template
- [ ] T084 [P] Catalog page-level components with implementation status
- [ ] T085 [P] Catalog form components with implementation status
- [ ] T086 [P] Catalog wizard components with implementation status
- [ ] T087 [P] Catalog dashboard components with implementation status
- [ ] T088 [P] Catalog shared/utility components
- [ ] T089 Map each component to user journeys it participates in
- [ ] T090 Identify components with missing API dependencies

**Checkpoint**: UI component inventory complete

---

## Phase 10: Gap Report Generation (Final)

**Purpose**: Consolidate all findings into prioritized gap report

- [ ] T091 Create `analysis/gap-report.md` with header and executive summary
- [ ] T092 Compile all CRITICAL gaps from previous analyses
- [ ] T093 [P] Compile all HIGH priority gaps
- [ ] T094 [P] Compile all MEDIUM priority gaps
- [ ] T095 [P] Compile all LOW priority gaps
- [ ] T096 Cross-reference gaps with existing specs (1-12)
- [ ] T097 Add remediation steps for each gap
- [ ] T098 Estimate effort for each gap (SMALL/MEDIUM/LARGE/XLARGE)
- [ ] T099 Create remediation roadmap by phase
- [ ] T100 Generate final summary statistics

**Checkpoint**: All analysis reports complete - ready for stakeholder review

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup - BLOCKS all user stories
- **User Stories (Phases 3-8)**: All depend on Foundational phase completion
  - US1 and US2 (P1) should be prioritized
  - US3-US6 can proceed in parallel after foundational
- **UI Inventory (Phase 9)**: Can run in parallel with user stories
- **Gap Report (Phase 10)**: Depends on all previous phases

### Parallel Opportunities

- All [P] marked tasks within a phase can run in parallel
- After Phase 2, all user stories can be worked on in parallel
- Different analysis reports can be written simultaneously

---

## Implementation Notes

- All output files are markdown format for easy GitHub viewing
- Use mermaid diagrams for visual representations
- Follow schema definitions in `contracts/` directory
- Cross-reference with existing documentation in `docs/`
- Verify findings against actual source code, not just documentation
