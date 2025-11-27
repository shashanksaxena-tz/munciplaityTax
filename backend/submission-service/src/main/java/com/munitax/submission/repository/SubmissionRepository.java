package com.munitax.submission.repository;

import com.munitax.submission.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, String> {
    List<Submission> findByTenantId(String tenantId);
    List<Submission> findByUserId(String userId);
}
