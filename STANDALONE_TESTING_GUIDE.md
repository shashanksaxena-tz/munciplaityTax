# Individual Service Testing Guide

This guide explains how to test individual microservices (Rule Engine, Ledger Service, Extraction Service) independently with their dedicated frontend UIs.

## Overview

The individual service testing feature allows you to:
- Test each microservice in isolation without running the full application stack
- Debug and validate service functionality independently
- Access dedicated test UIs for each service
- Run services with a special `standalone` profile that disables service discovery

## Architecture

### Backend Changes
Each service can now run in `standalone` mode with:
- **Standalone Spring Profile**: Disables Eureka service discovery
- **CORS Configuration**: Allows direct frontend access from localhost:3000
- **Independent Operation**: Services run without dependencies on other services

### Frontend Changes
New test UI pages have been added:
- `/test` - Main service testing dashboard
- `/test/rules` - Rule Service test interface
- `/test/ledger` - Ledger Service test interface
- `/test/extraction` - Extraction Service test interface

## Quick Start

### 1. Start Services in Standalone Mode

Open separate terminal windows for each service you want to test:

#### Rule Service (Port 8084)
```bash
cd backend/rule-service
mvn spring-boot:run -Dspring-boot.run.profiles=standalone
```

#### Ledger Service (Port 8087)
```bash
cd backend/ledger-service
mvn spring-boot:run -Dspring-boot.run.profiles=standalone
```

#### Extraction Service (Port 8083)
```bash
cd backend/extraction-service
mvn spring-boot:run -Dspring-boot.run.profiles=standalone
```

### 2. Start the Frontend

In a separate terminal:
```bash
npm run dev
```

The frontend will start on http://localhost:3000

### 3. Access Test UIs

Navigate to http://localhost:3000/test to see the service testing dashboard.

From there, you can access individual service test pages:
- **Rule Service**: http://localhost:3000/test/rules
- **Ledger Service**: http://localhost:3000/test/ledger
- **Extraction Service**: http://localhost:3000/test/extraction

## Service Test Features

### Rule Service Test UI
- View connection status
- List all tax rules
- Create new rules with category, value, and effective dates
- View rule details including approval status
- Test rule approval workflow

**Key Endpoints Tested:**
- `GET /api/rules` - List rules
- `POST /api/rules` - Create rule
- `GET /api/rules/{id}` - Get rule details

### Ledger Service Test UI
- Test connection to ledger service
- Load journal entries
- View trial balance
- Test payment and reconciliation features

**Key Endpoints Tested:**
- `GET /api/ledger/journal-entries` - Get journal entries
- `GET /api/ledger/trial-balance/{tenantId}` - Get trial balance

### Extraction Service Test UI
- Upload documents (PDF, PNG, JPG)
- Test AI-powered extraction
- View extraction results with confidence scores
- Test with sample data

**Key Endpoints Tested:**
- `POST /api/extraction/extract` - Extract from file
- `POST /api/extraction/extract/text` - Extract from text

## Configuration Files

### Backend Profiles

Each service has a `application-standalone.yml` or `application-standalone.properties` file with:

```yaml
# Disable Eureka
eureka:
  client:
    enabled: false
    register-with-eureka: false
    fetch-registry: false

# Enable CORS for frontend
management:
  endpoints:
    web:
      cors:
        allowed-origins: "http://localhost:3000"
        allowed-methods: "*"
        allowed-headers: "*"
```

### CORS Configuration

Each service has a `StandaloneCorsConfig.java` that is only active when the `standalone` profile is used:

```java
@Configuration
@Profile("standalone")
public class StandaloneCorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        // CORS configuration for localhost:3000
    }
}
```

## Testing Workflow

### Example: Testing Rule Service

1. **Start Rule Service**
   ```bash
   cd backend/rule-service
   mvn spring-boot:run -Dspring-boot.run.profiles=standalone
   ```

2. **Verify Service is Running**
   - Check terminal for "Started RuleServiceApplication"
   - Service should be on port 8084

3. **Open Test UI**
   - Navigate to http://localhost:3000/test/rules
   - Check connection status indicator (should show "Connected")

4. **Test Functionality**
   - Click "Load Rules" to fetch existing rules
   - Fill out the "Create Test Rule" form
   - Click "Create Rule" to test POST endpoint
   - Verify the new rule appears in the list

5. **Review Results**
   - Check test result banners for success/error messages
   - Review extracted data and responses
   - Verify service logs in the terminal

## Benefits

1. **Faster Development**: Test changes without restarting the entire stack
2. **Isolated Testing**: Verify individual service functionality independently
3. **Easy Debugging**: Direct access to service endpoints with clear error messages
4. **No Dependencies**: Services run without requiring other microservices
5. **No Interference**: Standalone mode doesn't affect the main application

## Troubleshooting

### Service Won't Start
- Check if the port is already in use
- Verify PostgreSQL/Redis is running (for services that need them)
- Check application logs for errors

### Connection Failed
- Ensure service is running with `standalone` profile
- Verify the correct port number
- Check CORS configuration
- Look for firewall or proxy issues

### Test Endpoints Fail
- Check service logs for errors
- Verify request payload format
- Ensure required data exists in database
- Check authentication requirements

## Important Notes

⚠️ **Standalone Mode vs. Normal Mode**
- Standalone mode is for testing only
- The main application should still run with default profiles
- Standalone services won't register with Eureka
- Don't use standalone mode in production

⚠️ **Database Access**
- Some services require PostgreSQL
- Ensure database is accessible
- Migrations should run automatically via Flyway

⚠️ **No Authentication Required**
- Test UIs bypass authentication for easier testing
- This is intentional for development/testing purposes
- Don't expose these endpoints in production

## Next Steps

After testing services individually:
1. Verify fixes/changes work in isolation
2. Test integration with other services
3. Run full application to ensure no regressions
4. Update service tests if needed

## Files Modified/Added

### Backend
- `backend/rule-service/src/main/resources/application-standalone.yml`
- `backend/ledger-service/src/main/resources/application-standalone.properties`
- `backend/extraction-service/src/main/resources/application-standalone.yml`
- `backend/rule-service/src/main/java/com/munitax/rules/config/StandaloneCorsConfig.java`
- `backend/ledger-service/src/main/java/com/munitax/ledger/config/StandaloneCorsConfig.java`
- `backend/extraction-service/src/main/java/com/munitax/extraction/config/StandaloneCorsConfig.java`

### Frontend
- `components/test/ServiceTestDashboard.tsx`
- `components/test/RuleServiceTestUI.tsx`
- `components/test/LedgerServiceTestUI.tsx`
- `components/test/ExtractionServiceTestUI.tsx`
- `App.tsx` (updated with test routes)

## Support

For issues or questions about individual service testing:
1. Check service logs for error details
2. Review this guide
3. Verify configuration files
4. Test with sample data first
