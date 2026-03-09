package obtuseloot.evolution;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.EnumMap;
import java.util.Map;

public class ExperienceEvolutionEngine {
    private static final double ADJUSTMENT_CAP = 0.10D;

    private final ArtifactUsageTracker usageTracker;
    private final ArtifactFitnessEvaluator fitnessEvaluator;

    public ExperienceEvolutionEngine(ArtifactUsageTracker usageTracker, ArtifactFitnessEvaluator fitnessEvaluator) {
        this.usageTracker = usageTracker;
        this.fitnessEvaluator = fitnessEvaluator;
    }

    public ArtifactGenome applyExperienceFeedback(ArtifactGenome genome, long artifactSeed) {
        return applyExperienceFeedback(genome, artifactSeed, 1);
    }

    public ArtifactGenome applyExperienceFeedback(ArtifactGenome genome, long artifactSeed, int nichePopulation) {
        ArtifactUsageProfile usage = usageTracker.profileForSeed(artifactSeed);
        double fitness = fitnessEvaluator.evaluate(usage);
        double effectiveFitness = fitnessEvaluator.effectiveFitness(fitness, nichePopulation);
        double normalized = normalizeFitness(effectiveFitness);

        EnumMap<GenomeTrait, Double> adjusted = new EnumMap<>(GenomeTrait.class);
        for (Map.Entry<GenomeTrait, Double> entry : genome.traits().entrySet()) {
            double multiplier = traitMultiplier(entry.getKey(), normalized);
            adjusted.put(entry.getKey(), clamp01(entry.getValue() * multiplier));
        }
        return new ArtifactGenome(genome.seed(), adjusted);
    }

    private double traitMultiplier(GenomeTrait trait, double normalizedFitness) {
        double direction = switch (trait) {
            case PRECISION_AFFINITY, MOBILITY_AFFINITY, SURVIVAL_INSTINCT, RESONANCE, STABILITY -> 1.0D;
            case CHAOS_AFFINITY, VOLATILITY, KINETIC_BIAS, MUTATION_SENSITIVITY -> 0.5D;
        };
        double delta = ADJUSTMENT_CAP * normalizedFitness * direction;
        return 1.0D + clamp(delta, -ADJUSTMENT_CAP, ADJUSTMENT_CAP);
    }

    private double normalizeFitness(double fitness) {
        return clamp(fitness / 8.0D, -1.0D, 1.0D);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0D, 1.0D);
    }
}
