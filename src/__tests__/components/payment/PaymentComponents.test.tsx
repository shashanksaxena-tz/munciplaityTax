import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { 
  PaymentMethodSelector,
  CreditCardForm,
  BankAccountForm,
  PaymentConfirmation,
  PaymentReceipt,
} from '../../../../components/payment';

describe('Payment Components', () => {
  describe('PaymentMethodSelector', () => {
    it('should render payment method options', () => {
      const mockOnChange = () => {};
      render(
        <PaymentMethodSelector 
          selectedMethod="CARD" 
          onMethodChange={mockOnChange}
        />
      );
      
      expect(screen.getByText(/Credit.*Debit/i)).toBeDefined();
      expect(screen.getByText(/Bank Account.*ACH/i)).toBeDefined();
    });
  });

  describe('CreditCardForm', () => {
    it('should render credit card input fields', () => {
      const mockOnChange = () => {};
      render(
        <CreditCardForm
          cardNumber=""
          cardExpiry=""
          cardCvv=""
          onCardNumberChange={mockOnChange}
          onCardExpiryChange={mockOnChange}
          onCardCvvChange={mockOnChange}
        />
      );
      
      expect(screen.getByText(/Card Number/i)).toBeDefined();
      expect(screen.getByText(/Expiry/i)).toBeDefined();
      expect(screen.getByText(/CVC/i)).toBeDefined();
    });

    it('should show test cards when in test mode', () => {
      const mockOnChange = () => {};
      const testCards = [
        {
          cardNumber: '4111111111111111',
          cardType: 'VISA' as const,
          expectedResult: 'APPROVED' as const,
          description: 'Test Visa',
        }
      ];
      
      render(
        <CreditCardForm
          cardNumber=""
          cardExpiry=""
          cardCvv=""
          onCardNumberChange={mockOnChange}
          onCardExpiryChange={mockOnChange}
          onCardCvvChange={mockOnChange}
          testCards={testCards}
          testMode={true}
        />
      );
      
      expect(screen.getByText(/Test Card Numbers/i)).toBeDefined();
    });
  });

  describe('BankAccountForm', () => {
    it('should render ACH input fields', () => {
      const mockOnChange = () => {};
      render(
        <BankAccountForm
          achRouting=""
          achAccount=""
          onAchRoutingChange={mockOnChange}
          onAchAccountChange={mockOnChange}
        />
      );
      
      expect(screen.getByText(/Routing Number/i)).toBeDefined();
      expect(screen.getByText(/Account Number/i)).toBeDefined();
    });

    it('should show test accounts when in test mode', () => {
      const mockOnChange = () => {};
      const testAccounts = [
        {
          routingNumber: '123456789',
          accountNumber: '987654321',
          expectedResult: 'APPROVED' as const,
          description: 'Test Account',
        }
      ];
      
      render(
        <BankAccountForm
          achRouting=""
          achAccount=""
          onAchRoutingChange={mockOnChange}
          onAchAccountChange={mockOnChange}
          testAccounts={testAccounts}
          testMode={true}
        />
      );
      
      expect(screen.getByText(/Test ACH Accounts/i)).toBeDefined();
    });
  });

  describe('PaymentConfirmation', () => {
    it('should show processing message when processing', () => {
      render(<PaymentConfirmation isProcessing={true} />);
      
      expect(screen.getByText(/Processing Transaction/i)).toBeDefined();
      expect(screen.getByText(/Do not close this window/i)).toBeDefined();
    });

    it('should not render when not processing', () => {
      const { container } = render(<PaymentConfirmation isProcessing={false} />);
      expect(container.firstChild).toBeNull();
    });
  });

  describe('PaymentReceipt', () => {
    it('should show success receipt for approved payment', () => {
      const mockResponse = {
        status: 'APPROVED' as const,
        transactionId: 'txn-123',
        providerTransactionId: 'prov-456',
        receiptNumber: 'REC-789',
        amount: 100.00,
        testMode: false,
        timestamp: '2024-12-09T12:00:00Z',
      };
      
      render(
        <PaymentReceipt
          response={mockResponse}
          amount={100}
        />
      );
      
      expect(screen.getByText(/Payment Successful/i)).toBeDefined();
      expect(screen.getByText(/REC-789/)).toBeDefined();
      expect(screen.getByText(/prov-456/)).toBeDefined();
    });

    it('should show error receipt for declined payment', () => {
      const mockResponse = {
        status: 'DECLINED' as const,
        transactionId: 'txn-123',
        providerTransactionId: 'prov-456',
        failureReason: 'Insufficient funds',
        amount: 100.00,
        testMode: false,
        timestamp: '2024-12-09T12:00:00Z',
      };
      
      const mockRetry = () => {};
      const mockClose = () => {};
      
      render(
        <PaymentReceipt
          response={mockResponse}
          amount={100}
          onRetry={mockRetry}
          onClose={mockClose}
        />
      );
      
      expect(screen.getByText(/Payment Failed/i)).toBeDefined();
      expect(screen.getByText(/Insufficient funds/i)).toBeDefined();
      expect(screen.getByText(/Try Again/i)).toBeDefined();
    });

    it('should show test mode indicator when in test mode', () => {
      const mockResponse = {
        status: 'APPROVED' as const,
        transactionId: 'txn-123',
        providerTransactionId: 'prov-456',
        receiptNumber: 'REC-789',
        amount: 100.00,
        testMode: true,
        timestamp: '2024-12-09T12:00:00Z',
      };
      
      render(
        <PaymentReceipt
          response={mockResponse}
          amount={100}
        />
      );
      
      expect(screen.getByText(/TEST MODE/i)).toBeDefined();
      expect(screen.getByText(/No Real Charges/i)).toBeDefined();
    });
  });
});
