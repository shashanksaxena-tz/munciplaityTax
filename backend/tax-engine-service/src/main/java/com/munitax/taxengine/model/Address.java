package com.munitax.taxengine.model;

public record Address(
    String street,
    String city,
    String state,
    String zip,
    String country,
    String verificationStatus // UNVERIFIED, VERIFIED_IN_DISTRICT, etc.
) {}
