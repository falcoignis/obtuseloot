package obtuseloot.evolution;

public class ArtifactFitnessEvaluator {
    public double evaluate(ArtifactUsageProfile usage) {
        return usage.usageFrequency()
                + usage.killParticipation()
                + usage.lifetimeHours()
                - usage.discardRate();
    }

    public double effectiveFitness(double fitness, int nichePopulation) {
        return fitness / Math.max(1, nichePopulation);
    }
}
