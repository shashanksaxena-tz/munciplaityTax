package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.ApportionmentFormula;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for retrieving apportionment formula configurations from rule-engine-service.
 * Integrates with the rule engine to determine applicable formulas and weights for each municipality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormulaConfigService {

    private final RestTemplate restTemplate;

    @Value("${rule-engine-service.url:http://localhost:8081}")
    private String ruleEngineServiceUrl;

    /**
     * Get the apportionment formula for a specific municipality and tax year.
     * Default to FOUR_FACTOR_DOUBLE_WEIGHTED_SALES if rule engine is unavailable.
     *
     * @param municipalityId the municipality ID
     * @param taxYear        the tax year
     * @param tenantId       the tenant ID for multi-tenant isolation
     * @return the applicable apportionment formula
     */
    public ApportionmentFormula getApportionmentFormula(UUID municipalityId, Integer taxYear, UUID tenantId) {
        log.debug("Fetching apportionment formula for municipality: {}, tax year: {}, tenant: {}",
                municipalityId, taxYear, tenantId);

        try {
            String url = String.format("%s/api/rules/apportionment-formula?municipalityId=%s&taxYear=%d&tenantId=%s",
                    ruleEngineServiceUrl, municipalityId, taxYear, tenantId);

            // Call rule engine service
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("formula")) {
                String formulaName = (String) response.get("formula");
                return ApportionmentFormula.valueOf(formulaName);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch apportionment formula from rule engine: {}. Using default.", e.getMessage());
        }

        // Default to four-factor double-weighted sales (most common for Ohio municipalities)
        return ApportionmentFormula.FOUR_FACTOR_DOUBLE_WEIGHTED_SALES;
    }

    /**
     * Get the factor weights for a specific apportionment formula.
     * Returns property, payroll, and sales factor weights that sum to 1.0.
     *
     * @param formula the apportionment formula
     * @return map of factor names to their weights
     */
    public Map<String, BigDecimal> getFormulaWeights(ApportionmentFormula formula) {
        log.debug("Getting weights for formula: {}", formula);

        Map<String, BigDecimal> weights = new HashMap<>();

        switch (formula) {
            case THREE_FACTOR_EQUAL_WEIGHTED:
                // Property: 33.33%, Payroll: 33.33%, Sales: 33.33%
                weights.put("property", new BigDecimal("0.3333"));
                weights.put("payroll", new BigDecimal("0.3333"));
                weights.put("sales", new BigDecimal("0.3334"));
                break;

            case FOUR_FACTOR_DOUBLE_WEIGHTED_SALES:
                // Property: 25%, Payroll: 25%, Sales: 50% (double-weighted)
                weights.put("property", new BigDecimal("0.25"));
                weights.put("payroll", new BigDecimal("0.25"));
                weights.put("sales", new BigDecimal("0.50"));
                break;

            case SINGLE_SALES_FACTOR:
                // Property: 0%, Payroll: 0%, Sales: 100%
                weights.put("property", BigDecimal.ZERO);
                weights.put("payroll", BigDecimal.ZERO);
                weights.put("sales", BigDecimal.ONE);
                break;

            default:
                log.warn("Unknown formula: {}. Using four-factor double-weighted sales as default.", formula);
                weights.put("property", new BigDecimal("0.25"));
                weights.put("payroll", new BigDecimal("0.25"));
                weights.put("sales", new BigDecimal("0.50"));
                break;
        }

        log.debug("Formula weights: {}", weights);
        return weights;
    }

    /**
     * Check if a municipality allows single-sales-factor election.
     * Queries rule engine for municipality-specific rules.
     *
     * @param municipalityId the municipality ID
     * @param taxYear        the tax year
     * @param tenantId       the tenant ID for multi-tenant isolation
     * @return true if single-sales-factor is allowed
     */
    public boolean allowsSingleSalesFactor(UUID municipalityId, Integer taxYear, UUID tenantId) {
        log.debug("Checking if single-sales-factor is allowed for municipality: {}, tax year: {}",
                municipalityId, taxYear);

        try {
            String url = String.format("%s/api/rules/allows-single-sales-factor?municipalityId=%s&taxYear=%d&tenantId=%s",
                    ruleEngineServiceUrl, municipalityId, taxYear, tenantId);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("allowed")) {
                return (Boolean) response.get("allowed");
            }
        } catch (Exception e) {
            log.warn("Failed to check single-sales-factor eligibility from rule engine: {}. Defaulting to false.",
                    e.getMessage());
        }

        // Default to not allowing single-sales-factor
        return false;
    }

    /**
     * Get formula description for display purposes.
     *
     * @param formula the apportionment formula
     * @return human-readable formula description
     */
    public String getFormulaDescription(ApportionmentFormula formula) {
        switch (formula) {
            case THREE_FACTOR_EQUAL_WEIGHTED:
                return "Three-Factor Equal Weighted (Property: 33.33%, Payroll: 33.33%, Sales: 33.33%)";
            case FOUR_FACTOR_DOUBLE_WEIGHTED_SALES:
                return "Four-Factor Double-Weighted Sales (Property: 25%, Payroll: 25%, Sales: 50%)";
            case SINGLE_SALES_FACTOR:
                return "Single Sales Factor (Sales: 100%)";
            default:
                return "Unknown Formula";
        }
    }
}
