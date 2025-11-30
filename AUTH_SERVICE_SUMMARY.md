# Auth Service - Ready for Testing

## Overview
The **auth-service** meets all requirements:
- ✅ Requires database (PostgreSQL)
- ✅ Has UI integration (LoginForm component)
- ✅ No compilation errors
- ✅ Fully testable

## Service Details

### Backend (auth-service)
**Location**: `backend/auth-service/`

**Database Configuration** (application.yml):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://pg-1ac838cf-taazaa-6110.g.aivencloud.com:23581/defaultdb?ssl=require
    username: avnadmin
    password: AVNS_CIZODtyC1VgMeY5KcC3
```

**Port**: 8081

**REST Endpoints**:
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/register` - User registration
- `GET /api/v1/auth/me` - Get current user info
- `POST /api/v1/auth/validate` - Validate JWT token

**Entities**:
- User (users table)
- UserProfile (user_profiles table)

**Build Status**: ✅ SUCCESS
```bash
cd backend/auth-service
mvn clean package -DskipTests
# Result: BUILD SUCCESS (builds auth-service-0.0.1-SNAPSHOT.jar)
```

### Frontend UI

**Login Component**: `components/auth/LoginForm.tsx`

**Features**:
- Modern, responsive design with Tailwind CSS
- Email/password authentication
- Show/hide password toggle
- Remember me checkbox
- Forgot password link
- Registration link
- Loading states
- Error handling
- Form validation

**Integration**: Uses `AuthContext` and `api.auth.login()` to connect to backend

**API Service**: `services/api.ts`
```typescript
auth: {
    login: async (credentials) => {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credentials)
        });
        return response.json();
    }
}
```

## Testing Instructions

### 1. Start the Service
```bash
cd backend/auth-service
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

### 2. Verify Health
```bash
curl http://localhost:8081/actuator/health
```

### 3. Test Login Endpoint
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

### 4. Access UI
1. Start the frontend: `npm run dev`
2. Navigate to: `http://localhost:5173/login`
3. Use the login form to authenticate

## Database Schema

The service automatically creates tables via JPA/Hibernate:

**users table**:
- id (UUID, PK)
- email (unique)
- password (hashed)
- first_name
- last_name
- created_at
- updated_at

**user_profiles table**:
- id (UUID, PK)
- user_id (FK to users)
- tenant_id (UUID)
- roles (comma-separated)
- additional profile data

## Security Features

1. **Password Encryption**: BCrypt hashing
2. **JWT Tokens**: Secure token generation with configurable expiration
3. **Role-Based Access**: User roles stored in profiles
4. **Multi-tenancy**: Tenant isolation via tenant_id

## Next Steps

1. Ensure PostgreSQL database is accessible
2. Start the auth-service
3. Start the frontend application
4. Test login functionality through the UI
5. Verify JWT token generation and validation
6. Test protected routes

## Screenshots

The login form provides a professional user experience with:
- Clean, modern design
- Gradient background (blue to indigo)
- Card-based layout with shadow
- Input validation
- Password visibility toggle
- Responsive layout for all screen sizes
