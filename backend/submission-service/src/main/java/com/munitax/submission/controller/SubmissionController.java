package com.munitax.submission.controller;

import com.munitax.submission.model.Submission;
import com.munitax.submission.repository.SubmissionRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionRepository repository;

    public SubmissionController(SubmissionRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Submission submitReturn(@RequestBody Submission submission) {
        submission.setId(UUID.randomUUID().toString());
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(Instant.now());
        return repository.save(submission);
    }

    @GetMapping
    public List<Submission> getSubmissions(@RequestParam(required = false) String tenantId) {
        if (tenantId != null) {
            return repository.findByTenantId(tenantId);
        }
        return repository.findAll();
    }

    @PostMapping("/{id}/approve")
    public Submission approveSubmission(@PathVariable String id, @RequestParam String auditorId) {
        Submission submission = repository.findById(id).orElseThrow();
        submission.setStatus("APPROVED");
        submission.setReviewedAt(Instant.now());
        submission.setReviewedBy(auditorId);
        return repository.save(submission);
    }

    @PostMapping("/{id}/reject")
    public Submission rejectSubmission(@PathVariable String id, @RequestParam String auditorId,
            @RequestBody String comments) {
        Submission submission = repository.findById(id).orElseThrow();
        submission.setStatus("REJECTED");
        submission.setAuditorComments(comments);
        submission.setReviewedAt(Instant.now());
        submission.setReviewedBy(auditorId);
        return repository.save(submission);
    }
}
