/**
 * Shared types for payment components
 */

export type PaymentMethod = 'CARD' | 'ACH';

export interface PaymentResponse {
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

export interface TestCreditCard {
  cardNumber: string;
  cardType: 'VISA' | 'MASTERCARD' | 'AMEX';
  expectedResult: 'APPROVED' | 'DECLINED' | 'ERROR';
  description: string;
}

export interface TestACHAccount {
  routingNumber: string;
  accountNumber: string;
  expectedResult: 'APPROVED' | 'DECLINED';
  description: string;
}

export interface TestPaymentMethods {
  creditCards: TestCreditCard[];
  achAccounts: TestACHAccount[];
  testMode: boolean;
}

export interface PaymentFormData {
  // Credit Card fields
  cardNumber: string;
  cardExpiry: string;
  cardCvv: string;
  
  // ACH fields
  achRouting: string;
  achAccount: string;
}

/**
 * Helper function to map expected result to display text and color
 */
export const getResultDisplay = (result: string): { text: string; color: string } => {
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
