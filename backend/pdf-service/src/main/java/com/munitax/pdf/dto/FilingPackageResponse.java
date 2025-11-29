package com.munitax.pdf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for filing package generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilingPackageResponse {

    private UUID packageId;
    private UUID returnId;
    private Integer taxYear;
    private String packageType;
    private Integer totalPages;
    private Integer formCount;
    private String packagePdfUrl;
    private Long fileSizeBytes;
    private Map<String, Integer> tableOfContents;
    private LocalDateTime createdDate;
    private String status;
    private String message;
    private Boolean success;
}
