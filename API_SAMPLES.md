# W-1 Withholding Filing API - Sample Requests and Responses

This document provides sample API requests and responses for the W-1 withholding reconciliation system.

## Base URL
```
http://localhost:8085/api/v1
```

## Authentication
All endpoints require JWT authentication. Include the token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

---

## 1. File Quarterly W-1 Return

### Endpoint
```
POST /w1-filings
```

### Request Body
```json
{
  "businessId": "550e8400-e29b-41d4-a716-446655440000",
  "taxYear": 2024,
  "filingFrequency": "QUARTERLY",
  "period": "Q1",
  "periodStartDate": "2024-01-01",
  "periodEndDate": "2024-03-31",
  "grossWages": 125000.00,
  "taxableWages": 125000.00,
  "adjustments": 0.00,
  "employeeCount": 15
}
```

### Success Response (201 Created)
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440000",
  "businessId": "550e8400-e29b-41d4-a716-446655440000",
  "taxYear": 2024,
  "filingFrequency": "QUARTERLY",
  "period": "Q1",
  "periodStartDate": "2024-01-01",
  "periodEndDate": "2024-03-31",
  "dueDate": "2024-04-30",
  "filingDate": "2024-04-25T14:30:00Z",
  "grossWages": 125000.00,
  "taxableWages": 125000.00,
  "taxRate": 0.0200,
  "taxDue": 2500.00,
  "adjustments": 0.00,
  "lateFilingPenalty": 0.00,
  "underpaymentPenalty": 0.00,
  "totalAmountDue": 2500.00,
  "isAmended": false,
  "amendsFilingId": null,
  "amendmentReason": null,
  "employeeCount": 15,
  "status": "FILED",
  "createdAt": "2024-04-25T14:30:00Z",
  "createdBy": "750e8400-e29b-41d4-a716-446655440000",
  "updatedAt": "2024-04-25T14:30:00Z",
  "cumulativeTotals": null
}
```

### Error Response (400 Bad Request) - Validation Error
```json
{
  "timestamp": "2024-04-25T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/w1-filings",
  "errors": [
    {
      "field": "grossWages",
      "message": "Gross wages is required"
    },
    {
      "field": "periodEndDate",
      "message": "Period end date must be on or after period start date"
    }
  ]
}
```

### Error Response (400 Bad Request) - Duplicate Filing
```json
{
  "timestamp": "2024-04-25T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "W-1 filing already exists for business 550e8400-e29b-41d4-a716-446655440000 tax year 2024 period Q1. Use amendment endpoint to modify.",
  "path": "/api/v1/w1-filings"
}
```

---

## 2. File Monthly W-1 Return

### Request Body
```json
{
  "businessId": "550e8400-e29b-41d4-a716-446655440000",
  "taxYear": 2024,
  "filingFrequency": "MONTHLY",
  "period": "M01",
  "periodStartDate": "2024-01-01",
  "periodEndDate": "2024-01-31",
  "grossWages": 42000.00,
  "employeeCount": 15
}
```

### Success Response (201 Created)
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "businessId": "550e8400-e29b-41d4-a716-446655440000",
  "taxYear": 2024,
  "filingFrequency": "MONTHLY",
  "period": "M01",
  "periodStartDate": "2024-01-01",
  "periodEndDate": "2024-01-31",
  "dueDate": "2024-02-15",
  "filingDate": "2024-02-10T09:15:00Z",
  "grossWages": 42000.00,
  "taxableWages": 42000.00,
  "taxRate": 0.0200,
  "taxDue": 840.00,
  "adjustments": 0.00,
  "lateFilingPenalty": 0.00,
  "underpaymentPenalty": 0.00,
  "totalAmountDue": 840.00,
  "isAmended": false,
  "status": "FILED",
  "employeeCount": 15
}
```

---

## 3. File Daily W-1 Return

### Request Body
```json
{
  "businessId": "550e8400-e29b-41d4-a716-446655440000",
  "taxYear": 2024,
  "filingFrequency": "DAILY",
  "period": "D20240115",
  "periodStartDate": "2024-01-15",
  "periodEndDate": "2024-01-15",
  "grossWages": 1200.00,
  "employeeCount": 8
}
```

### Success Response (201 Created)
```json
{
  "id": "670e8400-e29b-41d4-a716-446655440002",
  "businessId": "550e8400-e29b-41d4-a716-446655440000",
  "taxYear": 2024,
  "filingFrequency": "DAILY",
  "period": "D20240115",
  "periodStartDate": "2024-01-15",
  "periodEndDate": "2024-01-15",
  "dueDate": "2024-01-16",
  "filingDate": "2024-01-16T08:00:00Z",
  "grossWages": 1200.00,
  "taxableWages": 1200.00,
  "taxRate": 0.0200,
  "taxDue": 24.00,
  "adjustments": 0.00,
  "lateFilingPenalty": 0.00,
  "underpaymentPenalty": 0.00,
  "totalAmountDue": 24.00,
  "isAmended": false,
  "status": "FILED",
  "employeeCount": 8
}
```

---

## 4. File W-1 with Late Filing Penalty

### Request Body
```json
{
  "businessId": "550e8400-e29b-41d4-a716-446655440000",
  "taxYear": 2024,
  "filingFrequency": "QUARTERLY",
  "period": "Q2",
  "periodStartDate": "2024-04-01",
  "periodEndDate": "2024-06-30",
  "grossWages": 135000.00,
  "employeeCount": 16
}
```

### Success Response (201 Created) - Filed 1 Month Late
```json
{
  "id": "680e8400-e29b-41d4-a716-446655440003",
  "businessId": "550e8400-e29b-41d4-a716-446655440000",
  "taxYear": 2024,
  "filingFrequency": "QUARTERLY",
  "period": "Q2",
  "periodStartDate": "2024-04-01",
  "periodEndDate": "2024-06-30",
  "dueDate": "2024-07-30",
  "filingDate": "2024-08-28T16:45:00Z",
  "grossWages": 135000.00,
  "taxableWages": 135000.00,
  "taxRate": 0.0200,
  "taxDue": 2700.00,
  "adjustments": 0.00,
  "lateFilingPenalty": 135.00,
  "underpaymentPenalty": 0.00,
  "totalAmountDue": 2835.00,
  "isAmended": false,
  "status": "FILED",
  "employeeCount": 16
}
```

**Penalty Calculation:**
- Tax due: $2,700.00
- Days late: 29 days (July 30 → August 28)
- Months late: 1 month (partial month rounds up)
- Penalty rate: 5% per month
- Late penalty: $2,700.00 × 5% = $135.00

---

## 5. File W-1 with Adjustments

### Request Body
```json
{
  "businessId": "550e8400-e29b-41d4-a716-446655440000",
  "taxYear": 2024,
  "filingFrequency": "QUARTERLY",
  "period": "Q3",
  "periodStartDate": "2024-07-01",
  "periodEndDate": "2024-09-30",
  "grossWages": 140000.00,
  "adjustments": -200.00,
  "employeeCount": 17
}
```

### Success Response (201 Created)
```json
{
  "id": "690e8400-e29b-41d4-a716-446655440004",
  "businessId": "550e8400-e29b-41d4-a716-446655440000",
  "taxYear": 2024,
  "filingFrequency": "QUARTERLY",
  "period": "Q3",
  "periodStartDate": "2024-07-01",
  "periodEndDate": "2024-09-30",
  "dueDate": "2024-10-30",
  "filingDate": "2024-10-15T11:20:00Z",
  "grossWages": 140000.00,
  "taxableWages": 140000.00,
  "taxRate": 0.0200,
  "taxDue": 2800.00,
  "adjustments": -200.00,
  "lateFilingPenalty": 0.00,
  "underpaymentPenalty": 0.00,
  "totalAmountDue": 2600.00,
  "isAmended": false,
  "status": "FILED",
  "employeeCount": 17
}
```

**Note:** Negative adjustment of $200.00 (prior period credit) reduces total amount due from $2,800.00 to $2,600.00.

---

## 6. Validation Examples

### Invalid Daily Period Date
```json
{
  "period": "D20240230"
}
```

**Error Response:**
```json
{
  "errors": [
    {
      "field": "periodDateValid",
      "message": "For daily periods, the date portion (YYYYMMDD) must be a valid date"
    }
  ]
}
```

### Invalid Period Format
```json
{
  "period": "Q5"
}
```

**Error Response:**
```json
{
  "errors": [
    {
      "field": "period",
      "message": "Period format invalid. Expected: Q1-Q4, M01-M12, SM01-SM24, or DYYYYMMDD"
    }
  ]
}
```

### Invalid Date Range
```json
{
  "periodStartDate": "2024-03-31",
  "periodEndDate": "2024-01-01"
}
```

**Error Response:**
```json
{
  "errors": [
    {
      "field": "periodDateRangeValid",
      "message": "Period end date must be on or after period start date"
    }
  ]
}
```

---

## Tax Calculation Formula

```
taxDue = taxableWages × municipalTaxRate
```

**Default Rate:** 2.0% (configurable via `tax.municipal.rate` property)

**Example:**
- Gross wages: $125,000.00
- Taxable wages: $125,000.00 (defaults to gross wages if not provided)
- Municipal tax rate: 2.0%
- **Tax due: $125,000.00 × 0.02 = $2,500.00**

---

## Late Filing Penalty Calculation

**Rules:**
- 5% of tax due per month late (partial month rounds up using calendar months)
- Maximum penalty: 25% of tax due (5 months)
- Minimum penalty: $50.00 if tax due > $200.00
- No penalty if tax due = $0.00 (seasonal businesses)

**Example 1: Filed 1 Month Late**
- Tax due: $2,700.00
- Days late: 29 days
- Months late: 1 month
- Penalty: $2,700.00 × 5% = $135.00

**Example 2: Filed 3 Months Late**
- Tax due: $1,500.00
- Months late: 3 months
- Penalty: $1,500.00 × 15% = $225.00

**Example 3: Minimum Penalty Applied**
- Tax due: $300.00
- Months late: 1 month
- Calculated penalty: $300.00 × 5% = $15.00
- **Actual penalty: $50.00** (minimum applied)

---

## Due Date Calculation

### Quarterly Filing
- Due date: **30 days after quarter end**
- Q1 (Jan-Mar): Due April 30
- Q2 (Apr-Jun): Due July 30
- Q3 (Jul-Sep): Due October 30
- Q4 (Oct-Dec): Due January 30 (next year)

### Monthly Filing
- Due date: **15th of following month**
- January filing: Due February 15
- February filing: Due March 15
- If 15th falls on weekend, due date moves to next Monday

### Daily Filing
- Due date: **Next business day**
- Monday filing: Due Tuesday
- Friday filing: Due Monday (skips weekend)

### Semi-Monthly Filing
- Due date: **15th of following month**
- Same as monthly filing

---

## Next Steps

After implementing the REST controllers, these endpoints will be available at:
- `POST /api/v1/w1-filings` - File new W-1 return
- `GET /api/v1/w1-filings` - List W-1 filings (with pagination)
- `GET /api/v1/w1-filings/{id}` - Get filing details
- `POST /api/v1/w1-filings/{id}/amend` - File amended W-1
- `GET /api/v1/w1-filings/{id}/penalties` - Calculate penalties
- `GET /api/v1/cumulative-totals` - Query YTD cumulative totals

---

# Auditor Workflow API - Sample Requests and Responses

This section documents the auditor workflow endpoints for reviewing, approving, and rejecting tax returns.

## Base URL
```
http://localhost:8085/api/v1/audit
```

---

## 1. Get Audit Queue

Retrieve the submission queue with filtering, sorting, and pagination.

### Endpoint
```
GET /queue?status=PENDING&priority=HIGH&page=0&size=20&sortBy=submissionDate&sortDirection=DESC
```

### Query Parameters
- `status` (optional): Filter by status (PENDING, IN_REVIEW, AWAITING_DOCUMENTATION, APPROVED, REJECTED)
- `priority` (optional): Filter by priority (LOW, MEDIUM, HIGH)
- `auditorId` (optional): Filter by assigned auditor
- `tenantId` (optional): Filter by tenant
- `fromDate` (optional): Filter by submission date (epoch milliseconds)
- `toDate` (optional): Filter by submission date (epoch milliseconds)
- `page` (default: 0): Page number
- `size` (default: 20): Items per page
- `sortBy` (default: submissionDate): Sort field
- `sortDirection` (default: DESC): Sort direction (ASC/DESC)

### Success Response (200 OK)
```json
{
  "content": [
    {
      "queueId": "q-123",
      "returnId": "ret-456",
      "priority": "HIGH",
      "status": "PENDING",
      "submissionDate": "2024-11-15T10:30:00Z",
      "assignedAuditorId": null,
      "riskScore": 75,
      "flaggedIssuesCount": 3,
      "daysInQueue": 5,
      "taxpayerName": "ABC Corp",
      "returnType": "BUSINESS",
      "taxYear": "2024",
      "taxDue": 125000.00
    }
  ],
  "totalElements": 125,
  "totalPages": 7,
  "size": 20,
  "number": 0
}
```

---

## 2. Get Queue Statistics

Get summary statistics for the audit queue.

### Endpoint
```
GET /queue/stats
```

### Success Response (200 OK)
```json
{
  "pending": 45,
  "highPriority": 12
}
```

---

## 3. Assign Auditor

Assign a return to an auditor.

### Endpoint
```
POST /assign
```

### Request Body
```json
{
  "queueId": "q-123",
  "auditorId": "aud-789",
  "assignedBy": "supervisor-001"
}
```

### Success Response (200 OK)
```json
{
  "queueId": "q-123",
  "returnId": "ret-456",
  "priority": "HIGH",
  "status": "IN_REVIEW",
  "assignedAuditorId": "aud-789",
  "assignmentDate": "2024-11-20T14:00:00Z",
  "reviewStartedDate": "2024-11-20T14:00:00Z"
}
```

---

## 4. Approve Return

Approve a tax return with e-signature.

### Endpoint
```
POST /approve
```

### Request Body
```json
{
  "returnId": "ret-456",
  "auditorId": "aud-789",
  "eSignature": "base64EncodedSignature=="
}
```

### Success Response (200 OK)
```json
{
  "status": "success",
  "message": "Return approved successfully"
}
```

---

## 5. Reject Return

Reject a tax return with detailed explanation.

### Endpoint
```
POST /reject
```

### Request Body
```json
{
  "returnId": "ret-456",
  "auditorId": "aud-789",
  "reason": "MISSING_SCHEDULES",
  "detailedExplanation": "Schedule X (book-tax reconciliation) is missing. The reported federal taxable income does not match the amounts on the federal return. Please complete Schedule X showing all book-tax adjustments and resubmit.",
  "resubmitDeadline": "2024-12-15"
}
```

### Success Response (200 OK)
```json
{
  "status": "success",
  "message": "Return rejected successfully"
}
```

---

## 6. Request Additional Documentation

Request additional supporting documents from taxpayer.

### Endpoint
```
POST /request-docs
```

### Request Body
```json
{
  "returnId": "ret-456",
  "auditorId": "aud-789",
  "documentType": "DEPRECIATION_SCHEDULE",
  "description": "Please provide detailed depreciation schedules for all assets over $50,000 including purchase dates, cost basis, and depreciation method.",
  "deadline": "2024-12-01",
  "tenantId": "tenant-001"
}
```

### Success Response (200 OK)
```json
{
  "requestId": "doc-req-001",
  "returnId": "ret-456",
  "auditorId": "aud-789",
  "requestDate": "2024-11-20T15:00:00Z",
  "documentType": "DEPRECIATION_SCHEDULE",
  "description": "Please provide detailed depreciation schedules...",
  "deadline": "2024-12-01",
  "status": "PENDING",
  "uploadedFiles": []
}
```

---

## 7. Get Audit Trail

Retrieve complete audit trail for a return.

### Endpoint
```
GET /trail/{returnId}
```

### Success Response (200 OK)
```json
[
  {
    "trailId": "trail-001",
    "returnId": "ret-456",
    "eventType": "SUBMISSION",
    "userId": "user-123",
    "timestamp": "2024-11-15T10:30:00Z",
    "eventDetails": "Return submitted and added to audit queue"
  },
  {
    "trailId": "trail-002",
    "returnId": "ret-456",
    "eventType": "ASSIGNMENT",
    "userId": "supervisor-001",
    "timestamp": "2024-11-20T14:00:00Z",
    "eventDetails": "Return assigned to auditor aud-789"
  },
  {
    "trailId": "trail-003",
    "returnId": "ret-456",
    "eventType": "APPROVAL",
    "userId": "aud-789",
    "timestamp": "2024-11-22T16:30:00Z",
    "eventDetails": "Return approved by auditor",
    "digitalSignature": "sha256hash..."
  }
]
```

---

## 8. Get Audit Report

Retrieve automated audit report with risk assessment.

### Endpoint
```
GET /report/{returnId}
```

### Success Response (200 OK)
```json
{
  "reportId": "rpt-001",
  "returnId": "ret-456",
  "generatedDate": "2024-11-15T10:35:00Z",
  "riskScore": 75,
  "riskLevel": "HIGH",
  "flaggedItems": [
    "Income decreased 45% year-over-year",
    "Deductions 150% higher than industry average",
    "W-2 Box 18 (local wages) does not match Box 1 (federal wages)"
  ],
  "recommendedActions": [
    "Request general ledger",
    "Verify depreciation schedules",
    "Review year-over-year income variance"
  ],
  "auditorOverride": false
}
```

---

## 9. Get Auditor Workload

Get all returns assigned to a specific auditor.

### Endpoint
```
GET /workload/{auditorId}
```

### Success Response (200 OK)
```json
[
  {
    "queueId": "q-123",
    "returnId": "ret-456",
    "priority": "HIGH",
    "status": "IN_REVIEW",
    "assignedAuditorId": "aud-789",
    "daysInQueue": 5,
    "taxpayerName": "ABC Corp"
  },
  {
    "queueId": "q-124",
    "returnId": "ret-457",
    "priority": "MEDIUM",
    "status": "IN_REVIEW",
    "assignedAuditorId": "aud-789",
    "daysInQueue": 2,
    "taxpayerName": "XYZ Inc"
  }
]
```

---

## 10. Update Priority

Change the priority of a queue item.

### Endpoint
```
POST /priority
```

### Request Body
```json
{
  "queueId": "q-123",
  "priority": "HIGH",
  "userId": "supervisor-001"
}
```

### Success Response (200 OK)
```json
{
  "queueId": "q-123",
  "returnId": "ret-456",
  "priority": "HIGH",
  "status": "PENDING"
}
```

---

## Error Responses

### 404 Not Found
```json
{
  "error": "NOT_FOUND",
  "message": "Queue entry not found"
}
```

### 403 Forbidden
```json
{
  "error": "FORBIDDEN",
  "message": "User does not have AUDITOR role"
}
```

### 400 Bad Request
```json
{
  "error": "BAD_REQUEST",
  "message": "E-signature is required for approval"
}
```

---

## Workflow Examples

### Example 1: Approve a Return

1. Get queue items: `GET /queue?status=PENDING`
2. Assign to yourself: `POST /assign` with your auditorId
3. Review return details: `GET /queue/{returnId}`
4. Check audit report: `GET /report/{returnId}`
5. Approve: `POST /approve` with e-signature

### Example 2: Reject a Return

1. Get your workload: `GET /workload/{auditorId}`
2. Review return: `GET /queue/{returnId}`
3. Review audit trail: `GET /trail/{returnId}`
4. Reject with details: `POST /reject` with reason and explanation

### Example 3: Request Additional Documents

1. Start reviewing: `POST /start-review`
2. Identify missing documents
3. Request documents: `POST /request-docs` with details
4. System updates status to AWAITING_DOCUMENTATION
5. Taxpayer uploads docs
6. Mark received: `POST /document-requests/{requestId}/received`
7. Continue review

---

## Role-Based Access

Different user roles have different permissions:

| Role | Permissions |
|------|------------|
| AUDITOR | Review returns, request docs, recommend approval/rejection |
| SENIOR_AUDITOR | All AUDITOR permissions + approve/reject returns <$50K |
| SUPERVISOR | All permissions + approve/reject any return + reassign + override priority |
| MANAGER | All permissions + generate compliance reports + configure audit rules |
| ADMIN | System configuration + user management |

