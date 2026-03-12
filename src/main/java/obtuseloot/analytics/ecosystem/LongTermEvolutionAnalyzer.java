package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.EcosystemSnapshot;
import obtuseloot.telemetry.TelemetryRollupSnapshot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LongTermEvolutionAnalyzer {

    public LongTermEvolutionReport analyze(List<TelemetryRollupSnapshot> history) {
        if (history == null || history.size() < 3) {
            return new LongTermEvolutionReport(0.0D, Map.of(), List.of(), 0.0D,
                    "Insufficient historical rollups for long-term analysis.");
        }

        List<EcosystemSnapshot> snapshots = history.stream().map(TelemetryRollupSnapshot::ecosystemSnapshot).toList();
        double turnover = snapshots.stream().mapToDouble(EcosystemSnapshot::turnoverRate).average().orElse(0.0D);

        Map<String, Long> lifespanWindows = lineageLifespan(snapshots);
        List<String> emergingNiches = emergingNiches(snapshots);

        EcosystemSnapshot first = snapshots.getFirst();
        EcosystemSnapshot last = snapshots.getLast();
        double adaptationCycleStrength = (last.diversityIndex() - first.diversityIndex())
                + (last.turnoverRate() - first.turnoverRate());

        String summary = "Turnover=" + format(turnover)
                + ", adaptationCycleStrength=" + format(adaptationCycleStrength)
                + ", emergingNiches=" + emergingNiches;

        return new LongTermEvolutionReport(turnover, lifespanWindows, emergingNiches, adaptationCycleStrength, summary);
    }

    private Map<String, Long> lineageLifespan(List<EcosystemSnapshot> snapshots) {
        Map<String, Long> appearances = new LinkedHashMap<>();
        for (int i = 0; i < snapshots.size(); i++) {
            for (String lineage : snapshots.get(i).lineagePopulationRollup().populationByLineage().keySet()) {
                appearances.merge(lineage, 1L, Long::sum);
            }
        }
        return Map.copyOf(appearances);
    }

    private List<String> emergingNiches(List<EcosystemSnapshot> snapshots) {
        int split = snapshots.size() / 2;
        var early = snapshots.subList(0, split).stream()
                .flatMap(s -> s.nichePopulationRollup().populationByNiche().keySet().stream())
                .collect(Collectors.toSet());
        return snapshots.subList(split, snapshots.size()).stream()
                .flatMap(s -> s.nichePopulationRollup().populationByNiche().keySet().stream())
                .filter(niche -> !early.contains(niche))
                .distinct()
                .toList();
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
