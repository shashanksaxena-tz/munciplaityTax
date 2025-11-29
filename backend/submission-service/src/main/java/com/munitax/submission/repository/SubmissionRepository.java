package com.munitax.submission.repository;

import com.munitax.submission.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, String> {
    List<Submission> findByTenantId(String tenantId);
    List<Submission> findByUserId(String userId);
    
    /**
     * Find most recent prior year submission for a taxpayer
     * Uses method name query derivation for proper limit and ordering
     */
    Optional<Submission> findFirstByTaxpayerIdAndTaxYearOrderByFiledDateDesc(String taxpayerId, int taxYear);
}
