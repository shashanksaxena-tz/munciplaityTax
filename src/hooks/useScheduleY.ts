/**
 * React hook for Schedule Y operations
 */

import { useState, useCallback } from 'react';
import type { ScheduleY, ScheduleYRequest, ApportionmentBreakdown } from '../types/apportionment';
import * as scheduleYService from '../services/scheduleYService';

export function useScheduleY() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [scheduleY, setScheduleY] = useState<ScheduleY | null>(null);
  const [breakdown, setBreakdown] = useState<ApportionmentBreakdown | null>(null);

  const createScheduleY = useCallback(async (request: ScheduleYRequest) => {
    setLoading(true);
    setError(null);
    try {
      const result = await scheduleYService.createScheduleY(request);
      setScheduleY(result);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to create Schedule Y';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const loadScheduleY = useCallback(async (scheduleYId: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await scheduleYService.getScheduleY(scheduleYId);
      setScheduleY(result);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load Schedule Y';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const loadBreakdown = useCallback(async (scheduleYId: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await scheduleYService.getApportionmentBreakdown(scheduleYId);
      setBreakdown(result);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load breakdown';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    error,
    scheduleY,
    breakdown,
    createScheduleY,
    loadScheduleY,
    loadBreakdown
  };
}
