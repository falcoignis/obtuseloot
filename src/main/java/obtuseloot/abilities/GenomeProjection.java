package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;

import java.util.Map;

public final class GenomeProjection {
    private final ProjectionCacheKey key;
    private final Map<String, Double> abilityScores;
    private final long computeNanos;
    private final GenomeVector genomeVector;

    public GenomeProjection(ProjectionCacheKey key, Map<String, Double> abilityScores, long computeNanos) {
        this.key = key;
        this.abilityScores = Map.copyOf(abilityScores);
        this.computeNanos = computeNanos;
        this.genomeVector = null;
    }

    private GenomeProjection(GenomeVector genomeVector) {
        this.key = null;
        this.abilityScores = Map.of();
        this.computeNanos = 0L;
        this.genomeVector = genomeVector;
    }

    public static GenomeProjection fromGenome(ArtifactGenome genome) {
        return new GenomeProjection(GenomeVector.fromGenome(genome));
    }

    public double dot(AbilityTraitVector abilityVector) {
        return genomeVector == null ? 0.0D : genomeVector.dot(abilityVector);
    }

    public ProjectionCacheKey key() {
        return key;
    }

    public Map<String, Double> abilityScores() {
        return abilityScores;
    }

    public long computeNanos() {
        return computeNanos;
    }
}
