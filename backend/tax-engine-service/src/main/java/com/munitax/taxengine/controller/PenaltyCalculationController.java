package com.munitax.taxengine.controller;

import com.munitax.taxengine.domain.penalty.Penalty;
import com.munitax.taxengine.dto.PenaltyCalculationRequest;
import com.munitax.taxengine.dto.PenaltyCalculationResponse;
import com.munitax.taxengine.repository.PenaltyRepository;
import com.munitax.taxengine.service.penalty.CombinedPenaltyCapService;
import com.munitax.taxengine.service.penalty.LateFilingPenaltyService;
import com.munitax.taxengine.service.penalty.LatePaymentPenaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for penalty calculation operations.
 * 
 * Provides endpoints for:
 * - Calculating late filing penalties (FR-001 to FR-006)
 * - Calculating late payment penalties (FR-007 to FR-011)
 * - Applying combined penalty caps (FR-012 to FR-014)
 * - Retrieving penalty details and history
 * 
 * Functional Requirements:
 * - FR-001 to FR-006: Late filing penalty (5% per month, max 25%)
 * - FR-007 to FR-011: Late payment penalty (1% per month, max 25%)
 * - FR-012 to FR-014: Combined penalty cap (max 5% per month when both apply)
 */
@Slf4j
@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
public class PenaltyCalculationController {
    
    private final LateFilingPenaltyService lateFilingPenaltyService;
    private final LatePaymentPenaltyService latePaymentPenaltyService;
    private final CombinedPenaltyCapService combinedPenaltyCapService;
    private final PenaltyRepository penaltyRepository;
    
    // TODO: Replace with actual authentication service
    private static final UUID MOCK_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    
    /**
     * Calculate all applicable penalties for a tax return.
     * Automatically determines which penalties apply and applies combined cap if needed.
     * 
     * POST /api/penalties/calculate
     * 
     * @param request penalty calculation request
     * @return calculated penalties with combined cap applied if both penalties exist
     */
    @PostMapping("/calculate")
    public ResponseEntity<PenaltyCalculationResponse> calculatePenalties(
            @Valid @RequestBody PenaltyCalculationRequest request) {
        
        log.info("Calculating penalties for return: {}", request.getReturnId());
        
        try {
            // Set tenant ID and user ID from security context (mocked for now)
            if (request.getTenantId() == null) {
                request.setTenantId(MOCK_TENANT_ID);
            }
            if (request.getCreatedBy() == null) {
                request.setCreatedBy(MOCK_USER_ID);
            }
            
            // Determine penalty type from request or calculate both
            String penaltyType = request.getPenaltyType();
            
            if ("LATE_FILING".equalsIgnoreCase(penaltyType)) {
                // Calculate late filing penalty only
                PenaltyCalculationResponse response = lateFilingPenaltyService
                        .calculateLateFilingPenalty(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
            else if ("LATE_PAYMENT".equalsIgnoreCase(penaltyType)) {
                // Calculate late payment penalty only
                PenaltyCalculationResponse response = latePaymentPenaltyService
                        .calculateLatePaymentPenalty(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
            else {
                // Calculate both penalties with combined cap
                PenaltyCalculationResponse response = combinedPenaltyCapService
                        .calculateCombinedPenalties(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid penalty calculation request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error calculating penalties", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to calculate penalties: " + e.getMessage());
        }
    }
    
    /**
     * Get penalty details by ID.
     * 
     * GET /api/penalties/{id}
     * 
     * @param id penalty ID
     * @return penalty details
     */
    @GetMapping("/{id}")
    public ResponseEntity<PenaltyCalculationResponse> getPenaltyById(@PathVariable String id) {
        log.info("Retrieving penalty: {}", id);
        
        try {
            UUID penaltyId = UUID.fromString(id);
            Penalty penalty = penaltyRepository.findByIdAndTenantId(penaltyId, MOCK_TENANT_ID)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Penalty not found: " + id));
            
            PenaltyCalculationResponse response = buildResponseFromPenalty(penalty);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid penalty ID format");
        }
    }
    
    /**
     * Get all penalties with pagination.
     * 
     * GET /api/penalties?page=0&size=20&sort=assessmentDate,desc
     * 
     * @param page page number (default 0)
     * @param size page size (default 20)
     * @param sortBy sort field (default assessmentDate)
     * @param sortDir sort direction (default DESC)
     * @return page of penalties
     */
    @GetMapping
    public ResponseEntity<Page<PenaltyCalculationResponse>> getPenalties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "assessmentDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        log.info("Retrieving penalties: page={}, size={}, sortBy={}, sortDir={}", 
                page, size, sortBy, sortDir);
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Penalty> penalties = penaltyRepository
                    .findByTenantIdOrderByAssessmentDateDesc(MOCK_TENANT_ID, pageable);
            
            Page<PenaltyCalculationResponse> response = penalties
                    .map(this::buildResponseFromPenalty);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving penalties", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to retrieve penalties: " + e.getMessage());
        }
    }
    
    /**
     * Get all penalties for a specific tax return.
     * 
     * GET /api/penalties/return/{returnId}
     * 
     * @param returnId tax return ID
     * @param activeOnly whether to return only non-abated penalties (default false)
     * @return list of penalties for the return
     */
    @GetMapping("/return/{returnId}")
    public ResponseEntity<List<PenaltyCalculationResponse>> getPenaltiesByReturn(
            @PathVariable String returnId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        
        log.info("Retrieving penalties for return: {}, activeOnly={}", returnId, activeOnly);
        
        try {
            UUID returnUuid = UUID.fromString(returnId);
            
            List<Penalty> penalties;
            if (activeOnly) {
                penalties = penaltyRepository.findActiveByReturnIdAndTenantId(returnUuid, MOCK_TENANT_ID);
            } else {
                penalties = penaltyRepository.findByReturnIdAndTenantId(returnUuid, MOCK_TENANT_ID);
            }
            
            List<PenaltyCalculationResponse> response = penalties.stream()
                    .map(this::buildResponseFromPenalty)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid return ID format");
        } catch (Exception e) {
            log.error("Error retrieving penalties for return", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to retrieve penalties: " + e.getMessage());
        }
    }
    
    /**
     * Get combined penalty summary for a return.
     * Shows both late filing and late payment penalties with combined cap applied.
     * 
     * GET /api/penalties/return/{returnId}/combined
     * 
     * @param returnId tax return ID
     * @return combined penalty summary
     */
    @GetMapping("/return/{returnId}/combined")
    public ResponseEntity<PenaltyCalculationResponse> getCombinedPenaltySummary(
            @PathVariable String returnId) {
        
        log.info("Retrieving combined penalty summary for return: {}", returnId);
        
        try {
            PenaltyCalculationResponse response = combinedPenaltyCapService
                    .getCombinedPenaltySummary(returnId, MOCK_TENANT_ID.toString());
            
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "No penalties found for return: " + returnId);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid return ID format");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving combined penalty summary", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to retrieve combined penalty summary: " + e.getMessage());
        }
    }
    
    /**
     * Build response from penalty entity.
     */
    private PenaltyCalculationResponse buildResponseFromPenalty(Penalty penalty) {
        PenaltyCalculationResponse.PenaltyCalculationResponseBuilder builder = 
                PenaltyCalculationResponse.builder()
                .penaltyId(penalty.getId().toString())
                .returnId(penalty.getReturnId().toString())
                .taxYearAndPeriod(penalty.getTaxDueDate().getYear() + "")
                .dueDate(penalty.getTaxDueDate())
                .taxDue(penalty.getUnpaidTaxAmount())
                .totalPenalties(penalty.getPenaltyAmount())
                .isAbated(penalty.getIsAbated())
                .abatementReason(penalty.getAbatementReason());
        
        // Set penalty-specific fields based on type
        switch (penalty.getPenaltyType()) {
            case LATE_FILING:
                builder.lateFilingPenalty(penalty.getPenaltyAmount())
                        .lateFilingPenaltyRate(penalty.getPenaltyRate().multiply(
                                java.math.BigDecimal.valueOf(100)))
                        .filingDate(penalty.getActualDate())
                        .daysLate(java.time.Period.between(penalty.getTaxDueDate(), 
                                penalty.getActualDate()).getDays());
                break;
            case LATE_PAYMENT:
                builder.latePaymentPenalty(penalty.getPenaltyAmount())
                        .latePaymentPenaltyRate(penalty.getPenaltyRate().multiply(
                                java.math.BigDecimal.valueOf(100)))
                        .paymentDate(penalty.getActualDate())
                        .daysLate(java.time.Period.between(penalty.getTaxDueDate(), 
                                penalty.getActualDate()).getDays());
                break;
            default:
                // Other penalty types
                break;
        }
        
        return builder.build();
    }
}
