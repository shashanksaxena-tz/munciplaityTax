package com.munitax.taxengine.service.penalty;

import com.munitax.taxengine.domain.penalty.Penalty;
import com.munitax.taxengine.domain.penalty.PenaltyType;
import com.munitax.taxengine.dto.PenaltyCalculationRequest;
import com.munitax.taxengine.dto.PenaltyCalculationResponse;
import com.munitax.taxengine.repository.PenaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for applying combined penalty cap when both late filing and late payment penalties apply.
 * 
 * Functional Requirements:
 * - FR-012 to FR-014: Combined penalty cap (max 5% per month when both apply)
 * - FR-012: When both penalties apply, combined cap is 5% per month
 * - FR-013: Months 1-5: Combined 5% per month (late filing absorbs both)
 * - FR-014: After month 5: Late filing maxed at 25%, late payment continues at 1%/month
 * 
 * User Story 3: Combined Penalty Cap
 * As a tax administrator, I want the system to apply a combined 5% monthly cap
 * when both late filing and late payment penalties apply, so that taxpayers are not
 * penalized more than 5% per month during the first 5 months.
 * 
 * Algorithm:
 * - Months 1-5: Late filing penalty (5%) absorbs both penalties (no additional late payment)
 * - After month 5: Late filing capped at 25%, late payment continues at 1%/month
 * - Maximum combined penalty: 25% + (months after 5) × 1%, capped at 50% total
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CombinedPenaltyCapService {
    
    private final PenaltyRepository penaltyRepository;
    private final LateFilingPenaltyService lateFilingPenaltyService;
    private final LatePaymentPenaltyService latePaymentPenaltyService;
    
    private static final int SCALE = 2; // 2 decimal places for currency
    private static final BigDecimal COMBINED_RATE = new BigDecimal("0.05"); // 5% per month combined
    private static final BigDecimal MAX_COMBINED_PENALTY = new BigDecimal("0.50"); // 50% total cap
    private static final int LATE_FILING_MAX_MONTHS = 5; // Late filing maxes at 5 months
    
    /**
     * Calculate both late filing and late payment penalties with combined cap applied.
     * 
     * FR-012: Combined cap of 5% per month when both penalties apply
     * FR-013: Months 1-5 use late filing penalty rate (5%)
     * FR-014: After month 5, late filing capped, late payment continues
     * 
     * @param request the penalty calculation request
     * @return combined penalty calculation response
     */
    @Transactional
    public PenaltyCalculationResponse calculateCombinedPenalties(PenaltyCalculationRequest request) {
        log.info("Calculating combined penalties with cap for return: {}", request.getReturnId());
        
        // Calculate individual penalties first
        PenaltyCalculationResponse filingPenalty = lateFilingPenaltyService
                .calculateLateFilingPenalty(request);
        
        PenaltyCalculationResponse paymentPenalty = latePaymentPenaltyService
                .calculateLatePaymentPenalty(request);
        
        // Retrieve saved penalty entities
        List<Penalty> penalties = penaltyRepository.findByReturnIdAndTenantId(
                request.getReturnId(), request.getTenantId());
        
        Penalty filingPenaltyEntity = penalties.stream()
                .filter(p -> p.getPenaltyType() == PenaltyType.LATE_FILING)
                .filter(p -> !Boolean.TRUE.equals(p.getIsAbated()))
                .findFirst()
                .orElse(null);
        
        Penalty paymentPenaltyEntity = penalties.stream()
                .filter(p -> p.getPenaltyType() == PenaltyType.LATE_PAYMENT)
                .filter(p -> !Boolean.TRUE.equals(p.getIsAbated()))
                .findFirst()
                .orElse(null);
        
        if (filingPenaltyEntity == null || paymentPenaltyEntity == null) {
            log.warn("Could not find both penalty types for combined cap calculation");
            return buildCombinedResponse(filingPenalty, paymentPenalty);
        }
        
        // Apply combined cap
        applyCombinedCap(filingPenaltyEntity, paymentPenaltyEntity, request.getUnpaidTaxAmount());
        
        // Save adjusted penalties
        penaltyRepository.save(filingPenaltyEntity);
        penaltyRepository.save(paymentPenaltyEntity);
        
        log.info("Combined penalty cap applied. Filing: ${}, Payment: ${}", 
                filingPenaltyEntity.getPenaltyAmount(), paymentPenaltyEntity.getPenaltyAmount());
        
        // Build combined response
        return buildCombinedResponseFromEntities(filingPenaltyEntity, paymentPenaltyEntity);
    }
    
    /**
     * Apply combined penalty cap logic.
     * 
     * FR-013: Months 1-5: Combined 5% per month (late filing absorbs both)
     * FR-014: After month 5: Late filing maxed at 25%, late payment continues at 1%/month
     * 
     * @param filing the late filing penalty entity
     * @param payment the late payment penalty entity
     * @param unpaidTax the unpaid tax amount
     */
    private void applyCombinedCap(Penalty filing, Penalty payment, BigDecimal unpaidTax) {
        // Use the later of the two dates (worst case scenario)
        int filingMonthsLate = filing.getMonthsLate();
        int paymentMonthsLate = payment.getMonthsLate();
        int maxMonths = Math.max(filingMonthsLate, paymentMonthsLate);
        
        log.debug("Applying combined cap: filing months={}, payment months={}, max={}", 
                filingMonthsLate, paymentMonthsLate, maxMonths);
        
        // FR-013: Months 1-5 - Late filing penalty (5%) covers both
        if (maxMonths <= LATE_FILING_MAX_MONTHS) {
            // During first 5 months, late filing (5%) absorbs both penalties
            // Late payment penalty is reduced to zero
            BigDecimal combinedPenalty = unpaidTax
                    .multiply(COMBINED_RATE)
                    .multiply(new BigDecimal(maxMonths))
                    .setScale(SCALE, RoundingMode.HALF_UP);
            
            filing.setPenaltyAmount(combinedPenalty);
            payment.setPenaltyAmount(BigDecimal.ZERO);
            
            log.info("Months 1-5: Late filing penalty ${} covers both (5% × {} months). " +
                    "Late payment penalty set to $0.", combinedPenalty, maxMonths);
        }
        // FR-014: After month 5 - Late filing maxed, late payment continues
        else {
            // Late filing is already capped at 25% (5 months)
            BigDecimal lateFilingCap = unpaidTax
                    .multiply(new BigDecimal("0.25"))
                    .setScale(SCALE, RoundingMode.HALF_UP);
            
            // Late payment continues for additional months beyond 5
            int additionalMonths = maxMonths - LATE_FILING_MAX_MONTHS;
            BigDecimal latePaymentPenalty = unpaidTax
                    .multiply(new BigDecimal("0.01")) // 1% per month
                    .multiply(new BigDecimal(additionalMonths))
                    .setScale(SCALE, RoundingMode.HALF_UP);
            
            // Cap late payment at 25% (would take 25 months)
            BigDecimal latePaymentCap = unpaidTax
                    .multiply(new BigDecimal("0.25"))
                    .setScale(SCALE, RoundingMode.HALF_UP);
            
            if (latePaymentPenalty.compareTo(latePaymentCap) > 0) {
                latePaymentPenalty = latePaymentCap;
            }
            
            // Apply penalties
            filing.setPenaltyAmount(lateFilingCap);
            payment.setPenaltyAmount(latePaymentPenalty);
            
            // Check combined maximum (50%)
            BigDecimal combinedPenalty = lateFilingCap.add(latePaymentPenalty);
            BigDecimal absoluteMax = unpaidTax
                    .multiply(MAX_COMBINED_PENALTY)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            
            if (combinedPenalty.compareTo(absoluteMax) > 0) {
                log.warn("Combined penalties ${} exceed 50% cap. Capping at ${}", 
                        combinedPenalty, absoluteMax);
                // Reduce late payment proportionally to stay under 50% total
                BigDecimal adjustment = combinedPenalty.subtract(absoluteMax);
                latePaymentPenalty = latePaymentPenalty.subtract(adjustment)
                        .setScale(SCALE, RoundingMode.HALF_UP);
                payment.setPenaltyAmount(latePaymentPenalty);
            }
            
            log.info("After month 5: Late filing capped at ${} (25%). " +
                    "Late payment continues for {} additional months: ${}", 
                    lateFilingCap, additionalMonths, latePaymentPenalty);
        }
    }
    
    /**
     * Check if combined cap should apply for a return.
     * Returns true if both late filing and late payment penalties exist.
     * 
     * @param returnId the return ID
     * @param tenantId the tenant ID
     * @return true if both penalty types exist
     */
    @Transactional(readOnly = true)
    public boolean shouldApplyCombinedCap(String returnId, String tenantId) {
        List<Penalty> penalties = penaltyRepository.findActiveByReturnIdAndTenantId(
                java.util.UUID.fromString(returnId),
                java.util.UUID.fromString(tenantId));
        
        boolean hasFilingPenalty = penalties.stream()
                .anyMatch(p -> p.getPenaltyType() == PenaltyType.LATE_FILING);
        
        boolean hasPaymentPenalty = penalties.stream()
                .anyMatch(p -> p.getPenaltyType() == PenaltyType.LATE_PAYMENT);
        
        return hasFilingPenalty && hasPaymentPenalty;
    }
    
    /**
     * Get combined penalty summary for a return.
     * 
     * @param returnId the return ID
     * @param tenantId the tenant ID
     * @return combined penalty response or null if no penalties
     */
    @Transactional(readOnly = true)
    public PenaltyCalculationResponse getCombinedPenaltySummary(String returnId, String tenantId) {
        List<Penalty> penalties = penaltyRepository.findActiveByReturnIdAndTenantId(
                java.util.UUID.fromString(returnId),
                java.util.UUID.fromString(tenantId));
        
        Penalty filing = penalties.stream()
                .filter(p -> p.getPenaltyType() == PenaltyType.LATE_FILING)
                .findFirst()
                .orElse(null);
        
        Penalty payment = penalties.stream()
                .filter(p -> p.getPenaltyType() == PenaltyType.LATE_PAYMENT)
                .findFirst()
                .orElse(null);
        
        if (filing == null && payment == null) {
            return null;
        }
        
        return buildCombinedResponseFromEntities(filing, payment);
    }
    
    /**
     * Build combined response from two penalty response objects.
     */
    private PenaltyCalculationResponse buildCombinedResponse(
            PenaltyCalculationResponse filing,
            PenaltyCalculationResponse payment) {
        
        BigDecimal totalPenalties = BigDecimal.ZERO;
        if (filing != null && filing.getLateFilingPenalty() != null) {
            totalPenalties = totalPenalties.add(filing.getLateFilingPenalty());
        }
        if (payment != null && payment.getLatePaymentPenalty() != null) {
            totalPenalties = totalPenalties.add(payment.getLatePaymentPenalty());
        }
        
        return PenaltyCalculationResponse.builder()
                .returnId(filing != null ? filing.getReturnId() : payment.getReturnId())
                .taxYearAndPeriod(filing != null ? filing.getTaxYearAndPeriod() : payment.getTaxYearAndPeriod())
                .dueDate(filing != null ? filing.getDueDate() : payment.getDueDate())
                .taxDue(filing != null ? filing.getTaxDue() : payment.getTaxDue())
                .lateFilingPenalty(filing != null ? filing.getLateFilingPenalty() : BigDecimal.ZERO)
                .lateFilingPenaltyRate(filing != null ? filing.getLateFilingPenaltyRate() : null)
                .lateFilingPenaltyExplanation(filing != null ? filing.getLateFilingPenaltyExplanation() : "")
                .latePaymentPenalty(payment != null ? payment.getLatePaymentPenalty() : BigDecimal.ZERO)
                .latePaymentPenaltyRate(payment != null ? payment.getLatePaymentPenaltyRate() : null)
                .latePaymentPenaltyExplanation(payment != null ? payment.getLatePaymentPenaltyExplanation() : "")
                .totalPenalties(totalPenalties)
                .combinedCapApplied(true)
                .build();
    }
    
    /**
     * Build combined response from two penalty entities.
     */
    private PenaltyCalculationResponse buildCombinedResponseFromEntities(
            Penalty filing, Penalty payment) {
        
        BigDecimal totalPenalties = BigDecimal.ZERO;
        String combinedExplanation = "Combined Penalty Cap Applied:\n\n";
        
        if (filing != null) {
            totalPenalties = totalPenalties.add(filing.getPenaltyAmount());
            combinedExplanation += String.format(
                    "Late Filing Penalty: $%,.2f\n" +
                    "  - Filed %d months late\n" +
                    "  - Rate: %s%% per month\n" +
                    "  - Capped at 25%% (5 months)\n\n",
                    filing.getPenaltyAmount(),
                    filing.getMonthsLate(),
                    filing.getPenaltyRate().multiply(BigDecimal.valueOf(100))
            );
        }
        
        if (payment != null) {
            totalPenalties = totalPenalties.add(payment.getPenaltyAmount());
            
            if (payment.getPenaltyAmount().compareTo(BigDecimal.ZERO) == 0) {
                combinedExplanation += "Late Payment Penalty: $0.00\n" +
                        "  - Absorbed by late filing penalty during first 5 months\n" +
                        "  - Combined cap: 5% per month maximum\n\n";
            } else {
                combinedExplanation += String.format(
                        "Late Payment Penalty: $%,.2f\n" +
                        "  - Paid %d months late\n" +
                        "  - Rate: 1%% per month (after month 5)\n" +
                        "  - Capped at 25%% (25 months)\n\n",
                        payment.getPenaltyAmount(),
                        payment.getMonthsLate()
                );
            }
        }
        
        combinedExplanation += String.format("Total Penalties: $%,.2f", totalPenalties);
        
        return PenaltyCalculationResponse.builder()
                .returnId(filing != null ? filing.getReturnId().toString() : payment.getReturnId().toString())
                .taxYearAndPeriod(filing != null ? filing.getTaxDueDate().getYear() + "" : payment.getTaxDueDate().getYear() + "")
                .dueDate(filing != null ? filing.getTaxDueDate() : payment.getTaxDueDate())
                .taxDue(filing != null ? filing.getUnpaidTaxAmount() : payment.getUnpaidTaxAmount())
                .lateFilingPenalty(filing != null ? filing.getPenaltyAmount() : BigDecimal.ZERO)
                .lateFilingPenaltyRate(filing != null ? filing.getPenaltyRate().multiply(BigDecimal.valueOf(100)) : null)
                .latePaymentPenalty(payment != null ? payment.getPenaltyAmount() : BigDecimal.ZERO)
                .latePaymentPenaltyRate(payment != null ? payment.getPenaltyRate().multiply(BigDecimal.valueOf(100)) : null)
                .totalPenalties(totalPenalties)
                .combinedCapApplied(true)
                .combinedPenaltyExplanation(combinedExplanation)
                .build();
    }
}
