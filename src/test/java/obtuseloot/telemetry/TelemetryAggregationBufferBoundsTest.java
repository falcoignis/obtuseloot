package obtuseloot.telemetry;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryAggregationBufferBoundsTest {

    @Test
    void boundsDimensionCardinalityForLineageAndNicheMaps() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer(512, 64);
        for (int i = 0; i < 500; i++) {
            buffer.enqueue(new EcosystemTelemetryEvent(
                    System.currentTimeMillis(),
                    EcosystemTelemetryEventType.BRANCH_FORMATION,
                    i,
                    "lineage-" + i,
                    "niche-" + i,
                    TelemetryFieldContract.normalize(EcosystemTelemetryEventType.BRANCH_FORMATION,
                            Map.of("lineage_id", "lineage-" + i,
                                    "niche", "niche-" + i,
                                    "branch_id", "branch-" + i,
                                    "branch_divergence", "0.2"))));
        }

        assertTrue(buffer.lineagePopulationSnapshot().size() <= 64);
        assertTrue(buffer.nichePopulationSnapshot().size() <= 64);
        assertTrue(buffer.branchCountByLineageSnapshot().size() <= 64);
        assertTrue(buffer.nicheDistributionByLineageSnapshot().size() <= 64);
    }
}
