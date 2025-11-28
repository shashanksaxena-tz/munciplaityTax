package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.nol.*;
import com.munitax.taxengine.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Service for NOL Carryback operations (CARES Act provision).
 * 
 * Core Functions:
 * - FR-013: Support NOL carryback election for 2018-2020 losses
 * - FR-014: Allow carryback or waive carryback election
 * - FR-015: Retrieve prior 5 years of tax returns
 * - FR-016: Calculate carryback using FIFO ordering
 * - FR-017: Calculate refund amount
 * - FR-018: Generate Form 27-NOL-CB
 * - FR-019: Update NOL schedule with carryback
 * - FR-020: Support state-specific carryback rules
 * 
 * @see NOLCarryback
 * @see NOL
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NOLCarrybackService {
    
    private final NOLRepository nolRepository;
    private final NOLCarrybackRepository nolCarrybackRepository;
    
    // CARES Act carryback parameters
    private static final int CARES_ACT_START_YEAR = 2018;
    private static final int CARES_ACT_END_YEAR = 2020;
    private static final int CARES_ACT_CARRYBACK_YEARS = 5;
    
    /**
     * Check if NOL is eligible for CARES Act carryback.
     * 
     * Eligible: 2018, 2019, 2020 tax year losses
     * 
     * @param taxYear Tax year when NOL originated
     * @return true if eligible for carryback
     */
    public boolean isEligibleForCarryback(Integer taxYear) {
        return taxYear >= CARES_ACT_START_YEAR && taxYear <= CARES_ACT_END_YEAR;
    }
    
    /**
     * Process NOL carryback election for CARES Act eligible NOL.
     * 
     * Process:
     * 1. Validate NOL is eligible for carryback (2018-2020)
     * 2. Retrieve prior 5 years of tax returns with taxable income
     * 3. Apply NOL to prior years oldest first (FIFO)
     * 4. Calculate refund for each year: NOL applied × tax rate
     * 5. Create NOLCarryback records
     * 6. Update NOL with carryback amount and refund
     * 7. Generate Form 27-NOL-CB
     * 
     * @param nolId NOL ID to carry back
     * @param priorYearData Map of year → {taxableIncome, taxRate, taxPaid, returnId}
     * @param tenantId Tenant ID
     * @return List of carryback records created
     * @throws IllegalArgumentException if NOL not eligible or insufficient data
     */
    @Transactional
    public List<NOLCarryback> processCarrybackElection(UUID nolId,
                                                        Map<Integer, PriorYearData> priorYearData,
                                                        UUID tenantId) {
        
        log.info("Processing carryback election for NOL {}", nolId);
        
        // Retrieve NOL
        NOL nol = nolRepository.findById(nolId)
            .orElseThrow(() -> new IllegalArgumentException("NOL not found: " + nolId));
        
        // Validate eligibility
        if (!isEligibleForCarryback(nol.getTaxYear())) {
            throw new IllegalArgumentException(
                String.format("NOL from tax year %d is not eligible for CARES Act carryback (must be 2018-2020)",
                             nol.getTaxYear())
            );
        }
        
        if (nol.getIsCarriedBack()) {
            throw new IllegalArgumentException("NOL has already been carried back");
        }
        
        // Sort prior years oldest first (FIFO ordering)
        List<Integer> priorYears = new ArrayList<>(priorYearData.keySet());
        Collections.sort(priorYears);
        
        // Apply NOL to prior years
        List<NOLCarryback> carrybacks = new ArrayList<>();
        BigDecimal remainingNOL = nol.getCurrentNOLBalance();
        BigDecimal totalRefund = BigDecimal.ZERO;
        
        for (Integer priorYear : priorYears) {
            if (remainingNOL.compareTo(BigDecimal.ZERO) <= 0) {
                break; // All NOL has been used
            }
            
            // Validate year is within 5-year carryback period
            if (nol.getTaxYear() - priorYear > CARES_ACT_CARRYBACK_YEARS) {
                log.warn("Skipping year {} - beyond 5-year carryback period for NOL year {}",
                        priorYear, nol.getTaxYear());
                continue;
            }
            
            PriorYearData yearData = priorYearData.get(priorYear);
            
            // Skip years with no taxable income
            if (yearData.getTaxableIncome().compareTo(BigDecimal.ZERO) <= 0) {
                log.info("Skipping year {} - no taxable income to offset", priorYear);
                continue;
            }
            
            // Calculate NOL to apply (limited to prior year taxable income)
            BigDecimal nolToApply = remainingNOL.min(yearData.getTaxableIncome());
            
            // Calculate refund: NOL applied × prior year tax rate
            // Cap refund at actual tax paid in prior year
            BigDecimal calculatedRefund = nolToApply
                .multiply(yearData.getTaxRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal refundAmount = calculatedRefund.min(yearData.getTaxPaid());
            
            // Create carryback record
            NOLCarryback carryback = NOLCarryback.builder()
                .tenantId(tenantId)
                .nolId(nol.getId())
                .carrybackYear(priorYear)
                .priorYearTaxableIncome(yearData.getTaxableIncome())
                .nolApplied(nolToApply)
                .priorYearTaxRate(yearData.getTaxRate())
                .refundAmount(refundAmount)
                .priorYearReturnId(yearData.getReturnId())
                .refundStatus(RefundStatus.CLAIMED)
                .filedDate(LocalDate.now())
                .build();
            
            carryback = nolCarrybackRepository.save(carryback);
            carrybacks.add(carryback);
            
            remainingNOL = remainingNOL.subtract(nolToApply);
            totalRefund = totalRefund.add(refundAmount);
            
            log.info("Carried back {} to year {} with refund {}", nolToApply, priorYear, refundAmount);
        }
        
        // Update NOL with carryback totals
        BigDecimal totalCarriedBack = nol.getCurrentNOLBalance().subtract(remainingNOL);
        nol.setIsCarriedBack(true);
        nol.setCarrybackAmount(totalCarriedBack);
        nol.setCarrybackRefund(totalRefund);
        nol.setCurrentNOLBalance(remainingNOL);
        nolRepository.save(nol);
        
        log.info("Completed carryback election: carried back {} with total refund {} from NOL {}",
                 totalCarriedBack, totalRefund, nol.getId());
        
        return carrybacks;
    }
    
    /**
     * Retrieve carryback summary for NOL.
     * 
     * @param nolId NOL ID
     * @return List of carryback records with refund details
     */
    public List<NOLCarryback> getCarrybackSummary(UUID nolId) {
        return nolCarrybackRepository.findByNolIdOrderByCarrybackYearAsc(nolId);
    }
    
    /**
     * Update carryback refund status.
     * 
     * @param carrybackId Carryback record ID
     * @param newStatus New refund status
     * @param refundDate Date refund was received (if PAID)
     */
    @Transactional
    public void updateCarrybackStatus(UUID carrybackId, RefundStatus newStatus, LocalDate refundDate) {
        NOLCarryback carryback = nolCarrybackRepository.findById(carrybackId)
            .orElseThrow(() -> new IllegalArgumentException("Carryback not found: " + carrybackId));
        
        carryback.setRefundStatus(newStatus);
        if (newStatus == RefundStatus.PAID && refundDate != null) {
            carryback.setRefundDate(refundDate);
        }
        
        nolCarrybackRepository.save(carryback);
        
        log.info("Updated carryback {} status to {}", carrybackId, newStatus);
    }
    
    /**
     * Data class for prior year tax information.
     */
    public static class PriorYearData {
        private final BigDecimal taxableIncome;
        private final BigDecimal taxRate;
        private final BigDecimal taxPaid;
        private final UUID returnId;
        
        public PriorYearData(BigDecimal taxableIncome, BigDecimal taxRate, 
                            BigDecimal taxPaid, UUID returnId) {
            this.taxableIncome = taxableIncome;
            this.taxRate = taxRate;
            this.taxPaid = taxPaid;
            this.returnId = returnId;
        }
        
        public BigDecimal getTaxableIncome() { return taxableIncome; }
        public BigDecimal getTaxRate() { return taxRate; }
        public BigDecimal getTaxPaid() { return taxPaid; }
        public UUID getReturnId() { return returnId; }
    }
}
