package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.TelemetryRollupSnapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class HarnessOutputAdapter {

    public AnalyticsInputDataset adapt(Path harnessRoot) {
        Path normalized = harnessRoot.toAbsolutePath().normalize();

        Path telemetryArchive = normalized.resolve("telemetry").resolve("ecosystem-events.log");
        Path legacyEventLog = normalized.resolve("telemetry-events.log");
        if (!Files.exists(telemetryArchive) && Files.exists(legacyEventLog)) {
            throw new IllegalArgumentException("Harness dataset contains legacy telemetry-events.log but no telemetry/ecosystem-events.log archive. "
                    + "Re-run harness on Phase 6.6+ output format.");
        }
        if (!Files.exists(telemetryArchive)) {
            throw new IllegalArgumentException("Harness dataset missing telemetry archive at telemetry/ecosystem-events.log");
        }

        Path rollupHistoryDir = normalized.resolve("rollup_history");
        Path rollupSnapshot = normalized.resolve("telemetry").resolve("rollup-snapshot.properties");
        Path legacyRollupSnapshot = normalized.resolve("rollup-snapshot.properties");
        Path rollupDir = Files.isDirectory(rollupHistoryDir) ? rollupHistoryDir : normalized.resolve("telemetry");
        if (!Files.exists(rollupSnapshot) && Files.exists(legacyRollupSnapshot)) {
            rollupDir = normalized;
        }
        boolean hasPropertiesRollups;
        try (var files = Files.list(rollupDir)) {
            hasPropertiesRollups = files.anyMatch(path -> path.getFileName().toString().endsWith(".properties"));
        } catch (Exception ex) {
            hasPropertiesRollups = false;
        }
        if (!hasPropertiesRollups) {
            throw new IllegalArgumentException("Harness dataset missing rollup snapshots (.properties) in rollup_history/, telemetry/, or root legacy layout");
        }

        Path scenarioMetadata = normalized.resolve("scenario-metadata.properties");

        return new AnalyticsInputDataset(
                normalized,
                telemetryArchive,
                rollupDir,
                normalized,
                Files.exists(scenarioMetadata) ? scenarioMetadata : null,
                AnalyticsInputDataset.SourceKind.HARNESS,
                String.valueOf(TelemetryRollupSnapshot.CURRENT_VERSION),
                Map.of("layout", "harness-world-simulation"));
    }
}
