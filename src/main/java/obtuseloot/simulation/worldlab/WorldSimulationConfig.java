package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.ScoringMode;

public record WorldSimulationConfig(
        long seed,
        int playerCount,
        int artifactsPerPlayer,
        int sessionsPerSeason,
        int seasonCount,
        double bossFrequency,
        int encounterDensity,
        double chaosEventRate,
        double lowHealthEventRate,
        double mutationPressureMultiplier,
        double memoryEventMultiplier,
        String outputDirectory,
        boolean enableExperienceDrivenEvolution,
        boolean enableEcosystemBias,
        boolean enableDiversityPreservation,
        boolean enableSelfBalancingAdjustments,
        boolean enableEnvironmentalPressure,
        boolean enableTraitInteractions,
        boolean enableCoEvolution,
        FitnessSharingConfig fitnessSharing,
        SpeciesNicheAnalyticsEngine.BehavioralProjectionConfig behavioralProjection,
        SpeciesNicheAnalyticsEngine.RoleBasedRepulsionConfig roleBasedRepulsion,
        SpeciesNicheAnalyticsEngine.MinimumRoleSeparationConfig minimumRoleSeparation,
        AdaptiveNicheCapacityConfig adaptiveNicheCapacity,
        OpportunityWeightedMutationConfig opportunityWeightedMutation,
        boolean validationProfile,
        ScoringMode scoringMode,
        String scenarioConfigPath
) {
    public static WorldSimulationConfig defaults() {
        return new WorldSimulationConfig(
                90210L,
                120,
                4,
                18,
                6,
                0.18D,
                7,
                0.20D,
                0.15D,
                1.0D,
                1.0D,
                "analytics/world-lab",
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                FitnessSharingConfig.defaults(),
                SpeciesNicheAnalyticsEngine.BehavioralProjectionConfig.defaults(),
                SpeciesNicheAnalyticsEngine.RoleBasedRepulsionConfig.defaults(),
                SpeciesNicheAnalyticsEngine.MinimumRoleSeparationConfig.defaults(),
                AdaptiveNicheCapacityConfig.defaults(),
                OpportunityWeightedMutationConfig.defaults(),
                false,
                ScoringMode.PROJECTION_WITH_CACHE,
                ""
        );
    }
}
