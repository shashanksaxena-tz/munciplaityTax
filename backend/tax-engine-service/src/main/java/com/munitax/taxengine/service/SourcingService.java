package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.ServiceSourcingMethod;
import com.munitax.taxengine.domain.apportionment.SourcingMethodElection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
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

    // ========================================
    // User Story 3: Market-Based Service Sourcing
    // ========================================

    /**
     * T093: Source service revenue based on sourcing method election.
     * Market-Based: Source to customer location (where benefit received).
     * Cost-of-Performance: Prorate by employee location (where work performed).
     *
     * @param serviceRevenue        service revenue amount
     * @param customerState         customer location (required for market-based)
     * @param sourcingMethod        service sourcing method election
     * @param employeeLocations     map of state to employee percentage (required for cost-of-performance)
     * @param tenantId              tenant ID for multi-tenant isolation
     * @return map of state to sourced revenue amount
     */
    public Map<String, BigDecimal> sourceServiceRevenue(BigDecimal serviceRevenue,
                                                        String customerState,
                                                        ServiceSourcingMethod sourcingMethod,
                                                        Map<String, BigDecimal> employeeLocations,
                                                        UUID tenantId) {
        if (serviceRevenue == null || serviceRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid service revenue amount: {}", serviceRevenue);
            return new HashMap<>();
        }

        if (sourcingMethod == null) {
            throw new IllegalArgumentException("Service sourcing method is required");
        }

        log.debug("Sourcing ${} service revenue using {} method", serviceRevenue, sourcingMethod);

        switch (sourcingMethod) {
            case MARKET_BASED:
                return sourceMarketBased(serviceRevenue, customerState);

            case COST_OF_PERFORMANCE:
                return sourceCostOfPerformance(serviceRevenue, employeeLocations);

            default:
                throw new IllegalArgumentException("Unknown service sourcing method: " + sourcingMethod);
        }
    }

    /**
     * T093: Market-based sourcing - source to customer location.
     */
    private Map<String, BigDecimal> sourceMarketBased(BigDecimal serviceRevenue, String customerState) {
        if (customerState == null || customerState.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Customer location is required for market-based sourcing. " +
                    "Use sourceServiceRevenueWithFallback for automatic fallback.");
        }

        Map<String, BigDecimal> result = new HashMap<>();
        result.put(customerState, serviceRevenue);

        log.info("Market-based sourcing: ${} to {} (customer location)", serviceRevenue, customerState);
        return result;
    }

    /**
     * T094: Cost-of-performance sourcing - prorate by employee locations.
     */
    private Map<String, BigDecimal> sourceCostOfPerformance(BigDecimal serviceRevenue,
                                                             Map<String, BigDecimal> employeeLocations) {
        if (employeeLocations == null || employeeLocations.isEmpty()) {
            throw new IllegalArgumentException(
                    "Employee locations are required for cost-of-performance sourcing");
        }

        Map<String, BigDecimal> result = new HashMap<>();
        
        // Validate percentages sum to approximately 1.0
        BigDecimal totalPercentage = employeeLocations.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalPercentage.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.01")) > 0) {
            log.warn("Employee location percentages sum to {} (expected 1.0)", totalPercentage);
        }

        // Prorate service revenue by employee locations
        for (Map.Entry<String, BigDecimal> entry : employeeLocations.entrySet()) {
            String state = entry.getKey();
            BigDecimal percentage = entry.getValue();
            BigDecimal stateRevenue = serviceRevenue.multiply(percentage)
                    .setScale(0, RoundingMode.HALF_UP);
            result.put(state, stateRevenue);
        }

        log.info("Cost-of-performance sourcing: ${} prorated across {} states", serviceRevenue, result.size());
        return result;
    }

    /**
     * T093: Source service revenue to multi-location customer.
     * Handles Fortune 500 customers with operations in multiple states.
     *
     * @param serviceRevenue      service revenue amount
     * @param customerLocations   map of state to customer presence percentage
     * @param sourcingMethod      service sourcing method
     * @param tenantId            tenant ID for multi-tenant isolation
     * @return map of state to sourced revenue amount
     */
    public Map<String, BigDecimal> sourceServiceRevenueMultiLocation(BigDecimal serviceRevenue,
                                                                      Map<String, BigDecimal> customerLocations,
                                                                      ServiceSourcingMethod sourcingMethod,
                                                                      UUID tenantId) {
        if (serviceRevenue == null || serviceRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return new HashMap<>();
        }

        if (customerLocations == null || customerLocations.isEmpty()) {
            throw new IllegalArgumentException("Customer locations are required for multi-location sourcing");
        }

        Map<String, BigDecimal> result = new HashMap<>();

        // Prorate service revenue by customer locations
        for (Map.Entry<String, BigDecimal> entry : customerLocations.entrySet()) {
            String state = entry.getKey();
            BigDecimal percentage = entry.getValue();
            BigDecimal stateRevenue = serviceRevenue.multiply(percentage)
                    .setScale(0, RoundingMode.HALF_UP);
            result.put(state, stateRevenue);
        }

        log.info("Multi-location market-based sourcing: ${} across {} states", serviceRevenue, result.size());
        return result;
    }

    /**
     * T095: Cascading sourcing rules with fallback.
     * Try market-based (customer location) → fallback to cost-of-performance (employee location).
     *
     * @param serviceRevenue      service revenue amount
     * @param customerState       customer location (may be null)
     * @param employeeLocations   map of state to employee percentage (fallback data)
     * @param sourcingMethod      primary sourcing method
     * @param tenantId            tenant ID for multi-tenant isolation
     * @return map of state to sourced revenue amount
     */
    public Map<String, BigDecimal> sourceServiceRevenueWithFallback(BigDecimal serviceRevenue,
                                                                     String customerState,
                                                                     Map<String, BigDecimal> employeeLocations,
                                                                     ServiceSourcingMethod sourcingMethod,
                                                                     UUID tenantId) {
        log.debug("Sourcing ${} with cascading fallback rules", serviceRevenue);

        // Try primary method (market-based)
        if (sourcingMethod == ServiceSourcingMethod.MARKET_BASED) {
            if (customerState != null && !customerState.trim().isEmpty()) {
                return sourceMarketBased(serviceRevenue, customerState);
            }

            log.info("Customer location unknown, falling back to cost-of-performance");
            
            // Fallback to cost-of-performance
            if (employeeLocations != null && !employeeLocations.isEmpty()) {
                return sourceCostOfPerformance(serviceRevenue, employeeLocations);
            }

            throw new IllegalArgumentException(
                    "Cannot source service revenue: customer location unknown and no employee location data available");
        }

        // For cost-of-performance, no fallback needed
        return sourceCostOfPerformance(serviceRevenue, employeeLocations);
    }

    /**
     * T094: Source service revenue by payroll percentages.
     * Prorate service revenue by payroll amounts in each state.
     *
     * @param serviceRevenue service revenue amount
     * @param payrollByState map of state to payroll amount
     * @param tenantId       tenant ID for multi-tenant isolation
     * @return map of state to sourced revenue amount
     */
    public Map<String, BigDecimal> sourceServiceRevenueByPayroll(BigDecimal serviceRevenue,
                                                                  Map<String, BigDecimal> payrollByState,
                                                                  UUID tenantId) {
        if (payrollByState == null || payrollByState.isEmpty()) {
            throw new IllegalArgumentException("Payroll data is required");
        }

        // Calculate total payroll
        BigDecimal totalPayroll = payrollByState.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPayroll.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Total payroll is zero");
        }

        Map<String, BigDecimal> result = new HashMap<>();

        // Prorate service revenue by payroll percentages
        for (Map.Entry<String, BigDecimal> entry : payrollByState.entrySet()) {
            String state = entry.getKey();
            BigDecimal statePayroll = entry.getValue();
            BigDecimal percentage = statePayroll.divide(totalPayroll, 4, RoundingMode.HALF_UP);
            BigDecimal stateRevenue = serviceRevenue.multiply(percentage)
                    .setScale(0, RoundingMode.HALF_UP);
            result.put(state, stateRevenue);
        }

        log.info("Sourced ${} by payroll across {} states", serviceRevenue, result.size());
        return result;
    }

    /**
     * T094: Source service revenue by employee count.
     * Prorate service revenue by number of employees in each state.
     *
     * @param serviceRevenue       service revenue amount
     * @param employeeCountsByState map of state to employee count
     * @param tenantId             tenant ID for multi-tenant isolation
     * @return map of state to sourced revenue amount
     */
    public Map<String, BigDecimal> sourceServiceRevenueByEmployeeCount(BigDecimal serviceRevenue,
                                                                        Map<String, Integer> employeeCountsByState,
                                                                        UUID tenantId) {
        if (employeeCountsByState == null || employeeCountsByState.isEmpty()) {
            throw new IllegalArgumentException("Employee count data is required");
        }

        // Calculate total employees
        int totalEmployees = employeeCountsByState.values().stream()
                .reduce(0, Integer::sum);

        if (totalEmployees == 0) {
            throw new IllegalArgumentException("Total employee count is zero");
        }

        Map<String, BigDecimal> result = new HashMap<>();

        // Prorate service revenue by employee count percentages
        for (Map.Entry<String, Integer> entry : employeeCountsByState.entrySet()) {
            String state = entry.getKey();
            Integer stateEmployees = entry.getValue();
            BigDecimal percentage = BigDecimal.valueOf(stateEmployees)
                    .divide(BigDecimal.valueOf(totalEmployees), 4, RoundingMode.HALF_UP);
            BigDecimal stateRevenue = serviceRevenue.multiply(percentage)
                    .setScale(0, RoundingMode.HALF_UP);
            result.put(state, stateRevenue);
        }

        log.info("Sourced ${} by employee count across {} states", serviceRevenue, result.size());
        return result;
    }

    /**
     * T095: Full cascading sourcing rules with pro-rata fallback.
     * Try market-based → cost-of-performance → pro-rata (based on overall apportionment).
     *
     * @param serviceRevenue         service revenue amount
     * @param customerState          customer location (may be null)
     * @param employeeLocations      employee locations (may be null)
     * @param overallApportionment   overall apportionment percentage for filing state
     * @param filingState            state filing the return
     * @param tenantId               tenant ID for multi-tenant isolation
     * @return map of state to sourced revenue amount
     */
    public Map<String, BigDecimal> sourceServiceRevenueWithFullCascade(BigDecimal serviceRevenue,
                                                                        String customerState,
                                                                        Map<String, BigDecimal> employeeLocations,
                                                                        BigDecimal overallApportionment,
                                                                        String filingState,
                                                                        UUID tenantId) {
        log.debug("Sourcing ${} with full cascading rules (including pro-rata fallback)", serviceRevenue);

        // Try market-based
        if (customerState != null && !customerState.trim().isEmpty()) {
            return sourceMarketBased(serviceRevenue, customerState);
        }

        // Try cost-of-performance
        if (employeeLocations != null && !employeeLocations.isEmpty()) {
            log.info("Customer location unknown, using cost-of-performance");
            return sourceCostOfPerformance(serviceRevenue, employeeLocations);
        }

        // Final fallback: pro-rata by overall apportionment
        log.info("No customer or employee location data, using pro-rata apportionment");
        
        Map<String, BigDecimal> result = new HashMap<>();
        BigDecimal filingStateRevenue = serviceRevenue.multiply(overallApportionment)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal everywhereElseRevenue = serviceRevenue.subtract(filingStateRevenue);
        
        result.put(filingState, filingStateRevenue);
        result.put("EVERYWHERE_ELSE", everywhereElseRevenue);

        log.info("Pro-rata sourcing: ${} to {}, ${} to everywhere else", 
                filingStateRevenue, filingState, everywhereElseRevenue);
        
        return result;
    }
}
