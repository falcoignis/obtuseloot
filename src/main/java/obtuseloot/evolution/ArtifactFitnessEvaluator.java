package obtuseloot.evolution;

public class ArtifactFitnessEvaluator {
    // Decision hierarchy (utility-first, auditable):
    // 1) validated utility, 2) utility density/outcome efficiency,
    // 3) redundancy+spam/no-op penalties, 4) budget efficiency,
    // 5) legacy activity and longevity as secondary confidence support.
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
        double noisyVolumePenalty = Math.max(0.0D, usage.usageFrequency() - 6.0D) * 0.08D;
        double legacyConfidenceSupport = Math.min(0.45D, (usage.usageFrequency() * 0.015D) + (usage.lifetimeHours() * 0.01D));

        return (validatedUtility * 2.4D)
                + (utilityDensity * 5.2D)
                + (budgetEfficiency * 2.9D)
                + (meaningfulRate * 2.4D)
                + (contextualRelevance * 1.3D)
                + (usage.killParticipation() * 0.25D)
                + legacyConfidenceSupport
                - pressurePenalty
                - noisyVolumePenalty;
    }

    public double effectiveFitness(double fitness, int nichePopulation) {
        return fitness / Math.max(1, nichePopulation);
    }

    public double effectiveFitness(double fitness, RolePressureMetrics pressureMetrics) {
        if (pressureMetrics == null) {
            return fitness;
        }
        double adjusted = fitness * pressureMetrics.retentionBias();
        adjusted *= (1.0D + (pressureMetrics.specializationPressure() * 0.4D));
        adjusted *= (1.0D - (pressureMetrics.ecologicalRepulsion() * 0.45D));
        return adjusted;
    }

    public String decisionHierarchy() {
        return "validatedUtility > utilityDensity > noOpSpamRedundancyPenalty > budgetEfficiency > legacyActivityConfidence";
    }

}
