package com.munitax.ledger.service;

import com.munitax.ledger.model.AuditLog;
import com.munitax.ledger.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Tests")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private UUID entityId;
    private UUID userId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        entityId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("T066 - Should log action with all required fields")
    void shouldLogActionWithAllFields() {
        // Given
        String entityType = "JOURNAL_ENTRY";
        String action = "CREATE";
        String details = "Created journal entry for tax assessment";

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditLogService.logAction(entityId, entityType, action, userId, tenantId, details);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getEntityId()).isEqualTo(entityId);
        assertThat(savedLog.getEntityType()).isEqualTo(entityType);
        assertThat(savedLog.getAction()).isEqualTo(action);
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getTenantId()).isEqualTo(tenantId);
        assertThat(savedLog.getDetails()).isEqualTo(details);
        assertThat(savedLog.getTimestamp()).isNotNull();
        assertThat(savedLog.getTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("T066 - Should log modification with old and new values")
    void shouldLogModificationWithValues() {
        // Given
        String entityType = "JOURNAL_ENTRY";
        String action = "UPDATE";
        String oldValue = "DRAFT";
        String newValue = "POSTED";
        String reason = "Posted by finance officer";

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditLogService.logModification(entityId, entityType, action, userId, tenantId, 
                                       oldValue, newValue, reason);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getEntityId()).isEqualTo(entityId);
        assertThat(savedLog.getEntityType()).isEqualTo(entityType);
        assertThat(savedLog.getAction()).isEqualTo(action);
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getTenantId()).isEqualTo(tenantId);
        assertThat(savedLog.getOldValue()).isEqualTo(oldValue);
        assertThat(savedLog.getNewValue()).isEqualTo(newValue);
        assertThat(savedLog.getReason()).isEqualTo(reason);
        assertThat(savedLog.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("T067 - Should query audit trail by entity and return sorted by timestamp desc")
    void shouldQueryAuditTrailByEntity() {
        // Given
        AuditLog log1 = createAuditLog(entityId, "CREATE", LocalDateTime.now().minusHours(2));
        AuditLog log2 = createAuditLog(entityId, "UPDATE", LocalDateTime.now().minusHours(1));
        AuditLog log3 = createAuditLog(entityId, "POSTED", LocalDateTime.now());

        when(auditLogRepository.findByEntityIdOrderByTimestampDesc(entityId))
                .thenReturn(Arrays.asList(log3, log2, log1));

        // When
        List<AuditLog> auditTrail = auditLogService.getAuditTrail(entityId);

        // Then
        assertThat(auditTrail).hasSize(3);
        assertThat(auditTrail.get(0).getAction()).isEqualTo("POSTED");
        assertThat(auditTrail.get(1).getAction()).isEqualTo("UPDATE");
        assertThat(auditTrail.get(2).getAction()).isEqualTo("CREATE");
        
        // Verify timestamps are in descending order
        assertThat(auditTrail.get(0).getTimestamp()).isAfter(auditTrail.get(1).getTimestamp());
        assertThat(auditTrail.get(1).getTimestamp()).isAfter(auditTrail.get(2).getTimestamp());

        verify(auditLogRepository).findByEntityIdOrderByTimestampDesc(entityId);
    }

    @Test
    @DisplayName("T067 - Should query audit trail by date range (implicit through ordering)")
    void shouldQueryAuditTrailSortedByTimestamp() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        AuditLog log1 = createAuditLog(entityId, "CREATE", now.minusDays(3));
        AuditLog log2 = createAuditLog(entityId, "UPDATE", now.minusDays(1));
        AuditLog log3 = createAuditLog(entityId, "POSTED", now);

        when(auditLogRepository.findByEntityIdOrderByTimestampDesc(entityId))
                .thenReturn(Arrays.asList(log3, log2, log1));

        // When
        List<AuditLog> auditTrail = auditLogService.getAuditTrail(entityId);

        // Then
        assertThat(auditTrail).hasSize(3);
        
        // Most recent first
        assertThat(auditTrail.get(0).getTimestamp()).isEqualTo(now);
        assertThat(auditTrail.get(1).getTimestamp()).isEqualTo(now.minusDays(1));
        assertThat(auditTrail.get(2).getTimestamp()).isEqualTo(now.minusDays(3));
    }

    @Test
    @DisplayName("T067 - Should query tenant audit logs")
    void shouldQueryTenantAuditLogs() {
        // Given
        UUID entity1 = UUID.randomUUID();
        UUID entity2 = UUID.randomUUID();
        
        AuditLog log1 = createAuditLog(entity1, "CREATE", LocalDateTime.now().minusHours(2));
        log1.setTenantId(tenantId);
        
        AuditLog log2 = createAuditLog(entity2, "CREATE", LocalDateTime.now().minusHours(1));
        log2.setTenantId(tenantId);
        
        AuditLog log3 = createAuditLog(entity1, "UPDATE", LocalDateTime.now());
        log3.setTenantId(tenantId);

        when(auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId))
                .thenReturn(Arrays.asList(log3, log2, log1));

        // When
        List<AuditLog> tenantLogs = auditLogService.getTenantAuditLogs(tenantId);

        // Then
        assertThat(tenantLogs).hasSize(3);
        assertThat(tenantLogs).allMatch(log -> log.getTenantId().equals(tenantId));
        
        // Should be sorted by timestamp desc
        assertThat(tenantLogs.get(0).getTimestamp()).isAfter(tenantLogs.get(1).getTimestamp());
        assertThat(tenantLogs.get(1).getTimestamp()).isAfter(tenantLogs.get(2).getTimestamp());

        verify(auditLogRepository).findByTenantIdOrderByTimestampDesc(tenantId);
    }

    @Test
    @DisplayName("T066 - Should handle multiple consecutive log entries")
    void shouldHandleMultipleConsecutiveLogs() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditLogService.logAction(entityId, "JOURNAL_ENTRY", "CREATE", userId, tenantId, "Created");
        auditLogService.logModification(entityId, "JOURNAL_ENTRY", "UPDATE", userId, tenantId, 
                                       "DRAFT", "POSTED", "Posted by officer");
        auditLogService.logAction(entityId, "JOURNAL_ENTRY", "VIEW", userId, tenantId, "Viewed by auditor");

        // Then
        verify(auditLogRepository, times(3)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("T067 - Should return empty list for entity with no audit logs")
    void shouldReturnEmptyListForEntityWithNoLogs() {
        // Given
        UUID entityWithNoLogs = UUID.randomUUID();
        when(auditLogRepository.findByEntityIdOrderByTimestampDesc(entityWithNoLogs))
                .thenReturn(Arrays.asList());

        // When
        List<AuditLog> auditTrail = auditLogService.getAuditTrail(entityWithNoLogs);

        // Then
        assertThat(auditTrail).isEmpty();
        verify(auditLogRepository).findByEntityIdOrderByTimestampDesc(entityWithNoLogs);
    }

    private AuditLog createAuditLog(UUID entityId, String action, LocalDateTime timestamp) {
        return AuditLog.builder()
                .logId(UUID.randomUUID())
                .entityId(entityId)
                .entityType("JOURNAL_ENTRY")
                .action(action)
                .userId(userId)
                .tenantId(tenantId)
                .timestamp(timestamp)
                .details("Test audit log")
                .build();
    }
}
