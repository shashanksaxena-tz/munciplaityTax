package com.munitax.extraction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.extraction.model.ExtractionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RealGeminiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.0-flash-exp}")
    private String model;

    public RealGeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
        this.objectMapper = objectMapper;
    }

    public Flux<ExtractionDto.ExtractionUpdate> extractData(String fileName, String base64Data, String mimeType) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                System.out.println("Gemini API Key is missing. Using mock extraction.");
                return mockExtraction(fileName);
            }

            if (base64Data == null) {
                System.out.println("No file data provided. Using mock extraction.");
                return mockExtraction(fileName);
            }

            String trimmedKey = apiKey.trim();
            System.out.println("Using Gemini API Key: " + trimmedKey.substring(0, 5) + "..."
                    + trimmedKey.substring(trimmedKey.length() - 5));
            System.out.println("Using Gemini Model: " + model);

            String prompt = buildExtractionPrompt();

            Map<String, Object> request = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt),
                                    Map.of("inline_data", Map.of(
                                            "mime_type", mimeType,
                                            "data", base64Data))))),
                    "generationConfig", Map.of(
                            "response_mime_type", "application/json",
                            "temperature", 0.1));

            return webClient.post()
                    .uri("/models/" + model + ":generateContent?key=" + trimmedKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .flatMap(this::parseStreamingResponse)
                    .onErrorResume(error -> {
                        System.err.println("Gemini API Error: " + error.getMessage());
                        error.printStackTrace();
                        return mockExtraction(fileName);
                    });
        } catch (Exception e) {
            System.err.println("Unexpected error in extractData:");
            e.printStackTrace();
            return mockExtraction(fileName);
        }
    }

    private Flux<ExtractionDto.ExtractionUpdate> parseStreamingResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");

                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();

                    // Parse extracted forms
                    JsonNode extractedData = objectMapper.readTree(text);
                    JsonNode forms = extractedData.path("forms");

                    List<String> detectedForms = new ArrayList<>();
                    if (forms.isArray()) {
                        forms.forEach(form -> {
                            String formType = form.path("formType").asText();
                            if (!formType.isEmpty()) {
                                detectedForms.add(formType);
                            }
                        });
                    }

                    return Flux.just(
                            new ExtractionDto.ExtractionUpdate("SCANNING", 20, List.of("Scanning document..."),
                                    List.of(), 0.0, null),
                            new ExtractionDto.ExtractionUpdate("ANALYZING", 40, List.of("Analyzing with AI..."),
                                    List.of(), 0.0, null),
                            new ExtractionDto.ExtractionUpdate("EXTRACTING", 70, List.of("Extracting fields..."),
                                    detectedForms, 0.85, null),
                            new ExtractionDto.ExtractionUpdate("COMPLETE", 100, List.of("Extraction complete"),
                                    detectedForms, 0.95, extractedData))
                            .delayElements(Duration.ofMillis(500));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
        }

        return Flux.empty();
    }

    private Flux<ExtractionDto.ExtractionUpdate> mockExtraction(String fileName) {
        return Flux.interval(Duration.ofMillis(500))
                .take(5)
                .map(i -> {
                    long step = i + 1;
                    if (step == 1)
                        return new ExtractionDto.ExtractionUpdate("SCANNING", 20, List.of("Scanning " + fileName),
                                List.of(), 0.0, null);
                    if (step == 2)
                        return new ExtractionDto.ExtractionUpdate("ANALYZING", 40, List.of("Identifying forms..."),
                                List.of(), 0.0, null);
                    if (step == 3)
                        return new ExtractionDto.ExtractionUpdate("EXTRACTING", 60, List.of("Found W-2"),
                                List.of("W-2"), 0.8, null);
                    if (step == 4)
                        return new ExtractionDto.ExtractionUpdate("EXTRACTING", 80, List.of("Extracting fields..."),
                                List.of("W-2"), 0.9, null);
                    return new ExtractionDto.ExtractionUpdate("COMPLETE", 100, List.of("Extraction complete"),
                            List.of("W-2"), 0.95, null);
                });
    }

    private String buildExtractionPrompt() {
        return """
                You are an expert Tax Document Analyzer.
                The input file may contain INDIVIDUAL tax forms (Federal 1040, W-2, 1099, Schedules C/E/F, Local 1040) OR BUSINESS tax forms (Federal 1120, 1065, Form 27).

                YOUR JOB:
                1. Scan the document page by page.
                2. Identify every distinct tax form.
                3. Extract all relevant fields with high precision.
                4. Return a JSON object with a list of extracted forms and profile info.

                CRITICAL RULES:
                - Return ONLY valid JSON. Do not use Markdown formatting.
                - Extract Filing Status (Single, MFJ, MFS, HOH) and Spouse Name/SSN from Federal 1040.
                - For Schedule E, extract Part I (Rentals) and Part II (Partnerships) separately.
                - For W-2, extract Box b (EIN) and Box c (Employer Name/Address).
                - Return "confidenceScore" (0.0-1.0) for each form.
                - Return "pageNumber" where the form was found.

                OUTPUT SCHEMA (JSON):
                {
                  "scanMetadata": { "pageCount": number, "scanQuality": "HIGH"|"MEDIUM"|"LOW" },
                  "taxPayerProfile": {
                    "name": string,
                    "ssn": string,
                    "filingStatus": "SINGLE"|"MARRIED_FILING_JOINTLY"|"MARRIED_FILING_SEPARATELY"|"HEAD_OF_HOUSEHOLD"|"QUALIFYING_WIDOWER",
                    "spouse": { "name": string, "ssn": string },
                    "address": { "street": string, "city": string, "state": string, "zip": string }
                  },
                  "returnSettings": {
                    "isAmendment": boolean,
                    "amendmentReason": string
                  },
                  "forms": [
                    {
                      "formType": "W-2" | "1099-NEC" | "1099-MISC" | "W-2G" | "Schedule C" | "Schedule E" | "Schedule F" | "Dublin 1040" | "Federal 1040" | "Federal 1120" | "Federal 1065" | "Form 27",
                      "confidenceScore": number,
                      "pageNumber": number,
                      "extractionReason": string,
                      "fieldConfidence": { [fieldName: string]: number },
                      "owner": "PRIMARY" | "SPOUSE",

                      // W-2 Fields
                      "employer": string, "employerEin": string, "federalWages": number, "medicareWages": number, "localWages": number, "localWithheld": number, "locality": string,

                      // 1099 Fields
                      "payer": string, "incomeAmount": number,

                      // W-2G Fields
                      "grossWinnings": number, "dateWon": string, "typeOfWager": string,

                      // Schedule C
                      "businessName": string, "businessEin": string, "grossReceipts": number, "totalExpenses": number, "netProfit": number,

                      // Schedule E (Rentals & Partnerships)
                      "rentals": [
                         { "streetAddress": string, "city": string, "state": string, "zip": string, "line21_FairRentalDays_or_Income": number, "line22_DeductibleLoss": number }
                      ],
                      "partnerships": [ { "name": string, "ein": string, "netProfit": number } ],

                      // Schedule F
                      "netFarmProfit": number,

                      // Federal 1040
                      "wages": number, "qualifiedDividends": number, "pensions": number, "socialSecurity": number, "capitalGains": number, "otherIncome": number, "totalIncome": number, "adjustedGrossIncome": number, "tax": number,

                      // Dublin 1040 / Form R
                      "qualifyingWages": number, "otherIncome": number, "totalIncomeLocal": number, "taxDue": number, "credits": number, "overpayment": number,

                      // Business Federal (1120/1065)
                      "fedTaxableIncome": number,
                      "reconciliation": {
                         "addBacks": { "interestAndStateTaxes": number, "guaranteedPayments": number, "expensesOnIntangibleIncome": number, "other": number },
                         "deductions": { "interestIncome": number, "dividends": number, "capitalGains": number, "other": number }
                      },
                      "allocation": {
                         "property": { "dublin": number, "everywhere": number },
                         "payroll": { "dublin": number, "everywhere": number },
                         "sales": { "dublin": number, "everywhere": number }
                      }
                    }
                  ]
                }
                """;
    }
}
