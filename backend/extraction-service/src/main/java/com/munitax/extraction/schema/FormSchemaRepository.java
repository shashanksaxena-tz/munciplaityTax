package com.munitax.extraction.schema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FormSchemaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormSchemaRepository.class);
    private static final String SCHEMA_PATH_PATTERN = "classpath:form-schemas/*.json";

    private final ObjectMapper objectMapper;
    private final Map<String, FormSchema> schemasByFormType = new HashMap<>();

    public FormSchemaRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadSchemas() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] schemaResources = resolver.getResources(SCHEMA_PATH_PATTERN);
            for (Resource resource : schemaResources) {
                try (InputStream stream = resource.getInputStream()) {
                    FormSchema schema = objectMapper.readValue(stream, new TypeReference<FormSchema>() {});
                    if (schema.getFormType() == null || schema.getFormType().isBlank()) {
                        LOGGER.warn("Skipping schema without formType from resource {}", resource.getFilename());
                        continue;
                    }
                    schemasByFormType.put(schema.getFormType(), schema);
                }
            }
            LOGGER.info("Loaded {} form schemas", schemasByFormType.size());
        } catch (IOException ex) {
            LOGGER.error("Failed to load form schemas", ex);
        }
    }

    public FormSchema getSchema(String formId) {
        return schemasByFormType.get(formId);
    }

    public Map<String, FormSchema> getAllSchemas() {
        return Collections.unmodifiableMap(schemasByFormType);
    }

    public List<FormFieldSchema> getFieldsForForm(String formId) {
        FormSchema schema = schemasByFormType.get(formId);
        if (schema == null) {
            return Collections.emptyList();
        }
        return schema.getFields();
    }
}
