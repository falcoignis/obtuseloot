package obtuseloot.evolution;

import obtuseloot.abilities.genome.*;
import obtuseloot.ecosystem.EnvironmentPressureEngine;
import obtuseloot.evolution.params.EvolutionParameterRegistry;
import obtuseloot.lineage.LineageRegistry;
import java.util.EnumMap;

/** Test-only control copied from c04a211e617c4d10d190f60a3abccb5f8fe0662f. */
public final class LegacyExperienceEvolutionEngine extends ExperienceEvolutionEngine {
    private final ArtifactUsageTracker tracker;
    private final ArtifactFitnessEvaluator evaluator;
    private final EnvironmentPressureEngine environment;
    private final EvolutionParameterRegistry parameters;

    public LegacyExperienceEvolutionEngine(ArtifactUsageTracker tracker, ArtifactFitnessEvaluator evaluator,
                                           EnvironmentPressureEngine environment) {
        this(tracker, evaluator, environment, new EvolutionParameterRegistry());
    }
    private LegacyExperienceEvolutionEngine(ArtifactUsageTracker tracker, ArtifactFitnessEvaluator evaluator,
                                            EnvironmentPressureEngine environment, EvolutionParameterRegistry parameters) {
        super(tracker, evaluator, environment, new AdaptiveSupportAllocator(), parameters);
        this.tracker = tracker; this.evaluator = evaluator; this.environment = environment; this.parameters = parameters;
    }

    @Override public ArtifactGenome applyExperienceFeedback(ArtifactGenome genome, long seed, int population,
            double mutationInfluence, String lineageId, LineageRegistry lineages, ArtifactUsageProfile usage) {
        var tuning = parameters.profile();
        var pressure = tracker.nichePopulationTracker().pressureFor(seed);
        var support = adaptiveSupportFor(seed, lineageId, lineages);
        double fitness = evaluator.evaluate(usage);
        double effective = evaluator.effectiveFitness(fitness / Math.max(1.0,
                tracker.nichePopulationTracker().nicheCompetitionFactor(seed)), pressure);
        effective *= support.reinforcementMultiplier() * tuning.lineageMomentumInfluence();
        effective *= tracker.nichePopulationTracker().nicheAdoptionFitnessMultiplier(seed);
        double normalized = clamp(effective / 8.0, -1, 1);
        double influence = clamp(mutationInfluence, .75, 1.25) * support.mutationOpportunity();
        var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        var latent = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        for (var entry : genome.traits().entrySet()) {
            double direction = switch (entry.getKey()) {
                case PRECISION_AFFINITY, MOBILITY_AFFINITY, SURVIVAL_INSTINCT, RESONANCE, STABILITY -> 1;
                case CHAOS_AFFINITY, VOLATILITY, KINETIC_BIAS, MUTATION_SENSITIVITY -> .5;
            };
            double boost = 1 + clamp(.10 * normalized * direction, -.10, .10);
            double supportBlend = .94 + support.retentionOpportunity() * .04 + support.diminishingReturnFactor() * .02;
            double ecological = clamp((.95 + (pressure.retentionBias() - 1) * .35
                    + pressure.specializationPressure() * .08 - pressure.ecologicalRepulsion() * .10)
                    * (1 + (influence - 1) * .25) * supportBlend, .90, 1.12);
            double multiplier = boost * ecological;
            traits.put(entry.getKey(), clamp(entry.getValue() * multiplier * environment.multiplierFor(entry.getKey()), 0, 1));
            latent.put(entry.getKey(), clamp(genome.latentTrait(entry.getKey()) * (1 + (multiplier - 1) * .35), 0, 1));
        }
        return new ArtifactGenome(genome.seed(), traits, latent, genome.activatedLatentTraits());
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
