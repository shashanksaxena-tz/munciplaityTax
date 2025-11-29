package com.munitax.taxengine.domain.apportionment;

/**
 * Service revenue sourcing method: Market-Based vs Cost-of-Performance.
 * Determines how service revenue is allocated to states.
 *
 * @see <a href="https://www.mtc.gov/uniformity/project-teams/market-based-sourcing">MTC Market-Based Sourcing</a>
 */
public enum ServiceSourcingMethod {
    
    /**
     * Market-Based Sourcing (Modern Rule - Post 2010)
     * Source service revenue to where the customer receives the benefit.
     * 
     * Used by: OH, CA, TX, IL, and 30+ states (modern trend).
     * 
     * Example: IT consulting firm (OH office) provides $1M project to NY customer.
     * Service revenue sourced to: NY (where customer receives benefit)
     * 
     * Rationale: Tax where economic activity occurs (customer location),
     * not where labor is performed.
     */
    MARKET_BASED("Market-Based - Customer location (where benefit received)"),
    
    /**
     * Cost-of-Performance (Historical Rule - Pre 2010)
     * Source service revenue to where employees perform the work.
     * 
     * Used by: Declining number of states, historical approach.
     * 
     * Example: IT consulting firm (OH: 5 employees, CA: 2 employees) provides $1M project to NY customer.
     * Service revenue sourced by payroll: 70% OH ($700K), 30% CA ($300K), 0% NY
     * 
     * Rationale: Tax where income-producing activity occurs (employee location).
     */
    COST_OF_PERFORMANCE("Cost-of-Performance - Employee location (where work performed)");
    
    private final String description;
    
    ServiceSourcingMethod(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines if service revenue should be sourced to customer location.
     */
    public boolean isMarketBased() {
        return this == MARKET_BASED;
    }
    
    /**
     * Determines if service revenue should be prorated by employee location.
     */
    public boolean isCostOfPerformance() {
        return this == COST_OF_PERFORMANCE;
    }
}
