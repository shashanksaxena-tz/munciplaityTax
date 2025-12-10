package com.munitax.submission.controller;

import com.munitax.submission.dto.W3DiscrepancyResponse;
import com.munitax.submission.dto.W3ReconciliationRequest;
import com.munitax.submission.dto.W3ReconciliationResponse;
import com.munitax.submission.service.W3ReconciliationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for W-3 year-end reconciliation operations.
 * 
 * Endpoints:
 * - POST /api/v1/w3-reconciliation - Create W-3 reconciliation
 * - GET /api/v1/w3-reconciliation/{year} - Get reconciliation by year
 * - GET /api/v1/w3-reconciliation/id/{id} - Get reconciliation by ID
 * - POST /api/v1/w3-reconciliation/{id}/submit - Submit W-3
 * - GET /api/v1/w3-reconciliation/{id}/discrepancies - Get discrepancies
 */
@RestController
@RequestMapping("/api/v1/w3-reconciliation")
public class W3ReconciliationController {
    
    private static final Logger logger = LoggerFactory.getLogger(W3ReconciliationController.class);
    
    private final W3ReconciliationService w3ReconciliationService;
    
    public W3ReconciliationController(W3ReconciliationService w3ReconciliationService) {
        this.w3ReconciliationService = w3ReconciliationService;
    }
    
    /**
     * Create a new W-3 reconciliation.
     * 
     * POST /api/v1/w3-reconciliation
     * 
     * @param request W-3 reconciliation request
     * @param userId User ID from authentication (header or JWT)
     * @param tenantId Tenant ID from authentication (header or JWT)
     * @return Created W-3 reconciliation
     */
    @PostMapping
    public ResponseEntity<W3ReconciliationResponse> createW3Reconciliation(
            @Valid @RequestBody W3ReconciliationRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "dublin") String tenantId) {
        
        logger.info("Creating W-3 reconciliation for business {} tax year {}", 
                    request.getBusinessId(), request.getTaxYear());
        
        try {
            W3ReconciliationResponse response = w3ReconciliationService.createW3Reconciliation(
                request, userId, tenantId
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for W-3 reconciliation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error creating W-3 reconciliation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get W-3 reconciliation by year.
     * 
     * GET /api/v1/w3-reconciliation/{year}
     * 
     * @param year Tax year
     * @param businessId Business ID (query parameter)
     * @param tenantId Tenant ID from authentication (header or JWT)
     * @return W-3 reconciliation for the specified year
     */
    @GetMapping("/{year}")
    public ResponseEntity<W3ReconciliationResponse> getW3ReconciliationByYear(
            @PathVariable Integer year,
            @RequestParam String businessId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "dublin") String tenantId) {
        
        logger.info("Fetching W-3 reconciliation for business {} tax year {}", businessId, year);
        
        try {
            W3ReconciliationResponse response = w3ReconciliationService.getW3ReconciliationByYear(
                businessId, year, tenantId
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("W-3 reconciliation not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error fetching W-3 reconciliation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get W-3 reconciliation by ID.
     * 
     * GET /api/v1/w3-reconciliation/id/{id}
     * 
     * @param id W-3 reconciliation ID
     * @param tenantId Tenant ID from authentication (header or JWT)
     * @return W-3 reconciliation
     */
    @GetMapping("/id/{id}")
    public ResponseEntity<W3ReconciliationResponse> getW3ReconciliationById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "dublin") String tenantId) {
        
        logger.info("Fetching W-3 reconciliation with ID {}", id);
        
        try {
            W3ReconciliationResponse response = w3ReconciliationService.getW3ReconciliationById(
                id, tenantId
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("W-3 reconciliation not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (SecurityException e) {
            logger.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error fetching W-3 reconciliation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Submit W-3 reconciliation.
     * 
     * POST /api/v1/w3-reconciliation/{id}/submit
     * 
     * @param id W-3 reconciliation ID
     * @param tenantId Tenant ID from authentication (header or JWT)
     * @return Updated W-3 reconciliation with submission details
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<W3ReconciliationResponse> submitW3Reconciliation(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "dublin") String tenantId) {
        
        logger.info("Submitting W-3 reconciliation with ID {}", id);
        
        try {
            W3ReconciliationResponse response = w3ReconciliationService.submitW3Reconciliation(
                id, tenantId
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Invalid request for W-3 submission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (SecurityException e) {
            logger.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error submitting W-3 reconciliation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get discrepancies for W-3 reconciliation.
     * 
     * GET /api/v1/w3-reconciliation/{id}/discrepancies
     * 
     * @param id W-3 reconciliation ID
     * @param tenantId Tenant ID from authentication (header or JWT)
     * @return Discrepancy details with breakdown and recommendations
     */
    @GetMapping("/{id}/discrepancies")
    public ResponseEntity<W3DiscrepancyResponse> getDiscrepancies(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "dublin") String tenantId) {
        
        logger.info("Fetching discrepancies for W-3 reconciliation with ID {}", id);
        
        try {
            W3DiscrepancyResponse response = w3ReconciliationService.getDiscrepancies(
                id, tenantId
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("W-3 reconciliation not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (SecurityException e) {
            logger.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error fetching discrepancies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
