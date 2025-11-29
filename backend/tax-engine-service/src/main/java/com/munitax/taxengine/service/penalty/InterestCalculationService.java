package com.munitax.taxengine.service.penalty;

import com.munitax.taxengine.domain.penalty.CompoundingFrequency;
import com.munitax.taxengine.domain.penalty.Interest;
import com.munitax.taxengine.domain.penalty.Quarter;
import com.munitax.taxengine.domain.penalty.QuarterlyInterest;
import com.munitax.taxengine.dto.InterestCalculationRequest;
import com.munitax.taxengine.dto.InterestCalculationResponse;
import com.munitax.taxengine.repository.InterestRepository;
import com.munitax.taxengine.service.RuleEngineIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for calculating interest on unpaid tax with quarterly compounding.
 * 
 * Functional Requirements:
 * - FR-027 to FR-032: Calculate interest with quarterly compounding
 * - FR-028: Retrieve current interest rate from rule engine (federal short-term + 3%)
 * - FR-029: Calculate daily interest: (Unpaid tax) × (Annual rate / 365) × (Days)
 * - FR-030: Compound interest quarterly
 * - FR-031: Calculate interest on unpaid penalties and prior interest
 * - FR-032: Display interest calculation breakdown by quarter
 * 
 * User Story 6: Interest Calculation
 * As a tax administrator, I want the system to calculate interest on unpaid tax, penalties, 
 * and prior interest, compounding quarterly, so that taxpayers pay the correct amount for 
 * delayed payment.
 * 
 * Interest accrues daily and compounds quarterly on unpaid balances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestCalculationService {
    
    private final InterestRepository interestRepository;
    private final RuleEngineIntegrationService ruleEngineService;
    
    private static final int SCALE = 2; // 2 decimal places for currency
    private static final int RATE_SCALE = 6; // 6 decimal places for interest rates
    private static final int DAYS_PER_YEAR = 365;
    private static final int DAYS_PER_QUARTER = 91; // Approximate
    
    /**
     * Calculate interest on unpaid tax with quarterly compounding.
     * 
     * FR-027: Interest calculation with quarterly compounding
     * FR-028: Retrieve interest rate from rule engine
     * FR-029: Daily interest calculation
     * FR-030: Quarterly compounding
     * 
     * @param request interest calculation request
     * @return interest calculation response
     */
    @Transactional
    public InterestCalculationResponse calculateInterest(InterestCalculationRequest request) {
        log.info("Calculating interest for return: {}", request.getReturnId());
        
        // Validate request
        validateRequest(request);
        
        // Determine calculation period
        LocalDate startDate = request.getStartDate() != null 
                ? request.getStartDate() 
                : request.getTaxDueDate();
        LocalDate endDate = request.getEndDate() != null 
                ? request.getEndDate() 
                : LocalDate.now();
        
        // FR-028: Retrieve interest rate from rule engine
        BigDecimal annualInterestRate;
        if (Boolean.TRUE.equals(request.getRetrieveCurrentRate()) || request.getAnnualInterestRate() == null) {
            annualInterestRate = ruleEngineService.getInterestRate(
                    endDate, request.getTenantId().toString());
            log.debug("Retrieved interest rate from rule engine: {}%", 
                    annualInterestRate.multiply(BigDecimal.valueOf(100)));
        } else {
            annualInterestRate = request.getAnnualInterestRate();
        }
        
        // Calculate total days
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        if (totalDays <= 0) {
            log.info("No interest due - payment on or before due date");
            return createZeroInterestResponse(request, startDate, endDate, annualInterestRate);
        }
        
        // FR-030: Calculate interest with quarterly compounding
        List<QuarterlyInterest> quarterlyBreakdown = new ArrayList<>();
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal runningBalance = request.getUnpaidTaxAmount();
        
        // Break down calculation by quarter
        LocalDate currentQuarterStart = startDate;
        int quarterNumber = 1;
        
        while (currentQuarterStart.isBefore(endDate)) {
            LocalDate currentQuarterEnd = calculateQuarterEnd(currentQuarterStart, endDate);
            long daysInPeriod = ChronoUnit.DAYS.between(currentQuarterStart, currentQuarterEnd) + 1;
            
            // FR-029: Calculate daily interest for this quarter
            BigDecimal quarterInterest = calculateDailyInterest(
                    runningBalance, annualInterestRate, daysInPeriod);
            
            totalInterest = totalInterest.add(quarterInterest);
            
            // Create quarterly breakdown
            if (Boolean.TRUE.equals(request.getIncludeQuarterlyBreakdown())) {
                QuarterlyInterest quarterlyRecord = QuarterlyInterest.builder()
                        .quarter(determineQuarter(currentQuarterStart))
                        .startDate(currentQuarterStart)
                        .endDate(currentQuarterEnd)
                        .daysInPeriod((int) daysInPeriod)
                        .beginningBalance(runningBalance)
                        .interestRate(annualInterestRate)
                        .interestAmount(quarterInterest)
                        .endingBalance(runningBalance.add(quarterInterest))
                        .build();
                
                quarterlyBreakdown.add(quarterlyRecord);
                
                log.debug("Q{}: ${} × {}% × {} days = ${} interest",
                        quarterNumber, runningBalance, 
                        annualInterestRate.multiply(BigDecimal.valueOf(100)),
                        daysInPeriod, quarterInterest);
            }
            
            // FR-031: Compound interest - add to running balance for next quarter
            runningBalance = runningBalance.add(quarterInterest);
            
            // Move to next quarter
            currentQuarterStart = currentQuarterEnd.plusDays(1);
            quarterNumber++;
        }
        
        // Create and save interest entity
        Interest interest = Interest.builder()
                .tenantId(request.getTenantId())
                .returnId(request.getReturnId())
                .taxDueDate(request.getTaxDueDate())
                .startDate(startDate)
                .endDate(endDate)
                .unpaidTaxAmount(request.getUnpaidTaxAmount())
                .annualInterestRate(annualInterestRate)
                .totalDays((int) totalDays)
                .totalInterestAmount(totalInterest)
                .compoundingFrequency(CompoundingFrequency.QUARTERLY)
                .quarterlyBreakdown(quarterlyBreakdown)
                .createdBy(request.getCreatedBy())
                .build();
        
        // Set bidirectional relationship
        quarterlyBreakdown.forEach(q -> q.setInterest(interest));
        
        Interest savedInterest = interestRepository.save(interest);
        
        log.info("Interest calculated and saved: {} for ${}", savedInterest.getId(), totalInterest);
        
        // Build response
        return buildResponse(savedInterest, quarterlyBreakdown);
    }
    
    /**
     * Calculate daily interest for a period.
     * 
     * FR-029: Interest = (Unpaid tax) × (Annual rate / 365) × (Days)
     * 
     * @param principal the unpaid amount
     * @param annualRate the annual interest rate (e.g., 0.06 for 6%)
     * @param days the number of days
     * @return interest amount
     */
    private BigDecimal calculateDailyInterest(BigDecimal principal, BigDecimal annualRate, long days) {
        BigDecimal dailyRate = annualRate.divide(
                BigDecimal.valueOf(DAYS_PER_YEAR), RATE_SCALE, RoundingMode.HALF_UP);
        
        return principal
                .multiply(dailyRate)
                .multiply(BigDecimal.valueOf(days))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate the end date of the current quarter.
     * Quarters are approximately 91 days, but we align to calendar quarter ends.
     * 
     * @param quarterStart the start of the quarter
     * @param calculationEnd the end of the calculation period
     * @return end date of the quarter
     */
    private LocalDate calculateQuarterEnd(LocalDate quarterStart, LocalDate calculationEnd) {
        // Find next quarter end date (Mar 31, Jun 30, Sep 30, Dec 31)
        LocalDate nextQuarterEnd = getNextQuarterEndDate(quarterStart);
        
        // Use the earlier of next quarter end or calculation end date
        return nextQuarterEnd.isBefore(calculationEnd) ? nextQuarterEnd : calculationEnd;
    }
    
    /**
     * Get the next calendar quarter end date after the given date.
     * 
     * @param date the reference date
     * @return next quarter end date
     */
    private LocalDate getNextQuarterEndDate(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        
        if (month <= 3) {
            return LocalDate.of(year, 3, 31);
        } else if (month <= 6) {
            return LocalDate.of(year, 6, 30);
        } else if (month <= 9) {
            return LocalDate.of(year, 9, 30);
        } else {
            return LocalDate.of(year, 12, 31);
        }
    }
    
    /**
     * Determine which quarter a date falls into.
     * 
     * @param date the date
     * @return the quarter
     */
    private Quarter determineQuarter(LocalDate date) {
        int month = date.getMonthValue();
        
        if (month <= 3) {
            return Quarter.Q1;
        } else if (month <= 6) {
            return Quarter.Q2;
        } else if (month <= 9) {
            return Quarter.Q3;
        } else {
            return Quarter.Q4;
        }
    }
    
    /**
     * Create a zero interest response when no interest is due.
     * 
     * @param request the request
     * @param startDate the start date
     * @param endDate the end date
     * @param annualRate the annual interest rate
     * @return zero interest response
     */
    private InterestCalculationResponse createZeroInterestResponse(
            InterestCalculationRequest request,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal annualRate) {
        
        return InterestCalculationResponse.builder()
                .interestId(null)
                .returnId(request.getReturnId().toString())
                .taxDueDate(request.getTaxDueDate())
                .startDate(startDate)
                .endDate(endDate)
                .unpaidTaxAmount(request.getUnpaidTaxAmount())
                .annualInterestRate(annualRate)
                .totalDays(0)
                .totalInterestAmount(BigDecimal.ZERO)
                .explanation("No interest due - payment on or before due date")
                .build();
    }
    
    /**
     * Build response from saved interest entity.
     * 
     * @param interest the saved interest entity
     * @param quarterlyBreakdown the quarterly breakdown
     * @return interest calculation response
     */
    private InterestCalculationResponse buildResponse(Interest interest, 
                                                     List<QuarterlyInterest> quarterlyBreakdown) {
        String explanation = String.format(
                "Interest calculated over %d days (%s to %s) at %.2f%% annual rate with quarterly compounding. " +
                "Total interest: $%,.2f on unpaid tax of $%,.2f",
                interest.getTotalDays(),
                interest.getStartDate(),
                interest.getEndDate(),
                interest.getAnnualInterestRate().multiply(BigDecimal.valueOf(100)),
                interest.getTotalInterestAmount(),
                interest.getUnpaidTaxAmount()
        );
        
        return InterestCalculationResponse.builder()
                .interestId(interest.getId().toString())
                .returnId(interest.getReturnId().toString())
                .taxDueDate(interest.getTaxDueDate())
                .startDate(interest.getStartDate())
                .endDate(interest.getEndDate())
                .unpaidTaxAmount(interest.getUnpaidTaxAmount())
                .annualInterestRate(interest.getAnnualInterestRate())
                .totalDays(interest.getTotalDays())
                .totalInterestAmount(interest.getTotalInterestAmount())
                .compoundingFrequency(interest.getCompoundingFrequency())
                .quarterlyBreakdown(quarterlyBreakdown)
                .explanation(explanation)
                .build();
    }
    
    /**
     * Validate interest calculation request.
     */
    private void validateRequest(InterestCalculationRequest request) {
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
