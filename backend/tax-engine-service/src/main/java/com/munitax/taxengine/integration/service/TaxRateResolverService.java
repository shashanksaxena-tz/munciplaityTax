package com.munitax.taxengine.integration.service;

import com.munitax.taxengine.integration.client.RuleServiceClient;
import com.munitax.taxengine.integration.dto.RuleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service to resolve tax rates dynamically from rule-service.
 * Caches rules to minimize API calls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxRateResolverService {
    
    private final RuleServiceClient ruleServiceClient;
    
    /**
     * Get municipal tax rate for a tenant and tax year.
     * Falls back to default if rule not found.
     * 
     * @param tenantId Tenant identifier
     * @param taxYear Tax year
     * @param defaultRate Fallback rate if rule not found
     * @return Tax rate as decimal (e.g., 0.02 for 2%)
     */
    @Cacheable(value = "taxRates", key = "#tenantId + '-' + #taxYear + '-MUNICIPAL_TAX_RATE'")
    public BigDecimal getMunicipalTaxRate(String tenantId, int taxYear, BigDecimal defaultRate) {
        try {
            log.debug("Fetching municipal tax rate for tenant: {}, taxYear: {}", tenantId, taxYear);
            
            List<RuleResponse> rules = ruleServiceClient.getActiveRules(tenantId, taxYear, null);
            
            Optional<RuleResponse> rateRule = rules.stream()
                .filter(r -> "MUNICIPAL_TAX_RATE".equals(r.getRuleCode()) 
                          || "NEW_LAGAAN".equals(r.getRuleCode())) // Support custom rules
                .findFirst();
            
            if (rateRule.isPresent() && rateRule.get().getValue() != null) {
                BigDecimal scalar = rateRule.get().getValue().getScalar();
                if (scalar != null) {
                    // Convert percentage to decimal (e.g., 2% -> 0.02, 60000% -> 600.00)
                    BigDecimal rate = scalar.divide(BigDecimal.valueOf(100));
                    log.info("Using dynamic tax rate {} ({}) for tenant: {}, taxYear: {}", 
                        rate, rateRule.get().getRuleCode(), tenantId, taxYear);
                    return rate;
                }
            }
            
            log.warn("No municipal tax rate rule found for tenant: {}, taxYear: {}. Using default: {}", 
                tenantId, taxYear, defaultRate);
            return defaultRate;
            
        } catch (Exception e) {
            log.error("Error fetching tax rate from rule-service for tenant: {}, taxYear: {}. Using default: {}", 
                tenantId, taxYear, defaultRate, e);
            return defaultRate;
        }
    }
    
    /**
     * Get business municipal tax rate.
     * 
     * @param tenantId Tenant identifier
     * @param taxYear Tax year
     * @param defaultRate Fallback rate
     * @return Tax rate as decimal
     */
    @Cacheable(value = "taxRates", key = "#tenantId + '-' + #taxYear + '-BUSINESS_MUNICIPAL_TAX_RATE'")
    public BigDecimal getBusinessTaxRate(String tenantId, int taxYear, BigDecimal defaultRate) {
        try {
            log.debug("Fetching business tax rate for tenant: {}, taxYear: {}", tenantId, taxYear);
            
            List<RuleResponse> rules = ruleServiceClient.getActiveRules(tenantId, taxYear, "BUSINESS");
            
            Optional<RuleResponse> rateRule = rules.stream()
                .filter(r -> "BUSINESS_MUNICIPAL_TAX_RATE".equals(r.getRuleCode()))
                .findFirst();
            
            if (rateRule.isPresent() && rateRule.get().getValue() != null) {
                BigDecimal scalar = rateRule.get().getValue().getScalar();
                if (scalar != null) {
                    BigDecimal rate = scalar.divide(BigDecimal.valueOf(100));
                    log.info("Using dynamic business tax rate {} for tenant: {}, taxYear: {}", 
                        rate, tenantId, taxYear);
                    return rate;
                }
            }
            
            log.warn("No business tax rate rule found. Using default: {}", defaultRate);
            return defaultRate;
            
        } catch (Exception e) {
            log.error("Error fetching business tax rate. Using default: {}", defaultRate, e);
            return defaultRate;
        }
    }
}
