import React from 'react';
import { Loader2 } from 'lucide-react';

interface PaymentConfirmationProps {
  isProcessing: boolean;
  message?: string;
}

/**
 * PaymentConfirmation - Shows processing status during payment submission
 */
export const PaymentConfirmation: React.FC<PaymentConfirmationProps> = ({
  isProcessing,
  message = 'Processing Transaction...',
}) => {
  if (!isProcessing) {
    return null;
  }

  return (
    <div className="p-12 text-center">
      <Loader2 className="w-12 h-12 text-indigo-600 animate-spin mx-auto mb-4" />
      <p className="font-medium text-slate-600">{message}</p>
      <p className="text-xs text-slate-400 mt-2">Do not close this window.</p>
    </div>
  );
};
