package com.munitax.ledger.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * T096-T097: Resilience Configuration
 * 
 * Implements Circuit Breaker and Retry patterns for:
 * - External payment gateway calls (T097)
 * - Database operations (T096)
 * 
 * Circuit Breaker: Prevents cascading failures by failing fast when a service is down
 * Retry: Automatically retries failed operations with exponential backoff
 */
@Configuration
public class ResilienceConfig {

    /**
     * T097: Circuit Breaker for External Services (Payment Gateway)
     * 
     * Configuration:
     * - Opens after 50% failure rate with minimum 5 calls
     * - Waits 60 seconds before attempting half-open state
     * - Uses sliding window of 10 calls
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig paymentGatewayConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(60)) // Wait 60s before trying again
                .slidingWindowSize(10) // Look at last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls in half-open state
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // Calls > 5s are considered slow
                .slowCallRateThreshold(50) // Open if > 50% of calls are slow
                .build();

        return CircuitBreakerRegistry.of(paymentGatewayConfig);
    }

    /**
     * T096: Retry Configuration for Database Operations
     * 
     * Configuration:
     * - Retries up to 3 times
     * - Uses exponential backoff (1s, 2s, 4s)
     * - Only retries on transient database errors
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig databaseRetryConfig = RetryConfig.custom()
                .maxAttempts(3) // Try up to 3 times
                .waitDuration(Duration.ofSeconds(1)) // Initial wait 1 second
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(1000, 2.0)) // Exponential backoff
                .retryExceptions(
                        // Retry on these transient database exceptions
                        org.springframework.dao.DataAccessException.class,
                        org.springframework.dao.TransientDataAccessException.class,
                        org.springframework.dao.RecoverableDataAccessException.class,
                        java.sql.SQLTransientException.class
                )
                .ignoreExceptions(
                        // Don't retry on these permanent errors
                        org.springframework.dao.DataIntegrityViolationException.class,
                        org.springframework.dao.InvalidDataAccessApiUsageException.class
                )
                .build();

        return RetryRegistry.of(databaseRetryConfig);
    }
}
