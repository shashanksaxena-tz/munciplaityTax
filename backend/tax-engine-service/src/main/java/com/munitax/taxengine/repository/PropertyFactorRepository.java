package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.apportionment.PropertyFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for PropertyFactor entity operations.
 * Provides data access methods for property factor calculations in apportionment.
 */
@Repository
public interface PropertyFactorRepository extends JpaRepository<PropertyFactor, UUID> {

    /**
     * Find property factor by Schedule Y ID.
     *
     * @param scheduleYId the Schedule Y ID
     * @return Optional containing the property factor if found
     */
    Optional<PropertyFactor> findByScheduleY_ScheduleYId(UUID scheduleYId);

    /**
     * Find all property factors for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of property factors
     */
    @Query("SELECT pf FROM PropertyFactor pf JOIN pf.scheduleY s WHERE s.tenantId = :tenantId")
    List<PropertyFactor> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Calculate total Ohio property value for a return.
     *
     * @param returnId   the return ID (from Schedule Y)
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Total Ohio property value
     */
    @Query("SELECT COALESCE(SUM(pf.totalOhioProperty), 0) FROM PropertyFactor pf " +
           "JOIN pf.scheduleY s WHERE s.returnId = :returnId AND s.tenantId = :tenantId")
    BigDecimal sumOhioPropertyValueByReturn(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total everywhere property value for a return.
     *
     * @param returnId   the return ID (from Schedule Y)
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Total everywhere property value
     */
    @Query("SELECT COALESCE(SUM(pf.totalPropertyEverywhere), 0) FROM PropertyFactor pf " +
           "JOIN pf.scheduleY s WHERE s.returnId = :returnId AND s.tenantId = :tenantId")
    BigDecimal sumTotalPropertyValueByReturn(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find property factors with significant rented property.
     *
     * @param threshold minimum rented property value to include
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @return List of property factors with rented property above threshold
     */
    @Query("SELECT pf FROM PropertyFactor pf JOIN pf.scheduleY s WHERE pf.ohioRentedPropertyRent >= :threshold " +
           "AND s.tenantId = :tenantId")
    List<PropertyFactor> findWithSignificantRentedProperty(
            @Param("threshold") BigDecimal threshold,
            @Param("tenantId") UUID tenantId);
}
