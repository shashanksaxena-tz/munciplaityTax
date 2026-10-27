# components

Top-level React components for the Municipality Tax Management System application, including feature-rich dashboards, wizards, forms, and viewer components.

<!-- AUTO-GENERATED: Purpose -->
## Purpose

The `components` directory contains large-scale, feature-complete React components that compose together to create the complete tax management application. Unlike the smaller, reusable components in `src/components`, these are page-level or feature-level components that integrate multiple services, hooks, and UI elements to deliver complete user experiences.

These components represent major application features like tax filing wizards, dashboard views, document processing, and reconciliation interfaces. They are the primary building blocks used by `App.tsx` and `TaxFilingApp.tsx` to assemble the complete application.
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Key Files -->
## Key Files

| File/Directory | Description |
|---------------|-------------|
| **Dashboards** |
| `Dashboard.tsx` | Main application dashboard with overview metrics |
| `BusinessDashboard.tsx` | Business taxpayer dashboard |
| `AuditorDashboard.tsx` | Municipal auditor workflow dashboard |
| `LedgerDashboard.tsx` | Accounting ledger and trial balance view |
| `RuleManagementDashboard.tsx` | Business rules configuration (largest component: 85KB) |
| **Wizards** |
| `NetProfitsWizard.tsx` | Net profits tax filing wizard |
| `WithholdingWizard.tsx` | W1/W2 withholding tax wizard |
| `ReconciliationWizard.tsx` | Multi-period reconciliation wizard |
| **Forms & Filing** |
| `BusinessRegistration.tsx` | Business entity registration form |
| `RefundRequest.tsx` | Tax refund request form |
| `FilerAccountStatement.tsx` | Taxpayer account statement view |
| **Document Processing** |
| `ExtractionReview/` | AI extraction review components (4 files) |
| `PdfViewer/` | PDF document viewer components (4 files) |
| `DocumentViewer.tsx` | Document display and annotation |
| `SubmissionDocuments.tsx` | Document submission management |
| **Tax Calculations** |
| `ExtractionSummary.tsx` | Extracted data summary and validation |
| `ResultsSection.tsx` | Tax calculation results display |
| `ReviewSection.tsx` | Return review and validation (40KB) |
| `ReturnReviewPanel.tsx` | Final return review before submission |
| **Reconciliation** |
| `ReconciliationReport.tsx` | Reconciliation outcome report |
| `ReconciliationIssuesList.tsx` | Issues and discrepancies list |
| `ReconciliationStatusBadge.tsx` | Visual status indicators |
| `DiscrepancyView.tsx` | Detailed discrepancy analysis |
| **NOL & Schedules** |
| `NOLScheduleView.tsx` | Net Operating Loss schedule viewer |
| `CumulativeTotalsPanel.tsx` | Multi-period cumulative calculations |
| `PeriodHistoryTable.tsx` | Historical period data table |
| `TrialBalance.tsx` | Accounting trial balance display |
| **Upload & Processing** |
| `UploadSection.tsx` | Document upload interface |
| `ProcessingLoader.tsx` | Processing status and progress |
| **Configuration** |
| `BusinessRuleConfigScreen.tsx` | Business rule configuration UI |
| `RuleConfigurationScreen.tsx` | Rule definition and management |
| `FormSchemaViewer.tsx` | Form schema inspector |
| `FormSchemaTest.tsx` | Form schema testing tool |
| `DesignConfigurator.tsx` | UI design system configurator |
| **Payment** |
| `payment/` | Payment processing components (7 files) |
| `PaymentGateway.tsx` | Payment processing interface |
| **Authentication** |
| `auth/` | Authentication components (5 files) |
| `ProtectedRoute.tsx` | Route authorization wrapper |
| **Profile** |
| `profile/` | User profile components (5 files) |
| `UserMenu.tsx` | User account menu |
| **Utility** |
| `Header.tsx` | Application header and navigation |
| `Tooltip.tsx` | Tooltip component |
| `AuditTrail.tsx` | Audit log viewer |
| `ExtractionProvenanceDisplay.tsx` | Data provenance tracking |
| `ui/` | UI primitive components (7 files) |
| `forms/` | Form field components (4 files) |
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Component Categories -->
## Component Categories

### 📊 Dashboards (5 components)
High-level overviews and control centers for different user roles:

```typescript
// Main dashboard with metrics
<Dashboard />

// Business taxpayer view
<BusinessDashboard />

// Auditor workflow
<AuditorDashboard />

// Ledger and accounting
<LedgerDashboard />

// Rules management (largest: 85,849 bytes)
<RuleManagementDashboard />
```

**Features:**
- Real-time metrics and KPIs
- Role-based views
- Quick actions and navigation
- Data visualization

### 🧙 Wizards (3 components)
Step-by-step guided workflows:

```typescript
// Net profits filing
<NetProfitsWizard />

// W1/W2 withholding
<WithholdingWizard />

// Multi-period reconciliation
<ReconciliationWizard />
```

**Features:**
- Multi-step navigation
- Form validation at each step
- Progress indication
- Draft saving

### 📝 Forms & Registration (3 components)
Data entry and registration forms:

```typescript
<BusinessRegistration />
<RefundRequest />
<FilerAccountStatement />
```

### 📄 Document Processing (6 components + 2 folders)
Document upload, viewing, and AI extraction:

```typescript
// Document viewer
<DocumentViewer />

// PDF viewing with annotations
<PdfViewer />

// AI extraction review
<ExtractionReview />

// Upload interface
<UploadSection />

// Processing status
<ProcessingLoader />
```

**Key Feature:** Integration with `extraction-service` backend for AI-powered data extraction

### 🧮 Tax Calculations & Review (4 components)
Tax calculation results and validation:

```typescript
// Calculation results
<ResultsSection />

// Review interface (40KB+)
<ReviewSection />

// Final review panel
<ReturnReviewPanel />

// Extraction summary
<ExtractionSummary />
```

### 🔄 Reconciliation (4 components)
Multi-period reconciliation and discrepancy management:

```typescript
<ReconciliationReport />
<ReconciliationIssuesList />
<ReconciliationStatusBadge />
<DiscrepancyView />
```

### 📈 Schedules & NOL (4 components)
Net Operating Loss tracking and period management:

```typescript
<NOLScheduleView />
<CumulativeTotalsPanel />
<PeriodHistoryTable />
<TrialBalance />
```

### ⚙️ Configuration (5 components)
System configuration and testing tools:

```typescript
<BusinessRuleConfigScreen />
<RuleConfigurationScreen />
<FormSchemaViewer />
<FormSchemaTest />
<DesignConfigurator />
```

### 💳 Payment (8 components)
Payment processing and transactions:

```typescript
<PaymentGateway />
// + 7 components in payment/
```

### 🔐 Authentication & Authorization (6 components)
User authentication and profile management:

```typescript
<ProtectedRoute />
<UserMenu />
// + 5 components in auth/
// + 5 components in profile/
```

### 🎨 UI & Forms (11 components)
Reusable UI primitives and form elements:

```typescript
// 7 components in ui/
// 4 components in forms/
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Architecture Patterns -->
## Architecture Patterns

### Component Composition
Large components compose smaller ones:

```typescript
export function ReconciliationWizard() {
  return (
    <WizardContainer>
      <WizardHeader />
      <WizardSteps>
        <UploadSection />
        <ExtractionReview />
        <ResultsSection />
        <ReviewSection />
      </WizardSteps>
      <WizardNavigation />
    </WizardContainer>
  );
}
```

### Service Integration
Components integrate with backend services:

```typescript
export function ExtractionSummary() {
  const { data, isLoading } = useQuery({
    queryKey: ['extraction', documentId],
    queryFn: () => apiClient.get(`/api/extraction/${documentId}`)
  });
  
  // Render extraction results
}
```

### State Management
Complex state handled via React Query + local state:

```typescript
export function ReconciliationWizard() {
  // Server state
  const { data: reconciliation } = useQuery(...);
  
  // Local UI state
  const [currentStep, setCurrentStep] = useState(0);
  const [formData, setFormData] = useState({});
  
  // Mutations
  const submitMutation = useMutation(...);
}
```

### Form Handling
Forms integrate with form schema validation:

```typescript
export function BusinessRegistration() {
  const schema = useFormSchema('business-registration');
  const { errors, validate } = useFormValidation(schema);
  
  const handleSubmit = async (data) => {
    if (validate(data)) {
      await submitRegistration(data);
    }
  };
}
```
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Dependencies -->
## Dependencies

### Internal
- `../src/components/` - Reusable UI primitives
- `../src/services/` - API clients
- `../src/hooks/` - Custom hooks
- `../src/types/` - Type definitions
- `../contexts/` - React contexts

### External
- `react` - UI library
- `react-router-dom` - Routing
- `@tanstack/react-query` - Server state
- `react-hook-form` - Form management
- `zod` - Validation
- `lucide-react` - Icons
- `recharts` - Data visualization
- `@radix-ui/*` - Accessible primitives
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Used By -->
## Used By

- `../App.tsx` - Main application component
- `../TaxFilingApp.tsx` - Tax filing workflow app
- Other top-level components in this directory
<!-- END AUTO-GENERATED -->

<!-- AUTO-GENERATED: Patterns & Conventions -->
## Patterns & Conventions

### Component Structure
```typescript
// 1. Imports
import React from 'react';
import { apiClient } from '@/src/services/api';
import { useSomeHook } from '@/src/hooks/useSomeHook';

// 2. Types
interface Props {
  // ...
}

// 3. Component
export function ComponentName({ prop }: Props) {
  // 4. Server state (React Query)
  const { data, isLoading } = useQuery(...);
  
  // 5. Local state
  const [state, setState] = useState();
  
  // 6. Custom hooks
  const customLogic = useSomeHook();
  
  // 7. Effects
  useEffect(() => {}, []);
  
  // 8. Event handlers
  const handleClick = async () => {
    // mutation logic
  };
  
  // 9. Conditional rendering
  if (isLoading) return <Loader />;
  if (!data) return <EmptyState />;
  
  // 10. Main render
  return (
    <div>
      {/* Component UI */}
    </div>
  );
}
```

### Naming Conventions
- **Dashboards**: `*Dashboard.tsx`
- **Wizards**: `*Wizard.tsx`
- **Viewers**: `*Viewer.tsx` or `*View.tsx`
- **Lists**: `*List.tsx`
- **Forms**: `*Registration.tsx`, `*Request.tsx`
- **Sections**: `*Section.tsx`
- **Panels**: `*Panel.tsx`

### Error Handling
```typescript
export function SomeComponent() {
  const { data, error, isError } = useQuery(...);
  
  if (isError) {
    return (
      <ErrorBoundary error={error}>
        <ErrorMessage message={error.message} />
      </ErrorBoundary>
    );
  }
  
  // Normal render
}
```

### Loading States
```typescript
if (isLoading) {
  return <ProcessingLoader message="Loading data..." />;
}
```
<!-- END AUTO-GENERATED -->

<!-- CUSTOM SECTION: Project-Specific Notes -->
## Notes

Add component-specific architectural decisions, performance considerations, or integration notes here.

<!-- END CUSTOM SECTION -->

<!-- AUTO-GENERATED: Usage Examples -->
## Usage Examples

### Using Dashboard Components
```typescript
import { Dashboard } from '@/components/Dashboard';
import { AuditorDashboard } from '@/components/AuditorDashboard';

function App() {
  const { user } = useAuth();
  
  return user.role === 'AUDITOR' 
    ? <AuditorDashboard />
    : <Dashboard />;
}
```

### Using Wizards
```typescript
import { NetProfitsWizard } from '@/components/NetProfitsWizard';

function TaxFilingApp() {
  const [returnId, setReturnId] = useState<string>();
  
  return (
    <NetProfitsWizard
      returnId={returnId}
      onComplete={(id) => setReturnId(id)}
    />
  );
}
```

### Using Document Components
```typescript
import { DocumentViewer } from '@/components/DocumentViewer';
import { ExtractionReview } from '@/components/ExtractionReview';

function DocumentProcessing({ documentId }: Props) {
  return (
    <div className="grid grid-cols-2 gap-4">
      <DocumentViewer documentId={documentId} />
      <ExtractionReview documentId={documentId} />
    </div>
  );
}
```

### Protected Routes
```typescript
import { ProtectedRoute } from '@/components/ProtectedRoute';

<Route
  path="/admin"
  element={
    <ProtectedRoute requiredRole="ADMIN">
      <RuleManagementDashboard />
    </ProtectedRoute>
  }
/>
```
<!-- END AUTO-GENERATED -->
