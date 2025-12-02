# Data Model: System Deep Analysis & Gap Identification

**Feature**: System Deep Analysis & Gap Identification  
**Phase**: Phase 1 (Design & Contracts)  
**Date**: 2024-12-02

---

## Overview

This document defines the data structures for the analysis output. Unlike typical features, this is a documentation/analysis feature, so the "data model" describes the schema of generated reports rather than database entities.

**Output Format**: Markdown files with embedded JSON/YAML for structured data  
**Storage**: File system (`specs/013-system-deep-analysis/analysis/`)

---

## Entity Definitions

### 1. APIEndpoint

**Purpose**: Represents a single REST API endpoint discovered in backend services.

```typescript
interface APIEndpoint {
  id: string;                    // Unique identifier (service:method:path)
  service: string;               // Microservice name (e.g., "auth-service")
  controller: string;            // Controller class name (e.g., "AuthController.java")
  method: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  path: string;                  // Full path (e.g., "/api/v1/auth/login")
  lineNumber: number;            // Line number in source file
  consumers: UIComponentRef[];   // List of frontend consumers
  status: "USED" | "UNUSED";     // Whether any frontend consumes this
  swaggerDocumented: boolean;    // Whether endpoint appears in Swagger
}

interface UIComponentRef {
  component: string;             // Component file name
  path: string;                  // Full path to component file
  usage: string;                 // How it's used (e.g., "api.auth.login()")
}
```

---

### 2. UIComponent

**Purpose**: Represents a React component in the frontend application.

```typescript
interface UIComponent {
  id: string;                    // Unique identifier (file path)
  name: string;                  // Component name (e.g., "LoginForm")
  path: string;                  // File path (e.g., "src/components/auth/LoginForm.tsx")
  category: "page" | "component" | "form" | "wizard" | "dashboard" | "shared";
  implementationStatus: "COMPLETE" | "PARTIAL" | "MISSING" | "PLANNED";
  apiDependencies: APIEndpointRef[];  // APIs this component calls
  userJourneys: string[];        // User journeys this component participates in
  issues: ComponentIssue[];      // Known issues or gaps
}

interface APIEndpointRef {
  endpoint: string;              // API endpoint path
  method: string;                // HTTP method
  status: "CONNECTED" | "API_MISSING";  // Whether backend exists
}

interface ComponentIssue {
  severity: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
  description: string;
  remediation: string;
}
```

---

### 3. Service

**Purpose**: Represents a backend microservice.

```typescript
interface Service {
  name: string;                  // Service name (e.g., "auth-service")
  port: number;                  // Default port
  directory: string;             // Directory path
  endpoints: APIEndpoint[];      // List of endpoints
  endpointCount: number;         // Total number of endpoints
  swaggerStatus: "AVAILABLE" | "PARTIAL" | "MISSING";
  swaggerUrl: string | null;     // Swagger UI URL if available
  databaseConnection: string;    // Database URL from configuration
  integrations: ServiceIntegration[];  // Other services this calls
}

interface ServiceIntegration {
  targetService: string;         // Service being called
  method: "REST" | "DATABASE" | "MESSAGE";
  status: "WORKING" | "DISCONNECTED" | "UNKNOWN";
  notes: string;
}
```

---

### 4. UserJourney

**Purpose**: Represents an end-to-end user workflow.

```typescript
interface UserJourney {
  id: string;                    // Unique identifier
  name: string;                  // Journey name (e.g., "Individual Tax Filing")
  description: string;           // Brief description
  actor: string;                 // User role (e.g., "Taxpayer", "Auditor")
  steps: JourneyStep[];          // Ordered list of steps
  overallStatus: "COMPLETE" | "PARTIAL" | "MISSING";
  completionPercentage: number;  // 0-100
  criticalGaps: string[];        // List of critical gaps
}

interface JourneyStep {
  order: number;                 // Step order (1, 2, 3...)
  name: string;                  // Step name (e.g., "Document Upload")
  description: string;           // What this step does
  status: "COMPLETE" | "PARTIAL" | "MISSING";
  uiComponents: string[];        // UI components for this step
  apiEndpoints: string[];        // API endpoints for this step
  notes: string;                 // Additional details
  blockers: string[];            // What's blocking completion
}
```

---

### 5. Gap

**Purpose**: Represents an identified deficiency in the system.

```typescript
interface Gap {
  id: string;                    // Unique identifier (e.g., "GAP-001")
  title: string;                 // Brief title
  description: string;           // Detailed description
  category: "API" | "UI" | "INTEGRATION" | "DOCUMENTATION" | "SECURITY" | "LOGIC";
  severity: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
  affectedJourneys: string[];    // User journeys affected
  affectedServices: string[];    // Services affected
  affectedComponents: string[];  // UI components affected
  existingSpec: string | null;   // Reference to existing spec (1-12) if any
  remediation: RemediationStep[];
  estimatedEffort: "SMALL" | "MEDIUM" | "LARGE" | "XLARGE";
  priority: number;              // 1-100 (higher = more urgent)
}

interface RemediationStep {
  order: number;
  action: string;
  details: string;
}
```

---

### 6. RuleConfiguration

**Purpose**: Represents a tax rule and its implementation status.

```typescript
interface RuleConfiguration {
  code: string;                  // Rule code (e.g., "MUNICIPAL_TAX_RATE")
  name: string;                  // Rule name
  category: "TAX_RATES" | "INCOME_INCLUSION" | "DEDUCTIONS" | "PENALTIES" | "FILING" | "ALLOCATION" | "WITHHOLDING" | "VALIDATION";
  implementationStatus: "DYNAMIC" | "HARDCODED" | "NOT_IMPLEMENTED";
  currentLocation: string;       // Where rule is currently defined
  expectedLocation: string;      // Where rule should be defined
  defaultValue: string | number; // Current default value
  configurable: boolean;         // Whether it can be changed without code
  notes: string;
}
```

---

### 7. SequenceFlow

**Purpose**: Represents a system sequence diagram with implementation status.

```typescript
interface SequenceFlow {
  id: string;                    // Unique identifier
  name: string;                  // Flow name (e.g., "User Authentication Flow")
  description: string;           // Brief description
  actors: string[];              // Participants in the flow
  steps: SequenceStep[];         // Ordered sequence steps
  diagram: string;               // Mermaid diagram source
  overallStatus: "IMPLEMENTED" | "PARTIAL" | "PLANNED";
}

interface SequenceStep {
  order: number;
  from: string;                  // Source actor/service
  to: string;                    // Target actor/service
  action: string;                // What's being done
  status: "IMPLEMENTED" | "PARTIAL" | "MISSING";
  notes: string;
}
```

---

### 8. DataFlow

**Purpose**: Represents how data (especially sensitive data) flows through the system.

```typescript
interface DataFlow {
  id: string;                    // Unique identifier
  dataType: string;              // Type of data (e.g., "SSN", "EIN")
  sensitivity: "HIGH" | "MEDIUM" | "LOW";
  entryPoints: string[];         // Where data enters (e.g., "extraction-service")
  flowPath: DataFlowStep[];      // How data moves through system
  storageLocations: string[];    // Where data is persisted
  protectionMeasures: string[];  // How data is protected
  gaps: string[];                // Security gaps identified
}

interface DataFlowStep {
  order: number;
  component: string;             // Service or UI component
  action: string;                // What happens to data
  protection: string;            // How data is protected here
  status: "SECURE" | "NEEDS_REVIEW" | "VULNERABLE";
}
```

---

## Output Report Schemas

### API Coverage Report Schema

```yaml
# api-coverage-report.md structure
summary:
  totalEndpoints: number
  usedEndpoints: number
  unusedEndpoints: number
  coveragePercentage: number

byService:
  - serviceName: string
    endpoints:
      - method: string
        path: string
        status: USED | UNUSED
        consumers: string[]
```

### User Journey Report Schema

```yaml
# user-journey-report.md structure
journeys:
  - name: string
    actor: string
    completionPercentage: number
    steps:
      - name: string
        status: COMPLETE | PARTIAL | MISSING
        components: string[]
        apis: string[]
        notes: string
```

### Gap Report Schema

```yaml
# gap-report.md structure
summary:
  critical: number
  high: number
  medium: number
  low: number

gaps:
  - id: string
    title: string
    severity: CRITICAL | HIGH | MEDIUM | LOW
    category: string
    affectedJourneys: string[]
    remediation: string
    existingSpec: string | null
```

---

## Data Relationships

```
Service ─────────────< APIEndpoint
    │                      │
    └─ integrations        └─ consumers
                               │
UIComponent ─────────────────┘
    │
    └─ apiDependencies
    │
    └─ participatesIn ───────> UserJourney
                                   │
                                   └─ steps ──────> JourneyStep
                                                        │
Gap ──────────────────────────────────────────────────┘
    │
    └─ affectedJourneys
    └─ affectedServices
    └─ affectedComponents

RuleConfiguration ───────────> Service (where implemented)

SequenceFlow ──────────────> Service (participants)

DataFlow ──────────────────> Service (components in path)
```
