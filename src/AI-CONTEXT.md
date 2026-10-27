# src

Core frontend application source code containing components, services, hooks, utilities, and types for the Municipality Tax Management System.

<!-- AUTO-GENERATED: Purpose -->
## Purpose

The `src` directory contains the foundational frontend code for the tax management application. It provides a well-organized structure following React best practices with clear separation of concerns: presentational components, business logic hooks, API service layer, utility functions, and TypeScript type definitions. 

This directory is the heart of the frontend application, providing reusable building blocks used by the top-level application components (like `App.tsx`, `TaxFilingApp.tsx`) located in the project root.
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Key Files -->
## Key Files

| File/Directory | Description |
|---------------|-------------|
| `components/` | Reusable UI components used across the application |
| `services/` | API client services for backend communication |
| `hooks/` | Custom React hooks for state management and side effects |
| `utils/` | Utility functions and helpers |
| `types/` | TypeScript type definitions and interfaces |
| `constants/` | Application constants and configuration |
| `__tests__/` | Unit and integration tests |
| `test/` | Test utilities and setup |
| `vite-env.d.ts` | Vite environment type definitions |
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Directory Structure -->
## Directory Structure

### components/
Contains reusable React components organized by feature or domain:

```typescript
// Example: Form components
src/components/
├── FormField.tsx        // Generic form field component
├── FormSection.tsx      // Form section wrapper
├── ValidationMessage.tsx // Validation feedback
└── ... (24 components total)
```

**Key Components:**
- Form-related components (FormField, FormSection, etc.)
- UI primitives (Button, Input, Select, etc.)
- Business-specific components (TaxCalculator, DocumentUpload, etc.)

### services/
API communication layer with backend microservices:

```typescript
src/services/
├── api.ts                    // Main API client with interceptors
├── extractionMapper.ts       // Maps extraction responses to UI models
├── formSchemaService.ts      // Form schema validation service
├── ruleService.ts            // Business rules client
├── sessionService.ts         // Session management
└── __tests__/               // Service tests
```

**Architecture:**
- Centralized axios configuration
- JWT token management
- Request/response interceptors
- Error handling
- Multi-tenant header injection

### hooks/
Custom React hooks for encapsulating business logic and state:

```typescript
src/hooks/
├── useAuth.ts              // Authentication state and methods
├── useFormValidation.ts    // Form validation logic
├── useApi.ts               // API call management
├── useLocalStorage.ts      // Local storage persistence
├── ... (8 hooks total)
```

**Patterns:**
- Separation of concerns (UI vs. logic)
- Reusable stateful logic
- Side effect management
- Custom event handling

### types/
TypeScript interfaces and types for type safety:

```typescript
src/types/
├── index.ts              // Re-exports all types
├── auth.types.ts         // Authentication types
├── tax.types.ts          // Tax calculation types
├── form.types.ts         // Form field types
├── api.types.ts          // API response types
├── ... (8 type files)
```

**Type System:**
- Strict type checking
- API contract validation
- Redux/Context state typing
- Component prop types

### utils/
Utility functions and helpers:

```typescript
src/utils/
├── formatters.ts         // Number, date, currency formatting
├── validators.ts         // Validation functions
├── calculations.ts       // Tax calculations
├── helpers.ts            // General utilities
```

### constants/
Application-wide constants:

```typescript
src/constants/
├── index.ts              // Application constants
└── ... (form constants, API endpoints, etc.)
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Public API -->
## Public API

### Key Services

#### `api.ts` - Main API Client
```typescript
import { apiClient } from '@/services/api';

// Authenticated requests
const response = await apiClient.get('/api/resource');

// Multi-tenant requests
const data = await apiClient.post('/api/resource', {
  // payload
}, {
  headers: { 'X-Tenant-ID': tenantId }
});
```

**Features:**
- Axios instance with JWT interceptors
- Automatic token refresh
- Error handling middleware
- Request/response logging
- Multi-tenant support

#### `formSchemaService.ts` - Form Schema Management
```typescript
import { FormSchemaService } from '@/services/formSchemaService';

// Load form schema
const schema = await FormSchemaService.loadSchema('schedule-x');

// Validate form data
const result = FormSchemaService.validate(formData, schema);
```

#### `ruleService.ts` - Business Rules
```typescript
import { RuleService } from '@/services/ruleService';

// Evaluate business rules
const evaluation = await RuleService.evaluateRules(taxData);

// Get applicable rules
const rules = await RuleService.getRulesForForm('W1');
```

### Key Hooks

#### `useAuth.ts`
```typescript
import { useAuth } from '@/hooks/useAuth';

function MyComponent() {
  const { user, isAuthenticated, login, logout } = useAuth();
  
  // Use authentication state
}
```

#### `useFormValidation.ts`
```typescript
import { useFormValidation } from '@/hooks/useFormValidation';

function TaxForm() {
  const { errors, validate, isValid } = useFormValidation(schema);
  
  // Form validation logic
}
```

### Key Types

#### Tax Types
```typescript
import type { TaxReturn, TaxCalculation } from '@/types/tax.types';

interface TaxReturn {
  id: string;
  tenantId: string;
  taxYear: number;
  formType: 'W1' | 'W2' | 'SCHEDULE_X' | 'SCHEDULE_Y';
  status: 'DRAFT' | 'SUBMITTED' | 'APPROVED';
  // ... other fields
}
```

#### Form Types
```typescript
import type { FormField, FormSchema } from '@/types/form.types';

interface FormSchema {
  id: string;
  name: string;
  version: string;
  fields: FormField[];
}
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Dependencies -->
## Dependencies

### Internal
- `../components/` - Top-level application components
- `../contexts/` - React context providers
- `../config/` - Configuration files

### External
- `react` - UI library
- `react-router-dom` - Routing
- `axios` - HTTP client
- `@tanstack/react-query` - Server state management
- `zod` - Schema validation
- `date-fns` - Date utilities
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Used By -->
## Used By

- `../App.tsx` - Main application component
- `../TaxFilingApp.tsx` - Tax filing workflow
- `../components/**` - Top-level components
- `../index.tsx` - Application entry point
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Patterns & Conventions -->
## Patterns & Conventions

### Component Organization
Each component follows:
```typescript
// 1. Imports
import React from 'react';
import type { ComponentProps } from './types';

// 2. Types/Interfaces
interface Props {
  // ...
}

// 3. Component
export function Component({ prop }: Props) {
  // 4. Hooks
  const [state, setState] = useState();
  
  // 5. Effects
  useEffect(() => {}, []);
  
  // 6. Handlers
  const handleClick = () => {};
  
  // 7. Render
  return <div>{/* ... */}</div>;
}
```

### Service Layer Pattern
Services encapsulate API communication:

```typescript
// Service module structure
export class ServiceName {
  // Private methods for API calls
  private static async fetchData() { }
  
  // Public methods for business operations
  public static async getResource() { }
  public static async createResource() { }
}
```

### Custom Hook Pattern
Hooks encapsulate reusable logic:

```typescript
export function useCustomHook(param: Type) {
  const [state, setState] = useState();
  
  useEffect(() => {
    // Side effects
  }, [param]);
  
  // Return API
  return {
    data: state,
    methods: {}
  };
}
```

### Type Organization
Types are organized by domain:

```typescript
// types/domain.types.ts
export interface Entity {
  id: string;
  // fields
}

export type EntityStatus = 'ACTIVE' | 'INACTIVE';

export interface EntityDto extends Entity {
  // additional fields
}
```
<!-- END AUTO-GENERATED -->

<!-- CUSTOM SECTION: Project-Specific Notes -->
## Notes

Add project-specific patterns, conventions, or important implementation details here.

<!-- END CUSTOM SECTION -->

<!-- AUTO-GENERATED: Usage Examples -->
## Usage Examples

### Making API Calls
```typescript
import { apiClient } from '@/services/api';

// GET request
const fetchTaxReturns = async (tenantId: string) => {
  const response = await apiClient.get('/api/tax-returns', {
    headers: { 'X-Tenant-ID': tenantId }
  });
  return response.data;
};

// POST request
const createReturn = async (data: TaxReturnDto) => {
  const response = await apiClient.post('/api/tax-returns', data);
  return response.data;
};
```

### Using Custom Hooks
```typescript
import { useAuth } from '@/hooks/useAuth';
import { useFormValidation } from '@/hooks/useFormValidation';

function TaxFilingForm() {
  const { user } = useAuth();
  const { errors, validate } = useFormValidation(formSchema);
  
  const handleSubmit = async (data: FormData) => {
    if (validate(data)) {
      await submitTaxReturn(data, user.tenantId);
    }
  };
  
  return (/* ... */);
}
```

### Type-Safe Components
```typescript
import type { TaxReturn } from '@/types/tax.types';

interface Props {
  taxReturn: TaxReturn;
  onUpdate: (data: Partial<TaxReturn>) => void;
}

export function TaxReturnCard({ taxReturn, onUpdate }: Props) {
  // Component implementation
}
```
<!-- END AUTO-GENERATED -->
