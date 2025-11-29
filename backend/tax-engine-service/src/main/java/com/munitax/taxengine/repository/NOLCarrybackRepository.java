package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.nol.NOLCarryback;
import com.munitax.taxengine.domain.nol.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for NOLCarryback entity.
 * Provides data access methods for NOL carryback tracking.
 * 
 * Custom Queries:
 * - Find carrybacks by NOL ID (FR-016)
 * - Find carrybacks by refund status (FR-017)
 * - Calculate total carryback amount
 * 
 * @see NOLCarryback
 */
@Repository
public interface NOLCarrybackRepository extends JpaRepository<NOLCarryback, UUID> {
    
    /**
     * Find all carryback records for a specific NOL.
     * Ordered by carryback year (oldest first).
     * 
     * Used for:
     * - CARES Act carryback display (FR-016)
     * - Refund calculation (FR-017)
     * 
     * @param nolId NOL ID
     * @return List of carryback records
     */
    List<NOLCarryback> findByNolIdOrderByCarrybackYearAsc(UUID nolId);
    
    /**
     * Find all carryback records by refund status.
     * 
     * Used for:
     * - Refund tracking and reporting
     * - Outstanding claims monitoring
     * 
     * @param refundStatus Refund status
     * @return List of carryback records with status
     */
    List<NOLCarryback> findByRefundStatus(RefundStatus refundStatus);
    
    /**
     * Calculate total carryback amount for a specific NOL.
     * 
     * Used for:
     * - NOL balance calculation
     * - Carryback vs carryforward comparison
     * 
     * @param nolId NOL ID
     * @return Total carryback amount
     */
    @Query("SELECT COALESCE(SUM(c.nolApplied), 0) FROM NOLCarryback c WHERE c.nolId = :nolId")
    BigDecimal calculateTotalCarrybackForNOL(@Param("nolId") UUID nolId);
    
    /**
     * Calculate total refund amount for a specific NOL.
     * 
     * Used for:
     * - Refund reporting (FR-018)
     * - Cash flow analysis
     * 
     * @param nolId NOL ID
     * @return Total refund amount
     */
    @Query("SELECT COALESCE(SUM(c.refundAmount), 0) FROM NOLCarryback c WHERE c.nolId = :nolId")
    BigDecimal calculateTotalRefundForNOL(@Param("nolId") UUID nolId);
    
    /**
     * Find carryback records for a business in a specific carryback year.
     * 
     * Used for:
     * - Prior year refund analysis
     * - Amended return impact assessment
     * 
     * @param businessId Business profile ID (via join)
     * @param carrybackYear Year to which NOL was carried back
     * @return List of carryback records
     */
    @Query("SELECT c FROM NOLCarryback c JOIN NOL n ON c.nolId = n.id WHERE n.businessId = :businessId AND c.carrybackYear = :carrybackYear")
    List<NOLCarryback> findByBusinessIdAndCarrybackYear(@Param("businessId") UUID businessId, @Param("carrybackYear") Integer carrybackYear);
}
