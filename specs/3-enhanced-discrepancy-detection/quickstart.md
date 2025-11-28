# Quickstart Guide: Enhanced Discrepancy Detection

**Feature**: Enhanced Discrepancy Detection (10+ Validation Rules)  
**Audience**: Developers implementing or testing the validation feature  
**Time to Complete**: 15 minutes

## Overview

This guide walks through implementing and testing the enhanced discrepancy detection system with 10+ validation rules covering W-2s, Schedules, K-1s, municipal credits, and carryforwards.

## Prerequisites

- Java 21 and Maven installed
- Node.js 18+ and npm installed
- PostgreSQL 16+ running locally
- Docker (optional, for running full stack)
- IDE with Java and TypeScript support (IntelliJ IDEA, VS Code)

## Architecture Quick Reference

```
┌──────────────────┐       ┌────────────────────┐       ┌──────────────────┐
│  React Frontend  │──────▶│  Gateway Service   │──────▶│ Tax Engine Svc   │
│  (Port 5173)     │       │  (Port 8080)       │       │  (Port 8081)     │
└──────────────────┘       └────────────────────┘       └──────────────────┘
                                                                │
                                                                ▼
                                                         ┌──────────────────┐
                                                         │  PostgreSQL DB   │
                                                         │  (Port 5432)     │
                                                         └──────────────────┘
```

**Validation Flow**:
1. User completes tax return in frontend
2. Clicks "Review & Submit" button
3. Frontend calls `POST /api/tax-engine/validate` via gateway
4. Tax engine service runs 22 validation rules (FR-001 through FR-022)
5. Returns `DiscrepancyReport` with issues categorized by severity
6. Frontend displays issues in `DiscrepancyView` component
7. User accepts/fixes issues, submits return (blocked if HIGH severity remains)

## Step 1: Set Up Database Schema

Create the discrepancy tables in your PostgreSQL database:

```bash
cd backend/tax-engine-service
psql -U postgres -d munitax_dublin -f src/main/resources/db/migration/V1_4__create_discrepancy_tables.sql
```

**SQL Migration File** (`V1_4__create_discrepancy_tables.sql`):

```sql
-- Create discrepancy_reports table
CREATE TABLE IF NOT EXISTS discrepancy_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tax_return_id UUID NOT NULL,
    has_discrepancies BOOLEAN NOT NULL DEFAULT false,
    validation_date TIMESTAMP NOT NULL DEFAULT NOW(),
    validation_rules_version VARCHAR(50) NOT NULL,
    tenant_id UUID NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_discrepancy_reports_tax_return ON discrepancy_reports(tax_return_id);
CREATE INDEX idx_discrepancy_reports_tenant ON discrepancy_reports(tenant_id);

-- Create discrepancy_issues table
CREATE TABLE IF NOT EXISTS discrepancy_issues (
    issue_id VARCHAR(100) PRIMARY KEY,
    discrepancy_report_id UUID NOT NULL REFERENCES discrepancy_reports(id) ON DELETE CASCADE,
    rule_id VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    field VARCHAR(255) NOT NULL,
    source_value DECIMAL(15, 2),
    form_value DECIMAL(15, 2),
    difference DECIMAL(15, 2),
    difference_percent DECIMAL(5, 2),
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('HIGH', 'MEDIUM', 'LOW')),
    message TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    is_accepted BOOLEAN NOT NULL DEFAULT false,
    acceptance_note TEXT,
    accepted_date TIMESTAMP,
    accepted_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_acceptance_data CHECK (
        (is_accepted = false AND acceptance_note IS NULL AND accepted_date IS NULL AND accepted_by IS NULL)
        OR
        (is_accepted = true AND acceptance_note IS NOT NULL AND accepted_date IS NOT NULL AND accepted_by IS NOT NULL)
    )
);

CREATE INDEX idx_discrepancy_issues_report ON discrepancy_issues(discrepancy_report_id);
CREATE INDEX idx_discrepancy_issues_severity ON discrepancy_issues(severity);
CREATE INDEX idx_discrepancy_issues_category ON discrepancy_issues(category);
```

**Verify Tables**:
```bash
psql -U postgres -d munitax_dublin -c "\dt discrepancy*"
```

Expected output:
```
                List of relations
 Schema |         Name          | Type  |  Owner
--------+-----------------------+-------+----------
 public | discrepancy_issues    | table | postgres
 public | discrepancy_reports   | table | postgres
```

---

## Step 2: Implement Backend Models

**File**: `backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/DiscrepancyReport.java`

```java
package com.munitax.taxengine.model;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyReport {
    private UUID id;
    private UUID taxReturnId;
    private boolean hasDiscrepancies;
    private List<DiscrepancyIssue> issues;
    private DiscrepancySummary summary;
    private Instant validationDate;
    private String validationRulesVersion;
    private UUID tenantId;
    private UUID createdBy;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscrepancySummary {
        private int totalIssues;
        private int highSeverityCount;
        private int mediumSeverityCount;
        private int lowSeverityCount;
        private int acceptedIssuesCount;
        private boolean blocksFiling;
    }
}
```

**File**: `backend/tax-engine-service/src/main/java/com/munitax/taxengine/model/DiscrepancyIssue.java`

```java
package com.munitax.taxengine.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyIssue {
    private String issueId;
    private String ruleId; // FR-001, FR-002, etc.
    private String category; // W-2, Schedule C, etc.
    private String field;
    private BigDecimal sourceValue;
    private BigDecimal formValue;
    private BigDecimal difference;
    private BigDecimal differencePercent;
    private Severity severity;
    private String message;
    private String recommendedAction;
    private boolean isAccepted;
    private String acceptanceNote;
    private Instant acceptedDate;
    private UUID acceptedBy;
    
    public enum Severity {
        HIGH, MEDIUM, LOW
    }
}
```

---

## Step 3: Implement Core Validation Service

**File**: `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/DiscrepancyValidator.java`

```java
package com.munitax.taxengine.service;

import com.munitax.taxengine.model.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscrepancyValidator {
    
    private final W2Validator w2Validator;
    private final ScheduleValidator scheduleValidator;
    private final K1Validator k1Validator;
    private final CreditValidator creditValidator;
    private final ReconciliationValidator reconciliationValidator;
    private final CarryforwardValidator carryforwardValidator;
    
    public DiscrepancyReport validateTaxReturn(
            UUID taxReturnId,
            List<TaxFormData> forms,
            TaxRulesConfig rules,
            UUID tenantId,
            UUID userId) {
        
        List<DiscrepancyIssue> allIssues = new ArrayList<>();
        
        // Run all validation rules
        allIssues.addAll(w2Validator.validate(forms, rules));
        allIssues.addAll(scheduleValidator.validate(forms, rules));
        allIssues.addAll(k1Validator.validate(forms, rules));
        allIssues.addAll(creditValidator.validate(forms, rules));
        allIssues.addAll(reconciliationValidator.validate(forms, rules));
        allIssues.addAll(carryforwardValidator.validate(forms, rules, tenantId));
        
        // Compute summary
        DiscrepancyReport.DiscrepancySummary summary = computeSummary(allIssues);
        
        return DiscrepancyReport.builder()
                .id(UUID.randomUUID())
                .taxReturnId(taxReturnId)
                .hasDiscrepancies(!allIssues.isEmpty())
                .issues(allIssues)
                .summary(summary)
                .validationDate(Instant.now())
                .validationRulesVersion("1.0.0")
                .tenantId(tenantId)
                .createdBy(userId)
                .build();
    }
    
    private DiscrepancyReport.DiscrepancySummary computeSummary(List<DiscrepancyIssue> issues) {
        long highCount = issues.stream().filter(i -> i.getSeverity() == DiscrepancyIssue.Severity.HIGH).count();
        long mediumCount = issues.stream().filter(i -> i.getSeverity() == DiscrepancyIssue.Severity.MEDIUM).count();
        long lowCount = issues.stream().filter(i -> i.getSeverity() == DiscrepancyIssue.Severity.LOW).count();
        long acceptedCount = issues.stream().filter(DiscrepancyIssue::isAccepted).count();
        boolean blocksFile = issues.stream().anyMatch(i -> 
            i.getSeverity() == DiscrepancyIssue.Severity.HIGH && !i.isAccepted());
        
        return DiscrepancyReport.DiscrepancySummary.builder()
                .totalIssues(issues.size())
                .highSeverityCount((int) highCount)
                .mediumSeverityCount((int) mediumCount)
                .lowSeverityCount((int) lowCount)
                .acceptedIssuesCount((int) acceptedCount)
                .blocksFiling(blocksFile)
                .build();
    }
}
```

---

## Step 4: Implement W2 Validator (Example)

**File**: `backend/tax-engine-service/src/main/java/com/munitax/taxengine/service/W2Validator.java`

```java
package com.munitax.taxengine.service;

import com.munitax.taxengine.model.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class W2Validator {
    
    public List<DiscrepancyIssue> validate(List<TaxFormData> forms, TaxRulesConfig rules) {
        List<DiscrepancyIssue> issues = new ArrayList<>();
        List<W2Form> w2Forms = forms.stream()
                .filter(f -> f instanceof W2Form)
                .map(f -> (W2Form) f)
                .toList();
        
        // FR-001: W-2 Box 1 vs Box 18 variance
        issues.addAll(validateBoxVariance(w2Forms, rules));
        
        // FR-002: Withholding rate validation
        issues.addAll(validateWithholdingRate(w2Forms, rules));
        
        // FR-003: Duplicate W-2 detection
        issues.addAll(detectDuplicates(w2Forms));
        
        return issues;
    }
    
    private List<DiscrepancyIssue> validateBoxVariance(List<W2Form> w2Forms, TaxRulesConfig rules) {
        List<DiscrepancyIssue> issues = new ArrayList<>();
        
        for (W2Form w2 : w2Forms) {
            BigDecimal box1 = w2.federalWages() != null ? BigDecimal.valueOf(w2.federalWages()) : BigDecimal.ZERO;
            BigDecimal box18 = w2.localWages() != null ? BigDecimal.valueOf(w2.localWages()) : BigDecimal.ZERO;
            
            if (box1.compareTo(BigDecimal.ZERO) == 0) continue;
            
            BigDecimal variance = box1.subtract(box18).abs()
                    .divide(box1, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            DiscrepancyIssue.Severity severity = null;
            if (variance.compareTo(BigDecimal.valueOf(20)) > 0) {
                severity = DiscrepancyIssue.Severity.HIGH;
            } else if (variance.compareTo(BigDecimal.valueOf(10)) > 0) {
                severity = DiscrepancyIssue.Severity.MEDIUM;
            }
            
            if (severity != null) {
                issues.add(DiscrepancyIssue.builder()
                        .issueId(UUID.randomUUID().toString())
                        .ruleId("FR-001")
                        .category("W-2")
                        .field("W-2 Box 18 Local Wages (" + w2.employerName() + ")")
                        .sourceValue(box1)
                        .formValue(box18)
                        .difference(box1.subtract(box18))
                        .differencePercent(variance.setScale(2, RoundingMode.HALF_UP))
                        .severity(severity)
                        .message(String.format("Box 18 is %s%% different from Box 1", variance.setScale(1, RoundingMode.HALF_UP)))
                        .recommendedAction("Verify you entered Box 18 correctly. For full-year Dublin employment, Box 18 should be similar to Box 1.")
                        .isAccepted(false)
                        .build());
            }
        }
        
        return issues;
    }
    
    private List<DiscrepancyIssue> validateWithholdingRate(List<W2Form> w2Forms, TaxRulesConfig rules) {
        List<DiscrepancyIssue> issues = new ArrayList<>();
        
        for (W2Form w2 : w2Forms) {
            BigDecimal localWages = w2.localWages() != null ? BigDecimal.valueOf(w2.localWages()) : BigDecimal.ZERO;
            BigDecimal localWithheld = w2.localWithheld() != null ? BigDecimal.valueOf(w2.localWithheld()) : BigDecimal.ZERO;
            
            if (localWages.compareTo(BigDecimal.ZERO) == 0) continue;
            
            BigDecimal rate = localWithheld.divide(localWages, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            // FR-002: Rate exceeds 3.0%
            if (rate.compareTo(BigDecimal.valueOf(3.0)) > 0) {
                issues.add(DiscrepancyIssue.builder()
                        .issueId(UUID.randomUUID().toString())
                        .ruleId("FR-002")
                        .category("W-2")
                        .field("W-2 Withholding Rate (" + w2.employerName() + ")")
                        .sourceValue(BigDecimal.valueOf(2.5)) // Expected max rate
                        .formValue(rate)
                        .difference(rate.subtract(BigDecimal.valueOf(2.5)))
                        .differencePercent(null)
                        .severity(DiscrepancyIssue.Severity.MEDIUM)
                        .message(String.format("Withholding rate of %s%% exceeds Dublin rate of 2.5%%", rate.setScale(2, RoundingMode.HALF_UP)))
                        .recommendedAction("Verify employer withheld at correct rate or check Box 19 entry.")
                        .isAccepted(false)
                        .build());
            }
            
            // FR-002: Zero withholding on high wages
            if (localWithheld.compareTo(BigDecimal.ZERO) == 0 && localWages.compareTo(BigDecimal.valueOf(25000)) > 0) {
                issues.add(DiscrepancyIssue.builder()
                        .issueId(UUID.randomUUID().toString())
                        .ruleId("FR-002")
                        .category("W-2")
                        .field("W-2 Withholding (" + w2.employerName() + ")")
                        .sourceValue(localWages.multiply(BigDecimal.valueOf(0.025))) // Expected withholding
                        .formValue(BigDecimal.ZERO)
                        .difference(localWages.multiply(BigDecimal.valueOf(0.025)))
                        .differencePercent(null)
                        .severity(DiscrepancyIssue.Severity.MEDIUM)
                        .message("No local tax withheld on $" + localWages.setScale(0, RoundingMode.HALF_UP) + " wages")
                        .recommendedAction("Verify employer withholds Dublin tax or if you need to make estimated payments.")
                        .isAccepted(false)
                        .build());
            }
        }
        
        return issues;
    }
    
    private List<DiscrepancyIssue> detectDuplicates(List<W2Form> w2Forms) {
        List<DiscrepancyIssue> issues = new ArrayList<>();
        Map<String, W2Form> seenForms = new HashMap<>();
        
        for (W2Form w2 : w2Forms) {
            String key = w2.employerEIN() + "|" + w2.employeeSSN() + "|" + 
                         String.format("%.2f", w2.federalWages()) + "|" +
                         String.format("%.2f", w2.localWages());
            
            if (seenForms.containsKey(key)) {
                issues.add(DiscrepancyIssue.builder()
                        .issueId(UUID.randomUUID().toString())
                        .ruleId("FR-003")
                        .category("W-2")
                        .field("Duplicate W-2 (" + w2.employerName() + ")")
                        .sourceValue(null)
                        .formValue(null)
                        .difference(null)
                        .differencePercent(null)
                        .severity(DiscrepancyIssue.Severity.HIGH)
                        .message("Duplicate W-2 detected - same employer, SSN, and amounts as previously uploaded W-2")
                        .recommendedAction("Remove duplicate to avoid double-counting income.")
                        .isAccepted(false)
                        .build());
            } else {
                seenForms.put(key, w2);
            }
        }
        
        return issues;
    }
}
```

---

## Step 5: Add API Endpoint

**File**: `backend/tax-engine-service/src/main/java/com/munitax/taxengine/controller/TaxEngineController.java`

```java
@PostMapping("/validate")
public ResponseEntity<DiscrepancyReport> validateTaxReturn(
        @RequestBody ValidationRequest request,
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @AuthenticationPrincipal UserDetails userDetails) {
    
    // Load tax forms from request
    List<TaxFormData> forms = convertRequestToForms(request);
    
    // Load tenant-specific rules
    TaxRulesConfig rules = taxRulesService.getRules(tenantId);
    
    // Run validation
    DiscrepancyReport report = discrepancyValidator.validateTaxReturn(
            request.getTaxReturnId(),
            forms,
            rules,
            tenantId,
            getUserIdFromPrincipal(userDetails)
    );
    
    // Save report
    discrepancyReportRepository.save(report);
    
    return ResponseEntity.ok(report);
}
```

---

## Step 6: Test Backend

**File**: `backend/tax-engine-service/src/test/java/com/munitax/taxengine/service/W2ValidatorTest.java`

```java
@SpringBootTest
class W2ValidatorTest {
    
    @Autowired
    private W2Validator w2Validator;
    
    @Test
    void testBoxVarianceHighSeverity() {
        // Given: W-2 with Box 18 90% lower than Box 1
        W2Form w2 = new W2Form(
                "12-3456789",
                "***-**-6789",
                "Acme Corp",
                75000.0,
                7500.0,  // 90% variance
                187.50
        );
        
        TaxRulesConfig rules = TaxRulesConfig.builder().build();
        
        // When: Validate
        List<DiscrepancyIssue> issues = w2Validator.validate(List.of(w2), rules);
        
        // Then: HIGH severity issue
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getRuleId()).isEqualTo("FR-001");
        assertThat(issues.get(0).getSeverity()).isEqualTo(DiscrepancyIssue.Severity.HIGH);
        assertThat(issues.get(0).getDifferencePercent()).isCloseTo(BigDecimal.valueOf(90.0), within(BigDecimal.valueOf(0.1)));
    }
    
    @Test
    void testDuplicateDetection() {
        // Given: Two identical W-2s
        W2Form w2a = new W2Form("12-3456789", "***-**-6789", "Acme", 50000.0, 50000.0, 1250.0);
        W2Form w2b = new W2Form("12-3456789", "***-**-6789", "Acme", 50000.0, 50000.0, 1250.0);
        
        TaxRulesConfig rules = TaxRulesConfig.builder().build();
        
        // When: Validate
        List<DiscrepancyIssue> issues = w2Validator.validate(List.of(w2a, w2b), rules);
        
        // Then: Duplicate detected
        assertThat(issues).anyMatch(i -> i.getRuleId().equals("FR-003"));
        assertThat(issues).anyMatch(i -> i.getSeverity() == DiscrepancyIssue.Severity.HIGH);
    }
}
```

**Run Tests**:
```bash
cd backend/tax-engine-service
mvn test -Dtest=W2ValidatorTest
```

---

## Step 7: Implement Frontend Component

**File**: `components/DiscrepancyView.tsx`

```typescript
import React from 'react';
import { AlertTriangle, CheckCircle, AlertOctagon, XCircle } from 'lucide-react';
import { DiscrepancyReport, DiscrepancyIssue } from '../types';

interface DiscrepancyViewProps {
  report: DiscrepancyReport;
  onAcceptIssue: (issueId: string, note: string) => void;
}

export const DiscrepancyView: React.FC<DiscrepancyViewProps> = ({ report, onAcceptIssue }) => {
  if (!report.hasDiscrepancies) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-xl p-4 flex items-start gap-3">
        <CheckCircle className="w-5 h-5 text-green-600 mt-0.5" />
        <div>
          <h4 className="font-bold text-green-800">No Discrepancies Found</h4>
          <p className="text-sm text-green-700">Your tax return passed all validation checks.</p>
        </div>
      </div>
    );
  }

  const groupedIssues = report.issues.reduce((acc, issue) => {
    if (!acc[issue.category]) acc[issue.category] = [];
    acc[issue.category].push(issue);
    return acc;
  }, {} as Record<string, DiscrepancyIssue[]>);

  return (
    <div className="space-y-6">
      {/* Summary Header */}
      <div className={`border rounded-xl p-5 ${
        report.summary.blocksFiling ? 'bg-red-50 border-red-200' : 'bg-yellow-50 border-yellow-200'
      }`}>
        <div className="flex items-center gap-3">
          {report.summary.blocksFiling ? (
            <XCircle className="w-6 h-6 text-red-600" />
          ) : (
            <AlertTriangle className="w-6 h-6 text-yellow-600" />
          )}
          <div>
            <h4 className="font-bold text-lg">
              {report.summary.blocksFiling ? 'Filing Blocked' : 'Warnings Detected'}
            </h4>
            <p className="text-sm">
              {report.summary.totalIssues} issue{report.summary.totalIssues !== 1 ? 's' : ''} found:
              {report.summary.highSeverityCount > 0 && ` ${report.summary.highSeverityCount} HIGH`}
              {report.summary.mediumSeverityCount > 0 && ` ${report.summary.mediumSeverityCount} MEDIUM`}
              {report.summary.lowSeverityCount > 0 && ` ${report.summary.lowSeverityCount} LOW`}
            </p>
          </div>
        </div>
      </div>

      {/* Issues by Category */}
      {Object.entries(groupedIssues).map(([category, issues]) => (
        <div key={category}>
          <h3 className="font-bold text-lg mb-3">{category}</h3>
          <div className="space-y-3">
            {issues.map(issue => (
              <DiscrepancyIssueCard 
                key={issue.issueId} 
                issue={issue} 
                onAccept={onAcceptIssue} 
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

const DiscrepancyIssueCard: React.FC<{
  issue: DiscrepancyIssue;
  onAccept: (issueId: string, note: string) => void;
}> = ({ issue, onAccept }) => {
  const [acceptanceNote, setAcceptanceNote] = React.useState('');
  const [showAcceptForm, setShowAcceptForm] = React.useState(false);

  const severityColors = {
    HIGH: 'border-red-300 bg-red-50',
    MEDIUM: 'border-yellow-300 bg-yellow-50',
    LOW: 'border-blue-300 bg-blue-50',
  };

  return (
    <div className={`border rounded-lg p-4 ${severityColors[issue.severity]}`}>
      <div className="flex justify-between items-start">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <span className={`text-xs font-bold px-2 py-1 rounded ${
              issue.severity === 'HIGH' ? 'bg-red-200 text-red-800' :
              issue.severity === 'MEDIUM' ? 'bg-yellow-200 text-yellow-800' :
              'bg-blue-200 text-blue-800'
            }`}>
              {issue.severity}
            </span>
            <span className="text-xs text-gray-500">{issue.ruleId}</span>
          </div>
          <h4 className="font-semibold">{issue.field}</h4>
          <p className="text-sm mt-1">{issue.message}</p>
          
          {issue.sourceValue !== null && issue.formValue !== null && (
            <div className="flex gap-4 mt-2 text-sm">
              <div>
                <span className="text-gray-600">Expected:</span>
                <span className="font-mono font-bold ml-2">${issue.sourceValue.toLocaleString()}</span>
              </div>
              <div>
                <span className="text-gray-600">Reported:</span>
                <span className="font-mono font-bold ml-2">${issue.formValue.toLocaleString()}</span>
              </div>
            </div>
          )}
          
          <div className="mt-2 text-sm bg-white bg-opacity-50 p-2 rounded">
            <span className="font-semibold">Recommended:</span> {issue.recommendedAction}
          </div>
        </div>

        {!issue.isAccepted && issue.severity !== 'HIGH' && (
          <button
            onClick={() => setShowAcceptForm(!showAcceptForm)}
            className="ml-4 px-3 py-1 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50"
          >
            Accept
          </button>
        )}
      </div>

      {showAcceptForm && (
        <div className="mt-4 pt-4 border-t">
          <label className="block text-sm font-semibold mb-2">
            Explain why you're accepting this issue:
          </label>
          <textarea
            className="w-full p-2 border rounded text-sm"
            rows={3}
            value={acceptanceNote}
            onChange={(e) => setAcceptanceNote(e.target.value)}
            placeholder="E.g., Employee has Section 125 cafeteria plan..."
          />
          <button
            onClick={() => {
              if (acceptanceNote.trim()) {
                onAccept(issue.issueId, acceptanceNote);
                setShowAcceptForm(false);
              }
            }}
            className="mt-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            disabled={!acceptanceNote.trim()}
          >
            Confirm Acceptance
          </button>
        </div>
      )}

      {issue.isAccepted && (
        <div className="mt-4 pt-4 border-t text-sm">
          <p className="font-semibold text-green-700">✓ Accepted</p>
          <p className="text-gray-600 italic">{issue.acceptanceNote}</p>
          <p className="text-xs text-gray-500 mt-1">
            Accepted on {new Date(issue.acceptedDate!).toLocaleString()}
          </p>
        </div>
      )}
    </div>
  );
};
```

---

## Step 8: Wire Up Frontend Service

**File**: `services/taxEngineService.ts`

```typescript
import { DiscrepancyReport } from '../types';

export const validateTaxReturn = async (
  taxReturnId: string,
  forms: any
): Promise<DiscrepancyReport> => {
  const response = await fetch('/api/tax-engine/validate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getAuthToken()}`,
    },
    body: JSON.stringify({
      taxReturnId,
      taxYear: 2024,
      forms,
    }),
  });

  if (!response.ok) {
    throw new Error('Validation failed');
  }

  return response.json();
};

export const acceptIssue = async (
  reportId: string,
  issueId: string,
  acceptanceNote: string
): Promise<void> => {
  const response = await fetch(
    `/api/tax-engine/validate/${reportId}/issues/${issueId}/accept`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${getAuthToken()}`,
      },
      body: JSON.stringify({ acceptanceNote }),
    }
  );

  if (!response.ok) {
    throw new Error('Failed to accept issue');
  }
};
```

---

## Step 9: End-to-End Test

### Test Case 1: W-2 Box Variance (HIGH Severity)

**Input**:
```json
{
  "taxReturnId": "test-123",
  "taxYear": 2024,
  "forms": {
    "w2Forms": [
      {
        "employerEIN": "12-3456789",
        "employeeSSN": "***-**-6789",
        "employerName": "Test Corp",
        "federalWages": 75000.00,
        "localWages": 7500.00,
        "localWithheld": 187.50
      }
    ]
  }
}
```

**Expected Output**:
```json
{
  "hasDiscrepancies": true,
  "issues": [
    {
      "ruleId": "FR-001",
      "category": "W-2",
      "severity": "HIGH",
      "message": "Box 18 is 90% lower than Box 1...",
      "blocksFiling": true
    }
  ],
  "summary": {
    "totalIssues": 1,
    "highSeverityCount": 1,
    "blocksFiling": true
  }
}
```

**cURL Test**:
```bash
curl -X POST http://localhost:8081/api/tax-engine/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "taxReturnId": "test-123",
    "taxYear": 2024,
    "forms": {
      "w2Forms": [{
        "employerEIN": "12-3456789",
        "employeeSSN": "***-**-6789",
        "employerName": "Test Corp",
        "federalWages": 75000.00,
        "localWages": 7500.00,
        "localWithheld": 187.50
      }]
    }
  }'
```

---

## Step 10: Verify Filing Submission Gate

**File**: `backend/submission-service/.../SubmissionController.java`

```java
@PostMapping("/submit")
public ResponseEntity<?> submitReturn(@RequestBody SubmissionRequest request) {
    // Check for blocking discrepancies
    DiscrepancyReport report = discrepancyReportService.getLatestReport(request.getTaxReturnId());
    
    if (report != null && report.getSummary().isBlocksFiling()) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .body(Map.of(
                    "error", "VALIDATION_FAILED",
                    "message", "Cannot submit return with unresolved HIGH severity issues",
                    "discrepancyReportId", report.getId()
                ));
    }
    
    // Proceed with submission...
}
```

**Test**:
```bash
# Try to submit with HIGH severity issues
curl -X POST http://localhost:8080/api/submissions/submit \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"taxReturnId": "test-123"}'

# Expected: 412 Precondition Failed
```

---

## Troubleshooting

### Issue: Validation returns empty issues array

**Cause**: Tax forms not being parsed from request  
**Fix**: Check `convertRequestToForms()` method properly maps JSON to `TaxFormData` objects

### Issue: Database constraint violation on acceptance

**Cause**: Trying to accept HIGH severity issue  
**Fix**: Frontend should disable "Accept" button for HIGH severity

### Issue: Validation takes >5 seconds

**Cause**: Inefficient prior year queries  
**Fix**: Add database index on `(ssn, tenant_id, tax_year)` in `tax_returns` table

---

## Next Steps

1. **Implement remaining validators**: Schedule, K1, Credit, Reconciliation, Carryforward
2. **Add unit tests**: Aim for ≥95% coverage on validator classes
3. **Integration tests**: Test API endpoints with full request/response cycle
4. **PDF report generation**: Extend `pdf-service` to create validation reports
5. **Admin configuration UI**: Allow tenant admins to adjust validation thresholds

---

## Resources

- **OpenAPI Spec**: `specs/3-enhanced-discrepancy-detection/contracts/validation-api.yaml`
- **Data Model**: `specs/3-enhanced-discrepancy-detection/data-model.md`
- **Research Document**: `specs/3-enhanced-discrepancy-detection/research.md`
- **IRS Publications**: [Publication 15](https://www.irs.gov/pub/irs-pdf/p15.pdf), [Form 8582 Instructions](https://www.irs.gov/forms-pubs/about-form-8582)

---

**Estimated Implementation Time**: 
- Backend (6 validators + models + API): 3-4 days
- Frontend (components + integration): 2-3 days
- Testing (unit + integration + E2E): 2-3 days
- **Total**: 7-10 days for complete feature
