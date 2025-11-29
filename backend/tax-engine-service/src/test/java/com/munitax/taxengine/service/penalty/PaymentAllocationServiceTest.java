package com.munitax.taxengine.service.penalty;

import com.munitax.taxengine.domain.penalty.AllocationOrder;
import com.munitax.taxengine.domain.penalty.PaymentAllocation;
import com.munitax.taxengine.dto.PaymentAllocationRequest;
import com.munitax.taxengine.dto.PaymentAllocationResponse;
import com.munitax.taxengine.repository.PaymentAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentAllocationService.
 * 
 * Tests:
 * - IRS standard allocation order: Tax → Penalties → Interest
 * - Allocation breakdown with multiple categories
 * - Partial payments
 * - Full payment scenarios
 * - Overpayment handling
 */
@ExtendWith(MockitoExtension.class)
class PaymentAllocationServiceTest {
    
    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;
    
    @Mock
    private LatePaymentPenaltyService latePaymentPenaltyService;
    
    @InjectMocks
    private PaymentAllocationService paymentAllocationService;
    
    private UUID testTenantId;
    private UUID testReturnId;
    private UUID testUserId;
    private LocalDate paymentDate;
    
    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        testReturnId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        paymentDate = LocalDate.of(2024, 6, 15);
    }
    
    @Test
    @DisplayName("Should apply payment to tax first (IRS standard order)")
    void shouldApplyPaymentToTaxFirst() {
        // Given: $1,000 payment, $800 tax due, $200 penalties, $100 interest
        PaymentAllocationRequest request = PaymentAllocationRequest.builder()
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .paymentDate(paymentDate)
                .paymentAmount(new BigDecimal("1000.00"))
                .currentTaxBalance(new BigDecimal("800.00"))
                .currentPenaltyBalance(new BigDecimal("200.00"))
                .currentInterestBalance(new BigDecimal("100.00"))
                .createdBy(testUserId)
                .build();
        
        PaymentAllocation savedAllocation = createMockAllocation(
                new BigDecimal("800.00"),  // applied to tax
                new BigDecimal("200.00"),  // applied to penalties
                new BigDecimal("0.00"),    // applied to interest
                BigDecimal.ZERO,           // remaining tax
                BigDecimal.ZERO,           // remaining penalties
                new BigDecimal("100.00")   // remaining interest
        );
        
        when(paymentAllocationRepository.save(any(PaymentAllocation.class)))
                .thenReturn(savedAllocation);
        
        // When
        PaymentAllocationResponse response = paymentAllocationService.allocatePayment(request);
        
        // Then: Tax paid in full ($800), penalties paid in full ($200), interest unpaid
        assertThat(response).isNotNull();
        assertThat(response.getAppliedToTax()).isEqualByComparingTo("800.00");
        assertThat(response.getAppliedToPenalties()).isEqualByComparingTo("200.00");
        assertThat(response.getAppliedToInterest()).isEqualByComparingTo("0.00");
        assertThat(response.getRemainingTaxBalance()).isEqualByComparingTo("0.00");
        assertThat(response.getRemainingPenaltyBalance()).isEqualByComparingTo("0.00");
        assertThat(response.getRemainingInterestBalance()).isEqualByComparingTo("100.00");
        assertThat(response.getTaxFullyPaid()).isTrue();
        assertThat(response.getFullyPaid()).isFalse();
        
        verify(paymentAllocationRepository).save(any(PaymentAllocation.class));
    }
    
    @Test
    @DisplayName("Should apply payment to penalties second")
    void shouldApplyPaymentToPenaltiesSecond() {
        // Given: $500 payment, $300 tax due, $200 penalties, $100 interest
        PaymentAllocationRequest request = PaymentAllocationRequest.builder()
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .paymentDate(paymentDate)
                .paymentAmount(new BigDecimal("500.00"))
                .currentTaxBalance(new BigDecimal("300.00"))
                .currentPenaltyBalance(new BigDecimal("200.00"))
                .currentInterestBalance(new BigDecimal("100.00"))
                .createdBy(testUserId)
                .build();
        
        PaymentAllocation savedAllocation = createMockAllocation(
                new BigDecimal("300.00"),  // applied to tax
                new BigDecimal("200.00"),  // applied to penalties
                BigDecimal.ZERO,           // applied to interest
                BigDecimal.ZERO,           // remaining tax
                BigDecimal.ZERO,           // remaining penalties
                new BigDecimal("100.00")   // remaining interest
        );
        
        when(paymentAllocationRepository.save(any(PaymentAllocation.class)))
                .thenReturn(savedAllocation);
        
        // When
        PaymentAllocationResponse response = paymentAllocationService.allocatePayment(request);
        
        // Then: Tax and penalties paid in full, interest remains
        assertThat(response).isNotNull();
        assertThat(response.getAppliedToTax()).isEqualByComparingTo("300.00");
        assertThat(response.getAppliedToPenalties()).isEqualByComparingTo("200.00");
        assertThat(response.getAppliedToInterest()).isEqualByComparingTo("0.00");
        assertThat(response.getTaxFullyPaid()).isTrue();
        
        verify(paymentAllocationRepository).save(any(PaymentAllocation.class));
    }
    
    @Test
    @DisplayName("Should apply payment to interest last")
    void shouldApplyPaymentToInterestLast() {
        // Given: $700 payment, $300 tax due, $200 penalties, $100 interest
        PaymentAllocationRequest request = PaymentAllocationRequest.builder()
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .paymentDate(paymentDate)
                .paymentAmount(new BigDecimal("700.00"))
                .currentTaxBalance(new BigDecimal("300.00"))
                .currentPenaltyBalance(new BigDecimal("200.00"))
                .currentInterestBalance(new BigDecimal("150.00"))
                .createdBy(testUserId)
                .build();
        
        PaymentAllocation savedAllocation = createMockAllocation(
                new BigDecimal("300.00"),  // applied to tax
                new BigDecimal("200.00"),  // applied to penalties
                new BigDecimal("150.00"),  // applied to interest (only $150 needed)
                BigDecimal.ZERO,           // remaining tax
                BigDecimal.ZERO,           // remaining penalties
                BigDecimal.ZERO            // remaining interest
        );
        
        when(paymentAllocationRepository.save(any(PaymentAllocation.class)))
                .thenReturn(savedAllocation);
        
        // When
        PaymentAllocationResponse response = paymentAllocationService.allocatePayment(request);
        
        // Then: All balances paid in full
        assertThat(response).isNotNull();
        assertThat(response.getAppliedToTax()).isEqualByComparingTo("300.00");
        assertThat(response.getAppliedToPenalties()).isEqualByComparingTo("200.00");
        assertThat(response.getAppliedToInterest()).isEqualByComparingTo("150.00");
        assertThat(response.getFullyPaid()).isTrue();
        
        verify(paymentAllocationRepository).save(any(PaymentAllocation.class));
    }
    
    @Test
    @DisplayName("Should handle partial payment to tax only")
    void shouldHandlePartialPaymentToTaxOnly() {
        // Given: $100 payment, $500 tax due
        PaymentAllocationRequest request = PaymentAllocationRequest.builder()
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .paymentDate(paymentDate)
                .paymentAmount(new BigDecimal("100.00"))
                .currentTaxBalance(new BigDecimal("500.00"))
                .currentPenaltyBalance(BigDecimal.ZERO)
                .currentInterestBalance(BigDecimal.ZERO)
                .createdBy(testUserId)
                .build();
        
        PaymentAllocation savedAllocation = createMockAllocation(
                new BigDecimal("100.00"),  // applied to tax
                BigDecimal.ZERO,           // applied to penalties
                BigDecimal.ZERO,           // applied to interest
                new BigDecimal("400.00"),  // remaining tax
                BigDecimal.ZERO,           // remaining penalties
                BigDecimal.ZERO            // remaining interest
        );
        
        when(paymentAllocationRepository.save(any(PaymentAllocation.class)))
                .thenReturn(savedAllocation);
        
        // When
        PaymentAllocationResponse response = paymentAllocationService.allocatePayment(request);
        
        // Then: Partial payment to tax, $400 remains
        assertThat(response).isNotNull();
        assertThat(response.getAppliedToTax()).isEqualByComparingTo("100.00");
        assertThat(response.getRemainingTaxBalance()).isEqualByComparingTo("400.00");
        assertThat(response.getTaxFullyPaid()).isFalse();
        
        verify(paymentAllocationRepository).save(any(PaymentAllocation.class));
    }
    
    @Test
    @DisplayName("Should handle payment that covers everything with overpayment")
    void shouldHandleOverpayment() {
        // Given: $1,000 payment, only $600 owed
        PaymentAllocationRequest request = PaymentAllocationRequest.builder()
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .paymentDate(paymentDate)
                .paymentAmount(new BigDecimal("1000.00"))
                .currentTaxBalance(new BigDecimal("300.00"))
                .currentPenaltyBalance(new BigDecimal("200.00"))
                .currentInterestBalance(new BigDecimal("100.00"))
                .createdBy(testUserId)
                .build();
        
        PaymentAllocation savedAllocation = createMockAllocation(
                new BigDecimal("300.00"),  // applied to tax
                new BigDecimal("200.00"),  // applied to penalties
                new BigDecimal("100.00"),  // applied to interest
                BigDecimal.ZERO,           // remaining tax
                BigDecimal.ZERO,           // remaining penalties
                BigDecimal.ZERO            // remaining interest
        );
        
        when(paymentAllocationRepository.save(any(PaymentAllocation.class)))
                .thenReturn(savedAllocation);
        
        // When
        PaymentAllocationResponse response = paymentAllocationService.allocatePayment(request);
        
        // Then: All paid, $400 overpayment noted in logs
        assertThat(response).isNotNull();
        assertThat(response.getFullyPaid()).isTrue();
        assertThat(response.getTotalRemainingBalance()).isEqualByComparingTo("0.00");
        
        verify(paymentAllocationRepository).save(any(PaymentAllocation.class));
    }
    
    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
        // Given: Request missing return ID
        PaymentAllocationRequest request = PaymentAllocationRequest.builder()
                .tenantId(testTenantId)
                .paymentDate(paymentDate)
                .paymentAmount(new BigDecimal("100.00"))
                .currentTaxBalance(new BigDecimal("500.00"))
                .currentPenaltyBalance(BigDecimal.ZERO)
                .currentInterestBalance(BigDecimal.ZERO)
                .createdBy(testUserId)
                .build();
        
        // When/Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> paymentAllocationService.allocatePayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Return ID is required");
    }
    
    @Test
    @DisplayName("Should reject zero or negative payment amount")
    void shouldRejectInvalidPaymentAmount() {
        // Given: Zero payment amount
        PaymentAllocationRequest request = PaymentAllocationRequest.builder()
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .paymentDate(paymentDate)
                .paymentAmount(BigDecimal.ZERO)
                .currentTaxBalance(new BigDecimal("500.00"))
                .currentPenaltyBalance(BigDecimal.ZERO)
                .currentInterestBalance(BigDecimal.ZERO)
                .createdBy(testUserId)
                .build();
        
        // When/Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> paymentAllocationService.allocatePayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Valid payment amount is required");
    }
    
    @Test
    @DisplayName("Should reject negative balances")
    void shouldRejectNegativeBalances() {
        // Given: Negative tax balance
        PaymentAllocationRequest request = PaymentAllocationRequest.builder()
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .paymentDate(paymentDate)
                .paymentAmount(new BigDecimal("100.00"))
                .currentTaxBalance(new BigDecimal("-500.00"))
                .currentPenaltyBalance(BigDecimal.ZERO)
                .currentInterestBalance(BigDecimal.ZERO)
                .createdBy(testUserId)
                .build();
        
        // When/Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> paymentAllocationService.allocatePayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Valid current tax balance is required");
    }
    
    // Helper methods
    
    private PaymentAllocation createMockAllocation(
            BigDecimal appliedToTax,
            BigDecimal appliedToPenalties,
            BigDecimal appliedToInterest,
            BigDecimal remainingTax,
            BigDecimal remainingPenalties,
            BigDecimal remainingInterest) {
        
        return PaymentAllocation.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .paymentDate(paymentDate)
                .paymentAmount(appliedToTax.add(appliedToPenalties).add(appliedToInterest))
                .appliedToTax(appliedToTax)
                .appliedToPenalties(appliedToPenalties)
                .appliedToInterest(appliedToInterest)
                .remainingTaxBalance(remainingTax)
                .remainingPenaltyBalance(remainingPenalties)
                .remainingInterestBalance(remainingInterest)
                .allocationOrder(AllocationOrder.TAX_FIRST)
                .createdBy(testUserId)
                .build();
    }
}
