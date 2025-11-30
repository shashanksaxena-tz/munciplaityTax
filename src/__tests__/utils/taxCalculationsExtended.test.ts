import { describe, it, expect } from 'vitest';
import { calculateTaxAmount, calculateWithholding, validateTaxData } from '../../utils/taxCalculations';

describe('Additional Tax Utility Tests', () => {
  describe('Edge Cases for calculateTaxAmount', () => {
    it('should handle very large numbers', () => {
      expect(calculateTaxAmount(10000000, 0.025)).toBe(250000);
    });

    it('should handle very small rates', () => {
      expect(calculateTaxAmount(100000, 0.001)).toBe(100);
    });

    it('should handle decimal income amounts', () => {
      expect(calculateTaxAmount(50000.50, 0.025)).toBeCloseTo(1250.01, 2);
    });
  });

  describe('Multiple Tax Scenarios', () => {
    it('should calculate quarterly withholding', () => {
      const annualWages = 100000;
      const quarterly = calculateWithholding(annualWages / 4, 0.025);
      expect(quarterly).toBe(625);
    });

    it('should calculate monthly withholding', () => {
      const annualWages = 120000;
      const monthly = calculateWithholding(annualWages / 12, 0.025);
      expect(monthly).toBe(250);
    });

    it('should handle progressive rates', () => {
      // Simulate different brackets
      const baseAmount = calculateTaxAmount(50000, 0.02);
      const higherAmount = calculateTaxAmount(50000, 0.03);
      expect(higherAmount).toBeGreaterThan(baseAmount);
    });
  });

  describe('Data Validation Edge Cases', () => {
    it('should reject future tax years', () => {
      const futureYear = new Date().getFullYear() + 5;
      expect(validateTaxData({
        taxYear: futureYear,
        income: 50000,
        withholding: 1250,
        taxRate: 0.025
      })).toBe(false);
    });

    it('should reject very old tax years', () => {
      expect(validateTaxData({
        taxYear: 1990,
        income: 50000,
        withholding: 1250,
        taxRate: 0.025
      })).toBe(false);
    });

    it('should accept current tax year', () => {
      const currentYear = new Date().getFullYear();
      expect(validateTaxData({
        taxYear: currentYear,
        income: 50000,
        withholding: 1250,
        taxRate: 0.025
      })).toBe(true);
    });

    it('should accept next year (for planning)', () => {
      const nextYear = new Date().getFullYear() + 1;
      expect(validateTaxData({
        taxYear: nextYear,
        income: 50000,
        withholding: 1250,
        taxRate: 0.025
      })).toBe(true);
    });
  });
});
