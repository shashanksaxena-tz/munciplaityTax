package com.munitax.taxengine.model;

public record BusinessTaxRulesConfig(
        double municipalRate,
        double minimumTax,
        String allocationMethod, // 3_FACTOR, GROSS_RECEIPTS_ONLY
        double allocationSalesFactorWeight,
        boolean enableNOL,
        double nolOffsetCapPercent,
        double intangibleExpenseRate,
        double safeHarborPercent,
        double penaltyRateLateFiling,
        double penaltyRateUnderpayment,
        double interestRateAnnual) {
}
