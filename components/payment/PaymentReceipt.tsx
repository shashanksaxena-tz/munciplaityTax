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
      <div className="fixed inset-0 bg-[#0f1012]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full animate-fadeIn">
          <div className="w-16 h-16 bg-[#d5faeb] rounded-full flex items-center justify-center mx-auto mb-4 animate-bounce">
            <CheckCircle className="w-8 h-8 text-[#10b981]" />
          </div>
          <h3 className="text-xl font-bold text-[#0f1012] text-center">Payment Successful!</h3>
          <p className="text-[#5d6567] mt-2 text-center">
            Your payment of <strong>${response.amount.toLocaleString()}</strong> has been processed.
          </p>
          
          {/* Receipt Details */}
          <div className="mt-6 bg-[#fbfbfb] rounded-xl p-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-[#5d6567]">Receipt Number:</span>
              <span className="font-mono font-bold text-[#0f1012]">{response.receiptNumber}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#5d6567]">Transaction ID:</span>
              <span className="font-mono text-[#0f1012]">{response.providerTransactionId}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#5d6567]">Amount:</span>
              <span className="font-bold text-[#0f1012]">${response.amount.toFixed(2)}</span>
            </div>
            {response.authorizationCode && (
              <div className="flex justify-between">
                <span className="text-[#5d6567]">Authorization:</span>
                <span className="font-mono text-[#0f1012]">{response.authorizationCode}</span>
              </div>
            )}
            {response.journalEntryId && (
              <div className="flex justify-between">
                <span className="text-[#5d6567]">Journal Entry:</span>
                <span className="font-mono text-xs text-[#5d6567]">{response.journalEntryId}</span>
              </div>
            )}
            {response.testMode && (
              <div className="flex justify-center pt-2 border-t border-[#dcdede]">
                <span className="text-xs text-[#f59e0b] font-bold">⚠️ TEST MODE - No Real Charges</span>
              </div>
            )}
          </div>

          {onClose && (
            <div className="mt-6">
              <button 
                onClick={onClose}
                className="w-full py-3 bg-[#10b981] hover:bg-[#059669] text-white rounded-xl font-bold shadow-lg shadow-[#10b981]/20 transition-all"
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
    <div className="fixed inset-0 bg-[#0f1012]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full animate-fadeIn">
        <div className="w-16 h-16 bg-[#ec1656]/10 rounded-full flex items-center justify-center mx-auto mb-4">
          <AlertCircle className="w-8 h-8 text-[#ec1656]" />
        </div>
        <h3 className="text-xl font-bold text-[#0f1012] text-center">Payment Failed</h3>
        <p className="text-[#5d6567] mt-2 text-center">
          {response.failureReason || 'The payment could not be processed.'}
        </p>
        {response.testMode && (
          <p className="text-xs text-[#f59e0b] text-center mt-2">⚠️ TEST MODE - No real charges attempted</p>
        )}
        <div className="flex gap-3 mt-6">
          {onClose && (
            <button 
              onClick={onClose} 
              className="flex-1 py-3 border border-[#dcdede] rounded-xl font-medium text-[#5d6567] hover:bg-[#fbfbfb] transition-colors"
            >
              Cancel
            </button>
          )}
          {onRetry && (
            <button 
              onClick={onRetry} 
              className="flex-1 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-bold shadow-lg shadow-[#970bed]/20 transition-all"
            >
              Try Again
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
