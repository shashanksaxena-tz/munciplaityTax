# Auth Service Login UI - Visual Reference

## Login Form Design

The LoginForm component (`components/auth/LoginForm.tsx`) creates a beautiful authentication experience:

### Visual Elements:

**Header Section:**
```
┌────────────────────────────────────┐
│                                    │
│        Welcome Back                │
│   Sign in to your MuniTax account │
│                                    │
└────────────────────────────────────┘
```

**Form Fields:**
```
┌────────────────────────────────────┐
│ Email Address                      │
│ ┌────────────────────────────────┐ │
│ │ you@example.com                │ │
│ └────────────────────────────────┘ │
│                                    │
│ Password                           │
│ ┌────────────────────────────────┐ │
│ │ ••••••••••              👁     │ │
│ └────────────────────────────────┘ │
│                                    │
│ ☐ Remember me    Forgot password?  │
│                                    │
│ ┌────────────────────────────────┐ │
│ │         Sign in                │ │
│ └────────────────────────────────┘ │
│                                    │
│ Don't have an account? Register now│
│                                    │
└────────────────────────────────────┘
```

### Color Scheme:
- Background: Gradient from blue-50 to indigo-100
- Card: White with rounded corners and shadow
- Primary button: Indigo-600 with hover effect
- Text: Gray-900 for headings, Gray-600 for body
- Borders: Gray-300 with indigo focus ring

### Interactive Features:
1. Password toggle (eye icon) - shows/hides password
2. Loading spinner appears during authentication
3. Error messages display in red banner at top
4. Hover effects on buttons and links
5. Form validation (required fields, email format)

### Responsive Design:
- Mobile: Full width with padding
- Tablet: Max width 28rem (448px)
- Desktop: Centered with max width

## Backend API Contract

**Login Request:**
```json
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Login Response (Success):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": "USER,ADMIN",
  "tenantId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Login Response (Error):**
```json
{
  "message": "Invalid credentials",
  "status": 401
}
```

## User Flow

1. User navigates to `/login`
2. LoginForm component renders
3. User enters email and password
4. Click "Sign in" button
5. AuthContext calls `api.auth.login(email, password)`
6. API makes POST request to backend
7. Backend validates credentials
8. Backend generates JWT token
9. Frontend stores token in localStorage
10. Frontend redirects to main application
11. Subsequent requests include token in Authorization header

## Integration with Backend

The UI connects to the auth-service through:

**API Layer** (`services/api.ts`):
```typescript
const API_BASE_URL = '/api/v1';  // Proxied to http://localhost:8081

auth: {
    login: async (credentials: {email: string, password: string}) => {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credentials)
        });
        return response.json();
    }
}
```

**Auth Context** (`contexts/AuthContext.tsx`):
- Manages authentication state
- Stores user information
- Handles token persistence
- Provides login/logout methods to components

## Testing Checklist

- [ ] Service compiles without errors
- [ ] Service builds successfully
- [ ] Service starts and connects to database
- [ ] UI renders correctly
- [ ] Form validation works
- [ ] Password toggle functions
- [ ] Login request reaches backend
- [ ] JWT token is generated
- [ ] Token is stored in localStorage
- [ ] Protected routes work with token
- [ ] Logout clears token
- [ ] Error messages display correctly
