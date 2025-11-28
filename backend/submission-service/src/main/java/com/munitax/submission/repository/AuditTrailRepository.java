package com.munitax.submission.repository;

import com.munitax.submission.model.AuditTrail;
import com.munitax.submission.model.AuditTrail.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, String> {
    
    // Find all trail entries for a return (complete history)
    List<AuditTrail> findByReturnIdOrderByTimestampDesc(String returnId);
    Page<AuditTrail> findByReturnIdOrderByTimestampDesc(String returnId, Pageable pageable);
    
    // Find by event type
    List<AuditTrail> findByEventType(EventType eventType);
    
    // Find by user
    List<AuditTrail> findByUserIdOrderByTimestampDesc(String userId);
    
    // Find by return and event type
    List<AuditTrail> findByReturnIdAndEventType(String returnId, EventType eventType);
    
    // Find in date range
    @Query("SELECT at FROM AuditTrail at WHERE at.timestamp >= :startDate AND at.timestamp <= :endDate ORDER BY at.timestamp DESC")
    List<AuditTrail> findByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    // Find by tenant
    List<AuditTrail> findByTenantIdOrderByTimestampDesc(String tenantId);
    Page<AuditTrail> findByTenantIdOrderByTimestampDesc(String tenantId, Pageable pageable);
    
    // Find entries with digital signatures (approvals)
    @Query("SELECT at FROM AuditTrail at WHERE at.digitalSignature IS NOT NULL")
    List<AuditTrail> findEntriesWithDigitalSignature();
    
    // Count events by type
    long countByEventType(EventType eventType);
    
    // Note: No delete or update operations - audit trail is immutable/append-only
}
