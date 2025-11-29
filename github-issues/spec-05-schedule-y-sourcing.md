# Spec 5: Multi-State Income Sourcing & Apportionment (Schedule Y)

**Priority:** HIGH  
**Feature Branch:** `5-schedule-y-sourcing`  
**Spec Document:** `specs/5-schedule-y-sourcing/spec.md`

## Overview

Implement comprehensive multi-state income sourcing rules for businesses operating in multiple jurisdictions, including Joyce vs Finnigan election for sales factor sourcing, throwback/throwout rules, market-based sourcing methodologies, and service revenue attribution.

## Implementation Status

**Current:** 0% - System only supports single-jurisdiction reporting  
**Required:** Full multi-state apportionment calculation with Schedule Y

## Core Requirements (FR-001 to FR-050)

### Apportionment Formula Configuration (FR-001 to FR-005)
- [ ] Support multiple formulas: 3-factor, 4-factor double-weighted sales, single-sales-factor
- [ ] Retrieve formula from rule engine by tax year, entity type, industry code (NAICS)
- [ ] Calculate each factor as percentage: (Ohio amount) / (Everywhere amount) × 100%
- [ ] Apply factor weightings per formula rules
- [ ] Calculate final apportionment: (Sum of weighted factors) / (Sum of weights)

### Sales Factor Sourcing Elections (FR-006 to FR-010)
- [ ] Support Joyce vs Finnigan election
  - **Finnigan (default):** Include all group sales in denominator regardless of nexus
  - **Joyce (minority):** Include only sales of entities with Ohio nexus
- [ ] Display election choice on Schedule Y with explanations
- [ ] Save sourcing method election to business profile
- [ ] Apply elected method consistently across tax years
- [ ] Validate election against municipality rules

### Throwback/Throwout Rules (FR-011 to FR-016)
- [ ] Determine if throwback rule applies per sale:
  - Sale shipped from Ohio to another state
  - Business lacks nexus in destination state
  - Destination state would not tax the sale
- [ ] Apply throwback: add sale to Ohio sales factor numerator
- [ ] Support throwout rule alternative: exclude from both numerator and denominator
- [ ] Retrieve throwback/throwout election from rule engine
- [ ] Display throwback adjustments on Schedule Y
- [ ] Track nexus by state/municipality for throwback determination

### Market-Based Sourcing for Services (FR-017 to FR-022)
- [ ] Determine sourcing method: Market-based (modern) vs Cost-of-performance (historical)
- [ ] Default to market-based for professional services, IT, management, financial services
- [ ] Prompt for customer location when using market-based sourcing
- [ ] Support cascading sourcing rules:
  1. Market-based (customer location)
  2. If unknown: Cost-of-performance (employee location)
  3. If both unknown: Pro-rata by overall apportionment
- [ ] Source 100% of service revenue to single state (market-based)
- [ ] Prorate by employee location (cost-of-performance) - by payroll, days, or hours

### Property Factor Calculation (FR-027 to FR-031)
- [ ] Calculate: (Ohio property value) / (Total property everywhere)
- [ ] Use average property values: (Beginning + Ending) / 2
- [ ] Include real property (land, buildings) and tangible personal property
- [ ] Exclude intangible property (patents, trademarks, goodwill)
- [ ] Handle rented property: Annual rent × 8 capitalization rate

### Payroll Factor Calculation (FR-032 to FR-036)
- [ ] Calculate: (Ohio payroll) / (Total payroll everywhere)
- [ ] Include W-2 wages, contractor payments (1099-NEC), officer compensation
- [ ] Assign payroll to state by employee's primary work location
- [ ] Handle remote employees: assign to state where services performed
- [ ] Exclude payroll of employees in states where business lacks nexus (Joyce only)

### Sales Factor Calculation (FR-037 to FR-042)
- [ ] Calculate: (Ohio sales) / (Total sales everywhere)
- [ ] Source tangible goods to destination state (shipped/delivered)
- [ ] Source services per market-based or cost-of-performance
- [ ] Source rental income to state where property located
- [ ] Source interest income to state where borrower located
- [ ] Source royalty income to state where IP used

### Display & Reporting (FR-043 to FR-047)
- [ ] Display Schedule Y with sections: Property Factor, Payroll Factor, Sales Factor
- [ ] Show sourcing method elections prominently
- [ ] Display calculation breakdown with line-by-line detail
- [ ] Generate PDF Form 27-Y (Schedule Y)
- [ ] Support multi-year comparison: current, prior, 3-year average

### Validation & Audit Support (FR-048 to FR-050)
- [ ] Validate each factor percentage 0-100%
- [ ] Validate final apportionment percentage 0-100%
- [ ] Validate sum of all state apportionments ≈ 100% (±5% variance)
- [ ] Flag inconsistencies (0% property but business reports Ohio office)
- [ ] Create audit trail for apportionment calculations

## User Stories (5 Priority P1-P3)

1. **US-1 (P1):** Multi-State Business Elects Finnigan Method
2. **US-2 (P1):** Apply Throwback Rule for Destination State Without Nexus
3. **US-3 (P1):** Market-Based Sourcing for Service Revenue
4. **US-4 (P2):** Display Apportionment Factor Calculation with Breakdown
5. **US-5 (P3):** Handle Single-Sales-Factor Election

## Key Entities

### ScheduleY (Apportionment Schedule)
- scheduleYId, returnId, taxYear
- apportionmentFormula, formulaWeights
- propertyFactorPercentage, payrollFactorPercentage, salesFactorPercentage
- finalApportionmentPercentage
- sourcingMethodElection (FINNIGAN/JOYCE)
- throwbackElection, serviceSourcingMethod

### PropertyFactor, PayrollFactor, SalesFactor
- Detailed breakdowns with Ohio amounts, total amounts, percentages
- Specific fields for each factor type

### SaleTransaction
- Individual sale transaction details for detailed sourcing
- transactionType, originState, destinationState
- sourcingMethod, hasDestinationNexus, allocatedState

### NexusTracking
- Track nexus status in each state/municipality
- nexusReasons: PHYSICAL_PRESENCE, EMPLOYEE_PRESENCE, ECONOMIC_NEXUS
- Sales, property, payroll, employee counts by state

## Success Criteria

- 90%+ of multi-state filers correctly calculate apportionment using system
- System correctly identifies 100% of sales subject to throwback
- Service revenue sourced using market-based method by default
- Apportionment calculations pass audit with zero adjustments
- Multi-state filers complete Schedule Y in 20 minutes (vs 2-3 hours manual)
- CPAs rate Schedule Y feature 8+/10

## Edge Cases Documented

- Negative property/payroll factors
- Zero-factor scenario (no sales but has property/payroll)
- Sales factor >100% (data entry error)
- Nexus changes mid-year
- Throwback to multiple states
- Service revenue to B2C (customer location unknown)
- Sales to federal government
- Apportionment percentages sum ≠ 100%

## Dependencies

- Rule Engine (Spec 4) - Apportionment formulas, throwback elections, sourcing methods
- Withholding Reconciliation (Spec 1) - Payroll data feeds payroll factor
- Enhanced Discrepancy Detection (Spec 3) - Validate factors against W-2, property tax returns
- Business Entity Management - Affiliated group structure for Joyce/Finnigan
- Geographic Data - State/municipality boundaries, nexus thresholds, tax rates
- Industry Classification - NAICS codes for service sourcing defaults

## Technical Notes

- 50 Functional Requirements (FR-001 to FR-050)
- Complex calculation logic with multiple sourcing methods
- Requires GIS or address matching for nexus determination
- Integration with W-1 filings for payroll data

## Related Specs

- Feeds into: Spec 6 (NOL - state NOL is federal NOL × apportionment %)
- Integrates with: Spec 10 (JEDD zones - multi-jurisdiction allocation)
- Uses: Spec 4 (Rule Engine for formula configuration)
