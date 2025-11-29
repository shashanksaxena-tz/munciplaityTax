package com.munitax.pdf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.pdf.domain.FormTemplate;
import com.munitax.pdf.repository.FormTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing form templates
 */
@Service
public class FormTemplateService {

    private static final Logger log = LoggerFactory.getLogger(FormTemplateService.class);

    private final FormTemplateRepository formTemplateRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public FormTemplateService(FormTemplateRepository formTemplateRepository, ObjectMapper objectMapper) {
        this.formTemplateRepository = formTemplateRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Find active template for form code and tax year
     */
    public Optional<FormTemplate> findTemplateForForm(String formCode, Integer taxYear, String tenantId) {
        log.debug("Finding template for form={}, year={}, tenant={}", formCode, taxYear, tenantId);
        return formTemplateRepository.findActiveTemplateByFormCodeAndYear(formCode, taxYear, tenantId);
    }

    /**
     * Get all active templates for tenant
     */
    public List<FormTemplate> getAllActiveTemplates(String tenantId) {
        return formTemplateRepository.findAllActiveTemplates(tenantId);
    }

    /**
     * Parse field mappings from JSON
     */
    public JsonNode getFieldMappings(FormTemplate template) {
        try {
            return objectMapper.readTree(template.getFieldMappings());
        } catch (Exception e) {
            log.error("Error parsing field mappings: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    /**
     * Parse validation rules from JSON
     */
    public JsonNode getValidationRules(FormTemplate template) {
        try {
            return objectMapper.readTree(template.getValidationRules());
        } catch (Exception e) {
            log.error("Error parsing validation rules: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    /**
     * Save or update template
     */
    public FormTemplate saveTemplate(FormTemplate template) {
        return formTemplateRepository.save(template);
    }
}
