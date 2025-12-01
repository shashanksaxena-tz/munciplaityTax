package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.apportionment.PayrollFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for PayrollFactor entity operations.
 * Provides data access methods for payroll factor calculations in apportionment.
 */
@Repository
public interface PayrollFactorRepository extends JpaRepository<PayrollFactor, UUID> {

    /**
     * Find payroll factor by Schedule Y ID.
     *
     * @param scheduleYId the Schedule Y ID
     * @return Optional containing the payroll factor if found
     */
    Optional<PayrollFactor> findByScheduleY_ScheduleYId(UUID scheduleYId);

    /**
     * Find all payroll factors for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of payroll factors
     */
    @Query("SELECT pf FROM PayrollFactor pf JOIN pf.scheduleY s WHERE s.tenantId = :tenantId")
    List<PayrollFactor> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Calculate total Ohio payroll for a return.
     *
     * @param returnId   the return ID (from Schedule Y)
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Total Ohio payroll
     */
    @Query("SELECT COALESCE(SUM(pf.totalOhioPayroll), 0) FROM PayrollFactor pf " +
           "JOIN pf.scheduleY s WHERE s.returnId = :returnId AND s.tenantId = :tenantId")
    BigDecimal sumOhioPayrollByReturn(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Calculate total everywhere payroll for a return.
     *
     * @param returnId   the return ID (from Schedule Y)
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Total everywhere payroll
     */
    @Query("SELECT COALESCE(SUM(pf.totalPayrollEverywhere), 0) FROM PayrollFactor pf " +
           "JOIN pf.scheduleY s WHERE s.returnId = :returnId AND s.tenantId = :tenantId")
    BigDecimal sumTotalPayrollByReturn(
            @Param("returnId") UUID returnId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find payroll factors for businesses with remote employees.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of payroll factors with remote employee allocation
     */
    @Query("SELECT pf FROM PayrollFactor pf JOIN pf.scheduleY s WHERE SIZE(pf.remoteEmployeeAllocation) > 0 " +
           "AND s.tenantId = :tenantId")
    List<PayrollFactor> findWithRemoteEmployees(@Param("tenantId") UUID tenantId);

    /**
     * Calculate average payroll factor percentage for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return Average payroll factor percentage
     */
    @Query("SELECT AVG(pf.payrollFactorPercentage) FROM PayrollFactor pf " +
           "JOIN pf.scheduleY s WHERE s.tenantId = :tenantId")
    BigDecimal calculateAveragePayrollFactor(@Param("tenantId") UUID tenantId);
}
