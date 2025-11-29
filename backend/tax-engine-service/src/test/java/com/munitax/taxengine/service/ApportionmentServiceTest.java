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

    // ===== T129: Single-Sales-Factor Tests =====

    @Test
    @DisplayName("Calculate apportionment with single-sales-factor formula")
    void testCalculateApportionmentSingleSalesFactor() {
        // Given: Business elects single-sales-factor apportionment
        BigDecimal propertyFactor = new BigDecimal("5.0");   // 5% OH
        BigDecimal payrollFactor = new BigDecimal("10.0");   // 10% OH
        BigDecimal salesFactor = new BigDecimal("60.0");     // 60% OH

        // Single-sales-factor: 100% weight on sales, 0% on property/payroll
        Map<String, BigDecimal> weights = Map.of(
                "property", new BigDecimal("0.0"),
                "payroll", new BigDecimal("0.0"),
                "sales", new BigDecimal("1.0")
        );

        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.SINGLE_SALES_FACTOR))
                .thenReturn(weights);

        // When: Calculate apportionment with single-sales-factor
        BigDecimal apportionment = apportionmentService.calculateApportionmentPercentage(
                propertyFactor, payrollFactor, salesFactor,
                ApportionmentFormula.SINGLE_SALES_FACTOR);

        // Then: Apportionment equals sales factor (60%)
        // Formula: (5% * 0) + (10% * 0) + (60% * 1.0) = 60%
        assertEquals(new BigDecimal("60.0"), apportionment);
    }

    @Test
    @DisplayName("Single-sales-factor ignores property and payroll factors")
    void testSingleSalesFactorIgnoresPropertyAndPayroll() {
        // Given: High property and payroll but low sales
        BigDecimal propertyFactor = new BigDecimal("80.0");  // 80% OH
        BigDecimal payrollFactor = new BigDecimal("75.0");   // 75% OH
        BigDecimal salesFactor = new BigDecimal("20.0");     // 20% OH (much lower)

        Map<String, BigDecimal> weights = Map.of(
                "property", new BigDecimal("0.0"),
                "payroll", new BigDecimal("0.0"),
                "sales", new BigDecimal("1.0")
        );

        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.SINGLE_SALES_FACTOR))
                .thenReturn(weights);

        // When: Calculate with single-sales-factor
        BigDecimal apportionment = apportionmentService.calculateApportionmentPercentage(
                propertyFactor, payrollFactor, salesFactor,
                ApportionmentFormula.SINGLE_SALES_FACTOR);

        // Then: Only sales factor matters (20%)
        assertEquals(new BigDecimal("20.0"), apportionment);
    }

    // ===== T130: Formula Comparison Tests =====

    @Test
    @DisplayName("Compare traditional vs single-sales-factor - single-sales is lower")
    void testCompareFormulas_SingleSalesLower() {
        // Given: Property 5%, Payroll 10%, Sales 60%
        BigDecimal propertyFactor = new BigDecimal("5.0");
        BigDecimal payrollFactor = new BigDecimal("10.0");
        BigDecimal salesFactor = new BigDecimal("60.0");

        // Traditional four-factor: (5% * 0.25) + (10% * 0.25) + (60% * 0.50)
        //                         = 1.25 + 2.5 + 30 = 33.75%
        Map<String, BigDecimal> traditionalWeights = Map.of(
                "property", new BigDecimal("0.25"),
                "payroll", new BigDecimal("0.25"),
                "sales", new BigDecimal("0.50")
        );

        // Single-sales-factor: 60%
        Map<String, BigDecimal> singleSalesWeights = Map.of(
                "property", new BigDecimal("0.0"),
                "payroll", new BigDecimal("0.0"),
                "sales", new BigDecimal("1.0")
        );

        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES))
                .thenReturn(traditionalWeights);
        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.SINGLE_SALES_FACTOR))
                .thenReturn(singleSalesWeights);

        // When: Calculate both
        BigDecimal traditional = apportionmentService.calculateApportionmentPercentage(
                propertyFactor, payrollFactor, salesFactor,
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);

        BigDecimal singleSales = apportionmentService.calculateApportionmentPercentage(
                propertyFactor, payrollFactor, salesFactor,
                ApportionmentFormula.SINGLE_SALES_FACTOR);

        // Then: Single-sales-factor is higher (60% vs 33.75%)
        // For tax minimization, taxpayer should use traditional (33.75%)
        assertTrue(singleSales.compareTo(traditional) > 0,
                "Single-sales-factor (60%) should be greater than traditional (33.75%)");
        assertEquals(new BigDecimal("33.750"), traditional.setScale(3));
        assertEquals(new BigDecimal("60.0"), singleSales);
    }

    @Test
    @DisplayName("Compare traditional vs single-sales-factor - traditional is lower")
    void testCompareFormulas_TraditionalLower() {
        // Given: Property 50%, Payroll 60%, Sales 15%
        BigDecimal propertyFactor = new BigDecimal("50.0");
        BigDecimal payrollFactor = new BigDecimal("60.0");
        BigDecimal salesFactor = new BigDecimal("15.0");

        // Traditional four-factor: (50% * 0.25) + (60% * 0.25) + (15% * 0.50)
        //                         = 12.5 + 15.0 + 7.5 = 35%
        Map<String, BigDecimal> traditionalWeights = Map.of(
                "property", new BigDecimal("0.25"),
                "payroll", new BigDecimal("0.25"),
                "sales", new BigDecimal("0.50")
        );

        // Single-sales-factor: 15%
        Map<String, BigDecimal> singleSalesWeights = Map.of(
                "property", new BigDecimal("0.0"),
                "payroll", new BigDecimal("0.0"),
                "sales", new BigDecimal("1.0")
        );

        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES))
                .thenReturn(traditionalWeights);
        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.SINGLE_SALES_FACTOR))
                .thenReturn(singleSalesWeights);

        // When: Calculate both
        BigDecimal traditional = apportionmentService.calculateApportionmentPercentage(
                propertyFactor, payrollFactor, salesFactor,
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);

        BigDecimal singleSales = apportionmentService.calculateApportionmentPercentage(
                propertyFactor, payrollFactor, salesFactor,
                ApportionmentFormula.SINGLE_SALES_FACTOR);

        // Then: Single-sales-factor is lower (15% vs 35%)
        // For tax minimization, taxpayer should elect single-sales-factor (15%)
        assertTrue(singleSales.compareTo(traditional) < 0,
                "Single-sales-factor (15%) should be less than traditional (35%)");
        assertEquals(new BigDecimal("35.0"), traditional);
        assertEquals(new BigDecimal("15.0"), singleSales);
    }

    @Test
    @DisplayName("Recommend single-sales-factor when it minimizes tax liability")
    void testRecommendFormulaBasedOnLowerApportionment() {
        // Given: Two scenarios to test recommendation logic

        // Scenario 1: High sales factor (60%) vs traditional (33.75%)
        // Should recommend traditional
        BigDecimal scenario1Property = new BigDecimal("5.0");
        BigDecimal scenario1Payroll = new BigDecimal("10.0");
        BigDecimal scenario1Sales = new BigDecimal("60.0");

        // Scenario 2: Low sales factor (15%) vs traditional (35%)
        // Should recommend single-sales-factor
        BigDecimal scenario2Property = new BigDecimal("50.0");
        BigDecimal scenario2Payroll = new BigDecimal("60.0");
        BigDecimal scenario2Sales = new BigDecimal("15.0");

        Map<String, BigDecimal> traditionalWeights = Map.of(
                "property", new BigDecimal("0.25"),
                "payroll", new BigDecimal("0.25"),
                "sales", new BigDecimal("0.50")
        );

        Map<String, BigDecimal> singleSalesWeights = Map.of(
                "property", new BigDecimal("0.0"),
                "payroll", new BigDecimal("0.0"),
                "sales", new BigDecimal("1.0")
        );

        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES))
                .thenReturn(traditionalWeights);
        when(formulaConfigService.getFormulaWeights(ApportionmentFormula.SINGLE_SALES_FACTOR))
                .thenReturn(singleSalesWeights);

        // When/Then: Scenario 1 - recommend traditional
        BigDecimal s1Traditional = apportionmentService.calculateApportionmentPercentage(
                scenario1Property, scenario1Payroll, scenario1Sales,
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);
        BigDecimal s1SingleSales = apportionmentService.calculateApportionmentPercentage(
                scenario1Property, scenario1Payroll, scenario1Sales,
                ApportionmentFormula.SINGLE_SALES_FACTOR);

        assertTrue(s1Traditional.compareTo(s1SingleSales) < 0,
                "Scenario 1: Traditional should be recommended (lower)");

        // When/Then: Scenario 2 - recommend single-sales-factor
        BigDecimal s2Traditional = apportionmentService.calculateApportionmentPercentage(
                scenario2Property, scenario2Payroll, scenario2Sales,
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);
        BigDecimal s2SingleSales = apportionmentService.calculateApportionmentPercentage(
                scenario2Property, scenario2Payroll, scenario2Sales,
                ApportionmentFormula.SINGLE_SALES_FACTOR);

        assertTrue(s2SingleSales.compareTo(s2Traditional) < 0,
                "Scenario 2: Single-sales-factor should be recommended (lower)");
    }
}
