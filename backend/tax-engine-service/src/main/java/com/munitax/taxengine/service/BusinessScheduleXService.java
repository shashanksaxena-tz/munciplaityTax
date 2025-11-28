package com.munitax.taxengine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails.AddBacks;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails.Deductions;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails.Metadata;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for Business Schedule X operations including backward compatibility conversion (T005, Research R2).
 * 
 * Handles:
 * - Detection of old 6-field format vs new 27-field format
 * - Runtime conversion from old format to new nested structure
 * - Preservation of data during migration
 */
@Service
public class BusinessScheduleXService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Convert old 6-field format to new 27-field format (Research R2 runtime conversion)
     * 
     * Old format:
     * {
     *   "fedTaxableIncome": 500000,
     *   "incomeAndStateTaxes": 10000,
     *   "interestIncome": 5000,
     *   "dividends": 3000,
     *   "capitalGains": 2000,
     *   "other": 0
     * }
     * 
     * New format:
     * {
     *   "fedTaxableIncome": 500000,
     *   "addBacks": { "interestAndStateTaxes": 10000, ... },
     *   "deductions": { "interestIncome": 5000, "dividends": 3000, "capitalGains": 2000, ... },
     *   "calculatedFields": {...},
     *   "metadata": {...}
     * }
     *
     * @param json JSONB data from database
     * @return Converted BusinessScheduleXDetails in new 27-field format
     */
    public BusinessScheduleXDetails convertFromOldFormat(JsonNode json) {
        if (json == null) {
            return null;
        }
        
        // Detect old format: has top-level interestIncome without nested deductions object
        boolean isOldFormat = json.has("interestIncome") && !json.has("deductions");
        
        if (!isOldFormat) {
            // Already in new format, parse normally
            try {
                return objectMapper.treeToValue(json, BusinessScheduleXDetails.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Schedule X details", e);
            }
        }
        
        // Old format detected - convert to new structure
        Double fedTaxableIncome = json.has("fedTaxableIncome") ? 
            json.get("fedTaxableIncome").asDouble() : 0.0;
        
        // Migrate old fields to new nested structure
        AddBacks addBacks = new AddBacks(
            // Old field: incomeAndStateTaxes → addBacks.interestAndStateTaxes
            json.has("incomeAndStateTaxes") ? json.get("incomeAndStateTaxes").asDouble() : 0.0,
            0.0, // guaranteedPayments
            0.0, // expensesOnIntangibleIncome
            0.0, // depreciationAdjustment
            0.0, // amortizationAdjustment
            0.0, // mealsAndEntertainment
            0.0, // relatedPartyExcess
            0.0, // penaltiesAndFines
            0.0, // politicalContributions
            0.0, // officerLifeInsurance
            0.0, // capitalLossExcess
            0.0, // federalTaxRefunds
            0.0, // section179Excess
            0.0, // bonusDepreciation
            0.0, // badDebtReserveIncrease
            0.0, // charitableContributionExcess
            0.0, // domesticProductionActivities
            0.0, // stockCompensationAdjustment
            0.0, // inventoryMethodChange
            // Old field: "other" → addBacks.otherAddBacks (if positive)
            (json.has("other") && json.get("other").asDouble() > 0) ? json.get("other").asDouble() : 0.0,
            null, // otherAddBacksDescription
            0.0  // wagesCredit (deprecated)
        );
        
        Deductions deductions = new Deductions(
            // Old fields: migrate to deductions object
            json.has("interestIncome") ? json.get("interestIncome").asDouble() : 0.0,
            json.has("dividends") ? json.get("dividends").asDouble() : 0.0,
            json.has("capitalGains") ? json.get("capitalGains").asDouble() : 0.0,
            0.0, // section179Excess (deprecated in deductions, now in addBacks)
            // Old field: "other" → deductions.otherDeductions (if negative or zero)
            (json.has("other") && json.get("other").asDouble() <= 0) ? Math.abs(json.get("other").asDouble()) : 0.0,
            0.0, // section179Recapture
            0.0, // municipalBondInterest
            0.0, // depletionDifference
            null // otherDeductionsDescription
        );
        
        // Calculate totals
        Double totalAddBacks = addBacks.interestAndStateTaxes() + addBacks.otherAddBacks();
        Double totalDeductions = deductions.interestIncome() + deductions.dividends() + 
                                deductions.capitalGains() + deductions.otherDeductions();
        Double adjustedIncome = fedTaxableIncome + totalAddBacks - totalDeductions;
        
        BusinessScheduleXDetails.CalculatedFields calculatedFields = 
            new BusinessScheduleXDetails.CalculatedFields(totalAddBacks, totalDeductions, adjustedIncome);
        
        // Create metadata indicating this was converted from old format
        Metadata metadata = new Metadata(
            Instant.now().toString(),
            List.of(), // autoCalculatedFields
            List.of(), // manualOverrides
            List.of()  // attachedDocuments
        );
        
        return new BusinessScheduleXDetails(
            fedTaxableIncome,
            addBacks,
            deductions,
            calculatedFields,
            metadata
        );
    }
    
    /**
     * Detect if JSONB data is in old 6-field format
     *
     * @param json JSONB data from database
     * @return true if old format, false if new format
     */
    public boolean isOldFormat(JsonNode json) {
        if (json == null) {
            return false;
        }
        
        // Old format has top-level interestIncome without nested deductions object
        return json.has("interestIncome") && !json.has("deductions");
    }
    
    /**
     * Convert old format string (JSON) to new BusinessScheduleXDetails
     *
     * @param jsonString JSON string from database
     * @return Converted BusinessScheduleXDetails
     */
    public BusinessScheduleXDetails convertFromOldFormatString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        
        try {
            JsonNode json = objectMapper.readTree(jsonString);
            return convertFromOldFormat(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Schedule X JSON", e);
        }
    }
    
    /**
     * Save BusinessScheduleXDetails (always in new 27-field format)
     * This method ensures we always save in the new format, even if input was old format
     *
     * @param scheduleX Schedule X details to save
     * @return JSON string representation
     */
    public String toJsonString(BusinessScheduleXDetails scheduleX) {
        if (scheduleX == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(scheduleX);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Schedule X to JSON", e);
        }
    }
}
