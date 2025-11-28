/**
 * ScheduleYWizard Component
 * Multi-step wizard for filing Schedule Y (Apportionment)
 */

import React, { useState } from 'react';
import { SourcingElectionPanel } from './SourcingElectionPanel';
import { ApportionmentBreakdownCard } from './ApportionmentBreakdownCard';
import { useScheduleY } from '../hooks/useScheduleY';
import type { ScheduleYRequest, ApportionmentBreakdown } from '../types/apportionment';
import type { SourcingMethodElection, ThrowbackElection, ServiceSourcingMethod } from '../types/sourcing';

interface ScheduleYWizardProps {
  returnId: string;
  taxYear: number;
  onComplete?: (scheduleYId: string) => void;
}

export function ScheduleYWizard({ returnId, taxYear, onComplete }: ScheduleYWizardProps) {
  const [step, setStep] = useState(1);
  const { loading, error, createScheduleY, breakdown, loadBreakdown } = useScheduleY();

  // Form state
  const [formData, setFormData] = useState<Partial<ScheduleYRequest>>({
    returnId,
    taxYear,
    sourcingMethodElection: 'FINNIGAN' as SourcingMethodElection,
    throwbackElection: 'THROWBACK' as ThrowbackElection,
    serviceSourcingMethod: 'MARKET_BASED' as ServiceSourcingMethod,
    propertyFactor: {
      ohioRealProperty: 0,
      ohioTangiblePersonalProperty: 0,
      ohioRentedProperty: 0,
      totalPropertyEverywhere: 0
    },
    payrollFactor: {
      ohioW2Wages: 0,
      ohioContractorPayments: 0,
      ohioOfficerCompensation: 0,
      totalPayrollEverywhere: 0
    },
    salesFactor: {
      ohioSalesTangibleGoods: 0,
      ohioSalesServices: 0,
      totalSalesEverywhere: 0
    }
  });

  const handleNext = () => {
    if (step < 5) {
      setStep(step + 1);
    }
  };

  const handleBack = () => {
    if (step > 1) {
      setStep(step - 1);
    }
  };

  const handleSubmit = async () => {
    try {
      const result = await createScheduleY(formData as ScheduleYRequest);
      if (result.scheduleYId) {
        await loadBreakdown(result.scheduleYId);
        setStep(5); // Show results
        if (onComplete) {
          onComplete(result.scheduleYId);
        }
      }
    } catch (err) {
      console.error('Failed to submit Schedule Y:', err);
    }
  };

  return (
    <div className="max-w-4xl mx-auto">
      {/* Progress Steps */}
      <div className="mb-8">
        <div className="flex items-center justify-between">
          {[
            { num: 1, label: 'Sourcing Method' },
            { num: 2, label: 'Property Factor' },
            { num: 3, label: 'Payroll Factor' },
            { num: 4, label: 'Sales Factor' },
            { num: 5, label: 'Review' }
          ].map((s, i) => (
            <React.Fragment key={s.num}>
              <div className="flex flex-col items-center">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center font-semibold ${
                  step >= s.num ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-600'
                }`}>
                  {s.num}
                </div>
                <div className="text-xs mt-2 text-gray-600">{s.label}</div>
              </div>
              {i < 4 && (
                <div className={`flex-1 h-1 mx-2 ${step > s.num ? 'bg-blue-600' : 'bg-gray-200'}`} />
              )}
            </React.Fragment>
          ))}
        </div>
      </div>

      {/* Step Content */}
      <div className="bg-white rounded-lg shadow-lg p-8">
        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded text-red-700">
            {error}
          </div>
        )}

        {/* Step 1: Sourcing Method Election */}
        {step === 1 && (
          <div>
            <h2 className="text-2xl font-bold mb-6">Step 1: Choose Sourcing Method</h2>
            <SourcingElectionPanel
              value={formData.sourcingMethodElection!}
              onChange={(method) => setFormData({ ...formData, sourcingMethodElection: method })}
            />
          </div>
        )}

        {/* Step 2: Property Factor */}
        {step === 2 && (
          <div>
            <h2 className="text-2xl font-bold mb-6">Step 2: Property Factor</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Ohio Real Property (Land & Buildings)
                </label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  value={formData.propertyFactor?.ohioRealProperty || 0}
                  onChange={(e) => setFormData({
                    ...formData,
                    propertyFactor: {
                      ...formData.propertyFactor!,
                      ohioRealProperty: parseFloat(e.target.value) || 0
                    }
                  })}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Ohio Tangible Personal Property (Equipment, Inventory)
                </label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  value={formData.propertyFactor?.ohioTangiblePersonalProperty || 0}
                  onChange={(e) => setFormData({
                    ...formData,
                    propertyFactor: {
                      ...formData.propertyFactor!,
                      ohioTangiblePersonalProperty: parseFloat(e.target.value) || 0
                    }
                  })}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Total Property Everywhere
                </label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  value={formData.propertyFactor?.totalPropertyEverywhere || 0}
                  onChange={(e) => setFormData({
                    ...formData,
                    propertyFactor: {
                      ...formData.propertyFactor!,
                      totalPropertyEverywhere: parseFloat(e.target.value) || 0
                    }
                  })}
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 3: Payroll Factor */}
        {step === 3 && (
          <div>
            <h2 className="text-2xl font-bold mb-6">Step 3: Payroll Factor</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Ohio W-2 Wages
                </label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  value={formData.payrollFactor?.ohioW2Wages || 0}
                  onChange={(e) => setFormData({
                    ...formData,
                    payrollFactor: {
                      ...formData.payrollFactor!,
                      ohioW2Wages: parseFloat(e.target.value) || 0
                    }
                  })}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Total Payroll Everywhere
                </label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  value={formData.payrollFactor?.totalPayrollEverywhere || 0}
                  onChange={(e) => setFormData({
                    ...formData,
                    payrollFactor: {
                      ...formData.payrollFactor!,
                      totalPayrollEverywhere: parseFloat(e.target.value) || 0
                    }
                  })}
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 4: Sales Factor */}
        {step === 4 && (
          <div>
            <h2 className="text-2xl font-bold mb-6">Step 4: Sales Factor</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Ohio Sales (Tangible Goods)
                </label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  value={formData.salesFactor?.ohioSalesTangibleGoods || 0}
                  onChange={(e) => setFormData({
                    ...formData,
                    salesFactor: {
                      ...formData.salesFactor!,
                      ohioSalesTangibleGoods: parseFloat(e.target.value) || 0
                    }
                  })}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Ohio Sales (Services)
                </label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  value={formData.salesFactor?.ohioSalesServices || 0}
                  onChange={(e) => setFormData({
                    ...formData,
                    salesFactor: {
                      ...formData.salesFactor!,
                      ohioSalesServices: parseFloat(e.target.value) || 0
                    }
                  })}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Total Sales Everywhere
                </label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  value={formData.salesFactor?.totalSalesEverywhere || 0}
                  onChange={(e) => setFormData({
                    ...formData,
                    salesFactor: {
                      ...formData.salesFactor!,
                      totalSalesEverywhere: parseFloat(e.target.value) || 0
                    }
                  })}
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 5: Review & Results */}
        {step === 5 && breakdown && (
          <div>
            <h2 className="text-2xl font-bold mb-6">Schedule Y Results</h2>
            <ApportionmentBreakdownCard breakdown={breakdown} />
          </div>
        )}

        {/* Navigation Buttons */}
        <div className="mt-8 flex justify-between">
          <button
            onClick={handleBack}
            disabled={step === 1 || loading}
            className="px-6 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Back
          </button>
          {step < 4 && (
            <button
              onClick={handleNext}
              disabled={loading}
              className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          )}
          {step === 4 && (
            <button
              onClick={handleSubmit}
              disabled={loading}
              className="px-6 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Calculating...' : 'Calculate Apportionment'}
            </button>
          )}
          {step === 5 && (
            <button
              onClick={() => onComplete && onComplete(formData.returnId!)}
              className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              Complete
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
