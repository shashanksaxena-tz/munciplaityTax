package com.munitax.submission.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_reports")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String reportId;
    
    @Column(nullable = false)
    private String returnId;
    
    @Column(nullable = false)
    private Instant generatedDate = Instant.now();
    
    @Column(nullable = false)
    private Integer riskScore = 0; // 0-100
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;
    
    @ElementCollection
    @CollectionTable(name = "audit_report_flagged_items", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "flagged_item", columnDefinition = "TEXT")
    private List<String> flaggedItems = new ArrayList<>(); // JSON strings
    
    @Column(columnDefinition = "TEXT")
    private String yearOverYearComparison; // JSON
    
    @Column(columnDefinition = "TEXT")
    private String peerComparison; // JSON
    
    @Column(columnDefinition = "TEXT")
    private String patternAnalysis; // JSON
    
    @ElementCollection
    @CollectionTable(name = "audit_report_recommended_actions", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "recommended_action")
    private List<String> recommendedActions = new ArrayList<>();
    
    @Column(nullable = false)
    private Boolean auditorOverride = false;
    
    @Column(columnDefinition = "TEXT")
    private String overrideReason;
    
    private String tenantId;
    
    public enum RiskLevel {
        LOW,     // 0-20
        MEDIUM,  // 21-60
        HIGH     // 61-100
    }
    
    // Helper method to calculate risk level from score
    public static RiskLevel calculateRiskLevel(int score) {
        if (score <= 20) return RiskLevel.LOW;
        if (score <= 60) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }
    
    public void setRiskScoreAndLevel(int score) {
        this.riskScore = Math.max(0, Math.min(100, score)); // Clamp to 0-100
        this.riskLevel = calculateRiskLevel(this.riskScore);
    }
    
    public void addFlaggedItem(String item) {
        if (this.flaggedItems == null) {
            this.flaggedItems = new ArrayList<>();
        }
        this.flaggedItems.add(item);
    }
    
    public void addRecommendedAction(String action) {
        if (this.recommendedActions == null) {
            this.recommendedActions = new ArrayList<>();
        }
        this.recommendedActions.add(action);
    }
}
