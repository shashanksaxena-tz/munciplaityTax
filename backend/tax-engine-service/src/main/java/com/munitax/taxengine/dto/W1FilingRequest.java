package com.munitax.taxengine.dto;

import com.munitax.taxengine.domain.withholding.FilingFrequency;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for filing a new W-1 withholding return.
 * 
 * Validation Rules (aligned with FR-001, FR-013):
 * - businessId: Required, must be valid UUID
 * - taxYear: Required, >= 2020
 * - filingFrequency: Required, one of DAILY, SEMI_MONTHLY, MONTHLY, QUARTERLY
 * - period: Required, format varies by frequency (Q1-Q4, M01-M12, D20240115, SM01-SM24)
 * - periodStartDate: Required, <= periodEndDate
 * - periodEndDate: Required, >= periodStartDate
 * - grossWages: Required, >= 0
 * - taxableWages: Optional (defaults to grossWages), >= 0
 * - adjustments: Optional (defaults to 0), can be negative
 * - employeeCount: Optional, >= 0
 * 
 * Business Rules:
 * - taxRate provided by system based on municipality (2.0% for Dublin)
 * - taxDue calculated as taxableWages × taxRate
 * - dueDate calculated by system based on filingFrequency and periodEndDate (Research R5)
 * 
 * @see com.munitax.taxengine.domain.withholding.W1Filing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class W1FilingRequest {
    
    /**
     * Business profile ID filing this W-1.
     * Must exist in businesses table.
     */
    @NotNull(message = "Business ID is required")
    private UUID businessId;
    
    /**
     * Tax year for this filing (e.g., 2024).
     */
    @NotNull(message = "Tax year is required")
    @Min(value = 2020, message = "Tax year must be 2020 or later")
    @Max(value = 2099, message = "Tax year must be valid")
    private Integer taxYear;
    
    /**
     * Filing frequency for this business.
     */
    @NotNull(message = "Filing frequency is required")
    private FilingFrequency filingFrequency;
    
    /**
     * Period identifier.
     * Format depends on filingFrequency:
     * - QUARTERLY: Q1, Q2, Q3, Q4
     * - MONTHLY: M01, M02, ..., M12
     * - SEMI_MONTHLY: SM01, SM02, ..., SM24
     * - DAILY: D20240115 (DYYYYMMDD format)
     */
    @NotBlank(message = "Period is required")
    @Pattern(regexp = "^(Q[1-4]|M(0[1-9]|1[0-2])|SM(0[1-9]|1[0-9]|2[0-4])|D\\d{8})$", 
             message = "Period format invalid. Expected: Q1-Q4, M01-M12, SM01-SM24, or DYYYYMMDD")
    private String period;
    
    /**
     * First day of filing period.
     */
    @NotNull(message = "Period start date is required")
    private LocalDate periodStartDate;
    
    /**
     * Last day of filing period.
     */
    @NotNull(message = "Period end date is required")
    private LocalDate periodEndDate;
    
    /**
     * Total gross wages paid to all employees during period.
     */
    @NotNull(message = "Gross wages is required")
    @DecimalMin(value = "0.00", message = "Gross wages must be non-negative")
    @Digits(integer = 15, fraction = 2, message = "Gross wages must have at most 2 decimal places")
    private BigDecimal grossWages;
    
    /**
     * Taxable wages after deductions.
     * Typically equal to grossWages for municipal withholding.
     * If not provided, defaults to grossWages.
     */
    @DecimalMin(value = "0.00", message = "Taxable wages must be non-negative")
    @Digits(integer = 15, fraction = 2, message = "Taxable wages must have at most 2 decimal places")
    private BigDecimal taxableWages;
    
    /**
     * Manual adjustments (prior overpayments, credits).
     * Can be negative. Defaults to 0 if not provided.
     */
    @Digits(integer = 15, fraction = 2, message = "Adjustments must have at most 2 decimal places")
    private BigDecimal adjustments;
    
    /**
     * Number of employees paid during this period.
     * Optional but recommended for year-end employee count validation (FR-018).
     */
    @Min(value = 0, message = "Employee count must be non-negative")
    private Integer employeeCount;
    
    /**
     * Custom validator: periodEndDate must be >= periodStartDate.
     */
    @AssertTrue(message = "Period end date must be on or after period start date")
    public boolean isPeriodDateRangeValid() {
        if (periodStartDate == null || periodEndDate == null) {
            return false; // Both dates required for valid range
        }
        return !periodEndDate.isBefore(periodStartDate);
    }
    
    /**
     * Custom validator: For daily periods (DYYYYMMDD), ensure date portion is valid.
     */
    @AssertTrue(message = "For daily periods, the date portion (YYYYMMDD) must be a valid date")
    public boolean isPeriodDateValid() {
        if (period == null || period.isBlank()) {
            return true; // Not this method's responsibility
        }
        if (period.startsWith("D") && period.length() == 9) {
            String datePart = period.substring(1);
            try {
                int year = Integer.parseInt(datePart.substring(0, 4));
                int month = Integer.parseInt(datePart.substring(4, 6));
                int day = Integer.parseInt(datePart.substring(6, 8));
                java.time.LocalDate.of(year, month, day);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return true; // Not a daily period, so valid
    }
}
