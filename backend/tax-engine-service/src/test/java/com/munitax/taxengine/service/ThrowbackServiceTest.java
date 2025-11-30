package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.ThrowbackElection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ThrowbackService.
 * Tests throwback rule application and throwout alternative.
 * Task: T072 [US2]
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ThrowbackService Tests")
class ThrowbackServiceTest {

    @Mock
    private NexusService nexusService;

    @InjectMocks
    private ThrowbackService throwbackService;

    private UUID businessId;
    private UUID tenantId;
    private static final String ORIGIN_STATE = "OH";
    private static final String DESTINATION_STATE = "CA";

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should apply throwback when destination state has no nexus and throwback elected")
    void testApplyThrowback_NoNexus_ThrowbackElected() {
        // Arrange
        BigDecimal saleAmount = new BigDecimal("100000");
        when(nexusService.hasNexus(businessId, DESTINATION_STATE, tenantId)).thenReturn(false);

        // Act
        BigDecimal result = throwbackService.applyThrowbackRule(
            saleAmount, 
            ORIGIN_STATE, 
            DESTINATION_STATE, 
            businessId,
            tenantId,
            ThrowbackElection.THROWBACK
        );

        // Assert
        assertEquals(saleAmount, result, "Sale amount should remain unchanged (thrown back to origin)");
        verify(nexusService).hasNexus(businessId, DESTINATION_STATE, tenantId);
    }

    @Test
    @DisplayName("Should not apply throwback when destination state has nexus")
    void testApplyThrowback_HasNexus() {
        // Arrange
        BigDecimal saleAmount = new BigDecimal("100000");
        when(nexusService.hasNexus(businessId, DESTINATION_STATE, tenantId)).thenReturn(true);

        // Act
        BigDecimal result = throwbackService.applyThrowbackRule(
            saleAmount, 
            ORIGIN_STATE, 
            DESTINATION_STATE, 
            businessId,
            tenantId,
            ThrowbackElection.THROWBACK
        );

        // Assert
        assertEquals(saleAmount, result, "Sale amount should remain unchanged (destination has nexus)");
        verify(nexusService).hasNexus(businessId, DESTINATION_STATE, tenantId);
    }

    @Test
    @DisplayName("Should apply throwout when destination state has no nexus and throwout elected")
    void testApplyThrowout_NoNexus() {
        // Arrange
        BigDecimal saleAmount = new BigDecimal("100000");
        when(nexusService.hasNexus(businessId, DESTINATION_STATE, tenantId)).thenReturn(false);

        // Act
        BigDecimal result = throwbackService.applyThrowbackRule(
            saleAmount, 
            ORIGIN_STATE, 
            DESTINATION_STATE, 
            businessId,
            tenantId,
            ThrowbackElection.THROWOUT
        );

        // Assert
        assertEquals(BigDecimal.ZERO, result, "Sale amount should be zero (thrown out from denominator)");
        verify(nexusService).hasNexus(businessId, DESTINATION_STATE, tenantId);
    }

    @Test
    @DisplayName("Should return true when throwback should be applied")
    void testShouldApplyThrowback_NoNexus() {
        // Arrange
        when(nexusService.hasNexus(businessId, DESTINATION_STATE, tenantId)).thenReturn(false);

        // Act
        boolean result = throwbackService.shouldApplyThrowback(DESTINATION_STATE, businessId, tenantId);

        // Assert
        assertTrue(result, "Throwback should be applied when destination state has no nexus");
        verify(nexusService).hasNexus(businessId, DESTINATION_STATE, tenantId);
    }

    @Test
    @DisplayName("Should return false when throwback should not be applied")
    void testShouldApplyThrowback_HasNexus() {
        // Arrange
        when(nexusService.hasNexus(businessId, DESTINATION_STATE, tenantId)).thenReturn(true);

        // Act
        boolean result = throwbackService.shouldApplyThrowback(DESTINATION_STATE, businessId, tenantId);

        // Assert
        assertFalse(result, "Throwback should not be applied when destination state has nexus");
        verify(nexusService).hasNexus(businessId, DESTINATION_STATE, tenantId);
    }

    @Test
    @DisplayName("Should handle sale to same state (origin equals destination)")
    void testApplyThrowback_SameState() {
        // Arrange
        BigDecimal saleAmount = new BigDecimal("100000");
        String sameState = "OH";

        // Act
        BigDecimal result = throwbackService.applyThrowbackRule(
            saleAmount, 
            sameState, 
            sameState, 
            businessId,
            tenantId,
            ThrowbackElection.THROWBACK
        );

        // Assert
        assertEquals(saleAmount, result, "Sale amount should remain unchanged (same state)");
        verify(nexusService, never()).hasNexus(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle null throwback election as THROWBACK default")
    void testApplyThrowback_NullElection() {
        // Arrange
        BigDecimal saleAmount = new BigDecimal("100000");
        when(nexusService.hasNexus(businessId, DESTINATION_STATE, tenantId)).thenReturn(false);

        // Act
        BigDecimal result = throwbackService.applyThrowbackRule(
            saleAmount, 
            ORIGIN_STATE, 
            DESTINATION_STATE, 
            businessId,
            tenantId,
            null
        );

        // Assert
        assertEquals(saleAmount, result, "Sale amount should remain unchanged (default throwback)");
    }

    @Test
    @DisplayName("Should handle zero sale amount")
    void testApplyThrowback_ZeroAmount() {
        // Arrange
        BigDecimal saleAmount = BigDecimal.ZERO;

        // Act
        BigDecimal result = throwbackService.applyThrowbackRule(
            saleAmount, 
            ORIGIN_STATE, 
            DESTINATION_STATE, 
            businessId,
            tenantId,
            ThrowbackElection.THROWBACK
        );

        // Assert
        assertEquals(BigDecimal.ZERO, result, "Zero amount should remain zero");
    }

    @Test
    @DisplayName("Should handle negative sale amount")
    void testApplyThrowback_NegativeAmount() {
        // Arrange
        BigDecimal saleAmount = new BigDecimal("-50000");

        // Act
        BigDecimal result = throwbackService.applyThrowbackRule(
            saleAmount, 
            ORIGIN_STATE, 
            DESTINATION_STATE, 
            businessId,
            tenantId,
            ThrowbackElection.THROWBACK
        );

        // Assert
        assertEquals(saleAmount, result, "Negative amount should be preserved");
    }

    @Test
    @DisplayName("Should determine throwback state correctly for no-nexus destination")
    void testDetermineThrowbackState_NoNexus() {
        // Arrange
        when(nexusService.hasNexus(businessId, DESTINATION_STATE, tenantId)).thenReturn(false);

        // Act
        String result = throwbackService.determineThrowbackState(
            ORIGIN_STATE, 
            DESTINATION_STATE, 
            businessId,
            tenantId,
            ThrowbackElection.THROWBACK
        );

        // Assert
        assertEquals(ORIGIN_STATE, result, "Sale should be thrown back to origin state");
        verify(nexusService).hasNexus(businessId, DESTINATION_STATE, tenantId);
    }

    @Test
    @DisplayName("Should determine throwback state correctly for nexus destination")
    void testDetermineThrowbackState_HasNexus() {
        // Arrange
        when(nexusService.hasNexus(businessId, DESTINATION_STATE, tenantId)).thenReturn(true);

        // Act
        String result = throwbackService.determineThrowbackState(
            ORIGIN_STATE, 
            DESTINATION_STATE, 
            businessId,
            tenantId,
            ThrowbackElection.THROWBACK
        );

        // Assert
        assertEquals(DESTINATION_STATE, result, "Sale should be sourced to destination state");
        verify(nexusService).hasNexus(businessId, DESTINATION_STATE, tenantId);
    }

    @Test
    @DisplayName("Should return null for throwout election with no nexus")
    void testDetermineThrowbackState_Throwout() {
        // Arrange
        when(nexusService.hasNexus(businessId, DESTINATION_STATE, tenantId)).thenReturn(false);

        // Act
        String result = throwbackService.determineThrowbackState(
            ORIGIN_STATE, 
            DESTINATION_STATE, 
            businessId,
            tenantId,
            ThrowbackElection.THROWOUT
        );

        // Assert
        assertNull(result, "Sale should be thrown out (excluded from numerator and denominator)");
        verify(nexusService).hasNexus(businessId, DESTINATION_STATE, tenantId);
    }
}
