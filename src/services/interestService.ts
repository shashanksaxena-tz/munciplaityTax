/**
 * Interest Calculation API Service
 * 
 * API client for interest calculation operations:
 * - POST /api/interest/calculate
 * - GET /api/interest/{id}
 * - GET /api/interest/return/{returnId}
 */

import {
  InterestCalculationRequest,
  InterestCalculationResponse,
  Interest,
} from '../types/interest';

const BASE_URL = '/api/interest';

export interface InterestServiceError {
  message: string;
  code?: string;
  details?: unknown;
}

/**
 * Calculate interest on unpaid tax.
 */
export const calculateInterest = async (
  request: InterestCalculationRequest
): Promise<InterestCalculationResponse> => {
  const response = await fetch(`${BASE_URL}/calculate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error: InterestServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to calculate interest');
  }

  return response.json();
};

/**
 * Get interest calculation by ID.
 */
export const getInterest = async (interestId: string): Promise<Interest> => {
  const response = await fetch(`${BASE_URL}/${interestId}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: InterestServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to fetch interest');
  }

  return response.json();
};

/**
 * Get all interest calculations for a return.
 */
export const getInterestByReturnId = async (returnId: string): Promise<Interest[]> => {
  const response = await fetch(`${BASE_URL}/return/${returnId}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: InterestServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to fetch interest calculations');
  }

  return response.json();
};

const interestService = {
  calculateInterest,
  getInterest,
  getInterestByReturnId,
};

export default interestService;
