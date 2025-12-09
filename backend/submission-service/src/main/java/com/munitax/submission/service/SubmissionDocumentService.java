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
     * Get all documents for a submission.
     * This is a read-only operation.
     * 
     * @param submissionId The submission ID (must not be null)
     * @return List of documents, empty list if none found
     */
    public List<SubmissionDocument> getDocumentsBySubmissionId(String submissionId) {
        return documentRepository.findBySubmissionId(submissionId);
    }
    
    /**
     * Get all documents for a submission filtered by tenant.
     * This is a read-only operation used for multi-tenant isolation.
     * 
     * @param submissionId The submission ID (must not be null)
     * @param tenantId The tenant ID for filtering (must not be null)
     * @return List of documents for the specified tenant, empty list if none found
     */
    public List<SubmissionDocument> getDocumentsBySubmissionIdAndTenant(String submissionId, String tenantId) {
        return documentRepository.findBySubmissionIdAndTenantId(submissionId, tenantId);
    }
    
    /**
     * Get a specific document by ID.
     * This is a read-only operation.
     * 
     * @param documentId The document ID (must not be null)
     * @return Optional containing the document if found, empty Optional otherwise
     */
    public Optional<SubmissionDocument> getDocumentById(String documentId) {
        return documentRepository.findById(documentId);
    }
    
    /**
     * Save a single document.
     * This operation is not transactional at this level - transaction management
     * should be handled by the caller.
     * 
     * @param document The document to save (must not be null)
     * @return The saved document with generated ID
     */
    public SubmissionDocument saveDocument(SubmissionDocument document) {
        return documentRepository.save(document);
    }
    
    /**
     * Save multiple documents in batch.
     * This operation is not transactional at this level - transaction management
     * should be handled by the caller. For optimal performance with large batches,
     * ensure batch inserts are enabled in JPA configuration.
     * 
     * @param documents The list of documents to save (must not be null)
     * @return List of saved documents with generated IDs
     */
    public List<SubmissionDocument> saveDocuments(List<SubmissionDocument> documents) {
        return documentRepository.saveAll(documents);
    }
}
