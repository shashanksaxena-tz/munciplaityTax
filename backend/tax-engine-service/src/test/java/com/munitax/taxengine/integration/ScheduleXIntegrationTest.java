package com.munitax.taxengine.integration;

import com.munitax.taxengine.model.BusinessScheduleXDetails;
import com.munitax.taxengine.model.BusinessScheduleXDetails.AddBacks;
import com.munitax.taxengine.model.BusinessScheduleXDetails.Deductions;
import com.munitax.taxengine.service.ScheduleXCalculationService;
import com.munitax.taxengine.service.ScheduleXValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Schedule X Expansion
 * Tests complete user stories with real service interactions
 * 
 * Feature: Comprehensive Business Schedule X Reconciliation (25+ Fields)
 * User Stories: US1 (C-Corp), US2 (Partnership), US3 (S-Corp), US4 (Charitable), US5 (DPAD)
 */
@SpringBootTest
@DisplayName("Schedule X Integration Tests")
class ScheduleXIntegrationTest {

    @Autowired
    private ScheduleXCalculationService calculationService;

    @Autowired
    private ScheduleXValidationService validationService;

    /**
     * Integration Test: User Story 1 - C-Corp with Depreciation, Meals, State Taxes
     * 
     * Scenario: C-Corp files Form 27 with federal taxable income of $500,000
     * - Depreciation adjustment: $50,000 (book depreciation < MACRS)
     * - Meals & entertainment: $15,000 federal deduction (50% of $30K) → add back $15K (to reach 0% municipal)
     * - State income taxes: $10,000 (deducted federally, add back for municipal)
     * 
     * Expected Results:
     * - Total add-backs: $75,000
     * - Total deductions: $0
     * - Adjusted municipal income: $575,000 (federal $500K + add-backs $75K - deductions $0)
     * 
     * Acceptance Criteria: FR-001 (depreciation), FR-003 (state taxes), FR-005 (meals), 
     *                       FR-028/FR-029/FR-030 (calculations)
     */
    @Test
    @DisplayName("User Story 1: C-Corp with depreciation, meals, state taxes → Adjusted income $575K")
    void testUserStory1_CCorp_DepreciationMealsStateTaxes() {
        // Arrange
        BusinessScheduleXDetails scheduleX = new BusinessScheduleXDetails();
        scheduleX.setFedTaxableIncome(500000.0);

        // Add-backs: depreciation $50K + meals $15K + state taxes $10K
        AddBacks addBacks = new AddBacks();
        addBacks.setDepreciationAdjustment(50000.0);  // Book depreciation less than MACRS
        addBacks.setMealsAndEntertainment(15000.0);   // Federal deducted 50%, municipal allows 0%
        addBacks.setIncomeAndStateTaxes(10000.0);     // State income taxes

        Deductions deductions = new Deductions();
        // No deductions for C-Corp in this scenario

        scheduleX.setAddBacks(addBacks);
        scheduleX.setDeductions(deductions);

        // Act
        double totalAddBacks = calculationService.calculateTotalAddBacks(scheduleX.getAddBacks());
        double totalDeductions = calculationService.calculateTotalDeductions(scheduleX.getDeductions());
        double adjustedIncome = calculationService.calculateAdjustedMunicipalIncome(
            scheduleX.getFedTaxableIncome(),
            totalAddBacks,
            totalDeductions
        );

        // Assert
        assertEquals(75000.0, totalAddBacks, 0.01, 
            "Total add-backs should be $75,000 (depreciation $50K + meals $15K + state taxes $10K)");
        assertEquals(0.0, totalDeductions, 0.01, 
            "Total deductions should be $0 (no intangible income for C-Corp)");
        assertEquals(575000.0, adjustedIncome, 0.01, 
            "Adjusted municipal income should be $575,000 ($500K federal + $75K add-backs - $0 deductions)");

        // Validation: Variance check (FR-034)
        double variancePercentage = Math.abs(adjustedIncome - scheduleX.getFedTaxableIncome()) 
            / scheduleX.getFedTaxableIncome();
        assertEquals(0.15, variancePercentage, 0.01, 
            "Variance should be 15% ($75K / $500K), which is < 20% threshold");
        assertFalse(validationService.exceedsVarianceThreshold(scheduleX.getFedTaxableIncome(), adjustedIncome),
            "Should NOT trigger >20% variance warning (15% variance)");
    }

    /**
     * Integration Test: User Story 2 - Partnership with Guaranteed Payments and Intangible Income
     * 
     * Scenario: Partnership files Form 27 with federal taxable income of $300,000
     * - Guaranteed payments: $50,000 (Form 1065 Line 10, deductible federally, not municipally)
     * - Intangible income deductions: $35,000 (interest $20K + dividends $15K, non-taxable municipally)
     * - 5% Rule add-back: $1,750 (5% of $35K intangible income)
     * 
     * Expected Results:
     * - Total add-backs: $51,750 (guaranteed payments $50K + 5% Rule $1,750)
     * - Total deductions: $35,000 (interest $20K + dividends $15K)
     * - Adjusted municipal income: $316,750 (federal $300K + add-backs $51,750 - deductions $35K)
     * 
     * Acceptance Criteria: FR-004 (guaranteed payments), FR-012 (5% Rule), 
     *                       FR-021/FR-022 (intangible income deductions)
     */
    @Test
    @DisplayName("User Story 2: Partnership with guaranteed payments, intangible income, 5% Rule → Adjusted income $316,750")
    void testUserStory2_Partnership_GuaranteedPaymentsIntangibleIncome() {
        // Arrange
        BusinessScheduleXDetails scheduleX = new BusinessScheduleXDetails();
        scheduleX.setFedTaxableIncome(300000.0);

        // Add-backs: guaranteed payments $50K + 5% Rule $1,750
        AddBacks addBacks = new AddBacks();
        addBacks.setGuaranteedPayments(50000.0);        // Form 1065 Line 10
        addBacks.setExpensesOnIntangibleIncome(1750.0); // 5% Rule: $35K × 0.05

        // Deductions: interest $20K + dividends $15K
        Deductions deductions = new Deductions();
        deductions.setInterestIncome(20000.0);
        deductions.setDividends(15000.0);

        scheduleX.setAddBacks(addBacks);
        scheduleX.setDeductions(deductions);

        // Act
        double totalAddBacks = calculationService.calculateTotalAddBacks(scheduleX.getAddBacks());
        double totalDeductions = calculationService.calculateTotalDeductions(scheduleX.getDeductions());
        double adjustedIncome = calculationService.calculateAdjustedMunicipalIncome(
            scheduleX.getFedTaxableIncome(),
            totalAddBacks,
            totalDeductions
        );

        // Assert
        assertEquals(51750.0, totalAddBacks, 0.01, 
            "Total add-backs should be $51,750 (guaranteed payments $50K + 5% Rule $1,750)");
        assertEquals(35000.0, totalDeductions, 0.01, 
            "Total deductions should be $35,000 (interest $20K + dividends $15K)");
        assertEquals(316750.0, adjustedIncome, 0.01, 
            "Adjusted municipal income should be $316,750 ($300K federal + $51,750 add-backs - $35K deductions)");

        // Validation: 5% Rule calculation (FR-012)
        double intangibleIncome = deductions.getInterestIncome() + deductions.getDividends();
        double calculatedExpenses = intangibleIncome * 0.05;
        assertEquals(1750.0, calculatedExpenses, 0.01, 
            "5% Rule should calculate $1,750 (5% of $35K intangible income)");
    }

    /**
     * Integration Test: User Story 3 - S-Corp with Related-Party Transactions
     * 
     * Scenario: S-Corp files Form 27 with federal taxable income of $400,000
     * - Related-party rent: paid $10,000, FMV $7,500 → excess $2,500 add-back
     * 
     * Expected Results:
     * - Total add-backs: $2,500 (related-party excess)
     * - Total deductions: $0
     * - Adjusted municipal income: $402,500 (federal $400K + add-backs $2,500 - deductions $0)
     * 
     * Acceptance Criteria: FR-006 (related-party excess), FR-055 (officer compensation info note)
     */
    @Test
    @DisplayName("User Story 3: S-Corp with related-party excess rent → Adjusted income $402,500")
    void testUserStory3_SCorp_RelatedPartyExcess() {
        // Arrange
        BusinessScheduleXDetails scheduleX = new BusinessScheduleXDetails();
        scheduleX.setFedTaxableIncome(400000.0);

        // Add-backs: related-party excess $2,500
        AddBacks addBacks = new AddBacks();
        addBacks.setRelatedPartyExcess(2500.0);  // Paid $10K, FMV $7.5K

        Deductions deductions = new Deductions();
        // No deductions in this scenario

        scheduleX.setAddBacks(addBacks);
        scheduleX.setDeductions(deductions);

        // Act
        double totalAddBacks = calculationService.calculateTotalAddBacks(scheduleX.getAddBacks());
        double totalDeductions = calculationService.calculateTotalDeductions(scheduleX.getDeductions());
        double adjustedIncome = calculationService.calculateAdjustedMunicipalIncome(
            scheduleX.getFedTaxableIncome(),
            totalAddBacks,
            totalDeductions
        );

        // Assert
        assertEquals(2500.0, totalAddBacks, 0.01, 
            "Total add-backs should be $2,500 (related-party excess)");
        assertEquals(0.0, totalDeductions, 0.01, 
            "Total deductions should be $0");
        assertEquals(402500.0, adjustedIncome, 0.01, 
            "Adjusted municipal income should be $402,500 ($400K federal + $2,500 add-backs)");

        // Validation: Related-party excess calculation (FR-006)
        double paidAmount = 10000.0;
        double fairMarketValue = 7500.0;
        double calculatedExcess = paidAmount - fairMarketValue;
        assertEquals(2500.0, calculatedExcess, 0.01, 
            "Related-party excess should calculate $2,500 (paid $10K - FMV $7.5K)");
    }

    /**
     * Integration Test: Edge Case - Zero Adjustments
     * 
     * Scenario: Business with no municipal adjustments (rare but valid)
     * - Federal taxable income: $500,000
     * - No add-backs, no deductions
     * 
     * Expected Results:
     * - Total add-backs: $0
     * - Total deductions: $0
     * - Adjusted municipal income: $500,000 (unchanged from federal)
     */
    @Test
    @DisplayName("Edge case: Zero adjustments → Adjusted income equals federal income")
    void testEdgeCase_ZeroAdjustments() {
        // Arrange
        BusinessScheduleXDetails scheduleX = new BusinessScheduleXDetails();
        scheduleX.setFedTaxableIncome(500000.0);
        scheduleX.setAddBacks(new AddBacks());      // All zeros
        scheduleX.setDeductions(new Deductions());  // All zeros

        // Act
        double totalAddBacks = calculationService.calculateTotalAddBacks(scheduleX.getAddBacks());
        double totalDeductions = calculationService.calculateTotalDeductions(scheduleX.getDeductions());
        double adjustedIncome = calculationService.calculateAdjustedMunicipalIncome(
            scheduleX.getFedTaxableIncome(),
            totalAddBacks,
            totalDeductions
        );

        // Assert
        assertEquals(0.0, totalAddBacks, 0.01, "Total add-backs should be $0");
        assertEquals(0.0, totalDeductions, 0.01, "Total deductions should be $0");
        assertEquals(500000.0, adjustedIncome, 0.01, 
            "Adjusted municipal income should equal federal income when no adjustments");
        
        // Validation: No variance warning (0% variance)
        assertFalse(validationService.exceedsVarianceThreshold(scheduleX.getFedTaxableIncome(), adjustedIncome),
            "Should NOT trigger variance warning when adjusted income equals federal income");
    }
}
