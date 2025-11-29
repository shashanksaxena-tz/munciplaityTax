package com.munitax.taxengine.domain.apportionment;

/**
 * Reason(s) why nexus is established in a state/municipality.
 * Multiple reasons can apply simultaneously.
 *
 * @see <a href="https://www.supremecourt.gov/opinions/17pdf/17-494_j4el.pdf">South Dakota v. Wayfair (2018)</a>
 */
public enum NexusReason {
    
    /**
     * Physical Presence Nexus
     * Business has office, warehouse, property, or other physical presence in state.
     * Traditional nexus standard (pre-Wayfair).
     * 
     * Example: Business owns warehouse in OH → OH nexus
     */
    PHYSICAL_PRESENCE("Physical Presence - Office, warehouse, or property in state"),
    
    /**
     * Employee Presence Nexus
     * Business has employees working in state.
     * 
     * Example: Business has 5 employees working from home in OH → OH nexus
     */
    EMPLOYEE_PRESENCE("Employee Presence - Employees working in state"),
    
    /**
     * Economic Nexus
     * Business exceeds sales or transaction threshold in state.
     * Post-Wayfair standard (2018+).
     * 
     * Common thresholds:
     * - $500,000 sales in state (OH, CA, TX, IL)
     * - $100,000 sales in state (NY, MA)
     * - 200 transactions in state (some states)
     * 
     * Example: Online retailer has $600K sales to OH customers → OH nexus
     */
    ECONOMIC_NEXUS("Economic Nexus - Sales exceed state threshold (post-Wayfair)"),
    
    /**
     * Factor Presence Nexus
     * Business has substantial property, payroll, or sales in state.
     * P.L. 86-272 does not protect from nexus if non-solicitation activities occur.
     * 
     * Example: Business has $1M property and $500K payroll in OH → OH nexus
     */
    FACTOR_PRESENCE("Factor Presence - Substantial property, payroll, or sales in state"),
    
    /**
     * Affiliate Nexus
     * Business has nexus through related entity's presence in state.
     * Also called "click-through nexus" or "agency nexus".
     * 
     * Example: Parent has no OH presence, but subsidiary has OH warehouse → Parent may have OH nexus
     */
    AFFILIATE_NEXUS("Affiliate Nexus - Related entity has presence in state"),
    
    /**
     * Marketplace Facilitator Nexus
     * Business facilitated by marketplace (Amazon, eBay) has nexus through facilitator.
     * 
     * Example: Third-party seller on Amazon, Amazon has OH nexus → Seller may have OH nexus
     */
    MARKETPLACE_FACILITATOR("Marketplace Facilitator - Nexus through platform");
    
    private final String description;
    
    NexusReason(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines if this nexus type is physical (office, employees, property).
     */
    public boolean isPhysicalNexus() {
        return this == PHYSICAL_PRESENCE || this == EMPLOYEE_PRESENCE;
    }
    
    /**
     * Determines if this nexus type is economic (sales/transaction threshold).
     */
    public boolean isEconomicNexus() {
        return this == ECONOMIC_NEXUS;
    }
}
