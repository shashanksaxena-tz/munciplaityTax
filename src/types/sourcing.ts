/**
 * Frontend types for sourcing method elections and rules.
 * Defines elections for Finnigan/Joyce, throwback/throwout, and service sourcing.
 */

/**
 * Sourcing method election for affiliated group sales
 */
export enum SourcingMethodElection {
  /** Include only sales of entities with nexus in denominator (separate accounting) */
  JOYCE = 'JOYCE',
  
  /** Include all affiliated group sales in denominator regardless of nexus (combined reporting) */
  FINNIGAN = 'FINNIGAN',
}

/**
 * Throwback rule election for sales to no-nexus states
 */
export enum ThrowbackElection {
  /** Apply throwback - sales to no-nexus states thrown back to origin state */
  THROWBACK = 'THROWBACK',
  
  /** Apply throwout - sales to no-nexus states excluded from denominator */
  THROWOUT = 'THROWOUT',
  
  /** No special treatment - sales to no-nexus states included in denominator only */
  NONE = 'NONE',
}

/**
 * Service sourcing method election
 */
export enum ServiceSourcingMethod {
  /** Source service revenue to where customer receives benefit (market-based) */
  MARKET_BASED = 'MARKET_BASED',
  
  /** Source service revenue to where work is performed (cost-of-performance) */
  COST_OF_PERFORMANCE = 'COST_OF_PERFORMANCE',
  
  /** Pro-rata sourcing based on state proportions */
  PRO_RATA = 'PRO_RATA',
}

/**
 * Sourcing method options with descriptions
 */
export interface SourcingMethodOption {
  value: SourcingMethodElection;
  label: string;
  description: string;
  example: string;
}

/**
 * Throwback election options with descriptions
 */
export interface ThrowbackElectionOption {
  value: ThrowbackElection;
  label: string;
  description: string;
  example: string;
}

/**
 * Service sourcing method options with descriptions
 */
export interface ServiceSourcingMethodOption {
  value: ServiceSourcingMethod;
  label: string;
  description: string;
  example: string;
}

/**
 * Sourcing method election comparison
 */
export interface SourcingMethodComparison {
  joyceDenominator: number;
  finniganDenominator: number;
  joyceApportionment: number;
  finniganApportionment: number;
  difference: number;
  recommendation: SourcingMethodElection;
  explanation: string;
}

/**
 * Throwback calculation details
 */
export interface ThrowbackCalculation {
  destinationState: string;
  hasNexus: boolean;
  saleAmount: number;
  throwbackApplied: boolean;
  throwbackAmount: number;
  explanation: string;
}

/**
 * Service sourcing calculation details
 */
export interface ServiceSourcingCalculation {
  transactionId: string;
  serviceDescription: string;
  amount: number;
  sourcingMethod: ServiceSourcingMethod;
  customerLocation: string | null;
  workPerformedLocation: string | null;
  sourcedState: string;
  explanation: string;
}

/**
 * Sourcing election change audit
 */
export interface SourcingElectionChange {
  id: string;
  scheduleYId: string;
  electionType: 'SOURCING_METHOD' | 'THROWBACK' | 'SERVICE_SOURCING';
  oldValue: string;
  newValue: string;
  changedAt: string;
  changedBy: string;
  reason: string | null;
}

/**
 * Constants for sourcing method descriptions
 */
export const SOURCING_METHOD_DESCRIPTIONS = {
  [SourcingMethodElection.JOYCE]: {
    label: 'Joyce (Separate Accounting)',
    description: 'Include only sales of entities with Ohio nexus in the denominator',
    example: 'Parent has OH nexus ($5M sales), Subsidiary has no OH nexus ($3M sales) → Denominator = $5M',
  },
  [SourcingMethodElection.FINNIGAN]: {
    label: 'Finnigan (Combined Reporting)',
    description: 'Include all affiliated group sales in the denominator regardless of nexus',
    example: 'Parent has OH nexus ($5M sales), Subsidiary has no OH nexus ($3M sales) → Denominator = $8M',
  },
};

export const THROWBACK_ELECTION_DESCRIPTIONS = {
  [ThrowbackElection.THROWBACK]: {
    label: 'Throwback',
    description: 'Sales to no-nexus states are thrown back to the origin state',
    example: 'Ship $100K goods from OH to CA (no CA nexus) → $100K added to OH numerator',
  },
  [ThrowbackElection.THROWOUT]: {
    label: 'Throwout',
    description: 'Sales to no-nexus states are excluded from the denominator',
    example: 'Ship $100K goods from OH to CA (no CA nexus) → $100K removed from denominator',
  },
  [ThrowbackElection.NONE]: {
    label: 'None',
    description: 'Sales to no-nexus states included in denominator only',
    example: 'Ship $100K goods from OH to CA (no CA nexus) → $100K in denominator, $0 in OH numerator',
  },
};

export const SERVICE_SOURCING_DESCRIPTIONS = {
  [ServiceSourcingMethod.MARKET_BASED]: {
    label: 'Market-Based Sourcing',
    description: 'Source service revenue to where the customer receives the benefit',
    example: 'IT consulting: OH office performs work for NY customer → 100% sourced to NY',
  },
  [ServiceSourcingMethod.COST_OF_PERFORMANCE]: {
    label: 'Cost-of-Performance',
    description: 'Source service revenue to where the work is performed',
    example: 'IT consulting: OH office (70% payroll), CA office (30% payroll) → 70% OH, 30% CA',
  },
  [ServiceSourcingMethod.PRO_RATA]: {
    label: 'Pro-Rata',
    description: 'Source service revenue proportionally based on state factors',
    example: 'Service revenue split based on property, payroll, or sales factors',
  },
};
