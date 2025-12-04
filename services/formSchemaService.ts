/**
 * Form Schema Service
 * 
 * Single source of truth for form field definitions across UI and extraction service.
 * Loads form schemas from centralized JSON configuration files.
 */

export interface FormFieldSchema {
  id: string;
  label: string;
  boxNumber?: string;
  lineNumber?: string;
  type: 'text' | 'currency' | 'percentage' | 'date' | 'number';
  required: boolean;
  weight: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  displayInUI: boolean;
  displayOrder: number;
  validationRules?: {
    min?: number;
    max?: number;
    pattern?: string;
  };
}

export interface FormSchema {
  formType: string;
  version: string;
  description: string;
  fields: FormFieldSchema[];
}

// Cache for loaded schemas
const schemaCache: Map<string, FormSchema> = new Map();

/**
 * Load form schema from JSON configuration
 */
async function loadSchema(formType: string): Promise<FormSchema | null> {
  // Check cache first
  if (schemaCache.has(formType)) {
    return schemaCache.get(formType)!;
  }

  try {
    // Map form types to schema file names (including variations)
    const schemaFileMap: Record<string, string> = {
      'W-2': 'w2-schema.json',
      'Federal 1040': '1040-schema.json',
      'Schedule C': 'schedule-c-schema.json',
      '1099-NEC': '1099-nec-schema.json',
      '1099-MISC': '1099-misc-schema.json',
      'W-2G': 'w2g-schema.json',
      'Schedule E': 'schedule-e-schema.json',
      'Schedule F': 'schedule-e-schema.json', // Use Schedule E schema for now
      // Add fallback mappings for common variations
      'Form 1099-NEC': '1099-nec-schema.json',
      'Form 1099-MISC': '1099-misc-schema.json',
      '1040': '1040-schema.json',
      'ScheduleC': 'schedule-c-schema.json',
      'ScheduleE': 'schedule-e-schema.json',
    };

    const fileName = schemaFileMap[formType];
    if (!fileName) {
      console.warn(`No schema file mapped for form type: "${formType}". Available: ${Object.keys(schemaFileMap).join(', ')}`);
      return null;
    }

    // In production, this would fetch from an API endpoint or CDN
    // For now, we'll import the JSON directly
    const response = await fetch(`/config/form-schemas/${fileName}`);
    if (!response.ok) {
      console.error(`Failed to load schema for ${formType}: ${response.statusText}`);
      return null;
    }

    const schema: FormSchema = await response.json();
    schemaCache.set(formType, schema);
    return schema;
  } catch (error) {
    console.error(`Error loading schema for ${formType}:`, error);
    return null;
  }
}

/**
 * Get display fields for a form type (fields marked displayInUI: true)
 */
type DisplayFieldFormat = 'currency' | 'percentage' | 'text';

export async function getDisplayFields(formType: string): Promise<Array<{
  label: string;
  key: string;
  format?: DisplayFieldFormat;
}>> {
  console.log(`[formSchemaService] getDisplayFields called for: "${formType}"`);
  
  const schema = await loadSchema(formType);
  if (!schema) {
    console.warn(`[formSchemaService] No schema loaded for: "${formType}"`);
    return [];
  }

  console.log(`[formSchemaService] Schema loaded for "${formType}":`, {
    formType: schema.formType,
    totalFields: schema.fields.length,
    displayFields: schema.fields.filter(f => f.displayInUI).length
  });

  const displayFields = schema.fields
    .filter(field => field.displayInUI)
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map(field => ({
      label: field.label,
      key: field.id,
      format: mapFieldTypeToFormat(field.type)
    }));

  console.log(`[formSchemaService] Returning ${displayFields.length} display fields for "${formType}"`);
  return displayFields;
}

/**
 * Get all fields for a form type
 */
export async function getAllFields(formType: string): Promise<FormFieldSchema[]> {
  const schema = await loadSchema(formType);
  if (!schema) {
    return [];
  }

  return schema.fields.sort((a, b) => a.displayOrder - b.displayOrder);
}

function mapFieldTypeToFormat(type: FormFieldSchema['type']): DisplayFieldFormat {
  if (type === 'currency') {
    return 'currency';
  }
  if (type === 'percentage') {
    return 'percentage';
  }
  return 'text';
}

/**
 * Get field weights for extraction confidence scoring
 */
export async function getFieldWeights(formType: string): Promise<Map<string, string>> {
  const schema = await loadSchema(formType);
  if (!schema) {
    return new Map();
  }

  const weights = new Map<string, string>();
  schema.fields.forEach(field => {
    weights.set(field.id, field.weight);
  });

  return weights;
}

/**
 * Get critical fields (for validation)
 */
export async function getCriticalFields(formType: string): Promise<string[]> {
  const schema = await loadSchema(formType);
  if (!schema) {
    return [];
  }

  return schema.fields
    .filter(field => field.weight === 'CRITICAL')
    .map(field => field.id);
}

/**
 * Validate field value against schema rules
 */
export function validateField(field: FormFieldSchema, value: any): { valid: boolean; error?: string } {
  if (field.required && (value === null || value === undefined || value === '')) {
    return { valid: false, error: `${field.label} is required` };
  }

  if (field.validationRules) {
    const rules = field.validationRules;

    // Min/max validation for numeric fields
    if (typeof value === 'number') {
      if (rules.min !== undefined && value < rules.min) {
        return { valid: false, error: `${field.label} must be at least ${rules.min}` };
      }
      if (rules.max !== undefined && value > rules.max) {
        return { valid: false, error: `${field.label} must be at most ${rules.max}` };
      }
    }

    // Pattern validation for string fields
    if (typeof value === 'string' && rules.pattern) {
      const regex = new RegExp(rules.pattern);
      if (!regex.test(value)) {
        return { valid: false, error: `${field.label} format is invalid` };
      }
    }
  }

  return { valid: true };
}

/**
 * Preload all schemas (call on app initialization)
 */
export async function preloadSchemas(): Promise<void> {
  const formTypes = [
    'W-2',
    'Federal 1040',
    'Schedule C',
    '1099-NEC',
    '1099-MISC',
    'W-2G',
    'Schedule E'
  ];

  await Promise.all(formTypes.map(formType => loadSchema(formType)));
  console.log('Form schemas preloaded:', schemaCache.size);
}

/**
 * Get schema for a specific form type
 */
export async function getFormSchema(formType: string): Promise<FormSchema | null> {
  return loadSchema(formType);
}

// Export for testing/debugging
export const __clearCache = () => schemaCache.clear();
