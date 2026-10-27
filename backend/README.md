# Backend Services

Microservices architecture powering the Municipality Tax Management System.

## Overview

The backend is built as a collection of independent Spring Boot microservices, each responsible for a specific domain of the tax management system. Services communicate via REST APIs and use Eureka for service discovery. This architecture enables independent scaling, deployment, and development of each component.

**Key Features:**
- JWT-based authentication and authorization
- Multi-tenant support for multiple municipalities
- Comprehensive tax calculation engine
- AI-powered document extraction
- Accounting ledger integration
- Tax return submission and compliance

## Structure

```
backend/
├── discovery-service/       # Eureka service registry
├── gateway-service/        # API gateway and routing
├── auth-service/           # Authentication and user management
├── tax-engine-service/     # Core tax calculations (largest service)
├── extraction-service/     # AI document extraction
├── ledger-service/         # Double-entry accounting
├── submission-service/     # Tax return filing
├── pdf-service/           # PDF generation and manipulation
├── rule-service/          # Business rules engine
├── tenant-service/        # Multi-tenancy management
├── pom.xml               # Maven parent POM
└── Dockerfile            # Docker configuration
```

## Quick Start

### Build All Services
```bash
cd backend
mvn clean install
```

### Run Services with Docker
```bash
docker-compose up --build
```

### Run Individual Service
```bash
cd auth-service
mvn spring-boot:run
```

### Access Points
- **Service Discovery**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **Auth Service**: http://localhost:8081
- **Tax Engine**: http://localhost:8082

## Service Overview

### 🔐 auth-service
Handles user authentication, authorization, and profile management. Issues JWT tokens for secure service communication.

**Key Features:**
- User registration and login
- JWT token generation  and validation
- Role-based access control
- User profile management

### 🧮 tax-engine-service
The heart of the system - calculates taxes, manages forms, handles NOL, apportionment, interest, and penalties.

**Key Features:**
- Schedule X (business income tax)
- Schedule Y (municipal tax)
- W1/W2 withholding forms
- Net Operating Loss (NOL) tracking
- Apportionment calculations
- Interest and penalty calculations
- Multi-year reconciliation

### 📄 extraction-service
Leverages Google Gemini AI to extract structured data from tax documents and forms.

**Key Features:**
- AI-powered OCR
- Form schema validation
- Field mapping and normalization
- Confidence scoring

### 📊 ledger-service
Provides double-entry accounting capabilities with trial balance and reconciliation.

**Key Features:**
- Chart of accounts
- Journal entries
- Trial balance generation
- Account reconciliation

### 📝 submission-service
Manages tax return submissions, document uploads, and filing status tracking.

**Key Features:**
- Return submission workflow
- Document upload management
- Status tracking
- Electronic filing preparation

### 📑 pdf-service
Generates and manipulates PDF documents for tax forms and reports.

**Key Features:**
- Dynamic PDF generation
- Form filling
- Document merging
- Watermarking

### 📋 rule-service
Stores and evaluates business rules for tax compliance and validation.

**Key Features:**
- Rule repository
- Dynamic rule evaluation
- Compliance validation
- Custom rule creation

### 🏢 tenant-service
Manages multi-tenancy configuration for different municipalities.

**Key Features:**
- Tenant registration
- Municipality-specific settings
- Data isolation
- Configuration management

### 🚪 gateway-service
API gateway that routes requests, handles authentication, and provides a single entry point.

**Key Features:**
- Request routing
- JWT validation
- Rate limiting
- CORS handling

### 🔍 discovery-service
Eureka-based service registry for dynamic service discovery.

**Key Features:**
- Service registration
- Health monitoring
- Load balancing
- Service discovery

## Technology Stack

- **Java 17**
- **Spring Boot 3.x**
- **Spring Cloud (Eureka, Gateway)**
- **Spring Security (JWT)**
- **Spring Data JPA**
- **PostgreSQL**
- **Redis** (caching)
- **Maven**
- **Docker**

## API Authentication

All API requests (except `/api/auth/login` and `/api/auth/register`) require:

```http
Authorization: Bearer <JWT_TOKEN>
X-Tenant-ID: <tenant-id>
```

## Environment Variables

Each service requires:

```env
DB_URL=jdbc:postgresql://localhost:5432/dbname
DB_USER=username
DB_PASS=password
EUREKA_URL=http://localhost:8761/eureka
JWT_SECRET=your-secret-key
GEMINI_API_KEY=your-gemini-key  # extraction-service only
```

## Testing

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Generate coverage report
mvn jacoco:report
```

## Related Modules

- [Frontend Application](../src/) - React application
- [UI Components](../components/) - Reusable React components
- [Frontend Services](../services/) - Frontend API clients
- [Documentation](../docs/) - Technical documentation
