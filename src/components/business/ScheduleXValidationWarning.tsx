/**
 * ScheduleXValidationWarning Component (T041)
 * 
 * Warning component for >20% variance between federal and municipal income (FR-034)
 * Displays prominent warning when adjusted municipal income differs from federal by >20%
 */

import React from 'react';
import { AlertTriangle } from 'lucide-react';

export interface ScheduleXValidationWarningProps {
  fedTaxableIncome: number;
  adjustedMunicipalIncome: number;
  className?: string;
}

/**
 * Calculate variance percentage between federal and municipal income
 */
const calculateVariance = (fedIncome: number, muniIncome: number): number => {
  if (fedIncome === 0) {
    return muniIncome === 0 ? 0 : 100;
  }
  return Math.abs((muniIncome - fedIncome) / fedIncome) * 100;
};

/**
 * Format percentage for display
 */
const formatPercentage = (value: number): string => {
  return value.toFixed(1) + '%';
};

/**
 * Format currency for display
 */
const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
};

/**
 * Display validation warning if variance >20%
 * 
 * @example
 * <ScheduleXValidationWarning
 *   fedTaxableIncome={500000}
 *   adjustedMunicipalIncome={625000}
 * />
 * // Shows: "⚠️ Adjusted Municipal Income differs from Federal Income by 25% - please verify all adjustments"
 */
export const ScheduleXValidationWarning: React.FC<ScheduleXValidationWarningProps> = ({
  fedTaxableIncome,
  adjustedMunicipalIncome,
  className = '',
}) => {
  const variance = calculateVariance(fedTaxableIncome, adjustedMunicipalIncome);
  const VARIANCE_THRESHOLD = 20; // FR-034: Flag if >20% variance

  if (variance <= VARIANCE_THRESHOLD) {
    return null; // No warning needed
  }

  const difference = adjustedMunicipalIncome - fedTaxableIncome;
  const direction = difference > 0 ? 'higher' : 'lower';

  return (
    <div
      className={`bg-yellow-50 border-l-4 border-yellow-400 p-4 rounded-md ${className}`}
      role="alert"
      aria-live="polite"
    >
      <div className="flex items-start">
        <AlertTriangle className="h-5 w-5 text-yellow-400 mt-0.5 mr-3 flex-shrink-0" aria-hidden="true" />
        <div className="flex-1">
          <h3 className="text-sm font-medium text-yellow-800">Significant Variance Detected (FR-034)</h3>
          <div className="mt-2 text-sm text-yellow-700 space-y-1">
            <p>
              <strong>Adjusted Municipal Income differs from Federal Income by {formatPercentage(variance)}</strong>
            </p>
            <p className="mt-1">
              Federal: {formatCurrency(fedTaxableIncome)} → Municipal: {formatCurrency(adjustedMunicipalIncome)}
              <br />
              Difference: {formatCurrency(Math.abs(difference))} {direction}
            </p>
            <p className="mt-2 font-medium">
              Please verify all Schedule X adjustments are accurate and properly documented.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ScheduleXValidationWarning;
