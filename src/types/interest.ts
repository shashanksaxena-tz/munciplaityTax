/**
 * TypeScript types for interest calculations.
 * 
 * Functional Requirements:
 * - FR-027 to FR-032: Interest calculation with quarterly compounding
 */

export enum CompoundingFrequency {
  DAILY = 'DAILY',
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY',
  ANNUALLY = 'ANNUALLY'
}

/**
 * Quarterly interest breakdown.
 */
export interface QuarterlyInterest {
  id?: string;
  quarter: string;
  startDate: string;
  endDate: string;
  daysInPeriod: number;
  beginningBalance: number;
  interestRate: number;
  interestAmount: number;
  endingBalance: number;
}

/**
 * Interest calculation entity.
 */
export interface Interest {
  id: string;
  tenantId: string;
  returnId: string;
  taxDueDate: string;
  startDate: string;
  endDate: string;
  unpaidTaxAmount: number;
  annualInterestRate: number;
  totalDays: number;
  totalInterestAmount: number;
  compoundingFrequency: CompoundingFrequency;
  quarterlyBreakdown: QuarterlyInterest[];
  createdBy: string;
  createdAt: string;
}

/**
 * Interest calculation request.
 */
export interface InterestCalculationRequest {
  tenantId?: string;
  returnId: string;
  taxDueDate: string;
  unpaidTaxAmount: number;
  startDate?: string;
  endDate?: string;
  annualInterestRate?: number;
  createdBy?: string;
  retrieveCurrentRate?: boolean;
  includeQuarterlyBreakdown?: boolean;
}

/**
 * Interest calculation response.
 */
export interface InterestCalculationResponse {
  interestId: string | null;
  returnId: string;
  taxDueDate: string;
  startDate: string;
  endDate: string;
  unpaidTaxAmount: number;
  annualInterestRate: number;
  totalDays: number;
  totalInterestAmount: number;
  compoundingFrequency?: CompoundingFrequency;
  quarterlyBreakdown?: QuarterlyInterest[];
  explanation: string;
}
