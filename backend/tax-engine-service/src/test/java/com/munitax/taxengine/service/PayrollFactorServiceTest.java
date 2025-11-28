package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.PayrollFactor;
import com.munitax.taxengine.repository.PayrollFactorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PayrollFactorService.
 * Tests payroll factor calculation with remote employee allocation and W-1 filing integration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PayrollFactorService Tests")
class PayrollFactorServiceTest {

    @Mock
    private PayrollFactorRepository payrollFactorRepository;

    @Mock
    private WithholdingIntegrationService withholdingIntegrationService;

    @InjectMocks
    private PayrollFactorService payrollFactorService;

    private UUID scheduleYId;
    private UUID tenantId;
    private UUID businessId;
    private Integer taxYear;

    @BeforeEach
    void setUp() {
        scheduleYId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        businessId = UUID.randomUUID();
        taxYear = 2024;
    }

    @Test
    @DisplayName("Calculate payroll factor with W-1 filing data")
    void testCalculatePayrollFactorFromW1Filing() {
        // Given: Business has $3M OH payroll, $7M total payroll from W-1 filings
        BigDecimal ohioPayroll = new BigDecimal("3000000");
        BigDecimal totalPayroll = new BigDecimal("7000000");

        when(withholdingIntegrationService.getOhioPayrollForYear(businessId, taxYear, tenantId))
                .thenReturn(ohioPayroll);
        when(withholdingIntegrationService.getTotalPayrollForYear(businessId, taxYear, tenantId))
                .thenReturn(totalPayroll);

        // When: Calculate payroll factor percentage
        BigDecimal payrollFactor = payrollFactorService.calculatePayrollFactorPercentage(
                businessId, taxYear, tenantId);

        // Then: Payroll factor = $3M / $7M = 42.8571%
        assertEquals(new BigDecimal("42.8571"), payrollFactor);
    }

    @Test
    @DisplayName("Calculate payroll factor with manual input (override W-1 data)")
    void testCalculatePayrollFactorManualInput() {
        // Given: Business manually enters payroll amounts
        BigDecimal ohioPayroll = new BigDecimal("2500000");
        BigDecimal totalPayroll = new BigDecimal("10000000");

        // When: Calculate payroll factor percentage
        BigDecimal payrollFactor = payrollFactorService.calculatePayrollFactorPercentage(
                ohioPayroll, totalPayroll);

        // Then: Payroll factor = $2.5M / $10M = 25%
        assertEquals(new BigDecimal("25.0000"), payrollFactor);
    }

    @Test
    @DisplayName("Calculate payroll factor with remote employees (allocated by work days)")
    void testCalculatePayrollFactorWithRemoteEmployees() {
        // Given: Employee with $100K salary works 100 days in OH, 200 days in MI
        BigDecimal employeeSalary = new BigDecimal("100000");
        Integer ohioDays = 100;
        Integer totalDays = 300;

        // When: Calculate OH payroll allocation for remote employee
        BigDecimal ohioAllocation = payrollFactorService.allocateRemoteEmployeePayroll(
                employeeSalary, ohioDays, totalDays);

        // Then: OH allocation = $100K * (100 / 300) = $33,333.33
        assertEquals(new BigDecimal("33333.33"), ohioAllocation);
    }

    @Test
    @DisplayName("Handle zero payroll (no employees in OH)")
    void testHandleZeroPayroll() {
        // Given: Business has no employees in OH
        BigDecimal ohioPayroll = BigDecimal.ZERO;
        BigDecimal totalPayroll = new BigDecimal("5000000");

        // When: Calculate payroll factor
        BigDecimal payrollFactor = payrollFactorService.calculatePayrollFactorPercentage(
                ohioPayroll, totalPayroll);

        // Then: Payroll factor = 0%
        assertEquals(BigDecimal.ZERO, payrollFactor);
    }

    @Test
    @DisplayName("Handle zero total payroll (no employees)")
    void testHandleZeroTotalPayroll() {
        // Given: Business has no employees
        BigDecimal ohioPayroll = BigDecimal.ZERO;
        BigDecimal totalPayroll = BigDecimal.ZERO;

        // When: Calculate payroll factor
        BigDecimal payrollFactor = payrollFactorService.calculatePayrollFactorPercentage(
                ohioPayroll, totalPayroll);

        // Then: Should return 0% (not throw exception)
        assertEquals(BigDecimal.ZERO, payrollFactor);
    }

    @Test
    @DisplayName("Validate payroll values are non-negative")
    void testValidateNonNegativePayrollValues() {
        // Given: Negative payroll value (invalid)
        BigDecimal negativePayroll = new BigDecimal("-5000");

        // When/Then: Should throw validation exception
        assertThrows(IllegalArgumentException.class, () -> {
            payrollFactorService.validatePayrollValue(negativePayroll, "Ohio Payroll");
        });
    }

    @Test
    @DisplayName("Round payroll factor to 4 decimal places")
    void testRoundPayrollFactorTo4Decimals() {
        // Given: Payroll values that result in repeating decimal
        BigDecimal ohioPayroll = new BigDecimal("1000000");
        BigDecimal totalPayroll = new BigDecimal("3000000");

        // When: Calculate payroll factor
        BigDecimal payrollFactor = payrollFactorService.calculatePayrollFactorPercentage(
                ohioPayroll, totalPayroll);

        // Then: Should be 33.3333% (not 33.333333...)
        assertEquals(new BigDecimal("33.3333"), payrollFactor);
        assertEquals(4, payrollFactor.scale());
    }

    @Test
    @DisplayName("Save payroll factor to database")
    void testSavePayrollFactor() {
        // Given: PayrollFactor entity
        PayrollFactor payrollFactor = new PayrollFactor();
        payrollFactor.setScheduleYId(scheduleYId);
        payrollFactor.setTenantId(tenantId);
        payrollFactor.setOhioPayroll(new BigDecimal("3000000"));
        payrollFactor.setTotalPayroll(new BigDecimal("7000000"));
        payrollFactor.setPayrollFactorPercentage(new BigDecimal("42.8571"));

        when(payrollFactorRepository.save(any(PayrollFactor.class)))
                .thenReturn(payrollFactor);

        // When: Save payroll factor
        PayrollFactor saved = payrollFactorService.savePayrollFactor(payrollFactor);

        // Then: Should save and return entity
        assertNotNull(saved);
        assertEquals(new BigDecimal("42.8571"), saved.getPayrollFactorPercentage());
        verify(payrollFactorRepository, times(1)).save(any(PayrollFactor.class));
    }

    @Test
    @DisplayName("Calculate payroll factor exceeding 100% throws exception")
    void testPayrollFactorExceeding100Percent() {
        // Given: Ohio payroll exceeds total (data error)
        BigDecimal ohioPayroll = new BigDecimal("10000000");
        BigDecimal totalPayroll = new BigDecimal("5000000");

        // When/Then: Should throw validation exception
        assertThrows(IllegalArgumentException.class, () -> {
            payrollFactorService.calculatePayrollFactorPercentage(
                    ohioPayroll, totalPayroll);
        });
    }

    @Test
    @DisplayName("Integrate with WithholdingIntegrationService for automatic payroll population")
    void testIntegrationWithWithholdingService() {
        // Given: W-1 filings exist for business
        BigDecimal ohioPayroll = new BigDecimal("2000000");
        BigDecimal totalPayroll = new BigDecimal("8000000");

        when(withholdingIntegrationService.getOhioPayrollForYear(businessId, taxYear, tenantId))
                .thenReturn(ohioPayroll);
        when(withholdingIntegrationService.getTotalPayrollForYear(businessId, taxYear, tenantId))
                .thenReturn(totalPayroll);

        // When: Auto-populate payroll from W-1 filings
        PayrollFactor payrollFactor = payrollFactorService.autoPopulateFromW1Filings(
                businessId, taxYear, scheduleYId, tenantId);

        // Then: Payroll factor should be calculated from W-1 data
        assertNotNull(payrollFactor);
        assertEquals(new BigDecimal("2000000"), payrollFactor.getOhioPayroll());
        assertEquals(new BigDecimal("8000000"), payrollFactor.getTotalPayroll());
        assertEquals(new BigDecimal("25.0000"), payrollFactor.getPayrollFactorPercentage());
        
        verify(withholdingIntegrationService).getOhioPayrollForYear(businessId, taxYear, tenantId);
        verify(withholdingIntegrationService).getTotalPayrollForYear(businessId, taxYear, tenantId);
    }

    @Test
    @DisplayName("Calculate payroll factor with multiple state allocations")
    void testCalculatePayrollFactorMultipleStates() {
        // Given: Business operates in multiple states
        Map<String, BigDecimal> statePayrolls = new HashMap<>();
        statePayrolls.put("OH", new BigDecimal("2000000"));
        statePayrolls.put("MI", new BigDecimal("1500000"));
        statePayrolls.put("PA", new BigDecimal("1000000"));
        statePayrolls.put("NY", new BigDecimal("500000"));

        BigDecimal ohioPayroll = statePayrolls.get("OH");
        BigDecimal totalPayroll = statePayrolls.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // When: Calculate OH payroll factor
        BigDecimal payrollFactor = payrollFactorService.calculatePayrollFactorPercentage(
                ohioPayroll, totalPayroll);

        // Then: OH factor = $2M / $5M = 40%
        assertEquals(new BigDecimal("40.0000"), payrollFactor);
    }

    @Test
    @DisplayName("Handle partial year employment (prorate payroll)")
    void testHandlePartialYearEmployment() {
        // Given: Employee worked 6 months, annual salary $120K
        BigDecimal annualSalary = new BigDecimal("120000");
        Integer monthsWorked = 6;

        // When: Calculate prorated payroll
        BigDecimal proratedPayroll = payrollFactorService.proratePayrollForPartialYear(
                annualSalary, monthsWorked);

        // Then: Prorated = $120K * (6 / 12) = $60K
        assertEquals(new BigDecimal("60000.00"), proratedPayroll);
    }

    @Test
    @DisplayName("Calculate employee allocation based on work location percentage")
    void testCalculateEmployeeAllocationByWorkLocation() {
        // Given: Employee splits time: 60% OH, 40% MI
        BigDecimal employeeSalary = new BigDecimal("150000");
        BigDecimal ohioPercentage = new BigDecimal("0.60");

        // When: Calculate OH allocation
        BigDecimal ohioAllocation = employeeSalary.multiply(ohioPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        // Then: OH allocation = $150K * 0.60 = $90K
        assertEquals(new BigDecimal("90000.00"), ohioAllocation);
    }
}
