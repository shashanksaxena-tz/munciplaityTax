package com.munitax.taxengine.domain.apportionment;

/**
 * Type of sale transaction for sourcing determination.
 * Different sale types have different sourcing rules.
 */
public enum SaleType {
    
    /**
     * Tangible goods (physical products)
     * Sourced to destination (where goods are delivered).
     * Example: Ship widgets from OH warehouse to CA customer → CA sale
     */
    TANGIBLE_GOODS("Tangible Goods"),
    
    /**
     * Services (consulting, IT, legal, accounting, etc.)
     * Sourced using market-based (customer location) or cost-of-performance (employee location).
     * Example: IT consulting provided to NY customer → Market-based: NY, Cost-of-performance: OH (if employees in OH)
     */
    SERVICES("Services"),
    
    /**
     * Rental income (property leases)
     * Sourced to where property is located.
     * Example: Lease office building in OH → OH rental income
     */
    RENTAL_INCOME("Rental Income"),
    
    /**
     * Interest income
     * Sourced to borrower location (market-based).
     * Example: Loan to NY business → NY interest income
     */
    INTEREST("Interest Income"),
    
    /**
     * Royalty income (licensing intellectual property)
     * Sourced to where IP is used.
     * Example: Software license used by CA business → CA royalty income
     */
    ROYALTIES("Royalties"),
    
    /**
     * Other income (dividends, capital gains, etc.)
     * Sourcing rules vary by type and state.
     */
    OTHER("Other Income");
    
    private final String displayName;
    
    SaleType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Determines if this sale type uses destination-based sourcing.
     */
    public boolean isDestinationBased() {
        return this == TANGIBLE_GOODS;
    }
    
    /**
     * Determines if this sale type requires service sourcing method (market-based vs cost-of-performance).
     */
    public boolean requiresServiceSourcing() {
        return this == SERVICES;
    }
    
    /**
     * Determines if this sale type is sourced to property location.
     */
    public boolean isPropertyBased() {
        return this == RENTAL_INCOME;
    }
}
