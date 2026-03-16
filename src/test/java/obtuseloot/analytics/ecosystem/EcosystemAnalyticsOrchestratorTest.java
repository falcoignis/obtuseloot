package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EcosystemAnalyticsOrchestratorTest {

    @Test
    void producesTrendAnomalyTuningAndLongTermReportsFromRollupHistory() {
        TelemetryRollupSnapshot s1 = snapshot(1_000L,
                Map.of("SCOUT", 90L, "RITUAL", 80L),
                Map.of("SCOUT", 0.90D, "RITUAL", 0.65D),
                Map.of("lin-a", 80L, "lin-b", 100L),
                Map.of("lin-a", 2L, "lin-b", 3L),
                Map.of("lin-a", 0.02D, "lin-b", 0.01D),
                0.66D,
                0.18D,
                Map.of("SCOUT", 12L, "RITUAL", 9L));

        TelemetryRollupSnapshot s2 = snapshot(2_000L,
                Map.of("SCOUT", 120L, "RITUAL", 60L),
                Map.of("SCOUT", 1.0D, "RITUAL", 0.4D),
                Map.of("lin-a", 120L, "lin-b", 50L),
                Map.of("lin-a", 4L, "lin-b", 2L),
                Map.of("lin-a", 0.14D, "lin-b", 0.01D),
                0.70D,
                0.25D,
                Map.of("SCOUT", 18L, "RITUAL", 6L));

        TelemetryRollupSnapshot s3 = snapshot(3_000L,
                Map.of("SCOUT", 300L, "RITUAL", 20L, "FORGE", 30L),
                Map.of("SCOUT", 1.2D, "RITUAL", 0.2D, "FORGE", 0.7D),
                Map.of("lin-a", 250L, "lin-b", 20L),
                Map.of("lin-a", 8L, "lin-b", 1L),
                Map.of("lin-a", 0.45D, "lin-b", 0.01D),
                0.61D,
                0.30D,
                Map.of("SCOUT", 25L, "RITUAL", 3L, "FORGE", 4L));

        EcosystemAnalyticsReport report = new EcosystemAnalyticsOrchestrator().analyze(List.of(s1, s2, s3));

        assertTrue(report.nicheEvolutionReport().runawayNiches().contains("SCOUT"));
        assertTrue(report.nicheEvolutionReport().collapsingNiches().contains("RITUAL"));

        assertTrue(report.lineageSuccessReport().runawayLineages().contains("lin-a"));
        assertTrue(report.lineageSuccessReport().collapsingLineages().contains("lin-b"));

        assertFalse(report.anomalyReport().runawayLineages().isEmpty());
        assertFalse(report.anomalyReport().nicheCollapse().isEmpty());
        assertFalse(report.anomalyReport().mutationStagnationLineages().isEmpty());
        assertTrue(report.anomalyReport().baselineMetrics().containsKey("runaway_success_rate_threshold"));
        assertTrue(report.anomalyReport().anomalySeverityScore() >= 0.0D);

        assertEquals("phase6-ecosystem-stabilization", report.tuningProfileRecommendation().profileName());
        assertTrue(report.tuningProfileRecommendation().parameterAdjustments().containsKey("niche_saturation_sensitivity"));
        assertTrue(report.tuningProfileRecommendation().parameterAdjustments().containsKey("lineage_momentum_decay"));

        assertTrue(report.longTermEvolutionReport().ecosystemTurnover() > 0.0D);
        assertTrue(report.longTermEvolutionReport().lineageLifespanWindows().get("lin-a") >= 3L);
        assertTrue(report.longTermEvolutionReport().emergingNiches().contains("FORGE"));
    }

    @Test
    void schemaAnalyzerCoversAllTelemetryEventTypes() {
        Map<EcosystemTelemetryEventType, TelemetrySchemaAnalyzer.SchemaHealth> contracts =
                new TelemetrySchemaAnalyzer().analyzeContracts();

        assertEquals(EcosystemTelemetryEventType.values().length, contracts.size());
        assertTrue(contracts.values().stream().allMatch(TelemetrySchemaAnalyzer.SchemaHealth::contractBalanced));
    }

    private TelemetryRollupSnapshot snapshot(long ts,
                                             Map<String, Long> nichePopulation,
                                             Map<String, Double> nicheUtility,
                                             Map<String, Long> lineagePopulation,
                                             Map<String, Long> lineageBranches,
                                             Map<String, Double> specializationTrajectory,
                                             double diversity,
                                             double turnover,
                                             Map<String, Long> pressure) {
        NichePopulationRollup nicheRollup = new NichePopulationRollup(ts,
                nichePopulation,
                nichePopulation,
                nicheUtility,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of());

        LineagePopulationRollup lineageRollup = new LineagePopulationRollup(ts,
                lineagePopulation,
                lineageBranches,
                Map.of(),
                Map.of(),
                specializationTrajectory,
                Map.of(),
                Map.of(),
                Map.of());

        EcosystemSnapshot ecosystem = new EcosystemSnapshot(ts,
                Map.of(EcosystemTelemetryEventType.ABILITY_EXECUTION, 10L),
                nicheRollup,
                lineageRollup,
                nichePopulation.values().stream().mapToLong(Long::longValue).sum(),
                0.7D,
                diversity,
                turnover,
                lineageBranches.values().stream().mapToLong(Long::longValue).sum(),
                0L,
                pressure,
                List.of(),
                0L,
                Map.of());

        return new TelemetryRollupSnapshot(TelemetryRollupSnapshot.CURRENT_VERSION, ts, "test", ecosystem);
    }
}
