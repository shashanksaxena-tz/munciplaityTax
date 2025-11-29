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
            @PathVariable UUID tenantId,
            @PathVariable UUID municipalityId) {
        
        log.info("Generating reconciliation report for tenant {}", tenantId);
        ReconciliationResponse report = reconciliationService.generateReconciliationReport(
                tenantId, municipalityId);
        return ResponseEntity.ok(report);
    }
}
