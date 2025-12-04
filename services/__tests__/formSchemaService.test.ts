import { describe, it, expect, beforeEach } from 'vitest';
import { 
  getFormSchema, 
  getDisplayFields, 
  getAllFields,
  getFieldWeights,
  getCriticalFields,
  validateField,
  __clearCache 
} from '../formSchemaService';

describe('Form Schema Service', () => {
  beforeEach(() => {
    // Clear cache before each test
    __clearCache();
  });

  describe('getFormSchema', () => {
    it('should load W-2 schema', async () => {
      const schema = await getFormSchema('W-2');
      
      expect(schema).toBeDefined();
      expect(schema?.formType).toBe('W-2');
      expect(schema?.version).toBe('2024');
      expect(schema?.fields).toBeDefined();
      expect(schema?.fields.length).toBeGreaterThan(0);
    });

    it('should load Federal 1040 schema', async () => {
      const schema = await getFormSchema('Federal 1040');
      
      expect(schema).toBeDefined();
      expect(schema?.formType).toBe('Federal 1040');
      expect(schema?.fields).toBeDefined();
    });

    it('should return null for unknown form type', async () => {
      const schema = await getFormSchema('Unknown Form');
      
      expect(schema).toBeNull();
    });

    it('should cache loaded schemas', async () => {
      const schema1 = await getFormSchema('W-2');
      const schema2 = await getFormSchema('W-2');
      
      // Should return same instance from cache
      expect(schema1).toBe(schema2);
    });
  });

  describe('getDisplayFields', () => {
    it('should return only fields marked for display', async () => {
      const fields = await getDisplayFields('W-2');
      
      expect(fields.length).toBeGreaterThan(0);
      
      // All returned fields should have corresponding displayInUI: true
      const schema = await getFormSchema('W-2');
      fields.forEach(field => {
        const schemaField = schema?.fields.find(f => f.id === field.key);
        expect(schemaField?.displayInUI).toBe(true);
      });
    });

    it('should return fields in correct display order', async () => {
      const fields = await getDisplayFields('W-2');
      
      // Check that fields are sorted by displayOrder
      const schema = await getFormSchema('W-2');
      const displayFieldsFromSchema = schema?.fields
        .filter(f => f.displayInUI)
        .sort((a, b) => a.displayOrder - b.displayOrder);
      
      expect(fields.length).toBe(displayFieldsFromSchema?.length);
      fields.forEach((field, index) => {
        expect(field.key).toBe(displayFieldsFromSchema?.[index].id);
      });
    });

    it('should map field types to format correctly', async () => {
      const fields = await getDisplayFields('W-2');
      
      const currencyField = fields.find(f => f.key === 'federalWages');
      expect(currencyField?.format).toBe('currency');
      
      const textField = fields.find(f => f.key === 'employer');
      expect(textField?.format).toBe('text');
    });
  });

  describe('getAllFields', () => {
    it('should return all fields including non-display', async () => {
      const allFields = await getAllFields('W-2');
      const displayFields = await getDisplayFields('W-2');
      
      expect(allFields.length).toBeGreaterThanOrEqual(displayFields.length);
    });

    it('should return fields sorted by displayOrder', async () => {
      const fields = await getAllFields('W-2');
      
      for (let i = 1; i < fields.length; i++) {
        expect(fields[i].displayOrder).toBeGreaterThanOrEqual(fields[i - 1].displayOrder);
      }
    });
  });

  describe('getFieldWeights', () => {
    it('should return weight map for all fields', async () => {
      const weights = await getFieldWeights('W-2');
      
      expect(weights.size).toBeGreaterThan(0);
      expect(weights.get('federalWages')).toBe('CRITICAL');
      expect(weights.get('employer')).toBe('HIGH');
      expect(weights.get('locality')).toBe('MEDIUM');
    });

    it('should return empty map for unknown form', async () => {
      const weights = await getFieldWeights('Unknown Form');
      
      expect(weights.size).toBe(0);
    });
  });

  describe('getCriticalFields', () => {
    it('should return only CRITICAL weight fields', async () => {
      const critical = await getCriticalFields('W-2');
      
      expect(critical).toContain('federalWages');
      expect(critical).toContain('medicareWages');
      expect(critical).toContain('localWages');
      expect(critical).not.toContain('locality'); // MEDIUM weight
    });

    it('should return empty array for unknown form', async () => {
      const critical = await getCriticalFields('Unknown Form');
      
      expect(critical).toEqual([]);
    });
  });

  describe('validateField', () => {
    it('should validate required fields', async () => {
      const schema = await getFormSchema('W-2');
      const field = schema?.fields.find(f => f.id === 'federalWages');
      
      expect(field).toBeDefined();
      
      const validResult = validateField(field!, 50000);
      expect(validResult.valid).toBe(true);
      
      const invalidResult = validateField(field!, null);
      expect(invalidResult.valid).toBe(false);
      expect(invalidResult.error).toContain('required');
    });

    it('should validate numeric min/max rules', async () => {
      const schema = await getFormSchema('W-2');
      const field = schema?.fields.find(f => f.id === 'federalWages');
      
      expect(field).toBeDefined();
      
      // Valid value
      const validResult = validateField(field!, 50000);
      expect(validResult.valid).toBe(true);
      
      // Below min
      const belowMinResult = validateField(field!, -1000);
      expect(belowMinResult.valid).toBe(false);
      expect(belowMinResult.error).toContain('at least');
      
      // Above max
      const aboveMaxResult = validateField(field!, 999999999);
      expect(aboveMaxResult.valid).toBe(false);
      expect(aboveMaxResult.error).toContain('at most');
    });

    it('should validate pattern rules', async () => {
      const schema = await getFormSchema('W-2');
      const field = schema?.fields.find(f => f.id === 'employerEin');
      
      expect(field).toBeDefined();
      
      // Valid EIN format
      const validResult = validateField(field!, '12-3456789');
      expect(validResult.valid).toBe(true);
      
      // Invalid EIN format
      const invalidResult = validateField(field!, '123456789');
      expect(invalidResult.valid).toBe(false);
      expect(invalidResult.error).toContain('format');
    });

    it('should allow null for optional fields', async () => {
      const schema = await getFormSchema('W-2');
      const field = schema?.fields.find(f => f.id === 'locality');
      
      expect(field).toBeDefined();
      expect(field?.required).toBe(false);
      
      const result = validateField(field!, null);
      expect(result.valid).toBe(true);
    });
  });

  describe('Schema Structure Validation', () => {
    it('W-2 schema should have all expected properties', async () => {
      const schema = await getFormSchema('W-2');
      
      expect(schema).toMatchObject({
        formType: expect.any(String),
        version: expect.any(String),
        description: expect.any(String),
        fields: expect.any(Array)
      });
    });

    it('All fields should have required properties', async () => {
      const schema = await getFormSchema('W-2');
      
      schema?.fields.forEach(field => {
        expect(field).toMatchObject({
          id: expect.any(String),
          label: expect.any(String),
          type: expect.any(String),
          required: expect.any(Boolean),
          weight: expect.any(String),
          displayInUI: expect.any(Boolean),
          displayOrder: expect.any(Number)
        });
        
        // Validate weight values
        expect(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']).toContain(field.weight);
        
        // Validate type values
        expect(['text', 'currency', 'percentage', 'date', 'number']).toContain(field.type);
      });
    });

    it('Display orders should be unique within a form', async () => {
      const schema = await getFormSchema('W-2');
      
      const displayOrders = schema?.fields.map(f => f.displayOrder);
      const uniqueOrders = new Set(displayOrders);
      
      expect(displayOrders?.length).toBe(uniqueOrders.size);
    });

    it('Field IDs should be unique within a form', async () => {
      const schema = await getFormSchema('W-2');
      
      const fieldIds = schema?.fields.map(f => f.id);
      const uniqueIds = new Set(fieldIds);
      
      expect(fieldIds?.length).toBe(uniqueIds.size);
    });
  });

  describe('Multiple Form Types', () => {
    const formTypes = [
      'W-2',
      'Federal 1040',
      'Schedule C',
      '1099-NEC',
      '1099-MISC',
      'W-2G',
      'Schedule E'
    ];

    formTypes.forEach(formType => {
      it(`should load ${formType} schema successfully`, async () => {
        const schema = await getFormSchema(formType);
        
        expect(schema).toBeDefined();
        expect(schema?.formType).toBe(formType);
        expect(schema?.fields.length).toBeGreaterThan(0);
      });

      it(`${formType} should have at least one critical field`, async () => {
        const critical = await getCriticalFields(formType);
        
        expect(critical.length).toBeGreaterThan(0);
      });

      it(`${formType} should have at least one display field`, async () => {
        const displayFields = await getDisplayFields(formType);
        
        expect(displayFields.length).toBeGreaterThan(0);
      });
    });
  });
});
