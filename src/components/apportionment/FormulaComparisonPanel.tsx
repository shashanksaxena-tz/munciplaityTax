/**
 * FormulaComparisonPanel Component
 * Displays side-by-side comparison of traditional vs single-sales-factor formulas
 * with recommendation based on which minimizes tax liability.
 * Task: T137 [US5]
 */

import React from 'react';
import { TrendingDown, TrendingUp, AlertCircle, CheckCircle, Calculator } from 'lucide-react';

interface FormulaComparison {
  traditionalFormula: string;
  traditionalApportionment: number;
  traditionalPropertyContribution?: number;
  traditionalPayrollContribution?: number;
  traditionalSalesContribution?: number;
  singleSalesFormula: string;
  singleSalesApportionment: number;
  singleSalesPropertyContribution?: number;
  singleSalesPayrollContribution?: number;
  singleSalesSalesContribution?: number;
  recommendedFormula: string;
  recommendationReason: string;
  savingsPercentage: number;
}

interface FormulaComparisonPanelProps {
  comparison: FormulaComparison;
  selectedFormula?: string;
  onSelectFormula?: (formula: string) => void;
  disabled?: boolean;
}

export function FormulaComparisonPanel({
  comparison,
  selectedFormula,
  onSelectFormula,
  disabled
}: FormulaComparisonPanelProps) {
  const isTraditionalRecommended = comparison.recommendedFormula === comparison.traditionalFormula;
  const isSingleSalesRecommended = comparison.recommendedFormula === comparison.singleSalesFormula;

  const formatPercentage = (value: number | undefined) => {
    if (value === undefined) return '0.00%';
    return `${value.toFixed(2)}%`;
  };

  return (
    <div className="bg-white rounded-lg shadow-lg p-6">
      <div className="flex items-center mb-6">
        <Calculator className="w-6 h-6 text-blue-600 mr-3" />
        <h2 className="text-2xl font-bold text-gray-900">Formula Comparison</h2>
      </div>

      {/* Recommendation Banner */}
      <div className={`mb-6 p-4 rounded-lg border-2 ${
        isTraditionalRecommended ? 'bg-green-50 border-green-300' : 'bg-blue-50 border-blue-300'
      }`}>
        <div className="flex items-start">
          <CheckCircle className={`w-5 h-5 mt-0.5 mr-3 flex-shrink-0 ${
            isTraditionalRecommended ? 'text-green-600' : 'text-blue-600'
          }`} />
          <div>
            <p className="font-semibold text-gray-900 mb-1">
              Recommended: {comparison.recommendedFormula === comparison.traditionalFormula 
                ? 'Traditional Formula' 
                : 'Single-Sales-Factor'}
            </p>
            <p className="text-sm text-gray-700">
              {comparison.recommendationReason}
            </p>
            <p className="text-sm text-gray-600 mt-2">
              <strong>Potential savings: {formatPercentage(comparison.savingsPercentage)}</strong> lower apportionment
            </p>
          </div>
        </div>
      </div>

      {/* Side-by-Side Comparison */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Traditional Formula Card */}
        <div 
          className={`border-2 rounded-lg p-5 cursor-pointer transition ${
            selectedFormula === comparison.traditionalFormula
              ? 'border-blue-500 bg-blue-50'
              : isTraditionalRecommended
              ? 'border-green-300 bg-green-50'
              : 'border-gray-200 hover:border-gray-300'
          }`}
          onClick={() => !disabled && onSelectFormula && onSelectFormula(comparison.traditionalFormula)}
        >
          {onSelectFormula && (
            <div className="flex items-center mb-3">
              <input
                type="radio"
                checked={selectedFormula === comparison.traditionalFormula}
                onChange={() => onSelectFormula(comparison.traditionalFormula)}
                disabled={disabled}
                className="mr-2"
              />
              <span className="font-semibold text-gray-900">Select this formula</span>
            </div>
          )}

          <div className="flex items-center justify-between mb-3">
            <h3 className="text-lg font-bold text-gray-900">Traditional Formula</h3>
            {isTraditionalRecommended && (
              <span className="px-2 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded">
                RECOMMENDED
              </span>
            )}
          </div>

          <div className="mb-4">
            <div className="text-xs text-gray-600 mb-1">4-Factor Double-Weighted Sales</div>
            <div className="text-3xl font-bold text-gray-900">
              {formatPercentage(comparison.traditionalApportionment)}
            </div>
            <div className="text-sm text-gray-600">Ohio Apportionment</div>
          </div>

          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">Property Factor:</span>
              <span className="font-medium">
                {formatPercentage(comparison.traditionalPropertyContribution)} (25% weight)
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Payroll Factor:</span>
              <span className="font-medium">
                {formatPercentage(comparison.traditionalPayrollContribution)} (25% weight)
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Sales Factor:</span>
              <span className="font-medium">
                {formatPercentage(comparison.traditionalSalesContribution)} (50% weight)
              </span>
            </div>
          </div>

          <div className="mt-4 pt-4 border-t border-gray-200">
            <p className="text-xs text-gray-600">
              Balances property, payroll, and sales factors with double weight on sales.
              This is the most common apportionment formula used by states.
            </p>
          </div>
        </div>

        {/* Single-Sales-Factor Card */}
        <div 
          className={`border-2 rounded-lg p-5 cursor-pointer transition ${
            selectedFormula === comparison.singleSalesFormula
              ? 'border-blue-500 bg-blue-50'
              : isSingleSalesRecommended
              ? 'border-green-300 bg-green-50'
              : 'border-gray-200 hover:border-gray-300'
          }`}
          onClick={() => !disabled && onSelectFormula && onSelectFormula(comparison.singleSalesFormula)}
        >
          {onSelectFormula && (
            <div className="flex items-center mb-3">
              <input
                type="radio"
                checked={selectedFormula === comparison.singleSalesFormula}
                onChange={() => onSelectFormula(comparison.singleSalesFormula)}
                disabled={disabled}
                className="mr-2"
              />
              <span className="font-semibold text-gray-900">Select this formula</span>
            </div>
          )}

          <div className="flex items-center justify-between mb-3">
            <h3 className="text-lg font-bold text-gray-900">Single-Sales-Factor</h3>
            {isSingleSalesRecommended && (
              <span className="px-2 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded">
                RECOMMENDED
              </span>
            )}
          </div>

          <div className="mb-4">
            <div className="text-xs text-gray-600 mb-1">100% Sales Factor</div>
            <div className="text-3xl font-bold text-gray-900">
              {formatPercentage(comparison.singleSalesApportionment)}
            </div>
            <div className="text-sm text-gray-600">Ohio Apportionment</div>
          </div>

          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">Property Factor:</span>
              <span className="font-medium text-gray-400">
                {formatPercentage(comparison.singleSalesPropertyContribution)} (0% weight)
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Payroll Factor:</span>
              <span className="font-medium text-gray-400">
                {formatPercentage(comparison.singleSalesPayrollContribution)} (0% weight)
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Sales Factor:</span>
              <span className="font-medium">
                {formatPercentage(comparison.singleSalesSalesContribution)} (100% weight)
              </span>
            </div>
          </div>

          <div className="mt-4 pt-4 border-t border-gray-200">
            <p className="text-xs text-gray-600">
              Apportionment based solely on sales factor, ignoring property and payroll.
              Beneficial for businesses with low in-state sales but high in-state operations.
            </p>
          </div>
        </div>
      </div>

      {/* Comparison Visualization */}
      <div className="mt-6 p-4 bg-gray-50 rounded-lg">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-gray-700">Apportionment Comparison</span>
          <span className="text-xs text-gray-500">Lower is better (less tax)</span>
        </div>
        <div className="relative h-8 bg-gray-200 rounded-full overflow-hidden">
          <div 
            className={`absolute left-0 top-0 h-full ${
              isTraditionalRecommended ? 'bg-green-500' : 'bg-blue-500'
            } flex items-center justify-end pr-2`}
            style={{ width: `${Math.min(comparison.traditionalApportionment, 100)}%` }}
          >
            <span className="text-xs font-semibold text-white">
              Traditional: {formatPercentage(comparison.traditionalApportionment)}
            </span>
          </div>
        </div>
        <div className="relative h-8 bg-gray-200 rounded-full overflow-hidden mt-2">
          <div 
            className={`absolute left-0 top-0 h-full ${
              isSingleSalesRecommended ? 'bg-green-500' : 'bg-blue-500'
            } flex items-center justify-end pr-2`}
            style={{ width: `${Math.min(comparison.singleSalesApportionment, 100)}%` }}
          >
            <span className="text-xs font-semibold text-white">
              Single-Sales: {formatPercentage(comparison.singleSalesApportionment)}
            </span>
          </div>
        </div>
      </div>

      {/* Information Note */}
      <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <div className="flex items-start">
          <AlertCircle className="w-5 h-5 text-blue-600 mt-0.5 mr-3 flex-shrink-0" />
          <div className="text-sm text-gray-700">
            <p className="font-semibold mb-1">Important Election Information</p>
            <p>
              The formula you elect will apply to your entire tax year and cannot be changed after filing.
              Most businesses choose the formula that results in the lowest apportionment percentage to minimize tax liability.
              However, you may elect either formula if both are allowed by your municipality.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
