package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.withholding.CumulativeWithholdingTotals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CumulativeWithholdingTotals entity.
 * Provides data access for cached YTD cumulative totals.
 * 
 * Performance:
 * - Success Criteria: <2 seconds for dashboard query (FR-002)
 * - Research R2: Option B (Cached table) provides O(1) query vs O(n) SUM()
 * - Redis cache: 5-minute TTL with database fallback
 * 
 * Update Strategy:
 * - INSERT when first W-1 filed for business + tax year
 * - UPDATE on W1FiledEvent (event-driven architecture)
 * - CASCADE UPDATE when W-1 amended (Research R3)
 * 
 * @see CumulativeWithholdingTotals
 * @see W1Filing
 */
@Repository
public interface CumulativeWithholdingTotalsRepository extends JpaRepository<CumulativeWithholdingTotals, UUID> {
    
    /**
     * Find cumulative totals for business + tax year.
     * Unique constraint ensures at most one record.
     * 
     * Used for:
     * - Dashboard display (FR-002, FR-015)
     * - Year-end reconciliation (FR-006)
     * - On-track indicator calculation (FR-005)
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year (e.g., 2024)
     * @return Optional cumulative totals (empty if no W-1s filed yet)
     */
    Optional<CumulativeWithholdingTotals> findByBusinessIdAndTaxYear(UUID businessId, Integer taxYear);
    
    /**
     * Check if cumulative totals exist for business + tax year.
     * Used before INSERT to determine if UPDATE needed.
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year
     * @return true if record exists
     */
    boolean existsByBusinessIdAndTaxYear(UUID businessId, Integer taxYear);
}
