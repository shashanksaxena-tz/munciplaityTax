package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.withholding.IgnoredW2;
import com.munitax.taxengine.domain.withholding.IgnoredW2Reason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for IgnoredW2 entity.
 * 
 * Functional Requirements:
 * - Constitution IV: AI Transparency & Explainability
 * - Research R1: Ignored W-2 Detection Logic
 * 
 * Query Methods:
 * - Find ignored W-2s by reconciliation ID
 * - Find ignored W-2s by reason
 * - Count ignored W-2s per reconciliation
 * 
 * @see IgnoredW2
 */
@Repository
public interface IgnoredW2Repository extends JpaRepository<IgnoredW2, UUID> {
    
    /**
     * Find all ignored W-2s for a specific reconciliation.
     * Used for Ignored W-2 Report modal (Constitution IV).
     * 
     * @param reconciliationId Reconciliation ID
     * @return List of ignored W-2s for this reconciliation
     */
    List<IgnoredW2> findByReconciliationId(UUID reconciliationId);
    
    /**
     * Find all ignored W-2s for a specific reconciliation with a specific reason.
     * Used for filtering ignored W-2s by type (e.g., show only WRONG_EIN).
     * 
     * @param reconciliationId Reconciliation ID
     * @param ignoredReason Reason why W-2 was ignored
     * @return List of ignored W-2s with this reason
     */
    List<IgnoredW2> findByReconciliationIdAndIgnoredReason(
        UUID reconciliationId, 
        IgnoredW2Reason ignoredReason
    );
    
    /**
     * Count how many W-2s were ignored for a specific reconciliation.
     * Used for reconciliation dashboard summary.
     * 
     * @param reconciliationId Reconciliation ID
     * @return Count of ignored W-2s
     */
    long countByReconciliationId(UUID reconciliationId);
    
    /**
     * Check if any W-2s were ignored for a specific reconciliation.
     * Used to determine if "View Ignored W-2s" link should be shown.
     * 
     * @param reconciliationId Reconciliation ID
     * @return True if at least one W-2 was ignored
     */
    boolean existsByReconciliationId(UUID reconciliationId);
}
