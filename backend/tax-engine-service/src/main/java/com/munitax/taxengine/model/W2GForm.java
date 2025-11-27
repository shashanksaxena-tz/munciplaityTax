package com.munitax.taxengine.model;

import java.util.List;
import java.util.Map;

public record W2GForm(
    String id,
    String fileName,
    int taxYear,
    TaxFormType formType,
    Double confidenceScore,
    Map<String, Double> fieldConfidence,
    Integer sourcePage,
    String extractionReason,
    String owner,
    
    // W2G Specific
    String payer,
    String payerEin,
    Address payerAddress,
    String recipient,
    String recipientTin,
    Double grossWinnings,
    String dateWon,
    String typeOfWager,
    Double federalWithheld,
    Double stateWithheld,
    Double localWinnings,
    Double localWithheld,
    String locality,
    List<String> lowConfidenceFields
) implements TaxFormData {}
