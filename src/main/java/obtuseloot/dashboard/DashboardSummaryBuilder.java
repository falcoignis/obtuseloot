package obtuseloot.dashboard;

import java.util.Locale;

public class DashboardSummaryBuilder {
    public String build(DashboardMetrics metrics) {
        return "Dominance=" + fmt(metrics.dominanceIndex())
                + ", BranchEntropy=" + fmt(metrics.branchEntropy())
                + ", TraitVariance=" + fmt(metrics.traitVariance())
                + ", LineageConcentration=" + fmt(metrics.lineageConcentration())
                + ", CollapseRisk=" + metrics.collapseRisk().name()
                + ", END=" + fmt(metrics.endArtifacts())
                + ", TNT=" + fmt(metrics.latestTnt())
                + ", NSER=" + fmt(metrics.latestNser())
                + ", EcosystemStatus=" + metrics.ecosystemStatus().name()
                + ", EcologyDiagnostic=" + metrics.diagnosticState().name()
                + ", DiagnosticConfidence=" + fmt(metrics.diagnosticConfidence())
                + ", WarningFlags=" + metrics.diagnosticWarningFlags()
                + " | Data source: analytics/ecosystem-balance-data.json + analytics/ecosystem-health-gauge.json";
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
