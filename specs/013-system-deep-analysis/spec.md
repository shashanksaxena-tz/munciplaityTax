# Feature Specification: System Deep Analysis & Gap Identification

**Feature Branch**: `013-system-deep-analysis`  
**Created**: December 2, 2025  
**Status**: Draft  
**Input**: Comprehensive system analysis covering:
- Sequence flows and data flows
- User journeys with role-based logical validation
- Rule engine and tax engine integration
- API coverage mapping (backend ↔ frontend)
- Swagger documentation status
- UI component inventory and gap identification

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Technical Lead Reviews API Coverage Report (Priority: P1)

As a technical lead, I want to view a comprehensive report that maps all backend API endpoints to their consuming frontend components, so that I can identify orphaned APIs (unused endpoints) and UI components with missing backend support.

**Why this priority**: Understanding the current state of API-UI integration is foundational for all other analysis work. Without this mapping, teams cannot prioritize development efforts or identify critical gaps.

**Independent Test**: Can be fully tested by reviewing the generated API coverage report and verifying that each listed API endpoint correctly maps to its UI consumer or is marked as "unused."

**Acceptance Scenarios**:

1. **Given** a complete system with 9 backend microservices, **When** the analysis runs, **Then** a report is generated listing all API endpoints from each service (auth-service, tenant-service, tax-engine-service, extraction-service, submission-service, pdf-service, rule-service, ledger-service, gateway-service).

2. **Given** frontend components consuming backend APIs, **When** the analysis completes, **Then** each API endpoint shows which UI component(s) consume it, or is marked as "UNUSED" if no frontend consumer exists.

3. **Given** UI components requiring backend data, **When** the analysis completes, **Then** each component shows its API dependencies, or is flagged as "NO API LINKAGE" if the required backend endpoint does not exist.

---

### User Story 2 - Product Owner Reviews User Journey Gaps (Priority: P1)

As a product owner, I want to see a complete map of all user journeys (taxpayer individual filing, business filing, auditor review, administrator configuration) with clear indicators showing which steps are incomplete or have broken flows, so that I can prioritize development work.

**Why this priority**: User journey completeness directly impacts whether the system is usable for its intended purpose. Incomplete journeys mean users cannot complete their tasks.

**Independent Test**: Can be fully tested by walking through each documented user journey and verifying that the report correctly identifies complete vs. incomplete steps.

**Acceptance Scenarios**:

1. **Given** the taxpayer individual filing journey (document upload → extraction → review → calculation → submission → payment), **When** the analysis runs, **Then** each step shows its completion status (COMPLETE, PARTIAL, MISSING) with specific details about what's working vs. missing.

2. **Given** the business net profits filing journey (federal data entry → Schedule X reconciliation → Schedule Y allocation → tax calculation → submission), **When** the analysis runs, **Then** the report identifies all incomplete steps, particularly highlighting where Schedule X fields are insufficient compared to production requirements as documented in Gaps.md.

3. **Given** the auditor review journey (view queue → assign case → review return → approve/reject → document request → e-signature), **When** the analysis runs, **Then** the report shows which auditor workflow steps exist vs. are completely missing (currently 0% implemented per documentation).

4. **Given** logical business rules (e.g., "auditor login does not require SSN or location"), **When** the analysis runs, **Then** the report validates that user flows respect role-based logical constraints and flags any violations.

---

### User Story 3 - Architect Reviews Swagger Documentation Status (Priority: P2)

As a system architect, I want each backend microservice to have a clear link to its Swagger/OpenAPI documentation (or a status indicating documentation is missing), so that developers and integrators can understand available endpoints.

**Why this priority**: API documentation is essential for development velocity and integration. Missing documentation creates knowledge silos and slows onboarding.

**Independent Test**: Can be fully tested by clicking each Swagger link in the report and verifying it resolves to valid API documentation, or shows appropriate "NOT AVAILABLE" status.

**Acceptance Scenarios**:

1. **Given** 9 backend microservices, **When** the analysis completes, **Then** a documentation status table shows each service name, port, Swagger URL (if available), and documentation completeness percentage.

2. **Given** a microservice with Swagger enabled, **When** the Swagger URL is accessed, **Then** it displays all available endpoints with request/response schemas.

3. **Given** a microservice without Swagger enabled, **When** the analysis runs, **Then** the service is flagged as "SWAGGER MISSING" with a recommendation to add OpenAPI annotations.

---

### User Story 4 - Developer Reviews Rule Engine & Tax Engine Integration (Priority: P2)

As a developer, I want to understand how the rule engine integrates (or fails to integrate) with the tax engine, so that I can fix the documented database disconnect issue and ensure dynamic rules actually affect tax calculations.

**Why this priority**: The rule engine is documented as being disconnected from the main database, meaning rules cannot actually be loaded dynamically. This is a critical architectural gap that undermines the entire configurable rules feature.

**Independent Test**: Can be fully tested by reviewing the integration diagram and database connection configurations to verify the documented disconnect.

**Acceptance Scenarios**:

1. **Given** the rule-service and tax-engine-service, **When** the analysis examines their configurations, **Then** a clear diagram shows data flow between services, identifying any database connection mismatches.

2. **Given** the documented rule engine disconnect (rule-service database configuration differs from tax-engine-service), **When** the analysis runs, **Then** this integration failure is prominently highlighted with specific remediation steps.

3. **Given** rule categories (TAX_RATES, INCOME_INCLUSION, DEDUCTIONS, PENALTIES, FILING, ALLOCATION, WITHHOLDING, VALIDATION), **When** the analysis runs, **Then** a report shows which rules are actually loaded dynamically vs. hardcoded in Java source code.

---

### User Story 5 - QA Lead Reviews Sequence Flow Completeness (Priority: P3)

As a QA lead, I want detailed sequence diagrams for each major system flow with clear annotations showing which steps are implemented vs. planned, so that I can create comprehensive test plans.

**Why this priority**: Sequence diagrams enable systematic testing and help QA understand the expected behavior of each workflow.

**Independent Test**: Can be fully tested by comparing the documented sequence diagrams against actual system behavior through manual testing.

**Acceptance Scenarios**:

1. **Given** the authentication flow (login → JWT issuance → token validation → session management), **When** the analysis runs, **Then** a sequence diagram shows all steps with implementation status.

2. **Given** the document extraction flow (upload → Gemini AI processing → streaming response → form data parsing), **When** the analysis runs, **Then** the diagram shows implemented capabilities and missing features (e.g., visual provenance, bounding boxes).

3. **Given** the payment processing flow (calculate balance → collect payment → update ledger → generate receipt), **When** the analysis runs, **Then** the diagram clearly shows which steps exist vs. are unimplemented (per current documentation status).

---

### User Story 6 - Security Officer Reviews Data Flow for Sensitive Information (Priority: P3)

As a security officer, I want to understand how sensitive data (SSN, EIN, bank account numbers) flows through the system, so that I can ensure proper handling and identify potential security gaps.

**Why this priority**: Proper handling of PII is a compliance requirement. Understanding data flows helps identify where sensitive data may be exposed.

**Independent Test**: Can be fully tested by tracing SSN/EIN fields through the documented data flows and verifying appropriate handling at each step.

**Acceptance Scenarios**:

1. **Given** taxpayer profile data containing SSN, **When** the analysis traces data flow, **Then** a report shows every service/component that handles this data and how it's protected (encrypted, masked, etc.).

2. **Given** the extraction service receiving document images containing SSN, **When** the analysis runs, **Then** the report shows how extracted PII is handled from extraction through storage.

3. **Given** UI components displaying sensitive data, **When** the analysis runs, **Then** the report identifies which components show masked vs. unmasked sensitive fields.

---

### Edge Cases

- What happens when an API endpoint exists but has no frontend consumer? Report as UNUSED.
- What happens when a UI component attempts to call a non-existent API? Report as API MISSING.
- What happens when a user journey step has partial implementation? Report as PARTIAL with details.
- What happens when documentation is outdated? Flag discrepancies between documentation and actual implementation.
- What happens when a service is unreachable during analysis? Note connectivity status and proceed with available data.

## Requirements *(mandatory)*

### Functional Requirements

#### API Coverage Analysis
- **FR-001**: The analysis MUST enumerate all REST API endpoints from each of the 9 backend microservices.
- **FR-002**: The analysis MUST map each API endpoint to its consuming frontend component(s) from the React application.
- **FR-003**: The analysis MUST identify API endpoints with no frontend consumers and mark them as "UNUSED."
- **FR-004**: The analysis MUST identify frontend components with missing backend API support and mark them as "API MISSING."

#### User Journey Mapping
- **FR-005**: The analysis MUST document the complete taxpayer individual filing journey with step-by-step implementation status.
- **FR-006**: The analysis MUST document the complete business net profits filing journey with step-by-step implementation status.
- **FR-007**: The analysis MUST document the auditor review workflow journey with step-by-step implementation status.
- **FR-008**: The analysis MUST document the administrator configuration journey with step-by-step implementation status.
- **FR-009**: The analysis MUST validate that user journeys respect role-based logical constraints (e.g., auditor login flows should not request SSN or location data that is irrelevant to auditor role).

#### Swagger Documentation
- **FR-010**: The analysis MUST provide Swagger/OpenAPI links for each backend service that has documentation enabled.
- **FR-011**: The analysis MUST flag services without Swagger documentation and provide specific file/configuration references for adding it.
- **FR-012**: The analysis MUST assess documentation completeness (percentage of endpoints documented).

#### Rule Engine & Tax Engine Analysis
- **FR-013**: The analysis MUST document the current state of rule engine integration with tax calculations.
- **FR-014**: The analysis MUST identify all rules that are hardcoded vs. dynamically configurable.
- **FR-015**: The analysis MUST document the database disconnect issue between rule-service and tax-engine-service with remediation steps.
- **FR-016**: The analysis MUST enumerate all rule categories and their usage status.

#### Sequence Flow & Data Flow
- **FR-017**: The analysis MUST provide sequence diagrams for each major system workflow with implementation status annotations.
- **FR-018**: The analysis MUST document data flows showing how information moves between services.
- **FR-019**: The analysis MUST identify data flow gaps where expected integrations are missing.

#### UI Component Analysis
- **FR-020**: The analysis MUST catalog all UI components (approximately 95+ planned, subset implemented).
- **FR-021**: The analysis MUST identify UI components that exist but have incomplete functionality.
- **FR-022**: The analysis MUST highlight missing UI components that are required for complete user journeys.

#### Gap Identification
- **FR-023**: The analysis MUST produce a prioritized list of gaps organized by severity (CRITICAL, HIGH, MEDIUM, LOW).
- **FR-024**: The analysis MUST provide specific remediation recommendations for each identified gap.
- **FR-025**: The analysis MUST cross-reference gaps with existing specs (1-12) to avoid duplicate recommendations.

### Key Entities

- **API Endpoint**: Represents a single REST API path with HTTP method, request/response schemas, and consuming components. Linked to a Service.
- **Service**: Represents a backend microservice (9 total) with port, database connections, and Swagger status. Contains multiple API Endpoints.
- **UI Component**: Represents a React component with its API dependencies, implementation status, and user journey associations.
- **User Journey**: Represents an end-to-end user workflow (e.g., "Individual Tax Filing") with ordered steps, each having a completion status.
- **Gap**: Represents an identified deficiency with severity, category (API, UI, Integration, Documentation), and remediation recommendation.
- **Rule Configuration**: Represents a tax rule with its category, value type, current implementation status (hardcoded vs. dynamic), and effective dates.

## Assumptions

- The analysis will be based on static code review and documentation; runtime testing may identify additional gaps.
- Swagger documentation status is determined by presence of OpenAPI configuration files.
- API endpoint discovery is based on controller class scanning in backend services.
- UI component inventory is based on file system enumeration of React components.
- User journey completeness is assessed against existing documentation (CURRENT_FEATURES.md, Gaps.md, sequence diagrams).
- The rule engine database disconnect issue documented in RULE_ENGINE_DISCONNECT_ANALYSIS.md is accurate.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All backend API endpoints across all 9 services are cataloged with consumer mapping status.
- **SC-002**: All UI components are cataloged with implementation and linkage status.
- **SC-003**: All 4 primary user journeys (individual filing, business filing, auditor workflow, admin configuration) are documented with step-by-step completion status.
- **SC-004**: Each of the 9 microservices has a documented Swagger status (AVAILABLE with link, or MISSING with specific gap).
- **SC-005**: The rule engine integration analysis identifies all disconnects between intended architecture and current implementation.
- **SC-006**: The gap list is prioritized with at least CRITICAL, HIGH, and MEDIUM severity levels.
- **SC-007**: Each identified gap includes actionable remediation steps.
- **SC-008**: The analysis report can be used by development teams to create sprint backlogs within 30 minutes of review.
- **SC-009**: Zero false positives in UNUSED API or MISSING API categorizations when spot-checked against 10 random endpoints.
- **SC-010**: The analysis identifies role-specific logical inconsistencies (e.g., auditor flows requesting irrelevant data) for at least 3 distinct user roles.
