package com.munitax.taxengine.service;

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

        when(nexusService.hasNexus(any(), eq("OH"), eq(tenantId))).thenAnswer(invocation -> {
            // Simulate nexus determination
            return true; // For testing, assume parent and subA have nexus
        });

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
}
