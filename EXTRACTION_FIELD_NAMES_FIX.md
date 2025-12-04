# Extraction Field Names Fix

## Problem Identified

**Root Cause**: The AI extraction service was not providing consistent field names for non-W-2 forms.

### Analysis
- **W-2 forms worked** because the prompt explicitly specified JSON field names: 
  - `Box 1: Wages → federalWages`
  - `Box 2: Federal tax withheld → federalWithheld`
  
- **Other forms failed** because the prompt only described what to extract without specifying field names:
  - `Box 1: Nonemployee compensation` (no field name!)
  - `Line 31: Net profit or loss` (no field name!)

This caused the AI to use inconsistent or generic field names that didn't match the schema definitions.

## Solution Applied

Updated the extraction prompt in `RealGeminiService.java` to include explicit JSON field names for ALL form types:

### 1099-NEC/MISC
```
• Box 1: Nonemployee compensation → nonemployeeCompensation
• Box 3: Other income → otherIncome
• Box 1: Rents → rents
• Box 2: Royalties → royalties
• Box 4: Federal income tax withheld → federalWithheld
• Payer name → payerName
• Payer TIN → payerTin
• Recipient name → recipientName
• State tax withheld → stateTaxWithheld
```

### W-2G (Gambling Winnings)
```
• Box 1: Gross winnings → grossWinnings
• Box 2: Date won → dateWon
• Box 3: Type of wager → typeOfWager
• Box 4: Federal income tax withheld → federalWithheld
• Box 14: State winnings → stateWinnings
• Box 15: State income tax withheld → stateIncomeTax
• Box 16: Local winnings → localWinnings
• Payer name → payerName
• Winner name → winnerName
```

### Schedule C (Business Profit/Loss)
```
• Business name → businessName
• Principal business or profession → principalBusiness
• Line 1: Gross receipts or sales → grossReceipts
• Line 2: Returns and allowances → returnsAndAllowances
• Line 4: Cost of goods sold → costOfGoodsSold
• Line 5: Gross profit → grossProfit
• Line 8: Advertising → advertising
• Line 9: Car and truck expenses → carAndTruck
• Line 17: Legal and professional → legalAndProfessional
• Line 25: Utilities → utilities
• Line 26: Wages → wages
• Line 28: Total expenses → totalExpenses
• Line 31: Net profit or (loss) → netProfit
```

### Schedule E (Rental/Royalty Income)
```
• Property address → propertyAddress
• Property type → propertyType
• Line 3: Rents received → rentsReceived
• Line 4: Royalties received → royaltiesReceived
• Line 5: Advertising → advertising
• Line 9: Insurance → insurance
• Line 12: Mortgage interest → mortgageInterest
• Line 18: Depreciation → depreciation
• Line 20: Total expenses → totalExpenses
• Line 21: Net income → netIncome
```

### Federal 1040 (Already had most field names, added consistency)
```
• Line 1z: Total wages → totalWages
• Line 11: Adjusted gross income → agi
• Line 15: Taxable income → taxableIncome
• Line 35a: Refund amount → refundAmount
• Line 37: Amount owed → amountOwed
```

## Next Steps

1. **Rebuild backend service**:
   ```bash
   cd backend
   mvn clean package -DskipTests
   ```

2. **Restart extraction service**:
   ```bash
   docker-compose restart extraction-service
   ```
   OR if using full rebuild:
   ```bash
   ./deploy.sh
   ```

3. **Test extraction** by uploading sample documents for:
   - 1099-NEC
   - 1099-MISC
   - W-2G
   - Schedule C
   - Schedule E
   - Federal 1040

4. **Verify** that the extraction review UI now displays fields for all form types using the schema system

## Field Name Mapping

The field names in the extraction prompt now match the schema files:

| Schema File | Extraction Prompt | Example Field Name |
|-------------|-------------------|-------------------|
| `1099-nec-schema.json` | 1099-NEC section | `nonemployeeCompensation` |
| `1099-misc-schema.json` | 1099-MISC section | `rents`, `royalties` |
| `w2g-schema.json` | W-2G section | `grossWinnings`, `typeOfWager` |
| `schedule-c-schema.json` | Schedule C section | `grossReceipts`, `netProfit` |
| `schedule-e-schema.json` | Schedule E section | `rentsReceived`, `propertyAddress` |
| `1040-schema.json` | Federal 1040 section | `agi`, `taxableIncome` |

## Impact

This fix ensures that:
1. AI extraction uses consistent, predictable field names
2. Schema system can map extracted data to display fields
3. UI components show proper labels and formatting for all form types
4. Single source of truth works across UI and backend extraction

## Files Modified

- `backend/extraction-service/src/main/java/com/munitax/extraction/service/RealGeminiService.java`
  - Updated `buildProductionExtractionPrompt()` method
  - Added explicit field name mappings for all form types
