package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.ApportionmentFormula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApportionmentService.
 * Tests sales factor denominator calculation and apportionment percentage.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApportionmentService Tests")
class ApportionmentServiceTest {

    @Mock
    private FormulaConfigService formulaConfigService;

    // Note: PropertyFactorService and PayrollFactorService are part of US4, not yet implemented
    // @Mock
    // private PropertyFactorService propertyFactorService;

    // @Mock
    // private PayrollFactorService payrollFactorService;

    @Mock
    private SalesFactorService salesFactorService;

    @InjectMocks
    private ApportionmentService apportionmentService;

    private UUID municipalityId;
    private UUID tenantId;
    private Integer taxYear;

    @BeforeEach
    void setUp() {
        municipalityId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        taxYear = 2024;
    }

    @Test
    @DisplayName("Calculate apportionment with four-factor double-weighted sales formula")
    void testCalculateApportionmentFourFactorDoubleWeightedSales() {
        // Given: Business has property, payroll, and sales factors
        BigDecimal propertyFactor = new BigDecimal("20.0");  // 20%
        BigDecimal payrollFactor = new BigDecimal("42.86");  // 42.86%
        BigDecimal salesFactor = new BigDecimal("50.0");     // 50%

        // Four-factor double-weighted sales: weights are 25%, 25%, 50%
        Map<String, BigDecimal> weights = Map.of(
                "property", new BigDecimal("0.25"),
                "payroll", new BigDecimal("0.25"),
                "sales", new BigDecimal("0.50")
        );

        when(formulaConfigService.getApportionmentFormula(municipalityId, taxYear, tenantId))
                .thenReturn(ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);
        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES))
                .thenReturn(weights);

        // When: Calculate final apportionment percentage
        // Formula: (20% * 0.25) + (42.86% * 0.25) + (50% * 0.50)
        //        = 5.0 + 10.715 + 25.0 = 40.715%
        BigDecimal apportionment = apportionmentService.calculateApportionmentPercentage(
                propertyFactor, payrollFactor, salesFactor,
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);

        // Then: Apportionment should be 40.715%
        assertEquals(new BigDecimal("40.7150"), apportionment);
    }

    @Test
    @DisplayName("Calculate apportionment with three-factor equal weighted formula")
    void testCalculateApportionmentThreeFactorEqualWeighted() {
        // Given: Three-factor formula with equal weights
        BigDecimal propertyFactor = new BigDecimal("30.0");
        BigDecimal payrollFactor = new BigDecimal("40.0");
        BigDecimal salesFactor = new BigDecimal("50.0");

        Map<String, BigDecimal> weights = Map.of(
                "property", new BigDecimal("0.3333"),
                "payroll", new BigDecimal("0.3333"),
                "sales", new BigDecimal("0.3334")
        );

        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.TRADITIONAL_THREE_FACTOR))
                .thenReturn(weights);

        // When: Calculate apportionment
        // Formula: (30% * 0.3333) + (40% * 0.3333) + (50% * 0.3334)
        //        = 9.999 + 13.332 + 16.67 = 40.001%
        BigDecimal apportionment = apportionmentService.calculateApportionmentPercentage(
                propertyFactor, payrollFactor, salesFactor,
                ApportionmentFormula.TRADITIONAL_THREE_FACTOR);

        // Then: Apportionment should be approximately 40%
        assertTrue(apportionment.compareTo(new BigDecimal("39.9")) > 0);
        assertTrue(apportionment.compareTo(new BigDecimal("40.1")) < 0);
    }

    @Test
    @DisplayName("Calculate apportionment with single-sales-factor formula")
    void testCalculateApportionmentSingleSalesFactor() {
        // Given: Single-sales-factor formula
        BigDecimal propertyFactor = new BigDecimal("20.0");
        BigDecimal payrollFactor = new BigDecimal("30.0");
        BigDecimal salesFactor = new BigDecimal("60.0");

        Map<String, BigDecimal> weights = Map.of(
                "property", BigDecimal.ZERO,
                "payroll", BigDecimal.ZERO,
                "sales", BigDecimal.ONE
        );

        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.SINGLE_SALES_FACTOR))
                .thenReturn(weights);

        // When: Calculate apportionment
        // Formula: (20% * 0) + (30% * 0) + (60% * 1.0) = 60%
        BigDecimal apportionment = apportionmentService.calculateApportionmentPercentage(
                propertyFactor, payrollFactor, salesFactor,
                ApportionmentFormula.SINGLE_SALES_FACTOR);

        // Then: Apportionment equals sales factor only
        assertEquals(0, new BigDecimal("60.0").compareTo(apportionment),
                "Apportionment should equal 60.0, got: " + apportionment);
    }

    @Test
    @DisplayName("Validate apportionment percentage is between 0 and 100")
    void testValidateApportionmentRange() {
        // Given: Factors that could result in invalid percentage
        BigDecimal propertyFactor = new BigDecimal("120.0"); // Invalid: > 100%
        BigDecimal payrollFactor = new BigDecimal("50.0");
        BigDecimal salesFactor = new BigDecimal("50.0");

        // When/Then: Should throw validation exception
        assertThrows(IllegalArgumentException.class, () -> {
            apportionmentService.validateFactorPercentage(propertyFactor);
        });
    }

    @Test
    @DisplayName("Handle zero denominator in sales factor gracefully")
    void testHandleZeroDenominatorInSalesFactor() {
        // Given: Sales factor with zero denominator (no sales)
        BigDecimal ohioSales = BigDecimal.ZERO;
        BigDecimal totalSales = BigDecimal.ZERO;

        // When: Calculate sales factor percentage
        BigDecimal salesFactor = apportionmentService.calculateFactorPercentage(
                ohioSales, totalSales);

        // Then: Should return 0% (not throw exception)
        assertEquals(BigDecimal.ZERO, salesFactor);
    }

    @Test
    @DisplayName("Calculate factor percentage with proper rounding")
    void testCalculateFactorPercentageWithRounding() {
        // Given: Ohio numerator and total denominator
        BigDecimal numerator = new BigDecimal("1500000");   // $1.5M
        BigDecimal denominator = new BigDecimal("10000000"); // $10M

        // When: Calculate percentage
        BigDecimal percentage = apportionmentService.calculateFactorPercentage(
                numerator, denominator);

        // Then: Should be 15.0000% (4 decimal places)
        assertEquals(new BigDecimal("15.0000"), percentage);
    }

    @Test
    @DisplayName("Validate formula weights sum to 1.0")
    void testValidateFormulaWeightsSum() {
        // Given: Formula weights
        Map<String, BigDecimal> weights = Map.of(
                "property", new BigDecimal("0.25"),
                "payroll", new BigDecimal("0.25"),
                "sales", new BigDecimal("0.50")
        );

        // When: Validate weights
        BigDecimal sum = weights.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Then: Sum should equal 1.0
        assertEquals(BigDecimal.ONE, sum);
    }

    @Test
    @DisplayName("Calculate weighted contribution for each factor")
    void testCalculateWeightedContributions() {
        // Given: Factor percentages and weights
        BigDecimal propertyFactor = new BigDecimal("20.0");
        BigDecimal propertyWeight = new BigDecimal("0.25");

        BigDecimal payrollFactor = new BigDecimal("42.86");
        BigDecimal payrollWeight = new BigDecimal("0.25");

        BigDecimal salesFactor = new BigDecimal("50.0");
        BigDecimal salesWeight = new BigDecimal("0.50");

        // When: Calculate weighted contributions
        BigDecimal propertyContribution = propertyFactor.multiply(propertyWeight);
        BigDecimal payrollContribution = payrollFactor.multiply(payrollWeight);
        BigDecimal salesContribution = salesFactor.multiply(salesWeight);

        // Then: Contributions sum to final apportionment
        BigDecimal total = propertyContribution
                .add(payrollContribution)
                .add(salesContribution);

        // Expected: 5.0 + 10.715 + 25.0 = 40.715
        assertTrue(total.compareTo(new BigDecimal("40.7")) > 0);
        assertTrue(total.compareTo(new BigDecimal("40.8")) < 0);
    }

    @Test
    @DisplayName("Handle missing property or payroll factor (sales-only business)")
    void testHandleMissingFactors() {
        // Given: Sales-only business (no property or payroll in OH)
        BigDecimal propertyFactor = BigDecimal.ZERO;
        BigDecimal payrollFactor = BigDecimal.ZERO;
        BigDecimal salesFactor = new BigDecimal("50.0");

        Map<String, BigDecimal> weights = Map.of(
                "property", new BigDecimal("0.25"),
                "payroll", new BigDecimal("0.25"),
                "sales", new BigDecimal("0.50")
        );

        // When: Calculate apportionment
        BigDecimal apportionment = apportionmentService.calculateApportionmentPercentage(
                propertyFactor, payrollFactor, salesFactor,
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);

        // Then: Apportionment based on sales factor only
        // (0 * 0.25) + (0 * 0.25) + (50 * 0.50) = 25%
        assertEquals(new BigDecimal("25.0"), apportionment);
    }
}
