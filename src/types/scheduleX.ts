/**
 * TypeScript interfaces for expanded Business Schedule X (27 fields)
 * Corresponds to backend BusinessScheduleXDetails.java model
 * 
 * Feature: Comprehensive Business Schedule X Reconciliation (25+ Fields)
 * Functional Requirements: FR-001 through FR-038
 */

export interface BusinessScheduleXDetails {
  fedTaxableIncome: number;
  addBacks: AddBacks;
  deductions: Deductions;
  calculatedFields: CalculatedFields;
  metadata: Metadata;
}

/**
 * Add-backs (20 fields) - adjustments that increase federal taxable income for municipal purposes
 */
export interface AddBacks {
  // Old fields (maintained for backward compatibility)
  incomeAndStateTaxes: number;         // FR-003 - State/local/foreign income taxes
  guaranteedPayments: number;          // FR-004 - Form 1065 guaranteed payments (partnerships only)
  expensesOnIntangibleIncome: number;  // FR-012 - 5% Rule

  // New fields (FR-001 to FR-020)
  depreciationAdjustment: number;      // FR-001 - Book depreciation vs MACRS
  amortizationAdjustment: number;      // FR-002 - Book vs tax amortization (intangibles)
  mealsAndEntertainment: number;       // FR-005 - 100% add-back (50% federal → 0% municipal)
  relatedPartyExcess: number;          // FR-006 - Payments above fair market value
  penaltiesAndFines: number;           // FR-007 - Government penalties
  politicalContributions: number;      // FR-008 - Campaign contributions
  officerLifeInsurance: number;        // FR-009 - Premiums where corp is beneficiary
  capitalLossExcess: number;           // FR-010 - Capital losses exceeding gains
  federalTaxRefunds: number;           // FR-011 - Prior year federal refunds
  section179Excess: number;            // FR-013 - Section 179 over municipal limit
  bonusDepreciation: number;           // FR-014 - 100% federal bonus depreciation
  badDebtReserveIncrease: number;      // FR-015 - Reserve method adjustment
  charitableContributionExcess: number;// FR-016 - Contributions over 10% limit
  domesticProductionActivities: number;// FR-017 - DPAD Section 199
  stockCompensationAdjustment: number; // FR-018 - Book vs tax stock compensation
  inventoryMethodChange: number;       // FR-019 - Section 481(a) adjustment
  otherAddBacks: number;               // FR-020 - Catch-all field
  otherAddBacksDescription?: string | null;  // Required if otherAddBacks > 0
}

/**
 * Deductions (7 fields) - adjustments that decrease federal taxable income for municipal purposes
 */
export interface Deductions {
  // Old fields (maintained for backward compatibility)
  interestIncome: number;              // FR-021 - Non-taxable interest
  dividends: number;                   // FR-022 - Qualified and ordinary dividends
  capitalGains: number;                // FR-023 - Net capital gains
  
  // New fields (FR-024 to FR-027)
  section179Recapture: number;         // FR-024 - Recaptured Section 179 deduction
  municipalBondInterest: number;       // FR-025 - Cross-jurisdiction municipal bonds
  depletionDifference: number;         // FR-026 - Percentage vs cost depletion
  otherDeductions: number;             // FR-027 - Catch-all field
  otherDeductionsDescription?: string | null;  // Required if otherDeductions > 0
}

/**
 * Calculated fields (read-only) - computed from addBacks and deductions
 */
export interface CalculatedFields {
  totalAddBacks: number;               // FR-028 - Sum of all 20 add-back fields
  totalDeductions: number;             // FR-029 - Sum of all 7 deduction fields
  adjustedMunicipalIncome: number;     // FR-030 - fedTaxableIncome + totalAddBacks - totalDeductions
}

/**
 * Metadata for audit trail and AI extraction tracking
 */
export interface Metadata {
  lastModified: string;                     // ISO 8601 timestamp
  autoCalculatedFields: string[];           // FR-037 - List of fields auto-calculated
  manualOverrides: string[];                // FR-037 - List of fields manually overridden
  attachedDocuments: AttachedDocument[];    // FR-036 - Supporting documentation
}

export interface AttachedDocument {
  fileName: string;
  fileUrl: string;
  fieldName: string;        // Which Schedule X field this document supports
  uploadedAt: string;       // ISO 8601 timestamp
  uploadedBy: string;       // User ID
}

/**
 * AI Extraction result for Schedule X fields
 */
export interface ScheduleXExtractionResult {
  extractionId: string;
  returnId: string;
  extractedAt: string;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  fields: Record<string, ScheduleXFieldExtraction>;
  sourceDocuments: SourceDocument[];
  averageConfidence?: number;
  fieldsExtracted?: number;
}

export interface ScheduleXFieldExtraction {
  value: number;
  confidence: number;              // 0.0 - 1.0
  boundingBox?: BoundingBox;       // Constitution IV compliance
  sourceDocument: string;          // e.g., "Form 1120 Schedule M-1, Line 5a"
}

export interface BoundingBox {
  page: number;                    // 1-indexed PDF page number
  vertices: Vertex[];              // 4 vertices defining rectangle
}

export interface Vertex {
  x: number;                       // Horizontal position (pixels from left)
  y: number;                       // Vertical position (pixels from top)
}

export interface SourceDocument {
  fileName: string;
  fileUrl: string;
  pageCount: number;
  uploadedAt: string;
}

/**
 * Auto-calculation request/response
 */
export interface ScheduleXAutoCalcRequest {
  field: string;                   // Field name to auto-calculate
  inputs: Record<string, number>;  // Input values for calculation
  businessId?: string;             // Optional: for complex calculations requiring DB query
  taxYear?: number;                // Optional: for carryforward calculations
}

export interface ScheduleXAutoCalcResponse {
  calculatedValue: number;
  explanation: string;
  metadata?: Record<string, any>; // Additional data (e.g., carryforward amount)
}

/**
 * Multi-year comparison
 */
export interface MultiYearComparisonDto {
  years: number[];
  data: BusinessScheduleXDetails[];
  responseTime?: number;           // Query time in milliseconds
}

/**
 * Validation error types
 */
export interface ScheduleXValidationError {
  field: string;
  errorType: 'REQUIRED' | 'INVALID_FORMAT' | 'VARIANCE_WARNING' | 'REASONABLENESS_CHECK';
  message: string;
  severity: 'ERROR' | 'WARNING';
}

/**
 * Entity types for conditional field rendering
 */
export enum EntityType {
  C_CORP = 'C-CORP',
  S_CORP = 'S-CORP',
  PARTNERSHIP = 'PARTNERSHIP',
  SOLE_PROPRIETORSHIP = 'SOLE_PROPRIETORSHIP'
}

/**
 * Helper function to create empty BusinessScheduleXDetails
 */
export function createEmptyScheduleXDetails(fedTaxableIncome: number = 0): BusinessScheduleXDetails {
  return {
    fedTaxableIncome,
    addBacks: {
      depreciationAdjustment: 0,
      amortizationAdjustment: 0,
      incomeAndStateTaxes: 0,
      guaranteedPayments: 0,
      mealsAndEntertainment: 0,
      relatedPartyExcess: 0,
      penaltiesAndFines: 0,
      politicalContributions: 0,
      officerLifeInsurance: 0,
      capitalLossExcess: 0,
      federalTaxRefunds: 0,
      expensesOnIntangibleIncome: 0,
      section179Excess: 0,
      bonusDepreciation: 0,
      badDebtReserveIncrease: 0,
      charitableContributionExcess: 0,
      domesticProductionActivities: 0,
      stockCompensationAdjustment: 0,
      inventoryMethodChange: 0,
      otherAddBacks: 0,
      otherAddBacksDescription: null
    },
    deductions: {
      interestIncome: 0,
      dividends: 0,
      capitalGains: 0,
      section179Recapture: 0,
      municipalBondInterest: 0,
      depletionDifference: 0,
      otherDeductions: 0,
      otherDeductionsDescription: null
    },
    calculatedFields: {
      totalAddBacks: 0,
      totalDeductions: 0,
      adjustedMunicipalIncome: fedTaxableIncome
    },
    metadata: {
      lastModified: new Date().toISOString(),
      autoCalculatedFields: [],
      manualOverrides: [],
      attachedDocuments: []
    }
  };
}

/**
 * Helper function to recalculate totals
 */
export function recalculateTotals(scheduleX: BusinessScheduleXDetails): BusinessScheduleXDetails {
  const totalAddBacks = 
    scheduleX.addBacks.depreciationAdjustment +
    scheduleX.addBacks.amortizationAdjustment +
    scheduleX.addBacks.incomeAndStateTaxes +
    scheduleX.addBacks.guaranteedPayments +
    scheduleX.addBacks.mealsAndEntertainment +
    scheduleX.addBacks.relatedPartyExcess +
    scheduleX.addBacks.penaltiesAndFines +
    scheduleX.addBacks.politicalContributions +
    scheduleX.addBacks.officerLifeInsurance +
    scheduleX.addBacks.capitalLossExcess +
    scheduleX.addBacks.federalTaxRefunds +
    scheduleX.addBacks.expensesOnIntangibleIncome +
    scheduleX.addBacks.section179Excess +
    scheduleX.addBacks.bonusDepreciation +
    scheduleX.addBacks.badDebtReserveIncrease +
    scheduleX.addBacks.charitableContributionExcess +
    scheduleX.addBacks.domesticProductionActivities +
    scheduleX.addBacks.stockCompensationAdjustment +
    scheduleX.addBacks.inventoryMethodChange +
    scheduleX.addBacks.otherAddBacks;

  const totalDeductions =
    scheduleX.deductions.interestIncome +
    scheduleX.deductions.dividends +
    scheduleX.deductions.capitalGains +
    scheduleX.deductions.section179Recapture +
    scheduleX.deductions.municipalBondInterest +
    scheduleX.deductions.depletionDifference +
    scheduleX.deductions.otherDeductions;

  const adjustedMunicipalIncome = scheduleX.fedTaxableIncome + totalAddBacks - totalDeductions;

  return {
    ...scheduleX,
    calculatedFields: {
      totalAddBacks,
      totalDeductions,
      adjustedMunicipalIncome
    }
  };
}
