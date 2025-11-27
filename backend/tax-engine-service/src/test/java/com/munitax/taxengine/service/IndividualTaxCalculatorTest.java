package com.munitax.taxengine.service;

import com.munitax.taxengine.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndividualTaxCalculatorTest {

    private final IndividualTaxCalculator calculator = new IndividualTaxCalculator();

    @Test
    void testSimpleW2Calculation() {
        // Setup
        W2Form w2 = new W2Form(
                "1", "w2.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Acme Corp", "12-3456789", null, "Franklin", 12, "John Doe", null,
                50000.0, 50000.0, 50000.0, 1000.0, "Dublin", 0.0, List.of());

        TaxRulesConfig rules = new TaxRulesConfig(
                0.020, 0.020, Map.of(),
                TaxRulesConfig.W2QualifyingWagesRule.HIGHEST_OF_ALL,
                new TaxRulesConfig.IncomeInclusion(true, true, true, true, true),
                true);

        TaxCalculationResult result = calculator.calculateTaxes(
                List.of(w2),
                null,
                null,
                rules);

        // Assert
        assertEquals(50000.0, result.w2TaxableIncome());
        assertEquals(1000.0, result.municipalLiability());
        assertEquals(0.0, result.municipalBalance()); // 1000 withheld - 1000 liability = 0
    }
}
