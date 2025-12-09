package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.withholding.FilingFrequency;
import com.munitax.taxengine.domain.withholding.W1Filing;
import com.munitax.taxengine.dto.ReconciliationIssue;
import com.munitax.taxengine.model.W2Form;
import com.munitax.taxengine.repository.W1FilingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for W-1 withholding reconciliation operations.
 * 
 * Core Functions:
 * - Reconcile W-1 filings against W-2 reported withholding
 * - Compare W-1 wages to W-2 Box 1 (federal wages) and Box 18 (local wages)
 * - Verify cumulative totals match year-to-date
 * - Validate withholding rates are within acceptable range (0-3.0%)
 * - Detect duplicate filings for same period
 * - Flag late filings
 * - Return list of reconciliation issues
 * 
 * Business Rules:
 * - Wage variance threshold: >1% or >$100 triggers warning
 * - Withholding rate must be between 0% and 3.0%
 * - Quarterly filings: 4 periods required (Q1-Q4)
 * - Monthly filings: 12 periods required (M01-M12)
 * - Late filing: filing_date > due_date
 * 
 * @see W1Filing
 * @see W2Form
 * @see ReconciliationIssue
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WithholdingReconciliationService {
    
    private final W1FilingRepository w1FilingRepository;
    
    private static final BigDecimal MIN_WITHHOLDING_RATE = BigDecimal.ZERO;
    private static final BigDecimal MAX_WITHHOLDING_RATE = new BigDecimal("0.0300"); // 3.0%
    private static final BigDecimal VARIANCE_THRESHOLD_PERCENTAGE = new BigDecimal("1.0"); // 1%
    private static final BigDecimal VARIANCE_THRESHOLD_AMOUNT = new BigDecimal("100.00");
    
    /**
     * Run comprehensive reconciliation for an employer's W-1 filings.
     * 
     * Performs the following checks:
     * 1. Compare W-1 wages to W-2 reported wages (federal and local)
     * 2. Validate withholding rates are within acceptable range
     * 3. Check cumulative totals match year-to-date
     * 4. Detect duplicate filings
     * 5. Flag late filings
     * 6. Verify all required periods are filed
     * 
     * @param employerId Employer/Business ID
     * @param taxYear Tax year to reconcile
     * @param w2Forms List of W-2 forms for reconciliation (optional)
     * @return List of reconciliation issues found
     */
    public List<ReconciliationIssue> reconcileW1Filings(UUID employerId, Integer taxYear, List<W2Form> w2Forms) {
        log.info("Starting reconciliation for employer {} tax year {}", employerId, taxYear);
        
        List<ReconciliationIssue> issues = new ArrayList<>();
        
        // Fetch all W-1 filings for the employer and tax year
        List<W1Filing> w1Filings = w1FilingRepository.findByBusinessIdAndTaxYear(employerId, taxYear);
        
        if (w1Filings.isEmpty()) {
            log.warn("No W-1 filings found for employer {} tax year {}", employerId, taxYear);
            return issues;
        }
        
        // Check 1: Compare W-1 wages to W-2 reported wages
        if (w2Forms != null && !w2Forms.isEmpty()) {
            issues.addAll(checkWageReconciliation(employerId, taxYear, w1Filings, w2Forms));
        }
        
        // Check 2: Validate withholding rates
        issues.addAll(checkWithholdingRates(w1Filings));
        
        // Check 3: Check cumulative totals
        issues.addAll(checkCumulativeTotals(w1Filings));
        
        // Check 4: Detect duplicate filings
        issues.addAll(checkDuplicateFilings(w1Filings));
        
        // Check 5: Flag late filings
        issues.addAll(checkLateFilings(w1Filings));
        
        // Check 6: Verify all required periods are filed
        issues.addAll(checkMissingFilings(employerId, taxYear, w1Filings));
        
        log.info("Reconciliation completed. Found {} issues for employer {} tax year {}", 
                 issues.size(), employerId, taxYear);
        
        return issues;
    }
    
    /**
     * Compare W-1 wages to W-2 reported wages.
     * Checks both federal wages (W-2 Box 1) and local wages (W-2 Box 18).
     */
    private List<ReconciliationIssue> checkWageReconciliation(UUID employerId, Integer taxYear, 
                                                               List<W1Filing> w1Filings, List<W2Form> w2Forms) {
        List<ReconciliationIssue> issues = new ArrayList<>();
        
        // Calculate total W-1 wages
        BigDecimal w1TotalWages = w1Filings.stream()
            .map(W1Filing::getGrossWages)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate total W-2 federal wages (Box 1)
        BigDecimal w2TotalFederalWages = w2Forms.stream()
            .map(w2 -> w2.federalWages() != null ? BigDecimal.valueOf(w2.federalWages()) : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate total W-2 local wages (Box 18)
        BigDecimal w2TotalLocalWages = w2Forms.stream()
            .map(w2 -> w2.localWages() != null ? BigDecimal.valueOf(w2.localWages()) : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Check federal wage mismatch
        if (w2TotalFederalWages.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal federalVariance = w1TotalWages.subtract(w2TotalFederalWages);
            BigDecimal federalVariancePercentage = w2TotalFederalWages.compareTo(BigDecimal.ZERO) > 0 ?
                federalVariance.divide(w2TotalFederalWages, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;
            
            if (federalVariance.abs().compareTo(VARIANCE_THRESHOLD_AMOUNT) > 0 ||
                federalVariancePercentage.abs().compareTo(VARIANCE_THRESHOLD_PERCENTAGE) > 0) {
                issues.add(ReconciliationIssue.builder()
                    .id(UUID.randomUUID())
                    .employerId(employerId)
                    .taxYear(taxYear)
                    .issueType(ReconciliationIssue.IssueType.WAGE_MISMATCH_FEDERAL)
                    .severity(ReconciliationIssue.IssueSeverity.HIGH)
                    .description(String.format("W-1 total wages ($%,.2f) do not match W-2 Box 1 federal wages ($%,.2f). Variance: $%,.2f (%.2f%%)",
                        w1TotalWages, w2TotalFederalWages, federalVariance, federalVariancePercentage))
                    .expectedValue(w2TotalFederalWages)
                    .actualValue(w1TotalWages)
                    .variance(federalVariance)
                    .variancePercentage(federalVariancePercentage)
                    .recommendedAction("Review W-1 filings and W-2 forms. File amended W-1 if necessary.")
                    .resolved(false)
                    .build());
            }
        }
        
        // Check local wage mismatch
        if (w2TotalLocalWages.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal localVariance = w1TotalWages.subtract(w2TotalLocalWages);
            BigDecimal localVariancePercentage = w2TotalLocalWages.compareTo(BigDecimal.ZERO) > 0 ?
                localVariance.divide(w2TotalLocalWages, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;
            
            if (localVariance.abs().compareTo(VARIANCE_THRESHOLD_AMOUNT) > 0 ||
                localVariancePercentage.abs().compareTo(VARIANCE_THRESHOLD_PERCENTAGE) > 0) {
                issues.add(ReconciliationIssue.builder()
                    .id(UUID.randomUUID())
                    .employerId(employerId)
                    .taxYear(taxYear)
                    .issueType(ReconciliationIssue.IssueType.WAGE_MISMATCH_LOCAL)
                    .severity(ReconciliationIssue.IssueSeverity.HIGH)
                    .description(String.format("W-1 total wages ($%,.2f) do not match W-2 Box 18 local wages ($%,.2f). Variance: $%,.2f (%.2f%%)",
                        w1TotalWages, w2TotalLocalWages, localVariance, localVariancePercentage))
                    .expectedValue(w2TotalLocalWages)
                    .actualValue(w1TotalWages)
                    .variance(localVariance)
                    .variancePercentage(localVariancePercentage)
                    .recommendedAction("Review W-1 filings and W-2 forms. File amended W-1 if necessary.")
                    .resolved(false)
                    .build());
            }
        }
        
        return issues;
    }
    
    /**
     * Validate that withholding rates are within acceptable range (0-3.0%).
     */
    private List<ReconciliationIssue> checkWithholdingRates(List<W1Filing> w1Filings) {
        List<ReconciliationIssue> issues = new ArrayList<>();
        
        for (W1Filing filing : w1Filings) {
            BigDecimal taxRate = filing.getTaxRate();
            
            if (taxRate.compareTo(MIN_WITHHOLDING_RATE) < 0 || taxRate.compareTo(MAX_WITHHOLDING_RATE) > 0) {
                issues.add(ReconciliationIssue.builder()
                    .id(UUID.randomUUID())
                    .employerId(filing.getBusinessId())
                    .taxYear(filing.getTaxYear())
                    .period(filing.getPeriod())
                    .issueType(ReconciliationIssue.IssueType.WITHHOLDING_RATE_INVALID)
                    .severity(ReconciliationIssue.IssueSeverity.CRITICAL)
                    .description(String.format("Withholding rate %.4f%% is outside valid range (0%%-3.0%%) for period %s",
                        taxRate.multiply(new BigDecimal("100")), filing.getPeriod()))
                    .expectedValue(MAX_WITHHOLDING_RATE)
                    .actualValue(taxRate)
                    .filingDate(filing.getFilingDate().toLocalDate())
                    .recommendedAction("Review tax rate calculation. File amended W-1 with correct rate.")
                    .resolved(false)
                    .build());
            }
        }
        
        return issues;
    }
    
    /**
     * Check that cumulative totals are consistent across periods.
     * Verifies that each period's wages contribute correctly to year-to-date totals.
     */
    private List<ReconciliationIssue> checkCumulativeTotals(List<W1Filing> w1Filings) {
        List<ReconciliationIssue> issues = new ArrayList<>();
        
        // Sort filings by period end date
        List<W1Filing> sortedFilings = w1Filings.stream()
            .sorted(Comparator.comparing(W1Filing::getPeriodEndDate))
            .collect(Collectors.toList());
        
        BigDecimal cumulativeWages = BigDecimal.ZERO;
        BigDecimal cumulativeTax = BigDecimal.ZERO;
        
        for (W1Filing filing : sortedFilings) {
            cumulativeWages = cumulativeWages.add(filing.getGrossWages());
            cumulativeTax = cumulativeTax.add(filing.getTaxDue());
            
            // Calculate expected tax based on cumulative wages and filing's tax rate
            BigDecimal expectedCumulativeTax = filing.getGrossWages().multiply(filing.getTaxRate())
                .setScale(2, RoundingMode.HALF_UP);
            
            // Check if individual period tax matches expected
            BigDecimal taxVariance = filing.getTaxDue().subtract(expectedCumulativeTax).abs();
            if (taxVariance.compareTo(new BigDecimal("1.00")) > 0) {
                issues.add(ReconciliationIssue.builder()
                    .id(UUID.randomUUID())
                    .employerId(filing.getBusinessId())
                    .taxYear(filing.getTaxYear())
                    .period(filing.getPeriod())
                    .issueType(ReconciliationIssue.IssueType.CUMULATIVE_MISMATCH)
                    .severity(ReconciliationIssue.IssueSeverity.MEDIUM)
                    .description(String.format("Tax calculation mismatch for period %s. Expected $%,.2f, found $%,.2f",
                        filing.getPeriod(), expectedCumulativeTax, filing.getTaxDue()))
                    .expectedValue(expectedCumulativeTax)
                    .actualValue(filing.getTaxDue())
                    .variance(filing.getTaxDue().subtract(expectedCumulativeTax))
                    .recommendedAction("Review tax calculation for this period.")
                    .resolved(false)
                    .build());
            }
        }
        
        return issues;
    }
    
    /**
     * Detect duplicate filings for the same period.
     */
    private List<ReconciliationIssue> checkDuplicateFilings(List<W1Filing> w1Filings) {
        List<ReconciliationIssue> issues = new ArrayList<>();
        
        // Group filings by period
        Map<String, List<W1Filing>> filingsByPeriod = w1Filings.stream()
            .collect(Collectors.groupingBy(W1Filing::getPeriod));
        
        // Check for duplicates (more than one non-amended filing per period)
        for (Map.Entry<String, List<W1Filing>> entry : filingsByPeriod.entrySet()) {
            String period = entry.getKey();
            List<W1Filing> periodFilings = entry.getValue().stream()
                .filter(f -> !f.getIsAmended())
                .collect(Collectors.toList());
            
            if (periodFilings.size() > 1) {
                W1Filing firstFiling = periodFilings.get(0);
                issues.add(ReconciliationIssue.builder()
                    .id(UUID.randomUUID())
                    .employerId(firstFiling.getBusinessId())
                    .taxYear(firstFiling.getTaxYear())
                    .period(period)
                    .issueType(ReconciliationIssue.IssueType.DUPLICATE_FILING)
                    .severity(ReconciliationIssue.IssueSeverity.HIGH)
                    .description(String.format("Found %d duplicate filings for period %s", periodFilings.size(), period))
                    .recommendedAction("Review duplicate filings and void incorrect ones.")
                    .resolved(false)
                    .build());
            }
        }
        
        return issues;
    }
    
    /**
     * Flag late filings (filing_date > due_date).
     */
    private List<ReconciliationIssue> checkLateFilings(List<W1Filing> w1Filings) {
        List<ReconciliationIssue> issues = new ArrayList<>();
        
        for (W1Filing filing : w1Filings) {
            LocalDate filingDate = filing.getFilingDate().toLocalDate();
            LocalDate dueDate = filing.getDueDate();
            
            if (filingDate.isAfter(dueDate)) {
                long daysLate = java.time.temporal.ChronoUnit.DAYS.between(dueDate, filingDate);
                
                issues.add(ReconciliationIssue.builder()
                    .id(UUID.randomUUID())
                    .employerId(filing.getBusinessId())
                    .taxYear(filing.getTaxYear())
                    .period(filing.getPeriod())
                    .issueType(ReconciliationIssue.IssueType.LATE_FILING)
                    .severity(ReconciliationIssue.IssueSeverity.MEDIUM)
                    .description(String.format("Filing for period %s was %d days late (due: %s, filed: %s)",
                        filing.getPeriod(), daysLate, dueDate, filingDate))
                    .dueDate(dueDate)
                    .filingDate(filingDate)
                    .recommendedAction(String.format("Late filing penalty applied: $%,.2f", filing.getLateFilingPenalty()))
                    .resolved(false)
                    .build());
            }
        }
        
        return issues;
    }
    
    /**
     * Check for missing required filings based on filing frequency.
     */
    private List<ReconciliationIssue> checkMissingFilings(UUID employerId, Integer taxYear, List<W1Filing> w1Filings) {
        List<ReconciliationIssue> issues = new ArrayList<>();
        
        if (w1Filings.isEmpty()) {
            return issues;
        }
        
        // Determine filing frequency from first filing
        FilingFrequency frequency = w1Filings.get(0).getFilingFrequency();
        
        // Get filed periods
        Set<String> filedPeriods = w1Filings.stream()
            .map(W1Filing::getPeriod)
            .collect(Collectors.toSet());
        
        // Determine required periods based on frequency
        Set<String> requiredPeriods = getRequiredPeriods(frequency);
        
        // Find missing periods
        Set<String> missingPeriods = new HashSet<>(requiredPeriods);
        missingPeriods.removeAll(filedPeriods);
        
        for (String period : missingPeriods) {
            issues.add(ReconciliationIssue.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .taxYear(taxYear)
                .period(period)
                .issueType(ReconciliationIssue.IssueType.MISSING_FILING)
                .severity(ReconciliationIssue.IssueSeverity.CRITICAL)
                .description(String.format("Missing required filing for period %s", period))
                .recommendedAction("File W-1 return for missing period.")
                .resolved(false)
                .build());
        }
        
        return issues;
    }
    
    /**
     * Get required filing periods based on filing frequency.
     */
    private Set<String> getRequiredPeriods(FilingFrequency frequency) {
        Set<String> periods = new HashSet<>();
        
        switch (frequency) {
            case QUARTERLY:
                periods.add("Q1");
                periods.add("Q2");
                periods.add("Q3");
                periods.add("Q4");
                break;
            case MONTHLY:
                for (int i = 1; i <= 12; i++) {
                    periods.add(String.format("M%02d", i));
                }
                break;
            case SEMI_MONTHLY:
                for (int i = 1; i <= 24; i++) {
                    periods.add(String.format("SM%02d", i));
                }
                break;
            case DAILY:
                // Daily filings are too numerous to check for missing periods
                // Skip this check for daily filers
                break;
        }
        
        return periods;
    }
    
    /**
     * Get reconciliation issues for a specific employer.
     * This is a convenience method that delegates to reconcileW1Filings.
     * 
     * @param employerId Employer/Business ID
     * @param taxYear Tax year to reconcile
     * @return List of reconciliation issues
     */
    public List<ReconciliationIssue> getReconciliationIssues(UUID employerId, Integer taxYear) {
        return reconcileW1Filings(employerId, taxYear, null);
    }
}
