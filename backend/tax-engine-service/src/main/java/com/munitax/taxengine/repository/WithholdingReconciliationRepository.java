package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.withholding.ReconciliationStatus;
import com.munitax.taxengine.domain.withholding.WithholdingReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WithholdingReconciliation entity.
 * Provides data access for year-end W-1 to W-2/W-3 reconciliation.
 * 
 * Business Rules:
 * - FR-006: Flag discrepancy if variance > $100 OR variance% > 1%
 * - FR-009: Track reconciliation status per tax year
 * - FR-010: Prevent next year W-1 filing if prior year reconciliation incomplete
 * 
 * @see WithholdingReconciliation
 * @see W1Filing
 */
@Repository
public interface WithholdingReconciliationRepository extends JpaRepository<WithholdingReconciliation, UUID> {
    
    /**
     * Find reconciliation for business + tax year.
     * Unique constraint ensures at most one reconciliation per business + year.
     * 
     * Used for:
     * - Reconciliation report display (FR-007)
     * - Status check before next year filing (FR-010)
     * - Dashboard display (FR-015)
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year (e.g., 2024)
     * @return Optional reconciliation
     */
    Optional<WithholdingReconciliation> findByBusinessIdAndTaxYear(UUID businessId, Integer taxYear);
    
    /**
     * Check if reconciliation exists for business + tax year.
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year
     * @return true if reconciliation exists
     */
    boolean existsByBusinessIdAndTaxYear(UUID businessId, Integer taxYear);
    
    /**
     * Find all reconciliations for a business across all tax years.
     * Ordered by tax year descending (most recent first).
     * 
     * Used for reconciliation history display.
     * 
     * @param businessId Business profile ID
     * @return List of reconciliations
     */
    List<WithholdingReconciliation> findByBusinessIdOrderByTaxYearDesc(UUID businessId);
    
    /**
     * Find all reconciliations with a specific status.
     * Used for:
     * - Admin dashboard showing all businesses with discrepancies
     * - Auditor queue for discrepancy review
     * 
     * @param status ReconciliationStatus
     * @return List of reconciliations with given status
     */
    List<WithholdingReconciliation> findByStatus(ReconciliationStatus status);
    
    /**
     * Check if business has unresolved reconciliation for prior year (FR-010).
     * Prevents filing next year's W-1 if prior year reconciliation incomplete.
     * 
     * Example: Business tries to file 2025 Q1 W-1, but 2024 reconciliation status = DISCREPANCY.
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year to check
     * @return true if reconciliation exists and status = DISCREPANCY
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM WithholdingReconciliation r WHERE r.businessId = :businessId AND r.taxYear = :taxYear AND r.status = 'DISCREPANCY'")
    boolean hasUnresolvedReconciliation(@Param("businessId") UUID businessId, @Param("taxYear") Integer taxYear);
}
