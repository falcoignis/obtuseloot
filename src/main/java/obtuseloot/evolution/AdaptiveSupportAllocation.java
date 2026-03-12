package obtuseloot.evolution;

public record AdaptiveSupportAllocation(
        double reinforcementMultiplier,
        double mutationOpportunity,
        double retentionOpportunity,
        double branchPersistenceSupport,
        double nicheCompetitionPressure,
        double lineageCompetitionPressure,
        double diminishingReturnFactor
) {
    public static AdaptiveSupportAllocation neutral() {
        return new AdaptiveSupportAllocation(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 1.0D);
    }
}
