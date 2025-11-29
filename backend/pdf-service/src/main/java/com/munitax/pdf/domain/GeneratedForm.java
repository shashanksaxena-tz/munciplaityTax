package com.munitax.pdf.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generated Form Entity
 * Tracks all generated form instances with versioning and audit trail
 */
@Entity
@Table(name = "generated_forms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"template", "supersededBy"})
@EqualsAndHashCode(exclude = {"template", "supersededBy"})
public class GeneratedForm {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "generated_form_id")
    private UUID generatedFormId;

    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private FormTemplate template;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "form_code", length = 20, nullable = false)
    private String formCode;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private FormStatus status = FormStatus.DRAFT;

    @Column(name = "generated_date", nullable = false, updatable = false)
    private LocalDateTime generatedDate;

    @Column(name = "generated_by", length = 100, nullable = false)
    private String generatedBy;

    @Column(name = "pdf_file_path", length = 500, nullable = false)
    private String pdfFilePath;

    @Column(name = "xml_file_path", length = 500)
    private String xmlFilePath;

    @Column(name = "is_watermarked", nullable = false)
    @Builder.Default
    private Boolean isWatermarked = true;

    @Column(name = "page_count", nullable = false)
    private Integer pageCount;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "form_data", nullable = false, columnDefinition = "jsonb")
    private String formData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "superseded_by")
    private GeneratedForm supersededBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        generatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
