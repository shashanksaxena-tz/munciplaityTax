/**
 * TrialBalance Component
 * T051: Create frontend TrialBalance.tsx component to display trial balance
 * T052: Add account hierarchy tree view to TrialBalance.tsx
 * T053: Add balance validation indicator (balanced/unbalanced) to TrialBalance.tsx
 * 
 * Implements FR-031-035: Trial balance generation and display
 */

import React, { useState, useEffect } from 'react';
import { CheckCircle, AlertTriangle, Loader2, ChevronDown, ChevronRight, Calendar, FileText } from 'lucide-react';

interface TrialBalanceProps {
  tenantId: string;
}

interface AccountBalanceSummary {
  accountNumber: string;
  accountName: string;
  accountType: string;
  debitBalance: number;
  creditBalance: number;
  netBalance: number;
  normalBalance: string;
}

interface TrialBalanceData {
  asOfDate: string;
  accounts: AccountBalanceSummary[];
  totalDebits: number;
  totalCredits: number;
  difference: number;
  isBalanced: boolean;  // T053: Balance validation
  status: 'BALANCED' | 'UNBALANCED';
  accountsByType: Record<string, AccountBalanceSummary[]>;  // T052: Hierarchy
  totalsByType: Record<string, number>;
  accountCount: number;
  tenantId: string;
  entityId: string;
  generatedAt: string;
}

export const TrialBalance: React.FC<TrialBalanceProps> = ({ tenantId }) => {
  const [trialBalance, setTrialBalance] = useState<TrialBalanceData | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [asOfDate, setAsOfDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [selectedPeriod, setSelectedPeriod] = useState<string>('CURRENT');
  const [expandedSections, setExpandedSections] = useState<Record<string, boolean>>({
    ASSET: true,
    LIABILITY: true,
    REVENUE: true,
    EXPENSE: true,
  });

  // Fetch trial balance on component mount or when date changes
  useEffect(() => {
    fetchTrialBalance();
  }, [tenantId, asOfDate]);

  const fetchTrialBalance = async () => {
    try {
      setIsLoading(true);
      setError('');
      
      const url = selectedPeriod === 'CURRENT'
        ? `/api/v1/trial-balance?tenantId=${tenantId}&asOfDate=${asOfDate}`
        : `/api/v1/trial-balance/period?tenantId=${tenantId}&year=${new Date(asOfDate).getFullYear()}&period=${selectedPeriod}`;
      
      const response = await fetch(url);
      
      if (!response.ok) {
        throw new Error('Failed to fetch trial balance');
      }
      
      const data: TrialBalanceData = await response.json();
      setTrialBalance(data);
    } catch (err: any) {
      console.error('Error fetching trial balance:', err);
      setError(err.message || 'An error occurred while fetching the trial balance');
    } finally {
      setIsLoading(false);
    }
  };

  const handlePeriodChange = (period: string) => {
    setSelectedPeriod(period);
    
    // Auto-calculate end date for period
    const year = new Date().getFullYear();
    let newDate: Date;
    
    switch (period) {
      case 'Q1':
        newDate = new Date(year, 2, 31); // March 31
        break;
      case 'Q2':
        newDate = new Date(year, 5, 30); // June 30
        break;
      case 'Q3':
        newDate = new Date(year, 8, 30); // September 30
        break;
      case 'Q4':
      case 'YEAR':
        newDate = new Date(year, 11, 31); // December 31
        break;
      default:
        newDate = new Date();
    }
    
    setAsOfDate(newDate.toISOString().split('T')[0]);
  };

  const toggleSection = (sectionName: string) => {
    setExpandedSections(prev => ({
      ...prev,
      [sectionName]: !prev[sectionName]
    }));
  };

  const formatCurrency = (amount: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
    }).format(amount);
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="bg-white rounded-lg shadow-lg border border-[#dcdede] p-8">
        <div className="flex items-center justify-center space-x-3">
          <Loader2 className="w-8 h-8 animate-spin text-[#469fe8]" />
          <span className="text-lg text-[#5d6567]">Loading trial balance...</span>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="bg-white rounded-lg shadow-lg border border-[#dcdede] p-8">
        <div className="flex items-center space-x-3 text-[#ec1656]">
          <AlertTriangle className="w-8 h-8" />
          <div>
            <h3 className="font-semibold text-lg">Error Loading Trial Balance</h3>
            <p className="text-sm">{error}</p>
          </div>
        </div>
        <button
          onClick={fetchTrialBalance}
          className="mt-4 bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition"
        >
          Retry
        </button>
      </div>
    );
  }

  if (!trialBalance) {
    return null;
  }

  return (
    <div className="bg-white rounded-lg shadow-lg">
      {/* Header */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-2xl font-bold text-gray-900 flex items-center">
            <FileText className="w-6 h-6 mr-2" />
            Trial Balance
          </h2>
          
          {/* T053: Balance validation indicator */}
          <div className="flex items-center space-x-2">
            {trialBalance.isBalanced ? (
              <div className="flex items-center space-x-2 bg-green-50 border border-green-200 px-4 py-2 rounded-lg">
                <CheckCircle className="w-5 h-5 text-green-600" />
                <span className="text-green-900 font-semibold">BALANCED</span>
              </div>
            ) : (
              <div className="flex items-center space-x-2 bg-red-50 border border-red-200 px-4 py-2 rounded-lg">
                <AlertTriangle className="w-5 h-5 text-red-600" />
                <span className="text-red-900 font-semibold">UNBALANCED</span>
              </div>
            )}
          </div>
        </div>

        {/* Filters and Date Selection */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              <Calendar className="w-4 h-4 inline mr-1" />
              Period
            </label>
            <select
              value={selectedPeriod}
              onChange={(e) => handlePeriodChange(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            >
              <option value="CURRENT">Current Date</option>
              <option value="Q1">Q1 (March 31)</option>
              <option value="Q2">Q2 (June 30)</option>
              <option value="Q3">Q3 (September 30)</option>
              <option value="Q4">Q4 (December 31)</option>
              <option value="YEAR">Year End (December 31)</option>
            </select>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              As of Date
            </label>
            <input
              type="date"
              value={asOfDate}
              onChange={(e) => setAsOfDate(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            />
          </div>
          
          <div className="flex items-end">
            <button
              onClick={fetchTrialBalance}
              className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
            >
              Refresh
            </button>
          </div>
        </div>

        {/* Summary Stats */}
        <div className="grid grid-cols-3 gap-4 mt-6">
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="text-sm text-gray-600 mb-1">Total Debits</div>
            <div className="text-2xl font-bold text-gray-900">{formatCurrency(trialBalance.totalDebits)}</div>
          </div>
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="text-sm text-gray-600 mb-1">Total Credits</div>
            <div className="text-2xl font-bold text-gray-900">{formatCurrency(trialBalance.totalCredits)}</div>
          </div>
          <div className={`p-4 rounded-lg ${trialBalance.isBalanced ? 'bg-green-50' : 'bg-red-50'}`}>
            <div className="text-sm text-gray-600 mb-1">Difference</div>
            <div className={`text-2xl font-bold ${trialBalance.isBalanced ? 'text-green-900' : 'text-red-900'}`}>
              {formatCurrency(trialBalance.difference)}
            </div>
          </div>
        </div>
      </div>

      {/* T052: Account Hierarchy Tree View */}
      <div className="p-6">
        <div className="space-y-6">
          {['ASSET', 'LIABILITY', 'REVENUE', 'EXPENSE'].map((accountType) => {
            const accounts = trialBalance.accountsByType[accountType] || [];
            const typeTotal = trialBalance.totalsByType[accountType] || 0;
            const isExpanded = expandedSections[accountType];
            
            if (accounts.length === 0) return null;
            
            return (
              <div key={accountType} className="border border-gray-200 rounded-lg overflow-hidden">
                {/* Section Header */}
                <button
                  onClick={() => toggleSection(accountType)}
                  className="w-full flex items-center justify-between p-4 bg-gray-50 hover:bg-gray-100 transition"
                >
                  <div className="flex items-center space-x-3">
                    {isExpanded ? (
                      <ChevronDown className="w-5 h-5 text-gray-600" />
                    ) : (
                      <ChevronRight className="w-5 h-5 text-gray-600" />
                    )}
                    <span className="font-bold text-lg text-gray-900">{accountType}</span>
                    <span className="text-sm text-gray-500">({accounts.length} accounts)</span>
                  </div>
                  <div className="font-bold text-lg text-gray-900">
                    {formatCurrency(typeTotal)}
                  </div>
                </button>
                
                {/* Accounts Table */}
                {isExpanded && (
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead className="bg-gray-100">
                        <tr>
                          <th className="px-4 py-2 text-left text-xs font-semibold text-gray-700">Account #</th>
                          <th className="px-4 py-2 text-left text-xs font-semibold text-gray-700">Account Name</th>
                          <th className="px-4 py-2 text-right text-xs font-semibold text-gray-700">Debit</th>
                          <th className="px-4 py-2 text-right text-xs font-semibold text-gray-700">Credit</th>
                          <th className="px-4 py-2 text-right text-xs font-semibold text-gray-700">Net Balance</th>
                        </tr>
                      </thead>
                      <tbody>
                        {accounts.map((account, index) => (
                          <tr
                            key={account.accountNumber}
                            className={index % 2 === 0 ? 'bg-white' : 'bg-gray-50'}
                          >
                            <td className="px-4 py-3 text-sm font-mono text-gray-900">
                              {account.accountNumber}
                            </td>
                            <td className="px-4 py-3 text-sm text-gray-900">
                              {account.accountName}
                            </td>
                            <td className="px-4 py-3 text-sm text-right font-mono text-gray-900">
                              {account.debitBalance > 0 ? formatCurrency(account.debitBalance) : '-'}
                            </td>
                            <td className="px-4 py-3 text-sm text-right font-mono text-gray-900">
                              {account.creditBalance > 0 ? formatCurrency(account.creditBalance) : '-'}
                            </td>
                            <td className="px-4 py-3 text-sm text-right font-mono font-semibold text-gray-900">
                              {formatCurrency(account.netBalance)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Totals Summary */}
        <div className="mt-8 border-t-2 border-gray-300 pt-6">
          <div className="grid grid-cols-3 gap-8 max-w-3xl ml-auto">
            <div className="text-right">
              <div className="text-sm text-gray-600 mb-2">Total Debits</div>
              <div className="text-xl font-bold text-gray-900">{formatCurrency(trialBalance.totalDebits)}</div>
            </div>
            <div className="text-right">
              <div className="text-sm text-gray-600 mb-2">Total Credits</div>
              <div className="text-xl font-bold text-gray-900">{formatCurrency(trialBalance.totalCredits)}</div>
            </div>
            <div className="text-right">
              <div className="text-sm text-gray-600 mb-2">Difference</div>
              <div className={`text-xl font-bold ${trialBalance.isBalanced ? 'text-green-600' : 'text-red-600'}`}>
                {formatCurrency(trialBalance.difference)}
              </div>
            </div>
          </div>
        </div>

        {/* Metadata Footer */}
        <div className="mt-6 pt-6 border-t border-gray-200 text-sm text-gray-500">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <span className="font-semibold">As of Date:</span> {new Date(trialBalance.asOfDate).toLocaleDateString()}
            </div>
            <div className="text-right">
              <span className="font-semibold">Generated:</span> {new Date(trialBalance.generatedAt).toLocaleString()}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TrialBalance;
