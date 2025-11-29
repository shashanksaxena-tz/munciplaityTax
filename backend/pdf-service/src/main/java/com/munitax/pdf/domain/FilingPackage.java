package com.munitax.pdf.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filing Package Entity
 * Manages multi-form filing packages with table of contents and submission tracking
 */
@Entity
@Table(name = "filing_packages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilingPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "package_id")
    private UUID packageId;

    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", length = 20, nullable = false)
    @Builder.Default
    private PackageType packageType = PackageType.ORIGINAL;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;

    @Column(name = "total_pages", nullable = false)
    private Integer totalPages;

    @Column(name = "package_pdf_path", length = 500, nullable = false)
    private String packagePdfPath;

    @Column(name = "table_of_contents", nullable = false, columnDefinition = "jsonb")
    private String tableOfContents;

    @Column(name = "submission_date")
    private LocalDateTime submissionDate;

    @Column(name = "confirmation_number", length = 100)
    private String confirmationNumber;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @OneToMany(mappedBy = "filingPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FilingPackageForm> packageForms = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
