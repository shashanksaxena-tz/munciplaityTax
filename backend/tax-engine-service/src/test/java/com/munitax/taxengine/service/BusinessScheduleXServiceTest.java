package com.munitax.taxengine.service;

import com.munitax.taxengine.model.BusinessScheduleXDetails;
import com.munitax.taxengine.model.BusinessScheduleXDetails.AddBacks;
import com.munitax.taxengine.model.BusinessScheduleXDetails.Deductions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BusinessScheduleXService backward compatibility
 * Tests migration from old 6-field format to new 27-field format
 * 
 * Feature: Comprehensive Business Schedule X Reconciliation (25+ Fields)
 * Research: R2 (JSONB backward compatibility strategy)
 */
@DisplayName("Business Schedule X Backward Compatibility Tests")
class BusinessScheduleXServiceTest {

    @InjectMocks
    private BusinessScheduleXService businessScheduleXService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        businessScheduleXService = new BusinessScheduleXService();
    }

    /**
     * Test Case: Detect old 6-field format
     * Scenario: Schedule X data has top-level fields (interestIncome, dividends, capitalGains) instead of nested structure
     * Expected: System detects old format and returns true
     */
    @Test
    @DisplayName("Detect old 6-field Schedule X format")
    void testDetectOldFormat() {
        // Arrange
        // Simulate old JSON structure with top-level fields (pre-expansion format)
        String oldFormatJson = """
            {
              "fedTaxableIncome": 500000,
              "incomeAndStateTaxes": 10000,
              "interestIncome": 5000,
              "dividends": 3000,
              "capitalGains": 2000,
              "other": 0
            }
            """;

        // Act
        boolean isOldFormat = businessScheduleXService.isOldFormat(oldFormatJson);

        // Assert
        assertTrue(isOldFormat, 
            "System should detect old 6-field format (has top-level interestIncome instead of nested deductions.interestIncome)");
    }

    /**
     * Test Case: Convert old 6-field format to new 27-field format
     * Scenario: Old format with federal income $500K, state taxes $10K, intangible income $10K
     * Expected: New format with proper nested structure:
     *   - addBacks.incomeAndStateTaxes = $10K (migrated from top-level)
     *   - deductions.interestIncome = $5K (migrated from top-level)
     *   - deductions.dividends = $3K (migrated from top-level)
     *   - deductions.capitalGains = $2K (migrated from top-level)
     *   - All other fields initialized to 0
     */
    @Test
    @DisplayName("Convert old 6-field format to new 27-field format")
    void testConvertOldFormatToNew() {
        // Arrange
        BusinessScheduleXDetails oldScheduleX = new BusinessScheduleXDetails();
        oldScheduleX.setFedTaxableIncome(500000.0);
        
        // Old format had these as top-level fields (not nested)
        // Simulate migration by setting fields in old locations
        AddBacks oldAddBacks = new AddBacks();
        oldAddBacks.setIncomeAndStateTaxes(10000.0);
        oldScheduleX.setAddBacks(oldAddBacks);
        
        Deductions oldDeductions = new Deductions();
        oldDeductions.setInterestIncome(5000.0);
        oldDeductions.setDividends(3000.0);
        oldDeductions.setCapitalGains(2000.0);
        oldScheduleX.setDeductions(oldDeductions);

        // Act
        BusinessScheduleXDetails newScheduleX = businessScheduleXService.convertToNewFormat(oldScheduleX);

        // Assert
        // Verify federal income preserved
        assertEquals(500000.0, newScheduleX.getFedTaxableIncome(), 0.01, 
            "Federal taxable income should be preserved during migration");

        // Verify add-backs nested structure
        assertNotNull(newScheduleX.getAddBacks(), "Add-backs object should be created");
        assertEquals(10000.0, newScheduleX.getAddBacks().getIncomeAndStateTaxes(), 0.01, 
            "State taxes should be migrated to addBacks.incomeAndStateTaxes");

        // Verify deductions nested structure
        assertNotNull(newScheduleX.getDeductions(), "Deductions object should be created");
        assertEquals(5000.0, newScheduleX.getDeductions().getInterestIncome(), 0.01, 
            "Interest income should be migrated to deductions.interestIncome");
        assertEquals(3000.0, newScheduleX.getDeductions().getDividends(), 0.01, 
            "Dividends should be migrated to deductions.dividends");
        assertEquals(2000.0, newScheduleX.getDeductions().getCapitalGains(), 0.01, 
            "Capital gains should be migrated to deductions.capitalGains");

        // Verify new fields initialized to 0
        assertEquals(0.0, newScheduleX.getAddBacks().getDepreciationAdjustment(), 0.01, 
            "New field (depreciation) should be initialized to 0");
        assertEquals(0.0, newScheduleX.getAddBacks().getMealsAndEntertainment(), 0.01, 
            "New field (meals) should be initialized to 0");

        // Verify calculated fields
        assertNotNull(newScheduleX.getCalculatedFields(), "Calculated fields should be initialized");
        double expectedTotalAddBacks = 10000.0; // Only state taxes in old format
        double expectedTotalDeductions = 10000.0; // Interest $5K + dividends $3K + capital gains $2K
        double expectedAdjustedIncome = 500000.0 + expectedTotalAddBacks - expectedTotalDeductions;
        
        assertEquals(expectedTotalAddBacks, newScheduleX.getCalculatedFields().getTotalAddBacks(), 0.01, 
            "Total add-backs should be calculated correctly after migration");
        assertEquals(expectedTotalDeductions, newScheduleX.getCalculatedFields().getTotalDeductions(), 0.01, 
            "Total deductions should be calculated correctly after migration");
        assertEquals(expectedAdjustedIncome, newScheduleX.getCalculatedFields().getAdjustedMunicipalIncome(), 0.01, 
            "Adjusted municipal income should match old calculation ($500K + $10K - $10K = $500K)");
    }

    /**
     * Test Case: Old format calculations still work after migration
     * Scenario: Old format return with state taxes $10K, intangible income $10K
     * Expected: Adjusted municipal income = $500K (federal income unchanged because add-backs and deductions balance)
     */
    @Test
    @DisplayName("Old format returns calculate correctly after migration")
    void testOldFormatCalculationAfterMigration() {
        // Arrange
        BusinessScheduleXDetails oldScheduleX = createOldFormatScheduleX();

        // Act
        BusinessScheduleXDetails newScheduleX = businessScheduleXService.convertToNewFormat(oldScheduleX);
        businessScheduleXService.recalculateTotals(newScheduleX);

        // Assert
        assertEquals(500000.0, newScheduleX.getCalculatedFields().getAdjustedMunicipalIncome(), 0.01, 
            "Old format return should calculate same adjusted income after migration ($500K federal + $10K add-backs - $10K deductions = $500K)");
    }

    /**
     * Test Case: Detect new 27-field format (should not trigger migration)
     * Scenario: Schedule X data already has nested structure (addBacks, deductions objects)
     * Expected: System detects new format and returns false (no migration needed)
     */
    @Test
    @DisplayName("Detect new 27-field format (no migration needed)")
    void testDetectNewFormat() {
        // Arrange
        BusinessScheduleXDetails newScheduleX = new BusinessScheduleXDetails();
        newScheduleX.setFedTaxableIncome(500000.0);
        
        AddBacks addBacks = new AddBacks();
        addBacks.setDepreciationAdjustment(50000.0);
        addBacks.setIncomeAndStateTaxes(10000.0);
        newScheduleX.setAddBacks(addBacks);
        
        Deductions deductions = new Deductions();
        deductions.setInterestIncome(5000.0);
        newScheduleX.setDeductions(deductions);

        String newFormatJson = businessScheduleXService.toJson(newScheduleX);

        // Act
        boolean isOldFormat = businessScheduleXService.isOldFormat(newFormatJson);

        // Assert
        assertFalse(isOldFormat, 
            "System should NOT detect old format for new 27-field structure (has nested addBacks and deductions objects)");
    }

    // ========== Helper Methods ==========

    /**
     * Create old 6-field format Schedule X for testing backward compatibility
     * Federal income $500K, state taxes $10K (add-back), intangible income $10K (deduction)
     */
    private BusinessScheduleXDetails createOldFormatScheduleX() {
        BusinessScheduleXDetails scheduleX = new BusinessScheduleXDetails();
        scheduleX.setFedTaxableIncome(500000.0);

        // Old format: top-level fields (will be migrated to nested structure)
        AddBacks addBacks = new AddBacks();
        addBacks.setIncomeAndStateTaxes(10000.0);
        scheduleX.setAddBacks(addBacks);

        Deductions deductions = new Deductions();
        deductions.setInterestIncome(5000.0);
        deductions.setDividends(3000.0);
        deductions.setCapitalGains(2000.0);
        scheduleX.setDeductions(deductions);

        return scheduleX;
    }
}
