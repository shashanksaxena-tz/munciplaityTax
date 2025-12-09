package com.munitax.ledger.service;

import com.munitax.ledger.dto.PaymentRequest;
import com.munitax.ledger.dto.PaymentResponse;
import com.munitax.ledger.dto.TestACHAccount;
import com.munitax.ledger.dto.TestCreditCard;
import com.munitax.ledger.dto.TestPaymentMethodsResponse;
import com.munitax.ledger.enums.PaymentMethod;
import com.munitax.ledger.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mock Payment Provider Service
 * Simulates payment processing without real charges
 * Test cards: 4111-1111-1111-1111 = APPROVED, 4000-0000-0000-0002 = DECLINED
 */
@Service
@Slf4j
public class MockPaymentProviderService {
    
    @Value("${ledger.payment.mode:TEST}")
    private String paymentMode;
    
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment in {} mode: amount={}, method={}", 
                paymentMode, request.getAmount(), request.getPaymentMethod());
        
        // Validate test mode
        if (!"TEST".equals(paymentMode)) {
            return createErrorResponse("Real payments not allowed. System in TEST mode only.");
        }
        
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(UUID.randomUUID());
        response.setAmount(request.getAmount());
        response.setTimestamp(LocalDateTime.now());
        response.setTestMode(true);
        
        if (request.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
            return processCreditCard(request, response);
        } else if (request.getPaymentMethod() == PaymentMethod.ACH) {
            return processACH(request, response);
        } else {
            // CHECK, WIRE - manual processing, always approved
            return processManualPayment(request, response);
        }
    }
    
    private PaymentResponse processCreditCard(PaymentRequest request, PaymentResponse response) {
        String cardNumber = request.getCardNumber().replaceAll("[^0-9]", "");
        
        // Test card scenarios
        if (cardNumber.equals("4242424242424242") || cardNumber.equals("4111111111111111") 
                || cardNumber.equals("5555555555554444") || cardNumber.equals("378282246310005")) {
            // Approved test cards (Visa, Mastercard, Amex)
            response.setStatus(PaymentStatus.APPROVED);
            response.setProviderTransactionId("mock_ch_" + UUID.randomUUID().toString().substring(0, 8));
            response.setAuthorizationCode("mock_auth_" + System.currentTimeMillis());
            response.setReceiptNumber("RCPT-" + System.currentTimeMillis());
            log.info("Credit card payment APPROVED: {}", response.getProviderTransactionId());
        } else if (cardNumber.equals("4000000000000002")) {
            // Declined test card
            response.setStatus(PaymentStatus.DECLINED);
            response.setFailureReason("insufficient_funds");
            log.info("Credit card payment DECLINED: insufficient funds");
        } else if (cardNumber.equals("4000000000000119")) {
            // Error test card
            response.setStatus(PaymentStatus.ERROR);
            response.setFailureReason("processing_error");
            log.info("Credit card payment ERROR: processing error");
        } else {
            // Unknown test card - decline
            response.setStatus(PaymentStatus.DECLINED);
            response.setFailureReason("invalid_card");
            log.info("Credit card payment DECLINED: invalid test card");
        }
        
        return response;
    }
    
    private PaymentResponse processACH(PaymentRequest request, PaymentResponse response) {
        String routing = request.getAchRouting();
        String account = request.getAchAccount();
        
        // Test ACH scenarios
        if ("110000000".equals(routing) && "000123456789".equals(account)) {
            // Approved test account
            response.setStatus(PaymentStatus.APPROVED);
            response.setProviderTransactionId("mock_ach_" + UUID.randomUUID().toString().substring(0, 8));
            response.setAuthorizationCode("mock_auth_" + System.currentTimeMillis());
            response.setReceiptNumber("RCPT-" + System.currentTimeMillis());
            log.info("ACH payment APPROVED: {}", response.getProviderTransactionId());
        } else if ("110000000".equals(routing) && "000111111113".equals(account)) {
            // Declined test account
            response.setStatus(PaymentStatus.DECLINED);
            response.setFailureReason("insufficient_funds");
            log.info("ACH payment DECLINED: insufficient funds");
        } else {
            // Unknown test account - decline
            response.setStatus(PaymentStatus.DECLINED);
            response.setFailureReason("invalid_account");
            log.info("ACH payment DECLINED: invalid test account");
        }
        
        return response;
    }
    
    private PaymentResponse processManualPayment(PaymentRequest request, PaymentResponse response) {
        // Manual payments (check, wire) are always approved
        response.setStatus(PaymentStatus.APPROVED);
        response.setProviderTransactionId("mock_manual_" + UUID.randomUUID().toString().substring(0, 8));
        response.setAuthorizationCode("manual_" + System.currentTimeMillis());
        response.setReceiptNumber("RCPT-" + System.currentTimeMillis());
        log.info("Manual payment APPROVED: {}", response.getProviderTransactionId());
        return response;
    }
    
    private PaymentResponse createErrorResponse(String errorMessage) {
        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.ERROR);
        response.setFailureReason(errorMessage);
        response.setTimestamp(LocalDateTime.now());
        response.setTestMode(true);
        return response;
    }
    
    /**
     * Returns available test payment methods for the mock payment gateway.
     * In TEST mode: Returns populated lists of test credit cards and ACH accounts.
     * In PRODUCTION mode: Returns empty lists for security.
     * 
     * @return TestPaymentMethodsResponse containing test cards and ACH accounts
     */
    public TestPaymentMethodsResponse getTestPaymentMethods() {
        if (!"TEST".equals(paymentMode)) {
            log.info("Test payment methods requested in PRODUCTION mode - returning empty response");
            return TestPaymentMethodsResponse.builder()
                    .creditCards(List.of())
                    .achAccounts(List.of())
                    .testMode(false)
                    .build();
        }
        
        log.info("Returning test payment methods for TEST mode");
        
        List<TestCreditCard> testCards = List.of(
            TestCreditCard.builder()
                    .cardNumber("4242-4242-4242-4242")
                    .cardType("VISA")
                    .expectedResult("APPROVED")
                    .description("Standard Visa test card - always approved")
                    .build(),
            TestCreditCard.builder()
                    .cardNumber("4111-1111-1111-1111")
                    .cardType("VISA")
                    .expectedResult("APPROVED")
                    .description("Alternative Visa test card - always approved")
                    .build(),
            TestCreditCard.builder()
                    .cardNumber("5555-5555-5555-4444")
                    .cardType("MASTERCARD")
                    .expectedResult("APPROVED")
                    .description("Standard Mastercard test card - always approved")
                    .build(),
            TestCreditCard.builder()
                    .cardNumber("378282246310005")
                    .cardType("AMEX")
                    .expectedResult("APPROVED")
                    .description("American Express test card - always approved")
                    .build(),
            TestCreditCard.builder()
                    .cardNumber("4000-0000-0000-0002")
                    .cardType("VISA")
                    .expectedResult("DECLINED")
                    .description("Declined test card - insufficient funds")
                    .build(),
            TestCreditCard.builder()
                    .cardNumber("4000-0000-0000-0119")
                    .cardType("VISA")
                    .expectedResult("ERROR")
                    .description("Error test card - processing error")
                    .build()
        );
        
        List<TestACHAccount> testAccounts = List.of(
            TestACHAccount.builder()
                    .routingNumber("110000000")
                    .accountNumber("000123456789")
                    .expectedResult("APPROVED")
                    .description("Standard ACH test account - always approved")
                    .build(),
            TestACHAccount.builder()
                    .routingNumber("110000000")
                    .accountNumber("000111111113")
                    .expectedResult("DECLINED")
                    .description("Declined ACH test account - insufficient funds")
                    .build()
        );
        
        return TestPaymentMethodsResponse.builder()
                .creditCards(testCards)
                .achAccounts(testAccounts)
                .testMode(true)
                .build();
    }
}
