import React, { useRef } from 'react';
import { CreditCard } from 'lucide-react';
import { TestCreditCard, getResultDisplay } from './types';

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
        <div className="bg-[#ebf4ff] border border-[#dfedff] rounded-lg p-3">
          <button 
            type="button"
            onClick={() => setShowTestCards(!showTestCards)}
            disabled={disabled}
            className="w-full flex items-center justify-between text-sm font-medium text-[#469fe8] disabled:opacity-50"
          >
            <span>📋 Test Card Numbers (click to auto-fill)</span>
            <span className="text-xs">{showTestCards ? '▲' : '▼'}</span>
          </button>
          {showTestCards && (
            <div className="mt-3 space-y-2 text-xs">
              {isLoading && (
                <div className="text-[#5d6567] py-2">Loading test cards...</div>
              )}
              {!isLoading && testCards.map((card, idx) => {
                const result = getResultDisplay(card.expectedResult);
                return (
                  <div 
                    key={idx} 
                    onClick={() => !disabled && handleSelectTestCard(card)}
                    className={`flex justify-between items-center py-1 border-b border-[#ebf4ff] last:border-0 rounded px-1 transition-colors ${
                      disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-[#dfedff]'
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
        <label className="text-xs font-bold text-[#102124] uppercase">Card Number</label>
        <div className="relative mt-1">
          <CreditCard className="absolute left-3 top-2.5 w-4 h-4 text-[#babebf]" />
          <input 
            type="text" 
            placeholder="0000 0000 0000 0000" 
            value={cardNumber}
            onChange={(e) => onCardNumberChange(e.target.value)}
            disabled={disabled}
            className="w-full pl-9 pr-3 py-2 border border-[#dcdede] rounded-xl focus:ring-2 focus:ring-[#970bed]/20 focus:border-[#970bed] outline-none disabled:bg-[#f0f0f0] disabled:cursor-not-allowed" 
            required 
            maxLength={19}
          />
        </div>
      </div>

      {/* Expiry and CVV */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="text-xs font-bold text-[#102124] uppercase">Expiry</label>
          <input 
            ref={expiryInputRef}
            type="text" 
            placeholder="MM/YY" 
            value={cardExpiry}
            onChange={(e) => onCardExpiryChange(e.target.value)}
            disabled={disabled}
            className="w-full px-3 py-2 mt-1 border border-[#dcdede] rounded-xl focus:ring-2 focus:ring-[#970bed]/20 focus:border-[#970bed] outline-none disabled:bg-[#f0f0f0] disabled:cursor-not-allowed" 
            required 
            maxLength={5}
          />
        </div>
        <div>
          <label className="text-xs font-bold text-[#102124] uppercase">CVC</label>
          <input 
            type="text" 
            placeholder="123" 
            value={cardCvv}
            onChange={(e) => onCardCvvChange(e.target.value)}
            disabled={disabled}
            className="w-full px-3 py-2 mt-1 border border-[#dcdede] rounded-xl focus:ring-2 focus:ring-[#970bed]/20 focus:border-[#970bed] outline-none disabled:bg-[#f0f0f0] disabled:cursor-not-allowed" 
            required 
            maxLength={4}
          />
        </div>
      </div>
    </div>
  );
};
