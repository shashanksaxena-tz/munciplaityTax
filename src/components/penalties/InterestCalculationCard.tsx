/**
 * InterestCalculationCard Component
 * 
 * Display interest calculation breakdown with quarterly compounding (FR-032).
 * Shows interest accrual by quarter.
 */

import React, { useState } from 'react';
import { InterestCalculationResponse, QuarterlyInterest } from '../../types/interest';
import { formatCurrency, formatDate, formatPercentage } from '../../utils/formatters';

export interface InterestCalculationCardProps {
  interest: InterestCalculationResponse;
}

export const InterestCalculationCard: React.FC<InterestCalculationCardProps> = ({ interest }) => {
  const [showBreakdown, setShowBreakdown] = useState(false);

  return (
    <div className="bg-white shadow-md rounded-lg overflow-hidden">
      {/* Header */}
      <div className="px-6 py-4 bg-blue-50 border-b border-blue-200">
        <h3 className="text-lg font-semibold text-gray-900">Interest Calculation</h3>
        <p className="text-sm text-gray-600 mt-1">
          Quarterly compounding on unpaid tax
        </p>
      </div>

      {/* Summary */}
      <div className="px-6 py-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
          <div>
            <p className="text-sm text-gray-600">Period</p>
            <p className="text-lg font-semibold text-gray-900">
              {formatDate(interest.startDate)} - {formatDate(interest.endDate)}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Total Days</p>
            <p className="text-lg font-semibold text-gray-900">{interest.totalDays} days</p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Unpaid Tax</p>
            <p className="text-lg font-semibold text-gray-900">
              {formatCurrency(interest.unpaidTaxAmount)}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Annual Interest Rate</p>
            <p className="text-lg font-semibold text-gray-900">
              {formatPercentage(interest.annualInterestRate * 100)}
            </p>
          </div>
        </div>

        {/* Total Interest */}
        <div className="mt-4 p-4 bg-blue-50 rounded-lg">
          <div className="flex justify-between items-center">
            <span className="text-lg font-semibold text-gray-900">Total Interest</span>
            <span className="text-3xl font-bold text-blue-600">
              {formatCurrency(interest.totalInterestAmount)}
            </span>
          </div>
        </div>

        {/* Explanation */}
        <div className="mt-4 p-4 bg-gray-50 rounded-lg">
          <p className="text-sm text-gray-700">{interest.explanation}</p>
        </div>

        {/* Toggle Breakdown Button */}
        {interest.quarterlyBreakdown && interest.quarterlyBreakdown.length > 0 && (
          <button
            onClick={() => setShowBreakdown(!showBreakdown)}
            className="mt-4 w-full px-4 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            {showBreakdown ? 'Hide' : 'Show'} Quarterly Breakdown
            <svg
              className={`inline-block ml-2 h-4 w-4 transform transition-transform ${
                showBreakdown ? 'rotate-180' : ''
              }`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 9l-7 7-7-7"
              />
            </svg>
          </button>
        )}
      </div>

      {/* Quarterly Breakdown */}
      {showBreakdown && interest.quarterlyBreakdown && (
        <div className="border-t border-gray-200">
          <div className="px-6 py-4 bg-gray-50">
            <h4 className="text-sm font-semibold text-gray-900 mb-3">
              Quarterly Interest Breakdown
            </h4>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Quarter
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Period
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Days
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Beginning Balance
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Interest
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Ending Balance
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {interest.quarterlyBreakdown.map((quarter: QuarterlyInterest, index: number) => (
                  <tr key={index}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {quarter.quarter}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {formatDate(quarter.startDate)} - {formatDate(quarter.endDate)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-900">
                      {quarter.daysInPeriod}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-900">
                      {formatCurrency(quarter.beginningBalance)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-medium text-blue-600">
                      {formatCurrency(quarter.interestAmount)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-semibold text-gray-900">
                      {formatCurrency(quarter.endingBalance)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="px-6 py-4 bg-gray-50 border-t border-gray-200">
            <p className="text-xs text-gray-600">
              <strong>Note:</strong> Interest compounds quarterly. Each quarter's ending balance
              becomes the next quarter's beginning balance (principal + interest).
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default InterestCalculationCard;
