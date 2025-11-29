package com.munitax.taxengine.controller;

import com.munitax.taxengine.domain.penalty.Interest;
import com.munitax.taxengine.dto.InterestCalculationRequest;
import com.munitax.taxengine.dto.InterestCalculationResponse;
import com.munitax.taxengine.repository.InterestRepository;
import com.munitax.taxengine.service.penalty.InterestCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for interest calculation operations.
 * 
 * Provides endpoints for:
 * - Calculating interest on unpaid tax (FR-027 to FR-032)
 * - Retrieving interest calculation details
 * - Retrieving interest history for a return
 * 
 * Functional Requirements:
 * - FR-027 to FR-032: Calculate interest with quarterly compounding
 */
@Slf4j
@RestController
@RequestMapping("/api/interest")
@RequiredArgsConstructor
public class InterestCalculationController {
    
    private final InterestCalculationService interestCalculationService;
    private final InterestRepository interestRepository;
    
    // TODO: Replace with actual authentication service
    private static final UUID MOCK_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    
    /**
     * Calculate interest on unpaid tax.
     * 
     * POST /api/interest/calculate
     * 
     * FR-027 to FR-032: Interest calculation with quarterly compounding
     * 
     * @param request interest calculation request
     * @return interest calculation response
     */
    @PostMapping("/calculate")
    public ResponseEntity<InterestCalculationResponse> calculateInterest(
            @Valid @RequestBody InterestCalculationRequest request) {
        
        log.info("Calculating interest for return: {}", request.getReturnId());
        
        try {
            // Set tenant ID and user ID from security context (mocked for now)
            if (request.getTenantId() == null) {
                request.setTenantId(MOCK_TENANT_ID);
            }
            if (request.getCreatedBy() == null) {
                request.setCreatedBy(MOCK_USER_ID);
            }
            
            InterestCalculationResponse response = interestCalculationService.calculateInterest(request);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for interest calculation: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error calculating interest", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to calculate interest", e);
        }
    }
    
    /**
     * Get interest calculation by ID.
     * 
     * GET /api/interest/{id}
     * 
     * @param id interest calculation ID
     * @return interest calculation
     */
    @GetMapping("/{id}")
    public ResponseEntity<Interest> getInterest(@PathVariable UUID id) {
        log.info("Retrieving interest calculation: {}", id);
        
        Interest interest = interestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Interest calculation not found: " + id));
        
        // TODO: Add tenant isolation check
        
        return ResponseEntity.ok(interest);
    }
    
    /**
     * Get all interest calculations for a return.
     * 
     * GET /api/interest/return/{returnId}
     * 
     * @param returnId the return ID
     * @return list of interest calculations
     */
    @GetMapping("/return/{returnId}")
    public ResponseEntity<List<Interest>> getInterestByReturnId(@PathVariable UUID returnId) {
        log.info("Retrieving interest calculations for return: {}", returnId);
        
        // TODO: Add tenant ID from security context
        List<Interest> interests = interestRepository
                .findByReturnIdAndTenantId(returnId, MOCK_TENANT_ID);
        
        return ResponseEntity.ok(interests);
    }
    
    /**
     * Get interest calculations for a tenant.
     * 
     * GET /api/interest/tenant/{tenantId}
     * 
     * @param tenantId the tenant ID
     * @return list of interest calculations
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<Interest>> getInterestByTenantId(@PathVariable UUID tenantId) {
        log.info("Retrieving interest calculations for tenant: {}", tenantId);
        
        // TODO: Add tenant authorization check
        
        List<Interest> interests = interestRepository.findByTenantId(tenantId);
        
        return ResponseEntity.ok(interests);
    }
}
