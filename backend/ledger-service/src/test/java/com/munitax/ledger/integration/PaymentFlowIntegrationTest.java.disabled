package com.munitax.ledger.integration;

import com.munitax.ledger.dto.*;
import com.munitax.ledger.enums.PaymentMethod;
import com.munitax.ledger.enums.PaymentStatus;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.repository.JournalEntryRepository;
import com.munitax.ledger.repository.PaymentTransactionRepository;
import com.munitax.ledger.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T077: End-to-end integration test for complete payment flow
 * Tests: assessment → payment → ledger → statement → reconciliation
 */
@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class PaymentFlowIntegrationTest {

    @Autowired
    private TaxAssessmentService taxAssessmentService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountStatementService accountStatementService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    private UUID tenantId;
    private UUID filerId;
    private String taxReturnId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        filerId = UUID.randomUUID();
        taxReturnId = "TR-2024-Q1-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void testCompletePaymentFlow_AssessmentToReconciliation() {
        // Step 1: Record tax assessment
        TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .taxReturnId(taxReturnId)
                .taxAmount(new BigDecimal("10000.00"))
                .penaltyAmount(new BigDecimal("250.00"))
                .interestAmount(new BigDecimal("150.00"))
                .assessmentDate(LocalDate.of(2024, 4, 20))
                .description("Q1 2024 Tax Assessment")
                .build();

        TaxAssessmentResponse assessmentResponse = taxAssessmentService.recordTaxAssessment(assessmentRequest);
        assertNotNull(assessmentResponse);
        assertNotNull(assessmentResponse.getFilerJournalEntryId());
        assertNotNull(assessmentResponse.getMunicipalityJournalEntryId());

        // Verify assessment journal entries created
        List<JournalEntry> assessmentEntries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(taxReturnId);
        assertEquals(2, assessmentEntries.size()); // Filer and municipality entries

        // Step 2: Make payment for full amount
        BigDecimal totalAmount = new BigDecimal("10400.00"); // Tax + penalty + interest
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(totalAmount)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .description("Payment for " + taxReturnId)
                .taxReturnId(taxReturnId)
                .build();

        PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
        assertNotNull(paymentResponse);
        assertEquals(PaymentStatus.APPROVED, paymentResponse.getStatus());
        assertNotNull(paymentResponse.getTransactionId());
        assertTrue(paymentResponse.isTestMode());

        // Verify payment journal entries created
        List<JournalEntry> paymentEntries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(paymentResponse.getTransactionId().toString());
        assertEquals(2, paymentEntries.size()); // Filer and municipality payment entries

        // Step 3: Generate account statement
        AccountStatementRequest statementRequest = AccountStatementRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        AccountStatementResponse statementResponse = accountStatementService.generateStatement(statementRequest);
        assertNotNull(statementResponse);
        assertFalse(statementResponse.getTransactions().isEmpty());

        // Verify statement shows both assessment and payment
        List<StatementTransaction> transactions = statementResponse.getTransactions();
        assertTrue(transactions.size() >= 2); // At least assessment and payment

        // Verify running balance
        BigDecimal finalBalance = statementResponse.getCurrentBalance();
        assertEquals(BigDecimal.ZERO.compareTo(finalBalance), 0); // Balance should be zero after payment

        // Step 4: Run reconciliation
        ReconciliationRequest reconciliationRequest = ReconciliationRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 4, 30))
                .build();

        ReconciliationResponse reconciliationResponse = reconciliationService.reconcile(reconciliationRequest);
        assertNotNull(reconciliationResponse);

        // Verify reconciliation shows balanced books
        assertTrue(reconciliationResponse.isReconciled() || 
                   Math.abs(reconciliationResponse.getVariance().doubleValue()) < 0.01);

        // Step 5: Verify all journal entries balance
        List<JournalEntry> allEntries = journalEntryRepository
                .findByTenantIdAndEntityIdOrderByEntryDateDesc(tenantId, filerId, null);
        
        for (JournalEntry entry : allEntries) {
            assertEquals(0, entry.getTotalDebits().compareTo(entry.getTotalCredits()),
                    "Entry " + entry.getEntryNumber() + " should be balanced");
        }

        // Step 6: Verify total ledger balance
        BigDecimal totalDebits = allEntries.stream()
                .map(JournalEntry::getTotalDebits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = allEntries.stream()
                .map(JournalEntry::getTotalCredits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, totalDebits.compareTo(totalCredits),
                "Total debits should equal total credits across all entries");
    }

    @Test
    void testPartialPaymentFlow_MultiplePayments() {
        // Assess tax
        TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .taxReturnId(taxReturnId)
                .taxAmount(new BigDecimal("10000.00"))
                .assessmentDate(LocalDate.of(2024, 4, 20))
                .description("Q1 2024 Tax Assessment")
                .build();

        taxAssessmentService.recordTaxAssessment(assessmentRequest);

        // Make first partial payment
        PaymentRequest payment1 = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("6000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .description("Partial payment 1")
                .taxReturnId(taxReturnId)
                .build();

        PaymentResponse response1 = paymentService.processPayment(payment1);
        assertEquals(PaymentStatus.APPROVED, response1.getStatus());

        // Check balance after first payment
        AccountStatementRequest statementReq1 = AccountStatementRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        AccountStatementResponse statement1 = accountStatementService.generateStatement(statementReq1);
        assertEquals(0, new BigDecimal("4000.00").compareTo(statement1.getCurrentBalance()));

        // Make second partial payment
        PaymentRequest payment2 = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("4000.00"))
                .paymentMethod(PaymentMethod.ACH)
                .accountNumber("TEST123456789")
                .routingNumber("021000021")
                .description("Partial payment 2")
                .taxReturnId(taxReturnId)
                .build();

        PaymentResponse response2 = paymentService.processPayment(payment2);
        assertEquals(PaymentStatus.APPROVED, response2.getStatus());

        // Check final balance
        AccountStatementResponse statement2 = accountStatementService.generateStatement(statementReq1);
        assertEquals(0, BigDecimal.ZERO.compareTo(statement2.getCurrentBalance()));

        // Verify reconciliation
        ReconciliationRequest reconReq = ReconciliationRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 4, 30))
                .build();

        ReconciliationResponse reconResp = reconciliationService.reconcile(reconReq);
        assertTrue(reconResp.isReconciled() || 
                   Math.abs(reconResp.getVariance().doubleValue()) < 0.01);
    }

    @Test
    void testDeclinedPaymentFlow_NoJournalEntries() {
        // Assess tax
        TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .taxReturnId(taxReturnId)
                .taxAmount(new BigDecimal("5000.00"))
                .assessmentDate(LocalDate.of(2024, 4, 20))
                .description("Q1 2024 Tax Assessment")
                .build();

        taxAssessmentService.recordTaxAssessment(assessmentRequest);

        // Count journal entries before declined payment
        List<JournalEntry> entriesBefore = journalEntryRepository
                .findByTenantIdAndEntityIdOrderByEntryDateDesc(tenantId, filerId, null);
        int countBefore = entriesBefore.size();

        // Try payment with test card that will be declined
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4000-0000-0000-0002") // Test card for decline
                .description("Payment attempt")
                .taxReturnId(taxReturnId)
                .build();

        PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
        assertEquals(PaymentStatus.DECLINED, paymentResponse.getStatus());

        // Verify no new journal entries created for declined payment
        List<JournalEntry> entriesAfter = journalEntryRepository
                .findByTenantIdAndEntityIdOrderByEntryDateDesc(tenantId, filerId, null);
        assertEquals(countBefore, entriesAfter.size(),
                "No journal entries should be created for declined payments");

        // Verify balance unchanged
        AccountStatementRequest statementReq = AccountStatementRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        AccountStatementResponse statement = accountStatementService.generateStatement(statementReq);
        assertEquals(0, new BigDecimal("5000.00").compareTo(statement.getCurrentBalance()),
                "Balance should remain at tax amount since payment was declined");
    }
}
