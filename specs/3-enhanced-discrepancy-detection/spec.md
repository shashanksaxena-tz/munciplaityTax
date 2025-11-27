# Feature Specification: Enhanced Discrepancy Detection (10+ Validation Rules)

**Feature Branch**: `3-enhanced-discrepancy-detection`  
**Created**: 2025-11-27  
**Status**: Draft  
**Input**: Implement comprehensive discrepancy detection with 10+ validation rules including W-2 box consistency (Box 1 vs 18), Schedule C income validation, rental property counts, K-1 allocation checks, municipal credit limits, withholding rate validation, and cross-year carryforward verification

## User Scenarios & Testing *(mandatory)*

### User Story 1 - W-2 Box Validation Catches Data Entry Error (Priority: P1)

An individual taxpayer enters their W-2 manually. They correctly enter Box 1 (Federal wages) as $75,000 but accidentally enter Box 18 (Local wages) as $7,500 (missing a zero). The system detects that Box 18 is only 10% of Box 1, which is highly unusual for an employee working full-year in Dublin.

**Why this priority**: W-2 data entry errors are the #1 cause of incorrect tax calculations. Box 18 (local wages) should typically equal Box 1 (federal wages) for full-time Dublin residents. Catching this prevents 30% of filing errors.

**Independent Test**: Can be fully tested by entering W-2 with Box 1 = $75,000 and Box 18 = $7,500. System should flag: "WARNING: Local wages (Box 18) are significantly lower than Federal wages (Box 1). Verify you entered Box 18 correctly. For full-year Dublin employment, these amounts should be similar."

**Acceptance Scenarios**:

1. **Given** W-2 shows Box 1 Federal Wages = $75,000 and Box 18 Local Wages = $7,500, **When** validation runs, **Then** system displays HIGH severity warning "Box 18 is 90% lower than Box 1 - verify data entry"
2. **Given** W-2 shows Box 1 = $75,000 and Box 18 = $72,000 (employee started job mid-year), **When** validation runs, **Then** system accepts variance as reasonable (within 10% threshold)
3. **Given** employee has multiple W-2s (job change mid-year), **When** Box 18 on W-2 #1 shows $30,000 (Jan-June) and W-2 #2 shows $45,000 (July-Dec), **Then** system validates each W-2 independently and flags if either has >20% variance from Box 1
4. **Given** W-2 Box 19 (Local tax withheld) shows $2,000, **When** Box 18 (Local wages) is $7,500, **Then** system calculates implied rate of 26.7% (far exceeds 2.5% municipal rate) and flags "Withheld tax seems too high for reported wages - verify Box 18 entry"

---

### User Story 2 - Schedule C Income vs Estimated Tax Paid Mismatch (Priority: P1)

A self-employed consultant reports $120,000 net profit on Schedule C but made $0 estimated tax payments during the year. For this income level, quarterly estimates are required (90% safe harbor rule). System flags this for potential underpayment penalty.

**Why this priority**: Self-employed individuals often forget estimated taxes, leading to penalties. Catching this allows taxpayers to make 4th quarter payment before year-end or request penalty waiver.

**Independent Test**: Can be tested by entering Schedule C with $120,000 net profit and $0 estimated payments. System should calculate required estimated payments (90% of $120,000 × 2% = $2,160) and flag "Required estimated payments not made - underpayment penalty may apply."

**Acceptance Scenarios**:

1. **Given** Schedule C net profit is $120,000 and estimated payments are $0, **When** validation runs, **Then** system calculates required payments of $2,160 (90% of $2,400 liability) and shows WARNING "Estimated tax payments appear to be missing"
2. **Given** taxpayer made 4 quarterly estimated payments totaling $2,200, **When** validation runs, **Then** system shows "✓ Estimated tax requirements met - safe harbor satisfied"
3. **Given** Schedule C shows $120,000 profit but prior year liability was $3,000, **When** safe harbor calc runs, **Then** system uses 100% of prior year ($3,000) as safe harbor and shows "Estimated payments of $2,200 fall short of 100% prior year safe harbor by $800"
4. **Given** taxpayer has W-2 withholding of $2,500 that exceeds safe harbor, **When** validation runs, **Then** system combines W-2 withholding + estimates and shows "✓ Safe harbor met through W-2 withholding"

---

### User Story 3 - Schedule E Rental Property Count Validation (Priority: P2)

A taxpayer reports income from 3 rental properties on Schedule E but only uploads 2 property addresses. System flags the missing property data.

**Why this priority**: Incomplete Schedule E reporting is common. Missing properties means missing income/expenses and incorrect tax calculation. Affects 15% of Schedule E filers.

**Independent Test**: Can be tested by entering Schedule E with 3 rental properties showing net income of $15,000, $12,000, and $8,000, but only providing addresses for 2 properties. System should flag "3 rental properties reported but only 2 addresses provided - complete all property details."

**Acceptance Scenarios**:

1. **Given** Schedule E lists 3 rental properties with income, **When** only 2 property addresses are entered, **Then** system shows MEDIUM severity warning "Missing property address for Rental #3"
2. **Given** all 3 rental properties have complete data (address, income, expenses), **When** validation runs, **Then** system shows "✓ All rental properties have complete information"
3. **Given** rental property address is "123 Main St, Columbus OH" (outside Dublin), **When** validation runs, **Then** system flags "Rental property is outside Dublin municipal limits - verify this income is subject to Dublin tax"
4. **Given** rental shows gross income of $24,000 and expenses of $30,000 (loss of $6,000), **When** validation runs against IRS passive loss limits, **Then** system shows informational note "Rental loss may be limited by passive activity rules - verify with tax professional"

---

### User Story 4 - K-1 Income Allocation Check (Priority: P2)

A partner in a partnership receives Form K-1 showing $100,000 ordinary business income (Box 1). The partnership's Form 1065 Schedule K-1 also shows $50,000 guaranteed payments (Box 4c). System validates that both amounts are reported on individual return.

**Why this priority**: K-1 income is complex and frequently under-reported. Partnerships often have both ordinary income AND guaranteed payments that must both be included. Affects 20% of K-1 filers.

**Independent Test**: Can be tested by uploading K-1 with Box 1 (ordinary income) = $100,000 and Box 4c (guaranteed payments) = $50,000. If Schedule X only shows $100,000, system should flag "K-1 includes $50,000 guaranteed payments (Box 4c) - verify this income is included in Schedule X."

**Acceptance Scenarios**:

1. **Given** K-1 shows $100,000 ordinary income (Box 1) and $50,000 guaranteed payments (Box 4c), **When** Schedule X only includes $100,000, **Then** system flags "Missing $50,000 guaranteed payments from K-1 - this income must be reported separately"
2. **Given** taxpayer includes both ordinary income and guaranteed payments correctly, **When** validation runs, **Then** system shows "✓ K-1 income components properly reported on Schedule X"
3. **Given** K-1 shows partner's profit share is 25% of partnership, **When** partnership's Form 1065 shows total ordinary income of $500,000, **Then** system validates partner's K-1 shows $125,000 (25% × $500,000)
4. **Given** K-1 shows Section 179 deduction of $20,000 (Box 12), **When** Schedule X deductions don't include this, **Then** system flags "K-1 includes Section 179 deduction that may need to be adjusted for municipal purposes"

---

### User Story 5 - Municipal Credit Limit Validation (Priority: P1)

A taxpayer claims $3,000 municipal tax credit for taxes paid to Cleveland, but their total Dublin tax liability is only $2,500. Municipal credits cannot exceed liability (no refundable credits). System flags the excess credit.

**Why this priority**: Credit limits are strict IRS/municipal rules. Allowing credits to exceed liability creates invalid returns that will be rejected. Critical validation.

**Independent Test**: Can be tested by entering Schedule Y with $3,000 Cleveland credit when calculated Dublin liability is $2,500. System should cap credit at $2,500 and show "Municipal credit limited to Dublin tax liability - $500 credit cannot be applied this year."

**Acceptance Scenarios**:

1. **Given** taxpayer claims $3,000 Cleveland credit and Dublin liability is $2,500, **When** validation runs, **Then** system limits credit to $2,500 and shows WARNING "Credit exceeds tax liability - maximum credit allowed is $2,500"
2. **Given** taxpayer has $2,000 Cleveland credit and $1,000 Cincinnati credit (total $3,000) against $2,500 liability, **When** validation runs, **Then** system applies $2,000 Cleveland first, then limits Cincinnati to $500, showing "Municipal credit cap applied - $500 Cincinnati credit cannot be used"
3. **Given** credits exactly equal liability ($2,500 credit, $2,500 liability), **When** validation runs, **Then** system shows "✓ Municipal credits fully applied - no liability remaining"
4. **Given** taxpayer worked in Cleveland for 6 months ($30,000 income, $600 withheld) and Dublin for 6 months ($30,000 income, $600 withheld), **When** total liability calc runs, **Then** system applies Cleveland credit and shows final liability $0 with $0 refund (credits can reduce to zero but not create refund)

---

### User Story 6 - Withholding Rate Validation (Priority: P2)

A W-2 shows Box 18 (Local wages) = $50,000 and Box 19 (Local tax withheld) = $1,500. The implied withholding rate is 3.0%, which exceeds Dublin's maximum rate of 2.5%. System flags this as employer over-withholding or data entry error.

**Why this priority**: Employers sometimes withhold at wrong rate or taxpayers enter Box 19 incorrectly. Identifying over-withholding prevents refund confusion.

**Independent Test**: Can be tested by entering W-2 with Box 18 = $50,000 and Box 19 = $1,500. System should calculate 3.0% rate (1500/50000) and show "Withholding rate of 3.0% exceeds Dublin rate of 2.5% - verify employer withheld at correct rate or check Box 19 entry."

**Acceptance Scenarios**:

1. **Given** W-2 shows withholding rate of 3.0% (exceeds 2.5% max), **When** validation runs, **Then** system displays MEDIUM severity warning "Over-withholding detected - contact employer to verify withholding rate"
2. **Given** W-2 shows withholding rate of 2.0% (within reasonable range), **When** validation runs, **Then** system accepts without warning (2.0% is within 0.5% tolerance of 2.5%)
3. **Given** W-2 shows $50,000 wages with $0 withholding, **When** validation runs, **Then** system flags "No local tax withheld - verify employer withholds Dublin tax or if you need to make estimated payments"
4. **Given** employer is headquartered in Cleveland but employee works from home in Dublin, **When** W-2 shows Cleveland withholding instead of Dublin, **Then** system flags "Employer withheld for wrong municipality - you may need amended W-2 or file credit claim"

---

### User Story 7 - Cross-Year Carryforward Verification (Priority: P3)

A taxpayer claims $5,000 Net Operating Loss (NOL) carryforward from 2023 on their 2024 return. System checks prior year return and finds 2023 showed $0 NOL. Inconsistency flagged.

**Why this priority**: Carryforwards are often claimed incorrectly. Taxpayers may confuse federal vs municipal carryforwards or claim non-existent carryforwards. Prevents fraud and errors.

**Independent Test**: Can be tested by entering 2024 return with $5,000 NOL carryforward claimed, but querying 2023 return shows $0 NOL generated. System should flag "2024 return claims $5,000 NOL carryforward but 2023 return shows no loss - verify source of carryforward."

**Acceptance Scenarios**:

1. **Given** 2024 return claims $5,000 NOL carryforward, **When** 2023 return is queried and shows $0 NOL, **Then** system displays HIGH severity warning "Carryforward amount does not match prior year return"
2. **Given** 2023 return generated $5,000 NOL, **When** 2024 return claims $5,000 carryforward, **Then** system shows "✓ NOL carryforward verified against 2023 return"
3. **Given** 2023 return generated $8,000 NOL and 2024 applies $5,000, **When** validation runs, **Then** system shows "✓ Partial NOL utilization - $3,000 NOL remains for future years"
4. **Given** taxpayer claims $10,000 NOL from 2020, **When** system checks 2020 return doesn't exist in database, **Then** system shows "Cannot verify 2020 NOL carryforward - attach prior year return for verification"

---

### User Story 8 - Federal Form 1040 AGI vs Local Calculation (Priority: P2)

A taxpayer uploads Federal Form 1040 showing Adjusted Gross Income (AGI) of $100,000 (Line 11). However, their W-2s only total $80,000 and Schedule C shows $15,000, totaling $95,000. The $5,000 difference suggests missing income or Form 1040 error.

**Why this priority**: Form 1040 is the master document. If local calculation doesn't reconcile to federal, either local data is incomplete or federal return has errors. Critical for audit defense.

**Independent Test**: Can be tested by uploading Form 1040 with Line 11 AGI = $100,000, but entering W-2 wages of $80,000 and Schedule C of $15,000 (total $95,000). System should flag "$5,000 income difference between Federal Form 1040 AGI and local calculation - verify all income sources are reported."

**Acceptance Scenarios**:

1. **Given** Form 1040 AGI is $100,000 and local calc is $95,000, **When** validation runs, **Then** system shows MEDIUM severity warning "$5,000 unexplained difference - common causes: Interest, dividends, or other income not yet entered locally"
2. **Given** difference is explainable by interest/dividend income (not subject to local tax), **When** user enters $5,000 interest income in Schedule Y deductions, **Then** system shows "✓ Federal/local reconciliation complete - difference explained by non-taxable interest income"
3. **Given** Form 1040 includes $10,000 unemployment compensation (not taxable locally), **When** validation runs, **Then** system shows informational note "Federal AGI includes $10,000 unemployment - this income is not subject to local tax"
4. **Given** federal and local calculations match exactly, **When** validation runs, **Then** system shows "✓ Federal Form 1040 AGI reconciles to local income calculation"

---

### User Story 9 - Duplicate W-2 Detection (Priority: P2)

A taxpayer accidentally uploads the same W-2 PDF twice, resulting in double-counted wages of $100,000 instead of $50,000. System detects identical employer EIN, employee SSN, and wage amounts and flags duplicate.

**Why this priority**: Duplicate form entry is common user error, especially when uploading multiple years or corrected W-2s. Preventing double-counting is critical for accuracy.

**Independent Test**: Can be tested by uploading W-2 with Employer EIN 12-3456789, Employee SSN 123-45-6789, and wages $50,000, then uploading identical second W-2. System should flag "Duplicate W-2 detected - same employer, SSN, and amounts as previously uploaded W-2. Remove duplicate to avoid double-counting."

**Acceptance Scenarios**:

1. **Given** two W-2s with identical EIN, SSN, and wage amounts are uploaded, **When** validation runs, **Then** system flags HIGH severity "Duplicate W-2 detected - only one copy should be included"
2. **Given** two W-2s from same employer (EIN) but different wage amounts, **When** validation runs, **Then** system accepts both and shows "Multiple W-2s from same employer - verify you had multiple jobs/positions or this is a corrected W-2"
3. **Given** corrected W-2 is uploaded to replace original, **When** user marks it as "Corrected W-2", **Then** system archives original W-2 and uses corrected amounts, showing "Original W-2 replaced by corrected version"
4. **Given** joint filers have W-2s from same employer (both work for Company X), **When** validation runs, **Then** system accepts both because SSNs differ, showing "✓ Both spouses work for same employer - W-2s valid"

---

### User Story 10 - Schedule E Passive Loss Limitation Check (Priority: P3)

A taxpayer reports $25,000 rental loss on Schedule E. Their AGI is $180,000 (exceeds $150,000 threshold for passive loss deduction). System flags that full loss may not be deductible due to IRS passive activity limitations.

**Why this priority**: Passive loss rules are complex IRS regulations. While municipal follows federal treatment, taxpayers often claim full losses without understanding limits. Informational validation helps prevent errors.

**Independent Test**: Can be tested by entering Schedule E rental loss of $25,000 and Form 1040 AGI of $180,000. System should show "AGI exceeds $150,000 passive loss threshold - rental loss may be limited or suspended. Verify federal Form 8582 (Passive Activity Loss Limitations) was prepared."

**Acceptance Scenarios**:

1. **Given** rental loss is $25,000 and AGI is $180,000 (exceeds $150K), **When** validation runs, **Then** system shows informational warning "Passive loss limits may apply - losses exceeding allowed amount must be suspended"
2. **Given** rental loss is $10,000 and AGI is $120,000 (under $150K threshold), **When** validation runs, **Then** system shows "✓ Rental loss within passive activity limits - full deduction allowed"
3. **Given** taxpayer qualifies as real estate professional (>750 hours/year), **When** they check "Real Estate Professional" box, **Then** system skips passive loss validation and shows "✓ Real estate professional exception - passive loss limits do not apply"
4. **Given** Schedule E shows suspended losses from prior years, **When** current year has rental income, **Then** system prompts "Prior year suspended losses may now be deductible against current rental income"

---

### Edge Cases

- **Non-resident with partial-year Dublin employment**: W-2 Box 18 lower than Box 1 is normal. System should ask "Did you work full year in Dublin?" before flagging variance.
- **Multiple state W-2s**: Employee worked in 3 states during year. Each W-2 has different local tax jurisdiction. System must validate each separately, not aggregate.
- **Corrected W-2 after original filing**: Original W-2 showed $50,000, corrected shows $52,000. System must allow replacement without flagging as duplicate.
- **Section 125 cafeteria plan**: W-2 Box 1 is lower than Box 18 because Box 1 excludes pre-tax benefits but local wages (Box 18) includes them. This is correct IRS treatment.
- **Statutory employee**: W-2 Box 13 checked "Statutory employee". Reported on Schedule C instead of wages. System must not flag "missing W-2 income" in this case.
- **Household employee**: W-2 from individual employer (SSN instead of EIN in employer section). System must handle SSN-based employers.
- **Railroad retirement**: Form RRB-1099-R instead of W-2. System must recognize alternative income forms.
- **Foreign earned income exclusion**: Form 2555 excludes up to $120,000 foreign wages from federal AGI, but these wages may be taxable locally. System must handle this reconciliation.
- **Alimony received (pre-2019 divorces)**: Taxable federally but not locally in most jurisdictions. System must not flag as missing income.
- **IRA distributions**: Form 1099-R distributions are included in federal AGI but typically not taxable locally. System must recognize non-taxable retirement income.

---

## Requirements *(mandatory)*

### Functional Requirements

#### W-2 Validation Rules

- **FR-001**: System MUST validate W-2 Box 18 (Local wages) is within 20% of Box 1 (Federal wages) for same employer, flagging if variance exceeds threshold with MEDIUM severity
- **FR-002**: System MUST validate W-2 Box 19 (Local tax withheld) divided by Box 18 (Local wages) is between 0% and 3.0%, flagging if implied rate exceeds 3.0% or is exactly 0% for high earners (>$25K)
- **FR-003**: System MUST detect duplicate W-2s by comparing Employer EIN + Employee SSN + Wage Amount, flagging exact matches as HIGH severity duplicates
- **FR-004**: System MUST validate W-2 employer address is within Dublin municipal limits OR flag as "Out-of-jurisdiction employer - verify local withholding applies"
- **FR-005**: System MUST allow user to mark W-2 as "Corrected" (replaces original) vs "Duplicate" (delete one copy) when duplicate is detected

#### Schedule C/E/F Validation Rules

- **FR-006**: System MUST calculate required estimated tax payments (90% of liability or 100% of prior year) and flag if Schedule C/E/F filer made insufficient estimated payments
- **FR-007**: System MUST validate Schedule E rental property count matches number of properties with complete address + income + expense data
- **FR-008**: System MUST validate Schedule E rental property addresses against Dublin municipal boundary database, flagging out-of-jurisdiction properties
- **FR-009**: System MUST check Schedule E passive loss limits: if rental loss + AGI > $150,000, flag that loss may be limited or suspended (informational severity)
- **FR-010**: System MUST validate Schedule C gross receipts match any uploaded 1099-K forms (payment processor income reporting)

#### K-1 Validation Rules

- **FR-011**: System MUST extract K-1 Box 1 (Ordinary income), Box 4c (Guaranteed payments), Box 12 (Section 179), and Box 13 (Other deductions) and validate all components are reported on Schedule X
- **FR-012**: System MUST validate partner's profit share percentage: if K-1 shows partner owns 25% and partnership Form 1065 shows $400,000 total income, K-1 should show $100,000 (±5% variance allowed)
- **FR-013**: System MUST flag K-1 income that may require municipal adjustment (e.g., Section 179 deduction, charitable contributions exceeding limit)

#### Municipal Credit Validation Rules

- **FR-014**: System MUST validate municipal credits (Schedule Y) do not exceed Dublin tax liability, capping credits at liability amount and flagging excess with HIGH severity
- **FR-015**: System MUST validate credit order of application: credit for taxes paid to other cities applied first, then withholding credits, then estimate credits
- **FR-016**: System MUST validate credit percentages: credit allowed is lesser of (a) taxes paid to other city OR (b) Dublin rate × income earned in other city

#### Federal Form Reconciliation Rules

- **FR-017**: System MUST compare Federal Form 1040 Line 11 (AGI) to sum of local taxable income sources (W-2 + Schedule C/E/F + K-1) and flag if difference exceeds $500 or 10%
- **FR-018**: System MUST identify federal AGI components that are non-taxable locally (interest, dividends, unemployment, Social Security, pensions) and explain reconciliation differences
- **FR-019**: System MUST validate Federal Form 1040 Line 1 (Wages) matches sum of all W-2 Box 1 amounts (±$100 tolerance for rounding)

#### Cross-Year Validation Rules

- **FR-020**: System MUST verify carryforward amounts (NOL, overpayment credits, suspended losses) against prior year returns stored in database
- **FR-021**: System MUST query prior year return for safe harbor calculation: 100% of prior year liability vs 90% of current year liability
- **FR-022**: System MUST track multi-year NOL utilization: if 2023 generated $10,000 NOL and 2024 applied $5,000, 2025 return should show $5,000 available (not $10,000)

#### Validation Reporting

- **FR-023**: System MUST assign severity levels to each discrepancy: HIGH (prevents filing), MEDIUM (warning, allows filing), LOW (informational note)
- **FR-024**: System MUST display all discrepancies in DiscrepancyView component with: Field name, Calculated value, Reported value, Difference amount, Severity, Explanation message
- **FR-025**: System MUST allow user to "Accept" informational discrepancies (e.g., passive loss warning after confirming real estate professional status)
- **FR-026**: System MUST generate validation report PDF showing all checks performed, pass/fail status, and user acceptance of warnings
- **FR-027**: System MUST block filing submission if any HIGH severity discrepancies remain unresolved
- **FR-028**: System MUST save discrepancy analysis with return submission for audit trail purposes

### Key Entities *(include if feature involves data)*

- **DiscrepancyReport** (EXPANDED from existing basic structure):
  - **hasDiscrepancies**: boolean - true if any issues found
  - **issues**: array of DiscrepancyIssue objects:
    - **issueId**: string - unique identifier for tracking
    - **ruleId**: string - which validation rule triggered (e.g., "FR-001-W2-BOX-VARIANCE")
    - **category**: string - "W-2" | "Schedule C" | "Schedule E" | "K-1" | "Municipal Credit" | "Federal Reconciliation" | "Carryforward"
    - **field**: string - field name (e.g., "W-2 Box 18 Local Wages")
    - **sourceValue**: number - calculated/expected value
    - **formValue**: number - value entered by user
    - **difference**: number - sourceValue - formValue
    - **differencePercent**: number - (difference / sourceValue) × 100
    - **severity**: string - "HIGH" | "MEDIUM" | "LOW"
    - **message**: string - user-friendly explanation
    - **recommendedAction**: string - what user should do (e.g., "Verify Box 18 entry" or "Attach Form 8582")
    - **isAccepted**: boolean - user acknowledged and accepted this discrepancy
    - **acceptanceNote**: string - user's explanation for accepting
    - **acceptedDate**: timestamp
  - **summary**: object
    - **totalIssues**: number
    - **highSeverityCount**: number
    - **mediumSeverityCount**: number
    - **lowSeverityCount**: number
    - **acceptedIssuesCount**: number
    - **blocksFiling**: boolean - true if any high severity unresolved
  - **validationDate**: timestamp
  - **validationRulesVersion**: string - version of validation rules applied (for audit trail)

## Success Criteria *(mandatory)*

- System detects 95% of W-2 data entry errors (Box 18/19 transposition, missing zeros, duplicate forms)
- Validation completes in under 3 seconds for returns with up to 10 income forms (W-2s, 1099s, Schedules)
- False positive rate under 10% (90% of flagged discrepancies are actual errors, not valid edge cases)
- Users can resolve/accept all discrepancies in under 5 minutes with clear guidance on each issue
- HIGH severity discrepancies successfully block filing 100% of the time until resolved
- Validation report exports to PDF with all 10+ rule checks clearly documented for auditor review

## Assumptions

- Prior year return data is available in database for carryforward validation (after Year 1 of system operation)
- Federal Form 1040 is uploaded as PDF and AI extraction provides AGI (Line 11) and Wages (Line 1)
- Dublin municipal boundary GIS data is available for address validation
- IRS passive loss thresholds ($150,000 AGI, $25,000 max loss) are current tax law
- Municipal credit rules follow standard "lesser of taxes paid OR home rate × income" formula
- W-2 Box 18/19 are consistently used by employers (some small employers may not complete Box 18)
- Duplicate detection threshold is exact match on EIN + SSN + Amount (no fuzzy matching needed)

## Dependencies

- IndividualTaxCalculator.java: Must be updated with expanded analyzeDiscrepancies() method implementing FR-001 through FR-022
- DiscrepancyView.tsx: Must support expanded DiscrepancyIssue structure with severity, acceptance, and recommended actions
- AI Extraction Service: Must extract Federal Form 1040 Line 1 and Line 11, K-1 boxes, and 1099-K amounts
- GIS Service or Municipal Boundary Database: For address validation (FR-008)
- Session Storage: Must persist prior year return data for cross-year validation (FR-020 through FR-022)

## Out of Scope

- Automatic correction of discrepancies - system flags issues but user must manually correct
- Integration with IRS e-file system to validate against federal return - local validation only
- AI-powered natural language explanations for complex discrepancies - messages are template-based
- Real-time validation during data entry - validation runs on-demand when user clicks "Review" button
- Soft vs hard stops - all HIGH severity issues are hard stops (no override allowed)
- Multi-municipality consolidated validation - each jurisdiction validated independently

## Open Questions

None - all validation rules defined based on IRS Publication 17, Form 1040 instructions, and municipal tax code requirements.
