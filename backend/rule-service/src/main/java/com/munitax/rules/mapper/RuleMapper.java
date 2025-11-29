package com.munitax.rules.mapper;

import com.munitax.rules.dto.RuleHistoryResponse;
import com.munitax.rules.dto.RuleResponse;
import com.munitax.rules.model.RuleChangeLog;
import com.munitax.rules.model.TaxRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between entity and DTO objects.
 */
@Component
public class RuleMapper {
    
    /**
     * Convert TaxRule entity to RuleResponse DTO.
     */
    public RuleResponse toResponse(TaxRule rule) {
        if (rule == null) {
            return null;
        }
        
        RuleResponse response = new RuleResponse();
        response.setRuleId(rule.getRuleId());
        response.setRuleCode(rule.getRuleCode());
        response.setRuleName(rule.getRuleName());
        response.setCategory(rule.getCategory());
        response.setValueType(rule.getValueType());
        response.setValue(rule.getValue());
        response.setEffectiveDate(rule.getEffectiveDate());
        response.setEndDate(rule.getEndDate());
        response.setTenantId(rule.getTenantId());
        response.setEntityTypes(rule.getEntityTypes());
        response.setAppliesTo(rule.getAppliesTo());
        response.setApprovalStatus(rule.getApprovalStatus());
        response.setCreatedBy(rule.getCreatedBy());
        response.setCreatedDate(rule.getCreatedDate());
        response.setModifiedBy(rule.getModifiedBy());
        response.setModifiedDate(rule.getModifiedDate());
        response.setApprovedBy(rule.getApprovedBy());
        response.setApprovalDate(rule.getApprovalDate());
        response.setChangeReason(rule.getChangeReason());
        response.setOrdinanceReference(rule.getOrdinanceReference());
        response.setVersion(rule.getVersion());
        response.setPreviousVersionId(rule.getPreviousVersionId());
        response.setDependsOn(rule.getDependsOn());
        
        return response;
    }
    
    /**
     * Convert list of TaxRule entities to list of RuleResponse DTOs.
     */
    public List<RuleResponse> toResponseList(List<TaxRule> rules) {
        if (rules == null) {
            return List.of();
        }
        
        return rules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert RuleChangeLog entity to RuleHistoryResponse DTO.
     */
    public RuleHistoryResponse toHistoryResponse(RuleChangeLog changeLog) {
        if (changeLog == null) {
            return null;
        }
        
        RuleHistoryResponse response = new RuleHistoryResponse();
        response.setLogId(changeLog.getLogId());
        response.setRuleId(changeLog.getRuleId());
        response.setChangeType(changeLog.getChangeType());
        response.setOldValue(changeLog.getOldValue());
        response.setNewValue(changeLog.getNewValue());
        response.setChangedFields(changeLog.getChangedFields());
        response.setChangedBy(changeLog.getChangedBy());
        response.setChangeDate(changeLog.getChangeDate());
        response.setChangeReason(changeLog.getChangeReason());
        response.setAffectedReturnsCount(changeLog.getAffectedReturnsCount());
        response.setImpactEstimate(changeLog.getImpactEstimate());
        
        return response;
    }
    
    /**
     * Convert list of RuleChangeLog entities to list of RuleHistoryResponse DTOs.
     */
    public List<RuleHistoryResponse> toHistoryResponseList(List<RuleChangeLog> changeLogs) {
        if (changeLogs == null) {
            return List.of();
        }
        
        return changeLogs.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }
}
