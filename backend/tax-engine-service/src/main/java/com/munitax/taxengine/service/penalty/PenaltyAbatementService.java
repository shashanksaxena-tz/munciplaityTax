package com.munitax.taxengine.service.penalty;

import com.munitax.taxengine.domain.penalty.*;
import com.munitax.taxengine.repository.PenaltyAbatementRepository;
import com.munitax.taxengine.repository.PenaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for penalty abatement workflow and first-time penalty abatement eligibility.
 * 
 * Functional Requirements:
 * - FR-033 to FR-039: Penalty abatement workflow
 * - FR-033: Display penalty abatement request option in penalty summary screen
 * - FR-034: Support valid abatement reasons (Death, Illness, Disaster, etc.)
 * - FR-035: Validate first-time penalty abatement eligibility (no penalties in prior 3 years)
 * - FR-036: Generate Form 27-PA (Penalty Abatement Request) PDF via pdf-service
 * - FR-037: Track abatement status: PENDING | APPROVED | PARTIAL | DENIED | WITHDRAWN
 * - FR-038: Allow document upload (supporting evidence)
 * - FR-039: Administrator can review and approve/deny abatement requests
 * 
 * User Story 7: Penalty Abatement
 * As a tax administrator, I want the system to provide a penalty abatement workflow 
 * (request → review → approve/deny) and validate first-time penalty abatement eligibility 
 * (no penalties in prior 3 years) so that taxpayers can request relief from penalties.
 * 
 * Manages the complete lifecycle of penalty abatement requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PenaltyAbatementService {
    
    private final PenaltyAbatementRepository abatementRepository;
    private final PenaltyRepository penaltyRepository;
    // TODO: Inject PDF service for Form 27-PA generation
    // private final PdfGenerationService pdfService;
    
    private static final int FIRST_TIME_LOOKBACK_YEARS = 3;
    
    /**
     * Submit a penalty abatement request.
     * 
     * FR-033: Display penalty abatement request option
     * FR-034: Support valid abatement reasons
     * 
     * @param tenantId        the tenant ID
     * @param returnId        the return ID
     * @param penaltyId       the specific penalty to abate (null for all)
     * @param abatementType   the type of abatement
     * @param requestedAmount the amount to abate
     * @param reason          the reason for abatement
     * @param explanation     detailed explanation
     * @param requestedBy     the user requesting abatement
     * @return created penalty abatement request
     */
    @Transactional
    public PenaltyAbatement submitAbatementRequest(
            UUID tenantId,
            UUID returnId,
            UUID penaltyId,
            AbatementType abatementType,
            BigDecimal requestedAmount,
            AbatementReason reason,
            String explanation,
            UUID requestedBy) {
        
        log.info("Submitting penalty abatement request for return: {}, penalty: {}", 
                returnId, penaltyId);
        
        // Validate request
        validateAbatementRequest(tenantId, returnId, requestedAmount, reason, requestedBy);
        
        // Validate penalty exists if specific penalty ID provided
        if (penaltyId != null) {
            Penalty penalty = penaltyRepository.findById(penaltyId)
                    .orElseThrow(() -> new IllegalArgumentException("Penalty not found: " + penaltyId));
            
            if (!penalty.getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("Penalty does not belong to tenant");
            }
            
            if (!penalty.getReturnId().equals(returnId)) {
                throw new IllegalArgumentException("Penalty does not belong to return");
            }
        }
        
        // FR-035: Check first-time penalty abatement eligibility if reason is FIRST_TIME
        boolean isFirstTimeEligible = false;
        if (reason == AbatementReason.FIRST_TIME) {
            isFirstTimeEligible = validateFirstTimeEligibility(tenantId, returnId);
            if (!isFirstTimeEligible) {
                throw new IllegalStateException(
                        "Not eligible for first-time penalty abatement: penalties found in prior " 
                        + FIRST_TIME_LOOKBACK_YEARS + " years");
            }
            log.info("First-time penalty abatement eligibility validated");
        }
        
        // Create abatement request
        PenaltyAbatement abatement = PenaltyAbatement.builder()
                .tenantId(tenantId)
                .returnId(returnId)
                .penaltyId(penaltyId)
                .requestDate(LocalDate.now())
                .abatementType(abatementType)
                .requestedAmount(requestedAmount)
                .reason(reason)
                .explanation(explanation)
                .status(AbatementStatus.PENDING)
                .isFirstTimeAbatement(reason == AbatementReason.FIRST_TIME)
                .requestedBy(requestedBy)
                .createdBy(requestedBy)
                .build();
        
        PenaltyAbatement savedAbatement = abatementRepository.save(abatement);
        
        log.info("Penalty abatement request created: {}, status: PENDING", savedAbatement.getId());
        
        // TODO: FR-036: Generate Form 27-PA PDF
        // String formUrl = pdfService.generateForm27PA(savedAbatement);
        // savedAbatement.setFormUrl(formUrl);
        // abatementRepository.save(savedAbatement);
        
        return savedAbatement;
    }
    
    /**
     * Review and approve/deny an abatement request.
     * 
     * FR-037: Track abatement status
     * FR-039: Administrator review and approve/deny
     * 
     * @param abatementId    the abatement request ID
     * @param status         the new status (APPROVED, PARTIAL, DENIED)
     * @param approvedAmount the approved amount (if APPROVED or PARTIAL)
     * @param reviewNotes    notes from the reviewer
     * @param reviewedBy     the user reviewing the request
     * @return updated penalty abatement
     */
    @Transactional
    public PenaltyAbatement reviewAbatementRequest(
            UUID abatementId,
            AbatementStatus status,
            BigDecimal approvedAmount,
            String reviewNotes,
            UUID reviewedBy) {
        
        log.info("Reviewing penalty abatement request: {}, new status: {}", abatementId, status);
        
        // Validate status transition
        if (status != AbatementStatus.APPROVED && 
            status != AbatementStatus.PARTIAL && 
            status != AbatementStatus.DENIED) {
            throw new IllegalArgumentException("Invalid status for review: " + status);
        }
        
        // Retrieve abatement request
        PenaltyAbatement abatement = abatementRepository.findById(abatementId)
                .orElseThrow(() -> new IllegalArgumentException("Abatement request not found: " + abatementId));
        
        // Validate current status
        if (abatement.getStatus() != AbatementStatus.PENDING) {
            throw new IllegalStateException("Abatement request is not pending: " + abatement.getStatus());
        }
        
        // Update abatement
        abatement.setStatus(status);
        abatement.setReviewDate(LocalDate.now());
        abatement.setReviewedBy(reviewedBy);
        abatement.setReviewNotes(reviewNotes);
        
        if (status == AbatementStatus.APPROVED || status == AbatementStatus.PARTIAL) {
            if (approvedAmount == null || approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Approved amount is required for APPROVED/PARTIAL status");
            }
            
            if (approvedAmount.compareTo(abatement.getRequestedAmount()) > 0) {
                throw new IllegalArgumentException("Approved amount cannot exceed requested amount");
            }
            
            abatement.setApprovedAmount(approvedAmount);
            
            // Update penalty record to mark as abated
            if (abatement.getPenaltyId() != null) {
                updatePenaltyAsAbated(abatement.getPenaltyId(), approvedAmount);
            }
        } else {
            // Denied
            abatement.setApprovedAmount(BigDecimal.ZERO);
        }
        
        PenaltyAbatement updatedAbatement = abatementRepository.save(abatement);
        
        log.info("Penalty abatement request reviewed: {}, status: {}, approved amount: ${}", 
                updatedAbatement.getId(), status, approvedAmount);
        
        return updatedAbatement;
    }
    
    /**
     * Withdraw a pending abatement request.
     * 
     * FR-037: Track abatement status including WITHDRAWN
     * 
     * @param abatementId the abatement request ID
     * @param withdrawnBy the user withdrawing the request
     * @return updated penalty abatement
     */
    @Transactional
    public PenaltyAbatement withdrawAbatementRequest(UUID abatementId, UUID withdrawnBy) {
        log.info("Withdrawing penalty abatement request: {}", abatementId);
        
        PenaltyAbatement abatement = abatementRepository.findById(abatementId)
                .orElseThrow(() -> new IllegalArgumentException("Abatement request not found: " + abatementId));
        
        if (abatement.getStatus() != AbatementStatus.PENDING) {
            throw new IllegalStateException("Can only withdraw pending abatement requests");
        }
        
        abatement.setStatus(AbatementStatus.WITHDRAWN);
        abatement.setReviewDate(LocalDate.now());
        abatement.setReviewedBy(withdrawnBy);
        abatement.setReviewNotes("Withdrawn by requestor");
        
        return abatementRepository.save(abatement);
    }
    
    /**
     * Upload supporting documents for an abatement request.
     * 
     * FR-038: Allow document upload
     * 
     * @param abatementId  the abatement request ID
     * @param documentUrl  the URL of the uploaded document
     * @param documentType the type of document
     * @return updated penalty abatement
     */
    @Transactional
    public PenaltyAbatement uploadSupportingDocument(
            UUID abatementId,
            String documentUrl,
            String documentType) {
        
        log.info("Uploading supporting document for abatement request: {}", abatementId);
        
        PenaltyAbatement abatement = abatementRepository.findById(abatementId)
                .orElseThrow(() -> new IllegalArgumentException("Abatement request not found: " + abatementId));
        
        // TODO: Store document references in a separate table or as JSON
        // For now, just log the document URL
        log.info("Document uploaded: {} (type: {})", documentUrl, documentType);
        
        return abatement;
    }
    
    /**
     * Generate Form 27-PA (Penalty Abatement Request) PDF.
     * 
     * FR-036: Generate Form 27-PA PDF via pdf-service
     * 
     * @param abatementId the abatement request ID
     * @return URL of the generated PDF
     */
    public String generateForm27PA(UUID abatementId) {
        log.info("Generating Form 27-PA for abatement request: {}", abatementId);
        
        PenaltyAbatement abatement = abatementRepository.findById(abatementId)
                .orElseThrow(() -> new IllegalArgumentException("Abatement request not found: " + abatementId));
        
        // TODO: Integrate with PDF generation service
        // return pdfService.generateForm27PA(abatement);
        
        String mockPdfUrl = "/api/pdfs/form-27pa-" + abatementId + ".pdf";
        log.info("Form 27-PA generated (mock): {}", mockPdfUrl);
        
        return mockPdfUrl;
    }
    
    /**
     * Validate first-time penalty abatement eligibility.
     * 
     * FR-035: Validate first-time penalty abatement eligibility
     * Eligible if no penalties in prior 3 years.
     * 
     * @param tenantId the tenant ID
     * @param returnId the current return ID
     * @return true if eligible for first-time abatement
     */
    private boolean validateFirstTimeEligibility(UUID tenantId, UUID returnId) {
        // Get current return to determine tax year
        // For now, use lookback based on penalty dates
        LocalDate lookbackDate = LocalDate.now().minusYears(FIRST_TIME_LOOKBACK_YEARS);
        
        // Check for any non-abated penalties in prior 3 years
        List<Penalty> priorPenalties = penaltyRepository
                .findByTenantIdAndAssessmentDateAfter(tenantId, lookbackDate);
        
        // Filter out abated penalties and penalties for current return
        long nonAbatedPriorPenalties = priorPenalties.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsAbated()))
                .filter(p -> !p.getReturnId().equals(returnId))
                .count();
        
        boolean isEligible = nonAbatedPriorPenalties == 0;
        
        log.debug("First-time abatement eligibility check: {} prior penalties found in last {} years, eligible: {}",
                nonAbatedPriorPenalties, FIRST_TIME_LOOKBACK_YEARS, isEligible);
        
        return isEligible;
    }
    
    /**
     * Update penalty record to mark as abated.
     * 
     * @param penaltyId      the penalty ID
     * @param abatedAmount   the abated amount
     */
    private void updatePenaltyAsAbated(UUID penaltyId, BigDecimal abatedAmount) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalArgumentException("Penalty not found: " + penaltyId));
        
        penalty.setIsAbated(true);
        penalty.setAbatementReason("Abated via penalty abatement approval - Amount: $" + abatedAmount);
        penalty.setAbatementDate(LocalDate.now());
        
        penaltyRepository.save(penalty);
        
        log.info("Penalty marked as abated: {}, amount: ${}", penaltyId, abatedAmount);
    }
    
    /**
     * Validate abatement request parameters.
     */
    private void validateAbatementRequest(UUID tenantId, UUID returnId, 
                                         BigDecimal requestedAmount,
                                         AbatementReason reason, UUID requestedBy) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (returnId == null) {
            throw new IllegalArgumentException("Return ID is required");
        }
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valid requested amount is required");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Abatement reason is required");
        }
        if (requestedBy == null) {
            throw new IllegalArgumentException("Requested by is required");
        }
    }
}
