package obtuseloot.abilities;

public record TraitProjectionStats(
        boolean optimizedEnabled,
        long scoringCalls,
        long cacheHits,
        long cacheMisses,
        int cacheSize,
        int cacheCapacity,
        int abilityVectorCount,
        int dimensions,
        double averageScoringMicros,
        double estimatedSpeedupX
) {
    public double cacheHitRate() {
        long total = cacheHits + cacheMisses;
        return total == 0 ? 0.0D : (double) cacheHits / total;
    }
}
