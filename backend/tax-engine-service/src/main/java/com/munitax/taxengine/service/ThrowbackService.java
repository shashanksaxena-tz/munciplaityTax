package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.ThrowbackElection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for applying throwback and throwout rules.
 * Implements throwback/throwout logic for destination states without nexus.
 * Task: T074 [US2]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThrowbackService {

    private final NexusService nexusService;

    /**
     * Apply throwback/throwout rules for a sale transaction.
     * When business ships goods to a state without nexus:
     * - THROWBACK: Include sale in origin state numerator (amount unchanged)
     * - THROWOUT: Exclude sale from both numerator and denominator (return zero)
     *
     * @param saleAmount        the sale amount
     * @param originState       the state where sale originated
     * @param destinationState  the state where goods/services delivered
     * @param businessId        the business ID
     * @param tenantId          the tenant ID for multi-tenant isolation
     * @param election          throwback or throwout election
     * @return adjusted sale amount (original for throwback, zero for throwout)
     */
    public BigDecimal applyThrowbackRule(BigDecimal saleAmount, 
                                        String originState,
                                        String destinationState,
                                        UUID businessId,
                                        UUID tenantId,
                                        ThrowbackElection election) {
        // Handle null or zero amounts
        if (saleAmount == null || saleAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Zero or null sale amount, no throwback adjustment needed");
            return saleAmount == null ? BigDecimal.ZERO : saleAmount;
        }

        // Same state sales don't need throwback
        if (originState != null && originState.equals(destinationState)) {
            log.debug("Sale within same state ({}), no throwback adjustment", originState);
            return saleAmount;
        }

        // Default to THROWBACK if election is null
        ThrowbackElection effectiveElection = election != null ? election : ThrowbackElection.THROWBACK;

        // Check if business has nexus in destination state
        boolean hasDestinationNexus = nexusService.hasNexus(businessId, destinationState, tenantId);

        if (hasDestinationNexus) {
            // Business has nexus in destination state, sale is properly sourced
            log.debug("Business has nexus in destination state {}, no throwback needed", destinationState);
            return saleAmount;
        }

        // No nexus in destination state - apply throwback or throwout
        if (effectiveElection == ThrowbackElection.THROWOUT) {
            log.info("Throwing out sale of {} to {} (no nexus, throwout elected)", 
                    saleAmount, destinationState);
            return BigDecimal.ZERO;
        } else {
            log.info("Throwing back sale of {} from destination {} to origin {} (no nexus)", 
                    saleAmount, destinationState, originState);
            return saleAmount;
        }
    }

    /**
     * Check if throwback should be applied for a sale.
     * Throwback applies when destination state lacks nexus.
     *
     * @param destinationState  the destination state
     * @param businessId        the business ID
     * @param tenantId          the tenant ID for multi-tenant isolation
     * @return true if throwback should be applied
     */
    public boolean shouldApplyThrowback(String destinationState, UUID businessId, UUID tenantId) {
        boolean hasNexus = nexusService.hasNexus(businessId, destinationState, tenantId);
        boolean shouldApply = !hasNexus;
        
        log.debug("Throwback check for destination {}: hasNexus={}, shouldApply={}", 
                destinationState, hasNexus, shouldApply);
        
        return shouldApply;
    }

    /**
     * Determine which state to source the sale to after applying throwback rules.
     * Returns:
     * - Destination state if business has nexus there
     * - Origin state if throwback elected and no nexus in destination
     * - null if throwout elected and no nexus in destination
     *
     * @param originState       the state where sale originated
     * @param destinationState  the state where goods/services delivered
     * @param businessId        the business ID
     * @param tenantId          the tenant ID for multi-tenant isolation
     * @param election          throwback or throwout election
     * @return state code to source the sale to, or null if thrown out
     */
    public String determineThrowbackState(String originState,
                                         String destinationState,
                                         UUID businessId,
                                         UUID tenantId,
                                         ThrowbackElection election) {
        // Check if business has nexus in destination state
        boolean hasDestinationNexus = nexusService.hasNexus(businessId, destinationState, tenantId);

        if (hasDestinationNexus) {
            // Sale properly sourced to destination
            log.debug("Sale sourced to destination state: {}", destinationState);
            return destinationState;
        }

        // No nexus in destination - apply election
        ThrowbackElection effectiveElection = election != null ? election : ThrowbackElection.THROWBACK;

        if (effectiveElection == ThrowbackElection.THROWOUT) {
            log.debug("Sale thrown out (excluded from both numerator and denominator)");
            return null;
        } else {
            log.debug("Sale thrown back to origin state: {}", originState);
            return originState;
        }
    }
}
