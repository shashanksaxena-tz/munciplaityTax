package com.munitax.pdf.domain;

/**
 * Form lifecycle status enum
 */
public enum FormStatus {
    DRAFT,        // Form generated with watermark, not finalized
    FINAL,        // Form finalized, watermark removed, ready for submission
    SUBMITTED,    // Form submitted to municipality
    AMENDED,      // Amended version of previously submitted form
    SUPERSEDED    // Older version replaced by newer version
}
