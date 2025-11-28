# Net Operating Loss (NOL) Carryforward & Carryback Tracking System

## Overview

Comprehensive Net Operating Loss (NOL) tracking system that manages multi-year loss carryforwards, 20-year expiration schedules, CARES Act carryback provisions, and 80% taxable income limitation. This feature is essential for businesses that experience losses in some years and need to offset profits in future years.

## Features Implemented

### Core Functionality

1. **Multi-Year NOL Tracking** (FR-001 to FR-006)
   - Create and track NOLs generated in loss years
   - Maintain NOL balances across allowed carryforward periods
   - Automatic retrieval of prior year NOL data
   - Complete audit trail of NOL transactions

2. **80% Taxable Income Limitation** (FR-007 to FR-012)
   - Post-2017 (TCJA): 80% limitation on NOL deduction
   - Pre-2018: 100% offset allowed (legacy rule)
   - Automatic limitation percentage determination by tax year
   - Validation to prevent over-utilization

3. **CARES Act Carryback Support** (FR-013 to FR-020)
   - 2018-2020 losses eligible for 5-year carryback
   - Optional carryback election
   - Automatic refund calculation
   - Form 27-NOL-CB generation
   - State-specific carryback rules support

4. **NOL Expiration Management** (FR-021 to FR-026)
   - Pre-2018 NOLs: 20-year expiration tracking
   - Post-2017 NOLs: Indefinite carryforward (federal)
   - FIFO ordering (oldest first) by default
   - Expiration alerts (CRITICAL ≤1yr, WARNING 1-2yr, INFO 2-3yr)
   - Manual ordering override with justification

5. **Entity Type Handling** (FR-027 to FR-031)
   - C-Corporation: Entity-level NOL retention
   - S-Corporation: Pass-through to shareholders
   - Partnership: Allocation to partners
   - Sole Proprietorship: Individual return NOL

6. **Multi-State Apportionment** (FR-032 to FR-035)
   - Separate federal and state NOL tracking
   - Ohio NOL = Federal NOL × Ohio apportionment %
   - Jurisdiction-specific schedules

7. **Amended Return Support** (FR-040 to FR-043)
   - NOL recalculation on amended returns
   - Cascading effect identification
   - Estimated refund calculation
   - Amendment tracking and audit trail

## Architecture

### Domain Entities

- **NOL**: Core entity tracking net operating losses
- **NOLUsage**: Records of NOL utilization in tax years
- **NOLCarryback**: CARES Act carryback elections and refunds
- **NOLSchedule**: Consolidated NOL schedules for tax returns
- **NOLExpirationAlert**: Alerts for NOLs approaching expiration
- **NOLAmendment**: Amended return NOL impact tracking

### Service Layer

- **NOLService**: Core NOL creation, tracking, and application
- **NOLCarrybackService**: CARES Act carryback processing
- **NOLScheduleService**: Schedule generation and reconciliation

### Repository Layer

- JPA repositories with custom queries
- Multi-tenant support via tenant_id
- Optimized indexes for performance

### REST API

- POST `/api/nol` - Create new NOL
- GET `/api/nol/{businessId}` - Get NOLs for business
- GET `/api/nol/{businessId}/available` - Get available balance
- POST `/api/nol/apply` - Apply NOL deduction
- GET `/api/nol/schedule/{returnId}` - Get NOL schedule
- GET `/api/nol/schedule/{businessId}/vintages/{taxYear}` - Get vintage breakdown
- POST `/api/nol/carryback` - Elect carryback
- GET `/api/nol/alerts/{businessId}` - Get expiration alerts

## Database Schema

### Tables Created

1. **nols** (V1.30) - Core NOL tracking
2. **nol_usages** (V1.31) - Usage tracking
3. **nol_carrybacks** (V1.32) - Carryback records
4. **nol_schedules** (V1.33) - Consolidated schedules
5. **nol_expiration_alerts** (V1.34) - Expiration warnings
6. **nol_amendments** (V1.35) - Amendment tracking

### Key Constraints

- Multi-tenant isolation via tenant_id
- Check constraints for business rules
- Foreign key relationships
- Unique constraints where applicable

## Business Rules

### Tax Year Rules

| Tax Year | Limitation | Carryforward | Carryback |
|----------|-----------|--------------|-----------|
| Pre-2018 | 100% | 20 years | 2 years |
| 2018-2020 | 80% | Indefinite | 5 years (CARES Act) |
| 2021+ | 80% | Indefinite | 0 years |

### FIFO Ordering

NOLs are applied oldest first by default to minimize expiration risk:
1. Use 2005 NOL (expiring 2025)
2. Then 2010 NOL (expiring 2030)
3. Then 2019 NOL (no expiration)

Manual override allowed with justification.

### Apportionment

For multi-state businesses:
- Federal NOL: Total business loss
- Ohio NOL: Federal NOL × Ohio apportionment %
- Example: $1M federal loss × 30% Ohio = $300K Ohio NOL

## Testing

### Unit Tests

Comprehensive unit tests covering:
- NOL creation with expiration calculation
- State NOL apportionment
- Available balance calculation
- 80%/100% limitation logic
- FIFO ordering application
- Validation and error handling

### Test Coverage

- ✅ Core NOL service logic
- ✅ Expiration date calculation
- ✅ Limitation percentage determination
- ✅ Multi-year balance tracking
- ✅ FIFO ordering enforcement
- ✅ Error handling and validation

## Usage Examples

### Create NOL

```java
CreateNOLRequest request = CreateNOLRequest.builder()
    .businessId(businessId)
    .taxYear(2020)
    .lossAmount(new BigDecimal("200000.00"))
    .jurisdiction(Jurisdiction.FEDERAL)
    .entityType(EntityType.C_CORP)
    .build();

NOLResponse response = nolController.createNOL(request, userId, tenantId);
```

### Apply NOL Deduction

```java
ApplyNOLRequest request = ApplyNOLRequest.builder()
    .businessId(businessId)
    .returnId(returnId)
    .taxYear(2024)
    .taxableIncomeBeforeNOL(new BigDecimal("300000.00"))
    .nolDeductionAmount(new BigDecimal("240000.00")) // 80% limit
    .taxRate(new BigDecimal("2.50"))
    .build();

nolController.applyNOLDeduction(request, tenantId);
```

### Elect Carryback

```java
CarrybackElectionRequest request = CarrybackElectionRequest.builder()
    .nolId(nolId)
    .priorYearData(Map.of(
        2018, new PriorYearDataDTO(
            new BigDecimal("400000.00"), // Taxable income
            new BigDecimal("2.50"),      // Tax rate
            new BigDecimal("10000.00"),  // Tax paid
            priorReturnId
        )
    ))
    .build();

CarrybackElectionResponse response = nolController.electCarryback(request, tenantId);
```

## Success Metrics

- **Multi-Year Accuracy**: 100% automatic tracking across years
- **Expiration Prevention**: Zero NOLs expire due to lack of alerts
- **80% Compliance**: 100% enforcement of TCJA limitation
- **Time Savings**: 10 minutes vs 1-2 hours manual work

## Dependencies

- Spring Boot 3.2.3
- Spring Data JPA
- PostgreSQL 16
- Flyway (database migrations)
- Lombok (boilerplate reduction)
- JUnit 5 + Mockito (testing)

## Future Enhancements

- IRC Section 382 limitation (ownership changes)
- AMT NOL calculation
- Consolidated return SRLY rules
- State-by-state conformity tracking
- Enhanced UI components
- Integration tests
- Performance optimization

## Related Specifications

- Spec 2: Schedule X Expansion (M-1 adjustments affect NOL)
- Spec 3: Enhanced Discrepancy Detection (NOL validation)
- Spec 4: Rule Configuration UI (NOL rules management)
- Spec 5: Schedule Y Sourcing (apportionment for state NOL)
- Spec 8: Business Form Library (Form 27-NOL generation)

## References

- IRS Publication 536: Net Operating Losses (NOLs) for Individuals, Estates, and Trusts
- Tax Cuts and Jobs Act (TCJA) 2017
- CARES Act 2020
- Ohio Revised Code Chapter 5733 (Commercial Activity Tax)
