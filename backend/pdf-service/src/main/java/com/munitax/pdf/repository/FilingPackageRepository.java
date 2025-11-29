package com.munitax.pdf.repository;

import com.munitax.pdf.domain.FilingPackage;
import com.munitax.pdf.domain.PackageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FilingPackage entity
 */
@Repository
public interface FilingPackageRepository extends JpaRepository<FilingPackage, UUID> {

    /**
     * Find packages by return ID
     */
    List<FilingPackage> findByReturnIdOrderByCreatedDateDesc(UUID returnId);

    /**
     * Find packages by business
     */
    List<FilingPackage> findByBusinessIdOrderByCreatedDateDesc(UUID businessId);

    /**
     * Find packages by tenant and year
     */
    List<FilingPackage> findByTenantIdAndTaxYearOrderByCreatedDateDesc(String tenantId, Integer taxYear);

    /**
     * Find latest package for a return
     */
    Optional<FilingPackage> findFirstByReturnIdAndPackageTypeOrderByCreatedDateDesc(
        UUID returnId, 
        PackageType packageType
    );

    /**
     * Find packages by status
     */
    List<FilingPackage> findByTenantIdAndStatusOrderByCreatedDateDesc(String tenantId, String status);
}
