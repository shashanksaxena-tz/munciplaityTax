package com.munitax.rules.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a configurable tax rule.
 * Supports temporal effective dating, multi-tenant isolation, and version control.
 */
@Entity
@Table(name = "tax_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rule_id")
    private UUID ruleId;
    
    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;
    
    @Column(name = "rule_name", nullable = false)
    private String ruleName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private RuleCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 50)
    private RuleValueType valueType;
    
    /**
     * Polymorphic JSONB field storing the rule value.
     * Structure depends on valueType:
     * - NUMBER: {"scalar": 50000}
     * - PERCENTAGE: {"scalar": 2.5, "unit": "percent"}
     * - ENUM: {"option": "BOX_5_MEDICARE", "allowedValues": [...]}
     * - BOOLEAN: {"flag": true}
     * - FORMULA: {"expression": "...", "variables": [...]}
     * - CONDITIONAL: {"condition": "...", "thenValue": ..., "elseValue": ...}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> value;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;
    
    @Column(name = "entity_types", nullable = false, columnDefinition = "varchar[]")
    @Builder.Default
    private List<String> entityTypes = new ArrayList<>();
    
    @Column(name = "applies_to", columnDefinition = "text")
    private String appliesTo;
    
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;
    
    @Column(name = "previous_version_id")
    private UUID previousVersionId;
    
    @Column(name = "depends_on", columnDefinition = "uuid[]")
    private List<UUID> dependsOn;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    
    @Column(name = "approval_date")
    private LocalDateTime approvalDate;
    
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    
    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();
    
    @Column(name = "modified_by", length = 100)
    private String modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    @Column(name = "change_reason", nullable = false, columnDefinition = "text")
    private String changeReason;
    
    @Column(name = "ordinance_reference", columnDefinition = "text")
    private String ordinanceReference;
    
    /**
     * Check if this rule is currently active (approved and within effective date range).
     * @param asOfDate The date to check against
     * @return true if rule is active on the given date
     */
    public boolean isActiveOn(LocalDate asOfDate) {
        if (approvalStatus != ApprovalStatus.APPROVED) {
            return false;
        }
        boolean afterStart = !asOfDate.isBefore(effectiveDate);
        boolean beforeEnd = endDate == null || !asOfDate.isAfter(endDate);
        return afterStart && beforeEnd;
    }
    
    /**
     * Check if this rule's date range overlaps with another date range.
     * @param start Start date of range to check
     * @param end End date of range to check (null = infinite)
     * @return true if ranges overlap
     */
    public boolean overlaps(LocalDate start, LocalDate end) {
        LocalDate thisEnd = this.endDate != null ? this.endDate : LocalDate.MAX;
        LocalDate otherEnd = end != null ? end : LocalDate.MAX;
        return !start.isAfter(thisEnd) && !this.effectiveDate.isAfter(otherEnd);
    }
}
