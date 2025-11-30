package com.munitax.ledger.controller;

import com.munitax.ledger.model.AuditLog;
import com.munitax.ledger.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * T073 - Audit Controller
 * Provides endpoints for querying audit trails and accessing history
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {
    
    private final AuditLogService auditLogService;
    
    /**
     * Get audit trail for a specific entity
     */
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<AuditLog>> getAuditTrail(
            @PathVariable UUID entityId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID tenantId) {
        
        log.info("Getting audit trail for entity {}", entityId);
        
        // T071 - Log audit access if userId and tenantId provided
        if (userId != null && tenantId != null) {
            auditLogService.logAuditAccess(entityId, "ENTITY", userId, tenantId);
        }
        
        List<AuditLog> auditLogs = auditLogService.getAuditTrail(entityId);
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * Get all audit logs for a tenant
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<AuditLog>> getTenantAuditLogs(@PathVariable UUID tenantId) {
        log.info("Getting audit logs for tenant {}", tenantId);
        List<AuditLog> auditLogs = auditLogService.getTenantAuditLogs(tenantId);
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * T073 - Get audit trail for a specific journal entry
     * Returns complete history including creation, modifications, posting, reversals
     * 
     * @param entryId Journal entry UUID
     * @param userId User accessing the audit trail (for access logging)
     * @param tenantId Tenant ID (for access logging)
     * @return List of audit log entries sorted by timestamp descending
     */
    @GetMapping("/journal-entries/{entryId}")
    public ResponseEntity<List<AuditLog>> getJournalEntryAuditTrail(
            @PathVariable UUID entryId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID tenantId) {
        
        log.info("Getting audit trail for journal entry {}", entryId);
        
        // T071 - Log audit access
        if (userId != null && tenantId != null) {
            auditLogService.logAuditAccess(entryId, "JOURNAL_ENTRY", userId, tenantId);
        }
        
        List<AuditLog> auditTrail = auditLogService.getJournalEntryAuditTrail(entryId);
        
        log.info("Retrieved {} audit log entries for journal entry {}", auditTrail.size(), entryId);
        
        return ResponseEntity.ok(auditTrail);
    }
    
    /**
     * T072 - Get filtered audit trail
     * Supports filtering by entity type, action, and user
     * 
     * @param tenantId Tenant ID (required)
     * @param entityType Filter by entity type (optional, e.g., "JOURNAL_ENTRY", "PAYMENT")
     * @param action Filter by action (optional, e.g., "CREATE", "UPDATE", "REVERSE")
     * @param userId Filter by user (optional)
     * @return Filtered list of audit logs
     */
    @GetMapping("/filtered")
    public ResponseEntity<List<AuditLog>> getFilteredAuditTrail(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID userId) {
        
        log.info("Getting filtered audit trail for tenant {} with filters: entityType={}, action={}, userId={}", 
                 tenantId, entityType, action, userId);
        
        List<AuditLog> filteredLogs = auditLogService.getFilteredAuditTrail(
                tenantId, entityType, action, userId);
        
        log.info("Retrieved {} filtered audit log entries", filteredLogs.size());
        
        return ResponseEntity.ok(filteredLogs);
    }
}
