package com.munitax.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for document provenance data.
 * Contains field-level extraction data with locations and bounding boxes
 * for PDF highlighting functionality.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentProvenanceResponse {
    private String id;
    private String documentId;
    private String fileName;
    private String formType;
    private Integer pageCount;
    private Double extractionConfidence;
    private String fieldProvenance; // JSON string with field locations and bounding boxes
}
