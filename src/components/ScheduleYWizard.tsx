/**
 * ScheduleYWizard Component
 * Multi-step wizard for filing Schedule Y (Apportionment)
 * Task: T123 [US4] - Enhanced to integrate all factor forms
 */

import React, { useState } from 'react';
import { SourcingElectionPanel } from './SourcingElectionPanel';
import { ThrowbackElectionPanel } from './ThrowbackElectionPanel';
import { ServiceSourcingPanel } from './ServiceSourcingPanel';
import { PropertyFactorForm } from './PropertyFactorForm';
import { PayrollFactorForm } from './PayrollFactorForm';
import { SalesFactorForm } from './SalesFactorForm';
import { ApportionmentBreakdownCard } from './ApportionmentBreakdownCard';
import { useScheduleY } from '../hooks/useScheduleY';
import type { ScheduleYRequest, ApportionmentBreakdown, PropertyFactorInput, PayrollFactorInput, SalesFactorInput } from '../types/apportionment';
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
  const [sourcingMethodElection, setSourcingMethodElection] = useState<SourcingMethodElection>('FINNIGAN' as SourcingMethodElection);
  const [throwbackElection, setThrowbackElection] = useState<ThrowbackElection>('THROWBACK' as ThrowbackElection);
  const [serviceSourcingMethod, setServiceSourcingMethod] = useState<ServiceSourcingMethod>('MARKET_BASED' as ServiceSourcingMethod);
  
  const [propertyFactor, setPropertyFactor] = useState<PropertyFactorInput>({
    ohioPropertyValue: 0,
    totalPropertyValue: 0,
    rentedPropertyValue: 0,
    totalRentedPropertyValue: 0
  });
  
  const [payrollFactor, setPayrollFactor] = useState<PayrollFactorInput>({
    ohioPayroll: 0,
    totalPayroll: 0,
    ohioEmployeeCount: 0,
    totalEmployeeCount: 0,
    remoteEmployeeCount: 0
  });
  
  const [salesFactor, setSalesFactor] = useState<SalesFactorInput>({
    ohioSales: 0,
    totalSales: 0,
    saleTransactions: []
  });

  const handleNext = () => {
    if (step < 6) {
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
      const request: ScheduleYRequest = {
        businessId: '', // Will be set by backend from auth context
        returnId,
        taxYear,
        apportionmentFormula: 'FOUR_FACTOR_DOUBLE_WEIGHTED_SALES' as any,
        sourcingMethodElection,
        throwbackElection,
        serviceSourcingMethod,
        propertyFactor,
        payrollFactor,
        salesFactor
      };
      
      const result = await createScheduleY(request);
      if (result.id) {
        await loadBreakdown(result.id);
        setStep(6); // Show results
        if (onComplete) {
          onComplete(result.id);
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
            { num: 1, label: 'Property Factor' },
            { num: 2, label: 'Payroll Factor' },
            { num: 3, label: 'Sales Factor' },
            { num: 4, label: 'Elections' },
            { num: 5, label: 'Review' },
            { num: 6, label: 'Results' }
          ].map((s, i) => (
            <React.Fragment key={s.num}>
              <div className="flex flex-col items-center">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center font-semibold text-sm ${
                  step >= s.num ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-600'
                }`}>
                  {s.num}
                </div>
                <div className="text-xs mt-2 text-gray-600 text-center max-w-[80px]">{s.label}</div>
              </div>
              {i < 5 && (
                <div className={`flex-1 h-1 mx-1 ${step > s.num ? 'bg-blue-600' : 'bg-gray-200'}`} />
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

        {/* Step 1: Property Factor */}
        {step === 1 && (
          <div>
            <h2 className="text-2xl font-bold mb-6">Step 1: Property Factor</h2>
            <PropertyFactorForm
              value={propertyFactor}
              onChange={setPropertyFactor}
              disabled={loading}
            />
          </div>
        )}

        {/* Step 2: Payroll Factor */}
        {step === 2 && (
          <div>
            <h2 className="text-2xl font-bold mb-6">Step 2: Payroll Factor</h2>
            <PayrollFactorForm
              value={payrollFactor}
              onChange={setPayrollFactor}
              disabled={loading}
              autoPopulated={false}
            />
          </div>
        )}

        {/* Step 3: Sales Factor */}
        {step === 3 && (
          <div>
            <h2 className="text-2xl font-bold mb-6">Step 3: Sales Factor</h2>
            <SalesFactorForm
              value={salesFactor}
              onChange={setSalesFactor}
              throwbackElection={throwbackElection}
              serviceSourcingMethod={serviceSourcingMethod}
              disabled={loading}
            />
          </div>
        )}

        {/* Step 4: Elections (Sourcing, Throwback, Service Sourcing) */}
        {step === 4 && (
          <div>
            <h2 className="text-2xl font-bold mb-6">Step 4: Sourcing Elections</h2>
            
            <div className="space-y-6">
              {/* Sourcing Method Election */}
              <div>
                <h3 className="text-lg font-semibold mb-3">Sales Factor Sourcing Method</h3>
                <SourcingElectionPanel
                  value={sourcingMethodElection}
                  onChange={setSourcingMethodElection}
                  disabled={loading}
                />
              </div>

              {/* Throwback Election */}
              <div>
                <h3 className="text-lg font-semibold mb-3">Throwback Rule</h3>
                <ThrowbackElectionPanel
                  value={throwbackElection}
                  onChange={setThrowbackElection}
                  disabled={loading}
                />
              </div>

              {/* Service Sourcing Method */}
              <div>
                <h3 className="text-lg font-semibold mb-3">Service Revenue Sourcing</h3>
                <ServiceSourcingPanel
                  value={serviceSourcingMethod}
                  onChange={setServiceSourcingMethod}
                  disabled={loading}
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 5: Review */}
        {step === 5 && (
          <div>
            <h2 className="text-2xl font-bold mb-6">Step 5: Review Your Entries</h2>
            
            <div className="space-y-6">
              {/* Property Factor Summary */}
              <div className="border border-gray-200 rounded-lg p-4">
                <h3 className="font-semibold text-lg mb-3">Property Factor</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-gray-600">Ohio Property:</span>
                    <span className="ml-2 font-medium">${propertyFactor.ohioPropertyValue.toLocaleString()}</span>
                  </div>
                  <div>
                    <span className="text-gray-600">Total Property:</span>
                    <span className="ml-2 font-medium">${propertyFactor.totalPropertyValue.toLocaleString()}</span>
                  </div>
                  {propertyFactor.rentedPropertyValue && propertyFactor.rentedPropertyValue > 0 && (
                    <>
                      <div>
                        <span className="text-gray-600">Ohio Rented:</span>
                        <span className="ml-2 font-medium">${propertyFactor.rentedPropertyValue.toLocaleString()}</span>
                      </div>
                      <div>
                        <span className="text-gray-600">Total Rented:</span>
                        <span className="ml-2 font-medium">${propertyFactor.totalRentedPropertyValue?.toLocaleString()}</span>
                      </div>
                    </>
                  )}
                </div>
              </div>

              {/* Payroll Factor Summary */}
              <div className="border border-gray-200 rounded-lg p-4">
                <h3 className="font-semibold text-lg mb-3">Payroll Factor</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-gray-600">Ohio Payroll:</span>
                    <span className="ml-2 font-medium">${payrollFactor.ohioPayroll.toLocaleString()}</span>
                  </div>
                  <div>
                    <span className="text-gray-600">Total Payroll:</span>
                    <span className="ml-2 font-medium">${payrollFactor.totalPayroll.toLocaleString()}</span>
                  </div>
                  <div>
                    <span className="text-gray-600">Ohio Employees:</span>
                    <span className="ml-2 font-medium">{payrollFactor.ohioEmployeeCount}</span>
                  </div>
                  <div>
                    <span className="text-gray-600">Total Employees:</span>
                    <span className="ml-2 font-medium">{payrollFactor.totalEmployeeCount}</span>
                  </div>
                </div>
              </div>

              {/* Sales Factor Summary */}
              <div className="border border-gray-200 rounded-lg p-4">
                <h3 className="font-semibold text-lg mb-3">Sales Factor</h3>
                <div className="text-sm">
                  <div className="mb-2">
                    <span className="text-gray-600">Total Transactions:</span>
                    <span className="ml-2 font-medium">{salesFactor.saleTransactions?.length || 0}</span>
                  </div>
                  {salesFactor.saleTransactions && salesFactor.saleTransactions.length > 0 && (
                    <div className="mt-3 max-h-48 overflow-y-auto">
                      <table className="w-full text-xs">
                        <thead className="bg-gray-50">
                          <tr>
                            <th className="px-2 py-1 text-left">Type</th>
                            <th className="px-2 py-1 text-right">Amount</th>
                            <th className="px-2 py-1 text-left">Destination</th>
                          </tr>
                        </thead>
                        <tbody>
                          {salesFactor.saleTransactions.slice(0, 5).map((txn, idx) => (
                            <tr key={idx} className="border-t">
                              <td className="px-2 py-1">{txn.saleType}</td>
                              <td className="px-2 py-1 text-right">${txn.amount.toLocaleString()}</td>
                              <td className="px-2 py-1">{txn.destinationState}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                      {salesFactor.saleTransactions.length > 5 && (
                        <div className="text-xs text-gray-500 mt-2">
                          ... and {salesFactor.saleTransactions.length - 5} more transactions
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>

              {/* Elections Summary */}
              <div className="border border-gray-200 rounded-lg p-4">
                <h3 className="font-semibold text-lg mb-3">Elections</h3>
                <div className="space-y-2 text-sm">
                  <div>
                    <span className="text-gray-600">Sourcing Method:</span>
                    <span className="ml-2 font-medium">{sourcingMethodElection}</span>
                  </div>
                  <div>
                    <span className="text-gray-600">Throwback Rule:</span>
                    <span className="ml-2 font-medium">{throwbackElection}</span>
                  </div>
                  <div>
                    <span className="text-gray-600">Service Sourcing:</span>
                    <span className="ml-2 font-medium">{serviceSourcingMethod}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 6: Results */}
        {step === 6 && breakdown && (
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
          {step < 5 && (
            <button
              onClick={handleNext}
              disabled={loading}
              className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          )}
          {step === 5 && (
            <button
              onClick={handleSubmit}
              disabled={loading}
              className="px-6 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Calculating...' : 'Calculate Apportionment'}
            </button>
          )}
          {step === 6 && (
            <button
              onClick={() => onComplete && onComplete(returnId)}
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
