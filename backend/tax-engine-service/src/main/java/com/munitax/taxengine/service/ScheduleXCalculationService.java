package com.munitax.taxengine.service;

import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails.AddBacks;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails.Deductions;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails.CalculatedFields;
import org.springframework.stereotype.Service;

/**
 * Service for Schedule X calculation logic (FR-028, FR-029, FR-030).
 * Handles:
 * - FR-028: Calculate total add-backs (sum of 20 fields)
 * - FR-029: Calculate total deductions (sum of 7 fields)
 * - FR-030: Calculate adjusted municipal income (federal + addBacks - deductions)
 */
@Service
public class ScheduleXCalculationService {
    
    /**
     * Calculate total add-backs from all 22 add-back fields (FR-028)
     *
     * @param addBacks Add-backs object with 22 fields
     * @return Sum of all add-back fields
     */
    public Double calculateTotalAddBacks(AddBacks addBacks) {
        if (addBacks == null) {
            return 0.0;
        }
        
        return safeDouble(addBacks.depreciationAdjustment()) +
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
    }
    
    /**
     * Calculate total deductions from all 7 deduction fields (FR-029)
     *
     * @param deductions Deductions object with 7 fields
     * @return Sum of all deduction fields
     */
    public Double calculateTotalDeductions(Deductions deductions) {
        if (deductions == null) {
            return 0.0;
        }
        
        return safeDouble(deductions.interestIncome()) +
               safeDouble(deductions.dividends()) +
               safeDouble(deductions.capitalGains()) +
               safeDouble(deductions.section179Recapture()) +
               safeDouble(deductions.municipalBondInterest()) +
               safeDouble(deductions.depletionDifference()) +
               safeDouble(deductions.otherDeductions());
    }
    
    /**
     * Calculate adjusted municipal income (FR-030)
     * Formula: Federal Taxable Income + Total Add-Backs - Total Deductions
     *
     * @param scheduleX Complete Schedule X details
     * @return Adjusted municipal taxable income
     */
    public Double calculateAdjustedMunicipalIncome(BusinessScheduleXDetails scheduleX) {
        if (scheduleX == null) {
            return 0.0;
        }
        
        double fedTaxableIncome = safeDouble(scheduleX.fedTaxableIncome());
        double totalAddBacks = calculateTotalAddBacks(scheduleX.addBacks());
        double totalDeductions = calculateTotalDeductions(scheduleX.deductions());
        
        return fedTaxableIncome + totalAddBacks - totalDeductions;
    }
    
    /**
     * Recalculate all totals and return updated Schedule X with calculated fields
     *
     * @param scheduleX Schedule X to recalculate
     * @return Updated Schedule X with recalculated totals
     */
    public BusinessScheduleXDetails recalculateAll(BusinessScheduleXDetails scheduleX) {
        if (scheduleX == null) {
            return null;
        }
        
        Double totalAddBacks = calculateTotalAddBacks(scheduleX.addBacks());
        Double totalDeductions = calculateTotalDeductions(scheduleX.deductions());
        Double adjustedIncome = safeDouble(scheduleX.fedTaxableIncome()) + totalAddBacks - totalDeductions;
        
        CalculatedFields calculatedFields = new CalculatedFields(
            totalAddBacks,
            totalDeductions,
            adjustedIncome
        );
        
        return new BusinessScheduleXDetails(
            scheduleX.fedTaxableIncome(),
            scheduleX.addBacks(),
            scheduleX.deductions(),
            calculatedFields,
            scheduleX.metadata()
        );
    }
    
    /**
     * Safe conversion of Double to primitive double (null -> 0.0)
     */
    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}
