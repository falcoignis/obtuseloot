package obtuseloot.telemetry;

import java.util.LinkedHashMap;
import java.util.Map;

public class TelemetryEventFactory {
    public EcosystemTelemetryEvent create(EcosystemTelemetryEventType type,
                                          long artifactSeed,
                                          String lineageId,
                                          String niche,
                                          Map<String, String> attributes) {
        long now = System.currentTimeMillis();
        Map<String, String> normalized = new LinkedHashMap<>(attributes == null ? Map.of() : attributes);
        normalized.put("timestamp", String.valueOf(now));
        normalized.put("artifact_seed", String.valueOf(artifactSeed));
        normalized.put("artifact_id", "artifact-" + Long.toUnsignedString(artifactSeed));
        normalized.put("lineage_id", lineageId == null || lineageId.isBlank() ? TelemetryFieldContract.NOT_APPLICABLE : lineageId);
        normalized.put("niche", niche == null || niche.isBlank() ? TelemetryFieldContract.NOT_APPLICABLE : niche);
        return new EcosystemTelemetryEvent(
                now,
                type,
                artifactSeed,
                lineageId,
                niche,
                TelemetryFieldContract.normalize(type, normalized)
        );
    }
}
