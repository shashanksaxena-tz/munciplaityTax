package com.munitax.rules.dto;

import com.munitax.rules.model.ChangeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for rule change history.
 * Used in audit trail and version history views.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleHistoryResponse {
    
    private UUID logId;
    
    private UUID ruleId;
    
    private ChangeType changeType;
    
    private Object oldValue;
    
    private Object newValue;
    
    private List<String> changedFields;
    
    private String changedBy;
    
    private LocalDateTime changeDate;
    
    private String changeReason;
    
    private Integer affectedReturnsCount;
    
    private Object impactEstimate;
}
