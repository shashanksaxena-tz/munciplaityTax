/**
 * PayrollFactorForm Component
 * Form for entering payroll factor data for apportionment calculation
 * Task: T118 [US4]
 */

import React from 'react';
import { Users, DollarSign, UserCheck } from 'lucide-react';
import type { PayrollFactorInput } from '../types/apportionment';

interface PayrollFactorFormProps {
  value: PayrollFactorInput;
  onChange: (value: PayrollFactorInput) => void;
  disabled?: boolean;
  autoPopulated?: boolean;
}

export function PayrollFactorForm({ value, onChange, disabled, autoPopulated }: PayrollFactorFormProps) {
  const formatCurrency = (num: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(num);
  };

  const calculatePayrollFactor = () => {
    if (value.totalPayroll === 0) return 0;
    return (value.ohioPayroll / value.totalPayroll) * 100;
  };

  const payrollFactor = calculatePayrollFactor();

  return (
    <div className="space-y-6">
      {/* Auto-Population Notice */}
      {autoPopulated && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="flex items-start">
            <UserCheck className="w-5 h-5 text-green-600 mt-0.5 mr-2 flex-shrink-0" />
            <div>
              <p className="text-sm font-medium text-green-800">
                Auto-populated from W-1 Withholding Data
              </p>
              <p className="text-xs text-green-700 mt-1">
                This payroll data has been automatically populated from your previously filed W-1 withholding forms. 
                You can override these values if needed.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Ohio Payroll Section */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <div className="flex items-center mb-4">
          <DollarSign className="w-5 h-5 text-blue-600 mr-2" />
          <h3 className="text-lg font-semibold text-gray-900">Ohio Payroll</h3>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Total Ohio Payroll
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 text-gray-500">$</span>
              <input
                type="number"
                className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                value={value.ohioPayroll || ''}
                onChange={(e) => onChange({
                  ...value,
                  ohioPayroll: parseFloat(e.target.value) || 0
                })}
                disabled={disabled}
                placeholder="0"
              />
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Include W-2 wages, officer compensation, and independent contractor payments for work performed in Ohio
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Ohio Employee Count (Optional)
            </label>
            <input
              type="number"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              value={value.ohioEmployeeCount || ''}
              onChange={(e) => onChange({
                ...value,
                ohioEmployeeCount: parseInt(e.target.value) || 0
              })}
              disabled={disabled}
              placeholder="0"
            />
            <p className="text-xs text-gray-500 mt-1">
              Number of employees performing work in Ohio (full-time equivalents)
            </p>
          </div>
        </div>
      </div>

      {/* Total Payroll Everywhere Section */}
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
        <div className="flex items-center mb-4">
          <Users className="w-5 h-5 text-gray-600 mr-2" />
          <h3 className="text-lg font-semibold text-gray-900">Total Payroll Everywhere</h3>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Total Payroll (All States)
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 text-gray-500">$</span>
              <input
                type="number"
                className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                value={value.totalPayroll || ''}
                onChange={(e) => onChange({
                  ...value,
                  totalPayroll: parseFloat(e.target.value) || 0
                })}
                disabled={disabled}
                placeholder="0"
              />
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Total compensation paid to all employees across all states where your business operates
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Total Employee Count (Optional)
            </label>
            <input
              type="number"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              value={value.totalEmployeeCount || ''}
              onChange={(e) => onChange({
                ...value,
                totalEmployeeCount: parseInt(e.target.value) || 0
              })}
              disabled={disabled}
              placeholder="0"
            />
            <p className="text-xs text-gray-500 mt-1">
              Total number of employees across all states (full-time equivalents)
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Remote Employee Count (Optional)
            </label>
            <input
              type="number"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              value={value.remoteEmployeeCount || ''}
              onChange={(e) => onChange({
                ...value,
                remoteEmployeeCount: parseInt(e.target.value) || 0
              })}
              disabled={disabled}
              placeholder="0"
            />
            <p className="text-xs text-gray-500 mt-1">
              Number of remote employees (allocated based on their home location)
            </p>
          </div>
        </div>
      </div>

      {/* Payroll Factor Calculation Display */}
      <div className="bg-green-50 border-2 border-green-200 rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center">
            <Users className="w-5 h-5 text-green-600 mr-2" />
            <h3 className="text-lg font-semibold text-gray-900">Payroll Factor Calculation</h3>
          </div>
          <span className="text-2xl font-bold text-green-600">
            {payrollFactor.toFixed(2)}%
          </span>
        </div>

        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-600">Ohio Payroll:</span>
            <span className="font-medium">{formatCurrency(value.ohioPayroll || 0)}</span>
          </div>
          {value.ohioEmployeeCount > 0 && (
            <div className="flex justify-between">
              <span className="text-gray-600">Ohio Employees:</span>
              <span className="font-medium">{value.ohioEmployeeCount} FTE</span>
            </div>
          )}
          <div className="my-2 border-t"></div>
          <div className="flex justify-between">
            <span className="text-gray-600">Total Payroll:</span>
            <span className="font-medium">{formatCurrency(value.totalPayroll || 0)}</span>
          </div>
          {value.totalEmployeeCount > 0 && (
            <div className="flex justify-between">
              <span className="text-gray-600">Total Employees:</span>
              <span className="font-medium">{value.totalEmployeeCount} FTE</span>
            </div>
          )}
          {value.remoteEmployeeCount > 0 && (
            <div className="flex justify-between">
              <span className="text-gray-600">Remote Employees:</span>
              <span className="font-medium">{value.remoteEmployeeCount} FTE</span>
            </div>
          )}
        </div>

        <div className="mt-4 pt-4 border-t border-green-300">
          <p className="text-sm text-gray-700">
            <strong>Formula:</strong> Ohio Payroll / Total Payroll = {payrollFactor.toFixed(2)}%
          </p>
          <p className="text-xs text-gray-600 mt-1">
            Payroll is allocated based on where the work is performed
          </p>
        </div>
      </div>

      {/* Help Text */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <p className="text-sm text-blue-800">
          <strong>💡 Tip:</strong> The payroll factor represents the portion of your compensation expense paid to employees 
          working in Ohio. Include W-2 wages, officer compensation, and amounts paid to independent contractors. 
          For remote employees, use their primary work location.
        </p>
      </div>

      {/* Validation Warnings */}
      {value.ohioPayroll > value.totalPayroll && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-sm text-red-800">
            ⚠️ <strong>Warning:</strong> Ohio payroll cannot exceed total payroll. Please verify your entries.
          </p>
        </div>
      )}

      {value.ohioEmployeeCount && value.totalEmployeeCount && value.ohioEmployeeCount > value.totalEmployeeCount && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-sm text-red-800">
            ⚠️ <strong>Warning:</strong> Ohio employee count cannot exceed total employee count. Please verify your entries.
          </p>
        </div>
      )}
    </div>
  );
}
