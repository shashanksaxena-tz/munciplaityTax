package com.munitax.taxengine.dto;

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
 * Response DTO for tax rule data from rule-service.
 * Mirror of RuleResponse from rule-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleResponseDto {
    
    private UUID ruleId;
    private String ruleCode;
    private String ruleName;
    private String category;
    private String valueType;
    private Map<String, Object> value;
    private LocalDate effectiveDate;
    private LocalDate endDate;
    private String tenantId;
    private List<String> entityTypes;
    private String appliesTo;
    private Integer version;
    private UUID previousVersionId;
    private List<UUID> dependsOn;
    private String approvalStatus;
    private String approvedBy;
    private LocalDateTime approvalDate;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    private String changeReason;
    private String ordinanceReference;
}
