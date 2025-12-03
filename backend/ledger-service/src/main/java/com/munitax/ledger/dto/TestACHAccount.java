package com.munitax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Test ACH account data for testing the mock payment gateway.
 * Used by GET /api/v1/payments/test-methods endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestACHAccount {
    
    /**
     * 9-digit ABA routing number
     */
    private String routingNumber;
    
    /**
     * Test account number
     */
    private String accountNumber;
    
    /**
     * Expected outcome when this account is used (APPROVED, DECLINED)
     */
    private String expectedResult;
    
    /**
     * Human-readable description of this test account
     */
    private String description;
}
