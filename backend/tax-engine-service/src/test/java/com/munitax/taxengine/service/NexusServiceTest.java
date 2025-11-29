package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.NexusReason;
import com.munitax.taxengine.domain.apportionment.NexusTracking;
import com.munitax.taxengine.repository.NexusTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NexusService.
 * Tests nexus determination: physical, employee, economic, factor presence.
 * Task: T073 [US2]
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NexusService Tests")
class NexusServiceTest {

    @Mock
    private NexusTrackingRepository nexusTrackingRepository;

    @InjectMocks
    private NexusService nexusService;

    private UUID businessId;
    private UUID tenantId;
    private static final String STATE_OH = "OH";
    private static final String STATE_CA = "CA";
    private static final String STATE_NY = "NY";

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should return true when nexus record exists with hasNexus=true")
    void testHasNexus_RecordExists_True() {
        // Arrange
        NexusTracking nexusTracking = createNexusTracking(STATE_OH, true, NexusReason.PHYSICAL_PRESENCE);
        when(nexusTrackingRepository.findByBusinessIdAndStateAndTenantId(businessId, STATE_OH, tenantId))
            .thenReturn(Optional.of(nexusTracking));

        // Act
        boolean result = nexusService.hasNexus(businessId, STATE_OH, tenantId);

        // Assert
        assertTrue(result, "Should return true when nexus exists");
        verify(nexusTrackingRepository).findByBusinessIdAndStateAndTenantId(businessId, STATE_OH, tenantId);
    }

    @Test
    @DisplayName("Should return false when nexus record exists with hasNexus=false")
    void testHasNexus_RecordExists_False() {
        // Arrange
        NexusTracking nexusTracking = createNexusTracking(STATE_CA, false, NexusReason.FACTOR_PRESENCE);
        when(nexusTrackingRepository.findByBusinessIdAndStateAndTenantId(businessId, STATE_CA, tenantId))
            .thenReturn(Optional.of(nexusTracking));

        // Act
        boolean result = nexusService.hasNexus(businessId, STATE_CA, tenantId);

        // Assert
        assertFalse(result, "Should return false when no nexus");
        verify(nexusTrackingRepository).findByBusinessIdAndStateAndTenantId(businessId, STATE_CA, tenantId);
    }

    @Test
    @DisplayName("Should return false when no nexus record exists")
    void testHasNexus_NoRecord() {
        // Arrange
        when(nexusTrackingRepository.findByBusinessIdAndStateAndTenantId(businessId, STATE_NY, tenantId))
            .thenReturn(Optional.empty());

        // Act
        boolean result = nexusService.hasNexus(businessId, STATE_NY, tenantId);

        // Assert
        assertFalse(result, "Should return false when no record exists");
        verify(nexusTrackingRepository).findByBusinessIdAndStateAndTenantId(businessId, STATE_NY, tenantId);
    }

    @Test
    @DisplayName("Should return true for economic nexus when sales exceed threshold")
    void testHasEconomicNexus_SalesExceedThreshold() {
        // Arrange
        BigDecimal totalSales = new BigDecimal("600000"); // Exceeds $500K threshold
        int transactionCount = 150;

        // Act
        boolean result = nexusService.hasEconomicNexus(STATE_CA, totalSales, transactionCount);

        // Assert
        assertTrue(result, "Should return true when sales exceed $500K threshold");
    }

    @Test
    @DisplayName("Should return true for economic nexus when transactions exceed threshold")
    void testHasEconomicNexus_TransactionsExceedThreshold() {
        // Arrange
        BigDecimal totalSales = new BigDecimal("400000"); // Below $500K threshold
        int transactionCount = 250; // Exceeds 200 transaction threshold

        // Act
        boolean result = nexusService.hasEconomicNexus(STATE_CA, totalSales, transactionCount);

        // Assert
        assertTrue(result, "Should return true when transactions exceed 200 threshold");
    }

    @Test
    @DisplayName("Should return false for economic nexus when both thresholds not met")
    void testHasEconomicNexus_ThresholdsNotMet() {
        // Arrange
        BigDecimal totalSales = new BigDecimal("400000"); // Below $500K threshold
        int transactionCount = 150; // Below 200 threshold

        // Act
        boolean result = nexusService.hasEconomicNexus(STATE_NY, totalSales, transactionCount);

        // Assert
        assertFalse(result, "Should return false when neither threshold is met");
    }

    @Test
    @DisplayName("Should return true for economic nexus at exact sales threshold")
    void testHasEconomicNexus_ExactSalesThreshold() {
        // Arrange
        BigDecimal totalSales = new BigDecimal("500000"); // Exactly $500K
        int transactionCount = 0;

        // Act
        boolean result = nexusService.hasEconomicNexus(STATE_OH, totalSales, transactionCount);

        // Assert
        assertTrue(result, "Should return true at exact sales threshold");
    }

    @Test
    @DisplayName("Should return true for economic nexus at exact transaction threshold")
    void testHasEconomicNexus_ExactTransactionThreshold() {
        // Arrange
        BigDecimal totalSales = BigDecimal.ZERO;
        int transactionCount = 200; // Exactly 200 transactions

        // Act
        boolean result = nexusService.hasEconomicNexus(STATE_OH, totalSales, transactionCount);

        // Assert
        assertTrue(result, "Should return true at exact transaction threshold");
    }

    @Test
    @DisplayName("Should create new nexus record when none exists")
    void testUpdateNexusStatus_CreateNew() {
        // Arrange
        when(nexusTrackingRepository.findByBusinessIdAndStateAndTenantId(businessId, STATE_CA, tenantId))
            .thenReturn(Optional.empty());
        
        NexusTracking savedTracking = createNexusTracking(STATE_CA, true, NexusReason.ECONOMIC_NEXUS);
        when(nexusTrackingRepository.save(any(NexusTracking.class))).thenReturn(savedTracking);

        // Act
        NexusTracking result = nexusService.updateNexusStatus(
            businessId, STATE_CA, true, NexusReason.ECONOMIC_NEXUS, tenantId
        );

        // Assert
        assertNotNull(result, "Should return saved nexus tracking");
        assertTrue(result.getHasNexus(), "Should have nexus");
        assertEquals(STATE_CA, result.getState(), "State should match");
        verify(nexusTrackingRepository).save(any(NexusTracking.class));
    }

    @Test
    @DisplayName("Should update existing nexus record")
    void testUpdateNexusStatus_UpdateExisting() {
        // Arrange
        NexusTracking existingTracking = createNexusTracking(STATE_OH, false, NexusReason.FACTOR_PRESENCE);
        when(nexusTrackingRepository.findByBusinessIdAndStateAndTenantId(businessId, STATE_OH, tenantId))
            .thenReturn(Optional.of(existingTracking));
        
        when(nexusTrackingRepository.save(any(NexusTracking.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        NexusTracking result = nexusService.updateNexusStatus(
            businessId, STATE_OH, true, NexusReason.PHYSICAL_PRESENCE, tenantId
        );

        // Assert
        assertNotNull(result, "Should return updated nexus tracking");
        assertTrue(result.getHasNexus(), "Should have nexus after update");
        verify(nexusTrackingRepository).save(any(NexusTracking.class));
    }

    @Test
    @DisplayName("Should get all states with nexus for a business")
    void testGetNexusStates() {
        // Arrange
        List<NexusTracking> nexusStates = Arrays.asList(
            createNexusTracking(STATE_OH, true, NexusReason.PHYSICAL_PRESENCE),
            createNexusTracking(STATE_CA, true, NexusReason.ECONOMIC_NEXUS)
        );
        when(nexusTrackingRepository.findNexusStates(businessId, tenantId)).thenReturn(nexusStates);

        // Act
        List<NexusTracking> result = nexusService.getNexusStates(businessId, tenantId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return 2 nexus states");
        verify(nexusTrackingRepository).findNexusStates(businessId, tenantId);
    }

    @Test
    @DisplayName("Should get all states without nexus for a business")
    void testGetNonNexusStates() {
        // Arrange
        List<NexusTracking> nonNexusStates = Arrays.asList(
            createNexusTracking(STATE_NY, false, NexusReason.FACTOR_PRESENCE),
            createNexusTracking("TX", false, NexusReason.FACTOR_PRESENCE)
        );
        when(nexusTrackingRepository.findNonNexusStates(businessId, tenantId)).thenReturn(nonNexusStates);

        // Act
        List<NexusTracking> result = nexusService.getNonNexusStates(businessId, tenantId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return 2 non-nexus states");
        verify(nexusTrackingRepository).findNonNexusStates(businessId, tenantId);
    }

    @Test
    @DisplayName("Should get all nexus records for a business")
    void testGetAllNexusRecords() {
        // Arrange
        List<NexusTracking> allRecords = Arrays.asList(
            createNexusTracking(STATE_OH, true, NexusReason.PHYSICAL_PRESENCE),
            createNexusTracking(STATE_CA, false, NexusReason.FACTOR_PRESENCE),
            createNexusTracking(STATE_NY, true, NexusReason.ECONOMIC_NEXUS)
        );
        when(nexusTrackingRepository.findByBusinessIdAndTenantId(businessId, tenantId)).thenReturn(allRecords);

        // Act
        List<NexusTracking> result = nexusService.getAllNexusRecords(businessId, tenantId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Should return 3 total records");
        verify(nexusTrackingRepository).findByBusinessIdAndTenantId(businessId, tenantId);
    }

    @Test
    @DisplayName("Should count nexus states correctly")
    void testCountNexusStates() {
        // Arrange
        when(nexusTrackingRepository.countNexusStates(businessId, tenantId)).thenReturn(5L);

        // Act
        long result = nexusService.countNexusStates(businessId, tenantId);

        // Assert
        assertEquals(5L, result, "Should return correct count of nexus states");
        verify(nexusTrackingRepository).countNexusStates(businessId, tenantId);
    }

    @Test
    @DisplayName("Should batch update multiple nexus records")
    void testBatchUpdateNexusStatus() {
        // Arrange
        List<NexusTracking> nexusRecords = Arrays.asList(
            createNexusTracking(STATE_OH, true, NexusReason.PHYSICAL_PRESENCE),
            createNexusTracking(STATE_CA, true, NexusReason.ECONOMIC_NEXUS),
            createNexusTracking(STATE_NY, false, NexusReason.FACTOR_PRESENCE)
        );

        when(nexusTrackingRepository.findByBusinessIdAndStateAndTenantId(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(nexusTrackingRepository.save(any(NexusTracking.class)))
            .thenAnswer(i -> i.getArguments()[0]);

        // Act
        nexusService.batchUpdateNexusStatus(businessId, nexusRecords, tenantId);

        // Assert
        verify(nexusTrackingRepository, times(3)).save(any(NexusTracking.class));
    }

    @Test
    @DisplayName("Should handle empty list for batch update")
    void testBatchUpdateNexusStatus_EmptyList() {
        // Arrange
        List<NexusTracking> emptyList = Collections.emptyList();

        // Act
        nexusService.batchUpdateNexusStatus(businessId, emptyList, tenantId);

        // Assert
        verify(nexusTrackingRepository, never()).save(any(NexusTracking.class));
    }

    @Test
    @DisplayName("Should return empty list when business has no nexus records")
    void testGetAllNexusRecords_EmptyList() {
        // Arrange
        when(nexusTrackingRepository.findByBusinessIdAndTenantId(businessId, tenantId))
            .thenReturn(Collections.emptyList());

        // Act
        List<NexusTracking> result = nexusService.getAllNexusRecords(businessId, tenantId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Should return empty list");
    }

    @Test
    @DisplayName("Should return zero when business has no nexus states")
    void testCountNexusStates_Zero() {
        // Arrange
        when(nexusTrackingRepository.countNexusStates(businessId, tenantId)).thenReturn(0L);

        // Act
        long result = nexusService.countNexusStates(businessId, tenantId);

        // Assert
        assertEquals(0L, result, "Should return zero count");
    }

    // Helper method to create nexus tracking test data
    private NexusTracking createNexusTracking(String state, boolean hasNexus, NexusReason reason) {
        NexusTracking tracking = new NexusTracking();
        tracking.setNexusId(UUID.randomUUID());
        tracking.setBusinessId(businessId);
        tracking.setState(state);
        tracking.setHasNexus(hasNexus);
        tracking.setNexusReasons(new ArrayList<>(Arrays.asList(reason)));
        tracking.setTenantId(tenantId);
        return tracking;
    }
}
