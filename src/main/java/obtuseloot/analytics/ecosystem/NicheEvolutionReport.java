package obtuseloot.analytics.ecosystem;

import java.util.List;
import java.util.Map;

public record NicheEvolutionReport(
        Map<String, Double> populationTrendByNiche,
        Map<String, Double> utilityDensityTrendByNiche,
        Map<String, Double> competitionPressureByNiche,
        List<String> runawayNiches,
        List<String> collapsingNiches,
        double diversityIndexTrend,
        double turnoverTrend
) {
}
