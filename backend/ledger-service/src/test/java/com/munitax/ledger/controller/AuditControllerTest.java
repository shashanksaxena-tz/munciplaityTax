package com.munitax.ledger.controller;

import com.munitax.ledger.model.AuditLog;
import com.munitax.ledger.service.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@DisplayName("AuditController Integration Tests")
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    @DisplayName("T068 - GET /api/v1/audit/entity/{entityId} should return audit trail")
    void shouldGetAuditTrailByEntity() throws Exception {
        // Given
        UUID entityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        AuditLog log1 = createAuditLog(entityId, userId, tenantId, "CREATE", LocalDateTime.now().minusHours(2));
        AuditLog log2 = createAuditLog(entityId, userId, tenantId, "UPDATE", LocalDateTime.now().minusHours(1));
        AuditLog log3 = createAuditLog(entityId, userId, tenantId, "POSTED", LocalDateTime.now());

        List<AuditLog> auditTrail = Arrays.asList(log3, log2, log1);

        when(auditLogService.getAuditTrail(entityId)).thenReturn(auditTrail);

        // When/Then
        mockMvc.perform(get("/api/v1/audit/entity/" + entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].action").value("POSTED"))
                .andExpect(jsonPath("$[1].action").value("UPDATE"))
                .andExpect(jsonPath("$[2].action").value("CREATE"))
                .andExpect(jsonPath("$[0].entityId").value(entityId.toString()));
    }

    @Test
    @DisplayName("T068 - GET /api/v1/audit/entity/{entityId} should return empty list for no logs")
    void shouldReturnEmptyListForEntityWithNoLogs() throws Exception {
        // Given
        UUID entityId = UUID.randomUUID();
        when(auditLogService.getAuditTrail(entityId)).thenReturn(Arrays.asList());

        // When/Then
        mockMvc.perform(get("/api/v1/audit/entity/" + entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("T068 - GET /api/v1/audit/tenant/{tenantId} should return all tenant audit logs")
    void shouldGetTenantAuditLogs() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID entity1 = UUID.randomUUID();
        UUID entity2 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AuditLog log1 = createAuditLog(entity1, userId, tenantId, "CREATE", LocalDateTime.now().minusHours(3));
        AuditLog log2 = createAuditLog(entity2, userId, tenantId, "CREATE", LocalDateTime.now().minusHours(2));
        AuditLog log3 = createAuditLog(entity1, userId, tenantId, "UPDATE", LocalDateTime.now());

        List<AuditLog> tenantLogs = Arrays.asList(log3, log2, log1);

        when(auditLogService.getTenantAuditLogs(tenantId)).thenReturn(tenantLogs);

        // When/Then
        mockMvc.perform(get("/api/v1/audit/tenant/" + tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$[1].tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$[2].tenantId").value(tenantId.toString()));
    }

    @Test
    @DisplayName("T068 - GET /api/v1/audit/entity/{entityId} should return audit logs with modifications")
    void shouldGetAuditLogsWithModifications() throws Exception {
        // Given
        UUID entityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        AuditLog createLog = createAuditLog(entityId, userId, tenantId, "CREATE", LocalDateTime.now().minusHours(2));
        
        AuditLog modifyLog = createAuditLog(entityId, userId, tenantId, "UPDATE", LocalDateTime.now());
        modifyLog.setOldValue("DRAFT");
        modifyLog.setNewValue("POSTED");
        modifyLog.setReason("Posted by finance officer");

        List<AuditLog> auditTrail = Arrays.asList(modifyLog, createLog);

        when(auditLogService.getAuditTrail(entityId)).thenReturn(auditTrail);

        // When/Then
        mockMvc.perform(get("/api/v1/audit/entity/" + entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].action").value("UPDATE"))
                .andExpect(jsonPath("$[0].oldValue").value("DRAFT"))
                .andExpect(jsonPath("$[0].newValue").value("POSTED"))
                .andExpect(jsonPath("$[0].reason").value("Posted by finance officer"))
                .andExpect(jsonPath("$[1].action").value("CREATE"));
    }

    @Test
    @DisplayName("T068 - Should handle UUID path variables correctly")
    void shouldHandleUuidPathVariables() throws Exception {
        // Given
        UUID entityId = UUID.randomUUID();
        when(auditLogService.getAuditTrail(entityId)).thenReturn(Arrays.asList());

        // When/Then
        mockMvc.perform(get("/api/v1/audit/entity/" + entityId))
                .andExpect(status().isOk());
    }

    private AuditLog createAuditLog(UUID entityId, UUID userId, UUID tenantId, 
                                   String action, LocalDateTime timestamp) {
        return AuditLog.builder()
                .auditId(UUID.randomUUID())
                .entityId(entityId)
                .entityType("JOURNAL_ENTRY")
                .action(action)
                .userId(userId)
                .tenantId(tenantId)
                .timestamp(timestamp)
                .details("Test audit log for " + action)
                .build();
    }
}
