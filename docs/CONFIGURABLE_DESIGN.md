# MuniTax Configurable Design Documentation

## Overview

The MuniTax system is designed with configurability at its core, allowing municipalities to customize tax rules, rates, validation thresholds, and workflows without code changes. This document describes the configurable aspects of the system and how to customize them.

---

## Configuration Architecture

```mermaid
graph TB
    subgraph "Configuration Sources"
        DB[(Database Rules)]
        ENV[Environment Variables]
        YAML[YAML Config Files]
    end

    subgraph "Configuration Layer"
        RULE[Rule Service]
        CONFIG[Spring Config]
        CACHE[Redis Cache]
    end

    subgraph "Consuming Services"
        TAX[Tax Engine]
        VALID[Validation]
        PENALTY[Penalty Calculator]
        AUDIT[Auditor Workflow]
    end

    DB --> RULE
    ENV --> CONFIG
    YAML --> CONFIG
    
    RULE --> CACHE
    CONFIG --> TAX
    CONFIG --> VALID
    CACHE --> TAX
    CACHE --> VALID
    CACHE --> PENALTY
    CACHE --> AUDIT
```

---

## Configurable Components

### 1. Tax Rates Configuration

```mermaid
classDiagram
    class TaxRateConfig {
        <<configurable>>
        +municipalRate: double
        +minimumTax: double
        +creditLimitRate: double
        +withholdinRate: double
    }
    
    class TenantConfig {
        +tenantId: string
        +taxRates: TaxRateConfig
        +effectiveDate: date
        +endDate: date
    }
    
    TenantConfig --> TaxRateConfig
```

**Configurable Parameters:**

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| `municipalRate` | 2.5% | 0-5% | Municipal income tax rate |
| `minimumTax` | $0 | $0-$500 | Minimum annual business tax |
| `creditLimitRate` | 2.5% | 0-5% | Maximum credit for taxes paid elsewhere |
| `withholdingRate` | 2.5% | 0-5% | Employer withholding rate |

**Configuration via Rule Engine:**
```json
{
  "ruleCode": "MUNICIPAL_TAX_RATE",
  "category": "TAX_RATES",
  "valueType": "PERCENTAGE",
  "value": {"scalar": 2.5, "unit": "percent"},
  "tenantId": "dublin",
  "effectiveDate": "2024-01-01"
}
```

### 2. Income Inclusion Configuration

```mermaid
flowchart TB
    subgraph "Income Types"
        W2[W-2 Wages]
        C[Schedule C]
        E[Schedule E]
        F[Schedule F]
        G[W-2G Gambling]
        N[1099 Income]
    end

    subgraph "Configuration"
        INC{Include in<br/>Taxable Income?}
    end

    subgraph "Calculation"
        TAX[Tax Calculation]
    end

    W2 -->|Always| INC
    C -->|Configurable| INC
    E -->|Configurable| INC
    F -->|Configurable| INC
    G -->|Configurable| INC
    N -->|Configurable| INC
    
    INC --> TAX
```

**Configurable Parameters:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| `includeScheduleC` | true | Include self-employment income |
| `includeScheduleE` | true | Include rental/partnership income |
| `includeScheduleF` | true | Include farm income |
| `includeW2G` | true | Include gambling winnings |
| `include1099` | true | Include contractor income |

### 3. W-2 Qualifying Wages Configuration

```mermaid
graph LR
    subgraph "W-2 Boxes"
        B1[Box 1 - Federal Wages]
        B5[Box 5 - Medicare Wages]
        B18[Box 18 - Local Wages]
    end

    subgraph "Rule Selection"
        RULE{Qualifying<br/>Wages Rule}
    end

    subgraph "Methods"
        HIGH[HIGHEST_OF_ALL]
        FED[BOX_1_FEDERAL]
        MED[BOX_5_MEDICARE]
        LOC[BOX_18_LOCAL]
    end

    B1 --> RULE
    B5 --> RULE
    B18 --> RULE

    RULE --> HIGH
    RULE --> FED
    RULE --> MED
    RULE --> LOC
```

**Configuration:**
```json
{
  "ruleCode": "W2_QUALIFYING_WAGES_METHOD",
  "category": "INCOME_INCLUSION",
  "valueType": "ENUM",
  "value": {
    "option": "HIGHEST_OF_ALL",
    "allowedValues": [
      "HIGHEST_OF_ALL",
      "BOX_1_FEDERAL",
      "BOX_5_MEDICARE",
      "BOX_18_LOCAL"
    ]
  }
}
```

### 4. Allocation Factor Configuration (Business)

```mermaid
graph TB
    subgraph "3-Factor Formula"
        PROP[Property Factor]
        PAY[Payroll Factor]
        SALES[Sales Factor]
    end

    subgraph "Weighting Options"
        EQUAL[Equal Weight<br/>1:1:1]
        DOUBLE[Double Sales<br/>1:1:2]
        SALES_ONLY[Sales Only<br/>100%]
    end

    PROP --> EQUAL
    PAY --> EQUAL
    SALES --> EQUAL

    PROP --> DOUBLE
    PAY --> DOUBLE
    SALES --> DOUBLE

    SALES --> SALES_ONLY
```

**Configurable Parameters:**

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| `allocationMethod` | 3_FACTOR | 3_FACTOR, GROSS_RECEIPTS_ONLY | Apportionment method |
| `salesFactorWeight` | 1.0 | 1.0-3.0 | Weight applied to sales factor |

### 5. NOL (Net Operating Loss) Configuration

```mermaid
flowchart TB
    subgraph "NOL Settings"
        ENABLE[Enable NOL]
        CAP[Offset Cap %]
        YEARS[Carryforward Years]
    end

    subgraph "Values"
        E_VAL[true/false]
        C_VAL[0% - 100%]
        Y_VAL[0 - 20 years]
    end

    ENABLE --> E_VAL
    CAP --> C_VAL
    YEARS --> Y_VAL
```

**Configurable Parameters:**

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| `enableNOL` | true | boolean | Allow NOL deductions |
| `nolOffsetCapPercent` | 50% | 0-100% | Maximum income offset |
| `nolCarryforwardYears` | 20 | 5-20 | Years NOL can be carried forward |
| `nolCarrybackYears` | 0 | 0-2 | Years NOL can be carried back |

### 6. Penalty & Interest Configuration

```mermaid
graph TB
    subgraph "Penalty Types"
        LATE[Late Filing Penalty]
        UNDER[Underpayment Penalty]
        INT[Interest]
    end

    subgraph "Parameters"
        LATE_R[Rate per Month]
        LATE_M[Maximum %]
        LATE_MIN[Minimum Amount]
        UNDER_R[Penalty Rate]
        SAFE[Safe Harbor %]
        INT_R[Annual Rate]
    end

    LATE --> LATE_R
    LATE --> LATE_M
    LATE --> LATE_MIN
    UNDER --> UNDER_R
    UNDER --> SAFE
    INT --> INT_R
```

**Configurable Parameters:**

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| `lateFilingRatePerMonth` | 5% | 0-10% | Monthly late filing penalty |
| `lateFilingMaxPercent` | 25% | 0-50% | Maximum late penalty |
| `lateFilingMinAmount` | $50 | $0-$100 | Minimum late penalty |
| `underpaymentRate` | 15% | 0-25% | Underpayment penalty rate |
| `safeHarborPercent` | 90% | 80-100% | Threshold to avoid penalty |
| `interestRateAnnual` | 7% | 0-15% | Annual interest rate |

### 7. Validation Threshold Configuration

```mermaid
graph LR
    subgraph "Validation Rules"
        V1[W-2 Box Variance]
        V2[Max Withholding Rate]
        V3[Duplicate Detection]
        V4[AGI Variance]
    end

    subgraph "Thresholds"
        T1[20%]
        T2[3.0%]
        T3[$10]
        T4[10%]
    end

    V1 --> T1
    V2 --> T2
    V3 --> T3
    V4 --> T4
```

**Configurable Parameters:**

| Parameter | Default | Category | Description |
|-----------|---------|----------|-------------|
| `w2BoxVarianceThreshold` | 20% | VALIDATION | Max variance between Box 1 and 18 |
| `maxWithholdingRate` | 3.0% | VALIDATION | Maximum valid withholding rate |
| `duplicateWageThreshold` | $10 | VALIDATION | Threshold for duplicate W-2 detection |
| `agiVarianceThreshold` | 10% | VALIDATION | Max federal vs local income variance |
| `highWageThreshold` | $25,000 | VALIDATION | Threshold for zero withholding warning |

### 8. Filing Configuration

```mermaid
graph TB
    subgraph "Filing Options"
        ROUND[Rounding]
        FREQ[Filing Frequency]
        DUE[Due Date Rules]
    end

    subgraph "Settings"
        R_OPT[Whole Dollar / Cents]
        F_OPT[Daily / Monthly / Quarterly]
        D_OPT[Days After Period End]
    end

    ROUND --> R_OPT
    FREQ --> F_OPT
    DUE --> D_OPT
```

**Configurable Parameters:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| `enableRounding` | false | Round to whole dollars |
| `quarterlyDueDays` | 30 | Days after quarter end for due date |
| `monthlyDueDay` | 15 | Day of following month for monthly |
| `dailyDueNextBusiness` | true | Due next business day for daily |

---

## Multi-Tenant Configuration

### Tenant Hierarchy

```mermaid
graph TB
    subgraph "Default Rules"
        STATE[State-Level Defaults]
    end

    subgraph "Tenant Overrides"
        DUBLIN[Dublin]
        COLUMBUS[Columbus]
        WESTERVILLE[Westerville]
    end

    STATE -->|Inherit| DUBLIN
    STATE -->|Inherit| COLUMBUS
    STATE -->|Inherit| WESTERVILLE
    
    DUBLIN -->|Override| DUBLIN_RATE[Rate: 2.5%]
    COLUMBUS -->|Override| COLUMBUS_RATE[Rate: 2.0%]
    WESTERVILLE -->|Override| WEST_RATE[Rate: 2.25%]
```

### Tenant-Specific Configuration

```json
{
  "tenantId": "dublin",
  "configuration": {
    "taxRates": {
      "municipalRate": 0.025,
      "creditLimitRate": 0.025
    },
    "filing": {
      "enableRounding": false,
      "quarterlyDueDays": 30
    },
    "penalties": {
      "lateFilingRatePerMonth": 0.05,
      "underpaymentRate": 0.15
    }
  }
}
```

### Rule Precedence

```
1. Tenant-specific rule with exact date match
2. Tenant-specific rule with date range match
3. Default (NULL tenant) rule with date match
4. Hardcoded system default
```

---

## Temporal Configuration

### Effective Date Management

```mermaid
gantt
    title Tax Rate Changes Over Time
    dateFormat  YYYY-MM-DD
    section Dublin Rate
    2.0% Rate    :2020-01-01, 2023-12-31
    2.5% Rate    :2024-01-01, 2099-12-31
    section Credit Limit
    1.5% Limit   :2020-01-01, 2022-12-31
    2.0% Limit   :2023-01-01, 2023-12-31
    2.5% Limit   :2024-01-01, 2099-12-31
```

### Point-in-Time Queries

```java
// Get rules effective for tax year 2024
List<TaxRule> rules = temporalRuleService.getActiveRules(
    "dublin", 
    LocalDate.of(2024, 1, 1)
);

// Get historical rate for 2022 calculation
Optional<TaxRule> rate2022 = temporalRuleService.getActiveRuleByCode(
    "MUNICIPAL_TAX_RATE",
    "dublin",
    LocalDate.of(2022, 6, 15)
);
```

---

## Configuration via YAML

### Application Configuration

```yaml
# application.yml
munitax:
  tax:
    default-municipal-rate: 0.025
    default-credit-limit-rate: 0.025
    enable-rounding: false
    
  validation:
    w2-box-variance-threshold: 0.20
    max-withholding-rate: 0.03
    duplicate-wage-threshold: 10.0
    
  penalty:
    late-filing-rate-per-month: 0.05
    late-filing-max-percent: 0.25
    late-filing-min-amount: 50.0
    underpayment-rate: 0.15
    safe-harbor-percent: 0.90
    interest-rate-annual: 0.07
    
  business:
    allocation-method: 3_FACTOR
    sales-factor-weight: 1.0
    enable-nol: true
    nol-offset-cap-percent: 0.50
    minimum-tax: 0.0
```

### Environment Variable Override

```bash
# Override municipal rate
MUNITAX_TAX_DEFAULT_MUNICIPAL_RATE=0.03

# Override penalty rate
MUNITAX_PENALTY_LATE_FILING_RATE_PER_MONTH=0.10

# Override validation threshold
MUNITAX_VALIDATION_W2_BOX_VARIANCE_THRESHOLD=0.25
```

---

## Auditor Workflow Configuration

### Configurable Workflow Parameters

```mermaid
graph TB
    subgraph "Queue Management"
        PRIORITY[Priority Levels]
        AUTO[Auto-Assignment]
        THRESH[Risk Thresholds]
    end

    subgraph "Review Settings"
        APPROVAL[Approval Limits]
        TIMEOUT[Review Timeout]
        DOCS[Document Deadlines]
    end

    subgraph "Audit Rules"
        RISK[Risk Scoring]
        FLAG[Auto-Flag Rules]
        ESCAL[Escalation Rules]
    end
```

**Configurable Parameters:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| `autoAssignEnabled` | false | Automatically assign returns |
| `seniorApprovalLimit` | $50,000 | Max amount senior auditor can approve |
| `documentRequestDays` | 30 | Default deadline for document requests |
| `reviewTimeoutDays` | 14 | Days before review times out |
| `highRiskThreshold` | 60 | Score above which is HIGH risk |
| `mediumRiskThreshold` | 20 | Score above which is MEDIUM risk |

---

## UI Configuration

### Display Settings

| Parameter | Default | Description |
|-----------|---------|-------------|
| `showConfidenceScores` | true | Display AI extraction confidence |
| `allowManualOverride` | true | Allow editing extracted data |
| `showDiscrepancyWarnings` | true | Display validation warnings |
| `resultsDecimalPlaces` | 2 | Decimal places for currency |

### Theme Configuration

```json
{
  "branding": {
    "primaryColor": "#1e40af",
    "logo": "/assets/dublin-logo.png",
    "municipalityName": "City of Dublin"
  },
  "features": {
    "enableDarkMode": true,
    "enablePdfPreview": true,
    "enableBulkActions": false
  }
}
```

---

## Configuration Best Practices

### 1. Rule Management

- **Version all changes**: Every rule change creates a new version
- **Document reasons**: Always provide change reason and ordinance reference
- **Test before production**: Use staging tenant to test new rules
- **Plan effective dates**: Schedule rule changes for tax year boundaries

### 2. Multi-Tenant Setup

- **Start with defaults**: Define sensible state-level defaults
- **Override minimally**: Only override what's different per tenant
- **Test isolation**: Verify tenant rules don't affect other tenants

### 3. Temporal Rules

- **No overlaps**: Ensure date ranges don't overlap for same rule code
- **Plan transitions**: Schedule new rules well before effective date
- **Keep history**: Don't delete old rules, let them expire

### 4. Validation Thresholds

- **Balance strictness**: Too strict = false positives, too loose = missed errors
- **Monitor metrics**: Track discrepancy detection rates
- **Adjust based on feedback**: Use auditor feedback to tune thresholds

---

## Configuration API Reference

### Get Current Configuration

```http
GET /api/v1/configuration?tenantId=dublin
Authorization: Bearer {token}
```

### Update Configuration

```http
PUT /api/v1/configuration
Content-Type: application/json
Authorization: Bearer {token}

{
  "tenantId": "dublin",
  "setting": "MUNICIPAL_TAX_RATE",
  "value": 0.025,
  "effectiveDate": "2025-01-01",
  "changeReason": "Annual rate adjustment"
}
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-01 | Initial configurable design documentation |

---

**Document Owner:** Development Team  
**Last Updated:** December 1, 2025
