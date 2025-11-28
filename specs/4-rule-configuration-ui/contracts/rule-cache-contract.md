# Redis Cache Contract: Rule Service

**Version**: 1.0.0  
**Date**: 2025-11-28  
**Purpose**: Document Redis caching strategy for rule-service to achieve <100ms rule retrieval performance

---

## Cache Key Structure

### Active Rules Cache

**Key Pattern**: `rules:{tenantId}:{taxYear}:{entityType}`

**Examples**:
```
rules:dublin:2025:INDIVIDUAL
rules:columbus:2025:C_CORP
rules:cleveland:2026:PARTNERSHIP
rules:GLOBAL:2025:ALL
```

**Value Type**: JSON string (serialized `TaxRulesConfig` object)

**Value Example**:
```json
{
  "tenantId": "dublin",
  "taxYear": 2025,
  "entityType": "INDIVIDUAL",
  "rules": [
    {
      "ruleId": "550e8400-e29b-41d4-a716-446655440000",
      "ruleCode": "MUNICIPAL_RATE",
      "ruleName": "Municipal Tax Rate",
      "category": "TaxRates",
      "valueType": "PERCENTAGE",
      "value": {"scalar": 2.0, "unit": "percent"},
      "effectiveDate": "2020-01-01",
      "endDate": null,
      "tenantId": "dublin",
      "entityTypes": ["ALL"],
      "version": 1,
      "approvalStatus": "APPROVED"
    },
    {
      "ruleId": "650e8400-e29b-41d4-a716-446655440001",
      "ruleCode": "W2_QUALIFYING_WAGES_RULE",
      "ruleName": "W-2 Qualifying Wages Method",
      "category": "TaxRates",
      "valueType": "ENUM",
      "value": {"option": "HIGHEST_OF_ALL", "allowedValues": ["HIGHEST_OF_ALL", "BOX_5_MEDICARE", "BOX_18_LOCAL"]},
      "effectiveDate": "2020-01-01",
      "endDate": null,
      "tenantId": "dublin",
      "entityTypes": ["INDIVIDUAL"],
      "version": 1,
      "approvalStatus": "APPROVED"
    }
  ],
  "cachedAt": "2025-11-28T10:30:00Z"
}
```

**TTL (Time To Live)**: 24 hours (86400 seconds)

**Rationale**: 
- Rules change infrequently (2-4 times per year per tenant)
- 24-hour TTL ensures stale cache doesn't persist more than 1 day
- Manual invalidation on rule publish provides immediate consistency

---

## Cache Operations

### 1. Cache Write (SET)

**Operation**: Store active rules for tenant+taxYear+entityType

**Spring Boot Code**:
```java
@Service
public class RuleCacheService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public void cacheActiveRules(String tenantId, int taxYear, EntityType entityType, List<TaxRule> rules) {
        String key = buildCacheKey(tenantId, taxYear, entityType);
        
        ActiveRulesCacheEntry cacheEntry = new ActiveRulesCacheEntry(
            tenantId, taxYear, entityType, rules, Instant.now()
        );
        
        try {
            String json = objectMapper.writeValueAsString(cacheEntry);
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(24));
        } catch (JsonProcessingException e) {
            // Log error, but don't fail the request (cache miss acceptable)
            log.error("Failed to cache rules for key: {}", key, e);
        }
    }
    
    private String buildCacheKey(String tenantId, int taxYear, EntityType entityType) {
        return String.format("rules:%s:%d:%s", tenantId, taxYear, entityType.name());
    }
}
```

**Redis Command Equivalent**:
```bash
SET rules:dublin:2025:INDIVIDUAL '{"tenantId":"dublin",...}' EX 86400
```

---

### 2. Cache Read (GET)

**Operation**: Retrieve cached rules

**Spring Boot Code**:
```java
public Optional<List<TaxRule>> getCachedRules(String tenantId, int taxYear, EntityType entityType) {
    String key = buildCacheKey(tenantId, taxYear, entityType);
    
    String json = redisTemplate.opsForValue().get(key);
    if (json == null) {
        return Optional.empty(); // Cache miss
    }
    
    try {
        ActiveRulesCacheEntry cacheEntry = objectMapper.readValue(json, ActiveRulesCacheEntry.class);
        return Optional.of(cacheEntry.getRules());
    } catch (JsonProcessingException e) {
        log.error("Failed to deserialize cached rules for key: {}", key, e);
        // Delete corrupted cache entry
        redisTemplate.delete(key);
        return Optional.empty();
    }
}
```

**Redis Command Equivalent**:
```bash
GET rules:dublin:2025:INDIVIDUAL
```

---

### 3. Cache Invalidation (DELETE)

**Operation**: Clear cached rules when rule is published/updated

**Invalidation Strategies**:

#### Strategy A: Delete All Keys for Tenant (Recommended)
When rule is approved/updated, invalidate all cached rule sets for that tenant (across all years).

**Spring Boot Code**:
```java
public void invalidateTenantCache(String tenantId) {
    String pattern = String.format("rules:%s:*", tenantId);
    Set<String> keys = redisTemplate.keys(pattern);
    
    if (keys != null && !keys.isEmpty()) {
        Long deletedCount = redisTemplate.delete(keys);
        log.info("Invalidated {} cache entries for tenant: {}", deletedCount, tenantId);
    }
}
```

**Redis Command Equivalent**:
```bash
# Find all keys for tenant
KEYS rules:dublin:*

# Delete matching keys
DEL rules:dublin:2025:INDIVIDUAL rules:dublin:2025:C_CORP rules:dublin:2026:INDIVIDUAL
```

**Pros**: Simple, ensures consistency across all tax years
**Cons**: Deletes cache for unaffected years (e.g., changing 2026 rule clears 2025 cache)

#### Strategy B: Selective Invalidation (Optional Optimization)
Only delete cache keys for affected tax year range.

**Spring Boot Code**:
```java
public void invalidateRuleCache(String tenantId, LocalDate effectiveDate, LocalDate endDate) {
    int startYear = effectiveDate.getYear();
    int endYear = (endDate != null) ? endDate.getYear() : LocalDate.now().getYear() + 10;
    
    for (int year = startYear; year <= endYear; year++) {
        for (EntityType entityType : EntityType.values()) {
            String key = buildCacheKey(tenantId, year, entityType);
            redisTemplate.delete(key);
        }
    }
    
    log.info("Invalidated cache for tenant {} years {}-{}", tenantId, startYear, endYear);
}
```

**Pros**: More efficient, only invalidates affected years
**Cons**: More complex logic, potential edge cases with overlapping date ranges

**Recommendation**: Use Strategy A (delete all tenant keys) for v1. Optimize with Strategy B if cache churn becomes performance issue.

---

### 4. Cache Warming (Optional)

**Operation**: Preload commonly accessed rule sets on service startup

**Spring Boot Code**:
```java
@Component
public class CacheWarmer {
    @Autowired
    private RuleCacheService cacheService;
    
    @Autowired
    private TaxRuleRepository ruleRepository;
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmCache() {
        log.info("Starting cache warming...");
        
        int currentYear = LocalDate.now().getYear();
        List<String> tenants = List.of("dublin", "columbus", "cleveland");
        List<EntityType> entityTypes = List.of(EntityType.INDIVIDUAL, EntityType.C_CORP);
        
        for (String tenant : tenants) {
            for (EntityType entityType : entityTypes) {
                for (int year = currentYear - 1; year <= currentYear + 1; year++) {
                    // Query DB and cache
                    List<TaxRule> rules = ruleRepository.findActiveRules(tenant, year, entityType);
                    cacheService.cacheActiveRules(tenant, year, entityType, rules);
                }
            }
        }
        
        log.info("Cache warming complete");
    }
}
```

**When to Use**: 
- Production environment with predictable traffic patterns
- High cache hit ratio expected (same rules queried repeatedly)
- Service startup time not critical (warming adds 5-10 seconds)

**When to Skip**:
- Development/test environments (cache warming unnecessary overhead)
- Dynamic tenant list (can't preload all tenants)
- Low traffic admin service (lazy loading sufficient)

---

## Cache Consistency Strategy

### Consistency Model: **Eventual Consistency**

Rules are not mission-critical real-time data. It's acceptable for tax calculations to use slightly stale rules for 1-2 seconds after rule update, until cache invalidation propagates.

### Cache Invalidation Sequence (Important!)

**WRONG** (race condition):
```
1. Save rule to database (COMMIT)
2. Invalidate Redis cache
   ↓ Race window: Another request reads stale cache between steps 1-2
3. Next request queries DB and rebuilds cache
```

**CORRECT** (cache-aside pattern):
```
1. Invalidate Redis cache FIRST (delete keys)
2. Save rule to database (COMMIT)
3. Next request queries DB and rebuilds cache
```

**Implementation**:
```java
@Transactional
public TaxRule approveRule(UUID ruleId, String approvedBy, String approvalReason) {
    TaxRule rule = ruleRepository.findById(ruleId)
        .orElseThrow(() -> new RuleNotFoundException(ruleId));
    
    // 1. INVALIDATE CACHE FIRST (before database commit)
    cacheService.invalidateTenantCache(rule.getTenantId());
    
    // 2. Update rule in database
    rule.setApprovalStatus(ApprovalStatus.APPROVED);
    rule.setApprovedBy(approvedBy);
    rule.setApprovalDate(LocalDateTime.now());
    rule = ruleRepository.save(rule);
    
    // 3. Commit transaction (cache already invalidated, safe)
    return rule;
}
```

**Rationale**: 
- Invalidating cache first ensures no stale data served
- Worst case: Cache miss → database query → rebuild cache (acceptable)
- No risk of serving stale data from cache

---

## Cache Monitoring & Metrics

### Key Metrics to Track

1. **Cache Hit Ratio**:
   - Formula: `cache_hits / (cache_hits + cache_misses)`
   - Target: >80% (indicates caching is effective)
   - Tool: Redis INFO command, Spring Boot Actuator metrics

2. **Cache Size**:
   - Metric: Number of keys matching `rules:*` pattern
   - Expected: 10 tenants × 3 years × 5 entity types = 150 keys
   - Tool: `redis-cli DBSIZE`, Redis INFO memory stats

3. **Cache Invalidation Frequency**:
   - Metric: Number of DELETE operations on `rules:*` keys
   - Expected: 2-4 per tenant per year (rule change frequency)
   - Tool: Redis MONITOR command (caution: high overhead), application logs

4. **Rule Query Latency**:
   - Metric: p50, p95, p99 latency for `/api/rules/active` endpoint
   - Target: <100ms p95 (with cache hit), <200ms p99 (with cache miss)
   - Tool: Spring Boot Actuator metrics, APM (New Relic, DataDog)

### Redis INFO Command Output

```bash
# Check memory usage
redis-cli INFO memory
# used_memory_human:128.50M
# used_memory_peak_human:150.00M

# Check keyspace stats
redis-cli INFO keyspace
# db0:keys=150,expires=150,avg_ttl=72000000

# Check hit/miss ratio
redis-cli INFO stats
# keyspace_hits:45000
# keyspace_misses:5000
# Hit ratio: 90%
```

---

## Cache Failure Handling

### Failure Scenario 1: Redis Unavailable

**Behavior**: Rule-service should degrade gracefully, fall back to database queries

**Implementation**:
```java
public List<TaxRule> getActiveRules(String tenantId, int taxYear, EntityType entityType) {
    try {
        // Attempt cache read
        Optional<List<TaxRule>> cachedRules = cacheService.getCachedRules(tenantId, taxYear, entityType);
        if (cachedRules.isPresent()) {
            return cachedRules.get();
        }
    } catch (RedisConnectionException e) {
        log.warn("Redis unavailable, falling back to database query", e);
    }
    
    // Cache miss or Redis down → Query database
    List<TaxRule> rules = ruleRepository.findActiveRules(tenantId, taxYear, entityType);
    
    try {
        // Attempt cache write (may fail if Redis still down)
        cacheService.cacheActiveRules(tenantId, taxYear, entityType, rules);
    } catch (RedisConnectionException e) {
        log.warn("Failed to cache rules, proceeding without cache", e);
    }
    
    return rules;
}
```

**Circuit Breaker** (Optional):
```java
@CircuitBreaker(name = "redis", fallbackMethod = "getActiveRulesFallback")
public List<TaxRule> getActiveRules(String tenantId, int taxYear, EntityType entityType) {
    // ... cache logic
}

public List<TaxRule> getActiveRulesFallback(String tenantId, int taxYear, EntityType entityType, Throwable t) {
    log.error("Redis circuit breaker open, using database only", t);
    return ruleRepository.findActiveRules(tenantId, taxYear, entityType);
}
```

### Failure Scenario 2: Cache Poisoning (Corrupted Data)

**Behavior**: If deserialization fails, delete corrupted cache entry and query database

**Implementation**: (Already shown in Cache Read section above)

---

## Redis Configuration

### Spring Boot application.yml

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms

  cache:
    type: redis
    redis:
      time-to-live: 86400000  # 24 hours in milliseconds
      cache-null-values: false
      key-prefix: "rules:"
```

### Redis Configuration Bean

```java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for both keys and values (JSON stored as string)
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.setEnableTransactionSupport(false); // Redis transactions not needed
        return template;
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

---

## Testing Strategy

### Unit Test: Cache Hit/Miss

```java
@Test
public void testCacheHit() {
    String tenantId = "dublin";
    int taxYear = 2025;
    EntityType entityType = EntityType.INDIVIDUAL;
    
    List<TaxRule> rules = createSampleRules();
    
    // Write to cache
    cacheService.cacheActiveRules(tenantId, taxYear, entityType, rules);
    
    // Read from cache (should hit)
    Optional<List<TaxRule>> cached = cacheService.getCachedRules(tenantId, taxYear, entityType);
    
    assertTrue(cached.isPresent());
    assertEquals(rules.size(), cached.get().size());
}

@Test
public void testCacheMiss() {
    String tenantId = "nonexistent";
    int taxYear = 2025;
    EntityType entityType = EntityType.INDIVIDUAL;
    
    // No cache entry exists
    Optional<List<TaxRule>> cached = cacheService.getCachedRules(tenantId, taxYear, entityType);
    
    assertFalse(cached.isPresent());
}
```

### Integration Test: Cache Invalidation

```java
@SpringBootTest
@Testcontainers
public class CacheInvalidationTest {
    
    @Container
    private static final GenericContainer<?> redis = 
        new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    
    @Test
    public void testInvalidationOnRuleApproval() {
        // Create and cache rules
        TaxRule rule = createPendingRule();
        cacheService.cacheActiveRules(rule.getTenantId(), 2025, EntityType.INDIVIDUAL, List.of(rule));
        
        // Verify cache populated
        Optional<List<TaxRule>> cached = cacheService.getCachedRules("dublin", 2025, EntityType.INDIVIDUAL);
        assertTrue(cached.isPresent());
        
        // Approve rule (should invalidate cache)
        ruleService.approveRule(rule.getRuleId(), "admin", "Approved");
        
        // Verify cache cleared
        Optional<List<TaxRule>> afterInvalidation = cacheService.getCachedRules("dublin", 2025, EntityType.INDIVIDUAL);
        assertFalse(afterInvalidation.isPresent(), "Cache should be invalidated after approval");
    }
}
```

---

## Performance Benchmarks

### Expected Performance (with caching)

| Operation | Latency (p50) | Latency (p95) | Notes |
|-----------|---------------|---------------|-------|
| Cache Hit | 1-3ms | 5ms | Redis GET + deserialization |
| Cache Miss + DB Query | 50-80ms | 150ms | PostgreSQL query + serialization + cache write |
| Cache Invalidation | 10-20ms | 30ms | Delete 10-20 keys |

### Expected Performance (without caching - baseline)

| Operation | Latency (p50) | Latency (p95) | Notes |
|-----------|---------------|---------------|-------|
| Database Query | 80-120ms | 200ms | PostgreSQL temporal query |

**Speedup**: 30-50x faster with cache hit (1-3ms vs 80-120ms)

---

## Cache Key Namespace

All cache keys use prefix `rules:` to avoid collision with other services using same Redis instance.

**Other Potential Cache Keys** (for future features):
```
rules:pending:{tenantId}         # List of pending rules for approval
rules:recent:{tenantId}          # Recently changed rules (for notifications)
rules:formula:compiled:{ruleId}  # Compiled formula bytecode (performance optimization)
```

---

## Contract Compliance Checklist

- [x] Cache key structure documented with examples
- [x] Value structure (JSON schema) documented
- [x] TTL policy defined (24 hours)
- [x] Cache operations (SET, GET, DELETE) documented with code examples
- [x] Cache invalidation strategy defined (delete all tenant keys)
- [x] Cache consistency model defined (eventual consistency)
- [x] Cache failure handling strategy documented (graceful degradation)
- [x] Redis configuration documented (Spring Boot YAML)
- [x] Testing strategy documented (unit + integration tests)
- [x] Performance benchmarks provided
- [x] Monitoring metrics defined (hit ratio, size, latency)

**Next Phase**: Create quickstart.md for developer onboarding
