package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

public final class GenomeProjection {
    private final double[] components;

    private GenomeProjection(double[] components) {
        this.components = components;
    }

    public static GenomeProjection fromGenome(ArtifactGenome genome) {
        double[] vector = new double[GenomeTrait.values().length];
        for (GenomeTrait trait : GenomeTrait.values()) {
            vector[trait.ordinal()] = genome.trait(trait);
        }
        return new GenomeProjection(vector);
    }

    public double dot(AbilityTraitVector abilityVector) {
        double score = 0.0D;
        for (int i = 0; i < components.length; i++) {
            score += components[i] * abilityVector.component(i);
        }
        return score;
    }

    double component(int index) {
        return components[index];
    }
}
