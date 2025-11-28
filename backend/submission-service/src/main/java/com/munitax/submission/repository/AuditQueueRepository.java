package com.munitax.submission.repository;

import com.munitax.submission.model.AuditQueue;
import com.munitax.submission.model.AuditQueue.AuditStatus;
import com.munitax.submission.model.AuditQueue.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditQueueRepository extends JpaRepository<AuditQueue, String> {
    
    // Find by return ID
    Optional<AuditQueue> findByReturnId(String returnId);
    
    // Find by status
    List<AuditQueue> findByStatus(AuditStatus status);
    Page<AuditQueue> findByStatus(AuditStatus status, Pageable pageable);
    
    // Find by priority
    List<AuditQueue> findByPriority(Priority priority);
    Page<AuditQueue> findByPriority(Priority priority, Pageable pageable);
    
    // Find by assigned auditor
    List<AuditQueue> findByAssignedAuditorId(String auditorId);
    Page<AuditQueue> findByAssignedAuditorId(String auditorId, Pageable pageable);
    
    // Find by tenant
    List<AuditQueue> findByTenantId(String tenantId);
    Page<AuditQueue> findByTenantId(String tenantId, Pageable pageable);
    
    // Complex queries for filtering
    @Query("SELECT aq FROM AuditQueue aq WHERE " +
           "(:status IS NULL OR aq.status = :status) AND " +
           "(:priority IS NULL OR aq.priority = :priority) AND " +
           "(:auditorId IS NULL OR aq.assignedAuditorId = :auditorId) AND " +
           "(:tenantId IS NULL OR aq.tenantId = :tenantId) AND " +
           "(:fromDate IS NULL OR aq.submissionDate >= :fromDate) AND " +
           "(:toDate IS NULL OR aq.submissionDate <= :toDate)")
    Page<AuditQueue> findWithFilters(
        @Param("status") AuditStatus status,
        @Param("priority") Priority priority,
        @Param("auditorId") String auditorId,
        @Param("tenantId") String tenantId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate,
        Pageable pageable
    );
    
    // Count by status
    long countByStatus(AuditStatus status);
    
    // Count by priority
    long countByPriority(Priority priority);
    
    // Find high-priority pending items
    @Query("SELECT aq FROM AuditQueue aq WHERE aq.status = :status AND aq.priority = :priority")
    List<AuditQueue> findHighPriorityPending(@Param("status") AuditStatus status, @Param("priority") Priority priority);
    
    // Find items overdue for review (in queue > 7 days)
    @Query("SELECT aq FROM AuditQueue aq WHERE aq.status = :status AND aq.submissionDate < :cutoffDate")
    List<AuditQueue> findOverdueItems(@Param("status") AuditStatus status, @Param("cutoffDate") Instant cutoffDate);
}
