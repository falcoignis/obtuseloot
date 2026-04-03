package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.ArtifactUsageTracker;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FocusedNonCombatAbilityExpansionTest {

    @TempDir
    Path tempDir;

    @Test
    void focusedAbilitiesAreRegisteredWithExpectedTriggersAndMechanics() {
        AbilityRegistry registry = new AbilityRegistry();
        Map<String, AbilityTemplate> byId = registry.templates().stream().collect(Collectors.toMap(AbilityTemplate::id, Function.identity()));

        assertBinding(byId, "exploration.trail_sense", AbilityTrigger.ON_CHUNK_ENTER, AbilityMechanic.TRAIL_SENSE);
        assertBinding(byId, "gathering.forager_memory", AbilityTrigger.ON_RESOURCE_HARVEST_STREAK, AbilityMechanic.FORAGER_MEMORY);
        assertBinding(byId, "ritual.pattern_resonance", AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, AbilityMechanic.PATTERN_RESONANCE);
        assertBinding(byId, "social.witness_imprint", AbilityTrigger.ON_PLAYER_WITNESS, AbilityMechanic.WITNESS_IMPRINT);
        assertBinding(byId, "exploration.cartographers_echo", AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityMechanic.CARTOGRAPHERS_ECHO);
    }


    @Test
    void focusedAbilitiesProduceMeaningfulAndNoOpOutcomesBasedOnEventSignal() {
        AbilityExecutor executor = new AbilityExecutor();
        Artifact artifact = artifact(510L);

        assertOutcome(executor, definition("exploration.trail_sense", AbilityTrigger.ON_CHUNK_ENTER, AbilityMechanic.TRAIL_SENSE, Set.of("exploration"), Set.of("exploration")),
                new AbilityEventContext(AbilityTrigger.ON_CHUNK_ENTER, artifact, new ArtifactReputation(), 0.9D, "chunk", AbilityRuntimeContext.chunkAware(AbilitySource.OTHER, 99L, true)),
                AbilityExecutionStatus.SUCCESS);
        assertOutcome(executor, definition("exploration.trail_sense", AbilityTrigger.ON_CHUNK_ENTER, AbilityMechanic.TRAIL_SENSE, Set.of("exploration"), Set.of("exploration")),
                new AbilityEventContext(AbilityTrigger.ON_CHUNK_ENTER, artifact, new ArtifactReputation(), 0.2D, "chunk", AbilityRuntimeContext.passive(AbilitySource.OTHER)),
                AbilityExecutionStatus.NO_OP);

        assertOutcome(executor, definition("gathering.forager_memory", AbilityTrigger.ON_RESOURCE_HARVEST_STREAK, AbilityMechanic.FORAGER_MEMORY, Set.of("gathering"), Set.of("gathering")),
                new AbilityEventContext(AbilityTrigger.ON_RESOURCE_HARVEST_STREAK, artifact, new ArtifactReputation(), 0.8D, "harvest"), AbilityExecutionStatus.SUCCESS);
        assertOutcome(executor, definition("gathering.forager_memory", AbilityTrigger.ON_RESOURCE_HARVEST_STREAK, AbilityMechanic.FORAGER_MEMORY, Set.of("gathering"), Set.of("gathering")),
                new AbilityEventContext(AbilityTrigger.ON_RESOURCE_HARVEST_STREAK, artifact, new ArtifactReputation(), 0.1D, "harvest"), AbilityExecutionStatus.NO_OP);

        assertOutcome(executor, definition("ritual.pattern_resonance", AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, AbilityMechanic.PATTERN_RESONANCE, Set.of("ritual"), Set.of("ritual")),
                new AbilityEventContext(AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, artifact, new ArtifactReputation(), 0.9D, "pattern"), AbilityExecutionStatus.SUCCESS);
        assertOutcome(executor, definition("ritual.pattern_resonance", AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, AbilityMechanic.PATTERN_RESONANCE, Set.of("ritual"), Set.of("ritual")),
                new AbilityEventContext(AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, artifact, new ArtifactReputation(), 0.2D, "pattern"), AbilityExecutionStatus.NO_OP);

        assertOutcome(executor, definition("social.witness_imprint", AbilityTrigger.ON_PLAYER_WITNESS, AbilityMechanic.WITNESS_IMPRINT, Set.of("social"), Set.of("social")),
                new AbilityEventContext(AbilityTrigger.ON_PLAYER_WITNESS, artifact, new ArtifactReputation(), 0.8D, "witness"), AbilityExecutionStatus.SUCCESS);
        assertOutcome(executor, definition("social.witness_imprint", AbilityTrigger.ON_PLAYER_WITNESS, AbilityMechanic.WITNESS_IMPRINT, Set.of("social"), Set.of("social")),
                new AbilityEventContext(AbilityTrigger.ON_PLAYER_WITNESS, artifact, new ArtifactReputation(), 0.1D, "witness"), AbilityExecutionStatus.NO_OP);

        assertOutcome(executor, definition("exploration.cartographers_echo", AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityMechanic.CARTOGRAPHERS_ECHO, Set.of("exploration"), Set.of("exploration")),
                new AbilityEventContext(AbilityTrigger.ON_STRUCTURE_DISCOVERY, artifact, new ArtifactReputation(), 0.8D, "structure"), AbilityExecutionStatus.SUCCESS);
        assertOutcome(executor, definition("exploration.cartographers_echo", AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityMechanic.CARTOGRAPHERS_ECHO, Set.of("exploration"), Set.of("exploration")),
                new AbilityEventContext(AbilityTrigger.ON_STRUCTURE_DISCOVERY, artifact, new ArtifactReputation(), 0.1D, "structure"), AbilityExecutionStatus.NO_OP);
    }

    @Test
    void focusedAbilityTelemetrySignalsFlowIntoStructuredAbilityEvents() {
        Path archivePath = tempDir.resolve("focused-events.log");
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = telemetryService(buffer, archive, rollups, 4, tempDir.resolve("focused-snapshot.properties"));
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        tracker.setTelemetryEmitter(emitter);
        Artifact artifact = artifact(611L);

        var defs = Map.of(
                "trail", definition("exploration.trail_sense", AbilityTrigger.ON_CHUNK_ENTER, AbilityMechanic.TRAIL_SENSE, Set.of("exploration"), Set.of("exploration")),
                "forager", definition("gathering.forager_memory", AbilityTrigger.ON_RESOURCE_HARVEST_STREAK, AbilityMechanic.FORAGER_MEMORY, Set.of("gathering"), Set.of("gathering")),
                "pattern", definition("ritual.pattern_resonance", AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, AbilityMechanic.PATTERN_RESONANCE, Set.of("ritual"), Set.of("ritual")),
                "witness", definition("social.witness_imprint", AbilityTrigger.ON_PLAYER_WITNESS, AbilityMechanic.WITNESS_IMPRINT, Set.of("social"), Set.of("social")),
                "carto", definition("exploration.cartographers_echo", AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityMechanic.CARTOGRAPHERS_ECHO, Set.of("exploration"), Set.of("exploration"))
        );

        track(tracker, artifact, defs.get("trail"), 0.9D, AbilityOutcomeType.NAVIGATION_HINT);
        track(tracker, artifact, defs.get("forager"), 0.9D, AbilityOutcomeType.CROP_REPLANT);
        track(tracker, artifact, defs.get("pattern"), 0.9D, AbilityOutcomeType.MEMORY_MARK);
        track(tracker, artifact, defs.get("witness"), 0.9D, AbilityOutcomeType.MEMORY_MARK);
        track(tracker, artifact, defs.get("carto"), 0.9D, AbilityOutcomeType.NAVIGATION_HINT);

        emitter.flushAll();
        service.scheduledRollupTick(System.currentTimeMillis() + 2L);

        Map<String, EcosystemTelemetryEvent> byAbility = archive.readAll().stream()
                .filter(e -> e.type() == EcosystemTelemetryEventType.ABILITY_EXECUTION)
                .collect(Collectors.toMap(e -> e.attributes().get("ability_id"), Function.identity(), (a, b) -> b));

        assertNotEquals("na", byAbility.get("exploration.trail_sense").attributes().get("exploration_chain_length"));
        assertTrue(byAbility.get("exploration.trail_sense").attributes().get("niche_tags").contains("exploration"));
        assertNotEquals("na", byAbility.get("gathering.forager_memory").attributes().get("harvest_chain_length"));
        assertTrue(byAbility.get("gathering.forager_memory").attributes().get("niche_tags").contains("gathering"));
        assertNotEquals("na", byAbility.get("ritual.pattern_resonance").attributes().get("ritual_activation_count"));
        assertTrue(byAbility.get("ritual.pattern_resonance").attributes().get("niche_tags").contains("ritual"));
        assertNotEquals("na", byAbility.get("social.witness_imprint").attributes().get("witness_interactions"));
        assertTrue(byAbility.get("social.witness_imprint").attributes().get("niche_tags").contains("social"));
        assertNotEquals("na", byAbility.get("exploration.cartographers_echo").attributes().get("structure_chain_discovery"));
    }

    private void assertBinding(Map<String, AbilityTemplate> byId, String id, AbilityTrigger trigger, AbilityMechanic mechanic) {
        AbilityTemplate template = byId.get(id);
        assertNotNull(template, id + " missing");
        assertEquals(trigger, template.trigger());
        assertEquals(mechanic, template.mechanic());
    }

    private void assertOutcome(AbilityExecutor executor, AbilityDefinition definition, AbilityEventContext context, AbilityExecutionStatus expected) {
        AbilityExecutionResult result = executor.execute(definition, context, 3);
        assertEquals(expected, result.status());
    }

    private void track(ArtifactUsageTracker tracker, Artifact artifact, AbilityDefinition definition, double value, AbilityOutcomeType type) {
        tracker.trackAbilityExecution(artifact,
                new AbilityEventContext(definition.trigger(), artifact, new ArtifactReputation(), value, "focused", AbilityRuntimeContext.chunkAware(AbilitySource.OTHER, 42L, true)),
                new AbilityExecutionResult(definition.id(), definition.mechanic(), definition.trigger(), "k", artifact.getOwnerId(), AbilityExecutionStatus.SUCCESS, type, true, null, "ok"),
                definition);
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
                java.util.List.of(),
                java.util.List.of(),
                AbilityMetadata.of(domains, Set.of(trigger.name().toLowerCase()), affinities, 0.8D, 0.8D, 0.8D, 0.6D, 0.6D, 0.8D),
                "s1", "s2", "s3", "s4", "s5"
        );
    }

    private Artifact artifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "elytra");
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
