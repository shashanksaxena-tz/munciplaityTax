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
        int issueCounter = 1;

        // Find Federal Form
        FederalTaxForm federalForm = null;
        LocalTaxForm localForm = null;
        for (TaxFormData form : forms) {
            if (form instanceof FederalTaxForm f) {
                federalForm = f;
            }
            if (form instanceof LocalTaxForm l) {
                localForm = l;
            }
        }

        // Calculate total W-2 wages from forms for comparison
        double totalW2Wages = 0;
        double totalW2LocalWages = 0;
        List<W2Form> w2Forms = new ArrayList<>();
        for (TaxFormData form : forms) {
            if (form instanceof W2Form w2) {
                w2Forms.add(w2);
                totalW2Wages += w2.federalWages() != null ? w2.federalWages() : 0;
                totalW2LocalWages += w2.localWages() != null ? w2.localWages() : 0;
            }
        }

        // FR-001 to FR-005: W-2 Validation Rules
        issues.addAll(validateW2Forms(w2Forms, issueCounter));
        issueCounter += issues.size();

        // FR-006 to FR-010: Schedule C/E/F Validation
        issues.addAll(validateScheduleForms(forms, issueCounter));
        issueCounter += issues.size();

        // FR-014 to FR-016: Municipal Credit Validation (K-1 validation FR-011-013 requires more complex parsing)
        issues.addAll(validateMunicipalCredits(forms, calculatedIncome * 0.02, issueCounter));
        issueCounter += issues.size();

        // FR-017 to FR-019: Federal Form Reconciliation
        if (federalForm != null) {
            issues.addAll(validateFederalReconciliation(federalForm, totalW2Wages, totalW2LocalWages, calculatedIncome, issueCounter));
            issueCounter += issues.size();
        }

        // Compare Local vs Calculated
        if (localForm != null) {
            double reported = localForm.reportedTaxableIncome() != null ? localForm.reportedTaxableIncome() : 0;
            if (Math.abs(reported - calculatedIncome) > 5) {
                double diff = calculatedIncome - reported;
                double diffPercent = reported != 0 ? (diff / reported) * 100 : 0;
                issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                        "DISC-" + issueCounter++,
                        "FR-BASE-001",
                        "Income Reconciliation",
                        "Taxable Income",
                        calculatedIncome,
                        reported,
                        diff,
                        diffPercent,
                        Math.abs(diff) > 100 ? "HIGH" : "MEDIUM",
                        "Taxable income on local form does not match calculated value from source documents.",
                        "Review all income sources and ensure they are properly reported on the local return.",
                        false,
                        null,
                        null));
            }
        }

        // Build summary
        int highCount = 0, mediumCount = 0, lowCount = 0;
        for (TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue issue : issues) {
            switch (issue.severity()) {
                case "HIGH" -> highCount++;
                case "MEDIUM" -> mediumCount++;
                case "LOW" -> lowCount++;
            }
        }

        TaxCalculationResult.DiscrepancyReport.DiscrepancySummary summary = 
            new TaxCalculationResult.DiscrepancyReport.DiscrepancySummary(
                issues.size(), highCount, mediumCount, lowCount, highCount > 0);

        return new TaxCalculationResult.DiscrepancyReport(!issues.isEmpty(), issues, summary);
    }

    // FR-001 to FR-005: W-2 Validation
    private List<TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue> validateW2Forms(
            List<W2Form> w2Forms, int startCounter) {
        List<TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue> issues = new ArrayList<>();
        int counter = startCounter;

        for (int i = 0; i < w2Forms.size(); i++) {
            W2Form w2 = w2Forms.get(i);
            double box1 = w2.federalWages() != null ? w2.federalWages() : 0;
            double box18 = w2.localWages() != null ? w2.localWages() : 0;
            double box19 = w2.localWithheld() != null ? w2.localWithheld() : 0;

            // FR-001: Box 18 within 20% of Box 1
            if (box1 > 0 && box18 > 0) {
                double variance = Math.abs(box1 - box18);
                double variancePercent = (variance / box1) * 100;
                if (variancePercent > 20) {
                    issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                            "DISC-" + counter++,
                            "FR-001",
                            "W-2 Validation",
                            "W-2 Box 18 vs Box 1 (" + w2.employer() + ")",
                            box1,
                            box18,
                            box1 - box18,
                            variancePercent,
                            "MEDIUM",
                            String.format("W-2 Box 18 (Local wages: $%.2f) differs from Box 1 (Federal wages: $%.2f) by %.1f%%. For full-year Dublin employment, these should be similar.",
                                    box18, box1, variancePercent),
                            "Verify Box 18 was entered correctly. For partial-year employment or Section 125 plans, this variance may be normal.",
                            false,
                            null,
                            null));
                }
            }

            // FR-002: Withholding rate between 0% and 3.0%
            if (box18 > 0) {
                double withholdingRate = (box19 / box18) * 100;
                if (withholdingRate > 3.0) {
                    issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                            "DISC-" + counter++,
                            "FR-002",
                            "W-2 Validation",
                            "W-2 Withholding Rate (" + w2.employer() + ")",
                            box18,
                            box19,
                            box19,
                            withholdingRate,
                            "MEDIUM",
                            String.format("Withholding rate of %.2f%% exceeds maximum Dublin rate of 2.5%%. Employer may have over-withheld.",
                                    withholdingRate),
                            "Contact employer to verify correct withholding rate or check Box 19 entry.",
                            false,
                            null,
                            null));
                } else if (withholdingRate == 0 && box18 > 25000) {
                    issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                            "DISC-" + counter++,
                            "FR-002",
                            "W-2 Validation",
                            "W-2 No Withholding (" + w2.employer() + ")",
                            box18,
                            box19,
                            0.0,
                            0.0,
                            "LOW",
                            "No local tax withheld on wages of $" + String.format("%.2f", box18) + ".",
                            "Verify employer withholds Dublin tax or if you need to make estimated payments.",
                            false,
                            null,
                            null));
                }
            }

            // FR-003: Duplicate W-2 detection
            for (int j = i + 1; j < w2Forms.size(); j++) {
                W2Form other = w2Forms.get(j);
                if (w2.employerEin() != null && w2.employerEin().equals(other.employerEin()) &&
                    Math.abs((w2.federalWages() != null ? w2.federalWages() : 0) - 
                             (other.federalWages() != null ? other.federalWages() : 0)) < 1) {
                    issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                            "DISC-" + counter++,
                            "FR-003",
                            "W-2 Validation",
                            "Duplicate W-2 (" + w2.employer() + ")",
                            box1,
                            box1,
                            0.0,
                            0.0,
                            "HIGH",
                            "Potential duplicate W-2 detected: same employer EIN and wage amount.",
                            "Remove duplicate W-2 or mark as 'Corrected' if replacing original.",
                            false,
                            null,
                            null));
                }
            }

            // FR-004: Employer address validation (simplified - just check if locality is Dublin)
            if (w2.locality() != null && !w2.locality().toLowerCase().contains("dublin")) {
                issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                        "DISC-" + counter++,
                        "FR-004",
                        "W-2 Validation",
                        "W-2 Out-of-Jurisdiction (" + w2.employer() + ")",
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        "LOW",
                        "Employer locality '" + w2.locality() + "' is outside Dublin municipal limits.",
                        "Verify local withholding applies and consider claiming credit on Schedule Y.",
                        false,
                        null,
                        null));
            }
        }

        return issues;
    }

    // FR-006 to FR-010: Schedule C/E/F Validation
    private List<TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue> validateScheduleForms(
            List<TaxFormData> forms, int startCounter) {
        List<TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue> issues = new ArrayList<>();
        int counter = startCounter;

        double totalScheduleIncome = 0;
        int rentalPropertyCount = 0;
        int rentalPropertiesWithData = 0;
        double totalRentalLoss = 0;
        
        // Get AGI from federal form if available
        Double agi = null;
        for (TaxFormData form : forms) {
            if (form instanceof FederalTaxForm federal) {
                agi = federal.adjustedGrossIncome();
                break;
            }
        }

        for (TaxFormData form : forms) {
            // FR-006: Schedule C estimated tax validation
            if (form instanceof ScheduleC schedC) {
                double netProfit = schedC.netProfit() != null ? schedC.netProfit() : 0;
                totalScheduleIncome += netProfit;
                
                if (netProfit > 50000) {
                    // Simplified - assume 90% safe harbor rule
                    double requiredEstimated = netProfit * 0.02 * 0.90;
                    issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                            "DISC-" + counter++,
                            "FR-006",
                            "Schedule C Validation",
                            "Schedule C Estimated Payments",
                            requiredEstimated,
                            0.0,
                            requiredEstimated,
                            100.0,
                            "MEDIUM",
                            String.format("Schedule C net profit of $%.2f may require estimated tax payments of approximately $%.2f (90%% safe harbor).",
                                    netProfit, requiredEstimated),
                            "Verify estimated tax payments were made. Underpayment penalty may apply.",
                            false,
                            null,
                            null));
                }
            }

            // FR-007, FR-008, FR-009: Schedule E validation
            if (form instanceof ScheduleE schedE) {
                if (schedE.rentals() != null) {
                    rentalPropertyCount = schedE.rentals().size();
                    for (ScheduleE.RentalProperty rental : schedE.rentals()) {
                        if (rental.streetAddress() != null && !rental.streetAddress().isEmpty()) {
                            rentalPropertiesWithData++;
                            
                            // FR-008: Check if property is outside Dublin
                            if (rental.city() != null && !rental.city().toLowerCase().contains("dublin")) {
                                issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                                        "DISC-" + counter++,
                                        "FR-008",
                                        "Schedule E Validation",
                                        "Rental Property Location",
                                        0.0,
                                        0.0,
                                        0.0,
                                        0.0,
                                        "LOW",
                                        "Rental property at " + rental.streetAddress() + " is outside Dublin municipal limits.",
                                        "Verify this rental income is subject to Dublin tax.",
                                        false,
                                        null,
                                        null));
                            }
                        }
                        
                        // Calculate rental loss for FR-009
                        double rentalIncome = rental.line21_FairRentalDays_or_Income() != null ? 
                                rental.line21_FairRentalDays_or_Income() : 0;
                        double rentalDeduction = rental.line22_DeductibleLoss() != null ? 
                                rental.line22_DeductibleLoss() : 0;
                        double netRentalIncome = rentalIncome + rentalDeduction; // deduction is negative
                        if (netRentalIncome < 0) {
                            totalRentalLoss += Math.abs(netRentalIncome);
                        }
                    }
                    
                    // FR-007: Property count validation
                    if (rentalPropertyCount > rentalPropertiesWithData) {
                        issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                                "DISC-" + counter++,
                                "FR-007",
                                "Schedule E Validation",
                                "Rental Property Count",
                                (double) rentalPropertyCount,
                                (double) rentalPropertiesWithData,
                                (double) (rentalPropertyCount - rentalPropertiesWithData),
                                0.0,
                                "MEDIUM",
                                String.format("%d rental properties reported but only %d have complete address data.",
                                        rentalPropertyCount, rentalPropertiesWithData),
                                "Complete all property details including address, income, and expenses.",
                                false,
                                null,
                                null));
                    }
                }
            }
        }
        
        // FR-009: Passive loss limitation check
        if (totalRentalLoss > 0 && agi != null && agi > 150000) {
            issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                    "DISC-" + counter++,
                    "FR-009",
                    "Schedule E Validation",
                    "Passive Loss Limitation",
                    agi,
                    totalRentalLoss,
                    0.0,
                    0.0,
                    "LOW",
                    String.format("AGI of $%.2f exceeds $150,000 threshold. Rental loss of $%.2f may be limited by passive activity rules.",
                            agi, totalRentalLoss),
                    "Verify federal Form 8582 was prepared and passive loss limits were applied correctly.",
                    false,
                    null,
                    null));
        }

        return issues;
    }

    // FR-014 to FR-016: Municipal Credit Validation
    private List<TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue> validateMunicipalCredits(
            List<TaxFormData> forms, double dublinLiability, int startCounter) {
        List<TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue> issues = new ArrayList<>();
        int counter = startCounter;

        double totalCredits = 0;

        for (TaxFormData form : forms) {
            Double withheld = null;
            String locality = null;

            if (form instanceof W2Form w2) {
                withheld = w2.localWithheld();
                locality = w2.locality();
            } else if (form instanceof W2GForm w2g) {
                withheld = w2g.localWithheld();
                locality = w2g.locality();
            } else if (form instanceof Form1099 f1099) {
                withheld = f1099.localWithheld();
                locality = f1099.locality();
            }

            if (withheld != null && withheld > 0 && locality != null && !locality.toLowerCase().contains("dublin")) {
                totalCredits += withheld;
            }
        }

        // FR-014: Credits cannot exceed liability
        if (totalCredits > dublinLiability && dublinLiability > 0) {
            issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                    "DISC-" + counter++,
                    "FR-014",
                    "Municipal Credit Validation",
                    "Municipal Credit Limit",
                    dublinLiability,
                    totalCredits,
                    totalCredits - dublinLiability,
                    ((totalCredits - dublinLiability) / dublinLiability) * 100,
                    "HIGH",
                    String.format("Municipal credits of $%.2f exceed Dublin tax liability of $%.2f. Credits capped at liability.",
                            totalCredits, dublinLiability),
                    "Credit excess of $" + String.format("%.2f", totalCredits - dublinLiability) + " cannot be applied this year.",
                    false,
                    null,
                    null));
        }

        return issues;
    }

    // FR-017 to FR-019: Federal Form Reconciliation
    private List<TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue> validateFederalReconciliation(
            FederalTaxForm federalForm, double totalW2Wages, double totalW2LocalWages, 
            double localCalculatedIncome, int startCounter) {
        List<TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue> issues = new ArrayList<>();
        int counter = startCounter;

        double fedWages = federalForm.wages() != null ? federalForm.wages() : 0;
        double fedAGI = federalForm.adjustedGrossIncome() != null ? federalForm.adjustedGrossIncome() : 0;

        // FR-019: Federal wages vs W-2s
        if (Math.abs(fedWages - totalW2Wages) > 100) {
            double diff = totalW2Wages - fedWages;
            double diffPercent = fedWages != 0 ? (diff / fedWages) * 100 : 0;
            issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                    "DISC-" + counter++,
                    "FR-019",
                    "Federal Reconciliation",
                    "Federal Wages vs W-2s",
                    totalW2Wages,
                    fedWages,
                    diff,
                    diffPercent,
                    "MEDIUM",
                    String.format("Federal Form 1040 Line 1 (Wages: $%.2f) does not match sum of W-2 Box 1 amounts ($%.2f).",
                            fedWages, totalW2Wages),
                    "Verify all W-2s are included and Federal Form 1040 is accurate.",
                    false,
                    null,
                    null));
        }

        // FR-017: Federal AGI vs Local calculation
        if (fedAGI > 0 && localCalculatedIncome > 0) {
            double diff = fedAGI - localCalculatedIncome;
            if (Math.abs(diff) > 500) {
                double diffPercent = localCalculatedIncome != 0 ? (diff / localCalculatedIncome) * 100 : 0;
                issues.add(new TaxCalculationResult.DiscrepancyReport.DiscrepancyIssue(
                        "DISC-" + counter++,
                        "FR-017",
                        "Federal Reconciliation",
                        "Federal AGI vs Local Income",
                        localCalculatedIncome,
                        fedAGI,
                        diff,
                        diffPercent,
                        "MEDIUM",
                        String.format("Federal AGI ($%.2f) differs from local calculated income ($%.2f) by $%.2f.",
                                fedAGI, localCalculatedIncome, Math.abs(diff)),
                        "Common causes: Interest, dividends, unemployment, or other non-taxable local income. This may be normal.",
                        false,
                        null,
                        null));
            }
        }

        return issues;
    }
}
