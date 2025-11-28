package com.munitax.taxengine.domain.nol;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * NOLExpirationAlert Entity - Alerts for NOLs approaching expiration.
 * 
 * Functional Requirements:
 * - FR-024: Alert user of expiring NOLs
 * - FR-038: Generate NOL expiration report
 * 
 * Constitution Compliance:
 * - Principle II: Multi-tenant data isolation via tenant_id
 * - Principle III: Immutable audit trail
 * 
 * @see NOL
 * @see AlertSeverityLevel
 */
@Entity
@Table(name = "nol_expiration_alerts", schema = "dublin", indexes = {
    @Index(name = "idx_nol_alert_business", columnList = "business_id"),
    @Index(name = "idx_nol_alert_nol", columnList = "nol_id"),
    @Index(name = "idx_nol_alert_severity", columnList = "severity_level"),
    @Index(name = "idx_nol_alert_dismissed", columnList = "dismissed")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NOLExpirationAlert {
    
    /**
     * Unique identifier for this alert.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation - tenant owning this alert.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Business profile ID for this alert.
     */
    @Column(name = "business_id", nullable = false)
    private UUID businessId;
    
    /**
     * Reference to the NOL that is expiring.
     */
    @Column(name = "nol_id", nullable = false)
    private UUID nolId;
    
    /**
     * Tax year when the loss originated.
     */
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    /**
     * Remaining NOL balance that will expire if not used.
     * CHECK constraint: nol_balance > 0
     */
    @Column(name = "nol_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal nolBalance;
    
    /**
     * Date when this NOL expires.
     */
    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;
    
    /**
     * Number of years until expiration (calculated field).
     * Derived from: (expiration_date - current_date) / 365
     */
    @Column(name = "years_until_expiration", nullable = false, precision = 3, scale = 1)
    private BigDecimal yearsUntilExpiration;
    
    /**
     * Severity level of this alert based on time until expiration.
     * CRITICAL: ≤1 year
     * WARNING: 1-2 years
     * INFO: 2-3 years
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level", nullable = false, length = 20)
    private AlertSeverityLevel severityLevel;
    
    /**
     * Alert message displayed to user.
     * Example: "⚠️ $100K NOL expiring 12/31/2025 - Use in next 18 months or lose!"
     */
    @Column(name = "alert_message", nullable = false, columnDefinition = "TEXT")
    private String alertMessage;
    
    /**
     * Whether user has acknowledged/dismissed this alert.
     */
    @Column(name = "dismissed", nullable = false)
    @Builder.Default
    private Boolean dismissed = false;
    
    /**
     * Audit trail: When this alert was created (immutable).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Helper method to calculate years until expiration.
     * 
     * @param expirationDate the expiration date
     * @return years until expiration as BigDecimal
     */
    public static BigDecimal calculateYearsUntilExpiration(LocalDate expirationDate) {
        long daysUntilExpiration = ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
        return BigDecimal.valueOf(daysUntilExpiration).divide(BigDecimal.valueOf(365), 1, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Helper method to determine severity level based on years until expiration.
     * 
     * @param yearsUntilExpiration years until expiration
     * @return appropriate AlertSeverityLevel
     */
    public static AlertSeverityLevel determineSeverityLevel(BigDecimal yearsUntilExpiration) {
        if (yearsUntilExpiration.compareTo(BigDecimal.ONE) <= 0) {
            return AlertSeverityLevel.CRITICAL;
        } else if (yearsUntilExpiration.compareTo(BigDecimal.valueOf(2)) <= 0) {
            return AlertSeverityLevel.WARNING;
        } else {
            return AlertSeverityLevel.INFO;
        }
    }
}
