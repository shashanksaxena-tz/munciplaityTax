package com.munitax.pdf.model;

public class TaxCalculationResult {
    private TaxReturnSettings settings;
    private TaxPayerProfile profile;
    private double totalGrossIncome;
    private double totalLocalWithheld;
    private double w2TaxableIncome;
    private ScheduleXResult scheduleX;
    private ScheduleYResult scheduleY;
    private double totalTaxableIncome;
    private double municipalLiability;
    private double municipalLiabilityAfterCredits;
    private double municipalBalance;

    // Constructors
    public TaxCalculationResult() {}

    // Getters and Setters
    public TaxReturnSettings getSettings() { return settings; }
    public void setSettings(TaxReturnSettings settings) { this.settings = settings; }

    public TaxPayerProfile getProfile() { return profile; }
    public void setProfile(TaxPayerProfile profile) { this.profile = profile; }

    public double getTotalGrossIncome() { return totalGrossIncome; }
    public void setTotalGrossIncome(double totalGrossIncome) { this.totalGrossIncome = totalGrossIncome; }

    public double getTotalLocalWithheld() { return totalLocalWithheld; }
    public void setTotalLocalWithheld(double totalLocalWithheld) { this.totalLocalWithheld = totalLocalWithheld; }

    public double getW2TaxableIncome() { return w2TaxableIncome; }
    public void setW2TaxableIncome(double w2TaxableIncome) { this.w2TaxableIncome = w2TaxableIncome; }

    public ScheduleXResult getScheduleX() { return scheduleX; }
    public void setScheduleX(ScheduleXResult scheduleX) { this.scheduleX = scheduleX; }

    public ScheduleYResult getScheduleY() { return scheduleY; }
    public void setScheduleY(ScheduleYResult scheduleY) { this.scheduleY = scheduleY; }

    public double getTotalTaxableIncome() { return totalTaxableIncome; }
    public void setTotalTaxableIncome(double totalTaxableIncome) { this.totalTaxableIncome = totalTaxableIncome; }

    public double getMunicipalLiability() { return municipalLiability; }
    public void setMunicipalLiability(double municipalLiability) { this.municipalLiability = municipalLiability; }

    public double getMunicipalLiabilityAfterCredits() { return municipalLiabilityAfterCredits; }
    public void setMunicipalLiabilityAfterCredits(double municipalLiabilityAfterCredits) { this.municipalLiabilityAfterCredits = municipalLiabilityAfterCredits; }

    public double getMunicipalBalance() { return municipalBalance; }
    public void setMunicipalBalance(double municipalBalance) { this.municipalBalance = municipalBalance; }

    // Nested classes
    public static class TaxReturnSettings {
        private int taxYear;
        private boolean isAmendment;
        private String amendmentReason;

        public int getTaxYear() { return taxYear; }
        public void setTaxYear(int taxYear) { this.taxYear = taxYear; }

        public boolean isAmendment() { return isAmendment; }
        public void setAmendment(boolean amendment) { isAmendment = amendment; }

        public String getAmendmentReason() { return amendmentReason; }
        public void setAmendmentReason(String amendmentReason) { this.amendmentReason = amendmentReason; }
    }

    public static class TaxPayerProfile {
        private String name;
        private String ssn;
        private Address address;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSsn() { return ssn; }
        public void setSsn(String ssn) { this.ssn = ssn; }

        public Address getAddress() { return address; }
        public void setAddress(Address address) { this.address = address; }
    }

    public static class Address {
        private String street;
        private String city;
        private String state;
        private String zip;

        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getZip() { return zip; }
        public void setZip(String zip) { this.zip = zip; }
    }

    public static class ScheduleXResult {
        private double totalNetProfit;

        public double getTotalNetProfit() { return totalNetProfit; }
        public void setTotalNetProfit(double totalNetProfit) { this.totalNetProfit = totalNetProfit; }
    }

    public static class ScheduleYResult {
        private double totalCredit;

        public double getTotalCredit() { return totalCredit; }
        public void setTotalCredit(double totalCredit) { this.totalCredit = totalCredit; }
    }
}
