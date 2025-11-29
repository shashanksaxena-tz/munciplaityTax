package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.PayrollFactor;
import com.munitax.taxengine.domain.apportionment.ScheduleY;
import com.munitax.taxengine.repository.PayrollFactorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

/**
 * Service for calculating payroll factor for Schedule Y apportionment.
 * Implements payroll factor calculation with:
 * - Integration with W-1 filing data (WithholdingIntegrationService)
 * - Remote employee allocation by work days/location
 * - Payroll factor percentage = (OH payroll / Total payroll) * 100
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollFactorService {

    private final PayrollFactorRepository payrollFactorRepository;
    private final WithholdingIntegrationService withholdingIntegrationService;

    /**
     * Scale for percentage calculations (4 decimal places).
     */
    private static final int PERCENTAGE_SCALE = 4;

    /**
     * Scale for monetary calculations (2 decimal places).
     */
    private static final int MONEY_SCALE = 2;

    /**
     * Calculate payroll factor percentage from business ID and tax year.
     * Retrieves payroll data from W-1 filings via WithholdingIntegrationService.
     *
     * @param businessId Business ID
     * @param taxYear    Tax year
     * @param tenantId   Tenant ID
     * @return Payroll factor percentage (0-100%)
     */
    public BigDecimal calculatePayrollFactorPercentage(
            UUID businessId,
            Integer taxYear,
            UUID tenantId) {

        log.debug("Calculating payroll factor from W-1 filings: businessId={}, taxYear={}, tenantId={}",
                businessId, taxYear, tenantId);

        // Retrieve payroll from W-1 filings
        Map<String, Object> ohioData = withholdingIntegrationService.getOhioPayrollData(businessId, taxYear, tenantId);
        Map<String, Object> totalData = withholdingIntegrationService.getTotalPayrollData(businessId, taxYear, tenantId);

        BigDecimal ohioPayroll = (BigDecimal) ohioData.getOrDefault("ohioPayroll", BigDecimal.ZERO);
        BigDecimal totalPayroll = (BigDecimal) totalData.getOrDefault("totalPayroll", BigDecimal.ZERO);

        return calculatePayrollFactorPercentage(ohioPayroll, totalPayroll);
    }

    /**
     * Calculate payroll factor percentage from manual input.
     *
     * @param ohioPayroll  Ohio payroll amount
     * @param totalPayroll Total payroll amount (all states)
     * @return Payroll factor percentage (0-100%)
     */
    public BigDecimal calculatePayrollFactorPercentage(
            BigDecimal ohioPayroll,
            BigDecimal totalPayroll) {

        log.debug("Calculating payroll factor: OH payroll={}, Total payroll={}",
                ohioPayroll, totalPayroll);

        // Validate inputs
        validatePayrollValue(ohioPayroll, "Ohio Payroll");
        validatePayrollValue(totalPayroll, "Total Payroll");

        // Handle zero denominator
        if (totalPayroll.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Total payroll is zero, returning 0% factor");
            return BigDecimal.ZERO;
        }

        // Calculate percentage: (Ohio / Total) * 100
        BigDecimal percentage = ohioPayroll
                .multiply(new BigDecimal("100"))
                .divide(totalPayroll, PERCENTAGE_SCALE, RoundingMode.HALF_UP);

        // Validate result is within 0-100%
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "Payroll factor percentage must be between 0 and 100, got: " + percentage);
        }

        log.debug("Calculated payroll factor: {}%", percentage);
        return percentage;
    }

    /**
     * Allocate remote employee payroll based on work days in each location.
     *
     * @param employeeSalary Employee annual salary
     * @param ohioDays       Days worked in Ohio
     * @param totalDays      Total days worked
     * @return Ohio payroll allocation
     */
    public BigDecimal allocateRemoteEmployeePayroll(
            BigDecimal employeeSalary,
            Integer ohioDays,
            Integer totalDays) {

        if (totalDays == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal allocation = employeeSalary
                .multiply(new BigDecimal(ohioDays))
                .divide(new BigDecimal(totalDays), MONEY_SCALE, RoundingMode.HALF_UP);

        log.debug("Remote employee payroll allocation: Salary={}, OH days={}, Total days={}, Allocation={}",
                employeeSalary, ohioDays, totalDays, allocation);

        return allocation;
    }

    /**
     * Prorate payroll for partial year employment.
     *
     * @param annualSalary  Annual salary
     * @param monthsWorked  Months worked during tax year
     * @return Prorated payroll
     */
    public BigDecimal proratePayrollForPartialYear(
            BigDecimal annualSalary,
            Integer monthsWorked) {

        if (monthsWorked < 0 || monthsWorked > 12) {
            throw new IllegalArgumentException("Months worked must be between 0 and 12, got: " + monthsWorked);
        }

        BigDecimal prorated = annualSalary
                .multiply(new BigDecimal(monthsWorked))
                .divide(new BigDecimal("12"), MONEY_SCALE, RoundingMode.HALF_UP);

        log.debug("Prorated payroll: Annual={}, Months={}, Prorated={}",
                annualSalary, monthsWorked, prorated);

        return prorated;
    }

    /**
     * Validate payroll value is non-negative.
     *
     * @param value     Payroll value to validate
     * @param fieldName Name of the field for error message
     */
    public void validatePayrollValue(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be negative, got: " + value);
        }
    }

    /**
     * Save payroll factor to database.
     * Note: Entity calculates totals and percentage automatically via @PrePersist/@PreUpdate.
     *
     * @param payrollFactor PayrollFactor entity
     * @return Saved PayrollFactor
     */
    @Transactional
    public PayrollFactor savePayrollFactor(PayrollFactor payrollFactor) {
        log.info("Saving payroll factor for scheduleYId={}",
                payrollFactor.getScheduleY() != null ? payrollFactor.getScheduleY().getScheduleYId() : "null");

        // Entity automatically calculates totals and percentage via @PrePersist
        return payrollFactorRepository.save(payrollFactor);
    }

    /**
     * Auto-populate payroll factor from W-1 filings.
     * Retrieves payroll data from WithholdingIntegrationService and creates PayrollFactor entity.
     *
     * @param businessId  Business ID
     * @param taxYear     Tax year
     * @param scheduleY   ScheduleY entity
     * @param tenantId    Tenant ID
     * @return PayrollFactor entity with calculated percentage
     */
    public PayrollFactor autoPopulateFromW1Filings(
            UUID businessId,
            Integer taxYear,
            ScheduleY scheduleY,
            UUID tenantId) {

        log.info("Auto-populating payroll factor from W-1 filings: businessId={}, taxYear={}",
                businessId, taxYear);

        // Retrieve payroll from W-1 filings
        Map<String, Object> ohioData = withholdingIntegrationService.getOhioPayrollData(businessId, taxYear, tenantId);
        Map<String, Object> totalData = withholdingIntegrationService.getTotalPayrollData(businessId, taxYear, tenantId);

        BigDecimal ohioPayroll = (BigDecimal) ohioData.getOrDefault("ohioPayroll", BigDecimal.ZERO);
        BigDecimal totalPayroll = (BigDecimal) totalData.getOrDefault("totalPayroll", BigDecimal.ZERO);
        Integer ohioEmployeeCount = (Integer) ohioData.getOrDefault("ohioEmployeeCount", 0);
        Integer totalEmployeeCount = (Integer) totalData.getOrDefault("totalEmployeeCount", 0);

        // Create PayrollFactor entity
        PayrollFactor payrollFactor = new PayrollFactor();
        payrollFactor.setScheduleY(scheduleY);
        payrollFactor.setOhioW2Wages(ohioPayroll);
        payrollFactor.setTotalPayrollEverywhere(totalPayroll);
        payrollFactor.setOhioEmployeeCount(ohioEmployeeCount);
        payrollFactor.setEmployeeCount(totalEmployeeCount);

        // Entity will auto-calculate totals and percentage

        log.info("Auto-populated payroll factor: OH={}, Total={}, OH Employees={}, Total Employees={}",
                ohioPayroll, totalPayroll, ohioEmployeeCount, totalEmployeeCount);

        return payrollFactor;
    }

    /**
     * Get payroll factor by Schedule Y ID.
     *
     * @param scheduleYId Schedule Y ID
     * @return PayrollFactor or null if not found
     */
    public PayrollFactor getPayrollFactorByScheduleYId(UUID scheduleYId) {
        return payrollFactorRepository.findByScheduleYId(scheduleYId).orElse(null);
    }
}
