# Specification: Tax Engine - Rule Service Integration

**Feature Number**: 14  
**Created**: 2025-12-03  
**Status**: Implemented  

## 1. Overview

### Problem Statement

The tax-engine-service and rule-service were completely disconnected, causing tax calculations to ignore dynamically configured rules. Despite users creating rules in the rule management UI (e.g., NEW_LAGAAN with 60000% tax rate), tax calculations continued using hardcoded values (2% municipal tax rate). This architectural gap made the rule management system non-functional for its primary purpose.

### Solution Summary

Implement microservices integration using Spring Cloud OpenFeign to enable tax-engine-service to dynamically fetch and apply tax rates from rule-service at calculation time. Include caching for performance optimization and fallback mechanisms for resilience.

## 2. Business Value

### User Benefits
- **Municipal Tax Administrators**: Rule changes take effect immediately in calculations without code deployments
- **Tax Filers**: Accurate tax calculations using current approved rules
- **System Administrators**: Single source of truth for tax rules with proper governance

### Business Outcomes
- Eliminate manual code changes for tax rate updates
- Reduce deployment cycles from days to instant (rule approval)
- Enable A/B testing of tax policies via effective date ranges
- Improve audit trail with rule versioning and approval workflow

## 3. Functional Requirements

### FR-1: Dynamic Tax Rate Resolution
- **Requirement**: Tax-engine-service MUST fetch active tax rates from rule-service for each tax calculation
- **Acceptance Criteria**:
  - Tax calculations use rates from rule-service when available
  - System logs which rule code and rate was applied
  - Fallback to configured default rate if rule-service unavailable
- **Priority**: CRITICAL

### FR-2: Multi-Tenant Support
- **Requirement**: Tax rates MUST be resolved per tenant and tax year combination
- **Acceptance Criteria**:
  - Different tenants can have different tax rates for same tax year
  - Tenant context is passed in all rule-service API calls
- **Priority**: CRITICAL

### FR-3: Rule Code Flexibility
- **Requirement**: System MUST support both standard rule codes (MUNICIPAL_TAX_RATE) and custom rule codes (NEW_LAGAAN)
- **Acceptance Criteria**:
  - TaxRateResolverService searches for both standard and custom rule codes
  - First matching active rule is used for calculation
- **Priority**: HIGH

### FR-4: Performance Optimization via Caching
- **Requirement**: System MUST cache resolved tax rates to minimize API calls
- **Acceptance Criteria**:
  - Tax rates cached with key: `${tenantId}-${taxYear}-${ruleCode}`
  - Cache hit rate > 80% for repeated calculations within same tenant/tax year
  - Cache duration: configurable (default 1 hour)
- **Priority**: HIGH

### FR-5: Resilience and Fallback
- **Requirement**: Tax calculations MUST NOT fail when rule-service is unavailable
- **Acceptance Criteria**:
  - System catches exceptions from rule-service calls
  - Falls back to configured default rate (application.yml: tax.municipal.rate=0.0200)
  - Logs warning when using fallback rate
- **Priority**: HIGH

### FR-6: Service Discovery Integration
- **Requirement**: System MUST use Eureka service discovery to locate rule-service dynamically
- **Acceptance Criteria**:
  - Feign client configured with service name ("rule-service") not hardcoded URL
  - Integration works in local dev, Docker Compose, and Kubernetes environments
- **Priority**: MEDIUM

## 4. Technical Design

### 4.1 Architecture

**Component Diagram**:
```
┌─────────────────┐       ┌──────────────────┐       ┌─────────────────┐
│ W1FilingService │──────>│ TaxRateResolver  │──────>│ RuleServiceClient│
│                 │       │    Service       │       │   (Feign)        │
└─────────────────┘       └──────────────────┘       └─────────────────┘
                                  │                            │
                                  │                            │
                                  v                            v
                          ┌──────────────┐           ┌──────────────┐
                          │ Spring Cache │           │ rule-service │
                          └──────────────┘           │   REST API   │
                                                      └──────────────┘
```

### 4.2 Data Flow

**Sequence Diagram: Tax Calculation with Dynamic Rate**
```
User -> W1FilingService: fileW1Return(request)
W1FilingService -> TaxRateResolverService: getMunicipalTaxRate(tenantId, taxYear, defaultRate)
TaxRateResolverService -> Spring Cache: check cache[dublin-2025-MUNICIPAL_TAX_RATE]
Spring Cache --> TaxRateResolverService: MISS
TaxRateResolverService -> RuleServiceClient: getActiveRules(tenantId, taxYear, entityType)
RuleServiceClient -> Eureka: resolve "rule-service" location
Eureka --> RuleServiceClient: http://rule-service:8087
RuleServiceClient -> rule-service: GET /api/rules/active?tenantId=dublin&taxYear=2025&entityType=INDIVIDUAL
rule-service --> RuleServiceClient: [{"ruleCode": "NEW_LAGAAN", "value": {"scalar": 60000}, ...}]
RuleServiceClient --> TaxRateResolverService: List<RuleResponse>
TaxRateResolverService: parse value, convert 60000% -> 600.00 decimal
TaxRateResolverService -> Spring Cache: put[dublin-2025-MUNICIPAL_TAX_RATE] = 600.00
TaxRateResolverService --> W1FilingService: BigDecimal(600.00)
W1FilingService: taxDue = taxableWages × 600.00
W1FilingService --> User: W1FilingResponse with taxDue
```

### 4.3 Implementation Components

#### Component 1: RuleServiceClient (Feign Interface)
**File**: `backend/tax-engine-service/src/main/java/com/munitax/taxengine/integration/client/RuleServiceClient.java`

**Responsibilities**:
- Declarative REST client for rule-service API
- Uses Eureka service discovery to locate rule-service
- Handles HTTP communication with load balancing

**Key Methods**:
```java
@GetMapping("/active")
List<RuleResponse> getActiveRules(
    @RequestParam("tenantId") String tenantId,
    @RequestParam("taxYear") int taxYear,
    @RequestParam("entityType") String entityType
);
```

#### Component 2: TaxRateResolverService
**File**: `backend/tax-engine-service/src/main/java/com/munitax/taxengine/integration/service/TaxRateResolverService.java`

**Responsibilities**:
- Business logic for fetching and parsing tax rates
- Caching with @Cacheable annotation
- Error handling and fallback to default rates
- Percentage to decimal conversion (60000% → 600.00)

**Key Methods**:
```java
@Cacheable(value = "taxRates", key = "#{tenantId}-#{taxYear}-MUNICIPAL_TAX_RATE")
BigDecimal getMunicipalTaxRate(String tenantId, int taxYear, BigDecimal defaultRate)

@Cacheable(value = "taxRates", key = "#{tenantId}-#{taxYear}-BUSINESS_MUNICIPAL_TAX_RATE")
BigDecimal getBusinessTaxRate(String tenantId, int taxYear, BigDecimal defaultRate)
```

#### Component 3: DTOs (RuleResponse, RuleValue)
**Files**: 
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/integration/dto/RuleResponse.java`
- `backend/tax-engine-service/src/main/java/com/munitax/taxengine/integration/dto/RuleValue.java`

**Responsibilities**:
- Deserialize rule-service JSON responses
- Handle polymorphic `value` field (percentage, boolean, enum, etc.)

#### Component 4: W1FilingService Refactor
**File**: `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/W1FilingService.java`

**Changes**:
- Inject `TaxRateResolverService` dependency
- Fetch dynamic rate before tax calculation:
  ```java
  BigDecimal municipalTaxRate = taxRateResolver.getMunicipalTaxRate(
      defaultTenant, 
      request.getTaxYear(), 
      fallbackMunicipalTaxRate
  );
  ```
- Log which rate is being used for transparency

#### Component 5: Application Configuration
**File**: `backend/tax-engine-service/src/main/java/com/munitax/taxengine/TaxEngineApplication.java`

**Changes**:
- Add `@EnableFeignClients` to activate Feign client scanning
- Add `@EnableCaching` to enable Spring Cache abstraction

**File**: `backend/tax-engine-service/pom.xml`

**Changes**:
- Add dependency: `spring-cloud-starter-openfeign`

### 4.4 Configuration

**application.yml** (tax-engine-service):
```yaml
spring:
  cache:
    type: simple  # or caffeine for production
    cache-names:
      - taxRates
    caffeine:
      spec: maximumSize=500,expireAfterWrite=1h

tax:
  municipal:
    rate: 0.0200  # Fallback rate
  default-tenant: dublin

eureka:
  client:
    serviceUrl:
      defaultZone: http://discovery-service:8761/eureka/

feign:
  client:
    config:
      rule-service:
        connectTimeout: 5000
        readTimeout: 10000
```

## 5. Non-Functional Requirements

### NFR-1: Performance
- **Requirement**: Tax calculations with cached rates MUST complete within 200ms (p95)
- **Measurement**: Spring Boot Actuator metrics for cache hit rate and API response time

### NFR-2: Availability
- **Requirement**: Tax-engine-service MUST remain functional when rule-service is down
- **Measurement**: Fallback mechanism tested with rule-service stopped

### NFR-3: Scalability
- **Requirement**: System MUST support 10,000 tax calculations/hour across multiple instances
- **Measurement**: Load testing with JMeter showing cache hit rate > 80%

### NFR-4: Maintainability
- **Requirement**: Adding support for new rule codes MUST require only configuration changes (no code)
- **Measurement**: TaxRateResolverService already searches for custom rule codes

## 6. Testing Strategy

### Unit Tests
- **TaxRateResolverServiceTest**: Mock RuleServiceClient, test parsing and caching
- **W1FilingServiceTest**: Mock TaxRateResolverService, test tax calculation with dynamic rates

### Integration Tests
- **RuleServiceClientTest**: Use WireMock to simulate rule-service responses
- **W1FilingIntegrationTest**: End-to-end test with TestContainers (Postgres, Eureka, rule-service)

### Manual Testing
1. Start all services: `docker-compose up -d`
2. Create NEW_LAGAAN rule with 60000% rate via rule-service API
3. File W-1 return with $1000 taxable wages
4. Verify taxDue = $600,000 (not $20)
5. Check logs for "Using tax rate 600.0 (NEW_LAGAAN) for tenant: dublin, taxYear: 2025"

## 7. Deployment Considerations

### Rollout Plan
1. **Phase 1**: Deploy tax-engine-service with integration code (backward compatible - uses fallback if rule-service unavailable)
2. **Phase 2**: Verify rule-service is healthy and Eureka registration successful
3. **Phase 3**: Monitor cache hit rate and API latency for 24 hours
4. **Phase 4**: Gradually migrate tax rate configuration from application.yml to rule-service

### Rollback Plan
- If integration fails, tax-engine-service automatically falls back to hardcoded rates
- No rollback needed - fallback mechanism ensures zero downtime

### Monitoring
- **Metrics to track**:
  - Cache hit rate (`taxRates` cache)
  - Feign client latency (rule-service calls)
  - Fallback rate usage count
  - Tax calculations per hour
- **Alerts**:
  - Cache hit rate drops below 50% (investigate cache expiration settings)
  - Feign client errors exceed 5% (check rule-service health)
  - Fallback rate used > 100 times/hour (investigate rule-service connectivity)

## 8. Dependencies

### External Services
- **rule-service**: Must be running and registered with Eureka
- **discovery-service (Eureka)**: Must be healthy for service discovery

### Libraries
- Spring Cloud OpenFeign 4.0.x
- Spring Cache (included in Spring Boot)
- Spring Cloud Netflix Eureka Client 4.0.x

### Configuration
- `application.yml` must define:
  - `tax.municipal.rate` (fallback)
  - `tax.default-tenant`
  - Eureka client settings

## 9. Future Enhancements

### FE-1: Advanced Caching Strategy
- Replace simple cache with Redis for distributed caching across multiple tax-engine instances
- Implement cache invalidation when rules are updated in rule-service

### FE-2: Circuit Breaker Pattern
- Add Resilience4j circuit breaker to prevent cascading failures when rule-service is slow/down
- Implement retry logic with exponential backoff

### FE-3: Rule Webhooks
- rule-service publishes events when rules are approved/updated
- tax-engine-service subscribes and invalidates cache proactively

### FE-4: Support for More Tax Types
- Extend TaxRateResolverService to support:
  - Business municipal tax rate
  - Withholding tax rate
  - School district tax rate
  - Special assessment rates

## 10. Risks and Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| rule-service unavailable | Tax calculations use fallback rate (potentially incorrect) | Medium | Implement fallback mechanism (✅ completed), monitor fallback usage |
| Cache invalidation issues | Stale tax rates used after rule updates | Low | Implement TTL-based cache expiration (1 hour), document cache clear procedure |
| Percentage parsing errors | Tax calculations fail or incorrect | Low | Comprehensive unit tests for percentage conversion, handle null/invalid values |
| Eureka service discovery failures | Feign client cannot locate rule-service | Low | Configure static fallback URL in application.yml if Eureka unavailable |

## 11. Success Criteria

✅ **Achieved**:
- [x] Tax-engine-service successfully calls rule-service via Feign client
- [x] Dynamic tax rates applied in W-1 filing calculations
- [x] Caching implemented with @Cacheable annotations
- [x] Fallback mechanism tested (rule-service stopped)
- [x] Logging shows which rule/rate is used

🔄 **Pending Verification**:
- [ ] End-to-end test with NEW_LAGAAN 60000% rule
- [ ] Cache hit rate monitored over 24 hours
- [ ] Load testing confirms 10,000 calculations/hour capacity

## 12. Appendices

### Appendix A: Rule-Service API Contract

**Endpoint**: `GET /api/rules/active`

**Query Parameters**:
- `tenantId` (String, required): Tenant identifier (e.g., "dublin")
- `taxYear` (int, required): Tax year for rule filtering (e.g., 2025)
- `entityType` (String, required): Entity type filter (e.g., "INDIVIDUAL", "BUSINESS")

**Response**: Array of RuleResponse objects
```json
[
  {
    "ruleId": "123e4567-e89b-12d3-a456-426614174000",
    "ruleCode": "NEW_LAGAAN",
    "ruleName": "New Municipal Tax Rate",
    "category": "TAX_RATE",
    "valueType": "PERCENTAGE",
    "value": {
      "scalar": 60000,
      "unit": "PERCENT"
    },
    "effectiveDate": "2025-01-01",
    "endDate": null,
    "tenantId": "dublin",
    "entityTypes": ["INDIVIDUAL"],
    "approvalStatus": "APPROVED"
  }
]
```

### Appendix B: Percentage to Decimal Conversion

**Formula**: `decimalRate = percentageValue / 100.0`

**Examples**:
- 2% (0.02 decimal): `2 / 100.0 = 0.02`
- 60000% (600.00 decimal): `60000 / 100.0 = 600.00`
- 0.5% (0.005 decimal): `0.5 / 100.0 = 0.005`

**Tax Calculation**: `taxDue = taxableWages × decimalRate`

**Example with NEW_LAGAAN**:
- Taxable Wages: $1000.00
- NEW_LAGAAN Rate: 60000% → 600.00 decimal
- Tax Due: $1000.00 × 600.00 = $600,000.00

---

**Document Version**: 1.0  
**Last Updated**: 2025-12-03 by Integration Implementation Team
