package com.munitax.rules.repository;

import com.munitax.rules.model.ApprovalStatus;
import com.munitax.rules.model.RuleCategory;
import com.munitax.rules.model.TaxRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TaxRule entity.
 * Provides database access methods with temporal query support.
 */
@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, UUID> {
    
    /**
     * Find all approved rules active on a specific date for a given tenant.
     * This is the primary query used by tax calculators.
     * 
     * @param tenantId Tenant identifier
     * @param asOfDate Date to check (typically tax year or today)
     * @return List of active rules
     */
    @Query("""
        SELECT r FROM TaxRule r
        WHERE r.tenantId = :tenantId
        AND r.approvalStatus = 'APPROVED'
        AND r.effectiveDate <= :asOfDate
        AND (r.endDate IS NULL OR r.endDate >= :asOfDate)
        ORDER BY r.category, r.ruleCode
    """)
    List<TaxRule> findActiveRules(
        @Param("tenantId") String tenantId,
        @Param("asOfDate") LocalDate asOfDate
    );
    
    /**
     * Find active rules filtered by category.
     * 
     * @param tenantId Tenant identifier
     * @param category Rule category
     * @param asOfDate Date to check
     * @return List of active rules in category
     */
    @Query("""
        SELECT r FROM TaxRule r
        WHERE r.tenantId = :tenantId
        AND r.category = :category
        AND r.approvalStatus = 'APPROVED'
        AND r.effectiveDate <= :asOfDate
        AND (r.endDate IS NULL OR r.endDate >= :asOfDate)
        ORDER BY r.ruleCode
    """)
    List<TaxRule> findActiveRulesByCategory(
        @Param("tenantId") String tenantId,
        @Param("category") RuleCategory category,
        @Param("asOfDate") LocalDate asOfDate
    );
    
    /**
     * Find rules by code and tenant (for conflict detection).
     * 
     * @param ruleCode Rule code
     * @param tenantId Tenant identifier
     * @param approvalStatus Approval status filter
     * @return List of matching rules
     */
    List<TaxRule> findByRuleCodeAndTenantIdAndApprovalStatus(
        String ruleCode,
        String tenantId,
        ApprovalStatus approvalStatus
    );
    
    /**
     * Find overlapping rules for conflict detection.
     * Checks if any approved rules exist with overlapping date ranges.
     * 
     * @param ruleCode Rule code
     * @param tenantId Tenant identifier
     * @param effectiveDate New rule effective date
     * @param endDate New rule end date (nullable)
     * @param excludeRuleId Rule ID to exclude (for updates)
     * @return List of overlapping rules
     */
    @Query("""
        SELECT r FROM TaxRule r
        WHERE r.ruleCode = :ruleCode
        AND r.tenantId = :tenantId
        AND r.approvalStatus = 'APPROVED'
        AND r.ruleId != :excludeRuleId
        AND ( :endDate IS NULL OR r.effectiveDate <= :endDate )
        AND ( r.endDate IS NULL OR r.endDate >= :effectiveDate )
    """)
    List<TaxRule> findOverlappingRules(
        @Param("ruleCode") String ruleCode,
        @Param("tenantId") String tenantId,
        @Param("effectiveDate") LocalDate effectiveDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeRuleId") UUID excludeRuleId
    );
    
    /**
     * Find rules pending approval.
     * 
     * @return List of pending rules
     */
    List<TaxRule> findByApprovalStatusOrderByCreatedDateDesc(ApprovalStatus approvalStatus);
    
    /**
     * Find rules by tenant ID.
     * 
     * @param tenantId Tenant identifier
     * @return List of all rules for tenant
     */
    List<TaxRule> findByTenantIdOrderByCreatedDateDesc(String tenantId);
    
    /**
     * Find rule by code, tenant, and date (single match expected).
     * 
     * @param ruleCode Rule code
     * @param tenantId Tenant identifier
     * @param asOfDate Date to check
     * @return Optional containing matching rule
     */
    @Query("""
        SELECT r FROM TaxRule r
        WHERE r.ruleCode = :ruleCode
        AND r.tenantId = :tenantId
        AND r.approvalStatus = 'APPROVED'
        AND r.effectiveDate <= :asOfDate
        AND (r.endDate IS NULL OR r.endDate >= :asOfDate)
    """)
    Optional<TaxRule> findActiveRuleByCode(
        @Param("ruleCode") String ruleCode,
        @Param("tenantId") String tenantId,
        @Param("asOfDate") LocalDate asOfDate
    );
    
    /**
     * Find rules by tenant ID and category.
     */
    List<TaxRule> findByTenantIdAndCategory(String tenantId, RuleCategory category);
    
    /**
     * Find rules by tenant ID.
     */
    List<TaxRule> findByTenantId(String tenantId);
    
    /**
     * Find rules by tenant ID and effective date after.
     */
    List<TaxRule> findByTenantIdAndEffectiveDateGreaterThan(String tenantId, LocalDate fromDate);
    
    /**
     * Find rules by rule code and tenant ordered by version descending.
     */
    List<TaxRule> findByRuleCodeAndTenantIdOrderByVersionDesc(String ruleCode, String tenantId);
    
    /**
     * Find rules by rule code and tenant.
     */
    List<TaxRule> findByRuleCodeAndTenantId(String ruleCode, String tenantId);
}
