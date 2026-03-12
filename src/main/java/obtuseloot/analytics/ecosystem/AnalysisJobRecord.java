package obtuseloot.analytics.ecosystem;

import java.nio.file.Path;
import java.util.Map;

public record AnalysisJobRecord(
        String jobId,
        Path datasetRoot,
        Path outputDirectory,
        long createdAtMs,
        HistoricalBucketPolicy bucketPolicy,
        String sourceKind,
        String schemaVersion,
        Map<String, String> scenarioMetadata
) {
}
