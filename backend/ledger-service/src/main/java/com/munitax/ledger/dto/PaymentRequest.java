package com.munitax.ledger.dto;

import com.munitax.ledger.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * T092: Input validation added to prevent injection attacks and ensure data integrity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    @NotNull(message = "Filer ID is required")
    private UUID filerId;
    
    @NotNull(message = "Tenant ID is required")
    private String tenantId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @DecimalMax(value = "999999999.99", message = "Amount exceeds maximum allowed")
    @Digits(integer = 9, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-.,;:()]*$", message = "Description contains invalid characters")
    private String description;
    
    // Credit Card fields
    @Pattern(regexp = "^[0-9]{4}-?[0-9]{4}-?[0-9]{4}-?[0-9]{4}$", 
             message = "Invalid card number format. Expected: XXXX-XXXX-XXXX-XXXX")
    private String cardNumber;
    
    @Size(max = 100, message = "Cardholder name cannot exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]*$", message = "Cardholder name contains invalid characters")
    private String cardholderName;
    
    @Min(value = 1, message = "Expiration month must be between 1 and 12")
    @Max(value = 12, message = "Expiration month must be between 1 and 12")
    private Integer expirationMonth;
    
    @Min(value = 2024, message = "Expiration year must be current year or later")
    @Max(value = 2099, message = "Expiration year is too far in the future")
    private Integer expirationYear;
    
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3 or 4 digits")
    private String cvv;
    
    // ACH fields
    @Pattern(regexp = "^[0-9]{9}$", message = "ACH routing number must be 9 digits")
    private String achRouting;
    
    @Pattern(regexp = "^[0-9]{4,17}$", message = "ACH account number must be 4-17 digits")
    private String achAccount;
    
    // Check/Wire fields
    @Pattern(regexp = "^[0-9]{1,10}$", message = "Check number must be numeric (max 10 digits)")
    private String checkNumber;
    
    @Size(max = 100, message = "Wire confirmation cannot exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]*$", message = "Wire confirmation contains invalid characters")
    private String wireConfirmation;
    
    // Optional tax return ID for linking
    @Size(max = 50, message = "Tax return ID cannot exceed 50 characters")
    private String taxReturnId;
    
    // Allocation (optional)
    private PaymentAllocation allocation;
}
