package obtuseloot.telemetry;

import java.util.Map;
import java.util.Objects;

public record EcosystemTelemetryEvent(
        long timestampMs,
        EcosystemTelemetryEventType type,
        long artifactSeed,
        String lineageId,
        String niche,
        Map<String, String> attributes
) {
    public EcosystemTelemetryEvent {
        if (timestampMs <= 0L) {
            throw new IllegalArgumentException("Telemetry event timestampMs must be > 0 but was " + timestampMs);
        }
        type = Objects.requireNonNull(type, "type");
        if (artifactSeed < 0L) {
            throw new IllegalArgumentException("Telemetry event artifactSeed must be >= 0 but was " + artifactSeed);
        }
        lineageId = lineageId == null ? "" : lineageId;
        niche = niche == null ? "" : niche;
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
    }
}
