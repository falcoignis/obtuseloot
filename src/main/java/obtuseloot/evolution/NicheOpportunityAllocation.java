package obtuseloot.evolution;

public record NicheOpportunityAllocation(
        MechanicNicheTag niche,
        double opportunityShare,
        double reinforcementMultiplier,
        double mutationSupport,
        double retentionSupport,
        double competitionPressure,
        double nicheReinforcementMultiplier,
        double nicheMutationSupport,
        double nicheRetentionSupport
) {
}
