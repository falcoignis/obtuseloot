package obtuseloot.analytics.ecosystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EcosystemAnomalyDetector {

    public EcosystemAnomalyReport detect(NicheEvolutionReport nicheReport, LineageSuccessReport lineageReport) {
        List<String> runawayLineages = new ArrayList<>(lineageReport.runawayLineages());
        List<String> nicheCollapse = new ArrayList<>(nicheReport.collapsingNiches());

        List<String> stagnation = lineageReport.specializationCascadeRiskByLineage().entrySet().stream()
                .filter(e -> e.getValue() < 0.02D)
                .map(Map.Entry::getKey)
                .toList();

        List<String> deadZones = nicheReport.utilityDensityTrendByNiche().entrySet().stream()
                .filter(e -> e.getValue() < -0.6D)
                .map(Map.Entry::getKey)
                .toList();

        List<String> branchExplosion = lineageReport.branchSurvivalByLineage().entrySet().stream()
                .filter(e -> e.getValue() > 3.0D)
                .map(Map.Entry::getKey)
                .toList();

        Map<String, String> diagnostics = new LinkedHashMap<>();
        diagnostics.put("runaway_lineage", runawayLineages.isEmpty() ? "none" : "lineage momentum overshoot");
        diagnostics.put("niche_collapse", nicheCollapse.isEmpty() ? "none" : "competition saturation exceeds adaptation");
        diagnostics.put("mutation_stagnation", stagnation.isEmpty() ? "none" : "specialization trajectory is effectively flat");
        diagnostics.put("dead_zones", deadZones.isEmpty() ? "none" : "utility density is degrading in stable niches");
        diagnostics.put("branch_explosion", branchExplosion.isEmpty() ? "none" : "branch amplification outpaces collapse");

        return new EcosystemAnomalyReport(runawayLineages, nicheCollapse, stagnation, deadZones, branchExplosion,
                Map.copyOf(diagnostics));
    }
}
