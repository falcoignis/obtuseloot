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
        long telemetryRollupIntervalMs
) {
    public static EcosystemTuningProfile defaults() {
        return new EcosystemTuningProfile(0.90D, 1.00D, 0.70D, 1.65D, 5, 1.0D, 100, 256, 5_000L);
    }
}
