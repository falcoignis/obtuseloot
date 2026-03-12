package obtuseloot.lineage;

public class InheritanceBranchingHeuristics {
    private static final double DIVERGENCE_THRESHOLD = 0.18D;

    public boolean shouldBranch(EvolutionaryBiasGenome lineageBias,
                                EvolutionaryBiasGenome observedBias,
                                int repeatedDivergences,
                                double ecologicalPressure) {
        if (lineageBias == null || observedBias == null) {
            return false;
        }
        double distance = distance(lineageBias, observedBias);
        double ecologicalBoost = Math.max(0.0D, ecologicalPressure - 1.0D) * 0.05D;
        return repeatedDivergences >= 3 && distance > (DIVERGENCE_THRESHOLD - ecologicalBoost);
    }

    public double distance(EvolutionaryBiasGenome left, EvolutionaryBiasGenome right) {
        double sum = 0.0D;
        for (LineageBiasDimension dimension : LineageBiasDimension.values()) {
            double delta = left.tendency(dimension) - right.tendency(dimension);
            sum += delta * delta;
        }
        return Math.sqrt(sum / LineageBiasDimension.values().length);
    }
}
