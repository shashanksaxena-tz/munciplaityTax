package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.apportionment.SalesFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SalesFactor entity operations.
 * Provides data access methods for sales factor calculations in apportionment.
 */
@Repository
public interface SalesFactorRepository extends JpaRepository<SalesFactor, UUID> {

    /**
     * Find sales factor by Schedule Y ID.
     *
     * @param scheduleYId the Schedule Y ID
     * @return Optional containing the sales factor if found
     */
       @Query("SELECT sf FROM SalesFactor sf WHERE sf.scheduleY.scheduleYId = :scheduleYId")
       Optional<SalesFactor> findByScheduleYId(@Param("scheduleYId") UUID scheduleYId);

    /**
     * Find all sales factors for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of sales factors
     */
    List<SalesFactor> findByTenantId(UUID tenantId);

    /**
     * Calculate total Ohio sales for a business across all filings.
     *
     * @param businessId the business ID (from Schedule Y relationship)
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Total Ohio sales
     */
    @Query("SELECT COALESCE(SUM(sf.totalOhioSales), 0) FROM SalesFactor sf " +
           "JOIN sf.scheduleY s WHERE s.businessId = :businessId AND sf.tenantId = :tenantId")
    BigDecimal sumOhioSalesByBusiness(
            @Param("businessId") UUID businessId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total everywhere sales for a business across all filings.
     *
     * @param businessId the business ID (from Schedule Y relationship)
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Total everywhere sales
     */
    @Query("SELECT COALESCE(SUM(sf.totalSalesEverywhere), 0) FROM SalesFactor sf " +
           "JOIN sf.scheduleY s WHERE s.businessId = :businessId AND sf.tenantId = :tenantId")
    BigDecimal sumTotalSalesByBusiness(
            @Param("businessId") UUID businessId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find sales factors with throwback adjustments.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of sales factors with non-zero throwback adjustments
     */
    @Query("SELECT sf FROM SalesFactor sf WHERE sf.throwbackAdjustment <> 0 " +
           "AND sf.tenantId = :tenantId")
    List<SalesFactor> findWithThrowbackAdjustments(@Param("tenantId") UUID tenantId);

    /**
     * Calculate total service revenue for a business.
     *
     * @param businessId the business ID (from Schedule Y relationship)
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Total service revenue
     */
    @Query("SELECT COALESCE(SUM(sf.ohioSalesServices), 0) FROM SalesFactor sf " +
           "JOIN sf.scheduleY s WHERE s.businessId = :businessId AND sf.tenantId = :tenantId")
    BigDecimal sumServiceRevenueByBusiness(
            @Param("businessId") UUID businessId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate average sales factor percentage for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Average sales factor percentage
     */
    @Query("SELECT AVG(sf.salesFactorPercentage) FROM SalesFactor sf " +
           "WHERE sf.tenantId = :tenantId")
    BigDecimal calculateAverageSalesFactor(@Param("tenantId") UUID tenantId);
}
