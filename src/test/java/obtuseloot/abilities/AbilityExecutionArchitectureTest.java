package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryEngine;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AbilityExecutionArchitectureTest {

    @Test
    void dispatcherProducesStructuredResultsAndAnalytics() {
        ItemAbilityManager manager = new ItemAbilityManager((artifact, rep) -> profileWith(harvestAbility("Gentle Harvest")));
        manager.setTriggerSubscriptionIndexingEnabled(false);
        Artifact artifact = artifact(51L);

        AbilityDispatchResult dispatchResult = manager.resolveDispatch(new AbilityEventContext(
                AbilityTrigger.ON_BLOCK_HARVEST,
                artifact,
                new ArtifactReputation(),
                1.0D,
                "test-harvest"
        ));

        assertEquals(1, dispatchResult.executions().size());
        AbilityExecutionResult execution = dispatchResult.executions().getFirst();
        assertEquals("survival.gentle_harvest", execution.abilityId());
        assertEquals(AbilityExecutionStatus.SUCCESS, execution.status());
        assertEquals(AbilityOutcomeType.CROP_REPLANT, execution.outcomeType());
        assertTrue(execution.meaningfulOutcome());

        assertEquals(1L, manager.executionStatusCounts().get(AbilityExecutionStatus.TRIGGER_SEEN));
        assertEquals(1L, manager.executionStatusCounts().get(AbilityExecutionStatus.SUCCESS));
        assertEquals(1L, manager.meaningfulOutcomeByMechanicTrigger().get("HARVEST_RELAY@ON_BLOCK_HARVEST"));
    }

    @Test
    void gentleHarvestGatingUsesMechanicIdentityNotDisplayText() {
        ItemAbilityManager manager = new ItemAbilityManager((artifact, rep) -> profileWith(harvestAbility("Localized Crop Rhythm")));
        manager.setTriggerSubscriptionIndexingEnabled(false);
        Artifact artifact = artifact(77L);

        AbilityDispatchResult dispatchResult = manager.resolveDispatch(new AbilityEventContext(
                AbilityTrigger.ON_BLOCK_HARVEST,
                artifact,
                new ArtifactReputation(),
                1.0D,
                "crop-harvest"
        ));

        assertTrue(dispatchResult.hasSuccessfulMechanic(AbilityMechanic.HARVEST_RELAY));
        assertTrue(dispatchResult.presentationEffects().stream().noneMatch(text -> text.contains("Gentle Harvest")));
    }

    @Test
    void memoryEventCanDispatchAndCoalescingThrottlesEmission() {
        Artifact artifact = artifact(99L);
        ArtifactMemoryEngine memoryEngine = new ArtifactMemoryEngine();

        memoryEngine.recordAndProfile(artifact, ArtifactMemoryEvent.FIRST_KILL);
        long now = 10_000L;
        assertTrue(memoryEngine.shouldEmitMemoryTrigger(artifact, ArtifactMemoryEvent.FIRST_KILL, now));
        assertFalse(memoryEngine.shouldEmitMemoryTrigger(artifact, ArtifactMemoryEvent.FIRST_KILL, now + 200L));
        assertTrue(memoryEngine.shouldEmitMemoryTrigger(artifact, ArtifactMemoryEvent.FIRST_BOSS_KILL, now + 2_100L));

        ItemAbilityManager manager = new ItemAbilityManager((a, rep) -> profileWith(memoryAbility()));
        manager.setTriggerSubscriptionIndexingEnabled(false);
        AbilityDispatchResult dispatchResult = manager.resolveDispatch(new AbilityEventContext(
                AbilityTrigger.ON_MEMORY_EVENT,
                artifact,
                new ArtifactReputation(),
                artifact.getMemory().pressure(),
                "memory-event:first_kill"
        ));

        assertEquals(1, dispatchResult.executions().size());
        assertEquals("mobility.compass_stories", dispatchResult.executions().getFirst().abilityId());
        assertEquals(AbilityExecutionStatus.SUCCESS, dispatchResult.executions().getFirst().status());
        assertEquals(1L, manager.executionStatusByMechanicTrigger().get("NAVIGATION_ANCHOR@ON_MEMORY_EVENT#SUCCESS"));
    }

    @Test
    void nonMatchingTriggerDoesNotProduceGhostActivation() {
        ItemAbilityManager manager = new ItemAbilityManager((artifact, rep) -> profileWith(memoryAbility()));
        manager.setTriggerSubscriptionIndexingEnabled(false);

        AbilityDispatchResult dispatchResult = manager.resolveDispatch(new AbilityEventContext(
                AbilityTrigger.ON_BLOCK_INSPECT,
                artifact(10L),
                new ArtifactReputation(),
                1.0D,
                "inspect"
        ));

        assertTrue(dispatchResult.executions().isEmpty());
        Map<AbilityExecutionStatus, Long> counts = manager.executionStatusCounts();
        assertEquals(1L, counts.get(AbilityExecutionStatus.TRIGGER_SEEN));
        assertEquals(0L, counts.get(AbilityExecutionStatus.SUCCESS));
    }


    @Test
    void attemptedSuppressedAndTriggerSeenAreDistinct() {
        ItemAbilityManager manager = new ItemAbilityManager((artifact, rep) -> profileWith(memoryAbility()));
        manager.setTriggerSubscriptionIndexingEnabled(false);

        Artifact artifact = artifact(44L);
        AbilityDispatchResult first = manager.resolveDispatch(new AbilityEventContext(
                AbilityTrigger.ON_MEMORY_EVENT,
                artifact,
                new ArtifactReputation(),
                1.0D,
                "memory-repeat"
        ));
        AbilityDispatchResult second = manager.resolveDispatch(new AbilityEventContext(
                AbilityTrigger.ON_MEMORY_EVENT,
                artifact,
                new ArtifactReputation(),
                1.0D,
                "memory-repeat"
        ));

        assertEquals(1, first.executions().size());
        assertEquals(1, second.executions().size());
        assertEquals(AbilityExecutionStatus.SUCCESS, first.executions().getFirst().status());
        assertEquals(AbilityExecutionStatus.SUPPRESSED, second.executions().getFirst().status());

        Map<AbilityExecutionStatus, Long> counts = manager.executionStatusCounts();
        assertEquals(2L, counts.get(AbilityExecutionStatus.TRIGGER_SEEN));
        assertEquals(2L, counts.get(AbilityExecutionStatus.ATTEMPTED));
        assertEquals(1L, counts.get(AbilityExecutionStatus.SUCCESS));
        assertEquals(1L, counts.get(AbilityExecutionStatus.SUPPRESSED));
    }

    @Test
    void noOpAndMeaningfulOutcomesAreSeparatedAnalytically() {
        ItemAbilityManager manager = new ItemAbilityManager((artifact, rep) -> new AbilityProfile("mix", List.of(harvestAbility("Harvest"), flavorAbility())));
        manager.setTriggerSubscriptionIndexingEnabled(false);

        manager.resolveDispatch(new AbilityEventContext(
                AbilityTrigger.ON_BLOCK_HARVEST,
                artifact(55L),
                new ArtifactReputation(),
                1.0D,
                "harvest"
        ));

        assertEquals(1L, manager.meaningfulOutcomeByMechanicTrigger().get("HARVEST_RELAY@ON_BLOCK_HARVEST"));
        assertEquals(1L, manager.noOpByMechanicTrigger().get("PULSE@ON_BLOCK_HARVEST"));
        assertEquals(1L, manager.executionStatusByMechanicTrigger().get("PULSE@ON_BLOCK_HARVEST#NO_OP"));
        assertNull(manager.meaningfulOutcomeByMechanicTrigger().get("PULSE@ON_BLOCK_HARVEST"));
    }

    private AbilityProfile profileWith(AbilityDefinition definition) {
        return new AbilityProfile("test", List.of(definition));
    }

    private AbilityDefinition harvestAbility(String displayName) {
        return new AbilityDefinition(
                "survival.gentle_harvest",
                displayName,
                AbilityFamily.SURVIVAL,
                AbilityTrigger.ON_BLOCK_HARVEST,
                AbilityMechanic.HARVEST_RELAY,
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(),
                new AbilityMetadata(java.util.Set.of("harvest"), java.util.Set.of("farm"), java.util.Set.of("survival"), 1.0D, 1.0D, 1.0D, 0.4D, 0.0D, 0.0D),
                "s1", "s2", "s3", "s4", "s5"
        );
    }


    private AbilityDefinition flavorAbility() {
        return new AbilityDefinition(
                "support.ward_pulse",
                "Ward Pulse",
                AbilityFamily.SURVIVAL,
                AbilityTrigger.ON_BLOCK_HARVEST,
                AbilityMechanic.PULSE,
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(),
                new AbilityMetadata(java.util.Set.of("flavor"), java.util.Set.of("farm"), java.util.Set.of("support"), 0.1D, 0.1D, 0.1D, 0.1D, 0.1D, 0.1D),
                "f1", "f2", "f3", "f4", "f5"
        );
    }

    private AbilityDefinition memoryAbility() {
        return new AbilityDefinition(
                "mobility.compass_stories",
                "Compass of Stories",
                AbilityFamily.MOBILITY,
                AbilityTrigger.ON_MEMORY_EVENT,
                AbilityMechanic.NAVIGATION_ANCHOR,
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(),
                new AbilityMetadata(java.util.Set.of("memory"), java.util.Set.of("history"), java.util.Set.of("mobility"), 1.0D, 1.0D, 1.0D, 0.5D, 0.0D, 0.0D),
                "m1", "m2", "m3", "m4", "m5"
        );
    }

    private Artifact artifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactStorageKey("artifact:test:" + seed);
        artifact.setArtifactSeed(seed);
        return artifact;
    }
}
