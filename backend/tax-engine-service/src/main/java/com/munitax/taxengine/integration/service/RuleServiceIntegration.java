package com.munitax.taxengine.integration.service;

import com.munitax.taxengine.integration.client.RuleServiceClient;
import com.munitax.taxengine.integration.dto.RuleResponse;
import com.munitax.taxengine.model.BusinessTaxRulesConfig;
import com.munitax.taxengine.model.TaxRulesConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to fetch and convert rules from rule-service to TaxRulesConfig objects.
 * Provides caching and fallback mechanisms.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RuleServiceIntegration {
    
    private final RuleServiceClient ruleServiceClient;
    
    @Value("${app.rules.default-tenant-id:dublin}")
    private String defaultTenantId;
    
    @Value("${app.rules.fallback-municipal-rate:0.020}")
    private double fallbackMunicipalRate;
    
    @Value("${app.rules.fallback-credit-limit-rate:0.020}")
    private double fallbackCreditLimitRate;
    
    /**
     * Fetch and convert active rules to TaxRulesConfig for individual tax calculations.
     * 
     * @param tenantId Tenant identifier (e.g., "dublin")
     * @param taxYear Tax year (e.g., 2024)
     * @return TaxRulesConfig with rates from rule service or defaults
     */
    @Cacheable(value = "individualTaxRules", key = "#tenantId + '-' + #taxYear")
    public TaxRulesConfig getIndividualTaxRules(String tenantId, int taxYear) {
        log.debug("Fetching individual tax rules for tenant: {}, year: {}", tenantId, taxYear);
        
        try {
            List<RuleResponse> rules = ruleServiceClient.getActiveRules(tenantId, taxYear, "INDIVIDUAL");
            
            // Extract municipal rate
            double municipalRate = extractRate(rules, "MUNICIPAL_TAX_RATE", fallbackMunicipalRate);
            
            // Extract credit limit rate
            double creditLimitRate = extractRate(rules, "MUNICIPAL_CREDIT_LIMIT_RATE", fallbackCreditLimitRate);
            
            // Extract W2 qualifying wages rule
            TaxRulesConfig.W2QualifyingWagesRule wagesRule = extractW2QualifyingRule(rules);
            
            // Extract income inclusion rules
            TaxRulesConfig.IncomeInclusion incomeInclusion = extractIncomeInclusion(rules);
            
            // Rounding is always enabled
            boolean enableRounding = true;
            
            // Empty municipality rates map for now
            Map<String, Double> municipalRates = new HashMap<>();
            
            log.info("Resolved individual tax rules for {}-{}: municipalRate={}, creditLimit={}, wagesRule={}", 
                    tenantId, taxYear, municipalRate, creditLimitRate, wagesRule);
            
            return new TaxRulesConfig(
                    municipalRate,
                    creditLimitRate,
                    municipalRates,
                    wagesRule,
                    incomeInclusion,
                    enableRounding
            );
            
        } catch (Exception e) {
            log.error("Error fetching rules from rule-service for tenant: {}, year: {}. Using fallback values.", 
                    tenantId, taxYear, e);
            return getFallbackIndividualRules();
        }
    }
    
    /**
     * Fetch and convert active rules to BusinessTaxRulesConfig for business tax calculations.
     * 
     * @param tenantId Tenant identifier
     * @param taxYear Tax year
     * @return BusinessTaxRulesConfig with rates from rule service or defaults
     */
    @Cacheable(value = "businessTaxRules", key = "#tenantId + '-' + #taxYear")
    public BusinessTaxRulesConfig getBusinessTaxRules(String tenantId, int taxYear) {
        log.debug("Fetching business tax rules for tenant: {}, year: {}", tenantId, taxYear);
        
        try {
            List<RuleResponse> rules = ruleServiceClient.getActiveRules(tenantId, taxYear, "BUSINESS");
            
            // Extract business municipal rate
            double municipalRate = extractRate(rules, "BUSINESS_MUNICIPAL_TAX_RATE", fallbackMunicipalRate);
            
            // Extract minimum tax
            double minimumTax = extractScalar(rules, "MINIMUM_TAX", 50.0);
            
            // Allocation method defaults
            String allocationMethod = "3_FACTOR";
            double salesFactorWeight = 2.0;
            
            // NOL settings
            boolean enableNOL = extractBoolean(rules, "ENABLE_NOL", true);
            double nolOffsetCapPercent = extractScalar(rules, "NOL_OFFSET_CAP_PERCENT", 1.0);
            
            // Intangible expense rate
            double intangibleExpenseRate = 0.0; // Default
            
            // Safe harbor and penalties
            double safeHarborPercent = extractScalar(rules, "SAFE_HARBOR_PERCENT", 0.90);
            double penaltyLateFiling = extractScalar(rules, "PENALTY_RATE_LATE_FILING", 25.0);
            double penaltyUnderpayment = extractScalar(rules, "PENALTY_RATE_UNDERPAYMENT", 0.05);
            double interestRate = extractScalar(rules, "INTEREST_RATE_ANNUAL", 0.05);
            
            log.info("Resolved business tax rules for {}-{}: municipalRate={}, minimumTax={}", 
                    tenantId, taxYear, municipalRate, minimumTax);
            
            return new BusinessTaxRulesConfig(
                    municipalRate,
                    minimumTax,
                    allocationMethod,
                    salesFactorWeight,
                    enableNOL,
                    nolOffsetCapPercent,
                    intangibleExpenseRate,
                    safeHarborPercent,
                    penaltyLateFiling,
                    penaltyUnderpayment,
                    interestRate
            );
            
        } catch (Exception e) {
            log.error("Error fetching business rules from rule-service for tenant: {}, year: {}. Using fallback values.", 
                    tenantId, taxYear, e);
            return getFallbackBusinessRules();
        }
    }
    
    /**
     * Extract a rate value from rules list.
     */
    private double extractRate(List<RuleResponse> rules, String ruleCode, double fallback) {
        Optional<RuleResponse> rule = rules.stream()
                .filter(r -> ruleCode.equals(r.getRuleCode()))
                .findFirst();
        
        if (rule.isPresent() && rule.get().getValue() != null && rule.get().getValue().getScalar() != null) {
            BigDecimal scalar = rule.get().getValue().getScalar();
            // Convert percentage to decimal (2.0 -> 0.02)
            double rate = scalar.divide(BigDecimal.valueOf(100)).doubleValue();
            log.debug("Rule {} = {}", ruleCode, rate);
            return rate;
        }
        
        log.debug("Rule {} not found, using fallback: {}", ruleCode, fallback);
        return fallback;
    }
    
    /**
     * Extract a scalar value from rules list.
     */
    private double extractScalar(List<RuleResponse> rules, String ruleCode, double fallback) {
        Optional<RuleResponse> rule = rules.stream()
                .filter(r -> ruleCode.equals(r.getRuleCode()))
                .findFirst();
        
        if (rule.isPresent() && rule.get().getValue() != null && rule.get().getValue().getScalar() != null) {
            double value = rule.get().getValue().getScalar().doubleValue();
            log.debug("Rule {} = {}", ruleCode, value);
            return value;
        }
        
        log.debug("Rule {} not found, using fallback: {}", ruleCode, fallback);
        return fallback;
    }
    
    /**
     * Extract a boolean value from rules list.
     */
    private boolean extractBoolean(List<RuleResponse> rules, String ruleCode, boolean fallback) {
        Optional<RuleResponse> rule = rules.stream()
                .filter(r -> ruleCode.equals(r.getRuleCode()))
                .findFirst();
        
        if (rule.isPresent() && rule.get().getValue() != null && rule.get().getValue().getFlag() != null) {
            boolean value = rule.get().getValue().getFlag();
            log.debug("Rule {} = {}", ruleCode, value);
            return value;
        }
        
        log.debug("Rule {} not found, using fallback: {}", ruleCode, fallback);
        return fallback;
    }
    
    /**
     * Extract W2 qualifying wages rule from rules list.
     */
    private TaxRulesConfig.W2QualifyingWagesRule extractW2QualifyingRule(List<RuleResponse> rules) {
        Optional<RuleResponse> rule = rules.stream()
                .filter(r -> "W2_QUALIFYING_WAGES_RULE".equals(r.getRuleCode()))
                .findFirst();
        
        if (rule.isPresent() && rule.get().getValue() != null && rule.get().getValue().getOption() != null) {
            String option = rule.get().getValue().getOption();
            try {
                TaxRulesConfig.W2QualifyingWagesRule wagesRule = 
                        TaxRulesConfig.W2QualifyingWagesRule.valueOf(option);
                log.debug("W2 qualifying wages rule: {}", wagesRule);
                return wagesRule;
            } catch (IllegalArgumentException e) {
                log.warn("Invalid W2 qualifying wages rule option: {}", option);
            }
        }
        
        log.debug("W2 qualifying wages rule not found, using default: HIGHEST_OF_ALL");
        return TaxRulesConfig.W2QualifyingWagesRule.HIGHEST_OF_ALL;
    }
    
    /**
     * Extract income inclusion rules from rules list.
     */
    private TaxRulesConfig.IncomeInclusion extractIncomeInclusion(List<RuleResponse> rules) {
        boolean scheduleC = extractBoolean(rules, "INCLUDE_SCHEDULE_C", true);
        boolean scheduleE = extractBoolean(rules, "INCLUDE_SCHEDULE_E", true);
        boolean scheduleF = extractBoolean(rules, "INCLUDE_SCHEDULE_F", true);
        boolean w2g = extractBoolean(rules, "INCLUDE_W2G", true);
        boolean form1099 = extractBoolean(rules, "INCLUDE_1099", true);
        
        return new TaxRulesConfig.IncomeInclusion(scheduleC, scheduleE, scheduleF, w2g, form1099);
    }
    
    /**
     * Get fallback individual tax rules when rule service is unavailable.
     */
    private TaxRulesConfig getFallbackIndividualRules() {
        log.warn("Using fallback individual tax rules");
        return new TaxRulesConfig(
                fallbackMunicipalRate,
                fallbackCreditLimitRate,
                new HashMap<>(),
                TaxRulesConfig.W2QualifyingWagesRule.HIGHEST_OF_ALL,
                new TaxRulesConfig.IncomeInclusion(true, true, true, true, true),
                true
        );
    }
    
    /**
     * Get fallback business tax rules when rule service is unavailable.
     */
    private BusinessTaxRulesConfig getFallbackBusinessRules() {
        log.warn("Using fallback business tax rules");
        return new BusinessTaxRulesConfig(
                fallbackMunicipalRate,
                50.0,
                "3_FACTOR",
                2.0,
                true,
                1.0,
                0.0,
                0.90,
                25.0,
                0.05,
                0.05
        );
    }
}
