/**
 * ApportionmentBreakdownCard Component
 * Displays the three-factor apportionment calculation breakdown with throwback and service sourcing adjustments
 * Enhanced: T087 [US2], T102 [US3]
 */

import React from 'react';
import { AlertTriangle, MapPin, TrendingUp } from 'lucide-react';
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

  // Check if there are adjustments to display
  const hasThrowbackAdjustments = breakdown.throwbackAdjustments && 
    Object.keys(breakdown.throwbackAdjustments).length > 0;
  const hasServiceAdjustments = breakdown.serviceSourcingAdjustments && 
    Object.keys(breakdown.serviceSourcingAdjustments).length > 0;
  const hasAdjustments = hasThrowbackAdjustments || hasServiceAdjustments;

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
          Ohio property / Total property = {formatPercent(breakdown.propertyFactorPercentage)}
        </div>
        {breakdown.propertyFactorWeight && breakdown.propertyFactorWeight !== 1 && (
          <div className="text-xs text-gray-500 mt-1">
            Weight: {breakdown.propertyFactorWeight}x | 
            Weighted contribution: {formatPercent(breakdown.propertyFactorWeightedContribution || 0)}
          </div>
        )}
      </div>

      {/* Payroll Factor */}
      <div className="mb-4 pb-4 border-b">
        <div className="flex justify-between items-center mb-2">
          <span className="font-medium text-gray-700">Payroll Factor</span>
          <span className="text-lg font-semibold">{formatPercent(breakdown.payrollFactorPercentage)}</span>
        </div>
        <div className="text-sm text-gray-600">
          Ohio payroll / Total payroll = {formatPercent(breakdown.payrollFactorPercentage)}
        </div>
        {breakdown.payrollFactorWeight && breakdown.payrollFactorWeight !== 1 && (
          <div className="text-xs text-gray-500 mt-1">
            Weight: {breakdown.payrollFactorWeight}x | 
            Weighted contribution: {formatPercent(breakdown.payrollFactorWeightedContribution || 0)}
          </div>
        )}
      </div>

      {/* Sales Factor */}
      <div className="mb-4 pb-4 border-b">
        <div className="flex justify-between items-center mb-2">
          <span className="font-medium text-gray-700">Sales Factor</span>
          <span className="text-lg font-semibold">{formatPercent(breakdown.salesFactorPercentage)}</span>
        </div>
        <div className="text-sm text-gray-600">
          Ohio sales / Total sales = {formatPercent(breakdown.salesFactorPercentage)}
        </div>
        {breakdown.salesFactorWeight && breakdown.salesFactorWeight !== 1 && (
          <div className="text-xs text-gray-500 mt-1">
            Weight: {breakdown.salesFactorWeight}x (double-weighted) | 
            Weighted contribution: {formatPercent(breakdown.salesFactorWeightedContribution || 0)}
          </div>
        )}

        {/* Throwback Adjustments */}
        {hasThrowbackAdjustments && (
          <div className="mt-3 p-3 bg-yellow-50 border border-yellow-200 rounded">
            <div className="flex items-start mb-2">
              <AlertTriangle className="w-4 h-4 text-yellow-600 mt-0.5 mr-2 flex-shrink-0" />
              <div className="flex-1">
                <p className="text-sm font-medium text-yellow-800">Throwback Adjustments Applied</p>
                <p className="text-xs text-yellow-700 mt-1">
                  Sales to states where you lack nexus have been thrown back to Ohio
                </p>
              </div>
            </div>
            <div className="space-y-1 text-xs">
              {Object.entries(breakdown.throwbackAdjustments).map(([state, amount]) => (
                <div key={state} className="flex justify-between text-yellow-700">
                  <span>• Sales to {state} - No nexus - Thrown back:</span>
                  <span className="font-medium">{formatCurrency(amount)}</span>
                </div>
              ))}
            </div>
            {breakdown.throwbackTransactionCount > 0 && (
              <div className="text-xs text-yellow-600 mt-2 pt-2 border-t border-yellow-300">
                {breakdown.throwbackTransactionCount} transaction(s) affected by throwback rules
              </div>
            )}
          </div>
        )}

        {/* Service Sourcing Adjustments */}
        {hasServiceAdjustments && (
          <div className="mt-3 p-3 bg-blue-50 border border-blue-200 rounded">
            <div className="flex items-start mb-2">
              <MapPin className="w-4 h-4 text-blue-600 mt-0.5 mr-2 flex-shrink-0" />
              <div className="flex-1">
                <p className="text-sm font-medium text-blue-800">Service Revenue Sourcing</p>
                <p className="text-xs text-blue-700 mt-1">
                  Service revenue sourced based on customer location (market-based)
                </p>
              </div>
            </div>
            <div className="space-y-1 text-xs">
              {Object.entries(breakdown.serviceSourcingAdjustments).map(([state, amount]) => (
                <div key={state} className="flex justify-between text-blue-700">
                  <span>• Service revenue sourced to {state}:</span>
                  <span className="font-medium">{formatCurrency(amount)}</span>
                </div>
              ))}
            </div>
            {breakdown.marketBasedServiceCount > 0 && (
              <div className="text-xs text-blue-600 mt-2 pt-2 border-t border-blue-300">
                {breakdown.marketBasedServiceCount} service transaction(s) using market-based sourcing
              </div>
            )}
          </div>
        )}

        {/* Transaction Summary */}
        {breakdown.totalSaleTransactions > 0 && (
          <div className="mt-3 p-2 bg-gray-50 rounded text-xs text-gray-600">
            <TrendingUp className="inline w-3 h-3 mr-1" />
            Total transactions: {breakdown.totalSaleTransactions}
            {breakdown.throwbackTransactionCount > 0 && 
              ` | Throwback: ${breakdown.throwbackTransactionCount}`}
            {breakdown.marketBasedServiceCount > 0 && 
              ` | Market-based services: ${breakdown.marketBasedServiceCount}`}
          </div>
        )}
      </div>

      {/* Sales Factor (Double-Weighted) */}
      {breakdown.salesFactorWeight === 2 && (
        <div className="mb-4 pb-4 border-b">
          <div className="flex justify-between items-center mb-2">
            <span className="font-medium text-gray-700">Sales Factor (2x)</span>
            <span className="text-lg font-semibold">{formatPercent(breakdown.salesFactorPercentage)}</span>
          </div>
          <div className="text-sm text-gray-600">
            Double-weighted per Ohio four-factor formula
          </div>
        </div>
      )}

      {/* Formula Calculation */}
      <div className="mt-6 p-4 bg-gray-50 rounded">
        <div className="text-sm font-medium text-gray-700 mb-2">
          {breakdown.formulaDescription || 'Four-Factor Formula:'}
        </div>
        <div className="font-mono text-sm text-gray-600 mb-2">
          {breakdown.calculationExplanation || 
            `(${formatPercent(breakdown.propertyFactorPercentage)} + ${formatPercent(breakdown.payrollFactorPercentage)} + ${formatPercent(breakdown.salesFactorPercentage)} × 2) / ${breakdown.totalWeight || 4}`
          }
        </div>
        {hasAdjustments && (
          <div className="text-xs text-gray-600 mt-2 pt-2 border-t border-gray-300">
            <strong>Note:</strong> Sales factor includes throwback adjustments and service sourcing calculations
          </div>
        )}
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
