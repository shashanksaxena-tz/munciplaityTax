# Factor Calculation Contracts

**Feature**: Schedule Y - Multi-State Income Sourcing  
**Date**: 2025-11-28  
**Purpose**: Define calculation logic contracts for apportionment factors

This document specifies the precise calculation logic for property, payroll, and sales factors, including sourcing rules and formula application. These contracts serve as the specification for backend implementation and frontend validation.

---

## 1. Property Factor Calculation

### Contract: `calculatePropertyFactor()`

**Input Parameters:**
- `ohioRealProperty: BigDecimal` - Ohio land & buildings value ($)
- `ohioTangiblePersonalProperty: BigDecimal` - Ohio equipment, inventory, vehicles ($)
- `ohioAnnualRent: BigDecimal` - Annual rent paid for Ohio property ($)
- `totalPropertyEverywhere: BigDecimal` - Total property value everywhere ($)
- `averagingMethod: AveragingMethod` - AVERAGE_BEGINNING_ENDING, MONTHLY_AVERAGE, or DAILY_AVERAGE
- `beginningOfYearValue: BigDecimal` - Property value at Jan 1 ($)
- `endOfYearValue: BigDecimal` - Property value at Dec 31 ($)
- `monthlyValues: Map<Month, BigDecimal>` - Optional, required if averagingMethod = MONTHLY_AVERAGE

**Output:**
- `propertyFactorPercentage: BigDecimal` - Factor percentage (0.0000000000 to 1.0000000000)

**Calculation Logic:**

```java
// Step 1: Capitalize rent at 8x
BigDecimal ohioRentedPropertyValue = ohioAnnualRent.multiply(new BigDecimal("8"));

// Step 2: Sum Ohio property
BigDecimal totalOhioProperty = ohioRealProperty
    .add(ohioTangiblePersonalProperty)
    .add(ohioRentedPropertyValue);

// Step 3: Apply averaging method (if needed - for beginning/ending values)
// Note: In most cases, inputs are already averaged. This is for verification.
BigDecimal averagedTotalProperty;
if (averagingMethod == AVERAGE_BEGINNING_ENDING) {
    averagedTotalProperty = beginningOfYearValue
        .add(endOfYearValue)
        .divide(new BigDecimal("2"), 10, RoundingMode.HALF_UP);
} else if (averagingMethod == MONTHLY_AVERAGE) {
    BigDecimal sum = monthlyValues.values().stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    averagedTotalProperty = sum.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
} else if (averagingMethod == DAILY_AVERAGE) {
    // Daily average not implemented in Phase 1 - throw exception
    throw new UnsupportedOperationException("Daily averaging not yet supported");
}

// Step 4: Calculate factor percentage
BigDecimal propertyFactorPercentage;
if (totalPropertyEverywhere.compareTo(BigDecimal.ZERO) == 0) {
    // Zero denominator: Factor is 0%
    propertyFactorPercentage = BigDecimal.ZERO;
} else {
    propertyFactorPercentage = totalOhioProperty
        .divide(totalPropertyEverywhere, 10, RoundingMode.HALF_UP);
}

// Step 5: Validate result is in range [0, 1]
if (propertyFactorPercentage.compareTo(BigDecimal.ZERO) < 0 
    || propertyFactorPercentage.compareTo(BigDecimal.ONE) > 0) {
    throw new ValidationException("Property factor must be between 0% and 100%");
}

return propertyFactorPercentage;
```

**Validation Rules:**
1. `totalPropertyEverywhere > 0` (must have property somewhere)
2. `totalOhioProperty <= totalPropertyEverywhere` (Ohio cannot exceed total)
3. If `averagingMethod = MONTHLY_AVERAGE`, `monthlyValues` must have 12 entries
4. Result must be in range [0.0, 1.0]

**Edge Cases:**
- **Zero property everywhere:** Return 0% (cannot calculate factor)
- **Rented property only:** Capitalize at 8x annual rent
- **Negative property values:** Treat as validation error (property cannot be negative)

---

## 2. Payroll Factor Calculation

### Contract: `calculatePayrollFactor()`

**Input Parameters:**
- `ohioW2Wages: BigDecimal` - W-2 wages for Ohio employees ($)
- `ohioContractorPayments: BigDecimal` - 1099-NEC for Ohio contractors ($)
- `ohioOfficerCompensation: BigDecimal` - Officer W-2 wages for Ohio officers ($)
- `totalPayrollEverywhere: BigDecimal` - Total payroll everywhere ($)
- `remoteEmployeeAllocation: Map<String, BigDecimal>` - Optional, multi-state payroll allocation

**Output:**
- `payrollFactorPercentage: BigDecimal` - Factor percentage (0.0000000000 to 1.0000000000)

**Calculation Logic:**

```java
// Step 1: Sum Ohio payroll
BigDecimal totalOhioPayroll = ohioW2Wages
    .add(ohioContractorPayments)
    .add(ohioOfficerCompensation);

// Step 2: If remote employee allocation provided, validate and use it
if (remoteEmployeeAllocation != null && !remoteEmployeeAllocation.isEmpty()) {
    // Validate: Sum of allocated payroll must equal total payroll (±1% tolerance)
    BigDecimal allocatedSum = remoteEmployeeAllocation.values().stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal tolerance = totalPayrollEverywhere.multiply(new BigDecimal("0.01"));
    BigDecimal difference = allocatedSum.subtract(totalPayrollEverywhere).abs();
    
    if (difference.compareTo(tolerance) > 0) {
        throw new ValidationException("Remote employee allocation sum does not match total payroll");
    }
    
    // Use allocated Ohio payroll if provided
    if (remoteEmployeeAllocation.containsKey("OH")) {
        totalOhioPayroll = remoteEmployeeAllocation.get("OH");
    }
}

// Step 3: Calculate factor percentage
BigDecimal payrollFactorPercentage;
if (totalPayrollEverywhere.compareTo(BigDecimal.ZERO) == 0) {
    // Zero denominator: Factor is 0%
    payrollFactorPercentage = BigDecimal.ZERO;
} else {
    payrollFactorPercentage = totalOhioPayroll
        .divide(totalPayrollEverywhere, 10, RoundingMode.HALF_UP);
}

// Step 4: Validate result is in range [0, 1]
if (payrollFactorPercentage.compareTo(BigDecimal.ZERO) < 0 
    || payrollFactorPercentage.compareTo(BigDecimal.ONE) > 0) {
    throw new ValidationException("Payroll factor must be between 0% and 100%");
}

return payrollFactorPercentage;
```

**Validation Rules:**
1. `totalPayrollEverywhere > 0` (must have payroll somewhere)
2. `totalOhioPayroll <= totalPayrollEverywhere` (Ohio cannot exceed total)
3. If `remoteEmployeeAllocation` provided, sum must equal `totalPayrollEverywhere` (±1% tolerance)
4. Result must be in range [0.0, 1.0]

**Edge Cases:**
- **Zero payroll everywhere:** Return 0% (cannot calculate factor)
- **All remote employees:** Use `remoteEmployeeAllocation` to assign payroll by state
- **Negative payroll:** Treat as validation error (payroll cannot be negative)

---

## 3. Sales Factor Calculation

### Contract: `calculateSalesFactor()`

**Input Parameters:**
- `ohioSalesTangibleGoods: BigDecimal` - Sales of physical products delivered to Ohio ($)
- `ohioSalesServices: BigDecimal` - Service revenue sourced to Ohio ($)
- `ohioSalesRentalIncome: BigDecimal` - Rental income from Ohio property ($)
- `ohioSalesInterest: BigDecimal` - Interest income sourced to Ohio ($)
- `ohioSalesRoyalties: BigDecimal` - Royalty income sourced to Ohio ($)
- `ohioSalesOther: BigDecimal` - Other income sourced to Ohio ($)
- `totalSalesEverywhere: BigDecimal` - Total sales everywhere ($)
- `throwbackAdjustment: BigDecimal` - Sales thrown back to Ohio (calculated from transactions) ($)

**Output:**
- `salesFactorPercentage: BigDecimal` - Factor percentage (0.0000000000 to 1.1000000000, may exceed 100% due to throwback)

**Calculation Logic:**

```java
// Step 1: Sum Ohio sales
BigDecimal totalOhioSales = ohioSalesTangibleGoods
    .add(ohioSalesServices)
    .add(ohioSalesRentalIncome)
    .add(ohioSalesInterest)
    .add(ohioSalesRoyalties)
    .add(ohioSalesOther)
    .add(throwbackAdjustment);

// Step 2: Calculate factor percentage
BigDecimal salesFactorPercentage;
if (totalSalesEverywhere.compareTo(BigDecimal.ZERO) == 0) {
    // Zero denominator: Factor is 0%
    salesFactorPercentage = BigDecimal.ZERO;
} else {
    salesFactorPercentage = totalOhioSales
        .divide(totalSalesEverywhere, 10, RoundingMode.HALF_UP);
}

// Step 3: Validate result is in range [0, 1.1] (allow up to 110% due to throwback)
if (salesFactorPercentage.compareTo(BigDecimal.ZERO) < 0 
    || salesFactorPercentage.compareTo(new BigDecimal("1.1")) > 0) {
    throw new ValidationException("Sales factor must be between 0% and 110%");
}

// Step 4: Log warning if factor > 100%
if (salesFactorPercentage.compareTo(BigDecimal.ONE) > 0) {
    logger.warn("Sales factor exceeds 100% due to throwback: " + salesFactorPercentage);
}

return salesFactorPercentage;
```

**Validation Rules:**
1. `totalSalesEverywhere > 0` (must have sales somewhere)
2. `throwbackAdjustment >= 0` (throwback adds, never subtracts)
3. Result must be in range [0.0, 1.1] (allow up to 110% due to throwback)
4. If result > 1.0, log warning for review

**Edge Cases:**
- **Zero sales everywhere:** Return 0% (cannot calculate factor)
- **Throwback causes factor > 100%:** Allow up to 110%, flag if exceeds
- **Negative sales (refunds):** Treat as validation error (use net sales after refunds)

---

## 4. Throwback Determination

### Contract: `determineThrowback()`

**Input Parameters:**
- `saleTransaction: SaleTransaction` - Individual sale transaction
- `nexusTracking: NexusTracking` - Nexus status for destination state
- `throwbackElection: ThrowbackElection` - Municipality's throwback/throwout election

**Output:**
- `sourcingMethod: SourcingMethod` - DESTINATION, THROWBACK, or THROWOUT
- `allocatedState: String` - Final state assignment
- `throwbackReason: String` - Reason for throwback (if applicable)

**Calculation Logic:**

```java
// Step 1: Check if sale is tangible goods (throwback only applies to goods, not services)
if (saleTransaction.getSaleType() != SaleType.TANGIBLE_GOODS) {
    // Services use market-based or cost-of-performance, not throwback
    return new ThrowbackResult(
        SourcingMethod.DESTINATION, // or MARKET_BASED for services
        saleTransaction.getDestinationState(),
        null
    );
}

// Step 2: Check nexus in destination state
boolean hasDestinationNexus = nexusTracking.hasNexus(
    saleTransaction.getDestinationState(), 
    saleTransaction.getTaxYear()
);

if (hasDestinationNexus) {
    // Has nexus: Assign to destination state (normal sourcing)
    return new ThrowbackResult(
        SourcingMethod.DESTINATION,
        saleTransaction.getDestinationState(),
        null
    );
}

// Step 3: No nexus in destination state - apply throwback/throwout election
if (throwbackElection == ThrowbackElection.THROWBACK) {
    // Throwback: Assign to origin state
    String throwbackReason = String.format(
        "No nexus in %s - Thrown back to origin state %s",
        saleTransaction.getDestinationState(),
        saleTransaction.getOriginState()
    );
    
    return new ThrowbackResult(
        SourcingMethod.THROWBACK,
        saleTransaction.getOriginState(),
        throwbackReason
    );
    
} else if (throwbackElection == ThrowbackElection.THROWOUT) {
    // Throwout: Exclude from both numerator and denominator
    String throwbackReason = String.format(
        "No nexus in %s - Thrown out (excluded from factor)",
        saleTransaction.getDestinationState()
    );
    
    return new ThrowbackResult(
        SourcingMethod.THROWOUT,
        null, // No state assignment
        throwbackReason
    );
    
} else {
    // NONE: Assign to destination state anyway (creates "nowhere income")
    return new ThrowbackResult(
        SourcingMethod.DESTINATION,
        saleTransaction.getDestinationState(),
        "No throwback rule - Assigned to destination despite no nexus"
    );
}
```

**Validation Rules:**
1. Throwback only applies to `TANGIBLE_GOODS` sale type
2. Nexus determination must be current for tax year
3. `throwbackElection` must be fetched from rule engine (municipality-specific)

**Edge Cases:**
- **Services:** Throwback does not apply, use market-based/cost-of-performance
- **Multi-state origin:** If manufactured in OH, warehoused in PA, shipped from PA → Assign throwback to state with greatest property/payroll presence
- **Federal government customer:** Assign to state where federal agency is located (e.g., Pentagon = VA)

---

## 5. Service Revenue Sourcing

### Contract: `sourceServiceRevenue()`

**Input Parameters:**
- `saleTransaction: SaleTransaction` - Service revenue transaction
- `serviceSourcingMethod: ServiceSourcingMethod` - MARKET_BASED or COST_OF_PERFORMANCE
- `customerLocation: String` - Customer state (nullable)
- `serviceAllocationDetails: Map<String, BigDecimal>` - Multi-state allocation (for cost-of-performance)

**Output:**
- `sourcingMethod: SourcingMethod` - MARKET_BASED or COST_OF_PERFORMANCE
- `allocatedState: String` - Final state assignment
- `allocatedAmount: BigDecimal` - Amount allocated (may be prorated)

**Calculation Logic:**

```java
// Step 1: Default to market-based sourcing (modern rule)
if (serviceSourcingMethod == ServiceSourcingMethod.MARKET_BASED) {
    
    // Step 1a: Check if customer location is known
    if (customerLocation != null && !customerLocation.isEmpty()) {
        // Market-based: Assign 100% to customer's state
        return new SourcingResult(
            SourcingMethod.MARKET_BASED,
            customerLocation,
            saleTransaction.getSaleAmount()
        );
    } else {
        // Step 1b: Customer location unknown - fallback to cost-of-performance
        logger.warn("Customer location unknown for transaction " + saleTransaction.getId() 
            + " - Falling back to cost-of-performance");
        serviceSourcingMethod = ServiceSourcingMethod.COST_OF_PERFORMANCE;
    }
}

// Step 2: Cost-of-performance sourcing
if (serviceSourcingMethod == ServiceSourcingMethod.COST_OF_PERFORMANCE) {
    
    if (serviceAllocationDetails != null && !serviceAllocationDetails.isEmpty()) {
        // Step 2a: Multi-state allocation provided (e.g., 70% OH, 30% CA)
        // Validate: Sum must equal sale amount (±1% tolerance)
        BigDecimal allocatedSum = serviceAllocationDetails.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal tolerance = saleTransaction.getSaleAmount().multiply(new BigDecimal("0.01"));
        BigDecimal difference = allocatedSum.subtract(saleTransaction.getSaleAmount()).abs();
        
        if (difference.compareTo(tolerance) > 0) {
            throw new ValidationException("Service allocation sum does not match sale amount");
        }
        
        // Return primary allocation state (state with highest allocation)
        String primaryState = serviceAllocationDetails.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(saleTransaction.getOriginState());
        
        BigDecimal primaryAmount = serviceAllocationDetails.get(primaryState);
        
        return new SourcingResult(
            SourcingMethod.COST_OF_PERFORMANCE,
            primaryState,
            primaryAmount,
            serviceAllocationDetails // Store full allocation for audit
        );
        
    } else {
        // Step 2b: No allocation details - assign 100% to origin state (where work performed)
        return new SourcingResult(
            SourcingMethod.COST_OF_PERFORMANCE,
            saleTransaction.getOriginState(),
            saleTransaction.getSaleAmount()
        );
    }
}

throw new IllegalStateException("Unexpected sourcing method: " + serviceSourcingMethod);
```

**Validation Rules:**
1. Service revenue must have `saleType = SERVICES`
2. Market-based sourcing requires customer location (fallback to cost-of-performance if unknown)
3. Cost-of-performance allocation must sum to sale amount (±1% tolerance)
4. Allocated amount must be <= sale amount

**Edge Cases:**
- **Customer location unknown:** Fallback to cost-of-performance
- **Multi-location customer:** Prorate by benefit received (e.g., IT system used by 3 offices)
- **B2C services (individual consumers):** Use billing address as proxy for customer location

---

## 6. Final Apportionment Percentage

### Contract: `calculateFinalApportionment()`

**Input Parameters:**
- `propertyFactorPercentage: BigDecimal`
- `payrollFactorPercentage: BigDecimal`
- `salesFactorPercentage: BigDecimal`
- `apportionmentFormula: ApportionmentFormula` - TRADITIONAL_THREE_FACTOR, FOUR_FACTOR_DOUBLE_SALES, SINGLE_SALES_FACTOR, or CUSTOM
- `formulaWeights: Map<String, Integer>` - Custom weights if formula = CUSTOM

**Output:**
- `finalApportionmentPercentage: BigDecimal` - Final apportionment percentage (0.0000000000 to 1.0000000000)

**Calculation Logic:**

```java
BigDecimal finalApportionmentPercentage;

// Step 1: Apply formula weights
if (apportionmentFormula == ApportionmentFormula.TRADITIONAL_THREE_FACTOR) {
    // (Property + Payroll + Sales) / 3
    finalApportionmentPercentage = propertyFactorPercentage
        .add(payrollFactorPercentage)
        .add(salesFactorPercentage)
        .divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP);
        
} else if (apportionmentFormula == ApportionmentFormula.FOUR_FACTOR_DOUBLE_SALES) {
    // (Property + Payroll + Sales + Sales) / 4 (Ohio default)
    finalApportionmentPercentage = propertyFactorPercentage
        .add(payrollFactorPercentage)
        .add(salesFactorPercentage)
        .add(salesFactorPercentage) // Double-weight sales
        .divide(new BigDecimal("4"), 10, RoundingMode.HALF_UP);
        
} else if (apportionmentFormula == ApportionmentFormula.SINGLE_SALES_FACTOR) {
    // Sales factor only
    finalApportionmentPercentage = salesFactorPercentage;
    
} else if (apportionmentFormula == ApportionmentFormula.CUSTOM) {
    // Custom weights: e.g., {"property": 1, "payroll": 1, "sales": 2}
    if (formulaWeights == null || formulaWeights.isEmpty()) {
        throw new ValidationException("Custom formula requires formula weights");
    }
    
    int propertyWeight = formulaWeights.getOrDefault("property", 0);
    int payrollWeight = formulaWeights.getOrDefault("payroll", 0);
    int salesWeight = formulaWeights.getOrDefault("sales", 0);
    
    int totalWeight = propertyWeight + payrollWeight + salesWeight;
    if (totalWeight == 0) {
        throw new ValidationException("Formula weights cannot all be zero");
    }
    
    BigDecimal weightedSum = propertyFactorPercentage.multiply(new BigDecimal(propertyWeight))
        .add(payrollFactorPercentage.multiply(new BigDecimal(payrollWeight)))
        .add(salesFactorPercentage.multiply(new BigDecimal(salesWeight)));
    
    finalApportionmentPercentage = weightedSum.divide(
        new BigDecimal(totalWeight), 
        10, 
        RoundingMode.HALF_UP
    );
    
} else {
    throw new IllegalArgumentException("Unknown apportionment formula: " + apportionmentFormula);
}

// Step 2: Validate result is in range [0, 1]
if (finalApportionmentPercentage.compareTo(BigDecimal.ZERO) < 0 
    || finalApportionmentPercentage.compareTo(BigDecimal.ONE) > 0) {
    throw new ValidationException("Final apportionment must be between 0% and 100%");
}

// Step 3: Round to 4 decimals for display (store full precision internally)
BigDecimal displayPercentage = finalApportionmentPercentage.setScale(4, RoundingMode.HALF_UP);

return finalApportionmentPercentage;
```

**Validation Rules:**
1. All factor percentages must be in range [0.0, 1.0] (except sales may be up to 1.1)
2. If formula = CUSTOM, `formulaWeights` must be provided
3. Final result must be in range [0.0, 1.0]
4. Display with 4 decimal places, store with 10 decimal places

**Edge Cases:**
- **Zero-factor scenario:** If a factor has 0% and is included in formula, it contributes 0 to weighted sum (valid)
- **All factors zero:** Final apportionment would be 0% (business has no presence anywhere - validation warning)
- **Sales factor > 100%:** Still contributes to weighted average (throwback creates >100% sales factor)

---

## 7. Test Cases

### Property Factor Test Cases:

**TC-1: Standard Property Factor**
- Input: ohioProperty=$2M, totalProperty=$10M
- Expected: 0.2000000000 (20.00%)

**TC-2: Rented Property Capitalization**
- Input: ohioRent=$100K, totalProperty=$10M
- Expected: ohioRentedValue=$800K, factor=0.0800000000 (8.00%)

**TC-3: Zero Property Everywhere**
- Input: totalProperty=$0
- Expected: 0.0000000000 (0.00%) with warning

### Payroll Factor Test Cases:

**TC-1: Standard Payroll Factor**
- Input: ohioPayroll=$3M, totalPayroll=$7M
- Expected: 0.4285714286 (42.86%)

**TC-2: Remote Employee Allocation**
- Input: remoteAllocation={OH: $3.5M, CA: $2M, NY: $1.5M}, totalPayroll=$7M
- Expected: 0.5000000000 (50.00%)

### Sales Factor Test Cases:

**TC-1: Standard Sales Factor**
- Input: ohioSales=$5M, totalSales=$10M
- Expected: 0.5000000000 (50.00%)

**TC-2: Throwback Adjustment**
- Input: ohioSales=$5M, throwback=$1M, totalSales=$10M
- Expected: 0.6000000000 (60.00%)

**TC-3: Sales Factor > 100%**
- Input: ohioSales=$8M, throwback=$3M, totalSales=$10M
- Expected: 1.1000000000 (110.00%) with warning

### Apportionment Formula Test Cases:

**TC-1: Four-Factor Double-Sales (Ohio Default)**
- Input: property=20%, payroll=40%, sales=60%
- Calculation: (20 + 40 + 60 + 60) / 4 = 180 / 4 = 45%
- Expected: 0.4500000000

**TC-2: Single-Sales-Factor**
- Input: property=20%, payroll=40%, sales=60%
- Calculation: sales only = 60%
- Expected: 0.6000000000

**TC-3: Traditional Three-Factor**
- Input: property=20%, payroll=40%, sales=60%
- Calculation: (20 + 40 + 60) / 3 = 120 / 3 = 40%
- Expected: 0.4000000000

---

## Summary

These calculation contracts define precise logic for:
1. Property factor with rent capitalization
2. Payroll factor with remote employee allocation
3. Sales factor with throwback adjustments
4. Throwback determination based on nexus
5. Service revenue sourcing (market-based vs cost-of-performance)
6. Final apportionment with pluggable formulas

All calculations use `BigDecimal` with 10 decimal precision, rounding to 4 decimals for display. Validation ensures factors are in valid ranges and calculations are reproducible for audit defense.
