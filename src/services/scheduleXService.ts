/**
 * Schedule X API Service (T043)
 * 
 * API client for Schedule X operations:
 * - GET /api/net-profits/{returnId}/schedule-x
 * - PUT /api/net-profits/{returnId}/schedule-x
 */

import { BusinessScheduleXDetails } from '../types/scheduleX';

const BASE_URL = '/api/net-profits';

export interface ScheduleXServiceError {
  message: string;
  code?: string;
  details?: unknown;
}

/**
 * Get Schedule X data for a return
 */
export const getScheduleX = async (returnId: string): Promise<BusinessScheduleXDetails> => {
  const response = await fetch(`${BASE_URL}/${returnId}/schedule-x`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: ScheduleXServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to fetch Schedule X');
  }

  return response.json();
};

/**
 * Update Schedule X data for a return
 */
export const updateScheduleX = async (
  returnId: string,
  scheduleX: BusinessScheduleXDetails
): Promise<BusinessScheduleXDetails> => {
  const response = await fetch(`${BASE_URL}/${returnId}/schedule-x`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(scheduleX),
  });

  if (!response.ok) {
    const error: ScheduleXServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to update Schedule X');
  }

  return response.json();
};

const scheduleXService = {
  getScheduleX,
  updateScheduleX,
};

export default scheduleXService;
