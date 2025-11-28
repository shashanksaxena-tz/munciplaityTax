package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.nol.EntityType;
import com.munitax.taxengine.domain.nol.Jurisdiction;
import com.munitax.taxengine.domain.nol.NOL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for NOL entity.
 * Provides data access methods for Net Operating Loss tracking.
 * 
 * Custom Queries:
 * - Find all NOLs for business (FR-001)
 * - Find NOLs with remaining balance (FR-003)
 * - Find expiring NOLs (FR-024)
 * - Find NOLs by jurisdiction and entity type (FR-027, FR-032)
 * 
 * @see NOL
 */
@Repository
public interface NOLRepository extends JpaRepository<NOL, UUID> {
    
    /**
     * Find all NOLs for a business, ordered by tax year (oldest first for FIFO).
     * 
     * Used for:
     * - NOL schedule display (FR-004)
     * - Multi-year tracking (FR-001)
     * - FIFO ordering (FR-022)
     * 
     * @param businessId Business profile ID
     * @return List of NOLs ordered by tax year ascending
     */
    List<NOL> findByBusinessIdOrderByTaxYearAsc(UUID businessId);
    
    /**
     * Find NOL for specific business and tax year.
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year when loss originated
     * @return Optional NOL
     */
    Optional<NOL> findByBusinessIdAndTaxYear(UUID businessId, Integer taxYear);
    
    /**
     * Find all NOLs for a business with remaining balance (not fully used or expired).
     * Ordered by tax year (oldest first for FIFO).
     * 
     * Used for:
     * - Available NOL calculation (FR-003)
     * - Current year NOL deduction (FR-008)
     * - FIFO ordering (FR-022)
     * 
     * @param businessId Business profile ID
     * @param currentDate Current date for expiration check (passed as parameter for query plan caching)
     * @return List of NOLs with current_nol_balance > 0
     */
    @Query("SELECT n FROM NOL n WHERE n.businessId = :businessId AND n.currentNOLBalance > 0 AND (n.expirationDate IS NULL OR n.expirationDate > :currentDate) ORDER BY n.taxYear ASC")
    List<NOL> findAvailableNOLsByBusinessId(@Param("businessId") UUID businessId, @Param("currentDate") LocalDate currentDate);
    
    /**
     * Find all NOLs for a business by jurisdiction.
     * 
     * Used for:
     * - Federal vs state NOL separation (FR-032)
     * - Jurisdiction-specific NOL schedules (FR-035)
     * 
     * @param businessId Business profile ID
     * @param jurisdiction Tax jurisdiction (FEDERAL, STATE_OHIO, MUNICIPALITY)
     * @return List of NOLs for jurisdiction
     */
    List<NOL> findByBusinessIdAndJurisdictionOrderByTaxYearAsc(UUID businessId, Jurisdiction jurisdiction);
    
    /**
     * Find all NOLs for a business by entity type.
     * 
     * Used for:
     * - Entity-specific NOL handling (FR-027)
     * - Pass-through vs entity-level NOL (FR-028, FR-029)
     * 
     * @param businessId Business profile ID
     * @param entityType Entity type (C_CORP, S_CORP, PARTNERSHIP, SOLE_PROP)
     * @return List of NOLs for entity type
     */
    List<NOL> findByBusinessIdAndEntityTypeOrderByTaxYearAsc(UUID businessId, EntityType entityType);
    
    /**
     * Find all NOLs expiring before a given date.
     * 
     * Used for:
     * - Expiration alert generation (FR-024)
     * - NOL expiration report (FR-038)
     * 
     * @param expirationDate Expiration date threshold
     * @return List of NOLs expiring before date
     */
    @Query("SELECT n FROM NOL n WHERE n.expirationDate IS NOT NULL AND n.expirationDate < :expirationDate AND n.currentNOLBalance > 0 ORDER BY n.expirationDate ASC")
    List<NOL> findExpiringNOLs(@Param("expirationDate") LocalDate expirationDate);
    
    /**
     * Find all NOLs for a business expiring within a specific number of years.
     * 
     * Used for:
     * - 2-year expiration alerts (FR-024)
     * - 3-year expiration reports (FR-038)
     * 
     * @param businessId Business profile ID
     * @param expirationDate Date threshold (e.g., 2 years from now)
     * @return List of NOLs expiring before date
     */
    @Query("SELECT n FROM NOL n WHERE n.businessId = :businessId AND n.expirationDate IS NOT NULL AND n.expirationDate < :expirationDate AND n.currentNOLBalance > 0 ORDER BY n.expirationDate ASC")
    List<NOL> findBusinessNOLsExpiringBefore(@Param("businessId") UUID businessId, @Param("expirationDate") LocalDate expirationDate);
    
    /**
     * Calculate total available NOL balance for a business.
     * 
     * Used for:
     * - NOL schedule totals (FR-004)
     * - Maximum deduction calculation (FR-008)
     * 
     * @param businessId Business profile ID
     * @return Total available NOL balance
     */
    @Query("SELECT COALESCE(SUM(n.currentNOLBalance), 0) FROM NOL n WHERE n.businessId = :businessId AND n.currentNOLBalance > 0 AND (n.expirationDate IS NULL OR n.expirationDate > CURRENT_DATE)")
    BigDecimal calculateTotalAvailableNOL(@Param("businessId") UUID businessId);
    
    /**
     * Find all NOLs that have been carried back.
     * 
     * Used for:
     * - Carryback reporting (FR-018)
     * - Refund tracking
     * 
     * @param businessId Business profile ID
     * @return List of NOLs with carryback
     */
    @Query("SELECT n FROM NOL n WHERE n.businessId = :businessId AND n.isCarriedBack = true ORDER BY n.taxYear DESC")
    List<NOL> findCarriedBackNOLs(@Param("businessId") UUID businessId);
    
    /**
     * Find all expired NOLs for a business.
     * 
     * Used for:
     * - Expiration tracking (FR-023)
     * - Historical reporting
     * 
     * @param businessId Business profile ID
     * @return List of expired NOLs
     */
    @Query("SELECT n FROM NOL n WHERE n.businessId = :businessId AND n.expirationDate IS NOT NULL AND n.expirationDate < CURRENT_DATE ORDER BY n.taxYear ASC")
    List<NOL> findExpiredNOLs(@Param("businessId") UUID businessId);
}
