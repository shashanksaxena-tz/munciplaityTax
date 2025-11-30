package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.penalty.Penalty;
import com.munitax.taxengine.domain.penalty.PenaltyType;
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
 * Repository interface for Penalty entity operations.
 * Provides data access methods for penalty assessments.
 * 
 * Multi-tenant isolation: All queries MUST filter by tenant_id.
 */
@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, UUID> {

    /**
     * Find all penalties for a tax return with tenant isolation.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of penalties for the return
     */
    List<Penalty> findByReturnIdAndTenantId(UUID returnId, UUID tenantId);

    /**
     * Find penalties by type for a return with tenant isolation.
     *
     * @param returnId    the return ID
     * @param penaltyType the penalty type
     * @param tenantId    the tenant ID for multi-tenant isolation
     * @return List of penalties of the specified type
     */
    List<Penalty> findByReturnIdAndPenaltyTypeAndTenantId(
            UUID returnId, PenaltyType penaltyType, UUID tenantId);

    /**
     * Find non-abated penalties for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of active (non-abated) penalties
     */
    @Query("SELECT p FROM Penalty p WHERE p.returnId = :returnId " +
           "AND p.tenantId = :tenantId AND p.isAbated = false")
    List<Penalty> findActiveByReturnIdAndTenantId(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find abated penalties for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of abated penalties
     */
    @Query("SELECT p FROM Penalty p WHERE p.returnId = :returnId " +
           "AND p.tenantId = :tenantId AND p.isAbated = true")
    List<Penalty> findAbatedByReturnIdAndTenantId(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find all penalties for a tenant with pagination.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of penalties
     */
    Page<Penalty> findByTenantIdOrderByAssessmentDateDesc(UUID tenantId, Pageable pageable);

    /**
     * Find penalties assessed within a date range.
     *
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return List of penalties assessed within the date range
     */
    @Query("SELECT p FROM Penalty p WHERE p.tenantId = :tenantId " +
           "AND p.assessmentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.assessmentDate DESC")
    List<Penalty> findByTenantIdAndAssessmentDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total penalty amount for a return (non-abated only).
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Sum of all non-abated penalty amounts, or 0 if none
     */
    @Query("SELECT COALESCE(SUM(p.penaltyAmount), 0) FROM Penalty p " +
           "WHERE p.returnId = :returnId AND p.tenantId = :tenantId " +
           "AND p.isAbated = false")
    java.math.BigDecimal calculateTotalPenaltyForReturn(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total penalty amount by type for a return.
     *
     * @param returnId    the return ID
     * @param penaltyType the penalty type
     * @param tenantId    the tenant ID for multi-tenant isolation
     * @return Sum of non-abated penalty amounts of the specified type
     */
    @Query("SELECT COALESCE(SUM(p.penaltyAmount), 0) FROM Penalty p " +
           "WHERE p.returnId = :returnId AND p.penaltyType = :penaltyType " +
           "AND p.tenantId = :tenantId AND p.isAbated = false")
    java.math.BigDecimal calculateTotalPenaltyByType(
            @Param("returnId") UUID returnId,
            @Param("penaltyType") PenaltyType penaltyType,
            @Param("tenantId") UUID tenantId);

    /**
     * Find penalties reaching maximum cap.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of penalties at maximum cap
     */
    @Query("SELECT p FROM Penalty p WHERE p.tenantId = :tenantId " +
           "AND p.penaltyAmount >= p.maximumPenalty")
    List<Penalty> findPenaltiesAtMaximumCap(@Param("tenantId") UUID tenantId);

    /**
     * Check if a penalty exists for a return and type.
     *
     * @param returnId    the return ID
     * @param penaltyType the penalty type
     * @param tenantId    the tenant ID for multi-tenant isolation
     * @return true if penalty exists
     */
    boolean existsByReturnIdAndPenaltyTypeAndTenantId(
            UUID returnId, PenaltyType penaltyType, UUID tenantId);

    /**
     * Find penalty by ID with tenant isolation.
     *
     * @param id       the penalty ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the penalty if found
     */
    Optional<Penalty> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Count penalties for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Count of penalties
     */
    long countByReturnIdAndTenantId(UUID returnId, UUID tenantId);

    /**
     * Find penalties assessed after a certain date.
     *
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @param afterDate the date to find penalties after
     * @return List of penalties assessed after the date
     */
    @Query("SELECT p FROM Penalty p WHERE p.tenantId = :tenantId " +
           "AND p.assessmentDate > :afterDate " +
           "ORDER BY p.assessmentDate DESC")
    List<Penalty> findByTenantIdAndAssessmentDateAfter(
            @Param("tenantId") UUID tenantId,
            @Param("afterDate") LocalDate afterDate);
}
