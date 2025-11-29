package com.munitax.rules.service;

import com.munitax.rules.dto.CreateRuleRequest;
import com.munitax.rules.dto.UpdateRuleRequest;
import com.munitax.rules.model.*;
import com.munitax.rules.repository.RuleChangeLogRepository;
import com.munitax.rules.repository.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing tax rule lifecycle (CRUD operations, approval workflow).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RuleManagementService {
    
    private final TaxRuleRepository ruleRepository;
    private final RuleChangeLogRepository changeLogRepository;
    private final RuleValidationService validationService;
    private final RuleCacheService cacheService;
    
    /**
     * Create a new tax rule (status = PENDING).
     * 
     * @param request Rule creation request
     * @param createdBy User creating the rule
     * @return Created rule
     */
    public TaxRule createRule(CreateRuleRequest request, String createdBy) {
        log.info("Creating rule: {} for tenant: {}", request.getRuleCode(), request.getTenantId());
        
        // Validate tenant ID
        validationService.validateTenantId(request.getTenantId());
        
        // Validate no overlap with existing rules
        validationService.validateNoOverlap(
            request.getRuleCode(),
            request.getTenantId(),
            request.getEffectiveDate(),
            request.getEndDate()
        );
        
        // Create rule entity
        TaxRule rule = TaxRule.builder()
            .ruleCode(request.getRuleCode())
            .ruleName(request.getRuleName())
            .category(request.getCategory())
            .valueType(request.getValueType())
            .value(request.getValue())
            .effectiveDate(request.getEffectiveDate())
            .endDate(request.getEndDate())
            .tenantId(request.getTenantId())
            .entityTypes(request.getEntityTypes() != null ? request.getEntityTypes() : List.of("ALL"))
            .appliesTo(request.getAppliesTo())
            .previousVersionId(request.getPreviousVersionId())
            .dependsOn(request.getDependsOn())
            .approvalStatus(ApprovalStatus.PENDING)
            .createdBy(createdBy)
            .createdDate(LocalDateTime.now())
            .changeReason(request.getChangeReason())
            .ordinanceReference(request.getOrdinanceReference())
            .version(1)
            .build();
        
        // Increment version if this is a new version of existing rule
        if (request.getPreviousVersionId() != null) {
            ruleRepository.findById(request.getPreviousVersionId())
                .ifPresent(prevRule -> {
                    rule.setVersion(prevRule.getVersion() + 1);
                });
        }
        
        // Save to database
        TaxRule savedRule = ruleRepository.save(rule);
        
        // Log creation in audit trail
        logRuleChange(savedRule, ChangeType.CREATE, null, savedRule);
        
        log.info("Created rule: {} with ID: {}", savedRule.getRuleCode(), savedRule.getRuleId());
        return savedRule;
    }
    
    /**
     * Update an existing rule (creates new version if already effective).
     * 
     * @param ruleId Rule ID to update
     * @param request Update request
     * @param modifiedBy User making the update
     * @return Updated rule
     */
    public TaxRule updateRule(UUID ruleId, UpdateRuleRequest request, String modifiedBy) {
        log.info("Updating rule: {}", ruleId);
        
        TaxRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new RuleNotFoundException("Rule not found: " + ruleId));
        
        // Validate rule can be modified (not retroactive)
        validationService.validateNotRetroactive(rule);
        
        // Validate no overlap if dates are changing
        if (request.getEffectiveDate() != null || request.getEndDate() != null) {
            LocalDate newEffectiveDate = request.getEffectiveDate() != null ? 
                request.getEffectiveDate() : rule.getEffectiveDate();
            LocalDate newEndDate = request.getEndDate() != null ? 
                request.getEndDate() : rule.getEndDate();
            
            validationService.validateNoOverlapForUpdate(
                ruleId, rule.getRuleCode(), rule.getTenantId(),
                newEffectiveDate, newEndDate
            );
        }
        
        // Capture old state for audit log
        Object oldValue = captureRuleState(rule);
        List<String> changedFields = new ArrayList<>();
        
        // Apply updates
        if (request.getRuleName() != null) {
            rule.setRuleName(request.getRuleName());
            changedFields.add("ruleName");
        }
        if (request.getCategory() != null) {
            rule.setCategory(request.getCategory());
            changedFields.add("category");
        }
        if (request.getValueType() != null) {
            rule.setValueType(request.getValueType());
            changedFields.add("valueType");
        }
        if (request.getValue() != null) {
            rule.setValue(request.getValue());
            changedFields.add("value");
        }
        if (request.getEffectiveDate() != null) {
            rule.setEffectiveDate(request.getEffectiveDate());
            changedFields.add("effectiveDate");
        }
        if (request.getEndDate() != null) {
            rule.setEndDate(request.getEndDate());
            changedFields.add("endDate");
        }
        if (request.getEntityTypes() != null) {
            rule.setEntityTypes(request.getEntityTypes());
            changedFields.add("entityTypes");
        }
        if (request.getAppliesTo() != null) {
            rule.setAppliesTo(request.getAppliesTo());
            changedFields.add("appliesTo");
        }
        if (request.getOrdinanceReference() != null) {
            rule.setOrdinanceReference(request.getOrdinanceReference());
            changedFields.add("ordinanceReference");
        }
        
        rule.setModifiedBy(modifiedBy);
        rule.setModifiedDate(LocalDateTime.now());
        if (request.getChangeReason() != null) {
            rule.setChangeReason(request.getChangeReason());
        }
        
        // Save updated rule
        rule = ruleRepository.save(rule);
        
        // Log update in audit trail
        RuleChangeLog changeLog = RuleChangeLog.builder()
            .ruleId(rule.getRuleId())
            .changeType(ChangeType.UPDATE)
            .oldValue(oldValue)
            .newValue(captureRuleState(rule))
            .changedFields(changedFields)
            .changedBy(modifiedBy)
            .changeReason(request.getChangeReason() != null ? request.getChangeReason() : "Rule updated")
            .build();
        changeLogRepository.save(changeLog);
        
        // Invalidate cache for this tenant
        cacheService.invalidateTenantCache(rule.getTenantId());
        
        log.info("Updated rule: {}", ruleId);
        return rule;
    }
    
    /**
     * Approve a rule (changes status to APPROVED).
     * 
     * @param ruleId Rule ID to approve
     * @param approvedBy User approving the rule
     * @param approvalReason Reason for approval
     * @return Approved rule
     */
    public TaxRule approveRule(UUID ruleId, String approvedBy, String approvalReason) {
        log.info("Approving rule: {}", ruleId);
        
        TaxRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new RuleNotFoundException("Rule not found: " + ruleId));
        
        // Validate not self-approval
        validationService.validateNotSelfApproval(rule.getCreatedBy(), approvedBy);
        
        // Capture old state
        Object oldValue = captureRuleState(rule);
        
        // Update approval status
        rule.setApprovalStatus(ApprovalStatus.APPROVED);
        rule.setApprovedBy(approvedBy);
        rule.setApprovalDate(LocalDateTime.now());
        rule.setModifiedBy(approvedBy);
        rule.setModifiedDate(LocalDateTime.now());
        
        rule = ruleRepository.save(rule);
        
        // Log approval
        RuleChangeLog changeLog = RuleChangeLog.builder()
            .ruleId(rule.getRuleId())
            .changeType(ChangeType.APPROVE)
            .oldValue(oldValue)
            .newValue(captureRuleState(rule))
            .changedFields(List.of("approvalStatus", "approvedBy", "approvalDate"))
            .changedBy(approvedBy)
            .changeReason(approvalReason)
            .build();
        changeLogRepository.save(changeLog);
        
        // Invalidate cache for this tenant
        cacheService.invalidateTenantCache(rule.getTenantId());
        
        log.info("Approved rule: {}", ruleId);
        return rule;
    }
    
    /**
     * Reject a rule.
     * 
     * @param ruleId Rule ID to reject
     * @param rejectedBy User rejecting the rule
     * @param rejectionReason Reason for rejection
     * @return Rejected rule
     */
    public TaxRule rejectRule(UUID ruleId, String rejectedBy, String rejectionReason) {
        log.info("Rejecting rule: {}", ruleId);
        
        TaxRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new RuleNotFoundException("Rule not found: " + ruleId));
        
        Object oldValue = captureRuleState(rule);
        
        rule.setApprovalStatus(ApprovalStatus.REJECTED);
        rule.setModifiedBy(rejectedBy);
        rule.setModifiedDate(LocalDateTime.now());
        
        rule = ruleRepository.save(rule);
        
        // Log rejection
        logChange(rule, ChangeType.REJECT, oldValue, captureRuleState(rule), 
                  List.of("approvalStatus"), rejectedBy, rejectionReason);
        
        log.info("Rejected rule: {}", ruleId);
        return rule;
    }
    
    /**
     * Void a rule (soft delete).
     * 
     * @param ruleId Rule ID to void
     * @param voidedBy User voiding the rule
     * @param voidReason Reason for voiding
     * @return Voided rule
     */
    public TaxRule voidRule(UUID ruleId, String voidedBy, String voidReason) {
        log.info("Voiding rule: {}", ruleId);
        
        TaxRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new RuleNotFoundException("Rule not found: " + ruleId));
        
        Object oldValue = captureRuleState(rule);
        
        rule.setApprovalStatus(ApprovalStatus.VOIDED);
        rule.setModifiedBy(voidedBy);
        rule.setModifiedDate(LocalDateTime.now());
        
        rule = ruleRepository.save(rule);
        
        // Log void
        logChange(rule, ChangeType.VOID, oldValue, captureRuleState(rule),
                  List.of("approvalStatus"), voidedBy, voidReason);
        
        // Invalidate cache
        cacheService.invalidateTenantCache(rule.getTenantId());
        
        log.info("Voided rule: {}", ruleId);
        return rule;
    }
    
    /**
     * Get all rules for a tenant.
     */
    public List<TaxRule> getRulesByTenant(String tenantId) {
        return ruleRepository.findByTenantIdOrderByCreatedDateDesc(tenantId);
    }
    
    /**
     * Get rules by approval status.
     */
    public List<TaxRule> getRulesByStatus(ApprovalStatus status) {
        return ruleRepository.findByApprovalStatusOrderByCreatedDateDesc(status);
    }
    
    // Helper methods
    
    private void logRuleChange(TaxRule rule, ChangeType changeType, Object oldValue, Object newValue) {
        RuleChangeLog log = RuleChangeLog.builder()
            .ruleId(rule.getRuleId())
            .changeType(changeType)
            .oldValue(oldValue)
            .newValue(newValue)
            .changedFields(List.of())
            .changedBy(rule.getCreatedBy())
            .changeReason(rule.getChangeReason())
            .build();
        changeLogRepository.save(log);
    }
    
    private void logChange(TaxRule rule, ChangeType changeType, Object oldValue, Object newValue,
                           List<String> changedFields, String changedBy, String reason) {
        RuleChangeLog log = RuleChangeLog.builder()
            .ruleId(rule.getRuleId())
            .changeType(changeType)
            .oldValue(oldValue)
            .newValue(newValue)
            .changedFields(changedFields)
            .changedBy(changedBy)
            .changeReason(reason)
            .build();
        changeLogRepository.save(log);
    }
    
    private Object captureRuleState(TaxRule rule) {
        // Capture complete rule state for audit trail
        return java.util.Map.of(
            "ruleCode", rule.getRuleCode(),
            "ruleName", rule.getRuleName(),
            "category", rule.getCategory().toString(),
            "valueType", rule.getValueType().toString(),
            "value", rule.getValue(),
            "effectiveDate", rule.getEffectiveDate().toString(),
            "endDate", rule.getEndDate() != null ? rule.getEndDate().toString() : null,
            "approvalStatus", rule.getApprovalStatus().toString(),
            "version", rule.getVersion()
        );
    }
    
    /**
     * Get rule by ID.
     */
    public TaxRule getRule(UUID ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException("Rule not found: " + ruleId));
    }
    
    /**
     * Get all rules for a tenant and category.
     */
    public List<TaxRule> getRulesByTenantAndCategory(String tenantId, RuleCategory category) {
        return ruleRepository.findByTenantIdAndCategory(tenantId, category);
    }
    
    /**
     * Get all rules.
     */
    public List<TaxRule> getAllRules() {
        return ruleRepository.findAll();
    }
    
    // Exception class
    
    public static class RuleNotFoundException extends RuntimeException {
        public RuleNotFoundException(String message) {
            super(message);
        }
    }
}
