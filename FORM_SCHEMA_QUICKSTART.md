# 🚀 Quick Start Guide: Form Schema System

## 5-Minute Overview

The form schema system provides **a single JSON file for each tax form** that defines all field metadata (labels, types, validation, display settings, extraction weights).

## Try It Now

### 1. View the Schema Browser UI
```bash
# Start the app
npm run dev

# Login as admin and visit:
http://localhost:3000/admin/schemas
```

### 2. Browse W-2 Schema
Click on **[W-2]** button to see:
- All 14 fields defined for W-2 forms
- Field types (currency, text, etc.)
- Extraction weights (CRITICAL, HIGH, MEDIUM, LOW)
- Display settings (which fields show in UI)

### 3. See It In Action
Upload a W-2 PDF and watch the extraction review panel automatically display fields from the schema!

## For Developers

### Use Schema in Your Component
```typescript
import { getDisplayFields } from '../services/formSchemaService';

function MyComponent() {
  const [fields, setFields] = useState([]);

  useEffect(() => {
    async function load() {
      const fields = await getDisplayFields('W-2');
      setFields(fields);
    }
    load();
  }, []);

  return (
    <div>
      {fields.map(({ label, key }) => (
        <div key={key}>{label}: {data[key]}</div>
      ))}
    </div>
  );
}
```

### Add a New Field to W-2
1. Edit `config/form-schemas/w2-schema.json`
2. Add field definition:
```json
{
  "id": "newField",
  "label": "New Field Label",
  "type": "text",
  "required": false,
  "weight": "MEDIUM",
  "displayInUI": true,
  "displayOrder": 99
}
```
3. Save file
4. Refresh browser - field appears automatically! 🎉

### Validate a Field
```typescript
import { getFormSchema, validateField } from '../services/formSchemaService';

const schema = await getFormSchema('W-2');
const field = schema.fields.find(f => f.id === 'federalWages');
const result = validateField(field, userInput);

if (!result.valid) {
  alert(result.error);
}
```

## File Locations

### Source Schemas (Edit These)
```
config/form-schemas/
├── w2-schema.json
├── 1040-schema.json
└── ...
```

### Service Layer (Use These)
```
services/formSchemaService.ts
```

### Admin UI (View Here)
```
components/FormSchemaViewer.tsx
Route: /admin/schemas
```

## Common Tasks

### Add a New Form Type
1. Create `config/form-schemas/myform-schema.json`
2. Add to `index.json`
3. Update `formSchemaService.ts` schemaFileMap
4. Run `npm run copy-schemas`
5. Test at `/admin/schemas`

### Modify Field Label
1. Edit `config/form-schemas/w2-schema.json`
2. Change `"label"` property
3. Save - UI auto-updates!

### Change Display Order
1. Edit `config/form-schemas/w2-schema.json`
2. Modify `"displayOrder"` numbers
3. Save - fields re-sort automatically!

### Add Validation Rule
1. Edit schema field
2. Add `"validationRules"`:
```json
"validationRules": {
  "min": 0,
  "max": 100,
  "pattern": "^\\d{2}-\\d{7}$"
}
```
3. Save - validation active!

## Testing

### Run Schema Tests
```bash
npm test services/__tests__/formSchemaService.test.ts
```

### Test Specific Form
```bash
npm test -- --grep "W-2"
```

## Troubleshooting

### Schemas Not Loading?
```bash
# Ensure schemas copied to public/
npm run copy-schemas

# Check directory
ls -la public/config/form-schemas/
```

### Fields Not Showing?
- Check `"displayInUI": true` in schema
- Verify `"displayOrder"` is set
- Clear browser cache

### Validation Not Working?
- Ensure `"required": true` or `"validationRules"` set
- Check field type matches validation

## Next Steps

1. ✅ **You're all set!** Schemas are loaded and working
2. 📖 Read `FORM_SCHEMA_IMPLEMENTATION.md` for details
3. 🏗️ Check `FORM_SCHEMA_ARCHITECTURE.md` for architecture
4. 🧪 Run tests to verify everything works
5. 🎨 Customize schemas for your needs

## Need Help?

- **Documentation:** `config/form-schemas/README.md`
- **Examples:** `services/__tests__/formSchemaService.test.ts`
- **Admin UI:** http://localhost:3000/admin/schemas

Happy coding! 🎉
