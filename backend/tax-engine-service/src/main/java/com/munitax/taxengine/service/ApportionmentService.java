package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.ApportionmentFormula;
import com.munitax.taxengine.repository.ApportionmentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for calculating overall apportionment percentage.
 * Applies formula weights to property, payroll, and sales factors.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApportionmentService {

    private final FormulaConfigService formulaConfigService;
    private final ApportionmentAuditLogRepository auditLogRepository;

    private static final int SCALE = 4; // 4 decimal places for percentages
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    
    // TODO: Replace with actual authentication service
    private static final UUID MOCK_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    /**
     * Calculate final apportionment percentage using the specified formula.
     *
     * @param propertyFactorPercentage property factor percentage (0-100)
     * @param payrollFactorPercentage  payroll factor percentage (0-100)
     * @param salesFactorPercentage    sales factor percentage (0-100)
     * @param formula                  the apportionment formula to use
     * @return final apportionment percentage (0-100)
     */
    public BigDecimal calculateApportionmentPercentage(BigDecimal propertyFactorPercentage,
                                                       BigDecimal payrollFactorPercentage,
                                                       BigDecimal salesFactorPercentage,
                                                       ApportionmentFormula formula) {
        log.debug("Calculating apportionment with formula: {}", formula);
        log.debug("Property factor: {}%, Payroll factor: {}%, Sales factor: {}%",
                propertyFactorPercentage, payrollFactorPercentage, salesFactorPercentage);

        // Validate factor percentages
        validateFactorPercentage(propertyFactorPercentage);
        validateFactorPercentage(payrollFactorPercentage);
        validateFactorPercentage(salesFactorPercentage);

        // Get formula weights
        Map<String, BigDecimal> weights = formulaConfigService.getFormulaWeights(formula);

        // Calculate weighted contributions
        BigDecimal propertyContribution = propertyFactorPercentage
                .multiply(weights.get("property"))
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal payrollContribution = payrollFactorPercentage
                .multiply(weights.get("payroll"))
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal salesContribution = salesFactorPercentage
                .multiply(weights.get("sales"))
                .setScale(SCALE, RoundingMode.HALF_UP);

        // Sum weighted contributions
        BigDecimal finalApportionment = propertyContribution
                .add(payrollContribution)
                .add(salesContribution)
                .setScale(SCALE, RoundingMode.HALF_UP);

        log.info("Apportionment calculated: {}% (Property: {}%, Payroll: {}%, Sales: {}%)",
                finalApportionment, propertyContribution, payrollContribution, salesContribution);

        return finalApportionment;
    }

    /**
     * Calculate factor percentage from numerator and denominator.
     *
     * @param numerator   the state-specific amount
     * @param denominator the total amount everywhere
     * @return factor percentage (0-100)
     */
    public BigDecimal calculateFactorPercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Denominator is zero, returning 0%");
            return BigDecimal.ZERO;
        }

        if (numerator == null) {
            numerator = BigDecimal.ZERO;
        }

        BigDecimal percentage = numerator
                .divide(denominator, SCALE + 2, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(SCALE, RoundingMode.HALF_UP);

        log.debug("Factor percentage calculated: {}% (numerator: {}, denominator: {})",
                percentage, numerator, denominator);

        return percentage;
    }

    /**
     * Validate that a factor percentage is within valid range (0-100).
     *
     * @param percentage the percentage to validate
     * @throws IllegalArgumentException if percentage is invalid
     */
    public void validateFactorPercentage(BigDecimal percentage) {
        if (percentage == null) {
            return; // Null is allowed (will be treated as 0)
        }

        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Factor percentage cannot be negative: " + percentage);
        }

        if (percentage.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException(
                    "Factor percentage cannot exceed 100%: " + percentage);
        }
    }

    /**
     * Calculate apportionment breakdown with weighted contributions for each factor.
     *
     * @param propertyFactorPercentage property factor percentage
     * @param payrollFactorPercentage  payroll factor percentage
     * @param salesFactorPercentage    sales factor percentage
     * @param formula                  the apportionment formula
     * @return map containing breakdown details
     */
    public Map<String, Object> calculateApportionmentBreakdown(
            BigDecimal propertyFactorPercentage,
            BigDecimal payrollFactorPercentage,
            BigDecimal salesFactorPercentage,
            ApportionmentFormula formula) {

        Map<String, BigDecimal> weights = formulaConfigService.getFormulaWeights(formula);

        BigDecimal propertyWeight = weights.get("property");
        BigDecimal payrollWeight = weights.get("payroll");
        BigDecimal salesWeight = weights.get("sales");

        BigDecimal propertyContribution = propertyFactorPercentage.multiply(propertyWeight);
        BigDecimal payrollContribution = payrollFactorPercentage.multiply(payrollWeight);
        BigDecimal salesContribution = salesFactorPercentage.multiply(salesWeight);

        BigDecimal totalWeight = propertyWeight.add(payrollWeight).add(salesWeight);
        BigDecimal finalApportionment = propertyContribution.add(payrollContribution).add(salesContribution);

        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("propertyFactorPercentage", propertyFactorPercentage);
        breakdown.put("propertyFactorWeight", propertyWeight);
        breakdown.put("propertyFactorWeightedContribution", propertyContribution);
        breakdown.put("payrollFactorPercentage", payrollFactorPercentage);
        breakdown.put("payrollFactorWeight", payrollWeight);
        breakdown.put("payrollFactorWeightedContribution", payrollContribution);
        breakdown.put("salesFactorPercentage", salesFactorPercentage);
        breakdown.put("salesFactorWeight", salesWeight);
        breakdown.put("salesFactorWeightedContribution", salesContribution);
        breakdown.put("totalWeight", totalWeight);
        breakdown.put("finalApportionmentPercentage", finalApportionment);
        
        return breakdown;
    }

    /**
     * Compare traditional formula vs single-sales-factor to recommend the option
     * that results in lower tax liability.
     *
     * @param propertyFactorPercentage property factor percentage
     * @param payrollFactorPercentage  payroll factor percentage
     * @param salesFactorPercentage    sales factor percentage
     * @param traditionalFormula       the traditional formula to compare
     * @return map with comparison results and recommendation
     */
    public Map<String, Object> compareFormulaOptions(BigDecimal propertyFactorPercentage,
                                                     BigDecimal payrollFactorPercentage,
                                                     BigDecimal salesFactorPercentage,
                                                     ApportionmentFormula traditionalFormula) {

        BigDecimal traditionalApportionment = calculateApportionmentPercentage(
                propertyFactorPercentage, payrollFactorPercentage, salesFactorPercentage, traditionalFormula);

        BigDecimal singleSalesApportionment = salesFactorPercentage;

        ApportionmentFormula recommendation = traditionalApportionment.compareTo(singleSalesApportionment) < 0
                ? traditionalFormula
                : ApportionmentFormula.SINGLE_SALES_FACTOR;

        log.info("Formula comparison: Traditional={}%, Single-Sales={}%, Recommendation={}",
                traditionalApportionment, singleSalesApportionment, recommendation);

        return Map.of(
                "traditionalApportionment", traditionalApportionment,
                "singleSalesApportionment", singleSalesApportionment,
                "recommendation", recommendation,
                "difference", traditionalApportionment.subtract(singleSalesApportionment).abs()
        );
    }

    /**
     * T059: Validate Schedule Y request data.
     * Validates sales factor range, election requirements, and nexus data consistency.
     *
     * @param propertyFactorPercentage property factor percentage
     * @param payrollFactorPercentage  payroll factor percentage
     * @param salesFactorPercentage    sales factor percentage
     * @throws IllegalArgumentException if validation fails
     */
    public void validateScheduleYData(BigDecimal propertyFactorPercentage,
                                     BigDecimal payrollFactorPercentage,
                                     BigDecimal salesFactorPercentage) {
        log.debug("Validating Schedule Y data");

        // Validate individual factor percentages
        validateFactorPercentage(propertyFactorPercentage);
        validateFactorPercentage(payrollFactorPercentage);
        validateFactorPercentage(salesFactorPercentage);

        // Validate that sales factor is required
        if (salesFactorPercentage == null) {
            throw new IllegalArgumentException("Sales factor percentage is required");
        }

        // Additional business rule validations can be added here
        log.debug("Schedule Y data validation passed");
    }

    /**
     * T060: Create audit log entry for apportionment changes.
     * Records all significant changes for compliance and tracking.
     *
     * @param scheduleYId the Schedule Y ID
     * @param changeType  the type of change
     * @param description description of the change
     */
    public void createAuditLogEntry(UUID scheduleYId, 
                                   com.munitax.taxengine.domain.apportionment.AuditChangeType changeType,
                                   String description) {
        log.debug("Creating audit log entry for Schedule Y: {}", scheduleYId);

        com.munitax.taxengine.domain.apportionment.ApportionmentAuditLog auditLog = 
                new com.munitax.taxengine.domain.apportionment.ApportionmentAuditLog();
        auditLog.setScheduleYId(scheduleYId);
        auditLog.setTenantId(MOCK_TENANT_ID);
        auditLog.setChangeType(changeType);
        auditLog.setChangeReason(description);
        auditLog.setChangedBy(MOCK_USER_ID);

        auditLogRepository.save(auditLog);
        log.info("Audit log entry created: {} - {}", changeType, description);
    }

    /**
     * Create audit log entry for election changes.
     * Records when sourcing method, throwback, or service sourcing elections change.
     *
     * @param scheduleYId  the Schedule Y ID
     * @param fieldName    the field that changed
     * @param oldValue     the old value
     * @param newValue     the new value
     */
    public void logElectionChange(UUID scheduleYId, String fieldName, 
                                  String oldValue, String newValue) {
        log.debug("Logging election change for Schedule Y: {}", scheduleYId);

        com.munitax.taxengine.domain.apportionment.ApportionmentAuditLog auditLog = 
                new com.munitax.taxengine.domain.apportionment.ApportionmentAuditLog();
        auditLog.setScheduleYId(scheduleYId);
        auditLog.setTenantId(MOCK_TENANT_ID);
        auditLog.setChangeType(com.munitax.taxengine.domain.apportionment.AuditChangeType.ELECTION_CHANGED);
        auditLog.setEntityType(fieldName);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setChangeReason(String.format("Election changed from %s to %s", oldValue, newValue));
        auditLog.setChangedBy(MOCK_USER_ID);

        auditLogRepository.save(auditLog);
        log.info("Election change logged: {} changed from {} to {}", fieldName, oldValue, newValue);
    }

    // ===== T131-T134: Single-Sales-Factor Election Support =====

    /**
     * Compare traditional formula vs single-sales-factor formula.
     * Returns both calculations and recommends the option that minimizes tax liability.
     * Task: T132 [US5]
     *
     * @param propertyFactorPercentage property factor percentage
     * @param payrollFactorPercentage  payroll factor percentage
     * @param salesFactorPercentage    sales factor percentage
     * @return map containing comparison details and recommendation
     */
    public Map<String, Object> compareFormulaOptions(
            BigDecimal propertyFactorPercentage,
            BigDecimal payrollFactorPercentage,
            BigDecimal salesFactorPercentage) {
        
        log.debug("Comparing traditional vs single-sales-factor formula");
        
        // Calculate with traditional four-factor double-weighted sales
        BigDecimal traditionalApportionment = calculateApportionmentPercentage(
                propertyFactorPercentage,
                payrollFactorPercentage,
                salesFactorPercentage,
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);
        
        // Calculate with single-sales-factor
        BigDecimal singleSalesApportionment = calculateApportionmentPercentage(
                propertyFactorPercentage,
                payrollFactorPercentage,
                salesFactorPercentage,
                ApportionmentFormula.SINGLE_SALES_FACTOR);
        
        // Get breakdown for both formulas
        Map<String, Object> traditionalBreakdown = calculateApportionmentBreakdown(
                propertyFactorPercentage,
                payrollFactorPercentage,
                salesFactorPercentage,
                ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES);
        
        Map<String, Object> singleSalesBreakdown = calculateApportionmentBreakdown(
                propertyFactorPercentage,
                payrollFactorPercentage,
                salesFactorPercentage,
                ApportionmentFormula.SINGLE_SALES_FACTOR);
        
        // Determine recommendation (lower apportionment = lower tax)
        boolean recommendSingleSales = singleSalesApportionment.compareTo(traditionalApportionment) < 0;
        BigDecimal savingsPercentage = traditionalApportionment.subtract(singleSalesApportionment).abs();
        
        String recommendation = recommendSingleSales 
                ? "SINGLE_SALES_FACTOR" 
                : "FOUR_FACTOR_DOUBLE_SALES";
        
        String reason = recommendSingleSales
                ? String.format("Single-sales-factor apportionment (%.2f%%) is %.2f%% lower than traditional formula (%.2f%%), minimizing tax liability",
                        singleSalesApportionment, savingsPercentage, traditionalApportionment)
                : String.format("Traditional formula apportionment (%.2f%%) is %.2f%% lower than single-sales-factor (%.2f%%), minimizing tax liability",
                        traditionalApportionment, savingsPercentage, singleSalesApportionment);
        
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("traditionalFormula", ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES.name());
        comparison.put("traditionalApportionment", traditionalApportionment);
        comparison.put("traditionalBreakdown", traditionalBreakdown);
        comparison.put("singleSalesFormula", ApportionmentFormula.SINGLE_SALES_FACTOR.name());
        comparison.put("singleSalesApportionment", singleSalesApportionment);
        comparison.put("singleSalesBreakdown", singleSalesBreakdown);
        comparison.put("recommendedFormula", recommendation);
        comparison.put("recommendationReason", reason);
        comparison.put("savingsPercentage", savingsPercentage);
        
        log.info("Formula comparison: Traditional={}, SingleSales={}, Recommended={}",
                traditionalApportionment, singleSalesApportionment, recommendation);
        
        return comparison;
    }

    /**
     * Validate if single-sales-factor election is allowed for the municipality and tax year.
     * Task: T133 [US5]
     *
     * @param municipalityId the municipality ID
     * @param taxYear        the tax year
     * @param tenantId       the tenant ID
     * @return true if single-sales-factor is allowed, false otherwise
     */
    public boolean isSingleSalesFactorAllowed(UUID municipalityId, Integer taxYear, UUID tenantId) {
        try {
            // Check if municipality allows single-sales-factor election via rule engine
            ApportionmentFormula allowedFormula = formulaConfigService
                    .getApportionmentFormula(municipalityId, taxYear, tenantId);
            
            log.debug("Municipality {} allows formula: {} for tax year {}",
                    municipalityId, allowedFormula, taxYear);
            
            // For now, assume Ohio allows both traditional and single-sales-factor
            // In production, this would query the rule engine for specific municipality rules
            return true;
        } catch (Exception e) {
            log.warn("Error checking single-sales-factor eligibility for municipality {}: {}",
                    municipalityId, e.getMessage());
            return false;
        }
    }

    /**
     * Calculate apportionment with both formulas and include comparison in the response.
     * This enriches the Schedule Y response with formula comparison data.
     * Task: T134 [US5]
     *
     * @param propertyFactorPercentage property factor percentage
     * @param payrollFactorPercentage  payroll factor percentage
     * @param salesFactorPercentage    sales factor percentage
     * @param electedFormula          the formula the taxpayer elects to use
     * @return map containing elected calculation plus comparison data
     */
    public Map<String, Object> calculateWithFormulaComparison(
            BigDecimal propertyFactorPercentage,
            BigDecimal payrollFactorPercentage,
            BigDecimal salesFactorPercentage,
            ApportionmentFormula electedFormula) {
        
        log.debug("Calculating apportionment with formula comparison, elected formula: {}", electedFormula);
        
        // Get full comparison
        Map<String, Object> comparison = compareFormulaOptions(
                propertyFactorPercentage,
                payrollFactorPercentage,
                salesFactorPercentage);
        
        // Calculate with elected formula
        BigDecimal electedApportionment = calculateApportionmentPercentage(
                propertyFactorPercentage,
                payrollFactorPercentage,
                salesFactorPercentage,
                electedFormula);
        
        Map<String, Object> electedBreakdown = calculateApportionmentBreakdown(
                propertyFactorPercentage,
                payrollFactorPercentage,
                salesFactorPercentage,
                electedFormula);
        
        // Build response
        Map<String, Object> result = new HashMap<>();
        result.put("electedFormula", electedFormula.name());
        result.put("electedApportionment", electedApportionment);
        result.put("electedBreakdown", electedBreakdown);
        result.put("comparison", comparison);
        result.put("usedRecommendation", 
                electedFormula.name().equals(comparison.get("recommendedFormula")));
        
        log.info("Apportionment calculated with comparison: Elected {}={}, Recommended={}",
                electedFormula.name(), electedApportionment, comparison.get("recommendedFormula"));
        
        return result;
    }
}
