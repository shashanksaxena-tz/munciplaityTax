package com.munitax.pdf.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;

/**
 * Utility for adding/removing watermarks on PDF forms
 */
@Component
public class FormWatermarkUtil {

    private static final Logger log = LoggerFactory.getLogger(FormWatermarkUtil.class);
    private static final String DRAFT_WATERMARK_TEXT = "DRAFT - NOT FOR FILING";

    /**
     * Add DRAFT watermark to all pages of PDF
     */
    public void addDraftWatermark(PDDocument document) throws IOException {
        log.debug("Adding DRAFT watermark to PDF with {} pages", document.getNumberOfPages());
        
        for (PDPage page : document.getPages()) {
            addWatermarkToPage(page, document);
        }
    }

    /**
     * Add watermark to a single page
     */
    private void addWatermarkToPage(PDPage page, PDDocument document) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            // Set transparency
            PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
            graphicsState.setNonStrokingAlphaConstant(0.3f);
            contentStream.setGraphicsStateParameters(graphicsState);
            
            // Set font and color
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            contentStream.setFont(font, 60);
            contentStream.setNonStrokingColor(Color.RED);
            
            // Calculate position (center, diagonal)
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float textWidth = 500; // Approximate width
            float x = (pageWidth - textWidth) / 2;
            float y = pageHeight / 2;
            
            // Rotate text diagonally
            contentStream.beginText();
            Matrix matrix = new Matrix(
                (float) Math.cos(Math.toRadians(45)), 
                (float) Math.sin(Math.toRadians(45)),
                -(float) Math.sin(Math.toRadians(45)), 
                (float) Math.cos(Math.toRadians(45)),
                x, y
            );
            contentStream.setTextMatrix(matrix);
            contentStream.showText(DRAFT_WATERMARK_TEXT);
            contentStream.endText();
        }
    }

    /**
     * Check if document has watermark (simplified check - looks for watermark text)
     */
    public boolean hasWatermark(PDDocument document) {
        // This is a simplified implementation
        // In production, you might want to check actual content or metadata
        return false; // Placeholder
    }
}
