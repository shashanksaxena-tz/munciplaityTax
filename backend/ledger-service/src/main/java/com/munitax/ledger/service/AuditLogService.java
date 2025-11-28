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
}
