# MuniTax - Dublin Municipality Tax Calculator (AI Developer Guide)

## 1. Project Overview
**MuniTax** is a React-based web application designed to help individuals and businesses in the Dublin Municipality calculate and file their local taxes. It supports document upload (with AI extraction), manual form entry, rule-based tax calculation, and generating tax return summaries.

## 2. Technology Stack
- **Framework**: React 18 (Vite)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Icons**: Lucide React
- **State Management**: React `useState` (Centralized in `App.tsx`)
- **Persistence**: LocalStorage (`sessionService.ts`)
- **AI Integration**: Gemini API (for document extraction)

## 3. Directory Structure
```
/
├── App.tsx                 # Main entry point & central state manager
├── types.ts                # Core TypeScript interfaces (Forms, Session, Rules)
├── components/             # UI Components
│   ├── Dashboard.tsx       # Main landing page
│   ├── UploadSection.tsx   # File upload & AI processing trigger
│   ├── ReviewSection.tsx   # Form review & manual entry interface
│   ├── ResultsSection.tsx  # Calculation results & submission
│   ├── RuleConfigurationScreen.tsx # Individual tax rule config
│   ├── Business*.tsx       # Business-specific components (Dashboard, Registration, History)
│   ├── *Wizard.tsx         # Business filing wizards (Withholding, Net Profits, Reconciliation)
│   └── ...
└── services/               # Business Logic & External Services
    ├── taxRuleEngine.ts    # Individual tax calculation logic
    ├── businessRuleEngine.ts # Business tax calculation logic
    ├── geminiService.ts    # AI document extraction service
    ├── sessionService.ts   # LocalStorage wrapper
    └── pdfGenerator.ts     # PDF generation logic
```

## 4. Core Concepts & Data Models (`types.ts`)

### 4.1. Tax Return Session
The application state is encapsulated in a `TaxReturnSession` object.
- **Type**: `INDIVIDUAL` or `BUSINESS`
- **Status**: `DRAFT`, `SUBMITTED`, `AMENDED`, etc.
- **Data**: Contains `profile`, `settings`, `forms` (Individual), or `businessFilings` (Business).

### 4.2. Tax Forms (`TaxFormData`)
Polymorphic type handling various tax forms:
- **Individual**: `W2`, `W2G`, `1099-NEC/MISC`, `Schedule C/E/F`, `Local 1040`.
- **Business**: `Form W-1` (Withholding), `Form 27` (Net Profits), `Form W-3` (Reconciliation).

### 4.3. Application Flow (`AppStep`)
Navigation is controlled by the `step` state in `App.tsx`.
- **Individual Flow**: `DASHBOARD` -> `UPLOAD` -> `SUMMARY` -> `REVIEW` -> `RESULTS`.
- **Business Flow**: `DASHBOARD` -> `BUSINESS_DASHBOARD` -> `*WIZARD` -> `BUSINESS_HISTORY`.

## 5. Feature Deep Dive

### 5.1. Individual Tax Return
1.  **Document Upload**: Users upload PDFs/Images. `geminiService` extracts data into `TaxFormData` structures.
2.  **Review & Edit**: `ReviewSection` allows users to verify extracted data, add manual forms, and edit profiles.
3.  **Rule Engine**: `taxRuleEngine` calculates tax liability based on configurable rules (e.g., tax rates, credit limits).
4.  **Results**: Displays a breakdown of income, tax due, and credits. Allows submission (mock) and amendment.

### 5.2. Business Tax Return
1.  **Registration**: New businesses register via `BusinessRegistration`, creating a `BusinessProfile`.
2.  **Dashboard**: Central hub for business actions.
3.  **Withholding (Form W-1)**: Wizard for reporting employee withholding.
4.  **Net Profits (Form 27)**: Wizard for reporting business net profits, including Schedule X (Reconciliation) and Schedule Y (Allocation).
5.  **Reconciliation (Form W-3)**: End-of-year reconciliation between W-1 and W-2 data.

## 6. Key Services

### `services/taxRuleEngine.ts`
- **`calculateTaxes`**: Main function. Aggregates income from all forms, applies tax rates, calculates credits (taxes paid to other cities), and determines final liability.
- **Configuration**: Rules can be adjusted via `RuleConfigurationScreen`.

### `services/businessRuleEngine.ts`
- **`calculateBusinessTax`**: Logic for Net Profit tax calculation.
- **`calculateAllocation`**: Computes the "Business Allocation Percentage" based on Property, Payroll, and Sales factors.

### `services/geminiService.ts`
- **`extractTaxData`**: Sends file content to Gemini API with a prompt to extract structured JSON matching `TaxFormData` interfaces.

## 7. How to Update This Project

### Adding a New Tax Form
1.  **Update `types.ts`**: Add new `TaxFormType` enum and interface (e.g., `FormXYZ`). Add to `TaxFormData` union.
2.  **Update `geminiService.ts`**: Update the prompt to instruct Gemini to extract this new form type.
3.  **Update `ReviewSection.tsx`**: Add a case in `renderFormCard` to display the new form.
4.  **Update `taxRuleEngine.ts`**: Add logic to `calculateTaxes` to include income/deductions from this new form.

### Adding a New Business Wizard
1.  **Create Component**: Create `NewWizard.tsx` in `components/`.
2.  **Update `types.ts`**: Add new `AppStep` (e.g., `NEW_WIZARD`).
3.  **Update `App.tsx`**: Add route rendering logic for the new step.
4.  **Update `BusinessDashboard.tsx`**: Add a button to trigger the new wizard.

### Modifying Tax Logic
1.  **Edit `taxRuleEngine.ts`** or `businessRuleEngine.ts`.
2.  **Verify**: Ensure `TaxCalculationResult` reflects the changes.
3.  **UI**: If the logic requires new user inputs, update `RuleConfigurationScreen.tsx` or `BusinessRuleConfigScreen.tsx`.

## 8. Common Pitfalls
- **State Loss**: Refreshing the page will lose state unless `saveSession` is called. Ensure `handleSave` is triggered or auto-save is implemented where critical.
- **Type Safety**: When adding forms, ensure strict type guards are used to avoid runtime errors in the calculation engine.
- **Gemini Quotas**: Heavy use of the extraction feature may hit API rate limits. Implement error handling for 429 responses.
