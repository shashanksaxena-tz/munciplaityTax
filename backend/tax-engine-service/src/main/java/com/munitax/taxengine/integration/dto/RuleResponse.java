package com.munitax.taxengine.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for tax rule response from rule-service.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleResponse {
    private UUID ruleId;
    private String ruleCode;
    private String ruleName;
    private String category;
    private String valueType;
    private RuleValue value;
    private LocalDate effectiveDate;
    private LocalDate endDate;
    private String tenantId;
    private List<String> entityTypes;
    private Integer version;
    private String approvalStatus;
    private String approvedBy;
    private LocalDateTime approvalDate;
    private String changeReason;
    private String ordinanceReference;
}
