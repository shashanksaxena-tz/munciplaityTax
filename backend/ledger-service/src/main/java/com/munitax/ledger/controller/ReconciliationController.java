package com.munitax.ledger.controller;

import com.munitax.ledger.dto.ReconciliationResponse;
import com.munitax.ledger.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
@Slf4j
public class ReconciliationController {
    
    private final ReconciliationService reconciliationService;
    
    @GetMapping("/report/{tenantId}/{municipalityId}")
    public ResponseEntity<ReconciliationResponse> getReconciliationReport(
            @PathVariable String tenantId,
            @PathVariable String municipalityId) {
        
        log.info("Generating reconciliation report for tenant {}", tenantId);
        ReconciliationResponse report = reconciliationService.generateReconciliationReport(
                tenantId, municipalityId);
        return ResponseEntity.ok(report);
    }
    
    /**
     * T025: Get drill-down reconciliation for a specific filer.
     * 
     * @param tenantId The tenant identifier
     * @param filerId The filer identifier
     * @param municipalityId The municipality identifier
     * @return ReconciliationResponse for the specific filer
     */
    @GetMapping("/{tenantId}/{municipalityId}/filer/{filerId}")
    public ResponseEntity<ReconciliationResponse> getFilerReconciliation(
            @PathVariable String tenantId,
            @PathVariable String municipalityId,
            @PathVariable String filerId) {
        
        log.info("Generating filer reconciliation for filer {} in tenant {}", filerId, tenantId);
        ReconciliationResponse report = reconciliationService.generateFilerReconciliation(
                tenantId, filerId, municipalityId);
        return ResponseEntity.ok(report);
    }
}
