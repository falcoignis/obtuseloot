package obtuseloot.telemetry;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class TelemetryFieldContract {
    public static final String NOT_APPLICABLE = "na";

    private static final Set<String> BASE_REQUIRED = Set.of("event_type", "schema_version");

    private static final Map<EcosystemTelemetryEventType, SchemaContract> CONTRACTS = buildContracts();

    private TelemetryFieldContract() {
    }

    public static Map<String, String> normalize(EcosystemTelemetryEventType type, Map<String, String> provided) {
        SchemaContract contract = CONTRACTS.get(type);
        if (contract == null) {
            throw new IllegalArgumentException("Missing schema contract for telemetry event type: " + type);
        }

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
        out.put("opportunity_share", NOT_APPLICABLE);
        out.put("specialization_pressure", NOT_APPLICABLE);
        out.put("specialization_trajectory", NOT_APPLICABLE);
        out.put("rollup_type", NOT_APPLICABLE);
        out.put("rollup_record_count", NOT_APPLICABLE);
        out.put("rollup_window_ms", NOT_APPLICABLE);
        out.put("snapshot_version", NOT_APPLICABLE);
        out.put("event_type", type.name());
        out.put("schema_version", TelemetrySchemaVersion.PHASE_5_7_V1.name());

        if (provided != null) {
            provided.forEach((k, v) -> {
                if (out.containsKey(k)) {
                    out.put(k, normalizeValue(v));
                } else {
                    out.put(k, normalizeValue(v));
                }
            });
        }

        for (String field : contract.intentionallyAbsent()) {
            out.put(field, NOT_APPLICABLE);
        }

        validateRequired(type, contract, out);
        return Map.copyOf(out);
    }

    public static SchemaContract contractFor(EcosystemTelemetryEventType type) {
        return CONTRACTS.get(type);
    }

    private static void validateRequired(EcosystemTelemetryEventType type, SchemaContract contract, Map<String, String> attrs) {
        for (String field : contract.required()) {
            if (NOT_APPLICABLE.equalsIgnoreCase(attrs.getOrDefault(field, NOT_APPLICABLE))) {
                throw new IllegalArgumentException("Telemetry field '" + field + "' is required for " + type);
            }
        }
    }

    private static String normalizeValue(String value) {
        return value == null || value.isBlank() ? NOT_APPLICABLE : value;
    }

    private static Map<EcosystemTelemetryEventType, SchemaContract> buildContracts() {
        Map<EcosystemTelemetryEventType, SchemaContract> map = new EnumMap<>(EcosystemTelemetryEventType.class);

        map.put(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                new SchemaContract(
                        required("ability_id", "trigger", "mechanic", "execution_status"),
                        optional("lineage_id", "generation", "player_id", "world", "dimension", "chunk", "niche", "utility_score", "utility_density", "budget_cost", "context_tags"),
                        absent("branch_id", "mutation_influence", "drift_window_remaining", "branch_divergence", "reinforcement_multiplier", "lineage_momentum", "subniche")));

        map.put(EcosystemTelemetryEventType.LINEAGE_UPDATE,
                new SchemaContract(
                        required("lineage_id", "branch_divergence", "specialization_trajectory"),
                        optional("branch_id", "world", "dimension", "niche", "context_tags"),
                        absent("ability_id", "trigger", "mechanic", "execution_status", "utility_score", "utility_density", "budget_cost", "reinforcement_multiplier")));

        map.put(EcosystemTelemetryEventType.MUTATION_EVENT,
                new SchemaContract(
                        required("lineage_id", "mutation_influence", "drift_window_remaining", "branch_divergence", "specialization_trajectory"),
                        optional("world", "dimension", "utility_density", "ecology_pressure", "context_tags"),
                        absent("ability_id", "trigger", "mechanic", "execution_status", "budget_cost", "reinforcement_multiplier")));

        map.put(EcosystemTelemetryEventType.BRANCH_FORMATION,
                new SchemaContract(
                        required("lineage_id", "branch_id", "branch_divergence"),
                        optional("world", "dimension", "niche", "context_tags"),
                        absent("ability_id", "trigger", "mechanic", "execution_status", "budget_cost", "reinforcement_multiplier")));

        map.put(EcosystemTelemetryEventType.NICHE_CLASSIFICATION_CHANGE,
                new SchemaContract(
                        required("niche", "specialization_pressure", "specialization_trajectory"),
                        optional("lineage_id", "generation", "subniche", "world", "dimension", "context_tags"),
                        absent("ability_id", "trigger", "mechanic", "execution_status", "mutation_influence", "drift_window_remaining", "branch_divergence")));

        map.put(EcosystemTelemetryEventType.COMPETITION_ALLOCATION,
                new SchemaContract(
                        required("niche", "reinforcement_multiplier", "ecology_pressure", "specialization_trajectory"),
                        optional("lineage_id", "generation", "lineage_momentum", "world", "dimension", "utility_density", "context_tags"),
                        absent("ability_id", "trigger", "mechanic", "execution_status", "mutation_influence", "drift_window_remaining", "branch_divergence")));

        map.put(EcosystemTelemetryEventType.ROLLUP_GENERATED,
                new SchemaContract(
                        required("trigger", "context_tags"),
                        optional("world", "dimension", "niche", "lineage_id"),
                        absent("artifact_id", "artifact_seed", "ability_id", "mechanic", "execution_status", "mutation_influence", "drift_window_remaining", "branch_divergence", "reinforcement_multiplier")));

        return Map.copyOf(map);
    }

    private static Set<String> required(String... fields) {
        Set<String> out = new java.util.LinkedHashSet<>(BASE_REQUIRED);
        out.addAll(Set.of(fields));
        return Set.copyOf(out);
    }

    private static Set<String> optional(String... fields) {
        return Set.of(fields);
    }

    private static Set<String> absent(String... fields) {
        return Set.of(fields);
    }

    public record SchemaContract(Set<String> required,
                                 Set<String> optional,
                                 Set<String> intentionallyAbsent) {
    }
}
