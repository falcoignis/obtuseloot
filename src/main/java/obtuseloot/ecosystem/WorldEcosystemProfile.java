package obtuseloot.ecosystem;

public record WorldEcosystemProfile(
        double chaosActivityLevel,
        double combatAggression,
        double mobilityUsage,
        double survivalPressure,
        double precisionBehavior,
        double bossKillRate,
        double memoryEventDistribution,
        double driftMutationFrequency
) {
    public static WorldEcosystemProfile empty() {
        return new WorldEcosystemProfile(0, 0, 0, 0, 0, 0, 0, 0);
    }
}
