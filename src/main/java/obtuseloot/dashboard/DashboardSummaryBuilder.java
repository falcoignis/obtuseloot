package obtuseloot.dashboard;

import java.util.Locale;
import java.util.stream.Collectors;

public class DashboardSummaryBuilder {
    public String build(DashboardMetrics metrics) {
        String sources = metrics.dataSources().stream()
                .map(source -> source.id() + "=" + source.sourceKind() + (source.authoritative() ? "(authoritative)" : "(context)"))
                .collect(Collectors.joining(", "));
        return "Dominance=" + fmt(metrics.dominanceIndex())
                + ", BranchEntropy=" + fmt(metrics.branchEntropy())
                + ", TraitVariance=" + fmt(metrics.traitVariance())
                + ", LineageConcentration=" + fmt(metrics.lineageConcentration())
                + ", CollapseRisk=" + metrics.collapseRisk().name()
                + ", END=" + fmt(metrics.endArtifacts())
                + ", TNT=" + fmt(metrics.latestTnt())
                + ", NSER=" + fmt(metrics.latestNser())
                + ", PNNC=" + metrics.latestPnnc()
                + ", EcosystemStatus=" + metrics.ecosystemStatus().name()
                + ", EcologyDiagnostic=" + metrics.diagnosticState().name()
                + ", DiagnosticConfidence=" + fmt(metrics.diagnosticConfidence())
                + ", WarningFlags=" + metrics.diagnosticWarningFlags()
                + " | Data sources: " + sources;
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
