package com.munitax.taxengine.service;

import com.munitax.taxengine.model.*;
import org.springframework.stereotype.Service;

@Service
public class BusinessTaxCalculator {

    public NetProfitReturnData calculateBusinessTax(
            int year,
            double estimates,
            double priorCredit,
            BusinessFederalForm.BusinessScheduleXDetails schX,
            BusinessFederalForm.BusinessAllocation schY,
            double nolCarryforward,
            BusinessTaxRulesConfig rules) {

        // 1. Calculate Adjusted Federal Taxable Income (Schedule X)
        double totalAddBacks = schX.addBacks().interestAndStateTaxes() +
                schX.addBacks().wagesCredit() +
                schX.addBacks().losses1231() +
                schX.addBacks().guaranteedPayments() +
                schX.addBacks().expensesOnIntangibleIncome() +
                schX.addBacks().other();

        double totalDeductions = schX.deductions().interestIncome() +
                schX.deductions().dividends() +
                schX.deductions().capitalGains() +
                schX.deductions().section179Excess() +
                schX.deductions().other();

        double adjustedFedIncome = schX.fedTaxableIncome() + totalAddBacks - totalDeductions;

        // 2. Calculate Allocation % (Schedule Y) with Weighted Factors
        double propertyPct = safeDiv(schY.property().dublin(), schY.property().everywhere());
        double payrollPct = safeDiv(schY.payroll().dublin(), schY.payroll().everywhere());
        double salesPct = safeDiv(schY.sales().dublin(), schY.sales().everywhere());

        double factorSum = 0;
        double factorDivisor = 0;

        if (schY.property().everywhere() > 0) {
            factorSum += propertyPct;
            factorDivisor += 1;
        }
        if (schY.payroll().everywhere() > 0) {
            factorSum += payrollPct;
            factorDivisor += 1;
        }
        if (schY.sales().everywhere() > 0) {
            factorSum += (salesPct * rules.allocationSalesFactorWeight());
            factorDivisor += rules.allocationSalesFactorWeight();
        }

        double averagePct = factorDivisor == 0 ? 0 : factorSum / factorDivisor;

        // Update allocation with calculated percentages
        BusinessFederalForm.BusinessAllocation updatedSchY = new BusinessFederalForm.BusinessAllocation(
                new BusinessFederalForm.BusinessAllocation.Factor(schY.property().dublin(),
                        schY.property().everywhere(), propertyPct),
                new BusinessFederalForm.BusinessAllocation.Factor(schY.payroll().dublin(), schY.payroll().everywhere(),
                        payrollPct),
                new BusinessFederalForm.BusinessAllocation.Factor(schY.sales().dublin(), schY.sales().everywhere(),
                        salesPct),
                factorSum,
                averagePct); // 3. Tax Calculation
        double allocatedIncome = adjustedFedIncome * averagePct;

        // 4. NOL Application
        double nolApplied = 0;
        double taxableIncomeAfterNOL = allocatedIncome;

        if (rules.enableNOL() && allocatedIncome > 0 && nolCarryforward > 0) {
            double nolLimit = allocatedIncome * rules.nolOffsetCapPercent();
            nolApplied = Math.min(nolCarryforward, Math.min(nolLimit, allocatedIncome));
            taxableIncomeAfterNOL = allocatedIncome - nolApplied;
        } else if (allocatedIncome < 0) {
            taxableIncomeAfterNOL = 0;
        }

        // 5. Final Tax
        double taxDue = Math.max(rules.minimumTax(), taxableIncomeAfterNOL * rules.municipalRate());

        // 6. Penalty & Interest Logic
        double totalPayments = estimates + priorCredit;
        double requiredPayment = taxDue * rules.safeHarborPercent();
        double penaltyUnderpayment = 0;

        if (totalPayments < requiredPayment && taxDue > 200) {
            penaltyUnderpayment = (taxDue - totalPayments) * rules.penaltyRateUnderpayment();
        }

        double interest = 0; // Simplified interest calculation

        double balance = (taxDue + penaltyUnderpayment + interest) - totalPayments;

        return new NetProfitReturnData(
                java.util.UUID.randomUUID().toString(),
                java.time.LocalDateTime.now().toString(),
                year,
                schX,
                updatedSchY,
                adjustedFedIncome,
                allocatedIncome,
                nolCarryforward,
                nolApplied,
                taxableIncomeAfterNOL,
                taxDue,
                estimates,
                priorCredit,
                penaltyUnderpayment,
                0.0, // penaltyLateFiling
                interest,
                Math.max(0, balance),
                "UNPAID");
    }

    private double safeDiv(double numerator, double denominator) {
        return denominator == 0 ? 0 : numerator / denominator;
    }
}
