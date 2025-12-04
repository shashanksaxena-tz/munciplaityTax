package com.munitax.extraction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.extraction.model.ExtractionDto.*;
import com.munitax.extraction.schema.FormFieldSchema;
import com.munitax.extraction.schema.FormSchema;
import com.munitax.extraction.schema.FormSchemaRepository;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

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
    private final FormSchemaRepository formSchemaRepository;

    @Value("${gemini.api.key:}")
    private String defaultApiKey;

    @Value("${gemini.api.model:" + DEFAULT_MODEL + "}")
    private String defaultModel;

    public RealGeminiService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            FormSchemaRepository formSchemaRepository) {
        
        // Configure HTTP client with extended timeouts for large document processing
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000) // 60 seconds connect timeout
                .responseTimeout(Duration.ofMinutes(10)) // 10 minute response timeout
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(600)) // 10 minutes read timeout
                        .addHandlerLast(new WriteTimeoutHandler(120))); // 2 minute write timeout
        
        this.webClient = webClientBuilder
                .baseUrl(GEMINI_BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(100 * 1024 * 1024)) // 100MB buffer for large documents
                .build();
        this.objectMapper = objectMapper;
        this.formSchemaRepository = formSchemaRepository;
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
                        log.error("Response length: {} chars, Last 200 chars: {}", 
                                text.length(), 
                                text.substring(Math.max(0, text.length() - 200)));
                        
                        // Check if JSON was truncated
                        if (parseEx.getMessage().contains("end-of-input") || parseEx.getMessage().contains("Unexpected end")) {
                            log.error("JSON response appears truncated. This may indicate the document is too complex or the API response was cut off.");
                            return createErrorResponse("Document too complex - response was truncated. Try a smaller document or simpler pages.", startTime);
                        }
                        
                        log.debug("First 500 chars: {}", text.substring(0, Math.min(500, text.length())));
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
            StringBuilder prompt = new StringBuilder();
            prompt.append("You are an expert Tax Document Analyzer specialized in U.S. federal, state, and local municipality tax forms.\n");
            prompt.append("Review every page, detect all supported forms, and extract only the fields defined by the UI schemas.\n");

            prompt.append("\nPRIMARY OBJECTIVE:\n");
            prompt.append("- Detect each distinct form present in the document.\n");
            prompt.append("- Output an entry in the forms array for every detected form, including formType, confidenceScore, and pageNumber.\n");
            prompt.append("- Populate the form object with ONLY the schema-approved field ids; never invent or rename keys.\n");
            prompt.append("- Provide fieldConfidence and fieldBoundingBoxes maps so reviewers know where a value came from.\n");

            prompt.append("\nSTRICT FIELD POLICY:\n");
            prompt.append("- UI field ids are authoritative. Keys must match the ids below exactly.\n");
            prompt.append("- If a field is missing, illegible, or not applicable, still include the key with value null.\n");
            prompt.append("- Preserve number formatting as plain numbers (no currency symbols or commas).\n");
            prompt.append("- Mask personally identifiable information exactly as found in the document; never fabricate digits.\n");
            prompt.append("- When a form contains repeated sections (for example properties or dependents), output arrays following the schema structure.\n");

            prompt.append("\nSUPPORTED FORMS AND UI FIELD IDS:\n");
            prompt.append(buildFormFieldInstructions());

            prompt.append("BOUNDING BOX REQUIREMENTS:\n");
            prompt.append("- Provide boundingBox for the form and each field using normalized coordinates within [0,1].\n");
            prompt.append("- Coordinate order is {\"x\": left, \"y\": top, \"width\": width, \"height\": height}.\n");

            prompt.append("\nCONFIDENCE SCORING GUIDANCE:\n");
            prompt.append("- 0.95-1.0: high certainty (clear OCR, exact match).\n");
            prompt.append("- 0.85-0.94: moderate certainty (minor noise but readable).\n");
            prompt.append("- 0.70-0.84: low certainty (flag for review).\n");
            prompt.append("- Below 0.70: very low certainty; explain issues in processingNotes.\n");

            prompt.append("\nSKIPPED PAGES:\n");
            prompt.append("- If a page is skipped, record it in skippedPages with pageNumber, reason, and detectedContent.\n");

            prompt.append("\nOUTPUT JSON SHAPE:\n");
            prompt.append("{\n");
            prompt.append("  \"scanMetadata\": {\n");
            prompt.append("    \"pageCount\": number,\n");
            prompt.append("    \"scanQuality\": \"HIGH\" | \"MEDIUM\" | \"LOW\",\n");
            prompt.append("    \"processingNotes\": [string]\n");
            prompt.append("  },\n");
            prompt.append("  \"taxPayerProfile\": {\n");
            prompt.append("    \"name\": string | null,\n");
            prompt.append("    \"ssn\": string | null,\n");
            prompt.append("    \"filingStatus\": string | null,\n");
            prompt.append("    \"spouse\": { \"name\": string | null, \"ssn\": string | null } | null,\n");
            prompt.append("    \"address\": { \"street\": string | null, \"city\": string | null, \"state\": string | null, \"zip\": string | null } | null\n");
            prompt.append("  },\n");
            prompt.append("  \"returnSettings\": {\n");
            prompt.append("    \"taxYear\": number | null,\n");
            prompt.append("    \"isAmendment\": boolean | null,\n");
            prompt.append("    \"amendmentReason\": string | null\n");
            prompt.append("  },\n");
            prompt.append("  \"forms\": [\n");
            prompt.append("    {\n");
            prompt.append("      \"formType\": string,\n");
            prompt.append("      \"confidenceScore\": number,\n");
            prompt.append("      \"pageNumber\": number,\n");
            prompt.append("      \"extractionReason\": string,\n");
            prompt.append("      \"owner\": \"PRIMARY\" | \"SPOUSE\" | null,\n");
            prompt.append("      \"boundingBox\": { \"x\": number, \"y\": number, \"width\": number, \"height\": number },\n");
            prompt.append("      \"fieldConfidence\": { [fieldId]: number },\n");
            prompt.append("      \"fieldBoundingBoxes\": { [fieldId]: { \"x\": number, \"y\": number, \"width\": number, \"height\": number } },\n");
            prompt.append("      // Include the schema-defined fields for this formType using the ids above\n");
            prompt.append("    }\n");
            prompt.append("  ],\n");
            prompt.append("  \"skippedPages\": [\n");
            prompt.append("    { \"pageNumber\": number, \"reason\": string, \"suggestion\": string | null, \"detectedContent\": string | null }\n");
            prompt.append("  ]\n");
            prompt.append("}\n");
            prompt.append("IMPORTANT: Return ONLY valid JSON. No markdown code blocks. No explanatory text.\n");
            return prompt.toString();
        }

        private String buildFormFieldInstructions() {
            Map<String, FormSchema> schemaMap = formSchemaRepository.getAllSchemas();
            if (schemaMap.isEmpty()) {
                log.warn("Form schema repository is empty; prompt will lack field guidance.");
                return "- No schemas loaded. Return forms as an empty array.\n";
            }

            StringBuilder builder = new StringBuilder();
            List<FormSchema> schemas = new ArrayList<>(schemaMap.values());
            schemas.sort(Comparator.comparing(FormSchema::getFormType, String.CASE_INSENSITIVE_ORDER));

            for (FormSchema schema : schemas) {
                builder.append("- ").append(schema.getFormType());
                if (schema.getDescription() != null && !schema.getDescription().isBlank()) {
                    builder.append(" (").append(schema.getDescription()).append(")");
                }
                if (schema.getVersion() != null && !schema.getVersion().isBlank()) {
                    builder.append(" [version ").append(schema.getVersion()).append("]");
                }
                builder.append("\n");
                builder.append("  formType value: \"").append(schema.getFormType()).append("\"\n");
                builder.append("  UI field ids (use exactly these keys; set value null when missing):\n");

                List<FormFieldSchema> fields = new ArrayList<>(schema.getFields());
                Comparator<FormFieldSchema> fieldComparator = Comparator
                        .comparingInt((FormFieldSchema field) -> field.getDisplayOrder() > 0 ? field.getDisplayOrder() : Integer.MAX_VALUE)
                        .thenComparing(field -> {
                            String label = field.getLabel();
                            return label != null && !label.isBlank() ? label : field.getId();
                        }, String.CASE_INSENSITIVE_ORDER);
                fields.sort(fieldComparator);

                for (FormFieldSchema field : fields) {
                    builder.append("    - ").append(field.getId());
                    if (field.getLabel() != null && !field.getLabel().isBlank()) {
                        builder.append(" => ").append(field.getLabel());
                    }
                    if (field.getType() != null && !field.getType().isBlank()) {
                        builder.append(" [").append(field.getType()).append("]");
                    }
                    builder.append("\n");
                }

                builder.append("\n");
            }

            return builder.toString();
        }
    }
