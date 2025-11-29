package com.munitax.submission.controller;

import com.munitax.submission.model.*;
import com.munitax.submission.repository.AuditReportRepository;
import com.munitax.submission.service.AuditService;
import com.munitax.submission.service.AuditReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@CrossOrigin(origins = "*")
public class AuditController {
    
    private final AuditService auditService;
    private final AuditReportService auditReportService;
    private final AuditReportRepository auditReportRepository;
    
    public AuditController(
            AuditService auditService,
            AuditReportService auditReportService,
            AuditReportRepository auditReportRepository) {
        this.auditService = auditService;
        this.auditReportService = auditReportService;
        this.auditReportRepository = auditReportRepository;
    }
    
    // ===== Queue Management =====
    
    @GetMapping("/queue")
    public ResponseEntity<Page<AuditQueue>> getQueue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String auditorId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) Long fromDate,
            @RequestParam(required = false) Long toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "submissionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        AuditQueue.AuditStatus auditStatus = status != null ? AuditQueue.AuditStatus.valueOf(status) : null;
        AuditQueue.Priority auditPriority = priority != null ? AuditQueue.Priority.valueOf(priority) : null;
        Instant from = fromDate != null ? Instant.ofEpochMilli(fromDate) : null;
        Instant to = toDate != null ? Instant.ofEpochMilli(toDate) : null;
        
        Sort sort = sortDirection.equalsIgnoreCase("ASC") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AuditQueue> result = auditService.getQueueWithFilters(
            auditStatus, auditPriority, auditorId, tenantId, from, to, pageable);
        
        // Calculate daysInQueue for each item
        result.forEach(queue -> queue.setDaysInQueue(queue.calculateDaysInQueue()));
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/queue/{returnId}")
    public ResponseEntity<AuditQueue> getQueueEntry(@PathVariable String returnId) {
        return auditService.getQueueEntryByReturnId(returnId)
                .map(queue -> {
                    queue.setDaysInQueue(queue.calculateDaysInQueue());
                    return ResponseEntity.ok(queue);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/queue/stats")
    public ResponseEntity<Map<String, Object>> getQueueStats() {
        long pending = auditService.countPendingReturns();
        long highPriority = auditService.countHighPriorityReturns();
        
        return ResponseEntity.ok(Map.of(
            "pending", pending,
            "highPriority", highPriority
        ));
    }
    
    // ===== Auditor Assignment =====
    
    @PostMapping("/assign")
    public ResponseEntity<AuditQueue> assignAuditor(@RequestBody Map<String, String> request) {
        String queueId = request.get("queueId");
        String auditorId = request.get("auditorId");
        String assignedBy = request.get("assignedBy");
        
        AuditQueue queue = auditService.assignAuditor(queueId, auditorId, assignedBy);
        return ResponseEntity.ok(queue);
    }
    
    @PostMapping("/start-review")
    public ResponseEntity<AuditQueue> startReview(@RequestBody Map<String, String> request) {
        String queueId = request.get("queueId");
        String auditorId = request.get("auditorId");
        
        AuditQueue queue = auditService.startReview(queueId, auditorId);
        return ResponseEntity.ok(queue);
    }
    
    @PostMapping("/priority")
    public ResponseEntity<AuditQueue> updatePriority(@RequestBody Map<String, String> request) {
        String queueId = request.get("queueId");
        AuditQueue.Priority priority = AuditQueue.Priority.valueOf(request.get("priority"));
        String userId = request.get("userId");
        
        AuditQueue queue = auditService.updatePriority(queueId, priority, userId);
        return ResponseEntity.ok(queue);
    }
    
    // ===== Approval & Rejection =====
    
    @PostMapping("/approve")
    public ResponseEntity<Map<String, String>> approveReturn(@RequestBody Map<String, String> request) {
        String returnId = request.get("returnId");
        String auditorId = request.get("auditorId");
        String eSignature = request.get("eSignature");
        
        auditService.approveReturn(returnId, auditorId, eSignature);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Return approved successfully"
        ));
    }
    
    @PostMapping("/reject")
    public ResponseEntity<Map<String, String>> rejectReturn(@RequestBody Map<String, Object> request) {
        String returnId = (String) request.get("returnId");
        String auditorId = (String) request.get("auditorId");
        String reason = (String) request.get("reason");
        String detailedExplanation = (String) request.get("detailedExplanation");
        LocalDate resubmitDeadline = LocalDate.parse((String) request.get("resubmitDeadline"));
        
        auditService.rejectReturn(returnId, auditorId, reason, detailedExplanation, resubmitDeadline);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Return rejected successfully"
        ));
    }
    
    // ===== Document Requests =====
    
    @PostMapping("/request-docs")
    public ResponseEntity<DocumentRequest> requestDocuments(@RequestBody Map<String, Object> request) {
        String returnId = (String) request.get("returnId");
        String auditorId = (String) request.get("auditorId");
        DocumentRequest.DocumentType documentType = 
            DocumentRequest.DocumentType.valueOf((String) request.get("documentType"));
        String description = (String) request.get("description");
        LocalDate deadline = LocalDate.parse((String) request.get("deadline"));
        String tenantId = (String) request.get("tenantId");
        
        DocumentRequest docRequest = auditService.createDocumentRequest(
            returnId, auditorId, documentType, description, deadline, tenantId);
        
        return ResponseEntity.ok(docRequest);
    }
    
    @GetMapping("/document-requests/{returnId}")
    public ResponseEntity<List<DocumentRequest>> getDocumentRequests(@PathVariable String returnId) {
        // This would be implemented in the service
        return ResponseEntity.ok(List.of());
    }
    
    @PostMapping("/document-requests/{requestId}/received")
    public ResponseEntity<DocumentRequest> markDocumentReceived(
            @PathVariable String requestId,
            @RequestParam String userId) {
        DocumentRequest request = auditService.markDocumentReceived(requestId, userId);
        return ResponseEntity.ok(request);
    }
    
    @GetMapping("/document-requests/overdue")
    public ResponseEntity<List<DocumentRequest>> getOverdueRequests() {
        List<DocumentRequest> overdue = auditService.getOverdueDocumentRequests();
        return ResponseEntity.ok(overdue);
    }
    
    // ===== Audit Trail & History =====
    
    @GetMapping("/trail/{returnId}")
    public ResponseEntity<List<AuditTrail>> getAuditTrail(@PathVariable String returnId) {
        List<AuditTrail> trail = auditService.getAuditTrail(returnId);
        return ResponseEntity.ok(trail);
    }
    
    @GetMapping("/actions/{returnId}")
    public ResponseEntity<List<AuditAction>> getAuditActions(@PathVariable String returnId) {
        List<AuditAction> actions = auditService.getAuditActions(returnId);
        return ResponseEntity.ok(actions);
    }
    
    // ===== Audit Reports =====
    
    @PostMapping("/report/generate/{returnId}")
    public ResponseEntity<AuditReport> generateAuditReport(@PathVariable String returnId) {
        try {
            AuditReport report = auditReportService.generateAuditReport(returnId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/report/{returnId}")
    public ResponseEntity<AuditReport> getAuditReport(@PathVariable String returnId) {
        return auditReportRepository.findByReturnId(returnId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/reports/high-risk")
    public ResponseEntity<List<AuditReport>> getHighRiskReports() {
        List<AuditReport> reports = auditReportRepository.findHighRiskReports();
        return ResponseEntity.ok(reports);
    }
    
    // ===== Auditor Workload =====
    
    @GetMapping("/workload/{auditorId}")
    public ResponseEntity<List<AuditQueue>> getAuditorWorkload(@PathVariable String auditorId) {
        List<AuditQueue> workload = auditService.getAuditorWorkload(auditorId);
        workload.forEach(queue -> queue.setDaysInQueue(queue.calculateDaysInQueue()));
        return ResponseEntity.ok(workload);
    }
}
