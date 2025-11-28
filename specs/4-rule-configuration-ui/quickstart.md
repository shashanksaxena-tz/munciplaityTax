# Quickstart Guide: Dynamic Rule Configuration System

**Version**: 1.0.0  
**Date**: 2025-11-28  
**Audience**: Backend/Frontend developers implementing the feature

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Architecture Diagram](#architecture-diagram)
4. [Backend Setup](#backend-setup)
5. [Frontend Setup](#frontend-setup)
6. [Database Setup](#database-setup)
7. [Testing Guide](#testing-guide)
8. [Common Workflows](#common-workflows)
9. [Troubleshooting](#troubleshooting)

---

## Overview

This feature replaces hardcoded tax rules in `constants.ts` and Java calculators with a dynamic rule configuration system. Tax administrators can configure rules via a React admin UI, with rules stored in PostgreSQL and cached in Redis for performance.

**Key Components**:
- `rule-service`: New Spring Boot microservice managing TaxRule entities
- `tax-engine-service`: Modified to query rules via REST API instead of using constants
- React Admin UI: RuleConfigurationDashboard and related components
- PostgreSQL: Rule storage with temporal queries and version history
- Redis: Caching layer for <100ms rule retrieval

**Expected Timeline**: 
- Backend: 2-3 weeks (new microservice + calculator refactoring)
- Frontend: 1-2 weeks (admin UI components)
- Testing: 1 week (integration tests, migration verification)
- Total: 4-6 weeks for full implementation

---

## Prerequisites

### Required Software

- **Java 21**: `java -version` should show 21.x
- **Maven 3.9+**: `mvn -v`
- **Node.js 18+**: `node -v` should show 18.x or higher
- **PostgreSQL 16+**: `psql --version`
- **Redis 7+**: `redis-cli --version`
- **Docker** (optional, for local Redis/PostgreSQL): `docker --version`

### Required Knowledge

- Spring Boot 3.x (REST APIs, JPA, Spring Security)
- React 18 with TypeScript
- PostgreSQL (SQL, jsonb, indexes)
- Redis (basic commands: GET, SET, DELETE)
- REST API design

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                        │
│  ┌───────────────────────┐    ┌──────────────────────────┐    │
│  │ RuleConfiguration     │    │ Tax Filing Workflows     │    │
│  │ Dashboard (Admin)     │    │ (Individual/Business)    │    │
│  └───────────┬───────────┘    └──────────┬───────────────┘    │
└──────────────┼─────────────────────────────┼────────────────────┘
               │                             │
               │ HTTPS                       │ HTTPS
               ▼                             ▼
┌──────────────────────────────────────────────────────────────────┐
│                      Gateway Service (Port 8080)                 │
│                   Routes: /rules/*, /tax-engine/*                │
└──────────────┬────────────────────────────────┬──────────────────┘
               │                                 │
               │ HTTP (Service Mesh)             │ HTTP
               ▼                                 ▼
┌──────────────────────────┐      ┌──────────────────────────────┐
│   rule-service (8084)    │      │  tax-engine-service (8082)   │
│                          │      │                              │
│  ┌────────────────────┐  │      │  ┌────────────────────────┐ │
│  │ RuleConfigController│  │      │  │ IndividualTaxCalc     │ │
│  │ RuleQueryController │  │      │  │ BusinessTaxCalc       │ │
│  │ RuleHistoryController│ │      │  │ RuleServiceClient ────┼─┼──┐
│  └────────┬────────────┘  │      │  └───────────────────────┘ │  │
│           │               │      └────────────────────────────┬┘  │
│           ▼               │                                   │   │
│  ┌────────────────────┐  │                                   │   │
│  │ RuleCacheService   │  │                                   │   │
│  │ (Redis Client)     │  │                                   │   │
│  └────────┬───────────┘  │                                   │   │
│           │               │                                   │   │
└───────────┼───────────────┘                                   │   │
            │                   ┌───────────────────────────────┘   │
            │                   │                                   │
            ▼                   ▼                                   │
┌──────────────────┐   ┌────────────────────────┐                 │
│  Redis (6379)    │   │  PostgreSQL (5432)     │                 │
│                  │   │                        │◄────────────────┘
│  Cache Layer     │   │  Database:             │
│  TTL: 24h        │   │  - tax_rules           │
│                  │   │  - rule_change_log     │
└──────────────────┘   └────────────────────────┘

Legend:
→ HTTP REST API call
⇢ Database query
```

---

## Backend Setup

### Step 1: Create rule-service Microservice

```bash
cd backend/

# Generate Spring Boot project structure
mvn archetype:generate \
  -DgroupId=com.munitax.rules \
  -DartifactId=rule-service \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false

cd rule-service
```

### Step 2: Configure pom.xml

Add dependencies to `backend/rule-service/pom.xml`:

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    
    <!-- JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>
    
    <!-- Hibernate JSON support -->
    <dependency>
        <groupId>io.hypersistence</groupId>
        <artifactId>hypersistence-utils-hibernate-60</artifactId>
        <version>3.5.1</version>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Step 3: Configure application.yml

Create `backend/rule-service/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: rule-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/munitax
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: none  # Use Flyway for migrations
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false
  
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8

server:
  port: 8084

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### Step 4: Create Database Migrations

Create Flyway migration files in `backend/rule-service/src/main/resources/db/migration/`:

**V1__create_tax_rules_table.sql**:
```sql
CREATE TABLE tax_rules (
    rule_id UUID PRIMARY KEY,
    rule_code VARCHAR(100) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    value_type VARCHAR(50) NOT NULL,
    value JSONB NOT NULL,
    effective_date DATE NOT NULL,
    end_date DATE,
    tenant_id VARCHAR(50) NOT NULL,
    entity_types VARCHAR[] NOT NULL DEFAULT ARRAY['ALL'],
    applies_to TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    previous_version_id UUID REFERENCES tax_rules(rule_id),
    depends_on UUID[],
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by VARCHAR(100),
    approval_date TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100),
    modified_date TIMESTAMP,
    change_reason TEXT NOT NULL,
    ordinance_reference TEXT,
    
    CONSTRAINT chk_category CHECK (category IN ('TaxRates', 'IncomeInclusion', 'Deductions', 'Penalties', 'Filing', 'Allocation', 'Withholding', 'Validation')),
    CONSTRAINT chk_value_type CHECK (value_type IN ('NUMBER', 'PERCENTAGE', 'ENUM', 'BOOLEAN', 'FORMULA', 'CONDITIONAL')),
    CONSTRAINT chk_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'VOIDED')),
    CONSTRAINT chk_date_range CHECK (end_date IS NULL OR effective_date <= end_date),
    CONSTRAINT chk_approval_complete CHECK (
        (approval_status = 'APPROVED' AND approved_by IS NOT NULL AND approval_date IS NOT NULL) OR
        (approval_status != 'APPROVED')
    )
);

-- Indexes
CREATE INDEX idx_tax_rules_temporal ON tax_rules(tenant_id, effective_date, end_date)
WHERE approval_status = 'APPROVED';

CREATE INDEX idx_tax_rules_code ON tax_rules(rule_code, tenant_id, approval_status);

CREATE INDEX idx_tax_rules_approval ON tax_rules(approval_status, created_date);

CREATE INDEX idx_tax_rules_version_chain ON tax_rules(previous_version_id)
WHERE previous_version_id IS NOT NULL;
```

**V2__create_rule_change_log_table.sql**:
```sql
CREATE TABLE rule_change_log (
    log_id UUID PRIMARY KEY,
    rule_id UUID NOT NULL REFERENCES tax_rules(rule_id),
    change_type VARCHAR(20) NOT NULL,
    old_value JSONB,
    new_value JSONB NOT NULL,
    changed_fields VARCHAR[] NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    change_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_reason TEXT NOT NULL,
    affected_returns_count INTEGER DEFAULT 0,
    impact_estimate JSONB,
    
    CONSTRAINT chk_change_type CHECK (change_type IN ('CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT', 'VOID', 'ROLLBACK'))
);

-- Indexes
CREATE INDEX idx_rule_change_log_rule ON rule_change_log(rule_id, change_date DESC);
CREATE INDEX idx_rule_change_log_date ON rule_change_log(change_date DESC);
CREATE INDEX idx_rule_change_log_user ON rule_change_log(changed_by, change_date DESC);

-- Trigger to prevent updates/deletes (append-only)
CREATE OR REPLACE FUNCTION prevent_rule_change_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'rule_change_log is append-only. UPDATE and DELETE are not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER no_update_rule_change_log
BEFORE UPDATE OR DELETE ON rule_change_log
FOR EACH ROW EXECUTE FUNCTION prevent_rule_change_log_modification();
```

**V3__create_tenant_rls_policies.sql**:
```sql
-- Enable Row-Level Security
ALTER TABLE tax_rules ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only access their tenant's rules (or GLOBAL rules)
CREATE POLICY tenant_isolation ON tax_rules
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true) OR 
    tenant_id = 'GLOBAL'
);

-- Policy: Only TAX_ADMINISTRATOR role can modify rules
CREATE POLICY admin_write_only ON tax_rules
FOR INSERT, UPDATE, DELETE
USING (current_setting('app.user_role', true) = 'TAX_ADMINISTRATOR');
```

### Step 5: Run Database Migrations

```bash
cd backend/rule-service
mvn flyway:migrate

# Verify tables created
psql -U postgres -d munitax -c "\dt"
# Should show: tax_rules, rule_change_log, flyway_schema_history
```

### Step 6: Implement Core Services

**TaxRule.java** entity (see data-model.md for full implementation)

**RuleManagementService.java**:
```java
@Service
@Transactional
public class RuleManagementService {
    
    @Autowired
    private TaxRuleRepository ruleRepository;
    
    @Autowired
    private RuleValidationService validationService;
    
    @Autowired
    private RuleCacheService cacheService;
    
    @Autowired
    private RuleChangeLogRepository changeLogRepository;
    
    public TaxRule createRule(CreateRuleRequest request, String createdBy) {
        // Validate no overlapping rules
        validationService.validateNoOverlap(
            request.getRuleCode(), 
            request.getTenantId(), 
            request.getEffectiveDate(), 
            request.getEndDate()
        );
        
        // Create rule entity
        TaxRule rule = new TaxRule();
        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setCategory(request.getCategory());
        rule.setValueType(request.getValueType());
        rule.setValue(request.getValue());
        rule.setEffectiveDate(request.getEffectiveDate());
        rule.setEndDate(request.getEndDate());
        rule.setTenantId(request.getTenantId());
        rule.setEntityTypes(request.getEntityTypes());
        rule.setApprovalStatus(ApprovalStatus.PENDING);
        rule.setCreatedBy(createdBy);
        rule.setChangeReason(request.getChangeReason());
        
        // Save to database
        rule = ruleRepository.save(rule);
        
        // Log change
        logRuleChange(rule, ChangeType.CREATE, null, rule);
        
        return rule;
    }
    
    public TaxRule approveRule(UUID ruleId, String approvedBy, String approvalReason) {
        TaxRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new RuleNotFoundException(ruleId));
        
        // Validate approval (cannot approve own rule)
        if (rule.getCreatedBy().equals(approvedBy)) {
            throw new InvalidApprovalException("Cannot approve your own rule");
        }
        
        // Invalidate cache FIRST (before database commit)
        cacheService.invalidateTenantCache(rule.getTenantId());
        
        // Update rule
        Object oldValue = rule.getValue();
        rule.setApprovalStatus(ApprovalStatus.APPROVED);
        rule.setApprovedBy(approvedBy);
        rule.setApprovalDate(LocalDateTime.now());
        rule = ruleRepository.save(rule);
        
        // Log approval
        logRuleChange(rule, ChangeType.APPROVE, oldValue, rule.getValue());
        
        return rule;
    }
    
    private void logRuleChange(TaxRule rule, ChangeType changeType, Object oldValue, Object newValue) {
        RuleChangeLog log = new RuleChangeLog();
        log.setRuleId(rule.getRuleId());
        log.setChangeType(changeType);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setChangedBy(rule.getCreatedBy());
        log.setChangeReason(rule.getChangeReason());
        changeLogRepository.save(log);
    }
}
```

### Step 7: Build and Run

```bash
cd backend/rule-service
mvn clean install

# Run service
mvn spring-boot:run

# Verify service running
curl http://localhost:8084/actuator/health
# Should return: {"status":"UP"}
```

---

## Frontend Setup

### Step 1: Create Service Client

Create `services/ruleService.ts`:

```typescript
import axios from 'axios';
import { TaxRule, CreateRuleRequest, UpdateRuleRequest } from '../types';

const API_BASE = '/api/rules';

export const ruleService = {
  // List rules with filters
  async listRules(filters: {
    tenantId?: string;
    category?: string;
    approvalStatus?: string;
    page?: number;
    size?: number;
  }) {
    const response = await axios.get(API_BASE, { params: filters });
    return response.data;
  },

  // Get single rule
  async getRule(ruleId: string): Promise<TaxRule> {
    const response = await axios.get(`${API_BASE}/${ruleId}`);
    return response.data;
  },

  // Create new rule
  async createRule(request: CreateRuleRequest): Promise<TaxRule> {
    const response = await axios.post(API_BASE, request);
    return response.data;
  },

  // Update rule
  async updateRule(ruleId: string, request: UpdateRuleRequest): Promise<TaxRule> {
    const response = await axios.put(`${API_BASE}/${ruleId}`, request);
    return response.data;
  },

  // Approve rule
  async approveRule(ruleId: string, approvalReason: string): Promise<TaxRule> {
    const response = await axios.post(`${API_BASE}/${ruleId}/approve`, { approvalReason });
    return response.data;
  },

  // Reject rule
  async rejectRule(ruleId: string, rejectionReason: string): Promise<TaxRule> {
    const response = await axios.post(`${API_BASE}/${ruleId}/reject`, { rejectionReason });
    return response.data;
  },

  // Get active rules (for tax calculations)
  async getActiveRules(tenantId: string, taxYear: number, entityType: string) {
    const response = await axios.get(`${API_BASE}/active`, {
      params: { tenantId, taxYear, entityType }
    });
    return response.data;
  },

  // Get rule history
  async getRuleHistory(ruleId: string) {
    const response = await axios.get(`${API_BASE}/${ruleId}/history`);
    return response.data;
  }
};
```

### Step 2: Create Dashboard Component

Create `components/admin/RuleConfigurationDashboard.tsx`:

```typescript
import React, { useEffect, useState } from 'react';
import { ruleService } from '../../services/ruleService';
import { TaxRule } from '../../types';
import RuleList from './RuleList';
import RuleEditor from './RuleEditor';

export const RuleConfigurationDashboard: React.FC = () => {
  const [rules, setRules] = useState<TaxRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [showEditor, setShowEditor] = useState(false);
  const [selectedRule, setSelectedRule] = useState<TaxRule | null>(null);

  useEffect(() => {
    loadRules();
  }, []);

  const loadRules = async () => {
    try {
      const response = await ruleService.listRules({ approvalStatus: 'APPROVED' });
      setRules(response.content);
    } catch (error) {
      console.error('Failed to load rules:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRule = () => {
    setSelectedRule(null);
    setShowEditor(true);
  };

  const handleEditRule = (rule: TaxRule) => {
    setSelectedRule(rule);
    setShowEditor(true);
  };

  const handleSaveRule = async () => {
    await loadRules();
    setShowEditor(false);
  };

  if (loading) {
    return <div>Loading rules...</div>;
  }

  return (
    <div className="rule-configuration-dashboard">
      <div className="header">
        <h1>Tax Rule Configuration</h1>
        <button onClick={handleCreateRule} className="btn btn-primary">
          Create New Rule
        </button>
      </div>

      <div className="stats-cards">
        <div className="stat-card">
          <h3>Active Rules</h3>
          <p className="stat-value">{rules.filter(r => r.approvalStatus === 'APPROVED').length}</p>
        </div>
        <div className="stat-card">
          <h3>Pending Approval</h3>
          <p className="stat-value">{rules.filter(r => r.approvalStatus === 'PENDING').length}</p>
        </div>
      </div>

      <RuleList rules={rules} onEdit={handleEditRule} onRefresh={loadRules} />

      {showEditor && (
        <RuleEditor
          rule={selectedRule}
          onSave={handleSaveRule}
          onCancel={() => setShowEditor(false)}
        />
      )}
    </div>
  );
};

export default RuleConfigurationDashboard;
```

### Step 3: Add Routes

Update `App.tsx`:

```typescript
import RuleConfigurationDashboard from './components/admin/RuleConfigurationDashboard';

// In your routing config:
<Route path="/admin/rules" element={<RuleConfigurationDashboard />} />
```

### Step 4: Build and Test

```bash
npm install
npm run dev

# Navigate to http://localhost:5173/admin/rules
```

---

## Database Setup

### Local Development (Docker)

```bash
# Start PostgreSQL and Redis
docker-compose up -d postgres redis

# Verify connectivity
psql -h localhost -U postgres -d munitax -c "SELECT 1"
redis-cli ping  # Should return: PONG
```

### Production Setup

- PostgreSQL: Use managed service (AWS RDS, Azure Database, Google Cloud SQL)
- Redis: Use managed service (AWS ElastiCache, Azure Cache for Redis)
- Backup strategy: Daily automated backups, 30-day retention
- Monitoring: Enable CloudWatch/Azure Monitor metrics

---

## Testing Guide

### Unit Tests

```bash
# Backend
cd backend/rule-service
mvn test

# Frontend
cd /home/runner/work/munciplaityTax/munciplaityTax
npm test -- --testPathPattern=ruleService
```

### Integration Tests

```bash
# Run Testcontainers tests (requires Docker)
cd backend/rule-service
mvn verify -P integration-tests
```

### Manual Testing Checklist

- [ ] Create new rule with effective date in future → Status PENDING
- [ ] Approve rule → Status APPROVED, cache invalidated
- [ ] Update approved rule → Error "Cannot modify approved rule"
- [ ] Create overlapping rule → Error "Overlapping date range"
- [ ] Query active rules for tax year → Returns correct rules
- [ ] View rule history → Shows all versions

---

## Common Workflows

### Workflow 1: Create New Tax Rate

1. Admin logs in with TAX_ADMINISTRATOR role
2. Navigates to `/admin/rules`
3. Clicks "Create New Rule"
4. Fills form:
   - Rule Code: `MUNICIPAL_RATE`
   - Rule Name: `Municipal Tax Rate`
   - Category: `TaxRates`
   - Value Type: `PERCENTAGE`
   - Value: `{"scalar": 2.25, "unit": "percent"}`
   - Effective Date: `2026-01-01`
   - Tenant: `dublin`
   - Change Reason: `Ordinance 2025-45 increases rate`
5. Clicks "Save" → Rule created with status PENDING
6. Approver reviews and clicks "Approve"
7. Rule status changes to APPROVED
8. Cache invalidated for `dublin` tenant
9. Next tax calculation for 2026 uses 2.25% rate

### Workflow 2: Migrate Existing Rules

```sql
-- Example: Migrate Dublin municipal rate from constants.ts
INSERT INTO tax_rules (
  rule_id, rule_code, rule_name, category, value_type, value,
  effective_date, end_date, tenant_id, entity_types,
  approval_status, approved_by, approval_date,
  created_by, created_date, change_reason
) VALUES (
  gen_random_uuid(),
  'MUNICIPAL_RATE',
  'Municipal Tax Rate',
  'TaxRates',
  'PERCENTAGE',
  '{"scalar": 2.0, "unit": "percent"}'::jsonb,
  '2020-01-01',
  NULL,
  'dublin',
  ARRAY['ALL'],
  'APPROVED',
  'system',
  NOW(),
  'system',
  NOW(),
  'Initial migration from constants.ts'
);
```

---

## Troubleshooting

### Issue: "Redis connection refused"

**Solution**:
```bash
# Check Redis running
redis-cli ping

# Start Redis if not running
docker-compose up -d redis

# Verify application.yml has correct Redis host/port
```

### Issue: "Cannot modify approved rule"

**Solution**: This is expected behavior. Create new rule version with future effective date instead:
```typescript
// Instead of updating existing rule:
await ruleService.updateRule(ruleId, { value: newValue });  // ❌ Fails

// Create new version:
await ruleService.createRule({
  ...existingRule,
  ruleId: undefined,  // New rule
  effectiveDate: '2026-01-01',  // Future date
  previousVersionId: existingRule.ruleId,  // Link to old version
  version: existingRule.version + 1
});  // ✅ Success
```

### Issue: "Overlapping rule detected"

**Solution**: Check existing rules for same ruleCode + tenantId:
```sql
SELECT rule_id, effective_date, end_date, approval_status
FROM tax_rules
WHERE rule_code = 'MUNICIPAL_RATE' 
  AND tenant_id = 'dublin'
ORDER BY effective_date;
```

Set `end_date` on previous rule to day before new rule's `effective_date`.

---

## Next Steps

After completing this quickstart:

1. **Review Spec**: Read [spec.md](./spec.md) for full requirements
2. **Review Data Model**: Read [data-model.md](./data-model.md) for entity details
3. **Review API Contract**: Read [contracts/rule-service-api.yaml](./contracts/rule-service-api.yaml) for endpoint specs
4. **Review Cache Contract**: Read [contracts/rule-cache-contract.md](./contracts/rule-cache-contract.md) for caching strategy
5. **Implement Features**: Follow [tasks.md](./tasks.md) for implementation order (generated by `/speckit.tasks` command)

---

## Support

- **Questions**: Ask in #munitax-dev Slack channel
- **Bugs**: File issue in GitHub repository
- **Documentation**: See [plan.md](./plan.md) for high-level architecture

**Last Updated**: 2025-11-28
