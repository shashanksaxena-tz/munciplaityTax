package com.munitax.auth.controller;

import com.munitax.auth.model.User;
import com.munitax.auth.model.UserProfile;
import com.munitax.auth.service.UserManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationRequest request) {
        try {
            UserManagementService.RegistrationRequest serviceRequest = new UserManagementService.RegistrationRequest(
                    request.email(),
                    request.password(),
                    request.firstName(),
                    request.lastName(),
                    request.phoneNumber(),
                    request.userRole(),
                    request.profileType(),
                    request.ssnOrEin(),
                    request.businessName(),
                    request.fiscalYearEnd(),
                    request.address(),
                    request.tenantId());

            UserManagementService.RegistrationResult result = userManagementService.registerUser(serviceRequest);

            if (result.success()) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new RegistrationResponse(true, result.message(), result.userId()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new RegistrationResponse(false, result.message(), null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RegistrationResponse(false, "Registration failed: " + e.getMessage(), null));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        boolean verified = userManagementService.verifyEmail(token);

        if (verified) {
            return ResponseEntity.ok(new MessageResponse(true, "Email verified successfully. You can now log in."));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(false, "Invalid or expired verification token."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userManagementService.initiatePasswordReset(request.email());
        // Always return success to prevent email enumeration
        return ResponseEntity
                .ok(new MessageResponse(true, "If the email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        boolean reset = userManagementService.resetPassword(request.token(), request.newPassword());

        if (reset) {
            return ResponseEntity.ok(new MessageResponse(true, "Password reset successfully. You can now log in."));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(false, "Invalid or expired reset token."));
        }
    }

    // Profile Management
    @GetMapping("/profiles")
    public ResponseEntity<List<UserProfile>> getProfiles(Authentication authentication) {
        String userId = authentication.getName(); // Assumes userId is the principal
        List<UserProfile> profiles = userManagementService.getUserProfiles(userId);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/profiles/primary")
    public ResponseEntity<UserProfile> getPrimaryProfile(Authentication authentication) {
        String userId = authentication.getName();
        return userManagementService.getPrimaryProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/profiles")
    public ResponseEntity<UserProfile> createProfile(
            Authentication authentication,
            @RequestBody ProfileCreationRequest request) {
        String userId = authentication.getName();

        UserManagementService.ProfileCreationRequest serviceRequest = new UserManagementService.ProfileCreationRequest(
                request.type(),
                request.name(),
                request.ssnOrEin(),
                request.businessName(),
                request.fiscalYearEnd(),
                request.address(),
                request.relationshipToUser());

        UserProfile profile = userManagementService.createProfile(userId, serviceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @PutMapping("/profiles/{profileId}")
    public ResponseEntity<UserProfile> updateProfile(
            Authentication authentication,
            @PathVariable String profileId,
            @RequestBody ProfileUpdateRequest request) {
        String userId = authentication.getName();

        UserManagementService.ProfileUpdateRequest serviceRequest = new UserManagementService.ProfileUpdateRequest(
                request.name(),
                request.ssnOrEin(),
                request.businessName(),
                request.fiscalYearEnd(),
                request.address());

        try {
            UserProfile profile = userManagementService.updateProfile(userId, profileId, serviceRequest);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/profiles/{profileId}")
    public ResponseEntity<MessageResponse> deleteProfile(
            Authentication authentication,
            @PathVariable String profileId) {
        String userId = authentication.getName();

        try {
            userManagementService.deleteProfile(userId, profileId);
            return ResponseEntity.ok(new MessageResponse(true, "Profile deleted successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(false, e.getMessage()));
        }
    }

    // DTOs
    public record RegistrationRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber,
            User.UserRole userRole,
            UserProfile.ProfileType profileType,
            String ssnOrEin,
            String businessName,
            String fiscalYearEnd,
            UserProfile.Address address,
            String tenantId) {
    }

    public record RegistrationResponse(
            boolean success,
            String message,
            String userId) {
    }

    public record ForgotPasswordRequest(String email) {
    }

    public record ResetPasswordRequest(String token, String newPassword) {
    }

    public record MessageResponse(boolean success, String message) {
    }

    public record ProfileCreationRequest(
            UserProfile.ProfileType type,
            String name,
            String ssnOrEin,
            String businessName,
            String fiscalYearEnd,
            UserProfile.Address address,
            String relationshipToUser) {
    }

    public record ProfileUpdateRequest(
            String name,
            String ssnOrEin,
            String businessName,
            String fiscalYearEnd,
            UserProfile.Address address) {
    }
}
