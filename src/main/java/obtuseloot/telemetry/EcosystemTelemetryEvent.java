package obtuseloot.telemetry;

import java.util.Map;

public record EcosystemTelemetryEvent(
        long timestampMs,
        EcosystemTelemetryEventType type,
        long artifactSeed,
        String lineageId,
        String niche,
        Map<String, String> attributes
) {
}
