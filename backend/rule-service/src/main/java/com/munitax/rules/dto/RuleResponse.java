package com.munitax.rules.dto;

import com.munitax.rules.model.ApprovalStatus;
import com.munitax.rules.model.RuleCategory;
import com.munitax.rules.model.RuleValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for tax rule data.
 * Used in API responses to client applications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleResponse {
    
    private UUID ruleId;
    
    private String ruleCode;
    
    private String ruleName;
    
    private RuleCategory category;
    
    private RuleValueType valueType;
    
    private Map<String, Object> value;
    
    private LocalDate effectiveDate;
    
    private LocalDate endDate;
    
    private String tenantId;
    
    private List<String> entityTypes;
    
    private String appliesTo;
    
    private Integer version;
    
    private UUID previousVersionId;
    
    private List<UUID> dependsOn;
    
    private ApprovalStatus approvalStatus;
    
    private String approvedBy;
    
    private LocalDateTime approvalDate;
    
    private String createdBy;
    
    private LocalDateTime createdDate;
    
    private String modifiedBy;
    
    private LocalDateTime modifiedDate;
    
    private String changeReason;
    
    private String ordinanceReference;
}
