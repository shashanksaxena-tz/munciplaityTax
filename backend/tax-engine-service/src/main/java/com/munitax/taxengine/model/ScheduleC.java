package com.munitax.taxengine.model;

import java.util.List;
import java.util.Map;

public record ScheduleC(
    String id,
    String fileName,
    int taxYear,
    TaxFormType formType,
    Double confidenceScore,
    Map<String, Double> fieldConfidence,
    Integer sourcePage,
    String extractionReason,
    String owner,
    
    // Schedule C Specific
    String principalBusiness,
    String businessCode,
    String businessName,
    String businessEin,
    Address businessAddress,
    Double grossReceipts,
    Double totalExpenses,
    Double netProfit,
    List<String> lowConfidenceFields
) implements TaxFormData {}
