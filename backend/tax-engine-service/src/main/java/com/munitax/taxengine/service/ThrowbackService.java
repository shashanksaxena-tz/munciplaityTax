package com.munitax.taxengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for applying throwback and throwout rules.
 * Stub implementation - to be completed in T074 (US2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThrowbackService {

    /**
     * Apply throwback/throwout rules for a sale transaction.
     * This is a placeholder method that will be fully implemented in US2 (T074).
     *
     * @param saleAmount        the sale amount
     * @param originState       the state where sale originated
     * @param destinationState  the state where goods/services delivered
     * @param businessId        the business ID
     * @return adjusted sale amount (may be thrown back to origin state)
     */
    public BigDecimal applyThrowbackRule(BigDecimal saleAmount, 
                                        String originState,
                                        String destinationState,
                                        UUID businessId) {
        // Stub: Return original amount without adjustment
        // Full implementation in T074
        log.debug("Throwback service stub called (not yet implemented)");
        return saleAmount;
    }

    /**
     * Check if throwback should be applied for a sale.
     *
     * @param destinationState  the destination state
     * @param businessId        the business ID
     * @return true if throwback should be applied
     */
    public boolean shouldApplyThrowback(String destinationState, UUID businessId) {
        // Stub: Always return false
        // Full implementation in T074
        return false;
    }
}
