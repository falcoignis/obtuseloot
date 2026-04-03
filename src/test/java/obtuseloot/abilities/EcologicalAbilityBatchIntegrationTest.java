package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.ArtifactUsageTracker;
import obtuseloot.evolution.NichePopulationTracker;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EcologicalAbilityBatchIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void newBatchTriggersDispatchAndOutcomeClassification() {
        AbilityDefinition def = definition("exploration.trail_sense", AbilityTrigger.ON_CHUNK_ENTER, AbilityMechanic.TRAIL_SENSE,
                Set.of("exploration"), Set.of("exploration"));
        ItemAbilityManager manager = new ItemAbilityManager((artifact, rep) -> new AbilityProfile("eco", List.of(def)));
        manager.setTriggerSubscriptionIndexingEnabled(false);

        AbilityDispatchResult first = manager.resolveDispatch(new AbilityEventContext(
                AbilityTrigger.ON_CHUNK_ENTER,
                artifact(101L),
                new ArtifactReputation(),
                1.0D,
                "chunk-enter",
                AbilityRuntimeContext.chunkAware(AbilitySource.OTHER, 101L, true)
        ));

        assertEquals(1, first.executions().size());
        assertEquals(AbilityExecutionStatus.SUCCESS, first.executions().getFirst().status());
        assertEquals(AbilityOutcomeType.NAVIGATION_HINT, first.executions().getFirst().outcomeType());
        assertTrue(first.executions().getFirst().meaningfulOutcome());
    }

    @Test
    void triggerBudgetLimitsAreAppliedForNewTriggers() {
        AbilityDefinition def = definition("social.collective_insight", AbilityTrigger.ON_PLAYER_GROUP_ACTION, AbilityMechanic.COLLECTIVE_RELAY,
                Set.of("social"), Set.of("social"));
        ItemAbilityManager manager = new ItemAbilityManager((artifact, rep) -> new AbilityProfile("eco", List.of(def)));
        manager.setTriggerSubscriptionIndexingEnabled(false);
        Artifact artifact = artifact(202L);

        AbilityDispatchResult first = manager.resolveDispatch(new AbilityEventContext(AbilityTrigger.ON_PLAYER_GROUP_ACTION, artifact, new ArtifactReputation(), 1.0D, "group"));
        AbilityDispatchResult second = manager.resolveDispatch(new AbilityEventContext(AbilityTrigger.ON_PLAYER_GROUP_ACTION, artifact, new ArtifactReputation(), 1.0D, "group"));

        assertEquals(AbilityExecutionStatus.SUCCESS, first.executions().getFirst().status());
        assertEquals(AbilityExecutionStatus.SUPPRESSED, second.executions().getFirst().status());
        assertTrue(second.executions().getFirst().suppressionReason().startsWith("trigger-budget"));
    }

    @Test
    void telemetryContainsOutcomeClassificationAndNicheTags() {
        Path archivePath = tempDir.resolve("eco-events.log");
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = telemetryService(buffer, archive, rollups, 4, tempDir.resolve("eco-snapshot.properties"));
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        tracker.setTelemetryEmitter(emitter);

        Artifact artifact = artifact(303L);
        artifact.setLatentLineage("lin-eco");

        AbilityDefinition def = definition("environment.structure_attunement", AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityMechanic.STRUCTURE_ATTUNEMENT,
                Set.of("environmental", "structure-awareness"), Set.of("environmental", "exploration"));
        AbilityEventContext context = new AbilityEventContext(AbilityTrigger.ON_STRUCTURE_PROXIMITY, artifact, new ArtifactReputation(), 1.0D, "structure-prox");
        AbilityExecutionResult result = new AbilityExecutionResult(def.id(), def.mechanic(), def.trigger(), "k", artifact.getOwnerId(),
                AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.STRUCTURE_SENSE, true, null, "ok");

        tracker.trackAbilityExecution(artifact, context, result, def);
        emitter.flushAll();
        service.scheduledRollupTick(System.currentTimeMillis() + 2L);

        EcosystemTelemetryEvent abilityEvent = archive.readAll().stream()
                .filter(event -> event.type() == EcosystemTelemetryEventType.ABILITY_EXECUTION)
                .findFirst()
                .orElseThrow();

        assertEquals("MEANINGFUL", abilityEvent.attributes().get("outcome_classification"));
        assertTrue(abilityEvent.attributes().get("niche_tags").contains("environmental"));
        assertEquals("ON_STRUCTURE_PROXIMITY", abilityEvent.attributes().get("trigger"));
        assertFalse(rollups.ecosystemSnapshot().nichePopulationRollup().populationByNiche().isEmpty());

        AbilityDefinition trail = definition("exploration.trail_sense", AbilityTrigger.ON_CHUNK_ENTER, AbilityMechanic.TRAIL_SENSE,
                Set.of("exploration", "pathfinding"), Set.of("exploration"));
        AbilityExecutionResult trailResult = new AbilityExecutionResult(trail.id(), trail.mechanic(), trail.trigger(), "k", artifact.getOwnerId(),
                AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.NAVIGATION_HINT, true, null, "ok");
        tracker.trackAbilityExecution(artifact, new AbilityEventContext(AbilityTrigger.ON_CHUNK_ENTER, artifact, new ArtifactReputation(), 0.9D, "chunk"), trailResult, trail);
        emitter.flushAll();

        EcosystemTelemetryEvent trailEvent = archive.readAll().stream()
                .filter(event -> "exploration.trail_sense".equals(event.attributes().get("ability_id")))
                .findFirst()
                .orElseThrow();
        assertTrue(trailEvent.attributes().get("niche_tags").contains("exploration"));
        assertNotEquals("na", trailEvent.attributes().get("exploration_chain_length"));
    }

    @Test
    void harnessScenarioRecordsActivationTelemetryAndNichePresence() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        NichePopulationTracker niches = tracker.nichePopulationTracker();

        Artifact artifact = artifact(404L);
        artifact.setLatentLineage("lin-harness");
        tracker.trackCreated(artifact);

        AbilityDefinition explore = definition("exploration.cartographers_echo", AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityMechanic.CARTOGRAPHERS_ECHO,
                Set.of("exploration", "map-intelligence"), Set.of("exploration"));
        AbilityDefinition gather = definition("gathering.forager_memory", AbilityTrigger.ON_RESOURCE_HARVEST_STREAK, AbilityMechanic.FORAGER_MEMORY,
                Set.of("gathering", "resource-detection"), Set.of("gathering"));
        AbilityDefinition ritual = definition("ritual.pattern_resonance", AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, AbilityMechanic.PATTERN_RESONANCE,
                Set.of("ritual", "pattern-recognition"), Set.of("ritual"));
        AbilityDefinition social = definition("social.witness_imprint", AbilityTrigger.ON_PLAYER_WITNESS, AbilityMechanic.WITNESS_IMPRINT,
                Set.of("social", "lineage-narrative"), Set.of("social"));

        tracker.trackAbilityExecution(artifact,
                new AbilityEventContext(AbilityTrigger.ON_STRUCTURE_DISCOVERY, artifact, new ArtifactReputation(), 1.0D, "harness-structure"),
                new AbilityExecutionResult(explore.id(), explore.mechanic(), explore.trigger(), "k", artifact.getOwnerId(),
                        AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.NAVIGATION_HINT, true, null, "ok"),
                explore);

        tracker.trackAbilityExecution(artifact,
                new AbilityEventContext(AbilityTrigger.ON_RESOURCE_HARVEST_STREAK, artifact, new ArtifactReputation(), 1.0D, "harness-harvest"),
                new AbilityExecutionResult(gather.id(), gather.mechanic(), gather.trigger(), "k", artifact.getOwnerId(),
                        AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.CROP_REPLANT, true, null, "ok"),
                gather);

        tracker.trackAbilityExecution(artifact,
                new AbilityEventContext(AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, artifact, new ArtifactReputation(), 1.0D, "harness-pattern"),
                new AbilityExecutionResult(ritual.id(), ritual.mechanic(), ritual.trigger(), "k", artifact.getOwnerId(),
                        AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.MEMORY_MARK, true, null, "ok"),
                ritual);

        tracker.trackAbilityExecution(artifact,
                new AbilityEventContext(AbilityTrigger.ON_PLAYER_WITNESS, artifact, new ArtifactReputation(), 1.0D, "harness-witness"),
                new AbilityExecutionResult(social.id(), social.mechanic(), social.trigger(), "k", artifact.getOwnerId(),
                        AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.MEMORY_MARK, true, null, "ok"),
                social);

        Map<String, Object> snapshot = niches.analyticsSnapshot();
        assertFalse(((Map<?, ?>) snapshot.get("nichePopulation")).isEmpty());
        assertFalse(((Map<?, ?>) snapshot.get("nicheUtilityDensity")).isEmpty());
        assertFalse(tracker.profileFor(artifact).utilitySignalsByMechanic().isEmpty());
    }

    private AbilityDefinition definition(String id,
                                         AbilityTrigger trigger,
                                         AbilityMechanic mechanic,
                                         Set<String> domains,
                                         Set<String> affinities) {
        return new AbilityDefinition(
                id,
                id,
                AbilityFamily.CONSISTENCY,
                trigger,
                mechanic,
                "effect",
                "evo",
                "drift",
                "awakening",
                "convergence",
                "memory",
                List.of(),
                List.of(),
                AbilityMetadata.of(domains, Set.of(trigger.name().toLowerCase()), affinities, 0.8D, 0.8D, 0.8D, 0.6D, 0.6D, 0.8D),
                "s1", "s2", "s3", "s4", "s5"
        );
    }

    private Artifact artifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "wooden_sword");
        artifact.setArtifactSeed(seed);
        artifact.setArtifactStorageKey("artifact:" + seed);
        return artifact;
    }

    private TelemetryAggregationService telemetryService(TelemetryAggregationBuffer buffer,
                                                         EcosystemHistoryArchive archive,
                                                         ScheduledEcosystemRollups rollups,
                                                         int archiveBatchSize,
                                                         Path snapshotPath) {
        TelemetryRollupSnapshotStore snapshotStore = new TelemetryRollupSnapshotStore(snapshotPath);
        RollupStateHydrator hydrator = new RollupStateHydrator(snapshotStore, archive, 8);
        return new TelemetryAggregationService(buffer, archive, rollups, archiveBatchSize, snapshotStore, hydrator);
    }
}
