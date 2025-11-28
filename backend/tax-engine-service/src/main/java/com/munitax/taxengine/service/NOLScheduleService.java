package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.nol.*;
import com.munitax.taxengine.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for NOL Schedule generation and management.
 * 
 * Core Functions:
 * - FR-004: Display NOL schedule on tax return
 * - FR-012: Display calculation breakdown
 * - FR-036: Generate Form 27-NOL
 * - FR-045: Reconcile NOL balance across years
 * 
 * @see NOLSchedule
 * @see NOL
 * @see NOLUsage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NOLScheduleService {
    
    private final NOLRepository nolRepository;
    private final NOLUsageRepository nolUsageRepository;
    private final NOLScheduleRepository nolScheduleRepository;
    private final NOLService nolService;
    
    private static final int TCJA_EFFECTIVE_YEAR = 2018;
    private static final BigDecimal POST_TCJA_LIMITATION = new BigDecimal("80.00");
    private static final BigDecimal PRE_TCJA_LIMITATION = new BigDecimal("100.00");
    
    /**
     * Generate NOL schedule for a tax return.
     * 
     * Process:
     * 1. Get prior year ending balance (or 0 if first year)
     * 2. Calculate new NOL generated in current year (if loss)
     * 3. Calculate total available NOL
     * 4. Sum NOL deductions for current year
     * 5. Calculate expired NOL amount
     * 6. Calculate ending balance for carryforward
     * 7. Create NOLSchedule record
     * 
     * @param businessId Business profile ID
     * @param returnId Tax return ID
     * @param taxYear Current tax year
     * @param taxableIncomeBeforeNOL Taxable income before NOL deduction
     * @param newNOLGenerated New NOL if current year has loss (0 if profitable)
     * @param jurisdiction Tax jurisdiction
     * @param tenantId Tenant ID
     * @return Created NOL schedule
     */
    @Transactional
    public NOLSchedule generateNOLSchedule(UUID businessId, UUID returnId, Integer taxYear,
                                          BigDecimal taxableIncomeBeforeNOL,
                                          BigDecimal newNOLGenerated, Jurisdiction jurisdiction,
                                          UUID tenantId) {
        
        log.info("Generating NOL schedule for business {} tax year {} return {}",
                 businessId, taxYear, returnId);
        
        // Get prior year ending balance
        BigDecimal beginningBalance = getPriorYearEndingBalance(businessId, taxYear, jurisdiction);
        
        // Calculate total available NOL
        BigDecimal totalAvailable = beginningBalance.add(newNOLGenerated);
        
        // Get NOL deductions for current year
        List<NOLUsage> usages = nolUsageRepository.findByReturnId(returnId);
        BigDecimal nolDeduction = usages.stream()
            .map(NOLUsage::getActualNOLDeduction)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate expired NOL
        BigDecimal expiredNOL = calculateExpiredNOL(businessId, taxYear, jurisdiction);
        
        // Calculate ending balance
        BigDecimal endingBalance = totalAvailable.subtract(nolDeduction).subtract(expiredNOL);
        
        // Calculate taxable income after NOL
        BigDecimal taxableIncomeAfterNOL = taxableIncomeBeforeNOL.subtract(nolDeduction);
        
        // Determine limitation percentage
        BigDecimal limitationPercentage = taxYear >= TCJA_EFFECTIVE_YEAR ?
                                         POST_TCJA_LIMITATION : PRE_TCJA_LIMITATION;
        
        // Create schedule
        NOLSchedule schedule = NOLSchedule.builder()
            .tenantId(tenantId)
            .businessId(businessId)
            .returnId(returnId)
            .taxYear(taxYear)
            .totalBeginningBalance(beginningBalance)
            .newNOLGenerated(newNOLGenerated)
            .totalAvailableNOL(totalAvailable)
            .nolDeduction(nolDeduction)
            .expiredNOL(expiredNOL)
            .totalEndingBalance(endingBalance)
            .limitationPercentage(limitationPercentage)
            .taxableIncomeBeforeNOL(taxableIncomeBeforeNOL)
            .taxableIncomeAfterNOL(taxableIncomeAfterNOL)
            .build();
        
        schedule = nolScheduleRepository.save(schedule);
        
        log.info("Generated NOL schedule: beginning {} + new {} - used {} - expired {} = ending {}",
                 beginningBalance, newNOLGenerated, nolDeduction, expiredNOL, endingBalance);
        
        return schedule;
    }
    
    /**
     * Get NOL schedule for a tax return.
     * 
     * @param returnId Tax return ID
     * @return Optional NOL schedule
     */
    public Optional<NOLSchedule> getScheduleForReturn(UUID returnId) {
        return nolScheduleRepository.findByReturnId(returnId);
    }
    
    /**
     * Get multi-year NOL schedule for a business.
     * Shows all years with NOL activity.
     * 
     * @param businessId Business profile ID
     * @return List of NOL schedules by year
     */
    public List<NOLSchedule> getMultiYearSchedule(UUID businessId) {
        return nolScheduleRepository.findByBusinessIdOrderByTaxYearAsc(businessId);
    }
    
    /**
     * Get detailed NOL vintage breakdown for current year.
     * Shows each NOL vintage with usage details.
     * 
     * @param businessId Business profile ID
     * @param taxYear Current tax year
     * @return List of NOL vintage details
     */
    public List<NOLVintageDetail> getNOLVintageBreakdown(UUID businessId, Integer taxYear) {
        List<NOL> nols = nolRepository.findByBusinessIdOrderByTaxYearAsc(businessId);
        
        return nols.stream()
            .map(nol -> {
                // Get usage history for this NOL
                List<NOLUsage> usages = nolUsageRepository.findByNolIdOrderByUsageYearAsc(nol.getId());
                
                // Get usage for current year (if any)
                BigDecimal usedThisYear = usages.stream()
                    .filter(u -> u.getUsageYear().equals(taxYear))
                    .map(NOLUsage::getActualNOLDeduction)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                return new NOLVintageDetail(
                    nol.getTaxYear(),
                    nol.getOriginalNOLAmount(),
                    nol.getUsedAmount(),
                    nol.getExpiredAmount(),
                    nol.getCurrentNOLBalance(),
                    usedThisYear,
                    nol.getExpirationDate(),
                    nol.getIsCarriedBack(),
                    nol.getCarrybackAmount()
                );
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Validate NOL balance reconciliation across years.
     * Ensures ending balance from prior year = beginning balance of current year.
     * 
     * @param businessId Business profile ID
     * @param taxYear Current tax year
     * @return true if reconciled, false if discrepancy found
     */
    public boolean validateNOLReconciliation(UUID businessId, Integer taxYear) {
        Optional<NOLSchedule> priorYear = nolScheduleRepository.findPriorYearSchedule(businessId, taxYear);
        Optional<NOLSchedule> currentYear = nolScheduleRepository.findByBusinessIdAndTaxYear(businessId, taxYear);
        
        if (priorYear.isEmpty() || currentYear.isEmpty()) {
            return true; // Cannot validate without both years
        }
        
        BigDecimal priorEndingBalance = priorYear.get().getTotalEndingBalance();
        BigDecimal currentBeginningBalance = currentYear.get().getTotalBeginningBalance();
        
        boolean reconciled = priorEndingBalance.compareTo(currentBeginningBalance) == 0;
        
        if (!reconciled) {
            log.warn("NOL reconciliation mismatch for business {} year {}: prior ending {} != current beginning {}",
                     businessId, taxYear, priorEndingBalance, currentBeginningBalance);
        }
        
        return reconciled;
    }
    
    /**
     * Get prior year ending balance for beginning balance calculation.
     * 
     * @param businessId Business profile ID
     * @param taxYear Current tax year
     * @param jurisdiction Tax jurisdiction (optional filter)
     * @return Prior year ending balance (0 if no prior year)
     */
    private BigDecimal getPriorYearEndingBalance(UUID businessId, Integer taxYear, Jurisdiction jurisdiction) {
        Optional<NOLSchedule> priorSchedule = nolScheduleRepository.findPriorYearSchedule(businessId, taxYear);
        
        if (priorSchedule.isPresent()) {
            return priorSchedule.get().getTotalEndingBalance();
        } else {
            // First year - calculate from available NOLs
            return nolService.calculateAvailableNOLBalance(businessId, jurisdiction);
        }
    }
    
    /**
     * Calculate NOLs that expired during current year.
     * 
     * @param businessId Business profile ID
     * @param taxYear Current tax year
     * @param jurisdiction Tax jurisdiction
     * @return Total expired NOL amount
     */
    private BigDecimal calculateExpiredNOL(UUID businessId, Integer taxYear, Jurisdiction jurisdiction) {
        List<NOL> allNOLs = jurisdiction != null ?
            nolRepository.findByBusinessIdAndJurisdictionOrderByTaxYearAsc(businessId, jurisdiction) :
            nolRepository.findByBusinessIdOrderByTaxYearAsc(businessId);
        
        return allNOLs.stream()
            .filter(nol -> nol.getExpirationDate() != null)
            .filter(nol -> nol.getExpirationDate().getYear() == taxYear)
            .filter(nol -> nol.getCurrentNOLBalance().compareTo(BigDecimal.ZERO) > 0)
            .map(NOL::getCurrentNOLBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Data class for NOL vintage detail display.
     */
    public static class NOLVintageDetail {
        private final Integer taxYear;
        private final BigDecimal originalAmount;
        private final BigDecimal previouslyUsed;
        private final BigDecimal expired;
        private final BigDecimal availableThisYear;
        private final BigDecimal usedThisYear;
        private final java.time.LocalDate expirationDate;
        private final Boolean isCarriedBack;
        private final BigDecimal carrybackAmount;
        
        public NOLVintageDetail(Integer taxYear, BigDecimal originalAmount, BigDecimal previouslyUsed,
                               BigDecimal expired, BigDecimal availableThisYear, BigDecimal usedThisYear,
                               java.time.LocalDate expirationDate, Boolean isCarriedBack, BigDecimal carrybackAmount) {
            this.taxYear = taxYear;
            this.originalAmount = originalAmount;
            this.previouslyUsed = previouslyUsed;
            this.expired = expired;
            this.availableThisYear = availableThisYear;
            this.usedThisYear = usedThisYear;
            this.expirationDate = expirationDate;
            this.isCarriedBack = isCarriedBack;
            this.carrybackAmount = carrybackAmount;
        }
        
        // Getters
        public Integer getTaxYear() { return taxYear; }
        public BigDecimal getOriginalAmount() { return originalAmount; }
        public BigDecimal getPreviouslyUsed() { return previouslyUsed; }
        public BigDecimal getExpired() { return expired; }
        public BigDecimal getAvailableThisYear() { return availableThisYear; }
        public BigDecimal getUsedThisYear() { return usedThisYear; }
        public java.time.LocalDate getExpirationDate() { return expirationDate; }
        public Boolean getIsCarriedBack() { return isCarriedBack; }
        public BigDecimal getCarrybackAmount() { return carrybackAmount; }
        public BigDecimal getRemainingForFuture() { 
            return availableThisYear.subtract(usedThisYear); 
        }
    }
}
