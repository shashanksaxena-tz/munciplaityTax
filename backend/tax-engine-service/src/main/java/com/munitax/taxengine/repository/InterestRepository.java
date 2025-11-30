package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.penalty.Interest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Interest entity operations.
 * Provides data access methods for interest calculations.
 * 
 * Multi-tenant isolation: All queries MUST filter by tenant_id.
 */
@Repository
public interface InterestRepository extends JpaRepository<Interest, UUID> {

    /**
     * Find all interest calculations for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of interest calculations
     */
    List<Interest> findByReturnIdAndTenantId(UUID returnId, UUID tenantId);

    /**
     * Find most recent interest calculation for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the most recent interest calculation
     */
    @Query("SELECT i FROM Interest i WHERE i.returnId = :returnId AND i.tenantId = :tenantId " +
           "ORDER BY i.createdAt DESC LIMIT 1")
    Optional<Interest> findMostRecentByReturnIdAndTenantId(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find all interest calculations for a tenant with pagination.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of interest calculations
     */
    Page<Interest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    /**
     * Find all interest calculations for a tenant (no pagination).
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of interest calculations
     */
    List<Interest> findByTenantId(UUID tenantId);

    /**
     * Find interest calculations by tax due date range.
     *
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return List of interest calculations
     */
    @Query("SELECT i FROM Interest i WHERE i.tenantId = :tenantId " +
           "AND i.taxDueDate BETWEEN :startDate AND :endDate " +
           "ORDER BY i.taxDueDate")
    List<Interest> findByTenantIdAndTaxDueDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total interest for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Sum of all interest amounts
     */
    @Query("SELECT COALESCE(SUM(i.totalInterest), 0) FROM Interest i " +
           "WHERE i.returnId = :returnId AND i.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalInterestForReturn(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find interest calculations with total interest above threshold.
     *
     * @param threshold the minimum interest amount
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @return List of interest calculations above threshold
     */
    @Query("SELECT i FROM Interest i WHERE i.tenantId = :tenantId " +
           "AND i.totalInterest > :threshold ORDER BY i.totalInterest DESC")
    List<Interest> findWithInterestAbove(
            @Param("threshold") java.math.BigDecimal threshold,
            @Param("tenantId") UUID tenantId);

    /**
     * Find interest calculations by annual rate range.
     *
     * @param minRate  the minimum annual rate
     * @param maxRate  the maximum annual rate
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of interest calculations within rate range
     */
    @Query("SELECT i FROM Interest i WHERE i.tenantId = :tenantId " +
           "AND i.annualInterestRate BETWEEN :minRate AND :maxRate")
    List<Interest> findByAnnualRateRange(
            @Param("minRate") java.math.BigDecimal minRate,
            @Param("maxRate") java.math.BigDecimal maxRate,
            @Param("tenantId") UUID tenantId);

    /**
     * Find interest calculations for unpaid amounts above threshold.
     *
     * @param threshold the minimum unpaid tax amount
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @return List of interest calculations
     */
    @Query("SELECT i FROM Interest i WHERE i.tenantId = :tenantId " +
           "AND i.unpaidTaxAmount > :threshold ORDER BY i.unpaidTaxAmount DESC")
    List<Interest> findByUnpaidAmountAbove(
            @Param("threshold") java.math.BigDecimal threshold,
            @Param("tenantId") UUID tenantId);

    /**
     * Find interest calculations with longest duration.
     *
     * @param minDays  the minimum number of days
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of interest calculations with duration >= minDays
     */
    @Query("SELECT i FROM Interest i WHERE i.tenantId = :tenantId " +
           "AND i.totalDays >= :minDays ORDER BY i.totalDays DESC")
    List<Interest> findByDurationAtLeast(
            @Param("minDays") Integer minDays,
            @Param("tenantId") UUID tenantId);

    /**
     * Check if interest exists for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return true if interest exists
     */
    boolean existsByReturnIdAndTenantId(UUID returnId, UUID tenantId);

    /**
     * Find interest by ID with tenant isolation.
     *
     * @param id       the interest ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the interest if found
     */
    Optional<Interest> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Count interest calculations for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Count of interest calculations
     */
    long countByReturnIdAndTenantId(UUID returnId, UUID tenantId);
}
