package com.munitax.pdf.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Form Template Entity
 * Stores PDF form template metadata, field mappings, and validation rules
 */
@Entity
@Table(name = "form_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "form_code", length = 20, nullable = false)
    private String formCode;

    @Column(name = "form_name", length = 255, nullable = false)
    private String formName;

    @Column(name = "template_file_path", length = 500, nullable = false)
    private String templateFilePath;

    @Column(name = "revision_date", nullable = false)
    private LocalDate revisionDate;

    @Column(name = "applicable_years", nullable = false, columnDefinition = "integer[]")
    private Integer[] applicableYears;

    @Column(name = "field_mappings", nullable = false, columnDefinition = "jsonb")
    private String fieldMappings;

    @Column(name = "validation_rules", nullable = false, columnDefinition = "jsonb")
    private String validationRules;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
