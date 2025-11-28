# Research: Dynamic Rule Configuration System

**Date**: 2025-11-28  
**Phase**: 0 - Outline & Research  
**Status**: Complete

## Overview

This document consolidates research findings for technical decisions related to the Dynamic Rule Configuration System. All "NEEDS CLARIFICATION" items from Technical Context have been resolved through research of Spring Boot best practices, PostgreSQL jsonb capabilities, Redis caching patterns, and React admin dashboard design.

---

## Research Tasks

### 1. PostgreSQL jsonb Storage for Rule Values

**Question**: How to store heterogeneous rule values (numbers, strings, formulas, conditionals) in PostgreSQL while maintaining queryability?

**Decision**: Use PostgreSQL `jsonb` column for `TaxRule.value` field with hybrid schema design.

**Rationale**:
- **Flexibility**: jsonb supports arbitrary JSON structure (number, string, object, array) without schema migrations
- **Queryability**: PostgreSQL provides jsonb operators (`->`, `->>`, `@>`) for indexing and filtering specific rule types
- **Type Safety**: Application layer validates value structure based on `value_type` enum before saving
- **Performance**: jsonb uses binary format (faster than text JSON), supports GIN indexes for subset queries

**Schema Design**:
```sql
CREATE TABLE tax_rules (
    rule_id UUID PRIMARY KEY,
    rule_code VARCHAR(100) NOT NULL,
    value_type VARCHAR(50) NOT NULL, -- NUMBER, PERCENTAGE, FORMULA, CONDITIONAL, ENUM
    value JSONB NOT NULL,            -- Flexible storage
    -- Examples:
    -- NUMBER: {"scalar": 0.02}
    -- PERCENTAGE: {"scalar": 50}
    -- FORMULA: {"expression": "wages * municipalRate", "variables": ["wages", "municipalRate"]}
    -- CONDITIONAL: {"if": "income > 1000000", "then": 5000, "else": 50}
    -- ENUM: {"option": "BOX_5_MEDICARE"}
    ...
);

-- Index for common queries
CREATE INDEX idx_rules_tenant_year ON tax_rules(tenant_id, effective_date, end_date) 
WHERE approval_status = 'APPROVED';

-- GIN index for jsonb queries (optional, for advanced filtering)
CREATE INDEX idx_rules_value_gin ON tax_rules USING GIN (value jsonb_path_ops);
```

**Validation Strategy**:
- Define JSON schemas for each `value_type` (using Jackson annotations in Java)
- Validate value structure before save: `if (valueType == NUMBER) { assert value.has("scalar") && value.get("scalar").isNumber() }`
- Reject invalid values at API layer, not database layer (better error messages)

**Alternatives Considered**:
1. **Separate columns per rule type**: `numeric_value`, `string_value`, `formula_value`
   - **Rejected**: Complex schema, many nullable columns, harder to add new types
2. **EAV (Entity-Attribute-Value) pattern**: Separate rows for each rule attribute
   - **Rejected**: Poor query performance, complex joins, loses ACID guarantees
3. **Dedicated rule_values table**: Foreign key to tax_rules with polymorphic storage
   - **Rejected**: Over-engineering for current requirements, adds join overhead

**References**:
- PostgreSQL jsonb documentation: https://www.postgresql.org/docs/16/datatype-json.html
- Spring Data JPA jsonb mapping: Use `@Type(JsonBinaryType.class)` from Hibernate Types library

---

### 2. Redis Caching Strategy for Rule Retrieval

**Question**: How to cache rules in Redis to achieve <100ms retrieval performance while maintaining consistency across rule updates?

**Decision**: Cache complete rule sets per tenant+taxYear with TTL-based expiration and manual invalidation on rule changes.

**Rationale**:
- **Performance**: Redis GET operation ~1ms vs PostgreSQL query ~50-100ms (50x faster)
- **Access Pattern**: Tax calculations query same rule set repeatedly (batch processing), high cache hit ratio expected
- **Consistency**: Cache invalidation on rule publish ensures new calculations use updated rules
- **Simplicity**: Avoid complex distributed cache invalidation protocols

**Cache Key Structure**:
```
rules:{tenantId}:{taxYear}:{entityType}
Example: rules:dublin:2025:INDIVIDUAL
Value: JSON-serialized TaxRulesConfig object
TTL: 24 hours (expire daily, refresh from DB)
```

**Cache Lifecycle**:
1. **Cache Miss**: 
   - Query PostgreSQL: `SELECT * FROM tax_rules WHERE tenant_id=? AND effective_date <= ? AND (end_date IS NULL OR end_date >= ?) AND approval_status='APPROVED'`
   - Serialize to JSON
   - Store in Redis with TTL=24h
   - Return to caller

2. **Cache Hit**: 
   - Deserialize JSON from Redis
   - Return to caller (no DB query)

3. **Cache Invalidation** (on rule publish/update):
   - Delete all keys matching pattern: `rules:{tenantId}:*` (affects all tax years for tenant)
   - Next request triggers cache miss → rebuild from DB
   - Alternative: Delete specific key `rules:{tenantId}:{taxYear}:*` if effective date known

**Spring Boot Implementation**:
```java
@Service
public class RuleCacheService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public TaxRulesConfig getRules(String tenantId, int taxYear, EntityType entityType) {
        String key = String.format("rules:%s:%d:%s", tenantId, taxYear, entityType);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return objectMapper.readValue(cached, TaxRulesConfig.class);
        }
        // Cache miss - query DB
        TaxRulesConfig rules = queryDatabaseForRules(tenantId, taxYear, entityType);
        String json = objectMapper.writeValueAsString(rules);
        redisTemplate.opsForValue().set(key, json, Duration.ofHours(24));
        return rules;
    }
    
    public void invalidateCache(String tenantId) {
        // Delete all rule caches for tenant
        Set<String> keys = redisTemplate.keys("rules:" + tenantId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

**Alternatives Considered**:
1. **Application-level cache (Caffeine, Guava)**: In-memory cache within rule-service JVM
   - **Rejected**: Doesn't scale across multiple instances, no shared state
2. **Database query cache**: Rely on PostgreSQL query cache
   - **Rejected**: Not persistent across restarts, slower than Redis
3. **Event-driven invalidation**: Publish cache invalidation events via RabbitMQ
   - **Deferred**: Over-engineering for v1, acceptable to use simple TTL + manual invalidation

**Cache Warming Strategy** (optional for v2):
- Preload commonly accessed rule sets on service startup
- Background job refreshes cache nightly (before business hours)

---

### 3. Formula Evaluation Engine Selection

**Question**: How to safely parse and evaluate formula rules (e.g., "wages * municipalRate") without introducing code injection vulnerabilities?

**Decision**: Use **Spring Expression Language (SpEL)** with restricted evaluation context (whitelist variables, no method calls).

**Rationale**:
- **Native Spring Integration**: SpEL built into Spring Framework, no external dependencies
- **Type Safety**: Compile-time expression parsing catches syntax errors
- **Security**: Can disable method invocation, limit to variable references and arithmetic operators only
- **Expressiveness**: Supports basic math (`+`, `-`, `*`, `/`), comparisons (`>`, `<`, `==`), conditionals (`?:`)

**Implementation**:
```java
@Service
public class FormulaEvaluationService {
    private final SpelExpressionParser parser = new SpelExpressionParser();
    
    public double evaluateFormula(String formula, Map<String, Object> variables) {
        try {
            // Create secure evaluation context
            StandardEvaluationContext context = new StandardEvaluationContext();
            // SECURITY: Disable method invocation to prevent code execution
            context.setMethodResolvers(Collections.emptyList());
            // Whitelist allowed variables
            variables.forEach(context::setVariable);
            
            Expression expr = parser.parseExpression(formula);
            Object result = expr.getValue(context);
            
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
            throw new IllegalArgumentException("Formula must evaluate to number");
        } catch (Exception e) {
            throw new FormulaEvaluationException("Invalid formula: " + formula, e);
        }
    }
    
    public void validateFormula(String formula, Set<String> allowedVariables) {
        // Parse formula to check syntax
        Expression expr = parser.parseExpression(formula);
        // Extract variable names from AST
        Set<String> usedVariables = extractVariables(expr);
        // Verify all variables are in allowed set
        usedVariables.forEach(var -> {
            if (!allowedVariables.contains(var)) {
                throw new IllegalArgumentException("Unknown variable: " + var);
            }
        });
    }
}
```

**Example Formulas**:
```java
// Simple multiplication
"wages * municipalRate"  → evaluates to 50000 * 0.02 = 1000

// Maximum function
"T.max(minimumTax, income * rate)"  → evaluates to max(50, 45000 * 0.02) = 900

// Conditional (ternary operator)
"income > 1000000 ? 5000 : 50"  → evaluates to 5000 if income > 1M, else 50

// Nested expressions
"(wages + bonuses) * municipalRate * (1 - creditRate)"
```

**Security Constraints**:
- **No method calls**: Disallow `someString.toUpperCase()`, `Math.random()`, etc.
- **Whitelist variables**: Only allow pre-defined variable names (wages, income, municipalRate, etc.)
- **No external references**: No access to System properties, environment variables, file system
- **Sandboxed evaluation**: Expression cannot escape evaluation context

**Alternatives Considered**:
1. **JEP (Java Expression Parser)**: Third-party library for math expressions
   - **Rejected**: Another dependency, SpEL already available, less Spring-idiomatic
2. **MVEL (MVFLEX Expression Language)**: More powerful than SpEL
   - **Rejected**: Security concerns (harder to sandbox), overkill for simple math
3. **Custom DSL**: Build domain-specific language for tax rules
   - **Rejected**: Significant engineering effort, harder to debug, less flexible than SpEL
4. **JavaScript engine (Nashorn/GraalVM)**: Evaluate JS expressions
   - **Rejected**: Security nightmare (code injection), performance overhead, deprecated in Java 11+

**Validation Strategy**:
- Admin UI shows formula preview: "If wages=$50,000 and municipalRate=0.02, result = $1,000"
- Unit tests for common formula patterns (arithmetic, conditionals, edge cases)
- Syntax validation on form submit (before saving to database)

---

### 4. React Admin Dashboard Component Architecture

**Question**: How to structure React components for rule configuration UI to support reusability, testability, and maintainability?

**Decision**: Use **compound component pattern** with React Context for shared state, React Hook Form for validation, and Tailwind CSS for styling.

**Rationale**:
- **Compound Components**: Logical grouping (RuleEditor wraps TemporalRuleEditor, FormulaBuilder) without prop drilling
- **React Hook Form**: Declarative validation, async validation (check overlapping rules), better performance than controlled inputs
- **Context for Shared State**: Avoid passing tenant/user context through every component
- **Tailwind CSS**: Already used in project, consistent styling, responsive utilities

**Component Hierarchy**:
```
RuleConfigurationDashboard (main page)
├── RuleStatsCards (summary cards: active rules, pending approvals, recent changes)
├── RuleList (table with filters)
│   ├── RuleListFilters (category, tenant, date range dropdowns)
│   ├── RuleListTable (paginated table with sort)
│   └── RuleListRow (single row with edit/delete/history buttons)
└── RuleEditorModal (modal dialog for create/edit)
    ├── RuleBasicInfo (name, category, entity type)
    ├── RuleValueEditor (conditional rendering based on valueType)
    │   ├── NumberInput (for NUMBER type)
    │   ├── PercentageInput (for PERCENTAGE type)
    │   ├── EnumSelect (for ENUM type)
    │   └── FormulaBuilder (for FORMULA type)
    ├── TemporalRuleEditor (effective date, end date, overlap detection)
    ├── RuleApprovalSection (approval status, reason for change)
    └── WhatIfAnalysisPreview (impact preview before save)

Separate pages:
- RuleHistoryViewer (timeline of versions)
- WhatIfAnalysisTool (full-page tool for testing rules)
- TenantComparisonView (side-by-side comparison)
```

**State Management**:
```typescript
// Use React Context for global admin state
interface AdminContext {
  currentTenant: Tenant;
  currentUser: User;
  permissions: string[];
}

// Use React Hook Form for form state
const RuleEditorForm: React.FC = () => {
  const { register, handleSubmit, watch, formState: { errors } } = useForm<RuleFormData>({
    defaultValues: { /* ... */ },
    resolver: yupResolver(ruleValidationSchema)
  });
  
  const valueType = watch("valueType"); // Re-render when type changes
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <select {...register("valueType")}>...</select>
      
      {/* Conditional rendering based on valueType */}
      {valueType === "NUMBER" && <NumberInput {...register("value.scalar")} />}
      {valueType === "FORMULA" && <FormulaBuilder {...register("value.expression")} />}
      
      <TemporalRuleEditor 
        effectiveDate={register("effectiveDate")}
        endDate={register("endDate")}
        onOverlapDetected={(overlapping) => setError("effectiveDate", {...})}
      />
    </form>
  );
};
```

**Validation Strategy**:
- **Client-side**: Yup schema validation (required fields, date ranges, formula syntax)
- **Async validation**: Check overlapping rules via API call (debounced 500ms)
- **Server-side**: Backend validates again (never trust client), returns validation errors if invalid

**Reusable Patterns**:
- `TemporalRuleEditor` reusable across create/edit/clone scenarios
- `RuleValueEditor` polymorphic component (render different input based on type)
- `WhatIfAnalysisPreview` embeddable in editor modal or standalone page

**Alternatives Considered**:
1. **Redux for state management**: Centralized store for admin state
   - **Deferred**: Overkill for admin tool, React Context sufficient for v1
2. **Formik instead of React Hook Form**: Alternative form library
   - **Rejected**: React Hook Form better performance (uncontrolled inputs), more idiomatic with hooks
3. **Material-UI components**: Pre-built component library
   - **Rejected**: Project uses Tailwind, adding Material-UI increases bundle size, inconsistent design

---

### 5. Temporal Rule Query Optimization

**Question**: How to efficiently query rules active for specific tax year without full table scan?

**Decision**: Use **composite index** on `(tenant_id, effective_date, end_date)` with partial index on `approval_status='APPROVED'`.

**Rationale**:
- **Common Query Pattern**: `WHERE tenant_id = ? AND effective_date <= ? AND (end_date >= ? OR end_date IS NULL) AND approval_status = 'APPROVED'`
- **Index Selectivity**: tenant_id narrows search space significantly (10-20 tenants), date range further filters
- **Partial Index**: Only index approved rules (95% of queries), exclude pending/rejected (reduces index size)

**Index Definition**:
```sql
-- Primary temporal query index
CREATE INDEX idx_rules_temporal_query ON tax_rules(tenant_id, effective_date, end_date)
WHERE approval_status = 'APPROVED';

-- Alternative: Include approval_status in composite (if filtering by status common)
CREATE INDEX idx_rules_temporal_query_v2 ON tax_rules(tenant_id, approval_status, effective_date, end_date);

-- Covering index (optional): Include rule_code, value to avoid table lookup
CREATE INDEX idx_rules_covering ON tax_rules(tenant_id, effective_date, end_date)
INCLUDE (rule_code, value_type, value, entity_types)
WHERE approval_status = 'APPROVED';
```

**Query Plan Verification**:
```sql
EXPLAIN ANALYZE
SELECT rule_id, rule_code, value_type, value, entity_types
FROM tax_rules
WHERE tenant_id = 'dublin'
  AND effective_date <= '2025-12-31'
  AND (end_date >= '2025-01-01' OR end_date IS NULL)
  AND approval_status = 'APPROVED';

-- Expected: Index Scan using idx_rules_temporal_query (cost=0..8 rows=10)
-- NOT: Seq Scan on tax_rules (cost=0..1000 rows=10000)
```

**Performance Characteristics**:
- **Without index**: Sequential scan (~100ms for 10,000 rules)
- **With index**: Index scan (~5ms for typical query returning 50 rules)
- **Cache hit**: Redis cache bypass database entirely (~1ms)

**Alternatives Considered**:
1. **Bitemporal tables**: Separate system-time and valid-time dimensions
   - **Rejected**: Over-engineering, PostgreSQL temporal tables feature is preview (not production-ready)
2. **Materialized view**: Pre-compute active rules per tenant+year
   - **Rejected**: Adds complexity (refresh strategy), doesn't handle mid-year changes gracefully
3. **Partition by tenant**: Separate table per tenant
   - **Rejected**: Violates multi-tenant schema design, complicates cross-tenant queries

---

### 6. Approval Workflow Implementation

**Question**: Should approval workflow be synchronous (block save until approved) or asynchronous (save as pending, approve later)?

**Decision**: **Asynchronous approval** - rules saved with `approval_status='PENDING'`, requires separate approval action.

**Rationale**:
- **Separation of Concerns**: Rule creator (junior admin) can draft rules without publishing
- **Review Process**: Senior admin reviews pending rules, can request changes or reject
- **Audit Trail**: Captures approval decision (approved_by, approval_date, approval_reason)
- **Safety**: Prevents accidental publication of incorrect rules

**Workflow States**:
```
CREATE → PENDING → APPROVED → ACTIVE (when effective_date reached)
         ├→ REJECTED (cannot be modified, must create new rule)
         └→ VOIDED (admin withdrew before effective date)

State Transitions:
1. Admin creates rule → status=PENDING, effective_date=future
2. Approver reviews → status=APPROVED (or REJECTED with reason)
3. System activates → When current_date >= effective_date, rule becomes active
4. System expires → When current_date > end_date, rule becomes inactive (but still APPROVED in DB)
```

**API Endpoints**:
```
POST   /api/rules                    # Create rule (status=PENDING)
PUT    /api/rules/{id}               # Update rule (only if status=PENDING)
POST   /api/rules/{id}/approve       # Approve rule (requires TAX_ADMINISTRATOR role)
POST   /api/rules/{id}/reject        # Reject rule (requires TAX_ADMINISTRATOR role)
POST   /api/rules/{id}/void          # Void rule (before effective date)
DELETE /api/rules/{id}               # Soft delete (sets status=VOIDED)
```

**Authorization Rules**:
- Any TAX_ADMINISTRATOR can create/edit PENDING rules
- Only approver (different user than creator) can approve
- Cannot approve own rules (prevents single-user fraud)
- Cannot modify APPROVED rules (must create new version with future effective date)

**UI Workflow**:
1. Admin creates rule in RuleEditor → Saves as PENDING
2. Dashboard shows "Pending Approval" section with count badge
3. Approver clicks "Review" → Modal shows rule details + diff (if update)
4. Approver clicks "Approve" or "Reject" with reason
5. On approve → Cache invalidation triggered, rule active on effective date

**Alternatives Considered**:
1. **Synchronous approval**: Rule immediately active on save
   - **Rejected**: No review process, higher risk of errors
2. **Multi-step approval**: Submitter → Reviewer → Final Approver
   - **Deferred**: Over-engineering for v1, single approval sufficient
3. **External approval system**: Integrate with Jira/ServiceNow
   - **Deferred**: Future enhancement, in-app workflow sufficient for MVP

---

## Best Practices Research

### Spring Boot Microservices

**Service Registration**:
- rule-service registers with Eureka discovery service on startup
- tax-engine-service discovers rule-service via Eureka client
- Use `@EnableDiscoveryClient` and `@LoadBalanced RestTemplate`

**Circuit Breaker**:
- Implement Resilience4j circuit breaker on tax-engine → rule-service calls
- Fallback: Use cached rules if rule-service unavailable
- Prevents cascade failures during rule-service downtime

**Health Checks**:
- Expose Spring Boot Actuator endpoints: `/actuator/health`, `/actuator/metrics`
- Include database connectivity check, Redis connectivity check
- Kubernetes liveness/readiness probes use actuator endpoints

### PostgreSQL Multi-Tenant Best Practices

**Row-Level Security (RLS)**:
```sql
-- Enable RLS on tax_rules table
ALTER TABLE tax_rules ENABLE ROW LEVEL SECURITY;

-- Create policy: users can only see their tenant's rules
CREATE POLICY tenant_isolation ON tax_rules
FOR ALL
USING (tenant_id = current_setting('app.tenant_id', true));

-- Set tenant context in application
-- In Spring Boot: Use Hibernate Filter or set session variable
@Transactional
public void setTenantContext(String tenantId) {
    entityManager.createNativeQuery("SET app.tenant_id = :tenantId")
        .setParameter("tenantId", tenantId)
        .executeUpdate();
}
```

**Connection Pooling**:
- Use HikariCP (Spring Boot default) with appropriate pool size
- Recommended: `maximumPoolSize = (core_count * 2) + effective_spindle_count`
- For admin service (low traffic): 5-10 connections sufficient

### Redis Caching Best Practices

**Serialization**:
- Use JSON serialization for human-readable values (debugging)
- Alternative: Use Protobuf/MessagePack for smaller payload (10-30% size reduction)
- Trade-off: JSON easier to debug in Redis CLI

**Key Expiration**:
- Set TTL on all keys (avoid memory leaks if invalidation fails)
- Use sliding expiration: Refresh TTL on cache hit (keeps hot data cached)
- Implement cache warming: Preload common rules on service startup

**Cache Stampede Prevention**:
- Use `SETNX` (set if not exists) to ensure only one thread rebuilds cache on miss
- Implement backoff retry if cache rebuild fails
- Extend TTL slightly before expiration to avoid thundering herd

### React Testing Best Practices

**Component Tests**:
```typescript
// Example: Test RuleEditor validation
test('shows error when effective date overlaps existing rule', async () => {
  const existingRules = [
    { effectiveDate: '2025-01-01', endDate: '2025-12-31', ruleCode: 'MUNICIPAL_RATE' }
  ];
  
  render(<RuleEditor existingRules={existingRules} />);
  
  // Enter overlapping date
  await userEvent.type(screen.getByLabelText('Effective Date'), '2025-06-01');
  await userEvent.click(screen.getByText('Save'));
  
  // Expect validation error
  expect(screen.getByText(/overlaps with existing rule/i)).toBeInTheDocument();
});
```

**API Mocking**:
- Use MSW (Mock Service Worker) to intercept API calls in tests
- Define mock handlers for rule-service endpoints
- Verify correct API calls with proper tenant context

---

## Technology Decisions Summary

| Decision Area | Technology Choice | Justification |
|--------------|-------------------|---------------|
| Rule Storage | PostgreSQL jsonb | Flexible schema, queryable, ACID guarantees |
| Caching Layer | Redis | Sub-millisecond reads, distributed cache, TTL support |
| Formula Engine | Spring Expression Language (SpEL) | Native Spring, secure, expressive enough for tax formulas |
| API Design | RESTful with OpenAPI spec | Standardized, discoverable, client generation |
| Frontend Forms | React Hook Form + Yup | Performance, async validation, declarative |
| Styling | Tailwind CSS | Already used in project, responsive utilities |
| Testing | JUnit 5 + Testcontainers | Realistic integration tests with real database |
| Approval Workflow | Asynchronous (PENDING → APPROVED) | Safer, supports review process, audit trail |

---

## Risks & Mitigations

### Risk 1: Formula Evaluation Performance Bottleneck

**Risk**: Evaluating complex formulas for every tax calculation could slow down batch processing.

**Mitigation**: 
- Cache evaluated formula results if inputs repeat (unlikely in tax context)
- Limit formula complexity (max 10 operators, max 5 variables)
- Consider compiling frequently-used formulas to bytecode (Janino library) if SpEL too slow

### Risk 2: Cache Invalidation Race Condition

**Risk**: Tax calculation reads stale cache between rule update and cache invalidation.

**Mitigation**:
- Invalidate cache BEFORE committing rule change to database (cache-aside pattern)
- Use versioned cache keys: `rules:{tenantId}:{taxYear}:v{version}` (increment version on change)
- Accept eventual consistency (admin publishes rule, takes 1-2 seconds to propagate)

### Risk 3: Temporal Query Edge Cases

**Risk**: Mid-year rule changes, leap years, timezone handling complicate date logic.

**Mitigation**:
- Store all dates in UTC, convert to tenant timezone for display only
- Use inclusive date ranges: `effectiveDate <= calculationDate <= endDate`
- Comprehensive unit tests for edge cases: leap years, DST transitions, year boundaries

### Risk 4: Migration of Existing Hardcoded Rules

**Risk**: One-time migration from constants.ts/Java to database could introduce calculation differences.

**Mitigation**:
- Automated tests: Calculate 100 sample returns with old code vs new code, assert identical results
- Shadow mode: Run both implementations in parallel, log differences (before full cutover)
- Rollback plan: Keep constants.ts/Java code temporarily, feature flag to switch back if issues

---

## Phase 0 Completion Checklist

- [x] PostgreSQL jsonb storage design finalized
- [x] Redis caching strategy defined
- [x] Formula evaluation engine selected (SpEL)
- [x] React component architecture designed
- [x] Temporal query optimization strategy documented
- [x] Approval workflow states and transitions specified
- [x] Best practices research completed (Spring Boot, PostgreSQL, Redis, React)
- [x] Risks identified and mitigations planned

**Next Phase**: Phase 1 - Design & Contracts (data-model.md, contracts/, quickstart.md)
