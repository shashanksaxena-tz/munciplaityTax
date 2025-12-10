
export enum TaxFormType {
  W2 = 'W-2',
  FORM_1099_NEC = '1099-NEC',
  FORM_1099_MISC = '1099-MISC',
  W2G = 'W-2G',
  SCHEDULE_C = 'Schedule C',
  SCHEDULE_E = 'Schedule E',
  SCHEDULE_F = 'Schedule F',
  LOCAL_1040 = 'Dublin 1040',
  LOCAL_1040_EZ = 'Dublin 1040EZ',
  FORM_R = 'Form R',
  FEDERAL_1040 = 'Federal 1040',
  
  // Business Specific
  FORM_W1 = 'Form W-1 (Withholding)',
  FORM_W3 = 'Form W-3 (Reconciliation)',
  FORM_27 = 'Form 27 (Net Profits)',
  FORM_1120 = 'Federal 1120 (Corp)',
  FORM_1065 = 'Federal 1065 (Partnership)',
  PAYROLL_SUMMARY = 'Payroll Summary'
}

export enum TaxReturnStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  IN_REVIEW = 'IN_REVIEW',
  AWAITING_DOCUMENTATION = 'AWAITING_DOCUMENTATION',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  AMENDED = 'AMENDED',
  PAID = 'PAID',
  LATE = 'LATE'
}

export enum FilingFrequency {
  DAILY = 'DAILY',
  SEMI_MONTHLY = 'SEMI_MONTHLY',
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY'
}

export enum AppStep {
  DASHBOARD = 'DASHBOARD',
  REGISTER_BUSINESS = 'REGISTER_BUSINESS',
  BUSINESS_DASHBOARD = 'BUSINESS_DASHBOARD',
  WITHHOLDING_WIZARD = 'WITHHOLDING_WIZARD',
  NET_PROFITS_WIZARD = 'NET_PROFITS_WIZARD',
  RECONCILIATION_WIZARD = 'RECONCILIATION_WIZARD',
  BUSINESS_HISTORY = 'BUSINESS_HISTORY',
  BUSINESS_RULES = 'BUSINESS_RULES',
  AUDITOR_DASHBOARD = 'AUDITOR_DASHBOARD',
  AUDITOR_REVIEW = 'AUDITOR_REVIEW',
  UPLOAD = 'UPLOAD',
  SUMMARY = 'SUMMARY',
  REVIEW = 'REVIEW',
  CALCULATING = 'CALCULATING',
  RESULTS = 'RESULTS'
}

export enum FilingStatus {
  SINGLE = 'SINGLE',
  MARRIED_FILING_JOINTLY = 'MARRIED_FILING_JOINTLY',
  MARRIED_FILING_SEPARATELY = 'MARRIED_FILING_SEPARATELY',
  HEAD_OF_HOUSEHOLD = 'HEAD_OF_HOUSEHOLD',
  QUALIFYING_WIDOWER = 'QUALIFYING_WIDOWER'
}

export interface Address {
  street: string;
  city: string;
  state: string;
  zip: string;
  country?: string;
  verificationStatus?: 'UNVERIFIED' | 'VERIFIED_IN_DISTRICT' | 'VERIFIED_OUT_DISTRICT' | 'JEDD';
}

export interface TaxPayerProfile {
  name: string;
  ssn?: string; 
  address?: Address;
  filingStatus?: FilingStatus;
  spouse?: {
    name: string;
    ssn?: string;
  };
}

export interface BusinessProfile {
  businessName: string;
  fein: string;
  accountNumber: string; 
  address: Address;
  filingFrequency: FilingFrequency;
  fiscalYearEnd: string; 
}

export interface TaxReturnSettings {
  taxYear: number;
  isAmendment: boolean;
  amendmentReason?: string;
}

export interface WithholdingPeriod {
  year: number;
  period: string; 
  startDate: string;
  endDate: string;
  dueDate: string;
}

export interface WithholdingReturnData {
  id: string;
  dateFiled: string;
  period: WithholdingPeriod;
  grossWages: number; 
  taxDue: number;
  adjustments: number;
  penalty: number;
  interest: number;
  totalAmountDue: number;
  isReconciled: boolean;
  paymentStatus: 'PAID' | 'UNPAID';
  confirmationNumber?: string;
}

// Business Schedule X: Reconciliation - Import from canonical source
export type { BusinessScheduleXDetails, AddBacks, Deductions, CalculatedFields, Metadata } from './src/types/scheduleX';

// Business Schedule Y: Allocation
export interface BusinessAllocation {
  property: { dublin: number; everywhere: number; pct: number };
  payroll: { dublin: number; everywhere: number; pct: number };
  sales: { dublin: number; everywhere: number; pct: number };
  totalPct: number;
  averagePct: number;
}

export interface NetProfitReturnData {
  id: string;
  dateFiled: string;
  taxYear: number;
  
  // New granular details
  reconciliation: BusinessScheduleXDetails;
  allocation: BusinessAllocation;
  
  adjustedFedTaxableIncome: number; // From Sch X
  allocatedTaxableIncome: number; // Adjusted * Allocation%
  
  // NOL Logic
  nolAvailable: number;
  nolApplied: number;
  taxableIncomeAfterNOL: number;

  taxDue: number;
  estimatedPayments: number;
  priorYearCredit: number;
  
  // Penalty Details
  penaltyUnderpayment: number;
  penaltyLateFiling: number;
  interest: number;

  balanceDue: number;
  
  paymentStatus: 'PAID' | 'UNPAID';
  confirmationNumber?: string;
}

export interface ReconciliationReturnData {
  id: string;
  dateFiled: string;
  taxYear: number;
  totalW1Tax: number;
  totalW2Tax: number;
  discrepancy: number;
  status: 'BALANCED' | 'UNBALANCED';
  confirmationNumber?: string;
}

// --- FORM INTERFACES ---

export interface BaseTaxForm {
  id: string;
  fileName: string;
  taxYear: number;
  confidenceScore?: number; 
  fieldConfidence?: Record<string, number>;
  sourcePage?: number;
  extractionReason?: string;
  formType: TaxFormType;
  owner?: 'PRIMARY' | 'SPOUSE';
  // Enhanced extraction metadata
  isAiExtracted?: boolean;           // True if form was extracted by AI, false/undefined if manually entered
  sourceDocumentUrl?: string;        // URL to the original uploaded PDF for validation
  sourceDocumentName?: string;       // Original filename of uploaded document
  fieldExtractionReasons?: Record<string, string>;  // Per-field explanation of why value was extracted
  fieldPageNumbers?: Record<string, number>;        // Per-field page number in source document
  fieldBoundingBoxes?: Record<string, BoundingBox>; // Per-field location in PDF for highlighting
}

export interface W2Form extends BaseTaxForm { 
  formType: TaxFormType.W2; 
  employer: string; 
  employerEin: string; 
  employerAddress: Address; 
  employerCounty?: string; 
  totalMonthsInCity?: number; 
  employee: string; 
  employeeSSN?: string;
  employeeAddress?: Address;
  employeeInfo?: TaxPayerProfile; 
  federalWages: number; 
  federalWithheld?: number;
  socialSecurityWages?: number;
  socialSecurityTaxWithheld?: number;
  medicareWages: number; 
  medicareTaxWithheld?: number;
  stateWages?: number;
  stateIncomeTax?: number;
  state?: string;
  localWages: number; 
  localWithheld: number; 
  locality: string; 
  taxDue?: number; 
  lowConfidenceFields?: string[]; 
}
export interface W2GForm extends BaseTaxForm { formType: TaxFormType.W2G; payer: string; payerEin: string; payerAddress: Address; recipient: string; recipientTin?: string; grossWinnings: number; dateWon: string; typeOfWager: string; federalWithheld: number; stateWithheld: number; localWinnings: number; localWithheld: number; locality: string; lowConfidenceFields?: string[]; }
export interface Form1099 extends BaseTaxForm { formType: TaxFormType.FORM_1099_NEC | TaxFormType.FORM_1099_MISC; payer: string; payerTin?: string; payerAddress?: Address; recipient: string; incomeAmount: number; federalWithheld: number; stateWithheld: number; localWithheld: number; locality: string; lowConfidenceFields?: string[]; }
export interface ScheduleC extends BaseTaxForm { formType: TaxFormType.SCHEDULE_C; principalBusiness: string; businessCode: string; businessName: string; businessEin: string; businessAddress: Address; grossReceipts: number; totalExpenses: number; netProfit: number; lowConfidenceFields?: string[]; }
export interface ScheduleE extends BaseTaxForm { formType: TaxFormType.SCHEDULE_E; rentals: RentalProperty[]; partnerships: PartnershipEntity[]; totalNetIncome: number; lowConfidenceFields?: string[]; }
export interface ScheduleF extends BaseTaxForm { formType: TaxFormType.SCHEDULE_F; principalProduct: string; businessName: string; businessCode: string; ein: string; grossIncome: number; totalExpenses: number; netFarmProfit: number; lowConfidenceFields?: string[]; }

export interface LocalTaxForm extends BaseTaxForm { 
  formType: TaxFormType.LOCAL_1040 | TaxFormType.LOCAL_1040_EZ | TaxFormType.FORM_R; 
  qualifyingWages: number; // Line 1
  otherIncome: number;     // Line 2
  totalIncome: number;     // Line 3
  taxDue: number;          // Line 6
  credits: number;         // Line 7
  overpayment: number;     // Line 13
  reportedTaxableIncome: number; // Line 5 (Legacy prop)
  reportedTaxDue: number; // Legacy prop
}

export interface FederalTaxForm extends BaseTaxForm {
  formType: TaxFormType.FEDERAL_1040;
  wages: number; // 1z
  qualifiedDividends: number; // 3a
  pensions: number; // 5b
  socialSecurity: number; // 6b
  capitalGains: number; // 7
  otherIncome: number; // 8
  totalIncome: number; // 9
  adjustedGrossIncome: number; // 11
  tax: number; // 24
}

export interface BusinessFederalForm extends BaseTaxForm {
  formType: TaxFormType.FORM_1120 | TaxFormType.FORM_1065 | TaxFormType.FORM_27;
  businessName: string;
  ein: string;
  fedTaxableIncome: number;
  reconciliation?: BusinessScheduleXDetails;
  allocation?: BusinessAllocation;
}

export interface RentalProperty { id: string; streetAddress: string; city: string; state: string; zip: string; rentalType: string; line21_FairRentalDays_or_Income: number; line22_DeductibleLoss: number; calculatedNetIncome: number; }
export interface PartnershipEntity { id: string; name: string; ein: string; netProfit: number; }

export type TaxFormData = W2Form | W2GForm | Form1099 | ScheduleC | ScheduleE | ScheduleF | LocalTaxForm | FederalTaxForm | BusinessFederalForm;

// --- CONFIG ---

export enum W2QualifyingWagesRule {
  HIGHEST_OF_ALL = 'HIGHEST_OF_ALL',
  BOX_5_MEDICARE = 'BOX_5_MEDICARE',
  BOX_18_LOCAL = 'BOX_18_LOCAL',
  BOX_1_FEDERAL = 'BOX_1_FEDERAL'
}

export interface TaxRulesConfig {
  municipalRate: number;
  municipalCreditLimitRate: number;
  municipalRates: Record<string, number>;
  w2QualifyingWagesRule: W2QualifyingWagesRule;
  incomeInclusion: {
    scheduleC: boolean;
    scheduleE: boolean;
    scheduleF: boolean;
    w2g: boolean;
    form1099: boolean;
  };
  enableRounding: boolean;
}

export interface BusinessTaxRulesConfig {
  municipalRate: number;
  minimumTax: number;
  allocationMethod: '3_FACTOR' | 'GROSS_RECEIPTS_ONLY';
  allocationSalesFactorWeight: number; 
  enableNOL: boolean;
  nolOffsetCapPercent: number; 
  intangibleExpenseRate: number; // 5% Rule
  safeHarborPercent: number;
  penaltyRateLateFiling: number;
  penaltyRateUnderpayment: number;
  interestRateAnnual: number;
}

// --- SESSION ---

export interface TaxReturnSession {
  id: string;
  createdDate: string;
  lastModifiedDate: string;
  status: TaxReturnStatus;
  type: 'INDIVIDUAL' | 'BUSINESS';
  profile: TaxPayerProfile | BusinessProfile; 
  settings: TaxReturnSettings;
  forms: TaxFormData[];
  
  // Results
  lastCalculationResult?: TaxCalculationResult;
  
  // Business History
  businessFilings?: WithholdingReturnData[]; 
  netProfitFilings?: NetProfitReturnData[];
  reconciliations?: ReconciliationReturnData[];
}

export interface TaxBreakdownRule { category: string; ruleName: string; description: string; calculation: string; amount: number; }

export interface TaxCalculationResult {
  settings: TaxReturnSettings;
  profile: TaxPayerProfile;
  totalGrossIncome: number;
  totalLocalWithheld: number;
  w2TaxableIncome: number;
  scheduleX: { entries: ScheduleXEntry[], totalNetProfit: number };
  scheduleY: { entries: ScheduleYEntry[], totalCredit: number };
  totalTaxableIncome: number;
  municipalLiability: number;
  municipalLiabilityAfterCredits: number;
  municipalBalance: number;
  breakdown: TaxBreakdownRule[];
  discrepancyReport?: DiscrepancyReport;
}

export interface ScheduleXEntry { source: string; type: string; gross: number; expenses: number; netProfit: number; }
export interface ScheduleYEntry { source: string; locality: string; cityTaxRate: number; incomeTaxedByOtherCity: number; taxPaidToOtherCity: number; creditAllowed: number; }

export interface DiscrepancyReport { 
  hasDiscrepancies: boolean; 
  issues: DiscrepancyIssue[];
  summary?: DiscrepancySummary;
}

export interface DiscrepancyIssue { 
  issueId: string;
  ruleId: string;
  category: string;
  field: string; 
  sourceValue: number; 
  formValue: number; 
  difference: number;
  differencePercent?: number;
  severity: 'HIGH'|'MEDIUM'|'LOW'; 
  message: string;
  recommendedAction?: string;
  isAccepted?: boolean;
  acceptanceNote?: string;
  acceptedDate?: string;
}

export interface DiscrepancySummary {
  totalIssues: number;
  highSeverityCount: number;
  mediumSeverityCount: number;
  lowSeverityCount: number;
  blocksFiling: boolean;
}

export interface PaymentRecord { id: string; date: string; amount: number; confirmationNumber: string; method: 'CREDIT_CARD' | 'ACH'; status: 'SUCCESS' | 'FAILED'; }

// ===== RECONCILIATION TYPES (W-1 Filing Reconciliation) =====

export enum ReconciliationIssueType {
  WAGE_MISMATCH_FEDERAL = 'WAGE_MISMATCH_FEDERAL',
  WAGE_MISMATCH_LOCAL = 'WAGE_MISMATCH_LOCAL',
  WITHHOLDING_RATE_INVALID = 'WITHHOLDING_RATE_INVALID',
  CUMULATIVE_MISMATCH = 'CUMULATIVE_MISMATCH',
  MISSING_FILING = 'MISSING_FILING',
  DUPLICATE_FILING = 'DUPLICATE_FILING',
  LATE_FILING = 'LATE_FILING'
}

export enum ReconciliationIssueSeverity {
  CRITICAL = 'CRITICAL',
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW'
}

export interface ReconciliationIssue {
  id: string;
  employerId: string;
  taxYear: number;
  period: string;
  issueType: ReconciliationIssueType;
  severity: ReconciliationIssueSeverity;
  description: string;
  expectedValue?: number;
  actualValue?: number;
  variance?: number;
  variancePercentage?: number;
  dueDate?: string;
  filingDate?: string;
  recommendedAction: string;
  resolved: boolean;
  resolutionNote?: string;
  resolvedDate?: string;
}

export interface RealTimeExtractionUpdate {
  status: 'SCANNING' | 'ANALYZING' | 'EXTRACTING' | 'COMPLETE' | 'ERROR';
  progress: number;
  log: string[];
  detectedForms: string[];
  currentProfile?: { name: string; ssn: string };
  confidence: number;
  // Enhanced provenance and confidence tracking
  currentFormType?: string;
  currentTaxpayerName?: string;
  fieldConfidences?: Record<string, FieldConfidenceInfo>;
  formProvenances?: FormProvenance[];
  summary?: ExtractionSummary;
  result?: any;
}

export interface FieldConfidenceInfo {
  fieldName: string;
  confidence: number;        // 0.0 - 1.0
  weight: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  weightedScore: number;     // confidence * weight multiplier
  extractionSource: 'AI_EXTRACTED' | 'DERIVED' | 'DEFAULT';
}

export interface FormProvenance {
  formType: string;
  pageNumber: number;
  boundingBox?: BoundingBox;
  extractionReason: string;
  formConfidence: number;
  fields: FieldProvenance[];
}

export interface BoundingBox {
  x: number;      // Left position (0-1 normalized)
  y: number;      // Top position (0-1 normalized)
  width: number;  // Width (0-1 normalized)
  height: number; // Height (0-1 normalized)
}

export interface FieldProvenance {
  fieldName: string;
  pageNumber: number;
  boundingBox?: BoundingBox;
  rawValue?: string;
  processedValue?: string;
  confidence: number;
}

export interface ExtractionSummary {
  totalPagesScanned: number;
  formsExtracted: number;
  formsSkipped: number;
  extractedFormTypes: string[];
  skippedForms: SkippedForm[];
  overallConfidence: number;
  confidenceByFormType: Record<string, number>;
  extractionDurationMs: number;
  modelUsed: string;
}

export interface SkippedForm {
  formType: string;
  pageNumber: number;
  reason: string;
  suggestion: string;
}

// ===== AUDITOR WORKFLOW TYPES =====

export enum AuditPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH'
}

export enum AuditStatus {
  PENDING = 'PENDING',
  IN_REVIEW = 'IN_REVIEW',
  AWAITING_DOCUMENTATION = 'AWAITING_DOCUMENTATION',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  AMENDED = 'AMENDED'
}

export enum AuditActionType {
  ASSIGNED = 'ASSIGNED',
  REVIEW_STARTED = 'REVIEW_STARTED',
  REVIEW_COMPLETED = 'REVIEW_COMPLETED',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  DOCS_REQUESTED = 'DOCS_REQUESTED',
  ANNOTATED = 'ANNOTATED',
  ESCALATED = 'ESCALATED',
  PRIORITY_CHANGED = 'PRIORITY_CHANGED',
  REASSIGNED = 'REASSIGNED'
}

export enum DocumentType {
  GENERAL_LEDGER = 'GENERAL_LEDGER',
  BANK_STATEMENTS = 'BANK_STATEMENTS',
  DEPRECIATION_SCHEDULE = 'DEPRECIATION_SCHEDULE',
  CONTRACTS = 'CONTRACTS',
  INVOICES = 'INVOICES',
  RECEIPTS = 'RECEIPTS',
  PAYROLL_RECORDS = 'PAYROLL_RECORDS',
  TAX_RETURNS_PRIOR_YEAR = 'TAX_RETURNS_PRIOR_YEAR',
  OTHER = 'OTHER'
}

export enum DocumentRequestStatus {
  PENDING = 'PENDING',
  RECEIVED = 'RECEIVED',
  OVERDUE = 'OVERDUE',
  WAIVED = 'WAIVED'
}

export enum RiskLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH'
}

export enum AuditEventType {
  SUBMISSION = 'SUBMISSION',
  ASSIGNMENT = 'ASSIGNMENT',
  REVIEW_STARTED = 'REVIEW_STARTED',
  REVIEW_COMPLETED = 'REVIEW_COMPLETED',
  APPROVAL = 'APPROVAL',
  REJECTION = 'REJECTION',
  AMENDMENT = 'AMENDMENT',
  PAYMENT = 'PAYMENT',
  COMMUNICATION = 'COMMUNICATION',
  ESCALATION = 'ESCALATION',
  DOCUMENT_REQUEST = 'DOCUMENT_REQUEST',
  DOCUMENT_RECEIVED = 'DOCUMENT_RECEIVED',
  PRIORITY_CHANGE = 'PRIORITY_CHANGE',
  STATUS_CHANGE = 'STATUS_CHANGE',
  ANNOTATION_ADDED = 'ANNOTATION_ADDED'
}

export interface AuditQueue {
  queueId: string;
  returnId: string;
  priority: AuditPriority;
  status: AuditStatus;
  submissionDate: string;
  assignedAuditorId?: string;
  assignmentDate?: string;
  reviewStartedDate?: string;
  reviewCompletedDate?: string;
  riskScore: number;
  flaggedIssuesCount: number;
  daysInQueue?: number;
  tenantId?: string;
  
  // Additional display fields
  taxpayerName?: string;
  returnType?: string;
  taxYear?: string;
  taxDue?: number;
}

export interface AuditAction {
  actionId: string;
  returnId: string;
  auditorId: string;
  actionType: AuditActionType;
  actionDate: string;
  actionDetails?: string;
  previousStatus?: string;
  newStatus?: string;
  ipAddress?: string;
  userAgent?: string;
  tenantId?: string;
}

export interface DocumentRequest {
  requestId: string;
  returnId: string;
  auditorId: string;
  requestDate: string;
  documentType: DocumentType;
  description: string;
  deadline: string;
  status: DocumentRequestStatus;
  receivedDate?: string;
  uploadedFiles?: string[];
  tenantId?: string;
}

export interface AuditReport {
  reportId: string;
  returnId: string;
  generatedDate: string;
  riskScore: number;
  riskLevel: RiskLevel;
  flaggedItems: string[]; // JSON strings
  yearOverYearComparison?: string; // JSON
  peerComparison?: string; // JSON
  patternAnalysis?: string; // JSON
  recommendedActions: string[];
  auditorOverride: boolean;
  overrideReason?: string;
  tenantId?: string;
}

export interface AuditTrail {
  trailId: string;
  returnId: string;
  eventType: AuditEventType;
  userId: string;
  timestamp: string;
  ipAddress?: string;
  eventDetails?: string;
  digitalSignature?: string;
  immutable: boolean;
  tenantId?: string;
}

export interface AuditQueueFilters {
  status?: AuditStatus;
  priority?: AuditPriority;
  auditorId?: string;
  tenantId?: string;
  fromDate?: number;
  toDate?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface AuditQueueStats {
  pending: number;
  highPriority: number;
  inReview?: number;
  approved?: number;
  rejected?: number;
}

export interface ApprovalRequest {
  returnId: string;
  auditorId: string;
  eSignature: string;
}

export interface RejectionRequest {
  returnId: string;
  auditorId: string;
  reason: string;
  detailedExplanation: string;
  resubmitDeadline: string;
}

export interface DocumentRequestPayload {
  returnId: string;
  auditorId: string;
  documentType: DocumentType;
  description: string;
  deadline: string;
  tenantId: string;
}

// ============================================================================
// Tax Rule Configuration Types (Feature 4: Dynamic Rule Configuration System)
// ============================================================================

export type RuleCategory = 
  | 'TAX_RATES' 
  | 'INCOME_INCLUSION' 
  | 'DEDUCTIONS' 
  | 'PENALTIES' 
  | 'FILING' 
  | 'ALLOCATION' 
  | 'WITHHOLDING' 
  | 'VALIDATION';

export type RuleValueType = 
  | 'NUMBER' 
  | 'PERCENTAGE' 
  | 'ENUM' 
  | 'BOOLEAN' 
  | 'FORMULA' 
  | 'CONDITIONAL';

export type ApprovalStatus = 
  | 'PENDING' 
  | 'APPROVED' 
  | 'REJECTED' 
  | 'VOIDED';

export type ChangeType = 
  | 'CREATE' 
  | 'UPDATE' 
  | 'DELETE' 
  | 'APPROVE' 
  | 'REJECT' 
  | 'VOID' 
  | 'ROLLBACK';

export type RuleValue = 
  | NumberValue 
  | PercentageValue 
  | EnumValue 
  | BooleanValue 
  | FormulaValue 
  | ConditionalValue;

export interface NumberValue {
  scalar: number;
}

export interface PercentageValue {
  scalar: number;
  unit: 'percent';
}

export interface EnumValue {
  option: string;
  allowedValues: string[];
}

export interface BooleanValue {
  flag: boolean;
}

export interface FormulaValue {
  expression: string;
  variables: string[];
  returnType: 'number' | 'string' | 'boolean';
}

export interface ConditionalValue {
  condition: string;
  thenValue: any;
  elseValue: any;
  returnType: 'number' | 'string' | 'boolean';
}

export interface TaxRule {
  ruleId: string;
  ruleCode: string;
  ruleName: string;
  description?: string;    // Human-readable description of what this rule does
  category: RuleCategory;
  valueType: RuleValueType;
  value: RuleValue;
  effectiveDate: string;  // ISO 8601 date
  endDate?: string;       // ISO 8601 date
  tenantId: string;
  entityTypes: string[];
  appliesTo?: string;
  version: number;
  previousVersionId?: string;
  dependsOn?: string[];
  approvalStatus: ApprovalStatus;
  approvedBy?: string;
  approvalDate?: string;  // ISO 8601 timestamp
  createdBy: string;
  createdDate: string;    // ISO 8601 timestamp
  modifiedBy?: string;
  modifiedDate?: string;  // ISO 8601 timestamp
  changeReason: string;
  ordinanceReference?: string;
  isSystem?: boolean;     // Indicates if this is a default/system rule
  isMock?: boolean;       // Indicates if this is a mock/demo rule (not from backend)
}

export interface CreateRuleRequest {
  ruleCode: string;
  ruleName: string;
  category: RuleCategory;
  valueType: RuleValueType;
  value: RuleValue;
  effectiveDate: string;
  endDate?: string;
  tenantId: string;
  entityTypes?: string[];
  appliesTo?: string;
  previousVersionId?: string;
  dependsOn?: string[];
  changeReason: string;
  ordinanceReference?: string;
}

export interface UpdateRuleRequest {
  ruleName?: string;
  category?: RuleCategory;
  valueType?: RuleValueType;
  value?: RuleValue;
  effectiveDate?: string;
  endDate?: string;
  entityTypes?: string[];
  appliesTo?: string;
  dependsOn?: string[];
  changeReason?: string;
  ordinanceReference?: string;
}

export interface RuleChangeLog {
  logId: string;
  ruleId: string;
  changeType: ChangeType;
  oldValue?: any;
  newValue: any;
  changedFields: string[];
  changedBy: string;
  changeDate: string;  // ISO 8601 timestamp
  changeReason: string;
  affectedReturnsCount: number;
  impactEstimate?: ImpactEstimate;
}

export interface ImpactEstimate {
  totalAffectedTaxpayers: number;
  avgTaxIncrease: number;
  avgTaxDecrease: number;
  maxImpact: number;
  minImpact: number;
  medianImpact: number;
}

// ===== SUBMISSION DOCUMENT TYPES (Spec 015) =====

/**
 * Links an uploaded PDF document to a tax return submission
 */
export interface SubmissionDocument {
  id: string;
  submissionId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  uploadedAt: string;
  base64Data?: string; // For viewing
  thumbnailUrl?: string;
}

/**
 * Tracks manual entries with optional supporting documents
 */
export interface ManualEntry {
  id: string;
  formType: TaxFormType;
  fieldName: string;
  value: string | number;
  enteredBy: string;
  enteredAt: string;
  supportingDocumentId?: string;
  note?: string;
}

/**
 * Tracks changes to extracted values for audit trail
 */
export interface FieldAuditTrail {
  fieldId: string;
  formId: string;
  fieldName: string;
  originalValue: string | number;
  correctedValue: string | number;
  correctedBy: string;
  correctedAt: string;
  reason?: string;
  sourceDocumentId?: string;
  sourcePageNumber?: number;
  sourceBoundingBox?: BoundingBox;
}

/**
 * Extended W-2 form with all box fields (Spec 015 - Extended Extraction)
 */
export interface ExtendedW2Form extends W2Form {
  // Additional W-2 boxes for comprehensive extraction
  socialSecurityWages?: number;    // Box 3
  socialSecurityTaxWithheld?: number; // Box 4
  medicareTaxWithheld?: number;    // Box 6
  socialSecurityTips?: number;     // Box 7
  allocatedTips?: number;          // Box 8
  dependentCareBenefits?: number;  // Box 10
  nonqualifiedPlans?: number;      // Box 11
  box12Codes?: { code: string; amount: number }[]; // Box 12a-d
  statutoryEmployee?: boolean;     // Box 13
  retirementPlan?: boolean;        // Box 13
  thirdPartySickPay?: boolean;     // Box 13
  box14Other?: string;             // Box 14
  stateWages?: number;             // Box 16
  stateIncomeTax?: number;         // Box 17
}

/**
 * Extended Federal 1040 with all key lines (Spec 015 - Extended Extraction)
 */
export interface ExtendedFederal1040 extends FederalTaxForm {
  // Income lines
  taxExemptInterest?: number;       // Line 2a
  taxableInterest?: number;         // Line 2b
  ordinaryDividends?: number;       // Line 3b
  iraDistributions?: number;        // Line 4a
  taxableIra?: number;              // Line 4b
  pensionsAnnuities?: number;       // Line 5a
  taxablePensions?: number;         // Line 5b
  socialSecurityBenefits?: number;  // Line 6a
  taxableSocialSecurity?: number;   // Line 6b
  
  // Deductions and tax
  standardDeduction?: number;       // Line 12
  qualifiedBusinessIncome?: number; // Line 13
  taxableIncome?: number;           // Line 15
  totalTax?: number;                // Line 24
  federalWithholding?: number;      // Line 25a
  estimatedTaxPayments?: number;    // Line 26
  refundAmount?: number;            // Line 35a
  amountOwed?: number;              // Line 37
}

/**
 * PDF Viewer state for source highlighting
 */
export interface PdfViewerState {
  currentPage: number;
  totalPages: number;
  zoom: number;
  highlightedField?: {
    fieldName: string;
    boundingBox: BoundingBox;
    formType: string;
  };
  documentUrl: string;
}
