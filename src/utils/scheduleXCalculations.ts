/**
 * Schedule X Calculation Utilities (T012 - Research R3 Frontend Simple Calculations)
 * 
 * Provides client-side calculation helpers for simple Schedule X auto-calculations:
 * - Meals & Entertainment (50% → 100% add-back)
 * - 5% Rule (expenses on intangible income)
 * - Related-Party Excess (paid - FMV)
 * 
 * Complex calculations (charitable contribution 10% limit) are handled by backend API.
 */

/**
 * Calculate meals & entertainment add-back (Federal deduction × 2)
 * Federal allows 50% deduction, municipal allows 0%, so add back full expense
 * 
 * @param federalMealsDeduction Amount deducted on federal return (50% of total expense)
 * @returns Municipal add-back amount (100% of total expense)
 */
export function calculateMealsAddBack(federalMealsDeduction: number): number {
  if (!federalMealsDeduction || federalMealsDeduction <= 0) {
    return 0;
  }
  
  // Federal deducted 50%, so multiply by 2 to get full expense
  return federalMealsDeduction * 2;
}

/**
 * Calculate 5% Rule for expenses on intangible income (FR-012)
 * Municipal adds back 5% of non-taxable intangible income to account for related expenses
 * 
 * @param interestIncome Interest income amount
 * @param dividends Dividend income amount  
 * @param capitalGains Capital gains amount (optional)
 * @returns 5% of total intangible income
 */
export function calculate5PercentRule(
  interestIncome: number,
  dividends: number,
  capitalGains: number = 0
): number {
  const totalIntangibleIncome = 
    (interestIncome || 0) + 
    (dividends || 0) + 
    (capitalGains || 0);
  
  if (totalIntangibleIncome <= 0) {
    return 0;
  }
  
  return totalIntangibleIncome * 0.05;
}

/**
 * Calculate related-party excess (FR-006)
 * Add back payments to related parties exceeding fair market value
 * 
 * @param paidAmount Amount paid to related party
 * @param fairMarketValue Fair market value of goods/services received
 * @returns Excess amount to add back (0 if paid <= FMV)
 */
export function calculateRelatedPartyExcess(
  paidAmount: number,
  fairMarketValue: number
): number {
  if (!paidAmount || !fairMarketValue) {
    return 0;
  }
  
  const excess = paidAmount - fairMarketValue;
  return Math.max(0, excess); // Cannot be negative
}

/**
 * Calculate total add-backs from all 20 add-back fields (FR-028)
 * 
 * @param addBacks Object containing all 20 add-back fields
 * @returns Sum of all add-back fields
 */
export function calculateTotalAddBacks(addBacks: {
  depreciationAdjustment?: number;
  amortizationAdjustment?: number;
  incomeAndStateTaxes?: number;
  guaranteedPayments?: number;
  mealsAndEntertainment?: number;
  relatedPartyExcess?: number;
  penaltiesAndFines?: number;
  politicalContributions?: number;
  officerLifeInsurance?: number;
  capitalLossExcess?: number;
  federalTaxRefunds?: number;
  expensesOnIntangibleIncome?: number;
  section179Excess?: number;
  bonusDepreciation?: number;
  badDebtReserveIncrease?: number;
  charitableContributionExcess?: number;
  domesticProductionActivities?: number;
  stockCompensationAdjustment?: number;
  inventoryMethodChange?: number;
  otherAddBacks?: number;
}): number {
  return (
    (addBacks.depreciationAdjustment || 0) +
    (addBacks.amortizationAdjustment || 0) +
    (addBacks.incomeAndStateTaxes || 0) +
    (addBacks.guaranteedPayments || 0) +
    (addBacks.mealsAndEntertainment || 0) +
    (addBacks.relatedPartyExcess || 0) +
    (addBacks.penaltiesAndFines || 0) +
    (addBacks.politicalContributions || 0) +
    (addBacks.officerLifeInsurance || 0) +
    (addBacks.capitalLossExcess || 0) +
    (addBacks.federalTaxRefunds || 0) +
    (addBacks.expensesOnIntangibleIncome || 0) +
    (addBacks.section179Excess || 0) +
    (addBacks.bonusDepreciation || 0) +
    (addBacks.badDebtReserveIncrease || 0) +
    (addBacks.charitableContributionExcess || 0) +
    (addBacks.domesticProductionActivities || 0) +
    (addBacks.stockCompensationAdjustment || 0) +
    (addBacks.inventoryMethodChange || 0) +
    (addBacks.otherAddBacks || 0)
  );
}

/**
 * Calculate total deductions from all 7 deduction fields (FR-029)
 * 
 * @param deductions Object containing all 7 deduction fields
 * @returns Sum of all deduction fields
 */
export function calculateTotalDeductions(deductions: {
  interestIncome?: number;
  dividends?: number;
  capitalGains?: number;
  section179Recapture?: number;
  municipalBondInterest?: number;
  depletionDifference?: number;
  otherDeductions?: number;
}): number {
  return (
    (deductions.interestIncome || 0) +
    (deductions.dividends || 0) +
    (deductions.capitalGains || 0) +
    (deductions.section179Recapture || 0) +
    (deductions.municipalBondInterest || 0) +
    (deductions.depletionDifference || 0) +
    (deductions.otherDeductions || 0)
  );
}

/**
 * Calculate adjusted municipal income (FR-030)
 * Formula: Federal Taxable Income + Total Add-Backs - Total Deductions
 * 
 * @param fedTaxableIncome Federal taxable income from Form 1120/1065/1120-S
 * @param totalAddBacks Sum of all add-back fields
 * @param totalDeductions Sum of all deduction fields
 * @returns Adjusted municipal taxable income
 */
export function calculateAdjustedMunicipalIncome(
  fedTaxableIncome: number,
  totalAddBacks: number,
  totalDeductions: number
): number {
  return (fedTaxableIncome || 0) + (totalAddBacks || 0) - (totalDeductions || 0);
}

/**
 * Check if adjusted income varies from federal by more than threshold (FR-034)
 * 
 * @param fedTaxableIncome Federal taxable income
 * @param adjustedMunicipalIncome Adjusted municipal income
 * @param threshold Variance threshold (default 0.20 for 20%)
 * @returns Object with hasVariance flag and variance percentage
 */
export function checkVariance(
  fedTaxableIncome: number,
  adjustedMunicipalIncome: number,
  threshold: number = 0.20
): { hasVariance: boolean; variancePct: number } {
  if (!fedTaxableIncome || fedTaxableIncome === 0) {
    return { hasVariance: false, variancePct: 0 };
  }
  
  const variance = Math.abs(adjustedMunicipalIncome - fedTaxableIncome) / Math.abs(fedTaxableIncome);
  
  return {
    hasVariance: variance > threshold,
    variancePct: variance * 100
  };
}
