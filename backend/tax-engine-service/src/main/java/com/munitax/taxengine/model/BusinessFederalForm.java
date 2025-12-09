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
    
    /**
     * Expanded BusinessScheduleXDetails with 29 fields for complete M-1 reconciliation.
     * Supports both old 6-field format (runtime conversion) and new 29-field format.
     */
    public record BusinessScheduleXDetails(
        Double fedTaxableIncome,
        AddBacks addBacks,
        Deductions deductions,
        CalculatedFields calculatedFields,
        Metadata metadata
    ) {
        /**
         * Add-backs (22 fields) - adjustments that increase federal taxable income
         */
        public record AddBacks(
            // Old fields (maintained for backward compatibility)
            Double interestAndStateTaxes,      // FR-003 - renamed from old format for consistency
            Double guaranteedPayments,          // FR-004
            Double expensesOnIntangibleIncome,  // FR-012 (5% Rule)
            
            // New fields (FR-001 to FR-020)
            Double depreciationAdjustment,      // FR-001 - Book vs MACRS
            Double amortizationAdjustment,      // FR-002 - Intangible assets
            Double mealsAndEntertainment,       // FR-005 - 100% add-back
            Double relatedPartyExcess,          // FR-006 - Above FMV
            Double penaltiesAndFines,           // FR-007 - Government penalties
            Double politicalContributions,      // FR-008 - Campaign contributions
            Double officerLifeInsurance,        // FR-009 - Premiums where corp is beneficiary
            Double capitalLossExcess,           // FR-010 - renamed from losses1231
            Double federalTaxRefunds,           // FR-011 - Prior year refunds
            Double section179Excess,            // FR-013 - Over municipal limit
            Double bonusDepreciation,           // FR-014 - 100% federal bonus
            Double badDebtReserveIncrease,      // FR-015 - Reserve method adjustment
            Double charitableContributionExcess,// FR-016 - Over 10% limit
            Double domesticProductionActivities,// FR-017 - DPAD Section 199
            Double stockCompensationAdjustment, // FR-018 - Book vs tax
            Double inventoryMethodChange,       // FR-019 - Section 481(a)
            Double clubDues,                    // FR-020A - Non-deductible club dues
            Double pensionProfitSharingLimits,  // FR-020B - Excess pension/profit-sharing contributions
            Double otherAddBacks,               // FR-020 - catch-all (renamed from "other")
            String otherAddBacksDescription,    // Required if otherAddBacks > 0
            
            // Legacy fields no longer used (maintained for old format conversion)
            Double wagesCredit                  // Deprecated - moved to tax credits section
        ) {
            /**
             * Default constructor for new 29-field format (all fields initialized to 0)
             */
            public static AddBacks createEmpty() {
                return new AddBacks(
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null, 0.0
                );
            }
        }
        
        /**
         * Deductions (7 fields) - adjustments that decrease federal taxable income
         */
        public record Deductions(
            // Old fields (maintained for backward compatibility)
            Double interestIncome,              // FR-021
            Double dividends,                   // FR-022
            Double capitalGains,                // FR-023
            Double section179Excess,            // Deprecated - moved to addBacks.section179Excess
            Double otherDeductions,             // FR-027 - renamed from "other"
            
            // New fields (FR-024 to FR-027)
            Double section179Recapture,         // FR-024 - Recaptured deduction
            Double municipalBondInterest,       // FR-025 - Cross-jurisdiction
            Double depletionDifference,         // FR-026 - Percentage vs cost
            String otherDeductionsDescription   // Required if otherDeductions > 0
        ) {
            /**
             * Default constructor for new 27-field format (all fields initialized to 0)
             */
            public static Deductions createEmpty() {
                return new Deductions(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null);
            }
        }
        
        /**
         * Calculated fields (read-only) - computed from addBacks and deductions
         */
        public record CalculatedFields(
            Double totalAddBacks,               // FR-028 - Sum of all 22 add-back fields
            Double totalDeductions,             // FR-029 - Sum of all 7 deduction fields
            Double adjustedMunicipalIncome      // FR-030 - fedTaxableIncome + totalAddBacks - totalDeductions
        ) {}
        
        /**
         * Metadata for audit trail and AI extraction tracking
         */
        public record Metadata(
            String lastModified,                     // ISO 8601 timestamp
            java.util.List<String> autoCalculatedFields,  // FR-037 - Fields auto-calculated
            java.util.List<String> manualOverrides,       // FR-037 - Fields manually overridden
            java.util.List<AttachedDocument> attachedDocuments  // FR-036 - Supporting docs
        ) {
            public record AttachedDocument(
                String fileName,
                String fileUrl,
                String fieldName,
                String uploadedAt,
                String uploadedBy
            ) {}
        }
        
        /**
         * Recalculate totals and adjusted municipal income
         */
        public BusinessScheduleXDetails recalculateTotals() {
            if (addBacks == null || deductions == null) {
                return this;
            }
            
            double totalAddBacks = 
                safeDouble(addBacks.depreciationAdjustment()) +
                safeDouble(addBacks.amortizationAdjustment()) +
                safeDouble(addBacks.interestAndStateTaxes()) +
                safeDouble(addBacks.guaranteedPayments()) +
                safeDouble(addBacks.mealsAndEntertainment()) +
                safeDouble(addBacks.relatedPartyExcess()) +
                safeDouble(addBacks.penaltiesAndFines()) +
                safeDouble(addBacks.politicalContributions()) +
                safeDouble(addBacks.officerLifeInsurance()) +
                safeDouble(addBacks.capitalLossExcess()) +
                safeDouble(addBacks.federalTaxRefunds()) +
                safeDouble(addBacks.expensesOnIntangibleIncome()) +
                safeDouble(addBacks.section179Excess()) +
                safeDouble(addBacks.bonusDepreciation()) +
                safeDouble(addBacks.badDebtReserveIncrease()) +
                safeDouble(addBacks.charitableContributionExcess()) +
                safeDouble(addBacks.domesticProductionActivities()) +
                safeDouble(addBacks.stockCompensationAdjustment()) +
                safeDouble(addBacks.inventoryMethodChange()) +
                safeDouble(addBacks.clubDues()) +
                safeDouble(addBacks.pensionProfitSharingLimits()) +
                safeDouble(addBacks.otherAddBacks());
            
            double totalDeductions =
                safeDouble(deductions.interestIncome()) +
                safeDouble(deductions.dividends()) +
                safeDouble(deductions.capitalGains()) +
                safeDouble(deductions.section179Recapture()) +
                safeDouble(deductions.municipalBondInterest()) +
                safeDouble(deductions.depletionDifference()) +
                safeDouble(deductions.otherDeductions());
            
            double adjustedIncome = safeDouble(fedTaxableIncome) + totalAddBacks - totalDeductions;
            
            CalculatedFields newCalculatedFields = new CalculatedFields(
                totalAddBacks,
                totalDeductions,
                adjustedIncome
            );
            
            return new BusinessScheduleXDetails(
                fedTaxableIncome,
                addBacks,
                deductions,
                newCalculatedFields,
                metadata
            );
        }
        
        private static double safeDouble(Double value) {
            return value != null ? value : 0.0;
        }
        
        /**
         * Create empty BusinessScheduleXDetails with all fields initialized to 0
         */
        public static BusinessScheduleXDetails createEmpty(Double fedTaxableIncome) {
            return new BusinessScheduleXDetails(
                fedTaxableIncome,
                AddBacks.createEmpty(),
                Deductions.createEmpty(),
                new CalculatedFields(0.0, 0.0, fedTaxableIncome),
                new Metadata(
                    java.time.Instant.now().toString(),
                    java.util.List.of(),
                    java.util.List.of(),
                    java.util.List.of()
                )
            );
        }
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
