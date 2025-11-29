package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.penalty.PenaltyAuditLog;
import com.munitax.taxengine.domain.penalty.PenaltyAuditAction;
import com.munitax.taxengine.domain.penalty.PenaltyAuditEntityType;
import com.munitax.taxengine.domain.penalty.ActorRole;
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
 * Repository interface for PenaltyAuditLog entity operations.
 * Provides data access methods for immutable audit trail.
 * 
 * IMPORTANT: This is an INSERT-only repository - no UPDATE or DELETE operations.
 * Multi-tenant isolation: All queries MUST filter by tenant_id.
 */
@Repository
public interface PenaltyAuditLogRepository extends JpaRepository<PenaltyAuditLog, UUID> {

    /**
     * Find all audit log entries for an entity with pagination.
     *
     * @param entityType the entity type
     * @param entityId   the entity ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @param pageable   pagination information
     * @return Page of audit log entries ordered by timestamp descending
     */
    Page<PenaltyAuditLog> findByEntityTypeAndEntityIdAndTenantIdOrderByCreatedAtDesc(
            PenaltyAuditEntityType entityType, UUID entityId, UUID tenantId, Pageable pageable);

    /**
     * Find all audit log entries for an entity.
     *
     * @param entityType the entity type
     * @param entityId   the entity ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return List of audit log entries ordered by timestamp
     */
    List<PenaltyAuditLog> findByEntityTypeAndEntityIdAndTenantIdOrderByCreatedAt(
            PenaltyAuditEntityType entityType, UUID entityId, UUID tenantId);

    /**
     * Find audit log entries by action type.
     *
     * @param entityType the entity type
     * @param entityId   the entity ID
     * @param action     the audit action
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return List of audit log entries of the specified action
     */
    List<PenaltyAuditLog> findByEntityTypeAndEntityIdAndActionAndTenantIdOrderByCreatedAtDesc(
            PenaltyAuditEntityType entityType, UUID entityId, PenaltyAuditAction action, UUID tenantId);

    /**
     * Find audit log entries by actor.
     *
     * @param actorId  the actor user ID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of audit log entries made by the actor
     */
    Page<PenaltyAuditLog> findByActorIdAndTenantIdOrderByCreatedAtDesc(
            UUID actorId, UUID tenantId, Pageable pageable);

    /**
     * Find audit log entries by actor role.
     *
     * @param actorRole the actor role
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @param pageable  pagination information
     * @return Page of audit log entries by role
     */
    Page<PenaltyAuditLog> findByActorRoleAndTenantIdOrderByCreatedAtDesc(
            ActorRole actorRole, UUID tenantId, Pageable pageable);

    /**
     * Find audit log entries within a date range.
     *
     * @param tenantId  the tenant ID for multi-tenant isolation
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return List of audit log entries within the date range
     */
    @Query("SELECT a FROM PenaltyAuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<PenaltyAuditLog> findByTenantIdAndDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find all audit log entries for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of audit log entries
     */
    Page<PenaltyAuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    /**
     * Find recent audit log entries for an entity (last N days).
     *
     * @param entityType the entity type
     * @param entityId   the entity ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @param sinceDate  the cutoff date (entries after this date)
     * @return List of recent audit log entries
     */
    @Query("SELECT a FROM PenaltyAuditLog a WHERE a.entityType = :entityType " +
           "AND a.entityId = :entityId AND a.tenantId = :tenantId " +
           "AND a.createdAt >= :sinceDate ORDER BY a.createdAt DESC")
    List<PenaltyAuditLog> findRecentChanges(
            @Param("entityType") PenaltyAuditEntityType entityType,
            @Param("entityId") UUID entityId,
            @Param("tenantId") UUID tenantId,
            @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Count audit log entries for an entity.
     *
     * @param entityType the entity type
     * @param entityId   the entity ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Count of audit log entries
     */
    long countByEntityTypeAndEntityIdAndTenantId(
            PenaltyAuditEntityType entityType, UUID entityId, UUID tenantId);

    /**
     * Count audit log entries by action type for an entity.
     *
     * @param entityType the entity type
     * @param entityId   the entity ID
     * @param action     the audit action
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Count of audit log entries of the specified action
     */
    long countByEntityTypeAndEntityIdAndActionAndTenantId(
            PenaltyAuditEntityType entityType, UUID entityId, PenaltyAuditAction action, UUID tenantId);

    /**
     * Find system-generated audit entries.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of system-generated audit entries
     */
    @Query("SELECT a FROM PenaltyAuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.actorRole = 'SYSTEM' ORDER BY a.createdAt DESC")
    Page<PenaltyAuditLog> findSystemActions(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Find auditor actions.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of auditor actions
     */
    @Query("SELECT a FROM PenaltyAuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.actorRole = 'AUDITOR' ORDER BY a.createdAt DESC")
    Page<PenaltyAuditLog> findAuditorActions(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Find audit entries by entity type.
     *
     * @param entityType the entity type
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @param pageable   pagination information
     * @return Page of audit entries for the entity type
     */
    Page<PenaltyAuditLog> findByEntityTypeAndTenantIdOrderByCreatedAtDesc(
            PenaltyAuditEntityType entityType, UUID tenantId, Pageable pageable);

    /**
     * Find audit entries by action.
     *
     * @param action   the audit action
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param pageable pagination information
     * @return Page of audit entries for the action
     */
    Page<PenaltyAuditLog> findByActionAndTenantIdOrderByCreatedAtDesc(
            PenaltyAuditAction action, UUID tenantId, Pageable pageable);

    /**
     * Find create operations for an entity type.
     *
     * @param entityType the entity type
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return List of create operations
     */
    @Query("SELECT a FROM PenaltyAuditLog a WHERE a.entityType = :entityType " +
           "AND a.tenantId = :tenantId AND a.oldValue IS NULL ORDER BY a.createdAt DESC")
    List<PenaltyAuditLog> findCreateOperations(
            @Param("entityType") PenaltyAuditEntityType entityType,
            @Param("tenantId") UUID tenantId);

    /**
     * Find update operations for an entity type.
     *
     * @param entityType the entity type
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return List of update operations
     */
    @Query("SELECT a FROM PenaltyAuditLog a WHERE a.entityType = :entityType " +
           "AND a.tenantId = :tenantId AND a.oldValue IS NOT NULL ORDER BY a.createdAt DESC")
    List<PenaltyAuditLog> findUpdateOperations(
            @Param("entityType") PenaltyAuditEntityType entityType,
            @Param("tenantId") UUID tenantId);
}
