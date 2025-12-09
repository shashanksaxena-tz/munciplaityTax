import React from 'react';
import { PaymentMethod } from './types';

interface PaymentMethodSelectorProps {
  selectedMethod: PaymentMethod;
  onMethodChange: (method: PaymentMethod) => void;
  disabled?: boolean;
}

/**
 * PaymentMethodSelector - Allows users to choose between credit card and ACH payment methods
 */
export const PaymentMethodSelector: React.FC<PaymentMethodSelectorProps> = ({
  selectedMethod,
  onMethodChange,
  disabled = false,
}) => {
  return (
    <div className="flex p-1 bg-slate-100 rounded-lg">
      <button 
        type="button" 
        onClick={() => onMethodChange('CARD')}
        disabled={disabled}
        className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${
          selectedMethod === 'CARD' 
            ? 'bg-white shadow text-indigo-600' 
            : 'text-slate-500 hover:text-slate-700'
        } disabled:opacity-50 disabled:cursor-not-allowed`}
      >
        Credit / Debit
      </button>
      <button 
        type="button" 
        onClick={() => onMethodChange('ACH')}
        disabled={disabled}
        className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${
          selectedMethod === 'ACH' 
            ? 'bg-white shadow text-indigo-600' 
            : 'text-slate-500 hover:text-slate-700'
        } disabled:opacity-50 disabled:cursor-not-allowed`}
      >
        Bank Account (ACH)
      </button>
    </div>
  );
};
