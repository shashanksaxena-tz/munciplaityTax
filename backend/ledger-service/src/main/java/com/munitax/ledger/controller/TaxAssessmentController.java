package com.munitax.ledger.controller;

import com.munitax.ledger.dto.TaxAssessmentRequest;
import com.munitax.ledger.dto.TaxAssessmentResponse;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.service.TaxAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * T015 - Tax Assessment Controller
 * 
 * Handles recording of tax assessments and automatic creation of double-entry journal entries.
 * When a tax return is filed and tax is calculated, this endpoint creates:
 * - Filer journal entry: DEBIT Tax Expense, CREDIT Tax Liability
 * - Municipality journal entry: DEBIT Accounts Receivable, CREDIT Tax Revenue
 * 
 * Also handles penalty and interest assessments with proper account allocations.
 * 
 * @see TaxAssessmentService for business logic
 */
@RestController
@RequestMapping("/api/v1/tax-assessments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tax Assessments", description = "Record tax assessments with double-entry journal entries")
public class TaxAssessmentController {
    
    private final TaxAssessmentService taxAssessmentService;
    
    /**
     * T015 - Record Tax Assessment with Journal Entries
     * 
     * Creates double-entry journal entries for a tax assessment on both filer and municipality books.
     * Supports compound assessments (tax + penalty + interest).
     * 
     * @param tenantId Municipality/tenant identifier
     * @param filerId Filer entity identifier (business or individual)
     * @param returnId Tax return identifier that triggered the assessment
     * @param taxAmount Base tax amount assessed
     * @param penaltyAmount Penalty amount (optional, defaults to 0)
     * @param interestAmount Interest amount (optional, defaults to 0)
     * @param taxYear Tax year (e.g., "2024")
     * @param taxPeriod Tax period (e.g., "Q1", "Q2", "Annual")
     * @return Journal entry details for the assessment
     * 
     * Example:
     * POST /api/v1/tax-assessments/record?tenantId=uuid&filerId=uuid&returnId=uuid
     *   &taxAmount=10000.00&penaltyAmount=500.00&interestAmount=150.00
     *   &taxYear=2024&taxPeriod=Q1
     * 
     * Response:
     * {
     *   "entryId": "uuid",
     *   "entryNumber": "JE-2024-00001",
     *   "sourceType": "TAX_ASSESSMENT",
     *   "lines": [
     *     {"accountNumber": "2100", "debit": 0, "credit": 10000, "description": "Tax liability"},
     *     {"accountNumber": "6100", "debit": 10000, "credit": 0, "description": "Tax expense"},
     *     ...
     *   ]
     * }
     */
    @PostMapping("/record")
    @Operation(
        summary = "Record tax assessment (Query Parameters)",
        description = """
            Records a tax assessment and creates double-entry journal entries.
            Creates entries on both filer and municipality books.
            Supports compound assessments with tax, penalty, and interest.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tax assessment recorded successfully",
            content = @Content(schema = @Schema(implementation = JournalEntry.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid assessment data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<JournalEntry> recordTaxAssessment(
            @RequestParam @Parameter(description = "Tenant UUID") String tenantId,
            @RequestParam @Parameter(description = "Filer UUID") UUID filerId,
            @RequestParam @Parameter(description = "Tax Return UUID") UUID returnId,
            @RequestParam @Parameter(description = "Base tax amount") BigDecimal taxAmount,
            @RequestParam(defaultValue = "0") @Parameter(description = "Penalty amount") BigDecimal penaltyAmount,
            @RequestParam(defaultValue = "0") @Parameter(description = "Interest amount") BigDecimal interestAmount,
            @RequestParam @Parameter(description = "Tax year (e.g., 2024)") String taxYear,
            @RequestParam @Parameter(description = "Tax period (e.g., Q1, Q2, Annual)") String taxPeriod) {
        
        log.info("Recording tax assessment for filer {}: tax={}, penalty={}, interest={}", 
                 filerId, taxAmount, penaltyAmount, interestAmount);
        
        JournalEntry entry = taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, returnId, taxAmount, penaltyAmount, 
                interestAmount, taxYear, taxPeriod);
        
        log.info("Tax assessment recorded successfully: entryId={}, entryNumber={}", 
                 entry.getEntryId(), entry.getEntryNumber());
        
        return ResponseEntity.ok(entry);
    }
    
    /**
     * T015 - Record Tax Assessment (DTO version)
     * 
     * Alternative endpoint that accepts TaxAssessmentRequest DTO and returns structured response.
     * 
     * @param request TaxAssessmentRequest containing all assessment details
     * @return TaxAssessmentResponse with assessment summary and journal entry details
     */
    @PostMapping
    @Operation(
        summary = "Record tax assessment (DTO)",
        description = """
            Records a tax assessment using a request DTO.
            Automatically creates double-entry journal entries on both books.
            Returns detailed assessment response with journal entry information.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tax assessment recorded successfully",
            content = @Content(schema = @Schema(implementation = TaxAssessmentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid assessment request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TaxAssessmentResponse> recordTaxAssessmentWithDTO(
            @Valid @RequestBody @Parameter(description = "Tax assessment request") TaxAssessmentRequest request) {
        
        log.info("Recording tax assessment (DTO) for filer {}: totalAmount={}", 
                 request.getFilerId(), request.getTotalAmount());
        
        // Set defaults if not provided
        if (request.getPenaltyAmount() == null) {
            request.setPenaltyAmount(BigDecimal.ZERO);
        }
        if (request.getInterestAmount() == null) {
            request.setInterestAmount(BigDecimal.ZERO);
        }
        
        JournalEntry entry = taxAssessmentService.recordTaxAssessment(
                request.getTenantId(),
                request.getFilerId(),
                request.getReturnId(),
                request.getTaxAmount(),
                request.getPenaltyAmount(),
                request.getInterestAmount(),
                request.getTaxYear(),
                request.getTaxPeriod()
        );
        
        TaxAssessmentResponse response = TaxAssessmentResponse.builder()
                .filerJournalEntryId(entry.getEntryId())
                .entryNumber(entry.getEntryNumber())
                .filerId(request.getFilerId())
                .returnId(request.getReturnId())
                .assessmentDate(entry.getEntryDate())
                .taxAmount(request.getTaxAmount())
                .penaltyAmount(request.getPenaltyAmount())
                .interestAmount(request.getInterestAmount())
                .totalAmount(request.getTotalAmount())
                .taxYear(request.getTaxYear())
                .taxPeriod(request.getTaxPeriod())
                .sourceType(entry.getSourceType())
                .createdAt(LocalDateTime.now())
                .description(entry.getDescription())
                .message("Tax assessment recorded successfully")
                .build();
        
        log.info("Tax assessment recorded: entryNumber={}", response.getEntryNumber());
        
        return ResponseEntity.ok(response);
    }
}
