package com.munitax.submission.repository;

import com.munitax.submission.model.SubmissionDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionDocumentRepository extends JpaRepository<SubmissionDocument, String> {
    List<SubmissionDocument> findBySubmissionId(String submissionId);
    List<SubmissionDocument> findBySubmissionIdAndTenantId(String submissionId, String tenantId);
}
