package obtuseloot.analytics.ecosystem;

import java.util.List;
import java.util.Map;

public record EcosystemAnomalyReport(
        List<String> runawayLineages,
        List<String> nicheCollapse,
        List<String> mutationStagnationLineages,
        List<String> ecologicalDeadZones,
        List<String> branchExplosionLineages,
        Map<String, String> diagnostics,
        Map<String, Double> baselineMetrics,
        double anomalySeverityScore
) {
}
