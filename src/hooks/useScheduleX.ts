/**
 * useScheduleX Hook (T045)
 * 
 * React hook for Schedule X data management
 * Provides loading state, data, error handling, and update functionality
 */

import { useState, useEffect, useCallback } from 'react';
import { BusinessScheduleXDetails } from '../types/scheduleX';
import scheduleXService from '../services/scheduleXService';

export interface UseScheduleXOptions {
  returnId: string;
  autoFetch?: boolean;
}

export interface UseScheduleXResult {
  data: BusinessScheduleXDetails | null;
  loading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
  update: (scheduleX: BusinessScheduleXDetails) => Promise<void>;
  updating: boolean;
}

/**
 * Hook for managing Schedule X data
 * 
 * @param options Hook configuration
 * @returns Schedule X data and management functions
 * 
 * @example
 * const { data, loading, error, update } = useScheduleX({
 *   returnId: '550e8400-e29b-41d4-a716-446655440000',
 *   autoFetch: true
 * });
 * 
 * // Update Schedule X
 * await update({
 *   ...data,
 *   addBacks: {
 *     ...data.addBacks,
 *     depreciationAdjustment: 50000
 *   }
 * });
 */
export const useScheduleX = ({ returnId, autoFetch = true }: UseScheduleXOptions): UseScheduleXResult => {
  const [data, setData] = useState<BusinessScheduleXDetails | null>(null);
  const [loading, setLoading] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchData = useCallback(async () => {
    if (!returnId) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const scheduleX = await scheduleXService.getScheduleX(returnId);
      setData(scheduleX);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to fetch Schedule X'));
    } finally {
      setLoading(false);
    }
  }, [returnId]);

  const update = useCallback(
    async (scheduleX: BusinessScheduleXDetails) => {
      if (!returnId) {
        throw new Error('Return ID is required');
      }

      setUpdating(true);
      setError(null);

      try {
        const updated = await scheduleXService.updateScheduleX(returnId, scheduleX);
        setData(updated);
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to update Schedule X');
        setError(error);
        throw error; // Re-throw for component error handling
      } finally {
        setUpdating(false);
      }
    },
    [returnId]
  );

  useEffect(() => {
    if (autoFetch) {
      fetchData();
    }
  }, [autoFetch, fetchData]);

  return {
    data,
    loading,
    error,
    refetch: fetchData,
    update,
    updating,
  };
};

export default useScheduleX;
