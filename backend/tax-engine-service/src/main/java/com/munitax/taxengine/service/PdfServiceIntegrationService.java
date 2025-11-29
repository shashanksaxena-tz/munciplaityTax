package com.munitax.taxengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for integrating with pdf-service to generate Form 27-PA and other tax forms.
 * 
 * Functional Requirements:
 * - FR-036: Generate Form 27-PA (Penalty Abatement Request) via pdf-service
 * - FR-038: Include penalty calculation details in generated form
 * 
 * The pdf-service is responsible for:
 * - Rendering tax forms from templates
 * - Populating forms with data
 * - Generating PDFs for download or archival
 * - Storing generated PDFs in document management system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfServiceIntegrationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${services.pdf-service.url:http://localhost:8088}")
    private String pdfServiceBaseUrl;
    
    @Value("${services.pdf-service.timeout:30000}")
    private int pdfServiceTimeout;
    
    /**
     * Generate Form 27-PA (Penalty Abatement Request).
     * FR-036: Generate form with taxpayer information and abatement request details.
     * 
     * @param abatementId          the abatement request ID
     * @param tenantId             the tenant (municipality) ID
     * @param returnId             the tax return ID
     * @param taxpayerName         taxpayer name
     * @param taxpayerId           taxpayer ID (SSN/FEIN)
     * @param requestedAmount      amount requested for abatement
     * @param reason               reason for abatement
     * @param explanation          detailed explanation
     * @param penaltyDetails       penalty calculation details
     * @return path to generated PDF or null if generation failed
     */
    public String generateForm27PA(
            UUID abatementId,
            UUID tenantId,
            UUID returnId,
            String taxpayerName,
            String taxpayerId,
            BigDecimal requestedAmount,
            String reason,
            String explanation,
            Map<String, Object> penaltyDetails) {
        
        try {
            String url = String.format("%s/api/pdf/forms/27-PA", pdfServiceBaseUrl);
            
            log.debug("Generating Form 27-PA for abatement: {}", abatementId);
            
            // Prepare form data
            Map<String, Object> formData = new HashMap<>();
            formData.put("abatementId", abatementId.toString());
            formData.put("tenantId", tenantId.toString());
            formData.put("returnId", returnId.toString());
            formData.put("taxpayerName", taxpayerName);
            formData.put("taxpayerId", taxpayerId);
            formData.put("requestedAmount", requestedAmount);
            formData.put("reason", reason);
            formData.put("explanation", explanation);
            formData.put("requestDate", LocalDate.now().toString());
            
            if (penaltyDetails != null) {
                formData.put("penaltyDetails", penaltyDetails);
            }
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(formData, headers);
            
            // Call pdf-service
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String pdfPath = (String) responseBody.get("pdfPath");
                
                log.info("Successfully generated Form 27-PA: {}", pdfPath);
                return pdfPath;
            } else {
                log.error("Failed to generate Form 27-PA. Status: {}", response.getStatusCode());
                return null;
            }
            
        } catch (RestClientException e) {
            log.error("Error generating Form 27-PA for abatement {}: {}", abatementId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Generate penalty calculation summary document.
     * 
     * @param returnId         the tax return ID
     * @param tenantId         the tenant ID
     * @param taxpayerName     taxpayer name
     * @param calculationData  penalty calculation data
     * @return path to generated PDF or null if generation failed
     */
    public String generatePenaltyCalculationSummary(
            UUID returnId,
            UUID tenantId,
            String taxpayerName,
            Map<String, Object> calculationData) {
        
        try {
            String url = String.format("%s/api/pdf/forms/penalty-summary", pdfServiceBaseUrl);
            
            log.debug("Generating penalty calculation summary for return: {}", returnId);
            
            // Prepare form data
            Map<String, Object> formData = new HashMap<>();
            formData.put("returnId", returnId.toString());
            formData.put("tenantId", tenantId.toString());
            formData.put("taxpayerName", taxpayerName);
            formData.put("calculationDate", LocalDate.now().toString());
            formData.put("calculationData", calculationData);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(formData, headers);
            
            // Call pdf-service
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String pdfPath = (String) responseBody.get("pdfPath");
                
                log.info("Successfully generated penalty calculation summary: {}", pdfPath);
                return pdfPath;
            } else {
                log.error("Failed to generate penalty calculation summary. Status: {}", response.getStatusCode());
                return null;
            }
            
        } catch (RestClientException e) {
            log.error("Error generating penalty calculation summary for return {}: {}", 
                    returnId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Generate interest calculation breakdown document.
     * 
     * @param returnId         the tax return ID
     * @param tenantId         the tenant ID
     * @param taxpayerName     taxpayer name
     * @param interestData     interest calculation data with quarterly breakdown
     * @return path to generated PDF or null if generation failed
     */
    public String generateInterestCalculationBreakdown(
            UUID returnId,
            UUID tenantId,
            String taxpayerName,
            Map<String, Object> interestData) {
        
        try {
            String url = String.format("%s/api/pdf/forms/interest-breakdown", pdfServiceBaseUrl);
            
            log.debug("Generating interest calculation breakdown for return: {}", returnId);
            
            // Prepare form data
            Map<String, Object> formData = new HashMap<>();
            formData.put("returnId", returnId.toString());
            formData.put("tenantId", tenantId.toString());
            formData.put("taxpayerName", taxpayerName);
            formData.put("calculationDate", LocalDate.now().toString());
            formData.put("interestData", interestData);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(formData, headers);
            
            // Call pdf-service
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String pdfPath = (String) responseBody.get("pdfPath");
                
                log.info("Successfully generated interest calculation breakdown: {}", pdfPath);
                return pdfPath;
            } else {
                log.error("Failed to generate interest calculation breakdown. Status: {}", response.getStatusCode());
                return null;
            }
            
        } catch (RestClientException e) {
            log.error("Error generating interest calculation breakdown for return {}: {}", 
                    returnId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Generate payment allocation receipt.
     * 
     * @param returnId         the tax return ID
     * @param tenantId         the tenant ID
     * @param taxpayerName     taxpayer name
     * @param allocationData   payment allocation data
     * @return path to generated PDF or null if generation failed
     */
    public String generatePaymentAllocationReceipt(
            UUID returnId,
            UUID tenantId,
            String taxpayerName,
            Map<String, Object> allocationData) {
        
        try {
            String url = String.format("%s/api/pdf/forms/payment-receipt", pdfServiceBaseUrl);
            
            log.debug("Generating payment allocation receipt for return: {}", returnId);
            
            // Prepare form data
            Map<String, Object> formData = new HashMap<>();
            formData.put("returnId", returnId.toString());
            formData.put("tenantId", tenantId.toString());
            formData.put("taxpayerName", taxpayerName);
            formData.put("receiptDate", LocalDate.now().toString());
            formData.put("allocationData", allocationData);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(formData, headers);
            
            // Call pdf-service
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String pdfPath = (String) responseBody.get("pdfPath");
                
                log.info("Successfully generated payment allocation receipt: {}", pdfPath);
                return pdfPath;
            } else {
                log.error("Failed to generate payment allocation receipt. Status: {}", response.getStatusCode());
                return null;
            }
            
        } catch (RestClientException e) {
            log.error("Error generating payment allocation receipt for return {}: {}", 
                    returnId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Download generated PDF from pdf-service.
     * 
     * @param pdfPath the path to the PDF file
     * @return byte array of PDF content or null if download failed
     */
    public byte[] downloadPdf(String pdfPath) {
        try {
            String url = String.format("%s/api/pdf/download?path=%s", pdfServiceBaseUrl, pdfPath);
            
            log.debug("Downloading PDF from: {}", pdfPath);
            
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Successfully downloaded PDF: {}", pdfPath);
                return response.getBody();
            } else {
                log.error("Failed to download PDF. Status: {}", response.getStatusCode());
                return null;
            }
            
        } catch (RestClientException e) {
            log.error("Error downloading PDF from path {}: {}", pdfPath, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if pdf-service is available.
     * 
     * @return true if service is reachable
     */
    public boolean isPdfServiceAvailable() {
        try {
            String url = String.format("%s/actuator/health", pdfServiceBaseUrl);
            restTemplate.getForObject(url, Map.class);
            return true;
        } catch (RestClientException e) {
            log.warn("PDF service is not available: {}", e.getMessage());
            return false;
        }
    }
}
