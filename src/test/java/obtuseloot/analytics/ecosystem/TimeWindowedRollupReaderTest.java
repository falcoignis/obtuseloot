package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TimeWindowedRollupReaderTest {

    @Test
    void selectsRollingAndCalendarWindows() {
        List<TelemetryRollupSnapshot> history = List.of(
                snapshot(1_000L),
                snapshot(2_000L),
                snapshot(3_000L),
                snapshot(4_000L)
        );

        AnalysisWindowSelector selector = new AnalysisWindowSelector();
        List<TelemetryRollupSnapshot> rolling = selector.select(history, HistoricalBucketPolicy.rollingSnapshots(2));
        assertEquals(2, rolling.size());
        assertEquals(3_000L, rolling.getFirst().createdAtMs());

        List<TelemetryRollupSnapshot> hourly = selector.select(history,
                new HistoricalBucketPolicy(HistoricalBucketPolicy.BucketType.HOURLY, 1, 1, 0));
        assertEquals(4, hourly.size());
    }

    private TelemetryRollupSnapshot snapshot(long ts) {
        NichePopulationRollup niche = new NichePopulationRollup(ts, Map.of("SCOUT", 1L), Map.of("SCOUT", 1L), Map.of("SCOUT", 1.0D), Map.of(), Map.of(), Map.of(), Map.of());
        LineagePopulationRollup lineage = new LineagePopulationRollup(ts, Map.of("lin", 1L), Map.of("lin", 1L), Map.of(), Map.of(), Map.of("lin", 0.01D), Map.of(), Map.of(), Map.of());
        EcosystemSnapshot ecosystem = new EcosystemSnapshot(ts, Map.of(EcosystemTelemetryEventType.ABILITY_EXECUTION, 1L), niche, lineage, 1L, 0.5D, 0.5D, 0.1D, 1L, 0L, Map.of(), List.of(), 0L, Map.of());
        return new TelemetryRollupSnapshot(TelemetryRollupSnapshot.CURRENT_VERSION, ts, "test", ecosystem);
    }
}
