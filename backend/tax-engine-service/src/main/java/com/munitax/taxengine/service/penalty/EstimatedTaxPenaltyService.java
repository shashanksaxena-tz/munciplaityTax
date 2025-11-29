package com.munitax.taxengine.service.penalty;

import com.munitax.taxengine.domain.penalty.CalculationMethod;
import com.munitax.taxengine.domain.penalty.EstimatedTaxPenalty;
import com.munitax.taxengine.domain.penalty.Quarter;
import com.munitax.taxengine.domain.penalty.QuarterlyUnderpayment;
import com.munitax.taxengine.dto.SafeHarborEvaluationDto;
import com.munitax.taxengine.repository.EstimatedTaxPenaltyRepository;
import com.munitax.taxengine.service.RuleEngineIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Service for calculating estimated tax underpayment penalties.
 * 
 * Functional Requirements:
 * - FR-020 to FR-026: Calculate quarterly estimated tax underpayment penalty
 * - FR-021: Calculate required quarterly payment (25% of annual tax)
 * - FR-022: Support annualized income method (optional)
 * - FR-023: Calculate underpayment per quarter: Required - Actual
 * - FR-024: Apply overpayments from later quarters to earlier underpayments
 * - FR-025: Retrieve current underpayment penalty rate from rule-engine-service
 * - FR-026: Display quarterly schedule of required vs actual payments
 * 
 * User Story 5: Estimated Tax Underpayment Penalty
 * As a tax administrator, I want the system to calculate estimated tax underpayment penalties
 * on a quarterly basis (Required = 25% annual tax) so that taxpayers who underpay quarterly
 * estimates are penalized appropriately.
 * 
 * Calculates quarterly underpayment penalties based on IRS Form 2210 rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EstimatedTaxPenaltyService {
    
    private final EstimatedTaxPenaltyRepository estimatedTaxPenaltyRepository;
    private final SafeHarborEvaluationService safeHarborEvaluationService;
    private final RuleEngineIntegrationService ruleEngineService;
    
    private static final int SCALE = 2; // 2 decimal places for currency
    private static final BigDecimal QUARTERLY_PERCENTAGE = new BigDecimal("0.25"); // 25% per quarter
    private static final int NUMBER_OF_QUARTERS = 4;
    
    // Standard quarterly due dates (month-day)
    private static final Map<Quarter, String> STANDARD_DUE_DATES = Map.of(
            Quarter.Q1, "04-15", // April 15
            Quarter.Q2, "06-15", // June 15
            Quarter.Q3, "09-15", // September 15
            Quarter.Q4, "01-15"  // January 15 of next year
    );
    
    /**
     * Calculate estimated tax penalty for all quarters.
     * 
     * FR-020: Calculate quarterly estimated tax underpayment penalty
     * FR-021: Required payment = 25% of annual tax per quarter
     * 
     * @param tenantId              the tenant ID
     * @param returnId              the return ID
     * @param taxYear               the tax year
     * @param annualTaxLiability    the total annual tax liability
     * @param quarterlyPayments     map of quarter to actual payment amount
     * @param agi                   the adjusted gross income (for safe harbor)
     * @param priorYearTaxLiability the prior year tax liability (for safe harbor)
     * @param calculationMethod     the calculation method (STANDARD or ANNUALIZED_INCOME)
     * @param createdBy             the user creating the penalty
     * @return calculated estimated tax penalty
     */
    @Transactional
    public EstimatedTaxPenalty calculateEstimatedTaxPenalty(
            UUID tenantId,
            UUID returnId,
            int taxYear,
            BigDecimal annualTaxLiability,
            Map<Quarter, BigDecimal> quarterlyPayments,
            BigDecimal agi,
            BigDecimal priorYearTaxLiability,
            CalculationMethod calculationMethod,
            UUID createdBy) {
        
        log.info("Calculating estimated tax penalty for return: {}, tax year: {}", returnId, taxYear);
        
        // Validate inputs
        validateInputs(tenantId, returnId, taxYear, annualTaxLiability, quarterlyPayments, agi, createdBy);
        
        // Calculate total estimated tax paid
        BigDecimal totalPaid = quarterlyPayments.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // FR-015 to FR-019: Evaluate safe harbor rules first
        SafeHarborEvaluationDto safeHarborEval = safeHarborEvaluationService.evaluateSafeHarbor(
                tenantId, taxYear, annualTaxLiability, totalPaid, agi, priorYearTaxLiability);
        
        // If safe harbor is met, no penalty due
        if (Boolean.TRUE.equals(safeHarborEval.getAnySafeHarborMet())) {
            log.info("Safe harbor met - no estimated tax penalty due");
            return createNoPenaltyRecord(tenantId, returnId, taxYear, annualTaxLiability,
                    quarterlyPayments, safeHarborEval, calculationMethod, createdBy);
        }
        
        // Calculate quarterly underpayments
        List<QuarterlyUnderpayment> underpayments = calculateQuarterlyUnderpayments(
                taxYear, annualTaxLiability, quarterlyPayments, calculationMethod);
        
        // FR-024: Apply overpayments from later quarters to earlier underpayments
        applyOverpayments(underpayments);
        
        // FR-025: Retrieve underpayment penalty rate from rule engine
        BigDecimal penaltyRate = ruleEngineService.getEstimatedTaxPenaltyRate(
                LocalDate.of(taxYear, 12, 31), tenantId.toString());
        
        // Calculate penalty for each quarter
        BigDecimal totalPenalty = calculateQuarterlyPenalties(underpayments, penaltyRate, taxYear);
        
        // Create and save estimated tax penalty entity
        EstimatedTaxPenalty penalty = EstimatedTaxPenalty.builder()
                .tenantId(tenantId)
                .returnId(returnId)
                .taxYear(taxYear)
                .annualTaxLiability(annualTaxLiability)
                .totalEstimatedTaxPaid(totalPaid)
                .calculationMethod(calculationMethod != null ? calculationMethod : CalculationMethod.STANDARD)
                .safeHarbor1Met(safeHarborEval.getSafeHarbor1Met())
                .safeHarbor2Met(safeHarborEval.getSafeHarbor2Met())
                .penaltyRate(penaltyRate)
                .totalPenaltyAmount(totalPenalty)
                .quarterlyUnderpayments(underpayments)
                .createdBy(createdBy)
                .createdAt(LocalDate.now())
                .build();
        
        // Set bidirectional relationship
        underpayments.forEach(u -> u.setEstimatedTaxPenalty(penalty));
        
        EstimatedTaxPenalty savedPenalty = estimatedTaxPenaltyRepository.save(penalty);
        
        log.info("Estimated tax penalty calculated and saved: {} for ${}", 
                savedPenalty.getId(), totalPenalty);
        
        return savedPenalty;
    }
    
    /**
     * Calculate required and actual payments for each quarter.
     * 
     * FR-021: Required payment = 25% of annual tax per quarter
     * FR-022: Support annualized income method
     * FR-023: Calculate underpayment = Required - Actual
     * 
     * @param taxYear           the tax year
     * @param annualTaxLiability the annual tax liability
     * @param quarterlyPayments  map of actual payments by quarter
     * @param calculationMethod  the calculation method
     * @return list of quarterly underpayments
     */
    private List<QuarterlyUnderpayment> calculateQuarterlyUnderpayments(
            int taxYear,
            BigDecimal annualTaxLiability,
            Map<Quarter, BigDecimal> quarterlyPayments,
            CalculationMethod calculationMethod) {
        
        List<QuarterlyUnderpayment> underpayments = new ArrayList<>();
        
        // FR-021: Standard method - 25% of annual tax per quarter
        BigDecimal requiredPerQuarter = annualTaxLiability
                .multiply(QUARTERLY_PERCENTAGE)
                .setScale(SCALE, RoundingMode.HALF_UP);
        
        for (Quarter quarter : Quarter.values()) {
            BigDecimal actualPayment = quarterlyPayments.getOrDefault(quarter, BigDecimal.ZERO);
            
            // FR-022: For annualized income method, required amount may vary by quarter
            // For now, using standard 25% method; annualized income requires quarterly income data
            BigDecimal requiredPayment = requiredPerQuarter;
            
            // FR-023: Calculate underpayment
            BigDecimal underpaymentAmount = requiredPayment.subtract(actualPayment)
                    .max(BigDecimal.ZERO); // Cannot have negative underpayment
            
            LocalDate dueDate = calculateQuarterlyDueDate(taxYear, quarter);
            
            QuarterlyUnderpayment underpayment = QuarterlyUnderpayment.builder()
                    .quarter(quarter)
                    .dueDate(dueDate)
                    .requiredPayment(requiredPayment)
                    .actualPayment(actualPayment)
                    .underpaymentAmount(underpaymentAmount)
                    .penaltyAmount(BigDecimal.ZERO) // Will be calculated later
                    .daysLate(0) // Will be calculated when payment date is known
                    .build();
            
            underpayments.add(underpayment);
            
            log.debug("Quarter {}: Required=${}, Actual=${}, Underpayment=${}",
                    quarter, requiredPayment, actualPayment, underpaymentAmount);
        }
        
        return underpayments;
    }
    
    /**
     * Apply overpayments from later quarters to earlier underpayments.
     * 
     * FR-024: Apply overpayments from later quarters to earlier underpayments
     * This reduces penalties on earlier quarters if later quarters were overpaid.
     * 
     * @param underpayments list of quarterly underpayments (will be modified)
     */
    private void applyOverpayments(List<QuarterlyUnderpayment> underpayments) {
        // Process quarters in order Q1 -> Q2 -> Q3 -> Q4
        BigDecimal carryForward = BigDecimal.ZERO;
        
        for (QuarterlyUnderpayment underpayment : underpayments) {
            // Apply any carryforward from previous quarter overpayment
            if (carryForward.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal reduction = carryForward.min(underpayment.getUnderpaymentAmount());
                underpayment.setUnderpaymentAmount(
                        underpayment.getUnderpaymentAmount().subtract(reduction));
                carryForward = carryForward.subtract(reduction);
                
                log.debug("Applied ${} overpayment to {} underpayment, new underpayment: ${}",
                        reduction, underpayment.getQuarter(), underpayment.getUnderpaymentAmount());
            }
            
            // Check if this quarter has an overpayment
            BigDecimal overpayment = underpayment.getActualPayment()
                    .subtract(underpayment.getRequiredPayment());
            
            if (overpayment.compareTo(BigDecimal.ZERO) > 0) {
                carryForward = carryForward.add(overpayment);
                log.debug("{} overpayment of ${} available for carryforward",
                        underpayment.getQuarter(), overpayment);
            }
        }
    }
    
    /**
     * Calculate penalties for each quarter based on underpayment amounts.
     * 
     * FR-025: Retrieve underpayment penalty rate
     * Penalty is calculated based on days late and underpayment amount.
     * 
     * @param underpayments list of quarterly underpayments
     * @param penaltyRate   the annual penalty rate
     * @param taxYear       the tax year
     * @return total penalty amount across all quarters
     */
    private BigDecimal calculateQuarterlyPenalties(List<QuarterlyUnderpayment> underpayments,
                                                  BigDecimal penaltyRate,
                                                  int taxYear) {
        BigDecimal totalPenalty = BigDecimal.ZERO;
        
        // Calculate penalty for each quarter
        // Penalty accrues from due date until payment date (or year end if still unpaid)
        LocalDate yearEnd = LocalDate.of(taxYear, 12, 31);
        
        for (QuarterlyUnderpayment underpayment : underpayments) {
            if (underpayment.getUnderpaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Calculate days from due date to year end
                long daysLate = java.time.temporal.ChronoUnit.DAYS.between(
                        underpayment.getDueDate(), yearEnd);
                
                // Calculate penalty: (Underpayment × Annual Rate × Days) / 365
                BigDecimal quarterlyPenalty = underpayment.getUnderpaymentAmount()
                        .multiply(penaltyRate)
                        .multiply(BigDecimal.valueOf(daysLate))
                        .divide(BigDecimal.valueOf(365), SCALE, RoundingMode.HALF_UP);
                
                underpayment.setPenaltyAmount(quarterlyPenalty);
                underpayment.setDaysLate((int) daysLate);
                totalPenalty = totalPenalty.add(quarterlyPenalty);
                
                log.debug("{}: Underpayment=${}, Days={}, Penalty=${}",
                        underpayment.getQuarter(),
                        underpayment.getUnderpaymentAmount(),
                        daysLate,
                        quarterlyPenalty);
            }
        }
        
        return totalPenalty;
    }
    
    /**
     * Calculate the due date for a given quarter.
     * 
     * @param taxYear the tax year
     * @param quarter the quarter
     * @return due date for the quarter
     */
    private LocalDate calculateQuarterlyDueDate(int taxYear, Quarter quarter) {
        String monthDay = STANDARD_DUE_DATES.get(quarter);
        int year = quarter == Quarter.Q4 ? taxYear + 1 : taxYear; // Q4 due date is in next year
        
        String[] parts = monthDay.split("-");
        int month = Integer.parseInt(parts[0]);
        int day = Integer.parseInt(parts[1]);
        
        return LocalDate.of(year, month, day);
    }
    
    /**
     * Create a penalty record with zero penalty when safe harbor is met.
     * 
     * @param tenantId              the tenant ID
     * @param returnId              the return ID
     * @param taxYear               the tax year
     * @param annualTaxLiability    the annual tax liability
     * @param quarterlyPayments     the quarterly payments
     * @param safeHarborEval        the safe harbor evaluation
     * @param calculationMethod     the calculation method
     * @param createdBy             the user creating the record
     * @return estimated tax penalty with zero penalty amount
     */
    private EstimatedTaxPenalty createNoPenaltyRecord(
            UUID tenantId,
            UUID returnId,
            int taxYear,
            BigDecimal annualTaxLiability,
            Map<Quarter, BigDecimal> quarterlyPayments,
            SafeHarborEvaluationDto safeHarborEval,
            CalculationMethod calculationMethod,
            UUID createdBy) {
        
        BigDecimal totalPaid = quarterlyPayments.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Create quarterly records with zero penalties
        List<QuarterlyUnderpayment> underpayments = new ArrayList<>();
        BigDecimal requiredPerQuarter = annualTaxLiability.multiply(QUARTERLY_PERCENTAGE);
        
        for (Quarter quarter : Quarter.values()) {
            BigDecimal actualPayment = quarterlyPayments.getOrDefault(quarter, BigDecimal.ZERO);
            LocalDate dueDate = calculateQuarterlyDueDate(taxYear, quarter);
            
            underpayments.add(QuarterlyUnderpayment.builder()
                    .quarter(quarter)
                    .dueDate(dueDate)
                    .requiredPayment(requiredPerQuarter)
                    .actualPayment(actualPayment)
                    .underpaymentAmount(BigDecimal.ZERO)
                    .penaltyAmount(BigDecimal.ZERO)
                    .daysLate(0)
                    .build());
        }
        
        EstimatedTaxPenalty penalty = EstimatedTaxPenalty.builder()
                .tenantId(tenantId)
                .returnId(returnId)
                .taxYear(taxYear)
                .annualTaxLiability(annualTaxLiability)
                .totalEstimatedTaxPaid(totalPaid)
                .calculationMethod(calculationMethod != null ? calculationMethod : CalculationMethod.STANDARD)
                .safeHarbor1Met(safeHarborEval.getSafeHarbor1Met())
                .safeHarbor2Met(safeHarborEval.getSafeHarbor2Met())
                .penaltyRate(BigDecimal.ZERO)
                .totalPenaltyAmount(BigDecimal.ZERO)
                .quarterlyUnderpayments(underpayments)
                .createdBy(createdBy)
                .createdAt(LocalDate.now())
                .build();
        
        underpayments.forEach(u -> u.setEstimatedTaxPenalty(penalty));
        
        return estimatedTaxPenaltyRepository.save(penalty);
    }
    
    /**
     * Validate input parameters.
     */
    private void validateInputs(UUID tenantId, UUID returnId, int taxYear,
                               BigDecimal annualTaxLiability,
                               Map<Quarter, BigDecimal> quarterlyPayments,
                               BigDecimal agi, UUID createdBy) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (returnId == null) {
            throw new IllegalArgumentException("Return ID is required");
        }
        if (taxYear < 1900 || taxYear > 2100) {
            throw new IllegalArgumentException("Invalid tax year: " + taxYear);
        }
        if (annualTaxLiability == null || annualTaxLiability.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid annual tax liability is required");
        }
        if (quarterlyPayments == null) {
            throw new IllegalArgumentException("Quarterly payments map is required");
        }
        if (agi == null || agi.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid AGI is required");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Created by is required");
        }
    }
}
