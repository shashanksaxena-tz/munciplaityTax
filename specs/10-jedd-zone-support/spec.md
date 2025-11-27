# JEDD Zone Support

**Feature Name:** Joint Economic Development District (JEDD) Tax Allocation  
**Priority:** MEDIUM  
**Status:** Specification  
**Created:** 2025-11-27

---

## Overview

Implement Joint Economic Development District (JEDD) tax allocation system for businesses operating in special economic zones shared by multiple municipalities. JEDD agreements allow municipalities to collaborate on economic development by sharing tax revenue from designated zones according to contractual percentages. This feature handles JEDD zone identification, multi-jurisdiction allocation, employer withholding for JEDD employees, and revenue distribution reporting.

**Current State:** No JEDD support (0% complete). System treats each municipality independently, cannot handle split tax liability across multiple municipalities within same zone.

**Target Users:** Businesses with locations in JEDD zones, payroll managers withholding JEDD taxes, municipality finance officers receiving JEDD revenue, JEDD administrators managing agreements.

---

## User Scenarios & Testing

### US-1: Identify Business Location in JEDD Zone (P1 - Critical)

**User Story:**  
As a business registering my office address, I want the system to automatically detect if my location is within a JEDD zone and inform me which municipalities share the zone, so that I understand I'll be filing with multiple municipalities.

**Business Context:**  
JEDD zones are special geographic areas (often industrial parks, commercial corridors) where 2-3 municipalities have agreed to share tax revenue. Business located at "123 Innovation Drive" might be in "Dublin-Columbus JEDD" where Dublin gets 60%, Columbus gets 40% of tax collected.

**Independent Test:**  
- Business address: 123 Innovation Drive, Dublin, OH 43017
- System checks address against JEDD database
- Finds: "Dublin-Columbus JEDD Zone 1"
- Participating municipalities: Dublin (60%), Columbus (40%)
- System displays: "Your business is located in Dublin-Columbus JEDD. Tax will be allocated: Dublin 60%, Columbus 40%."

**Acceptance Criteria:**
- GIVEN business enters address
- WHEN address is in JEDD zone
- THEN system MUST detect JEDD zone using geocoding or address match
- AND system MUST display JEDD information:
  - JEDD zone name
  - Participating municipalities
  - Revenue allocation percentages
  - JEDD agreement effective date
- AND system MUST provide link to JEDD agreement (PDF)
- AND system MUST save JEDD status to business profile

---

### US-2: Allocate Business Income Tax Across JEDD Municipalities (P1 - Critical)

**User Story:**  
As a business in Dublin-Columbus JEDD with $100K net profit, I want the system to automatically split my tax liability 60% Dublin ($1,500) and 40% Columbus ($1,000), generate separate returns for each municipality, and handle payment allocation, so that I comply with JEDD tax obligations.

**Business Context:**  
JEDD businesses file single "master return" but tax is allocated per agreement. If Dublin rate is 2.5% and Columbus rate is 2.0%, calculation:
- Dublin: $100K × 60% × 2.5% = $1,500
- Columbus: $100K × 40% × 2.0% = $800
- Total JEDD tax: $2,300

**Independent Test:**  
- Business net profit: $100K
- JEDD zone: Dublin (60%, 2.5% rate), Columbus (40%, 2.0% rate)
- Dublin tax: $100K × 60% = $60K taxable × 2.5% = $1,500
- Columbus tax: $100K × 40% = $40K taxable × 2.0% = $800
- Total: $2,300
- System generates 2 returns: Dublin Form 27-JEDD, Columbus Form 27-JEDD

**Acceptance Criteria:**
- GIVEN business in JEDD zone with income
- WHEN calculating tax liability
- THEN system MUST allocate income by JEDD percentages:
  - Municipality A taxable income = Total income × A's percentage
  - Municipality B taxable income = Total income × B's percentage
- AND system MUST apply each municipality's tax rate independently
- AND system MUST calculate tax for each municipality
- AND system MUST generate separate returns for each municipality (Form 27-JEDD)
- AND system MUST display allocation breakdown:
  ```
  Total Net Profit: $100,000
  
  Dublin (60%):
    Allocated Income: $60,000
    Tax Rate: 2.5%
    Tax Due: $1,500
  
  Columbus (40%):
    Allocated Income: $40,000
    Tax Rate: 2.0%
    Tax Due: $800
  
  Total JEDD Tax: $2,300
  ```

---

### US-3: Withhold JEDD Tax from Employee Wages (P1 - Critical)

**User Story:**  
As a payroll manager, I want to withhold JEDD tax from employees working at our JEDD location using the correct blended rate (60% × 2.5% + 40% × 2.0% = 2.3%), and report withholding to both municipalities on quarterly W-1 forms, so that employees' municipal tax obligations are correctly withheld.

**Business Context:**  
Employees working in JEDD zone have tax withheld at blended rate. If employee works multiple locations (some JEDD, some non-JEDD), must prorate. Example: Employee earns $50K annually, works 100% at JEDD location → Withhold at 2.3% blended rate = $1,150/year.

**Independent Test:**  
- Employee: John Doe, $50K annual salary
- Works 100% at Dublin-Columbus JEDD location
- Blended JEDD rate: (60% × 2.5%) + (40% × 2.0%) = 1.5% + 0.8% = 2.3%
- Annual withholding: $50K × 2.3% = $1,150
- Quarterly withholding: $1,150 / 4 = $287.50
- Q1 W-1 reports: Dublin $172.50 (60%), Columbus $115 (40%)

**Acceptance Criteria:**
- GIVEN employee works at JEDD location
- WHEN calculating withholding
- THEN system MUST calculate blended JEDD rate:
  - Blended rate = (Muni A % × A rate) + (Muni B % × B rate) + ...
- AND system MUST withhold from wages at blended rate
- AND system MUST allocate withheld tax by JEDD percentages for reporting:
  - Muni A withholding = Total withholding × A percentage
  - Muni B withholding = Total withholding × B percentage
- AND system MUST generate separate W-1 reports for each JEDD municipality
- AND system MUST track year-to-date withholding by municipality
- AND system MUST generate W-2 showing JEDD withholding (Box 18-20)

---

### US-4: Handle Multi-Location Business (JEDD + Non-JEDD) (P2 - High Value)

**User Story:**  
As a business with 2 locations (HQ in Dublin JEDD, Branch in Cleveland non-JEDD), I want the system to allocate income and withholding based on where revenue is earned and where employees work, so that I correctly file with all municipalities (Dublin 60%, Columbus 40% for JEDD, Cleveland 100% for branch).

**Business Context:**  
Multi-location businesses must apportion income. Simple approach: Apportion by payroll (where employees work). If 70% of payroll at JEDD location, 30% at Cleveland → 70% of income allocated to JEDD (split 60/40), 30% to Cleveland.

**Independent Test:**  
- Total net profit: $200K
- Payroll apportionment: JEDD location 70% ($140K income), Cleveland 30% ($60K income)
- JEDD allocation:
  - Dublin (60% of $140K = $84K): $84K × 2.5% = $2,100
  - Columbus (40% of $140K = $56K): $56K × 2.0% = $1,120
- Cleveland: $60K × 2.5% = $1,500
- Total tax: $4,720

**Acceptance Criteria:**
- GIVEN business with multiple locations (some JEDD, some non-JEDD)
- WHEN calculating tax liability
- THEN system MUST apportion income to locations:
  - Method 1: Payroll percentage (where employees work)
  - Method 2: Revenue percentage (where sales are made)
  - Method 3: Property percentage (where assets are located)
  - Default: Payroll percentage
- AND system MUST further allocate JEDD-location income by JEDD percentages
- AND system MUST apply each municipality's tax rate
- AND system MUST generate returns for all municipalities (JEDD and non-JEDD)
- AND system MUST provide allocation summary showing income by location and municipality

---

### US-5: JEDD Revenue Distribution Report for Municipalities (P3 - Future)

**User Story:**  
As a municipality finance officer, I want to view a JEDD revenue distribution report showing all businesses in our JEDD zones, taxes collected, our municipality's share, and payment status, so that I can reconcile JEDD revenue and coordinate with partner municipalities.

**Business Context:**  
Municipalities need reports to track JEDD revenue. Report shows: Business name, JEDD zone, Total tax collected, Our share ($), Partner municipality share ($), Payment status. Used for financial reconciliation and revenue forecasting.

**Independent Test:**  
- Dublin finance officer views JEDD report for Q1 2024
- Shows 50 businesses in Dublin-Columbus JEDD
- Total JEDD tax collected: $500K
- Dublin's share (60%): $300K
- Columbus's share (40%): $200K
- Payment status: $450K received, $50K outstanding

**Acceptance Criteria:**
- GIVEN municipality with JEDD agreements
- WHEN finance officer generates JEDD report
- THEN system MUST display:
  - List of all JEDD zones involving municipality
  - Businesses in each zone
  - Tax collected from each business
  - Municipality's allocated share
  - Partner municipalities' shares
  - Payment status (paid, outstanding, overdue)
  - Summary totals by zone
- AND system MUST support filtering by: Zone, Quarter, Payment status
- AND system MUST export report as PDF/Excel for reconciliation
- AND system MUST highlight discrepancies (e.g., business paid wrong municipality)

---

## Functional Requirements

### JEDD Zone Management

**FR-001:** System MUST maintain database of JEDD zones with attributes:
- Zone ID, Zone name
- Participating municipalities (2-3 typically)
- Revenue allocation percentages (must sum to 100%)
- Geographic boundaries (polygon coordinates or address ranges)
- Effective date, Expiration date (if applicable)
- JEDD agreement document (PDF)

**FR-002:** System MUST support geocoding to determine if business address falls within JEDD zone boundaries

**FR-003:** System MUST allow manual JEDD zone assignment if geocoding fails or address ambiguous

**FR-004:** System MUST validate JEDD configuration:
- Percentages sum to 100%
- All participating municipalities have active tax rates
- No overlapping JEDD zones at same address

**FR-005:** System MUST display JEDD zone information on business profile:
- Zone name, Participating municipalities, Allocation percentages, Effective date

### Business Income Allocation

**FR-006:** System MUST allocate business net profit to JEDD municipalities by configured percentages

**FR-007:** System MUST apply each municipality's tax rate independently to allocated income

**FR-008:** System MUST calculate total JEDD tax: Sum of all municipality taxes

**FR-009:** System MUST generate separate tax return for each JEDD municipality (Form 27-JEDD)

**FR-010:** System MUST display allocation breakdown showing:
- Total net profit
- Allocated income per municipality (amount and percentage)
- Tax rate per municipality
- Tax due per municipality
- Total JEDD tax

**FR-011:** System MUST support multi-location businesses:
- Apportion income to locations by payroll, revenue, or property
- Apply JEDD allocation to JEDD-location income only
- Generate separate returns for all municipalities (JEDD and non-JEDD)

**FR-012:** System MUST handle JEDD zone changes mid-year:
- Pro-rate income by days in each zone
- Example: Business in Dublin-Columbus JEDD Jan-Jun, moves to Cleveland Jul-Dec
  - 50% of income allocated to JEDD (Dublin/Columbus), 50% to Cleveland

### Withholding for JEDD Employees

**FR-013:** System MUST calculate blended JEDD withholding rate:
- Blended rate = Σ (Municipality % × Municipality rate)
- Example: (60% × 2.5%) + (40% × 2.0%) = 2.3%

**FR-014:** System MUST withhold from employee wages at blended JEDD rate

**FR-015:** System MUST allocate withheld tax to JEDD municipalities by configured percentages for W-1 reporting

**FR-016:** System MUST generate separate W-1 withholding reports for each JEDD municipality showing:
- Employees working in JEDD zone
- Wages subject to JEDD tax
- Withholding allocated to this municipality (blended rate × allocation %)

**FR-017:** System MUST track year-to-date withholding by municipality for each employee

**FR-018:** System MUST generate W-2 for JEDD employees with:
- Box 18: Total JEDD wages
- Box 19: Total JEDD withholding (blended rate)
- Box 20: JEDD zone name or "Multiple municipalities"

**FR-019:** System MUST handle employees working multiple locations:
- Calculate days/hours at each location
- Apply JEDD withholding only to JEDD-location wages
- Apply standard withholding to non-JEDD-location wages

### Payment Processing & Distribution

**FR-020:** System MUST accept single payment for total JEDD tax

**FR-021:** System MUST distribute payment to municipality accounts by JEDD percentages:
- Municipality A receives: Total payment × A percentage
- Municipality B receives: Total payment × B percentage

**FR-022:** System MUST track payment status separately for each municipality:
- If total JEDD tax $2,300 paid, mark Dublin ($1,500) PAID and Columbus ($800) PAID
- If partial payment $1,500, allocate: Dublin $900, Columbus $600, both marked PARTIAL

**FR-023:** System MUST generate payment vouchers for each JEDD municipality with allocated amount

**FR-024:** System MUST support electronic payment with automatic distribution to municipality accounts

### JEDD Forms & Reporting

**FR-025:** System MUST generate Form 27-JEDD for each participating municipality with:
- Business identification
- JEDD zone name
- Total business income
- Allocated income to this municipality (percentage and amount)
- Tax rate
- Tax due to this municipality
- Withholding credits allocated to this municipality
- Net tax due

**FR-026:** System MUST generate JEDD allocation summary (master document) showing:
- Total income
- Allocation to all municipalities
- Tax calculations for all municipalities
- Total JEDD tax
- Payment distribution

**FR-027:** System MUST generate W-1 withholding reports for each JEDD municipality with allocated withholding amounts

**FR-028:** System MUST generate annual reconciliation report for JEDD businesses:
- Total income, Total tax by municipality
- Total withholding by municipality
- Payments made
- Balance due/refund by municipality

### Municipality Revenue Reporting

**FR-029:** System MUST generate JEDD revenue report for municipality finance officers with:
- All businesses in municipality's JEDD zones
- Tax collected from each business
- Municipality's allocated share
- Partner municipalities' shares
- Payment status
- Summary totals

**FR-030:** System MUST support filtering by: JEDD zone, Quarter, Tax year, Payment status

**FR-031:** System MUST export JEDD report as PDF and Excel

**FR-032:** System MUST highlight discrepancies:
- Business paid wrong municipality
- Payment allocation doesn't match JEDD percentages
- Withholding doesn't reconcile to W-2 totals

### Validation & Audit

**FR-033:** System MUST validate JEDD allocations:
- Percentages sum to 100%
- Allocated income sums to total income
- Municipality taxes sum to total JEDD tax
- Payment distribution matches JEDD percentages

**FR-034:** System MUST create audit trail for JEDD transactions:
- Income allocation calculations
- Tax calculations by municipality
- Payment distribution
- Withholding allocations

**FR-035:** System MUST flag potential errors:
- JEDD business filing with single municipality only (should file with all)
- Payment to one municipality when JEDD zone involves multiple
- Withholding rate doesn't match blended JEDD rate

---

## Key Entities

### JEDDZone

**Attributes:**
- `zoneId` (UUID)
- `zoneName` (string): "Dublin-Columbus JEDD Zone 1"
- `participatingMunicipalities` (array): List of MunicipalityAllocation objects
- `geographicBoundaries` (GeoJSON): Polygon coordinates
- `addressRanges` (array): Street address ranges as alternative to coordinates
- `effectiveDate` (date): When JEDD agreement became active
- `expirationDate` (date): When agreement expires (null if indefinite)
- `agreementDocumentPath` (string): PDF of JEDD agreement
- `createdDate` (timestamp)

### MunicipalityAllocation

**Attributes:**
- `municipalityCode` (string): "DUBLIN", "COLUMBUS"
- `municipalityName` (string)
- `allocationPercentage` (decimal): 60.0, 40.0 (must sum to 100)
- `taxRate` (decimal): 2.5%, 2.0%

### JEDDBusinessProfile

**Attributes:**
- `businessId` (UUID): Foreign key to Business
- `jeddZoneId` (UUID): Foreign key to JEDDZone
- `inJEDD` (boolean): Whether business is in JEDD zone
- `jeddStartDate` (date): When business entered JEDD zone
- `jeddEndDate` (date): If business left JEDD zone
- `multiLocation` (boolean): Whether business has locations in/out of JEDD
- `allocationMethod` (enum): PAYROLL | REVENUE | PROPERTY (for multi-location)

### JEDDTaxAllocation

**Attributes:**
- `allocationId` (UUID)
- `returnId` (UUID): Foreign key to TaxReturn
- `jeddZoneId` (UUID)
- `totalIncome` (decimal): Total business net profit
- `allocations` (array): List of MunicipalityTaxAllocation objects
- `totalJEDDTax` (decimal): Sum of all municipality taxes

### MunicipalityTaxAllocation

**Attributes:**
- `municipalityCode` (string)
- `allocationPercentage` (decimal): 60%, 40%
- `allocatedIncome` (decimal): Total income × percentage
- `taxRate` (decimal): Municipality tax rate
- `taxDue` (decimal): Allocated income × rate
- `withholdingCredit` (decimal): Allocated withholding
- `netTaxDue` (decimal): Tax due - withholding

### JEDDWithholding

**Attributes:**
- `withholdingId` (UUID)
- `employeeId` (UUID)
- `jeddZoneId` (UUID)
- `payPeriod` (string): "2024-Q1"
- `wagesEarned` (decimal): Wages earned at JEDD location
- `blendedRate` (decimal): Calculated blended JEDD rate (e.g., 2.3%)
- `totalWithholding` (decimal): Wages × blended rate
- `allocations` (array): Withholding distributed to municipalities
- `w2Box18` (decimal): Total JEDD wages for W-2
- `w2Box19` (decimal): Total JEDD withholding for W-2

### JEDDRevenueReport

**Attributes:**
- `reportId` (UUID)
- `municipalityCode` (string): Municipality viewing report
- `reportPeriod` (string): "2024-Q1"
- `jeddZones` (array): Zones involving this municipality
- `businesses` (array): List of businesses in JEDD zones
- `totalTaxCollected` (decimal): All JEDD tax from businesses
- `municipalityShare` (decimal): This municipality's allocated amount
- `partnerShares` (JSON): Other municipalities' shares
- `paymentStatus` (JSON): Paid, outstanding, overdue by business
- `generatedDate` (timestamp)

---

## Success Criteria

- **JEDD Coverage:** 100% of JEDD-zone businesses correctly identified and allocated (vs current 0% - no JEDD support)
- **Allocation Accuracy:** Zero manual calculation required for JEDD splits (automatic allocation)
- **Withholding Compliance:** Blended JEDD rates calculated correctly 100% of time (vs manual rate calculation errors)
- **Revenue Distribution:** Municipality revenue reports reconcile to 100% accuracy (no discrepancies)
- **Multi-Jurisdiction Filing:** JEDD businesses file with all participating municipalities (zero missed filings)

---

## Assumptions

- Most JEDD zones have 2 municipalities (some have 3, rare to have 4+)
- JEDD allocation percentages remain constant during tax year (no mid-year changes)
- All JEDD municipalities adopt same tax year and filing deadlines
- Payment distribution happens automatically based on JEDD percentages
- JEDD zones are static geographic areas (boundaries don't change frequently)

---

## Dependencies

- **Rule Engine (Spec 4):** JEDD zone configurations, allocation percentages, tax rates stored as rules
- **Schedule Y Sourcing (Spec 5):** Multi-location apportionment logic used for JEDD + non-JEDD locations
- **Withholding Reconciliation (Spec 1):** W-1 forms generated separately for each JEDD municipality
- **Business Form Library (Spec 8):** Form 27-JEDD generation for each municipality
- **Double-Entry Ledger (Spec 12):** Payment distribution to multiple municipality accounts

---

## Out of Scope

- **JEDD vs TIF zones:** Tax Increment Financing (different allocation mechanism)
- **Enterprise zones:** Special economic development zones (different tax incentives)
- **Inter-state JEDD:** Cross-state JEDD agreements (e.g., Ohio-Kentucky) - focus on intra-state only
- **JEDD zone creation:** Administrative process to establish new JEDD (handled by municipalities)

---

## Edge Cases

1. **Business on JEDD boundary:** Address is on border of JEDD zone. Geocoding is ambiguous. System prompts user: "Your address may be in JEDD zone. Confirm location" with map view.

2. **JEDD zone expires mid-year:** JEDD agreement expires July 1. System pro-rates: Jan-Jun use JEDD allocation, Jul-Dec use standard single-municipality filing.

3. **Municipality changes tax rate mid-year in JEDD:** Dublin increases rate from 2.5% to 2.75% on July 1. System recalculates blended rate for H2: (60% × 2.75%) + (40% × 2.0%) = 2.45% vs H1: 2.3%.

4. **Employee works multiple JEDD zones:** Employee splits time between two JEDD zones (different allocations). System calculates weighted blended rate based on hours at each location.

5. **Partial payment allocated incorrectly:** Business pays $1,500 to Dublin directly (should pay $2,300 total with $1,500 Dublin, $800 Columbus). System creates imbalance: Dublin overpaid, Columbus unpaid. Flags for correction.

6. **JEDD business files with one municipality only:** Business in Dublin-Columbus JEDD files return only with Dublin. System detects missing Columbus return, displays error: "You are in JEDD zone. You must also file with Columbus."

7. **Municipality leaves JEDD mid-year:** Rare, but if JEDD dissolves or municipality withdraws, system must handle transition. Lock historical allocations, apply new rules prospectively.

8. **JEDD with 3+ municipalities:** Example: Dublin (50%), Columbus (30%), Westerville (20%). Same logic applies but more complex display. System handles N municipalities programmatically, no hard limit.
