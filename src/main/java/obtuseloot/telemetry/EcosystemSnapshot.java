package obtuseloot.telemetry;

import java.util.Map;

public record EcosystemSnapshot(
        long generatedAtMs,
        Map<EcosystemTelemetryEventType, Long> eventCounts,
        NichePopulationRollup nichePopulationRollup,
        LineagePopulationRollup lineagePopulationRollup
) {
}
