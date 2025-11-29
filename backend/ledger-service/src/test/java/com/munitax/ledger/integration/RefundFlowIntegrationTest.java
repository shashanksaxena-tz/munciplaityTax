package com.munitax.ledger.integration;

import com.munitax.ledger.dto.*;
import com.munitax.ledger.enums.PaymentMethod;
import com.munitax.ledger.enums.PaymentStatus;
import com.munitax.ledger.enums.RefundMethod;
import com.munitax.ledger.enums.RefundStatus;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.repository.JournalEntryRepository;
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
 * T078: End-to-end integration test for refund flow
 * Tests: overpayment → refund request → approval → issuance → ledger entries
 */
@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class RefundFlowIntegrationTest {

    @Autowired
    private TaxAssessmentService taxAssessmentService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private AccountStatementService accountStatementService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private AuditLogService auditLogService;

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
    void testCompleteRefundFlow_OverpaymentToIssuance() {
        // Step 1: Record tax assessment
        TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .taxReturnId(taxReturnId)
                .taxAmount(new BigDecimal("10000.00"))
                .assessmentDate(LocalDate.of(2024, 4, 20))
                .description("Q1 2024 Tax Assessment")
                .build();

        TaxAssessmentResponse assessmentResponse = taxAssessmentService.recordTaxAssessment(assessmentRequest);
        assertNotNull(assessmentResponse);

        // Step 2: Make overpayment
        BigDecimal overpaymentAmount = new BigDecimal("11000.00"); // $1000 overpayment
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(overpaymentAmount)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .description("Overpayment for " + taxReturnId)
                .taxReturnId(taxReturnId)
                .build();

        PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
        assertEquals(PaymentStatus.APPROVED, paymentResponse.getStatus());

        // Step 3: Verify overpayment in account statement
        AccountStatementRequest statementRequest = AccountStatementRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        AccountStatementResponse statement = accountStatementService.generateStatement(statementRequest);
        BigDecimal overpayment = new BigDecimal("-1000.00"); // Negative balance = credit balance
        assertEquals(0, overpayment.compareTo(statement.getCurrentBalance()),
                "Should show $1000 credit balance (overpayment)");

        // Step 4: Request refund
        RefundRequest refundRequest = RefundRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("1000.00"))
                .refundMethod(RefundMethod.ACH)
                .accountNumber("TEST123456789")
                .routingNumber("021000021")
                .reason("Overpayment refund for " + taxReturnId)
                .taxReturnId(taxReturnId)
                .build();

        RefundResponse refundResponse = refundService.requestRefund(refundRequest);
        assertNotNull(refundResponse);
        assertNotNull(refundResponse.getRefundId());
        assertEquals(RefundStatus.REQUESTED, refundResponse.getStatus());
        assertNotNull(refundResponse.getRequestJournalEntryId());

        // Verify refund request journal entries
        List<JournalEntry> requestEntries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc(refundResponse.getRefundId().toString());
        assertEquals(2, requestEntries.size()); // Filer and municipality request entries

        // Step 5: Approve refund
        UUID refundId = refundResponse.getRefundId();
        RefundApprovalRequest approvalRequest = RefundApprovalRequest.builder()
                .refundId(refundId)
                .tenantId(tenantId)
                .approvedBy("FinanceManager")
                .approvalNotes("Verified overpayment")
                .build();

        RefundResponse approvalResponse = refundService.approveRefund(approvalRequest);
        assertEquals(RefundStatus.APPROVED, approvalResponse.getStatus());

        // Step 6: Issue refund
        RefundIssuanceRequest issuanceRequest = RefundIssuanceRequest.builder()
                .refundId(refundId)
                .tenantId(tenantId)
                .issuanceDate(LocalDate.of(2024, 5, 1))
                .issuedBy("PaymentProcessor")
                .build();

        RefundResponse issuanceResponse = refundService.issueRefund(issuanceRequest);
        assertEquals(RefundStatus.ISSUED, issuanceResponse.getStatus());
        assertNotNull(issuanceResponse.getIssuanceJournalEntryId());

        // Verify issuance journal entries
        List<JournalEntry> issuanceEntries = journalEntryRepository
                .findBySourceIdOrderByEntryDateDesc("REFUND_ISSUANCE_" + refundId);
        assertEquals(2, issuanceEntries.size()); // Filer and municipality issuance entries

        // Step 7: Verify final account balance
        AccountStatementResponse finalStatement = accountStatementService.generateStatement(statementRequest);
        assertEquals(0, BigDecimal.ZERO.compareTo(finalStatement.getCurrentBalance()),
                "Balance should be zero after refund issuance");

        // Step 8: Verify reconciliation
        ReconciliationRequest reconciliationRequest = ReconciliationRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 5, 1))
                .build();

        ReconciliationResponse reconciliationResponse = reconciliationService.reconcile(reconciliationRequest);
        assertTrue(reconciliationResponse.isReconciled() || 
                   Math.abs(reconciliationResponse.getVariance().doubleValue()) < 0.01);

        // Step 9: Verify all journal entries balance
        List<JournalEntry> allEntries = journalEntryRepository
                .findByTenantIdAndEntityIdOrderByEntryDateDesc(tenantId, filerId, null);
        
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        
        for (JournalEntry entry : allEntries) {
            assertEquals(0, entry.getTotalDebits().compareTo(entry.getTotalCredits()),
                    "Entry " + entry.getEntryNumber() + " should be balanced");
            totalDebits = totalDebits.add(entry.getTotalDebits());
            totalCredits = totalCredits.add(entry.getTotalCredits());
        }

        assertEquals(0, totalDebits.compareTo(totalCredits),
                "Total debits should equal total credits");
    }

    @Test
    void testRefundValidation_CannotExceedOverpayment() {
        // Create tax assessment
        TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .taxReturnId(taxReturnId)
                .taxAmount(new BigDecimal("5000.00"))
                .assessmentDate(LocalDate.of(2024, 4, 20))
                .build();

        taxAssessmentService.recordTaxAssessment(assessmentRequest);

        // Make payment
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("5500.00")) // $500 overpayment
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .taxReturnId(taxReturnId)
                .build();

        paymentService.processPayment(paymentRequest);

        // Try to request refund larger than overpayment
        RefundRequest refundRequest = RefundRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("1000.00")) // Exceeds $500 overpayment
                .refundMethod(RefundMethod.CHECK)
                .reason("Overpayment refund")
                .taxReturnId(taxReturnId)
                .build();

        // Should throw exception or return error status
        assertThrows(Exception.class, () -> {
            refundService.requestRefund(refundRequest);
        }, "Should not allow refund exceeding overpayment");
    }

    @Test
    void testMultipleRefundMethods_ACH_Check_Wire() {
        // Setup: Create overpayment
        TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .taxReturnId(taxReturnId)
                .taxAmount(new BigDecimal("10000.00"))
                .assessmentDate(LocalDate.of(2024, 4, 20))
                .build();

        taxAssessmentService.recordTaxAssessment(assessmentRequest);

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("13000.00")) // $3000 overpayment
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .taxReturnId(taxReturnId)
                .build();

        paymentService.processPayment(paymentRequest);

        // Test 1: ACH Refund
        RefundRequest achRefund = RefundRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("1000.00"))
                .refundMethod(RefundMethod.ACH)
                .accountNumber("TEST123")
                .routingNumber("021000021")
                .reason("ACH refund")
                .build();

        RefundResponse achResponse = refundService.requestRefund(achRefund);
        assertEquals(RefundMethod.ACH, achResponse.getRefundMethod());

        // Approve and issue ACH refund
        refundService.approveRefund(RefundApprovalRequest.builder()
                .refundId(achResponse.getRefundId())
                .tenantId(tenantId)
                .approvedBy("Manager")
                .build());

        refundService.issueRefund(RefundIssuanceRequest.builder()
                .refundId(achResponse.getRefundId())
                .tenantId(tenantId)
                .issuanceDate(LocalDate.now())
                .issuedBy("System")
                .build());

        // Test 2: Check Refund
        RefundRequest checkRefund = RefundRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("1000.00"))
                .refundMethod(RefundMethod.CHECK)
                .mailingAddress("123 Main St, City, State 12345")
                .reason("Check refund")
                .build();

        RefundResponse checkResponse = refundService.requestRefund(checkRefund);
        assertEquals(RefundMethod.CHECK, checkResponse.getRefundMethod());

        // Test 3: Wire Refund
        RefundRequest wireRefund = RefundRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("1000.00"))
                .refundMethod(RefundMethod.WIRE)
                .accountNumber("WIRE987654321")
                .routingNumber("026009593")
                .swiftCode("BOFAUS3N")
                .reason("Wire refund")
                .build();

        RefundResponse wireResponse = refundService.requestRefund(wireRefund);
        assertEquals(RefundMethod.WIRE, wireResponse.getRefundMethod());

        // Verify all three refunds recorded
        AccountStatementRequest statementRequest = AccountStatementRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        AccountStatementResponse statement = accountStatementService.generateStatement(statementRequest);
        // Should have tax, payment, and 3 refund transactions
        assertTrue(statement.getTransactions().size() >= 5);
    }

    @Test
    void testRefundAuditTrail_CompleteHistory() {
        // Create overpayment scenario
        TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .taxReturnId(taxReturnId)
                .taxAmount(new BigDecimal("5000.00"))
                .assessmentDate(LocalDate.of(2024, 4, 20))
                .build();

        taxAssessmentService.recordTaxAssessment(assessmentRequest);

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("6000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .taxReturnId(taxReturnId)
                .build();

        paymentService.processPayment(paymentRequest);

        // Request refund
        RefundRequest refundRequest = RefundRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(new BigDecimal("1000.00"))
                .refundMethod(RefundMethod.ACH)
                .accountNumber("TEST123")
                .routingNumber("021000021")
                .reason("Overpayment refund")
                .build();

        RefundResponse refundResponse = refundService.requestRefund(refundRequest);
        UUID refundId = refundResponse.getRefundId();

        // Approve refund
        refundService.approveRefund(RefundApprovalRequest.builder()
                .refundId(refundId)
                .tenantId(tenantId)
                .approvedBy("FinanceManager")
                .approvalNotes("Verified")
                .build());

        // Issue refund
        refundService.issueRefund(RefundIssuanceRequest.builder()
                .refundId(refundId)
                .tenantId(tenantId)
                .issuanceDate(LocalDate.now())
                .issuedBy("Processor")
                .build());

        // Verify audit trail captures all refund lifecycle events
        AuditLogRequest auditRequest = AuditLogRequest.builder()
                .tenantId(tenantId)
                .entityType("REFUND")
                .entityId(refundId.toString())
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        AuditLogResponse auditResponse = auditLogService.getAuditLog(auditRequest);
        assertNotNull(auditResponse);
        assertFalse(auditResponse.getAuditLogs().isEmpty());

        // Verify audit trail contains: REQUEST, APPROVE, ISSUE events
        List<String> actions = auditResponse.getAuditLogs().stream()
                .map(log -> log.getAction())
                .toList();

        assertTrue(actions.contains("REFUND_REQUESTED") || actions.contains("CREATE"));
        assertTrue(actions.contains("REFUND_APPROVED") || actions.contains("APPROVE"));
        assertTrue(actions.contains("REFUND_ISSUED") || actions.contains("ISSUE"));
    }
}
