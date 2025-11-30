package com.munitax.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Common CORS Configuration for Standalone Mode
 * Enables direct frontend access to all services when running independently
 * Supports multiple frontend ports for flexible development
 */
@Configuration
@Profile("standalone")
public class CommonStandaloneCorsConfig {

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:3000",
        "http://localhost:3001",
        "http://localhost:3002",
        "http://localhost:3003",
        "http://localhost:5173"  // Vite default port
    );

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowCredentials(true);
        ALLOWED_ORIGINS.forEach(config::addAllowedOrigin);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count",
            "X-Request-Id"
        ));
        config.setMaxAge(3600L); // Cache preflight response for 1 hour
        
        // Apply CORS to all endpoints
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
