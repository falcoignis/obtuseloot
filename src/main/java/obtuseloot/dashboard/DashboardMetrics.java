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
        int latestPnnc,
        List<Double> endTrend,
        List<Double> tntTrend,
        List<Double> nserTrend,
        List<Integer> pnncTrend,
        String nserInterpretation,
        EcosystemStatus ecosystemStatus,
        EcologyDiagnosticState diagnosticState,
        double diagnosticConfidence,
        List<String> diagnosticWarningFlags,
        boolean ecologicalMemoryActive,
        double dominantAttractorDuration,
        double memoryPressureMagnitude,
        java.util.List<DashboardDataSourceDescriptor> dataSources
) {
    public enum CollapseRisk {
        LOW,
        MEDIUM,
        HIGH
    }
}
