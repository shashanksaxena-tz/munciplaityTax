# Enhanced Penalty & Interest Calculation

**Feature Name:** Comprehensive Tax Penalty & Interest Engine  
**Priority:** HIGH  
**Status:** Specification  
**Created:** 2025-11-27

---

## Overview

Implement detailed penalty and interest calculation system covering late filing penalties (5% per month, max 25%), late payment penalties (1% per month, max 25%), quarterly estimated tax underpayment penalties with safe harbor rules, compound quarterly interest calculation, and automated penalty abatement for reasonable cause. This feature is critical for accurate tax liability calculation and helps filers avoid surprises at payment time.

**Current State:** System has basic penalty calculation (flat percentage) but lacks monthly accrual, underpayment penalty logic, safe harbor rules, interest compounding, and penalty abatement workflows.

**Target Users:** Individual and business filers paying tax after deadline, filers with underpaid estimates, CPAs calculating quarterly estimates, tax managers planning payment strategies, auditors verifying penalty calculations.

---

## User Scenarios & Testing

### US-1: Late Filing Penalty (5% Per Month, Max 25%) (P1 - Critical)

**User Story:**  
As a business that filed my 2024 tax return 3 months late (due date April 15, filed July 15), I want the system to automatically calculate my late filing penalty as 15% of unpaid tax (5% × 3 months), so that I know exactly how much additional penalty I owe.

**Business Context:**  
Late filing penalty accrues at 5% of unpaid tax per month (or partial month), with maximum of 25% after 5 months. This penalty is separate from late payment penalty. If both penalties apply in same month, combined rate is capped at 5% that month (not 6%).

**Independent Test:**  
- Tax due: $10,000
- Due date: April 15, 2024
- Filed: July 15, 2024 (3 months late)
- Late filing penalty: $10,000 × 5% × 3 = $1,500
- If filed in October (6+ months late): Penalty capped at $10,000 × 25% = $2,500

**Acceptance Criteria:**
- GIVEN a tax return filed after due date
- WHEN calculating penalties
- THEN system MUST calculate months late: (File date - Due date) rounded up to full months
- AND system MUST calculate late filing penalty: (Unpaid tax) × 5% × (Months late)
- AND system MUST cap penalty at 25% of unpaid tax (5 months max)
- AND system MUST display breakdown: "Filed 3 months late → 5% × 3 = 15% penalty on $10,000 unpaid tax = $1,500"
- AND system MUST NOT charge late filing penalty if tax paid by due date (even if return filed late)

---

### US-2: Late Payment Penalty (1% Per Month, Max 25%) (P1 - Critical)

**User Story:**  
As a filer who submitted my return on time but didn't pay until 4 months later, I want the system to calculate my late payment penalty as 4% of unpaid tax (1% per month), so that I understand the cost of delaying payment.

**Business Context:**  
Late payment penalty accrues at 1% per month (or partial month) of unpaid tax, maximum 25% after 25 months. This runs independently from late filing penalty. If filer makes partial payments, penalty applies only to remaining unpaid balance.

**Independent Test:**  
- Tax due: $5,000
- Due date: April 15, 2024
- Return filed: April 15, 2024 (on time)
- Payment made: August 15, 2024 (4 months late)
- Late payment penalty: $5,000 × 1% × 4 = $200
- No late filing penalty (return was on time)

**Acceptance Criteria:**
- GIVEN tax paid after due date
- WHEN calculating penalties
- THEN system MUST calculate months late: (Payment date - Due date) rounded up to full months
- AND system MUST calculate late payment penalty: (Unpaid tax) × 1% × (Months late)
- AND system MUST cap penalty at 25% of unpaid tax (25 months max)
- AND system MUST display breakdown: "Paid 4 months late → 1% × 4 = 4% penalty on $5,000 = $200"
- AND system MUST adjust penalty if partial payments made (apply penalty only to remaining unpaid balance)

---

### US-3: Combined Late Filing & Late Payment Penalty Cap (P1 - Critical)

**User Story:**  
As a filer who filed 2 months late AND paid 2 months late, I want the system to correctly apply the combined penalty cap (maximum 5% per month, not 6%), so that I'm not overcharged when both penalties apply.

**Business Context:**  
When both late filing (5%/month) and late payment (1%/month) penalties apply in the same month, the combined rate is capped at 5% per month (not 6%). This prevents double-penalizing filers. After 5 months, late filing penalty maxes out at 25%, and only late payment penalty continues accruing.

**Independent Test:**  
- Tax due: $10,000
- Due date: April 15, 2024
- Filed: June 15, 2024 (2 months late)
- Paid: June 15, 2024 (2 months late)

Without cap: Late filing 10% + Late payment 2% = 12% = $1,200  
With cap: Combined penalty capped at 5% × 2 = 10% = $1,000

**Acceptance Criteria:**
- GIVEN both late filing and late payment penalties apply
- WHEN calculating penalties
- THEN system MUST apply combined cap: Max 5% per month (not 6%)
- AND system MUST calculate:
  - For months 1-5: Combined penalty = 5% per month (late filing absorbs both)
  - After month 5: Late filing maxed at 25%, late payment continues at 1%/month
- AND system MUST display breakdown showing cap applied: "Month 1: 5% (combined), Month 2: 5% (combined)"

---

### US-4: Quarterly Estimated Tax Underpayment Penalty with Safe Harbor (P1 - Critical)

**User Story:**  
As a self-employed individual who paid estimated taxes but still owe $1,000 at year-end, I want the system to calculate my underpayment penalty only if I failed to meet safe harbor rules (90% of current year tax OR 100% of prior year tax), so that I avoid penalty if I followed safe harbor guidelines.

**Business Context:**  
Estimated tax must be paid quarterly (April 15, June 15, Sept 15, Jan 15). If underpaid, penalty accrues based on shortfall in each quarter. Safe harbor rules:
- Safe Harbor 1: Paid 90% of current year tax → No penalty
- Safe Harbor 2: Paid 100% of prior year tax → No penalty (110% if AGI > $150K)

Penalty rate = current interest rate (typically 3-7% annually), applied quarterly.

**Independent Test:**  
- 2024 tax liability: $20,000
- 2023 tax liability: $15,000
- Estimated payments: Q1=$4K, Q2=$4K, Q3=$4K, Q4=$4K = $16K total
- Owe at filing: $4,000

Check safe harbor:
- 90% of current year: $20,000 × 90% = $18,000 (paid $16K → Failed)
- 100% of prior year: $15,000 (paid $16K → Passed ✓)

Since passed safe harbor #2, NO underpayment penalty.

**Acceptance Criteria:**
- GIVEN tax liability and estimated payments
- WHEN calculating underpayment penalty
- THEN system MUST check safe harbor rules:
  - Safe Harbor 1: Total payments ≥ 90% × Current year tax
  - Safe Harbor 2: Total payments ≥ 100% × Prior year tax (110% if AGI > $150K)
- AND if either safe harbor met: NO penalty
- AND if both safe harbors failed: Calculate penalty on quarterly shortfalls
- AND system MUST display safe harbor status: "✓ Met Safe Harbor 2 (100% prior year) → No underpayment penalty"

---

### US-5: Quarterly Estimated Tax Underpayment Penalty Calculation (P2 - High Value)

**User Story:**  
As a filer who underpaid estimated taxes (failed safe harbor), I want the system to calculate my underpayment penalty separately for each quarter based on when payments were due vs made, so that I understand which quarters had shortfalls and how much penalty accrued in each.

**Business Context:**  
Each quarter has required payment: 25% of annual tax (or annualized income method). Penalty accrues from quarter due date until payment made (or filing date, whichever earlier). Annual penalty rate (e.g., 5%) is applied quarterly.

**Independent Test:**  
- 2024 annual tax: $20,000 (required $5K per quarter)
- Q1 (due Apr 15): Paid $2K → Underpaid $3K → Penalty from Apr 15 - Apr 15 next year
- Q2 (due Jun 15): Paid $3K → Underpaid $2K → Penalty from Jun 15 - Apr 15 next year
- Q3 (due Sep 15): Paid $5K → Fully paid → No penalty
- Q4 (due Jan 15): Paid $6K → Overpaid $1K → Apply to Q1 shortfall

Penalty calculation (assume 5% annual rate = 1.25% per quarter):
- Q1 underpayment: $3K × 1.25% × 4 quarters = $150
- Q2 underpayment: $2K × 1.25% × 3 quarters (Jun-Apr) = $75
- Q4 overpayment reduces Q1 shortfall: $3K - $1K = $2K → Recalculate Q1
- Adjusted Q1: $2K × 1.25% × 4 = $100
- Total penalty: $100 (Q1) + $75 (Q2) = $175

**Acceptance Criteria:**
- GIVEN estimated tax underpayment (failed safe harbor)
- WHEN calculating underpayment penalty
- THEN system MUST calculate required payment per quarter:
  - Standard method: 25% of annual tax per quarter
  - Annualized income method: Based on income earned in each quarter (if uneven income)
- AND system MUST calculate underpayment per quarter: (Required) - (Paid)
- AND system MUST calculate penalty per quarter: (Underpayment) × (Quarterly rate) × (Number of quarters from due date to filing)
- AND system MUST apply overpayments from later quarters to earlier underpayments
- AND system MUST display quarterly breakdown showing required, paid, underpaid, and penalty for each quarter

---

### US-6: Interest on Unpaid Tax with Quarterly Compounding (P2 - High Value)

**User Story:**  
As a filer with $10,000 unpaid tax for 12 months, I want the system to calculate compound interest quarterly (not simple interest), so that I see the accurate interest charge including compounding effects.

**Business Context:**  
Interest on unpaid tax compounds quarterly at the federal short-term rate + 3% (typical range: 3-7% annually). Interest accrues daily but compounds quarterly. This differs from penalties which are simple percentages.

**Independent Test:**  
- Unpaid tax: $10,000
- Annual interest rate: 6%
- Period: 12 months (4 quarters)

Simple interest: $10,000 × 6% = $600  
Compound quarterly: $10,000 × (1 + 6%/4)^4 - $10,000 = $10,000 × 1.0614 - $10,000 = $614

**Acceptance Criteria:**
- GIVEN unpaid tax balance
- WHEN calculating interest
- THEN system MUST retrieve current interest rate from rule engine (updated quarterly by IRS)
- AND system MUST calculate daily interest: (Unpaid tax) × (Annual rate / 365) × (Days)
- AND system MUST compound interest quarterly: Add accrued interest to principal each quarter
- AND system MUST display breakdown:
  ```
  Q1 (Jan-Mar): $10,000 × 6%/4 = $150 → New balance: $10,150
  Q2 (Apr-Jun): $10,150 × 6%/4 = $152 → New balance: $10,302
  Q3 (Jul-Sep): $10,302 × 6%/4 = $155 → New balance: $10,457
  Q4 (Oct-Dec): $10,457 × 6%/4 = $157 → New balance: $10,614
  Total interest: $614
  ```

---

### US-7: Penalty Abatement for Reasonable Cause (P3 - Future Enhancement)

**User Story:**  
As a filer who filed late due to hospitalization (reasonable cause), I want to request penalty abatement by explaining the circumstances, attaching supporting documentation, and having the system generate Form 27-PA (Penalty Abatement Request), so that I can potentially have penalties waived.

**Business Context:**  
IRS and municipalities allow penalty abatement for "reasonable cause" such as:
- Death, serious illness, or incapacitation
- Natural disaster (fire, flood, hurricane)
- Unable to obtain records
- Erroneous advice from tax authority

First-time penalty abatement (FPA) also available for filers with clean 3-year history.

**Independent Test:**  
- Filer filed 2 months late due to COVID-19 hospitalization (March-May 2024)
- Late filing penalty: $10,000 × 10% = $1,000
- Filer submits Form 27-PA with hospital records
- Municipality reviews and grants abatement → Penalty reduced to $0

**Acceptance Criteria:**
- GIVEN penalties assessed on return
- WHEN user requests penalty abatement
- THEN system MUST display abatement request form with:
  - Penalty types and amounts
  - Reason dropdown: Death/illness, Disaster, Records unavailable, Erroneous advice, First-time penalty abatement, Other
  - Explanation text field (required)
  - Document upload (hospital records, death certificate, disaster declaration, etc.)
- AND system MUST generate Form 27-PA (Penalty Abatement Request) as PDF
- AND system MUST track abatement status: PENDING | APPROVED | DENIED
- AND if approved: System MUST adjust return to remove penalties and recalculate total due

---

## Functional Requirements

### Late Filing Penalty

**FR-001:** System MUST calculate months late for filing: (File date - Due date) in months, rounded up to next full month

**FR-002:** System MUST calculate late filing penalty: (Unpaid tax at due date) × 5% × (Months late)

**FR-003:** System MUST cap late filing penalty at 25% of unpaid tax (maximum 5 months)

**FR-004:** System MUST NOT apply late filing penalty if full tax paid by due date (even if return filed late)

**FR-005:** System MUST treat extension deadline as new due date for late filing penalty:
- Without extension: Due April 15
- With extension: Due October 15 (or municipality-specific date)
- Late filing penalty starts after extended due date

**FR-006:** System MUST calculate separate late filing penalties for federal, state, and local returns (each has own due dates)

### Late Payment Penalty

**FR-007:** System MUST calculate months late for payment: (Payment date - Due date) in months, rounded up

**FR-008:** System MUST calculate late payment penalty: (Unpaid tax) × 1% × (Months late)

**FR-009:** System MUST cap late payment penalty at 25% of unpaid tax (maximum 25 months)

**FR-010:** System MUST handle partial payments by recalculating penalty on remaining balance:
- Initial unpaid: $10,000
- Month 1: Paid $4,000 → Penalty on $10,000 = $100
- Month 2: Remaining $6,000 → Penalty on $6,000 = $60
- Total: $160 (not $200 which would be penalty on full $10K for 2 months)

**FR-011:** System MUST track payment application order:
- Apply to tax liability first
- Then to penalties
- Then to interest
- (IRS standard ordering)

### Combined Penalty Cap

**FR-012:** System MUST apply combined penalty cap when both late filing and late payment penalties apply in same month:
- Maximum combined rate: 5% per month (not 6%)
- Late filing penalty (5%) absorbs late payment penalty (1%)

**FR-013:** System MUST handle transition after 5 months:
- Months 1-5: Combined 5% per month (capped)
- Month 6+: Late filing capped at 25%, only late payment continues at 1%/month

**FR-014:** System MUST display combined penalty breakdown:
```
Month 1: 5% (filing + payment combined)
Month 2: 5% (filing + payment combined)
...
Month 5: 5% (filing + payment combined) → Filing penalty maxed at 25%
Month 6: 1% (payment only) → Filing penalty stays at 25%
```

### Estimated Tax Underpayment - Safe Harbor

**FR-015:** System MUST check estimated tax safe harbor rules:
- Safe Harbor 1: Total payments ≥ 90% of current year tax
- Safe Harbor 2: Total payments ≥ 100% of prior year tax (110% if AGI > $150K)

**FR-016:** System MUST retrieve prior year tax liability from database for safe harbor calculation

**FR-017:** System MUST determine AGI threshold for 110% rule:
- If current year AGI > $150K (individual) or $1M (business): Use 110% of prior year
- Otherwise: Use 100% of prior year

**FR-018:** System MUST display safe harbor status prominently:
- "✓ Safe Harbor Met: Paid 100% of prior year tax ($15,000) → No underpayment penalty"
- "✗ Safe Harbor Failed: Paid $16,000 but need $18,000 (90% current year) or $16,500 (110% prior year) → Underpayment penalty applies"

**FR-019:** System MUST skip underpayment penalty calculation if either safe harbor met

### Estimated Tax Underpayment - Penalty Calculation

**FR-020:** System MUST calculate required payment per quarter using standard method:
- Q1 (Apr 15): 25% of annual tax
- Q2 (Jun 15): 25% of annual tax
- Q3 (Sep 15): 25% of annual tax
- Q4 (Jan 15): 25% of annual tax

**FR-021:** System MUST support annualized income method (optional):
- Calculate income earned through each quarter
- Annualize (multiply by 12/months elapsed)
- Calculate tax on annualized income
- Required payment = (Annualized tax) × (Months elapsed / 12)
- Use if income uneven throughout year (e.g., seasonal business)

**FR-022:** System MUST calculate underpayment per quarter: (Required payment) - (Actual payment)

**FR-023:** System MUST calculate penalty per quarter:
- Underpayment amount × Quarterly interest rate × Number of quarters from due date to filing
- Quarterly rate = Annual rate / 4 (e.g., 5% annual = 1.25% per quarter)

**FR-024:** System MUST apply overpayments from later quarters to earlier underpayments:
- If Q4 overpaid by $1K and Q1 underpaid by $3K
- Reduce Q1 underpayment to $2K
- Recalculate Q1 penalty on $2K instead of $3K

**FR-025:** System MUST retrieve current underpayment penalty rate from rule engine (updated quarterly)

**FR-026:** System MUST display quarterly underpayment schedule:

| Quarter | Due Date | Required | Paid | Underpaid | Penalty Rate | Penalty |
|---------|----------|----------|------|-----------|--------------|---------|
| Q1      | Apr 15   | $5,000   | $2,000 | $3,000  | 5% × 4Q      | $150    |
| Q2      | Jun 15   | $5,000   | $3,000 | $2,000  | 5% × 3Q      | $75     |
| Q3      | Sep 15   | $5,000   | $5,000 | $0      | -            | $0      |
| Q4      | Jan 15   | $5,000   | $6,000 | -$1,000 | (Apply to Q1)| -$25    |
| **Total** |        | $20,000  | $16,000| $4,000  |              | **$200** |

### Interest Calculation

**FR-027:** System MUST retrieve current interest rate from rule engine:
- Federal short-term rate + 3% (typical)
- Updated quarterly by IRS
- Municipalities may adopt federal rate or set own rate

**FR-028:** System MUST calculate daily interest: (Unpaid tax) × (Annual rate / 365) × (Days unpaid)

**FR-029:** System MUST compound interest quarterly:
- End of Q1: Add accrued interest to principal
- End of Q2: Calculate interest on new principal (original + Q1 interest)
- Continue for all quarters until paid

**FR-030:** System MUST calculate interest on unpaid penalties and prior interest (full compounding)

**FR-031:** System MUST display interest calculation breakdown by quarter:
```
Beginning balance: $10,000
Q1 interest (Jan-Mar, 91 days): $10,000 × 6% × 91/365 = $150
Q1 ending balance: $10,150
Q2 interest (Apr-Jun, 91 days): $10,150 × 6% × 91/365 = $152
Q2 ending balance: $10,302
...
```

**FR-032:** System MUST calculate interest from due date to payment date (or current date if unpaid)

### Penalty Abatement

**FR-033:** System MUST display penalty abatement request option on return with assessed penalties

**FR-034:** System MUST provide penalty abatement form with fields:
- Penalty type: Late filing, Late payment, Underpayment
- Penalty amount (pre-filled)
- Reason for abatement (dropdown + text)
- Supporting documentation (file upload)

**FR-035:** System MUST support abatement reasons:
- Death in immediate family
- Serious illness or incapacitation of taxpayer or family member
- Natural disaster (fire, flood, earthquake, hurricane)
- Unable to obtain necessary records
- Erroneous written advice from tax authority
- First-time penalty abatement (FPA) - clean 3-year history
- Other reasonable cause

**FR-036:** System MUST validate first-time penalty abatement eligibility:
- No penalties in prior 3 years
- All returns filed and taxes paid (or on payment plan)
- Automatically approved if eligible (no explanation required)

**FR-037:** System MUST generate Form 27-PA (Penalty Abatement Request) as PDF with:
- Taxpayer information
- Return details (year, tax due, penalties assessed)
- Penalty abatement request (type, amount, reason)
- Supporting documentation references

**FR-038:** System MUST track abatement status:
- PENDING: Submitted, awaiting review
- APPROVED: Penalty removed
- PARTIAL: Some penalty abated, some upheld
- DENIED: Full penalty upheld
- WITHDRAWN: Taxpayer withdrew request

**FR-039:** System MUST adjust return when abatement approved:
- Remove abated penalties from total due
- Recalculate interest (if penalty removal changes payment timeline)
- Generate amended calculation showing original vs adjusted

### Payment Application & Tracking

**FR-040:** System MUST apply payments in standard order:
1. Tax liability (principal)
2. Penalties (late filing, then late payment, then underpayment)
3. Interest

**FR-041:** System MUST track multiple payments with dates:
- Payment 1: $5,000 on 5/15/2024
- Payment 2: $3,000 on 7/15/2024
- Payment 3: $2,000 on 9/15/2024

**FR-042:** System MUST recalculate penalties/interest after each payment:
- Payment reduces principal
- Subsequent penalties/interest calculated on reduced balance

**FR-043:** System MUST display payment history with running balance:

| Date  | Payment | Applied To | Balance |
|-------|---------|------------|---------|
| 4/15  | -       | Tax due    | $10,000 |
| 5/15  | $5,000  | Tax        | $5,000  |
| 5/31  | -       | Late filing penalty (1.5 mo) | $5,375 |
| 7/15  | $3,000  | Tax        | $2,375  |
| 9/15  | $2,500  | Tax + penalties | $0      |

### Validation & Audit Trail

**FR-044:** System MUST validate penalty calculations:
- Late filing penalty ≤ 25% of tax
- Late payment penalty ≤ 25% of tax
- Combined penalty in any month ≤ 5% of tax
- Underpayment penalty based on correct quarterly amounts

**FR-045:** System MUST create audit log for penalties:
- Penalty assessed (type, amount, date)
- Penalty calculation (inputs and formula)
- Payments applied (date, amount, allocation)
- Penalties abated (type, amount, reason, approval date)

**FR-046:** System MUST flag unusual penalties for review:
- Penalty > $10,000
- Penalty > 50% of tax liability (indicates data error)
- Abatement requested for same penalty type multiple years

**FR-047:** System MUST generate penalty summary report:
- Breakdown by penalty type (filing, payment, underpayment)
- Calculation methodology
- Safe harbor analysis (if applicable)
- Total penalties and interest
- Payment history
- Remaining balance due

---

## Key Entities

### Penalty

Represents a penalty assessed on a tax return.

**Attributes:**
- `penaltyId` (UUID): Unique identifier
- `returnId` (UUID): Foreign key to TaxReturn
- `penaltyType` (enum): LATE_FILING | LATE_PAYMENT | ESTIMATED_UNDERPAYMENT | OTHER
- `assessmentDate` (date): When penalty was calculated
- `taxDueDate` (date): Original due date
- `actualDate` (date): Actual filing or payment date
- `monthsLate` (number): Calculated months late (rounded up)
- `unpaidTaxAmount` (decimal): Tax balance subject to penalty
- `penaltyRate` (decimal): Rate per month (5% for filing, 1% for payment)
- `penaltyAmount` (decimal): Calculated penalty
- `maximumPenalty` (decimal): Cap on penalty (25% of tax)
- `isAbated` (boolean): Whether penalty was abated
- `abatementReason` (string): Reason for abatement
- `abatementDate` (date): When abatement approved
- `createdDate` (timestamp)

---

### EstimatedTaxPenalty

Represents quarterly estimated tax underpayment penalty.

**Attributes:**
- `estimatedPenaltyId` (UUID)
- `returnId` (UUID): Foreign key to TaxReturn
- `taxYear` (number)
- `annualTaxLiability` (decimal): Total tax for year
- `priorYearTaxLiability` (decimal): Prior year tax (for safe harbor)
- `agi` (decimal): Adjusted gross income (for 110% threshold)
- `safeHarbor1Met` (boolean): Paid 90% of current year
- `safeHarbor2Met` (boolean): Paid 100%/110% of prior year
- `calculationMethod` (enum): STANDARD | ANNUALIZED_INCOME
- `totalPenalty` (decimal): Sum of all quarterly penalties
- `quarters` (array): Array of QuarterlyUnderpayment objects

---

### QuarterlyUnderpayment

Represents underpayment for one quarter.

**Attributes:**
- `quarter` (enum): Q1 | Q2 | Q3 | Q4
- `dueDate` (date): Apr 15, Jun 15, Sep 15, Jan 15
- `requiredPayment` (decimal): 25% of annual tax (standard method)
- `actualPayment` (decimal): Amount paid
- `underpayment` (decimal): Required - Actual (if negative, it's overpayment)
- `quartersUnpaid` (number): Number of quarters from due date to filing
- `penaltyRate` (decimal): Annual rate / 4
- `penaltyAmount` (decimal): Underpayment × Rate × Quarters

---

### Interest

Represents interest on unpaid tax.

**Attributes:**
- `interestId` (UUID)
- `returnId` (UUID): Foreign key to TaxReturn
- `taxDueDate` (date): When tax was due
- `unpaidTaxAmount` (decimal): Original tax balance
- `annualInterestRate` (decimal): Retrieved from rule engine (e.g., 6%)
- `compoundingFrequency` (enum): QUARTERLY (standard)
- `startDate` (date): Interest starts accruing (usually due date)
- `endDate` (date): Interest stops accruing (payment date or current date)
- `totalDays` (number): Days interest accrued
- `totalInterest` (decimal): Sum of all interest
- `interestByQuarter` (array): Array of QuarterlyInterest objects

---

### QuarterlyInterest

Represents interest for one quarter.

**Attributes:**
- `quarter` (string): "Q1 2024", "Q2 2024", etc.
- `startDate` (date): First day of quarter
- `endDate` (date): Last day of quarter
- `days` (number): Days in quarter (90-92)
- `beginningBalance` (decimal): Principal at start of quarter
- `interestAccrued` (decimal): Balance × Rate × Days/365
- `endingBalance` (decimal): Beginning + Interest (compounded)

---

### PenaltyAbatement

Represents a request to abate penalties.

**Attributes:**
- `abatementId` (UUID)
- `returnId` (UUID): Foreign key to TaxReturn
- `penaltyId` (UUID): Foreign key to Penalty (if abating specific penalty)
- `requestDate` (date): When request submitted
- `abatementType` (enum): LATE_FILING | LATE_PAYMENT | ESTIMATED | ALL
- `requestedAmount` (decimal): Penalty amount to abate
- `reason` (enum): DEATH | ILLNESS | DISASTER | MISSING_RECORDS | ERRONEOUS_ADVICE | FIRST_TIME | OTHER
- `explanation` (text): User-provided narrative
- `supportingDocuments` (array): File references (hospital records, death cert, etc.)
- `status` (enum): PENDING | APPROVED | PARTIAL | DENIED | WITHDRAWN
- `reviewedBy` (UUID): Auditor/admin who reviewed
- `reviewDate` (date): When decision made
- `approvedAmount` (decimal): Amount actually abated (may differ from requested)
- `denialReason` (text): Explanation if denied
- `formGenerated` (string): Form 27-PA PDF path

---

### PaymentAllocation

Tracks how each payment is applied to tax, penalties, and interest.

**Attributes:**
- `allocationId` (UUID)
- `returnId` (UUID): Foreign key to TaxReturn
- `paymentDate` (date)
- `paymentAmount` (decimal): Total payment received
- `appliedToTax` (decimal): Amount applied to principal
- `appliedToPenalties` (decimal): Amount applied to penalties
- `appliedToInterest` (decimal): Amount applied to interest
- `remainingTaxBalance` (decimal): After this payment
- `remainingPenaltyBalance` (decimal): After this payment
- `remainingInterestBalance` (decimal): After this payment
- `allocationOrder` (enum): TAX_FIRST (standard IRS ordering)

---

## Success Criteria

- **Penalty Accuracy:** 100% of penalties calculated correctly (vs current ~60% - many edge cases missing)
- **Safe Harbor Compliance:** Zero underpayment penalties assessed when safe harbor met (automatic check prevents errors)
- **Interest Compounding:** Compound quarterly interest calculated accurately (vs current simple interest approximation)
- **Abatement Processing:** First-time penalty abatement requests auto-approved in <1 minute (vs manual review taking weeks)
- **Transparency:** Filers understand penalty calculations (vs current "black box" where penalties seem arbitrary)
- **Audit Defense:** Penalty and interest calculations pass audit 100% of time with full documentation

---

## Assumptions

- Ohio adopts federal interest rate (short-term rate + 3%, updated quarterly)
- Late filing penalty: 5% per month, max 25%
- Late payment penalty: 1% per month, max 25%
- Combined cap: 5% per month when both apply
- Estimated tax safe harbor: 90% current year OR 100% prior year (110% if high income)
- Interest compounds quarterly (not daily or monthly)
- Payment allocation order: Tax → Penalties → Interest (IRS standard)
- First-time penalty abatement available for filers with clean 3-year history

---

## Dependencies

- **Rule Engine (Spec 4):** Penalty rates, interest rates, safe harbor thresholds stored as configurable rules (updated quarterly)
- **NOL Carryforward Tracker (Spec 6):** Prior year tax liability needed for estimated tax safe harbor calculation
- **Business Form Library (Spec 8):** Form 27-PA (Penalty Abatement Request) PDF generation
- **Double-Entry Ledger (Spec 12):** Payment tracking and allocation to tax/penalties/interest accounts
- **Historical Return Data:** Prior year returns needed for safe harbor and first-time abatement eligibility

---

## Out of Scope

- **Criminal penalties:** Fraud, evasion, willful failure to file (refer to legal authorities)
- **State-specific penalties:** Focus on Ohio only; other states have different penalty structures
- **Offer in compromise:** Negotiating reduced tax liability based on inability to pay (complex administrative process)
- **Installment agreement:** Payment plans (covered in Spec 12: Ledger System)
- **Trust fund recovery penalty:** For unpaid employee withholding (specialized penalty)

---

## Edge Cases

1. **Extension filed but no payment:** Extension extends filing deadline but NOT payment deadline. Late payment penalty starts accruing from original due date (April 15), even though extension is valid until October 15. No late filing penalty until after October 15.

2. **Overpayment applied to next year:** Filer has $2K refund from 2023, elects to apply to 2024 estimated tax. Counts as Q1 2024 payment for safe harbor purposes.

3. **Partial payment on due date:** Paid $7K of $10K tax on April 15. No late filing penalty (return filed on time). Late payment penalty on remaining $3K only (not full $10K).

4. **Amended return increases tax:** Original return filed on time, but amended return filed 6 months later adds $5K tax. Late filing penalty on $5K starting from original due date (not amendment date).

5. **Quarterly payment made day before next quarter due:** Q1 payment made on June 14 (one day before Q2 due date June 15). Counts for Q1, not Q2. System must use payment date, not posting date.

6. **First-time abatement already used:** Filer requests FPA but already received FPA in 2020. System denies automatic FPA but allows request for reasonable cause instead.

7. **Safe harbor passes but still owe at filing:** Paid $16K estimated tax, which met 100% prior year safe harbor. But actually owe $20K at filing. No underpayment penalty (safe harbor protects), but still must pay $4K balance plus late payment penalty if not paid by due date.

8. **Interest on refund:** Filer overpaid and receives refund 8 months after filing. Municipality may owe interest on refund (same rate as interest on underpayment). System calculates refund interest using same quarterly compounding methodology.

9. **Penalty abatement applied retroactively:** Penalty assessed and paid in 2024. Abatement approved in 2025. System must generate refund of penalty paid, including interest on the overpayment.
