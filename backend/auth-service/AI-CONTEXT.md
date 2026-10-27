# auth-service

Authentication and user management microservice for JWT-based authentication, user registration, and profile management.

<!-- AUTO-GENERATED: Purpose -->
## Purpose

The auth-service is responsible for all authentication and authorization operations in the municipality tax management system. It handles user registration, login, JWT token generation and validation, email verification, password reset, and multi-profile management. This service acts as the security gateway for all other microservices, issuing JWT tokens that are validated across the service mesh.

The service supports multi-tenancy, role-based access control (RBAC), and allows users to manage multiple tax profiles (individual and business) under a single account.
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Key Files -->
## Key Files

| File/Package | Description |
|--------------|-------------|
| `AuthServiceApplication.java` | Spring Boot main application class with `@EnableDiscoveryClient` |
| **controller/** |
| `AuthController.java` | REST endpoints for login, token generation, validation, and user info |
| `UserController.java` | User management endpoints |
| **service/** |
| `CustomUserDetailsService.java` | Spring Security UserDetailsService implementation |
| `UserManagementService.java` | User registration, email verification, password reset, profile management |
| **model/** |
| `User.java` | User entity with authentication details |
| `UserProfile.java` | Tax filer profile entity (individual/business) |
| `AuthDto.java` | DTOs for authentication requests/responses |
| **repository/** |
| `UserRepository.java` | JPA repository for User entity |
| `UserProfileRepository.java` | JPA repository for UserProfile entity |
| **config/** |
| `SecurityConfig.java` | Spring Security configuration |
| `DataInitializer.java` | Initial data seeding |
| **util/** |
| `JwtUtil.java` | JWT token generation and validation utilities |
| **resources/** |
| `application.yml` | Service configuration |
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Public API -->
## Public API

### Authentication Endpoints

#### POST `/api/auth/login`
Authenticates user and returns JWT token.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "uuid-123",
  "email": "user@example.com",
  "roles": "BUSINESS_USER,ADMIN",
  "message": "Login successful"
}
```

#### POST `/api/auth/token/generate`
Generates JWT token for authenticated user.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "uuid-123"
}
```

#### POST `/api/auth/token/validate`
Validates a JWT token.

**Request:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "valid": true,
  "message": "Token is valid"
}
```

#### GET `/api/auth/user/current`
Gets current authenticated user information.

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Response:**
```json
{
  "userId": "uuid-123",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": "BUSINESS_USER",
  "tenantId": "municipality-1"
}
```

### User Management Endpoints

#### POST `/api/users/register`
Registers a new user with profile.

**Request:**
```json
{
  "email": "newuser@example.com",
  "password": "SecurePass123!",
  "firstName": "Jane",
  "lastName": "Smith",
  "phoneNumber": "+1234567890",
  "userRole": "BUSINESS_USER",
  "profileType": "BUSINESS",
  "ssnOrEin": "12-3456789",
  "businessName": "Acme Corp",
  "fiscalYearEnd": "12-31",
  "address": {
    "street": "123 Main St",
    "city": "Pittsburgh",
    "state": "PA",
    "zipCode": "15222"
  },
  "tenantId": "municipality-1"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Registration successful. You can now log in.",
  "userId": "uuid-456"
}
```

#### POST `/api/users/profiles`
Creates additional profile for user.

**Request:**
```json
{
  "type": "INDIVIDUAL",
  "name": "John Doe Personal",
  "ssnOrEin": "123-45-6789",
  "address": {
    "street": "456 Oak Ave",
    "city": "Pittsburgh",
    "state": "PA",
    "zipCode": "15222"
  },
  "relationshipToUser": "Self"
}
```

#### GET `/api/users/{userId}/profiles`
Gets all profiles for a user.

#### PUT `/api/users/{userId}/profiles/{profileId}`
Updates a user profile.

#### DELETE `/api/users/{userId}/profiles/{profileId}`
Soft-deletes a profile (cannot delete primary profile).

### Password Management

#### POST `/api/users/password-reset/initiate`
Initiates password reset flow.

#### POST `/api/users/password-reset/confirm`
Confirms password reset with token.
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Data Models -->
## Data Models

### User Entity
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<UserRole> roles;
    
    private String tenantId;
    private boolean emailVerified;
    
    // Verification tokens
    private String emailVerificationToken;
    private LocalDateTime emailVerificationExpiry;
    private String passwordResetToken;
    private LocalDateTime passwordResetExpiry;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**User Roles:**
- `BUSINESS_USER` - Business taxpayer
- `INDIVIDUAL_USER` - Individual taxpayer
- `ADMIN` - System administrator
- `AUDITOR` - Municipal auditor
- `ACCOUNTANT` - Accounting professional

### UserProfile Entity
```java
@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String userId;
    
    @Enumerated(EnumType.STRING)
    private ProfileType type;  // INDIVIDUAL or BUSINESS
    
    private String name;
    private String ssnOrEin;
    private boolean isPrimary;
    private boolean active;
    
    // Business-specific fields
    private String businessName;
    private String fiscalYearEnd;
    
    @Embedded
    private Address address;
    
    private String relationshipToUser;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Profile Types:**
- `INDIVIDUAL` - Personal tax profile
- `BUSINESS` - Business tax profile

### Address Embeddable
```java
@Embeddable
public class Address {
    private String street;
    private String street2;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Dependencies -->
## Dependencies

### Internal
- `discovery-service` - Registers with Eureka
- `gateway-service` - Accessed through API gateway

### External
- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - Database access
- `spring-boot-starter-security` - Security framework
- `spring-cloud-starter-netflix-eureka-client` - Service discovery
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` - JWT token handling
- `postgresql` - Database driver
- `bcrypt` - Password hashing
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Used By -->
## Used By

- `gateway-service` - Routes authentication requests
- All other microservices - Validate JWT tokens
- Frontend application - User authentication and registration
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Patterns & Conventions -->
## Patterns & Conventions

### Security Architecture

**JWT Token Flow:**
```
1. User → POST /api/auth/login
2. auth-service validates credentials
3. auth-service generates JWT token
4. Client receives token
5. Client includes token in subsequent requests:
   Authorization: Bearer {token}
6. Other services validate token with auth-service
```

**JWT Token Structure:**
```java
{
  "sub": "user-email@example.com",
  "userId": "uuid-123",
  "roles": ["BUSINESS_USER"],
  "tenantId": "municipality-1",
  "iat": 1234567890,
  "exp": 1234571490  // 1 hour expiry
}
```

### Password Security
- Passwords hashed with BCrypt (strength 10)
- Minimum password requirements enforced
- Password reset tokens expire after 1 hour
- Email verification tokens expire after 24 hours

### Multi-Profile Support
```java
// User can have multiple profiles
User user1 {
  id: "user-1",
  email: "john@example.com",
  profiles: [
    {
      id: "profile-1",
      type: BUSINESS,
      businessName: "Acme Corp",
      isPrimary: true
    },
    {
      id: "profile-2",
      type: INDIVIDUAL,
      name: "John Doe",
      isPrimary: false
    }
  ]
}
```

### Error Handling
All endpoints return consistent error responses:
```json
{
  "success": false,
  "message": "Email already registered",
  "userId": null
}
```

### Multi-Tenancy
Every user belongs to a tenant (municipality):
```java
user.setTenantId("municipality-1");
```

Headers for multi-tenant requests:
```
X-Tenant-ID: municipality-1
```
<!-- END AUTO-GENERATED -->

<!-- CUSTOM SECTION: Project-Specific Notes -->
## Notes

Add authentication-specific notes, security considerations, or deployment requirements here.

<!-- END CUSTOM SECTION -->

<!-- AUTO-GENERATED: Configuration -->
## Configuration

### application.yml
```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: ${DB_USER}
    password: ${DB_PASS}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000  # 1 hour in milliseconds
```

### Environment Variables
```bash
DB_USER=auth_user
DB_PASS=secure_password
JWT_SECRET=your-256-bit-secret-key
EUREKA_URL=http://localhost:8761/eureka
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Usage Examples -->
## Usage Examples

### User Registration
```bash
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "business@example.com",
    "password": "SecurePass123!",
    "firstName": "Jane",
    "lastName": "Business",
    "phoneNumber": "+1234567890",
    "userRole": "BUSINESS_USER",
    "profileType": "BUSINESS",
    "ssnOrEin": "12-3456789",
    "businessName": "ABC Corp",
    "fiscalYearEnd": "12-31",
    "address": {
      "street": "123 Main St",
      "city": "Pittsburgh",
      "state": "PA",
      "zipCode": "15222"
    },
    "tenantId": "pittsburgh"
  }'
```

### Login
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "business@example.com",
    "password": "SecurePass123!"
  }'
```

### Get Current User
```bash
curl -X GET http://localhost:8081/api/auth/user/current \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Create Additional Profile
```bash
curl -X POST http://localhost:8081/api/users/user-123/profiles \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INDIVIDUAL",
    "name": "John Doe Personal",
    "ssnOrEin": "123-45-6789",
    "address": {
      "street": "456 Oak Ave",
      "city": "Pittsburgh",
      "state": "PA",
      "zipCode": "15222"
    },
    "relationshipToUser": "Self"
  }'
```
<!-- END AUTO-GENERATED -->
