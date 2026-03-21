package obtuseloot.convergence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
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
        assertTrue(Set.of(EquipmentArchetype.ELYTRA.id(), EquipmentArchetype.TRIDENT.id()).contains(replacement.getItemCategory()));
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
    void convergenceRecipeDefinitionsStayWithinDeclaredSemanticRoleBounds() {
        assertDoesNotThrow(ConvergenceEngine::recipeIntegrityDiagnostics);
        assertEquals(5, ConvergenceEngine.recipeIntegrityDiagnostics().size());
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

    @Test
    void seedIntegrityDifferentArtifactHistoriesProduceDifferentSeeds() {
        ConvergenceEngine engine = new ConvergenceEngine();

        Artifact artifactA = preparedArtifact(100L, EquipmentArchetype.BOW);
        artifactA.addLoreHistory("battle-alpha");
        artifactA.addNotableEvent("event.alpha");

        Artifact artifactB = preparedArtifact(100L, EquipmentArchetype.BOW);
        artifactB.addLoreHistory("battle-beta");
        artifactB.addNotableEvent("event.beta");
        artifactB.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);

        ArtifactReputation rep = rangedRep();
        ArtifactIdentityTransition transitionA = engine.evaluateSimulation(artifactA, rep);
        ArtifactIdentityTransition transitionB = engine.evaluateSimulation(artifactB, rep);

        assertNotNull(transitionA);
        assertNotNull(transitionB);
        assertNotEquals(transitionA.replacement().getArtifactSeed(), transitionB.replacement().getArtifactSeed(),
                "Different artifact histories must produce different convergence seeds");
    }

    @Test
    void seedIntegritySameArtifactStateProducesIdenticalSeed() {
        ConvergenceEngine engine = new ConvergenceEngine();

        Artifact artifact = preparedArtifact(77L, EquipmentArchetype.BOW);
        ArtifactReputation rep = rangedRep();

        ArtifactIdentityTransition first = engine.evaluateSimulation(artifact, rep);
        ArtifactIdentityTransition second = engine.evaluateSimulation(artifact, rep);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.replacement().getArtifactSeed(), second.replacement().getArtifactSeed(),
                "Same artifact state must produce identical convergence seed");
        assertEquals(first.replacement().getConvergenceVariantId(), second.replacement().getConvergenceVariantId());
    }

    @Test
    void worldpiercerIsReachableForElytra() {
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = preparedArtifact(55L, EquipmentArchetype.ELYTRA);
        ArtifactReputation rep = worldpiercerElytraRep();

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);

        assertNotNull(transition, "ELYTRA must be able to reach a convergence recipe");
        assertEquals("worldpiercer", transition.replacement().getConvergencePath(),
                "ELYTRA must resolve to worldpiercer after sky-bastion no longer accepts it");
        assertTrue(Set.of(EquipmentArchetype.TRIDENT.id(), EquipmentArchetype.ELYTRA.id())
                        .contains(transition.replacement().getItemCategory()),
                "worldpiercer targets must be TRIDENT or ELYTRA");
        assertNotEquals(EquipmentArchetype.ELYTRA.id(), transition.replacement().getItemCategory(),
                "Convergence must produce a distinct identity target");
    }

    @Test
    void engineUnificationDiagnosticCountersAccumulateAcrossAllCalls() {
        ConvergenceEngine engine = new ConvergenceEngine();

        Artifact a1 = preparedArtifact(10L, EquipmentArchetype.BOW);
        Artifact a2 = preparedArtifact(20L, EquipmentArchetype.DIAMOND_SWORD);
        ArtifactReputation rep1 = rangedRep();
        ArtifactReputation rep2 = meleeRep();

        engine.evaluateSimulation(a1, rep1);
        engine.evaluateSimulation(a2, rep2);

        Map<String, Integer> counters = engine.diagnosticCounters();
        assertTrue(counters.get("convergence_attempted") >= 2,
                "Shared engine must accumulate attempted count across all callers");
        assertTrue(counters.get("convergence_applied") >= 1,
                "Shared engine must reflect all applied convergences");
    }

    private ArtifactReputation worldpiercerElytraRep() {
        ArtifactReputation rep = new ArtifactReputation();
        rep.setMobility(28);
        rep.setSurvival(18);
        rep.setKills(24);
        rep.setBossKills(2);
        return rep;
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
        rep.setKills(24);
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

    // ---- Phase 6.25: finalist window, lineage, and cadence refinement tests ----

    @Test
    void finalistWindowNarrowSpreadPermitsCompetitivePool() {
        // When ELYTRA and TRIDENT score identically (tie), both are within the dynamic window.
        // Different artifact seeds should select different targets through historySalt variation.
        // Scores derived analytically: precision=16, bossKills=1, PRECISION_STREAK×1 → TRIDENT=23;
        //   mobility=20, killChain=1, mobilityWeight=0.45 → ELYTRA=20+1+round(2.25)=23.
        ConvergenceEngine engine = new ConvergenceEngine();
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(16);
        rep.setMobility(20);
        rep.setBossKills(1);
        rep.setRecentKillChain(1);
        rep.setKills(26); // totalScore = 16+20+52+5 = 93 ≥ 92

        Set<String> targetsSeen = new java.util.HashSet<>();
        for (long seed = 200L; seed < 220L; seed++) {
            Artifact artifact = preparedArtifact(seed, EquipmentArchetype.BOW);
            ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
            assertNotNull(transition, "Tied-score artifact must converge");
            targetsSeen.add(transition.replacement().getItemCategory());
        }
        assertTrue(targetsSeen.size() >= 2,
                "Near-tie score spread must allow multiple targets into finalist pool; only saw: " + targetsSeen);
    }

    @Test
    void finalistWindowWideSpreadEnforcesSingleWinner() {
        // When TRIDENT dominates by >10 points over ELYTRA, only TRIDENT enters the finalist pool.
        // precision=45, bossKills=3, PRECISION_STREAK×1 → TRIDENT=45+9+4=58;
        //   mobility=5, killChain=0, mobilityWeight=0.45 → ELYTRA=5+0+2=7; gap=51, window=10.
        ConvergenceEngine engine = new ConvergenceEngine();
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(45);
        rep.setMobility(5);
        rep.setBossKills(3);
        rep.setRecentKillChain(0);
        rep.setKills(14); // totalScore = 45+5+28+15 = 93 ≥ 92

        Set<String> targetsSeen = new java.util.HashSet<>();
        for (long seed = 300L; seed < 315L; seed++) {
            Artifact artifact = preparedArtifact(seed, EquipmentArchetype.BOW);
            ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
            assertNotNull(transition, "Dominant-score artifact must converge");
            targetsSeen.add(transition.replacement().getItemCategory());
        }
        assertEquals(1, targetsSeen.size(),
                "Wide score gap must collapse finalist pool to single winner; saw: " + targetsSeen);
        assertTrue(targetsSeen.contains(EquipmentArchetype.TRIDENT.id()),
                "Clear winner must be TRIDENT when precision+bossKills dominates");
    }

    @Test
    void lineageInfluenceIsNonDominant() {
        // Lineage bias (max 0.12 primary + 0.05 secondary = 0.17 total) cannot override
        // a reputation signal that dominates by dozens of points.
        // With precision=50 and all other stats=0, vector must always be "deadeye" regardless of lineage.
        ConvergenceEngine engine = new ConvergenceEngine();
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(50);
        rep.setMobility(0);
        rep.setBossKills(1);
        rep.setKills(22); // totalScore = 50+44+5 = 99 ≥ 92

        String[] lineages = {"lineage-alpha", "lineage-beta", "lineage-gamma",
                "lineage-omega", "lineage-zeta", "lineage-theta"};
        for (String lineage : lineages) {
            Artifact artifact = preparedArtifact(77L, EquipmentArchetype.BOW);
            artifact.setLatentLineage(lineage);
            ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
            assertNotNull(transition);
            String variantId = transition.replacement().getConvergenceVariantId();
            assertTrue(variantId.contains("deadeye"),
                    "Lineage '" + lineage + "' must not override dominant precision signal; variantId=" + variantId);
        }
    }

    @Test
    void lineageInfluenceIsDeterministicAcrossRuns() {
        // The same artifact+lineage must always produce the same convergence variant.
        ConvergenceEngine engine = new ConvergenceEngine();
        ArtifactReputation rep = rangedRep();
        Artifact artifact = preparedArtifact(42L, EquipmentArchetype.BOW);
        artifact.setLatentLineage("lineage-stable");

        ArtifactIdentityTransition first  = engine.evaluateSimulation(artifact, rep);
        ArtifactIdentityTransition second = engine.evaluateSimulation(artifact, rep);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.replacement().getConvergenceVariantId(),
                second.replacement().getConvergenceVariantId(),
                "Same artifact state and lineage must always produce the same variant");
        assertEquals(first.replacement().getConvergenceLineageTrace(),
                second.replacement().getConvergenceLineageTrace(),
                "Lineage trace must be stable across repeated evaluations");
    }

    @Test
    void cadenceWeightedBossKillsReachesSurge() {
        // With bossKills=4 and killChain=2, weighted intensity = 2 + 0 + 0 + 1*2 + 4*2 = 12 ≥ 10 → surge.
        // Verifies that bossKills are no longer drowned out in the intensity calculation.
        ConvergenceEngine engine = new ConvergenceEngine();
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(28);
        rep.setMobility(16);
        rep.setBossKills(4);
        rep.setRecentKillChain(2);
        rep.setKills(20); // totalScore = 28+16+40+20 = 104 ≥ 92

        Artifact artifact = preparedArtifact(55L, EquipmentArchetype.BOW);
        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);

        assertNotNull(transition);
        String variantId = transition.replacement().getConvergenceVariantId();
        assertTrue(variantId.endsWith("-surge"),
                "Heavy boss-kill artifact must reach surge cadence; variantId=" + variantId);
    }

    private Artifact seeded(long seed, EquipmentArchetype archetype) {
        Artifact artifact = new Artifact(UUID.randomUUID(), archetype);
        artifact.setArtifactSeed(seed);
        artifact.addLoreHistory("seeded-history");
        artifact.addNotableEvent("seeded-event");
        return artifact;
    }
}
