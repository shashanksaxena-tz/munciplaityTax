package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.PropertyFactor;
import com.munitax.taxengine.domain.apportionment.ScheduleY;
import com.munitax.taxengine.repository.PropertyFactorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Service for calculating property factor for Schedule Y apportionment.
 * Implements property factor calculation with:
 * - Averaging (beginning and end of year)
 * - Rented property capitalization (8x annual rent)
 * - Property factor percentage = (OH property / Total property) * 100
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyFactorService {

    private final PropertyFactorRepository propertyFactorRepository;

    /**
     * Capitalization multiplier for annual rent (per Ohio tax rules).
     * Annual rent is multiplied by 8 to convert to property value equivalent.
     */
    private static final BigDecimal RENT_CAPITALIZATION_MULTIPLIER = new BigDecimal("8");

    /**
     * Scale for percentage calculations (4 decimal places).
     */
    private static final int PERCENTAGE_SCALE = 4;

    /**
     * Calculate property factor percentage with owned and rented property.
     *
     * @param ohioOwnedProperty      Ohio owned property value
     * @param totalOwnedProperty     Total owned property value (all states)
     * @param ohioRentedProperty     Ohio annual rent expense
     * @param totalRentedProperty    Total annual rent expense (all states)
     * @return Property factor percentage (0-100%)
     */
    public BigDecimal calculatePropertyFactorPercentage(
            BigDecimal ohioOwnedProperty,
            BigDecimal totalOwnedProperty,
            BigDecimal ohioRentedProperty,
            BigDecimal totalRentedProperty) {

        log.debug("Calculating property factor: OH owned={}, Total owned={}, OH rent={}, Total rent={}",
                ohioOwnedProperty, totalOwnedProperty, ohioRentedProperty, totalRentedProperty);

        // Validate inputs
        validatePropertyValue(ohioOwnedProperty, "Ohio Owned Property");
        validatePropertyValue(totalOwnedProperty, "Total Owned Property");
        validatePropertyValue(ohioRentedProperty, "Ohio Rented Property");
        validatePropertyValue(totalRentedProperty, "Total Rented Property");

        // Capitalize rented property
        BigDecimal ohioRentedCapitalized = capitalizeRentedProperty(ohioRentedProperty);
        BigDecimal totalRentedCapitalized = capitalizeRentedProperty(totalRentedProperty);

        // Calculate total property values (owned + capitalized rent)
        BigDecimal ohioTotalProperty = ohioOwnedProperty.add(ohioRentedCapitalized);
        BigDecimal totalProperty = totalOwnedProperty.add(totalRentedCapitalized);

        // Handle zero denominator
        if (totalProperty.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Total property is zero, returning 0% factor");
            return BigDecimal.ZERO;
        }

        // Calculate percentage: (Ohio / Total) * 100
        BigDecimal percentage = ohioTotalProperty
                .multiply(new BigDecimal("100"))
                .divide(totalProperty, PERCENTAGE_SCALE, RoundingMode.HALF_UP);

        // Validate result is within 0-100%
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "Property factor percentage must be between 0 and 100, got: " + percentage);
        }

        log.debug("Calculated property factor: {}%", percentage);
        return percentage;
    }

    /**
     * Capitalize annual rent to property value equivalent.
     * Per Ohio tax rules, annual rent is multiplied by 8.
     *
     * @param annualRent Annual rent expense
     * @return Capitalized property value
     */
    public BigDecimal capitalizeRentedProperty(BigDecimal annualRent) {
        if (annualRent == null || annualRent.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return annualRent.multiply(RENT_CAPITALIZATION_MULTIPLIER);
    }

    /**
     * Validate property value is non-negative.
     *
     * @param value     Property value to validate
     * @param fieldName Name of the field for error message
     */
    public void validatePropertyValue(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be negative, got: " + value);
        }
    }

    /**
     * Save property factor to database.
     * Note: Entity calculates totals and percentage automatically via @PrePersist/@PreUpdate.
     *
     * @param propertyFactor PropertyFactor entity
     * @return Saved PropertyFactor
     */
    @Transactional
    public PropertyFactor savePropertyFactor(PropertyFactor propertyFactor) {
        log.info("Saving property factor for scheduleYId={}",
                propertyFactor.getScheduleY() != null ? propertyFactor.getScheduleY().getScheduleYId() : "null");

        // Entity automatically calculates totals and percentage via @PrePersist
        return propertyFactorRepository.save(propertyFactor);
    }

    /**
     * Calculate averaged property factor (beginning and end of year).
     * Per Ohio tax rules, property is averaged over the tax year.
     *
     * @param beginningValue Property value at beginning of year
     * @param endValue       Property value at end of year
     * @return Averaged property value
     */
    public BigDecimal averagePropertyValue(BigDecimal beginningValue, BigDecimal endValue) {
        if (beginningValue == null || endValue == null) {
            throw new IllegalArgumentException("Property values for averaging cannot be null");
        }

        return beginningValue.add(endValue)
                .divide(new BigDecimal("2"), PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Get property factor by Schedule Y ID.
     *
     * @param scheduleYId Schedule Y ID
     * @return PropertyFactor or null if not found
     */
    public PropertyFactor getPropertyFactorByScheduleYId(UUID scheduleYId) {
        return propertyFactorRepository.findByScheduleY_ScheduleYId(scheduleYId).orElse(null);
    }

    /**
     * Create property factor entity from Schedule Y relationship.
     *
     * @param scheduleY ScheduleY entity
     * @param ohioRealProperty Ohio real property value
     * @param ohioPersonalProperty Ohio personal property value
     * @param ohioRentedPropertyRent Ohio annual rent
     * @param totalPropertyEverywhere Total property everywhere
     * @return PropertyFactor entity
     */
    public PropertyFactor createPropertyFactor(ScheduleY scheduleY,
                                              BigDecimal ohioRealProperty,
                                              BigDecimal ohioPersonalProperty,
                                              BigDecimal ohioRentedPropertyRent,
                                              BigDecimal totalPropertyEverywhere) {
        PropertyFactor propertyFactor = new PropertyFactor();
        propertyFactor.setScheduleY(scheduleY);
        propertyFactor.setOhioRealProperty(ohioRealProperty != null ? ohioRealProperty : BigDecimal.ZERO);
        propertyFactor.setOhioTangiblePersonalProperty(ohioPersonalProperty != null ? ohioPersonalProperty : BigDecimal.ZERO);
        propertyFactor.setOhioRentedPropertyRent(ohioRentedPropertyRent != null ? ohioRentedPropertyRent : BigDecimal.ZERO);
        propertyFactor.setTotalPropertyEverywhere(totalPropertyEverywhere);
        
        // Entity will auto-calculate totals and percentage
        return propertyFactor;
    }
}
