package com.munitax.ledger.controller;

import com.munitax.ledger.dto.TaxAssessmentRequest;
import com.munitax.ledger.dto.TaxAssessmentResponse;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.service.TaxAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<JournalEntry> recordTaxAssessment(
            @RequestParam UUID tenantId,
            @RequestParam UUID filerId,
            @RequestParam UUID returnId,
            @RequestParam BigDecimal taxAmount,
            @RequestParam(defaultValue = "0") BigDecimal penaltyAmount,
            @RequestParam(defaultValue = "0") BigDecimal interestAmount,
            @RequestParam String taxYear,
            @RequestParam String taxPeriod) {
        
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
    public ResponseEntity<TaxAssessmentResponse> recordTaxAssessmentWithDTO(
            @RequestBody TaxAssessmentRequest request) {
        
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
