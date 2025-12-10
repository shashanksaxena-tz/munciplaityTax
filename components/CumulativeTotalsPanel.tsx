/**
 * Cumulative Totals Panel Component
 * Displays year-to-date cumulative totals for W-1 filings
 */

import React from 'react';
import { TrendingUp, DollarSign, Calendar, AlertCircle } from 'lucide-react';
import { WithholdingReturnData } from '../types';

interface CumulativeTotalsPanelProps {
  filings: WithholdingReturnData[];
  taxYear: number;
}

export const CumulativeTotalsPanel: React.FC<CumulativeTotalsPanelProps> = ({ 
  filings,
  taxYear 
}) => {
  // Calculate cumulative totals
  const totalGrossWages = filings.reduce((sum, f) => sum + f.grossWages, 0);
  const totalTaxDue = filings.reduce((sum, f) => sum + f.taxDue, 0);
  const totalAdjustments = filings.reduce((sum, f) => sum + f.adjustments, 0);
  const totalPenalties = filings.reduce((sum, f) => sum + f.penalty, 0);
  const totalInterest = filings.reduce((sum, f) => sum + f.interest, 0);
  const totalAmountDue = filings.reduce((sum, f) => sum + f.totalAmountDue, 0);
  
  // Payment status
  const paidFilings = filings.filter(f => f.paymentStatus === 'PAID');
  const unpaidFilings = filings.filter(f => f.paymentStatus === 'UNPAID');
  const totalPaid = paidFilings.reduce((sum, f) => sum + f.totalAmountDue, 0);
  const totalUnpaid = unpaidFilings.reduce((sum, f) => sum + f.totalAmountDue, 0);
  
  // Reconciliation status
  const reconciledCount = filings.filter(f => f.isReconciled).length;
  const unreconciledCount = filings.length - reconciledCount;

  // Calculate effective tax rate
  const effectiveTaxRate = totalGrossWages > 0 
    ? (totalTaxDue / totalGrossWages) * 100 
    : 0;

  return (
    <div className="space-y-4">
      {/* Main Totals Card */}
      <div className="bg-gradient-to-br from-[#970bed] to-[#469fe8] rounded-xl p-6 text-white shadow-lg">
        <div className="flex items-center gap-2 mb-4">
          <TrendingUp className="w-6 h-6" />
          <h3 className="font-bold text-xl">Year-to-Date Totals</h3>
        </div>
        
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
            <div className="text-sm text-white/80 mb-1">Total Gross Wages</div>
            <div className="text-3xl font-bold font-mono">
              ${totalGrossWages.toLocaleString(undefined, { minimumFractionDigits: 2 })}
            </div>
          </div>
          
          <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
            <div className="text-sm text-white/80 mb-1">Total Tax Due</div>
            <div className="text-3xl font-bold font-mono">
              ${totalTaxDue.toLocaleString(undefined, { minimumFractionDigits: 2 })}
            </div>
          </div>
        </div>

        <div className="mt-4 pt-4 border-t border-white/20 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Calendar className="w-4 h-4 text-white/80" />
            <span className="text-sm text-white/80">Tax Year {taxYear}</span>
          </div>
          <div className="text-sm">
            <span className="text-white/80">Effective Rate: </span>
            <span className="font-bold">{effectiveTaxRate.toFixed(2)}%</span>
          </div>
        </div>
      </div>

      {/* Detailed Breakdown */}
      <div className="grid grid-cols-2 gap-4">
        {/* Left Column */}
        <div className="space-y-4">
          {/* Adjustments & Penalties */}
          <div className="bg-white border border-gray-200 rounded-xl p-4">
            <h4 className="font-bold text-gray-900 mb-3 text-sm uppercase tracking-wide">
              Adjustments & Penalties
            </h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between items-center">
                <span className="text-gray-600">Adjustments</span>
                <span className={`font-mono font-medium ${totalAdjustments >= 0 ? 'text-gray-900' : 'text-red-600'}`}>
                  {totalAdjustments >= 0 ? '+' : ''}${totalAdjustments.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-600">Penalties</span>
                <span className={`font-mono font-medium ${totalPenalties > 0 ? 'text-red-600' : 'text-gray-900'}`}>
                  ${totalPenalties.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-600">Interest</span>
                <span className={`font-mono font-medium ${totalInterest > 0 ? 'text-red-600' : 'text-gray-900'}`}>
                  ${totalInterest.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </span>
              </div>
              <div className="pt-2 border-t border-gray-200 flex justify-between items-center">
                <span className="text-gray-900 font-bold">Total Amount Due</span>
                <span className="font-mono font-bold text-[#970bed]">
                  ${totalAmountDue.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </span>
              </div>
            </div>
          </div>

          {/* Payment Status */}
          <div className="bg-white border border-gray-200 rounded-xl p-4">
            <h4 className="font-bold text-gray-900 mb-3 text-sm uppercase tracking-wide">
              Payment Status
            </h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full bg-green-500"></div>
                  <span className="text-gray-600">Paid ({paidFilings.length})</span>
                </div>
                <span className="font-mono font-medium text-green-600">
                  ${totalPaid.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
                  <span className="text-gray-600">Unpaid ({unpaidFilings.length})</span>
                </div>
                <span className="font-mono font-medium text-yellow-600">
                  ${totalUnpaid.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column */}
        <div className="space-y-4">
          {/* Filing Statistics */}
          <div className="bg-white border border-gray-200 rounded-xl p-4">
            <h4 className="font-bold text-gray-900 mb-3 text-sm uppercase tracking-wide">
              Filing Statistics
            </h4>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-gray-600 text-sm">Total Filings</span>
                <span className="text-2xl font-bold text-gray-900">{filings.length}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-600 text-sm">Reconciled</span>
                <div className="flex items-center gap-2">
                  <span className="text-2xl font-bold text-green-600">{reconciledCount}</span>
                  {reconciledCount === filings.length && filings.length > 0 && (
                    <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded-full">
                      ✓ All
                    </span>
                  )}
                </div>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-600 text-sm">Unreconciled</span>
                <div className="flex items-center gap-2">
                  <span className={`text-2xl font-bold ${unreconciledCount > 0 ? 'text-yellow-600' : 'text-gray-400'}`}>
                    {unreconciledCount}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Warnings/Alerts */}
          {(totalPenalties > 0 || totalInterest > 0 || unreconciledCount > 0) && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4">
              <div className="flex items-start gap-2">
                <AlertCircle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <h4 className="font-bold text-yellow-900 text-sm mb-2">Attention Required</h4>
                  <ul className="text-sm text-yellow-800 space-y-1">
                    {totalPenalties > 0 && (
                      <li>• ${totalPenalties.toLocaleString(undefined, { minimumFractionDigits: 2 })} in penalties assessed</li>
                    )}
                    {totalInterest > 0 && (
                      <li>• ${totalInterest.toLocaleString(undefined, { minimumFractionDigits: 2 })} in interest charges</li>
                    )}
                    {unreconciledCount > 0 && (
                      <li>• {unreconciledCount} filing{unreconciledCount !== 1 ? 's' : ''} not yet reconciled</li>
                    )}
                    {totalUnpaid > 0 && (
                      <li>• ${totalUnpaid.toLocaleString(undefined, { minimumFractionDigits: 2 })} in unpaid taxes</li>
                    )}
                  </ul>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CumulativeTotalsPanel;
