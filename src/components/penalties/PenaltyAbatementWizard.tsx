/**
 * PenaltyAbatementWizard Component
 * 
 * Multi-step form for penalty abatement request (FR-033 to FR-039).
 * Allows taxpayers to request penalty relief with supporting documentation.
 */

import React, { useState } from 'react';
import {
  AbatementType,
  AbatementReason,
  AbatementReasonLabels,
  AbatementRequest,
} from '../../types/abatement';
import { formatCurrency } from '../../utils/formatters';

export interface PenaltyAbatementWizardProps {
  returnId: string;
  penaltyId?: string;
  maxAbatementAmount: number;
  onSubmit: (request: AbatementRequest) => Promise<void>;
  onCancel: () => void;
}

type Step = 'type' | 'reason' | 'explanation' | 'review';

export const PenaltyAbatementWizard: React.FC<PenaltyAbatementWizardProps> = ({
  returnId,
  penaltyId,
  maxAbatementAmount,
  onSubmit,
  onCancel,
}) => {
  const [currentStep, setCurrentStep] = useState<Step>('type');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [formData, setFormData] = useState<Partial<AbatementRequest>>({
    returnId,
    penaltyId,
    abatementType: undefined,
    requestedAmount: maxAbatementAmount,
    reason: undefined,
    explanation: '',
  });

  const handleNext = () => {
    const steps: Step[] = ['type', 'reason', 'explanation', 'review'];
    const currentIndex = steps.indexOf(currentStep);
    if (currentIndex < steps.length - 1) {
      setCurrentStep(steps[currentIndex + 1]);
    }
  };

  const handleBack = () => {
    const steps: Step[] = ['type', 'reason', 'explanation', 'review'];
    const currentIndex = steps.indexOf(currentStep);
    if (currentIndex > 0) {
      setCurrentStep(steps[currentIndex - 1]);
    }
  };

  const handleSubmit = async () => {
    if (!formData.abatementType || !formData.reason || !formData.requestedAmount) {
      setError('Please complete all required fields');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await onSubmit(formData as AbatementRequest);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit abatement request');
    } finally {
      setSubmitting(false);
    }
  };

  const stepProgress = {
    type: 25,
    reason: 50,
    explanation: 75,
    review: 100,
  };

  return (
    <div className="bg-white shadow-lg rounded-lg overflow-hidden max-w-3xl mx-auto">
      {/* Header */}
      <div className="px-6 py-4 bg-blue-600 text-white">
        <h2 className="text-2xl font-bold">Request Penalty Abatement</h2>
        <p className="text-sm text-blue-100 mt-1">
          Complete this form to request relief from penalties
        </p>
      </div>

      {/* Progress Bar */}
      <div className="h-2 bg-gray-200">
        <div
          className="h-full bg-blue-600 transition-all duration-300"
          style={{ width: `${stepProgress[currentStep]}%` }}
        />
      </div>

      {/* Content */}
      <div className="px-6 py-8">
        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-md">
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}

        {/* Step 1: Abatement Type */}
        {currentStep === 'type' && (
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Step 1: Select Penalty Type
            </h3>
            <p className="text-sm text-gray-600 mb-4">
              Choose which penalty you want to request abatement for.
            </p>

            <div className="space-y-3">
              {Object.values(AbatementType).map((type) => (
                <label
                  key={type}
                  className={`flex items-center p-4 border-2 rounded-lg cursor-pointer transition-colors ${
                    formData.abatementType === type
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-blue-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="abatementType"
                    value={type}
                    checked={formData.abatementType === type}
                    onChange={(e) =>
                      setFormData({ ...formData, abatementType: e.target.value as AbatementType })
                    }
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500"
                  />
                  <span className="ml-3 text-sm font-medium text-gray-900">
                    {type.replace('_', ' ')}
                  </span>
                </label>
              ))}
            </div>

            <div className="mt-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Requested Abatement Amount
              </label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <span className="text-gray-500 sm:text-sm">$</span>
                </div>
                <input
                  type="number"
                  value={formData.requestedAmount}
                  onChange={(e) =>
                    setFormData({ ...formData, requestedAmount: parseFloat(e.target.value) })
                  }
                  max={maxAbatementAmount}
                  className="focus:ring-blue-500 focus:border-blue-500 block w-full pl-7 pr-12 sm:text-sm border-gray-300 rounded-md"
                />
              </div>
              <p className="mt-2 text-sm text-gray-500">
                Maximum: {formatCurrency(maxAbatementAmount)}
              </p>
            </div>
          </div>
        )}

        {/* Step 2: Abatement Reason */}
        {currentStep === 'reason' && (
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Step 2: Select Reason for Abatement
            </h3>
            <p className="text-sm text-gray-600 mb-4">
              Choose the reason that best describes why the penalty should be abated.
            </p>

            <div className="space-y-3">
              {Object.entries(AbatementReasonLabels).map(([key, label]) => (
                <label
                  key={key}
                  className={`flex items-start p-4 border-2 rounded-lg cursor-pointer transition-colors ${
                    formData.reason === key
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-blue-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="reason"
                    value={key}
                    checked={formData.reason === key}
                    onChange={(e) =>
                      setFormData({ ...formData, reason: e.target.value as AbatementReason })
                    }
                    className="h-4 w-4 mt-1 text-blue-600 focus:ring-blue-500"
                  />
                  <div className="ml-3">
                    <span className="text-sm font-medium text-gray-900">{label}</span>
                    {key === 'FIRST_TIME' && (
                      <p className="text-xs text-gray-500 mt-1">
                        Requires clean penalty history for past 3 years
                      </p>
                    )}
                  </div>
                </label>
              ))}
            </div>
          </div>
        )}

        {/* Step 3: Explanation */}
        {currentStep === 'explanation' && (
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Step 3: Provide Detailed Explanation
            </h3>
            <p className="text-sm text-gray-600 mb-4">
              Explain in detail why you believe the penalty should be abated. Include relevant
              facts, dates, and circumstances.
            </p>

            <textarea
              value={formData.explanation}
              onChange={(e) => setFormData({ ...formData, explanation: e.target.value })}
              rows={8}
              className="shadow-sm focus:ring-blue-500 focus:border-blue-500 block w-full sm:text-sm border-gray-300 rounded-md"
              placeholder="Provide a detailed explanation..."
            />

            <p className="mt-2 text-sm text-gray-500">
              Minimum 50 characters. Current: {formData.explanation?.length || 0}
            </p>
          </div>
        )}

        {/* Step 4: Review */}
        {currentStep === 'review' && (
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Step 4: Review Your Request
            </h3>
            <p className="text-sm text-gray-600 mb-6">
              Please review your abatement request before submitting.
            </p>

            <div className="bg-gray-50 rounded-lg p-6 space-y-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Penalty Type</p>
                <p className="text-base text-gray-900">
                  {formData.abatementType?.replace('_', ' ')}
                </p>
              </div>

              <div>
                <p className="text-sm font-medium text-gray-500">Requested Amount</p>
                <p className="text-base text-gray-900">
                  {formatCurrency(formData.requestedAmount || 0)}
                </p>
              </div>

              <div>
                <p className="text-sm font-medium text-gray-500">Reason</p>
                <p className="text-base text-gray-900">
                  {formData.reason && AbatementReasonLabels[formData.reason]}
                </p>
              </div>

              <div>
                <p className="text-sm font-medium text-gray-500">Explanation</p>
                <p className="text-base text-gray-900 whitespace-pre-wrap">
                  {formData.explanation}
                </p>
              </div>
            </div>

            <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-md">
              <p className="text-sm text-yellow-800">
                <strong>Note:</strong> After submission, your request will be reviewed by a tax
                administrator. You will be notified of the decision via email.
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Footer Actions */}
      <div className="px-6 py-4 bg-gray-50 border-t border-gray-200 flex justify-between">
        <button
          onClick={currentStep === 'type' ? onCancel : handleBack}
          className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
        >
          {currentStep === 'type' ? 'Cancel' : 'Back'}
        </button>

        {currentStep === 'review' ? (
          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="px-6 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            {submitting ? 'Submitting...' : 'Submit Request'}
          </button>
        ) : (
          <button
            onClick={handleNext}
            disabled={
              (currentStep === 'type' && !formData.abatementType) ||
              (currentStep === 'reason' && !formData.reason) ||
              (currentStep === 'explanation' && (formData.explanation?.length || 0) < 50)
            }
            className="px-6 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            Next
          </button>
        )}
      </div>
    </div>
  );
};

export default PenaltyAbatementWizard;
