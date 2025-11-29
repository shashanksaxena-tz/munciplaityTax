package com.munitax.taxengine.controller;

import com.munitax.taxengine.domain.penalty.*;
import com.munitax.taxengine.repository.PenaltyAbatementRepository;
import com.munitax.taxengine.service.penalty.PenaltyAbatementService;
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
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for penalty abatement operations.
 * 
 * Provides endpoints for:
 * - Submitting penalty abatement requests (FR-033 to FR-035)
 * - Reviewing and approving/denying requests (FR-037, FR-039)
 * - Uploading supporting documents (FR-038)
 * - Generating Form 27-PA (FR-036)
 * 
 * Functional Requirements:
 * - FR-033 to FR-039: Penalty abatement workflow
 */
@Slf4j
@RestController
@RequestMapping("/api/abatements")
@RequiredArgsConstructor
public class PenaltyAbatementController {
    
    private final PenaltyAbatementService abatementService;
    private final PenaltyAbatementRepository abatementRepository;
    
    // TODO: Replace with actual authentication service
    private static final UUID MOCK_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    
    /**
     * Submit a penalty abatement request.
     * 
     * POST /api/abatements
     * 
     * FR-033: Display penalty abatement request option
     * FR-034: Support valid abatement reasons
     * FR-035: Validate first-time penalty abatement eligibility
     * 
     * @param request abatement request
     * @return created penalty abatement
     */
    @PostMapping
    public ResponseEntity<PenaltyAbatement> submitAbatementRequest(
            @Valid @RequestBody AbatementRequest request) {
        
        log.info("Submitting penalty abatement request for return: {}", request.getReturnId());
        
        try {
            UUID tenantId = request.getTenantId() != null ? request.getTenantId() : MOCK_TENANT_ID;
            UUID requestedBy = request.getRequestedBy() != null ? request.getRequestedBy() : MOCK_USER_ID;
            
            PenaltyAbatement abatement = abatementService.submitAbatementRequest(
                    tenantId,
                    request.getReturnId(),
                    request.getPenaltyId(),
                    request.getAbatementType(),
                    request.getRequestedAmount(),
                    request.getReason(),
                    request.getExplanation(),
                    requestedBy
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(abatement);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Invalid abatement request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error submitting abatement request", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to submit abatement request", e);
        }
    }
    
    /**
     * Get abatement request by ID.
     * 
     * GET /api/abatements/{id}
     * 
     * @param id abatement request ID
     * @return penalty abatement
     */
    @GetMapping("/{id}")
    public ResponseEntity<PenaltyAbatement> getAbatement(@PathVariable UUID id) {
        log.info("Retrieving penalty abatement request: {}", id);
        
        PenaltyAbatement abatement = abatementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Penalty abatement not found: " + id));
        
        // TODO: Add tenant isolation check
        
        return ResponseEntity.ok(abatement);
    }
    
    /**
     * Get abatement requests by return ID.
     * 
     * GET /api/abatements/return/{returnId}
     * 
     * @param returnId the return ID
     * @return list of abatement requests
     */
    @GetMapping("/return/{returnId}")
    public ResponseEntity<List<PenaltyAbatement>> getAbatementsByReturnId(@PathVariable UUID returnId) {
        log.info("Retrieving penalty abatements for return: {}", returnId);
        
        // TODO: Add tenant ID from security context
        List<PenaltyAbatement> abatements = abatementRepository
                .findByReturnIdAndTenantId(returnId, MOCK_TENANT_ID);
        
        return ResponseEntity.ok(abatements);
    }
    
    /**
     * Get pending abatement requests for a tenant.
     * 
     * GET /api/abatements/tenant/{tenantId}/pending
     * 
     * @param tenantId the tenant ID
     * @return list of pending abatement requests
     */
    @GetMapping("/tenant/{tenantId}/pending")
    public ResponseEntity<List<PenaltyAbatement>> getPendingAbatements(@PathVariable UUID tenantId) {
        log.info("Retrieving pending abatement requests for tenant: {}", tenantId);
        
        // TODO: Add tenant authorization check
        
        List<PenaltyAbatement> abatements = abatementRepository
                .findByTenantIdAndStatus(tenantId, AbatementStatus.PENDING);
        
        return ResponseEntity.ok(abatements);
    }
    
    /**
     * Review and approve/deny an abatement request.
     * 
     * PATCH /api/abatements/{id}/review
     * 
     * FR-037: Track abatement status
     * FR-039: Administrator review and approve/deny
     * 
     * @param id abatement request ID
     * @param request review request
     * @return updated penalty abatement
     */
    @PatchMapping("/{id}/review")
    public ResponseEntity<PenaltyAbatement> reviewAbatement(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request) {
        
        log.info("Reviewing penalty abatement request: {}, status: {}", id, request.getStatus());
        
        try {
            UUID reviewedBy = request.getReviewedBy() != null ? request.getReviewedBy() : MOCK_USER_ID;
            
            PenaltyAbatement abatement = abatementService.reviewAbatementRequest(
                    id,
                    request.getStatus(),
                    request.getApprovedAmount(),
                    request.getReviewNotes(),
                    reviewedBy
            );
            
            return ResponseEntity.ok(abatement);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Invalid review request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error reviewing abatement request", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to review abatement request", e);
        }
    }
    
    /**
     * Withdraw a pending abatement request.
     * 
     * PATCH /api/abatements/{id}/withdraw
     * 
     * @param id abatement request ID
     * @return updated penalty abatement
     */
    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<PenaltyAbatement> withdrawAbatement(@PathVariable UUID id) {
        log.info("Withdrawing penalty abatement request: {}", id);
        
        try {
            PenaltyAbatement abatement = abatementService.withdrawAbatementRequest(id, MOCK_USER_ID);
            return ResponseEntity.ok(abatement);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Cannot withdraw abatement: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error withdrawing abatement request", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to withdraw abatement request", e);
        }
    }
    
    /**
     * Upload supporting document for an abatement request.
     * 
     * POST /api/abatements/{id}/documents
     * 
     * FR-038: Allow document upload
     * 
     * @param id abatement request ID
     * @param request document upload request
     * @return updated penalty abatement
     */
    @PostMapping("/{id}/documents")
    public ResponseEntity<PenaltyAbatement> uploadDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentUploadRequest request) {
        
        log.info("Uploading document for abatement request: {}", id);
        
        try {
            PenaltyAbatement abatement = abatementService.uploadSupportingDocument(
                    id,
                    request.getDocumentUrl(),
                    request.getDocumentType()
            );
            
            return ResponseEntity.ok(abatement);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid document upload: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error uploading document", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload document", e);
        }
    }
    
    /**
     * Generate Form 27-PA (Penalty Abatement Request) PDF.
     * 
     * GET /api/abatements/{id}/form-27pa
     * 
     * FR-036: Generate Form 27-PA PDF
     * 
     * @param id abatement request ID
     * @return URL of generated PDF
     */
    @GetMapping("/{id}/form-27pa")
    public ResponseEntity<FormUrlResponse> generateForm27PA(@PathVariable UUID id) {
        log.info("Generating Form 27-PA for abatement request: {}", id);
        
        try {
            String pdfUrl = abatementService.generateForm27PA(id);
            
            FormUrlResponse response = FormUrlResponse.builder()
                    .formUrl(pdfUrl)
                    .formName("Form 27-PA - Penalty Abatement Request")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Cannot generate form: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error generating Form 27-PA", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate Form 27-PA", e);
        }
    }
    
    /**
     * Request DTO for submitting an abatement.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AbatementRequest {
        
        private UUID tenantId;
        
        @NotNull(message = "Return ID is required")
        private UUID returnId;
        
        private UUID penaltyId;
        
        @NotNull(message = "Abatement type is required")
        private AbatementType abatementType;
        
        @NotNull(message = "Requested amount is required")
        @DecimalMin(value = "0.01", message = "Requested amount must be positive")
        private BigDecimal requestedAmount;
        
        @NotNull(message = "Reason is required")
        private AbatementReason reason;
        
        private String explanation;
        
        private UUID requestedBy;
    }
    
    /**
     * Request DTO for reviewing an abatement.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewRequest {
        
        @NotNull(message = "Status is required")
        private AbatementStatus status;
        
        private BigDecimal approvedAmount;
        
        private String reviewNotes;
        
        private UUID reviewedBy;
    }
    
    /**
     * Request DTO for uploading a document.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentUploadRequest {
        
        @NotNull(message = "Document URL is required")
        private String documentUrl;
        
        @NotNull(message = "Document type is required")
        private String documentType;
    }
    
    /**
     * Response DTO for form URL.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FormUrlResponse {
        
        private String formUrl;
        
        private String formName;
    }
}
