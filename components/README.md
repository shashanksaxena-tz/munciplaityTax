# Components

Top-level React components for the Municipality Tax Management System.

## Overview

The `components` directory contains feature-rich, page-level React components that make up the complete tax management application. These are large-scale components that integrate multiple services, hooks, and smaller UI elements to deliver complete user experiences like dashboards, wizards, and document processing interfaces.

**Key Characteristics:**
- **Feature-Complete**: Each component represents a complete user workflow
- **Highly Integrated**: Components use services, hooks, and contexts
- **Large Scale**: Components range from 6KB to 85KB in size
- **User-Facing**: Direct mapping to application routes and features

## Structure

```
components/
├── Dashboards/
│   ├── Dashboard.tsx              # Main dashboard
│   ├── BusinessDashboard.tsx      # Business view
│   ├── AuditorDashboard.tsx       # Auditor workflow
│   ├── LedgerDashboard.tsx        # Accounting view
│   └── RuleManagementDashboard.tsx  # Rules config (85KB!)
├── Wizards/
│   ├── NetProfitsWizard.tsx       # Net profits filing
│   ├── WithholdingWizard.tsx      # W1/W2 withholding
│   └── ReconciliationWizard.tsx   # Multi-period reconciliation
├── Document Processing/
│   ├── ExtractionReview/          # AI extraction review
│   ├── PdfViewer/                 # PDF viewer
│   ├── DocumentViewer.tsx         # Document display
│   └── UploadSection.tsx          # File upload
├── Forms/
│   ├── BusinessRegistration.tsx
│   ├── RefundRequest.tsx
│   └── forms/                     # Form primitives
├── Payment/
│   ├── PaymentGateway.tsx
│   └── payment/                   # 7 payment components
├── Auth/
│   ├── ProtectedRoute.tsx
│   ├── UserMenu.tsx
│   ├── auth/                      # 5 auth components
│   └── profile/                   # 5 profile components
└── Shared/
    ├── ui/                        # 7 UI primitives
    ├── Header.tsx
    ├── Tooltip.tsx
    └── AuditTrail.tsx

Total: 50+ components across 43 files and 7 subdirectories
```

## Component Categories

### 📊 Dashboards (5)
Role-specific overview and control panels:
- **Dashboard.tsx** - Main application dashboard
- **BusinessDashboard.tsx** - Taxpayer view
- **AuditorDashboard.tsx** - Municipal auditor workflow
- **LedgerDashboard.tsx** - Accounting and trial balance
- **RuleManagementDashboard.tsx** - Rules configuration (largest component)

### 🧙‍♂️ Wizards (3)
Guided multi-step workflows:
- **NetProfitsWizard.tsx** - Net profits tax filing
- **WithholdingWizard.tsx** - W1/W2 withholding forms
- **ReconciliationWizard.tsx** - Multi-period reconciliation

### 📄 Document Processing (8)
Upload, view, and process documents with AI:
- **UploadSection.tsx** - File upload interface
- **DocumentViewer.tsx** - Document display
- **PdfViewer/** - PDF viewing and annotation
- **ExtractionReview/** - AI extraction review
- **ExtractionSummary.tsx** - Extracted data summary
- **ProcessingLoader.tsx** - Processing status

### 📝 Forms & Filing (7)
Data entry and submission forms:
- **BusinessRegistration.tsx** - Register business entity
- **RefundRequest.tsx** - Tax refund requests
- **FilerAccountStatement.tsx** - Account statement
- **forms/** - Form field components (4 files)

### 💳 Payment (8)
Payment processing and transactions:
- **PaymentGateway.tsx** - Payment interface
- **payment/** - Payment components (7 files)

### 🔐 Auth & Profile (11)
Authentication and user management:
- **ProtectedRoute.tsx** - Route authorization
- **UserMenu.tsx** - User account menu
- **auth/** - Auth components (5 files)
- **profile/** - Profile components (5 files)

### 🧮 Tax Calculations (4)
Tax results and validation:
- **ResultsSection.tsx** - Calculation results
- **ReviewSection.tsx** - Return review (40KB+)
- **ReturnReviewPanel.tsx** - Final review
- **ExtractionProvenanceDisplay.tsx** - Data provenance

### 🔄 Reconciliation (4)
Multi-period reconciliation:
- **ReconciliationReport.tsx** - Reconciliation results
- **ReconciliationIssuesList.tsx** - Issues and discrepancies
- **ReconciliationStatusBadge.tsx** - Status indicators
- **DiscrepancyView.tsx** - Detailed analysis

### 📈 Schedules & NOL (4)
Net Operating Loss and period tracking:
- **NOLScheduleView.tsx** - NOL schedule viewer
- **CumulativeTotalsPanel.tsx** - Multi-period totals
- **PeriodHistoryTable.tsx** - Historical data
- **TrialBalance.tsx** - Accounting trial balance

### ⚙️ Configuration (5)
System configuration and tools:
- **BusinessRuleConfigScreen.tsx** - Rule configuration
- **RuleConfigurationScreen.tsx** - Rule management
- **FormSchemaViewer.tsx** - Schema inspector
- **FormSchemaTest.tsx** - Schema testing
- **DesignConfigurator.tsx** - Design system config

## Quick Start

### Import and Use
```typescript
import { Dashboard } from '@/components/Dashboard';
import { NetProfitsWizard } from '@/components/NetProfitsWizard';
import { ProtectedRoute } from '@/components/ProtectedRoute';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route
        path="/filing"
        element={
          <ProtectedRoute>
            <NetProfitsWizard />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}
```

### Component Props
Components typically accept standard props:
```typescript
// Dashboard with user context
<BusinessDashboard userId={user.id} tenantId={tenant.id} />

// Wizard with callbacks
<NetProfitsWizard
  returnId={returnId}
  onComplete={(id) => navigate(`/returns/${id}`)}
  onCancel={() => navigate('/dashboard')}
/>

// Document viewer
<DocumentViewer documentId={docId} readOnly={false} />
```

## Common Patterns

### Protected Routes
```typescript
<ProtectedRoute requiredRole="ADMIN">
  <RuleManagementDashboard />
</ProtectedRoute>
```

### Wizard Navigation
```typescript
<NetProfitsWizard
  steps={['Upload', 'Extract', 'Review', 'Submit']}
  onStepChange={(step) => console.log(`Now on step: ${step}`)}
/>
```

### Document Processing
```typescript
<div className="grid grid-cols-2">
  <PdfViewer documentId={id} />
  <ExtractionReview documentId={id} />
</div>
```

## Integration

### Backend Services
Components integrate with backend microservices:
- **auth-service**: Authentication components
- **tax-engine-service**: Calculation components
- **extraction-service**: Document processing
- **ledger-service**: Accounting components
- **submission-service**: Filing components

### State Management
- **React Query** for server state
- **React Context** for global state
- **Local state** for UI state

## Testing

```bash
# Test all components
npm test components/

# Test specific component
npm test Dashboard.test.tsx

# Integration tests
npm test -- --integration
```

## Performance

### Code Splitting
Large components are lazy-loaded:
```typescript
const RuleManagementDashboard = lazy(() => 
  import('./RuleManagementDashboard')
);
```

### Memoization
Expensive components use React.memo:
```typescript
export const ExpensiveComponent = memo(function ExpensiveComponent(props) {
  // ...
});
```

## Related Directories

- [`../src/components/`](../src/components/) - Smaller, reusable UI components
- [`../src/services/`](../src/services/) - API clients
- [`../src/hooks/`](../src/hooks/) - Custom hooks
- [`../src/types/`](../src/types/) - TypeScript types
- [`../backend/`](../backend/) - Backend microservices
