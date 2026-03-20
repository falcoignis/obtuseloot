package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerSubscriptionIndexIdentityRefreshTest {

    @Test
    void getOrRebuildRefreshesWhenArtifactIdentityChanges() {
        TriggerSubscriptionIndex index = new TriggerSubscriptionIndex();
        ItemAbilityManager manager = new ItemAbilityManager((artifact, rep) ->
                new AbilityProfile("test", List.of(ability("seed-" + artifact.getArtifactSeed()))));
        ArtifactReputation reputation = new ArtifactReputation();
        UUID playerId = UUID.randomUUID();

        Artifact original = artifact(playerId, 11L, "player:" + playerId);
        PlayerArtifactTriggerMap first = index.getOrRebuild(playerId, original, reputation, manager, "initial");

        Artifact replacement = artifact(playerId, 22L, "player:" + playerId);
        PlayerArtifactTriggerMap second = index.getOrRebuild(playerId, replacement, reputation, manager, "replacement");

        assertNotSame(first, second);
        assertEquals(11L, first.artifactSeed());
        assertEquals(22L, second.artifactSeed());
        assertEquals("replacement", second.lastRebuildReason());
        assertTrue(second.matchesArtifactIdentity(22L, "player:" + playerId));
        assertEquals("seed-22", second.bindingsFor(AbilityTrigger.ON_HIT).getFirst().abilityId());
    }

    private Artifact artifact(UUID ownerId, long seed, String storageKey) {
        Artifact artifact = new Artifact(ownerId, "wooden_sword");
        artifact.setArtifactSeed(seed);
        artifact.setArtifactStorageKey(storageKey);
        artifact.setDisplayName("Artifact-" + seed);
        return artifact;
    }

    private AbilityDefinition ability(String id) {
        return new AbilityDefinition(
                id,
                id,
                AbilityFamily.BRUTALITY,
                AbilityTrigger.ON_HIT,
                AbilityMechanic.PULSE,
                "effect",
                "evo",
                "drift",
                "awake",
                "conv",
                "memory",
                List.of(),
                List.of(),
                AbilityMetadata.of(Set.of("combat"), Set.of("attack"), Set.of("offense"), 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D),
                "stage1",
                "stage2",
                "stage3",
                "stage4",
                "stage5"
        );
    }
}
