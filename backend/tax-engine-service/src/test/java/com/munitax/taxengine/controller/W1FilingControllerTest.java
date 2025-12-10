package com.munitax.taxengine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.taxengine.dto.ReconciliationIssue;
import com.munitax.taxengine.dto.W1FilingRequest;
import com.munitax.taxengine.dto.W1FilingResponse;
import com.munitax.taxengine.domain.withholding.FilingFrequency;
import com.munitax.taxengine.domain.withholding.W1FilingStatus;
import com.munitax.taxengine.service.W1FilingService;
import com.munitax.taxengine.service.WithholdingReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for W1FilingController.
 * Tests API endpoints for W-1 filing and reconciliation operations.
 */
@WebMvcTest(W1FilingController.class)
class W1FilingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private W1FilingService w1FilingService;

    @MockBean
    private WithholdingReconciliationService reconciliationService;

    private UUID testBusinessId;
    private UUID testUserId;
    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        testBusinessId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/w1-filings - Should file W-1 return successfully")
    void shouldFileW1ReturnSuccessfully() throws Exception {
        // Given
        W1FilingRequest request = W1FilingRequest.builder()
            .businessId(testBusinessId)
            .taxYear(2024)
            .filingFrequency(FilingFrequency.QUARTERLY)
            .period("Q1")
            .periodStartDate(LocalDate.of(2024, 1, 1))
            .periodEndDate(LocalDate.of(2024, 3, 31))
            .grossWages(new BigDecimal("100000.00"))
            .employeeCount(10)
            .build();

        W1FilingResponse response = W1FilingResponse.builder()
            .id(UUID.randomUUID())
            .businessId(testBusinessId)
            .taxYear(2024)
            .filingFrequency(FilingFrequency.QUARTERLY)
            .period("Q1")
            .periodStartDate(LocalDate.of(2024, 1, 1))
            .periodEndDate(LocalDate.of(2024, 3, 31))
            .dueDate(LocalDate.of(2024, 4, 30))
            .filingDate(LocalDateTime.now())
            .grossWages(new BigDecimal("100000.00"))
            .taxableWages(new BigDecimal("100000.00"))
            .taxRate(new BigDecimal("0.0200"))
            .taxDue(new BigDecimal("2000.00"))
            .adjustments(BigDecimal.ZERO)
            .totalAmountDue(new BigDecimal("2000.00"))
            .status(W1FilingStatus.FILED)
            .build();

        when(w1FilingService.fileW1Return(any(W1FilingRequest.class), any(UUID.class), any(UUID.class)))
            .thenReturn(response);

        // When/Then
        mockMvc.perform(post("/api/v1/w1-filings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", testUserId.toString())
                .header("X-Tenant-Id", testTenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.businessId").value(testBusinessId.toString()))
            .andExpect(jsonPath("$.taxYear").value(2024))
            .andExpect(jsonPath("$.period").value("Q1"))
            .andExpect(jsonPath("$.grossWages").value(100000.00))
            .andExpect(jsonPath("$.taxDue").value(2000.00))
            .andExpect(jsonPath("$.status").value("FILED"));

        verify(w1FilingService, times(1)).fileW1Return(any(W1FilingRequest.class), any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("POST /api/v1/w1-filings/reconcile - Should reconcile W-1 filings successfully")
    void shouldReconcileW1FilingsSuccessfully() throws Exception {
        // Given
        UUID employerId = UUID.randomUUID();
        W1FilingController.ReconciliationRequest request = W1FilingController.ReconciliationRequest.builder()
            .employerId(employerId)
            .taxYear(2024)
            .w2Forms(new ArrayList<>())
            .build();

        List<ReconciliationIssue> issues = List.of(
            ReconciliationIssue.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .taxYear(2024)
                .period("Q3")
                .issueType(ReconciliationIssue.IssueType.MISSING_FILING)
                .severity(ReconciliationIssue.IssueSeverity.CRITICAL)
                .description("Missing required filing for period Q3")
                .recommendedAction("File W-1 return for missing period.")
                .resolved(false)
                .build()
        );

        when(reconciliationService.reconcileW1Filings(any(UUID.class), anyInt(), anyList()))
            .thenReturn(issues);

        // When/Then
        mockMvc.perform(post("/api/v1/w1-filings/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].employerId").value(employerId.toString()))
            .andExpect(jsonPath("$[0].taxYear").value(2024))
            .andExpect(jsonPath("$[0].period").value("Q3"))
            .andExpect(jsonPath("$[0].issueType").value("MISSING_FILING"))
            .andExpect(jsonPath("$[0].severity").value("CRITICAL"));

        verify(reconciliationService, times(1)).reconcileW1Filings(any(UUID.class), anyInt(), anyList());
    }

    @Test
    @DisplayName("GET /api/v1/w1-filings/reconciliation/{employerId} - Should get reconciliation issues")
    void shouldGetReconciliationIssues() throws Exception {
        // Given
        UUID employerId = UUID.randomUUID();
        int taxYear = 2024;

        List<ReconciliationIssue> issues = List.of(
            ReconciliationIssue.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .taxYear(taxYear)
                .period("Q1")
                .issueType(ReconciliationIssue.IssueType.LATE_FILING)
                .severity(ReconciliationIssue.IssueSeverity.MEDIUM)
                .description("Filing for period Q1 was 15 days late")
                .dueDate(LocalDate.of(2024, 4, 30))
                .filingDate(LocalDate.of(2024, 5, 15))
                .recommendedAction("Late filing penalty applied: $100.00")
                .resolved(false)
                .build()
        );

        when(reconciliationService.getReconciliationIssues(employerId, taxYear))
            .thenReturn(issues);

        // When/Then
        mockMvc.perform(get("/api/v1/w1-filings/reconciliation/{employerId}", employerId)
                .param("taxYear", taxYear.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].employerId").value(employerId.toString()))
            .andExpect(jsonPath("$[0].taxYear").value(taxYear))
            .andExpect(jsonPath("$[0].period").value("Q1"))
            .andExpect(jsonPath("$[0].issueType").value("LATE_FILING"))
            .andExpect(jsonPath("$[0].severity").value("MEDIUM"));

        verify(reconciliationService, times(1)).getReconciliationIssues(employerId, taxYear);
    }

    @Test
    @DisplayName("GET /api/v1/w1-filings/reconciliation/{employerId} - Should use current year when taxYear not provided")
    void shouldUseCurrentYearWhenNotProvided() throws Exception {
        // Given
        UUID employerId = UUID.randomUUID();
        Integer currentYear = java.time.Year.now().getValue();

        when(reconciliationService.getReconciliationIssues(employerId, currentYear))
            .thenReturn(new ArrayList<>());

        // When/Then
        mockMvc.perform(get("/api/v1/w1-filings/reconciliation/{employerId}", employerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        verify(reconciliationService, times(1)).getReconciliationIssues(employerId, currentYear);
    }

    @Test
    @DisplayName("POST /api/v1/w1-filings/reconcile - Should return 400 for invalid request")
    void shouldReturn400ForInvalidReconciliationRequest() throws Exception {
        // Given - Request missing required employerId
        W1FilingController.ReconciliationRequest request = W1FilingController.ReconciliationRequest.builder()
            .taxYear(2024)
            .build();

        // When/Then
        mockMvc.perform(post("/api/v1/w1-filings/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(reconciliationService, never()).reconcileW1Filings(any(), anyInt(), anyList());
    }
}
