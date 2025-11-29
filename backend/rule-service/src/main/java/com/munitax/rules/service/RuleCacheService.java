package com.munitax.rules.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Service for managing Redis cache of tax rules.
 * Provides cache operations with tenant-scoped invalidation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RuleCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${app.cache.rule-ttl:86400}")
    private long ruleCacheTtl;  // Default 24 hours
    
    @Value("${app.cache.tenant-cache-prefix:rules:tenant:}")
    private String tenantCachePrefix;
    
    /**
     * Get cached rules for a tenant.
     * 
     * @param tenantId Tenant identifier
     * @param cacheKey Specific cache key (e.g., "rules:tenant:dublin:2024")
     * @return Cached object or null if not found
     */
    public Object get(String tenantId, String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.error("Redis cache get failed for key: {}", cacheKey, e);
            return null;  // Graceful degradation
        }
    }
    
    /**
     * Cache rules for a tenant with TTL.
     * 
     * @param tenantId Tenant identifier
     * @param cacheKey Specific cache key
     * @param value Object to cache
     */
    public void put(String tenantId, String cacheKey, Object value) {
        try {
            redisTemplate.opsForValue().set(cacheKey, value, Duration.ofSeconds(ruleCacheTtl));
            log.debug("Cached rules for key: {} with TTL: {}s", cacheKey, ruleCacheTtl);
        } catch (Exception e) {
            log.error("Redis cache put failed for key: {}", cacheKey, e);
            // Don't throw exception - caching failure shouldn't break application
        }
    }
    
    /**
     * Invalidate all cached rules for a specific tenant.
     * Called when any rule is modified for that tenant.
     * Uses SCAN instead of KEYS for production safety.
     * 
     * @param tenantId Tenant identifier
     */
    public void invalidateTenantCache(String tenantId) {
        try {
            String pattern = tenantCachePrefix + tenantId + ":*";
            
            // Use SCAN instead of KEYS for production safety (non-blocking operation)
            Set<String> keysToDelete = new HashSet<>();
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                ScanOptions options = ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100)
                        .build();
                
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        keysToDelete.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                } catch (Exception e) {
                    log.error("Error scanning Redis keys", e);
                }
                return null;
            });
            
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.info("Invalidated {} cache keys for tenant: {}", keysToDelete.size(), tenantId);
            }
        } catch (Exception e) {
            log.error("Redis cache invalidation failed for tenant: {}", tenantId, e);
        }
    }
    
    /**
     * Invalidate a specific cache key.
     * 
     * @param cacheKey Cache key to invalidate
     */
    public void invalidate(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated cache key: {}", cacheKey);
        } catch (Exception e) {
            log.error("Redis cache invalidation failed for key: {}", cacheKey, e);
        }
    }
    
    /**
     * Invalidate all rules cache (use sparingly - global operation).
     * Uses SCAN instead of KEYS for production safety.
     */
    public void invalidateAllRules() {
        try {
            String pattern = tenantCachePrefix + "*";
            
            // Use SCAN instead of KEYS for production safety (non-blocking operation)
            Set<String> keysToDelete = new HashSet<>();
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                ScanOptions options = ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100)
                        .build();
                
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        keysToDelete.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                } catch (Exception e) {
                    log.error("Error scanning Redis keys", e);
                }
                return null;
            });
            
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.warn("Invalidated ALL rule cache keys: {} total", keysToDelete.size());
            }
        } catch (Exception e) {
            log.error("Global cache invalidation failed", e);
        }
    }
    
    /**
     * Build cache key for tenant and tax year.
     * 
     * @param tenantId Tenant identifier
     * @param taxYear Tax year
     * @return Cache key string
     */
    public String buildCacheKey(String tenantId, int taxYear) {
        return tenantCachePrefix + tenantId + ":" + taxYear;
    }
    
    /**
     * Build cache key for tenant, tax year, and entity type.
     * 
     * @param tenantId Tenant identifier
     * @param taxYear Tax year
     * @param entityType Entity type (e.g., "C-CORP")
     * @return Cache key string
     */
    public String buildCacheKey(String tenantId, int taxYear, String entityType) {
        return tenantCachePrefix + tenantId + ":" + taxYear + ":" + entityType;
    }
}
