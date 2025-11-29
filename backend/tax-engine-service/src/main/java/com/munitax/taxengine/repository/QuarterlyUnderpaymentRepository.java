package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.penalty.QuarterlyUnderpayment;
import com.munitax.taxengine.domain.penalty.Quarter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for QuarterlyUnderpayment entity operations.
 * Provides data access methods for quarterly underpayment details.
 * 
 * Multi-tenant isolation: All queries MUST filter by tenant_id.
 */
@Repository
public interface QuarterlyUnderpaymentRepository extends JpaRepository<QuarterlyUnderpayment, UUID> {

    /**
     * Find all quarterly underpayments for an estimated penalty.
     *
     * @param estimatedPenaltyId the estimated penalty ID
     * @param tenantId           the tenant ID for multi-tenant isolation
     * @return List of quarterly underpayments ordered by quarter
     */
    List<QuarterlyUnderpayment> findByEstimatedPenaltyIdAndTenantIdOrderByQuarter(
            UUID estimatedPenaltyId, UUID tenantId);

    /**
     * Find quarterly underpayment by quarter.
     *
     * @param estimatedPenaltyId the estimated penalty ID
     * @param quarter            the quarter (Q1, Q2, Q3, Q4)
     * @param tenantId           the tenant ID for multi-tenant isolation
     * @return Optional containing the quarterly underpayment if found
     */
    Optional<QuarterlyUnderpayment> findByEstimatedPenaltyIdAndQuarterAndTenantId(
            UUID estimatedPenaltyId, Quarter quarter, UUID tenantId);

    /**
     * Find quarters with underpayments (positive underpayment amount).
     *
     * @param estimatedPenaltyId the estimated penalty ID
     * @param tenantId           the tenant ID for multi-tenant isolation
     * @return List of quarters with underpayments
     */
    @Query("SELECT q FROM QuarterlyUnderpayment q WHERE q.estimatedPenaltyId = :estimatedPenaltyId " +
           "AND q.tenantId = :tenantId AND q.underpayment > 0 ORDER BY q.quarter")
    List<QuarterlyUnderpayment> findQuartersWithUnderpayment(
            @Param("estimatedPenaltyId") UUID estimatedPenaltyId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find quarters with overpayments (negative underpayment amount).
     *
     * @param estimatedPenaltyId the estimated penalty ID
     * @param tenantId           the tenant ID for multi-tenant isolation
     * @return List of quarters with overpayments
     */
    @Query("SELECT q FROM QuarterlyUnderpayment q WHERE q.estimatedPenaltyId = :estimatedPenaltyId " +
           "AND q.tenantId = :tenantId AND q.underpayment < 0 ORDER BY q.quarter")
    List<QuarterlyUnderpayment> findQuartersWithOverpayment(
            @Param("estimatedPenaltyId") UUID estimatedPenaltyId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total underpayment for an estimated penalty.
     *
     * @param estimatedPenaltyId the estimated penalty ID
     * @param tenantId           the tenant ID for multi-tenant isolation
     * @return Sum of underpayments (excluding overpayments)
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN q.underpayment > 0 THEN q.underpayment ELSE 0 END), 0) " +
           "FROM QuarterlyUnderpayment q WHERE q.estimatedPenaltyId = :estimatedPenaltyId " +
           "AND q.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalUnderpayment(
            @Param("estimatedPenaltyId") UUID estimatedPenaltyId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total overpayment for an estimated penalty.
     *
     * @param estimatedPenaltyId the estimated penalty ID
     * @param tenantId           the tenant ID for multi-tenant isolation
     * @return Sum of absolute values of overpayments
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN q.underpayment < 0 THEN ABS(q.underpayment) ELSE 0 END), 0) " +
           "FROM QuarterlyUnderpayment q WHERE q.estimatedPenaltyId = :estimatedPenaltyId " +
           "AND q.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalOverpayment(
            @Param("estimatedPenaltyId") UUID estimatedPenaltyId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total penalty across all quarters.
     *
     * @param estimatedPenaltyId the estimated penalty ID
     * @param tenantId           the tenant ID for multi-tenant isolation
     * @return Sum of all quarterly penalties
     */
    @Query("SELECT COALESCE(SUM(q.penaltyAmount), 0) FROM QuarterlyUnderpayment q " +
           "WHERE q.estimatedPenaltyId = :estimatedPenaltyId AND q.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalPenalty(
            @Param("estimatedPenaltyId") UUID estimatedPenaltyId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find quarterly underpayment by ID with tenant isolation.
     *
     * @param id       the quarterly underpayment ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the quarterly underpayment if found
     */
    Optional<QuarterlyUnderpayment> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Count quarterly underpayments for an estimated penalty.
     *
     * @param estimatedPenaltyId the estimated penalty ID
     * @param tenantId           the tenant ID for multi-tenant isolation
     * @return Count of quarterly underpayments
     */
    long countByEstimatedPenaltyIdAndTenantId(UUID estimatedPenaltyId, UUID tenantId);

    /**
     * Delete all quarterly underpayments for an estimated penalty.
     * Used when recalculating penalties.
     *
     * @param estimatedPenaltyId the estimated penalty ID
     * @param tenantId           the tenant ID for multi-tenant isolation
     */
    void deleteByEstimatedPenaltyIdAndTenantId(UUID estimatedPenaltyId, UUID tenantId);
}
