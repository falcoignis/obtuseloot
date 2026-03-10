package obtuseloot.dashboard;

import obtuseloot.analytics.EcosystemStatus;
import obtuseloot.analytics.EcologyDiagnosticState;

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
        double latestNser,
        List<Double> endTrend,
        List<Double> tntTrend,
        List<Double> nserTrend,
        String nserInterpretation,
        EcosystemStatus ecosystemStatus,
        EcologyDiagnosticState diagnosticState,
        double diagnosticConfidence,
        List<String> diagnosticWarningFlags
) {
    public enum CollapseRisk {
        LOW,
        MEDIUM,
        HIGH
    }
}
