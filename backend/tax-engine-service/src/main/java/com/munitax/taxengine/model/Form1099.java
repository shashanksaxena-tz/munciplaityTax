package com.munitax.taxengine.model;

import java.util.List;
import java.util.Map;

public record Form1099(
    String id,
    String fileName,
    int taxYear,
    TaxFormType formType, // NEC or MISC
    Double confidenceScore,
    Map<String, Double> fieldConfidence,
    Integer sourcePage,
    String extractionReason,
    String owner,
    
    // 1099 Specific
    String payer,
    String payerTin,
    Address payerAddress,
    String recipient,
    Double incomeAmount,
    Double federalWithheld,
    Double stateWithheld,
    Double localWithheld,
    String locality,
    List<String> lowConfidenceFields
) implements TaxFormData {}
