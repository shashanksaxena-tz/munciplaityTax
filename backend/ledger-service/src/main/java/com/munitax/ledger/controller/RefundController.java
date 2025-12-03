package com.munitax.ledger.controller;

import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Slf4j
public class RefundController {
    
    private final RefundService refundService;
    
    @PostMapping("/request")
    public ResponseEntity<JournalEntry> requestRefund(
            @RequestParam String tenantId,
            @RequestParam UUID filerId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason,
            @RequestParam UUID requestedBy) {
        
        log.info("Refund request received for filer {}: amount={}", filerId, amount);
        JournalEntry entry = refundService.processRefundRequest(tenantId, filerId, amount, reason, requestedBy);
        return ResponseEntity.ok(entry);
    }
    
    @PostMapping("/issue")
    public ResponseEntity<String> issueRefund(
            @RequestParam String tenantId,
            @RequestParam UUID filerId,
            @RequestParam UUID refundRequestId,
            @RequestParam BigDecimal amount,
            @RequestParam UUID issuedBy) {
        
        log.info("Issuing refund for filer {}: amount={}", filerId, amount);
        refundService.issueRefund(tenantId, filerId, refundRequestId, amount, issuedBy);
        return ResponseEntity.ok("Refund issued successfully");
    }
}
