package com.munitax.ledger.service;

import com.munitax.ledger.dto.*;
import com.munitax.ledger.enums.PaymentStatus;
import com.munitax.ledger.enums.SourceType;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.model.PaymentTransaction;
import com.munitax.ledger.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final MockPaymentProviderService paymentProvider;
    private final JournalEntryService journalEntryService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditLogService auditLogService;
    
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for filer {} amount {}", request.getFilerId(), request.getAmount());
        
        // Process payment through mock provider
        PaymentResponse providerResponse = paymentProvider.processPayment(request);
        
        // Save payment transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionId(providerResponse.getTransactionId())
                .paymentId(UUID.randomUUID())
                .filerId(request.getFilerId())
                .tenantId(request.getTenantId())
                .providerTransactionId(providerResponse.getProviderTransactionId())
                .status(providerResponse.getStatus())
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .currency("USD")
                .authorizationCode(providerResponse.getAuthorizationCode())
                .failureReason(providerResponse.getFailureReason())
                .timestamp(providerResponse.getTimestamp())
                .isTestMode(providerResponse.isTestMode())
                .build();
        
        paymentTransactionRepository.save(transaction);
        
        // If payment approved, create journal entries
        if (providerResponse.getStatus() == PaymentStatus.APPROVED) {
            try {
                JournalEntry entry = createPaymentJournalEntries(request, transaction);
                transaction.setJournalEntryId(entry.getEntryId());
                paymentTransactionRepository.save(transaction);
                providerResponse.setJournalEntryId(entry.getEntryId());
            } catch (Exception e) {
                log.error("Failed to create journal entries for payment", e);
                // Rethrow to trigger transaction rollback
                throw new RuntimeException("Failed to record in ledger: " + e.getMessage(), e);
            }
        }
        
        // Audit log
        auditLogService.logAction(
                transaction.getTransactionId(),
                "PAYMENT",
                providerResponse.getStatus().name(),
                request.getFilerId(),
                request.getTenantId(),
                String.format("Payment %s: amount=%s, method=%s", 
                        providerResponse.getStatus(), request.getAmount(), request.getPaymentMethod())
        );
        
        return providerResponse;
    }
    
    private JournalEntry createPaymentJournalEntries(PaymentRequest request, PaymentTransaction transaction) {
        // Default allocation if not specified
        PaymentAllocation allocation = request.getAllocation();
        if (allocation == null) {
            allocation = PaymentAllocation.builder()
                    .toTax(request.getAmount())
                    .toPenalty(BigDecimal.ZERO)
                    .toInterest(BigDecimal.ZERO)
                    .build();
        }
        
        // Use a fixed municipality entity ID (deterministic based on tenant ID)
        // PRODUCTION NOTE: In production, municipality entity ID should be stored in tenant
        // configuration and retrieved from the tenant service. This deterministic approach
        // ensures consistency during development and testing.
        String municipalityEntityId = UUID.nameUUIDFromBytes(
                ("MUNICIPALITY-" + request.getTenantId()).getBytes(StandardCharsets.UTF_8)).toString();
        
        // Create filer journal entry (payment reduces liability)
        JournalEntryRequest filerEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description(String.format("Payment via %s - %s", 
                        request.getPaymentMethod(), request.getDescription()))
                .sourceType(SourceType.PAYMENT)
                .sourceId(transaction.getPaymentId())
                .tenantId(request.getTenantId())
                .entityId(request.getFilerId().toString())
                .createdBy(request.getFilerId())
                .lines(new ArrayList<>())
                .build();
        
        // Filer: DEBIT Tax Liability (reduces liability), CREDIT Cash (reduces asset)
        if (allocation.getToTax().compareTo(BigDecimal.ZERO) > 0) {
            filerEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("2100")
                    .debit(allocation.getToTax())
                    .credit(BigDecimal.ZERO)
                    .description("Payment applied to tax liability")
                    .build());
        }
        
        if (allocation.getToPenalty().compareTo(BigDecimal.ZERO) > 0) {
            filerEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("2120")
                    .debit(allocation.getToPenalty())
                    .credit(BigDecimal.ZERO)
                    .description("Payment applied to penalty")
                    .build());
        }
        
        if (allocation.getToInterest().compareTo(BigDecimal.ZERO) > 0) {
            filerEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("2130")
                    .debit(allocation.getToInterest())
                    .credit(BigDecimal.ZERO)
                    .description("Payment applied to interest")
                    .build());
        }
        
        // Credit Cash
        filerEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1000")
                .debit(BigDecimal.ZERO)
                .credit(request.getAmount())
                .description("Cash payment")
                .build());
        
        // Create filer entry
        JournalEntry filerJournalEntry = journalEntryService.createJournalEntry(filerEntry);
        
        // Create municipality journal entry (payment increases cash, reduces AR)
        JournalEntryRequest municipalityEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description(String.format("Payment received from filer %s", request.getFilerId()))
                .sourceType(SourceType.PAYMENT)
                .sourceId(transaction.getPaymentId())
                .tenantId(request.getTenantId())
                .entityId(municipalityEntityId)
                .createdBy(request.getFilerId())
                .lines(new ArrayList<>())
                .build();
        
        // Municipality: DEBIT Cash (increases asset), CREDIT AR (reduces asset)
        municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1001")
                .debit(request.getAmount())
                .credit(BigDecimal.ZERO)
                .description("Cash received")
                .build());
        
        municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1201")
                .debit(BigDecimal.ZERO)
                .credit(request.getAmount())
                .description("Accounts receivable collected")
                .build());
        
        journalEntryService.createJournalEntry(municipalityEntry);
        
        return filerJournalEntry;
    }
    
    public List<PaymentTransaction> getFilerPayments(UUID filerId) {
        return paymentTransactionRepository.findByFilerIdOrderByTimestampDesc(filerId);
    }
    
    public PaymentTransaction getPaymentByPaymentId(UUID paymentId) {
        return paymentTransactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }
    
    public PaymentReceipt generatePaymentReceipt(UUID paymentId) {
        PaymentTransaction payment = getPaymentByPaymentId(paymentId);
        
        // Get journal entry if exists
        String journalEntryNumber = null;
        if (payment.getJournalEntryId() != null) {
            JournalEntry entry = journalEntryService.getEntryById(payment.getJournalEntryId());
            journalEntryNumber = entry.getEntryNumber();
        }
        
        return PaymentReceipt.builder()
                .paymentId(payment.getPaymentId())
                .transactionId(payment.getTransactionId())
                .receiptNumber(generateReceiptNumber(payment))
                .providerTransactionId(payment.getProviderTransactionId())
                .authorizationCode(payment.getAuthorizationCode())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .paymentDate(payment.getTimestamp())
                .filerId(payment.getFilerId())
                .tenantId(payment.getTenantId())
                .description(String.format("Payment via %s", payment.getPaymentMethod()))
                .journalEntryId(payment.getJournalEntryId())
                .journalEntryNumber(journalEntryNumber)
                .testMode(Boolean.TRUE.equals(payment.getIsTestMode()))
                .build();
    }
    
    private String generateReceiptNumber(PaymentTransaction payment) {
        // Generate receipt number if not already set
        // Format: RCPT-YYYYMMDD-XXXXX
        if (payment.getTimestamp() != null) {
            String dateStr = payment.getTimestamp().toLocalDate().toString().replace("-", "");
            String uniqueId = payment.getTransactionId().toString().substring(0, 8).toUpperCase();
            return String.format("RCPT-%s-%s", dateStr, uniqueId);
        }
        return "RCPT-" + payment.getTransactionId().toString().substring(0, 8).toUpperCase();
    }
}
