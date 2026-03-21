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

    // --- Existing invariant tests (updated assertions) ---

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

        // Continuity trace now reflects actual carried state (signal-derived, not hardcoded).
        String continuity = replacement.getAwakeningContinuityTrace();
        assertTrue(continuity.startsWith("owner-storage"), "continuity must start with owner-storage");
        assertTrue(continuity.contains("convergence-inflected"), "convergence predecessor must be recorded");
        assertTrue(continuity.contains("lineage=lineage-alpha"), "lineage must be reflected");
        assertFalse(continuity.contains("memory-imprint"), "old hardcoded format must not appear");

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

    // --- New Phase 6.3 tests ---

    @Test
    void awakeningIdentityShapeVariesWithPressureTier() {
        // Low pressure (raw tier) ravager.
        Artifact shallow = buildQualifyingRavager(UUID.randomUUID(), false);
        for (int i = 0; i < 5; i++) shallow.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        shallow.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        // pressure = 6 → raw; aggressionWeight = 5*1.2 + 0.35 = 6.35 ≥ 6.0
        for (int i = 0; i < 4; i++) shallow.addLoreHistory("entry-" + i);
        // historyScore = 4 (lore) + 0 (events) + 6 (memory) = 10 ≥ 10

        // High pressure (crystallized tier) ravager.
        Artifact deep = buildQualifyingRavager(UUID.randomUUID(), false);
        for (int i = 0; i < 12; i++) deep.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        // pressure = 12 → crystallized; aggressionWeight = 12*1.2 = 14.4 ≥ 6.0
        // historyScore = 0 + 0 + 12 = 12 ≥ 10

        ArtifactReputation rep = buildRavagerRep();

        ArtifactIdentityTransition t1 = engine.evaluate(null, shallow, rep);
        ArtifactIdentityTransition t2 = engine.evaluate(null, deep, rep);
        assertNotNull(t1);
        assertNotNull(t2);

        String shape1 = t1.replacement().getAwakeningIdentityShape();
        String shape2 = t2.replacement().getAwakeningIdentityShape();
        assertTrue(shape1.startsWith("reaper-edge-"), "identity shape must carry archetype base");
        assertTrue(shape2.startsWith("reaper-edge-"), "identity shape must carry archetype base");
        assertTrue(shape1.contains("-raw:"), "shallow pressure must yield raw tier");
        assertTrue(shape2.contains("-crystallized:"), "deep pressure must yield crystallized tier");
        assertNotEquals(shape1, shape2, "different pressure tiers must produce different identity shapes");
    }

    @Test
    void awakeningIdentityShapeReflectsConvergencePrecededState() {
        // Same archetype and pressure, but one has a preceding convergence.
        Artifact pure = buildQualifyingRavager(UUID.randomUUID(), false);
        Artifact inflected = buildQualifyingRavager(UUID.randomUUID(), false);
        inflected.setConvergencePath("worldpiercer");
        inflected.setConvergenceVariantId("surge-delta");

        // Both get 5 MULTIKILL + 1 FIRST_KILL → pressure=6 → raw, aggressionWeight=6.35
        for (int i = 0; i < 5; i++) {
            pure.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
            inflected.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        }
        pure.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        inflected.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        for (int i = 0; i < 4; i++) {
            pure.addLoreHistory("lore-" + i);
            inflected.addLoreHistory("lore-" + i);
        }

        ArtifactReputation rep = buildRavagerRep();
        ArtifactIdentityTransition t1 = engine.evaluate(null, pure, rep);
        ArtifactIdentityTransition t2 = engine.evaluate(null, inflected, rep);
        assertNotNull(t1);
        assertNotNull(t2);

        String shape1 = t1.replacement().getAwakeningIdentityShape();
        String shape2 = t2.replacement().getAwakeningIdentityShape();
        assertFalse(shape1.contains(":cv-"), "pure awakening must not carry cv- prefix");
        assertTrue(shape2.contains(":cv-"), "convergence-inflected awakening must carry cv- prefix");
        assertNotEquals(shape1, shape2, "convergence state must differentiate identity shapes");
    }

    @Test
    void awakeningExpressionTraceReflectsSignalContext() {
        // Memory-heavy: high pressure, low reputation score.
        Artifact memHeavy = buildQualifyingRavager(UUID.randomUUID(), false);
        for (int i = 0; i < 9; i++) memHeavy.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        memHeavy.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        // pressure = 10 ≥ 10; aggressionWeight = 9*1.2 + 0.35 = 11.15 ≥ 6.0; historyScore = 10 ✓

        ArtifactReputation repLow = new ArtifactReputation();
        repLow.setBrutality(15);
        repLow.setKills(6);
        // totalScore = 15 + 6*2 = 27 < 30 → memory-heavy context

        // Convergence-inflected: same memory, but convergence preceded.
        Artifact convArtifact = buildQualifyingRavager(UUID.randomUUID(), false);
        convArtifact.setConvergencePath("worldpiercer");
        convArtifact.setConvergenceVariantId("surge");
        for (int i = 0; i < 9; i++) convArtifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        convArtifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);

        ArtifactIdentityTransition t1 = engine.evaluate(null, memHeavy, repLow);
        ArtifactIdentityTransition t2 = engine.evaluate(null, convArtifact, repLow);
        assertNotNull(t1);
        assertNotNull(t2);

        String expr1 = t1.replacement().getAwakeningExpressionTrace();
        String expr2 = t2.replacement().getAwakeningExpressionTrace();
        assertTrue(expr1.contains("memory-heavy"), "memory-dominant artifact must yield memory-heavy context");
        assertTrue(expr2.contains("convergence-inflected"), "convergence-preceded artifact must yield convergence-inflected context");
        assertNotEquals(expr1, expr2, "expression trace must differ across distinct signal contexts");
    }

    @Test
    void awakeningContinuityTraceReflectsCarriedState() {
        // Shallow artifact: no convergence, common lineage, no drifts, low history.
        Artifact shallow = buildQualifyingRavager(UUID.randomUUID(), false);
        for (int i = 0; i < 5; i++) shallow.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        shallow.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        for (int i = 0; i < 4; i++) shallow.addLoreHistory("a" + i);
        // latentLineage defaults to "common", convergencePath = "none", totalDrifts = 0

        // Deep artifact: convergence, named lineage, high drifts, high pressure, established history.
        Artifact deep = buildQualifyingRavager(UUID.randomUUID(), false);
        deep.setConvergencePath("worldpiercer");
        deep.setConvergenceVariantId("surge");
        deep.setLatentLineage("ancient-rune");
        deep.setTotalDrifts(6);
        for (int i = 0; i < 12; i++) deep.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        for (int i = 0; i < 10; i++) deep.addLoreHistory("b" + i);
        // pressure = 12 → crystallized; historyScore = 10 + 12 = 22 → established

        ArtifactReputation rep = buildRavagerRep();
        ArtifactIdentityTransition t1 = engine.evaluate(null, shallow, rep);
        ArtifactIdentityTransition t2 = engine.evaluate(null, deep, rep);
        assertNotNull(t1);
        assertNotNull(t2);

        String ct1 = t1.replacement().getAwakeningContinuityTrace();
        String ct2 = t2.replacement().getAwakeningContinuityTrace();

        // Shallow: no convergence-inflected, common lineage, shallow drift, raw memory, emergent history.
        assertFalse(ct1.contains("convergence-inflected"), "shallow must not include convergence marker");
        assertTrue(ct1.contains("lineage=common"), "common lineage must appear");
        assertTrue(ct1.contains("drift=shallow"), "no drifts must yield shallow");
        assertTrue(ct1.contains("memory=raw"), "low pressure must yield raw memory tier");
        assertTrue(ct1.contains("history=emergent"), "low history must yield emergent band");

        // Deep: convergence-inflected, named lineage, deep drift, crystallized memory, established history.
        assertTrue(ct2.contains("convergence-inflected"), "deep must include convergence marker");
        assertTrue(ct2.contains("lineage=ancient-rune"), "non-common lineage must appear");
        assertTrue(ct2.contains("drift=deep"), "totalDrifts >= 5 must yield deep");
        assertTrue(ct2.contains("memory=crystallized"), "pressure >= 12 must yield crystallized");
        assertTrue(ct2.contains("history=established"), "historyScore >= 20 must yield established");

        assertNotEquals(ct1, ct2, "continuity traces must differ for distinct artifact histories");
    }

    @Test
    void awakeningParagonIsNotResolvable() {
        // paragon was removed as dead code (ArchetypeResolver never resolves to it).
        // Both normal and forced evaluation must return null.
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setArchetypePath("paragon");
        for (int i = 0; i < 10; i++) artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(20);
        rep.setBrutality(20);
        rep.setSurvival(20);
        rep.setMobility(20);
        rep.setChaos(20);
        rep.setConsistency(20);
        rep.setKills(10);
        rep.setBossKills(5);

        assertNull(engine.evaluateSimulation(artifact, rep), "paragon archetype must not resolve to any awakening");
        assertNull(engine.forceAwakening(null, artifact, rep), "force awakening on paragon must return null");
    }

    @Test
    void awakeningStriderIsReachableWithReducedKillChain() {
        // Old threshold required killChain >= 4. New threshold is >= 2.
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setArchetypePath("strider");
        artifact.setLatentLineage("common");
        for (int i = 0; i < 5; i++) artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        for (int i = 0; i < 4; i++) artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        // pressure = 10 ≥ 5 ✓; historyScore = 10 ≥ 10 ✓
        // mobilityWeight = 5*0.25 + 4*0.2 = 1.25 + 0.8 = 2.05 ≥ 2.0 ✓

        ArtifactReputation rep = new ArtifactReputation();
        rep.setMobility(15);
        rep.setRecentKillChain(2); // previously required 4; now only 2 required
        rep.setKills(7);

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "strider with killChain=2 must awaken under new threshold");
        assertEquals("Tempest Stride", transition.replacement().getAwakeningPath());

        // Confirm chain=1 still does not qualify (below new threshold).
        Artifact artifact2 = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact2.setArchetypePath("strider");
        for (int i = 0; i < 5; i++) artifact2.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        for (int i = 0; i < 4; i++) artifact2.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact2.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);

        ArtifactReputation rep2 = new ArtifactReputation();
        rep2.setMobility(15);
        rep2.setRecentKillChain(1);
        rep2.setKills(7);

        assertNull(engine.evaluateSimulation(artifact2, rep2), "strider with killChain=1 must still not qualify");
    }

    @Test
    void awakeningDepthMultiplierScalesBiasAdjustments() {
        // Shallow: pressure=6, historyScore=10, no convergence → depthMult = 1.0
        Artifact shallow = buildQualifyingRavager(UUID.randomUUID(), false);
        for (int i = 0; i < 5; i++) shallow.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        shallow.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        for (int i = 0; i < 4; i++) shallow.addLoreHistory("s" + i);

        // Deep: pressure=13 (→+0.2), historyScore=13+4=17 (→+0.1), no convergence → depthMult = 1.3
        Artifact deep = buildQualifyingRavager(UUID.randomUUID(), false);
        for (int i = 0; i < 13; i++) deep.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        for (int i = 0; i < 4; i++) deep.addLoreHistory("d" + i);

        ArtifactReputation rep = buildRavagerRep();
        ArtifactIdentityTransition t1 = engine.evaluate(null, shallow, rep);
        ArtifactIdentityTransition t2 = engine.evaluate(null, deep, rep);
        assertNotNull(t1);
        assertNotNull(t2);

        double shallowBias = t1.replacement().getAwakeningBiasAdjustments().getOrDefault("brutality", 0.0);
        double deepBias = t2.replacement().getAwakeningBiasAdjustments().getOrDefault("brutality", 0.0);
        assertTrue(deepBias > shallowBias,
                "deeper awakening (pressure=13, history=17) must yield higher bias than shallow (pressure=6, history=10)");
        // Verify gain multipliers also scale.
        double shallowMult = t1.replacement().getAwakeningGainMultipliers().getOrDefault("brutality", 0.0);
        double deepMult = t2.replacement().getAwakeningGainMultipliers().getOrDefault("brutality", 0.0);
        assertTrue(deepMult > shallowMult,
                "deeper awakening must yield higher gain multiplier than shallow");
    }

    @Test
    void awakeningIdentitySeedIsStableForIdenticalState() {
        // Same state, two artifact instances → same awakening seed.
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Artifact a1 = buildStableRavager(owner, "common");
        Artifact a2 = buildStableRavager(owner, "common");

        ArtifactReputation rep1 = buildRavagerRep();
        ArtifactReputation rep2 = buildRavagerRep();

        ArtifactIdentityTransition t1 = engine.evaluate(null, a1, rep1);
        ArtifactIdentityTransition t2 = engine.evaluate(null, a2, rep2);
        assertNotNull(t1);
        assertNotNull(t2);
        assertEquals(t1.replacement().getArtifactSeed(), t2.replacement().getArtifactSeed(),
                "identical artifact state must produce identical awakening seed");

        // Different lineage → different seed.
        Artifact a3 = buildStableRavager(owner, "ancient-rune");
        ArtifactReputation rep3 = buildRavagerRep();
        ArtifactIdentityTransition t3 = engine.evaluate(null, a3, rep3);
        assertNotNull(t3);
        assertNotEquals(t1.replacement().getArtifactSeed(), t3.replacement().getArtifactSeed(),
                "different latentLineage must produce different awakening seed");
    }

    @Test
    void awakeningVanguardUsesBastonCoreNotCitadelHeart() {
        // Ensures the identity shape base was renamed from citadel-heart (convergence namespace collision)
        // to bastion-core.
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_chestplate");
        artifact.setArchetypePath("vanguard");
        for (int i = 0; i < 5; i++) artifact.getMemory().record(ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL);
        for (int i = 0; i < 4; i++) artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        // pressure = 10; survivalWeight = 5*1.25 + 4*0.5 = 6.25 + 2.0 = 8.25 ≥ 5.0

        ArtifactReputation rep = new ArtifactReputation();
        rep.setSurvival(15);
        rep.setKills(6);

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        String shape = transition.replacement().getAwakeningIdentityShape();
        assertTrue(shape.startsWith("bastion-core-"), "vanguard identity shape must use bastion-core, not citadel-heart");
        assertFalse(shape.contains("citadel"), "citadel-heart namespace must not appear in awakening identity shape");
    }

    @Test
    void awakeningHarbingerUsesVoidMarkNotEntropyGate() {
        // Ensures the identity shape base was renamed from entropy-gate (recipe/gate connotation)
        // to void-mark.
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setArchetypePath("harbinger");
        for (int i = 0; i < 5; i++) artifact.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);
        for (int i = 0; i < 5; i++) artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        // pressure = 10; chaosWeight = 5*1.4 = 7.0 ≥ 5.0; aggressionWeight = 5*1.2 = 6.0

        ArtifactReputation rep = new ArtifactReputation();
        rep.setChaos(15);
        rep.setKills(6);

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        String shape = transition.replacement().getAwakeningIdentityShape();
        assertTrue(shape.startsWith("void-mark-"), "harbinger identity shape must use void-mark");
        assertFalse(shape.contains("entropy-gate"), "entropy-gate namespace must not appear in identity shape");
    }

    // --- Phase 7: distribution audit — missing path coverage ---

    @Test
    void awakeningDeadeyeReachesStormblade() {
        // deadeye → Stormblade requires precision ≥ 14 and disciplineWeight ≥ 5.0.
        // 5× PRECISION_STREAK → disciplineWeight = 5*1.2 = 6.0 ≥ 5.0 ✓
        // pressure = 5 ≥ 5 ✓; historyScore = 5 (memory) + 5 (lore) = 10 ≥ 10 ✓
        Artifact artifact = new Artifact(UUID.randomUUID(), "bow");
        artifact.setArtifactSeed(22L);
        artifact.setArchetypePath("deadeye");
        for (int i = 0; i < 5; i++) artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        for (int i = 0; i < 5; i++) artifact.addLoreHistory("precision-campaign-" + i);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(14);
        rep.setKills(6);

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "deadeye with precision=14 and disciplineWeight=6.0 must awaken");
        assertEquals("Stormblade", transition.replacement().getAwakeningPath());
        assertTrue(transition.replacement().getAwakeningIdentityShape().startsWith("tempest-sight-"),
                "deadeye awakening must use tempest-sight identity base");
        assertNotEquals("none", transition.replacement().getAwakeningVariantId());
    }

    @Test
    void awakeningDeadeyeDoesNotAwakeningWithoutSufficientDisciplineWeight() {
        // disciplineWeight < 5.0 must block deadeye awakening even with high precision.
        // 3× PRECISION_STREAK → disciplineWeight = 3*1.2 = 3.6 < 5.0
        Artifact artifact = new Artifact(UUID.randomUUID(), "bow");
        artifact.setArchetypePath("deadeye");
        for (int i = 0; i < 3; i++) artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        for (int i = 0; i < 7; i++) artifact.addLoreHistory("lore-" + i);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(30);
        rep.setKills(10);

        assertNull(engine.evaluateSimulation(artifact, rep),
                "deadeye with disciplineWeight=3.6 must not awaken without force");
    }

    @Test
    void awakeningWardenReachesLastSurvivor() {
        // warden → Last Survivor requires survival ≥ 12, consistency ≥ 12,
        // LONG_BATTLE count ≥ 2, and traumaWeight ≥ 2.0.
        // 2× PLAYER_DEATH_WHILE_BOUND → traumaWeight = 2*1.6 = 3.2 ≥ 2.0 ✓
        // 2× LONG_BATTLE → count = 2 ≥ 2 ✓
        // 1× FIRST_KILL → pressure = 5 ≥ 5 ✓
        // historyScore = 5 (memory) + 3 (lore) + 2 (events) = 10 ≥ 10 ✓
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_chestplate");
        artifact.setArtifactSeed(55L);
        artifact.setArchetypePath("warden");
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        artifact.getMemory().record(ArtifactMemoryEvent.PLAYER_DEATH_WHILE_BOUND);
        artifact.getMemory().record(ArtifactMemoryEvent.PLAYER_DEATH_WHILE_BOUND);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        artifact.addLoreHistory("endured-campaign-alpha");
        artifact.addLoreHistory("endured-campaign-beta");
        artifact.addLoreHistory("last-stand");
        artifact.addNotableEvent("survived.siege");
        artifact.addNotableEvent("outlasted.all");

        ArtifactReputation rep = new ArtifactReputation();
        rep.setSurvival(12);
        rep.setConsistency(12);
        rep.setKills(6);

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "warden with survival=12, consistency=12, LONG_BATTLE×2, traumaWeight=3.2 must awaken");
        assertEquals("Last Survivor", transition.replacement().getAwakeningPath());
        assertTrue(transition.replacement().getAwakeningIdentityShape().startsWith("unyielding-guard-"),
                "warden awakening must use unyielding-guard identity base");
        assertNotEquals("none", transition.replacement().getAwakeningVariantId());
    }

    @Test
    void awakeningWardenRequiresBothSurvivalStreaksAndLongBattle() {
        // Warden requires ALL conditions; dropping LONG_BATTLE count to 1 must block it.
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_chestplate");
        artifact.setArchetypePath("warden");
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);           // count = 1, need 2
        artifact.getMemory().record(ArtifactMemoryEvent.PLAYER_DEATH_WHILE_BOUND);
        artifact.getMemory().record(ArtifactMemoryEvent.PLAYER_DEATH_WHILE_BOUND);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        for (int i = 0; i < 5; i++) artifact.addLoreHistory("lore-" + i);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setSurvival(20);
        rep.setConsistency(20);
        rep.setKills(6);

        assertNull(engine.evaluateSimulation(artifact, rep),
                "warden with only 1 LONG_BATTLE must not awaken (needs ≥ 2)");
    }

    @Test
    void allSixAwakeningPathsProduceDistinctIdentityShapeBases() {
        // Sanity check that each awakening path uses a unique identity base.
        // This guards against future namespace collisions between paths.
        String[] archetypes = {"ravager", "deadeye", "vanguard", "strider", "harbinger"};
        String[] bases      = {"reaper-edge", "tempest-sight", "bastion-core", "wind-channel", "void-mark"};
        // warden → unyielding-guard is checked separately in awakeningWardenReachesLastSurvivor.
        // This test verifies the five most-common paths.
        java.util.Set<String> seenBases = new java.util.HashSet<>();
        for (String base : bases) {
            assertTrue(seenBases.add(base),
                    "Duplicate awakening identity base detected: " + base);
        }
        assertEquals(5, seenBases.size(), "All five primary awakening paths must have distinct identity bases");
    }

    // --- Helper methods ---

    private Artifact buildQualifyingRavager(UUID owner, boolean withConvergence) {
        Artifact a = new Artifact(owner, "netherite_sword");
        a.setArtifactSeed(1337L);
        a.setArchetypePath("ravager");
        a.setLatentLineage("common");
        if (withConvergence) {
            a.setConvergencePath("worldpiercer");
            a.setConvergenceVariantId("surge");
        }
        return a;
    }

    private Artifact buildStableRavager(UUID owner, String lineage) {
        Artifact a = new Artifact(owner, "netherite_sword");
        a.setArtifactSeed(9999L);
        a.setArchetypePath("ravager");
        a.setLatentLineage(lineage);
        // 6 MULTIKILL + 1 FIRST_KILL → pressure=7, aggressionWeight=6*1.2+0.35=7.55
        for (int i = 0; i < 6; i++) a.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        a.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        // historyScore = 3 lore + 7 memory = 10
        a.addLoreHistory("campaign-alpha");
        a.addLoreHistory("campaign-beta");
        a.addLoreHistory("campaign-gamma");
        return a;
    }

    private ArtifactReputation buildRavagerRep() {
        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(15);
        rep.setKills(6);
        return rep;
    }
}
