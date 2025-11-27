package com.munitax.tenant.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tax_return_sessions")
public class TaxReturnSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionType type; // INDIVIDUAL or BUSINESS

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionStatus status; // DRAFT, IN_PROGRESS, CALCULATED, SUBMITTED, AMENDED

    @Column(columnDefinition = "TEXT")
    private String profileJson; // Serialized TaxPayerProfile or BusinessProfile

    @Column(columnDefinition = "TEXT")
    private String settingsJson; // Serialized TaxReturnSettings

    @Column(columnDefinition = "TEXT")
    private String formsJson; // Serialized List<TaxFormData>

    @Column(columnDefinition = "TEXT")
    private String calculationResultJson; // Serialized TaxCalculationResult

    @Column(columnDefinition = "TEXT")
    private String businessFilingsJson; // For business sessions

    @Column(columnDefinition = "TEXT")
    private String netProfitFilingsJson; // For business sessions

    @Column(columnDefinition = "TEXT")
    private String reconciliationsJson; // For business sessions

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;

    private LocalDateTime submittedDate;

    @Column(length = 500)
    private String notes;

    public enum SessionType {
        INDIVIDUAL,
        BUSINESS
    }

    public enum SessionStatus {
        DRAFT,
        IN_PROGRESS,
        CALCULATED,
        SUBMITTED,
        AMENDED
    }

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        lastModifiedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }

    // Constructors
    public TaxReturnSession() {}

    public TaxReturnSession(String id, String tenantId, String userId, SessionType type, SessionStatus status,
                           String profileJson, String settingsJson, String formsJson, String calculationResultJson,
                           String businessFilingsJson, String netProfitFilingsJson, String reconciliationsJson,
                           LocalDateTime createdDate, LocalDateTime lastModifiedDate, LocalDateTime submittedDate, String notes) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.type = type;
        this.status = status;
        this.profileJson = profileJson;
        this.settingsJson = settingsJson;
        this.formsJson = formsJson;
        this.calculationResultJson = calculationResultJson;
        this.businessFilingsJson = businessFilingsJson;
        this.netProfitFilingsJson = netProfitFilingsJson;
        this.reconciliationsJson = reconciliationsJson;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
        this.submittedDate = submittedDate;
        this.notes = notes;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public SessionType getType() { return type; }
    public void setType(SessionType type) { this.type = type; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public String getProfileJson() { return profileJson; }
    public void setProfileJson(String profileJson) { this.profileJson = profileJson; }

    public String getSettingsJson() { return settingsJson; }
    public void setSettingsJson(String settingsJson) { this.settingsJson = settingsJson; }

    public String getFormsJson() { return formsJson; }
    public void setFormsJson(String formsJson) { this.formsJson = formsJson; }

    public String getCalculationResultJson() { return calculationResultJson; }
    public void setCalculationResultJson(String calculationResultJson) { this.calculationResultJson = calculationResultJson; }

    public String getBusinessFilingsJson() { return businessFilingsJson; }
    public void setBusinessFilingsJson(String businessFilingsJson) { this.businessFilingsJson = businessFilingsJson; }

    public String getNetProfitFilingsJson() { return netProfitFilingsJson; }
    public void setNetProfitFilingsJson(String netProfitFilingsJson) { this.netProfitFilingsJson = netProfitFilingsJson; }

    public String getReconciliationsJson() { return reconciliationsJson; }
    public void setReconciliationsJson(String reconciliationsJson) { this.reconciliationsJson = reconciliationsJson; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    public LocalDateTime getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(LocalDateTime submittedDate) { this.submittedDate = submittedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
