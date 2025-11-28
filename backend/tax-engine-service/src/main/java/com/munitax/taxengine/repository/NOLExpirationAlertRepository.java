package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.nol.AlertSeverityLevel;
import com.munitax.taxengine.domain.nol.NOLExpirationAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for NOLExpirationAlert entity.
 * Provides data access methods for NOL expiration alert tracking.
 * 
 * Custom Queries:
 * - Find alerts by business (FR-024)
 * - Find alerts by severity level
 * - Find undismissed alerts
 * 
 * @see NOLExpirationAlert
 */
@Repository
public interface NOLExpirationAlertRepository extends JpaRepository<NOLExpirationAlert, UUID> {
    
    /**
     * Find all expiration alerts for a business.
     * Ordered by expiration date (soonest first).
     * 
     * Used for:
     * - Alert display (FR-024)
     * - Expiration report (FR-038)
     * 
     * @param businessId Business profile ID
     * @return List of expiration alerts
     */
    @Query("SELECT a FROM NOLExpirationAlert a WHERE a.businessId = :businessId ORDER BY a.expirationDate ASC")
    List<NOLExpirationAlert> findByBusinessIdOrderByExpirationDateAsc(@Param("businessId") UUID businessId);
    
    /**
     * Find undismissed expiration alerts for a business.
     * 
     * Used for:
     * - Active alert display
     * - Notification system
     * 
     * @param businessId Business profile ID
     * @return List of undismissed alerts
     */
    @Query("SELECT a FROM NOLExpirationAlert a WHERE a.businessId = :businessId AND a.dismissed = false ORDER BY a.severityLevel DESC, a.expirationDate ASC")
    List<NOLExpirationAlert> findUndismissedAlertsByBusinessId(@Param("businessId") UUID businessId);
    
    /**
     * Find alerts by severity level.
     * 
     * Used for:
     * - Critical alert prioritization
     * - Severity-based reporting
     * 
     * @param severityLevel Alert severity level
     * @return List of alerts with severity
     */
    List<NOLExpirationAlert> findBySeverityLevel(AlertSeverityLevel severityLevel);
    
    /**
     * Find alerts by NOL ID.
     * 
     * Used for:
     * - NOL-specific alert lookup
     * - Alert update when NOL is used
     * 
     * @param nolId NOL ID
     * @return List of alerts for NOL
     */
    List<NOLExpirationAlert> findByNolId(UUID nolId);
    
    /**
     * Find critical undismissed alerts across all businesses.
     * 
     * Used for:
     * - System-wide urgent alert monitoring
     * - Administrative reporting
     * 
     * @return List of critical undismissed alerts
     */
    @Query("SELECT a FROM NOLExpirationAlert a WHERE a.severityLevel = 'CRITICAL' AND a.dismissed = false ORDER BY a.expirationDate ASC")
    List<NOLExpirationAlert> findAllCriticalUndismissedAlerts();
}
