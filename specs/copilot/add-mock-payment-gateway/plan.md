# Implementation Plan: Mock Payment Gateway Integration

**Branch**: `copilot/add-mock-payment-gateway` | **Date**: 2025-12-03 | **Spec**: [spec.md](../../17-mock-payment-gateway-integration/spec.md)
**Input**: Feature specification from `/specs/17-mock-payment-gateway-integration/spec.md`

## Summary

Enhance the existing mock payment gateway to:
1. **Remove hardcoded frontend test payment data** by fetching it from a new backend API endpoint (`GET /api/v1/payments/test-methods`)
2. **Ensure proper payment-ledger integration** with double-entry journal entries (already implemented)
3. **Enhance reconciliation capabilities** for payment tracking with drill-down to individual payment discrepancies
4. **Provide comprehensive audit reports** for all payment events with payment-specific filtering

**Technical Approach**: Add a new REST endpoint to `PaymentController` that returns test credit cards and ACH accounts from `MockPaymentProviderService`. Update `PaymentGateway.tsx` to fetch test methods via API instead of using static data. Enhance `ReconciliationReport.tsx` with payment-specific drill-down. Enhance `AuditTrail.tsx` with payment-specific filters.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript/React 18+ (frontend)  
**Primary Dependencies**: Spring Boot 3.2.3, React 18, Vite, Tailwind CSS  
**Storage**: PostgreSQL 16+ (multi-tenant schemas), Redis 7+ (caching)  
**Testing**: JUnit (backend), Jest/Vitest (frontend)  
**Target Platform**: Web application (Linux server backend, browser frontend)  
**Project Type**: Web application (frontend + backend microservices)  
**Performance Goals**: Test methods API must respond within 500ms (per NFR-001)  
**Constraints**: Test methods API is stateless and cacheable (per NFR-002)  
**Scale/Scope**: Municipality tax system supporting 1,000+ concurrent users during tax season

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Implementation Check (✅ PASS)

| Principle | Status | Justification |
|-----------|--------|---------------|
| I. Microservices Architecture First | ✅ PASS | Feature adds endpoint to existing `ledger-service`, no new service needed |
| II. Multi-Tenant Data Isolation | ✅ PASS | Test methods are tenant-agnostic (same test cards for all tenants); TEST mode returns empty in PRODUCTION |
| III. Audit Trail Immutability | ✅ PASS | Payment events already logged via `AuditLogService`; enhancing filter UI only |
| IV. AI Transparency & Explainability | ⚪ N/A | Feature does not involve AI extraction |
| V. Security & Compliance First | ✅ PASS | Test methods endpoint does NOT require authentication (protected by TEST mode); no sensitive data exposed |
| VI. User-Centric Design | ✅ PASS | Auto-fill test cards improves developer/tester experience; graceful error handling |
| VII. Test Coverage & Quality Gates | ✅ PASS | Unit tests for new endpoint; frontend integration tests for API fetch |

### Post-Design Check (✅ PASS)

All principles verified post-design. No violations detected.

## Project Structure

### Documentation (this feature)

```text
specs/copilot/add-mock-payment-gateway/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── test-methods-api.yaml
└── tasks.md             # Phase 2 output (NOT created by this plan)
```

### Source Code (repository root)

```text
backend/ledger-service/
├── src/main/java/com/munitax/ledger/
│   ├── controller/
│   │   └── PaymentController.java      # Add GET /test-methods endpoint
│   ├── dto/
│   │   ├── TestPaymentMethodsResponse.java  # NEW: Response DTO
│   │   └── TestCreditCard.java              # NEW: Card DTO
│   │   └── TestACHAccount.java              # NEW: ACH DTO
│   └── service/
│       ├── MockPaymentProviderService.java  # Add getTestPaymentMethods()
│       └── PaymentService.java              # Existing (no changes needed)
└── src/test/java/com/munitax/ledger/
    └── controller/
        └── PaymentControllerTest.java       # Add test-methods endpoint tests

components/
├── PaymentGateway.tsx      # Update to fetch test methods from API
├── ReconciliationReport.tsx # Add payment-specific drill-down
└── AuditTrail.tsx          # Add payment-specific filters
```

**Structure Decision**: Web application structure with existing backend (`ledger-service`) and frontend (`components/`). Changes are minimal additions to existing files.

## Complexity Tracking

> No constitution violations detected. Complexity is appropriate for the feature scope.
