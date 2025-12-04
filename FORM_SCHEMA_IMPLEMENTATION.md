# Form Schema System - Implementation Summary

## What Was Created

### ✅ Centralized Schema Files
Created JSON schema definitions for all form types:

```
config/form-schemas/
├── README.md              # Comprehensive documentation
├── index.json            # Schema registry
├── w2-schema.json        # W-2 Wage and Tax Statement
├── 1040-schema.json      # Federal 1040 Individual Tax Return
├── schedule-c-schema.json # Schedule C Profit/Loss from Business
├── 1099-nec-schema.json  # 1099-NEC Nonemployee Compensation
├── 1099-misc-schema.json # 1099-MISC Miscellaneous Income
├── w2g-schema.json       # W-2G Gambling Winnings
└── schedule-e-schema.json # Schedule E Supplemental Income
```

### ✅ TypeScript Service Layer
**File:** `services/formSchemaService.ts`

Provides utilities to:
- Load form schemas dynamically
- Get display fields for UI rendering
- Get field weights for extraction confidence
- Validate field values against rules
- Preload schemas on app initialization

### ✅ Updated UI Component
**File:** `components/ExtractionReview/FieldWithSource.tsx`

**Before:** Hardcoded `FORM_DISPLAY_FIELDS` constant
**After:** Dynamically loads fields from schema service

### ✅ Admin UI Tool
**File:** `components/FormSchemaViewer.tsx`

Interactive browser to:
- View all form types and their fields
- See field metadata (type, weight, required, display)
- Statistics dashboard
- Filterable table view

**Access:** `/admin/schemas` (requires admin role)

### ✅ Build Integration
**File:** `package.json`

Added scripts:
```json
"copy-schemas": "mkdir -p public/config/form-schemas && cp config/form-schemas/*.json public/config/form-schemas/",
"prebuild": "npm run copy-schemas"
```

Ensures schemas are copied to public directory before build.

## Schema Structure

Each schema defines:

```typescript
interface FormSchema {
  formType: string;      // "W-2", "Federal 1040", etc.
  version: string;       // "2024"
  description: string;   // Human-readable description
  fields: FormFieldSchema[];
}

interface FormFieldSchema {
  id: string;            // Field identifier (e.g., "federalWages")
  label: string;         // Display label (e.g., "Federal Wages (Box 1)")
  boxNumber?: string;    // W-2/1099 box number
  lineNumber?: string;   // 1040/Schedule line number
  type: string;          // "text" | "currency" | "percentage" | "date" | "number"
  required: boolean;     // Validation flag
  weight: string;        // "CRITICAL" | "HIGH" | "MEDIUM" | "LOW"
  displayInUI: boolean;  // Show in extraction review
  displayOrder: number;  // Sort order
  validationRules?: {    // Optional validation
    min?: number;
    max?: number;
    pattern?: string;
  };
}
```

## Example: W-2 Schema

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

## Usage Examples

### Load Display Fields in UI

```typescript
import { getDisplayFields } from '../services/formSchemaService';

// In component
const [displayFields, setDisplayFields] = useState([]);

useEffect(() => {
  async function loadFields() {
    const fields = await getDisplayFields('W-2');
    setDisplayFields(fields);
  }
  loadFields();
}, [formType]);

// Render fields
{displayFields.map(({ label, key, format }) => (
  <div key={key}>
    <label>{label}</label>
    <input type="text" value={formData[key]} />
  </div>
))}
```

### Get Field Weights for Extraction

```typescript
import { getFieldWeights } from '../services/formSchemaService';

const weights = await getFieldWeights('W-2');
console.log(weights.get('federalWages')); // "CRITICAL"
console.log(weights.get('locality'));      // "MEDIUM"
```

### Validate Field Value

```typescript
import { getFormSchema, validateField } from '../services/formSchemaService';

const schema = await getFormSchema('W-2');
const field = schema.fields.find(f => f.id === 'federalWages');
const result = validateField(field, 50000);

if (!result.valid) {
  console.error(result.error);
}
```

## Benefits

### 🎯 Single Source of Truth
- One JSON file per form type
- No duplicate field definitions
- Consistent across UI and backend

### 🔄 Dynamic Updates
- Change field labels without code changes
- Add new fields by editing JSON
- Version control for schema changes

### 📊 Better Maintenance
- Clear field metadata
- Centralized validation rules
- Easy to audit and update

### 🚀 Developer Experience
- Visual schema browser UI
- Auto-complete in code
- Type-safe field access

### 🧪 Testability
- Schema validation tests
- Field rendering tests
- Integration tests

## Next Steps

### 1. Backend Integration
Update extraction service to use schemas:

**File:** `backend/extraction-service/src/main/java/com/munitax/extraction/service/RealGeminiService.java`

```java
// Replace hardcoded FIELD_WEIGHTS
private static final Map<String, String> FIELD_WEIGHTS = loadFieldWeightsFromSchema();

private static Map<String, String> loadFieldWeightsFromSchema() {
  // Load from classpath resources or HTTP endpoint
  FormSchema schema = schemaLoader.load("W-2");
  return schema.getFieldWeights();
}
```

### 2. Prompt Generation from Schema
Generate extraction prompts dynamically:

```java
private String buildExtractionPrompt(String formType) {
  FormSchema schema = schemaLoader.load(formType);
  
  StringBuilder prompt = new StringBuilder();
  prompt.append("Extract fields from ").append(formType).append(":\n");
  
  for (FormField field : schema.getFields()) {
    prompt.append("- ").append(field.getLabel());
    if (field.getBoxNumber() != null) {
      prompt.append(" (Box ").append(field.getBoxNumber()).append(")");
    }
    prompt.append(": ").append(field.getType()).append("\n");
  }
  
  return prompt.toString();
}
```

### 3. PDF Field Mapping
Use schemas for PDF generation:

**File:** `backend/pdf-service/src/main/java/com/munitax/pdf/service/FieldMappingService.java`

```java
public Map<String, String> mapFormData(String formType, Map<String, Object> data) {
  FormSchema schema = schemaLoader.load(formType);
  
  Map<String, String> pdfFields = new HashMap<>();
  for (FormField field : schema.getFields()) {
    Object value = data.get(field.getId());
    if (value != null) {
      String formatted = formatValue(value, field.getType());
      pdfFields.put(field.getPdfFieldName(), formatted);
    }
  }
  
  return pdfFields;
}
```

### 4. API Endpoint
Create REST API to serve schemas:

```java
@RestController
@RequestMapping("/api/forms/schemas")
public class FormSchemaController {
  
  @GetMapping
  public List<FormSchema> listSchemas() {
    return schemaRepository.findAll();
  }
  
  @GetMapping("/{formType}")
  public FormSchema getSchema(@PathVariable String formType) {
    return schemaRepository.findByFormType(formType);
  }
  
  @GetMapping("/{formType}/{version}")
  public FormSchema getSchemaVersion(
    @PathVariable String formType,
    @PathVariable String version
  ) {
    return schemaRepository.findByFormTypeAndVersion(formType, version);
  }
}
```

### 5. Multi-Year Support
Structure schemas by tax year:

```
config/form-schemas/
  2024/
    w2-schema.json
    1040-schema.json
  2023/
    w2-schema.json
    1040-schema.json
```

### 6. Code Generation
Generate TypeScript types from schemas:

```bash
npm run generate-types
```

Creates `types.generated.ts`:

```typescript
// Auto-generated from w2-schema.json
export interface W2Form {
  federalWages: number;
  medicareWages: number;
  localWages: number;
  // ... more fields
}
```

## Testing

### Test Schema Loading

```typescript
import { getFormSchema, getDisplayFields } from './formSchemaService';

describe('Form Schema Service', () => {
  test('loads W-2 schema', async () => {
    const schema = await getFormSchema('W-2');
    expect(schema).toBeDefined();
    expect(schema.formType).toBe('W-2');
    expect(schema.fields.length).toBeGreaterThan(0);
  });

  test('gets display fields', async () => {
    const fields = await getDisplayFields('W-2');
    expect(fields.every(f => f.displayInUI)).toBe(true);
  });
});
```

### Test UI Integration

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import { FieldWithSource } from './FieldWithSource';

test('renders fields from schema', async () => {
  const mockForm = { formType: 'W-2', federalWages: 50000 };
  render(<FieldWithSource form={mockForm} />);
  
  await waitFor(() => {
    expect(screen.getByText('Federal Wages (Box 1)')).toBeInTheDocument();
  });
});
```

## Accessing the Schema Viewer

1. Start the app: `npm run dev`
2. Login as admin user
3. Navigate to: `http://localhost:3000/admin/schemas`
4. Browse form types and field definitions

## Files Changed/Created

### Created
- `config/form-schemas/*.json` (7 schema files + README + index)
- `services/formSchemaService.ts` (schema loader service)
- `components/FormSchemaViewer.tsx` (admin UI tool)
- `public/config/form-schemas/*.json` (runtime copies)

### Modified
- `components/ExtractionReview/FieldWithSource.tsx` (uses schema service)
- `App.tsx` (added /admin/schemas route)
- `package.json` (added copy-schemas script)
- `vite.config.ts` (publicDir config)

## Documentation

See `config/form-schemas/README.md` for comprehensive documentation including:
- Architecture overview
- Schema structure reference
- Usage examples (frontend & backend)
- Development workflow
- Migration guide
- Troubleshooting tips

## Summary

✅ **Single source of truth** for form field definitions
✅ **Dynamic loading** from JSON schemas
✅ **UI tool** to browse and manage schemas
✅ **Type-safe** service layer with validation
✅ **Build integration** for production deployment
✅ **Comprehensive documentation** for developers
✅ **Ready for backend integration** with extraction service

The form schema system is now ready to use! Field definitions are centralized, maintainable, and can be consumed by both UI and backend services.
