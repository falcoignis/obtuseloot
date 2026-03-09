package obtuseloot.analytics.performance;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TraitScoringBenchmarkMain {
    private TraitScoringBenchmarkMain() {
    }

    public static void main(String[] args) throws Exception {
        TraitScoringBenchmarkConfig config = TraitScoringBenchmarkConfig.defaults();
        TraitScoringBenchmarkRunner runner = new TraitScoringBenchmarkRunner(config);
        TraitScoringBenchmarkResult result = runner.runAll();
        BenchmarkReportBuilder reportBuilder = new BenchmarkReportBuilder();

        Path out = Path.of("analytics/performance");
        Files.createDirectories(out);
        Files.writeString(out.resolve("trait-scoring-benchmark.md"), reportBuilder.benchmarkMarkdown(result));
        Files.writeString(out.resolve("trait-scoring-benchmark.json"), reportBuilder.json(result));
        Files.writeString(out.resolve("trait-scoring-parity-report.md"), reportBuilder.parityMarkdown(result));
        Files.writeString(out.resolve("world-sim-scoring-benchmark.md"), reportBuilder.worldMarkdown(result));

        System.out.println("Benchmark reports written to analytics/performance");
    }
}
