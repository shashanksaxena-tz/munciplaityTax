package com.munitax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing available test payment methods.
 * Used by GET /api/v1/payments/test-methods endpoint.
 * 
 * In TEST mode: Returns populated lists of test cards and ACH accounts
 * In PRODUCTION mode: Returns empty lists
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestPaymentMethodsResponse {
    
    /**
     * List of test credit cards
     */
    private List<TestCreditCard> creditCards;
    
    /**
     * List of test ACH accounts
     */
    private List<TestACHAccount> achAccounts;
    
    /**
     * Indicates whether system is in test mode
     */
    private boolean testMode;
}
