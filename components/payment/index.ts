/**
 * Payment components module
 * 
 * Provides modular payment UI components for credit card and ACH payments:
 * - PaymentMethodSelector: Choose between payment methods
 * - CreditCardForm: Credit card input with test card helpers
 * - BankAccountForm: Bank account (ACH) input with test account helpers
 * - PaymentConfirmation: Processing/loading state
 * - PaymentReceipt: Success/failure receipt display
 */

export { PaymentMethodSelector } from './PaymentMethodSelector';
export { CreditCardForm } from './CreditCardForm';
export { BankAccountForm } from './BankAccountForm';
export { PaymentConfirmation } from './PaymentConfirmation';
export { PaymentReceipt } from './PaymentReceipt';

export type {
  PaymentMethod,
  PaymentResponse,
  TestCreditCard,
  TestACHAccount,
  TestPaymentMethods,
  PaymentFormData,
} from './types';
