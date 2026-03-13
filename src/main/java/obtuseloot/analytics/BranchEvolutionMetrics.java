package obtuseloot.analytics;

import java.util.Map;

public record BranchEvolutionMetrics(
        Map<String, Integer> branchCounts,
        Map<String, Double> branchSurvivalRates,
        Map<String, Integer> driftWindowRemaining,
        Map<String, Double> averageSurvivalScore,
        Map<String, Double> averageMaintenanceCost,
        Map<String, Double> birthToCollapseRatio
) {
}
