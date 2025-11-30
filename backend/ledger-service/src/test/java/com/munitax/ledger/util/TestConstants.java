package com.munitax.ledger.util;

import java.util.UUID;

/**
 * Test constants used across test classes
 * Provides consistent test data to prevent typos and improve maintainability
 */
public final class TestConstants {
    
    private TestConstants() {
        // Utility class - prevent instantiation
    }
    
    // Test Tenant ID - consistent across all tests
    public static final UUID TEST_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    
    // Test Credit Card Numbers
    public static final String APPROVED_VISA_CARD = "4111-1111-1111-1111";
    public static final String APPROVED_VISA_CARD_RAW = "4111111111111111";
    
    public static final String APPROVED_MASTERCARD = "5555-5555-5555-4444";
    public static final String APPROVED_MASTERCARD_RAW = "5555555555554444";
    
    public static final String APPROVED_AMEX = "3782-822463-10005";
    public static final String APPROVED_AMEX_RAW = "378282246310005";
    
    public static final String DECLINED_CARD = "4000-0000-0000-0002";
    public static final String DECLINED_CARD_RAW = "4000000000000002";
    
    public static final String ERROR_CARD = "4000-0000-0000-0119";
    public static final String ERROR_CARD_RAW = "4000000000000119";
    
    // Test ACH Account Numbers
    public static final String ACH_ROUTING_APPROVED = "110000000";
    public static final String ACH_ACCOUNT_APPROVED = "000123456789";
    
    public static final String ACH_ROUTING_DECLINED = "110000000";
    public static final String ACH_ACCOUNT_DECLINED = "000111111113";
}
