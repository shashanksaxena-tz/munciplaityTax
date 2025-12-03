package com.munitax.rules.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT authentication filter for rule-service.
 * Extracts user and role information from JWT token.
 * 
 * NOTE: This is a simplified implementation. In production, use Spring Security OAuth2 Resource Server.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final Environment environment;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Extract JWT from Authorization header
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                // ⚠️ SECURITY WARNING: This is a placeholder implementation for development only!
                // TODO: Implement actual JWT parsing and validation before production deployment
                // Current behavior: Accepts ANY Bearer token without validation
                
                // Check if running in production and log critical warning
                String[] activeProfiles = environment.getActiveProfiles();
                boolean isProduction = Arrays.stream(activeProfiles)
                    .anyMatch(profile -> profile.equalsIgnoreCase("prod") || 
                                       profile.equalsIgnoreCase("production"));
                
                if (isProduction) {
                    log.error("⚠️ CRITICAL SECURITY ISSUE: Hardcoded JWT credentials in production! " +
                             "This accepts ANY token. Implement proper JWT validation immediately.");
                }
                
                // Extract user ID and role from token (simplified - INSECURE)
                String userId = "admin";  // Default
                String role = "TAX_ADMINISTRATOR";  // Default
                String tenantId = "dublin";  // Default
                
                // Handle demo tokens explicitly
                if (token.equals("demo-token-admin")) {
                    userId = "demo-user-1";
                    role = "TAX_ADMINISTRATOR";
                    tenantId = "dublin";
                } else if (token.equals("demo-token-auditor")) {
                    userId = "auditor-user-1";
                    role = "AUDITOR";
                    tenantId = "dublin";
                }
                
                // Create authentication
                List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
                );
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Store tenant context for RLS
                request.setAttribute("tenantId", tenantId);
                request.setAttribute("userRole", role);
                
                log.debug("Authenticated user: {} with role: {} for tenant: {}", 
                         userId, role, tenantId);
                
            } catch (Exception e) {
                log.error("JWT authentication failed", e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
