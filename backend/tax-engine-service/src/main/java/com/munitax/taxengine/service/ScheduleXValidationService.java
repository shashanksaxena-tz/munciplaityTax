package com.munitax.taxengine.service;

import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails.AddBacks;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails.Deductions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for Schedule X validation logic (FR-033, FR-034).
 * Handles:
 * - FR-033: Validate federal income matches Form 1120/1065/1120-S
 * - FR-034: Flag if adjusted income differs from federal by >20%
 */
@Service
public class ScheduleXValidationService {
    
    private static final double VARIANCE_THRESHOLD = 0.20; // 20% variance threshold
    private static final double FEDERAL_INCOME_TOLERANCE = 100.0; // $100 tolerance for rounding
    
    /**
     * Validation result containing errors and warnings
     */
    public record ValidationResult(
        boolean valid,
        List<String> errors,
        List<String> warnings
    ) {
        public static ValidationResult valid() {
            return new ValidationResult(true, List.of(), List.of());
        }
        
        public static ValidationResult withErrors(List<String> errors) {
            return new ValidationResult(false, errors, List.of());
        }
        
        public static ValidationResult withWarnings(List<String> warnings) {
            return new ValidationResult(true, List.of(), warnings);
        }
        
        public static ValidationResult withErrorsAndWarnings(List<String> errors, List<String> warnings) {
            return new ValidationResult(errors.isEmpty(), errors, warnings);
        }
    }
    
    /**
     * Comprehensive validation of Schedule X data
     *
     * @param scheduleX Schedule X to validate
     * @param expectedFederalIncome Expected federal taxable income from Form 1120/1065/1120-S
     * @param entityType Entity type (C-CORP, PARTNERSHIP, S-CORP)
     * @return Validation result with errors and warnings
     */
    public ValidationResult validate(BusinessScheduleXDetails scheduleX, Double expectedFederalIncome, String entityType) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (scheduleX == null) {
            errors.add("Schedule X data is required");
            return ValidationResult.withErrors(errors);
        }
        
        // FR-033: Validate federal income matches expected value
        validateFederalIncome(scheduleX, expectedFederalIncome, errors);
        
        // Validate required fields
        validateRequiredFields(scheduleX, errors);
        
        // Validate numeric fields are non-negative
        validateNonNegativeFields(scheduleX, errors);
        
        // Validate entity-specific fields
        validateEntitySpecificFields(scheduleX, entityType, warnings);
        
        // FR-034: Check for variance >20%
        validateVariance(scheduleX, warnings);
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    /**
     * FR-033: Validate federal income matches Form 1120/1065/1120-S (within $100 tolerance)
     */
    private void validateFederalIncome(BusinessScheduleXDetails scheduleX, Double expectedFederalIncome, List<String> errors) {
        if (expectedFederalIncome == null) {
            return; // Skip if no expected value provided
        }
        
        Double actualFederalIncome = scheduleX.fedTaxableIncome();
        if (actualFederalIncome == null) {
            errors.add("Federal taxable income is required");
            return;
        }
        
        double difference = Math.abs(actualFederalIncome - expectedFederalIncome);
        if (difference > FEDERAL_INCOME_TOLERANCE) {
            errors.add(String.format(
                "Federal taxable income mismatch: Expected $%.2f, but Schedule X shows $%.2f (difference: $%.2f)",
                expectedFederalIncome, actualFederalIncome, difference
            ));
        }
    }
    
    /**
     * Validate required description fields
     */
    private void validateRequiredFields(BusinessScheduleXDetails scheduleX, List<String> errors) {
        AddBacks addBacks = scheduleX.addBacks();
        Deductions deductions = scheduleX.deductions();
        
        if (addBacks != null) {
            // otherAddBacksDescription required if otherAddBacks > 0
            if (safeDouble(addBacks.otherAddBacks()) > 0 && 
                (addBacks.otherAddBacksDescription() == null || addBacks.otherAddBacksDescription().trim().isEmpty())) {
                errors.add("Description is required when 'Other Add-Backs' amount is entered");
            }
        }
        
        if (deductions != null) {
            // otherDeductionsDescription required if otherDeductions > 0
            if (safeDouble(deductions.otherDeductions()) > 0 && 
                (deductions.otherDeductionsDescription() == null || deductions.otherDeductionsDescription().trim().isEmpty())) {
                errors.add("Description is required when 'Other Deductions' amount is entered");
            }
        }
    }
    
    /**
     * Validate all numeric fields are non-negative (add-backs and deductions cannot be negative)
     */
    private void validateNonNegativeFields(BusinessScheduleXDetails scheduleX, List<String> errors) {
        AddBacks addBacks = scheduleX.addBacks();
        if (addBacks != null) {
            validateNonNegative("Depreciation Adjustment", addBacks.depreciationAdjustment(), errors);
            validateNonNegative("Amortization Adjustment", addBacks.amortizationAdjustment(), errors);
            validateNonNegative("Income and State Taxes", addBacks.interestAndStateTaxes(), errors);
            validateNonNegative("Guaranteed Payments", addBacks.guaranteedPayments(), errors);
            validateNonNegative("Meals and Entertainment", addBacks.mealsAndEntertainment(), errors);
            validateNonNegative("Related Party Excess", addBacks.relatedPartyExcess(), errors);
            validateNonNegative("Penalties and Fines", addBacks.penaltiesAndFines(), errors);
            validateNonNegative("Political Contributions", addBacks.politicalContributions(), errors);
            validateNonNegative("Officer Life Insurance", addBacks.officerLifeInsurance(), errors);
            validateNonNegative("Capital Loss Excess", addBacks.capitalLossExcess(), errors);
            validateNonNegative("Federal Tax Refunds", addBacks.federalTaxRefunds(), errors);
            validateNonNegative("Expenses on Intangible Income", addBacks.expensesOnIntangibleIncome(), errors);
            validateNonNegative("Section 179 Excess", addBacks.section179Excess(), errors);
            validateNonNegative("Bonus Depreciation", addBacks.bonusDepreciation(), errors);
            validateNonNegative("Bad Debt Reserve Increase", addBacks.badDebtReserveIncrease(), errors);
            validateNonNegative("Charitable Contribution Excess", addBacks.charitableContributionExcess(), errors);
            validateNonNegative("Domestic Production Activities", addBacks.domesticProductionActivities(), errors);
            validateNonNegative("Stock Compensation Adjustment", addBacks.stockCompensationAdjustment(), errors);
            validateNonNegative("Inventory Method Change", addBacks.inventoryMethodChange(), errors);
            validateNonNegative("Other Add-Backs", addBacks.otherAddBacks(), errors);
        }
        
        Deductions deductions = scheduleX.deductions();
        if (deductions != null) {
            validateNonNegative("Interest Income", deductions.interestIncome(), errors);
            validateNonNegative("Dividends", deductions.dividends(), errors);
            validateNonNegative("Capital Gains", deductions.capitalGains(), errors);
            validateNonNegative("Section 179 Recapture", deductions.section179Recapture(), errors);
            validateNonNegative("Municipal Bond Interest", deductions.municipalBondInterest(), errors);
            validateNonNegative("Depletion Difference", deductions.depletionDifference(), errors);
            validateNonNegative("Other Deductions", deductions.otherDeductions(), errors);
        }
    }
    
    /**
     * Validate entity-specific fields (e.g., guaranteed payments only for partnerships)
     */
    private void validateEntitySpecificFields(BusinessScheduleXDetails scheduleX, String entityType, List<String> warnings) {
        if (entityType == null) {
            return;
        }
        
        AddBacks addBacks = scheduleX.addBacks();
        if (addBacks != null && !entityType.equalsIgnoreCase("PARTNERSHIP")) {
            // Guaranteed payments only apply to partnerships
            if (safeDouble(addBacks.guaranteedPayments()) > 0) {
                warnings.add("Guaranteed payments typically only apply to partnerships (Form 1065). Please verify this is correct for " + entityType);
            }
        }
    }
    
    /**
     * FR-034: Check for variance >20% between federal and adjusted municipal income
     */
    private void validateVariance(BusinessScheduleXDetails scheduleX, List<String> warnings) {
        if (scheduleX.calculatedFields() == null) {
            return;
        }
        
        Double fedIncome = safeDouble(scheduleX.fedTaxableIncome());
        Double adjustedIncome = safeDouble(scheduleX.calculatedFields().adjustedMunicipalIncome());
        
        if (fedIncome == 0) {
            return; // Skip variance check if federal income is zero
        }
        
        double variance = Math.abs(adjustedIncome - fedIncome) / Math.abs(fedIncome);
        
        if (variance > VARIANCE_THRESHOLD) {
            double variancePct = variance * 100;
            warnings.add(String.format(
                "⚠️ Adjusted Municipal Income ($%.2f) differs from Federal Taxable Income ($%.2f) by %.1f%% " +
                "(threshold: %.0f%%). Please verify all Schedule X adjustments are correct.",
                adjustedIncome, fedIncome, variancePct, VARIANCE_THRESHOLD * 100
            ));
        }
    }
    
    /**
     * Validate a single field is non-negative
     */
    private void validateNonNegative(String fieldName, Double value, List<String> errors) {
        if (value != null && value < 0) {
            errors.add(String.format("%s cannot be negative (value: $%.2f)", fieldName, value));
        }
    }
    
    /**
     * Safe conversion of Double to primitive double (null -> 0.0)
     */
    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}
