package obtuseloot.telemetry;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.ArtifactUsageTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
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
        TelemetryAggregationService service = telemetryService(buffer, archive, rollups, 4, tempDir.resolve("runtime-schema-snapshot.properties"), 8);
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        ArtifactUsageTracker usageTracker = new ArtifactUsageTracker();
        usageTracker.setTelemetryEmitter(emitter);

        Artifact artifact = new Artifact(UUID.randomUUID(), "wooden_sword");
        artifact.setArtifactSeed(42L);
        artifact.setLatentLineage("lineage-a");

        AbilityDefinition definition = new AbilityDefinition(
                "test.echo",
                "Echo",
                AbilityFamily.PRECISION,
                AbilityTrigger.ON_WORLD_SCAN,
                AbilityMechanic.SENSE_PING,
                "effect", "evo", "drift", "awake", "convergence", "memory", List.of(), List.of(),
                AbilityMetadata.of(java.util.Set.of("info"), java.util.Set.of("scan"), java.util.Set.of("watchful"), 0.8, 0.8, 0.8, 0.2, 0.2, 0.2),
                "s1", "s2", "s3", "s4", "s5"
        );
        AbilityRuntimeContext runtimeContext = AbilityRuntimeContext.chunkAware(AbilitySource.OTHER, 128L, false, "world", "NORMAL");
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
        assertEquals("PHASE_5_7_V1", ability.attributes().get("schema_version"));
        assertEquals("world", ability.attributes().get("world"));
        assertEquals("NORMAL", ability.attributes().get("dimension"));
        assertNotNull(ability.attributes().get("utility_density"));
    }



    @Test
    void copyToSamePathPreservesExistingArchiveContent() {
        Path archivePath = tempDir.resolve("same-path-events.log");
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);
        archive.append(List.of(new EcosystemTelemetryEvent(
                System.currentTimeMillis(),
                EcosystemTelemetryEventType.ABILITY_EXECUTION,
                9L,
                "lin-9",
                "SCOUT",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                        Map.of("niche", "SCOUT", "lineage_id", "lin-9", "ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS")))));

        archive.copyTo(archivePath);

        List<EcosystemTelemetryEvent> events = archive.readAll();
        assertEquals(1, events.size());
        assertEquals(9L, events.getFirst().artifactSeed());
    }

    @Test
    void startupRehydrationRestoresRollupStateFromSnapshotWithoutFullReplay() {
        Path archivePath = tempDir.resolve("rehydrate-events.log");
        Path snapshotPath = tempDir.resolve("rollup-snapshot.properties");
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryRollupSnapshotStore snapshotStore = new TelemetryRollupSnapshotStore(snapshotPath);
        RollupStateHydrator hydrator = new RollupStateHydrator(snapshotStore, archive, 2);
        TelemetryAggregationService service = new TelemetryAggregationService(buffer, archive, rollups, 8, snapshotStore, hydrator);

        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION, 11L, "lin-11", "SCOUT",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION, Map.of("niche", "SCOUT", "lineage_id", "lin-11", "ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"))));
        service.scheduledRollupTick(System.currentTimeMillis() + 2L);

        TelemetryAggregationBuffer restartedBuffer = new TelemetryAggregationBuffer();
        ScheduledEcosystemRollups restartedRollups = new ScheduledEcosystemRollups(restartedBuffer, 1L);
        TelemetryAggregationService restarted = new TelemetryAggregationService(restartedBuffer, archive, restartedRollups, 8,
                snapshotStore, new RollupStateHydrator(snapshotStore, archive, 1));
        restarted.initializeFromHistory();

        assertEquals("rehydrated_snapshot", restarted.initialization().mode());
        assertFalse(restarted.rollups().ecosystemSnapshot().eventCounts().isEmpty());
        assertEquals(1L, restarted.rollups().ecosystemSnapshot().eventCounts().get(EcosystemTelemetryEventType.ABILITY_EXECUTION));
    }

    @Test
    void rollupGeneratedEventIsEmittedOnActualGeneration() {
        Path archivePath = tempDir.resolve("rollup-generated-events.log");
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = telemetryService(buffer, archive, rollups, 4, tempDir.resolve("rollup-generated-snapshot.properties"), 8);

        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION, 22L, "lin-22", "RITUAL",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION, Map.of("niche", "RITUAL", "lineage_id", "lin-22", "ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"))));
        service.scheduledRollupTick(System.currentTimeMillis() + 5L);

        List<EcosystemTelemetryEvent> all = archive.readAll();
        EcosystemTelemetryEvent rollupEvent = all.stream().filter(e -> e.type() == EcosystemTelemetryEventType.ROLLUP_GENERATED).findFirst().orElseThrow();
        assertEquals("scheduled-rollup", rollupEvent.attributes().get("trigger"));
        assertNotEquals("na", rollupEvent.attributes().get("rollup_record_count"));
    }

    @Test
    void schemaContractsFailFastWhenRequiredFieldsMissing() {
        assertThrows(IllegalArgumentException.class, () -> TelemetryFieldContract.normalize(
                EcosystemTelemetryEventType.ABILITY_EXECUTION,
                Map.of("niche", "SCOUT")));
    }


    @Test
    void schemaRequiresBranchDivergenceAndSpecializationTrajectoryOnRelevantEvents() {
        assertThrows(IllegalArgumentException.class, () -> TelemetryFieldContract.normalize(
                EcosystemTelemetryEventType.BRANCH_FORMATION,
                Map.of("lineage_id", "lin-a", "branch_id", "b-1")));

        assertThrows(IllegalArgumentException.class, () -> TelemetryFieldContract.normalize(
                EcosystemTelemetryEventType.NICHE_CLASSIFICATION_CHANGE,
                Map.of("niche", "SCOUT", "specialization_pressure", "0.6")));

        Map<String, String> normalized = TelemetryFieldContract.normalize(
                EcosystemTelemetryEventType.COMPETITION_ALLOCATION,
                Map.of("niche", "SCOUT", "reinforcement_multiplier", "1.1", "ecology_pressure", "0.4", "specialization_trajectory", "-0.12"));
        assertEquals("-0.12", normalized.get("specialization_trajectory"));
    }

    @Test
    void periodicFlushRunsDuringRuntimeAndPersistsBeforeShutdown() {
        Path archivePath = tempDir.resolve("periodic-events.log");
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = telemetryService(buffer, archive, rollups, 1000, tempDir.resolve("periodic-snapshot.properties"), 8);
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        emitter.emit(EcosystemTelemetryEventType.LINEAGE_UPDATE, 7L, "lin-a", "SCOUT", Map.of("event", "ancestor-added", "branch_divergence", "0.0", "specialization_trajectory", "0.0"));
        assertEquals(1, buffer.pendingCount());

        new TelemetryFlushScheduler(emitter).run();

        assertEquals(0, buffer.pendingCount());
        assertFalse(archive.readAll().isEmpty());
    }

    @Test
    void rollupsRemainTelemetryBackedAndExposeExpandedMetrics() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(tempDir.resolve("rollup.log"));
        TelemetryAggregationService service = telemetryService(serviceBuffer(buffer), archive, rollups, 32, tempDir.resolve("rollup-metrics-snapshot.properties"), 8);

        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION, 1L, "lin-1", "SCOUT",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION, Map.of("niche", "SCOUT", "lineage_id", "lin-1", "utility_density", "0.75", "meaningful", "true", "ecology_pressure", "0.4", "ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"))));
        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.BRANCH_FORMATION, 1L, "lin-1", "SCOUT",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.BRANCH_FORMATION, Map.of("niche", "SCOUT", "lineage_id", "lin-1", "branch_id", "b-1", "branch_divergence", "0.2"))));
        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.COMPETITION_ALLOCATION, 2L, "lin-2", "RITUAL",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.COMPETITION_ALLOCATION, Map.of("niche", "RITUAL", "lineage_id", "lin-2", "lineage_momentum", "1.1", "opportunity_share", "0.22", "reinforcement_multiplier", "1.1", "ecology_pressure", "0.2", "specialization_trajectory", "0.08"))));

        service.scheduledRollupTick(System.currentTimeMillis() + 5L);
        EcosystemSnapshot snapshot = rollups.ecosystemSnapshot();

        assertEquals(1L, snapshot.nichePopulationRollup().populationByNiche().get("SCOUT"));
        assertEquals(1L, snapshot.nichePopulationRollup().meaningfulOutcomesByNiche().get("SCOUT"));
        assertEquals(1L, snapshot.lineagePopulationRollup().branchCountByLineage().get("lin-1"));
        assertTrue(snapshot.lineagePopulationRollup().momentumByLineage().get("lin-2") > 1.0D);
        assertEquals(snapshot.eventCounts().get(EcosystemTelemetryEventType.BRANCH_FORMATION), snapshot.branchBirthCount());
    }


    @Test
    void branchAndMeaningfulAttributionUseNicheContextAcrossEvents() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(tempDir.resolve("niche-attribution.log"));
        TelemetryAggregationService service = telemetryService(serviceBuffer(buffer), archive, rollups, 32, tempDir.resolve("niche-attribution-snapshot.properties"), 8);

        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION, 3L, "lin-ctx", "SCOUT",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                        Map.of("niche", "SCOUT", "lineage_id", "lin-ctx", "outcome_classification", "MEANINGFUL", "ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"))));
        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.BRANCH_FORMATION, 3L, "lin-ctx", TelemetryFieldContract.NOT_APPLICABLE,
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.BRANCH_FORMATION,
                        Map.of("lineage_id", "lin-ctx", "branch_id", "b-ctx", "branch_divergence", "0.3"))));

        service.scheduledRollupTick(System.currentTimeMillis() + 5L);
        EcosystemSnapshot snapshot = rollups.ecosystemSnapshot();

        assertEquals(1L, snapshot.nichePopulationRollup().meaningfulOutcomesByNiche().get("SCOUT"));
        assertEquals(1L, snapshot.nichePopulationRollup().branchContributionByNiche().get("SCOUT"));
    }

    @Test
    void populationRollupTracksLatestArtifactNicheInsteadOfEventVolume() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(tempDir.resolve("population-latest-niche.log"));
        TelemetryAggregationService service = telemetryService(serviceBuffer(buffer), archive, rollups, 32, tempDir.resolve("population-latest-snapshot.properties"), 8);

        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION, 42L, "lin-move", "NAVIGATION",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                        Map.of("niche", "NAVIGATION", "lineage_id", "lin-move", "ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"))));
        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION, 42L, "lin-move", "NAVIGATION_A1",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                        Map.of("niche", "NAVIGATION", "lineage_id", "lin-move", "ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"))));

        service.scheduledRollupTick(System.currentTimeMillis() + 5L);
        EcosystemSnapshot snapshot = rollups.ecosystemSnapshot();

        assertEquals(1L, snapshot.nichePopulationRollup().populationByNiche().get("NAVIGATION_A1"));
        assertEquals(1L, snapshot.activeArtifactCount());
        assertEquals(1L, snapshot.lineagePopulationRollup().populationByLineage().get("lin-move"));
        assertFalse(snapshot.nichePopulationRollup().populationByNiche().containsKey("NAVIGATION")
                        && snapshot.nichePopulationRollup().populationByNiche().get("NAVIGATION") > 0L,
                "artifact should be counted only in its latest effective niche");
    }

    @Test
    void aggregationPrefersEventNicheOverLegacyAttributeNicheForDynamicChildren() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(tempDir.resolve("dynamic-child-niche.log"));
        TelemetryAggregationService service = telemetryService(serviceBuffer(buffer), archive, rollups, 32, tempDir.resolve("dynamic-child-snapshot.properties"), 8);

        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.NICHE_BIFURCATION, 0L, "", "RITUAL_STRANGE_UTILITY",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.NICHE_BIFURCATION,
                        Map.of("event_type", "niche_bifurcation", "parent_niche", "RITUAL_STRANGE_UTILITY", "child_niche_a", "RITUAL_A1", "child_niche_b", "RITUAL_B1", "context_tags", "niche-bifurcation lifecycle"))));

        service.record(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION, 99L, "lin-child", "RITUAL_A1",
                TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                        Map.of("niche", "RITUAL", "lineage_id", "lin-child", "meaningful", "true", "ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"))));

        service.scheduledRollupTick(System.currentTimeMillis() + 5L);
        EcosystemSnapshot snapshot = rollups.ecosystemSnapshot();

        assertEquals(1L, snapshot.nichePopulationRollup().meaningfulOutcomesByNiche().get("RITUAL_A1"));
        assertEquals(1L, snapshot.dynamicNichePopulation().get("RITUAL_A1"));
    }

    @Test
    void aggregationBufferOperationsRemainBoundedWithActiveArtifacts() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        int events = 2500;
        for (int i = 0; i < events; i++) {
            buffer.enqueue(new EcosystemTelemetryEvent(System.currentTimeMillis(), EcosystemTelemetryEventType.ABILITY_EXECUTION,
                    i, "lin-" + (i % 11), "NICHE-" + (i % 7), TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION, Map.of("niche", "NICHE-" + (i % 7), "lineage_id", "lin-" + (i % 11), "ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"))));
        }
        long before = System.nanoTime();
        List<EcosystemTelemetryEvent> drained = buffer.drain(events);
        long elapsedMicros = (System.nanoTime() - before) / 1000L;
        assertEquals(events, drained.size());
        assertTrue(elapsedMicros < 200_000L, "drain should remain bounded and fast for active artifact counts");
    }

    @Test
    void recordRejectsMalformedEventInputEarly() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        TelemetryAggregationService service = telemetryService(
                buffer,
                new EcosystemHistoryArchive(tempDir.resolve("invalid-input.log")),
                new ScheduledEcosystemRollups(buffer, 1L),
                8,
                tempDir.resolve("invalid-input-snapshot.properties"),
                8);
        EcosystemTelemetryEvent invalid = new EcosystemTelemetryEvent(
                System.currentTimeMillis(),
                EcosystemTelemetryEventType.ABILITY_EXECUTION,
                1L,
                "lin-a",
                "SCOUT",
                Map.of("niche", "SCOUT"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.record(invalid));
        assertTrue(ex.getMessage().contains("required"));
    }

    @Test
    void archiveReadReportsPersistedLineContextForMalformedEvents() throws Exception {
        Path archivePath = tempDir.resolve("malformed-archive.log");
        Files.writeString(archivePath, "broken-line\n");
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, archive::readAll);
        assertTrue(ex.getMessage().contains("line 1"));
    }

    @Test
    void snapshotReadReportsInvalidPersistedRollupState() throws Exception {
        Path snapshotPath = tempDir.resolve("bad-rollup.properties");
        Files.writeString(snapshotPath, """
                version=1
                createdAtMs=1
                initializationMode=rehydrated_snapshot
                snapshot.generatedAtMs=7
                eventCounts.not-a-real-event=3
                """);
        TelemetryRollupSnapshotStore store = new TelemetryRollupSnapshotStore(snapshotPath);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, store::readLatest);
        assertTrue(ex.getMessage().contains("Bad persisted rollup state"));
    }

    private TelemetryAggregationBuffer serviceBuffer(TelemetryAggregationBuffer buffer) {
        return buffer;
    }

    private TelemetryAggregationService telemetryService(TelemetryAggregationBuffer buffer,
                                                         EcosystemHistoryArchive archive,
                                                         ScheduledEcosystemRollups rollups,
                                                         int archiveBatchSize,
                                                         Path snapshotPath,
                                                         int replayWindowEvents) {
        TelemetryRollupSnapshotStore snapshotStore = new TelemetryRollupSnapshotStore(snapshotPath);
        RollupStateHydrator hydrator = new RollupStateHydrator(snapshotStore, archive, replayWindowEvents);
        return new TelemetryAggregationService(buffer, archive, rollups, archiveBatchSize, snapshotStore, hydrator);
    }
}
