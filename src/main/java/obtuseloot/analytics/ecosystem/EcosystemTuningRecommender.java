package obtuseloot.analytics.ecosystem;

import java.util.LinkedHashMap;
import java.util.Map;

public class EcosystemTuningRecommender {

    public TuningProfileRecommendation recommend(NicheEvolutionReport nicheReport,
                                                 LineageSuccessReport lineageReport,
                                                 EcosystemAnomalyReport anomalyReport) {
        Map<String, Double> adjustments = new LinkedHashMap<>();

        adjustments.put("niche_saturation_sensitivity",
                anomalyReport.nicheCollapse().isEmpty() ? -0.05D : 0.20D);

        boolean stagnationDetected = !anomalyReport.mutationStagnationLineages().isEmpty();
        adjustments.put("mutation_amplitude_min", stagnationDetected ? 0.12D : 0.08D);
        adjustments.put("mutation_amplitude_max", !anomalyReport.branchExplosionLineages().isEmpty() ? 0.22D : 0.30D);

        boolean runawayDetected = !anomalyReport.runawayLineages().isEmpty() || !nicheReport.runawayNiches().isEmpty();
        adjustments.put("lineage_momentum_decay", runawayDetected ? 0.35D : 0.18D);

        double averagePressure = nicheReport.competitionPressureByNiche().values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0D);
        adjustments.put("competition_reinforcement_scaling",
                averagePressure > 0.33D ? 0.75D : 1.05D);

        String rationale = "Profile computed from niche/lineage trends: collapse=>saturation sensitivity increase, "
                + "runaway=>higher momentum decay, stagnation=>raise mutation floor, branch explosion=>lower mutation ceiling.";
        return new TuningProfileRecommendation("phase6-ecosystem-stabilization", Map.copyOf(adjustments), rationale);
    }
}
