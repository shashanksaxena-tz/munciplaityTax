package com.munitax.tenant.service;

import com.munitax.tenant.model.Address;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AddressValidationService {

    private static final List<String> VALID_DUBLIN_ZIPS = Arrays.asList(
            "43016", "43017", "43065"
    );

    private static final List<String> VALID_OHIO_CITIES = Arrays.asList(
            "dublin", "columbus", "cleveland", "cincinnati", "toledo",
            "akron", "dayton", "westerville", "hilliard", "upper arlington",
            "grandview", "bexley", "worthington", "powell"
    );

    public AddressValidationResult validateAddress(Address address) {
        if (address == null) {
            return new AddressValidationResult(false, "Address is required", AddressVerificationStatus.INVALID);
        }

        // Validate street
        if (address.getStreet() == null || address.getStreet().trim().isEmpty()) {
            return new AddressValidationResult(false, "Street address is required", AddressVerificationStatus.INVALID);
        }

        // Validate city
        if (address.getCity() == null || address.getCity().trim().isEmpty()) {
            return new AddressValidationResult(false, "City is required", AddressVerificationStatus.INVALID);
        }

        // Validate state
        if (address.getState() == null || !address.getState().equalsIgnoreCase("OH")) {
            return new AddressValidationResult(false, "State must be OH (Ohio)", AddressVerificationStatus.INVALID);
        }

        // Validate ZIP
        if (address.getZip() == null || address.getZip().trim().isEmpty()) {
            return new AddressValidationResult(false, "ZIP code is required", AddressVerificationStatus.INVALID);
        }

        // Validate ZIP format (5 digits or 5+4)
        if (!address.getZip().matches("^\\d{5}(-\\d{4})?$")) {
            return new AddressValidationResult(false, "Invalid ZIP code format", AddressVerificationStatus.INVALID);
        }

        // Check if it's a Dublin address
        String city = address.getCity().toLowerCase().trim();
        String zip = address.getZip().substring(0, 5); // Get first 5 digits

        if (city.equals("dublin")) {
            if (!VALID_DUBLIN_ZIPS.contains(zip)) {
                return new AddressValidationResult(
                        false,
                        "ZIP code does not match Dublin, OH. Valid Dublin ZIPs: 43016, 43017, 43065",
                        AddressVerificationStatus.MISMATCH
                );
            }
            return new AddressValidationResult(true, "Valid Dublin address", AddressVerificationStatus.VERIFIED);
        }

        // Check if it's a valid Ohio city
        if (!VALID_OHIO_CITIES.contains(city)) {
            return new AddressValidationResult(
                    true,
                    "City not in known list but format is valid",
                    AddressVerificationStatus.UNVERIFIED
            );
        }

        return new AddressValidationResult(true, "Valid Ohio address", AddressVerificationStatus.VERIFIED);
    }

    public boolean isDublinAddress(Address address) {
        if (address == null || address.getCity() == null) {
            return false;
        }
        String city = address.getCity().toLowerCase().trim();
        return city.equals("dublin");
    }

    public static class AddressValidationResult {
        private boolean valid;
        private String message;
        private AddressVerificationStatus status;

        public AddressValidationResult(boolean valid, String message, AddressVerificationStatus status) {
            this.valid = valid;
            this.message = message;
            this.status = status;
        }

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public AddressVerificationStatus getStatus() { return status; }
        public void setStatus(AddressVerificationStatus status) { this.status = status; }
    }

    public enum AddressVerificationStatus {
        VERIFIED,      // Address verified as valid
        UNVERIFIED,    // Format valid but not in known list
        MISMATCH,      // City/ZIP mismatch
        INVALID        // Invalid format or missing data
    }
}
