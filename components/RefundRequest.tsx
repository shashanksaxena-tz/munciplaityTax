/**
 * RefundRequest Component
 * T063: Create frontend RefundRequest.tsx component for filers to request refunds
 * T064: Add overpayment amount display to RefundRequest.tsx
 * T065: Add refund method selector to RefundRequest.tsx
 * 
 * Implements FR-036, FR-037, FR-041: Refund request with overpayment detection and method selection
 */

import React, { useState, useEffect } from 'react';
import { DollarSign, CheckCircle, AlertCircle, Loader2, Info, CreditCard } from 'lucide-react';

interface RefundRequestProps {
  filerId: string;
  tenantId: string;
  onSuccess: (refundResponse: RefundResponse) => void;
  onCancel: () => void;
}

interface RefundResponse {
  refundId: string;
  refundRequestId: string;
  filerId: string;
  amount: number;
  overpaymentAmount: number;  // T064: Overpayment display
  status: 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'ISSUED' | 'COMPLETED';
  refundMethod: 'ACH' | 'CHECK' | 'WIRE';  // T065: Method selection
  reason: string;
  journalEntryId: string;
  requestedAt: string;
  confirmationNumber: string;
  message: string;
}

export const RefundRequest: React.FC<RefundRequestProps> = ({
  filerId,
  tenantId,
  onSuccess,
  onCancel
}) => {
  const [overpaymentAmount, setOverpaymentAmount] = useState<number>(0);
  const [refundAmount, setRefundAmount] = useState<string>('');
  const [reason, setReason] = useState<string>('');
  const [refundMethod, setRefundMethod] = useState<'ACH' | 'CHECK' | 'WIRE'>('ACH');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [step, setStep] = useState<'FORM' | 'SUCCESS' | 'ERROR'>('FORM');
  const [refundResponse, setRefundResponse] = useState<RefundResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>('');
  
  // ACH fields
  const [achRouting, setAchRouting] = useState<string>('');
  const [achAccount, setAchAccount] = useState<string>('');
  const [achAccountHolder, setAchAccountHolder] = useState<string>('');
  
  // Check fields
  const [checkAddress, setCheckAddress] = useState<string>('');
  const [checkPayeeName, setCheckPayeeName] = useState<string>('');
  
  // Wire fields
  const [wireBankName, setWireBankName] = useState<string>('');
  const [wireRouting, setWireRouting] = useState<string>('');
  const [wireAccount, setWireAccount] = useState<string>('');
  const [wireSwift, setWireSwift] = useState<string>('');

  // T064: Fetch overpayment amount on component load
  useEffect(() => {
    const fetchOverpayment = async () => {
      try {
        setIsLoading(true);
        const response = await fetch(
          `/api/v1/refunds/overpayment?tenantId=${tenantId}&filerId=${filerId}`
        );
        
        if (!response.ok) {
          throw new Error('Failed to fetch overpayment amount');
        }
        
        const data = await response.json();
        setOverpaymentAmount(data.overpaymentAmount || 0);
      } catch (error) {
        console.error('Error fetching overpayment:', error);
        setErrorMessage('Unable to fetch overpayment amount. Please try again.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchOverpayment();
  }, [filerId, tenantId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setErrorMessage('');

    try {
      // Validate refund amount
      const amount = parseFloat(refundAmount);
      if (isNaN(amount) || amount <= 0) {
        throw new Error('Please enter a valid refund amount');
      }

      if (amount > overpaymentAmount) {
        throw new Error(`Refund amount cannot exceed overpayment amount of $${overpaymentAmount.toFixed(2)}`);
      }

      // Build refund request
      const refundRequest = {
        filerId,
        tenantId,
        amount,
        reason: reason || 'Overpayment refund request',
        refundMethod,
        requestedBy: filerId, // In real app, would be current user ID
        // Include method-specific fields
        ...(refundMethod === 'ACH' && {
          achRouting,
          achAccount,
          achAccountHolderName: achAccountHolder,
        }),
        ...(refundMethod === 'CHECK' && {
          checkMailingAddress: checkAddress,
          checkPayeeName,
        }),
        ...(refundMethod === 'WIRE' && {
          wireBankName,
          wireRoutingNumber: wireRouting,
          wireAccountNumber: wireAccount,
          wireSwiftCode: wireSwift,
        }),
      };

      const response = await fetch('/api/v1/refunds/request', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(refundRequest),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to submit refund request');
      }

      const data: RefundResponse = await response.json();
      setRefundResponse(data);
      setStep('SUCCESS');
      
      // Notify parent component
      setTimeout(() => {
        onSuccess(data);
      }, 3000);
    } catch (error: any) {
      console.error('Error submitting refund request:', error);
      setErrorMessage(error.message || 'An error occurred while submitting your refund request');
      setStep('ERROR');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    onCancel();
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="bg-white rounded-lg shadow-lg p-8">
        <div className="flex items-center justify-center space-x-3">
          <Loader2 className="w-6 h-6 animate-spin text-blue-600" />
          <span className="text-gray-600">Loading overpayment information...</span>
        </div>
      </div>
    );
  }

  // Success state
  if (step === 'SUCCESS' && refundResponse) {
    return (
      <div className="bg-white rounded-lg shadow-lg p-8">
        <div className="text-center">
          <CheckCircle className="w-16 h-16 text-green-600 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Refund Request Submitted</h2>
          <p className="text-gray-600 mb-6">{refundResponse.message}</p>
          
          <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
            <div className="text-sm text-gray-700 space-y-2">
              <div className="flex justify-between">
                <span className="font-semibold">Confirmation Number:</span>
                <span className="font-mono">{refundResponse.confirmationNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="font-semibold">Refund Amount:</span>
                <span>${refundResponse.amount.toFixed(2)}</span>
              </div>
              <div className="flex justify-between">
                <span className="font-semibold">Refund Method:</span>
                <span>{refundResponse.refundMethod}</span>
              </div>
              <div className="flex justify-between">
                <span className="font-semibold">Status:</span>
                <span className="text-yellow-600 font-semibold">{refundResponse.status}</span>
              </div>
            </div>
          </div>
          
          <p className="text-sm text-gray-600 mb-4">
            Your refund request is pending approval. You will receive a notification once it has been reviewed.
          </p>
          
          <button
            onClick={() => onSuccess(refundResponse)}
            className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 transition"
          >
            Close
          </button>
        </div>
      </div>
    );
  }

  // Error state
  if (step === 'ERROR') {
    return (
      <div className="bg-white rounded-lg shadow-lg p-8">
        <div className="text-center">
          <AlertCircle className="w-16 h-16 text-red-600 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Request Failed</h2>
          <p className="text-gray-600 mb-6">{errorMessage}</p>
          
          <div className="flex justify-center space-x-4">
            <button
              onClick={() => setStep('FORM')}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 transition"
            >
              Try Again
            </button>
            <button
              onClick={handleCancel}
              className="bg-gray-300 text-gray-700 px-6 py-2 rounded hover:bg-gray-400 transition"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Form state
  return (
    <div className="bg-white rounded-lg shadow-lg p-8 max-w-2xl mx-auto">
      <h2 className="text-2xl font-bold text-gray-900 mb-6">Request Refund</h2>
      
      {/* T064: Overpayment Display */}
      {overpaymentAmount > 0 ? (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <div className="flex items-start space-x-3">
            <Info className="w-5 h-5 text-blue-600 mt-0.5" />
            <div className="flex-1">
              <p className="text-sm font-semibold text-blue-900 mb-1">Available for Refund</p>
              <p className="text-2xl font-bold text-blue-900">${overpaymentAmount.toFixed(2)}</p>
              <p className="text-xs text-blue-700 mt-1">
                This is your current overpayment amount. You can request a refund up to this amount.
              </p>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
          <div className="flex items-start space-x-3">
            <AlertCircle className="w-5 h-5 text-yellow-600 mt-0.5" />
            <div>
              <p className="text-sm font-semibold text-yellow-900">No Overpayment Available</p>
              <p className="text-xs text-yellow-700 mt-1">
                You do not currently have any overpayment that can be refunded.
              </p>
            </div>
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Refund Amount */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Refund Amount <span className="text-red-500">*</span>
          </label>
          <div className="relative">
            <DollarSign className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
            <input
              type="number"
              value={refundAmount}
              onChange={(e) => setRefundAmount(e.target.value)}
              step="0.01"
              min="0"
              max={overpaymentAmount}
              required
              disabled={overpaymentAmount === 0}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100"
              placeholder="0.00"
            />
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Maximum: ${overpaymentAmount.toFixed(2)}
          </p>
        </div>

        {/* Reason */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Reason for Refund
          </label>
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={3}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Optional: Describe the reason for your refund request"
          />
        </div>

        {/* T065: Refund Method Selector */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Refund Method <span className="text-red-500">*</span>
          </label>
          <div className="grid grid-cols-3 gap-4">
            <button
              type="button"
              onClick={() => setRefundMethod('ACH')}
              className={`p-4 border-2 rounded-lg transition ${
                refundMethod === 'ACH'
                  ? 'border-blue-600 bg-blue-50'
                  : 'border-gray-300 hover:border-gray-400'
              }`}
            >
              <CreditCard className="w-6 h-6 mx-auto mb-2" />
              <div className="text-sm font-semibold">ACH Transfer</div>
              <div className="text-xs text-gray-500">2-3 business days</div>
            </button>
            
            <button
              type="button"
              onClick={() => setRefundMethod('CHECK')}
              className={`p-4 border-2 rounded-lg transition ${
                refundMethod === 'CHECK'
                  ? 'border-blue-600 bg-blue-50'
                  : 'border-gray-300 hover:border-gray-400'
              }`}
            >
              <CheckCircle className="w-6 h-6 mx-auto mb-2" />
              <div className="text-sm font-semibold">Check</div>
              <div className="text-xs text-gray-500">7-10 business days</div>
            </button>
            
            <button
              type="button"
              onClick={() => setRefundMethod('WIRE')}
              className={`p-4 border-2 rounded-lg transition ${
                refundMethod === 'WIRE'
                  ? 'border-blue-600 bg-blue-50'
                  : 'border-gray-300 hover:border-gray-400'
              }`}
            >
              <DollarSign className="w-6 h-6 mx-auto mb-2" />
              <div className="text-sm font-semibold">Wire Transfer</div>
              <div className="text-xs text-gray-500">Same day</div>
            </button>
          </div>
        </div>

        {/* Method-specific fields */}
        {refundMethod === 'ACH' && (
          <div className="space-y-4 bg-gray-50 p-4 rounded-lg">
            <h3 className="font-semibold text-gray-900">ACH Details</h3>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Account Holder Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={achAccountHolder}
                onChange={(e) => setAchAccountHolder(e.target.value)}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                placeholder="John Doe"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Routing Number <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={achRouting}
                onChange={(e) => setAchRouting(e.target.value)}
                required
                maxLength={9}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                placeholder="110000000"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Account Number <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={achAccount}
                onChange={(e) => setAchAccount(e.target.value)}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                placeholder="000123456789"
              />
            </div>
          </div>
        )}

        {refundMethod === 'CHECK' && (
          <div className="space-y-4 bg-gray-50 p-4 rounded-lg">
            <h3 className="font-semibold text-gray-900">Check Mailing Details</h3>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Payee Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={checkPayeeName}
                onChange={(e) => setCheckPayeeName(e.target.value)}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                placeholder="John Doe"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Mailing Address <span className="text-red-500">*</span>
              </label>
              <textarea
                value={checkAddress}
                onChange={(e) => setCheckAddress(e.target.value)}
                required
                rows={3}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                placeholder="123 Main St&#10;Dublin, OH 43017"
              />
            </div>
          </div>
        )}

        {refundMethod === 'WIRE' && (
          <div className="space-y-4 bg-gray-50 p-4 rounded-lg">
            <h3 className="font-semibold text-gray-900">Wire Transfer Details</h3>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Bank Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={wireBankName}
                onChange={(e) => setWireBankName(e.target.value)}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                placeholder="First National Bank"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Routing Number <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={wireRouting}
                  onChange={(e) => setWireRouting(e.target.value)}
                  required
                  maxLength={9}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  placeholder="110000000"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Account Number <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={wireAccount}
                  onChange={(e) => setWireAccount(e.target.value)}
                  required
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  placeholder="000123456789"
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                SWIFT Code (International) <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={wireSwift}
                onChange={(e) => setWireSwift(e.target.value)}
                required
                maxLength={11}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                placeholder="CHASUS33XXX"
              />
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex justify-end space-x-4 pt-4">
          <button
            type="button"
            onClick={handleCancel}
            className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
            disabled={isSubmitting}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting || overpaymentAmount === 0}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center space-x-2"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>Submitting...</span>
              </>
            ) : (
              <span>Submit Refund Request</span>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default RefundRequest;
