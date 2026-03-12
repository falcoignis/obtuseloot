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
        normalized.put("artifact_seed", artifactSeed > 0L ? String.valueOf(artifactSeed) : TelemetryFieldContract.NOT_APPLICABLE);
        normalized.put("artifact_id", artifactSeed > 0L ? "artifact-" + Long.toUnsignedString(artifactSeed) : TelemetryFieldContract.NOT_APPLICABLE);
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
