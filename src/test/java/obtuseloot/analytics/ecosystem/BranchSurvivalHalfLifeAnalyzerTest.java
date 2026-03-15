package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BranchSurvivalHalfLifeAnalyzerTest {

    @Test
    void computesHalfLifeFromBranchLifecycleEvents() {
        BranchSurvivalHalfLifeAnalyzer analyzer = new BranchSurvivalHalfLifeAnalyzer();
        List<EcosystemTelemetryEvent> events = List.of(
                new EcosystemTelemetryEvent(1_000L, EcosystemTelemetryEventType.BRANCH_FORMATION, 1L, "lin", "SCOUT", Map.of("branch_id", "b1")),
                new EcosystemTelemetryEvent(1_100L, EcosystemTelemetryEventType.BRANCH_FORMATION, 2L, "lin", "SCOUT", Map.of("branch_id", "b2")),
                new EcosystemTelemetryEvent(2_100L, EcosystemTelemetryEventType.LINEAGE_UPDATE, 1L, "lin", "SCOUT", Map.of("event", "branch-collapsed", "branch_id", "b1")),
                new EcosystemTelemetryEvent(3_100L, EcosystemTelemetryEventType.LINEAGE_UPDATE, 2L, "lin", "SCOUT", Map.of("event", "branch-collapsed", "branch_id", "b2"))
        );

        List<TelemetryRollupSnapshot> rollups = List.of(snapshot(1_500L), snapshot(2_500L), snapshot(3_500L));
        BranchSurvivalHalfLifeReport report = analyzer.analyze(events, rollups);

        assertEquals(1, report.cohortsMeasured());
        assertEquals(0, report.censoredCohorts());
        assertEquals(2.0D, report.branchSurvivalHalfLife(), 1.0E-9D);
        assertEquals(2.0D, report.cohortEstimates().getFirst().halfLifeWindows(), 1.0E-9D);
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
        assertEquals(3.0D, report.branchSurvivalHalfLife(), 1.0E-9D);
        assertTrue(report.cohortEstimates().getFirst().censored());
    }

    private TelemetryRollupSnapshot snapshot(long ts) {
        NichePopulationRollup niche = new NichePopulationRollup(ts, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        LineagePopulationRollup lineage = new LineagePopulationRollup(ts, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        EcosystemSnapshot ecosystem = new EcosystemSnapshot(ts, Map.of(), niche, lineage, 0L, 0.0D, 0.0D, 0.0D, 0L, 0L, Map.of());
        return new TelemetryRollupSnapshot(TelemetryRollupSnapshot.CURRENT_VERSION, ts, "test", ecosystem);
    }
}

