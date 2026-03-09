package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

public final class GenomeVector {
    private static final GenomeTrait[] ORDER = GenomeTrait.values();

    private final long seed;
    private final double[] components;

    private GenomeVector(long seed, double[] components) {
        this.seed = seed;
        this.components = components;
    }

    public static GenomeVector fromGenome(ArtifactGenome genome) {
        double[] vector = new double[ORDER.length];
        for (GenomeTrait trait : ORDER) {
            vector[trait.ordinal()] = genome.trait(trait);
        }
        return new GenomeVector(genome.seed(), vector);
    }

    public long seed() {
        return seed;
    }

    public int dimensions() {
        return components.length;
    }

    public double component(int index) {
        return components[index];
    }

    public double dot(AbilityTraitVector abilityVector) {
        double score = 0.0D;
        for (int i = 0; i < components.length; i++) {
            score += components[i] * abilityVector.component(i);
        }
        return score;
    }

    public long stableHash() {
        long hash = 1469598103934665603L;
        hash ^= seed;
        hash *= 1099511628211L;
        for (double component : components) {
            hash ^= Double.doubleToLongBits(component);
            hash *= 1099511628211L;
        }
        return hash;
    }
}
