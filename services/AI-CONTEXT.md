# services

Frontend API client services for communicating with backend microservices in the Municipality Tax Management System.

<!-- AUTO-GENERATED: Purpose -->
## Purpose

The `services` directory provides a centralized API client layer for all frontend-to-backend communication. It abstracts HTTP requests, handles authentication, manages multi-tenancy, and transforms API responses into frontend-friendly formats. This service layer acts as the boundary between React components and backend microservices, ensuring consistent error handling, request configuration, and data transformation.

Each service module corresponds to a specific backend microservice or domain area (extraction, rules, forms, sessions), providing typed, reusable methods for API interactions.
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Key Files -->
## Key Files

| File | Description |
|------|-------------|
| `api.ts` | Main API client with axios configuration, interceptors, and JWT handling |
| `extractionMapper.ts` | Maps extraction service responses to frontend data models |
| `formSchemaService.ts` | Form schema loading, validation, and field resolution |
| `ruleService.ts` | Business rules evaluation and management client |
| `sessionService.ts` | Session management and persistence |
| `__tests__/` | Service layer unit tests |
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Service Architecture -->
## Service Architecture

### Layered Architecture

```
┌────────────────────────────────────┐
│      React Components              │
└──────────────┬─────────────────────┘
               │
┌──────────────▼─────────────────────┐
│      Service Layer (this dir)      │
│  ┌──────────────────────────────┐  │
│  │  api.ts (axios instance)     │  │ ← Core HTTP client
│  └──────────────────────────────┘  │
│  ┌──────────────────────────────┐  │
│  │  Domain Services             │  │ ← Specialized clients
│  │  - extractionMapper          │  │
│  │  - formSchemaService         │  │
│  │  - ruleService               │  │
│  │  - sessionService            │  │
│  └──────────────────────────────┘  │
└──────────────┬─────────────────────┘
               │
┌──────────────▼─────────────────────┐
│      Backend Microservices         │
│  - gateway-service                 │
│  - auth-service                    │
│  - extraction-service              │
│  - rule-service                    │
│  - ... (other services)            │
└────────────────────────────────────┘
```

### Request Flow

```
Component Call → Service Method → api.ts → Interceptors → Backend API
                                              ↓
                                        JWT Injection
                                        Tenant Header
                                              ↓
Backend Response → Transform → Return to Component
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Public API -->
## Public API

### `api.ts` - Core API Client

The main axios instance configured for backend communication:

```typescript
import { apiClient } from '@/services/api';

// GET request
const response = await apiClient.get<TaxReturn[]>('/api/tax-returns');
const taxReturns = response.data;

// POST request
const newReturn = await apiClient.post<TaxReturn>('/api/tax-returns', {
  taxYear: 2024,
  formType: 'SCHEDULE_X'
});

// PUT request
const updated = await apiClient.put<TaxReturn>(
  `/api/tax-returns/${id}`,
  updateData
);

// DELETE request
await apiClient.delete(`/api/tax-returns/${id}`);
```

**Features:**
- Automatic JWT token injection from localStorage
- Multi-tenant header injection (`X-Tenant-ID`)
- Request/response interceptors
- Error transformation
- Token refresh on 401 Unauthorized
- Base URL configuration from environment

**Configuration:**
```typescript
// Set tenant context
apiClient.defaults.headers.common['X-Tenant-ID'] = tenantId;

// Set authorization
apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
```

### `extractionMapper.ts` - Extraction Data Mapping

Maps AI extraction service responses to UI-friendly formats:

```typescript
import { ExtractionMapper } from '@/services/extractionMapper';

// Map extraction to form fields
const formData = ExtractionMapper.mapToFormFields(extractionResponse);

// Map extraction to review format
const reviewData = ExtractionMapper.mapToReviewFormat(extractionResponse);

// Extract field by name
const fieldValue = ExtractionMapper.getFieldValue(
  extractionResponse,
  'businessName'
);

// Get confidence score
const confidence = ExtractionMapper.getConfidenceScore(
  extractionResponse,
  'businessName'
);
```

**Responsibilities:**
- Transform extraction API responses
- Map extracted fields to form field names
- Calculate confidence scores
- Handle missing or low-confidence fields
- Normalize field values

**Mapping Example:**
```typescript
// Extraction service response
{
  "extractedFields": [
    { "name": "business_name", "value": "Acme Corp", "confidence": 0.95 },
    { "name": "tax_year", "value": "2024", "confidence": 0.99 }
  ]
}

// Mapped to frontend format
{
  "businessName": "Acme Corp",
  "taxYear": 2024,
  "_confidence": {
    "businessName": 0.95,
    "taxYear": 0.99
  }
}
```

### `formSchemaService.ts` - Form Schema Management

Loads and validates form schemas:

```typescript
import { FormSchemaService } from '@/services/formSchemaService';

// Load schema by ID
const schema = await FormSchemaService.loadSchema('schedule-x');

// Validate form data against schema
const validationResult = FormSchemaService.validate(formData, schema);
if (!validationResult.isValid) {
  console.error(validationResult.errors);
}

// Get field definition
const fieldDef = FormSchemaService.getField(schema, 'businessName');

// Get required fields
const requiredFields = FormSchemaService.getRequiredFields(schema);

// Get field by JSONPath
const fieldValue = FormSchemaService.getFieldByPath(
  schema,
  'section1.businessInfo.name'
);
```

**Capabilities:**
- Load schemas from backend or local cache
- Validate form data against Zod schemas
- Resolve field definitions
- Handle conditional fields
- Extract field metadata (type, validation, props)

**Schema Structure:**
```typescript
interface FormSchema {
  id: string;
  name: string;
  version: string;
  fields: FormField[];
}

interface FormField {
  name: string;
  label: string;
  type: 'text' | 'number' | 'date' | 'select' | ...;
  required: boolean;
  validation?: ZodSchema;
  props?: Record<string, any>;
}
```

### `ruleService.ts` - Business Rules Client

Interacts with the rule-service backend:

```typescript
import { RuleService } from '@/services/ruleService';

// Get all rules
const allRules = await RuleService.getRules();

// Get rules for specific form
const scheduleXRules = await RuleService.getRulesForForm('SCHEDULE_X');

// Evaluate rules against data
const evaluation = await RuleService.evaluateRules({
  formType: 'SCHEDULE_X',
  data: taxReturnData
});

// Check if data passes all rules
if (evaluation.passed) {
  console.log('All rules passed!');
} else {
  console.error('Rule failures:', evaluation.failures);
}

// Create custom rule
await RuleService.createRule({
  name: 'Custom Validation',
  formType: 'SCHEDULE_X',
  expression: 'data.grossReceipts > 0',
  errorMessage: 'Gross receipts must be greater than zero'
});

// Update rule
await RuleService.updateRule(ruleId, updatedRule);

// Delete rule
await RuleService.deleteRule(ruleId);
```

**Features:**
- Rule CRUD operations
- Rule evaluation
- Form-specific rule filtering
- Validation result transformation

**Evaluation Response:**
```typescript
interface RuleEvaluationResult {
  passed: boolean;
  failures: Array<{
    ruleId: string;
    ruleName: string;
    errorMessage: string;
    fieldName?: string;
  }>;
  warnings: Array<{
    ruleId: string;
    message: string;
  }>;
}
```

### `sessionService.ts` - Session Management

Manages user session state and JWT tokens:

```typescript
import { SessionService } from '@/services/sessionService';

// Store session
SessionService.setSession({
  token: jwtToken,
  user: userData,
  tenantId: tenantId,
  expiresAt: expirationTimestamp
});

// Get current session
const session = SessionService.getSession();
if (session && session.token) {
  // User is authenticated
}

// Check if session is valid
const isValid = SessionService.isSessionValid();

// Get JWT token
const token = SessionService.getToken();

// Clear session (logout)
SessionService.clearSession();

// Refresh token
const newToken = await SessionService.refreshToken();
```

**Storage:**
- Uses `localStorage` for persistence
- Handles token expiration
- Automatic session cleanup on logout
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Dependencies -->
## Dependencies

### Internal
- `../src/types/` - TypeScript type definitions
- `../src/hooks/` - Custom hooks (used by consumers)
- `../contexts/` - Auth and tenant contexts

### External
- `axios` - HTTP client
- `zod` - Schema validation (formSchemaService)
- `jwt-decode` - JWT token parsing (sessionService)
- `date-fns` - Date utilities
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Used By -->
## Used By

- `../src/hooks/useAuth.ts` - Uses sessionService
- `../src/hooks/useApi.ts` - Uses api.ts
- `../components/*` - All components use services for data
- `../src/components/*` - Reusable components use services
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Patterns & Conventions -->
## Patterns & Conventions

### Service Module Pattern
Each service is a class with static methods:

```typescript
export class ServiceName {
  // Private helper methods
  private static transform(data: ApiResponse): UiModel {
    // transformation logic
  }
  
  // Public API methods
  public static async getResource(id: string): Promise<Resource> {
    const response = await apiClient.get(`/api/resource/${id}`);
    return this.transform(response.data);
  }
  
  public static async createResource(data: CreateDto): Promise<Resource> {
    const response = await apiClient.post('/api/resource', data);
    return this.transform(response.data);
  }
}
```

### Error Handling
Services throw typed errors:

```typescript
import { ApiError } from '@/services/api';

try {
  const data = await RuleService.getRules();
} catch (error) {
  if (error instanceof ApiError) {
    console.error(`API Error (${error.statusCode}): ${error.message}`);
  }
}
```

### Type Safety
All service methods are fully typed:

```typescript
// Input and output types
public static async getReturn(id: string): Promise<TaxReturn> {
  const response = await apiClient.get<TaxReturnDto>(`/api/returns/${id}`);
  return this.mapDtoToModel(response.data);
}
```

### Response Transformation
Services transform backend DTOs to frontend models:

```typescript
private static mapDtoToModel(dto: TaxReturnDto): TaxReturn {
  return {
    id: dto.id,
    taxYear: dto.tax_year,           // snake_case → camelCase
    createdAt: new Date(dto.created_at), // string → Date
    amount: parseFloat(dto.amount),  // string → number
    // ... other transformations
  };
}
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Testing -->
## Testing

### Unit Tests
Located in `__tests__/`:

```bash
npm test services/
```

### Mocking Services
```typescript
import { vi } from 'vitest';
import { RuleService } from '@/services/ruleService';

// Mock service method
vi.spyOn(RuleService, 'getRules').mockResolvedValue([
  { id: '1', name: 'Test Rule', /* ... */ }
]);

// Test component that uses RuleService
const { result } = renderHook(() => useRules());
expect(result.current.rules).toHaveLength(1);
```

### Integration Tests
Test with real API calls in development:

```typescript
describe('RuleService Integration', () => {
  it('should fetch rules from backend', async () => {
    const rules = await RuleService.getRules();
    expect(rules).toBeInstanceOf(Array);
  });
});
```
<!-- END AUTO-GENERATED -->

<!-- CUSTOM SECTION: Project-Specific Notes -->
## Notes

Add service-specific configuration, API endpoint documentation, or authentication flow details here.

<!-- END CUSTOM SECTION -->

<!-- AUTO-GENERATED: Usage Examples -->
## Usage Examples

### Complete Request Flow
```typescript
import { apiClient } from '@/services/api';
import { RuleService } from '@/services/ruleService';
import { FormSchemaService } from '@/services/formSchemaService';

async function submitTaxReturn(formData: TaxReturnData) {
  // 1. Load form schema
  const schema = await FormSchemaService.loadSchema('schedule-x');
  
  // 2. Validate data
  const validation = FormSchemaService.validate(formData, schema);
  if (!validation.isValid) {
    throw new Error('Validation failed');
  }
  
  // 3. Evaluate business rules
  const ruleEvaluation = await RuleService.evaluateRules({
    formType: 'SCHEDULE_X',
    data: formData
  });
  if (!ruleEvaluation.passed) {
    throw new Error('Rule validation failed');
  }
  
  // 4. Submit to backend
  const response = await apiClient.post('/api/tax-returns', formData);
  
  return response.data;
}
```

### Error Handling
```typescript
import { apiClient, isApiError } from '@/services/api';

async function fetchData() {
  try {
    const response = await apiClient.get('/api/data');
    return response.data;
  } catch (error) {
    if (isApiError(error)) {
      if (error.statusCode === 401) {
        // Redirect to login
        window.location.href = '/login';
      } else if (error.statusCode === 403) {
        // Show permission error
        alert('Permission denied');
      } else {
        // Generic error
        console.error(error.message);
      }
    }
    throw error;
  }
}
```

### Multi-Tenant Requests
```typescript
import { apiClient } from '@/services/api';

async function getTenantData(tenantId: string) {
  const response = await apiClient.get('/api/tenant-data', {
    headers: {
      'X-Tenant-ID': tenantId
    }
  });
  return response.data;
}
```
<!-- END AUTO-GENERATED -->
