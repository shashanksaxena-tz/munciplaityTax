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
}
