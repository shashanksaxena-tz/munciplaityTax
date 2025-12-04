# Form Schema Configuration System

## Overview

This system provides a **single source of truth** for form field definitions across both the UI and extraction service. Instead of maintaining duplicate field mappings in multiple places, all form metadata is centralized in JSON schema files.

## Architecture

```
config/form-schemas/          # Source schemas (version controlled)
├── index.json                # Schema registry
├── w2-schema.json           # W-2 form definition
├── 1040-schema.json         # Federal 1040 definition
├── schedule-c-schema.json   # Schedule C definition
├── 1099-nec-schema.json     # 1099-NEC definition
├── 1099-misc-schema.json    # 1099-MISC definition
├── w2g-schema.json          # W-2G definition
└── schedule-e-schema.json   # Schedule E definition

public/config/form-schemas/   # Runtime schemas (served by Vite)
└── (copies of above)

services/formSchemaService.ts # Schema loader & utilities
components/FormSchemaViewer.tsx # Admin UI to browse schemas
```

## Schema Structure

Each form schema file follows this structure:

```json
{
  "formType": "W-2",
  "version": "2024",
  "description": "Wage and Tax Statement",
  "fields": [
    {
      "id": "federalWages",
      "label": "Federal Wages (Box 1)",
      "boxNumber": "1",
      "type": "currency",
      "required": true,
      "weight": "CRITICAL",
      "displayInUI": true,
      "displayOrder": 3,
      "validationRules": {
        "min": 0,
        "max": 99999999
      }
    }
  ]
}
```

### Field Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | string | Unique field identifier (matches property names in types) |
| `label` | string | Human-readable label for UI display |
| `boxNumber` | string | W-2/1099 box number (optional) |
| `lineNumber` | string | 1040/Schedule line number (optional) |
| `type` | enum | Data type: `text`, `currency`, `percentage`, `date`, `number` |
| `required` | boolean | Whether field must have a value |
| `weight` | enum | Importance: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |
| `displayInUI` | boolean | Show in extraction review panel |
| `displayOrder` | number | Sort order for UI display |
| `validationRules` | object | Validation constraints (min, max, pattern) |

## Usage

### Frontend (TypeScript/React)

#### Load Display Fields for a Form

```typescript
import { getDisplayFields } from '../services/formSchemaService';

const fields = await getDisplayFields('W-2');
// Returns: [{ label: 'Employer', key: 'employer', format: 'text' }, ...]
```

#### Get All Fields (including non-display)

```typescript
import { getAllFields } from '../services/formSchemaService';

const allFields = await getAllFields('W-2');
// Returns full FormFieldSchema[] array
```

#### Get Field Weights for Confidence Scoring

```typescript
import { getFieldWeights } from '../services/formSchemaService';

const weights = await getFieldWeights('W-2');
// Returns: Map { 'federalWages' => 'CRITICAL', 'locality' => 'MEDIUM', ... }
```

#### Validate Field Value

```typescript
import { validateField, getFormSchema } from '../services/formSchemaService';

const schema = await getFormSchema('W-2');
const field = schema.fields.find(f => f.id === 'federalWages');
const result = validateField(field, 50000);
// Returns: { valid: true } or { valid: false, error: 'Message' }
```

#### Preload All Schemas (on App Init)

```typescript
import { preloadSchemas } from '../services/formSchemaService';

// In your App.tsx or main entry
useEffect(() => {
  preloadSchemas();
}, []);
```

### Backend (Java)

The extraction service can load schemas via HTTP:

```java
// Pseudo-code - to be implemented
FormSchema schema = formSchemaClient.getSchema("W-2");
Map<String, String> fieldWeights = schema.getFieldWeights();
```

Or read from classpath resources:

```java
// Copy schemas to backend/extraction-service/src/main/resources/form-schemas/
ObjectMapper mapper = new ObjectMapper();
FormSchema schema = mapper.readValue(
    getClass().getResourceAsStream("/form-schemas/w2-schema.json"),
    FormSchema.class
);
```

## UI Tools

### Form Schema Viewer

Admin interface to browse and test schemas:

**URL:** `/admin/schemas` (requires admin role)

**Features:**
- View all form types and their field definitions
- See field metadata (type, weight, required, display settings)
- Statistics dashboard (total fields, display fields, required fields)
- Interactive table with sorting and filtering

**Access:** 
```bash
# After logging in as admin
http://localhost:3000/admin/schemas
```

## Integration Points

### 1. Extraction Review Panel

**File:** `components/ExtractionReview/FieldWithSource.tsx`

**Before:**
```typescript
// Hardcoded mapping
const FORM_DISPLAY_FIELDS = {
  'W-2': [
    { label: 'Employer', key: 'employer' },
    // ... more fields
  ]
};
```

**After:**
```typescript
// Dynamic from schema
const fields = await getDisplayFields(form.formType);
```

### 2. Extraction Service Prompt Generation

**File:** `backend/extraction-service/.../RealGeminiService.java`

**Before:**
```java
// Hardcoded field list in prompt
private static final Map<String, String> FIELD_WEIGHTS = Map.of(
    "federalWages", "CRITICAL",
    "locality", "MEDIUM"
);
```

**After:**
```java
// Load from schema
FormSchema schema = loadSchema("W-2");
Map<String, String> weights = schema.getFieldWeights();
String prompt = buildPromptFromSchema(schema);
```

### 3. PDF Form Field Mapping

**File:** `backend/pdf-service/.../FormTemplate.java`

Already has `field_mappings` JSONB column - extend to use schema definitions.

## Development Workflow

### Adding a New Form

1. **Create schema file:** `config/form-schemas/new-form-schema.json`
2. **Add to index:** Update `config/form-schemas/index.json`
3. **Copy to public:** Run `npm run copy-schemas` (or build script)
4. **Add to service:** Update `formSchemaService.ts` mapping
5. **Test in UI:** Visit `/admin/schemas` and select new form

### Modifying a Field

1. **Edit schema:** Update field properties in JSON
2. **Rebuild:** Changes auto-reload in dev mode
3. **Test extraction:** Upload document and verify field display
4. **Update types:** If adding new fields, update TypeScript interfaces

### Build Process

Add to `package.json`:
```json
{
  "scripts": {
    "copy-schemas": "mkdir -p public/config/form-schemas && cp config/form-schemas/*.json public/config/form-schemas/",
    "prebuild": "npm run copy-schemas"
  }
}
```

## Benefits

### ✅ Single Source of Truth
- One JSON file per form type
- No duplicate mappings across codebase
- Consistent field definitions everywhere

### ✅ Easy Maintenance
- Add new fields without code changes
- Update labels/validation rules centrally
- Version control for schema changes

### ✅ Type Safety
- TypeScript interfaces generated from schemas
- Compile-time validation
- IntelliSense support

### ✅ Runtime Flexibility
- Load schemas dynamically
- Support multiple tax years
- A/B test field configurations

### ✅ Developer Experience
- Visual schema browser UI
- Auto-complete in code
- Clear documentation

## Future Enhancements

### API Endpoint
Create REST API to serve schemas:
```
GET /api/forms/schemas          # List all schemas
GET /api/forms/schemas/W-2      # Get specific schema
GET /api/forms/schemas/W-2/2024 # Get versioned schema
```

### Code Generation
Generate TypeScript types from schemas:
```bash
npm run generate-types
# Produces types.generated.ts from schemas
```

### Validation Framework
Runtime validation using schemas:
```typescript
import { validateForm } from './formValidator';

const errors = validateForm('W-2', formData);
// Returns field-level error messages
```

### Multi-Year Support
Store schemas per tax year:
```
config/form-schemas/
  2024/
    w2-schema.json
  2023/
    w2-schema.json
```

## Migration Guide

### From Hardcoded to Schema-Driven

**Step 1:** Identify hardcoded field lists
```bash
grep -r "FORM_DISPLAY_FIELDS" components/
grep -r "FIELD_WEIGHTS" backend/
```

**Step 2:** Create schema files for each form

**Step 3:** Replace hardcoded logic with schema service calls

**Step 4:** Test all extraction flows

**Step 5:** Remove old constants/mappings

## Testing

### Unit Tests
```typescript
import { getDisplayFields, validateField } from './formSchemaService';

test('W-2 schema loads correctly', async () => {
  const fields = await getDisplayFields('W-2');
  expect(fields.length).toBeGreaterThan(0);
  expect(fields[0]).toHaveProperty('label');
});

test('Field validation works', async () => {
  const schema = await getFormSchema('W-2');
  const field = schema.fields.find(f => f.id === 'federalWages');
  expect(validateField(field, -100).valid).toBe(false);
  expect(validateField(field, 50000).valid).toBe(true);
});
```

### Integration Tests
```typescript
test('Extraction review uses schema fields', async () => {
  render(<FieldWithSource form={mockW2Form} />);
  
  // Should display fields from schema
  expect(screen.getByText('Federal Wages (Box 1)')).toBeInTheDocument();
  expect(screen.getByText('Employer')).toBeInTheDocument();
});
```

## Troubleshooting

### Schemas Not Loading
- Check `public/config/form-schemas/` directory exists
- Verify Vite config has `publicDir: 'public'`
- Clear browser cache and rebuild

### Fields Not Displaying
- Ensure `displayInUI: true` in schema
- Check `displayOrder` values are unique
- Verify form type string matches exactly

### Validation Failing
- Check `validationRules` syntax in schema
- Ensure data types match (number vs string)
- Test with `validateField()` directly

## Contributing

When adding new form types:
1. Follow existing schema structure
2. Include comprehensive field metadata
3. Add to FormSchemaViewer UI
4. Update this README
5. Create test cases

## License

Part of MuniTax application - same license applies.
