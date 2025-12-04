# Spec 019 - Clarifications Summary

**Date**: 2025-12-03  
**Status**: CLARIFIED

## Decisions Made

### 1. Service Architecture ✅
**Decision**: Create new **Form Registry Service**
- Owns: `form_definitions`, `field_definitions`, `extraction_mappings` tables
- Reason: Foundational infrastructure used by extraction, rule, tax-engine, and frontend
- Benefits: Single source of truth, independent scaling, clean separation

### 2. Rule Engine Expression Language ✅
**Decision**: Use existing **rule-service** with SpEL
- Extend with: `rule_field_mappings`, `tax_rule_templates` tables
- Already implemented: Spring Expression Language (SpEL) 
- No new service needed

### 3. Extraction Service Refactoring ✅
**Decision**: Apply **SOLID Principles** - Modular refactoring
- Break down monolithic `RealGeminiService.java` (2000+ lines)
- Create 15+ focused service classes, each < 200 lines
- Each class has ONE responsibility:
  - `DocumentAnalysisService` - PDF structure analysis
  - `FormDetectionService` - Identify form types
  - `GeminiClientService` - Low-level API calls only
  - `PromptBuilderService` - Build prompts from database
  - `ResponseParserService` - Parse JSON responses
  - `FieldMappingService` - AI names → canonical names
  - `TypeConversionService` - Type conversion
  - `FormValidationService` - Form validation
  - `FieldValidationService` - Field validation
  - `ProvenanceTrackingService` - Track extraction sources
  - `ExtractionOrchestrationService` - Coordinate pipeline

### 4. Field Population Strategy ✅
**Decision**: **Hybrid approach** - Parse TypeScript + manual enhancement
- Auto-generate SQL from `types.ts` interfaces (80% automation)
- Human review and add: extraction hints, help text, validation rules (20% manual)
- Script to be created: `scripts/generate-form-definitions.js`

### 5. Calculated Fields Philosophy ✅
**Decision**: **Extract ALL printed values** (including calculated totals)
- Use Case: Form 1040 Line 1z shows "Total wages: $75,000"
  - **Extract**: Line 1z printed value ($75,000) as `line1z_totalWages_printed`
  - **Calculate**: Line 1z via rule (SUM of W-2 Box 1) as `line1z_totalWages_calculated`
  - **Validate**: Compare printed vs calculated
- **NEVER** use extracted calculated fields in tax calculation
- **ALWAYS** compute via rule engine
- Gemini extracts values only, never computes

### 6. Caching Strategy ✅
**Decision**: **Redis with 24-hour TTL**
- Cache: Form metadata (changes infrequently)
- TTL: 24 hours
- Invalidation: Manual via admin API when forms updated
- Keys: `metadata:{formType}`, `fields:{formType}`

### 7. Admin UI Timeline ✅
**Decision**: **Phase 4** (Week 4-5) - Mid-project
- Initial: Use SQL scripts for form definitions
- Later: Build admin UI for ongoing maintenance
- Priority: Core extraction/calculation first, admin UI second

## Contract-First Development ✅

**ALL services MUST respect contracts from Day 1:**

1. **Form Metadata Contract** - `GET /api/v1/form-registry/metadata/{formType}`
2. **Extraction Response Contract** - `POST /api/v1/extraction/extract`
3. **Tax Calculation Request Contract** - `POST /api/v1/tax-engine/calculate/individual`
4. **Rule Service Contract** - `GET /api/v1/rules/templates/{jurisdiction}/{taxYear}`
5. **Frontend Component Contract** - All components use canonical field names

## Implementation Order

### Phase 0: Contracts & Foundation (Week 1)
1. Create Form Registry Service
2. Create database schema (all tables)
3. Generate SQL scripts from TypeScript types
4. Define OpenAPI contracts
5. Generate client SDKs
6. Configure Redis cache

### Phase 1: Extraction Refactor (Week 2-3)
1. Create 15+ SOLID service classes
2. Unit test each service
3. Remove calculations from extraction
4. Use database-driven prompts

### Phase 2: Rule Engine Integration (Week 3-4)
1. Extend rule-service with new tables
2. Auto-generate rule templates
3. Update tax-engine to use rules
4. Connect all form fields to calculations

### Phase 3: Frontend Integration (Week 4-5)
1. Use form registry API
2. Dynamic form rendering
3. Contract-based components

### Phase 4: Testing (Week 5-6)
1. Unit tests (90%+ coverage)
2. Contract tests
3. Integration tests
4. E2E tests

### Phase 5: Admin UI (Week 6-7) - Optional
1. Form management CRUD
2. Field editor
3. Rule editor

## Non-Negotiables

- ✅ Gemini NEVER calculates (only extracts)
- ✅ All prompts from database (NO hardcoding)
- ✅ All field names canonical (from form registry)
- ✅ Each service class < 200 lines (SOLID)
- ✅ Contracts respected by ALL services
- ✅ Forms auto-connect to tax calculations (via rules)
- ✅ Redis caching for performance
