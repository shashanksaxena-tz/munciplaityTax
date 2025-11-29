package com.munitax.ledger.performance;

import com.munitax.ledger.dto.*;
import com.munitax.ledger.enums.PaymentMethod;
import com.munitax.ledger.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T080: Performance test for trial balance with large dataset (10,000+ entries)
 * Tests: Large-scale journal entry processing and trial balance calculation
 * 
 * Note: This test is disabled by default. To run it, add:
 * -Dperformance.tests.enabled=true
 */
@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
@EnabledIfSystemProperty(named = "performance.tests.enabled", matches = "true")
class TrialBalancePerformanceTest {

    @Autowired
    private TaxAssessmentService taxAssessmentService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TrialBalanceService trialBalanceService;

    @Autowired
    private JournalEntryService journalEntryService;

    private UUID tenantId;
    private List<UUID> filerIds;
    
    private static final int NUM_FILERS = 100;
    private static final int TRANSACTIONS_PER_FILER = 50; // Total: 5000 transactions = 10,000+ entries

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        filerIds = new ArrayList<>();
        for (int i = 0; i < NUM_FILERS; i++) {
            filerIds.add(UUID.randomUUID());
        }
    }

    @Test
    void testTrialBalancePerformance_10000PlusEntries() {
        System.out.println("=== Trial Balance Performance Test ===");
        System.out.println("Filers: " + NUM_FILERS);
        System.out.println("Transactions per filer: " + TRANSACTIONS_PER_FILER);
        System.out.println("Expected total entries: " + (NUM_FILERS * TRANSACTIONS_PER_FILER * 2));
        
        // Phase 1: Create large dataset
        long dataSetupStart = System.currentTimeMillis();
        createLargeDataset();
        long dataSetupTime = System.currentTimeMillis() - dataSetupStart;
        
        System.out.println("\nDataset creation time: " + dataSetupTime + "ms (" + 
                          formatTime(dataSetupTime) + ")");

        // Phase 2: Generate trial balance
        long trialBalanceStart = System.currentTimeMillis();
        
        TrialBalanceRequest request = TrialBalanceRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 12, 31))
                .build();

        TrialBalanceResponse response = trialBalanceService.generateTrialBalance(request);
        
        long trialBalanceTime = System.currentTimeMillis() - trialBalanceStart;
        
        System.out.println("Trial balance generation time: " + trialBalanceTime + "ms (" + 
                          formatTime(trialBalanceTime) + ")");

        // Assertions
        assertNotNull(response, "Trial balance should be generated");
        assertTrue(response.isBalanced(), "Trial balance should be balanced");
        assertEquals(0, response.getTotalDebits().compareTo(response.getTotalCredits()),
                "Total debits should equal total credits");

        // Performance assertions
        assertTrue(trialBalanceTime < 60000, 
                "Trial balance generation should complete within 60 seconds");
        
        if (trialBalanceTime < 5000) {
            System.out.println("✓ EXCELLENT: < 5 seconds");
        } else if (trialBalanceTime < 15000) {
            System.out.println("✓ GOOD: < 15 seconds");
        } else if (trialBalanceTime < 30000) {
            System.out.println("✓ ACCEPTABLE: < 30 seconds");
        } else {
            System.out.println("⚠ SLOW: > 30 seconds, optimization recommended");
        }

        // Verify data integrity
        assertFalse(response.getAccountBalances().isEmpty(), 
                "Should have account balances");
        
        System.out.println("\nAccounts in trial balance: " + response.getAccountBalances().size());
        System.out.println("Total debits: $" + response.getTotalDebits());
        System.out.println("Total credits: $" + response.getTotalCredits());
    }

    @Test
    void testTrialBalancePerformance_DateRangeFiltering() {
        // Create dataset spanning multiple quarters
        createQuarterlyDataset();

        // Test 1: Full year trial balance
        long fullYearStart = System.currentTimeMillis();
        TrialBalanceRequest fullYearRequest = TrialBalanceRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 12, 31))
                .build();
        TrialBalanceResponse fullYearResponse = trialBalanceService.generateTrialBalance(fullYearRequest);
        long fullYearTime = System.currentTimeMillis() - fullYearStart;

        // Test 2: Quarter-end trial balance
        long quarterStart = System.currentTimeMillis();
        TrialBalanceRequest quarterRequest = TrialBalanceRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 3, 31))
                .build();
        TrialBalanceResponse quarterResponse = trialBalanceService.generateTrialBalance(quarterRequest);
        long quarterTime = System.currentTimeMillis() - quarterStart;

        // Test 3: Month-end trial balance
        long monthStart = System.currentTimeMillis();
        TrialBalanceRequest monthRequest = TrialBalanceRequest.builder()
                .tenantId(tenantId)
                .asOfDate(LocalDate.of(2024, 1, 31))
                .build();
        TrialBalanceResponse monthResponse = trialBalanceService.generateTrialBalance(monthRequest);
        long monthTime = System.currentTimeMillis() - monthStart;

        System.out.println("\n=== Date Range Performance ===");
        System.out.println("Full year: " + fullYearTime + "ms");
        System.out.println("Quarter: " + quarterTime + "ms");
        System.out.println("Month: " + monthTime + "ms");

        // All should be balanced
        assertTrue(fullYearResponse.isBalanced());
        assertTrue(quarterResponse.isBalanced());
        assertTrue(monthResponse.isBalanced());

        // Performance: Month should be fastest, full year slowest
        assertTrue(monthTime <= fullYearTime, 
                "Month-end should not be slower than full year");
    }

    @Test
    void testTrialBalancePerformance_ConcurrentAccess() throws InterruptedException {
        createLargeDataset();

        // Simulate multiple concurrent trial balance requests
        int numThreads = 5;
        List<Long> executionTimes = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            Thread thread = new Thread(() -> {
                long start = System.currentTimeMillis();
                
                TrialBalanceRequest request = TrialBalanceRequest.builder()
                        .tenantId(tenantId)
                        .asOfDate(LocalDate.of(2024, 12, 31))
                        .build();

                TrialBalanceResponse response = trialBalanceService.generateTrialBalance(request);
                
                long time = System.currentTimeMillis() - start;
                synchronized (executionTimes) {
                    executionTimes.add(time);
                }
                
                assertTrue(response.isBalanced(), "Thread " + threadNum + " should get balanced result");
            });
            
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(TimeUnit.MINUTES.toMillis(2)); // 2 minute timeout
        }

        System.out.println("\n=== Concurrent Access Performance ===");
        System.out.println("Number of concurrent requests: " + numThreads);
        for (int i = 0; i < executionTimes.size(); i++) {
            System.out.println("Thread " + (i + 1) + ": " + executionTimes.get(i) + "ms");
        }

        double avgTime = executionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        System.out.println("Average time: " + avgTime + "ms");

        assertTrue(avgTime < 60000, "Average concurrent request time should be under 60 seconds");
    }

    // Helper methods

    private void createLargeDataset() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        
        for (int filerIndex = 0; filerIndex < NUM_FILERS; filerIndex++) {
            UUID filerId = filerIds.get(filerIndex);
            
            for (int txnIndex = 0; txnIndex < TRANSACTIONS_PER_FILER; txnIndex++) {
                LocalDate txnDate = startDate.plusDays(txnIndex * 7); // Weekly transactions
                BigDecimal amount = new BigDecimal(String.valueOf(1000 + (txnIndex * 100)));
                String taxReturnId = "TR-2024-F" + filerIndex + "-T" + txnIndex;

                // Alternate between assessments and payments
                if (txnIndex % 2 == 0) {
                    // Create assessment
                    TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                            .filerId(filerId)
                            .tenantId(tenantId)
                            .taxReturnId(taxReturnId)
                            .taxAmount(amount)
                            .assessmentDate(txnDate)
                            .description("Assessment " + txnIndex)
                            .build();
                    taxAssessmentService.recordTaxAssessment(assessmentRequest);
                } else {
                    // Create payment
                    PaymentRequest paymentRequest = PaymentRequest.builder()
                            .filerId(filerId)
                            .tenantId(tenantId)
                            .amount(amount)
                            .paymentMethod(PaymentMethod.CREDIT_CARD)
                            .cardNumber("4111-1111-1111-1111")
                            .description("Payment " + txnIndex)
                            .taxReturnId(taxReturnId)
                            .build();
                    paymentService.processPayment(paymentRequest);
                }
            }
            
            // Progress indicator
            if ((filerIndex + 1) % 10 == 0) {
                System.out.println("Created transactions for " + (filerIndex + 1) + "/" + NUM_FILERS + " filers");
            }
        }
    }

    private void createQuarterlyDataset() {
        LocalDate[] quarterEnds = {
            LocalDate.of(2024, 3, 31),
            LocalDate.of(2024, 6, 30),
            LocalDate.of(2024, 9, 30),
            LocalDate.of(2024, 12, 31)
        };

        for (int filerIndex = 0; filerIndex < Math.min(50, NUM_FILERS); filerIndex++) {
            UUID filerId = filerIds.get(filerIndex);
            
            for (int quarter = 0; quarter < quarterEnds.length; quarter++) {
                LocalDate quarterEnd = quarterEnds[quarter];
                BigDecimal taxAmount = new BigDecimal(String.valueOf(5000 + (filerIndex * 100)));
                String taxReturnId = "TR-2024-Q" + (quarter + 1) + "-F" + filerIndex;

                // Assessment
                TaxAssessmentRequest assessmentRequest = TaxAssessmentRequest.builder()
                        .filerId(filerId)
                        .tenantId(tenantId)
                        .taxReturnId(taxReturnId)
                        .taxAmount(taxAmount)
                        .assessmentDate(quarterEnd)
                        .description("Q" + (quarter + 1) + " 2024 Tax")
                        .build();
                taxAssessmentService.recordTaxAssessment(assessmentRequest);

                // Payment
                PaymentRequest paymentRequest = PaymentRequest.builder()
                        .filerId(filerId)
                        .tenantId(tenantId)
                        .amount(taxAmount)
                        .paymentMethod(PaymentMethod.ACH)
                        .accountNumber("TEST" + filerIndex)
                        .routingNumber("021000021")
                        .description("Q" + (quarter + 1) + " Payment")
                        .taxReturnId(taxReturnId)
                        .build();
                paymentService.processPayment(paymentRequest);
            }
        }
    }

    private String formatTime(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.2f seconds", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return minutes + "m " + seconds + "s";
        }
    }
}
