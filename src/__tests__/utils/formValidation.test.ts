import { describe, it, expect } from 'vitest';

describe('Form Validation Tests', () => {
  describe('Email Validation', () => {
    const validateEmail = (email: string): boolean => {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      return emailRegex.test(email);
    };

    it('should validate correct email formats', () => {
      expect(validateEmail('user@example.com')).toBe(true);
      expect(validateEmail('test.user@domain.co.uk')).toBe(true);
      expect(validateEmail('admin+tag@company.org')).toBe(true);
    });

    it('should reject invalid email formats', () => {
      expect(validateEmail('invalid.email')).toBe(false);
      expect(validateEmail('@example.com')).toBe(false);
      expect(validateEmail('user@')).toBe(false);
      expect(validateEmail('user @example.com')).toBe(false);
    });
  });

  describe('EIN Validation', () => {
    const validateEIN = (ein: string): boolean => {
      const einRegex = /^\d{2}-\d{7}$/;
      return einRegex.test(ein);
    };

    it('should validate correct EIN formats', () => {
      expect(validateEIN('12-3456789')).toBe(true);
      expect(validateEIN('00-0000001')).toBe(true);
      expect(validateEIN('99-9999999')).toBe(true);
    });

    it('should reject invalid EIN formats', () => {
      expect(validateEIN('123456789')).toBe(false);
      expect(validateEIN('12-345678')).toBe(false);
      expect(validateEIN('1-23456789')).toBe(false);
      expect(validateEIN('AB-1234567')).toBe(false);
    });
  });

  describe('Currency Validation', () => {
    const validateCurrency = (amount: string): boolean => {
      const currencyRegex = /^\d+(\.\d{1,2})?$/;
      return currencyRegex.test(amount);
    };

    it('should validate correct currency formats', () => {
      expect(validateCurrency('100')).toBe(true);
      expect(validateCurrency('100.00')).toBe(true);
      expect(validateCurrency('1000.5')).toBe(true);
      expect(validateCurrency('0.99')).toBe(true);
    });

    it('should reject invalid currency formats', () => {
      expect(validateCurrency('100.001')).toBe(false);
      expect(validateCurrency('$100')).toBe(false);
      expect(validateCurrency('-100')).toBe(false);
      expect(validateCurrency('abc')).toBe(false);
    });
  });

  describe('Zip Code Validation', () => {
    const validateZipCode = (zip: string): boolean => {
      const zipRegex = /^\d{5}(-\d{4})?$/;
      return zipRegex.test(zip);
    };

    it('should validate correct zip code formats', () => {
      expect(validateZipCode('43016')).toBe(true);
      expect(validateZipCode('12345')).toBe(true);
      expect(validateZipCode('12345-6789')).toBe(true);
    });

    it('should reject invalid zip code formats', () => {
      expect(validateZipCode('1234')).toBe(false);
      expect(validateZipCode('123456')).toBe(false);
      expect(validateZipCode('12345-678')).toBe(false);
      expect(validateZipCode('ABCDE')).toBe(false);
    });
  });
});
