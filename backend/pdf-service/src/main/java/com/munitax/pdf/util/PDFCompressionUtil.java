package com.munitax.pdf.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Utility for compressing PDF files to reduce file size
 */
@Component
public class PDFCompressionUtil {

    private static final Logger log = LoggerFactory.getLogger(PDFCompressionUtil.class);
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    /**
     * Optimize PDF file size by compressing images and removing duplicates
     */
    public void optimizeFileSize(PDDocument document) throws IOException {
        log.debug("Optimizing PDF file size for {} pages", document.getNumberOfPages());
        
        // In a production implementation, you would:
        // 1. Compress images (JPEG compression)
        // 2. Remove duplicate fonts
        // 3. Remove duplicate images
        // 4. Subset fonts (include only used characters)
        // 5. Remove unused resources
        
        // For now, this is a placeholder
        // PDFBox handles some optimization automatically during save
    }

    /**
     * Check if file size is within acceptable limits
     */
    public boolean isWithinSizeLimit(long fileSizeBytes) {
        return fileSizeBytes <= MAX_FILE_SIZE_BYTES;
    }

    /**
     * Get file size in MB
     */
    public double getFileSizeMB(long fileSizeBytes) {
        return fileSizeBytes / (1024.0 * 1024.0);
    }
}
