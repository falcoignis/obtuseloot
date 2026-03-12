package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.TelemetryRollupSnapshot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TimeWindowedRollupReader {
    private final AnalysisWindowSelector selector;

    public TimeWindowedRollupReader() {
        this(new AnalysisWindowSelector());
    }

    public TimeWindowedRollupReader(AnalysisWindowSelector selector) {
        this.selector = selector;
    }

    public List<TelemetryRollupSnapshot> readWindow(List<TelemetryRollupSnapshot> history, HistoricalBucketPolicy policy) {
        return selector.select(history, policy);
    }

    public Map<String, List<TelemetryRollupSnapshot>> bucket(List<TelemetryRollupSnapshot> history, HistoricalBucketPolicy policy) {
        List<TelemetryRollupSnapshot> selected = readWindow(history, policy);
        Map<String, List<TelemetryRollupSnapshot>> out = new LinkedHashMap<>();
        out.put(policy == null ? "scenario" : policy.bucketType().name().toLowerCase(java.util.Locale.ROOT), selected);
        return Map.copyOf(out);
    }
}
