package com.munitax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Test credit card data for testing the mock payment gateway.
 * Used by GET /api/v1/payments/test-methods endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCreditCard {
    
    /**
     * Test card number (may be formatted with dashes)
     */
    private String cardNumber;
    
    /**
     * Card network type (VISA, MASTERCARD, AMEX)
     */
    private String cardType;
    
    /**
     * Expected outcome when this card is used (APPROVED, DECLINED, ERROR)
     */
    private String expectedResult;
    
    /**
     * Human-readable description of this test card
     */
    private String description;
}
