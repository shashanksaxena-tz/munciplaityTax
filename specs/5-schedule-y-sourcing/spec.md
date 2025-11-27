# Schedule Y: Multi-State Income Sourcing Rules

**Feature Name:** Multi-State Income Sourcing & Apportionment  
**Priority:** HIGH  
**Status:** Specification  
**Created:** 2025-11-27

---

## Overview

Implement comprehensive multi-state income sourcing rules for businesses operating in multiple jurisdictions, including Joyce vs Finnigan election for sales factor sourcing, throwback/throwout rules, market-based sourcing methodologies, and service revenue attribution. This feature is critical for 40%+ of business filers who have multi-state operations and must accurately allocate income to each jurisdiction.

**Current State:** System only supports single-jurisdiction reporting. Multi-state businesses must manually calculate apportionment outside the system.

**Target Users:** Business filers with operations in multiple states/municipalities, CPAs preparing multi-state returns, tax managers, auditors reviewing apportionment calculations.

---

## User Scenarios & Testing

### US-1: Multi-State Business Elects Finnigan Method (P1 - Critical)

**User Story:**  
As a multi-state business CFO, I want to elect Finnigan sales factor sourcing so that I can include all sales made by my affiliated group when calculating my Ohio apportionment factor, consistent with how most states calculate nexus and apportionment.

**Business Context:**  
Under Finnigan method (used by majority of states), all sales of the affiliated group are included in the sales factor denominator, regardless of whether each entity individually has nexus in Ohio. Under Joyce method (minority rule), only sales of entities with Ohio nexus are included. This can result in 10-30% differences in taxable income allocation.

**Independent Test:**  
Given a multi-state corporate group with 3 entities: Parent (OH nexus, $5M sales everywhere, $1M OH sales), Sub A (OH nexus, $3M sales everywhere, $500K OH sales), Sub B (no OH nexus, $2M sales everywhere, $0 OH sales):
- Under Finnigan: OH apportionment = ($1M + $500K) / ($5M + $3M + $2M) = 15%
- Under Joyce: OH apportionment = ($1M + $500K) / ($5M + $3M) = 18.75%

**Acceptance Criteria:**
- GIVEN a business with multi-state operations
- WHEN filing Schedule Y (Apportionment)
- THEN system MUST display election choice: "Finnigan Method (Include all group sales)" vs "Joyce Method (Include only nexus entity sales)"
- AND pre-select Finnigan as default (majority rule)
- AND allow taxpayer to override to Joyce if required
- AND calculate sales factor denominator accordingly
- AND display calculation breakdown showing which entities are included/excluded

---

### US-2: Apply Throwback Rule for Destination State Without Nexus (P1 - Critical)

**User Story:**  
As a CPA preparing multi-state tax returns, I want the system to automatically apply throwback rules when my client ships goods to states where they lack nexus, so that sales are correctly "thrown back" to the origin state and included in the Ohio sales factor numerator.

**Business Context:**  
Throwback rule: If a business ships goods to State B where it lacks nexus (and State B cannot tax the sale), the sale is "thrown back" to the origin state (e.g., Ohio) and included in the Ohio sales factor numerator. This prevents "nowhere income" that escapes taxation in all states. About 25 states have throwback rules.

**Independent Test:**  
Company ships $100K of goods from Ohio warehouse to California customer. Company has no California nexus (no office, employees, or property in CA):
- Without throwback: OH sales factor numerator = $0 (shipped to CA)
- With throwback: OH sales factor numerator = $100K (thrown back to OH because no CA nexus)

**Acceptance Criteria:**
- GIVEN a sale shipped from Ohio to another state
- WHEN the business lacks nexus in the destination state
- THEN system MUST apply throwback rule
- AND add the sale amount to Ohio sales factor numerator
- AND display throwback indicator on Schedule Y showing: "Sale to [State] - No nexus - Thrown back to OH"
- AND support throwout rule as alternative (discard the sale from both numerator and denominator) if municipality elects throwout instead

---

### US-3: Market-Based Sourcing for Service Revenue (P1 - Critical)

**User Story:**  
As a professional services firm (consulting, IT, legal), I want the system to source my service revenue based on where my customers receive the benefit (market-based sourcing), rather than where my employees perform the work (cost-of-performance), so that my multi-state apportionment reflects modern economic nexus rules.

**Business Context:**  
Historically, service revenue was sourced using cost-of-performance (where employees work). Modern trend (adopted by 30+ states post-2010) is market-based sourcing (where customers are located). This shift significantly impacts service businesses. Ohio follows market-based sourcing for most services.

**Independent Test:**  
IT consulting firm has office in Ohio (5 employees) and California (2 employees). Provides $1M consulting project to New York customer (customer is NY-based Fortune 500, benefit received in NY):
- Cost-of-performance: Apportion based on employee time: 70% OH, 30% CA → NY gets $0
- Market-based: 100% to NY (where customer receives benefit) → OH gets $0, CA gets $0

**Acceptance Criteria:**
- GIVEN service revenue transaction
- WHEN entering service income on Schedule C or Schedule E
- THEN system MUST prompt: "Where does the customer receive the benefit?" (market-based) or "Where did your employees perform the work?" (cost-of-performance)
- AND default to market-based sourcing (modern rule)
- AND for market-based: assign 100% of revenue to customer's state/municipality
- AND for cost-of-performance: assign revenue proportionally to where employees worked (by payroll, days, or hours)
- AND support cascading rules: Try market-based first; if customer location unknown, fall back to cost-of-performance

---

### US-4: Display Apportionment Factor Calculation with Breakdown (P2 - High Value)

**User Story:**  
As a business filer, I want to see a clear breakdown of my three-factor apportionment formula (property, payroll, sales) with double-weighted sales, so that I understand how my Ohio taxable income is calculated and can verify the math.

**Business Context:**  
Ohio uses three-factor apportionment formula: (Property Factor + Payroll Factor + Sales Factor + Sales Factor) / 4. Sales factor is double-weighted. Each factor ranges 0-100%. Many businesses don't understand how this works and need visual breakdown.

**Independent Test:**  
Business has:
- Property: $2M OH, $8M everywhere → Factor = 20%
- Payroll: $3M OH, $7M everywhere → Factor = 42.86%
- Sales: $5M OH, $10M everywhere → Factor = 50%
- Apportionment: (20% + 42.86% + 50% + 50%) / 4 = 40.715%

**Acceptance Criteria:**
- GIVEN completed Schedule Y with property, payroll, and sales data
- WHEN calculating apportionment percentage
- THEN system MUST display breakdown:
  ```
  Property Factor:    20.00%   ($2M OH / $8M total)
  Payroll Factor:     42.86%   ($3M OH / $7M total)
  Sales Factor:       50.00%   ($5M OH / $10M total)
  Sales Factor (2x):  50.00%   (double-weighted)
  ────────────────────────────
  Total:             162.86%   (sum of 4 factors)
  Apportionment:      40.72%   (162.86% / 4)
  ```
- AND highlight which factors are weighted (e.g., sales factor shown twice)
- AND explain formula in tooltip: "Ohio uses 4-factor formula with double-weighted sales"

---

### US-5: Handle Single-Sales-Factor Election (P3 - Future Enhancement)

**User Story:**  
As a business with minimal property and payroll in Ohio but significant sales, I want to elect single-sales-factor apportionment (if allowed by municipality), so that my apportionment percentage is based solely on my sales factor, potentially reducing my Ohio tax liability.

**Business Context:**  
Some states (e.g., Illinois, Iowa, Maryland) have moved to single-sales-factor formulas to incentivize in-state employment and property investment. Ohio currently uses 4-factor (double-weighted sales), but municipalities may experiment with single-sales-factor in future.

**Independent Test:**  
Business has:
- Property: 5% OH
- Payroll: 10% OH
- Sales: 60% OH
- Traditional 4-factor: (5% + 10% + 60% + 60%) / 4 = 33.75%
- Single-sales-factor: 60%

If allowed to elect single-sales-factor, business would pay tax on 60% vs 33.75% apportionment. In this case, business would NOT elect it (higher apportionment = more tax). But reverse scenario (low sales, high property/payroll) would benefit from traditional formula.

**Acceptance Criteria:**
- GIVEN municipality allows single-sales-factor election
- WHEN business files Schedule Y
- THEN system MUST display election option: "Single-Sales-Factor (Sales only)" vs "Traditional Four-Factor Formula"
- AND calculate both scenarios side-by-side
- AND recommend the option that minimizes tax liability
- AND save election for future years (binding election, cannot change year-to-year)

---

## Functional Requirements

### Apportionment Formula Configuration

**FR-001:** System MUST support multiple apportionment formulas:
- Traditional three-factor (property, payroll, sales) equally weighted
- Four-factor with double-weighted sales (Ohio default)
- Single-sales-factor
- Custom weightings (e.g., 50% sales, 25% property, 25% payroll)

**FR-002:** System MUST retrieve apportionment formula from rule engine based on:
- Tax year (formulas change over time)
- Entity type (C-Corp, S-Corp, Partnership may have different rules)
- Industry code (NAICS) - some states have industry-specific formulas

**FR-003:** System MUST calculate each factor as percentage: (Ohio amount) / (Everywhere amount) * 100%

**FR-004:** System MUST apply factor weightings per formula rules (e.g., double-weight sales factor)

**FR-005:** System MUST calculate final apportionment percentage: (Sum of weighted factors) / (Sum of weights)

### Sales Factor Sourcing Elections

**FR-006:** System MUST support Joyce vs Finnigan election for sales factor:
- Finnigan (default): Include all group sales in denominator, regardless of nexus
- Joyce (minority rule): Include only sales of entities with Ohio nexus in denominator

**FR-007:** System MUST display election choice on Schedule Y with explanation:
- "Finnigan Method: Includes sales from all affiliated entities, even if they lack Ohio nexus. Used by majority of states."
- "Joyce Method: Includes only sales from entities with Ohio nexus. Use if explicitly required by municipality."

**FR-008:** System MUST save sourcing method election to business profile

**FR-009:** System MUST apply elected method consistently across all tax years unless explicitly changed

**FR-010:** System MUST validate election against municipality rules (e.g., block Joyce if municipality mandates Finnigan)

### Throwback/Throwout Rules

**FR-011:** System MUST determine if throwback rule applies for each sale:
- Sale shipped from Ohio to another state/municipality
- Business lacks nexus in destination state (no office, employees, property, or sales above threshold)
- Destination state would not tax the sale

**FR-012:** System MUST apply throwback rule by adding sale to Ohio sales factor numerator

**FR-013:** System MUST support throwout rule (alternative to throwback): Exclude sale from both numerator and denominator

**FR-014:** System MUST retrieve throwback/throwout election from rule engine (municipality-specific policy)

**FR-015:** System MUST display throwback adjustments on Schedule Y with line items:
- "Sales to [State] - No nexus - Thrown back: $[amount]"

**FR-016:** System MUST track nexus by state/municipality for throwback determination:
- Nexus established if: office, employees, property, or sales > $500K (economic nexus threshold)

### Market-Based Sourcing for Services

**FR-017:** System MUST determine sourcing method for service revenue:
- Market-based sourcing (modern): Where customer receives benefit
- Cost-of-performance (historical): Where employees perform work

**FR-018:** System MUST default to market-based sourcing for service industries:
- Professional services (NAICS 54): Consulting, legal, accounting, engineering
- Information technology (NAICS 518-519)
- Management services
- Financial services (NAICS 52)

**FR-019:** System MUST prompt user for customer location when using market-based sourcing:
- "Where is the customer headquartered?" (for B2B services)
- "Where does the customer receive the benefit?" (if multi-location customer)

**FR-020:** System MUST support cascading sourcing rules:
- First attempt: Market-based (customer location)
- If customer location unknown: Cost-of-performance (employee location)
- If both unknown: Pro-rata apportionment based on overall apportionment percentage

**FR-021:** System MUST source 100% of service revenue to single state when using market-based sourcing (no proration)

**FR-022:** System MUST prorate service revenue by employee location when using cost-of-performance:
- Options: By payroll dollars, by employee days worked, by employee hours
- Default: By payroll dollars

### Multi-Jurisdiction Support

**FR-023:** System MUST support apportionment to multiple municipalities within Ohio:
- Example: Columbus (2.5% rate), Cleveland (2.0% rate), Cincinnati (2.1% rate)
- Calculate separate apportionment percentage for each municipality

**FR-024:** System MUST prevent double-taxation by tracking which municipalities have already taxed the income

**FR-025:** System MUST support JEDD (Joint Economic Development District) allocation:
- Special zones where multiple municipalities share tax base
- Pro-rata allocation based on JEDD agreement percentages

**FR-026:** System MUST allocate income to home municipality vs work municipality for resident businesses

### Property Factor Calculation

**FR-027:** System MUST calculate property factor: (Ohio property value) / (Total property value everywhere)

**FR-028:** System MUST use average property values for the year: (Beginning value + Ending value) / 2

**FR-029:** System MUST include both real property (land, buildings) and tangible personal property (equipment, vehicles, inventory)

**FR-030:** System MUST exclude intangible property (patents, trademarks, goodwill) from property factor

**FR-031:** System MUST handle rented property: Multiply annual rent by 8 to get property value (capitalization rate)

### Payroll Factor Calculation

**FR-032:** System MUST calculate payroll factor: (Ohio payroll) / (Total payroll everywhere)

**FR-033:** System MUST include W-2 wages, contractor payments (1099-NEC), and officer compensation

**FR-034:** System MUST assign payroll to state based on employee's primary work location (not home address)

**FR-035:** System MUST handle remote employees: Assign to state where employee performs services, not where company is headquartered

**FR-036:** System MUST exclude payroll of employees working in states where business lacks nexus (under Joyce method only)

### Sales Factor Calculation

**FR-037:** System MUST calculate sales factor: (Ohio sales) / (Total sales everywhere)

**FR-038:** System MUST source tangible goods sales to destination state (where goods are shipped/delivered)

**FR-039:** System MUST source service revenue per market-based or cost-of-performance rules (FR-017 to FR-022)

**FR-040:** System MUST source rental income to state where property is located

**FR-041:** System MUST source interest income to state where borrower is located (market-based)

**FR-042:** System MUST source royalty income to state where intellectual property is used

### Display & Reporting

**FR-043:** System MUST display Schedule Y (Apportionment) with sections:
- Property Factor: Ohio property, Total property, Percentage
- Payroll Factor: Ohio payroll, Total payroll, Percentage
- Sales Factor: Ohio sales, Total sales, Percentage
- Weighted Formula: Show factor weights and final apportionment percentage

**FR-044:** System MUST display sourcing method elections prominently:
- Joyce vs Finnigan
- Throwback vs Throwout
- Market-based vs Cost-of-performance

**FR-045:** System MUST display calculation breakdown with line-by-line detail:
- Each factor with numerator, denominator, percentage
- Applied weights
- Final apportionment percentage

**FR-046:** System MUST generate PDF Form 27-Y (Schedule Y) with all factors and calculations

**FR-047:** System MUST support multi-year comparison: Show apportionment percentages for current year, prior year, and 3-year average

### Validation & Audit Support

**FR-048:** System MUST validate apportionment factors:
- Each factor percentage must be 0-100%
- Final apportionment percentage must be 0-100%
- Sum of all state apportionment percentages should equal approximately 100% (allow ±5% variance due to throwback/throwout)

**FR-049:** System MUST flag inconsistencies:
- Property factor 0% but business reports Ohio office address
- Payroll factor 0% but business has Ohio employees (from W-2 data)
- Sales factor >100% (impossible unless data entry error)

**FR-050:** System MUST create audit trail for apportionment:
- Log sourcing method elections
- Log each factor calculation with supporting data
- Log throwback adjustments with rationale (no nexus in destination state)

---

## Key Entities

### ScheduleY (Apportionment Schedule)

Represents the multi-state apportionment calculation for a business tax return.

**Attributes:**
- `scheduleYId` (UUID): Unique identifier
- `returnId` (UUID): Foreign key to BusinessTaxReturn
- `taxYear` (number): Tax year (2024, 2025, etc.)
- `apportionmentFormula` (enum): TRADITIONAL_THREE_FACTOR | FOUR_FACTOR_DOUBLE_SALES | SINGLE_SALES_FACTOR | CUSTOM
- `formulaWeights` (object): `{ property: 1, payroll: 1, sales: 2 }` for double-weighted sales
- `propertyFactorPercentage` (decimal): 0-100%
- `payrollFactorPercentage` (decimal): 0-100%
- `salesFactorPercentage` (decimal): 0-100%
- `finalApportionmentPercentage` (decimal): Calculated weighted average
- `sourcingMethodElection` (enum): FINNIGAN | JOYCE
- `throwbackElection` (enum): THROWBACK | THROWOUT | NONE
- `serviceSourcingMethod` (enum): MARKET_BASED | COST_OF_PERFORMANCE
- `createdDate` (timestamp)
- `lastModifiedDate` (timestamp)

---

### PropertyFactor

Represents property values used in apportionment calculation.

**Attributes:**
- `propertyFactorId` (UUID)
- `scheduleYId` (UUID): Foreign key
- `ohioRealProperty` (decimal): Ohio land & buildings
- `ohioTangiblePersonalProperty` (decimal): Ohio equipment, inventory
- `ohioRentedProperty` (decimal): Annual rent × 8 capitalization
- `totalOhioProperty` (decimal): Sum of above
- `totalPropertyEverywhere` (decimal): Denominator
- `propertyFactorPercentage` (decimal): (Ohio / Everywhere) × 100%
- `averagingMethod` (enum): AVERAGE_BEGINNING_ENDING | MONTHLY_AVERAGE | DAILY_AVERAGE
- `beginningOfYearValue` (decimal)
- `endOfYearValue` (decimal)

---

### PayrollFactor

Represents payroll used in apportionment calculation.

**Attributes:**
- `payrollFactorId` (UUID)
- `scheduleYId` (UUID): Foreign key
- `ohioW2Wages` (decimal): W-2 wages for Ohio employees
- `ohioContractorPayments` (decimal): 1099-NEC for Ohio contractors
- `ohioOfficerCompensation` (decimal): Officer W-2 wages
- `totalOhioPayroll` (decimal): Sum of above
- `totalPayrollEverywhere` (decimal): Denominator
- `payrollFactorPercentage` (decimal): (Ohio / Everywhere) × 100%
- `employeeCount` (number): Total employees
- `ohioEmployeeCount` (number): Employees working primarily in Ohio
- `remoteEmployeeAllocation` (object): `{ OH: $500K, CA: $300K, NY: $200K }`

---

### SalesFactor

Represents sales used in apportionment calculation.

**Attributes:**
- `salesFactorId` (UUID)
- `scheduleYId` (UUID): Foreign key
- `ohioSalesTangibleGoods` (decimal): Sales of physical products delivered to Ohio
- `ohioSalesServices` (decimal): Service revenue sourced to Ohio
- `ohioSalesRentalIncome` (decimal): Rental income from Ohio property
- `ohioSalesInterest` (decimal): Interest income sourced to Ohio
- `ohioSalesRoyalties` (decimal): Royalty income sourced to Ohio
- `ohioSalesOther` (decimal): Other income sourced to Ohio
- `throwbackAdjustment` (decimal): Sales thrown back to Ohio (no nexus in destination)
- `totalOhioSales` (decimal): Sum of above
- `totalSalesEverywhere` (decimal): Denominator
- `salesFactorPercentage` (decimal): (Ohio / Everywhere) × 100%

---

### SaleTransaction

Represents individual sale transaction for detailed sourcing.

**Attributes:**
- `transactionId` (UUID)
- `salesFactorId` (UUID): Foreign key
- `transactionDate` (date)
- `customerName` (string)
- `saleAmount` (decimal)
- `saleType` (enum): TANGIBLE_GOODS | SERVICES | RENTAL_INCOME | INTEREST | ROYALTIES | OTHER
- `originState` (string): Where goods shipped from or services performed
- `destinationState` (string): Where goods delivered or customer located
- `sourcingMethod` (enum): DESTINATION | MARKET_BASED | COST_OF_PERFORMANCE | THROWBACK
- `hasDestinationNexus` (boolean): Does business have nexus in destination state?
- `allocatedState` (string): Final state assignment after throwback rules
- `allocatedAmount` (decimal): Amount allocated to state (may differ from saleAmount for service proration)

---

### NexusTracking

Tracks nexus status in each state/municipality for throwback determination.

**Attributes:**
- `nexusId` (UUID)
- `businessId` (UUID): Foreign key
- `taxYear` (number)
- `state` (string): State code (OH, CA, NY, etc.)
- `municipality` (string): City/county name
- `hasNexus` (boolean)
- `nexusReasons` (array): Reasons nexus established
  - `PHYSICAL_PRESENCE` (office or property)
  - `EMPLOYEE_PRESENCE` (employees working in state)
  - `ECONOMIC_NEXUS` (sales > $500K threshold)
  - `FACTOR_PRESENCE` (P.L. 86-272 substantial presence)
- `salesInState` (decimal): Total sales in state (for economic nexus tracking)
- `propertyInState` (decimal): Property value in state
- `payrollInState` (decimal): Payroll in state
- `employeeCountInState` (number)

---

### ApportionmentAuditLog

Audit trail for apportionment calculations and elections.

**Attributes:**
- `auditLogId` (UUID)
- `scheduleYId` (UUID): Foreign key
- `changeType` (enum): ELECTION_CHANGED | FACTOR_RECALCULATED | TRANSACTION_ADDED | NEXUS_CHANGED
- `changedBy` (UUID): User who made change
- `changeDate` (timestamp)
- `oldValue` (string): Previous value (JSON)
- `newValue` (string): New value (JSON)
- `changeReason` (string): User-provided explanation
- `affectedCalculation` (string): Which factor/calculation was affected

---

## Success Criteria

- **Multi-State Coverage:** 90%+ of multi-state business filers correctly calculate apportionment using system (vs current 0% - manual calculations required)
- **Throwback Accuracy:** System correctly identifies 100% of sales subject to throwback (no nexus in destination state) and adjusts Ohio sales factor
- **Service Sourcing Compliance:** Service revenue sourced using market-based method by default, matching modern state rules (vs historical cost-of-performance)
- **Audit Defense:** Apportionment calculations pass audit with zero adjustments (complete audit trail, documented sourcing methods, factor breakdowns)
- **Time Savings:** Multi-state filers complete Schedule Y in 20 minutes vs 2-3 hours of manual spreadsheet calculations
- **CPA Adoption:** CPAs preparing multi-state returns rate Schedule Y feature 8+/10 (comparable to professional tax software)

---

## Assumptions

- Ohio uses four-factor apportionment formula with double-weighted sales factor (most common formula)
- Finnigan method is default (majority rule in US states)
- Throwback rule applies unless municipality explicitly elects throwout
- Market-based sourcing is default for services (modern trend, adopted post-2010)
- Economic nexus threshold is $500K sales or 200 transactions per year (post-Wayfair standard)
- Property factor uses average of beginning and ending year values
- Rented property capitalized at 8x annual rent (standard multiplier)
- Business maintains adequate records to support apportionment (sales by destination, employee locations, property values)

---

## Dependencies

- **Rule Engine (Spec 4):** Apportionment formulas, throwback/throwout elections, sourcing methods stored as configurable rules
- **Withholding Reconciliation (Spec 1):** Payroll data from W-1 filings feeds into payroll factor
- **Enhanced Discrepancy Detection (Spec 3):** Validate apportionment factors against W-2 data, property tax returns, sales records
- **Business Entity Management:** Affiliated group structure needed for Joyce/Finnigan election
- **Geographic Data:** State/municipality boundaries, nexus thresholds, tax rates
- **Industry Classification:** NAICS codes determine service sourcing method defaults

---

## Out of Scope

- **Foreign income apportionment:** International operations with FDII/GILTI (defer to federal tax professional)
- **Water's edge election:** Including/excluding foreign subsidiaries in combined reporting (complex, rare)
- **Transfer pricing:** Intercompany transaction pricing for arm's length standard (requires specialized software)
- **Combined reporting:** Consolidating multiple legal entities into single apportioned return (covered in Spec 11: Consolidated Returns)
- **Apportionment for specific industries:** Airlines, trucking, railroads, telecommunications have specialized formulas (handle as custom rules)

---

## Edge Cases

1. **Negative property/payroll factors:** Business has losses in some states, negative payroll (refunds). System treats negative values as zero for factor calculation (cannot have negative percentage).

2. **Zero-factor scenario:** Business has property/payroll in Ohio but zero sales. System cannot calculate sales factor (0/0 undefined). Fallback: Use two-factor formula (property + payroll only).

3. **Sales factor > 100%:** Data entry error or unusual transaction (refund exceeded sales). System flags as validation error, blocks filing.

4. **Nexus changes mid-year:** Business opens office in new state on July 1. System prorates factors by days: Jan-Jun use old nexus, Jul-Dec use new nexus.

5. **Throwback to multiple states:** Sale originates from multi-state operations (manufactured in OH, warehoused in PA, shipped from PA to CA customer). System throws back to state with greatest property/payroll presence.

6. **Service revenue to individual consumers (B2C):** Market-based sourcing requires customer location, but individual consumer location may be unknown. System prompts for customer state or falls back to cost-of-performance.

7. **Sales to federal government:** Federal government not a "state" for throwback purposes. System assigns to state where federal agency is located (e.g., Pentagon = VA, IRS Cincinnati = OH).

8. **Apportionment percentages sum to ≠ 100%:** Due to throwback rules, sum of all state apportionment percentages may be 110% (some income taxed by multiple states). System allows ±20% variance, flags if outside range.
