/**
 * usePenaltyCalculation Hook
 * 
 * React hook for penalty calculation management
 * Provides loading state, data, error handling, and calculation functionality
 */

import { useState, useEffect, useCallback } from 'react';
import {
  PenaltyCalculationRequest,
  PenaltyCalculationResponse,
  Penalty,
} from '../types/penalty';
import penaltyService from '../services/penaltyService';

export interface UsePenaltyCalculationOptions {
  returnId?: string;
  autoFetch?: boolean;
}

export interface UsePenaltyCalculationResult {
  penalties: Penalty[];
  loading: boolean;
  error: Error | null;
  calculate: (request: PenaltyCalculationRequest) => Promise<PenaltyCalculationResponse>;
  calculating: boolean;
  refetch: () => Promise<void>;
}

/**
 * Hook for managing penalty calculations
 * 
 * @param options Hook configuration
 * @returns Penalty data and calculation functions
 * 
 * @example
 * const { penalties, loading, calculate } = usePenaltyCalculation({
 *   returnId: '550e8400-e29b-41d4-a716-446655440000',
 *   autoFetch: true
 * });
 * 
 * // Calculate penalties
 * await calculate({
 *   returnId: '550e8400-e29b-41d4-a716-446655440000',
 *   taxDueDate: '2024-04-15',
 *   unpaidTaxAmount: 10000,
 *   penaltyType: 'LATE_PAYMENT'
 * });
 */
export const usePenaltyCalculation = ({
  returnId,
  autoFetch = false,
}: UsePenaltyCalculationOptions = {}): UsePenaltyCalculationResult => {
  const [penalties, setPenalties] = useState<Penalty[]>([]);
  const [loading, setLoading] = useState(false);
  const [calculating, setCalculating] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchPenalties = useCallback(async () => {
    if (!returnId) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const data = await penaltyService.getPenaltiesByReturnId(returnId);
      setPenalties(data);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to fetch penalties'));
    } finally {
      setLoading(false);
    }
  }, [returnId]);

  const calculate = useCallback(
    async (request: PenaltyCalculationRequest): Promise<PenaltyCalculationResponse> => {
      setCalculating(true);
      setError(null);

      try {
        const response = await penaltyService.calculatePenalties(request);
        
        // Refetch penalties after calculation
        if (returnId) {
          await fetchPenalties();
        }
        
        return response;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to calculate penalties');
        setError(error);
        throw error;
      } finally {
        setCalculating(false);
      }
    },
    [returnId, fetchPenalties]
  );

  useEffect(() => {
    if (autoFetch && returnId) {
      fetchPenalties();
    }
  }, [autoFetch, returnId, fetchPenalties]);

  return {
    penalties,
    loading,
    error,
    calculate,
    calculating,
    refetch: fetchPenalties,
  };
};

export default usePenaltyCalculation;
