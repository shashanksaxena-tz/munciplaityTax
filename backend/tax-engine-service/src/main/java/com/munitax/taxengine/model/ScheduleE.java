package com.munitax.taxengine.model;

import java.util.List;
import java.util.Map;

public record ScheduleE(
    String id,
    String fileName,
    int taxYear,
    TaxFormType formType,
    Double confidenceScore,
    Map<String, Double> fieldConfidence,
    Integer sourcePage,
    String extractionReason,
    String owner,
    
    // Schedule E Specific
    List<RentalProperty> rentals,
    List<PartnershipEntity> partnerships,
    Double totalNetIncome,
    List<String> lowConfidenceFields
) implements TaxFormData {
    public record RentalProperty(
        String id, 
        String streetAddress, 
        String city, 
        String state, 
        String zip, 
        String rentalType, 
        Double line21_FairRentalDays_or_Income, 
        Double line22_DeductibleLoss, 
        Double calculatedNetIncome
    ) {}
    
    public record PartnershipEntity(
        String id, 
        String name, 
        String ein, 
        Double netProfit
    ) {}
}
