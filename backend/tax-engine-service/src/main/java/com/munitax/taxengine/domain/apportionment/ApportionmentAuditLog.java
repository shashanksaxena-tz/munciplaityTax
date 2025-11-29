package com.munitax.taxengine.domain.apportionment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Apportionment Audit Log entity for immutable audit trail.
 * Tracks all changes to Schedule Y and related entities for compliance (7-year retention).
 */
@Entity
@Table(name = "apportionment_audit_log", indexes = {
    @Index(name = "idx_apportionment_audit_schedule_y_id", columnList = "schedule_y_id"),
    @Index(name = "idx_apportionment_audit_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_apportionment_audit_change_type", columnList = "change_type"),
    @Index(name = "idx_apportionment_audit_change_date", columnList = "change_date"),
    @Index(name = "idx_apportionment_audit_changed_by", columnList = "changed_by")
})
@Data
@NoArgsConstructor
public class ApportionmentAuditLog {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "audit_log_id", updatable = false, nullable = false)
    private UUID auditLogId;

    @Column(name = "schedule_y_id", nullable = false)
    private UUID scheduleYId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private AuditChangeType changeType;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "change_date", nullable = false, updatable = false)
    private LocalDateTime changeDate;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "affected_calculation", length = 100)
    private String affectedCalculation;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        changeDate = LocalDateTime.now();
    }

    /**
     * Constructor for creating audit log entry.
     */
    public ApportionmentAuditLog(UUID scheduleYId, UUID tenantId, AuditChangeType changeType, 
                                UUID changedBy, String changeReason) {
        this.scheduleYId = scheduleYId;
        this.tenantId = tenantId;
        this.changeType = changeType;
        this.changedBy = changedBy;
        this.changeReason = changeReason;
    }

    /**
     * Builder pattern for creating audit log entries.
     */
    public static class Builder {
        private UUID scheduleYId;
        private UUID tenantId;
        private AuditChangeType changeType;
        private String entityType;
        private UUID entityId;
        private UUID changedBy;
        private String oldValue;
        private String newValue;
        private String changeReason;
        private String affectedCalculation;
        private String ipAddress;
        private String userAgent;

        public Builder scheduleYId(UUID scheduleYId) {
            this.scheduleYId = scheduleYId;
            return this;
        }

        public Builder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder changeType(AuditChangeType changeType) {
            this.changeType = changeType;
            return this;
        }

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder entityId(UUID entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder changedBy(UUID changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public Builder oldValue(String oldValue) {
            this.oldValue = oldValue;
            return this;
        }

        public Builder newValue(String newValue) {
            this.newValue = newValue;
            return this;
        }

        public Builder changeReason(String changeReason) {
            this.changeReason = changeReason;
            return this;
        }

        public Builder affectedCalculation(String affectedCalculation) {
            this.affectedCalculation = affectedCalculation;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public ApportionmentAuditLog build() {
            ApportionmentAuditLog log = new ApportionmentAuditLog(scheduleYId, tenantId, changeType, changedBy, changeReason);
            log.entityType = this.entityType;
            log.entityId = this.entityId;
            log.oldValue = this.oldValue;
            log.newValue = this.newValue;
            log.affectedCalculation = this.affectedCalculation;
            log.ipAddress = this.ipAddress;
            log.userAgent = this.userAgent;
            return log;
        }
    }
}
