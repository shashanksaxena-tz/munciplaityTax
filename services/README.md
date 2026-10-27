# Services

Frontend API client layer for backend microservice communication.

## Overview

The `services` directory provides a clean, typed interface for all frontend-backend communication. It abstracts HTTP requests, handles authentication, manages multi-tenancy, and transforms API responses into frontend-friendly formats.

**Key Benefits:**
- **Centralized API Logic**: All backend communication in one place
- **Type Safety**: Fully typed requests and responses
- **Consistent Error Handling**: Standardized error transformation
- **DRY Principle**: Reusable API methods across components

## Structure

```
services/
├── api.ts                    # Core axios client (JWT, interceptors)
├── extractionMapper.ts       # Document extraction mapping
├── formSchemaService.ts      # Form schema validation
├── ruleService.ts            # Business rules client
├── sessionService.ts         # Session management
└── __tests__/                # Service tests
```

## Quick Start

### Basic API Calls

```typescript
import { apiClient } from '@/services/api';

// GET request
const taxReturns = await apiClient.get('/api/tax-returns');

// POST request
const newReturn = await apiClient.post('/api/tax-returns', {
  taxYear: 2024,
  formType: 'SCHEDULE_X'
});

// With tenant context
const data = await apiClient.get('/api/resource', {
  headers: { 'X-Tenant-ID': tenantId }
});
```

### Using Domain Services

```typescript
import { RuleService } from '@/services/ruleService';
import { FormSchemaService } from '@/services/formSchemaService';

// Load form schema
const schema = await FormSchemaService.loadSchema('schedule-x');

// Validate data
const isValid = FormSchemaService.validate(formData, schema);

// Evaluate business rules
const rules = await RuleService.evaluateRules({
  formType: 'SCHEDULE_X',
  data: formData
});
```

## Services

### 🔌 api.ts - Core HTTP Client

Main axios instance with authentication and interceptors.

**Features:**
- Automatic JWT token injection
- Multi-tenant header support
- Request/response interceptors
- Token refresh on 401
- Error transformation

**Usage:**
```typescript
import { apiClient } from '@/services/api';

const response = await apiClient.get<TaxReturn[]>('/api/tax-returns');
```

### 🔄 extractionMapper.ts - Data Transformation

Maps AI extraction responses to UI models.

**Features:**
- Field name normalization
- Confidence score extraction
- Missing field handling
- Type conversion

**Usage:**
```typescript
import { ExtractionMapper } from '@/services/extractionMapper';

const formData = ExtractionMapper.mapToFormFields(extractionResponse);
const confidence = ExtractionMapper.getConfidenceScore(response, 'fieldName');
```

### 📋 formSchemaService.ts - Schema Management

Loads and validates form schemas.

**Features:**
- Schema loading from backend
- Zod schema validation
- Field definition resolution
- Required field extraction

**Usage:**
```typescript
import { FormSchemaService } from '@/services/formSchemaService';

const schema = await FormSchemaService.loadSchema('schedule-x');
const result = FormSchemaService.validate(formData, schema);
```

### 📐 ruleService.ts - Business Rules

Client for rule-service backend.

**Features:**
- Rule CRUD operations
- Rule evaluation
- Form-specific filtering
- Validation results

**Usage:**
```typescript
import { RuleService } from '@/services/ruleService';

const rules = await RuleService.getRulesForForm('SCHEDULE_X');
const evaluation = await RuleService.evaluateRules({ formType, data });
```

### 🔐 sessionService.ts - Session Management

Manages JWT tokens and user sessions.

**Features:**
- Session persistence (localStorage)
- Token expiration handling
- Session validation
- Token refresh

**Usage:**
```typescript
import { SessionService } from '@/services/sessionService';

SessionService.setSession({ token, user, tenantId });
const session = SessionService.getSession();
const isValid = SessionService.isSessionValid();
SessionService.clearSession(); // Logout
```

## Architecture

### Request Flow

```
Component
   ↓
Service Method (e.g., RuleService.getRules())
   ↓
api.ts (axios instance)
   ↓
Request Interceptor
  - Add JWT token
  - Add tenant header
   ↓
Backend API
   ↓
Response Interceptor
  - Transform errors
  - Handle 401 (refresh token)
   ↓
Service Method (transform response)
   ↓
Component
```

### Error Handling

All services use consistent error handling:

```typescript
try {
  const data = await RuleService.getRules();
} catch (error) {
  if (error.statusCode === 401) {
    // Unauthorized - redirect to login
  } else if (error.statusCode === 403) {
    // Forbidden - show permission error
  } else {
    // Generic error
    console.error(error.message);
  }
}
```

## Best Practices

### 1. Always Use Services
❌ **Bad:**
```typescript
// Direct fetch in component
const response = await fetch('/api/rules');
```

✅ **Good:**
```typescript
// Use service layer
const rules = await RuleService.getRules();
```

### 2. Type Your Responses
❌ **Bad:**
```typescript
const response = await apiClient.get('/api/data');
const data = response.data; // any type
```

✅ **Good:**
```typescript
const response = await apiClient.get<TaxReturn[]>('/api/data');
const data = response.data; // TaxReturn[]
```

### 3. Handle Errors
❌ **Bad:**
```typescript
const data = await RuleService.getRules(); // Could throw
```

✅ **Good:**
```typescript
try {
  const data = await RuleService.getRules();
} catch (error) {
  handleError(error);
}
```

### 4. Transform Responses
Services should transform backend DTOs to frontend models:

```typescript
// In service
private static mapDto(dto: BackendDto): FrontendModel {
  return {
    id: dto.id,
    name: dto.business_name, // snake_case → camelCase
    createdAt: new Date(dto.created_at), // string → Date
  };
}
```

## Testing

```bash
# Run service tests
npm test services/

# Watch mode
npm test services/ -- --watch

# Coverage
npm test services/ -- --coverage
```

### Mock Services in Tests

```typescript
import { vi } from 'vitest';
import { RuleService } from '@/services/ruleService';

vi.spyOn(RuleService, 'getRules').mockResolvedValue([
  { id: '1', name: 'Test Rule' }
]);
```

## Configuration

### Environment Variables

```env
# API Gateway URL
VITE_API_URL=http://localhost:8080

# Tenant ID (optional, can be set per-request)
VITE_TENANT_ID=municipality-1
```

### API Client Setup

```typescript
// Configure base URL
apiClient.defaults.baseURL = import.meta.env.VITE_API_URL;

// Set default headers
apiClient.defaults.headers.common['X-Tenant-ID'] = tenantId;
```

## Related Directories

- [`../src/hooks/`](../src/hooks/) - Custom hooks that use services
- [`../src/types/`](../src/types/) - TypeScript types for API contracts
- [`../backend/`](../backend/) - Backend microservices
- [`../components/`](../components/) - Components that consume services
