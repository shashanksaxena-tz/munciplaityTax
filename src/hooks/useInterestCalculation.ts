/**
 * useInterestCalculation Hook
 * 
 * React hook for interest calculation management
 * Provides loading state, data, error handling, and calculation functionality
 */

import { useState, useEffect, useCallback } from 'react';
import {
  InterestCalculationRequest,
  InterestCalculationResponse,
  Interest,
} from '../types/interest';
import interestService from '../services/interestService';

export interface UseInterestCalculationOptions {
  returnId?: string;
  autoFetch?: boolean;
}

export interface UseInterestCalculationResult {
  interests: Interest[];
  loading: boolean;
  error: Error | null;
  calculate: (request: InterestCalculationRequest) => Promise<InterestCalculationResponse>;
  calculating: boolean;
  refetch: () => Promise<void>;
}

/**
 * Hook for managing interest calculations
 * 
 * @param options Hook configuration
 * @returns Interest data and calculation functions
 * 
 * @example
 * const { interests, loading, calculate } = useInterestCalculation({
 *   returnId: '550e8400-e29b-41d4-a716-446655440000',
 *   autoFetch: true
 * });
 * 
 * // Calculate interest
 * await calculate({
 *   returnId: '550e8400-e29b-41d4-a716-446655440000',
 *   taxDueDate: '2024-04-15',
 *   unpaidTaxAmount: 10000,
 *   includeQuarterlyBreakdown: true
 * });
 */
export const useInterestCalculation = ({
  returnId,
  autoFetch = false,
}: UseInterestCalculationOptions = {}): UseInterestCalculationResult => {
  const [interests, setInterests] = useState<Interest[]>([]);
  const [loading, setLoading] = useState(false);
  const [calculating, setCalculating] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchInterests = useCallback(async () => {
    if (!returnId) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const data = await interestService.getInterestByReturnId(returnId);
      setInterests(data);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to fetch interest calculations'));
    } finally {
      setLoading(false);
    }
  }, [returnId]);

  const calculate = useCallback(
    async (request: InterestCalculationRequest): Promise<InterestCalculationResponse> => {
      setCalculating(true);
      setError(null);

      try {
        const response = await interestService.calculateInterest(request);
        
        // Refetch interests after calculation
        if (returnId) {
          await fetchInterests();
        }
        
        return response;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to calculate interest');
        setError(error);
        throw error;
      } finally {
        setCalculating(false);
      }
    },
    [returnId, fetchInterests]
  );

  useEffect(() => {
    if (autoFetch && returnId) {
      fetchInterests();
    }
  }, [autoFetch, returnId, fetchInterests]);

  return {
    interests,
    loading,
    error,
    calculate,
    calculating,
    refetch: fetchInterests,
  };
};

export default useInterestCalculation;
