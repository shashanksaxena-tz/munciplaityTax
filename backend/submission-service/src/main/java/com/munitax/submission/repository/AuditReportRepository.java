package com.munitax.submission.repository;

import com.munitax.submission.model.AuditReport;
import com.munitax.submission.model.AuditReport.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditReportRepository extends JpaRepository<AuditReport, String> {
    
    // Find report by return ID
    Optional<AuditReport> findByReturnId(String returnId);
    List<AuditReport> findAllByReturnId(String returnId);
    
    // Find by risk level
    List<AuditReport> findByRiskLevel(RiskLevel riskLevel);
    Page<AuditReport> findByRiskLevel(RiskLevel riskLevel, Pageable pageable);
    
    // Find high-risk reports
    @Query("SELECT ar FROM AuditReport ar WHERE ar.riskLevel = 'HIGH' ORDER BY ar.riskScore DESC")
    List<AuditReport> findHighRiskReports();
    
    // Find reports with overrides
    List<AuditReport> findByAuditorOverrideTrue();
    
    // Find by tenant
    List<AuditReport> findByTenantId(String tenantId);
    
    // Find reports with risk score above threshold
    @Query("SELECT ar FROM AuditReport ar WHERE ar.riskScore >= :threshold")
    List<AuditReport> findByRiskScoreGreaterThanEqual(@Param("threshold") int threshold);
    
    // Count by risk level
    long countByRiskLevel(RiskLevel riskLevel);
}
