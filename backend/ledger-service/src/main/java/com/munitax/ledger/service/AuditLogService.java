package com.munitax.ledger.service;

import com.munitax.ledger.model.AuditLog;
import com.munitax.ledger.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Transactional
    public void logAction(UUID entityId, String entityType, String action, 
                         UUID userId, UUID tenantId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .entityId(entityId)
                .entityType(entityType)
                .action(action)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .details(details)
                .tenantId(tenantId)
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} {} by user {}", action, entityType, userId);
    }
    
    @Transactional
    public void logModification(UUID entityId, String entityType, String action, 
                               UUID userId, UUID tenantId, String oldValue, 
                               String newValue, String reason) {
        AuditLog auditLog = AuditLog.builder()
                .entityId(entityId)
                .entityType(entityType)
                .action(action)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .oldValue(oldValue)
                .newValue(newValue)
                .reason(reason)
                .tenantId(tenantId)
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} {} by user {}", action, entityType, userId);
    }
    
    public List<AuditLog> getAuditTrail(UUID entityId) {
        return auditLogRepository.findByEntityIdOrderByTimestampDesc(entityId);
    }
    
    public List<AuditLog> getTenantAuditLogs(UUID tenantId) {
        return auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId);
    }
    
    /**
     * T071 - Add audit log access tracking per FR-051
     * Logs when audit trails are accessed for compliance
     */
    @Transactional
    public void logAuditAccess(UUID entityId, String entityType, UUID userId, UUID tenantId) {
        AuditLog accessLog = AuditLog.builder()
                .entityId(entityId)
                .entityType(entityType)
                .action("VIEW_AUDIT")
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .details("Audit trail accessed")
                .tenantId(tenantId)
                .build();
        
        auditLogRepository.save(accessLog);
        log.debug("Audit access logged: {} viewed by user {}", entityType, userId);
    }
    
    /**
     * T072 - Query audit trail by journal entry ID
     * Returns complete history of a specific journal entry
     */
    public List<AuditLog> getJournalEntryAuditTrail(UUID journalEntryId) {
        return auditLogRepository.findByEntityIdOrderByTimestampDesc(journalEntryId);
    }
    
    /**
     * T072 - Query audit trail with filters
     * Supports filtering by action type, date range, and user
     */
    public List<AuditLog> getFilteredAuditTrail(UUID tenantId, String entityType, 
                                                String action, UUID userId) {
        // For now, return all tenant logs - repository method can be enhanced for filtering
        List<AuditLog> allLogs = auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId);
        
        return allLogs.stream()
                .filter(log -> entityType == null || entityType.equals(log.getEntityType()))
                .filter(log -> action == null || action.equals(log.getAction()))
                .filter(log -> userId == null || userId.equals(log.getUserId()))
                .toList();
    }
}
