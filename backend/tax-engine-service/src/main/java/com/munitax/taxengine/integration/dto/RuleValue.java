package com.munitax.taxengine.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO representing the polymorphic 'value' field in a tax rule.
 * Different rule types have different value structures.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleValue {
    // For PERCENTAGE and NUMBER types
    private String unit;
    private BigDecimal scalar;
    
    // For BOOLEAN types
    private Boolean flag;
    
    // For ENUM types
    private String option;
    private List<String> allowedValues;
    
    // For locality rates
    private String municipalityCode;
    private String municipalityName;
}
