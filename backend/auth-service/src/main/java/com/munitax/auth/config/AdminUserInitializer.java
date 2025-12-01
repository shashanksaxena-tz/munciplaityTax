package com.munitax.auth.config;

import com.munitax.auth.model.User;
import com.munitax.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Initializes the default admin user on application startup.
 * Admin credentials: username=admin, password=admin
 */
@Configuration
public class AdminUserInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserInitializer.class);
    private static final String ADMIN_EMAIL = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    @Bean
    public CommandLineRunner initializeAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
                User adminUser = new User();
                adminUser.setEmail(ADMIN_EMAIL);
                adminUser.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
                adminUser.setFirstName("System");
                adminUser.setLastName("Administrator");
                adminUser.setPhoneNumber("000-000-0000");
                adminUser.setEmailVerified(true);
                adminUser.setActive(true);
                adminUser.setCreatedAt(LocalDateTime.now());
                adminUser.setUpdatedAt(LocalDateTime.now());
                
                // Admin has all roles - can do everything any lower role can do
                Set<User.UserRole> roles = new HashSet<>();
                roles.add(User.UserRole.ROLE_ADMIN);
                roles.add(User.UserRole.ROLE_MANAGER);
                roles.add(User.UserRole.ROLE_SUPERVISOR);
                roles.add(User.UserRole.ROLE_SENIOR_AUDITOR);
                roles.add(User.UserRole.ROLE_AUDITOR);
                roles.add(User.UserRole.ROLE_BUSINESS);
                roles.add(User.UserRole.ROLE_INDIVIDUAL);
                adminUser.setRoles(roles);
                
                // Admin doesn't belong to a specific tenant - can manage all tenants
                adminUser.setTenantId(null);
                
                userRepository.save(adminUser);
                logger.info("Default admin user created with email: {}", ADMIN_EMAIL);
            } else {
                logger.info("Admin user already exists");
            }
        };
    }
}
