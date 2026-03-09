package obtuseloot.evolution;

public class ArtifactFitnessEvaluator {
    public double evaluate(ArtifactUsageProfile usage) {
        return usage.usageFrequency()
                + usage.killParticipation()
                + usage.lifetimeHours()
                - usage.discardRate();
    }
}
