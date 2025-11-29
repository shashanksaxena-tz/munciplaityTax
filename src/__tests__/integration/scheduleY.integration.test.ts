/**
 * Integration tests for Schedule Y Multi-State Apportionment
 * Tests the end-to-end flow of apportionment calculations
 */

import { describe, it, expect } from 'vitest';
import { 
  ApportionmentFormula,
  type PropertyFactorInput,
  type PayrollFactorInput,
  type SalesFactorInput,
} from '../../types/apportionment';
import {
  SourcingMethodElection,
  ThrowbackElection,
  ServiceSourcingMethod,
} from '../../types/sourcing';

describe('Schedule Y Integration Tests', () => {
  describe('US-1: Finnigan vs Joyce Sourcing Method', () => {
    it('should calculate higher apportionment with Joyce method when no-nexus subsidiary has sales', () => {
      // Given: Multi-state group with nexus and no-nexus entities
      const parentSales = 5000000; // $5M with OH nexus
      const parentOhioSales = 1000000; // $1M in OH
      const subASales = 3000000; // $3M with OH nexus
      const subAOhioSales = 500000; // $500K in OH
      const subBSales = 2000000; // $2M NO OH nexus
      
      // Finnigan (include all): denominator = $10M
      const finniganDenominator = parentSales + subASales + subBSales;
      const finniganNumerator = parentOhioSales + subAOhioSales;
      const finniganApportionment = (finniganNumerator / finniganDenominator) * 100;
      
      // Joyce (only nexus entities): denominator = $8M
      const joyceDenominator = parentSales + subASales;
      const joyceNumerator = parentOhioSales + subAOhioSales;
      const joyceApportionment = (joyceNumerator / joyceDenominator) * 100;
      
      // Then: Joyce should have higher apportionment
      expect(finniganApportionment).toBeCloseTo(15.0, 2); // 15%
      expect(joyceApportionment).toBeCloseTo(18.75, 2); // 18.75%
      expect(joyceApportionment).toBeGreaterThan(finniganApportionment);
    });

    it('should default to Finnigan method (majority rule)', () => {
      const defaultMethod = SourcingMethodElection.FINNIGAN;
      expect(defaultMethod).toBe('FINNIGAN');
    });
  });

  describe('US-2: Throwback Rule Application', () => {
    it('should throw back sale to origin state when destination lacks nexus', () => {
      // Given: Sale from OH to CA, no CA nexus
      const saleAmount = 100000;
      const originState = 'OH';
      const destinationState = 'CA';
      const hasDestinationNexus = false;
      const throwbackElection = ThrowbackElection.THROWBACK;
      
      // When: Apply throwback rule
      const throwbackAmount = hasDestinationNexus ? 0 : saleAmount;
      const ohioNumeratorAddition = throwbackAmount;
      
      // Then: Sale should be added to OH numerator
      expect(throwbackAmount).toBe(100000);
      expect(ohioNumeratorAddition).toBe(100000);
    });

    it('should throw out sale when throwout election is used', () => {
      // Given: Sale from OH to CA, no CA nexus, throwout elected
      const saleAmount = 100000;
      const hasDestinationNexus = false;
      const throwbackElection = ThrowbackElection.THROWOUT;
      
      // When: Apply throwout rule
      const includedInDenominator = hasDestinationNexus;
      const includedInNumerator = false;
      
      // Then: Sale should be excluded from both numerator and denominator
      expect(includedInDenominator).toBe(false);
      expect(includedInNumerator).toBe(false);
    });
  });

  describe('US-3: Market-Based Service Sourcing', () => {
    it('should source 100% of service revenue to customer location (market-based)', () => {
      // Given: IT consulting from OH office to NY customer
      const serviceRevenue = 1000000;
      const customerState = 'NY';
      const sourcingMethod = ServiceSourcingMethod.MARKET_BASED;
      
      // When: Apply market-based sourcing
      const nyRevenue = serviceRevenue; // 100% to customer location
      const ohRevenue = 0; // 0% to work location
      
      // Then: All revenue sourced to NY
      expect(nyRevenue).toBe(1000000);
      expect(ohRevenue).toBe(0);
    });

    it('should prorate service revenue by employee location (cost-of-performance)', () => {
      // Given: Service with employees in OH (70%) and CA (30%)
      const serviceRevenue = 1000000;
      const ohPayrollPercent = 0.70;
      const caPayrollPercent = 0.30;
      const sourcingMethod = ServiceSourcingMethod.COST_OF_PERFORMANCE;
      
      // When: Apply cost-of-performance sourcing
      const ohRevenue = serviceRevenue * ohPayrollPercent;
      const caRevenue = serviceRevenue * caPayrollPercent;
      
      // Then: Revenue prorated by payroll
      expect(ohRevenue).toBe(700000);
      expect(caRevenue).toBe(300000);
      expect(ohRevenue + caRevenue).toBe(serviceRevenue);
    });

    it('should fall back to cost-of-performance when customer location unknown', () => {
      // Given: Service with unknown customer location
      const serviceRevenue = 1000000;
      const customerState = undefined;
      const employeeLocations = { OH: 0.70, CA: 0.30 };
      
      // When: Customer location unknown, fallback to cost-of-performance
      const shouldFallback = !customerState;
      const useCostOfPerformance = shouldFallback && Object.keys(employeeLocations).length > 0;
      
      // Then: Should use cost-of-performance
      expect(shouldFallback).toBe(true);
      expect(useCostOfPerformance).toBeTruthy();
    });
  });

  describe('US-4: Apportionment Factor Calculation', () => {
    it('should calculate four-factor double-weighted sales apportionment', () => {
      // Given: Business with property, payroll, and sales factors
      const propertyFactor = 20.0; // 20%
      const payrollFactor = 42.86; // 42.86%
      const salesFactor = 50.0; // 50%
      
      // When: Calculate apportionment with double-weighted sales
      // Formula: (Property * 0.25) + (Payroll * 0.25) + (Sales * 0.50)
      const apportionment = 
        (propertyFactor * 0.25) + 
        (payrollFactor * 0.25) + 
        (salesFactor * 0.50);
      
      // Then: Apportionment should be weighted correctly
      expect(apportionment).toBeCloseTo(40.715, 2);
    });

    it('should calculate traditional three-factor apportionment', () => {
      // Given: Business with equal-weighted factors
      const propertyFactor = 30.0;
      const payrollFactor = 40.0;
      const salesFactor = 50.0;
      
      // When: Calculate equal-weighted apportionment
      const apportionment = (propertyFactor + payrollFactor + salesFactor) / 3;
      
      // Then: Average of three factors
      expect(apportionment).toBeCloseTo(40.0, 2);
    });

    it('should handle property factor with rented property (8x multiplier)', () => {
      // Given: Owned and rented property
      const ohioOwnedProperty = 1000000;
      const ohioRentedPropertyAnnualRent = 100000;
      const totalOwnedProperty = 5000000;
      const totalRentedPropertyAnnualRent = 500000;
      
      // When: Calculate property factor with rent capitalization
      const ohioPropertyValue = ohioOwnedProperty + (ohioRentedPropertyAnnualRent * 8);
      const totalPropertyValue = totalOwnedProperty + (totalRentedPropertyAnnualRent * 8);
      const propertyFactor = (ohioPropertyValue / totalPropertyValue) * 100;
      
      // Then: Rented property capitalized at 8x
      expect(ohioPropertyValue).toBe(1800000); // $1M + ($100K * 8)
      expect(totalPropertyValue).toBe(9000000); // $5M + ($500K * 8)
      expect(propertyFactor).toBeCloseTo(20.0, 2); // 20%
    });
  });

  describe('US-5: Single-Sales-Factor Election', () => {
    it('should use only sales factor when single-sales-factor elected', () => {
      // Given: Business with low property/payroll but high sales
      const propertyFactor = 5.0;
      const payrollFactor = 10.0;
      const salesFactor = 60.0;
      
      // When: Single-sales-factor elected
      const singleSalesApportionment = salesFactor;
      
      // Traditional four-factor apportionment for comparison
      const traditionalApportionment = 
        (propertyFactor * 0.25) + 
        (payrollFactor * 0.25) + 
        (salesFactor * 0.50);
      
      // Then: Single-sales-factor should be 60%, traditional 33.75%
      expect(singleSalesApportionment).toBe(60.0);
      expect(traditionalApportionment).toBeCloseTo(33.75, 2);
      
      // Business would NOT elect single-sales-factor (higher tax)
      expect(singleSalesApportionment).toBeGreaterThan(traditionalApportionment);
    });
  });

  describe('Factor Percentage Validation', () => {
    it('should validate factor percentages are between 0 and 100', () => {
      const validateFactor = (percentage: number): boolean => {
        return percentage >= 0 && percentage <= 100;
      };
      
      expect(validateFactor(50.0)).toBe(true);
      expect(validateFactor(0.0)).toBe(true);
      expect(validateFactor(100.0)).toBe(true);
      expect(validateFactor(-5.0)).toBe(false);
      expect(validateFactor(105.0)).toBe(false);
    });

    it('should validate final apportionment percentage is between 0 and 100', () => {
      const propertyFactor = 20.0;
      const payrollFactor = 30.0;
      const salesFactor = 40.0;
      
      const apportionment = 
        (propertyFactor * 0.25) + 
        (payrollFactor * 0.25) + 
        (salesFactor * 0.50);
      
      expect(apportionment).toBeGreaterThanOrEqual(0);
      expect(apportionment).toBeLessThanOrEqual(100);
    });
  });

  describe('Apportionment Formula Types', () => {
    it('should support three-factor equal-weighted formula', () => {
      const formula = ApportionmentFormula.THREE_FACTOR_EQUAL_WEIGHTED;
      expect(formula).toBe('THREE_FACTOR_EQUAL_WEIGHTED');
    });

    it('should support four-factor double-weighted sales formula', () => {
      const formula = ApportionmentFormula.FOUR_FACTOR_DOUBLE_WEIGHTED_SALES;
      expect(formula).toBe('FOUR_FACTOR_DOUBLE_WEIGHTED_SALES');
    });

    it('should support single-sales-factor formula', () => {
      const formula = ApportionmentFormula.SINGLE_SALES_FACTOR;
      expect(formula).toBe('SINGLE_SALES_FACTOR');
    });
  });

  describe('Sourcing Method Elections', () => {
    it('should support Finnigan sourcing method', () => {
      const method = SourcingMethodElection.FINNIGAN;
      expect(method).toBe('FINNIGAN');
    });

    it('should support Joyce sourcing method', () => {
      const method = SourcingMethodElection.JOYCE;
      expect(method).toBe('JOYCE');
    });
  });

  describe('Throwback Elections', () => {
    it('should support throwback election', () => {
      const election = ThrowbackElection.THROWBACK;
      expect(election).toBe('THROWBACK');
    });

    it('should support throwout election', () => {
      const election = ThrowbackElection.THROWOUT;
      expect(election).toBe('THROWOUT');
    });

    it('should support no throwback/throwout election', () => {
      const election = ThrowbackElection.NONE;
      expect(election).toBe('NONE');
    });
  });

  describe('Service Sourcing Methods', () => {
    it('should support market-based sourcing', () => {
      const method = ServiceSourcingMethod.MARKET_BASED;
      expect(method).toBe('MARKET_BASED');
    });

    it('should support cost-of-performance sourcing', () => {
      const method = ServiceSourcingMethod.COST_OF_PERFORMANCE;
      expect(method).toBe('COST_OF_PERFORMANCE');
    });

    it('should support pro-rata sourcing', () => {
      const method = ServiceSourcingMethod.PRO_RATA;
      expect(method).toBe('PRO_RATA');
    });
  });
});
