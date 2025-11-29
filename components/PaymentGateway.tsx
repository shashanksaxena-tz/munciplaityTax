
import React, { useState } from 'react';
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

const TEST_CARDS = [
  { number: '4111-1111-1111-1111', type: 'Visa', result: 'Approved ✓', color: 'text-green-600' },
  { number: '5555-5555-5555-4444', type: 'Mastercard', result: 'Approved ✓', color: 'text-green-600' },
  { number: '378282246310005', type: 'Amex', result: 'Approved ✓', color: 'text-green-600' },
  { number: '4000-0000-0000-0002', type: 'Visa', result: 'Declined (Insufficient Funds)', color: 'text-red-600' },
  { number: '4000-0000-0000-0119', type: 'Visa', result: 'Error (Processing)', color: 'text-orange-600' },
];

const TEST_ACH_ACCOUNTS = [
  { routing: '110000000', account: '000123456789', result: 'Approved ✓', color: 'text-green-600' },
  { routing: '110000000', account: '000111111113', result: 'Declined (Insufficient Funds)', color: 'text-red-600' },
];

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
  const [errorMessage, setErrorMessage] = useState<string>('');
  
  // Form fields
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardCvv, setCardCvv] = useState('');
  const [achRouting, setAchRouting] = useState('');
  const [achAccount, setAchAccount] = useState('');

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
          cardExpiration: cardExpiry,
          cardCvv: cardCvv,
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
       <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
         <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full animate-fadeIn">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4 animate-bounce">
              <CheckCircle className="w-8 h-8 text-green-600" />
            </div>
            <h3 className="text-xl font-bold text-slate-800 text-center">Payment Successful!</h3>
            <p className="text-slate-500 mt-2 text-center">Your payment of <strong>${amount.toLocaleString()}</strong> has been processed.</p>
            
            {/* Receipt Details */}
            <div className="mt-6 bg-slate-50 rounded-lg p-4 space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-600">Receipt Number:</span>
                <span className="font-mono font-bold text-slate-800">{paymentResponse.receiptNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">Transaction ID:</span>
                <span className="font-mono text-slate-800">{paymentResponse.providerTransactionId}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">Amount:</span>
                <span className="font-bold text-slate-800">${paymentResponse.amount.toFixed(2)}</span>
              </div>
              {paymentResponse.authorizationCode && (
                <div className="flex justify-between">
                  <span className="text-slate-600">Authorization:</span>
                  <span className="font-mono text-slate-800">{paymentResponse.authorizationCode}</span>
                </div>
              )}
              {paymentResponse.journalEntryId && (
                <div className="flex justify-between">
                  <span className="text-slate-600">Journal Entry:</span>
                  <span className="font-mono text-xs text-slate-600">{paymentResponse.journalEntryId}</span>
                </div>
              )}
              {paymentResponse.testMode && (
                <div className="flex justify-center pt-2 border-t border-slate-200">
                  <span className="text-xs text-orange-600 font-bold">⚠️ TEST MODE - No Real Charges</span>
                </div>
              )}
            </div>
         </div>
       </div>
     );
  }

  if (step === 'ERROR') {
    return (
      <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full animate-fadeIn">
          <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <AlertCircle className="w-8 h-8 text-red-600" />
          </div>
          <h3 className="text-xl font-bold text-slate-800 text-center">Payment Failed</h3>
          <p className="text-slate-600 mt-2 text-center">{errorMessage}</p>
          {paymentResponse?.testMode && (
            <p className="text-xs text-orange-600 text-center mt-2">⚠️ TEST MODE - No real charges attempted</p>
          )}
          <div className="flex gap-3 mt-6">
            <button onClick={onCancel} className="flex-1 py-3 border border-slate-300 rounded-xl font-medium text-slate-600 hover:bg-slate-50">
              Cancel
            </button>
            <button onClick={handleRetry} className="flex-1 py-3 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700">
              Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
       <div className="bg-white rounded-2xl shadow-2xl overflow-hidden max-w-md w-full animate-slideUp">
         {/* Header with TEST MODE indicator */}
         <div className="bg-slate-900 text-white p-6 relative">
            <div className="flex items-center justify-between mb-1">
              <h3 className="text-lg font-bold flex items-center gap-2">
                <Lock className="w-4 h-4 text-green-400" /> Secure Payment
              </h3>
              <div className="bg-orange-500 text-white text-xs font-bold px-3 py-1 rounded-full">
                TEST MODE
              </div>
            </div>
            <div className="flex items-center justify-between">
              <p className="text-slate-400 text-sm">Pay to: {recipient}</p>
              <div className="font-mono text-2xl font-bold">${amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
            </div>
            <div className="mt-2 text-xs text-orange-300 flex items-center gap-1">
              <Info className="w-3 h-3" />
              No real charges will be processed
            </div>
         </div>

         {step === 'PROCESSING' ? (
           <div className="p-12 text-center">
              <Loader2 className="w-12 h-12 text-indigo-600 animate-spin mx-auto mb-4" />
              <p className="font-medium text-slate-600">Processing Transaction...</p>
              <p className="text-xs text-slate-400 mt-2">Do not close this window.</p>
           </div>
         ) : (
           <form onSubmit={handlePay} className="p-6 space-y-6">
              {/* Method Switcher */}
              <div className="flex p-1 bg-slate-100 rounded-lg">
                 <button 
                   type="button" 
                   onClick={() => setMethod('CARD')}
                   className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${method === 'CARD' ? 'bg-white shadow text-indigo-600' : 'text-slate-500 hover:text-slate-700'}`}
                 >
                   Credit / Debit
                 </button>
                 <button 
                   type="button" 
                   onClick={() => setMethod('ACH')}
                   className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${method === 'ACH' ? 'bg-white shadow text-indigo-600' : 'text-slate-500 hover:text-slate-700'}`}
                 >
                   Bank Account (ACH)
                 </button>
              </div>

              {method === 'CARD' ? (
                <div className="space-y-4">
                   {/* Test Cards Helper */}
                   <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                     <button 
                       type="button"
                       onClick={() => setShowTestCards(!showTestCards)}
                       className="w-full flex items-center justify-between text-sm font-medium text-blue-800"
                     >
                       <span>📋 Test Card Numbers</span>
                       <span className="text-xs">{showTestCards ? '▲' : '▼'}</span>
                     </button>
                     {showTestCards && (
                       <div className="mt-3 space-y-2 text-xs">
                         {TEST_CARDS.map((card, idx) => (
                           <div key={idx} className="flex justify-between items-center py-1 border-b border-blue-100 last:border-0">
                             <span className="font-mono">{card.number}</span>
                             <span className={`font-medium ${card.color}`}>{card.result}</span>
                           </div>
                         ))}
                       </div>
                     )}
                   </div>

                   <div>
                     <label className="text-xs font-bold text-slate-500 uppercase">Card Number</label>
                     <div className="relative mt-1">
                       <CreditCard className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                       <input 
                         type="text" 
                         placeholder="0000 0000 0000 0000" 
                         value={cardNumber}
                         onChange={(e) => setCardNumber(e.target.value)}
                         className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" 
                         required 
                       />
                     </div>
                   </div>
                   <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="text-xs font-bold text-slate-500 uppercase">Expiry</label>
                        <input 
                          type="text" 
                          placeholder="MM/YY" 
                          value={cardExpiry}
                          onChange={(e) => setCardExpiry(e.target.value)}
                          className="w-full px-3 py-2 mt-1 border border-slate-300 rounded-lg outline-none" 
                          required 
                        />
                      </div>
                      <div>
                        <label className="text-xs font-bold text-slate-500 uppercase">CVC</label>
                        <input 
                          type="text" 
                          placeholder="123" 
                          value={cardCvv}
                          onChange={(e) => setCardCvv(e.target.value)}
                          className="w-full px-3 py-2 mt-1 border border-slate-300 rounded-lg outline-none" 
                          required 
                        />
                      </div>
                   </div>
                </div>
              ) : (
                <div className="space-y-4">
                   {/* Test ACH Helper */}
                   <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                     <button 
                       type="button"
                       onClick={() => setShowTestCards(!showTestCards)}
                       className="w-full flex items-center justify-between text-sm font-medium text-blue-800"
                     >
                       <span>📋 Test ACH Accounts</span>
                       <span className="text-xs">{showTestCards ? '▲' : '▼'}</span>
                     </button>
                     {showTestCards && (
                       <div className="mt-3 space-y-2 text-xs">
                         {TEST_ACH_ACCOUNTS.map((account, idx) => (
                           <div key={idx} className="py-1 border-b border-blue-100 last:border-0">
                             <div className="flex justify-between items-center">
                               <span className="font-mono">{account.routing} / {account.account}</span>
                               <span className={`font-medium ${account.color}`}>{account.result}</span>
                             </div>
                           </div>
                         ))}
                       </div>
                     )}
                   </div>

                   <div>
                     <label className="text-xs font-bold text-slate-500 uppercase">Routing Number</label>
                     <div className="relative mt-1">
                       <Building className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                       <input 
                         type="text" 
                         placeholder="9 Digits" 
                         value={achRouting}
                         onChange={(e) => setAchRouting(e.target.value)}
                         className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" 
                         required 
                       />
                     </div>
                   </div>
                   <div>
                     <label className="text-xs font-bold text-slate-500 uppercase">Account Number</label>
                     <input 
                       type="text" 
                       placeholder="Account #" 
                       value={achAccount}
                       onChange={(e) => setAchAccount(e.target.value)}
                       className="w-full px-3 py-2 mt-1 border border-slate-300 rounded-lg outline-none" 
                       required 
                     />
                   </div>
                </div>
              )}

              <div className="flex gap-3 pt-2">
                 <button type="button" onClick={onCancel} className="flex-1 py-3 border border-slate-300 rounded-xl font-medium text-slate-600 hover:bg-slate-50">Cancel</button>
                 <button type="submit" disabled={isProcessing} className="flex-1 py-3 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700 shadow-lg shadow-indigo-200 disabled:opacity-50">
                    Pay Now
                 </button>
              </div>
           </form>
         )}
       </div>
    </div>
  );
};
