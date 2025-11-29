package com.munitax.rules.controller;

import com.munitax.rules.dto.RuleResponse;
import com.munitax.rules.service.RuleFacadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for querying active tax rules with temporal logic.
 * Handles time-based rule retrieval for tax calculations.
 */
@RestController
@RequestMapping("/api/rules")
@CrossOrigin(origins = "*")
public class RuleQueryController {

    @Autowired
    private RuleFacadeService ruleFacadeService;

    /**
     * Get all active rules for a specific tax year and tenant.
     * Returns rules where effectiveDate <= taxYear <= endDate.
     * Critical for tax calculations - must be fast (< 100ms with cache).
     * 
     * @param tenantId Tenant/municipality ID (e.g., "dublin", "columbus")
     * @param taxYear Tax year to retrieve rules for (e.g., 2024)
     * @param entityType Optional entity type filter (e.g., "C_CORP", "S_CORP")
     * @return List of active rules for the specified parameters
     */
    @GetMapping("/active")
    public ResponseEntity<List<RuleResponse>> getActiveRules(
            @RequestParam String tenantId,
            @RequestParam int taxYear,
            @RequestParam(required = false) String entityType) {
        
        LocalDate queryDate = LocalDate.of(taxYear, 12, 31); // End of tax year
        List<RuleResponse> rules = ruleFacadeService.getActiveRules(tenantId, queryDate, entityType);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get a specific rule as of a point in time (historical query).
     * Used for audit trail and compliance reviews.
     * Example: "What was the municipal rate on 2022-06-15?"
     * 
     * @param ruleCode Rule code to query (e.g., "MUNICIPAL_RATE")
     * @param tenantId Tenant ID
     * @param asOfDate Date to query rule state
     * @return Rule that was active on the specified date, if any
     */
    @GetMapping("/as-of")
    public ResponseEntity<RuleResponse> getRuleAsOf(
            @RequestParam String ruleCode,
            @RequestParam String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        RuleResponse rule = ruleFacadeService.getRuleAsOf(ruleCode, tenantId, asOfDate);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rule);
    }

    /**
     * Get all rules that will be active in the future (pending activation).
     * Used for planning and preview of upcoming rule changes.
     * 
     * @param tenantId Tenant ID
     * @param fromDate Start date for future rules query
     * @return List of rules with future effective dates
     */
    @GetMapping("/future")
    public ResponseEntity<List<RuleResponse>> getFutureRules(
            @RequestParam String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        
        List<RuleResponse> rules = ruleFacadeService.getFutureRules(tenantId, fromDate);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get all historical versions of a specific rule.
     * Returns rules ordered by effective date descending (newest first).
     * 
     * @param ruleCode Rule code to get history for
     * @param tenantId Tenant ID
     * @return List of all versions of the rule
     */
    @GetMapping("/history/{ruleCode}")
    public ResponseEntity<List<RuleResponse>> getRuleHistory(
            @PathVariable String ruleCode,
            @RequestParam String tenantId) {
        
        List<RuleResponse> history = ruleFacadeService.getRuleHistory(ruleCode, tenantId);
        return ResponseEntity.ok(history);
    }

    /**
     * Validate if a new rule would overlap with existing rules.
     * Used by UI to prevent invalid rule configurations.
     * 
     * @param ruleCode Rule code to validate
     * @param tenantId Tenant ID
     * @param effectiveDate Proposed effective date
     * @param endDate Proposed end date (optional)
     * @return True if overlap exists, false otherwise
     */
    @GetMapping("/validate-overlap")
    public ResponseEntity<Boolean> validateOverlap(
            @RequestParam String ruleCode,
            @RequestParam String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        boolean hasOverlap = ruleFacadeService.hasOverlap(ruleCode, tenantId, effectiveDate, endDate);
        return ResponseEntity.ok(hasOverlap);
    }
}
