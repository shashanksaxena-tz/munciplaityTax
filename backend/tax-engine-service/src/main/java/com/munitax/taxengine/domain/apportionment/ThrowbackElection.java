package com.munitax.taxengine.domain.apportionment;

/**
 * Throwback/Throwout rule election for sales to states without nexus.
 * Prevents "nowhere income" that escapes taxation in all states.
 *
 * @see <a href="https://www.mtc.gov/uniformity/project-teams/throwback-rule">MTC Throwback Rule</a>
 */
public enum ThrowbackElection {
    
    /**
     * Throwback Rule (Most Common)
     * If business ships goods to State B where it lacks nexus,
     * "throw back" the sale to the origin state (State A).
     * Add to numerator, keep in denominator.
     * 
     * Used by: OH, CA, IL, and ~25 states.
     * 
     * Example: Ship $100K from OH to CA, no CA nexus.
     * OH sales numerator: +$100K (thrown back)
     * Total sales denominator: $100K (kept in)
     */
    THROWBACK("Throwback - Add to origin state numerator"),
    
    /**
     * Throwout Rule (Alternative)
     * If business ships goods to State B where it lacks nexus,
     * "throw out" the sale entirely.
     * Exclude from both numerator and denominator.
     * 
     * Used by: PA, IL (option), and some municipalities.
     * 
     * Example: Ship $100K from OH to CA, no CA nexus.
     * OH sales numerator: $0 (not thrown back)
     * Total sales denominator: $0 (thrown out)
     */
    THROWOUT("Throwout - Exclude from both numerator and denominator"),
    
    /**
     * No Throwback/Throwout
     * Business may ship to states without nexus, but no adjustment made.
     * Results in "nowhere income" (not taxed by any state).
     * 
     * Used by: States without throwback rules (minority).
     */
    NONE("No Throwback/Throwout - No adjustment");
    
    private final String description;
    
    ThrowbackElection(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines if sales to no-nexus states should be added to numerator.
     */
    public boolean shouldThrowbackToOrigin() {
        return this == THROWBACK;
    }
    
    /**
     * Determines if sales to no-nexus states should be excluded from denominator.
     */
    public boolean shouldThrowoutFromDenominator() {
        return this == THROWOUT;
    }
}
