package com.munitax.ledger.service;

import com.munitax.ledger.dto.JournalEntryLineRequest;
import com.munitax.ledger.dto.JournalEntryRequest;
import com.munitax.ledger.dto.RefundRequest;
import com.munitax.ledger.dto.RefundResponse;
import com.munitax.ledger.enums.RefundMethod;
import com.munitax.ledger.enums.RefundStatus;
import com.munitax.ledger.enums.SourceType;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.model.JournalEntryLine;
import com.munitax.ledger.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {
    
    private final JournalEntryService journalEntryService;
    private final AuditLogService auditLogService;
    private final JournalEntryRepository journalEntryRepository;
    
    /**
     * T057: Detect overpayment for a filer
     * FR-036: System MUST detect overpayments (payments > tax liability)
     * 
     * @param tenantId The tenant ID
     * @param filerId The filer ID
     * @return The overpayment amount (positive if overpaid, zero if balanced or underpaid)
     */
    public BigDecimal detectOverpayment(UUID tenantId, UUID filerId) {
        log.info("Detecting overpayment for filer {}", filerId);
        
        // Get all journal entries for the filer
        List<JournalEntry> filerEntries = journalEntryRepository.findByEntityId(filerId);
        
        // Calculate net tax liability (account 2100, 2110, 2120, 2130)
        // Liabilities have credit normal balance, so credits increase, debits decrease
        BigDecimal totalLiability = BigDecimal.ZERO;
        
        for (JournalEntry entry : filerEntries) {
            for (JournalEntryLine line : entry.getLines()) {
                String accountNumber = line.getAccountNumber();
                
                // Tax liability accounts: 2100, 2110, 2120, 2130
                if (accountNumber.equals("2100") || accountNumber.equals("2110") || 
                    accountNumber.equals("2120") || accountNumber.equals("2130")) {
                    // Credits increase liability, debits decrease liability
                    totalLiability = totalLiability
                            .add(line.getCredit())
                            .subtract(line.getDebit());
                }
            }
        }
        
        // If liability is negative, it means overpayment
        BigDecimal overpayment = totalLiability.negate();
        
        log.info("Filer {} total liability: {}, overpayment: {}", 
                filerId, totalLiability, overpayment);
        
        return overpayment.compareTo(BigDecimal.ZERO) > 0 ? overpayment : BigDecimal.ZERO;
    }
    
    /**
     * T058: Process refund request with full validation
     * T059: Support refund approval workflow per FR-039
     * T060: Support refund method selection per FR-041
     * FR-038: System MUST create journal entries for refund
     */
    @Transactional
    public RefundResponse processRefundRequestWithDTO(RefundRequest request) {
        log.info("Processing refund request for filer {}: amount={}, method={}", 
                request.getFilerId(), request.getAmount(), request.getRefundMethod());
        
        // T055: Validate refund amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        
        // T057: Detect overpayment and validate refund amount
        BigDecimal overpayment = detectOverpayment(request.getTenantId(), request.getFilerId());
        
        if (request.getAmount().compareTo(overpayment) > 0) {
            throw new IllegalArgumentException(
                    String.format("Refund amount $%s exceeds overpayment amount $%s",
                            request.getAmount(), overpayment));
        }
        
        // T060: Validate refund method
        if (request.getRefundMethod() == null) {
            request.setRefundMethod(RefundMethod.ACH); // Default to ACH
        }
        
        // Create journal entry
        JournalEntry entry = processRefundRequest(
                request.getTenantId(),
                request.getFilerId(),
                request.getAmount(),
                request.getReason(),
                request.getRequestedBy()
        );
        
        // T042: Generate confirmation number (FR-042)
        String confirmationNumber = "REF-" + LocalDate.now().getYear() + "-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Build response with approval workflow info (T059)
        return RefundResponse.builder()
                .refundId(entry.getSourceId())
                .refundRequestId(entry.getSourceId())
                .filerId(request.getFilerId())
                .amount(request.getAmount())
                .overpaymentAmount(overpayment)
                .status(RefundStatus.REQUESTED) // T059: Initial status
                .refundMethod(request.getRefundMethod())
                .reason(request.getReason())
                .journalEntryId(entry.getEntryId())
                .requestedAt(LocalDateTime.now())
                .requestedBy(request.getRequestedBy())
                .confirmationNumber(confirmationNumber)
                .message("Refund request submitted successfully. Pending approval.")
                .build();
    }
    
    /**
     * T059: Approve refund request
     * FR-039: System MUST support refund approval workflow
     */
    @Transactional
    public RefundResponse approveRefund(UUID tenantId, UUID refundRequestId, UUID approvedBy) {
        log.info("Approving refund request {}", refundRequestId);
        
        // In a real system, we would fetch the refund record from database
        // For now, we'll just log the approval
        
        auditLogService.logAction(
                refundRequestId,
                "REFUND",
                "APPROVED",
                approvedBy,
                tenantId,
                "Refund request approved"
        );
        
        return RefundResponse.builder()
                .refundRequestId(refundRequestId)
                .status(RefundStatus.APPROVED)
                .approvedAt(LocalDateTime.now())
                .approvedBy(approvedBy)
                .message("Refund approved. Ready for issuance.")
                .build();
    }
    
    /**
     * T059: Reject refund request
     * FR-039: System MUST support refund approval workflow
     */
    @Transactional
    public RefundResponse rejectRefund(UUID tenantId, UUID refundRequestId, 
                                      UUID rejectedBy, String rejectionReason) {
        log.info("Rejecting refund request {}: reason={}", refundRequestId, rejectionReason);
        
        auditLogService.logAction(
                refundRequestId,
                "REFUND",
                "REJECTED",
                rejectedBy,
                tenantId,
                "Refund request rejected: " + rejectionReason
        );
        
        return RefundResponse.builder()
                .refundRequestId(refundRequestId)
                .status(RefundStatus.REJECTED)
                .message("Refund rejected: " + rejectionReason)
                .build();
    }
    
    /**
     * T060: Issue refund with method selection
     * FR-040: When refund issued, proper entries created
     * FR-041: System MUST support refund methods (ACH, Check, Wire)
     */
    @Transactional
    public RefundResponse issueRefundWithMethod(UUID tenantId, UUID filerId, 
                                               UUID refundRequestId, BigDecimal refundAmount,
                                               RefundMethod refundMethod, UUID issuedBy) {
        log.info("Issuing refund for filer {} via {}: amount={}", 
                filerId, refundMethod, refundAmount);
        
        // Issue the refund (creates journal entries)
        issueRefund(tenantId, filerId, refundRequestId, refundAmount, issuedBy);
        
        // Generate confirmation number based on method
        String confirmationNumber = generateConfirmationNumber(refundMethod);
        
        // Audit log with method information
        auditLogService.logAction(
                refundRequestId,
                "REFUND",
                "ISSUED_" + refundMethod,
                issuedBy,
                tenantId,
                String.format("Refund issued via %s: amount=%s, confirmation=%s", 
                        refundMethod, refundAmount, confirmationNumber)
        );
        
        return RefundResponse.builder()
                .refundRequestId(refundRequestId)
                .filerId(filerId)
                .amount(refundAmount)
                .status(RefundStatus.ISSUED)
                .refundMethod(refundMethod)
                .issuedAt(LocalDateTime.now())
                .issuedBy(issuedBy)
                .confirmationNumber(confirmationNumber)
                .message(String.format("Refund issued via %s. Confirmation: %s", 
                        refundMethod, confirmationNumber))
                .build();
    }
    
    /**
     * Generate confirmation number based on refund method
     */
    private String generateConfirmationNumber(RefundMethod method) {
        String prefix = switch (method) {
            case ACH -> "ACH";
            case CHECK -> "CHK";
            case WIRE -> "WIRE";
        };
        
        return prefix + "-" + LocalDate.now().getYear() + "-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Legacy method - kept for backward compatibility
     */
    
    @Transactional
    public JournalEntry processRefundRequest(UUID tenantId, UUID filerId, BigDecimal refundAmount, 
                                            String reason, UUID requestedBy) {
        log.info("Processing refund request for filer {}: amount={}", filerId, refundAmount);
        
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        
        // Use a fixed municipality entity ID (deterministic based on tenant ID)
        UUID municipalityEntityId = UUID.nameUUIDFromBytes(
                ("MUNICIPALITY-" + tenantId.toString()).getBytes(StandardCharsets.UTF_8));
        
        // Create filer journal entry for refund request
        JournalEntryRequest filerEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description(String.format("Refund Request - %s", reason))
                .sourceType(SourceType.REFUND)
                .sourceId(UUID.randomUUID()) // Refund request ID
                .tenantId(tenantId)
                .entityId(filerId)
                .createdBy(requestedBy)
                .lines(new ArrayList<>())
                .build();
        
        // Filer: DEBIT Refund Receivable (asset increases), CREDIT Tax Liability (reduces overpayment)
        filerEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1200")
                .debit(refundAmount)
                .credit(BigDecimal.ZERO)
                .description("Refund receivable from municipality")
                .build());
        
        filerEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("2100")
                .debit(BigDecimal.ZERO)
                .credit(refundAmount)
                .description("Reduce overpayment")
                .build());
        
        JournalEntry filerJournalEntry = journalEntryService.createJournalEntry(filerEntry);
        
        // Create municipality journal entry for refund
        JournalEntryRequest municipalityEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description(String.format("Refund to filer %s - %s", filerId, reason))
                .sourceType(SourceType.REFUND)
                .sourceId(filerJournalEntry.getSourceId())
                .tenantId(tenantId)
                .entityId(municipalityEntityId)
                .createdBy(requestedBy)
                .lines(new ArrayList<>())
                .build();
        
        // Municipality: DEBIT Refund Expense (expense increases), CREDIT Refunds Payable (liability increases)
        municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("5200")
                .debit(refundAmount)
                .credit(BigDecimal.ZERO)
                .description("Refund expense")
                .build());
        
        municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("2200")
                .debit(BigDecimal.ZERO)
                .credit(refundAmount)
                .description("Refund payable")
                .build());
        
        journalEntryService.createJournalEntry(municipalityEntry);
        
        // Audit log
        auditLogService.logAction(
                filerJournalEntry.getEntryId(),
                "REFUND",
                "REQUEST",
                requestedBy,
                tenantId,
                String.format("Refund requested: amount=%s, reason=%s", refundAmount, reason)
        );
        
        log.info("Refund request processed successfully for filer {}", filerId);
        return filerJournalEntry;
    }
    
    @Transactional
    public void issueRefund(UUID tenantId, UUID filerId, UUID refundRequestId, 
                           BigDecimal refundAmount, UUID issuedBy) {
        log.info("Issuing refund for filer {}: amount={}", filerId, refundAmount);
        
        // Use a fixed municipality entity ID (deterministic based on tenant ID)
        UUID municipalityEntityId = UUID.nameUUIDFromBytes(
                ("MUNICIPALITY-" + tenantId.toString()).getBytes(StandardCharsets.UTF_8));
        
        // Create filer journal entry for refund issuance
        JournalEntryRequest filerEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description("Refund Issued")
                .sourceType(SourceType.REFUND)
                .sourceId(refundRequestId)
                .tenantId(tenantId)
                .entityId(filerId)
                .createdBy(issuedBy)
                .lines(new ArrayList<>())
                .build();
        
        // Filer: DEBIT Cash (asset increases), CREDIT Refund Receivable (asset decreases)
        filerEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1000")
                .debit(refundAmount)
                .credit(BigDecimal.ZERO)
                .description("Cash refund received")
                .build());
        
        filerEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1200")
                .debit(BigDecimal.ZERO)
                .credit(refundAmount)
                .description("Refund receivable cleared")
                .build());
        
        JournalEntry filerJournalEntry = journalEntryService.createJournalEntry(filerEntry);
        
        // Create municipality journal entry for refund issuance
        JournalEntryRequest municipalityEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description(String.format("Refund Issued to filer %s", filerId))
                .sourceType(SourceType.REFUND)
                .sourceId(refundRequestId)
                .tenantId(tenantId)
                .entityId(municipalityEntityId)
                .createdBy(issuedBy)
                .lines(new ArrayList<>())
                .build();
        
        // Municipality: DEBIT Refunds Payable (liability decreases), CREDIT Cash (asset decreases)
        municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("2200")
                .debit(refundAmount)
                .credit(BigDecimal.ZERO)
                .description("Refund payable cleared")
                .build());
        
        municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1001")
                .debit(BigDecimal.ZERO)
                .credit(refundAmount)
                .description("Cash refund paid")
                .build());
        
        journalEntryService.createJournalEntry(municipalityEntry);
        
        // Audit log
        auditLogService.logAction(
                filerJournalEntry.getEntryId(),
                "REFUND",
                "ISSUED",
                issuedBy,
                tenantId,
                String.format("Refund issued: amount=%s", refundAmount)
        );
        
        log.info("Refund issued successfully for filer {}", filerId);
    }
}
