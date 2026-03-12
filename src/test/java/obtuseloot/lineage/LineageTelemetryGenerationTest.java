package obtuseloot.lineage;

import obtuseloot.artifacts.Artifact;
import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LineageTelemetryGenerationTest {

    @TempDir
    Path tempDir;

    @Test
    void lineageEventsPopulateGenerationWhenKnown() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(tempDir.resolve("lineage-events.log"));
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = new TelemetryAggregationService(buffer, archive, rollups, 8,
                new TelemetryRollupSnapshotStore(tempDir.resolve("lineage-snapshot.properties")),
                new RollupStateHydrator(new TelemetryRollupSnapshotStore(tempDir.resolve("lineage-snapshot.properties")), archive, 16));
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        LineageRegistry registry = new LineageRegistry();
        registry.setTelemetryEmitter(emitter);

        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactSeed(91L);
        artifact.setLatentLineage("lin-g");

        registry.assignLineage(artifact);
        emitter.flushAll();
        List<EcosystemTelemetryEvent> events = archive.readAll();
        EcosystemTelemetryEvent lineage = events.stream().filter(e -> e.type() == EcosystemTelemetryEventType.LINEAGE_UPDATE).findFirst().orElseThrow();
        assertNotEquals(TelemetryFieldContract.NOT_APPLICABLE, lineage.attributes().get("generation"));
        assertEquals("1", lineage.attributes().get("generation"));
    }
}
