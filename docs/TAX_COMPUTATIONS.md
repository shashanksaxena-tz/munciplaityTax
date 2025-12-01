# MuniTax Tax Computation Documentation

## Overview

This document details the tax computation algorithms used in the MuniTax system for both individual and business tax calculations.

---

## Tax Computation Architecture

```mermaid
graph TB
    subgraph "Input"
        FORMS[Tax Forms]
        PROFILE[Taxpayer Profile]
        RULES[Tax Rules Config]
    end

    subgraph "Individual Tax Engine"
        W2_PROC[W-2 Processing]
        SCHED_X[Schedule X Processing]
        SCHED_Y[Schedule Y Credits]
        DISCR[Discrepancy Detection]
    end

    subgraph "Business Tax Engine"
        RECON[Schedule X Reconciliation]
        ALLOC[Schedule Y Allocation]
        NOL[NOL Application]
        PENALTY[Penalty & Interest]
    end

    subgraph "Output"
        RESULT[Tax Calculation Result]
        BREAK[Tax Breakdown]
        REPORT[Discrepancy Report]
    end

    FORMS --> W2_PROC
    FORMS --> SCHED_X
    RULES --> W2_PROC
    RULES --> RECON

    W2_PROC --> SCHED_Y
    SCHED_X --> SCHED_Y
    SCHED_Y --> DISCR

    FORMS --> RECON
    RECON --> ALLOC
    ALLOC --> NOL
    NOL --> PENALTY

    DISCR --> RESULT
    PENALTY --> RESULT
    RESULT --> BREAK
    RESULT --> REPORT
```

---

## Individual Tax Calculation

### Calculation Flow Diagram

```mermaid
flowchart TB
    START([Start]) --> W2[Process W-2 Forms]
    W2 --> QW[Determine Qualifying Wages]
    QW --> SCHEDX[Process Schedules C/E/F]
    SCHEDX --> OTHERI[Process 1099/W-2G Income]
    OTHERI --> GROSS[Calculate Gross Income]
    GROSS --> LIABILITY[Calculate Tax Liability]
    LIABILITY --> CREDIT[Calculate Schedule Y Credits]
    CREDIT --> WITHHELD[Apply Withholding]
    WITHHELD --> BALANCE[Calculate Balance Due/Refund]
    BALANCE --> DISCREP[Run Discrepancy Detection]
    DISCREP --> RESULT([Return Result])
```

### W-2 Processing

#### Qualifying Wages Rules

```mermaid
flowchart TD
    W2[W-2 Form] --> RULE{Qualifying<br/>Wages Rule}
    
    RULE -->|HIGHEST_OF_ALL| MAX[MAX(Box 1, Box 5, Box 18)]
    RULE -->|BOX_1_FEDERAL| B1[Federal Wages - Box 1]
    RULE -->|BOX_5_MEDICARE| B5[Medicare Wages - Box 5]
    RULE -->|BOX_18_LOCAL| B18[Local Wages - Box 18]
    
    MAX --> QW[Qualifying Wages]
    B1 --> QW
    B5 --> QW
    B18 --> QW
```

**Algorithm:**
```
For each W-2 form:
    box1 = federalWages ?? 0
    box5 = medicareWages ?? 0
    box18 = localWages ?? 0
    
    qualifyingWages = SWITCH(rule):
        HIGHEST_OF_ALL: MAX(box1, box5, box18)
        BOX_1_FEDERAL: box1
        BOX_5_MEDICARE: box5
        BOX_18_LOCAL: box18
    
    totalGrossIncome += box1
    totalLocalWithheld += localWithheld ?? 0
    w2TaxableIncome += qualifyingWages
```

### Schedule X Processing (Individual)

**Included Income Types:**
- Schedule C (Business Profit/Loss)
- Schedule E (Rental/Partnership Income)
- Schedule F (Farm Income)
- W-2G (Gambling Winnings)
- 1099-NEC/MISC (Non-Employee Compensation)

**Algorithm:**
```
totalNetProfit = 0

For each form:
    IF form is Schedule C AND rules.includeScheduleC:
        netProfit = form.netProfit ?? 0
        totalNetProfit += netProfit
        
    IF form is Schedule E AND rules.includeScheduleE:
        For each rental:
            netProfit += rental.income + rental.deductibleLoss
        For each partnership:
            netProfit += partnership.netProfit
        totalNetProfit += netProfit
        
    IF form is Schedule F AND rules.includeScheduleF:
        totalNetProfit += form.netFarmProfit ?? 0
        
    IF form is W-2G AND rules.includeW2G:
        totalNetProfit += form.grossWinnings ?? 0
        
    IF form is 1099 AND rules.include1099:
        totalNetProfit += form.incomeAmount ?? 0

taxableScheduleX = MAX(0, totalNetProfit)
```

### Schedule Y Credits

```mermaid
flowchart TB
    FORM[Form with Local Tax] --> CHECK{Locality is<br/>NOT Dublin?}
    CHECK -->|Yes| CALC[Calculate Credit]
    CHECK -->|No| SKIP[No Credit]
    
    CALC --> LIMIT[Apply Credit Limit]
    LIMIT --> TOTAL[Add to Total Credit]
```

**Algorithm:**
```
totalCredit = 0

For each form with localWithheld > 0:
    IF locality is NOT Dublin:
        incomeForCredit = form.localWages ?? form.grossWinnings ?? form.incomeAmount ?? 0
        creditLimit = incomeForCredit * rules.municipalCreditLimitRate
        creditAllowed = MIN(localWithheld, creditLimit)
        totalCredit += creditAllowed
```

### Final Calculation

```
totalTaxableIncome = w2TaxableIncome + taxableScheduleX
municipalLiability = totalTaxableIncome * rules.municipalRate
liabilityAfterCredits = MAX(0, municipalLiability - totalCredit)
balance = totalLocalWithheld - liabilityAfterCredits

IF rules.enableRounding:
    liabilityAfterCredits = ROUND(liabilityAfterCredits)
    balance = ROUND(balance)
```

### Individual Tax Formula Summary

```
┌─────────────────────────────────────────────────────────────────┐
│  INDIVIDUAL TAX CALCULATION                                     │
├─────────────────────────────────────────────────────────────────┤
│  W-2 Qualifying Wages (Rule-Based)              $XXX,XXX        │
│  + Schedule X Net Profit                        $XXX,XXX        │
│  ─────────────────────────────────────────────────────────────  │
│  = Total Taxable Income                         $XXX,XXX        │
│                                                                 │
│  × Municipal Tax Rate (2.5%)                    × 0.025         │
│  ─────────────────────────────────────────────────────────────  │
│  = Gross Municipal Liability                    $XX,XXX         │
│                                                                 │
│  − Schedule Y Credits                           $(X,XXX)        │
│  ─────────────────────────────────────────────────────────────  │
│  = Net Municipal Liability                      $XX,XXX         │
│                                                                 │
│  − Local Tax Withheld                           $(X,XXX)        │
│  ─────────────────────────────────────────────────────────────  │
│  = Balance Due / (Refund)                       $X,XXX          │
└─────────────────────────────────────────────────────────────────┘
```

---

## Business Tax Calculation

### Calculation Flow Diagram

```mermaid
flowchart TB
    START([Start]) --> SCHX[Process Schedule X<br/>Book-Tax Reconciliation]
    SCHX --> ADJ[Calculate Adjusted<br/>Federal Taxable Income]
    ADJ --> SCHY[Process Schedule Y<br/>Allocation Factors]
    SCHY --> ALLOC[Calculate<br/>Allocated Income]
    ALLOC --> NOL{NOL Available?}
    NOL -->|Yes| APPLY[Apply NOL<br/>Max 50% Offset]
    NOL -->|No| TAX[Calculate Tax Due]
    APPLY --> TAX
    TAX --> SAFE{Safe Harbor<br/>Met?}
    SAFE -->|No| PENALTY[Add Underpayment<br/>Penalty]
    SAFE -->|Yes| INTEREST[Calculate Interest]
    PENALTY --> INTEREST
    INTEREST --> BALANCE[Calculate Balance Due]
    BALANCE --> RESULT([Return Result])
```

### Schedule X (Book-Tax Reconciliation)

```mermaid
graph TB
    subgraph "Federal Taxable Income"
        FED[Federal Taxable Income]
    end

    subgraph "Add-Backs"
        ADD1[+ Interest & State Taxes Deducted]
        ADD2[+ Wages Credit]
        ADD3[+ Capital/1231 Losses]
        ADD4[+ Guaranteed Payments]
        ADD5[+ Intangible Expenses 5% Rule]
        ADD6[+ Other Add-Backs]
    end

    subgraph "Deductions"
        DED1[- Interest Income]
        DED2[- Dividends]
        DED3[- Capital/1231 Gains]
        DED4[- Section 179 Excess]
        DED5[- Other Deductions]
    end

    subgraph "Result"
        ADJ[= Adjusted Municipal Income]
    end

    FED --> ADD1 --> ADD2 --> ADD3 --> ADD4 --> ADD5 --> ADD6
    ADD6 --> DED1 --> DED2 --> DED3 --> DED4 --> DED5
    DED5 --> ADJ
```

**27-Field Schedule X Calculation:**
```
// Add-Backs
totalAddBacks = 
    interestAndStateTaxes +
    wagesCredit +
    capitalLosses +
    guaranteedPayments +
    intangibleExpenses +
    depreciation +
    mealsEntertainment +
    relatedPartyExpenses +
    politicalContributions +
    officerLifeInsurance +
    otherAddBacks

// Deductions
totalDeductions = 
    interestIncome +
    dividends +
    capitalGains +
    section179Excess +
    bonusDepreciation +
    badDebtReserves +
    charitableExcess +
    domesticProductionDeduction +
    stockBasedCompensation +
    inventoryValuationChanges +
    otherDeductions

adjustedFedIncome = fedTaxableIncome + totalAddBacks - totalDeductions
```

### Schedule Y (Allocation Formula)

```mermaid
graph LR
    subgraph "Property Factor"
        PD[Dublin Property]
        PE[Everywhere Property]
        PP[Property %]
        PD --> PP
        PE --> PP
    end

    subgraph "Payroll Factor"
        YD[Dublin Payroll]
        YE[Everywhere Payroll]
        YP[Payroll %]
        YD --> YP
        YE --> YP
    end

    subgraph "Sales Factor"
        SD[Dublin Sales]
        SE[Everywhere Sales]
        SP[Sales %]
        SD --> SP
        SE --> SP
    end

    subgraph "Weighted Average"
        PP --> AVG[Average %]
        YP --> AVG
        SP --> AVG
    end

    AVG --> ALLOC[Allocated Income]
```

**3-Factor Apportionment:**
```
propertyPct = safeDiv(property.dublin, property.everywhere)
payrollPct = safeDiv(payroll.dublin, payroll.everywhere)
salesPct = safeDiv(sales.dublin, sales.everywhere)

factorSum = 0
factorDivisor = 0

IF property.everywhere > 0:
    factorSum += propertyPct
    factorDivisor += 1

IF payroll.everywhere > 0:
    factorSum += payrollPct
    factorDivisor += 1

IF sales.everywhere > 0:
    factorSum += salesPct * salesFactorWeight  // Default 1.0, can be 2.0
    factorDivisor += salesFactorWeight

averagePct = safeDiv(factorSum, factorDivisor)
allocatedIncome = adjustedFedIncome * averagePct
```

### NOL (Net Operating Loss) Application

```mermaid
flowchart TB
    ALLOC[Allocated Income] --> CHECK{Income > 0?}
    CHECK -->|No| ZERO[Tax = 0<br/>Generate NOL]
    CHECK -->|Yes| NOL{NOL Available?}
    NOL -->|No| TAX[Calculate Tax]
    NOL -->|Yes| CAP[Apply 50% Cap]
    CAP --> APPLY[Apply NOL]
    APPLY --> TAX
```

**Algorithm:**
```
IF allocatedIncome <= 0:
    // Generate NOL for future use
    nolGenerated = ABS(allocatedIncome)
    taxableIncomeAfterNOL = 0
ELSE IF rules.enableNOL AND nolCarryforward > 0:
    nolLimit = allocatedIncome * rules.nolOffsetCapPercent  // 50%
    nolApplied = MIN(nolCarryforward, nolLimit, allocatedIncome)
    taxableIncomeAfterNOL = allocatedIncome - nolApplied
ELSE:
    taxableIncomeAfterNOL = allocatedIncome

taxDue = MAX(rules.minimumTax, taxableIncomeAfterNOL * rules.municipalRate)
```

### Penalty & Interest Calculation

```mermaid
flowchart TB
    TAX[Tax Due] --> SAFE{Total Payments >=<br/>90% of Tax Due?}
    SAFE -->|Yes| NO_PEN[No Underpayment<br/>Penalty]
    SAFE -->|No| PEN[Calculate<br/>Underpayment Penalty]
    
    PEN --> INT[Calculate Interest]
    NO_PEN --> INT
    INT --> BALANCE[Final Balance Due]
```

**Underpayment Penalty:**
```
totalPayments = estimatedPayments + priorYearCredit
requiredPayment = taxDue * rules.safeHarborPercent  // 90%

IF totalPayments < requiredPayment AND taxDue > 200:
    penaltyUnderpayment = (taxDue - totalPayments) * rules.penaltyRateUnderpayment  // 15%
ELSE:
    penaltyUnderpayment = 0
```

**Late Filing Penalty:**
```
IF filingDate > dueDate:
    monthsLate = CEILING((filingDate - dueDate) / 30)
    penaltyRate = MIN(monthsLate * 0.05, 0.25)  // 5% per month, max 25%
    penaltyLateFiling = taxDue * penaltyRate
    
    IF taxDue > 200 AND penaltyLateFiling < 50:
        penaltyLateFiling = 50  // Minimum penalty
```

**Interest:**
```
// Simple interest calculation (7% annual)
IF balance > 0 AND daysOverdue > 0:
    dailyRate = rules.interestRateAnnual / 365
    interest = balance * dailyRate * daysOverdue
```

### Business Tax Formula Summary

```
┌─────────────────────────────────────────────────────────────────┐
│  BUSINESS TAX CALCULATION                                       │
├─────────────────────────────────────────────────────────────────┤
│  SCHEDULE X - BOOK-TAX RECONCILIATION                           │
│  Federal Taxable Income (Line 28/30)            $XXX,XXX        │
│  + Add-Backs (Interest, Losses, etc.)           $XX,XXX         │
│  − Deductions (Interest Income, Gains, etc.)    $(XX,XXX)       │
│  ─────────────────────────────────────────────────────────────  │
│  = Adjusted Municipal Income                    $XXX,XXX        │
├─────────────────────────────────────────────────────────────────┤
│  SCHEDULE Y - ALLOCATION                                        │
│  Property Factor: Dublin / Everywhere           XX.XX%          │
│  Payroll Factor: Dublin / Everywhere            XX.XX%          │
│  Sales Factor: Dublin / Everywhere              XX.XX%          │
│  ─────────────────────────────────────────────────────────────  │
│  = Weighted Average Allocation %                XX.XX%          │
├─────────────────────────────────────────────────────────────────┤
│  TAX CALCULATION                                                │
│  Adjusted Municipal Income                      $XXX,XXX        │
│  × Allocation %                                 × XX.XX%        │
│  ─────────────────────────────────────────────────────────────  │
│  = Allocated Taxable Income                     $XXX,XXX        │
│                                                                 │
│  − NOL Carryforward Applied (max 50%)           $(XX,XXX)       │
│  ─────────────────────────────────────────────────────────────  │
│  = Taxable Income After NOL                     $XXX,XXX        │
│                                                                 │
│  × Municipal Tax Rate (2.5%)                    × 0.025         │
│  ─────────────────────────────────────────────────────────────  │
│  = Tax Due                                      $XX,XXX         │
├─────────────────────────────────────────────────────────────────┤
│  PENALTIES & INTEREST                                           │
│  + Underpayment Penalty                         $X,XXX          │
│  + Late Filing Penalty                          $X,XXX          │
│  + Interest                                     $XXX            │
│  − Estimated Payments                           $(X,XXX)        │
│  − Prior Year Credit                            $(X,XXX)        │
│  ─────────────────────────────────────────────────────────────  │
│  = BALANCE DUE                                  $XX,XXX         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Discrepancy Detection

### Validation Rules Matrix

```mermaid
flowchart LR
    subgraph "W-2 Validations"
        FR001[FR-001: Box 18 vs Box 1<br/>Threshold: 20%]
        FR002[FR-002: Withholding Rate<br/>Max: 3.0%]
        FR003[FR-003: Duplicate Detection<br/>Same EIN + Wages]
        FR004[FR-004: Locality Check<br/>Dublin Jurisdiction]
    end

    subgraph "Schedule Validations"
        FR006[FR-006: Schedule C<br/>Estimated Tax Warning]
        FR007[FR-007: Rental Property<br/>Data Completeness]
        FR009[FR-009: Passive Loss<br/>AGI > $150K]
    end

    subgraph "Credit Validations"
        FR014[FR-014: Credit Limit<br/>Credits ≤ Liability]
    end

    subgraph "Federal Reconciliation"
        FR017[FR-017: AGI vs Local<br/>Income Comparison]
        FR019[FR-019: Fed Wages vs W-2s<br/>Line 1 Match]
    end
```

### Discrepancy Severity Levels

| Severity | Threshold | Action |
|----------|-----------|--------|
| **HIGH** | Major issues | Blocks filing, requires resolution |
| **MEDIUM** | Needs review | Should review before filing |
| **LOW** | Informational | No action required |

### Example Discrepancy Detection

```java
// FR-001: W-2 Box 18 vs Box 1 Variance
if (box1 > 0 && box18 > 0) {
    double variance = Math.abs(box1 - box18);
    double variancePercent = (variance / box1) * 100;
    
    if (variancePercent > BOX_VARIANCE_THRESHOLD) {  // 20%
        issues.add(new DiscrepancyIssue(
            "FR-001",
            "W-2 Validation", 
            "W-2 Box 18 vs Box 1",
            box1,           // sourceValue
            box18,          // formValue
            box1 - box18,   // difference
            variancePercent,
            "MEDIUM",
            "W-2 Box 18 differs from Box 1 by " + variancePercent + "%",
            "Verify Box 18 was entered correctly"
        ));
    }
}
```

---

## Configurable Tax Parameters

### Individual Tax Configuration

```typescript
interface TaxRulesConfig {
  municipalRate: number;            // Default: 0.025 (2.5%)
  municipalCreditLimitRate: number; // Default: 0.025 (2.5%)
  municipalRates: Record<string, number>; // Per-city rates
  
  w2QualifyingWagesRule: 
    | 'HIGHEST_OF_ALL' 
    | 'BOX_5_MEDICARE' 
    | 'BOX_18_LOCAL' 
    | 'BOX_1_FEDERAL';
    
  incomeInclusion: {
    scheduleC: boolean;   // Default: true
    scheduleE: boolean;   // Default: true
    scheduleF: boolean;   // Default: true
    w2g: boolean;         // Default: true
    form1099: boolean;    // Default: true
  };
  
  enableRounding: boolean;  // Default: false
}
```

### Business Tax Configuration

```typescript
interface BusinessTaxRulesConfig {
  municipalRate: number;              // Default: 0.025 (2.5%)
  minimumTax: number;                 // Default: 0
  
  allocationMethod: '3_FACTOR' | 'GROSS_RECEIPTS_ONLY';
  allocationSalesFactorWeight: number; // Default: 1.0
  
  enableNOL: boolean;                 // Default: true
  nolOffsetCapPercent: number;        // Default: 0.50 (50%)
  
  intangibleExpenseRate: number;      // Default: 0.05 (5% rule)
  
  safeHarborPercent: number;          // Default: 0.90 (90%)
  penaltyRateLateFiling: number;      // Default: 0.05 (5% per month)
  penaltyRateUnderpayment: number;    // Default: 0.15 (15%)
  interestRateAnnual: number;         // Default: 0.07 (7%)
}
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-01 | Initial tax computation documentation |

---

**Document Owner:** Development Team  
**Last Updated:** December 1, 2025
