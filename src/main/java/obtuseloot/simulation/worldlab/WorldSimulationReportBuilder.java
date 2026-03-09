package obtuseloot.simulation.worldlab;

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
        sb.append("Conservative balancing recommendations from world-scale simulation data:\n\n");
        report.recommendations().forEach(r -> sb.append("- [").append(r.severity()).append("] ").append(r.category())
                .append(": ").append(r.evidence()).append(" -> ").append(r.suggestion()).append("\n"));
        return sb.toString();
    }

    private Object world(Map<String, Object> data, String key) {
        return ((Map<?, ?>) data.get("world")).get(key);
    }
}
