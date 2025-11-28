package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.nol.NOLSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for NOLSchedule entity.
 * Provides data access methods for NOL schedule tracking.
 * 
 * Custom Queries:
 * - Find schedule by tax return (FR-004)
 * - Find schedule by business and year
 * - Find prior year schedule for balance carryforward
 * 
 * @see NOLSchedule
 */
@Repository
public interface NOLScheduleRepository extends JpaRepository<NOLSchedule, UUID> {
    
    /**
     * Find NOL schedule for a specific tax return.
     * 
     * Used for:
     * - Schedule display on return (FR-004)
     * - Form generation (FR-036)
     * 
     * @param returnId Tax return ID
     * @return Optional NOL schedule
     */
    Optional<NOLSchedule> findByReturnId(UUID returnId);
    
    /**
     * Find NOL schedule for a business in a specific tax year.
     * 
     * Used for:
     * - Year-specific schedule lookup
     * - Multi-year comparison
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year
     * @return Optional NOL schedule
     */
    Optional<NOLSchedule> findByBusinessIdAndTaxYear(UUID businessId, Integer taxYear);
    
    /**
     * Find all NOL schedules for a business, ordered by tax year.
     * 
     * Used for:
     * - Multi-year schedule display
     * - Historical tracking (FR-001)
     * 
     * @param businessId Business profile ID
     * @return List of NOL schedules ordered by year
     */
    List<NOLSchedule> findByBusinessIdOrderByTaxYearAsc(UUID businessId);
    
    /**
     * Find prior year NOL schedule for balance carryforward.
     * 
     * Used for:
     * - Beginning balance calculation (FR-045)
     * - Year-to-year reconciliation
     * 
     * @param businessId Business profile ID
     * @param taxYear Current tax year
     * @return Optional prior year schedule
     */
    @Query("SELECT s FROM NOLSchedule s WHERE s.businessId = :businessId AND s.taxYear = :taxYear - 1")
    Optional<NOLSchedule> findPriorYearSchedule(@Param("businessId") UUID businessId, @Param("taxYear") Integer taxYear);
}
