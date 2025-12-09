import React, { useRef } from 'react';
import { Building } from 'lucide-react';
import { TestACHAccount } from './types';

interface BankAccountFormProps {
  achRouting: string;
  achAccount: string;
  onAchRoutingChange: (value: string) => void;
  onAchAccountChange: (value: string) => void;
  testAccounts?: TestACHAccount[];
  testMode?: boolean;
  isLoading?: boolean;
  disabled?: boolean;
  onAutoFillComplete?: () => void;
}

// Helper function to map expected result to display text and color
const getResultDisplay = (result: string): { text: string; color: string } => {
  switch (result) {
    case 'APPROVED':
      return { text: 'Approved ✓', color: 'text-green-600' };
    case 'DECLINED':
      return { text: 'Declined (Insufficient Funds)', color: 'text-red-600' };
    default:
      return { text: result, color: 'text-slate-600' };
  }
};

/**
 * BankAccountForm - Bank account (ACH) payment details input form with test account helpers
 */
export const BankAccountForm: React.FC<BankAccountFormProps> = ({
  achRouting,
  achAccount,
  onAchRoutingChange,
  onAchAccountChange,
  testAccounts = [],
  testMode = false,
  isLoading = false,
  disabled = false,
  onAutoFillComplete,
}) => {
  const [showTestACH, setShowTestACH] = React.useState(false);

  const handleSelectTestACH = (account: TestACHAccount) => {
    onAchRoutingChange(account.routingNumber);
    onAchAccountChange(account.accountNumber);
    // Notify parent that auto-fill is complete
    if (onAutoFillComplete) {
      setTimeout(() => {
        onAutoFillComplete();
      }, 50);
    }
  };

  return (
    <div className="space-y-4">
      {/* Test ACH Helper - only show if testMode is true */}
      {testMode && testAccounts.length > 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
          <button 
            type="button"
            onClick={() => setShowTestACH(!showTestACH)}
            disabled={disabled}
            className="w-full flex items-center justify-between text-sm font-medium text-blue-800 disabled:opacity-50"
          >
            <span>📋 Test ACH Accounts (click to auto-fill)</span>
            <span className="text-xs">{showTestACH ? '▲' : '▼'}</span>
          </button>
          {showTestACH && (
            <div className="mt-3 space-y-2 text-xs">
              {isLoading && (
                <div className="text-slate-500 py-2">Loading test accounts...</div>
              )}
              {!isLoading && testAccounts.map((account, idx) => {
                const result = getResultDisplay(account.expectedResult);
                return (
                  <div 
                    key={idx} 
                    onClick={() => !disabled && handleSelectTestACH(account)}
                    className={`py-1 border-b border-blue-100 last:border-0 rounded px-1 transition-colors ${
                      disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-blue-100'
                    }`}
                    title={disabled ? '' : `Click to auto-fill: ${account.description}`}
                  >
                    <div className="flex justify-between items-center">
                      <span className="font-mono">{account.routingNumber} / {account.accountNumber}</span>
                      <span className={`font-medium ${result.color}`}>{result.text}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Routing Number */}
      <div>
        <label className="text-xs font-bold text-slate-500 uppercase">Routing Number</label>
        <div className="relative mt-1">
          <Building className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
          <input 
            type="text" 
            placeholder="9 Digits" 
            value={achRouting}
            onChange={(e) => onAchRoutingChange(e.target.value)}
            disabled={disabled}
            className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none disabled:bg-slate-100 disabled:cursor-not-allowed" 
            required 
            maxLength={9}
            pattern="[0-9]*"
          />
        </div>
      </div>

      {/* Account Number */}
      <div>
        <label className="text-xs font-bold text-slate-500 uppercase">Account Number</label>
        <input 
          type="text" 
          placeholder="Account #" 
          value={achAccount}
          onChange={(e) => onAchAccountChange(e.target.value)}
          disabled={disabled}
          className="w-full px-3 py-2 mt-1 border border-slate-300 rounded-lg outline-none disabled:bg-slate-100 disabled:cursor-not-allowed" 
          required 
          maxLength={17}
          pattern="[0-9]*"
        />
      </div>
    </div>
  );
};
