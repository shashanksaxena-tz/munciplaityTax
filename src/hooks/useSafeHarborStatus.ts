/**
 * useSafeHarborStatus Hook
 * 
 * React hook for safe harbor evaluation
 * Provides loading state, evaluation data, error handling, and evaluation functionality
 */

import { useState, useCallback } from 'react';
import { SafeHarborRequest, SafeHarborEvaluation } from '../types/penalty';
import estimatedTaxService from '../services/estimatedTaxService';

export interface UseSafeHarborStatusResult {
  evaluation: SafeHarborEvaluation | null;
  loading: boolean;
  error: Error | null;
  evaluate: (request: SafeHarborRequest) => Promise<SafeHarborEvaluation>;
}

/**
 * Hook for evaluating safe harbor status
 * 
 * @returns Safe harbor evaluation data and functions
 * 
 * @example
 * const { evaluation, loading, evaluate } = useSafeHarborStatus();
 * 
 * // Evaluate safe harbor
 * const result = await evaluate({
 *   taxYear: 2024,
 *   currentYearTaxLiability: 100000,
 *   totalPaidEstimated: 95000,
 *   agi: 200000,
 *   priorYearTaxLiability: 90000
 * });
 * 
 * if (result.anySafeHarborMet) {
 *   console.log('No penalty due!');
 * }
 */
export const useSafeHarborStatus = (): UseSafeHarborStatusResult => {
  const [evaluation, setEvaluation] = useState<SafeHarborEvaluation | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const evaluate = useCallback(
    async (request: SafeHarborRequest): Promise<SafeHarborEvaluation> => {
      setLoading(true);
      setError(null);

      try {
        const result = await estimatedTaxService.evaluateSafeHarbor(request);
        setEvaluation(result);
        return result;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to evaluate safe harbor');
        setError(error);
        throw error;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  return {
    evaluation,
    loading,
    error,
    evaluate,
  };
};

export default useSafeHarborStatus;
