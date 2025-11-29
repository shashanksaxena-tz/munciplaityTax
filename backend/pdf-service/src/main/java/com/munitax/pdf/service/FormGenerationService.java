package com.munitax.pdf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.pdf.domain.*;
import com.munitax.pdf.dto.FormGenerationRequest;
import com.munitax.pdf.dto.FormGenerationResponse;
import com.munitax.pdf.repository.FormAuditLogRepository;
import com.munitax.pdf.repository.GeneratedFormRepository;
import com.munitax.pdf.util.FormWatermarkUtil;
import com.munitax.pdf.util.PDFBoxHelper;
import com.munitax.pdf.util.PDFCompressionUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core service for generating PDF forms
 */
@Service
public class FormGenerationService {

    private static final Logger log = LoggerFactory.getLogger(FormGenerationService.class);

    @Value("${form.storage.base-path:${user.home}/munitax-forms}")
    private String storageBasePath;

    private final FormTemplateService formTemplateService;
    private final FieldMappingService fieldMappingService;
    private final FormValidationService formValidationService;
    private final GeneratedFormRepository generatedFormRepository;
    private final FormAuditLogRepository auditLogRepository;
    private final PDFBoxHelper pdfBoxHelper;
    private final FormWatermarkUtil watermarkUtil;
    private final PDFCompressionUtil compressionUtil;
    private final ObjectMapper objectMapper;

    @Autowired
    public FormGenerationService(
            FormTemplateService formTemplateService,
            FieldMappingService fieldMappingService,
            FormValidationService formValidationService,
            GeneratedFormRepository generatedFormRepository,
            FormAuditLogRepository auditLogRepository,
            PDFBoxHelper pdfBoxHelper,
            FormWatermarkUtil watermarkUtil,
            PDFCompressionUtil compressionUtil,
            ObjectMapper objectMapper) {
        this.formTemplateService = formTemplateService;
        this.fieldMappingService = fieldMappingService;
        this.formValidationService = formValidationService;
        this.generatedFormRepository = generatedFormRepository;
        this.auditLogRepository = auditLogRepository;
        this.pdfBoxHelper = pdfBoxHelper;
        this.watermarkUtil = watermarkUtil;
        this.compressionUtil = compressionUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate a form from template and data
     */
    @Transactional
    public FormGenerationResponse generateForm(FormGenerationRequest request) {
        PDDocument pdfDocument = null;
        try {
            log.info("Generating form {} for return {}", request.getFormCode(), request.getReturnId());

            // 1. Find template
            FormTemplate template = formTemplateService.findTemplateForForm(
                request.getFormCode(), 
                request.getTaxYear(), 
                request.getTenantId()
            ).orElseThrow(() -> new RuntimeException(
                String.format("Template not found for form %s, year %d", request.getFormCode(), request.getTaxYear())
            ));

            // 2. Validate form data
            JsonNode validationRules = formTemplateService.getValidationRules(template);
            List<String> validationErrors = formValidationService.validateFormData(
                request.getFormData(), 
                template, 
                validationRules
            );
            
            if (!validationErrors.isEmpty()) {
                return FormGenerationResponse.builder()
                    .success(false)
                    .message("Validation failed. Please check all required fields.")
                    .build();
            }

            // 3. Map data to PDF fields
            JsonNode fieldMappings = formTemplateService.getFieldMappings(template);
            Map<String, String> pdfFieldData = fieldMappingService.mapFormData(request.getFormData(), fieldMappings);

            // 4. Generate PDF
            pdfDocument = generatePDFDocument(template, pdfFieldData, request.getIncludeWatermark());

            // 5. Save PDF to storage
            String pdfFilePath = savePDFToStorage(
                pdfDocument, 
                request.getTenantId(), 
                request.getBusinessId(), 
                request.getFormCode(),
                request.getTaxYear()
            );

            // 6. Get actual file size after saving
            long actualFileSize = java.nio.file.Files.size(java.nio.file.Paths.get(pdfFilePath));

            // 7. Calculate version number
            Integer version = calculateNextVersion(request.getReturnId(), request.getFormCode());

            // 8. Save generated form entity
            GeneratedForm generatedForm = GeneratedForm.builder()
                .tenantId(request.getTenantId())
                .template(template)
                .returnId(request.getReturnId())
                .businessId(request.getBusinessId())
                .formCode(request.getFormCode())
                .taxYear(request.getTaxYear())
                .version(version)
                .status(FormStatus.DRAFT)
                .generatedBy(request.getUserId() != null ? request.getUserId() : "SYSTEM")
                .pdfFilePath(pdfFilePath)
                .isWatermarked(request.getIncludeWatermark())
                .pageCount(pdfDocument.getNumberOfPages())
                .fileSizeBytes(actualFileSize)
                .formData(objectMapper.writeValueAsString(request.getFormData()))
                .build();

            generatedForm = generatedFormRepository.save(generatedForm);

            // 9. Create audit log entry
            createAuditLogEntry(generatedForm, "GENERATED", "Form generated successfully");

            // 10. Build response
            return FormGenerationResponse.builder()
                .generatedFormId(generatedForm.getGeneratedFormId())
                .formCode(generatedForm.getFormCode())
                .formName(template.getFormName())
                .taxYear(generatedForm.getTaxYear())
                .version(generatedForm.getVersion())
                .status(generatedForm.getStatus().name())
                .pdfUrl("/api/forms/" + generatedForm.getGeneratedFormId() + "/download")
                .pageCount(generatedForm.getPageCount())
                .fileSizeBytes(generatedForm.getFileSizeBytes())
                .isWatermarked(generatedForm.getIsWatermarked())
                .generatedDate(generatedForm.getGeneratedDate())
                .success(true)
                .message("Form generated successfully")
                .build();

        } catch (Exception e) {
            log.error("Error generating form: {}", e.getMessage(), e);
            return FormGenerationResponse.builder()
                .success(false)
                .message("Error generating form. Please try again.")
                .build();
        } finally {
            // Always close PDF document to prevent resource leak
            if (pdfDocument != null) {
                try {
                    pdfDocument.close();
                } catch (IOException e) {
                    log.error("Error closing PDF document: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Generate PDF document from template
     */
    private PDDocument generatePDFDocument(FormTemplate template, Map<String, String> fieldData, Boolean includeWatermark) 
            throws IOException {
        // Load template (placeholder - actual implementation would load from storage)
        // For now, create a simple PDF
        PDDocument document = new PDDocument();
        
        // In production, you would:
        // 1. Load template PDF from storage path: template.getTemplateFilePath()
        // 2. Fill form fields using PDFBoxHelper
        // 3. Flatten the form
        
        // Placeholder: Add a blank page for demonstration
        document.addPage(new PDPage());
        
        // Add watermark if requested
        if (includeWatermark) {
            watermarkUtil.addDraftWatermark(document);
        }

        return document;
    }

    /**
     * Save PDF to file storage
     */
    private String savePDFToStorage(PDDocument document, String tenantId, UUID businessId, String formCode, Integer taxYear) 
            throws IOException {
        // Validate formCode to prevent path traversal
        if (!formCode.matches("^[a-zA-Z0-9-]+$")) {
            throw new IllegalArgumentException("Invalid form code format");
        }
        
        // Create directory structure
        Path dirPath = Paths.get(storageBasePath, "generated", tenantId, taxYear.toString(), businessId.toString());
        Files.createDirectories(dirPath);

        // Generate filename
        String filename = String.format("%s-%s.pdf", formCode, UUID.randomUUID().toString());
        Path filePath = dirPath.resolve(filename);

        // Save PDF
        document.save(filePath.toFile());
        log.info("Saved PDF to: {}", filePath);

        return filePath.toString();
    }

    /**
     * Calculate next version number for form
     */
    private Integer calculateNextVersion(UUID returnId, String formCode) {
        Optional<GeneratedForm> latestForm = generatedFormRepository.findLatestVersion(returnId, formCode);
        return latestForm.map(form -> form.getVersion() + 1).orElse(1);
    }

    /**
     * Create audit log entry
     */
    private void createAuditLogEntry(GeneratedForm form, String eventType, String description) {
        FormAuditLog auditLog = FormAuditLog.builder()
            .tenantId(form.getTenantId())
            .generatedFormId(form.getGeneratedFormId())
            .eventType(eventType)
            .eventDescription(description)
            .actorId(form.getGeneratedBy())
            .build();
        auditLogRepository.save(auditLog);
    }

    /**
     * Get generated form by ID
     */
    public Optional<GeneratedForm> getGeneratedForm(UUID formId) {
        return generatedFormRepository.findById(formId);
    }

    /**
     * Get PDF file for form with path validation
     */
    public File getPDFFile(GeneratedForm form) throws IOException {
        File pdfFile = new File(form.getPdfFilePath());
        
        // Validate that file is within expected storage directory
        Path expectedBasePath = Paths.get(storageBasePath, "generated").toAbsolutePath().normalize();
        Path actualPath = pdfFile.toPath().toAbsolutePath().normalize();
        
        if (!actualPath.startsWith(expectedBasePath)) {
            log.error("Security violation: Attempted to access file outside storage directory: {}", actualPath);
            throw new SecurityException("Invalid file path");
        }
        
        return pdfFile;
    }
}
