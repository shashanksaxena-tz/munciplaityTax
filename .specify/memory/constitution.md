<!--
Sync Impact Report - Constitution v1.0.0

VERSION CHANGE: Initial → 1.0.0
- First formal constitution establishing governance framework

PRINCIPLES DEFINED:
1. Microservices Architecture First
2. Multi-Tenant Data Isolation
3. Audit Trail Immutability
4. AI Transparency & Explainability
5. Security & Compliance First
6. User-Centric Design
7. Test Coverage & Quality Gates

TEMPLATES STATUS:
✅ plan-template.md - Aligned with microservices architecture
✅ spec-template.md - Aligned with user story requirements
✅ tasks-template.md - Aligned with independent story implementation
⏳ FOLLOW-UP: Review commands/*.md for consistency after creation

DEFERRED ITEMS: None
-->

# MuniTax Constitution

## Core Principles

### I. Microservices Architecture First

**Service Decomposition**: Every feature MUST be evaluated for appropriate service placement. New capabilities MUST be added to existing services if they share domain concerns, or as new services only when justified by domain boundaries, scale requirements, or team autonomy needs.

**Service Boundaries**: Each microservice MUST own its data, expose well-defined APIs, and be independently deployable. Services MUST communicate asynchronously where possible to maintain loose coupling.

**Current Services**: discovery-service (Eureka), gateway-service (routing), auth-service (JWT), tenant-service (multi-tenancy), tax-engine-service (calculations), extraction-service (AI), submission-service (workflows), pdf-service (generation).

**Rationale**: Municipal tax systems require independent scaling of CPU-intensive calculations (tax engine), I/O-bound operations (AI extraction), and high-throughput traffic (gateway). Service boundaries enable specialized optimization and fault isolation critical for tax filing deadlines.

### II. Multi-Tenant Data Isolation (NON-NEGOTIABLE)

**Schema-per-Tenant**: Each municipality (Dublin, Columbus, Westerville, etc.) MUST have isolated database schemas in PostgreSQL. No cross-tenant data access MUST be possible at the application layer.

**Tenant Context**: Every API request MUST carry tenant identification (JWT tenant claim or subdomain). All database queries MUST be scoped to the active tenant's schema.

**Rule Isolation**: Tax rules, rates, forms, and configuration MUST be tenant-specific. Dublin's 2.25% rate MUST NOT affect Columbus's 2.5% rate.

**Rationale**: Legal compliance mandates absolute data segregation between municipalities. A data breach or bug affecting one city must not compromise another's taxpayer data. This principle is non-negotiable for production deployment.

### III. Audit Trail Immutability

**Append-Only Logs**: All user actions, system events, approval/rejection decisions, and data modifications MUST be recorded in immutable audit logs with timestamps, actor identification, and before/after states.

**No Deletion**: Audit records MUST NEVER be deleted or modified. Soft deletes with audit trails MUST be used for all business entities.

**Retention**: Audit logs MUST be retained for 7 years minimum to comply with IRS retention requirements (IRC § 6001).

**Chain of Custody**: Every tax return modification MUST maintain a complete version history showing who changed what, when, and why (rejection reason, amendment justification, auditor override).

**Rationale**: Municipal tax systems face regular audits by state tax authorities. Inability to prove data integrity or explain historical decisions creates legal liability. Immutable audit trails protect both taxpayers and municipalities in disputes.

### IV. AI Transparency & Explainability

**Visual Provenance**: Every AI-extracted field MUST store bounding box coordinates linking to the source document location. Users MUST be able to click any form field and view the exact PDF page/location it was extracted from.

**Confidence Scoring**: AI extractions MUST include per-field confidence scores (0-100%). Fields below configurable thresholds MUST be flagged for human review.

**Ignored Items Report**: After extraction, users MUST see a comprehensive report listing all uploaded pages/files that were NOT used in extraction, with reasons (irrelevant document, low quality, unsupported form type).

**Human Override**: All AI-extracted values MUST be editable by users and auditors. Overrides MUST be logged in audit trail with justification.

**Rationale**: Tax liability decisions based on "black box" AI are legally risky and erode user trust. Provenance enables taxpayers to verify calculations and auditors to validate compliance. Confidence scores prioritize human attention to high-risk extractions.

### V. Security & Compliance First

**Authentication**: JWT-based authentication MUST be enforced on all non-public endpoints. Passwords MUST be hashed with bcrypt (minimum cost factor 12).

**Authorization**: Role-based access control (RBAC) MUST enforce least-privilege access. INDIVIDUAL filers MUST NOT access AUDITOR workflows. AUDITOR role MUST NOT access other tenants' data.

**Data Protection**: Sensitive fields (SSN, EIN, bank accounts) MUST be encrypted at rest using AES-256. Logs MUST NOT contain SSN/EIN in plaintext.

**Secure Communication**: All production traffic MUST use TLS 1.3+. Internal service-to-service communication MUST use mTLS or service mesh security.

**Compliance Standards**: System MUST comply with IRS Publication 1075 (safeguarding federal tax information) and Ohio R.C. 718 (municipal income tax confidentiality).

**Rationale**: Municipal tax systems handle highly sensitive PII (SSN, income, addresses). A data breach creates massive legal liability, destroys public trust, and may result in IRS decertification. Security must be foundational, not bolted on later.

### VI. User-Centric Design

**Progressive Disclosure**: Complex tax concepts MUST be hidden behind simple interfaces with tooltips, wizards, and contextual help. Expert mode MUST be available but not default.

**Error Prevention**: Real-time validation MUST catch common errors (mismatched totals, missing required forms, duplicate entries) before submission, not after rejection.

**Transparency**: Users MUST always know their return status (Draft, Submitted, Under Review, Approved, Rejected), expected processing time, and next steps.

**Accessibility**: UI MUST meet WCAG 2.1 AA standards. All interactive elements MUST be keyboard-navigable and screen-reader compatible.

**Mobile-First**: Core filing workflows (document upload, review, submission) MUST function on mobile devices (iOS/Android, 375px+ width).

**Rationale**: Municipal tax software often competes with predatory "tax prep services" charging excessive fees. User-friendly design directly serves the public interest by reducing barriers to self-filing, decreasing support burden, and increasing voluntary compliance.

### VII. Test Coverage & Quality Gates

**Unit Tests**: All business logic (tax calculations, rule engines, allocation formulas) MUST have unit test coverage ≥80%. Critical path functions (tax liability computation) MUST have 100% coverage.

**Integration Tests**: All microservice API contracts MUST have integration tests verifying request/response schemas, error handling, and cross-service workflows.

**Contract Tests**: Services exposing APIs to other services or frontend MUST maintain contract tests (e.g., Pact) to prevent breaking changes.

**End-to-End Tests**: Critical user journeys (individual filing, business filing, auditor approval) MUST have automated E2E tests running in CI/CD.

**Pre-Deployment Gates**: No deployment to production MUST occur without passing all tests, security scans (OWASP dependency check), and code review approval.

**Rationale**: Tax miscalculations destroy user trust and create financial liability. A single error in NOL carryforward logic or withholding reconciliation can affect thousands of taxpayers. High test coverage is not perfectionism—it's fiduciary duty.

## Architecture Constraints

### Technology Stack

**Backend**: Spring Boot 3.2.3, Java 21 (LTS until September 2028). Microservices MUST use Spring Cloud for service discovery, circuit breaking, and distributed tracing.

**Frontend**: React 18+ with TypeScript, Vite build tool, Tailwind CSS. State management via React Context + useReducer (upgrade to Redux only if complexity justifies).

**Database**: PostgreSQL 16+ for transactional data (multi-tenant schemas), Redis 7+ for session caching and rate limiting.

**Message Queue**: RabbitMQ or Kafka MUST be used for asynchronous communication (e.g., extraction completion notifications, batch report generation).

**Observability**: Distributed tracing via Zipkin or Jaeger. Structured JSON logging to centralized log aggregator (ELK stack or CloudWatch).

**Rationale**: Stack choices prioritize long-term support, enterprise stability, and ecosystem maturity over bleeding-edge trends. Municipal software must operate reliably for 5-10 year lifecycles.

### Performance Standards

**Response Time**: API endpoints MUST respond within 200ms p95 for CRUD operations, 2 seconds p95 for tax calculations, 30 seconds for AI extraction (per 10-page document).

**Throughput**: System MUST handle 1,000 concurrent users during tax filing season (January-April) without degradation.

**Availability**: Production system MUST maintain 99.5% uptime (43 hours downtime/year allowed). Planned maintenance windows MUST avoid tax deadlines.

**Data Integrity**: Database transactions MUST use ACID guarantees. Financial calculations MUST be deterministic and reproducible.

**Rationale**: Tax deadline days (April 15, quarterly estimated dates) create 10x-100x traffic spikes. System failure on deadline day creates public relations disasters and legal liability for missed filings.

### Scalability Requirements

**Horizontal Scaling**: All microservices MUST be stateless and horizontally scalable via Kubernetes or Docker Swarm.

**Database Scaling**: PostgreSQL MUST support read replicas for reporting queries. Write-heavy services (audit logs) MUST use connection pooling.

**File Storage**: Document uploads MUST use object storage (S3, Azure Blob, MinIO) with CDN caching for generated PDFs.

**Rationale**: Initial deployment (Dublin, 50k population) may handle 5k annual returns. Future expansion to Columbus (900k population) requires 100x scale without architectural rework.

## Development Workflow

### Feature Development Process

1. **Specification**: All features MUST start with a spec in `/specs/###-feature-name/spec.md` defining user stories, acceptance criteria, and success metrics.

2. **Planning**: Use `/speckit.plan` to generate `plan.md` with technical approach, constitution compliance check, and project structure.

3. **Task Breakdown**: Use `/speckit.tasks` to generate `tasks.md` organizing work by user story for independent, testable increments.

4. **Test-First**: Write integration/contract tests for new APIs BEFORE implementation. Verify tests fail.

5. **Implementation**: Implement features in priority order (P1 → P2 → P3), committing after each logical task.

6. **Review**: All code MUST pass peer review verifying constitution compliance, test coverage, and security best practices.

7. **Deployment**: Use CI/CD pipeline with automated tests, security scans, and blue-green deployment to minimize downtime.

### Code Review Requirements

**Constitution Check**: Reviewer MUST verify no principles are violated. Complexity deviations MUST be explicitly justified in plan.md.

**Test Coverage**: New code MUST include tests. Reviewers MUST verify tests actually fail before implementation (prevent false positives).

**Security Review**: Any code touching authentication, authorization, encryption, or sensitive data MUST have security-focused review.

**Documentation**: Public APIs, complex algorithms, and non-obvious business rules MUST have inline documentation.

**Breaking Changes**: API contract changes MUST be flagged, versioned, and communicated to consumers (other services, frontend).

### Branching Strategy

**Main Branch**: Always production-ready. Protected, requires PR approval + passing tests.

**Feature Branches**: Named `###-feature-name` matching spec directory. Short-lived (max 2 weeks).

**Release Branches**: `release/vX.Y.Z` for final testing before production deployment.

**Hotfix Branches**: `hotfix/###-description` for critical production fixes, merged to main and release.

## Governance

### Amendment Process

1. **Proposal**: Any team member may propose constitution amendments via pull request to `.specify/memory/constitution.md`.

2. **Justification**: Amendment MUST include rationale explaining why current principle is inadequate or incorrect.

3. **Impact Assessment**: Amendment MUST document affected templates, existing code, and migration plan.

4. **Approval**: Amendments require majority approval from core team (minimum 3 approvals).

5. **Versioning**: Constitution follows semantic versioning:
   - **MAJOR**: Principle removed or redefined (backward incompatible)
   - **MINOR**: New principle added or section expanded
   - **PATCH**: Clarifications, typos, wording improvements

6. **Migration**: After approval, proposer MUST update affected templates and create migration tasks for existing code.

### Compliance Enforcement

**Pre-Review Check**: Feature specs MUST include "Constitution Check" section listing principles that apply and how they're satisfied.

**Review Gate**: PRs violating constitution without explicit justification in plan.md MUST be rejected.

**Retrospectives**: Monthly reviews MUST assess constitution compliance and identify patterns requiring clarification or amendment.

**Technical Debt**: Temporary violations (e.g., missing tests during prototype) MUST be tracked as technical debt tickets with resolution deadlines.

### Exception Handling

**Temporary Exceptions**: May be granted for prototypes, spikes, or time-critical hotfixes. MUST include:
- Justification (why violation is necessary)
- Scope (exactly what's exempted)
- Remediation plan (how to resolve before production)
- Expiration date (when exception expires)

**Permanent Exceptions**: Require formal amendment process. "It's too hard" is not justification—principle should be changed if consistently impractical.

### Living Document

This constitution is a living document reflecting the project's current understanding of best practices. As the project matures and requirements evolve, amendments are expected and encouraged. The goal is not rigid dogma but shared principles enabling sustainable, high-quality development.

**Version**: 1.0.0 | **Ratified**: 2025-11-27 | **Last Amended**: 2025-11-27
