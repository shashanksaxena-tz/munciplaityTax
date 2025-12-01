# MuniTax Rule Engine Documentation

## Overview

The MuniTax Rule Engine is a dynamic, configurable system that manages all tax-related business rules. It supports temporal effective dating, multi-tenant isolation, and approval workflows for rule changes.

---

## Rule Engine Architecture

```mermaid
graph TB
    subgraph "Rule Management"
        CREATE[Create Rule]
        UPDATE[Update Rule]
        APPROVE[Approve Rule]
        VOID[Void Rule]
    end

    subgraph "Rule Storage"
        DB[(PostgreSQL<br/>tax_rules table)]
        CACHE[(Redis<br/>Rule Cache)]
    end

    subgraph "Rule Evaluation"
        LOAD[Load Active Rules]
        EVAL[Evaluate Rules]
        APPLY[Apply to Calculation]
    end

    subgraph "Consumers"
        TAX[Tax Engine]
        VALID[Validation]
        PENALTY[Penalty Calculator]
    end

    CREATE --> DB
    UPDATE --> DB
    APPROVE --> DB
    VOID --> DB
    
    DB --> CACHE
    CACHE --> LOAD
    LOAD --> EVAL
    EVAL --> APPLY
    
    APPLY --> TAX
    APPLY --> VALID
    APPLY --> PENALTY
```

---

## Rule Engine Components

### Component Diagram

```mermaid
classDiagram
    class RuleFacadeService {
        +createRule(CreateRuleRequest) RuleResponse
        +updateRule(id, UpdateRuleRequest) RuleResponse
        +approveRule(id, approverId) RuleResponse
        +rejectRule(id, reason) RuleResponse
        +getRules(tenantId, category, status) List~RuleResponse~
        +getActiveRules(tenantId, date, entityType) List~RuleResponse~
        +getRuleAsOf(code, tenantId, date) RuleResponse
    }

    class RuleManagementService {
        +createRule(request, userId) TaxRule
        +updateRule(id, request, userId) TaxRule
        +approveRule(id, approverId, notes) TaxRule
        +rejectRule(id, userId, reason) TaxRule
        +voidRule(id, userId, reason) void
        +getRule(id) TaxRule
        +getRulesByTenant(tenantId) List~TaxRule~
    }

    class TemporalRuleService {
        +getActiveRules(tenantId, asOfDate) List~TaxRule~
        +getActiveRuleByCode(code, tenantId, date) Optional~TaxRule~
        +getFutureRules(tenantId, fromDate) List~TaxRule~
        +getHistoricalRules(code, tenantId) List~TaxRule~
    }

    class RuleValidationService {
        +validateRule(TaxRule) ValidationResult
        +checkOverlap(code, tenantId, start, end) boolean
        +validateDependencies(TaxRule) List~String~
    }

    class RuleCacheService {
        +cacheRules(tenantId, rules) void
        +getCachedRules(tenantId) List~TaxRule~
        +invalidateCache(tenantId) void
    }

    RuleFacadeService --> RuleManagementService
    RuleFacadeService --> TemporalRuleService
    RuleManagementService --> RuleValidationService
    RuleManagementService --> RuleCacheService
    TemporalRuleService --> RuleCacheService
```

---

## Rule Data Model

### TaxRule Entity

```mermaid
erDiagram
    TAX_RULE {
        uuid rule_id PK
        string rule_code
        string rule_name
        enum category
        enum value_type
        jsonb value
        date effective_date
        date end_date
        string tenant_id
        array entity_types
        text applies_to
        int version
        uuid previous_version_id
        array depends_on
        enum approval_status
        string approved_by
        timestamp approval_date
        string created_by
        timestamp created_date
        string modified_by
        timestamp modified_date
        text change_reason
        text ordinance_reference
    }

    RULE_CHANGE_LOG {
        uuid log_id PK
        uuid rule_id FK
        enum change_type
        jsonb old_value
        jsonb new_value
        array changed_fields
        string changed_by
        timestamp change_date
        text change_reason
        int affected_returns_count
        jsonb impact_estimate
    }

    TAX_RULE ||--o{ RULE_CHANGE_LOG : "tracks"
```

---

## Rule Categories

### Category Hierarchy

```mermaid
graph TB
    subgraph "Rule Categories"
        TAX_RATES[TAX_RATES<br/>Municipal tax rates]
        INCOME[INCOME_INCLUSION<br/>What counts as taxable]
        DEDUCT[DEDUCTIONS<br/>Allowed deductions]
        PENALTY[PENALTIES<br/>Penalty calculations]
        FILING[FILING<br/>Filing requirements]
        ALLOC[ALLOCATION<br/>Apportionment formulas]
        WITHHOLD[WITHHOLDING<br/>Employer withholding]
        VALID[VALIDATION<br/>Data quality rules]
    end
```

### Category Details

| Category | Description | Example Rules |
|----------|-------------|---------------|
| **TAX_RATES** | Municipal and state tax rates | Dublin municipal rate 2.5%, Credit limit rate 2.5% |
| **INCOME_INCLUSION** | Which income types are taxable | Include Schedule C, Include gambling winnings |
| **DEDUCTIONS** | Allowed deductions and limitations | NOL offset cap 50%, Section 179 limits |
| **PENALTIES** | Penalty calculation parameters | Late filing 5% per month, Underpayment 15% |
| **FILING** | Filing thresholds and requirements | Minimum income threshold, Filing frequency |
| **ALLOCATION** | Business apportionment rules | Sales factor weight, Joyce/Finnigan election |
| **WITHHOLDING** | Employer withholding rules | Withholding rate, Deposit frequency |
| **VALIDATION** | Data validation rules | W-2 Box variance threshold, Duplicate detection |

---

## Rule Value Types

### Value Type Structure

```mermaid
graph TB
    subgraph "Value Types"
        NUM[NUMBER<br/>Simple numeric value]
        PCT[PERCENTAGE<br/>Percentage with unit]
        ENUM[ENUM<br/>Selection from list]
        BOOL[BOOLEAN<br/>True/false flag]
        FORM[FORMULA<br/>Expression with variables]
        COND[CONDITIONAL<br/>If-then-else logic]
    end
```

### Value Type Examples

**NUMBER:**
```json
{
  "scalar": 50000
}
```

**PERCENTAGE:**
```json
{
  "scalar": 2.5,
  "unit": "percent"
}
```

**ENUM:**
```json
{
  "option": "BOX_5_MEDICARE",
  "allowedValues": ["HIGHEST_OF_ALL", "BOX_5_MEDICARE", "BOX_18_LOCAL", "BOX_1_FEDERAL"]
}
```

**BOOLEAN:**
```json
{
  "flag": true
}
```

**FORMULA:**
```json
{
  "expression": "taxableIncome * rate * (1 - creditRate)",
  "variables": ["taxableIncome", "rate", "creditRate"],
  "returnType": "number"
}
```

**CONDITIONAL:**
```json
{
  "condition": "income > 150000",
  "thenValue": 0,
  "elseValue": 25000,
  "returnType": "number"
}
```

---

## Rule Lifecycle

### Lifecycle Diagram

```mermaid
stateDiagram-v2
    [*] --> PENDING: Create Rule
    PENDING --> APPROVED: Approve
    PENDING --> REJECTED: Reject
    APPROVED --> APPROVED: Update (new version)
    APPROVED --> VOIDED: Void
    REJECTED --> PENDING: Revise
    VOIDED --> [*]
```

### Approval Workflow

```mermaid
sequenceDiagram
    participant Admin
    participant RuleService
    participant DB
    participant Cache

    Admin->>RuleService: Create Rule Request
    RuleService->>RuleService: Validate Rule
    RuleService->>RuleService: Check Date Overlaps
    RuleService->>DB: Insert with PENDING status
    RuleService->>DB: Log Change
    RuleService-->>Admin: Rule Created (PENDING)

    Admin->>RuleService: Approve Rule
    RuleService->>RuleService: Validate Approver Permissions
    RuleService->>DB: Update status to APPROVED
    RuleService->>DB: Set approved_by, approval_date
    RuleService->>Cache: Invalidate tenant cache
    RuleService-->>Admin: Rule Approved
```

---

## Temporal Rule Management

### Effective Date Handling

```mermaid
gantt
    title Rule Effective Date Example
    dateFormat  YYYY-MM-DD
    section Tax Rate Rule
    Version 1 (2.0%)    :v1, 2024-01-01, 2024-06-30
    Version 2 (2.5%)    :v2, 2024-07-01, 2025-12-31
    Version 3 (3.0%)    :v3, 2026-01-01, 2099-12-31
```

### Point-in-Time Query

```java
// Get rule active on specific date
public Optional<TaxRule> getActiveRuleByCode(
    String ruleCode, 
    String tenantId, 
    LocalDate asOfDate
) {
    return ruleRepository.findByRuleCodeAndTenantId(ruleCode, tenantId)
        .stream()
        .filter(rule -> rule.isActiveOn(asOfDate))
        .findFirst();
}
```

### Overlap Prevention

```mermaid
flowchart TB
    CREATE[Create Rule Request]
    CHECK[Check Existing Rules]
    OVERLAP{Dates Overlap?}
    REJECT[Reject with Error]
    ACCEPT[Accept Rule]

    CREATE --> CHECK
    CHECK --> OVERLAP
    OVERLAP -->|Yes| REJECT
    OVERLAP -->|No| ACCEPT
```

---

## Rule Integration with Tax Engine

### Integration Flow

```mermaid
sequenceDiagram
    participant TaxEngine
    participant RuleIntegration
    participant RuleService
    participant Cache

    TaxEngine->>RuleIntegration: getConfigForCalculation(tenantId, taxYear)
    RuleIntegration->>Cache: Check cached rules
    
    alt Cache Hit
        Cache-->>RuleIntegration: Cached rules
    else Cache Miss
        RuleIntegration->>RuleService: getActiveRules(tenantId, asOfDate)
        RuleService-->>RuleIntegration: Active rules
        RuleIntegration->>Cache: Store in cache
    end
    
    RuleIntegration->>RuleIntegration: Build TaxRulesConfig
    RuleIntegration-->>TaxEngine: TaxRulesConfig object
    TaxEngine->>TaxEngine: Calculate tax using rules
```

### TaxRulesConfig Structure

```java
public interface TaxRulesConfig {
    double municipalRate();           // From TAX_RATES category
    double municipalCreditLimitRate();
    Map<String, Double> municipalRates();
    
    W2QualifyingWagesRule w2QualifyingWagesRule();  // From INCOME_INCLUSION
    IncomeInclusion incomeInclusion();
    
    boolean enableRounding();         // From FILING category
}

public interface BusinessTaxRulesConfig {
    double municipalRate();           // From TAX_RATES
    double minimumTax();
    
    String allocationMethod();        // From ALLOCATION
    double allocationSalesFactorWeight();
    
    boolean enableNOL();              // From DEDUCTIONS
    double nolOffsetCapPercent();
    
    double penaltyRateLateFiling();   // From PENALTIES
    double penaltyRateUnderpayment();
    double interestRateAnnual();
    double safeHarborPercent();
}
```

---

## Built-in Rules (Default Configuration)

### Tax Rate Rules

| Rule Code | Category | Default Value | Description |
|-----------|----------|---------------|-------------|
| `MUNICIPAL_TAX_RATE` | TAX_RATES | 2.5% | Dublin municipal tax rate |
| `CREDIT_LIMIT_RATE` | TAX_RATES | 2.5% | Maximum credit rate |
| `MINIMUM_BUSINESS_TAX` | TAX_RATES | $0 | Minimum annual business tax |

### Income Inclusion Rules

| Rule Code | Category | Default Value | Description |
|-----------|----------|---------------|-------------|
| `W2_QUALIFYING_WAGES_METHOD` | INCOME_INCLUSION | HIGHEST_OF_ALL | How to determine qualifying wages |
| `INCLUDE_SCHEDULE_C` | INCOME_INCLUSION | true | Include Schedule C income |
| `INCLUDE_SCHEDULE_E` | INCOME_INCLUSION | true | Include Schedule E income |
| `INCLUDE_SCHEDULE_F` | INCOME_INCLUSION | true | Include Schedule F income |
| `INCLUDE_GAMBLING` | INCOME_INCLUSION | true | Include W-2G gambling winnings |
| `INCLUDE_1099` | INCOME_INCLUSION | true | Include 1099 income |

### Deduction Rules

| Rule Code | Category | Default Value | Description |
|-----------|----------|---------------|-------------|
| `NOL_ENABLED` | DEDUCTIONS | true | Allow NOL carryforward |
| `NOL_OFFSET_CAP` | DEDUCTIONS | 50% | Maximum NOL offset percentage |
| `INTANGIBLE_EXPENSE_RATE` | DEDUCTIONS | 5% | 5% rule for intangible expenses |

### Penalty Rules

| Rule Code | Category | Default Value | Description |
|-----------|----------|---------------|-------------|
| `LATE_FILING_RATE` | PENALTIES | 5% | Monthly late filing penalty |
| `LATE_FILING_MAX` | PENALTIES | 25% | Maximum late filing penalty |
| `LATE_FILING_MIN` | PENALTIES | $50 | Minimum penalty amount |
| `UNDERPAYMENT_RATE` | PENALTIES | 15% | Underpayment penalty rate |
| `INTEREST_RATE_ANNUAL` | PENALTIES | 7% | Annual interest rate |
| `SAFE_HARBOR_PERCENT` | PENALTIES | 90% | Safe harbor threshold |

### Filing Rules

| Rule Code | Category | Default Value | Description |
|-----------|----------|---------------|-------------|
| `ENABLE_ROUNDING` | FILING | false | Round to whole dollars |
| `QUARTERLY_DUE_DAYS` | FILING | 30 | Days after quarter end |
| `MONTHLY_DUE_DAY` | FILING | 15 | Day of following month |

### Allocation Rules

| Rule Code | Category | Default Value | Description |
|-----------|----------|---------------|-------------|
| `ALLOCATION_METHOD` | ALLOCATION | 3_FACTOR | Apportionment method |
| `SALES_FACTOR_WEIGHT` | ALLOCATION | 1.0 | Double weighting for sales |

### Validation Rules

| Rule Code | Category | Default Value | Description |
|-----------|----------|---------------|-------------|
| `W2_BOX_VARIANCE_THRESHOLD` | VALIDATION | 20% | Max variance between Box 1 and 18 |
| `MAX_WITHHOLDING_RATE` | VALIDATION | 3.0% | Maximum valid withholding rate |
| `DUPLICATE_WAGE_THRESHOLD` | VALIDATION | $10 | Threshold for duplicate detection |

---

## Adding Custom Rules

### Rule Creation Process

```mermaid
flowchart TB
    subgraph "Step 1: Define Rule"
        DEF[Define rule code, name, category]
        VAL[Choose value type and value]
        DATE[Set effective dates]
    end

    subgraph "Step 2: Configure"
        TENANT[Select tenant]
        ENTITY[Select entity types]
        DEP[Define dependencies]
    end

    subgraph "Step 3: Submit"
        CREATE[Create rule]
        REASON[Provide change reason]
        REF[Add ordinance reference]
    end

    subgraph "Step 4: Approve"
        REVIEW[Manager reviews]
        APPROVE[Approve rule]
        ACTIVE[Rule becomes active]
    end

    DEF --> VAL --> DATE
    DATE --> TENANT --> ENTITY --> DEP
    DEP --> CREATE --> REASON --> REF
    REF --> REVIEW --> APPROVE --> ACTIVE
```

### Example: Adding New Penalty Rule

```json
{
  "ruleCode": "EXTENSION_PENALTY_RATE",
  "ruleName": "Filing Extension Penalty Rate",
  "category": "PENALTIES",
  "valueType": "PERCENTAGE",
  "value": {
    "scalar": 2.0,
    "unit": "percent"
  },
  "effectiveDate": "2025-01-01",
  "tenantId": "dublin",
  "entityTypes": ["INDIVIDUAL", "BUSINESS"],
  "changeReason": "Adding penalty for late extension requests per City Council Resolution 2024-123",
  "ordinanceReference": "Dublin City Code §1234.56"
}
```

### Example: Adding Formula Rule

```json
{
  "ruleCode": "TIERED_RATE_CALCULATION",
  "ruleName": "Tiered Tax Rate Calculation",
  "category": "TAX_RATES",
  "valueType": "CONDITIONAL",
  "value": {
    "condition": "taxableIncome > 500000",
    "thenValue": 0.03,
    "elseValue": 0.025,
    "returnType": "number"
  },
  "effectiveDate": "2026-01-01",
  "tenantId": "dublin",
  "entityTypes": ["BUSINESS"],
  "changeReason": "Implementing tiered rate structure for high-income businesses",
  "ordinanceReference": "Dublin City Ordinance 2025-789"
}
```

---

## Rule API Reference

### Create Rule

```http
POST /api/v1/rules
Content-Type: application/json
Authorization: Bearer {token}

{
  "ruleCode": "NEW_RULE_CODE",
  "ruleName": "New Rule Name",
  "category": "TAX_RATES",
  "valueType": "PERCENTAGE",
  "value": {"scalar": 2.5, "unit": "percent"},
  "effectiveDate": "2025-01-01",
  "tenantId": "dublin",
  "entityTypes": ["INDIVIDUAL", "BUSINESS"],
  "changeReason": "Adding new rule for tax calculation"
}
```

### Get Active Rules

```http
GET /api/v1/rules/active?tenantId=dublin&asOfDate=2025-01-15&entityType=BUSINESS
Authorization: Bearer {token}
```

### Approve Rule

```http
POST /api/v1/rules/{ruleId}/approve
Content-Type: application/json
Authorization: Bearer {token}

{
  "approverId": "manager-uuid"
}
```

---

## Integration Status & Known Issues

> **Reference:** See `/RULE_ENGINE_DISCONNECT_ANALYSIS.md` for detailed analysis.

### Current Integration Architecture

```mermaid
flowchart TB
    subgraph "Intended Flow"
        TE1[Tax Engine Service<br/>Port 8085] -->|REST Call| RS1[Rule Service<br/>Port 8087]
        RS1 --> DB1[(Shared PostgreSQL<br/>munitax_db)]
        TE1 --> DB1
    end
    
    subgraph "Current Reality - DISCONNECTED"
        TE2[Tax Engine Service<br/>Port 8085] --> DB2[(Local PostgreSQL<br/>munitax_db)]
        RS2[Rule Service<br/>Port 8087] --> DB3[(Aiven Cloud<br/>External DB)]
    end
```

### Known Issues

| Issue | Severity | Status | Description |
|-------|----------|--------|-------------|
| Database Disconnect | CRITICAL | Open | Rule service connects to external cloud DB instead of local Docker postgres |
| Missing Schema | CRITICAL | Open | tax_rules table not created in munitax_db |
| Enum Mismatch | LOW | Documented | Spec uses PascalCase, code uses SCREAMING_SNAKE_CASE (code is correct) |
| Service Isolation | HIGH | Open | Rule service cannot integrate with tax-engine due to separate databases |

### Recommended Fixes

1. **Fix rule-service database configuration** to use local Docker postgres
2. **Run migrations** to create tax_rules tables in munitax_db
3. **Update specs** to match actual code enum conventions

For detailed remediation steps, see `/RULE_ENGINE_DISCONNECT_ANALYSIS.md`.

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.1 | 2025-12-01 | Added integration status and known issues section |
| 1.0 | 2025-12-01 | Initial rule engine documentation |

---

**Document Owner:** Development Team  
**Last Updated:** December 1, 2025
