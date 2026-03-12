package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.TelemetryRollupSnapshot;

import java.util.List;

public class AnalysisWindowSelector {

    public List<TelemetryRollupSnapshot> select(List<TelemetryRollupSnapshot> history, HistoricalBucketPolicy policy) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        if (policy == null || policy.bucketType() == HistoricalBucketPolicy.BucketType.SCENARIO_RUN) {
            return List.copyOf(history);
        }
        return switch (policy.bucketType()) {
            case ROLLING_WINDOW, GENERATION_WINDOW -> {
                int keep = Math.min(Math.max(1, policy.rollingWindowSnapshots()), history.size());
                yield List.copyOf(history.subList(history.size() - keep, history.size()));
            }
            case HOURLY, DAILY -> {
                long bucketMs = policy.bucketType() == HistoricalBucketPolicy.BucketType.HOURLY
                        ? 3_600_000L * Math.max(1, policy.bucketSpan())
                        : 86_400_000L * Math.max(1, policy.bucketSpan());
                long end = history.getLast().createdAtMs();
                long start = end - (bucketMs * Math.max(1, policy.retentionBuckets()));
                yield history.stream()
                        .filter(s -> s.createdAtMs() >= start)
                        .toList();
            }
            case SCENARIO_RUN -> List.copyOf(history);
        };
    }
}
