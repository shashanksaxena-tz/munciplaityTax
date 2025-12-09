package com.munitax.taxengine.controller;

import com.munitax.taxengine.dto.ReconciliationIssue;
import com.munitax.taxengine.dto.W1FilingRequest;
import com.munitax.taxengine.dto.W1FilingResponse;
import com.munitax.taxengine.model.W2Form;
import com.munitax.taxengine.service.W1FilingService;
import com.munitax.taxengine.service.WithholdingReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for W-1 withholding return filing and reconciliation operations.
 * 
 * Endpoints:
 * - POST /api/v1/w1-filings - File new W-1 return
 * - POST /api/v1/w1-filings/reconcile - Run reconciliation for an employer
 * - GET /api/v1/w1-filings/reconciliation/{employerId} - Get reconciliation issues
 * 
 * @see W1FilingService
 * @see WithholdingReconciliationService
 */
@RestController
@RequestMapping("/api/v1/w1-filings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "W-1 Filing", description = "W-1 withholding return filing and reconciliation operations")
public class W1FilingController {
    
    private final W1FilingService w1FilingService;
    private final WithholdingReconciliationService reconciliationService;
    
    /**
     * File a new W-1 withholding return.
     * 
     * @param request W-1 filing request
     * @return W-1 filing response with calculated totals
     */
    @PostMapping
    @Operation(summary = "File W-1 return", description = "Submit a new W-1 withholding return filing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "W-1 filing created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Duplicate filing - W-1 already exists for this period")
    })
    public ResponseEntity<W1FilingResponse> fileW1Return(
            @Valid @RequestBody W1FilingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader) {
        
        // Parse user and tenant IDs from headers (in production, these would come from JWT)
        UUID userId = userIdHeader != null ? UUID.fromString(userIdHeader) : UUID.randomUUID();
        UUID tenantId = tenantIdHeader != null ? UUID.fromString(tenantIdHeader) : UUID.randomUUID();
        
        log.info("Filing W-1 return for business {} tax year {} period {}", 
                 request.getBusinessId(), request.getTaxYear(), request.getPeriod());
        
        W1FilingResponse response = w1FilingService.fileW1Return(request, userId, tenantId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Run comprehensive reconciliation for an employer's W-1 filings.
     * 
     * Performs the following checks:
     * - W-1 wages match W-2 Box 1 (federal wages)
     * - W-1 local wages match W-2 Box 18 (local wages)
     * - Withholding rate within 0-3.0% range
     * - Quarterly totals match cumulative
     * - All required periods filed
     * - No duplicate EIN filings for same period
     * - Late filings detected
     * 
     * @param request Reconciliation request containing employer ID, tax year, and optional W-2 forms
     * @return List of reconciliation issues found
     */
    @PostMapping("/reconcile")
    @Operation(
        summary = "Run W-1 reconciliation", 
        description = "Reconcile W-1 filings against W-2 forms and detect issues"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reconciliation completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "No W-1 filings found for employer")
    })
    public ResponseEntity<List<ReconciliationIssue>> reconcileW1Filings(
            @Valid @RequestBody ReconciliationRequest request) {
        
        log.info("Running reconciliation for employer {} tax year {}", 
                 request.getEmployerId(), request.getTaxYear());
        
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            request.getEmployerId(), 
            request.getTaxYear(), 
            request.getW2Forms()
        );
        
        return ResponseEntity.ok(issues);
    }
    
    /**
     * Get reconciliation issues for a specific employer.
     * 
     * @param employerId Employer/Business ID
     * @param taxYear Tax year to check (optional, defaults to current year)
     * @return List of reconciliation issues
     */
    @GetMapping("/reconciliation/{employerId}")
    @Operation(
        summary = "Get reconciliation issues", 
        description = "Retrieve reconciliation issues for an employer"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reconciliation issues retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Employer not found")
    })
    public ResponseEntity<List<ReconciliationIssue>> getReconciliationIssues(
            @Parameter(description = "Employer/Business ID") 
            @PathVariable UUID employerId,
            @Parameter(description = "Tax year (optional)")
            @RequestParam(required = false) Integer taxYear) {
        
        // Default to current year if not specified
        Integer year = taxYear != null ? taxYear : java.time.Year.now().getValue();
        
        log.info("Retrieving reconciliation issues for employer {} tax year {}", employerId, year);
        
        List<ReconciliationIssue> issues = reconciliationService.getReconciliationIssues(employerId, year);
        
        return ResponseEntity.ok(issues);
    }
    
    /**
     * Request DTO for reconciliation endpoint.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReconciliationRequest {
        
        /**
         * Employer/Business ID to reconcile.
         */
        @NotNull(message = "Employer ID is required")
        private UUID employerId;
        
        /**
         * Tax year to reconcile.
         */
        @NotNull(message = "Tax year is required")
        @Min(value = 2020, message = "Tax year must be 2020 or later")
        private Integer taxYear;
        
        /**
         * Optional list of W-2 forms for reconciliation.
         * If provided, will compare W-1 totals to W-2 totals.
         */
        private List<W2Form> w2Forms;
    }
}
