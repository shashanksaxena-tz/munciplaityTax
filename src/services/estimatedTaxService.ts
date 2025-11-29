/**
 * Estimated Tax API Service
 * 
 * API client for estimated tax penalty and safe harbor operations:
 * - POST /api/estimated-tax/evaluate-safe-harbor
 * - POST /api/estimated-tax/calculate-penalty
 * - GET /api/estimated-tax/penalties/{id}
 */

import {
  SafeHarborRequest,
  SafeHarborEvaluation,
  EstimatedTaxPenaltyRequest,
  EstimatedTaxPenalty,
} from '../types/penalty';

const BASE_URL = '/api/estimated-tax';

export interface EstimatedTaxServiceError {
  message: string;
  code?: string;
  details?: unknown;
}

/**
 * Evaluate safe harbor rules for estimated tax.
 */
export const evaluateSafeHarbor = async (
  request: SafeHarborRequest
): Promise<SafeHarborEvaluation> => {
  const response = await fetch(`${BASE_URL}/evaluate-safe-harbor`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error: EstimatedTaxServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to evaluate safe harbor');
  }

  return response.json();
};

/**
 * Calculate estimated tax penalty.
 */
export const calculateEstimatedTaxPenalty = async (
  request: EstimatedTaxPenaltyRequest
): Promise<EstimatedTaxPenalty> => {
  const response = await fetch(`${BASE_URL}/calculate-penalty`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error: EstimatedTaxServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to calculate estimated tax penalty');
  }

  return response.json();
};

/**
 * Get estimated tax penalty by ID.
 */
export const getEstimatedTaxPenalty = async (
  penaltyId: string
): Promise<EstimatedTaxPenalty> => {
  const response = await fetch(`${BASE_URL}/penalties/${penaltyId}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: EstimatedTaxServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to fetch estimated tax penalty');
  }

  return response.json();
};

/**
 * Get estimated tax penalty by return ID.
 */
export const getEstimatedTaxPenaltyByReturnId = async (
  returnId: string
): Promise<EstimatedTaxPenalty> => {
  const response = await fetch(`${BASE_URL}/penalties/return/${returnId}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: EstimatedTaxServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to fetch estimated tax penalty');
  }

  return response.json();
};

const estimatedTaxService = {
  evaluateSafeHarbor,
  calculateEstimatedTaxPenalty,
  getEstimatedTaxPenalty,
  getEstimatedTaxPenaltyByReturnId,
};

export default estimatedTaxService;
