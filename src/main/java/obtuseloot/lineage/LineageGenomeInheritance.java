package obtuseloot.lineage;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class LineageGenomeInheritance {
    private static final double NORMAL_MUTATION_RANGE = 0.05D;
    private static final double HIGH_VARIANCE_MUTATION_RANGE = 0.15D;
    private static final double LOW_PROBABILITY_MUTATION_CHANCE = 0.10D;

    public ArtifactGenome inherit(ArtifactLineage lineage, ArtifactGenome parentGenome, long childSeed) {
        EnumMap<GenomeTrait, Double> childTraits = new EnumMap<>(GenomeTrait.class);
        EnumMap<GenomeTrait, Double> childLatentTraits = new EnumMap<>(GenomeTrait.class);
        Random random = new Random(childSeed ^ parentGenome.seed() ^ lineage.lineageId().hashCode());
        Map<GenomeTrait, Double> lineageTraits = lineage.genomeTraits();
        boolean hasLineageGenome = lineage.generationIndex() > 0;

        double specializationBias = lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.SPECIALIZATION);
        double riskBias = lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.RISK_APPETITE);
        double reliabilityBias = lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.RELIABILITY);

        for (GenomeTrait trait : GenomeTrait.values()) {
            double parentTrait = parentGenome.trait(trait);
            if (hasLineageGenome && lineageTraits.containsKey(trait)) {
                parentTrait = lineageTraits.get(trait);
            }
            double mutationRange = random.nextDouble() < LOW_PROBABILITY_MUTATION_CHANCE ? HIGH_VARIANCE_MUTATION_RANGE : NORMAL_MUTATION_RANGE;
            double mutation = ((random.nextDouble() * 2.0D) - 1.0D) * mutationRange;
            mutation += traitDirectionalBias(trait, specializationBias, riskBias, reliabilityBias);
            childTraits.put(trait, clamp01(parentTrait + mutation));

            double latentBase = parentGenome.latentTrait(trait);
            double latentMutation = ((random.nextDouble() * 2.0D) - 1.0D) * (mutationRange * 0.4D);
            childLatentTraits.put(trait, clamp01(latentBase + latentMutation));
        }

        ArtifactGenome childGenome = new ArtifactGenome(childSeed, childTraits, childLatentTraits, parentGenome.activatedLatentTraits());
        lineage.registerGenome(childSeed, childGenome);
        return childGenome;
    }

    private double traitDirectionalBias(GenomeTrait trait, double specializationBias, double riskBias, double reliabilityBias) {
        return switch (trait) {
            case STABILITY, RESONANCE, PRECISION_AFFINITY -> (reliabilityBias * 0.014D) + (specializationBias * 0.006D);
            case VOLATILITY, MUTATION_SENSITIVITY, CHAOS_AFFINITY -> (riskBias * 0.016D) - (reliabilityBias * 0.006D);
            case KINETIC_BIAS, MOBILITY_AFFINITY -> riskBias * 0.010D;
            case SURVIVAL_INSTINCT -> specializationBias * 0.010D;
        };
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
