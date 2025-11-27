package com.munitax.taxengine.model;

import java.util.Map;

public record LocalTaxForm(
    String id,
    String fileName,
    int taxYear,
    TaxFormType formType,
    Double confidenceScore,
    Map<String, Double> fieldConfidence,
    Integer sourcePage,
    String extractionReason,
    String owner,
    
    // Local Form Specific
    Double qualifyingWages, // Line 1
    Double otherIncome,     // Line 2
    Double totalIncome,     // Line 3
    Double taxDue,          // Line 6
    Double credits,         // Line 7
    Double overpayment,     // Line 13
    Double reportedTaxableIncome,
    Double reportedTaxDue
) implements TaxFormData {}
