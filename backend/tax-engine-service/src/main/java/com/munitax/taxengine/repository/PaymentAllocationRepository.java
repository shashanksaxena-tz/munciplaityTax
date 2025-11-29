package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.penalty.PaymentAllocation;
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
 * Repository interface for PaymentAllocation entity operations.
 * Provides data access methods for payment allocation tracking.
 * 
 * Multi-tenant isolation: All queries MUST filter by tenant_id.
 */
@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, UUID> {

    /**
     * Find all payment allocations for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of payment allocations ordered by payment date
     */
    List<PaymentAllocation> findByReturnIdAndTenantIdOrderByPaymentDate(UUID returnId, UUID tenantId);

    /**
     * Find most recent payment allocation for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the most recent payment allocation
     */
    @Query("SELECT p FROM PaymentAllocation p WHERE p.returnId = :returnId " +
           "AND p.tenantId = :tenantId ORDER BY p.paymentDate DESC, p.createdAt DESC LIMIT 1")
    Optional<PaymentAllocation> findMostRecentByReturnIdAndTenantId(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find all payment allocations for a tenant with pagination.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of payment allocations
     */
    Page<PaymentAllocation> findByTenantIdOrderByPaymentDateDesc(UUID tenantId, Pageable pageable);

    /**
     * Find payment allocations within a date range.
     *
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return List of payment allocations within the date range
     */
    @Query("SELECT p FROM PaymentAllocation p WHERE p.tenantId = :tenantId " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.paymentDate")
    List<PaymentAllocation> findByTenantIdAndPaymentDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total payments for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Sum of all payment amounts
     */
    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM PaymentAllocation p " +
           "WHERE p.returnId = :returnId AND p.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalPayments(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total amount applied to tax for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Sum of amounts applied to tax
     */
    @Query("SELECT COALESCE(SUM(p.appliedToTax), 0) FROM PaymentAllocation p " +
           "WHERE p.returnId = :returnId AND p.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalAppliedToTax(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total amount applied to penalties for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Sum of amounts applied to penalties
     */
    @Query("SELECT COALESCE(SUM(p.appliedToPenalties), 0) FROM PaymentAllocation p " +
           "WHERE p.returnId = :returnId AND p.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalAppliedToPenalties(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total amount applied to interest for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Sum of amounts applied to interest
     */
    @Query("SELECT COALESCE(SUM(p.appliedToInterest), 0) FROM PaymentAllocation p " +
           "WHERE p.returnId = :returnId AND p.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalAppliedToInterest(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find returns with fully paid balances.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of payment allocations where all balances are zero
     */
    @Query("SELECT p FROM PaymentAllocation p WHERE p.tenantId = :tenantId " +
           "AND p.remainingTaxBalance = 0 AND p.remainingPenaltyBalance = 0 " +
           "AND p.remainingInterestBalance = 0")
    List<PaymentAllocation> findFullyPaidReturns(@Param("tenantId") UUID tenantId);

    /**
     * Find returns with outstanding balances.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of payment allocations where any balance is non-zero
     */
    @Query("SELECT p FROM PaymentAllocation p WHERE p.tenantId = :tenantId " +
           "AND (p.remainingTaxBalance > 0 OR p.remainingPenaltyBalance > 0 " +
           "OR p.remainingInterestBalance > 0)")
    List<PaymentAllocation> findReturnsWithOutstandingBalances(@Param("tenantId") UUID tenantId);

    /**
     * Find payment allocations with largest amounts.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param limit    maximum number of results
     * @return List of top payment allocations by amount
     */
    @Query("SELECT p FROM PaymentAllocation p WHERE p.tenantId = :tenantId " +
           "ORDER BY p.paymentAmount DESC LIMIT :limit")
    List<PaymentAllocation> findTopPaymentsByAmount(
            @Param("tenantId") UUID tenantId,
            @Param("limit") int limit);

    /**
     * Find payment allocation by ID with tenant isolation.
     *
     * @param id       the payment allocation ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the payment allocation if found
     */
    Optional<PaymentAllocation> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Count payment allocations for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Count of payment allocations
     */
    long countByReturnIdAndTenantId(UUID returnId, UUID tenantId);

    /**
     * Check if any payments exist for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return true if payments exist
     */
    boolean existsByReturnIdAndTenantId(UUID returnId, UUID tenantId);
}
