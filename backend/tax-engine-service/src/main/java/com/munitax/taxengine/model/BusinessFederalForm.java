package com.munitax.taxengine.model;

import java.util.Map;

public record BusinessFederalForm(
    String id,
    String fileName,
    int taxYear,
    TaxFormType formType,
    Double confidenceScore,
    Map<String, Double> fieldConfidence,
    Integer sourcePage,
    String extractionReason,
    String owner,
    
    // Business Federal Specific
    String businessName,
    String ein,
    Double fedTaxableIncome,
    BusinessScheduleXDetails reconciliation,
    BusinessAllocation allocation
) implements TaxFormData {
    
    public record BusinessScheduleXDetails(
        Double fedTaxableIncome,
        AddBacks addBacks,
        Deductions deductions
    ) {
        public record AddBacks(
            Double interestAndStateTaxes,
            Double wagesCredit,
            Double losses1231,
            Double guaranteedPayments,
            Double expensesOnIntangibleIncome,
            Double other
        ) {}
        
        public record Deductions(
            Double interestIncome,
            Double dividends,
            Double capitalGains,
            Double section179Excess,
            Double other
        ) {}
    }
    
    public record BusinessAllocation(
        Factor property,
        Factor payroll,
        Factor sales,
        Double totalPct,
        Double averagePct
    ) {
        public record Factor(Double dublin, Double everywhere, Double pct) {}
    }
}
