/**
 * EstimatedTaxPenaltyTable Component
 * 
 * Display quarterly estimated tax underpayment schedule (FR-026).
 * Shows required vs actual payments for each quarter.
 */

import React from 'react';
import { Quarter, QuarterlyUnderpayment } from '../../types/penalty';
import { formatCurrency, formatDate } from '../../utils/formatters';

export interface EstimatedTaxPenaltyTableProps {
  quarterlyUnderpayments: QuarterlyUnderpayment[];
  totalPenalty: number;
}

const quarterNames: Record<Quarter, string> = {
  [Quarter.Q1]: 'Q1 (Jan-Mar)',
  [Quarter.Q2]: 'Q2 (Apr-Jun)',
  [Quarter.Q3]: 'Q3 (Jul-Sep)',
  [Quarter.Q4]: 'Q4 (Oct-Dec)',
};

export const EstimatedTaxPenaltyTable: React.FC<EstimatedTaxPenaltyTableProps> = ({
  quarterlyUnderpayments,
  totalPenalty,
}) => {
  return (
    <div className="bg-white shadow-md rounded-lg overflow-hidden">
      <div className="px-6 py-4 bg-gray-50 border-b border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900">
          Quarterly Estimated Tax Schedule
        </h3>
        <p className="text-sm text-gray-600 mt-1">
          Required payments: 25% of annual tax per quarter
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th
                scope="col"
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Quarter
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Due Date
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Required
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Actual
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Underpayment
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Days Late
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Penalty
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {quarterlyUnderpayments.map((underpayment) => {
              const hasUnderpayment = underpayment.underpaymentAmount > 0;
              const hasPenalty = underpayment.penaltyAmount > 0;

              return (
                <tr
                  key={underpayment.quarter}
                  className={hasUnderpayment ? 'bg-red-50' : ''}
                >
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {quarterNames[underpayment.quarter]}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {formatDate(underpayment.dueDate)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-900">
                    {formatCurrency(underpayment.requiredPayment)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-900">
                    {formatCurrency(underpayment.actualPayment)}
                  </td>
                  <td
                    className={`px-6 py-4 whitespace-nowrap text-sm text-right font-medium ${
                      hasUnderpayment ? 'text-red-600' : 'text-green-600'
                    }`}
                  >
                    {hasUnderpayment ? formatCurrency(underpayment.underpaymentAmount) : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-600">
                    {underpayment.daysLate > 0 ? underpayment.daysLate : '-'}
                  </td>
                  <td
                    className={`px-6 py-4 whitespace-nowrap text-sm text-right font-medium ${
                      hasPenalty ? 'text-red-600' : 'text-gray-400'
                    }`}
                  >
                    {hasPenalty ? formatCurrency(underpayment.penaltyAmount) : '-'}
                  </td>
                </tr>
              );
            })}
          </tbody>
          <tfoot className="bg-gray-50">
            <tr>
              <td
                colSpan={6}
                className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-right text-gray-900"
              >
                Total Penalty:
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-lg font-bold text-right text-red-600">
                {formatCurrency(totalPenalty)}
              </td>
            </tr>
          </tfoot>
        </table>
      </div>

      {/* Summary Notes */}
      <div className="px-6 py-4 bg-gray-50 border-t border-gray-200">
        <p className="text-xs text-gray-600">
          <strong>Note:</strong> Penalties are calculated from the due date until year-end.
          Overpayments in later quarters can offset underpayments in earlier quarters.
        </p>
      </div>
    </div>
  );
};

export default EstimatedTaxPenaltyTable;
