/**
 * useScheduleXAutoCalc Hook (T046)
 * 
 * React hook for Schedule X auto-calculation helpers
 * Provides functions for common auto-calculations with loading states
 */

import { useState, useCallback } from 'react';
import autoCalculationService, { AutoCalcResponse } from '../services/autoCalculationService';

export interface UseScheduleXAutoCalcResult {
  calculating: boolean;
  error: Error | null;
  lastResult: AutoCalcResponse | null;
  calculateMeals: (federalMealsDeduction: number) => Promise<AutoCalcResponse>;
  calculate5Percent: (
    interestIncome: number,
    dividendIncome: number,
    capitalGains: number
  ) => Promise<AutoCalcResponse>;
  calculateCharitable: (
    businessId: string,
    taxYear: number,
    contributionsPaid: number,
    taxableIncomeBeforeContributions: number
  ) => Promise<AutoCalcResponse>;
  calculateRelatedParty: (paidAmount: number, fairMarketValue: number) => Promise<AutoCalcResponse>;
}

/**
 * Hook for Schedule X auto-calculations
 * 
 * @returns Auto-calculation functions and state
 * 
 * @example
 * const { calculateMeals, calculating, lastResult, error } = useScheduleXAutoCalc();
 * 
 * // Calculate meals add-back
 * const result = await calculateMeals(15000);
 * console.log(result.calculatedValue); // 30000
 * console.log(result.explanation); // "Federal allows 50%..."
 */
export const useScheduleXAutoCalc = (): UseScheduleXAutoCalcResult => {
  const [calculating, setCalculating] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [lastResult, setLastResult] = useState<AutoCalcResponse | null>(null);

  const calculate = useCallback(async (fn: () => Promise<AutoCalcResponse>): Promise<AutoCalcResponse> => {
    setCalculating(true);
    setError(null);

    try {
      const result = await fn();
      setLastResult(result);
      return result;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Auto-calculation failed');
      setError(error);
      throw error;
    } finally {
      setCalculating(false);
    }
  }, []);

  const calculateMeals = useCallback(
    (federalMealsDeduction: number) => {
      return calculate(() => autoCalculationService.calculateMealsAddBack(federalMealsDeduction));
    },
    [calculate]
  );

  const calculate5Percent = useCallback(
    (interestIncome: number, dividendIncome: number, capitalGains: number) => {
      return calculate(() =>
        autoCalculationService.calculate5PercentRule(interestIncome, dividendIncome, capitalGains)
      );
    },
    [calculate]
  );

  const calculateCharitable = useCallback(
    (
      businessId: string,
      taxYear: number,
      contributionsPaid: number,
      taxableIncomeBeforeContributions: number
    ) => {
      return calculate(() =>
        autoCalculationService.calculateCharitableContribution(
          businessId,
          taxYear,
          contributionsPaid,
          taxableIncomeBeforeContributions
        )
      );
    },
    [calculate]
  );

  const calculateRelatedParty = useCallback(
    (paidAmount: number, fairMarketValue: number) => {
      return calculate(() => autoCalculationService.calculateRelatedPartyExcess(paidAmount, fairMarketValue));
    },
    [calculate]
  );

  return {
    calculating,
    error,
    lastResult,
    calculateMeals,
    calculate5Percent,
    calculateCharitable,
    calculateRelatedParty,
  };
};

export default useScheduleXAutoCalc;
