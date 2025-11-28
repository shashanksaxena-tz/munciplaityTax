package com.munitax.ledger.controller;

import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.service.TaxAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tax-assessments")
@RequiredArgsConstructor
@Slf4j
public class TaxAssessmentController {
    
    private final TaxAssessmentService taxAssessmentService;
    
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
        
        log.info("Recording tax assessment for filer {}: tax={}", filerId, taxAmount);
        JournalEntry entry = taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, returnId, taxAmount, penaltyAmount, 
                interestAmount, taxYear, taxPeriod);
        return ResponseEntity.ok(entry);
    }
}
