import React from 'react';

interface FactorPercentageDisplayProps {
  numerator: number;
  denominator: number;
  percentage?: number;
  label: string;
  showCalculation?: boolean;
  className?: string;
}

/**
 * FactorPercentageDisplay Component
 * 
 * Reusable component for displaying factor percentages with numerator/denominator.
 * Shows the calculation breakdown and final percentage.
 * 
 * @param numerator - Ohio/state-specific amount
 * @param denominator - Total amount everywhere
 * @param percentage - Pre-calculated percentage (optional, will calculate if not provided)
 * @param label - Label for the factor (e.g., "Property Factor", "Payroll Factor")
 * @param showCalculation - Whether to show the calculation formula
 * @param className - Additional CSS classes
 */
const FactorPercentageDisplay: React.FC<FactorPercentageDisplayProps> = ({
  numerator,
  denominator,
  percentage,
  label,
  showCalculation = true,
  className = ''
}) => {
  // Calculate percentage if not provided
  const calculatedPercentage = percentage !== undefined
    ? percentage
    : denominator > 0
    ? (numerator / denominator) * 100
    : 0;

  // Format currency
  const formatCurrency = (value: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  };

  // Format percentage
  const formatPercentage = (value: number): string => {
    return `${value.toFixed(4)}%`;
  };

  return (
    <div className={`border border-gray-300 rounded-lg p-4 bg-white ${className}`}>
      {/* Label */}
      <h4 className="text-sm font-semibold text-gray-700 mb-3">{label}</h4>

      {/* Calculation Display */}
      {showCalculation && (
        <div className="space-y-2 mb-4">
          <div className="flex justify-between items-center text-sm">
            <span className="text-gray-600">Ohio Amount:</span>
            <span className="font-medium text-gray-900">{formatCurrency(numerator)}</span>
          </div>
          <div className="flex justify-between items-center text-sm">
            <span className="text-gray-600">Total Amount:</span>
            <span className="font-medium text-gray-900">{formatCurrency(denominator)}</span>
          </div>
          
          {/* Calculation Formula */}
          <div className="border-t border-gray-200 pt-2 mt-2">
            <div className="text-xs text-gray-500 text-center">
              {formatCurrency(numerator)} ÷ {formatCurrency(denominator)} × 100
            </div>
          </div>
        </div>
      )}

      {/* Percentage Result */}
      <div className="bg-blue-50 border border-blue-200 rounded p-3">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-blue-900">{label}:</span>
          <span className="text-lg font-bold text-blue-700">
            {formatPercentage(calculatedPercentage)}
          </span>
        </div>
      </div>

      {/* Warning for edge cases */}
      {denominator === 0 && (
        <div className="mt-2 text-xs text-amber-600 bg-amber-50 border border-amber-200 rounded p-2">
          ⚠️ Total amount is zero. Factor defaults to 0%.
        </div>
      )}
      {numerator > denominator && (
        <div className="mt-2 text-xs text-red-600 bg-red-50 border border-red-200 rounded p-2">
          ⚠️ Ohio amount exceeds total amount. Please verify your data.
        </div>
      )}
    </div>
  );
};

export default FactorPercentageDisplay;
