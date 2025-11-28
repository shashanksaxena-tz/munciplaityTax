package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.nol.*;
import com.munitax.taxengine.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NOLService.
 * Tests core business logic for NOL tracking and utilization.
 */
@ExtendWith(MockitoExtension.class)
class NOLServiceTest {

    @Mock
    private NOLRepository nolRepository;

    @Mock
    private NOLUsageRepository nolUsageRepository;

    @Mock
    private NOLCarrybackRepository nolCarrybackRepository;

    @Mock
    private NOLScheduleRepository nolScheduleRepository;

    @Mock
    private NOLExpirationAlertRepository nolExpirationAlertRepository;

    @Mock
    private NOLAmendmentRepository nolAmendmentRepository;

    @InjectMocks
    private NOLService nolService;

    private UUID testBusinessId;
    private UUID testUserId;
    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        testBusinessId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create NOL with correct expiration for pre-TCJA year")
    void shouldCreateNOLWithExpirationForPreTCJAYear() {
        // Given
        Integer taxYear = 2015;
        BigDecimal lossAmount = new BigDecimal("200000.00");
        
        NOL savedNOL = createMockNOL(taxYear, lossAmount);
        when(nolRepository.save(any(NOL.class))).thenReturn(savedNOL);

        // When
        NOL result = nolService.createNOL(
            testBusinessId, taxYear, lossAmount,
            Jurisdiction.FEDERAL, EntityType.C_CORP,
            null, null, testUserId, testTenantId
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaxYear()).isEqualTo(2015);
        assertThat(result.getOriginalNOLAmount()).isEqualTo(lossAmount);
        assertThat(result.getCurrentNOLBalance()).isEqualTo(lossAmount);
        assertThat(result.getExpirationDate()).isEqualTo(LocalDate.of(2035, 12, 31)); // 2015 + 20 years
        assertThat(result.getCarryforwardYears()).isEqualTo(20);
        
        verify(nolRepository).save(any(NOL.class));
        verify(nolExpirationAlertRepository).save(any(NOLExpirationAlert.class));
    }

    @Test
    @DisplayName("Should create NOL with no expiration for post-TCJA year")
    void shouldCreateNOLWithNoExpirationForPostTCJAYear() {
        // Given
        Integer taxYear = 2020;
        BigDecimal lossAmount = new BigDecimal("300000.00");
        
        NOL savedNOL = createMockNOL(taxYear, lossAmount);
        savedNOL.setExpirationDate(null);
        savedNOL.setCarryforwardYears(null);
        when(nolRepository.save(any(NOL.class))).thenReturn(savedNOL);

        // When
        NOL result = nolService.createNOL(
            testBusinessId, taxYear, lossAmount,
            Jurisdiction.FEDERAL, EntityType.C_CORP,
            null, null, testUserId, testTenantId
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaxYear()).isEqualTo(2020);
        assertThat(result.getExpirationDate()).isNull(); // Indefinite carryforward
        assertThat(result.getCarryforwardYears()).isNull();
        
        verify(nolRepository).save(any(NOL.class));
        verify(nolExpirationAlertRepository, never()).save(any(NOLExpirationAlert.class));
    }

    @Test
    @DisplayName("Should create state NOL with apportionment")
    void shouldCreateStateNOLWithApportionment() {
        // Given
        Integer taxYear = 2023;
        BigDecimal federalLoss = new BigDecimal("1000000.00");
        BigDecimal apportionmentPct = new BigDecimal("30.00");
        BigDecimal expectedStateNOL = new BigDecimal("300000.00"); // 1M * 30%
        
        NOL savedNOL = createMockNOL(taxYear, expectedStateNOL);
        savedNOL.setJurisdiction(Jurisdiction.STATE_OHIO);
        savedNOL.setApportionmentPercentage(apportionmentPct);
        when(nolRepository.save(any(NOL.class))).thenReturn(savedNOL);

        // When
        NOL result = nolService.createNOL(
            testBusinessId, taxYear, federalLoss,
            Jurisdiction.STATE_OHIO, EntityType.C_CORP,
            apportionmentPct, null, testUserId, testTenantId
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginalNOLAmount()).isEqualTo(expectedStateNOL);
        assertThat(result.getApportionmentPercentage()).isEqualTo(apportionmentPct);
        
        verify(nolRepository).save(any(NOL.class));
    }

    @Test
    @DisplayName("Should throw exception for negative loss amount")
    void shouldThrowExceptionForNegativeLossAmount() {
        // Given
        Integer taxYear = 2023;
        BigDecimal negativeLoss = new BigDecimal("-100000.00");

        // When/Then
        assertThatThrownBy(() -> nolService.createNOL(
            testBusinessId, taxYear, negativeLoss,
            Jurisdiction.FEDERAL, EntityType.C_CORP,
            null, null, testUserId, testTenantId
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Loss amount must be positive");
        
        verify(nolRepository, never()).save(any(NOL.class));
    }

    @Test
    @DisplayName("Should calculate available NOL balance correctly")
    void shouldCalculateAvailableNOLBalance() {
        // Given
        List<NOL> availableNOLs = Arrays.asList(
            createMockNOLWithBalance(2020, new BigDecimal("100000.00")),
            createMockNOLWithBalance(2021, new BigDecimal("150000.00")),
            createMockNOLWithBalance(2022, new BigDecimal("200000.00"))
        );
        
        when(nolRepository.findAvailableNOLsByBusinessId(testBusinessId))
            .thenReturn(availableNOLs);

        // When
        BigDecimal result = nolService.calculateAvailableNOLBalance(testBusinessId, null);

        // Then
        assertThat(result).isEqualByComparingTo(new BigDecimal("450000.00"));
        verify(nolRepository).findAvailableNOLsByBusinessId(testBusinessId);
    }

    @Test
    @DisplayName("Should calculate maximum NOL deduction with 80% limitation for post-TCJA")
    void shouldCalculateMaxNOLDeductionWith80Percent() {
        // Given
        Integer taxYear = 2023;
        BigDecimal taxableIncome = new BigDecimal("300000.00");
        BigDecimal availableNOL = new BigDecimal("500000.00");
        
        // Expected: Min(500K available, 80% * 300K = 240K) = 240K
        BigDecimal expected = new BigDecimal("240000.00");

        // When
        BigDecimal result = nolService.calculateMaximumNOLDeduction(
            taxableIncome, availableNOL, taxYear
        );

        // Then
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("Should calculate maximum NOL deduction with 100% limitation for pre-TCJA")
    void shouldCalculateMaxNOLDeductionWith100Percent() {
        // Given
        Integer taxYear = 2017;
        BigDecimal taxableIncome = new BigDecimal("300000.00");
        BigDecimal availableNOL = new BigDecimal("500000.00");
        
        // Expected: Min(500K available, 100% * 300K = 300K) = 300K
        BigDecimal expected = new BigDecimal("300000.00");

        // When
        BigDecimal result = nolService.calculateMaximumNOLDeduction(
            taxableIncome, availableNOL, taxYear
        );

        // Then
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("Should limit NOL deduction to available balance")
    void shouldLimitNOLDeductionToAvailableBalance() {
        // Given
        Integer taxYear = 2023;
        BigDecimal taxableIncome = new BigDecimal("300000.00");
        BigDecimal availableNOL = new BigDecimal("100000.00");
        
        // Expected: Min(100K available, 80% * 300K = 240K) = 100K
        BigDecimal expected = new BigDecimal("100000.00");

        // When
        BigDecimal result = nolService.calculateMaximumNOLDeduction(
            taxableIncome, availableNOL, taxYear
        );

        // Then
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("Should apply NOL deduction using FIFO ordering")
    void shouldApplyNOLDeductionUsingFIFO() {
        // Given
        UUID returnId = UUID.randomUUID();
        Integer taxYear = 2024;
        BigDecimal taxableIncome = new BigDecimal("300000.00");
        BigDecimal deductionAmount = new BigDecimal("150000.00");
        BigDecimal taxRate = new BigDecimal("2.50");
        
        NOL nol2020 = createMockNOLWithBalance(2020, new BigDecimal("100000.00"));
        NOL nol2021 = createMockNOLWithBalance(2021, new BigDecimal("150000.00"));
        
        List<NOL> availableNOLs = Arrays.asList(nol2020, nol2021);
        
        when(nolRepository.findAvailableNOLsByBusinessId(testBusinessId))
            .thenReturn(availableNOLs);
        when(nolUsageRepository.save(any(NOLUsage.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(nolRepository.save(any(NOL.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<NOLUsage> result = nolService.applyNOLDeduction(
            testBusinessId, returnId, taxYear,
            taxableIncome, deductionAmount, taxRate,
            null, testTenantId
        );

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getActualNOLDeduction()).isEqualByComparingTo(new BigDecimal("100000.00")); // Use all of 2020
        assertThat(result.get(1).getActualNOLDeduction()).isEqualByComparingTo(new BigDecimal("50000.00")); // Use part of 2021
        
        verify(nolUsageRepository, times(2)).save(any(NOLUsage.class));
        verify(nolRepository, times(2)).save(any(NOL.class));
    }

    // Helper methods

    private NOL createMockNOL(Integer taxYear, BigDecimal amount) {
        LocalDate expirationDate = taxYear < 2018 ? 
            LocalDate.of(taxYear + 20, 12, 31) : null;
        Integer carryforwardYears = taxYear < 2018 ? 20 : null;
        
        return NOL.builder()
            .id(UUID.randomUUID())
            .tenantId(testTenantId)
            .businessId(testBusinessId)
            .taxYear(taxYear)
            .jurisdiction(Jurisdiction.FEDERAL)
            .entityType(EntityType.C_CORP)
            .originalNOLAmount(amount)
            .currentNOLBalance(amount)
            .usedAmount(BigDecimal.ZERO)
            .expiredAmount(BigDecimal.ZERO)
            .expirationDate(expirationDate)
            .carryforwardYears(carryforwardYears)
            .isCarriedBack(false)
            .carrybackAmount(BigDecimal.ZERO)
            .carrybackRefund(BigDecimal.ZERO)
            .createdBy(testUserId)
            .build();
    }

    private NOL createMockNOLWithBalance(Integer taxYear, BigDecimal balance) {
        NOL nol = createMockNOL(taxYear, balance);
        nol.setId(UUID.randomUUID());
        return nol;
    }
}
