/**
 * React hook for nexus operations
 * Task: T083 [US2]
 */

import { useState, useCallback } from 'react';
import type { NexusStatus } from '../types/nexus';
import * as nexusService from '../services/nexusService';

export function useNexus() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [nexusStatuses, setNexusStatuses] = useState<NexusStatus[]>([]);
  const [nexusStateCount, setNexusStateCount] = useState<number>(0);

  /**
   * Load nexus status for a business across all states
   */
  const loadNexusStatus = useCallback(async (businessId: string, tenantId: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await nexusService.getNexusStatus(businessId, tenantId);
      setNexusStatuses(result);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load nexus status';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Load nexus status for a business in a specific state
   */
  const loadNexusStatusByState = useCallback(async (
    businessId: string,
    state: string,
    tenantId: string
  ) => {
    setLoading(true);
    setError(null);
    try {
      const result = await nexusService.getNexusStatusByState(businessId, state, tenantId);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load nexus status by state';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Update nexus status for a business in a state
   */
  const updateNexusStatus = useCallback(async (params: {
    businessId: string;
    state: string;
    hasNexus: boolean;
    reason?: string;
    tenantId: string;
  }) => {
    setLoading(true);
    setError(null);
    try {
      const result = await nexusService.updateNexusStatus(params);
      // Refresh the list after update
      await loadNexusStatus(params.businessId, params.tenantId);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to update nexus status';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [loadNexusStatus]);

  /**
   * Batch update nexus status for multiple states
   */
  const batchUpdateNexusStatus = useCallback(async (
    businessId: string,
    nexusUpdates: NexusStatus[],
    tenantId: string
  ) => {
    setLoading(true);
    setError(null);
    try {
      const result = await nexusService.batchUpdateNexusStatus(businessId, nexusUpdates, tenantId);
      setNexusStatuses(result);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to batch update nexus status';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Determine and update economic nexus for a business in a state
   */
  const determineEconomicNexus = useCallback(async (params: {
    businessId: string;
    state: string;
    totalSales: number;
    transactionCount: number;
    tenantId: string;
  }) => {
    setLoading(true);
    setError(null);
    try {
      const result = await nexusService.determineEconomicNexus(params);
      // Refresh the list after determination
      await loadNexusStatus(params.businessId, params.tenantId);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to determine economic nexus';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [loadNexusStatus]);

  /**
   * Get economic nexus thresholds for a state
   */
  const getEconomicNexusThresholds = useCallback(async (state: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await nexusService.getEconomicNexusThresholds(state);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to get economic nexus thresholds';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Load count of states where business has nexus
   */
  const loadNexusStateCount = useCallback(async (businessId: string, tenantId: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await nexusService.countNexusStates(businessId, tenantId);
      setNexusStateCount(result);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load nexus state count';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Load all states where business has nexus
   */
  const loadNexusStates = useCallback(async (businessId: string, tenantId: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await nexusService.getNexusStates(businessId, tenantId);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load nexus states';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Load all states where business lacks nexus
   */
  const loadNonNexusStates = useCallback(async (businessId: string, tenantId: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await nexusService.getNonNexusStates(businessId, tenantId);
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load non-nexus states';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    error,
    nexusStatuses,
    nexusStateCount,
    loadNexusStatus,
    loadNexusStatusByState,
    updateNexusStatus,
    batchUpdateNexusStatus,
    determineEconomicNexus,
    getEconomicNexusThresholds,
    loadNexusStateCount,
    loadNexusStates,
    loadNonNexusStates
  };
}
