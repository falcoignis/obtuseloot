package obtuseloot.telemetry;

import java.util.List;
import java.util.Map;

public record EcosystemSnapshot(
        long generatedAtMs,
        Map<EcosystemTelemetryEventType, Long> eventCounts,
        NichePopulationRollup nichePopulationRollup,
        LineagePopulationRollup lineagePopulationRollup,
        long activeArtifactCount,
        double carryingCapacityUtilization,
        double diversityIndex,
        double turnoverRate,
        long branchBirthCount,
        long branchCollapseCount,
        Map<String, Long> competitionPressureDistribution,
        List<String> dynamicNiches,
        long bifurcationCount,
        Map<String, Long> dynamicNichePopulation
) {
}
