package obtuseloot.analytics.ecosystem;

public record HistoricalBucketPolicy(
        BucketType bucketType,
        int bucketSpan,
        int retentionBuckets,
        int rollingWindowSnapshots
) {
    public enum BucketType {
        HOURLY,
        DAILY,
        ROLLING_WINDOW,
        SCENARIO_RUN,
        GENERATION_WINDOW
    }

    public static HistoricalBucketPolicy daily(int retentionDays) {
        return new HistoricalBucketPolicy(BucketType.DAILY, 1, Math.max(1, retentionDays), 0);
    }

    public static HistoricalBucketPolicy rollingSnapshots(int snapshots) {
        return new HistoricalBucketPolicy(BucketType.ROLLING_WINDOW, 1, 1, Math.max(1, snapshots));
    }

    public static HistoricalBucketPolicy scenarioWindow() {
        return new HistoricalBucketPolicy(BucketType.SCENARIO_RUN, 1, 1, 0);
    }
}
