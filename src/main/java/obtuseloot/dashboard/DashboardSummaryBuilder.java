package obtuseloot.dashboard;

import java.util.Locale;

public class DashboardSummaryBuilder {
    public String build(DashboardMetrics metrics) {
        return "Dominance=" + fmt(metrics.dominanceIndex())
                + ", BranchEntropy=" + fmt(metrics.branchEntropy())
                + ", TraitVariance=" + fmt(metrics.traitVariance())
                + ", LineageConcentration=" + fmt(metrics.lineageConcentration())
                + ", CollapseRisk=" + metrics.collapseRisk().name();
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
