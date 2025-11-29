/**
 * Frontend types for Schedule Y apportionment and multi-state sourcing.
 * Mirrors backend domain models and DTOs for type-safe API interactions.
 */

/**
 * Apportionment formula types
 */
export enum ApportionmentFormula {
  THREE_FACTOR_EQUAL_WEIGHTED = 'THREE_FACTOR_EQUAL_WEIGHTED',
  FOUR_FACTOR_DOUBLE_WEIGHTED_SALES = 'FOUR_FACTOR_DOUBLE_WEIGHTED_SALES',
  SINGLE_SALES_FACTOR = 'SINGLE_SALES_FACTOR',
}

/**
 * Schedule Y filing record
 */
export interface ScheduleY {
  id: string;
  businessId: string;
  returnId: string;
  taxYear: number;
  apportionmentFormula: ApportionmentFormula;
  sourcingMethodElection: string;
  throwbackElection: string;
  serviceSourcingMethod: string | null;
  apportionmentPercentage: number;
  propertyFactor: PropertyFactor | null;
  payrollFactor: PayrollFactor | null;
  salesFactor: SalesFactor;
  apportionmentBreakdown: ApportionmentBreakdown | null;
  nexusStatus: NexusStatus | null;
  notes: string | null;
  isAmended: boolean;
  originalScheduleYId: string | null;
  filedAt: string;
  filedBy: string;
  updatedAt: string;
  updatedBy: string;
  createdAt: string;
}

/**
 * Schedule Y creation/update request
 */
export interface ScheduleYRequest {
  businessId: string;
  returnId: string;
  taxYear: number;
  apportionmentFormula: ApportionmentFormula;
  sourcingMethodElection: string;
  throwbackElection: string;
  serviceSourcingMethod?: string;
  propertyFactor?: PropertyFactorInput;
  payrollFactor?: PayrollFactorInput;
  salesFactor: SalesFactorInput;
  notes?: string;
  isAmended?: boolean;
  originalScheduleYId?: string;
}

/**
 * Property factor for apportionment calculation
 */
export interface PropertyFactor {
  id: string;
  ohioPropertyValue: number;
  totalPropertyValue: number;
  rentedPropertyValue: number;
  totalRentedPropertyValue: number;
  propertyFactorPercentage: number;
  ohioPropertyNumerator: number;
  everywherePropertyDenominator: number;
  notes: string | null;
}

/**
 * Property factor input for creation/update
 */
export interface PropertyFactorInput {
  ohioPropertyValue: number;
  totalPropertyValue: number;
  rentedPropertyValue?: number;
  totalRentedPropertyValue?: number;
  notes?: string;
}

/**
 * Payroll factor for apportionment calculation
 */
export interface PayrollFactor {
  id: string;
  ohioPayroll: number;
  totalPayroll: number;
  ohioEmployeeCount: number;
  totalEmployeeCount: number;
  remoteEmployeeCount: number;
  payrollFactorPercentage: number;
  autoPopulated: boolean;
  notes: string | null;
}

/**
 * Payroll factor input for creation/update
 */
export interface PayrollFactorInput {
  ohioPayroll: number;
  totalPayroll: number;
  ohioEmployeeCount?: number;
  totalEmployeeCount?: number;
  remoteEmployeeCount?: number;
  notes?: string;
}

/**
 * Sales factor for apportionment calculation
 */
export interface SalesFactor {
  id: string;
  ohioSales: number;
  totalSales: number;
  throwbackAdjustment: number;
  serviceRevenue: number;
  tangibleGoodsSales: number;
  salesFactorPercentage: number;
  saleTransactions: SaleTransaction[];
  transactionCount: number;
  throwbackTransactionCount: number;
  notes: string | null;
}

/**
 * Sales factor input for creation/update
 */
export interface SalesFactorInput {
  ohioSales: number;
  totalSales: number;
  throwbackAdjustment?: number;
  serviceRevenue?: number;
  tangibleGoodsSales?: number;
  saleTransactions?: SaleTransactionInput[];
  notes?: string;
}

/**
 * Individual sale transaction
 */
export interface SaleTransaction {
  id: string;
  saleType: string;
  amount: number;
  destinationState: string;
  originState: string | null;
  customerLocation: string | null;
  sourcingMethod: string | null;
  throwbackApplied: boolean;
  throwbackAmount: number;
  ohioSourcedAmount: number;
  description: string | null;
  transactionReference: string | null;
}

/**
 * Sale transaction input for creation
 */
export interface SaleTransactionInput {
  saleType: string;
  amount: number;
  destinationState: string;
  originState?: string;
  customerLocation?: string;
  description?: string;
  transactionReference?: string;
}

/**
 * Detailed apportionment breakdown
 */
export interface ApportionmentBreakdown {
  propertyFactorPercentage: number;
  propertyFactorWeight: number;
  propertyFactorWeightedContribution: number;
  payrollFactorPercentage: number;
  payrollFactorWeight: number;
  payrollFactorWeightedContribution: number;
  salesFactorPercentage: number;
  salesFactorWeight: number;
  salesFactorWeightedContribution: number;
  totalWeight: number;
  finalApportionmentPercentage: number;
  throwbackAdjustments: Record<string, number>;
  serviceSourcingAdjustments: Record<string, number>;
  totalSaleTransactions: number;
  throwbackTransactionCount: number;
  marketBasedServiceCount: number;
  formulaDescription: string;
  calculationExplanation: string;
}

/**
 * Nexus status information
 */
export interface NexusStatus {
  nexusByState: Record<string, boolean>;
  nexusReasonByState: Record<string, string>;
  nexusDeterminationDateByState: Record<string, string>;
  nexusStates: string[];
  nonNexusStates: string[];
  nexusStateCount: number;
  hasEconomicNexus: boolean;
  hasPhysicalPresenceNexus: boolean;
  hasFactorPresenceNexus: boolean;
  nexusDetails: Record<string, string>;
  lastUpdated: string;
}

/**
 * Schedule Y list response for pagination
 */
export interface ScheduleYListResponse {
  content: ScheduleY[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/**
 * API error response
 */
export interface ApiError {
  message: string;
  code: string;
  timestamp: string;
  path: string;
  details?: Record<string, string>;
}
