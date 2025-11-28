package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.nol.NOLAmendment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for NOLAmendment entity.
 * Provides data access methods for NOL amendment tracking.
 * 
 * Custom Queries:
 * - Find amendments by NOL ID (FR-040)
 * - Find amendments by original/amended return
 * - Find amendments with cascading effects
 * 
 * @see NOLAmendment
 */
@Repository
public interface NOLAmendmentRepository extends JpaRepository<NOLAmendment, UUID> {
    
    /**
     * Find all amendments for a specific NOL.
     * Ordered by amendment date descending (most recent first).
     * 
     * Used for:
     * - Amendment history display (FR-042)
     * - Multi-amendment tracking
     * 
     * @param nolId NOL ID
     * @return List of amendments
     */
    List<NOLAmendment> findByNolIdOrderByAmendmentDateDesc(UUID nolId);
    
    /**
     * Find amendment by original return ID.
     * 
     * Used for:
     * - Amendment lookup when return is amended
     * - Impact analysis (FR-041)
     * 
     * @param originalReturnId Original return ID
     * @return Optional amendment
     */
    Optional<NOLAmendment> findByOriginalReturnId(UUID originalReturnId);
    
    /**
     * Find amendment by amended return ID.
     * 
     * Used for:
     * - Amendment verification
     * - Return-specific amendment lookup
     * 
     * @param amendedReturnId Amended return ID
     * @return Optional amendment
     */
    Optional<NOLAmendment> findByAmendedReturnId(UUID amendedReturnId);
    
    /**
     * Find amendments with non-empty affected years (cascading effects).
     * 
     * Used for:
     * - Identifying amendments requiring subsequent year amendments (FR-041)
     * - Follow-up tracking
     * 
     * @return List of amendments with cascading effects
     */
    @Query("SELECT a FROM NOLAmendment a WHERE a.affectedYears IS NOT NULL AND a.affectedYears <> '' ORDER BY a.amendmentDate DESC")
    List<NOLAmendment> findAmendmentsWithCascadingEffects();
    
    /**
     * Find amendments for a business (via NOL join).
     * 
     * Used for:
     * - Business-level amendment history
     * - Compliance reporting
     * 
     * @param businessId Business profile ID
     * @return List of amendments ordered by date
     */
    @Query("SELECT a FROM NOLAmendment a JOIN NOL n ON a.nolId = n.id WHERE n.businessId = :businessId ORDER BY a.amendmentDate DESC")
    List<NOLAmendment> findByBusinessIdOrderByAmendmentDateDesc(@Param("businessId") UUID businessId);
}
