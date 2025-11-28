package com.munitax.submission.repository;

import com.munitax.submission.model.DocumentRequest;
import com.munitax.submission.model.DocumentRequest.RequestStatus;
import com.munitax.submission.model.DocumentRequest.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, String> {
    
    // Find by return ID
    List<DocumentRequest> findByReturnId(String returnId);
    Page<DocumentRequest> findByReturnId(String returnId, Pageable pageable);
    
    // Find by auditor
    List<DocumentRequest> findByAuditorId(String auditorId);
    
    // Find by status
    List<DocumentRequest> findByStatus(RequestStatus status);
    Page<DocumentRequest> findByStatus(RequestStatus status, Pageable pageable);
    
    // Find by document type
    List<DocumentRequest> findByDocumentType(DocumentType documentType);
    
    // Find overdue requests
    @Query("SELECT dr FROM DocumentRequest dr WHERE dr.status = :status AND dr.deadline < :today")
    List<DocumentRequest> findOverdueRequests(@Param("today") LocalDate today, @Param("status") RequestStatus status);
    
    // Find requests approaching deadline (within days)
    @Query("SELECT dr FROM DocumentRequest dr WHERE dr.status = :status AND dr.deadline <= :targetDate AND dr.deadline >= :today")
    List<DocumentRequest> findApproachingDeadline(@Param("today") LocalDate today, @Param("targetDate") LocalDate targetDate, @Param("status") RequestStatus status);
    
    // Find by tenant
    List<DocumentRequest> findByTenantId(String tenantId);
    
    // Count by status
    long countByStatus(RequestStatus status);
    
    // Count pending requests for a return
    long countByReturnIdAndStatus(String returnId, RequestStatus status);
}
