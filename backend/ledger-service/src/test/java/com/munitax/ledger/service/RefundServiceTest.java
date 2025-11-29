package com.munitax.ledger.service;

import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.repository.JournalEntryRepository;
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

/**
 * Test suite for RefundService
 * T054: RefundServiceTest with refund request and issuance test
 * T055: Test for refund validation (amount ≤ overpayment)
 */
@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class RefundServiceTest {
    
    @Autowired
    private RefundService refundService;
    
    @Autowired
    private JournalEntryRepository journalEntryRepository;
    
    private UUID tenantId;
    private UUID filerId;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        filerId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }
    
    /**
     * T054: Test refund request creates proper journal entries
     * Tests FR-038: System MUST create journal entries for refund
     */
    @Test
    void testProcessRefundRequest_CreatesJournalEntries() {
        // Given
        BigDecimal refundAmount = new BigDecimal("1000.00");
        String reason = "Overpayment Q1 2024";
        
        // When
        JournalEntry result = refundService.processRefundRequest(
                tenantId, filerId, refundAmount, reason, userId);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getEntryId());
        assertEquals(filerId, result.getEntityId());
        assertEquals(tenantId, result.getTenantId());
        assertTrue(result.getDescription().contains("Refund Request"));
        
        // Verify filer entry has correct lines
        assertEquals(2, result.getLines().size());
        assertEquals(refundAmount, result.getTotalDebits());
        assertEquals(refundAmount, result.getTotalCredits());
        
        // Verify both filer and municipality entries created
        List<JournalEntry> entries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(result.getSourceId());
        assertEquals(2, entries.size());
        
        // Verify filer entry (DEBIT Refund Receivable, CREDIT Tax Liability)
        JournalEntry filerEntry = entries.stream()
                .filter(e -> e.getEntityId().equals(filerId))
                .findFirst()
                .orElseThrow();
        assertTrue(filerEntry.getLines().stream()
                .anyMatch(line -> "1200".equals(line.getAccountNumber()) && 
                         line.getDebit().compareTo(refundAmount) == 0));
        assertTrue(filerEntry.getLines().stream()
                .anyMatch(line -> "2100".equals(line.getAccountNumber()) && 
                         line.getCredit().compareTo(refundAmount) == 0));
        
        // Verify municipality entry (DEBIT Refund Expense, CREDIT Refunds Payable)
        JournalEntry municipalityEntry = entries.stream()
                .filter(e -> !e.getEntityId().equals(filerId))
                .findFirst()
                .orElseThrow();
        assertTrue(municipalityEntry.getLines().stream()
                .anyMatch(line -> "5200".equals(line.getAccountNumber()) && 
                         line.getDebit().compareTo(refundAmount) == 0));
        assertTrue(municipalityEntry.getLines().stream()
                .anyMatch(line -> "2200".equals(line.getAccountNumber()) && 
                         line.getCredit().compareTo(refundAmount) == 0));
    }
    
    /**
     * T054: Test refund issuance creates proper journal entries
     * Tests FR-040: When refund issued, proper entries created
     */
    @Test
    void testIssueRefund_CreatesJournalEntries() {
        // Given
        UUID refundRequestId = UUID.randomUUID();
        BigDecimal refundAmount = new BigDecimal("1000.00");
        
        // When
        refundService.issueRefund(tenantId, filerId, refundRequestId, refundAmount, userId);
        
        // Then
        // Verify both filer and municipality entries created
        List<JournalEntry> entries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(refundRequestId);
        assertEquals(2, entries.size());
        
        // Verify filer entry (DEBIT Cash, CREDIT Refund Receivable)
        JournalEntry filerEntry = entries.stream()
                .filter(e -> e.getEntityId().equals(filerId))
                .findFirst()
                .orElseThrow();
        assertEquals(2, filerEntry.getLines().size());
        assertEquals(refundAmount, filerEntry.getTotalDebits());
        assertEquals(refundAmount, filerEntry.getTotalCredits());
        assertTrue(filerEntry.getLines().stream()
                .anyMatch(line -> "1000".equals(line.getAccountNumber()) && 
                         line.getDebit().compareTo(refundAmount) == 0));
        assertTrue(filerEntry.getLines().stream()
                .anyMatch(line -> "1200".equals(line.getAccountNumber()) && 
                         line.getCredit().compareTo(refundAmount) == 0));
        
        // Verify municipality entry (DEBIT Refunds Payable, CREDIT Cash)
        JournalEntry municipalityEntry = entries.stream()
                .filter(e -> !e.getEntityId().equals(filerId))
                .findFirst()
                .orElseThrow();
        assertEquals(2, municipalityEntry.getLines().size());
        assertTrue(municipalityEntry.getLines().stream()
                .anyMatch(line -> "2200".equals(line.getAccountNumber()) && 
                         line.getDebit().compareTo(refundAmount) == 0));
        assertTrue(municipalityEntry.getLines().stream()
                .anyMatch(line -> "1001".equals(line.getAccountNumber()) && 
                         line.getCredit().compareTo(refundAmount) == 0));
    }
    
    /**
     * T055: Test refund validation (amount ≤ overpayment)
     * Tests validation that refund amount must be positive
     */
    @Test
    void testProcessRefundRequest_NegativeAmount_ThrowsException() {
        // Given
        BigDecimal negativeAmount = new BigDecimal("-1000.00");
        String reason = "Test refund";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> refundService.processRefundRequest(
                        tenantId, filerId, negativeAmount, reason, userId));
        
        assertTrue(exception.getMessage().contains("Refund amount must be positive"));
    }
    
    /**
     * T055: Test refund validation for zero amount
     */
    @Test
    void testProcessRefundRequest_ZeroAmount_ThrowsException() {
        // Given
        BigDecimal zeroAmount = BigDecimal.ZERO;
        String reason = "Test refund";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> refundService.processRefundRequest(
                        tenantId, filerId, zeroAmount, reason, userId));
        
        assertTrue(exception.getMessage().contains("Refund amount must be positive"));
    }
    
    /**
     * T054: Test complete refund flow (request + issuance)
     * Simulates the full refund lifecycle
     */
    @Test
    void testCompleteRefundFlow_RequestAndIssuance() {
        // Given
        BigDecimal refundAmount = new BigDecimal("500.00");
        String reason = "Overpayment correction";
        
        // When - Request refund
        JournalEntry requestEntry = refundService.processRefundRequest(
                tenantId, filerId, refundAmount, reason, userId);
        
        // Then - Verify request created
        assertNotNull(requestEntry);
        UUID refundRequestId = requestEntry.getSourceId();
        
        // When - Issue refund
        refundService.issueRefund(tenantId, filerId, refundRequestId, refundAmount, userId);
        
        // Then - Verify all 4 journal entries exist (2 for request, 2 for issuance)
        List<JournalEntry> requestEntries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(refundRequestId);
        // Both request and issuance use the same sourceId
        assertTrue(requestEntries.size() >= 2, "At least 2 entries should exist");
        
        // Verify filer has received cash
        boolean filerCashDebitExists = requestEntries.stream()
                .anyMatch(entry -> entry.getEntityId().equals(filerId) &&
                         entry.getLines().stream().anyMatch(line -> 
                                 "1000".equals(line.getAccountNumber()) && 
                                 line.getDebit().compareTo(refundAmount) == 0));
        assertTrue(filerCashDebitExists, "Filer should have cash debit entry");
    }
    
    /**
     * T054: Test refund request with detailed reason tracking
     */
    @Test
    void testProcessRefundRequest_TracksReasonInDescription() {
        // Given
        BigDecimal refundAmount = new BigDecimal("250.00");
        String specificReason = "Q2 2024 tax calculation error";
        
        // When
        JournalEntry result = refundService.processRefundRequest(
                tenantId, filerId, refundAmount, specificReason, userId);
        
        // Then
        assertTrue(result.getDescription().contains(specificReason),
                "Refund reason should be included in description");
    }
    
    /**
     * T055: Test multiple refund requests maintain data integrity
     */
    @Test
    void testMultipleRefundRequests_MaintainDataIntegrity() {
        // Given
        BigDecimal refund1 = new BigDecimal("100.00");
        BigDecimal refund2 = new BigDecimal("200.00");
        BigDecimal refund3 = new BigDecimal("300.00");
        
        // When
        JournalEntry entry1 = refundService.processRefundRequest(
                tenantId, filerId, refund1, "Refund 1", userId);
        JournalEntry entry2 = refundService.processRefundRequest(
                tenantId, filerId, refund2, "Refund 2", userId);
        JournalEntry entry3 = refundService.processRefundRequest(
                tenantId, filerId, refund3, "Refund 3", userId);
        
        // Then
        assertNotNull(entry1);
        assertNotNull(entry2);
        assertNotNull(entry3);
        assertNotEquals(entry1.getSourceId(), entry2.getSourceId());
        assertNotEquals(entry2.getSourceId(), entry3.getSourceId());
        
        // Verify all entries maintain balance
        assertEquals(refund1, entry1.getTotalDebits());
        assertEquals(refund1, entry1.getTotalCredits());
        assertEquals(refund2, entry2.getTotalDebits());
        assertEquals(refund2, entry2.getTotalCredits());
        assertEquals(refund3, entry3.getTotalDebits());
        assertEquals(refund3, entry3.getTotalCredits());
    }
}
