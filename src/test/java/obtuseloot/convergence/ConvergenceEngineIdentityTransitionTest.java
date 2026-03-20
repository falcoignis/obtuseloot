package obtuseloot.convergence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConvergenceEngineIdentityTransitionTest {

    @Test
    void convergenceProducesReplacementIdentityWithNewArchetypeSeedAndLineageContinuity() {
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = seeded(41L, EquipmentArchetype.BOW);
        artifact.setArchetypePath("deadeye");
        artifact.setEvolutionPath("advanced-deadeye");
        artifact.setAwakeningPath("Stormblade");
        artifact.setLatentLineage("lineage-omega");
        artifact.addLoreHistory("earned through battle");
        artifact.addNotableEvent("awakening.stormblade");
        artifact.getMemory().record(ArtifactMemoryEvent.AWAKENING);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(28);
        rep.setMobility(18);
        rep.setBrutality(12);
        rep.setKills(14);
        rep.setBossKills(2);

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);

        assertNotNull(transition);
        Artifact replacement = transition.replacement();
        assertNotSame(artifact, replacement);
        assertTrue(transition.reason().startsWith("artifact-convergence:horizon-syndicate:"));
        assertEquals(artifact.getArtifactStorageKey(), replacement.getArtifactStorageKey());
        assertEquals(artifact.getOwnerId(), replacement.getOwnerId());
        assertEquals("lineage-omega", replacement.getLatentLineage());
        assertEquals("horizon-syndicate", replacement.getConvergencePath());
        assertTrue(replacement.getArchetypePath().startsWith("horizon-"));
        assertNotEquals(artifact.getArtifactSeed(), replacement.getArtifactSeed());
        assertNotEquals(artifact.getNaming().getNamingSeed(), replacement.getNaming().getNamingSeed());
        assertEquals(EquipmentArchetype.TRIDENT.id(), replacement.getItemCategory());
        assertTrue(replacement.getNotableEvents().stream().anyMatch(event -> event.startsWith("identity.replaced.")));
        assertEquals(artifact.getMemory().pressure(), replacement.getMemory().pressure());
        assertNotEquals("none", replacement.getConvergenceVariantId());
        assertNotEquals("none", replacement.getConvergenceIdentityShape());
        assertNotEquals("none", replacement.getConvergenceLineageTrace());
        assertNotEquals("none", replacement.getConvergenceLoreTrace());
        assertNotEquals("none", replacement.getConvergenceExpressionTrace());
    }

    @Test
    void convergenceDoesNotLeakEphemeralRuntimeSnapshotsIntoReplacement() {
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = seeded(88L, EquipmentArchetype.DIAMOND_SWORD);
        artifact.setAwakeningPath("Ember Wake");
        artifact.setLastAbilityBranchPath("[slash, finisher]");
        artifact.setLastMutationHistory("[mutation-a]");
        artifact.setLastMemoryInfluence("rage-echo");
        artifact.setLastRegulatoryProfile("[boss_gate]");
        artifact.setLastOpenRegulatoryGates("alpha,beta");
        artifact.setLastGateCandidatePool("8->2");
        artifact.setLastTriggerProfile("combat-heavy");
        artifact.setLastMechanicProfile("burst-window");
        artifact.setLastInterferenceEffects("staggered");
        artifact.setLastLatentActivationRate(0.73D);
        artifact.setLastActivatedLatentTraits("[echo]");
        artifact.setLastUtilityHistory("utility-trace");
        artifact.getMemory().record(ArtifactMemoryEvent.AWAKENING);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        artifact.getMemory().record(ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(32);
        rep.setMobility(14);
        rep.setChaos(8);
        rep.setKills(22);
        rep.setBossKills(2);

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);

        assertNotNull(transition);
        Artifact replacement = transition.replacement();
        assertEquals("[]", replacement.getLastAbilityBranchPath());
        assertEquals("[]", replacement.getLastMutationHistory());
        assertEquals("none", replacement.getLastMemoryInfluence());
        assertEquals("[]", replacement.getLastRegulatoryProfile());
        assertEquals("", replacement.getLastOpenRegulatoryGates());
        assertEquals("0->0", replacement.getLastGateCandidatePool());
        assertEquals("", replacement.getLastTriggerProfile());
        assertEquals("", replacement.getLastMechanicProfile());
        assertEquals("none", replacement.getLastInterferenceEffects());
        assertEquals(0.0D, replacement.getLastLatentActivationRate());
        assertEquals("[]", replacement.getLastActivatedLatentTraits());
        assertEquals("", replacement.getLastUtilityHistory());
    }

    @Test
    void convergenceRecipesAlwaysProduceDistinctIdentityTargetsForEligibleArtifacts() {
        ConvergenceEngine engine = new ConvergenceEngine();

        assertConvergesToDistinctTarget(engine, preparedArtifact(11L, EquipmentArchetype.BOW), rangedRep());
        assertConvergesToDistinctTarget(engine, preparedArtifact(12L, EquipmentArchetype.CROSSBOW), rangedRep());
        assertConvergesToDistinctTarget(engine, preparedArtifact(13L, EquipmentArchetype.DIAMOND_SWORD), meleeRep());
        assertConvergesToDistinctTarget(engine, preparedArtifact(14L, EquipmentArchetype.DIAMOND_AXE), meleeRep());
        assertConvergesToDistinctTarget(engine, preparedArtifact(15L, EquipmentArchetype.DIAMOND_CHESTPLATE), armoredRep());
        assertConvergesToDistinctTarget(engine, preparedArtifact(16L, EquipmentArchetype.ELYTRA), mobilityRep());
        assertConvergesToDistinctTarget(engine, preparedArtifact(17L, EquipmentArchetype.TRIDENT), worldpiercerRep());
    }

    private void assertConvergesToDistinctTarget(ConvergenceEngine engine, Artifact artifact, ArtifactReputation rep) {
        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, () -> "Expected convergence for " + artifact.getItemCategory());
        assertNotEquals(artifact.getItemCategory(), transition.replacement().getItemCategory(),
                () -> "Expected distinct identity target for " + artifact.getItemCategory());
    }

    private Artifact preparedArtifact(long seed, EquipmentArchetype archetype) {
        Artifact artifact = seeded(seed, archetype);
        artifact.setAwakeningPath("Convergence Ready");
        artifact.addLoreHistory("battle-hardened");
        artifact.addNotableEvent("milestone.ready");
        artifact.getMemory().record(ArtifactMemoryEvent.AWAKENING);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        artifact.getMemory().record(ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL);
        artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        return artifact;
    }

    private ArtifactReputation rangedRep() {
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(30);
        rep.setMobility(16);
        rep.setKills(20);
        rep.setBossKills(2);
        return rep;
    }

    private ArtifactReputation meleeRep() {
        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(30);
        rep.setMobility(15);
        rep.setKills(21);
        rep.setBossKills(2);
        return rep;
    }

    private ArtifactReputation armoredRep() {
        ArtifactReputation rep = new ArtifactReputation();
        rep.setSurvival(28);
        rep.setConsistency(18);
        rep.setKills(24);
        rep.setBossKills(3);
        return rep;
    }

    private ArtifactReputation mobilityRep() {
        ArtifactReputation rep = new ArtifactReputation();
        rep.setSurvival(18);
        rep.setMobility(28);
        rep.setKills(20);
        rep.setBossKills(2);
        return rep;
    }

    private ArtifactReputation worldpiercerRep() {
        ArtifactReputation rep = new ArtifactReputation();
        rep.setMobility(26);
        rep.setSurvival(18);
        rep.setKills(22);
        rep.setBossKills(3);
        return rep;
    }

    private Artifact seeded(long seed, EquipmentArchetype archetype) {
        Artifact artifact = new Artifact(UUID.randomUUID(), archetype);
        artifact.setArtifactSeed(seed);
        artifact.addLoreHistory("seeded-history");
        artifact.addNotableEvent("seeded-event");
        return artifact;
    }
}
