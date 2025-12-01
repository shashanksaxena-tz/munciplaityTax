package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.apportionment.ScheduleY;
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
 * Repository interface for ScheduleY entity operations.
 * Provides data access methods for Schedule Y multi-state sourcing filings.
 */
@Repository
public interface ScheduleYRepository extends JpaRepository<ScheduleY, UUID> {

    /**
     * Find Schedule Y by return ID and tenant ID.
     *
     * @param returnId the return ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Optional containing the Schedule Y if found
     */
    Optional<ScheduleY> findByReturnIdAndTenantId(UUID returnId, UUID tenantId);

    /**
     * Find all Schedule Y filings for a business with pagination.
     *
     * @param businessId the business ID
     * @param returnId   the tax return ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @param pageable   pagination information
     * @return Page of Schedule Y filings
     */
    Page<ScheduleY> findByReturnIdAndTenantIdOrderByTaxYearDesc(
            UUID returnId, UUID tenantId, Pageable pageable);

    /**
     * Find Schedule Y by return ID, tax year, and tenant ID.
     *
     * @param returnId   the tax return ID
     * @param taxYear    the tax year
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Optional containing the Schedule Y if found
     */
    Optional<ScheduleY> findByReturnIdAndTaxYearAndTenantId(
            UUID returnId, Integer taxYear, UUID tenantId);

    /**
     * Find all Schedule Y filings for a tax year with pagination.
     *
     * @param taxYear  the tax year
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of Schedule Y filings
     */
    Page<ScheduleY> findByTaxYearAndTenantId(Integer taxYear, UUID tenantId, Pageable pageable);

    /**
     * Check if a Schedule Y filing exists for a return and tax year.
     *
     * @param returnId   the return ID
     * @param taxYear    the tax year
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return true if a filing exists
     */
    boolean existsByReturnIdAndTaxYearAndTenantId(UUID returnId, Integer taxYear, UUID tenantId);

    /**
     * Find all Schedule Y filings with a specific sourcing method election.
     *
     * @param sourcingMethodElection the sourcing method election
     * @param tenantId               the tenant ID for multi-tenant isolation
     * @return List of Schedule Y filings
     */
    @Query("SELECT s FROM ScheduleY s WHERE s.sourcingMethodElection = :election AND s.tenantId = :tenantId")
    List<ScheduleY> findBySourcingMethodElection(
            @Param("election") String sourcingMethodElection,
            @Param("tenantId") UUID tenantId);

    /**
     * Find Schedule Y filings requiring audit review.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of Schedule Y filings flagged for audit
     */
    @Query("SELECT s FROM ScheduleY s WHERE s.tenantId = :tenantId " +
           "AND (s.finalApportionmentPercentage > 0.95 OR s.finalApportionmentPercentage < 0.05)")
    List<ScheduleY> findFilingsRequiringAuditReview(@Param("tenantId") UUID tenantId);
}
