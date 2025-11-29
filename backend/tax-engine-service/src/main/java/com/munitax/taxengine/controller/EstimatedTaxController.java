package com.munitax.taxengine.controller;

import com.munitax.taxengine.domain.penalty.CalculationMethod;
import com.munitax.taxengine.domain.penalty.EstimatedTaxPenalty;
import com.munitax.taxengine.domain.penalty.Quarter;
import com.munitax.taxengine.dto.SafeHarborEvaluationDto;
import com.munitax.taxengine.repository.EstimatedTaxPenaltyRepository;
import com.munitax.taxengine.service.penalty.EstimatedTaxPenaltyService;
import com.munitax.taxengine.service.penalty.SafeHarborEvaluationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for estimated tax penalty operations.
 * 
 * Provides endpoints for:
 * - Evaluating safe harbor rules (FR-015 to FR-019)
 * - Calculating estimated tax penalties (FR-020 to FR-026)
 * - Retrieving estimated tax penalty details
 * 
 * Functional Requirements:
 * - FR-015 to FR-019: Safe harbor evaluation (90% current year OR 100%/110% prior year)
 * - FR-020 to FR-026: Calculate quarterly estimated tax underpayment penalty
 */
@Slf4j
@RestController
@RequestMapping("/api/estimated-tax")
@RequiredArgsConstructor
public class EstimatedTaxController {
    
    private final EstimatedTaxPenaltyService estimatedTaxPenaltyService;
    private final SafeHarborEvaluationService safeHarborEvaluationService;
    private final EstimatedTaxPenaltyRepository estimatedTaxPenaltyRepository;
    
    // TODO: Replace with actual authentication service
    private static final UUID MOCK_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    
    /**
     * Evaluate safe harbor rules for estimated tax.
     * 
     * POST /api/estimated-tax/evaluate-safe-harbor
     * 
     * FR-015 to FR-019: Safe harbor evaluation
     * 
     * @param request safe harbor evaluation request
     * @return safe harbor evaluation result
     */
    @PostMapping("/evaluate-safe-harbor")
    public ResponseEntity<SafeHarborEvaluationDto> evaluateSafeHarbor(
            @Valid @RequestBody SafeHarborRequest request) {
        
        log.info("Evaluating safe harbor for tax year: {}", request.getTaxYear());
        
        try {
            UUID tenantId = request.getTenantId() != null ? request.getTenantId() : MOCK_TENANT_ID;
            
            SafeHarborEvaluationDto evaluation = safeHarborEvaluationService.evaluateSafeHarbor(
                    tenantId,
                    request.getTaxYear(),
                    request.getCurrentYearTaxLiability(),
                    request.getTotalPaidEstimated(),
                    request.getAgi(),
                    request.getPriorYearTaxLiability()
            );
            
            return ResponseEntity.ok(evaluation);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for safe harbor evaluation: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error evaluating safe harbor", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to evaluate safe harbor", e);
        }
    }
    
    /**
     * Calculate estimated tax penalty for a tax return.
     * 
     * POST /api/estimated-tax/calculate-penalty
     * 
     * FR-020 to FR-026: Calculate quarterly estimated tax underpayment penalty
     * 
     * @param request estimated tax penalty calculation request
     * @return calculated estimated tax penalty
     */
    @PostMapping("/calculate-penalty")
    public ResponseEntity<EstimatedTaxPenalty> calculatePenalty(
            @Valid @RequestBody EstimatedTaxPenaltyRequest request) {
        
        log.info("Calculating estimated tax penalty for return: {}, tax year: {}",
                request.getReturnId(), request.getTaxYear());
        
        try {
            UUID tenantId = request.getTenantId() != null ? request.getTenantId() : MOCK_TENANT_ID;
            UUID createdBy = request.getCreatedBy() != null ? request.getCreatedBy() : MOCK_USER_ID;
            
            EstimatedTaxPenalty penalty = estimatedTaxPenaltyService.calculateEstimatedTaxPenalty(
                    tenantId,
                    request.getReturnId(),
                    request.getTaxYear(),
                    request.getAnnualTaxLiability(),
                    request.getQuarterlyPayments(),
                    request.getAgi(),
                    request.getPriorYearTaxLiability(),
                    request.getCalculationMethod(),
                    createdBy
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(penalty);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for estimated tax penalty calculation: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error calculating estimated tax penalty", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to calculate estimated tax penalty", e);
        }
    }
    
    /**
     * Get estimated tax penalty by ID.
     * 
     * GET /api/estimated-tax/penalties/{id}
     * 
     * @param id penalty ID
     * @return estimated tax penalty
     */
    @GetMapping("/penalties/{id}")
    public ResponseEntity<EstimatedTaxPenalty> getPenalty(@PathVariable UUID id) {
        log.info("Retrieving estimated tax penalty: {}", id);
        
        EstimatedTaxPenalty penalty = estimatedTaxPenaltyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Estimated tax penalty not found: " + id));
        
        // TODO: Add tenant isolation check
        
        return ResponseEntity.ok(penalty);
    }
    
    /**
     * Get estimated tax penalty by return ID.
     * 
     * GET /api/estimated-tax/penalties/return/{returnId}
     * 
     * @param returnId the return ID
     * @return estimated tax penalty
     */
    @GetMapping("/penalties/return/{returnId}")
    public ResponseEntity<EstimatedTaxPenalty> getPenaltyByReturnId(@PathVariable UUID returnId) {
        log.info("Retrieving estimated tax penalty for return: {}", returnId);
        
        // TODO: Add tenant ID from security context
        EstimatedTaxPenalty penalty = estimatedTaxPenaltyRepository
                .findByReturnIdAndTenantId(returnId, MOCK_TENANT_ID)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Estimated tax penalty not found for return: " + returnId));
        
        return ResponseEntity.ok(penalty);
    }
    
    /**
     * Request DTO for safe harbor evaluation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SafeHarborRequest {
        
        private UUID tenantId;
        
        @NotNull(message = "Tax year is required")
        private Integer taxYear;
        
        @NotNull(message = "Current year tax liability is required")
        @DecimalMin(value = "0.00", message = "Current year tax liability must be non-negative")
        private BigDecimal currentYearTaxLiability;
        
        @NotNull(message = "Total paid estimated is required")
        @DecimalMin(value = "0.00", message = "Total paid estimated must be non-negative")
        private BigDecimal totalPaidEstimated;
        
        @NotNull(message = "AGI is required")
        @DecimalMin(value = "0.00", message = "AGI must be non-negative")
        private BigDecimal agi;
        
        private BigDecimal priorYearTaxLiability;
    }
    
    /**
     * Request DTO for estimated tax penalty calculation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EstimatedTaxPenaltyRequest {
        
        private UUID tenantId;
        
        @NotNull(message = "Return ID is required")
        private UUID returnId;
        
        @NotNull(message = "Tax year is required")
        private Integer taxYear;
        
        @NotNull(message = "Annual tax liability is required")
        @DecimalMin(value = "0.00", message = "Annual tax liability must be non-negative")
        private BigDecimal annualTaxLiability;
        
        @NotNull(message = "Quarterly payments are required")
        private Map<Quarter, BigDecimal> quarterlyPayments;
        
        @NotNull(message = "AGI is required")
        @DecimalMin(value = "0.00", message = "AGI must be non-negative")
        private BigDecimal agi;
        
        private BigDecimal priorYearTaxLiability;
        
        private CalculationMethod calculationMethod;
        
        private UUID createdBy;
    }
}
