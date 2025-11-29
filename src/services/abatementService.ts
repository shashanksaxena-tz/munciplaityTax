/**
 * Penalty Abatement API Service
 * 
 * API client for penalty abatement operations:
 * - POST /api/abatements
 * - GET /api/abatements/{id}
 * - PATCH /api/abatements/{id}/review
 * - PATCH /api/abatements/{id}/withdraw
 * - POST /api/abatements/{id}/documents
 * - GET /api/abatements/{id}/form-27pa
 */

import {
  PenaltyAbatement,
  AbatementRequest,
  AbatementReviewRequest,
  DocumentUploadRequest,
  FormUrlResponse,
} from '../types/abatement';

const BASE_URL = '/api/abatements';

export interface AbatementServiceError {
  message: string;
  code?: string;
  details?: unknown;
}

/**
 * Submit a penalty abatement request.
 */
export const submitAbatementRequest = async (
  request: AbatementRequest
): Promise<PenaltyAbatement> => {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error: AbatementServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to submit abatement request');
  }

  return response.json();
};

/**
 * Get abatement request by ID.
 */
export const getAbatement = async (abatementId: string): Promise<PenaltyAbatement> => {
  const response = await fetch(`${BASE_URL}/${abatementId}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: AbatementServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to fetch abatement');
  }

  return response.json();
};

/**
 * Get abatement requests by return ID.
 */
export const getAbatementsByReturnId = async (
  returnId: string
): Promise<PenaltyAbatement[]> => {
  const response = await fetch(`${BASE_URL}/return/${returnId}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: AbatementServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to fetch abatements');
  }

  return response.json();
};

/**
 * Get pending abatement requests for a tenant.
 */
export const getPendingAbatements = async (
  tenantId: string
): Promise<PenaltyAbatement[]> => {
  const response = await fetch(`${BASE_URL}/tenant/${tenantId}/pending`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: AbatementServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to fetch pending abatements');
  }

  return response.json();
};

/**
 * Review and approve/deny an abatement request.
 */
export const reviewAbatement = async (
  abatementId: string,
  request: AbatementReviewRequest
): Promise<PenaltyAbatement> => {
  const response = await fetch(`${BASE_URL}/${abatementId}/review`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error: AbatementServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to review abatement');
  }

  return response.json();
};

/**
 * Withdraw a pending abatement request.
 */
export const withdrawAbatement = async (
  abatementId: string
): Promise<PenaltyAbatement> => {
  const response = await fetch(`${BASE_URL}/${abatementId}/withdraw`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: AbatementServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to withdraw abatement');
  }

  return response.json();
};

/**
 * Upload supporting document for an abatement request.
 */
export const uploadDocument = async (
  abatementId: string,
  request: DocumentUploadRequest
): Promise<PenaltyAbatement> => {
  const response = await fetch(`${BASE_URL}/${abatementId}/documents`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error: AbatementServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to upload document');
  }

  return response.json();
};

/**
 * Generate Form 27-PA PDF.
 */
export const generateForm27PA = async (
  abatementId: string
): Promise<FormUrlResponse> => {
  const response = await fetch(`${BASE_URL}/${abatementId}/form-27pa`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const error: AbatementServiceError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));
    throw new Error(error.message || 'Failed to generate Form 27-PA');
  }

  return response.json();
};

const abatementService = {
  submitAbatementRequest,
  getAbatement,
  getAbatementsByReturnId,
  getPendingAbatements,
  reviewAbatement,
  withdrawAbatement,
  uploadDocument,
  generateForm27PA,
};

export default abatementService;
