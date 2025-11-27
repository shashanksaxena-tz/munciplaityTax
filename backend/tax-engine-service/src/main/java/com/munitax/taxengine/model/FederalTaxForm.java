package com.munitax.taxengine.model;

import java.util.Map;

public record FederalTaxForm(
    String id,
    String fileName,
    int taxYear,
    TaxFormType formType,
    Double confidenceScore,
    Map<String, Double> fieldConfidence,
    Integer sourcePage,
    String extractionReason,
    String owner,
    
    // Federal 1040 Specific
    Double wages, // 1z
    Double qualifiedDividends, // 3a
    Double pensions, // 5b
    Double socialSecurity, // 6b
    Double capitalGains, // 7
    Double otherIncome, // 8
    Double totalIncome, // 9
    Double adjustedGrossIncome, // 11
    Double tax // 24
) implements TaxFormData {}
