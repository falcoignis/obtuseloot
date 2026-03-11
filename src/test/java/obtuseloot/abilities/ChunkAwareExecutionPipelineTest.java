package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChunkAwareExecutionPipelineTest {

    @Test
    void chunkAwareDeferredExecutionStillUsesBudgetAndStableIds() {
        ItemAbilityManager manager = new ItemAbilityManager((artifact, rep) -> new AbilityProfile("sense", List.of(
                new AbilityDefinition(
                        "consistency.buried_memory",
                        "Buried Memory",
                        AbilityFamily.CONSISTENCY,
                        AbilityTrigger.ON_STRUCTURE_SENSE,
                        AbilityMechanic.SENSE_PING,
                        "", "", "", "", "", "",
                        List.of(),
                        List.of(),
                        AbilityMetadata.of(
                                java.util.Set.of("structure-awareness"),
                                java.util.Set.of("chunk-entry"),
                                java.util.Set.of("memory"),
                                0.5D, 0.8D, 0.4D, 0.2D, 0.1D, 0.6D,
                                new TriggerBudgetProfile(1.3D, 0.8D, 6.0D, 1.0D, 2, 900L, 35, TriggerBudgetPolicy.PASSIVE_LOW_PRIORITY, false, 200.0D)
                        ),
                        "s1", "s2", "s3", "s4", "s5"
                )
        )));
        manager.setTriggerSubscriptionIndexingEnabled(false);

        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactStorageKey("artifact:chunk:test");

        AbilityEventContext context = new AbilityEventContext(
                AbilityTrigger.ON_STRUCTURE_SENSE,
                artifact,
                new ArtifactReputation(),
                1.0D,
                "structure-sense",
                AbilityRuntimeContext.chunkAware(AbilitySource.STRUCTURE_SENSE, 777L, true)
        );

        AbilityDispatchResult result = manager.resolveDispatch(context);

        assertTrue(result.hasSuccessfulMechanic(AbilityMechanic.SENSE_PING));
        assertTrue(manager.triggerBudgetConsumptionByAbility().containsKey("SENSE_PING"));
        assertEquals(1L, manager.coalescedExecutionByTrigger().getOrDefault(AbilityTrigger.ON_STRUCTURE_SENSE.name(), 0L));
        Map<String, Long> outcomes = manager.outcomeTypeCounts();
        assertTrue(outcomes.containsKey("SENSE_PING@ON_STRUCTURE_SENSE#STRUCTURE_SENSE"));
    }
}
