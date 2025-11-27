# Feature Specification: Complete Withholding Reconciliation System

**Feature Branch**: `1-withholding-reconciliation`  
**Created**: 2025-11-27  
**Status**: Draft  
**Input**: Build comprehensive withholding reconciliation logic essential for business filers - W-1 to W-2/W-3 reconciliation engine with cumulative tracking, year-end validation, and discrepancy detection

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quarterly Filer Submits W-1 with Cumulative Validation (Priority: P1)

A business owner with quarterly filing frequency needs to submit their Q2 withholding return (Form W-1). The system should validate their Q2 wages against their cumulative Q1+Q2 totals and warn if they're significantly off-track for year-end W-2/W-3 reconciliation.

**Why this priority**: This is the core workflow - businesses file W-1 returns 4-12 times per year and need immediate validation to avoid year-end surprises.

**Independent Test**: Can be fully tested by submitting a Q2 W-1 return with wages of $50,000 when Q1 showed $45,000. System should display cumulative total of $95,000 and project annual withholding based on run rate.

**Acceptance Scenarios**:

1. **Given** business has filed Q1 W-1 with $45,000 wages and $900 tax, **When** they file Q2 W-1 with $50,000 wages, **Then** system displays cumulative totals: $95,000 wages YTD, $1,900 tax YTD, and projects $190,000 annual wages
2. **Given** business filed Q1 with $50,000 wages, **When** they file Q2 with $15,000 wages (70% drop), **Then** system shows WARNING: "Q2 wages are significantly lower than Q1 - verify payroll data"
3. **Given** business has 3 employees with estimated annual wages of $120,000, **When** Q1+Q2 cumulative shows $80,000 (67% of annual), **Then** system shows "On track - 67% of estimated annual wages through 50% of year"

---

### User Story 2 - Year-End W-2/W-3 Reconciliation with Discrepancy Detection (Priority: P1)

At year-end, business uploads their W-2s and W-3 summary. System reconciles total W-2 wages against cumulative W-1 filings for the year and flags discrepancies for correction.

**Why this priority**: This is the IRS/state compliance requirement - W-1 quarterly totals MUST match annual W-2/W-3 totals. Failing this reconciliation triggers audits.

**Independent Test**: Can be fully tested by uploading W-3 showing $200,000 total wages when W-1 filings showed $198,500 cumulative. System should flag $1,500 discrepancy and require explanation or amended W-1 filing.

**Acceptance Scenarios**:

1. **Given** business filed W-1 returns totaling $198,500 wages and $3,970 tax withheld, **When** they upload W-3 showing $200,000 wages and $4,000 tax, **Then** system displays DISCREPANCY: "$1,500 wage difference, $30 tax difference" with options to "File Amended W-1" or "Explain Discrepancy"
2. **Given** W-1 total ($200,000) matches W-3 total ($200,000) exactly, **When** year-end reconciliation runs, **Then** system displays "✓ Reconciled - W-1 and W-3 totals match" with green checkmark
3. **Given** discrepancy is explained as "Corrected W-2 for employee SSN error", **When** explanation is saved, **Then** system marks reconciliation as "Reviewed with Explanation" and allows filing to proceed

---

### User Story 3 - Monthly Filer with Mid-Year Correction (Priority: P2)

A business discovers in Month 5 that they under-reported Month 3 wages by $10,000. They need to file an amended W-1 for Month 3 and the system should automatically adjust all subsequent cumulative totals.

**Why this priority**: Amendments are common in payroll - employees get retroactive raises, bonuses are corrected, or reporting errors are discovered. System must handle cascading updates.

**Independent Test**: Can be tested by filing amended March W-1 with $10,000 additional wages. System should recalculate cumulative totals for April, May, and all subsequent months automatically.

**Acceptance Scenarios**:

1. **Given** business has filed W-1 for Jan-May with cumulative wages of $250,000, **When** they file amended March W-1 adding $10,000 wages, **Then** system recalculates cumulative totals: March becomes $60,000 (was $50,000), and May cumulative becomes $260,000 (was $250,000)
2. **Given** amended W-1 is filed, **When** system recalculates, **Then** additional tax due is calculated: $10,000 × 2.0% = $200 additional tax for March
3. **Given** amended return creates additional liability, **When** business views payment summary, **Then** system shows "Amended Return - Additional Tax Due: $200" with payment button

---

### User Story 4 - Daily Filer with High-Volume Reconciliation (Priority: P2)

A construction company with daily filing frequency (100+ W-1 filings per year) needs to reconcile their daily submissions efficiently at year-end without manually reviewing 260+ individual filings.

**Why this priority**: Daily filers have the highest compliance burden. They need automated reconciliation to avoid manual review of hundreds of filings.

**Independent Test**: Can be tested by simulating 252 daily W-1 filings (one per business day) totaling $5,000,000 wages, then uploading W-3 with matching total. System should reconcile automatically in under 5 seconds.

**Acceptance Scenarios**:

1. **Given** business has filed 252 daily W-1 returns for the year, **When** they initiate year-end reconciliation, **Then** system aggregates all 252 filings and displays cumulative total within 5 seconds
2. **Given** daily filings total $5,000,000 wages and W-3 shows $5,001,500, **When** reconciliation runs, **Then** system identifies which daily filings may have under-reported (flags days with unusually low wage amounts)
3. **Given** discrepancy is within safe harbor threshold (<0.1%), **When** reconciliation completes, **Then** system offers "Accept Minor Variance" option without requiring amended filings

---

### User Story 5 - Semi-Monthly Filer with Employee Count Validation (Priority: P3)

A business files W-1 semi-monthly (24 times/year) and reports average employee count on each filing. At year-end, system validates that reported employee counts align with number of W-2s issued.

**Why this priority**: Employee count tracking helps identify missing W-2s or reporting errors, but is secondary to wage/tax reconciliation.

**Independent Test**: Can be tested by filing W-1 returns reporting average of 15 employees, then uploading only 12 W-2s. System should flag "3 fewer W-2s than reported employee count - verify terminations were reported."

**Acceptance Scenarios**:

1. **Given** business reported average employee count of 15 across 24 W-1 filings, **When** they upload 12 W-2s at year-end, **Then** system shows WARNING: "Employee count mismatch - 15 average reported vs 12 W-2s issued"
2. **Given** employee count variance is explained as "3 terminations in Q4", **When** explanation includes termination dates, **Then** system validates dates align with reduced employee counts in later W-1 filings
3. **Given** employee count increased from 10 to 15 mid-year, **When** system validates, **Then** it verifies that new-hire W-2s show partial-year wages consistent with hire dates

---

### Edge Cases

- **Late W-1 filing**: What happens when business files Q2 W-1 after the due date? System should calculate late-filing penalty (5% per month, max 25%) and add to amount due.
- **Zero-wage period**: Business has no payroll in Q3 (seasonal business). System should still require $0 W-1 filing to maintain filing history and avoid "missing return" flags.
- **Negative adjustment**: Amended W-1 reduces wages (e.g., bonus was reversed). System should handle negative cumulative changes and generate refund/credit if tax was overpaid.
- **Multiple EIN numbers**: Business has subsidiaries with separate EINs. Each EIN should have independent W-1 tracking and reconciliation.
- **Partial-year business**: Business started operations in May. System should not flag "missing" Q1 filing if registration date is after Q1 end date.
- **Bankruptcy/dissolution**: Business closed mid-year. System should allow final W-1 filing and W-2 reconciliation for partial year without flagging missing future periods.
- **Household employer**: Individual with household employee (nanny, housekeeper) needs to file W-1 but may not have business EIN. System should support SSN-based filing for household employers.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST maintain complete W-1 filing history for each business profile (EIN), storing period, filing date, wages, adjustments, tax due, penalties, interest, and payment status for each filing
- **FR-002**: System MUST calculate cumulative year-to-date totals automatically whenever a new W-1 is filed, updating wages YTD, tax withheld YTD, and periods filed count
- **FR-003**: System MUST support amended W-1 filings that replace original filings and automatically recalculate all subsequent cumulative totals with cascade updates
- **FR-004**: System MUST validate W-1 wage amounts against filing frequency patterns (e.g., quarterly filer with wildly inconsistent quarter amounts triggers WARNING)
- **FR-005**: System MUST project annual totals based on current run rate and display "on track" or "behind pace" indicators for businesses that provided estimated annual wages during registration
- **FR-006**: System MUST perform year-end reconciliation by comparing cumulative W-1 totals against uploaded W-2/W-3 forms and flagging discrepancies exceeding $100 or 1% variance threshold
- **FR-007**: System MUST generate reconciliation report showing: (a) Total W-1 wages, (b) Total W-2 wages from uploaded forms, (c) Variance amount and percentage, (d) Recommended action (Accept/Amend/Explain)
- **FR-008**: System MUST allow businesses to attach explanation text to reconciliation discrepancies (e.g., "Corrected W-2 issued for employee bonus error")
- **FR-009**: System MUST track reconciliation status for each tax year: "Not Started", "In Progress", "Discrepancy Requires Resolution", "Reconciled", "Reconciled with Explanation"
- **FR-010**: System MUST prevent submission of next year's first W-1 filing if prior year reconciliation is incomplete (status = "Discrepancy Requires Resolution")
- **FR-011**: System MUST calculate late-filing penalties for W-1 returns filed after due date: 5% of tax due per month late, maximum 25%, minimum $50 if tax due > $200
- **FR-012**: System MUST calculate underpayment penalties if cumulative payments fall below 90% of current year liability or 100% of prior year liability (safe harbor rules)
- **FR-013**: System MUST support daily, semi-monthly, monthly, and quarterly filing frequencies with appropriate due date calculations for each frequency
- **FR-014**: System MUST aggregate W-2 data from AI extraction service, matching W-2 employer EIN to business profile and summing Box 18 (Local wages) and Box 19 (Local tax withheld)
- **FR-015**: System MUST display reconciliation dashboard showing: filing compliance (%periods filed), cumulative totals, reconciliation status, and action items for business owner
- **FR-016**: System MUST generate W-3 reconciliation form (equivalent to IRS Form W-3) summarizing all W-1 filings for the year and showing variance from W-2 totals
- **FR-017**: System MUST archive amended returns while preserving original filing history (both original and amended returns visible in history view)
- **FR-018**: System MUST validate employee count consistency: W-1 reported employee counts should align with number of W-2s issued at year-end (±20% tolerance)
- **FR-019**: System MUST support partial-year reconciliation for businesses that started/ceased operations mid-year, only requiring W-1 filings for active periods
- **FR-020**: System MUST integrate with payment gateway to track which W-1 periods are paid, partially paid, or unpaid, and calculate aging of unpaid liabilities

### Key Entities *(include if feature involves data)*

- **W1Filing**: Represents a single withholding return filing
  - Key attributes: Filing ID, Business Profile (EIN), Period (e.g., "2024-Q2"), Filing Date, Due Date, Gross Wages, Adjustments, Tax Due, Penalty, Interest, Total Amount Due, Payment Status, Is Amended (boolean), Amends Filing ID (reference to original if amended), Employee Count, Confirmation Number
  
- **WithholdingReconciliation**: Represents year-end reconciliation process
  - Key attributes: Reconciliation ID, Business Profile (EIN), Tax Year, W1 Total Wages (cumulative from all W-1 filings), W1 Total Tax (cumulative), W2 Total Wages (from uploaded W-2s), W2 Total Tax (from uploaded W-2s), Wage Variance (W1 - W2), Tax Variance, Variance Percentage, Status ("Not Started" | "In Progress" | "Discrepancy" | "Reconciled" | "Reconciled with Explanation"), Explanation Text, Resolution Date, Reviewed By (user/auditor)

- **CumulativeWithholdingTotals**: Running totals for current tax year
  - Key attributes: Business Profile (EIN), Tax Year, Periods Filed (count), Cumulative Wages YTD, Cumulative Tax YTD, Cumulative Adjustments YTD, Last Filing Date, Estimated Annual Wages (from business registration), Projected Annual Wages (based on run rate), On Track Indicator (boolean)

- **WithholdingPayment**: Tracks payments made against W-1 filings
  - Key attributes: Payment ID, W1 Filing ID, Payment Date, Amount, Payment Method, Confirmation Number, Applied To (Tax | Penalty | Interest), Remaining Balance After Payment

## Success Criteria *(mandatory)*

- Business owners can view cumulative withholding totals within 2 seconds of filing a W-1 return
- Year-end reconciliation process completes in under 10 seconds for businesses with up to 52 W-1 filings (weekly filers)
- 95% of businesses with <1% W-1/W-2 variance can complete reconciliation without filing amended returns
- System correctly calculates late-filing penalties matching IRS/municipal penalty schedule
- Amended W-1 filings automatically update all downstream cumulative calculations without manual intervention
- Businesses can drill down from reconciliation summary to individual W-1 filing details in 1 click
- Reconciliation report is downloadable as PDF for submission to auditors or tax professionals

## Assumptions

- W-2 data extraction via AI service is already functional (existing Gemini extraction service)
- Payment gateway integration exists for processing W-1 payments (existing PaymentGateway component)
- Business profile includes EIN, filing frequency, and registration date (existing BusinessProfile type)
- Tax rate for withholding is 2.0% of gross wages (existing MUNICIPAL_RATE constant)
- W-1 due dates follow municipal tax code: Monthly = 15th of following month, Quarterly = 30 days after quarter end, Daily = Next business day
- Safe harbor rules follow IRS guidelines: 90% of current year OR 100% of prior year, whichever is lower
- Penalty rates: Late filing = 5% per month (max 25%), Late payment = 1% per month, Underpayment = 15% annual
- W-3 form is a summary document (not separately filed by businesses) - system generates it from W-1 data
- Household employers file under SSN rather than EIN (separate profile type)

## Dependencies

- AI Extraction Service: Must extract W-2 Box 18 (local wages) and Box 19 (local tax) accurately
- Payment Service: Must provide payment tracking and ledger integration for W-1 liabilities
- Business Profile Management: Must maintain EIN, filing frequency, estimated annual wages
- PDF Generation Service: Must generate W-3 reconciliation forms and W-1 summaries
- Session Storage: Must persist W-1 filing history across sessions

## Out of Scope

- Integration with third-party payroll systems (ADP, Paychex) - manual data entry only in v1
- Electronic W-2 filing on behalf of businesses - businesses upload their own W-2 PDFs
- Multi-state withholding reconciliation - scope limited to single municipality (Dublin)
- Household employer quarterly filing automation (Federal Schedule H) - separate feature
- 1099-NEC contractor withholding - W-1 is for W-2 employees only
- Garnishment tracking (child support, tax levies) - not included in municipal withholding
- Union dues or other voluntary withholdings - only municipal tax withholding tracked
- Multi-currency support - all amounts in USD only

## Open Questions

None - specification is complete based on user input and municipal tax filing requirements.
