# backend

Microservices-based backend for the Municipality Tax Management System, implementing tax calculations, document processing, and business operations management.

<!-- AUTO-GENERATED: Purpose -->
## Purpose

The backend directory contains all microservices that power the municipality tax management platform. This modular architecture separates concerns into independent services for authentication, tax calculation, document extraction, payment processing, and more. Each service is independently deployable and scalable, communicating via REST APIs and service discovery.

The backend is built using Spring Boot (Java) for business logic services and follows Domain-Driven Design (DDD) principles with clear separation of controllers, services, repositories, and domain models.
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Key Files -->
## Key Files

| File/Directory | Description |
|---------------|-------------|
| `auth-service/` | Authentication and user management service |
| `tax-engine-service/` | Core tax calculation engine with schedules, forms, and compliance logic |
| `extraction-service/` | AI-powered document extraction and OCR service |
| `ledger-service/` | Accounting ledger and double-entry bookkeeping |
| `submission-service/` | Tax return submission and filing management |
| `pdf-service/` | PDF generation and manipulation service |
| `rule-service/` | Business rules engine for tax compliance |
| `tenant-service/` | Multi-tenancy and municipality configuration |
| `gateway-service/` | API gateway for routing and request aggregation |
| `discovery-service/` | Service discovery and registration (Eureka) |
| `pom.xml` | Maven parent POM for all services |
| `Dockerfile` | Shared Docker configuration |
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Service Architecture -->
## Service Architecture

### Microservices Pattern
The backend follows a microservices architecture with the following layers:

```
┌─────────────────┐
│  Frontend/UI    │
└────────┬────────┘
         │
    ┌────▼────────┐
    │   Gateway   │ ← Entry point, routing, auth
    └────┬────────┘
         │
    ┌────▼──────────────────────────────────────┐
    │         Service Mesh                       │
    ├────────────┬─────────────┬────────────────┤
    │ Auth       │ Tax Engine  │ Extraction     │
    │ Service    │ Service     │ Service        │
    ├────────────┼─────────────┼────────────────┤
    │ Ledger     │ Submission  │ PDF Service    │
    │ Service    │ Service     │                │
    ├────────────┼─────────────┼────────────────┤
    │ Rule       │ Tenant      │ Discovery      │
    │ Service    │ Service     │ Service        │
    └────────────┴─────────────┴────────────────┘
```

### Core Services

1. **auth-service**
   - JWT-based authentication
   - User management and profiles
   - Role-based access control (RBAC)
   - Session management

2. **tax-engine-service** (Largest service)
   - Tax calculation engine
   - Schedule X, Y, W1, W2 forms
   - Net Operating Loss (NOL) tracking
   - Apportionment calculations
   - Interest and penalty calculations
   - Nexus determination
   - Multi-year reconciliation

3. **extraction-service**
   - AI-powered document extraction using Gemini
   - Form schema validation
   - Field mapping and normalization
   - OCR integration

4. **ledger-service**
   - Double-entry accounting
   - Trial balance
   - Account reconciliation
   - Financial reporting

5. **submission-service**
   - Tax return filing
   - Document upload management
   - Submission tracking and status
   - Electronic filing integration

6. **pdf-service**
   - PDF generation from tax forms
   - Document watermarking
   - PDF manipulation and merging

7. **rule-service**
   - Business rules repository
   - Tax compliance rules
   - Validation rules
   - Dynamic rule evaluation

8. **tenant-service**
   - Multi-tenancy support
   - Municipality-specific configuration
   - Tenant isolation

9. **gateway-service**
   - API routing
   - Request authentication
   - Load balancing
   - Rate limiting

10. **discovery-service**
    - Service registry (Eureka)
    - Service health monitoring
    - Dynamic service discovery
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Technology Stack -->
## Technology Stack

### Core Framework
- **Spring Boot 3.x** - Application framework
- **Spring Cloud** - Microservices toolkit
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data access layer

### Database
- **PostgreSQL** - Primary relational database
- **Redis** - Caching layer (tax-engine-service)
- **H2** - In-memory database for testing

### Integration
- **Google Gemini API** - AI document extraction
- **JWT** - Token-based authentication
- **REST** - Service communication

### DevOps
- **Maven** - Build tool
- **Docker** - Containerization
- **Eureka** - Service discovery
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: API Communication -->
## API Communication

### Service Discovery
All services register with `discovery-service` (Eureka) and communicate using service names:

```java
// Example: Tax Engine calling Ledger Service
@FeignClient(name = "ledger-service")
public interface LedgerClient {
    @GetMapping("/api/ledger/trial-balance/{tenantId}")
    TrialBalanceDto getTrialBalance(@PathVariable String tenantId);
}
```

### Gateway Routing
External requests go through `gateway-service`:

```
External Request → Gateway → Service
  /api/auth/*    → Gateway → auth-service
  /api/tax/*     → Gateway → tax-engine-service
  /api/ledger/*  → Gateway → ledger-service
```

### Authentication Flow
```
1. Client → POST /api/auth/login → auth-service
2. auth-service → JWT token
3. Client → Request with JWT → gateway-service
4. gateway-service → Validates JWT → Routes to service
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Data Models -->
## Common Data Patterns

### Entity Structure
Each service follows consistent entity patterns:

```java
@Entity
@Table(name = "entity_name")
public class EntityName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id")
    private String tenantId;  // Multi-tenancy
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### DTO Pattern
Data Transfer Objects for API contracts:

```java
public record EntityDto(
    Long id,
    String tenantId,
    // ... fields
) {}
```

### Repository Pattern
```java
@Repository
public interface EntityRepository extends JpaRepository<Entity, Long> {
    List<Entity> findByTenantId(String tenantId);
}
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Configuration -->
## Configuration

### Service Configuration
Each service has `application.yml` or `application.properties`:

```yaml
server:
  port: ${SERVICE_PORT:8080}

spring:
  application:
    name: service-name
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASS}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka}
```

### Multi-Tenancy
All services support tenant isolation via `tenantId` header:

```
GET /api/resource
Headers:
  Authorization: Bearer <JWT>
  X-Tenant-ID: <tenant-id>
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Development Setup -->
## Development Setup

### Prerequisites
- Java 17 or higher
- Maven 3.8+
- Docker and Docker Compose
- PostgreSQL 14+
- Redis (for tax-engine-service)

### Build All Services
```bash
cd backend
mvn clean install
```

### Run Individual Service
```bash
cd auth-service
mvn spring-boot:run
```

### Run with Docker Compose
```bash
docker-compose up --build
```

### Service Ports
| Service | Port |
|---------|------|
| discovery-service | 8761 |
| gateway-service | 8080 |
| auth-service | 8081 |
| tax-engine-service | 8082 |
| extraction-service | 8083 |
| ledger-service | 8084 |
| submission-service | 8085 |
| pdf-service | 8086 |
| rule-service | 8087 |
| tenant-service | 8088 |
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Testing -->
## Testing

### Unit Tests
Each service has unit tests in `src/test/java`:

```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Test Coverage
```bash
mvn jacoco:report
```
<!-- END AUTO-GENERATED -->

<!-- CUSTOM SECTION: Project-Specific Notes -->
## Notes

Add backend-specific notes, architectural decisions, or deployment considerations here.

<!-- END CUSTOM SECTION -->

<!-- AUTO-GENERATED: Related Modules -->
## Related Modules

- [`../src/`](../src/README.md) - Frontend React application
- [`../components/`](../components/README.md) - React UI components
- [`../services/`](../services/README.md) - Frontend API services
- [`../docs/`](../docs/) - Technical documentation

<!-- END AUTO-GENERATED -->
