package obtuseloot.analytics.performance;

import obtuseloot.abilities.ScoringMode;

import java.util.List;
import java.util.Map;

public record TraitScoringBenchmarkResult(
        TraitScoringBenchmarkConfig config,
        Map<Integer, List<ModeRun>> timingByWorkload,
        List<ParityResult> parityResults,
        List<WorldSimResult> worldSimResults
) {
    public record ModeRun(
            ScoringMode mode,
            int workload,
            long totalNanos,
            double averageNanosPerArtifact,
            double throughputPerSecond,
            long cacheHits,
            long cacheMisses,
            double cacheHitRate,
            int cacheSize,
            int cacheCapacity,
            long cacheEvictions,
            long usedMemoryBytes,
            long estimatedPeakCacheMemoryBytes,
            Map<String, Integer> topAbilityDistribution,
            Map<String, Integer> topFamilyDistribution,
            double averageScoreSpread
    ) {}

    public record ParityResult(
            ScoringMode baselineMode,
            ScoringMode candidateMode,
            int sampleSize,
            double top1ExactMatchRate,
            double top3ExactMatchRate,
            double top3SetMatchRate,
            double orderingConsistencyRate,
            double averageSpreadDelta
    ) {}

    public record WorldSimResult(
            ScoringMode mode,
            int players,
            int seasons,
            long totalRuntimeMillis,
            long scoringCalls,
            double averageScoringMicros,
            double scoringRuntimeMillisEstimate,
            long cacheHits,
            long cacheMisses,
            double cacheHitRate,
            int cacheSize,
            long cacheEvictions
    ) {}
}
