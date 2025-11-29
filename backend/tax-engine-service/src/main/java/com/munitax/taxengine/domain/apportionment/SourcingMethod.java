package com.munitax.taxengine.domain.apportionment;

/**
 * Sourcing method applied to individual sale transactions.
 * Indicates how the sale was allocated to a state.
 */
public enum SourcingMethod {
    
    /**
     * Destination-based sourcing (tangible goods)
     * Sale allocated to state where goods are delivered.
     */
    DESTINATION("Destination - Where goods delivered"),
    
    /**
     * Market-based sourcing (services)
     * Sale allocated to state where customer receives benefit.
     */
    MARKET_BASED("Market-Based - Where customer receives benefit"),
    
    /**
     * Cost-of-performance sourcing (services)
     * Sale allocated to state(s) where employees performed work.
     */
    COST_OF_PERFORMANCE("Cost-of-Performance - Where work performed"),
    
    /**
     * Throwback sourcing
     * Sale thrown back to origin state (no nexus in destination).
     */
    THROWBACK("Throwback - Returned to origin state"),
    
    /**
     * Throwout sourcing
     * Sale excluded from both numerator and denominator.
     */
    THROWOUT("Throwout - Excluded from calculation"),
    
    /**
     * Pro-rata sourcing (fallback)
     * Sale allocated proportionally across states based on overall apportionment.
     * Used when customer location and employee location are both unknown.
     */
    PRO_RATA("Pro-Rata - Allocated by overall apportionment percentage");
    
    private final String description;
    
    SourcingMethod(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines if this sourcing method requires nexus check.
     */
    public boolean requiresNexusCheck() {
        return this == DESTINATION || this == MARKET_BASED;
    }
    
    /**
     * Determines if this is a fallback/default sourcing method.
     */
    public boolean isFallback() {
        return this == PRO_RATA;
    }
}
