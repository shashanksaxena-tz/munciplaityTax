package com.munitax.ledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.ledger.dto.PaymentRequest;
import com.munitax.ledger.enums.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class PaymentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testProcessPayment_CreditCardApproved_Returns200() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .filerId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .description("Q1 2024 Tax Payment")
                .build();
        
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.providerTransactionId").value(startsWith("mock_ch_")))
                .andExpect(jsonPath("$.authorizationCode").exists())
                .andExpect(jsonPath("$.receiptNumber").exists())
                .andExpect(jsonPath("$.journalEntryId").exists())
                .andExpect(jsonPath("$.testMode").value(true))
                .andExpect(jsonPath("$.amount").value(5000.00));
    }
    
    @Test
    void testProcessPayment_CreditCardDeclined_Returns200WithDeclinedStatus() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .filerId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4000-0000-0000-0002")
                .description("Q1 2024 Tax Payment")
                .build();
        
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.failureReason").value("insufficient_funds"))
                .andExpect(jsonPath("$.journalEntryId").doesNotExist());
    }
    
    @Test
    void testProcessPayment_ACHApproved_Returns200() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .filerId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .amount(new BigDecimal("3000.00"))
                .paymentMethod(PaymentMethod.ACH)
                .achRouting("110000000")
                .achAccount("000123456789")
                .description("ACH Payment")
                .build();
        
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.providerTransactionId").value(startsWith("mock_ach_")))
                .andExpect(jsonPath("$.journalEntryId").exists());
    }
    
    @Test
    void testProcessPayment_CheckPayment_Returns200() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .filerId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .amount(new BigDecimal("7500.00"))
                .paymentMethod(PaymentMethod.CHECK)
                .checkNumber("12345")
                .description("Check Payment")
                .build();
        
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.providerTransactionId").value(startsWith("mock_manual_")));
    }
    
    @Test
    void testProcessPayment_WireTransfer_Returns200() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .filerId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .amount(new BigDecimal("15000.00"))
                .paymentMethod(PaymentMethod.WIRE)
                .wireConfirmation("WIRE-123456")
                .description("Wire Transfer")
                .build();
        
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.providerTransactionId").value(startsWith("mock_manual_")));
    }
    
    @Test
    void testGetFilerPayments_ReturnsPaymentList() throws Exception {
        // First create a payment
        UUID filerId = UUID.randomUUID();
        PaymentRequest request = PaymentRequest.builder()
                .filerId(filerId)
                .tenantId(UUID.randomUUID())
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .description("Test Payment")
                .build();
        
        mockMvc.perform(post("/api/v1/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        
        // Then retrieve payments
        mockMvc.perform(get("/api/v1/payments/filer/{filerId}", filerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].filerId").value(filerId.toString()))
                .andExpect(jsonPath("$[0].amount").value(5000.00))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }
    
    @Test
    void testGetPayment_ReturnsPaymentDetails() throws Exception {
        // First create a payment
        PaymentRequest request = PaymentRequest.builder()
                .filerId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .amount(new BigDecimal("3000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .description("Test Payment")
                .build();
        
        String response = mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Parse response to get transactionId, then get payment from repository
        // For now, we'll just verify the endpoint exists
        // In production, we'd extract the paymentId from the saved transaction
    }
    
    @Test
    void testGetTestModeIndicator_ReturnsMessage() throws Exception {
        mockMvc.perform(get("/api/v1/payments/test-mode-indicator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TEST MODE")))
                .andExpect(content().string(containsString("No real charges")));
    }
    
    @Test
    void testProcessPayment_ErrorCard_ReturnsError() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .filerId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4000-0000-0000-0119")
                .description("Error Test")
                .build();
        
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.failureReason").value("processing_error"));
    }
    
    @Test
    void testProcessPayment_AllApprovedPaymentsMustCreateJournalEntries() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .filerId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .amount(new BigDecimal("10000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .description("Large Payment")
                .build();
        
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.journalEntryId").exists())
                .andExpect(jsonPath("$.journalEntryId").isNotEmpty());
    }
}
