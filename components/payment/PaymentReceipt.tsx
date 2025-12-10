import React from 'react';
import { CheckCircle, AlertCircle } from 'lucide-react';
import { PaymentResponse } from './types';

interface PaymentReceiptProps {
  response: PaymentResponse;
  amount: number;
  onRetry?: () => void;
  onClose?: () => void;
}

/**
 * PaymentReceipt - Displays payment success or failure details with transaction information
 */
export const PaymentReceipt: React.FC<PaymentReceiptProps> = ({
  response,
  amount,
  onRetry,
  onClose,
}) => {
  const isSuccess = response.status === 'APPROVED';

  if (isSuccess) {
    return (
      <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full animate-fadeIn">
          <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4 animate-bounce">
            <CheckCircle className="w-8 h-8 text-green-600" />
          </div>
          <h3 className="text-xl font-bold text-slate-800 text-center">Payment Successful!</h3>
          <p className="text-slate-500 mt-2 text-center">
            Your payment of <strong>${amount.toLocaleString()}</strong> has been processed.
          </p>
          
          {/* Receipt Details */}
          <div className="mt-6 bg-slate-50 rounded-lg p-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-slate-600">Receipt Number:</span>
              <span className="font-mono font-bold text-slate-800">{response.receiptNumber}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-600">Transaction ID:</span>
              <span className="font-mono text-slate-800">{response.providerTransactionId}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-600">Amount:</span>
              <span className="font-bold text-slate-800">${response.amount.toFixed(2)}</span>
            </div>
            {response.authorizationCode && (
              <div className="flex justify-between">
                <span className="text-slate-600">Authorization:</span>
                <span className="font-mono text-slate-800">{response.authorizationCode}</span>
              </div>
            )}
            {response.journalEntryId && (
              <div className="flex justify-between">
                <span className="text-slate-600">Journal Entry:</span>
                <span className="font-mono text-xs text-slate-600">{response.journalEntryId}</span>
              </div>
            )}
            {response.testMode && (
              <div className="flex justify-center pt-2 border-t border-slate-200">
                <span className="text-xs text-orange-600 font-bold">⚠️ TEST MODE - No Real Charges</span>
              </div>
            )}
          </div>

          {onClose && (
            <div className="mt-6">
              <button 
                onClick={onClose}
                className="w-full py-3 bg-green-600 text-white rounded-xl font-bold hover:bg-green-700 transition-colors"
              >
                Continue
              </button>
            </div>
          )}
        </div>
      </div>
    );
  }

  // Error/Declined state
  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full animate-fadeIn">
        <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <AlertCircle className="w-8 h-8 text-red-600" />
        </div>
        <h3 className="text-xl font-bold text-slate-800 text-center">Payment Failed</h3>
        <p className="text-slate-600 mt-2 text-center">
          {response.failureReason || 'The payment could not be processed.'}
        </p>
        {response.testMode && (
          <p className="text-xs text-orange-600 text-center mt-2">⚠️ TEST MODE - No real charges attempted</p>
        )}
        <div className="flex gap-3 mt-6">
          {onClose && (
            <button 
              onClick={onClose} 
              className="flex-1 py-3 border border-slate-300 rounded-xl font-medium text-slate-600 hover:bg-slate-50 transition-colors"
            >
              Cancel
            </button>
          )}
          {onRetry && (
            <button 
              onClick={onRetry} 
              className="flex-1 py-3 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700 transition-colors"
            >
              Try Again
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
