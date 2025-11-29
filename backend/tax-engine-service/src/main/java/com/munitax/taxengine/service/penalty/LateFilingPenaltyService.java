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
import java.util.UUID;

/**
 * Service for calculating late filing penalties.
 * 
 * Functional Requirements:
 * - FR-001 to FR-006: Late filing penalty (5% per month, max 25%)
 * - FR-003: 5% of unpaid tax per month filed late
 * - FR-004: Penalty rate retrieved from rule-engine-service (default 5%)
 * - FR-005: Partial months rounded up to next full month
 * - FR-006: Maximum penalty of 25% (5 months × 5% = 25%)
 * 
 * User Story 1: Late Filing Penalty
 * As a tax administrator, I want the system to automatically calculate late filing penalties
 * so that businesses are charged 5% per month (max 25%) for late-filed returns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LateFilingPenaltyService {
    
    private final PenaltyRepository penaltyRepository;
    private final RuleEngineIntegrationService ruleEngineService;
    
    private static final int SCALE = 2; // 2 decimal places for currency
    private static final BigDecimal MAX_PENALTY_RATE = new BigDecimal("0.25"); // 25%
    private static final int MAX_MONTHS = 5; // Maximum months for late filing penalty
    
    /**
     * Calculate late filing penalty for a tax return.
     * 
     * FR-003: 5% of unpaid tax per month filed late
     * FR-005: Partial months rounded up to next full month
     * FR-006: Maximum penalty of 25%
     * 
     * @param request the penalty calculation request
     * @return penalty calculation response
     */
    @Transactional
    public PenaltyCalculationResponse calculateLateFilingPenalty(PenaltyCalculationRequest request) {
        log.info("Calculating late filing penalty for return: {}", request.getReturnId());
        
        // Validate request
        validateRequest(request);
        
        // Check if penalty already exists
        if (Boolean.TRUE.equals(request.getCheckExisting())) {
            Penalty existingPenalty = checkExistingPenalty(request);
            if (existingPenalty != null) {
                log.info("Found existing late filing penalty: {}", existingPenalty.getId());
                return buildResponseFromExistingPenalty(existingPenalty);
            }
        }
        
        // Determine actual date (filing date)
        LocalDate actualDate = request.getActualDate() != null 
                ? request.getActualDate() 
                : LocalDate.now();
        
        // Calculate months late (FR-005: round up partial months)
        int monthsLate = calculateMonthsLate(request.getTaxDueDate(), actualDate);
        
        // Cap at maximum months for late filing
        int cappedMonthsLate = Math.min(monthsLate, MAX_MONTHS);
        
        // Retrieve penalty rate from rule engine (FR-004)
        BigDecimal penaltyRate = ruleEngineService.getLateFilingPenaltyRate(
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
        
        log.info("Late filing penalty calculated and saved: {} for ${}", 
                savedPenalty.getId(), penaltyAmount);
        
        // Build response
        return buildResponse(savedPenalty, monthsLate, cappedMonthsLate, penaltyRate);
    }
    
    /**
     * Calculate months late from due date to actual date.
     * FR-005: Partial months rounded up to next full month.
     * 
     * @param dueDate    the original due date
     * @param actualDate the actual filing date
     * @return number of months late (rounded up)
     */
    private int calculateMonthsLate(LocalDate dueDate, LocalDate actualDate) {
        if (!actualDate.isAfter(dueDate)) {
            return 0; // Filed on time or early
        }
        
        Period period = Period.between(dueDate, actualDate);
        int months = period.getYears() * 12 + period.getMonths();
        
        // Round up if there are any additional days (FR-005)
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
     * @param penaltyRate the monthly penalty rate (e.g., 0.05 for 5%)
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
                        PenaltyType.LATE_FILING,
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
                .penaltyType(PenaltyType.LATE_FILING)
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
                "Filed %d month%s late (%s to %s). " +
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
                .filingDate(penalty.getActualDate())
                .daysLate(Period.between(penalty.getTaxDueDate(), penalty.getActualDate()).getDays())
                .taxDue(penalty.getUnpaidTaxAmount())
                .lateFilingPenalty(penalty.getPenaltyAmount())
                .lateFilingPenaltyRate(penaltyRate.multiply(BigDecimal.valueOf(100)))
                .lateFilingPenaltyExplanation(explanation)
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
