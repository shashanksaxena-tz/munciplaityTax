package com.munitax.pdf.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service for mapping database fields to PDF form fields
 */
@Service
public class FieldMappingService {

    private static final Logger log = LoggerFactory.getLogger(FieldMappingService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.US);
    private static final NumberFormat NUMBER_FORMATTER = NumberFormat.getNumberInstance(Locale.US);

    /**
     * Map form data to PDF field values using template field mappings
     */
    public Map<String, String> mapFormData(Map<String, Object> formData, JsonNode fieldMappings) {
        Map<String, String> pdfFieldData = new HashMap<>();
        
        fieldMappings.fields().forEachRemaining(entry -> {
            String dataFieldName = entry.getKey();
            String pdfFieldName = entry.getValue().asText();
            
            Object value = formData.get(dataFieldName);
            if (value != null) {
                String formattedValue = formatValue(value, dataFieldName);
                pdfFieldData.put(pdfFieldName, formattedValue);
                log.debug("Mapped {} -> {} = {}", dataFieldName, pdfFieldName, formattedValue);
            }
        });
        
        return pdfFieldData;
    }

    /**
     * Format value based on its type
     */
    private String formatValue(Object value, String fieldName) {
        if (value == null) {
            return "";
        }

        // Currency fields
        if (fieldName.toLowerCase().contains("tax") || 
            fieldName.toLowerCase().contains("amount") ||
            fieldName.toLowerCase().contains("payment") ||
            fieldName.toLowerCase().contains("income")) {
            if (value instanceof Number) {
                return CURRENCY_FORMATTER.format(((Number) value).doubleValue());
            }
        }

        // Date fields
        if (fieldName.toLowerCase().contains("date")) {
            if (value instanceof LocalDate) {
                return ((LocalDate) value).format(DATE_FORMATTER);
            }
        }

        // FEIN formatting (XX-XXXXXXX)
        if (fieldName.toLowerCase().contains("fein") || fieldName.toLowerCase().equals("ein")) {
            String fein = value.toString().replaceAll("[^0-9]", "");
            if (fein.length() == 9) {
                return fein.substring(0, 2) + "-" + fein.substring(2);
            }
        }

        // SSN formatting (XXX-XX-XXXX)
        if (fieldName.toLowerCase().contains("ssn")) {
            String ssn = value.toString().replaceAll("[^0-9]", "");
            if (ssn.length() == 9) {
                return ssn.substring(0, 3) + "-" + ssn.substring(3, 5) + "-" + ssn.substring(5);
            }
        }

        // Phone formatting
        if (fieldName.toLowerCase().contains("phone")) {
            String phone = value.toString().replaceAll("[^0-9]", "");
            if (phone.length() == 10) {
                return "(" + phone.substring(0, 3) + ") " + phone.substring(3, 6) + "-" + phone.substring(6);
            }
        }

        // Default: string conversion
        return value.toString();
    }

    /**
     * Validate that all required fields are present in form data
     */
    public boolean validateRequiredFields(Map<String, Object> formData, JsonNode validationRules) {
        if (!validationRules.has("requiredFields")) {
            return true;
        }

        JsonNode requiredFields = validationRules.get("requiredFields");
        for (JsonNode fieldNode : requiredFields) {
            String fieldName = fieldNode.asText();
            if (!formData.containsKey(fieldName) || formData.get(fieldName) == null) {
                log.error("Required field missing: {}", fieldName);
                return false;
            }
        }

        return true;
    }
}
