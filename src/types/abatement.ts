/**
 * TypeScript types for penalty abatement.
 * 
 * Functional Requirements:
 * - FR-033 to FR-039: Penalty abatement workflow
 */

export enum AbatementStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  PARTIAL = 'PARTIAL',
  DENIED = 'DENIED',
  WITHDRAWN = 'WITHDRAWN'
}

export enum AbatementReason {
  DEATH = 'DEATH',
  ILLNESS = 'ILLNESS',
  DISASTER = 'DISASTER',
  ERRONEOUS_ADVICE = 'ERRONEOUS_ADVICE',
  FIRST_TIME = 'FIRST_TIME',
  REASONABLE_CAUSE = 'REASONABLE_CAUSE',
  ADMINISTRATIVE_ERROR = 'ADMINISTRATIVE_ERROR',
  OTHER = 'OTHER'
}

export enum AbatementType {
  LATE_FILING = 'LATE_FILING',
  LATE_PAYMENT = 'LATE_PAYMENT',
  ESTIMATED = 'ESTIMATED',
  ALL = 'ALL'
}

/**
 * Penalty abatement entity.
 */
export interface PenaltyAbatement {
  id: string;
  tenantId: string;
  returnId: string;
  penaltyId?: string;
  requestDate: string;
  abatementType: AbatementType;
  requestedAmount: number;
  reason: AbatementReason;
  explanation: string;
  status: AbatementStatus;
  reviewDate?: string;
  reviewedBy?: string;
  reviewNotes?: string;
  approvedAmount?: number;
  isFirstTimeAbatement: boolean;
  requestedBy: string;
  createdBy: string;
  createdAt: string;
}

/**
 * Abatement request payload.
 */
export interface AbatementRequest {
  tenantId?: string;
  returnId: string;
  penaltyId?: string;
  abatementType: AbatementType;
  requestedAmount: number;
  reason: AbatementReason;
  explanation: string;
  requestedBy?: string;
}

/**
 * Abatement review request.
 */
export interface AbatementReviewRequest {
  status: AbatementStatus;
  approvedAmount?: number;
  reviewNotes?: string;
  reviewedBy?: string;
}

/**
 * Document upload request.
 */
export interface DocumentUploadRequest {
  documentUrl: string;
  documentType: string;
}

/**
 * Form URL response.
 */
export interface FormUrlResponse {
  formUrl: string;
  formName: string;
}

/**
 * Abatement reason label mapping.
 */
export const AbatementReasonLabels: Record<AbatementReason, string> = {
  [AbatementReason.DEATH]: 'Death of taxpayer or immediate family',
  [AbatementReason.ILLNESS]: 'Serious illness or incapacitation',
  [AbatementReason.DISASTER]: 'Natural disaster or casualty',
  [AbatementReason.ERRONEOUS_ADVICE]: 'Erroneous advice from tax authority',
  [AbatementReason.FIRST_TIME]: 'First-time penalty abatement',
  [AbatementReason.REASONABLE_CAUSE]: 'Reasonable cause',
  [AbatementReason.ADMINISTRATIVE_ERROR]: 'Administrative error',
  [AbatementReason.OTHER]: 'Other'
};

/**
 * Abatement status label mapping.
 */
export const AbatementStatusLabels: Record<AbatementStatus, string> = {
  [AbatementStatus.PENDING]: 'Pending Review',
  [AbatementStatus.APPROVED]: 'Approved',
  [AbatementStatus.PARTIAL]: 'Partially Approved',
  [AbatementStatus.DENIED]: 'Denied',
  [AbatementStatus.WITHDRAWN]: 'Withdrawn'
};

/**
 * Abatement status colors for UI.
 */
export const AbatementStatusColors: Record<AbatementStatus, string> = {
  [AbatementStatus.PENDING]: 'yellow',
  [AbatementStatus.APPROVED]: 'green',
  [AbatementStatus.PARTIAL]: 'blue',
  [AbatementStatus.DENIED]: 'red',
  [AbatementStatus.WITHDRAWN]: 'gray'
};
