import { describe, it, expect } from 'vitest';
import { calculateTaxAmount, calculateWithholding, validateTaxData } from '../../utils/taxCalculations';

describe('Tax Calculations Utilities', () => {
  describe('calculateTaxAmount', () => {
    it('should calculate tax correctly for standard income', () => {
      const income = 100000;
      const rate = 0.025; // 2.5%
      const expected = 2500;
      expect(calculateTaxAmount(income, rate)).toBe(expected);
    });

    it('should handle zero income', () => {
      expect(calculateTaxAmount(0, 0.025)).toBe(0);
    });

    it('should handle negative income', () => {
      expect(calculateTaxAmount(-1000, 0.025)).toBe(0);
    });

    it('should round to 2 decimal places', () => {
      const income = 123.456;
      const rate = 0.025;
      const result = calculateTaxAmount(income, rate);
      expect(result).toBeCloseTo(3.09, 2);
    });
  });

  describe('calculateWithholding', () => {
    it('should calculate withholding correctly', () => {
      const wages = 50000;
      const rate = 0.025;
      const expected = 1250;
      expect(calculateWithholding(wages, rate)).toBe(expected);
    });

    it('should handle quarterly withholding', () => {
      const annualWages = 100000;
      const rate = 0.025;
      const quarterly = calculateWithholding(annualWages / 4, rate);
      expect(quarterly).toBeCloseTo(625, 2);
    });
  });

  describe('validateTaxData', () => {
    it('should validate complete tax data', () => {
      const validData = {
        taxYear: 2024,
        income: 100000,
        withholding: 2500,
        taxRate: 0.025
      };
      expect(validateTaxData(validData)).toBe(true);
    });

    it('should reject missing required fields', () => {
      const invalidData = {
        taxYear: 2024,
        income: 100000
        // Missing withholding and taxRate
      };
      expect(validateTaxData(invalidData)).toBe(false);
    });

    it('should reject invalid tax year', () => {
      const invalidData = {
        taxYear: 1900,
        income: 100000,
        withholding: 2500,
        taxRate: 0.025
      };
      expect(validateTaxData(invalidData)).toBe(false);
    });

    it('should reject negative values', () => {
      const invalidData = {
        taxYear: 2024,
        income: -100,
        withholding: 2500,
        taxRate: 0.025
      };
      expect(validateTaxData(invalidData)).toBe(false);
    });
  });
});
