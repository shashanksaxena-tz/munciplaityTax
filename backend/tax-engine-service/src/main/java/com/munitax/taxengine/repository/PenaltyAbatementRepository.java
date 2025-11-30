package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.penalty.PenaltyAbatement;
import com.munitax.taxengine.domain.penalty.AbatementStatus;
import com.munitax.taxengine.domain.penalty.AbatementType;
import com.munitax.taxengine.domain.penalty.AbatementReason;
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
 * Repository interface for PenaltyAbatement entity operations.
 * Provides data access methods for penalty abatement requests and approvals.
 * 
 * Multi-tenant isolation: All queries MUST filter by tenant_id.
 */
@Repository
public interface PenaltyAbatementRepository extends JpaRepository<PenaltyAbatement, UUID> {

    /**
     * Find all abatement requests for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of abatement requests
     */
    List<PenaltyAbatement> findByReturnIdAndTenantId(UUID returnId, UUID tenantId);

    /**
     * Find abatement requests by status.
     *
     * @param status   the abatement status
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of abatement requests with the specified status
     */
    Page<PenaltyAbatement> findByStatusAndTenantIdOrderByRequestDateDesc(
            AbatementStatus status, UUID tenantId, Pageable pageable);

    /**
     * Find pending abatement requests for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of pending abatement requests
     */
    @Query("SELECT a FROM PenaltyAbatement a WHERE a.tenantId = :tenantId " +
           "AND a.status = 'PENDING' ORDER BY a.requestDate")
    List<PenaltyAbatement> findPendingAbatements(@Param("tenantId") UUID tenantId);

    /**
     * Find approved abatement requests for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of approved abatement requests
     */
    @Query("SELECT a FROM PenaltyAbatement a WHERE a.returnId = :returnId " +
           "AND a.tenantId = :tenantId AND a.status IN ('APPROVED', 'PARTIAL')")
    List<PenaltyAbatement> findApprovedByReturnIdAndTenantId(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find abatement request for a specific penalty.
     *
     * @param penaltyId the penalty ID
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @return Optional containing the abatement request if found
     */
    Optional<PenaltyAbatement> findByPenaltyIdAndTenantId(UUID penaltyId, UUID tenantId);

    /**
     * Find abatement requests by type.
     *
     * @param abatementType the abatement type
     * @param tenantId      the tenant ID for multi-tenant isolation
     * @return List of abatement requests of the specified type
     */
    List<PenaltyAbatement> findByAbatementTypeAndTenantId(
            AbatementType abatementType, UUID tenantId);

    /**
     * Find abatement requests by reason.
     *
     * @param reason   the abatement reason
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of abatement requests with the specified reason
     */
    List<PenaltyAbatement> findByReasonAndTenantId(AbatementReason reason, UUID tenantId);

    /**
     * Find abatement requests reviewed by a specific user.
     *
     * @param reviewedBy the reviewer user ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @param pageable   pagination information
     * @return Page of abatement requests reviewed by the user
     */
    Page<PenaltyAbatement> findByReviewedByAndTenantIdOrderByReviewDateDesc(
            UUID reviewedBy, UUID tenantId, Pageable pageable);

    /**
     * Find abatement requests within a date range.
     *
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return List of abatement requests within the date range
     */
    @Query("SELECT a FROM PenaltyAbatement a WHERE a.tenantId = :tenantId " +
           "AND a.requestDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.requestDate DESC")
    List<PenaltyAbatement> findByTenantIdAndRequestDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total approved abatement amount for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Sum of approved amounts
     */
    @Query("SELECT COALESCE(SUM(a.approvedAmount), 0) FROM PenaltyAbatement a " +
           "WHERE a.returnId = :returnId AND a.tenantId = :tenantId " +
           "AND a.status IN ('APPROVED', 'PARTIAL')")
    java.math.BigDecimal calculateTotalApprovedAmount(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total requested abatement amount for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Sum of requested amounts
     */
    @Query("SELECT COALESCE(SUM(a.requestedAmount), 0) FROM PenaltyAbatement a " +
           "WHERE a.returnId = :returnId AND a.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalRequestedAmount(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find abatements with Form 27-PA generated.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of abatements with generated forms
     */
    @Query("SELECT a FROM PenaltyAbatement a WHERE a.tenantId = :tenantId " +
           "AND a.formGenerated IS NOT NULL")
    List<PenaltyAbatement> findWithGeneratedForms(@Param("tenantId") UUID tenantId);

    /**
     * Find abatement by ID with tenant isolation.
     *
     * @param id       the abatement ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the abatement if found
     */
    Optional<PenaltyAbatement> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Count pending abatement requests for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Count of pending requests
     */
    long countByStatusAndTenantId(AbatementStatus status, UUID tenantId);

    /**
     * Find abatement requests by tenant and status (no pagination).
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param status   the abatement status
     * @return List of abatement requests with the specified status
     */
    List<PenaltyAbatement> findByTenantIdAndStatus(UUID tenantId, AbatementStatus status);

    /**
     * Check if abatement exists for a penalty.
     *
     * @param penaltyId the penalty ID
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @return true if abatement exists
     */
    boolean existsByPenaltyIdAndTenantId(UUID penaltyId, UUID tenantId);
}
