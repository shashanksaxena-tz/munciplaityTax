# Data Model: Dynamic Rule Configuration System

**Date**: 2025-11-28  
**Phase**: 1 - Design & Contracts  
**Status**: Complete

## Overview

This document defines the data model for the Dynamic Rule Configuration System. The model supports temporal rules (effective dating), multi-tenant isolation, entity-specific targeting, version control, and flexible value types (numbers, formulas, conditionals).

---

## Entity Relationship Diagram

```
┌─────────────────────────────────────┐
│          TaxRule (Core Entity)      │
├─────────────────────────────────────┤
│ PK: rule_id (UUID)                  │
│     rule_code (VARCHAR)             │◄───┐
│     rule_name (VARCHAR)             │    │
│     category (ENUM)                 │    │
│     value_type (ENUM)               │    │ Links to previous version
│     value (JSONB)                   │    │ (version chain)
│     effective_date (DATE)           │    │
│     end_date (DATE, nullable)       │    │
│     tenant_id (VARCHAR)             │    │
│     entity_types (VARCHAR[])        │    │
│     applies_to (TEXT, nullable)     │    │
│     version (INT)                   │    │
│     previous_version_id (UUID) ─────┘    │
│     depends_on (UUID[])             │    │
│     approval_status (ENUM)          │    │
│     approved_by (VARCHAR, nullable) │    │
│     approval_date (TIMESTAMP, null) │    │
│     created_by (VARCHAR)            │    │
│     created_date (TIMESTAMP)        │    │
│     modified_by (VARCHAR, nullable) │    │
│     modified_date (TIMESTAMP, null) │    │
│     change_reason (TEXT)            │    │
│     ordinance_reference (TEXT)      │    │
└─────────────────────────────────────┘    │
            │                               │
            │ 1:N                           │
            ▼                               │
┌─────────────────────────────────────┐    │
│   RuleChangeLog (Audit Trail)       │    │
├─────────────────────────────────────┤    │
│ PK: log_id (UUID)                   │    │
│ FK: rule_id (UUID)                  │────┘
│     change_type (ENUM)              │
│     old_value (JSONB, nullable)     │
│     new_value (JSONB)               │
│     changed_fields (VARCHAR[])      │
│     changed_by (VARCHAR)            │
│     change_date (TIMESTAMP)         │
│     change_reason (TEXT)            │
│     affected_returns_count (INT)    │
│     impact_estimate (JSONB)         │
└─────────────────────────────────────┘
```

---

## Core Entities

### 1. TaxRule

Primary entity representing a single tax rule configuration.

#### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| **rule_id** | UUID | PRIMARY KEY, NOT NULL | Unique identifier for rule |
| **rule_code** | VARCHAR(100) | NOT NULL, INDEX | System code used in calculations (e.g., "MUNICIPAL_RATE") |
| **rule_name** | VARCHAR(255) | NOT NULL | Human-readable name (e.g., "Municipal Tax Rate") |
| **category** | VARCHAR(50) | NOT NULL, CHECK constraint | Category: "TaxRates", "IncomeInclusion", "Deductions", "Penalties", "Filing", "Allocation", "Withholding", "Validation" |
| **value_type** | VARCHAR(50) | NOT NULL, CHECK constraint | Type: "NUMBER", "PERCENTAGE", "ENUM", "BOOLEAN", "FORMULA", "CONDITIONAL" |
| **value** | JSONB | NOT NULL | Actual rule value (structure depends on value_type) |
| **effective_date** | DATE | NOT NULL, INDEX | Date when rule becomes active (inclusive) |
| **end_date** | DATE | NULLABLE, INDEX | Date when rule expires (inclusive). NULL = active indefinitely |
| **tenant_id** | VARCHAR(50) | NOT NULL, INDEX | Tenant identifier ("dublin", "columbus", "GLOBAL") |
| **entity_types** | VARCHAR[] | NOT NULL, DEFAULT ['ALL'] | Entity targeting: ["ALL"], ["C-CORP"], ["PARTNERSHIP", "S-CORP"], etc. |
| **applies_to** | TEXT | NULLABLE | Additional targeting criteria (e.g., "income > 1000000") |
| **version** | INTEGER | NOT NULL, DEFAULT 1 | Version number (increments with each change) |
| **previous_version_id** | UUID | NULLABLE, FOREIGN KEY → rule_id | Links to previous version (forms version chain) |
| **depends_on** | UUID[] | NULLABLE | Array of rule_ids this rule references (for formula dependencies) |
| **approval_status** | VARCHAR(20) | NOT NULL, INDEX, DEFAULT 'PENDING' | Status: "PENDING", "APPROVED", "REJECTED", "VOIDED" |
| **approved_by** | VARCHAR(100) | NULLABLE | User ID of approver (NULL if PENDING) |
| **approval_date** | TIMESTAMP | NULLABLE | Timestamp of approval (NULL if PENDING) |
| **created_by** | VARCHAR(100) | NOT NULL | User ID of creator |
| **created_date** | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| **modified_by** | VARCHAR(100) | NULLABLE | User ID of last modifier |
| **modified_date** | TIMESTAMP | NULLABLE | Last modification timestamp |
| **change_reason** | TEXT | NOT NULL | Reason for change (required for audit trail) |
| **ordinance_reference** | TEXT | NULLABLE | Legal citation (e.g., "Ordinance 2022-45") |

#### Indexes

```sql
-- Primary key index (automatic)
CREATE UNIQUE INDEX idx_tax_rules_pk ON tax_rules(rule_id);

-- Temporal query optimization (most common query)
CREATE INDEX idx_tax_rules_temporal ON tax_rules(tenant_id, effective_date, end_date)
WHERE approval_status = 'APPROVED';

-- Rule code lookup (for dependency resolution)
CREATE INDEX idx_tax_rules_code ON tax_rules(rule_code, tenant_id, approval_status);

-- Approval workflow queries
CREATE INDEX idx_tax_rules_approval ON tax_rules(approval_status, created_date);

-- Version chain traversal
CREATE INDEX idx_tax_rules_version_chain ON tax_rules(previous_version_id)
WHERE previous_version_id IS NOT NULL;
```

#### Constraints

```sql
-- Check category values
ALTER TABLE tax_rules ADD CONSTRAINT chk_category 
CHECK (category IN ('TaxRates', 'IncomeInclusion', 'Deductions', 'Penalties', 'Filing', 'Allocation', 'Withholding', 'Validation'));

-- Check value_type values
ALTER TABLE tax_rules ADD CONSTRAINT chk_value_type 
CHECK (value_type IN ('NUMBER', 'PERCENTAGE', 'ENUM', 'BOOLEAN', 'FORMULA', 'CONDITIONAL'));

-- Check approval_status values
ALTER TABLE tax_rules ADD CONSTRAINT chk_approval_status 
CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'VOIDED'));

-- Ensure effective_date <= end_date (if end_date present)
ALTER TABLE tax_rules ADD CONSTRAINT chk_date_range 
CHECK (end_date IS NULL OR effective_date <= end_date);

-- Ensure approved rules have approver
ALTER TABLE tax_rules ADD CONSTRAINT chk_approval_complete 
CHECK (
  (approval_status = 'APPROVED' AND approved_by IS NOT NULL AND approval_date IS NOT NULL) OR
  (approval_status != 'APPROVED')
);

-- Prevent self-approval (enforced at application layer, not DB constraint)
-- CHECK (created_by != approved_by)  -- Commented: Not enforceable at DB level due to NULL handling
```

#### Row-Level Security (RLS)

```sql
-- Enable RLS on tax_rules table
ALTER TABLE tax_rules ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only access their tenant's rules (or GLOBAL rules)
CREATE POLICY tenant_isolation ON tax_rules
FOR ALL
USING (
  tenant_id = current_setting('app.tenant_id', true) OR 
  tenant_id = 'GLOBAL'
);

-- Policy: Only TAX_ADMINISTRATOR role can modify rules
CREATE POLICY admin_write_only ON tax_rules
FOR INSERT, UPDATE, DELETE
USING (current_setting('app.user_role', true) = 'TAX_ADMINISTRATOR');
```

#### Value Field Structure (JSONB)

The `value` field uses different JSON structures based on `value_type`:

**NUMBER**:
```json
{
  "scalar": 0.02
}
```

**PERCENTAGE**:
```json
{
  "scalar": 50,
  "unit": "percent"
}
```

**ENUM**:
```json
{
  "option": "BOX_5_MEDICARE",
  "allowedValues": ["HIGHEST_OF_ALL", "BOX_5_MEDICARE", "BOX_18_LOCAL", "BOX_1_FEDERAL"]
}
```

**BOOLEAN**:
```json
{
  "flag": true
}
```

**FORMULA**:
```json
{
  "expression": "wages * municipalRate",
  "variables": ["wages", "municipalRate"],
  "returnType": "number"
}
```

**CONDITIONAL**:
```json
{
  "condition": "income > 1000000",
  "thenValue": 5000,
  "elseValue": 50,
  "returnType": "number"
}
```

---

### 2. RuleChangeLog

Immutable audit trail for all rule changes.

#### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| **log_id** | UUID | PRIMARY KEY, NOT NULL | Unique identifier for log entry |
| **rule_id** | UUID | NOT NULL, INDEX, FOREIGN KEY → tax_rules(rule_id) | Reference to modified rule |
| **change_type** | VARCHAR(20) | NOT NULL | Type: "CREATE", "UPDATE", "DELETE", "APPROVE", "REJECT", "VOID", "ROLLBACK" |
| **old_value** | JSONB | NULLABLE | Complete rule state before change (NULL for CREATE) |
| **new_value** | JSONB | NOT NULL | Complete rule state after change |
| **changed_fields** | VARCHAR[] | NOT NULL | List of field names that changed (e.g., ["value", "effective_date"]) |
| **changed_by** | VARCHAR(100) | NOT NULL | User ID who made the change |
| **change_date** | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP, INDEX | When change occurred |
| **change_reason** | TEXT | NOT NULL | Explanation for change (required) |
| **affected_returns_count** | INTEGER | DEFAULT 0 | Number of tax returns filed using previous version (for impact analysis) |
| **impact_estimate** | JSONB | NULLABLE | Estimated impact of change (optional) |

#### Impact Estimate Structure (JSONB)

```json
{
  "totalAffectedTaxpayers": 5000,
  "avgTaxIncrease": 40.50,
  "avgTaxDecrease": 0,
  "maxImpact": 150.00,
  "minImpact": 10.00,
  "medianImpact": 38.25
}
```

#### Indexes

```sql
-- Primary key index (automatic)
CREATE UNIQUE INDEX idx_rule_change_log_pk ON rule_change_log(log_id);

-- Query by rule (get history for specific rule)
CREATE INDEX idx_rule_change_log_rule ON rule_change_log(rule_id, change_date DESC);

-- Query by date range (audit reports)
CREATE INDEX idx_rule_change_log_date ON rule_change_log(change_date DESC);

-- Query by user (who changed what)
CREATE INDEX idx_rule_change_log_user ON rule_change_log(changed_by, change_date DESC);
```

#### Constraints

```sql
-- Check change_type values
ALTER TABLE rule_change_log ADD CONSTRAINT chk_change_type 
CHECK (change_type IN ('CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT', 'VOID', 'ROLLBACK'));

-- No updates or deletes allowed (append-only)
-- Enforced via PostgreSQL trigger
CREATE OR REPLACE FUNCTION prevent_rule_change_log_modification()
RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'rule_change_log is append-only. UPDATE and DELETE are not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER no_update_rule_change_log
BEFORE UPDATE OR DELETE ON rule_change_log
FOR EACH ROW EXECUTE FUNCTION prevent_rule_change_log_modification();
```

---

## Java Entity Classes

### TaxRule.java

```java
package com.munitax.rules.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tax_rules")
public class TaxRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rule_id")
    private UUID ruleId;
    
    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;
    
    @Column(name = "rule_name", nullable = false)
    private String ruleName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private RuleCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false)
    private RuleValueType valueType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value", nullable = false, columnDefinition = "jsonb")
    private Object value;  // Polymorphic: can be Number, String, Map, etc.
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "entity_types", nullable = false)
    private List<String> entityTypes;
    
    @Column(name = "applies_to")
    private String appliesTo;
    
    @Column(name = "version", nullable = false)
    private Integer version = 1;
    
    @Column(name = "previous_version_id")
    private UUID previousVersionId;
    
    @Column(name = "depends_on")
    private List<UUID> dependsOn;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @Column(name = "approval_date")
    private LocalDateTime approvalDate;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
    
    @Column(name = "modified_by")
    private String modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    @Column(name = "change_reason", nullable = false)
    private String changeReason;
    
    @Column(name = "ordinance_reference")
    private String ordinanceReference;
    
    // Constructors, getters, setters, equals, hashCode omitted for brevity
}
```

### RuleChangeLog.java

```java
package com.munitax.rules.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rule_change_log")
public class RuleChangeLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;
    
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private Object oldValue;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", nullable = false, columnDefinition = "jsonb")
    private Object newValue;
    
    @Column(name = "changed_fields", nullable = false)
    private List<String> changedFields;
    
    @Column(name = "changed_by", nullable = false)
    private String changedBy;
    
    @Column(name = "change_date", nullable = false)
    private LocalDateTime changeDate = LocalDateTime.now();
    
    @Column(name = "change_reason", nullable = false)
    private String changeReason;
    
    @Column(name = "affected_returns_count")
    private Integer affectedReturnsCount = 0;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "impact_estimate", columnDefinition = "jsonb")
    private Object impactEstimate;
    
    // Constructors, getters, setters omitted for brevity
}
```

---

## Enumerations

### RuleCategory

```java
public enum RuleCategory {
    TAX_RATES("TaxRates"),
    INCOME_INCLUSION("IncomeInclusion"),
    DEDUCTIONS("Deductions"),
    PENALTIES("Penalties"),
    FILING("Filing"),
    ALLOCATION("Allocation"),
    WITHHOLDING("Withholding"),
    VALIDATION("Validation");
    
    private final String displayName;
    
    RuleCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

### RuleValueType

```java
public enum RuleValueType {
    NUMBER,
    PERCENTAGE,
    ENUM,
    BOOLEAN,
    FORMULA,
    CONDITIONAL
}
```

### ApprovalStatus

```java
public enum ApprovalStatus {
    PENDING,    // Rule created, awaiting approval
    APPROVED,   // Rule approved, active on effective date
    REJECTED,   // Rule rejected, cannot be modified
    VOIDED      // Rule withdrawn before effective date
}
```

### ChangeType

```java
public enum ChangeType {
    CREATE,     // New rule created
    UPDATE,     // Existing rule modified
    DELETE,     // Rule soft-deleted (status → VOIDED)
    APPROVE,    // Rule approved
    REJECT,     // Rule rejected
    VOID,       // Rule voided
    ROLLBACK    // Rule rolled back to previous version
}
```

---

## TypeScript Interfaces (Frontend)

```typescript
// types.ts

export interface TaxRule {
  ruleId: string;
  ruleCode: string;
  ruleName: string;
  category: RuleCategory;
  valueType: RuleValueType;
  value: RuleValue;
  effectiveDate: string;  // ISO 8601 date
  endDate?: string;       // ISO 8601 date
  tenantId: string;
  entityTypes: string[];
  appliesTo?: string;
  version: number;
  previousVersionId?: string;
  dependsOn?: string[];
  approvalStatus: ApprovalStatus;
  approvedBy?: string;
  approvalDate?: string;  // ISO 8601 timestamp
  createdBy: string;
  createdDate: string;    // ISO 8601 timestamp
  modifiedBy?: string;
  modifiedDate?: string;  // ISO 8601 timestamp
  changeReason: string;
  ordinanceReference?: string;
}

export type RuleCategory = 
  | 'TaxRates' 
  | 'IncomeInclusion' 
  | 'Deductions' 
  | 'Penalties' 
  | 'Filing' 
  | 'Allocation' 
  | 'Withholding' 
  | 'Validation';

export type RuleValueType = 
  | 'NUMBER' 
  | 'PERCENTAGE' 
  | 'ENUM' 
  | 'BOOLEAN' 
  | 'FORMULA' 
  | 'CONDITIONAL';

export type ApprovalStatus = 
  | 'PENDING' 
  | 'APPROVED' 
  | 'REJECTED' 
  | 'VOIDED';

export type RuleValue = 
  | NumberValue 
  | PercentageValue 
  | EnumValue 
  | BooleanValue 
  | FormulaValue 
  | ConditionalValue;

export interface NumberValue {
  scalar: number;
}

export interface PercentageValue {
  scalar: number;
  unit: 'percent';
}

export interface EnumValue {
  option: string;
  allowedValues: string[];
}

export interface BooleanValue {
  flag: boolean;
}

export interface FormulaValue {
  expression: string;
  variables: string[];
  returnType: 'number' | 'string' | 'boolean';
}

export interface ConditionalValue {
  condition: string;
  thenValue: any;
  elseValue: any;
  returnType: 'number' | 'string' | 'boolean';
}

export interface RuleChangeLog {
  logId: string;
  ruleId: string;
  changeType: ChangeType;
  oldValue?: any;
  newValue: any;
  changedFields: string[];
  changedBy: string;
  changeDate: string;  // ISO 8601 timestamp
  changeReason: string;
  affectedReturnsCount: number;
  impactEstimate?: ImpactEstimate;
}

export type ChangeType = 
  | 'CREATE' 
  | 'UPDATE' 
  | 'DELETE' 
  | 'APPROVE' 
  | 'REJECT' 
  | 'VOID' 
  | 'ROLLBACK';

export interface ImpactEstimate {
  totalAffectedTaxpayers: number;
  avgTaxIncrease: number;
  avgTaxDecrease: number;
  maxImpact: number;
  minImpact: number;
  medianImpact: number;
}
```

---

## Validation Rules

### Business Logic Validation

1. **Temporal Overlap Detection**:
   - Before creating/updating rule, query existing rules with same `rule_code + tenant_id`
   - Check if new `[effective_date, end_date]` overlaps with any existing `[effective_date, end_date]`
   - Overlap exists if: `new_start <= existing_end AND new_end >= existing_start`
   - Reject if overlap detected (exception: can create PENDING rule with overlapping date if it will replace APPROVED rule)

2. **Formula Variable Validation**:
   - Parse formula expression to extract variable names (using SpEL AST)
   - Verify all variables exist in allowed variable list (defined per category)
   - Example: "TaxRates" category allows ["wages", "income", "municipalRate", "creditRate"]
   - Reject if unknown variable referenced

3. **Conditional Logic Validation**:
   - Parse condition expression (e.g., "income > 1000000")
   - Verify left-hand side is known variable
   - Verify operator is allowed (`>`, `<`, `>=`, `<=`, `==`, `!=`)
   - Verify right-hand side is literal or known variable
   - Reject if invalid syntax

4. **Self-Approval Prevention**:
   - When approving rule, verify `approved_by != created_by`
   - Reject if same user tries to approve their own rule
   - Exception: If only one admin user in system (for initial setup), allow self-approval with warning

5. **Retroactive Change Prevention**:
   - When updating rule, check if `effective_date < TODAY`
   - If yes, check if any tax returns filed using this rule
   - If yes, reject update with error "Cannot modify rule retroactively - create new version with future effective date"

---

## Migration Strategy

### Step 1: Create New Tables

Run Flyway migration scripts in rule-service:

1. `V1__create_tax_rules_table.sql` - Create tax_rules table with indexes
2. `V2__create_rule_change_log_table.sql` - Create rule_change_log table with triggers
3. `V3__create_tenant_rls_policies.sql` - Enable RLS and create policies

### Step 2: Seed Initial Rules

Run migration script `V4__migrate_existing_rules_from_constants.sql`:

```sql
-- Example: Migrate Dublin municipal rate from constants.ts
INSERT INTO tax_rules (
  rule_id, rule_code, rule_name, category, value_type, value,
  effective_date, end_date, tenant_id, entity_types,
  approval_status, approved_by, approval_date,
  created_by, created_date, change_reason, ordinance_reference
) VALUES (
  gen_random_uuid(),
  'MUNICIPAL_RATE',
  'Municipal Tax Rate',
  'TaxRates',
  'PERCENTAGE',
  '{"scalar": 2.0, "unit": "percent"}'::jsonb,
  '2020-01-01',
  NULL,
  'dublin',
  ARRAY['ALL'],
  'APPROVED',
  'system',
  NOW(),
  'system',
  NOW(),
  'Initial migration from constants.ts',
  'Historical rate'
);

-- Repeat for all tenants and all rule types...
```

### Step 3: Update Tax Calculators

Modify `IndividualTaxCalculator.java` and `BusinessTaxCalculator.java`:

```java
// BEFORE (hardcoded)
public TaxCalculationResult calculate(TaxFormData formData) {
    double municipalRate = DEFAULT_INDIVIDUAL_RULES.municipalRate();
    // ...
}

// AFTER (dynamic)
public TaxCalculationResult calculate(TaxFormData formData) {
    TaxRulesConfig rules = ruleServiceClient.getActiveRules(
        formData.getTenantId(),
        formData.getTaxYear(),
        EntityType.INDIVIDUAL
    );
    double municipalRate = rules.municipalRate();
    // ...
}
```

### Step 4: Verification Tests

Create integration tests to verify calculations match before/after migration:

```java
@Test
public void testCalculationConsistencyAfterMigration() {
    // Create sample form data
    TaxFormData formData = createSampleFormData();
    
    // Calculate with OLD hardcoded rules
    TaxCalculationResult oldResult = oldCalculator.calculate(formData);
    
    // Calculate with NEW dynamic rules (database)
    TaxCalculationResult newResult = newCalculator.calculate(formData);
    
    // Assert identical results
    assertEquals(oldResult.getTotalTax(), newResult.getTotalTax(), 0.01);
    assertEquals(oldResult.getMunicipalTax(), newResult.getMunicipalTax(), 0.01);
    // ... assert all fields match
}
```

---

## Phase 1 Completion Checklist

- [x] TaxRule entity defined with all required fields
- [x] RuleChangeLog entity defined for audit trail
- [x] Enumerations defined (RuleCategory, RuleValueType, ApprovalStatus, ChangeType)
- [x] JSONB value structures documented per value_type
- [x] Database indexes optimized for temporal queries
- [x] Database constraints ensure data integrity
- [x] Row-Level Security policies for multi-tenant isolation
- [x] Java entity classes with JPA annotations
- [x] TypeScript interfaces for frontend
- [x] Validation rules documented
- [x] Migration strategy defined

**Next Deliverables**: OpenAPI contracts (contracts/) and quickstart.md
