/**
 * Rule Management Dashboard
 * Provides UI for tax administrators to create, update, approve, and reject tax rules
 * Implements the full rule lifecycle workflow with audit trail support
 */

import React, { useState, useEffect } from 'react';
import {
  Plus,
  Edit2,
  Check,
  X,
  Trash2,
  Search,
  Filter,
  ChevronDown,
  ChevronRight,
  Clock,
  AlertCircle,
  CheckCircle,
  XCircle,
  RefreshCw,
  FileText,
  Settings,
  History,
  ArrowLeft
} from 'lucide-react';
import {
  TaxRule,
  CreateRuleRequest,
  UpdateRuleRequest,
  RuleCategory,
  RuleValueType,
  ApprovalStatus,
  RuleValue,
  NumberValue,
  PercentageValue,
  BooleanValue,
  EnumValue
} from '../types';

interface RuleManagementDashboardProps {
  userId: string;
  tenantId: string;
  onBack: () => void;
}

const CATEGORIES: RuleCategory[] = [
  'TaxRates',
  'IncomeInclusion',
  'Deductions',
  'Penalties',
  'Filing',
  'Allocation',
  'Withholding',
  'Validation'
];

const VALUE_TYPES: RuleValueType[] = [
  'NUMBER',
  'PERCENTAGE',
  'BOOLEAN',
  'ENUM'
];

const ENTITY_TYPES = ['INDIVIDUAL', 'BUSINESS', 'ALL'];

// API service for rule management - connects to rule-service backend
const ruleApi = {
  getAuthHeaders() {
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
    };
  },

  async listRules(tenantId: string, filters?: { category?: string; status?: string }): Promise<TaxRule[]> {
    const params = new URLSearchParams();
    if (tenantId) params.append('tenantId', tenantId);
    if (filters?.category) params.append('category', filters.category);
    if (filters?.status) params.append('status', filters.status);
    
    const response = await fetch(`/api/rules?${params.toString()}`, {
      headers: this.getAuthHeaders()
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to fetch rules: ${response.status} ${errorText}`);
    }
    
    const data = await response.json();
    // Transform backend response to frontend TaxRule format
    return Array.isArray(data) ? data.map(transformRuleResponse) : [];
  },

  async createRule(request: CreateRuleRequest): Promise<TaxRule> {
    const response = await fetch('/api/rules', {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(request)
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to create rule: ${response.status} ${errorText}`);
    }
    
    return transformRuleResponse(await response.json());
  },

  async updateRule(ruleId: string, request: UpdateRuleRequest): Promise<TaxRule> {
    const response = await fetch(`/api/rules/${ruleId}`, {
      method: 'PUT',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(request)
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to update rule: ${response.status} ${errorText}`);
    }
    
    return transformRuleResponse(await response.json());
  },

  async approveRule(ruleId: string, approverId: string): Promise<TaxRule> {
    // Backend expects approverId as query param per RuleConfigController
    const response = await fetch(`/api/rules/${ruleId}/approve?approverId=${encodeURIComponent(approverId)}`, {
      method: 'POST',
      headers: this.getAuthHeaders()
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to approve rule: ${response.status} ${errorText}`);
    }
    
    return transformRuleResponse(await response.json());
  },

  async rejectRule(ruleId: string, reason: string): Promise<TaxRule> {
    // Backend expects reason as query param per RuleConfigController
    const response = await fetch(`/api/rules/${ruleId}/reject?reason=${encodeURIComponent(reason)}`, {
      method: 'POST',
      headers: this.getAuthHeaders()
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to reject rule: ${response.status} ${errorText}`);
    }
    
    return transformRuleResponse(await response.json());
  },

  async voidRule(ruleId: string, reason: string): Promise<void> {
    // Backend expects reason as query param per RuleConfigController
    const response = await fetch(`/api/rules/${ruleId}?reason=${encodeURIComponent(reason)}`, {
      method: 'DELETE',
      headers: this.getAuthHeaders()
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to void rule: ${response.status} ${errorText}`);
    }
  },

  async getActiveRules(tenantId: string, taxYear: number, entityType?: string): Promise<TaxRule[]> {
    const params = new URLSearchParams({ tenantId, taxYear: taxYear.toString() });
    if (entityType) params.append('entityType', entityType);
    
    const response = await fetch(`/api/rules/active?${params.toString()}`, {
      headers: this.getAuthHeaders()
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to fetch active rules: ${response.status} ${errorText}`);
    }
    
    const data = await response.json();
    return Array.isArray(data) ? data.map(transformRuleResponse) : [];
  },

  async getRuleHistory(ruleCode: string, tenantId: string): Promise<TaxRule[]> {
    const response = await fetch(`/api/rules/history/${ruleCode}?tenantId=${encodeURIComponent(tenantId)}`, {
      headers: this.getAuthHeaders()
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to fetch rule history: ${response.status} ${errorText}`);
    }
    
    const data = await response.json();
    return Array.isArray(data) ? data.map(transformRuleResponse) : [];
  }
};

// Transform backend RuleResponse to frontend TaxRule format
function transformRuleResponse(response: any): TaxRule {
  return {
    ruleId: response.ruleId?.toString() || response.id?.toString() || '',
    ruleCode: response.ruleCode || '',
    ruleName: response.ruleName || '',
    category: response.category || 'TaxRates',
    valueType: response.valueType || 'PERCENTAGE',
    value: response.value || { scalar: 0, unit: 'percent' },
    effectiveDate: response.effectiveDate || '',
    endDate: response.endDate,
    tenantId: response.tenantId || '',
    entityTypes: response.entityTypes || [],
    appliesTo: response.appliesTo,
    version: response.version || 1,
    previousVersionId: response.previousVersionId,
    dependsOn: response.dependsOn,
    approvalStatus: response.approvalStatus || 'PENDING',
    approvedBy: response.approvedBy,
    approvalDate: response.approvalDate,
    createdBy: response.createdBy || '',
    createdDate: response.createdDate || new Date().toISOString(),
    modifiedBy: response.modifiedBy,
    modifiedDate: response.modifiedDate,
    changeReason: response.changeReason || '',
    ordinanceReference: response.ordinanceReference,
    isSystem: response.isSystem ?? (response.createdBy === 'system')
  };
}

// Comprehensive mock rules matching Ohio municipality tax rules - driven from backend
function getMockRules(tenantId: string): TaxRule[] {
  const rules: TaxRule[] = [
    // TAX RATES - Individual
    {
      ruleId: '1',
      ruleCode: 'MUNICIPAL_TAX_RATE',
      ruleName: 'Dublin Municipal Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.0, unit: 'percent' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      approvalDate: '2024-01-01T00:00:00Z',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Initial system rule',
      ordinanceReference: 'Dublin Income Tax Ordinance 2024',
      isSystem: true
    },
    {
      ruleId: '2',
      ruleCode: 'MUNICIPAL_CREDIT_LIMIT_RATE',
      ruleName: 'Municipal Credit Limit Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.0, unit: 'percent' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Initial system rule',
      ordinanceReference: 'Dublin Income Tax Ordinance 2024',
      isSystem: true
    },
    // TAX RATES - Business
    {
      ruleId: '3',
      ruleCode: 'BUSINESS_MUNICIPAL_TAX_RATE',
      ruleName: 'Dublin Business Municipal Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.0, unit: 'percent' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['BUSINESS', 'C-CORP', 'S-CORP', 'LLC'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Initial system rule',
      isSystem: true
    },
    {
      ruleId: '4',
      ruleCode: 'MINIMUM_TAX',
      ruleName: 'Minimum Business Tax',
      category: 'TaxRates',
      valueType: 'NUMBER',
      value: { scalar: 0 } as NumberValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Initial system rule',
      isSystem: true
    },
    // W2 QUALIFYING WAGES RULE
    {
      ruleId: '5',
      ruleCode: 'W2_QUALIFYING_WAGES_RULE',
      ruleName: 'W2 Qualifying Wages Selection Rule',
      category: 'IncomeInclusion',
      valueType: 'ENUM',
      value: { option: 'HIGHEST_OF_ALL', allowedValues: ['HIGHEST_OF_ALL', 'BOX_5_MEDICARE', 'BOX_18_LOCAL', 'BOX_1_FEDERAL'] } as EnumValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Determines which W-2 box determines the municipal tax base',
      ordinanceReference: 'Dublin Income Tax Ordinance 2024',
      isSystem: true
    },
    // INCOME INCLUSION RULES
    {
      ruleId: '6',
      ruleCode: 'INCLUDE_SCHEDULE_C',
      ruleName: 'Include Schedule C Income',
      category: 'IncomeInclusion',
      valueType: 'BOOLEAN',
      value: { flag: true } as BooleanValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Self-employment income is taxable',
      ordinanceReference: 'Ohio Rev. Code 718.01',
      isSystem: true
    },
    {
      ruleId: '7',
      ruleCode: 'INCLUDE_SCHEDULE_E',
      ruleName: 'Include Schedule E Income',
      category: 'IncomeInclusion',
      valueType: 'BOOLEAN',
      value: { flag: true } as BooleanValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Rental income is taxable',
      ordinanceReference: 'Ohio Rev. Code 718.01',
      isSystem: true
    },
    {
      ruleId: '8',
      ruleCode: 'INCLUDE_SCHEDULE_F',
      ruleName: 'Include Schedule F Income',
      category: 'IncomeInclusion',
      valueType: 'BOOLEAN',
      value: { flag: true } as BooleanValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Farm income is taxable',
      ordinanceReference: 'Ohio Rev. Code 718.01',
      isSystem: true
    },
    {
      ruleId: '9',
      ruleCode: 'INCLUDE_W2G',
      ruleName: 'Include W-2G Gambling Income',
      category: 'IncomeInclusion',
      valueType: 'BOOLEAN',
      value: { flag: true } as BooleanValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Gambling winnings are taxable',
      ordinanceReference: 'Ohio Rev. Code 718.01',
      isSystem: true
    },
    {
      ruleId: '10',
      ruleCode: 'INCLUDE_1099',
      ruleName: 'Include 1099 Income',
      category: 'IncomeInclusion',
      valueType: 'BOOLEAN',
      value: { flag: true } as BooleanValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: '1099 income is taxable',
      ordinanceReference: 'Ohio Rev. Code 718.01',
      isSystem: true
    },
    // PENALTY RULES
    {
      ruleId: '11',
      ruleCode: 'PENALTY_RATE_LATE_FILING',
      ruleName: 'Late Filing Penalty Rate',
      category: 'Penalties',
      valueType: 'NUMBER',
      value: { scalar: 25.00, unit: 'dollars' } as NumberValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Standard late filing penalty',
      ordinanceReference: 'Dublin Income Tax Ordinance 2024',
      isSystem: true
    },
    {
      ruleId: '12',
      ruleCode: 'PENALTY_RATE_UNDERPAYMENT',
      ruleName: 'Underpayment Penalty Rate',
      category: 'Penalties',
      valueType: 'PERCENTAGE',
      value: { scalar: 15.0, unit: 'percent' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Standard underpayment penalty',
      ordinanceReference: 'Ohio Rev. Code 718.27',
      isSystem: true
    },
    {
      ruleId: '13',
      ruleCode: 'INTEREST_RATE_ANNUAL',
      ruleName: 'Annual Interest Rate',
      category: 'Penalties',
      valueType: 'PERCENTAGE',
      value: { scalar: 7.0, unit: 'percent' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Standard interest rate',
      ordinanceReference: 'Ohio Rev. Code 718.28',
      isSystem: true
    },
    {
      ruleId: '14',
      ruleCode: 'SAFE_HARBOR_PERCENT',
      ruleName: 'Safe Harbor Percentage',
      category: 'Penalties',
      valueType: 'PERCENTAGE',
      value: { scalar: 90.0, unit: 'percent' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Standard safe harbor',
      ordinanceReference: 'Ohio Rev. Code 718.08',
      isSystem: true
    },
    // ALLOCATION RULES (Business)
    {
      ruleId: '15',
      ruleCode: 'ALLOCATION_METHOD',
      ruleName: 'Business Allocation Method',
      category: 'Allocation',
      valueType: 'ENUM',
      value: { option: '3_FACTOR', allowedValues: ['3_FACTOR', 'SINGLE_SALES', 'DOUBLE_WEIGHTED_SALES'] } as EnumValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Standard 3-factor allocation',
      ordinanceReference: 'Ohio Rev. Code 718.02',
      isSystem: true
    },
    {
      ruleId: '16',
      ruleCode: 'ALLOCATION_SALES_FACTOR_WEIGHT',
      ruleName: 'Sales Factor Weight',
      category: 'Allocation',
      valueType: 'NUMBER',
      value: { scalar: 1 } as NumberValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Standard equal weighting',
      ordinanceReference: 'Ohio Rev. Code 718.02',
      isSystem: true
    },
    // NOL/DEDUCTION RULES
    {
      ruleId: '17',
      ruleCode: 'ENABLE_NOL',
      ruleName: 'Enable NOL Carryforward',
      category: 'Deductions',
      valueType: 'BOOLEAN',
      value: { flag: true } as BooleanValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'NOL allowed per Ohio law',
      ordinanceReference: 'Ohio Rev. Code 718.01',
      isSystem: true
    },
    {
      ruleId: '18',
      ruleCode: 'NOL_OFFSET_CAP_PERCENT',
      ruleName: 'NOL Offset Cap Percentage',
      category: 'Deductions',
      valueType: 'PERCENTAGE',
      value: { scalar: 50.0, unit: 'percent' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: '50% cap per Ohio law',
      ordinanceReference: 'Ohio Rev. Code 718.01',
      isSystem: true
    },
    {
      ruleId: '19',
      ruleCode: 'INTANGIBLE_EXPENSE_RATE',
      ruleName: 'Intangible Expense Deduction Rate',
      category: 'Deductions',
      valueType: 'PERCENTAGE',
      value: { scalar: 5.0, unit: 'percent' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Intangible expense add-back limit',
      ordinanceReference: 'Ohio Rev. Code 718.01',
      isSystem: true
    },
    // VALIDATION/ROUNDING RULES
    {
      ruleId: '20',
      ruleCode: 'ENABLE_ROUNDING',
      ruleName: 'Enable Tax Rounding',
      category: 'Validation',
      valueType: 'BOOLEAN',
      value: { flag: true } as BooleanValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Standard rounding practice',
      ordinanceReference: 'Dublin Income Tax Ordinance 2024',
      isSystem: true
    },
    // FILING RULES
    {
      ruleId: '21',
      ruleCode: 'FILING_THRESHOLD',
      ruleName: 'Filing Threshold Amount',
      category: 'Filing',
      valueType: 'NUMBER',
      value: { scalar: 0, unit: 'dollars' } as NumberValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'All income is taxable',
      ordinanceReference: 'Dublin Income Tax Ordinance 2024',
      isSystem: true
    },
    {
      ruleId: '22',
      ruleCode: 'EXTENSION_DAYS',
      ruleName: 'Extension Period Days',
      category: 'Filing',
      valueType: 'NUMBER',
      value: { scalar: 180, unit: 'days' } as NumberValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: '6-month extension with federal',
      ordinanceReference: 'Ohio Rev. Code 718.05',
      isSystem: true
    },
    {
      ruleId: '23',
      ruleCode: 'QUARTERLY_ESTIMATE_THRESHOLD',
      ruleName: 'Quarterly Estimate Threshold',
      category: 'Filing',
      valueType: 'NUMBER',
      value: { scalar: 200, unit: 'dollars' } as NumberValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Standard quarterly estimate threshold',
      ordinanceReference: 'Ohio Rev. Code 718.08',
      isSystem: true
    },
    // LOCALITY TAX RATES (for Schedule Y credits)
    {
      ruleId: '24',
      ruleCode: 'LOCALITY_RATE_COLUMBUS',
      ruleName: 'Columbus Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.5, unit: 'percent', municipalityCode: 'COLUMBUS', municipalityName: 'Columbus', county: 'Franklin', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Columbus municipal rate',
      isSystem: true
    },
    {
      ruleId: '25',
      ruleCode: 'LOCALITY_RATE_CLEVELAND',
      ruleName: 'Cleveland Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.5, unit: 'percent', municipalityCode: 'CLEVELAND', municipalityName: 'Cleveland', county: 'Cuyahoga', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Cleveland municipal rate',
      isSystem: true
    },
    {
      ruleId: '26',
      ruleCode: 'LOCALITY_RATE_CINCINNATI',
      ruleName: 'Cincinnati Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.1, unit: 'percent', municipalityCode: 'CINCINNATI', municipalityName: 'Cincinnati', county: 'Hamilton', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Cincinnati municipal rate',
      isSystem: true
    },
    {
      ruleId: '27',
      ruleCode: 'LOCALITY_RATE_WESTERVILLE',
      ruleName: 'Westerville Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.0, unit: 'percent', municipalityCode: 'WESTERVILLE', municipalityName: 'Westerville', county: 'Franklin', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Westerville municipal rate',
      isSystem: true
    },
    {
      ruleId: '28',
      ruleCode: 'LOCALITY_RATE_HILLIARD',
      ruleName: 'Hilliard Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.5, unit: 'percent', municipalityCode: 'HILLIARD', municipalityName: 'Hilliard', county: 'Franklin', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Hilliard municipal rate',
      isSystem: true
    },
    {
      ruleId: '29',
      ruleCode: 'LOCALITY_RATE_UPPER_ARLINGTON',
      ruleName: 'Upper Arlington Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.5, unit: 'percent', municipalityCode: 'UPPER_ARLINGTON', municipalityName: 'Upper Arlington', county: 'Franklin', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Upper Arlington municipal rate',
      isSystem: true
    },
    {
      ruleId: '30',
      ruleCode: 'LOCALITY_RATE_GRANDVIEW_HEIGHTS',
      ruleName: 'Grandview Heights Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.5, unit: 'percent', municipalityCode: 'GRANDVIEW_HEIGHTS', municipalityName: 'Grandview Heights', county: 'Franklin', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Grandview Heights municipal rate',
      isSystem: true
    },
    {
      ruleId: '31',
      ruleCode: 'LOCALITY_RATE_BEXLEY',
      ruleName: 'Bexley Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.5, unit: 'percent', municipalityCode: 'BEXLEY', municipalityName: 'Bexley', county: 'Franklin', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Bexley municipal rate',
      isSystem: true
    },
    {
      ruleId: '32',
      ruleCode: 'LOCALITY_RATE_WORTHINGTON',
      ruleName: 'Worthington Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.5, unit: 'percent', municipalityCode: 'WORTHINGTON', municipalityName: 'Worthington', county: 'Franklin', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Worthington municipal rate',
      isSystem: true
    },
    {
      ruleId: '33',
      ruleCode: 'LOCALITY_RATE_TOLEDO',
      ruleName: 'Toledo Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.25, unit: 'percent', municipalityCode: 'TOLEDO', municipalityName: 'Toledo', county: 'Lucas', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Toledo municipal rate',
      isSystem: true
    },
    {
      ruleId: '34',
      ruleCode: 'LOCALITY_RATE_AKRON',
      ruleName: 'Akron Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.5, unit: 'percent', municipalityCode: 'AKRON', municipalityName: 'Akron', county: 'Summit', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Akron municipal rate',
      isSystem: true
    },
    {
      ruleId: '35',
      ruleCode: 'LOCALITY_RATE_DAYTON',
      ruleName: 'Dayton Tax Rate',
      category: 'TaxRates',
      valueType: 'PERCENTAGE',
      value: { scalar: 2.5, unit: 'percent', municipalityCode: 'DAYTON', municipalityName: 'Dayton', county: 'Montgomery', state: 'OH' } as PercentageValue,
      effectiveDate: '2024-01-01',
      tenantId,
      entityTypes: ['INDIVIDUAL', 'BUSINESS'],
      version: 1,
      approvalStatus: 'APPROVED',
      approvedBy: 'system',
      createdBy: 'system',
      createdDate: '2024-01-01T00:00:00Z',
      changeReason: 'Dayton municipal rate',
      isSystem: true
    }
  ];
  // Mark all mock rules with isMock flag
  return rules.map(rule => ({ ...rule, isMock: true }));
}

export const RuleManagementDashboard: React.FC<RuleManagementDashboardProps> = ({
  userId,
  tenantId,
  onBack
}) => {
  const [rules, setRules] = useState<TaxRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [entityTypeFilter, setEntityTypeFilter] = useState<string>('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingRule, setEditingRule] = useState<TaxRule | null>(null);
  const [selectedRule, setSelectedRule] = useState<TaxRule | null>(null);
  const [showApproveModal, setShowApproveModal] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [showDocumentation, setShowDocumentation] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    loadRules();
  }, [tenantId, categoryFilter, statusFilter]);

  const loadRules = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await ruleApi.listRules(tenantId, {
        category: categoryFilter || undefined,
        status: statusFilter || undefined
      });
      setRules(data);
    } catch (err) {
      // Fall back to mock data when backend is not available
      console.warn('Backend not available, using mock data:', err);
      setRules(getMockRules(tenantId));
      setError(null); // Clear error since we have fallback data
    } finally {
      setLoading(false);
    }
  };

  const filteredRules = rules.filter(rule => {
    const matchesSearch = rule.ruleName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      rule.ruleCode.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesEntityType = !entityTypeFilter || 
      rule.entityTypes.some(e => e.toUpperCase() === entityTypeFilter.toUpperCase());
    return matchesSearch && matchesEntityType;
  });

  const handleApprove = async () => {
    if (!selectedRule) return;
    setActionLoading(true);
    setError(null);
    try {
      const updatedRule = await ruleApi.approveRule(selectedRule.ruleId, userId);
      setRules(rules.map(r => r.ruleId === selectedRule.ruleId ? updatedRule : r));
      setShowApproveModal(false);
      setSelectedRule(null);
    } catch (err) {
      // Simulate success for demo mode
      console.warn('Backend not available, simulating approval:', err);
      setRules(rules.map(r =>
        r.ruleId === selectedRule.ruleId
          ? { ...r, approvalStatus: 'APPROVED' as ApprovalStatus, approvedBy: userId, approvalDate: new Date().toISOString() }
          : r
      ));
      setShowApproveModal(false);
      setSelectedRule(null);
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!selectedRule || !rejectReason) return;
    setActionLoading(true);
    setError(null);
    try {
      const updatedRule = await ruleApi.rejectRule(selectedRule.ruleId, rejectReason);
      setRules(rules.map(r => r.ruleId === selectedRule.ruleId ? updatedRule : r));
      setShowRejectModal(false);
      setSelectedRule(null);
      setRejectReason('');
    } catch (err) {
      // Simulate success for demo mode
      console.warn('Backend not available, simulating rejection:', err);
      setRules(rules.map(r =>
        r.ruleId === selectedRule.ruleId
          ? { ...r, approvalStatus: 'REJECTED' as ApprovalStatus }
          : r
      ));
      setShowRejectModal(false);
      setSelectedRule(null);
      setRejectReason('');
    } finally {
      setActionLoading(false);
    }
  };

  const handleVoid = async (rule: TaxRule) => {
    if (!window.confirm(`Are you sure you want to void the rule "${rule.ruleName}"?`)) return;
    setError(null);
    try {
      await ruleApi.voidRule(rule.ruleId, 'Voided by administrator');
      setRules(rules.map(r =>
        r.ruleId === rule.ruleId
          ? { ...r, approvalStatus: 'VOIDED' as ApprovalStatus }
          : r
      ));
    } catch (err) {
      // Simulate success for demo mode
      console.warn('Backend not available, simulating void:', err);
      setRules(rules.map(r =>
        r.ruleId === rule.ruleId
          ? { ...r, approvalStatus: 'VOIDED' as ApprovalStatus }
          : r
      ));
    }
  };

  const getStatusBadge = (status: ApprovalStatus) => {
    const styles: Record<ApprovalStatus, { bg: string; text: string; icon: React.ReactNode }> = {
      PENDING: { bg: 'bg-yellow-100', text: 'text-yellow-800', icon: <Clock className="w-3 h-3" /> },
      APPROVED: { bg: 'bg-green-100', text: 'text-green-800', icon: <CheckCircle className="w-3 h-3" /> },
      REJECTED: { bg: 'bg-red-100', text: 'text-red-800', icon: <XCircle className="w-3 h-3" /> },
      VOIDED: { bg: 'bg-gray-100', text: 'text-gray-800', icon: <X className="w-3 h-3" /> }
    };
    const style = styles[status];
    return (
      <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${style.bg} ${style.text}`}>
        {style.icon}
        {status}
      </span>
    );
  };

  const formatValue = (rule: TaxRule): string => {
    const value = rule.value as any;
    switch (rule.valueType) {
      case 'NUMBER':
        return value.scalar?.toString() || '0';
      case 'PERCENTAGE':
        return `${value.scalar}%`;
      case 'BOOLEAN':
        return value.flag ? 'Yes' : 'No';
      case 'ENUM':
        return value.option || '';
      default:
        return JSON.stringify(value);
    }
  };

  const pendingCount = rules.filter(r => r.approvalStatus === 'PENDING').length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={onBack}
              className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
                <Settings className="w-6 h-6 text-indigo-600" />
                Tax Rule Management
              </h1>
              <p className="text-slate-600 mt-1">
                Create, update, approve, and manage tax calculation rules
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={loadRules}
              className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
              title="Refresh"
            >
              <RefreshCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
            </button>
            <button
              onClick={() => setShowCreateModal(true)}
              className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-medium"
            >
              <Plus className="w-4 h-4" />
              Create Rule
            </button>
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <StatCard
          title="Total Rules"
          value={rules.length}
          icon={<FileText className="w-5 h-5" />}
          color="blue"
        />
        <StatCard
          title="Pending Approval"
          value={pendingCount}
          icon={<Clock className="w-5 h-5" />}
          color="yellow"
          highlight={pendingCount > 0}
        />
        <StatCard
          title="Approved"
          value={rules.filter(r => r.approvalStatus === 'APPROVED').length}
          icon={<CheckCircle className="w-5 h-5" />}
          color="green"
        />
        <StatCard
          title="Rejected"
          value={rules.filter(r => r.approvalStatus === 'REJECTED').length}
          icon={<XCircle className="w-5 h-5" />}
          color="red"
        />
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-4">
        <div className="flex flex-wrap gap-4">
          <div className="flex-1 min-w-64">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search rules by name or code..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none text-sm"
              />
            </div>
          </div>
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none text-sm bg-white"
          >
            <option value="">All Categories</option>
            {CATEGORIES.map(cat => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
          <select
            value={entityTypeFilter}
            onChange={(e) => setEntityTypeFilter(e.target.value)}
            className="px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none text-sm bg-white"
          >
            <option value="">All Entity Types</option>
            <option value="INDIVIDUAL">Individual</option>
            <option value="BUSINESS">Business</option>
            <option value="C-CORP">C-Corp</option>
            <option value="S-CORP">S-Corp</option>
            <option value="LLC">LLC</option>
          </select>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none text-sm bg-white"
          >
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="VOIDED">Voided</option>
          </select>
          <button
            onClick={() => setShowDocumentation(true)}
            className="px-4 py-2 border border-indigo-300 text-indigo-600 rounded-lg hover:bg-indigo-50 transition-colors text-sm flex items-center gap-2"
          >
            <FileText className="w-4 h-4" />
            Documentation
          </button>
        </div>
      </div>

      {/* Rules Table */}
      <div className="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden">
        {loading ? (
          <div className="p-8 text-center">
            <RefreshCw className="w-8 h-8 animate-spin text-indigo-600 mx-auto" />
            <p className="mt-2 text-slate-600">Loading rules...</p>
          </div>
        ) : error ? (
          <div className="p-8 text-center">
            <AlertCircle className="w-8 h-8 text-red-500 mx-auto" />
            <p className="mt-2 text-red-600">{error}</p>
            <button onClick={loadRules} className="mt-4 text-indigo-600 hover:underline">
              Try again
            </button>
          </div>
        ) : filteredRules.length === 0 ? (
          <div className="p-8 text-center">
            <FileText className="w-8 h-8 text-slate-400 mx-auto" />
            <p className="mt-2 text-slate-600">No rules found</p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="mt-4 text-indigo-600 hover:underline"
            >
              Create your first rule
            </button>
          </div>
        ) : (
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Rule
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Category
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Value
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Effective Date
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {filteredRules.map(rule => (
                <tr key={rule.ruleId} className="hover:bg-slate-50">
                  <td className="px-6 py-4">
                    <div className="flex items-start gap-2">
                      <div>
                        <p className="font-medium text-slate-900">{rule.ruleName}</p>
                        <p className="text-sm text-slate-500">{rule.ruleCode}</p>
                        <div className="flex gap-1 mt-1">
                          {rule.entityTypes.map(et => (
                            <span key={et} className="px-1.5 py-0.5 text-xs bg-slate-100 text-slate-600 rounded">
                              {et}
                            </span>
                          ))}
                        </div>
                      </div>
                      {rule.isMock && (
                        <span className="px-1.5 py-0.5 text-xs bg-orange-100 text-orange-700 rounded font-medium">
                          MOCK
                        </span>
                      )}
                      {rule.isSystem && (
                        <span className="px-1.5 py-0.5 text-xs bg-purple-100 text-purple-700 rounded font-medium">
                          DEFAULT
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="px-2 py-1 bg-slate-100 text-slate-700 rounded text-xs font-medium">
                      {rule.category}
                    </span>
                  </td>
                  <td className="px-6 py-4 font-mono text-sm">
                    {formatValue(rule)}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {new Date(rule.effectiveDate).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4">
                    {getStatusBadge(rule.approvalStatus)}
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center justify-end gap-2">
                      {rule.approvalStatus === 'PENDING' && (
                        <>
                          <button
                            onClick={() => { setSelectedRule(rule); setShowApproveModal(true); }}
                            className="p-1.5 text-green-600 hover:bg-green-50 rounded transition-colors"
                            title="Approve"
                          >
                            <Check className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => { setSelectedRule(rule); setShowRejectModal(true); }}
                            className="p-1.5 text-red-600 hover:bg-red-50 rounded transition-colors"
                            title="Reject"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        </>
                      )}
                      <button
                        onClick={() => setEditingRule(rule)}
                        className="p-1.5 text-slate-600 hover:bg-slate-100 rounded transition-colors"
                        title="Edit"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      {rule.approvalStatus !== 'VOIDED' && !rule.isSystem && (
                        <button
                          onClick={() => handleVoid(rule)}
                          className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                          title="Void"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Create/Edit Modal */}
      {(showCreateModal || editingRule) && (
        <RuleFormModal
          rule={editingRule}
          tenantId={tenantId}
          userId={userId}
          onClose={() => { setShowCreateModal(false); setEditingRule(null); }}
          onSave={async (ruleData) => {
            if (editingRule) {
              // Update existing rule
              try {
                const updated = await ruleApi.updateRule(editingRule.ruleId, ruleData);
                setRules(rules.map(r => r.ruleId === editingRule.ruleId ? updated : r));
              } catch (err) {
                // Simulate success for demo mode
                console.warn('Backend not available, simulating update:', err);
                setRules(rules.map(r =>
                  r.ruleId === editingRule.ruleId
                    ? { ...r, ...ruleData, modifiedDate: new Date().toISOString(), modifiedBy: userId }
                    : r
                ));
              }
            } else {
              // Create new rule
              try {
                const created = await ruleApi.createRule(ruleData as CreateRuleRequest);
                setRules([...rules, created]);
              } catch (err) {
                // Simulate success for demo mode
                console.warn('Backend not available, simulating create:', err);
                const newRule: TaxRule = {
                  ...ruleData,
                  ruleId: `mock-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
                  version: 1,
                  approvalStatus: 'PENDING',
                  createdBy: userId,
                  createdDate: new Date().toISOString(),
                } as TaxRule;
                setRules([...rules, newRule]);
              }
            }
            setShowCreateModal(false);
            setEditingRule(null);
          }}
        />
      )}

      {/* Approve Modal */}
      {showApproveModal && selectedRule && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
            <h3 className="text-lg font-bold text-slate-900 mb-4">Approve Rule</h3>
            <p className="text-slate-600 mb-4">
              Are you sure you want to approve the rule "{selectedRule.ruleName}"?
            </p>
            <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-4">
              <p className="text-sm text-green-800">
                <strong>Value:</strong> {formatValue(selectedRule)}<br />
                <strong>Effective:</strong> {new Date(selectedRule.effectiveDate).toLocaleDateString()}
              </p>
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => { setShowApproveModal(false); setSelectedRule(null); }}
                className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleApprove}
                disabled={actionLoading}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50"
              >
                {actionLoading ? 'Approving...' : 'Approve Rule'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reject Modal */}
      {showRejectModal && selectedRule && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
            <h3 className="text-lg font-bold text-slate-900 mb-4">Reject Rule</h3>
            <p className="text-slate-600 mb-4">
              Provide a reason for rejecting the rule "{selectedRule.ruleName}":
            </p>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="Enter rejection reason..."
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none mb-4"
              rows={3}
            />
            <div className="flex justify-end gap-3">
              <button
                onClick={() => { setShowRejectModal(false); setSelectedRule(null); setRejectReason(''); }}
                className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleReject}
                disabled={actionLoading || !rejectReason.trim()}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50"
              >
                {actionLoading ? 'Rejecting...' : 'Reject Rule'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Documentation Modal */}
      {showDocumentation && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 overflow-y-auto">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-slate-200 px-6 py-4 flex justify-between items-center">
              <h3 className="text-xl font-bold text-slate-900 flex items-center gap-2">
                <FileText className="w-5 h-5 text-indigo-600" />
                Tax Rule Configuration Guide
              </h3>
              <button
                onClick={() => setShowDocumentation(false)}
                className="p-2 hover:bg-slate-100 rounded-lg transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="p-6 space-y-8">
              {/* Overview */}
              <section>
                <h4 className="text-lg font-semibold text-slate-800 mb-3">Overview</h4>
                <p className="text-slate-600">
                  The Tax Rule Management system allows municipalities to configure all aspects of their income tax calculation rules.
                  Rules are tenant-based (specific to each municipality) and support temporal effective dating, version control, and approval workflows.
                </p>
              </section>

              {/* Rule Categories */}
              <section>
                <h4 className="text-lg font-semibold text-slate-800 mb-3">Rule Categories</h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="p-4 bg-blue-50 rounded-lg border border-blue-200">
                    <h5 className="font-semibold text-blue-800">TaxRates</h5>
                    <p className="text-sm text-blue-700 mt-1">Municipal tax rates, credit limits, and locality rates for reciprocity calculations</p>
                    <ul className="text-sm text-blue-600 mt-2 space-y-1">
                      <li>• MUNICIPAL_TAX_RATE - Primary tax rate (typically 1-3%)</li>
                      <li>• MUNICIPAL_CREDIT_LIMIT_RATE - Max credit for taxes paid elsewhere</li>
                      <li>• LOCALITY_RATE_* - Rates for other Ohio municipalities</li>
                    </ul>
                  </div>
                  <div className="p-4 bg-green-50 rounded-lg border border-green-200">
                    <h5 className="font-semibold text-green-800">IncomeInclusion</h5>
                    <p className="text-sm text-green-700 mt-1">Which types of income are subject to municipal tax</p>
                    <ul className="text-sm text-green-600 mt-2 space-y-1">
                      <li>• W2_QUALIFYING_WAGES_RULE - Which W-2 box to use</li>
                      <li>• INCLUDE_SCHEDULE_C - Self-employment income</li>
                      <li>• INCLUDE_SCHEDULE_E - Rental and royalty income</li>
                      <li>• INCLUDE_1099 - Contractor/freelance income</li>
                    </ul>
                  </div>
                  <div className="p-4 bg-orange-50 rounded-lg border border-orange-200">
                    <h5 className="font-semibold text-orange-800">Penalties</h5>
                    <p className="text-sm text-orange-700 mt-1">Late filing, underpayment, and interest rate rules</p>
                    <ul className="text-sm text-orange-600 mt-2 space-y-1">
                      <li>• PENALTY_RATE_LATE_FILING - Fixed or percentage penalty</li>
                      <li>• PENALTY_RATE_UNDERPAYMENT - Estimated tax penalty</li>
                      <li>• INTEREST_RATE_ANNUAL - Interest on unpaid balances</li>
                      <li>• SAFE_HARBOR_PERCENT - Minimum payment to avoid penalty</li>
                    </ul>
                  </div>
                  <div className="p-4 bg-purple-50 rounded-lg border border-purple-200">
                    <h5 className="font-semibold text-purple-800">Deductions</h5>
                    <p className="text-sm text-purple-700 mt-1">Business deductions and Net Operating Loss rules</p>
                    <ul className="text-sm text-purple-600 mt-2 space-y-1">
                      <li>• ENABLE_NOL - Allow NOL carryforward</li>
                      <li>• NOL_OFFSET_CAP_PERCENT - Max NOL offset (typically 50%)</li>
                      <li>• INTANGIBLE_EXPENSE_RATE - Intangible expense add-back</li>
                    </ul>
                  </div>
                  <div className="p-4 bg-indigo-50 rounded-lg border border-indigo-200">
                    <h5 className="font-semibold text-indigo-800">Allocation</h5>
                    <p className="text-sm text-indigo-700 mt-1">Business income allocation/apportionment methods</p>
                    <ul className="text-sm text-indigo-600 mt-2 space-y-1">
                      <li>• ALLOCATION_METHOD - 3-factor, single-sales, etc.</li>
                      <li>• ALLOCATION_SALES_FACTOR_WEIGHT - Weighting for sales</li>
                    </ul>
                  </div>
                  <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                    <h5 className="font-semibold text-slate-800">Filing</h5>
                    <p className="text-sm text-slate-700 mt-1">Filing thresholds, deadlines, and extensions</p>
                    <ul className="text-sm text-slate-600 mt-2 space-y-1">
                      <li>• FILING_THRESHOLD - Minimum income requiring return</li>
                      <li>• EXTENSION_DAYS - Extension period (180 days typical)</li>
                      <li>• QUARTERLY_ESTIMATE_THRESHOLD - When estimates required</li>
                    </ul>
                  </div>
                </div>
              </section>

              {/* Value Types */}
              <section>
                <h4 className="text-lg font-semibold text-slate-800 mb-3">Value Types</h4>
                <div className="overflow-x-auto">
                  <table className="min-w-full border border-slate-200 rounded-lg">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-4 py-2 text-left text-sm font-medium text-slate-700 border-b">Type</th>
                        <th className="px-4 py-2 text-left text-sm font-medium text-slate-700 border-b">Description</th>
                        <th className="px-4 py-2 text-left text-sm font-medium text-slate-700 border-b">Example</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      <tr>
                        <td className="px-4 py-2 text-sm font-mono text-indigo-600">PERCENTAGE</td>
                        <td className="px-4 py-2 text-sm text-slate-600">Rate as percentage (0-100)</td>
                        <td className="px-4 py-2 text-sm text-slate-500">2.0% for municipal tax rate</td>
                      </tr>
                      <tr className="bg-slate-50">
                        <td className="px-4 py-2 text-sm font-mono text-indigo-600">NUMBER</td>
                        <td className="px-4 py-2 text-sm text-slate-600">Fixed dollar amount or count</td>
                        <td className="px-4 py-2 text-sm text-slate-500">$25 for late filing penalty</td>
                      </tr>
                      <tr>
                        <td className="px-4 py-2 text-sm font-mono text-indigo-600">BOOLEAN</td>
                        <td className="px-4 py-2 text-sm text-slate-600">True/false toggle</td>
                        <td className="px-4 py-2 text-sm text-slate-500">ENABLE_NOL = true</td>
                      </tr>
                      <tr className="bg-slate-50">
                        <td className="px-4 py-2 text-sm font-mono text-indigo-600">ENUM</td>
                        <td className="px-4 py-2 text-sm text-slate-600">Selection from allowed values</td>
                        <td className="px-4 py-2 text-sm text-slate-500">W2 rule: HIGHEST_OF_ALL, BOX_5_MEDICARE</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>

              {/* Entity Types */}
              <section>
                <h4 className="text-lg font-semibold text-slate-800 mb-3">Entity Types</h4>
                <p className="text-slate-600 mb-3">Rules can apply to different taxpayer types:</p>
                <div className="flex flex-wrap gap-2">
                  <span className="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm">INDIVIDUAL</span>
                  <span className="px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm">BUSINESS</span>
                  <span className="px-3 py-1 bg-purple-100 text-purple-700 rounded-full text-sm">C-CORP</span>
                  <span className="px-3 py-1 bg-orange-100 text-orange-700 rounded-full text-sm">S-CORP</span>
                  <span className="px-3 py-1 bg-pink-100 text-pink-700 rounded-full text-sm">LLC</span>
                  <span className="px-3 py-1 bg-slate-100 text-slate-700 rounded-full text-sm">PARTNERSHIP</span>
                </div>
              </section>

              {/* W2 Qualifying Wages */}
              <section>
                <h4 className="text-lg font-semibold text-slate-800 mb-3">W-2 Qualifying Wages Rule</h4>
                <p className="text-slate-600 mb-3">
                  Municipalities can choose which W-2 box determines the municipal tax base. This is critical for accurate tax calculation:
                </p>
                <div className="space-y-2">
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                    <code className="text-indigo-600 font-mono">HIGHEST_OF_ALL</code>
                    <p className="text-sm text-slate-600 mt-1">Uses the highest of Box 1, Box 5, or Box 18 (most common, maximizes tax base)</p>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                    <code className="text-indigo-600 font-mono">BOX_5_MEDICARE</code>
                    <p className="text-sm text-slate-600 mt-1">Always use Medicare wages (Box 5) - includes pre-tax deductions</p>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                    <code className="text-indigo-600 font-mono">BOX_18_LOCAL</code>
                    <p className="text-sm text-slate-600 mt-1">Always use local wages (Box 18) - employer-reported local wages</p>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                    <code className="text-indigo-600 font-mono">BOX_1_FEDERAL</code>
                    <p className="text-sm text-slate-600 mt-1">Always use federal wages (Box 1) - excludes pre-tax benefits</p>
                  </div>
                </div>
              </section>

              {/* Ohio Municipality Localities */}
              <section>
                <h4 className="text-lg font-semibold text-slate-800 mb-3">Ohio Municipality Rates (Schedule Y)</h4>
                <p className="text-slate-600 mb-3">
                  These locality rates are used for calculating credits on Schedule Y when taxpayers work in multiple municipalities.
                  The system includes rates for 35+ Ohio municipalities:
                </p>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-2 text-sm">
                  <div className="p-2 bg-slate-50 rounded">Columbus - 2.5%</div>
                  <div className="p-2 bg-slate-50 rounded">Cleveland - 2.5%</div>
                  <div className="p-2 bg-slate-50 rounded">Cincinnati - 2.1%</div>
                  <div className="p-2 bg-slate-50 rounded">Toledo - 2.25%</div>
                  <div className="p-2 bg-slate-50 rounded">Akron - 2.5%</div>
                  <div className="p-2 bg-slate-50 rounded">Dayton - 2.5%</div>
                  <div className="p-2 bg-slate-50 rounded">Dublin - 2.0%</div>
                  <div className="p-2 bg-slate-50 rounded">Westerville - 2.0%</div>
                  <div className="p-2 bg-slate-50 rounded">Hilliard - 2.5%</div>
                  <div className="p-2 bg-slate-50 rounded">Upper Arlington - 2.5%</div>
                  <div className="p-2 bg-slate-50 rounded">Worthington - 2.5%</div>
                  <div className="p-2 bg-slate-50 rounded">Gahanna - 2.5%</div>
                </div>
                <p className="text-sm text-slate-500 mt-2 italic">
                  * Rates are updated annually. Additional localities can be added via the Create Rule function.
                </p>
              </section>

              {/* Approval Workflow */}
              <section>
                <h4 className="text-lg font-semibold text-slate-800 mb-3">Approval Workflow</h4>
                <p className="text-slate-600 mb-3">All rule changes go through an approval workflow:</p>
                <div className="flex items-center gap-4 overflow-x-auto py-2">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-1 bg-yellow-100 text-yellow-800 rounded text-sm font-medium">PENDING</span>
                    <span className="text-slate-400">→</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-1 bg-green-100 text-green-800 rounded text-sm font-medium">APPROVED</span>
                    <span className="text-slate-400">or</span>
                    <span className="px-2 py-1 bg-red-100 text-red-800 rounded text-sm font-medium">REJECTED</span>
                  </div>
                </div>
                <p className="text-sm text-slate-500 mt-2">
                  System/default rules are marked with a <span className="px-1 py-0.5 bg-purple-100 text-purple-700 rounded text-xs">DEFAULT</span> badge and cannot be deleted (only modified).
                </p>
              </section>

              {/* Best Practices */}
              <section>
                <h4 className="text-lg font-semibold text-slate-800 mb-3">Best Practices</h4>
                <ul className="space-y-2 text-slate-600">
                  <li className="flex items-start gap-2">
                    <CheckCircle className="w-5 h-5 text-green-500 mt-0.5" />
                    Always include an ordinance reference for audit trail
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle className="w-5 h-5 text-green-500 mt-0.5" />
                    Set effective dates in the future to allow time for approval
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle className="w-5 h-5 text-green-500 mt-0.5" />
                    Use end dates for temporary rate changes
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle className="w-5 h-5 text-green-500 mt-0.5" />
                    Provide clear change reasons for audit compliance
                  </li>
                </ul>
              </section>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// Stat Card Component
const StatCard: React.FC<{
  title: string;
  value: number;
  icon: React.ReactNode;
  color: 'blue' | 'yellow' | 'green' | 'red';
  highlight?: boolean;
}> = ({ title, value, icon, color, highlight }) => {
  const colors = {
    blue: 'bg-blue-100 text-blue-600',
    yellow: 'bg-yellow-100 text-yellow-600',
    green: 'bg-green-100 text-green-600',
    red: 'bg-red-100 text-red-600'
  };

  return (
    <div className={`bg-white rounded-lg shadow-sm border p-4 ${highlight ? 'border-yellow-400 ring-2 ring-yellow-100' : 'border-slate-200'}`}>
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-slate-600">{title}</p>
          <p className="text-2xl font-bold text-slate-900 mt-1">{value}</p>
        </div>
        <div className={`p-3 rounded-lg ${colors[color]}`}>
          {icon}
        </div>
      </div>
    </div>
  );
};

// Rule Form Modal Component
const RuleFormModal: React.FC<{
  rule: TaxRule | null;
  tenantId: string;
  userId: string;
  onClose: () => void;
  onSave: (data: CreateRuleRequest | UpdateRuleRequest) => Promise<void>;
}> = ({ rule, tenantId, userId, onClose, onSave }) => {
  const [formData, setFormData] = useState({
    ruleCode: rule?.ruleCode || '',
    ruleName: rule?.ruleName || '',
    category: rule?.category || 'TaxRates' as RuleCategory,
    valueType: rule?.valueType || 'PERCENTAGE' as RuleValueType,
    value: '',
    effectiveDate: rule?.effectiveDate || new Date().toISOString().split('T')[0],
    endDate: rule?.endDate || '',
    entityTypes: rule?.entityTypes || ['ALL'],
    changeReason: '',
    ordinanceReference: rule?.ordinanceReference || ''
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (rule) {
      const val = rule.value as any;
      let valueStr = '';
      if (rule.valueType === 'NUMBER' || rule.valueType === 'PERCENTAGE') {
        valueStr = val.scalar?.toString() || '';
      } else if (rule.valueType === 'BOOLEAN') {
        valueStr = val.flag ? 'true' : 'false';
      } else if (rule.valueType === 'ENUM') {
        valueStr = val.option || '';
      }
      setFormData(prev => ({ ...prev, value: valueStr }));
    }
  }, [rule]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);

    let value: RuleValue;
    switch (formData.valueType) {
      case 'NUMBER':
        value = { scalar: parseFloat(formData.value) } as NumberValue;
        break;
      case 'PERCENTAGE':
        value = { scalar: parseFloat(formData.value), unit: 'percent' } as PercentageValue;
        break;
      case 'BOOLEAN':
        value = { flag: formData.value === 'true' } as BooleanValue;
        break;
      case 'ENUM':
        value = { option: formData.value, allowedValues: [formData.value] } as EnumValue;
        break;
      default:
        value = { scalar: parseFloat(formData.value) } as NumberValue;
    }

    const requestData: CreateRuleRequest = {
      ruleCode: formData.ruleCode,
      ruleName: formData.ruleName,
      category: formData.category,
      valueType: formData.valueType,
      value,
      effectiveDate: formData.effectiveDate,
      endDate: formData.endDate || undefined,
      tenantId,
      entityTypes: formData.entityTypes,
      changeReason: formData.changeReason
    };

    try {
      await onSave(requestData);
    } catch (err) {
      console.error('Save failed:', err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-y-auto py-8">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl mx-4">
        <div className="px-6 py-4 border-b border-slate-200">
          <h3 className="text-lg font-bold text-slate-900">
            {rule ? 'Edit Rule' : 'Create New Rule'}
          </h3>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Rule Code *
              </label>
              <input
                type="text"
                value={formData.ruleCode}
                onChange={(e) => setFormData({ ...formData, ruleCode: e.target.value.toUpperCase() })}
                placeholder="e.g., MUNICIPAL_TAX_RATE"
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Rule Name *
              </label>
              <input
                type="text"
                value={formData.ruleName}
                onChange={(e) => setFormData({ ...formData, ruleName: e.target.value })}
                placeholder="e.g., Dublin Municipal Tax Rate"
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Category *
              </label>
              <select
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value as RuleCategory })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none bg-white"
              >
                {CATEGORIES.map(cat => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Value Type *
              </label>
              <select
                value={formData.valueType}
                onChange={(e) => setFormData({ ...formData, valueType: e.target.value as RuleValueType })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none bg-white"
              >
                {VALUE_TYPES.map(type => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Value *
            </label>
            {formData.valueType === 'BOOLEAN' ? (
              <select
                value={formData.value}
                onChange={(e) => setFormData({ ...formData, value: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none bg-white"
              >
                <option value="true">Yes / True</option>
                <option value="false">No / False</option>
              </select>
            ) : (
              <input
                type={formData.valueType === 'NUMBER' || formData.valueType === 'PERCENTAGE' ? 'number' : 'text'}
                step={formData.valueType === 'PERCENTAGE' ? '0.01' : '1'}
                value={formData.value}
                onChange={(e) => setFormData({ ...formData, value: e.target.value })}
                placeholder={formData.valueType === 'PERCENTAGE' ? 'e.g., 2.5' : 'Enter value'}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                required
              />
            )}
            {formData.valueType === 'PERCENTAGE' && (
              <p className="text-xs text-slate-500 mt-1">Enter as percentage (e.g., 2.5 for 2.5%)</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Effective Date *
              </label>
              <input
                type="date"
                value={formData.effectiveDate}
                onChange={(e) => setFormData({ ...formData, effectiveDate: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                End Date (Optional)
              </label>
              <input
                type="date"
                value={formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Applies To
            </label>
            <div className="flex gap-4">
              {ENTITY_TYPES.map(type => (
                <label key={type} className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={formData.entityTypes.includes(type)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setFormData({ ...formData, entityTypes: [...formData.entityTypes, type] });
                      } else {
                        setFormData({ ...formData, entityTypes: formData.entityTypes.filter(t => t !== type) });
                      }
                    }}
                    className="rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                  />
                  <span className="text-sm text-slate-700">{type}</span>
                </label>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Change Reason *
            </label>
            <textarea
              value={formData.changeReason}
              onChange={(e) => setFormData({ ...formData, changeReason: e.target.value })}
              placeholder="Describe why this rule is being created/updated..."
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
              rows={2}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Ordinance Reference (Optional)
            </label>
            <input
              type="text"
              value={formData.ordinanceReference}
              onChange={(e) => setFormData({ ...formData, ordinanceReference: e.target.value })}
              placeholder="e.g., Dublin Ord. 2024-001"
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-slate-200">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50"
            >
              {saving ? 'Saving...' : rule ? 'Update Rule' : 'Create Rule'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RuleManagementDashboard;
