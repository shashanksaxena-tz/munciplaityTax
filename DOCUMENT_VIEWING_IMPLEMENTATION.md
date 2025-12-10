# Document Viewing Feature - Implementation Summary

## Overview
Added comprehensive document viewing capabilities to the auditor review panel, enabling auditors to view, download, and inspect documents attached to tax return submissions with full extraction provenance tracking.

## Components Created

### 1. SubmissionDocumentsList.tsx
**Purpose:** Displays a list of all documents attached to a submission with metadata.

**Features:**
- Shows document name, file type, size, upload date
- Displays extraction status with color-coded indicators (Completed, Processing, Failed, Pending)
- Shows extraction confidence scores as percentages
- Supports document selection for viewing
- Download button for each document
- Highlights selected document
- Responsive layout

**Props:**
```typescript
{
  documents: SubmissionDocument[];
  selectedDocumentId?: string;
  onDocumentSelect: (document: SubmissionDocument) => void;
  onDocumentDownload: (document: SubmissionDocument) => void;
}
```

### 2. DocumentViewer.tsx
**Purpose:** Displays PDF documents inline with field highlighting.

**Features:**
- Loads documents from backend API (`/api/v1/submissions/{id}/documents/{docId}`)
- Integrates with existing PdfViewer component
- Parses and displays field provenance for highlighting
- Shows loading state and error handling
- Download functionality
- Close button (optional)
- Supports base64-encoded PDFs

**Props:**
```typescript
{
  document: SubmissionDocument;
  submissionId: string;
  onClose?: () => void;
  highlightedField?: {
    fieldName: string;
    boundingBox?: BoundingBox;
    formType: string;
    pageNumber: number;
    confidence?: number;
  };
}
```

### 3. ExtractionProvenanceDisplay.tsx
**Purpose:** Shows detailed field-level extraction information with interactive highlighting.

**Features:**
- Displays all extracted fields grouped by form type
- Shows raw and processed values for each field
- Displays confidence scores with color-coding (green ≥90%, yellow ≥70%, red <70%)
- Click-to-highlight functionality for fields
- Page number and extraction reason display
- Scrollable list for many fields
- Empty state when no provenance data

**Props:**
```typescript
{
  provenance: FormProvenance[];
  onFieldClick?: (field: FieldProvenance, formProvenance: FormProvenance) => void;
  selectedFieldName?: string;
}
```

## Utility Functions (utils/documentUtils.ts)

### getDocumentUploadDate(doc)
Handles backward compatibility for dual upload date fields.

### parseFieldProvenance(json)
Safely parses JSON field provenance with error handling.

### normalizeValue(value)
Normalizes values for comparison (handles null, undefined, empty strings).

### isDifferentValue(value1, value2)
Compares two values after normalization.

## Type Additions (types.ts)

### ExtractionStatus Enum
```typescript
enum ExtractionStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  NOT_APPLICABLE = 'NOT_APPLICABLE'
}
```

### SubmissionDocument Interface Updates
Added fields:
- `documentId`: Reference to document in storage
- `formType`: Type of tax form (W-2, 1099, etc.)
- `pageCount`: Number of pages in document
- `extractionConfidence`: Overall confidence score (0-1)
- `extractionResult`: JSON string of extraction results
- `fieldProvenance`: JSON string with field-level provenance
- `extractionStatus`: Current status of extraction
- `tenantId`: Multi-tenant support

### SubmissionDocumentWithProvenance Interface
Extended interface with parsed provenance data.

## Integration with ReturnReviewPanel

### New State Variables
```typescript
const [documents, setDocuments] = useState<SubmissionDocument[]>([]);
const [selectedDocument, setSelectedDocument] = useState<SubmissionDocument | null>(null);
const [selectedFieldProvenance, setSelectedFieldProvenance] = useState<{
  field: FieldProvenance;
  formProvenance: FormProvenance;
} | null>(null);
```

### New Handler Functions
- `handleDocumentSelect(document)`: Selects a document for viewing
- `handleDocumentDownload(document)`: Downloads a document
- `handleFieldClick(field, formProvenance)`: Highlights a field in the PDF
- `getHighlightedField()`: Computes highlight data for PDF viewer

### API Integration
- Loads documents from `/api/v1/submissions/{id}/documents`
- Downloads from `/api/v1/submissions/{id}/documents/{docId}`
- Provenance endpoint: `/api/v1/submissions/{id}/documents/{docId}/provenance`

### Layout Updates
Documents section added to the left column (lg:col-span-2) after audit report:
1. Document list with selection
2. Document viewer (when document selected)
3. Extraction provenance display (when document has provenance)

## API Compatibility

The implementation is compatible with the existing backend API:
- **SubmissionController** endpoints in `backend/submission-service`
- **SubmissionDocument** entity model
- **DocumentProvenanceResponse** DTO

## Security Considerations

1. **File Download Security:**
   - Filenames are sanitized to prevent header injection
   - Content-Type headers include `X-Content-Type-Options: nosniff`
   - File downloads use proper MIME type validation

2. **Input Validation:**
   - JSON parsing wrapped in try-catch blocks
   - Array type checking before iteration
   - Null/undefined safety throughout

3. **Access Control:**
   - All API calls respect tenant isolation
   - Document access verified through submission ownership
   - No client-side credentials stored

4. **XSS Prevention:**
   - React's built-in escaping for all user-generated content
   - No dangerouslySetInnerHTML usage
   - File names and metadata properly escaped

## Testing Recommendations

### Manual Testing Steps:
1. Navigate to auditor dashboard
2. Select a submission with documents
3. Verify document list displays correctly
4. Click on a document to view it
5. Verify PDF loads and displays
6. Click on extracted fields in provenance panel
7. Verify field highlighting in PDF
8. Test document download functionality
9. Test with submissions without documents
10. Test with documents without extraction data

### Edge Cases to Test:
- Empty document list
- Large PDF files
- PDFs with many pages
- Documents with no extraction data
- Documents with failed extraction
- Multiple documents per submission
- Network failures during load

## Future Enhancements (Not Implemented)

These features were listed in the original requirements but marked as "Future":

1. **Split-Screen Layout:**
   - PDF on left, data on right side-by-side
   - Synchronized scrolling
   - Responsive breakpoints

2. **Advanced Field Linking:**
   - Click field in data panel to highlight in PDF
   - Click PDF region to show extracted field
   - Multi-select for comparing fields

3. **Enhanced Viewer Features:**
   - Annotation tools
   - Comparison view (original vs extracted)
   - Field validation indicators
   - Bulk document operations

## Performance Notes

- Documents are lazy-loaded (only when selected)
- PDF rendering uses react-pdf with worker threads
- Large file warning in build (consider code splitting)
- JSON parsing cached in component state

## Dependencies Used

- **react-pdf**: PDF rendering
- **pdfjs-dist**: PDF.js library
- **lucide-react**: Icon components
- Existing PdfViewer, HighlightOverlay, FieldSourceTooltip components

## Build Output

Build successful with no errors:
- Bundle size: ~1.1 MB (gzipped: ~301 KB)
- CSS: ~71 KB (gzipped: ~13 KB)
- No TypeScript errors
- No linting errors
