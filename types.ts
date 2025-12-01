
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

// Business Schedule X: Reconciliation
export interface BusinessScheduleXDetails {
  fedTaxableIncome: number;
  addBacks: {
    interestAndStateTaxes: number; // Income Taxes paid to states/cities
    wagesCredit: number; // Wages deducted for federal credits
    losses1231: number; // Capital Losses / 1231 Losses
    guaranteedPayments: number; // Payments to partners (Line 10 1065)
    expensesOnIntangibleIncome: number; // 5% Rule: Expenses incurred to earn non-taxable income
    other: number;
  };
  deductions: {
    interestIncome: number;
    dividends: number;
    capitalGains: number; // Capital Gains / 1231 Gains
    section179Excess: number;
    other: number;
  };
}

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
}

export interface W2Form extends BaseTaxForm { formType: TaxFormType.W2; employer: string; employerEin: string; employerAddress: Address; employerCounty?: string; totalMonthsInCity?: number; employee: string; employeeInfo?: TaxPayerProfile; federalWages: number; medicareWages: number; localWages: number; localWithheld: number; locality: string; taxDue?: number; lowConfidenceFields?: string[]; }
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
  userId?: string; // Links session to the authenticated user
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

export interface RealTimeExtractionUpdate {
  status: 'SCANNING' | 'ANALYZING' | 'EXTRACTING' | 'COMPLETE';
  progress: number;
  log: string[];
  detectedForms: string[];
  currentProfile?: { name: string; ssn: string };
  confidence: number;
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
  | 'TaxRates' 
  | 'IncomeInclusion' 
  | 'Deductions' 
  | 'Penalties' 
  | 'Filing' 
  | 'Allocation' 
  | 'Withholding' 
  | 'Validation';

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
