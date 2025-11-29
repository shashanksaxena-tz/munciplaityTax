package com.munitax.rules.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an immutable audit log entry for rule changes.
 * This table is append-only - updates and deletes are prevented by database trigger.
 */
@Entity
@Table(name = "rule_change_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleChangeLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;
    
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ChangeType changeType;
    
    /**
     * Complete rule state before the change (NULL for CREATE operations).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private Object oldValue;
    
    /**
     * Complete rule state after the change.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", nullable = false, columnDefinition = "jsonb")
    private Object newValue;
    
    /**
     * List of field names that were modified (e.g., ["value", "effective_date"]).
     */
    @Column(name = "changed_fields", nullable = false, columnDefinition = "varchar[]")
    private List<String> changedFields;
    
    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;
    
    @Column(name = "change_date", nullable = false)
    @Builder.Default
    private LocalDateTime changeDate = LocalDateTime.now();
    
    @Column(name = "change_reason", nullable = false, columnDefinition = "text")
    private String changeReason;
    
    /**
     * Number of tax returns that were filed using the previous version of this rule.
     * Used for impact analysis.
     */
    @Column(name = "affected_returns_count")
    @Builder.Default
    private Integer affectedReturnsCount = 0;
    
    /**
     * JSONB object containing estimated impact metrics:
     * {
     *   "totalAffectedTaxpayers": 5000,
     *   "avgTaxIncrease": 40.50,
     *   "avgTaxDecrease": 0,
     *   "maxImpact": 150.00,
     *   "minImpact": 10.00,
     *   "medianImpact": 38.25
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "impact_estimate", columnDefinition = "jsonb")
    private Object impactEstimate;
}
