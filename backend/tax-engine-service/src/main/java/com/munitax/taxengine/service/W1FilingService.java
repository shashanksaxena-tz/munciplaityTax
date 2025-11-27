package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.withholding.FilingFrequency;
import com.munitax.taxengine.domain.withholding.W1Filing;
import com.munitax.taxengine.domain.withholding.W1FilingStatus;
import com.munitax.taxengine.dto.W1FilingRequest;
import com.munitax.taxengine.dto.W1FilingResponse;
import com.munitax.taxengine.repository.W1FilingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for W-1 withholding return filing operations.
 * 
 * Core Functions:
 * - FR-001: File new W-1 returns
 * - FR-003: File amended W-1 returns with cascade updates
 * - FR-004: Validate filing patterns and detect anomalies
 * - FR-011: Calculate late-filing penalties
 * - FR-013: Support multiple filing frequencies
 * 
 * Business Rules:
 * - Municipal tax rate: 2.0% (configurable per municipality)
 * - Late filing penalty: 5% per month, max 25%, min $50 if tax > $200 (Research R4)
 * - Due dates: Varies by filing frequency (Research R5)
 * 
 * @see W1Filing
 * @see W1FilingRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class W1FilingService {
    
    private final W1FilingRepository w1FilingRepository;
    
    // Municipal tax rate for Dublin (configurable per municipality)
    private static final BigDecimal MUNICIPAL_TAX_RATE = new BigDecimal("0.0200"); // 2.0%
    
    /**
     * File a new W-1 withholding return.
     * 
     * Process:
     * 1. Validate no duplicate filing for same period
     * 2. Calculate due date based on filing frequency
     * 3. Calculate tax due = taxableWages × taxRate
     * 4. Calculate late filing penalty if filing_date > due_date
     * 5. Save W-1 filing
     * 6. Trigger cumulative totals update (event-driven)
     * 7. Log audit trail
     * 
     * @param request W-1 filing request
     * @param userId User filing the W-1 (from JWT)
     * @param tenantId Tenant ID (from JWT)
     * @return W-1 filing response with cumulative totals
     * @throws IllegalArgumentException if duplicate filing or invalid data
     */
    @Transactional
    public W1FilingResponse fileW1Return(W1FilingRequest request, UUID userId, UUID tenantId) {
        log.info("Filing W-1 return for business {} tax year {} period {}", 
                 request.getBusinessId(), request.getTaxYear(), request.getPeriod());
        
        // Check for duplicate filing
        boolean exists = w1FilingRepository.existsByBusinessIdAndTaxYearAndPeriodAndIsAmended(
            request.getBusinessId(), request.getTaxYear(), request.getPeriod(), false
        );
        if (exists) {
            throw new IllegalArgumentException(
                String.format("W-1 filing already exists for business %s tax year %d period %s. Use amendment endpoint to modify.",
                             request.getBusinessId(), request.getTaxYear(), request.getPeriod())
            );
        }
        
        // Set defaults
        BigDecimal taxableWages = request.getTaxableWages() != null ? 
                                  request.getTaxableWages() : request.getGrossWages();
        BigDecimal adjustments = request.getAdjustments() != null ? 
                                 request.getAdjustments() : BigDecimal.ZERO;
        
        // Calculate due date
        LocalDate dueDate = calculateDueDate(request.getFilingFrequency(), request.getPeriodEndDate());
        
        // Calculate tax due
        BigDecimal taxDue = taxableWages.multiply(MUNICIPAL_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        
        // Calculate late filing penalty
        LocalDateTime now = LocalDateTime.now();
        BigDecimal lateFilingPenalty = calculateLateFilingPenalty(dueDate, LocalDate.now(), taxDue);
        
        // Calculate total amount due
        BigDecimal totalAmountDue = taxDue.add(adjustments).add(lateFilingPenalty);
        
        // Build W1Filing entity
        W1Filing filing = W1Filing.builder()
            .tenantId(tenantId)
            .businessId(request.getBusinessId())
            .taxYear(request.getTaxYear())
            .filingFrequency(request.getFilingFrequency())
            .period(request.getPeriod())
            .periodStartDate(request.getPeriodStartDate())
            .periodEndDate(request.getPeriodEndDate())
            .dueDate(dueDate)
            .filingDate(now)
            .grossWages(request.getGrossWages())
            .taxableWages(taxableWages)
            .taxRate(MUNICIPAL_TAX_RATE)
            .taxDue(taxDue)
            .adjustments(adjustments)
            .totalAmountDue(totalAmountDue)
            .isAmended(false)
            .employeeCount(request.getEmployeeCount())
            .status(W1FilingStatus.FILED)
            .lateFilingPenalty(lateFilingPenalty)
            .underpaymentPenalty(BigDecimal.ZERO)
            .createdBy(userId)
            .build();
        
        // Save filing
        W1Filing savedFiling = w1FilingRepository.save(filing);
        log.info("W-1 filing saved with ID {}", savedFiling.getId());
        
        // TODO: Publish W1FiledEvent for cumulative totals update (event-driven)
        // TODO: Log audit trail to WithholdingAuditLog
        
        // Convert to response DTO
        return mapToResponse(savedFiling);
    }
    
    /**
     * Calculate due date based on filing frequency (Research R5).
     * 
     * Rules:
     * - QUARTERLY: 30 days after period end
     * - MONTHLY: 15th of following month
     * - SEMI_MONTHLY: 15th of following month
     * - DAILY: Next business day after period end
     * 
     * Adjustments for weekends and federal holidays applied.
     * 
     * @param frequency Filing frequency
     * @param periodEndDate Last day of filing period
     * @return Due date (accounting for weekends/holidays)
     */
    private LocalDate calculateDueDate(FilingFrequency frequency, LocalDate periodEndDate) {
        LocalDate dueDate;
        
        switch (frequency) {
            case QUARTERLY:
                dueDate = periodEndDate.plusDays(30);
                break;
            case MONTHLY:
            case SEMI_MONTHLY:
                dueDate = periodEndDate.plusMonths(1).withDayOfMonth(15);
                break;
            case DAILY:
                dueDate = periodEndDate.plusDays(1);
                break;
            default:
                throw new IllegalArgumentException("Unknown filing frequency: " + frequency);
        }
        
        // Adjust for weekends
        return adjustForWeekend(dueDate);
    }
    
    /**
     * Adjust date to next business day if falls on weekend.
     * TODO: Add federal holiday calendar support.
     * 
     * @param date Original date
     * @return Adjusted date (next Monday if Saturday/Sunday)
     */
    private LocalDate adjustForWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY) {
            return date.plusDays(2);
        } else if (dayOfWeek == DayOfWeek.SUNDAY) {
            return date.plusDays(1);
        }
        return date;
    }
    
    /**
     * Calculate late filing penalty (FR-011, Research R4).
     * 
     * Rules:
     * - 5% of tax due per month late (partial month rounds up)
     * - Maximum penalty: 25% of tax due
     * - Minimum penalty: $50 if tax due > $200
     * - No penalty if tax due = $0 (seasonal businesses)
     * 
     * @param dueDate W-1 due date
     * @param filingDate Actual filing date
     * @param taxDue Tax amount due
     * @return Late filing penalty
     */
    private BigDecimal calculateLateFilingPenalty(LocalDate dueDate, LocalDate filingDate, BigDecimal taxDue) {
        if (filingDate.isBefore(dueDate) || filingDate.isEqual(dueDate)) {
            return BigDecimal.ZERO; // Filed on time
        }
        
        if (taxDue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; // No penalty for $0 tax due (seasonal businesses)
        }
        
        // Calculate months late (round up partial months per Research R4)
        long daysLate = java.time.temporal.ChronoUnit.DAYS.between(dueDate, filingDate);
        int monthsLate = (int) Math.ceil(daysLate / 30.0);
        
        // Cap at 5 months (25% maximum penalty)
        monthsLate = Math.min(monthsLate, 5);
        
        // Calculate penalty: 5% per month
        BigDecimal penaltyRate = new BigDecimal("0.05").multiply(new BigDecimal(monthsLate));
        BigDecimal penalty = taxDue.multiply(penaltyRate).setScale(2, RoundingMode.HALF_UP);
        
        // Apply minimum $50 penalty if tax due > $200
        if (taxDue.compareTo(new BigDecimal("200.00")) > 0 && 
            penalty.compareTo(new BigDecimal("50.00")) < 0) {
            penalty = new BigDecimal("50.00");
        }
        
        return penalty;
    }
    
    /**
     * Map W1Filing entity to response DTO.
     * 
     * @param filing W1Filing entity
     * @return W1FilingResponse DTO
     */
    private W1FilingResponse mapToResponse(W1Filing filing) {
        return W1FilingResponse.builder()
            .id(filing.getId())
            .businessId(filing.getBusinessId())
            .taxYear(filing.getTaxYear())
            .filingFrequency(filing.getFilingFrequency())
            .period(filing.getPeriod())
            .periodStartDate(filing.getPeriodStartDate())
            .periodEndDate(filing.getPeriodEndDate())
            .dueDate(filing.getDueDate())
            .filingDate(filing.getFilingDate())
            .grossWages(filing.getGrossWages())
            .taxableWages(filing.getTaxableWages())
            .taxRate(filing.getTaxRate())
            .taxDue(filing.getTaxDue())
            .adjustments(filing.getAdjustments())
            .lateFilingPenalty(filing.getLateFilingPenalty())
            .underpaymentPenalty(filing.getUnderpaymentPenalty())
            .totalAmountDue(filing.getTotalAmountDue())
            .isAmended(filing.getIsAmended())
            .amendsFilingId(filing.getAmendsFilingRef() != null ? filing.getAmendsFilingRef().getId() : null)
            .amendmentReason(filing.getAmendmentReason())
            .employeeCount(filing.getEmployeeCount())
            .status(filing.getStatus())
            .createdAt(filing.getCreatedAt())
            .createdBy(filing.getCreatedBy())
            .updatedAt(filing.getUpdatedAt())
            .build();
        // TODO: Add cumulative totals to response
    }
}
