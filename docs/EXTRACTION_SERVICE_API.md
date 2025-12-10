# Gemini AI Extraction Service API Documentation

> **Version:** 2.0.0  
> **Model:** Gemini 2.0 Flash (`gemini-2.0-flash`)  
> **Base URL:** `/api/v1/extraction`

## Overview

The Extraction Service provides AI-powered document extraction for U.S. Federal, State, and Local municipality tax forms. It uses Google's Gemini 2.0 Flash model to extract structured data from uploaded tax documents with high precision and confidence scoring.

### Supported Gemini Models

| Model Name | Description | Recommended For |
|------------|-------------|-----------------|
| `gemini-2.0-flash` | Stable document processing, 1M token context | **Default - Production use** |
| `gemini-1.5-flash-latest` | Latest 1.5 Flash version | Fallback option |
| `gemini-2.5-flash` | Newer version (may have intermittent issues) | Testing new features |
| `gemini-2.5-pro` | Higher reasoning capability | Complex documents |

> **Note:** Model availability and behavior may change. See [Google's official documentation](https://ai.google.dev/gemini-api/docs/models) for the latest model list.

## Features

- **Universal Tax Form Extraction**: Supports both individual and business tax forms
- **User-Provided API Keys**: Users can provide their own Gemini API key for extraction (never persisted)
- **Real-Time Progress Updates**: Server-sent events (SSE) for live extraction feedback
- **Confidence Scoring**: Field-level and form-level confidence with weighted scoring
- **Document Provenance**: Tracks which page and location each field was extracted from
- **Batch Processing**: Support for multi-file uploads

## Supported Form Types

### Individual Forms
| Form Type | Description | Key Fields |
|-----------|-------------|------------|
| `W-2` | Wage and Tax Statement | employer, federalWages, medicareWages, localWages, localWithheld |
| `1099-NEC` | Nonemployee Compensation | payer, incomeAmount |
| `1099-MISC` | Miscellaneous Income | payer, incomeAmount |
| `W-2G` | Gambling Winnings | grossWinnings, dateWon, typeOfWager |
| `Schedule C` | Profit or Loss From Business | businessName, grossReceipts, totalExpenses, netProfit |
| `Schedule E` | Supplemental Income and Loss | rentals[], partnerships[] |
| `Schedule F` | Profit or Loss From Farming | netFarmProfit |
| `Federal 1040` | Individual Income Tax Return | wages, totalIncome, adjustedGrossIncome, tax |
| `Dublin 1040` | Local Municipality Tax Return | qualifyingWages, taxDue, credits |
| `Form R` | Local Tax Return | qualifyingWages, otherIncome, taxDue |

### Business Forms
| Form Type | Description | Key Fields |
|-----------|-------------|------------|
| `Federal 1120` | Corporation Income Tax Return | fedTaxableIncome, reconciliation |
| `Federal 1065` | Partnership Income Return | fedTaxableIncome, allocation |
| `Form 27` | Net Profits Tax Return | fedTaxableIncome, reconciliation, allocation |
| `Form W-1` | Quarterly Withholding Return | grossWages, taxDue |
| `Form W-3` | Annual Reconciliation | totalW1Tax, totalW2Tax |

---

## Endpoints

### 1. Extract Data from Document

**POST** `/api/v1/extraction/extract`

Upload a tax document for AI-powered extraction.

#### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | Bearer token for authentication |
| `X-Gemini-Api-Key` | No | User's Gemini API key (optional, uses server default if not provided) |
| `X-Gemini-Model` | No | Model override (default: `gemini-2.0-flash`) |
| `Content-Type` | Yes | `multipart/form-data` |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | Tax document (PDF, JPG, PNG) |
| `taxYear` | String | No | Tax year context |

#### Response

Returns a stream of Server-Sent Events (SSE) with extraction progress updates.

**Content-Type:** `text/event-stream`

#### SSE Event Format

```json
data: {
  "status": "SCANNING" | "ANALYZING" | "EXTRACTING" | "COMPLETE" | "ERROR",
  "progress": 0-100,
  "log": ["Message 1", "Message 2"],
  "detectedForms": ["W-2", "Federal 1040"],
  "confidence": 0.0-1.0,
  "currentFormType": "W-2",
  "currentTaxpayerName": "John Q. Taxpayer",
  "fieldConfidences": {
    "W-2.federalWages": {
      "fieldName": "federalWages",
      "confidence": 0.95,
      "weight": "CRITICAL",
      "weightedScore": 1.425,
      "extractionSource": "AI_EXTRACTED"
    }
  },
  "formProvenances": [
    {
      "formType": "W-2",
      "pageNumber": 1,
      "extractionReason": "Standard W-2 form detected",
      "formConfidence": 0.95,
      "fields": [
        {
          "fieldName": "federalWages",
          "pageNumber": 1,
          "confidence": 0.95
        }
      ]
    }
  ],
  "summary": {
    "totalPagesScanned": 5,
    "formsExtracted": 3,
    "formsSkipped": 1,
    "extractedFormTypes": ["W-2", "Federal 1040", "Schedule C"],
    "skippedForms": [
      {
        "formType": "Instruction Page",
        "pageNumber": 2,
        "reason": "Non-data page detected",
        "suggestion": "No action required"
      }
    ],
    "overallConfidence": 0.92,
    "confidenceByFormType": {
      "W-2": 0.95,
      "Federal 1040": 0.89,
      "Schedule C": 0.92
    },
    "extractionDurationMs": 3500,
    "modelUsed": "gemini-2.0-flash"
  },
  "result": {
    // Final extracted data (only on COMPLETE status)
  }
}
```

#### Example Request (cURL)

```bash
curl -X POST "http://localhost:8084/api/v1/extraction/extract" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Gemini-Api-Key: YOUR_GEMINI_API_KEY" \
  -F "file=@/path/to/tax_forms.pdf"
```

#### Example Request (JavaScript)

```javascript
const formData = new FormData();
formData.append('file', file);

const response = await fetch('/api/v1/extraction/extract', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'X-Gemini-Api-Key': userApiKey // Optional
  },
  body: formData
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  
  const chunk = decoder.decode(value);
  // Parse SSE events
  const lines = chunk.split('\n\n');
  for (const line of lines) {
    if (line.startsWith('data:')) {
      const data = JSON.parse(line.substring(5));
      console.log('Extraction update:', data);
    }
  }
}
```

---

### 2. Batch Extract Documents

**POST** `/api/v1/extraction/extract/batch`

Upload multiple tax documents for sequential extraction.

#### Request Headers

Same as single document extraction.

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `files` | File[] | Yes | Array of tax documents |

#### Response

Returns a stream of SSE events for each file in sequence.

---

### 3. Test Stream (Mock Data)

**GET** `/api/v1/extraction/stream?fileName=test.pdf`

Test endpoint that returns mock extraction data without using API credits.

#### Query Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `fileName` | Yes | Simulated file name |

#### Response

Returns mock SSE events for UI development and testing.

---

## Confidence Scoring

### Field Weight Classifications

Fields are classified by importance for tax calculations:

| Weight | Multiplier | Examples |
|--------|------------|----------|
| `CRITICAL` | 1.5x | federalWages, localWages, taxDue, totalIncome |
| `HIGH` | 1.25x | employerEin, filingStatus, businessName |
| `MEDIUM` | 1.0x | locality, address, totalExpenses |
| `LOW` | 0.75x | extractionReason, pageNumber, dateWon |

### Confidence Score Ranges

| Range | Interpretation | Action |
|-------|----------------|--------|
| 0.95-1.0 | High confidence | Auto-verified |
| 0.85-0.94 | Good confidence | Standard review |
| 0.70-0.84 | Moderate confidence | Manual verification recommended |
| < 0.70 | Low confidence | Requires manual entry |

---

## Extraction Result Schema

### Complete Result Object

```typescript
interface ExtractionResult {
  scanMetadata: {
    pageCount: number;
    scanQuality: 'HIGH' | 'MEDIUM' | 'LOW';
    processingNotes?: string[];
  };
  
  taxPayerProfile: {
    name: string;
    ssn: string; // Masked as ***-**-XXXX
    filingStatus: FilingStatus;
    spouse?: { name: string; ssn: string };
    address: Address;
  };
  
  returnSettings: {
    taxYear: number;
    isAmendment: boolean;
    amendmentReason?: string;
  };
  
  forms: ExtractedForm[];
  
  skippedPages: Array<{
    pageNumber: number;
    reason: string;
  }>;
}
```

### ExtractedForm Object

```typescript
interface ExtractedForm {
  formType: string;
  confidenceScore: number;
  pageNumber: number;
  extractionReason: string;
  owner: 'PRIMARY' | 'SPOUSE';
  fieldConfidence: Record<string, number>;
  
  // Form-specific fields vary by formType
  // W-2: employer, employerEin, federalWages, etc.
  // 1099: payer, incomeAmount, etc.
  // Schedule C: businessName, grossReceipts, netProfit, etc.
}
```

---

## Error Handling

### Error Response Format

The extraction service properly parses Gemini API error responses and returns meaningful error messages. When an invalid API key is used, users will see the actual error message from Google's API (e.g., "API key not valid. Please pass a valid API key.") rather than generic HTTP error codes.

```json
{
  "status": "ERROR",
  "progress": 0,
  "log": ["Extraction failed: API key not valid. Please pass a valid API key."],
  "detectedForms": [],
  "confidence": 0,
  "summary": {
    "totalPagesScanned": 0,
    "formsExtracted": 0,
    "formsSkipped": 0,
    "extractedFormTypes": [],
    "skippedForms": [
      {
        "formType": "Unknown",
        "pageNumber": 0,
        "reason": "API Error: API key not valid. Please pass a valid API key.",
        "suggestion": "Please check your API key and try again"
      }
    ],
    "overallConfidence": 0,
    "confidenceByFormType": {},
    "extractionDurationMs": 500,
    "modelUsed": "error"
  }
}
```

### Gemini API Error Messages

The service extracts and returns the actual error message from Google's Gemini API:

| Error Message | Cause | Resolution |
|--------------|-------|------------|
| `API key not valid. Please pass a valid API key.` | Invalid or malformed API key | Verify API key in Google AI Studio |
| `Request lacks valid authentication credentials.` | Missing API key | Provide API key via header or config |
| `Resource has been exhausted` | API quota exceeded | Wait or increase quota |
| `Model not found` | Invalid model name | Use supported model (e.g., `gemini-2.0-flash`) |

### HTTP Status Codes

| Error | Description | Resolution |
|-------|-------------|------------|
| `401 Unauthorized` | Missing or invalid JWT token | Provide valid authentication |
| `400 Bad Request` | Invalid file format or Gemini API error | Check file and API key |
| `413 Payload Too Large` | File exceeds 50MB limit | Reduce file size |
| `429 Too Many Requests` | API rate limit exceeded | Wait and retry |
| `500 Internal Server Error` | Extraction processing error | Check logs and retry |

---

## Security Considerations

### API Key Handling

- User-provided API keys are transmitted via secure headers
- Keys are **never** persisted to storage or logs
- Keys are used only for the duration of the request
- After request completion, keys are discarded from memory
- Server logs mask API keys (showing only first/last 5 characters)

### Best Practices

1. **Never** store API keys in client-side storage
2. Use HTTPS for all API communications
3. Implement request timeouts (recommended: 5 minutes)
4. Handle SSE connection drops gracefully
5. Validate extracted data before processing

---

## Rate Limits

| Tier | Requests/Minute | Files/Request |
|------|----------------|---------------|
| Free | 10 | 5 |
| Standard | 60 | 20 |
| Enterprise | Unlimited | 100 |

---

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `GEMINI_API_KEY` | (none) | Default Gemini API key |
| `GEMINI_API_MODEL` | `gemini-2.0-flash` | Default model |

### Application Properties

```yaml
gemini:
  api:
    key: ${GEMINI_API_KEY:}
    model: ${GEMINI_API_MODEL:gemini-2.0-flash}

spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB
```

---

## Changelog

### v2.0.0 (2024)
- Upgraded to Gemini 2.5 Flash model
- Added user-provided API key support
- Enhanced confidence scoring with field weights
- Added document provenance tracking
- Improved extraction prompts for all form types
- Added batch upload support
- Real-time extraction feedback with form detection

### v1.0.0 (2023)
- Initial release with Gemini 1.5 Flash
- Basic extraction for W-2, 1099, and Schedule forms
