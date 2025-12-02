package com.munitax.auth.config;

import com.munitax.auth.model.User;
import com.munitax.auth.model.User.UserRole;
import com.munitax.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Initializes default users for the application.
 * Creates admin and auditor users if they don't exist.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDefaultUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create admin user if not exists
            if (userRepository.findByEmail("admin@example.com").isEmpty()) {
                User admin = new User(
                    "admin@example.com",
                    passwordEncoder.encode("admin"),
                    "System",
                    "Administrator",
                    "0000000000"
                );
                admin.setEmailVerified(true);
                admin.setActive(true);
                admin.setTenantId("dublin");
                admin.setRoles(Set.of(
                    UserRole.ROLE_ADMIN,
                    UserRole.ROLE_MANAGER,
                    UserRole.ROLE_SUPERVISOR,
                    UserRole.ROLE_SENIOR_AUDITOR,
                    UserRole.ROLE_AUDITOR
                ));
                userRepository.save(admin);
                System.out.println("Created default admin user: admin@example.com / admin");
            }

            // Create auditor user if not exists
            if (userRepository.findByEmail("auditor@example.com").isEmpty()) {
                User auditor = new User(
                    "auditor@example.com",
                    passwordEncoder.encode("auditor"),
                    "Demo",
                    "Auditor",
                    "0000000001"
                );
                auditor.setEmailVerified(true);
                auditor.setActive(true);
                auditor.setTenantId("dublin");
                auditor.setRoles(Set.of(
                    UserRole.ROLE_AUDITOR,
                    UserRole.ROLE_SENIOR_AUDITOR
                ));
                userRepository.save(auditor);
                System.out.println("Created default auditor user: auditor@example.com / auditor");
            }

            // Create individual filer test user if not exists
            if (userRepository.findByEmail("filer@example.com").isEmpty()) {
                User filer = new User(
                    "filer@example.com",
                    passwordEncoder.encode("filer"),
                    "Test",
                    "Filer",
                    "5551234567"
                );
                filer.setEmailVerified(true);
                filer.setActive(true);
                filer.setTenantId("dublin");
                filer.setRoles(Set.of(UserRole.ROLE_INDIVIDUAL));
                userRepository.save(filer);
                System.out.println("Created default filer user: filer@example.com / filer");
            }

            // Create business filer test user if not exists
            if (userRepository.findByEmail("business@example.com").isEmpty()) {
                User business = new User(
                    "business@example.com",
                    passwordEncoder.encode("business"),
                    "Business",
                    "Owner",
                    "5559876543"
                );
                business.setEmailVerified(true);
                business.setActive(true);
                business.setTenantId("dublin");
                business.setRoles(Set.of(UserRole.ROLE_BUSINESS));
                userRepository.save(business);
                System.out.println("Created default business user: business@example.com / business");
            }
        };
    }
}
