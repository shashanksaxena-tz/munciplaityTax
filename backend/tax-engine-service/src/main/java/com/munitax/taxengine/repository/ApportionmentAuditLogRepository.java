package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.apportionment.ApportionmentAuditLog;
import com.munitax.taxengine.domain.apportionment.AuditChangeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ApportionmentAuditLog entity operations.
 * Provides data access methods for immutable audit trail of apportionment changes.
 * Note: This is an INSERT-only repository - no UPDATE or DELETE operations.
 */
@Repository
public interface ApportionmentAuditLogRepository extends JpaRepository<ApportionmentAuditLog, UUID> {

    /**
     * Find all audit log entries for a Schedule Y filing with pagination.
     *
     * @param scheduleYId the Schedule Y ID
     * @param pageable    pagination information
     * @return Page of audit log entries ordered by timestamp descending
     */
    Page<ApportionmentAuditLog> findByScheduleYIdOrderByChangedAtDesc(
            UUID scheduleYId, Pageable pageable);

    /**
     * Find all audit log entries for a Schedule Y filing.
     *
     * @param scheduleYId the Schedule Y ID
     * @return List of audit log entries ordered by timestamp
     */
    List<ApportionmentAuditLog> findByScheduleYIdOrderByChangedAt(UUID scheduleYId);

    /**
     * Find audit log entries by change type.
     *
     * @param scheduleYId the Schedule Y ID
     * @param changeType  the type of change
     * @return List of audit log entries of the specified type
     */
    List<ApportionmentAuditLog> findByScheduleYIdAndChangeTypeOrderByChangedAtDesc(
            UUID scheduleYId, AuditChangeType changeType);

    /**
     * Find audit log entries by user.
     *
     * @param changedBy the user ID who made the change
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @param pageable  pagination information
     * @return Page of audit log entries made by the user
     */
    Page<ApportionmentAuditLog> findByChangedByAndTenantIdOrderByChangedAtDesc(
            UUID changedBy, UUID tenantId, Pageable pageable);

    /**
     * Find audit log entries within a date range.
     *
     * @param scheduleYId the Schedule Y ID
     * @param startDate   the start date (inclusive)
     * @param endDate     the end date (inclusive)
     * @return List of audit log entries within the date range
     */
    @Query("SELECT a FROM ApportionmentAuditLog a WHERE a.scheduleYId = :scheduleYId " +
           "AND a.changedAt BETWEEN :startDate AND :endDate ORDER BY a.changedAt DESC")
    List<ApportionmentAuditLog> findByScheduleYIdAndDateRange(
            @Param("scheduleYId") UUID scheduleYId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find all audit log entries for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of audit log entries
     */
    Page<ApportionmentAuditLog> findByTenantIdOrderByChangedAtDesc(UUID tenantId, Pageable pageable);

    /**
     * Find recent audit log entries for a Schedule Y (last N days).
     *
     * @param scheduleYId the Schedule Y ID
     * @param sinceDate   the cutoff date (entries after this date)
     * @return List of recent audit log entries
     */
    @Query("SELECT a FROM ApportionmentAuditLog a WHERE a.scheduleYId = :scheduleYId " +
           "AND a.changedAt >= :sinceDate ORDER BY a.changedAt DESC")
    List<ApportionmentAuditLog> findRecentChanges(
            @Param("scheduleYId") UUID scheduleYId,
            @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Count audit log entries for a Schedule Y.
     *
     * @param scheduleYId the Schedule Y ID
     * @return Count of audit log entries
     */
    long countByScheduleYId(UUID scheduleYId);

    /**
     * Count audit log entries by change type for a Schedule Y.
     *
     * @param scheduleYId the Schedule Y ID
     * @param changeType  the type of change
     * @return Count of audit log entries of the specified type
     */
    long countByScheduleYIdAndChangeType(UUID scheduleYId, AuditChangeType changeType);
}
