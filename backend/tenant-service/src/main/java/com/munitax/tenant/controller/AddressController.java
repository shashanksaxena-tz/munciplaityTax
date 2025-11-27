package com.munitax.tenant.controller;

import com.munitax.tenant.model.Address;
import com.munitax.tenant.service.AddressValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/address")
public class AddressController {

    private final AddressValidationService addressValidationService;

    public AddressController(AddressValidationService addressValidationService) {
        this.addressValidationService = addressValidationService;
    }

    @PostMapping("/validate")
    public ResponseEntity<AddressValidationService.AddressValidationResult> validateAddress(@RequestBody Address address) {
        AddressValidationService.AddressValidationResult result = addressValidationService.validateAddress(address);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/is-dublin")
    public ResponseEntity<Boolean> isDublinAddress(@RequestBody Address address) {
        boolean isDublin = addressValidationService.isDublinAddress(address);
        return ResponseEntity.ok(isDublin);
    }
}
