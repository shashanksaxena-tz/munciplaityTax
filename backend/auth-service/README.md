# auth-service

Authentication and User Management Service

## Overview

The auth-service handles all authentication and authorization for the Municipality Tax Management System. It provides JWT-based authentication, user registration, multi-profile management, and RBAC (Role-Based Access Control).

**Port:** 8081  
**Database:** PostgreSQL (`auth_db` **Technology:** Spring Boot 3.x, Spring Security, JWT

## Key Features

- ✅ **JWT Authentication** - Secure token-based authentication
- ✅ **User Registration** - Email/password registration with validation
- ✅ **Multi-Profile Support** - Users can have multiple tax profiles (individual + business)
- ✅ **Role-Based Access** - BUSINESS_USER, INDIVIDUAL_USER, ADMIN, AUDITOR, ACCOUNTANT
- ✅ **Password Management** - Password reset with email tokens
- ✅ **Multi-Tenancy** - Supports multiple municipalities
- ✅ **Email Verification** - Email verification workflow (configurable)

## Quick Start

### Run Service
```bash
cd backend/auth-service
mvn spring-boot:run
```

### Access
```
http://localhost:8081
```

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | User login, returns JWT |
| POST | `/api/auth/token/generate` | Generate JWT token |
| POST | `/api/auth/token/validate` | Validate JWT token |
| GET | `/api/auth/user/current` | Get current user info |

### User Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/register` | Register new user with profile |
| GET | `/api/users/{userId}/profiles` | Get all user profiles |
| POST | `/api/users/{userId}/profiles` | Create additional profile |
| PUT | `/api/users/{userId}/profiles/{id}` | Update profile |
| DELETE | `/api/users/{userId}/profiles/{id}` | Delete profile (soft delete) |
| POST | `/api/users/password-reset/initiate` | Start password reset |
| POST | `/api/users/password-reset/confirm` | Complete password reset |

## Data Models

### User
- Email (unique)
- Password (BCrypt hashed)
- Name, phone
- Roles (list)
- Tenant ID
- Email verification status
- Verification tokens

### UserProfile
- User ID reference
- Profile type (INDIVIDUAL | BUSINESS)
- SSN/EIN
- Business name (for business profiles)
- Address
- Primary flag
- Active status

## Configuration

```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000  # 1 hour
```

## Security

- Passwords hashed with BCrypt (strength 10)
- JWT tokens expire after 1 hour
- Password reset tokens expire after 1 hour
- Email verification tokens expire after 24 hours
- All endpoints except `/login` and `/register` require authentication

## Related Services

- [gateway-service](../gateway-service/) - Routes authentication requests
- [discovery-service](../discovery-service/) - Service registration
- All other services - Token validation

## Development

```bash
# Build
mvn clean install

# Run tests
mvn test

# Run with profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
