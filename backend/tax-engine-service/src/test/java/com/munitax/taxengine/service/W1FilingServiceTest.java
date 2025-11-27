package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.withholding.FilingFrequency;
import com.munitax.taxengine.domain.withholding.W1Filing;
import com.munitax.taxengine.domain.withholding.W1FilingStatus;
import com.munitax.taxengine.dto.W1FilingRequest;
import com.munitax.taxengine.dto.W1FilingResponse;
import com.munitax.taxengine.repository.W1FilingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for W1FilingService.
 * Tests core business logic for W-1 filing operations.
 */
@ExtendWith(MockitoExtension.class)
class W1FilingServiceTest {

    @Mock
    private W1FilingRepository w1FilingRepository;

    @InjectMocks
    private W1FilingService w1FilingService;

    private UUID testBusinessId;
    private UUID testUserId;
    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        testBusinessId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
        
        // Set the configurable tax rate for testing
        ReflectionTestUtils.setField(w1FilingService, "municipalTaxRate", new BigDecimal("0.0200"));
    }

    @Test
    @DisplayName("Should file quarterly W-1 with correct tax calculation")
    void shouldFileQuarterlyW1WithCorrectTax() {
        // Given
        W1FilingRequest request = W1FilingRequest.builder()
            .businessId(testBusinessId)
            .taxYear(2024)
            .filingFrequency(FilingFrequency.QUARTERLY)
            .period("Q1")
            .periodStartDate(LocalDate.of(2024, 1, 1))
            .periodEndDate(LocalDate.of(2024, 3, 31))
            .grossWages(new BigDecimal("125000.00"))
            .build();

        W1Filing savedFiling = createMockW1Filing();
        when(w1FilingRepository.existsByBusinessIdAndTaxYearAndPeriodAndIsAmended(
            any(), any(), any(), any())).thenReturn(false);
        when(w1FilingRepository.save(any(W1Filing.class))).thenReturn(savedFiling);

        // When
        W1FilingResponse response = w1FilingService.fileW1Return(request, testUserId, testTenantId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getGrossWages()).isEqualTo(new BigDecimal("125000.00"));
        assertThat(response.getTaxDue()).isEqualTo(new BigDecimal("2500.00")); // 125000 * 0.02
        assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2024, 4, 30)); // 30 days after Q1 end
        assertThat(response.getStatus()).isEqualTo(W1FilingStatus.FILED);
        
        verify(w1FilingRepository, times(1)).save(any(W1Filing.class));
    }

    @Test
    @DisplayName("Should throw exception for duplicate filing")
    void shouldThrowExceptionForDuplicateFiling() {
        // Given
        W1FilingRequest request = W1FilingRequest.builder()
            .businessId(testBusinessId)
            .taxYear(2024)
            .filingFrequency(FilingFrequency.QUARTERLY)
            .period("Q1")
            .periodStartDate(LocalDate.of(2024, 1, 1))
            .periodEndDate(LocalDate.of(2024, 3, 31))
            .grossWages(new BigDecimal("125000.00"))
            .build();

        when(w1FilingRepository.existsByBusinessIdAndTaxYearAndPeriodAndIsAmended(
            testBusinessId, 2024, "Q1", false)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> w1FilingService.fileW1Return(request, testUserId, testTenantId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("W-1 filing already exists");
        
        verify(w1FilingRepository, never()).save(any(W1Filing.class));
    }

    @Test
    @DisplayName("Should calculate late filing penalty correctly")
    void shouldCalculateLateFilingPenaltyCorrectly() {
        // Given - Filing 1 month late
        W1FilingRequest request = W1FilingRequest.builder()
            .businessId(testBusinessId)
            .taxYear(2024)
            .filingFrequency(FilingFrequency.QUARTERLY)
            .period("Q1")
            .periodStartDate(LocalDate.of(2024, 1, 1))
            .periodEndDate(LocalDate.of(2024, 3, 31))
            .grossWages(new BigDecimal("100000.00"))
            .build();

        W1Filing savedFiling = createMockW1Filing();
        savedFiling.setLateFilingPenalty(new BigDecimal("100.00")); // 5% of 2000
        
        when(w1FilingRepository.existsByBusinessIdAndTaxYearAndPeriodAndIsAmended(
            any(), any(), any(), any())).thenReturn(false);
        when(w1FilingRepository.save(any(W1Filing.class))).thenReturn(savedFiling);

        // When
        W1FilingResponse response = w1FilingService.fileW1Return(request, testUserId, testTenantId);

        // Then
        assertThat(response).isNotNull();
        verify(w1FilingRepository, times(1)).save(any(W1Filing.class));
    }

    @Test
    @DisplayName("Should calculate monthly due date as 15th of following month")
    void shouldCalculateMonthlyDueDateCorrectly() {
        // Given
        W1FilingRequest request = W1FilingRequest.builder()
            .businessId(testBusinessId)
            .taxYear(2024)
            .filingFrequency(FilingFrequency.MONTHLY)
            .period("M01")
            .periodStartDate(LocalDate.of(2024, 1, 1))
            .periodEndDate(LocalDate.of(2024, 1, 31))
            .grossWages(new BigDecimal("50000.00"))
            .build();

        W1Filing savedFiling = createMockW1Filing();
        savedFiling.setDueDate(LocalDate.of(2024, 2, 15));
        
        when(w1FilingRepository.existsByBusinessIdAndTaxYearAndPeriodAndIsAmended(
            any(), any(), any(), any())).thenReturn(false);
        when(w1FilingRepository.save(any(W1Filing.class))).thenReturn(savedFiling);

        // When
        W1FilingResponse response = w1FilingService.fileW1Return(request, testUserId, testTenantId);

        // Then
        assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2024, 2, 15));
    }

    @Test
    @DisplayName("Should default taxable wages to gross wages when not provided")
    void shouldDefaultTaxableWagesToGrossWages() {
        // Given
        W1FilingRequest request = W1FilingRequest.builder()
            .businessId(testBusinessId)
            .taxYear(2024)
            .filingFrequency(FilingFrequency.QUARTERLY)
            .period("Q1")
            .periodStartDate(LocalDate.of(2024, 1, 1))
            .periodEndDate(LocalDate.of(2024, 3, 31))
            .grossWages(new BigDecimal("100000.00"))
            .taxableWages(null) // Not provided
            .build();

        W1Filing savedFiling = createMockW1Filing();
        when(w1FilingRepository.existsByBusinessIdAndTaxYearAndPeriodAndIsAmended(
            any(), any(), any(), any())).thenReturn(false);
        when(w1FilingRepository.save(any(W1Filing.class))).thenReturn(savedFiling);

        // When
        W1FilingResponse response = w1FilingService.fileW1Return(request, testUserId, testTenantId);

        // Then
        assertThat(response).isNotNull();
        verify(w1FilingRepository, times(1)).save(argThat(filing -> 
            filing.getTaxableWages().equals(new BigDecimal("100000.00"))
        ));
    }

    private W1Filing createMockW1Filing() {
        return W1Filing.builder()
            .id(UUID.randomUUID())
            .tenantId(testTenantId)
            .businessId(testBusinessId)
            .taxYear(2024)
            .filingFrequency(FilingFrequency.QUARTERLY)
            .period("Q1")
            .periodStartDate(LocalDate.of(2024, 1, 1))
            .periodEndDate(LocalDate.of(2024, 3, 31))
            .dueDate(LocalDate.of(2024, 4, 30))
            .filingDate(LocalDateTime.now())
            .grossWages(new BigDecimal("125000.00"))
            .taxableWages(new BigDecimal("125000.00"))
            .taxRate(new BigDecimal("0.0200"))
            .taxDue(new BigDecimal("2500.00"))
            .adjustments(BigDecimal.ZERO)
            .totalAmountDue(new BigDecimal("2500.00"))
            .isAmended(false)
            .status(W1FilingStatus.FILED)
            .lateFilingPenalty(BigDecimal.ZERO)
            .underpaymentPenalty(BigDecimal.ZERO)
            .createdBy(testUserId)
            .build();
    }
}
