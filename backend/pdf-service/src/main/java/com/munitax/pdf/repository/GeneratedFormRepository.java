package com.munitax.pdf.repository;

import com.munitax.pdf.domain.FormStatus;
import com.munitax.pdf.domain.GeneratedForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for GeneratedForm entity
 */
@Repository
public interface GeneratedFormRepository extends JpaRepository<GeneratedForm, UUID> {

    /**
     * Find all forms for a return
     */
    List<GeneratedForm> findByReturnIdOrderByGeneratedDateDesc(UUID returnId);

    /**
     * Find forms by return and form code
     */
    List<GeneratedForm> findByReturnIdAndFormCodeOrderByVersionDesc(UUID returnId, String formCode);

    /**
     * Find latest version of a form
     */
    @Query("SELECT gf FROM GeneratedForm gf WHERE gf.returnId = :returnId " +
           "AND gf.formCode = :formCode " +
           "AND gf.status != 'SUPERSEDED' " +
           "ORDER BY gf.version DESC")
    Optional<GeneratedForm> findLatestVersion(@Param("returnId") UUID returnId, @Param("formCode") String formCode);

    /**
     * Find all forms by tenant and status
     */
    List<GeneratedForm> findByTenantIdAndStatusOrderByGeneratedDateDesc(String tenantId, FormStatus status);

    /**
     * Find forms by business
     */
    List<GeneratedForm> findByBusinessIdOrderByGeneratedDateDesc(UUID businessId);

    /**
     * Find forms by tenant and tax year
     */
    List<GeneratedForm> findByTenantIdAndTaxYearOrderByGeneratedDateDesc(String tenantId, Integer taxYear);

    /**
     * Count forms by status for a return
     */
    Long countByReturnIdAndStatus(UUID returnId, FormStatus status);
}
