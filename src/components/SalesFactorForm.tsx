/**
 * SalesFactorForm Component
 * Comprehensive form for entering sales factor data including throwback and service sourcing
 * Tasks: T119 [US4], T086 [US2], T100 [US3], T101 [US3]
 */

import React, { useState } from 'react';
import { TrendingUp, Package, Briefcase, MapPin, AlertTriangle, Plus, X } from 'lucide-react';
import type { SalesFactorInput, SaleTransactionInput } from '../types/apportionment';
import type { ThrowbackElection, ServiceSourcingMethod } from '../types/sourcing';

interface SalesFactorFormProps {
  value: SalesFactorInput;
  onChange: (value: SalesFactorInput) => void;
  throwbackElection: ThrowbackElection;
  serviceSourcingMethod: ServiceSourcingMethod;
  disabled?: boolean;
}

const US_STATES = [
  'AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA',
  'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD',
  'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NV', 'NH', 'NJ',
  'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI', 'SC',
  'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY'
];

export function SalesFactorForm({ 
  value, 
  onChange, 
  throwbackElection, 
  serviceSourcingMethod,
  disabled 
}: SalesFactorFormProps) {
  const [showTransactionEntry, setShowTransactionEntry] = useState(false);
  const [newTransaction, setNewTransaction] = useState<SaleTransactionInput>({
    saleType: 'TANGIBLE_GOODS',
    amount: 0,
    destinationState: 'OH',
    originState: 'OH',
    description: ''
  });

  const formatCurrency = (num: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(num);
  };

  const calculateSalesFactor = () => {
    if (value.totalSales === 0) return 0;
    return (value.ohioSales / value.totalSales) * 100;
  };

  const salesFactor = calculateSalesFactor();

  const addTransaction = () => {
    if (newTransaction.amount > 0) {
      onChange({
        ...value,
        saleTransactions: [...(value.saleTransactions || []), newTransaction]
      });
      setNewTransaction({
        saleType: 'TANGIBLE_GOODS',
        amount: 0,
        destinationState: 'OH',
        originState: 'OH',
        description: ''
      });
      setShowTransactionEntry(false);
    }
  };

  const removeTransaction = (index: number) => {
    onChange({
      ...value,
      saleTransactions: (value.saleTransactions || []).filter((_, i) => i !== index)
    });
  };

  return (
    <div className="space-y-6">
      {/* Sales Summary Section */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <div className="flex items-center mb-4">
          <TrendingUp className="w-5 h-5 text-blue-600 mr-2" />
          <h3 className="text-lg font-semibold text-gray-900">Ohio Sales</h3>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Total Ohio Sales
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 text-gray-500">$</span>
              <input
                type="number"
                className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                value={value.ohioSales || ''}
                onChange={(e) => onChange({
                  ...value,
                  ohioSales: parseFloat(e.target.value) || 0
                })}
                disabled={disabled}
                placeholder="0"
              />
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Sales sourced to Ohio based on your sourcing method election
            </p>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <Package className="inline w-4 h-4 mr-1" />
                Tangible Goods Sales
              </label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-gray-500">$</span>
                <input
                  type="number"
                  className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  value={value.tangibleGoodsSales || ''}
                  onChange={(e) => onChange({
                    ...value,
                    tangibleGoodsSales: parseFloat(e.target.value) || 0
                  })}
                  disabled={disabled}
                  placeholder="0"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <Briefcase className="inline w-4 h-4 mr-1" />
                Service Revenue
              </label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-gray-500">$</span>
                <input
                  type="number"
                  className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  value={value.serviceRevenue || ''}
                  onChange={(e) => onChange({
                    ...value,
                    serviceRevenue: parseFloat(e.target.value) || 0
                  })}
                  disabled={disabled}
                  placeholder="0"
                />
              </div>
            </div>
          </div>

          {/* Throwback Adjustment Display */}
          {throwbackElection === 'THROWBACK' && value.throwbackAdjustment > 0 && (
            <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
              <div className="flex items-start">
                <AlertTriangle className="w-4 h-4 text-yellow-600 mt-0.5 mr-2" />
                <div className="flex-1">
                  <p className="text-sm font-medium text-yellow-800">
                    Throwback Adjustment Applied
                  </p>
                  <p className="text-xs text-yellow-700 mt-1">
                    {formatCurrency(value.throwbackAdjustment || 0)} in sales to no-nexus states 
                    have been thrown back to Ohio
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Total Sales Everywhere Section */}
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
        <div className="flex items-center mb-4">
          <TrendingUp className="w-5 h-5 text-gray-600 mr-2" />
          <h3 className="text-lg font-semibold text-gray-900">Total Sales Everywhere</h3>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Total Sales (All States)
          </label>
          <div className="relative">
            <span className="absolute left-3 top-2.5 text-gray-500">$</span>
            <input
              type="number"
              className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              value={value.totalSales || ''}
              onChange={(e) => onChange({
                ...value,
                totalSales: parseFloat(e.target.value) || 0
              })}
              disabled={disabled}
              placeholder="0"
            />
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Total sales across all states where your business operates
          </p>
        </div>
      </div>

      {/* Detailed Transaction Entry Section */}
      <div className="border border-gray-200 rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center">
            <MapPin className="w-5 h-5 text-gray-600 mr-2" />
            <h3 className="text-lg font-semibold text-gray-900">Sales Transactions (Optional)</h3>
          </div>
          <button
            type="button"
            onClick={() => setShowTransactionEntry(!showTransactionEntry)}
            className="flex items-center px-3 py-1.5 text-sm bg-blue-600 text-white rounded hover:bg-blue-700"
            disabled={disabled}
          >
            <Plus className="w-4 h-4 mr-1" />
            Add Transaction
          </button>
        </div>

        <p className="text-sm text-gray-600 mb-4">
          Enter individual transactions for detailed tracking and automatic throwback/service sourcing calculation
        </p>

        {/* Transaction Entry Form */}
        {showTransactionEntry && (
          <div className="bg-gray-50 border border-gray-300 rounded-lg p-4 mb-4">
            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Sale Type
                  </label>
                  <select
                    className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded"
                    value={newTransaction.saleType}
                    onChange={(e) => setNewTransaction({
                      ...newTransaction,
                      saleType: e.target.value
                    })}
                  >
                    <option value="TANGIBLE_GOODS">Tangible Goods</option>
                    <option value="SERVICES">Services</option>
                    <option value="RENTAL_INCOME">Rental Income</option>
                    <option value="INTEREST">Interest</option>
                    <option value="ROYALTIES">Royalties</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Amount
                  </label>
                  <input
                    type="number"
                    className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded"
                    value={newTransaction.amount || ''}
                    onChange={(e) => setNewTransaction({
                      ...newTransaction,
                      amount: parseFloat(e.target.value) || 0
                    })}
                    placeholder="0.00"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Origin State
                  </label>
                  <select
                    className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded"
                    value={newTransaction.originState || 'OH'}
                    onChange={(e) => setNewTransaction({
                      ...newTransaction,
                      originState: e.target.value
                    })}
                  >
                    {US_STATES.map(state => (
                      <option key={state} value={state}>{state}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Destination State
                  </label>
                  <select
                    className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded"
                    value={newTransaction.destinationState}
                    onChange={(e) => setNewTransaction({
                      ...newTransaction,
                      destinationState: e.target.value
                    })}
                  >
                    {US_STATES.map(state => (
                      <option key={state} value={state}>{state}</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Customer Location for Services */}
              {newTransaction.saleType === 'SERVICES' && serviceSourcingMethod === 'MARKET_BASED' && (
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    <MapPin className="inline w-3 h-3 mr-1" />
                    Customer Location (Market-Based Sourcing)
                  </label>
                  <input
                    type="text"
                    className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded"
                    value={newTransaction.customerLocation || ''}
                    onChange={(e) => setNewTransaction({
                      ...newTransaction,
                      customerLocation: e.target.value
                    })}
                    placeholder="City, State or description of where customer receives benefit"
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    Enter where the customer receives the benefit of the service
                  </p>
                </div>
              )}

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Description (Optional)
                </label>
                <input
                  type="text"
                  className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded"
                  value={newTransaction.description || ''}
                  onChange={(e) => setNewTransaction({
                    ...newTransaction,
                    description: e.target.value
                  })}
                  placeholder="Brief description of transaction"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowTransactionEntry(false)}
                  className="px-3 py-1.5 text-sm border border-gray-300 rounded hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={addTransaction}
                  className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded hover:bg-blue-700"
                >
                  Add Transaction
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Transaction List */}
        {value.saleTransactions && value.saleTransactions.length > 0 && (
          <div className="space-y-2">
            {value.saleTransactions.map((transaction, index) => (
              <div key={index} className="flex items-start justify-between bg-white border border-gray-200 rounded p-3">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-gray-900">
                      {transaction.saleType.replace(/_/g, ' ')}
                    </span>
                    <span className="text-sm font-bold text-gray-900">
                      {formatCurrency(transaction.amount)}
                    </span>
                  </div>
                  <div className="text-xs text-gray-600 mt-1">
                    {transaction.originState} → {transaction.destinationState}
                    {transaction.customerLocation && ` | Customer: ${transaction.customerLocation}`}
                    {transaction.description && ` | ${transaction.description}`}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => removeTransaction(index)}
                  className="text-red-600 hover:text-red-800 ml-2"
                  disabled={disabled}
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            ))}
            <div className="text-xs text-gray-500 mt-2">
              Total transactions: {value.saleTransactions.length} | 
              Total amount: {formatCurrency(value.saleTransactions.reduce((sum, t) => sum + t.amount, 0))}
            </div>
          </div>
        )}

        {(!value.saleTransactions || value.saleTransactions.length === 0) && !showTransactionEntry && (
          <p className="text-sm text-gray-500 text-center py-4">
            No transactions entered. Add transactions for detailed tracking.
          </p>
        )}
      </div>

      {/* Sales Factor Calculation Display */}
      <div className="bg-green-50 border-2 border-green-200 rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center">
            <TrendingUp className="w-5 h-5 text-green-600 mr-2" />
            <h3 className="text-lg font-semibold text-gray-900">Sales Factor Calculation</h3>
          </div>
          <span className="text-2xl font-bold text-green-600">
            {salesFactor.toFixed(2)}%
          </span>
        </div>

        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-600">Ohio Sales:</span>
            <span className="font-medium">{formatCurrency(value.ohioSales || 0)}</span>
          </div>
          {value.throwbackAdjustment > 0 && (
            <div className="flex justify-between text-yellow-700">
              <span>+ Throwback Adjustment:</span>
              <span className="font-medium">{formatCurrency(value.throwbackAdjustment)}</span>
            </div>
          )}
          {value.tangibleGoodsSales > 0 && (
            <div className="flex justify-between text-gray-500 text-xs">
              <span>  • Tangible Goods:</span>
              <span>{formatCurrency(value.tangibleGoodsSales)}</span>
            </div>
          )}
          {value.serviceRevenue > 0 && (
            <div className="flex justify-between text-gray-500 text-xs">
              <span>  • Services ({serviceSourcingMethod}):</span>
              <span>{formatCurrency(value.serviceRevenue)}</span>
            </div>
          )}
          <div className="my-2 border-t"></div>
          <div className="flex justify-between">
            <span className="text-gray-600">Total Sales:</span>
            <span className="font-medium">{formatCurrency(value.totalSales || 0)}</span>
          </div>
        </div>

        <div className="mt-4 pt-4 border-t border-green-300">
          <p className="text-sm text-gray-700">
            <strong>Formula:</strong> Ohio Sales / Total Sales = {salesFactor.toFixed(2)}%
          </p>
          <p className="text-xs text-gray-600 mt-1">
            Sales factor is double-weighted in the four-factor formula
          </p>
        </div>
      </div>

      {/* Help Text */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <p className="text-sm text-blue-800">
          <strong>💡 Tip:</strong> The sales factor represents sales sourced to Ohio based on your sourcing method election. 
          For tangible goods, this is typically destination-based. For services, use the elected service sourcing method 
          ({serviceSourcingMethod === 'MARKET_BASED' ? 'market-based' : 
            serviceSourcingMethod === 'COST_OF_PERFORMANCE' ? 'cost-of-performance' : 'pro-rata'}).
        </p>
      </div>

      {/* Validation Warnings */}
      {value.ohioSales > value.totalSales && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-sm text-red-800">
            ⚠️ <strong>Warning:</strong> Ohio sales cannot exceed total sales. Please verify your entries.
          </p>
        </div>
      )}
    </div>
  );
}
