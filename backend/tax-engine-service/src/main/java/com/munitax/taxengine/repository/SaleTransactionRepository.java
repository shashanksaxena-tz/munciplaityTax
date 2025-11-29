package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.apportionment.SaleTransaction;
import com.munitax.taxengine.domain.apportionment.SaleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for SaleTransaction entity operations.
 * Provides data access methods for individual sale transactions in apportionment.
 */
@Repository
public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, UUID> {

    /**
     * Find all sale transactions for a sales factor.
     *
     * @param salesFactorId the sales factor ID
     * @return List of sale transactions
     */
    List<SaleTransaction> findBySalesFactorId(UUID salesFactorId);

    /**
     * Find sale transactions for a sales factor with pagination.
     *
     * @param salesFactorId the sales factor ID
     * @param pageable      pagination information
     * @return Page of sale transactions
     */
    Page<SaleTransaction> findBySalesFactorId(UUID salesFactorId, Pageable pageable);

    /**
     * Find all sale transactions for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of sale transactions
     */
    List<SaleTransaction> findByTenantId(UUID tenantId);

    /**
     * Find sale transactions by sale type.
     *
     * @param salesFactorId the sales factor ID
     * @param saleType      the type of sale
     * @return List of sale transactions of the specified type
     */
    List<SaleTransaction> findBySalesFactorIdAndSaleType(UUID salesFactorId, SaleType saleType);

    /**
     * Find sale transactions with throwback applied.
     *
     * @param salesFactorId the sales factor ID
     * @return List of sale transactions where throwback was applied
     */
    @Query("SELECT st FROM SaleTransaction st WHERE st.salesFactorId = :salesFactorId " +
           "AND st.throwbackApplied = true")
    List<SaleTransaction> findThrowbackTransactions(@Param("salesFactorId") UUID salesFactorId);

    /**
     * Calculate total sales for a specific destination state.
     *
     * @param salesFactorId    the sales factor ID
     * @param destinationState the destination state code
     * @return Total sales to the destination state
     */
    @Query("SELECT COALESCE(SUM(st.amount), 0) FROM SaleTransaction st " +
           "WHERE st.salesFactorId = :salesFactorId AND st.destinationState = :state")
    BigDecimal sumSalesByDestinationState(
            @Param("salesFactorId") UUID salesFactorId,
            @Param("state") String destinationState);

    /**
     * Calculate total throwback adjustments for a sales factor.
     *
     * @param salesFactorId the sales factor ID
     * @return Total throwback amount
     */
    @Query("SELECT COALESCE(SUM(st.throwbackAmount), 0) FROM SaleTransaction st " +
           "WHERE st.salesFactorId = :salesFactorId AND st.throwbackApplied = true")
    BigDecimal sumThrowbackAdjustments(@Param("salesFactorId") UUID salesFactorId);

    /**
     * Count transactions by sale type for a sales factor.
     *
     * @param salesFactorId the sales factor ID
     * @param saleType      the type of sale
     * @return Count of transactions of the specified type
     */
    @Query("SELECT COUNT(st) FROM SaleTransaction st " +
           "WHERE st.salesFactorId = :salesFactorId AND st.saleType = :saleType")
    long countBySaleType(
            @Param("salesFactorId") UUID salesFactorId,
            @Param("saleType") SaleType saleType);

    /**
     * Find service transactions requiring market-based sourcing.
     *
     * @param salesFactorId the sales factor ID
     * @return List of service sale transactions
     */
    @Query("SELECT st FROM SaleTransaction st WHERE st.salesFactorId = :salesFactorId " +
           "AND st.saleType = 'SERVICE' AND st.customerLocation IS NOT NULL")
    List<SaleTransaction> findServiceTransactions(@Param("salesFactorId") UUID salesFactorId);
}
