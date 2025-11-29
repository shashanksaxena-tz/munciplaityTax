package com.munitax.submission.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "submissions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Submission {
    @Id
    private String id;
    private String tenantId;
    private String userId;
    
    @Column(name = "taxpayer_id")
    private String taxpayerId; // Foreign key to taxpayer/business
    
    @Column(name = "tax_year")
    private Integer taxYear; // Changed from String to Integer
    
    @Column(nullable = false)
    private String status; // SUBMITTED, IN_REVIEW, APPROVED, REJECTED, AMENDED, AWAITING_DOCUMENTATION
    
    @Column(columnDefinition = "TEXT")
    private String auditorComments;
    
    @Column(name = "submitted_at")
    private Instant submittedAt;
    
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    
    @Column(name = "reviewed_by")
    private String reviewedBy;
    
    @Column(length = 50, name = "return_type")
    private String returnType; // INDIVIDUAL or BUSINESS
    
    @Column(precision = 10, scale = 2, name = "tax_due")
    private Double taxDue;
    
    @Column(length = 255, name = "taxpayer_name")
    private String taxpayerName;
    
    @Column(length = 50, name = "taxpayer_fein")
    private String taxpayerFEIN;
    
    // Audit workflow fields
    @Column(length = 20)
    private String priority; // HIGH, MEDIUM, LOW
    
    @Column(name = "risk_score")
    private Integer riskScore;
    
    @Column(name = "has_discrepancies")
    private Boolean hasDiscrepancies;
    
    @Column(name = "discrepancy_count")
    private Integer discrepancyCount;
    
    @Column(columnDefinition = "TEXT", name = "rejection_reason")
    private String rejectionReason;
    
    @Column(length = 512, name = "digital_signature")
    private String digitalSignature; // E-signature hash for approvals
    
    // Business-specific fields for audit analysis
    @Column(precision = 12, scale = 2, name = "gross_receipts")
    private Double grossReceipts;
    
    @Column(precision = 12, scale = 2, name = "net_profit")
    private Double netProfit;
    
    @Column(name = "filed_date")
    private Instant filedDate;
    
    @Column(name = "due_date")
    private LocalDate dueDate;

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTenantId() { return tenantId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }
    public void setTaxpayerId(String taxpayerId) { this.taxpayerId = taxpayerId; }
    public String getTaxpayerId() { return taxpayerId; }
    public void setTaxYear(Integer taxYear) { this.taxYear = taxYear; }
    public Integer getTaxYear() { return taxYear; }
    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
    public void setAuditorComments(String auditorComments) { this.auditorComments = auditorComments; }
    public String getAuditorComments() { return auditorComments; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public String getReturnType() { return returnType; }
    public void setTaxDue(Double taxDue) { this.taxDue = taxDue; }
    public Double getTaxDue() { return taxDue; }
    public void setTaxpayerName(String taxpayerName) { this.taxpayerName = taxpayerName; }
    public String getTaxpayerName() { return taxpayerName; }
    public void setTaxpayerFEIN(String taxpayerFEIN) { this.taxpayerFEIN = taxpayerFEIN; }
    public String getTaxpayerFEIN() { return taxpayerFEIN; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getPriority() { return priority; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public Integer getRiskScore() { return riskScore; }
    public void setHasDiscrepancies(Boolean hasDiscrepancies) { this.hasDiscrepancies = hasDiscrepancies; }
    public Boolean getHasDiscrepancies() { return hasDiscrepancies; }
    public void setDiscrepancyCount(Integer discrepancyCount) { this.discrepancyCount = discrepancyCount; }
    public Integer getDiscrepancyCount() { return discrepancyCount; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getRejectionReason() { return rejectionReason; }
    public void setDigitalSignature(String digitalSignature) { this.digitalSignature = digitalSignature; }
    public String getDigitalSignature() { return digitalSignature; }
    public void setGrossReceipts(Double grossReceipts) { this.grossReceipts = grossReceipts; }
    public Double getGrossReceipts() { return grossReceipts; }
    public void setNetProfit(Double netProfit) { this.netProfit = netProfit; }
    public Double getNetProfit() { return netProfit; }
    public void setFiledDate(Instant filedDate) { this.filedDate = filedDate; }
    public Instant getFiledDate() { return filedDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getDueDate() { return dueDate; }
}
