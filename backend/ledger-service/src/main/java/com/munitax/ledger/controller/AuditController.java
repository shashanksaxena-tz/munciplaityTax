package com.munitax.ledger.controller;

import com.munitax.ledger.model.AuditLog;
import com.munitax.ledger.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {
    
    private final AuditLogService auditLogService;
    
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<AuditLog>> getAuditTrail(@PathVariable UUID entityId) {
        List<AuditLog> auditLogs = auditLogService.getAuditTrail(entityId);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<AuditLog>> getTenantAuditLogs(@PathVariable UUID tenantId) {
        List<AuditLog> auditLogs = auditLogService.getTenantAuditLogs(tenantId);
        return ResponseEntity.ok(auditLogs);
    }
}
