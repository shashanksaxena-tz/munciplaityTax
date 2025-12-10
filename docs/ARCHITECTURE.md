# MuniTax System Architecture

## Overview

MuniTax is a comprehensive municipal tax filing and auditing system built using a modern microservices architecture. The system supports individual and business tax filing for Dublin Municipality with features for automated document extraction, tax calculation, auditor workflow, and payment processing.

---

## 🔴 CRITICAL ISSUE: Rule Service Integration Disconnect

> **⚠️ CRITICAL ARCHITECTURAL DISCONNECT**
>
> The Rule Service is **NOT integrated** with tax calculators. While rules can be created, approved, and stored in the database, they are **never applied during tax calculations**. Tax rates and rules are **hardcoded** in:
> - `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/IndividualTaxCalculator.java`
> - `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/BusinessTaxCalculator.java`
>
> **Status:** Architectural disconnect - Rule service exists but is unused.
>
> **Resolution:** See Issue [#95](https://github.com/shashanksaxena-tz/munciplaityTax/issues/95) for integration work.

---

## High-Level Architecture Diagram

```mermaid
graph TB
    subgraph "Client Layer"
        WEB[React Web Application<br/>Port 3000]
        MOBILE[Mobile App<br/>Future]
    end

    subgraph "API Gateway Layer"
        GW[Gateway Service<br/>Port 8080]
    end

    subgraph "Service Discovery"
        EUREKA[Eureka Discovery Service<br/>Port 8761]
    end

    subgraph "Core Microservices"
        AUTH[Auth Service<br/>Port 8081]
        TENANT[Tenant Service<br/>Port 8082]
        SUBMISSION[Submission Service<br/>Port 8084]
        TAX[Tax Engine Service<br/>Port 8085]
        EXTRACT[Extraction Service<br/>Port 8083]
        PDF[PDF Service<br/>Port 8086]
        RULE[Rule Service<br/>Port 8087]
        LEDGER[Ledger Service<br/>Port 8088]
    end

    subgraph "Infrastructure Layer"
        PG[(PostgreSQL 16<br/>Port 5432)]
        REDIS[(Redis 7<br/>Port 6379)]
        ZIPKIN[Zipkin Tracing<br/>Port 9411]
    end

    subgraph "External Services"
        GEMINI[Google Gemini AI<br/>Document Extraction]
        PAYMENT[Payment Gateway<br/>Mock/Stripe]
    end

    WEB --> GW
    MOBILE --> GW
    
    GW --> AUTH
    GW --> TENANT
    GW --> SUBMISSION
    GW --> TAX
    GW --> EXTRACT
    GW --> PDF
    GW --> RULE
    GW --> LEDGER
    
    AUTH --> EUREKA
    TENANT --> EUREKA
    SUBMISSION --> EUREKA
    TAX --> EUREKA
    EXTRACT --> EUREKA
    PDF --> EUREKA
    RULE --> EUREKA
    LEDGER --> EUREKA
    GW --> EUREKA
    
    AUTH --> PG
    TENANT --> PG
    SUBMISSION --> PG
    TAX --> PG
    RULE --> PG
    LEDGER --> PG
    
    AUTH --> REDIS
    RULE --> REDIS
    
    EXTRACT --> GEMINI
    LEDGER --> PAYMENT
    
    GW --> ZIPKIN
    AUTH --> ZIPKIN
    TAX --> ZIPKIN
```

---

## Technology Stack

### Frontend
| Component | Technology | Version |
|-----------|------------|---------|
| Framework | React | 19.x |
| Language | TypeScript | 5.x |
| Build Tool | Vite | 5.x |
| Styling | Tailwind CSS | 3.x |
| State Management | React Context + useReducer | - |
| Routing | React Router | v7 |
| Icons | Lucide React | - |

### Backend
| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.2.3 |
| Language | Java | 21 |
| API Gateway | Spring Cloud Gateway | - |
| Service Discovery | Netflix Eureka | - |
| ORM | JPA/Hibernate | - |
| Security | Spring Security + JWT | - |

### Infrastructure
| Component | Technology | Version |
|-----------|------------|---------|
| Database | PostgreSQL | 16 |
| Cache | Redis | 7 |
| Tracing | Zipkin | - |
| Containerization | Docker | - |
| Orchestration | Docker Compose | - |
| Reverse Proxy | Nginx | - |

---

## Microservices Architecture

### Service Catalog

```mermaid
graph LR
    subgraph "Service Registry"
        EUREKA[Eureka Discovery<br/>Service Registry]
    end

    subgraph "Gateway"
        GW[API Gateway<br/>Routing & Load Balancing]
    end

    subgraph "Security"
        AUTH[Auth Service<br/>JWT Authentication<br/>Role-Based Access]
    end

    subgraph "Core Business"
        TAX[Tax Engine<br/>Individual & Business<br/>Tax Calculations]
        RULE[Rule Service<br/>Dynamic Rules<br/>Configuration]
        SUBMISSION[Submission Service<br/>Tax Returns<br/>Auditor Workflow]
    end

    subgraph "Support"
        EXTRACT[Extraction Service<br/>AI Document<br/>Processing]
        PDF[PDF Service<br/>Form Generation]
        LEDGER[Ledger Service<br/>Payments &<br/>Accounting]
        TENANT[Tenant Service<br/>Multi-Tenant<br/>Management]
    end

    GW --> EUREKA
    AUTH --> EUREKA
    TAX --> EUREKA
    RULE --> EUREKA
    SUBMISSION --> EUREKA
    EXTRACT --> EUREKA
    PDF --> EUREKA
    LEDGER --> EUREKA
    TENANT --> EUREKA
```

### Service Responsibilities

| Service | Port | Primary Responsibilities |
|---------|------|-------------------------|
| **Discovery Service** | 8761 | Service registration, health monitoring, load balancing coordination |
| **Gateway Service** | 8080 | API routing, rate limiting, request/response transformation |
| **Auth Service** | 8081 | User authentication, JWT token management, role-based access control |
| **Tenant Service** | 8082 | Multi-tenant management, session storage, address validation |
| **Extraction Service** | 8083 | AI-powered document extraction using Google Gemini |
| **Submission Service** | 8084 | Tax return submissions, auditor workflow, audit trail |
| **Tax Engine Service** | 8085 | Individual and business tax calculations, discrepancy detection |
| **PDF Service** | 8086 | Tax form PDF generation using Apache PDFBox |
| **Rule Service** | 8087 | Dynamic tax rule configuration, temporal rule management |
| **Ledger Service** | 8088 | Double-entry ledger, payment processing, reconciliation |

---

## Deployment Architecture

### Container Deployment

```mermaid
graph TB
    subgraph "Docker Host"
        subgraph "Frontend Container"
            NGINX[Nginx<br/>Static Assets]
            REACT[React App]
        end

        subgraph "Backend Containers"
            GW[Gateway]
            DISC[Discovery]
            AUTH[Auth]
            TENANT[Tenant]
            TAX[Tax Engine]
            EXTRACT[Extraction]
            PDF[PDF]
            SUBMISSION[Submission]
            RULE[Rule]
            LEDGER[Ledger]
        end

        subgraph "Infrastructure Containers"
            PG[(PostgreSQL)]
            REDIS[(Redis)]
            ZIP[Zipkin]
        end
    end

    NGINX --> GW
    GW --> DISC
    AUTH --> DISC
    TAX --> DISC
    AUTH --> PG
    TENANT --> PG
    SUBMISSION --> PG
    AUTH --> REDIS
```

### Network Architecture

- **munitax-network**: Bridge network connecting all containers
- **External Ports**:
  - 3000: Frontend (Nginx)
  - 8080: API Gateway
  - 8761: Eureka Dashboard
  - 9411: Zipkin Dashboard
  - 5432: PostgreSQL (development only)
  - 6379: Redis (development only)

---

## Communication Patterns

### Synchronous Communication
- REST APIs over HTTP/HTTPS
- Service-to-service calls via Eureka service discovery
- JSON request/response format

### Asynchronous Communication (Planned)
- Event-driven messaging for audit trail
- Message queues for document processing
- WebSocket for real-time extraction updates

### Inter-Service Communication

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Auth
    participant TaxEngine
    participant Rule
    participant Submission

    Client->>Gateway: POST /api/v1/tax/calculate
    Gateway->>Auth: Validate JWT
    Auth-->>Gateway: Token Valid
    Gateway->>TaxEngine: Calculate Tax
    TaxEngine->>Rule: Get Active Rules
    Rule-->>TaxEngine: Tax Rules
    TaxEngine-->>Gateway: Tax Result
    Gateway->>Submission: Store Session
    Submission-->>Gateway: Session ID
    Gateway-->>Client: Tax Calculation Response
```

---

## Scalability Considerations

### Horizontal Scaling
- All microservices are stateless and can be horizontally scaled
- Eureka handles service registration for multiple instances
- Gateway performs client-side load balancing

### Database Scaling
- PostgreSQL supports read replicas for read-heavy workloads
- Schema-per-tenant isolation enables database sharding
- Connection pooling via HikariCP

### Caching Strategy
- Redis caches frequently accessed tax rules
- Session data cached for performance
- Rule evaluation results cached per tax year/tenant

---

## High Availability

### Service Redundancy
- Multiple instances per service recommended for production
- Eureka self-preservation mode protects against network partitions
- Circuit breaker pattern (planned) for fault tolerance

### Data Persistence
- PostgreSQL with regular backups
- Volume persistence for container data
- 7-year audit trail retention per IRS requirements

---

## Security Architecture

### Authentication Flow
```mermaid
flowchart LR
    A[User] --> B[Login Request]
    B --> C[Auth Service]
    C --> D{Validate Credentials}
    D -->|Valid| E[Generate JWT]
    D -->|Invalid| F[401 Unauthorized]
    E --> G[Return Token]
    G --> H[Store in LocalStorage]
    H --> I[Include in Requests]
    I --> J[Gateway Validates]
```

### Security Layers
1. **Transport Security**: TLS/SSL for all communications
2. **Authentication**: JWT tokens with configurable expiration
3. **Authorization**: Role-based access control (RBAC)
4. **API Security**: Rate limiting, request validation
5. **Data Security**: Encryption at rest, PII handling

---

## Monitoring & Observability

### Distributed Tracing
- Zipkin collects traces from all services
- Request correlation via trace IDs
- Performance bottleneck identification

### Health Checks
- Spring Boot Actuator endpoints
- Eureka health monitoring
- Docker healthcheck configurations

### Logging
- Structured JSON logging
- Centralized log aggregation (planned)
- Audit trail for compliance

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-01 | Initial architecture documentation |

---

**Document Owner:** Development Team  
**Last Updated:** December 1, 2025
