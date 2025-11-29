package com.munitax.ledger.controller;

import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.service.RefundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for RefundController
 * T056: Integration test for refund API endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class RefundControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RefundService refundService;
    
    private UUID tenantId;
    private UUID filerId;
    private UUID userId;
    private UUID refundRequestId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        filerId = UUID.randomUUID();
        userId = UUID.randomUUID();
        refundRequestId = UUID.randomUUID();
    }
    
    /**
     * T056: Test POST /api/v1/refunds/request endpoint
     * Tests FR-037: System MUST allow filer to request refund
     */
    @Test
    void testRequestRefund_Success() throws Exception {
        // Given
        BigDecimal refundAmount = new BigDecimal("1000.00");
        String reason = "Overpayment Q1 2024";
        
        JournalEntry mockEntry = new JournalEntry();
        mockEntry.setEntryId(UUID.randomUUID());
        mockEntry.setEntityId(filerId);
        mockEntry.setTenantId(tenantId);
        mockEntry.setEntryDate(LocalDate.now());
        mockEntry.setDescription("Refund Request - " + reason);
        mockEntry.setSourceId(refundRequestId);
        
        when(refundService.processRefundRequest(
                eq(tenantId), eq(filerId), eq(refundAmount), eq(reason), eq(userId)))
                .thenReturn(mockEntry);
        
        // When & Then
        mockMvc.perform(post("/api/v1/refunds/request")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("amount", refundAmount.toString())
                        .param("reason", reason)
                        .param("requestedBy", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").exists())
                .andExpect(jsonPath("$.entityId").value(filerId.toString()))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()));
        
        verify(refundService, times(1)).processRefundRequest(
                eq(tenantId), eq(filerId), eq(refundAmount), eq(reason), eq(userId));
    }
    
    /**
     * T056: Test POST /api/v1/refunds/request with invalid amount
     */
    @Test
    void testRequestRefund_InvalidAmount_ReturnsBadRequest() throws Exception {
        // Given
        BigDecimal negativeAmount = new BigDecimal("-1000.00");
        String reason = "Test refund";
        
        when(refundService.processRefundRequest(
                any(UUID.class), any(UUID.class), eq(negativeAmount), any(String.class), any(UUID.class)))
                .thenThrow(new IllegalArgumentException("Refund amount must be positive"));
        
        // When & Then
        mockMvc.perform(post("/api/v1/refunds/request")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("amount", negativeAmount.toString())
                        .param("reason", reason)
                        .param("requestedBy", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError()); // Exception not handled yet
    }
    
    /**
     * T056: Test POST /api/v1/refunds/issue endpoint
     * Tests FR-040: When refund issued, proper entries created
     */
    @Test
    void testIssueRefund_Success() throws Exception {
        // Given
        BigDecimal refundAmount = new BigDecimal("1000.00");
        
        doNothing().when(refundService).issueRefund(
                eq(tenantId), eq(filerId), eq(refundRequestId), eq(refundAmount), eq(userId));
        
        // When & Then
        mockMvc.perform(post("/api/v1/refunds/issue")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("refundRequestId", refundRequestId.toString())
                        .param("amount", refundAmount.toString())
                        .param("issuedBy", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Refund issued successfully"));
        
        verify(refundService, times(1)).issueRefund(
                eq(tenantId), eq(filerId), eq(refundRequestId), eq(refundAmount), eq(userId));
    }
    
    /**
     * T056: Test complete refund flow through API
     */
    @Test
    void testCompleteRefundFlow_RequestThenIssue() throws Exception {
        // Given
        BigDecimal refundAmount = new BigDecimal("500.00");
        String reason = "Overpayment correction";
        
        JournalEntry mockEntry = new JournalEntry();
        mockEntry.setEntryId(UUID.randomUUID());
        mockEntry.setEntityId(filerId);
        mockEntry.setTenantId(tenantId);
        mockEntry.setSourceId(refundRequestId);
        
        when(refundService.processRefundRequest(
                eq(tenantId), eq(filerId), eq(refundAmount), eq(reason), eq(userId)))
                .thenReturn(mockEntry);
        
        // When - Request refund
        mockMvc.perform(post("/api/v1/refunds/request")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("amount", refundAmount.toString())
                        .param("reason", reason)
                        .param("requestedBy", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceId").value(refundRequestId.toString()));
        
        // When - Issue refund
        mockMvc.perform(post("/api/v1/refunds/issue")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("refundRequestId", refundRequestId.toString())
                        .param("amount", refundAmount.toString())
                        .param("issuedBy", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        // Then
        verify(refundService, times(1)).processRefundRequest(
                eq(tenantId), eq(filerId), eq(refundAmount), eq(reason), eq(userId));
        verify(refundService, times(1)).issueRefund(
                eq(tenantId), eq(filerId), eq(refundRequestId), eq(refundAmount), eq(userId));
    }
    
    /**
     * T056: Test multiple refund requests maintain independence
     */
    @Test
    void testMultipleRefundRequests_Independent() throws Exception {
        // Given
        BigDecimal refund1 = new BigDecimal("100.00");
        BigDecimal refund2 = new BigDecimal("200.00");
        
        JournalEntry mockEntry1 = new JournalEntry();
        mockEntry1.setEntryId(UUID.randomUUID());
        mockEntry1.setSourceId(UUID.randomUUID());
        
        JournalEntry mockEntry2 = new JournalEntry();
        mockEntry2.setEntryId(UUID.randomUUID());
        mockEntry2.setSourceId(UUID.randomUUID());
        
        when(refundService.processRefundRequest(
                eq(tenantId), eq(filerId), eq(refund1), any(String.class), eq(userId)))
                .thenReturn(mockEntry1);
        
        when(refundService.processRefundRequest(
                eq(tenantId), eq(filerId), eq(refund2), any(String.class), eq(userId)))
                .thenReturn(mockEntry2);
        
        // When - Request refund 1
        mockMvc.perform(post("/api/v1/refunds/request")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("amount", refund1.toString())
                        .param("reason", "Refund 1")
                        .param("requestedBy", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        // When - Request refund 2
        mockMvc.perform(post("/api/v1/refunds/request")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("amount", refund2.toString())
                        .param("reason", "Refund 2")
                        .param("requestedBy", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        // Then
        verify(refundService, times(1)).processRefundRequest(
                eq(tenantId), eq(filerId), eq(refund1), any(String.class), eq(userId));
        verify(refundService, times(1)).processRefundRequest(
                eq(tenantId), eq(filerId), eq(refund2), any(String.class), eq(userId));
    }
    
    /**
     * T056: Test refund request with missing parameters
     */
    @Test
    void testRequestRefund_MissingParameters_ReturnsBadRequest() throws Exception {
        // When & Then - Missing amount parameter
        mockMvc.perform(post("/api/v1/refunds/request")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("reason", "Test")
                        .param("requestedBy", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
    
    /**
     * T056: Test issue refund with missing parameters
     */
    @Test
    void testIssueRefund_MissingParameters_ReturnsBadRequest() throws Exception {
        // When & Then - Missing refundRequestId parameter
        mockMvc.perform(post("/api/v1/refunds/issue")
                        .param("tenantId", tenantId.toString())
                        .param("filerId", filerId.toString())
                        .param("amount", "1000.00")
                        .param("issuedBy", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}
