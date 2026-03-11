package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.ScoringMode;

public final class WorldSimulationRunner {
    private WorldSimulationRunner() {
    }

    public static void main(String[] args) throws Exception {
        WorldSimulationConfig defaults = WorldSimulationConfig.defaults();
        WorldSimulationConfig config = new WorldSimulationConfig(
                longProp("world.seed", defaults.seed()),
                intProp("world.players", defaults.playerCount()),
                intProp("world.artifactsPerPlayer", defaults.artifactsPerPlayer()),
                intProp("world.sessionsPerSeason", defaults.sessionsPerSeason()),
                intProp("world.seasonCount", defaults.seasonCount()),
                doubleProp("world.bossFrequency", defaults.bossFrequency()),
                intProp("world.encounterDensity", defaults.encounterDensity()),
                doubleProp("world.chaosEventRate", defaults.chaosEventRate()),
                doubleProp("world.lowHealthEventRate", defaults.lowHealthEventRate()),
                doubleProp("world.mutationPressureMultiplier", defaults.mutationPressureMultiplier()),
                doubleProp("world.memoryEventMultiplier", defaults.memoryEventMultiplier()),
                System.getProperty("world.outputDirectory", defaults.outputDirectory()),
                boolProp("world.enableEde", defaults.enableExperienceDrivenEvolution()),
                boolProp("world.enableEcosystemBias", defaults.enableEcosystemBias()),
                boolProp("world.enableDiversityPreservation", defaults.enableDiversityPreservation()),
                boolProp("world.enableSelfBalancing", defaults.enableSelfBalancingAdjustments()),
                boolProp("world.enableEnvironmentalPressure", defaults.enableEnvironmentalPressure()),
                boolProp("world.enableTraitInteractions", defaults.enableTraitInteractions()),
                boolProp("world.enableCoEvolution", defaults.enableCoEvolution()),
                new FitnessSharingConfig(
                        boolProp("world.fitnessSharing.enabled", defaults.fitnessSharing().enabled()),
                        System.getProperty("world.fitnessSharing.mode", defaults.fitnessSharing().mode()),
                        doubleProp("world.fitnessSharing.alpha", defaults.fitnessSharing().alpha()),
                        doubleProp("world.fitnessSharing.maxPenalty", defaults.fitnessSharing().maxPenalty()),
                        doubleProp("world.fitnessSharing.targetOccupancy", defaults.fitnessSharing().targetOccupancy()),
                        doubleProp("world.fitnessSharing.similarityRadius", defaults.fitnessSharing().similarityRadius())
                ).bounded(),
                new SpeciesNicheAnalyticsEngine.BehavioralProjectionConfig(
                        boolProp("world.behavioralProjection.enabled", defaults.behavioralProjection().enabled()),
                        doubleProp("world.behavioralProjection.traitEcologyWeight", defaults.behavioralProjection().traitEcologyWeight()),
                        doubleProp("world.behavioralProjection.behaviorWeight", defaults.behavioralProjection().behaviorWeight())
                ),
                new SpeciesNicheAnalyticsEngine.RoleBasedRepulsionConfig(
                        boolProp("world.roleBasedRepulsion.enabled", defaults.roleBasedRepulsion().enabled()),
                        doubleProp("world.roleBasedRepulsion.beta", defaults.roleBasedRepulsion().beta()),
                        doubleProp("world.roleBasedRepulsion.supportDamageWeight", defaults.roleBasedRepulsion().supportDamageWeight()),
                        doubleProp("world.roleBasedRepulsion.burstPersistenceWeight", defaults.roleBasedRepulsion().burstPersistenceWeight()),
                        doubleProp("world.roleBasedRepulsion.mobilityStationaryWeight", defaults.roleBasedRepulsion().mobilityStationaryWeight()),
                        doubleProp("world.roleBasedRepulsion.environmentWeight", defaults.roleBasedRepulsion().environmentWeight()),
                        doubleProp("world.roleBasedRepulsion.memoryWeight", defaults.roleBasedRepulsion().memoryWeight()),
                        doubleProp("world.roleBasedRepulsion.interactionWeight", defaults.roleBasedRepulsion().interactionWeight())
                ).bounded(),
                new AdaptiveNicheCapacityConfig(
                        boolProp("world.adaptiveNicheCapacity.enabled", defaults.adaptiveNicheCapacity().enabled()),
                        doubleProp("world.adaptiveNicheCapacity.minCapacity", defaults.adaptiveNicheCapacity().minCapacity()),
                        doubleProp("world.adaptiveNicheCapacity.maxCapacity", defaults.adaptiveNicheCapacity().maxCapacity()),
                        doubleProp("world.adaptiveNicheCapacity.noveltyWeight", defaults.adaptiveNicheCapacity().noveltyWeight()),
                        doubleProp("world.adaptiveNicheCapacity.diversityWeight", defaults.adaptiveNicheCapacity().diversityWeight()),
                        doubleProp("world.adaptiveNicheCapacity.persistenceWeight", defaults.adaptiveNicheCapacity().persistenceWeight()),
                        doubleProp("world.adaptiveNicheCapacity.overcrowdingWeight", defaults.adaptiveNicheCapacity().overcrowdingWeight()),
                        doubleProp("world.adaptiveNicheCapacity.stagnationWeight", defaults.adaptiveNicheCapacity().stagnationWeight()),
                        doubleProp("world.adaptiveNicheCapacity.maxSeasonDelta", defaults.adaptiveNicheCapacity().maxSeasonDelta())
                ).bounded(),
                ScoringMode.fromString(System.getProperty("world.scoringMode"), defaults.scoringMode())
        );
        new WorldSimulationHarness(config).runAndWriteOutputs();
        System.out.println("World simulation outputs written to " + config.outputDirectory());
    }

    private static int intProp(String key, int defaultValue) {
        return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
    }

    private static long longProp(String key, long defaultValue) {
        return Long.parseLong(System.getProperty(key, String.valueOf(defaultValue)));
    }

    private static double doubleProp(String key, double defaultValue) {
        return Double.parseDouble(System.getProperty(key, String.valueOf(defaultValue)));
    }

    private static boolean boolProp(String key, boolean defaultValue) {
        return Boolean.parseBoolean(System.getProperty(key, String.valueOf(defaultValue)));
    }
}
