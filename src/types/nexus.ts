/**
 * Frontend types for nexus tracking and determination.
 * Defines nexus status, reasons, and related information.
 */

// Re-export NexusStatus from apportionment types
export type { NexusStatus } from './apportionment';

/**
 * Nexus reason types
 */
export enum NexusReason {
  /** Physical presence (office, warehouse, property) */
  PHYSICAL_PRESENCE = 'PHYSICAL_PRESENCE',
  
  /** Employee presence (full-time, part-time, remote) */
  EMPLOYEE_PRESENCE = 'EMPLOYEE_PRESENCE',
  
  /** Economic nexus ($500K+ sales or 200+ transactions) */
  ECONOMIC_NEXUS = 'ECONOMIC_NEXUS',
  
  /** Factor presence nexus (property, payroll, or sales factor) */
  FACTOR_PRESENCE = 'FACTOR_PRESENCE',
  
  /** Affiliate or subsidiary nexus */
  AFFILIATE_NEXUS = 'AFFILIATE_NEXUS',
  
  /** Click-through nexus (referral agreements) */
  CLICK_THROUGH_NEXUS = 'CLICK_THROUGH_NEXUS',
  
  /** Public Law 86-272 protection (solicitation only) */
  PL_86_272_PROTECTED = 'PL_86_272_PROTECTED',
  
  /** No nexus */
  NO_NEXUS = 'NO_NEXUS',
}

/**
 * Nexus tracking record for a specific state
 */
export interface NexusTracking {
  id: string;
  businessId: string;
  state: string;
  hasNexus: boolean;
  nexusReason: NexusReason;
  lastDeterminationDate: string;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Nexus status for a business across all states
 */
export interface NexusStatusSummary {
  businessId: string;
  nexusByState: Record<string, boolean>;
  nexusReasonByState: Record<string, NexusReason>;
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
 * Nexus determination request
 */
export interface NexusDeterminationRequest {
  businessId: string;
  state: string;
  taxYear: number;
  totalSales?: number;
  transactionCount?: number;
  hasPhysicalPresence?: boolean;
  hasEmployees?: boolean;
  notes?: string;
}

/**
 * Nexus update request
 */
export interface NexusUpdateRequest {
  businessId: string;
  state: string;
  hasNexus: boolean;
  nexusReason: NexusReason;
  notes?: string;
}

/**
 * Nexus determination result
 */
export interface NexusDeterminationResult {
  businessId: string;
  state: string;
  hasNexus: boolean;
  nexusReason: NexusReason;
  determinationDate: string;
  explanation: string;
  meetsEconomicThreshold: boolean;
  economicNexusDetails?: {
    salesAmount: number;
    salesThreshold: number;
    transactionCount: number;
    transactionThreshold: number;
  };
}

/**
 * Economic nexus thresholds by state
 */
export interface EconomicNexusThreshold {
  state: string;
  salesThreshold: number;
  transactionThreshold: number;
  effectiveDate: string;
}

/**
 * Nexus change audit log
 */
export interface NexusChangeLog {
  id: string;
  businessId: string;
  state: string;
  changeType: 'CREATED' | 'UPDATED' | 'DELETED';
  oldHasNexus: boolean | null;
  newHasNexus: boolean | null;
  oldReason: NexusReason | null;
  newReason: NexusReason | null;
  changedAt: string;
  changedBy: string;
  notes: string | null;
}

/**
 * Nexus status display options
 */
export interface NexusStatusOption {
  state: string;
  stateName: string;
  hasNexus: boolean;
  nexusReason: NexusReason;
  nexusReasonLabel: string;
  lastDetermined: string;
  icon: string;
  color: string;
}

/**
 * Constants for nexus reason descriptions
 */
export const NEXUS_REASON_DESCRIPTIONS = {
  [NexusReason.PHYSICAL_PRESENCE]: {
    label: 'Physical Presence',
    description: 'Business has physical presence in the state (office, warehouse, property)',
    icon: '🏢',
    color: 'blue',
  },
  [NexusReason.EMPLOYEE_PRESENCE]: {
    label: 'Employee Presence',
    description: 'Business has employees working in the state',
    icon: '👥',
    color: 'green',
  },
  [NexusReason.ECONOMIC_NEXUS]: {
    label: 'Economic Nexus',
    description: 'Business exceeds economic nexus thresholds ($500K+ sales or 200+ transactions)',
    icon: '💰',
    color: 'purple',
  },
  [NexusReason.FACTOR_PRESENCE]: {
    label: 'Factor Presence',
    description: 'Business has significant property, payroll, or sales factor in the state',
    icon: '📊',
    color: 'orange',
  },
  [NexusReason.AFFILIATE_NEXUS]: {
    label: 'Affiliate Nexus',
    description: 'Business has nexus through affiliated or subsidiary entities',
    icon: '🔗',
    color: 'teal',
  },
  [NexusReason.CLICK_THROUGH_NEXUS]: {
    label: 'Click-Through Nexus',
    description: 'Business has nexus through referral or advertising agreements',
    icon: '🖱️',
    color: 'indigo',
  },
  [NexusReason.PL_86_272_PROTECTED]: {
    label: 'P.L. 86-272 Protected',
    description: 'Business protected by Public Law 86-272 (solicitation only)',
    icon: '🛡️',
    color: 'gray',
  },
  [NexusReason.NO_NEXUS]: {
    label: 'No Nexus',
    description: 'Business does not have nexus in the state',
    icon: '❌',
    color: 'red',
  },
};

/**
 * Common economic nexus thresholds (as of 2024)
 */
export const COMMON_ECONOMIC_NEXUS_THRESHOLDS: Record<string, EconomicNexusThreshold> = {
  OH: {
    state: 'OH',
    salesThreshold: 500000,
    transactionThreshold: 200,
    effectiveDate: '2018-01-01',
  },
  CA: {
    state: 'CA',
    salesThreshold: 500000,
    transactionThreshold: 200,
    effectiveDate: '2019-04-01',
  },
  NY: {
    state: 'NY',
    salesThreshold: 500000,
    transactionThreshold: 200,
    effectiveDate: '2019-01-01',
  },
  // Additional states can be added as needed
};

/**
 * State abbreviations to full names
 */
export const STATE_NAMES: Record<string, string> = {
  OH: 'Ohio',
  CA: 'California',
  NY: 'New York',
  TX: 'Texas',
  FL: 'Florida',
  PA: 'Pennsylvania',
  IL: 'Illinois',
  MI: 'Michigan',
  GA: 'Georgia',
  NC: 'North Carolina',
  // Additional states can be added as needed
};
