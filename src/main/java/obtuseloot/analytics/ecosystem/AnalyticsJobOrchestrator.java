package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.EcosystemHistoryArchive;
import obtuseloot.telemetry.EcosystemTelemetryEvent;
import obtuseloot.telemetry.TelemetryRollupSnapshot;
import obtuseloot.telemetry.TelemetryRollupSnapshotStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsJobOrchestrator {
    private final TimeWindowedRollupReader windowedRollupReader;

    public AnalyticsJobOrchestrator() {
        this(new TimeWindowedRollupReader());
    }

    public AnalyticsJobOrchestrator(TimeWindowedRollupReader windowedRollupReader) {
        this.windowedRollupReader = windowedRollupReader;
    }

    public AnalysisPipelineContext prepare(EcosystemAnalysisJob job) {
        List<EcosystemTelemetryEvent> telemetry = loadTelemetry(job.telemetryArchivePath());
        List<TelemetryRollupSnapshot> rollups = loadRollups(job.rollupSnapshotDirectory());
        Map<String, String> scenarioMetadata = loadScenarioMetadata(job.harnessOutputDirectory());
        List<TelemetryRollupSnapshot> selected = windowedRollupReader.readWindow(rollups, job.bucketPolicy());
        return new AnalysisPipelineContext(job, telemetry, rollups, scenarioMetadata, selected);
    }

    private List<EcosystemTelemetryEvent> loadTelemetry(Path telemetryArchivePath) {
        if (telemetryArchivePath == null) {
            return List.of();
        }
        return new EcosystemHistoryArchive(telemetryArchivePath).readAll();
    }

    private List<TelemetryRollupSnapshot> loadRollups(Path rollupSnapshotDirectory) {
        List<TelemetryRollupSnapshot> out = new ArrayList<>();
        if (rollupSnapshotDirectory != null && Files.isDirectory(rollupSnapshotDirectory)) {
            try (var files = Files.list(rollupSnapshotDirectory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".properties"))
                        .sorted()
                        .forEach(path -> new TelemetryRollupSnapshotStore(path).readLatest().ifPresent(out::add));
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to read rollup snapshot directory", ex);
            }
        }

        return List.copyOf(out.stream().sorted(java.util.Comparator.comparingLong(TelemetryRollupSnapshot::createdAtMs)).toList());
    }

    private Map<String, String> loadScenarioMetadata(Path harnessDir) {
        if (harnessDir == null) {
            return Map.of();
        }
        Path metadata = harnessDir.resolve("scenario-metadata.properties");
        if (!Files.exists(metadata)) {
            return Map.of();
        }
        try {
            Map<String, String> out = new LinkedHashMap<>();
            for (String line : Files.readAllLines(metadata, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] kv = line.split("=", 2);
                out.put(kv[0].trim(), kv.length > 1 ? kv[1].trim() : "");
            }
            return Map.copyOf(out);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read scenario metadata", ex);
        }
    }
}
