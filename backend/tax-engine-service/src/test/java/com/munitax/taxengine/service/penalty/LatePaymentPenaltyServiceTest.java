package com.munitax.taxengine.service.penalty;

import com.munitax.taxengine.domain.penalty.Penalty;
import com.munitax.taxengine.domain.penalty.PenaltyType;
import com.munitax.taxengine.dto.PenaltyCalculationRequest;
import com.munitax.taxengine.dto.PenaltyCalculationResponse;
import com.munitax.taxengine.repository.PenaltyRepository;
import com.munitax.taxengine.service.RuleEngineIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LatePaymentPenaltyService.
 * 
 * Tests:
 * - 1% per month late payment penalty calculation
 * - Partial month rounding up
 * - 25% maximum cap (25 months)
 * - Penalty on unpaid tax balance
 * - Recalculation after partial payments
 */
@ExtendWith(MockitoExtension.class)
class LatePaymentPenaltyServiceTest {
    
    @Mock
    private PenaltyRepository penaltyRepository;
    
    @Mock
    private RuleEngineIntegrationService ruleEngineService;
    
    @InjectMocks
    private LatePaymentPenaltyService latePaymentPenaltyService;
    
    private UUID testTenantId;
    private UUID testReturnId;
    private UUID testUserId;
    private LocalDate dueDate;
    private BigDecimal unpaidTax;
    private BigDecimal latePaymentRate;
    
    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        testReturnId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        dueDate = LocalDate.of(2024, 4, 15);
        unpaidTax = new BigDecimal("10000.00");
        latePaymentRate = new BigDecimal("0.01"); // 1% per month
        
        // Default: return 1% rate from rule engine
        when(ruleEngineService.getLatePaymentPenaltyRate(any(LocalDate.class), anyString()))
                .thenReturn(latePaymentRate);
    }
    
    @Test
    @DisplayName("Should calculate 1% penalty for 1 month late")
    void shouldCalculateOneMonthLatePaymentPenalty() {
        // Given: Payment 1 month (30 days) late
        LocalDate paymentDate = dueDate.plusMonths(1);
        PenaltyCalculationRequest request = createRequest(paymentDate);
        
        Penalty savedPenalty = createMockPenalty(1, new BigDecimal("100.00"));
        when(penaltyRepository.findByReturnIdAndPenaltyTypeAndTenantId(
                any(UUID.class), eq(PenaltyType.LATE_PAYMENT), any(UUID.class)))
                .thenReturn(Collections.emptyList());
        when(penaltyRepository.save(any(Penalty.class))).thenReturn(savedPenalty);
        
        // When
        PenaltyCalculationResponse response = latePaymentPenaltyService
                .calculateLatePaymentPenalty(request);
        
        // Then: 1 month × 1% × $10,000 = $100
        assertThat(response).isNotNull();
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("100.00");
        assertThat(response.getLatePaymentPenaltyRate()).isEqualByComparingTo("1.00"); // 1%
        
        verify(penaltyRepository).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should calculate 3% penalty for 3 months late")
    void shouldCalculateThreeMonthLatePaymentPenalty() {
        // Given: Payment 3 months late
        LocalDate paymentDate = dueDate.plusMonths(3);
        PenaltyCalculationRequest request = createRequest(paymentDate);
        
        Penalty savedPenalty = createMockPenalty(3, new BigDecimal("300.00"));
        when(penaltyRepository.findByReturnIdAndPenaltyTypeAndTenantId(
                any(UUID.class), eq(PenaltyType.LATE_PAYMENT), any(UUID.class)))
                .thenReturn(Collections.emptyList());
        when(penaltyRepository.save(any(Penalty.class))).thenReturn(savedPenalty);
        
        // When
        PenaltyCalculationResponse response = latePaymentPenaltyService
                .calculateLatePaymentPenalty(request);
        
        // Then: 3 months × 1% × $10,000 = $300
        assertThat(response).isNotNull();
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("300.00");
        assertThat(response.getLatePaymentPenaltyRate()).isEqualByComparingTo("1.00");
        
        verify(penaltyRepository).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should round up partial months")
    void shouldRoundUpPartialMonths() {
        // Given: Payment 1 month and 1 day late (should round to 2 months)
        LocalDate paymentDate = dueDate.plusMonths(1).plusDays(1);
        PenaltyCalculationRequest request = createRequest(paymentDate);
        
        Penalty savedPenalty = createMockPenalty(2, new BigDecimal("200.00"));
        when(penaltyRepository.findByReturnIdAndPenaltyTypeAndTenantId(
                any(UUID.class), eq(PenaltyType.LATE_PAYMENT), any(UUID.class)))
                .thenReturn(Collections.emptyList());
        when(penaltyRepository.save(any(Penalty.class))).thenReturn(savedPenalty);
        
        // When
        PenaltyCalculationResponse response = latePaymentPenaltyService
                .calculateLatePaymentPenalty(request);
        
        // Then: 2 months × 1% × $10,000 = $200
        assertThat(response).isNotNull();
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("200.00");
        
        verify(penaltyRepository).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should cap penalty at 25% (25 months)")
    void shouldCapPenaltyAt25Percent() {
        // Given: Payment 30 months late (exceeds 25 month cap)
        LocalDate paymentDate = dueDate.plusMonths(30);
        PenaltyCalculationRequest request = createRequest(paymentDate);
        
        // Maximum: 25% of $10,000 = $2,500
        Penalty savedPenalty = createMockPenalty(30, new BigDecimal("2500.00"));
        when(penaltyRepository.findByReturnIdAndPenaltyTypeAndTenantId(
                any(UUID.class), eq(PenaltyType.LATE_PAYMENT), any(UUID.class)))
                .thenReturn(Collections.emptyList());
        when(penaltyRepository.save(any(Penalty.class))).thenReturn(savedPenalty);
        
        // When
        PenaltyCalculationResponse response = latePaymentPenaltyService
                .calculateLatePaymentPenalty(request);
        
        // Then: Capped at 25% × $10,000 = $2,500 (not 30% = $3,000)
        assertThat(response).isNotNull();
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("2500.00");
        assertThat(response.getLatePaymentPenaltyExplanation()).contains("capped at 25% maximum");
        
        verify(penaltyRepository).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should calculate penalty on reduced balance after partial payment")
    void shouldRecalculateOnReducedBalance() {
        // Given: Original penalty on $10,000, then partial payment reduces balance to $5,000
        BigDecimal newUnpaidBalance = new BigDecimal("5000.00");
        LocalDate paymentDate = dueDate.plusMonths(3);
        
        Penalty existingPenalty = createMockPenalty(2, new BigDecimal("200.00"));
        when(penaltyRepository.findByReturnIdAndPenaltyTypeAndTenantId(
                any(UUID.class), eq(PenaltyType.LATE_PAYMENT), any(UUID.class)))
                .thenReturn(Collections.singletonList(existingPenalty));
        
        PenaltyCalculationRequest request = createRequest(paymentDate);
        request.setUnpaidTaxAmount(newUnpaidBalance);
        
        Penalty newPenalty = createMockPenalty(3, new BigDecimal("150.00"));
        when(penaltyRepository.save(any(Penalty.class))).thenReturn(newPenalty);
        
        // When: Recalculate on $5,000 balance
        PenaltyCalculationResponse response = latePaymentPenaltyService
                .calculateLatePaymentPenalty(request);
        
        // Then: 3 months × 1% × $5,000 = $150 (not $300)
        assertThat(response).isNotNull();
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("150.00");
        
        verify(penaltyRepository).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should return zero penalty if paid on time")
    void shouldReturnZeroPenaltyIfPaidOnTime() {
        // Given: Payment on due date
        LocalDate paymentDate = dueDate;
        PenaltyCalculationRequest request = createRequest(paymentDate);
        
        Penalty savedPenalty = createMockPenalty(0, BigDecimal.ZERO);
        when(penaltyRepository.findByReturnIdAndPenaltyTypeAndTenantId(
                any(UUID.class), eq(PenaltyType.LATE_PAYMENT), any(UUID.class)))
                .thenReturn(Collections.emptyList());
        when(penaltyRepository.save(any(Penalty.class))).thenReturn(savedPenalty);
        
        // When
        PenaltyCalculationResponse response = latePaymentPenaltyService
                .calculateLatePaymentPenalty(request);
        
        // Then: No penalty
        assertThat(response).isNotNull();
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("0.00");
        
        verify(penaltyRepository).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
        // Given: Request missing tenant ID
        PenaltyCalculationRequest request = PenaltyCalculationRequest.builder()
                .returnId(testReturnId)
                .taxDueDate(dueDate)
                .unpaidTaxAmount(unpaidTax)
                .createdBy(testUserId)
                .build();
        
        // When/Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> latePaymentPenaltyService.calculateLatePaymentPenalty(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant ID is required");
    }
    
    @Test
    @DisplayName("Should reject negative unpaid tax amount")
    void shouldRejectNegativeUnpaidTax() {
        // Given: Negative unpaid tax
        PenaltyCalculationRequest request = createRequest(dueDate.plusMonths(1));
        request.setUnpaidTaxAmount(new BigDecimal("-1000.00"));
        
        // When/Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> latePaymentPenaltyService.calculateLatePaymentPenalty(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Valid unpaid tax amount is required");
    }
    
    // Helper methods
    
    private PenaltyCalculationRequest createRequest(LocalDate actualDate) {
        return PenaltyCalculationRequest.builder()
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .penaltyType("LATE_PAYMENT")
                .taxDueDate(dueDate)
                .actualDate(actualDate)
                .unpaidTaxAmount(unpaidTax)
                .createdBy(testUserId)
                .checkExisting(false)
                .build();
    }
    
    private Penalty createMockPenalty(int monthsLate, BigDecimal penaltyAmount) {
        return Penalty.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .penaltyType(PenaltyType.LATE_PAYMENT)
                .assessmentDate(LocalDate.now())
                .taxDueDate(dueDate)
                .actualDate(dueDate.plusMonths(monthsLate))
                .monthsLate(monthsLate)
                .unpaidTaxAmount(unpaidTax)
                .penaltyRate(latePaymentRate)
                .penaltyAmount(penaltyAmount)
                .maximumPenalty(unpaidTax.multiply(new BigDecimal("0.25")))
                .isAbated(false)
                .createdBy(testUserId)
                .build();
    }
}
