package obtuseloot.telemetry;

import java.util.Map;

public record NichePopulationRollup(
        long generatedAtMs,
        Map<String, Long> populationByNiche,
        Map<String, Long> meaningfulOutcomesByNiche,
        Map<String, Double> utilityDensityByNiche,
        Map<String, Double> saturationPressureByNiche,
        Map<String, Double> opportunityShareByNiche,
        Map<String, Double> specializationPressureByNiche,
        Map<String, Long> branchContributionByNiche
) {
}
