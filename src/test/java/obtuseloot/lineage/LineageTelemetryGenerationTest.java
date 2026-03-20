package obtuseloot.lineage;

import obtuseloot.artifacts.Artifact;
import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        Artifact artifact = new Artifact(UUID.randomUUID(), "wooden_sword");
        artifact.setArtifactSeed(91L);
        artifact.setLatentLineage("lin-g");

        registry.assignLineage(artifact);
        emitter.flushAll();
        List<EcosystemTelemetryEvent> events = archive.readAll();
        EcosystemTelemetryEvent lineage = events.stream().filter(e -> e.type() == EcosystemTelemetryEventType.LINEAGE_UPDATE).findFirst().orElseThrow();
        assertNotEquals(TelemetryFieldContract.NOT_APPLICABLE, lineage.attributes().get("generation"));
        assertEquals("1", lineage.attributes().get("generation"));
        assertEquals("0.0", lineage.attributes().get("branch_divergence"));
        assertEquals("0.0", lineage.attributes().get("specialization_trajectory"));
    }
    @Test
    void endToEndDivergenceAndSpecializationTrajectoryFlowThroughArchiveAndSnapshot() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(tempDir.resolve("phase57-e2e-events.log"));
        TelemetryRollupSnapshotStore snapshotStore = new TelemetryRollupSnapshotStore(tempDir.resolve("phase57-e2e-snapshot.properties"));
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = new TelemetryAggregationService(buffer, archive, rollups, 32,
                snapshotStore,
                new RollupStateHydrator(snapshotStore, archive, 16));
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        LineageRegistry registry = new LineageRegistry();
        registry.setTelemetryEmitter(emitter);

        Artifact artifact = new Artifact(UUID.randomUUID(), "wooden_sword");
        artifact.setArtifactSeed(9001L);
        artifact.setLatentLineage("lin-phase57");
        registry.assignLineage(artifact);

        EvolutionaryBiasGenome divergent = new EvolutionaryBiasGenome();
        divergent.add(LineageBiasDimension.EXPLORATION_PREFERENCE, 0.33D);
        divergent.add(LineageBiasDimension.SPECIALIZATION, 0.30D);

        for (int i = 0; i < 20; i++) {
            Artifact descendant = new Artifact(UUID.randomUUID(), "wooden_sword");
            descendant.setArtifactSeed(9002L + i);
            descendant.setLatentLineage("lin-phase57");
            registry.recordDescendantBias(descendant, divergent, 1.30D, 1.15D);
        }


        ArtifactLineage trackedLineage = registry.lineageFor("lin-phase57");
        emitter.emit(EcosystemTelemetryEventType.BRANCH_FORMATION,
                9001L,
                "lin-phase57",
                "SCOUT",
                Map.of("lineage_id", "lin-phase57",
                        "branch_id", "manual-phase57-branch",
                        "branch_divergence", String.valueOf(trackedLineage.currentBranchDivergence()),
                        "context_tags", "phase57-e2e"));

        emitter.emit(EcosystemTelemetryEventType.NICHE_CLASSIFICATION_CHANGE,
                9001L,
                "lin-phase57",
                "SCOUT",
                Map.of("niche", "SCOUT", "specialization_pressure", "0.7", "specialization_trajectory", "0.15", "context_tags", "phase57-e2e"));

        emitter.flushAll();
        service.scheduledRollupTick(System.currentTimeMillis() + 10L);

        List<EcosystemTelemetryEvent> events = archive.readAll();
        EcosystemTelemetryEvent branchEvent = events.stream().filter(e -> e.type() == EcosystemTelemetryEventType.BRANCH_FORMATION).findFirst().orElseThrow();
        EcosystemTelemetryEvent nicheEvent = events.stream().filter(e -> e.type() == EcosystemTelemetryEventType.NICHE_CLASSIFICATION_CHANGE).findFirst().orElseThrow();

        assertNotEquals(TelemetryFieldContract.NOT_APPLICABLE, branchEvent.attributes().get("branch_divergence"));
        assertNotEquals(TelemetryFieldContract.NOT_APPLICABLE, nicheEvent.attributes().get("specialization_trajectory"));

        EcosystemSnapshot snapshot = rollups.ecosystemSnapshot();
        assertTrue(snapshot.lineagePopulationRollup().branchDivergenceByLineage().containsKey("lin-phase57"));
        assertTrue(snapshot.lineagePopulationRollup().specializationTrajectoryByLineage().containsKey("lin-phase57"));

        TelemetryAggregationBuffer restartedBuffer = new TelemetryAggregationBuffer();
        ScheduledEcosystemRollups restartedRollups = new ScheduledEcosystemRollups(restartedBuffer, 1L);
        TelemetryAggregationService restarted = new TelemetryAggregationService(restartedBuffer, archive, restartedRollups, 32,
                snapshotStore, new RollupStateHydrator(snapshotStore, archive, 8));
        restarted.initializeFromHistory();

        assertTrue(restarted.rollups().lineagePopulationRollup().branchDivergenceByLineage().containsKey("lin-phase57"));
        assertTrue(restarted.rollups().lineagePopulationRollup().specializationTrajectoryByLineage().containsKey("lin-phase57"));
    }

}
