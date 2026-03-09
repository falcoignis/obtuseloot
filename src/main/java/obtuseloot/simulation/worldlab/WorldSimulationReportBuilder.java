package obtuseloot.simulation.worldlab;

import obtuseloot.analytics.BalanceRecommendation;
import obtuseloot.analytics.EcosystemHealthReport;

import java.util.List;
import java.util.Map;

public class WorldSimulationReportBuilder {
    public String reportMarkdown(WorldSimulationConfig config, Map<String, Object> data) {
        return "# World Simulation Report\n\n"
                + "- Players: " + config.playerCount() + "\n"
                + "- Artifacts per player: " + config.artifactsPerPlayer() + "\n"
                + "- Seasons: " + config.seasonCount() + "\n"
                + "- Sessions/season: " + config.sessionsPerSeason() + "\n\n"
                + "## World outcomes\n"
                + "- Dominant family rate: " + world(data, "dominant_family_rate") + "\n"
                + "- Branch convergence rate: " + world(data, "branch_convergence_rate") + "\n"
                + "- Dead branch rate: " + world(data, "dead_branch_rate") + "\n"
                + "- Fusion adoption: " + world(data, "long_run_fusion_adoption") + "\n"
                + "- Awakening adoption: " + world(data, "long_run_awakening_adoption") + "\n";
    }

    public String metaShiftMarkdown(SimulationMetricsCollector metrics) {
        List<Double> dominant = metrics.dominantFamilyTimeline();
        int size = dominant.size();
        double early = size == 0 ? 0.0D : dominant.get(0);
        double mid = size < 3 ? early : dominant.get(size / 2);
        double late = size == 0 ? 0.0D : dominant.get(size - 1);
        return "# World Simulation Meta Shifts\n\n"
                + "## Early season meta\n"
                + "- Dominant family concentration: " + early + "\n"
                + "- Typically ability exploration and branch spread are highest.\n\n"
                + "## Mid season meta\n"
                + "- Dominant family concentration: " + mid + "\n"
                + "- Awakening prevalence starts to bend branch selection and memory specialization.\n\n"
                + "## Late season meta\n"
                + "- Dominant family concentration: " + late + "\n"
                + "- Fusion/drift pressure compounds; watch for convergence spikes and dead branches.\n";
    }

    public String balanceFindings(EcosystemHealthReport report) {
        StringBuilder sb = new StringBuilder("# World Simulation Balance Findings\n\n");
        sb.append("## Ranked recommendations from world-scale simulation data\n\n");
        for (BalanceRecommendation r : report.recommendations()) {
            sb.append("### ").append(r.issueSummary()).append(" [").append(r.severity()).append("]\n")
                    .append("1. Issue summary: ").append(r.issueSummary()).append("\n")
                    .append("2. Evidence: ").append(r.evidence()).append("\n")
                    .append("3. Confidence level: ").append(r.confidence()).append("\n")
                    .append("4. Estimated impact: ").append(r.estimatedImpact()).append("\n")
                    .append("5. Suggested response: ").append(r.suggestion()).append("\n")
                    .append("6. Act now or gather more simulation first: ").append(r.action()).append("\n\n");
        }
        return sb.toString();
    }


    public String lineageEvolutionMarkdown(Map<String, Object> data) {
        Map<?, ?> lineage = (Map<?, ?>) data.get("lineage");
        return "# Lineage Evolution\n\n"
                + "- Lineage count: " + lineage.get("lineage_count") + "\n"
                + "- Extinction rate: " + lineage.get("lineage_extinction_rate") + "\n"
                + "- Distribution: `" + lineage.get("lineage_distribution") + "`\n"
                + "- Depth: `" + lineage.get("lineage_depth_distribution") + "`\n";
    }

    private Object world(Map<String, Object> data, String key) {
        return ((Map<?, ?>) data.get("world")).get(key);
    }
}
