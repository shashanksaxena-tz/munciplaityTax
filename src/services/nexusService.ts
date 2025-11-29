/**
 * Nexus API Service
 * Handles all API calls related to nexus tracking and management
 * Task: T082 [US2]
 */

import type { NexusStatus } from '../types/nexus';

const API_BASE = '/api/nexus';

/**
 * Get nexus status for a business across all states
 */
export async function getNexusStatus(businessId: string, tenantId: string): Promise<NexusStatus[]> {
  const response = await fetch(`${API_BASE}/${businessId}`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'X-Tenant-Id': tenantId
    }
  });

  if (!response.ok) {
    throw new Error('Failed to fetch nexus status');
  }

  return response.json();
}

/**
 * Get nexus status for a business in a specific state
 */
export async function getNexusStatusByState(
  businessId: string,
  state: string,
  tenantId: string
): Promise<NexusStatus> {
  const response = await fetch(`${API_BASE}/${businessId}/state/${state}`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'X-Tenant-Id': tenantId
    }
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch nexus status for state ${state}`);
  }

  return response.json();
}

/**
 * Update nexus status for a business in a state
 */
export async function updateNexusStatus(params: {
  businessId: string;
  state: string;
  hasNexus: boolean;
  reason?: string;
  tenantId: string;
}): Promise<NexusStatus> {
  const queryParams = new URLSearchParams({
    state: params.state,
    hasNexus: params.hasNexus.toString()
  });
  if (params.reason) {
    queryParams.append('reason', params.reason);
  }

  const response = await fetch(`${API_BASE}/${params.businessId}/update?${queryParams}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'X-Tenant-Id': params.tenantId
    }
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to update nexus status');
  }

  return response.json();
}

/**
 * Batch update nexus status for multiple states
 */
export async function batchUpdateNexusStatus(
  businessId: string,
  nexusStatuses: NexusStatus[],
  tenantId: string
): Promise<NexusStatus[]> {
  const response = await fetch(`${API_BASE}/${businessId}/batch-update`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'X-Tenant-Id': tenantId
    },
    body: JSON.stringify(nexusStatuses)
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to batch update nexus status');
  }

  return response.json();
}

/**
 * Determine and update economic nexus for a business in a state
 */
export async function determineEconomicNexus(params: {
  businessId: string;
  state: string;
  totalSales: number;
  transactionCount: number;
  tenantId: string;
}): Promise<NexusStatus> {
  const queryParams = new URLSearchParams({
    state: params.state,
    totalSales: params.totalSales.toString(),
    transactionCount: params.transactionCount.toString()
  });

  const response = await fetch(`${API_BASE}/${params.businessId}/economic-nexus?${queryParams}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'X-Tenant-Id': params.tenantId
    }
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to determine economic nexus');
  }

  return response.json();
}

/**
 * Get economic nexus thresholds for a state
 */
export async function getEconomicNexusThresholds(state: string): Promise<{
  sales_threshold: number;
  transaction_threshold: number;
  state: string;
}> {
  const response = await fetch(`${API_BASE}/thresholds/${state}`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch economic nexus thresholds for ${state}`);
  }

  return response.json();
}

/**
 * Get count of states where business has nexus
 */
export async function countNexusStates(businessId: string, tenantId: string): Promise<number> {
  const response = await fetch(`${API_BASE}/${businessId}/count`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'X-Tenant-Id': tenantId
    }
  });

  if (!response.ok) {
    throw new Error('Failed to count nexus states');
  }

  const data = await response.json();
  return data.nexusStateCount;
}

/**
 * Get all states where business has nexus
 */
export async function getNexusStates(businessId: string, tenantId: string): Promise<NexusStatus[]> {
  const response = await fetch(`${API_BASE}/${businessId}/nexus-states`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'X-Tenant-Id': tenantId
    }
  });

  if (!response.ok) {
    throw new Error('Failed to fetch nexus states');
  }

  return response.json();
}

/**
 * Get all states where business lacks nexus
 */
export async function getNonNexusStates(businessId: string, tenantId: string): Promise<NexusStatus[]> {
  const response = await fetch(`${API_BASE}/${businessId}/non-nexus-states`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'X-Tenant-Id': tenantId
    }
  });

  if (!response.ok) {
    throw new Error('Failed to fetch non-nexus states');
  }

  return response.json();
}
