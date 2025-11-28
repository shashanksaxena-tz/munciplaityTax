package com.munitax.taxengine.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for Schedule X auto-calculation helpers (FR-031, Research R3).
 * Handles complex calculations that require business logic:
 * - Meals & Entertainment: Federal deduction × 2 (50% → 100%)
 * - 5% Rule: (Interest + Dividends + Capital Gains) × 0.05
 * - Related-Party Excess: Paid Amount - Fair Market Value
 * - Charitable Contribution 10% Limit (with carryforward)
 * - Officer Compensation Reasonableness Test
 */
@Service
public class ScheduleXAutoCalculationService {
    
    /**
     * Auto-calculation request containing field name and input values
     */
    public record AutoCalcRequest(
        String fieldName,
        Map<String, Double> inputs
    ) {}
    
    /**
     * Auto-calculation response with calculated value and explanation
     */
    public record AutoCalcResponse(
        Double calculatedValue,
        String explanation,
        Map<String, Object> details
    ) {}
    
    /**
     * Execute auto-calculation based on field name and inputs
     *
     * @param request Auto-calculation request
     * @return Calculated value with explanation
     */
    public AutoCalcResponse autoCalculate(AutoCalcRequest request) {
        return switch (request.fieldName()) {
            case "mealsAndEntertainment" -> calculateMealsAddBack(request.inputs());
            case "expensesOnIntangibleIncome" -> calculate5PercentRule(request.inputs());
            case "relatedPartyExcess" -> calculateRelatedPartyExcess(request.inputs());
            case "charitableContributionExcess" -> calculateCharitableContribution(request.inputs());
            default -> new AutoCalcResponse(
                0.0,
                "Auto-calculation not available for field: " + request.fieldName(),
                Map.of()
            );
        };
    }
    
    /**
     * Calculate meals & entertainment add-back (Federal deduction × 2)
     * Federal allows 50% deduction, municipal allows 0%, so add back full expense
     *
     * Expected inputs:
     * - federalMealsDeduction: Amount deducted on federal return (50% of total expense)
     *
     * @param inputs Input values
     * @return Calculated add-back amount
     */
    private AutoCalcResponse calculateMealsAddBack(Map<String, Double> inputs) {
        Double federalDeduction = inputs.get("federalMealsDeduction");
        if (federalDeduction == null || federalDeduction <= 0) {
            return new AutoCalcResponse(
                0.0,
                "Federal meals deduction is required",
                Map.of()
            );
        }
        
        // Federal deducted 50%, municipal allows 0%, so add back full expense (federalDeduction × 2)
        Double totalExpense = federalDeduction * 2;
        
        Map<String, Object> details = new HashMap<>();
        details.put("federalDeduction", federalDeduction);
        details.put("federalDeductionPct", 50);
        details.put("municipalDeductionPct", 0);
        details.put("totalMealsExpense", totalExpense);
        
        String explanation = String.format(
            "Federal deducted $%.2f (50%% of $%.2f total meals expense). " +
            "Municipal allows 0%% deduction, so add back full $%.2f expense.",
            federalDeduction, totalExpense, totalExpense
        );
        
        return new AutoCalcResponse(totalExpense, explanation, details);
    }
    
    /**
     * Calculate 5% Rule for expenses on intangible income (FR-012)
     * Municipal adds back 5% of non-taxable intangible income to account for related expenses
     *
     * Expected inputs:
     * - interestIncome: Interest income
     * - dividends: Dividend income
     * - capitalGains: Capital gains (optional)
     *
     * @param inputs Input values
     * @return Calculated add-back amount
     */
    private AutoCalcResponse calculate5PercentRule(Map<String, Double> inputs) {
        Double interestIncome = inputs.getOrDefault("interestIncome", 0.0);
        Double dividends = inputs.getOrDefault("dividends", 0.0);
        Double capitalGains = inputs.getOrDefault("capitalGains", 0.0);
        
        Double totalIntangibleIncome = interestIncome + dividends + capitalGains;
        
        if (totalIntangibleIncome <= 0) {
            return new AutoCalcResponse(
                0.0,
                "No intangible income to calculate 5% Rule",
                Map.of()
            );
        }
        
        Double fivePercentAddBack = totalIntangibleIncome * 0.05;
        
        Map<String, Object> details = new HashMap<>();
        details.put("interestIncome", interestIncome);
        details.put("dividends", dividends);
        details.put("capitalGains", capitalGains);
        details.put("totalIntangibleIncome", totalIntangibleIncome);
        details.put("calculationPct", 5);
        
        String explanation = String.format(
            "5%% Rule: Total intangible income $%.2f (Interest: $%.2f + Dividends: $%.2f + Capital Gains: $%.2f) × 5%% = $%.2f add-back",
            totalIntangibleIncome, interestIncome, dividends, capitalGains, fivePercentAddBack
        );
        
        return new AutoCalcResponse(fivePercentAddBack, explanation, details);
    }
    
    /**
     * Calculate related-party excess (FR-006)
     * Add back payments to related parties exceeding fair market value
     *
     * Expected inputs:
     * - paidAmount: Amount paid to related party
     * - fairMarketValue: Fair market value of goods/services received
     *
     * @param inputs Input values
     * @return Calculated excess amount
     */
    private AutoCalcResponse calculateRelatedPartyExcess(Map<String, Double> inputs) {
        Double paidAmount = inputs.get("paidAmount");
        Double fairMarketValue = inputs.get("fairMarketValue");
        
        if (paidAmount == null || fairMarketValue == null) {
            return new AutoCalcResponse(
                0.0,
                "Both paid amount and fair market value are required",
                Map.of()
            );
        }
        
        if (paidAmount <= fairMarketValue) {
            return new AutoCalcResponse(
                0.0,
                String.format("No excess: Paid amount ($%.2f) does not exceed fair market value ($%.2f)", 
                    paidAmount, fairMarketValue),
                Map.of("paidAmount", paidAmount, "fairMarketValue", fairMarketValue, "excess", 0.0)
            );
        }
        
        Double excess = paidAmount - fairMarketValue;
        
        Map<String, Object> details = new HashMap<>();
        details.put("paidAmount", paidAmount);
        details.put("fairMarketValue", fairMarketValue);
        details.put("excess", excess);
        
        String explanation = String.format(
            "Related-party excess: Paid $%.2f - Fair Market Value $%.2f = $%.2f excess add-back",
            paidAmount, fairMarketValue, excess
        );
        
        return new AutoCalcResponse(excess, explanation, details);
    }
    
    /**
     * Calculate charitable contribution 10% limit (FR-016)
     * C-Corps can deduct charitable contributions up to 10% of taxable income before contributions
     * Excess carries forward up to 5 years
     *
     * Expected inputs:
     * - contributionsThisYear: Charitable contributions this year
     * - taxableIncomeBeforeContributions: Taxable income before charitable deduction
     * - priorYearCarryforward: Carryforward from prior years (optional)
     *
     * @param inputs Input values
     * @return Calculated excess and carryforward
     */
    private AutoCalcResponse calculateCharitableContribution(Map<String, Double> inputs) {
        Double contributionsThisYear = inputs.get("contributionsThisYear");
        Double taxableIncomeBeforeContributions = inputs.get("taxableIncomeBeforeContributions");
        Double priorYearCarryforward = inputs.getOrDefault("priorYearCarryforward", 0.0);
        
        if (contributionsThisYear == null || taxableIncomeBeforeContributions == null) {
            return new AutoCalcResponse(
                0.0,
                "Contributions amount and taxable income are required",
                Map.of()
            );
        }
        
        // 10% limit
        Double tenPercentLimit = taxableIncomeBeforeContributions * 0.10;
        
        // Total available to deduct (current year + prior carryforward)
        Double totalAvailable = contributionsThisYear + priorYearCarryforward;
        
        // Current year deduction (cannot exceed 10% limit)
        Double currentYearDeduction = Math.min(totalAvailable, tenPercentLimit);
        
        // New carryforward (excess over 10% limit)
        Double newCarryforward = Math.max(0.0, totalAvailable - tenPercentLimit);
        
        // If federal incorrectly deducted more than 10% limit, add back the excess
        Double municipalAddBack = 0.0;
        if (contributionsThisYear > tenPercentLimit) {
            municipalAddBack = contributionsThisYear - tenPercentLimit;
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("contributionsThisYear", contributionsThisYear);
        details.put("priorYearCarryforward", priorYearCarryforward);
        details.put("taxableIncomeBeforeContributions", taxableIncomeBeforeContributions);
        details.put("tenPercentLimit", tenPercentLimit);
        details.put("currentYearDeduction", currentYearDeduction);
        details.put("newCarryforward", newCarryforward);
        details.put("municipalAddBack", municipalAddBack);
        
        String explanation;
        if (municipalAddBack > 0) {
            explanation = String.format(
                "Contributions $%.2f exceed 10%% limit ($%.2f). Deduct $%.2f this year, " +
                "add back excess $%.2f, carry forward $%.2f to next year.",
                contributionsThisYear, tenPercentLimit, tenPercentLimit, municipalAddBack, newCarryforward
            );
        } else {
            explanation = String.format(
                "Contributions $%.2f within 10%% limit ($%.2f). Deduct $%.2f this year, " +
                "carry forward $%.2f to next year.",
                contributionsThisYear, tenPercentLimit, currentYearDeduction, newCarryforward
            );
        }
        
        return new AutoCalcResponse(municipalAddBack, explanation, details);
    }
    
    /**
     * Test officer compensation reasonableness (FR-055)
     * Informational only - not an add-back calculation
     * Warning if officer compensation >50% of net income
     *
     * Expected inputs:
     * - officerCompensation: Total officer compensation
     * - netIncome: Net income before officer compensation
     *
     * @param inputs Input values
     * @return Reasonableness assessment
     */
    public AutoCalcResponse testOfficerCompensationReasonableness(Map<String, Double> inputs) {
        Double officerCompensation = inputs.get("officerCompensation");
        Double netIncome = inputs.get("netIncome");
        
        if (officerCompensation == null || netIncome == null) {
            return new AutoCalcResponse(
                0.0,
                "Officer compensation and net income are required",
                Map.of()
            );
        }
        
        if (netIncome <= 0) {
            return new AutoCalcResponse(
                0.0,
                "Cannot assess reasonableness when net income is zero or negative",
                Map.of()
            );
        }
        
        Double compensationPct = (officerCompensation / netIncome) * 100;
        
        Map<String, Object> details = new HashMap<>();
        details.put("officerCompensation", officerCompensation);
        details.put("netIncome", netIncome);
        details.put("compensationPct", compensationPct);
        
        String explanation;
        if (compensationPct > 50) {
            explanation = String.format(
                "⚠️ Officer compensation ($%.2f) represents %.1f%% of net income ($%.2f), " +
                "which exceeds the 50%% reasonableness threshold. IRS may challenge as excessive compensation. " +
                "This is informational only - no municipal add-back required.",
                officerCompensation, compensationPct, netIncome
            );
        } else {
            explanation = String.format(
                "✓ Officer compensation ($%.2f) represents %.1f%% of net income ($%.2f), " +
                "which is within the 50%% reasonableness threshold.",
                officerCompensation, compensationPct, netIncome
            );
        }
        
        return new AutoCalcResponse(0.0, explanation, details);
    }
}
