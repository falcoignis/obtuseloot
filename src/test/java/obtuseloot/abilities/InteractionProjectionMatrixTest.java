package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionProjectionMatrixTest {
    @Test
    void sameTraitInteractionsAreSaturated() {
        InteractionProjectionMatrix matrix = new InteractionProjectionMatrix();
        double[][] weights = new double[GenomeTrait.values().length][GenomeTrait.values().length];
        weights[GenomeTrait.CHAOS_AFFINITY.ordinal()][GenomeTrait.CHAOS_AFFINITY.ordinal()] = 5.0D;
        matrix.register("ability", weights);

        ArtifactGenome genome = genomeWith(GenomeTrait.CHAOS_AFFINITY, 1.0D, GenomeTrait.STABILITY, 0.0D);
        double score = matrix.interactionScore("ability", GenomeVector.fromGenome(genome));

        assertTrue(score < 5.0D, "Saturation should reduce runaway same-trait amplification");
        assertEquals(1.5384615384615383D, score, 1.0E-9D);
    }

    @Test
    void crossTraitInteractionsRemainLinear() {
        InteractionProjectionMatrix matrix = new InteractionProjectionMatrix();
        double[][] weights = new double[GenomeTrait.values().length][GenomeTrait.values().length];
        weights[GenomeTrait.CHAOS_AFFINITY.ordinal()][GenomeTrait.STABILITY.ordinal()] = 2.0D;
        matrix.register("ability", weights);

        ArtifactGenome genome = genomeWith(GenomeTrait.CHAOS_AFFINITY, 0.5D, GenomeTrait.STABILITY, 0.4D);
        double score = matrix.interactionScore("ability", GenomeVector.fromGenome(genome));

        assertEquals(0.4D, score, 1.0E-9D);
    }

    private ArtifactGenome genomeWith(GenomeTrait first, double firstValue, GenomeTrait second, double secondValue) {
        EnumMap<GenomeTrait, Double> traits = new EnumMap<>(GenomeTrait.class);
        for (GenomeTrait trait : GenomeTrait.values()) {
            traits.put(trait, 0.0D);
        }
        traits.put(first, firstValue);
        traits.put(second, secondValue);
        return new ArtifactGenome(7L, traits);
    }
}
