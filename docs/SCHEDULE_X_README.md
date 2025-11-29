# Schedule X Expansion - Comprehensive Business Tax Reconciliation

## Overview

This feature expands Business Schedule X from 6 basic fields to 27 comprehensive book-to-tax reconciliation fields, enabling CPAs to complete accurate M-1 reconciliations for C-Corporations, Partnerships, and S-Corporations.

## What's New

### ✨ Features

- **27 Comprehensive Fields**: Complete M-1 reconciliation coverage
  - 20 Add-Back fields (expenses that increase federal income for municipal purposes)
  - 7 Deduction fields (income that decreases federal income for municipal purposes)
  
- **Smart UI Organization**: Fields grouped by category with help text
  - Depreciation & Amortization
  - Taxes & State Adjustments
  - Meals & Entertainment
  - Related-Party Transactions
  - Intangible Income (5% Rule)
  - And more...

- **Conditional Rendering**: Entity-specific fields
  - Guaranteed Payments (partnerships only)
  - Related-Party Transactions (S-Corps)
  
- **Auto-Calculation Helpers**:
  - Meals & Entertainment (50% federal → 0% municipal)
  - 5% Rule (expenses on intangible income)
  - Related-Party Excess (paid - FMV)

- **Real-Time Calculations**:
  - Total Add-Backs
  - Total Deductions
  - Adjusted Municipal Income

## Quick Start

### Prerequisites

- **Backend**: Java 21, Maven 3.9+, PostgreSQL 16
- **Frontend**: Node.js 20.x, npm 10.x
- **IDE**: IntelliJ IDEA, VS Code with Java/TypeScript extensions

### Installation

```bash
# Clone repository
git clone https://github.com/yourusername/munciplaityTax.git
cd munciplaityTax

# Install frontend dependencies
npm install

# Build frontend
npm run build

# Backend setup (requires Java 21)
cd backend/tax-engine-service
mvn clean install
mvn spring-boot:run
```

### Development

```bash
# Frontend development server
npm run dev

# Backend development (hot reload)
cd backend/tax-engine-service
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

## Architecture

### Data Model

Schedule X data is stored as JSONB in PostgreSQL:

```typescript
interface BusinessScheduleXDetails {
  fedTaxableIncome: number;
  addBacks: {
    // 20 add-back fields
    depreciationAdjustment: number;
    mealsAndEntertainment: number;
    incomeAndStateTaxes: number;
    // ... 17 more fields
  };
  deductions: {
    // 7 deduction fields
    interestIncome: number;
    dividends: number;
    capitalGains: number;
    // ... 4 more fields
  };
  calculatedFields: {
    totalAddBacks: number;
    totalDeductions: number;
    adjustedMunicipalIncome: number;
  };
}
```

### Calculation Formula

```
Adjusted Municipal Income = Federal Taxable Income + Total Add-Backs - Total Deductions
```

Example (User Story 1 - C-Corp):
```
Federal Income:        $500,000
+ Depreciation:        $ 50,000
+ Meals:               $ 15,000
+ State Taxes:         $ 10,000
- Deductions:          $      0
= Adjusted Municipal:  $575,000
```

## API Examples

### 1. Get Schedule X Data

```bash
curl -X GET http://localhost:8080/api/net-profits/123e4567-e89b-12d3-a456-426614174000/schedule-x \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Response:
```json
{
  "fedTaxableIncome": 500000,
  "addBacks": {
    "depreciationAdjustment": 50000,
    "mealsAndEntertainment": 15000,
    "incomeAndStateTaxes": 10000,
    ...
  },
  "deductions": {
    "interestIncome": 0,
    "dividends": 0,
    "capitalGains": 0,
    ...
  },
  "calculatedFields": {
    "totalAddBacks": 75000,
    "totalDeductions": 0,
    "adjustedMunicipalIncome": 575000
  }
}
```

### 2. Update Schedule X Data

```bash
curl -X PUT http://localhost:8080/api/net-profits/123e4567-e89b-12d3-a456-426614174000/schedule-x \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "fedTaxableIncome": 500000,
    "addBacks": {
      "depreciationAdjustment": 50000,
      "mealsAndEntertainment": 15000,
      "incomeAndStateTaxes": 10000
    },
    "deductions": {}
  }'
```

### 3. Auto-Calculate Meals Add-Back

```bash
curl -X POST http://localhost:8080/api/schedule-x/auto-calculate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "field": "mealsAndEntertainment",
    "inputs": {
      "federalMeals": 15000
    }
  }'
```

Response:
```json
{
  "calculatedValue": 30000,
  "explanation": "Municipal allows 0% meals deduction. Federal deducted $15,000 (50% of $30,000 total meals expense). Add back full $30,000."
}
```

### 4. Auto-Calculate 5% Rule

```bash
curl -X POST http://localhost:8080/api/schedule-x/auto-calculate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "field": "expensesOnIntangibleIncome",
    "inputs": {
      "interestIncome": 20000,
      "dividends": 15000,
      "capitalGains": 0
    }
  }'
```

Response:
```json
{
  "calculatedValue": 1750,
  "explanation": "5% Rule: Add back 5% of non-taxable intangible income. $35,000 intangible income × 5% = $1,750."
}
```

## User Stories

### User Story 1: C-Corp with Depreciation, Meals, State Taxes

**Scenario**: CPA files Form 27 for C-Corp with $500K federal income

**Inputs**:
- Federal Income: $500,000
- Depreciation Adjustment: $50,000 (book < MACRS)
- Meals & Entertainment: $15,000 federal deduction
- State Income Taxes: $10,000

**Result**: Adjusted Municipal Income = $575,000

### User Story 2: Partnership with Guaranteed Payments and Intangible Income

**Scenario**: Partnership with $300K federal income

**Inputs**:
- Federal Income: $300,000
- Guaranteed Payments: $50,000 (Form 1065 Line 10)
- Interest Income: $20,000 (deduction)
- Dividends: $15,000 (deduction)
- 5% Rule: $1,750 (5% of $35K intangible income)

**Result**: Adjusted Municipal Income = $316,750

### User Story 3: S-Corp with Related-Party Transactions

**Scenario**: S-Corp with $400K federal income

**Inputs**:
- Federal Income: $400,000
- Related-Party Rent: Paid $10,000, FMV $7,500 → Excess $2,500

**Result**: Adjusted Municipal Income = $402,500

## Testing

### Run Frontend Tests

```bash
npm run test
```

### Run Backend Tests

```bash
cd backend/tax-engine-service
mvn test
```

### Test Coverage

- ✅ **Unit Tests**: 10+ tests covering calculation logic
- ✅ **Component Tests**: 5+ tests for UI rendering
- ✅ **Integration Tests**: 3 user story tests

## Troubleshooting

### Issue: Frontend Build Fails

**Solution**: Check Node.js version
```bash
node --version  # Should be v20.x
npm install     # Reinstall dependencies
npm run build   # Retry build
```

### Issue: Backend Won't Start

**Solution**: Verify Java version
```bash
java --version  # Should be Java 21
mvn --version   # Should be Maven 3.9+
```

**Error**: `Unsupported class file major version 65`
- This means Java 21 bytecode is being run on Java 17
- **Solution**: Upgrade to Java 21 or wait for backwards-compatible build

### Issue: ScheduleXAccordion Not Showing All Fields

**Solution**: Check entity type prop
```typescript
<ScheduleXAccordion
  scheduleX={schX}
  onUpdate={setSchX}
  entityType="C-CORP"  // Must be valid: C-CORP, PARTNERSHIP, or S-CORP
/>
```

### Issue: Calculations Not Updating

**Solution**: Ensure `recalculateTotals()` is called after updates
```typescript
const updated = { ...schX, fedTaxableIncome: newValue };
setSchX(recalculateTotals(updated));  // Recalculate totals
```

## Documentation

- **Specification**: `/specs/2-expand-schedule-x/spec.md`
- **Data Model**: `/specs/2-expand-schedule-x/data-model.md`
- **API Contracts**: `/specs/2-expand-schedule-x/contracts/`
- **Tasks**: `/specs/2-expand-schedule-x/tasks.md`

## Contributing

### Code Style

- **Backend**: Follow Java Google Style Guide
- **Frontend**: Use Prettier (runs on save in VS Code)

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/schedule-x-enhancement

# Commit changes
git add .
git commit -m "feat: add depreciation field help text"

# Push to remote
git push origin feature/schedule-x-enhancement

# Create pull request on GitHub
```

### Testing Requirements

- All new features must have unit tests
- Integration tests for user stories
- Frontend builds must pass (`npm run build`)

## Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/munciplaityTax/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/munciplaityTax/discussions)
- **Email**: support@munitax.com

## License

© 2024 MuniTax Corporation. All rights reserved.
