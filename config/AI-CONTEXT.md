# config

Configuration files and form schemas for the Municipality Tax Management System.

<!-- AUTO-GENERATED: Purpose -->
## Purpose

The `config` directory contains application-wide configuration files and form schema definitions. It serves as the single source of truth for design system configuration and form structure definitions used throughout the application. This centralized configuration approach ensures consistency across the UI and enables dynamic form generation.
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Key Files -->
## Key Files

| File/Directory | Description |
|---------------|-------------|
| `design-system.ts` | Design system configuration (colors, typography, spacing) |
| `form-schemas/` | JSON schema definitions for tax forms (9 schemas) |
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Public API -->
## Public API

### `design-system.ts` - Design System Configuration

Centralized design tokens for the entire application:

```typescript
import { designSystem } from '@/config/design-system';

// Access design tokens
const primaryColor = designSystem.colors.primary;
const spacing = designSystem.spacing.md;
const fontSize = designSystem.typography.fontSize.lg;

// Use in components
<div style={{ color: designSystem.colors.primary }}>
  Content
</div>
```

**Configuration Structure:**
```typescript
interface DesignSystem {
  colors: {
    primary: string;
    secondary: string;
    success: string;
    error: string;
    warning: string;
    // ... more colors
  };
  typography: {
    fontFamily: {
      sans: string;
      mono: string;
    };
    fontSize: {
      xs: string;
      sm: string;
      md: string;
      lg: string;
      xl: string;
      // ... more sizes
    };
  };
  spacing: {
    xs: string;
    sm: string;
    md: string;
    lg: string;
    xl: string;
    // ... more spacing
  };
  borderRadius: {
    sm: string;
    md: string;
    lg: string;
  };
  shadows: {
    sm: string;
    md: string;
    lg: string;
  };
}
```

### `form-schemas/` - Form Schema Definitions

JSON schemas for tax forms used by the form generation system:

**Available Schemas:**
1. `schedule-x.schema.json` - Business income tax
2. `schedule-y.schema.json` - Municipal tax
3. `w1-form.schema.json` - Monthly withholding
4. `w2-form.schema.json` - Quarterly withholding
5. `business-registration.schema.json` - Business registration
6. `refund-request.schema.json` - Refund request
7. And more...

**Schema Format:**
```json
{
  "id": "schedule-x",
  "name": "Schedule X - Business Income Tax",
  "version": "1.0.0",
  "fields": [
    {
      "name": "businessName",
      "label": "Business Name",
      "type": "text",
      "required": true,
      "validation": {
        "minLength": 1,
        "maxLength": 100
      }
    },
    {
      "name": "taxYear",
      "label": "Tax Year",
      "type": "number",
      "required": true,
      "validation": {
        "min": 2000,
        "max": 2100
      }
    }
  ]
}
```

**Loading Schemas:**
```typescript
import { FormSchemaService } from '@/services/formSchemaService';

const schema = await FormSchemaService.loadSchema('schedule-x');
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Dependencies -->
## Dependencies

### Internal
- Used by `../src/services/formSchemaService.ts`
- Used by `../components/forms/`
- Used by `../src/components/`

### External
- `zod` - Schema validation
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Used By -->
## Used By

- All components for design system tokens
- Form components for schema-driven rendering
- FormSchemaService for validation
<!-- END AUTO-GENERATED -->

<!-- CUSTOM SECTION: Project-Specific Notes -->
## Notes

Configuration files are version-controlled and should be updated carefully to maintain consistency across the application.

<!-- END CUSTOM SECTION -->
