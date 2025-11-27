package com.munitax.extraction.controller;

import com.munitax.extraction.model.ExtractionDto;
import com.munitax.extraction.service.RealGeminiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/extraction")
public class ExtractionController {

    private final RealGeminiService geminiService;

    public ExtractionController(RealGeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExtractionDto.ExtractionUpdate> extractData(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String taxYear) throws IOException {

        String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType();

        return geminiService.extractData(file.getOriginalFilename(), base64Data, mimeType);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ExtractionDto.ExtractionUpdate> streamExtraction(@RequestParam String fileName) {
        // Fallback for testing
        return geminiService.extractData(fileName, null, null);
    }
}
