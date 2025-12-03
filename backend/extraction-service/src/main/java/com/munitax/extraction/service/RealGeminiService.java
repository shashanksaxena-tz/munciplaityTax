package com.munitax.extraction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.extraction.model.ExtractionDto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;

/**
 * Production-ready Gemini AI Extraction Service for Municipality Tax Forms.
 * 
 * Supports extraction from:
 * - Individual Forms: Federal 1040, W-2, 1099-NEC, 1099-MISC, W-2G, 
 *   Schedule C/E/F, Local 1040, Form R
 * - Business Forms: Federal 1120, Federal 1065, Form 27 (Net Profits),
 *   Form W-1 (Withholding), Form W-3 (Reconciliation)
 * 
 * Key Features:
 * - User-provided API keys (per-session, never persisted)
 * - Confidence scoring with field-level weights
 * - Document provenance tracking (page numbers, bounding boxes)
 * - Real-time extraction feedback
 * 
 * @version 2.0.0
 * @see <a href="https://ai.google.dev/gemini-api">Gemini API Documentation</a>
 */
@Service
public class RealGeminiService {

    private static final Logger log = LoggerFactory.getLogger(RealGeminiService.class);

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    // Use gemini-2.0-flash for stable document processing (1M token context, multimodal support)
    // Alternative models: gemini-1.5-flash-latest, gemini-2.5-flash, gemini-2.5-pro
    // See: https://ai.google.dev/gemini-api/docs/models
    private static final String DEFAULT_MODEL = "gemini-2.0-flash";
    
    // Field weight classifications for confidence scoring
    private static final Map<String, String> FIELD_WEIGHTS = initializeFieldWeights();

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String defaultApiKey;

    @Value("${gemini.api.model:" + DEFAULT_MODEL + "}")
    private String defaultModel;

    public RealGeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl(GEMINI_BASE_URL)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Extract tax data from document using default API key from configuration.
     */
    public Flux<ExtractionUpdate> extractData(String fileName, String base64Data, String mimeType) {
        return extractData(fileName, base64Data, mimeType, null, null);
    }

    /**
     * Extract tax data from document using user-provided API key.
     * 
     * @param fileName Original file name
     * @param base64Data Base64-encoded file content
     * @param mimeType MIME type of the file
     * @param userApiKey User-provided Gemini API key (optional, uses default if null)
     * @param modelOverride Model to use (optional, uses default if null)
     * @return Flux of extraction updates for real-time feedback
     */
    public Flux<ExtractionUpdate> extractData(
            String fileName, 
            String base64Data, 
            String mimeType,
            String userApiKey,
            String modelOverride) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Determine which API key to use - prefer user-provided
            String apiKey = resolveApiKey(userApiKey);
            String model = modelOverride != null && !modelOverride.trim().isEmpty() 
                    ? modelOverride.trim() 
                    : defaultModel;
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.info("Gemini API Key is missing. Using mock extraction.");
                return mockExtraction(fileName, startTime);
            }

            if (base64Data == null) {
                log.info("No file data provided. Using mock extraction.");
                return mockExtraction(fileName, startTime);
            }

            String trimmedKey = apiKey.trim();
            logApiUsage(trimmedKey, model);

            String prompt = buildProductionExtractionPrompt();

            // Build request using HashMap to avoid Map.of() entry limit
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", base64Data);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> dataPart = new HashMap<>();
            dataPart.put("inline_data", inlineData);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(textPart, dataPart));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("response_mime_type", "application/json");
            generationConfig.put("temperature", 0.1);
            generationConfig.put("topP", 0.95);
            generationConfig.put("topK", 40);

            Map<String, Object> request = new HashMap<>();
            request.put("contents", List.of(content));
            request.put("generationConfig", generationConfig);

            return webClient.post()
                    .uri("/models/" + model + ":generateContent?key=" + trimmedKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchangeToFlux(response -> {
                        if (response.statusCode().isError()) {
                            // Extract error message from response body
                            return response.bodyToMono(String.class)
                                    .flatMapMany(errorBody -> {
                                        String errorMessage = extractGeminiErrorMessage(errorBody, response.statusCode().value());
                                        log.error("Gemini API Error (HTTP {}): {}", response.statusCode().value(), errorMessage);
                                        return createErrorResponse(errorMessage, startTime);
                                    })
                                    .onErrorResume(e -> {
                                        String fallbackMessage = "API Error (HTTP " + response.statusCode().value() + ")";
                                        log.error("Failed to parse error response: {}", e.getMessage());
                                        return createErrorResponse(fallbackMessage, startTime);
                                    });
                        }
                        // Use bodyToMono to ensure we get the complete JSON response before parsing
                        // bodyToFlux(String.class) can split the response into chunks, causing malformed JSON errors
                        return response.bodyToMono(String.class)
                                .flatMapMany(body -> parseStreamingResponse(body, startTime, model));
                    })
                    .onErrorResume(error -> {
                        log.error("Gemini API Error: {}", error.getMessage(), error);
                        return createErrorResponse(error.getMessage(), startTime);
                    });
        } catch (Exception e) {
            log.error("Unexpected error in extractData", e);
            return createErrorResponse(e.getMessage(), startTime);
        }
    }

    /**
     * Extract error message from Gemini API error response.
     * Expected format: {"error":{"code":400,"message":"API key not valid...","status":"INVALID_ARGUMENT"}}
     */
    private String extractGeminiErrorMessage(String errorBody, int httpStatus) {
        try {
            if (errorBody == null || errorBody.trim().isEmpty()) {
                return "API Error (HTTP " + httpStatus + ")";
            }
            
            JsonNode root = objectMapper.readTree(errorBody);
            JsonNode error = root.path("error");
            
            if (!error.isMissingNode()) {
                String message = error.path("message").asText("");
                String status = error.path("status").asText("");
                int code = error.path("code").asInt(httpStatus);
                
                if (!message.isEmpty()) {
                    // Return the actual API error message (e.g., "API key not valid...")
                    log.debug("Gemini API error - Code: {}, Status: {}, Message: {}", code, status, message);
                    return message;
                }
            }
            
            // Fallback: return truncated raw body
            String truncated = errorBody.length() > 200 ? errorBody.substring(0, 200) + "..." : errorBody;
            return "API Error: " + truncated;
            
        } catch (Exception e) {
            log.warn("Failed to parse error body: {}", e.getMessage());
            return "API Error (HTTP " + httpStatus + "): " + 
                   (errorBody != null ? errorBody.substring(0, Math.min(100, errorBody.length())) : "Unknown");
        }
    }

    private String resolveApiKey(String userApiKey) {
        if (userApiKey != null && !userApiKey.trim().isEmpty()) {
            return userApiKey.trim();
        }
        return defaultApiKey;
    }

    private void logApiUsage(String apiKey, String model) {
        // Log only first/last 5 chars for security
        String maskedKey = apiKey.length() > 10 
                ? apiKey.substring(0, 5) + "..." + apiKey.substring(apiKey.length() - 5)
                : "***";
        log.info("Using Gemini API Key: {}", maskedKey);
        log.info("Using Gemini Model: {}", model);
    }

    private Flux<ExtractionUpdate> parseStreamingResponse(
            String jsonResponse, 
            long startTime, 
            String model) {
        try {
            // Validate response is not empty or malformed
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("Empty response received from Gemini API");
                return createErrorResponse("Empty response from API", startTime);
            }

            // Check for common malformed responses
            String trimmedResponse = jsonResponse.trim();
            if (trimmedResponse.equals("}") || trimmedResponse.equals("{") || 
                trimmedResponse.length() < 10) {
                log.warn("Malformed response received: {}", trimmedResponse);
                return createErrorResponse("Malformed API response. Please try again.", startTime);
            }

            JsonNode root = objectMapper.readTree(jsonResponse);
            
            // Check for API error in response
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String errorMessage = error.path("message").asText("Unknown API error");
                int errorCode = error.path("code").asInt(0);
                log.error("Gemini API returned error: {} (code: {})", errorMessage, errorCode);
                return createErrorResponse(errorMessage, startTime);
            }
            
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode candidate = candidates.get(0);
                
                // Check for finishReason to handle safety blocks or other stop reasons
                String finishReason = candidate.path("finishReason").asText("");
                if ("SAFETY".equals(finishReason)) {
                    log.warn("Gemini API blocked content due to safety settings");
                    return createErrorResponse("Content blocked by safety filters. Please ensure the document is appropriate.", startTime);
                }
                if ("RECITATION".equals(finishReason)) {
                    log.warn("Gemini API blocked content due to recitation check");
                    return createErrorResponse("Content blocked by copyright/recitation filters.", startTime);
                }

                JsonNode content = candidate.path("content");
                JsonNode parts = content.path("parts");

                if (parts.isArray() && !parts.isEmpty()) {
                    String text = parts.get(0).path("text").asText();
                    
                    // Validate extracted text is valid JSON
                    if (text == null || text.trim().isEmpty()) {
                        log.warn("Empty text content in Gemini response");
                        return createErrorResponse("No data extracted from document", startTime);
                    }

                    // Parse extracted forms with additional validation
                    JsonNode extractedData;
                    try {
                        extractedData = objectMapper.readTree(text);
                    } catch (Exception parseEx) {
                        log.error("Failed to parse extracted data as JSON: {}", parseEx.getMessage());
                        log.debug("Raw text that failed parsing: {}", text.substring(0, Math.min(500, text.length())));
                        return createErrorResponse("Invalid JSON in extraction result. Please try again.", startTime);
                    }
                    
                    JsonNode forms = extractedData.path("forms");

                    List<String> detectedForms = new ArrayList<>();
                    List<FormProvenance> provenances = new ArrayList<>();
                    Map<String, Double> confidenceByForm = new HashMap<>();
                    String taxpayerName = extractTaxpayerName(extractedData);
                    int pageCount = extractedData.path("scanMetadata").path("pageCount").asInt(1);

                    if (forms.isArray()) {
                        forms.forEach(form -> {
                            String formType = form.path("formType").asText();
                            if (!formType.isEmpty()) {
                                detectedForms.add(formType);
                                double confidence = form.path("confidenceScore").asDouble(0.8);
                                confidenceByForm.put(formType, confidence);
                                provenances.add(buildFormProvenance(form));
                            }
                        });
                    }

                    // Build extraction summary
                    long duration = System.currentTimeMillis() - startTime;
                    double overallConfidence = calculateOverallConfidence(confidenceByForm);
                    ExtractionSummary summary = new ExtractionSummary(
                            pageCount,
                            detectedForms.size(),
                            0, // TODO: Track skipped forms
                            detectedForms,
                            List.of(), // Skipped forms
                            overallConfidence,
                            confidenceByForm,
                            duration,
                            model
                    );

                    // Build field confidences with weights
                    Map<String, FieldConfidence> fieldConfidences = buildFieldConfidences(extractedData);

                    return Flux.just(
                            new ExtractionUpdate("SCANNING", 15, 
                                    List.of("Scanning document structure..."),
                                    List.of(), 0.0, null, null, null, null, null, null),
                            new ExtractionUpdate("ANALYZING", 35, 
                                    List.of("Analyzing with Gemini 2.5 Flash AI...", "Identifying tax forms..."),
                                    List.of(), 0.0, null, null, taxpayerName, null, null, null),
                            new ExtractionUpdate("EXTRACTING", 55, 
                                    List.of("Detected " + detectedForms.size() + " form(s)", "Extracting critical fields..."),
                                    detectedForms, 0.7, null, 
                                    detectedForms.isEmpty() ? null : detectedForms.get(0),
                                    taxpayerName, null, provenances, null),
                            new ExtractionUpdate("EXTRACTING", 75, 
                                    List.of("Validating extracted data...", "Computing confidence scores..."),
                                    detectedForms, 0.85, null, null, taxpayerName, fieldConfidences, provenances, null),
                            new ExtractionUpdate("COMPLETE", 100, 
                                    List.of("Extraction complete", "Processed " + pageCount + " page(s)"),
                                    detectedForms, overallConfidence, extractedData, 
                                    null, taxpayerName, fieldConfidences, provenances, summary))
                            .delayElements(Duration.ofMillis(400));
                }
            }
            
            // No candidates in response
            log.warn("No candidates found in Gemini response");
            return createErrorResponse("No extraction results returned. The document may not contain recognizable tax forms.", startTime);
            
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage(), e);
            return createErrorResponse("Error processing response: " + e.getMessage(), startTime);
        }
    }

    private String extractTaxpayerName(JsonNode extractedData) {
        return extractedData.path("taxPayerProfile").path("name").asText(null);
    }

    private FormProvenance buildFormProvenance(JsonNode form) {
        String formType = form.path("formType").asText();
        int pageNumber = form.path("pageNumber").asInt(1);
        double confidence = form.path("confidenceScore").asDouble(0.8);
        String reason = form.path("extractionReason").asText("AI Identified Form");

        // Parse form-level bounding box if available
        BoundingBox formBoundingBox = parseBoundingBox(form.path("boundingBox"));

        // Build field provenances with bounding boxes
        List<FieldProvenance> fieldProvenances = new ArrayList<>();
        JsonNode fieldConfidences = form.path("fieldConfidence");
        JsonNode fieldBoundingBoxes = form.path("fieldBoundingBoxes");
        
        if (fieldConfidences.isObject()) {
            fieldConfidences.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                double fieldConf = entry.getValue().asDouble(0.8);
                
                // Try to get bounding box for this field
                BoundingBox fieldBox = null;
                if (fieldBoundingBoxes.isObject() && fieldBoundingBoxes.has(fieldName)) {
                    fieldBox = parseBoundingBox(fieldBoundingBoxes.path(fieldName));
                }
                
                // Get raw and processed values if available
                String rawValue = form.has(fieldName) ? form.path(fieldName).asText(null) : null;
                
                fieldProvenances.add(new FieldProvenance(
                        fieldName,
                        pageNumber,
                        fieldBox,
                        rawValue,
                        rawValue, // Processed value same as raw for now
                        fieldConf
                ));
            });
        }

        return new FormProvenance(
                formType,
                pageNumber,
                formBoundingBox,
                reason,
                confidence,
                fieldProvenances
        );
    }
    
    /**
     * Parse a bounding box from JSON node with normalized coordinates
     */
    private BoundingBox parseBoundingBox(JsonNode boxNode) {
        if (boxNode == null || boxNode.isMissingNode() || !boxNode.isObject()) {
            return null;
        }
        
        double x = boxNode.path("x").asDouble(-1);
        double y = boxNode.path("y").asDouble(-1);
        double width = boxNode.path("width").asDouble(-1);
        double height = boxNode.path("height").asDouble(-1);
        
        // Validate coordinates are in expected range (0-1 normalized)
        if (x < 0 || x > 1 || y < 0 || y > 1 || width <= 0 || height <= 0) {
            log.debug("Invalid bounding box coordinates: x={}, y={}, width={}, height={}", x, y, width, height);
            return null;
        }
        
        return new BoundingBox(x, y, width, height);
    }

    private Map<String, FieldConfidence> buildFieldConfidences(JsonNode extractedData) {
        Map<String, FieldConfidence> result = new HashMap<>();
        JsonNode forms = extractedData.path("forms");
        
        if (forms.isArray()) {
            forms.forEach(form -> {
                String formType = form.path("formType").asText();
                JsonNode fieldConf = form.path("fieldConfidence");
                
                if (fieldConf.isObject()) {
                    fieldConf.fields().forEachRemaining(entry -> {
                        String fieldName = formType + "." + entry.getKey();
                        double confidence = entry.getValue().asDouble(0.8);
                        String weight = FIELD_WEIGHTS.getOrDefault(entry.getKey(), "MEDIUM");
                        double weightMultiplier = getWeightMultiplier(weight);
                        
                        result.put(fieldName, new FieldConfidence(
                                entry.getKey(),
                                confidence,
                                weight,
                                confidence * weightMultiplier,
                                "AI_EXTRACTED"
                        ));
                    });
                }
            });
        }
        return result;
    }

    private double calculateOverallConfidence(Map<String, Double> confidenceByForm) {
        if (confidenceByForm.isEmpty()) return 0.0;
        return confidenceByForm.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double getWeightMultiplier(String weight) {
        return switch (weight) {
            case "CRITICAL" -> 1.5;
            case "HIGH" -> 1.25;
            case "MEDIUM" -> 1.0;
            case "LOW" -> 0.75;
            default -> 1.0;
        };
    }

    private Flux<ExtractionUpdate> mockExtraction(String fileName, long startTime) {
        return Flux.interval(Duration.ofMillis(600))
                .take(6)
                .map(i -> {
                    long step = i + 1;
                    String taxpayerName = "John Q. Taxpayer";
                    List<String> forms = List.of("W-2", "Federal 1040");
                    
                    if (step == 1)
                        return new ExtractionUpdate("SCANNING", 15, 
                                List.of("Scanning " + fileName + "..."),
                                List.of(), 0.0, null, null, null, null, null, null);
                    if (step == 2)
                        return new ExtractionUpdate("ANALYZING", 35, 
                                List.of("Analyzing document structure...", "Identifying tax forms..."),
                                List.of(), 0.0, null, null, taxpayerName, null, null, null);
                    if (step == 3)
                        return new ExtractionUpdate("EXTRACTING", 55, 
                                List.of("Found W-2 form", "Extracting employer information..."),
                                List.of("W-2"), 0.75, null, "W-2", taxpayerName, null, null, null);
                    if (step == 4)
                        return new ExtractionUpdate("EXTRACTING", 70, 
                                List.of("Found Federal 1040", "Extracting income data..."),
                                forms, 0.82, null, "Federal 1040", taxpayerName, null, null, null);
                    if (step == 5)
                        return new ExtractionUpdate("EXTRACTING", 85, 
                                List.of("Validating extracted fields...", "Computing confidence scores..."),
                                forms, 0.88, null, null, taxpayerName, null, null, null);
                    
                    // Final mock result
                    long duration = System.currentTimeMillis() - startTime;
                    ExtractionSummary summary = new ExtractionSummary(
                            2, 2, 0, forms, List.of(), 0.92, 
                            Map.of("W-2", 0.95, "Federal 1040", 0.89),
                            duration, "mock"
                    );
                    return new ExtractionUpdate("COMPLETE", 100, 
                            List.of("Extraction complete", "Mock data generated"),
                            forms, 0.92, buildMockResult(), null, taxpayerName, 
                            Map.of(), List.of(), summary);
                });
    }

    private Object buildMockResult() {
        Map<String, Object> scanMetadata = new HashMap<>();
        scanMetadata.put("pageCount", 2);
        scanMetadata.put("scanQuality", "HIGH");

        Map<String, Object> address = new HashMap<>();
        address.put("street", "123 Main St");
        address.put("city", "Dublin");
        address.put("state", "OH");
        address.put("zip", "43017");

        Map<String, Object> taxPayerProfile = new HashMap<>();
        taxPayerProfile.put("name", "John Q. Taxpayer");
        taxPayerProfile.put("ssn", "***-**-1234");
        taxPayerProfile.put("filingStatus", "SINGLE");
        taxPayerProfile.put("address", address);

        Map<String, Object> fieldConfidence = new HashMap<>();
        fieldConfidence.put("federalWages", 0.98);
        fieldConfidence.put("localWages", 0.92);
        fieldConfidence.put("employer", 0.95);

        Map<String, Object> w2Form = new HashMap<>();
        w2Form.put("formType", "W-2");
        w2Form.put("confidenceScore", 0.95);
        w2Form.put("pageNumber", 1);
        w2Form.put("extractionReason", "Standard W-2 form detected");
        w2Form.put("employer", "Acme Corporation");
        w2Form.put("employerEin", "12-3456789");
        w2Form.put("federalWages", 75000.00);
        w2Form.put("medicareWages", 75000.00);
        w2Form.put("localWages", 75000.00);
        w2Form.put("localWithheld", 1500.00);
        w2Form.put("locality", "Dublin OH");
        w2Form.put("fieldConfidence", fieldConfidence);

        Map<String, Object> result = new HashMap<>();
        result.put("scanMetadata", scanMetadata);
        result.put("taxPayerProfile", taxPayerProfile);
        result.put("forms", List.of(w2Form));

        return result;
    }

    private Flux<ExtractionUpdate> createErrorResponse(String errorMessage, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        ExtractionSummary summary = new ExtractionSummary(
                0, 0, 0, List.of(), 
                List.of(new SkippedForm("Unknown", 0, "API Error: " + errorMessage, "Please check your API key and try again")),
                0.0, Map.of(), duration, "error"
        );
        return Flux.just(new ExtractionUpdate(
                "ERROR", 0, 
                List.of("Extraction failed: " + errorMessage),
                List.of(), 0.0, null, null, null, null, null, summary
        ));
    }

    private static Map<String, String> initializeFieldWeights() {
        Map<String, String> weights = new HashMap<>();
        
        // CRITICAL fields - essential for tax calculation
        weights.put("federalWages", "CRITICAL");
        weights.put("medicareWages", "CRITICAL");
        weights.put("localWages", "CRITICAL");
        weights.put("localWithheld", "CRITICAL");
        weights.put("taxDue", "CRITICAL");
        weights.put("fedTaxableIncome", "CRITICAL");
        weights.put("totalIncome", "CRITICAL");
        weights.put("adjustedGrossIncome", "CRITICAL");
        weights.put("netProfit", "CRITICAL");
        weights.put("grossReceipts", "CRITICAL");
        weights.put("incomeAmount", "CRITICAL");
        weights.put("grossWinnings", "CRITICAL");
        
        // HIGH fields - important for accurate filing
        weights.put("employerEin", "HIGH");
        weights.put("employer", "HIGH");
        weights.put("filingStatus", "HIGH");
        weights.put("ssn", "HIGH");
        weights.put("name", "HIGH");
        weights.put("payer", "HIGH");
        weights.put("businessEin", "HIGH");
        weights.put("businessName", "HIGH");
        weights.put("qualifyingWages", "HIGH");
        weights.put("credits", "HIGH");
        
        // MEDIUM fields - helpful context
        weights.put("locality", "MEDIUM");
        weights.put("address", "MEDIUM");
        weights.put("totalExpenses", "MEDIUM");
        weights.put("rentals", "MEDIUM");
        weights.put("partnerships", "MEDIUM");
        weights.put("overpayment", "MEDIUM");
        
        // LOW fields - supplementary information
        weights.put("extractionReason", "LOW");
        weights.put("pageNumber", "LOW");
        weights.put("dateWon", "LOW");
        weights.put("typeOfWager", "LOW");
        
        return weights;
    }

    /**
     * Production-grade extraction prompt with precise field descriptions.
     * Optimized for Gemini 2.5 Flash model.
     */
    private String buildProductionExtractionPrompt() {
        return """
                You are an expert Tax Document Analyzer specialized in U.S. Federal, State, and Local municipality tax forms.
                
                SUPPORTED FORM TYPES:
                ═══════════════════════════════════════════════════════════════════════════════
                INDIVIDUAL FORMS:
                • Federal 1040 - U.S. Individual Income Tax Return
                • W-2 - Wage and Tax Statement (employee wage reporting)
                • 1099-NEC - Nonemployee Compensation
                • 1099-MISC - Miscellaneous Income (rents, royalties, prizes)
                • W-2G - Certain Gambling Winnings
                • Schedule C - Profit or Loss From Business (Sole Proprietorship)
                • Schedule E - Supplemental Income and Loss (Rental, Partnership, S-Corp, Trust)
                • Schedule F - Profit or Loss From Farming
                • Dublin 1040 / Form R - Local Municipality Tax Return
                
                BUSINESS FORMS:
                • Federal 1120 - U.S. Corporation Income Tax Return
                • Federal 1065 - U.S. Return of Partnership Income
                • Form 27 - Net Profits Tax Return (Municipality)
                • Form W-1 - Employer's Quarterly Withholding Return
                • Form W-3 - Annual Reconciliation of Withholding Tax
                • Schedule X - Book-to-Tax Reconciliation
                • Schedule Y - Business Allocation Factor
                ═══════════════════════════════════════════════════════════════════════════════
                
                YOUR TASK:
                1. Scan each page of the document systematically
                2. Identify ALL distinct tax forms present
                3. Extract ALL relevant fields with maximum precision
                4. Provide confidence scores (0.0-1.0) for each field
                5. Track document provenance (page number, bounding box location)
                6. Skip instruction pages and blank pages with clear reasons
                
                CRITICAL EXTRACTION RULES:
                ═══════════════════════════════════════════════════════════════════════════════
                W-2 FORM - Extract ALL boxes (1-20):
                • Box a: Employee SSN (masked as ***-**-XXXX)
                • Box b: Employer Identification Number (EIN) - 9 digits, format XX-XXXXXXX
                • Box c: Employer Name, Address, City, State, ZIP
                • Box d: Control number (if present)
                • Box e: Employee Name
                • Box f: Employee Address
                • Box 1: Wages, tips, other compensation (federalWages)
                • Box 2: Federal income tax withheld (federalWithheld)
                • Box 3: Social Security wages (socialSecurityWages)
                • Box 4: Social Security tax withheld (socialSecurityTaxWithheld)
                • Box 5: Medicare wages and tips (medicareWages)
                • Box 6: Medicare tax withheld (medicareTaxWithheld)
                • Box 7: Social Security tips
                • Box 8: Allocated tips
                • Box 10: Dependent care benefits
                • Box 11: Nonqualified plans
                • Box 12a-d: Codes and amounts (e.g., DD, D, E, etc.)
                • Box 13: Checkboxes (statutory, retirement, third-party sick pay)
                • Box 14: Other (employer-specific info)
                • Box 15: State/Employer's state ID
                • Box 16: State wages (stateWages)
                • Box 17: State income tax (stateIncomeTax)
                • Box 18: Local wages, tips, etc. (localWages)
                • Box 19: Local income tax (localWithheld)
                • Box 20: Locality name (locality)
                
                1099-NEC/MISC - Extract:
                • Payer name, address, and TIN
                • Recipient name, address, and TIN (masked)
                • Box 1: Nonemployee compensation (1099-NEC) OR
                • Box 3: Other income (1099-MISC)
                • Box 4: Federal income tax withheld
                • State/Local tax information if present
                
                W-2G (Gambling) - Extract:
                • Box 1: Gross winnings (grossWinnings)
                • Box 2: Date won (dateWon)
                • Box 3: Type of wager (typeOfWager)
                • Box 4: Federal income tax withheld (federalWithheld)
                • Box 5: Transaction
                • Box 6: Race
                • Box 7: Winnings from identical wagers
                • Box 14: State winnings
                • Box 15: State income tax withheld
                • Box 16: Local winnings
                • Box 17: Local income tax withheld
                • Payer info (name, TIN, address)
                
                SCHEDULE C - Extract:
                • Business name and principal business/product
                • EIN (if separate from SSN)
                • Business address
                • Accounting method (cash/accrual)
                • Line 1: Gross receipts or sales
                • Line 2: Returns and allowances
                • Line 3: Subtract line 2 from line 1
                • Line 4: Cost of goods sold
                • Line 5: Gross profit
                • Line 6: Other income
                • Line 7: Gross income
                • Lines 8-27: Individual expense categories
                • Line 28: Total expenses
                • Line 29: Tentative profit/loss
                • Line 30: Expenses for business use of home
                • Line 31: Net profit or loss (netProfit)
                
                SCHEDULE E Part I (Rentals) - For EACH property:
                • Property address (street, city, state, zip)
                • Property type (Single Family, Multi-Family, Vacation/Short-term, Commercial, Land, Royalties, Self-Rental)
                • Fair rental days (line 1)
                • Personal use days (line 2)
                • Rents received (line 3)
                • Royalties received (line 4)
                • Lines 5-19: Individual expense categories
                • Line 20: Total expenses
                • Line 21: Net income or loss for property
                • Line 22: Deductible rental real estate loss
                
                SCHEDULE E Part II (Partnerships/S-Corps) - For EACH entity:
                • Entity name
                • Entity type (P for Partnership, S for S-Corp)
                • Check if foreign partnership
                • EIN
                • Line 28j (passive): Passive income/loss
                • Line 28k (nonpassive): Nonpassive income/loss
                
                FEDERAL 1040 - Extract ALL key lines:
                • Filing Status: Single, MFJ, MFS, HOH, QW (checkboxes)
                • Taxpayer name, SSN
                • Spouse name and SSN (if joint)
                • Home address
                • Digital assets question (Yes/No)
                • Line 1a: Total amount from W-2s
                • Line 1b: Household employee wages
                • Line 1c: Tip income
                • Line 1d: Medicaid waiver payments
                • Line 1e: Taxable dependent care benefits
                • Line 1f: Employer-provided adoption benefits
                • Line 1g: Wages from Form 8919
                • Line 1h: Other earned income
                • Line 1i: Nontaxable combat pay
                • Line 1z: Total wages (sum of 1a-1i)
                • Line 2a: Tax-exempt interest
                • Line 2b: Taxable interest
                • Line 3a: Qualified dividends
                • Line 3b: Ordinary dividends
                • Line 4a: IRA distributions
                • Line 4b: Taxable IRA
                • Line 5a: Pensions and annuities
                • Line 5b: Taxable pensions
                • Line 6a: Social Security benefits
                • Line 6b: Taxable Social Security
                • Line 7: Capital gain or loss
                • Line 8: Other income (from Schedule 1)
                • Line 9: Total income
                • Line 10: Adjustments (from Schedule 1)
                • Line 11: Adjusted gross income
                • Line 12: Standard or itemized deduction
                • Line 13: Qualified business income deduction
                • Line 14: Total deductions
                • Line 15: Taxable income
                • Line 16: Tax
                • Line 17: Schedule 2 tax
                • Line 18: Total tax before credits
                • Line 19-21: Credits
                • Line 22: Total tax after credits
                • Line 23: Other taxes
                • Line 24: Total tax
                • Line 25a: Federal income tax withheld from W-2
                • Line 25b: Federal income tax withheld from 1099
                • Line 25c: Other withholding
                • Line 25d: Total withholding
                • Line 26: Estimated tax payments
                • Line 27-32: Other payments and credits
                • Line 33: Total payments
                • Line 34: Overpayment
                • Line 35a: Refund amount
                • Line 37: Amount owed
                
                BUSINESS FORMS (1120/1065/Form 27):
                • Business name and EIN
                • Fiscal year dates
                • Total income (from appropriate line)
                • Total deductions
                • Taxable income (Line 30 for 1120, Line 22 for 1065)
                • Schedule X reconciliation items (add-backs and deductions)
                • Schedule Y allocation factors (Property, Payroll, Sales - each with inside/everywhere)
                ═══════════════════════════════════════════════════════════════════════════════
                
                BOUNDING BOX COORDINATES:
                For each extracted field, provide approximate bounding box as normalized coordinates (0-1):
                • x: left position (0 = left edge, 1 = right edge)
                • y: top position (0 = top edge, 1 = bottom edge)
                • width: width of the field area
                • height: height of the field area
                
                CONFIDENCE SCORING:
                • 0.95-1.0: Clear OCR, exact match to expected format
                • 0.85-0.94: Minor uncertainty, slightly blurry but readable
                • 0.70-0.84: Some ambiguity, may need human verification
                • Below 0.70: Low confidence, flag for manual review
                
                SKIPPED PAGES - Provide detailed reasons:
                • "Blank page detected"
                • "Instruction page - no data to extract"
                • "Image quality too low for accurate extraction"
                • "Unrecognized form type - [describe what was detected]"
                • "Partially obscured - [specify which parts]"
                
                OUTPUT FORMAT (JSON only, no markdown):
                {
                  "scanMetadata": {
                    "pageCount": number,
                    "scanQuality": "HIGH" | "MEDIUM" | "LOW",
                    "processingNotes": [string]
                  },
                  "taxPayerProfile": {
                    "name": string,
                    "ssn": string (masked as ***-**-XXXX),
                    "filingStatus": "SINGLE" | "MARRIED_FILING_JOINTLY" | "MARRIED_FILING_SEPARATELY" | "HEAD_OF_HOUSEHOLD" | "QUALIFYING_WIDOWER",
                    "spouse": { "name": string, "ssn": string } | null,
                    "address": { "street": string, "city": string, "state": string, "zip": string }
                  },
                  "returnSettings": {
                    "taxYear": number,
                    "isAmendment": boolean,
                    "amendmentReason": string | null
                  },
                  "forms": [
                    {
                      "formType": string,
                      "confidenceScore": number,
                      "pageNumber": number,
                      "extractionReason": string,
                      "owner": "PRIMARY" | "SPOUSE",
                      "boundingBox": { "x": number, "y": number, "width": number, "height": number },
                      "fieldConfidence": { [fieldName]: number },
                      "fieldBoundingBoxes": { 
                        [fieldName]: { "x": number, "y": number, "width": number, "height": number } 
                      },
                      
                      // Form-specific fields as detailed above
                      // Include ALL applicable fields for the form type
                    }
                  ],
                  "skippedPages": [
                    { 
                      "pageNumber": number, 
                      "reason": string,
                      "suggestion": string,
                      "detectedContent": string | null
                    }
                  ]
                }
                
                IMPORTANT: Return ONLY valid JSON. No markdown code blocks. No explanatory text.
                """;
}
}
