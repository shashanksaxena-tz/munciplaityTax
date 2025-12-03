package com.munitax.rules.dto;

import com.munitax.rules.model.RuleCategory;
import com.munitax.rules.model.RuleValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for updating an existing tax rule.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRuleRequest {
    
    private String ruleName;
    
    private RuleCategory category;
    
    private RuleValueType valueType;
    
    private Map<String, Object> value;
    
    private LocalDate effectiveDate;
    
    private LocalDate endDate;
    
    private List<String> entityTypes;
    
    private String appliesTo;
    
    private List<UUID> dependsOn;
    
    private String changeReason;
    
    private String ordinanceReference;
}
