# Implementation Plan: Enhanced Discrepancy Detection (10+ Validation Rules)

**Branch**: `3-enhanced-discrepancy-detection` | **Date**: 2025-11-27 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/3-enhanced-discrepancy-detection/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

This feature implements comprehensive discrepancy detection for municipal tax returns with 10+ validation rules covering W-2 box consistency (Box 1 vs 18), Schedule C estimated tax validation, rental property counts, K-1 allocation checks, municipal credit limits, withholding rate validation, cross-year carryforward verification, federal/local reconciliation, duplicate detection, and passive loss limitations. The system will detect data entry errors, incomplete forms, and compliance violations before filing submission, reducing filing errors by 30% and preventing invalid submissions that would be rejected during auditor review.

**Technical Approach**: Extend the existing `IndividualTaxCalculator.java` service with a comprehensive `analyzeDiscrepancies()` method that validates tax return data against 22 functional requirements (FR-001 through FR-022). The expanded `DiscrepancyReport` model will support severity levels (HIGH/MEDIUM/LOW), user acceptance of warnings, and recommended actions. Frontend `DiscrepancyView.tsx` component will be enhanced to display categorized issues with acceptance workflows. Validation will run on-demand when users click "Review" button before submission, with HIGH severity issues blocking the filing submission endpoint.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3.2.3), TypeScript (React 18+)  
**Primary Dependencies**: Spring Boot, Spring Cloud (service discovery), React, Vite, Tailwind CSS  
**Storage**: PostgreSQL 16+ (multi-tenant schemas), Redis 7+ (caching)  
**Testing**: JUnit 5 (backend unit/integration tests), React Testing Library (frontend)  
**Target Platform**: Linux server (Docker/Kubernetes), Web browsers (Chrome, Firefox, Safari, Edge)  
**Project Type**: Web application (React frontend + Spring Boot microservices backend)  
**Performance Goals**: Validation completes in <3 seconds for returns with 10 forms, <200ms p95 API response  
**Constraints**: Must maintain 95% accuracy (false positive rate <10%), block HIGH severity issues 100% of time  
**Scale/Scope**: 5,000-10,000 annual tax returns per municipality, 1,000 concurrent users during tax season

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Microservices Architecture First ✅

**Compliant**: Feature extends existing `tax-engine-service` microservice, which is the appropriate domain owner for tax calculation and validation logic. No new service required—validation rules are core tax domain concerns that belong in the tax engine.

**Service Placement**: `IndividualTaxCalculator` is already part of `tax-engine-service`. Expanded `analyzeDiscrepancies()` method will live alongside existing calculation logic, sharing access to `TaxFormData` models and `TaxRulesConfig`.

### II. Multi-Tenant Data Isolation (NON-NEGOTIABLE) ✅

**Compliant**: All validation rules respect tenant context inherited from existing tax calculation flows. Tenant-specific tax rates, rules, and prior year data are already scoped via JWT tenant claims. No cross-tenant data access.

**Tenant-Specific Validation**: Dublin's 2.5% withholding rate validation (FR-002) will use tenant-specific `TaxRulesConfig`, not hardcoded values. Municipal credit rules (FR-014 through FR-016) will vary by tenant jurisdiction.

### III. Audit Trail Immutability ✅

**Compliant**: Validation results will be persisted with tax return submissions. `DiscrepancyReport` includes `validationDate`, `validationRulesVersion`, and user acceptance data (`isAccepted`, `acceptanceNote`, `acceptedDate`) for audit trail. Discrepancy acceptance creates audit log entries showing who accepted what warning and when.

**Retention**: Validation reports stored with returns for 7-year IRS retention requirement.

### IV. AI Transparency & Explainability ✅

**Compliant**: Validation rules compare AI-extracted values against expected calculations, surfacing discrepancies when extraction may be incorrect. Each discrepancy issue includes `recommendedAction` explaining what user should verify (e.g., "Verify Box 18 entry against W-2 paper copy").

**Confidence Integration**: Feature will leverage existing AI extraction confidence scores. Low-confidence extractions below threshold will automatically trigger validation warnings (e.g., "Box 18 value has low extraction confidence—verify against paper W-2").

### V. Security & Compliance First ✅

**Compliant**: No new PII storage. Validation operates on existing tax form data already subject to encryption and access controls. No SSN/EIN logging in validation messages. Validation results inherit same security posture as tax return data.

**Authorization**: Only filers and auditors can view validation results for returns they have access to. No new authorization rules needed—existing RBAC applies.

### VI. User-Centric Design ✅

**Compliant**: Validation errors categorized by severity with clear, actionable messages. HIGH severity issues block filing with explanation. MEDIUM/LOW issues show warnings with "Accept" option. Progressive disclosure: users see summary counts (3 HIGH, 2 MEDIUM) before expanding to see details.

**Error Prevention**: Real-time validation catches errors before submission, not after rejection. Recommended actions guide users to fix issues (e.g., "Attach Form 8582 for passive loss limitation support").

### VII. Test Coverage & Quality Gates ✅

**Compliant**: All 22 validation rules (FR-001 through FR-022) will have unit tests covering pass/fail scenarios. Integration tests will verify validation blocks submission for HIGH severity issues. User story acceptance scenarios from spec.md will map directly to test cases.

**Coverage Target**: `analyzeDiscrepancies()` method ≥95% coverage, critical path validations (W-2 Box consistency, credit limits) 100% coverage.

---

**Gate Status**: ✅ **PASSED** - All constitution principles satisfied. No deviations or exceptions required.

## Project Structure

### Documentation (this feature)

```text
specs/3-enhanced-discrepancy-detection/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── validation-api.yaml          # POST /api/tax-engine/validate endpoint
│   └── discrepancy-report-schema.json  # DiscrepancyReport response model
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/tax-engine-service/
├── src/main/java/com/munitax/taxengine/
│   ├── model/
│   │   ├── DiscrepancyReport.java           # NEW: Expanded model with severity, acceptance
│   │   ├── DiscrepancyIssue.java            # NEW: Individual issue entity
│   │   ├── ValidationRule.java              # NEW: Rule ID enum (FR-001, FR-002, etc.)
│   │   └── TaxCalculationResult.java        # MODIFIED: Add discrepancyReport field
│   ├── service/
│   │   ├── IndividualTaxCalculator.java     # MODIFIED: Add analyzeDiscrepancies() method
│   │   ├── DiscrepancyValidator.java        # NEW: Validation logic organized by category
│   │   ├── W2Validator.java                 # NEW: FR-001 through FR-005 implementations
│   │   ├── ScheduleValidator.java           # NEW: FR-006 through FR-010 implementations
│   │   ├── K1Validator.java                 # NEW: FR-011 through FR-013 implementations
│   │   ├── CreditValidator.java             # NEW: FR-014 through FR-016 implementations
│   │   ├── ReconciliationValidator.java     # NEW: FR-017 through FR-019 implementations
│   │   └── CarryforwardValidator.java       # NEW: FR-020 through FR-022 implementations
│   └── controller/
│       └── TaxEngineController.java         # MODIFIED: Add POST /validate endpoint
└── src/test/java/com/munitax/taxengine/
    ├── service/
    │   ├── IndividualTaxCalculatorTest.java # MODIFIED: Add validation tests
    │   ├── W2ValidatorTest.java             # NEW: FR-001 through FR-005 test cases
    │   ├── ScheduleValidatorTest.java       # NEW: FR-006 through FR-010 test cases
    │   ├── K1ValidatorTest.java             # NEW: FR-011 through FR-013 test cases
    │   ├── CreditValidatorTest.java         # NEW: FR-014 through FR-016 test cases
    │   ├── ReconciliationValidatorTest.java # NEW: FR-017 through FR-019 test cases
    │   └── CarryforwardValidatorTest.java   # NEW: FR-020 through FR-022 test cases
    └── integration/
        └── ValidationEndpointTest.java      # NEW: Integration tests for validation API

components/
├── DiscrepancyView.tsx                      # MODIFIED: Support severity, categories, acceptance
├── DiscrepancyIssueCard.tsx                 # NEW: Individual issue display component
├── DiscrepancySummary.tsx                   # NEW: High-level summary with counts
└── ValidationReport.tsx                     # NEW: PDF export of validation results

services/
└── taxEngineService.ts                      # MODIFIED: Add validateReturn() API call
```

**Structure Decision**: Web application structure (Option 2 from template) with separate frontend (React components) and backend (Spring Boot microservices). Feature extends existing `tax-engine-service` microservice, which is appropriate for tax calculation and validation domain logic. No new microservice needed—validation is core tax engine responsibility. Frontend components will be added to existing `components/` directory following established patterns.

## Complexity Tracking

> **Feature does not violate any constitution principles—no justification needed.**

This section intentionally left empty. All validation rules are additions to existing `tax-engine-service` domain. No new services, repositories, or architectural patterns introduced. Complexity is managed through modular validator classes (W2Validator, ScheduleValidator, etc.) that follow Single Responsibility Principle, making rules independently testable and maintainable.
