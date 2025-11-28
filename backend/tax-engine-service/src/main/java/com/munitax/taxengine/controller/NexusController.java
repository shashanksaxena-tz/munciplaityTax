package com.munitax.taxengine.controller;

import com.munitax.taxengine.domain.apportionment.NexusReason;
import com.munitax.taxengine.domain.apportionment.NexusTracking;
import com.munitax.taxengine.dto.NexusStatusDto;
import com.munitax.taxengine.service.NexusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for nexus tracking and management.
 * Provides endpoints for managing nexus status across states.
 * Tasks: T077-T078 [US2]
 */
@Slf4j
@RestController
@RequestMapping("/api/nexus")
@RequiredArgsConstructor
public class NexusController {

    private final NexusService nexusService;

    /**
     * Get nexus status for a business across all states.
     * Task: T077 [US2]
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return list of nexus status by state
     */
    @GetMapping("/{businessId}")
    public ResponseEntity<List<NexusStatusDto>> getNexusStatus(
            @PathVariable UUID businessId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Getting nexus status for business: {}, tenant: {}", businessId, tenantId);

        List<NexusTracking> nexusRecords = nexusService.getAllNexusRecords(businessId, tenantId);
        
        List<NexusStatusDto> statusDtos = nexusRecords.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.info("Retrieved {} nexus records for business: {}", statusDtos.size(), businessId);
        
        return ResponseEntity.ok(statusDtos);
    }

    /**
     * Get nexus status for a business in a specific state.
     * Task: T077 [US2]
     *
     * @param businessId the business ID
     * @param state      the state code (e.g., "OH", "CA", "NY")
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return nexus status for the specified state
     */
    @GetMapping("/{businessId}/state/{state}")
    public ResponseEntity<NexusStatusDto> getNexusStatusByState(
            @PathVariable UUID businessId,
            @PathVariable String state,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Getting nexus status for business: {} in state: {}, tenant: {}", 
                businessId, state, tenantId);

        boolean hasNexus = nexusService.hasNexus(businessId, state, tenantId);
        
        NexusStatusDto statusDto = new NexusStatusDto();
        statusDto.setState(state);
        statusDto.setHasNexus(hasNexus);
        statusDto.setBusinessId(businessId);

        return ResponseEntity.ok(statusDto);
    }

    /**
     * Update nexus status for a business in a state.
     * Task: T078 [US2]
     *
     * @param businessId the business ID
     * @param state      the state code
     * @param hasNexus   whether nexus exists
     * @param reason     the reason for nexus determination
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return updated nexus status
     */
    @PostMapping("/{businessId}/update")
    public ResponseEntity<NexusStatusDto> updateNexusStatus(
            @PathVariable UUID businessId,
            @RequestParam String state,
            @RequestParam boolean hasNexus,
            @RequestParam(required = false, defaultValue = "FACTOR_PRESENCE") NexusReason reason,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Updating nexus status for business: {} in state: {}, hasNexus: {}, reason: {}",
                businessId, state, hasNexus, reason);

        NexusTracking updated = nexusService.updateNexusStatus(
                businessId, state, hasNexus, reason, tenantId);
        
        NexusStatusDto statusDto = convertToDto(updated);
        
        log.info("Nexus status updated successfully for business: {} in state: {}", businessId, state);
        
        return ResponseEntity.ok(statusDto);
    }

    /**
     * Batch update nexus status for multiple states.
     * Task: T078 [US2]
     *
     * @param businessId    the business ID
     * @param nexusStatuses list of nexus status updates
     * @param tenantId      the tenant ID for multi-tenant isolation
     * @return list of updated nexus statuses
     */
    @PostMapping("/{businessId}/batch-update")
    public ResponseEntity<List<NexusStatusDto>> batchUpdateNexusStatus(
            @PathVariable UUID businessId,
            @RequestBody List<NexusStatusDto> nexusStatuses,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Batch updating nexus status for business: {}, {} states, tenant: {}",
                businessId, nexusStatuses.size(), tenantId);

        List<NexusTracking> nexusRecords = nexusStatuses.stream()
                .map(dto -> convertFromDto(dto, businessId, tenantId))
                .collect(Collectors.toList());

        nexusService.batchUpdateNexusStatus(businessId, nexusRecords, tenantId);

        // Retrieve updated records
        List<NexusTracking> updated = nexusService.getAllNexusRecords(businessId, tenantId);
        List<NexusStatusDto> updatedDtos = updated.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.info("Batch nexus update completed for business: {}", businessId);
        
        return ResponseEntity.ok(updatedDtos);
    }

    /**
     * Determine and update economic nexus for a business in a state.
     * Task: T078 [US2]
     *
     * @param businessId        the business ID
     * @param state             the state code
     * @param totalSales        total sales to the state
     * @param transactionCount  number of transactions to the state
     * @param tenantId          the tenant ID for multi-tenant isolation
     * @return updated nexus status with economic nexus determination
     */
    @PostMapping("/{businessId}/economic-nexus")
    public ResponseEntity<NexusStatusDto> determineEconomicNexus(
            @PathVariable UUID businessId,
            @RequestParam String state,
            @RequestParam BigDecimal totalSales,
            @RequestParam int transactionCount,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Determining economic nexus for business: {} in state: {}, sales: {}, transactions: {}",
                businessId, state, totalSales, transactionCount);

        NexusTracking updated = nexusService.determineAndUpdateEconomicNexus(
                businessId, state, totalSales, transactionCount, tenantId);
        
        NexusStatusDto statusDto = convertToDto(updated);
        
        log.info("Economic nexus determined for business: {} in state: {}, hasNexus: {}",
                businessId, state, updated.getHasNexus());
        
        return ResponseEntity.ok(statusDto);
    }

    /**
     * Get economic nexus thresholds for a state.
     * Task: T078 [US2]
     *
     * @param state the state code
     * @return map with sales and transaction thresholds
     */
    @GetMapping("/thresholds/{state}")
    public ResponseEntity<Map<String, Object>> getEconomicNexusThresholds(
            @PathVariable String state) {
        
        log.info("Getting economic nexus thresholds for state: {}", state);

        Map<String, Object> thresholds = nexusService.getEconomicNexusThresholds(state);
        
        return ResponseEntity.ok(thresholds);
    }

    /**
     * Get count of states where business has nexus.
     * Task: T077 [US2]
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return count of nexus states
     */
    @GetMapping("/{businessId}/count")
    public ResponseEntity<Map<String, Long>> countNexusStates(
            @PathVariable UUID businessId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Counting nexus states for business: {}, tenant: {}", businessId, tenantId);

        long count = nexusService.countNexusStates(businessId, tenantId);
        
        return ResponseEntity.ok(Map.of("nexusStateCount", count));
    }

    /**
     * Get all states where business has nexus.
     * Task: T077 [US2]
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return list of states with nexus
     */
    @GetMapping("/{businessId}/nexus-states")
    public ResponseEntity<List<NexusStatusDto>> getNexusStates(
            @PathVariable UUID businessId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Getting nexus states for business: {}, tenant: {}", businessId, tenantId);

        List<NexusTracking> nexusStates = nexusService.getNexusStates(businessId, tenantId);
        
        List<NexusStatusDto> statusDtos = nexusStates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(statusDtos);
    }

    /**
     * Get all states where business lacks nexus.
     * Task: T077 [US2]
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return list of states without nexus
     */
    @GetMapping("/{businessId}/non-nexus-states")
    public ResponseEntity<List<NexusStatusDto>> getNonNexusStates(
            @PathVariable UUID businessId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Getting non-nexus states for business: {}, tenant: {}", businessId, tenantId);

        List<NexusTracking> nonNexusStates = nexusService.getNonNexusStates(businessId, tenantId);
        
        List<NexusStatusDto> statusDtos = nonNexusStates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(statusDtos);
    }

    // Helper methods

    private NexusStatusDto convertToDto(NexusTracking nexusTracking) {
        NexusStatusDto dto = new NexusStatusDto();
        dto.setState(nexusTracking.getState());
        dto.setHasNexus(nexusTracking.getHasNexus());
        dto.setBusinessId(nexusTracking.getBusinessId());
        dto.setNexusReasons(nexusTracking.getNexusReasons());
        dto.setLastUpdated(nexusTracking.getLastModifiedDate());
        return dto;
    }

    private NexusTracking convertFromDto(NexusStatusDto dto, UUID businessId, UUID tenantId) {
        NexusTracking tracking = new NexusTracking();
        tracking.setBusinessId(businessId);
        tracking.setState(dto.getState());
        tracking.setHasNexus(dto.getHasNexus());
        tracking.setNexusReasons(dto.getNexusReasons());
        tracking.setTenantId(tenantId);
        return tracking;
    }
}
