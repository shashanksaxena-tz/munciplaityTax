package com.munitax.taxengine.service;

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
 * Service for integrating with payroll withholding data from Spec 1 (W-1 filings).
 * Auto-populates payroll factor data for apportionment calculations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithholdingIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${tax-engine-service.url:http://localhost:8080}")
    private String taxEngineServiceUrl;

    /**
     * Retrieve Ohio payroll data from W-1 withholding filings.
     * Sums all W-1 filings for a business in a given tax year.
     *
     * @param businessId the business ID
     * @param taxYear    the tax year
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return map containing Ohio payroll and employee count
     */
    public Map<String, Object> getOhioPayrollData(UUID businessId, Integer taxYear, UUID tenantId) {
        log.debug("Fetching Ohio payroll data for business: {}, tax year: {}, tenant: {}",
                businessId, taxYear, tenantId);

        Map<String, Object> payrollData = new HashMap<>();

        try {
            String url = String.format("%s/api/withholding/payroll-summary?businessId=%s&taxYear=%d&tenantId=%s",
                    taxEngineServiceUrl, businessId, taxYear, tenantId);

            // Call internal withholding API (Spec 1)
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                // Extract Ohio payroll and employee count from W-1 filings
                BigDecimal ohioPayroll = response.containsKey("totalPayroll")
                        ? new BigDecimal(response.get("totalPayroll").toString())
                        : BigDecimal.ZERO;

                Integer ohioEmployeeCount = response.containsKey("employeeCount")
                        ? ((Number) response.get("employeeCount")).intValue()
                        : 0;

                payrollData.put("ohioPayroll", ohioPayroll);
                payrollData.put("ohioEmployeeCount", ohioEmployeeCount);
                payrollData.put("autoPopulated", true);

                log.info("Successfully retrieved Ohio payroll data: payroll={}, employees={}",
                        ohioPayroll, ohioEmployeeCount);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Ohio payroll data from withholding service: {}. " +
                    "Payroll factor must be manually entered.", e.getMessage());
            payrollData.put("autoPopulated", false);
        }

        return payrollData;
    }

    /**
     * Retrieve total everywhere payroll data for multi-state businesses.
     * This may aggregate data from multiple sources or require manual entry.
     *
     * @param businessId the business ID
     * @param taxYear    the tax year
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return map containing total payroll and employee count
     */
    public Map<String, Object> getTotalPayrollData(UUID businessId, Integer taxYear, UUID tenantId) {
        log.debug("Fetching total (everywhere) payroll data for business: {}, tax year: {}, tenant: {}",
                businessId, taxYear, tenantId);

        Map<String, Object> payrollData = new HashMap<>();

        try {
            String url = String.format("%s/api/withholding/total-payroll?businessId=%s&taxYear=%d&tenantId=%s",
                    taxEngineServiceUrl, businessId, taxYear, tenantId);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                BigDecimal totalPayroll = response.containsKey("totalPayroll")
                        ? new BigDecimal(response.get("totalPayroll").toString())
                        : BigDecimal.ZERO;

                Integer totalEmployeeCount = response.containsKey("employeeCount")
                        ? ((Number) response.get("employeeCount")).intValue()
                        : 0;

                payrollData.put("totalPayroll", totalPayroll);
                payrollData.put("totalEmployeeCount", totalEmployeeCount);
                payrollData.put("autoPopulated", true);

                log.info("Successfully retrieved total payroll data: payroll={}, employees={}",
                        totalPayroll, totalEmployeeCount);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch total payroll data: {}. Total payroll must be manually entered.",
                    e.getMessage());
            payrollData.put("autoPopulated", false);
        }

        return payrollData;
    }

    /**
     * Get payroll allocation by state for cost-of-performance sourcing.
     * Returns payroll percentages by state for service revenue sourcing.
     *
     * @param businessId the business ID
     * @param taxYear    the tax year
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return map of state code to payroll percentage (0-1)
     */
    public Map<String, BigDecimal> getPayrollAllocationByState(UUID businessId, Integer taxYear, UUID tenantId) {
        log.debug("Fetching payroll allocation by state for business: {}, tax year: {}, tenant: {}",
                businessId, taxYear, tenantId);

        Map<String, BigDecimal> allocationByState = new HashMap<>();

        try {
            String url = String.format("%s/api/withholding/payroll-by-state?businessId=%s&taxYear=%d&tenantId=%s",
                    taxEngineServiceUrl, businessId, taxYear, tenantId);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("stateAllocations")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> allocations = (Map<String, Object>) response.get("stateAllocations");

                allocations.forEach((state, percentage) -> {
                    allocationByState.put(state, new BigDecimal(percentage.toString()));
                });

                log.info("Retrieved payroll allocation for {} states", allocationByState.size());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch payroll allocation by state: {}. " +
                    "Cost-of-performance sourcing may require manual allocation.", e.getMessage());
        }

        return allocationByState;
    }

    /**
     * Check if payroll data is available for auto-population.
     *
     * @param businessId the business ID
     * @param taxYear    the tax year
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return true if W-1 filings exist for the business and tax year
     */
    public boolean hasWithholdingData(UUID businessId, Integer taxYear, UUID tenantId) {
        log.debug("Checking if withholding data exists for business: {}, tax year: {}, tenant: {}",
                businessId, taxYear, tenantId);

        try {
            String url = String.format("%s/api/withholding/exists?businessId=%s&taxYear=%d&tenantId=%s",
                    taxEngineServiceUrl, businessId, taxYear, tenantId);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("exists")) {
                return (Boolean) response.get("exists");
            }
        } catch (Exception e) {
            log.warn("Failed to check withholding data existence: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Get Ohio payroll for a specific tax year.
     *
     * @param businessId the business ID
     * @param taxYear    the tax year
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Ohio payroll amount
     */
    public BigDecimal getOhioPayrollForYear(UUID businessId, Integer taxYear, UUID tenantId) {
        log.debug("Fetching Ohio payroll for year: business={}, taxYear={}, tenant={}",
                businessId, taxYear, tenantId);

        Map<String, Object> payrollData = getOhioPayrollData(businessId, taxYear, tenantId);
        Object ohioPayroll = payrollData.get("ohioPayroll");
        
        if (ohioPayroll instanceof BigDecimal) {
            return (BigDecimal) ohioPayroll;
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Get total payroll (everywhere) for a specific tax year.
     *
     * @param businessId the business ID
     * @param taxYear    the tax year
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Total payroll amount
     */
    public BigDecimal getTotalPayrollForYear(UUID businessId, Integer taxYear, UUID tenantId) {
        log.debug("Fetching total payroll for year: business={}, taxYear={}, tenant={}",
                businessId, taxYear, tenantId);

        Map<String, Object> payrollData = getTotalPayrollData(businessId, taxYear, tenantId);
        Object totalPayroll = payrollData.get("totalPayroll");
        
        if (totalPayroll instanceof BigDecimal) {
            return (BigDecimal) totalPayroll;
        }
        
        return BigDecimal.ZERO;
    }
}
