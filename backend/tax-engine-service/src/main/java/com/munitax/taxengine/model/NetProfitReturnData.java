package com.munitax.taxengine.model;

import com.munitax.taxengine.model.BusinessFederalForm.BusinessAllocation;
import com.munitax.taxengine.model.BusinessFederalForm.BusinessScheduleXDetails;

public record NetProfitReturnData(
    String id,
    String dateFiled,
    int taxYear,
    
    BusinessScheduleXDetails reconciliation,
    BusinessAllocation allocation,
    
    Double adjustedFedTaxableIncome,
    Double allocatedTaxableIncome,
    
    Double nolAvailable,
    Double nolApplied,
    Double taxableIncomeAfterNOL,
    
    Double taxDue,
    Double estimatedPayments,
    Double priorYearCredit,
    
    Double penaltyUnderpayment,
    Double penaltyLateFiling,
    Double interest,
    
    Double balanceDue,
    String paymentStatus // PAID, UNPAID
) {}
