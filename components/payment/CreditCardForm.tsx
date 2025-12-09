import React, { useRef } from 'react';
import { CreditCard } from 'lucide-react';
import { TestCreditCard } from './types';

interface CreditCardFormProps {
  cardNumber: string;
  cardExpiry: string;
  cardCvv: string;
  onCardNumberChange: (value: string) => void;
  onCardExpiryChange: (value: string) => void;
  onCardCvvChange: (value: string) => void;
  testCards?: TestCreditCard[];
  testMode?: boolean;
  isLoading?: boolean;
  disabled?: boolean;
}

// Helper function to map expected result to display text and color
const getResultDisplay = (result: string): { text: string; color: string } => {
  switch (result) {
    case 'APPROVED':
      return { text: 'Approved ✓', color: 'text-green-600' };
    case 'DECLINED':
      return { text: 'Declined (Insufficient Funds)', color: 'text-red-600' };
    case 'ERROR':
      return { text: 'Error (Processing)', color: 'text-orange-600' };
    default:
      return { text: result, color: 'text-slate-600' };
  }
};

/**
 * CreditCardForm - Credit card payment details input form with test card helpers
 */
export const CreditCardForm: React.FC<CreditCardFormProps> = ({
  cardNumber,
  cardExpiry,
  cardCvv,
  onCardNumberChange,
  onCardExpiryChange,
  onCardCvvChange,
  testCards = [],
  testMode = false,
  isLoading = false,
  disabled = false,
}) => {
  const [showTestCards, setShowTestCards] = React.useState(false);
  const expiryInputRef = useRef<HTMLInputElement>(null);

  const handleSelectTestCard = (card: TestCreditCard) => {
    onCardNumberChange(card.cardNumber);
    // Auto-focus expiry field after card auto-fill
    setTimeout(() => {
      expiryInputRef.current?.focus();
    }, 50);
  };

  return (
    <div className="space-y-4">
      {/* Test Cards Helper - only show if testMode is true */}
      {testMode && testCards.length > 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
          <button 
            type="button"
            onClick={() => setShowTestCards(!showTestCards)}
            disabled={disabled}
            className="w-full flex items-center justify-between text-sm font-medium text-blue-800 disabled:opacity-50"
          >
            <span>📋 Test Card Numbers (click to auto-fill)</span>
            <span className="text-xs">{showTestCards ? '▲' : '▼'}</span>
          </button>
          {showTestCards && (
            <div className="mt-3 space-y-2 text-xs">
              {isLoading && (
                <div className="text-slate-500 py-2">Loading test cards...</div>
              )}
              {!isLoading && testCards.map((card, idx) => {
                const result = getResultDisplay(card.expectedResult);
                return (
                  <div 
                    key={idx} 
                    onClick={() => !disabled && handleSelectTestCard(card)}
                    className={`flex justify-between items-center py-1 border-b border-blue-100 last:border-0 rounded px-1 transition-colors ${
                      disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-blue-100'
                    }`}
                    title={disabled ? '' : `Click to auto-fill: ${card.description}`}
                  >
                    <span className="font-mono">{card.cardNumber}</span>
                    <span className={`font-medium ${result.color}`}>{result.text}</span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Card Number */}
      <div>
        <label className="text-xs font-bold text-slate-500 uppercase">Card Number</label>
        <div className="relative mt-1">
          <CreditCard className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
          <input 
            type="text" 
            placeholder="0000 0000 0000 0000" 
            value={cardNumber}
            onChange={(e) => onCardNumberChange(e.target.value)}
            disabled={disabled}
            className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none disabled:bg-slate-100 disabled:cursor-not-allowed" 
            required 
            maxLength={19}
          />
        </div>
      </div>

      {/* Expiry and CVV */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="text-xs font-bold text-slate-500 uppercase">Expiry</label>
          <input 
            ref={expiryInputRef}
            type="text" 
            placeholder="MM/YY" 
            value={cardExpiry}
            onChange={(e) => onCardExpiryChange(e.target.value)}
            disabled={disabled}
            className="w-full px-3 py-2 mt-1 border border-slate-300 rounded-lg outline-none disabled:bg-slate-100 disabled:cursor-not-allowed" 
            required 
            maxLength={5}
          />
        </div>
        <div>
          <label className="text-xs font-bold text-slate-500 uppercase">CVC</label>
          <input 
            type="text" 
            placeholder="123" 
            value={cardCvv}
            onChange={(e) => onCardCvvChange(e.target.value)}
            disabled={disabled}
            className="w-full px-3 py-2 mt-1 border border-slate-300 rounded-lg outline-none disabled:bg-slate-100 disabled:cursor-not-allowed" 
            required 
            maxLength={4}
          />
        </div>
      </div>
    </div>
  );
};
