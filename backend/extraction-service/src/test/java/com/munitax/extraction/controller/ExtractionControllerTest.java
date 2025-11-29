package com.munitax.extraction.controller;

import com.munitax.extraction.model.ExtractionDto;
import com.munitax.extraction.service.RealGeminiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExtractionController.class)
class ExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RealGeminiService geminiService;

    @Test
    void testExtractData_Success() throws Exception {
        ExtractionDto.ExtractionUpdate update = new ExtractionDto.ExtractionUpdate();
        update.setProgress(100);
        update.setMessage("Extraction complete");
        
        when(geminiService.extractData(anyString(), anyString(), anyString()))
                .thenReturn(Flux.just(update));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/extraction/extract")
                .file(file)
                .param("taxYear", "2024"))
                .andExpect(status().isOk());
    }

    @Test
    void testStreamExtraction() throws Exception {
        ExtractionDto.ExtractionUpdate update = new ExtractionDto.ExtractionUpdate();
        update.setProgress(50);
        
        when(geminiService.extractData(anyString(), any(), any()))
                .thenReturn(Flux.just(update));

        mockMvc.perform(get("/api/v1/extraction/stream")
                .param("fileName", "test.pdf"))
                .andExpect(status().isOk());
    }
}
