/**
 * PropertyFactorForm Component
 * Form for entering property factor data for apportionment calculation
 * Task: T117 [US4]
 */

import React from 'react';
import { Building2, Warehouse, Home } from 'lucide-react';
import type { PropertyFactorInput } from '../types/apportionment';

interface PropertyFactorFormProps {
  value: PropertyFactorInput;
  onChange: (value: PropertyFactorInput) => void;
  disabled?: boolean;
}

export function PropertyFactorForm({ value, onChange, disabled }: PropertyFactorFormProps) {
  const formatCurrency = (num: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(num);
  };

  const calculatePropertyFactor = () => {
    const ohioTotal = value.ohioPropertyValue + (value.rentedPropertyValue || 0) * 8;
    const totalTotal = value.totalPropertyValue + (value.totalRentedPropertyValue || 0) * 8;
    if (totalTotal === 0) return 0;
    return (ohioTotal / totalTotal) * 100;
  };

  const propertyFactor = calculatePropertyFactor();

  return (
    <div className="space-y-6">
      {/* Ohio Property Section */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <div className="flex items-center mb-4">
          <Building2 className="w-5 h-5 text-blue-600 mr-2" />
          <h3 className="text-lg font-semibold text-gray-900">Ohio Property</h3>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Owned Property Value (Real Estate + Equipment + Inventory)
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 text-gray-500">$</span>
              <input
                type="number"
                className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                value={value.ohioPropertyValue || ''}
                onChange={(e) => onChange({
                  ...value,
                  ohioPropertyValue: parseFloat(e.target.value) || 0
                })}
                disabled={disabled}
                placeholder="0"
              />
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Include all real property (land, buildings) and tangible personal property (equipment, inventory) owned in Ohio
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Rented Property Annual Rent (8x Multiplier Will Be Applied)
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 text-gray-500">$</span>
              <input
                type="number"
                className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                value={value.rentedPropertyValue || ''}
                onChange={(e) => onChange({
                  ...value,
                  rentedPropertyValue: parseFloat(e.target.value) || 0
                })}
                disabled={disabled}
                placeholder="0"
              />
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Annual rent paid for property in Ohio. This will be multiplied by 8 per Ohio apportionment rules.
            </p>
            {value.rentedPropertyValue > 0 && (
              <p className="text-xs text-blue-600 mt-1">
                Capitalized value: {formatCurrency((value.rentedPropertyValue || 0) * 8)}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Total Property Everywhere Section */}
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
        <div className="flex items-center mb-4">
          <Warehouse className="w-5 h-5 text-gray-600 mr-2" />
          <h3 className="text-lg font-semibold text-gray-900">Total Property Everywhere</h3>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Total Owned Property Value (All States)
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 text-gray-500">$</span>
              <input
                type="number"
                className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                value={value.totalPropertyValue || ''}
                onChange={(e) => onChange({
                  ...value,
                  totalPropertyValue: parseFloat(e.target.value) || 0
                })}
                disabled={disabled}
                placeholder="0"
              />
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Total property value across all states where your business operates
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Total Rented Property Annual Rent (All States)
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 text-gray-500">$</span>
              <input
                type="number"
                className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                value={value.totalRentedPropertyValue || ''}
                onChange={(e) => onChange({
                  ...value,
                  totalRentedPropertyValue: parseFloat(e.target.value) || 0
                })}
                disabled={disabled}
                placeholder="0"
              />
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Total annual rent paid for all rented property (8x multiplier will be applied)
            </p>
            {value.totalRentedPropertyValue > 0 && (
              <p className="text-xs text-blue-600 mt-1">
                Capitalized value: {formatCurrency((value.totalRentedPropertyValue || 0) * 8)}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Property Factor Calculation Display */}
      <div className="bg-green-50 border-2 border-green-200 rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center">
            <Home className="w-5 h-5 text-green-600 mr-2" />
            <h3 className="text-lg font-semibold text-gray-900">Property Factor Calculation</h3>
          </div>
          <span className="text-2xl font-bold text-green-600">
            {propertyFactor.toFixed(2)}%
          </span>
        </div>

        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-600">Ohio Owned Property:</span>
            <span className="font-medium">{formatCurrency(value.ohioPropertyValue || 0)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">Ohio Rented (8x):</span>
            <span className="font-medium">{formatCurrency((value.rentedPropertyValue || 0) * 8)}</span>
          </div>
          <div className="flex justify-between border-t pt-2">
            <span className="text-gray-900 font-medium">Ohio Total:</span>
            <span className="font-bold">{formatCurrency(value.ohioPropertyValue + (value.rentedPropertyValue || 0) * 8)}</span>
          </div>
          <div className="my-2 border-t"></div>
          <div className="flex justify-between">
            <span className="text-gray-600">Total Owned Property:</span>
            <span className="font-medium">{formatCurrency(value.totalPropertyValue || 0)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">Total Rented (8x):</span>
            <span className="font-medium">{formatCurrency((value.totalRentedPropertyValue || 0) * 8)}</span>
          </div>
          <div className="flex justify-between border-t pt-2">
            <span className="text-gray-900 font-medium">Total Everywhere:</span>
            <span className="font-bold">{formatCurrency(value.totalPropertyValue + (value.totalRentedPropertyValue || 0) * 8)}</span>
          </div>
        </div>

        <div className="mt-4 pt-4 border-t border-green-300">
          <p className="text-sm text-gray-700">
            <strong>Formula:</strong> Ohio Property / Total Property = {propertyFactor.toFixed(2)}%
          </p>
          <p className="text-xs text-gray-600 mt-1">
            Rented property is capitalized at 8 times annual rent per Ohio law
          </p>
        </div>
      </div>

      {/* Help Text */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <p className="text-sm text-blue-800">
          <strong>💡 Tip:</strong> The property factor represents the portion of your business property located in Ohio. 
          Include the average value of property during the tax year. Property should be valued at original cost (not depreciated).
        </p>
      </div>
    </div>
  );
}
