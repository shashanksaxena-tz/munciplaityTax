package com.munitax.pdf.controller;

import com.munitax.pdf.model.TaxCalculationResult;
import com.munitax.pdf.service.PdfGeneratorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/pdf")
public class PdfController {

    private final PdfGeneratorService pdfGeneratorService;

    public PdfController(PdfGeneratorService pdfGeneratorService) {
        this.pdfGeneratorService = pdfGeneratorService;
    }

    @PostMapping("/generate/tax-return")
    public ResponseEntity<byte[]> generateTaxReturnPdf(@RequestBody TaxCalculationResult result) {
        try {
            byte[] pdfBytes = pdfGeneratorService.generateTaxReturnPdf(result);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = String.format("Dublin_Tax_Return_%d_%s.pdf",
                    result.getSettings().getTaxYear(),
                    result.getProfile().getName().replaceAll("\\s+", "_"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
