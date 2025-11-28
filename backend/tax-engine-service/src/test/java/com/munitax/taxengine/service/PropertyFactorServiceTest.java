package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.PropertyFactor;
import com.munitax.taxengine.repository.PropertyFactorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PropertyFactorService.
 * Tests property factor calculation with averaging and rented property capitalization.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyFactorService Tests")
class PropertyFactorServiceTest {

    @Mock
    private PropertyFactorRepository propertyFactorRepository;

    @InjectMocks
    private PropertyFactorService propertyFactorService;

    private UUID scheduleYId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        scheduleYId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Calculate property factor with owned property only")
    void testCalculatePropertyFactorOwnedOnly() {
        // Given: Business owns $2M property in OH, $8M total
        BigDecimal ohioOwnedProperty = new BigDecimal("2000000");
        BigDecimal totalOwnedProperty = new BigDecimal("8000000");
        BigDecimal ohioRentedProperty = BigDecimal.ZERO;
        BigDecimal totalRentedProperty = BigDecimal.ZERO;

        // When: Calculate property factor percentage
        BigDecimal propertyFactor = propertyFactorService.calculatePropertyFactorPercentage(
                ohioOwnedProperty, totalOwnedProperty,
                ohioRentedProperty, totalRentedProperty);

        // Then: Property factor = $2M / $8M = 25%
        assertEquals(new BigDecimal("25.0000"), propertyFactor);
    }

    @Test
    @DisplayName("Calculate property factor with rented property capitalization (8x multiplier)")
    void testCalculatePropertyFactorWithRentedPropertyCapitalization() {
        // Given: Business owns $1M in OH + rents $100K/year, total $5M owned + $500K/year rent
        // Capitalization factor: 8x annual rent
        BigDecimal ohioOwnedProperty = new BigDecimal("1000000");
        BigDecimal totalOwnedProperty = new BigDecimal("5000000");
        BigDecimal ohioRentedProperty = new BigDecimal("100000");  // Annual rent
        BigDecimal totalRentedProperty = new BigDecimal("500000");  // Annual rent

        // When: Calculate property factor with capitalization
        BigDecimal propertyFactor = propertyFactorService.calculatePropertyFactorPercentage(
                ohioOwnedProperty, totalOwnedProperty,
                ohioRentedProperty, totalRentedProperty);

        // Then: 
        // Ohio: $1M owned + ($100K * 8) = $1.8M
        // Total: $5M owned + ($500K * 8) = $9M
        // Property factor = $1.8M / $9M = 20%
        assertEquals(new BigDecimal("20.0000"), propertyFactor);
    }

    @Test
    @DisplayName("Calculate property factor with averaging (beginning and end of year)")
    void testCalculatePropertyFactorWithAveraging() {
        // Given: Property values at beginning and end of year
        BigDecimal ohioPropertyBeginning = new BigDecimal("1000000");
        BigDecimal ohioPropertyEnd = new BigDecimal("3000000");
        BigDecimal totalPropertyBeginning = new BigDecimal("5000000");
        BigDecimal totalPropertyEnd = new BigDecimal("15000000");

        // When: Calculate averaged property factor
        BigDecimal ohioAverage = ohioPropertyBeginning.add(ohioPropertyEnd)
                .divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
        BigDecimal totalAverage = totalPropertyBeginning.add(totalPropertyEnd)
                .divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
        
        BigDecimal propertyFactor = propertyFactorService.calculatePropertyFactorPercentage(
                ohioAverage, totalAverage,
                BigDecimal.ZERO, BigDecimal.ZERO);

        // Then: 
        // Ohio average: ($1M + $3M) / 2 = $2M
        // Total average: ($5M + $15M) / 2 = $10M
        // Property factor = $2M / $10M = 20%
        assertEquals(new BigDecimal("20.0000"), propertyFactor);
    }

    @Test
    @DisplayName("Handle zero property (service-only business)")
    void testHandleZeroProperty() {
        // Given: Service business with no property
        BigDecimal ohioProperty = BigDecimal.ZERO;
        BigDecimal totalProperty = BigDecimal.ZERO;

        // When: Calculate property factor
        BigDecimal propertyFactor = propertyFactorService.calculatePropertyFactorPercentage(
                ohioProperty, totalProperty,
                BigDecimal.ZERO, BigDecimal.ZERO);

        // Then: Should return 0% (not throw exception)
        assertEquals(BigDecimal.ZERO, propertyFactor);
    }

    @Test
    @DisplayName("Validate property values are non-negative")
    void testValidateNonNegativePropertyValues() {
        // Given: Negative property value (invalid)
        BigDecimal negativeProperty = new BigDecimal("-1000");

        // When/Then: Should throw validation exception
        assertThrows(IllegalArgumentException.class, () -> {
            propertyFactorService.validatePropertyValue(negativeProperty, "Ohio Property");
        });
    }

    @Test
    @DisplayName("Calculate property factor with mixed owned and rented property")
    void testCalculatePropertyFactorMixedOwnedAndRented() {
        // Given: Complex scenario with both owned and rented property
        BigDecimal ohioOwnedProperty = new BigDecimal("500000");
        BigDecimal totalOwnedProperty = new BigDecimal("2000000");
        BigDecimal ohioRentedProperty = new BigDecimal("50000");   // $50K annual rent
        BigDecimal totalRentedProperty = new BigDecimal("200000"); // $200K annual rent

        // When: Calculate property factor
        BigDecimal propertyFactor = propertyFactorService.calculatePropertyFactorPercentage(
                ohioOwnedProperty, totalOwnedProperty,
                ohioRentedProperty, totalRentedProperty);

        // Then:
        // Ohio: $500K owned + ($50K * 8) = $900K
        // Total: $2M owned + ($200K * 8) = $3.6M
        // Property factor = $900K / $3.6M = 25%
        assertEquals(new BigDecimal("25.0000"), propertyFactor);
    }

    @Test
    @DisplayName("Round property factor to 4 decimal places")
    void testRoundPropertyFactorTo4Decimals() {
        // Given: Property values that result in repeating decimal
        BigDecimal ohioProperty = new BigDecimal("1000000");
        BigDecimal totalProperty = new BigDecimal("3000000");

        // When: Calculate property factor
        BigDecimal propertyFactor = propertyFactorService.calculatePropertyFactorPercentage(
                ohioProperty, totalProperty,
                BigDecimal.ZERO, BigDecimal.ZERO);

        // Then: Should be 33.3333% (not 33.333333...)
        assertEquals(new BigDecimal("33.3333"), propertyFactor);
        assertEquals(4, propertyFactor.scale());
    }

    @Test
    @DisplayName("Save property factor to database")
    void testSavePropertyFactor() {
        // Given: PropertyFactor entity
        PropertyFactor propertyFactor = new PropertyFactor();
        propertyFactor.setScheduleYId(scheduleYId);
        propertyFactor.setTenantId(tenantId);
        propertyFactor.setOhioOwnedPropertyValue(new BigDecimal("2000000"));
        propertyFactor.setTotalOwnedPropertyValue(new BigDecimal("8000000"));
        propertyFactor.setPropertyFactorPercentage(new BigDecimal("25.0000"));

        when(propertyFactorRepository.save(any(PropertyFactor.class)))
                .thenReturn(propertyFactor);

        // When: Save property factor
        PropertyFactor saved = propertyFactorService.savePropertyFactor(propertyFactor);

        // Then: Should save and return entity
        assertNotNull(saved);
        assertEquals(new BigDecimal("25.0000"), saved.getPropertyFactorPercentage());
        verify(propertyFactorRepository, times(1)).save(any(PropertyFactor.class));
    }

    @Test
    @DisplayName("Calculate property factor exceeding 100% throws exception")
    void testPropertyFactorExceeding100Percent() {
        // Given: Ohio property exceeds total (data error)
        BigDecimal ohioProperty = new BigDecimal("10000000");
        BigDecimal totalProperty = new BigDecimal("5000000");

        // When/Then: Should throw validation exception
        assertThrows(IllegalArgumentException.class, () -> {
            propertyFactorService.calculatePropertyFactorPercentage(
                    ohioProperty, totalProperty,
                    BigDecimal.ZERO, BigDecimal.ZERO);
        });
    }

    @Test
    @DisplayName("Apply rented property capitalization with 8x multiplier")
    void testRentedPropertyCapitalizationMultiplier() {
        // Given: Annual rent of $100K
        BigDecimal annualRent = new BigDecimal("100000");

        // When: Calculate capitalized value
        BigDecimal capitalized = propertyFactorService.capitalizeRentedProperty(annualRent);

        // Then: Should be $800K (8x multiplier)
        assertEquals(new BigDecimal("800000"), capitalized);
    }
}
