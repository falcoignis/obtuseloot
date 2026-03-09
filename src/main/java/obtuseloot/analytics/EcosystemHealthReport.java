package obtuseloot.analytics;

import java.util.List;
import java.util.Map;

public record EcosystemHealthReport(
        Map<String, Integer> familyDistribution,
        Map<String, Integer> branchDistribution,
        Map<String, Integer> mutationDistribution,
        Map<String, Integer> triggerDistribution,
        Map<String, Integer> mechanicDistribution,
        Map<String, Integer> memoryEventDistribution,
        List<BalanceRecommendation> recommendations
) {
}
