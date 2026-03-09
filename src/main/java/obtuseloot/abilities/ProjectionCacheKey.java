package obtuseloot.abilities;

public record ProjectionCacheKey(long genomeHash, long contextHash) {
    public static ProjectionCacheKey from(GenomeVector vector, long contextHash) {
        return new ProjectionCacheKey(vector.stableHash(), contextHash);
    }
}
