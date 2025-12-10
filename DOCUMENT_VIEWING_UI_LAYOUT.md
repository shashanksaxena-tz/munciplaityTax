# Document Viewing UI - Visual Layout

## ReturnReviewPanel Layout

```
┌─────────────────────────────────────────────────────────────────────┐
│ [<- Back to Queue]                                                  │
│                                                                       │
│ Return Review                                              [Approve] │
│ Taxpayer: John Doe | Tax Year: 2024 | Return ID: 12345    [Reject]  │
│                                                  [Request Docs]      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Left Column (2/3 width)          │  Right Column (1/3 width)       │
│  ════════════════════════          │  ════════════════════           │
│                                    │                                 │
│  ┌───────────────────────────────┐ │  ┌──────────────────────────┐  │
│  │ Return Information             │ │  │ Audit Trail             │  │
│  │ • Status: IN_REVIEW            │ │  │                         │  │
│  │ • Priority: HIGH               │ │  │ • SUBMISSION            │  │
│  │ • Risk Score: 75               │ │  │   Dec 1, 2024 10:00 AM  │  │
│  │ • Flagged Issues: 3            │ │  │                         │  │
│  └───────────────────────────────┘ │  │ • REVIEW_STARTED        │  │
│                                    │ │  │   Dec 5, 2024 2:30 PM   │  │
│  ┌───────────────────────────────┐ │  │                         │  │
│  │ ⚠️  Automated Audit Report     │ │  └──────────────────────────┘  │
│  │ Risk Level: HIGH               │ │                                 │
│  │                                │ │                                 │
│  │ Flagged Items:                 │ │                                 │
│  │ • Large deduction variance     │ │                                 │
│  │ • Missing documentation        │ │                                 │
│  └───────────────────────────────┘ │                                 │
│                                    │                                 │
│  ┌───────────────────────────────┐ │                                 │
│  │ 📄 Attached Documents (2)      │ │                                 │
│  ├───────────────────────────────┤ │                                 │
│  │ 📄 W2-2024.pdf                 │ │                                 │
│  │    Type: W-2  Size: 240 KB    │ │                                 │
│  │    Uploaded: Dec 1, 10:00 AM  │ │                                 │
│  │    ✓ Extracted (95%)     [↓]  │ │                                 │
│  ├───────────────────────────────┤ │                                 │
│  │ 📄 1099-NEC-2024.pdf           │ │                                 │
│  │    Type: 1099-NEC  Size: 180KB│ │                                 │
│  │    Uploaded: Dec 1, 10:05 AM  │ │                                 │
│  │    ✓ Extracted (92%)     [↓]  │ │                                 │
│  └───────────────────────────────┘ │                                 │
│                                    │                                 │
│  ┌───────────────────────────────┐ │                                 │
│  │ 📄 W2-2024.pdf          [↓]    │ │                                 │
│  ├───────────────────────────────┤ │                                 │
│  │ [<] Page 1 of 2        [>]    │ │                                 │
│  │ [-] 100% [+]          [⤢]    │ │                                 │
│  ├───────────────────────────────┤ │                                 │
│  │                                │ │                                 │
│  │   ┌────────────────────┐       │ │                                 │
│  │   │                    │       │ │                                 │
│  │   │   PDF PREVIEW      │       │ │                                 │
│  │   │                    │       │ │                                 │
│  │   │  [Highlighted      │       │ │                                 │
│  │   │   Field Region]    │       │ │                                 │
│  │   │                    │       │ │                                 │
│  │   └────────────────────┘       │ │                                 │
│  │                                │ │                                 │
│  └───────────────────────────────┘ │                                 │
│                                    │                                 │
│  ┌───────────────────────────────┐ │                                 │
│  │ ✨ Extracted Fields            │ │                                 │
│  │ Click on a field to highlight  │ │                                 │
│  ├───────────────────────────────┤ │                                 │
│  │ 📄 W-2  Page 1  95% confidence│ │                                 │
│  │ Detected W-2 form structure   │ │                                 │
│  │                                │ │                                 │
│  │ ┌─────────────────────────┐   │ │                                 │
│  │ │ 📍 Employer Name        │   │ │                                 │
│  │ │    Raw: ACME Corp       │   │ │                                 │
│  │ │    Page: 1       [98%] │   │ │                                 │
│  │ └─────────────────────────┘   │ │                                 │
│  │                                │ │                                 │
│  │ ┌─────────────────────────┐   │ │                                 │
│  │ │ 📍 Federal Wages        │   │ │                                 │
│  │ │    Raw: 85000.00        │   │ │                                 │
│  │ │    Processed: $85,000   │   │ │                                 │
│  │ │    Page: 1       [95%] │   │ │                                 │
│  │ └─────────────────────────┘   │ │                                 │
│  │                                │ │                                 │
│  └───────────────────────────────┘ │                                 │
│                                    │                                 │
└─────────────────────────────────────────────────────────────────────┘
```

## Component Hierarchy

```
ReturnReviewPanel
├── Header
│   ├── Back Button
│   └── Action Buttons (Approve/Reject/Request Docs)
│
├── Main Content Grid
│   ├── Left Column (lg:col-span-2)
│   │   ├── Return Information Card
│   │   ├── Audit Report Card
│   │   ├── SubmissionDocumentsList ← NEW
│   │   │   └── Document Items
│   │   │       ├── Document Metadata
│   │   │       ├── Extraction Status Badge
│   │   │       └── Download Button
│   │   │
│   │   ├── DocumentViewer ← NEW (when document selected)
│   │   │   ├── Header (filename + download)
│   │   │   └── PdfViewer (existing component)
│   │   │       ├── Navigation Controls
│   │   │       ├── Zoom Controls
│   │   │       ├── PDF Canvas
│   │   │       ├── HighlightOverlay (for selected field)
│   │   │       └── FieldSourceTooltip
│   │   │
│   │   └── ExtractionProvenanceDisplay ← NEW (when document has provenance)
│   │       └── Form Provenance Groups
│   │           ├── Form Header (type, page, confidence)
│   │           └── Field Items (clickable)
│   │               ├── Field Name
│   │               ├── Raw/Processed Values
│   │               └── Confidence Badge
│   │
│   └── Right Column (lg:col-span-1)
│       └── Audit Trail Card
│
└── Dialogs
    ├── Approve Dialog
    ├── Reject Dialog
    └── Document Request Dialog
```

## User Interaction Flow

```
1. Auditor navigates to Return Review
   ↓
2. View document list automatically loaded
   ↓
3. Click on document → DocumentViewer appears with PDF
   ↓
4. Scroll through PDF, view extraction data below
   ↓
5. Click on a field in ExtractionProvenanceDisplay
   ↓
6. PDF automatically highlights that field's location
   ↓
7. View confidence scores, raw/processed values
   ↓
8. Download document if needed
   ↓
9. Review all documents and data
   ↓
10. Make decision: Approve/Reject/Request More Docs
```

## Key Features Visualization

### Document List Item
```
┌────────────────────────────────────────────────────┐
│ 📄 W2-2024.pdf                              [↓]   │
│    Type: W-2      Size: 240 KB                     │
│    Uploaded: Dec 1, 2024 10:00 AM                  │
│    ✓ Extracted (95%)  ← Green badge                │
└────────────────────────────────────────────────────┘
```

### Extraction Status Badges
```
✓ Extracted (95%)     [Green]   - ExtractionStatus.COMPLETED
⏱ Processing          [Blue]    - ExtractionStatus.PROCESSING
✗ Failed              [Red]     - ExtractionStatus.FAILED
⚠ Pending             [Yellow]  - ExtractionStatus.PENDING
📄 N/A                [Gray]    - ExtractionStatus.NOT_APPLICABLE
```

### Field Confidence Colors
```
[98%]  Green  - confidence >= 0.9
[75%]  Yellow - confidence >= 0.7
[55%]  Red    - confidence < 0.7
```

### Interactive Field Card
```
┌───────────────────────────────────────┐
│ 📍 Employer Name              [98%]  │ ← Click to highlight in PDF
│    Raw: ACME Corp                     │
│    Page: 1                            │
└───────────────────────────────────────┘
     │
     └─→ Triggers PDF highlight:
         ┌─────────────────┐
         │ PDF Page 1      │
         │                 │
         │  ┌───────────┐  │ ← Highlighted region
         │  │ACME Corp  │  │   (yellow overlay)
         │  └───────────┘  │
         └─────────────────┘
```

## State Management

```typescript
// Document-related state
documents: SubmissionDocument[]           // All documents for submission
selectedDocument: SubmissionDocument      // Currently viewing
selectedFieldProvenance: {                // Currently highlighted field
  field: FieldProvenance
  formProvenance: FormProvenance
}

// Handlers
handleDocumentSelect()     // Updates selectedDocument
handleDocumentDownload()   // Downloads file
handleFieldClick()         // Updates selectedFieldProvenance
getHighlightedField()      // Computes highlight data for PDF
```

## Color Scheme (Existing Design System)

```
Primary:        #970bed (Purple gradient start)
Secondary:      #469fe8 (Blue)
Success:        #10b981 (Green)
Warning:        #f59e0b (Orange)
Error:          #ec1656 (Red)
Text Primary:   #0f1012 (Almost black)
Text Secondary: #5d6567 (Gray)
Border:         #dcdede (Light gray)
Background:     #f8f9fa (Off-white)
```
