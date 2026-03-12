package obtuseloot.analytics;

import java.util.Map;

public record LineagePopulationMetrics(
        Map<String, Integer> lineagePopulation,
        Map<String, Integer> lineageNicheDistribution,
        Map<String, Double> lineageNicheCrowding
) {
}

