package obtuseloot.analytics.ecosystem;

import java.nio.file.Path;

public record EcosystemAnalysisJob(
        String jobId,
        Path telemetryArchivePath,
        Path rollupSnapshotDirectory,
        Path harnessOutputDirectory,
        Path outputDirectory,
        HistoricalBucketPolicy bucketPolicy
) {
}
