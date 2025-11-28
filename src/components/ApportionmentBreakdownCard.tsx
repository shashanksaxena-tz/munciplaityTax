/**
 * ApportionmentBreakdownCard Component
 * Displays the three-factor apportionment calculation breakdown
 */

import React from 'react';
import type { ApportionmentBreakdown } from '../types/apportionment';

interface ApportionmentBreakdownCardProps {
  breakdown: ApportionmentBreakdown;
}

export function ApportionmentBreakdownCard({ breakdown }: ApportionmentBreakdownCardProps) {
  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  };

  const formatPercent = (value: number) => {
    return `${value.toFixed(2)}%`;
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">Apportionment Calculation Breakdown</h3>
      
      {/* Property Factor */}
      <div className="mb-4 pb-4 border-b">
        <div className="flex justify-between items-center mb-2">
          <span className="font-medium text-gray-700">Property Factor</span>
          <span className="text-lg font-semibold">{formatPercent(breakdown.propertyFactorPercentage)}</span>
        </div>
        <div className="text-sm text-gray-600">
          {formatCurrency(breakdown.propertyFactorNumerator)} OH / {formatCurrency(breakdown.propertyFactorDenominator)} total
        </div>
      </div>

      {/* Payroll Factor */}
      <div className="mb-4 pb-4 border-b">
        <div className="flex justify-between items-center mb-2">
          <span className="font-medium text-gray-700">Payroll Factor</span>
          <span className="text-lg font-semibold">{formatPercent(breakdown.payrollFactorPercentage)}</span>
        </div>
        <div className="text-sm text-gray-600">
          {formatCurrency(breakdown.payrollFactorNumerator)} OH / {formatCurrency(breakdown.payrollFactorDenominator)} total
        </div>
      </div>

      {/* Sales Factor */}
      <div className="mb-4 pb-4 border-b">
        <div className="flex justify-between items-center mb-2">
          <span className="font-medium text-gray-700">Sales Factor</span>
          <span className="text-lg font-semibold">{formatPercent(breakdown.salesFactorPercentage)}</span>
        </div>
        <div className="text-sm text-gray-600">
          {formatCurrency(breakdown.salesFactorNumerator)} OH / {formatCurrency(breakdown.salesFactorDenominator)} total
        </div>
      </div>

      {/* Sales Factor (Double-Weighted) */}
      <div className="mb-4 pb-4 border-b">
        <div className="flex justify-between items-center mb-2">
          <span className="font-medium text-gray-700">Sales Factor (2x)</span>
          <span className="text-lg font-semibold">{formatPercent(breakdown.salesFactorPercentage)}</span>
        </div>
        <div className="text-sm text-gray-600">
          Double-weighted per Ohio formula
        </div>
      </div>

      {/* Formula Calculation */}
      <div className="mt-6 p-4 bg-gray-50 rounded">
        <div className="text-sm font-medium text-gray-700 mb-2">Four-Factor Formula:</div>
        <div className="font-mono text-sm text-gray-600 mb-2">
          ({formatPercent(breakdown.propertyFactorPercentage)} + {formatPercent(breakdown.payrollFactorPercentage)} + {formatPercent(breakdown.salesFactorPercentage)} + {formatPercent(breakdown.salesFactorPercentage)}) / 4
        </div>
        <div className="text-sm text-gray-600">
          = {formatPercent(breakdown.propertyFactorPercentage + breakdown.payrollFactorPercentage + breakdown.salesFactorPercentage * 2)} / 4
        </div>
      </div>

      {/* Final Result */}
      <div className="mt-6 p-4 bg-blue-50 border-2 border-blue-200 rounded-lg">
        <div className="flex justify-between items-center">
          <span className="text-lg font-semibold text-gray-900">Ohio Apportionment Percentage:</span>
          <span className="text-2xl font-bold text-blue-600">
            {formatPercent(breakdown.finalApportionmentPercentage)}
          </span>
        </div>
        <div className="text-sm text-gray-600 mt-2">
          This percentage will be applied to your total business income to determine Ohio taxable income.
        </div>
      </div>

      {/* Help Text */}
      <div className="mt-4 text-xs text-gray-500">
        <strong>Note:</strong> Ohio uses a four-factor apportionment formula with double-weighted sales factor. 
        This means the sales factor counts twice in the calculation.
      </div>
    </div>
  );
}
