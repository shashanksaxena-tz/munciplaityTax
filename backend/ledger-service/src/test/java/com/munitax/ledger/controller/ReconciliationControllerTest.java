package com.munitax.ledger.controller;

import com.munitax.ledger.config.TestDataInitializer;
import com.munitax.ledger.dto.ReconciliationResponse;
import com.munitax.ledger.enums.ReconciliationStatus;
import com.munitax.ledger.service.PaymentService;
import com.munitax.ledger.service.TaxAssessmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ReconciliationController.
 * Tests the REST API endpoints for reconciliation reporting.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class ReconciliationControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaxAssessmentService taxAssessmentService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TestDataInitializer testDataInitializer;

    private UUID tenantId;
    private UUID municipalityId;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        tenantId = testDataInitializer.getTenantId();
        municipalityId = testDataInitializer.getMunicipalityId();
        baseUrl = "http://localhost:" + port + "/api/v1/reconciliation";
    }

    @Test
    @DisplayName("GET /report/{tenantId}/{municipalityId} should return reconciliation report")
    void testGetReconciliationReport_Success() {
        // Arrange: Create test data
        UUID filerId = UUID.randomUUID();
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        // Act: Call API endpoint
        String url = baseUrl + "/report/" + tenantId + "/" + municipalityId;
        ResponseEntity<ReconciliationResponse> response = restTemplate.getForEntity(
                url, ReconciliationResponse.class);

        // Assert: Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ReconciliationStatus.RECONCILED);
        assertThat(response.getBody().getMunicipalityAR()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(response.getBody().getFilerLiabilities()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("GET /report should return reconciled status for matching balances")
    void testGetReconciliationReport_MatchingBalances() {
        // Arrange: Create multiple filer assessments
        for (int i = 0; i < 5; i++) {
            UUID filerId = UUID.randomUUID();
            taxAssessmentService.recordTaxAssessment(
                    tenantId, filerId, municipalityId,
                    new BigDecimal("5000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                    "Q1 2024", UUID.randomUUID().toString()
            );
        }

        // Act: Call API endpoint
        String url = baseUrl + "/report/" + tenantId + "/" + municipalityId;
        ResponseEntity<ReconciliationResponse> response = restTemplate.getForEntity(
                url, ReconciliationResponse.class);

        // Assert: Verify total is $25,000
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ReconciliationStatus.RECONCILED);
        assertThat(response.getBody().getMunicipalityAR()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(response.getBody().getFilerLiabilities()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(response.getBody().getArVariance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GET /report should handle payments correctly")
    void testGetReconciliationReport_WithPayments() {
        // Arrange: Create assessment and payment
        UUID filerId = UUID.randomUUID();
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        paymentService.processPayment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), "CREDIT_CARD",
                "mock_ch_test", "Full Payment"
        );

        // Act: Call API endpoint
        String url = baseUrl + "/report/" + tenantId + "/" + municipalityId;
        ResponseEntity<ReconciliationResponse> response = restTemplate.getForEntity(
                url, ReconciliationResponse.class);

        // Assert: AR should be zero after full payment
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ReconciliationStatus.RECONCILED);
        assertThat(response.getBody().getMunicipalityAR()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getBody().getMunicipalityCash()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("GET /report should return 200 even with no transactions")
    void testGetReconciliationReport_NoTransactions() {
        // Act: Call API endpoint with no prior transactions
        String url = baseUrl + "/report/" + tenantId + "/" + municipalityId;
        ResponseEntity<ReconciliationResponse> response = restTemplate.getForEntity(
                url, ReconciliationResponse.class);

        // Assert: Should return reconciled at zero
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ReconciliationStatus.RECONCILED);
        assertThat(response.getBody().getMunicipalityAR()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getBody().getFilerLiabilities()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GET /report should handle complex scenario with multiple transaction types")
    void testGetReconciliationReport_ComplexScenario() {
        // Arrange: Multiple filers with different transaction patterns
        UUID filer1 = UUID.randomUUID();
        UUID filer2 = UUID.randomUUID();
        UUID filer3 = UUID.randomUUID();

        // Filer 1: Tax + Payment
        taxAssessmentService.recordTaxAssessment(
                tenantId, filer1, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );
        paymentService.processPayment(
                tenantId, filer1, municipalityId,
                new BigDecimal("10000.00"), "CREDIT_CARD",
                "mock_ch_1", "Payment"
        );

        // Filer 2: Tax with penalty + Partial payment
        taxAssessmentService.recordTaxAssessment(
                tenantId, filer2, municipalityId,
                new BigDecimal("15000.00"),
                new BigDecimal("500.00"), // Penalty
                BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );
        paymentService.processPayment(
                tenantId, filer2, municipalityId,
                new BigDecimal("10000.00"), "ACH",
                "mock_ach_1", "Partial Payment"
        );

        // Filer 3: Tax only, no payment
        taxAssessmentService.recordTaxAssessment(
                tenantId, filer3, municipalityId,
                new BigDecimal("8000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        // Act: Call API endpoint
        String url = baseUrl + "/report/" + tenantId + "/" + municipalityId;
        ResponseEntity<ReconciliationResponse> response = restTemplate.getForEntity(
                url, ReconciliationResponse.class);

        // Assert: Verify complex reconciliation
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ReconciliationStatus.RECONCILED);
        
        // Total liabilities: $10,000 + $15,500 + $8,000 = $33,500
        assertThat(response.getBody().getFilerLiabilities()).isEqualByComparingTo(new BigDecimal("33500.00"));
        
        // After payments: $33,500 - $20,000 = $13,500
        assertThat(response.getBody().getMunicipalityAR()).isEqualByComparingTo(new BigDecimal("13500.00"));
        
        // Total payments: $20,000
        assertThat(response.getBody().getMunicipalityCash()).isEqualByComparingTo(new BigDecimal("20000.00"));
    }

    @Test
    @DisplayName("GET /report should return report date")
    void testGetReconciliationReport_IncludesReportDate() {
        // Act: Call API endpoint
        String url = baseUrl + "/report/" + tenantId + "/" + municipalityId;
        ResponseEntity<ReconciliationResponse> response = restTemplate.getForEntity(
                url, ReconciliationResponse.class);

        // Assert: Report should include date
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReportDate()).isNotNull();
    }
}
