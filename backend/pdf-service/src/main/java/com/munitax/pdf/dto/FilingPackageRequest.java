package com.munitax.pdf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for filing package generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilingPackageRequest {

    @NotNull(message = "Return ID is required")
    private UUID returnId;

    @NotNull(message = "Business ID is required")
    private UUID businessId;

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotNull(message = "Tax year is required")
    private Integer taxYear;

    @NotBlank(message = "Package type is required")
    private String packageType;

    @NotEmpty(message = "At least one form must be included")
    private List<UUID> includedFormIds;

    @Builder.Default
    private Boolean generateTableOfContents = true;

    @Builder.Default
    private Boolean addBookmarks = true;

    @Builder.Default
    private Boolean optimizeFileSize = true;

    private String userId;
}
