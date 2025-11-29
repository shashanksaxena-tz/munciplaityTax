package com.munitax.rules;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for rule-service microservice.
 * Manages configurable tax rules with temporal effective dating and multi-tenant support.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaRepositories
@EnableTransactionManagement
public class RuleServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RuleServiceApplication.class, args);
    }
}
