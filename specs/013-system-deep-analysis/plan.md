# Implementation Plan: System Deep Analysis & Gap Identification

**Branch**: `013-system-deep-analysis` | **Date**: 2024-12-02 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/013-system-deep-analysis/spec.md`

## Summary

Implement comprehensive system analysis tooling and documentation that maps all backend API endpoints to frontend consumers, documents user journey completeness across 4 primary workflows, identifies rule engine and tax engine integration issues, provides Swagger documentation status per microservice, and produces a prioritized gap list with remediation recommendations.

**Primary Requirement**: Generate a complete API coverage report mapping 50+ backend endpoints across 9 microservices to their React frontend consumers, identifying unused APIs and missing backend support for UI components.

**Technical Approach**: Create analysis scripts that scan Java controller classes for REST endpoints, scan React components for API calls, cross-reference to produce coverage matrix. Document user journeys with step-by-step implementation status. Generate Swagger links table. Analyze rule-service database configuration. Produce prioritized gap report in markdown format.

---

## Technical Context

**Language/Version**: 
- **Analysis Scripts**: Python 3.11+ or Node.js 20.x (for static analysis tooling)
- **Documentation Output**: Markdown files with mermaid diagrams
- **Target System**: Java 21/Spring Boot 3.2.3 (backend), TypeScript 5.x/React 18.2 (frontend)

**Primary Dependencies**:
- **Analysis**: AST parsers (TypeScript compiler API, JavaParser or regex-based scanning)
- **Documentation**: Mermaid for diagrams, Markdown for reports
- **Cross-referencing**: JSON/YAML for intermediate data exchange

**Storage**: 
- Output files stored in `specs/013-system-deep-analysis/analysis/` directory
- No database required for this analysis feature

**Testing**:
- Manual validation against spot-checked endpoints (SC-009)
- Review by stakeholders for accuracy (SC-008: 30-minute sprint backlog creation)

**Target Platform**: 
- Analysis runs on developer machines (macOS, Linux, Windows)
- Output readable in any markdown viewer (GitHub, VSCode, etc.)

**Project Type**: Documentation/Analysis feature (no runtime code deployment)

**Performance Goals**:
- Full system analysis completes in <10 minutes
- Report generation: <1 minute per service

**Constraints**:
- Static analysis only (no runtime dependencies)
- Must not modify existing source code
- Must produce human-readable output

**Scale/Scope**:
- 9 backend microservices to analyze
- ~95+ planned UI components to catalog
- 4 primary user journeys to document
- ~50+ API endpoints to map

---

## Constitution Check

*GATE: This is a documentation/analysis feature. Constitution checks apply to the analysis methodology, not code implementation.*

### ✅ I. Microservices Architecture First

**Evaluation**: Analysis feature does not introduce new microservices. Documents existing 9-service architecture.

**No Violations**: Feature is documentation only.

---

### ✅ II. Multi-Tenant Data Isolation

**Evaluation**: Analysis examines code structure, not runtime data. No tenant data access.

**No Violations**: Feature does not access tenant data.

---

### ✅ III. Audit Trail Immutability

**Evaluation**: Analysis reports are static documentation. No audit trail requirements.

**No Violations**: Feature is documentation only.

---

### ✅ IV. AI Transparency & Explainability

**Evaluation**: No AI components in this analysis feature.

**No Violations**: Feature does not use AI.

---

### ✅ V. Security & Compliance First

**Evaluation**: Analysis may document sensitive data flows (SSN, EIN). Reports must not contain actual PII.

**Implementation**:
- Document field names and flow paths, not actual data values
- Highlight sensitive data handling in data flow diagrams

**No Violations**: Feature produces documentation only.

---

### ✅ VI. User-Centric Design

**Evaluation**: Analysis reports target multiple stakeholders (technical lead, product owner, architect, developer, QA lead, security officer).

**Implementation**:
- Executive summary for product owners
- Detailed technical sections for developers
- Prioritized gap list for sprint planning
- Data flow diagrams for security review

**No Violations**: Feature provides stakeholder-appropriate views.

---

## Project Structure

### Documentation (this feature)

```text
specs/013-system-deep-analysis/
├── spec.md              # Feature specification
├── plan.md              # This file
├── research.md          # Analysis methodology research
├── data-model.md        # Entity definitions for analysis output
├── quickstart.md        # How to run the analysis
├── tasks.md             # Implementation tasks
├── contracts/           # Output format schemas
│   ├── api-coverage-schema.md
│   ├── user-journey-schema.md
│   └── gap-report-schema.md
└── analysis/            # Generated analysis reports
    ├── api-coverage-report.md
    ├── user-journey-report.md
    ├── swagger-status.md
    ├── rule-engine-analysis.md
    ├── ui-component-inventory.md
    ├── sequence-diagrams.md
    ├── data-flow-diagrams.md
    └── gap-report.md
```

### Analysis Scripts (temporary, not committed)

```text
/tmp/analysis-scripts/
├── scan-backend-apis.py    # Scans Java controllers for REST endpoints
├── scan-frontend-apis.py   # Scans React components for API calls
├── cross-reference.py      # Matches backend ↔ frontend
├── swagger-checker.py      # Validates Swagger availability
└── gap-analyzer.py         # Produces prioritized gap list
```

**Structure Decision**: Documentation-only feature. All output in `specs/013-system-deep-analysis/analysis/` directory. Analysis scripts are temporary and not committed.

---

## Complexity Tracking

> No violations requiring justification. This is a documentation feature.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

---

## Analysis Methodology

### Phase 0: Research (Covered in research.md)

1. Identify all backend controller classes across 9 microservices
2. Identify all React components making API calls
3. Review existing documentation for user journeys
4. Examine rule-service and tax-engine-service database configurations

### Phase 1: Data Collection

1. **Backend API Scan**: Parse Java controller files, extract `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc.
2. **Frontend API Scan**: Parse TypeScript/React files, extract `fetch()`, `axios`, and `api.` calls
3. **Swagger Status**: Check for `/swagger-ui.html` or `/v3/api-docs` availability per service
4. **Rule Engine Config**: Review `application.yml` and database connection settings

### Phase 2: Cross-Reference & Analysis

1. Match backend endpoints to frontend consumers
2. Identify unused endpoints (UNUSED status)
3. Identify frontend calls without backend (API MISSING status)
4. Validate user journey steps against implementation status
5. Categorize gaps by severity (CRITICAL, HIGH, MEDIUM, LOW)

### Phase 3: Report Generation

1. Generate API Coverage Report (markdown table)
2. Generate User Journey Report (mermaid flowcharts with status)
3. Generate Swagger Status Table
4. Generate Rule Engine Integration Analysis
5. Generate UI Component Inventory
6. Generate Sequence Diagrams with implementation status
7. Generate Data Flow Diagrams for sensitive data
8. Generate Prioritized Gap Report

---

## Services to Analyze

| Service | Port | Directory | Expected Endpoints |
|---------|------|-----------|-------------------|
| gateway-service | 8080 | backend/gateway-service/ | Routes, no direct APIs |
| auth-service | 8081 | backend/auth-service/ | Login, register, validate |
| tenant-service | 8082 | backend/tenant-service/ | Sessions, address validation |
| extraction-service | 8083 | backend/extraction-service/ | Document extraction |
| submission-service | 8084 | backend/submission-service/ | Tax submissions |
| tax-engine-service | 8085 | backend/tax-engine-service/ | Tax calculations |
| pdf-service | 8086 | backend/pdf-service/ | PDF generation |
| rule-service | 8087 | backend/rule-service/ | Dynamic rules |
| ledger-service | 8088 | backend/ledger-service/ | Payments, ledger |

---

## User Journeys to Document

1. **Individual Tax Filing Journey**: Document upload → AI extraction → Data review → Tax calculation → Submission → Payment
2. **Business Net Profits Filing Journey**: Federal data entry → Schedule X reconciliation → Schedule Y allocation → Tax calculation → Submission
3. **Auditor Review Workflow Journey**: View queue → Assign case → Review return → Approve/Reject → Document request → E-signature
4. **Administrator Configuration Journey**: Login → Configure rules → Manage tenants → View reports

---

## Gap Severity Definitions

| Severity | Definition | Example |
|----------|------------|---------|
| CRITICAL | Blocks primary user journeys | Payment integration missing (0% complete) |
| HIGH | Significant feature incomplete | Auditor workflow 0% implemented |
| MEDIUM | Feature exists but with gaps | Schedule X has 6 fields, needs 25+ |
| LOW | Documentation or polish issues | Swagger documentation missing |

---

## Output Deliverables

1. **api-coverage-report.md**: Full mapping of backend endpoints to frontend consumers
2. **user-journey-report.md**: Step-by-step journey documentation with status
3. **swagger-status.md**: Table of Swagger availability per service
4. **rule-engine-analysis.md**: Integration status and database disconnect details
5. **ui-component-inventory.md**: Catalog of all React components with status
6. **sequence-diagrams.md**: Annotated diagrams for each major flow
7. **data-flow-diagrams.md**: Sensitive data flow documentation
8. **gap-report.md**: Prioritized gap list with remediation steps
