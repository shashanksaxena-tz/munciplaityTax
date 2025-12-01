package com.munitax.auth.config;

import com.munitax.auth.model.User;
import com.munitax.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Initializes the default admin user on application startup.
 * Admin credentials can be configured via environment variables:
 * - ADMIN_USERNAME (default: admin)
 * - ADMIN_PASSWORD (default: admin - should be changed in production)
 */
@Configuration
public class AdminUserInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserInitializer.class);

    @Bean
    public CommandLineRunner initializeAdminUser(
            UserRepository userRepository, 
            PasswordEncoder passwordEncoder,
            @Value("${admin.username:admin}") String adminUsername,
            @Value("${admin.password:admin}") String adminPassword) {
        return args -> {
            if (!userRepository.existsByEmail(adminUsername)) {
                User adminUser = new User();
                adminUser.setEmail(adminUsername);
                adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
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
                logger.info("Default admin user created with username: {}", adminUsername);
                
                // Warn if using default password
                if ("admin".equals(adminPassword)) {
                    logger.warn("Admin user created with default password. Change this in production by setting ADMIN_PASSWORD environment variable.");
                }
            } else {
                logger.info("Admin user already exists");
            }
        };
    }
}
