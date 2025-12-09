# Schedule X Migration Guide: 27 to 29 Fields

## Overview

This guide documents the migration from the 27-field Schedule X implementation to the enhanced 29-field version.

## What Changed

**Added Fields:**
1. `clubDues` (FR-020A) - Non-deductible club dues
2. `pensionProfitSharingLimits` (FR-020B) - Excess pension/profit-sharing contributions

**Field Count:**
- Previous: 27 total fields (20 add-backs + 7 deductions)
- Current: 29 total fields (22 add-backs + 7 deductions)

## Database Migration

### PostgreSQL JSONB Compatibility

The Schedule X data is stored as JSONB in the database. The new fields are **backward compatible** - no migration script is required because:

1. Existing records without the new fields will default to `0.0` when read
2. The `AddBacks.createEmpty()` method initializes all fields to `0.0`
3. Runtime conversion handles both old and new formats transparently

### Migration Approach

**Option 1: No-op Migration (Recommended)**
- Let the application handle backward compatibility at runtime
- Existing records remain unchanged in database
- New fields are added when records are next updated

**Option 2: Explicit Migration (Optional)**
```sql
-- Add new fields to existing Schedule X records
UPDATE business_returns
SET reconciliation = jsonb_set(
    jsonb_set(
        reconciliation,
        '{addBacks,clubDues}',
        '0.0'
    ),
    '{addBacks,pensionProfitSharingLimits}',
    '0.0'
)
WHERE reconciliation IS NOT NULL
  AND reconciliation->'addBacks' IS NOT NULL
  AND reconciliation->'addBacks'->>'clubDues' IS NULL;
```

## Application Code Changes

### Model Classes

**File:** `BusinessFederalForm.java`
- `AddBacks` record: Added 2 new fields
- `createEmpty()`: Updated to initialize 24 fields instead of 22
- `recalculateTotals()`: Updated to include new fields in calculation

### DTOs

**File:** `BusinessScheduleXDetailsDto.java`
- `AddBacksDto`: Added 2 new fields
- `fromDomainAddBacks()`: Updated mapper to include new fields

### Services

**File:** `ScheduleXCalculationService.java`
- `calculateTotalAddBacks()`: Updated to sum 22 fields instead of 20

**File:** `ScheduleXValidationService.java`
- `validateNonNegativeFields()`: Added validation for 2 new fields

**File:** `BusinessScheduleXService.java`
- `convertFromOldFormat()`: Updated constructor to include new fields (initialized to 0.0)

### Tests

**File:** `BusinessScheduleXServiceTest.java`
- `testDetectNewFormat()`: Updated to construct AddBacks with 24 fields

## Verification Steps

1. **Compile Code:** Ensure all files compile without errors
2. **Run Tests:** Execute `BusinessScheduleXServiceTest` to verify backward compatibility
3. **Database Check:** Query existing records to ensure they load correctly
4. **Integration Test:** Create new return with both old and new fields

## Rollback Plan

If issues occur, rollback is straightforward:
1. The database schema hasn't changed (JSONB is flexible)
2. Revert code changes to previous commit
3. Existing data remains compatible

## API Compatibility

**REST API:** Fully backward compatible
- Old clients not sending new fields: Fields default to 0.0
- New clients sending new fields: Fields stored and processed correctly
- GET requests: Return new fields (defaulted to 0.0 for old records)

## Frontend Impact

Frontend applications need to be updated to:
1. Display new fields in Schedule X form
2. Include new fields in API requests
3. Update total calculations to include new fields

**Recommended Timeline:**
- Backend deployed first (backward compatible)
- Frontend updated within 1 week
- No coordination required between deployments

## Testing Checklist

- [ ] Unit tests pass for new fields
- [ ] Integration tests pass for backward compatibility
- [ ] Old format records load correctly
- [ ] New format records save correctly  
- [ ] Total calculations include new fields
- [ ] Validation rules apply to new fields
- [ ] API accepts both old and new payloads
- [ ] Frontend displays new fields

## Support

For questions or issues, contact the tax-engine-service team.

---

**Version:** 1.0  
**Date:** 2024-12-09  
**Author:** Tax Engine Service Team
