package com.munitax.auth.service;

import com.munitax.auth.model.User;
import com.munitax.auth.model.UserProfile;
import com.munitax.auth.repository.UserProfileRepository;
import com.munitax.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    // TODO: Add EmailService for verification emails

    public UserManagementService(UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResult registerUser(RegistrationRequest request) {
        // Validate email not already registered
        if (userRepository.existsByEmail(request.email())) {
            return new RegistrationResult(false, "Email already registered", null);
        }

        // Create user
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.phoneNumber());

        // Add role based on registration type
        user.addRole(request.userRole());

        // Set tenant
        user.setTenantId(request.tenantId());

        // Generate email verification token
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));

        // Save user
        user = userRepository.save(user);

        // Create primary profile
        UserProfile primaryProfile = new UserProfile();
        primaryProfile.setUserId(user.getId());
        primaryProfile.setType(request.profileType());
        primaryProfile.setName(user.getFullName());
        primaryProfile.setSsnOrEin(request.ssnOrEin());
        primaryProfile.setAddress(request.address());
        primaryProfile.setPrimary(true);

        if (request.profileType() == UserProfile.ProfileType.BUSINESS) {
            primaryProfile.setBusinessName(request.businessName());
            primaryProfile.setFiscalYearEnd(request.fiscalYearEnd());
        }

        userProfileRepository.save(primaryProfile);

        // TODO: Send verification email

        return new RegistrationResult(true, "Registration successful. Please check your email to verify your account.",
                user.getId());
    }

    @Transactional
    public boolean verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByEmailVerificationToken(token);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Check if token is expired
        if (user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Verify email
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        userRepository.save(user);

        return true;
    }

    @Transactional
    public boolean initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Don't reveal if email exists
            return true;
        }

        User user = userOpt.get();
        user.setPasswordResetToken(UUID.randomUUID().toString());
        user.setPasswordResetExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // TODO: Send password reset email

        return true;
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByPasswordResetToken(token);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Check if token is expired
        if (user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Reset password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);

        return true;
    }

    // Profile Management
    @Transactional
    public UserProfile createProfile(String userId, ProfileCreationRequest request) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setType(request.type());
        profile.setName(request.name());
        profile.setSsnOrEin(request.ssnOrEin());
        profile.setAddress(request.address());
        profile.setRelationshipToUser(request.relationshipToUser());
        profile.setPrimary(false);

        if (request.type() == UserProfile.ProfileType.BUSINESS) {
            profile.setBusinessName(request.businessName());
            profile.setFiscalYearEnd(request.fiscalYearEnd());
        }

        return userProfileRepository.save(profile);
    }

    public List<UserProfile> getUserProfiles(String userId) {
        return userProfileRepository.findByUserIdAndActive(userId, true);
    }

    public Optional<UserProfile> getPrimaryProfile(String userId) {
        return userProfileRepository.findByUserIdAndIsPrimaryAndActive(userId, true, true);
    }

    @Transactional
    public UserProfile updateProfile(String userId, String profileId, ProfileUpdateRequest request) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByIdAndUserId(profileId, userId);

        if (profileOpt.isEmpty()) {
            throw new RuntimeException("Profile not found");
        }

        UserProfile profile = profileOpt.get();
        profile.setName(request.name());
        profile.setSsnOrEin(request.ssnOrEin());
        profile.setAddress(request.address());

        if (profile.getType() == UserProfile.ProfileType.BUSINESS) {
            profile.setBusinessName(request.businessName());
            profile.setFiscalYearEnd(request.fiscalYearEnd());
        }

        return userProfileRepository.save(profile);
    }

    @Transactional
    public void deleteProfile(String userId, String profileId) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByIdAndUserId(profileId, userId);

        if (profileOpt.isEmpty()) {
            throw new RuntimeException("Profile not found");
        }

        UserProfile profile = profileOpt.get();

        // Don't allow deletion of primary profile
        if (profile.isPrimary()) {
            throw new RuntimeException("Cannot delete primary profile");
        }

        profile.setActive(false);
        userProfileRepository.save(profile);
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

    public record RegistrationResult(
            boolean success,
            String message,
            String userId) {
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
