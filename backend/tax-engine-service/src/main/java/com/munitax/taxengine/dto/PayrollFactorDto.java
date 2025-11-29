package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for payroll factor in apportionment calculation.
 * Represents wages and compensation paid in Ohio vs everywhere.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollFactorDto {

    /**
     * Unique identifier for the payroll factor record.
     */
    private UUID id;

    /**
     * Total payroll (wages, salaries, commissions) paid in Ohio.
     */
    @NotNull(message = "Ohio payroll is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Ohio payroll must be non-negative")
    private BigDecimal ohioPayroll;

    /**
     * Total payroll (wages, salaries, commissions) paid everywhere.
     */
    @NotNull(message = "Total payroll is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total payroll must be non-negative")
    private BigDecimal totalPayroll;

    /**
     * Number of employees based in Ohio.
     */
    @Min(value = 0, message = "Ohio employee count must be non-negative")
    @Builder.Default
    private Integer ohioEmployeeCount = 0;

    /**
     * Total number of employees everywhere.
     */
    @Min(value = 0, message = "Total employee count must be non-negative")
    @Builder.Default
    private Integer totalEmployeeCount = 0;

    /**
     * Number of remote employees whose payroll is allocated to Ohio.
     */
    @Min(value = 0, message = "Remote employee count must be non-negative")
    @Builder.Default
    private Integer remoteEmployeeCount = 0;

    /**
     * Calculated payroll factor percentage (0-100).
     * Formula: Ohio payroll / Total payroll
     */
    private BigDecimal payrollFactorPercentage;

    /**
     * Whether payroll data was auto-populated from W-1 withholding filings.
     */
    @Builder.Default
    private Boolean autoPopulated = false;

    /**
     * Notes about payroll factor calculation or adjustments.
     */
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    // Alias methods for backward compatibility with controller
    public BigDecimal getPayrollInOhio() {
        return ohioPayroll;
    }

    public void setPayrollInOhio(BigDecimal payrollInOhio) {
        this.ohioPayroll = payrollInOhio;
    }

    public BigDecimal getTotalPayrollEverywhere() {
        return totalPayroll;
    }

    public void setTotalPayrollEverywhere(BigDecimal totalPayrollEverywhere) {
        this.totalPayroll = totalPayrollEverywhere;
    }

    public Integer getEmployeesInOhio() {
        return ohioEmployeeCount;
    }

    public void setEmployeesInOhio(Integer employeesInOhio) {
        this.ohioEmployeeCount = employeesInOhio;
    }

    public Integer getTotalEmployeesEverywhere() {
        return totalEmployeeCount;
    }

    public void setTotalEmployeesEverywhere(Integer totalEmployeesEverywhere) {
        this.totalEmployeeCount = totalEmployeesEverywhere;
    }
}
