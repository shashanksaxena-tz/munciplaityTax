package com.munitax.submission.service;

import com.munitax.submission.model.AuditQueue;
import com.munitax.submission.model.AuditReport;
import com.munitax.submission.model.Submission;
import com.munitax.submission.repository.AuditQueueRepository;
import com.munitax.submission.repository.AuditReportRepository;
import com.munitax.submission.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for generating automated audit reports with risk scoring,
 * variance analysis, peer comparison, and pattern detection.
 * 
 * Implements FR-037 to FR-041: Automated Audit Checks
 */
@Service
@Transactional
public class AuditReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditReportService.class);
    
    private final AuditReportRepository auditReportRepository;
    private final SubmissionRepository submissionRepository;
    private final AuditQueueRepository auditQueueRepository;
    
    public AuditReportService(
            AuditReportRepository auditReportRepository,
            SubmissionRepository submissionRepository,
            AuditQueueRepository auditQueueRepository) {
        this.auditReportRepository = auditReportRepository;
        this.submissionRepository = submissionRepository;
        this.auditQueueRepository = auditQueueRepository;
    }
    
    /**
     * Generate comprehensive audit report for a submission
     * @param returnId The return ID to audit
     * @return Generated audit report
     */
    public AuditReport generateAuditReport(String returnId) {
        Submission submission = submissionRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        
        AuditReport report = new AuditReport();
        report.setReturnId(returnId);
        report.setGeneratedDate(Instant.now());
        report.setTenantId(submission.getTenantId());
        
        // Perform automated checks
        List<String> flaggedItems = new ArrayList<>();
        int riskScore = 0;
        
        // 1. Year-over-year variance analysis
        YearOverYearAnalysis yoyAnalysis = performYearOverYearAnalysis(submission);
        if (yoyAnalysis.hasSignificantVariance()) {
            flaggedItems.add(yoyAnalysis.getMessage());
            riskScore += yoyAnalysis.getRiskPoints();
        }
        report.setYearOverYearComparison(yoyAnalysis.toJson());
        
        // 2. Ratio analysis (profit margin, expense ratios)
        RatioAnalysis ratioAnalysis = performRatioAnalysis(submission);
        if (ratioAnalysis.hasAnomalies()) {
            flaggedItems.add(ratioAnalysis.getMessage());
            riskScore += ratioAnalysis.getRiskPoints();
        }
        
        // 3. Peer comparison (same industry/revenue range)
        PeerComparison peerComparison = performPeerComparison(submission);
        if (peerComparison.isOutlier()) {
            flaggedItems.add(peerComparison.getMessage());
            riskScore += peerComparison.getRiskPoints();
        }
        report.setPeerComparison(peerComparison.toJson());
        
        // 4. Pattern analysis (unusual timing, round numbers)
        PatternAnalysis patternAnalysis = performPatternAnalysis(submission);
        if (patternAnalysis.hasSuspiciousPatterns()) {
            flaggedItems.add(patternAnalysis.getMessage());
            riskScore += patternAnalysis.getRiskPoints();
        }
        report.setPatternAnalysis(patternAnalysis.toJson());
        
        // 5. Rule compliance (discrepancy checks from Spec 3)
        int discrepancyCount = submission.getDiscrepancyCount() != null ? 
                submission.getDiscrepancyCount() : 0;
        if (discrepancyCount > 0) {
            flaggedItems.add("Found " + discrepancyCount + " discrepancies in submission");
            riskScore += Math.min(discrepancyCount * 5, 30); // Max 30 points for discrepancies
        }
        
        // Calculate final risk score (capped at 100)
        riskScore = Math.min(riskScore, 100);
        report.setRiskScoreAndLevel(riskScore);
        report.setFlaggedItems(flaggedItems);
        
        // Generate recommended actions
        List<String> recommendedActions = generateRecommendedActions(report, submission);
        report.setRecommendedActions(recommendedActions);
        
        // Save report
        AuditReport saved = auditReportRepository.save(report);
        
        // Update queue entry with risk score
        updateQueueWithRiskScore(returnId, riskScore, flaggedItems.size());
        
        return saved;
    }
    
    /**
     * Perform year-over-year variance analysis
     */
    private YearOverYearAnalysis performYearOverYearAnalysis(Submission submission) {
        YearOverYearAnalysis analysis = new YearOverYearAnalysis();
        
        // Get prior year submission for same taxpayer
        Optional<Submission> priorYear = submissionRepository
                .findPriorYearSubmission(submission.getTaxpayerId(), submission.getTaxYear() - 1);
        
        if (priorYear.isPresent()) {
            double currentTax = submission.getTaxDue() != null ? submission.getTaxDue() : 0;
            double priorTax = priorYear.get().getTaxDue() != null ? priorYear.get().getTaxDue() : 0;
            
            if (priorTax > 0) {
                double variance = ((currentTax - priorTax) / priorTax) * 100;
                analysis.setVariancePercent(variance);
                
                // Flag if variance > 50% up or down
                if (Math.abs(variance) > 50) {
                    analysis.setSignificantVariance(true);
                    analysis.setMessage(String.format(
                        "Tax liability changed by %.1f%% from prior year (was $%.2f, now $%.2f)",
                        variance, priorTax, currentTax));
                    analysis.setRiskPoints(20);
                }
            }
        }
        
        return analysis;
    }
    
    /**
     * Perform ratio analysis (profit margins, expense ratios)
     */
    private RatioAnalysis performRatioAnalysis(Submission submission) {
        RatioAnalysis analysis = new RatioAnalysis();
        
        // For business returns, check profit margin
        if ("BUSINESS".equals(submission.getReturnType())) {
            double revenue = submission.getGrossReceipts() != null ? submission.getGrossReceipts() : 0;
            double netProfit = submission.getNetProfit() != null ? submission.getNetProfit() : 0;
            
            if (revenue > 0) {
                double profitMargin = (netProfit / revenue) * 100;
                analysis.setProfitMargin(profitMargin);
                
                // Flag if profit margin is negative or unusually high
                if (profitMargin < 0) {
                    analysis.setAnomalies(true);
                    analysis.setMessage("Business reporting losses (negative profit margin)");
                    analysis.setRiskPoints(15);
                } else if (profitMargin > 50) {
                    analysis.setAnomalies(true);
                    analysis.setMessage(String.format("Unusually high profit margin: %.1f%%", profitMargin));
                    analysis.setRiskPoints(10);
                }
            }
        }
        
        return analysis;
    }
    
    /**
     * Perform peer comparison (same industry/revenue range)
     */
    private PeerComparison performPeerComparison(Submission submission) {
        PeerComparison comparison = new PeerComparison();
        
        // Get average tax rate for similar businesses
        if ("BUSINESS".equals(submission.getReturnType())) {
            double revenue = submission.getGrossReceipts() != null ? submission.getGrossReceipts() : 0;
            double taxDue = submission.getTaxDue() != null ? submission.getTaxDue() : 0;
            
            if (revenue > 0) {
                double effectiveTaxRate = (taxDue / revenue) * 100;
                comparison.setEffectiveTaxRate(effectiveTaxRate);
                
                // Industry average is typically 1.5-2.5% for municipal tax
                double industryAvg = 2.0;
                comparison.setIndustryAverage(industryAvg);
                
                if (effectiveTaxRate < (industryAvg - 1.0)) {
                    comparison.setOutlier(true);
                    comparison.setMessage(String.format(
                        "Effective tax rate (%.2f%%) is below industry average (%.2f%%)",
                        effectiveTaxRate, industryAvg));
                    comparison.setRiskPoints(15);
                }
            }
        }
        
        return comparison;
    }
    
    /**
     * Perform pattern analysis (unusual timing, round numbers)
     */
    private PatternAnalysis performPatternAnalysis(Submission submission) {
        PatternAnalysis analysis = new PatternAnalysis();
        
        // Check for round numbers (possible estimation)
        double taxDue = submission.getTaxDue() != null ? submission.getTaxDue() : 0;
        if (taxDue > 1000 && taxDue % 1000 == 0) {
            analysis.setSuspiciousPatterns(true);
            analysis.setMessage("Tax amount is a round number ($" + String.format("%.0f", taxDue) + 
                    "), may indicate estimation rather than actual calculation");
            analysis.setRiskPoints(5);
        }
        
        // Check for late filing
        if (submission.getFiledDate() != null && submission.getDueDate() != null) {
            if (submission.getFiledDate().isAfter(submission.getDueDate())) {
                analysis.setSuspiciousPatterns(true);
                analysis.setMessage("Return filed after due date");
                analysis.setRiskPoints(10);
            }
        }
        
        return analysis;
    }
    
    /**
     * Generate recommended actions based on audit findings
     */
    private List<String> generateRecommendedActions(AuditReport report, Submission submission) {
        List<String> actions = new ArrayList<>();
        
        if (report.getRiskScore() >= 61) {
            actions.add("HIGH RISK: Assign to senior auditor for detailed review");
            actions.add("Request supporting documentation for all major line items");
            actions.add("Verify taxpayer identification and business registration");
        } else if (report.getRiskScore() >= 21) {
            actions.add("MEDIUM RISK: Review flagged items before approval");
            actions.add("Request clarification on any unusual entries");
        } else {
            actions.add("LOW RISK: Standard review process");
            actions.add("Verify basic information and approve if no issues found");
        }
        
        // Add specific recommendations based on flagged items
        if (report.getFlaggedItems().size() > 5) {
            actions.add("Multiple issues detected - consider requesting amended return");
        }
        
        return actions;
    }
    
    /**
     * Update queue entry with calculated risk score
     */
    private void updateQueueWithRiskScore(String returnId, int riskScore, int flaggedCount) {
        Optional<AuditQueue> queueOpt = auditQueueRepository.findByReturnId(returnId);
        if (queueOpt.isPresent()) {
            AuditQueue queue = queueOpt.get();
            queue.setRiskScore(riskScore);
            queue.setFlaggedIssuesCount(flaggedCount);
            
            // Auto-assign priority based on risk score
            if (riskScore >= 61) {
                queue.setPriority(AuditQueue.Priority.HIGH);
            } else if (riskScore >= 21) {
                queue.setPriority(AuditQueue.Priority.MEDIUM);
            } else {
                queue.setPriority(AuditQueue.Priority.LOW);
            }
            
            auditQueueRepository.save(queue);
        }
    }
    
    // Inner classes for analysis results
    private static class YearOverYearAnalysis {
        private boolean significantVariance = false;
        private double variancePercent = 0;
        private String message = "";
        private int riskPoints = 0;
        
        public boolean hasSignificantVariance() { return significantVariance; }
        public void setSignificantVariance(boolean v) { this.significantVariance = v; }
        public double getVariancePercent() { return variancePercent; }
        public void setVariancePercent(double v) { this.variancePercent = v; }
        public String getMessage() { return message; }
        public void setMessage(String m) { this.message = m; }
        public int getRiskPoints() { return riskPoints; }
        public void setRiskPoints(int p) { this.riskPoints = p; }
        
        public String toJson() {
            return String.format("{\"variancePercent\": %.2f, \"significant\": %b}", 
                    variancePercent, significantVariance);
        }
    }
    
    private static class RatioAnalysis {
        private boolean anomalies = false;
        private double profitMargin = 0;
        private String message = "";
        private int riskPoints = 0;
        
        public boolean hasAnomalies() { return anomalies; }
        public void setAnomalies(boolean a) { this.anomalies = a; }
        public double getProfitMargin() { return profitMargin; }
        public void setProfitMargin(double m) { this.profitMargin = m; }
        public String getMessage() { return message; }
        public void setMessage(String m) { this.message = m; }
        public int getRiskPoints() { return riskPoints; }
        public void setRiskPoints(int p) { this.riskPoints = p; }
    }
    
    private static class PeerComparison {
        private boolean outlier = false;
        private double effectiveTaxRate = 0;
        private double industryAverage = 0;
        private String message = "";
        private int riskPoints = 0;
        
        public boolean isOutlier() { return outlier; }
        public void setOutlier(boolean o) { this.outlier = o; }
        public double getEffectiveTaxRate() { return effectiveTaxRate; }
        public void setEffectiveTaxRate(double r) { this.effectiveTaxRate = r; }
        public double getIndustryAverage() { return industryAverage; }
        public void setIndustryAverage(double a) { this.industryAverage = a; }
        public String getMessage() { return message; }
        public void setMessage(String m) { this.message = m; }
        public int getRiskPoints() { return riskPoints; }
        public void setRiskPoints(int p) { this.riskPoints = p; }
        
        public String toJson() {
            return String.format("{\"effectiveTaxRate\": %.2f, \"industryAverage\": %.2f, \"outlier\": %b}", 
                    effectiveTaxRate, industryAverage, outlier);
        }
    }
    
    private static class PatternAnalysis {
        private boolean suspiciousPatterns = false;
        private String message = "";
        private int riskPoints = 0;
        
        public boolean hasSuspiciousPatterns() { return suspiciousPatterns; }
        public void setSuspiciousPatterns(boolean s) { this.suspiciousPatterns = s; }
        public String getMessage() { return message; }
        public void setMessage(String m) { this.message = m; }
        public int getRiskPoints() { return riskPoints; }
        public void setRiskPoints(int p) { this.riskPoints = p; }
        
        public String toJson() {
            return String.format("{\"suspicious\": %b, \"message\": \"%s\"}", 
                    suspiciousPatterns, message.replace("\"", "\\\""));
        }
    }
}
