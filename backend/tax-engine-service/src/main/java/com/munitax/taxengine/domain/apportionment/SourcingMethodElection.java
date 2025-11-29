package com.munitax.taxengine.domain.apportionment;

/**
 * Sourcing method election for sales factor: Finnigan vs Joyce.
 * Determines which entities' sales are included in the denominator.
 *
 * @see <a href="https://www.mtc.gov/uniformity/project-teams/joyce-finnigan">MTC Joyce/Finnigan Analysis</a>
 */
public enum SourcingMethodElection {
    
    /**
     * Finnigan Method (Majority Rule)
     * Include all sales of the affiliated group in the denominator,
     * regardless of whether each entity has nexus in the state.
     * 
     * Used by: CA, IL, TX, NY, and majority of states.
     * 
     * Example: Parent has OH nexus ($5M sales), Sub A has OH nexus ($3M sales),
     * Sub B has no OH nexus ($2M sales).
     * Denominator = $5M + $3M + $2M = $10M (all entities included)
     */
    FINNIGAN("Finnigan Method - Include all group sales"),
    
    /**
     * Joyce Method (Minority Rule)
     * Include only sales of entities with nexus in the state.
     * 
     * Used by: NJ, PA, and minority of states.
     * 
     * Example: Parent has OH nexus ($5M sales), Sub A has OH nexus ($3M sales),
     * Sub B has no OH nexus ($2M sales).
     * Denominator = $5M + $3M = $8M (only nexus entities included)
     */
    JOYCE("Joyce Method - Include only nexus entity sales");
    
    private final String description;
    
    SourcingMethodElection(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines if non-nexus entity sales should be included in denominator.
     */
    public boolean includeNonNexusEntities() {
        return this == FINNIGAN;
    }
}
