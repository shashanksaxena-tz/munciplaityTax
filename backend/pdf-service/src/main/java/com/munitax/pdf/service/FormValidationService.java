package com.munitax.pdf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.munitax.pdf.domain.FormTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for validating form data before PDF generation
 */
@Service
public class FormValidationService {

    private static final Logger log = LoggerFactory.getLogger(FormValidationService.class);

    /**
     * Validate form data against template validation rules
     */
    public List<String> validateFormData(Map<String, Object> formData, FormTemplate template, JsonNode validationRules) {
        List<String> errors = new ArrayList<>();

        // Check required fields
        if (validationRules.has("requiredFields")) {
            validateRequiredFields(formData, validationRules.get("requiredFields"), errors);
        }

        // Check field validations (patterns, ranges, etc.)
        if (validationRules.has("validations")) {
            validateFieldRules(formData, validationRules.get("validations"), errors);
        }

        // Check calculations if present
        if (validationRules.has("calculations")) {
            validateCalculations(formData, validationRules.get("calculations"), errors);
        }

        return errors;
    }

    /**
     * Validate required fields are present
     */
    private void validateRequiredFields(Map<String, Object> formData, JsonNode requiredFields, List<String> errors) {
        for (JsonNode fieldNode : requiredFields) {
            String fieldName = fieldNode.asText();
            if (!formData.containsKey(fieldName) || formData.get(fieldName) == null || 
                formData.get(fieldName).toString().trim().isEmpty()) {
                errors.add(String.format("Required field '%s' is missing or empty", fieldName));
            }
        }
    }

    /**
     * Validate field-specific rules (patterns, ranges, etc.)
     */
    private void validateFieldRules(Map<String, Object> formData, JsonNode validations, List<String> errors) {
        validations.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode rules = entry.getValue();
            Object value = formData.get(fieldName);

            if (value == null) return;

            // Pattern validation (e.g., FEIN format)
            if (rules.has("pattern")) {
                String pattern = rules.get("pattern").asText();
                String message = rules.has("message") ? rules.get("message").asText() : "Invalid format";
                if (!value.toString().matches(pattern)) {
                    errors.add(String.format("%s: %s", fieldName, message));
                }
            }

            // Min/max validation (e.g., tax amount must be positive)
            if (value instanceof Number) {
                double numValue = ((Number) value).doubleValue();
                
                if (rules.has("min")) {
                    double min = rules.get("min").asDouble();
                    if (numValue < min) {
                        String message = rules.has("message") ? rules.get("message").asText() : 
                            String.format("Value must be at least %s", min);
                        errors.add(String.format("%s: %s", fieldName, message));
                    }
                }
                
                if (rules.has("max")) {
                    double max = rules.get("max").asDouble();
                    if (numValue > max) {
                        String message = rules.has("message") ? rules.get("message").asText() : 
                            String.format("Value must be at most %s", max);
                        errors.add(String.format("%s: %s", fieldName, message));
                    }
                }
            }

            // Enum validation (e.g., quarter must be 1, 2, 3, or 4)
            if (rules.has("enum")) {
                JsonNode enumValues = rules.get("enum");
                boolean valid = false;
                for (JsonNode enumValue : enumValues) {
                    if (enumValue.asText().equals(value.toString())) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    String message = rules.has("message") ? rules.get("message").asText() : "Invalid value";
                    errors.add(String.format("%s: %s", fieldName, message));
                }
            }
        });
    }

    /**
     * Validate calculations (placeholder - actual calculation validation would be more complex)
     */
    private void validateCalculations(Map<String, Object> formData, JsonNode calculations, List<String> errors) {
        // Placeholder for calculation validation
        // In a full implementation, you would:
        // 1. Parse calculation expressions
        // 2. Evaluate them against form data
        // 3. Check if calculated values match provided values
    }

    /**
     * Check if form is valid (no errors)
     */
    public boolean isValid(List<String> errors) {
        return errors.isEmpty();
    }
}
