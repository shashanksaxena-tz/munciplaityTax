# Rule Engine Service - Comprehensive Disconnect Analysis

**Date**: December 1, 2025
**Analysis Focus**: rule-service vs tax-engine-service inconsistencies

---

## Executive Summary

The rule-service has **CRITICAL DISCONNECTS** across all layers:
1. ❌ **Different Database**: Connects to Aiven cloud DB, not local Docker postgres
2. ❌ **Missing Schema**: tax_rules table doesn't exist in munitax_db (only in cloud DB)
3. ❌ **Wrong Enum Values**: Spec uses underscored (TAX_RATES), code uses PascalCase (TaxRates)
4. ❌ **Isolated Service**: Cannot integrate with tax-engine-service due to separate databases
5. ❌ **Not Deployable**: Cannot run in Docker environment as configured

---

## 1. Database Connection Disconnect

### Spec Says (specs/4-rule-configuration-ui/data-model.md)
- Should use same database as other services
- Should have tax_rules and rule_change_log tables in munitax_db

### Code Reality (backend/rule-service/src/main/resources/application.yml)
```yaml
datasource:
  url: jdbc:postgresql://pg-1ac838cf-taazaa-6110.g.aivencloud.com:23581/defaultdb?ssl=require
  username: avnadmin
  password: AVNS_CIZODtyC1VgMeY5KcC3
```

### Other Services (tax-engine, auth, tenant, etc.)
```yaml
datasource:
  url: jdbc:postgresql://postgres:5432/munitax_db
  username: postgres
  password: password
```

### Impact
- ❌ rule-service cannot run in Docker compose (tries to connect to external cloud DB)
- ❌ Migrations in rule-service never run against munitax_db
- ❌ tax_rules table doesn't exist in local database
- ❌ Cannot be tested/developed locally
- ❌ Cannot integrate with tax-engine-service (different databases)

---

## 2. Schema Migration Disconnect

### What Exists
```
munitax_db tables:
- users, user_profiles, user_roles, tenants
- filing_packages, filing_package_forms
- form_templates, generated_forms, form_audit_log
- flyway_schema_history (from auth/tenant services)
```

### What's Missing
```
rule-service migrations (NEVER RAN):
- V1__create_tax_rules_table.sql
- V2__create_rule_change_log_table.sql  
- V3__create_tenant_rls_policies.sql
```

### Why Missing
- Flyway migrations in rule-service only run against Aiven cloud DB
- rule-service has its own separate flyway_schema_history
- No coordination with main database schema

### Fix Required
- Move rule-service migrations to common-starter or auth-service
- OR update rule-service to use same DB as other services
- Ensure tax_rules table created in munitax_db

---

## 3. Enum Values Disconnect

### Spec Says (data-model.md Line 30)
```sql
category VARCHAR(50) CHECK (category IN (
  'TaxRates', 'IncomeInclusion', 'Deductions', 'Penalties',
  'Filing', 'Allocation', 'Withholding', 'Validation'
))
```

### Migration Says (V1__create_tax_rules_table.sql Line 28)
```sql
CONSTRAINT chk_category CHECK (category IN (
  'TAX_RATES', 'INCOME_INCLUSION', 'DEDUCTIONS', 'PENALTIES', 
  'FILING', 'ALLOCATION', 'WITHHOLDING', 'VALIDATION'
))
```

### Code Says (RuleCategory.java)
```java
public enum RuleCategory {
    TAX_RATES,
    INCOME_INCLUSION,
    DEDUCTIONS,
    PENALTIES,
    FILING,
    ALLOCATION,
    WITHHOLDING,
    VALIDATION
}
```

### Mismatch
- **Spec**: PascalCase (TaxRates, IncomeInclusion)
- **Code**: SCREAMING_SNAKE_CASE (TAX_RATES, INCOME_INCLUSION)
- **Result**: ✅ Code and migration match, ❌ Spec is wrong

### Fix Required
- Update spec to use SCREAMING_SNAKE_CASE
- OR update code and migration to use PascalCase
- **Recommendation**: Keep code as-is (Java enum convention), update spec

---

## 4. Tax Engine Service Issues (Separate Problem)

### Current Status
- tax-engine-service has 30+ JPA repository query errors
- Entities don't match repository assumptions
- Examples:
  - PayrollFactor.totalPayroll doesn't exist → should be totalPayrollEverywhere
  - PayrollFactor.ohioPayroll doesn't exist → should be totalOhioPayroll  
  - PayrollFactor.remoteEmployeeCount doesn't exist → should be SIZE(remoteEmployeeAllocation)
  - ApportionmentAuditLog.changedAt doesn't exist → should be changeDate

### Root Cause
- Repository queries written without consulting actual entity definitions
- No schema-first or code-first discipline
- Queries assumed field names that never existed

---

## 5. Integration Disconnect

### How Tax Engine Should Use Rules
```
tax-engine-service (port 8085)
  ↓ REST call
rule-service (port 8084)
  ↓ Query
tax_rules table
```

### Current Reality
```
tax-engine-service (port 8085)
  ↓ connects to
postgres:5432/munitax_db (local Docker)
  
rule-service (port 8084)  
  ↓ connects to
pg-1ac838cf-taazaa-6110.g.aivencloud.com:23581/defaultdb (remote cloud)
```

### Impact
- ❌ Services cannot share data
- ❌ Rules configured in rule-service not visible to tax-engine-service
- ❌ Complete architectural breakdown

---

## 6. Deployment Disconnect

### Docker Compose Assumption
```yaml
services:
  rule-service:
    depends_on:
      postgres:
        condition: service_healthy
```

### Actual Configuration
- Tries to connect to external cloud database (not postgres container)
- SSL certificate validation required
- External dependency not in docker-compose.yml
- Cannot start without internet connection

---

## Recommended Fix Strategy

### Phase 1: Fix rule-service Database (CRITICAL)
1. Update `backend/rule-service/src/main/resources/application.yml`:
   ```yaml
   datasource:
     url: jdbc:postgresql://${DB_HOST:postgres}:${DB_PORT:5432}/${DB_NAME:munitax_db}
     username: ${DB_USERNAME:postgres}
     password: ${DB_PASSWORD:password}
   ```

2. Move migrations to run before rule-service starts:
   - Option A: Move to auth-service (runs first)
   - Option B: Create init-db service with all migrations
   - Option C: Keep in rule-service, ensure proper depends_on

3. Run migrations manually for now:
   ```bash
   docker exec -it munitax-postgres psql -U postgres -d munitax_db -f /path/to/V1__create_tax_rules_table.sql
   ```

### Phase 2: Fix tax-engine-service Queries
1. Read actual entity files for field names
2. Fix all repository queries to match entity definitions
3. Test startup without errors

### Phase 3: Integration Testing
1. Deploy rule-service successfully
2. Deploy tax-engine-service successfully
3. Test integration between services
4. Verify rules flow end-to-end

### Phase 4: Update Specs
1. Correct enum naming conventions in spec
2. Document actual database schema
3. Update data-model.md to match reality

---

## Immediate Action Required

### Step 1: Fix rule-service datasource
```yaml
# backend/rule-service/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:postgres}:${DB_PORT:5432}/${DB_NAME:munitax_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
```

### Step 2: Create tax_rules schema in munitax_db
```bash
# Run the migration SQL files manually
docker cp backend/rule-service/src/main/resources/db/migration/V1__create_tax_rules_table.sql munitax-postgres:/tmp/
docker exec -it munitax-postgres psql -U postgres -d munitax_db -f /tmp/V1__create_tax_rules_table.sql
```

### Step 3: Build and deploy ONLY rule-service
```bash
docker compose build rule-service
docker compose up -d rule-service
docker logs rule-service --follow
```

### Step 4: After rule-service works, fix tax-engine-service
- Fix all repository query errors
- Deploy separately
- Verify startup

### Step 5: Deploy full stack
- Start all services
- Verify integration
- Check docker stats

---

## Files Requiring Changes

### Priority 1 - CRITICAL (Cannot deploy without)
1. `backend/rule-service/src/main/resources/application.yml` - Fix database URL
2. Create tax_rules tables in munitax_db - Run migrations

### Priority 2 - HIGH (Current errors preventing startup)
3. `backend/tax-engine-service/.../repository/ApportionmentAuditLogRepository.java` - Fix changeDate
4. `backend/tax-engine-service/.../repository/PayrollFactorRepository.java` - Fix field names
5. `backend/tax-engine-service/.../repository/PropertyFactorRepository.java` - Verify no more issues

### Priority 3 - MEDIUM (Documentation)
6. `specs/4-rule-configuration-ui/data-model.md` - Update enum conventions
7. Add integration documentation

---

## Questions for User

1. **Should rule-service use local Docker postgres** or keep cloud database?
   - Recommendation: Use local for development/testing

2. **Should we preserve cloud DB data** or start fresh?
   - Affects migration strategy

3. **Should we fix specs to match code** or code to match specs?
   - Recommendation: Specs to match code (enums)

4. **Priority: Fix rule-service first** or tax-engine-service first?
   - Recommendation: rule-service (blocks integration)

---

## Current Blocker

**Cannot proceed with deployment until**:
- rule-service database connection fixed
- tax_rules table created in munitax_db
- OR decision to deploy tax-engine-service independently without rules

