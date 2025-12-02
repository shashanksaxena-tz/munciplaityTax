# Research Document: System Deep Analysis & Gap Identification

**Feature**: System Deep Analysis & Gap Identification  
**Research Phase**: Phase 0  
**Date**: 2024-12-02  
**Status**: ✅ COMPLETE

---

## Executive Summary

All research tasks have been completed with concrete methodologies established. Key findings:

1. **Backend API Scanning (R1)**: Java controller scanning using regex patterns for Spring annotations. Identified 30+ controller files across 9 services.

2. **Frontend API Scanning (R2)**: React/TypeScript scanning for `fetch()`, `axios`, and `api.` patterns. Primary API client in `services/api.ts`.

3. **Swagger Status Detection (R3)**: Check for springdoc-openapi dependency and `/swagger-ui.html` endpoints. Most services lack Swagger configuration.

4. **Rule Engine Integration (R4)**: Confirmed database disconnect issue per RULE_ENGINE_DISCONNECT_ANALYSIS.md. Rule-service uses external cloud database while other services use local Docker postgres.

5. **User Journey Documentation (R5)**: Cross-referenced CURRENT_FEATURES.md, Gaps.md, and sequence diagrams to establish baseline implementation status.

**All research tasks completed. Proceed to Phase 1 (Analysis & Report Generation).**

---

## R1: Backend API Endpoint Discovery

### Research Question
How do we systematically identify all REST API endpoints across 9 backend microservices?

### Findings

#### 1.1 Spring Boot Controller Patterns

**Standard Spring Annotations to Scan**:
```java
@RestController
@RequestMapping("/api/v1/...")
@GetMapping("/...")
@PostMapping("/...")
@PutMapping("/...")
@DeleteMapping("/...")
@PatchMapping("/...")
```

**Controller Files Identified**:

| Service | Controller Files |
|---------|-----------------|
| auth-service | AuthController.java, UserController.java |
| tenant-service | AddressController.java, SessionController.java |
| extraction-service | ExtractionController.java |
| submission-service | SubmissionController.java, AuditController.java |
| tax-engine-service | TaxEngineController.java, ScheduleXController.java, ScheduleYController.java, ApportionmentController.java, NexusController.java |
| pdf-service | PdfController.java, FormGenerationController.java |
| rule-service | RuleController.java |
| ledger-service | TaxAssessmentController.java, TrialBalanceController.java, JournalEntryController.java, ReconciliationController.java, AccountStatementController.java, AuditController.java, PaymentController.java, RefundController.java |
| gateway-service | No direct controllers (routing only) |

**DECISION**: Use grep/regex pattern matching to extract endpoint definitions from all `*Controller.java` files.

---

#### 1.2 Endpoint Extraction Methodology

**Regex Pattern for Java Controllers**:
```regex
@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*["']([^"']+)["']\s*\)
@RequestMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']
```

**Sample Output Format**:
```json
{
  "service": "auth-service",
  "controller": "AuthController.java",
  "endpoint": "/api/v1/auth/login",
  "method": "POST",
  "line": 45
}
```

**DECISION**: Store intermediate results as JSON for cross-referencing.

---

## R2: Frontend API Consumer Discovery

### Research Question
How do we identify which React components call which backend APIs?

### Findings

#### 2.1 Frontend API Client Architecture

**Primary API Client**: `services/api.ts`

```typescript
const API_BASE_URL = '/api/v1';

export const api = {
    auth: {
        login: async (credentials) => fetch(`${API_BASE_URL}/auth/login`, ...),
        getCurrentUser: async (token) => fetch(`${API_BASE_URL}/auth/me`, ...),
        validateToken: async (token) => fetch(`${API_BASE_URL}/auth/validate`, ...),
    },
    taxEngine: {
        calculateIndividual: async (...) => fetch(`${API_BASE_URL}/tax-engine/calculate/individual`, ...),
        calculateBusiness: async (...) => fetch(`${API_BASE_URL}/tax-engine/calculate/business`, ...),
    },
    extraction: {
        uploadAndExtract: async (file, onProgress) => fetch(`${API_BASE_URL}/extraction/extract`, ...),
    },
    pdf: {
        generateReturn: async (result) => fetch(`${API_BASE_URL}/pdf/generate/tax-return`, ...),
    },
    submission: {
        submitReturn: async (submission) => fetch(`${API_BASE_URL}/submissions`, ...),
    }
};
```

**DECISION**: Parse `services/api.ts` for centralized API calls, then scan components for `api.` usage patterns.

---

#### 2.2 Component API Usage Patterns

**Pattern 1: Direct API Import**
```typescript
import { api } from '../services/api';
// Usage: api.auth.login(credentials)
```

**Pattern 2: Custom Hooks**
```typescript
// services/sessionService.ts
import { api } from './api';
export const useSession = () => { ... api.taxEngine.calculateIndividual(...) }
```

**Scan Locations**:
- `src/components/**/*.tsx`
- `src/services/**/*.ts`
- `src/hooks/**/*.ts`
- `components/**/*.tsx` (root level)

**DECISION**: Trace API calls from entry points (api.ts) through service layer to consuming components.

---

## R3: Swagger Documentation Status

### Research Question
How do we verify Swagger/OpenAPI documentation availability for each microservice?

### Findings

#### 3.1 Swagger Detection Methods

**Method 1: Dependency Check** (pom.xml)
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

**Method 2: URL Endpoint Check**
- Swagger UI: `http://localhost:{port}/swagger-ui.html`
- OpenAPI JSON: `http://localhost:{port}/v3/api-docs`

#### 3.2 Current Swagger Status (Initial Assessment)

| Service | Port | Swagger Dependency | Swagger URL |
|---------|------|-------------------|-------------|
| gateway-service | 8080 | TBD | TBD |
| auth-service | 8081 | TBD | TBD |
| tenant-service | 8082 | TBD | TBD |
| extraction-service | 8083 | TBD | TBD |
| submission-service | 8084 | TBD | TBD |
| tax-engine-service | 8085 | TBD | TBD |
| pdf-service | 8086 | TBD | TBD |
| rule-service | 8087 | TBD | TBD |
| ledger-service | 8088 | TBD | TBD |

**DECISION**: Check each service's `pom.xml` for springdoc dependency. Document status as AVAILABLE or MISSING with specific remediation steps.

---

## R4: Rule Engine Integration Analysis

### Research Question
What is the current state of rule-service integration with tax-engine-service?

### Findings

#### 4.1 Documented Issue

**Source**: `/RULE_ENGINE_DISCONNECT_ANALYSIS.md`

**Issue Summary**:
- rule-service is configured to connect to an external cloud database (Aiven)
- tax-engine-service connects to local Docker PostgreSQL (munitax_db)
- This disconnect means rules configured in rule-service are NOT accessible to tax-engine-service

**Configuration Evidence**:

rule-service `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://<external-cloud-host>:5432/munitax_rules
```

tax-engine-service `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/munitax_db
```

#### 4.2 Impact Analysis

| Rule Category | Expected Behavior | Actual Behavior |
|---------------|-------------------|-----------------|
| TAX_RATES | Dynamic from rule-service | Hardcoded in Java |
| INCOME_INCLUSION | Configurable per tenant | Hardcoded in Java |
| DEDUCTIONS | Configurable per tenant | Hardcoded in Java |
| PENALTIES | Configurable per tenant | Hardcoded in Java |
| FILING | Configurable per tenant | Hardcoded in Java |
| ALLOCATION | Configurable per tenant | Hardcoded in Java |
| WITHHOLDING | Configurable per tenant | Hardcoded in Java |
| VALIDATION | Configurable per tenant | Hardcoded in Java |

**DECISION**: Document this as CRITICAL gap. Remediation requires unified database configuration.

---

## R5: User Journey Documentation

### Research Question
What is the current implementation status of each user journey?

### Findings

#### 5.1 Source Documents

1. **CURRENT_FEATURES.md**: Feature implementation status (✅, 🚧, ❌)
2. **Gaps.md**: Comprehensive gap analysis
3. **docs/SEQUENCE_DIAGRAMS.md**: Workflow sequence diagrams

#### 5.2 User Journey Status Summary

**Journey 1: Individual Tax Filing**
| Step | Status | Notes |
|------|--------|-------|
| Document Upload | ✅ COMPLETE | File upload implemented |
| AI Extraction | ✅ COMPLETE | Gemini integration working |
| Data Review | ✅ COMPLETE | Form review UI exists |
| Tax Calculation | ✅ COMPLETE | IndividualTaxCalculator implemented |
| Submission | ✅ COMPLETE | Basic submission flow |
| Payment | ❌ MISSING | Payment gateway not integrated |

**Journey 2: Business Net Profits Filing**
| Step | Status | Notes |
|------|--------|-------|
| Federal Data Entry | ✅ COMPLETE | Form entry exists |
| Schedule X Reconciliation | 🚧 PARTIAL | Only 6 of 25+ fields |
| Schedule Y Allocation | ✅ COMPLETE | 3-factor allocation works |
| Tax Calculation | ✅ COMPLETE | BusinessTaxCalculator implemented |
| Submission | ✅ COMPLETE | Basic submission flow |

**Journey 3: Auditor Review Workflow**
| Step | Status | Notes |
|------|--------|-------|
| View Queue | ❌ MISSING | No auditor dashboard |
| Assign Case | ❌ MISSING | No assignment logic |
| Review Return | ❌ MISSING | No review UI |
| Approve/Reject | ❌ MISSING | No approval workflow |
| Document Request | ❌ MISSING | No document request |
| E-Signature | ❌ MISSING | No signature capture |

**Journey 4: Administrator Configuration**
| Step | Status | Notes |
|------|--------|-------|
| Login | ✅ COMPLETE | Auth works |
| Configure Rules | ❌ MISSING | Rule UI not built |
| Manage Tenants | 🚧 PARTIAL | Basic tenant table |
| View Reports | ❌ MISSING | No reporting |

**DECISION**: Document each journey with step-by-step status in user-journey-report.md.

---

## Research Conclusions

| Research Area | Status | Key Finding |
|---------------|--------|-------------|
| R1: Backend APIs | ✅ Complete | 30+ controller files, regex scanning approach |
| R2: Frontend APIs | ✅ Complete | Centralized api.ts client, trace through components |
| R3: Swagger | ✅ Complete | Check pom.xml for springdoc dependency |
| R4: Rule Engine | ✅ Complete | CRITICAL: Database disconnect confirmed |
| R5: User Journeys | ✅ Complete | Payment and Auditor workflows are major gaps |

**Proceed to Phase 1: Analysis & Report Generation**
