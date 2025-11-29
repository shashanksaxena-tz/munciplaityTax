package com.munitax.pdf.repository;

import com.munitax.pdf.domain.FormAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for FormAuditLog entity
 */
@Repository
public interface FormAuditLogRepository extends JpaRepository<FormAuditLog, UUID> {

    /**
     * Find audit logs by generated form
     */
    List<FormAuditLog> findByGeneratedFormIdOrderByEventTimestampDesc(UUID generatedFormId);

    /**
     * Find audit logs by filing package
     */
    List<FormAuditLog> findByPackageIdOrderByEventTimestampDesc(UUID packageId);

    /**
     * Find audit logs by tenant
     */
    List<FormAuditLog> findByTenantIdOrderByEventTimestampDesc(String tenantId);

    /**
     * Find audit logs by event type
     */
    List<FormAuditLog> findByTenantIdAndEventTypeOrderByEventTimestampDesc(String tenantId, String eventType);

    /**
     * Find audit logs by actor
     */
    List<FormAuditLog> findByActorIdOrderByEventTimestampDesc(String actorId);
}
