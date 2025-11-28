package com.munitax.taxengine.service;

import com.munitax.taxengine.model.BusinessScheduleXDetails;
import com.munitax.taxengine.model.BusinessScheduleXDetails.AddBacks;
import com.munitax.taxengine.model.BusinessScheduleXDetails.Deductions;
import com.munitax.taxengine.model.BusinessScheduleXDetails.CalculatedFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScheduleXCalculationService
 * Tests critical calculations for Schedule X: total add-backs, total deductions, adjusted municipal income
 * 
 * Feature: Comprehensive Business Schedule X Reconciliation (25+ Fields)
 * Functional Requirements: FR-028 (total add-backs), FR-029 (total deductions), FR-030 (adjusted income)
 */
@DisplayName("Schedule X Calculation Service Tests")
class ScheduleXCalculationServiceTest {

    @InjectMocks
    private ScheduleXCalculationService scheduleXCalculationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduleXCalculationService = new ScheduleXCalculationService();
    }

    /**
     * Test Case: User Story 1 - C-Corp with depreciation, meals, state taxes
     * Scenario: Federal income $500K, add-backs $75K (depreciation $50K + meals $15K + state taxes $10K), deductions $0
     * Expected: Total add-backs = $75,000, adjusted municipal income = $575,000
     */
    @Test
    @DisplayName("FR-028: Calculate total add-backs for C-Corp (User Story 1)")
    void testCalculateTotalAddBacks_UserStory1() {
        // Arrange
        BusinessScheduleXDetails scheduleX = createScheduleXUserStory1();

        // Act
        double totalAddBacks = scheduleXCalculationService.calculateTotalAddBacks(scheduleX.getAddBacks());

        // Assert
        assertEquals(75000.0, totalAddBacks, 0.01, 
            "Total add-backs should be $75,000 (depreciation $50K + meals $15K + state taxes $10K)");
    }

    /**
     * Test Case: Calculate total deductions
     * Scenario: Partnership with intangible income deductions ($20K interest + $15K dividends + $5K capital gains)
     * Expected: Total deductions = $40,000
     */
    @Test
    @DisplayName("FR-029: Calculate total deductions for partnership")
    void testCalculateTotalDeductions() {
        // Arrange
        Deductions deductions = new Deductions();
        deductions.setInterestIncome(20000.0);
        deductions.setDividends(15000.0);
        deductions.setCapitalGains(5000.0);
        deductions.setSection179Recapture(0.0);
        deductions.setMunicipalBondInterest(0.0);
        deductions.setDepletionDifference(0.0);
        deductions.setOtherDeductions(0.0);

        // Act
        double totalDeductions = scheduleXCalculationService.calculateTotalDeductions(deductions);

        // Assert
        assertEquals(40000.0, totalDeductions, 0.01, 
            "Total deductions should be $40,000 (interest $20K + dividends $15K + capital gains $5K)");
    }

    /**
     * Test Case: Calculate adjusted municipal income
     * Scenario: Federal income $500K, add-backs $75K, deductions $0
     * Expected: Adjusted municipal income = $575,000 (formula: federal + addBacks - deductions)
     */
    @Test
    @DisplayName("FR-030: Calculate adjusted municipal income (User Story 1)")
    void testCalculateAdjustedMunicipalIncome_UserStory1() {
        // Arrange
        BusinessScheduleXDetails scheduleX = createScheduleXUserStory1();
        double fedTaxableIncome = 500000.0;
        double totalAddBacks = scheduleXCalculationService.calculateTotalAddBacks(scheduleX.getAddBacks());
        double totalDeductions = scheduleXCalculationService.calculateTotalDeductions(scheduleX.getDeductions());

        // Act
        double adjustedIncome = scheduleXCalculationService.calculateAdjustedMunicipalIncome(
            fedTaxableIncome, totalAddBacks, totalDeductions
        );

        // Assert
        assertEquals(575000.0, adjustedIncome, 0.01, 
            "Adjusted municipal income should be $575,000 ($500K federal + $75K add-backs - $0 deductions)");
    }

    /**
     * Test Case: User Story 2 - Partnership with guaranteed payments and intangible income
     * Scenario: Federal income $300K, add-backs $51,750 (guaranteed payments $50K + 5% Rule $1,750), 
     *           deductions $35K (interest $20K + dividends $15K)
     * Expected: Adjusted municipal income = $316,750
     */
    @Test
    @DisplayName("Calculate adjusted municipal income for partnership (User Story 2)")
    void testCalculateAdjustedMunicipalIncome_UserStory2() {
        // Arrange
        double fedTaxableIncome = 300000.0;
        
        AddBacks addBacks = new AddBacks();
        addBacks.setGuaranteedPayments(50000.0);
        addBacks.setExpensesOnIntangibleIncome(1750.0); // 5% Rule: $35K × 0.05
        
        Deductions deductions = new Deductions();
        deductions.setInterestIncome(20000.0);
        deductions.setDividends(15000.0);

        // Act
        double totalAddBacks = scheduleXCalculationService.calculateTotalAddBacks(addBacks);
        double totalDeductions = scheduleXCalculationService.calculateTotalDeductions(deductions);
        double adjustedIncome = scheduleXCalculationService.calculateAdjustedMunicipalIncome(
            fedTaxableIncome, totalAddBacks, totalDeductions
        );

        // Assert
        assertEquals(51750.0, totalAddBacks, 0.01, "Total add-backs should be $51,750");
        assertEquals(35000.0, totalDeductions, 0.01, "Total deductions should be $35,000");
        assertEquals(316750.0, adjustedIncome, 0.01, 
            "Adjusted municipal income should be $316,750 ($300K + $51,750 - $35K)");
    }

    /**
     * Test Case: Zero adjustments (edge case)
     * Scenario: Federal income $500K, no add-backs, no deductions
     * Expected: Adjusted municipal income = $500,000 (unchanged)
     */
    @Test
    @DisplayName("Edge case: Zero adjustments - adjusted income equals federal income")
    void testCalculateAdjustedMunicipalIncome_ZeroAdjustments() {
        // Arrange
        double fedTaxableIncome = 500000.0;
        AddBacks addBacks = new AddBacks(); // All fields default to 0
        Deductions deductions = new Deductions(); // All fields default to 0

        // Act
        double totalAddBacks = scheduleXCalculationService.calculateTotalAddBacks(addBacks);
        double totalDeductions = scheduleXCalculationService.calculateTotalDeductions(deductions);
        double adjustedIncome = scheduleXCalculationService.calculateAdjustedMunicipalIncome(
            fedTaxableIncome, totalAddBacks, totalDeductions
        );

        // Assert
        assertEquals(0.0, totalAddBacks, 0.01, "Total add-backs should be $0");
        assertEquals(0.0, totalDeductions, 0.01, "Total deductions should be $0");
        assertEquals(500000.0, adjustedIncome, 0.01, 
            "Adjusted municipal income should equal federal income when no adjustments");
    }

    /**
     * Test Case: All 20 add-back fields populated
     * Scenario: Each add-back field has $1,000 value
     * Expected: Total add-backs = $20,000
     */
    @Test
    @DisplayName("Calculate total add-backs with all 20 fields populated")
    void testCalculateTotalAddBacks_AllFieldsPopulated() {
        // Arrange
        AddBacks addBacks = new AddBacks();
        addBacks.setDepreciationAdjustment(1000.0);
        addBacks.setAmortizationAdjustment(1000.0);
        addBacks.setIncomeAndStateTaxes(1000.0);
        addBacks.setGuaranteedPayments(1000.0);
        addBacks.setMealsAndEntertainment(1000.0);
        addBacks.setRelatedPartyExcess(1000.0);
        addBacks.setPenaltiesAndFines(1000.0);
        addBacks.setPoliticalContributions(1000.0);
        addBacks.setOfficerLifeInsurance(1000.0);
        addBacks.setCapitalLossExcess(1000.0);
        addBacks.setFederalTaxRefunds(1000.0);
        addBacks.setExpensesOnIntangibleIncome(1000.0);
        addBacks.setSection179Excess(1000.0);
        addBacks.setBonusDepreciation(1000.0);
        addBacks.setBadDebtReserveIncrease(1000.0);
        addBacks.setCharitableContributionExcess(1000.0);
        addBacks.setDomesticProductionActivities(1000.0);
        addBacks.setStockCompensationAdjustment(1000.0);
        addBacks.setInventoryMethodChange(1000.0);
        addBacks.setOtherAddBacks(1000.0);

        // Act
        double totalAddBacks = scheduleXCalculationService.calculateTotalAddBacks(addBacks);

        // Assert
        assertEquals(20000.0, totalAddBacks, 0.01, 
            "Total add-backs should be $20,000 (20 fields × $1,000 each)");
    }

    // ========== Helper Methods ==========

    /**
     * Create Schedule X for User Story 1: C-Corp with depreciation, meals, state taxes
     * Federal income $500K, add-backs $75K, deductions $0
     */
    private BusinessScheduleXDetails createScheduleXUserStory1() {
        BusinessScheduleXDetails scheduleX = new BusinessScheduleXDetails();
        scheduleX.setFedTaxableIncome(500000.0);

        AddBacks addBacks = new AddBacks();
        addBacks.setDepreciationAdjustment(50000.0);  // Book $80K - MACRS $130K = -$50K (add back)
        addBacks.setMealsAndEntertainment(15000.0);   // Federal meals $15K → Municipal add-back $15K
        addBacks.setIncomeAndStateTaxes(10000.0);     // State income taxes deducted federally

        Deductions deductions = new Deductions();
        // No deductions for C-Corp in User Story 1

        scheduleX.setAddBacks(addBacks);
        scheduleX.setDeductions(deductions);

        return scheduleX;
    }
}
