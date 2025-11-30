package com.munitax.ledger.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * T093: Rate Limiting Configuration
 * Implements token bucket algorithm to prevent API abuse
 * 
 * Features:
 * - Per-IP rate limiting
 * - Different limits for read vs write operations
 * - Configurable limits via application properties
 * - Returns 429 Too Many Requests when limit exceeded
 * 
 * Note: For production, consider using Redis-based rate limiting for distributed systems
 */
@Configuration
public class RateLimitConfig extends OncePerRequestFilter implements WebMvcConfigurer {

    @Value("${ledger.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${ledger.security.rate-limit.read-per-minute:100}")
    private int readRequestsPerMinute;

    @Value("${ledger.security.rate-limit.write-per-minute:20}")
    private int writeRequestsPerMinute;

    // In-memory bucket cache per IP address
    // For production, use Redis or similar distributed cache
    private final Map<String, Bucket> readBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> writeBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Skip rate limiting if disabled
        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for actuator endpoints (health checks)
        String path = request.getRequestURI();
        if (path.startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get client IP address
        String clientIp = getClientIP(request);
        
        // Determine if this is a read or write operation
        boolean isWriteOperation = isWriteOperation(request.getMethod());
        
        // Get or create bucket for this IP
        Bucket bucket = isWriteOperation 
                ? writeBuckets.computeIfAbsent(clientIp, k -> createWriteBucket())
                : readBuckets.computeIfAbsent(clientIp, k -> createReadBucket());
        
        // Try to consume a token
        if (bucket.tryConsume(1)) {
            // Request allowed
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\": \"Rate limit exceeded\", " +
                "\"message\": \"Too many %s requests. Please try again later.\", " +
                "\"limit\": %d, " +
                "\"window\": \"1 minute\"}",
                isWriteOperation ? "write" : "read",
                isWriteOperation ? writeRequestsPerMinute : readRequestsPerMinute
            ));
        }
    }

    private Bucket createReadBucket() {
        Bandwidth limit = Bandwidth.classic(
                readRequestsPerMinute, 
                Refill.greedy(readRequestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createWriteBucket() {
        Bandwidth limit = Bandwidth.classic(
                writeRequestsPerMinute, 
                Refill.greedy(writeRequestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private boolean isWriteOperation(String method) {
        return method.equals("POST") || 
               method.equals("PUT") || 
               method.equals("PATCH") || 
               method.equals("DELETE");
    }

    private String getClientIP(HttpServletRequest request) {
        // Try to get IP from X-Forwarded-For header (if behind proxy/load balancer)
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        
        // Try other common proxy headers
        String xrHeader = request.getHeader("X-Real-IP");
        if (xrHeader != null && !xrHeader.isEmpty()) {
            return xrHeader;
        }
        
        // Fall back to remote address
        return request.getRemoteAddr();
    }

    // Cleanup old buckets periodically to prevent memory leak
    // In production, use a scheduled task or TTL-based cache
    public void cleanupOldBuckets() {
        // Simple cleanup: clear all buckets every hour
        // More sophisticated: track last access time and remove stale entries
        readBuckets.clear();
        writeBuckets.clear();
    }
}
