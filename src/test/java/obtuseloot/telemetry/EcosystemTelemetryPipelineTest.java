package obtuseloot.telemetry;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.ArtifactUsageTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EcosystemTelemetryPipelineTest {

    @TempDir
    Path tempDir;

    @Test
    void emitsAbilityTelemetryAndPersistsAcrossRestart() {
        Path archivePath = tempDir.resolve("ecosystem-events.log");
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = new TelemetryAggregationService(buffer, archive, rollups, 4);
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        ArtifactUsageTracker usageTracker = new ArtifactUsageTracker();
        usageTracker.setTelemetryEmitter(emitter);

        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactSeed(42L);
        artifact.setLatentLineage("lineage-a");

        AbilityDefinition definition = new AbilityDefinition(
                "test.echo",
                "Echo",
                AbilityFamily.PRECISION,
                AbilityTrigger.ON_WORLD_SCAN,
                AbilityMechanic.SENSE_PING,
                "effect", "evo", "drift", "awake", "fusion", "memory", List.of(), List.of(),
                AbilityMetadata.of(java.util.Set.of("info"), java.util.Set.of("scan"), java.util.Set.of("watchful"), 0.8, 0.8, 0.8, 0.2, 0.2, 0.2),
                "s1", "s2", "s3", "s4", "s5"
        );
        AbilityEventContext context = new AbilityEventContext(AbilityTrigger.ON_WORLD_SCAN, artifact, null, 1.0D, "test");
        AbilityExecutionResult result = new AbilityExecutionResult("test.echo", AbilityMechanic.SENSE_PING, AbilityTrigger.ON_WORLD_SCAN,
                "k", artifact.getOwnerId(), AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.INFORMATION, true, null, "ok");

        usageTracker.trackAbilityExecution(artifact, context, result, definition);
        emitter.flush();

        List<EcosystemTelemetryEvent> events = archive.readAll();
        assertFalse(events.isEmpty());
        assertTrue(events.stream().anyMatch(e -> e.type() == EcosystemTelemetryEventType.ABILITY_EXECUTION));

        EcosystemHistoryArchive reloaded = new EcosystemHistoryArchive(archivePath);
        assertEquals(events.size(), reloaded.readAll().size());
    }

    @Test
    void rollupsDeriveFromBufferSnapshotsNotRawHistory() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = new TelemetryAggregationService(buffer,
                new EcosystemHistoryArchive(tempDir.resolve("rollup.log")), rollups, 32);

        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION, 1L, "lin-1", "SCOUT", Map.of()));
        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.BRANCH_FORMATION, 2L, "lin-1", "SCOUT", Map.of()));
        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.COMPETITION_ALLOCATION, 3L, "lin-2", "RITUAL", Map.of()));

        service.scheduledRollupTick(System.currentTimeMillis() + 5L);

        EcosystemSnapshot snapshot = rollups.ecosystemSnapshot();
        assertEquals(2L, snapshot.nichePopulationRollup().populationByNiche().get("SCOUT"));
        assertEquals(1L, snapshot.lineagePopulationRollup().populationByLineage().get("lin-2"));
        assertTrue(snapshot.eventCounts().get(EcosystemTelemetryEventType.COMPETITION_ALLOCATION) >= 1L);
    }

    @Test
    void aggregationBufferOperationsRemainBoundedWithActiveArtifacts() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        int events = 2500;
        for (int i = 0; i < events; i++) {
            buffer.enqueue(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION,
                    i, "lin-" + (i % 11), "NICHE-" + (i % 7), Map.of()));
        }
        long before = System.nanoTime();
        List<EcosystemTelemetryEvent> drained = buffer.drain(events);
        long elapsedMicros = (System.nanoTime() - before) / 1000L;
        assertEquals(events, drained.size());
        assertTrue(elapsedMicros < 200_000L, "drain should remain bounded and fast for active artifact counts");
    }
}
