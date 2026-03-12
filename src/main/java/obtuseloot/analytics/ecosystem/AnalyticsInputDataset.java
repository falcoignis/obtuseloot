package obtuseloot.analytics.ecosystem;

import java.nio.file.Path;
import java.util.Map;

public record AnalyticsInputDataset(
        Path datasetRoot,
        Path telemetryArchivePath,
        Path rollupSnapshotDirectory,
        Path harnessOutputDirectory,
        Path scenarioMetadataPath,
        SourceKind sourceKind,
        String schemaVersion,
        Map<String, String> layoutHints
) {
    public enum SourceKind {
        RUNTIME,
        HARNESS
    }
}
