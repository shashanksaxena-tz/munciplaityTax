package com.munitax.submission.service;

import com.munitax.submission.model.SubmissionDocument;
import com.munitax.submission.repository.SubmissionDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionDocumentServiceTest {

    @Mock
    private SubmissionDocumentRepository documentRepository;

    @InjectMocks
    private SubmissionDocumentService documentService;

    private SubmissionDocument testDocument;

    @BeforeEach
    void setUp() {
        testDocument = new SubmissionDocument();
        testDocument.setId("doc-123");
        testDocument.setSubmissionId("sub-123");
        testDocument.setDocumentId("storage-doc-123");
        testDocument.setFileName("W2_2024.pdf");
        testDocument.setFormType("W-2");
        testDocument.setFileSize(102400L);
        testDocument.setMimeType("application/pdf");
        testDocument.setUploadDate(Instant.now());
        testDocument.setExtractionConfidence(0.95);
        testDocument.setTenantId("tenant-1");
    }

    @Test
    void testGetDocumentsBySubmissionId() {
        List<SubmissionDocument> documents = Arrays.asList(testDocument);
        when(documentRepository.findBySubmissionId("sub-123")).thenReturn(documents);

        List<SubmissionDocument> result = documentService.getDocumentsBySubmissionId("sub-123");

        assertEquals(1, result.size());
        assertEquals("doc-123", result.get(0).getId());
        verify(documentRepository, times(1)).findBySubmissionId("sub-123");
    }

    @Test
    void testGetDocumentsBySubmissionIdAndTenant() {
        List<SubmissionDocument> documents = Arrays.asList(testDocument);
        when(documentRepository.findBySubmissionIdAndTenantId("sub-123", "tenant-1")).thenReturn(documents);

        List<SubmissionDocument> result = documentService.getDocumentsBySubmissionIdAndTenant("sub-123", "tenant-1");

        assertEquals(1, result.size());
        assertEquals("tenant-1", result.get(0).getTenantId());
        verify(documentRepository, times(1)).findBySubmissionIdAndTenantId("sub-123", "tenant-1");
    }

    @Test
    void testGetDocumentById() {
        when(documentRepository.findById("doc-123")).thenReturn(Optional.of(testDocument));

        Optional<SubmissionDocument> result = documentService.getDocumentById("doc-123");

        assertTrue(result.isPresent());
        assertEquals("doc-123", result.get().getId());
        verify(documentRepository, times(1)).findById("doc-123");
    }

    @Test
    void testGetDocumentById_NotFound() {
        when(documentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<SubmissionDocument> result = documentService.getDocumentById("nonexistent");

        assertFalse(result.isPresent());
        verify(documentRepository, times(1)).findById("nonexistent");
    }

    @Test
    void testSaveDocument() {
        when(documentRepository.save(testDocument)).thenReturn(testDocument);

        SubmissionDocument result = documentService.saveDocument(testDocument);

        assertNotNull(result);
        assertEquals("doc-123", result.getId());
        verify(documentRepository, times(1)).save(testDocument);
    }

    @Test
    void testSaveDocuments() {
        List<SubmissionDocument> documents = Arrays.asList(testDocument);
        when(documentRepository.saveAll(documents)).thenReturn(documents);

        List<SubmissionDocument> result = documentService.saveDocuments(documents);

        assertEquals(1, result.size());
        assertEquals("doc-123", result.get(0).getId());
        verify(documentRepository, times(1)).saveAll(documents);
    }
}
