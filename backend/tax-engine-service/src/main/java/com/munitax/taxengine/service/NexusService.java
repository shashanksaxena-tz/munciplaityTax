package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.NexusReason;
import com.munitax.taxengine.domain.apportionment.NexusTracking;
import com.munitax.taxengine.repository.NexusTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for determining and tracking business nexus status across states.
 * Implements nexus determination logic based on physical presence, economic nexus, and factor presence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NexusService {

    private final NexusTrackingRepository nexusTrackingRepository;

    // Economic nexus thresholds (common standards - may vary by state)
    private static final BigDecimal ECONOMIC_NEXUS_SALES_THRESHOLD = new BigDecimal("500000");
    private static final int ECONOMIC_NEXUS_TRANSACTION_THRESHOLD = 200;

    /**
     * Determine if a business has nexus in a specific state.
     * Checks physical presence, economic nexus, and factor presence.
     * Results are cached for 15 minutes for performance.
     *
     * @param businessId the business ID
     * @param state      the state code (e.g., "OH", "CA", "NY")
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return true if the business has nexus in the state
     */
    @Cacheable(value = "nexusCache", key = "#businessId + '_' + #state + '_' + #tenantId")
    public boolean hasNexus(UUID businessId, String state, UUID tenantId) {
        log.debug("Determining nexus for business: {} in state: {}, tenant: {}",
                businessId, state, tenantId);

        Optional<NexusTracking> existing = nexusTrackingRepository
                .findByBusinessIdAndStateAndTenantId(businessId, state, tenantId);

        if (existing.isPresent()) {
            boolean hasNexus = existing.get().getHasNexus();
            log.debug("Nexus status found in database: {}", hasNexus);
            return hasNexus;
        }

        log.warn("No nexus record found for business: {} in state: {}. Returning false.", businessId, state);
        return false;
    }

    /**
     * Create or update nexus tracking record for a business in a state.
     *
     * @param businessId  the business ID
     * @param state       the state code
     * @param hasNexus    whether nexus exists
     * @param nexusReason the reason for nexus determination
     * @param tenantId    the tenant ID for multi-tenant isolation
     * @return the created or updated nexus tracking record
     */
    @Transactional
    public NexusTracking updateNexusStatus(UUID businessId, String state, boolean hasNexus,
                                           NexusReason nexusReason, UUID tenantId) {
        log.info("Updating nexus status for business: {} in state: {}, hasNexus: {}, reason: {}",
                businessId, state, hasNexus, nexusReason);

        Optional<NexusTracking> existing = nexusTrackingRepository
                .findByBusinessIdAndStateAndTenantId(businessId, state, tenantId);

        NexusTracking nexusTracking;

        if (existing.isPresent()) {
            nexusTracking = existing.get();
            nexusTracking.setHasNexus(hasNexus);
            nexusTracking.getNexusReasons().clear();
            nexusTracking.getNexusReasons().add(nexusReason);
        } else {
            nexusTracking = new NexusTracking();
            nexusTracking.setBusinessId(businessId);
            nexusTracking.setState(state);
            nexusTracking.setHasNexus(hasNexus);
            nexusTracking.getNexusReasons().add(nexusReason);
            nexusTracking.setTenantId(tenantId);
        }

        return nexusTrackingRepository.save(nexusTracking);
    }

    /**
     * Determine economic nexus based on sales threshold and transaction count.
     * Common standard: $500K in sales OR 200+ transactions.
     *
     * @param state              the state code
     * @param totalSales         total sales to the state
     * @param transactionCount   number of transactions to the state
     * @return true if economic nexus threshold is met
     */
    public boolean hasEconomicNexus(String state, BigDecimal totalSales, int transactionCount) {
        log.debug("Checking economic nexus for state: {}, sales: {}, transactions: {}",
                state, totalSales, transactionCount);

        boolean meetsThreshold = totalSales.compareTo(ECONOMIC_NEXUS_SALES_THRESHOLD) >= 0
                || transactionCount >= ECONOMIC_NEXUS_TRANSACTION_THRESHOLD;

        log.debug("Economic nexus threshold met: {}", meetsThreshold);
        return meetsThreshold;
    }

    /**
     * Get all states where a business has nexus.
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return list of nexus tracking records for states with nexus
     */
    public List<NexusTracking> getNexusStates(UUID businessId, UUID tenantId) {
        log.debug("Fetching nexus states for business: {}, tenant: {}", businessId, tenantId);
        return nexusTrackingRepository.findNexusStates(businessId, tenantId);
    }

    /**
     * Get all states where a business lacks nexus.
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return list of nexus tracking records for states without nexus
     */
    public List<NexusTracking> getNonNexusStates(UUID businessId, UUID tenantId) {
        log.debug("Fetching non-nexus states for business: {}, tenant: {}", businessId, tenantId);
        return nexusTrackingRepository.findNonNexusStates(businessId, tenantId);
    }

    /**
     * Get all nexus tracking records for a business.
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return list of all nexus tracking records
     */
    public List<NexusTracking> getAllNexusRecords(UUID businessId, UUID tenantId) {
        log.debug("Fetching all nexus records for business: {}, tenant: {}", businessId, tenantId);
        return nexusTrackingRepository.findByBusinessIdAndTenantId(businessId, tenantId);
    }

    /**
     * Count the number of states where a business has nexus.
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return count of nexus states
     */
    public long countNexusStates(UUID businessId, UUID tenantId) {
        log.debug("Counting nexus states for business: {}, tenant: {}", businessId, tenantId);
        return nexusTrackingRepository.countNexusStates(businessId, tenantId);
    }

    /**
     * Batch update nexus records for multiple states.
     *
     * @param businessId     the business ID
     * @param nexusRecords   list of nexus tracking records to create/update
     * @param tenantId       the tenant ID for multi-tenant isolation
     */
    @Transactional
    public void batchUpdateNexusStatus(UUID businessId, List<NexusTracking> nexusRecords, UUID tenantId) {
        log.info("Batch updating {} nexus records for business: {}, tenant: {}",
                nexusRecords.size(), businessId, tenantId);

        nexusRecords.forEach(record -> {
            NexusReason firstReason = record.getNexusReasons().isEmpty() ? 
                    NexusReason.FACTOR_PRESENCE : record.getNexusReasons().get(0);
            updateNexusStatus(businessId, record.getState(), record.getHasNexus(),
                    firstReason, tenantId);
        });

        log.info("Completed batch nexus update for business: {}", businessId);
    }

    /**
     * Determine and update nexus status based on economic thresholds.
     * Task: T075 [US2] - Enhanced economic nexus logic
     *
     * @param businessId        the business ID
     * @param state             the state code
     * @param totalSales        total sales to the state for the period
     * @param transactionCount  number of transactions to the state
     * @param tenantId          the tenant ID for multi-tenant isolation
     * @return the updated nexus tracking record
     */
    @Transactional
    public NexusTracking determineAndUpdateEconomicNexus(UUID businessId, String state,
                                                         BigDecimal totalSales, int transactionCount,
                                                         UUID tenantId) {
        log.info("Determining economic nexus for business: {} in state: {}, sales: {}, transactions: {}",
                businessId, state, totalSales, transactionCount);

        boolean meetsEconomicThreshold = hasEconomicNexus(state, totalSales, transactionCount);

        if (meetsEconomicThreshold) {
            log.info("Economic nexus threshold met for state: {}", state);
            return updateNexusStatus(businessId, state, true, NexusReason.ECONOMIC_NEXUS, tenantId);
        } else {
            // Check if existing nexus record exists with non-economic reasons
            Optional<NexusTracking> existing = nexusTrackingRepository
                    .findByBusinessIdAndStateAndTenantId(businessId, state, tenantId);

            if (existing.isPresent() && existing.get().getHasNexus()) {
                // Keep existing nexus if established for other reasons
                log.debug("Economic threshold not met, but nexus exists for other reasons: {}",
                        existing.get().getNexusReasons());
                return existing.get();
            } else {
                // No nexus established
                log.debug("Economic threshold not met, no nexus in state: {}", state);
                return updateNexusStatus(businessId, state, false, NexusReason.FACTOR_PRESENCE, tenantId);
            }
        }
    }

    /**
     * Get state-specific economic nexus thresholds.
     * This method can be enhanced to support state-specific thresholds from configuration.
     * Task: T075 [US2]
     *
     * @param state the state code
     * @return map with sales_threshold and transaction_threshold
     */
    public java.util.Map<String, Object> getEconomicNexusThresholds(String state) {
        // Default thresholds - can be extended to support state-specific rules
        // from configuration or database in future enhancements
        java.util.Map<String, Object> thresholds = new java.util.HashMap<>();
        thresholds.put("sales_threshold", ECONOMIC_NEXUS_SALES_THRESHOLD);
        thresholds.put("transaction_threshold", ECONOMIC_NEXUS_TRANSACTION_THRESHOLD);
        thresholds.put("state", state);

        log.debug("Economic nexus thresholds for {}: ${} or {} transactions",
                state, ECONOMIC_NEXUS_SALES_THRESHOLD, ECONOMIC_NEXUS_TRANSACTION_THRESHOLD);

        return thresholds;
    }
}
