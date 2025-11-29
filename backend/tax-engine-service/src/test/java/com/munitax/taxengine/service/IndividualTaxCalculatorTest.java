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

    @Test
    void testW2BoxVarianceDetection_FR001() {
        // Test FR-001: W-2 Box 18 variance from Box 1
        W2Form w2 = new W2Form(
                "1", "w2.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Acme Corp", "12-3456789", null, "Franklin", 12, "John Doe", null,
                75000.0, 75000.0, 7500.0, 150.0, "Dublin", 0.0, List.of());

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

        // Assert discrepancy is detected
        assertTrue(result.discrepancyReport().hasDiscrepancies());
        assertTrue(result.discrepancyReport().issues().stream()
                .anyMatch(i -> i.ruleId().equals("FR-001")));
    }

    @Test
    void testWithholdingRateValidation_FR002() {
        // Test FR-002: Withholding rate exceeds 3.0%
        W2Form w2 = new W2Form(
                "1", "w2.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Acme Corp", "12-3456789", null, "Franklin", 12, "John Doe", null,
                50000.0, 50000.0, 50000.0, 1600.0, "Dublin", 0.0, List.of());

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

        // Assert over-withholding is detected
        assertTrue(result.discrepancyReport().hasDiscrepancies());
        assertTrue(result.discrepancyReport().issues().stream()
                .anyMatch(i -> i.ruleId().equals("FR-002") && i.severity().equals("MEDIUM")));
    }

    @Test
    void testDuplicateW2Detection_FR003() {
        // Test FR-003: Duplicate W-2 detection
        W2Form w2a = new W2Form(
                "1", "w2a.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Acme Corp", "12-3456789", null, "Franklin", 12, "John Doe", null,
                50000.0, 50000.0, 50000.0, 1000.0, "Dublin", 0.0, List.of());
        
        W2Form w2b = new W2Form(
                "2", "w2b.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Acme Corp", "12-3456789", null, "Franklin", 12, "John Doe", null,
                50000.0, 50000.0, 50000.0, 1000.0, "Dublin", 0.0, List.of());

        TaxRulesConfig rules = new TaxRulesConfig(
                0.020, 0.020, Map.of(),
                TaxRulesConfig.W2QualifyingWagesRule.HIGHEST_OF_ALL,
                new TaxRulesConfig.IncomeInclusion(true, true, true, true, true),
                true);

        TaxCalculationResult result = calculator.calculateTaxes(
                List.of(w2a, w2b),
                null,
                null,
                rules);

        // Assert duplicate is detected
        assertTrue(result.discrepancyReport().hasDiscrepancies());
        assertTrue(result.discrepancyReport().issues().stream()
                .anyMatch(i -> i.ruleId().equals("FR-003") && i.severity().equals("HIGH")));
    }

    @Test
    void testScheduleCEstimatedTaxValidation_FR006() {
        // Test FR-006: Schedule C estimated tax validation
        ScheduleC schedC = new ScheduleC(
                "1", "schedC.pdf", 2023, TaxFormType.SCHEDULE_C, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Software Development", "541511", "John's Consulting", "12-3456789", null,
                120000.0, 20000.0, 100000.0, List.of());

        TaxRulesConfig rules = new TaxRulesConfig(
                0.020, 0.020, Map.of(),
                TaxRulesConfig.W2QualifyingWagesRule.HIGHEST_OF_ALL,
                new TaxRulesConfig.IncomeInclusion(true, true, true, true, true),
                true);

        TaxCalculationResult result = calculator.calculateTaxes(
                List.of(schedC),
                null,
                null,
                rules);

        // Assert estimated tax warning is present
        assertTrue(result.discrepancyReport().hasDiscrepancies());
        assertTrue(result.discrepancyReport().issues().stream()
                .anyMatch(i -> i.ruleId().equals("FR-006")));
    }

    @Test
    void testMunicipalCreditLimitValidation_FR014() {
        // Test FR-014: Municipal credits cannot exceed liability
        W2Form w2Cleveland = new W2Form(
                "1", "w2.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Cleveland Corp", "98-7654321", null, "Cuyahoga", 12, "John Doe", null,
                50000.0, 50000.0, 50000.0, 1500.0, "Cleveland", 0.0, List.of());

        TaxRulesConfig rules = new TaxRulesConfig(
                0.020, 0.020, Map.of(),
                TaxRulesConfig.W2QualifyingWagesRule.HIGHEST_OF_ALL,
                new TaxRulesConfig.IncomeInclusion(true, true, true, true, true),
                true);

        TaxCalculationResult result = calculator.calculateTaxes(
                List.of(w2Cleveland),
                null,
                null,
                rules);

        // Assert credit limit warning is present (1500 credit > 1000 liability)
        assertTrue(result.discrepancyReport().hasDiscrepancies());
        assertTrue(result.discrepancyReport().issues().stream()
                .anyMatch(i -> i.ruleId().equals("FR-014") && i.severity().equals("HIGH")));
    }

    @Test
    void testFederalWagesReconciliation_FR019() {
        // Test FR-019: Federal wages vs W-2 totals
        W2Form w2 = new W2Form(
                "1", "w2.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Acme Corp", "12-3456789", null, "Franklin", 12, "John Doe", null,
                50000.0, 50000.0, 50000.0, 1000.0, "Dublin", 0.0, List.of());

        FederalTaxForm federal = new FederalTaxForm(
                "2", "1040.pdf", 2023, TaxFormType.FEDERAL_1040, 0.99, Map.of(), 1, "AI", "PRIMARY",
                55000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 55000.0, 55000.0, 0.0);

        TaxRulesConfig rules = new TaxRulesConfig(
                0.020, 0.020, Map.of(),
                TaxRulesConfig.W2QualifyingWagesRule.HIGHEST_OF_ALL,
                new TaxRulesConfig.IncomeInclusion(true, true, true, true, true),
                true);

        TaxCalculationResult result = calculator.calculateTaxes(
                List.of(w2, federal),
                null,
                null,
                rules);

        // Assert federal reconciliation issue is detected
        assertTrue(result.discrepancyReport().hasDiscrepancies());
        assertTrue(result.discrepancyReport().issues().stream()
                .anyMatch(i -> i.ruleId().equals("FR-019")));
    }

    @Test
    void testNoDiscrepanciesWithValidData() {
        // Test that valid data produces no discrepancies
        W2Form w2 = new W2Form(
                "1", "w2.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Acme Corp", "12-3456789", null, "Franklin", 12, "John Doe", null,
                50000.0, 50000.0, 50000.0, 1000.0, "Dublin", 0.0, List.of());

        FederalTaxForm federal = new FederalTaxForm(
                "2", "1040.pdf", 2023, TaxFormType.FEDERAL_1040, 0.99, Map.of(), 1, "AI", "PRIMARY",
                50000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 50000.0, 50000.0, 0.0);

        TaxRulesConfig rules = new TaxRulesConfig(
                0.020, 0.020, Map.of(),
                TaxRulesConfig.W2QualifyingWagesRule.HIGHEST_OF_ALL,
                new TaxRulesConfig.IncomeInclusion(true, true, true, true, true),
                true);

        TaxCalculationResult result = calculator.calculateTaxes(
                List.of(w2, federal),
                null,
                null,
                rules);

        // Assert no discrepancies
        assertFalse(result.discrepancyReport().hasDiscrepancies());
        assertEquals(0, result.discrepancyReport().issues().size());
    }

    @Test
    void testDiscrepancySummaryGeneration() {
        // Test that summary is properly generated
        W2Form w2High = new W2Form(
                "1", "w2a.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Acme Corp", "12-3456789", null, "Franklin", 12, "John Doe", null,
                50000.0, 50000.0, 50000.0, 1000.0, "Dublin", 0.0, List.of());
        
        W2Form w2Dup = new W2Form(
                "2", "w2b.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Acme Corp", "12-3456789", null, "Franklin", 12, "John Doe", null,
                50000.0, 50000.0, 50000.0, 1000.0, "Dublin", 0.0, List.of());

        W2Form w2Medium = new W2Form(
                "3", "w2c.pdf", 2023, TaxFormType.W2, 0.99, Map.of(), 1, "AI", "PRIMARY",
                "Beta Corp", "98-7654321", null, "Franklin", 12, "Jane Doe", null,
                60000.0, 60000.0, 60000.0, 1900.0, "Dublin", 0.0, List.of());

        TaxRulesConfig rules = new TaxRulesConfig(
                0.020, 0.020, Map.of(),
                TaxRulesConfig.W2QualifyingWagesRule.HIGHEST_OF_ALL,
                new TaxRulesConfig.IncomeInclusion(true, true, true, true, true),
                true);

        TaxCalculationResult result = calculator.calculateTaxes(
                List.of(w2High, w2Dup, w2Medium),
                null,
                null,
                rules);

        // Assert summary exists and has correct counts
        assertNotNull(result.discrepancyReport().summary());
        assertTrue(result.discrepancyReport().summary().totalIssues() > 0);
        assertTrue(result.discrepancyReport().summary().highSeverityCount() > 0);
        assertTrue(result.discrepancyReport().summary().blocksFiling());
    }
}
