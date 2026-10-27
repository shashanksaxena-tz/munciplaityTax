# src

Frontend application source code for the Municipality Tax Management System.

## Overview

The `src` directory contains the core frontend code organized into components, services, hooks, utilities, and type definitions. This modular architecture promotes code reusability, maintainability, and type safety throughout the React application.

**Purpose:**
- Provides reusable building blocks for the tax management UI
- Implements service layer for backend communication
- Defines custom hooks for business logic encapsulation
- Establishes TypeScript contracts for type safety

## Structure

```
src/
├── components/         # 24 reusable UI components
├── services/          # API clients and business services
│   ├── api.ts                   # Main API client
│   ├── extractionMapper.ts     # Document extraction mapping
│   ├── formSchemaService.ts    # Form validation
│   ├── ruleService.ts          # Business rules
│   └── sessionService.ts       # Session management
├── hooks/             # 8 custom React hooks
├── types/             # TypeScript type definitions
├── utils/             # Utility functions
├── constants/         # Application constants
└── __tests__/         # Unit and integration tests
```

## Quick Start

### Import Components
```typescript
import { FormField } from '@/src/components/FormField';
import { ValidationMessage } from '@/src/components/ValidationMessage';
```

### Use Services
```typescript
import { apiClient } from '@/src/services/api';

// Make authenticated API calls
const data = await apiClient.get('/api/resource');
```

### Use Hooks
```typescript
import { useAuth } from '@/src/hooks/useAuth';

function MyComponent() {
  const { user, login, logout } = useAuth();
  // ...
}
```

### Import Types
```typescript
import type { TaxReturn, FormSchema } from '@/src/types';
```

## Key Features

### 🧩 Component Library
24 reusable components including:
- Form controls (FormField, FormSection)
- Validation UI (ValidationMessage)
- Business components (TaxCalculator, DocumentUpload)
- UI primitives (Button, Input, Select)

### 🔌 Service Layer
Centralized API communication with:
- JWT authentication
- Multi-tenant support
- Request/response interceptors
- Error handling
- Type-safe API clients

### 🪝 Custom Hooks
8 hooks for common patterns:
- `useAuth` - Authentication state
- `useFormValidation` - Form validation logic
- `useApi` - API call management
- `useLocalStorage` - Persistent storage
- And more...

### 📘 Type System
Comprehensive TypeScript types for:
- Authentication (`auth.types.ts`)
- Tax calculations (`tax.types.ts`)
- Forms (`form.types.ts`)
- API contracts (`api.types.ts`)

### 🛠️ Utilities
Helper functions for:
- Number/currency formatting
- Date manipulation
- Validation
- Calculations

## Service Layer

### API Client (`api.ts`)

Main axios instance with authentication:

```typescript
import { apiClient } from '@/src/services/api';

// GET with auth
const response = await apiClient.get('/api/tax-returns');

// POST with tenant header
await apiClient.post('/api/resource', data, {
  headers: { 'X-Tenant-ID': tenantId }
});
```

**Features:**
- Automatic JWT token injection
- Token refresh on 401
- Multi-tenant header support
- Error boundary integration

### Form Schema Service

Validates form data against schemas:

```typescript
import { FormSchemaService } from '@/src/services/formSchemaService';

const schema = await FormSchemaService.loadSchema('schedule-x');
const isValid = FormSchemaService.validate(formData, schema);
```

### Rule Service

Evaluates business rules:

```typescript
import { RuleService } from '@/src/services/ruleService';

const rules = await RuleService.getRulesForForm('W1');
const result = await RuleService.evaluateRules(taxData);
```

## Testing

```bash
# Run all tests
npm test

# Run tests for specific folder
npm test src/services

# Watch mode
npm test -- --watch
```

## Best Practices

### 1. Component Organization
- Keep components focused and single-responsibility
- Use TypeScript interfaces for props
- Extract complex logic into custom hooks

### 2. Service Layer
- All API calls go through service layer
- Services handle error transformation
- Return typed responses

### 3. Hooks
- Use hooks for reusable stateful logic
- Keep hooks focused on single concern
- Document hook dependencies

### 4. Type Safety
- Define interfaces for all data structures
- Use strict TypeScript configuration
- Avoid `any` types

## Related Directories

- [`../components/`](../components/) - Top-level application components
- [`../contexts/`](../contexts/) - React context providers
- [`../services/`](../services/) - Additional service modules
- [`../backend/`](../backend/) - Backend microservices
