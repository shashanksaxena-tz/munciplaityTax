package com.munitax.pdf.repository;

import com.munitax.pdf.domain.FormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FormTemplate entity
 */
@Repository
public interface FormTemplateRepository extends JpaRepository<FormTemplate, UUID> {

    /**
     * Find active template by form code and tax year
     */
    @Query("SELECT ft FROM FormTemplate ft WHERE ft.formCode = :formCode " +
           "AND :taxYear = ANY(ft.applicableYears) " +
           "AND ft.isActive = true " +
           "AND (ft.tenantId = :tenantId OR ft.tenantId IS NULL) " +
           "ORDER BY ft.tenantId DESC NULLS LAST, ft.revisionDate DESC")
    Optional<FormTemplate> findActiveTemplateByFormCodeAndYear(
        @Param("formCode") String formCode, 
        @Param("taxYear") Integer taxYear,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all active templates for a tenant
     */
    @Query("SELECT ft FROM FormTemplate ft WHERE ft.isActive = true " +
           "AND (ft.tenantId = :tenantId OR ft.tenantId IS NULL) " +
           "ORDER BY ft.formCode, ft.revisionDate DESC")
    List<FormTemplate> findAllActiveTemplates(@Param("tenantId") String tenantId);

    /**
     * Find templates by form code
     */
    List<FormTemplate> findByFormCodeAndIsActive(String formCode, Boolean isActive);
}
