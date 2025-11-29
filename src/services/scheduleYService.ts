/**
 * Schedule Y API Service
 * Handles all API calls related to Schedule Y apportionment
 */

import type {
  ScheduleY,
  ScheduleYRequest,
  ApportionmentBreakdown,
  ApportionmentAuditLog
} from '../types/apportionment';

const API_BASE = '/api/schedule-y';

/**
 * Create a new Schedule Y filing
 */
export async function createScheduleY(request: ScheduleYRequest): Promise<ScheduleY> {
  const response = await fetch(API_BASE, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to create Schedule Y');
  }

  return response.json();
}

/**
 * Get Schedule Y by ID
 */
export async function getScheduleY(scheduleYId: string): Promise<ScheduleY> {
  const response = await fetch(`${API_BASE}/${scheduleYId}`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });

  if (!response.ok) {
    throw new Error('Failed to fetch Schedule Y');
  }

  return response.json();
}

/**
 * List Schedule Y filings with pagination
 */
export async function listScheduleY(params: {
  returnId?: string;
  taxYear?: number;
  page?: number;
  size?: number;
}): Promise<{ content: ScheduleY[]; totalElements: number; totalPages: number }> {
  const queryParams = new URLSearchParams();
  if (params.returnId) queryParams.append('returnId', params.returnId);
  if (params.taxYear) queryParams.append('taxYear', params.taxYear.toString());
  if (params.page !== undefined) queryParams.append('page', params.page.toString());
  if (params.size !== undefined) queryParams.append('size', params.size.toString());

  const response = await fetch(`${API_BASE}?${queryParams}`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });

  if (!response.ok) {
    throw new Error('Failed to list Schedule Y filings');
  }

  return response.json();
}

/**
 * Get apportionment breakdown for a Schedule Y
 */
export async function getApportionmentBreakdown(scheduleYId: string): Promise<ApportionmentBreakdown> {
  const response = await fetch(`${API_BASE}/${scheduleYId}/breakdown`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });

  if (!response.ok) {
    throw new Error('Failed to fetch apportionment breakdown');
  }

  return response.json();
}

/**
 * Get audit log for a Schedule Y
 */
export async function getAuditLog(scheduleYId: string): Promise<ApportionmentAuditLog[]> {
  const response = await fetch(`${API_BASE}/${scheduleYId}/audit-log`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });

  if (!response.ok) {
    throw new Error('Failed to fetch audit log');
  }

  return response.json();
}
