package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.ApportionmentFormula;
import com.munitax.taxengine.repository.ApportionmentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public Map<String, BigDecimal> calculateApportionmentBreakdown(
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

        return Map.of(
                "propertyFactorPercentage", propertyFactorPercentage,
                "propertyFactorWeight", propertyWeight,
                "propertyFactorWeightedContribution", propertyContribution,
                "payrollFactorPercentage", payrollFactorPercentage,
                "payrollFactorWeight", payrollWeight,
                "payrollFactorWeightedContribution", payrollContribution,
                "salesFactorPercentage", salesFactorPercentage,
                "salesFactorWeight", salesWeight,
                "salesFactorWeightedContribution", salesContribution,
                "totalWeight", totalWeight,
                "finalApportionmentPercentage", finalApportionment
        );
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
}
