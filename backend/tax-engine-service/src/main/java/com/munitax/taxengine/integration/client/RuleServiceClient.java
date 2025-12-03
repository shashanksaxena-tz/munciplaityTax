package com.munitax.taxengine.integration.client;

import com.munitax.taxengine.integration.dto.RuleResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for rule-service integration.
 * Fetches active tax rules dynamically instead of using hardcoded values.
 */
@FeignClient(name = "rule-service", path = "/api/rules")
public interface RuleServiceClient {
    
    /**
     * Get active rules for a specific tenant and tax year.
     * 
     * @param tenantId Tenant identifier (e.g., "dublin")
     * @param taxYear Tax year (e.g., 2025)
     * @param entityType Optional entity type filter
     * @return List of active rules
     */
    @GetMapping("/active")
    List<RuleResponse> getActiveRules(
        @RequestParam("tenantId") String tenantId,
        @RequestParam("taxYear") int taxYear,
        @RequestParam(value = "entityType", required = false) String entityType
    );
}
