package com.munitax.rules.service;

import com.munitax.rules.dto.CreateRuleRequest;
import com.munitax.rules.dto.RuleResponse;
import com.munitax.rules.dto.UpdateRuleRequest;
import com.munitax.rules.mapper.RuleMapper;
import com.munitax.rules.model.ApprovalStatus;
import com.munitax.rules.model.RuleCategory;
import com.munitax.rules.model.TaxRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Facade service that provides DTO-based API for controllers.
 * Wraps RuleManagementService and TemporalRuleService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RuleFacadeService {
    
    private final RuleManagementService ruleManagementService;
    private final TemporalRuleService temporalRuleService;
    private final RuleMapper ruleMapper;
    
    /**
     * Create a new tax rule.
     */
    public RuleResponse createRule(CreateRuleRequest request) {
        String userId = "system"; // TODO: Extract from SecurityContext
        TaxRule rule = ruleManagementService.createRule(request, userId);
        return ruleMapper.toResponse(rule);
    }
    
    /**
     * Update an existing tax rule.
     */
    public RuleResponse updateRule(Long ruleId, UpdateRuleRequest request) {
        String userId = "system"; // TODO: Extract from SecurityContext
        UUID uuid = UUID.fromString(String.valueOf(ruleId));
        TaxRule rule = ruleManagementService.updateRule(uuid, request, userId);
        return ruleMapper.toResponse(rule);
    }
    
    /**
     * Approve a pending tax rule.
     */
    public RuleResponse approveRule(Long ruleId, String approverId) {
        UUID uuid = UUID.fromString(String.valueOf(ruleId));
        TaxRule rule = ruleManagementService.approveRule(uuid, approverId, "Approved");
        return ruleMapper.toResponse(rule);
    }
    
    /**
     * Reject a pending tax rule.
     */
    public RuleResponse rejectRule(Long ruleId, String reason) {
        String userId = "system"; // TODO: Extract from SecurityContext
        UUID uuid = UUID.fromString(String.valueOf(ruleId));
        TaxRule rule = ruleManagementService.rejectRule(uuid, userId, reason);
        return ruleMapper.toResponse(rule);
    }
    
    /**
     * Get rules with optional filters.
     */
    public List<RuleResponse> getRules(String tenantId, String category, String status) {
        List<TaxRule> rules;
        
        if (tenantId != null && category != null) {
            RuleCategory cat = RuleCategory.valueOf(category);
            rules = ruleManagementService.getRulesByTenantAndCategory(tenantId, cat);
        } else if (tenantId != null) {
            rules = ruleManagementService.getRulesByTenant(tenantId);
        } else {
            rules = ruleManagementService.getAllRules();
        }
        
        // Filter by status if provided
        if (status != null) {
            ApprovalStatus statusEnum = ApprovalStatus.valueOf(status);
            rules = rules.stream()
                    .filter(r -> r.getApprovalStatus() == statusEnum)
                    .toList();
        }
        
        return ruleMapper.toResponseList(rules);
    }
    
    /**
     * Get a specific rule by ID.
     */
    public RuleResponse getRule(Long ruleId) {
        UUID uuid = UUID.fromString(String.valueOf(ruleId));
        TaxRule rule = ruleManagementService.getRule(uuid);
        return ruleMapper.toResponse(rule);
    }
    
    /**
     * Void/delete a rule (soft delete).
     */
    public void voidRule(Long ruleId, String reason) {
        String userId = "system"; // TODO: Extract from SecurityContext
        UUID uuid = UUID.fromString(String.valueOf(ruleId));
        ruleManagementService.voidRule(uuid, userId, reason);
    }
    
    /**
     * Get active rules for a specific tax year and tenant.
     */
    public List<RuleResponse> getActiveRules(String tenantId, LocalDate asOfDate, String entityType) {
        List<TaxRule> rules = temporalRuleService.getActiveRules(tenantId, asOfDate);
        
        // Filter by entity type if specified
        if (entityType != null && !entityType.isEmpty()) {
            rules = rules.stream()
                    .filter(rule -> rule.getEntityTypes() != null &&
                            (rule.getEntityTypes().contains(entityType) ||
                                    rule.getEntityTypes().contains("ALL")))
                    .toList();
        }
        
        return ruleMapper.toResponseList(rules);
    }
    
    /**
     * Get rule as of a specific date (point-in-time query).
     */
    public RuleResponse getRuleAsOf(String ruleCode, String tenantId, LocalDate asOfDate) {
        return temporalRuleService.getActiveRuleByCode(ruleCode, tenantId, asOfDate)
                .map(ruleMapper::toResponse)
                .orElse(null);
    }
    
    /**
     * Get future rules (pending activation).
     */
    public List<RuleResponse> getFutureRules(String tenantId, LocalDate fromDate) {
        List<TaxRule> rules = ruleManagementService.getRulesByTenant(tenantId).stream()
                .filter(r -> r.getEffectiveDate().isAfter(fromDate))
                .toList();
        return ruleMapper.toResponseList(rules);
    }
    
    /**
     * Get rule history (all versions).
     */
    public List<RuleResponse> getRuleHistory(String ruleCode, String tenantId) {
        List<TaxRule> rules = ruleManagementService.getRulesByTenant(tenantId).stream()
                .filter(r -> r.getRuleCode().equals(ruleCode))
                .sorted((a, b) -> b.getVersion() - a.getVersion())
                .toList();
        return ruleMapper.toResponseList(rules);
    }
    
    /**
     * Validate if a rule would overlap with existing rules.
     */
    public boolean hasOverlap(String ruleCode, String tenantId, LocalDate effectiveDate, LocalDate endDate) {
        List<TaxRule> existingRules = ruleManagementService.getRulesByTenant(tenantId).stream()
                .filter(r -> r.getRuleCode().equals(ruleCode))
                .toList();
        
        for (TaxRule existing : existingRules) {
            if (existing.overlaps(effectiveDate, endDate)) {
                return true;
            }
        }
        
        return false;
    }
}
