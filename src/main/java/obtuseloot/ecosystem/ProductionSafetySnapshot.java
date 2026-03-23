package obtuseloot.ecosystem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable point-in-time snapshot of production-safety-relevant ecosystem metrics.
 *
 * <p>Snapshots can be compared with each other via {@link #compareTo(ProductionSafetySnapshot)}
 * and serialised to JSON via {@link #toJson()} for external analysis or the {@code ecosystem dump}
 * command.  Replay from a snapshot combined with a deterministic seed restores prior system state.
 */
public record ProductionSafetySnapshot(
        long timestampMs,
        Map<String, Double> categoryShares,
        Map<String, Double> templateShares,
        double averageCandidatePoolSize,
        double diversityIndex,
        int windowFill,
        List<String> activeGuards,
        List<String> activeFailureSignals) {

    /** Returns an empty placeholder snapshot (no observations recorded). */
    public static ProductionSafetySnapshot empty() {
        return new ProductionSafetySnapshot(
                System.currentTimeMillis(),
                Collections.emptyMap(), Collections.emptyMap(),
                0.0, 0.0, 0,
                Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Returns a human-readable delta summary relative to {@code other} (treated as the earlier snapshot).
     */
    public String compareTo(ProductionSafetySnapshot other) {
        StringBuilder sb = new StringBuilder();
        sb.append("Snapshot delta (ms apart: ")
                .append(Math.abs(timestampMs - other.timestampMs))
                .append(")\n");

        java.util.Set<String> allCategories = new java.util.LinkedHashSet<>();
        allCategories.addAll(categoryShares.keySet());
        allCategories.addAll(other.categoryShares.keySet());
        for (String cat : allCategories) {
            double before = other.categoryShares.getOrDefault(cat, 0.0);
            double after = categoryShares.getOrDefault(cat, 0.0);
            double delta = after - before;
            if (Math.abs(delta) > 0.001) {
                sb.append(String.format(Locale.ROOT,
                        "  category %s: %.3f → %.3f (%+.3f)%n", cat, before, after, delta));
            }
        }
        sb.append(String.format(Locale.ROOT,
                "  diversityIndex: %.4f → %.4f%n", other.diversityIndex, diversityIndex));
        sb.append(String.format(Locale.ROOT,
                "  avgCandidatePool: %.2f → %.2f%n", other.averageCandidatePoolSize, averageCandidatePoolSize));
        return sb.toString();
    }

    /** Serialise to a JSON string suitable for the {@code ecosystem dump} command. */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"timestampMs\": ").append(timestampMs).append(",\n");
        sb.append("  \"timestamp\": \"").append(Instant.ofEpochMilli(timestampMs)).append("\",\n");
        sb.append("  \"windowFill\": ").append(windowFill).append(",\n");
        sb.append("  \"diversityIndex\": ")
                .append(String.format(Locale.ROOT, "%.6f", diversityIndex)).append(",\n");
        sb.append("  \"averageCandidatePoolSize\": ")
                .append(String.format(Locale.ROOT, "%.2f", averageCandidatePoolSize)).append(",\n");
        sb.append("  \"categoryShares\": {\n");
        appendDoubleMapJson(sb, categoryShares, "    ");
        sb.append("  },\n");
        sb.append("  \"templateShares\": {\n");
        appendDoubleMapJson(sb, templateShares, "    ");
        sb.append("  },\n");
        sb.append("  \"activeGuards\": ").append(toJsonStringArray(activeGuards)).append(",\n");
        sb.append("  \"activeFailureSignals\": ").append(toJsonStringArray(activeFailureSignals)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private static void appendDoubleMapJson(StringBuilder sb, Map<String, Double> map, String indent) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        for (int i = 0; i < entries.size(); i++) {
            sb.append(indent).append("\"").append(entries.get(i).getKey()).append("\": ")
                    .append(String.format(Locale.ROOT, "%.6f", entries.get(i).getValue()));
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
    }

    private static String toJsonStringArray(List<String> values) {
        if (values.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append("\"").append(values.get(i).replace("\"", "\\\"")).append("\"");
            if (i < values.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /** Return a copy of this snapshot with the top-N category entries (by share) kept, others zeroed. */
    public ProductionSafetySnapshot topNCategories(int n) {
        Map<String, Double> sorted = new LinkedHashMap<>();
        categoryShares.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(n)
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return new ProductionSafetySnapshot(
                timestampMs, sorted, templateShares,
                averageCandidatePoolSize, diversityIndex, windowFill,
                activeGuards, activeFailureSignals);
    }
}
