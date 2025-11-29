package com.munitax.pdf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for form generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormGenerationRequest {

    @NotBlank(message = "Form code is required")
    private String formCode;

    @NotNull(message = "Tax year is required")
    private Integer taxYear;

    @NotNull(message = "Return ID is required")
    private UUID returnId;

    @NotNull(message = "Business ID is required")
    private UUID businessId;

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotNull(message = "Form data is required")
    private Map<String, Object> formData;

    @Builder.Default
    private Boolean includeWatermark = true;

    @Builder.Default
    private Boolean generateXml = false;

    private String userId;
}
