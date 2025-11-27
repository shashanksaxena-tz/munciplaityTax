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
    private String status; // SUBMITTED, APPROVED, REJECTED
    private String auditorComments;
    private Instant submittedAt;
    private Instant reviewedAt;
    private String reviewedBy;

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
}
