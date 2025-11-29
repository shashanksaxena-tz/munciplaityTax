package com.munitax.rules.repository;

import com.munitax.rules.model.ChangeType;
import com.munitax.rules.model.RuleChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for RuleChangeLog entity.
 * Provides access to immutable audit trail.
 */
@Repository
public interface RuleChangeLogRepository extends JpaRepository<RuleChangeLog, UUID> {
    
    /**
     * Find all changes for a specific rule, ordered by date descending.
     * 
     * @param ruleId Rule identifier
     * @return List of change log entries
     */
    List<RuleChangeLog> findByRuleIdOrderByChangeDateDesc(UUID ruleId);
    
    /**
     * Find changes by user, ordered by date descending.
     * 
     * @param changedBy User identifier
     * @return List of change log entries
     */
    List<RuleChangeLog> findByChangedByOrderByChangeDateDesc(String changedBy);
    
    /**
     * Find changes within a date range (for audit reports).
     * 
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of change log entries
     */
    @Query("""
        SELECT l FROM RuleChangeLog l
        WHERE l.changeDate >= :startDate
        AND l.changeDate <= :endDate
        ORDER BY l.changeDate DESC
    """)
    List<RuleChangeLog> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find changes by type.
     * 
     * @param changeType Type of change
     * @return List of change log entries
     */
    List<RuleChangeLog> findByChangeTypeOrderByChangeDateDesc(ChangeType changeType);
    
    /**
     * Find recent changes (last N days).
     * 
     * @param cutoffDate Cutoff date (changes after this date)
     * @return List of recent change log entries
     */
    @Query("""
        SELECT l FROM RuleChangeLog l
        WHERE l.changeDate >= :cutoffDate
        ORDER BY l.changeDate DESC
    """)
    List<RuleChangeLog> findRecentChanges(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Count changes for a specific rule.
     * 
     * @param ruleId Rule identifier
     * @return Number of changes
     */
    long countByRuleId(UUID ruleId);
}
