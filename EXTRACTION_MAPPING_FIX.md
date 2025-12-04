# Extraction Mapping Fix Summary

## Problem
After PDF extraction using Gemini AI, forms appeared empty in the UI despite successful extraction. Console logs showed that extraction was returning W-2 fields using IRS box numbers (`box_1`, `box_2`, `box_a`, etc.) but the UI components expected semantic field names (`employer`, `federalWages`, `localWages`, etc.).

## Root Causes

### 1. Missing Helper Functions
The `extractionMapper.ts` was calling `parseAddress()` and `parseNumber()` helper functions that didn't exist, causing runtime errors during the mapping process.

### 2. Incomplete Type Definition
The `W2Form` interface in `types.ts` was missing several important fields:
- `employeeSSN`
- `employeeAddress`
- `federalWithheld`
- `socialSecurityWages`
- `socialSecurityTaxWithheld`
- `medicareTaxWithheld`
- `stateWages`
- `stateIncomeTax`
- `state`

### 3. W-2 Box Number Mapping
Gemini AI extraction returns W-2 data using IRS box numbers:
- `box_a` → SSN
- `box_b` → EIN
- `box_c` → Employer name/address
- `box_e` → Employee name
- `box_f` → Employee address
- `box_1` → Federal wages
- `box_2` → Federal withholding
- `box_3` → Social Security wages
- `box_4` → Social Security tax withheld
- `box_5` → Medicare wages
- `box_6` → Medicare tax withheld
- `box_15` → State
- `box_16` → State wages
- `box_17` → State income tax
- `box_18` → Local wages
- `box_19` → Local withholding
- `box_20` → Locality

But the UI components (particularly `W2Card` in `ReviewSection.tsx`) expect semantic field names like `employer`, `federalWages`, etc.

## Solutions Implemented

### 1. Added Helper Functions (`services/extractionMapper.ts`)

#### `parseAddress(addressString?: string)`
Parses W-2 address format (multi-line with city, state, zip on last line) into structured address object:
```typescript
{
  street: string;
  city: string;
  state: string;
  zip: string;
}
```

Handles format like:
```
HONDA DEV AND MFG OF AM LLC
24000 HONDA PKWY
MARYSVILLE OH 43040-8612
```

#### `parseNumber(value?: string | number)`
Converts W-2 numeric strings (e.g., `"245,695."`) to proper JavaScript numbers:
- Removes commas
- Removes trailing periods
- Handles string or number input
- Returns `null` for invalid values

### 2. Updated W2Form Type (`types.ts`)
Extended the `W2Form` interface to include all W-2 fields, making them optional where appropriate:
```typescript
export interface W2Form extends BaseTaxForm {
  formType: TaxFormType.W2;
  employer: string;
  employerEin: string;
  employerAddress: Address;
  employerCounty?: string;
  totalMonthsInCity?: number;
  employee: string;
  employeeSSN?: string;           // Added
  employeeAddress?: Address;      // Added
  employeeInfo?: TaxPayerProfile;
  federalWages: number;
  federalWithheld?: number;       // Added
  socialSecurityWages?: number;   // Added
  socialSecurityTaxWithheld?: number; // Added
  medicareWages: number;
  medicareTaxWithheld?: number;   // Added
  stateWages?: number;            // Added
  stateIncomeTax?: number;        // Added
  state?: string;                 // Added
  localWages: number;
  localWithheld: number;
  locality: string;
  taxDue?: number;
  lowConfidenceFields?: string[];
}
```

### 3. Box Number to Semantic Field Mapping
The W-2 mapping in `extractionMapper.ts` now:

1. **Preserves original extracted data**: Uses spread operator to keep box numbers
   ```typescript
   const w2Form = {
     ...f, // Keeps box_1, box_2, etc. for provenance tracking
     ...base, // Adds id, fileName, etc.
     // ... semantic mappings
   }
   ```

2. **Maps to semantic names**: Translates box numbers to UI-expected field names
   ```typescript
   employer: f.box_c?.split('\n')[0] || f.employer || '',
   employerEin: f.box_b || f.employerEin || '',
   employerAddress: parseAddress(f.box_c) || ...,
   employee: f.box_e || f.employee || '',
   employeeSSN: f.box_a || f.employeeSSN || '',
   federalWages: parseNumber(f.box_1) ?? f.federalWages ?? 0,
   federalWithheld: parseNumber(f.box_2) ?? f.federalWithheld ?? 0,
   // ... etc for all fields
   ```

3. **Handles both formats**: Falls back to semantic names if box numbers aren't present (for manually entered forms or different extraction formats)

## Data Flow After Fix

1. **Upload**: User uploads PDF → converted to base64
2. **Extraction**: Gemini AI extracts data using box numbers
3. **Mapping**: `extractionMapper.ts` translates box numbers to semantic names
4. **Storage**: TaxFilingApp state holds transformed data with semantic field names
5. **Display**: ReviewSection/W2Card reads semantic field names and displays correctly

## Testing
To verify the fix:

1. Upload a W-2 PDF
2. Check console for `[ExtractionMapper] Mapped W2:` log showing both box numbers and semantic names
3. Verify W2Card displays:
   - Employer name (from box_c)
   - EIN (from box_b)
   - Federal wages (from box_1)
   - Medicare wages (from box_5)
   - Local wages (from box_18)
   - Local withholding (from box_19)
   - Locality (from box_20)

## Benefits

1. **Backward Compatible**: Still accepts manually created forms with semantic field names
2. **Traceable**: Preserves original box numbers for audit trail
3. **Flexible**: Can handle variations in extraction format
4. **Type Safe**: TypeScript catches missing or incorrect field mappings
5. **Maintainable**: Clear mapping logic in one place

## Next Steps

If similar issues occur with other form types (1099, W-2G, etc.), apply the same pattern:
1. Add helper functions for format-specific parsing
2. Update type definitions
3. Add box number to semantic name mapping
4. Preserve both formats in the mapped object
