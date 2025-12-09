package com.munitax.taxengine.dto;

import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails;

import java.util.List;
import java.util.Map;

/**
 * DTOs for Schedule X API endpoints (T022)
 */

/**
 * DTO for BusinessScheduleXDetails (matches frontend TypeScript interface)
 */
public record BusinessScheduleXDetailsDto(
    Double fedTaxableIncome,
    AddBacksDto addBacks,
    DeductionsDto deductions,
    CalculatedFieldsDto calculatedFields,
    MetadataDto metadata
) {
    public record AddBacksDto(
        Double depreciationAdjustment,
        Double amortizationAdjustment,
        Double interestAndStateTaxes,
        Double guaranteedPayments,
        Double mealsAndEntertainment,
        Double relatedPartyExcess,
        Double penaltiesAndFines,
        Double politicalContributions,
        Double officerLifeInsurance,
        Double capitalLossExcess,
        Double federalTaxRefunds,
        Double expensesOnIntangibleIncome,
        Double section179Excess,
        Double bonusDepreciation,
        Double badDebtReserveIncrease,
        Double charitableContributionExcess,
        Double domesticProductionActivities,
        Double stockCompensationAdjustment,
        Double inventoryMethodChange,
        Double clubDues,
        Double pensionProfitSharingLimits,
        Double otherAddBacks,
        String otherAddBacksDescription
    ) {}
    
    public record DeductionsDto(
        Double interestIncome,
        Double dividends,
        Double capitalGains,
        Double section179Recapture,
        Double municipalBondInterest,
        Double depletionDifference,
        Double otherDeductions,
        String otherDeductionsDescription
    ) {}
    
    public record CalculatedFieldsDto(
        Double totalAddBacks,
        Double totalDeductions,
        Double adjustedMunicipalIncome
    ) {}
    
    public record MetadataDto(
        String lastModified,
        List<String> autoCalculatedFields,
        List<String> manualOverrides,
        List<AttachedDocumentDto> attachedDocuments
    ) {
        public record AttachedDocumentDto(
            String fileName,
            String fileUrl,
            String fieldName,
            String uploadedAt,
            String uploadedBy
        ) {}
    }
    
    /**
     * Convert from domain model to DTO
     */
    public static BusinessScheduleXDetailsDto fromDomain(BusinessScheduleXDetails domain) {
        if (domain == null) {
            return null;
        }
        
        return new BusinessScheduleXDetailsDto(
            domain.fedTaxableIncome(),
            fromDomainAddBacks(domain.addBacks()),
            fromDomainDeductions(domain.deductions()),
            fromDomainCalculatedFields(domain.calculatedFields()),
            fromDomainMetadata(domain.metadata())
        );
    }
    
    private static AddBacksDto fromDomainAddBacks(BusinessScheduleXDetails.AddBacks addBacks) {
        if (addBacks == null) {
            return null;
        }
        
        return new AddBacksDto(
            addBacks.depreciationAdjustment(),
            addBacks.amortizationAdjustment(),
            addBacks.interestAndStateTaxes(),
            addBacks.guaranteedPayments(),
            addBacks.mealsAndEntertainment(),
            addBacks.relatedPartyExcess(),
            addBacks.penaltiesAndFines(),
            addBacks.politicalContributions(),
            addBacks.officerLifeInsurance(),
            addBacks.capitalLossExcess(),
            addBacks.federalTaxRefunds(),
            addBacks.expensesOnIntangibleIncome(),
            addBacks.section179Excess(),
            addBacks.bonusDepreciation(),
            addBacks.badDebtReserveIncrease(),
            addBacks.charitableContributionExcess(),
            addBacks.domesticProductionActivities(),
            addBacks.stockCompensationAdjustment(),
            addBacks.inventoryMethodChange(),
            addBacks.clubDues(),
            addBacks.pensionProfitSharingLimits(),
            addBacks.otherAddBacks(),
            addBacks.otherAddBacksDescription()
        );
    }
    
    private static DeductionsDto fromDomainDeductions(BusinessScheduleXDetails.Deductions deductions) {
        if (deductions == null) {
            return null;
        }
        
        return new DeductionsDto(
            deductions.interestIncome(),
            deductions.dividends(),
            deductions.capitalGains(),
            deductions.section179Recapture(),
            deductions.municipalBondInterest(),
            deductions.depletionDifference(),
            deductions.otherDeductions(),
            deductions.otherDeductionsDescription()
        );
    }
    
    private static CalculatedFieldsDto fromDomainCalculatedFields(BusinessScheduleXDetails.CalculatedFields calculatedFields) {
        if (calculatedFields == null) {
            return null;
        }
        
        return new CalculatedFieldsDto(
            calculatedFields.totalAddBacks(),
            calculatedFields.totalDeductions(),
            calculatedFields.adjustedMunicipalIncome()
        );
    }
    
    private static MetadataDto fromDomainMetadata(BusinessScheduleXDetails.Metadata metadata) {
        if (metadata == null) {
            return null;
        }
        
        List<MetadataDto.AttachedDocumentDto> attachedDocs = metadata.attachedDocuments().stream()
            .map(doc -> new MetadataDto.AttachedDocumentDto(
                doc.fileName(),
                doc.fileUrl(),
                doc.fieldName(),
                doc.uploadedAt(),
                doc.uploadedBy()
            ))
            .toList();
        
        return new MetadataDto(
            metadata.lastModified(),
            metadata.autoCalculatedFields(),
            metadata.manualOverrides(),
            attachedDocs
        );
    }
}
