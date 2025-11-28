/**
 * ScheduleXFieldInput Component (T016)
 * 
 * Reusable input field for Schedule X adjustments with:
 * - Currency formatting
 * - Help icon with tooltip
 * - Auto-calculation button (if applicable)
 * - AI confidence score badge (if AI-extracted)
 */

import React from 'react';
import { HelpCircle } from 'lucide-react';
import { formatCurrency, parseCurrency } from '../../utils/scheduleXFormatting';

export interface ScheduleXFieldInputProps {
  fieldName: string;
  label: string;
  value: number | null | undefined;
  onChange: (value: number) => void;
  helpText?: string;
  confidenceScore?: number;
  showAutoCalcButton?: boolean;
  onAutoCalculate?: () => void;
  disabled?: boolean;
  required?: boolean;
  className?: string;
}

export const ScheduleXFieldInput: React.FC<ScheduleXFieldInputProps> = ({
  fieldName,
  label,
  value,
  onChange,
  helpText,
  confidenceScore,
  showAutoCalcButton = false,
  onAutoCalculate,
  disabled = false,
  required = false,
  className = '',
}) => {
  const [displayValue, setDisplayValue] = React.useState<string>(
    value ? formatCurrency(value) : ''
  );

  const handleFocus = () => {
    // Show raw number when focused for easier editing
    setDisplayValue(value ? value.toString() : '');
  };

  const handleBlur = () => {
    // Parse and format currency on blur
    const numericValue = parseCurrency(displayValue);
    onChange(numericValue);
    setDisplayValue(formatCurrency(numericValue));
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setDisplayValue(e.target.value);
  };

  return (
    <div className={`mb-4 ${className}`}>
      <div className="flex items-center gap-2 mb-1">
        <label htmlFor={fieldName} className="text-sm font-medium text-gray-700">
          {label}
          {required && <span className="text-red-500 ml-1">*</span>}
        </label>
        
        {helpText && (
          <button
            type="button"
            className="text-gray-400 hover:text-gray-600 transition-colors"
            title={helpText}
            aria-label={`Help for ${label}`}
          >
            <HelpCircle className="w-4 h-4" />
          </button>
        )}
        
        {confidenceScore !== undefined && (
          <span className={`text-xs px-2 py-0.5 rounded-full ${
            confidenceScore >= 0.9 ? 'bg-green-100 text-green-800' :
            confidenceScore >= 0.7 ? 'bg-yellow-100 text-yellow-800' :
            'bg-red-100 text-red-800'
          }`}>
            AI: {(confidenceScore * 100).toFixed(0)}%
          </span>
        )}
      </div>
      
      <div className="flex gap-2">
        <input
          id={fieldName}
          type="text"
          value={displayValue}
          onChange={handleChange}
          onFocus={handleFocus}
          onBlur={handleBlur}
          disabled={disabled}
          className="flex-1 px-3 py-2 border border-gray-300 rounded-md shadow-sm 
                   focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 
                   disabled:cursor-not-allowed"
          placeholder="$0.00"
          aria-describedby={helpText ? `${fieldName}-help` : undefined}
        />
        
        {showAutoCalcButton && onAutoCalculate && (
          <button
            type="button"
            onClick={onAutoCalculate}
            className="px-3 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 
                     transition-colors text-sm font-medium whitespace-nowrap"
          >
            Auto-Calc
          </button>
        )}
      </div>
      
      {helpText && (
        <p id={`${fieldName}-help`} className="mt-1 text-xs text-gray-500">
          {helpText}
        </p>
      )}
    </div>
  );
};

export default ScheduleXFieldInput;
