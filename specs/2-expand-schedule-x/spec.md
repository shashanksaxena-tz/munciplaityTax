# Feature Specification: Comprehensive Business Schedule X Reconciliation (25+ Fields)

**Feature Branch**: `2-expand-schedule-x`  
**Created**: 2025-11-27  
**Status**: Draft  
**Input**: Expand Schedule X from 6 basic fields to 25+ comprehensive book-to-tax reconciliation fields including depreciation (MACRS vs Book), amortization, officer compensation limits, related-party transactions, charitable contributions, meals & entertainment (50% rule), penalties/fines, political contributions, bad debt reserves, and all standard M-1 adjustments

## User Scenarios & Testing *(mandatory)*

### User Story 1 - CPA Performs Complete M-1 Reconciliation for C-Corporation (Priority: P1)

A CPA preparing a Form 27 for a C-Corp client needs to reconcile federal taxable income from Form 1120 with municipal taxable income. The federal return shows $500,000 taxable income, but includes $50,000 in depreciation differences (MACRS vs book), $15,000 in meals & entertainment (only 50% deductible federally, 0% municipally), and $10,000 in state tax deductions that must be added back.

**Why this priority**: This is the core compliance workflow - Schedule X (equivalent to IRS Form 1120 Schedule M-1) is required for all C-Corp and Partnership returns. Without comprehensive fields, CPAs cannot accurately prepare returns.

**Independent Test**: Can be fully tested by entering Federal Form 1120 Line 30 (taxable income) of $500,000, then adding back: $50,000 depreciation difference, $15,000 meals (50% of $30,000), $10,000 state taxes. System should calculate adjusted municipal taxable income of $575,000.

**Acceptance Scenarios**:

1. **Given** Federal Form 1120 shows taxable income of $500,000, **When** CPA adds $50,000 depreciation add-back (Book depreciation $80,000 - MACRS depreciation $130,000), **Then** system increases adjusted income to $550,000 and labels this adjustment "Depreciation - Book/Tax Difference"
2. **Given** federal return deducted $30,000 meals & entertainment, **When** CPA enters $30,000 in Meals field, **Then** system automatically calculates 50% add-back ($15,000) with explanation "Municipal does not allow meals & entertainment deduction - 100% add-back of federal 50% deduction"
3. **Given** company paid $10,000 state income tax deducted federally, **When** CPA enters state taxes in "Income Taxes (State/Local)" field, **Then** system adds full $10,000 back with label "State/local taxes not deductible for municipal purposes"
4. **Given** all add-backs total $75,000, **When** final calculation runs, **Then** system displays: Federal Taxable Income $500,000 + Add-backs $75,000 = Adjusted Municipal Income $575,000

---

### User Story 2 - Partnership Files Form 27 with Guaranteed Payments and Intangible Income (Priority: P1)

A law firm (partnership) files Form 1065 showing $300,000 ordinary business income. However, this includes $50,000 guaranteed payments to partners (Line 10 on 1065) which are deductible federally but not municipally. The firm also has $20,000 interest income and $15,000 dividend income (non-taxable for municipal) and incurred $2,000 expenses related to earning this intangible income (5% rule applies).

**Why this priority**: Partnerships are common in professional services (law, accounting, consulting) and have unique add-backs (guaranteed payments) and deductions (intangible income). Guaranteed payments alone affect 40% of business returns.

**Independent Test**: Can be tested by entering Form 1065 Line 22 (ordinary income) of $300,000, adding back $50,000 guaranteed payments, deducting $35,000 intangible income ($20K interest + $15K dividends), and applying 5% rule: $35,000 × 5% = $1,750 add-back for expenses. Final municipal income should be $316,750.

**Acceptance Scenarios**:

1. **Given** Form 1065 shows $300,000 ordinary business income and $50,000 guaranteed payments (Line 10), **When** CPA enters guaranteed payments in add-backs, **Then** system adds $50,000 with explanation "Guaranteed payments deductible for federal, not deductible for municipal"
2. **Given** firm has $20,000 interest income (Schedule K Line 5) and $15,000 dividends (Line 6a), **When** CPA enters these in Deductions section, **Then** system subtracts $35,000 with label "Intangible income not subject to municipal tax"
3. **Given** intangible deductions total $35,000, **When** system applies 5% Rule, **Then** it calculates $1,750 add-back ($35,000 × 5%) and labels it "Expenses incurred to earn non-taxable intangible income (5% Rule)"
4. **Given** 5% rule can be manually overridden, **When** CPA enters $2,500 actual expenses traced to earning intangible income, **Then** system accepts override and uses $2,500 instead of auto-calculated $1,750

---

### User Story 3 - S-Corporation with Related-Party Transactions and Officer Compensation (Priority: P2)

An S-Corp with 2 shareholder-employees shows federal taxable income of $400,000. The company paid $150,000 in officer compensation to the owner-shareholders and paid $10,000 rent to a related-party LLC (also owned by shareholders). Municipal rules limit related-party rent to fair market value of $7,500.

**Why this priority**: S-Corps are the most common business entity (50%+ of all business returns). Related-party transaction limits prevent tax avoidance through inflated expenses to related entities.

**Independent Test**: Can be tested by entering S-Corp Form 1120-S taxable income of $400,000, adding back $2,500 excess related-party rent ($10,000 paid - $7,500 FMV). System should flag this as "Related-Party Rent Adjustment" and calculate adjusted income of $402,500.

**Acceptance Scenarios**:

1. **Given** S-Corp paid $150,000 officer compensation (Form 1120-S Line 7), **When** CPA reviews officer compensation field, **Then** system displays informational note "Officer compensation is deductible for municipal if reasonable - ensure amounts align with industry standards"
2. **Given** company paid $10,000 rent to related-party LLC, **When** CPA enters $10,000 paid and $7,500 FMV in Related-Party Transactions section, **Then** system calculates $2,500 add-back with label "Excess related-party rent disallowed"
3. **Given** officer compensation of $150,000 for 2 officers ($75K each) in a business with $400,000 income, **When** system applies reasonableness test, **Then** it compares to IRS reasonable compensation guidelines and shows WARNING if > 50% of net income

---

### User Story 4 - Corporation with Charitable Contributions Exceeding 10% Limit (Priority: P2)

A C-Corporation had $600,000 taxable income before charitable contributions and donated $80,000 to qualified charities (deducted on Form 1120 Line 19). Federal law allows deduction up to 10% of taxable income ($60,000), with $20,000 carried forward. Municipal follows federal limits.

**Why this priority**: Charitable contribution limits affect 20% of C-Corp returns and require carryforward tracking. Incorrect handling creates multi-year errors.

**Independent Test**: Can be tested by entering federal taxable income of $600,000 and charitable contributions of $80,000. System should calculate current year deduction of $60,000 (10% limit) and carryforward of $20,000 to next year, resulting in no municipal add-back (follows federal treatment).

**Acceptance Scenarios**:

1. **Given** federal return shows $80,000 charitable contributions with $60,000 deducted and $20,000 carried forward, **When** CPA enters charitable contribution data, **Then** system displays "Municipal follows federal treatment - $60,000 deductible this year, $20,000 carryforward to 2025"
2. **Given** federal erroneously deducted full $80,000 (ignoring 10% limit), **When** CPA enters contributions, **Then** system calculates correct 10% limit ($60,000) and shows add-back of $20,000 with explanation "Charitable contributions limited to 10% of taxable income - federal calculation error"
3. **Given** prior year carryforward of $15,000 exists, **When** current year has $50,000 new contributions and $70,000 limit (10% of $700K income), **Then** system applies full $65,000 ($50K + $15K) deduction with no carryforward

---

### User Story 5 - Service Business with Domestic Production Activities Deduction (Priority: P3)

A manufacturing company claimed $25,000 Domestic Production Activities Deduction (DPAD/Section 199) on their federal Form 1120 (pre-TCJA returns or specific qualified activities). Municipal does not allow this deduction and requires add-back.

**Why this priority**: DPAD affects manufacturing and certain service businesses. While TCJA eliminated it for most taxpayers, some JEDD zones or special entities still qualify, and prior-year audits still require it.

**Independent Test**: Can be tested by entering federal Form 1120 showing DPAD of $25,000 (Line 25 on older returns). System should add back full $25,000 with label "Domestic Production Activities Deduction not allowed for municipal purposes."

**Acceptance Scenarios**:

1. **Given** federal Form 1120 includes $25,000 DPAD deduction, **When** CPA enters this in Deductions section, **Then** system adds back $25,000 with explanation "Municipal does not recognize Section 199 deduction"
2. **Given** business is in JEDD zone with special federal deduction rules, **When** system checks business address against JEDD zone database, **Then** it adjusts add-back rules according to JEDD-specific tax code
3. **Given** prior-year return (2017) included DPAD, **When** CPA is amending 2017 return, **Then** system applies historical tax rules (DPAD was allowed pre-TCJA) and does NOT add back

---

### Edge Cases

- **First-year business with startup costs**: Company capitalized $50,000 startup costs federally (amortized over 180 months) but wants immediate deduction municipally. System should handle different amortization schedules for book vs tax vs municipal.
- **Section 179 election**: Business elected $1M immediate expensing federally but municipal limits Section 179 to $500K. System should add back $500K excess.
- **Net Operating Loss from federal**: Federal return shows -$200,000 loss carried from 2023. Municipal has separate NOL rules (50% limit). System should not automatically carry federal NOL - must be entered in separate NOL tracker.
- **Penalties paid to government agencies**: $10,000 EPA fine paid. Federally non-deductible, so already added back on Form 1120. Municipal should NOT double-add-back - system must detect this.
- **Bonus depreciation**: Federal allows 100% bonus depreciation on equipment ($200K). Municipal follows MACRS (5-year). Huge depreciation difference requiring multi-year tracking.
- **Political contributions**: $5,000 donated to political campaign. Federally non-deductible (already in federal taxable income). Municipal should NOT add back - already disallowed federally.
- **Club dues and lobbying**: $3,000 country club dues and $8,000 lobbying expenses. Federally 0% deductible (already in federal income). Municipal should NOT add back.
- **Mixed-use vehicle**: 60% business use, 40% personal. Federal only deducted 60% of $30K cost ($18K). Municipal may have stricter rules - system should allow manual override with justification.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Add-Back Fields (Increase Federal Taxable Income)

- **FR-001**: System MUST provide field for **Depreciation Adjustments (Book vs Tax)** - difference between book depreciation expense and MACRS/tax depreciation, with helper text explaining MACRS vs straight-line differences
- **FR-002**: System MUST provide field for **Amortization Adjustments** - similar to depreciation, for intangible assets (goodwill, patents, trademarks) where book vs tax amortization differs
- **FR-003**: System MUST provide field for **Income Taxes (State/Local/Foreign)** - add back state income taxes, city taxes, and foreign income taxes deducted on federal return (already exists, keep)
- **FR-004**: System MUST provide field for **Guaranteed Payments to Partners** - Form 1065 Line 10 guaranteed payments that are deductible federally but not municipally (already exists for partnerships, keep)
- **FR-005**: System MUST provide field for **Meals & Entertainment** - 100% of meals/entertainment expenses, with auto-calculation option: if user enters federal 50% deduction, system calculates 100% add-back for municipal
- **FR-006**: System MUST provide field for **Related-Party Excess Expenses** - excess payments to related parties above fair market value (e.g., rent, management fees, interest)
- **FR-007**: System MUST provide field for **Penalties and Fines** - government penalties and fines that are non-deductible federally AND municipally (should already be in federal income, but some taxpayers erroneously deduct)
- **FR-008**: System MUST provide field for **Political Contributions** - contributions to political campaigns or lobbying expenses (should already be non-deductible federally, but verify)
- **FR-009**: System MUST provide field for **Officer Life Insurance Premiums** - premiums paid by corporation on officer life insurance where corp is beneficiary (non-deductible federally, but sometimes deducted in error)
- **FR-010**: System MUST provide field for **Capital Losses in Excess of Capital Gains** - Form 1120 Line 8 capital loss carryforward that was deducted federally (already exists as "Losses 1231", keep but expand label)
- **FR-011**: System MUST provide field for **Federal Income Tax Refunds** - if prior year federal refund was included in income but state/local taxes were not (rare, but required for complete M-1)
- **FR-012**: System MUST provide field for **Expenses Related to Tax-Exempt Income (5% Rule)** - auto-calculated as 5% of total deductions (interest, dividends, capital gains) OR manual override (already exists, keep)
- **FR-013**: System MUST provide field for **Section 179 Excess Depreciation** - portion of Section 179 expensing that exceeds municipal limits (if municipal has lower cap than federal $1M)
- **FR-014**: System MUST provide field for **Bonus Depreciation** - 100% federal bonus depreciation not allowed municipally (should be tracked in Depreciation Adjustments field but needs separate line for clarity)
- **FR-015**: System MUST provide field for **Bad Debt Reserve Increase** - if company uses reserve method for books but direct write-off for tax, add back reserve increase
- **FR-016**: System MUST provide field for **Charitable Contributions Exceeding Limit** - contributions exceeding 10% limit that were deducted federally in error (municipal follows federal 10% limit, so only add back if federal return has error)
- **FR-017**: System MUST provide field for **Domestic Production Activities Deduction (DPAD)** - Section 199 deduction taken federally but not allowed municipally (historical for pre-TCJA returns)
- **FR-018**: System MUST provide field for **Stock-Based Compensation Adjustment** - difference between book expense (ASC 718 fair value) and tax deduction (intrinsic value at exercise)
- **FR-019**: System MUST provide field for **Inventory Accounting Method Change** - Section 481(a) adjustment for changes in inventory method (LIFO to FIFO, etc.)
- **FR-020**: System MUST provide field for **Other Add-Backs** - catch-all field with required description for any items not covered by specific fields (already exists, keep)

#### Deduction Fields (Decrease Federal Taxable Income)

- **FR-021**: System MUST provide field for **Interest Income** - taxable interest that is non-taxable for municipal purposes (already exists, keep)
- **FR-022**: System MUST provide field for **Dividend Income** - qualified and ordinary dividends non-taxable municipally (already exists, keep)
- **FR-023**: System MUST provide field for **Capital Gains** - net capital gains non-taxable municipally (already exists, keep)
- **FR-024**: System MUST provide field for **Section 179 Recapture** - if asset was sold/disposed before end of recovery period, recaptured federal deduction may not need to be recaptured municipally (rare, but complete M-1 requires it)
- **FR-025**: System MUST provide field for **Municipal Bond Interest Income** - if business has municipal bonds, this income is tax-exempt federally but may be taxable at different municipal level (cross-jurisdiction)
- **FR-026**: System MUST provide field for **Depletion Deduction Difference** - if percentage depletion (oil/gas/mining) for federal exceeds cost depletion required for municipal
- **FR-027**: System MUST provide field for **Other Deductions** - catch-all field with required description (already exists, keep)

#### Calculation & Validation

- **FR-028**: System MUST calculate **Total Add-Backs** by summing all FR-001 through FR-020 fields
- **FR-029**: System MUST calculate **Total Deductions** by summing all FR-021 through FR-027 fields
- **FR-030**: System MUST calculate **Adjusted Municipal Taxable Income** = Federal Taxable Income + Total Add-Backs - Total Deductions
- **FR-031**: System MUST display each line item with: Field Label, Amount Entered, Help Icon (explaining what this adjustment represents), and Auto-Calc Icon (if field has auto-calculation option like 5% Rule or Meals 50%→100%)
- **FR-032**: System MUST provide "Import from Federal Return" button that pre-fills fields from uploaded Form 1120/1065 using AI extraction
- **FR-033**: System MUST validate that Federal Taxable Income matches Form 1120 Line 30 (C-Corp) or Form 1065 Line 22 (Partnership) or Form 1120-S Line 23 (S-Corp) from uploaded PDF
- **FR-034**: System MUST flag for review if Adjusted Municipal Income differs from Federal Income by more than 20% (suggests data entry error or unusual transaction)
- **FR-035**: System MUST save Schedule X data in structured format compatible with PDF generation service for Form 27 creation
- **FR-036**: System MUST allow CPA to attach supporting documentation (Excel workpaper, depreciation schedule) to each adjustment line item
- **FR-037**: System MUST track which fields were auto-calculated vs manually entered for audit trail purposes
- **FR-038**: System MUST support multi-year comparison view showing current year vs prior year Schedule X side-by-side for recurring adjustments (depreciation, amortization)

#### AI Extraction Integration

- **FR-039**: System MUST update AI extraction prompts (Gemini service) to extract all 27 new Schedule X fields from Form 1120/1065/1120-S PDFs
- **FR-040**: AI extraction MUST identify: (a) Depreciation expense (Line 20 on 1120), (b) State/local taxes (Line 17), (c) Charitable contributions (Line 19), (d) Interest expense (Line 18), (e) Guaranteed payments (Line 10 on 1065), (f) Officer compensation (Line 12 on 1120)
- **FR-041**: AI extraction MUST parse attached depreciation schedules (Form 4562) to identify MACRS vs book depreciation differences
- **FR-042**: System MUST display AI confidence scores for each extracted Schedule X field (similar to existing form extraction confidence)
- **FR-043**: System MUST allow user to override any AI-extracted value with manual entry, preserving AI value in history for comparison

### Key Entities *(include if feature involves data)*

- **BusinessScheduleXDetails** (EXPANDED from existing 6 fields to 27 fields):
  - **fedTaxableIncome**: number - Form 1120 Line 30 / 1065 Line 22 / 1120-S Line 23
  - **addBacks**: object with properties:
    - depreciationAdjustment: number - Book depreciation minus MACRS (FR-001)
    - amortizationAdjustment: number - Book amortization minus tax (FR-002)
    - incomeAndStateTaxes: number - State/local/foreign income taxes (FR-003) [existing]
    - guaranteedPayments: number - Form 1065 Line 10 (FR-004) [existing]
    - mealsAndEntertainment: number - 100% add-back (FR-005)
    - relatedPartyExcess: number - Above FMV payments (FR-006)
    - penaltiesAndFines: number - Government penalties (FR-007)
    - politicalContributions: number - Campaign contributions (FR-008)
    - officerLifeInsurance: number - Life insurance premiums (FR-009)
    - losses1231: number - Capital losses excess (FR-010) [existing, rename from "losses1231" to "capitalLossExcess"]
    - federalTaxRefunds: number - Prior year federal refunds (FR-011)
    - expensesOnIntangibleIncome: number - 5% Rule (FR-012) [existing]
    - section179Excess: number - Section 179 over municipal limit (FR-013)
    - bonusDepreciation: number - 100% federal bonus (FR-014)
    - badDebtReserveIncrease: number - Reserve method adjustment (FR-015)
    - charitableContributionExcess: number - Over 10% limit (FR-016)
    - domesticProductionActivities: number - DPAD Section 199 (FR-017)
    - stockCompensationAdjustment: number - Book vs tax difference (FR-018)
    - inventoryMethodChange: number - Section 481(a) (FR-019)
    - otherAddBacks: number - Catch-all (FR-020) [existing as "other"]
    - otherAddBacksDescription: string - Required if otherAddBacks > 0
  - **deductions**: object with properties:
    - interestIncome: number - Tax-exempt interest (FR-021) [existing]
    - dividends: number - Dividend income (FR-022) [existing]
    - capitalGains: number - Net capital gains (FR-023) [existing]
    - section179Recapture: number - Recapture adjustment (FR-024)
    - municipalBondInterest: number - Cross-jurisdiction bonds (FR-025)
    - depletionDifference: number - Percentage vs cost (FR-026)
    - section179Excess: number - Over municipal cap (FR-013) [existing]
    - otherDeductions: number - Catch-all (FR-027) [existing as "other"]
    - otherDeductionsDescription: string - Required if otherDeductions > 0
  - **calculatedFields**: object (read-only, computed)
    - totalAddBacks: number - Sum of all add-backs (FR-028)
    - totalDeductions: number - Sum of all deductions (FR-029)
    - adjustedMunicipalIncome: number - Final calculation (FR-030)
  - **metadata**: object
    - lastModified: timestamp
    - autoCalculatedFields: string[] - List of field names that were auto-calculated
    - manualOverrides: string[] - List of field names manually overridden after AI extraction
    - attachedDocuments: array of {fileName: string, fileUrl: string, fieldName: string}

## Success Criteria *(mandatory)*

- CPAs can complete full Schedule X reconciliation in under 10 minutes with AI-assisted extraction (vs 45+ minutes manual)
- 90% of AI-extracted Schedule X fields require zero manual correction for standard C-Corp returns
- System correctly handles 95% of common book-to-tax adjustments without manual formula entry
- Adjusted Municipal Income calculation matches CPA's manual workpaper within $100 for 98% of returns
- Schedule X data exports to Form 27 PDF with all line items properly labeled and totaled
- Multi-year comparison view loads in under 2 seconds for businesses with 3+ prior year returns

## Assumptions

- Federal Form 1120/1065/1120-S is primary source document (uploaded as PDF)
- Municipal tax code follows federal treatment except where explicitly stated otherwise (state taxes, intangible income, meals, guaranteed payments)
- 5% Rule for intangible expenses is standard unless taxpayer can prove higher actual expense with documentation
- AI extraction service (Gemini) can parse Form 1120 Schedule M-1 and Form 4562 (depreciation schedule) with 85%+ accuracy
- Businesses using accrual accounting method (vs cash) - adjustments like bad debt reserves and inventory changes only apply to accrual taxpayers
- MACRS depreciation is standard for tax purposes (vs straight-line or double-declining for books)
- Officer compensation reasonableness test uses IRS guidelines: compensation should not exceed 50% of net income for closely-held corporations
- Related-party transaction FMV is determined by taxpayer (CPA provides documentation) - system does not calculate FMV automatically

## Dependencies

- AI Extraction Service: Must be updated to parse Form 1120 Schedule M-1, Form 4562, and all related schedules
- PDF Generation Service: Must support expanded Schedule X layout with 27+ line items
- Business Tax Calculator (BusinessTaxCalculator.java): Must be updated to use expanded BusinessScheduleXDetails
- NetProfitsWizard UI: Must be redesigned to show 27 fields in organized sections (collapsible accordion recommended)
- Constants: DEFAULT_BUSINESS_RULES must be updated with new rule parameters (municipal Section 179 limit, meals treatment, etc.)

## Out of Scope

- Automatic FMV calculation for related-party transactions - taxpayer must provide FMV
- Integration with depreciation software (ProSystem fx, CCH ProSystem) - manual entry only
- Multi-state apportionment adjustments - separate feature (Schedule Y handles this)
- Consolidated return adjustments for parent-subsidiary groups - separate feature
- AMT (Alternative Minimum Tax) adjustments - municipal does not have AMT
- Foreign tax credit calculations - beyond municipal scope
- GILTI (Global Intangible Low-Taxed Income) and FDII (Foreign-Derived Intangible Income) - not applicable to municipal
- Section 163(j) interest limitation - follow federal treatment, no municipal adjustment

## Open Questions

None - all requirements defined based on IRS Form 1120 Schedule M-1 standard and municipal tax code requirements.
