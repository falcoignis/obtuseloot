package obtuseloot.telemetry;

import java.util.Map;

public record LineagePopulationRollup(
        long generatedAtMs,
        Map<String, Long> populationByLineage,
        Map<String, Long> branchCountByLineage,
        Map<String, Double> utilityDensityByLineage,
        Map<String, Double> momentumByLineage,
        Map<String, Double> specializationTrajectoryByLineage,
        Map<String, Map<String, Long>> nicheDistributionByLineage,
        Map<String, Double> driftWindowRemainingByLineage,
        Map<String, Double> branchDivergenceByLineage
) {
}
