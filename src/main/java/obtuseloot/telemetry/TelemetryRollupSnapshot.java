package obtuseloot.telemetry;

public record TelemetryRollupSnapshot(
        int version,
        long createdAtMs,
        String initializationMode,
        EcosystemSnapshot ecosystemSnapshot
) {
    public static final int CURRENT_VERSION = 1;

    public static TelemetryRollupSnapshot coldStart(long nowMs) {
        EcosystemSnapshot empty = new EcosystemSnapshot(0L, java.util.Map.of(),
                new NichePopulationRollup(0L, java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of()),
                new LineagePopulationRollup(0L, java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of()),
                0L, 0.0D, 0.0D, 0.0D, 0L, 0L, java.util.Map.of(), java.util.List.of(), 0L, java.util.Map.of());
        return new TelemetryRollupSnapshot(CURRENT_VERSION, nowMs, "cold_start", empty);
    }
}
