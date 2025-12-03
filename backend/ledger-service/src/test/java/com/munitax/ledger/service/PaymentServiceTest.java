package com.munitax.ledger.service;

import com.munitax.ledger.dto.PaymentAllocation;
import com.munitax.ledger.dto.PaymentRequest;
import com.munitax.ledger.dto.PaymentResponse;
import com.munitax.ledger.enums.PaymentMethod;
import com.munitax.ledger.enums.PaymentStatus;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.model.PaymentTransaction;
import com.munitax.ledger.repository.JournalEntryRepository;
import com.munitax.ledger.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class PaymentServiceTest {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    
    @Autowired
    private JournalEntryRepository journalEntryRepository;
    
    private String tenantId;
    private UUID filerId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID().toString();
        filerId = UUID.randomUUID();
    }
    
    @Test
    void testProcessPayment_CreditCardApproved_CreatesJournalEntries() {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .description("Q1 2024 Tax Payment")
                .build();
        
        // When
        PaymentResponse response = paymentService.processPayment(request);
        
        // Then
        assertNotNull(response);
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertNotNull(response.getTransactionId());
        assertNotNull(response.getProviderTransactionId());
        assertTrue(response.getProviderTransactionId().startsWith("mock_ch_"));
        assertNotNull(response.getJournalEntryId());
        assertTrue(response.isTestMode());
        
        // Verify payment transaction saved
        PaymentTransaction transaction = paymentTransactionRepository
                .findById(response.getTransactionId())
                .orElseThrow();
        assertEquals(filerId, transaction.getFilerId());
        assertEquals(tenantId, transaction.getTenantId());
        assertEquals(new BigDecimal("5000.00"), transaction.getAmount());
        assertEquals(PaymentStatus.APPROVED, transaction.getStatus());
        assertNotNull(transaction.getJournalEntryId());
        
        // Verify journal entries created (2 entries: filer and municipality)
        List<JournalEntry> entries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(transaction.getPaymentId());
        assertEquals(2, entries.size());
        
        // Verify filer entry (debits liability, credits cash)
        JournalEntry filerEntry = entries.stream()
                .filter(e -> e.getEntityId().equals(filerId))
                .findFirst()
                .orElseThrow();
        assertEquals(2, filerEntry.getLines().size());
        assertEquals(new BigDecimal("5000.00"), filerEntry.getTotalDebits());
        assertEquals(new BigDecimal("5000.00"), filerEntry.getTotalCredits());
        
        // Verify municipality entry (debits cash, credits AR)
        JournalEntry municipalityEntry = entries.stream()
                .filter(e -> !e.getEntityId().equals(filerId))
                .findFirst()
                .orElseThrow();
        assertEquals(2, municipalityEntry.getLines().size());
        assertEquals(new BigDecimal("5000.00"), municipalityEntry.getTotalDebits());
        assertEquals(new BigDecimal("5000.00"), municipalityEntry.getTotalCredits());
    }
    
    @Test
    void testProcessPayment_WithAllocation_DistributesCorrectly() {
        // Given - payment split between tax, penalty, and interest
        PaymentAllocation allocation = PaymentAllocation.builder()
                .toTax(new BigDecimal("1000.00"))
                .toPenalty(new BigDecimal("50.00"))
                .toInterest(new BigDecimal("25.00"))
                .build();
        
        PaymentRequest request = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("1075.00"))
                .paymentMethod(PaymentMethod.ACH)
                .achRouting("110000000")
                .achAccount("000123456789")
                .description("Compound payment")
                .allocation(allocation)
                .build();
        
        // When
        PaymentResponse response = paymentService.processPayment(request);
        
        // Then
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        
        // Verify journal entry has correct line items
        PaymentTransaction transaction = paymentTransactionRepository
                .findById(response.getTransactionId())
                .orElseThrow();
        
        List<JournalEntry> entries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(transaction.getPaymentId());
        
        JournalEntry filerEntry = entries.stream()
                .filter(e -> e.getEntityId().equals(filerId))
                .findFirst()
                .orElseThrow();
        
        // Should have 4 lines: tax (2100 debit), penalty (2120 debit), interest (2130 debit), cash (1000 credit)
        assertEquals(4, filerEntry.getLines().size());
        assertEquals(new BigDecimal("1075.00"), filerEntry.getTotalDebits());
        assertEquals(new BigDecimal("1075.00"), filerEntry.getTotalCredits());
    }
    
    @Test
    void testProcessPayment_DeclinedCard_NoJournalEntries() {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4000-0000-0000-0002")
                .description("Q1 2024 Tax Payment")
                .build();
        
        // When
        PaymentResponse response = paymentService.processPayment(request);
        
        // Then
        assertEquals(PaymentStatus.DECLINED, response.getStatus());
        assertEquals("insufficient_funds", response.getFailureReason());
        assertNull(response.getJournalEntryId());
        
        // Verify transaction saved but no journal entries
        PaymentTransaction transaction = paymentTransactionRepository
                .findById(response.getTransactionId())
                .orElseThrow();
        assertNull(transaction.getJournalEntryId());
        
        List<JournalEntry> entries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(transaction.getPaymentId());
        assertEquals(0, entries.size());
    }
    
    @Test
    void testProcessPayment_CheckPayment_CreatesJournalEntries() {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("7500.00"))
                .paymentMethod(PaymentMethod.CHECK)
                .checkNumber("12345")
                .description("Check payment")
                .build();
        
        // When
        PaymentResponse response = paymentService.processPayment(request);
        
        // Then
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertNotNull(response.getJournalEntryId());
        assertTrue(response.getProviderTransactionId().startsWith("mock_manual_"));
        
        // Verify journal entries created
        PaymentTransaction transaction = paymentTransactionRepository
                .findById(response.getTransactionId())
                .orElseThrow();
        
        List<JournalEntry> entries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(transaction.getPaymentId());
        assertEquals(2, entries.size());
    }
    
    @Test
    void testProcessPayment_WireTransfer_CreatesJournalEntries() {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("15000.00"))
                .paymentMethod(PaymentMethod.WIRE)
                .wireConfirmation("WIRE-123456")
                .description("Wire transfer payment")
                .build();
        
        // When
        PaymentResponse response = paymentService.processPayment(request);
        
        // Then
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertNotNull(response.getJournalEntryId());
        
        // Verify correct amounts
        PaymentTransaction transaction = paymentTransactionRepository
                .findById(response.getTransactionId())
                .orElseThrow();
        assertEquals(new BigDecimal("15000.00"), transaction.getAmount());
    }
    
    @Test
    void testGetFilerPayments_ReturnsPaymentHistory() {
        // Given - create multiple payments
        for (int i = 0; i < 3; i++) {
            PaymentRequest request = PaymentRequest.builder()
                    .filerId(filerId)
                    .tenantId(tenantId)
                    .amount(new BigDecimal(String.valueOf(1000 + i * 100)))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .cardNumber("4111-1111-1111-1111")
                    .description("Payment " + (i + 1))
                    .build();
            paymentService.processPayment(request);
        }
        
        // When
        List<PaymentTransaction> payments = paymentService.getFilerPayments(filerId);
        
        // Then
        assertEquals(3, payments.size());
        // Verify descending order by timestamp
        assertTrue(payments.get(0).getTimestamp().isAfter(payments.get(1).getTimestamp())
                || payments.get(0).getTimestamp().isEqual(payments.get(1).getTimestamp()));
    }
    
    @Test
    void testGetPaymentByPaymentId_ReturnsCorrectPayment() {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("3000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .description("Test payment")
                .build();
        PaymentResponse response = paymentService.processPayment(request);
        
        // When
        PaymentTransaction transaction = paymentTransactionRepository
                .findById(response.getTransactionId())
                .orElseThrow();
        PaymentTransaction retrieved = paymentService.getPaymentByPaymentId(transaction.getPaymentId());
        
        // Then
        assertNotNull(retrieved);
        assertEquals(transaction.getPaymentId(), retrieved.getPaymentId());
        assertEquals(filerId, retrieved.getFilerId());
        assertEquals(new BigDecimal("3000.00"), retrieved.getAmount());
    }
    
    @Test
    void testProcessPayment_BalancesLedger() {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("10000.00"))
                .paymentMethod(PaymentMethod.ACH)
                .achRouting("110000000")
                .achAccount("000123456789")
                .description("Large payment")
                .build();
        
        // When
        PaymentResponse response = paymentService.processPayment(request);
        
        // Then - verify double-entry accounting (debits = credits)
        PaymentTransaction transaction = paymentTransactionRepository
                .findById(response.getTransactionId())
                .orElseThrow();
        
        List<JournalEntry> entries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(transaction.getPaymentId());
        
        for (JournalEntry entry : entries) {
            assertEquals(entry.getTotalDebits(), entry.getTotalCredits(),
                    "Journal entry must balance (debits = credits)");
        }
    }
}
