package obtuseloot.awakening;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AwakeningEngineTest {
    private final AwakeningEngine engine = new AwakeningEngine();

    @Test
    void awakeningReplacesIdentityAndCarriesBoundedContinuity() {
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setArtifactSeed(42L);
        artifact.setArchetypePath("ravager");
        artifact.setLatentLineage("lineage-alpha");
        artifact.setConvergencePath("worldpiercer");
        artifact.setConvergenceVariantId("trident-deadeye-surge");
        artifact.setConvergenceIdentityShape("reaper-deadeye-surge");
        artifact.setConvergenceLineageTrace("deadeye:surge:lineage-alpha");
        artifact.setConvergenceLoreTrace("stormblade:deadeye:precision+boss");
        artifact.setConvergenceContinuityTrace("seed=123|carry=bounded-core");
        artifact.setConvergenceExpressionTrace("trident:deadeye:weapon+spear");
        artifact.setConvergenceMemorySignature("precision+boss");
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
        artifact.addLoreHistory("Won a brutal campaign.");
        artifact.addNotableEvent("boss.cut-down");
        artifact.addNotableEvent("survived.hunt");
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(15);
        rep.setKills(8);
        rep.setBossKills(1);

        ArtifactIdentityTransition transition = engine.evaluate(null, artifact, rep);
        assertNotNull(transition);
        Artifact replacement = transition.replacement();
        assertNotSame(artifact, replacement);
        assertNotEquals(artifact.getArtifactSeed(), replacement.getArtifactSeed());
        assertNotEquals(artifact.getNaming().getNamingSeed(), replacement.getNaming().getNamingSeed());
        assertEquals(artifact.getArtifactStorageKey(), replacement.getArtifactStorageKey());
        assertEquals(artifact.getOwnerId(), replacement.getOwnerId());
        assertEquals(artifact.getItemCategory(), replacement.getItemCategory());
        assertEquals("Executioner's Oath", replacement.getAwakeningPath());
        assertNotEquals("none", replacement.getAwakeningVariantId());
        assertNotEquals("none", replacement.getAwakeningIdentityShape());
        assertEquals("owner-storage|memory-imprint|lineage-thread|bounded-history", replacement.getAwakeningContinuityTrace());
        assertEquals("worldpiercer", replacement.getConvergencePath());
        assertEquals("trident-deadeye-surge", replacement.getConvergenceVariantId());
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
        assertTrue(replacement.getNotableEvents().stream().anyMatch(e -> e.startsWith("awakening.variant.")));
        assertTrue(replacement.getNotableEvents().stream().anyMatch(e -> e.equals("identity.replaced.42")));
        assertTrue(replacement.getLoreHistory().stream().anyMatch(e -> e.contains("Awakening replaced")));
    }

    @Test
    void awakeningRequiresExplicitEligibleIdentityAndDoesNotUseGenericFallback() {
        Artifact unsupported = new Artifact(UUID.randomUUID(), "elytra");
        unsupported.setArtifactSeed(77L);
        unsupported.setArchetypePath("unformed");
        unsupported.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        unsupported.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        unsupported.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(25);
        rep.setBrutality(25);
        rep.setSurvival(25);
        rep.setMobility(25);
        rep.setChaos(25);
        rep.setConsistency(25);
        rep.setKills(20);
        rep.setBossKills(3);
        rep.setRecentKillChain(6);

        assertNull(engine.evaluateSimulation(unsupported, rep));
        assertNull(engine.forceAwakening(null, unsupported, rep));
    }

    @Test
    void awakeningCannotRepeat() {
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setAwakeningPath("Stormblade");

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(20);
        rep.setKills(20);

        assertNull(engine.evaluate(null, artifact, rep));
        assertNull(engine.forceAwakening(null, artifact, rep));
    }
}
