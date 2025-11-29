package com.munitax.taxengine.controller;

import com.munitax.taxengine.domain.penalty.PaymentAllocation;
import com.munitax.taxengine.dto.PaymentAllocationRequest;
import com.munitax.taxengine.dto.PaymentAllocationResponse;
import com.munitax.taxengine.repository.PaymentAllocationRepository;
import com.munitax.taxengine.service.penalty.PaymentAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for payment allocation operations.
 * 
 * Provides endpoints for:
 * - Allocating payments to tax, penalties, and interest (FR-040 to FR-043)
 * - Retrieving payment allocation details
 * - Getting payment history for a return
 * 
 * Functional Requirements:
 * - FR-040 to FR-043: Payment allocation order (Tax → Penalties → Interest)
 * - FR-041: IRS standard allocation order
 * - FR-042: Display allocation breakdown
 * - FR-043: Recalculate penalties and interest after payment
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentAllocationController {
    
    private final PaymentAllocationService paymentAllocationService;
    private final PaymentAllocationRepository paymentAllocationRepository;
    
    // TODO: Replace with actual authentication service
    private static final UUID MOCK_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    
    /**
     * Allocate a payment to tax, penalties, and interest.
     * Applies IRS standard allocation order: Tax → Penalties → Interest
     * 
     * POST /api/payments/allocate
     * 
     * @param request payment allocation request
     * @return payment allocation response with breakdown
     */
    @PostMapping("/allocate")
    public ResponseEntity<PaymentAllocationResponse> allocatePayment(
            @Valid @RequestBody PaymentAllocationRequest request) {
        
        log.info("Allocating payment of ${} for return: {}", 
                request.getPaymentAmount(), request.getReturnId());
        
        try {
            // Set tenant ID and user ID from security context (mocked for now)
            if (request.getTenantId() == null) {
                request.setTenantId(MOCK_TENANT_ID);
            }
            if (request.getCreatedBy() == null) {
                request.setCreatedBy(MOCK_USER_ID);
            }
            
            // Allocate payment
            PaymentAllocationResponse response = paymentAllocationService.allocatePayment(request);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid payment allocation request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error allocating payment", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to allocate payment: " + e.getMessage());
        }
    }
    
    /**
     * Get payment allocation details by ID.
     * 
     * GET /api/payments/{id}
     * 
     * @param id payment allocation ID
     * @return payment allocation details
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentAllocationResponse> getPaymentAllocationById(
            @PathVariable String id) {
        
        log.info("Retrieving payment allocation: {}", id);
        
        try {
            UUID allocationId = UUID.fromString(id);
            PaymentAllocation allocation = paymentAllocationRepository
                    .findByIdAndTenantId(allocationId, MOCK_TENANT_ID)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Payment allocation not found: " + id));
            
            PaymentAllocationResponse response = buildResponseFromAllocation(allocation);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invalid payment allocation ID format");
        }
    }
    
    /**
     * Get all payment allocations with pagination.
     * 
     * GET /api/payments?page=0&size=20&sort=paymentDate,desc
     * 
     * @param page page number (default 0)
     * @param size page size (default 20)
     * @param sortBy sort field (default paymentDate)
     * @param sortDir sort direction (default DESC)
     * @return page of payment allocations
     */
    @GetMapping
    public ResponseEntity<Page<PaymentAllocationResponse>> getPaymentAllocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        log.info("Retrieving payment allocations: page={}, size={}, sortBy={}, sortDir={}", 
                page, size, sortBy, sortDir);
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<PaymentAllocation> allocations = paymentAllocationRepository
                    .findByTenantIdOrderByPaymentDateDesc(MOCK_TENANT_ID, pageable);
            
            Page<PaymentAllocationResponse> response = allocations
                    .map(this::buildResponseFromAllocation);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving payment allocations", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to retrieve payment allocations: " + e.getMessage());
        }
    }
    
    /**
     * Get payment history for a specific tax return.
     * Returns all payment allocations ordered by payment date.
     * 
     * GET /api/payments/return/{returnId}
     * 
     * @param returnId tax return ID
     * @return list of payment allocations for the return
     */
    @GetMapping("/return/{returnId}")
    public ResponseEntity<List<PaymentAllocationResponse>> getPaymentHistory(
            @PathVariable String returnId) {
        
        log.info("Retrieving payment history for return: {}", returnId);
        
        try {
            List<PaymentAllocationResponse> response = paymentAllocationService
                    .getPaymentHistory(returnId, MOCK_TENANT_ID.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid return ID format");
        } catch (Exception e) {
            log.error("Error retrieving payment history", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to retrieve payment history: " + e.getMessage());
        }
    }
    
    /**
     * Get most recent payment allocation for a return.
     * Useful for determining current balances.
     * 
     * GET /api/payments/return/{returnId}/latest
     * 
     * @param returnId tax return ID
     * @return most recent payment allocation or 404 if none
     */
    @GetMapping("/return/{returnId}/latest")
    public ResponseEntity<PaymentAllocationResponse> getLatestPaymentAllocation(
            @PathVariable String returnId) {
        
        log.info("Retrieving latest payment allocation for return: {}", returnId);
        
        try {
            PaymentAllocationResponse response = paymentAllocationService
                    .getMostRecentAllocation(returnId, MOCK_TENANT_ID.toString());
            
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "No payment allocations found for return: " + returnId);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid return ID format");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving latest payment allocation", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to retrieve latest payment allocation: " + e.getMessage());
        }
    }
    
    /**
     * Get total payments for a return.
     * 
     * GET /api/payments/return/{returnId}/total
     * 
     * @param returnId tax return ID
     * @return total payment amount
     */
    @GetMapping("/return/{returnId}/total")
    public ResponseEntity<BigDecimal> getTotalPayments(@PathVariable String returnId) {
        log.info("Retrieving total payments for return: {}", returnId);
        
        try {
            BigDecimal total = paymentAllocationService
                    .getTotalPayments(returnId, MOCK_TENANT_ID.toString());
            
            return ResponseEntity.ok(total);
            
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid return ID format");
        } catch (Exception e) {
            log.error("Error retrieving total payments", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to retrieve total payments: " + e.getMessage());
        }
    }
    
    /**
     * Build response from payment allocation entity.
     */
    private PaymentAllocationResponse buildResponseFromAllocation(PaymentAllocation allocation) {
        boolean fullyPaid = allocation.isFullyPaid();
        boolean taxFullyPaid = allocation.isTaxFullyPaid();
        BigDecimal totalRemaining = allocation.getTotalRemainingBalance();
        
        PaymentAllocationResponse response = PaymentAllocationResponse.builder()
                .allocationId(allocation.getId().toString())
                .returnId(allocation.getReturnId().toString())
                .paymentDate(allocation.getPaymentDate())
                .paymentAmount(allocation.getPaymentAmount())
                .appliedToTax(allocation.getAppliedToTax())
                .appliedToPenalties(allocation.getAppliedToPenalties())
                .appliedToInterest(allocation.getAppliedToInterest())
                .remainingTaxBalance(allocation.getRemainingTaxBalance())
                .remainingPenaltyBalance(allocation.getRemainingPenaltyBalance())
                .remainingInterestBalance(allocation.getRemainingInterestBalance())
                .totalRemainingBalance(totalRemaining)
                .allocationOrder("TAX_FIRST")
                .fullyPaid(fullyPaid)
                .taxFullyPaid(taxFullyPaid)
                .createdAt(allocation.getCreatedAt())
                .build();
        
        // Generate explanation
        response.setAllocationExplanation(response.generateAllocationExplanation());
        
        return response;
    }
}
