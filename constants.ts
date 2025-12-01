import { TaxRulesConfig, BusinessTaxRulesConfig, W2QualifyingWagesRule } from "./types";

// Gemini AI Configuration
// Use gemini-2.0-flash for stable document processing (1M token context, multimodal support)
// Alternative models: gemini-1.5-flash-latest, gemini-2.5-flash, gemini-2.5-pro
// See: https://ai.google.dev/gemini-api/docs/models
export const GEMINI_DEFAULT_MODEL = 'gemini-2.0-flash';

export const DEFAULT_TAX_RULES: TaxRulesConfig = {
    municipalRate: 0.020, // 2.0% Dublin
    municipalCreditLimitRate: 0.020, // 2.0% Cap
    municipalRates: {
        'columbus': 0.025, 'cleveland': 0.025, 'dublin': 0.020, 'westerville': 0.020,
        'hilliard': 0.025, 'upper arlington': 0.025, 'grandview': 0.025, 'bexley': 0.025
    },
    w2QualifyingWagesRule: W2QualifyingWagesRule.HIGHEST_OF_ALL,
    incomeInclusion: {
        scheduleC: true,
        scheduleE: true,
        scheduleF: true,
        w2g: true,
        form1099: true
    },
    enableRounding: true
};

export const DEFAULT_BUSINESS_RULES: BusinessTaxRulesConfig = {
    municipalRate: 0.020,
    minimumTax: 0,
    allocationMethod: '3_FACTOR',
    allocationSalesFactorWeight: 1, // Standard 3-factor (1/1/1)
    enableNOL: true,
    nolOffsetCapPercent: 0.50, // 50% Limit rule common in recent years
    intangibleExpenseRate: 0.05, // 5% Rule for intangible income expenses
    safeHarborPercent: 0.90, // 90% must be paid to avoid penalty
    penaltyRateLateFiling: 25.00,
    penaltyRateUnderpayment: 0.15, // 15% penalty
    interestRateAnnual: 0.07 // 7% annual
};
