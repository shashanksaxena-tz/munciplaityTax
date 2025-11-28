# Data Model: Schedule Y - Multi-State Income Sourcing

**Feature**: Schedule Y - Multi-State Income Sourcing  
**Date**: 2025-11-28  
**Phase**: Phase 1 - Design

This document defines the data entities, relationships, and validation rules for the multi-state apportionment feature.

---

## Entity Relationship Overview

```
BusinessTaxReturn (1) ──── (0..n) ScheduleY (one per municipality)
                                    │
                                    ├── (1) PropertyFactor
                                    ├── (1) PayrollFactor
                                    ├── (1) SalesFactor
                                    │        │
                                    │        └── (0..n) SaleTransaction
                                    │
                                    └── (0..n) ApportionmentAuditLog

Business (1) ──── (0..n) NexusTracking (one per state/municipality/year)
```

---

## Core Entities

### 1. ScheduleY

**Description**: Represents the apportionment schedule (Schedule Y) for a business tax return. Each municipality within Ohio requires a separate Schedule Y.

**Table**: `schedule_y` (tenant-scoped schema)

**Attributes**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| schedule_y_id | UUID | PK | Unique identifier |
| return_id | UUID | FK, NOT NULL | Foreign key to business_tax_return |
| municipality_code | VARCHAR(20) | NOT NULL | Municipality code (e.g., "COLUMBUS", "DUBLIN", "CLEVELAND") |
| tax_year | INT | NOT NULL | Tax year (2024, 2025, etc.) |
| apportionment_formula | ENUM | NOT NULL | TRADITIONAL_THREE_FACTOR, FOUR_FACTOR_DOUBLE_SALES, SINGLE_SALES_FACTOR, CUSTOM |
| formula_weights | JSONB | NULL | Custom weights if formula is CUSTOM: `{"property": 1, "payroll": 1, "sales": 2}` |
| property_factor_percentage | NUMERIC(14,10) | NOT NULL, CHECK (>= 0 AND <= 1) | Property factor: 0.0000000000 to 1.0000000000 (0% to 100%) |
| payroll_factor_percentage | NUMERIC(14,10) | NOT NULL, CHECK (>= 0 AND <= 1) | Payroll factor: 0.0000000000 to 1.0000000000 |
| sales_factor_percentage | NUMERIC(14,10) | NOT NULL, CHECK (>= 0 AND <= 1) | Sales factor: 0.0000000000 to 1.0000000000 |
| final_apportionment_percentage | NUMERIC(14,10) | NOT NULL, CHECK (>= 0 AND <= 1) | Calculated weighted average |
| sourcing_method_election | ENUM | NOT NULL | FINNIGAN, JOYCE |
| throwback_election | ENUM | NOT NULL | THROWBACK, THROWOUT, NONE |
| service_sourcing_method | ENUM | NOT NULL | MARKET_BASED, COST_OF_PERFORMANCE |
| is_jedd_zone | BOOLEAN | DEFAULT FALSE | True if municipality is part of JEDD (Joint Economic Development District) |
| jedd_allocation_percentages | JSONB | NULL | JEDD split if applicable: `{"COLUMBUS": 0.6, "GROVE_CITY": 0.4}` |
| created_date | TIMESTAMP | NOT NULL | Creation timestamp (UTC) |
| last_modified_date | TIMESTAMP | NOT NULL | Last modification timestamp (UTC) |
| created_by | UUID | FK | User who created the schedule |
| last_modified_by | UUID | FK | User who last modified the schedule |

**Indexes**:
- Primary: `schedule_y_id`
- Foreign: `return_id`
- Composite: `(return_id, municipality_code)` - Unique (one Schedule Y per municipality per return)
- Lookup: `tax_year, municipality_code` - For historical comparison

**Validation Rules**:
1. `final_apportionment_percentage` must be calculated as: `(sum of weighted factors) / (sum of weights)`
2. If `apportionment_formula = CUSTOM`, `formula_weights` must be provided
3. If `is_jedd_zone = TRUE`, `jedd_allocation_percentages` must be provided and sum to 1.0
4. `sourcing_method_election`, `throwback_election`, `service_sourcing_method` must match municipality's allowed rules (validated against rule engine)

**State Transitions**:
- DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED (inherited from parent BusinessTaxReturn)
- Elections can be changed in DRAFT state only
- Factor values can be changed in DRAFT or UNDER_REVIEW (by auditor with justification)

---

### 2. PropertyFactor

**Description**: Property values used in apportionment calculation. Includes real property, tangible personal property, and capitalized rents.

**Table**: `property_factor` (tenant-scoped schema)

**Attributes**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| property_factor_id | UUID | PK | Unique identifier |
| schedule_y_id | UUID | FK, NOT NULL | Foreign key to schedule_y |
| ohio_real_property | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Ohio land & buildings ($) |
| ohio_tangible_personal_property | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Ohio equipment, inventory, vehicles ($) |
| ohio_annual_rent | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Annual rent paid for Ohio property ($) |
| ohio_rented_property_value | NUMERIC(20,2) | COMPUTED | `ohio_annual_rent * 8` (capitalized value) |
| total_ohio_property | NUMERIC(20,2) | COMPUTED | Sum of owned + rented property |
| total_property_everywhere | NUMERIC(20,2) | NOT NULL, CHECK (> 0) | Total property value everywhere ($) |
| property_factor_percentage | NUMERIC(14,10) | COMPUTED | `total_ohio_property / total_property_everywhere` |
| averaging_method | ENUM | NOT NULL | AVERAGE_BEGINNING_ENDING, MONTHLY_AVERAGE, DAILY_AVERAGE |
| beginning_of_year_value | NUMERIC(20,2) | NOT NULL | Property value at Jan 1 ($) |
| end_of_year_value | NUMERIC(20,2) | NOT NULL | Property value at Dec 31 ($) |
| monthly_values | JSONB | NULL | Monthly property values if averaging_method = MONTHLY_AVERAGE: `{"JAN": 100000, "FEB": 105000, ...}` |
| created_date | TIMESTAMP | NOT NULL | Creation timestamp |
| last_modified_date | TIMESTAMP | NOT NULL | Last modification timestamp |

**Indexes**:
- Primary: `property_factor_id`
- Foreign: `schedule_y_id` - Unique (one PropertyFactor per Schedule Y)

**Validation Rules**:
1. `total_property_everywhere >= total_ohio_property` (Ohio cannot exceed total)
2. If `averaging_method = MONTHLY_AVERAGE`, `monthly_values` must have 12 entries
3. `end_of_year_value` should be within reasonable range of `beginning_of_year_value` (flag if change > 200%)
4. `ohio_rented_property_value` must equal `ohio_annual_rent * 8` (standard capitalization rate)
5. Exclude intangible property (patents, trademarks) - validation via property type classification

**Calculations**:
```sql
-- Computed fields (database triggers or application layer)
ohio_rented_property_value = ohio_annual_rent * 8
total_ohio_property = ohio_real_property + ohio_tangible_personal_property + ohio_rented_property_value
property_factor_percentage = CASE 
    WHEN total_property_everywhere = 0 THEN 0 
    ELSE total_ohio_property / total_property_everywhere 
END
```

---

### 3. PayrollFactor

**Description**: Payroll used in apportionment calculation. Includes W-2 wages, contractor payments, and officer compensation.

**Table**: `payroll_factor` (tenant-scoped schema)

**Attributes**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| payroll_factor_id | UUID | PK | Unique identifier |
| schedule_y_id | UUID | FK, NOT NULL | Foreign key to schedule_y |
| ohio_w2_wages | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | W-2 wages for Ohio employees ($) |
| ohio_contractor_payments | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | 1099-NEC for Ohio contractors ($) |
| ohio_officer_compensation | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Officer W-2 wages for Ohio officers ($) |
| total_ohio_payroll | NUMERIC(20,2) | COMPUTED | Sum of W-2 + contractors + officers |
| total_payroll_everywhere | NUMERIC(20,2) | NOT NULL, CHECK (> 0) | Total payroll everywhere ($) |
| payroll_factor_percentage | NUMERIC(14,10) | COMPUTED | `total_ohio_payroll / total_payroll_everywhere` |
| employee_count_total | INT | NOT NULL, DEFAULT 0 | Total employee headcount |
| employee_count_ohio | INT | NOT NULL, DEFAULT 0 | Ohio employee headcount |
| remote_employee_allocation | JSONB | NULL | Multi-state allocation: `{"OH": 500000, "CA": 300000, "NY": 200000}` |
| created_date | TIMESTAMP | NOT NULL | Creation timestamp |
| last_modified_date | TIMESTAMP | NOT NULL | Last modification timestamp |

**Indexes**:
- Primary: `payroll_factor_id`
- Foreign: `schedule_y_id` - Unique (one PayrollFactor per Schedule Y)

**Validation Rules**:
1. `total_payroll_everywhere >= total_ohio_payroll` (Ohio cannot exceed total)
2. If `remote_employee_allocation` provided, sum of values must equal `total_payroll_everywhere` (±1% tolerance for rounding)
3. `employee_count_ohio <= employee_count_total`
4. If `employee_count_ohio > 0` but `total_ohio_payroll = 0`, flag inconsistency warning
5. Cross-validate with W-1 withholding data if available (Spec 1 integration)

**Calculations**:
```sql
total_ohio_payroll = ohio_w2_wages + ohio_contractor_payments + ohio_officer_compensation
payroll_factor_percentage = CASE 
    WHEN total_payroll_everywhere = 0 THEN 0 
    ELSE total_ohio_payroll / total_payroll_everywhere 
END
```

---

### 4. SalesFactor

**Description**: Sales used in apportionment calculation. Aggregates sales from SaleTransaction records.

**Table**: `sales_factor` (tenant-scoped schema)

**Attributes**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| sales_factor_id | UUID | PK | Unique identifier |
| schedule_y_id | UUID | FK, NOT NULL | Foreign key to schedule_y |
| ohio_sales_tangible_goods | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Sales of physical products delivered to Ohio ($) |
| ohio_sales_services | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Service revenue sourced to Ohio ($) |
| ohio_sales_rental_income | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Rental income from Ohio property ($) |
| ohio_sales_interest | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Interest income sourced to Ohio ($) |
| ohio_sales_royalties | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Royalty income sourced to Ohio ($) |
| ohio_sales_other | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Other income sourced to Ohio ($) |
| throwback_adjustment | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Sales thrown back to Ohio (no nexus in destination) ($) |
| total_ohio_sales | NUMERIC(20,2) | COMPUTED | Sum of all Ohio sales + throwback adjustment |
| total_sales_everywhere | NUMERIC(20,2) | NOT NULL, CHECK (> 0) | Total sales everywhere ($) |
| sales_factor_percentage | NUMERIC(14,10) | COMPUTED | `total_ohio_sales / total_sales_everywhere` |
| created_date | TIMESTAMP | NOT NULL | Creation timestamp |
| last_modified_date | TIMESTAMP | NOT NULL | Last modification timestamp |

**Indexes**:
- Primary: `sales_factor_id`
- Foreign: `schedule_y_id` - Unique (one SalesFactor per Schedule Y)

**Validation Rules**:
1. `total_sales_everywhere >= total_ohio_sales` (Ohio cannot exceed total, but may due to throwback - allow up to 110%)
2. `sales_factor_percentage <= 1.1` (allow slightly over 100% due to throwback, but flag if > 110%)
3. `throwback_adjustment` should match sum of throwback transactions in `sale_transaction` table
4. Cross-validate with Schedule C (Business Income) total gross receipts

**Calculations**:
```sql
total_ohio_sales = ohio_sales_tangible_goods + ohio_sales_services + ohio_sales_rental_income 
                 + ohio_sales_interest + ohio_sales_royalties + ohio_sales_other + throwback_adjustment
sales_factor_percentage = CASE 
    WHEN total_sales_everywhere = 0 THEN 0 
    ELSE total_ohio_sales / total_sales_everywhere 
END
```

---

### 5. SaleTransaction

**Description**: Individual sale transaction with sourcing details. Supports transaction-level throwback determination and service revenue allocation.

**Table**: `sale_transaction` (tenant-scoped schema)

**Attributes**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| transaction_id | UUID | PK | Unique identifier |
| sales_factor_id | UUID | FK, NOT NULL | Foreign key to sales_factor |
| transaction_date | DATE | NOT NULL | Date of sale |
| customer_name | VARCHAR(255) | NOT NULL | Customer name (encrypted for PII) |
| customer_location_state | VARCHAR(2) | NULL | Customer state (for service revenue sourcing) |
| sale_amount | NUMERIC(20,2) | NOT NULL, CHECK (> 0) | Total sale amount ($) |
| sale_type | ENUM | NOT NULL | TANGIBLE_GOODS, SERVICES, RENTAL_INCOME, INTEREST, ROYALTIES, OTHER |
| origin_state | VARCHAR(2) | NOT NULL | State where goods shipped from or services performed |
| destination_state | VARCHAR(2) | NOT NULL | State where goods delivered or customer located |
| sourcing_method | ENUM | NOT NULL | DESTINATION, MARKET_BASED, COST_OF_PERFORMANCE, THROWBACK, THROWOUT |
| has_destination_nexus | BOOLEAN | NOT NULL | Does business have nexus in destination state? |
| allocated_state | VARCHAR(2) | NOT NULL | Final state assignment after sourcing rules |
| allocated_amount | NUMERIC(20,2) | NOT NULL | Amount allocated to state (may differ from sale_amount for proration) |
| service_allocation_details | JSONB | NULL | Multi-state allocation for services: `{"OH": 70000, "CA": 30000}` |
| throwback_reason | TEXT | NULL | Reason for throwback (e.g., "No nexus in CA - Economic threshold not met") |
| created_date | TIMESTAMP | NOT NULL | Creation timestamp |
| last_modified_date | TIMESTAMP | NOT NULL | Last modification timestamp |

**Indexes**:
- Primary: `transaction_id`
- Foreign: `sales_factor_id`
- Lookup: `(sales_factor_id, allocated_state)` - For aggregating sales by state
- Lookup: `(transaction_date)` - For date range queries

**Validation Rules**:
1. If `sale_type = TANGIBLE_GOODS`, `sourcing_method` must be `DESTINATION` or `THROWBACK`
2. If `sale_type = SERVICES`, `sourcing_method` must be `MARKET_BASED` or `COST_OF_PERFORMANCE`
3. If `sourcing_method = THROWBACK`, `has_destination_nexus` must be `FALSE`
4. If `sourcing_method = THROWBACK`, `allocated_state` must equal `origin_state`
5. If `service_allocation_details` provided, sum of values must equal `sale_amount` (±1% tolerance)
6. `allocated_amount` must be <= `sale_amount` (no allocation can exceed original sale)
7. `customer_name` must be encrypted at rest (PII protection)

**Sourcing Logic**:
```pseudocode
IF sale_type = TANGIBLE_GOODS:
    IF has_destination_nexus = TRUE:
        sourcing_method = DESTINATION
        allocated_state = destination_state
    ELSE:
        IF throwback_election = THROWBACK:
            sourcing_method = THROWBACK
            allocated_state = origin_state
        ELSE IF throwback_election = THROWOUT:
            sourcing_method = THROWOUT
            allocated_state = NULL (exclude from factor)
        ELSE:
            sourcing_method = DESTINATION
            allocated_state = destination_state

ELSE IF sale_type = SERVICES:
    IF service_sourcing_method = MARKET_BASED:
        IF customer_location_state IS NOT NULL:
            sourcing_method = MARKET_BASED
            allocated_state = customer_location_state
        ELSE:
            // Fallback to cost-of-performance
            sourcing_method = COST_OF_PERFORMANCE
            allocated_state = origin_state
    ELSE:
        sourcing_method = COST_OF_PERFORMANCE
        allocated_state = origin_state (or prorate via service_allocation_details)
```

---

### 6. NexusTracking

**Description**: Tracks nexus status in each state/municipality for throwback determination and compliance.

**Table**: `nexus_tracking` (tenant-scoped schema)

**Attributes**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| nexus_id | UUID | PK | Unique identifier |
| business_id | UUID | FK, NOT NULL | Foreign key to business |
| tax_year | INT | NOT NULL | Tax year (2024, 2025, etc.) |
| state_code | VARCHAR(2) | NOT NULL | State code (OH, CA, NY, etc.) |
| municipality_name | VARCHAR(100) | NULL | City/county name (for intra-state nexus) |
| has_nexus | BOOLEAN | NOT NULL | Does business have nexus? |
| nexus_reasons | JSONB | NOT NULL | Array of reasons: `["PHYSICAL_PRESENCE", "ECONOMIC_NEXUS"]` |
| sales_in_state | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Total sales in state ($) |
| property_in_state | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Property value in state ($) |
| payroll_in_state | NUMERIC(20,2) | NOT NULL, DEFAULT 0 | Payroll in state ($) |
| employee_count_in_state | INT | NOT NULL, DEFAULT 0 | Employees working in state |
| economic_nexus_threshold | NUMERIC(20,2) | NOT NULL | State's economic nexus threshold ($) |
| last_evaluated_date | TIMESTAMP | NOT NULL | When nexus was last evaluated |
| created_date | TIMESTAMP | NOT NULL | Creation timestamp |
| last_modified_date | TIMESTAMP | NOT NULL | Last modification timestamp |

**Indexes**:
- Primary: `nexus_id`
- Foreign: `business_id`
- Composite: `(business_id, tax_year, state_code, municipality_name)` - Unique
- Lookup: `(state_code, has_nexus)` - For querying nexus by state

**Validation Rules**:
1. If `sales_in_state >= economic_nexus_threshold`, `has_nexus` must be `TRUE` and `nexus_reasons` must include `ECONOMIC_NEXUS`
2. If `employee_count_in_state > 0` or `property_in_state > 0`, `has_nexus` must be `TRUE` and `nexus_reasons` must include `PHYSICAL_PRESENCE`
3. `economic_nexus_threshold` defaults to $500,000 for Ohio (post-Wayfair standard)
4. `nexus_reasons` must be non-empty array if `has_nexus = TRUE`

**Nexus Determination Logic**:
```pseudocode
has_nexus = FALSE
nexus_reasons = []

// Physical presence
IF employee_count_in_state > 0 OR property_in_state > 0:
    has_nexus = TRUE
    nexus_reasons.append("PHYSICAL_PRESENCE")

// Economic nexus
IF sales_in_state >= economic_nexus_threshold:
    has_nexus = TRUE
    nexus_reasons.append("ECONOMIC_NEXUS")

// Factor presence (rough heuristic - 15% threshold)
IF (property_in_state / total_property_everywhere) > 0.15 
   OR (payroll_in_state / total_payroll_everywhere) > 0.15:
    has_nexus = TRUE
    nexus_reasons.append("FACTOR_PRESENCE")
```

---

### 7. ApportionmentAuditLog

**Description**: Immutable audit trail for all apportionment calculations, elections, and modifications.

**Table**: `apportionment_audit_log` (tenant-scoped schema)

**Attributes**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| audit_log_id | UUID | PK | Unique identifier |
| schedule_y_id | UUID | FK, NOT NULL | Foreign key to schedule_y |
| change_type | ENUM | NOT NULL | ELECTION_CHANGED, FACTOR_RECALCULATED, TRANSACTION_ADDED, TRANSACTION_MODIFIED, NEXUS_CHANGED, CALCULATION_OVERRIDE |
| changed_by | UUID | FK, NOT NULL | User who made change |
| user_role | ENUM | NOT NULL | INDIVIDUAL, CPA, AUDITOR, ADMIN |
| change_date | TIMESTAMP | NOT NULL | Timestamp (UTC) |
| old_value | JSONB | NULL | Previous value (JSON object) |
| new_value | JSONB | NULL | New value (JSON object) |
| change_reason | TEXT | NULL | User-provided explanation (required for auditor overrides) |
| affected_calculation | VARCHAR(100) | NOT NULL | Which factor/calculation was affected (e.g., "property_factor", "throwback_adjustment") |
| ip_address | VARCHAR(45) | NULL | IP address of user (for security audit) |
| created_date | TIMESTAMP | NOT NULL | Creation timestamp (immutable) |

**Indexes**:
- Primary: `audit_log_id`
- Foreign: `schedule_y_id`
- Lookup: `(changed_by, change_date)` - For user activity audit
- Lookup: `(change_type, change_date)` - For change type filtering

**Validation Rules**:
1. No updates or deletes allowed (append-only)
2. If `user_role = AUDITOR` and `change_type = CALCULATION_OVERRIDE`, `change_reason` is REQUIRED
3. If `change_type = TRANSACTION_ADDED`, `new_value` must contain full `SaleTransaction` JSON
4. `old_value` and `new_value` must be valid JSON objects
5. Retention: 7+ years (IRS requirement)

**Example Audit Log Entries**:

```json
// Election changed
{
  "change_type": "ELECTION_CHANGED",
  "changed_by": "user-uuid-123",
  "user_role": "CPA",
  "old_value": {"sourcing_method_election": "JOYCE"},
  "new_value": {"sourcing_method_election": "FINNIGAN"},
  "change_reason": "Changed to Finnigan per municipality ordinance",
  "affected_calculation": "sales_factor_denominator"
}

// Factor recalculated
{
  "change_type": "FACTOR_RECALCULATED",
  "changed_by": "user-uuid-456",
  "user_role": "INDIVIDUAL",
  "old_value": {"property_factor_percentage": 0.2000},
  "new_value": {"property_factor_percentage": 0.2500},
  "change_reason": "Updated end-of-year property value",
  "affected_calculation": "property_factor"
}

// Transaction added
{
  "change_type": "TRANSACTION_ADDED",
  "changed_by": "user-uuid-789",
  "user_role": "INDIVIDUAL",
  "old_value": null,
  "new_value": {
    "transaction_id": "txn-uuid-abc",
    "sale_amount": 100000,
    "sale_type": "TANGIBLE_GOODS",
    "destination_state": "CA",
    "has_destination_nexus": false,
    "sourcing_method": "THROWBACK",
    "allocated_state": "OH"
  },
  "change_reason": "Added sale to California customer",
  "affected_calculation": "throwback_adjustment"
}

// Auditor override
{
  "change_type": "CALCULATION_OVERRIDE",
  "changed_by": "auditor-uuid-999",
  "user_role": "AUDITOR",
  "old_value": {"sales_factor_percentage": 0.5000},
  "new_value": {"sales_factor_percentage": 0.4500},
  "change_reason": "Corrected sourcing error - Transaction #12345 should not have been thrown back to OH per taxpayer documentation showing CA nexus",
  "affected_calculation": "sales_factor"
}
```

---

## Entity Relationships

### One-to-One Relationships:
- `ScheduleY (1) ── (1) PropertyFactor`
- `ScheduleY (1) ── (1) PayrollFactor`
- `ScheduleY (1) ── (1) SalesFactor`

### One-to-Many Relationships:
- `BusinessTaxReturn (1) ── (0..n) ScheduleY` - One Schedule Y per municipality
- `SalesFactor (1) ── (0..n) SaleTransaction` - Detailed sales transactions
- `ScheduleY (1) ── (0..n) ApportionmentAuditLog` - Audit trail
- `Business (1) ── (0..n) NexusTracking` - Nexus status per state/year

### Cross-References:
- `ScheduleY.return_id → BusinessTaxReturn.return_id`
- `PropertyFactor.schedule_y_id → ScheduleY.schedule_y_id`
- `PayrollFactor.schedule_y_id → ScheduleY.schedule_y_id`
- `SalesFactor.schedule_y_id → ScheduleY.schedule_y_id`
- `SaleTransaction.sales_factor_id → SalesFactor.sales_factor_id`
- `NexusTracking.business_id → Business.business_id`
- `ApportionmentAuditLog.schedule_y_id → ScheduleY.schedule_y_id`

---

## Data Flow

### Calculation Flow:
1. **Input**: User enters property values, payroll, sales transactions
2. **Factor Calculation**: System calculates property/payroll/sales factor percentages
3. **Sourcing Rules**: System applies Joyce/Finnigan, throwback/throwout, market-based/cost-of-performance
4. **Weighted Formula**: System applies apportionment formula weights (e.g., double sales factor)
5. **Final Apportionment**: System calculates final apportionment percentage
6. **Audit Trail**: All changes logged to `apportionment_audit_log`

### Nexus Determination Flow:
1. **Input**: User enters sales/property/payroll by state
2. **Nexus Evaluation**: System evaluates nexus rules (physical, economic, factor presence)
3. **Nexus Status**: System updates `nexus_tracking` table
4. **Throwback Application**: System uses nexus status to determine throwback for sales transactions
5. **Factor Adjustment**: System recalculates sales factor with throwback adjustments

---

## Validation Summary

### Cross-Entity Validations:
1. Sum of Ohio amounts in PropertyFactor, PayrollFactor, SalesFactor must be <= total amounts everywhere
2. SalesFactor.throwback_adjustment must equal sum of throwback transactions in SaleTransaction table
3. ScheduleY.final_apportionment_percentage must be calculated from factor percentages per formula weights
4. NexusTracking.has_nexus must determine SaleTransaction.has_destination_nexus for throwback
5. ApportionmentAuditLog must exist for every change to ScheduleY, PropertyFactor, PayrollFactor, SalesFactor

### Consistency Checks:
1. If PropertyFactor.total_ohio_property = 0, warn if business has Ohio office address
2. If PayrollFactor.total_ohio_payroll = 0, warn if business has Ohio employees in employee records
3. If SalesFactor.sales_factor_percentage > 1.0 (100%), flag for review (may be valid due to throwback)
4. Sum of all state apportionment percentages should be ~100% (allow 95-105% due to throwback variations)

---

## Data Retention

- **Transactional Data** (ScheduleY, factors, transactions): 7+ years per IRS requirements
- **Audit Logs** (ApportionmentAuditLog): 7+ years, immutable
- **Nexus Tracking** (NexusTracking): 7+ years for historical nexus status
- **Soft Deletes**: All entities use soft delete (is_deleted flag) to preserve audit trail
- **Archival**: After 7 years, move to archive schema but do not delete

---

## Next Steps

This data model supports Phase 1 implementation. Next deliverables:
1. **API Contracts** (`contracts/apportionment-api.yaml`) - REST endpoints for CRUD operations
2. **Quickstart Guide** (`quickstart.md`) - Developer setup instructions
3. **Update Agent Context** - Add new technologies/patterns to `.github/agents/copilot-instructions.md`
