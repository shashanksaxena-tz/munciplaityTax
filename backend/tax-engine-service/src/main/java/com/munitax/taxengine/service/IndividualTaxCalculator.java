package com.munitax.taxengine.service;

import com.munitax.taxengine.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IndividualTaxCalculator {

    public TaxCalculationResult calculateTaxes(
            List<TaxFormData> forms,
            TaxPayerProfile profile,
            TaxCalculationResult.TaxReturnSettings settings,
            TaxRulesConfig rules) {
        List<TaxCalculationResult.TaxBreakdownRule> breakdown = new ArrayList<>();

        double totalGross = 0;
        double totalLocalWithheld = 0;
        double w2TaxableIncome = 0;

        // 1. Process W-2s
        for (TaxFormData form : forms) {
            if (form instanceof W2Form w2) {
                double qualifyingWages = 0;
                double b1 = w2.federalWages() != null ? w2.federalWages() : 0;
                double b5 = w2.medicareWages() != null ? w2.medicareWages() : 0;
                double b18 = w2.localWages() != null ? w2.localWages() : 0;

                switch (rules.w2QualifyingWagesRule()) {
                    case HIGHEST_OF_ALL -> qualifyingWages = Math.max(b1, Math.max(b5, b18));
                    case BOX_5_MEDICARE -> qualifyingWages = b5;
                    case BOX_18_LOCAL -> qualifyingWages = b18;
                    case BOX_1_FEDERAL -> qualifyingWages = b1;
                }

                totalGross += b1;
                totalLocalWithheld += w2.localWithheld() != null ? w2.localWithheld() : 0;
                w2TaxableIncome += qualifyingWages;
            }
        }

        if (w2TaxableIncome > 0) {
            breakdown.add(new TaxCalculationResult.TaxBreakdownRule(
                    "Municipal",
                    "W-2 Qualifying Wages",
                    "Rule: " + rules.w2QualifyingWagesRule(),
                    String.format("$%.2f", w2TaxableIncome),
                    w2TaxableIncome));
        }

        // 2. Schedule X
        List<TaxCalculationResult.ScheduleXEntry> scheduleXEntries = new ArrayList<>();
        double totalNetProfit = 0;

        for (TaxFormData form : forms) {
            if (form instanceof ScheduleC s && rules.incomeInclusion().scheduleC()) {
                double net = s.netProfit() != null ? s.netProfit() : 0;
                scheduleXEntries.add(new TaxCalculationResult.ScheduleXEntry(
                        s.businessName(), "Schedule C", s.grossReceipts(), s.totalExpenses(), net));
                totalNetProfit += net;
            } else if (form instanceof ScheduleE s && rules.incomeInclusion().scheduleE()) {
                double net = 0;
                if (s.rentals() != null) {
                    for (ScheduleE.RentalProperty r : s.rentals()) {
                        net += (r.line21_FairRentalDays_or_Income() != null ? r.line21_FairRentalDays_or_Income() : 0) +
                                (r.line22_DeductibleLoss() != null ? r.line22_DeductibleLoss() : 0);
                    }
                }
                if (s.partnerships() != null) {
                    for (ScheduleE.PartnershipEntity p : s.partnerships()) {
                        net += p.netProfit() != null ? p.netProfit() : 0;
                    }
                }
                scheduleXEntries.add(new TaxCalculationResult.ScheduleXEntry(
                        "Rentals/Partnerships", "Schedule E", 0.0, 0.0, net));
                totalNetProfit += net;
            } else if (form instanceof ScheduleF s && rules.incomeInclusion().scheduleF()) {
                double net = s.netFarmProfit() != null ? s.netFarmProfit() : 0;
                scheduleXEntries.add(new TaxCalculationResult.ScheduleXEntry(
                        s.businessName(), "Schedule F", s.grossIncome(), s.totalExpenses(), net));
                totalNetProfit += net;
            } else if (form instanceof W2GForm s && rules.incomeInclusion().w2g()) {
                double winnings = s.grossWinnings() != null ? s.grossWinnings() : 0;
                scheduleXEntries.add(new TaxCalculationResult.ScheduleXEntry(
                        s.payer(), "Gambling", winnings, 0.0, winnings));
                totalNetProfit += winnings;
                totalLocalWithheld += s.localWithheld() != null ? s.localWithheld() : 0;
            } else if (form instanceof Form1099 s && rules.incomeInclusion().form1099()) {
                double income = s.incomeAmount() != null ? s.incomeAmount() : 0;
                scheduleXEntries.add(new TaxCalculationResult.ScheduleXEntry(
                        s.payer(), "1099", income, 0.0, income));
                totalNetProfit += income;
                totalLocalWithheld += s.localWithheld() != null ? s.localWithheld() : 0;
            }
        }

        double taxableSchX = Math.max(0, totalNetProfit);
        if (taxableSchX > 0) {
            breakdown.add(new TaxCalculationResult.TaxBreakdownRule(
                    "Sch X", "Business Income", "", String.format("$%.2f", taxableSchX), taxableSchX));
        }

        double totalTaxableIncome = w2TaxableIncome + taxableSchX;
        double municipalLiability = totalTaxableIncome * rules.municipalRate();

        // 3. Schedule Y (Credits)
        List<TaxCalculationResult.ScheduleYEntry> scheduleYEntries = new ArrayList<>();
        double totalCredit = 0;

        for (TaxFormData form : forms) {
            // Check for localWithheld and locality
            Double withheld = null;
            String locality = null;
            Double income = 0.0;
            String source = "Source";

            if (form instanceof W2Form f) {
                withheld = f.localWithheld();
                locality = f.locality();
                income = f.localWages();
                source = f.employer();
            } else if (form instanceof W2GForm f) {
                withheld = f.localWithheld();
                locality = f.locality();
                income = f.grossWinnings();
                source = f.payer();
            } else if (form instanceof Form1099 f) {
                withheld = f.localWithheld();
                locality = f.locality();
                income = f.incomeAmount();
                source = f.payer();
            }

            if (withheld != null && withheld > 0 && locality != null && !locality.toLowerCase().contains("dublin")) {
                double limit = (income != null ? income : 0) * rules.municipalCreditLimitRate();
                double allowed = Math.min(withheld, limit);

                scheduleYEntries.add(new TaxCalculationResult.ScheduleYEntry(
                        source, locality, 0.0, income, withheld, allowed));
                totalCredit += allowed;
            }
        }

        double liabilityFinal = Math.max(0, municipalLiability - totalCredit);
        double balance = totalLocalWithheld - liabilityFinal;

        if (rules.enableRounding()) {
            liabilityFinal = Math.round(liabilityFinal);
            balance = Math.round(balance);
        }

        return new TaxCalculationResult(
                settings,
                profile,
                totalGross,
                totalLocalWithheld,
                w2TaxableIncome,
                new TaxCalculationResult.ScheduleXResult(scheduleXEntries, totalNetProfit),
                new TaxCalculationResult.ScheduleYResult(scheduleYEntries, totalCredit),
                totalTaxableIncome,
                municipalLiability,
                liabilityFinal,
                balance,
                breakdown,
                analyzeDiscrepancies(forms, totalTaxableIncome));
    }

    private TaxCalculationResult.DiscrepancyReport analyzeDiscrepancies(List<TaxFormData> forms,
            double calculatedIncome) {
        List<TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue> issues = new ArrayList<>();

        // Find Federal Form
        FederalTaxForm federalForm = null;
        for (TaxFormData form : forms) {
            if (form instanceof FederalTaxForm f) {
                federalForm = f;
                break;
            }
        }

        // Calculate total W-2 wages from forms for comparison
        double totalW2Wages = 0;
        for (TaxFormData form : forms) {
            if (form instanceof W2Form w2) {
                totalW2Wages += w2.federalWages() != null ? w2.federalWages() : 0;
            }
        }

        // Compare Local vs Calculated
        for (TaxFormData form : forms) {
            if (form instanceof LocalTaxForm localForm) {
                double reported = localForm.reportedTaxableIncome() != null ? localForm.reportedTaxableIncome() : 0;
                if (Math.abs(reported - calculatedIncome) > 5) {
                    issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                            "Taxable Income",
                            calculatedIncome,
                            reported,
                            calculatedIncome - reported,
                            Math.abs(calculatedIncome - reported) > 100 ? "HIGH" : "MEDIUM",
                            "Taxable income on local form matches source documents."));
                }
            }
        }

        // Compare Federal vs W-2s (if Federal exists)
        if (federalForm != null) {
            double fedWages = federalForm.wages() != null ? federalForm.wages() : 0;

            if (Math.abs(fedWages - totalW2Wages) > 5) {
                issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                        "Federal Wages vs W-2s",
                        totalW2Wages,
                        fedWages,
                        totalW2Wages - fedWages,
                        "MEDIUM",
                        "Wages reported on Federal 1040 do not match sum of W-2s provided."));
            }
        }

        return new TaxCalculationResult.DiscrepancyReport(!issues.isEmpty(), issues);
    }
}
