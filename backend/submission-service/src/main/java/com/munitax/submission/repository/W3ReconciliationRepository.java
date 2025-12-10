package com.munitax.submission.repository;

import com.munitax.submission.model.W3Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for W3Reconciliation entity.
 * Provides data access methods for W-3 year-end reconciliation records.
 */
@Repository
public interface W3ReconciliationRepository extends JpaRepository<W3Reconciliation, UUID> {
    
    /**
     * Find W-3 reconciliation for a specific business and tax year.
     * 
     * @param businessId Business ID (FEIN)
     * @param taxYear Tax year (e.g., 2024)
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return Optional W-3 reconciliation
     */
    Optional<W3Reconciliation> findByBusinessIdAndTaxYearAndTenantId(
        String businessId, Integer taxYear, String tenantId
    );
    
    /**
     * Find all W-3 reconciliations for a business.
     * 
     * @param businessId Business ID (FEIN)
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return List of W-3 reconciliations ordered by tax year descending
     */
    List<W3Reconciliation> findByBusinessIdAndTenantIdOrderByTaxYearDesc(
        String businessId, String tenantId
    );
    
    /**
     * Find all W-3 reconciliations for a specific tax year.
     * 
     * @param taxYear Tax year (e.g., 2024)
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return List of W-3 reconciliations
     */
    List<W3Reconciliation> findByTaxYearAndTenantId(Integer taxYear, String tenantId);
    
    /**
     * Find all unbalanced W-3 reconciliations.
     * 
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return List of unbalanced W-3 reconciliations
     */
    List<W3Reconciliation> findByStatusAndTenantId(String status, String tenantId);
    
    /**
     * Find all unsubmitted W-3 reconciliations.
     * 
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return List of unsubmitted W-3 reconciliations
     */
    List<W3Reconciliation> findByIsSubmittedAndTenantId(Boolean isSubmitted, String tenantId);
    
    /**
     * Check if W-3 reconciliation exists for a business and tax year.
     * 
     * @param businessId Business ID (FEIN)
     * @param taxYear Tax year (e.g., 2024)
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return true if exists, false otherwise
     */
    boolean existsByBusinessIdAndTaxYearAndTenantId(
        String businessId, Integer taxYear, String tenantId
    );
    
    /**
     * Find W-3 reconciliations by status and submitted flag.
     * 
     * @param status Reconciliation status (BALANCED/UNBALANCED)
     * @param isSubmitted Submission status
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return List of W-3 reconciliations
     */
    List<W3Reconciliation> findByStatusAndIsSubmittedAndTenantId(
        String status, Boolean isSubmitted, String tenantId
    );
}
