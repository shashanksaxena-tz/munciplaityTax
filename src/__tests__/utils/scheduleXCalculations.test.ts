/**
 * Unit tests for scheduleXCalculations utility functions
 * Tests frontend calculation helpers for Schedule X
 * 
 * Feature: Comprehensive Business Schedule X Reconciliation (25+ Fields)
 * Research: R3 (auto-calculation helpers - frontend implementation)
 */

import { describe, it, expect } from 'vitest';
import {
  calculateTotalAddBacks,
  calculateTotalDeductions,
  calculateAdjustedMunicipalIncome,
  calculateMealsAddBack,
  calculate5PercentRule,
  calculateRelatedPartyExcess,
} from '../../utils/scheduleXCalculations';
import { AddBacks, Deductions } from '../../types/scheduleX';

describe('Schedule X Calculations', () => {
  
  describe('calculateTotalAddBacks', () => {
    it('should calculate total add-backs for User Story 1 (C-Corp)', () => {
      // Arrange - User Story 1: Depreciation $50K + Meals $15K + State taxes $10K
      const addBacks: AddBacks = {
        depreciationAdjustment: 50000,
        amortizationAdjustment: 0,
        incomeAndStateTaxes: 10000,
        guaranteedPayments: 0,
        mealsAndEntertainment: 15000,
        relatedPartyExcess: 0,
        penaltiesAndFines: 0,
        politicalContributions: 0,
        officerLifeInsurance: 0,
        capitalLossExcess: 0,
        federalTaxRefunds: 0,
        expensesOnIntangibleIncome: 0,
        section179Excess: 0,
        bonusDepreciation: 0,
        badDebtReserveIncrease: 0,
        charitableContributionExcess: 0,
        domesticProductionActivities: 0,
        stockCompensationAdjustment: 0,
        inventoryMethodChange: 0,
        otherAddBacks: 0,
        otherAddBacksDescription: null,
      };

      // Act
      const total = calculateTotalAddBacks(addBacks);

      // Assert
      expect(total).toBe(75000);
    });

    it('should return 0 for empty add-backs', () => {
      // Arrange
      const addBacks: AddBacks = {
        depreciationAdjustment: 0,
        amortizationAdjustment: 0,
        incomeAndStateTaxes: 0,
        guaranteedPayments: 0,
        mealsAndEntertainment: 0,
        relatedPartyExcess: 0,
        penaltiesAndFines: 0,
        politicalContributions: 0,
        officerLifeInsurance: 0,
        capitalLossExcess: 0,
        federalTaxRefunds: 0,
        expensesOnIntangibleIncome: 0,
        section179Excess: 0,
        bonusDepreciation: 0,
        badDebtReserveIncrease: 0,
        charitableContributionExcess: 0,
        domesticProductionActivities: 0,
        stockCompensationAdjustment: 0,
        inventoryMethodChange: 0,
        otherAddBacks: 0,
        otherAddBacksDescription: null,
      };

      // Act
      const total = calculateTotalAddBacks(addBacks);

      // Assert
      expect(total).toBe(0);
    });
  });

  describe('calculateTotalDeductions', () => {
    it('should calculate total deductions for partnership intangible income', () => {
      // Arrange - Interest $20K + Dividends $15K + Capital gains $5K
      const deductions: Deductions = {
        interestIncome: 20000,
        dividends: 15000,
        capitalGains: 5000,
        section179Recapture: 0,
        municipalBondInterest: 0,
        depletionDifference: 0,
        otherDeductions: 0,
        otherDeductionsDescription: null,
      };

      // Act
      const total = calculateTotalDeductions(deductions);

      // Assert
      expect(total).toBe(40000);
    });

    it('should return 0 for empty deductions', () => {
      // Arrange
      const deductions: Deductions = {
        interestIncome: 0,
        dividends: 0,
        capitalGains: 0,
        section179Recapture: 0,
        municipalBondInterest: 0,
        depletionDifference: 0,
        otherDeductions: 0,
        otherDeductionsDescription: null,
      };

      // Act
      const total = calculateTotalDeductions(deductions);

      // Assert
      expect(total).toBe(0);
    });
  });

  describe('calculateAdjustedMunicipalIncome', () => {
    it('should calculate adjusted income for User Story 1 (C-Corp)', () => {
      // Arrange - Federal $500K + Add-backs $75K - Deductions $0 = $575K
      const fedTaxableIncome = 500000;
      const totalAddBacks = 75000;
      const totalDeductions = 0;

      // Act
      const adjustedIncome = calculateAdjustedMunicipalIncome(
        fedTaxableIncome,
        totalAddBacks,
        totalDeductions
      );

      // Assert
      expect(adjustedIncome).toBe(575000);
    });

    it('should calculate adjusted income for User Story 2 (Partnership)', () => {
      // Arrange - Federal $300K + Add-backs $51,750 - Deductions $35K = $316,750
      const fedTaxableIncome = 300000;
      const totalAddBacks = 51750; // Guaranteed payments $50K + 5% Rule $1,750
      const totalDeductions = 35000; // Interest $20K + Dividends $15K

      // Act
      const adjustedIncome = calculateAdjustedMunicipalIncome(
        fedTaxableIncome,
        totalAddBacks,
        totalDeductions
      );

      // Assert
      expect(adjustedIncome).toBe(316750);
    });

    it('should return federal income when no adjustments', () => {
      // Arrange
      const fedTaxableIncome = 500000;
      const totalAddBacks = 0;
      const totalDeductions = 0;

      // Act
      const adjustedIncome = calculateAdjustedMunicipalIncome(
        fedTaxableIncome,
        totalAddBacks,
        totalDeductions
      );

      // Assert
      expect(adjustedIncome).toBe(500000);
    });
  });

  describe('calculateMealsAddBack', () => {
    it('should calculate meals add-back (50% federal → 0% municipal)', () => {
      // Arrange - Federal meals deduction $15K (50% of $30K total)
      const federalMeals = 15000;

      // Act
      const mealsAddBack = calculateMealsAddBack(federalMeals);

      // Assert
      expect(mealsAddBack).toBe(30000); // Double to get back to 100%
    });

    it('should return 0 for zero federal meals', () => {
      // Arrange
      const federalMeals = 0;

      // Act
      const mealsAddBack = calculateMealsAddBack(federalMeals);

      // Assert
      expect(mealsAddBack).toBe(0);
    });
  });

  describe('calculate5PercentRule', () => {
    it('should calculate 5% Rule for User Story 2 (Partnership)', () => {
      // Arrange - Interest $20K + Dividends $15K = $35K → 5% = $1,750
      const interestIncome = 20000;
      const dividends = 15000;
      const capitalGains = 0;

      // Act
      const expensesAddBack = calculate5PercentRule(interestIncome, dividends, capitalGains);

      // Assert
      expect(expensesAddBack).toBe(1750);
    });

    it('should calculate 5% Rule with all intangible income types', () => {
      // Arrange - Interest $50K + Dividends $30K + Capital gains $20K = $100K → 5% = $5K
      const interestIncome = 50000;
      const dividends = 30000;
      const capitalGains = 20000;

      // Act
      const expensesAddBack = calculate5PercentRule(interestIncome, dividends, capitalGains);

      // Assert
      expect(expensesAddBack).toBe(5000);
    });

    it('should return 0 for zero intangible income', () => {
      // Arrange
      const interestIncome = 0;
      const dividends = 0;
      const capitalGains = 0;

      // Act
      const expensesAddBack = calculate5PercentRule(interestIncome, dividends, capitalGains);

      // Assert
      expect(expensesAddBack).toBe(0);
    });
  });

  describe('calculateRelatedPartyExcess', () => {
    it('should calculate related-party excess for User Story 3 (S-Corp)', () => {
      // Arrange - Paid $10K, FMV $7.5K → Excess $2.5K
      const paidAmount = 10000;
      const fairMarketValue = 7500;

      // Act
      const excessAddBack = calculateRelatedPartyExcess(paidAmount, fairMarketValue);

      // Assert
      expect(excessAddBack).toBe(2500);
    });

    it('should return 0 when paid equals FMV', () => {
      // Arrange
      const paidAmount = 10000;
      const fairMarketValue = 10000;

      // Act
      const excessAddBack = calculateRelatedPartyExcess(paidAmount, fairMarketValue);

      // Assert
      expect(excessAddBack).toBe(0);
    });

    it('should return 0 when paid below FMV (bargain purchase)', () => {
      // Arrange - Paid $5K, FMV $10K → No add-back (bargain purchase, not an adjustment)
      const paidAmount = 5000;
      const fairMarketValue = 10000;

      // Act
      const excessAddBack = calculateRelatedPartyExcess(paidAmount, fairMarketValue);

      // Assert
      expect(excessAddBack).toBe(0); // Bargain purchases don't create add-backs
    });
  });
});
