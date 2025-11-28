package com.munitax.submission.repository;

import com.munitax.submission.model.AuditAction;
import com.munitax.submission.model.AuditAction.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditActionRepository extends JpaRepository<AuditAction, String> {
    
    // Find all actions for a return (audit history)
    List<AuditAction> findByReturnIdOrderByActionDateDesc(String returnId);
    Page<AuditAction> findByReturnIdOrderByActionDateDesc(String returnId, Pageable pageable);
    
    // Find actions by auditor
    List<AuditAction> findByAuditorIdOrderByActionDateDesc(String auditorId);
    Page<AuditAction> findByAuditorIdOrderByActionDateDesc(String auditorId, Pageable pageable);
    
    // Find actions by type
    List<AuditAction> findByActionType(ActionType actionType);
    
    // Find actions by return and type
    List<AuditAction> findByReturnIdAndActionType(String returnId, ActionType actionType);
    
    // Find actions in date range
    @Query("SELECT aa FROM AuditAction aa WHERE aa.actionDate >= :startDate AND aa.actionDate <= :endDate")
    List<AuditAction> findByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    // Find actions by tenant
    List<AuditAction> findByTenantId(String tenantId);
    
    // Count actions by type
    long countByActionType(ActionType actionType);
    
    // Count actions by auditor
    long countByAuditorId(String auditorId);
}
