package obtuseloot.dashboard;

public record DashboardMetrics(
        double dominanceIndex,
        double branchEntropy,
        double lineageConcentration,
        double traitVariance,
        CollapseRisk collapseRisk
) {
    public enum CollapseRisk {
        LOW,
        MEDIUM,
        HIGH
    }
}
