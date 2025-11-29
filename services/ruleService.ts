/**
 * Rule Service API Client
 * Provides methods to interact with the rule-service backend for tax rule configuration.
 */

import axios, { AxiosResponse } from 'axios';
import {
  TaxRule,
  CreateRuleRequest,
  UpdateRuleRequest,
  RuleChangeLog,
  ApprovalStatus,
  RuleCategory
} from '../types';

const API_BASE = '/api/rules';

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
    const response: AxiosResponse<PagedResponse<TaxRule>> = await axios.get(API_BASE, { params });
    return response.data;
  },

  /**
   * Get a single rule by ID
   */
  async getRule(ruleId: string): Promise<TaxRule> {
    const response: AxiosResponse<TaxRule> = await axios.get(`${API_BASE}/${ruleId}`);
    return response.data;
  },

  /**
   * Create a new tax rule
   */
  async createRule(request: CreateRuleRequest): Promise<TaxRule> {
    const response: AxiosResponse<TaxRule> = await axios.post(API_BASE, request);
    return response.data;
  },

  /**
   * Update an existing rule
   */
  async updateRule(ruleId: string, request: UpdateRuleRequest): Promise<TaxRule> {
    const response: AxiosResponse<TaxRule> = await axios.put(`${API_BASE}/${ruleId}`, request);
    return response.data;
  },

  /**
   * Approve a pending rule
   */
  async approveRule(ruleId: string, approvalReason: string): Promise<TaxRule> {
    const response: AxiosResponse<TaxRule> = await axios.post(`${API_BASE}/${ruleId}/approve`, {
      approvalReason
    });
    return response.data;
  },

  /**
   * Reject a pending rule
   */
  async rejectRule(ruleId: string, rejectionReason: string): Promise<TaxRule> {
    const response: AxiosResponse<TaxRule> = await axios.post(`${API_BASE}/${ruleId}/reject`, {
      rejectionReason
    });
    return response.data;
  },

  /**
   * Void a rule (soft delete)
   */
  async voidRule(ruleId: string, voidReason: string): Promise<TaxRule> {
    const response: AxiosResponse<TaxRule> = await axios.post(`${API_BASE}/${ruleId}/void`, {
      voidReason
    });
    return response.data;
  },

  /**
   * Get active rules for tax calculations
   * This is the primary method used by tax calculators
   */
  async getActiveRules(params: ActiveRulesParams): Promise<TaxRule[]> {
    const response: AxiosResponse<TaxRule[]> = await axios.get(`${API_BASE}/active`, { params });
    return response.data;
  },

  /**
   * Get change history for a rule
   */
  async getRuleHistory(ruleId: string): Promise<RuleChangeLog[]> {
    const response: AxiosResponse<RuleChangeLog[]> = await axios.get(`${API_BASE}/${ruleId}/history`);
    return response.data;
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

export default ruleService;
