package obtuseloot.analytics.performance;

import obtuseloot.abilities.ScoringMode;

import java.util.List;

public record TraitScoringBenchmarkConfig(
        long seed,
        int seedPoolSize,
        List<Integer> workloads,
        int picks,
        int paritySampleSize,
        int worldPlayers,
        int worldSeasons,
        int worldSessionsPerSeason,
        List<ScoringMode> scoringModes
) {
    public static TraitScoringBenchmarkConfig defaults() {
        return new TraitScoringBenchmarkConfig(
                20260309L,
                2048,
                List.of(10_000, 100_000, 1_000_000),
                3,
                20_000,
                100,
                2,
                8,
                List.of(ScoringMode.BASELINE, ScoringMode.PROJECTION_NO_CACHE, ScoringMode.PROJECTION_WITH_CACHE)
        );
    }
}
