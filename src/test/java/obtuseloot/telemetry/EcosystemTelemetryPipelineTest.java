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
    void emitsNormalizedTelemetrySchemaForRuntimeAbilityExecution() {
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
        AbilityRuntimeContext runtimeContext = AbilityRuntimeContext.chunkAware(AbilitySource.OTHER, 128L, false);
        AbilityEventContext context = new AbilityEventContext(AbilityTrigger.ON_WORLD_SCAN, artifact, null, 1.0D, "test", runtimeContext);
        AbilityExecutionResult result = new AbilityExecutionResult("test.echo", AbilityMechanic.SENSE_PING, AbilityTrigger.ON_WORLD_SCAN,
                "k", artifact.getOwnerId(), AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.INFORMATION, true, null, "ok");

        usageTracker.trackAbilityExecution(artifact, context, result, definition);
        emitter.flushAll();

        List<EcosystemTelemetryEvent> events = archive.readAll();
        EcosystemTelemetryEvent ability = events.stream().filter(e -> e.type() == EcosystemTelemetryEventType.ABILITY_EXECUTION).findFirst().orElseThrow();
        assertEquals("test.echo", ability.attributes().get("ability_id"));
        assertEquals("SUCCESS", ability.attributes().get("execution_status"));
        assertEquals("42", ability.attributes().get("artifact_seed"));
        assertEquals("lineage-a", ability.attributes().get("lineage_id"));
        assertEquals("128", ability.attributes().get("chunk"));
        assertEquals("PHASE_5_5_V1", ability.attributes().get("schema_version"));
        assertEquals("na", ability.attributes().get("world"));
        assertNotNull(ability.attributes().get("utility_density"));
    }

    @Test
    void periodicFlushRunsDuringRuntimeAndPersistsBeforeShutdown() {
        Path archivePath = tempDir.resolve("periodic-events.log");
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = new TelemetryAggregationService(buffer, archive, rollups, 1000);
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        emitter.emit(EcosystemTelemetryEventType.LINEAGE_UPDATE, 7L, "lin-a", "SCOUT", Map.of("event", "ancestor-added"));
        assertEquals(1, buffer.pendingCount());

        new TelemetryFlushScheduler(emitter).run();

        assertEquals(0, buffer.pendingCount());
        assertFalse(archive.readAll().isEmpty());
    }

    @Test
    void rollupsRemainTelemetryBackedAndExposeExpandedMetrics() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = new TelemetryAggregationService(serviceBuffer(buffer),
                new EcosystemHistoryArchive(tempDir.resolve("rollup.log")), rollups, 32);

        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION, 1L, "lin-1", "SCOUT",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION, Map.of("niche", "SCOUT", "lineage_id", "lin-1", "utility_density", "0.75", "meaningful", "true", "ecology_pressure", "0.4"))));
        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.BRANCH_FORMATION, 1L, "lin-1", "SCOUT",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.BRANCH_FORMATION, Map.of("niche", "SCOUT", "lineage_id", "lin-1", "branch_id", "b-1", "branch_divergence", "0.2"))));
        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.COMPETITION_ALLOCATION, 2L, "lin-2", "RITUAL",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.COMPETITION_ALLOCATION, Map.of("niche", "RITUAL", "lineage_id", "lin-2", "lineage_momentum", "1.1", "opportunity_share", "0.22"))));

        service.scheduledRollupTick(System.currentTimeMillis() + 5L);
        EcosystemSnapshot snapshot = rollups.ecosystemSnapshot();

        assertEquals(1L, snapshot.nichePopulationRollup().populationByNiche().get("SCOUT"));
        assertEquals(1L, snapshot.nichePopulationRollup().meaningfulOutcomesByNiche().get("SCOUT"));
        assertEquals(1L, snapshot.lineagePopulationRollup().branchCountByLineage().get("lin-1"));
        assertTrue(snapshot.lineagePopulationRollup().momentumByLineage().get("lin-2") > 1.0D);
        assertEquals(snapshot.eventCounts().get(EcosystemTelemetryEventType.BRANCH_FORMATION), snapshot.branchBirthCount());
    }

    @Test
    void aggregationBufferOperationsRemainBoundedWithActiveArtifacts() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        int events = 2500;
        for (int i = 0; i < events; i++) {
            buffer.enqueue(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION,
                    i, "lin-" + (i % 11), "NICHE-" + (i % 7), TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION, Map.of("niche", "NICHE-" + (i % 7), "lineage_id", "lin-" + (i % 11)))));
        }
        long before = System.nanoTime();
        List<EcosystemTelemetryEvent> drained = buffer.drain(events);
        long elapsedMicros = (System.nanoTime() - before) / 1000L;
        assertEquals(events, drained.size());
        assertTrue(elapsedMicros < 200_000L, "drain should remain bounded and fast for active artifact counts");
    }

    private TelemetryAggregationBuffer serviceBuffer(TelemetryAggregationBuffer buffer) {
        return buffer;
    }
}
