/**
 * Rule Service API Client
 * Provides methods to interact with the rule-service backend for tax rule configuration.
 */

import {
  TaxRule,
  CreateRuleRequest,
  UpdateRuleRequest,
  RuleChangeLog,
  ApprovalStatus,
  RuleCategory,
  TaxRulesConfig,
  BusinessTaxRulesConfig,
  W2QualifyingWagesRule
} from '../types';
import { safeLocalStorage } from '../utils/safeStorage';

const API_BASE = '/api/rules';

function getAuthHeaders(): HeadersInit {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${safeLocalStorage.getItem('auth_token')}`
  };
}

export interface ListRulesParams {
  tenantId?: string;
  category?: RuleCategory;
  approvalStatus?: ApprovalStatus;
  page?: number;
  size?: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  size: number;
}

export interface ActiveRulesParams {
  tenantId: string;
  taxYear: number;
  entityType?: string;
}

/**
 * Rule Service Client
 */
export const ruleService = {
  /**
   * List rules with optional filters
   */
  async listRules(params: ListRulesParams = {}): Promise<PagedResponse<TaxRule>> {
    const searchParams = new URLSearchParams();
    if (params.tenantId) searchParams.append('tenantId', params.tenantId);
    if (params.category) searchParams.append('category', params.category);
    if (params.approvalStatus) searchParams.append('approvalStatus', params.approvalStatus);
    if (params.page !== undefined) searchParams.append('page', params.page.toString());
    if (params.size !== undefined) searchParams.append('size', params.size.toString());
    
    const response = await fetch(`${API_BASE}?${searchParams.toString()}`, {
      headers: getAuthHeaders()
    });
    
    if (!response.ok) {
      throw new Error(`Failed to list rules: ${response.status}`);
    }
    
    return response.json();
  },

  /**
   * Get a single rule by ID
   */
  async getRule(ruleId: string): Promise<TaxRule> {
    const response = await fetch(`${API_BASE}/${ruleId}`, {
      headers: getAuthHeaders()
    });
    
    if (!response.ok) {
      throw new Error(`Failed to get rule: ${response.status}`);
    }
    
    return response.json();
  },

  /**
   * Create a new tax rule
   */
  async createRule(request: CreateRuleRequest): Promise<TaxRule> {
    const response = await fetch(API_BASE, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(request)
    });
    
    if (!response.ok) {
      throw new Error(`Failed to create rule: ${response.status}`);
    }
    
    return response.json();
  },

  /**
   * Update an existing rule
   */
  async updateRule(ruleId: string, request: UpdateRuleRequest): Promise<TaxRule> {
    const response = await fetch(`${API_BASE}/${ruleId}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(request)
    });
    
    if (!response.ok) {
      throw new Error(`Failed to update rule: ${response.status}`);
    }
    
    return response.json();
  },

  /**
   * Approve a pending rule
   * @param ruleId Rule ID to approve
   * @param approverId User ID of the person approving the rule
   */
  async approveRule(ruleId: string, approverId: string): Promise<TaxRule> {
    const response = await fetch(
      `${API_BASE}/${ruleId}/approve?approverId=${encodeURIComponent(approverId)}`,
      {
        method: 'POST',
        headers: getAuthHeaders()
      }
    );
    
    if (!response.ok) {
      throw new Error(`Failed to approve rule: ${response.status}`);
    }
    
    return response.json();
  },

  /**
   * Reject a pending rule
   * @param ruleId Rule ID to reject
   * @param reason Rejection reason
   */
  async rejectRule(ruleId: string, reason: string): Promise<TaxRule> {
    const response = await fetch(
      `${API_BASE}/${ruleId}/reject?reason=${encodeURIComponent(reason)}`,
      {
        method: 'POST',
        headers: getAuthHeaders()
      }
    );
    
    if (!response.ok) {
      throw new Error(`Failed to reject rule: ${response.status}`);
    }
    
    return response.json();
  },

  /**
   * Void a rule (soft delete)
   */
  async voidRule(ruleId: string, voidReason: string): Promise<TaxRule> {
    const response = await fetch(`${API_BASE}/${ruleId}/void`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ voidReason })
    });
    
    if (!response.ok) {
      throw new Error(`Failed to void rule: ${response.status}`);
    }
    
    return response.json();
  },

  /**
   * Get active rules for tax calculations
   * This is the primary method used by tax calculators
   */
  async getActiveRules(params: ActiveRulesParams): Promise<TaxRule[]> {
    const searchParams = new URLSearchParams();
    searchParams.append('tenantId', params.tenantId);
    searchParams.append('taxYear', params.taxYear.toString());
    if (params.entityType) searchParams.append('entityType', params.entityType);
    
    const response = await fetch(`${API_BASE}/active?${searchParams.toString()}`, {
      headers: getAuthHeaders()
    });
    
    if (!response.ok) {
      throw new Error(`Failed to get active rules: ${response.status}`);
    }
    
    return response.json();
  },

  /**
   * Get change history for a rule
   */
  async getRuleHistory(ruleId: string): Promise<RuleChangeLog[]> {
    const response = await fetch(`${API_BASE}/${ruleId}/history`, {
      headers: getAuthHeaders()
    });
    
    if (!response.ok) {
      throw new Error(`Failed to get rule history: ${response.status}`);
    }
    
    return response.json();
  },

  /**
   * Get pending rules (for approval workflow)
   */
  async getPendingRules(): Promise<TaxRule[]> {
    return (await this.listRules({ approvalStatus: 'PENDING' })).content;
  },

  /**
   * Get rules by tenant
   */
  async getRulesByTenant(tenantId: string, page: number = 0, size: number = 50): Promise<PagedResponse<TaxRule>> {
    return await this.listRules({ tenantId, page, size });
  },

  /**
   * Get rules by category
   */
  async getRulesByCategory(category: RuleCategory, page: number = 0, size: number = 50): Promise<PagedResponse<TaxRule>> {
    return await this.listRules({ category, page, size });
  }
};

/**
 * Transform an array of TaxRule objects into the TaxRulesConfig format
 * used by the tax calculator. This bridges the rule-service data with the
 * calculation logic.
 * 
 * Note: Locality rate codes follow the format LOCALITY_RATE_{CITY_NAME} where
 * city names use underscores for spaces (e.g., LOCALITY_RATE_UPPER_ARLINGTON).
 */

// Helper type for extracting values from rule values
interface RuleValueWithScalar {
  scalar?: number;
  unit?: string;
}

interface RuleValueWithFlag {
  flag?: boolean;
}

interface RuleValueWithOption {
  option?: string;
  allowedValues?: string[];
}

type ParsedRuleValue = RuleValueWithScalar | RuleValueWithFlag | RuleValueWithOption;

// Valid W2 qualifying wages rule options
const VALID_W2_RULES = ['HIGHEST_OF_ALL', 'BOX_5_MEDICARE', 'BOX_18_LOCAL', 'BOX_1_FEDERAL'];

// Valid allocation method options
const VALID_ALLOCATION_METHODS = ['3_FACTOR', 'GROSS_RECEIPTS_ONLY'];

function isValidW2Rule(value: string): value is W2QualifyingWagesRule {
  return VALID_W2_RULES.includes(value);
}

function isValidAllocationMethod(value: string): value is '3_FACTOR' | 'GROSS_RECEIPTS_ONLY' {
  return VALID_ALLOCATION_METHODS.includes(value);
}

export function transformRulesToConfig(rules: TaxRule[]): {
  taxRules: TaxRulesConfig;
  businessRules: BusinessTaxRulesConfig;
} {
  // Start with defaults
  const taxRules: TaxRulesConfig = {
    municipalRate: 0.02,
    municipalCreditLimitRate: 0.02,
    municipalRates: {},
    w2QualifyingWagesRule: W2QualifyingWagesRule.HIGHEST_OF_ALL,
    incomeInclusion: {
      scheduleC: true,
      scheduleE: true,
      scheduleF: true,
      w2g: true,
      form1099: true
    },
    enableRounding: true
  };

  const businessRules: BusinessTaxRulesConfig = {
    municipalRate: 0.02,
    minimumTax: 0,
    allocationMethod: '3_FACTOR',
    allocationSalesFactorWeight: 1,
    enableNOL: true,
    nolOffsetCapPercent: 0.50,
    intangibleExpenseRate: 0.05,
    safeHarborPercent: 0.90,
    penaltyRateLateFiling: 25.00,
    penaltyRateUnderpayment: 0.15,
    interestRateAnnual: 0.07
  };

  // Process each rule and map to config values
  for (const rule of rules) {
    if (rule.approvalStatus !== 'APPROVED') continue;

    const value = rule.value as ParsedRuleValue;
    const numericValue = (value as RuleValueWithScalar)?.scalar ?? 
                         ((value as RuleValueWithFlag)?.flag ? 1 : 0);

    switch (rule.ruleCode) {
      // Individual tax rates
      case 'MUNICIPAL_TAX_RATE':
        taxRules.municipalRate = numericValue / 100;
        break;
      case 'MUNICIPAL_CREDIT_LIMIT_RATE':
        taxRules.municipalCreditLimitRate = numericValue / 100;
        break;
      
      // W2 Qualifying Wages Rule
      case 'W2_QUALIFYING_WAGES_RULE': {
        const optionValue = (value as RuleValueWithOption)?.option;
        if (optionValue && isValidW2Rule(optionValue)) {
          taxRules.w2QualifyingWagesRule = optionValue;
        }
        break;
      }

      // Income inclusion rules
      case 'INCLUDE_SCHEDULE_C':
        taxRules.incomeInclusion.scheduleC = (value as RuleValueWithFlag)?.flag ?? true;
        break;
      case 'INCLUDE_SCHEDULE_E':
        taxRules.incomeInclusion.scheduleE = (value as RuleValueWithFlag)?.flag ?? true;
        break;
      case 'INCLUDE_SCHEDULE_F':
        taxRules.incomeInclusion.scheduleF = (value as RuleValueWithFlag)?.flag ?? true;
        break;
      case 'INCLUDE_W2G':
        taxRules.incomeInclusion.w2g = (value as RuleValueWithFlag)?.flag ?? true;
        break;
      case 'INCLUDE_1099':
        taxRules.incomeInclusion.form1099 = (value as RuleValueWithFlag)?.flag ?? true;
        break;

      // Rounding
      case 'ENABLE_ROUNDING':
        taxRules.enableRounding = (value as RuleValueWithFlag)?.flag ?? true;
        break;

      // Locality rates (for Schedule Y credits)
      // Rule codes follow format: LOCALITY_RATE_{CITY_NAME} where underscores replace spaces
      default:
        if (rule.ruleCode.startsWith('LOCALITY_RATE_')) {
          const cityCode = rule.ruleCode.replace('LOCALITY_RATE_', '').toLowerCase().replace(/_/g, ' ');
          const rate = ((value as RuleValueWithScalar)?.scalar ?? 0) / 100;
          taxRules.municipalRates[cityCode] = rate;
        }
        break;
    }

    // Business rules
    switch (rule.ruleCode) {
      case 'BUSINESS_MUNICIPAL_TAX_RATE':
        businessRules.municipalRate = numericValue / 100;
        break;
      case 'MINIMUM_TAX':
        businessRules.minimumTax = numericValue;
        break;
      case 'ALLOCATION_METHOD': {
        const optionValue = (value as RuleValueWithOption)?.option;
        if (optionValue && isValidAllocationMethod(optionValue)) {
          businessRules.allocationMethod = optionValue;
        }
        break;
      }
      case 'ALLOCATION_SALES_FACTOR_WEIGHT':
        businessRules.allocationSalesFactorWeight = numericValue;
        break;
      case 'ENABLE_NOL':
        businessRules.enableNOL = (value as RuleValueWithFlag)?.flag ?? true;
        break;
      case 'NOL_OFFSET_CAP_PERCENT':
        businessRules.nolOffsetCapPercent = numericValue / 100;
        break;
      case 'INTANGIBLE_EXPENSE_RATE':
        businessRules.intangibleExpenseRate = numericValue / 100;
        break;
      case 'SAFE_HARBOR_PERCENT':
        businessRules.safeHarborPercent = numericValue / 100;
        break;
      case 'PENALTY_RATE_LATE_FILING':
        businessRules.penaltyRateLateFiling = numericValue;
        break;
      case 'PENALTY_RATE_UNDERPAYMENT':
        businessRules.penaltyRateUnderpayment = numericValue / 100;
        break;
      case 'INTEREST_RATE_ANNUAL':
        businessRules.interestRateAnnual = numericValue / 100;
        break;
    }
  }

  return { taxRules, businessRules };
}

export default ruleService;
