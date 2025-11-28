/**
 * ScheduleXAutoCalcButton Component (T019)
 * 
 * Auto-calculation button for Schedule X adjustments (FR-031)
 * Handles 5% Rule, meals 50%→100%, related-party excess calculations
 */

import React from 'react';
import { Calculator } from 'lucide-react';

export interface ScheduleXAutoCalcButtonProps {
  fieldName: string;
  label?: string;
  onClick: () => void;
  loading?: boolean;
  disabled?: boolean;
  className?: string;
}

export const ScheduleXAutoCalcButton: React.FC<ScheduleXAutoCalcButtonProps> = ({
  fieldName,
  label = 'Auto-Calculate',
  onClick,
  loading = false,
  disabled = false,
  className = '',
}) => {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled || loading}
      className={`inline-flex items-center gap-2 px-3 py-2 bg-blue-600 text-white 
                rounded-md hover:bg-blue-700 transition-colors text-sm font-medium
                disabled:bg-gray-400 disabled:cursor-not-allowed ${className}`}
      aria-label={`Auto-calculate ${fieldName}`}
    >
      {loading ? (
        <>
          <span className="animate-spin">⏳</span>
          Calculating...
        </>
      ) : (
        <>
          <Calculator className="w-4 h-4" />
          {label}
        </>
      )}
    </button>
  );
};

export default ScheduleXAutoCalcButton;
