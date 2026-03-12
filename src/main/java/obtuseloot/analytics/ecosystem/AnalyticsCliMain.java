package obtuseloot.analytics.ecosystem;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class AnalyticsCliMain {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase(Locale.ROOT);
        switch (command) {
            case "analyze" -> runAnalyze(parseArgs(args, 1));
            case "run-spec" -> runSpec(parseArgs(args, 1));
            case "decide" -> runDecision(parseArgs(args, 1));
            case "export-accepted" -> runExportAccepted(parseArgs(args, 1));
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    private static void runAnalyze(CliArgs args) {
        Path datasetPath = Path.of(args.required("dataset"));
        Path outputPath = Path.of(args.required("output"));
        String jobId = args.getOrDefault("job-id", "analysis-" + Instant.now().toEpochMilli());

        HistoricalBucketPolicy policy = buildPolicy(args);

        TelemetryDatasetContract contract = new TelemetryDatasetContract();
        AnalyticsInputDataset dataset = contract.resolve(datasetPath);
        contract.validate(dataset);

        EcosystemAnalysisJob job = new EcosystemAnalysisJob(
                jobId,
                dataset.datasetRoot(),
                dataset.telemetryArchivePath(),
                dataset.rollupSnapshotDirectory(),
                dataset.harnessOutputDirectory(),
                outputPath,
                policy);

        AnalyticsOutputBundle bundle = new EcosystemAnalyticsRunner().run(job);

        if (Boolean.parseBoolean(args.getOrDefault("export-accepted", "false"))) {
            exportAcceptedRecommendations(bundle.recommendationHistoryPath(), outputPath.resolve("accepted-profiles"));
        }

        System.out.println("analysis complete: job=" + bundle.jobId());
        System.out.println("report=" + bundle.reportPath());
        System.out.println("manifest=" + bundle.outputManifestPath());
    }

    private static void runSpec(CliArgs args) throws Exception {
        Path specPath = Path.of(args.required("spec"));
        Properties p = new Properties();
        p.load(Files.newBufferedReader(specPath, StandardCharsets.UTF_8));

        AnalysisJobSpec spec = AnalysisJobSpec.fromProperties(p);
        List<String> analyzeArgs = new ArrayList<>();
        analyzeArgs.add("analyze");
        analyzeArgs.add("--dataset");
        analyzeArgs.add(spec.datasetPath().toString());
        analyzeArgs.add("--output");
        analyzeArgs.add(spec.outputDirectory().toString());
        analyzeArgs.add("--job-id");
        analyzeArgs.add(spec.jobId());
        analyzeArgs.add("--bucket-type");
        analyzeArgs.add(spec.bucketPolicy().bucketType().name());
        analyzeArgs.add("--retention");
        analyzeArgs.add(String.valueOf(spec.bucketPolicy().retentionBuckets()));
        analyzeArgs.add("--bucket-span");
        analyzeArgs.add(String.valueOf(spec.bucketPolicy().bucketSpan()));
        analyzeArgs.add("--rolling-window-snapshots");
        analyzeArgs.add(String.valueOf(spec.bucketPolicy().rollingWindowSnapshots()));
        if (spec.exportAcceptedRecommendations()) {
            analyzeArgs.add("--export-accepted");
            analyzeArgs.add("true");
        }
        main(analyzeArgs.toArray(String[]::new));
    }

    private static void runDecision(CliArgs args) {
        Path historyPath = Path.of(args.required("history"));
        String recommendationId = args.required("recommendation-id");
        RecommendationDecision decision = RecommendationDecision.valueOf(args.required("decision").toUpperCase(Locale.ROOT));
        String note = args.getOrDefault("note", "decision recorded via CLI");

        RecommendationHistoryStore store = new RecommendationHistoryStore(historyPath);
        TuningRecommendationRecord updated = store.setDecision(recommendationId, decision, note)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation ID not found: " + recommendationId));

        if (decision == RecommendationDecision.ACCEPTED && args.has("export-dir")) {
            Path exportedPath = Path.of(args.required("export-dir")).resolve(updated.recommendationId() + "-tuning-profile.properties");
            new TuningProfileExport().exportAccepted(updated, exportedPath);
            System.out.println("exported=" + exportedPath);
        }
    }

    private static void runExportAccepted(CliArgs args) {
        Path historyPath = Path.of(args.required("history"));
        Path outputDir = Path.of(args.required("output-dir"));
        exportAcceptedRecommendations(historyPath, outputDir);
    }

    private static void exportAcceptedRecommendations(Path historyPath, Path outputDir) {
        RecommendationHistoryStore store = new RecommendationHistoryStore(historyPath);
        store.readAll().stream()
                .filter(record -> record.decision() == RecommendationDecision.ACCEPTED)
                .forEach(record -> {
                    Path path = outputDir.resolve(record.recommendationId() + "-tuning-profile.properties");
                    new TuningProfileExport().exportAccepted(record, path);
                });
    }

    private static HistoricalBucketPolicy buildPolicy(CliArgs args) {
        HistoricalBucketPolicy.BucketType bucketType = HistoricalBucketPolicy.BucketType.valueOf(
                args.getOrDefault("bucket-type", "ROLLING_WINDOW").toUpperCase(Locale.ROOT));
        int retention = Integer.parseInt(args.getOrDefault("retention", "7"));
        int bucketSpan = Integer.parseInt(args.getOrDefault("bucket-span", "1"));
        int rollingWindow = Integer.parseInt(args.getOrDefault("rolling-window-snapshots", "0"));
        return new HistoricalBucketPolicy(bucketType, bucketSpan, retention, rollingWindow);
    }

    private static CliArgs parseArgs(String[] args, int start) {
        CliArgs out = new CliArgs();
        for (int i = start; i < args.length; i++) {
            String token = args[i];
            if (!token.startsWith("--")) {
                continue;
            }
            String key = token.substring(2);
            String value = (i + 1 < args.length && !args[i + 1].startsWith("--")) ? args[++i] : "true";
            out.put(key, value);
        }
        return out;
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  analyze --dataset <path> --output <path> [--job-id <id>] [--bucket-type <type>] [--retention <n>] [--bucket-span <n>] [--rolling-window-snapshots <n>] [--export-accepted]");
        System.out.println("  run-spec --spec <properties-file>");
        System.out.println("  decide --history <path> --recommendation-id <id> --decision <PROPOSED|ACCEPTED|REJECTED|SUPERSEDED> [--note <text>] [--export-dir <path>]");
        System.out.println("  export-accepted --history <path> --output-dir <path>");
    }

    private static final class CliArgs {
        private final java.util.Map<String, String> values = new java.util.LinkedHashMap<>();

        void put(String key, String value) {
            values.put(key, value);
        }

        String required(String key) {
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing required --" + key);
            }
            return value;
        }

        String getOrDefault(String key, String fallback) {
            return values.getOrDefault(key, fallback);
        }

        boolean has(String key) {
            return values.containsKey(key);
        }
    }
}
