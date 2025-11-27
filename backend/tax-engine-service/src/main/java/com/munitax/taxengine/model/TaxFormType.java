package com.munitax.taxengine.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TaxFormType {
    W2("W-2"),
    FORM_1099_NEC("1099-NEC"),
    FORM_1099_MISC("1099-MISC"),
    W2G("W-2G"),
    SCHEDULE_C("Schedule C"),
    SCHEDULE_E("Schedule E"),
    SCHEDULE_F("Schedule F"),
    LOCAL_1040("Dublin 1040"),
    LOCAL_1040_EZ("Dublin 1040EZ"),
    FORM_R("Form R"),
    FEDERAL_1040("Federal 1040"),

    // Business Specific
    FORM_W1("Form W-1 (Withholding)"),
    FORM_W3("Form W-3 (Reconciliation)"),
    FORM_27("Form 27 (Net Profits)"),
    FORM_1120("Federal 1120 (Corp)"),
    FORM_1065("Federal 1065 (Partnership)"),
    PAYROLL_SUMMARY("Payroll Summary");

    private final String label;

    TaxFormType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
