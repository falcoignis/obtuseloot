package obtuseloot.evolution.params;

public record EcosystemTuningProfile(
        double nicheSaturationSensitivity,
        double lineageMomentumInfluence,
        double mutationAmplitudeMin,
        double mutationAmplitudeMax,
        int driftWindowDurationTicks,
        double competitionReinforcementCurve,
        int telemetryFlushIntervalTicks,
        int telemetryArchiveBatchSize,
        long telemetryRollupIntervalMs,
        int telemetryRehydrateReplayWindowEvents
) {
    public static EcosystemTuningProfile defaults() {
        return new EcosystemTuningProfile(0.92D, 1.00D, 0.68D, 1.50D, 4, 1.05D, 100, 256, 5_000L, 512);
    }
}
