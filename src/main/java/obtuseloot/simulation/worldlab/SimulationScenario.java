package obtuseloot.simulation.worldlab;

import java.util.Map;

public record SimulationScenario(
        String name,
        int artifactPopulationSize,
        int generations,
        double mutationIntensity,
        double competitionPressure,
        double ecologySensitivity,
        double lineageDriftWindow,
        Map<PlayerBehaviorModel, Double> behaviorMix
) {
    public static SimulationScenario defaults(WorldSimulationConfig cfg) {
        return new SimulationScenario(
                "default-world-lab",
                cfg.playerCount() * cfg.artifactsPerPlayer(),
                cfg.seasonCount() * cfg.sessionsPerSeason(),
                cfg.mutationPressureMultiplier(),
                1.0D,
                1.0D,
                1.0D,
                Map.of(
                        PlayerBehaviorModel.EXPLORER, 0.25D,
                        PlayerBehaviorModel.RITUALIST, 0.25D,
                        PlayerBehaviorModel.GATHERER, 0.25D,
                        PlayerBehaviorModel.RANDOM_BASELINE, 0.25D)
        );
    }
}
