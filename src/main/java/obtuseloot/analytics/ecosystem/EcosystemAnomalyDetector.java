package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.TelemetryRollupSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EcosystemAnomalyDetector {

    public EcosystemAnomalyReport detect(NicheEvolutionReport nicheReport, LineageSuccessReport lineageReport) {
        return detect(nicheReport, lineageReport, List.of());
    }

    public EcosystemAnomalyReport detect(NicheEvolutionReport nicheReport,
                                         LineageSuccessReport lineageReport,
                                         List<TelemetryRollupSnapshot> history) {
        Map<String, Double> baseline = computeBaselines(history);

        List<String> runawayLineages = lineageReport.successRateByLineage().entrySet().stream()
                .filter(e -> e.getValue() > baseline.getOrDefault("runaway_success_rate_threshold", 2.5D))
                .map(Map.Entry::getKey)
                .toList();

        List<String> nicheCollapse = nicheReport.populationTrendByNiche().entrySet().stream()
                .filter(e -> e.getValue() < baseline.getOrDefault("niche_collapse_threshold", -0.5D))
                .map(Map.Entry::getKey)
                .toList();

        List<String> stagnation = lineageReport.specializationCascadeRiskByLineage().entrySet().stream()
                .filter(e -> e.getValue() < baseline.getOrDefault("mutation_stagnation_threshold", 0.02D))
                .map(Map.Entry::getKey)
                .toList();

        List<String> deadZones = nicheReport.utilityDensityTrendByNiche().entrySet().stream()
                .filter(e -> e.getValue() < baseline.getOrDefault("dead_zone_threshold", -0.6D))
                .map(Map.Entry::getKey)
                .toList();

        List<String> branchExplosion = lineageReport.branchSurvivalByLineage().entrySet().stream()
                .filter(e -> e.getValue() > baseline.getOrDefault("branch_explosion_threshold", 3.0D))
                .map(Map.Entry::getKey)
                .toList();

        Map<String, String> diagnostics = new LinkedHashMap<>();
        diagnostics.put("runaway_lineage", runawayLineages.isEmpty() ? "none" : "lineage momentum exceeds historical percentile baseline");
        diagnostics.put("niche_collapse", nicheCollapse.isEmpty() ? "none" : "niche trajectory drops below window-relative baseline");
        diagnostics.put("mutation_stagnation", stagnation.isEmpty() ? "none" : "specialization shift under lineage-specific expectation");
        diagnostics.put("dead_zones", deadZones.isEmpty() ? "none" : "utility density trend violates rolling floor");
        diagnostics.put("branch_explosion", branchExplosion.isEmpty() ? "none" : "branch survival acceleration exceeds rolling baseline");

        double severity = (runawayLineages.size() * 1.2D + nicheCollapse.size() * 1.0D + stagnation.size() * 0.8D
                + deadZones.size() * 0.7D + branchExplosion.size() * 1.0D)
                / Math.max(1.0D, lineageReport.successRateByLineage().size() + nicheReport.populationTrendByNiche().size());

        return new EcosystemAnomalyReport(
                List.copyOf(runawayLineages),
                List.copyOf(nicheCollapse),
                List.copyOf(stagnation),
                List.copyOf(deadZones),
                List.copyOf(branchExplosion),
                Map.copyOf(diagnostics),
                Map.copyOf(baseline),
                severity);
    }

    private Map<String, Double> computeBaselines(List<TelemetryRollupSnapshot> history) {
        if (history == null || history.size() < 3) {
            return Map.of(
                    "runaway_success_rate_threshold", 2.5D,
                    "niche_collapse_threshold", -0.5D,
                    "mutation_stagnation_threshold", 0.02D,
                    "dead_zone_threshold", -0.6D,
                    "branch_explosion_threshold", 3.0D
            );
        }

        List<Double> diversity = history.stream().map(s -> s.ecosystemSnapshot().diversityIndex()).sorted().toList();
        List<Double> turnover = history.stream().map(s -> s.ecosystemSnapshot().turnoverRate()).sorted().toList();
        double diversityP50 = percentile(diversity, 0.50D);
        double turnoverP75 = percentile(turnover, 0.75D);

        Map<String, Double> baseline = new LinkedHashMap<>();
        baseline.put("runaway_success_rate_threshold", 2.0D + (1.0D - diversityP50));
        baseline.put("niche_collapse_threshold", -0.35D - (turnoverP75 * 0.6D));
        baseline.put("mutation_stagnation_threshold", 0.015D + (turnoverP75 * 0.02D));
        baseline.put("dead_zone_threshold", -0.45D - ((1.0D - diversityP50) * 0.2D));
        baseline.put("branch_explosion_threshold", 2.5D + (turnoverP75 * 1.5D));
        baseline.put("diversity_p50", diversityP50);
        baseline.put("turnover_p75", turnoverP75);
        return Map.copyOf(baseline);
    }

    private double percentile(List<Double> values, double percentile) {
        if (values.isEmpty()) {
            return 0.0D;
        }
        int index = (int) Math.floor((values.size() - 1) * percentile);
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }
}
