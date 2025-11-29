# Data Model: Enhanced Discrepancy Detection

**Feature**: Enhanced Discrepancy Detection (10+ Validation Rules)  
**Date**: 2025-11-27

## Overview

This document defines the data structures for comprehensive tax return validation covering W-2 box consistency, Schedule C/E/F validations, K-1 allocation checks, municipal credit limits, federal/local reconciliation, duplicate detection, and cross-year carryforward verification.

## Entities

### 1. DiscrepancyReport

**Purpose**: Root entity containing complete validation results for a tax return.

**Lifecycle**: Created when user clicks "Review & Submit" button. Persisted with return submission for audit trail. Retained for 7 years per IRS requirements.

**Relationships**:
- Has many `DiscrepancyIssue` (1:N)
- Belongs to one `TaxReturn` (1:1)

**Fields**:

| Field | Type | Required | Description | Validation Rules |
|-------|------|----------|-------------|-----------------|
| id | UUID | Yes | Primary key | Auto-generated |
| taxReturnId | UUID | Yes | Foreign key to tax return | Must reference valid tax return |
| hasDiscrepancies | boolean | Yes | True if any issues found | Computed: issues.length > 0 |
| issues | List<DiscrepancyIssue> | Yes | All detected issues | Empty list if no issues |
| summary | DiscrepancySummary | Yes | Aggregate counts | Computed from issues |
| validationDate | Timestamp | Yes | When validation ran | ISO 8601 format |
| validationRulesVersion | String | Yes | Version of rules applied | Semantic version (e.g., "1.0.0") or Git SHA |
| tenantId | UUID | Yes | Municipality identifier | Inherited from tax return |
| createdBy | UUID | Yes | User who triggered validation | From JWT token |

**Example**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "taxReturnId": "123e4567-e89b-12d3-a456-426614174000",
  "hasDiscrepancies": true,
  "issues": [...],
  "summary": {
    "totalIssues": 5,
    "highSeverityCount": 2,
    "mediumSeverityCount": 2,
    "lowSeverityCount": 1,
    "acceptedIssuesCount": 1,
    "blocksFiling": true
  },
  "validationDate": "2025-11-27T14:30:00Z",
  "validationRulesVersion": "1.0.0",
  "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "createdBy": "user-uuid-here"
}
```

---

### 2. DiscrepancyIssue

**Purpose**: Individual validation failure or warning.

**Lifecycle**: Created during validation run. Can be accepted by user (state change). Immutable after acceptance.

**Relationships**:
- Belongs to one `DiscrepancyReport` (N:1)

**Fields**:

| Field | Type | Required | Description | Validation Rules |
|-------|------|----------|-------------|-----------------|
| issueId | String | Yes | Unique identifier for tracking | UUID string |
| ruleId | String | Yes | Validation rule that triggered | Enum: FR-001 through FR-022 |
| category | String | Yes | Issue category for grouping | Enum: W-2, Schedule C, Schedule E, K-1, Municipal Credit, Federal Reconciliation, Carryforward |
| field | String | Yes | Field name that failed validation | Human-readable (e.g., "W-2 Box 18 Local Wages") |
| sourceValue | Decimal | No | Calculated/expected value | Nullable for non-numeric validations |
| formValue | Decimal | No | Value entered by user | Nullable for non-numeric validations |
| difference | Decimal | No | sourceValue - formValue | Nullable, can be negative |
| differencePercent | Decimal | No | (difference / sourceValue) × 100 | Nullable, rounded to 2 decimals |
| severity | String | Yes | Impact level | Enum: HIGH, MEDIUM, LOW |
| message | String | Yes | User-friendly explanation | Max 500 characters |
| recommendedAction | String | Yes | What user should do to fix | Max 500 characters |
| isAccepted | boolean | Yes | User acknowledged warning | Default: false |
| acceptanceNote | String | No | User's explanation for accepting | Max 1000 characters, required if isAccepted = true |
| acceptedDate | Timestamp | No | When user accepted | ISO 8601, required if isAccepted = true |
| acceptedBy | UUID | No | User who accepted | From JWT, required if isAccepted = true |

**Validation Rules**:
- `severity = HIGH` AND `isAccepted = false` → blocks filing submission
- If `isAccepted = true`, then `acceptanceNote`, `acceptedDate`, and `acceptedBy` must be set
- `sourceValue` and `formValue` are nullable (some rules don't have numeric comparisons, e.g., "duplicate W-2 detected")

**Example**:
```json
{
  "issueId": "issue-001",
  "ruleId": "FR-001",
  "category": "W-2",
  "field": "W-2 Box 18 Local Wages",
  "sourceValue": 75000.00,
  "formValue": 7500.00,
  "difference": 67500.00,
  "differencePercent": 90.0,
  "severity": "HIGH",
  "message": "Box 18 is 90% lower than Box 1 - verify data entry",
  "recommendedAction": "Verify you entered Box 18 correctly. For full-year Dublin employment, Box 18 should be similar to Box 1.",
  "isAccepted": false,
  "acceptanceNote": null,
  "acceptedDate": null,
  "acceptedBy": null
}
```

**State Transitions**:
```
[Created] → severity determines initial state
  ├─ HIGH + !isAccepted → BLOCKS_FILING
  ├─ MEDIUM + !isAccepted → WARNS_USER
  └─ LOW → INFORMATIONAL

[User Action] → isAccepted = true (with note)
  ├─ HIGH → still BLOCKS_FILING (HIGH cannot be accepted, must be fixed)
  ├─ MEDIUM → ACCEPTED_WARNING
  └─ LOW → ACKNOWLEDGED
```

**Note**: HIGH severity issues cannot be "accepted"—they must be fixed. UI should disable "Accept" button for HIGH severity.

---

### 3. DiscrepancySummary

**Purpose**: Aggregate statistics for quick assessment.

**Lifecycle**: Computed from `issues` array whenever `DiscrepancyReport` is loaded.

**Fields**:

| Field | Type | Required | Description | Calculation |
|-------|------|----------|-------------|-------------|
| totalIssues | int | Yes | Total count of issues | issues.length |
| highSeverityCount | int | Yes | Count of HIGH severity | issues.filter(i => i.severity === 'HIGH').length |
| mediumSeverityCount | int | Yes | Count of MEDIUM severity | issues.filter(i => i.severity === 'MEDIUM').length |
| lowSeverityCount | int | Yes | Count of LOW severity | issues.filter(i => i.severity === 'LOW').length |
| acceptedIssuesCount | int | Yes | Count of accepted issues | issues.filter(i => i.isAccepted).length |
| blocksFiling | boolean | Yes | Has unresolved HIGH issues | issues.some(i => i.severity === 'HIGH' && !i.isAccepted) |

**Note**: This is a computed object, not a separate database entity.

---

### 4. ValidationRule (Enumeration)

**Purpose**: Standardize rule identifiers for tracking and versioning.

**Values**:

| Rule ID | Description | Severity | Category |
|---------|-------------|----------|----------|
| FR-001 | W-2 Box 18 variance from Box 1 | HIGH (>20%), MEDIUM (10-20%) | W-2 |
| FR-002 | W-2 withholding rate validation | MEDIUM | W-2 |
| FR-003 | Duplicate W-2 detection | HIGH | W-2 |
| FR-004 | W-2 employer jurisdiction check | MEDIUM | W-2 |
| FR-005 | W-2 corrected vs duplicate flag | HIGH | W-2 |
| FR-006 | Schedule C/E/F estimated tax check | MEDIUM | Schedule C |
| FR-007 | Schedule E property count validation | MEDIUM | Schedule E |
| FR-008 | Schedule E jurisdiction check | MEDIUM | Schedule E |
| FR-009 | Schedule E passive loss limits | LOW | Schedule E |
| FR-010 | Schedule C vs 1099-K reconciliation | MEDIUM | Schedule C |
| FR-011 | K-1 component completeness | MEDIUM | K-1 |
| FR-012 | K-1 profit share allocation | MEDIUM | K-1 |
| FR-013 | K-1 municipal adjustment items | LOW | K-1 |
| FR-014 | Municipal credit exceeds liability | HIGH | Municipal Credit |
| FR-015 | Municipal credit order of application | MEDIUM | Municipal Credit |
| FR-016 | Municipal credit percentage limits | MEDIUM | Municipal Credit |
| FR-017 | Federal AGI reconciliation | MEDIUM | Federal Reconciliation |
| FR-018 | Non-taxable income identification | LOW | Federal Reconciliation |
| FR-019 | Federal wages vs W-2 sum | MEDIUM | Federal Reconciliation |
| FR-020 | Carryforward source verification | HIGH | Carryforward |
| FR-021 | Safe harbor prior year query | MEDIUM | Carryforward |
| FR-022 | Multi-year NOL tracking | MEDIUM | Carryforward |

**Usage**: 
- Frontend can filter issues by `ruleId` to show category-specific views
- Rule versioning: `validationRulesVersion` tracks which rule set was applied
- Auditor reports can reference specific FR codes for clarity

---

### 5. PriorYearData (Lookup Entity)

**Purpose**: Carryforward amounts from prior year return for cross-year validation.

**Lifecycle**: Queried from prior year tax return when validating FR-020 through FR-022. Not stored separately—derived from existing `TaxReturn` entity.

**Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| taxYear | int | Yes | Prior year (e.g., 2023) |
| ssn | String | Yes | Taxpayer SSN (encrypted) |
| tenantId | UUID | Yes | Municipality |
| nolAmount | Decimal | No | Net Operating Loss generated |
| suspendedPassiveLosses | Decimal | No | From Form 8582 |
| overpaymentCredit | Decimal | No | Amount elected for next year |
| totalTaxLiability | Decimal | Yes | For safe harbor calculation |

**Query Pattern**:
```sql
SELECT 
  nol_amount, 
  suspended_passive_losses, 
  overpayment_credit, 
  total_tax_liability
FROM tax_returns
WHERE ssn = :ssn 
  AND tenant_id = :tenant_id 
  AND tax_year = :prior_year
LIMIT 1;
```

**Note**: If query returns no rows (prior year return not in system), validation generates MEDIUM severity informational warning instead of HIGH severity error.

---

## Relationships Diagram

```
TaxReturn (1) ──────┬─── (1) DiscrepancyReport
                    │
                    └─── (N) W2Form
                    │
                    └─── (N) ScheduleCForm
                    │
                    └─── (N) ScheduleEForm
                    │
                    └─── (N) K1Form
                    │
                    └─── (1) ScheduleYForm (credits)


DiscrepancyReport (1) ──── (N) DiscrepancyIssue


DiscrepancySummary ←─── computed from ──── DiscrepancyIssue[]


PriorYearTaxReturn ←─── queried via ──── PriorYearReturnService
```

---

## Database Schema

### Table: `discrepancy_reports`

```sql
CREATE TABLE discrepancy_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tax_return_id UUID NOT NULL REFERENCES tax_returns(id) ON DELETE CASCADE,
    has_discrepancies BOOLEAN NOT NULL DEFAULT false,
    validation_date TIMESTAMP NOT NULL DEFAULT NOW(),
    validation_rules_version VARCHAR(50) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_discrepancy_reports_tax_return ON discrepancy_reports(tax_return_id);
CREATE INDEX idx_discrepancy_reports_tenant ON discrepancy_reports(tenant_id);
CREATE INDEX idx_discrepancy_reports_created_by ON discrepancy_reports(created_by);
```

### Table: `discrepancy_issues`

```sql
CREATE TABLE discrepancy_issues (
    issue_id VARCHAR(100) PRIMARY KEY,
    discrepancy_report_id UUID NOT NULL REFERENCES discrepancy_reports(id) ON DELETE CASCADE,
    rule_id VARCHAR(20) NOT NULL, -- FR-001, FR-002, etc.
    category VARCHAR(50) NOT NULL, -- W-2, Schedule C, etc.
    field VARCHAR(255) NOT NULL,
    source_value DECIMAL(15, 2),
    form_value DECIMAL(15, 2),
    difference DECIMAL(15, 2),
    difference_percent DECIMAL(5, 2),
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('HIGH', 'MEDIUM', 'LOW')),
    message TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    is_accepted BOOLEAN NOT NULL DEFAULT false,
    acceptance_note TEXT,
    accepted_date TIMESTAMP,
    accepted_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_acceptance_data CHECK (
        (is_accepted = false AND acceptance_note IS NULL AND accepted_date IS NULL AND accepted_by IS NULL)
        OR
        (is_accepted = true AND acceptance_note IS NOT NULL AND accepted_date IS NOT NULL AND accepted_by IS NOT NULL)
    )
);

CREATE INDEX idx_discrepancy_issues_report ON discrepancy_issues(discrepancy_report_id);
CREATE INDEX idx_discrepancy_issues_severity ON discrepancy_issues(severity);
CREATE INDEX idx_discrepancy_issues_category ON discrepancy_issues(category);
CREATE INDEX idx_discrepancy_issues_accepted ON discrepancy_issues(is_accepted);
```

**Notes**:
- `CHECK` constraint ensures acceptance fields are all-or-nothing
- Cascade delete: deleting `DiscrepancyReport` auto-deletes all `DiscrepancyIssue` rows
- Multi-tenant isolation via `tenant_id` on reports table (issues inherit via FK)

---

## JSON Schema (for API responses)

### DiscrepancyReport Response

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["id", "taxReturnId", "hasDiscrepancies", "issues", "summary", "validationDate", "validationRulesVersion"],
  "properties": {
    "id": { "type": "string", "format": "uuid" },
    "taxReturnId": { "type": "string", "format": "uuid" },
    "hasDiscrepancies": { "type": "boolean" },
    "issues": {
      "type": "array",
      "items": { "$ref": "#/definitions/DiscrepancyIssue" }
    },
    "summary": { "$ref": "#/definitions/DiscrepancySummary" },
    "validationDate": { "type": "string", "format": "date-time" },
    "validationRulesVersion": { "type": "string" },
    "tenantId": { "type": "string", "format": "uuid" },
    "createdBy": { "type": "string", "format": "uuid" }
  },
  "definitions": {
    "DiscrepancyIssue": {
      "type": "object",
      "required": ["issueId", "ruleId", "category", "field", "severity", "message", "recommendedAction", "isAccepted"],
      "properties": {
        "issueId": { "type": "string" },
        "ruleId": { "type": "string", "pattern": "^FR-[0-9]{3}$" },
        "category": { "type": "string", "enum": ["W-2", "Schedule C", "Schedule E", "K-1", "Municipal Credit", "Federal Reconciliation", "Carryforward"] },
        "field": { "type": "string", "maxLength": 255 },
        "sourceValue": { "type": "number" },
        "formValue": { "type": "number" },
        "difference": { "type": "number" },
        "differencePercent": { "type": "number" },
        "severity": { "type": "string", "enum": ["HIGH", "MEDIUM", "LOW"] },
        "message": { "type": "string", "maxLength": 500 },
        "recommendedAction": { "type": "string", "maxLength": 500 },
        "isAccepted": { "type": "boolean" },
        "acceptanceNote": { "type": "string", "maxLength": 1000 },
        "acceptedDate": { "type": "string", "format": "date-time" },
        "acceptedBy": { "type": "string", "format": "uuid" }
      }
    },
    "DiscrepancySummary": {
      "type": "object",
      "required": ["totalIssues", "highSeverityCount", "mediumSeverityCount", "lowSeverityCount", "acceptedIssuesCount", "blocksFiling"],
      "properties": {
        "totalIssues": { "type": "integer", "minimum": 0 },
        "highSeverityCount": { "type": "integer", "minimum": 0 },
        "mediumSeverityCount": { "type": "integer", "minimum": 0 },
        "lowSeverityCount": { "type": "integer", "minimum": 0 },
        "acceptedIssuesCount": { "type": "integer", "minimum": 0 },
        "blocksFiling": { "type": "boolean" }
      }
    }
  }
}
```

---

## TypeScript Interfaces (Frontend)

```typescript
export interface DiscrepancyReport {
  id: string;
  taxReturnId: string;
  hasDiscrepancies: boolean;
  issues: DiscrepancyIssue[];
  summary: DiscrepancySummary;
  validationDate: string; // ISO 8601
  validationRulesVersion: string;
  tenantId: string;
  createdBy: string;
}

export interface DiscrepancyIssue {
  issueId: string;
  ruleId: string; // FR-001, FR-002, etc.
  category: 'W-2' | 'Schedule C' | 'Schedule E' | 'K-1' | 'Municipal Credit' | 'Federal Reconciliation' | 'Carryforward';
  field: string;
  sourceValue?: number;
  formValue?: number;
  difference?: number;
  differencePercent?: number;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  message: string;
  recommendedAction: string;
  isAccepted: boolean;
  acceptanceNote?: string;
  acceptedDate?: string;
  acceptedBy?: string;
}

export interface DiscrepancySummary {
  totalIssues: number;
  highSeverityCount: number;
  mediumSeverityCount: number;
  lowSeverityCount: number;
  acceptedIssuesCount: number;
  blocksFiling: boolean;
}

export type ValidationRuleId = 
  | 'FR-001' | 'FR-002' | 'FR-003' | 'FR-004' | 'FR-005'
  | 'FR-006' | 'FR-007' | 'FR-008' | 'FR-009' | 'FR-010'
  | 'FR-011' | 'FR-012' | 'FR-013' | 'FR-014' | 'FR-015'
  | 'FR-016' | 'FR-017' | 'FR-018' | 'FR-019' | 'FR-020'
  | 'FR-021' | 'FR-022';
```

---

## Validation Rules

### Business Rules

1. **Report Completeness**: `DiscrepancyReport` cannot be saved without running all 22 validation rules (FR-001 through FR-022).

2. **Severity Assignment**: Rule implementer determines severity based on thresholds defined in research.md:
   - HIGH: Blocks filing submission (e.g., duplicate W-2, credit exceeds liability)
   - MEDIUM: Warns user, allows filing with acceptance (e.g., Box 1 vs 18 variance 10-20%)
   - LOW: Informational only (e.g., passive loss limit reminder)

3. **Acceptance Rules**:
   - HIGH severity issues cannot be accepted—must be fixed
   - MEDIUM/LOW severity issues can be accepted with user explanation
   - Once accepted, issue is immutable (cannot be un-accepted)

4. **Filing Submission Gate**: `POST /api/submissions/submit` endpoint must query `DiscrepancyReport` and reject submission if `blocksFiling = true`.

### Data Integrity

1. **Orphan Prevention**: Deleting `TaxReturn` cascades to `DiscrepancyReport` and all `DiscrepancyIssue` rows.

2. **Audit Trail**: All acceptance actions (setting `isAccepted = true`) must be logged in separate audit table with user, timestamp, and before/after state.

3. **Version Immutability**: Once `validationRulesVersion` is set on a report, it cannot be changed. Re-running validation creates new report.

4. **Tenant Isolation**: All queries MUST include `tenant_id` filter. Cross-tenant access is prohibited.

---

## Migration Path

For existing returns in database without `DiscrepancyReport`:

1. **Backfill**: Run batch job to generate reports for all returns in DRAFT status.
2. **Skipped Returns**: Returns in SUBMITTED/APPROVED status skip validation (already processed).
3. **Default Severity**: If validation fails to run (e.g., missing prior year data), create LOW severity informational issue: "Validation incomplete—manual review recommended."

---

## Success Metrics

- **Data Quality**: 95% of discrepancy reports have ≤10% false positive rate (measured via user feedback)
- **Performance**: Report generation completes in <3 seconds for return with 10 forms
- **Coverage**: 100% of submitted returns have associated `DiscrepancyReport` (no orphans)
- **Acceptance Rate**: <20% of MEDIUM severity issues accepted (most users fix errors instead)
