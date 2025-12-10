import React, { useState, useEffect, useRef } from 'react';
import { Lock, Info } from 'lucide-react';
import { PaymentRecord } from '../types';
import {
  PaymentMethodSelector,
  CreditCardForm,
  BankAccountForm,
  PaymentConfirmation,
  PaymentReceipt,
  PaymentMethod,
  PaymentResponse,
  TestPaymentMethods,
} from './payment';

interface PaymentGatewayProps {
  amount: number;
  recipient: string; // e.g., "City of Dublin"
  filerId: string;
  tenantId: string;
  onSuccess: (record: PaymentRecord) => void;
  onCancel: () => void;
}

export const PaymentGateway: React.FC<PaymentGatewayProps> = ({ 
  amount, 
  recipient, 
  filerId,
  tenantId,
  onSuccess, 
  onCancel 
}) => {
  const [method, setMethod] = useState<PaymentMethod>('CARD');
  const [isProcessing, setIsProcessing] = useState(false);
  const [step, setStep] = useState<'ENTRY' | 'PROCESSING' | 'SUCCESS' | 'ERROR'>('ENTRY');
  const [paymentResponse, setPaymentResponse] = useState<PaymentResponse | null>(null);
  
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
  
  // Ref for submit button focus after auto-fill
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

  // Auto-focus submit button after ACH auto-fill
  const handleACHAutoFillComplete = () => {
    setTimeout(() => {
      submitButtonRef.current?.focus();
    }, 50);
  };

  const handlePay = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsProcessing(true);
    setStep('PROCESSING');
    
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
      }
    } catch (error) {
      setIsProcessing(false);
      setStep('ERROR');
      setPaymentResponse({
        status: 'ERROR',
        transactionId: '',
        providerTransactionId: '',
        failureReason: 'Network error. Please try again.',
        amount,
        testMode: testMethods?.testMode || false,
        timestamp: new Date().toISOString(),
      });
    }
  };

  const handleRetry = () => {
    setStep('ENTRY');
    setPaymentResponse(null);
  };

  // Show receipt for success or error
  if ((step === 'SUCCESS' || step === 'ERROR') && paymentResponse) {
    return (
      <PaymentReceipt
        response={paymentResponse}
        amount={amount}
        onRetry={step === 'ERROR' ? handleRetry : undefined}
        onClose={step === 'ERROR' ? onCancel : undefined}
      />
    );
  }

  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
       <div className="bg-white rounded-2xl shadow-2xl overflow-hidden max-w-md w-full animate-slideUp">
         {/* Header with TEST MODE indicator - conditional on testMode */}
         <div className="bg-slate-900 text-white p-6 relative">
            <div className="flex items-center justify-between mb-1">
              <h3 className="text-lg font-bold flex items-center gap-2">
                <Lock className="w-4 h-4 text-green-400" /> Secure Payment
              </h3>
              {testMethods?.testMode && (
                <div className="bg-orange-500 text-white text-xs font-bold px-3 py-1 rounded-full">
                  TEST MODE
                </div>
              )}
            </div>
            <div className="flex items-center justify-between">
              <p className="text-slate-400 text-sm">Pay to: {recipient}</p>
              <div className="font-mono text-2xl font-bold">${amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
            </div>
            {testMethods?.testMode && (
              <div className="mt-2 text-xs text-orange-300 flex items-center gap-1">
                <Info className="w-3 h-3" />
                No real charges will be processed
              </div>
            )}
         </div>

         {step === 'PROCESSING' ? (
           <PaymentConfirmation isProcessing={isProcessing} />
         ) : (
           <form onSubmit={handlePay} className="p-6 space-y-6">
              {/* Method Switcher */}
              <PaymentMethodSelector
                selectedMethod={method}
                onMethodChange={setMethod}
                disabled={isProcessing}
              />

              {method === 'CARD' ? (
                <CreditCardForm
                  cardNumber={cardNumber}
                  cardExpiry={cardExpiry}
                  cardCvv={cardCvv}
                  onCardNumberChange={setCardNumber}
                  onCardExpiryChange={setCardExpiry}
                  onCardCvvChange={setCardCvv}
                  testCards={testMethods?.creditCards || []}
                  testMode={testMethods?.testMode || false}
                  isLoading={testMethodsLoading}
                  disabled={isProcessing}
                />
              ) : (
                <BankAccountForm
                  achRouting={achRouting}
                  achAccount={achAccount}
                  onAchRoutingChange={setAchRouting}
                  onAchAccountChange={setAchAccount}
                  testAccounts={testMethods?.achAccounts || []}
                  testMode={testMethods?.testMode || false}
                  isLoading={testMethodsLoading}
                  disabled={isProcessing}
                  onAutoFillComplete={handleACHAutoFillComplete}
                />
              )}

              <div className="flex gap-3 pt-2">
                 <button type="button" onClick={onCancel} className="flex-1 py-3 border border-slate-300 rounded-xl font-medium text-slate-600 hover:bg-slate-50">Cancel</button>
                 <button ref={submitButtonRef} type="submit" disabled={isProcessing} className="flex-1 py-3 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700 shadow-lg shadow-indigo-200 disabled:opacity-50">
                    Pay Now
                 </button>
              </div>
           </form>
         )}
       </div>
    </div>
  );
};
