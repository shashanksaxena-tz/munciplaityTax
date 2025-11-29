# Implementation Plan: Dynamic Rule Configuration System

**Branch**: `4-rule-configuration-ui` | **Date**: 2025-11-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/4-rule-configuration-ui/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

This feature replaces hardcoded tax rules in constants.ts and Java calculators with a dynamic rule configuration system stored in PostgreSQL. Tax administrators can configure rules (municipal rates, NOL caps, W-2 qualifying wages logic, entity-specific deductions) via a React admin UI with temporal effective dating, multi-tenant support, version control, and formula-based conditional rules. The system enables zero-code-deployment rule updates, reduces compliance risk through audit trails, and supports complex scenarios like mid-year rate changes and entity-specific overrides.

**Primary Technical Approach**: Create new `rule-service` microservice managing TaxRule entities with PostgreSQL jsonb storage. Refactor tax-engine-service calculators to query rules via REST API with Redis caching. Build React RuleConfigurationDashboard with temporal rule editor, what-if analysis tool, and version history viewer. Implement approval workflow with TAX_ADMINISTRATOR role enforcement.

## Technical Context

**Language/Version**: 
- Backend: Java 21 (Spring Boot 3.2.3)
- Frontend: TypeScript 5.x (React 18)

**Primary Dependencies**: 
- Backend: Spring Cloud (Eureka, Gateway), Spring Data JPA, PostgreSQL Driver, Redis Client (Lettuce), Jackson for JSON
- Frontend: React 18, TypeScript, Vite, Tailwind CSS, React Hook Form, date-fns

**Storage**: 
- PostgreSQL 16+ (multi-tenant schemas, jsonb column for rule.value field)
- Redis 7+ (rule caching layer, TTL-based invalidation)

**Testing**: 
- Backend: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), MockMvc
- Frontend: Jest, React Testing Library, MSW (Mock Service Worker)

**Target Platform**: 
- Docker containers on Linux (microservices architecture)
- Web browsers (Chrome 100+, Firefox 100+, Safari 15+, Edge 100+)

**Project Type**: Web application (microservices backend + React SPA frontend)

**Performance Goals**: 
- Rule retrieval: <100ms per tax calculation (with Redis cache hit)
- Rule save/update: <500ms database write + cache invalidation
- What-if analysis: <5 seconds for 100 sample returns
- Temporal query (point-in-time): <200ms for historical rule retrieval

**Constraints**: 
- Must maintain ACID transactions for rule changes (prevent concurrent conflicting updates)
- Cannot break backward compatibility with existing tax calculations during migration
- Must support 10+ tenants with independent rule sets in single database
- Audit trail must be immutable (no UPDATE/DELETE on rule_change_log table)

**Scale/Scope**: 
- Expected rules per tenant: 50-100 active rules
- Expected rule changes per year per tenant: 8-16 (2-4 per quarter)
- Expected tenants: 10-20 municipalities initially, 100+ long-term
- Expected concurrent admin users: 5-10 (not high-traffic admin tool)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Microservices Architecture First ✅ COMPLIANT

**Service Placement**: Creating new `rule-service` microservice is justified by:
- **Domain Boundary**: Rule management is distinct from tax calculation (tax-engine-service) and submission workflows
- **Independent Scaling**: Admin configuration tool has different traffic patterns than taxpayer-facing services
- **Data Ownership**: TaxRule and RuleChangeLog entities form cohesive bounded context
- **Team Autonomy**: Tax policy experts can iterate on rule UI without affecting calculation engine

**Service Communication**: 
- rule-service exposes REST API: `GET /api/rules/active?tenantId={id}&taxYear={year}`
- tax-engine-service consumes via synchronous HTTP (acceptable for low-volume admin operations)
- Redis cache reduces inter-service calls to database for rule retrieval

**Alternative Considered**: Add rule management to tax-engine-service directly
**Rejected Because**: Tax engine should remain focused on calculation logic. Rule configuration is administrative concern with different access patterns, security requirements (admin-only vs taxpayer-facing), and lifecycle (frequent changes vs stable calculation algorithms).

### II. Multi-Tenant Data Isolation ✅ COMPLIANT

**Schema-per-Tenant**: Each municipality's TaxRule records include `tenant_id` column with database-level row-level security (RLS) policies enforcing isolation. All queries filter by tenant context from JWT.

**Tenant Context Enforcement**: 
- JWT contains `tenantId` claim (set by auth-service during login)
- rule-service validates tenant_id in request matches JWT claim
- PostgreSQL RLS policy: `CREATE POLICY tenant_isolation ON tax_rules USING (tenant_id = current_setting('app.tenant_id'))`

**Rule Isolation**: Dublin's rules (tenant_id='dublin') never visible to Columbus (tenant_id='columbus'). API enforces tenant filtering at application layer + database RLS as defense-in-depth.

### III. Audit Trail Immutability ✅ COMPLIANT

**Append-Only Logs**: 
- `rule_change_log` table has no UPDATE/DELETE triggers
- Every rule modification creates new TaxRule record with incremented `version` field
- Previous version linked via `previous_version_id` (immutable chain)
- RuleChangeLog records track: old_value, new_value, changed_by, change_date, change_reason

**Soft Deletes**: TaxRule "deletion" sets `approval_status='VOIDED'` and `end_date=NOW()`, preserving record

**Retention**: Audit logs retained for 7 years minimum (application enforces retention policy, no database-level auto-purge)

**Chain of Custody**: Every rule change shows: who made change (changed_by user_id), when (change_date timestamp), why (change_reason text), what changed (old_value → new_value diff)

### IV. AI Transparency & Explainability ✅ NOT APPLICABLE

This feature does not involve AI/ML. Rule configuration is manual administrative process. No AI extraction, confidence scoring, or provenance tracking needed.

### V. Security & Compliance First ✅ COMPLIANT

**Authentication**: All rule configuration endpoints require JWT with valid user session. No anonymous access to rule management.

**Authorization**: New role `TAX_ADMINISTRATOR` enforced via Spring Security:
```java
@PreAuthorize("hasRole('TAX_ADMINISTRATOR')")
public class RuleConfigController { ... }
```
Only TAX_ADMINISTRATOR role can create/update/approve rules. Regular INDIVIDUAL and AUDITOR roles have read-only access to view active rules (for transparency).

**Data Protection**: 
- Rule values may contain sensitive policy data but not PII (SSN/EIN)
- Standard TLS 1.3 for API communication
- No encryption at rest needed for rule configuration (not taxpayer data)

**Compliance**: Rule approval workflow + immutable audit trail satisfy IRS Publication 1075 requirements for demonstrating data integrity.

### VI. User-Centric Design ✅ COMPLIANT

**Progressive Disclosure**: 
- Simple rules (municipal rate percentage) use basic number input
- Complex rules (formulas, conditionals) behind "Advanced" accordion section
- Tooltips explain tax concepts (e.g., "NOL carryforward" → "Net Operating Loss from previous years")

**Error Prevention**: 
- Real-time validation: overlapping date ranges blocked before save
- Formula syntax checking: parser validates before saving rule
- Conflict detection: system prevents contradictory rules (e.g., two active rates for same tenant+date)

**Transparency**: 
- Rule changes show "Effective Date" and "Status" (Pending/Approved/Active)
- What-If Analysis preview shows impact on sample returns before publishing
- Version history timeline shows all changes with diff view

**Accessibility**: 
- Form inputs keyboard-navigable with proper ARIA labels
- Date picker supports manual entry + keyboard navigation
- Color contrast meets WCAG 2.1 AA (4.5:1 for text)

**Mobile Support**: Admin UI is responsive (works on tablet 768px+) but not optimized for phone (admin tool, desktop-primary acceptable)

### VII. Test Coverage & Quality Gates ✅ COMPLIANT

**Unit Tests**: 
- All rule validation logic (overlap detection, formula parsing, conflict checking): 100% coverage
- Tax calculator rule query logic: 100% coverage
- Business logic (temporal rule selection algorithm): ≥80% coverage

**Integration Tests**: 
- RuleService REST API contract tests: request/response validation, error cases
- Tax engine integration: verify calculators correctly load rules from new service
- Cache invalidation tests: verify Redis cache clears when rule published

**Contract Tests**: 
- Pact tests between rule-service (provider) and tax-engine-service (consumer)
- Ensures /api/rules/active endpoint maintains backward compatibility

**End-to-End Tests**: 
- Critical path: Admin creates rule → Approves → Calculator uses new rule in tax calculation
- Regression: Existing tax calculations produce same results after rule migration

**Pre-Deployment Gates**: 
- All tests pass in CI/CD pipeline
- OWASP dependency check (no high/critical vulnerabilities)
- Code review approval from senior engineer

## Project Structure

### Documentation (this feature)

```text
specs/4-rule-configuration-ui/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── rule-service-api.yaml      # OpenAPI spec for rule-service REST endpoints
│   └── rule-cache-contract.yaml   # Redis cache key structure and TTL policies
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Backend: New microservice
backend/rule-service/
├── src/main/java/com/munitax/rules/
│   ├── RuleServiceApplication.java
│   ├── controller/
│   │   ├── RuleConfigController.java        # REST API for CRUD operations
│   │   ├── RuleQueryController.java         # Read-only API for active rules
│   │   └── RuleHistoryController.java       # Version history and audit trail
│   ├── service/
│   │   ├── RuleManagementService.java       # Business logic: create, update, approve
│   │   ├── RuleValidationService.java       # Overlap detection, conflict checking
│   │   ├── TemporalRuleService.java         # Date-range queries, effective date logic
│   │   ├── FormulaEvaluationService.java    # Parse and evaluate formula rules
│   │   └── RuleCacheService.java            # Redis caching layer
│   ├── repository/
│   │   ├── TaxRuleRepository.java           # JPA repository for tax_rules table
│   │   └── RuleChangeLogRepository.java     # JPA repository for rule_change_log table
│   ├── model/
│   │   ├── TaxRule.java                     # Entity: rule definition
│   │   ├── RuleChangeLog.java               # Entity: audit trail
│   │   ├── RuleCategory.java                # Enum: TaxRates, IncomeInclusion, etc.
│   │   ├── RuleValueType.java               # Enum: NUMBER, PERCENTAGE, FORMULA, etc.
│   │   └── ApprovalStatus.java              # Enum: PENDING, APPROVED, REJECTED, VOIDED
│   └── dto/
│       ├── CreateRuleRequest.java
│       ├── UpdateRuleRequest.java
│       ├── RuleResponse.java
│       ├── RuleHistoryResponse.java
│       └── WhatIfAnalysisRequest.java
├── src/main/resources/
│   ├── application.yml                      # Service configuration
│   ├── db/migration/
│   │   ├── V1__create_tax_rules_table.sql
│   │   ├── V2__create_rule_change_log_table.sql
│   │   └── V3__create_tenant_rls_policies.sql
└── src/test/java/
    ├── controller/
    │   └── RuleConfigControllerTest.java    # Integration tests with MockMvc
    ├── service/
    │   ├── RuleValidationServiceTest.java   # Unit tests for overlap detection
    │   ├── TemporalRuleServiceTest.java     # Unit tests for date logic
    │   └── FormulaEvaluationServiceTest.java # Unit tests for formula parsing
    └── integration/
        └── RuleServiceIntegrationTest.java  # Testcontainers + Redis

# Backend: Updated microservice
backend/tax-engine-service/src/main/java/com/munitax/taxengine/
├── service/
│   ├── IndividualTaxCalculator.java         # MODIFIED: Query rules from rule-service
│   ├── BusinessTaxCalculator.java           # MODIFIED: Query rules from rule-service
│   └── RuleServiceClient.java               # NEW: REST client for rule-service API
├── model/
│   ├── TaxRulesConfig.java                  # KEEP: But populate from API, not constants
│   └── BusinessTaxRulesConfig.java          # KEEP: But populate from API, not constants

# Frontend: New components
components/admin/
├── RuleConfigurationDashboard.tsx           # Main dashboard: stats, recent changes
├── RuleList.tsx                             # Paginated table of all rules with filters
├── RuleEditor.tsx                           # Form for create/update rule with validation
├── RuleHistoryViewer.tsx                    # Timeline view of rule versions with diff
├── WhatIfAnalysisTool.tsx                   # Preview impact of rule changes on sample returns
├── TemporalRuleEditor.tsx                   # Date range picker with overlap detection UI
├── FormulaBuilder.tsx                       # Visual editor for formula rules (optional)
└── TenantComparisonView.tsx                 # Side-by-side table comparing rules across tenants

# Frontend: Services
services/
├── ruleService.ts                           # API client for rule-service endpoints
└── ruleCache.ts                             # Client-side caching (optional, for dashboard performance)

# Frontend: Types
types.ts                                     # ADD: TypeScript interfaces for TaxRule, RuleChangeLog, etc.

# Database migrations (within rule-service)
backend/rule-service/src/main/resources/db/migration/
├── V1__create_tax_rules_table.sql
├── V2__create_rule_change_log_table.sql
├── V3__create_tenant_rls_policies.sql
└── V4__migrate_existing_rules_from_constants.sql  # One-time data migration
```

**Structure Decision**: 

This feature follows **Option 2: Web application (microservices backend + React frontend)** from the template. We are adding a new microservice (`rule-service`) because:

1. **Domain Separation**: Rule configuration is a distinct administrative domain from tax calculation
2. **Service Ownership**: rule-service owns TaxRule data, exposes well-defined REST API, independently deployable
3. **Existing Pattern**: Project already uses microservices (auth-service, tenant-service, tax-engine-service, etc.) - adding rule-service is consistent

We are modifying tax-engine-service calculators (IndividualTaxCalculator, BusinessTaxCalculator) to call rule-service API instead of using hardcoded constants. This is the minimum change required to decouple rules from code.

Frontend admin components go in existing `components/admin/` directory alongside other admin tools. This is where tax administrator UI should live.

Database migrations use Flyway (standard Spring Boot pattern) within rule-service project. One-time migration script (V4) will seed initial rules from constants.ts/TaxRulesConfig.java defaults into database.

## Complexity Tracking

> **No violations requiring justification. All design decisions comply with constitution principles.**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |

**Notes**:
- New microservice (rule-service) is justified by clear domain boundary and independent lifecycle
- Temporal logic (effective dating) is core requirement from spec, not premature optimization
- Formula evaluation adds complexity but required for FR-025 (conditional rules like "IF income > 1M")
- If formula parsing proves too complex in Phase 1, can defer to Phase 3 and start with simple number/percentage rules only
