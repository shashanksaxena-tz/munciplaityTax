/**
 * Period History Table Component
 * Displays all W-1 filings for the year with status and cumulative totals
 */

import React from 'react';
import { Calendar, CheckCircle, Clock, AlertTriangle, DollarSign } from 'lucide-react';
import { WithholdingReturnData } from '../types';

interface PeriodHistoryTableProps {
  filings: WithholdingReturnData[];
  onSelectPeriod?: (filing: WithholdingReturnData) => void;
}

export const PeriodHistoryTable: React.FC<PeriodHistoryTableProps> = ({ 
  filings,
  onSelectPeriod 
}) => {
  const getStatusConfig = (filing: WithholdingReturnData) => {
    const isPaid = filing.paymentStatus === 'PAID';
    const isLate = filing.penalty > 0 || filing.interest > 0;

    if (isPaid && !isLate) {
      return {
        label: 'Paid',
        icon: CheckCircle,
        color: 'text-green-600 bg-green-50 border-green-200',
      };
    } else if (isPaid && isLate) {
      return {
        label: 'Paid Late',
        icon: AlertTriangle,
        color: 'text-yellow-600 bg-yellow-50 border-yellow-200',
      };
    } else if (!isPaid && isLate) {
      return {
        label: 'Overdue',
        icon: AlertTriangle,
        color: 'text-red-600 bg-red-50 border-red-200',
      };
    } else {
      return {
        label: 'Pending',
        icon: Clock,
        color: 'text-blue-600 bg-blue-50 border-blue-200',
      };
    }
  };

  const calculateCumulativeTotals = (upToIndex: number) => {
    let cumulativeWages = 0;
    let cumulativeTax = 0;

    for (let i = 0; i <= upToIndex; i++) {
      cumulativeWages += filings[i].grossWages;
      cumulativeTax += filings[i].taxDue;
    }

    return { cumulativeWages, cumulativeTax };
  };

  if (filings.length === 0) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-xl p-8 text-center">
        <Calendar className="w-12 h-12 text-gray-400 mx-auto mb-3" />
        <h4 className="font-bold text-gray-700 mb-2">No W-1 Filings</h4>
        <p className="text-sm text-gray-600">
          No W-1 withholding returns have been filed for this period yet.
        </p>
      </div>
    );
  }

  // Sort filings by date
  const sortedFilings = [...filings].sort((a, b) => 
    new Date(a.period.startDate).getTime() - new Date(b.period.startDate).getTime()
  );

  return (
    <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
      {/* Header */}
      <div className="bg-gradient-to-r from-[#970bed] to-[#469fe8] p-4">
        <div className="flex items-center gap-2 text-white">
          <Calendar className="w-5 h-5" />
          <h3 className="font-bold text-lg">Filing Period History</h3>
        </div>
        <p className="text-sm text-white/80 mt-1">
          Complete history of W-1 filings with cumulative totals
        </p>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                Period
              </th>
              <th className="px-4 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                Due Date
              </th>
              <th className="px-4 py-3 text-right text-xs font-bold text-gray-600 uppercase tracking-wider">
                Gross Wages
              </th>
              <th className="px-4 py-3 text-right text-xs font-bold text-gray-600 uppercase tracking-wider">
                Tax Due
              </th>
              <th className="px-4 py-3 text-right text-xs font-bold text-gray-600 uppercase tracking-wider">
                Total Due
              </th>
              <th className="px-4 py-3 text-right text-xs font-bold text-gray-600 uppercase tracking-wider">
                Cumulative Wages
              </th>
              <th className="px-4 py-3 text-right text-xs font-bold text-gray-600 uppercase tracking-wider">
                Cumulative Tax
              </th>
              <th className="px-4 py-3 text-center text-xs font-bold text-gray-600 uppercase tracking-wider">
                Status
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {sortedFilings.map((filing, index) => {
              const statusConfig = getStatusConfig(filing);
              const StatusIcon = statusConfig.icon;
              const { cumulativeWages, cumulativeTax } = calculateCumulativeTotals(index);
              const isClickable = onSelectPeriod !== undefined;

              return (
                <tr 
                  key={filing.id}
                  className={`hover:bg-gray-50 transition-colors ${isClickable ? 'cursor-pointer' : ''}`}
                  onClick={() => isClickable && onSelectPeriod(filing)}
                >
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-gray-900">{filing.period.period}</span>
                      {filing.isReconciled && (
                        <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">
                          Reconciled
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-600">
                    {filing.period.dueDate}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-right font-mono text-sm text-gray-900">
                    ${filing.grossWages.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-right font-mono text-sm text-gray-900">
                    ${filing.taxDue.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-right font-mono text-sm font-medium text-gray-900">
                    ${filing.totalAmountDue.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-right font-mono text-sm font-bold text-[#970bed]">
                    ${cumulativeWages.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-right font-mono text-sm font-bold text-[#469fe8]">
                    ${cumulativeTax.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="flex items-center justify-center">
                      <span className={`inline-flex items-center gap-1.5 px-2 py-1 rounded-full text-xs font-medium border ${statusConfig.color}`}>
                        <StatusIcon className="w-3.5 h-3.5" />
                        {statusConfig.label}
                      </span>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Footer Summary */}
      <div className="bg-gray-50 border-t border-gray-200 p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <DollarSign className="w-4 h-4" />
            <span>Total Filings: <span className="font-bold text-gray-900">{filings.length}</span></span>
          </div>
          <div className="flex items-center gap-6 text-sm">
            <div>
              <span className="text-gray-600">Total Wages YTD: </span>
              <span className="font-mono font-bold text-[#970bed]">
                ${sortedFilings.reduce((sum, f) => sum + f.grossWages, 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
              </span>
            </div>
            <div>
              <span className="text-gray-600">Total Tax YTD: </span>
              <span className="font-mono font-bold text-[#469fe8]">
                ${sortedFilings.reduce((sum, f) => sum + f.taxDue, 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PeriodHistoryTable;
