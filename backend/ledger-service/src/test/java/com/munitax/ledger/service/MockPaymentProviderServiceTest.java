package com.munitax.ledger.service;

import com.munitax.ledger.dto.PaymentRequest;
import com.munitax.ledger.dto.PaymentResponse;
import com.munitax.ledger.enums.PaymentMethod;
import com.munitax.ledger.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MockPaymentProviderServiceTest {
    
    private MockPaymentProviderService paymentProvider;
    
    @BeforeEach
    void setUp() {
        paymentProvider = new MockPaymentProviderService();
        ReflectionTestUtils.setField(paymentProvider, "paymentMode", "TEST");
    }
    
    @Test
    void testCreditCardApproved() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertNotNull(response.getProviderTransactionId());
        assertTrue(response.getProviderTransactionId().startsWith("mock_ch_"));
        assertNotNull(response.getAuthorizationCode());
        assertTrue(response.isTestMode());
    }
    
    @Test
    void testCreditCardDeclined() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4000-0000-0000-0002")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.DECLINED, response.getStatus());
        assertEquals("insufficient_funds", response.getFailureReason());
    }
    
    @Test
    void testCreditCardError() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4000-0000-0000-0119")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.ERROR, response.getStatus());
        assertEquals("processing_error", response.getFailureReason());
    }
    
    @Test
    void testACHApproved() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.ACH)
                .achRouting("110000000")
                .achAccount("000123456789")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertNotNull(response.getProviderTransactionId());
        assertTrue(response.getProviderTransactionId().startsWith("mock_ach_"));
    }
    
    @Test
    void testACHDeclined() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.ACH)
                .achRouting("110000000")
                .achAccount("000111111113")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.DECLINED, response.getStatus());
        assertEquals("insufficient_funds", response.getFailureReason());
    }
    
    @Test
    void testMasterCardApproved() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("5555-5555-5555-4444")
                .cardholderName("John Doe")
                .expirationMonth(12)
                .expirationYear(2025)
                .cvv("123")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertNotNull(response.getProviderTransactionId());
        assertTrue(response.getProviderTransactionId().startsWith("mock_ch_"));
        assertNotNull(response.getAuthorizationCode());
        assertNotNull(response.getReceiptNumber());
        assertTrue(response.getReceiptNumber().startsWith("RCPT-"));
        assertTrue(response.isTestMode());
        assertEquals(new BigDecimal("5000.00"), response.getAmount());
    }
    
    @Test
    void testAmexApproved() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("2500.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("378282246310005")
                .cardholderName("Jane Smith")
                .expirationMonth(6)
                .expirationYear(2026)
                .cvv("1234")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertTrue(response.isTestMode());
    }
    
    @Test
    void testInvalidTestCard() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("1234-5678-9012-3456")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.DECLINED, response.getStatus());
        assertEquals("invalid_card", response.getFailureReason());
    }
    
    @Test
    void testACHInvalidAccount() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.ACH)
                .achRouting("999999999")
                .achAccount("000999999999")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.DECLINED, response.getStatus());
        assertEquals("invalid_account", response.getFailureReason());
    }
    
    @Test
    void testCheckPaymentApproved() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("7500.00"))
                .paymentMethod(PaymentMethod.CHECK)
                .checkNumber("12345")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertNotNull(response.getProviderTransactionId());
        assertTrue(response.getProviderTransactionId().startsWith("mock_manual_"));
        assertTrue(response.isTestMode());
    }
    
    @Test
    void testWireTransferApproved() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("15000.00"))
                .paymentMethod(PaymentMethod.WIRE)
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertNotNull(response);
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertNotNull(response.getProviderTransactionId());
        assertTrue(response.getProviderTransactionId().startsWith("mock_manual_"));
        assertTrue(response.isTestMode());
    }
    
    @Test
    void testAllApprovedPaymentsShouldHaveReceiptNumber() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertNotNull(response.getReceiptNumber());
        assertTrue(response.getReceiptNumber().startsWith("RCPT-"));
    }
    
    @Test
    void testAllApprovedPaymentsShouldHaveTimestamp() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111-1111-1111-1111")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testDeclinedPaymentsShouldNotHaveAuthorizationCode() {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4000-0000-0000-0002")
                .build();
        
        PaymentResponse response = paymentProvider.processPayment(request);
        
        assertEquals(PaymentStatus.DECLINED, response.getStatus());
        assertNull(response.getAuthorizationCode());
    }
}
