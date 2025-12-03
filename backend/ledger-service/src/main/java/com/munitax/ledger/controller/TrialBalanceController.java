package com.munitax.ledger.controller;

import com.munitax.ledger.dto.TrialBalanceResponse;
import com.munitax.ledger.service.TrialBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for trial balance operations
 * T050: Create TrialBalanceController with GET /api/v1/trial-balance endpoint
 */
@RestController
@RequestMapping("/api/v1/trial-balance")
@RequiredArgsConstructor
@Slf4j
public class TrialBalanceController {
    
    private final TrialBalanceService trialBalanceService;
    
    /**
     * Generate trial balance as of a specific date
     * GET /api/v1/trial-balance?tenantId={tenantId}&asOfDate={date}
     * 
     * @param tenantId The tenant ID
     * @param asOfDate The date to generate trial balance as of (optional, defaults to current date)
     * @return TrialBalanceResponse
     */
    @GetMapping
    public ResponseEntity<TrialBalanceResponse> getTrialBalance(
            @RequestParam String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        log.info("Trial balance requested for tenant {} as of {}", tenantId, asOfDate);
        TrialBalanceResponse response = trialBalanceService.generateTrialBalance(tenantId, asOfDate);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generate trial balance for a specific period (month-end, quarter-end, year-end)
     * GET /api/v1/trial-balance/period?tenantId={tenantId}&year={year}&period={period}
     * 
     * @param tenantId The tenant ID
     * @param year The year
     * @param period The period: Q1, Q2, Q3, Q4, M1-M12, or YEAR
     * @return TrialBalanceResponse
     */
    @GetMapping("/period")
    public ResponseEntity<TrialBalanceResponse> getTrialBalanceForPeriod(
            @RequestParam String tenantId,
            @RequestParam int year,
            @RequestParam String period) {
        
        log.info("Trial balance requested for tenant {} for period {} {}", tenantId, period, year);
        TrialBalanceResponse response = trialBalanceService.generateTrialBalanceForPeriod(tenantId, year, period);
        return ResponseEntity.ok(response);
    }
}
