
import React, { useState } from 'react';
import { CreditCard, Lock, Building, CheckCircle, Loader2 } from 'lucide-react';
import { PaymentRecord } from '../types';

interface PaymentGatewayProps {
  amount: number;
  recipient: string; // e.g., "City of Dublin"
  onSuccess: (record: PaymentRecord) => void;
  onCancel: () => void;
}

export const PaymentGateway: React.FC<PaymentGatewayProps> = ({ amount, recipient, onSuccess, onCancel }) => {
  const [method, setMethod] = useState<'CARD' | 'ACH'>('CARD');
  const [isProcessing, setIsProcessing] = useState(false);
  const [step, setStep] = useState<'ENTRY' | 'PROCESSING' | 'SUCCESS'>('ENTRY');

  const handlePay = (e: React.FormEvent) => {
    e.preventDefault();
    setIsProcessing(true);
    setStep('PROCESSING');
    
    // Simulate API call
    setTimeout(() => {
      setIsProcessing(false);
      setStep('SUCCESS');
      
      setTimeout(() => {
        onSuccess({
          id: crypto.randomUUID(),
          date: new Date().toISOString(),
          amount: amount,
          confirmationNumber: `TXN-${Math.floor(Math.random() * 1000000)}`,
          method: method === 'CARD' ? 'CREDIT_CARD' : 'ACH',
          status: 'SUCCESS'
        });
      }, 1500);
    }, 2000);
  };

  if (step === 'SUCCESS') {
     return (
       <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
         <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-sm w-full text-center animate-fadeIn">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4 animate-bounce">
              <CheckCircle className="w-8 h-8 text-green-600" />
            </div>
            <h3 className="text-xl font-bold text-slate-800">Payment Successful!</h3>
            <p className="text-slate-500 mt-2">Your payment of <strong>${amount.toLocaleString()}</strong> has been processed.</p>
         </div>
       </div>
     );
  }

  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
       <div className="bg-white rounded-2xl shadow-2xl overflow-hidden max-w-md w-full animate-slideUp">
         {/* Header */}
         <div className="bg-slate-900 text-white p-6 relative">
            <h3 className="text-lg font-bold flex items-center gap-2"><Lock className="w-4 h-4 text-green-400" /> Secure Payment</h3>
            <div className="absolute top-6 right-6 font-mono text-2xl font-bold">${amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
            <p className="text-slate-400 text-sm mt-1">Pay to: {recipient}</p>
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
                   <div>
                     <label className="text-xs font-bold text-slate-500 uppercase">Card Number</label>
                     <div className="relative mt-1">
                       <CreditCard className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                       <input type="text" placeholder="0000 0000 0000 0000" className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" required />
                     </div>
                   </div>
                   <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="text-xs font-bold text-slate-500 uppercase">Expiry</label>
                        <input type="text" placeholder="MM/YY" className="w-full px-3 py-2 mt-1 border border-slate-300 rounded-lg outline-none" required />
                      </div>
                      <div>
                        <label className="text-xs font-bold text-slate-500 uppercase">CVC</label>
                        <input type="text" placeholder="123" className="w-full px-3 py-2 mt-1 border border-slate-300 rounded-lg outline-none" required />
                      </div>
                   </div>
                </div>
              ) : (
                <div className="space-y-4">
                   <div>
                     <label className="text-xs font-bold text-slate-500 uppercase">Routing Number</label>
                     <div className="relative mt-1">
                       <Building className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                       <input type="text" placeholder="9 Digits" className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" required />
                     </div>
                   </div>
                   <div>
                     <label className="text-xs font-bold text-slate-500 uppercase">Account Number</label>
                     <input type="text" placeholder="Account #" className="w-full px-3 py-2 mt-1 border border-slate-300 rounded-lg border border-slate-300 rounded-lg outline-none" required />
                   </div>
                </div>
              )}

              <div className="flex gap-3 pt-2">
                 <button type="button" onClick={onCancel} className="flex-1 py-3 border border-slate-300 rounded-xl font-medium text-slate-600 hover:bg-slate-50">Cancel</button>
                 <button type="submit" className="flex-1 py-3 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700 shadow-lg shadow-indigo-200">
                    Pay Now
                 </button>
              </div>
           </form>
         )}
       </div>
    </div>
  );
};
