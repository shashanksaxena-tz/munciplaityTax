package com.munitax.rules.service;

import com.munitax.rules.model.RuleCategory;
import com.munitax.rules.model.TaxRule;
import com.munitax.rules.repository.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for temporal rule queries.
 * Handles date-range queries and point-in-time rule retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemporalRuleService {
    
    private final TaxRuleRepository ruleRepository;
    
    /**
     * Get all active rules for a tenant on a specific date.
     * This is the primary method used by tax calculators.
     * 
     * @param tenantId Tenant identifier
     * @param asOfDate Date to check (typically tax year or today)
     * @return List of active rules
     */
    public List<TaxRule> getActiveRules(String tenantId, LocalDate asOfDate) {
        Assert.hasText(tenantId, "Tenant ID cannot be blank");
        Assert.notNull(asOfDate, "As-of date cannot be null");
        
        log.debug("Getting active rules for tenant: {} as of: {}", tenantId, asOfDate);
        return ruleRepository.findActiveRules(tenantId, asOfDate);
    }
    
    /**
     * Get active rules filtered by category.
     * 
     * @param tenantId Tenant identifier
     * @param category Rule category
     * @param asOfDate Date to check
     * @return List of active rules in category
     */
    public List<TaxRule> getActiveRulesByCategory(String tenantId, RuleCategory category, 
                                                   LocalDate asOfDate) {
        Assert.hasText(tenantId, "Tenant ID cannot be blank");
        Assert.notNull(category, "Category cannot be null");
        Assert.notNull(asOfDate, "As-of date cannot be null");
        
        log.debug("Getting active rules for tenant: {} category: {} as of: {}", 
                  tenantId, category, asOfDate);
        return ruleRepository.findActiveRulesByCategory(tenantId, category, asOfDate);
    }
    
    /**
     * Get a specific rule by code (single match expected).
     * 
     * @param ruleCode Rule code
     * @param tenantId Tenant identifier
     * @param asOfDate Date to check
     * @return Optional containing rule if found
     */
    public Optional<TaxRule> getActiveRuleByCode(String ruleCode, String tenantId, 
                                                  LocalDate asOfDate) {
        Assert.hasText(ruleCode, "Rule code cannot be blank");
        Assert.hasText(tenantId, "Tenant ID cannot be blank");
        Assert.notNull(asOfDate, "As-of date cannot be null");
        
        log.debug("Getting active rule by code: {} for tenant: {} as of: {}", 
                  ruleCode, tenantId, asOfDate);
        return ruleRepository.findActiveRuleByCode(ruleCode, tenantId, asOfDate);
    }
    
    /**
     * Check if a rule is active on a specific date.
     * 
     * @param rule Rule to check
     * @param asOfDate Date to check
     * @return true if rule is active
     */
    public boolean isRuleActiveOn(TaxRule rule, LocalDate asOfDate) {
        return rule.isActiveOn(asOfDate);
    }
    
    /**
     * Get rules for a tax year (converts year to date range).
     * For tax year 2024, checks rules active on January 1, 2024.
     * 
     * @param tenantId Tenant identifier
     * @param taxYear Tax year (e.g., 2024)
     * @return List of active rules for tax year
     */
    public List<TaxRule> getActiveRulesForTaxYear(String tenantId, int taxYear) {
        LocalDate taxYearStart = LocalDate.of(taxYear, 1, 1);
        log.debug("Getting active rules for tenant: {} tax year: {}", tenantId, taxYear);
        return getActiveRules(tenantId, taxYearStart);
    }
    
    /**
     * Get rules active within a date range.
     * Returns rules that are active at any point during the range.
     * 
     * @param tenantId Tenant identifier
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of rules active during range
     */
    public List<TaxRule> getActiveRulesInDateRange(String tenantId, LocalDate startDate, 
                                                    LocalDate endDate) {
        log.debug("Getting active rules for tenant: {} from: {} to: {}", 
                  tenantId, startDate, endDate);
        
        // Use repository query to filter at database level for better performance
        // This is more efficient than loading all rules into memory
        List<TaxRule> rulesStartingInRange = ruleRepository.findActiveRules(tenantId, startDate);
        List<TaxRule> rulesEndingInRange = ruleRepository.findActiveRules(tenantId, endDate);
        
        // Combine and deduplicate results
        return rulesStartingInRange.stream()
            .filter(rule -> rule.overlaps(startDate, endDate))
            .distinct()
            .toList();
    }
}
