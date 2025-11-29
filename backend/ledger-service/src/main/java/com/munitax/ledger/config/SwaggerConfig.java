package com.munitax.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * T084: Swagger/OpenAPI configuration for ledger-service
 * Provides interactive API documentation at /api/docs
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI ledgerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ledger Service API")
                        .description("""
                                Double-Entry Ledger System with Mock Payment Provider
                                
                                This API provides comprehensive accounting ledger functionality including:
                                - Double-entry bookkeeping with automatic journal entries
                                - Mock payment provider for testing (Stripe-like test mode)
                                - Tax assessment recording on both filer and municipality books
                                - Payment processing with multiple payment methods (Credit Card, ACH, Check)
                                - Refund management (request, approval, issuance)
                                - Two-way reconciliation between filer accounts and municipality AR
                                - Account statement generation with transaction history
                                - Trial balance reporting
                                - Complete audit trail for all ledger operations
                                
                                **Test Mode**: The system supports test credit cards:
                                - 4111-1111-1111-1111: Visa (always approved)
                                - 4000-0000-0000-0002: Visa (always declined)
                                - 5555-5555-5555-4444: Mastercard (approved)
                                - 378282246310005: American Express (approved)
                                
                                All monetary amounts are in USD with 2 decimal places.
                                All journal entries automatically balance (debits = credits).
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Munitax Development Team")
                                .email("support@munitax.com")
                                .url("https://www.munitax.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://www.munitax.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api-staging.munitax.com")
                                .description("Staging Environment"),
                        new Server()
                                .url("https://api.munitax.com")
                                .description("Production Environment")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Payments")
                                .description("Payment processing with mock payment provider"),
                        new Tag()
                                .name("Tax Assessments")
                                .description("Record tax assessments with double-entry journal entries"),
                        new Tag()
                                .name("Refunds")
                                .description("Refund request, approval, and issuance management"),
                        new Tag()
                                .name("Reconciliation")
                                .description("Two-way reconciliation between filer and municipality books"),
                        new Tag()
                                .name("Account Statements")
                                .description("Filer account statements with transaction history"),
                        new Tag()
                                .name("Trial Balance")
                                .description("Generate trial balance reports for municipality"),
                        new Tag()
                                .name("Journal Entries")
                                .description("Double-entry journal entry management"),
                        new Tag()
                                .name("Audit Trail")
                                .description("Complete audit history of all ledger operations"),
                        new Tag()
                                .name("Health & Monitoring")
                                .description("Service health checks and monitoring endpoints")
                ));
    }
}
