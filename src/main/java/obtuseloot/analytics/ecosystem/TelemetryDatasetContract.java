package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.TelemetryRollupSnapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class TelemetryDatasetContract {

    public AnalyticsInputDataset resolve(Path datasetRoot) {
        Path normalized = datasetRoot.toAbsolutePath().normalize();

        Path runtimeTelemetry = normalized.resolve("telemetry").resolve("ecosystem-events.log");
        Path runtimeRollup = normalized.resolve("telemetry").resolve("rollup-snapshot.properties");
        Path rollupDir = normalized.resolve("rollups");

        if (Files.exists(runtimeTelemetry) && Files.exists(runtimeRollup) && !Files.exists(normalized.resolve("scenario-metadata.properties"))) {
            return new AnalyticsInputDataset(
                    normalized,
                    runtimeTelemetry,
                    normalized.resolve("telemetry"),
                    null,
                    null,
                    AnalyticsInputDataset.SourceKind.RUNTIME,
                    String.valueOf(TelemetryRollupSnapshot.CURRENT_VERSION),
                    Map.of("layout", "runtime-telemetry"));
        }

        if (Files.exists(runtimeTelemetry) && Files.isDirectory(rollupDir) && !Files.exists(normalized.resolve("scenario-metadata.properties"))) {
            return new AnalyticsInputDataset(
                    normalized,
                    runtimeTelemetry,
                    rollupDir,
                    null,
                    null,
                    AnalyticsInputDataset.SourceKind.RUNTIME,
                    String.valueOf(TelemetryRollupSnapshot.CURRENT_VERSION),
                    Map.of("layout", "runtime-telemetry-with-rollup-dir"));
        }

        return new HarnessOutputAdapter().adapt(normalized);
    }

    public void validate(AnalyticsInputDataset dataset) {
        if (dataset.telemetryArchivePath() == null || !Files.exists(dataset.telemetryArchivePath())) {
            throw new IllegalArgumentException("Dataset contract violation: telemetry archive is required and was not found");
        }
        if (dataset.rollupSnapshotDirectory() == null || !Files.isDirectory(dataset.rollupSnapshotDirectory())) {
            throw new IllegalArgumentException("Dataset contract violation: rollup snapshot directory is required and was not found");
        }
    }

    public DatasetLayoutDescriptor descriptor(AnalyticsInputDataset dataset) {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("telemetryArchive", String.valueOf(dataset.telemetryArchivePath()));
        required.put("rollupSnapshotDirectory", String.valueOf(dataset.rollupSnapshotDirectory()));

        Map<String, String> optional = new LinkedHashMap<>();
        if (dataset.scenarioMetadataPath() != null) {
            optional.put("scenarioMetadata", String.valueOf(dataset.scenarioMetadataPath()));
        }
        if (dataset.harnessOutputDirectory() != null) {
            optional.put("harnessOutputDirectory", String.valueOf(dataset.harnessOutputDirectory()));
        }

        return new DatasetLayoutDescriptor(dataset.sourceKind().name(), dataset.schemaVersion(), required, optional, dataset.layoutHints());
    }
}
