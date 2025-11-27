package com.munitax.taxengine.model;

public record TaxPayerProfile(
    String name,
    String ssn,
    Address address,
    String filingStatus, // SINGLE, MARRIED_FILING_JOINTLY, etc.
    SpouseInfo spouse
) {
    public record SpouseInfo(String name, String ssn) {}
}
