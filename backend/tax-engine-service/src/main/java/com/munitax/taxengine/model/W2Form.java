package com.munitax.taxengine.model;

import java.util.List;
import java.util.Map;

public record W2Form(
    String id,
    String fileName,
    int taxYear,
    TaxFormType formType,
    Double confidenceScore,
    Map<String, Double> fieldConfidence,
    Integer sourcePage,
    String extractionReason,
    String owner,
    
    // W2 Specific
    String employer,
    String employerEin,
    Address employerAddress,
    String employerCounty,
    Integer totalMonthsInCity,
    String employee,
    TaxPayerProfile employeeInfo,
    Double federalWages,
    Double medicareWages,
    Double localWages,
    Double localWithheld,
    String locality,
    Double taxDue,
    List<String> lowConfidenceFields
) implements TaxFormData {}
