package com.munitax.taxengine.service.penalty;

import com.munitax.taxengine.domain.penalty.EstimatedTaxPenalty;
import com.munitax.taxengine.dto.SafeHarborEvaluationDto;
import com.munitax.taxengine.repository.EstimatedTaxPenaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Service for evaluating Safe Harbor rules for estimated tax penalties.
 * 
 * Functional Requirements:
 * - FR-015 to FR-019: Safe harbor evaluation (90% current year OR 100%/110% prior year)
 * - FR-016: Safe Harbor 1 - Taxpayer paid >= 90% of current year tax liability
 * - FR-017: Safe Harbor 2 - Taxpayer paid >= 100% of prior year tax (110% if AGI > $150K)
 * - FR-018: Retrieve prior year tax liability from database
 * - FR-019: Display safe harbor status prominently in penalty calculation
 * 
 * User Story 4: Safe Harbor Rules
 * As a tax administrator, I want the system to check safe harbor rules (90% of current year 
 * tax OR 100%/110% of prior year tax) before assessing estimated tax penalties so that compliant 
 * taxpayers are not penalized.
 * 
 * Safe harbor rules determine whether estimated tax underpayment penalties apply.
 * If either safe harbor is met, no penalty is due regardless of quarterly underpayments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SafeHarborEvaluationService {
    
    private final EstimatedTaxPenaltyRepository estimatedTaxPenaltyRepository;
    
    private static final int SCALE = 2; // 2 decimal places for currency
    private static final BigDecimal SAFE_HARBOR_1_THRESHOLD = new BigDecimal("0.90"); // 90%
    private static final BigDecimal SAFE_HARBOR_2_THRESHOLD_NORMAL = new BigDecimal("1.00"); // 100%
    private static final BigDecimal SAFE_HARBOR_2_THRESHOLD_HIGH_AGI = new BigDecimal("1.10"); // 110%
    private static final BigDecimal AGI_THRESHOLD = new BigDecimal("150000.00"); // $150,000
    
    /**
     * Evaluate safe harbor rules for estimated tax penalty assessment.
     * 
     * FR-016: Safe Harbor 1 - Paid >= 90% of current year tax
     * FR-017: Safe Harbor 2 - Paid >= 100%/110% of prior year tax
     * FR-018: Retrieve prior year tax liability from database
     * 
     * @param tenantId              the tenant ID
     * @param taxYear               the current tax year
     * @param currentYearTaxLiability the current year tax liability
     * @param totalPaidEstimated    the total estimated tax paid for current year
     * @param agi                   the adjusted gross income (for 110% threshold)
     * @param priorYearTaxLiability the prior year tax liability (if null, will retrieve from DB)
     * @return safe harbor evaluation result
     */
    @Transactional(readOnly = true)
    public SafeHarborEvaluationDto evaluateSafeHarbor(
            UUID tenantId,
            int taxYear,
            BigDecimal currentYearTaxLiability,
            BigDecimal totalPaidEstimated,
            BigDecimal agi,
            BigDecimal priorYearTaxLiability) {
        
        log.info("Evaluating safe harbor for tenant: {}, tax year: {}", tenantId, taxYear);
        
        // Validate inputs
        validateInputs(tenantId, taxYear, currentYearTaxLiability, totalPaidEstimated, agi);
        
        // FR-018: Retrieve prior year tax liability if not provided
        if (priorYearTaxLiability == null) {
            priorYearTaxLiability = retrievePriorYearTaxLiability(tenantId, taxYear - 1);
            log.debug("Retrieved prior year tax liability: {}", priorYearTaxLiability);
        }
        
        // Evaluate Safe Harbor 1: Paid >= 90% of current year tax
        boolean safeHarbor1Met = evaluateSafeHarbor1(totalPaidEstimated, currentYearTaxLiability);
        BigDecimal safeHarbor1Required = currentYearTaxLiability
                .multiply(SAFE_HARBOR_1_THRESHOLD)
                .setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal currentYearPercentage = calculatePercentage(totalPaidEstimated, currentYearTaxLiability);
        
        log.debug("Safe Harbor 1: {} (Paid: ${}, Required: ${}, {}%)",
                safeHarbor1Met ? "MET" : "NOT MET",
                totalPaidEstimated,
                safeHarbor1Required,
                currentYearPercentage);
        
        // Evaluate Safe Harbor 2: Paid >= 100%/110% of prior year tax
        boolean highAgi = agi.compareTo(AGI_THRESHOLD) > 0;
        BigDecimal safeHarbor2Threshold = highAgi 
                ? SAFE_HARBOR_2_THRESHOLD_HIGH_AGI 
                : SAFE_HARBOR_2_THRESHOLD_NORMAL;
        
        boolean safeHarbor2Met = evaluateSafeHarbor2(
                totalPaidEstimated, priorYearTaxLiability, highAgi);
        BigDecimal safeHarbor2Required = priorYearTaxLiability
                .multiply(safeHarbor2Threshold)
                .setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal priorYearPercentage = calculatePercentage(totalPaidEstimated, priorYearTaxLiability);
        
        log.debug("Safe Harbor 2: {} (Paid: ${}, Required: ${} [{}% of prior year], AGI: ${} {} $150K)",
                safeHarbor2Met ? "MET" : "NOT MET",
                totalPaidEstimated,
                safeHarbor2Required,
                highAgi ? "110" : "100",
                agi,
                highAgi ? ">" : "<=");
        
        // Build response
        SafeHarborEvaluationDto evaluation = SafeHarborEvaluationDto.builder()
                .safeHarbor1Met(safeHarbor1Met)
                .currentYearPaid(totalPaidEstimated)
                .safeHarbor1Required(safeHarbor1Required)
                .currentYearPercentage(currentYearPercentage)
                .safeHarbor2Met(safeHarbor2Met)
                .priorYearPaid(totalPaidEstimated)
                .safeHarbor2Required(safeHarbor2Required)
                .priorYearPercentage(priorYearPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                .agi(agi)
                .agiThreshold(AGI_THRESHOLD)
                .build();
        
        evaluation.setAnySafeHarborMet(evaluation.calculateAnySafeHarborMet());
        evaluation.setExplanation(evaluation.generateExplanation());
        
        log.info("Safe harbor evaluation complete: {}", 
                evaluation.getAnySafeHarborMet() ? "PENALTY NOT DUE" : "PENALTY MAY APPLY");
        
        return evaluation;
    }
    
    /**
     * Evaluate Safe Harbor 1: Paid >= 90% of current year tax liability.
     * 
     * FR-016: Safe Harbor 1 rule
     * 
     * @param paid the amount paid
     * @param currentYearTax the current year tax liability
     * @return true if safe harbor 1 is met
     */
    private boolean evaluateSafeHarbor1(BigDecimal paid, BigDecimal currentYearTax) {
        if (currentYearTax.compareTo(BigDecimal.ZERO) == 0) {
            return true; // No tax due, safe harbor automatically met
        }
        
        BigDecimal required = currentYearTax.multiply(SAFE_HARBOR_1_THRESHOLD);
        return paid.compareTo(required) >= 0;
    }
    
    /**
     * Evaluate Safe Harbor 2: Paid >= 100%/110% of prior year tax liability.
     * 
     * FR-017: Safe Harbor 2 rule with AGI threshold
     * 
     * @param paid the amount paid
     * @param priorYearTax the prior year tax liability
     * @param highAgi whether AGI exceeds $150,000 threshold
     * @return true if safe harbor 2 is met
     */
    private boolean evaluateSafeHarbor2(BigDecimal paid, BigDecimal priorYearTax, boolean highAgi) {
        if (priorYearTax.compareTo(BigDecimal.ZERO) == 0) {
            return true; // No prior year tax, safe harbor automatically met
        }
        
        BigDecimal threshold = highAgi 
                ? SAFE_HARBOR_2_THRESHOLD_HIGH_AGI 
                : SAFE_HARBOR_2_THRESHOLD_NORMAL;
        BigDecimal required = priorYearTax.multiply(threshold);
        
        return paid.compareTo(required) >= 0;
    }
    
    /**
     * Retrieve prior year tax liability from database.
     * 
     * FR-018: Retrieve prior year tax liability
     * 
     * @param tenantId the tenant ID
     * @param priorYear the prior tax year
     * @return prior year tax liability (zero if not found)
     */
    private BigDecimal retrievePriorYearTaxLiability(UUID tenantId, int priorYear) {
        // Query database for prior year estimated tax penalty record
        // which contains the final tax liability
        return estimatedTaxPenaltyRepository
                .findByTenantIdAndTaxYear(tenantId, priorYear)
                .stream()
                .findFirst()
                .map(EstimatedTaxPenalty::getAnnualTaxLiability)
                .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Calculate percentage paid of required amount.
     * 
     * @param paid the amount paid
     * @param required the required amount
     * @return percentage (e.g., 95.5 for 95.5%)
     */
    private BigDecimal calculatePercentage(BigDecimal paid, BigDecimal required) {
        if (required.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100.0); // No requirement, 100% met
        }
        
        return paid.divide(required, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Validate input parameters.
     */
    private void validateInputs(UUID tenantId, int taxYear, BigDecimal currentYearTaxLiability,
                               BigDecimal totalPaidEstimated, BigDecimal agi) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (taxYear < 1900 || taxYear > 2100) {
            throw new IllegalArgumentException("Invalid tax year: " + taxYear);
        }
        if (currentYearTaxLiability == null || currentYearTaxLiability.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid current year tax liability is required");
        }
        if (totalPaidEstimated == null || totalPaidEstimated.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid total paid estimated is required");
        }
        if (agi == null || agi.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid AGI is required");
        }
    }
}
