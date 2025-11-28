/**
 * ScheduleXHelpTooltip Component (T017)
 * 
 * Help tooltip component explaining each Schedule X adjustment (FR-031)
 */

import React from 'react';
import { HelpCircle } from 'lucide-react';

export interface ScheduleXHelpTooltipProps {
  fieldName: string;
  title?: string;
  description: string;
  example?: string;
  className?: string;
}

export const ScheduleXHelpTooltip: React.FC<ScheduleXHelpTooltipProps> = ({
  fieldName,
  title,
  description,
  example,
  className = '',
}) => {
  const [isOpen, setIsOpen] = React.useState(false);

  return (
    <div className={`relative inline-block ${className}`}>
      <button
        type="button"
        onMouseEnter={() => setIsOpen(true)}
        onMouseLeave={() => setIsOpen(false)}
        className="text-gray-400 hover:text-gray-600 transition-colors"
        aria-label={`Help for ${fieldName}`}
      >
        <HelpCircle className="w-4 h-4" />
      </button>
      
      {isOpen && (
        <div className="absolute z-10 w-80 p-4 bg-white border border-gray-200 rounded-lg shadow-lg 
                      left-0 bottom-full mb-2">
          {title && (
            <h4 className="font-semibold text-gray-900 mb-2">{title}</h4>
          )}
          <p className="text-sm text-gray-700 mb-2">{description}</p>
          {example && (
            <div className="text-xs text-gray-600 bg-gray-50 p-2 rounded">
              <strong>Example:</strong> {example}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ScheduleXHelpTooltip;
