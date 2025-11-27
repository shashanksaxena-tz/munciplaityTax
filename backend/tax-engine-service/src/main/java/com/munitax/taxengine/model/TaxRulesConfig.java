package com.munitax.taxengine.model;

import java.util.Map;

public record TaxRulesConfig(
    double municipalRate,
    double municipalCreditLimitRate,
    Map<String, Double> municipalRates,
    W2QualifyingWagesRule w2QualifyingWagesRule,
    IncomeInclusion incomeInclusion,
    boolean enableRounding
) {
    public enum W2QualifyingWagesRule {
        HIGHEST_OF_ALL,
        BOX_5_MEDICARE,
        BOX_18_LOCAL,
        BOX_1_FEDERAL
    }

    public record IncomeInclusion(
        boolean scheduleC,
        boolean scheduleE,
        boolean scheduleF,
        boolean w2g,
        boolean form1099
    ) {}
}
