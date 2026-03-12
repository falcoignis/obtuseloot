package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.EcosystemSnapshot;
import obtuseloot.telemetry.TelemetryRollupSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EcosystemTrendAnalyzer {

    public NicheEvolutionReport analyze(List<TelemetryRollupSnapshot> rollupHistory) {
        if (rollupHistory == null || rollupHistory.size() < 2) {
            return new NicheEvolutionReport(Map.of(), Map.of(), Map.of(), List.of(), List.of(), 0.0D, 0.0D);
        }
        List<EcosystemSnapshot> snapshots = rollupHistory.stream()
                .map(TelemetryRollupSnapshot::ecosystemSnapshot)
                .toList();
        EcosystemSnapshot first = snapshots.getFirst();
        EcosystemSnapshot last = snapshots.getLast();

        Map<String, Double> populationTrend = trendMap(first.nichePopulationRollup().populationByNiche(),
                last.nichePopulationRollup().populationByNiche());
        Map<String, Double> utilityTrend = trendMap(first.nichePopulationRollup().utilityDensityByNiche(),
                last.nichePopulationRollup().utilityDensityByNiche());
        Map<String, Double> competitionPressure = normalizePressure(last.competitionPressureDistribution());

        List<String> runawayNiches = new ArrayList<>();
        List<String> collapsingNiches = new ArrayList<>();
        populationTrend.forEach((niche, trend) -> {
            if (trend > 1.8D && utilityTrend.getOrDefault(niche, 0.0D) > 0.1D) {
                runawayNiches.add(niche);
            }
            if (trend < -0.5D) {
                collapsingNiches.add(niche);
            }
        });

        return new NicheEvolutionReport(
                populationTrend,
                utilityTrend,
                competitionPressure,
                List.copyOf(runawayNiches),
                List.copyOf(collapsingNiches),
                last.diversityIndex() - first.diversityIndex(),
                last.turnoverRate() - first.turnoverRate());
    }

    private Map<String, Double> trendMap(Map<String, ? extends Number> first, Map<String, ? extends Number> last) {
        Map<String, Double> trend = new LinkedHashMap<>();
        last.forEach((key, end) -> {
            Number baseline = first.get(key);
            double start = Math.max(1.0D, baseline == null ? 0.0D : baseline.doubleValue());
            trend.put(key, (end.doubleValue() - start) / start);
        });
        return Map.copyOf(trend);
    }

    private Map<String, Double> normalizePressure(Map<String, Long> pressure) {
        long total = pressure.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0L) {
            return Map.of();
        }
        Map<String, Double> out = new LinkedHashMap<>();
        pressure.forEach((niche, value) -> out.put(niche, value / (double) total));
        return Map.copyOf(out);
    }
}
