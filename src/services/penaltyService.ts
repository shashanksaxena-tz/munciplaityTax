/**
 * Penalty API Service
 * 
 * API client for penalty calculation operations:
 * - POST /api/penalties/calculate
 * - GET /api/penalties/{id}
 * - GET /api/penalties/return/{returnId}
 */

import {
  PenaltyCalculationRequest,
  PenaltyCalculationResponse,
  Penalty,
} from '../types/penalty';

const BASE_URL = '/api/penalties';

export interface PenaltyServiceError {
  message: string;
  code?: string;
  details?: unknown;
}

/**
 * Calculate penalties for a tax return.
 */
export const calculatePenalties = async (
  request: PenaltyCalculationRequest
): Promise<PenaltyCalculationResponse> => {
  const response = await fetch(`${BASE_URL}/calculate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error: PenaltyServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to calculate penalties');
  }

  return response.json();
};

/**
 * Get penalty by ID.
 */
export const getPenalty = async (penaltyId: string): Promise<Penalty> => {
  const response = await fetch(`${BASE_URL}/${penaltyId}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: PenaltyServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to fetch penalty');
  }

  return response.json();
};

/**
 * Get penalties for a tax return.
 */
export const getPenaltiesByReturnId = async (returnId: string): Promise<Penalty[]> => {
  const response = await fetch(`${BASE_URL}/return/${returnId}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: PenaltyServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to fetch penalties');
  }

  return response.json();
};

const penaltyService = {
  calculatePenalties,
  getPenalty,
  getPenaltiesByReturnId,
};

export default penaltyService;
