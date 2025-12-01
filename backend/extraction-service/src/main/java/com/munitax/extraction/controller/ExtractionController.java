package com.munitax.extraction.controller;

import com.munitax.extraction.model.ExtractionDto;
import com.munitax.extraction.service.RealGeminiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Base64;

/**
 * REST Controller for AI-powered tax document extraction.
 * 
 * Endpoints:
 * - POST /api/v1/extraction/extract - Extract data from uploaded document
 * - GET /api/v1/extraction/stream - Test streaming extraction (mock data)
 * 
 * API Key Handling:
 * - Users can provide their own Gemini API key via X-Gemini-Api-Key header
 * - Keys are never persisted and used only for the current request
 * - Falls back to server-configured key if not provided
 * 
 * @version 2.0.0
 */
@RestController
@RequestMapping("/api/v1/extraction")
public class ExtractionController {

    private final RealGeminiService geminiService;

    public ExtractionController(RealGeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Extract tax data from uploaded document using Gemini AI.
     * 
     * @param file The tax document (PDF, JPG, PNG supported)
     * @param geminiApiKey User-provided Gemini API key (optional, header: X-Gemini-Api-Key)
     * @param geminiModel Model override (optional, defaults to gemini-2.5-flash)
     * @param taxYear Tax year for context (optional)
     * @return Server-sent events stream with extraction progress and results
     * @throws IOException if file cannot be read
     */
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExtractionDto.ExtractionUpdate> extractData(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Gemini-Api-Key", required = false) String geminiApiKey,
            @RequestHeader(value = "X-Gemini-Model", required = false) String geminiModel,
            @RequestParam(required = false) String taxYear) throws IOException {

        String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType();

        return geminiService.extractData(
                file.getOriginalFilename(), 
                base64Data, 
                mimeType,
                geminiApiKey,
                geminiModel
        );
    }

    /**
     * Batch extract from multiple documents.
     * 
     * @param files Array of tax documents
     * @param geminiApiKey User-provided Gemini API key (optional)
     * @param geminiModel Model override (optional)
     * @return Server-sent events stream with batch extraction progress
     * @throws IOException if files cannot be read
     */
    @PostMapping(value = "/extract/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExtractionDto.ExtractionUpdate> extractBatch(
            @RequestPart("files") MultipartFile[] files,
            @RequestHeader(value = "X-Gemini-Api-Key", required = false) String geminiApiKey,
            @RequestHeader(value = "X-Gemini-Model", required = false) String geminiModel) throws IOException {

        // Process files sequentially, concatenating results
        Flux<ExtractionDto.ExtractionUpdate> result = Flux.empty();
        
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType();
            
            Flux<ExtractionDto.ExtractionUpdate> fileExtraction = geminiService.extractData(
                    file.getOriginalFilename(),
                    base64Data,
                    mimeType,
                    geminiApiKey,
                    geminiModel
            );
            
            result = result.concatWith(fileExtraction);
        }
        
        return result;
    }

    /**
     * Test streaming extraction with mock data.
     * Useful for UI development and testing without using API credits.
     * 
     * @param fileName Simulated file name
     * @return Server-sent events stream with mock extraction data
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExtractionDto.ExtractionUpdate> streamExtraction(@RequestParam String fileName) {
        // Returns mock data for testing
        return geminiService.extractData(fileName, null, null, null, null);
    }
}
