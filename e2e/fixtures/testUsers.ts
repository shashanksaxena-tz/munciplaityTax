/**
 * Shared test user credentials for E2E tests
 * Update these credentials in one place when needed
 */

export const testUsers = {
  individual: {
    email: 'individual@example.com',
    password: 'password123'
  },
  business: {
    email: 'business@example.com',
    password: 'password123'
  },
  auditor: {
    email: 'auditor@example.com',
    password: 'password123'
  },
  taxpayer: {
    email: 'taxpayer@example.com',
    password: 'password123'
  }
};

/**
 * Standard test card numbers for payment testing
 */
export const testCards = {
  visa: {
    number: '4242424242424242', // Stripe-compatible Visa test card (always approved)
    expiry: '12/25',
    cvv: '123'
  },
  visaAlt: {
    number: '4111111111111111', // Standard Visa test card (always approved)
    expiry: '12/25',
    cvv: '123'
  },
  mastercard: {
    number: '5555555555554444', // Standard Mastercard test card (always approved)
    expiry: '12/25',
    cvv: '123'
  },
  declined: {
    number: '4000000000000002', // Declined test card (insufficient funds)
    expiry: '12/25',
    cvv: '123'
  }
};
