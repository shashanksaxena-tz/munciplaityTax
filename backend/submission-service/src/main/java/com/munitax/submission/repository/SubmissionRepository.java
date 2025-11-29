package com.munitax.submission.repository;

import com.munitax.submission.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, String> {
    List<Submission> findByTenantId(String tenantId);
    List<Submission> findByUserId(String userId);
    
    /**
     * Find prior year submission for a taxpayer
     */
    @Query("SELECT s FROM Submission s WHERE s.taxpayerId = :taxpayerId AND s.taxYear = :taxYear ORDER BY s.filedDate DESC")
    Optional<Submission> findPriorYearSubmission(@Param("taxpayerId") String taxpayerId, @Param("taxYear") int taxYear);
}
