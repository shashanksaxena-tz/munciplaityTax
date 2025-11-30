package com.munitax.taxengine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails.AddBacks;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails.Deductions;
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
    void testDetectOldFormat() throws Exception {
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

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(oldFormatJson);

        // Act
        boolean isOldFormat = businessScheduleXService.isOldFormat(jsonNode);

        // Assert
        assertTrue(isOldFormat, 
            "System should detect old 6-field format (has top-level interestIncome instead of nested deductions.interestIncome)");
    }

    /**
     * Test Case: Convert old 6-field format to new 27-field format
     * Scenario: Old format with federal income $500K, state taxes $10K, intangible income $10K
     * Expected: New format with proper nested structure:
     *   - addBacks.interestAndStateTaxes = $10K (migrated from top-level)
     *   - deductions.interestIncome = $5K (migrated from top-level)
     *   - deductions.dividends = $3K (migrated from top-level)
     *   - deductions.capitalGains = $2K (migrated from top-level)
     *   - All other fields initialized to 0
     */
    @Test
    @DisplayName("Convert old 6-field format to new 27-field format")
    void testConvertOldFormatToNew() throws Exception {
        // Arrange - Create old format JSON
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
        
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(oldFormatJson);

        // Act
        BusinessScheduleXDetails newScheduleX = businessScheduleXService.convertFromOldFormat(jsonNode);

        // Assert
        // Verify federal income preserved
        assertEquals(500000.0, newScheduleX.fedTaxableIncome(), 0.01, 
            "Federal taxable income should be preserved during migration");

        // Verify add-backs nested structure
        assertNotNull(newScheduleX.addBacks(), "Add-backs object should be created");
        assertEquals(10000.0, newScheduleX.addBacks().interestAndStateTaxes(), 0.01, 
            "State taxes should be migrated to addBacks.interestAndStateTaxes");

        // Verify deductions nested structure
        assertNotNull(newScheduleX.deductions(), "Deductions object should be created");
        assertEquals(5000.0, newScheduleX.deductions().interestIncome(), 0.01, 
            "Interest income should be migrated to deductions.interestIncome");
        assertEquals(3000.0, newScheduleX.deductions().dividends(), 0.01, 
            "Dividends should be migrated to deductions.dividends");
        assertEquals(2000.0, newScheduleX.deductions().capitalGains(), 0.01, 
            "Capital gains should be migrated to deductions.capitalGains");

        // Verify new fields initialized to 0
        assertEquals(0.0, newScheduleX.addBacks().depreciationAdjustment(), 0.01, 
            "New field (depreciation) should be initialized to 0");
        assertEquals(0.0, newScheduleX.addBacks().mealsAndEntertainment(), 0.01, 
            "New field (meals) should be initialized to 0");

        // Verify calculated fields
        assertNotNull(newScheduleX.calculatedFields(), "Calculated fields should be initialized");
        double expectedTotalAddBacks = 10000.0; // Only state taxes in old format
        double expectedTotalDeductions = 10000.0; // Interest $5K + dividends $3K + capital gains $2K
        double expectedAdjustedIncome = 500000.0 + expectedTotalAddBacks - expectedTotalDeductions;
        
        assertEquals(expectedTotalAddBacks, newScheduleX.calculatedFields().totalAddBacks(), 0.01, 
            "Total add-backs should be calculated correctly after migration");
        assertEquals(expectedTotalDeductions, newScheduleX.calculatedFields().totalDeductions(), 0.01, 
            "Total deductions should be calculated correctly after migration");
        assertEquals(expectedAdjustedIncome, newScheduleX.calculatedFields().adjustedMunicipalIncome(), 0.01, 
            "Adjusted municipal income should match old calculation ($500K + $10K - $10K = $500K)");
    }

    /**
     * Test Case: Old format calculations still work after migration
     * Scenario: Old format return with state taxes $10K, intangible income $10K
     * Expected: Adjusted municipal income = $500K (federal income unchanged because add-backs and deductions balance)
     */
    @Test
    @DisplayName("Old format returns calculate correctly after migration")
    void testOldFormatCalculationAfterMigration() throws Exception {
        // Arrange - Create old format JSON
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
        
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(oldFormatJson);

        // Act
        BusinessScheduleXDetails newScheduleX = businessScheduleXService.convertFromOldFormat(jsonNode);

        // Assert
        assertEquals(500000.0, newScheduleX.calculatedFields().adjustedMunicipalIncome(), 0.01, 
            "Old format return should calculate same adjusted income after migration ($500K federal + $10K add-backs - $10K deductions = $500K)");
    }

    /**
     * Test Case: Detect new 27-field format (should not trigger migration)
     * Scenario: Schedule X data already has nested structure (addBacks, deductions objects)
     * Expected: System detects new format and returns false (no migration needed)
     */
    @Test
    @DisplayName("Detect new 27-field format (no migration needed)")
    void testDetectNewFormat() throws Exception {
        // Arrange
        AddBacks addBacks = new AddBacks(
            10000.0,  // interestAndStateTaxes (incomeAndStateTaxes)
            0.0,      // guaranteedPayments
            0.0,      // expensesOnIntangibleIncome
            50000.0,  // depreciationAdjustment
            0.0,      // amortizationAdjustment
            0.0,      // mealsAndEntertainment
            0.0,      // relatedPartyExcess
            0.0,      // penaltiesAndFines
            0.0,      // politicalContributions
            0.0,      // officerLifeInsurance
            0.0,      // capitalLossExcess
            0.0,      // federalTaxRefunds
            0.0,      // section179Excess
            0.0,      // bonusDepreciation
            0.0,      // badDebtReserveIncrease
            0.0,      // charitableContributionExcess
            0.0,      // domesticProductionActivities
            0.0,      // stockCompensationAdjustment
            0.0,      // inventoryMethodChange
            0.0,      // otherAddBacks
            null,     // otherAddBacksDescription
            0.0       // wagesCredit
        );
        
        Deductions deductions = new Deductions(
            5000.0,   // interestIncome
            0.0,      // dividends
            0.0,      // capitalGains
            0.0,      // section179Excess
            0.0,      // otherDeductions
            0.0,      // section179Recapture
            0.0,      // municipalBondInterest
            0.0,      // depletionDifference
            null      // otherDeductionsDescription
        );

        BusinessScheduleXDetails.CalculatedFields calculatedFields = 
            new BusinessScheduleXDetails.CalculatedFields(60000.0, 5000.0, 555000.0);
        
        BusinessScheduleXDetails.Metadata metadata = 
            new BusinessScheduleXDetails.Metadata(
                java.time.Instant.now().toString(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of()
            );

        BusinessScheduleXDetails newScheduleX = new BusinessScheduleXDetails(
            500000.0,
            addBacks,
            deductions,
            calculatedFields,
            metadata
        );

        String newFormatJson = businessScheduleXService.toJsonString(newScheduleX);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(newFormatJson);

        // Act
        boolean isOldFormat = businessScheduleXService.isOldFormat(jsonNode);

        // Assert
        assertFalse(isOldFormat, 
            "System should NOT detect old format for new 27-field structure (has nested addBacks and deductions objects)");
    }

    // ========== Helper Methods ==========

    /**
     * Create old 6-field format Schedule X for testing backward compatibility
     * Federal income $500K, state taxes $10K (add-back), intangible income $10K (deduction)
     */}
