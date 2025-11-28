package com.munitax.submission.service;

import com.munitax.submission.model.*;
import com.munitax.submission.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AuditService {
    
    private final AuditQueueRepository auditQueueRepository;
    private final AuditActionRepository auditActionRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final SubmissionRepository submissionRepository;
    private final DocumentRequestRepository documentRequestRepository;
    
    public AuditService(
            AuditQueueRepository auditQueueRepository,
            AuditActionRepository auditActionRepository,
            AuditTrailRepository auditTrailRepository,
            SubmissionRepository submissionRepository,
            DocumentRequestRepository documentRequestRepository) {
        this.auditQueueRepository = auditQueueRepository;
        this.auditActionRepository = auditActionRepository;
        this.auditTrailRepository = auditTrailRepository;
        this.submissionRepository = submissionRepository;
        this.documentRequestRepository = documentRequestRepository;
    }
    
    // ===== Queue Management =====
    
    public AuditQueue createQueueEntry(String returnId, String tenantId) {
        AuditQueue queue = new AuditQueue();
        queue.setReturnId(returnId);
        queue.setSubmissionDate(Instant.now());
        queue.setStatus(AuditQueue.AuditStatus.PENDING);
        queue.setPriority(AuditQueue.Priority.MEDIUM);
        queue.setTenantId(tenantId);
        queue.setRiskScore(0);
        queue.setFlaggedIssuesCount(0);
        
        AuditQueue saved = auditQueueRepository.save(queue);
        
        // Create audit trail entry
        createTrailEntry(returnId, "SYSTEM", AuditTrail.EventType.SUBMISSION, 
                        "Return submitted and added to audit queue");
        
        return saved;
    }
    
    public Page<AuditQueue> getQueueWithFilters(
            AuditQueue.AuditStatus status,
            AuditQueue.Priority priority,
            String auditorId,
            String tenantId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable) {
        return auditQueueRepository.findWithFilters(
            status, priority, auditorId, tenantId, fromDate, toDate, pageable);
    }
    
    public Optional<AuditQueue> getQueueEntryByReturnId(String returnId) {
        return auditQueueRepository.findByReturnId(returnId);
    }
    
    public AuditQueue updatePriority(String queueId, AuditQueue.Priority newPriority, String userId) {
        AuditQueue queue = auditQueueRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        
        AuditQueue.Priority oldPriority = queue.getPriority();
        queue.setPriority(newPriority);
        AuditQueue saved = auditQueueRepository.save(queue);
        
        // Log action
        createActionEntry(queue.getReturnId(), userId, AuditAction.ActionType.PRIORITY_CHANGED,
                         oldPriority.name(), newPriority.name(), 
                         String.format("Priority changed from %s to %s", oldPriority, newPriority));
        
        return saved;
    }
    
    // ===== Auditor Assignment =====
    
    public AuditQueue assignAuditor(String queueId, String auditorId, String assignedBy) {
        AuditQueue queue = auditQueueRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        
        String previousAuditor = queue.getAssignedAuditorId();
        queue.setAssignedAuditorId(auditorId);
        queue.setAssignmentDate(Instant.now());
        
        if (queue.getStatus() == AuditQueue.AuditStatus.PENDING) {
            queue.setStatus(AuditQueue.AuditStatus.IN_REVIEW);
            queue.setReviewStartedDate(Instant.now());
        }
        
        AuditQueue saved = auditQueueRepository.save(queue);
        
        // Log action
        AuditAction.ActionType actionType = previousAuditor != null 
            ? AuditAction.ActionType.REASSIGNED 
            : AuditAction.ActionType.ASSIGNED;
        
        createActionEntry(queue.getReturnId(), assignedBy, actionType,
                         previousAuditor, auditorId,
                         String.format("Return assigned to auditor %s", auditorId));
        
        createTrailEntry(queue.getReturnId(), assignedBy, AuditTrail.EventType.ASSIGNMENT,
                        String.format("Return assigned to auditor %s", auditorId));
        
        return saved;
    }
    
    public AuditQueue startReview(String queueId, String auditorId) {
        AuditQueue queue = auditQueueRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        
        queue.startReview(auditorId);
        AuditQueue saved = auditQueueRepository.save(queue);
        
        createActionEntry(queue.getReturnId(), auditorId, AuditAction.ActionType.REVIEW_STARTED,
                         "PENDING", "IN_REVIEW", "Review started");
        
        createTrailEntry(queue.getReturnId(), auditorId, AuditTrail.EventType.REVIEW_STARTED,
                        "Auditor started reviewing return");
        
        return saved;
    }
    
    // ===== Approval Workflow =====
    
    public void approveReturn(String returnId, String auditorId, String eSignature) {
        // Update queue
        AuditQueue queue = auditQueueRepository.findByReturnId(returnId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        
        queue.setStatus(AuditQueue.AuditStatus.APPROVED);
        queue.completeReview();
        auditQueueRepository.save(queue);
        
        // Update submission
        Submission submission = submissionRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        
        submission.setStatus("APPROVED");
        submission.setReviewedAt(Instant.now());
        submission.setReviewedBy(auditorId);
        submission.setDigitalSignature(eSignature);
        submissionRepository.save(submission);
        
        // Log action
        createActionEntry(returnId, auditorId, AuditAction.ActionType.APPROVED,
                         "IN_REVIEW", "APPROVED", "Return approved");
        
        // Create audit trail with digital signature
        AuditTrail trail = new AuditTrail(returnId, auditorId, AuditTrail.EventType.APPROVAL,
                                          "Return approved by auditor", eSignature);
        trail.setTenantId(queue.getTenantId());
        auditTrailRepository.save(trail);
    }
    
    // ===== Rejection Workflow =====
    
    public void rejectReturn(String returnId, String auditorId, String reason, 
                           String detailedExplanation, LocalDate resubmitDeadline) {
        // Update queue
        AuditQueue queue = auditQueueRepository.findByReturnId(returnId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        
        queue.setStatus(AuditQueue.AuditStatus.REJECTED);
        queue.completeReview();
        auditQueueRepository.save(queue);
        
        // Update submission
        Submission submission = submissionRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        
        submission.setStatus("REJECTED");
        submission.setReviewedAt(Instant.now());
        submission.setReviewedBy(auditorId);
        submission.setRejectionReason(reason);
        submission.setAuditorComments(detailedExplanation);
        submissionRepository.save(submission);
        
        // Log action
        String actionDetails = String.format("Reason: %s, Explanation: %s, Resubmit by: %s",
                                            reason, detailedExplanation, resubmitDeadline);
        createActionEntry(returnId, auditorId, AuditAction.ActionType.REJECTED,
                         "IN_REVIEW", "REJECTED", actionDetails);
        
        createTrailEntry(returnId, auditorId, AuditTrail.EventType.REJECTION,
                        actionDetails);
    }
    
    // ===== Document Request Management =====
    
    public DocumentRequest createDocumentRequest(String returnId, String auditorId,
                                                 DocumentRequest.DocumentType documentType,
                                                 String description, LocalDate deadline,
                                                 String tenantId) {
        DocumentRequest request = new DocumentRequest();
        request.setReturnId(returnId);
        request.setAuditorId(auditorId);
        request.setDocumentType(documentType);
        request.setDescription(description);
        request.setDeadline(deadline);
        request.setStatus(DocumentRequest.RequestStatus.PENDING);
        request.setTenantId(tenantId);
        
        DocumentRequest saved = documentRequestRepository.save(request);
        
        // Update queue status
        AuditQueue queue = auditQueueRepository.findByReturnId(returnId).orElse(null);
        if (queue != null) {
            queue.setStatus(AuditQueue.AuditStatus.AWAITING_DOCUMENTATION);
            auditQueueRepository.save(queue);
        }
        
        // Log action
        String actionDetails = String.format("Requested: %s - %s, Deadline: %s",
                                            documentType, description, deadline);
        createActionEntry(returnId, auditorId, AuditAction.ActionType.DOCS_REQUESTED,
                         null, null, actionDetails);
        
        createTrailEntry(returnId, auditorId, AuditTrail.EventType.DOCUMENT_REQUEST,
                        actionDetails);
        
        return saved;
    }
    
    public DocumentRequest markDocumentReceived(String requestId, String userId) {
        DocumentRequest request = documentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Document request not found"));
        
        request.markReceived();
        DocumentRequest saved = documentRequestRepository.save(request);
        
        createTrailEntry(request.getReturnId(), userId, AuditTrail.EventType.DOCUMENT_RECEIVED,
                        String.format("Documents received for request: %s", request.getDescription()));
        
        return saved;
    }
    
    // ===== Audit Trail =====
    
    public List<AuditTrail> getAuditTrail(String returnId) {
        return auditTrailRepository.findByReturnIdOrderByTimestampDesc(returnId);
    }
    
    public List<AuditAction> getAuditActions(String returnId) {
        return auditActionRepository.findByReturnIdOrderByActionDateDesc(returnId);
    }
    
    // ===== Helper Methods =====
    
    private void createActionEntry(String returnId, String userId, AuditAction.ActionType actionType,
                                   String previousStatus, String newStatus, String details) {
        AuditAction action = new AuditAction(returnId, userId, actionType, 
                                            previousStatus, newStatus, details);
        auditActionRepository.save(action);
    }
    
    private void createTrailEntry(String returnId, String userId, AuditTrail.EventType eventType,
                                 String details) {
        AuditTrail trail = new AuditTrail(returnId, userId, eventType, details);
        auditTrailRepository.save(trail);
    }
    
    // ===== Statistics & Reporting =====
    
    public long countPendingReturns() {
        return auditQueueRepository.countByStatus(AuditQueue.AuditStatus.PENDING);
    }
    
    public long countHighPriorityReturns() {
        return auditQueueRepository.countByPriority(AuditQueue.Priority.HIGH);
    }
    
    public List<AuditQueue> getAuditorWorkload(String auditorId) {
        return auditQueueRepository.findByAssignedAuditorId(auditorId);
    }
    
    public List<DocumentRequest> getOverdueDocumentRequests() {
        return documentRequestRepository.findOverdueRequests(LocalDate.now(), DocumentRequest.RequestStatus.PENDING);
    }
}
