package com.munitax.submission.service;

import com.munitax.submission.dto.W3DiscrepancyResponse;
import com.munitax.submission.dto.W3ReconciliationRequest;
import com.munitax.submission.dto.W3ReconciliationResponse;
import com.munitax.submission.model.W3Reconciliation;
import com.munitax.submission.repository.W3ReconciliationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Service for W-3 year-end reconciliation operations.
 * 
 * Core Functions:
 * - Create W-3 reconciliation by summing W-1 filings
 * - Compare W-1 totals to W-2 totals
 * - Identify discrepancies
 * - Calculate penalties for late/missing filings
 * - Submit W-3 reconciliation
 */
@Service
@Transactional
public class W3ReconciliationService {
    
    private static final Logger logger = LoggerFactory.getLogger(W3ReconciliationService.class);
    private static final BigDecimal DISCREPANCY_TOLERANCE = new BigDecimal("1.00");
    private static final BigDecimal LATE_FILING_PENALTY_RATE = new BigDecimal("0.05"); // 5% per month
    private static final BigDecimal MAX_LATE_PENALTY_RATE = new BigDecimal("0.25"); // 25% maximum
    private static final BigDecimal MINIMUM_PENALTY = new BigDecimal("50.00");
    private static final BigDecimal MISSING_FILING_PENALTY = new BigDecimal("100.00"); // Per missing filing
    
    private final W3ReconciliationRepository w3ReconciliationRepository;
    private final RestTemplate restTemplate;
    
    @Value("${tax-engine-service.url:http://localhost:8082}")
    private String taxEngineServiceUrl;
    
    @Value("${tax.w3.due-date.month:1}")
    private int w3DueDateMonth; // January
    
    @Value("${tax.w3.due-date.day:31}")
    private int w3DueDateDay; // 31st
    
    public W3ReconciliationService(
            W3ReconciliationRepository w3ReconciliationRepository,
            RestTemplate restTemplate) {
        this.w3ReconciliationRepository = w3ReconciliationRepository;
        this.restTemplate = restTemplate;
    }
    
    /**
     * Create a new W-3 reconciliation.
     * 
     * Process:
     * 1. Fetch all W-1 filings for the business and tax year from tax-engine-service
     * 2. Sum all W-1 tax amounts
     * 3. Compare to W-2 total provided in request
     * 4. Calculate discrepancy
     * 5. Determine if balanced or unbalanced
     * 6. Calculate penalties for late/missing filings
     * 7. Save W-3 reconciliation
     * 
     * @param request W-3 reconciliation request
     * @param userId User creating the reconciliation
     * @param tenantId Tenant ID
     * @return W-3 reconciliation response
     */
    public W3ReconciliationResponse createW3Reconciliation(
            W3ReconciliationRequest request, String userId, String tenantId) {
        
        logger.info("Creating W-3 reconciliation for business {} tax year {}", 
                    request.getBusinessId(), request.getTaxYear());
        
        // Check if W-3 already exists
        boolean exists = w3ReconciliationRepository.existsByBusinessIdAndTaxYearAndTenantId(
            request.getBusinessId(), request.getTaxYear(), tenantId
        );
        if (exists) {
            throw new IllegalArgumentException(
                String.format("W-3 reconciliation already exists for business %s tax year %d",
                             request.getBusinessId(), request.getTaxYear())
            );
        }
        
        // Fetch W-1 filings from tax-engine-service
        W1FilingSummary w1Summary = fetchW1FilingsForYear(
            request.getBusinessId(), request.getTaxYear(), tenantId
        );
        
        // Calculate discrepancy
        BigDecimal totalW1Tax = w1Summary.getTotalTax();
        BigDecimal totalW2Tax = request.getTotalW2Tax();
        BigDecimal discrepancy = totalW1Tax.subtract(totalW2Tax);
        
        // Determine status
        String status = discrepancy.abs().compareTo(DISCREPANCY_TOLERANCE) < 0 
                        ? "BALANCED" : "UNBALANCED";
        
        // Calculate due date (January 31st of following year)
        LocalDate dueDate = LocalDate.of(request.getTaxYear() + 1, w3DueDateMonth, w3DueDateDay);
        
        // Calculate penalties
        PenaltyCalculation penalties = calculatePenalties(
            w1Summary, totalW1Tax, dueDate, request.getTaxYear()
        );
        
        // Create W-3 reconciliation entity
        W3Reconciliation w3 = W3Reconciliation.builder()
            .tenantId(tenantId)
            .businessId(request.getBusinessId())
            .taxYear(request.getTaxYear())
            .totalW1Tax(totalW1Tax)
            .totalW2Tax(totalW2Tax)
            .discrepancy(discrepancy)
            .status(status)
            .w1FilingCount(w1Summary.getFilingCount())
            .w2FormCount(request.getW2FormCount())
            .totalEmployees(request.getTotalEmployees())
            .lateFilingPenalty(penalties.getLateFilingPenalty())
            .missingFilingPenalty(penalties.getMissingFilingPenalty())
            .totalPenalties(penalties.getTotalPenalties())
            .dueDate(dueDate)
            .isSubmitted(false)
            .notes(request.getNotes())
            .createdBy(userId)
            .w1FilingIds(w1Summary.getFilingIds())
            .build();
        
        W3Reconciliation saved = w3ReconciliationRepository.save(w3);
        
        logger.info("Created W-3 reconciliation {} with status {}", saved.getId(), status);
        
        return toResponse(saved);
    }
    
    /**
     * Get W-3 reconciliation by year.
     * 
     * @param businessId Business ID
     * @param taxYear Tax year
     * @param tenantId Tenant ID
     * @return W-3 reconciliation response
     */
    public W3ReconciliationResponse getW3ReconciliationByYear(
            String businessId, Integer taxYear, String tenantId) {
        
        W3Reconciliation w3 = w3ReconciliationRepository
            .findByBusinessIdAndTaxYearAndTenantId(businessId, taxYear, tenantId)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("W-3 reconciliation not found for business %s tax year %d",
                             businessId, taxYear)
            ));
        
        return toResponse(w3);
    }
    
    /**
     * Get W-3 reconciliation by ID.
     * 
     * @param id W-3 reconciliation ID
     * @param tenantId Tenant ID
     * @return W-3 reconciliation response
     */
    public W3ReconciliationResponse getW3ReconciliationById(UUID id, String tenantId) {
        W3Reconciliation w3 = w3ReconciliationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("W-3 reconciliation not found with ID %s", id)
            ));
        
        // Verify tenant access
        if (!w3.getTenantId().equals(tenantId)) {
            throw new SecurityException("Access denied to W-3 reconciliation");
        }
        
        return toResponse(w3);
    }
    
    /**
     * Submit W-3 reconciliation.
     * 
     * @param id W-3 reconciliation ID
     * @param tenantId Tenant ID
     * @return Updated W-3 reconciliation response
     */
    public W3ReconciliationResponse submitW3Reconciliation(UUID id, String tenantId) {
        W3Reconciliation w3 = w3ReconciliationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("W-3 reconciliation not found with ID %s", id)
            ));
        
        // Verify tenant access
        if (!w3.getTenantId().equals(tenantId)) {
            throw new SecurityException("Access denied to W-3 reconciliation");
        }
        
        if (w3.getIsSubmitted()) {
            throw new IllegalStateException("W-3 reconciliation already submitted");
        }
        
        // Generate confirmation number
        String confirmationNumber = generateConfirmationNumber(w3);
        
        // Update W-3 with submission details
        w3.setIsSubmitted(true);
        w3.setFilingDate(Instant.now());
        w3.setConfirmationNumber(confirmationNumber);
        
        // Recalculate late filing penalty based on actual filing date
        if (w3.getFilingDate() != null) {
            LocalDate filingLocalDate = LocalDate.ofInstant(w3.getFilingDate(), 
                java.time.ZoneId.systemDefault());
            if (filingLocalDate.isAfter(w3.getDueDate())) {
                BigDecimal lateFilingPenalty = calculateLateFilingPenalty(
                    w3.getTotalW1Tax(), w3.getDueDate(), filingLocalDate
                );
                w3.setLateFilingPenalty(lateFilingPenalty);
                w3.setTotalPenalties(lateFilingPenalty.add(w3.getMissingFilingPenalty()));
            }
        }
        
        W3Reconciliation saved = w3ReconciliationRepository.save(w3);
        
        logger.info("Submitted W-3 reconciliation {} with confirmation {}", 
                    saved.getId(), confirmationNumber);
        
        return toResponse(saved);
    }
    
    /**
     * Get discrepancy details for W-3 reconciliation.
     * 
     * @param id W-3 reconciliation ID
     * @param tenantId Tenant ID
     * @return Discrepancy response with detailed breakdown
     */
    public W3DiscrepancyResponse getDiscrepancies(UUID id, String tenantId) {
        W3Reconciliation w3 = w3ReconciliationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("W-3 reconciliation not found with ID %s", id)
            ));
        
        // Verify tenant access
        if (!w3.getTenantId().equals(tenantId)) {
            throw new SecurityException("Access denied to W-3 reconciliation");
        }
        
        // Calculate discrepancy percentage
        BigDecimal discrepancyPercentage = BigDecimal.ZERO;
        if (w3.getTotalW2Tax().compareTo(BigDecimal.ZERO) > 0) {
            discrepancyPercentage = w3.getDiscrepancy()
                .divide(w3.getTotalW2Tax(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }
        
        // Determine expected W-1 filings based on filing frequency
        // For simplicity, assuming quarterly (4) or monthly (12) filings
        // This could be enhanced by fetching business filing frequency from business profile
        Integer expectedW1Filings = 12; // Default to monthly
        Integer missingW1Filings = Math.max(0, expectedW1Filings - w3.getW1FilingCount());
        
        // Build description
        StringBuilder description = new StringBuilder();
        if ("BALANCED".equals(w3.getStatus())) {
            description.append("W-1 and W-2 totals match within acceptable tolerance. ");
        } else {
            BigDecimal absDiff = w3.getDiscrepancy().abs();
            if (w3.getDiscrepancy().compareTo(BigDecimal.ZERO) > 0) {
                description.append(String.format(
                    "More tax remitted via W-1 filings ($%,.2f) than reported on W-2 forms ($%,.2f). " +
                    "Overpayment of $%,.2f. ",
                    w3.getTotalW1Tax(), w3.getTotalW2Tax(), absDiff
                ));
            } else {
                description.append(String.format(
                    "Less tax remitted via W-1 filings ($%,.2f) than reported on W-2 forms ($%,.2f). " +
                    "Underpayment of $%,.2f. ",
                    w3.getTotalW1Tax(), w3.getTotalW2Tax(), absDiff
                ));
            }
        }
        
        if (missingW1Filings > 0) {
            description.append(String.format(
                "%d W-1 filing(s) may be missing. ", missingW1Filings
            ));
        }
        
        // Build recommended action
        String recommendedAction;
        if ("BALANCED".equals(w3.getStatus()) && missingW1Filings == 0) {
            recommendedAction = "No action required. Proceed with W-3 submission.";
        } else {
            List<String> actions = new ArrayList<>();
            if (!"BALANCED".equals(w3.getStatus())) {
                if (w3.getDiscrepancy().compareTo(BigDecimal.ZERO) > 0) {
                    actions.add("Review W-2 forms for missing or incorrect Box 19 amounts");
                    actions.add("Verify all W-1 filings were accurately reported");
                } else {
                    actions.add("File amended W-1 returns if additional wages were discovered");
                    actions.add("Review W-2 forms for duplicate or inflated Box 19 amounts");
                }
            }
            if (missingW1Filings > 0) {
                actions.add("File missing W-1 returns for all periods in the tax year");
            }
            recommendedAction = String.join("; ", actions);
        }
        
        return W3DiscrepancyResponse.builder()
            .totalW1Tax(w3.getTotalW1Tax())
            .totalW2Tax(w3.getTotalW2Tax())
            .discrepancy(w3.getDiscrepancy())
            .discrepancyPercentage(discrepancyPercentage)
            .status(w3.getStatus())
            .w1FilingCount(w3.getW1FilingCount())
            .w2FormCount(w3.getW2FormCount())
            .missingW1Filings(missingW1Filings)
            .expectedW1Filings(expectedW1Filings)
            .lateFilingPenalty(w3.getLateFilingPenalty())
            .missingFilingPenalty(w3.getMissingFilingPenalty())
            .totalPenalties(w3.getTotalPenalties())
            .description(description.toString().trim())
            .recommendedAction(recommendedAction)
            .build();
    }
    
    /**
     * Fetch W-1 filings summary from tax-engine-service.
     * 
     * @param businessId Business ID
     * @param taxYear Tax year
     * @param tenantId Tenant ID
     * @return W-1 filing summary
     */
    private W1FilingSummary fetchW1FilingsForYear(
            String businessId, Integer taxYear, String tenantId) {
        
        try {
            // TODO: Complete integration with tax-engine-service
            // Call tax-engine-service to get W-1 filings summary
            // Expected endpoint: GET /api/v1/w1-filings/summary?businessId={businessId}&taxYear={taxYear}
            // 
            // Example implementation:
            // String url = taxEngineServiceUrl + "/api/v1/w1-filings/summary?businessId=" + 
            //              businessId + "&taxYear=" + taxYear + "&tenantId=" + tenantId;
            // W1FilingSummary summary = restTemplate.getForObject(url, W1FilingSummary.class);
            // return summary;
            
            // MOCK IMPLEMENTATION - Returns zero amounts
            // This will cause reconciliations to show incorrect discrepancies
            // Replace with actual REST call before production use
            logger.warn("Using mock W-1 filing summary - integration with tax-engine-service required for accurate reconciliation");
            
            return W1FilingSummary.builder()
                .totalTax(BigDecimal.ZERO)
                .filingCount(0)
                .filingIds(new ArrayList<>())
                .build();
            
        } catch (Exception e) {
            logger.error("Failed to fetch W-1 filings from tax-engine-service", e);
            // Return empty summary on error
            return W1FilingSummary.builder()
                .totalTax(BigDecimal.ZERO)
                .filingCount(0)
                .filingIds(new ArrayList<>())
                .build();
        }
    }
    
    /**
     * Calculate penalties for late/missing filings.
     * 
     * @param w1Summary W-1 filing summary
     * @param totalW1Tax Total W-1 tax amount
     * @param dueDate W-3 due date
     * @param taxYear Tax year
     * @return Penalty calculation
     */
    private PenaltyCalculation calculatePenalties(
            W1FilingSummary w1Summary, BigDecimal totalW1Tax, 
            LocalDate dueDate, Integer taxYear) {
        
        BigDecimal lateFilingPenalty = BigDecimal.ZERO;
        BigDecimal missingFilingPenalty = BigDecimal.ZERO;
        
        // Calculate missing filing penalty
        // TODO: Fetch actual filing frequency from business profile service
        // For now, assuming monthly filing frequency (12 filings per year)
        // This should be enhanced to support quarterly (4), semi-monthly (24), and daily frequencies
        int expectedFilings = 12; // Default to monthly
        int missingFilings = Math.max(0, expectedFilings - w1Summary.getFilingCount());
        if (missingFilings > 0) {
            missingFilingPenalty = MISSING_FILING_PENALTY.multiply(
                new BigDecimal(missingFilings)
            );
        }
        
        BigDecimal totalPenalties = lateFilingPenalty.add(missingFilingPenalty);
        
        return PenaltyCalculation.builder()
            .lateFilingPenalty(lateFilingPenalty)
            .missingFilingPenalty(missingFilingPenalty)
            .totalPenalties(totalPenalties)
            .build();
    }
    
    /**
     * Calculate late filing penalty.
     * 
     * @param taxAmount Tax amount
     * @param dueDate Due date
     * @param filingDate Actual filing date
     * @return Late filing penalty amount
     */
    private BigDecimal calculateLateFilingPenalty(
            BigDecimal taxAmount, LocalDate dueDate, LocalDate filingDate) {
        
        if (!filingDate.isAfter(dueDate)) {
            return BigDecimal.ZERO;
        }
        
        // Calculate months late using proper month calculation
        java.time.Period period = java.time.Period.between(dueDate, filingDate);
        int monthsLate = period.getYears() * 12 + period.getMonths();
        // Partial months count as full month
        if (period.getDays() > 0) {
            monthsLate++;
        }
        // Ensure at least 1 month if filed after due date
        if (monthsLate == 0) {
            monthsLate = 1;
        }
        
        // Calculate penalty: 5% per month, max 25%
        BigDecimal penaltyRate = LATE_FILING_PENALTY_RATE.multiply(new BigDecimal(monthsLate));
        if (penaltyRate.compareTo(MAX_LATE_PENALTY_RATE) > 0) {
            penaltyRate = MAX_LATE_PENALTY_RATE;
        }
        
        BigDecimal penalty = taxAmount.multiply(penaltyRate);
        
        // Minimum penalty $50 if tax > $200
        if (taxAmount.compareTo(new BigDecimal("200")) > 0 && 
            penalty.compareTo(MINIMUM_PENALTY) < 0) {
            penalty = MINIMUM_PENALTY;
        }
        
        return penalty.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Generate confirmation number for W-3 submission.
     * 
     * @param w3 W-3 reconciliation
     * @return Confirmation number
     */
    private String generateConfirmationNumber(W3Reconciliation w3) {
        // Format: W3-YYYY-BUSINESSID-UUID
        if (w3.getBusinessId() == null || w3.getBusinessId().isEmpty()) {
            throw new IllegalStateException("Business ID cannot be null or empty for confirmation number generation");
        }
        String businessIdShort = w3.getBusinessId().substring(0, Math.min(8, w3.getBusinessId().length()));
        String uuidShort = w3.getId().toString().substring(0, 8);
        return String.format("W3-%d-%s-%s", w3.getTaxYear(), businessIdShort, uuidShort).toUpperCase();
    }
    
    /**
     * Convert W3Reconciliation entity to response DTO.
     * 
     * @param w3 W3Reconciliation entity
     * @return W3ReconciliationResponse
     */
    private W3ReconciliationResponse toResponse(W3Reconciliation w3) {
        return W3ReconciliationResponse.builder()
            .id(w3.getId())
            .tenantId(w3.getTenantId())
            .businessId(w3.getBusinessId())
            .taxYear(w3.getTaxYear())
            .totalW1Tax(w3.getTotalW1Tax())
            .totalW2Tax(w3.getTotalW2Tax())
            .discrepancy(w3.getDiscrepancy())
            .status(w3.getStatus())
            .w1FilingCount(w3.getW1FilingCount())
            .w2FormCount(w3.getW2FormCount())
            .totalEmployees(w3.getTotalEmployees())
            .lateFilingPenalty(w3.getLateFilingPenalty())
            .missingFilingPenalty(w3.getMissingFilingPenalty())
            .totalPenalties(w3.getTotalPenalties())
            .dueDate(w3.getDueDate())
            .filingDate(w3.getFilingDate())
            .isSubmitted(w3.getIsSubmitted())
            .confirmationNumber(w3.getConfirmationNumber())
            .notes(w3.getNotes())
            .createdAt(w3.getCreatedAt())
            .createdBy(w3.getCreatedBy())
            .updatedAt(w3.getUpdatedAt())
            .w1FilingIds(w3.getW1FilingIds())
            .build();
    }
    
    // Inner classes for data transfer
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class W1FilingSummary {
        private BigDecimal totalTax;
        private Integer filingCount;
        private List<String> filingIds;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class PenaltyCalculation {
        private BigDecimal lateFilingPenalty;
        private BigDecimal missingFilingPenalty;
        private BigDecimal totalPenalties;
    }
}
