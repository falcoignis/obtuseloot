package obtuseloot.analytics.ecosystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class AnalysisJobPersistence {

    public Path writeJobRecord(AnalysisJobRecord record) {
        Path path = record.outputDirectory().resolve(record.jobId() + "-job-record.properties");
        Properties p = new Properties();
        p.setProperty("jobId", record.jobId());
        p.setProperty("datasetRoot", String.valueOf(record.datasetRoot()));
        p.setProperty("outputDirectory", String.valueOf(record.outputDirectory()));
        p.setProperty("createdAtMs", String.valueOf(record.createdAtMs()));
        p.setProperty("sourceKind", record.sourceKind());
        p.setProperty("schemaVersion", record.schemaVersion());
        p.setProperty("bucketType", record.bucketPolicy().bucketType().name());
        p.setProperty("retentionBuckets", String.valueOf(record.bucketPolicy().retentionBuckets()));
        p.setProperty("bucketSizeHours", String.valueOf(record.bucketPolicy().bucketSpan()));
        p.setProperty("offsetBuckets", String.valueOf(record.bucketPolicy().rollingWindowSnapshots()));
        record.scenarioMetadata().forEach((k, v) -> p.setProperty("scenario." + k, v));
        store(path, p);
        return path;
    }

    public Path writeRunMetadata(Path outputDir, AnalysisRunMetadata metadata) {
        Path path = outputDir.resolve(metadata.jobId() + "-run-metadata.properties");
        Properties p = new Properties();
        p.setProperty("jobId", metadata.jobId());
        p.setProperty("startedAtMs", String.valueOf(metadata.startedAtMs()));
        p.setProperty("finishedAtMs", String.valueOf(metadata.finishedAtMs()));
        p.setProperty("status", metadata.status());
        p.setProperty("sourceKind", metadata.sourceKind());
        p.setProperty("analysisWindow", metadata.analysisWindow());
        p.setProperty("rollupsLoaded", String.valueOf(metadata.rollupsLoaded()));
        p.setProperty("rollupsSelected", String.valueOf(metadata.rollupsSelected()));
        p.setProperty("telemetryEventsLoaded", String.valueOf(metadata.telemetryEventsLoaded()));
        p.setProperty("failureReason", metadata.failureReason() == null ? "" : metadata.failureReason());
        store(path, p);
        return path;
    }

    public Path writeOutputManifest(Path outputDir, JobOutputManifest manifest) {
        Path path = outputDir.resolve(manifest.jobId() + "-output-manifest.properties");
        Properties p = new Properties();
        p.setProperty("jobId", manifest.jobId());
        p.setProperty("recommendationId", manifest.recommendationId());
        p.setProperty("recommendationDecision", manifest.recommendationDecision());
        p.setProperty("recommendationRationale", manifest.recommendationRationale());
        p.setProperty("analysisSummary", manifest.analysisSummary());
        manifest.artifacts().forEach((k, v) -> p.setProperty("artifact." + k, v));
        store(path, p);
        return path;
    }

    public static Map<String, String> readProperties(Path path) {
        if (!Files.exists(path)) {
            return Map.of();
        }
        try {
            Properties p = new Properties();
            p.load(Files.newBufferedReader(path, StandardCharsets.UTF_8));
            Map<String, String> out = new LinkedHashMap<>();
            for (String key : p.stringPropertyNames()) {
                out.put(key, p.getProperty(key));
            }
            return Map.copyOf(out);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read properties: " + path, ex);
        }
    }

    private void store(Path path, Properties p) {
        try {
            Files.createDirectories(path.getParent());
            StringBuilder out = new StringBuilder();
            p.stringPropertyNames().stream().sorted().forEach(k -> out.append(k).append('=').append(p.getProperty(k, "")).append('\n'));
            Files.writeString(path, out.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write analytics metadata file " + path, ex);
        }
    }
}
