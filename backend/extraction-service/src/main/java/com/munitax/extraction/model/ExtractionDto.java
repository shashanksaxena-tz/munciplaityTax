package com.munitax.extraction.model;

import java.util.List;
import java.util.Map;

public class ExtractionDto {
    public record ExtractionRequest(String fileId, String fileName) {
    }

    public record ExtractionUpdate(
            String status, // SCANNING, ANALYZING, EXTRACTING, COMPLETE, ERROR
            int progress,
            List<String> log,
            List<String> detectedForms,
            double confidence,
            Object result,
            // Enhanced provenance and confidence tracking
            String currentFormType,
            String currentTaxpayerName,
            Map<String, FieldConfidence> fieldConfidences,
            List<FormProvenance> formProvenances,
            ExtractionSummary summary) {
        
        // Constructor for backward compatibility
        public ExtractionUpdate(String status, int progress, List<String> log,
                List<String> detectedForms, double confidence, Object result) {
            this(status, progress, log, detectedForms, confidence, result, null, null, null, null, null);
        }
    }

    /**
     * Confidence score for individual fields with weight classification
     */
    public record FieldConfidence(
            String fieldName,
            double confidence,        // 0.0 - 1.0
            String weight,            // CRITICAL, HIGH, MEDIUM, LOW
            double weightedScore,     // confidence * weight multiplier
            String extractionSource   // "AI_EXTRACTED", "DERIVED", "DEFAULT"
    ) {}

    /**
     * Document provenance tracking - where each field was extracted from
     */
    public record FormProvenance(
            String formType,
            int pageNumber,
            BoundingBox boundingBox,
            String extractionReason,
            double formConfidence,
            List<FieldProvenance> fields
    ) {}

    /**
     * Bounding box for visual highlighting in document
     */
    public record BoundingBox(
            double x,      // Left position (0-1 normalized)
            double y,      // Top position (0-1 normalized)
            double width,  // Width (0-1 normalized)
            double height  // Height (0-1 normalized)
    ) {}

    /**
     * Provenance for individual fields
     */
    public record FieldProvenance(
            String fieldName,
            int pageNumber,
            BoundingBox boundingBox,
            String rawValue,
            String processedValue,
            double confidence
    ) {}

    /**
     * Extraction summary for audit and review
     */
    public record ExtractionSummary(
            int totalPagesScanned,
            int formsExtracted,
            int formsSkipped,
            List<String> extractedFormTypes,
            List<SkippedForm> skippedForms,
            double overallConfidence,
            Map<String, Double> confidenceByFormType,
            long extractionDurationMs,
            String modelUsed
    ) {}

    /**
     * Information about forms that were detected but skipped
     */
    public record SkippedForm(
            String formType,
            int pageNumber,
            String reason,
            String suggestion
    ) {}
}
