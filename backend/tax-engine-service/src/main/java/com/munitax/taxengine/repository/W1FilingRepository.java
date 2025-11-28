package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.withholding.W1Filing;
import com.munitax.taxengine.domain.withholding.W1FilingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for W1Filing entity.
 * Provides data access methods for W-1 withholding return filings.
 * 
 * Custom Queries:
 * - Find all W-1 filings for business + tax year (FR-001)
 * - Find filings by status (OVERDUE for penalty calculations)
 * - Find filings due after date (for late filing detection)
 * - Check if business has filed for specific period (duplicate prevention)
 * 
 * @see W1Filing
 */
@Repository
public interface W1FilingRepository extends JpaRepository<W1Filing, UUID> {
    
    /**
     * Find all W-1 filings for a business in a specific tax year.
     * Ordered by period end date ascending (Q1 → Q2 → Q3 → Q4).
     * 
     * Used for:
     * - Filing history display (FR-001)
     * - Cumulative totals calculation (FR-002)
     * - Year-end reconciliation (FR-006)
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year (e.g., 2024)
     * @return List of W-1 filings ordered by period
     */
    @Query("SELECT w FROM W1Filing w WHERE w.businessId = :businessId AND w.taxYear = :taxYear AND w.isAmended = false ORDER BY w.periodEndDate ASC")
    List<W1Filing> findByBusinessIdAndTaxYear(@Param("businessId") UUID businessId, @Param("taxYear") Integer taxYear);
    
    /**
     * Find all W-1 filings for a business in a tax year including amended filings.
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year
     * @return List of all filings (original + amended)
     */
    List<W1Filing> findByBusinessIdAndTaxYearOrderByPeriodEndDateAsc(UUID businessId, Integer taxYear);
    
    /**
     * Check if a W-1 filing exists for business + tax year + period.
     * Prevents duplicate filing for same period (unique constraint).
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year
     * @param period Period identifier (Q1, Q2, M01, etc.)
     * @param isAmended false to check original filings only
     * @return true if filing exists
     */
    boolean existsByBusinessIdAndTaxYearAndPeriodAndIsAmended(UUID businessId, Integer taxYear, String period, Boolean isAmended);
    
    /**
     * Find W-1 filing by business + tax year + period.
     * Used for amendment lookup.
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year
     * @param period Period identifier
     * @param isAmended false for original, true for amended
     * @return Optional W-1 filing
     */
    Optional<W1Filing> findByBusinessIdAndTaxYearAndPeriodAndIsAmended(UUID businessId, Integer taxYear, String period, Boolean isAmended);
    
    /**
     * Find all W-1 filings by status.
     * Used for penalty calculation (OVERDUE filings).
     * 
     * @param status W1FilingStatus
     * @return List of filings with given status
     */
    List<W1Filing> findByStatus(W1FilingStatus status);
    
    /**
     * Find all W-1 filings due after a specific date and not yet paid.
     * Used for late filing detection and penalty calculation.
     * 
     * @param dueDate Due date threshold
     * @return List of overdue filings
     */
    @Query("SELECT w FROM W1Filing w WHERE w.dueDate < :dueDate AND w.status IN ('FILED', 'OVERDUE')")
    List<W1Filing> findOverdueFilings(@Param("dueDate") LocalDate dueDate);
    
    /**
     * Find all W-1 filings for a business after a specific period end date.
     * Used for cascade updates when W-1 is amended (Research R3).
     * 
     * Example: If March W-1 is amended, update cumulative totals for April, May, June, etc.
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year
     * @param periodEndDate Period end date threshold
     * @return List of filings after period
     */
    @Query("SELECT w FROM W1Filing w WHERE w.businessId = :businessId AND w.taxYear = :taxYear AND w.periodEndDate > :periodEndDate AND w.isAmended = false ORDER BY w.periodEndDate ASC")
    List<W1Filing> findFilingsAfterPeriod(@Param("businessId") UUID businessId, @Param("taxYear") Integer taxYear, @Param("periodEndDate") LocalDate periodEndDate);
    
    /**
     * Count number of W-1 filings for business + tax year.
     * Used for filing frequency validation and cumulative totals.
     * 
     * @param businessId Business profile ID
     * @param taxYear Tax year
     * @param isAmended false to count original filings only
     * @return Count of filings
     */
    long countByBusinessIdAndTaxYearAndIsAmended(UUID businessId, Integer taxYear, Boolean isAmended);
}
