package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.apportionment.NexusReason;
import com.munitax.taxengine.domain.apportionment.NexusTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for NexusTracking entity operations.
 * Provides data access methods for tracking business nexus status across states.
 */
@Repository
public interface NexusTrackingRepository extends JpaRepository<NexusTracking, UUID> {

    /**
     * Find all nexus records for a business.
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return List of nexus tracking records for the business
     */
    List<NexusTracking> findByBusinessIdAndTenantId(UUID businessId, UUID tenantId);

    /**
     * Find nexus record for a business in a specific state.
     *
     * @param businessId the business ID
     * @param state      the state code (e.g., "OH", "CA", "NY")
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Optional containing the nexus record if found
     */
    Optional<NexusTracking> findByBusinessIdAndStateAndTenantId(
            UUID businessId, String state, UUID tenantId);

    /**
     * Find all states where a business has nexus.
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return List of nexus tracking records where nexus exists
     */
    @Query("SELECT n FROM NexusTracking n WHERE n.businessId = :businessId " +
           "AND n.hasNexus = true AND n.tenantId = :tenantId")
    List<NexusTracking> findNexusStates(
            @Param("businessId") UUID businessId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find all states where a business lacks nexus.
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return List of nexus tracking records where nexus does not exist
     */
    @Query("SELECT n FROM NexusTracking n WHERE n.businessId = :businessId " +
           "AND n.hasNexus = false AND n.tenantId = :tenantId")
    List<NexusTracking> findNonNexusStates(
            @Param("businessId") UUID businessId,
            @Param("tenantId") UUID tenantId);

    /**
     * Check if a business has nexus in a specific state.
     *
     * @param businessId the business ID
     * @param state      the state code
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return true if the business has nexus in the state
     */
    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM NexusTracking n " +
           "WHERE n.businessId = :businessId AND n.state = :state " +
           "AND n.hasNexus = true AND n.tenantId = :tenantId")
    boolean hasNexusInState(
            @Param("businessId") UUID businessId,
            @Param("state") String state,
            @Param("tenantId") UUID tenantId);

    /**
     * Find nexus records by nexus reason.
     *
     * @param businessId  the business ID
     * @param nexusReason the reason for nexus
     * @param tenantId    the tenant ID for multi-tenant isolation
     * @return List of nexus tracking records with the specified reason
     */
    List<NexusTracking> findByBusinessIdAndNexusReasonAndTenantId(
            UUID businessId, NexusReason nexusReason, UUID tenantId);

    /**
     * Find all nexus records for a tenant.
     *
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return List of all nexus tracking records
     */
    List<NexusTracking> findByTenantId(UUID tenantId);

    /**
     * Count nexus states for a business.
     *
     * @param businessId the business ID
     * @param tenantId   the tenant ID for multi-tenant isolation
     * @return Count of states where business has nexus
     */
    @Query("SELECT COUNT(n) FROM NexusTracking n WHERE n.businessId = :businessId " +
           "AND n.hasNexus = true AND n.tenantId = :tenantId")
    long countNexusStates(
            @Param("businessId") UUID businessId,
            @Param("tenantId") UUID tenantId);
}
