package obtuseloot.evolution;

import obtuseloot.abilities.genome.*;
import obtuseloot.ecosystem.EnvironmentPressureEngine;
import obtuseloot.lineage.LineageRegistry;
import java.util.EnumMap;

/** Test-only transition transform frozen from db1928072f7c861426ecab0abdd29a211ce2140f (#417). */
public class LegacyEcologicalExperienceEvolutionEngine extends ExperienceEvolutionEngine {
    private final ArtifactUsageTracker tracker;
    private final EnvironmentPressureEngine environment;

    public LegacyEcologicalExperienceEvolutionEngine(ArtifactUsageTracker tracker, ArtifactFitnessEvaluator evaluator,
                                                     EnvironmentPressureEngine environment) {
        super(tracker, evaluator, environment);
        this.tracker = tracker;
        this.environment = environment;
    }

    @Override public ArtifactGenome applyExperienceFeedback(ArtifactGenome genome, long seed, int population,
            double mutationInfluence, String lineageId, LineageRegistry lineages, ArtifactUsageProfile usage) {
        var pressure = tracker.nichePopulationTracker().pressureFor(seed);
        var support = adaptiveSupportFor(seed, lineageId, lineages);
        double influence = clamp(mutationInfluence, .75, 1.25) * support.mutationOpportunity();
        double supportBlend = .94 + support.retentionOpportunity() * .04 + support.diminishingReturnFactor() * .02;
        double multiplier = clamp((.95 + (pressure.retentionBias() - 1) * .35
                + pressure.specializationPressure() * .08 - pressure.ecologicalRepulsion() * .10)
                * (1 + (influence - 1) * .25) * supportBlend, .90, 1.12);
        var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        var latent = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        for (var entry : genome.traits().entrySet()) {
            traits.put(entry.getKey(), clamp(entry.getValue() * multiplier * environment.multiplierFor(entry.getKey()), 0, 1));
            latent.put(entry.getKey(), clamp(genome.latentTrait(entry.getKey()) * (1 + (multiplier - 1) * .35), 0, 1));
        }
        return new ArtifactGenome(genome.seed(), traits, latent, genome.activatedLatentTraits());
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
