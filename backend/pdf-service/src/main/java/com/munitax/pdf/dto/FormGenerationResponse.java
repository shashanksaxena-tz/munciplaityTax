package com.munitax.pdf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for form generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormGenerationResponse {

    private UUID generatedFormId;
    private String formCode;
    private String formName;
    private Integer taxYear;
    private Integer version;
    private String status;
    private String pdfUrl;
    private String xmlUrl;
    private Integer pageCount;
    private Long fileSizeBytes;
    private Boolean isWatermarked;
    private LocalDateTime generatedDate;
    private String message;
    private Boolean success;
}
