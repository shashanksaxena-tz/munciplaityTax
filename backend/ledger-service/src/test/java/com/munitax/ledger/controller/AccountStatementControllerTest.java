package com.munitax.ledger.controller;

import com.munitax.ledger.config.TestDataInitializer;
import com.munitax.ledger.dto.AccountStatementResponse;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for AccountStatementController.
 * Tests the REST API endpoints for account statement generation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class AccountStatementControllerTest {

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
    private UUID filerId;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        tenantId = testDataInitializer.getTenantId();
        municipalityId = testDataInitializer.getMunicipalityId();
        filerId = UUID.randomUUID();
        baseUrl = "http://localhost:" + port + "/api/v1/statements";
    }

    @Test
    @DisplayName("GET /filer/{tenantId}/{filerId} should return account statement")
    void testGetFilerStatement_Success() {
        // Arrange: Create test data
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        // Act: Call API endpoint
        String url = baseUrl + "/filer/" + tenantId + "/" + filerId;
        ResponseEntity<AccountStatementResponse> response = restTemplate.getForEntity(
                url, AccountStatementResponse.class);

        // Assert: Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTransactions()).isNotEmpty();
        assertThat(response.getBody().getEndingBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("GET /filer should return statement with multiple transactions")
    void testGetFilerStatement_MultipleTransactions() {
        // Arrange: Create assessment and payment
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        paymentService.processPayment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), "CREDIT_CARD",
                "mock_ch_test", "Payment"
        );

        // Act: Call API endpoint
        String url = baseUrl + "/filer/" + tenantId + "/" + filerId;
        ResponseEntity<AccountStatementResponse> response = restTemplate.getForEntity(
                url, AccountStatementResponse.class);

        // Assert: Should show zero balance after payment
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEndingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GET /filer with date range should filter transactions")
    void testGetFilerStatement_WithDateRange() {
        // Arrange: Create transaction
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        // Act: Call API with date range parameters
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(30);
        String url = baseUrl + "/filer/" + tenantId + "/" + filerId 
                + "?startDate=" + startDate + "&endDate=" + endDate;
        
        ResponseEntity<AccountStatementResponse> response = restTemplate.getForEntity(
                url, AccountStatementResponse.class);

        // Assert: Should return filtered statement
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /filer with start date only should filter correctly")
    void testGetFilerStatement_StartDateOnly() {
        // Arrange: Create transaction
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        // Act: Call API with start date only
        LocalDate startDate = LocalDate.now().minusDays(30);
        String url = baseUrl + "/filer/" + tenantId + "/" + filerId 
                + "?startDate=" + startDate;
        
        ResponseEntity<AccountStatementResponse> response = restTemplate.getForEntity(
                url, AccountStatementResponse.class);

        // Assert: Should return statement from start date onward
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /filer with end date only should filter correctly")
    void testGetFilerStatement_EndDateOnly() {
        // Arrange: Create transaction
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        // Act: Call API with end date only
        LocalDate endDate = LocalDate.now().plusDays(30);
        String url = baseUrl + "/filer/" + tenantId + "/" + filerId 
                + "?endDate=" + endDate;
        
        ResponseEntity<AccountStatementResponse> response = restTemplate.getForEntity(
                url, AccountStatementResponse.class);

        // Assert: Should return statement up to end date
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /filer should return 200 even with no transactions")
    void testGetFilerStatement_NoTransactions() {
        // Act: Call API for filer with no transactions
        String url = baseUrl + "/filer/" + tenantId + "/" + filerId;
        ResponseEntity<AccountStatementResponse> response = restTemplate.getForEntity(
                url, AccountStatementResponse.class);

        // Assert: Should return empty statement with zero balance
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTransactions()).isEmpty();
        assertThat(response.getBody().getEndingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GET /filer should include statement metadata")
    void testGetFilerStatement_IncludesMetadata() {
        // Arrange: Create transaction
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        // Act: Call API endpoint
        String url = baseUrl + "/filer/" + tenantId + "/" + filerId;
        ResponseEntity<AccountStatementResponse> response = restTemplate.getForEntity(
                url, AccountStatementResponse.class);

        // Assert: Verify metadata
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccountName()).isNotNull();
        assertThat(response.getBody().getStatementDate()).isNotNull();
        assertThat(response.getBody().getTotalDebits()).isNotNull();
        assertThat(response.getBody().getTotalCredits()).isNotNull();
    }

    @Test
    @DisplayName("GET /filer should handle complex transaction history")
    void testGetFilerStatement_ComplexHistory() {
        // Arrange: Create complex transaction history
        // Q1 Tax
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        // Partial payment
        paymentService.processPayment(
                tenantId, filerId, municipalityId,
                new BigDecimal("5000.00"), "CREDIT_CARD",
                "mock_ch_1", "Partial Payment"
        );

        // Penalty
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                BigDecimal.ZERO,
                new BigDecimal("250.00"),
                BigDecimal.ZERO,
                "Late Payment Penalty", UUID.randomUUID().toString()
        );

        // Final payment
        paymentService.processPayment(
                tenantId, filerId, municipalityId,
                new BigDecimal("5250.00"), "ACH",
                "mock_ach_1", "Final Payment"
        );

        // Act: Call API endpoint
        String url = baseUrl + "/filer/" + tenantId + "/" + filerId;
        ResponseEntity<AccountStatementResponse> response = restTemplate.getForEntity(
                url, AccountStatementResponse.class);

        // Assert: Should show zero balance
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEndingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getBody().getTransactions()).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("GET /filer should calculate running balance correctly")
    void testGetFilerStatement_RunningBalance() {
        // Arrange: Create sequence of transactions
        taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, municipalityId,
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Q1 2024", UUID.randomUUID().toString()
        );

        paymentService.processPayment(
                tenantId, filerId, municipalityId,
                new BigDecimal("3000.00"), "CREDIT_CARD",
                "mock_ch_1", "Payment 1"
        );

        // Act: Call API endpoint
        String url = baseUrl + "/filer/" + tenantId + "/" + filerId;
        ResponseEntity<AccountStatementResponse> response = restTemplate.getForEntity(
                url, AccountStatementResponse.class);

        // Assert: Should show correct balance
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEndingBalance()).isEqualByComparingTo(new BigDecimal("7000.00"));
    }
}
