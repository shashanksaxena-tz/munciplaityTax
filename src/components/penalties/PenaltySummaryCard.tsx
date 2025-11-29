/**
 * PenaltySummaryCard Component
 * 
 * Display penalty breakdown for a tax return.
 * Shows late filing, late payment, estimated tax penalties, and total.
 * 
 * Functional Requirements:
 * - FR-001 to FR-026: Display all penalty types
 * - FR-019: Display safe harbor status prominently
 */

import React from 'react';
import { PenaltyCalculationResponse } from '../../types/penalty';
import { formatCurrency } from '../../utils/formatters';

export interface PenaltySummaryCardProps {
  penalties: PenaltyCalculationResponse;
  onViewDetails?: () => void;
  onRequestAbatement?: () => void;
}

export const PenaltySummaryCard: React.FC<PenaltySummaryCardProps> = ({
  penalties,
  onViewDetails,
  onRequestAbatement,
}) => {
  const hasLateFilingPenalty = penalties.lateFilingPenalty && penalties.lateFilingPenalty > 0;
  const hasLatePaymentPenalty = penalties.latePaymentPenalty && penalties.latePaymentPenalty > 0;
  const isAbated = penalties.isAbated;

  return (
    <div className="bg-white shadow-md rounded-lg p-6">
      <div className="flex justify-between items-start mb-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Penalty Summary</h2>
          <p className="text-sm text-gray-600 mt-1">
            Tax Year: {penalties.taxYearAndPeriod} | Due Date: {penalties.dueDate}
          </p>
        </div>
        {isAbated && (
          <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-800">
            Abated
          </span>
        )}
      </div>

      {/* Penalty Breakdown */}
      <div className="space-y-4 mb-6">
        {/* Late Filing Penalty */}
        {hasLateFilingPenalty && (
          <div className="border-l-4 border-orange-500 pl-4">
            <div className="flex justify-between items-center">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">Late Filing Penalty</h3>
                <p className="text-sm text-gray-600">
                  {penalties.lateFilingPenaltyRate}% per month (max 25%)
                </p>
              </div>
              <div className="text-right">
                <p className="text-2xl font-bold text-orange-600">
                  {formatCurrency(penalties.lateFilingPenalty)}
                </p>
              </div>
            </div>
            {penalties.lateFilingPenaltyExplanation && (
              <p className="text-sm text-gray-500 mt-2">
                {penalties.lateFilingPenaltyExplanation}
              </p>
            )}
          </div>
        )}

        {/* Late Payment Penalty */}
        {hasLatePaymentPenalty && (
          <div className="border-l-4 border-red-500 pl-4">
            <div className="flex justify-between items-center">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">Late Payment Penalty</h3>
                <p className="text-sm text-gray-600">
                  {penalties.latePaymentPenaltyRate}% per month (max 25%)
                </p>
              </div>
              <div className="text-right">
                <p className="text-2xl font-bold text-red-600">
                  {formatCurrency(penalties.latePaymentPenalty)}
                </p>
              </div>
            </div>
            {penalties.latePaymentPenaltyExplanation && (
              <p className="text-sm text-gray-500 mt-2">
                {penalties.latePaymentPenaltyExplanation}
              </p>
            )}
          </div>
        )}

        {/* Combined Cap Notice */}
        {penalties.combinedCapApplied && (
          <div className="bg-blue-50 border border-blue-200 rounded-md p-3">
            <div className="flex items-start">
              <svg
                className="h-5 w-5 text-blue-400 mt-0.5 mr-2"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <div>
                <h4 className="text-sm font-medium text-blue-900">Combined Penalty Cap Applied</h4>
                <p className="text-sm text-blue-700 mt-1">
                  {penalties.combinedCapExplanation}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Total Penalties */}
      <div className="border-t-2 border-gray-200 pt-4">
        <div className="flex justify-between items-center">
          <h3 className="text-xl font-bold text-gray-900">Total Penalties</h3>
          <p className="text-3xl font-bold text-gray-900">
            {formatCurrency(penalties.totalPenalties)}
          </p>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="mt-6 flex gap-3">
        {onViewDetails && (
          <button
            onClick={onViewDetails}
            className="flex-1 px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            View Calculation Details
          </button>
        )}
        {onRequestAbatement && !isAbated && (
          <button
            onClick={onRequestAbatement}
            className="flex-1 px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Request Abatement
          </button>
        )}
      </div>

      {/* Payment Information */}
      <div className="mt-4 text-sm text-gray-500">
        <p>Days Late: {penalties.daysLate} days</p>
        <p>Payment Date: {penalties.paymentDate || 'Unpaid'}</p>
        <p>Tax Due: {formatCurrency(penalties.taxDue)}</p>
      </div>
    </div>
  );
};

export default PenaltySummaryCard;
