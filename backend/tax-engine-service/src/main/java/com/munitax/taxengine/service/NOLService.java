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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Net Operating Loss (NOL) operations.
 * 
 * Core Functions:
 * - FR-001: Create and track NOLs across multiple years
 * - FR-003: Calculate available NOL balance
 * - FR-008: Calculate maximum NOL deduction (80% limitation)
 * - FR-016: Process NOL carryback elections
 * - FR-022: Apply FIFO ordering for NOL usage
 * - FR-024: Generate expiration alerts
 * - FR-032: Calculate state NOL with apportionment
 * - FR-040: Handle amended return NOL recalculation
 * 
 * Business Rules:
 * - Post-2017: 80% taxable income limitation, indefinite carryforward
 * - Pre-2018: 100% offset allowed, 20-year carryforward
 * - CARES Act: 2018-2020 losses can be carried back 5 years
 * - FIFO ordering: Use oldest NOLs first to prevent expiration
 * 
 * @see NOL
 * @see NOLUsage
 * @see NOLCarryback
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NOLService {
    
    private final NOLRepository nolRepository;
    private final NOLUsageRepository nolUsageRepository;
    private final NOLCarrybackRepository nolCarrybackRepository;
    private final NOLScheduleRepository nolScheduleRepository;
    private final NOLExpirationAlertRepository nolExpirationAlertRepository;
    private final NOLAmendmentRepository nolAmendmentRepository;
    
    // Tax year when TCJA took effect (80% limitation)
    private static final int TCJA_EFFECTIVE_YEAR = 2018;
    
    // CARES Act carryback eligible years
    private static final int CARES_ACT_START_YEAR = 2018;
    private static final int CARES_ACT_END_YEAR = 2020;
    private static final int CARES_ACT_CARRYBACK_YEARS = 5;
    
    // Pre-TCJA carryforward period
    private static final int PRE_TCJA_CARRYFORWARD_YEARS = 20;
    
    // Limitation percentages
    private static final BigDecimal POST_TCJA_LIMITATION = new BigDecimal("80.00");
    private static final BigDecimal PRE_TCJA_LIMITATION = new BigDecimal("100.00");
    
    /**
     * Create a new NOL record for a business with a net operating loss.
     * 
     * Process:
     * 1. Validate loss amount > 0
     * 2. Determine expiration date based on tax year and jurisdiction
     * 3. Calculate apportionment if multi-state
     * 4. Create NOL record with initial balance = original amount
     * 5. Generate expiration alert if applicable
     * 6. Log audit trail
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year when loss occurred
     * @param lossAmount Net operating loss amount (positive number)
     * @param jurisdiction Tax jurisdiction (FEDERAL, STATE_OHIO, MUNICIPALITY)
     * @param entityType Entity type (C_CORP, S_CORP, PARTNERSHIP, SOLE_PROP)
     * @param apportionmentPercentage Ohio apportionment % (null for federal)
     * @param municipalityCode Municipality code (if MUNICIPALITY jurisdiction)
     * @param userId User creating the NOL
     * @param tenantId Tenant ID
     * @return Created NOL entity
     */
    @Transactional
    public NOL createNOL(UUID businessId, Integer taxYear, BigDecimal lossAmount,
                         Jurisdiction jurisdiction, EntityType entityType,
                         BigDecimal apportionmentPercentage, String municipalityCode,
                         UUID userId, UUID tenantId) {
        
        log.info("Creating NOL for business {} tax year {} amount {} jurisdiction {}",
                 businessId, taxYear, lossAmount, jurisdiction);
        
        if (lossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loss amount must be positive");
        }
        
        // Calculate expiration date
        LocalDate expirationDate = calculateExpirationDate(taxYear, jurisdiction);
        Integer carryforwardYears = calculateCarryforwardYears(taxYear, jurisdiction);
        
        // Apply apportionment for state NOLs
        BigDecimal nolAmount = lossAmount;
        if (jurisdiction == Jurisdiction.STATE_OHIO && apportionmentPercentage != null) {
            nolAmount = lossAmount.multiply(apportionmentPercentage)
                                  .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            log.info("Applied {}% apportionment to federal NOL {} = state NOL {}",
                     apportionmentPercentage, lossAmount, nolAmount);
        }
        
        // Create NOL entity
        NOL nol = NOL.builder()
                .tenantId(tenantId)
                .businessId(businessId)
                .taxYear(taxYear)
                .jurisdiction(jurisdiction)
                .municipalityCode(municipalityCode)
                .entityType(entityType)
                .originalNOLAmount(nolAmount)
                .currentNOLBalance(nolAmount)
                .usedAmount(BigDecimal.ZERO)
                .expiredAmount(BigDecimal.ZERO)
                .expirationDate(expirationDate)
                .carryforwardYears(carryforwardYears)
                .isCarriedBack(false)
                .carrybackAmount(BigDecimal.ZERO)
                .carrybackRefund(BigDecimal.ZERO)
                .apportionmentPercentage(apportionmentPercentage)
                .createdBy(userId)
                .build();
        
        nol = nolRepository.save(nol);
        
        // Generate expiration alert if NOL has expiration date
        if (expirationDate != null) {
            generateExpirationAlert(nol, tenantId);
        }
        
        log.info("Created NOL {} for business {} with balance {}", 
                 nol.getId(), businessId, nolAmount);
        
        return nol;
    }
    
    /**
     * Calculate available NOL balance for a business in current year.
     * 
     * Process:
     * 1. Retrieve all NOLs with remaining balance
     * 2. Filter out expired NOLs
     * 3. Sum remaining balances
     * 
     * @param businessId Business profile ID
     * @param jurisdiction Filter by jurisdiction (null for all)
     * @return Total available NOL balance
     */
    public BigDecimal calculateAvailableNOLBalance(UUID businessId, Jurisdiction jurisdiction) {
        List<NOL> nols;
        
        if (jurisdiction != null) {
            nols = nolRepository.findByBusinessIdAndJurisdictionOrderByTaxYearAsc(businessId, jurisdiction);
        } else {
            nols = nolRepository.findAvailableNOLsByBusinessId(businessId);
        }
        
        BigDecimal totalBalance = nols.stream()
            .filter(NOL::hasRemainingBalance)
            .map(NOL::getCurrentNOLBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Available NOL balance for business {} jurisdiction {}: {}",
                 businessId, jurisdiction, totalBalance);
        
        return totalBalance;
    }
    
    /**
     * Calculate maximum NOL deduction for current year.
     * 
     * Applies 80% limitation for post-2017 tax years (FR-008).
     * 
     * @param taxableIncomeBeforeNOL Taxable income before NOL deduction
     * @param availableNOLBalance Total available NOL balance
     * @param taxYear Current tax year
     * @return Maximum allowable NOL deduction
     */
    public BigDecimal calculateMaximumNOLDeduction(BigDecimal taxableIncomeBeforeNOL,
                                                    BigDecimal availableNOLBalance,
                                                    Integer taxYear) {
        
        // Determine limitation percentage based on tax year
        BigDecimal limitationPercentage = taxYear >= TCJA_EFFECTIVE_YEAR ?
                                         POST_TCJA_LIMITATION : PRE_TCJA_LIMITATION;
        
        // Calculate maximum based on limitation
        BigDecimal maxByLimitation = taxableIncomeBeforeNOL
            .multiply(limitationPercentage)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        // Maximum deduction is lesser of available balance and limitation
        BigDecimal maxDeduction = maxByLimitation.min(availableNOLBalance);
        
        log.info("Max NOL deduction for tax year {}: income {} × {}% = {}, available {}, max deduction {}",
                 taxYear, taxableIncomeBeforeNOL, limitationPercentage, maxByLimitation,
                 availableNOLBalance, maxDeduction);
        
        return maxDeduction;
    }
    
    /**
     * Apply NOL deduction to current year return.
     * 
     * Process:
     * 1. Get available NOLs ordered by tax year (FIFO)
     * 2. Calculate maximum deduction (80% limitation)
     * 3. Apply NOLs oldest first until deduction amount reached
     * 4. Create NOLUsage records
     * 5. Update NOL balances
     * 6. Update expiration alerts
     * 
     * @param businessId Business profile ID
     * @param returnId Tax return ID
     * @param taxYear Current tax year
     * @param taxableIncomeBeforeNOL Taxable income before NOL
     * @param nolDeductionAmount Desired NOL deduction (must be ≤ max)
     * @param taxRate Tax rate for savings calculation
     * @param jurisdiction Tax jurisdiction
     * @param tenantId Tenant ID
     * @return List of NOLUsage records created
     */
    @Transactional
    public List<NOLUsage> applyNOLDeduction(UUID businessId, UUID returnId, Integer taxYear,
                                            BigDecimal taxableIncomeBeforeNOL,
                                            BigDecimal nolDeductionAmount, BigDecimal taxRate,
                                            Jurisdiction jurisdiction, UUID tenantId) {
        
        log.info("Applying NOL deduction for business {} tax year {} amount {}",
                 businessId, taxYear, nolDeductionAmount);
        
        // Validate deduction amount
        BigDecimal availableBalance = calculateAvailableNOLBalance(businessId, jurisdiction);
        BigDecimal maxDeduction = calculateMaximumNOLDeduction(taxableIncomeBeforeNOL, availableBalance, taxYear);
        
        if (nolDeductionAmount.compareTo(maxDeduction) > 0) {
            throw new IllegalArgumentException(
                String.format("NOL deduction %s exceeds maximum %s", nolDeductionAmount, maxDeduction)
            );
        }
        
        // Get available NOLs in FIFO order
        List<NOL> availableNOLs = jurisdiction != null ?
            nolRepository.findByBusinessIdAndJurisdictionOrderByTaxYearAsc(businessId, jurisdiction) :
            nolRepository.findAvailableNOLsByBusinessId(businessId);
        
        availableNOLs = availableNOLs.stream()
            .filter(NOL::hasRemainingBalance)
            .collect(Collectors.toList());
        
        // Apply NOLs oldest first
        List<NOLUsage> usages = new ArrayList<>();
        BigDecimal remainingDeduction = nolDeductionAmount;
        BigDecimal limitationPercentage = taxYear >= TCJA_EFFECTIVE_YEAR ?
                                         POST_TCJA_LIMITATION : PRE_TCJA_LIMITATION;
        
        for (NOL nol : availableNOLs) {
            if (remainingDeduction.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            // Use up to remaining balance of this NOL
            BigDecimal amountToUse = remainingDeduction.min(nol.getCurrentNOLBalance());
            
            // Create usage record
            NOLUsage usage = NOLUsage.builder()
                .tenantId(tenantId)
                .nolId(nol.getId())
                .returnId(returnId)
                .usageYear(taxYear)
                .taxableIncomeBeforeNOL(taxableIncomeBeforeNOL)
                .nolLimitationPercentage(limitationPercentage)
                .maximumNOLDeduction(maxDeduction)
                .actualNOLDeduction(amountToUse)
                .taxableIncomeAfterNOL(taxableIncomeBeforeNOL.subtract(nolDeductionAmount))
                .taxSavings(amountToUse.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orderingMethod(NOLOrderingMethod.FIFO)
                .build();
            
            usage = nolUsageRepository.save(usage);
            usages.add(usage);
            
            // Update NOL balance
            nol.setCurrentNOLBalance(nol.getCurrentNOLBalance().subtract(amountToUse));
            nol.setUsedAmount(nol.getUsedAmount().add(amountToUse));
            nolRepository.save(nol);
            
            // Update expiration alert if exists
            updateExpirationAlert(nol, tenantId);
            
            remainingDeduction = remainingDeduction.subtract(amountToUse);
            
            log.info("Applied {} from NOL {} (year {}) to return {}", 
                     amountToUse, nol.getId(), nol.getTaxYear(), returnId);
        }
        
        log.info("Applied total NOL deduction {} using {} NOL vintages",
                 nolDeductionAmount, usages.size());
        
        return usages;
    }
    
    /**
     * Calculate expiration date for NOL based on tax year and jurisdiction.
     * 
     * Rules:
     * - Pre-2018 federal NOLs: tax_year + 20 years
     * - Post-2017 federal NOLs: null (indefinite)
     * - State NOLs: Follow state rules (Ohio = indefinite post-2017)
     * 
     * @param taxYear Tax year when NOL originated
     * @param jurisdiction Tax jurisdiction
     * @return Expiration date (null if indefinite)
     */
    private LocalDate calculateExpirationDate(Integer taxYear, Jurisdiction jurisdiction) {
        if (taxYear >= TCJA_EFFECTIVE_YEAR) {
            // Post-TCJA: indefinite carryforward for federal and Ohio
            return null;
        } else {
            // Pre-TCJA: 20-year carryforward
            return LocalDate.of(taxYear + PRE_TCJA_CARRYFORWARD_YEARS, 12, 31);
        }
    }
    
    /**
     * Calculate carryforward years for NOL.
     * 
     * @param taxYear Tax year when NOL originated
     * @param jurisdiction Tax jurisdiction
     * @return Number of carryforward years (null if indefinite)
     */
    private Integer calculateCarryforwardYears(Integer taxYear, Jurisdiction jurisdiction) {
        if (taxYear >= TCJA_EFFECTIVE_YEAR) {
            return null; // Indefinite
        } else {
            return PRE_TCJA_CARRYFORWARD_YEARS;
        }
    }
    
    /**
     * Generate expiration alert for NOL if expiring within 3 years.
     * 
     * @param nol NOL entity
     * @param tenantId Tenant ID
     */
    private void generateExpirationAlert(NOL nol, UUID tenantId) {
        if (nol.getExpirationDate() == null) {
            return; // No expiration for indefinite NOLs
        }
        
        BigDecimal yearsUntilExpiration = NOLExpirationAlert.calculateYearsUntilExpiration(nol.getExpirationDate());
        
        // Only create alerts for NOLs expiring within 3 years
        if (yearsUntilExpiration.compareTo(BigDecimal.valueOf(3)) > 0) {
            return;
        }
        
        AlertSeverityLevel severity = NOLExpirationAlert.determineSeverityLevel(yearsUntilExpiration);
        String message = String.format("⚠️ $%s NOL from %d expiring %s - Use before expiration or lose!",
                                      nol.getCurrentNOLBalance(), nol.getTaxYear(), nol.getExpirationDate());
        
        NOLExpirationAlert alert = NOLExpirationAlert.builder()
            .tenantId(tenantId)
            .businessId(nol.getBusinessId())
            .nolId(nol.getId())
            .taxYear(nol.getTaxYear())
            .nolBalance(nol.getCurrentNOLBalance())
            .expirationDate(nol.getExpirationDate())
            .yearsUntilExpiration(yearsUntilExpiration)
            .severityLevel(severity)
            .alertMessage(message)
            .dismissed(false)
            .build();
        
        nolExpirationAlertRepository.save(alert);
        
        log.info("Generated {} expiration alert for NOL {} expiring {}",
                 severity, nol.getId(), nol.getExpirationDate());
    }
    
    /**
     * Update expiration alert when NOL balance changes.
     * 
     * @param nol NOL entity with updated balance
     * @param tenantId Tenant ID
     */
    private void updateExpirationAlert(NOL nol, UUID tenantId) {
        List<NOLExpirationAlert> alerts = nolExpirationAlertRepository.findByNolId(nol.getId());
        
        for (NOLExpirationAlert alert : alerts) {
            if (!alert.getDismissed()) {
                alert.setNolBalance(nol.getCurrentNOLBalance());
                nolExpirationAlertRepository.save(alert);
            }
        }
    }
}
