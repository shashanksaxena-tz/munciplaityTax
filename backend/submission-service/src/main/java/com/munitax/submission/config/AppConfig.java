package com.munitax.submission.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application configuration for submission service.
 */
@Configuration
public class AppConfig {
    
    /**
     * RestTemplate bean for making HTTP requests to other services.
     * 
     * @return RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
