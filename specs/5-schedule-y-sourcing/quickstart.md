# Quickstart Guide: Schedule Y - Multi-State Income Sourcing

**Feature**: Schedule Y - Multi-State Income Sourcing  
**Date**: 2025-11-28  
**Audience**: Developers implementing apportionment feature

This guide provides step-by-step instructions for setting up, developing, and testing the Schedule Y apportionment feature.

---

## Prerequisites

### Required Tools
- **Java 21 LTS** (backend)
- **Node.js 18+** and npm (frontend)
- **PostgreSQL 16+** (database)
- **Maven 3.9+** (Java build tool)
- **Git** (version control)
- **IntelliJ IDEA or VS Code** (IDE)
- **Postman or cURL** (API testing)

### Required Knowledge
- Spring Boot 3.x (REST APIs, JPA, dependency injection)
- React 18+ with TypeScript
- PostgreSQL (multi-tenant schemas, JSONB, triggers)
- REST API design and OpenAPI/Swagger
- Tax apportionment concepts (Joyce/Finnigan, throwback, market-based sourcing)

---

## Project Setup

### 1. Clone Repository

```bash
git clone https://github.com/your-org/munciplaityTax.git
cd munciplaityTax
git checkout 5-schedule-y-sourcing
```

### 2. Database Setup

**Create Tenant Schemas:**

```sql
-- Connect to PostgreSQL as superuser
psql -U postgres -d munitax

-- Create tenant schemas (one per municipality)
CREATE SCHEMA IF NOT EXISTS dublin;
CREATE SCHEMA IF NOT EXISTS columbus;
CREATE SCHEMA IF NOT EXISTS westerville;

-- Grant permissions
GRANT ALL ON SCHEMA dublin TO munitax_app;
GRANT ALL ON SCHEMA columbus TO munitax_app;
GRANT ALL ON SCHEMA westerville TO munitax_app;
```

**Run Migrations:**

```bash
# Backend directory
cd backend

# Run Flyway migrations to create tables
mvn flyway:migrate

# Verify tables created
psql -U munitax_app -d munitax -c "\dt dublin.*"
# Should show: schedule_y, property_factor, payroll_factor, sales_factor, 
#              sale_transaction, nexus_tracking, apportionment_audit_log
```

### 3. Backend Setup

**Configure Application Properties:**

```yaml
# backend/src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/munitax
    username: munitax_app
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway for schema changes
    properties:
      hibernate:
        default_schema: dublin  # Default tenant
        jdbc:
          time_zone: UTC

# Multi-tenancy configuration
tenant:
  resolver: jwt-claim  # Extract tenant from JWT "tenant" claim
  default: dublin

# Logging
logging:
  level:
    com.munitax.taxengine.apportionment: DEBUG
    org.hibernate.SQL: DEBUG
```

**Build Backend:**

```bash
cd backend
mvn clean install -DskipTests

# Run backend service
mvn spring-boot:run
```

**Verify Backend Started:**
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### 4. Frontend Setup

**Install Dependencies:**

```bash
cd ..  # Back to repo root
npm install
```

**Configure Environment:**

```bash
# Create .env.local
cat > .env.local << EOF
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_AUTH_ENABLED=true
VITE_TENANT_DEFAULT=dublin
EOF
```

**Run Frontend:**

```bash
npm run dev
# Frontend accessible at http://localhost:5173
```

---

## Development Workflow

### 1. Create New Feature Branch

```bash
git checkout 5-schedule-y-sourcing
git pull origin 5-schedule-y-sourcing
git checkout -b feature/schedule-y-property-factor
```

### 2. Backend Development

**Add Entity (JPA Model):**

```java
// backend/src/main/java/com/munitax/taxengine/apportionment/model/PropertyFactor.java
package com.munitax.taxengine.apportionment.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "property_factor", schema = "#{tenantSchema}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyFactor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID propertyFactorId;
    
    @Column(nullable = false)
    private UUID scheduleYId;
    
    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal ohioRealProperty = BigDecimal.ZERO;
    
    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal ohioTangiblePersonalProperty = BigDecimal.ZERO;
    
    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal ohioAnnualRent = BigDecimal.ZERO;
    
    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal totalPropertyEverywhere;
    
    @Column(precision = 14, scale = 10, nullable = false)
    private BigDecimal propertyFactorPercentage;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AveragingMethod averagingMethod = AveragingMethod.AVERAGE_BEGINNING_ENDING;
    
    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal beginningOfYearValue;
    
    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal endOfYearValue;
    
    @Column(nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate = LocalDateTime.now();
    
    // Computed field (calculated in service layer)
    @Transient
    public BigDecimal getOhioRentedPropertyValue() {
        return ohioAnnualRent.multiply(new BigDecimal("8"));
    }
    
    @Transient
    public BigDecimal getTotalOhioProperty() {
        return ohioRealProperty
            .add(ohioTangiblePersonalProperty)
            .add(getOhioRentedPropertyValue());
    }
}
```

**Add Repository:**

```java
// backend/src/main/java/com/munitax/taxengine/apportionment/repository/PropertyFactorRepository.java
package com.munitax.taxengine.apportionment.repository;

import com.munitax.taxengine.apportionment.model.PropertyFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyFactorRepository extends JpaRepository<PropertyFactor, UUID> {
    Optional<PropertyFactor> findByScheduleYId(UUID scheduleYId);
}
```

**Add Service:**

```java
// backend/src/main/java/com/munitax/taxengine/apportionment/service/FactorCalculationService.java
package com.munitax.taxengine.apportionment.service;

import com.munitax.taxengine.apportionment.model.PropertyFactor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class FactorCalculationService {
    
    public BigDecimal calculatePropertyFactor(PropertyFactor propertyFactor) {
        // Step 1: Get total Ohio property (includes rented property capitalized at 8x)
        BigDecimal totalOhioProperty = propertyFactor.getTotalOhioProperty();
        
        // Step 2: Get total property everywhere
        BigDecimal totalPropertyEverywhere = propertyFactor.getTotalPropertyEverywhere();
        
        // Step 3: Handle zero denominator
        if (totalPropertyEverywhere.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Total property everywhere is zero - returning 0% factor");
            return BigDecimal.ZERO;
        }
        
        // Step 4: Calculate factor percentage
        BigDecimal factorPercentage = totalOhioProperty.divide(
            totalPropertyEverywhere, 
            10,  // 10 decimal places for precision
            RoundingMode.HALF_UP
        );
        
        // Step 5: Validate range [0, 1]
        if (factorPercentage.compareTo(BigDecimal.ZERO) < 0 
            || factorPercentage.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Property factor must be between 0% and 100%");
        }
        
        log.debug("Calculated property factor: {} ({} / {})", 
            factorPercentage, totalOhioProperty, totalPropertyEverywhere);
        
        return factorPercentage;
    }
}
```

**Add Controller:**

```java
// backend/src/main/java/com/munitax/taxengine/apportionment/controller/ApportionmentController.java
package com.munitax.taxengine.apportionment.controller;

import com.munitax.taxengine.apportionment.dto.ApportionmentCalculationRequest;
import com.munitax.taxengine.apportionment.dto.ApportionmentCalculationResponse;
import com.munitax.taxengine.apportionment.service.ApportionmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/apportionment")
@RequiredArgsConstructor
public class ApportionmentController {
    
    private final ApportionmentService apportionmentService;
    
    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('INDIVIDUAL', 'CPA', 'AUDITOR')")
    public ResponseEntity<ApportionmentCalculationResponse> calculateApportionment(
        @RequestBody ApportionmentCalculationRequest request
    ) {
        ApportionmentCalculationResponse response = apportionmentService.calculateApportionment(request);
        return ResponseEntity.ok(response);
    }
}
```

### 3. Frontend Development

**Add TypeScript Types:**

```typescript
// src/types/apportionment.ts
export interface PropertyFactorInput {
  ohioRealProperty: number;
  ohioTangiblePersonalProperty: number;
  ohioAnnualRent: number;
  totalPropertyEverywhere: number;
  averagingMethod: AveragingMethod;
  beginningOfYearValue: number;
  endOfYearValue: number;
}

export enum AveragingMethod {
  AVERAGE_BEGINNING_ENDING = 'AVERAGE_BEGINNING_ENDING',
  MONTHLY_AVERAGE = 'MONTHLY_AVERAGE',
  DAILY_AVERAGE = 'DAILY_AVERAGE'
}

export interface ScheduleYResponse {
  scheduleYId: string;
  returnId: string;
  municipalityCode: string;
  taxYear: number;
  propertyFactorPercentage: number;
  payrollFactorPercentage: number;
  salesFactorPercentage: number;
  finalApportionmentPercentage: number;
}
```

**Add Service Client:**

```typescript
// src/services/apportionmentService.ts
import axios from 'axios';
import { ApportionmentCalculationRequest, ScheduleYResponse } from '../types/apportionment';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const apportionmentService = {
  calculateApportionment: async (
    request: ApportionmentCalculationRequest
  ): Promise<ScheduleYResponse> => {
    const response = await axios.post(
      `${API_BASE_URL}/apportionment/calculate`,
      request,
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('jwt')}`
        }
      }
    );
    return response.data;
  },

  getScheduleY: async (returnId: string): Promise<ScheduleYResponse[]> => {
    const response = await axios.get(
      `${API_BASE_URL}/apportionment/${returnId}`,
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('jwt')}`
        }
      }
    );
    return response.data;
  }
};
```

**Add React Component:**

```tsx
// src/components/apportionment/PropertyFactorInput.tsx
import React, { useState } from 'react';
import { PropertyFactorInput } from '../../types/apportionment';

interface PropertyFactorInputProps {
  value: PropertyFactorInput;
  onChange: (value: PropertyFactorInput) => void;
}

export const PropertyFactorInputComponent: React.FC<PropertyFactorInputProps> = ({ 
  value, 
  onChange 
}) => {
  const handleChange = (field: keyof PropertyFactorInput, newValue: number) => {
    onChange({ ...value, [field]: newValue });
  };

  // Calculate rented property value (8x annual rent)
  const rentedPropertyValue = value.ohioAnnualRent * 8;
  const totalOhioProperty = 
    value.ohioRealProperty + 
    value.ohioTangiblePersonalProperty + 
    rentedPropertyValue;

  return (
    <div className="space-y-4 p-4 border rounded-lg">
      <h3 className="text-lg font-semibold">Property Factor</h3>
      
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium mb-1">
            Ohio Real Property (Land & Buildings)
          </label>
          <input
            type="number"
            value={value.ohioRealProperty}
            onChange={(e) => handleChange('ohioRealProperty', parseFloat(e.target.value) || 0)}
            className="w-full px-3 py-2 border rounded"
            placeholder="$0.00"
          />
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">
            Ohio Tangible Personal Property (Equipment, Inventory)
          </label>
          <input
            type="number"
            value={value.ohioTangiblePersonalProperty}
            onChange={(e) => handleChange('ohioTangiblePersonalProperty', parseFloat(e.target.value) || 0)}
            className="w-full px-3 py-2 border rounded"
            placeholder="$0.00"
          />
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">
            Ohio Annual Rent
          </label>
          <input
            type="number"
            value={value.ohioAnnualRent}
            onChange={(e) => handleChange('ohioAnnualRent', parseFloat(e.target.value) || 0)}
            className="w-full px-3 py-2 border rounded"
            placeholder="$0.00"
          />
          <p className="text-xs text-gray-500 mt-1">
            Rent will be capitalized at 8x: ${rentedPropertyValue.toLocaleString()}
          </p>
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">
            Total Property Everywhere
          </label>
          <input
            type="number"
            value={value.totalPropertyEverywhere}
            onChange={(e) => handleChange('totalPropertyEverywhere', parseFloat(e.target.value) || 0)}
            className="w-full px-3 py-2 border rounded"
            placeholder="$0.00"
          />
        </div>
      </div>

      <div className="bg-blue-50 p-3 rounded">
        <p className="text-sm font-medium">Calculated Values:</p>
        <p className="text-sm">Total Ohio Property: ${totalOhioProperty.toLocaleString()}</p>
        <p className="text-sm">
          Property Factor: {
            value.totalPropertyEverywhere > 0 
              ? ((totalOhioProperty / value.totalPropertyEverywhere) * 100).toFixed(2) 
              : 0
          }%
        </p>
      </div>
    </div>
  );
};
```

### 4. Testing

**Backend Unit Tests:**

```java
// backend/src/test/java/com/munitax/taxengine/apportionment/service/FactorCalculationServiceTest.java
package com.munitax.taxengine.apportionment.service;

import com.munitax.taxengine.apportionment.model.PropertyFactor;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class FactorCalculationServiceTest {
    
    private final FactorCalculationService service = new FactorCalculationService();
    
    @Test
    void testCalculatePropertyFactor_Standard() {
        // Arrange
        PropertyFactor propertyFactor = PropertyFactor.builder()
            .ohioRealProperty(new BigDecimal("2000000"))
            .ohioTangiblePersonalProperty(BigDecimal.ZERO)
            .ohioAnnualRent(BigDecimal.ZERO)
            .totalPropertyEverywhere(new BigDecimal("10000000"))
            .build();
        
        // Act
        BigDecimal result = service.calculatePropertyFactor(propertyFactor);
        
        // Assert
        assertEquals(new BigDecimal("0.2000000000"), result);
    }
    
    @Test
    void testCalculatePropertyFactor_WithRent() {
        // Arrange: $100K rent should be capitalized to $800K
        PropertyFactor propertyFactor = PropertyFactor.builder()
            .ohioRealProperty(BigDecimal.ZERO)
            .ohioTangiblePersonalProperty(BigDecimal.ZERO)
            .ohioAnnualRent(new BigDecimal("100000"))
            .totalPropertyEverywhere(new BigDecimal("10000000"))
            .build();
        
        // Act
        BigDecimal result = service.calculatePropertyFactor(propertyFactor);
        
        // Assert (800K / 10M = 0.08)
        assertEquals(new BigDecimal("0.0800000000"), result);
    }
    
    @Test
    void testCalculatePropertyFactor_ZeroDenominator() {
        // Arrange
        PropertyFactor propertyFactor = PropertyFactor.builder()
            .ohioRealProperty(new BigDecimal("100000"))
            .totalPropertyEverywhere(BigDecimal.ZERO)
            .build();
        
        // Act
        BigDecimal result = service.calculatePropertyFactor(propertyFactor);
        
        // Assert: Should return 0 when denominator is zero
        assertEquals(BigDecimal.ZERO, result);
    }
}
```

**Frontend Tests:**

```typescript
// src/components/apportionment/PropertyFactorInput.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { PropertyFactorInputComponent } from './PropertyFactorInput';
import { AveragingMethod } from '../../types/apportionment';

describe('PropertyFactorInput', () => {
  const mockValue = {
    ohioRealProperty: 2000000,
    ohioTangiblePersonalProperty: 500000,
    ohioAnnualRent: 100000,
    totalPropertyEverywhere: 10000000,
    averagingMethod: AveragingMethod.AVERAGE_BEGINNING_ENDING,
    beginningOfYearValue: 10000000,
    endOfYearValue: 10000000
  };

  it('should render property factor inputs', () => {
    render(
      <PropertyFactorInputComponent 
        value={mockValue} 
        onChange={() => {}} 
      />
    );

    expect(screen.getByLabelText(/Ohio Real Property/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Ohio Annual Rent/i)).toBeInTheDocument();
  });

  it('should calculate rented property value at 8x', () => {
    render(
      <PropertyFactorInputComponent 
        value={mockValue} 
        onChange={() => {}} 
      />
    );

    // $100K rent * 8 = $800K
    expect(screen.getByText(/\$800,000/i)).toBeInTheDocument();
  });

  it('should calculate property factor percentage', () => {
    render(
      <PropertyFactorInputComponent 
        value={mockValue} 
        onChange={() => {}} 
      />
    );

    // (2M + 500K + 800K) / 10M = 33%
    expect(screen.getByText(/33\.00%/i)).toBeInTheDocument();
  });
});
```

**Run Tests:**

```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd ..
npm test
```

---

## API Testing

### Using Postman

**1. Import OpenAPI Spec:**
- File → Import → `specs/5-schedule-y-sourcing/contracts/apportionment-api.yaml`

**2. Set Environment Variables:**
```json
{
  "baseUrl": "http://localhost:8080/api/v1",
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**3. Test Calculate Apportionment:**

```json
POST {{baseUrl}}/apportionment/calculate
Authorization: Bearer {{jwt}}

{
  "returnId": "123e4567-e89b-12d3-a456-426614174000",
  "municipalityCode": "COLUMBUS",
  "taxYear": 2024,
  "propertyFactor": {
    "ohioRealProperty": 2000000,
    "ohioTangiblePersonalProperty": 500000,
    "ohioAnnualRent": 100000,
    "totalPropertyEverywhere": 10000000,
    "averagingMethod": "AVERAGE_BEGINNING_ENDING",
    "beginningOfYearValue": 10000000,
    "endOfYearValue": 10000000
  },
  "payrollFactor": {
    "ohioW2Wages": 3000000,
    "ohioContractorPayments": 0,
    "ohioOfficerCompensation": 0,
    "totalPayrollEverywhere": 7000000
  },
  "salesFactor": {
    "ohioSalesTangibleGoods": 5000000,
    "totalSalesEverywhere": 10000000
  },
  "elections": {
    "sourcingMethodElection": "FINNIGAN",
    "throwbackElection": "THROWBACK",
    "serviceSourcingMethod": "MARKET_BASED"
  }
}
```

**Expected Response:**

```json
{
  "scheduleYId": "987fcdeb-51a3-12d3-b567-123456789abc",
  "returnId": "123e4567-e89b-12d3-a456-426614174000",
  "municipalityCode": "COLUMBUS",
  "taxYear": 2024,
  "propertyFactorPercentage": 0.3300000000,
  "payrollFactorPercentage": 0.4285714286,
  "salesFactorPercentage": 0.5000000000,
  "finalApportionmentPercentage": 0.4221428571,
  "apportionmentFormula": "FOUR_FACTOR_DOUBLE_SALES",
  "elections": {
    "sourcingMethodElection": "FINNIGAN",
    "throwbackElection": "THROWBACK",
    "serviceSourcingMethod": "MARKET_BASED"
  },
  "calculatedDate": "2025-11-28T03:23:50Z"
}
```

---

## Common Issues & Solutions

### Issue 1: Multi-Tenant Schema Not Found

**Error:** `org.postgresql.util.PSQLException: ERROR: schema "dublin" does not exist`

**Solution:**
```sql
-- Create missing schema
CREATE SCHEMA IF NOT EXISTS dublin;
GRANT ALL ON SCHEMA dublin TO munitax_app;

-- Run migrations
mvn flyway:migrate
```

### Issue 2: BigDecimal Precision Loss

**Error:** Factor calculations return incorrect percentages

**Solution:**
```java
// Always use scale=10 and HALF_UP rounding
BigDecimal result = numerator.divide(denominator, 10, RoundingMode.HALF_UP);
```

### Issue 3: JWT Tenant Claim Missing

**Error:** `IllegalStateException: Tenant not found in JWT`

**Solution:**
```java
// Ensure JWT includes "tenant" claim
{
  "sub": "user123",
  "tenant": "dublin",
  "roles": ["INDIVIDUAL"]
}
```

---

## Next Steps

1. **Complete Property Factor** (this quickstart covers)
2. **Implement Payroll Factor** (similar to property factor)
3. **Implement Sales Factor** (more complex - requires transaction-level sourcing)
4. **Add Throwback Determination** (integrate with nexus tracking)
5. **Implement Service Sourcing** (market-based vs cost-of-performance)
6. **Build Frontend UI** (Schedule Y form with factor inputs)
7. **Integration Testing** (end-to-end apportionment calculation)
8. **Performance Testing** (10K transactions per return)

---

## Additional Resources

- **API Documentation:** [apportionment-api.yaml](./contracts/apportionment-api.yaml)
- **Data Model:** [data-model.md](./data-model.md)
- **Calculation Contracts:** [factor-calculation-contract.md](./contracts/factor-calculation-contract.md)
- **Research:** [research.md](./research.md)
- **Spec:** [spec.md](./spec.md)

---

## Getting Help

- **Slack:** #munitax-dev
- **Code Reviews:** Tag @tax-engine-team
- **Questions:** Open GitHub Discussion or ask in daily standup
