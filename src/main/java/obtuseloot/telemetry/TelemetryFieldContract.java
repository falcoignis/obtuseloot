package obtuseloot.telemetry;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TelemetryFieldContract {
    public static final String NOT_APPLICABLE = "na";

    private TelemetryFieldContract() {
    }

    public static Map<String, String> normalize(EcosystemTelemetryEventType type, Map<String, String> provided) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("timestamp", NOT_APPLICABLE);
        out.put("artifact_id", NOT_APPLICABLE);
        out.put("artifact_seed", NOT_APPLICABLE);
        out.put("lineage_id", NOT_APPLICABLE);
        out.put("branch_id", NOT_APPLICABLE);
        out.put("generation", NOT_APPLICABLE);
        out.put("player_id", NOT_APPLICABLE);
        out.put("world", NOT_APPLICABLE);
        out.put("dimension", NOT_APPLICABLE);
        out.put("chunk", NOT_APPLICABLE);
        out.put("trigger", NOT_APPLICABLE);
        out.put("mechanic", NOT_APPLICABLE);
        out.put("ability_id", NOT_APPLICABLE);
        out.put("niche", NOT_APPLICABLE);
        out.put("subniche", NOT_APPLICABLE);
        out.put("execution_status", NOT_APPLICABLE);
        out.put("utility_score", NOT_APPLICABLE);
        out.put("utility_density", NOT_APPLICABLE);
        out.put("budget_cost", NOT_APPLICABLE);
        out.put("reinforcement_multiplier", NOT_APPLICABLE);
        out.put("ecology_pressure", NOT_APPLICABLE);
        out.put("lineage_momentum", NOT_APPLICABLE);
        out.put("mutation_influence", NOT_APPLICABLE);
        out.put("drift_window_remaining", NOT_APPLICABLE);
        out.put("branch_divergence", NOT_APPLICABLE);
        out.put("context_tags", NOT_APPLICABLE);
        out.put("event_type", type.name());
        out.put("schema_version", TelemetrySchemaVersion.PHASE_5_5_V1.name());
        if (provided != null) {
            provided.forEach((k, v) -> out.put(k, v == null || v.isBlank() ? NOT_APPLICABLE : v));
        }
        return Map.copyOf(out);
    }
}
