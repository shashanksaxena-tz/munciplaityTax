package com.munitax.rules.service;

import com.munitax.rules.model.ApprovalStatus;
import com.munitax.rules.model.TaxRule;
import com.munitax.rules.repository.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for validating tax rules.
 * Handles overlap detection, conflict checking, and business rule validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RuleValidationService {
    
    private final TaxRuleRepository ruleRepository;
    
    /**
     * Validate that a new rule doesn't overlap with existing approved rules.
     * 
     * @param ruleCode Rule code to check
     * @param tenantId Tenant identifier
     * @param effectiveDate New rule effective date
     * @param endDate New rule end date (nullable)
     * @throws RuleOverlapException if overlap detected
     */
    public void validateNoOverlap(String ruleCode, String tenantId, 
                                   LocalDate effectiveDate, LocalDate endDate) {
        // Use a non-existent UUID (all zeros) to ensure no rules are excluded
        UUID excludeNone = new UUID(0L, 0L);
        List<TaxRule> overlapping = ruleRepository.findOverlappingRules(
            ruleCode, tenantId, effectiveDate, endDate, excludeNone
        );
        
        if (!overlapping.isEmpty()) {
            log.warn("Overlap detected for rule code: {} tenant: {}", ruleCode, tenantId);
            throw new RuleOverlapException(
                String.format("Rule '%s' for tenant '%s' overlaps with %d existing rule(s). " +
                    "Overlapping date range: %s to %s", 
                    ruleCode, tenantId, overlapping.size(), effectiveDate, endDate)
            );
        }
    }
    
    /**
     * Validate that a rule update doesn't overlap with other rules.
     * 
     * @param ruleId ID of rule being updated (to exclude from overlap check)
     * @param ruleCode Rule code
     * @param tenantId Tenant identifier
     * @param effectiveDate New effective date
     * @param endDate New end date (nullable)
     * @throws RuleOverlapException if overlap detected
     */
    public void validateNoOverlapForUpdate(UUID ruleId, String ruleCode, String tenantId,
                                           LocalDate effectiveDate, LocalDate endDate) {
        List<TaxRule> overlapping = ruleRepository.findOverlappingRules(
            ruleCode, tenantId, effectiveDate, endDate, ruleId
        );
        
        if (!overlapping.isEmpty()) {
            log.warn("Overlap detected for rule update: {} code: {}", ruleId, ruleCode);
            throw new RuleOverlapException(
                String.format("Updated rule '%s' overlaps with %d existing rule(s)", 
                    ruleCode, overlapping.size())
            );
        }
    }
    
    /**
     * Validate that a rule can be modified (not already effective).
     * Prevents retroactive changes to rules that are already in use.
     * 
     * @param rule Rule to validate
     * @throws RetroactiveChangeException if rule is already effective
     */
    public void validateNotRetroactive(TaxRule rule) {
        if (rule.getApprovalStatus() == ApprovalStatus.APPROVED && 
            rule.getEffectiveDate().isBefore(LocalDate.now())) {
            throw new RetroactiveChangeException(
                String.format("Cannot modify rule '%s' - already effective since %s. " +
                    "Create new version with future effective date instead.", 
                    rule.getRuleCode(), rule.getEffectiveDate())
            );
        }
    }
    
    /**
     * Validate that approver is different from creator (prevent self-approval).
     * 
     * @param createdBy Original creator
     * @param approvedBy Approver
     * @throws SelfApprovalException if same user
     */
    public void validateNotSelfApproval(String createdBy, String approvedBy) {
        if (createdBy.equals(approvedBy)) {
            throw new SelfApprovalException(
                "Cannot approve your own rule. Another TAX_ADMINISTRATOR must review and approve."
            );
        }
    }
    
    /**
     * Validate tenant ID is not blank.
     * 
     * @param tenantId Tenant identifier
     * @throws IllegalArgumentException if blank
     */
    public void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID cannot be blank");
        }
    }
    
    // Exception classes
    
    public static class RuleOverlapException extends RuntimeException {
        public RuleOverlapException(String message) {
            super(message);
        }
    }
    
    public static class RetroactiveChangeException extends RuntimeException {
        public RetroactiveChangeException(String message) {
            super(message);
        }
    }
    
    public static class SelfApprovalException extends RuntimeException {
        public SelfApprovalException(String message) {
            super(message);
        }
    }
}
