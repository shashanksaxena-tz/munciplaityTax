/**
 * Auto-Calculation API Service (T044)
 * 
 * API client for Schedule X auto-calculation helpers:
 * - POST /api/schedule-x/auto-calculate
 * 
 * Supports:
 * - Meals & Entertainment (50% → 100%)
 * - 5% Rule (intangible income expenses)
 * - Charitable Contribution 10% Limit
 * - Related-Party Excess
 */

const BASE_URL = '/api/schedule-x';

export interface AutoCalcRequest {
  field: string;
  inputs: Record<string, number | string>;
}

export interface AutoCalcResponse {
  calculatedValue: number;
  explanation: string;
  metadata?: Record<string, unknown>;
}

/**
 * Auto-calculate a Schedule X field value
 * 
 * @param request Field name and input values
 * @returns Calculated value with explanation
 * 
 * @example
 * // Meals & Entertainment (50% → 100%)
 * autoCalculate({
 *   field: 'mealsAndEntertainment',
 *   inputs: { federalMealsDeduction: 15000 }
 * })
 * // Returns: { calculatedValue: 30000, explanation: "Federal allows 50%..." }
 * 
 * @example
 * // 5% Rule
 * autoCalculate({
 *   field: 'expensesOnIntangibleIncome',
 *   inputs: { interestIncome: 20000, dividendIncome: 15000, capitalGains: 0 }
 * })
 * // Returns: { calculatedValue: 1750, explanation: "5% Rule: 5% × $35,000..." }
 */
export const autoCalculate = async (request: AutoCalcRequest): Promise<AutoCalcResponse> => {
  const response = await fetch(`${BASE_URL}/auto-calculate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Auto-calculation failed');
  }

  return response.json();
};

/**
 * Helper functions for common auto-calculations
 */

/**
 * Calculate meals & entertainment add-back (50% → 100%)
 */
export const calculateMealsAddBack = (federalMealsDeduction: number): Promise<AutoCalcResponse> => {
  return autoCalculate({
    field: 'mealsAndEntertainment',
    inputs: { federalMealsDeduction },
  });
};

/**
 * Calculate 5% Rule expense add-back
 */
export const calculate5PercentRule = (
  interestIncome: number,
  dividendIncome: number,
  capitalGains: number
): Promise<AutoCalcResponse> => {
  return autoCalculate({
    field: 'expensesOnIntangibleIncome',
    inputs: { interestIncome, dividendIncome, capitalGains },
  });
};

/**
 * Calculate charitable contribution 10% limit
 */
export const calculateCharitableContribution = (
  businessId: string,
  taxYear: number,
  contributionsPaid: number,
  taxableIncomeBeforeContributions: number
): Promise<AutoCalcResponse> => {
  return autoCalculate({
    field: 'charitableContributionExcess',
    inputs: { businessId, taxYear, contributionsPaid, taxableIncomeBeforeContributions },
  });
};

/**
 * Calculate related-party excess expenses
 */
export const calculateRelatedPartyExcess = (
  paidAmount: number,
  fairMarketValue: number
): Promise<AutoCalcResponse> => {
  return autoCalculate({
    field: 'relatedPartyExcess',
    inputs: { paidAmount, fairMarketValue },
  });
};

const autoCalculationService = {
  autoCalculate,
  calculateMealsAddBack,
  calculate5PercentRule,
  calculateCharitableContribution,
  calculateRelatedPartyExcess,
};

export default autoCalculationService;
