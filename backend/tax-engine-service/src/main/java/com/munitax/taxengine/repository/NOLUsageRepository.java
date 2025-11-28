package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.nol.NOLUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for NOLUsage entity.
 * Provides data access methods for NOL utilization tracking.
 * 
 * Custom Queries:
 * - Find usage by NOL ID (FR-002)
 * - Find usage by tax return (FR-004)
 * - Calculate total usage for NOL
 * 
 * @see NOLUsage
 */
@Repository
public interface NOLUsageRepository extends JpaRepository<NOLUsage, UUID> {
    
    /**
     * Find all usage records for a specific NOL.
     * Ordered by usage year ascending.
     * 
     * Used for:
     * - NOL usage history (FR-002)
     * - Schedule display (FR-004)
     * 
     * @param nolId NOL ID
     * @return List of usage records
     */
    List<NOLUsage> findByNolIdOrderByUsageYearAsc(UUID nolId);
    
    /**
     * Find all NOL usage for a specific tax return.
     * 
     * Used for:
     * - Return-specific NOL deduction display
     * - Schedule calculation
     * 
     * @param returnId Tax return ID
     * @return List of NOL usage for return
     */
    List<NOLUsage> findByReturnId(UUID returnId);
    
    /**
     * Calculate total NOL used for a specific NOL record.
     * 
     * Used for:
     * - NOL balance calculation (FR-003)
     * - Remaining balance determination
     * 
     * @param nolId NOL ID
     * @return Total amount used
     */
    @Query("SELECT COALESCE(SUM(u.actualNOLDeduction), 0) FROM NOLUsage u WHERE u.nolId = :nolId")
    BigDecimal calculateTotalUsageForNOL(@Param("nolId") UUID nolId);
    
    /**
     * Find NOL usage for a business in a specific tax year.
     * 
     * Used for:
     * - Year-specific NOL deduction reporting
     * - Multi-year tracking
     * 
     * @param businessId Business profile ID (via join)
     * @param usageYear Tax year when NOL was used
     * @return List of NOL usage records
     */
    @Query("SELECT u FROM NOLUsage u JOIN NOL n ON u.nolId = n.id WHERE n.businessId = :businessId AND u.usageYear = :usageYear")
    List<NOLUsage> findByBusinessIdAndUsageYear(@Param("businessId") UUID businessId, @Param("usageYear") Integer usageYear);
}
