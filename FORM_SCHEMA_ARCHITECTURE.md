# Form Schema System - Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SINGLE SOURCE OF TRUTH                               │
│                         config/form-schemas/                                 │
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ w2-schema    │  │ 1040-schema  │  │ schedule-c   │  │ 1099-nec     │   │
│  │   .json      │  │   .json      │  │  -schema.json│  │  -schema.json│   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
│                                                                              │
│  Each defines: id, label, type, weight, displayInUI, validationRules        │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ (copied to public/ at build time)
                                     │
                    ┌────────────────┴────────────────┐
                    │                                 │
                    ▼                                 ▼
    
    ┌───────────────────────────┐       ┌───────────────────────────┐
    │   FRONTEND (TypeScript)   │       │   BACKEND (Java)          │
    │                           │       │                           │
    │  services/                │       │  extraction-service/      │
    │  ├─ formSchemaService.ts  │       │  ├─ FormSchemaLoader     │
    │  │  • getDisplayFields()  │       │  │  • loadSchema()        │
    │  │  • getFieldWeights()   │       │  │  • getFieldWeights()   │
    │  │  • validateField()     │       │  │  • buildPrompt()       │
    │  └─ preloadSchemas()      │       │  └─ generateExtraction()  │
    │                           │       │                           │
    │  Used by:                 │       │  pdf-service/             │
    │  ├─ FieldWithSource.tsx   │       │  ├─ FieldMappingService  │
    │  ├─ ExtractionReview/     │       │  └─ FormTemplateService   │
    │  └─ FormSchemaViewer.tsx  │       │                           │
    └───────────────────────────┘       └───────────────────────────┘
                    │                                 │
                    │                                 │
                    ▼                                 ▼
    
    ┌───────────────────────────┐       ┌───────────────────────────┐
    │   UI COMPONENTS           │       │   EXTRACTION RESULTS      │
    │                           │       │                           │
    │  • Dynamic field display  │       │  • Field-level confidence │
    │  • Auto-validation        │       │  • Weight-based scoring   │
    │  • Schema-driven forms    │       │  • Structured extraction  │
    │  • Admin configuration    │       │  • PDF field mapping      │
    └───────────────────────────┘       └───────────────────────────┘
```

## Data Flow

### 1. Schema Definition (Design Time)
```
Developer creates/updates schema file
         ↓
config/form-schemas/w2-schema.json
         ↓
Version control (Git commit)
```

### 2. Build Process
```
npm run prebuild
         ↓
copy-schemas script executes
         ↓
Schemas copied to public/config/form-schemas/
         ↓
Vite build bundles application
```

### 3. Runtime Loading (Frontend)
```
App initialization
         ↓
preloadSchemas() called
         ↓
Fetch schemas from /config/form-schemas/*.json
         ↓
Cache in memory (Map<string, FormSchema>)
         ↓
Components consume via formSchemaService
```

### 4. Component Usage
```
<FieldWithSource form={w2Data} />
         ↓
useEffect(() => loadFields())
         ↓
getDisplayFields('W-2')
         ↓
Returns: [{ label: 'Employer', key: 'employer', format: 'text' }, ...]
         ↓
Render dynamic field list
```

### 5. Extraction Flow (Backend - Future)
```
Upload document
         ↓
ExtractionService.extractData()
         ↓
FormSchemaLoader.load('W-2')
         ↓
buildPrompt(schema.fields)
         ↓
Send to Gemini AI with field definitions
         ↓
Parse response using schema metadata
         ↓
Calculate confidence using field weights
         ↓
Return structured result
```

## Before vs After

### Before: Duplicated Definitions

```
UI Component (FieldWithSource.tsx)
┌─────────────────────────────────────┐
│ const FORM_DISPLAY_FIELDS = {      │
│   'W-2': [                          │
│     { label: 'Employer', ... },     │
│     { label: 'Federal Wages', ... } │
│   ]                                 │
│ }                                   │
└─────────────────────────────────────┘

Extraction Service (RealGeminiService.java)
┌─────────────────────────────────────┐
│ FIELD_WEIGHTS = Map.of(             │
│   "federalWages", "CRITICAL",       │
│   "employer", "HIGH"                │
│ )                                   │
│                                     │
│ String prompt = "Extract fields:    │
│   - Employer (Box c)                │
│   - Federal Wages (Box 1)"          │
└─────────────────────────────────────┘

PDF Service (FieldMappingService.java)
┌─────────────────────────────────────┐
│ Map<String, String> fieldMap = {    │
│   "employer" -> "txt_employer",     │
│   "federalWages" -> "txt_box_1"     │
│ }                                   │
└─────────────────────────────────────┘

❌ 3 separate definitions
❌ Manual sync required
❌ Prone to inconsistencies
```

### After: Single Source of Truth

```
Schema File (w2-schema.json)
┌─────────────────────────────────────────────────────┐
│ {                                                   │
│   "formType": "W-2",                               │
│   "fields": [                                       │
│     {                                               │
│       "id": "employer",                            │
│       "label": "Employer",                         │
│       "boxNumber": "c",                            │
│       "weight": "HIGH",                            │
│       "displayInUI": true,                         │
│       "pdfFieldName": "txt_employer"               │
│     },                                              │
│     {                                               │
│       "id": "federalWages",                        │
│       "label": "Federal Wages (Box 1)",            │
│       "boxNumber": "1",                            │
│       "type": "currency",                          │
│       "weight": "CRITICAL",                        │
│       "displayInUI": true,                         │
│       "pdfFieldName": "txt_box_1"                  │
│     }                                               │
│   ]                                                 │
│ }                                                   │
└─────────────────────────────────────────────────────┘
                       ▼
        ┌──────────────┴──────────────┐
        ▼                             ▼
    Frontend                      Backend
    reads dynamically             reads dynamically
    
✅ 1 definition
✅ Auto-sync
✅ Consistent everywhere
```

## Key Benefits Illustrated

### 1. Add New Field
```
OLD WAY:                          NEW WAY:
1. Update UI constant             1. Edit schema JSON
2. Update extraction prompt       
3. Update PDF mapping             DONE! ✨
4. Update validation              (All consumers auto-update)
5. Test all 3 places              
```

### 2. Change Field Label
```
OLD WAY:                          NEW WAY:
1. Find all label usages          1. Update "label" in schema
2. Update each manually           
3. Risk missing some              DONE! ✨
                                  (UI auto-reflects change)
```

### 3. Add Validation Rule
```
OLD WAY:                          NEW WAY:
1. Add validation in UI           1. Add "validationRules" to field
2. Add validation in backend      
3. Duplicate logic                DONE! ✨
                                  (Both sides use same rules)
```

## Admin Tools

```
┌─────────────────────────────────────────────────────────────────┐
│  Form Schema Viewer (/admin/schemas)                            │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ [W-2] [1040] [Schedule C] [1099-NEC] [1099-MISC] ...  │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
│  Stats: Total: 14 | Display: 7 | Required: 5 | Critical: 4     │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Order │ Field ID      │ Label              │ Type     │   │
│  ├───────┼───────────────┼────────────────────┼──────────┤   │
│  │   1   │ employer      │ Employer           │ text     │   │
│  │   2   │ employerEin   │ Employer EIN       │ text     │   │
│  │   3   │ federalWages  │ Federal Wages      │ currency │   │
│  │   4   │ medicareWages │ Medicare Wages     │ currency │   │
│  │   5   │ localWages    │ Local Wages        │ currency │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
│  [Export JSON] [Import JSON] [Validate All]                     │
└─────────────────────────────────────────────────────────────────┘
```

## Implementation Status

✅ Schema files created (7 forms)
✅ TypeScript service layer
✅ UI component integration
✅ Admin viewer tool
✅ Build scripts
✅ Documentation
⏳ Backend integration (planned)
⏳ API endpoints (planned)
⏳ Multi-year support (future)

---

**Next:** Backend integration with extraction service to complete the end-to-end flow.
