package com.munitax.taxengine.model;

import java.util.List;

public record TaxCalculationResult(
    TaxReturnSettings settings,
    TaxPayerProfile profile,
    Double totalGrossIncome,
    Double totalLocalWithheld,
    Double w2TaxableIncome,
    ScheduleXResult scheduleX,
    ScheduleYResult scheduleY,
    Double totalTaxableIncome,
    Double municipalLiability,
    Double municipalLiabilityAfterCredits,
    Double municipalBalance,
    List<TaxBreakdownRule> breakdown,
    DiscrepancyReport discrepancyReport
) {
    public record TaxReturnSettings(
        int taxYear,
        boolean isAmendment,
        String amendmentReason
    ) {}
    
    public record ScheduleXResult(
        List<ScheduleXEntry> entries,
        Double totalNetProfit
    ) {}
    
    public record ScheduleYResult(
        List<ScheduleYEntry> entries,
        Double totalCredit
    ) {}
    
    public record ScheduleXEntry(
        String source,
        String type,
        Double gross,
        Double expenses,
        Double netProfit
    ) {}
    
    public record ScheduleYEntry(
        String source,
        String locality,
        Double cityTaxRate,
        Double incomeTaxedByOtherCity,
        Double taxPaidToOtherCity,
        Double creditAllowed
    ) {}
    
    public record TaxBreakdownRule(
        String category,
        String ruleName,
        String description,
        String calculation,
        Double amount
    ) {}
    
    public record DiscrepancyReport(
        boolean hasDiscrepancies,
        List<DiscrepancyIssue> issues
    ) {
        public record DiscrepancyIssue(
            String field,
            Double sourceValue,
            Double formValue,
            Double difference,
            String severity, // HIGH, MEDIUM, LOW
            String message
        ) {}
    }
}
