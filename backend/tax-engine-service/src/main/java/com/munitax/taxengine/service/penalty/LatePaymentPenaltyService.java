package com.munitax.taxengine.service.penalty;

import com.munitax.taxengine.domain.penalty.Penalty;
import com.munitax.taxengine.domain.penalty.PenaltyType;
import com.munitax.taxengine.dto.PenaltyCalculationRequest;
import com.munitax.taxengine.dto.PenaltyCalculationResponse;
import com.munitax.taxengine.repository.PenaltyRepository;
import com.munitax.taxengine.service.RuleEngineIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

/**
 * Service for calculating late payment penalties.
 * 
 * Functional Requirements:
 * - FR-007 to FR-011: Late payment penalty (1% per month, max 25%)
 * - FR-008: 1% of unpaid tax per month paid late
 * - FR-009: Penalty rate retrieved from rule-engine-service (default 1%)
 * - FR-010: Partial months rounded up to next full month
 * - FR-011: Maximum penalty of 25% (25 months × 1% = 25%)
 * 
 * User Story 2: Late Payment Penalty
 * As a tax administrator, I want the system to automatically calculate late payment penalties
 * so that businesses are charged 1% per month (max 25%) for late-paid taxes.
 * 
 * Handles partial payments by recalculating penalties on remaining balance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LatePaymentPenaltyService {
    
    private final PenaltyRepository penaltyRepository;
    private final RuleEngineIntegrationService ruleEngineService;
    
    private static final int SCALE = 2; // 2 decimal places for currency
    private static final BigDecimal MAX_PENALTY_RATE = new BigDecimal("0.25"); // 25%
    private static final int MAX_MONTHS = 25; // Maximum months for late payment penalty
    
    /**
     * Calculate late payment penalty for a tax return.
     * 
     * FR-008: 1% of unpaid tax per month paid late
     * FR-010: Partial months rounded up to next full month
     * FR-011: Maximum penalty of 25%
     * 
     * @param request the penalty calculation request
     * @return penalty calculation response
     */
    @Transactional
    public PenaltyCalculationResponse calculateLatePaymentPenalty(PenaltyCalculationRequest request) {
        log.info("Calculating late payment penalty for return: {}", request.getReturnId());
        
        // Validate request
        validateRequest(request);
        
        // Check if penalty already exists
        if (Boolean.TRUE.equals(request.getCheckExisting())) {
            Penalty existingPenalty = checkExistingPenalty(request);
            if (existingPenalty != null) {
                log.info("Found existing late payment penalty: {}", existingPenalty.getId());
                return buildResponseFromExistingPenalty(existingPenalty);
            }
        }
        
        // Determine actual date (payment date)
        LocalDate actualDate = request.getActualDate() != null 
                ? request.getActualDate() 
                : LocalDate.now();
        
        // Calculate months late (FR-010: round up partial months)
        int monthsLate = calculateMonthsLate(request.getTaxDueDate(), actualDate);
        
        // Cap at maximum months for late payment
        int cappedMonthsLate = Math.min(monthsLate, MAX_MONTHS);
        
        // Retrieve penalty rate from rule engine (FR-009)
        BigDecimal penaltyRate = ruleEngineService.getLatePaymentPenaltyRate(
                actualDate, request.getTenantId().toString());
        
        // Calculate penalty amount
        BigDecimal penaltyAmount = calculatePenaltyAmount(
                request.getUnpaidTaxAmount(), penaltyRate, cappedMonthsLate);
        
        // Calculate maximum penalty (25% of unpaid tax)
        BigDecimal maximumPenalty = request.getUnpaidTaxAmount()
                .multiply(MAX_PENALTY_RATE)
                .setScale(SCALE, RoundingMode.HALF_UP);
        
        // Cap penalty at maximum
        if (penaltyAmount.compareTo(maximumPenalty) > 0) {
            log.info("Capping penalty at maximum: {} (calculated: {})", maximumPenalty, penaltyAmount);
            penaltyAmount = maximumPenalty;
        }
        
        // Create and save penalty entity
        Penalty penalty = buildPenaltyEntity(request, actualDate, monthsLate, 
                penaltyRate, penaltyAmount, maximumPenalty);
        
        Penalty savedPenalty = penaltyRepository.save(penalty);
        
        log.info("Late payment penalty calculated and saved: {} for ${}", 
                savedPenalty.getId(), penaltyAmount);
        
        // Build response
        return buildResponse(savedPenalty, monthsLate, cappedMonthsLate, penaltyRate);
    }
    
    /**
     * Recalculate late payment penalty after a partial payment.
     * Used when a payment reduces the unpaid tax balance.
     * 
     * @param returnId the return ID
     * @param tenantId the tenant ID
     * @param newUnpaidBalance the new unpaid tax balance after payment
     * @param paymentDate the date of the payment
     * @param createdBy the user making the recalculation
     * @return recalculated penalty response
     */
    @Transactional
    public PenaltyCalculationResponse recalculateAfterPayment(
            String returnId, 
            String tenantId,
            BigDecimal newUnpaidBalance,
            LocalDate paymentDate,
            String createdBy) {
        
        log.info("Recalculating late payment penalty after payment for return: {}", returnId);
        
        // Get existing penalty
        Penalty existingPenalty = penaltyRepository.findByReturnIdAndPenaltyTypeAndTenantId(
                        java.util.UUID.fromString(returnId),
                        PenaltyType.LATE_PAYMENT,
                        java.util.UUID.fromString(tenantId))
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsAbated()))
                .findFirst()
                .orElse(null);
        
        if (existingPenalty == null) {
            log.warn("No existing late payment penalty found for return: {}", returnId);
            return null;
        }
        
        // If balance is zero, no further penalty
        if (newUnpaidBalance.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Tax fully paid, no further late payment penalty");
            return buildResponseFromExistingPenalty(existingPenalty);
        }
        
        // Calculate new months late from due date to payment date
        int monthsLate = calculateMonthsLate(existingPenalty.getTaxDueDate(), paymentDate);
        int cappedMonthsLate = Math.min(monthsLate, MAX_MONTHS);
        
        // Calculate new penalty on reduced balance
        BigDecimal penaltyAmount = calculatePenaltyAmount(
                newUnpaidBalance, existingPenalty.getPenaltyRate(), cappedMonthsLate);
        
        // Calculate maximum penalty on new balance
        BigDecimal maximumPenalty = newUnpaidBalance
                .multiply(MAX_PENALTY_RATE)
                .setScale(SCALE, RoundingMode.HALF_UP);
        
        // Cap at maximum
        if (penaltyAmount.compareTo(maximumPenalty) > 0) {
            penaltyAmount = maximumPenalty;
        }
        
        // Create new penalty record (keep history)
        PenaltyCalculationRequest request = PenaltyCalculationRequest.builder()
                .tenantId(java.util.UUID.fromString(tenantId))
                .returnId(java.util.UUID.fromString(returnId))
                .taxDueDate(existingPenalty.getTaxDueDate())
                .actualDate(paymentDate)
                .unpaidTaxAmount(newUnpaidBalance)
                .createdBy(java.util.UUID.fromString(createdBy))
                .build();
        
        return calculateLatePaymentPenalty(request);
    }
    
    /**
     * Calculate months late from due date to actual date.
     * FR-010: Partial months rounded up to next full month.
     * 
     * @param dueDate    the original due date
     * @param actualDate the actual payment date
     * @return number of months late (rounded up)
     */
    private int calculateMonthsLate(LocalDate dueDate, LocalDate actualDate) {
        if (!actualDate.isAfter(dueDate)) {
            return 0; // Paid on time or early
        }
        
        Period period = Period.between(dueDate, actualDate);
        int months = period.getYears() * 12 + period.getMonths();
        
        // Round up if there are any additional days (FR-010)
        if (period.getDays() > 0) {
            months++;
        }
        
        log.debug("Calculated months late: {} (from {} to {})", months, dueDate, actualDate);
        return months;
    }
    
    /**
     * Calculate penalty amount.
     * Formula: unpaid_tax × penalty_rate × months_late
     * 
     * @param unpaidTax   the unpaid tax amount
     * @param penaltyRate the monthly penalty rate (e.g., 0.01 for 1%)
     * @param monthsLate  the number of months late
     * @return calculated penalty amount
     */
    private BigDecimal calculatePenaltyAmount(BigDecimal unpaidTax, 
                                             BigDecimal penaltyRate, 
                                             int monthsLate) {
        return unpaidTax
                .multiply(penaltyRate)
                .multiply(new BigDecimal(monthsLate))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Check if penalty already exists for this return.
     * 
     * @param request the penalty calculation request
     * @return existing penalty or null
     */
    private Penalty checkExistingPenalty(PenaltyCalculationRequest request) {
        return penaltyRepository.findByReturnIdAndPenaltyTypeAndTenantId(
                        request.getReturnId(),
                        PenaltyType.LATE_PAYMENT,
                        request.getTenantId())
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsAbated()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Build penalty entity from request and calculation results.
     */
    private Penalty buildPenaltyEntity(PenaltyCalculationRequest request,
                                      LocalDate actualDate,
                                      int monthsLate,
                                      BigDecimal penaltyRate,
                                      BigDecimal penaltyAmount,
                                      BigDecimal maximumPenalty) {
        return Penalty.builder()
                .tenantId(request.getTenantId())
                .returnId(request.getReturnId())
                .penaltyType(PenaltyType.LATE_PAYMENT)
                .assessmentDate(LocalDate.now())
                .taxDueDate(request.getTaxDueDate())
                .actualDate(actualDate)
                .monthsLate(monthsLate)
                .unpaidTaxAmount(request.getUnpaidTaxAmount())
                .penaltyRate(penaltyRate)
                .penaltyAmount(penaltyAmount)
                .maximumPenalty(maximumPenalty)
                .isAbated(false)
                .createdBy(request.getCreatedBy())
                .build();
    }
    
    /**
     * Build response from saved penalty entity.
     */
    private PenaltyCalculationResponse buildResponse(Penalty penalty, 
                                                    int actualMonthsLate,
                                                    int cappedMonthsLate,
                                                    BigDecimal penaltyRate) {
        boolean cappedAtMax = actualMonthsLate > MAX_MONTHS;
        
        String explanation = String.format(
                "Paid %d month%s late (%s to %s). " +
                "Penalty: %d month%s × %s%% × $%,.2f = $%,.2f%s",
                actualMonthsLate,
                actualMonthsLate == 1 ? "" : "s",
                penalty.getTaxDueDate(),
                penalty.getActualDate(),
                cappedMonthsLate,
                cappedMonthsLate == 1 ? "" : "s",
                penaltyRate.multiply(BigDecimal.valueOf(100)),
                penalty.getUnpaidTaxAmount(),
                penalty.getPenaltyAmount(),
                cappedAtMax ? " (capped at 25% maximum)" : ""
        );
        
        return PenaltyCalculationResponse.builder()
                .penaltyId(penalty.getId().toString())
                .returnId(penalty.getReturnId().toString())
                .taxYearAndPeriod(penalty.getTaxDueDate().getYear() + "")
                .dueDate(penalty.getTaxDueDate())
                .paymentDate(penalty.getActualDate())
                .daysLate(Period.between(penalty.getTaxDueDate(), penalty.getActualDate()).getDays())
                .taxDue(penalty.getUnpaidTaxAmount())
                .latePaymentPenalty(penalty.getPenaltyAmount())
                .latePaymentPenaltyRate(penaltyRate.multiply(BigDecimal.valueOf(100)))
                .latePaymentPenaltyExplanation(explanation)
                .totalPenalties(penalty.getPenaltyAmount())
                .isAbated(penalty.getIsAbated())
                .build();
    }
    
    /**
     * Build response from existing penalty.
     */
    private PenaltyCalculationResponse buildResponseFromExistingPenalty(Penalty penalty) {
        return buildResponse(penalty, penalty.getMonthsLate(), penalty.getMonthsLate(), 
                penalty.getPenaltyRate());
    }
    
    /**
     * Validate penalty calculation request.
     */
    private void validateRequest(PenaltyCalculationRequest request) {
        if (request.getTenantId() == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (request.getReturnId() == null) {
            throw new IllegalArgumentException("Return ID is required");
        }
        if (request.getTaxDueDate() == null) {
            throw new IllegalArgumentException("Tax due date is required");
        }
        if (request.getUnpaidTaxAmount() == null || 
                request.getUnpaidTaxAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid unpaid tax amount is required");
        }
        if (request.getCreatedBy() == null) {
            throw new IllegalArgumentException("Created by is required");
        }
    }
}
