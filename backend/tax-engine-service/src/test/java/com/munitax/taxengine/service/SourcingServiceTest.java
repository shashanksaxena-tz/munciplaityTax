package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.ServiceSourcingMethod;
import com.munitax.taxengine.domain.apportionment.SourcingMethodElection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SourcingService.
 * Tests Finnigan vs Joyce sales factor sourcing calculations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SourcingService Tests")
class SourcingServiceTest {

    @Mock
    private NexusService nexusService;

    @InjectMocks
    private SourcingService sourcingService;

    private UUID businessId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Finnigan: Include all affiliated group sales in denominator")
    void testFinniganMethodIncludesAllGroupSales() {
        // Given: Multi-state corporate group with 3 entities
        // Parent (OH nexus, $5M sales, $1M OH sales)
        // Sub A (OH nexus, $3M sales, $500K OH sales)
        // Sub B (no OH nexus, $2M sales, $0 OH sales)
        
        Map<String, BigDecimal> affiliatedSales = new HashMap<>();
        affiliatedSales.put("parent", new BigDecimal("5000000")); // $5M
        affiliatedSales.put("subA", new BigDecimal("3000000"));   // $3M
        affiliatedSales.put("subB", new BigDecimal("2000000"));   // $2M

        Map<String, BigDecimal> ohioSales = new HashMap<>();
        ohioSales.put("parent", new BigDecimal("1000000")); // $1M
        ohioSales.put("subA", new BigDecimal("500000"));    // $500K
        ohioSales.put("subB", BigDecimal.ZERO);             // $0

        // When: Calculate sales factor with Finnigan election
        BigDecimal denominator = sourcingService.calculateSalesDenominator(
                affiliatedSales, SourcingMethodElection.FINNIGAN, tenantId);

        // Then: Denominator includes ALL group sales (nexus or no nexus)
        // Expected: $5M + $3M + $2M = $10M
        assertEquals(new BigDecimal("10000000"), denominator);
    }

    @Test
    @DisplayName("Joyce: Include only nexus entity sales in denominator")
    void testJoyceMethodIncludesOnlyNexusSales() {
        // Given: Same corporate group as above
        Map<String, BigDecimal> affiliatedSales = new HashMap<>();
        affiliatedSales.put("parent", new BigDecimal("5000000"));
        affiliatedSales.put("subA", new BigDecimal("3000000"));
        affiliatedSales.put("subB", new BigDecimal("2000000"));

        Map<String, Boolean> nexusStatus = new HashMap<>();
        nexusStatus.put("parent", true);  // Has OH nexus
        nexusStatus.put("subA", true);    // Has OH nexus
        nexusStatus.put("subB", false);   // No OH nexus

        // When: Calculate sales factor with Joyce election
        BigDecimal denominator = sourcingService.calculateSalesDenominator(
                affiliatedSales, nexusStatus, SourcingMethodElection.JOYCE, tenantId);

        // Then: Denominator includes ONLY sales from entities with OH nexus
        // Expected: $5M + $3M = $8M (excludes subB's $2M)
        assertEquals(new BigDecimal("8000000"), denominator);
    }

    @Test
    @DisplayName("Finnigan vs Joyce: Calculate apportionment difference")
    void testFinniganVsJoyceApportionmentComparison() {
        // Given: Ohio numerator = $1.5M ($1M + $500K)
        BigDecimal ohioNumerator = new BigDecimal("1500000");

        // Finnigan denominator = $10M (all group sales)
        BigDecimal finniganDenominator = new BigDecimal("10000000");

        // Joyce denominator = $8M (only nexus entity sales)
        BigDecimal joyceDenominator = new BigDecimal("8000000");

        // When: Calculate apportionment percentages
        BigDecimal finniganApportionment = ohioNumerator
                .divide(finniganDenominator, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));

        BigDecimal joyceApportionment = ohioNumerator
                .divide(joyceDenominator, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));

        // Then: Finnigan = 15%, Joyce = 18.75%
        assertEquals(new BigDecimal("15.0000"), finniganApportionment);
        assertEquals(new BigDecimal("18.7500"), joyceApportionment);

        // Joyce results in higher apportionment (less favorable for taxpayer)
        assertTrue(joyceApportionment.compareTo(finniganApportionment) > 0);
    }

    @Test
    @DisplayName("Finnigan: Handle single entity (no affiliated group)")
    void testFinniganMethodWithSingleEntity() {
        // Given: Single entity business (no affiliates)
        Map<String, BigDecimal> sales = new HashMap<>();
        sales.put("singleEntity", new BigDecimal("5000000"));

        // When: Calculate with Finnigan
        BigDecimal denominator = sourcingService.calculateSalesDenominator(
                sales, SourcingMethodElection.FINNIGAN, tenantId);

        // Then: Denominator equals single entity sales
        assertEquals(new BigDecimal("5000000"), denominator);
    }

    @Test
    @DisplayName("Joyce: Handle all entities with nexus")
    void testJoyceMethodWhenAllEntitiesHaveNexus() {
        // Given: All entities have OH nexus
        Map<String, BigDecimal> sales = new HashMap<>();
        sales.put("parent", new BigDecimal("5000000"));
        sales.put("sub", new BigDecimal("3000000"));

        Map<String, Boolean> nexusStatus = new HashMap<>();
        nexusStatus.put("parent", true);
        nexusStatus.put("sub", true);

        // When: Calculate with Joyce
        BigDecimal denominator = sourcingService.calculateSalesDenominator(
                sales, nexusStatus, SourcingMethodElection.JOYCE, tenantId);

        // Then: Denominator includes all sales (same as Finnigan in this case)
        assertEquals(new BigDecimal("8000000"), denominator);
    }

    @Test
    @DisplayName("Validate sourcing method election is required")
    void testSourcingMethodElectionRequired() {
        // Given: Sales data without election
        Map<String, BigDecimal> sales = new HashMap<>();
        sales.put("entity", new BigDecimal("1000000"));

        // When/Then: Should throw exception when election is null
        assertThrows(IllegalArgumentException.class, () -> {
            sourcingService.calculateSalesDenominator(sales, null, tenantId);
        });
    }

    @Test
    @DisplayName("Handle zero sales gracefully")
    void testHandleZeroSales() {
        // Given: Entity with zero sales
        Map<String, BigDecimal> sales = new HashMap<>();
        sales.put("entity", BigDecimal.ZERO);

        // When: Calculate with Finnigan
        BigDecimal denominator = sourcingService.calculateSalesDenominator(
                sales, SourcingMethodElection.FINNIGAN, tenantId);

        // Then: Denominator is zero
        assertEquals(BigDecimal.ZERO, denominator);
    }

    @Test
    @DisplayName("Handle empty affiliated group")
    void testHandleEmptyAffiliatedGroup() {
        // Given: Empty sales map
        Map<String, BigDecimal> sales = new HashMap<>();

        // When: Calculate with Finnigan
        BigDecimal denominator = sourcingService.calculateSalesDenominator(
                sales, SourcingMethodElection.FINNIGAN, tenantId);

        // Then: Denominator is zero
        assertEquals(BigDecimal.ZERO, denominator);
    }

    // ========================================
    // User Story 3: Market-Based Service Sourcing Tests (T091-T092)
    // ========================================

    @Test
    @DisplayName("T091: Market-Based Sourcing - Source service revenue to customer location")
    void testMarketBasedSourcingToCustomerLocation() {
        // Given: IT consulting firm (OH office) provides $1M project to NY customer
        // Market-based: 100% to NY (where customer receives benefit)
        
        String customerState = "NY";
        BigDecimal serviceRevenue = new BigDecimal("1000000"); // $1M
        
        // When: Apply market-based sourcing
        Map<String, BigDecimal> sourcedRevenue = sourcingService.sourceServiceRevenue(
                serviceRevenue, 
                customerState, 
                ServiceSourcingMethod.MARKET_BASED,
                null, // No employee locations needed for market-based
                tenantId
        );
        
        // Then: 100% sourced to NY (customer location)
        assertEquals(new BigDecimal("1000000"), sourcedRevenue.get("NY"));
        assertEquals(1, sourcedRevenue.size()); // Only NY should be present
    }

    @Test
    @DisplayName("T091: Market-Based Sourcing - Handle multi-location customer")
    void testMarketBasedSourcingMultiLocationCustomer() {
        // Given: Fortune 500 customer with offices in multiple states
        // Customer has 60% operations in NY, 40% in CA
        
        Map<String, BigDecimal> customerLocations = new HashMap<>();
        customerLocations.put("NY", new BigDecimal("0.60")); // 60% in NY
        customerLocations.put("CA", new BigDecimal("0.40")); // 40% in CA
        
        BigDecimal serviceRevenue = new BigDecimal("1000000"); // $1M
        
        // When: Apply market-based sourcing with customer location proration
        Map<String, BigDecimal> sourcedRevenue = sourcingService.sourceServiceRevenueMultiLocation(
                serviceRevenue, 
                customerLocations, 
                ServiceSourcingMethod.MARKET_BASED,
                tenantId
        );
        
        // Then: Revenue prorated by customer locations
        assertEquals(new BigDecimal("600000"), sourcedRevenue.get("NY")); // $600K to NY
        assertEquals(new BigDecimal("400000"), sourcedRevenue.get("CA")); // $400K to CA
    }

    @Test
    @DisplayName("T091: Market-Based Sourcing - Cascading fallback when customer location unknown")
    void testMarketBasedSourcingCascadingFallback() {
        // Given: Service revenue but customer location is unknown
        BigDecimal serviceRevenue = new BigDecimal("500000"); // $500K
        String customerState = null; // Unknown customer location
        
        Map<String, BigDecimal> employeeLocations = new HashMap<>();
        employeeLocations.put("OH", new BigDecimal("0.70")); // 70% employees in OH
        employeeLocations.put("CA", new BigDecimal("0.30")); // 30% employees in CA
        
        // When: Apply cascading sourcing rules
        // Should fallback from market-based → cost-of-performance (employee location)
        Map<String, BigDecimal> sourcedRevenue = sourcingService.sourceServiceRevenueWithFallback(
                serviceRevenue, 
                customerState, 
                employeeLocations,
                ServiceSourcingMethod.MARKET_BASED,
                tenantId
        );
        
        // Then: Falls back to cost-of-performance (employee locations)
        assertEquals(new BigDecimal("350000"), sourcedRevenue.get("OH")); // $350K to OH (70%)
        assertEquals(new BigDecimal("150000"), sourcedRevenue.get("CA")); // $150K to CA (30%)
    }

    @Test
    @DisplayName("T092: Cost-of-Performance - Prorate service revenue by employee location")
    void testCostOfPerformanceSourcingByEmployeeLocation() {
        // Given: IT consulting firm with OH office (5 employees) and CA office (2 employees)
        // Provides $1M project to NY customer
        // Cost-of-performance: Prorate by employee location (70% OH, 30% CA)
        
        BigDecimal serviceRevenue = new BigDecimal("1000000"); // $1M
        String customerState = "NY"; // Customer location (ignored for cost-of-performance)
        
        Map<String, BigDecimal> employeeLocations = new HashMap<>();
        employeeLocations.put("OH", new BigDecimal("0.70")); // 70% of employees in OH
        employeeLocations.put("CA", new BigDecimal("0.30")); // 30% of employees in CA
        
        // When: Apply cost-of-performance sourcing
        Map<String, BigDecimal> sourcedRevenue = sourcingService.sourceServiceRevenue(
                serviceRevenue, 
                customerState, 
                ServiceSourcingMethod.COST_OF_PERFORMANCE,
                employeeLocations,
                tenantId
        );
        
        // Then: Revenue prorated by employee locations (NY gets $0)
        assertEquals(new BigDecimal("700000"), sourcedRevenue.get("OH")); // $700K to OH
        assertEquals(new BigDecimal("300000"), sourcedRevenue.get("CA")); // $300K to CA
        assertNull(sourcedRevenue.get("NY")); // NY gets nothing (no employees there)
    }

    @Test
    @DisplayName("T092: Cost-of-Performance - Prorate by payroll when available")
    void testCostOfPerformanceSourcingByPayroll() {
        // Given: Service revenue with payroll data available
        // OH payroll: $3.5M (70%), CA payroll: $1.5M (30%)
        
        BigDecimal serviceRevenue = new BigDecimal("1000000"); // $1M service revenue
        
        Map<String, BigDecimal> payrollByState = new HashMap<>();
        payrollByState.put("OH", new BigDecimal("3500000")); // $3.5M payroll
        payrollByState.put("CA", new BigDecimal("1500000")); // $1.5M payroll
        
        // When: Apply cost-of-performance with payroll proration
        Map<String, BigDecimal> sourcedRevenue = sourcingService.sourceServiceRevenueByPayroll(
                serviceRevenue, 
                payrollByState,
                tenantId
        );
        
        // Then: Revenue prorated by payroll percentages
        assertEquals(new BigDecimal("700000"), sourcedRevenue.get("OH")); // 70% → $700K
        assertEquals(new BigDecimal("300000"), sourcedRevenue.get("CA")); // 30% → $300K
    }

    @Test
    @DisplayName("T092: Cost-of-Performance - Handle remote employees")
    void testCostOfPerformanceWithRemoteEmployees() {
        // Given: Company has remote employees in multiple states
        Map<String, Integer> employeeCountsByState = new HashMap<>();
        employeeCountsByState.put("OH", 5);  // 5 employees in OH
        employeeCountsByState.put("CA", 2);  // 2 employees in CA
        employeeCountsByState.put("TX", 1);  // 1 remote employee in TX
        
        BigDecimal serviceRevenue = new BigDecimal("800000"); // $800K
        
        // When: Apply cost-of-performance with employee counts
        Map<String, BigDecimal> sourcedRevenue = sourcingService.sourceServiceRevenueByEmployeeCount(
                serviceRevenue, 
                employeeCountsByState,
                tenantId
        );
        
        // Then: Revenue prorated by employee counts (5/8, 2/8, 1/8)
        assertEquals(new BigDecimal("500000"), sourcedRevenue.get("OH")); // 5/8 = $500K
        assertEquals(new BigDecimal("200000"), sourcedRevenue.get("CA")); // 2/8 = $200K
        assertEquals(new BigDecimal("100000"), sourcedRevenue.get("TX")); // 1/8 = $100K
    }

    @Test
    @DisplayName("T091: Cascading Rules - Market-Based → Cost-of-Performance → Pro-Rata")
    void testCascadingSourcingRulesFallbackChain() {
        // Given: Service revenue with no customer location and no employee location data
        BigDecimal serviceRevenue = new BigDecimal("600000"); // $600K
        String customerState = null; // Unknown
        Map<String, BigDecimal> employeeLocations = null; // Unknown
        
        // Assume overall apportionment is 40% OH
        BigDecimal overallApportionment = new BigDecimal("0.40");
        String filingState = "OH";
        
        // When: Apply cascading rules (all fallbacks exhausted → pro-rata)
        Map<String, BigDecimal> sourcedRevenue = sourcingService.sourceServiceRevenueWithFullCascade(
                serviceRevenue, 
                customerState, 
                employeeLocations,
                overallApportionment,
                filingState,
                tenantId
        );
        
        // Then: Falls back to pro-rata based on overall apportionment
        assertEquals(new BigDecimal("240000"), sourcedRevenue.get("OH")); // 40% → $240K
        assertEquals(new BigDecimal("360000"), sourcedRevenue.get("EVERYWHERE_ELSE")); // 60% → $360K
    }

    @Test
    @DisplayName("T091: Validate customer location required for market-based")
    void testMarketBasedRequiresCustomerLocation() {
        // Given: Market-based sourcing without customer location
        BigDecimal serviceRevenue = new BigDecimal("500000");
        String customerState = null; // Missing customer location
        
        // When/Then: Should throw exception when customer location missing and no fallback allowed
        assertThrows(IllegalArgumentException.class, () -> {
            sourcingService.sourceServiceRevenue(
                    serviceRevenue, 
                    customerState, 
                    ServiceSourcingMethod.MARKET_BASED,
                    null,
                    tenantId
            );
        });
    }

    @Test
    @DisplayName("T092: Validate employee locations required for cost-of-performance")
    void testCostOfPerformanceRequiresEmployeeLocations() {
        // Given: Cost-of-performance sourcing without employee location data
        BigDecimal serviceRevenue = new BigDecimal("500000");
        Map<String, BigDecimal> employeeLocations = null; // Missing employee data
        
        // When/Then: Should throw exception when employee locations missing
        assertThrows(IllegalArgumentException.class, () -> {
            sourcingService.sourceServiceRevenue(
                    serviceRevenue, 
                    "NY", // Customer state (ignored for cost-of-performance)
                    ServiceSourcingMethod.COST_OF_PERFORMANCE,
                    employeeLocations,
                    tenantId
            );
        });
    }
}
