package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.withholding.WithholdingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for WithholdingAuditLog entity.
 * 
 * Functional Requirements:
 * - Constitution III: Audit Trail Immutability
 * - FR-010: Filing history with audit trail
 * 
 * Query Methods:
 * - Find audit logs by entity type and ID
 * - Find audit logs by actor (user)
 * - Find audit logs within date range
 * 
 * Note: This repository is READ-ONLY after initial insert.
 * Audit logs MUST NOT be updated or deleted (Constitution Principle III).
 * 
 * @see WithholdingAuditLog
 */
@Repository
public interface WithholdingAuditLogRepository extends JpaRepository<WithholdingAuditLog, UUID> {
    
    /**
     * Find all audit logs for a specific entity (e.g., all actions on a W-1 filing).
     * Ordered by created_at descending (most recent first).
     * 
     * @param tenantId Tenant ID (for multi-tenant isolation)
     * @param entityType Entity type (W1_FILING, RECONCILIATION, CUMULATIVE_TOTALS, PAYMENT)
     * @param entityId Entity ID
     * @return List of audit logs for this entity
     */
    List<WithholdingAuditLog> findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
        UUID tenantId,
        String entityType,
        UUID entityId
    );
    
    /**
     * Find all audit logs for a specific actor (user).
     * Used for user activity reports and security audits.
     * 
     * @param tenantId Tenant ID (for multi-tenant isolation)
     * @param actorId User ID who performed the action
     * @return List of audit logs for this actor
     */
    List<WithholdingAuditLog> findByTenantIdAndActorIdOrderByCreatedAtDesc(
        UUID tenantId,
        UUID actorId
    );
    
    /**
     * Find all audit logs within a date range.
     * Used for compliance reports and audit investigations.
     * 
     * @param tenantId Tenant ID (for multi-tenant isolation)
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of audit logs within date range
     */
    @Query("""
        SELECT a FROM WithholdingAuditLog a
        WHERE a.tenantId = :tenantId
        AND a.createdAt >= :startDate
        AND a.createdAt <= :endDate
        ORDER BY a.createdAt DESC
        """)
    List<WithholdingAuditLog> findByDateRange(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find all audit logs for a specific action type.
     * Used for tracking specific operations (e.g., all AMENDED filings).
     * 
     * @param tenantId Tenant ID (for multi-tenant isolation)
     * @param action Action type (FILED, AMENDED, RECONCILED, PAYMENT_RECEIVED, etc.)
     * @return List of audit logs for this action
     */
    List<WithholdingAuditLog> findByTenantIdAndActionOrderByCreatedAtDesc(
        UUID tenantId,
        String action
    );
}
