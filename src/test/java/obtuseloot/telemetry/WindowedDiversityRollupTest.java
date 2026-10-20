package obtuseloot.telemetry;

import obtuseloot.analytics.NicheDiversityMetrics;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WindowedDiversityRollupTest {
    @Test
    void reportsAdjacentOccupancyChangesAndNotCumulativeBranchEvents() {
        var buffer = new TelemetryAggregationBuffer();
        var rollups = new ScheduledEcosystemRollups(buffer, 1L);
        buffer.enqueue(event(1, "a"));
        buffer.enqueue(event(2, "b"));
        rollups.run(1);
        assertEquals(0.5D, rollups.ecosystemSnapshot().diversityIndex());
        assertEquals(0, rollups.ecosystemSnapshot().turnoverRate());
        for (int i = 0; i < 100; i++) {
            buffer.enqueue(new EcosystemTelemetryEvent(1L, EcosystemTelemetryEventType.BRANCH_FORMATION, 1, "lineage", "a", Map.of()));
        }
        rollups.run(2);
        assertEquals(100, rollups.ecosystemSnapshot().branchBirthCount());
        assertEquals(0, rollups.ecosystemSnapshot().turnoverRate());
        buffer.enqueue(event(1, "c"));
        rollups.run(3);
        assertEquals(0.5D, rollups.ecosystemSnapshot().diversityIndex());
        assertEquals(2.0D / 3.0D, rollups.ecosystemSnapshot().turnoverRate(), 1.0E-12D);
        var restored = new ScheduledEcosystemRollups(buffer, 1);
        restored.restore(rollups.ecosystemSnapshot(), 3);
        restored.run(4);
        assertEquals(0, restored.ecosystemSnapshot().turnoverRate());
        buffer.enqueue(event(2, "c"));
        restored.run(5);
        assertEquals(0, restored.ecosystemSnapshot().diversityIndex());
        assertEquals(0.5D, restored.ecosystemSnapshot().turnoverRate());
        assertEquals(1, NicheDiversityMetrics.diversity(buffer.nichePopulationSnapshot()).liveNiches());
    }

    private EcosystemTelemetryEvent event(long seed, String niche) {
        return new TelemetryEventFactory().create(EcosystemTelemetryEventType.ABILITY_EXECUTION, seed, "lineage", niche,
                Map.of("ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"));
    }
}
