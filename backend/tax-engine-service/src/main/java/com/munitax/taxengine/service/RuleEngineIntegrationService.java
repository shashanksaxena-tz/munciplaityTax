package com.munitax.taxengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for integrating with rule-engine-service to retrieve penalty and interest rates.
 * 
 * Functional Requirements:
 * - FR-004: Late filing penalty rate (5% per month)
 * - FR-008: Late payment penalty rate (1% per month)
 * - FR-028: Interest rate retrieved from rule-engine-service (federal short-term rate + 3%)
 * 
 * The rule-engine-service maintains the authoritative source for:
 * - Penalty rates (may vary by jurisdiction or time period)
 * - Interest rates (based on federal short-term rate + 3%)
 * - Safe harbor thresholds
 * - Calculation methods
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineIntegrationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${services.rule-engine.url:http://localhost:8087}")
    private String ruleEngineBaseUrl;
    
    // Default rates as fallback (should match FR specifications)
    private static final BigDecimal DEFAULT_LATE_FILING_RATE = new BigDecimal("0.05");      // 5% per month
    private static final BigDecimal DEFAULT_LATE_PAYMENT_RATE = new BigDecimal("0.01");     // 1% per month
    private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("0.05");         // 5% annual
    private static final BigDecimal DEFAULT_CURRENT_YEAR_SAFE_HARBOR = new BigDecimal("0.90");  // 90%
    private static final BigDecimal DEFAULT_PRIOR_YEAR_SAFE_HARBOR = new BigDecimal("1.00");    // 100%
    private static final BigDecimal DEFAULT_HIGH_AGI_PRIOR_YEAR_SAFE_HARBOR = new BigDecimal("1.10"); // 110%
    private static final BigDecimal DEFAULT_AGI_THRESHOLD = new BigDecimal("150000.00");
    
    /**
     * Retrieve late filing penalty rate for a given date.
     * FR-004: Default is 5% per month, but may vary by jurisdiction or time period.
     * 
     * @param effectiveDate the date for which to retrieve the rate
     * @param tenantId      the tenant (municipality) ID
     * @return monthly penalty rate (e.g., 0.05 for 5%)
     */
    public BigDecimal getLateFilingPenaltyRate(LocalDate effectiveDate, String tenantId) {
        try {
            String url = String.format("%s/api/rules/penalty-rates/late-filing?date=%s&tenantId=%s",
                    ruleEngineBaseUrl, effectiveDate, tenantId);
            
            log.debug("Retrieving late filing penalty rate from: {}", url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("rate")) {
                BigDecimal rate = new BigDecimal(response.get("rate").toString());
                log.info("Retrieved late filing penalty rate: {} for date: {}", rate, effectiveDate);
                return rate;
            }
        } catch (RestClientException e) {
            log.warn("Failed to retrieve late filing penalty rate from rule-engine-service: {}. Using default rate.", 
                    e.getMessage());
        }
        
        log.info("Using default late filing penalty rate: {}", DEFAULT_LATE_FILING_RATE);
        return DEFAULT_LATE_FILING_RATE;
    }
    
    /**
     * Retrieve late payment penalty rate for a given date.
     * FR-008: Default is 1% per month, but may vary by jurisdiction or time period.
     * 
     * @param effectiveDate the date for which to retrieve the rate
     * @param tenantId      the tenant (municipality) ID
     * @return monthly penalty rate (e.g., 0.01 for 1%)
     */
    public BigDecimal getLatePaymentPenaltyRate(LocalDate effectiveDate, String tenantId) {
        try {
            String url = String.format("%s/api/rules/penalty-rates/late-payment?date=%s&tenantId=%s",
                    ruleEngineBaseUrl, effectiveDate, tenantId);
            
            log.debug("Retrieving late payment penalty rate from: {}", url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("rate")) {
                BigDecimal rate = new BigDecimal(response.get("rate").toString());
                log.info("Retrieved late payment penalty rate: {} for date: {}", rate, effectiveDate);
                return rate;
            }
        } catch (RestClientException e) {
            log.warn("Failed to retrieve late payment penalty rate from rule-engine-service: {}. Using default rate.", 
                    e.getMessage());
        }
        
        log.info("Using default late payment penalty rate: {}", DEFAULT_LATE_PAYMENT_RATE);
        return DEFAULT_LATE_PAYMENT_RATE;
    }
    
    /**
     * Retrieve current annual interest rate.
     * FR-028: Federal short-term rate + 3%, typically 3-8%.
     * Rate compounds quarterly per IRS standard.
     * 
     * @param effectiveDate the date for which to retrieve the rate
     * @param tenantId      the tenant (municipality) ID
     * @return annual interest rate (e.g., 0.05 for 5%)
     */
    public BigDecimal getInterestRate(LocalDate effectiveDate, String tenantId) {
        try {
            String url = String.format("%s/api/rules/interest-rates?date=%s&tenantId=%s",
                    ruleEngineBaseUrl, effectiveDate, tenantId);
            
            log.debug("Retrieving interest rate from: {}", url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("annualRate")) {
                BigDecimal rate = new BigDecimal(response.get("annualRate").toString());
                log.info("Retrieved annual interest rate: {} for date: {}", rate, effectiveDate);
                return rate;
            }
        } catch (RestClientException e) {
            log.warn("Failed to retrieve interest rate from rule-engine-service: {}. Using default rate.", 
                    e.getMessage());
        }
        
        log.info("Using default annual interest rate: {}", DEFAULT_INTEREST_RATE);
        return DEFAULT_INTEREST_RATE;
    }
    
    /**
     * Retrieve safe harbor thresholds for estimated tax penalties.
     * FR-016: Safe Harbor 1 - 90% of current year tax
     * FR-017: Safe Harbor 2 - 100% of prior year tax (110% if AGI > $150K)
     * 
     * @param taxYear  the tax year
     * @param tenantId the tenant (municipality) ID
     * @return map with safe harbor thresholds
     */
    public Map<String, BigDecimal> getSafeHarborThresholds(Integer taxYear, String tenantId) {
        Map<String, BigDecimal> thresholds = new HashMap<>();
        
        try {
            String url = String.format("%s/api/rules/safe-harbor-thresholds?taxYear=%d&tenantId=%s",
                    ruleEngineBaseUrl, taxYear, tenantId);
            
            log.debug("Retrieving safe harbor thresholds from: {}", url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                if (response.containsKey("currentYearThreshold")) {
                    thresholds.put("currentYearThreshold", 
                            new BigDecimal(response.get("currentYearThreshold").toString()));
                }
                if (response.containsKey("priorYearThreshold")) {
                    thresholds.put("priorYearThreshold", 
                            new BigDecimal(response.get("priorYearThreshold").toString()));
                }
                if (response.containsKey("highAgiPriorYearThreshold")) {
                    thresholds.put("highAgiPriorYearThreshold", 
                            new BigDecimal(response.get("highAgiPriorYearThreshold").toString()));
                }
                if (response.containsKey("agiThreshold")) {
                    thresholds.put("agiThreshold", 
                            new BigDecimal(response.get("agiThreshold").toString()));
                }
                
                log.info("Retrieved safe harbor thresholds for tax year: {}", taxYear);
                return thresholds;
            }
        } catch (RestClientException e) {
            log.warn("Failed to retrieve safe harbor thresholds from rule-engine-service: {}. Using defaults.", 
                    e.getMessage());
        }
        
        // Return default thresholds
        thresholds.put("currentYearThreshold", DEFAULT_CURRENT_YEAR_SAFE_HARBOR);
        thresholds.put("priorYearThreshold", DEFAULT_PRIOR_YEAR_SAFE_HARBOR);
        thresholds.put("highAgiPriorYearThreshold", DEFAULT_HIGH_AGI_PRIOR_YEAR_SAFE_HARBOR);
        thresholds.put("agiThreshold", DEFAULT_AGI_THRESHOLD);
        
        log.info("Using default safe harbor thresholds");
        return thresholds;
    }
    
    /**
     * Retrieve estimated tax penalty rate (per quarter).
     * Used for calculating quarterly underpayment penalties.
     * 
     * @param effectiveDate the date for which to retrieve the rate
     * @param tenantId      the tenant (municipality) ID
     * @return quarterly penalty rate
     */
    public BigDecimal getEstimatedTaxPenaltyRate(LocalDate effectiveDate, String tenantId) {
        try {
            String url = String.format("%s/api/rules/penalty-rates/estimated-tax?date=%s&tenantId=%s",
                    ruleEngineBaseUrl, effectiveDate, tenantId);
            
            log.debug("Retrieving estimated tax penalty rate from: {}", url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("quarterlyRate")) {
                BigDecimal rate = new BigDecimal(response.get("quarterlyRate").toString());
                log.info("Retrieved estimated tax penalty rate: {} for date: {}", rate, effectiveDate);
                return rate;
            }
        } catch (RestClientException e) {
            log.warn("Failed to retrieve estimated tax penalty rate from rule-engine-service: {}. Using interest rate as fallback.", 
                    e.getMessage());
        }
        
        // Fall back to quarterly interest rate (annual / 4)
        BigDecimal annualRate = getInterestRate(effectiveDate, tenantId);
        BigDecimal quarterlyRate = annualRate.divide(new BigDecimal("4"), 6, java.math.RoundingMode.HALF_UP);
        
        log.info("Using quarterly interest rate as estimated tax penalty rate: {}", quarterlyRate);
        return quarterlyRate;
    }
    
    /**
     * Check if rule-engine-service is available.
     * 
     * @return true if service is reachable
     */
    public boolean isRuleEngineAvailable() {
        try {
            String url = String.format("%s/actuator/health", ruleEngineBaseUrl);
            restTemplate.getForObject(url, Map.class);
            return true;
        } catch (RestClientException e) {
            log.warn("Rule engine service is not available: {}", e.getMessage());
            return false;
        }
    }
}
