package com.munitax.submission.service;

import com.munitax.submission.model.SubmissionDocument;
import com.munitax.submission.repository.SubmissionDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubmissionDocumentService {
    
    private final SubmissionDocumentRepository documentRepository;
    
    public SubmissionDocumentService(SubmissionDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }
    
    /**
     * Get all documents for a submission
     */
    public List<SubmissionDocument> getDocumentsBySubmissionId(String submissionId) {
        return documentRepository.findBySubmissionId(submissionId);
    }
    
    /**
     * Get all documents for a submission filtered by tenant
     */
    public List<SubmissionDocument> getDocumentsBySubmissionIdAndTenant(String submissionId, String tenantId) {
        return documentRepository.findBySubmissionIdAndTenantId(submissionId, tenantId);
    }
    
    /**
     * Get a specific document by ID
     */
    public Optional<SubmissionDocument> getDocumentById(String documentId) {
        return documentRepository.findById(documentId);
    }
    
    /**
     * Save a document
     */
    public SubmissionDocument saveDocument(SubmissionDocument document) {
        return documentRepository.save(document);
    }
    
    /**
     * Save multiple documents
     */
    public List<SubmissionDocument> saveDocuments(List<SubmissionDocument> documents) {
        return documentRepository.saveAll(documents);
    }
}
