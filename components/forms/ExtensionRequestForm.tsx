/**
 * Extension Request Form Component
 * Form 27-EXT: Request 6-month extension with estimated tax payment
 */

import React, { useState } from 'react';
import { Calendar, DollarSign, FileText, AlertCircle } from 'lucide-react';
import { FormGenerationButton } from './FormGenerationButton';
import type { FormGenerationResponse } from '../../src/types/formTypes';

interface ExtensionRequestFormProps {
  returnId: string;
  businessId: string;
  tenantId: string;
  taxYear: number;
  businessName: string;
  fein: string;
  address?: string;
  onSuccess?: (response: FormGenerationResponse) => void;
  onError?: (error: any) => void;
}

export const ExtensionRequestForm: React.FC<ExtensionRequestFormProps> = ({
  returnId,
  businessId,
  tenantId,
  taxYear,
  businessName,
  fein,
  address = '',
  onSuccess,
  onError,
}) => {
  const [estimatedTax, setEstimatedTax] = useState<string>('');
  const [priorPayments, setPriorPayments] = useState<string>('');
  const [amountPaid, setAmountPaid] = useState<string>('');
  const [reason, setReason] = useState<string>('');
  
  const calculateBalanceDue = () => {
    const tax = parseFloat(estimatedTax) || 0;
    const payments = parseFloat(priorPayments) || 0;
    return Math.max(0, tax - payments);
  };

  const calculateExtendedDeadline = () => {
    const originalDeadline = new Date(taxYear, 3, 15); // April 15
    const extendedDeadline = new Date(taxYear, 9, 15); // October 15
    return extendedDeadline.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const getFormData = () => {
    return {
      businessName,
      fein,
      address,
      taxYear,
      estimatedTax: parseFloat(estimatedTax) || 0,
      priorPayments: parseFloat(priorPayments) || 0,
      balanceDue: calculateBalanceDue(),
      amountPaid: parseFloat(amountPaid) || 0,
      extensionReason: reason,
      preparerName: 'System Generated',
      prepareDate: new Date().toISOString().split('T')[0],
    };
  };

  const isFormValid = () => {
    return estimatedTax && parseFloat(estimatedTax) > 0 && reason.trim() !== '';
  };

  const balanceDue = calculateBalanceDue();
  const recommendedPayment = balanceDue;
  const paymentInsufficient = parseFloat(amountPaid) < balanceDue * 0.9;

  return (
    <div className="max-w-4xl mx-auto bg-white rounded-lg shadow-md p-6">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <Calendar className="w-6 h-6 text-blue-600" />
          Extension Request (Form 27-EXT)
        </h2>
        <p className="text-gray-600 mt-2">
          Request a 6-month extension to file your {taxYear} business tax return
        </p>
      </div>

      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
        <div className="flex items-start gap-3">
          <FileText className="w-5 h-5 text-blue-600 mt-0.5" />
          <div>
            <h3 className="font-semibold text-blue-900">Business Information</h3>
            <p className="text-sm text-blue-800">{businessName}</p>
            <p className="text-sm text-blue-700">FEIN: {fein}</p>
            {address && <p className="text-sm text-blue-700">{address}</p>}
          </div>
        </div>
      </div>

      <div className="space-y-4 mb-6">
        <div>
          <label htmlFor="estimatedTax" className="block text-sm font-medium text-gray-700 mb-1">
            Estimated Tax Liability <span className="text-red-500">*</span>
          </label>
          <div className="relative">
            <DollarSign className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="number"
              id="estimatedTax"
              value={estimatedTax}
              onChange={(e) => setEstimatedTax(e.target.value)}
              className="pl-10 w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="0.00"
              step="0.01"
              min="0"
              required
            />
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Your estimated total tax for {taxYear}
          </p>
        </div>

        <div>
          <label htmlFor="priorPayments" className="block text-sm font-medium text-gray-700 mb-1">
            Prior Payments Made
          </label>
          <div className="relative">
            <DollarSign className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="number"
              id="priorPayments"
              value={priorPayments}
              onChange={(e) => setPriorPayments(e.target.value)}
              className="pl-10 w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="0.00"
              step="0.01"
              min="0"
            />
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Total payments already made for {taxYear}
          </p>
        </div>

        <div className="bg-gray-50 p-4 rounded-lg">
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm font-medium text-gray-700">Balance Due:</span>
            <span className="text-lg font-bold text-gray-900">
              ${balanceDue.toFixed(2)}
            </span>
          </div>
          <p className="text-xs text-gray-600">
            Calculated: Estimated Tax - Prior Payments
          </p>
        </div>

        <div>
          <label htmlFor="amountPaid" className="block text-sm font-medium text-gray-700 mb-1">
            Amount Paying with Extension
          </label>
          <div className="relative">
            <DollarSign className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="number"
              id="amountPaid"
              value={amountPaid}
              onChange={(e) => setAmountPaid(e.target.value)}
              className="pl-10 w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="0.00"
              step="0.01"
              min="0"
            />
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Recommended: ${recommendedPayment.toFixed(2)} (100% of balance to avoid penalties)
          </p>
          {paymentInsufficient && parseFloat(amountPaid) > 0 && (
            <div className="flex items-start gap-2 mt-2 text-sm text-yellow-600">
              <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>
                Payment is less than 90% of estimated tax. You may incur late payment penalties.
              </span>
            </div>
          )}
        </div>

        <div>
          <label htmlFor="reason" className="block text-sm font-medium text-gray-700 mb-1">
            Reason for Extension <span className="text-red-500">*</span>
          </label>
          <select
            id="reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            required
          >
            <option value="">Select a reason...</option>
            <option value="More time needed">More time needed to prepare return</option>
            <option value="Awaiting K-1s">Awaiting Schedule K-1 forms</option>
            <option value="Awaiting documentation">Awaiting additional documentation</option>
            <option value="Other">Other</option>
          </select>
        </div>
      </div>

      <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
        <h3 className="font-semibold text-green-900 mb-2">Extended Filing Deadline</h3>
        <p className="text-green-800">
          If approved, your return will be due by <strong>{calculateExtendedDeadline()}</strong>
        </p>
        <p className="text-sm text-green-700 mt-1">
          6-month extension from the original April 15 deadline
        </p>
      </div>

      <div className="flex justify-end">
        <FormGenerationButton
          formCode="27-EXT"
          formName="Extension Request"
          taxYear={taxYear}
          returnId={returnId}
          businessId={businessId}
          tenantId={tenantId}
          formData={getFormData()}
          onSuccess={onSuccess}
          onError={onError}
          className={!isFormValid() ? 'opacity-50 cursor-not-allowed' : ''}
        />
      </div>

      {!isFormValid() && (
        <p className="text-sm text-red-600 mt-2 text-right">
          Please fill in all required fields (*)
        </p>
      )}
    </div>
  );
};

export default ExtensionRequestForm;
