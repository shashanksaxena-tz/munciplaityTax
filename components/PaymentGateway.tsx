
import React, { useState, useEffect, useRef } from 'react';
import { CreditCard, Lock, Building, CheckCircle, Loader2, AlertCircle, Info } from 'lucide-react';
import { PaymentRecord } from '../types';

interface PaymentGatewayProps {
  amount: number;
  recipient: string; // e.g., "City of Dublin"
  filerId: string;
  tenantId: string;
  onSuccess: (record: PaymentRecord) => void;
  onCancel: () => void;
}

interface PaymentResponse {
  status: 'APPROVED' | 'DECLINED' | 'ERROR';
  transactionId: string;
  providerTransactionId: string;
  authorizationCode?: string;
  receiptNumber?: string;
  journalEntryId?: string;
  failureReason?: string;
  amount: number;
  testMode: boolean;
  timestamp: string;
}

// TypeScript interfaces for test payment methods API response
interface TestCreditCard {
  cardNumber: string;
  cardType: 'VISA' | 'MASTERCARD' | 'AMEX';
  expectedResult: 'APPROVED' | 'DECLINED' | 'ERROR';
  description: string;
}

interface TestACHAccount {
  routingNumber: string;
  accountNumber: string;
  expectedResult: 'APPROVED' | 'DECLINED';
  description: string;
}

interface TestPaymentMethods {
  creditCards: TestCreditCard[];
  achAccounts: TestACHAccount[];
  testMode: boolean;
}

// Helper function to map expected result to display text and color
const getResultDisplay = (result: string): { text: string; color: string } => {
  switch (result) {
    case 'APPROVED':
      return { text: 'Approved ✓', color: 'text-[#10b981]' };
    case 'DECLINED':
      return { text: 'Declined (Insufficient Funds)', color: 'text-[#ec1656]' };
    case 'ERROR':
      return { text: 'Error (Processing)', color: 'text-orange-600' };
    default:
      return { text: result, color: 'text-[#5d6567]' };
  }
};

export const PaymentGateway: React.FC<PaymentGatewayProps> = ({ 
  amount, 
  recipient, 
  filerId,
  tenantId,
  onSuccess, 
  onCancel 
}) => {
  const [method, setMethod] = useState<'CARD' | 'ACH'>('CARD');
  const [isProcessing, setIsProcessing] = useState(false);
  const [step, setStep] = useState<'ENTRY' | 'PROCESSING' | 'SUCCESS' | 'ERROR'>('ENTRY');
  const [paymentResponse, setPaymentResponse] = useState<PaymentResponse | null>(null);
  const [showTestCards, setShowTestCards] = useState(false);
  const [showTestACH, setShowTestACH] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>('');
  
  // Test methods state (fetched from API)
  const [testMethods, setTestMethods] = useState<TestPaymentMethods | null>(null);
  const [testMethodsLoading, setTestMethodsLoading] = useState(true);
  const [testMethodsError, setTestMethodsError] = useState<string | null>(null);
  
  // Form fields
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardCvv, setCardCvv] = useState('');
  const [achRouting, setAchRouting] = useState('');
  const [achAccount, setAchAccount] = useState('');
  
  // Refs for auto-focus after auto-fill
  const expiryInputRef = useRef<HTMLInputElement>(null);
  const submitButtonRef = useRef<HTMLButtonElement>(null);

  // Fetch test methods from API on component mount
  useEffect(() => {
    const fetchTestMethods = async () => {
      try {
        setTestMethodsLoading(true);
        setTestMethodsError(null);
        const response = await fetch('/api/v1/payments/test-methods');
        if (!response.ok) {
          throw new Error('Failed to fetch test methods');
        }
        const data: TestPaymentMethods = await response.json();
        setTestMethods(data);
      } catch (error) {
        console.error('Error fetching test methods:', error);
        setTestMethodsError('Failed to load test methods. You can still enter card/account details manually.');
      } finally {
        setTestMethodsLoading(false);
      }
    };
    
    fetchTestMethods();
  }, []);

  // Auto-fill handlers for test cards and ACH accounts
  const handleSelectTestCard = (card: TestCreditCard) => {
    setCardNumber(card.cardNumber);
    // Auto-focus expiry field after card auto-fill
    setTimeout(() => {
      expiryInputRef.current?.focus();
    }, 50);
  };

  const handleSelectTestACH = (account: TestACHAccount) => {
    setAchRouting(account.routingNumber);
    setAchAccount(account.accountNumber);
    // Auto-focus submit button after ACH auto-fill (both fields filled)
    setTimeout(() => {
      submitButtonRef.current?.focus();
    }, 50);
  };

  const handlePay = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsProcessing(true);
    setStep('PROCESSING');
    setErrorMessage('');
    
    try {
      const paymentRequest = {
        filerId,
        tenantId,
        amount,
        paymentMethod: method === 'CARD' ? 'CREDIT_CARD' : 'ACH',
        description: `Tax payment to ${recipient}`,
        ...(method === 'CARD' ? {
          cardNumber,
          cvv: cardCvv,
          expirationMonth: parseInt(cardExpiry.split('/')[0]),
          expirationYear: parseInt('20' + cardExpiry.split('/')[1]),
        } : {
          achRouting,
          achAccount,
        })
      };
      
      const response = await fetch('/api/v1/payments/process', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(paymentRequest),
      });
      
      const data: PaymentResponse = await response.json();
      setPaymentResponse(data);
      setIsProcessing(false);
      
      if (data.status === 'APPROVED') {
        setStep('SUCCESS');
        setTimeout(() => {
          onSuccess({
            id: data.transactionId,
            date: data.timestamp,
            amount: data.amount,
            confirmationNumber: data.receiptNumber || data.providerTransactionId,
            method: method === 'CARD' ? 'CREDIT_CARD' : 'ACH',
            status: 'SUCCESS'
          });
        }, 2000);
      } else {
        setStep('ERROR');
        setErrorMessage(data.failureReason || 'Payment failed');
      }
    } catch (error) {
      setIsProcessing(false);
      setStep('ERROR');
      setErrorMessage('Network error. Please try again.');
    }
  };

  const handleRetry = () => {
    setStep('ENTRY');
    setPaymentResponse(null);
    setErrorMessage('');
  };

  if (step === 'SUCCESS' && paymentResponse) {
     return (
       <div className="fixed inset-0 bg-[#0f1012]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
         <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full animate-fadeIn">
            <div className="w-16 h-16 bg-[#d5faeb] rounded-full flex items-center justify-center mx-auto mb-4 animate-bounce">
              <CheckCircle className="w-8 h-8 text-[#10b981]" />
            </div>
            <h3 className="text-xl font-bold text-[#0f1012] text-center">Payment Successful!</h3>
            <p className="text-[#5d6567] mt-2 text-center">Your payment of <strong>${amount.toLocaleString()}</strong> has been processed.</p>
            
            {/* Receipt Details */}
            <div className="mt-6 bg-[#fbfbfb] rounded-lg p-4 space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-[#5d6567]">Receipt Number:</span>
                <span className="font-mono font-bold text-[#0f1012]">{paymentResponse.receiptNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#5d6567]">Transaction ID:</span>
                <span className="font-mono text-[#0f1012]">{paymentResponse.providerTransactionId}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#5d6567]">Amount:</span>
                <span className="font-bold text-[#0f1012]">${paymentResponse.amount.toFixed(2)}</span>
              </div>
              {paymentResponse.authorizationCode && (
                <div className="flex justify-between">
                  <span className="text-[#5d6567]">Authorization:</span>
                  <span className="font-mono text-[#0f1012]">{paymentResponse.authorizationCode}</span>
                </div>
              )}
              {paymentResponse.journalEntryId && (
                <div className="flex justify-between">
                  <span className="text-[#5d6567]">Journal Entry:</span>
                  <span className="font-mono text-xs text-[#5d6567]">{paymentResponse.journalEntryId}</span>
                </div>
              )}
              {paymentResponse.testMode && (
                <div className="flex justify-center pt-2 border-t border-[#dcdede]">
                  <span className="text-xs text-[#f59e0b] font-bold">⚠️ TEST MODE - No Real Charges</span>
                </div>
              )}
            </div>
         </div>
       </div>
     );
  }

  if (step === 'ERROR') {
    return (
      <div className="fixed inset-0 bg-[#0f1012]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full animate-fadeIn">
          <div className="w-16 h-16 bg-[#ec1656]/10 rounded-full flex items-center justify-center mx-auto mb-4">
            <AlertCircle className="w-8 h-8 text-[#ec1656]" />
          </div>
          <h3 className="text-xl font-bold text-[#0f1012] text-center">Payment Failed</h3>
          <p className="text-[#5d6567] mt-2 text-center">{errorMessage}</p>
          {paymentResponse?.testMode && (
            <p className="text-xs text-[#f59e0b] text-center mt-2">⚠️ TEST MODE - No real charges attempted</p>
          )}
          <div className="flex gap-3 mt-6">
            <button onClick={onCancel} className="flex-1 py-3 border border-[#dcdede] rounded-xl font-medium text-[#5d6567] hover:bg-[#fbfbfb]">
              Cancel
            </button>
            <button onClick={handleRetry} className="flex-1 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-bold">
              Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-[#0f1012]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
       <div className="bg-white rounded-2xl shadow-2xl overflow-hidden max-w-md w-full animate-slideUp">
         {/* Header with TEST MODE indicator - conditional on testMode */}
         <div className="bg-[#0f1012] text-white p-6 relative">
            <div className="flex items-center justify-between mb-1">
              <h3 className="text-lg font-bold flex items-center gap-2">
                <Lock className="w-4 h-4 text-[#10b981]" /> Secure Payment
              </h3>
              {testMethods?.testMode && (
                <div className="bg-[#f59e0b] text-white text-xs font-bold px-3 py-1 rounded-full">
                  TEST MODE
                </div>
              )}
            </div>
            <div className="flex items-center justify-between">
              <p className="text-[#babebf] text-sm">Pay to: {recipient}</p>
              <div className="font-mono text-2xl font-bold">${amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
            </div>
            {testMethods?.testMode && (
              <div className="mt-2 text-xs text-[#f59e0b]/80 flex items-center gap-1">
                <Info className="w-3 h-3" />
                No real charges will be processed
              </div>
            )}
         </div>

         {step === 'PROCESSING' ? (
           <div className="p-12 text-center">
              <Loader2 className="w-12 h-12 text-[#970bed] animate-spin mx-auto mb-4" />
              <p className="font-medium text-[#5d6567]">Processing Transaction...</p>
              <p className="text-xs text-[#babebf] mt-2">Do not close this window.</p>
           </div>
         ) : (
           <form onSubmit={handlePay} className="p-6 space-y-6">
              {/* Method Switcher */}
              <div className="flex p-1 bg-[#f0f0f0] rounded-lg">
                 <button 
                   type="button" 
                   onClick={() => setMethod('CARD')}
                   className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${method === 'CARD' ? 'bg-white shadow text-[#970bed]' : 'text-[#5d6567] hover:text-[#0f1012]'}`}
                 >
                   Credit / Debit
                 </button>
                 <button 
                   type="button" 
                   onClick={() => setMethod('ACH')}
                   className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${method === 'ACH' ? 'bg-white shadow text-[#970bed]' : 'text-[#5d6567] hover:text-[#0f1012]'}`}
                 >
                   Bank Account (ACH)
                 </button>
              </div>

              {method === 'CARD' ? (
                <div className="space-y-4">
                   {/* Test Cards Helper - only show if testMode is true */}
                   {testMethods?.testMode && (
                     <div className="bg-[#ebf4ff] border border-[#469fe8]/30 rounded-lg p-3">
                       <button 
                         type="button"
                         onClick={() => setShowTestCards(!showTestCards)}
                         className="w-full flex items-center justify-between text-sm font-medium text-[#469fe8]"
                       >
                         <span>📋 Test Card Numbers (click to auto-fill)</span>
                         <span className="text-xs">{showTestCards ? '▲' : '▼'}</span>
                       </button>
                       {showTestCards && (
                         <div className="mt-3 space-y-2 text-xs">
                           {testMethodsLoading && (
                             <div className="text-[#5d6567] py-2">Loading test cards...</div>
                           )}
                           {testMethodsError && (
                             <div className="text-[#ec1656] py-2">{testMethodsError}</div>
                           )}
                           {testMethods?.creditCards.map((card, idx) => {
                             const result = getResultDisplay(card.expectedResult);
                             return (
                               <div 
                                 key={idx} 
                                 onClick={() => handleSelectTestCard(card)}
                                 className="flex justify-between items-center py-1 border-b border-[#469fe8]/20 last:border-0 cursor-pointer hover:bg-[#469fe8]/10 rounded px-1 transition-colors"
                                 title={`Click to auto-fill: ${card.description}`}
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

                   <div>
                     <label className="text-xs font-bold text-[#102124] uppercase">Card Number</label>
                     <div className="relative mt-1">
                       <CreditCard className="absolute left-3 top-2.5 w-4 h-4 text-[#babebf]" />
                       <input 
                         type="text" 
                         placeholder="0000 0000 0000 0000" 
                         value={cardNumber}
                         onChange={(e) => setCardNumber(e.target.value)}
                         className="w-full pl-9 pr-3 py-2 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed]/20 focus:border-[#970bed] outline-none" 
                         required 
                       />
                     </div>
                   </div>
                   <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="text-xs font-bold text-[#102124] uppercase">Expiry</label>
                        <input 
                          ref={expiryInputRef}
                          type="text" 
                          placeholder="MM/YY" 
                          value={cardExpiry}
                          onChange={(e) => setCardExpiry(e.target.value)}
                          className="w-full px-3 py-2 mt-1 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed]/20 focus:border-[#970bed] outline-none" 
                          required 
                        />
                      </div>
                      <div>
                        <label className="text-xs font-bold text-[#102124] uppercase">CVC</label>
                        <input 
                          type="text" 
                          placeholder="123" 
                          value={cardCvv}
                          onChange={(e) => setCardCvv(e.target.value)}
                          className="w-full px-3 py-2 mt-1 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed]/20 focus:border-[#970bed] outline-none" 
                          required 
                        />
                      </div>
                   </div>
                </div>
              ) : (
                <div className="space-y-4">
                   {/* Test ACH Helper - only show if testMode is true */}
                   {testMethods?.testMode && (
                     <div className="bg-[#ebf4ff] border border-[#469fe8]/30 rounded-lg p-3">
                       <button 
                         type="button"
                         onClick={() => setShowTestACH(!showTestACH)}
                         className="w-full flex items-center justify-between text-sm font-medium text-[#469fe8]"
                       >
                         <span>📋 Test ACH Accounts (click to auto-fill)</span>
                         <span className="text-xs">{showTestACH ? '▲' : '▼'}</span>
                       </button>
                       {showTestACH && (
                         <div className="mt-3 space-y-2 text-xs">
                           {testMethodsLoading && (
                             <div className="text-[#5d6567] py-2">Loading test accounts...</div>
                           )}
                           {testMethodsError && (
                             <div className="text-[#ec1656] py-2">{testMethodsError}</div>
                           )}
                           {testMethods?.achAccounts.map((account, idx) => {
                             const result = getResultDisplay(account.expectedResult);
                             return (
                               <div 
                                 key={idx} 
                                 onClick={() => handleSelectTestACH(account)}
                                 className="py-1 border-b border-[#469fe8]/20 last:border-0 cursor-pointer hover:bg-[#469fe8]/10 rounded px-1 transition-colors"
                                 title={`Click to auto-fill: ${account.description}`}
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

                   <div>
                     <label className="text-xs font-bold text-[#102124] uppercase">Routing Number</label>
                     <div className="relative mt-1">
                       <Building className="absolute left-3 top-2.5 w-4 h-4 text-[#babebf]" />
                       <input 
                         type="text" 
                         placeholder="9 Digits" 
                         value={achRouting}
                         onChange={(e) => setAchRouting(e.target.value)}
                         className="w-full pl-9 pr-3 py-2 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed]/20 focus:border-[#970bed] outline-none" 
                         required 
                       />
                     </div>
                   </div>
                   <div>
                     <label className="text-xs font-bold text-[#102124] uppercase">Account Number</label>
                     <input 
                       type="text" 
                       placeholder="Account #" 
                       value={achAccount}
                       onChange={(e) => setAchAccount(e.target.value)}
                       className="w-full px-3 py-2 mt-1 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed]/20 focus:border-[#970bed] outline-none" 
                       required 
                     />
                   </div>
                </div>
              )}

              <div className="flex gap-3 pt-2">
                 <button type="button" onClick={onCancel} className="flex-1 py-3 border border-[#dcdede] rounded-xl font-medium text-[#5d6567] hover:bg-[#fbfbfb]">Cancel</button>
                 <button ref={submitButtonRef} type="submit" disabled={isProcessing} className="flex-1 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-bold shadow-lg shadow-[#970bed]/20 disabled:opacity-50">
                    Pay Now
                 </button>
              </div>
           </form>
         )}
       </div>
    </div>
  );
};
