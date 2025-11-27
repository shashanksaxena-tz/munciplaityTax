# NOL Carryforward Tracker

**Feature Name:** Net Operating Loss (NOL) Carryforward & Carryback System  
**Priority:** HIGH  
**Status:** Specification  
**Created:** 2025-11-27

---

## Overview

Implement comprehensive Net Operating Loss (NOL) tracking system that manages multi-year loss carryforwards, 20-year expiration schedules, CARES Act carryback provisions, and 80% taxable income limitation. This feature is essential for businesses that experience losses in some years and need to offset profits in future years, as well as for auditors who must verify NOL utilization across multiple tax years.

**Current State:** System has basic NOL calculation but no multi-year tracking, expiration management, or carryback support. NOLs are entered manually each year with no validation against prior year returns.

**Target Users:** Business filers with operating losses, CPAs managing NOL schedules, tax managers planning NOL utilization strategies, auditors verifying multi-year NOL compliance.

---

## User Scenarios & Testing

### US-1: Track NOL Carryforward Across Multiple Years (P1 - Critical)

**User Story:**  
As a business that had losses in 2020-2022 ($500K total NOL), I want the system to automatically track my NOL balance across years, deduct amounts used in profitable years, and show me remaining NOL available for future years, so that I don't lose valuable tax benefits due to poor recordkeeping.

**Business Context:**  
NOLs can be carried forward for 20 years (pre-TCJA) or indefinitely (post-TCJA for federal, varies by state). Each year, business uses NOL to offset taxable income, reducing the NOL balance. Without tracking, businesses lose track of remaining NOL and either over-utilize (audit risk) or under-utilize (pay unnecessary tax).

**Independent Test:**  
- 2020: Net loss = $200K → NOL created = $200K
- 2021: Net loss = $150K → NOL created = $150K, Total NOL = $350K
- 2022: Net loss = $150K → NOL created = $150K, Total NOL = $500K
- 2023: Taxable income = $300K → NOL utilized = $240K (80% limit), Remaining NOL = $260K
- 2024: Taxable income = $100K → NOL utilized = $80K (80% limit), Remaining NOL = $180K
- 2025: Taxable income = $400K → NOL utilized = $180K (remaining balance), Taxable income after NOL = $220K

**Acceptance Criteria:**
- GIVEN a business with NOL carryforwards from prior years
- WHEN filing current year return
- THEN system MUST display NOL schedule showing:
  - Each prior year with NOL generated
  - Original NOL amount
  - Amounts used in subsequent years
  - Remaining balance available this year
  - Expiration date (if applicable)
- AND system MUST auto-populate "NOL deduction" field with available balance (up to 80% of taxable income)
- AND system MUST update NOL schedule after current year filing
- AND system MUST carry forward updated NOL balance to next year

---

### US-2: Apply 80% Taxable Income Limitation (Post-TCJA) (P1 - Critical)

**User Story:**  
As a CPA preparing 2023+ tax returns, I want the system to automatically limit NOL deduction to 80% of taxable income (per TCJA rules), so that my clients cannot use NOL to reduce taxable income to zero and instead must always pay tax on at least 20% of income.

**Business Context:**  
Tax Cuts and Jobs Act (TCJA) 2017 changed NOL rules for tax years 2018+:
- Pre-TCJA: NOL could reduce taxable income to $0 (100% offset)
- Post-TCJA: NOL limited to 80% of taxable income (must pay tax on 20% minimum)

This is permanent change for federal and adopted by many states/municipalities.

**Independent Test:**  
Business has $500K NOL carryforward and $300K taxable income in 2024:
- Pre-TCJA rules: NOL deduction = $300K (100% offset), Taxable income after NOL = $0, Remaining NOL = $200K
- Post-TCJA rules: NOL deduction = $240K (80% limit), Taxable income after NOL = $60K, Remaining NOL = $260K

**Acceptance Criteria:**
- GIVEN taxable income before NOL deduction
- WHEN calculating NOL deduction for post-2018 tax years
- THEN system MUST limit NOL deduction to 80% of taxable income
- AND system MUST display calculation: "Taxable income: $300K → Max NOL deduction (80%): $240K → Taxable income after NOL: $60K"
- AND system MUST flag if user manually overrides to >80% (validation error)
- AND system MUST allow 100% offset for pre-2018 tax years (grandfathered NOLs)

---

### US-3: CARES Act NOL Carryback (2018-2020 Losses) (P2 - High Value)

**User Story:**  
As a business that had losses in 2018-2020 due to COVID-19, I want to carry back my NOL to prior 5 years (when I had profits and paid tax), receive refunds of prior year taxes, and use carryback before carryforward, so that I can get immediate cash refund rather than waiting years to use NOL.

**Business Context:**  
CARES Act 2020 temporarily restored NOL carryback for 2018-2020 losses:
- Can carry back 5 years (vs 0 years under TCJA)
- Carryback to pre-TCJA years: No 80% limitation (can offset 100% of income)
- Generates immediate tax refund for prior year taxes paid
- Must use carryback before carryforward (ordering rule)

This is one-time provision for COVID relief, but some states have permanent carryback rules.

**Independent Test:**  
- 2016: Taxable income = $400K, Tax paid = $10K (2.5% rate)
- 2017: Taxable income = $300K, Tax paid = $7.5K
- 2018: Net loss = $500K → Carry back to 2016 and 2017
  - Offset 2017 income: $300K → Refund = $7.5K, Remaining NOL = $200K
  - Offset 2016 income: $200K → Refund = $5K (2.5% × $200K), Remaining NOL = $0
  - Total refund: $12.5K

**Acceptance Criteria:**
- GIVEN an NOL generated in 2018, 2019, or 2020
- WHEN user elects carryback (optional election)
- THEN system MUST display prior 5 years with taxable income available for carryback
- AND system MUST calculate carryback starting with oldest year first (5 years back, then 4 years back, etc.)
- AND system MUST calculate refund amount: (NOL used) × (prior year tax rate)
- AND system MUST generate Form 27-NOL-CB (Carryback Application) with refund calculation
- AND system MUST update NOL schedule showing carryback amounts and remaining balance for carryforward

---

### US-4: NOL Expiration Tracking with Alerts (P2 - High Value)

**User Story:**  
As a tax manager, I want to be alerted when NOLs are approaching expiration (e.g., 18+ years old under 20-year rule), so that I can strategically use older NOLs before they expire and avoid losing valuable tax benefits.

**Business Context:**  
Pre-TCJA NOLs (losses before 2018) have 20-year carryforward limit. After 20 years, NOL expires unused and is permanently lost. Post-TCJA NOLs (2018+) carry forward indefinitely at federal level, but some states still have expiration.

Example: 2005 NOL must be used by 2025 or it expires. If business has multiple NOL vintages, must use oldest first (FIFO - First In First Out).

**Independent Test:**  
- 2005 NOL: $100K remaining, expires 12/31/2025 (18 months away)
- 2019 NOL: $300K remaining, no expiration (indefinite)
- 2024 taxable income: $150K → Max NOL deduction = $120K (80% limit)
- System should use 2005 NOL first: $100K (oldest), then 2019 NOL: $20K

**Acceptance Criteria:**
- GIVEN NOLs with expiration dates
- WHEN viewing NOL schedule
- THEN system MUST display expiration date for each NOL vintage
- AND system MUST highlight NOLs expiring within 2 years with warning: "⚠️ Expires 12/31/2025 - Use soon or lose!"
- AND system MUST auto-apply FIFO ordering: Use oldest NOLs first
- AND system MUST allow manual override of NOL utilization order if user has tax planning reason
- AND system MUST generate expiration report: "NOLs Expiring in Next 3 Years" with amounts and years

---

### US-5: NOL by Entity Type & Apportionment (P2 - High Value)

**User Story:**  
As a multi-state business, I want my Ohio NOL to be calculated after apportionment (Ohio share of total loss), and tracked separately from federal NOL and other states' NOLs, so that I correctly apply state-specific NOL rules and don't confuse federal vs state NOL balances.

**Business Context:**  
NOLs are entity-level and jurisdiction-level:
- Federal NOL: Total business loss (pre-apportionment)
- State NOL: Apportioned loss (only Ohio's share)
- Example: Business has $1M federal loss, 30% Ohio apportionment → Ohio NOL = $300K

Additionally, S-Corps and Partnerships pass losses to owners (Schedule K-1), while C-Corps retain losses at entity level.

**Independent Test:**  
- 2023 C-Corp: Federal net loss = $1M, Ohio apportionment = 25% → Ohio NOL = $250K
- 2024: Federal income = $800K, Ohio apportionment = 30% → Ohio taxable income = $240K
- Ohio NOL deduction: Min($250K available, 80% × $240K = $192K) = $192K
- Remaining Ohio NOL: $250K - $192K = $58K

**Acceptance Criteria:**
- GIVEN a multi-state business with apportionment
- WHEN calculating NOL
- THEN system MUST calculate Ohio NOL = (Federal NOL) × (Ohio apportionment %)
- AND system MUST track Ohio NOL separately from federal NOL and other states
- AND system MUST display NOL schedule showing: Federal NOL, Ohio apportionment %, Ohio NOL
- AND for S-Corps/Partnerships: System MUST show NOL passed to owners on Schedule K-1 (not available at entity level)
- AND for C-Corps: System MUST retain NOL at entity level (not passed to shareholders)

---

### US-6: Amended Return NOL Recalculation (P3 - Future Enhancement)

**User Story:**  
As a business filing an amended return to correct 2022 income (reducing income by $100K), I want the system to recalculate my 2022 NOL, update my NOL carryforward schedule, and show me the impact on 2023-2024 returns that already used that NOL, so that I can decide whether to also amend future years.

**Business Context:**  
Amended returns can create new NOLs or increase existing NOLs. This has cascading effects on all subsequent years. IRS allows "quickie refunds" for NOL carryback amendments.

**Independent Test:**  
- 2022 original return: Taxable income = $50K, no NOL
- 2023: Taxable income = $200K, used $0 NOL
- 2024: File amended 2022 return: Reduce income by $100K → New 2022 NOL = $50K
- Impact: 2023 can now claim $40K NOL deduction (80% limit applies), reducing 2023 tax by $1K (2.5% rate)
- System should alert: "Amended 2022 return creates $50K NOL. You can amend 2023 to claim $40K NOL refund."

**Acceptance Criteria:**
- GIVEN an amended return that creates or increases NOL
- WHEN filing amended return
- THEN system MUST recalculate NOL for amended year
- AND system MUST update NOL carryforward schedule
- AND system MUST identify future years that can benefit from amended NOL: "2023 can use $40K additional NOL (estimated refund: $1K)"
- AND system MUST offer to prepare amended returns for subsequent years
- AND system MUST generate Form 27-NOL-AMD (NOL Amendment Schedule)

---

## Functional Requirements

### Multi-Year NOL Tracking

**FR-001:** System MUST create NOL record for each year with net operating loss:
- Tax year of loss origin
- Original NOL amount
- Entity type (C-Corp, S-Corp, Partnership)
- Jurisdiction (Federal, Ohio, specific municipality)

**FR-002:** System MUST track NOL usage across years:
- Year NOL was used
- Taxable income in that year
- Amount of NOL applied
- Remaining NOL balance after utilization

**FR-003:** System MUST calculate NOL balance available for current year:
- Sum of all prior year NOLs
- Minus all NOL utilization in intervening years
- Minus expired NOLs (if applicable)

**FR-004:** System MUST display NOL schedule on tax return with columns:
- Tax Year of Origin
- Original NOL Amount
- Previously Used
- Expired
- Available This Year
- Used This Year
- Remaining for Future

**FR-005:** System MUST automatically carry forward NOL balance to next tax year

**FR-006:** System MUST retrieve prior year NOL data from database (not require manual re-entry)

### 80% Taxable Income Limitation

**FR-007:** System MUST determine applicable NOL limitation rule based on tax year and jurisdiction:
- Post-2017 (TCJA): 80% limitation
- Pre-2018: 100% offset allowed
- State-specific: Check rule configuration

**FR-008:** System MUST calculate maximum NOL deduction: Min(Available NOL balance, 80% × Taxable income before NOL)

**FR-009:** System MUST apply NOL deduction to taxable income: (Taxable income before NOL) - (NOL deduction) = (Taxable income after NOL)

**FR-010:** System MUST validate NOL deduction does not exceed 80% limit (for post-2017 years)

**FR-011:** System MUST calculate remaining NOL after current year: (Available NOL) - (Used this year)

**FR-012:** System MUST display calculation breakdown:
```
Taxable income before NOL:        $300,000
Available NOL balance:            $500,000
Maximum NOL deduction (80%):      $240,000
NOL deduction applied:            $240,000
Taxable income after NOL:         $60,000
Remaining NOL for future years:   $260,000
```

### CARES Act Carryback

**FR-013:** System MUST support NOL carryback election for 2018, 2019, 2020 losses (CARES Act provision)

**FR-014:** System MUST allow user to elect carryback or waive carryback (optional election)

**FR-015:** System MUST retrieve prior 5 years of tax returns when carryback elected:
- Prior year taxable income
- Prior year tax rate
- Prior year tax paid

**FR-016:** System MUST calculate carryback amount using FIFO ordering (oldest year first):
- Start with 5th prior year
- Apply NOL to offset taxable income (100% offset allowed for pre-TCJA years)
- If NOL remains, apply to 4th prior year
- Continue until NOL exhausted or all 5 years used

**FR-017:** System MUST calculate refund amount for each carryback year: (NOL applied) × (Prior year tax rate)

**FR-018:** System MUST generate Form 27-NOL-CB (Carryback Application) with:
- NOL year and amount
- Carryback years and amounts applied to each
- Refund calculation for each year
- Total refund claimed

**FR-019:** System MUST update NOL schedule showing:
- Carryback amounts by year
- Refunds generated
- Remaining NOL for carryforward (if any)

**FR-020:** System MUST support state-specific carryback rules (some states allow 2-year carryback permanently)

### NOL Expiration Management

**FR-021:** System MUST assign expiration date to each NOL based on rules:
- Pre-2018 NOLs: 20-year carryforward (expires after 20 years)
- Post-2017 NOLs (federal): Indefinite carryforward (no expiration)
- State NOLs: Check state-specific expiration rules (10, 15, 20 years, or indefinite)

**FR-022:** System MUST apply FIFO ordering by default: Use oldest NOLs first (to avoid expiration)

**FR-023:** System MUST calculate expired NOLs:
- Check each NOL vintage against expiration date
- If current year > expiration year, mark NOL as expired
- Remove expired NOLs from available balance

**FR-024:** System MUST alert user of expiring NOLs:
- Flag NOLs expiring within 2 years: "⚠️ Expires 12/31/2025"
- Generate expiration report: "You have $100K NOL expiring in 2025. Consider strategies to use before expiration."

**FR-025:** System MUST allow manual NOL utilization ordering override:
- User can choose to use newer NOLs first if tax planning requires it
- System must log reason for override (e.g., "Using post-TCJA NOL first because pre-TCJA NOL can offset 100% of income in future high-income year")

**FR-026:** System MUST prevent use of expired NOLs (validation error if user tries to manually enter expired NOL)

### NOL by Entity Type

**FR-027:** System MUST track entity type for each NOL:
- C-Corporation: NOL retained at entity level
- S-Corporation: NOL passed to shareholders (pro-rata by ownership %)
- Partnership: NOL passed to partners (per allocation % in partnership agreement)
- Sole Proprietorship: NOL on individual return (Schedule C)

**FR-028:** For C-Corps: System MUST track NOL at entity level, not passed to shareholders

**FR-029:** For S-Corps: System MUST calculate each shareholder's share of NOL:
- Total S-Corp loss × Shareholder ownership %
- Display on shareholder's K-1 (Box 1 - Ordinary income/loss)
- Shareholder uses NOL on individual return (limited by basis and at-risk rules)

**FR-030:** For Partnerships: System MUST allocate NOL to partners per partnership agreement:
- May not be pro-rata (special allocations allowed)
- Display on partner's K-1 (Box 1 - Ordinary income/loss)

**FR-031:** System MUST validate S-Corp/Partnership NOL usage against shareholder/partner basis:
- NOL deduction limited to shareholder's basis in stock
- If basis insufficient, NOL suspended until basis increases

### Multi-State NOL Apportionment

**FR-032:** System MUST calculate state NOL separately from federal NOL:
- Ohio NOL = Federal NOL × Ohio apportionment %
- Track Ohio NOL balance separately

**FR-033:** System MUST handle state-specific NOL rules:
- State NOL limitation (80% vs 100% offset)
- State carryforward period (indefinite vs fixed years)
- State carryback rules (if any)

**FR-034:** System MUST reconcile federal vs state NOL differences:
- State addbacks/deductions may create NOL difference
- Example: Federal NOL = $1M, State addback = $200K → State NOL = $800K

**FR-035:** System MUST display separate NOL schedules for federal and state:
- Federal NOL schedule (total business loss)
- Ohio NOL schedule (apportioned loss)

### NOL Forms & Reporting

**FR-036:** System MUST generate Form 27-NOL (NOL Schedule) with:
- Table of NOL carryforwards by year
- Current year NOL deduction calculation
- Remaining NOL balance

**FR-037:** System MUST generate Form 27-NOL-CB (Carryback Application) when carryback elected:
- NOL amount and year of origin
- Carryback years and amounts
- Refund calculation

**FR-038:** System MUST generate NOL expiration report:
- NOLs expiring in next 3 years
- Amounts and expiration dates
- Recommended actions

**FR-039:** System MUST include NOL detail in tax return PDF:
- NOL deduction line with amount
- Footnote referencing attached Form 27-NOL

### Amended Return NOL Recalculation

**FR-040:** System MUST recalculate NOL when amended return filed:
- If amended return increases loss: Increase NOL balance
- If amended return decreases loss or creates income: Decrease NOL balance or eliminate NOL

**FR-041:** System MUST identify cascading effects of amended NOL:
- List all subsequent years that used the amended NOL
- Calculate impact on each year's tax liability
- Estimate refund if subsequent years are also amended

**FR-042:** System MUST generate amended NOL schedule:
- Original NOL
- Amended NOL
- Change amount
- Reason for amendment

**FR-043:** System MUST offer to prepare amended returns for subsequent years:
- Pre-fill amended returns with corrected NOL amounts
- Calculate refund for each amended year

### Validation & Audit Trail

**FR-044:** System MUST validate NOL deduction:
- NOL deduction ≤ Available NOL balance
- NOL deduction ≤ 80% × Taxable income (post-2017)
- NOL deduction ≤ 100% × Taxable income (pre-2018)

**FR-045:** System MUST reconcile NOL balance across years:
- Beginning balance (from prior year)
- Plus: New NOLs generated
- Minus: NOLs used
- Minus: NOLs expired
- Equals: Ending balance (carried to next year)

**FR-046:** System MUST create audit log for NOL transactions:
- NOL created (year and amount)
- NOL used (year, amount, taxable income)
- NOL expired (year and amount)
- NOL amended (year, old amount, new amount)
- Carryback elections (year and refund amount)

**FR-047:** System MUST flag discrepancies:
- NOL balance mismatch between years (prior year ending ≠ current year beginning)
- NOL usage exceeds available balance
- NOL deduction exceeds 80% limit
- Expired NOL usage attempt

---

## Key Entities

### NOL (Net Operating Loss)

Represents a net operating loss from a specific tax year.

**Attributes:**
- `nolId` (UUID): Unique identifier
- `businessId` (UUID): Foreign key to Business
- `taxYear` (number): Year loss originated (2020, 2021, etc.)
- `jurisdiction` (enum): FEDERAL | STATE_OHIO | MUNICIPALITY
- `municipalityCode` (string): If municipality-level NOL
- `entityType` (enum): C_CORP | S_CORP | PARTNERSHIP | SOLE_PROP
- `originalNOLAmount` (decimal): Loss amount when created
- `currentNOLBalance` (decimal): Remaining balance available
- `usedAmount` (decimal): Total NOL used across all years
- `expiredAmount` (decimal): Amount expired unused
- `expirationDate` (date): When NOL expires (null if indefinite)
- `carryforwardYears` (number): 20 for pre-TCJA, null for indefinite
- `isCarriedBack` (boolean): Whether NOL was carried back
- `carrybackAmount` (decimal): Amount used in carryback
- `carrybackRefund` (decimal): Tax refund from carryback
- `apportionmentPercentage` (decimal): Ohio % if multi-state (0-100%)
- `createdDate` (timestamp)
- `lastModifiedDate` (timestamp)

---

### NOLUsage

Represents NOL utilized in a specific tax year.

**Attributes:**
- `nolUsageId` (UUID)
- `nolId` (UUID): Foreign key to NOL
- `returnId` (UUID): Foreign key to TaxReturn
- `usageYear` (number): Year NOL was used
- `taxableIncomeBeforeNOL` (decimal): Income before NOL deduction
- `nolLimitationPercentage` (number): 80 or 100 (%)
- `maximumNOLDeduction` (decimal): Min(available NOL, limitation% × income)
- `actualNOLDeduction` (decimal): Amount actually used (may be less than max)
- `taxableIncomeAfterNOL` (decimal): Income after NOL deduction
- `taxSavings` (decimal): Tax saved due to NOL (deduction × tax rate)
- `orderingMethod` (enum): FIFO | MANUAL_OVERRIDE
- `overrideReason` (string): If manual ordering, why
- `usageDate` (timestamp)

---

### NOLCarryback

Represents NOL carried back to prior years for refund.

**Attributes:**
- `carrybackId` (UUID)
- `nolId` (UUID): Foreign key to NOL
- `carrybackYear` (number): Year NOL is carried back to (e.g., 2015)
- `priorYearTaxableIncome` (decimal): Income in carryback year before NOL
- `nolApplied` (decimal): Amount of NOL used in carryback year
- `priorYearTaxRate` (decimal): Tax rate in carryback year (%)
- `refundAmount` (decimal): (NOL applied) × (prior year rate)
- `priorYearReturnId` (UUID): Foreign key to prior year TaxReturn
- `carrybackFormId` (UUID): Form 27-NOL-CB generated
- `refundStatus` (enum): CLAIMED | APPROVED | DENIED | PAID
- `refundDate` (date): When refund was received
- `filedDate` (date): When carryback claim was filed

---

### NOLSchedule

Consolidated NOL schedule for a tax return (all NOL vintages).

**Attributes:**
- `scheduleId` (UUID)
- `returnId` (UUID): Foreign key to TaxReturn
- `taxYear` (number): Current year
- `totalBeginningBalance` (decimal): Sum of all NOL balances at start of year
- `newNOLGenerated` (decimal): NOL created in current year (if any)
- `totalAvailableNOL` (decimal): Beginning + New
- `nolDeduction` (decimal): Total NOL used in current year
- `expiredNOL` (decimal): NOL expired in current year
- `totalEndingBalance` (decimal): Available - Used - Expired
- `limitationPercentage` (number): 80 or 100 (%)
- `taxableIncomeBeforeNOL` (decimal)
- `taxableIncomeAfterNOL` (decimal)
- `nolVintages` (array): List of NOLUsage records (one per NOL vintage)

---

### NOLExpirationAlert

Alerts for NOLs approaching expiration.

**Attributes:**
- `alertId` (UUID)
- `businessId` (UUID): Foreign key
- `nolId` (UUID): Foreign key to NOL
- `taxYear` (number): Year of loss origin
- `nolBalance` (decimal): Remaining balance
- `expirationDate` (date)
- `yearsUntilExpiration` (number): Calculated field
- `severityLevel` (enum): CRITICAL (≤1 year) | WARNING (1-2 years) | INFO (2-3 years)
- `alertMessage` (string): "⚠️ $100K NOL expiring 12/31/2025 - Use in next 18 months or lose!"
- `dismissed` (boolean): Whether user has acknowledged alert
- `createdDate` (timestamp)

---

### NOLAmendment

Represents amended return that changes NOL.

**Attributes:**
- `amendmentId` (UUID)
- `nolId` (UUID): Foreign key to NOL
- `originalReturnId` (UUID): Original return
- `amendedReturnId` (UUID): Amended return
- `amendmentDate` (date)
- `originalNOL` (decimal): NOL before amendment
- `amendedNOL` (decimal): NOL after amendment
- `nolChange` (decimal): Difference (amended - original)
- `reasonForAmendment` (string): User explanation
- `affectedYears` (array): List of subsequent years that used this NOL
- `estimatedRefund` (decimal): Potential refund if subsequent years amended
- `cascadingAmendments` (array): Amended returns filed for subsequent years

---

## Success Criteria

- **Multi-Year Accuracy:** 100% of NOL carryforwards automatically tracked across years with zero manual entry required (vs current 0% - must re-enter each year)
- **Expiration Prevention:** Zero NOLs expire unused due to lack of tracking alerts (vs estimated 10-15% currently lost)
- **80% Compliance:** 100% of post-2017 NOL deductions comply with 80% limitation (automatic enforcement)
- **Carryback Refunds:** Businesses with 2018-2020 losses claim average $10K-$50K refunds via carryback (vs current $0 - no carryback support)
- **Audit Pass Rate:** NOL schedules pass audit with zero adjustments (complete documentation, accurate multi-year tracking)
- **Time Savings:** NOL schedule preparation takes 10 minutes vs 1-2 hours of manual spreadsheet work

---

## Assumptions

- Ohio follows federal NOL rules: 80% limitation post-2017, indefinite carryforward post-2017
- CARES Act carryback applies to 2018-2020 losses only (temporary provision)
- Pre-2018 NOLs have 20-year expiration (legacy rule)
- FIFO ordering (oldest first) is default and recommended to avoid expiration
- S-Corp and Partnership NOLs passed to owners are tracked at owner level (not entity level)
- Multi-state businesses calculate state NOL as federal NOL × state apportionment %
- Business maintains prior year tax return data in system (not manual re-entry)

---

## Dependencies

- **Rule Engine (Spec 4):** NOL limitation percentage (80% vs 100%), carryforward period, carryback rules stored as configurable rules
- **Schedule X Expansion (Spec 2):** Book-to-tax differences affect NOL calculation (M-1 adjustments)
- **Enhanced Discrepancy Detection (Spec 3):** Validate NOL deduction against available balance, flag over-utilization
- **Schedule Y Sourcing (Spec 5):** Multi-state apportionment percentage needed to calculate state NOL
- **Business Form Library (Spec 8):** Form 27-NOL and Form 27-NOL-CB generation
- **Historical Return Data:** System must retrieve prior year returns for carryback and multi-year tracking

---

## Out of Scope

- **IRC Section 382 limitation:** NOL limitation after ownership change (requires specialized calculation, defer to tax professional)
- **Built-in loss limitations:** NUBIL/NUBIG rules for corporate acquisitions (complex, rare)
- **Separate return limitation year (SRLY):** Consolidated return NOL rules (covered in Spec 11: Consolidated Returns)
- **AMT NOL:** Alternative Minimum Tax NOL calculation (different rules, complex)
- **State NOL conformity analysis:** Detailed comparison of all 50 states' NOL rules (focus on Ohio only)

---

## Edge Cases

1. **NOL exceeds 20-year carryforward:** Pre-2018 NOL reaches year 21 and expires. System must automatically remove from schedule and alert user: "2003 NOL of $50K expired unused in 2023."

2. **Carryback to zero-income year:** Business elects carryback but prior year had $0 taxable income (loss or break-even). System skips that year and applies to next oldest year.

3. **Negative NOL balance:** Data entry error causes NOL usage to exceed available balance. System flags validation error, blocks filing.

4. **Mid-year entity type change:** S-Corp elects C-Corp status mid-year. NOL generated before election passed to shareholders; NOL after election retained at entity level.

5. **State addback creates negative state NOL:** Federal NOL = $100K, State addback = $150K → State taxable income = $50K (not a loss). System correctly calculates no state NOL.

6. **Multiple NOL vintages with different expiration dates:** 2005 NOL expires 2025, 2010 NOL expires 2030, 2019 NOL never expires. System uses FIFO: 2005 first, then 2010, then 2019.

7. **Carryback refund exceeds tax paid:** NOL carryback calculation shows refund of $15K but actual tax paid in prior year was $10K. System caps refund at $10K (cannot refund more than was paid).

8. **Amended return eliminates NOL:** Original return had $100K NOL, amended return corrects to $0 loss. System must remove NOL from schedule and alert if subsequent years used it: "Amending 2022 eliminates NOL. You must also amend 2023 return to remove $80K NOL deduction."
