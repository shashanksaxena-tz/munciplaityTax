package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.penalty.QuarterlyInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for QuarterlyInterest entity operations.
 * Provides data access methods for quarterly interest breakdown.
 * 
 * Multi-tenant isolation: All queries MUST filter by tenant_id.
 */
@Repository
public interface QuarterlyInterestRepository extends JpaRepository<QuarterlyInterest, UUID> {

    /**
     * Find all quarterly interests for an interest calculation.
     *
     * @param interestId the interest calculation ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return List of quarterly interests ordered by start date
     */
    List<QuarterlyInterest> findByInterestIdAndTenantIdOrderByStartDate(
            UUID interestId, UUID tenantId);

    /**
     * Find quarterly interest by quarter label.
     *
     * @param interestId the interest calculation ID
     * @param quarter    the quarter label (e.g., "Q1 2024")
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Optional containing the quarterly interest if found
     */
    Optional<QuarterlyInterest> findByInterestIdAndQuarterAndTenantId(
            UUID interestId, String quarter, UUID tenantId);

    /**
     * Calculate total interest across all quarters.
     *
     * @param interestId the interest calculation ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Sum of all quarterly interest amounts
     */
    @Query("SELECT COALESCE(SUM(q.interestAccrued), 0) FROM QuarterlyInterest q " +
           "WHERE q.interestId = :interestId AND q.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalInterest(
            @Param("interestId") UUID interestId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find quarters with highest interest accrued.
     *
     * @param interestId the interest calculation ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @param limit      maximum number of results
     * @return List of top quarters by interest accrued
     */
    @Query("SELECT q FROM QuarterlyInterest q WHERE q.interestId = :interestId " +
           "AND q.tenantId = :tenantId ORDER BY q.interestAccrued DESC LIMIT :limit")
    List<QuarterlyInterest> findTopQuartersByInterest(
            @Param("interestId") UUID interestId,
            @Param("tenantId") UUID tenantId,
            @Param("limit") int limit);

    /**
     * Find the first quarter for an interest calculation.
     *
     * @param interestId the interest calculation ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Optional containing the first quarter
     */
    @Query("SELECT q FROM QuarterlyInterest q WHERE q.interestId = :interestId " +
           "AND q.tenantId = :tenantId ORDER BY q.startDate ASC LIMIT 1")
    Optional<QuarterlyInterest> findFirstQuarter(
            @Param("interestId") UUID interestId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find the last quarter for an interest calculation.
     *
     * @param interestId the interest calculation ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Optional containing the last quarter
     */
    @Query("SELECT q FROM QuarterlyInterest q WHERE q.interestId = :interestId " +
           "AND q.tenantId = :tenantId ORDER BY q.endDate DESC LIMIT 1")
    Optional<QuarterlyInterest> findLastQuarter(
            @Param("interestId") UUID interestId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total days across all quarters.
     *
     * @param interestId the interest calculation ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Sum of days across all quarters
     */
    @Query("SELECT COALESCE(SUM(q.days), 0) FROM QuarterlyInterest q " +
           "WHERE q.interestId = :interestId AND q.tenantId = :tenantId")
    Integer calculateTotalDays(
            @Param("interestId") UUID interestId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find quarterly interest by ID with tenant isolation.
     *
     * @param id       the quarterly interest ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the quarterly interest if found
     */
    Optional<QuarterlyInterest> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Count quarterly interests for an interest calculation.
     *
     * @param interestId the interest calculation ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Count of quarterly interests
     */
    long countByInterestIdAndTenantId(UUID interestId, UUID tenantId);

    /**
     * Delete all quarterly interests for an interest calculation.
     * Used when recalculating interest.
     *
     * @param interestId the interest calculation ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     */
    void deleteByInterestIdAndTenantId(UUID interestId, UUID tenantId);

    /**
     * Verify balance continuity between quarters.
     * Returns quarters where ending balance doesn't match next quarter's beginning balance.
     *
     * @param interestId the interest calculation ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return List of quarters with balance discrepancies
     */
    @Query("SELECT q FROM QuarterlyInterest q WHERE q.interestId = :interestId " +
           "AND q.tenantId = :tenantId AND EXISTS (" +
           "  SELECT q2 FROM QuarterlyInterest q2 " +
           "  WHERE q2.interestId = q.interestId " +
           "  AND q2.startDate = DATEADD(DAY, 1, q.endDate) " +
           "  AND ABS(q2.beginningBalance - q.endingBalance) > 0.01" +
           ")")
    List<QuarterlyInterest> findBalanceDiscrepancies(
            @Param("interestId") UUID interestId,
            @Param("tenantId") UUID tenantId);
}
