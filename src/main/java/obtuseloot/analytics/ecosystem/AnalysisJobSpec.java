package obtuseloot.analytics.ecosystem;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public record AnalysisJobSpec(
        String jobId,
        Path datasetPath,
        Path outputDirectory,
        HistoricalBucketPolicy bucketPolicy,
        boolean exportAcceptedRecommendations,
        Map<String, String> tags
) {
    public static AnalysisJobSpec fromProperties(Properties p) {
        String jobId = p.getProperty("job.id", "analytics-job-" + System.currentTimeMillis());
        Path datasetPath = Path.of(required(p, "dataset.path"));
        Path outputDirectory = Path.of(required(p, "output.path"));

        HistoricalBucketPolicy.BucketType bucketType = HistoricalBucketPolicy.BucketType.valueOf(
                p.getProperty("bucket.type", "ROLLING_WINDOW").trim().toUpperCase(java.util.Locale.ROOT));
        int retention = Integer.parseInt(p.getProperty("bucket.retention", "7"));
        int bucketSpan = Integer.parseInt(p.getProperty("bucket.span", "1"));
        int rollingWindowSnapshots = Integer.parseInt(p.getProperty("bucket.rollingWindowSnapshots", "0"));

        HistoricalBucketPolicy policy = new HistoricalBucketPolicy(bucketType, bucketSpan, retention, rollingWindowSnapshots);
        boolean exportAccepted = Boolean.parseBoolean(p.getProperty("recommendation.exportAccepted", "false"));

        Map<String, String> tags = new LinkedHashMap<>();
        p.stringPropertyNames().stream()
                .filter(k -> k.startsWith("tag."))
                .forEach(k -> tags.put(k.substring("tag.".length()), p.getProperty(k)));

        return new AnalysisJobSpec(jobId, datasetPath, outputDirectory, policy, exportAccepted, Map.copyOf(tags));
    }

    private static String required(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required job spec property: " + key);
        }
        return v.trim();
    }
}
