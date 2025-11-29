/**
 * SafeHarborStatusBanner Component
 * 
 * Display safe harbor status prominently (FR-019).
 * Shows whether taxpayer met safe harbor requirements to avoid estimated tax penalties.
 */

import React from 'react';
import { SafeHarborEvaluation } from '../../types/penalty';
import { formatCurrency, formatPercentage } from '../../utils/formatters';

export interface SafeHarborStatusBannerProps {
  evaluation: SafeHarborEvaluation;
  onLearnMore?: () => void;
}

export const SafeHarborStatusBanner: React.FC<SafeHarborStatusBannerProps> = ({
  evaluation,
  onLearnMore,
}) => {
  const isMet = evaluation.anySafeHarborMet;
  const bgColor = isMet ? 'bg-green-50' : 'bg-yellow-50';
  const borderColor = isMet ? 'border-green-200' : 'border-yellow-200';
  const iconColor = isMet ? 'text-green-400' : 'text-yellow-400';
  const textColor = isMet ? 'text-green-900' : 'text-yellow-900';
  const subTextColor = isMet ? 'text-green-700' : 'text-yellow-700';

  const Icon = isMet ? (
    <svg
      className={`h-8 w-8 ${iconColor}`}
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
      />
    </svg>
  ) : (
    <svg
      className={`h-8 w-8 ${iconColor}`}
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
      />
    </svg>
  );

  return (
    <div className={`${bgColor} border ${borderColor} rounded-lg p-6 mb-6`}>
      <div className="flex items-start">
        <div className="flex-shrink-0">{Icon}</div>
        
        <div className="ml-4 flex-1">
          <h3 className={`text-xl font-bold ${textColor}`}>
            {isMet ? 'Safe Harbor Met - No Penalty Due' : 'Safe Harbor Not Met - Penalty May Apply'}
          </h3>
          
          <p className={`mt-2 text-sm ${subTextColor}`}>
            {evaluation.explanation}
          </p>

          {/* Safe Harbor Details */}
          <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Safe Harbor 1 */}
            <div className={`bg-white rounded-md p-4 ${evaluation.safeHarbor1Met ? 'border-2 border-green-300' : 'border border-gray-200'}`}>
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-sm font-semibold text-gray-900">Safe Harbor 1</h4>
                {evaluation.safeHarbor1Met && (
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800">
                    Met
                  </span>
                )}
              </div>
              <p className="text-xs text-gray-600 mb-2">90% of current year tax</p>
              <div className="space-y-1 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-600">Paid:</span>
                  <span className="font-medium">{formatCurrency(evaluation.currentYearPaid)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Required:</span>
                  <span className="font-medium">{formatCurrency(evaluation.safeHarbor1Required)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Percentage:</span>
                  <span className={`font-medium ${evaluation.safeHarbor1Met ? 'text-green-600' : 'text-red-600'}`}>
                    {formatPercentage(evaluation.currentYearPercentage)}
                  </span>
                </div>
              </div>
            </div>

            {/* Safe Harbor 2 */}
            <div className={`bg-white rounded-md p-4 ${evaluation.safeHarbor2Met ? 'border-2 border-green-300' : 'border border-gray-200'}`}>
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-sm font-semibold text-gray-900">Safe Harbor 2</h4>
                {evaluation.safeHarbor2Met && (
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800">
                    Met
                  </span>
                )}
              </div>
              <p className="text-xs text-gray-600 mb-2">
                {evaluation.agi > evaluation.agiThreshold ? '110%' : '100%'} of prior year tax
                {evaluation.agi > evaluation.agiThreshold && (
                  <span className="ml-1 text-gray-500">(AGI &gt; $150K)</span>
                )}
              </p>
              <div className="space-y-1 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-600">Paid:</span>
                  <span className="font-medium">{formatCurrency(evaluation.priorYearPaid)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Required:</span>
                  <span className="font-medium">{formatCurrency(evaluation.safeHarbor2Required)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Percentage:</span>
                  <span className={`font-medium ${evaluation.safeHarbor2Met ? 'text-green-600' : 'text-red-600'}`}>
                    {formatPercentage(evaluation.priorYearPercentage * 100)}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Learn More Button */}
          {onLearnMore && (
            <button
              onClick={onLearnMore}
              className="mt-4 text-sm font-medium text-blue-600 hover:text-blue-500"
            >
              Learn more about safe harbor rules →
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default SafeHarborStatusBanner;
