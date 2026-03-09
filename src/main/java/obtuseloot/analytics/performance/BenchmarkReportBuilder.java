package obtuseloot.analytics.performance;

import java.util.*;

public class BenchmarkReportBuilder {
    public String benchmarkMarkdown(TraitScoringBenchmarkResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Trait Scoring Benchmark\n\n");
        sb.append("## 1. benchmark setup\n");
        sb.append("- Seed pool size: ").append(result.config().seedPoolSize()).append("\n");
        sb.append("- Picks per artifact: ").append(result.config().picks()).append("\n");
        sb.append("- Shared deterministic seed: ").append(result.config().seed()).append("\n\n");
        sb.append("## 2. scoring modes tested\n- BASELINE\n- PROJECTION_NO_CACHE\n- PROJECTION_WITH_CACHE\n\n");
        sb.append("## 3. workload sizes\n");
        for (Integer w : result.config().workloads()) sb.append("- ").append(w).append(" evaluations\n");
        sb.append("\n## 4. timing results\n");
        for (var e : result.timingByWorkload().entrySet()) {
            sb.append("\n### Workload ").append(e.getKey()).append("\n");
            sb.append("| mode | total ms | avg ns/artifact | throughput/s |\n|---|---:|---:|---:|\n");
            for (var run : e.getValue()) {
                sb.append("| ").append(run.mode()).append(" | ").append(String.format(Locale.ROOT, "%.2f", run.totalNanos() / 1_000_000.0D))
                        .append(" | ").append(String.format(Locale.ROOT, "%.2f", run.averageNanosPerArtifact()))
                        .append(" | ").append(String.format(Locale.ROOT, "%.2f", run.throughputPerSecond())).append(" |\n");
            }
        }
        sb.append("\n## 5. cache results\n");
        for (var e : result.timingByWorkload().entrySet()) {
            sb.append("\n### Workload ").append(e.getKey()).append("\n");
            sb.append("| mode | hits | misses | hit rate | size/capacity | evictions |\n|---|---:|---:|---:|---:|---:|\n");
            for (var run : e.getValue()) {
                sb.append("| ").append(run.mode()).append(" | ").append(run.cacheHits()).append(" | ").append(run.cacheMisses())
                        .append(" | ").append(String.format(Locale.ROOT, "%.2f%%", run.cacheHitRate() * 100.0D)).append(" | ")
                        .append(run.cacheSize()).append("/").append(run.cacheCapacity()).append(" | ")
                        .append(run.cacheEvictions()).append(" |\n");
            }
        }
        sb.append("\n## 6. parity results\n");
        for (var p : result.parityResults()) {
            sb.append("- ").append(p.baselineMode()).append(" vs ").append(p.candidateMode())
                    .append(": top1=").append(String.format(Locale.ROOT, "%.2f%%", p.top1ExactMatchRate() * 100.0D))
                    .append(", top3 exact=").append(String.format(Locale.ROOT, "%.2f%%", p.top3ExactMatchRate() * 100.0D))
                    .append(", top3 set=").append(String.format(Locale.ROOT, "%.2f%%", p.top3SetMatchRate() * 100.0D))
                    .append(", ordering consistency=").append(String.format(Locale.ROOT, "%.2f%%", p.orderingConsistencyRate() * 100.0D)).append("\n");
        }
        sb.append("\n## 7. interpretation\n");
        sb.append("- Performance gain is strongest where throughput improves while parity remains high.\n");
        sb.append("- Cache effectiveness depends on repeated genome state in workload and world sim loops.\n");
        sb.append("\n## 8. recommendation\n").append(recommendation(result)).append("\n");
        return sb.toString();
    }

    public String parityMarkdown(TraitScoringBenchmarkResult result) {
        StringBuilder sb = new StringBuilder("# Trait Scoring Parity Report\n\n");
        sb.append("| baseline | candidate | sample | top1 exact | top3 exact | top3 set | ordering | avg spread delta |\n|---|---|---:|---:|---:|---:|---:|---:|\n");
        for (var p : result.parityResults()) {
            sb.append("| ").append(p.baselineMode()).append(" | ").append(p.candidateMode()).append(" | ").append(p.sampleSize())
                    .append(" | ").append(String.format(Locale.ROOT, "%.2f%%", p.top1ExactMatchRate() * 100.0D))
                    .append(" | ").append(String.format(Locale.ROOT, "%.2f%%", p.top3ExactMatchRate() * 100.0D))
                    .append(" | ").append(String.format(Locale.ROOT, "%.2f%%", p.top3SetMatchRate() * 100.0D))
                    .append(" | ").append(String.format(Locale.ROOT, "%.2f%%", p.orderingConsistencyRate() * 100.0D))
                    .append(" | ").append(String.format(Locale.ROOT, "%.6f", p.averageSpreadDelta())).append(" |\n");
        }
        return sb.toString();
    }

    public String worldMarkdown(TraitScoringBenchmarkResult result) {
        StringBuilder sb = new StringBuilder("# World Simulation Scoring Benchmark\n\n");
        sb.append("| mode | players | seasons | total runtime ms | scoring runtime ms (est) | scoring calls | cache hit rate | cache size | evictions |\n|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (var w : result.worldSimResults()) {
            sb.append("| ").append(w.mode()).append(" | ").append(w.players()).append(" | ").append(w.seasons()).append(" | ").append(w.totalRuntimeMillis())
                    .append(" | ").append(String.format(Locale.ROOT, "%.2f", w.scoringRuntimeMillisEstimate()))
                    .append(" | ").append(w.scoringCalls())
                    .append(" | ").append(String.format(Locale.ROOT, "%.2f%%", w.cacheHitRate() * 100.0D))
                    .append(" | ").append(w.cacheSize())
                    .append(" | ").append(w.cacheEvictions()).append(" |\n");
        }
        return sb.toString();
    }

    public String json(TraitScoringBenchmarkResult result) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("config", result.config());
        out.put("timing", result.timingByWorkload());
        out.put("parity", result.parityResults());
        out.put("world", result.worldSimResults());
        return toJson(out, 0);
    }

    private String recommendation(TraitScoringBenchmarkResult result) {
        double withCache = avgThroughput(result, "PROJECTION_WITH_CACHE");
        double noCache = avgThroughput(result, "PROJECTION_NO_CACHE");
        double baseline = avgThroughput(result, "BASELINE");
        double withCacheParity = parity(result, "PROJECTION_WITH_CACHE");
        if (withCache > noCache && withCache > baseline && withCacheParity >= 0.98D) {
            return "- Use **PROJECTION_WITH_CACHE** for simulation and diagnostics; keep live gameplay default unchanged until more live validation.";
        }
        if (noCache > baseline) {
            return "- Use **PROJECTION_NO_CACHE** where cache hit rates are low; retain baseline as safe fallback for gameplay.";
        }
        return "- Keep **BASELINE** for live gameplay and use optimized modes only for controlled simulation runs.";
    }

    private double avgThroughput(TraitScoringBenchmarkResult result, String mode) {
        return result.timingByWorkload().values().stream().flatMap(List::stream)
                .filter(v -> v.mode().name().equals(mode))
                .mapToDouble(TraitScoringBenchmarkResult.ModeRun::throughputPerSecond)
                .average().orElse(0.0D);
    }

    private double parity(TraitScoringBenchmarkResult result, String mode) {
        return result.parityResults().stream().filter(p -> p.candidateMode().name().equals(mode))
                .mapToDouble(TraitScoringBenchmarkResult.ParityResult::top3SetMatchRate)
                .average().orElse(0.0D);
    }

    private String toJson(Object value, int indent) {
        String pad = "  ".repeat(indent);
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{\n");
            Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                sb.append(pad).append("  \"").append(e.getKey()).append("\": ").append(toJson(e.getValue(), indent + 1));
                if (it.hasNext()) sb.append(',');
                sb.append('\n');
            }
            return sb.append(pad).append('}').toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toJson(list.get(i), indent + 1));
            }
            return sb.append(']').toString();
        }
        if (value instanceof String s) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        }
        if (value == null) return "null";
        return String.valueOf(value);
    }
}
