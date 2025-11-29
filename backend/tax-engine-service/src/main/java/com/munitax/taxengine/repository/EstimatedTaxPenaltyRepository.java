package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.penalty.EstimatedTaxPenalty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for EstimatedTaxPenalty entity operations.
 * Provides data access methods for estimated tax penalty assessments.
 * 
 * Multi-tenant isolation: All queries MUST filter by tenant_id.
 */
@Repository
public interface EstimatedTaxPenaltyRepository extends JpaRepository<EstimatedTaxPenalty, UUID> {

    /**
     * Find estimated tax penalty by return ID with tenant isolation.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the penalty if found
     */
    Optional<EstimatedTaxPenalty> findByReturnIdAndTenantId(UUID returnId, UUID tenantId);

    /**
     * Find all estimated tax penalties for a tenant with pagination.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of estimated tax penalties
     */
    Page<EstimatedTaxPenalty> findByTenantIdOrderByTaxYearDesc(UUID tenantId, Pageable pageable);

    /**
     * Find estimated tax penalties by tax year.
     *
     * @param taxYear  the tax year
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of estimated tax penalties for the year
     */
    List<EstimatedTaxPenalty> findByTaxYearAndTenantId(Integer taxYear, UUID tenantId);

    /**
     * Find penalties where no safe harbor was met.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of penalties with no safe harbor
     */
    @Query("SELECT e FROM EstimatedTaxPenalty e WHERE e.tenantId = :tenantId " +
           "AND e.safeHarbor1Met = false AND e.safeHarbor2Met = false")
    List<EstimatedTaxPenalty> findWithoutSafeHarbor(@Param("tenantId") UUID tenantId);

    /**
     * Find penalties where safe harbor 1 was met (90% current year).
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of penalties with safe harbor 1
     */
    @Query("SELECT e FROM EstimatedTaxPenalty e WHERE e.tenantId = :tenantId " +
           "AND e.safeHarbor1Met = true")
    List<EstimatedTaxPenalty> findWithSafeHarbor1(@Param("tenantId") UUID tenantId);

    /**
     * Find penalties where safe harbor 2 was met (100%/110% prior year).
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of penalties with safe harbor 2
     */
    @Query("SELECT e FROM EstimatedTaxPenalty e WHERE e.tenantId = :tenantId " +
           "AND e.safeHarbor2Met = true")
    List<EstimatedTaxPenalty> findWithSafeHarbor2(@Param("tenantId") UUID tenantId);

    /**
     * Calculate total penalty amount for a tax year.
     *
     * @param taxYear  the tax year
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Sum of total penalties for the year
     */
    @Query("SELECT COALESCE(SUM(e.totalPenalty), 0) FROM EstimatedTaxPenalty e " +
           "WHERE e.taxYear = :taxYear AND e.tenantId = :tenantId")
    java.math.BigDecimal calculateTotalPenaltyForYear(
            @Param("taxYear") Integer taxYear,
            @Param("tenantId") UUID tenantId);

    /**
     * Find penalties with total penalty greater than threshold.
     *
     * @param threshold the minimum penalty amount
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @return List of penalties above threshold
     */
    @Query("SELECT e FROM EstimatedTaxPenalty e WHERE e.tenantId = :tenantId " +
           "AND e.totalPenalty > :threshold ORDER BY e.totalPenalty DESC")
    List<EstimatedTaxPenalty> findWithPenaltyAbove(
            @Param("threshold") java.math.BigDecimal threshold,
            @Param("tenantId") UUID tenantId);

    /**
     * Check if penalty exists for a return.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return true if penalty exists
     */
    boolean existsByReturnIdAndTenantId(UUID returnId, UUID tenantId);

    /**
     * Find penalty by ID with tenant isolation.
     *
     * @param id       the penalty ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the penalty if found
     */
    Optional<EstimatedTaxPenalty> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Count penalties for a tax year.
     *
     * @param taxYear  the tax year
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Count of penalties
     */
    long countByTaxYearAndTenantId(Integer taxYear, UUID tenantId);

    /**
     * Find penalties using standard calculation method.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of penalties using standard method
     */
    @Query("SELECT e FROM EstimatedTaxPenalty e WHERE e.tenantId = :tenantId " +
           "AND e.calculationMethod = 'STANDARD'")
    List<EstimatedTaxPenalty> findUsingStandardMethod(@Param("tenantId") UUID tenantId);

    /**
     * Find penalties using annualized income method.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of penalties using annualized income method
     */
    @Query("SELECT e FROM EstimatedTaxPenalty e WHERE e.tenantId = :tenantId " +
           "AND e.calculationMethod = 'ANNUALIZED_INCOME'")
    List<EstimatedTaxPenalty> findUsingAnnualizedIncomeMethod(@Param("tenantId") UUID tenantId);
}
