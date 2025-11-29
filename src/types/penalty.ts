/**
 * TypeScript types for penalty calculations.
 * 
 * Functional Requirements:
 * - FR-001 to FR-026: Penalty types and calculations
 * - Multi-tenant data structures
 */

export enum PenaltyType {
  LATE_FILING = 'LATE_FILING',
  LATE_PAYMENT = 'LATE_PAYMENT',
  ESTIMATED_TAX = 'ESTIMATED_TAX',
  COMBINED = 'COMBINED'
}

export enum Quarter {
  Q1 = 'Q1',
  Q2 = 'Q2',
  Q3 = 'Q3',
  Q4 = 'Q4'
}

export enum CalculationMethod {
  STANDARD = 'STANDARD',
  ANNUALIZED_INCOME = 'ANNUALIZED_INCOME'
}

/**
 * Base penalty interface.
 */
export interface Penalty {
  id: string;
  tenantId: string;
  returnId: string;
  penaltyType: PenaltyType;
  assessmentDate: string;
  taxDueDate: string;
  actualDate: string;
  monthsLate: number;
  unpaidTaxAmount: number;
  penaltyRate: number;
  penaltyAmount: number;
  maximumPenalty: number;
  isAbated: boolean;
  abatedAmount?: number;
  abatedDate?: string;
  createdBy: string;
  createdAt: string;
}

/**
 * Penalty calculation response.
 */
export interface PenaltyCalculationResponse {
  penaltyId: string;
  returnId: string;
  taxYearAndPeriod: string;
  dueDate: string;
  paymentDate: string;
  daysLate: number;
  taxDue: number;
  lateFilingPenalty?: number;
  lateFilingPenaltyRate?: number;
  lateFilingPenaltyExplanation?: string;
  latePaymentPenalty?: number;
  latePaymentPenaltyRate?: number;
  latePaymentPenaltyExplanation?: string;
  combinedCapApplied?: boolean;
  combinedCapExplanation?: string;
  totalPenalties: number;
  isAbated: boolean;
}

/**
 * Safe harbor evaluation result.
 */
export interface SafeHarborEvaluation {
  safeHarbor1Met: boolean;
  currentYearPaid: number;
  safeHarbor1Required: number;
  currentYearPercentage: number;
  safeHarbor2Met: boolean;
  priorYearPaid: number;
  safeHarbor2Required: number;
  priorYearPercentage: number;
  agi: number;
  agiThreshold: number;
  anySafeHarborMet: boolean;
  explanation: string;
}

/**
 * Quarterly underpayment details.
 */
export interface QuarterlyUnderpayment {
  id?: string;
  quarter: Quarter;
  dueDate: string;
  requiredPayment: number;
  actualPayment: number;
  underpaymentAmount: number;
  penaltyAmount: number;
  daysLate: number;
}

/**
 * Estimated tax penalty.
 */
export interface EstimatedTaxPenalty {
  id: string;
  tenantId: string;
  returnId: string;
  taxYear: number;
  annualTaxLiability: number;
  totalEstimatedTaxPaid: number;
  calculationMethod: CalculationMethod;
  safeHarbor1Met: boolean;
  safeHarbor2Met: boolean;
  penaltyRate: number;
  totalPenaltyAmount: number;
  quarterlyUnderpayments: QuarterlyUnderpayment[];
  createdBy: string;
  createdAt: string;
}

/**
 * Penalty calculation request.
 */
export interface PenaltyCalculationRequest {
  tenantId?: string;
  returnId: string;
  taxDueDate: string;
  actualDate?: string;
  unpaidTaxAmount: number;
  penaltyType?: string;
  createdBy?: string;
  checkExisting?: boolean;
}

/**
 * Estimated tax penalty request.
 */
export interface EstimatedTaxPenaltyRequest {
  tenantId?: string;
  returnId: string;
  taxYear: number;
  annualTaxLiability: number;
  quarterlyPayments: Record<Quarter, number>;
  agi: number;
  priorYearTaxLiability?: number;
  calculationMethod?: CalculationMethod;
  createdBy?: string;
}

/**
 * Safe harbor evaluation request.
 */
export interface SafeHarborRequest {
  tenantId?: string;
  taxYear: number;
  currentYearTaxLiability: number;
  totalPaidEstimated: number;
  agi: number;
  priorYearTaxLiability?: number;
}
