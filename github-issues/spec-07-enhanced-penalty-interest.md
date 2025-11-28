# Spec 7: Comprehensive Tax Penalty & Interest Engine

**Priority:** HIGH  
**Feature Branch:** `7-enhanced-penalty-interest`  
**Spec Document:** `specs/7-enhanced-penalty-interest/spec.md`

## Overview

Implement detailed penalty and interest calculation system covering late filing penalties (5% per month, max 25%), late payment penalties (1% per month, max 25%), quarterly estimated tax underpayment penalties with safe harbor rules, compound quarterly interest calculation, and automated penalty abatement for reasonable cause.

## Implementation Status

**Current:** Basic penalty calculation exists (~20%)  
**Required:** Full penalty engine with monthly accrual, underpayment logic, interest compounding

## Core Requirements (FR-001 to FR-055)

### Late Filing Penalty (FR-001 to FR-006)
- [ ] Calculate months late: (File date - Due date) rounded up to full months
- [ ] Calculate penalty: (Unpaid tax at due date) × 5% × (Months late)
- [ ] Cap penalty at 25% of unpaid tax (5 months max)
- [ ] Do NOT apply if full tax paid by due date (even if return filed late)
- [ ] Treat extension deadline as new due date
- [ ] Calculate separate penalties for federal, state, local returns

### Late Payment Penalty (FR-007 to FR-011)
- [ ] Calculate months late: (Payment date - Due date) rounded up
- [ ] Calculate penalty: (Unpaid tax) × 1% × (Months late)
- [ ] Cap penalty at 25% of unpaid tax (25 months max)
- [ ] Handle partial payments by recalculating on remaining balance
- [ ] Track payment application order: Tax → Penalties → Interest

### Combined Penalty Cap (FR-012 to FR-014)
- [ ] Apply combined cap: Max 5% per month when both penalties apply
- [ ] Months 1-5: Combined 5% per month (late filing absorbs both)
- [ ] After month 5: Late filing maxed at 25%, late payment continues at 1%/month
- [ ] Display breakdown showing cap applied

### Estimated Tax Underpayment - Safe Harbor (FR-015 to FR-019)
- [ ] Check safe harbor rules:
  - Safe Harbor 1: Paid ≥ 90% of current year tax
  - Safe Harbor 2: Paid ≥ 100% of prior year tax (110% if AGI > $150K)
- [ ] Retrieve prior year tax liability from database
- [ ] Determine AGI threshold for 110% rule ($150K individual, $1M business)
- [ ] Display safe harbor status prominently
- [ ] Skip underpayment penalty if either safe harbor met

### Estimated Tax Underpayment - Penalty Calculation (FR-020 to FR-026)
- [ ] Calculate required payment per quarter: 25% of annual tax (standard method)
- [ ] Support annualized income method (optional for uneven income)
- [ ] Calculate underpayment per quarter: (Required) - (Actual)
- [ ] Calculate penalty: (Underpayment) × (Quarterly rate) × (Quarters from due date to filing)
- [ ] Apply overpayments from later quarters to earlier underpayments
- [ ] Retrieve current underpayment penalty rate from rule engine
- [ ] Display quarterly underpayment schedule with breakdown

### Interest Calculation (FR-027 to FR-032)
- [ ] Retrieve current interest rate from rule engine (federal short-term + 3%)
- [ ] Calculate daily interest: (Unpaid tax) × (Annual rate / 365) × (Days)
- [ ] Compound interest quarterly: Add accrued interest to principal each quarter
- [ ] Calculate interest on unpaid penalties and prior interest (full compounding)
- [ ] Display interest calculation breakdown by quarter
- [ ] Calculate from due date to payment date (or current date if unpaid)

### Penalty Abatement (FR-033 to FR-039)
- [ ] Display penalty abatement request option on return
- [ ] Provide form with fields: Penalty type, Amount, Reason, Documentation
- [ ] Support abatement reasons: Death, Illness, Disaster, Missing Records, Erroneous Advice, First-Time, Other
- [ ] Validate first-time penalty abatement eligibility (no penalties in prior 3 years)
- [ ] Generate Form 27-PA (Penalty Abatement Request) PDF
- [ ] Track abatement status: PENDING | APPROVED | PARTIAL | DENIED | WITHDRAWN
- [ ] Adjust return when abatement approved (remove penalties, recalculate)

### Payment Application & Tracking (FR-040 to FR-043)
- [ ] Apply payments in standard order: Tax → Penalties → Interest
- [ ] Track multiple payments with dates
- [ ] Recalculate penalties/interest after each payment
- [ ] Display payment history with running balance

### Validation & Audit Trail (FR-044 to FR-047)
- [ ] Validate penalty calculations: All caps and limits enforced
- [ ] Create audit log for penalties: assessed, calculated, paid, abated
- [ ] Flag unusual penalties for review (>$10K, >50% of tax)
- [ ] Generate penalty summary report with breakdown

## User Stories (7 Priority P1-P3)

1. **US-1 (P1):** Late Filing Penalty (5% Per Month, Max 25%)
2. **US-2 (P1):** Late Payment Penalty (1% Per Month, Max 25%)
3. **US-3 (P1):** Combined Late Filing & Late Payment Penalty Cap
4. **US-4 (P1):** Quarterly Estimated Tax Underpayment Penalty with Safe Harbor
5. **US-5 (P2):** Quarterly Estimated Tax Underpayment Penalty Calculation
6. **US-6 (P2):** Interest on Unpaid Tax with Quarterly Compounding
7. **US-7 (P3):** Penalty Abatement for Reasonable Cause

## Key Entities

### Penalty
- penaltyId, returnId, penaltyType (LATE_FILING/LATE_PAYMENT/ESTIMATED_UNDERPAYMENT)
- assessmentDate, taxDueDate, actualDate, monthsLate
- unpaidTaxAmount, penaltyRate, penaltyAmount, maximumPenalty
- isAbated, abatementReason, abatementDate

### EstimatedTaxPenalty
- estimatedPenaltyId, returnId, taxYear, annualTaxLiability, priorYearTaxLiability
- agi, safeHarbor1Met, safeHarbor2Met
- calculationMethod (STANDARD/ANNUALIZED_INCOME), totalPenalty
- quarters[] (array of QuarterlyUnderpayment objects)

### QuarterlyUnderpayment
- quarter (Q1/Q2/Q3/Q4), dueDate, requiredPayment, actualPayment
- underpayment, quartersUnpaid, penaltyRate, penaltyAmount

### Interest
- interestId, returnId, taxDueDate, unpaidTaxAmount
- annualInterestRate, compoundingFrequency (QUARTERLY)
- startDate, endDate, totalDays, totalInterest
- interestByQuarter[] (array of QuarterlyInterest objects)

### QuarterlyInterest
- quarter, startDate, endDate, days
- beginningBalance, interestAccrued, endingBalance

### PenaltyAbatement
- abatementId, returnId, penaltyId, requestDate
- abatementType, requestedAmount, reason, explanation
- supportingDocuments[], status, reviewedBy, reviewDate
- approvedAmount, denialReason, formGenerated

### PaymentAllocation
- allocationId, returnId, paymentDate, paymentAmount
- appliedToTax, appliedToPenalties, appliedToInterest
- remainingTaxBalance, remainingPenaltyBalance, remainingInterestBalance
- allocationOrder (TAX_FIRST standard IRS ordering)

## Success Criteria

- 100% of penalties calculated correctly (vs current ~60%)
- Zero underpayment penalties assessed when safe harbor met
- Compound quarterly interest calculated accurately
- First-time penalty abatement requests auto-approved in <1 minute
- Filers understand penalty calculations (vs "black box")
- Penalty and interest calculations pass audit 100% of time

## Edge Cases Documented

- Extension filed but no payment
- Overpayment applied to next year
- Partial payment on due date
- Amended return increases tax
- Quarterly payment made day before next quarter
- First-time abatement already used
- Safe harbor passes but still owe at filing
- Interest on refund
- Penalty abatement applied retroactively

## Technical Implementation

### PenaltyService.java
- [ ] calculateLateFilingPenalty()
- [ ] calculateLatePaymentPenalty()
- [ ] calculateEstimatedTaxPenalty()
- [ ] checkSafeHarbor()
- [ ] applyPenaltyCaps()

### InterestService.java
- [ ] calculateDailyInterest()
- [ ] compoundInterestQuarterly()
- [ ] generateInterestSchedule()

### PenaltyController.java
- [ ] GET /api/penalty/{returnId}
- [ ] POST /api/penalty/abatement
- [ ] GET /api/penalty/safe-harbor/{returnId}

## Dependencies

- Rule Engine (Spec 4) - Penalty rates, interest rates, safe harbor thresholds
- NOL Tracker (Spec 6) - Prior year tax liability for safe harbor
- Form Library (Spec 8) - Form 27-PA PDF generation
- Double-Entry Ledger (Spec 12) - Payment tracking and allocation
- Historical Return Data - Prior year returns for safe harbor

## Related Specs

- Feeds into: Spec 12 (Ledger - penalties recorded as journal entries)
- Uses: Spec 4 (Rule Engine for rates updated quarterly)
- Integrates with: All filing specs (applies to any late/unpaid return)
