package com.munitax.pdf.controller;

import com.munitax.pdf.domain.GeneratedForm;
import com.munitax.pdf.dto.FormGenerationRequest;
import com.munitax.pdf.dto.FormGenerationResponse;
import com.munitax.pdf.service.FormGenerationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.UUID;

/**
 * REST Controller for form generation operations
 */
@RestController
@RequestMapping("/api/forms")
public class FormGenerationController {

    private static final Logger log = LoggerFactory.getLogger(FormGenerationController.class);

    private final FormGenerationService formGenerationService;

    @Autowired
    public FormGenerationController(FormGenerationService formGenerationService) {
        this.formGenerationService = formGenerationService;
    }

    /**
     * Generate a new form
     * POST /api/forms/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<FormGenerationResponse> generateForm(@Valid @RequestBody FormGenerationRequest request) {
        log.info("Received form generation request for form={}, return={}", 
            request.getFormCode(), request.getReturnId());

        try {
            FormGenerationResponse response = formGenerationService.generateForm(request);
            HttpStatus status = response.getSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            log.error("Error in form generation endpoint: {}", e.getMessage(), e);
            FormGenerationResponse errorResponse = FormGenerationResponse.builder()
                .success(false)
                .message("An error occurred while generating the form. Please try again.")
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get form metadata by ID
     * GET /api/forms/{formId}
     */
    @GetMapping("/{formId}")
    public ResponseEntity<FormGenerationResponse> getForm(@PathVariable UUID formId) {
        log.info("Fetching form metadata for formId={}", formId);

        return formGenerationService.getGeneratedForm(formId)
            .map(form -> {
                FormGenerationResponse response = FormGenerationResponse.builder()
                    .generatedFormId(form.getGeneratedFormId())
                    .formCode(form.getFormCode())
                    .taxYear(form.getTaxYear())
                    .version(form.getVersion())
                    .status(form.getStatus().name())
                    .pdfUrl("/api/forms/" + form.getGeneratedFormId() + "/download")
                    .pageCount(form.getPageCount())
                    .fileSizeBytes(form.getFileSizeBytes())
                    .isWatermarked(form.getIsWatermarked())
                    .generatedDate(form.getGeneratedDate())
                    .success(true)
                    .build();
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Download generated PDF
     * GET /api/forms/{formId}/download
     */
    @GetMapping("/{formId}/download")
    public ResponseEntity<Resource> downloadForm(@PathVariable UUID formId) {
        log.info("Downloading PDF for formId={}", formId);

        return formGenerationService.getGeneratedForm(formId)
            .map(form -> {
                try {
                    File pdfFile = formGenerationService.getPDFFile(form);
                    if (!pdfFile.exists()) {
                        log.error("PDF file not found: {}", pdfFile.getAbsolutePath());
                        return ResponseEntity.<Resource>notFound().build();
                    }

                    Resource resource = new FileSystemResource(pdfFile);
                    String filename = String.format("Form-%s-%d-v%d.pdf", 
                        form.getFormCode(), form.getTaxYear(), form.getVersion());

                    return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .contentLength(pdfFile.length())
                        .body(resource);
                } catch (Exception e) {
                    log.error("Error downloading form: {}", e.getMessage(), e);
                    return ResponseEntity.<Resource>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Health check endpoint
     * GET /api/forms/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Form Generation Service is running");
    }
}
