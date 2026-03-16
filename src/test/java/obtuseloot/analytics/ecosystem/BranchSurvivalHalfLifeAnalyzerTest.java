package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BranchSurvivalHalfLifeAnalyzerTest {

    @Test
    void computesHalfLifeFromLifecycleInactivityAndCollapseEvents() {
        BranchSurvivalHalfLifeAnalyzer analyzer = new BranchSurvivalHalfLifeAnalyzer();
        List<EcosystemTelemetryEvent> events = List.of(
                new EcosystemTelemetryEvent(1_000L, EcosystemTelemetryEventType.BRANCH_FORMATION, 1L, "lin", "SCOUT", Map.of("branch_id", "b1")),
                new EcosystemTelemetryEvent(1_100L, EcosystemTelemetryEventType.BRANCH_FORMATION, 2L, "lin", "SCOUT", Map.of("branch_id", "b2")),
                new EcosystemTelemetryEvent(2_100L, EcosystemTelemetryEventType.LINEAGE_UPDATE, 1L, "lin", "SCOUT", Map.of("event", "branch-lifecycle-transition", "branch_id", "b1", "lifecycle_state", "UNSTABLE")),
                new EcosystemTelemetryEvent(3_100L, EcosystemTelemetryEventType.LINEAGE_UPDATE, 2L, "lin", "SCOUT", Map.of("event", "branch-collapsed", "branch_id", "b2", "lifecycle_state", "COLLAPSING"))
        );

        List<TelemetryRollupSnapshot> rollups = List.of(snapshot(1_500L), snapshot(2_500L), snapshot(3_500L));
        BranchSurvivalHalfLifeReport report = analyzer.analyze(events, rollups);

        assertEquals(1, report.cohortsMeasured());
        assertEquals(0, report.censoredCohorts());
        assertEquals(2.0D, report.branchSurvivalHalfLife(), 1.0E-9D);
        assertEquals(2.0D, report.cohortEstimates().getFirst().halfLifeWindows(), 1.0E-9D);
        assertEquals(List.of(2, 1), report.cohortEstimates().getFirst().activeByWindow());
        assertEquals(List.of(0, 1), report.cohortEstimates().getFirst().inactiveOrDeadByWindow());
        assertEquals("complete", report.estimateStatus());
    }

    @Test
    void marksRightCensoringWhenHalfLifeNotReached() {
        BranchSurvivalHalfLifeAnalyzer analyzer = new BranchSurvivalHalfLifeAnalyzer();
        List<EcosystemTelemetryEvent> events = List.of(
                new EcosystemTelemetryEvent(1_000L, EcosystemTelemetryEventType.BRANCH_FORMATION, 1L, "lin", "SCOUT", Map.of("branch_id", "b1")),
                new EcosystemTelemetryEvent(1_100L, EcosystemTelemetryEventType.BRANCH_FORMATION, 2L, "lin", "SCOUT", Map.of("branch_id", "b2"))
        );

        List<TelemetryRollupSnapshot> rollups = List.of(snapshot(1_500L), snapshot(2_500L), snapshot(3_500L));
        BranchSurvivalHalfLifeReport report = analyzer.analyze(events, rollups);

        assertEquals(1, report.cohortsMeasured());
        assertEquals(1, report.censoredCohorts());
        assertTrue(Double.isNaN(report.branchSurvivalHalfLife()));
        assertTrue(Double.isNaN(report.cohortEstimates().getFirst().halfLifeWindows()));
        assertTrue(report.cohortEstimates().getFirst().censored());
        assertEquals(List.of(2, 2, 2), report.cohortEstimates().getFirst().activeByWindow());
        assertEquals(List.of(0, 0, 0), report.cohortEstimates().getFirst().inactiveOrDeadByWindow());
        assertEquals("censored", report.estimateStatus());
    }

    @Test
    void supportsMultipleCohortsAcrossWindows() {
        BranchSurvivalHalfLifeAnalyzer analyzer = new BranchSurvivalHalfLifeAnalyzer();
        List<EcosystemTelemetryEvent> events = List.of(
                new EcosystemTelemetryEvent(1_000L, EcosystemTelemetryEventType.BRANCH_FORMATION, 1L, "lin-a", "SCOUT", Map.of("branch_id", "a1")),
                new EcosystemTelemetryEvent(1_050L, EcosystemTelemetryEventType.BRANCH_FORMATION, 2L, "lin-a", "SCOUT", Map.of("branch_id", "a2")),
                new EcosystemTelemetryEvent(2_100L, EcosystemTelemetryEventType.LINEAGE_UPDATE, 1L, "lin-a", "SCOUT", Map.of("event", "branch-collapsed", "branch_id", "a1", "lifecycle_state", "COLLAPSING")),
                new EcosystemTelemetryEvent(2_200L, EcosystemTelemetryEventType.BRANCH_FORMATION, 3L, "lin-b", "FORGE", Map.of("branch_id", "b1")),
                new EcosystemTelemetryEvent(3_100L, EcosystemTelemetryEventType.LINEAGE_UPDATE, 2L, "lin-a", "SCOUT", Map.of("event", "branch-lifecycle-transition", "branch_id", "a2", "lifecycle_state", "UNSTABLE"))
        );

        List<TelemetryRollupSnapshot> rollups = List.of(snapshot(1_500L), snapshot(2_500L), snapshot(3_500L));
        BranchSurvivalHalfLifeReport report = analyzer.analyze(events, rollups);

        assertEquals(2, report.cohortsMeasured());
        assertEquals(1, report.censoredCohorts());
        assertEquals("mixed", report.estimateStatus());
        assertEquals(2.0D, report.cohortEstimates().getFirst().halfLifeWindows(), 1.0E-9D);
        assertTrue(report.cohortEstimates().get(1).censored());
    }

    private TelemetryRollupSnapshot snapshot(long ts) {
        NichePopulationRollup niche = new NichePopulationRollup(ts, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        LineagePopulationRollup lineage = new LineagePopulationRollup(ts, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        EcosystemSnapshot ecosystem = new EcosystemSnapshot(ts, Map.of(), niche, lineage, 0L, 0.0D, 0.0D, 0.0D, 0L, 0L, Map.of(), List.of(), 0L, Map.of());
        return new TelemetryRollupSnapshot(TelemetryRollupSnapshot.CURRENT_VERSION, ts, "test", ecosystem);
    }
}
