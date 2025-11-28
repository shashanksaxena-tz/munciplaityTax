package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.SourcingMethodElection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling sales sourcing method elections (Finnigan vs Joyce).
 * Determines how to calculate sales factor denominator for multi-state businesses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SourcingService {

    private final NexusService nexusService;

    /**
     * Calculate sales factor denominator based on sourcing method election.
     * Finnigan: Include ALL affiliated group sales regardless of nexus.
     * Joyce: Include ONLY sales from entities with nexus in the state.
     *
     * @param affiliatedSales      map of entity ID to total sales
     * @param sourcingMethodElection the sourcing method election
     * @param tenantId             the tenant ID for multi-tenant isolation
     * @return total sales denominator
     */
    public BigDecimal calculateSalesDenominator(Map<String, BigDecimal> affiliatedSales,
                                                SourcingMethodElection sourcingMethodElection,
                                                UUID tenantId) {
        if (sourcingMethodElection == null) {
            throw new IllegalArgumentException("Sourcing method election is required");
        }

        log.debug("Calculating sales denominator with {} method for {} entities",
                sourcingMethodElection, affiliatedSales.size());

        if (affiliatedSales == null || affiliatedSales.isEmpty()) {
            log.warn("Empty affiliated sales map provided");
            return BigDecimal.ZERO;
        }

        switch (sourcingMethodElection) {
            case FINNIGAN:
                // Include ALL affiliated group sales
                return calculateFinniganDenominator(affiliatedSales);

            case JOYCE:
                // This method requires nexus information
                throw new IllegalArgumentException(
                        "Joyce method requires nexus status. Use overloaded method with nexus status map.");

            default:
                throw new IllegalArgumentException("Unknown sourcing method: " + sourcingMethodElection);
        }
    }

    /**
     * Calculate sales factor denominator with nexus status (for Joyce method).
     *
     * @param affiliatedSales      map of entity ID to total sales
     * @param nexusStatus          map of entity ID to nexus status (true = has nexus)
     * @param sourcingMethodElection the sourcing method election
     * @param tenantId             the tenant ID for multi-tenant isolation
     * @return total sales denominator
     */
    public BigDecimal calculateSalesDenominator(Map<String, BigDecimal> affiliatedSales,
                                                Map<String, Boolean> nexusStatus,
                                                SourcingMethodElection sourcingMethodElection,
                                                UUID tenantId) {
        if (sourcingMethodElection == null) {
            throw new IllegalArgumentException("Sourcing method election is required");
        }

        log.debug("Calculating sales denominator with {} method for {} entities",
                sourcingMethodElection, affiliatedSales.size());

        if (affiliatedSales == null || affiliatedSales.isEmpty()) {
            log.warn("Empty affiliated sales map provided");
            return BigDecimal.ZERO;
        }

        switch (sourcingMethodElection) {
            case FINNIGAN:
                // Include ALL affiliated group sales (nexus status irrelevant)
                return calculateFinniganDenominator(affiliatedSales);

            case JOYCE:
                // Include ONLY sales from entities with nexus
                return calculateJoyceDenominator(affiliatedSales, nexusStatus);

            default:
                throw new IllegalArgumentException("Unknown sourcing method: " + sourcingMethodElection);
        }
    }

    /**
     * Finnigan method: Sum all affiliated group sales.
     *
     * @param affiliatedSales map of entity ID to total sales
     * @return total sales denominator
     */
    private BigDecimal calculateFinniganDenominator(Map<String, BigDecimal> affiliatedSales) {
        BigDecimal denominator = affiliatedSales.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Finnigan denominator calculated: {}", denominator);
        return denominator;
    }

    /**
     * Joyce method: Sum only sales from entities with nexus.
     *
     * @param affiliatedSales map of entity ID to total sales
     * @param nexusStatus     map of entity ID to nexus status
     * @return total sales denominator
     */
    private BigDecimal calculateJoyceDenominator(Map<String, BigDecimal> affiliatedSales,
                                                  Map<String, Boolean> nexusStatus) {
        BigDecimal denominator = affiliatedSales.entrySet().stream()
                .filter(entry -> nexusStatus.getOrDefault(entry.getKey(), false))
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Joyce denominator calculated: {} (included only nexus entities)", denominator);
        return denominator;
    }

    /**
     * Compare Finnigan vs Joyce apportionment to recommend the method
     * that results in lower tax liability (lower apportionment percentage).
     *
     * @param ohioSales       total Ohio sales (numerator)
     * @param affiliatedSales map of entity ID to total sales
     * @param nexusStatus     map of entity ID to nexus status
     * @param tenantId        the tenant ID for multi-tenant isolation
     * @return map with comparison results and recommendation
     */
    public Map<String, Object> compareSourcingMethods(BigDecimal ohioSales,
                                                      Map<String, BigDecimal> affiliatedSales,
                                                      Map<String, Boolean> nexusStatus,
                                                      UUID tenantId) {
        BigDecimal finniganDenominator = calculateFinniganDenominator(affiliatedSales);
        BigDecimal joyceDenominator = calculateJoyceDenominator(affiliatedSales, nexusStatus);

        BigDecimal finniganApportionment = calculateApportionmentPercentage(ohioSales, finniganDenominator);
        BigDecimal joyceApportionment = calculateApportionmentPercentage(ohioSales, joyceDenominator);

        SourcingMethodElection recommendation = finniganApportionment.compareTo(joyceApportionment) < 0
                ? SourcingMethodElection.FINNIGAN
                : SourcingMethodElection.JOYCE;

        log.info("Sourcing method comparison: Finnigan={}%, Joyce={}%, Recommendation={}",
                finniganApportionment, joyceApportionment, recommendation);

        return Map.of(
                "finniganDenominator", finniganDenominator,
                "joyceDenominator", joyceDenominator,
                "finniganApportionment", finniganApportionment,
                "joyceApportionment", joyceApportionment,
                "recommendation", recommendation,
                "difference", joyceApportionment.subtract(finniganApportionment).abs()
        );
    }

    /**
     * Calculate apportionment percentage from numerator and denominator.
     *
     * @param numerator   Ohio sales
     * @param denominator total sales
     * @return apportionment percentage (0-100)
     */
    private BigDecimal calculateApportionmentPercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return numerator.divide(denominator, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
