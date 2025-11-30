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
    @Query(value = "SELECT * FROM form_templates ft WHERE ft.form_code = :formCode " +
           "AND :taxYear = ANY(ft.applicable_years) " +
           "AND ft.is_active = true " +
           "AND (ft.tenant_id = :tenantId OR ft.tenant_id IS NULL) " +
           "ORDER BY ft.tenant_id DESC NULLS LAST, ft.revision_date DESC",
           nativeQuery = true)
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
