# Feature Specification: Dynamic Rule Configuration System

**Feature Branch**: `4-rule-configuration-ui`  
**Created**: 2025-11-27  
**Status**: Draft  
**Input**: Create dynamic rule configuration UI to stop hardcoding tax rules - build rule management system with versioning, temporal rules (effective dates), tenant-specific overrides, and JSON-based rule storage for both individual and business tax calculations

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tax Administrator Updates Municipal Rate for New Tax Year (Priority: P1)

The City of Dublin passes ordinance increasing municipal tax rate from 2.0% to 2.25% effective January 1, 2026. Tax administrator needs to configure new rate with effective date so 2025 returns use 2.0% while 2026 returns use 2.25%, without code changes.

**Why this priority**: Tax rates change annually. Hardcoded rates require developer deployment for simple config changes. This is the #1 reason for dynamic rule management.

**Independent Test**: Can be fully tested by configuring new rule "Municipal Rate = 2.25%" with effective date 2026-01-01. System should apply 2.0% to returns for tax year 2025 and 2.25% to returns for tax year 2026.

**Acceptance Scenarios**:

1. **Given** current municipal rate is 2.0% (effective 2020-01-01), **When** admin creates new rate rule 2.25% effective 2026-01-01, **Then** system saves rule with future effective date and continues applying 2.0% to 2025 returns
2. **Given** new rate 2.25% is effective 2026-01-01, **When** user calculates 2026 tax return on Jan 15 2026, **Then** system automatically applies 2.25% rate without manual selection
3. **Given** admin needs to amend the 2026 rate to 2.3% before it takes effect, **When** admin updates 2026-01-01 rule from 2.25% to 2.3%, **Then** system archives 2.25% version and activates 2.3% (no returns filed yet, safe to modify)
4. **Given** 2026-01-01 effective date has passed and returns have been filed, **When** admin tries to modify 2.25% rate, **Then** system blocks modification with error "Cannot edit rule after effective date - create new rate with future effective date instead"

---

### User Story 2 - CPA Firm Manages Rules for Multiple Municipal Clients (Priority: P1)

A CPA firm prepares returns for businesses in 3 municipalities: Dublin (2.0%), Columbus (2.5%), and Cleveland (2.0%). Each municipality has different rules for Schedule X adjustments, NOL limits, and penalty rates. CPA needs tenant-specific rule sets.

**Why this priority**: Multi-tenant SaaS must support jurisdiction-specific rules. Single-tenant deployment per municipality doesn't scale. Critical for product scalability.

**Independent Test**: Can be tested by configuring 3 tenants (Dublin, Columbus, Cleveland) with different municipal rates and NOL cap percentages. When CPA selects "Dublin" tenant, system applies 2.0% rate and 50% NOL cap; when selecting "Columbus", applies 2.5% rate and 80% NOL cap.

**Acceptance Scenarios**:

1. **Given** system has 3 configured tenants (Dublin, Columbus, Cleveland), **When** CPA creates new business return and selects "Dublin" from tenant dropdown, **Then** system loads Dublin-specific rules: 2.0% rate, 50% NOL cap, $50 minimum tax
2. **Given** Columbus has unique rule "Meals & Entertainment 80% add-back" (vs Dublin's 100%), **When** CPA files Columbus return with $10,000 meals, **Then** system adds back $8,000 (80%) instead of $10,000
3. **Given** CPA has 15 client businesses across 3 municipalities, **When** they view rule comparison dashboard, **Then** system displays side-by-side table showing rate, NOL cap, minimum tax, and key adjustment rules for each municipality
4. **Given** Dublin updates a rule, **When** CPA logs in next time, **Then** system shows notification "Dublin rules updated - review changes" with changelog of modified rules

---

### User Story 3 - Individual Tax Rules Configure W-2 Qualifying Wages Logic (Priority: P2)

Dublin tax administrator wants to change W-2 qualifying wages rule from "Highest of All Boxes" to "Box 5 Medicare Wages Only" for tax year 2026 to align with state guidance. Admin updates rule configuration without code deployment.

**Why this priority**: W-2 qualifying wages logic varies by jurisdiction and changes based on court cases or state guidance. Must be configurable without developer intervention.

**Independent Test**: Can be tested by configuring rule "W2 Qualifying Wages Method = BOX_5_MEDICARE" effective 2026-01-01. When calculating 2026 return with W-2 showing Box 1=$50K, Box 5=$52K, Box 18=$50K, system should use Box 5 value ($52K) as qualifying wages.

**Acceptance Scenarios**:

1. **Given** current rule is "HIGHEST_OF_ALL" (uses highest value from Boxes 1, 5, 18), **When** admin changes to "BOX_5_MEDICARE" effective 2026-01-01, **Then** system creates new rule version with effective date
2. **Given** W-2 shows Box 1=$50K (Federal), Box 5=$52K (Medicare), Box 18=$50K (Local), **When** calculating 2026 return with BOX_5_MEDICARE rule, **Then** system uses $52K as qualifying wages
3. **Given** admin wants to preview impact before deploying, **When** they run "What-If Analysis" on sample returns, **Then** system calculates tax using both old and new rules and shows difference ($52K × 2% = $1,040 vs $50K × 2% = $1,000 = $40 increase per return)
4. **Given** rule change affects 5,000 taxpayers, **When** admin publishes new rule, **Then** system sends notification email to all affected taxpayers "Tax calculation method updated for 2026 - your tax may be slightly different"

---

### User Story 4 - Configure Income Inclusion Rules for Business Entities (Priority: P2)

Tax administrator wants to exclude dividend income from municipal tax for C-Corporations (to avoid double taxation) but include it for Partnerships and S-Corps. Admin configures entity-specific income inclusion rules.

**Why this priority**: Tax treatment varies by entity type (C-Corp vs S-Corp vs Partnership). Business tax rules need entity-specific customization without separate codebases.

**Independent Test**: Can be tested by configuring rule "Dividend Income Inclusion: C-Corp=false, Partnership=true, S-Corp=true". When C-Corp reports $10K dividends on Schedule X, system deducts full $10K; when Partnership reports $10K dividends, system deducts $0.

**Acceptance Scenarios**:

1. **Given** rule "Dividend Income Deductible for C-Corps" is configured, **When** C-Corp files Form 27 with $10,000 dividend income, **Then** system automatically adds $10,000 to Schedule X Deductions (non-taxable)
2. **Given** same rule configured differently for Partnerships, **When** Partnership (Form 1065) reports $10,000 dividend income, **Then** system does NOT deduct dividends (treats as taxable partnership income)
3. **Given** admin wants to change rule for S-Corps only, **When** they edit entity-specific overrides, **Then** system shows entity dropdown: [All Entities | C-Corp Only | S-Corp Only | Partnership Only | LLC taxed as...]
4. **Given** complex rule "Interest income deductible EXCEPT if from related-party debt", **When** admin configures conditional rule with exception clause, **Then** system supports nested rule logic: IF (incomeType = Interest) AND (relatedParty = false) THEN deduct

---

### User Story 5 - Version Control and Audit Trail for Rule Changes (Priority: P3)

State auditor reviews Dublin's tax calculation methodology for 2020-2024 period. Tax administrator needs to generate report showing all rule changes during this period, including who changed rules, when, and why.

**Why this priority**: Audit defense requires proving tax calculations followed published rules. Rule versioning creates defensible audit trail. Essential for compliance but not daily operations.

**Independent Test**: Can be tested by querying rule history for "Municipal Rate" rule from 2020-2024. System should return: v1 (1.75%, effective 2020-01-01, created by Admin A), v2 (2.0%, effective 2023-01-01, created by Admin B with reason "Ordinance 2022-45"), v3 (2.25%, effective 2024-01-01, created by Admin B).

**Acceptance Scenarios**:

1. **Given** Municipal Rate rule has 3 historical versions (1.75% in 2020, 2.0% in 2023, 2.25% in 2024), **When** admin views rule history, **Then** system displays timeline showing each version with effective date, rate value, who changed it, and approval documentation
2. **Given** auditor asks "What was the NOL carryforward rule for 2022?", **When** admin queries rule as-of date 2022-06-15, **Then** system returns exact rule configuration that was active on that date (50% NOL cap, effective 2020-01-01, ordinance reference)
3. **Given** admin needs to generate audit report, **When** they export "Rule Change Log 2020-2024" to PDF, **Then** system produces document listing all 47 rule changes with timestamps, change reasons, and affected taxpayer counts
4. **Given** rule change was made in error (wrong effective date entered), **When** admin rolls back to previous version within 24 hours, **Then** system restores previous rule and marks current version as "Voided - Data Entry Error"

---

### Edge Cases

- **Overlapping effective dates**: Admin creates 2026-01-01 rate of 2.25% but forgets to end-date the 2025 rate. System should detect overlap and prompt "Previous rate has no end date - set end date to 2025-12-31?"
- **Retroactive rule changes**: Admin wants to correct 2024 rate from 2.0% to 2.25% after 1,000 returns already filed. System should block and recommend "Create amended return program for affected taxpayers" instead.
- **Rule dependencies**: Municipal rate rule feeds into penalty calculation (15% of tax due). If rate changes, penalty calculation auto-updates. System must track these dependencies.
- **Mid-year rate change**: Rare but legal (e.g., emergency tax increase July 1). System must pro-rate calculations: first 6 months at 2.0%, last 6 months at 2.25%.
- **Rule expiration (sunset clause)**: Temporary rate reduction for COVID relief (1.5% for 2021-2022, reverts to 2.0% in 2023). System should support expiration dates that automatically restore previous rule.
- **Complex conditional rules**: "If business income > $1M, minimum tax = $5,000; otherwise $50." System must support if/then/else rule logic.
- **Grandfathered rules**: Some businesses filed under old rules and get to keep them (e.g., depreciation election made in 2015 continues indefinitely). System must support entity-specific rule overrides.
- **Multi-criteria rules**: W-2 qualifying wages depends on: entity type AND filing status AND resident vs non-resident. System must support multi-dimensional rule matrices.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Rule Definition & Storage

- **FR-001**: System MUST store all tax rules in JSON format in database (NOT hardcoded in constants.ts or Java code), with each rule having: ruleId, ruleName, ruleCategory, ruleType, value, effectiveDate, endDate, tenantId, entityTypeAppliesTo, createdBy, createdDate, approvalStatus
- **FR-002**: System MUST support rule categories: "Tax Rates", "Income Inclusion", "Deductions & Credits", "Penalties & Interest", "Filing Requirements", "Business Allocation", "Withholding", "Validation Thresholds"
- **FR-003**: System MUST support rule value types: Number (e.g., 2.0%), Percentage (e.g., 50%), Enum (e.g., "BOX_5_MEDICARE"), Boolean (e.g., true/false), Formula (e.g., "wages * 0.02"), Conditional (e.g., IF income > 1000000 THEN 5000 ELSE 50)
- **FR-004**: System MUST validate rule values before saving: percentages must be 0-100%, tax rates must be 0-10%, effective dates must be valid dates, formulas must parse correctly
- **FR-005**: System MUST support rule dependencies: if Rule A references Rule B, changing Rule B triggers recalculation warning for all dependent rules

#### Temporal Rules (Effective Dating)

- **FR-006**: System MUST enforce effective date logic: when calculating return for tax year 2025, use rules where effectiveDate <= 2025-12-31 AND (endDate >= 2025-01-01 OR endDate IS NULL)
- **FR-007**: System MUST prevent overlapping rules: if new rule has effectiveDate = 2026-01-01, system must check no other active rule exists for same ruleId + tenantId with overlapping date range
- **FR-008**: System MUST support mid-year effective dates for emergency tax changes: if effectiveDate = 2026-07-01, system pro-rates calculation (6 months old rate + 6 months new rate)
- **FR-009**: System MUST support expiration dates (sunset clauses): if rule has endDate = 2026-12-31, system automatically reverts to previous rule version on 2027-01-01
- **FR-010**: System MUST block retroactive rule changes: if effectiveDate < TODAY and returns have been filed under old rule, system prevents modification and shows error "Cannot change rule retroactively - file amended returns instead"

#### Multi-Tenant Support

- **FR-011**: System MUST support tenant-specific rules: each rule has tenantId field (Dublin, Columbus, Cleveland, etc.) and only applies to returns filed under that tenant
- **FR-012**: System MUST provide tenant inheritance: if rule is configured at "Global" level, all tenants inherit it unless they create tenant-specific override
- **FR-013**: System MUST display tenant comparison view: show side-by-side table of all rule values for selected tenants (e.g., compare Dublin vs Columbus municipal rate, NOL cap, minimum tax)
- **FR-014**: System MUST support tenant-specific effective dates: Dublin adopts new rule 2026-01-01 while Columbus adopts same rule 2027-01-01

#### Entity-Specific Rules

- **FR-015**: System MUST support entity type targeting: rules can apply to [All Entities | C-Corp Only | S-Corp Only | Partnership Only | Individual Only | LLC taxed as...]
- **FR-016**: System MUST evaluate entity-specific rules before general rules: if C-Corp has specific dividend deduction rule, use it instead of "All Entities" rule
- **FR-017**: System MUST support multi-criteria rules: rule can target "C-Corp AND Income > $1M" or "Partnership AND Multi-state operation"

#### Rule Configuration UI

- **FR-018**: System MUST provide RuleConfigurationDashboard component showing: Active Rules count, Pending Approval count, Upcoming Effective Dates (next 90 days), Recently Changed (last 30 days)
- **FR-019**: System MUST provide RuleEditor component with fields: Rule Name (read-only after creation), Category (dropdown), Value Type (Number/Percentage/Enum/Formula/Conditional), Current Value, Effective Date, End Date (optional), Applies To (Entity Type multi-select), Tenant (dropdown), Reason for Change (required), Approval Status
- **FR-020**: System MUST provide rule search/filter: by category, by tenant, by effective date range, by entity type, by approval status
- **FR-021**: System MUST provide "What-If Analysis" tool: admin selects sample returns and tests new rule configuration, system calculates tax under old vs new rules and shows impact per return
- **FR-022**: System MUST provide rule versioning UI: display timeline of all versions for a rule, with diff view showing what changed between versions
- **FR-023**: System MUST support rule approval workflow: new rules have status "Pending" until approved by authorized admin, then status changes to "Approved" and effective date activates
- **FR-024**: System MUST validate rule conflicts: if admin tries to create overlapping rule or conflicting logic (e.g., "Dividends 100% deductible" + "Dividends 0% deductible"), system blocks and explains conflict

#### Formula & Conditional Rules

- **FR-025**: System MUST support formula rules: value can be expression like "wages * municipalRate" or "MAX(minimumTax, income * rate)" where variables reference other rules or form fields
- **FR-026**: System MUST support conditional rules: "IF income > 1000000 THEN minimumTax = 5000 ELSE minimumTax = 50" with support for AND, OR, >, <, >=, <=, =, != operators
- **FR-027**: System MUST validate formulas: before saving, parse formula and check all referenced variables exist and are compatible types (can't multiply string by number)
- **FR-028**: System MUST provide formula testing tool: admin enters sample input values, system evaluates formula and shows result (e.g., "If wages=50000 and municipalRate=0.02, result = 1000")

#### Integration with Tax Calculators

- **FR-029**: System MUST replace hardcoded rules in IndividualTaxCalculator.java: read TaxRulesConfig from database query filtered by taxYear + tenantId instead of from DEFAULT_INDIVIDUAL_RULES constant
- **FR-030**: System MUST replace hardcoded rules in BusinessTaxCalculator.java: read BusinessTaxRulesConfig from database query instead of from DEFAULT_BUSINESS_RULES constant
- **FR-031**: System MUST cache active rules in memory: query database once per tax calculation session, not on every calculation (performance optimization)
- **FR-032**: System MUST provide rule refresh API: when admin publishes rule change, backend clears rule cache and reloads from database (invalidate cache)

#### Audit Trail & Reporting

- **FR-033**: System MUST log all rule changes: who changed, what changed (old value → new value), when changed, reason for change, approval status
- **FR-034**: System MUST support point-in-time rule query: "What was municipal rate on 2022-06-15?" returns exact rule active on that date
- **FR-035**: System MUST generate Rule Change Report: PDF export showing all rule changes for specified date range, with before/after values, effective dates, and affected taxpayer counts
- **FR-036**: System MUST track rule usage: count how many returns calculated using each rule version (for impact analysis)
- **FR-037**: System MUST support rule rollback: revert to previous version within 24 hours if change was made in error (after 24 hours, must create new rule version)

### Key Entities *(include if feature involves data)*

- **TaxRule**:
  - **ruleId**: string (UUID) - unique identifier
  - **ruleName**: string - human-readable name (e.g., "Municipal Tax Rate")
  - **ruleCode**: string - system code used in calculations (e.g., "MUNICIPAL_RATE")
  - **category**: enum - "TaxRates" | "IncomeInclusion" | "Deductions" | "Penalties" | "Filing" | "Allocation" | "Withholding" | "Validation"
  - **valueType**: enum - "NUMBER" | "PERCENTAGE" | "ENUM" | "BOOLEAN" | "FORMULA" | "CONDITIONAL"
  - **value**: any - actual rule value (0.02 for 2%, "BOX_5_MEDICARE" for enum, "wages * 0.02" for formula)
  - **effectiveDate**: date - when rule becomes active (YYYY-MM-DD)
  - **endDate**: date | null - when rule expires (null = active indefinitely)
  - **tenantId**: string - "dublin" | "columbus" | "cleveland" | "GLOBAL"
  - **entityTypes**: string[] - ["C-CORP"] or ["ALL"] or ["PARTNERSHIP", "S-CORP"]
  - **appliesTo**: string - additional targeting criteria (e.g., "income > 1000000")
  - **createdBy**: string - admin user ID
  - **createdDate**: timestamp
  - **modifiedBy**: string | null
  - **modifiedDate**: timestamp | null
  - **approvalStatus**: enum - "PENDING" | "APPROVED" | "REJECTED" | "VOIDED"
  - **approvedBy**: string | null
  - **approvalDate**: timestamp | null
  - **changeReason**: string - required for audit trail
  - **ordinanceReference**: string - legal citation (e.g., "Ordinance 2022-45")
  - **version**: number - increments with each change
  - **previousVersionId**: string | null - links to previous version
  - **dependsOn**: string[] - array of ruleIds this rule references

- **RuleChangeLog**:
  - **logId**: string (UUID)
  - **ruleId**: string - references TaxRule
  - **changeType**: enum - "CREATE" | "UPDATE" | "DELETE" | "APPROVE" | "REJECT" | "VOID" | "ROLLBACK"
  - **oldValue**: any - value before change
  - **newValue**: any - value after change
  - **changedFields**: string[] - list of fields modified
  - **changedBy**: string - admin user ID
  - **changeDate**: timestamp
  - **changeReason**: string
  - **affectedReturnsCount**: number - how many filed returns used this rule
  - **impactEstimate**: object - {avgTaxIncrease: number, avgTaxDecrease: number, totalImpactedTaxpayers: number}

## Success Criteria *(mandatory)*

- Tax administrators can update municipal rate, NOL caps, and penalty rates without developer involvement (0 code deployments for rule changes)
- Rule changes take effect according to configured effective dates with 100% accuracy (no manual code edits)
- Tax calculations retrieve rules from database in under 100ms per calculation (with caching)
- Multi-tenant deployments support 10+ municipalities with independent rule sets, sharing single codebase
- Rule version history provides complete audit trail for state compliance reviews
- What-If Analysis tool shows impact of proposed rule changes on sample returns within 5 seconds

## Assumptions

- Tax rules change 2-4 times per year per municipality (annual rate adjustments + occasional policy changes)
- Rule configuration is performed by tax administrators or CPA firm managers (not end-user taxpayers)
- Complex formulas are limited to basic arithmetic and conditional logic (no scripting language needed in v1)
- Rule approval workflow is single-step (one approver) - multi-step approval is future enhancement
- Rule migration from constants.ts and Java code to database is one-time manual effort by developers
- JSON storage in PostgreSQL jsonb column provides sufficient query performance for rule lookup

## Dependencies

- PostgreSQL database with jsonb column support for flexible rule schema
- IndividualTaxCalculator.java and BusinessTaxCalculator.java refactored to accept TaxRulesConfig parameter instead of using hardcoded constants
- AuthContext extended to include user role "TAX_ADMINISTRATOR" with permission to edit rules
- Redis or in-memory cache for rule storage to avoid database query on every tax calculation
- RuleConfigurationDashboard React component with form validation and temporal rule editing

## Out of Scope

- AI-powered rule suggestion ("Other municipalities use 2.5%, should Dublin increase from 2.0%?") - future enhancement
- Natural language rule definition ("If income exceeds one million, minimum tax is five thousand") - rules use structured format
- Rule testing framework with unit tests per rule - manual What-If Analysis only in v1
- Integration with ordinance management system (document repository for tax ordinances) - manual ordinance references
- Multi-step approval workflow (submitter → reviewer → final approver) - single approver in v1
- Rule localization for multiple languages - English only
- Rule templates or wizards for common patterns - manual rule creation only

## Open Questions

None - specification is complete based on standard enterprise rule engine requirements and municipal tax administration needs.
