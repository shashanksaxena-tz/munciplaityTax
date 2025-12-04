package com.munitax.extraction.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FormSchema {
    private String formType;
    private String version;
    private String description;
    private List<FormFieldSchema> fields = new ArrayList<>();

    public FormSchema() {
    }

    @JsonProperty("formType")
    public String getFormType() {
        return formType;
    }

    public void setFormType(String formType) {
        this.formType = formType;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("fields")
    public List<FormFieldSchema> getFields() {
        return fields;
    }

    public void setFields(List<FormFieldSchema> fields) {
        this.fields = fields;
    }
}
