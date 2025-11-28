package com.munitax.taxengine.dto;

import com.munitax.taxengine.domain.apportionment.NexusReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for nexus status information across states.
 * Tracks where the business has nexus and why.
 * Enhanced for T077-T078 [US2]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NexusStatusDto {

    /**
     * The business ID.
     */
    private UUID businessId;

    /**
     * The state code (e.g., "OH", "CA", "NY").
     */
    private String state;

    /**
     * Whether the business has nexus in this state.
     */
    private Boolean hasNexus;

    /**
     * List of reasons for nexus determination.
     */
    private List<NexusReason> nexusReasons;

    /**
     * Map of state code to nexus status (true = has nexus, false = no nexus).
     */
    private Map<String, Boolean> nexusByState;

    /**
     * Map of state code to nexus reason.
     */
    private Map<String, NexusReason> nexusReasonByState;

    /**
     * Map of state code to nexus determination date.
     */
    private Map<String, LocalDateTime> nexusDeterminationDateByState;

    /**
     * List of states where business has nexus.
     */
    private List<String> nexusStates;

    /**
     * List of states where business lacks nexus.
     */
    private List<String> nonNexusStates;

    /**
     * Total count of states with nexus.
     */
    private Integer nexusStateCount;

    /**
     * Whether business has economic nexus in any state.
     */
    private Boolean hasEconomicNexus;

    /**
     * Whether business has physical presence nexus in any state.
     */
    private Boolean hasPhysicalPresenceNexus;

    /**
     * Whether business has factor presence nexus in any state.
     */
    private Boolean hasFactorPresenceNexus;

    /**
     * Map of state code to additional nexus details/notes.
     */
    private Map<String, String> nexusDetails;

    /**
     * Last nexus determination update timestamp.
     */
    private LocalDateTime lastUpdated;
}
