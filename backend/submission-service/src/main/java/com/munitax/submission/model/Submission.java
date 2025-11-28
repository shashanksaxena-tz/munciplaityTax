package com.munitax.submission.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    private String taxYear;
    private String status; // SUBMITTED, IN_REVIEW, APPROVED, REJECTED, AMENDED, AWAITING_DOCUMENTATION
    private String auditorComments;
    private Instant submittedAt;
    private Instant reviewedAt;
    private String reviewedBy;
    private String returnType; // INDIVIDUAL or BUSINESS
    private Double taxDue;
    private String taxpayerName;
    private String taxpayerFEIN;
    
    // Audit workflow fields
    private String priority; // HIGH, MEDIUM, LOW
    private Integer riskScore;
    private Boolean hasDiscrepancies;
    private String rejectionReason;
    private String digitalSignature; // E-signature hash for approvals

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTenantId() { return tenantId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }
    public void setTaxYear(String taxYear) { this.taxYear = taxYear; }
    public String getTaxYear() { return taxYear; }
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
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getRejectionReason() { return rejectionReason; }
    public void setDigitalSignature(String digitalSignature) { this.digitalSignature = digitalSignature; }
    public String getDigitalSignature() { return digitalSignature; }
}
