package com.munitax.taxengine.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "formType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = W2Form.class, name = "W-2"),
        @JsonSubTypes.Type(value = W2GForm.class, name = "W-2G"),
        @JsonSubTypes.Type(value = Form1099.class, name = "1099-NEC"),
        @JsonSubTypes.Type(value = Form1099.class, name = "1099-MISC"),
        @JsonSubTypes.Type(value = ScheduleC.class, name = "Schedule C"),
        @JsonSubTypes.Type(value = ScheduleE.class, name = "Schedule E"),
        @JsonSubTypes.Type(value = ScheduleF.class, name = "Schedule F"),
        @JsonSubTypes.Type(value = LocalTaxForm.class, name = "Dublin 1040"),
        @JsonSubTypes.Type(value = LocalTaxForm.class, name = "Dublin 1040EZ"),
        @JsonSubTypes.Type(value = LocalTaxForm.class, name = "Form R"),
        @JsonSubTypes.Type(value = FederalTaxForm.class, name = "Federal 1040"),
        @JsonSubTypes.Type(value = BusinessFederalForm.class, name = "Federal 1120 (Corp)"),
        @JsonSubTypes.Type(value = BusinessFederalForm.class, name = "Federal 1065 (Partnership)"),
        @JsonSubTypes.Type(value = BusinessFederalForm.class, name = "Form 27 (Net Profits)")
})
public sealed interface TaxFormData permits
        W2Form,
        W2GForm,
        Form1099,
        ScheduleC,
        ScheduleE,
        ScheduleF,
        LocalTaxForm,
        FederalTaxForm,
        BusinessFederalForm {

    String id();

    String fileName();

    int taxYear();

    TaxFormType formType();

    Double confidenceScore();

    Map<String, Double> fieldConfidence();

    Integer sourcePage();

    String extractionReason();

    String owner(); // PRIMARY or SPOUSE
}
