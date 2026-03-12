package obtuseloot.evolution;

public class ArtifactFitnessEvaluator {
    public double evaluate(ArtifactUsageProfile usage) {
        double validatedUtility = usage.validatedUtilityScore();
        double utilityDensity = usage.utilityDensity();
        double meaningfulRate = usage.meaningfulOutcomeRate();
        double contextualRelevance = usage.averageContextualRelevance();
        double budgetEfficiency = usage.utilityBudgetEfficiency();

        double pressurePenalty = (usage.averageNoOpRate() * 2.4D)
                + (usage.averageSpamPenalty() * 1.9D)
                + (usage.averageRedundancyPenalty() * 1.6D)
                + (usage.discardRate() * 0.8D);
        double noisyVolumePenalty = Math.max(0.0D, usage.usageFrequency() - 6.0D) * 0.05D;

        return (validatedUtility * 1.9D)
                + (utilityDensity * 4.0D)
                + (budgetEfficiency * 2.3D)
                + (meaningfulRate * 2.0D)
                + (contextualRelevance * 1.3D)
                + (usage.killParticipation() * 0.35D)
                + (usage.lifetimeHours() * 0.06D)
                - pressurePenalty
                - noisyVolumePenalty;
    }

    public double effectiveFitness(double fitness, int nichePopulation) {
        return fitness / Math.max(1, nichePopulation);
    }
}
