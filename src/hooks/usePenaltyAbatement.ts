/**
 * usePenaltyAbatement Hook
 * 
 * React hook for penalty abatement management
 * Provides loading state, data, error handling, and submission/review functionality
 */

import { useState, useEffect, useCallback } from 'react';
import {
  PenaltyAbatement,
  AbatementRequest,
  AbatementReviewRequest,
  FormUrlResponse,
} from '../types/abatement';
import abatementService from '../services/abatementService';

export interface UsePenaltyAbatementOptions {
  returnId?: string;
  autoFetch?: boolean;
}

export interface UsePenaltyAbatementResult {
  abatements: PenaltyAbatement[];
  loading: boolean;
  error: Error | null;
  submit: (request: AbatementRequest) => Promise<PenaltyAbatement>;
  review: (abatementId: string, request: AbatementReviewRequest) => Promise<PenaltyAbatement>;
  withdraw: (abatementId: string) => Promise<PenaltyAbatement>;
  generateForm: (abatementId: string) => Promise<FormUrlResponse>;
  submitting: boolean;
  refetch: () => Promise<void>;
}

/**
 * Hook for managing penalty abatement requests
 * 
 * @param options Hook configuration
 * @returns Abatement data and management functions
 * 
 * @example
 * const { abatements, loading, submit, review } = usePenaltyAbatement({
 *   returnId: '550e8400-e29b-41d4-a716-446655440000',
 *   autoFetch: true
 * });
 * 
 * // Submit abatement request
 * await submit({
 *   returnId: '550e8400-e29b-41d4-a716-446655440000',
 *   abatementType: 'LATE_PAYMENT',
 *   requestedAmount: 500,
 *   reason: 'FIRST_TIME',
 *   explanation: 'First-time penalty, clean record for past 3 years'
 * });
 * 
 * // Review abatement
 * await review('abatement-id', {
 *   status: 'APPROVED',
 *   approvedAmount: 500,
 *   reviewNotes: 'Approved - eligible for first-time abatement'
 * });
 */
export const usePenaltyAbatement = ({
  returnId,
  autoFetch = false,
}: UsePenaltyAbatementOptions = {}): UsePenaltyAbatementResult => {
  const [abatements, setAbatements] = useState<PenaltyAbatement[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchAbatements = useCallback(async () => {
    if (!returnId) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const data = await abatementService.getAbatementsByReturnId(returnId);
      setAbatements(data);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to fetch abatements'));
    } finally {
      setLoading(false);
    }
  }, [returnId]);

  const submit = useCallback(
    async (request: AbatementRequest): Promise<PenaltyAbatement> => {
      setSubmitting(true);
      setError(null);

      try {
        const abatement = await abatementService.submitAbatementRequest(request);
        
        // Refetch abatements after submission
        if (returnId) {
          await fetchAbatements();
        }
        
        return abatement;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to submit abatement request');
        setError(error);
        throw error;
      } finally {
        setSubmitting(false);
      }
    },
    [returnId, fetchAbatements]
  );

  const review = useCallback(
    async (abatementId: string, request: AbatementReviewRequest): Promise<PenaltyAbatement> => {
      setSubmitting(true);
      setError(null);

      try {
        const abatement = await abatementService.reviewAbatement(abatementId, request);
        
        // Refetch abatements after review
        if (returnId) {
          await fetchAbatements();
        }
        
        return abatement;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to review abatement');
        setError(error);
        throw error;
      } finally {
        setSubmitting(false);
      }
    },
    [returnId, fetchAbatements]
  );

  const withdraw = useCallback(
    async (abatementId: string): Promise<PenaltyAbatement> => {
      setSubmitting(true);
      setError(null);

      try {
        const abatement = await abatementService.withdrawAbatement(abatementId);
        
        // Refetch abatements after withdrawal
        if (returnId) {
          await fetchAbatements();
        }
        
        return abatement;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to withdraw abatement');
        setError(error);
        throw error;
      } finally {
        setSubmitting(false);
      }
    },
    [returnId, fetchAbatements]
  );

  const generateForm = useCallback(
    async (abatementId: string): Promise<FormUrlResponse> => {
      setError(null);

      try {
        return await abatementService.generateForm27PA(abatementId);
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to generate Form 27-PA');
        setError(error);
        throw error;
      }
    },
    []
  );

  useEffect(() => {
    if (autoFetch && returnId) {
      fetchAbatements();
    }
  }, [autoFetch, returnId, fetchAbatements]);

  return {
    abatements,
    loading,
    error,
    submit,
    review,
    withdraw,
    generateForm,
    submitting,
    refetch: fetchAbatements,
  };
};

export default usePenaltyAbatement;
