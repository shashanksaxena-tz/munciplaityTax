# MuniTax Data Security Documentation

## Overview

This document outlines the security architecture, controls, and best practices implemented in the MuniTax system to protect sensitive tax data.

---

## Security Architecture

```mermaid
flowchart TB
    subgraph "Public Zone"
        USER[User Browser]
        WAF[Web Application Firewall]
    end

    subgraph "DMZ"
        LB[Load Balancer<br/>TLS Termination]
        GW[API Gateway<br/>Rate Limiting]
    end

    subgraph "Application Zone"
        AUTH[Auth Service]
        SERVICES[Business Services]
    end

    subgraph "Data Zone"
        DB[(PostgreSQL<br/>Encrypted)]
        CACHE[(Redis<br/>In-Memory)]
    end

    subgraph "External Zone"
        GEMINI[Gemini AI]
        PAYMENT[Payment Gateway]
    end

    USER -->|HTTPS| WAF
    WAF --> LB
    LB -->|TLS| GW
    GW -->|JWT Validated| AUTH
    GW -->|Authorized| SERVICES
    
    SERVICES -->|TLS| DB
    SERVICES --> CACHE
    
    SERVICES -->|TLS| GEMINI
    SERVICES -->|TLS| PAYMENT
```

---

## Authentication & Authorization

### JWT-Based Authentication

```mermaid
sequenceDiagram
    participant User
    participant Gateway
    participant Auth
    participant Service

    User->>Gateway: Login Request
    Gateway->>Auth: Validate Credentials
    Auth->>Auth: Hash & Verify Password
    Auth-->>Gateway: JWT Token
    Gateway-->>User: Token + Refresh Token

    User->>Gateway: API Request + Bearer Token
    Gateway->>Gateway: Validate JWT Signature
    Gateway->>Gateway: Check Token Expiration
    Gateway->>Gateway: Extract User Context
    Gateway->>Service: Authorized Request
    Service-->>User: Response
```

### JWT Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-uuid",
    "username": "john.doe",
    "roles": ["ROLE_TAXPAYER"],
    "tenantId": "dublin",
    "iat": 1701388800,
    "exp": 1701392400
  },
  "signature": "..."
}
```

### Role-Based Access Control (RBAC)

```mermaid
graph TB
    subgraph "User Roles"
        IND[ROLE_INDIVIDUAL]
        BUS[ROLE_BUSINESS]
        AUD[ROLE_AUDITOR]
        SAUD[ROLE_SENIOR_AUDITOR]
        SUP[ROLE_SUPERVISOR]
        MGR[ROLE_MANAGER]
        ADM[ROLE_ADMIN]
    end

    subgraph "Permissions"
        FILE[File Own Returns]
        VIEW[View Assigned Returns]
        APPROVE[Approve Returns]
        ASSIGN[Assign Auditors]
        CONFIGURE[Configure Rules]
        ADMIN[System Administration]
    end

    IND --> FILE
    BUS --> FILE
    AUD --> VIEW
    SAUD --> VIEW
    SAUD --> APPROVE
    SUP --> VIEW
    SUP --> APPROVE
    SUP --> ASSIGN
    MGR --> VIEW
    MGR --> APPROVE
    MGR --> ASSIGN
    MGR --> CONFIGURE
    ADM --> ADMIN
```

### Permission Matrix

| Resource | Individual | Business | Auditor | Senior Auditor | Supervisor | Manager | Admin |
|----------|:----------:|:--------:|:-------:|:--------------:|:----------:|:-------:|:-----:|
| Own Tax Returns | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Assigned Returns | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Approve < $50K | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| Approve Any | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Assign Auditors | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Configure Rules | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| User Management | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## Data Encryption

### Encryption at Rest

```mermaid
flowchart LR
    subgraph "Application"
        APP[Service Layer]
    end

    subgraph "Encryption Layer"
        TDE[PostgreSQL TDE]
        LUKS[Disk Encryption]
    end

    subgraph "Storage"
        DB[(Database Files)]
        DISK[(Disk Volume)]
    end

    APP --> TDE
    TDE --> DB
    DB --> LUKS
    LUKS --> DISK
```

**Implementation:**
- PostgreSQL Transparent Data Encryption (TDE)
- AES-256 encryption for database files
- LUKS encryption for Docker volumes
- Encrypted backups with separate key management

### Encryption in Transit

```mermaid
flowchart LR
    subgraph "Client"
        BROWSER[Browser<br/>TLS 1.3]
    end

    subgraph "API Gateway"
        GW[Gateway<br/>TLS Termination]
    end

    subgraph "Services"
        SVC[Microservices<br/>mTLS]
    end

    subgraph "Database"
        DB[(PostgreSQL<br/>TLS)]
    end

    BROWSER -->|HTTPS| GW
    GW -->|TLS| SVC
    SVC -->|TLS| DB
```

**Implementation:**
- TLS 1.3 for all external connections
- Certificate-based authentication (mTLS) for internal services
- Strong cipher suites only
- HSTS headers enforced

---

## Network Security

### Network Segmentation

```mermaid
flowchart TB
    subgraph "Internet"
        INET[Public Internet]
    end

    subgraph "Public Subnet"
        LB[Load Balancer]
        WAF[WAF]
    end

    subgraph "Application Subnet"
        GW[Gateway]
        DISC[Discovery]
        AUTH[Auth]
        SERVICES[Services]
    end

    subgraph "Data Subnet"
        DB[(PostgreSQL)]
        REDIS[(Redis)]
    end

    INET --> WAF
    WAF --> LB
    LB --> GW
    GW --> AUTH
    GW --> SERVICES
    AUTH --> DB
    SERVICES --> DB
    AUTH --> REDIS
```

### Firewall Rules

| Source | Destination | Port | Protocol | Purpose |
|--------|-------------|------|----------|---------|
| Internet | Load Balancer | 443 | HTTPS | User access |
| Load Balancer | Gateway | 8080 | HTTP | Internal routing |
| Gateway | Services | 8081-8088 | HTTP | Service communication |
| Services | PostgreSQL | 5432 | PostgreSQL | Database access |
| Services | Redis | 6379 | Redis | Cache access |

---

## Application Security

### Input Validation

```java
// Example: Validation on SSN field
@Pattern(regexp = "^\\d{3}-\\d{2}-\\d{4}$", message = "Invalid SSN format")
@NotBlank(message = "SSN is required")
private String ssn;

// Example: Validation on tax amounts
@PositiveOrZero(message = "Amount must be positive")
@DecimalMax(value = "999999999.99", message = "Amount exceeds maximum")
private BigDecimal taxableIncome;
```

### SQL Injection Prevention
- JPA/Hibernate parameterized queries
- No raw SQL construction
- Input sanitization at API layer

### XSS Prevention
- React automatic escaping
- Content Security Policy headers
- Input sanitization

### CSRF Protection
- JWT tokens instead of cookies
- SameSite cookie attributes
- Custom headers required

---

## Audit Logging

### Audit Trail Schema

```mermaid
erDiagram
    AUDIT_TRAIL {
        uuid trail_id PK
        uuid return_id FK
        string event_type
        uuid user_id
        timestamp timestamp
        string ip_address
        text event_details
        string digital_signature
        boolean immutable
        string tenant_id
    }
    
    AUDIT_EVENT_TYPE {
        SUBMISSION
        ASSIGNMENT
        REVIEW_STARTED
        REVIEW_COMPLETED
        APPROVAL
        REJECTION
        AMENDMENT
        PAYMENT
        DOCUMENT_REQUEST
        PRIORITY_CHANGE
    }
```

### Logged Events

| Event Category | Events Logged | Retention |
|----------------|---------------|-----------|
| Authentication | Login, Logout, Failed Attempts | 2 years |
| Tax Returns | Create, Update, Submit, Amend | 7 years |
| Auditor Actions | Assign, Review, Approve, Reject | 7 years |
| Document Requests | Request, Receive, Waive | 7 years |
| Payments | Initiate, Complete, Refund | 7 years |
| Admin Actions | Rule Changes, User Management | 7 years |

### Audit Log Security
- Append-only storage
- Cryptographic signing
- Tamper detection
- Separate audit database (planned)

---

## Secret Management

### Secret Categories

| Secret Type | Storage | Rotation | Access |
|-------------|---------|----------|--------|
| JWT Secret | Environment | 90 days | Auth Service only |
| Database Password | Environment | 90 days | Database services |
| API Keys | Environment | On compromise | Service-specific |
| Encryption Keys | HSM (planned) | Annually | Encryption layer |

### Current Implementation

```yaml
# docker-compose.yml (development)
environment:
  JWT_SECRET: ${JWT_SECRET}
  SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
  GEMINI_API_KEY: ${GEMINI_API_KEY}
```

### Production Recommendations
- Use HashiCorp Vault or AWS Secrets Manager
- Implement automatic secret rotation
- Never commit secrets to source control
- Use service accounts with minimal permissions

---

## Security Headers

### HTTP Security Headers

```nginx
# nginx.conf
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

---

## API Security

### Rate Limiting

| Endpoint Category | Rate Limit | Window | Penalty |
|-------------------|------------|--------|---------|
| Authentication | 5 requests | 1 minute | 15 min lockout |
| Tax Calculation | 20 requests | 1 minute | Slow down |
| Document Extraction | 10 requests | 5 minutes | Slow down |
| General API | 100 requests | 1 minute | 429 response |

### Request Validation

```mermaid
flowchart LR
    REQ[Request] --> SIZE[Size Check<br/>Max 10MB]
    SIZE --> TYPE[Content-Type<br/>Validation]
    TYPE --> SCHEMA[JSON Schema<br/>Validation]
    SCHEMA --> SANITIZE[Input<br/>Sanitization]
    SANITIZE --> AUTH[Authorization<br/>Check]
    AUTH --> HANDLER[Request Handler]
```

---

## Vulnerability Management

### Security Scanning

| Scan Type | Tool | Frequency | Target |
|-----------|------|-----------|--------|
| SAST | CodeQL | Every commit | Source code |
| SCA | Dependabot | Daily | Dependencies |
| DAST | OWASP ZAP | Weekly | Running app |
| Container | Trivy | Every build | Docker images |

### Patch Management
- Critical vulnerabilities: 24 hours
- High vulnerabilities: 7 days
- Medium vulnerabilities: 30 days
- Low vulnerabilities: 90 days

---

## Incident Response

### Security Incident Categories

| Severity | Examples | Response Time | Escalation |
|----------|----------|---------------|------------|
| Critical | Data breach, System compromise | Immediate | Executive team |
| High | Authentication bypass, Privilege escalation | 4 hours | Security lead |
| Medium | Excessive access, Policy violation | 24 hours | Team lead |
| Low | Failed login attempts, Minor policy gaps | 72 hours | Regular process |

### Incident Response Steps

```mermaid
flowchart TB
    DETECT[Detect] --> CONTAIN[Contain]
    CONTAIN --> ERADICATE[Eradicate]
    ERADICATE --> RECOVER[Recover]
    RECOVER --> LESSONS[Lessons Learned]
    LESSONS --> IMPROVE[Improve Controls]
```

---

## Compliance Controls

### IRS Publication 1075 Alignment

| Control | Implementation |
|---------|----------------|
| Access Controls | RBAC, MFA (planned) |
| Audit Trails | Immutable logging |
| Data Encryption | TLS + TDE |
| Physical Security | Cloud provider (AWS/GCP) |
| Personnel Security | Background checks (organizational) |
| Incident Response | Documented procedures |

### SOC 2 Considerations

- Security: Access controls, encryption
- Availability: Redundancy, backups
- Processing Integrity: Validation, audit trails
- Confidentiality: Encryption, access controls
- Privacy: PII handling, consent management

---

## Security Checklist

### Pre-Production

- [ ] All secrets in secure vault
- [ ] TLS certificates valid and monitored
- [ ] Rate limiting enabled
- [ ] Audit logging enabled
- [ ] Security headers configured
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention verified
- [ ] XSS prevention verified
- [ ] CSRF protection enabled
- [ ] Error messages sanitized
- [ ] Dependency vulnerabilities addressed
- [ ] Container images scanned
- [ ] Network segmentation in place
- [ ] Backup encryption verified

### Ongoing

- [ ] Security patches applied within SLA
- [ ] Access reviews quarterly
- [ ] Penetration testing annually
- [ ] Incident response drills semi-annually
- [ ] Security training for developers
- [ ] Audit log reviews weekly

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-01 | Initial security documentation |

---

**Document Owner:** Security Team  
**Last Updated:** December 1, 2025  
**Review Frequency:** Quarterly  
**Classification:** Internal Use Only
