package com.munitax.rules.dto;

import com.munitax.rules.model.RuleCategory;
import com.munitax.rules.model.RuleValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new tax rule.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRuleRequest {
    
    @NotBlank(message = "Rule code is required")
    private String ruleCode;
    
    @NotBlank(message = "Rule name is required")
    private String ruleName;
    
    @NotNull(message = "Category is required")
    private RuleCategory category;
    
    @NotNull(message = "Value type is required")
    private RuleValueType valueType;
    
    @NotNull(message = "Value is required")
    private Object value;
    
    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
    
    private LocalDate endDate;
    
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    @Builder.Default
    private List<String> entityTypes = List.of("ALL");
    
    private String appliesTo;
    
    private UUID previousVersionId;
    
    private List<UUID> dependsOn;
    
    @NotBlank(message = "Change reason is required")
    private String changeReason;
    
    private String ordinanceReference;
}
