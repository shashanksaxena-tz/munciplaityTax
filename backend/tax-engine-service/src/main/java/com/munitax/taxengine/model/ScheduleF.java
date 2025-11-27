package com.munitax.taxengine.model;

import java.util.List;
import java.util.Map;

public record ScheduleF(
    String id,
    String fileName,
    int taxYear,
    TaxFormType formType,
    Double confidenceScore,
    Map<String, Double> fieldConfidence,
    Integer sourcePage,
    String extractionReason,
    String owner,
    
    // Schedule F Specific
    String principalProduct,
    String businessName,
    String businessCode,
    String ein,
    Double grossIncome,
    Double totalExpenses,
    Double netFarmProfit,
    List<String> lowConfidenceFields
) implements TaxFormData {}
