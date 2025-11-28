/**
 * Constants for Schedule X field definitions, help text, and validation rules
 * 
 * Feature: Comprehensive Business Schedule X Reconciliation (25+ Fields)
 * Functional Requirements: FR-001 through FR-038
 */

import { EntityType } from '../types/scheduleX';

export interface ScheduleXFieldDef {
  label: string;
  helpText: string;
  example: string;
  applicableEntityTypes: EntityType[];
  autoCalcAvailable: boolean;
  autoCalcFormula?: string;
  validationRule?: string;
}

/**
 * Complete field definitions for all 27 Schedule X fields
 */
export const SCHEDULE_X_FIELDS = {
  addBacks: {
    depreciationAdjustment: {
      label: "Depreciation Adjustment (Book vs Tax)",
      helpText: "Add back if book depreciation is less than MACRS tax depreciation. Subtract (enter negative) if book depreciation exceeds MACRS. Common for businesses with accelerated tax depreciation (5-year MACRS) vs straight-line book depreciation.",
      example: "Book depreciation $80,000, MACRS depreciation $130,000 → Add-back ($80K - $130K) = -$50,000 (negative means book < tax)",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Must match Form 4562 (Depreciation Schedule) calculation"
    },
    amortizationAdjustment: {
      label: "Amortization Adjustment (Intangible Assets)",
      helpText: "Difference between book and tax amortization for intangible assets (goodwill, patents, trademarks). Similar to depreciation adjustment but for intangibles with 15-year amortization period.",
      example: "Book amortization $5,000, tax amortization $3,000 → Add-back $2,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Must reconcile to Section 197 intangibles schedule"
    },
    interestAndStateTaxes: {
      label: "Income & State Taxes (Add-Back)",
      helpText: "State, local, and foreign income taxes deducted on federal return. Municipal does not allow deduction for income-based taxes. Add back full amount deducted federally.",
      example: "Deducted $10,000 state income taxes on Form 1120 Line 17 → Add-back $10,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Must match Form 1120 Line 17 (Taxes and Licenses) - income tax portion only"
    },
    guaranteedPayments: {
      label: "Guaranteed Payments to Partners",
      helpText: "Form 1065 Line 10 guaranteed payments. Deductible for federal purposes but not allowed for municipal purposes. Partnerships only.",
      example: "Form 1065 shows $50,000 guaranteed payments to partners → Add-back $50,000",
      applicableEntityTypes: [EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Must match Form 1065 Line 10. Only applicable to partnerships."
    },
    mealsAndEntertainment: {
      label: "Meals & Entertainment (100% Add-Back)",
      helpText: "Federal allows 50% deduction for business meals. Municipal allows 0% deduction. Add back the full federal meals expense (not just the 50% that was deducted). Click auto-calc to calculate from federal meals deduction.",
      example: "Total meals expense $30,000. Federal deducted $15,000 (50%). Municipal add-back = $30,000 (100%).",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: true,
      autoCalcFormula: "federalMealsDeduction × 2 (to get back to 100%)",
      validationRule: "Cannot exceed total operating expenses. Should be ~2% of revenue for most businesses."
    },
    relatedPartyExcess: {
      label: "Related-Party Excess Expenses",
      helpText: "Payments to related parties (shareholders, family members, affiliated entities) above fair market value. Municipal disallows excess amounts. Common for rent, management fees, interest paid to shareholders.",
      example: "Paid $10,000 rent to shareholder-owned LLC, FMV is $7,500 → Add-back $2,500 excess",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: true,
      autoCalcFormula: "amountPaid - fairMarketValue",
      validationRule: "Requires documentation of fair market value (appraisal, market comparables)"
    },
    penaltiesAndFines: {
      label: "Penalties and Fines (Government)",
      helpText: "Penalties, fines, or sanctions imposed by government agencies (IRS, EPA, OSHA, etc.). Already non-deductible for federal, but some taxpayers erroneously deduct. Add back if deducted in error.",
      example: "EPA fine of $10,000 was incorrectly deducted → Add-back $10,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Should already be in federal taxable income (non-deductible federally)"
    },
    politicalContributions: {
      label: "Political Contributions",
      helpText: "Contributions to political campaigns, political action committees (PACs), or lobbying expenses. Non-deductible federally and municipally. Add back if deducted in error.",
      example: "Campaign contribution of $5,000 was incorrectly deducted → Add-back $5,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Should already be in federal taxable income (non-deductible federally)"
    },
    officerLifeInsurance: {
      label: "Officer Life Insurance Premiums",
      helpText: "Life insurance premiums paid by corporation on officers where corporation is beneficiary. Non-deductible federally and municipally. Add back if deducted in error.",
      example: "Paid $3,000 premiums on officer life insurance (corp is beneficiary) → Add-back $3,000 if deducted",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP],
      autoCalcAvailable: false,
      validationRule: "Only premiums where corp is beneficiary (not employee-owned policies)"
    },
    capitalLossExcess: {
      label: "Capital Losses Exceeding Capital Gains",
      helpText: "Capital losses that exceed capital gains in current year. Excess losses are not deductible against ordinary income for corporations (can carry back 3 years, forward 5 years). Add back excess losses.",
      example: "Capital gains $10,000, capital losses $15,000 → Add-back $5,000 excess loss",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: true,
      autoCalcFormula: "Math.max(0, capitalLosses - capitalGains)",
      validationRule: "Must reconcile to Form 1120 Schedule D (Capital Gains and Losses)"
    },
    federalTaxRefunds: {
      label: "Federal Income Tax Refunds",
      helpText: "Prior year federal income tax refunds that were included in current year income. Rare - only applies if state/local taxes were not previously deducted. Most refunds are not taxable income.",
      example: "Prior year federal refund of $1,000 was included in income → May require adjustment",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Verify federal refund treatment before adding back"
    },
    expensesOnIntangibleIncome: {
      label: "Expenses on Intangible Income (5% Rule)",
      helpText: "Municipal requires adding back 5% of intangible income (interest, dividends, capital gains) as presumed expenses incurred to earn that income. You can override with actual documented expenses if greater than 5%.",
      example: "Interest income $20,000 + Dividends $15,000 = $35,000 × 5% = $1,750 add-back",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: true,
      autoCalcFormula: "(interestIncome + dividends + capitalGains) × 0.05",
      validationRule: "Can override with actual expenses if > 5% (requires documentation)"
    },
    section179Excess: {
      label: "Section 179 Excess Depreciation",
      helpText: "Portion of Section 179 immediate expensing that exceeds municipal limits. Federal allows up to $1M immediate expensing (2024 limit). If municipal has lower cap (e.g., $500K), add back the excess.",
      example: "Federal Section 179: $1M, Municipal limit: $500K → Add-back $500K excess",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Compare federal Section 179 to municipal Section 179 limit"
    },
    bonusDepreciation: {
      label: "Bonus Depreciation (100% Federal)",
      helpText: "100% bonus depreciation allowed federally but not municipally. Federal allows immediate write-off of qualified property. Municipal follows MACRS standard depreciation. Add back full bonus amount.",
      example: "Equipment cost $200,000, took 100% federal bonus → Add-back $200,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Must reconcile to Form 4562 Part II bonus depreciation"
    },
    badDebtReserveIncrease: {
      label: "Bad Debt Reserve Increase",
      helpText: "If company uses reserve method for book purposes but direct write-off method for tax, add back the increase in bad debt reserve. Most taxpayers must use direct write-off method (reserve method only for certain financial institutions).",
      example: "Bad debt reserve increased by $8,000 during year → Add-back $8,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Only applies if using reserve method for books (rare)"
    },
    charitableContributionExcess: {
      label: "Charitable Contribution Excess (Over 10% Limit)",
      helpText: "Charitable contributions exceeding 10% of taxable income before contributions. Federal enforces 10% limit (excess carries forward 5 years). Municipal follows federal treatment. Only add back if federal erroneously deducted excess.",
      example: "Contributions $80K, taxable income $600K, 10% limit $60K → Federal error: add-back $20K excess",
      applicableEntityTypes: [EntityType.C_CORP],
      autoCalcAvailable: true,
      autoCalcFormula: "Math.max(0, contributionsPaid - (taxableIncomeBeforeContributions × 0.10))",
      validationRule: "Municipal follows federal 10% limit. Verify federal return correctly applied limit."
    },
    domesticProductionActivities: {
      label: "Domestic Production Activities Deduction (DPAD)",
      helpText: "Section 199 DPAD deduction taken federally but not allowed municipally. Pre-TCJA deduction (eliminated after 2017 for most taxpayers). Affects historical returns and some JEDD zones with special rules.",
      example: "Federal Form 1120 shows $25,000 DPAD (pre-2018) → Add-back $25,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Check if business is in JEDD zone (may have different rules)"
    },
    stockCompensationAdjustment: {
      label: "Stock-Based Compensation Adjustment",
      helpText: "Difference between book expense (ASC 718 fair value at grant) and tax deduction (intrinsic value at exercise). Add back if book expense exceeds tax deduction.",
      example: "Book expense $50,000, tax deduction $40,000 → Add-back $10,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP],
      autoCalcAvailable: false,
      validationRule: "Reconcile to ASC 718 disclosure in financial statements"
    },
    inventoryMethodChange: {
      label: "Inventory Method Change (Section 481(a))",
      helpText: "Section 481(a) adjustment for change in inventory accounting method (LIFO to FIFO, etc.). Adjustment spreads over 4 years for federal purposes. Municipal may have different treatment.",
      example: "LIFO to FIFO change resulted in $30,000 Section 481(a) adjustment → May require add-back",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Requires IRS Form 3115 (Application for Change in Accounting Method)"
    },
    otherAddBacks: {
      label: "Other Add-Backs (Specify in Description)",
      helpText: "Catch-all field for any add-backs not covered by specific fields. MUST provide description if amount > 0. Examples: foreign currency adjustments, unicap adjustments, related-party interest limitations.",
      example: "Foreign currency translation adjustment of $5,000 → Enter $5,000 and describe: 'Foreign currency translation adjustment per ASC 830'",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Description REQUIRED if otherAddBacks > 0. Description must be clear and specific."
    }
  },
  deductions: {
    interestIncome: {
      label: "Interest Income (Non-Taxable)",
      helpText: "Taxable interest income included in federal return but not subject to municipal tax. Includes interest from savings accounts, bonds, CDs, and other debt instruments.",
      example: "Business earned $5,000 interest from corporate bonds (included in Form 1120 Line 5) → Deduct $5,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Must not exceed total income reported on federal return"
    },
    dividends: {
      label: "Dividend Income (Non-Taxable)",
      helpText: "Qualified and ordinary dividends included in federal return but not subject to municipal tax. Includes dividends from stock investments and partnerships.",
      example: "Received $3,000 in dividends (included in Form 1120 Line 4) → Deduct $3,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Must not exceed total income reported on federal return"
    },
    capitalGains: {
      label: "Capital Gains (Non-Taxable)",
      helpText: "Net capital gains included in federal return but not subject to municipal tax. Gains from sale of stocks, bonds, real estate, and other capital assets.",
      example: "Sold stock for $2,000 gain (included in Form 1120 Line 8) → Deduct $2,000",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Must reconcile to Schedule D (Capital Gains and Losses). Only net gains (not gross proceeds)."
    },
    section179Recapture: {
      label: "Section 179 Recapture",
      helpText: "Recapture of Section 179 deduction if asset was sold or converted to personal use before end of recovery period. Federal adds recaptured amount back to income. Municipal may not require recapture.",
      example: "Recaptured $10,000 Section 179 on Form 4797 → May deduct $10,000 for municipal",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Must reconcile to Form 4797 (Sales of Business Property)"
    },
    municipalBondInterest: {
      label: "Municipal Bond Interest (Cross-Jurisdiction)",
      helpText: "Municipal bond interest that is tax-exempt federally but may be taxable at different municipal level (cross-jurisdiction). Example: Dublin business owns Columbus municipal bonds.",
      example: "Earned $1,500 interest from Columbus municipal bonds → May be taxable in Dublin",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Verify municipal bond issuer jurisdiction vs filing jurisdiction"
    },
    depletionDifference: {
      label: "Depletion Deduction Difference",
      helpText: "Difference between percentage depletion (oil/gas/mining) and cost depletion. Federal allows percentage depletion if greater than cost depletion. Municipal may only allow cost depletion.",
      example: "Percentage depletion $50,000, cost depletion $40,000 → Deduct $10,000 excess",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Must reconcile to Form 1120 Line 20 (Other Deductions) - depletion"
    },
    otherDeductions: {
      label: "Other Deductions (Specify in Description)",
      helpText: "Catch-all field for any deductions not covered by specific fields. MUST provide description if amount > 0. Examples: IRC §108 insolvency exclusion, cancellation of debt income adjustments.",
      example: "IRC §108 insolvency exclusion of $2,000 → Enter $2,000 and describe: 'IRC §108(a)(1)(B) insolvency exclusion'",
      applicableEntityTypes: [EntityType.C_CORP, EntityType.S_CORP, EntityType.PARTNERSHIP],
      autoCalcAvailable: false,
      validationRule: "Description REQUIRED if otherDeductions > 0. Description must be clear and specific."
    }
  }
} as const;

/**
 * Auto-calculation rules for frontend and backend
 */
export const AUTO_CALC_RULES = {
  mealsAndEntertainment: {
    formula: '(federalMealsDeduction) × 2',
    explanation: 'Federal allows 50% deduction for business meals ($federalMealsDeduction). Municipal allows 0% deduction. Add back full expense: $federalMealsDeduction × 2 = $result.',
    simpleCalculation: true  // Can be calculated in frontend
  },
  expensesOnIntangibleIncome: {
    formula: '(interestIncome + dividends + capitalGains) × 0.05',
    explanation: '5% Rule: Add back expenses incurred to earn non-taxable intangible income. Total intangible income: $interestIncome (interest) + $dividends (dividends) + $capitalGains (gains) = $total. Add-back: $total × 5% = $result.',
    simpleCalculation: true  // Can be calculated in frontend
  },
  relatedPartyExcess: {
    formula: 'amountPaid - fairMarketValue',
    explanation: 'Disallow payments to related parties above fair market value. Paid $amountPaid, FMV $fairMarketValue → Add-back $result excess.',
    simpleCalculation: true  // Can be calculated in frontend
  },
  capitalLossExcess: {
    formula: 'Math.max(0, capitalLosses - capitalGains)',
    explanation: 'Capital losses exceeding capital gains cannot offset ordinary income. Losses $capitalLosses, gains $capitalGains → Add-back $result excess loss.',
    simpleCalculation: true  // Can be calculated in frontend
  },
  charitableContributionExcess: {
    formula: 'Math.max(0, contributionsPaid - (taxableIncomeBeforeContributions × 0.10))',
    explanation: '10% limit on taxable income: $taxableIncomeBeforeContributions × 10% = $limit. Contributions paid: $contributionsPaid. Excess: $result (carries forward if > 0).',
    simpleCalculation: false  // Requires backend (DB query for prior year carryforward)
  }
} as const;

/**
 * Validation thresholds
 */
export const VALIDATION_THRESHOLDS = {
  varianceWarning: 0.20,  // FR-034 - Flag if >20% variance between federal and municipal
  mealsReasonableness: 0.05,  // Warning if meals > 5% of total expenses
  depreciationReasonableness: 0.50,  // Warning if depreciation adjustment > 50% of federal income
  officerCompensationReasonableness: 0.50  // Warning if officer compensation > 50% of net income
} as const;

/**
 * Field groups for UI organization (collapsible accordions)
 */
export const FIELD_GROUPS = {
  addBacks: {
    depreciation: ['depreciationAdjustment', 'amortizationAdjustment', 'section179Excess', 'bonusDepreciation'],
    income: ['interestAndStateTaxes', 'guaranteedPayments', 'federalTaxRefunds'],
    expenses: ['mealsAndEntertainment', 'relatedPartyExcess', 'penaltiesAndFines', 'politicalContributions', 'officerLifeInsurance'],
    accounting: ['badDebtReserveIncrease', 'inventoryMethodChange', 'stockCompensationAdjustment'],
    other: ['capitalLossExcess', 'expensesOnIntangibleIncome', 'charitableContributionExcess', 'domesticProductionActivities', 'otherAddBacks']
  },
  deductions: {
    intangibleIncome: ['interestIncome', 'dividends', 'capitalGains'],
    adjustments: ['section179Recapture', 'municipalBondInterest', 'depletionDifference'],
    other: ['otherDeductions']
  }
} as const;

/**
 * Field names mapped to user-friendly display names
 */
export const FIELD_NAMES: Record<string, string> = {
  depreciationAdjustment: 'Depreciation Adjustment',
  amortizationAdjustment: 'Amortization Adjustment',
  interestAndStateTaxes: 'Income & State Taxes',
  guaranteedPayments: 'Guaranteed Payments',
  mealsAndEntertainment: 'Meals & Entertainment',
  relatedPartyExcess: 'Related-Party Excess',
  penaltiesAndFines: 'Penalties and Fines',
  politicalContributions: 'Political Contributions',
  officerLifeInsurance: 'Officer Life Insurance',
  capitalLossExcess: 'Capital Loss Excess',
  federalTaxRefunds: 'Federal Tax Refunds',
  expensesOnIntangibleIncome: '5% Rule (Intangible Expenses)',
  section179Excess: 'Section 179 Excess',
  bonusDepreciation: 'Bonus Depreciation',
  badDebtReserveIncrease: 'Bad Debt Reserve',
  charitableContributionExcess: 'Charitable Contribution Excess',
  domesticProductionActivities: 'DPAD (Section 199)',
  stockCompensationAdjustment: 'Stock Compensation',
  inventoryMethodChange: 'Inventory Method Change',
  otherAddBacks: 'Other Add-Backs',
  interestIncome: 'Interest Income',
  dividends: 'Dividend Income',
  capitalGains: 'Capital Gains',
  section179Recapture: 'Section 179 Recapture',
  municipalBondInterest: 'Municipal Bond Interest',
  depletionDifference: 'Depletion Difference',
  otherDeductions: 'Other Deductions'
};
