package obtuseloot.dashboard;

import obtuseloot.analytics.EcosystemStatus;

import java.util.List;

public record DashboardMetrics(
        double dominanceIndex,
        double branchEntropy,
        double lineageConcentration,
        double traitVariance,
        CollapseRisk collapseRisk,
        double endArtifacts,
        Double endSpecies,
        double latestTnt,
        List<Double> endTrend,
        List<Double> tntTrend,
        EcosystemStatus ecosystemStatus
) {
    public enum CollapseRisk {
        LOW,
        MEDIUM,
        HIGH
    }
}
