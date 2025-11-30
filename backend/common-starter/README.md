# MuniTax Common Starter

A Spring Boot starter module that provides common configuration for all MuniTax microservices.

## Features

### 1. Actuator Endpoints (Standalone Mode)
Automatically configures Spring Boot Actuator with the following endpoints:
- `/actuator/health` - Health check with detailed component status
- `/actuator/info` - Service information
- `/actuator/metrics` - Performance metrics
- `/actuator/env` - Environment properties
- `/actuator/loggers` - Log level management

### 2. CORS Configuration (Standalone Mode)
Enables CORS for frontend development on multiple ports:
- `http://localhost:3000-3003` - Standard React/Vite ports
- `http://localhost:5173` - Default Vite port

**Features:**
- Credentials support enabled
- All HTTP methods allowed
- All headers allowed
- Exposed headers: `Authorization`, `Content-Type`, `X-Total-Count`, `X-Request-Id`
- 1-hour preflight cache

### 3. Profile-Based Activation
All configurations are activated only when the `standalone` profile is active, ensuring they don't interfere with production deployments.

## Usage

### 1. Add Dependency

Add to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.munitax</groupId>
    <artifactId>common-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Import Common Configuration

Add to your service's `application-standalone.yml`:

```yaml
spring:
  application:
    name: your-service-standalone
  config:
    import: optional:classpath:application-common-standalone.yml
```

Or in `application-standalone.properties`:

```properties
spring.application.name=your-service-standalone
spring.config.import=optional:classpath:application-common-standalone.yml
```

### 3. Run in Standalone Mode

```bash
cd backend/your-service
mvn spring-boot:run -Dspring-boot.run.profiles=standalone
```

## What It Replaces

Before common-starter, each service had:
- Individual `StandaloneCorsConfig.java` files (duplicated across 6+ services)
- Duplicate actuator configuration in each `application-standalone.yml`
- Inconsistent CORS allowed origins

Now:
- ✅ Single source of truth for CORS and actuator configuration
- ✅ Consistent behavior across all services
- ✅ Easy to update frontend ports in one place
- ✅ Reduced maintenance burden

## Services Using Common Starter

1. **auth-service** (Port 8081)
2. **rule-service** (Port 8084)
3. **ledger-service** (Port 8087)
4. **tax-engine-service** (Port 8085)
5. **extraction-service** (Port 8083)
6. **submission-service** (Port 8089)
7. **pdf-service** (Port 8086)

## Configuration Details

### Actuator Endpoints
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,loggers
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      show-components: always
```

### CORS Configuration
Applies to all paths (`/**`) and supports:
- Multiple localhost ports for frontend flexibility
- Credential-based authentication
- All standard HTTP methods
- Custom headers for API responses

## Development

### Building the Module

```bash
cd backend/common-starter
mvn clean install
```

This installs the starter to your local Maven repository, making it available to all services.

### Testing

Start any service with standalone profile and verify:

```bash
# Check health endpoint
curl http://localhost:PORT/actuator/health

# Check CORS headers
curl -H "Origin: http://localhost:3003" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS \
     http://localhost:PORT/actuator/health
```

## Maintenance

To update CORS allowed origins or actuator endpoints:

1. Edit `/backend/common-starter/src/main/resources/application-common-standalone.yml`
2. Or modify `CommonStandaloneCorsConfig.java` for programmatic changes
3. Rebuild: `mvn clean install`
4. Restart services to pick up changes

## Best Practices

- **Don't** use standalone profile in production
- **Do** add new frontend ports to `ALLOWED_ORIGINS` list if needed
- **Do** keep actuator endpoints minimal in production (only health/info)
- **Do** document any service-specific overrides

## Troubleshooting

### Services Can't Find common-starter

```bash
cd backend/common-starter
mvn clean install
```

### CORS Still Failing

1. Verify service is running with `standalone` profile
2. Check frontend is using an allowed origin port
3. Verify common config is imported in `application-standalone.yml`
4. Check browser console for specific CORS error

### Actuator Endpoints Return 404

1. Ensure `standalone` profile is active
2. Verify common-starter dependency is in service's `pom.xml`
3. Check that Spring Boot Actuator is not disabled elsewhere
