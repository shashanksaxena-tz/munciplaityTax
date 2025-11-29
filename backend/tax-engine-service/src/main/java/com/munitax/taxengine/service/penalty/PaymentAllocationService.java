package com.munitax.taxengine.service.penalty;

import com.munitax.taxengine.domain.penalty.AllocationOrder;
import com.munitax.taxengine.domain.penalty.PaymentAllocation;
import com.munitax.taxengine.dto.PaymentAllocationRequest;
import com.munitax.taxengine.dto.PaymentAllocationResponse;
import com.munitax.taxengine.repository.PaymentAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for allocating payments to tax, penalties, and interest.
 * 
 * Functional Requirements:
 * - FR-040 to FR-043: Payment allocation order (Tax → Penalties → Interest)
 * - FR-041: IRS standard allocation order
 * - FR-042: Display allocation breakdown
 * - FR-043: Recalculate penalties and interest after payment
 * 
 * User Story 2: Late Payment Penalty (Payment Handling)
 * Applies payments in the standard IRS order:
 * 1. Tax principal first
 * 2. Penalties second
 * 3. Interest last
 * 
 * Tracks multiple payments with dates and recalculates penalties on remaining balance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAllocationService {
    
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final LatePaymentPenaltyService latePaymentPenaltyService;
    
    private static final int SCALE = 2; // 2 decimal places for currency
    
    /**
     * Allocate a payment to tax, penalties, and interest in IRS standard order.
     * 
     * FR-041: Apply in order: Tax → Penalties → Interest
     * FR-042: Provide detailed allocation breakdown
     * FR-043: Optionally recalculate penalties/interest after payment
     * 
     * @param request the payment allocation request
     * @return payment allocation response with breakdown
     */
    @Transactional
    public PaymentAllocationResponse allocatePayment(PaymentAllocationRequest request) {
        log.info("Allocating payment of ${} for return: {}", 
                request.getPaymentAmount(), request.getReturnId());
        
        // Validate request
        validateRequest(request);
        
        // Initialize allocation amounts
        BigDecimal remainingPayment = request.getPaymentAmount();
        BigDecimal appliedToTax = BigDecimal.ZERO;
        BigDecimal appliedToPenalties = BigDecimal.ZERO;
        BigDecimal appliedToInterest = BigDecimal.ZERO;
        
        // Initialize remaining balances
        BigDecimal remainingTaxBalance = request.getCurrentTaxBalance();
        BigDecimal remainingPenaltyBalance = request.getCurrentPenaltyBalance();
        BigDecimal remainingInterestBalance = request.getCurrentInterestBalance();
        
        // FR-041: Step 1 - Apply to tax principal first
        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0 && 
                remainingTaxBalance.compareTo(BigDecimal.ZERO) > 0) {
            
            BigDecimal taxPayment = remainingPayment.min(remainingTaxBalance);
            appliedToTax = taxPayment.setScale(SCALE, RoundingMode.HALF_UP);
            remainingTaxBalance = remainingTaxBalance.subtract(appliedToTax)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            remainingPayment = remainingPayment.subtract(appliedToTax)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            
            log.debug("Applied ${} to tax principal. Remaining tax: ${}, remaining payment: ${}", 
                    appliedToTax, remainingTaxBalance, remainingPayment);
        }
        
        // FR-041: Step 2 - Apply to penalties second
        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0 && 
                remainingPenaltyBalance.compareTo(BigDecimal.ZERO) > 0) {
            
            BigDecimal penaltyPayment = remainingPayment.min(remainingPenaltyBalance);
            appliedToPenalties = penaltyPayment.setScale(SCALE, RoundingMode.HALF_UP);
            remainingPenaltyBalance = remainingPenaltyBalance.subtract(appliedToPenalties)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            remainingPayment = remainingPayment.subtract(appliedToPenalties)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            
            log.debug("Applied ${} to penalties. Remaining penalties: ${}, remaining payment: ${}", 
                    appliedToPenalties, remainingPenaltyBalance, remainingPayment);
        }
        
        // FR-041: Step 3 - Apply to interest last
        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0 && 
                remainingInterestBalance.compareTo(BigDecimal.ZERO) > 0) {
            
            BigDecimal interestPayment = remainingPayment.min(remainingInterestBalance);
            appliedToInterest = interestPayment.setScale(SCALE, RoundingMode.HALF_UP);
            remainingInterestBalance = remainingInterestBalance.subtract(appliedToInterest)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            remainingPayment = remainingPayment.subtract(appliedToInterest)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            
            log.debug("Applied ${} to interest. Remaining interest: ${}, remaining payment: ${}", 
                    appliedToInterest, remainingInterestBalance, remainingPayment);
        }
        
        // Log any overpayment
        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Overpayment of ${} - this amount was not allocated", remainingPayment);
        }
        
        // Create and save payment allocation entity
        PaymentAllocation allocation = buildAllocationEntity(
                request, 
                appliedToTax, 
                appliedToPenalties, 
                appliedToInterest,
                remainingTaxBalance,
                remainingPenaltyBalance,
                remainingInterestBalance
        );
        
        PaymentAllocation savedAllocation = paymentAllocationRepository.save(allocation);
        
        log.info("Payment allocation saved: {} - Tax: ${}, Penalties: ${}, Interest: ${}", 
                savedAllocation.getId(), appliedToTax, appliedToPenalties, appliedToInterest);
        
        // FR-043: Recalculate penalties if requested and tax still outstanding
        if (Boolean.TRUE.equals(request.getRecalculateAfterPayment()) && 
                remainingTaxBalance.compareTo(BigDecimal.ZERO) > 0) {
            
            log.info("Recalculating late payment penalty after payment");
            try {
                latePaymentPenaltyService.recalculateAfterPayment(
                        request.getReturnId().toString(),
                        request.getTenantId().toString(),
                        remainingTaxBalance,
                        request.getPaymentDate(),
                        request.getCreatedBy().toString()
                );
            } catch (Exception e) {
                log.warn("Failed to recalculate penalty after payment: {}", e.getMessage());
                // Don't fail the allocation if recalculation fails
            }
        }
        
        // Build and return response
        return buildResponse(savedAllocation, request);
    }
    
    /**
     * Get the most recent payment allocation for a return.
     * Useful for determining current balances.
     * 
     * @param returnId the return ID
     * @param tenantId the tenant ID
     * @return most recent payment allocation or null if none
     */
    @Transactional(readOnly = true)
    public PaymentAllocationResponse getMostRecentAllocation(String returnId, String tenantId) {
        return paymentAllocationRepository
                .findMostRecentByReturnIdAndTenantId(
                        java.util.UUID.fromString(returnId),
                        java.util.UUID.fromString(tenantId))
                .map(this::buildResponseFromEntity)
                .orElse(null);
    }
    
    /**
     * Get all payment allocations for a return.
     * Provides complete payment history.
     * 
     * @param returnId the return ID
     * @param tenantId the tenant ID
     * @return list of payment allocations ordered by date
     */
    @Transactional(readOnly = true)
    public java.util.List<PaymentAllocationResponse> getPaymentHistory(String returnId, String tenantId) {
        return paymentAllocationRepository
                .findByReturnIdAndTenantIdOrderByPaymentDate(
                        java.util.UUID.fromString(returnId),
                        java.util.UUID.fromString(tenantId))
                .stream()
                .map(this::buildResponseFromEntity)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate total payments made for a return.
     * 
     * @param returnId the return ID
     * @param tenantId the tenant ID
     * @return total payment amount
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPayments(String returnId, String tenantId) {
        return paymentAllocationRepository.calculateTotalPayments(
                java.util.UUID.fromString(returnId),
                java.util.UUID.fromString(tenantId));
    }
    
    /**
     * Build payment allocation entity.
     */
    private PaymentAllocation buildAllocationEntity(
            PaymentAllocationRequest request,
            BigDecimal appliedToTax,
            BigDecimal appliedToPenalties,
            BigDecimal appliedToInterest,
            BigDecimal remainingTaxBalance,
            BigDecimal remainingPenaltyBalance,
            BigDecimal remainingInterestBalance) {
        
        return PaymentAllocation.builder()
                .tenantId(request.getTenantId())
                .returnId(request.getReturnId())
                .paymentDate(request.getPaymentDate())
                .paymentAmount(request.getPaymentAmount())
                .appliedToTax(appliedToTax)
                .appliedToPenalties(appliedToPenalties)
                .appliedToInterest(appliedToInterest)
                .remainingTaxBalance(remainingTaxBalance)
                .remainingPenaltyBalance(remainingPenaltyBalance)
                .remainingInterestBalance(remainingInterestBalance)
                .allocationOrder(AllocationOrder.TAX_FIRST)
                .createdBy(request.getCreatedBy())
                .build();
    }
    
    /**
     * Build response from saved allocation and request.
     */
    private PaymentAllocationResponse buildResponse(
            PaymentAllocation allocation, 
            PaymentAllocationRequest request) {
        
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
                .paymentMethod(request.getPaymentMethod())
                .paymentReference(request.getPaymentReference())
                .build();
        
        // Generate explanation
        response.setAllocationExplanation(response.generateAllocationExplanation());
        
        return response;
    }
    
    /**
     * Build response from existing allocation entity.
     */
    private PaymentAllocationResponse buildResponseFromEntity(PaymentAllocation allocation) {
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
    
    /**
     * Validate payment allocation request.
     */
    private void validateRequest(PaymentAllocationRequest request) {
        if (request.getTenantId() == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (request.getReturnId() == null) {
            throw new IllegalArgumentException("Return ID is required");
        }
        if (request.getPaymentDate() == null) {
            throw new IllegalArgumentException("Payment date is required");
        }
        if (request.getPaymentAmount() == null || 
                request.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valid payment amount is required (must be positive)");
        }
        if (request.getCurrentTaxBalance() == null || 
                request.getCurrentTaxBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid current tax balance is required");
        }
        if (request.getCurrentPenaltyBalance() == null || 
                request.getCurrentPenaltyBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid current penalty balance is required");
        }
        if (request.getCurrentInterestBalance() == null || 
                request.getCurrentInterestBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid current interest balance is required");
        }
        if (request.getCreatedBy() == null) {
            throw new IllegalArgumentException("Created by is required");
        }
    }
}
