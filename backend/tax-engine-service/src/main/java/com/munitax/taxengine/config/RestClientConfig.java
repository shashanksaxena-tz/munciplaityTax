package com.munitax.taxengine.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for REST client components.
 * Provides RestTemplate bean for integration with external services.
 */
@Configuration
public class RestClientConfig {
    
    /**
     * Create RestTemplate bean for external service integration.
     * Configured with reasonable timeouts for rule-engine-service and pdf-service calls.
     * 
     * @param builder the RestTemplateBuilder
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
