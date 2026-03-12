package obtuseloot.analytics;

import java.util.Map;

public record BranchEvolutionMetrics(
        Map<String, Integer> branchCounts,
        Map<String, Double> branchSurvivalRates,
        Map<String, Integer> driftWindowRemaining
) {
}

