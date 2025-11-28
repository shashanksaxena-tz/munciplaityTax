# Research: Multi-State Income Sourcing & Apportionment

**Feature**: Schedule Y - Multi-State Income Sourcing  
**Date**: 2025-11-28  
**Research Phase**: Phase 0

This document consolidates research findings on apportionment methodologies, sourcing rules, and tax calculation standards to support implementation of Schedule Y.

---

## 1. Joyce vs Finnigan Sales Factor Sourcing

### Decision: Implement Both Methods with Finnigan as Default

**Background:**
Joyce and Finnigan are two methods for determining the sales factor denominator in multi-state apportionment when dealing with affiliated corporate groups.

**Joyce Method (Minority Rule - ~15 states):**
- Only sales of entities with nexus in the taxing state are included in the denominator
- Example: Parent (OH nexus, $5M sales) + Sub A (OH nexus, $3M sales) + Sub B (no OH nexus, $2M sales)
  - Denominator = $5M + $3M = $8M (excludes Sub B)
  - OH sales factor = OH sales / $8M

**Finnigan Method (Majority Rule - ~35 states):**
- All sales of the affiliated group are included in the denominator, regardless of whether each entity has nexus
- Same example:
  - Denominator = $5M + $3M + $2M = $10M (includes Sub B)
  - OH sales factor = OH sales / $10M

**Rationale for Default:**
- Finnigan is used by the majority of states (California, Illinois, New York, Pennsylvania, Texas, etc.)
- Ohio case law and administrative guidance lean toward Finnigan interpretation
- Finnigan is more taxpayer-friendly for businesses with out-of-state affiliates (larger denominator = lower factor = less taxable income)
- Most tax software defaults to Finnigan

**Alternatives Considered:**
- Finnigan-only: Rejected because some municipalities may require Joyce by ordinance
- Joyce-only: Rejected because it's minority rule and would over-allocate income
- Auto-detect based on rules: Deferred to rule engine (Spec 4) - system will support both but let configuration determine default

**Implementation:**
- Expose as user-selectable election on Schedule Y form
- Pre-select Finnigan as default
- Allow municipality to override default via rule engine
- Display explanatory text: "Finnigan (recommended): Includes all group sales. Joyce (if required): Only entities with Ohio nexus."

---

## 2. Throwback and Throwout Rules

### Decision: Implement Throwback as Default, Support Throwout as Alternative

**Background:**
Throwback and throwout rules address "nowhere income" - sales that would not be taxed by any state if the business lacks nexus in the destination state.

**Throwback Rule (Majority - ~25 states):**
- If goods are shipped from State A to State B, and the business lacks nexus in State B, the sale is "thrown back" to State A
- Increases the origin state's sales factor numerator
- Example: Ship $100K of goods from Ohio to California customer. No CA nexus → Add $100K to Ohio sales numerator
- Effect: Prevents income from escaping taxation ("nowhere income")

**Throwout Rule (Minority - ~10 states):**
- Alternative to throwback: Discard the sale from both numerator and denominator
- Example: Ship $100K from OH to CA. No CA nexus → Remove $100K from denominator entirely
- Effect: Increases sales factor percentage for states where business has nexus

**No Rule (Minority - ~15 states):**
- Sale is simply assigned to destination state even if no nexus
- Effect: Creates "nowhere income" (income not taxed by any state)

**Ohio Context:**
- Ohio R.C. 718 does not explicitly mandate throwback or throwout
- Municipalities have discretion to adopt either rule via ordinance
- Most Ohio municipalities use throwback rule (Dublin, Columbus, etc.)
- Throwout is used by some municipalities trying to attract businesses (lower effective tax rate)

**Rationale:**
- Throwback is more common and aligns with tax fairness (prevents nowhere income)
- Implementation must support both for flexibility
- Rule engine (Spec 4) will store municipality-specific election

**Alternatives Considered:**
- Throwback-only: Rejected because some municipalities use throwout
- Throwout-only: Rejected because it's minority rule and creates competitive disadvantages
- No-rule default: Rejected because it creates nowhere income and audit issues

**Implementation:**
- Default: Throwback rule (add to origin state numerator)
- Support throwout as alternative (remove from denominator)
- Fetch rule from municipality configuration via rule engine
- Display indicator on Schedule Y: "Sale to CA - No nexus - Thrown back to OH: $100K"
- Allow auditor override with justification in audit log

**Nexus Determination:**
For throwback determination, nexus exists if:
1. Physical presence: Office, warehouse, employees, property in destination state
2. Economic nexus: Sales exceed $500K or 200 transactions per year (post-Wayfair standard)
3. Factor presence: Substantial presence under P.L. 86-272 (15% property/payroll/sales)

System will track nexus status per state/municipality in `nexus_tracking` table.

---

## 3. Market-Based Sourcing for Service Revenue

### Decision: Implement Market-Based as Default, Cost-of-Performance as Fallback

**Background:**
Service revenue sourcing has shifted from cost-of-performance (where work is done) to market-based sourcing (where customer benefits).

**Cost-of-Performance (Historical Method - pre-2010):**
- Service revenue assigned to state where employees perform the work
- Example: IT consulting firm has 5 employees in Ohio, 2 in California. Provides $1M project to NY customer.
  - Allocation: 71% Ohio ($710K), 29% California ($290K), 0% New York
- Rationale: Tax where business has physical presence (payroll, offices)

**Market-Based Sourcing (Modern Method - post-2010):**
- Service revenue assigned to state where customer receives the benefit
- Same example: $1M project to NY customer → 100% assigned to New York ($1M), 0% to OH/CA
- Rationale: Tax where economic activity occurs (customer's market)

**Ohio Transition:**
- Ohio adopted market-based sourcing in 2014 for most services (following MTC model statute)
- Applies to: Professional services (NAICS 54), IT services (NAICS 518-519), management consulting, financial advisory
- Does NOT apply to: Construction services (still cost-of-performance)

**Cascading Rules:**
Market-based sourcing has a hierarchy:
1. **First attempt:** Where customer receives the benefit
   - B2B services: Customer's headquarters or primary business location
   - Multi-location customer: Proportional allocation by where benefit is received (e.g., IT system used by 3 offices → split by office usage)
2. **Fallback:** If customer location is unknown or indeterminable → Use cost-of-performance
3. **Ultimate fallback:** If both unknown → Pro-rata allocation based on overall apportionment percentage

**Rationale:**
- Market-based sourcing is modern standard (30+ states post-2010)
- Aligns with economic nexus trend (Wayfair decision)
- Reflects reality: Service firms serve national customers, not just local
- Ohio R.C. 718.02 adopts market-based for municipal income tax

**Alternatives Considered:**
- Cost-of-performance only: Rejected because it's outdated and conflicts with Ohio law
- Market-based only: Rejected because some service types still use cost-of-performance (construction, installation)
- Auto-detect by NAICS: Partially implemented - use NAICS to suggest default but allow override

**Implementation:**
- Default: Market-based sourcing for NAICS 52, 54, 518, 519
- Display prompt: "Where does the customer receive the benefit?" with state dropdown
- For B2C services (individual consumers): Prompt for customer state or use billing address
- Fallback to cost-of-performance if customer location unknown
- Cost-of-performance allocation by payroll dollars (simpler than tracking employee hours)
- Store sourcing method per transaction for audit trail

---

## 4. Apportionment Formula Variations

### Decision: Support Four-Factor (Double-Weighted Sales) as Default, with Pluggable Formula Engine

**Background:**
States use different apportionment formulas to allocate income. Formulas have evolved from equal-weighted three-factor to sales-weighted to single-sales-factor.

**Historical Three-Factor (Equal-Weighted):**
- Formula: (Property Factor + Payroll Factor + Sales Factor) / 3
- Used by most states until 1990s
- Example: Property 20%, Payroll 40%, Sales 60% → (20 + 40 + 60) / 3 = 40%

**Four-Factor (Double-Weighted Sales) - Ohio Default:**
- Formula: (Property Factor + Payroll Factor + Sales Factor + Sales Factor) / 4
- Sales factor counted twice
- Rationale: Incentivize in-state property/payroll investment while recognizing sales importance
- Example: Property 20%, Payroll 40%, Sales 60% → (20 + 40 + 60 + 60) / 4 = 45%
- Used by Ohio (R.C. 718), Michigan, several other Midwest states

**Single-Sales-Factor (Modern Trend):**
- Formula: Sales Factor only (100% weight)
- Used by Illinois, Iowa, Maryland, and ~12 other states
- Rationale: Highly mobile businesses can shift property/payroll out of state - sales factor is harder to manipulate
- Example: Property 20%, Payroll 40%, Sales 60% → 60% (only sales factor used)

**Custom Weightings:**
- Some states: 50% sales, 25% property, 25% payroll
- Some municipalities: Industry-specific formulas (e.g., financial institutions use different factors)

**Ohio Context:**
- Ohio R.C. 718.02: Four-factor with double-weighted sales is statutory default for municipalities
- Municipalities can adopt alternative formulas by ordinance (rare but possible)
- Some municipalities experimenting with single-sales-factor to attract businesses (Columbus considered in 2020)

**Rationale:**
- Four-factor double-sales is Ohio default - must implement
- Future-proof: Support pluggable formulas via rule engine
- Allow municipality customization without code changes

**Alternatives Considered:**
- Hard-code four-factor only: Rejected because municipalities may adopt alternatives
- Support only common formulas (3-factor, 4-factor, single): Rejected because custom weightings may be needed
- Fully custom formula DSL: Deferred (over-engineering) - current need is fixed formula types

**Implementation:**
- Store formula as enum: TRADITIONAL_THREE_FACTOR, FOUR_FACTOR_DOUBLE_SALES, SINGLE_SALES_FACTOR, CUSTOM
- For CUSTOM: Store weights as JSON: `{ "property": 1, "payroll": 1, "sales": 2 }`
- Fetch formula from rule engine based on municipality + tax year + entity type
- Validation: Sum of factors / sum of weights must yield 0-100% apportionment percentage
- Display formula visually:
  ```
  Property Factor:    20.00%   × 1
  Payroll Factor:     40.00%   × 1
  Sales Factor:       60.00%   × 2
  ────────────────────────────────
  Total:             180.00%   ÷ 4 = 45.00%
  ```

---

## 5. Property Factor Calculation Best Practices

### Decision: Use Beginning/Ending Average, Capitalize Rent at 8x

**Background:**
Property factor calculation requires determining "average property value" over the tax year. Two main methods:

**Averaging Methods:**
1. **Beginning/Ending Average (Most Common):**
   - (Beginning of year value + End of year value) / 2
   - Simplest method, used by most small/medium businesses
   - Ohio allows this method per R.C. 718.02(A)(1)

2. **Monthly Average (More Accurate):**
   - Sum of property values on the first day of each month / 12
   - Better for businesses with significant property fluctuations
   - Required by some states for businesses with >$X threshold

3. **Daily Average (Most Accurate, Rare):**
   - Sum of property values for each day / 365
   - Used only by large businesses with extremely volatile property (e.g., auto dealers with rotating inventory)
   - Administratively burdensome

**Rented Property Treatment:**
- Issue: Business rents property rather than owning - how to value for factor?
- Standard: Multiply annual rent by capitalization multiplier to get property value equivalent
- **Multiplier = 8x** (IRS standard, adopted by most states including Ohio)
- Example: Rent $100K/year → Treated as $800K property value
- Rationale: 8x approximates property value that would generate $100K annual rental income (12.5% return)

**Property Types Included:**
- **Real Property:** Land, buildings, improvements
- **Tangible Personal Property:** Machinery, equipment, vehicles, inventory, furniture
- **Excluded:** Intangible property (patents, trademarks, goodwill, securities)

**Rationale:**
- Beginning/ending average is simplest and meets Ohio requirements
- 8x rent multiplier is industry standard
- Support monthly average as optional enhancement for sophisticated filers

**Implementation:**
- Default: Beginning/ending average method
- UI prompts: "Beginning of year property value" and "End of year property value"
- Rented property section: "Annual rent paid: $____. System will multiply by 8 to approximate property value."
- Validation: Ending value should not be wildly different from beginning (flag if >200% change)
- Store raw values (beginning, ending, rent) plus calculated average for audit trail

---

## 6. Remote Employee Payroll Allocation

### Decision: Allocate by Primary Work Location, Support Multi-State Employees

**Background:**
Remote work has complicated payroll factor calculation. Key question: Which state gets credit for a remote employee's payroll?

**Traditional Rule: Physical Worksite:**
- Payroll assigned to state where employee performs services
- Example: Employee works in Ohio office → Ohio payroll

**Remote Work Complications:**
1. **Home-state employee (works from home in different state):**
   - Employee lives in Pennsylvania, works remotely for Ohio company
   - Payroll assigned to Pennsylvania (where services are performed), not Ohio

2. **Multi-state employee (travels for work):**
   - Employee is based in Ohio, travels to clients in multiple states (20 days NY, 30 days CA, rest in OH)
   - Options:
     - **Assignment method:** Prorate by days worked in each state
     - **Primary location method:** Assign 100% to base state (Ohio)
   - Most states require assignment method for material out-of-state work (>30 days)

3. **Convenience rule (minority - NY, NE, DE):**
   - If employee works remotely for personal convenience (not employer requirement), payroll assigned to employer's state
   - Example: NY employer allows employee to work from FL → Still taxed by NY as "convenience"
   - Ohio does NOT have convenience rule (follows physical location)

**Ohio Approach:**
- R.C. 718.02(A)(2): Payroll assigned to state where "compensation is paid for services performed"
- For remote employees: Assign to state where employee physically works
- For multi-state employees: Prorate if material presence (>30 days/year or >10% of time)

**Rationale:**
- Physical worksite rule is clear, objective, and matches Ohio law
- Multi-state proration is necessary for accuracy (e.g., traveling sales reps)
- Convenience rule NOT applicable in Ohio (simplifies implementation)

**Alternatives Considered:**
- Primary location only: Rejected because it inaccurately allocates payroll for employees who work 6 months in another state
- Track exact days per state: Ideal but administratively burdensome - use percentage-based allocation instead
- Ignore remote work: Rejected because it creates inaccurate factors and audit risk

**Implementation:**
- Default: Single primary work location per employee (dropdown to select state)
- Advanced option: "Multi-state allocation" - enter percentage of payroll by state
  - Example: Employee total comp $100K → 70% OH ($70K), 20% CA ($20K), 10% NY ($10K)
- Validation: Percentages must sum to 100%
- Store allocation in `remote_employee_allocation` JSON field
- Display on Schedule Y: "Remote employees: 5 employees, $500K total payroll → $350K OH, $100K CA, $50K NY"

---

## 7. Nexus Determination Standards (Post-Wayfair)

### Decision: Implement Economic Nexus ($500K or 200 Transactions) Plus Physical Presence

**Background:**
Nexus (the connection between a business and a state that gives the state taxing authority) has evolved significantly after the 2018 Wayfair decision.

**Pre-Wayfair (Physical Presence Only):**
- Nexus only if: Office, property, employees in state
- Remote sellers with no physical presence → No nexus, not taxed
- Created "sales tax loophole" for online retailers

**Post-Wayfair (Economic Nexus):**
- U.S. Supreme Court: Physical presence NOT required
- Economic nexus established if sales exceed threshold
- Standard threshold (South Dakota model): **$100K sales OR 200 transactions** per year
- Ohio adopted economic nexus for sales tax in 2018, for income tax in 2019

**Municipal Income Tax Nexus (Ohio R.C. 718):**
- Physical presence: Office, property, employees, or agent in municipality
- Economic nexus: Gross receipts > **$500K** (higher than sales tax threshold)
- **Note:** Ohio municipalities use $500K (not $100K) to avoid over-burdening small remote businesses
- Transactions count NOT used for income tax (only dollar threshold)

**Factor Presence Nexus (P.L. 86-272 Protection):**
- P.L. 86-272: Federal law prohibits states from taxing out-of-state sellers of tangible goods if their only activity is solicitation
- Exception: If business has substantial factor presence (property, payroll, non-solicitation activities), nexus established
- Rough guideline: >15% of property/payroll/sales in state → Factor presence nexus

**Rationale:**
- Economic nexus is now the standard (post-Wayfair)
- $500K threshold matches Ohio municipal income tax law
- Physical presence still creates nexus (lower bar than economic)
- Factor presence needed for edge cases (e.g., consignment inventory)

**Alternatives Considered:**
- Physical presence only: Rejected (outdated, conflicts with Wayfair and Ohio law)
- Use $100K threshold: Rejected (Ohio municipalities use $500K for income tax, not sales tax threshold)
- Track transactions: Rejected for income tax (transaction count not used in Ohio R.C. 718)

**Implementation:**
- Track nexus per state/municipality in `nexus_tracking` table
- Nexus established if ANY of:
  1. Physical presence: Has office/property/employees in state
  2. Economic nexus: `sales_in_state >= $500,000`
  3. Factor presence: `(property_in_state / total_property) > 0.15 OR (payroll_in_state / total_payroll) > 0.15`
- Update nexus status annually at year-end
- Use for throwback determination: If `has_nexus = false` in destination state → Apply throwback rule
- Display on Schedule Y: "Nexus Status: OH (Physical + Economic), CA (Economic only), NY (No nexus - throwback applies)"

---

## 8. Decimal Precision and Rounding for Tax Calculations

### Decision: Use BigDecimal with 10 Decimal Places, Round Final Results to 4 Decimals

**Background:**
Tax calculations require high precision to avoid rounding errors that compound across multiple calculations.

**Precision Requirements:**
- **Factors (percentages):** Display as percentages with 2 decimal places (e.g., 45.67%), store with 4 decimals (0.4567)
- **Dollar amounts:** Store as cents (no decimals) or use BigDecimal with 2 decimals
- **Intermediate calculations:** Maintain high precision (8-10 decimals) to avoid rounding errors
- **Final apportionment percentage:** Round to 4 decimals (0.4567 = 45.67%)

**Why BigDecimal (Java) / Decimal (PostgreSQL):**
- Floating-point (double, float) has precision errors: `0.1 + 0.2 = 0.30000000000000004`
- Tax calculations must be deterministic and reproducible
- Audit defense requires exact calculations
- Java: Use `BigDecimal` with `MathContext(10, RoundingMode.HALF_UP)`
- PostgreSQL: Use `NUMERIC(20, 10)` for intermediate values, `NUMERIC(14, 4)` for percentages

**Rounding Strategy:**
- **During calculation:** Maintain full precision (10 decimals)
- **For display:** Round to 2-4 decimals as appropriate
- **For storage:** Store full precision to allow recalculation
- **Rounding mode:** HALF_UP (standard rounding: 0.5 rounds up)

**Example:**
```java
// Bad: double arithmetic
double propertyFactor = 2000000.0 / 10000000.0; // 0.19999999999999998

// Good: BigDecimal arithmetic
BigDecimal ohioProperty = new BigDecimal("2000000.00");
BigDecimal totalProperty = new BigDecimal("10000000.00");
BigDecimal propertyFactor = ohioProperty.divide(totalProperty, 10, RoundingMode.HALF_UP);
// Result: 0.2000000000 (exact)
```

**Rationale:**
- Tax software must produce identical results every time (reproducibility)
- Auditors will check calculations manually - must match exactly
- Rounding errors can accumulate in multi-step calculations
- IRS and state tax agencies require exact calculations

**Alternatives Considered:**
- Use floating-point (double): Rejected (precision errors create audit risk)
- Use integers (store cents): Possible but complicates percentage calculations
- Use lower precision (2 decimals): Rejected (compounds rounding errors in multi-step calculations)

**Implementation:**
- Java: All monetary calculations use `BigDecimal`, all percentages use `BigDecimal` with 10 decimals
- PostgreSQL: `NUMERIC(20, 2)` for dollar amounts, `NUMERIC(14, 10)` for factors/percentages
- Display: Format as percentage with 2-4 decimals: `factorValue.setScale(4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) + "%"`
- Validation: Factors must be 0.0000 to 1.0000 (0% to 100%)

---

## 9. Multi-Municipality Allocation Within Ohio

### Decision: Support Separate Apportionment per Municipality, with JEDD Support

**Background:**
Businesses operating in multiple Ohio municipalities must allocate income to each municipality separately. Each municipality has its own tax rate and may have different apportionment rules.

**Scenarios:**
1. **Single-municipality business:**
   - Office in Columbus only → 100% of apportioned income taxed by Columbus at 2.5% rate

2. **Multi-municipality business:**
   - Offices in Columbus (2.5% rate) and Cleveland (2.0% rate)
   - Must calculate separate apportionment for each: "What % of income is attributable to Columbus? To Cleveland?"
   - May need nested apportionment: First apportion to Ohio (vs other states), then apportion within Ohio (Columbus vs Cleveland)

3. **JEDD (Joint Economic Development District):**
   - Special zones where multiple municipalities share tax base (e.g., Columbus-Grove City JEDD)
   - Income generated in JEDD is split between municipalities per pre-negotiated percentages (e.g., 60% Columbus, 40% Grove City)
   - Adds complexity: Must track which property/payroll/sales occur in JEDD vs regular municipality

**Ohio Rules (R.C. 718.02):**
- Each municipality calculates apportionment independently
- Business may have different apportionment percentages in different municipalities
- Example: Company has 80% of property in Columbus but 50% of sales in Cleveland → Different factors for each city

**Rationale:**
- Multi-municipality businesses are common (40%+ of business filers)
- Each municipality is a separate taxing jurisdiction with separate rates/rules
- JEDD zones require special handling (legal requirement per JEDD agreements)

**Alternatives Considered:**
- Single apportionment for all Ohio: Rejected (violates Ohio law - each municipality is separate)
- Ignore JEDD: Rejected (legal requirement per R.C. 718.02(D))
- Manual JEDD allocation: Possible but error-prone - better to automate

**Implementation:**
- Support multiple `ScheduleY` records per tax return (one per municipality)
- Each Schedule Y has separate property/payroll/sales factors specific to that municipality
- JEDD support:
  - Flag property/payroll/sales as "in JEDD zone" (boolean field)
  - Fetch JEDD allocation percentages from rule engine (e.g., `{ "Columbus": 0.6, "Grove City": 0.4 }`)
  - Prorate JEDD amounts across municipalities per agreement
- Display: "Schedule Y - Columbus: 45% apportionment. Schedule Y - Cleveland: 30% apportionment."
- Validation: Sum of all municipality apportionments should equal overall Ohio apportionment (with ±5% tolerance)

---

## 10. Audit Trail Requirements

### Decision: Immutable Append-Only Audit Log with Before/After State

**Background:**
Tax calculations are subject to audit by municipalities, CPAs, and IRS. Complete audit trail is non-negotiable.

**Audit Trail Requirements:**
1. **What changed:** Factor value, election, transaction, nexus status
2. **Who changed it:** User ID, role (filer, CPA, auditor)
3. **When:** Timestamp (UTC)
4. **Why:** User-provided reason (optional for taxpayer, required for auditor overrides)
5. **Before/After:** Old value and new value (JSON for complex objects)

**Events to Log:**
- Election changed: Joyce → Finnigan, Throwback → Throwout, Market-based → Cost-of-performance
- Factor recalculated: Property value updated, payroll corrected, sales transaction added
- Transaction added/modified: New sale entered, customer location changed, sourcing method changed
- Nexus status changed: New state nexus established, economic nexus threshold crossed
- Apportionment percentage changed: Final apportionment recalculated

**Audit Log Structure:**
```sql
apportionment_audit_log:
- audit_log_id (UUID)
- schedule_y_id (UUID FK)
- change_type (enum)
- changed_by (UUID)
- change_date (timestamp)
- old_value (JSONB)
- new_value (JSONB)
- change_reason (text, nullable)
- affected_calculation (text) -- e.g., "property_factor", "throwback_adjustment"
```

**Retention:**
- 7+ years per IRS requirements
- Immutable: No updates or deletes allowed
- Tenant-scoped: Audit logs stored in tenant schema (multi-tenant isolation)

**Rationale:**
- Audit defense: Prove calculations are correct and changes were justified
- Transparency: Taxpayers can see calculation history
- Compliance: IRS Pub 1075 requires audit trail for federal tax information
- Debugging: Developers can trace calculation errors

**Alternatives Considered:**
- Log only final values: Rejected (cannot reconstruct calculation history)
- Log in application logs: Rejected (not queryable, not immutable, not tenant-scoped)
- Use database triggers: Possible but harder to add context (user ID, reason)

**Implementation:**
- `ApportionmentAuditService.logChange(scheduleYId, changeType, oldValue, newValue, reason)` called after every modification
- Store old/new values as JSON to handle complex objects (e.g., full `SalesFactor` object)
- Display audit trail on Schedule Y page: "History" tab shows all changes with before/after diff
- Allow auditors to add notes: "Verified throwback adjustment - confirmed no CA nexus per taxpayer documentation"

---

## Summary of Research Findings

### Key Decisions Documented:
1. ✅ Joyce vs Finnigan: Support both, default to Finnigan (majority rule)
2. ✅ Throwback/Throwout: Support both, default to throwback (prevents nowhere income)
3. ✅ Service Sourcing: Market-based default, cost-of-performance fallback
4. ✅ Apportionment Formula: Four-factor double-sales default, support pluggable formulas
5. ✅ Property Factor: Beginning/ending average, 8x rent capitalization
6. ✅ Remote Payroll: Allocate by primary work location, support multi-state proration
7. ✅ Nexus Standards: Economic nexus $500K + physical presence + factor presence
8. ✅ Decimal Precision: BigDecimal with 10 decimals, round to 4 for display
9. ✅ Multi-Municipality: Separate Schedule Y per municipality, JEDD support
10. ✅ Audit Trail: Immutable append-only log with before/after state

### All NEEDS CLARIFICATION Items Resolved

Technical context is now complete. Proceeding to Phase 1: Design & Contracts.
