package com.munitax.pdf.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Helper utility for PDFBox operations
 */
@Component
public class PDFBoxHelper {

    private static final Logger log = LoggerFactory.getLogger(PDFBoxHelper.class);

    /**
     * Fill PDF form fields from data map
     */
    public void fillFormFields(PDDocument document, Map<String, String> fieldData) throws IOException {
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        
        if (acroForm == null) {
            log.warn("PDF does not contain AcroForm fields");
            return;
        }

        for (Map.Entry<String, String> entry : fieldData.entrySet()) {
            String fieldName = entry.getKey();
            String value = entry.getValue();
            
            try {
                PDField field = acroForm.getField(fieldName);
                if (field != null) {
                    field.setValue(value);
                    log.debug("Set field '{}' to '{}'", fieldName, value);
                } else {
                    log.debug("Field '{}' not found in form", fieldName);
                }
            } catch (IOException e) {
                log.error("Error setting field '{}': {}", fieldName, e.getMessage());
            }
        }
        
        // Flatten the form to make it read-only
        acroForm.flatten();
    }

    /**
     * Add page numbers to document
     */
    public void addPageNumbers(PDDocument document, int startPage) throws IOException {
        int totalPages = document.getNumberOfPages();
        int pageNum = startPage;
        
        for (PDPage page : document.getPages()) {
            addPageNumber(page, document, pageNum, totalPages);
            pageNum++;
        }
    }

    /**
     * Add page number to single page
     */
    private void addPageNumber(PDPage page, PDDocument document, int pageNum, int totalPages) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            String pageText = String.format("Page %d of %d", pageNum, totalPages);
            
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            contentStream.setFont(font, 10);
            PDRectangle mediaBox = page.getMediaBox();
            float x = (mediaBox.getWidth() - 80) / 2;
            float y = 30;
            
            contentStream.beginText();
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(pageText);
            contentStream.endText();
        }
    }

    /**
     * Merge multiple PDF documents
     */
    public PDDocument mergeDocuments(PDDocument target, PDDocument source) throws IOException {
        for (PDPage page : source.getPages()) {
            target.addPage(page);
        }
        return target;
    }

    /**
     * Get document file size in bytes
     */
    public long estimateFileSize(PDDocument document) {
        // Rough estimate: pages * average size per page
        int pages = document.getNumberOfPages();
        return pages * 50000L; // ~50KB per page average
    }
}
