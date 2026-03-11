package obtuseloot.lineage;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class LineageGenomeInheritance {
    private static final double NORMAL_MUTATION_RANGE = 0.05D;
    private static final double RARE_MUTATION_RANGE = 0.15D;
    private static final double RARE_MUTATION_CHANCE = 0.10D;

    public ArtifactGenome inherit(ArtifactLineage lineage, ArtifactGenome parentGenome, long childSeed) {
        EnumMap<GenomeTrait, Double> childTraits = new EnumMap<>(GenomeTrait.class);
        EnumMap<GenomeTrait, Double> childLatentTraits = new EnumMap<>(GenomeTrait.class);
        Random random = new Random(childSeed ^ parentGenome.seed() ^ lineage.lineageId().hashCode());
        Map<GenomeTrait, Double> lineageTraits = lineage.genomeTraits();
        boolean hasLineageGenome = lineage.generationIndex() > 0;

        for (GenomeTrait trait : GenomeTrait.values()) {
            double parentTrait = parentGenome.trait(trait);
            if (hasLineageGenome && lineageTraits.containsKey(trait)) {
                parentTrait = lineageTraits.get(trait);
            }
            double mutationRange = random.nextDouble() < RARE_MUTATION_CHANCE ? RARE_MUTATION_RANGE : NORMAL_MUTATION_RANGE;
            double mutation = ((random.nextDouble() * 2.0D) - 1.0D) * mutationRange;
            childTraits.put(trait, clamp01(parentTrait + mutation));
            double latentBase = parentGenome.latentTrait(trait);
            double latentMutation = ((random.nextDouble() * 2.0D) - 1.0D) * (mutationRange * 0.4D);
            childLatentTraits.put(trait, clamp01(latentBase + latentMutation));
        }

        ArtifactGenome childGenome = new ArtifactGenome(childSeed, childTraits, childLatentTraits, parentGenome.activatedLatentTraits());
        lineage.registerGenome(childSeed, childGenome);
        return childGenome;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
