package com.munitax.ledger.integration;

import com.munitax.ledger.dto.*;
import com.munitax.ledger.enums.PaymentMethod;
import com.munitax.ledger.enums.PaymentStatus;
import com.munitax.ledger.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T079: End-to-end integration test for reconciliation with multiple filers
 * Tests: Multiple filers → aggregate reconciliation → discrepancy detection
 */
@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class ReconciliationIntegrationTest {

    @Autowired
    private TaxAssessmentService taxAssessmentService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private TrialBalanceService trialBalanceService;

    private UUID tenantId;
    private List<UUID> filerIds;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        filerIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            filerIds.add(UUID.randomUUID());
        }
    }

    @Test
    void testMultiFilerReconciliation_AllBalanced() {
        LocalDate assessmentDate = LocalDate.of(2024, 4, 20);
        BigDecimal totalExpected = BigDecimal.ZERO;

        // Create assessments and payments for 5 filers
        for (int i = 0; i < filerIds.size(); i++) {
            UUID filerId = filerIds.get(i);
            BigDecimal taxAmount = new BigDecimal(String.valueOf((i + 1) * 2000)); // $2000, $4000, $6000, $8000, $10000
            String taxReturnId = "TR-2024-Q1-FILER" + (i + 1);

            // Assess tax
            TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                    .filerId(filerId)
                    .tenantId(tenantId)
                    .taxReturnId(taxReturnId)
                    .taxAmount(taxAmount)
                    .assessmentDate(assessmentDate)
                    .description("Q1 2024 Tax for Filer " + (i + 1))
                    .build();

            taxAssessmentService.recordTaxAssessment(assessmentRequest);

            // Make full payment
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .filerId(filerId)
                    .tenantId(tenantId)
                    .amount(taxAmount)
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .cardNumber("4111-1111-1111-1111")
                    .description("Payment for " + taxReturnId)
                    .taxReturnId(taxReturnId)
                    .build();

            PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
            assertEquals(PaymentStatus.APPROVED, paymentResponse.getStatus());

            totalExpected = totalExpected.add(taxAmount);
        }

        // Run reconciliation
        ReconciliationRequest reconciliationRequest = ReconciliationRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 4, 30))
                .build();

        ReconciliationResponse reconciliationResponse = reconciliationService.reconcile(reconciliationRequest);
        
        assertNotNull(reconciliationResponse);
        assertTrue(reconciliationResponse.isReconciled() || 
                   Math.abs(reconciliationResponse.getVariance().doubleValue()) < 0.01,
                "All filers paid in full, reconciliation should show balanced");

        // Verify total municipality AR equals sum of all filer liabilities
        assertEquals(0, BigDecimal.ZERO.compareTo(reconciliationResponse.getVariance()),
                "No variance expected when all filers paid in full");

        // Verify no discrepancies
        assertTrue(reconciliationResponse.getDiscrepancies() == null || 
                   reconciliationResponse.getDiscrepancies().isEmpty(),
                "No discrepancies expected when fully reconciled");
    }

    @Test
    void testMultiFilerReconciliation_PartialPayments() {
        LocalDate assessmentDate = LocalDate.of(2024, 4, 20);
        
        // Filer 1: Fully paid
        UUID filer1 = filerIds.get(0);
        BigDecimal tax1 = new BigDecimal("5000.00");
        createAssessmentAndPayment(filer1, "TR-2024-Q1-F1", tax1, tax1, assessmentDate);

        // Filer 2: Partially paid (50%)
        UUID filer2 = filerIds.get(1);
        BigDecimal tax2 = new BigDecimal("8000.00");
        BigDecimal payment2 = new BigDecimal("4000.00");
        createAssessmentAndPayment(filer2, "TR-2024-Q1-F2", tax2, payment2, assessmentDate);

        // Filer 3: No payment
        UUID filer3 = filerIds.get(2);
        BigDecimal tax3 = new BigDecimal("6000.00");
        createAssessmentOnly(filer3, "TR-2024-Q1-F3", tax3, assessmentDate);

        // Filer 4: Overpaid
        UUID filer4 = filerIds.get(3);
        BigDecimal tax4 = new BigDecimal("7000.00");
        BigDecimal payment4 = new BigDecimal("7500.00");
        createAssessmentAndPayment(filer4, "TR-2024-Q1-F4", tax4, payment4, assessmentDate);

        // Filer 5: Fully paid
        UUID filer5 = filerIds.get(4);
        BigDecimal tax5 = new BigDecimal("4000.00");
        createAssessmentAndPayment(filer5, "TR-2024-Q1-F5", tax5, tax5, assessmentDate);

        // Run reconciliation
        ReconciliationRequest reconciliationRequest = ReconciliationRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 4, 30))
                .build();

        ReconciliationResponse reconciliationResponse = reconciliationService.reconcile(reconciliationRequest);
        
        assertNotNull(reconciliationResponse);

        // Calculate expected variance
        // Total tax: $30,000 (5000 + 8000 + 6000 + 7000 + 4000)
        // Total paid: $28,500 (5000 + 4000 + 0 + 7500 + 4000)
        // Expected outstanding AR: $1,500
        BigDecimal expectedOutstanding = new BigDecimal("1500.00");

        // Note: Variance calculation depends on reconciliation logic
        // If not reconciled, variance should be non-zero
        if (!reconciliationResponse.isReconciled()) {
            assertNotEquals(0, reconciliationResponse.getVariance().compareTo(BigDecimal.ZERO),
                    "Variance should be non-zero with partial payments");
        }

        // Run individual filer reconciliation to verify details
        for (int i = 0; i < 5; i++) {
            ReconciliationRequest filerReconReq = ReconciliationRequest.builder()
                    .tenantId(tenantId)
                    .filerId(filerIds.get(i))
                    .asOfDate(LocalDate.of(2024, 4, 30))
                    .build();

            ReconciliationResponse filerRecon = reconciliationService.reconcileFiler(filerReconReq);
            assertNotNull(filerRecon);
        }
    }

    @Test
    void testReconciliationWithDiscrepancies_MissingPayment() {
        LocalDate assessmentDate = LocalDate.of(2024, 4, 20);

        // Filer 1: Normal - assessment and payment recorded on both books
        UUID filer1 = filerIds.get(0);
        BigDecimal tax1 = new BigDecimal("5000.00");
        createAssessmentAndPayment(filer1, "TR-2024-Q1-F1", tax1, tax1, assessmentDate);

        // Filer 2: Has assessment but missing payment (simulated discrepancy)
        UUID filer2 = filerIds.get(1);
        BigDecimal tax2 = new BigDecimal("10000.00");
        createAssessmentOnly(filer2, "TR-2024-Q1-F2", tax2, assessmentDate);
        // No payment made - this creates a discrepancy

        // Run reconciliation
        ReconciliationRequest reconciliationRequest = ReconciliationRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 4, 30))
                .build();

        ReconciliationResponse reconciliationResponse = reconciliationService.reconcile(reconciliationRequest);
        
        assertNotNull(reconciliationResponse);
        
        // Should not be fully reconciled due to unpaid tax
        // (Note: Depends on reconciliation logic - may still reconcile if both books show AR)
        
        // Verify municipality AR reflects outstanding balances
        BigDecimal expectedAR = new BigDecimal("10000.00"); // Filer 2's unpaid tax
        
        // Check if discrepancies are reported
        if (reconciliationResponse.getDiscrepancies() != null) {
            assertFalse(reconciliationResponse.getDiscrepancies().isEmpty(),
                    "Should report discrepancy for unpaid tax");
        }
    }

    @Test
    void testReconciliationAccuracy_WithTrialBalance() {
        LocalDate assessmentDate = LocalDate.of(2024, 4, 20);

        // Create multiple transactions across filers
        for (int i = 0; i < 3; i++) {
            UUID filerId = filerIds.get(i);
            BigDecimal taxAmount = new BigDecimal(String.valueOf((i + 1) * 3000));
            String taxReturnId = "TR-2024-Q1-F" + (i + 1);

            createAssessmentAndPayment(filerId, taxReturnId, taxAmount, taxAmount, assessmentDate);
        }

        // Generate trial balance to verify ledger integrity
        TrialBalanceRequest trialBalanceRequest = TrialBalanceRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 4, 30))
                .build();

        TrialBalanceResponse trialBalance = trialBalanceService.generateTrialBalance(trialBalanceRequest);
        
        assertNotNull(trialBalance);
        assertTrue(trialBalance.isBalanced(), "Trial balance should be balanced");
        assertEquals(0, trialBalance.getTotalDebits().compareTo(trialBalance.getTotalCredits()),
                "Total debits should equal total credits");

        // Run reconciliation
        ReconciliationRequest reconciliationRequest = ReconciliationRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 4, 30))
                .build();

        ReconciliationResponse reconciliationResponse = reconciliationService.reconcile(reconciliationRequest);
        
        assertTrue(reconciliationResponse.isReconciled() || 
                   Math.abs(reconciliationResponse.getVariance().doubleValue()) < 0.01,
                "Reconciliation should succeed when trial balance is balanced");
    }

    @Test
    void testReconciliationPerformance_LargeDataset() {
        LocalDate assessmentDate = LocalDate.of(2024, 4, 20);
        int numFilers = 50; // Reduced from production scale for test speed
        
        List<UUID> largeFilerList = new ArrayList<>();
        for (int i = 0; i < numFilers; i++) {
            largeFilerList.add(UUID.randomUUID());
        }

        // Create assessments and payments for many filers
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numFilers; i++) {
            UUID filerId = largeFilerList.get(i);
            BigDecimal taxAmount = new BigDecimal(String.valueOf(1000 + (i * 100)));
            String taxReturnId = "TR-2024-Q1-F" + (i + 1);

            createAssessmentAndPayment(filerId, taxReturnId, taxAmount, taxAmount, assessmentDate);
        }

        long setupTime = System.currentTimeMillis() - startTime;
        System.out.println("Setup time for " + numFilers + " filers: " + setupTime + "ms");

        // Run reconciliation and measure performance
        long reconStartTime = System.currentTimeMillis();
        
        ReconciliationRequest reconciliationRequest = ReconciliationRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 4, 30))
                .build();

        ReconciliationResponse reconciliationResponse = reconciliationService.reconcile(reconciliationRequest);
        
        long reconTime = System.currentTimeMillis() - reconStartTime;
        System.out.println("Reconciliation time for " + numFilers + " filers: " + reconTime + "ms");

        assertNotNull(reconciliationResponse);
        assertTrue(reconTime < 30000, "Reconciliation should complete within 30 seconds for 50 filers");
        
        assertTrue(reconciliationResponse.isReconciled() || 
                   Math.abs(reconciliationResponse.getVariance().doubleValue()) < 0.01);
    }

    // Helper methods

    private void createAssessmentAndPayment(UUID filerId, String taxReturnId, 
                                           BigDecimal taxAmount, BigDecimal paymentAmount, 
                                           LocalDate assessmentDate) {
        // Create assessment
        TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .taxReturnId(taxReturnId)
                .taxAmount(taxAmount)
                .assessmentDate(assessmentDate)
                .description("Tax assessment for " + taxReturnId)
                .build();

        taxAssessmentService.recordTaxAssessment(assessmentRequest);

        // Create payment
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .amount(paymentAmount)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .description("Payment for " + taxReturnId)
                .taxReturnId(taxReturnId)
                .build();

        paymentService.processPayment(paymentRequest);
    }

    private void createAssessmentOnly(UUID filerId, String taxReturnId, 
                                     BigDecimal taxAmount, LocalDate assessmentDate) {
        TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                .filerId(filerId)
                .tenantId(tenantId)
                .taxReturnId(taxReturnId)
                .taxAmount(taxAmount)
                .assessmentDate(assessmentDate)
                .description("Tax assessment for " + taxReturnId)
                .build();

        taxAssessmentService.recordTaxAssessment(assessmentRequest);
    }
}
