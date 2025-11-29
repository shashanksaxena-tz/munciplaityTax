package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.ApportionmentFormula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FormulaConfigService.
 * Tests retrieval of apportionment formulas from rule engine (mocked).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FormulaConfigService Tests")
class FormulaConfigServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FormulaConfigService formulaConfigService;

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
    @DisplayName("Get apportionment formula from rule engine")
    void testGetApportionmentFormulaFromRuleEngine() {
        // Given: Rule engine returns four-factor double-weighted sales
        Map<String, Object> ruleEngineResponse = Map.of(
                "formula", "FOUR_FACTOR_DOUBLE_SALES",
                "municipalityId", municipalityId.toString(),
                "taxYear", taxYear
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(ruleEngineResponse);

        // When: Get formula for municipality and tax year
        ApportionmentFormula formula = formulaConfigService.getApportionmentFormula(
                municipalityId, taxYear, tenantId);

        // Then: Should return the formula from rule engine
        assertEquals(ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES, formula);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Fall back to default formula when rule engine unavailable")
    void testFallbackToDefaultFormulaWhenRuleEngineUnavailable() {
        // Given: Rule engine is unavailable (throws exception)
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Rule engine service unavailable"));

        // When: Get formula for municipality
        ApportionmentFormula formula = formulaConfigService.getApportionmentFormula(
                municipalityId, taxYear, tenantId);

        // Then: Should return default formula (four-factor double-weighted sales)
        assertEquals(ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES, formula);
    }

    @Test
    @DisplayName("Get formula weights for four-factor double-weighted sales")
    void testGetFormulaWeightsFourFactorDoubleWeightedSales() {
        // When: Get weights for four-factor double-weighted sales
        Map<String, BigDecimal> weights = formulaConfigService.getFormulaWeights(
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);

        // Then: Weights should be 25%, 25%, 50%
        assertEquals(new BigDecimal("0.25"), weights.get("property"));
        assertEquals(new BigDecimal("0.25"), weights.get("payroll"));
        assertEquals(new BigDecimal("0.50"), weights.get("sales"));

        // Verify weights sum to 1.0
        BigDecimal sum = weights.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(BigDecimal.ONE, sum);
    }

    @Test
    @DisplayName("Get formula weights for three-factor equal weighted")
    void testGetFormulaWeightsThreeFactorEqualWeighted() {
        // When: Get weights for three-factor equal weighted
        Map<String, BigDecimal> weights = formulaConfigService.getFormulaWeights(
                ApportionmentFormula.TRADITIONAL_THREE_FACTOR);

        // Then: Weights should each be approximately 33.33%
        assertEquals(new BigDecimal("0.3333"), weights.get("property"));
        assertEquals(new BigDecimal("0.3333"), weights.get("payroll"));
        assertEquals(new BigDecimal("0.3334"), weights.get("sales"));

        // Verify weights sum to 1.0
        BigDecimal sum = weights.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertTrue(sum.compareTo(new BigDecimal("0.9999")) > 0);
        assertTrue(sum.compareTo(new BigDecimal("1.0001")) < 0);
    }

    @Test
    @DisplayName("Get formula weights for single-sales-factor")
    void testGetFormulaWeightsSingleSalesFactor() {
        // When: Get weights for single-sales-factor
        Map<String, BigDecimal> weights = formulaConfigService.getFormulaWeights(
                ApportionmentFormula.SINGLE_SALES_FACTOR);

        // Then: Only sales factor has weight (100%)
        assertEquals(BigDecimal.ZERO, weights.get("property"));
        assertEquals(BigDecimal.ZERO, weights.get("payroll"));
        assertEquals(BigDecimal.ONE, weights.get("sales"));
    }

    @Test
    @DisplayName("Check if municipality allows single-sales-factor election")
    void testAllowsSingleSalesFactorElection() {
        // Given: Rule engine indicates single-sales-factor is allowed
        Map<String, Object> ruleEngineResponse = Map.of(
                "allowed", true,
                "municipalityId", municipalityId.toString()
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(ruleEngineResponse);

        // When: Check if single-sales-factor is allowed
        boolean allowed = formulaConfigService.allowsSingleSalesFactor(
                municipalityId, taxYear, tenantId);

        // Then: Should return true
        assertTrue(allowed);
    }

    @Test
    @DisplayName("Default to not allowing single-sales-factor when rule engine unavailable")
    void testDefaultToNotAllowingSingleSalesFactorWhenRuleEngineUnavailable() {
        // Given: Rule engine is unavailable
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When: Check if single-sales-factor is allowed
        boolean allowed = formulaConfigService.allowsSingleSalesFactor(
                municipalityId, taxYear, tenantId);

        // Then: Should default to false (not allowed)
        assertFalse(allowed);
    }

    @Test
    @DisplayName("Get formula description for display")
    void testGetFormulaDescription() {
        // When: Get descriptions for each formula
        String fourFactorDesc = formulaConfigService.getFormulaDescription(
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);
        String threeFactorDesc = formulaConfigService.getFormulaDescription(
                ApportionmentFormula.TRADITIONAL_THREE_FACTOR);
        String singleSalesDesc = formulaConfigService.getFormulaDescription(
                ApportionmentFormula.SINGLE_SALES_FACTOR);

        // Then: Descriptions should be human-readable
        assertTrue(fourFactorDesc.contains("Four-Factor"));
        assertTrue(fourFactorDesc.contains("50%"));
        assertTrue(threeFactorDesc.contains("Three-Factor"));
        assertTrue(threeFactorDesc.contains("33.33%"));
        assertTrue(singleSalesDesc.contains("Single Sales Factor"));
        assertTrue(singleSalesDesc.contains("100%"));
    }

    @Test
    @DisplayName("Handle unknown formula gracefully")
    void testHandleUnknownFormula() {
        // When: Get description for unknown formula (null)
        String description = formulaConfigService.getFormulaDescription(null);

        // Then: Should return unknown formula message
        assertEquals("Unknown Formula", description);
    }

    @Test
    @DisplayName("Verify formula weights are immutable")
    void testFormulaWeightsAreImmutable() {
        // When: Get formula weights
        Map<String, BigDecimal> weights = formulaConfigService.getFormulaWeights(
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);

        // Then: Should not be able to modify the map
        assertThrows(UnsupportedOperationException.class, () -> {
            weights.put("property", new BigDecimal("0.50"));
        });
    }

    @Test
    @DisplayName("Verify RestTemplate is called with correct URL format")
    void testRestTemplateCalledWithCorrectUrl() {
        // Given: Mock rule engine response
        Map<String, Object> response = Map.of("formula", "SINGLE_SALES_FACTOR");
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(response);

        // When: Get apportionment formula
        formulaConfigService.getApportionmentFormula(municipalityId, taxYear, tenantId);

        // Then: Verify RestTemplate called with URL containing query parameters
        verify(restTemplate).getForObject(
                argThat(url -> url.toString().contains("municipalityId=")
                        && url.toString().contains("taxYear=")
                        && url.toString().contains("tenantId=")),
                eq(Map.class)
        );
    }
}
