package obtuseloot.analytics.ecosystem;

import java.nio.file.Path;

public record EcosystemAnalysisJob(
        String jobId,
        Path datasetRoot,
        Path telemetryArchivePath,
        Path rollupSnapshotDirectory,
        Path harnessOutputDirectory,
        Path outputDirectory,
        HistoricalBucketPolicy bucketPolicy
) {
}
