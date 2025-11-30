package com.munitax.ledger.controller;

import com.munitax.ledger.enums.SourceType;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.service.TaxAssessmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaxAssessmentController.class)
@DisplayName("TaxAssessmentController Integration Tests")
class TaxAssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaxAssessmentService taxAssessmentService;

    @Test
    @DisplayName("T013 - POST /api/v1/tax-assessments/record should create tax assessment with journal entries")
    void shouldRecordTaxAssessment() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID filerId = UUID.randomUUID();
        UUID returnId = UUID.randomUUID();
        BigDecimal taxAmount = new BigDecimal("10000.00");
        BigDecimal penaltyAmount = new BigDecimal("500.00");
        BigDecimal interestAmount = new BigDecimal("150.00");
        String taxYear = "2024";
        String taxPeriod = "Q1";

        JournalEntry mockEntry = createMockJournalEntry(filerId, returnId);

        when(taxAssessmentService.recordTaxAssessment(
                eq(tenantId), eq(filerId), eq(returnId), 
                eq(taxAmount), eq(penaltyAmount), eq(interestAmount),
                eq(taxYear), eq(taxPeriod)
        )).thenReturn(mockEntry);

        // When/Then
        mockMvc.perform(post("/api/v1/tax-assessments/record")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("returnId", returnId.toString())
                        .param("taxAmount", taxAmount.toString())
                        .param("penaltyAmount", penaltyAmount.toString())
                        .param("interestAmount", interestAmount.toString())
                        .param("taxYear", taxYear)
                        .param("taxPeriod", taxPeriod))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").value(mockEntry.getEntryId().toString()))
                .andExpect(jsonPath("$.entityId").value(filerId.toString()))
                .andExpect(jsonPath("$.sourceType").value("TAX_ASSESSMENT"))
                .andExpect(jsonPath("$.sourceId").value(returnId.toString()));
    }

    @Test
    @DisplayName("T013 - POST /api/v1/tax-assessments/record with default penalty and interest")
    void shouldRecordTaxAssessmentWithDefaults() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID filerId = UUID.randomUUID();
        UUID returnId = UUID.randomUUID();
        BigDecimal taxAmount = new BigDecimal("5000.00");
        String taxYear = "2024";
        String taxPeriod = "Q2";

        JournalEntry mockEntry = createMockJournalEntry(filerId, returnId);

        when(taxAssessmentService.recordTaxAssessment(
                eq(tenantId), eq(filerId), eq(returnId), 
                eq(taxAmount), eq(BigDecimal.ZERO), eq(BigDecimal.ZERO),
                eq(taxYear), eq(taxPeriod)
        )).thenReturn(mockEntry);

        // When/Then
        mockMvc.perform(post("/api/v1/tax-assessments/record")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("returnId", returnId.toString())
                        .param("taxAmount", taxAmount.toString())
                        .param("taxYear", taxYear)
                        .param("taxPeriod", taxPeriod))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").exists())
                .andExpect(jsonPath("$.sourceType").value("TAX_ASSESSMENT"));
    }

    @Test
    @DisplayName("T013 - POST /api/v1/tax-assessments/record with compound assessment")
    void shouldRecordCompoundTaxAssessment() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID filerId = UUID.randomUUID();
        UUID returnId = UUID.randomUUID();
        BigDecimal taxAmount = new BigDecimal("15000.00");
        BigDecimal penaltyAmount = new BigDecimal("750.00");
        BigDecimal interestAmount = new BigDecimal("225.00");
        String taxYear = "2024";
        String taxPeriod = "Q3";

        JournalEntry mockEntry = createMockJournalEntry(filerId, returnId);

        when(taxAssessmentService.recordTaxAssessment(
                any(UUID.class), any(UUID.class), any(UUID.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
                any(String.class), any(String.class)
        )).thenReturn(mockEntry);

        // When/Then
        mockMvc.perform(post("/api/v1/tax-assessments/record")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("returnId", returnId.toString())
                        .param("taxAmount", taxAmount.toString())
                        .param("penaltyAmount", penaltyAmount.toString())
                        .param("interestAmount", interestAmount.toString())
                        .param("taxYear", taxYear)
                        .param("taxPeriod", taxPeriod))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").exists())
                .andExpect(jsonPath("$.entityId").value(filerId.toString()));
    }

    private JournalEntry createMockJournalEntry(UUID filerId, UUID returnId) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryId(UUID.randomUUID());
        entry.setEntityId(filerId);
        entry.setSourceType(SourceType.TAX_ASSESSMENT);
        entry.setSourceId(returnId);
        entry.setEntryDate(LocalDate.now());
        entry.setDescription("Test tax assessment");
        return entry;
    }
}
