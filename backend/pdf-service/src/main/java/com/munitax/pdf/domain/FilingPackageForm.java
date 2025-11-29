package com.munitax.pdf.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Filing Package Form Entity
 * Junction table linking packages to individual forms with page information
 */
@Entity
@Table(name = "filing_package_forms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(FilingPackageFormId.class)
public class FilingPackageForm {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private FilingPackage filingPackage;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_form_id", nullable = false)
    private GeneratedForm generatedForm;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "start_page", nullable = false)
    private Integer startPage;

    @Column(name = "end_page", nullable = false)
    private Integer endPage;
}

/**
 * Composite key for FilingPackageForm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
class FilingPackageFormId implements Serializable {
    private UUID filingPackage;
    private UUID generatedForm;
}
