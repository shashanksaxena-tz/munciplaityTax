package com.munitax.extraction.model;

import java.util.List;

public class ExtractionDto {
    public record ExtractionRequest(String fileId, String fileName) {
    }

    public record ExtractionUpdate(
            String status, // SCANNING, ANALYZING, EXTRACTING, COMPLETE
            int progress,
            List<String> log,
            List<String> detectedForms,
            double confidence,
            Object result) {
    }
}
