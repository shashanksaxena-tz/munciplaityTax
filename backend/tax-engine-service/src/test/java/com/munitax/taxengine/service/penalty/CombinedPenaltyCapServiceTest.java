package com.munitax.taxengine.service.penalty;

import com.munitax.taxengine.domain.penalty.Penalty;
import com.munitax.taxengine.domain.penalty.PenaltyType;
import com.munitax.taxengine.dto.PenaltyCalculationRequest;
import com.munitax.taxengine.dto.PenaltyCalculationResponse;
import com.munitax.taxengine.repository.PenaltyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CombinedPenaltyCapService.
 * 
 * Tests:
 * - Combined 5% per month cap when both penalties apply
 * - Months 1-5: Late filing absorbs both penalties
 * - After month 5: Late filing capped at 25%, late payment continues at 1%
 * - Maximum 50% combined penalty cap
 */
@ExtendWith(MockitoExtension.class)
class CombinedPenaltyCapServiceTest {
    
    @Mock
    private PenaltyRepository penaltyRepository;
    
    @Mock
    private LateFilingPenaltyService lateFilingPenaltyService;
    
    @Mock
    private LatePaymentPenaltyService latePaymentPenaltyService;
    
    @InjectMocks
    private CombinedPenaltyCapService combinedPenaltyCapService;
    
    private UUID testTenantId;
    private UUID testReturnId;
    private UUID testUserId;
    private LocalDate dueDate;
    private BigDecimal unpaidTax;
    
    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        testReturnId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        dueDate = LocalDate.of(2024, 4, 15);
        unpaidTax = new BigDecimal("10000.00");
    }
    
    @Test
    @DisplayName("Should apply 5% combined cap for month 1 (late filing absorbs both)")
    void shouldApplyCombinedCapForMonth1() {
        // Given: 1 month late for both filing and payment
        Penalty filingPenalty = createPenalty(PenaltyType.LATE_FILING, 1, new BigDecimal("500.00"));
        Penalty paymentPenalty = createPenalty(PenaltyType.LATE_PAYMENT, 1, new BigDecimal("100.00"));
        
        setupMocksForCombinedCalculation(filingPenalty, paymentPenalty);
        
        PenaltyCalculationRequest request = createRequest(dueDate.plusMonths(1));
        
        // When
        PenaltyCalculationResponse response = combinedPenaltyCapService
                .calculateCombinedPenalties(request);
        
        // Then: Filing penalty $500 (5%), payment penalty $0 (absorbed)
        assertThat(response).isNotNull();
        assertThat(response.getLateFilingPenalty()).isEqualByComparingTo("500.00");
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalPenalties()).isEqualByComparingTo("500.00");
        assertThat(response.getCombinedCapApplied()).isTrue();
        
        verify(penaltyRepository, times(2)).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should apply 5% combined cap for months 2-5")
    void shouldApplyCombinedCapForMonths2To5() {
        // Given: 3 months late for both filing and payment
        Penalty filingPenalty = createPenalty(PenaltyType.LATE_FILING, 3, new BigDecimal("1500.00"));
        Penalty paymentPenalty = createPenalty(PenaltyType.LATE_PAYMENT, 3, new BigDecimal("300.00"));
        
        setupMocksForCombinedCalculation(filingPenalty, paymentPenalty);
        
        PenaltyCalculationRequest request = createRequest(dueDate.plusMonths(3));
        
        // When
        PenaltyCalculationResponse response = combinedPenaltyCapService
                .calculateCombinedPenalties(request);
        
        // Then: 3 months × 5% = 15% filing penalty, $0 payment penalty
        assertThat(response).isNotNull();
        assertThat(response.getLateFilingPenalty()).isEqualByComparingTo("1500.00");
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalPenalties()).isEqualByComparingTo("1500.00");
        
        verify(penaltyRepository, times(2)).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should cap filing at 25% at month 5")
    void shouldCapFilingAt25PercentAtMonth5() {
        // Given: 5 months late for both filing and payment
        Penalty filingPenalty = createPenalty(PenaltyType.LATE_FILING, 5, new BigDecimal("2500.00"));
        Penalty paymentPenalty = createPenalty(PenaltyType.LATE_PAYMENT, 5, new BigDecimal("500.00"));
        
        setupMocksForCombinedCalculation(filingPenalty, paymentPenalty);
        
        PenaltyCalculationRequest request = createRequest(dueDate.plusMonths(5));
        
        // When
        PenaltyCalculationResponse response = combinedPenaltyCapService
                .calculateCombinedPenalties(request);
        
        // Then: 5 months × 5% = 25% filing penalty (max), $0 payment penalty
        assertThat(response).isNotNull();
        assertThat(response.getLateFilingPenalty()).isEqualByComparingTo("2500.00");
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalPenalties()).isEqualByComparingTo("2500.00");
        
        verify(penaltyRepository, times(2)).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should allow late payment penalty after month 5")
    void shouldAllowLatePaymentPenaltyAfterMonth5() {
        // Given: 8 months late for both filing and payment
        // Filing: 5 months × 5% = 25% = $2,500 (maxed)
        // Payment: (8-5=3) months × 1% = 3% = $300
        Penalty filingPenalty = createPenalty(PenaltyType.LATE_FILING, 8, new BigDecimal("4000.00"));
        Penalty paymentPenalty = createPenalty(PenaltyType.LATE_PAYMENT, 8, new BigDecimal("800.00"));
        
        setupMocksForCombinedCalculation(filingPenalty, paymentPenalty);
        
        PenaltyCalculationRequest request = createRequest(dueDate.plusMonths(8));
        
        // When
        PenaltyCalculationResponse response = combinedPenaltyCapService
                .calculateCombinedPenalties(request);
        
        // Then: Filing capped at $2,500 (25%), payment continues for 3 months = $300
        assertThat(response).isNotNull();
        assertThat(response.getLateFilingPenalty()).isEqualByComparingTo("2500.00");
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("300.00");
        assertThat(response.getTotalPenalties()).isEqualByComparingTo("2800.00");
        
        verify(penaltyRepository, times(2)).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should allow late payment penalty to reach its own 25% cap")
    void shouldAllowLatePaymentToReachOwnCap() {
        // Given: 30 months late for both filing and payment
        // Filing: 5 months × 5% = 25% = $2,500 (maxed at month 5)
        // Payment: (30-5=25) months × 1% = 25% = $2,500 (maxed at 25 months)
        Penalty filingPenalty = createPenalty(PenaltyType.LATE_FILING, 30, new BigDecimal("1500.00"));
        Penalty paymentPenalty = createPenalty(PenaltyType.LATE_PAYMENT, 30, new BigDecimal("3000.00"));
        
        setupMocksForCombinedCalculation(filingPenalty, paymentPenalty);
        
        PenaltyCalculationRequest request = createRequest(dueDate.plusMonths(30));
        
        // When
        PenaltyCalculationResponse response = combinedPenaltyCapService
                .calculateCombinedPenalties(request);
        
        // Then: Both capped - filing at $2,500, payment at $2,500
        assertThat(response).isNotNull();
        assertThat(response.getLateFilingPenalty()).isEqualByComparingTo("2500.00");
        assertThat(response.getLatePaymentPenalty()).isEqualByComparingTo("2500.00");
        assertThat(response.getTotalPenalties()).isEqualByComparingTo("5000.00");
        
        verify(penaltyRepository, times(2)).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should enforce 50% absolute maximum combined penalty")
    void shouldEnforce50PercentAbsoluteMaximum() {
        // Given: Scenario that would exceed 50% if not capped
        // Edge case: very long delay
        Penalty filingPenalty = createPenalty(PenaltyType.LATE_FILING, 50, new BigDecimal("2500.00"));
        Penalty paymentPenalty = createPenalty(PenaltyType.LATE_PAYMENT, 50, new BigDecimal("4500.00"));
        
        setupMocksForCombinedCalculation(filingPenalty, paymentPenalty);
        
        PenaltyCalculationRequest request = createRequest(dueDate.plusMonths(50));
        
        // When
        PenaltyCalculationResponse response = combinedPenaltyCapService
                .calculateCombinedPenalties(request);
        
        // Then: Total capped at 50% of $10,000 = $5,000
        assertThat(response).isNotNull();
        assertThat(response.getTotalPenalties()).isLessThanOrEqualTo(new BigDecimal("5000.00"));
        
        verify(penaltyRepository, times(2)).save(any(Penalty.class));
    }
    
    @Test
    @DisplayName("Should check if combined cap applies when both penalties exist")
    void shouldCheckIfCombinedCapApplies() {
        // Given: Both penalties exist
        List<Penalty> penalties = Arrays.asList(
                createPenalty(PenaltyType.LATE_FILING, 3, new BigDecimal("1500.00")),
                createPenalty(PenaltyType.LATE_PAYMENT, 3, new BigDecimal("300.00"))
        );
        
        when(penaltyRepository.findActiveByReturnIdAndTenantId(any(UUID.class), any(UUID.class)))
                .thenReturn(penalties);
        
        // When
        boolean shouldApply = combinedPenaltyCapService.shouldApplyCombinedCap(
                testReturnId.toString(), testTenantId.toString());
        
        // Then
        assertThat(shouldApply).isTrue();
    }
    
    @Test
    @DisplayName("Should not apply combined cap when only one penalty exists")
    void shouldNotApplyCombinedCapWhenOnlyOnePenalty() {
        // Given: Only filing penalty exists
        List<Penalty> penalties = Arrays.asList(
                createPenalty(PenaltyType.LATE_FILING, 3, new BigDecimal("1500.00"))
        );
        
        when(penaltyRepository.findActiveByReturnIdAndTenantId(any(UUID.class), any(UUID.class)))
                .thenReturn(penalties);
        
        // When
        boolean shouldApply = combinedPenaltyCapService.shouldApplyCombinedCap(
                testReturnId.toString(), testTenantId.toString());
        
        // Then
        assertThat(shouldApply).isFalse();
    }
    
    @Test
    @DisplayName("Should get combined penalty summary")
    void shouldGetCombinedPenaltySummary() {
        // Given: Both penalties exist
        List<Penalty> penalties = Arrays.asList(
                createPenalty(PenaltyType.LATE_FILING, 3, new BigDecimal("1500.00")),
                createPenalty(PenaltyType.LATE_PAYMENT, 3, new BigDecimal("0.00"))
        );
        
        when(penaltyRepository.findActiveByReturnIdAndTenantId(any(UUID.class), any(UUID.class)))
                .thenReturn(penalties);
        
        // When
        PenaltyCalculationResponse response = combinedPenaltyCapService.getCombinedPenaltySummary(
                testReturnId.toString(), testTenantId.toString());
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCombinedCapApplied()).isTrue();
        assertThat(response.getTotalPenalties()).isEqualByComparingTo("1500.00");
    }
    
    // Helper methods
    
    private PenaltyCalculationRequest createRequest(LocalDate actualDate) {
        return PenaltyCalculationRequest.builder()
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .taxDueDate(dueDate)
                .actualDate(actualDate)
                .unpaidTaxAmount(unpaidTax)
                .createdBy(testUserId)
                .applyCombinedCap(true)
                .build();
    }
    
    private Penalty createPenalty(PenaltyType type, int monthsLate, BigDecimal penaltyAmount) {
        Penalty penalty = Penalty.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .returnId(testReturnId)
                .penaltyType(type)
                .assessmentDate(LocalDate.now())
                .taxDueDate(dueDate)
                .actualDate(dueDate.plusMonths(monthsLate))
                .monthsLate(monthsLate)
                .unpaidTaxAmount(unpaidTax)
                .penaltyRate(type == PenaltyType.LATE_FILING ? 
                        new BigDecimal("0.05") : new BigDecimal("0.01"))
                .penaltyAmount(penaltyAmount)
                .maximumPenalty(unpaidTax.multiply(new BigDecimal("0.25")))
                .isAbated(false)
                .createdBy(testUserId)
                .build();
        return penalty;
    }
    
    private void setupMocksForCombinedCalculation(Penalty filingPenalty, Penalty paymentPenalty) {
        // Mock individual penalty calculations
        PenaltyCalculationResponse filingResponse = PenaltyCalculationResponse.builder()
                .lateFilingPenalty(filingPenalty.getPenaltyAmount())
                .build();
        
        PenaltyCalculationResponse paymentResponse = PenaltyCalculationResponse.builder()
                .latePaymentPenalty(paymentPenalty.getPenaltyAmount())
                .build();
        
        when(lateFilingPenaltyService.calculateLateFilingPenalty(any()))
                .thenReturn(filingResponse);
        when(latePaymentPenaltyService.calculateLatePaymentPenalty(any()))
                .thenReturn(paymentResponse);
        
        // Mock repository calls
        when(penaltyRepository.findByReturnIdAndTenantId(any(UUID.class), any(UUID.class)))
                .thenReturn(Arrays.asList(filingPenalty, paymentPenalty));
        
        when(penaltyRepository.save(any(Penalty.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
